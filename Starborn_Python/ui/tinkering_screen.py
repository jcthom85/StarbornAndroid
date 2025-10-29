# ui/tinkering_screen.py  —  v2 portrait-first, themed, with icons + recipe book

from __future__ import annotations

import json, os
from collections import Counter
from kivy.uix.anchorlayout import AnchorLayout
from kivy.app import App
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.metrics import dp, sp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.image import Image
from kivy.uix.label import Label
from kivy.uix.screenmanager import Screen, FadeTransition
from kivy.uix.scrollview import ScrollView
from kivy.uix.gridlayout import GridLayout
from font_manager import fonts
from ui.menu_overlay import MenuOverlay
from ui.menu_popup import MenuPopup
from ui.bordered_frame import BorderedFrame

# v2.1: Swipe-down to close, "Schematics" instead of "Recipe Book", learned-only list.
class TinkeringScreen(Screen):
    """
    Portrait-first, themed tinkering interface:
    - Left: Base + two component slots, with icons and pulse feedback
    - Right: Recipe Preview (result + short desc + requirement checklist)
    - Footer: status message + Recipe Book + primary Tinker button
    - Supports Auto-Fill from Recipe Book if you have the parts
    """
    station_name      = "Tinkering Bench"
    subtitle          = "Repair, modify, and innovate."
    background_image  = "images/ui/starborn_menu_bg.png"

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.game = None
        self._overlay = None
        self._closing_after_tutorial = False
        self._post_tutorial_exit_done = False
        self.tm = None  # ThemeManager (per-room)

        # selection state
        self.base_item: str | None = None
        self.components: list[str | None] = [None, None]
        self.active_recipe: dict | None = None

        # data
        self.recipes = self._load_recipes()

        # widgets references we’ll update
        self.base_item_slot = None
        self.component_slot_1 = None
        self.component_slot_2 = None
        self.result_label = None
        self.req_grid = None
        self.message_label = None
        self._dismiss_fn = None
        self.btn_tinker = None

    # ───────────────────────────────────────────────────────── lifecycle
    def on_pre_enter(self, *_):
        app = App.get_running_app()
        self.game = app.current_game
        self.tm = self._room_themes()
        self._build_overlay()
        self._did_finish_tutorial = False
        self._closing_after_tutorial = False
        self._post_tutorial_exit_done = False

        # Listen for the tutorial completion event
        if self.game and getattr(self.game, "event_manager", None):
            self.game.event_manager.subscribe("player_action", self._on_player_action)

        self._reset_bench()
        self.message_label.text = "Select a slot to add parts from your inventory."

        if self.game and getattr(self.game, "event_manager", None):
            try:
                self.game.event_manager.player_action("tinkering_screen_entered")
            except Exception:
                pass

    def on_leave(self, *_):
        # Unsubscribe from events to prevent memory leaks
        if self.game and getattr(self.game, "event_manager", None):
            self.game.event_manager.unsubscribe("player_action", self._on_player_action)

        if self._overlay:
            try:
                Window.remove_widget(self._overlay)
            except Exception:
                pass
            self._overlay = None

        # Fire a final event so the game knows the screen is gone
        if self.game and getattr(self.game, "event_manager", None):
            if getattr(self, "_did_finish_tutorial", False):
                self.game.event_manager.player_action("tinkering_screen_closed")

        # close transition overlay used for blur/vignette
        try:
            App.get_running_app().tx_mgr.close()
        except Exception:
            pass

        if self.game:
            try:
                self.game.resume_enemy_timers()
                if getattr(self.game, "system_tutorials", None):
                    self.game.system_tutorials.stop()
            except Exception:
                pass

        self._did_finish_tutorial = False
        self._closing_after_tutorial = False
        self._post_tutorial_exit_done = False

    def _on_player_action(self, payload: dict | None):
        """Event handler to catch the tutorial completion signal."""
        if payload and payload.get("action") == "tinkering_tutorial_finished":
            self._did_finish_tutorial = True
            Clock.schedule_once(self._close_after_tutorial, 1)

    def _close_after_tutorial(self, *_):
        """Close the overlay (and thus the screen) once the tutorial wraps."""
        if self._closing_after_tutorial:
            return
        self._closing_after_tutorial = True

        if self._overlay:
            try:
                if hasattr(self._overlay, "close"):
                    self._overlay.close()
                elif hasattr(self._overlay, "dismiss"):
                    self._overlay.dismiss()
            except Exception:
                pass

        Clock.schedule_once(self._finish_overlay_exit, 0.45)

    def _finish_overlay_exit(self, *_):
        if self._post_tutorial_exit_done:
            return
        self._post_tutorial_exit_done = True

        if self._overlay:
            parent = getattr(self._overlay, 'parent', None)
            if parent:
                try:
                    parent.remove_widget(self._overlay)
                except Exception:
                    pass
            else:
                try:
                    Window.remove_widget(self._overlay)
                except Exception:
                    pass
            self._overlay = None

        app = App.get_running_app()
        if app and hasattr(app, "tx_mgr"):
            try:
                app.tx_mgr.close()
            except Exception:
                pass

        sm = getattr(app, "screen_manager", None)
        if sm and sm.current != "explore":
            old = sm.transition
            sm.transition = FadeTransition(duration=1.0)
            sm.current = "explore"
            Clock.schedule_once(lambda *_: setattr(sm, "transition", old), 0)

    # ───────────────────────────────────────────────────────── themes
    def _room_themes(self):
        """
        Returns a ThemeManager instance configured to the current room/hub theme.
        Falls back gracefully to default.
        """
        try:
            from theme_manager import ThemeManager
            tm = ThemeManager()
            room   = getattr(self.game, "current_room", None)
            hub_id = self.game.world_manager.current_hub_id
            hub    = self.game.world_manager.hubs.get(hub_id, {}) if hub_id else {}
            env    = getattr(room, "env", None) or hub.get("theme", "default")
            tm.use(env)
            return tm
        except Exception:
            return None

    # ───────────────────────────────────────────────────────── overlay
    def _build_overlay(self):
        if self._overlay:
            return

        try:
            self.game.pause_enemy_timers()
        except Exception:
            pass

        tex = App.get_running_app().last_room_tex
        self._overlay = MenuOverlay(
            default_tab=None,
            title="Let’s Tinker!",
            background_tex=tex,
            show_background=False,
        )

        from kivy.clock import Clock
        from kivy.uix.screenmanager import FadeTransition

        # Ensure we always return to the exploration view when the overlay closes.
        self._overlay.bind(on_dismiss=self._finish_overlay_exit)

        # ───────── body (portrait-first) ─────────
        content = BoxLayout(orientation="vertical",
                            spacing=dp(14),
                            padding=(dp(16), dp(18)))

        # === WORKSPACE (VERTICAL): Base (center) → Components (2-up) → Preview (wide) ===
        workspace = BoxLayout(orientation="vertical",
                            spacing=dp(12),
                            size_hint_y=0.68)

        # ---- BASE (centered) ----
        workspace.add_widget(Label(
            text="[b]BASE ITEM[/b]", markup=True,
            font_name=fonts["popup_title"]["name"],
            font_size=fonts["popup_title"]["size"],
            size_hint_y=None, height=dp(22), color=self._fg())
        )

        base_wrap = AnchorLayout(anchor_x="center", anchor_y="center",
                                size_hint_y=None, height=dp(68))
        # keep the base slot visually centered; let it scale with screen width
        self.base_item_slot = self._create_slot(0)
        self.base_item_slot.size_hint = (0.92, None)
        base_wrap.add_widget(self.base_item_slot)
        workspace.add_widget(base_wrap)

        # ---- COMPONENTS (side-by-side) ----
        workspace.add_widget(Label(
            text="[b]COMPONENTS[/b]", markup=True,
            font_name=fonts["popup_title"]["name"],
            font_size=fonts["popup_title"]["size"],
            size_hint_y=None, height=dp(22), color=self._fg())
        )

        comps_row = BoxLayout(orientation="horizontal", spacing=dp(10),
                            size_hint_y=None, height=dp(68))
        self.component_slot_1 = self._create_slot(1)
        self.component_slot_2 = self._create_slot(2)
        self.component_slot_1.size_hint = (0.46, None)
        self.component_slot_2.size_hint = (0.46, None)
        comps_row.add_widget(self.component_slot_1)
        comps_row.add_widget(self.component_slot_2)
        workspace.add_widget(comps_row)

        # ---- PREVIEW (wide, scrollable req list retained) ----
        workspace.add_widget(Label(
            text="[b]PREVIEW[/b]", markup=True,
            font_name=fonts["popup_title"]["name"],
            font_size=fonts["popup_title"]["size"],
            size_hint_y=None, height=dp(22), color=self._fg())
        )

        preview_frame = BorderedFrame(
            padding=dp(10),
            border_color=self._border(0.85),
            size_hint=(1, 1)
        )
        right_stack = BoxLayout(orientation="vertical", spacing=dp(8))

        self.result_label = Label(
            text="[i]Add a base item…[/i]",
            markup=True,
            font_name=fonts["medium_text"]["name"],
            font_size=fonts["medium_text"]["size"],
            halign="left",
            valign="top",
            color=self._fg(),
            size_hint=(1, None),
        )
        self.result_label.bind(
            width=lambda l, w: setattr(l, "text_size", (w, None)),
            texture_size=lambda l, ts: setattr(l, "height", ts[1])
        )

        self.req_grid = GridLayout(cols=1, spacing=dp(4), size_hint_y=None)
        self.req_grid.bind(minimum_height=self.req_grid.setter("height"))

        req_scroll = ScrollView(size_hint=(1, 1), do_scroll_x=False, bar_width=dp(4))
        req_scroll.add_widget(self.req_grid)

        right_stack.add_widget(self.result_label)
        right_stack.add_widget(req_scroll)
        preview_frame.add_widget(right_stack)
        workspace.add_widget(preview_frame)

        content.add_widget(workspace)

        # === FOOTER: message + buttons (Recipe Book / Clear All / Tinker) ===
        footer = BoxLayout(orientation="vertical", spacing=dp(10),
                        size_hint_y=None, height=dp(128))

        self.message_label = Label(
            text="", markup=True,
            font_name=fonts["small_text"]["name"],
            font_size=fonts["small_text"]["size"],
            halign="center",
            color=self._fg()
        )
        self.message_label.bind(size=lambda l, *_: setattr(l, "text_size", l.size))
        footer.add_widget(self.message_label)

        # buttons row
        row = BoxLayout(size_hint_y=None, height=dp(56), spacing=dp(10))
        
        # --- MODIFIED SCHEMATICS BUTTON ---
        btn_recipes = Button(
            text="Schematics",
            font_name=fonts["popup_button"]["name"],
            font_size=fonts["popup_button"]["size"],
            background_normal="", background_color=self._fg(),
            color=self._bg(),
            on_release=self._open_recipe_book
        )
        # --- MODIFIED CLEAR BUTTON ---
        btn_clear = Button(
            text="Clear All",
            font_name=fonts["popup_button"]["name"],
            font_size=fonts["popup_button"]["size"],
            background_normal="", background_color=self._fg(0.6), # Slightly dimmer for secondary action
            color=self._bg(),
            on_release=lambda *_: self._reset_bench()
        )
        # --- MODIFIED TINKER BUTTON ---
        self.btn_tinker = Button(
            text="[b]T I N K E R[/b]", markup=True,
            font_name=fonts["popup_button"]["name"],
            font_size=fonts["popup_button"]["size"],
            background_normal="", background_color=self._accent(),
            color=(1, 1, 1, 1),
            on_release=self._on_tinker
        )
        row.add_widget(btn_recipes)
        row.add_widget(btn_clear)
        row.add_widget(self.btn_tinker)
        footer.add_widget(row)

        content.add_widget(footer)

        # mount
        self._overlay.content_area.clear_widgets()
        self._overlay.content_area.add_widget(content)
        Window.add_widget(self._overlay)

    # ───────────────────────────────────────────────────────── visuals helpers
    def _bg(self):  # background color from theme
        if self.tm:
            return self.tm.col("bg")
        return (0, 0, 0, 1)

    def _fg(self, alpha: float | None = None):
        if self.tm:
            r, g, b, a = self.tm.col("fg")
            return (r, g, b, alpha if alpha is not None else a)
        return (1, 1, 1, alpha if alpha is not None else 1)

    def _border(self, a: float = 1.0):
        if self.tm:
            r, g, b, _ = self.tm.col("border")
            return (r, g, b, a)
        return (0.35, 0.8, 1.0, a)

    def _accent(self):
        # use fg as button fill if we don't have a distinct accent in theme
        return self.tm.col("fg") if self.tm else (0.8, 0.3, 0.1, 1)

    # icon selection borrowed from your shop row logic
    def _icon_for_item(self, name: str) -> str:
        try:
            item = self.game.all_items.find(name)
        except Exception:
            item = None

        icon_path = "images/ui/item_icon_generic.png"
        if item is None:
            return icon_path

        item_type = getattr(item, 'type', 'junk')
        name_lc = item.name.lower()

        if item_type == 'consumable':
            icon_path = "images/ui/item_icon_consumable.png"
            if any(k in name_lc for k in ('stew', 'salad', 'ramen', 'fish', 'meat')):
                icon_path = "images/ui/item_icon_food.png"
        elif item_type == 'ingredient':
            if getattr(item, 'subtype', None) == 'fish':
                icon_path = "images/ui/item_icon_fish.png"
            else:
                icon_path = "images/ui/item_icon_ingredient.png"
                if any(k in name_lc for k in ('scrap', 'wiring', 'lens', 'weave')):
                    icon_path = "images/ui/item_icon_material.png"
        elif item_type == 'fishing_rod':
            icon_path = "images/ui/item_icon_fishing.png"
        elif item_type == 'equippable':
            slot = item.equipment.get('slot', '')
            if slot == 'armor':
                icon_path = "images/ui/item_icon_armor.png"
                if 'gloves' in name_lc:
                    icon_path = "images/ui/item_icon_gloves.png"
                elif 'pendant' in name_lc:
                    icon_path = "images/ui/item_icon_pendant.png"
            elif slot == 'accessory':
                icon_path = "images/ui/item_icon_accessory.png"
            elif slot == 'weapon':
                icon_path = "images/ui/item_icon_sword.png"
                if any(k in name_lc for k in ('gun', 'pistol', 'rifle')):
                    icon_path = "images/ui/item_icon_gun.png"

        return getattr(item, "icon", icon_path)

    # ───────────────────────────────────────────────────────── slots
    def _create_slot(self, slot_index: int) -> BorderedFrame:
        frame = BorderedFrame(
            padding=(dp(8), dp(8)),
            border_color=self._border(0.85)
        )

        row = BoxLayout(orientation="horizontal", spacing=dp(10))
        icon = Image(size_hint=(None, None), size=(dp(40), dp(40)),
                     source="images/ui/item_icon_generic.png",
                     color=(1, 1, 1, 0.3))
        # --- MODIFIED SLOT BUTTON ---
        name_btn = Button(
            text="[ EMPTY ]",
            markup=True,
            halign="center", valign="middle",
            font_name=fonts["popup_button"]["name"],
            font_size=fonts["popup_button"]["size"],
            background_normal="", background_color=self._fg(0.10),
            color=self._fg(),
            size_hint=(1, None), height=dp(44)
        )
        name_btn.bind(size=lambda b, *_: setattr(b, "text_size", b.size))
        name_btn.bind(on_release=lambda *_: self._on_slot_press(slot_index))

        row.add_widget(icon)
        row.add_widget(name_btn)
        frame.add_widget(row)

        # stash for update
        frame._icon = icon
        frame._btn = name_btn

        return frame

    def _pulse_frame(self, frame: BorderedFrame, good=True):
        # quick border pulse for feedback
        try:
            from kivy.animation import Animation
            c = self._border(1.0) if good else (1, 0.35, 0.35, 1)
            base = self._border(0.9)
            frame.border_color = c
            Animation.cancel_all(frame)
            (Animation(d=0.12) + Animation(d=0.14)).bind(on_complete=lambda *_: setattr(frame, "border_color", base)).start(frame)
        except Exception:
            pass

    def _on_slot_press(self, slot_index: int):
        # picker popup: inventory items as buttons
        actions = []
        inv = getattr(self.game, "inventory", {}) or {}

        # “clear”
        cur = (self.base_item if slot_index == 0 else self.components[slot_index - 1])
        if cur:
            actions.append(("[ Clear Slot ]", lambda *_: self._select_item_for_slot(None, slot_index)))

        # items in inventory
        if inv:
            # build a scrollable grid of buttons (matching MenuPopup look)
            body = BoxLayout(orientation="vertical", spacing=dp(8), padding=dp(8), size_hint=(1, None))
            body.bind(minimum_height=lambda b, h: setattr(b, "height", h))

            grid = GridLayout(cols=1, spacing=dp(6), size_hint_y=None)
            grid.bind(minimum_height=grid.setter("height"))

            for name, qty in sorted(inv.items()):
                if qty <= 0:
                    continue
                btn = Button(
                    text=f"{name}  [color=aaaaaa](x{qty})[/color]",
                    markup=True,
                    size_hint_y=None, height=dp(44),
                    background_normal="", background_color=self._fg(),
                    color=self._bg()
                )
                btn.bind(on_release=lambda _b, _n=name: (
                    self._select_item_for_slot(_n, slot_index),
                    popup.dismiss())
                )
                grid.add_widget(btn)

            sv = ScrollView(size_hint=(1, None), height=dp(280), do_scroll_x=False, bar_width=dp(4))
            sv.add_widget(grid)
            body.add_widget(sv)

            popup = MenuPopup(
                "Select an Item",
                body=body,
                actions=[],
                size_hint=(0.9, None),
                theme_mgr=self.tm,
                autoclose=False
            )
            popup.open()
            return

        # no inventory
        self.message_label.text = "[i]Your inventory is empty.[/i]"

    def _select_item_for_slot(self, item_name: str | None, slot_index: int):
        if slot_index == 0:
            self.base_item = item_name if item_name else None
            self._refresh_slot_visual(self.base_item_slot, self.base_item)
            self._pulse_frame(self.base_item_slot, good=bool(item_name))
        else:
            self.components[slot_index - 1] = item_name if item_name else None
            frame = self.component_slot_1 if slot_index == 1 else self.component_slot_2
            self._refresh_slot_visual(frame, item_name)
            self._pulse_frame(frame, good=bool(item_name))

        self._update_preview()
        if slot_index == 0 and item_name and self.game and getattr(self.game, "event_manager", None):
            try:
                itm = self.game.all_items.find(item_name)
                itm_id = getattr(itm, "id", None) if itm else None
                if isinstance(itm_id, str):
                    itm_id = itm_id.lower()
                self.game.event_manager.player_action(
                    "select_tinkering_item",
                    item_id=itm_id or item_name.lower()
                )
            except Exception:
                pass

    def _refresh_slot_visual(self, frame: BorderedFrame, item_name: str | None):
        if item_name:
            frame._btn.text = item_name
            frame._btn.color = self._bg()  # invert on fg button fill
            frame._btn.background_color = self._fg()
            frame._icon.source = self._icon_for_item(item_name)
            frame._icon.color = (1, 1, 1, 1)
        else:
            frame._btn.text = "[ EMPTY ]"
            frame._btn.color = self._fg()
            frame._btn.background_color = self._fg(0.10)
            frame._icon.source = "images/ui/item_icon_generic.png"
            frame._icon.color = (1, 1, 1, 0.30)

    # ───────────────────────────────────────────────────────── preview / craft
    def _normalize(self, s):
        return s.strip().lower() if isinstance(s, str) else s

    def _output_name(self, recipe):
        return recipe.get("name") or recipe.get("result") or recipe.get("id", "Unknown Item")

    def _update_preview(self):
        """
        Match currently selected base + components to a recipe (unordered comps).
        Build a requirement checklist and update Tinker button enabled state.
        """
        self.active_recipe = None

        if not self.base_item:
            self.result_label.text = "[i]Add a base item…[/i]"
            self._rebuild_requirements([])
            self._set_tinker_enabled(False)
            return

        sel_base_norm = self._normalize(self.base_item)
        sel_comps = [c for c in self.components if c]
        sel_comp_norm = [self._normalize(c) for c in sel_comps]

        # find exact match
        for recipe in self.recipes:
            r_base = recipe.get("base")
            r_comps = recipe.get("components", []) or []
            if not r_base:
                continue
            if sel_base_norm != self._normalize(r_base):
                continue
            if Counter(sel_comp_norm) != Counter(self._normalize(x) for x in r_comps if x):
                continue
            self.active_recipe = recipe
            break

        if self.active_recipe:
            title = self._output_name(self.active_recipe)
            desc = self.active_recipe.get("description", "")
            self.result_label.text = f"[b]{title}[/b]\n\n{desc}" if desc else f"[b]{title}[/b]"
            needs = [self.base_item] + sel_comps
            self._rebuild_requirements(needs)
            self._set_tinker_enabled(self._has_inventory_for(needs))
        else:
            self.result_label.text = "These parts don't seem to fit together."
            self._rebuild_requirements([])
            self._set_tinker_enabled(False)

    def _has_inventory_for(self, items: list[str]) -> bool:
        inv = getattr(self.game, "inventory", {}) or {}
        need = Counter(items)
        return all(inv.get(n, 0) >= c for n, c in need.items())

    def _rebuild_requirements(self, items: list[str]):
        # Clear
        self.req_grid.clear_widgets()
        if not items:
            return

        inv = getattr(self.game, "inventory", {}) or {}
        for name, count in Counter(items).items():
            have = inv.get(name, 0)
            ok = have >= count
            icon = "✓" if ok else "✗"
            col = "a8ffc0" if ok else "ff9a9a"
            lbl = Label(
                text=f"[color={col}]{icon}[/color]  {name}  [color=aaaaaa](need {count}, have {have})[/color]",
                markup=True,
                size_hint_y=None, height=dp(24),
                font_size=sp(14),
                color=self._fg()
            )
            lbl.bind(size=lambda l, *_: setattr(l, "text_size", l.size))
            self.req_grid.add_widget(lbl)

    def _set_tinker_enabled(self, enabled: bool):
        if not self.btn_tinker:
            return
        self.btn_tinker.disabled = not enabled
        self.btn_tinker.background_color = self._accent() if enabled else self._fg(0.18)

    def _on_tinker(self, *_):
        if not self.active_recipe:
            self.message_label.text = "[color=ff8888]Parts spark, fizzle, then die…[/color]"
            return

        selected = [self.base_item] + [c for c in self.components if c]
        inv = self.game.inventory

        base_item_id = None
        if self.base_item:
            itm = self.game.all_items.find(self.base_item)
            base_item_id = getattr(itm, "id", None)
            if isinstance(base_item_id, str):
                base_item_id = base_item_id.lower()

        # validate
        need = Counter(selected)
        for item_name, count_needed in need.items():
            if inv.get(item_name, 0) < count_needed:
                self.message_label.text = f"[color=ff8888]You're missing another {item_name}![/color]"
                return

        # consume
        for item_name, count_needed in need.items():
            inv[item_name] -= count_needed
            if inv[item_name] <= 0:
                del inv[item_name]

        # grant
        crafted = self._output_name(self.active_recipe)
        self._grant_item_to_player(crafted, 1)

        # refresh inventory hud if present
        try:
            self.game.update_inventory_display()
        except Exception:
            pass

        # feedback popup
        line = self.active_recipe.get("success_message", f"You assemble [b]{crafted}[/b].")
        body = BoxLayout(orientation="vertical", spacing=dp(10), padding=dp(12), size_hint=(1, None))
        body.bind(minimum_height=lambda b, h: setattr(b, "height", h))

        lbl = Label(text=line, markup=True, font_size=sp(20),
                    size_hint=(1, None), color=self._fg())
        lbl.bind(width=lambda l, w: setattr(l, "text_size", (w, None)),
                 texture_size=lambda l, ts: setattr(l, "height", ts[1]))
        body.add_widget(lbl)

        MenuPopup("Tinkered!", body=body,
                  actions=[("OK", None)],
                  size_hint=(0.7, None),
                  theme_mgr=self.tm).open()

        if self.game and getattr(self.game, "event_manager", None):
            try:
                payload_id = base_item_id or (self._normalize(self.base_item) if isinstance(self.base_item, str) else None)
                self.game.event_manager.player_action("tinkering_craft", item_id=payload_id)
            except Exception:
                pass

        self._reset_bench()

    def _grant_item_to_player(self, item_name: str, qty: int = 1):
        game = self.game or App.get_running_app()
        inv = getattr(game, "inventory", None)
        if inv is None:
            if not hasattr(game, "inventory"):
                game.inventory = {}
            inv = game.inventory

        inv[item_name] = inv.get(item_name, 0) + qty

        # nudge any known hooks
        for hook in ("on_inventory_changed", "update_inventory_display", "refresh_inventory"):
            if hasattr(game, hook):
                try:
                    getattr(game, hook)()
                except TypeError:
                    getattr(game, hook)(None)
                break

    def _reset_bench(self):
        self.base_item = None
        self.components = [None, None]
        self.active_recipe = None

        if self.base_item_slot:
            self._refresh_slot_visual(self.base_item_slot, None)
        if self.component_slot_1:
            self._refresh_slot_visual(self.component_slot_1, None)
        if self.component_slot_2:
            self._refresh_slot_visual(self.component_slot_2, None)

        self.result_label.text = "[i]Add a base item…[/i]"
        self._rebuild_requirements([])
        self._set_tinker_enabled(False)

    # ───────────────────────────────────────────────────────── recipes
    def _load_recipes(self):
        # Prefer /data/recipes_tinkering.json; allow legacy fallback
        data_path = os.path.join(App.get_running_app().directory, "data", "recipes_tinkering.json")
        legacy_path = os.path.join(App.get_running_app().directory, "recipes_tinkering.json")

        for path in (data_path, legacy_path):
            try:
                with open(path, "r", encoding="utf-8") as fp:
                    recipes = json.load(fp)
                    print(f"[Tinkering] Loaded {len(recipes)} recipes from {os.path.abspath(path)}")
                    return recipes
            except FileNotFoundError:
                continue
            except Exception as e:
                print(f"[Tinkering] Error loading {os.path.abspath(path)}: {e}")

        print("[Tinkering] No recipes loaded. Expect: /data/recipes_tinkering.json")
        return []

    # ───────────────────────────────────────────────────────── schematics book
    def _open_recipe_book(self, *_):
        """
        Themed schematics browser:
        - Lists all *learned* recipes by name.
        - Tap a recipe to attempt Auto-Fill.
        - Shows a minimal detail line (base + components)
        """
        # --- MODIFIED: Filter recipes by what the player has learned ---
        learned_ids = getattr(self.game, "learned_schematics", set())
        known_recipes = [r for r in self.recipes if r.get("id") in learned_ids]

        if not known_recipes:
            self.message_label.text = "You haven't learned any schematics yet."
            return

        # --- END MODIFIED ---

        body = BoxLayout(orientation="vertical", spacing=dp(10), padding=dp(10), size_hint=(1, None))
        body.bind(minimum_height=lambda b, h: setattr(b, "height", h))

        grid = GridLayout(cols=1, spacing=dp(6), size_hint_y=None)
        grid.bind(minimum_height=grid.setter("height"))

        # --- MODIFIED: Iterate over known_recipes ---
        for r in known_recipes:
            title = self._output_name(r)
            base = r.get("base", "?")
            comps = ", ".join(r.get("components", []) or [])
            line = f"[b]{title}[/b]\n[color=aaaaaa]{base} + {comps}[/color]"

            # --- MODIFIED RECIPE BUTTON ---
            btn = Button(
                text=line, markup=True,
                size_hint_y=None, height=dp(64),
                halign="left", valign="middle",
                font_name=fonts["popup_button"]["name"],
                font_size=sp(16), # Slightly smaller for multiline text
                background_normal="", background_color=self._fg(),
                color=self._bg()
            )
            btn.bind(size=lambda b, *_: setattr(b, "text_size", (b.size[0] - dp(20), b.size[1]))) # Add padding to text_size
            btn.bind(on_release=lambda _b, _r=r: (self._auto_fill_from_recipe(_r), popup.dismiss()))
            grid.add_widget(btn)

        sv = ScrollView(size_hint=(1, None), height=dp(360), do_scroll_x=False, bar_width=dp(4))
        sv.add_widget(grid)
        body.add_widget(sv)

        popup = MenuPopup("Schematics", body=body, actions=[],
                          size_hint=(0.92, None), theme_mgr=self.tm, autoclose=False)
        popup.open()

    def _auto_fill_from_recipe(self, recipe: dict):
        """
        Fill slots with recipe's base & components if inventory allows.
        Otherwise, populate anyway but disable Tinker (the checklist will show what's missing).
        """
        base = recipe.get("base")
        comps = list(recipe.get("components", []) or [])
        # set values
        self.base_item = base
        # pad to two comps
        while len(comps) < 2:
            comps.append(None)
        self.components = comps[:2]

        # refresh visuals
        self._refresh_slot_visual(self.base_item_slot, self.base_item)
        self._refresh_slot_visual(self.component_slot_1, self.components[0])
        self._refresh_slot_visual(self.component_slot_2, self.components[1])
        self._update_preview()  # will also enable/disable Tinker

    def _auto_fill_from_best(self, *_):
        """
        Try to find a craftable recipe using current selections as hints:
        - If base is set: choose a recipe for that base that you can fully craft from inventory.
        - Else: pick any recipe that is fully craftable.
        """
        inv = getattr(self.game, "inventory", {}) or {}

        def craftable(r: dict) -> bool:
            base = r.get("base")
            comps = r.get("components", []) or []
            need = [base] + comps
            return all(inv.get(n, 0) >= c for n, c in Counter(need).items())

        candidates = [r for r in self.recipes if craftable(r)]
        if self.base_item:
            candidates = [r for r in candidates if self._normalize(r.get("base")) == self._normalize(self.base_item)]

        if not candidates:
            self.message_label.text = "[i]No craftable recipe found with current inventory.[/i]"
            return

        # simple pick: first candidate
        self._auto_fill_from_recipe(candidates[0])
