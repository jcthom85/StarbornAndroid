# ui/themed_button.py
from kivy.app import App
from kivy.uix.button import Button
from kivy.uix.togglebutton import ToggleButton
from kivy.graphics import Color, RoundedRectangle
from kivy.properties import ListProperty, NumericProperty, BooleanProperty
from kivy.metrics import dp

class ThemedButton(Button):
    """ A button with a solid, rounded-rectangle background that respects themes. """
    bg_color = ListProperty([0.2, 0.2, 0.2, 1])
    corner_radius = NumericProperty(dp(12))

    def __init__(self, **kwargs):
        kwargs['background_normal'] = ''
        kwargs['background_down'] = ''
        kwargs['background_disabled_normal'] = ''
        kwargs['background_disabled_down'] = ''
        kwargs['background_color'] = (0, 0, 0, 0)

        self.color_instruction = Color(rgba=self.bg_color)
        self.rect = RoundedRectangle(radius=[self.corner_radius])
        self._corner_radii = None  # For custom per-corner radii

        super().__init__(**kwargs)

        self.canvas.before.clear()
        with self.canvas.before:
            self.canvas.before.add(self.color_instruction)
            self.canvas.before.add(self.rect)

        self.bind(pos=self._update_rect, size=self._update_rect,
                  bg_color=self.on_bg_color, corner_radius=self._update_rect)
        
        self.on_state(self, self.state)
        self._update_rect()

    def set_corner_radii(self, radii):
        """Override the per-corner radii. Provide 4 entries (tl, tr, br, bl)."""
        if radii is None:
            self._corner_radii = None
        else:
            processed = []
            for entry in radii:
                if isinstance(entry, (list, tuple)) and len(entry) == 2:
                    processed.append((float(entry[0]), float(entry[1])))
                else:
                    val = float(entry)
                    processed.append((val, val))
            if len(processed) != 4:
                raise ValueError('corner radii must contain four entries')
            self._corner_radii = processed
        self._update_rect()

    def _update_rect(self, *args):
        self.rect.pos = self.pos
        self.rect.size = self.size
        if self._corner_radii is not None:
            self.rect.radius = self._corner_radii
        else:
            self.rect.radius = [self.corner_radius]

    def on_bg_color(self, instance, value):
        self.on_state(self, self.state)

    def on_state(self, instance, value):
        """ Darken the button when pressed. """
        if self.disabled:
            self.color_instruction.rgba = [c * 0.4 for c in self.bg_color[:3]] + [0.5]
            self.color = [c * 0.7 for c in self.color[:3]] + [0.7]
        elif value == 'down':
            self.color_instruction.rgba = [c * 0.75 for c in self.bg_color[:3]] + [self.bg_color[3]]
        else:
            self.color_instruction.rgba = self.bg_color


class ThemedToggleButton(ToggleButton):
    """ A toggle button with a solid, rounded-rectangle background. """
    bg_color = ListProperty([0.2, 0.2, 0.2, 1])
    corner_radius = NumericProperty(dp(12))
    allow_no_selection = BooleanProperty(False)

    def __init__(self, **kwargs):
        kwargs['background_normal'] = ''
        kwargs['background_down'] = ''
        kwargs['background_disabled_normal'] = ''
        kwargs['background_disabled_down'] = ''
        kwargs['background_color'] = (0, 0, 0, 0)

        self.color_instruction = Color(rgba=self.bg_color)
        self.rect = RoundedRectangle(radius=[self.corner_radius])
        self._corner_radii = None  # For custom per-corner radii

        super().__init__(**kwargs)

        self.canvas.before.clear()
        with self.canvas.before:
            self.canvas.before.add(self.color_instruction)
            self.canvas.before.add(self.rect)

        self.bind(pos=self._update_rect, size=self._update_rect, corner_radius=self._update_rect)
        self.on_state(self, self.state)
        self._update_rect()

    def set_corner_radii(self, radii):
        """Override the per-corner radii. Provide 4 entries (tl, tr, br, bl)."""
        if radii is None:
            self._corner_radii = None
        else:
            processed = []
            for entry in radii:
                if isinstance(entry, (list, tuple)) and len(entry) == 2:
                    processed.append((float(entry[0]), float(entry[1])))
                else:
                    val = float(entry)
                    processed.append((val, val))
            if len(processed) != 4:
                raise ValueError('corner radii must contain four entries')
            self._corner_radii = processed
        self._update_rect()

    def _update_rect(self, *args):
        self.rect.pos = self.pos
        self.rect.size = self.size
        if self._corner_radii is not None:
            self.rect.radius = self._corner_radii
        else:
            self.rect.radius = [self.corner_radius]

    def on_state(self, instance, value):
        """Update fill color for toggle state transitions."""
        if not App.get_running_app() or not hasattr(App.get_running_app(), 'current_game'):
            return

        tm = App.get_running_app().current_game.themes
        bg_color = tm.col('bg')
        accent_color = tm.col('accent')

        if self.disabled:
            self.color_instruction.rgba = [c * 0.4 for c in bg_color[:3]] + [0.5]
            self.color = [c * 0.7 for c in self.color[:3]] + [0.7]
        elif value == 'down':  # Active/Selected
            self.color_instruction.rgba = accent_color
            self.color = bg_color
        else:  # Inactive
            self.color_instruction.rgba = [c * 0.15 for c in bg_color[:3]] + [0.75]
            self.color = [c * 0.65 for c in self.color[:3]] + [0.85]

