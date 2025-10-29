# ui/tab_button.py
from kivy.uix.togglebutton import ToggleButton
from kivy.clock import Clock
from kivy.graphics import Color, RoundedRectangle
from kivy.properties import ListProperty, NumericProperty, ColorProperty
from kivy.metrics import dp
import math

class TabButton(ToggleButton):
    """
    A dedicated ToggleButton for creating tab-like interfaces.
    It handles its own background color changes and corner rounding.
    """
    bg_color_normal = ListProperty([0.1, 0.1, 0.1, 0.85])
    bg_color_down = ListProperty([0.3, 0.7, 0.9, 0.92])
    bg_color_down_active = ListProperty([0.35, 0.75, 0.95, 0.92]) # A slightly brighter 'glow' for the active tab
    text_color_normal = ColorProperty([0.9, 0.9, 0.9, 1.0]) # Fully opaque normal text
    text_color_down = ColorProperty([0.1, 0.1, 0.1, 0.98])
    corner_radius = ListProperty([dp(12), dp(12), dp(12), dp(12)])

    def __init__(self, **kwargs):
        # Force Kivy's default button backgrounds to be transparent
        kwargs['background_normal'] = ''
        kwargs['background_down'] = ''
        kwargs['background_disabled_normal'] = ''
        kwargs['background_disabled_down'] = ''

        # Pop custom properties before calling super
        if 'text_color_normal' in kwargs: self.text_color_normal = kwargs.pop('text_color_normal')
        if 'text_color_down' in kwargs: self.text_color_down = kwargs.pop('text_color_down')
        if 'bg_color_normal' in kwargs: self.bg_color_normal = kwargs.pop('bg_color_normal')
        if 'bg_color_down' in kwargs: self.bg_color_down = kwargs.pop('bg_color_down')
        if 'bg_color_down_active' in kwargs: self.bg_color_down_active = kwargs.pop('bg_color_down_active')

        super().__init__(**kwargs)

        with self.canvas.before:
            self.color_instruction = Color(rgba=self.bg_color_normal)
            self.rect = RoundedRectangle(
                pos=self.pos,
                size=self.size,
                radius=self.corner_radius
            )

        self.bind(
            pos=self._update_graphics,
            size=self._update_graphics,
            state=self._update_graphics,
            corner_radius=self._update_graphics
        )
        self._update_graphics()

    def on_release(self):
        # This is the core fix. Prevent the on_release event from firing if
        # the button is already in the 'down' (active) state.
        if self.state == 'down':
            return

    def set_corner_radii(self, radii: list):
        """
        Sets the corner radii for the button.
        Format: [(tl_x, tl_y), (tr_x, tr_y), (br_x, br_y), (bl_x, bl_y)]
        """
        self.corner_radius = radii

    def _update_graphics(self, *args):
        # Update background color and text color based on state
        if self.state == 'down':
            # Use a subtle sine wave to pulse the background color when active
            t = Clock.get_time()
            pulse = (math.sin(t * 4.0) + 1.0) / 2.0  # Ranges from 0 to 1
            r = self.bg_color_down[0] * (1 - pulse) + self.bg_color_down_active[0] * pulse
            g = self.bg_color_down[1] * (1 - pulse) + self.bg_color_down_active[1] * pulse
            b = self.bg_color_down[2] * (1 - pulse) + self.bg_color_down_active[2] * pulse
            self.color_instruction.rgba = (r, g, b, self.bg_color_down[3])
            self.color = self.text_color_down
        else:
            self.color_instruction.rgba = self.bg_color_normal
            self.color = self.text_color_normal

        # Update the rectangle's position, size, and radius
        self.rect.pos = self.pos
        self.rect.size = self.size
        
        # This was the main bug. The radius property needs a flat list for complex radii.
        self.rect.radius = [item for sublist in self.corner_radius for item in (sublist if isinstance(sublist, (list, tuple)) else (sublist, sublist))]