# ui/combat_background.py
from __future__ import annotations
import random
from kivy.animation import Animation
from typing import Dict, Tuple, Optional
from kivy.uix.widget import Widget
from kivy.graphics import Color, Rectangle, Ellipse, Line, InstructionGroup, Mesh, PushMatrix, PopMatrix, Translate
from kivy.graphics.texture import Texture
from kivy.graphics.fbo import Fbo
from kivy.clock import Clock
from kivy.metrics import dp
from kivy.app import App as KivyApp
from kivy.properties    import NumericProperty

# --- NEW: Import the ParallaxStarfield from its own file ---
from generate_starfield import ParallaxStarfield

RGBA = Tuple[float, float, float, float]

def _lerp(a, b, t): return a + (b - a) * t
def _mix(c0, c1, t): return tuple(_lerp(c0[i], c1[i], t) for i in range(4))
def _rgba(v) -> RGBA: return (float(v[0]), float(v[1]), float(v[2]), float(v[3] if len(v)>3 else 1.0))

def _mk_vertical_gradient(h: int, top_rgba: RGBA, bot_rgba: RGBA) -> Texture:
    """Create a 1xH vertical gradient texture we can stretch."""
    import numpy as np
    arr = np.zeros((h, 1, 4), dtype=np.uint8)
    for y in range(h):
        t = y / max(1, h - 1)
        r = _lerp(top_rgba[0], bot_rgba[0], t)
        g = _lerp(top_rgba[1], bot_rgba[1], t)
        b = _lerp(top_rgba[2], bot_rgba[2], t)
        a = _lerp(top_rgba[3], bot_rgba[3], t)
        arr[y, 0] = (int(r*255), int(g*255), int(b*255), int(a*255))
    tex = Texture.create(size=(1, h))
    tex.blit_buffer(arr.tobytes(), colorfmt='rgba', bufferfmt='ubyte')
    tex.wrap = 'clamp_to_edge'
    tex.mag_filter = 'linear'; tex.min_filter = 'linear'
    return tex

def _scale_alpha(rgba, a):
    """Helper to scale an RGBA tuple's alpha component."""
    return (rgba[0], rgba[1], rgba[2], rgba[3] * max(0.0, min(1.0, a)))

def palette_from_room(room: Dict, themes: Dict) -> Dict[str, RGBA]:
    env = (room or {}).get("env") or "default"
    theme = themes.get(env) or themes.get(env.lower()) or themes.get("default") or {}
    bg     = _rgba(theme.get("bg",     [0.07,0.07,0.07,1]))
    fg     = _rgba(theme.get("fg",     [1,1,1,1]))
    border = _rgba(theme.get("border", fg))
    accent = _rgba(theme.get("accent", fg))
    return {"bg": bg, "fg": fg, "border": border, "accent": accent}

class CombatBackground(Widget):
    """
    A dynamic, theme-aware combat background with parallaxing starfield and nebula clouds.
    """
    # --- MODIFIED: Control the drift speed of the parallax bands ---
    def __init__(self, *, room: dict, themes_dict: dict,
                snapshot_tex: Optional[Texture] = None,
                enable_parallax=True, weather_layer=None, **kw):
        super().__init__(**kw)

        # Base state
        self.starfield = None  # ensure attribute always exists
        self.room = room
        self.themes_dict = themes_dict

        # Theme palette
        pal = palette_from_room(room, themes_dict)
        bg, fg, edge, accent = pal["bg"], pal["fg"], pal["border"], pal["accent"]

        # Gradient stays inside theme; no huge accent washes
        grad_top = _mix(bg, edge, 0.05)  # brighter gradient top
        grad_bot = _mix(bg, edge, 0.12)  # brighter gradient bottom
        self._gradient_tex = _mk_vertical_gradient(256, grad_top, grad_bot)

        # Silhouette tints (quiet)
        self.sil_far  = _mix(bg, edge, 0.15)  # brighter far grid
        self.sil_mid  = _mix(bg, edge, 0.22)  # brighter mid grid
        self.sil_near = _mix(bg, edge, 0.25)

        # --- Drawing groups in render order ---
        self._g_bg = InstructionGroup()
        self.canvas.add(self._g_bg)

        # Optional room snapshot under everything
        self._snapshot_tex = snapshot_tex
        self._grad_rect = None
        self._snap_rect = None

        # Resolve theme manager early so we can apply later
        env_name = (room or {}).get("env") or "default"
        app = KivyApp.get_running_app()
        themes = getattr(getattr(app, "current_game", None), "themes", {})

        # Starfield (child widget)
        if enable_parallax:
            # Stars draw as their own widget so parent Colors don't bleed in
            self.starfield = ParallaxStarfield()
            self.add_widget(self.starfield)

        # Optional atmospherics & HUD lanes (draw above stars)
        self._g_hudlanes = InstructionGroup()
        self.canvas.add(self._g_hudlanes)

        if weather_layer is not None:
            # tip: keep rain sparse; intensity < 0.35
            self.add_widget(weather_layer)

        # Vignette (soft, on top)
        self._g_vig = InstructionGroup()
        self.canvas.add(self._g_vig)

        # Initial layout & parallax bookkeeping
        from kivy.core.window import Window
        self.size = Window.size  # ensure initial layout
        self.bind(pos=self._layout, size=self._layout)
        self._offset_px = 0.0  # accumulated horizontal offset in pixels

        # External clock drives animation; no internal Clock.schedule_interval here

        # Apply theme to starfield and harmonize tint/alpha
        if self.starfield:
            # Theme colors now flow into each star/trail via ParallaxStarfield.apply_theme
            self.starfield.apply_theme(themes, env_name)
            # Neutral tint avoids double-multiplying theme hues
            self.starfield.color_tint = (1.0, 1.0, 1.0)
            # Global transparency now correctly updates Color instructions
            self.starfield.alpha = 0.3

    def _layout(self, *_):
        self._g_bg.clear()
        if self._snapshot_tex is not None:
            # --- MODIFIED: Increased snapshot visibility ---
            self._g_bg.add(Color(1, 1, 1, 0.7))
            self._g_bg.add(Rectangle(texture=self._snapshot_tex, pos=self.pos, size=self.size))

        # gradient with lower opacity so the room still reads
        # --- MODIFIED: Reduced gradient opacity to brighten the scene ---
        self._g_bg.add(Color(1,1,1,0.08 if self._snapshot_tex is not None else 0.15))
        self._g_bg.add(Rectangle(texture=self._gradient_tex, pos=self.pos, size=self.size,
                                 tex_coords=(0,0, 1,0, 1,1, 0,1)))

        # --- REMOVED: The top and bottom HUD bands are gone. ---
        self._g_hudlanes.clear()

        # --- REMOVED: The vignette effect has been removed for a brighter look. ---
        self._g_vig.clear()

        # Size children on layout (one redraw), not per-frame
        if hasattr(self, "starfield") and self.starfield:
            self.starfield.size = self.size
            self.starfield.pos  = self.pos

    def update(self, dt: float):
        """Public method to advance the parallax animation by delta-time."""
        sf = getattr(self, "starfield", None)
        if sf:
            sf.update(dt)



    def render_to_texture(self) -> Texture:
        fbo = Fbo(size=self.size)
        with fbo:
            if self._snapshot_tex is not None:
                Color(1,1,1,1); Rectangle(texture=self._snapshot_tex, pos=self.pos, size=self.size)
            Color(1,1,1,0.65); Rectangle(texture=self._gradient_tex, pos=self.pos, size=self.size)
            # flattened: three quiet bands
            Color(*self.sil_far);  Rectangle(pos=(self.x, self.center_y-2), size=(self.width, 2))
            Color(*self.sil_mid);  Rectangle(pos=(self.x, self.center_y-8), size=(self.width, 3))
            Color(*self.sil_near); Rectangle(pos=(self.x, self.center_y+6), size=(self.width, 3))
        fbo.draw(); return fbo.texture