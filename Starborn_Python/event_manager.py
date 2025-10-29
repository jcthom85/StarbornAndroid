# event_manager.py – Event/Quest dispatcher with forgiving item name matching
# ---------------------------------------------------------------------------
# • Adds _norm() helper that trims leading articles ("a", "an", "the") and
#   lower‑cases strings so data files can use either "small key" or
#   "a small key" without breaking triggers.
# • _fire() now calls _norm() when comparing the "item" field in triggers.
# ---------------------------------------------------------------------------

from __future__ import annotations
import re
from typing import TYPE_CHECKING
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.metrics import dp
from ui.menu_popup import MenuPopup
from ui.narrative_popup import NarrativePopup
from ui.system_tutorial import SystemTutorialManager
from font_manager import fonts

if TYPE_CHECKING:  # Avoid circular imports at runtime
    from game import MainWidget
    from game_objects import Room # Import your new Room class


class EventManager:
    """Central dispatcher for room/quest/cinematic triggers."""

    # ---------------------------------------------------------------------
    # Construction & helpers
    # ---------------------------------------------------------------------
    def __init__(self, events: list[dict], rooms: dict[str, "Room"], ui: "MainWidget"):
        self.events = events      # Raw list loaded from events.json
        self.rooms = rooms        # id → Room instance
        self.ui = ui              # Reference to the running game widget
        self._subs = {}
        try:
            if not hasattr(self.ui, "quest_index"):
                self.ui.quest_index = {}
        except Exception:
            pass
        self._sync_quest_index_cache()

    def emit(self, event_name: str, payload: dict | None = None):
        for fn in self._subs.get(event_name, []):
            try: fn(payload or {})
            except Exception: pass

    def subscribe(self, event_name: str, callback):
        self._subs.setdefault(event_name, []).append(callback)

    def unsubscribe(self, event_name: str, callback):
        subs = self._subs.get(event_name)
        if not subs:
            return
        try:
            subs.remove(callback)
        except ValueError:
            pass

    @staticmethod
    def _norm(name: str | None) -> str:
        """Return *name* lower‑cased and without a leading article."""
        if not name:
            return ""
        return re.sub(r"^(a|an|the)\s+", "", name.strip(), flags=re.I).lower()

    # ------------------------------------------------------------------
    # Dialogue‑driven side effects (invoked directly by DialogueManager)
    # ------------------------------------------------------------------
    def dialogue_trigger(self, trigger_type: str, trigger_value: str):
        """Simple one‑off helpers used by dialogue condition strings."""
        if trigger_type == "start_quest":
            # Re-implementing the robust quest start logic from _action_start_quest
            start_fn = getattr(self.ui, "start_quest", None)
            if callable(start_fn):
                start_fn(trigger_value)
                self._sync_quest_index_cache()
                return

            qm = getattr(self.ui, "quest_manager", None)
            if qm:
                if callable(getattr(qm, "start", None)):
                    qm.start(trigger_value)
                elif callable(getattr(qm, "start_quest", None)):
                    qm.start_quest(trigger_value)
                self._sync_quest_index_cache()

        elif trigger_type == "complete_quest":
            self.ui.complete_quest(trigger_value)
            
        # 🔺 --- REVISED "take_item" LOGIC --- 🔺
        elif trigger_type == "take_item":
            # The inventory is a dict: {item_name: quantity}.
            # We must find the item by its name or alias and then decrement the quantity.
            
            # Find the canonical item object from the master list to get its true name
            item_obj = self.ui.all_items.find(trigger_value)
            
            if item_obj and self.ui.inventory.get(item_obj.name, 0) > 0:
                self.ui.inventory[item_obj.name] -= 1
                if self.ui.inventory[item_obj.name] == 0:
                    del self.ui.inventory[item_obj.name]
                
                self.ui.update_inventory_display()
                NarrativePopup.show(f"You give the {item_obj.name}.", theme_mgr=self.ui.themes)
            else:
                # This case should ideally not happen if conditions are checked correctly
                print(f"[EventManager] Could not 'take_item': {trigger_value} not in inventory.")

        elif trigger_type == "give_item":
            self._action_give_item_to_player({"item": trigger_value})
        elif trigger_type == "give_credits":
            try:
                amount = int(trigger_value)
            except (TypeError, ValueError):
                amount = 0
            if amount > 0:
                self._action_give_credits({"amount": amount})
        elif trigger_type == "set_milestone":
            self._action_set_milestone({"milestone": trigger_value})
        elif trigger_type == "set_quest_task_done":
            try:
                quest_id, task_id = trigger_value.split(":", 1)
            except (ValueError, AttributeError):
                quest_id = task_id = None
            if quest_id and task_id:
                self._action_set_quest_task_done({"quest_id": quest_id, "task_id": task_id})
        elif trigger_type == "recruit":
            self.ui.recruit(trigger_value)

    # ------------------------------------------------------------------
    # Public trigger entry‑points (call these from game code)
    # ------------------------------------------------------------------
    def player_action(self, action_name: str, **payload):
        data = {"action": action_name}
        data.update(payload)
        self._fire("player_action", **data)
        self.emit("player_action", data)

    def talk_to(self, npc_name: str):
        self._fire("talk_to", npc=npc_name)

    def item_acquired(self, item_name: str):
        self._fire("item_acquired", item=item_name)

    def item_given(self, item_name: str, npc_name: str):
        self._fire("item_given", item=item_name, npc=npc_name)

    def enemy_defeated(self, enemy_id: str):
        self._fire("enemy_defeated", enemy=enemy_id)

    def enter_room(self, room_id: str):
        self._fire("enter_room", room=room_id)

    def event_completed(self, event_id: str):
        self._fire("complete_event", event_id=event_id)

    # ------------------------------------------------------------------
    # Core dispatcher – Match trigger blocks & run their actions
    # ------------------------------------------------------------------
    def _fire(self, trig_type: str, **kwargs):
        for ev in self.events:
            # --- Check for repeatable flag ---
            event_id = ev.get("id")
            is_repeatable = ev.get("repeatable", False)
            if event_id and not is_repeatable and event_id in self.ui.fired_events:
                continue # Already fired, not repeatable.

            trig = ev.get("trigger", {})
            if trig.get("type") != trig_type:
                continue

            matched = True
            for k, v in trig.items():
                # Ignore metadata fields during trigger matching
                if k in ("id", "description", "repeatable", "notes", "xp_reward", "on_message", "off_message"):
                    continue

                if k == "type":
                    continue
                actual = kwargs.get(k)

                # Forgiving comparison for item names (ignore articles, case)
                if k == "item":
                    if self._norm(actual) != self._norm(v):
                        matched = False
                        break
                else:
                    if actual != v:
                        matched = False
                        break

            if matched:
                # Mark as fired *before* running actions to prevent re-entry loops
                if event_id:
                    self.ui.fired_events.add(event_id)

                self._run_actions(ev.get("actions", []))
                # --- NEW: Fire completion event ---
                if event_id:
                    self.event_completed(event_id)

                # --- Optional: emit on/off message for toggle events ---
                on_msg  = ev.get("on_message")
                off_msg = ev.get("off_message")
                # Grant optional per-event XP reward
                xp = ev.get("xp_reward", 0)
                try:
                    xp_val = int(xp)
                except Exception:
                    xp_val = 0
                if xp_val > 0 and hasattr(self.ui, "add_xp"):
                    self.ui.add_xp(xp_val)

                if on_msg or off_msg:
                    # Look for the toggle action inside this event
                    for act in ev.get("actions", []):
                        if act.get("type") == "toggle_room_state":
                            room_id = act.get("room_id")
                            key     = act.get("state_key")
                            room = self.rooms.get(room_id)
                            if room and key:
                                # Determine message
                                show_text = None
                                if getattr(room, key, False) and on_msg:
                                    show_text = on_msg
                                elif not getattr(room, key, False) and off_msg:
                                    show_text = off_msg

                                if show_text:
                                    # Use the new titleless narrative popup by default
                                    try:
                                        NarrativePopup.show(show_text, theme_mgr=self.ui.themes)
                                    except Exception:
                                        # Fallback to log text if popup construction fails
                                        self.ui.say(show_text)
                            break

    # ------------------------------------------------------------------
    # Action executors – one method per action type
    # ------------------------------------------------------------------
    def _run_actions(self, actions: list[dict], start_index: int = 0):
        for i in range(start_index, len(actions)):
            act = actions[i]
            action_type = act.get('type')

            # NEW: Handle asynchronous wait action
            if action_type == 'wait_for_draw':
                # Schedule the rest of the actions to run on the next frame
                from kivy.clock import Clock
                Clock.schedule_once(lambda dt: self._run_actions(actions, start_index=i + 1), 0)
                return # Stop processing further actions in this loop

            # Modified to include the new action handler
            handler = getattr(self, f"_action_{action_type}", None)
            if callable(handler):
                handler(act)
            else:
                print(f"[EventManager] Unknown action type: {action_type}")

    def _sync_quest_index_cache(self):
        """Mirror live quest states onto legacy quest_index dicts for dialogue/tools."""
        qm = getattr(self.ui, "quest_manager", None)
        if not qm:
            return
        cache = {}
        try:
            for q in getattr(qm, "_by_id", {}).values():
                qid = getattr(q, "id", None)
                if not qid:
                    continue
                cache[qid] = {
                    "status": getattr(q, "status", None),
                    "stage_index": getattr(q, "stage_index", None)
                }
        except Exception:
            pass
        try:
            setattr(qm, "quest_index", cache)
        except Exception:
            pass
        try:
            self.ui.quest_index = cache
        except Exception:
            pass

    def _action_wait(self, act: dict):
        """This is a placeholder. Use 'wait_for_draw' for event sequences."""
        # The previous 'wait' action was not implemented correctly for event sequences.
        pass

    def _action_begin_node(self, act: dict):
        """Calls the UI to enter a node and set the starting room."""
        room_id = act.get("room_id")
        if room_id and hasattr(self.ui, "begin_node"):
            self.ui.begin_node(room_id)

    def _action_set_global_dark(self, act: dict):
        """Sets the global screen darkness, optionally forcing it."""
        from kivy.app import App
        app = App.get_running_app()
        if not app: return

        alpha = float(act.get("alpha", 0.0))
        duration = float(act.get("duration", 0.25))
        force = bool(act.get("force", False))
        app._force_black_screen = force
        app.set_global_dark(alpha, animate=(duration > 0), duration=duration)

    def _action_rebuild_ui(self, act: dict):
        """Forces the main widget to re-run its post-init setup, which can fix UI layout issues."""
        if hasattr(self.ui, "_post_init_setup"):
            self.ui._post_init_setup(0)

    # Add this new method to the EventManager class
    def _action_advance_quest_stage(self, act: dict):
        """Calls the UI to advance a quest to its next stage."""
        quest_id = act.get("quest_id")
        if not quest_id:
            return

        stage_id = act.get("stage_id")

        advanced = False
        adv_fn = getattr(self.ui, "advance_quest_stage", None)
        if callable(adv_fn):
            try:
                if stage_id:
                    adv_fn(quest_id, stage_id)
                else:
                    adv_fn(quest_id)
                advanced = True
            except TypeError:
                # Legacy signature that only accepts quest_id
                try:
                    adv_fn(quest_id)
                    advanced = True
                except Exception:
                    advanced = False
            except Exception:
                advanced = False

        if not advanced:
            qm = getattr(self.ui, "quest_manager", None)
            if qm:
                try:
                    if stage_id and callable(getattr(qm, "set_stage", None)):
                        qm.set_stage(quest_id, stage_id)
                        advanced = True
                    elif callable(getattr(qm, "next_stage", None)):
                        qm.next_stage(quest_id)
                        advanced = True
                except Exception:
                    advanced = False

        if advanced:
            self._sync_quest_index_cache()

    def _action_if_quest_status_is(self, act: dict):
        """Run nested actions only if the quest's status matches the specified status."""
        qid = act.get("quest_id")
        # Allow a single status string or a list of them
        status_to_check = act.get("status", [])
        if not isinstance(status_to_check, list):
            status_to_check = [status_to_check]

        quest = None
        qm = getattr(self.ui, "quest_manager", None)
        if qm and callable(getattr(qm, "get", None)):
            quest = qm.get(qid)
        if quest is None:
            quest_index = getattr(self.ui, "quest_index", {})
            if quest_index:
                quest = quest_index.get(qid)

        status = None
        if isinstance(quest, dict):
            status = quest.get("status")
        else:
            status = getattr(quest, "status", None)

        if status in status_to_check:
            self._run_actions(act.get("do", []))

    # ------------------------------------------------------------------
    # Local helpers for state synchronization
    # ------------------------------------------------------------------
    def _room_state_bool(self, room_id: str, key: str) -> bool:
        room = self.rooms.get(room_id)
        if not room:
            return False
        try:
            attr_val = getattr(room, key)
        except Exception:
            attr_val = None
        if attr_val is None:
            try:
                state_map = getattr(room, "state", {}) or {}
            except Exception:
                state_map = {}
            attr_val = state_map.get(key)
        return bool(attr_val)

    def _sync_dark_light_flags(self, room, key: str, value: bool) -> None:
        """
        Ensure the room keeps its complementary darkness/light flags aligned.
        Rooms may expose either 'light_on' or 'dark' (or both); keep them consistent.
        """
        try:
            state_map = getattr(room, "state", None)
        except Exception:
            state_map = None
        if state_map is None:
            state_map = {}
            try:
                setattr(room, "state", state_map)
            except Exception:
                pass

        if key == "light_on":
            light_val = bool(value)
            dark_val = not light_val
            if isinstance(state_map, dict):
                state_map["light_on"] = light_val
                state_map["dark"] = dark_val
            try:
                setattr(room, "light_on", light_val)
            except Exception:
                pass
            try:
                setattr(room, "dark", dark_val)
            except Exception:
                pass
        elif key == "dark":
            dark_val = bool(value)
            light_val = not dark_val
            if isinstance(state_map, dict):
                state_map["dark"] = dark_val
                state_map["light_on"] = light_val
            try:
                setattr(room, "dark", dark_val)
            except Exception:
                pass
            try:
                setattr(room, "light_on", light_val)
            except Exception:
                pass

    def _update_switchback_gate(self) -> None:
        """Unlock or re-lock the Switchback Hub gate based on breaker states."""
        west_on = self._room_state_bool("mines_8", "breaker_on")
        east_on = self._room_state_bool("mines_9", "breaker_on")
        unlocked = west_on and east_on

        for room_id, direction in (("mines_7", "north"), ("mines_10", "south")):
            room = self.rooms.get(room_id)
            if not room:
                continue
            try:
                block_map = getattr(room, "blocked_directions", {}) or {}
            except Exception:
                block_map = {}
            entry = block_map.get(direction)
            if not entry:
                continue
            if unlocked:
                entry["unlocked"] = True
            else:
                entry.pop("unlocked", None)

        try:
            cur = getattr(self.ui, "current_room", None)
            if cur and getattr(cur, "room_id", None) in ("mines_7", "mines_10"):
                self.ui.update_direction_markers()
        except Exception:
            pass
    # --------------------------------------------------------------
    # Concrete _action_* helpers
    # --------------------------------------------------------------
    def _action_toggle_room_state(self, act: dict):
        room = self.rooms.get(act.get("room_id"))
        key = act.get("state_key")
        if not room or not key:
            return

        # Read current value from attribute first; if missing, fall back to state dict
        cur = getattr(room, key, None)
        if cur is None:
            cur = room.state.get(key, False)
        new_val = not bool(cur)

        try:
            setattr(room, key, new_val)
        except Exception:
            pass
        try:
            room.state[key] = new_val
        except Exception:
            pass
        self._sync_dark_light_flags(room, key, new_val)
        if key == "breaker_on":
            if new_val:
                self._maybe_announce_breakers()
            self._update_switchback_gate()

        if self.ui.current_room is room:
            self.ui.update_room_display()
            # If this toggle turned on the light in the current room, and the
            # app is in the special new-game long fade, complete it immediately.
            if key == 'light_on' and bool(new_val):
                # Mark that we've completed the initial light hint in Nova's House
                try:
                    if getattr(room, 'room_id', None) == 'town_9':
                        room.state['light_hint_done'] = True
                except Exception:
                    pass
                # Notify tutorial system that the light has been turned on
                try:
                    tm = getattr(self.ui, 'tutorials', None)
                    if tm:
                        tm.on_room_state_changed(getattr(room, 'room_id', ''), key, new_val)
                except Exception:
                    pass
                try:
                    from kivy.app import App
                    app = App.get_running_app()
                    tx = getattr(app, 'tx_mgr', None)
                    if tx and getattr(tx, 'abort_on_light_switch', False) and getattr(tx, '_explore_fade_active', False):
                        tx.force_finish_exploration_fade()
                except Exception:
                    pass

    def _action_set_room_state(self, act: dict):
        """Set a room state key to an explicit boolean value.
        Example action:
            {"type": "set_room_state", "room_id": "town_9", "state_key": "dark", "value": true}
        """
        room = self.rooms.get(act.get("room_id"))
        key = act.get("state_key")
        if not room or not key:
            return

        val = act.get("value")
        # Accept common truthy/falsey forms from JSON authoring
        def _to_bool(v):
            if isinstance(v, bool):
                return v
            if isinstance(v, (int, float)):
                return v != 0
            if isinstance(v, str):
                s = v.strip().lower()
                if s in ("1", "true", "on", "yes"): return True
                if s in ("0", "false", "off", "no"): return False
            return False

        b = _to_bool(val)

        try:
            setattr(room, key, b)
        except Exception:
            pass
        try:
            room.state[key] = b
        except Exception:
            pass

        self._sync_dark_light_flags(room, key, b)
        if key == "breaker_on":
            if b:
                self._maybe_announce_breakers()
            self._update_switchback_gate()
        elif key == "dark":
            # Dark-state changes may occur from global generator toggles.
            self._update_switchback_gate()

        if self.ui.current_room is room:
            self.ui.update_room_display()

        if key == "breaker_on" and not b:
            # If the breaker was turned off, ensure gate markers refresh when present.
            try:
                cur = getattr(self.ui, "current_room", None)
                if cur and getattr(cur, "room_id", None) in ("mines_7", "mines_10"):
                    self.ui.update_direction_markers()
            except Exception:
                pass

    def _maybe_announce_breakers(self):
        """If both mine breakers are engaged, surface a narrative cue once."""
        west = self.rooms.get("mines_8")
        east = self.rooms.get("mines_9")
        hub = self.rooms.get("mines_7")
        if not (west and east and hub):
            return

        def _is_on(room):
            if not room:
                return False
            state_val = getattr(room, "breaker_on", None)
            if state_val is None:
                state_dict = getattr(room, "state", None)
                if state_dict is None:
                    state_dict = {}
                state_val = state_dict.get("breaker_on", False)
            return bool(state_val)

        if not (_is_on(west) and _is_on(east)):
            return

        hub_state = getattr(hub, "state", None)
        if hub_state is None:
            hub_state = {}
            try:
                setattr(hub, "state", hub_state)
            except Exception:
                pass
        already = getattr(hub, "breaker_alert_shown", hub_state.get("breaker_alert_shown", False))
        if already:
            return

        message = "You hear a loud noise coming from nearby."
        popup = None
        narrate_fn = getattr(self.ui, "narrate", None)
        if callable(narrate_fn):
            popup = narrate_fn(message)
        if not popup:
            say_fn = getattr(self.ui, "say", None)
            if callable(say_fn):
                say_fn(message)

        try:
            setattr(hub, "breaker_alert_shown", True)
        except Exception:
            pass
        try:
            hub_state["breaker_alert_shown"] = True
        except Exception:
            pass

    def _action_give_item_to_player(self, act: dict):
        item_name = act.get("item")
        if not item_name:
            return

        qty = act.get("quantity", 1)
        try:
            qty = int(qty)
        except Exception:
            qty = 1
        qty = max(1, qty)

        search_key = str(item_name).lower()
        item_obj = self.ui.all_items.find(search_key)
        if not item_obj:
            catalog = getattr(self.ui, "all_items", None)
            if catalog:
                for itm in catalog:
                    itm_id = getattr(itm, "id", "")
                    if itm_id and itm_id.lower() == search_key:
                        item_obj = itm
                        break
                    if getattr(itm, "name", "").lower() == search_key:
                        item_obj = itm
                        break

        if item_obj:
            inv = self.ui.inventory
            new_total = inv.get(item_obj.name, 0) + qty
            inv[item_obj.name] = new_total
            self.ui.update_inventory_display()
            if qty == 1:
                NarrativePopup.show(f"You receive [b]{item_obj.name}[/b].", theme_mgr=self.ui.themes)
            else:
                NarrativePopup.show(f"You receive [b]{qty}× {item_obj.name}[/b].", theme_mgr=self.ui.themes)
            try:
                for _ in range(qty):
                    self.item_acquired(item_obj.name)
            except Exception:
                pass

    def _action_give_credits(self, act: dict):
        amount = act.get("amount")
        try:
            amount_int = int(amount)
        except (TypeError, ValueError):
            amount_int = 0
        if amount_int <= 0:
            return
        try:
            current = getattr(self.ui, "credits", 0)
            self.ui.credits = current + amount_int
        except Exception:
            return

        label = "credit" if amount_int == 1 else "credits"
        NarrativePopup.show(
            f"You receive [b]{amount_int:,} {label}[/b].",
            theme_mgr=getattr(self.ui, "themes", None)
        )
        update_fn = getattr(self.ui, "update_inventory_display", None)
        if callable(update_fn):
            try:
                update_fn()
            except Exception:
                pass

    def _action_start_quest(self, act: dict):
        quest_id = act.get("quest_id")
        if not quest_id:
            return
        start_fn = getattr(self.ui, "start_quest", None)
        if callable(start_fn):
            self.ui.start_quest(quest_id)
            self._sync_quest_index_cache()
            return
        qm = getattr(self.ui, "quest_manager", None)
        if qm:
            if callable(getattr(qm, "start", None)):
                qm.start(quest_id)
            elif callable(getattr(qm, "start_quest", None)):
                qm.start_quest(quest_id)
            self._sync_quest_index_cache()

    def _action_complete_quest(self, act: dict):
        quest_id = act.get("quest_id")
        if not quest_id:
            return
        complete_fn = getattr(self.ui, "complete_quest", None)
        if callable(complete_fn):
            self.ui.complete_quest(quest_id)
            self._sync_quest_index_cache()
            return
        qm = getattr(self.ui, "quest_manager", None)
        if qm:
            if callable(getattr(qm, "complete", None)):
                qm.complete(quest_id)
            elif callable(getattr(qm, "complete_quest", None)):
                qm.complete_quest(quest_id)
            self._sync_quest_index_cache()

    def _action_set_quest_task_done(self, act: dict):
        quest_id = act.get("quest_id")
        task_id = act.get("task_id")
        if not quest_id or not task_id:
            return
        done = act.get("done", True)
        done_bool = bool(done)

        set_fn = getattr(self.ui, "set_quest_task_done", None)
        if callable(set_fn):
            set_fn(quest_id, task_id, done_bool)

        qm = getattr(self.ui, "quest_manager", None)
        if qm and callable(getattr(qm, "set_task_done", None)):
            qm.set_task_done(quest_id, task_id, done_bool)
        self._sync_quest_index_cache()

    def _action_set_milestone(self, act: dict):
        milestone_id = act.get("milestone") or act.get("milestone_id") or act.get("id")
        if not milestone_id:
            return
        milestones = getattr(self.ui, "milestones", None)
        if not milestones:
            return
        # Ensure milestone exists in registry for save serialization
        try:
            if isinstance(milestones.milestones, dict) and milestone_id not in milestones.milestones:
                milestones.milestones[milestone_id] = {"id": milestone_id, "name": milestone_id}
        except Exception:
            pass
        try:
            milestones.completed.add(milestone_id)
        except Exception:
            pass

    def _action_if_milestone_set(self, act: dict):
        milestone_id = act.get("milestone") or act.get("milestone_id")
        if not milestone_id:
            return
        milestones = getattr(self.ui, "milestones", None)
        completed = getattr(milestones, "completed", set()) if milestones else set()
        if milestone_id in completed:
            self._run_actions(act.get("do", []))

    def _action_if_milestone_not_set(self, act: dict):
        milestone_id = act.get("milestone") or act.get("milestone_id")
        if not milestone_id:
            return
        milestones = getattr(self.ui, "milestones", None)
        completed = getattr(milestones, "completed", set()) if milestones else set()
        if milestone_id not in completed:
            self._run_actions(act.get("do", []))

    def _action_if_milestones_set(self, act: dict):
        milestones_list = act.get("milestones") or act.get("ids") or []
        if isinstance(milestones_list, str):
            milestones_list = [milestones_list]
        milestones = getattr(self.ui, "milestones", None)
        completed = getattr(milestones, "completed", set()) if milestones else set()
        if all(mid in completed for mid in milestones_list):
            self._run_actions(act.get("do", []))

    def _action_system_tutorial(self, act: dict):
        scene_id = act.get("scene_id")
        if not scene_id:
            return
        context = act.get("context")
        manager = getattr(self.ui, "system_tutorials", None)
        if not manager:
            try:
                manager = SystemTutorialManager(self.ui)
                self.ui.system_tutorials = manager
            except Exception:
                return
        on_complete_actions = act.get("on_complete")
        manager.play(scene_id, context=context, on_complete_actions=on_complete_actions)

    def _action_player_action(self, act: dict):
        action_name = act.get("action")
        if not action_name:
            return
        payload = {k: v for k, v in act.items() if k not in {"type", "action"}}
        self.player_action(action_name, **payload)

    def _action_play_dialogue(self, act: dict):
        """Plays a single, standalone dialogue entry."""
        dialogue_id = act.get("dialogue_id")
        if dialogue_id and hasattr(self.ui, "play_dialogue"):
            self.ui.play_dialogue(dialogue_id)

    def _action_update_npc_dialogue(self, act: dict):
        # Now handled automatically by DialogueManager via conditions.
        pass

    def _action_play_cinematic(self, act: dict):
        """Tell the CinematicManager to play *scene_id*."""
        scene_id = act.get("scene_id")
        if scene_id and hasattr(self.ui, "cinematics"):
            self.ui.cinematics.play(scene_id)
        else:
            print(f"[EventManager] Cinematic scene_id not found in action: {act}")

    def _action_start_battle(self, act: dict):
        """Starts a battle with the enemies specified in the event action."""
        enemy_ids = act.get("enemy_ids")
        if not enemy_ids:
            return

        # Read optional formation data from the event
        formation = act.get("formation")

        # Define a callback to run on victory that removes all enemies
        def victory_callback():
             self.ui._on_enemy_victory(enemy_ids)

        if hasattr(self.ui, 'start_battle'):
            self.ui.start_battle(enemy_ids=enemy_ids, victory_cb=victory_callback, formation=formation)

    # ──────────────────────────────────────────────────────────────
    # Narrative popup (data-driven)
    # ──────────────────────────────────────────────────────────────
    def _action_narrate(self, act: dict):
        """
        Shows a titleless narrative popup.
        JSON schema (minimal):
          {"type":"narrate", "text":"..."}

        Optional fields:
          - buttons: [ {"label":"Continue", "do":[ <actions> ] }, ... ]
          - tap_to_dismiss: true|false  (default: true if no buttons)
          - width_hint: float (default 0.74)
          - autoclose: true|false (default true)
        """
        try:
            text = act.get("text") or act.get("message") or ""
            if not text:
                return

            buttons = act.get("buttons") or []
            title = act.get("title")
            try:
                width_hint = float(act.get("width_hint", 0.74))
            except Exception:
                width_hint = 0.74
            autoclose = bool(act.get("autoclose", True))
            tap_default = False if buttons else True
            tap_to_dismiss = bool(act.get("tap_to_dismiss", tap_default))

            actions = []
            for b in buttons:
                label = b.get("label") or b.get("text") or "OK"
                do_list = b.get("do") or []

                def _mk_cb(todo):
                    return lambda *_: self._run_actions(todo)

                actions.append((label, _mk_cb(do_list)))

            NarrativePopup.show(text,
                                title=title,
                                actions=actions if actions else None,
                                theme_mgr=self.ui.themes,
                                width_hint=width_hint,
                                autoclose=autoclose,
                                tap_to_dismiss=tap_to_dismiss)
        except Exception as e:
            try:
                self.ui.say(act.get("text") or act.get("message") or "")
            except Exception:
                print(f"[EventManager] narrate failed: {e}")

    # ──────────────────────────────────────────────────────────────
    # NEW helpers for data-driven item spawns & quest-gated blocks
    # ──────────────────────────────────────────────────────────────
    def _action_spawn_item(self, act: dict):
        """Drop *item* into *room_id* and refresh UI if the player is inside."""
        room       = self.rooms.get(act.get("room_id"))
        item_name  = act.get("item")
        if not room or not item_name:
            return
        item_obj = self.ui.all_items.find(item_name)
        if item_obj:
            room.items.add(item_obj)
            if self.ui.current_room is room:
                self.ui.update_room_display()

    def _action_if_quest_active(self, act: dict):
        """Run the nested actions only if the quest is currently active."""
        qid = act.get("quest_id")
        if not qid:
            return

        quest = None
        qm = getattr(self.ui, "quest_manager", None)
        if qm and callable(getattr(qm, "get", None)):
            quest = qm.get(qid)
        if quest is None:
            quest_index = getattr(self.ui, "quest_index", {})
            if quest_index:
                quest = quest_index.get(qid)

        status = None
        if isinstance(quest, dict):
            status = quest.get("status")
        else:
            status = getattr(quest, "status", None)

        if status == "active":
            self._run_actions(act.get("do", []))

    # ──────────────────────────────────────────────────────────────
    # Route access helpers (unlock hubs/nodes/worlds via events)
    # ──────────────────────────────────────────────────────────────
    def _action_unlock_hub(self, act: dict):
        hub_id = act.get("hub_id") or act.get("id")
        if not hub_id:
            return
        if hasattr(self.ui, "unlock_hub"):
            self.ui.unlock_hub(hub_id)

    def _action_unlock_node(self, act: dict):
        node_id = act.get("node_id") or act.get("id")
        if not node_id:
            return
        if hasattr(self.ui, "unlock_node"):
            self.ui.unlock_node(node_id)

    def _action_unlock_world(self, act: dict):
        world_id = act.get("world_id") or act.get("id")
        if not world_id:
            return
        if hasattr(self.ui, "unlock_world"):
            self.ui.unlock_world(world_id)
            
