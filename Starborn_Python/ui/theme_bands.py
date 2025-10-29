# ui/theme_bands.py
from __future__ import annotations

from kivy.uix.floatlayout import FloatLayout
from kivy.uix.label import Label
from kivy.graphics import Rectangle, Color, Fbo, ClearColor, ClearBuffers
from kivy.animation import Animation
from kivy.metrics import dp, sp
from kivy.properties import NumericProperty
from ui.blurred_extension import BlurredExtension
from ui.background_snapshot import snapshot as ui_snapshot


class ThemeBands(FloatLayout):
    """
    Top/Bottom themed bands that frame a TRUE 9:16 safe viewport.

    â€¢ If the window is taller than 9:16 (e.g., 9:21), ALL leftover height is
      turned into top+bottom bands (split evenly) so the area between them is
      exactly 9:16. Your 9:16 background will never stretch.
    â€¢ If the window is exactly 9:16 (or shorter), bands collapse to 0px so the
      safe area is the full window (still 9:16 on a 9:16 screen).
    â€¢ get_safe_rect() returns (x, y, w, h) for that centered safe area.
    â€¢ apply_theme() lets you tweak colors and title font.
    â€¢ morph_to() can temporarily animate into letterbox for cinematics.
    """

    # These are kept for future tuning/skin needs, but the layout logic below
    # uses *all* leftover space for bands and ignores caps/ratios by default.
    band_ratio = NumericProperty(0.06)         # not used by update_layout()
    min_band_px = NumericProperty(dp(56))      # not used by update_layout()
    max_band_px = NumericProperty(dp(120))     # not used by update_layout()

    def __init__(self, **kw):
        super().__init__(**kw)
        self.size_hint = (1, 1)
        self.pos = (0, 0)
        # Track base alphas so we can add darkness on top without losing theme
        self._top_base_a = 0.35
        self._bot_base_a = 0.35

        # ---------------- Top band ----------------
        self.top = FloatLayout(size_hint=(1, None), height=0)
        self.add_widget(self.top)
        # Blur extension widget
        self._top_blur = BlurredExtension(
            fade_direction='bottom',  # top band fades downward toward safe area
            size_hint=(1, 1),
            blur_intensity=2.8,
            fade_start=0.92,
            fade_end=0.20,
            fade_power=1.25,
            sample_band=0.08,
        )
        self.top.add_widget(self._top_blur)
        with self.top.canvas:
            self._top_col = Color(0.06, 0.07, 0.10, self._top_base_a)  # semi-opaque so blur shows through
            self._top_rect = Rectangle(pos=self.top.pos, size=self.top.size)
        # Dark overlay as its own child widget so it renders ABOVE the blur
        # (also a child) but BELOW later-added band children like the title
        # and minimap. This guarantees full-height coverage without dimming UI.
        from kivy.uix.widget import Widget
        self._top_dark_w = Widget(size_hint=(1, 1))
        with self._top_dark_w.canvas:
            self._top_dark_col = Color(0, 0, 0, 0.0)
            self._top_dark_rect = Rectangle(pos=self._top_dark_w.pos, size=self._top_dark_w.size)
        # Keep rect synced to the child widget bounds
        self._top_dark_w.bind(pos=lambda inst, p: setattr(self._top_dark_rect, 'pos', p),
                              size=lambda inst, s: setattr(self._top_dark_rect, 'size', s))
        # Insert after blur so it draws above blur but before other children
        self.top.add_widget(self._top_dark_w)
        # subtle inner seam (highlight + shadow)
        with self.top.canvas.after:
            self._top_hi = Color(1, 1, 1, 0.08)   # 1px highlight at inner edge
            self._top_hi_rect = Rectangle(pos=(0, 0), size=(0, dp(1)))
            self._top_sh = Color(0, 0, 0, 0.30)   # 2px inner shadow just below
            self._top_sh_rect = Rectangle(pos=(0, 0), size=(0, dp(2)))

        # ---------------- Bottom band -------------
        self.bottom = FloatLayout(size_hint=(1, None), height=0)
        self.add_widget(self.bottom)
        self._bot_blur = BlurredExtension(
            fade_direction='top',  # bottom band fades upward toward safe area
            size_hint=(1, 1),
            blur_intensity=2.8,
            fade_start=0.92,
            fade_end=0.20,
            fade_power=1.25,
            sample_band=0.08,
        )
        self.bottom.add_widget(self._bot_blur)
        with self.bottom.canvas:
            self._bot_col = Color(0.06, 0.07, 0.10, self._bot_base_a)
            self._bot_rect = Rectangle(pos=self.bottom.pos, size=self.bottom.size)
        self._bot_dark_w = Widget(size_hint=(1, 1))
        with self._bot_dark_w.canvas:
            self._bot_dark_col = Color(0, 0, 0, 0.0)
            self._bot_dark_rect = Rectangle(pos=self._bot_dark_w.pos, size=self._bot_dark_w.size)
        self._bot_dark_w.bind(pos=lambda inst, p: setattr(self._bot_dark_rect, 'pos', p),
                              size=lambda inst, s: setattr(self._bot_dark_rect, 'size', s))
        self.bottom.add_widget(self._bot_dark_w)
        with self.bottom.canvas.after:
            self._bot_sh = Color(0, 0, 0, 0.30)   # 2px inner shadow at top edge
            self._bot_sh_rect = Rectangle(pos=(0, 0), size=(0, dp(2)))
            self._bot_hi = Color(1, 1, 1, 0.08)   # 1px highlight just below
            self._bot_hi_rect = Rectangle(pos=(0, 0), size=(0, dp(1)))

        # Optional title area (you can reparent your title label here)
        self._title = Label(
            text="", markup=True, size_hint=(None, None),
            halign="left", valign="middle"
        )
        self._title.bind(texture_size=lambda *_: self._layout_title())
        self.top.add_widget(self._title)

        # Keep canvases in sync
        for w in (self, self.top, self.bottom):
            w.bind(pos=self._sync, size=self._sync)

        # Recompute band sizes whenever the overall widget size changes
        self.bind(size=lambda *_: self.update_layout())
        self.bind(pos=lambda *_: self.update_layout())

        # Darkness progress (0..1) to blend extra alpha in bands only
        self._dark_prog = 0.0
        self._dark_target_alpha = 0.55

    def set_dark_target_alpha(self, alpha: float):
        """Public method to set the target alpha for the dark overlay."""
        self._dark_target_alpha = float(alpha)

    # ---------------------- Public API ----------------------

    def apply_theme(self, *, top_rgba=None, bottom_rgba=None, title_style=None):
        """Change colors and title style at runtime."""
        if top_rgba:
            self._top_col.r, self._top_col.g, self._top_col.b, self._top_col.a = top_rgba
        if bottom_rgba:
            self._bot_col.r, self._bot_col.g, self._bot_col.b, self._bot_col.a = bottom_rgba
        if isinstance(title_style, dict):
            self._title.font_name = title_style.get("name", self._title.font_name)
            self._title.font_size = title_style.get("size", self._title.font_size)
            self._title.color = title_style.get("color", getattr(self._title, "color", (1, 1, 1, 1)))
        self._layout_title()
        self._sync()

    # Public: set band darkness level to match rooms
    def set_dark_progress(self, progress: float, *, overlay_alpha: float = 0.55,
                          animate: bool = True, duration: float = 0.25):
        try:
            p = max(0.0, min(1.0, float(progress)))
        except Exception:
            p = 0.0
        self._dark_prog = p
        self._dark_target_alpha = float(overlay_alpha)

        top_a = max(0.0, min(1.0, self._dark_target_alpha * p))
        bot_a = max(0.0, min(1.0, self._dark_target_alpha * p))

        if animate:
            try:
                Animation.cancel_all(self._top_dark_col, 'a')
                Animation.cancel_all(self._bot_dark_col, 'a')
            except Exception:
                pass
            Animation(a=top_a, d=duration, t='out_quad').start(self._top_dark_col)
            Animation(a=bot_a, d=duration, t='out_quad').start(self._bot_dark_col)
        else:
            self._top_dark_col.a = top_a
            self._bot_dark_col.a = bot_a

    def set_title(self, text: str, subtitle: str | None = None):
        if subtitle:
            self._title.text = f"[b]{text}[/b]  [size={int(sp(14))}]{subtitle}[/size]"
        else:
            self._title.text = f"[b]{text}[/b]"
        self._layout_title()
        self._sync()

    def update_layout(self):
        """
        Use the entire leftover height to frame a TRUE 9:16 safe area.
        - If taller than 9:16: split leftover evenly into top/bottom bands.
        - If exactly 9:16 (or shorter): bands collapse to 0px.
        """
        w, h = self.width, self.height
        if w <= 0 or h <= 0:
            return

        ideal_safe_h = w * (16.0 / 9.0)
        leftover = h - ideal_safe_h

        if leftover >= 0:
            # Perfect: all leftover becomes bands â†’ safe area is exactly 9:16.
            br = leftover / 2.0
        else:
            # Not enough height to preserve a 9:16 between bands â†’ no bands.
            br = 0.0

        self.top.height = br
        self.bottom.height = br
        self.top.y = h - br
        self.bottom.y = 0

        self._layout_title()
        self._sync()

    def get_safe_rect(self) -> tuple[float, float, float, float]:
        """
        Return (x, y, w, h) of the viewport BETWEEN the bands.
        On a 9:16 screen, bands are 0px and this returns the full window.
        """
        w, h = self.width, self.height
        safe_h = max(0.0, h - (self.top.height + self.bottom.height))
        y = self.bottom.height
        return (0.0, y, w, safe_h)

    def morph_to(self, letterbox_height: float, duration: float = 0.25):
        """
        Animate bands to a specific height (in px) for cinematic letterboxing.
        Pass 0 to collapse bands.
        """
        target = max(0.0, float(letterbox_height))
        a1 = Animation(height=target, d=duration)
        a2 = Animation(height=target, d=duration)
        a1.bind(on_complete=lambda *_: self._sync())
        a2.bind(on_complete=lambda *_: self._sync())
        a1.start(self.top)
        a2.start(self.bottom)

    # ---------------------- Internals ----------------------

    def _sync(self, *args):
        # Band rectangles
        self._top_rect.pos, self._top_rect.size = self.top.pos, self.top.size
        self._bot_rect.pos, self._bot_rect.size = self.bottom.pos, self.bottom.size
        # Blur widgets track bars
        if hasattr(self, '_top_blur'):
            self._top_blur.pos, self._top_blur.size = self.top.pos, self.top.size
        if hasattr(self, '_bot_blur'):
            self._bot_blur.pos, self._bot_blur.size = self.bottom.pos, self.bottom.size
        # Dark overlay widgets track bars (their own binds update rects)
        if hasattr(self, '_top_dark_w'):
            self._top_dark_w.pos, self._top_dark_w.size = (0, 0), self.top.size
        if hasattr(self, '_bot_dark_w'):
            self._bot_dark_w.pos, self._bot_dark_w.size = (0, 0), self.bottom.size

        # Seams at inner edges
        self._top_hi_rect.pos = (self.x, self.top.y)
        self._top_hi_rect.size = (self.width, dp(1))
        self._top_sh_rect.pos = (self.x, self.top.y - dp(2))
        self._top_sh_rect.size = (self.width, dp(2))

        self._bot_sh_rect.pos = (self.x, self.bottom.top)
        self._bot_sh_rect.size = (self.width, dp(2))
        self._bot_hi_rect.pos = (self.x, self.bottom.top - dp(1))
        self._bot_hi_rect.size = (self.width, dp(1))

    def _layout_title(self):
        # Left padding & vertical centering in the top band
        self._title.texture_update()
        th = self._title.texture_size[1] or sp(20)
        self._title.size = (min(self.width * 0.9, self._title.texture_size[0] + dp(20)), th)
        self._title.pos = (dp(16), self.top.y + (self.top.height - th) / 2)

    # ---------------------- Blur extension ----------------------
    def update_blur_from(self, widget) -> None:
        """
        Sample the full on-screen background (including overlays/shaders)
        so the bands reflect the final look. Uses a downsampled snapshot for
        performance instead of grabbing the raw layer texture.
        """
        try:
            tex = ui_snapshot(widget, downsample=2)
            if not tex:
                return
            try:
                tex.wrap = 'clamp_to_edge'
            except Exception:
                pass
            if hasattr(self, '_top_blur'):
                self._top_blur.texture = tex
            if hasattr(self, '_bot_blur'):
                self._bot_blur.texture = tex
            self._sync()
            # NEW: auto-bind band darkness to the environment loader's
            # dark_progress so bands darken in sync without external calls.
            self._bind_env_dark(widget)
        except Exception:
            return
        