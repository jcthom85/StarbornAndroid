# particles.py

import random
import math  # <--- IMPORT THE MATH MODULE
from dataclasses import dataclass
from kivy.uix.widget import Widget
from kivy.graphics import Color, Rectangle
from kivy.clock import Clock

@dataclass
class Particle:
    """Represents a single particle with its properties."""
    x: float
    y: float
    vx: float
    vy: float
    life: float
    size: float
    color: list

class ParticleEmitter(Widget):
    """
    A widget that emits, updates, and draws particles.
    """
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.particles = []
        self._update_event = None
        self._emit_acc = 0.0  # accumulator for continuous emission

    def start(self, config):
        """
        Begins emitting particles based on a configuration dictionary.
        
        :param config: A dictionary with emitter settings:
            - count: Number of particles to emit.
            - life: Lifespan of each particle in seconds.
            - size: A (min, max) tuple for particle size.
            - pos: An (x, y) tuple for the emission point.
            - velocity: A (min, max) tuple for initial speed.
            - angle: A (min, max) tuple for emission direction in degrees.
            - color: The [r, g, b] color for the particles.
        """
        self.config = dict(config or {})
        # initial burst
        self._emit()
        # begin update loop
        if not self._update_event:
            self._update_event = Clock.schedule_interval(self._update, 1/60.0)

    def stop(self):
        """Stops the update loop."""
        if self._update_event:
            self._update_event.cancel()
            self._update_event = None

    def _emit(self):
        """Initial burst emission based on the current configuration."""
        cfg = self.config
        count = int(cfg.get("count", 50))
        self._emit_n(count)

    def _emit_n(self, n):
        """Emit N particles using config; supports spawn_area and per-particle randomness."""
        if n <= 0:
            return
        cfg = self.config
        life_min, life_max = cfg.get("life", (0.5, 1.5))
        size_min, size_max = cfg.get("size", (1, 3))
        vel_min, vel_max = cfg.get("velocity", (20, 80))
        ang_min, ang_max = cfg.get("angle", (0, 360))
        color = cfg.get("color", [1, 1, 1])

        # spawn area rectangle: (x, y, w, h)
        area = cfg.get("spawn_area")
        use_area = isinstance(area, (list, tuple)) and len(area) == 4
        base_pos = cfg.get("pos", (self.center_x, self.center_y))

        for _ in range(int(n)):
            life = random.uniform(life_min, life_max)
            size = random.uniform(size_min, size_max)
            speed = random.uniform(vel_min, vel_max)
            angle = random.uniform(ang_min, ang_max)

            rad = -angle * (3.14159 / 180.0)  # Negative for Kivy's coordinate system
            vx = speed * math.cos(rad)
            vy = speed * math.sin(rad)

            if use_area:
                ax, ay, aw, ah = area
                pos_x = random.uniform(ax, ax + aw)
                pos_y = random.uniform(ay, ay + ah)
            else:
                pos_x, pos_y = base_pos

            self.particles.append(Particle(
                x=pos_x, y=pos_y,
                vx=vx, vy=vy,
                life=life,
                size=size,
                color=(color + [1.0])  # Start with full alpha
            ))

    def _update(self, dt):
        """Updates particle positions, life, and appearance."""
        # Use a list comprehension to build the list of surviving particles
        survivors = []
        cfg = self.config
        # optional gravity (gx, gy)
        gx, gy = 0.0, 0.0
        try:
            grav = cfg.get("gravity")
            if isinstance(grav, (list, tuple)) and len(grav) >= 2:
                gx, gy = float(grav[0]), float(grav[1])
        except Exception:
            gx, gy = 0.0, 0.0
        # optional drag (per-second)
        drag = 0.0
        try:
            drag = float(cfg.get("drag", 0.0) or 0.0)
        except Exception:
            drag = 0.0
        # optional continuous emission
        one_shot = bool(cfg.get("one_shot", True))
        emit_rate = 0.0
        try:
            emit_rate = float(cfg.get("emit_rate", 0.0) or 0.0)
        except Exception:
            emit_rate = 0.0

        for p in self.particles:
            p.life -= dt
            if p.life > 0:
                # integrate velocity
                p.vx += gx * dt
                p.vy += gy * dt
                if drag > 0.0:
                    damp = max(0.0, 1.0 - drag * dt)
                    p.vx *= damp
                    p.vy *= damp
                p.x += p.vx * dt
                p.y += p.vy * dt
                # Fade alpha by remaining life proportion (relative to min life to make fade visible)
                life_min = self.config.get("life", (0.5, 1.5))[0]
                p.color[3] = max(0.0, min(1.0, p.life / max(0.001, life_min)))
                survivors.append(p)

        self.particles = survivors
        self._draw_particles()

        # Continuous emission support
        if not one_shot and emit_rate > 0.0:
            self._emit_acc += dt * emit_rate
            spawn_n = int(self._emit_acc)
            if spawn_n > 0:
                self._emit_n(spawn_n)
                self._emit_acc -= spawn_n

        # If all particles have faded, stop the update loop (only for one_shot)
        if one_shot and not self.particles:
            self.stop()

    def _draw_particles(self):
        """Redraws all particles on the canvas."""
        self.canvas.clear()
        with self.canvas:
            for p in self.particles:
                Color(rgba=p.color)
                Rectangle(pos=(p.x - p.size / 2, p.y - p.size / 2), size=(p.size, p.size))
