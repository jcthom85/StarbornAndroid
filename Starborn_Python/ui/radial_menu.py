# In StarBorn/ui/radial_menu.py

from kivy.uix.scatter import Scatter
from kivy.clock import Clock
from kivy.metrics import dp
from kivy.uix.behaviors import ButtonBehavior
from kivy.uix.image import Image
from kivy.uix.floatlayout import FloatLayout
from kivy.animation import Animation
from kivy.core.window import Window
import math

class ButtonBehaviorImage(ButtonBehavior, Image):
    pass

class RadialMenu(FloatLayout):
    """
    A radial menu that animates icons outward from a center widget in a circle.
    """

    def __init__(self, icons, center_widget, radius=dp(40), button_size=dp(24),
                 start_angle=180, angle_range=180, duration=0.15, offset_y=0, **kwargs):
        super().__init__(**kwargs)
        self.icons = icons
        self.center_widget = center_widget
        self.radius = radius
        self.button_size = button_size
        self.start_angle = start_angle
        self.angle_range = angle_range
        self.duration = duration
        self.offset_y = offset_y

        self.size = Window.size
        self.pos = (0, 0)

        Clock.schedule_once(self.build_menu, 0)

    # ---------------------------------------------------------------------
    # Build & animate
    # ---------------------------------------------------------------------
    def build_menu(self, *_):
        self.clear_widgets()

        # find the window-center of the anchor widget
        win_cx, win_cy = self.center_widget.to_window(
            self.center_widget.center_x,
            self.center_widget.center_y,
            initial=False
        )
        cx, cy = self.to_widget(win_cx, win_cy)
        cy += self.offset_y
        self._cx, self._cy = cx, cy

        # separate out the Settings icon
        main_icons = [(n, s, c) for (n, s, c) in self.icons if n != 'settings']
        settings_icon = next(((n, s, c) for (n, s, c) in self.icons if n == 'settings'), None)
        main_count = len(main_icons)
        step = self.angle_range / (main_count - 1) if main_count > 1 else 0

        for name, src, callback in self.icons:
            # 1. Create the icon image
            btn = ButtonBehaviorImage(
                source=src,
                size_hint=(None, None),
                size=(self.button_size, self.button_size),
                allow_stretch=True,
                keep_ratio=True
            )
            btn.callback = callback
            btn.bind(on_release=self._on_icon_release)

            # 2. Wrap in a Scatter for rotation
            scatter = Scatter(
                size_hint=(None, None),
                size=btn.size,
                do_rotation=True,
                do_scale=False,
                do_translation=False,
                auto_bring_to_front=False
            )
            scatter.center = (cx, cy)
            scatter.add_widget(btn)
            self.add_widget(scatter)

            # 3. Choose angle & radius
            if name == 'settings':
                # Shoot straight up toward the middle of the screen
                angle = 90
                # Push farther than the other icons so it lands near mid‑screen
                r     = self.radius * 1.96

            else:
                idx = main_icons.index((name, src, callback))
                angle = self.start_angle + idx * step
                r = self.radius

            rad = math.radians(angle)
            tx = cx + math.cos(rad) * r
            ty = cy + math.sin(rad) * r

            # 4. Animate to (tx, ty) with a spin
            anim = Animation(
                center_x=tx,
                center_y=ty,
                rotation=360,
                duration=self.duration,
                t='out_quad'
            )
            anim.bind(on_complete=lambda inst, w=scatter: setattr(w, 'rotation', 0))
            anim.start(scatter)

    # ------------------------------------------------------------------
    def _on_icon_release(self, btn):
        if btn.callback:
            btn.callback()
        self.dismiss()

    # -------------------------------------------------------------------
    #  Reverse-animation then self-destruct
    # -------------------------------------------------------------------
    def dismiss(self, *_):
        for child in list(self.children):
            Animation.cancel_all(child)
            Animation(
                center_x=self._cx,
                center_y=self._cy,
                rotation=0,
                duration=self.duration,
                t='in_quad'
            ).start(child)
        Clock.schedule_once(lambda *_: (
            self.parent and self.parent.remove_widget(self)
        ), self.duration)

    # -------------------------------------------------------------------
    #  Close if tapping outside any icon
    # -------------------------------------------------------------------
    def on_touch_down(self, touch):
        if any(child.collide_point(*touch.pos) for child in self.children):
            return super().on_touch_down(touch)
