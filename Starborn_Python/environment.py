"""Environment background loader (safe 9:16 + parallax-friendly).

Keeps backgrounds visible by fitting each image to the container width while
preserving aspect ratio. Allows a per-layer vertical offset for gentle nudging
without moving the whole viewport.
"""

from kivy.uix.floatlayout import FloatLayout
from kivy.uix.image import Image
from kivy.animation import Animation
from kivy.properties import NumericProperty
from kivy.resources import resource_find
from kivy.graphics import Color, Rectangle, RenderContext


class ParallaxLayer(FloatLayout):
    """Single parallax layer with width-fit image and optional y-offset."""
    depth = NumericProperty(1.0)
    y_offset = NumericProperty(0.0)
    bleed_ratio = NumericProperty(0.0)  # extra vertical height relative to natural aspect

    def __init__(self, source: str | None, depth: float = 1.0,
                 *, is_bg_flag: bool = False, **kwargs):
        super().__init__(**kwargs)
        self.depth = depth
        self.is_background = bool(is_bg_flag or depth == 0)
        self._desat_enabled = False
        self._desat_strength = 0.0

        # Use a RenderContext so we can optionally apply a fragment shader
        self.canvas = RenderContext(use_parent_modelview=True, use_parent_projection=True)

        # We'll draw a Rectangle with the layer's texture
        self._color = None
        self._rect = None
        self._img_loader = None

        # Establish a pass-through shader immediately so drawing works pre-desat
        self._ensure_shader()
        try:
            self.canvas['u_desat'] = 0.0
            self.canvas['u_dark']  = 0.0
        except Exception:
            pass

        path = resource_find(source) if source else None
        if path:
            # Hidden Image loader to get an async texture handle
            self._img_loader = Image(source=path, allow_stretch=True, keep_ratio=True)
            # expose as .image for external integrations expecting an Image
            self.image = self._img_loader
            # Draw rectangle now; set texture when available
            with self.canvas:
                self._color = Color(1, 1, 1, 1)
                self._rect = Rectangle(pos=self.pos, size=self.size, texture=None)
            # Try to set texture immediately using CoreImage; fall back to loader signal
            try:
                from kivy.core.image import Image as CoreImage
                img = CoreImage(path)
                if getattr(img, 'texture', None) is not None:
                    self._rect.texture = img.texture
                    try:
                        self._rect.texture.mag_filter = 'linear'
                        self._rect.texture.min_filter = 'linear'
                    except Exception:
                        pass
            except Exception:
                pass
            self._img_loader.bind(texture=self._on_texture_ready)
            self.bind(size=self._fit_width, pos=self._fit_width)
        # else: no valid source; leave blank

    def update_parallax(self, offset_x: float, offset_y: float) -> None:
        self.x = offset_x * self.depth
        self.y = offset_y * self.depth

    def _fit_width(self, *_):
        if not self._rect:
            return
        pw = max(1.0, float(self.width))
        ph = max(1.0, float(self.height))
        tex = self._rect.texture
        tw, th = (tex.size if tex is not None else (9.0, 16.0))
        tw = tw or 1.0
        # natural width-fit height
        nat_h = pw * (float(th) / float(tw)) * (1.0 + float(self.bleed_ratio))
        # If not tall enough to cover, scale up width to maintain aspect and fill height (cover)
        if nat_h < ph:
            scale = ph / nat_h
            w = pw * scale
            h = ph
            x = -(w - pw) / 2.0  # center crop horizontally
        else:
            w = pw
            h = nat_h
            x = 0.0
        self._rect.size = (w, h)
        self._rect.pos = (x, float(self.y_offset))

    def set_offset_y(self, off: float):
        self.y_offset = float(off)
        self._fit_width()
    def set_bleed(self, ratio: float):
        try:
            self.bleed_ratio = float(ratio)
        except Exception:
            self.bleed_ratio = 0.0
        self._fit_width()

    # ---- Optional grayscale shader -----------------------------------------
    def _ensure_shader(self):
        # Minimal pass-through header + grayscale mix + darken factor
        self.canvas.shader.fs = (
            """
            $HEADER$
            uniform float u_desat; // 0..1
            uniform float u_dark;  // 0..1 darkness
            void main(void) {
                vec4 c = texture2D(texture0, tex_coord0);
                float y = dot(c.rgb, vec3(0.299, 0.587, 0.114));
                vec3 g = vec3(y);
                vec3 mixed = mix(c.rgb, g, clamp(u_desat, 0.0, 1.0));
                vec3 darkened = mix(mixed, vec3(0.0), clamp(u_dark, 0.0, 1.0));
                gl_FragColor = vec4(darkened, c.a);
            }
            """
        )

    def _on_texture_ready(self, *_):
        if self._rect is not None and self._img_loader is not None and self._img_loader.texture is not None:
            self._rect.texture = self._img_loader.texture
            # improve sampling if needed
            try:
                self._rect.texture.mag_filter = 'linear'
                self._rect.texture.min_filter = 'linear'
            except Exception:
                pass
            self._fit_width()

    def set_desaturate(self, strength: float = 0.8):
        try:
            s = max(0.0, min(1.0, float(strength)))
        except Exception:
            s = 0.0
        self._desat_strength = s
        if s <= 0.0:
            # Disable shader effect
            try:
                # If shader is present, set uniform to 0; otherwise ignore
                self.canvas['u_desat'] = 0.0
            except Exception:
                pass
            return
        # Enable and set uniform
        self._ensure_shader()
        try:
            self.canvas['u_desat'] = s
        except Exception:
            pass

    def set_dark_amount(self, strength: float = 0.0):
        """Darken layer by mixing with black (0..1)."""
        try:
            s = max(0.0, min(1.0, float(strength)))
        except Exception:
            s = 0.0
        try:
            self.canvas['u_dark'] = s
        except Exception:
            pass


class EnvironmentLoader(FloatLayout):
    """Owns and displays one or more ParallaxLayer widgets."""

    # Progress 0..1 controlling current darkness level (animated)
    dark_progress = NumericProperty(0.0)

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.layers: list[ParallaxLayer] = []
        # Shader-driven darkness targets (no overlays)
        self._dark_desat_max = 0.85
        self._dark_luma_max = 0.55

    def load_environment(self, env: dict | None) -> None:
        """
        Expects either:
          - {'background_image': 'path'}
          - {'layers': [{'source': '...', 'depth': 0, ...}, ...]}
        """
        # Build prospective layers first so we don't wipe a visible background
        new_layers: list[ParallaxLayer] = []

        def _add_candidate(src: str | None, depth: float = 0.0, is_bg: bool = True):
            layer = ParallaxLayer(source=src, depth=depth, is_bg_flag=is_bg)
            if layer.image:
                layer.size_hint = (1, 1)
                new_layers.append(layer)

        if not env:
            pass
        elif 'layers' not in env:
            _add_candidate(env.get('background_image'))
        else:
            for layer_data in env['layers']:
                _add_candidate(
                    layer_data.get('source'),
                    depth=layer_data.get('depth', 1.0),
                    is_bg=bool(layer_data.get('is_background', False) or layer_data.get('depth', 1.0) == 0),
                )

        # If nothing resolved and we already have a background, keep it
        if not new_layers and self.layers:
            return

        # If still nothing, try a very safe placeholder used elsewhere in UI
        if not new_layers:
            placeholder = resource_find('images/ui/starborn_menu_bg.png') or resource_find('images/rooms/mine_2.png')
            _add_candidate(placeholder)

        # Now replace existing layers with the new set
        self.clear_widgets()
        self.layers.clear()
        for lay in new_layers:
            self.add_widget(lay)
            self.layers.append(lay)
        # (No internal dark overlay is added anymore.)

    def update_parallax(self, offset_x: float, offset_y: float) -> None:
        for layer in self.layers:
            layer.update_parallax(offset_x, offset_y)

    def set_bg_offset_y(self, off: float) -> None:
        for layer in self.layers:
            layer.set_offset_y(off)
    def set_bg_bleed(self, ratio: float) -> None:
        for layer in self.layers:
            layer.set_bleed(ratio)

    def _animate_flicker(self, widget: Image) -> None:
        anim = Animation(opacity=.5, duration=.1) + Animation(opacity=1.0, duration=.2)
        anim.repeat = True
        anim.start(widget)

    # ---- Darkness helper ----------------------------------------------------
    def on_dark_progress(self, *_):
        # Update layers and overlay alpha according to current progress 0..1
        p = max(0.0, min(1.0, float(self.dark_progress)))
        for layer in self.layers:
            try:
                layer.set_desaturate(self._dark_desat_max * p)
            except Exception:
                pass
        for layer in self.layers:
            try:
                layer.set_dark_amount(self._dark_luma_max * p)
            except Exception:
                pass

    def set_dark_mode(self, enabled: bool, *, desaturation: float = 0.85, overlay_alpha: float = 0.55, animate: bool = True, duration: float = 0.25) -> None:
        # Save targets
        try:
            self._dark_desat_max = float(desaturation)
        except Exception:
            pass
        try:
            # Reuse overlay_alpha param as shader darkening amount
            self._dark_luma_max = float(overlay_alpha)
        except Exception:
            pass
        # Animate progress
        target = 1.0 if enabled else 0.0
        if not animate:
            self.dark_progress = target
            self.on_dark_progress()
            return
        Animation.cancel_all(self, 'dark_progress')
        anim = Animation(dark_progress=target, duration=max(0.0, float(duration)), t='out_quad')
        anim.start(self)
