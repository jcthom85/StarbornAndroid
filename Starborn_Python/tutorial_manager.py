from __future__ import annotations

from typing import Optional

from kivy.clock import Clock


class TutorialManager:
    """
    Lightweight tutorial/hint system.

    Responsibilities:
    - Track seen/completed tutorials (persist via MainWidget save payload)
    - Listen to high-level signals (room enter, player moved, room state changes)
    - Schedule time-based hints and cancel them when no longer needed
    - Display guidance via NarrativePopup
    """

    def __init__(self, ui, is_new_game: bool = False):
        # ui is the MainWidget (game) instance
        self.ui = ui
        # Simple state dicts (persist across sessions)
        self.seen: set[str] = set()
        self.completed: set[str] = set()
        self.rooms_seen: set[str] = set()  # track first-visit rooms by id
        self.is_new_game = is_new_game

        # Timers we may schedule (by key)
        self._timers: dict[str, object] = {}

    # --------------------------
    # Persistence (load/save)
    # --------------------------
    def to_dict(self) -> dict:
        return {
            "seen": sorted(self.seen),
            "completed": sorted(self.completed),
            "rooms_seen": sorted(self.rooms_seen),
        }

    def load_dict(self, data: Optional[dict]):
        if not isinstance(data, dict):
            return
        try:
            self.seen = set(data.get("seen", []) or [])
            self.completed = set(data.get("completed", []) or [])
            self.rooms_seen = set(data.get("rooms_seen", []) or [])
        except Exception:
            pass

    # --------------------------
    # Public signals from Game
    # --------------------------
    def on_room_enter(self, room_id: Optional[str]):
        """Called after the player enters a room (including on load)."""
        # Cancel any room-scoped timers
        self._cancel("light_switch_hint")
        self._cancel("swipe_hint")

        if not room_id:
            return

        # 1) Nova's House: hint to tap the light switch if it's still dark
        if room_id == "town_9" and not self._is_completed("light_switch_touch"):
            try:
                room = self.ui.rooms.get(room_id)
                is_light_on = bool(getattr(room, "state", {}).get("light_on"))
            except Exception:
                is_light_on = True
            first_visit = room_id not in self.rooms_seen
            # Mark as seen now so we consider only the very first entry
            self.rooms_seen.add(room_id)
            if first_visit and not is_light_on:
                # Schedule the hint after 14 seconds of inactivity
                self._schedule(
                    "light_switch_hint",
                    14.0,
                    lambda dt: self._maybe_show_light_switch_hint(room_id),
                )

    def on_player_moved(self, from_room_id: Optional[str], to_room_id: Optional[str]):
        """Called after a successful room transition."""
        # Any successful movement satisfies the movement tutorial
        self._complete("swipe_move")
        self._cancel("swipe_hint")

    def on_room_state_changed(self, room_id: str, key: str, value):
        """Called when EventManager toggles a room state."""
        # The light switch was just turned on for the first time.
        if room_id == "town_9" and key == "light_on" and bool(value) and not self._is_completed("light_switch_touch"):
            # 1. Mark the light tutorial as done and cancel any pending hint for it.
            self._complete("light_switch_touch")
            self._cancel("light_switch_hint")

            # 2. This is the ONLY place we schedule the swipe hint.
            #    If the swipe tutorial hasn't been done yet, schedule its hint now.
            if not self._is_completed("swipe_move"):
                # Schedule with a short delay so it doesn't feel immediate after the light comes on.
                self._schedule("swipe_hint", 3.0, lambda dt: self._maybe_show_swipe_hint(room_id))

    # --------------------------
    # Internals
    # --------------------------
    def _schedule(self, key: str, delay: float, cb):
        self._cancel(key)
        try:
            ev = Clock.schedule_once(cb, float(delay))
            self._timers[key] = ev
        except Exception:
            pass

    def _cancel(self, key: str):
        ev = self._timers.pop(key, None)
        if ev is not None:
            try:
                Clock.unschedule(ev)  # tolerate if already fired
            except Exception:
                pass

    # Public pause/resume helpers for Light Switch popup
    def pause_light_switch_hint(self):
        self._cancel("light_switch_hint")
        setattr(self, "_light_switch_snoozed", True)

    def resume_light_switch_hint_if_needed(self, room_id: str, delay: float = 5.0):
        # Only resume if not completed, still in that room, still dark, and the
        # intro condition (first visit) applied when scheduled.
        if self._is_completed("light_switch_touch"):
            return
        try:
            cur = getattr(self.ui, "current_room", None)
            if not cur or getattr(cur, "room_id", None) != room_id:
                return
            is_light_on = bool(getattr(cur, "state", {}).get("light_on"))
        except Exception:
            return

        if getattr(self, "_light_switch_snoozed", False) and not is_light_on and (room_id in self.rooms_seen):
            # Defer if a fade is active
            try:
                from kivy.app import App
                app = App.get_running_app()
                tx = getattr(app, 'tx_mgr', None)
                if tx and getattr(tx, '_explore_fade_active', False):
                    self._schedule("light_switch_hint", 1.0, lambda dt: self.resume_light_switch_hint_if_needed(room_id, delay))
                    return
            except Exception:
                pass
            self._schedule("light_switch_hint", delay, lambda dt: self._maybe_show_light_switch_hint(room_id))
        setattr(self, "_light_switch_snoozed", False)

    def _mark_seen(self, tid: str):
        self.seen.add(tid)

    def _complete(self, tid: str):
        self.seen.add(tid)
        self.completed.add(tid)

    def _is_completed(self, tid: str) -> bool:
        return tid in self.completed

    # Hint presenters
    def _maybe_show_light_switch_hint(self, room_id: str):
        if self._is_completed("light_switch_touch"):
            return
        try:
            room = self.ui.rooms.get(room_id)
            if not room or bool(getattr(room, "state", {}).get("light_on")):
                return  # already lit or invalid
        except Exception:
            return

        # Defer if a full-screen exploration fade is actively covering UI
        try:
            from kivy.app import App
            app = App.get_running_app()
            tx = getattr(app, 'tx_mgr', None)
            if tx and getattr(tx, '_explore_fade_active', False):
                # Try again shortly after the fade changes
                self._schedule("light_switch_hint", 1.0, lambda dt: self._maybe_show_light_switch_hint(room_id))
                return
        except Exception:
            pass

        # Show narrative popup (soft, tap-anywhere-to-dismiss)
        try:
            from ui.narrative_popup import NarrativePopup
            text = ("Try tapping the \"light switch\" to turn on the lights.")
            NarrativePopup.show(text, theme_mgr=self.ui.themes, tap_to_dismiss=True)
            self._mark_seen("light_switch_touch")
        except Exception:
            # Fallback: log line
            try:
                self.ui.say("Hint: Tap the Light Switch to turn on the lights.")
                self._mark_seen("light_switch_touch")
            except Exception:
                pass

    def _maybe_show_swipe_hint(self, room_id: str):
        if self._is_completed("swipe_move"):
            return
        # If we've left the room, do nothing
        try:
            cur = getattr(self.ui, "current_room", None)
            if not cur or getattr(cur, "room_id", None) != room_id:
                return
            has_exits = bool(getattr(cur, "exits", {}))
            is_light_on = bool(getattr(cur, "state", {}).get("light_on"))
        except Exception:
            has_exits, is_light_on = False, True
        if not has_exits:
            return
        # In Nova's House, don't show swipe hint until the light is ON
        if room_id == "town_9" and not is_light_on:
            return

        # Defer if a full-screen exploration fade is actively covering UI
        try:
            from kivy.app import App
            app = App.get_running_app()
            tx = getattr(app, 'tx_mgr', None)
            if tx and getattr(tx, '_explore_fade_active', False):
                self._schedule("swipe_hint", 1.0, lambda dt: self._maybe_show_swipe_hint(room_id))
                return
        except Exception:
            pass

        try:
            from ui.narrative_popup import NarrativePopup
            text = ("Swipe left or right to move between rooms.")
            NarrativePopup.show(text, theme_mgr=self.ui.themes, tap_to_dismiss=True)
            self._mark_seen("swipe_move")
        except Exception:
            try:
                self.ui.say("Hint: Swipe left/right to move between rooms.")
                self._mark_seen("swipe_move")
            except Exception:
                pass
