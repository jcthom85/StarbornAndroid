# ui/fishing_screen.py
from __future__ import annotations

import random
from functools import partial
from font_manager import fonts
from kivy.app import App
from kivy.clock import Clock
from kivy.metrics import dp, sp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.anchorlayout import AnchorLayout
from kivy.uix.label import Label
from kivy.uix.screenmanager import Screen, FadeTransition
from kivy.uix.scrollview import ScrollView
from kivy.uix.gridlayout import GridLayout
from kivy.uix.button import Button
from kivy.animation import Animation
from kivy.core.window import Window
from kivy.uix.behaviors import ButtonBehavior
from kivy.graphics import Color, Rectangle
from kivy.uix.widget import Widget
from ui.menu_overlay import MenuOverlay
from ui.menu_popup import MenuPopup
from ui.fishing_minigame import ReelMinigame

# optional haptics
try:
    from plyer import vibrator as _vibrator
except Exception:
    _vibrator = None


class FishingScreen(Screen):
    """
    Blur/vignette → setup popup (rod+lure) → wait popup → FULL-SCREEN MINIGAME LAYER →
    result popup → return to Explore.
    """

    def __init__(self, zone_id: str, **kwargs):
        name = kwargs.pop("name", "fishing")
        super().__init__(name=name, **kwargs)
        self.zone_id = zone_id
        self.game    = None
        self._overlay = None

        self._setup_popup: MenuPopup | None = None
        self._wait_popup:  MenuPopup | None = None
        self._reel_layer:  FloatLayout | None = None  # full-screen container (NOT a popup)

        self.selected_rod_power: float | None = None
        self.selected_lure_bonus: int = 0

    # ——— lifecycle ————————————————————————————————————————————————
    def on_pre_enter(self, *_):
        self.game = App.get_running_app().current_game
        app = App.get_running_app()

        tex = getattr(app, "last_room_tex", None)
        self._overlay = MenuOverlay(
            default_tab=None,
            title="Let's Fish!",
            background_tex=tex,
            show_background=False,
        )
        self._overlay.bind(on_dismiss=lambda *_: self._return_to_explore())
        Window.add_widget(self._overlay)

        Clock.schedule_once(lambda *_: self._open_setup_popup(), 0.05)

    def on_leave(self, *_):
        if self._reel_layer and self._reel_layer.parent:
            try: Window.remove_widget(self._reel_layer)
            except Exception: pass
        self._reel_layer = None

        if self._overlay:
            try: Window.remove_widget(self._overlay)
            except Exception: pass
        self._overlay = None

        App.get_running_app().tx_mgr.close()

    # ——— Theme helpers ——————————————————————————————————————————————
    def _room_themes(self):
        """Return a fresh ThemeManager locked to the current room's env."""
        from theme_manager import ThemeManager  # local import to avoid cycles
        tm = ThemeManager()
        try:
            room   = getattr(self.game, "current_room", None)
            hub_id = self.game.world_manager.current_hub_id
            hub    = self.game.world_manager.hubs.get(hub_id, {}) if hub_id else {}
            env    = getattr(room, "env", None) or hub.get("theme", "default")
            tm.use(env)
        except Exception:
            # worst case, stays on default from file
            pass
        return tm

    # ——— Setup (rod + lure) ————————————————————————————————————————
    def _open_setup_popup(self):
        rods = self._get_rods()
        if not rods:
            self._toast("You have no fishing rod!")
            self._return_to_explore()
            return

        lures = self._get_lures()
        themes = self._room_themes()
        fg = themes.col("fg") if themes else (1,1,1,1)
        bg = themes.col("bg") if themes else (0,0,0,1)

        body = BoxLayout(orientation="vertical", spacing=dp(12), padding=dp(12), size_hint=(1, None))
        body.bind(minimum_height=lambda b, h: setattr(b, "height", h))

        body.add_widget(Label(
            text="[b]Choose Rod[/b]",
            markup=True,
            font_name=fonts["popup_title"]["name"],
            font_size=fonts["popup_title"]["size"],
            size_hint_y=None, height=dp(28), color=fg
        ))
        
        # --- MODIFIED: Pass structured data instead of a formatted string ---
        body.add_widget(self._make_picker_grid(
            entries=[((name, f"(Power {p})"), partial(self._select_rod, p))
                     for name, p in rods],
            entry_colors={'bonus_text': (0xb0/255, 0xff/255, 0xff/255, 1)}, # Pass color for rod bonus
            min_height=dp(120),
            themes=themes
        ))

        body.add_widget(Label(
            text="[b]Choose Lure[/b]",
            markup=True,
            font_name=fonts["popup_title"]["name"],
            font_size=fonts["popup_title"]["size"],
            size_hint_y=None, height=dp(28), color=fg
        ))
        if lures:
            # --- MODIFIED: Pass structured data for lures as well ---
            lure_entries = [((name, f"(+{b}% rarity)"), partial(self._select_lure, b)) for name, b in lures]
            lure_entries.append((("— Fish without lure —", ""), partial(self._select_lure, 0))) # Handle special case

            body.add_widget(self._make_picker_grid(
                entries=lure_entries,
                entry_colors={'bonus_text': (0xc0/255, 0xff/255, 0xc0/255, 1)}, # Pass color for lure bonus
                min_height=dp(120),
                themes=themes
            ))
        else:
            body.add_widget(Label(text="[i](No lures found — you can still fish.)[/i]",
                                  markup=True, size_hint_y=None, height=dp(24), color=fg))

        self._setup_msg = Label(
            text="", markup=True,
            font_name=fonts["small_text"]["name"],
            font_size=fonts["small_text"]["size"],
            size_hint_y=None, height=dp(24), color=fg
        )

        body.add_widget(self._setup_msg)

        actions = [("Cast Line", self._on_cast_line), ("Cancel", self._return_to_explore)]

        self._setup_popup = MenuPopup("Fishing Setup", body=body, actions=actions,
                                      autoclose=False, size_hint=(0.9, None),
                                      theme_mgr=themes)
        try:
            self.game._style_popup(self._setup_popup)
        except Exception:
            pass
        self._setup_popup.open()

    def _make_picker_grid(self, entries: list[tuple[any, callable]], *, min_height: float, themes, entry_colors: dict):
        # A clickable row that allows us to manage our own background color
        class PickerRow(ButtonBehavior, BoxLayout):
            def __init__(self, bg_color=(0,0,0,1), **kwargs):
                super().__init__(**kwargs)
                with self.canvas.before:
                    Color(rgba=bg_color)
                    self.rect = Rectangle(pos=self.pos, size=self.size)
                self.bind(pos=self._update_rect, size=self._update_rect)

            def _update_rect(self, *args):
                self.rect.pos = self.pos
                self.rect.size = self.size

        # Theme-derived colors for inner buttons
        btn_bg = themes.col("fg") if themes else (.22, .22, .25, 1)
        btn_fg = themes.col("bg") if themes else (0, 0, 0, 1)
        bonus_text_color = entry_colors.get('bonus_text', btn_fg)

        grid = GridLayout(cols=1, spacing=dp(6), size_hint_y=None)
        grid.bind(minimum_height=grid.setter("height"))

        for entry_data, cb in entries:
            main_text, bonus_text = entry_data

            row = PickerRow(bg_color=btn_bg, size_hint_y=None, height=dp(44),
                            spacing=dp(8), padding=(dp(12), 0, dp(12), 0))

            # Label for the main item name
            main_label = Label(
                text=main_text,
                color=btn_fg,
                font_name=fonts["popup_button"]["name"],
                font_size=fonts["popup_button"]["size"],
                halign='left', valign='middle',
                size_hint_x=None,
            )
            main_label.bind(texture_size=main_label.setter('size'))

            # Label for the bonus text (Power, Rarity, etc.)
            bonus_label = Label(
                text=bonus_text,
                color=bonus_text_color,
                font_name=fonts["popup_button"]["name"],
                font_size=fonts["popup_button"]["size"],
                outline_color=(0, 0, 0),
                outline_width=dp(1.5), # The crucial outline
                halign='right', valign='middle',
            )
            
            row.add_widget(main_label)
            row.add_widget(Widget()) # Spacer to push bonus_label to the right
            row.add_widget(bonus_label)

            row.bind(on_release=lambda _b, _cb=cb: _cb())
            grid.add_widget(row)

        sv = ScrollView(size_hint=(1, None), height=max(min_height, min(grid.height, dp(160))), bar_width=dp(5))
        sv.add_widget(grid)
        return sv

    def _select_rod(self, power: float):
        self.selected_rod_power = float(power)
        self._setup_msg.text = f"[color=80ffdf]Rod selected (Power {power}).[/color]"

    def _select_lure(self, bonus: int):
        self.selected_lure_bonus = int(bonus)
        self._setup_msg.text = f"[color=c0ffc0]Lure selected (+{bonus}% rarity).[/color]" if bonus > 0 else "[color=a0a0a0]No lure.[/color]"

    def _on_cast_line(self):
        if self.selected_rod_power is None:
            self._setup_msg.text = "[color=ff9a9a]Pick a rod first.[/color]"
            return
        if self._setup_popup:
            self._setup_popup.dismiss()
            self._setup_popup = None
        self._start_wait_for_bite()

    # ——— Wait (themed MenuPopup) ————————————————————————————————
    def _start_wait_for_bite(self):
        themes = self._room_themes()
        fg = themes.col("fg") if themes else (1,1,1,1)

        box = BoxLayout(orientation="vertical", spacing=dp(10), padding=dp(16), size_hint=(1, None))
        box.bind(minimum_height=lambda b,h: setattr(b, "height", h))
        self._wait_lbl = Label(
            text="[i]Casting…[/i]", markup=True,
            font_name=fonts["medium_text"]["name"],
            font_size=fonts["medium_text"]["size"],
            size_hint=(1,None), color=fg
        )
        self._wait_lbl.bind(width=lambda l,w: setattr(l, "text_size", (w, None)),
                            texture_size=lambda l,ts: setattr(l, "height", ts[1]))
        hint = Label(
            text="[i]Wait for the tug…[/i]", markup=True,
            font_name=fonts["small_text"]["name"],
            font_size=fonts["small_text"]["size"],
            size_hint=(1,None), color=fg
        )
        hint.bind(width=lambda l,w: setattr(l, "text_size", (w, None)),
                  texture_size=lambda l,ts: setattr(l, "height", ts[1]))
        box.add_widget(self._wait_lbl)
        box.add_widget(hint)

        self._wait_popup = MenuPopup("Let's Fish!", body=box, actions=[],
                                     autoclose=False, size_hint=(0.85, None),
                                     theme_mgr=themes)
        try:
            self.game._style_popup(self._wait_popup)
        except Exception:
            pass
        self._wait_popup.open()

        delay = random.uniform(0.9, 2.2)
        Clock.schedule_once(self._bite_cue, delay)

        def _pulse(_dt):
            if not self._wait_popup or not self._wait_popup.parent:
                return False
            t = (Clock.get_time() * 2.0) % 3
            dots = "." * (int(t)+1)
            self._wait_lbl.text = f"[i]Casting{dots}[/i]"
            return True
        Clock.schedule_interval(_pulse, 0.25)

    def _bite_cue(self, *_):
        self._buzz()
        self._wait_lbl.text = "[b]Fish on![/b]"
        anim = Animation(font_size=self._wait_lbl.font_size * 1.1, d=0.06) + \
               Animation(font_size=self._wait_lbl.font_size, d=0.06)
        anim.start(self._wait_lbl)
        Clock.schedule_once(lambda *_: self._start_reel_minigame(), 0.25)

    def _buzz(self):
        app = App.get_running_app()
        if not getattr(app, "settings", {}).get("haptics", True):
            return
        if _vibrator:
            try:
                _vibrator.vibrate(time=0.06)
                return
            except Exception:
                pass
        try:
            self.game.audio.play_sfx("tap")
        except Exception:
            pass

    # ——— FULL-SCREEN MINIGAME LAYER (NOT a popup) ————————————————
    def _start_reel_minigame(self):
        # close the wait popup if it's still up
        if getattr(self, "_wait_popup", None):
            try:
                self._wait_popup.dismiss()
            except Exception:
                pass
            self._wait_popup = None

        # difficulty by rod power
        rod = self.selected_rod_power or 1.0
        if rod >= 3.0:
            diff, pattern = "easy", "constant"
        elif rod >= 2.0:
            diff, pattern = "medium", "wobble"
        else:
            diff, pattern = "hard", "erratic"

        # create the minigame widget
        mg = ReelMinigame(difficulty=diff, pattern=pattern)

        # theme the minigame to match the current room
        try:
            themes = self._room_themes() if hasattr(self, "_room_themes") else getattr(App.get_running_app(), "themes", None)
            if themes:
                # pull colors
                fg = list(themes.col("fg"))
                bg = list(themes.col("bg"))
                bd = list(themes.col("border"))

                # ensure alpha for bg
                if len(bg) == 3:
                    bg.append(0.95)
                else:
                    bg[3] = 0.95

                # apply to widget
                mg.theme_bg     = bg
                mg.theme_border = bd
                mg.theme_fish   = fg
                mg.theme_progress = [fg[0], fg[1], fg[2], 0.85]

                # use border tint for the capture band (soft + bright line)
                # quick brighten for the mid stripe
                def _brighten(c, f=0.35):
                    return [min(1.0, c[0] + (1-c[0])*f),
                            min(1.0, c[1] + (1-c[1])*f),
                            min(1.0, c[2] + (1-c[2])*f),
                            1.0]
                band = [bd[0], bd[1], bd[2], 0.22]
                mg.theme_zone     = band
                mg.theme_zone_mid = _brighten(bd, 0.45)
        except Exception:
            pass

        # build a full-screen layer above the overlay for the minigame
        layer = FloatLayout(size=Window.size)

        # top instructions (no clipping; themed text)
        try:
            fg = themes.col("fg") if themes else (1, 1, 1, 1)
        except Exception:
            fg = (1, 1, 1, 1)

        top = BoxLayout(
            orientation="vertical",
            size_hint=(1, None),
            height=dp(72),
            padding=(dp(16), dp(12), dp(16), dp(0)),
        )
        # --- MODIFIED: Added outline to instruction label ---
        instr = Label(
            text="[i]Keep the fish in the bright middle band. Tap to pull it down.[/i]",
            markup=True,
            font_size=sp(18),
            size_hint=(1, 1),
            halign="center",
            valign="middle",
            color=fg,
            outline_color=(0, 0, 0),
            outline_width=dp(1.5),
        )
        instr.bind(size=lambda l, *_: setattr(l, "text_size", (l.width, None)))
        top.add_widget(instr)

        # center the minigame widget
        center = AnchorLayout(anchor_x="center", anchor_y="center")
        mg.size_hint = (None, None)
        mg.width = dp(160)
        mg.height = int(Window.height * 0.68)
        center.add_widget(mg)

        # assemble layer
        layer.add_widget(center)
        layer.add_widget(top)

        # fade-in and attach
        layer.opacity = 0.0
        Window.add_widget(layer)  # sits above MenuOverlay
        Animation(opacity=1.0, d=0.12).start(layer)
        self._reel_layer = layer

        # result handler → finish
        mg.bind(on_result=self._finish_fishing)
        mg.start(rod_power=rod)

    # ——— Finish → result popup (themed) ——————————————————————————————
    def _finish_fishing(self, _minigame: ReelMinigame, result: str):
        # remove layer
        if self._reel_layer and self._reel_layer.parent:
            try:
                Window.remove_widget(self._reel_layer)
            except Exception:
                pass
        self._reel_layer = None

        catch_name = None
        if result == "success":
            try:
                fm = getattr(self.game, "fishing_manager", None)
                if fm and hasattr(fm, "get_catch"):
                    catch = fm.get_catch(
                        self.zone_id, "success",
                        self.selected_rod_power or 1.0,
                        self.selected_lure_bonus or 0
                    )
                    if catch:
                        catch_name = getattr(catch, "name", str(catch))
                        self.game.inventory[catch_name] = self.game.inventory.get(catch_name, 0) + 1
                        try: self.game.update_inventory_display()
                        except Exception: pass
            except Exception:
                pass

        themes = self._room_themes()
        if result == "success":
            line = f"You caught a [b]{catch_name or 'Fish'}[/b]!"
            body = self._body_label(line, themes)
            title = "Nice job!"
        else:
            body = self._body_label("It got away!", themes)
            title = "Uh oh…"

        popup = MenuPopup(title, body=body,
                          actions=[("OK", self._return_to_explore)],
                          size_hint=(0.75, None), theme_mgr=themes)
        try:
            self.game._style_popup(popup)
        except Exception:
            pass
        popup.open()

    # ——— helpers ————————————————————————————————————————————————
    def _body_label(self, text: str, themes) -> BoxLayout:
        fg = themes.col("fg") if themes else (1,1,1,1)
        box = BoxLayout(orientation="vertical", spacing=dp(10), padding=dp(12),
                        size_hint=(1, None))
        box.bind(minimum_height=lambda b,h: setattr(b, "height", h))
        lbl = Label(text=text, markup=True, font_size=sp(20), halign="center", valign="middle",
                    size_hint=(1, None), color=fg)
        lbl.bind(width=lambda l,w: setattr(l, "text_size", (w, None)),
                 texture_size=lambda l,ts: setattr(l, "height", ts[1]))
        box.add_widget(lbl)
        return box

    def _get_rods(self) -> list[tuple[str, float]]:
        rods = []
        for item_name in getattr(self.game, "inventory", {}).keys():
            item = self.game.all_items.find(item_name)
            if item and getattr(item, "type", "") == "fishing_rod":
                rods.append((item.name, float(getattr(item, "fishing_power", 1))))
        return sorted(rods, key=lambda t: t[1])

    def _get_lures(self) -> list[tuple[str, int]]:
        lures = []
        for item_name in getattr(self.game, "inventory", {}).keys():
            item = self.game.all_items.find(item_name)
            if item and getattr(item, "type", "") == "lure":
                lures.append((item.name, int(getattr(item, "rarity_bonus", 0))))
        return sorted(lures, key=lambda t: t[1], reverse=True)

    def _toast(self, msg: str):
        try:
            self.game.narrate(msg)
        except Exception:
            pass

    def _return_to_explore(self, *_):
        # Dismiss any active popups this screen owns before leaving
        if self._setup_popup:
            self._setup_popup.dismiss()
            self._setup_popup = None
        if self._wait_popup:
            self._wait_popup.dismiss()
            self._wait_popup = None

        App.get_running_app().tx_mgr.close()
        sm = self.manager or App.get_running_app().screen_manager
        if not sm:
            return
        old = sm.transition
        sm.transition = FadeTransition(duration=1.0)
        sm.current = "explore"
        Clock.schedule_once(lambda *_: setattr(sm, "transition", old), 0)
        Clock.schedule_once(lambda *_: sm.remove_widget(self), 0.1)
        