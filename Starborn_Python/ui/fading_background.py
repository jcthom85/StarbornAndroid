# ui/fading_background.py
from kivy.app import App
from kivy.uix.widget import Widget
from kivy.graphics import RenderContext, Fbo, Color, Rectangle
from kivy.properties import ObjectProperty, NumericProperty
import os

class FadingBackground(Widget):
    texture = ObjectProperty(None, allownone=True)
    vignette_intensity = NumericProperty(1.0)
    blur_intensity = NumericProperty(1.0)

    def __init__(self, **kwargs):
        # Call super() first. This initializes the widget and its properties,
        # which will trigger the on_... methods before the canvas is ready.
        super(FadingBackground, self).__init__(**kwargs)

        # Now, create our custom canvas with the shader.
        # This replaces the default canvas created by the base Widget class.
        self.canvas = RenderContext(use_parent_projection=True)
        self.canvas.shader.source = os.path.join(
            App.get_running_app().directory, 'shaders', 'blur_vignette.glsl'
        )

        # Manually set the initial uniform values on our new canvas,
        # since the on_... methods were already called when self.canvas didn't exist.
        self.on_vignette_intensity(self, self.vignette_intensity)
        self.on_blur_intensity(self, self.blur_intensity)

        with self.canvas:
            self.fbo = Fbo(size=self.size)
            self.fbo_color = Color(1, 1, 1, 1)
            self.fbo_rect = Rectangle(size=self.size, texture=self.fbo.texture)

        self.fbo.shader.source = self.canvas.shader.source
        self.bind(size=self._update_fbo, texture=self._update_texture)

    def _update_fbo(self, *args):
        self.fbo.size = self.size
        self.fbo_rect.size = self.size

    def _update_texture(self, *args):
        if self.texture:
            self.fbo_rect.texture = self.texture

    def on_vignette_intensity(self, instance, value):
        # This method can be called during __init__ before self.canvas is set.
        # We add a check to ensure it exists before we try to use it.
        if hasattr(self, 'canvas') and self.canvas:
            self.canvas['vignette_intensity'] = float(value)

    def on_blur_intensity(self, instance, value):
        # We do the same check here for the blur intensity.
        if hasattr(self, 'canvas') and self.canvas:
            self.canvas['blur_intensity'] = float(value)