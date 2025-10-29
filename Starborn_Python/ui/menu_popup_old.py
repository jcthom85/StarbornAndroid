# ui/menu_popup.py
from __future__ import annotations

import sys
import os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from kivy.animation import Animation

from typing import Callable, Sequence

from kivy.clock           import Clock
from kivy.core.window     import Window
from kivy.metrics         import dp
from kivy.uix.boxlayout   import BoxLayout
from kivy.uix.button      import Button
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.gridlayout  import GridLayout
from kivy.uix.image       import Image
from kivy.uix.label       import Label
from kivy.uix.popup       import Popup
from kivy.uix.scrollview  import ScrollView
from kivy.uix.widget      import Widget
from kivy.graphics        import Color, Line, Rectangle, RoundedRectangle

from kivy.app import App
from font_manager  import fonts
from theme_manager import ThemeManager as DefaultThemeManager


class MenuPopup(Popup):
    """
    In-game modal with rounded border + auto-scrollable button list.
    By default only one MenuPopup is visible at a time; pass stack=True
    to layer another popup on top (e.g., “Done!” confirmation).
    """
    _active: "MenuPopup | None" = None   # track the top-most popup

    # ───────────────────────────────────────────────────────── ctor
    def __init__(
        self,
        title: str,
        *,
        body: Widget | None = None,
        actions: Sequence[tuple[str, Callable | None]] | None = None,
        theme_mgr=None,
        stack: bool = False,
        autoclose: bool = True, # If action buttons should close the popup
        dismiss_on_touch_outside: bool = True, # Default to allowing outside tap to close
        button_style: dict | None = None,
        **kwargs,
    ):
        # close previous unless stacking is requested
        if not stack and MenuPopup._active is not None:
            MenuPopup._active.dismiss()
        MenuPopup._active = self

        # --- THIS IS THE FIX: Use a counter to manage input lock ---
        try:
            app = App.get_running_app()
            if not hasattr(app, '_modal_ref_count'):
                app._modal_ref_count = 0
            app._modal_ref_count += 1
            
            game = getattr(app, 'current_game', None)
            if game:
                game.input_locked = True
        except Exception:
            pass

        theme_mgr = kwargs.pop("theme_mgr", theme_mgr)
        self._button_style = button_style or {}
        corner_radius = kwargs.pop("corner_radius", dp(22))
        super().__init__(
            title="",
            separator_height=0,
            auto_dismiss=False,
            **kwargs,
        )
        self.background = ""
        self.background_color = (0, 0, 0, 0)
        self._autoclose = autoclose
        self._tm = theme_mgr if theme_mgr else DefaultThemeManager()
        self._action_grid: GridLayout | None = None  # will be made on demand

        # --- NEW: Swipe-to-dismiss state ---
        self._touch_start_y = 0
        self._is_dragging = False

        # keep centred on resize
        Window.bind(on_resize=lambda *_: Clock.schedule_once(self._resize, 0))
        Clock.schedule_once(self._resize, 0)

        # ── root + background ────────────────────────────────────────
        root = FloatLayout(size_hint=(1, 1))
        self.add_widget(root)

        self.corner_radius = corner_radius
        with root.canvas.before:
            r, g, b, a = self._tm.col("bg")
            Color(r, g, b, a)
            self._bg_rect = RoundedRectangle(radius=[(self.corner_radius, self.corner_radius)] * 4)
            # Store border color so visibility can be toggled later
            from kivy.graphics import Color as _Color
            self._br_col = _Color(*self._tm.col("border"))
            self._border_ln = Line(width=dp(2))
        self.bind(pos=self._update_canvas, size=self._update_canvas)

        # ── vertical layout for content ──────────────────────────────
        layout = BoxLayout(
            orientation="vertical",
            spacing=dp(10),
            padding=[dp(16), dp(4), dp(16), dp(48)],  # L T R B
            size_hint=(1, None),
            pos_hint={"center_x": 0.5, "top": 1},
        )
        self.bind(width=lambda *_: setattr(layout, "width", self.width))
        layout.bind(minimum_height=lambda inst, h: setattr(inst, "height", h))
        root.add_widget(layout)
        self._layout = layout
        # NEW: whenever layout’s min height changes, recalc the popup height next frame
        self._layout.bind(minimum_height=lambda *_: Clock.schedule_once(self._resize, 0))
        # ── title ────────────────────────────────────────────────────
        title_lbl = Label(
            text=title,
            font_name=fonts["popup_title"]["name"],
            font_size=fonts["popup_title"]["size"],
            size_hint=(1, None),
            halign="center", valign="middle",
        )
        title_lbl.bind(texture_size=lambda l, ts: setattr(l, "height", ts[1]))
        layout.add_widget(title_lbl)

        # underline
        underline = Widget(size_hint=(0.9, None), height=dp(1),
                           pos_hint={"center_x": 0.5})
        with underline.canvas:
            Color(*self._tm.col("border"))
            urect = Rectangle()
        underline.bind(pos=lambda i, v: setattr(urect, "pos", i.pos),
                       size=lambda i, v: setattr(urect, "size", i.size))
        layout.add_widget(underline)
        layout.add_widget(Widget(size_hint=(1, None), height=dp(8)))

        # body
        if body:
            # If body is a callable (like a lambda), execute it to get the widget.
            if callable(body):
                body = body()

            # ============================= FIX STARTS HERE ============================
            # This logic is for a SCROLLVIEW body inside a FIXED-HEIGHT popup.
            # It makes the ScrollView fill the available space. For dynamic-height
            # popups, the calling code is responsible for sizing the ScrollView.
            if self.size_hint_y is not None and isinstance(body, ScrollView):
                body.size_hint_y = None

                def _set_scrollview_height(*_):
                    other_widgets_height = sum(c.height for c in layout.children if c is not body)
                    available_height = self.height - layout.padding[1] - layout.padding[3]
                    new_height = available_height - other_widgets_height

                    if new_height > 0:
                        body.height = new_height
                
                self.bind(height=_set_scrollview_height)
                Clock.schedule_once(_set_scrollview_height, 0)
            # ============================== FIX ENDS HERE =============================

            body.size_hint_x = 1
            layout.add_widget(body)
            layout.add_widget(Widget(size_hint=(1, None), height=dp(4)))

            # This makes the popup dynamically resize to fit its content.
            def _kick(*_): Clock.schedule_once(self._resize, 0)

            if hasattr(body, "height"):           body.bind(height=_kick)
            if hasattr(body, "minimum_height"):   body.bind(minimum_height=_kick)

            # If the body is a ScrollView, watch its content's size as well,
            # as the ScrollView's own height might not change.
            try:
                if hasattr(body, "children") and body.children:
                    child = body.children[0]
                    if hasattr(child, "height"):           child.bind(height=_kick)
                    if hasattr(child, "minimum_height"):   child.bind(minimum_height=_kick)
            except Exception:
                pass

        # initial actions (if supplied)
        if actions:
            for txt, cb in actions:
                self.add_action(txt, cb)     # uses helper below

        if dismiss_on_touch_outside:
            self.bind(on_touch_down=self._on_any_touch)

    # ───────────────────────────────────────────────────── add_action
    def add_action(self, text: str, callback: Callable | None):
        """
        Append a button after the popup has been built/opened.
        Dynamically creates the action area if it didn’t exist yet.
        """
        # lazily build the scroll + grid on first call
        if self._action_grid is None:
            self._action_grid = GridLayout(cols=1, spacing=dp(4),
                                           size_hint_y=None)
            self._action_grid.bind(minimum_height=self._action_grid.setter("height"))

            max_h = Window.height * 0.6
            scroll = ScrollView(do_scroll_x=False, size_hint=(1, None),
                                bar_width=dp(4))
            scroll.height = min(self._action_grid.height, max_h)
            def _update_action_height(_g, h):
                scroll.height = min(h, max_h)
                Clock.schedule_once(self._resize, 0)  # NEW: resize popup as buttons appear/disappear
            self._action_grid.bind(minimum_height=_update_action_height)

            scroll.add_widget(self._action_grid)
            self._layout.add_widget(scroll)

        # helper to respect autoclose
        def _on_release(*_):
            if self._autoclose:
                self.dismiss()
            if callback:
                callback()

        # Build the button with default styles, then merge in overrides without mutating the base style
        style = dict(self._button_style)
        corner_radius_btn = style.pop('corner_radius', dp(10))
        text_color = style.pop('text_color', None)
        normal_color = style.pop('background_color', self._tm.col('fg'))
        down_color = style.pop('background_down_color', None)

        def _to_rgba(col):
            col = tuple(col)
            if len(col) == 4:
                return col
            if len(col) == 3:
                return (*col, 1.0)
            if len(col) == 2:
                return (col[0], col[1], 0.0, 1.0)
            if len(col) == 1:
                shade = col[0]
                return (shade, shade, shade, 1.0)
            return (0.0, 0.0, 0.0, 1.0)

        normal_color = _to_rgba(normal_color)
        if down_color is not None:
            down_color = _to_rgba(down_color)
        # Remove texture-specific keys; we draw the rounded background ourselves
        style.pop('background_normal', None)
        style.pop('background_down', None)
        style.pop('border', None)

        btn_kwargs = {
            'text': text,
            'size_hint': (1, None),
            'height': dp(44),
            'font_name': fonts["popup_button"]["name"],
            'font_size': fonts["popup_button"]["size"],
            'background_normal': '',
            'background_down': '',
            'background_color': (0, 0, 0, 0),
            'color': self._tm.col('bg'),
        }
        btn_kwargs.update(style)
        btn = Button(**btn_kwargs)
        if text_color is not None:
            btn.color = text_color

        with btn.canvas.before:
            btn_bg_color = Color(*normal_color)
            btn_bg_rect = RoundedRectangle(
                pos=btn.pos,
                size=btn.size,
                radius=[(corner_radius_btn, corner_radius_btn)] * 4,
            )

        def _update_rect(instance, _value):
            btn_bg_rect.pos = instance.pos
            btn_bg_rect.size = instance.size

        btn.bind(pos=_update_rect, size=_update_rect)

        if down_color is not None:
            def _sync_state(instance, value):
                btn_bg_color.rgba = down_color if value == 'down' else normal_color
            btn.bind(state=_sync_state)

        btn.bind(on_release=_on_release)
        self._action_grid.add_widget(btn)
        Clock.schedule_once(self._resize, 0)  # NEW

    # ───────────────────────────────────────────────────── helpers
    def _update_canvas(self, *_):
        self._bg_rect.pos  = self.pos
        self._bg_rect.size = self.size
        corner = (self.corner_radius, self.corner_radius)
        self._bg_rect.radius = [corner, corner, corner, corner]
        self._border_ln.rounded_rectangle = (
            self.x, self.y, self.width, self.height, self.corner_radius)

    def _resize(self, _dt):
        if self.size_hint_x:
            self.width = Window.width * self.size_hint_x
        if self.size_hint_y:
            self.height = Window.height * self.size_hint_y
        else:
            self.height = self._layout.minimum_height + dp(12)
        self.pos = (
            Window.width / 2 - self.width / 2,
            Window.height / 2 - self.height / 2,
        )

    # --- NEW: Swipe-to-dismiss and outside tap logic ---
    def _on_any_touch(self, _inst, touch):
        if not self._layout.collide_point(touch.x, touch.y):
            self.dismiss()
            return True
        return False

    def on_touch_down(self, touch):
        if self.collide_point(*touch.pos):
            # Let children (buttons, etc.) handle the touch first.
            if super().on_touch_down(touch):
                return True
            # If no child handled it, we might start a drag-to-dismiss.
            touch.grab(self)
            self._touch_start_y = self.y
            self._is_dragging = True
            return True
        # If dismiss_on_touch_outside is True, this will handle it.
        return super().on_touch_down(touch)

    def on_touch_move(self, touch):
        if touch.grab_current is self and self._is_dragging:
            # Move the popup vertically with the finger.
            self.y = self._touch_start_y + (touch.y - touch.ud['start_y'])
            return True
        return super().on_touch_move(touch)

    def on_touch_up(self, touch):
        if touch.grab_current is self and self._is_dragging:
            touch.ungrab(self)
            self._is_dragging = False
            # If swiped down far enough, dismiss it. Otherwise, snap back.
            if (self._touch_start_y - self.y) > dp(60):
                self.dismiss(animate_out=True)
            else:
                Animation(pos=self.pos, d=0.2, t='out_cubic').start(self)
            return True
        return super().on_touch_up(touch)

    def on_dismiss(self, *_):
        if MenuPopup._active is self:
            MenuPopup._active = None
        
        # --- THIS IS THE FIX: Decrement counter and unlock only if it reaches zero ---
        try:
            app = App.get_running_app()
            if hasattr(app, '_modal_ref_count'):
                app._modal_ref_count -= 1
                if app._modal_ref_count <= 0:
                    app._modal_ref_count = 0 # Prevent negative counts
                    game = getattr(app, 'current_game', None)
                    if game:
                        game.input_locked = False
        except Exception:
            pass

    def dismiss(self, *args, **kwargs):
        """Override dismiss to add an animation for swipe-out."""
        if kwargs.get('animate_out'):
            anim = Animation(y=-self.height, d=0.2, t='in_quad')
            anim.bind(on_complete=lambda *_: super(MenuPopup, self).dismiss())
            anim.start(self)
        else:
            super(MenuPopup, self).dismiss()

    # ---- Public: toggle border outline visibility (for dark rooms) ---------
    def set_border_visible(self, visible: bool):
        self._border_visible = bool(visible)
        try:
            if not visible:
                # Hide by zeroing the rectangle and making stroke transparent
                self._border_ln.rounded_rectangle = (0, 0, 0, 0, 0)
                # Also reduce width so accidental draws are invisible
                self._border_ln.width = 0
                try:
                    # alpha 0 for good measure
                    r, g, b, _ = self._tm.col("border")
                    self._br_col.rgba = (r, g, b, 0)
                except Exception:
                    pass
            else:
                # Restore width and color; geometry will be placed by _update_canvas
                self._border_ln.width = dp(2)
                try:
                    r, g, b, a = self._tm.col("border")
                    self._br_col.rgba = (r, g, b, a)
                except Exception:
                    pass
                self._update_canvas()
        except Exception:
            pass
