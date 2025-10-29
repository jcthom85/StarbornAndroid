# ui/combat_fx.py
from __future__ import annotations
from dataclasses import dataclass
from math import sin, cos, pi
from random import uniform
from typing import Callable, Iterable, Tuple, Optional

from kivy.uix.widget import Widget
from kivy.graphics import Color, Line, Ellipse, Rectangle
from kivy.clock import Clock
from kivy.metrics import dp
from kivy.animation import Animation

# --- Base: a tiny lifetime-driven FX widget ---------------------------------
class FX(Widget):
    def __init__(self, life: float = 0.35, on_done: Optional[Callable] = None, **kw):
        super().__init__(size_hint=(None, None), **kw)
        self.t = 0.0
        self.life = max(1e-3, life)
        self._upd_ev = Clock.schedule_interval(self._tick, 1/60)
        self._on_done = on_done

    def _tick(self, dt: float):
        self.t += dt
        finished = self.t >= self.life
        try:
            self.draw(min(self.t / self.life, 1.0))
        except Exception:
            finished = True
        if finished:
            self._upd_ev.cancel()
            if self.parent:
                self.parent.remove_widget(self)
            if self._on_done:
                self._on_done()

    def draw(self, u: float):
        """Override in subclasses to render at normalized time u ∈ [0,1]."""
        pass

# --- NEW: Simple white flash (for attacker emphasis) -----------------------
class Flash(FX):
    """A simple, bright flash that fades out quickly."""
    def __init__(self, color=(1,1,1,1), **kw):
        # Use a very short life for a quick "pop"
        super().__init__(life=0.22, **kw)
        with self.canvas:
            self._c = Color(*color)
            # Use an ellipse that covers the sprite
            self._shape = Ellipse(pos=self.pos, size=self.size)

    def draw(self, u: float):
        # Animate alpha from 1.0 down to 0.0 using an ease-out curve
        self._c.a = 1.0 - u**2
        self._shape.pos = self.pos; self._shape.size = self.size

# --- Impact ring (expanding + fading) ---------------------------------------
class ImpactRing(FX):
    def __init__(self, radius: float = dp(24), width: float = dp(3), color=(1,1,1,1), **kw):
        super().__init__(life=0.28, **kw)
        self.r0, self.w, self.col = max(1, radius), width, color
        with self.canvas:
            self._c = Color(*color)
            self._ln = Line(circle=(0,0,self.r0), width=self.w)

    def draw(self, u: float):
        # Ease-out
        k = 1 - (1 - u)**2
        r = self.r0 * (0.6 + 0.8*k)
        self._ln.circle = (self.center_x, self.center_y, r)
        self._c.a = 1.0 - k

# --- Particle burst (triangles-as-sparks) -----------------------------------
class HitSparks(FX):
    def __init__(self, n: int = 25, spread: float = pi, speed: Tuple[float,float]=(220, 450),
                 color=(1, 1, 0.8, 1), **kw):
        super().__init__(life=0.5, **kw)
        self._parts = []
        self._speed = speed
        self._base = uniform(-pi, pi)
        with self.canvas:
            # Pre-allocate particles as triangles
            for _ in range(n):
                c = Color(*color)
                # Use a Triangle instead of a Rectangle for a sharper "spark" look
                tri = Line(points=[], width=dp(1.5), cap='round')
                self._parts.append((c, tri, uniform(-spread/2, spread/2), uniform(*speed)))

    def draw(self, u: float):
        # Use an ease-out curve for fading to make it less linear
        fade = 1 - u**2
        for i, (c, line, ang_off, v) in enumerate(self._parts):
            ang = self._base + ang_off
            # Ease-out flight
            dist = v * (1 - (1 - u)**3) * (1/60) * 25 # Increased distance
            px = self.center_x + cos(ang) * dist
            py = self.center_y + sin(ang) * dist
            # Draw a short line segment for each particle to simulate a streak
            tail_len = dp(8)
            line.points = [px, py, px - cos(ang) * tail_len, py - sin(ang) * tail_len]
            c.a = fade

# --- Slash arc (procedural curved stroke) -----------------------------------
class SlashArc(FX):
    def __init__(self, start, end, thickness: float = dp(4), color=(1,1,1,1), **kw):
        super().__init__(life=0.28, **kw)
        self.sx, self.sy = start
        self.ex, self.ey = end
        with self.canvas:
            self._c = Color(*color)
            self._ln = Line(points=[], width=thickness, cap='round')

    def draw(self, u: float):
        # Lerp + small bow for a katana-swoosh vibe
        cx = (self.sx + self.ex) / 2
        cy = (self.sy + self.ey) / 2 + dp(24)  # bow up
        steps = 16
        pts = []
        for i in range(steps+1):
            t = i/steps
            # Quadratic Bezier between s -> c -> e
            x = (1-t)**2*self.sx + 2*(1-t)*t*cx + t**2*self.ex
            y = (1-t)**2*self.sy + 2*(1-t)*t*cy + t**2*self.ey
            pts += [x, y]
        self._ln.points = pts
        self._c.a = 1 - u

# --- Target brackets (four corner glyphs) -----------------------------------
class TargetBrackets(FX):
    def __init__(self, rect: Tuple[float,float,float,float], color=(1,1,1,0.85), pad=dp(6), **kw):
        super().__init__(life=999, **kw)  # persistent; call .dismiss()
        self._rect = rect  # x, y, w, h in parent space
        self.pad = pad
        with self.canvas:
            self._c = Color(*color)
            self._lines = [Line(points=[], width=dp(2)) for _ in range(4)]

    def set_rect(self, rect):
        self._rect = rect

    def draw(self, u: float):
        x, y, w, h = self._rect
        p = self.pad
        L = dp(10)
        # four corner L-shapes
        pts = [
            [x-p,     y-p+L, x-p,   y-p,   x-p+L, y-p],                   # bottom-left
            [x+w+p,   y-p+L, x+w+p, y-p,   x+w+p-L, y-p],                 # bottom-right
            [x-p,     y+h+p-L, x-p, y+h+p, x-p+L, y+h+p],                 # top-left
            [x+w+p,   y+h+p-L, x+w+p, y+h+p, x+w+p-L, y+h+p],             # top-right
        ]
        for ln, seg in zip(self._lines, pts):
            ln.points = seg

    def dismiss(self):
        # manually end persistent effect
        self.t = self.life

# --- Timed-hit rings (two rings converging) ---------------------------------
class TimedHitRings(FX):
    def __init__(self, center: Tuple[float,float], radius=dp(42), **kw):
        super().__init__(life=0.7, **kw)
        self.cx, self.cy = center
        self.r = radius
        with self.canvas:
            self._c = Color(1,1,1,1)
            self._outer = Line(circle=(self.cx, self.cy, self.r), width=dp(2))
            self._inner = Line(circle=(self.cx, self.cy, self.r*0.2), width=dp(2))

    def draw(self, u: float):
        # Outer shrinks, inner grows; they "kiss" around u≈0.5
        o = self.r * (1 - 0.8*u)
        i = self.r * (0.2 + 0.8*u)
        self._outer.circle = (self.cx, self.cy, max(1, o))
        self._inner.circle = (self.cx, self.cy, max(1, i))
        self._c.a = 1 - abs(0.5 - u)*2  # brightest near the sweet spot
        if u >= 1:
            self._c.a = 0

# --- Minimal ATB/turn timeline (bottom overlay) ------------------------------
# ui/combat_fx.py
class TurnTimeline(Widget):
    def __init__(self, panels, **kw):
        super().__init__(size_hint=(1, None), height=dp(24), **kw)
        self._panels = list(panels)
        self.bind(pos=self._redraw, size=self._redraw)
        Clock.schedule_interval(lambda dt: self._redraw(), 1/30)

    def _redraw(self, *_):
        self.canvas.clear()
        if not self._panels:
            return
        with self.canvas:
            Color(0.08, 0.08, 0.12, 0.9)
            Rectangle(pos=self.pos, size=self.size)

            for pan in self._panels:
                # ATB ratio
                atb = getattr(pan, "atb", None)
                max_val = getattr(atb, "max_val", 0)
                curr    = getattr(atb, "curr", 0)
                ratio = (curr / float(max_val)) if max_val else 0.0

                # Player vs enemy
                is_enemy = getattr(getattr(pan, "battler", None), "is_enemy", False)

                x = self.x + ratio * (self.width - dp(10))
                y = self.y + (dp(2) if is_enemy else self.height/2)

                Color(0.25, 0.55, 1.0, 1) if is_enemy else Color(0.25, 1.0, 0.25, 1)
                Rectangle(pos=(x, y), size=(dp(10), dp(10)))