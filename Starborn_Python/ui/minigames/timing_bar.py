from kivy.uix.widget import Widget
from kivy.graphics import Color, Rectangle, RoundedRectangle
from kivy.properties import NumericProperty, StringProperty, ListProperty, OptionProperty
from kivy.animation import Animation
from kivy.metrics import dp

DIFFICULTY_PRESETS = {
    "easy":   {"speed": 1.4, "success": 0.30, "perfect": 0.12},
    "medium": {"speed": 1.9, "success": 0.22, "perfect": 0.08},
    "hard":   {"speed": 2.5, "success": 0.16, "perfect": 0.05},
}

class TimingBar(Widget):
    """
    A reusable timing mini‑game.

    Tap while the cursor overlaps the PERFECT or SUCCESS zones.
    Emits on_complete(result) where result ∈ {"perfect","success","failure"}.

    Usage:
        tb = TimingBar(difficulty="medium", orientation="vertical", theme="cooking")
        tb.bind(on_complete=lambda inst,res: print("Result:", res))
        tb.start()
    """

    difficulty  = StringProperty("medium")
    orientation = OptionProperty("vertical", options=("vertical", "horizontal"))

    # Animated cursor position (x or y depending on orientation)
    cursor_pos = NumericProperty(0)

    # Theme: choose base palette
    theme      = StringProperty("default")

    # Override colors if desired
    color_bg       = ListProperty([0.1, 0.1, 0.12, 0.9])
    color_success  = ListProperty([0.2, 0.6, 1.0, 0.45])
    color_perfect  = ListProperty([1.0, 0.95, 0.4, 0.9])
    color_cursor   = ListProperty([1, 1, 1, 1])

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.register_event_type("on_complete")
        self.anim = None
        self.bind(cursor_pos=self._redraw_cursor, size=lambda *_: self._rebuild_canvas(),
                  pos=lambda *_: self._rebuild_canvas())

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------
    def start(self):
        """Begin (or restart) the timing loop."""
        self._apply_theme()
        self._rebuild_canvas()
        self._start_anim()

    def stop_game(self):
        """Stop and evaluate the result."""
        if not self.anim:
            return
        self.anim.cancel(self)
        self.anim = None

        # Cursor center along movement axis
        if self.orientation == "vertical":
            center = self.cursor_pos + self.cursor_rect.size[1] / 2
        else:
            center = self.cursor_pos + self.cursor_rect.size[0] / 2

        if self.zone_perfect[0] <= center <= self.zone_perfect[1]:
            result = "perfect"
        elif any(a <= center <= b for a, b in self.zone_success):
            result = "success"
        else:
            result = "failure"
        self.dispatch("on_complete", result)

    # ------------------------------------------------------------------
    # Events
    # ------------------------------------------------------------------
    def on_complete(self, result: str):
        pass  # user binds to this

    # ------------------------------------------------------------------
    # Theming / Drawing
    # ------------------------------------------------------------------
    def _apply_theme(self):
        """Light differentiation for cooking vs fishing."""
        if self.theme == "cooking":
            self.color_success = [0.95, 0.5, 0.2, 0.45]   # warm orange
            self.color_perfect = [1.0, 0.9, 0.5, 0.9]
            self.color_cursor  = [1, 1, 1, 1]
        elif self.theme == "fishing":
            self.color_success = [0.2, 0.7, 1.0, 0.45]    # cool blue
            self.color_perfect = [0.6, 1.0, 0.9, 0.9]
            self.color_cursor  = [1, 1, 1, 1]
        # else: keep defaults

    def _rebuild_canvas(self):
        self.canvas.clear()
        settings = DIFFICULTY_PRESETS.get(self.difficulty, DIFFICULTY_PRESETS["medium"])
        speed = settings["speed"]

        w, h = self.size
        x, y = self.pos

        # Movement length & zone sizes depend on orientation
        total_len = h if self.orientation == "vertical" else w
        success_len = total_len * settings["success"]
        perfect_len = total_len * settings["perfect"]

        mid = (y + h / 2) if self.orientation == "vertical" else (x + w / 2)
        perfect_start = mid - perfect_len / 2
        perfect_end   = mid + perfect_len / 2

        success_low_start  = perfect_start - success_len
        success_low_end    = perfect_start
        success_high_start = perfect_end
        success_high_end   = perfect_end + success_len

        # Save for hit detection
        self.zone_perfect = (perfect_start, perfect_end)
        self.zone_success = [(success_low_start, success_low_end),
                             (success_high_start, success_high_end)]

        # Visual sizes
        cursor_thickness = dp(8)
        corner = dp(6)

        with self.canvas:
            # Background
            Color(*self.color_bg)
            Rectangle(pos=self.pos, size=self.size)

            # Success zones
            Color(*self.color_success)
            if self.orientation == "vertical":
                Rectangle(pos=(x, success_low_start),   size=(w, success_len))
                Rectangle(pos=(x, success_high_start), size=(w, success_len))
            else:
                Rectangle(pos=(success_low_start, y),   size=(success_len, h))
                Rectangle(pos=(success_high_start, y), size=(success_len, h))

            # Perfect core
            Color(*self.color_perfect)
            if self.orientation == "vertical":
                Rectangle(pos=(x, perfect_start), size=(w, perfect_len))
            else:
                Rectangle(pos=(perfect_start, y), size=(perfect_len, h))

            # Cursor (RoundedRectangle)
            Color(*self.color_cursor)
            if self.orientation == "vertical":
                self.cursor_pos = y  # reset
                self.cursor_rect = RoundedRectangle(
                    pos=(x, self.cursor_pos),
                    size=(w, cursor_thickness),
                    radius=[corner]
                )
            else:
                self.cursor_pos = x
                self.cursor_rect = RoundedRectangle(
                    pos=(self.cursor_pos, y),
                    size=(cursor_thickness, h),
                    radius=[corner]
                )

        # Recreate animation
        self._start_anim(speed=speed)

    def _start_anim(self, speed=None):
        if speed is None:
            speed = DIFFICULTY_PRESETS.get(self.difficulty, DIFFICULTY_PRESETS["medium"])["speed"]
        if self.anim:
            self.anim.cancel(self)
        if self.orientation == "vertical":
            start = self.y
            end   = self.top - self.cursor_rect.size[1]
        else:
            start = self.x
            end   = self.right - self.cursor_rect.size[0]

        self.cursor_pos = start
        self.anim = Animation(cursor_pos=end, duration=speed) + Animation(cursor_pos=start, duration=speed)
        self.anim.repeat = True
        self.anim.start(self)

    def _redraw_cursor(self, *_):
        if not hasattr(self, "cursor_rect"):
            return
        if self.orientation == "vertical":
            self.cursor_rect.pos = (self.x, self.cursor_pos)
        else:
            self.cursor_rect.pos = (self.cursor_pos, self.y)

    # ------------------------------------------------------------------
    # Input
    # ------------------------------------------------------------------
    def on_touch_down(self, touch):
        if self.collide_point(*touch.pos) and self.anim:
            self.stop_game()
            return True
        return super().on_touch_down(touch)
