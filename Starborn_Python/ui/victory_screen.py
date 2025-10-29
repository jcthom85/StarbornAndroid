# ui/victory_screen.py
from __future__ import annotations
from functools import lru_cache
import random

from kivy.app import App
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.metrics import dp, sp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.image import Image
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView
from kivy.uix.widget import Widget

from ui.menu_popup import MenuPopup
from particles import ParticleEmitter
from font_manager import fonts


class VictoryScreen(MenuPopup):
    """
    Two sequential popups:
      - mode='spoils'  : spoils list (+ Continue / Take All)
      - mode='levelup' : level-up + new skills (+ Done)

    Fireworks render on the main Window, not inside the popup.
    """

    def __init__(self, victory_data: dict, mode: str, on_close=None, **kwargs):
        game = getattr(App.get_running_app(), "current_game", None)
        theme_mgr = kwargs.pop("theme_mgr", getattr(game, "themes", None))
        self._close_callback = on_close
        self._close_invoked = False

        if mode == "spoils":
            title = "Victory!"
            has_levelups = victory_data.get("level_ups") or victory_data.get("new_skills")
            button_text = "Continue" if has_levelups else "Take All"
            raw_body = self._build_spoils_body(victory_data, theme_mgr)
        elif mode == "levelup":
            title = "Level Up!"
            button_text = "Done"
            raw_body = self._build_level_up_body(victory_data, theme_mgr)
        else:
            title, button_text = "Error", "Close"
            raw_body = Label(text="Invalid popup mode")

        body_with_fx = raw_body

        super().__init__(
            title=title,
            body=body_with_fx,
            actions=[(button_text, self._handle_primary_action)],
            size_hint=(0.86, None), # size_hint_y=None is crucial for dynamic height
            dismiss_on_touch_outside=True,
            theme_mgr=theme_mgr,
            autoclose=True,
            **kwargs,
        )
        self.bind(on_dismiss=lambda *_: self._trigger_on_close())

        # ensure transparent Kivy default bg is off
        Clock.schedule_once(lambda *_: setattr(self, "background", ""))
        Clock.schedule_once(lambda *_: setattr(self, "background_color", (0, 0, 0, 0)))

        if mode == "levelup":
            Clock.schedule_once(lambda *_: self._trigger_fireworks())

    def _handle_primary_action(self, *_):
        self._trigger_on_close()

    def _trigger_on_close(self):
        if self._close_invoked:
            return
        self._close_invoked = True
        if self._close_callback:
            try:
                self._close_callback()
            except Exception:
                pass

    def _dismiss_via_tap(self):
        if self._close_invoked:
            return
        self.dismiss()
        self._trigger_on_close()

    def on_touch_down(self, touch):
        if self.collide_point(*touch.pos):
            touch.ud["victory_origin"] = touch.pos
        return super().on_touch_down(touch)

    def on_touch_up(self, touch):
        origin = touch.ud.get("victory_origin")
        handled = super().on_touch_up(touch)
        if self._close_invoked:
            return handled
        if origin and self.collide_point(*touch.pos):
            dx = abs(touch.x - origin[0])
            dy = abs(touch.y - origin[1])
            if dx <= dp(10) and dy <= dp(10):
                self._dismiss_via_tap()
                return True
        return handled

    def _build_spoils_body(self, victory_data, tm):
        spoils = victory_data.get("spoils", [])
        fg = tm.col("fg") if tm else (1, 1, 1, 1)
        dim = (0.78, 0.78, 0.78, 1)

        container = BoxLayout(
            orientation="vertical",
            spacing=dp(10),
            padding=[dp(16), dp(10), dp(16), dp(10)],
            size_hint_y=None,
        )
        container.bind(minimum_height=container.setter("height"))

        meta_first = [s for s in spoils if s.get("name", "").lower() in ("experience", "credits")]
        loot_items = [s for s in spoils if s not in meta_first]
        for spoil in meta_first + loot_items:
            container.add_widget(self._row_for_spoil(spoil, tm, fg, dim))

        EXTRA_PADDING = dp(140)
        total_h = container.minimum_height + EXTRA_PADDING
        max_h = Window.height * 0.85

        if total_h > max_h:
            sv = ScrollView(do_scroll_x=False, do_scroll_y=True, bar_width=dp(5), size_hint=(1, None), height=max_h)
            # --- FIX: Only add the widget to the ScrollView if we are returning the ScrollView ---
            sv.add_widget(container)
            return sv
        else:
            return container

    def _build_level_up_body(self, victory_data, tm):
        fg = tm.col("fg") if tm else (1, 1, 1, 1)
        box = BoxLayout(
            orientation="vertical",
            spacing=dp(12),
            padding=[dp(16), dp(8), dp(16), dp(8)],
            size_hint_y=None,
        )
        box.bind(minimum_height=box.setter("height"))

        def _line(text, fsize):
            lbl = Label(
                text=text,
                markup=True,
                color=fg,
                font_name=fonts["medium_text"]["name"],
                font_size=fsize,
                size_hint_y=None,
                halign="center",
                valign="middle",
            )
            lbl.bind(
                size=lambda l, *_: setattr(l, "text_size", (l.width, None)),
                texture_size=lambda l, ts: setattr(l, "height", ts[1]),
            )
            box.add_widget(lbl)

        level_ups  = victory_data.get("level_ups", []) or []
        new_skills = victory_data.get("new_skills", []) or []

        if not level_ups and not new_skills:
            _line("[i]No level ups on this victory.[/i]", sp(16))
        else:
            for lu in level_ups:
                _line(f"[b]{lu['character']}[/b] reached Level [b]{lu['new_level']}![/b]", sp(18))
            if new_skills:
                 box.add_widget(Widget(size_hint_y=None, height=dp(10)))
            for sk in new_skills:
                _line(f"{sk['character']} learned [b]{sk['skill_name']}![/b]", sp(17))

        EXTRA_PADDING = dp(140)
        total_h = box.minimum_height + EXTRA_PADDING
        max_h = Window.height * 0.85

        if total_h > max_h:
            sv = ScrollView(do_scroll_x=False, do_scroll_y=True, bar_width=dp(5), size_hint=(1, None), height=max_h)
            # --- FIX: Only add the widget to the ScrollView if we are returning the ScrollView ---
            sv.add_widget(box)
            return sv
        else:
            return box

    def _trigger_fireworks(self):
        if self.width <= 1:
            Clock.schedule_once(lambda *_: self._trigger_fireworks(), 0.05)
            return

        for _ in range(4):
            emitter = ParticleEmitter()
            # This line adds the firework to the main window, which is a separate layer on top of all UI.
            # This is exactly the architecture you wanted!
            Window.add_widget(emitter)
            popup_x, popup_y = self.pos
            rx = popup_x + self.width  * random.uniform(0.15, 0.85)
            ry = popup_y + self.height * random.uniform(0.25, 0.75)
            color = [random.uniform(0.5, 1.0) for _ in range(3)]
            cfg = {
                "count": 60, "life": [0.8, 1.5], "size": [dp(3), dp(7)],
                "pos": [rx, ry], "velocity": [dp(100), dp(200)],
                "angle": [0, 360], "gravity": [0, -dp(250)], "color": color,
                "one_shot": True,
            }
            emitter.start(cfg)
            Clock.schedule_once(lambda _dt, w=emitter: self._remove_fx(w), 2.0)

    def _remove_fx(self, w):
        if w and w.parent:
            try:
                w.parent.remove_widget(w)
            except Exception:
                pass

    # Method stubs are included for completeness, no changes needed below this line
    def _wrap_with_fx_layer(self, body_widget, popup_height: float):
        pass # This function is no longer used but can be kept for reference

    def _row_for_spoil(self, s: dict, tm, fg, dim) -> BoxLayout:
        name   = s.get("name", "").strip() or "Unknown"
        qty    = s.get("quantity")
        rarity = s.get("rarity")
        flavor = s.get("flavor_text")
        wrapper = BoxLayout(orientation="vertical", spacing=dp(4), size_hint_y=None)
        wrapper.bind(minimum_height=wrapper.setter("height"))
        top = BoxLayout(orientation="horizontal", spacing=dp(10), size_hint_y=None, height=dp(44))
        if name.lower() not in ("experience", "credits"):
            icon = Image(
                source=self._icon_for(name),
                allow_stretch=True, keep_ratio=True,
                size_hint=(None, None), size=(dp(40), dp(40)),
                color=(1, 1, 1, 1),
            )
            top.add_widget(icon)
        name_lbl = Label(
            text=f"[b]{name}[/b]" if name else "",
            markup=True, color=fg,
            font_name=fonts["medium_text"]["name"],
            size_hint=(1, 1), font_size=sp(18),
            halign="left", valign="middle",
        )
        name_lbl.bind(size=lambda l, *_: setattr(l, "text_size", (l.width, None)))
        top.add_widget(name_lbl)
        bits = []
        if qty not in (None, ""): bits.append(f"x{qty}")
        if rarity: bits.append(f"[i]{str(rarity).title()}[/i]")
        right_lbl = Label(
            text="  ".join(bits), markup=True, color=fg, font_name=fonts["small_text"]["name"], font_size=sp(16),
            size_hint=(None, 1), width=dp(120), halign="right", valign="middle",
        )
        right_lbl.bind(size=lambda l, *_: setattr(l, "text_size", (l.width, None)))
        top.add_widget(right_lbl)
        wrapper.add_widget(top)
        if flavor:
            fl = Label(
                text=f"[color=bbbbbb][i]{flavor}[/i][/color]",
                markup=True,
                font_name=fonts["small_text"]["name"],
                font_size=sp(15), size_hint=(1, None),
                halign="left", valign="top",
            )
            fl.bind(
                size=lambda l, *_: setattr(l, "text_size", (l.width, None)),
                texture_size=lambda l, ts: setattr(l, "height", ts[1]),
            )
            wrapper.add_widget(fl)
        return wrapper

    @lru_cache(maxsize=256)
    def _icon_for(self, item_name: str) -> str:
        game = getattr(App.get_running_app(), "current_game", None)
        if not game or not hasattr(game, "all_items"):
            return "images/ui/item_icon_generic.png"
        item = game.all_items.find(item_name)
        if not item: return "images/ui/item_icon_generic.png"
        icon_path = "images/ui/item_icon_generic.png"
        item_type = getattr(item, "type", "junk")
        name_lc = item.name.lower()
        if item_type == "consumable":
            icon_path = "images/ui/item_icon_consumable.png"
            if any(k in name_lc for k in ("stew", "salad", "ramen", "fish", "meat")):
                icon_path = "images/ui/item_icon_food.png"
        elif item_type == "ingredient":
            if getattr(item, "subtype", None) == "fish":
                icon_path = "images/ui/item_icon_fish.png"
            else:
                icon_path = "images/ui/item_icon_ingredient.png"
                if any(k in name_lc for k in ("scrap", "wiring", "lens", "weave")):
                    icon_path = "images/ui/item_icon_material.png"
        elif item_type == "fishing_rod":
            icon_path = "images/ui/item_icon_fishing.png"
        elif item_type == "equippable":
            slot = getattr(item, "equipment", {}).get("slot", "")
            if slot == "armor":
                icon_path = "images/ui/item_icon_armor.png"
                if "gloves" in name_lc: icon_path = "images/ui/item_icon_gloves.png"
                elif "pendant" in name_lc: icon_path = "images/ui/item_icon_pendant.png"
            elif slot == "accessory":
                icon_path = "images/ui/item_icon_accessory.png"
            elif slot == "weapon":
                icon_path = "images/ui/item_icon_sword.png"
                if any(k in name_lc for k in ("gun", "pistol", "rifle")):
                    icon_path = "images/ui/item_icon_gun.png"
        return getattr(item, "icon", icon_path)
