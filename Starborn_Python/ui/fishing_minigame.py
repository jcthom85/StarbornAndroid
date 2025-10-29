# ui/fishing_minigame.py
from __future__ import annotations

import math, random
from kivy.uix.widget import Widget
from kivy.graphics import Color, Rectangle, Ellipse, Line
from kivy.properties import (
    NumericProperty, OptionProperty, ListProperty, BooleanProperty
)
from kivy.clock import Clock
from kivy.metrics import dp
from kivy.core.image import Image as CoreImage


class ReelMinigame(Widget):
    """
    Vertical 'reel' bar.

    Win: keep the fish inside the bright middle band long enough (target_hold_time).
    Lose: keep it outside long enough (escape_time) OR exceed max_duration.

    Taps apply a downward impulse; fish tends to drift upward by pattern.
    Emits: on_result("success" | "fail")
    """

    # Difficulty/pattern
    difficulty = OptionProperty("medium", options=("easy", "medium", "hard"))
    pattern    = OptionProperty("auto",   options=("auto", "constant", "wobble", "erratic"))

    # Timers (seconds)
    target_hold_time = NumericProperty(3.0)   # required "in-zone" time to win
    escape_time      = NumericProperty(2.5)   # allowed "out-of-zone" before fail
    max_duration     = NumericProperty(25.0)  # overall timeout → fail

    # Capture band (normalized 0..1 of widget height)
    zone_center_ratio = NumericProperty(0.5)
    zone_half_height  = NumericProperty(0.10)

    # Fish physics
    fish_pos     = NumericProperty(0.5)   # 0=bottom..1=top
    fish_vel     = NumericProperty(0.0)
    base_up_force = NumericProperty(0.25)
    tap_impulse   = NumericProperty(0.32)
    damping       = NumericProperty(0.88)
    max_speed     = NumericProperty(1.35)

    # Visual colors
    theme_bg       = ListProperty([0.07, 0.10, 0.14, 0.95])
    theme_zone     = ListProperty([0.20, 0.70, 1.00, 0.25])
    theme_zone_mid = ListProperty([0.60, 1.00, 0.90, 0.85])
    theme_border   = ListProperty([0.35, 0.85, 1.00, 1.00])
    theme_fish     = ListProperty([1.00, 1.00, 1.00, 1.00])
    theme_progress = ListProperty([1.00, 1.00, 1.00, 0.85])   # right “win” meter

    show_progress = BooleanProperty(True)

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.register_event_type("on_result")
        self._ev       = None
        self._held     = 0.0
        self._missed   = 0.0
        self._elapsed  = 0.0
        self._fish_tex = None
        self.bind(size=self._redraw, pos=self._redraw)
        Clock.schedule_once(lambda *_: self._apply_difficulty_defaults(), 0)

    # Public API --------------------------------------------------------------
    def start(self, *, rod_power: float = 1.0):
        self.stop()
        self._held = self._missed = self._elapsed = 0.0
        # give stronger rods a bit more pull
        self.tap_impulse *= max(0.5, min(1.5, 0.85 + (rod_power - 1.0) * 0.15))
        self._ev = Clock.schedule_interval(self._update, 1/60.0)
        self._redraw()

    def stop(self):
        if self._ev is not None:
            Clock.unschedule(self._ev)
            self._ev = None

    def on_result(self, _result: str):
        pass

    # Interaction -------------------------------------------------------------
    def on_touch_down(self, touch):
        if not self.collide_point(*touch.pos):
            return super().on_touch_down(touch)
        self.fish_vel = max(-self.max_speed, self.fish_vel - self.tap_impulse)
        return True

    # Core loop ---------------------------------------------------------------
    def _update(self, dt: float):
        # upward pull + pattern
        up = self._pattern_force(self._elapsed)
        self.fish_vel += (self.base_up_force + up) * dt
        self.fish_vel *= (self.damping ** dt)
        self.fish_vel = max(-self.max_speed, min(self.max_speed, self.fish_vel))
        self.fish_pos = max(0.0, min(1.0, self.fish_pos + self.fish_vel * dt))

        zone_lo = self.zone_center_ratio - self.zone_half_height
        zone_hi = self.zone_center_ratio + self.zone_half_height
        in_zone = zone_lo <= self.fish_pos <= zone_hi

        if in_zone:
            self._held += dt
            # bleed off some miss while controlled
            self._missed = max(0.0, self._missed - dt * 0.5)
        else:
            self._missed += dt

        self._elapsed += dt

        # light late-fight ramp
        if self._elapsed > self.max_duration * 0.6:
            self.base_up_force *= (1.0 + 0.06 * dt)

        self._redraw()

        # win / lose
        if self._held >= self.target_hold_time:
            self.stop()
            self.dispatch("on_result", "success")
            return
        if self._missed >= self.escape_time or self._elapsed >= self.max_duration:
            self.stop()
            self.dispatch("on_result", "fail")
            return

    # Patterns ---------------------------------------------------------------
    def _apply_difficulty_defaults(self):
        if self.pattern == "auto":
            self.pattern = {"easy": "constant", "medium": "wobble", "hard": "erratic"}.get(
                self.difficulty, "wobble"
            )

        if self.difficulty == "easy":
            self.zone_half_height = 0.125
            self.target_hold_time = max(2.5, self.target_hold_time)
            self.escape_time      = 3.2
            self.max_duration     = 30.0
            self.base_up_force    = 0.22
        elif self.difficulty == "hard":
            self.zone_half_height = 0.085
            self.target_hold_time = max(3.5, self.target_hold_time)
            self.escape_time      = 2.0
            self.max_duration     = 20.0
            self.base_up_force    = 0.30
        else:
            self.zone_half_height = 0.10
            self.target_hold_time = max(3.0, self.target_hold_time)
            self.escape_time      = 2.5
            self.max_duration     = 25.0
            self.base_up_force    = 0.25

    def _pattern_force(self, t: float) -> float:
        if self.pattern == "constant":
            return 0.0
        if self.pattern == "wobble":
            return 0.15 * math.sin(t * 1.8) + 0.05 * math.sin(t * 0.6)
        if self.pattern == "erratic":
            seg = int(t * 2.0)
            random.seed(seg)
            target = 0.20 + 0.35 * random.random()
            local = (t * 2.0) - seg
            return target * (3 * local**2 - 2 * local**3)
        return 0.0

    # Draw -------------------------------------------------------------------
    def _redraw(self, *_):
        self.canvas.clear()
        w, h = self.size
        x, y = self.pos

        cx    = x + w / 2
        bar_w = dp(64)
        bar_x = cx - bar_w / 2

        fy = y + self.fish_pos * h

        zone_lo = y + (self.zone_center_ratio - self.zone_half_height) * h
        zone_hi = y + (self.zone_center_ratio + self.zone_half_height) * h

        # right progress (win)
        prog_w = dp(8)
        prog_x = bar_x + bar_w + dp(12)
        prog_h = h * min(1.0, self._held / max(0.001, self.target_hold_time))

        with self.canvas:
            # background panel
            Color(*self.theme_bg)
            Rectangle(pos=(bar_x - dp(12), y), size=(bar_w + dp(24), h))

            # border
            Color(*self.theme_border)
            Line(rounded_rectangle=(bar_x - dp(12), y, bar_w + dp(24), h, dp(10)), width=dp(2))

            # capture band
            Color(*self.theme_zone)
            Rectangle(pos=(bar_x, zone_lo), size=(bar_w, zone_hi - zone_lo))
            Color(*self.theme_zone_mid)
            Rectangle(pos=(bar_x, (zone_lo + zone_hi) / 2 - dp(2)), size=(bar_w, dp(4)))

            # fish line
            Color(1, 1, 1, 0.25)
            Line(points=[cx, y, cx, y + h], width=dp(1))

            # fish icon
            fish_size = dp(28)
            fx = cx - fish_size / 2
            if self._fish_tex is None:
                try:
                    self._fish_tex = CoreImage("images/ui/item_icon_fish.png").texture
                except Exception:
                    self._fish_tex = None

            if self._fish_tex is not None:
                Color(1, 1, 1, 0.95)
                Rectangle(texture=self._fish_tex, pos=(fx, fy - fish_size/2), size=(fish_size, fish_size))
            else:
                Color(*self.theme_fish)
                Ellipse(pos=(fx, fy - fish_size/2), size=(fish_size, fish_size))

            # progress (right)
            if self.show_progress:
                Color(1, 1, 1, 0.12)
                Rectangle(pos=(prog_x, y), size=(prog_w, h))
                Color(*self.theme_progress)
                Rectangle(pos=(prog_x, y), size=(prog_w, prog_h))

            # end caps
            Color(1, 1, 1, 0.25)
            Rectangle(pos=(bar_x, y), size=(bar_w, dp(4)))
            Rectangle(pos=(bar_x, y + h - dp(4)), size=(bar_w, dp(4)))
