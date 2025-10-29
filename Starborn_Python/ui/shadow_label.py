import sys
import os, random
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from kivy.graphics import Color, Rectangle, Line, InstructionGroup  # NEW
from kivy.core.image import Image as CoreImage                       # NEW
from kivy.resources import resource_find
from kivy.uix.label import Label
from kivy.properties import ListProperty, NumericProperty, BooleanProperty, ObjectProperty, StringProperty
from kivy.metrics import dp
from kivy.core.window import Window
try:
    from kivy.graphics import RoundedRectangle
    _HAS_ROUNDED = True
except Exception:  # pragma: no cover
    _HAS_ROUNDED = False

from theme_manager import ThemeManager


class ShadowLabel(Label):
    """
    Label with lightweight readability helpers:
    - Soft drop shadow (texture copy offset)
    - 1px/2px outline (four texture copies around)
    - Optional translucent backdrop panel (rounded if available)

    Notes:
    - Implemented entirely with extra canvas instructions using the label's
      own texture; fast and shader-free.
    - Update bindings ensure the effect stays in sync with text, size, and pos.
    """

    theme_manager = ObjectProperty(None, allownone=True)

    # Shadow
    use_shadow = BooleanProperty(True)
    shadow_color = ListProperty([0, 0, 0, 0.55])
    shadow_offset = ListProperty([0, -2])  # dx, dy in px

    # Outline
    use_outline = BooleanProperty(True)
    outline_color = ListProperty([0, 0, 0, 0.35])
    outline_width = NumericProperty(1.0)  # px
    outline_offset = ListProperty([0.0, 0.0])  # extra dx,dy for outline only

    # Backdrop panel
    use_backdrop = BooleanProperty(False)
    backdrop_color = ListProperty([0, 0, 0, 0.25])
    backdrop_radius = NumericProperty(12.0)
    backdrop_pad = ListProperty([10.0, 6.0])  # x, y padding in px
    # Optional glass accent: subtle 1px highlight on top edge + 2px inner shadow on bottom
    glass = BooleanProperty(False)
    glass_hi_color = ListProperty([1, 1, 1, 0.10])
    glass_sh_color = ListProperty([0, 0, 0, 0.22])
    glass_hi_px = NumericProperty(1.0)
    glass_sh_px = NumericProperty(2.0)

    # Backdrop outline
    use_backdrop_outline = BooleanProperty(True)
    backdrop_outline_width = NumericProperty(1.0)
    backdrop_outline_color = ListProperty([0.6, 0.6, 0.6, 1.0])  # NEW: explicit color

    # Debug visualization: tint layers so you can see what's active
    debug_visualize = BooleanProperty(False)
    debug_shadow_color = ListProperty([1.0, 0.0, 1.0, 0.85])   # magenta
    debug_outline_color = ListProperty([0.1, 1.0, 0.1, 0.90])  # lime
    debug_backdrop_color = ListProperty([0.0, 0.6, 0.9, 0.25]) # cyan glass

    # Align helpers exactly to the text texture within the widget
    # Default False to preserve legacy behavior unless explicitly enabled per-label
    align_to_text = BooleanProperty(False)

    # --- NEW DEBUG FLAGS ---
    debug_refs = BooleanProperty(False)
    debug_bounds = BooleanProperty(False)
    debug_verbose = BooleanProperty(False)

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        if self.theme_manager is None:
            self.theme_manager = ThemeManager()
        with self.canvas.before:
            # Backdrop (optional)
            self._bk_col = Color(*self.backdrop_color)
            if _HAS_ROUNDED:
                self._bk_rect = RoundedRectangle(pos=self.pos, size=self.size,
                                                 radius=[self.backdrop_radius] * 4)
            else:
                self._bk_rect = Rectangle(pos=self.pos, size=self.size)
            # Glass accents (top highlight + bottom inner shadow)
            self._bk_hi_col = Color(*self.glass_hi_color)
            self._bk_hi_rect = Rectangle(pos=self.pos, size=(0, 0))
            self._bk_sh_col = Color(*self.glass_sh_color)
            self._bk_sh_rect = Rectangle(pos=self.pos, size=(0, 0))

            # Backdrop Outline (optional)
            self._bko_col = Color(0, 0, 0, 0)
            if _HAS_ROUNDED:
                # Use a stroked outline, not a filled rect
                self._bko_outline = Line(rounded_rectangle=(self.x, self.y, self.width, self.height, self.backdrop_radius),
                                        width=self.backdrop_outline_width,
                                        group='bko_outline')
            else:
                self._bko_outline = Line(rectangle=(self.x, self.y, self.width, self.height),
                                        width=self.backdrop_outline_width,
                                        group='bko_outline')

            # Shadow + outline layers share the label's texture
            self._sh_col = Color(*self.shadow_color)
            self._sh_rect = Rectangle(texture=self.texture, pos=self.pos, size=(0, 0))

            self._ol_col = Color(*self.outline_color)
            # Four directions
            self._ol_up = Rectangle(texture=self.texture, pos=self.pos, size=(0, 0))
            self._ol_dn = Rectangle(texture=self.texture, pos=self.pos, size=(0, 0))
            self._ol_lt = Rectangle(texture=self.texture, pos=self.pos, size=(0, 0))
            self._ol_rt = Rectangle(texture=self.texture, pos=self.pos, size=(0, 0))

        # Keep effects in sync
        for prop in ("texture", "pos", "size", "text_size"):
            self.bind(**{prop: self._refresh})
        for prop in ("use_shadow", "shadow_color", "shadow_offset",
                     "use_outline", "outline_color", "outline_width",
                     "outline_offset",
                     "use_backdrop", "backdrop_color", "backdrop_radius", "backdrop_pad",
                     "glass", "glass_hi_color", "glass_sh_color", "glass_hi_px", "glass_sh_px",
                     "debug_visualize", "debug_shadow_color", "debug_outline_color", "debug_backdrop_color",
                     "align_to_text",
                     "use_backdrop_outline", "backdrop_outline_width", "theme_manager"):
            self.bind(**{prop: self._refresh})

        # Also react to opacity changes so outline/shadow/backdrop track fades
        self.bind(opacity=self._refresh)

        # --- NEW DEBUG FLAGS ---
        for prop in ("debug_refs", "debug_bounds", "debug_verbose"):
            self.bind(**{prop: self._refresh})

        self._refresh()

    def _get_scale(self):
        """Calculates the current scale of the widget."""
        # Get the window object
        win = self.get_root_window()
        if not win:
            return 1.0, 1.0

        # Transform a unit vector to find the scale
        p1 = self.to_window(0, 0)
        p2 = self.to_window(1, 0)
        p3 = self.to_window(0, 1)

        scale_x = ((p2[0] - p1[0]) ** 2 + (p2[1] - p1[1]) ** 2) ** 0.5
        scale_y = ((p3[0] - p1[0]) ** 2 + (p3[1] - p1[1]) ** 2) ** 0.5
        
        return scale_x, scale_y

    def _refresh(self, *args):
        tw, th = (self.texture.size if self.texture else (0, 0))
        if tw <= 0 or th <= 0:
            # Hide helper rects until we have a texture
            for r in (self._sh_rect, self._ol_up, self._ol_dn, self._ol_lt, self._ol_rt):
                r.size = (0, 0)
            self._bk_rect.size = (0, 0)
            if _HAS_ROUNDED:
                self._bko_outline.rounded_rectangle = (0, 0, 0, 0, 0)
            else:
                self._bko_outline.rectangle = (0, 0, 0, 0)
            return

        # --- SCALING FIX ---
        # Calculate the current scale of the widget
        scale_x, scale_y = self._get_scale()
        # Avoid division by zero
        if scale_x == 0: scale_x = 1.0
        if scale_y == 0: scale_y = 1.0
        
        # Determine where the label actually draws its glyph texture
        if self.align_to_text and self.texture is not None:
            tw, th = self.texture.size
            px = 0.0
            py = 0.0
            try:
                pad = self.padding
                if isinstance(pad, (list, tuple)) and len(pad) >= 2:
                    px, py = float(pad[0] or 0), float(pad[1] or 0)
            except Exception:
                pass
            # Horizontal alignment
            hal = getattr(self, 'halign', 'left')
            if hal == 'center':
                tx = self.x + (self.width - tw) / 2.0
            elif hal == 'right':
                tx = self.right - tw - px
            else:  # left
                tx = self.x + px
            # Vertical alignment
            val = getattr(self, 'valign', 'bottom')
            if val in ('middle', 'center'):
                ty = self.y + (self.height - th) / 2.0
            elif val == 'top':
                ty = self.top - th - py
            else:  # bottom
                ty = self.y + py
            base_pos = (tx, ty)
        else:
            base_pos = self.pos
        base_size = (tw, th)

        # Backdrop
        _bk_base = (self.debug_backdrop_color if self.debug_visualize else self.backdrop_color)
        self._bk_col.rgba = (_bk_base[0], _bk_base[1], _bk_base[2], _bk_base[3] * float(self.opacity))
        if self.use_backdrop:
            pad_x, pad_y = self.backdrop_pad
            
            # Calculate backdrop width and height
            bw = max(self.width, tw + 2 * pad_x)
            bh = th + 2 * pad_y

            # Calculate backdrop x-position to center it within the ShadowLabel's width
            # The ShadowLabel itself is now centered by its pos_hint in KV.
            # So, we want the backdrop to be centered within the ShadowLabel's own bounds.
            bx = self.x + (self.width - bw) / 2.0
            by = base_pos[1] - pad_y # Vertical position can still be relative to text bottom

            self._bk_rect.pos = (bx, by)
            self._bk_rect.size = (bw, bh)
            if _HAS_ROUNDED:
                self._bk_rect.radius = [self.backdrop_radius] * 4
            # Glass accents
            if self.glass:
                self._bk_hi_col.rgba = (self.glass_hi_color[0], self.glass_hi_color[1], self.glass_hi_color[2], self.glass_hi_color[3] * float(self.opacity))
                self._bk_sh_col.rgba = (self.glass_sh_color[0], self.glass_sh_color[1], self.glass_sh_color[2], self.glass_sh_color[3] * float(self.opacity))
                # 1px highlight along top edge
                self._bk_hi_rect.pos = (bx, by + bh - self.glass_hi_px)
                self._bk_hi_rect.size = (bw, self.glass_hi_px)
                # 2px inner shadow just inside bottom edge
                self._bk_sh_rect.pos = (bx, by)
                self._bk_sh_rect.size = (bw, self.glass_sh_px)
            else:
                self._bk_hi_rect.size = (0, 0)
                self._bk_sh_rect.size = (0, 0)

            # Backdrop Outline
            _acc = self.theme_manager.col('accent')  # theme accent color
            try:
                self._bko_col.rgba = (_acc[0], _acc[1], _acc[2], _acc[3] * float(self.opacity))
            except Exception:
                # Fallback in case theme color is not RGBA
                self._bko_col.rgba = (1, 1, 1, 1 * float(self.opacity))
            if self.use_backdrop_outline:
                outline_w = float(self.backdrop_outline_width)

                # Expand the stroke by half the width on each side so it hugs the backdrop
                ox = bx - outline_w * 0.5
                oy = by - outline_w * 0.5
                ow = bw + outline_w
                oh = bh + outline_w

                if _HAS_ROUNDED:
                    self._bko_outline.rounded_rectangle = (ox, oy, ow, oh, self.backdrop_radius)
                else:
                    self._bko_outline.rectangle = (ox, oy, ow, oh)

                # Ensure stroke width is applied
                for instruction in self.canvas.before.get_group('bko_outline'):
                    if hasattr(instruction, 'width'):
                        instruction.width = outline_w
            else:
                # Hide outline cleanly
                if _HAS_ROUNDED:
                    self._bko_outline.rounded_rectangle = (0, 0, 0, 0, 0)
                else:
                    self._bko_outline.rectangle = (0, 0, 0, 0)

        else:
            self._bk_rect.size = (0, 0)
            if _HAS_ROUNDED:
                self._bko_outline.rounded_rectangle = (0, 0, 0, 0, 0)
            else:
                self._bko_outline.rectangle = (0, 0, 0, 0)
            self._bk_hi_rect.size = (0, 0)
            self._bk_sh_rect.size = (0, 0)

        # Shadow
        if self.use_shadow:
            _sh_base = (self.debug_shadow_color if self.debug_visualize else self.shadow_color)
            self._sh_col.rgba = (_sh_base[0], _sh_base[1], _sh_base[2], _sh_base[3] * float(self.opacity))
            dx, dy = self.shadow_offset
            
            # --- SCALING FIX ---
            # Un-scale the shadow offset
            unscaled_dx = dx / scale_x
            unscaled_dy = dy / scale_y

            self._sh_rect.texture = self.texture
            self._sh_rect.pos = (base_pos[0] + unscaled_dx, base_pos[1] + unscaled_dy)
            self._sh_rect.size = base_size
        else:
            # Forcefully hide shadow: zero size and zero alpha
            self._sh_rect.size = (0, 0)
            try:
                r, g, b, _ = self.shadow_color
                self._sh_col.rgba = (r, g, b, 0.0)
            except Exception:
                self._sh_col.a = 0.0

        # Outline
        _ol_base = (self.debug_outline_color if self.debug_visualize else self.outline_color)
        self._ol_col.rgba = (_ol_base[0], _ol_base[1], _ol_base[2], _ol_base[3] * float(self.opacity))
        if self.use_outline:
            w = float(self.outline_width)
            
            # --- SCALING FIX ---
            # Un-scale the outline width, using an average of x and y scales
            avg_scale = (scale_x + scale_y) / 2.0
            unscaled_w = w / avg_scale

            ox, oy = (self.outline_offset[0] if isinstance(self.outline_offset, (list, tuple)) and len(self.outline_offset) > 0 else 0.0,
                      self.outline_offset[1] if isinstance(self.outline_offset, (list, tuple)) and len(self.outline_offset) > 1 else 0.0)
            for r in (self._ol_up, self._ol_dn, self._ol_lt, self._ol_rt):
                r.texture = self.texture
                r.size = base_size
            self._ol_up.pos = (base_pos[0] + ox, base_pos[1] + oy + unscaled_w)
            self._ol_dn.pos = (base_pos[0] + ox, base_pos[1] + oy - unscaled_w)
            self._ol_lt.pos = (base_pos[0] + ox - unscaled_w, base_pos[1] + oy)
            self._ol_rt.pos = (base_pos[0] + ox + unscaled_w, base_pos[1] + oy)
        else:
            for r in (self._ol_up, self._ol_dn, self._ol_lt, self._ol_rt):
                r.size = (0, 0)


        # --- NEW DEBUG OVERLAYS ---
        if self.debug_bounds:
            with self.canvas.after:
                Color(1, 0, 1, 0.5)
                Line(rectangle=(self.x, self.y, self.width, self.height), width=1)
                Color(0, 1, 1, 0.5)
                Line(rectangle=(base_pos[0], base_pos[1], base_size[0], base_size[1]), width=1)

        if self.debug_refs and hasattr(self, "refs") and self.refs:
            with self.canvas.after:
                for ref, boxes in self.refs.items():
                    Color(random.random(), random.random(), random.random(), 0.4)
                    for x1, y1, x2, y2 in boxes:
                        pos = (x1, y1)
                        size = (x2 - x1, y2 - y1)
                        Rectangle(pos=pos, size=size)

    def on_touch_down(self, touch):
        """If debug_verbose is on, print details of any ref under the touch."""
        # --- REVISED DEBUG LOGIC ---
        # The debug check should not interfere with the event flow.
        # It should only inspect the touch and print if a ref is found.
        # The actual event handling is always passed to the parent method.
        if self.debug_verbose and self.collide_point(*touch.pos):
            if hasattr(self, "refs") and self.refs:
                for ref, boxes in self.refs.items():
                    for x1, y1, x2, y2 in boxes:
                        if x1 <= touch.x <= x2 and y1 <= touch.y <= y2:
                            print(f"[ShadowLabel DEBUG] Touched ref='{ref}'")
                            # Don't return here; let the event pass through normally.
                            break # Found the ref for this touch, no need to check others.
        return super().on_touch_down(touch)