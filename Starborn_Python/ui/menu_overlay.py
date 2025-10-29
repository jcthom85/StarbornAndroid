# ui/menu_overlay.py — v3.5 “Starborn Card Overlay”
# Changes vs 3.4:
# - Removed the top header bar and its rounded background entirely.
# - Close (✕) now floats at the card’s top-right corner without taking layout height.
# - Screen title is CENTERED directly under the tab ribbon.
# - Everything else (dim blur bg, tabs, lazy-loaded screens, template mode,
#   touch routing) remains the same.

from __future__ import annotations

from kivy.animation import Animation
from kivy.app import App
from kivy.core.window import Window
from kivy.graphics import Color, RoundedRectangle, Line, Rectangle
from kivy.metrics import dp
from kivy.properties import StringProperty, ObjectProperty, BooleanProperty, NumericProperty
from kivy.uix.behaviors import ButtonBehavior
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.image import Image
from kivy.uix.label import Label
from kivy.uix.screenmanager import ScreenManager, Screen, NoTransition
from kivy.uix.scrollview import ScrollView
from kivy.clock import Clock
from kivy.resources import resource_find

from font_manager import fonts
from theme_manager import ThemeManager

# Background blur shader wrapper
from ui.fading_background import FadingBackground


# ───────────────────────────────────────────────────────── helpers
def _room_theme() -> ThemeManager:
    tm = ThemeManager()
    try:
        app  = App.get_running_app()
        game = getattr(app, "current_game", None)
        hub_id = game.world_manager.current_hub_id if (game and game.world_manager) else None
        hub    = game.world_manager.hubs.get(hub_id, {}) if (game and hub_id) else {}
        env    = getattr(getattr(game, "current_room", None), "env", None) or hub.get("theme", "default")
        tm.use(env)
    except Exception:
        pass
    return tm


# ───────────────────────────────────────────────────────── inline Settings
class _OverlaySettings(Screen):
    def __init__(self, **kw):
        super().__init__(**kw)
        self.app = App.get_running_app()
        self.tm  = _room_theme()
        fg, bg = self.tm.col("fg"), self.tm.col("bg")
        accent = self.tm.col("accent")

        root = BoxLayout(orientation="vertical", spacing=dp(10), size_hint_y=None,
                         padding=[dp(16), dp(10), dp(16), dp(10)], pos_hint={'top': 1})
        root.bind(minimum_height=root.setter('height'))

        root.add_widget(Label(text="[b]Accessibility[/b]", markup=True,
                              font_name=fonts["popup_title"]["name"],
                              font_size=fonts["popup_title"]["size"],
                              color=fg, size_hint_y=None, height=dp(30)))

        from kivy.uix.gridlayout import GridLayout
        from kivy.uix.switch import Switch
        grid = GridLayout(cols=2, spacing=dp(8), size_hint_y=None, height=dp(120))
        self._toggles = {}
        for key, text in [
            ('screenshake', 'Disable Screenshake'),
            ('flashes',     'Disable Color Flashes'),
            ('haptics',     'Disable Haptics'),
        ]:
            lbl = Label(text=text, halign="left", valign="middle",
                        font_name=fonts["medium_text"]["name"],
                        font_size=fonts["medium_text"]["size"],
                        color=fg, size_hint_y=None, height=dp(28))
            lbl.bind(size=lambda l, *_: setattr(l, "text_size", (l.width, None)))
            sw = Switch(active=self.app.settings.get(key, False))
            sw.bind(active=lambda inst, val, k=key: self._on_change(k, val))
            grid.add_widget(lbl); grid.add_widget(sw)
            self._toggles[key] = sw
        root.add_widget(grid)

        row = BoxLayout(orientation="horizontal", spacing=dp(10), size_hint_y=None, height=dp(44))
        btn_save = Button(text="Save Game", background_normal="",
                          background_color=fg, color=bg,
                          font_name=fonts["popup_button"]["name"],
                          font_size=fonts["popup_button"]["size"])
        btn_load = Button(text="Load Game", background_normal="",
                          background_color=fg, color=bg,
                          font_name=fonts["popup_button"]["name"],
                          font_size=fonts["popup_button"]["size"])
        btn_save.bind(on_release=lambda *_: getattr(self.app.current_game, "prompt_save_slot", lambda: None)())
        btn_load.bind(on_release=lambda *_: self.app.prompt_load_slot())
        row.add_widget(btn_save); row.add_widget(btn_load)
        root.add_widget(row)

        btn_dbg = Button(text="Open Debug Panel", size_hint_y=None, height=dp(44),
                         font_name=fonts["popup_button"]["name"],
                         font_size=fonts["popup_button"]["size"])
        def _open_debug(*_):
            sm = getattr(self.app, "screen_manager", None)
            if sm and sm.has_screen("debug_panel"):
                sm.current = "debug_panel"
        btn_dbg.bind(on_release=_open_debug)
        root.add_widget(btn_dbg)

        self.add_widget(root)

    def on_pre_enter(self, *a):
        for k, sw in self._toggles.items():
            sw.active = self.app.settings.get(k, False)

    def _on_change(self, key, val):
        try:
            self.app.settings[key] = val
            if hasattr(self.app, "save_settings"):
                self.app.save_settings()
        except Exception:
            pass


# ───────────────────────────────────────────────────────── Tab Chip
class _TabChip(ButtonBehavior, BoxLayout):
    """
    Pill-style tab. If an icon exists and we go compact → show icon-only.
    If the icon is missing → ALWAYS show a label (or an abbreviation in compact).
    """
    def __init__(self, tab_id: str, text: str, icon_source: str | None, on_select, *,
                 theme: ThemeManager, abbr: str | None = None, **kw):
        # --- MODIFIED: Adjust padding for a more balanced look ---
        super().__init__(orientation="horizontal", spacing=dp(8), padding=(dp(16), 0), **kw)
        self.tab_id = tab_id
        self._theme = theme
        self._on_select = on_select
        self.size_hint_y = None
        self.height = dp(42)
        self.size_hint_x = None
        self.full_text = text
        self.abbr_text = (abbr or (text[0].upper()))

        # icon (only if resource exists)
        # --- MODIFIED: Remove halign and size_hint to allow natural sizing ---
        self.lbl = Label(text=text, valign="middle",
                         font_name=fonts["popup_button"]["name"],
                         font_size=fonts["popup_button"]["size"],
                         color=self._theme.col("fg"))
        self.lbl.bind(texture_size=lambda l, ts: self._recalc_width())
        self.add_widget(self.lbl)

        # --- REMOVED: Icon is no longer used in this simplified layout ---
        self.icon = None

        # --- REMOVED: Width is now calculated dynamically ---

        # pill backplate
        with self.canvas.before:
            self._pill_col = Color(1, 1, 1, 0.10)
            self._pill = RoundedRectangle(radius=[dp(12)], pos=self.pos, size=self.size)
            br, bg, bb, _ = self._theme.col("border")
            self._stroke_col = Color(br, bg, bb, 0.35)
            self._stroke = Line(rounded_rectangle=(self.x, self.y, self.width, self.height, dp(12)),
                                width=dp(1.2))
        self.bind(pos=self._repaint, size=self._repaint)

    def _recalc_width(self):
        """Dynamically set the button's width based on its label and padding."""
        if self.lbl.texture_size[0] > 1:
            self.width = self.lbl.texture_size[0] + self.padding[0] * 2

    def _repaint(self, *_):
        self._pill.pos = self.pos
        self._pill.size = self.size
        self._stroke.rounded_rectangle = (self.x, self.y, self.width, self.height, dp(12))

    def on_release(self):
        if callable(self._on_select):
            self._on_select(self.tab_id)

    def set_active(self, active: bool):
        a = 0.24 if active else 0.10
        sa = 0.85 if active else 0.35
        self._pill_col.rgba = (1, 1, 1, a)
        br, bg, bb, _ = self._theme.col("border")
        self._stroke_col.rgba = (br, bg, bb, sa)


# ───────────────────────────────────────────────────────── Overlay
class MenuOverlay(FloatLayout):
    """
    Slide-up overlay hosting tabs or acting as a template frame.

    - default_tab: "inventory"|"journal"|"stats"|"map"|"settings" or None (template)
    - title: optional header override (displayed under the tab ribbon)
    - background_tex: optional live snapshot texture (blurred)
    - show_background: draw blurred bg behind card
    - dim_alpha: extra darkness over bg (0..1)
    """
    background_image = StringProperty("images/ui/starborn_menu_bg.png")  # legacy compat
    background_tex   = ObjectProperty(None, allownone=True)
    show_background  = BooleanProperty(True)
    dim_alpha        = NumericProperty(0.50)

    __events__ = ("on_dismiss",)

    def on_dismiss(self, *args): pass

    def set_title(self, text: str):
        if hasattr(self, "_title_label"):
            self._title_label.text = text

    def __init__(self, default_tab: str | None = "map", *,
                 title: str | None = None,
                 background_tex=None,
                 show_background: bool = True,
                 dim_alpha: float = 0.50,
                 **kwargs):
        super().__init__(**kwargs)
        # --- THIS IS THE FIX: Increment the modal counter on creation ---
        try:
            app = App.get_running_app()
            if not hasattr(app, '_modal_ref_count'):
                app._modal_ref_count = 0
            app._modal_ref_count += 1
        except Exception:
            pass
        self.size_hint = (1, 1)
        self.pos = (0, -Window.height)
        self.default_tab   = default_tab
        self.custom_title  = title
        self.background_tex = background_tex
        self.show_background = bool(show_background)
        self.dim_alpha = float(max(0.0, min(1.0, dim_alpha)))

        self.tm = _room_theme()
        fg, bg = self.tm.col("fg"), self.tm.col("bg")
        accent = self.tm.col("accent")

        # ---- blurred background + dim scrim
        if self.show_background:
            self._frost = FadingBackground(texture=self.background_tex,
                                           vignette_intensity=1.0,
                                           blur_intensity=1.0,
                                           size_hint=(1, 1))
            self.add_widget(self._frost)

            self._scrim = FloatLayout(size_hint=(1, 1))
            with self._scrim.canvas:
                self._scrim_col = Color(0, 0, 0, self.dim_alpha)
                self._scrim_rect = Rectangle(size=self.size, pos=self.pos)
            self.bind(pos=lambda *_: setattr(self._scrim_rect, "pos", self.pos),
                      size=lambda *_: setattr(self._scrim_rect, "size", self.size))
            self.add_widget(self._scrim)
        else:
            self._frost = None
            self._scrim = None

        # ---- Foreground card
        card = FloatLayout(size_hint=(0.96, 0.90),
                           pos_hint={"center_x": 0.5, "center_y": 0.5})
        with card.canvas.before:
            Color(bg[0], bg[1], bg[2], min(0.96, bg[3] + 0.06))
            self._card_bg = RoundedRectangle(radius=[dp(18)], pos=card.pos, size=card.size)
            br, bgc, bb, _ = self.tm.col("border")
            Color(br, bgc, bb, 0.85)
            self._card_border = Line(rounded_rectangle=(card.x, card.y, card.width, card.height, dp(18)),
                                     width=dp(2))
        card.bind(pos=lambda *_: self._sync_card(card), size=lambda *_: self._sync_card(card))
        self.add_widget(card)

        # ---- Body (vertical): TAB RIBBON → TITLE → CONTENT (fills the card)
        body = BoxLayout(orientation="vertical", spacing=dp(12),
                         padding=[dp(14), dp(18), dp(14), dp(14)],
                         size_hint=(1, 1), pos_hint={'x': 0, 'y': 0})
        card.add_widget(body)

        # 1) Optional tab ribbon
        tabs_row = None
        if self.default_tab is not None:
            tabs_scroll = ScrollView(do_scroll_x=True, do_scroll_y=False, bar_width=0,
                                     scroll_type=['content'], size_hint_y=None, height=dp(42))
            tabs_row = BoxLayout(orientation="horizontal", spacing=dp(8),
                                 size_hint_x=None, height=dp(42))
            tabs_row.bind(minimum_width=tabs_row.setter("width"))
            tabs_scroll.add_widget(tabs_row)
            body.add_widget(tabs_scroll)

        # 2) Centered title just under the ribbon
        self._title_label = Label(
            text=(self.custom_title or (self.default_tab.capitalize() if self.default_tab else "")),
            font_name=fonts["section_title"]["name"],
            font_size=fonts["section_title"]["size"],
            color=fg,
            size_hint_y=None,
            height=dp(32),
            halign="center", valign="middle",
        )
        self._title_label.bind(size=lambda l, *_: setattr(l, "text_size", (l.width, None)))
        body.add_widget(self._title_label)

        # 3) Content stack (tabs + screens) or template content holder
        self._content_stack = BoxLayout(orientation="vertical", spacing=dp(10))
        body.add_widget(self._content_stack)

        # ---- Tabs + screens OR template area
        if self.default_tab is None:
            self.content_area = BoxLayout(orientation="vertical")
            if hasattr(body, 'padding'):
                body.padding = [body.padding[0], dp(12), body.padding[2], body.padding[3]]
            self._content_stack.add_widget(self.content_area)
        else:
            icon_map = {
                "inventory": "images/ui/tab_inventory.png",
                "journal":   "images/ui/tab_journal.png",
                "stats":     "images/ui/tab_stats.png",
                "map":       "images/ui/tab_map.png",
                "settings":  "images/ui/tab_settings.png",
            }
            labels = {
                "inventory": "Inventory",
                "journal":   "Journal",
                "stats":     "Stats",
                "map":       "Map",
                "settings":  "Settings",
            }
            abbr = {"inventory": "Inv", "journal": "Jrn", "stats": "St", "map": "Map", "settings": "⚙"}

            self._chips = {}
            def _on_select(tab_id: str): self._select_tab(tab_id)
            for tab_id in ("inventory", "journal", "stats", "map", "settings"):
                chip = _TabChip(tab_id, labels[tab_id], None, _on_select, theme=self.tm, abbr=abbr[tab_id])
                self._chips[tab_id] = chip
                if tabs_row is not None:
                    tabs_row.add_widget(chip)

            # Screens
            self.sm = ScreenManager(transition=NoTransition())
            self._content_stack.add_widget(self.sm)
            self.content_area = self.sm

            self._built: set[str] = set()

            # --- REMOVED: Compact mode logic is no longer needed ---
            def _update_tab_mode(*_): pass
            card.bind(size=_update_tab_mode)
            Clock.schedule_once(_update_tab_mode, 0)

            # Initial tab
            start = self.default_tab if self.default_tab in self._chips else "map"
            Clock.schedule_once(lambda *_: self._select_tab(start), 0)

        # Slide in
        Animation(y=0, d=.30, t="out_quad").start(self)

    # ── Layout sync
    def _sync_card(self, card):
        self._card_bg.pos = card.pos; self._card_bg.size = card.size
        self._card_border.rounded_rectangle = (card.x, card.y, card.width, card.height, dp(18))

    # ── Touch handling: swipe down to close
    def on_touch_down(self, touch):
        if self.collide_point(*touch.pos):
            touch.ud['start_y'] = touch.y
            # Let children handle touch first
            if super().on_touch_down(touch):
                return True
            # If no child handled it, we might start a drag
            touch.grab(self)
            return True
        return False

    def on_touch_move(self, touch):
        if touch.grab_current is self:
            return True # Consume move events while dragging
        return super().on_touch_move(touch)

    def on_touch_up(self, touch):
        if touch.grab_current is self:
            touch.ungrab(self)
            if 'start_y' in touch.ud and (touch.ud['start_y'] - touch.y) > dp(50):
                self.dismiss()
            return True
        return super().on_touch_up(touch)

    # ── Tabs
    def _ensure_screen(self, tab_name: str):
        if getattr(self, "_built", None) is None or tab_name in self._built:
            return
        # --- FIX: Moved imports here to break circular dependency ---
        from ui.bag_screen import BagScreen
        from ui.journal_screen import JournalScreen
        from ui.stats_screen import StatsScreen
        from ui.full_map_screen import FullMapScreen
        # -----------------------------------------------------------
        factory = {
            "inventory": BagScreen,
            "journal":   JournalScreen,
            "stats":     StatsScreen,
            "map":       FullMapScreen,
            "settings":  _OverlaySettings,
        }.get(tab_name)
        if not factory: return
        try:
            scr = factory(name=tab_name)
        except Exception:
            scr = Screen(name=tab_name)
            box = BoxLayout()
            msg = Label(text="[i]Load or start a game to use this tab.[/i]",
                        markup=True,
                        font_name=fonts["medium_text"]["name"],
                        font_size=fonts["medium_text"]["size"])
            msg.bind(size=lambda l, *_: setattr(l, "text_size", (l.width, None)))
            box.add_widget(msg)
            scr.add_widget(box)
        self.sm.add_widget(scr)
        self._built.add(tab_name)

    def _select_tab(self, tab_id: str):
        if not hasattr(self, "sm"): return
        # --- THIS IS THE FIX ---
        # Store the currently selected tab on the app instance so it's remembered
        # the next time the menu is opened.
        app = App.get_running_app()
        if app: app._last_menu_tab = tab_id
        if not self.custom_title and hasattr(self, "_title_label"):
            self._title_label.text = tab_id.capitalize()
        self._ensure_screen(tab_id)
        self.sm.current = tab_id
        if hasattr(self, "_chips"):
            for key, chip in self._chips.items():
                chip.set_active(key == tab_id)
        try:
            scr = self.sm.get_screen(tab_id)
            if hasattr(scr, "refresh"):
                scr.refresh()
        except Exception:
            pass

    # Legacy helpers kept
    def set_initial_screen(self, tab_name, *args):
        if self.default_tab is None or not hasattr(self, "sm"): return
        if tab_name and self.sm.has_screen(tab_name):
            self._select_tab(tab_name)

    def open(self):
        Animation.cancel_all(self)
        Animation(y=0, d=0.35, t='out_cubic').start(self)

    def close(self, on_complete=None):
        if on_complete:
            try: self.bind(on_dismiss=lambda *_: on_complete())
            except Exception: pass
        self.dismiss()

    def dismiss(self, *args):
        anim = Animation(y=-Window.height, d=0.30, t="in_quad")
        def _cleanup(*_):
            try: self.dispatch("on_dismiss")
            except Exception: pass
            if self.parent:
                try: self.parent.remove_widget(self)
                except Exception: pass
            try:
                app = App.get_running_app()
                if getattr(app, "_menu_overlay", None) is self:
                    app._menu_overlay = None
                # --- THIS IS THE FIX: Decrement counter and unlock only if it's the last modal ---
                if hasattr(app, '_modal_ref_count'):
                    app._modal_ref_count -= 1
                    if app._modal_ref_count <= 0:
                        app._modal_ref_count = 0
                        game = getattr(app, 'current_game', None)
                        if game:
                            game.input_locked = False
            except Exception:
                pass
        anim.bind(on_complete=lambda *_: _cleanup())
        anim.start(self)
