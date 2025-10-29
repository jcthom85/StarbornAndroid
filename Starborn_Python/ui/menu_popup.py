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
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.gridlayout  import GridLayout
from kivy.uix.label       import Label
from kivy.uix.popup       import Popup
from kivy.uix.scrollview  import ScrollView
from kivy.uix.widget      import Widget
from kivy.graphics        import Color, Line, RoundedRectangle

from kivy.app import App
from font_manager  import fonts
from theme_manager import ThemeManager as DefaultThemeManager
from ui.themed_button import ThemedButton


def _mix_rgb(a: Sequence[float], b: Sequence[float], weight: float) -> tuple[float, float, float]:
    """Blend two RGB colors (ignores alpha) by weight."""
    weight = max(0.0, min(1.0, weight))
    get = lambda col, idx: float(col[idx]) if idx < len(col) else 0.0
    return tuple(get(a, i) * (1.0 - weight) + get(b, i) * weight for i in range(3))


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
        size_hint = kwargs.pop("size_hint", None)
        size_hint_x = kwargs.pop("size_hint_x", None)
        size_hint_y = kwargs.pop("size_hint_y", None)

        width_hint: float | None = None
        height_hint: float | None = None
        if isinstance(size_hint, (tuple, list)):
            if len(size_hint) > 0 and size_hint[0] not in (None,):
                width_hint = float(size_hint[0])
            if len(size_hint) > 1 and size_hint[1] not in (None,):
                height_hint = float(size_hint[1])
        elif size_hint is not None:
            width_hint = float(size_hint)
        if size_hint_x is not None:
            width_hint = float(size_hint_x)
        if size_hint_y is not None:
            height_hint = float(size_hint_y)

        def _clamp(value: float | None, default: float, *, lo: float = 0.3, hi: float = 0.95) -> float:
            if value is None or value <= 0:
                return default
            return max(lo, min(hi, float(value)))

        self._width_hint = _clamp(width_hint, 0.78, hi=0.9)
        self._max_height_ratio = _clamp(height_hint, 0.74, lo=0.35, hi=0.9)
        kwargs.setdefault("size_hint", (None, None))

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
        self.size_hint = (None, None)
        self._autoclose = autoclose
        self._tm = theme_mgr if theme_mgr else DefaultThemeManager()
        self._action_grid: GridLayout | None = None  # will be made on demand
        self._action_scroll: ScrollView | None = None
        self._fg_col = self._tm.col("fg")
        self._bg_col = self._tm.col("bg")
        self._accent_col = self._tm.col("accent")
        self._border_col = self._tm.col("border")
        self._base_rgb = _mix_rgb(self._bg_col, self._fg_col, 0.34)
        self._border_rgb = _mix_rgb(self._border_col, self._accent_col, 0.4)
        self._accent_alpha = self._accent_col[3] if len(self._accent_col) > 3 else 1.0
        self._border_rgba = (
            self._border_rgb[0],
            self._border_rgb[1],
            self._border_rgb[2],
            self._border_col[3] if len(self._border_col) > 3 else 0.9,
        )

        # --- NEW: Swipe-to-dismiss state ---
        self._touch_start_y = 0
        self._is_dragging = False

        # keep centred on resize
        Window.bind(on_resize=lambda *_: self._schedule_resize())
        self._resize_pending = False
        self._in_resize = False
        self._schedule_resize()

        # ── root + background ────────────────────────────────────────
        root = FloatLayout(size_hint=(1, 1))
        self.add_widget(root)

        self.corner_radius = corner_radius
        with root.canvas.before:
            shadow_alpha = 0.32
            Color(0, 0, 0, shadow_alpha)
            self._shadow_rect = RoundedRectangle(radius=[(self.corner_radius, self.corner_radius)] * 4)
            base = self._base_rgb
            Color(base[0], base[1], base[2], 0.95)
            self._bg_rect = RoundedRectangle(radius=[(self.corner_radius, self.corner_radius)] * 4)
            accent = self._accent_col
            Color(accent[0], accent[1], accent[2], min(0.65, self._accent_alpha))
            self._accent_rect = RoundedRectangle(radius=[
                (self.corner_radius, self.corner_radius),
                (self.corner_radius, self.corner_radius),
                (0, 0),
                (0, 0),
            ])
            from kivy.graphics import Color as _Color
            self._br_col = _Color(*self._border_rgba)
            self._border_ln = Line(width=dp(1.6))
        self.bind(pos=self._update_canvas, size=self._update_canvas)

        # ── vertical layout for content ──────────────────────────────
        layout = BoxLayout(
            orientation="vertical",
            spacing=dp(18),
            padding=[dp(24), dp(32), dp(24), dp(54)],  # L T R B
            size_hint=(1, None),
            pos_hint={"center_x": 0.5, "top": 1},
        )
        self.bind(width=lambda *_: setattr(layout, "width", self.width))
        layout.bind(minimum_height=lambda *_: self._sync_layout_height())
        root.add_widget(layout)
        self._layout = layout
        # ── title ────────────────────────────────────────────────────
        title_lbl = Label(
            text=f"[b]{title}[/b]",
            markup=True,
            font_name=fonts["popup_title"]["name"],
            font_size=fonts["popup_title"]["size"] * 1.02,
            size_hint=(1, None),
            halign="center",
            valign="middle",
            color=(self._fg_col[0], self._fg_col[1], self._fg_col[2], 0.98),
        )
        title_lbl.bind(
            width=lambda inst, width: setattr(inst, "text_size", (width, None)),
            texture_size=lambda inst, size: setattr(inst, "height", max(size[1], dp(36))),
        )
        layout.add_widget(title_lbl)

        layout.add_widget(Widget(size_hint=(1, None), height=dp(4)))

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
            def _kick(*_): self._schedule_resize()

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

            self._action_scroll = ScrollView(do_scroll_x=False, size_hint=(1, None), bar_width=dp(4))
            def _update_action_height(*_):
                Clock.schedule_once(self._refresh_action_container, 0)
            self._action_grid.bind(minimum_height=_update_action_height)
            self._action_scroll.bind(height=lambda *_: self._schedule_resize())
            scroll = self._action_scroll
            scroll.add_widget(self._action_grid)
            self._layout.add_widget(scroll)
            Clock.schedule_once(self._refresh_action_container, 0)

        # helper to respect autoclose
        def _on_release(*_):
            if self._autoclose:
                self.dismiss()
            if callback:
                callback()

        style = dict(self._button_style)
        if "color" in style and "text_color" not in style:
            style["text_color"] = style.pop("color")
        else:
            style.pop("color", None)
        if "background_color" in style and "bg_color" not in style:
            style["bg_color"] = style.pop("background_color")
        else:
            style.pop("background_color", None)
        # Textured backgrounds are no longer used in the redesigned buttons
        style.pop("background_normal", None)
        style.pop("background_down", None)
        style.pop("border", None)
        button_height = style.pop("height", dp(48))
        corner_radius_btn = style.pop("corner_radius", button_height / 2.0)
        font_name = style.pop("font_name", fonts["popup_button"]["name"])
        font_size = style.pop("font_size", fonts["popup_button"]["size"])
        def _to_rgba(value, default):
            if value is None:
                return list(default)
            if isinstance(value, (list, tuple)):
                vals = list(value)
            else:
                vals = [float(value)]
            if len(vals) == 1:
                vals = [vals[0], vals[0], vals[0], 1.0]
            elif len(vals) == 2:
                vals = [vals[0], vals[1], default[2], 1.0]
            elif len(vals) == 3:
                vals = [vals[0], vals[1], vals[2], 1.0]
            elif len(vals) > 4:
                vals = vals[:4]
            return [float(vals[i]) for i in range(4)]

        base_bg = (
            self._accent_col[0],
            self._accent_col[1],
            self._accent_col[2],
            min(0.95, self._accent_alpha if self._accent_alpha else 1.0),
        )
        base_fg = (
            self._bg_col[0],
            self._bg_col[1],
            self._bg_col[2],
            0.96,
        )
        text_color = _to_rgba(style.pop("text_color", None), base_fg)
        bg_color = _to_rgba(style.pop("bg_color", None), base_bg)

        btn = ThemedButton(
            text=text,
            size_hint=(1, None),
            height=button_height,
            font_name=font_name,
            font_size=font_size,
            bg_color=list(bg_color),
            color=text_color,
        )
        initial_radius = float(corner_radius_btn) if corner_radius_btn is not None else button_height / 2.0
        btn.corner_radius = initial_radius
        btn.bind(height=lambda inst, h: setattr(inst, "corner_radius", max(1.0, h / 2.0)))
        for key, value in style.items():
            try:
                setattr(btn, key, value)
            except Exception:
                pass

        btn.bind(on_release=_on_release)
        self._action_grid.add_widget(btn)
        self._schedule_resize()

    # ───────────────────────────────────────────────────── helpers
    def _schedule_resize(self):
        if getattr(self, "_resize_pending", False) or getattr(self, "_in_resize", False):
            return
        self._resize_pending = True
        Clock.schedule_once(self._resize, 0)

    def _sync_layout_height(self):
        if not hasattr(self, "_layout") or self._layout is None:
            return
        self._layout.height = self._layout.minimum_height
        if not getattr(self, "_in_resize", False):
            self._schedule_resize()

    def _refresh_action_container(self, *_args):
        self._schedule_resize()

    def _update_canvas(self, *_):
        x, y = self.pos
        w, h = self.size
        corner = (self.corner_radius, self.corner_radius)
        shadow_offset = dp(6)
        if hasattr(self, "_shadow_rect"):
            self._shadow_rect.pos = (x - shadow_offset, y - shadow_offset)
            self._shadow_rect.size = (w + shadow_offset * 2, h + shadow_offset * 2)
            self._shadow_rect.radius = [corner, corner, corner, corner]
        self._bg_rect.pos = (x, y)
        self._bg_rect.size = (w, h)
        self._bg_rect.radius = [corner, corner, corner, corner]

        accent_height = dp(10)
        accent_inset = dp(6)
        accent_width = max(0, w - accent_inset * 2)
        if hasattr(self, "_accent_rect"):
            self._accent_rect.pos = (x + accent_inset, y + h - accent_height)
            self._accent_rect.size = (accent_width, accent_height)
            self._accent_rect.radius = [
                (self.corner_radius, self.corner_radius),
                (self.corner_radius, self.corner_radius),
                (0, 0),
                (0, 0),
            ]

        self._border_ln.rounded_rectangle = (x, y, w, h, self.corner_radius)

    def _resize(self, _dt):
        self._resize_pending = False
        self._in_resize = True
        try:
            win_w, win_h = Window.width, Window.height
            min_width = dp(320)
            max_width = max(min_width, win_w - dp(48))
            target_width = self._width_hint * win_w if self._width_hint else min_width
            self.width = max(min_width, min(target_width, max_width))

            min_height = dp(220)
            max_height = max(min_height, win_h * self._max_height_ratio)

            other_height = 0.0
            if self._layout:
                children = list(self._layout.children)
                spacing_total = self._layout.spacing * max(0, len(children) - 1)
                try:
                    top = float(self._layout.padding[1])
                    bottom = float(self._layout.padding[3])
                except Exception:
                    top = bottom = 0.0
                other_height = top + bottom + spacing_total
                for child in children:
                    if child is self._action_scroll:
                        continue
                    other_height += float(getattr(child, "height", 0) or 0)

            desired_action_height = 0.0
            if self._action_grid is not None:
                desired_action_height = float(getattr(self._action_grid, "height", 0) or 0)

            desired_total = other_height + desired_action_height
            target_height = max(min_height, min(desired_total, max_height))
            self.height = target_height

            available_for_actions = max(target_height - other_height, 0.0)
            actual_action_height = 0.0
            if self._action_scroll and self._action_grid:
                actual_action_height = min(desired_action_height, available_for_actions)
                if self._action_grid.children and desired_action_height > 0 and actual_action_height <= 0 < available_for_actions:
                    actual_action_height = min(desired_action_height, available_for_actions)
                self._action_scroll.size_hint_y = None
                self._action_scroll.height = actual_action_height

            content_height = other_height + actual_action_height
            if self._layout:
                self._layout.height = self._layout.minimum_height
                content_height = max(content_height, float(self._layout.height))

            final_height = max(min_height, min(content_height, max_height))
            if final_height != self.height:
                self.height = final_height
                available_for_actions = max(final_height - other_height, 0.0)
                if self._action_scroll and self._action_grid:
                    actual_action_height = min(desired_action_height, available_for_actions)
                    self._action_scroll.height = actual_action_height
                    if self._layout:
                        self._layout.height = self._layout.minimum_height

            self.pos = (
                win_w / 2 - self.width / 2,
                win_h / 2 - self.height / 2,
            )
        finally:
            self._in_resize = False

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
            touch.ud['start_y'] = touch.y
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
                    base = self._border_rgba
                    self._br_col.rgba = (base[0], base[1], base[2], 0)
                except Exception:
                    pass
            else:
                # Restore width and color; geometry will be placed by _update_canvas
                self._border_ln.width = dp(1.6)
                try:
                    self._br_col.rgba = self._border_rgba
                except Exception:
                    pass
                self._update_canvas()
        except Exception:
            pass
