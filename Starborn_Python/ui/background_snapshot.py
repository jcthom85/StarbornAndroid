# ui/background_snapshot.py

from __future__ import annotations
from kivy.graphics import Fbo, ClearColor, ClearBuffers, Rectangle
from kivy.uix.widget import Widget
from kivy.graphics.texture import Texture

def _build_fbo(size) -> Fbo:
    """
    Create an Fbo with a transparent clear state.
    """
    fbo = Fbo(size=size, with_stencilbuffer=False)
    with fbo:
        ClearColor(0, 0, 0, 0)
        ClearBuffers()
    return fbo

def snapshot(widget: Widget, downsample: int = 4) -> Texture:
    """
    Take a full-res snapshot of `widget` and then render it
    into a smaller Fbo to simulate a quick blur via down/up-scaling.
    """
    # 1) grab a full-res image of the widget
    img = widget.export_as_image()
    full_tex = img.texture

    # 2) compute target downsampled size
    w, h = map(int, widget.size)
    w_ds = max(2, int(w / downsample))
    h_ds = max(2, int(h / downsample))

    # 3) render into the smaller Fbo without applying widget-world transforms
    #    (export_as_image already returns a texture cropped to the widget area)
    fbo = _build_fbo((w_ds, h_ds))
    with fbo:
        Rectangle(size=(w_ds, h_ds), pos=(0, 0), texture=full_tex)
    fbo.draw()

    return fbo.texture

def blurred_dimmed(widget: Widget, darkness: float = .5) -> Texture:
    """
    Wrapper that returns a lightly blurred texture of `widget`.
    Dimming is handled by drawing a semi-transparent black overlay
    later in MenuOverlay, so we only do the blur here.
    """
    tex = snapshot(widget)
    # smooth upscaling for better blur
    tex.mag_filter = 'linear'
    tex.min_filter = 'linear'
    return tex
