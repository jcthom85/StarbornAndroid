# ui/narrative_popup.py
from __future__ import annotations

from typing import Callable, Sequence

from kivy.app import App
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.metrics import dp, sp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.label import Label
from kivy.uix.popup import Popup
from kivy.graphics import Color, RoundedRectangle, Line

from font_manager import fonts
from theme_manager import ThemeManager as DefaultThemeManager


class NarrativePopup(Popup):
    """
    Lightweight, titleless popup for short narrative beats.
    Centered text with subtle accent and a single OK by default.

    Usage:
        NarrativePopup.show("You flip the switch. The lights come on.")
    """

    def __init__(self,
                 text: str,
                 *,
                 title: str | None = None,
                 actions: Sequence[tuple[str, Callable | None]] | None = None,
                 theme_mgr=None,
                 width_hint: float = 0.74,
                 autoclose: bool = True,
                 tap_to_dismiss: bool = True,
                 dismiss_on_touch_outside: bool = False,
                 **kwargs):
        super().__init__(title="",
                         separator_height=0,
                         auto_dismiss=False,
                         background="",
                         **kwargs)

        self._tm = theme_mgr if theme_mgr else DefaultThemeManager()
        self._autoclose = autoclose
        self._tap_to_dismiss = bool(tap_to_dismiss and not actions)
        self.background_color = (0, 0, 0, 0)

        # Base container
        root = FloatLayout(size_hint=(1, 1))
        self.add_widget(root)

        # Backplate styling (rounded with soft border)
        self.corner_radius = dp(16)
        with root.canvas.before:
            r, g, b, a = self._tm.col("bg")
            Color(r, g, b, min(0.96, a + 0.05))
            self._bg_rect = RoundedRectangle(radius=[self.corner_radius])
            # Outer border
            br, bg, bb, ba = self._tm.col("border")
            Color(br, bg, bb, 0.85)
            self._border = Line(width=dp(2))
        self.bind(pos=self._repaint, size=self._repaint)

        # Content layout
        layout = BoxLayout(orientation="vertical",
                           spacing=dp(14),
                           padding=[dp(18), dp(22), dp(18), dp(18)],
                           size_hint=(1, None),
                           pos_hint={"center_x": 0.5, "top": 1})
        self.bind(width=lambda *_: setattr(layout, "width", self.width))
        layout.bind(minimum_height=lambda inst, h: setattr(inst, "height", h))
        root.add_widget(layout)
        self._layout = layout

        # Optional title (only if specified)
        fg = self._tm.col("fg")
        if title:
            ttl = Label(text=title,
                        markup=True,
                        font_name=fonts["popup_title"]["name"],
                        font_size=fonts["popup_title"]["size"],
                        color=fg,
                        halign="center", valign="middle",
                        size_hint=(1, None))
            ttl.bind(size=lambda inst, *_: setattr(inst, "text_size", (inst.width - dp(8), None)))
            ttl.texture_update()
            ttl.height = max(dp(42), getattr(ttl, "texture_size", (0, 0))[1])
            layout.add_widget(ttl)

        # Narrative label
        lbl = Label(text=text,
                    markup=True,
                    font_name=fonts["medium_text"]["name"],
                    font_size=fonts["medium_text"]["size"],
                    color=fg,
                    halign="center", valign="middle",
                    size_hint=(1, None))
        lbl.bind(size=lambda inst, *_: setattr(inst, "text_size", (inst.width - dp(8), None)))
        lbl.texture_update()
        lbl.height = max(dp(80), getattr(lbl, "texture_size", (0, 0))[1] + dp(2))
        layout.add_widget(lbl)

        # Actions (single OK by default)
        if actions:
            # Only render actions when explicitly provided
            for txt, cb in actions:
                btn = Button(text=txt,
                             size_hint=(1, None), height=dp(48),
                             background_normal="",
                             background_color=self._tm.col("fg"),
                             color=self._tm.col("bg"),
                             font_name=fonts["popup_button"]["name"],
                             font_size=fonts["popup_button"]["size"])

            def _wrap(_cb: Callable | None):
                def _do(*_):
                    if callable(_cb):
                        _cb()
                    if self._autoclose:
                        self.dismiss()
                return _do

            btn.bind(on_release=_wrap(cb))
            layout.add_widget(btn)

        # Size/position now
        self._resize(width_hint)
        Window.bind(on_resize=lambda *_: Clock.schedule_once(lambda *_: self._resize(width_hint), 0))

        if dismiss_on_touch_outside:
            self.bind(on_touch_down=self._on_any_touch)

    def on_touch_down(self, touch):
        # If configured for tap-anywhere-to-dismiss (and no action buttons),
        # close the popup and swallow the event so the world doesn’t also react.
        if self._tap_to_dismiss:
            self.dismiss()
            return True
        return super().on_touch_down(touch)

    def _repaint(self, *_):
        self._bg_rect.pos = self.pos
        self._bg_rect.size = self.size
        self._border.rounded_rectangle = (*self.pos, *self.size, self.corner_radius)

    def _resize(self, width_hint: float):
        max_w = min(Window.width * width_hint, dp(720))
        self.size_hint = (None, None)
        # Height from content + padding
        content_h = getattr(self._layout, "minimum_height", self._layout.height)
        self.width = max_w
        self.height = min(max(dp(180), content_h + dp(40)), Window.height * 0.6)
        # Center on screen
        self.x = (Window.width - self.width) / 2.0
        self.y = (Window.height - self.height) / 2.0

    def _on_any_touch(self, inst, touch):
        if not self.collide_point(*touch.pos):
            self.dismiss()
            return True
        return False

    @staticmethod
    def show(text: str,
             *,
             title: str | None = None,
             actions: Sequence[tuple[str, Callable | None]] | None = None,
             theme_mgr=None,
             width_hint: float = 0.74,
             autoclose: bool = True,
             tap_to_dismiss: bool = True):
        pop = NarrativePopup(text, title=title, actions=actions, theme_mgr=theme_mgr,
                             width_hint=width_hint, autoclose=autoclose,
                             tap_to_dismiss=tap_to_dismiss)
        pop.open()
        return pop
