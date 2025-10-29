from kivy.app import App
from kivy.uix.widget import Widget
from kivy.graphics import RenderContext, Color, Rectangle
from kivy.properties import ObjectProperty, NumericProperty, OptionProperty
import os


class BlurredExtension(Widget):
    texture = ObjectProperty(None, allownone=True)
    blur_intensity = NumericProperty(1.0)
    fade_start = NumericProperty(0.5)  # Y-coordinate where fade starts (0.0-1.0)
    fade_end = NumericProperty(0.0)    # Y-coordinate where fade ends (fully transparent)
    fade_direction = OptionProperty('top', options=('top', 'bottom'))  # 'top' or 'bottom' for which end fades
    # Optional curve control for the fade, mapped to shader uniform 'fade_power'
    fade_power = NumericProperty(1.0)
    # Sample only a thin vertical band of the source texture near the edge
    # and stretch/blur it into the overlay area.
    sample_band = NumericProperty(0.05)

    def __init__(self, **kwargs):
        # Create our render context first so property setters can write uniforms safely
        self.canvas = RenderContext(use_parent_projection=True)
        self.canvas.shader.source = os.path.join(
            App.get_running_app().directory, 'shaders', 'blur_fade.glsl'
        )
        super(BlurredExtension, self).__init__(**kwargs)  # Call super after canvas is set up

        # Initialize shader uniforms
        self.on_blur_intensity(self, self.blur_intensity)
        self.on_fade_start(self, self.fade_start)
        self.on_fade_end(self, self.fade_end)
        self.on_fade_power(self, self.fade_power)
        self.on_fade_direction(self, self.fade_direction)
        self._update_uv_mapping()

        with self.canvas:
            Color(1, 1, 1, 1)  # Ensure full color for the texture
            self.rect = Rectangle(size=self.size, texture=self.texture)

        self.bind(size=self._update_rect, pos=self._update_rect, texture=self._update_rect)

    def _update_rect(self, *args):
        if self.texture:
            self.rect.texture = self.texture
        self.rect.size = self.size
        self.rect.pos = self.pos

    def on_blur_intensity(self, instance, value):
        if hasattr(self, 'canvas') and self.canvas:
            self.canvas['blur_intensity'] = float(value)

    def on_fade_start(self, instance, value):
        if hasattr(self, 'canvas') and self.canvas:
            # Pass through; orientation handled in shader via fade_flip
            self.canvas['fade_start'] = float(value)
            self.canvas['fade_end'] = float(self.fade_end)

    def on_fade_end(self, instance, value):
        if hasattr(self, 'canvas') and self.canvas:
            # Pass through; orientation handled in shader via fade_flip
            self.canvas['fade_end'] = float(value)
            self.canvas['fade_start'] = float(self.fade_start)

    def on_fade_power(self, instance, value):
        if hasattr(self, 'canvas') and self.canvas:
            self.canvas['fade_power'] = float(value)

    def on_fade_direction(self, instance, value):
        # Pass orientation to shader and refresh uniforms
        if hasattr(self, 'canvas') and self.canvas:
            self.canvas['fade_flip'] = 1.0 if value == 'top' else 0.0
        self.on_fade_start(self, self.fade_start)
        self.on_fade_end(self, self.fade_end)
        self._update_uv_mapping()

    def on_sample_band(self, instance, value):
        self._update_uv_mapping()

    def _update_uv_mapping(self):
        # Map overlay quad UVs to a thin band at the top or bottom of the source
        # texture. Use fade_direction to infer which edge to anchor to:
        # - If fading at the bottom ("bottom"), the overlay is at the top, so anchor to top edge.
        # - If fading at the top ("top"), the overlay is at the bottom, so anchor to bottom edge.
        band = max(0.001, min(0.5, float(self.sample_band)))
        if hasattr(self, 'canvas') and self.canvas:
            if self.fade_direction == 'bottom':
                # Top overlay: anchor to the very top of the source image
                uv_offset = (0.0, 1.0 - band)
                uv_scale = (1.0, band)
            else:
                # Bottom overlay: anchor to the very bottom of the source image
                uv_offset = (0.0, 0.0)
                uv_scale = (1.0, band)
            self.canvas['uv_offset'] = uv_offset
            self.canvas['uv_scale'] = uv_scale
