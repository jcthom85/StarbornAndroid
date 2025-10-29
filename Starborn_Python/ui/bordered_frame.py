# StarBorn/ui/bordered_frame.py
from kivy.uix.boxlayout import BoxLayout
from kivy.graphics import Color, Line, RoundedRectangle
from kivy.properties import ListProperty, NumericProperty
from kivy.metrics import dp

class BorderedFrame(BoxLayout):
    """
    A BoxLayout that draws a simple, clean border around itself using
    Kivy's graphics instructions, removing the need for an external image.
    """
    border_color = ListProperty([1, 1, 1, 1])
    border_width = dp(1.5)
    radius = ListProperty([dp(12)])

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        # We draw the border on the canvas *after* the background but *before* children
        with self.canvas.before:
            self.color_instruction = Color(rgba=self.border_color)
            self.line = Line(width=self.border_width, rounded_rectangle=(self.x, self.y, self.width, self.height, *self.radius))

        # Bind the update method to position, size, and color changes
        self.bind(pos=self._update_rect, size=self._update_rect,
                  border_color=self._update_color, radius=self._update_rect)

    def _update_rect(self, *args):
        """Update the position and size of the border line."""
        self.line.rounded_rectangle = (self.x, self.y, self.width, self.height, *self.radius)

    def _update_color(self, *args):
        """Update the color of the border."""
        self.color_instruction.rgba = self.border_color