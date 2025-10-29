# vfx.py — Starborn programmatic visual effects (split out of cinematics.py)
from __future__ import annotations

from typing import Callable, Optional, Tuple
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.label import Label
from kivy.graphics import Color, Rectangle
from kivy.graphics import Ellipse, StencilPush, StencilUse, StencilPop
from kivy.animation import Animation
from kivy.clock import Clock
from kivy.metrics import dp, sp
from kivy.app import App

import random
from font_manager import fonts
from particles import ParticleEmitter
from ui.background_snapshot import snapshot as ui_snapshot

class _TouchBlocker(FloatLayout):
    """Overlay that can optionally notify a click handler while swallowing input."""
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._click_handler: Optional[Callable] = None

    def set_click_handler(self, fn: Optional[Callable]):
        self._click_handler = fn

    def on_touch_down(self, touch):
        # Always swallow while active; optionally notify
        if self._click_handler:
            self._click_handler()
        return True

class VFX:
    """
    Centralized, programmatic effects service.
    Owns the fade rect, letterbox bars, and camera helpers.
    Safe to call directly from gameplay, or indirectly via CutsceneRunner.
    """
    def __init__(self, ui_root):
        self.ui = ui_root
        # Try to keep a handle to the App for camera ops fallback
        try:
            self._app = App.get_running_app()
        except Exception:
            self._app = None
        # --- overlay & color rect (used for fade AND color filters) ---
        self.overlay = _TouchBlocker(size_hint=(1, 1), pos=(0, 0), opacity=1)
        with self.overlay.canvas:
            self._fade_color = Color(0, 0, 0, 0)
            self._fade_rect = Rectangle(pos=self.overlay.pos, size=self.overlay.size)

        # --- letterbox bars (top & bottom) ---
        self.letterbox_top = FloatLayout(size_hint=(1, None), height=0, pos=(0, 0))
        with self.letterbox_top.canvas:
            self._top_color = Color(0, 0, 0, 1)
            self._top_rect = Rectangle(pos=self.letterbox_top.pos, size=self.letterbox_top.size)

        self.letterbox_bottom = FloatLayout(size_hint=(1, None), height=0, pos=(0, 0))
        with self.letterbox_bottom.canvas:
            self._bot_color = Color(0, 0, 0, 1)
            self._bot_rect = Rectangle(pos=self.letterbox_bottom.pos, size=self.letterbox_bottom.size)

        # Bind rects to widget geometry
        self.overlay.bind(pos=self._sync_canvases, size=self._sync_canvases)
        self.letterbox_top.bind(pos=self._sync_canvases, size=self._sync_canvases)
        self.letterbox_bottom.bind(pos=self._sync_canvases, size=self._sync_canvases)
        self.ui.bind(pos=self._sync_canvases, size=self._sync_canvases)

        # Camera baseline
        self._base_pos: Tuple[float, float] = (0, 0)
        self._base_scale: float = 1.0

        # Active shakes
        self._shake_ev = None

        # Track temporarily hidden widgets (id -> (widget, old_opacity, old_disabled))
        self._hidden_widgets = {}

    def _recenter_scatter(self):
        try:
            from kivy.core.window import Window
            app = self._app or App.get_running_app()
            if hasattr(app, "_on_resize"):
                app._on_resize(Window, *Window.size)
        except Exception:
            pass

    def _get_scatter(self):
        """Return the Scatter used for camera transforms.
        Prefers ui._scatter; falls back to App._scatter if present.
        """
        sc = getattr(self.ui, "_scatter", None)
        if sc is None and self._app is None:
            try:
                self._app = App.get_running_app()
            except Exception:
                self._app = None
        if sc is None and self._app is not None:
            sc = getattr(self._app, "_scatter", None)
        return sc

    # ------------------------------- lifecycle -------------------------------
    def enter_cinematic(self):
        """Lock input and attach overlay widgets."""
        if getattr(self.ui, "input_locked", False) is False:
            self.ui.input_locked = True

        # Save camera baseline if present
        scatter = self._get_scatter()
        if scatter is not None:
            self._base_pos = tuple(scatter.pos)
            self._base_scale = float(scatter.scale)

        # Attach
        if self.overlay.parent is None:
            self.ui.add_widget(self.overlay)
        if self.letterbox_top.parent is None:
            self.ui.add_widget(self.letterbox_top)
        if self.letterbox_bottom.parent is None:
            self.ui.add_widget(self.letterbox_bottom)
        self._sync_canvases()

        # Hide intrusive gameplay UI (menu button, minimap, etc.) during cinematics
        try:
            ids = getattr(self.ui, 'ids', {}) or {}
            for key in ('menu_button', 'minimap'):
                if key in ids and ids[key] is not None:
                    w = ids[key]
                    # store once
                    if key not in self._hidden_widgets:
                        self._hidden_widgets[key] = (w, getattr(w, 'opacity', 1), getattr(w, 'disabled', False))
                    # hide
                    if hasattr(w, 'opacity'):
                        w.opacity = 0
                    if hasattr(w, 'disabled'):
                        w.disabled = True
        except Exception:
            pass

    def exit_cinematic(self, restore_camera: bool = True):
        """Restore camera and remove overlay widgets."""
        # stop shakes
        if self._shake_ev:
            Clock.unschedule(self._shake_ev)
            self._shake_ev = None

        scatter = self._get_scatter()
        if restore_camera and scatter is not None:
            Animation.cancel_all(scatter)
            Animation(pos=self._base_pos, scale=self._base_scale, duration=0.2).start(scatter)
            # Ensure exact recentring in case of any accumulated drift
            Clock.schedule_once(lambda *_: self._recenter_scatter(), 0.22)

        for w in (self.overlay, self.letterbox_top, self.letterbox_bottom):
            if w.parent is not None:
                self.ui.remove_widget(w)
        # Ensure overlay is fully cleared for next use
        try:
            self._fade_color.a = 0.0
        except Exception:
            pass
        self.ui.input_locked = False

        # Restore any widgets we hid in enter_cinematic
        try:
            for key, (w, old_op, old_dis) in list(self._hidden_widgets.items()):
                if hasattr(w, 'opacity'):
                    w.opacity = old_op
                if hasattr(w, 'disabled'):
                    w.disabled = old_dis
            self._hidden_widgets.clear()
        except Exception:
            pass

    def _sync_canvases(self, *args):
        # overlay
        self.overlay.pos = self.ui.pos
        self.overlay.size = self.ui.size
        self._fade_rect.pos = self.overlay.pos
        self._fade_rect.size = self.overlay.size
        # top bar sits at top
        self.letterbox_top.width = self.ui.width
        self.letterbox_top.y = self.ui.top - (self.letterbox_top.height / 2)
        self._top_rect.pos = self.letterbox_top.pos
        self._top_rect.size = self.letterbox_top.size
        # bottom bar sits at bottom
        self.letterbox_bottom.width = self.ui.width
        self.letterbox_bottom.y = self.ui.y
        self._bot_rect.pos = self.letterbox_bottom.pos
        self._bot_rect.size = self.letterbox_bottom.size

    # ------------------------------ click-to-continue -------------------------
    def set_wait_for_click(self, enabled: bool, callback: Optional[Callable] = None):
        """Enable/disable click-to-continue. While enabled the overlay swallows touches and notifies callback."""
        self.overlay.set_click_handler(callback if enabled else None)

    # --------------------------------- effects --------------------------------
    def fade(self, alpha: float, duration: float = 0.5, color=(0, 0, 0), on_complete=None, t: str = 'linear'):
        self._fade_color.r, self._fade_color.g, self._fade_color.b = color
        anim = Animation(a=float(alpha), duration=max(0.0, duration), t=t)
        if on_complete:
            anim.bind(on_complete=lambda *_: on_complete())
        anim.start(self._fade_color)

    def letterbox(self, height: float, duration: float = 0.3, on_complete=None, t: str = 'out_quad'):
        """Animate bars; if ThemeBands exist, morph them instead for a native look."""
        height = max(0.0, float(height))
        try:
            bands = getattr(self.ui, 'bands', None)
            if bands is None:
                # try app-level bands (common when VFX ui != App root)
                app = self._app or App.get_running_app()
                bands = getattr(app, 'bands', None)
        except Exception:
            bands = None

        if bands is not None:
            bands.morph_to(height, duration)
            if on_complete:
                Clock.schedule_once(lambda *_: on_complete(), max(0.0, duration))
            return

        # Fallback to internal bars
        a1 = Animation(height=height, duration=duration, t=t)
        a2 = Animation(height=height, duration=duration, t=t)
        if on_complete:
            a2.bind(on_complete=lambda *_: on_complete())
        a1.start(self.letterbox_top)
        a2.start(self.letterbox_bottom)

    def flash(self, color=(1, 1, 1), duration: float = 0.15, on_complete=None):
        # alpha 1 -> 0
        self._fade_color.r, self._fade_color.g, self._fade_color.b = color
        self._fade_color.a = 1.0
        anim = Animation(a=0.0, duration=max(0.0, duration))
        if on_complete:
            anim.bind(on_complete=lambda *_: on_complete())
        anim.start(self._fade_color)

    def color_filter(self, color=(0, 0, 0), alpha: float = 1.0, duration: float = 0.5, on_complete=None, t: str='linear'):
        self._fade_color.r, self._fade_color.g, self._fade_color.b = color
        anim = Animation(a=float(alpha), duration=max(0.0, duration), t=t)
        if on_complete:
            anim.bind(on_complete=lambda *_: on_complete())
        anim.start(self._fade_color)

    def flashback_overlay(self, alpha: float = 0.35, duration: float = 0.4, on_complete=None):
        # warm sepia tone
        self.color_filter((0.75, 0.65, 0.5), alpha=alpha, duration=duration, on_complete=on_complete)

    def screen_shake(self, magnitude: float = 10.0, duration: float = 0.35, axis: str = 'both', on_complete=None):
        """Simple time-based shake that jitters the Scatter."""
        scatter = self._get_scatter()
        if scatter is None:
            if on_complete: on_complete()
            return

        # stop any existing shake
        if self._shake_ev:
            Clock.unschedule(self._shake_ev)
            self._shake_ev = None

        base_x, base_y = scatter.pos

        elapsed = 0.0
        def _tick(dt):
            nonlocal elapsed
            elapsed += dt
            p = max(0.0, 1.0 - (elapsed / duration))
            amp = magnitude * p
            dx = random.uniform(-amp, amp) if axis in ('both', 'x') else 0.0
            dy = random.uniform(-amp, amp) if axis in ('both', 'y') else 0.0
            scatter.pos = (base_x + dx, base_y + dy)
            if elapsed >= duration:
                # restore and finish
                scatter.pos = (base_x, base_y)
                Clock.unschedule(self._shake_ev)
                self._shake_ev = None
                if on_complete:
                    on_complete()

        self._shake_ev = Clock.schedule_interval(_tick, 1/60.0)

    def camera_zoom(self, scale: float, duration: float = 1.0, t: str = 'in_out_quad', pos_hint: Optional[Tuple[float,float]] = None, on_complete=None):
        scatter = self._get_scatter()
        if scatter is None:
            if on_complete: on_complete()
            return
        Animation.cancel_all(scatter)
        # If pos_hint given, center the given ui-relative (x,y) after zoom
        if pos_hint is not None:
            # ui coordinates
            ux, uy = self.ui.pos
            uw, uh = self.ui.size
            target_x = ux + pos_hint[0] * uw
            target_y = uy + pos_hint[1] * uh
            # compute scatter pos so that target point appears centered
            bw, bh = getattr(self.ui, "BASE_WIDTH", 480), getattr(self.ui, "BASE_HEIGHT", 854)
            # after scaling, the content size is bw*scale x bh*scale
            def _apply_center(*_):
                sw, sh = bw * scale, bh * scale
                x = target_x - sw/2
                y = target_y - sh/2
                Animation(pos=(x, y), scale=scale, duration=duration, t=t).start(scatter)
                if on_complete:
                    # chain completion on the same anim
                    pass
            # schedule apply on next frame to ensure ui size known
            Clock.schedule_once(_apply_center, 0)
        else:
            anim = Animation(scale=scale, duration=duration, t=t)
            if on_complete:
                anim.bind(on_complete=lambda *_: on_complete())
            anim.start(scatter)

    def camera_pan(self, dx: float = 0.0, dy: float = 0.0, duration: float = 1.0, t: str='in_out_quad', on_complete=None):
        scatter = self._get_scatter()
        if scatter is None:
            if on_complete: on_complete()
            return
        target = (scatter.x + dx, scatter.y + dy)
        anim = Animation(pos=target, duration=duration, t=t)
        if on_complete:
            anim.bind(on_complete=lambda *_: on_complete())
        anim.start(scatter)

    def caption(self, text: str, duration: float = 1.5, on_complete=None):
        # Pick a safe font from the registry; never allow None for font_name
        fdef = (
            fonts.get('narration_text')
            or fonts.get('overlay_header')
            or fonts.get('menu_title')
            or fonts.get('dialogue_text')
            or fonts.get('medium_text')
            or {"name": "Roboto-Regular", "size": sp(22)}
        )
        fname = fdef.get('name', 'Roboto-Regular')
        fsize = fdef.get('size', sp(22))

        lbl = Label(
            text=text,
            size_hint=(None, None),
            halign='center',
            valign='middle',
            font_name=fname,
            font_size=fsize,
            color=(1,1,1,1),
            markup=True,
            opacity=0.0
        )
        # center label and autosize
        def _layout(*_):
            lbl.texture_update()
            lbl.size = (lbl.texture_size[0] + dp(24), lbl.texture_size[1] + dp(12))
            # --- FIX: Position relative to Window, not self.ui ---
            from kivy.core.window import Window
            lbl.center_x = Window.center_x
            lbl.center_y = Window.center_y
            lbl.opacity = 1.0
        Clock.schedule_once(_layout, 0)
        self.ui.add_widget(lbl)

        def _fade_out(*_):
            a = Animation(opacity=0.0, duration=0.3)
            def _rm(*__):
                if lbl.parent: self.ui.remove_widget(lbl)
                if on_complete: on_complete()
            a.bind(on_complete=_rm)
            a.start(lbl)

        Clock.schedule_once(lambda *_: _fade_out(), max(0.0, duration))

    def particles(self, config: dict, on_complete=None):
        # Friendly wrapper around ParticleEmitter.start(config)
        cfg = dict(config or {})

        # Presets -> concrete defaults
        em_cfg = {}
        preset = cfg.pop('preset', None)
        if preset == 'ember_sparkle':
            em_cfg.update({
                'count': 80,
                'life': (0.6, 1.2),
                'size': (2, 5),
                'velocity': (40, 90),
                'angle': (80, 100),
                'color': [1.0, 0.55, 0.2],
            })
        elif preset == 'glitch_static':
            em_cfg.update({
                'count': 120,
                'life': (0.25, 0.5),
                'size': (1, 3),
                'velocity': (5, 20),
                'angle': (0, 360),
                'color': [0.6, 0.85, 1.0],
            })

        # Allow explicit overrides in config
        for k in ('count', 'life', 'size', 'velocity', 'angle', 'color', 'pos', 'pos_hint'):
            if k in cfg:
                em_cfg[k] = cfg[k]

        # Scale particle size/velocity for current UI scale and provide a bigger default look.
        # This compensates for the Screen Scatter scaling (which often shrinks content),
        # and makes particles easier to see by default. Cinematics can override via cfg['scale'].
        try:
            scatter = self._get_scatter()
            scatter_scale = float(getattr(scatter, 'scale', 1.0) or 1.0)
        except Exception:
            scatter_scale = 1.0

        # Base multiplier: inverse scatter to cancel global shrink, then a small visual boost.
        # Allow explicit override in config with key 'scale'.
        size_mult = 1.0
        # Cancel scatter shrink (e.g., scale=0.375 -> x2.666)
        if scatter_scale > 0:
            size_mult *= (1.0 / scatter_scale)
        # Optional explicit scale from config
        explicit_mult = cfg.pop('scale', None)
        if explicit_mult is not None:
            try:
                size_mult *= float(explicit_mult)
            except Exception:
                pass
        else:
            # Provide a gentle default boost so particles read well on small screens
            size_mult *= 1.8

        def _mul_range(v, m):
            try:
                # tuple/list range (min, max)
                if isinstance(v, (list, tuple)) and len(v) >= 2:
                    return (float(v[0]) * m, float(v[1]) * m)
                # single number
                return float(v) * m
            except Exception:
                return v

        if 'size' in em_cfg:
            em_cfg['size'] = _mul_range(em_cfg['size'], size_mult)
        if 'velocity' in em_cfg:
            em_cfg['velocity'] = _mul_range(em_cfg['velocity'], size_mult)

        # Translate pos_hint to absolute ui coordinates
        if 'pos_hint' in em_cfg and isinstance(em_cfg['pos_hint'], (list, tuple)) and len(em_cfg['pos_hint']) == 2:
            px, py = em_cfg['pos_hint']
            x = self.ui.x + float(px) * self.ui.width
            y = self.ui.y + float(py) * self.ui.height
            em_cfg['pos'] = (x, y)
            em_cfg.pop('pos_hint', None)

        emitter = ParticleEmitter()
        self.ui.add_widget(emitter)
        try:
            emitter.start(em_cfg)
        except Exception:
            try:
                if emitter.parent:
                    self.ui.remove_widget(emitter)
            except Exception:
                pass
            if on_complete:
                on_complete()
            return

        # Cleanup timing: ttl override, else estimate from life max
        ttl = None
        try:
            ttl = float(cfg.get('ttl')) if cfg.get('ttl') is not None else None
        except Exception:
            ttl = None
        if ttl is None:
            life = em_cfg.get('life', (1.0, 1.0))
            life_max = life[1] if isinstance(life, (list, tuple)) and len(life) >= 2 else 1.0
            ttl = float(life_max) + 0.2

        def _cleanup(*_):
            try:
                emitter.stop()
            except Exception:
                pass
            try:
                if emitter.parent:
                    self.ui.remove_widget(emitter)
            except Exception:
                pass
            if on_complete:
                on_complete()

        Clock.schedule_once(_cleanup, max(0.0, ttl))

    # ------------------------------ shockwave pulse -------------------------
    def shockwave(self,
                  *,
                  duration: float = 0.4,
                  start_scale: float = 0.98,
                  end_scale: float = 1.25,
                  alpha: float = 0.45,
                  on_complete=None):
        """Expanding snapshot pulse from screen center."""
        tex = ui_snapshot(self.ui, downsample=1)
        lay = FloatLayout(size_hint=(1, 1), pos=(0, 0), opacity=1.0)
        self.ui.add_widget(lay)
        img = FloatLayout(size_hint=(None, None))
        with img.canvas:
            col = Color(1, 1, 1, alpha)
            rect = Rectangle(texture=tex, pos=self.ui.pos, size=self.ui.size)

        # helper to resize around center
        def _apply_scale(scale):
            w = self.ui.width * scale
            h = self.ui.height * scale
            x = self.ui.center_x - w / 2
            y = self.ui.center_y - h / 2
            rect.pos = (x, y)
            rect.size = (w, h)

        _apply_scale(start_scale)
        lay.add_widget(img)

        def _on_progress(anim, widget, prog):
            _apply_scale(start_scale + (end_scale - start_scale) * float(prog))

        a = Animation(d=duration, t='out_quad')
        a.bind(on_progress=_on_progress)
        def _fade_out(*_):
            Animation(a=0.0, duration=0.18).start(col)
            Animation(opacity=0.0, duration=0.18).start(lay)
        a.bind(on_complete=_fade_out)

        def _done(*_):
            if lay.parent: self.ui.remove_widget(lay)
            if on_complete: on_complete()
        Animation(opacity=0.0, duration=max(0.0, duration) + 0.22).bind(on_complete=lambda *_: _done()).start(lay)
        a.start(lay)

    # ------------------------------ chromatic glitch -----------------------
    def chromatic_glitch(self,
                         *,
                         duration: float = 0.18,
                         jitter: float = dp(2),
                         on_complete=None):
        tex = ui_snapshot(self.ui, downsample=1)
        lay = FloatLayout(size_hint=(1, 1), pos=(0, 0), opacity=1.0)
        self.ui.add_widget(lay)

        channels = [(1, 0, 0, 0.7), (0, 1, 0, 0.6), (0, 0, 1, 0.6)]
        rects = []
        for r, g, b, a in channels:
            fl = FloatLayout(size_hint=(None, None))
            with fl.canvas:
                Color(r, g, b, a)
                rect = Rectangle(texture=tex, pos=self.ui.pos, size=self.ui.size)
            rects.append((fl, rect))
            lay.add_widget(fl)

        def _jitter(*_):
            for fl, rect in rects:
                ox = random.uniform(-jitter, jitter)
                oy = random.uniform(-jitter, jitter)
                rect.pos = (self.ui.x + ox, self.ui.y + oy)
        # jitter a few times
        steps = max(2, int(duration / 0.04))
        for i in range(steps):
            Clock.schedule_once(_jitter, i * 0.04)

        def _cleanup(*_):
            if lay.parent: self.ui.remove_widget(lay)
            if on_complete: on_complete()
        Animation(opacity=0.0, duration=max(0.05, duration)).bind(on_complete=lambda *_: _cleanup()).start(lay)

    # ------------------------------ afterimages -----------------------------
    def afterimages(self, widget,
                    *,
                    count: int = 4,
                    interval: float = 0.035,
                    life: float = 0.22,
                    color=(1, 1, 1, 0.5),
                    offset=(0.0, 0.0)):
        """Spawn ghost images trailing a moving widget (Image or any exportable widget)."""
        def _make_ghost(*_):
            try:
                tex = getattr(widget, 'texture', None)
                if tex is None:
                    tex = widget.export_as_image().texture
            except Exception:
                return
            # position in UI coords
            wx, wy = widget.to_window(widget.center_x, widget.center_y)
            ux, uy = self.ui.to_widget(wx, wy)
            img = FloatLayout(size_hint=(None, None), opacity=1.0)
            with img.canvas:
                Color(*color)
                rect = Rectangle(texture=tex)
            w, h = getattr(widget, 'width', tex.width), getattr(widget, 'height', tex.height)
            rect.size = (w, h)
            rect.pos = (ux - w / 2 + offset[0], uy - h / 2 + offset[1])
            self.ui.add_widget(img)
            Animation(opacity=0.0, duration=max(0.05, life)).bind(on_complete=lambda *_: (self.ui.remove_widget(img) if img.parent else None)).start(img)

        for i in range(max(1, int(count))):
            Clock.schedule_once(_make_ghost, i * max(0.0, float(interval)))

    # ------------------------------ depth focus -----------------------------
    def depth_focus(self,
                    *,
                    center: tuple[float, float] | None = None,  # pos_hint (0..1, 0..1)
                    radius: float = dp(120),
                    blur: int = 4,
                    dim_alpha: float = 0.0,
                    duration: float = 0.8,
                    fade_in: float = 0.15,
                    fade_out: float = 0.25,
                    on_complete=None):
        """Blur everything except a circular sharp region."""
        sharp = ui_snapshot(self.ui, downsample=1)
        blur_tex = ui_snapshot(self.ui, downsample=max(1, int(blur)))

        lay = FloatLayout(size_hint=(1, 1), pos=(0, 0), opacity=0.0)
        self.ui.add_widget(lay)

        with lay.canvas:
            # blurred full-screen
            Color(1, 1, 1, 1)
            Rectangle(texture=blur_tex, pos=self.ui.pos, size=self.ui.size)
            # optional dim
            if dim_alpha > 0:
                Color(0, 0, 0, dim_alpha)
                Rectangle(pos=self.ui.pos, size=self.ui.size)
            # stencil: sharp circle
            StencilPush()
            Color(1, 1, 1, 1)
            if center is None:
                cx, cy = self.ui.center
            else:
                cx = self.ui.x + self.ui.width * float(center[0])
                cy = self.ui.y + self.ui.height * float(center[1])
            Ellipse(pos=(cx - radius, cy - radius), size=(radius * 2, radius * 2))
            StencilUse()
            Color(1, 1, 1, 1)
            Rectangle(texture=sharp, pos=self.ui.pos, size=self.ui.size)
            StencilPop()

        Animation(opacity=1.0, duration=max(0.0, float(fade_in))).start(lay)
        def _finish(*_):
            def _cleanup(*__):
                if lay.parent: self.ui.remove_widget(lay)
                if on_complete: on_complete()
            Animation(opacity=0.0, duration=max(0.0, float(fade_out))).bind(on_complete=lambda *_: _cleanup()).start(lay)
        Clock.schedule_once(_finish, max(0.0, float(duration)))

    # ------------------------------ ambient weather -------------------------
    def weather_rain(self,
                     *,
                     duration: float = 2.0,
                     intensity: str = 'medium',
                     drift: float = 40.0,
                     color=(0.75, 0.9, 1.0)):
        w = self.ui.width; h = self.ui.height
        rates = {'low': 80, 'medium': 160, 'high': 260}
        rate = rates.get(intensity, 160)
        cfg = {
            'count': 0,
            'emit_rate': rate,
            'one_shot': False,
            'life': (0.8, 1.4),
            'size': (1.8, 3.0),
            'spawn_area': (self.ui.x, self.ui.y + h - dp(8), w, dp(8)),
            'velocity': (180, 260),
            'angle': (250, 290),
            'gravity': (0, -420),
            'drag': 0.2,
            'color': color,
            'ttl': duration,
        }
        self.particles(cfg)

    def weather_snow(self,
                      *,
                      duration: float = 3.0,
                      intensity: str = 'low',
                      color=(0.98, 0.98, 1.0)):
        w = self.ui.width; h = self.ui.height
        rates = {'low': 40, 'medium': 80, 'high': 140}
        rate = rates.get(intensity, 80)
        cfg = {
            'count': 0,
            'emit_rate': rate,
            'one_shot': False,
            'life': (2.2, 3.5),
            'size': (2.5, 4.5),
            'spawn_area': (self.ui.x, self.ui.y + h - dp(10), w, dp(10)),
            'velocity': (20, 40),
            'angle': (260, 280),
            'gravity': (0, -60),
            'drag': 0.05,
            'color': color,
            'ttl': duration,
        }
        self.particles(cfg)

    # ------------------------------ hit stop (light) -----------------------
    def hit_stop(self,
                 *,
                 duration: float = 0.12,
                 zoom: float = 1.03,
                 on_complete=None):
        """Quick camera pop-in and back; lightweight alternative to full pause."""
        scatter = self._get_scatter()
        if scatter is None:
            if on_complete: on_complete()
            return
        # cancel any running camera anims
        Animation.cancel_all(scatter)
        a = Animation(scale=zoom, duration=max(0.0, duration) * 0.4, t='out_quad')
        b = Animation(scale=self._base_scale, duration=max(0.0, duration) * 0.6, t='in_quad')
        def _done(*_):
            if on_complete: on_complete()
        (a + b).bind(on_complete=_done).start(scatter)

    # ------------------------------- narration panel ------------------------
    def narration(self,
                  text: str,
                  *,
                  duration: float = 2.0,
                  position: str = 'center',   # 'bottom' | 'center' | 'top'
                  width: float = 0.82,        # fraction of ui width
                  fade_in: float = 0.18,
                  fade_out: float = 0.25,
                  on_complete=None):
        """
        Cinematic-friendly text card with a translucent panel.

        - Does not use DialogueBox; no portrait or name
        - Picks theme colors from the game when available
        - Fades in/out automatically, then calls on_complete
        """
        # Theme colors (safe fallbacks)
        try:
            tm = getattr(self.ui, 'themes', None)
            bg = getattr(self.ui, 'theme_bg', None) or (tm and tm.col('bg')) or (0.05, 0.06, 0.14, 0.98)
            fg = getattr(self.ui, 'theme_fg', None) or (tm and tm.col('fg')) or (0.92, 0.94, 1.0, 1.0)
            border = getattr(self.ui, 'theme_border', None) or (tm and tm.col('border')) or (0.5, 0.8, 1.0, 1.0)
        except Exception:
            bg, fg, border = (0.05, 0.06, 0.14, 0.98), (1, 1, 1, 1), (0.6, 0.6, 0.9, 1)

        # font selection (robust)
        fdef = (
            fonts.get('overlay_header') or fonts.get('menu_title') or
            fonts.get('dialogue_text') or fonts.get('medium_text') or
            {"name": "Roboto-Regular", "size": sp(22)}
        )
        fname = fdef.get('name', 'Roboto-Regular')
        fsize = fdef.get('size', sp(22))

        # container panel
        panel = FloatLayout(size_hint=(None, None), opacity=0.0)
        self.ui.add_widget(panel)

        # background rounded rectangle
        from kivy.graphics import RoundedRectangle
        with panel.canvas.before:
            self._n_bg_color = Color(bg[0], bg[1], bg[2], 0.82)
            self._n_bg_rect  = RoundedRectangle(pos=panel.pos, size=panel.size, radius=[dp(12)])
            self._n_bd_color = Color(border[0], border[1], border[2], 1.0)
            self._n_bd_rect  = RoundedRectangle(pos=panel.pos, size=panel.size, radius=[dp(12)])

        # text label (markup enabled so cinematics can use [i] etc.)
        # allow a touch of accent in text via color tag if none is present
        accent = None
        try:
            tm = getattr(self.ui, 'themes', None)
            accent = tm and tm.col('accent')
        except Exception:
            accent = None
        raw = text or ''
        if '[color=' not in raw and accent:
            r,g,b,a = accent
            hexcol = '#%02x%02x%02x' % (int(r*255), int(g*255), int(b*255))
            raw = f"[color={hexcol}]{raw}[/color]"

        lbl = Label(text=raw, markup=True,
                    font_name=fname, font_size=fsize,
                    halign='center', valign='middle',
                    color=fg,
                    size_hint=(None, None))

        panel.add_widget(lbl)

        def _layout(*_):
            # compute panel size from text + padding
            pad_x, pad_y = dp(28), dp(18)
            max_w = self.ui.width * max(0.4, min(0.96, width))
            # Wrap to max_w and expand height accordingly
            lbl.text_size = (max_w - pad_x * 2, None)
            lbl.texture_update()
            w = max_w
            h = lbl.texture_size[1] + pad_y * 2
            # --- FIX: Position relative to Window, not self.ui ---
            from kivy.core.window import Window
            x = Window.center_x - w / 2
            if position == 'top':
                y = Window.height - h - dp(120)
            elif position == 'center':
                y = Window.center_y - h / 2
            else:  # bottom
                y = dp(140)
            panel.size = (w, h)
            panel.pos  = (x, y)
            lbl.size   = (w - pad_x * 2, h - pad_y * 2)
            lbl.pos    = (pad_x, pad_y)
            # sync bg/border rects
            self._n_bg_rect.pos  = panel.pos
            self._n_bg_rect.size = panel.size
            self._n_bd_rect.pos  = panel.pos
            self._n_bd_rect.size = panel.size

        # layout on next frame
        Clock.schedule_once(_layout, 0)

        # fade in → wait → fade out
        a_in  = Animation(opacity=1.0, duration=max(0.0, float(fade_in)))
        a_out = Animation(opacity=0.0, duration=max(0.0, float(fade_out)))

        def _after_in(*_):
            Clock.schedule_once(lambda *_: a_out.start(panel), max(0.0, float(duration)))

        def _after_out(*_):
            if panel.parent:
                self.ui.remove_widget(panel)
            if on_complete:
                on_complete()

        a_in.bind(on_complete=_after_in)
        a_out.bind(on_complete=_after_out)
        a_in.start(panel)

    # ------------------------------ cinematic text card ---------------------
    def cinematic_text(self,
                        text: str,
                        *,
                        duration: float = 2.2,
                        position: str = 'center',  # 'top' | 'center' | 'bottom'
                        width: float = 0.9,        # fraction of screen width
                        dim_alpha: float = 0.28,   # backdrop dim strength
                        on_complete=None):
        """Full-screen stylized text for cutscenes.

        - Dims the whole screen slightly
        - Centers a wide accent bar with animated text
        - Wraps text to the given width fraction
        """
        # Theme & font
        try:
            tm = getattr(self.ui, 'themes', None)
            fg = getattr(self.ui, 'theme_fg', None) or (tm and tm.col('fg')) or (1, 1, 1, 1)
            accent = None
            if tm:
                accent = tm.col('accent') if 'accent' in tm._cur else tm.col('border')
            if accent is None:
                accent = (0.56, 0.79, 1.0, 1.0)
        except Exception:
            fg, accent = (1, 1, 1, 1), (0.56, 0.79, 1.0, 1.0)

        fdef = (
            fonts.get('room_title') or fonts.get('overlay_header') or
            fonts.get('menu_title') or {"name": "Roboto-Bold", "size": sp(40)}
        )
        fname = fdef.get('name', 'Roboto-Bold')
        fsize = max(sp(28), fdef.get('size', sp(40)))

        # Root panel for this element
        root = FloatLayout(size_hint=(1, 1), pos=(0, 0), opacity=0.0)
        self.ui.add_widget(root)

        # Dim background layer
        from kivy.graphics import Rectangle
        with root.canvas.before:
            dim = Color(0, 0, 0, 0)
            dim_rect = Rectangle(pos=self.ui.pos, size=self.ui.size)

        # Accent bar (behind the text)
        bar = FloatLayout(size_hint=(None, None))
        root.add_widget(bar)
        with bar.canvas.before:
            # subtle accent with reduced alpha
            br, bg, bb, ba = accent
            bar_col = Color(br, bg, bb, min(0.35, ba))
            bar_rect = Rectangle(pos=bar.pos, size=bar.size)

        # Text label
        lbl = Label(text=text, markup=True, color=fg,
                    font_name=fname, font_size=fsize,
                    halign='center', valign='middle',
                    size_hint=(None, None))
        bar.add_widget(lbl)

        def _layout(*_):
            pad_x, pad_y = dp(32), dp(22)
            max_w = self.ui.width * max(0.5, min(0.98, width))
            # wrap to max_w and compute height
            lbl.text_size = (max_w - pad_x * 2, None)
            lbl.texture_update()
            w = max_w
            h = lbl.texture_size[1] + pad_y * 2
            x = self.ui.center_x - w/2
            if position == 'top':
                y = self.ui.top - h - dp(140)
            elif position == 'bottom':
                y = self.ui.y + dp(160)
            else:
                y = self.ui.center_y - h/2
            bar.size = (w, h)
            bar.pos  = (x, y)
            lbl.size = (w - pad_x * 2, h - pad_y * 2)
            lbl.pos  = (pad_x, pad_y)
            bar_rect.pos  = bar.pos
            bar_rect.size = bar.size
            dim_rect.pos  = self.ui.pos
            dim_rect.size = self.ui.size

        # ensure correct layout after attach
        Clock.schedule_once(_layout, 0)

        # Animations: dim in, root fade in, slight text scale pop
        a_dim = Animation(a=float(dim_alpha), duration=0.18)
        a_in  = Animation(opacity=1.0, duration=0.18)
        a_out = Animation(opacity=0.0, duration=0.28)

        a_dim.start(dim)
        a_in.start(root)

        def _finish(*_):
            # fade out and cleanup
            def _cleanup(*__):
                if root.parent:
                    self.ui.remove_widget(root)
                if on_complete:
                    on_complete()
            a_dim_out = Animation(a=0.0, duration=0.2)
            a_dim_out.bind(on_complete=lambda *_: None)
            a_dim_out.start(dim)
            a_out.bind(on_complete=lambda *_: _cleanup())
            a_out.start(root)

        Clock.schedule_once(_finish, max(0.0, float(duration)))

    # ------------------------------ ring flash / pulse ----------------------
    def ring(self,
             *,
             start_radius: float = 24.0,
             end_radius: float = 480.0,
             duration: float = 0.5,
             color=(1, 1, 1, 1),
             thickness: float = 3.0,
             on_complete=None):
        """Expanding ring (stroke) useful for emphasis/explosions."""
        w = FloatLayout(size_hint=(1, 1), pos=(0, 0))
        self.ui.add_widget(w)

        r = {'val': float(start_radius)}

        def _draw(*_):
            from kivy.graphics import Line
            w.canvas.clear()
            with w.canvas:
                Color(*color)
                cx, cy = self.ui.center
                Line(circle=(cx, cy, r['val']), width=thickness)

        _draw()
        total = max(0.0, float(duration))
        elapsed = 0.0

        def _tick(dt):
            nonlocal elapsed
            elapsed += dt
            p = min(1.0, elapsed / total)
            r['val'] = start_radius + (end_radius - start_radius) * p
            _draw()
            if p >= 1.0:
                Clock.unschedule(_tick)
                if w.parent: self.ui.remove_widget(w)
                if on_complete: on_complete()

        Clock.schedule_interval(_tick, 1/60.0)

    # ------------------------------ speed lines -----------------------------
    def speed_lines(self,
                    *,
                    count: int = 36,
                    duration: float = 0.5,
                    color=(1, 1, 1, 0.45),
                    thickness: float = 1.2,
                    on_complete=None):
        """Radial speed lines that quickly fade out."""
        lay = FloatLayout(size_hint=(1, 1), pos=(0, 0), opacity=1.0)
        self.ui.add_widget(lay)
        with lay.canvas:
            Color(*color)
            from math import sin, cos, pi
            from kivy.graphics import Line
            cx, cy = self.ui.center
            w, h = self.ui.width, self.ui.height
            radius = max(w, h)
            for i in range(max(1, int(count))):
                ang = i * (2 * 3.14159265 / max(1, int(count)))
                ex  = cx + cos(ang) * radius
                ey  = cy + sin(ang) * radius
                Line(points=[cx, cy, ex, ey], width=thickness)
        def _cleanup(*_):
            if lay.parent: self.ui.remove_widget(lay)
            if on_complete: on_complete()
        _anim = Animation(opacity=0.0, duration=max(0.0, float(duration)))
        _anim.bind(on_complete=lambda *_: _cleanup())
        _anim.start(lay)

    # ------------------------------ vignette edges --------------------------
    def vignette(self,
                 *,
                 alpha: float = 0.35,
                 inset: float = 120.0,
                 color=(0, 0, 0),
                 duration: float = 0.8,
                 fade_in: float = 0.18,
                 fade_out: float = 0.25,
                 on_complete=None):
        """Simple edge vignette using four soft rectangles."""
        vg = FloatLayout(size_hint=(1, 1), pos=(0, 0), opacity=0.0)
        self.ui.add_widget(vg)
        with vg.canvas:
            Color(color[0], color[1], color[2], alpha)
            top = Rectangle()
            bot = Rectangle()
            lef = Rectangle()
            rig = Rectangle()

        def _layout(*_):
            m = float(inset)
            x, y, w, h = self.ui.x, self.ui.y, self.ui.width, self.ui.height
            top.pos, top.size = (x, y + h - m), (w, m)
            bot.pos, bot.size = (x, y), (w, m)
            lef.pos, lef.size = (x, y + m), (m, h - m * 2)
            rig.pos, rig.size = (x + w - m, y + m), (m, h - m * 2)

        Clock.schedule_once(_layout, 0)
        Animation(opacity=1.0, duration=max(0.0, float(fade_in))).start(vg)
        def _finish(*_):
            def _cleanup(*__):
                if vg.parent: self.ui.remove_widget(vg)
                if on_complete: on_complete()
            _a = Animation(opacity=0.0, duration=max(0.0, float(fade_out)))
            _a.bind(on_complete=lambda *_: _cleanup())
            _a.start(vg)
        Clock.schedule_once(_finish, max(0.0, float(duration)))

    # ------------------------------ bar wipe --------------------------------
    def bar_wipe(self,
                 *,
                 orientation: str = 'vertical',  # 'vertical' (L→R) or 'horizontal' (T→B)
                 direction: str = 'in',          # 'in' covers screen, 'out' reveals screen
                 duration: float = 0.5,
                 color=(0, 0, 0, 1),
                 on_complete=None):
        """Single-bar wipe transition overlay."""
        w = FloatLayout(size_hint=(1, 1), pos=(0, 0))
        self.ui.add_widget(w)
        with w.canvas:
            Color(*color)
            rect = Rectangle(pos=w.pos, size=(0, 0))

        def _layout(*_):
            if orientation == 'horizontal':
                # Top to bottom
                if direction == 'in':
                    rect.pos = (self.ui.x, self.ui.top)
                    rect.size = (self.ui.width, 0)
                    Animation(size=(self.ui.width, self.ui.height), pos=(self.ui.x, self.ui.y), duration=duration).start(rect)
                else:
                    rect.pos = (self.ui.x, self.ui.y)
                    rect.size = (self.ui.width, self.ui.height)
                    Animation(size=(self.ui.width, 0), pos=(self.ui.x, self.ui.top), duration=duration).start(rect)
            else:
                # Left to right
                if direction == 'in':
                    rect.pos  = (self.ui.x, self.ui.y)
                    rect.size = (0, self.ui.height)
                    Animation(size=(self.ui.width, self.ui.height), duration=duration).start(rect)
                else:
                    rect.pos  = (self.ui.x, self.ui.y)
                    rect.size = (self.ui.width, self.ui.height)
                    Animation(size=(0, self.ui.height), duration=duration).start(rect)

        def _cleanup(*_):
            if w.parent: self.ui.remove_widget(w)
            if on_complete: on_complete()

        Clock.schedule_once(_layout, 0)
        Clock.schedule_once(_cleanup, max(0.0, float(duration)) + 0.01)

    # ------------------------------ camera tilt ------------------------------
    def camera_tilt(self, angle: float = 6.0, duration: float = 0.25, restore: bool = True, on_complete=None):
        scatter = self._get_scatter()
        if scatter is None:
            if on_complete: on_complete()
            return
        # Kivy Scatter has rotation; animate it
        a = Animation(rotation=angle, duration=max(0.0, float(duration)), t='out_quad')
        def _after(*_):
            if restore:
                _ar = Animation(rotation=0, duration=max(0.0, float(duration*0.6)), t='in_quad')
                _ar.bind(on_complete=lambda *_: on_complete and on_complete())
                _ar.start(scatter)
            else:
                if on_complete: on_complete()
        a.bind(on_complete=_after)
        a.start(scatter)
