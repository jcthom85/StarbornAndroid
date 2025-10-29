# ui/hub_screen.py
from __future__ import annotations
import os, json, math, random
from typing import Tuple, Callable, Optional

from kivy.app import App
from kivy.clock import Clock
from kivy.core.image import Image as CoreImage
from kivy.metrics import dp
from kivy.properties import StringProperty, NumericProperty
from kivy.uix.behaviors import ButtonBehavior
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.image import Image
from kivy.graphics import PushMatrix, PopMatrix, Translate, Rotate, Scale
from kivy.uix.screenmanager import Screen
from font_manager import fonts
from ui.shadow_label import ShadowLabel

from ui.radial_menu import RadialMenu, ButtonBehaviorImage
from ui.blurred_extension import BlurredExtension

#from PyQt5.QtWidgets import QWidget, QVBoxLayout, QHBoxLayout, QLabel, QSpacerItem, QSizePolicy
#from PyQt5.QtCore import Qt
from ui.bordered_frame import BorderedFrame
from ui.minimap_widget import MinimapWidget

# -----------------------------------------------------------------------------
# Aspect-constrained stage (9:21)
# -----------------------------------------------------------------------------
class AspectStage(FloatLayout):
    def __init__(self, aspect_w=9.0, aspect_h=21.0, **kw):
        super().__init__(**kw)
        self.aspect_w = float(aspect_w)
        self.aspect_h = float(aspect_h)
        self.stage = FloatLayout()
        self.add_widget(self.stage)
        self.bind(size=self._relayout, pos=self._relayout)

    def _relayout(self, *_):
        W, H = self.size
        if W <= 0 or H <= 0:
            return
        target = self.aspect_w / self.aspect_h
        cur = W / H
        if cur > target:
            h = H
            w = H * target
            x = self.x + (W - w) * 0.5
            y = self.y
        else:
            w = W
            h = W / target
            x = self.x
            y = self.y + (H - h) * 0.5
        self.stage.size = (w, h)
        self.stage.pos  = (x, y)


# -----------------------------------------------------------------------------
# Transparent HUD - matches overlay title spacing and room title font
# -----------------------------------------------------------------------------
class HubHUD(FloatLayout):
    """Header bar with a centered Hub title, spaced down to match overlays."""
    TITLE_TOP_PADDING = dp(18)
    TITLE_BOT_PADDING = dp(8)

    def __init__(self, **kw):
        h = kw.pop("height", dp(50))
        super().__init__(size_hint=(1, None), height=h, **kw)

        self._title = ShadowLabel(
            text="",
            halign="center", valign="middle",
            size_hint=(None, None),
            # --- ADDED: Shadow and outline properties ---
            use_shadow=True,
            shadow_color=[0, 0, 0, 0.65],
            shadow_offset=[-8, -4],
            use_outline=True,
            outline_color=[0, 0, 0, 0.35],
            outline_width=dp(1),
        )
        self._title.bind(texture_size=lambda w, ts: setattr(w, "size", ts))
        self._title.pos_hint = {"center_x": 0.5, "center_y": 0.5}

        # --- ADDED: Underline image ---
        self._underline = Image(
            source='images/ui/underline_4.png',
            allow_stretch=True,
            keep_ratio=False,
            size_hint=(None, None),
            opacity=0
        )
        # Ensure pixel-perfect rendering
        self._underline.texture.mag_filter = 'nearest'
        self._underline.texture.min_filter = 'nearest'
        self.add_widget(self._underline)
        self.add_widget(self._title)

    def set_title(self, txt: str):
        app = App.get_running_app()
        try:
            fonts = app.fonts
            if "overlay_title" in fonts:
                self._title.font_name = fonts["overlay_title"]["name"]
                self._title.font_size = fonts["overlay_title"]["size"]
            else:
                self._title.font_name = fonts["room_title"]["name"]
                self._title.font_size = fonts["room_title"]["size"]
        except Exception:
            pass
        self._title.text = txt or "Hub"
        Clock.schedule_once(self._fit_to_title, 0)

    def _fit_to_title(self, *_):
        th = (self._title.texture_size[1] or 0)
        min_h = dp(50)
        self.height = max(min_h, th + self.TITLE_TOP_PADDING + self.TITLE_BOT_PADDING)
        self._update_underline()

    def _update_underline(self, *args):
        """Positions and sizes the underline beneath the title label."""
        if not self._title or not self._underline:
            return

        if not self._title.text:
            self._underline.opacity = 0
            return

        # Position the underline relative to the title
        title_width = self._title.texture_size[0]
        self._underline.width = title_width + dp(20)  # Add some padding
        self._underline.height = max(dp(32), int(self._title.font_size * 0.62))
        self._underline.center_x = self._title.center_x
        self._underline.top = self._title.y - dp(4) # Position just below the title
        self._underline.opacity = 1

# -----------------------------------------------------------------------------
# Node widget (click to enter; right-click drag to reposition)
# -----------------------------------------------------------------------------
class NodeButton(ButtonBehavior, FloatLayout):
    idle_img_dx = NumericProperty(0.0)
    idle_img_dy = NumericProperty(0.0)
    idle_img_angle = NumericProperty(0.0)
    idle_img_scale = NumericProperty(1.0)
    idle_lbl_dy = NumericProperty(0.0)

    def __init__(self, node: dict, size_px: Tuple[float, float],
                 save_cb: Optional[Callable[[dict, dict], None]] = None, **kw):
        super().__init__(**kw)
        self.node = node
        self._design_size = tuple(node.get("size", [256, 256]))
        self._title_gap = float(node.get("title_gap", 0) or 0)
        self.save_cb = save_cb
        self.size_hint = (None, None)
        self.size = size_px
        self.pos_hint = node.get("pos_hint", {"center_x": 0.5, "center_y": 0.5})

        self.img = Image(
            source=node.get("icon_image", ""),
            allow_stretch=True,
            keep_ratio=True,
            size_hint=(1, 1),
            pos_hint={"center_x": 0.5, "center_y": 0.5},
        )
        self.add_widget(self.img)

        caption = node.get("title") or node.get("name") or node.get("id") or ""
        label_h = fonts["node_label"]["size"] + dp(2)
        self.lbl = ShadowLabel(
            text=caption,
            color=(1, 1, 1, 1),
            font_size=fonts["node_label"]["size"],
            font_name=fonts["node_label"]["name"],
            size_hint=(1, None),
            height=label_h,
            halign="center", valign="bottom",
            pos_hint={"center_x": 0.5},
            use_shadow=False,
            use_outline=True,
            outline_color=[0, 0, 0, 0.35],
            outline_width=dp(1.6),
        )
        self.lbl.bind(size=lambda w, *_: setattr(w, "text_size", (w.width, None)))
        self.lbl.bind(texture_size=lambda instance, size: setattr(instance, "height", size[1]))
        self.add_widget(self.lbl)

        self._dragging = False
        self._drag_touch = None
        self._drag_off = (0.0, 0.0)

        self.bind(size=self._update_title_gap, pos=self._update_title_gap)
        Clock.schedule_once(lambda *_: self._update_title_gap(), 0)

        tau = getattr(math, "tau", math.pi * 2.0)

        self._idle_event = None
        self._idle_elapsed = random.uniform(0.0, tau)
        self._idle_paused = False
        self._idle_freq = {
            "x": random.uniform(1.85, 1.95),
            "y": random.uniform(1.8, 1.98),
            "angle": random.uniform(0.3, 0.5),
            "scale": random.uniform(1.65, 1.85),
        }
        self._idle_phase = {
            "x": random.uniform(0, tau),
            "y": random.uniform(0, tau),
            "angle": random.uniform(0, tau),
            "scale": random.uniform(0, tau),
        }
        self._idle_tilt_amp = random.uniform(1.0, 2.5)
        self._idle_scale_amp = random.uniform(0.012, 0.028)
        self._idle_amp_factor_x = random.uniform(0.65, 1.05)
        self._idle_amp_factor_y = random.uniform(0.75, 1.15)

        self._init_idle_motion()
        Clock.schedule_once(self._start_idle_animation, random.uniform(0.25, 0.6))

    def _init_idle_motion(self):
        with self.img.canvas.before:
            PushMatrix()
            self._img_center_to_origin = Translate(0, 0, 0)
            self._img_rotate = Rotate(angle=0, axis=(0, 0, 1))
            self._img_scale = Scale(1.0, 1.0, 1.0)
            self._img_origin_back = Translate(0, 0, 0)
            self._img_translate = Translate(0, 0, 0)
        with self.img.canvas.after:
            PopMatrix()

        with self.lbl.canvas.before:
            PushMatrix()
            self._lbl_translate = Translate(0, 0, 0)
        with self.lbl.canvas.after:
            PopMatrix()

        self.bind(
            idle_img_dx=self._apply_img_transform,
            idle_img_dy=self._apply_img_transform,
            idle_img_angle=self._apply_img_transform,
            idle_img_scale=self._apply_img_transform,
            size=self._apply_img_transform,
            pos=self._apply_img_transform,
        )
        self.img.bind(size=self._apply_img_transform, pos=self._apply_img_transform)
        self.bind(idle_lbl_dy=self._apply_lbl_transform,
                  idle_img_dx=self._apply_lbl_transform)
        self._apply_img_transform()
        self._apply_lbl_transform()

    def _apply_img_transform(self, *_):
        if not hasattr(self, "_img_translate"):
            return
        cx = self.img.width * 0.5
        cy = self.img.height * 0.5
        self._img_center_to_origin.x = cx
        self._img_center_to_origin.y = cy
        self._img_rotate.angle = self.idle_img_angle
        self._img_scale.x = self.idle_img_scale
        self._img_scale.y = self.idle_img_scale
        self._img_scale.z = 1.0
        self._img_origin_back.x = -cx
        self._img_origin_back.y = -cy
        self._img_translate.x = self.idle_img_dx
        self._img_translate.y = self.idle_img_dy

    def _apply_lbl_transform(self, *_):
        if not hasattr(self, "_lbl_translate"):
            return
        self._lbl_translate.x = 0.0
        self._lbl_translate.y = 0.0

    def _start_idle_animation(self, *_):
        if self._idle_event is None and self.parent is not None:
            self._idle_event = Clock.schedule_interval(self._update_idle_frame, 1.0 / 30.0)

    def _stop_idle_animation(self):
        if self._idle_event is not None:
            self._idle_event.cancel()
            self._idle_event = None

    def _update_idle_frame(self, dt: float):
        if self._idle_paused:
            return
        self._idle_elapsed += dt
        t = self._idle_elapsed
        size_max = max(self.width, self.height) or 1.0
        amp_x = min(dp(0), size_max * 0.01) * self._idle_amp_factor_x
        amp_y = min(dp(0), size_max * 0.01) * self._idle_amp_factor_y

        # --- THIS IS THE FIX ---
        # Decouple amplitude from on-screen size. Use the node's design size instead.
        design_max = max(self._design_size) if self._design_size else 256.0
        amp_x = min(dp(1), design_max * 0.02) * self._idle_amp_factor_x # Horizontal motion (unchanged)
        amp_y = min(dp(1), design_max * 0.002) * self._idle_amp_factor_y  # Reduced vertical motion

        dx = math.sin(t * self._idle_freq["x"] + self._idle_phase["x"]) * amp_x
        dy = math.sin(t * self._idle_freq["y"] + self._idle_phase["y"]) * amp_y
        angle = math.sin(t * self._idle_freq["angle"] + self._idle_phase["angle"]) * self._idle_tilt_amp
        scale = 1.0 + math.sin(t * self._idle_freq["scale"] + self._idle_phase["scale"]) * self._idle_scale_amp

        self.idle_img_dx = dx
        self.idle_img_dy = dy
        self.idle_img_angle = angle
        self.idle_img_scale = scale
        self.idle_lbl_dy = dy * 0.55

    def on_parent(self, instance, value):
        parent_handler = getattr(super(), "on_parent", None)
        result = parent_handler(instance, value) if callable(parent_handler) else None
        if value is None:
            self._stop_idle_animation()
        elif self._idle_event is None:
            self._start_idle_animation()
        return result

    def on_release(self):
        if not self._dragging:
            App.get_running_app().enter_node(self.node)

    def on_touch_down(self, touch):
        if not self.collide_point(*touch.pos):
            return super().on_touch_down(touch)

        button = getattr(touch, "button", "left")
        if button == "right":
            self._dragging = True
            self._idle_paused = True
            self.idle_img_dx = 0.0
            self.idle_img_dy = 0.0
            self.idle_img_angle = 0.0
            self.idle_img_scale = 1.0
            self.idle_lbl_dy = 0.0
            self._drag_touch = touch
            touch.grab(self)
            self._drag_off = (self.center_x - touch.x, self.center_y - touch.y)
            return True
        return super().on_touch_down(touch)

    def on_touch_move(self, touch):
        if touch.grab_current is self and self._dragging:
            p = self.parent
            cx_abs = touch.x + self._drag_off[0]
            cy_abs = touch.y + self._drag_off[1]

            half_w, half_h = self.width / 2.0, self.height / 2.0
            min_x, max_x = p.x + half_w, p.right - half_w
            min_y, max_y = p.y + half_h, p.top - half_h
            cx_abs = max(min_x, min(max_x, cx_abs))
            cy_abs = max(min_y, min(max_y, cy_abs))

            cx_norm = (cx_abs - p.x) / float(p.width or 1)
            cy_norm = (cy_abs - p.y) / float(p.height or 1)
            self.pos_hint = {"center_x": cx_norm, "center_y": cy_norm}
            self.node["pos_hint"] = self.pos_hint
            return True
        return super().on_touch_move(touch)

    def on_touch_up(self, touch):
        if touch.grab_current is self:
            touch.ungrab(self)
            dragging = self._dragging
            self._dragging = False
            self._drag_touch = None
            self._idle_paused = False
            if dragging:
                if callable(self.save_cb):
                    self.save_cb(self.node, self.node.get("pos_hint", {}))
                return True
        return super().on_touch_up(touch)

    def sync_from_node(self):
        size_def = self.node.get("size")
        if isinstance(size_def, (list, tuple)) and len(size_def) >= 2:
            self._design_size = (float(size_def[0] or 0.0), float(size_def[1] or 0.0))
        self._title_gap = float(self.node.get("title_gap", 0) or 0)
        caption = self.node.get("title") or self.node.get("name") or self.node.get("id") or ""
        if caption != self.lbl.text:
            self.lbl.text = caption
        self._update_title_gap()

    def _on_size_changed(self, *_):
        self._update_title_gap()

    def _update_title_gap(self, *_):
        if not hasattr(self, "lbl"):
            return

        design_h = float(self._design_size[1]) if isinstance(self._design_size, (list, tuple)) and len(self._design_size) >= 2 and self._design_size[1] else 1.0
        current_h = self.height if self.height > 0 else 1.0

        scale_y = current_h / design_h

        self.lbl.y = self.y - (self._title_gap * scale_y)


# Hub Screen
# -----------------------------------------------------------------------------
class HubScreen(Screen):
    hub_id   = StringProperty("")
    design_w = NumericProperty(1080)
    design_h = NumericProperty(2520)
    ALLOW_HUB_EDIT_PERSIST = True

    def __init__(self, **kw):
        super().__init__(**kw)
        root = FloatLayout(size_hint=(1, 1))
        self.add_widget(root)

        # Background layers (drawn first)
        self._bg_top_blur = BlurredExtension(
            fade_direction='bottom', size_hint=(1, None),
            blur_intensity=2.5, fade_start=0.9, fade_end=0.2, fade_power=1.5
        )
        self._bg_bottom_blur = BlurredExtension(
            fade_direction='top', size_hint=(1, None),
            blur_intensity=2.5, fade_start=0.9, fade_end=0.2, fade_power=1.5
        )
        root.add_widget(self._bg_top_blur)
        root.add_widget(self._bg_bottom_blur)

        # Main content stage
        self._aspect = AspectStage(9, 21, size_hint=(1, 1))
        root.add_widget(self._aspect)

        self._bg = Image(allow_stretch=True, keep_ratio=False, size_hint=(1, 1))
        self._aspect.stage.add_widget(self._bg)

        self._nodes_layer = FloatLayout(size_hint=(1, 1))
        self._aspect.stage.add_widget(self._nodes_layer)

        # UI Overlay layers
        self._hud = HubHUD(pos_hint={"top": 0.96, "x": 0})
        root.add_widget(self._hud)

        self._viewport = FloatLayout(size_hint=(1, 1))
        root.add_widget(self._viewport)

        self.menu_button = ButtonBehaviorImage(
            source='images/ui/menu_button.png',
            size_hint=(None, None), size=(dp(192), dp(192)),
            pos_hint={'center_x': 0.5, 'y': 0.011},
            allow_stretch=True, keep_ratio=True,
        )
        self.menu_button.bind(on_release=self._toggle_radial_menu)
        root.add_widget(self.menu_button)

        self._aspect.stage.bind(pos=self._update_background_layout, size=self._update_background_layout)
        self.bind(size=self._update_background_layout)
        self._radial = None

    def _update_background_layout(self, *args):
        stage_y = self._aspect.stage.y
        stage_top = self._aspect.stage.top
        window_height = self.height

        self._bg_top_blur.height = max(0, window_height - stage_top)
        self._bg_top_blur.pos = (0, stage_top)

        self._bg_bottom_blur.height = max(0, stage_y)
        self._bg_bottom_blur.pos = (0, 0)


    def on_pre_enter(self, *_):
        self._build()
        Clock.schedule_once(self._update_background_layout, -1)

    def on_leave(self, *_):
        if self._radial:
            try: self._radial.dismiss()
            except Exception: pass
            self._radial = None

    def _build(self):
        app = App.get_running_app()
        wm = getattr(app, "current_game", None) and app.current_game.world_manager
        if not wm: return

        if not self.hub_id:
            self.hub_id = wm.current_hub_id or next(iter(wm.hubs.keys()), "")

        hub = wm.hubs.get(self.hub_id, {})
        self._hud.set_title(hub.get("title") or hub.get("name") or "Hub")

        bg_path = hub.get("background_image", "")
        self._update_design_size_from_image(bg_path)
        if bg_path and os.path.exists(bg_path):
            texture = CoreImage(bg_path).texture
            # Clamp to edge so blurred extensions sample the border instead of repeating
            try:
                texture.wrap = 'clamp_to_edge'
            except Exception:
                pass
            self._bg.texture = texture
            self._bg_top_blur.texture = texture
            self._bg_bottom_blur.texture = texture
        else:
            self._bg.texture = self._bg_top_blur.texture = self._bg_bottom_blur.texture = None

        self._nodes_layer.clear_widgets()
        game = getattr(app, "current_game", None)
        for node in wm.get_nodes_for_hub(self.hub_id):
            node_id = node.get("id")
            if game and node_id and hasattr(game, "is_node_discovered"):
                if not game.is_node_discovered(node_id):
                    continue
            size_px = self._scaled_size(node.get("size", [256, 256]))
            btn = NodeButton(node=node, size_px=size_px, save_cb=self._persist_node_position)
            self._nodes_layer.add_widget(btn)

        Clock.schedule_once(lambda dt: self._rescale_nodes(), 0)

    def _toggle_radial_menu(self, *_):
        if self._radial:
            self._radial.dismiss()
            self._radial = None
            return

        app = App.get_running_app()
        icons = [
            ('journal',   'images/ui/journal_icon.png',   lambda: app.open_menu('journal')),
            ('stats',     'images/ui/stats_icon.png',     lambda: app.open_menu('stats')),
            ('inventory', 'images/ui/inventory_icon.png', lambda: app.open_menu('inventory')),
            ('settings',  'images/ui/settings_icon.png',  lambda: app.open_menu('settings')),
        ]
        radial = RadialMenu(
            icons=icons[::-1], center_widget=self.menu_button, radius=dp(168),
            button_size=dp(124), start_angle=30, angle_range=120, duration=0.22,
            offset_y=dp(0),
        )
        self._viewport.add_widget(radial)
        self._radial = radial

    def _update_design_size_from_image(self, path: str):
        try:
            if path and os.path.exists(path):
                tex = CoreImage(path).texture
                if tex and tex.size:
                    self.design_w, self.design_h = map(float, tex.size)
        except Exception: pass

    def _scaled_size(self, size_xy) -> Tuple[float, float]:
        sw, sh = self._aspect.stage.size
        if not sw or not sh: return (size_xy[0], size_xy[1])
        sx = sw / float(self.design_w or 1)
        sy = sh / float(self.design_h or 1)
        return size_xy[0] * sx, size_xy[1] * sy

    def _rescale_nodes(self):
        app = App.get_running_app()
        wm = getattr(app, "current_game", None) and app.current_game.world_manager
        if not wm: return
        for w in self._nodes_layer.children:
            if isinstance(w, NodeButton):
                w.size = self._scaled_size(w.node.get("size", [256, 256]))
                w.sync_from_node()

    def _persist_node_position(self, node: dict, pos_hint: dict):
        if not self.ALLOW_HUB_EDIT_PERSIST:
            print(f"[Hub] New pos_hint for {node.get('id')}: {pos_hint}")
            return
        app = App.get_running_app()
        wm = getattr(app, "current_game", None) and app.current_game.world_manager
        if not wm:
            print(f"[Hub] New pos_hint for {node.get('id')}: {pos_hint}")
            return
        try:
            nid = node.get("id")
            if nid and hasattr(wm, "nodes_by_id") and nid in wm.nodes_by_id:
                wm.nodes_by_id[nid]["pos_hint"] = pos_hint
        except Exception: pass

        nodes_path = None
        for attr in ("nodes_path", "nodes_file"):
            if hasattr(wm, attr):
                nodes_path = getattr(wm, attr)
                break
        if not nodes_path:
            root_dir = getattr(app, "project_root", os.getcwd())
            candidate = os.path.join(root_dir, "nodes.json")
            if os.path.exists(candidate):
                nodes_path = candidate

        if nodes_path:
            try:
                nodes_data = getattr(wm, "nodes", list(getattr(wm, "nodes_by_id", {}).values()))
                if nodes_data is not None:
                    with open(nodes_path, "w", encoding="utf-8") as f:
                        json.dump(nodes_data, f, ensure_ascii=False, indent=2)
                    print(f"[Hub] Saved node '{node.get('id')}' new pos_hint {pos_hint} to {nodes_path}")
                    return
            except Exception as e:
                print(f"[Hub] Could not save nodes.json: {e!r}")

        print(f"[Hub] Set this in nodes.json for '{node.get('id')}': \"pos_hint\": {pos_hint}")
