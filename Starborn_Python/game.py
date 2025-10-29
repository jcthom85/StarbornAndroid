# game.py  ??? Starborn (clickable nouns & pop-ups) ??? JSON-driven items & NPCs
# ---------------------------------------------------------------------------
# Requires:  kivy  ???  models.py (with NPC subclass)
#
# New in this version:
#   ??? Items and NPCs are loaded from items.json / npcs.json.
#   ??? Helper _load_list_json() makes missing files safe.
#   ??? All other behaviour is unchanged from your working build.
#
# If you also add events.json or enemies.json later, you can load them in the
# same place marked ?????? F???.
# ---------------------------------------------------------------------------

from kivy.config import Config
Config.set('graphics', 'texture_min_filter', 'nearest')
Config.set('graphics', 'texture_mag_filter', 'nearest')
Config.set('graphics', 'position', 'custom')
Config.set('graphics', 'left',   '0')
Config.set('graphics', 'top',    '0')

# Enable touch input providers and sane mouse behavior before Window is created.
# - On Windows 7+ touchscreens, wm_touch is required for true touch events.
# - Keep mouse provider with multitouch_on_demand so single-finger acts like mouse,
#   and multi-touch is available when a widget requests it.
try:
    Config.set('input', 'mouse', 'mouse,multitouch_on_demand')
    Config.set('input', 'wm_touch', 'wm_touch')  # Windows native touch
except Exception:
    # If running on a platform without these providers, ignore.
    pass

import json, os, re, uuid
from quest_manager import QuestManager
from game_objects import Room, Item, Bag
from kivy.uix.screenmanager import ScreenManager, Screen
from combat import load_enemy, BattleScreen
from milestone_manager import MilestoneManager
from ui.shop_screen import ShopScreen
from cinematics import CinematicManager
from skill_tree_manager import SkillTreeManager as STM
from crafting_manager import CraftingManager
from kivy.core.text import LabelBase
from kivy.app           import App
from kivy.lang          import Builder
from kivy.properties    import StringProperty, BooleanProperty, NumericProperty, ListProperty, DictProperty, ObjectProperty
from kivy.uix.image import Image
from kivy.uix.behaviors import ButtonBehavior
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.floatlayout import FloatLayout
from kivy.properties import NumericProperty
from kivy.uix.scrollview import ScrollView
from kivy.uix.popup     import Popup
from kivy.uix.button    import Button
from kivy.uix.label import Label
from kivy.uix.anchorlayout   import AnchorLayout
from kivy.uix.scatter import Scatter
from kivy.clock         import Clock
from kivy.metrics import dp, sp
from kivy.uix.widget import Widget
from models import NPC
from functools import partial
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.animation  import Animation
from kivy.graphics   import Color, Ellipse, Triangle, PushMatrix, PopMatrix, Scale, Translate, Rectangle, Line, RoundedRectangle, Rotate
from kivy.uix.stencilview import StencilView
from sound_manager import SoundManager
from audio_router import AudioRouter, AudioEvents
from theme_manager import ThemeManager
from font_manager import fonts
from kivy.properties import ListProperty
from ui.menu_popup import MenuPopup
from ui.menu_overlay import MenuOverlay
from ui.radial_menu import RadialMenu
from ui.narrative_popup import NarrativePopup
from ui.system_tutorial import SystemTutorialManager
from ui.quest_popup import QuestPopupManager
from ui.themed_button import ThemedButton
from map_utils import build_ascii_grid
from dialogue_box import DialogueBox
from dialogue_manager import DialogueManager
from event_manager import EventManager
from data.fishing_manager import FishingManager
from ui.fishing_screen import FishingScreen
from data.leveling_manager import LevelingManager
from ui.debug_panel import DebugPanel
from ui.bordered_frame import BorderedFrame
from kivy.uix.gridlayout import GridLayout
import datetime
from world_manager import WorldManager
from environment import EnvironmentLoader
from font_loader import register_fonts
from tutorial_manager import TutorialManager
from ui.minimap_widget import MinimapWidget
from ui.tinkering_screen import TinkeringScreen
from entities import Character, Status, GROWTH_TABLE
from ui.transition_manager import TransitionManager
from ui.hub_screen import HubScreen
from pathlib import Path
from ui.weather_layer import WeatherLayer
from save_system import SaveSystem
from ui.theme_bands import ThemeBands

# Register all fonts at startup by calling the new function
register_fonts()

DEBUG = True   # top of game.py
print = (lambda *a,**k: None) if not DEBUG else print

BEH_PASSIVE   = "passive"
BEH_ALERTABLE = "alertable"
BEH_AMBUSH    = "ambush"

# BASE_WIDTH = 360
# BASE_HEIGHT = 640

BASE_WIDTH = 1080 # was 360
BASE_HEIGHT = 2400  # was 640
FONT_SCALE = 1.0    # 100% of the size you designed for

# Global background tuning (baseline for all rooms)
# How far to raise the background image within the safe 9:16 viewport (in dp)
BG_OFFSET_Y_BASE = 0
# Extra vertical height added to the background as a fraction (e.g., 0.12 = +12%)
BG_BLEED_BASE = 0

#BG_OFFSET_Y = dp(0) 

# # force portrait mobile resolution
Window.size = (405, 900)  # Disabled for Android packaging to use native window size

# compute the uniform scale factor (so initial_scale exists for build())
win_w, win_h   = Window.size
scale_x        = win_w  / BASE_WIDTH
scale_y        = win_h  / BASE_HEIGHT
initial_scale  = min(scale_x, scale_y)

class ScalableFloatLayout(FloatLayout):
    # expose a 'scale' property so Animation can find it
    scale = NumericProperty(1)

class VignetteOverlay(Widget):
    """Full-window vignette shader overlay that fades to black at edges.
    Draws using window coordinates so it covers the entire screen regardless of
    parent size/pos. Keep this widget beneath UI widgets.
    """
    def __init__(self, **kwargs):
        from kivy.graphics import RenderContext
        super().__init__(**kwargs)
        self.canvas = RenderContext(use_parent_modelview=True, use_parent_projection=True)
        self._alpha = 0.0
        # Simple vignette shader
        self.canvas.shader.fs = (
            """
            $HEADER$
            uniform float u_alpha;   // 0..1 overall strength
            uniform float u_radius;  // where vignette starts (0..1)
            uniform float u_soft;    // falloff width (0..1)
            uniform vec2  u_res;     // window resolution
            void main(void) {
                vec2 uv = gl_FragCoord.xy / u_res; // 0..1
                // aspect-corrected distance from center
                vec2 p = uv - vec2(0.5, 0.5);
                p.x *= u_res.x / u_res.y;
                float d = length(p);
                float edge = smoothstep(u_radius, u_radius - u_soft, d);
                float a = clamp(edge * u_alpha, 0.0, 1.0);
                gl_FragColor = vec4(0.0, 0.0, 0.0, a);
            }
            """
        )
        with self.canvas:
            self._rect = Rectangle(pos=(0, 0), size=(1, 1))
        self._update_uniforms()
        from kivy.core.window import Window
        Window.bind(on_resize=lambda *_: self._update_uniforms())
        self.bind(size=lambda *_: self._update_uniforms(), pos=lambda *_: self._update_uniforms())

    def _update_uniforms(self):
        from kivy.core.window import Window
        try:
            self.canvas['u_alpha']  = float(self._alpha)
            self.canvas['u_radius'] = 0.42
            self.canvas['u_soft']   = 0.45
            self.canvas['u_res']    = (float(Window.width), float(Window.height))
            # Always cover the window
            self._rect.pos = (0, 0)
            self._rect.size = (Window.width, Window.height)
        except Exception:
            pass

    def set_alpha(self, a: float):
        self._alpha = max(0.0, min(1.0, float(a)))
        self._update_uniforms()

def _debug_vfx_room_scene_map() -> dict:
    return {
        # VFX Lab
        'vfx_stage': 'vfx_stage_demo',
        'wipe_lab': 'wipe_demo',
        'particles_bay': 'particles_demo',
        'letterbox_room': 'letterbox_demo',
        'flash_room': 'flash_demo',
        'color_filter_room': 'color_filter_demo',
        'flashback_room': 'flashback_demo',
        'speed_lines_room': 'speed_lines_demo',
        'ring_room': 'ring_demo',
        'vignette_room': 'vignette_demo',
        'shake_room': 'shake_demo',
        'fade_room': 'fade_demo',
        # Camera Gallery
        'camera_gallery': 'camera_gallery_demo',
        'tilt_suite': 'tilt_demo',
        'zoom_room': 'zoom_only_demo',
        'zoom_lounge': 'zoom_pan_demo',
        'pan_room': 'pan_only_demo',
        # Text Studio
        'caption_studio': 'caption_cycle',
        'narration_theatre': 'narration_demo',
        'cinematic_text_hall': 'cinematic_text_demo',
    }

def _clone_enemy(base_item):
    from game_objects import Item # Import your new Item class
    new = Item(base_item.name, *base_item.aliases)
    new.enemy_id = base_item.enemy_id
    new.behavior = getattr(base_item, "behavior", BEH_PASSIVE)
    new.alert_delay = getattr(base_item, "alert_delay", 3)
    new.party = getattr(base_item, "party", [base_item.enemy_id])
    if hasattr(base_item, "flavor"):
        new.flavor = base_item.flavor
    return new

LIGHT_BLUE   = "66e3ff"            # brighter cyan for actionable nouns
current_game = None                # global pointer for adventurelib commands

# ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
# Adventurelib command handlers
# ---------------------------------------------------------------------------

def look():
    current_game.update_room()
    # current_game.say("You look around.")

def go(direction):
    dir_key = (direction or "").lower().replace(" ", "_")
    # THIS IS THE FIX: No parentheses after 'exits'
    if not hasattr(current_game.current_room, dir_key) and \
       dir_key not in current_game.current_room.exits:
        # current_game.say(f"You can't go {direction}.")
        return

    # When leaving a room, cancel any alert timers for the enemies within it.
    old_room = current_game.current_room
    for enemy in old_room.enemies:
        current_game._cancel_enemy_timers(enemy)

    # And this line is also corrected to not use parentheses
    if current_game.is_direction_blocked(dir_key):
        current_game._handle_blocked_direction(dir_key)
        return

    nxt = current_game.current_room.exit(dir_key)
    if nxt:
        opposite = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}
        opp_dir = opposite.get(dir_key)
        if opp_dir and current_game._direction_block_info(nxt, opp_dir):
            current_game._handle_blocked_direction(dir_key)
            return
        # keep a simple coordinate grid for the minimap
        old_id = current_game.current_room.room_id
        ox, oy = current_game.room_positions.get(old_id, (0, 0))
        dx, dy = current_game.direction_vectors.get(dir_key, (0, 0))
        new_coords = (ox + dx, oy + dy)

        current_game.current_room = nxt

        # AUDIO: tell the router we entered a new room
        AudioEvents.emit("room.enter", {
            "hub_id": current_game.world_manager.current_hub_id,
            "room_id": nxt.room_id,
        })
        
        # When entering the new room, start the timers for any alertable enemies.
        for enemy in current_game.current_room.enemies:
            current_game.configure_enemy(enemy)
        
        current_game.event_manager.enter_room(nxt.room_id)
        # Tutorials: mark movement and enter-room
        try:
            tm = getattr(current_game, 'tutorials', None)
            if tm:
                tm.on_player_moved(old_id, nxt.room_id)
                tm.on_room_enter(nxt.room_id)
        except Exception:
            pass
        # Fallback: directly trigger known debug VFX scenes by room id
        try:
            current_game._auto_vfx_debug_if_applicable()
        except Exception:
            pass
        current_game.room_positions[nxt.room_id] = new_coords
        # Track whether this coordinate has been discovered before this move
        is_first_visit = new_coords not in current_game.discovered
        try:
            current_game._just_discovered = bool(is_first_visit)
        except Exception:
            pass
        current_game.discovered.add(new_coords)

        current_game.clear_output_log()
        # current_game.say(f"You go {direction}.\n")
        current_game.update_room()
    # else:
        # current_game.say(f"You can't go {direction}.")

def take(item):
    obj = current_game.current_room.items.find(item)
    if not obj:
        # current_game.say(f"You don???t see a {item} here.")
        return

    # move item to inventory
    current_game.inventory[obj.name] = current_game.inventory.get(obj.name, 0) + 1
    current_game.current_room.items.remove(obj)

    # ?????? build a reliable trigger word ?????????????????????????????????????????????????????????????????????????????????
    # ??? first alias if it exists   (items.json lists "wrench" first)
    # ??? otherwise the name itself, minus any leading article
    canonical = (obj.aliases[0] if obj.aliases else obj.name).lower()
    canonical = re.sub(r'^(a|an|the)\s+', '', canonical)   # drop ???a ??? / ???the ???
    current_game.event_manager.item_acquired(canonical)

    # refresh UI
    current_game.narrate(f"You take [b]{obj.name}[/b].")
    current_game.update_inventory_display()
    # Immediately refresh the room now that wrench_taken has been set
    current_game.update_room_display()

def use(item_name, target_name):
    """Attempt to use a consumable item on a party member."""
    game = current_game
    target = next((p for p in game.party if p.name.lower() == target_name.lower()), None)
    item = game.all_items.find(item_name)

    if not target:
        NarrativePopup.show(f"Could not find a party member named {target_name}.", theme_mgr=game.themes)
    elif item and game.inventory.get(item.name, 0) > 0:
        game.use_item(item, target)

def examine(item):
    # Check inventory first (new dictionary format)
    if current_game.inventory.get(item.lower(), 0) > 0:
        obj = current_game.all_items.find(item)
        if obj:
            # current_game.say(getattr(obj, 'description', f"It's just a {obj.name}."))
            return
            
    # Then check the room
    obj = current_game.current_room.items.find(item)
    if obj:
        # current_game.say(getattr(obj, 'description', f"You look closely at the {obj.name}."))
        return
        
    # current_game.say(f"You don't have or see a {item}.")

def show_inventory():
    if not current_game.inventory:
        # current_game.say("Your inventory is empty.")
        return
    items_text = "\n".join(f"- {name} (x{qty})" for name, qty in current_game.inventory.items())
    # current_game.say("You are carrying:\n" + items_text)

# ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
# KV layout string (UI)
# ---------------------------------------------------------------------------
KV = f'''
#:import ScrollEffect kivy.effects.scroll.ScrollEffect
#:import dp kivy.metrics.dp
#:import ButtonBehaviorImage ui.radial_menu.ButtonBehaviorImage
#:import MinimapWidget ui.minimap_widget.MinimapWidget
#:import ShadowLabel ui.shadow_label.ShadowLabel

<Label>:
    font_name: 'SourceCodePro-Regular'
<TextInput>:
    font_name: 'SourceCodePro-Regular'

# NEW: Define the layout and IDs for the MainWidget itself
<MainWidget>:
    # This ScalableFloatLayout is the root container for the main game UI
    ScalableFloatLayout:
        id: viewport # FIXED: The ID for the root layout now matches the Python code

        # --- Main Content Area (Room Title and Details) -----------------
        FloatLayout:
            size_hint: 1, 0.75
            pos_hint: {{'top': 1}}
            # (title panel scrim removed)

            # ROOM TITLE (top-anchored, auto height)
            ShadowLabel:
                id: title_label
                text: ""
                markup: True
                halign: 'left'
                valign: 'middle'
                font_name: app.fonts["room_title"]["name"]
                font_size: app.fonts["room_title"]["size"]
                text_size: self.width - dp(20), None
                size_hint: 0.75, None
                height: self.texture_size[1]
                padding: dp(32), dp(0)
                pos_hint: {{'x': 0, 'top': 0.96}}
                color: root.theme_fg
                on_texture: if self.texture: self.texture.mag_filter='nearest'; self.texture.min_filter='nearest'
                # Readability helpers
                use_shadow: True
                shadow_color: 0, 0, 0, 0.65
                shadow_offset: 0, -2
                use_outline: True
                outline_color: 0, 0, 0, 0.35
                outline_width: dp(1)
                debug_visualize: False

            Image:
                id: title_underline
                source: 'images/ui/underline_4.png'
                color: root.theme_fg
                allow_stretch: True
                keep_ratio: False
                size_hint: None, None
                opacity: 0
                on_texture:
                    (self.texture and setattr(self.texture, 'mag_filter', 'nearest')) or None
                    (self.texture and setattr(self.texture, 'min_filter', 'nearest')) or None

            # ROOM DESCRIPTION
            ScrollView:
                id: details_scroll
                do_scroll_x: False
                scroll_type: ['bars']              # let content receive taps; scroll via bars
                bar_width: '8dp'
                effect_cls: ScrollEffect
                size_hint: 1, None
                height: self.parent.height
                y: self.parent.height - self.height + dp(128)
                pos_hint: {{'x': 0}}

                # Single content container; it provides spacing (labels have no padding)
                BoxLayout:
                    orientation: 'vertical'
                    size_hint_y: None
                    height: self.minimum_height
                    padding: [dp(45), dp(40), dp(45), dp(40)]
                    spacing: dp(8)

                    # ONE label only: description + flavor/extras (all refs here)
                    ShadowLabel:
                        id: room_details_label
                        text: root.room_details_text
                        markup: True
                        halign: 'left'
                        align_to_text: True
                        valign: 'top'
                        font_name: app.fonts["description"]["name"]
                        font_size: app.fonts["description"]["size"]
                        size_hint_y: None
                        padding: [dp(30), 0]  # Add horizontal padding
                        text_size: self.width - self.padding[0] * 2, None
                        align_to_text: True
                        on_ref_press: root.on_ref_press(args[1])
                        # --- THIS IS THE FIX ---
                        on_height: self.parent.height = self.parent.minimum_height
                        color: root.theme_fg
                        theme_manager: root.themes
    
                        use_backdrop: True
                        backdrop_color: 0, 0, 0, 0.24
                        backdrop_pad: [dp(0), dp(30)]
                        backdrop_radius: dp(22)
                        use_backdrop_outline: True
                        backdrop_outline_width: dp(1.5)
                        use_shadow: False
                        use_outline: True
                        outline_color: 0, 0, 0, 0.25
                        outline_width: 1.5
                        outline_offset: dp(-30)-2, -2

                        debug_refs: False
                        debug_bounds: False
                        debug_verbose: False

                # --- END OF FIX ---

        #:import dp kivy.metrics.dp

        # --- Minimap Widget ---
        MinimapWidget:
            id: minimap
            size_hint: None, None
            size: dp(150), dp(150)
            pos: self.parent.width - self.width - dp(40), \
                self.parent.height - self.height - dp(40)

        # --- Output Log ---
        ScrollView:
            id: log_scroll
            do_scroll_x: False
            size_hint: 0.8, 0.20
            pos_hint: {{'center_x': 0.5, 'y': 0.12}}
            bar_width: '8dp'
            effect_cls: ScrollEffect

            Label:
                id: output_log_label
                text: root.output_log_text
                font_name: app.fonts["exploration_log"]["name"]
                font_size: app.fonts["exploration_log"]["size"]
                markup: True
                halign: 'center'
                valign: 'bottom'
                text_size: self.width - dp(10), None
                size_hint_y: None
                height: self.texture_size[1]
                padding: dp(10), dp(10)
                on_texture_size: root.scroll_output_log_to_bottom()
                on_texture: if self.texture: self.texture.mag_filter='nearest'; self.texture.min_filter='nearest'

        # --- Bottom bar: vertical stack (Return above Menu) -----------------
        BoxLayout:
            id: bottom_bar
            orientation: 'vertical'
            spacing: dp(8)
            size_hint: None, None
            height: self.minimum_height
            width: dp(192)
            pos_hint: {{'center_x': 0.5, 'y': 0.011}}

            # NEW: RETURN TO HUB BUTTON
            ButtonBehaviorImage:
                id: return_hub_button
                source: 'images/ui/return_hub_icon.png'
                size_hint: None, None
                size: dp(192), dp(192)
                opacity: 0
                disabled: True
                on_release: app.return_to_hub()

            # MENU
            ButtonBehaviorImage:
                id: menu_button
                source: 'images/ui/menu_button.png'
                size_hint: None, None
                size: dp(192), dp(192)
                on_release: root.toggle_radial_menu()

# Define the root widget for the exploration screen
<ExplorationScreen>:
    # This screen will now contain our MainWidget
    MainWidget:
        id: main_widget_instance
'''

# ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
# Helper to safely load list-style JSON
# ---------------------------------------------------------------------------
def _load_list_json(filename):
    """Return [] if the JSON file does not exist or is malformed."""
    path = os.path.join(os.path.dirname(__file__), filename)
    if not os.path.exists(path):
        return []
    try:
        with open(path, encoding="utf-8") as fp:
            return json.load(fp)
    except Exception:
        return []

def _load_dict_json(filename):
    """Return {} if the JSON file does not exist or is malformed."""
    path = os.path.join(os.path.dirname(__file__), filename)
    if not os.path.exists(path):
        return {}
    try:
        with open(path, encoding="utf-8") as fp:
            return json.load(fp)
    except Exception:
        return {}

# ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????`?????????????????????????????????????????????????????????????????????


def _select_desc(room):
    """
    Picks the most specific description for a room based on its current state.
    This version correctly references attributes on the custom Room object.
    """
    # For the kitchen, prioritize the on/off description
    if room.room_id == "kitchen":
        if getattr(room, "light_on", False):
            return getattr(room, "description_on", room.description)
        else:
            return getattr(room, "description_off", room.description)

    # For the bedroom, handle the multi-stage locker state
    if room.room_id == "bedroom":
        if getattr(room, "wrench_taken", False):
            return getattr(room, "description_empty", room.description)
        if getattr(room, "locker_open", False):
            return getattr(room, "description_open", room.description)

    # Generic: if a room defines a dark-specific description and the room is dark
    try:
        st = getattr(room, 'state', {}) or {}
        is_dark = False
        if 'dark' in st:
            is_dark = bool(st.get('dark'))
        elif 'light_on' in st:
            is_dark = not bool(st.get('light_on'))
        if hasattr(room, "description_dark") and is_dark:
            return getattr(room, "description_dark")
    except Exception:
        pass

    # For all other rooms, or if no special state matches, use the default
    return room.description

def _pixel_filter(lbl):
    """
    Force a label???s texture to use nearest???neighbour filtering
    the moment the texture exists.
    """
    def _apply(inst, tex):
        if tex:
            tex.mag_filter = 'nearest'
            tex.min_filter = 'nearest'
    _apply(lbl, lbl.texture)
    lbl.bind(on_texture=_apply)
    return lbl

def bag_from_names(self, names):
    new_bag = Bag()
    for name in names:
        item = self.find(name)
        if item:
            new_bag.add(item)
    return new_bag
Bag.bag_from_names = bag_from_names

class ExplorationScreen(Screen):
    """A simple container screen for the main game widget."""
    def on_enter(self, *args):
        """
        This Kivy event fires whenever the screen becomes active.
        We use it to resume timers when returning from a sub-screen like
        cooking, crafting, or fishing.
        """
        app = App.get_running_app()
        if hasattr(app, 'current_game') and app.current_game:
            # Only resume/update if a room is actually active
            if getattr(app.current_game, 'current_room', None):
                app.current_game.resume_enemy_timers()
                # NEW: Refresh the room display when coming back to it
                app.current_game.update_room()

    def on_leave(self, *args):
        app = App.get_running_app()
        if hasattr(app, 'current_game') and app.current_game:
            app.current_game._stash_current_node_map()

class DirectionMarker(Widget):
    """
    Edge-of-screen indicator for nearby directions or blocked paths.
    Supports three statuses:
      - unexplored : cyan arrow (default behaviour)
      - lock       : padlock icon, grey
      - enemy      : hazard diamond, red
    """

    direction = StringProperty("east")        # 'north'|'east'|'south'|'west'
    active    = BooleanProperty(False)
    status    = StringProperty("unexplored")  # 'unexplored'|'lock'|'enemy'
    pulse     = NumericProperty(0.0)          # 0..1 used for glow radius

    _STATUS_COLOURS = {
        "unexplored": (0.56, 0.78, 1.0),
        "lock":       (0.70, 0.72, 0.80),
        "enemy":      (1.00, 0.32, 0.32),
    }

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.size_hint = (None, None)
        self.size = (dp(64), dp(64))
        self._loop_anim = None
        self._burst_anim = None
        self.bind(pos=self._redraw, size=self._redraw, pulse=self._redraw, status=lambda *_: self._redraw())
        self._redraw()

    def on_active(self, *_):
        if self.active:
            self.opacity = 0.0
            self._start_pulse()
        else:
            self._stop_pulse()
            self.opacity = 0.0

    def _start_pulse(self):
        if self._loop_anim:
            self._loop_anim.cancel(self)
        colours = self._STATUS_COLOURS.get(self.status, self._STATUS_COLOURS["unexplored"])
        up_d, down_d = (0.6, 0.6) if self.status == "enemy" else (0.8, 0.8)
        self._loop_anim = Animation(opacity=0.95, pulse=1.0, d=up_d) + Animation(opacity=0.55, pulse=0.0, d=down_d)
        self._loop_anim.repeat = True
        self._loop_anim.start(self)

    def _stop_pulse(self):
        if self._loop_anim:
            self._loop_anim.cancel(self)
            self._loop_anim = None
        if self._burst_anim:
            self._burst_anim.cancel(self)
            self._burst_anim = None
        self.pulse = 0.0

    def flash_block(self):
        """Momentarily exaggerate the pulse to indicate a blocked direction."""
        if self._loop_anim:
            self._loop_anim.cancel(self)
            self._loop_anim = None
        if self._burst_anim:
            self._burst_anim.cancel(self)

        up = Animation(opacity=1.0, pulse=1.0, d=0.18)
        down = Animation(opacity=0.2, pulse=0.0, d=0.18)
        seq = up + down + up + down

        def _finish(*_):
            self._burst_anim = None
            if self.active:
                self._start_pulse()
            else:
                self.opacity = 0.0

        seq.bind(on_complete=_finish)
        self._burst_anim = seq
        seq.start(self)

    # --- drawing helpers -------------------------------------------------
    def _colour(self):
        r, g, b = self._STATUS_COLOURS.get(self.status, self._STATUS_COLOURS["unexplored"])
        return r, g, b

    def _draw_glow(self, colour):
        base = min(self.width, self.height) * 0.45
        glow_r1 = base * (1.15 + 0.25 * self.pulse)
        glow_r2 = base * (1.45 + 0.40 * self.pulse)
        r, g, b = colour
        Color(r, g, b, 0.24)
        Ellipse(pos=(self.center_x - glow_r2, self.center_y - glow_r2),
                size=(glow_r2 * 2, glow_r2 * 2))
        Color(r, g, b, 0.38)
        Ellipse(pos=(self.center_x - glow_r1, self.center_y - glow_r1),
                size=(glow_r1 * 2, glow_r1 * 2))

    def _draw_arrow(self, colour):
        cx, cy = self.center
        Color(*colour, 1.0)
        if self.direction == "east":
            tri = [
                cx - dp(10), cy - dp(12),
                cx - dp(10), cy + dp(12),
                cx + dp(14), cy
            ]
        elif self.direction == "west":
            tri = [
                cx + dp(10), cy - dp(12),
                cx + dp(10), cy + dp(12),
                cx - dp(14), cy
            ]
        elif self.direction == "north":
            tri = [
                cx - dp(12), cy - dp(10),
                cx + dp(12), cy - dp(10),
                cx,          cy + dp(14)
            ]
        else:  # south
            tri = [
                cx - dp(12), cy + dp(10),
                cx + dp(12), cy + dp(10),
                cx,          cy - dp(14)
            ]
        Triangle(points=tri)
        Color(*colour, 1.0)
        Line(points=tri + tri[:2], width=dp(1.6))

    def _draw_lock(self, colour):
        cx, cy = self.center
        body_w = dp(28)
        body_h = dp(24)
        body_pos = (cx - body_w / 2, cy - body_h / 2 - dp(4))

        # Body
        Color(colour[0], colour[1], colour[2], 0.92)
        RoundedRectangle(pos=body_pos, size=(body_w, body_h), radius=[dp(4)] * 4)
        Color(colour[0]*0.65, colour[1]*0.65, colour[2]*0.65, 1.0)
        RoundedRectangle(
            pos=(body_pos[0] + dp(2), body_pos[1] + dp(2)),
            size=(body_w - dp(4), body_h - dp(4)),
            radius=[dp(3)] * 4
        )
        Color(colour[0], colour[1], colour[2], 1.0)
        Line(rounded_rectangle=(body_pos[0], body_pos[1], body_w, body_h, dp(4)), width=dp(1.6))

        # Shackle (rotated 90° around its own center)
        shackle_r = body_w * 0.45
        shackle_cx = cx
        shackle_cy = body_pos[1] + body_h + dp(4)
        Color(colour[0], colour[1], colour[2], 1.0)
        PushMatrix()
        Rotate(angle=90, origin=(shackle_cx, shackle_cy))
        Line(circle=(shackle_cx, shackle_cy, shackle_r, 205, -25), width=dp(3))
        PopMatrix()

        # Keyhole
        Color(0, 0, 0, 0.8)
        Ellipse(pos=(cx - dp(3), cy - dp(2)), size=(dp(6), dp(8)))
        Line(circle=(cx, cy - dp(6), dp(2), 0, 360), width=dp(1.2))

    def _draw_enemy(self, colour):
        cx, cy = self.center
        r = dp(20)
        pts = [
            cx, cy + r,
            cx + r, cy,
            cx, cy - r,
            cx - r, cy
        ]
        Color(colour[0], colour[1], colour[2], 0.95)
        Triangle(points=pts[:6])
        Triangle(points=pts[4:] + pts[:2])
        Color(colour[0], colour[1], colour[2], 1.0)
        Line(points=pts + pts[:2], width=dp(2.2))
        # Cross overlay
        Color(1, 1, 1, 0.6)
        Line(points=[cx - dp(10), cy - dp(10), cx + dp(10), cy + dp(10)], width=dp(2))
        Line(points=[cx - dp(10), cy + dp(10), cx + dp(10), cy - dp(10)], width=dp(2))

    def _redraw(self, *_):
        self.canvas.clear()
        colour = self._colour()
        with self.canvas:
            self._draw_glow(colour)
            if self.status == "lock":
                self._draw_lock(colour)
            elif self.status == "enemy":
                self._draw_enemy(colour)
            else:
                self._draw_arrow(colour)

class MainWidget(FloatLayout):
    # UI-bound properties
    room_title = StringProperty("")
    room_details_text = StringProperty("")
    output_log_text   = StringProperty("")
    inv_text          = StringProperty("(empty)")
    minimap_text      = StringProperty("")
    # theme???driven colour props (bound into KV)
    theme_bg     = ListProperty([0.07, 0.07, 0.07, 1])
    theme_fg     = ListProperty([1, 1, 1, 1])
    theme_border = ListProperty([1, 1, 1, 1])
    themes = ObjectProperty(None)
    progression_data = DictProperty({}) # ADD THIS LINE
    # Runtime route access flags persisted in saves
    # { 'worlds': {world_id: bool}, 'hubs': {hub_id: bool}, 'nodes': {node_id: bool} }
    routes: dict = {}

    
    # --- POPUP DEFAULTS with font manager ---
    popup_defaults = {
        'size_hint': (.66, .66),
        'size': None,
        'auto_dismiss': False,
        'header_height': dp(40),
        'footer_height': dp(32),
        'title_font': fonts["popup_title"]["name"],
        'title_size': fonts["popup_title"]["size"],
        'btn_font': fonts["popup_button"]["name"],
        'btn_font_size': fonts["popup_button"]["size"],
        'body_font': fonts["medium_text"]["name"],
        'body_font_size': fonts["medium_text"]["size"],
        'btn_bg': lambda self: self.theme_fg,
        'btn_fg': lambda self: self.theme_bg,
        'sep_color': lambda self: self.theme_border,
    }

    def _auto_vfx_debug_if_applicable(self):
        try:
            rid = getattr(self.current_room, 'room_id', None)
            if not rid:
                return
            scene = _debug_vfx_room_scene_map().get(rid)
            if scene and hasattr(self, 'cinematics'):
                self.cinematics.play(scene)
        except Exception:
            pass

    def _update_title_underline(self, *args):
        """
        Wrap the title with a hanging indent (lines 2+ get a small indent)
        and size the underline to the widest wrapped line.
        """
        title = self.ids.get('title_label')
        underline = self.ids.get('title_underline')

        if not title or not underline:
            return

        if getattr(self, '_room_is_dark', False):
            underline.opacity = 0
            return

        has_text = bool(title.text.strip())
        if not has_text:
            underline.opacity = 0
            return

        try:
            app = App.get_running_app()
            transitioning = bool(getattr(getattr(app, 'tx_mgr', None), 'is_transitioning', False))
        except Exception:
            transitioning = False

        # NEW: Get per-room tuning options from JSON
        opts = getattr(self, '_title_options', {})
        wrap_nudge = dp(opts.get('wrap_nudge', 0))
        underline_adjust = dp(opts.get('underline_adjust', 0))

        try:
            wrap_w = float(title.texture_size[0]) if title.texture and title.texture_size[0] > 1 else float(title.width)
            # Apply global and per-room nudges
            wrap_w -= dp(57)  # Your global nudge
            wrap_w += wrap_nudge
        except Exception:
            wrap_w = float(title.width)

        indent_px = float(dp(14))

        wrapped_text, widest_px = self._wrap_title_with_hanging_indent(
            self.room_title or title.text or "",
            wrap_w,
            font_name=title.font_name,
            font_size=title.font_size,
            indent_px=indent_px,
        )

        title.text = wrapped_text
        title.texture_update()

        try:
            import math
            line_px = float(title.font_size) * float(getattr(title, 'line_height', 1.0) or 1.0)
            measured_h = float(title.texture_size[1] or 0)
            measured_lines = max(1, math.ceil(measured_h / max(1.0, line_px)))
            our_lines = max(1, wrapped_text.count('\n') + 1)

            if measured_lines > our_lines:
                for shrink_factor in (dp(2), dp(5), wrap_w * 0.05):
                    eff_w = max(1.0, wrap_w - float(shrink_factor))
                    wt, wp = self._wrap_title_with_hanging_indent(
                        self.room_title or "", eff_w,
                        font_name=title.font_name, font_size=title.font_size, indent_px=indent_px,
                    )
                    if wt.count('\n') + 1 == measured_lines:
                        widest_px = min(eff_w, wp)
                        break
        except Exception:
            pass

        # Recompute underline bounds now that wrapping is finalized
        import math
        line_px = float(title.font_size) * float(getattr(title, 'line_height', 1.0) or 1.0)
        measured_h = float(title.texture_size[1] or 0)
        measured_lines = max(1, math.ceil(measured_h / max(1.0, line_px)))

        final_underline_width = widest_px
        if measured_lines == 1:
            final_underline_width = title.texture_size[0]

        underline.width = max(0, min(final_underline_width, wrap_w)) + underline_adjust
        underline.height = max(dp(64), int(title.font_size * 0.62))
        underline.x = title.x + title.padding[0]
        underline.y = title.y + dp(16) - underline.height
        underline.opacity = 0 if transitioning else 1

        try:
            self._title_calib_runs = getattr(self, '_title_calib_runs', 0) + 1
            if self._title_calib_runs < 3:
                from kivy.clock import Clock
                Clock.schedule_once(self._update_title_underline, 0)
        except Exception:
            pass

    def _apply_dark_ui_state(self, is_dark: bool):
        """Toggle UI affordances when the current room is dark."""
        try:
            minimap = self.ids.get('minimap')
            if minimap:
                hidden = bool(is_dark)
                minimap._hidden_in_dark = hidden
                minimap.opacity = 0.0 if hidden else 1.0
                minimap.disabled = hidden

            title = self.ids.get('title_label')
            underline = self.ids.get('title_underline')
            pending_transition = bool(getattr(self, "_title_apply_event", None))
            if title:
                if hasattr(title, '_stored_text'):
                    title._stored_text = None
                if hasattr(title, '_stored_room_id'):
                    title._stored_room_id = None
                if is_dark:
                    title.opacity = 0.0
                    title.disabled = True
                    if underline:
                        underline.opacity = 0.0
                else:
                    if pending_transition:
                        title.opacity = 0.0
                        title.disabled = True
                        if underline:
                            underline.opacity = 0.0
                    else:
                        desired_title = getattr(self, 'room_title', "")
                        self._set_title_text(desired_title)

            menu_button = self.ids.get('menu_button')
            if menu_button:
                menu_button.opacity = 1.0
                menu_button.disabled = False
            self._last_dark_ui_state = bool(is_dark)
        except Exception:
            pass

    def _schedule_title_update(self, new_text: str, *, immediate: bool = False) -> None:
        self._pending_title_text = new_text or ""
        if immediate:
            self._apply_pending_title()
            return

        try:
            from kivy.app import App
            tx_mgr = getattr(App.get_running_app(), "tx_mgr", None)
        except Exception:
            tx_mgr = None

        if tx_mgr and getattr(tx_mgr, "is_transitioning", False):
            if getattr(self, "_title_apply_event", None) is None:
                self._title_apply_event = Clock.schedule_once(self._apply_pending_title_after_transition, 0)
        else:
            self._apply_pending_title()

    def _apply_pending_title_after_transition(self, *_):
        self._title_apply_event = None
        try:
            from kivy.app import App
            tx_mgr = getattr(App.get_running_app(), "tx_mgr", None)
        except Exception:
            tx_mgr = None

        if tx_mgr and getattr(tx_mgr, "is_transitioning", False):
            self._title_apply_event = Clock.schedule_once(self._apply_pending_title_after_transition, 0)
            return
        self._apply_pending_title()

    def _apply_pending_title(self):
        text = getattr(self, "_pending_title_text", "")
        self._set_title_text(text)

    def _set_title_text(self, value: str) -> None:
        title = self.ids.get("title_label")
        if not title:
            return

        final_text = value or ""
        self._last_visible_room_title = final_text

        underline = self.ids.get("title_underline")

        if getattr(self, "_room_is_dark", False):
            if title.text:
                title.text = ""
            title.opacity = 0.0
            title.disabled = True
            if underline:
                underline.opacity = 0.0
        else:
            if title.text != final_text:
                title.text = final_text
            title.opacity = 1.0
            title.disabled = False
            self._schedule_title_underline()

    def _schedule_title_underline(self):
        underline = self.ids.get("title_underline")
        if getattr(self, "_room_is_dark", False):
            if underline:
                underline.opacity = 0.0
            return

        if getattr(self, "_pending_title_underline_event", None):
            try:
                Clock.unschedule(self._pending_title_underline_event)
            except Exception:
                pass
            self._pending_title_underline_event = None

        self._pending_title_underline_event = Clock.schedule_once(self._run_title_underline_update, 0)

    def _run_title_underline_update(self, *_):
        self._pending_title_underline_event = None
        self._update_title_underline()

    def _wrap_title_with_hanging_indent(self, text: str, wrap_w: float, *, font_name: str, font_size: float, indent_px: float = 12.0) -> tuple[str, float]:
        """
        Greedy word-wrap the title to fit wrap_w, applying a hanging indent to
        lines 2+ of width indent_px. Returns (wrapped_text, widest_line_px)
        where widest_line_px includes the indent for subsequent lines.
        """
        try:
            from kivy.core.text import Label as CoreLabel
        except Exception:
            # Fallback: leave text as-is and just bound underline to wrap width
            return text, max(0.0, float(wrap_w))

        txt = (text or "").strip()
        if not txt:
            return "", 0.0

        # Measure helpers (reuse a single CoreLabel for efficiency)
        cl = CoreLabel(font_name=font_name, font_size=font_size, text="", markup=False)
        def width_of(s: str) -> float:
            if not s:
                return 0.0
            try:
                w, _ = cl.get_extents(s)
                return float(w)
            except Exception:
                return float(len(s)) * 8.0

        space_char = "\u00A0"  # measurement only
        space_w = max(1.0, width_of(space_char))
        max_w = max(1.0, float(wrap_w))
        indent = max(0.0, float(indent_px))

        words = [w for w in txt.split() if w]
        if not words:
            return txt, min(max_w, width_of(txt))

        lines: list[list[str]] = []
        widths: list[float] = []  # text width per line (excluding indent)

        current: list[str] = []
        cur_w = 0.0
        limit = max_w  # first line limit (no indent)

        def flush_line():
            nonlocal current, cur_w, limit
            if current:
                lines.append(current)
                widths.append(cur_w)
            current = []
            cur_w = 0.0
            # Subsequent lines include hanging indent in layout space; text content
            # width is limited by (wrap_w - indent)
            limit = max(1.0, max_w - indent)

        for w in words:
            ww = width_of(w)
            add_w = (space_w if current else 0.0) + ww
            if cur_w + add_w <= limit or not current:
                # fits
                current.append(w)
                cur_w += add_w
            else:
                # new line
                flush_line()
                current.append(w)
                cur_w = ww
        flush_line()

        if len(lines) == 1 and len(words) == 2:
            if width_of(words[0] + " " + words[1]) > max_w >= width_of(words[0]):
                lines = [[words[0]], [words[1]]]
                widths = [width_of(words[0]), width_of(words[1])]

        # --- CASCADING INDENT LOGIC ---
        if len(lines) <= 1:
            wrapped = " ".join(lines[0]) if lines else ""
            widest = widths[0] if widths else 0.0
        else:
            glyph = "0"
            gw = max(1.0, width_of(glyph))
            n_glyph = max(1, int(round(indent / gw)))
            indent_str = f"[color=#00000000]{glyph * n_glyph}[/color]"

            composed = [" ".join(lines[0])]
            for i, line_words in enumerate(lines[1:], start=1):
                cascading_indent = indent_str * i
                composed.append(f"{cascading_indent}{' '.join(line_words)}")
            
            wrapped = "\n".join(composed)

            widest = 0.0
            if widths:
                widest = widths[0]
            for i, wpx in enumerate(widths[1:], start=1):
                vis = wpx + (indent * i)
                widest = max(widest, vis)

        return wrapped, min(max_w, widest)

    # --- Dark overlay helpers (bands) ------------------------------------
    def _ensure_band_overlays(self, *, animate=False, duration=0.0):
        """Create semi-transparent black overlays inside ThemeBands' top/bottom
        so darkness covers the full screen. Overlays render BELOW UI children
        (title, minimap, buttons) by inserting them at index 0 in each band,
        ensuring they draw before later-added UI widgets.
        """
        try:
            app = App.get_running_app()
            bands = getattr(app, 'bands', None)
            if bands is None:
                return None

            from kivy.graphics import Color, Rectangle

            # Draw scrims in the blur widgets' canvas.after so they render above
            # the band backgrounds/blur but below later-added UI children.
            top_blur = getattr(bands, '_top_blur', None)
            if top_blur is not None and not hasattr(bands, '_bd_top_col'):
                with top_blur.canvas.after:
                    bands._bd_top_col = Color(0, 0, 0, 0)
                    bands._bd_top_rect = Rectangle(pos=top_blur.pos, size=top_blur.size)
                top_blur.bind(pos=lambda inst, p: setattr(bands._bd_top_rect, 'pos', p),
                              size=lambda inst, s: setattr(bands._bd_top_rect, 'size', s))

            bot_blur = getattr(bands, '_bot_blur', None)
            if bot_blur is not None and not hasattr(bands, '_bd_bot_col'):
                with bot_blur.canvas.after:
                    bands._bd_bot_col = Color(0, 0, 0, 0)
                    bands._bd_bot_rect = Rectangle(pos=bot_blur.pos, size=bot_blur.size)
                bot_blur.bind(pos=lambda inst, p: setattr(bands._bd_bot_rect, 'pos', p),
                              size=lambda inst, s: setattr(bands._bd_bot_rect, 'size', s))

            return bands
        except Exception:
            return None
            
    @staticmethod
    def _normalize_item_key(name: str) -> str:
        """Normalize item names for fuzzy comparisons."""
        return "".join(ch.lower() for ch in name if ch.isalnum()) if name else ""

    def _ensure_inventory_entry(self, item_obj, fallback_key: str | None = None) -> str | None:
        """
        Ensure the inventory has an entry keyed by the item's display name.
        Returns the key that should be used (or None if not present).
        """
        inventory = getattr(self, "inventory", None)
        if not inventory:
            return None

        candidates: list[str] = []
        if fallback_key:
            candidates.append(fallback_key)
        if getattr(item_obj, "name", None):
            candidates.append(item_obj.name)
        candidates.extend(getattr(item_obj, "aliases", []))

        # Exact matches first
        canonical = None
        for key in candidates:
            if key and key in inventory:
                canonical = key
                break

        if canonical is None:
            # Fuzzy match using normalized keys
            normalized_targets = {
                self._normalize_item_key(key) for key in candidates if key
            }
            if not normalized_targets and getattr(item_obj, "name", None):
                normalized_targets = {self._normalize_item_key(item_obj.name)}

            for key in inventory:
                if self._normalize_item_key(key) in normalized_targets:
                    canonical = key
                    break

        if canonical is None:
            return None

        if getattr(item_obj, "name", None) and canonical != item_obj.name:
            inventory[item_obj.name] = inventory.pop(canonical)
            canonical = item_obj.name

        try:
            item_obj._original_key = canonical
        except Exception:
            pass

        return canonical

    def _apply_item_effect(self, item, target, effect: dict):
        """Apply a single item's effect dictionary to a specific target."""
        if not target or not effect:
            return

        # Buff (single stat) -------------------------------------------------
        if "buff_stat" in effect:
            buff_info = effect["buff_stat"]
            stat = buff_info.get("stat")
            value = buff_info.get("value", 0)
            duration = buff_info.get("duration", 1)
            if stat:
                temp_id = f"temp_{getattr(item, 'id', item.name).lower()}_{stat}_{uuid.uuid4().hex[:6]}"
                temp_def = {
                    "id": temp_id,
                    "duration": duration,
                    "stack": "refresh",
                    "effects": {"flat": {stat: value}, "mult": {}, "resist": {}},
                    "flags": []
                }
                self.buff_index[temp_id] = temp_def
                target.add_buff(temp_id)
                self.narrate(f"{target.name}'s {stat.upper()} was boosted!")

        # Multiple buffs in a list -------------------------------------------
        for buff in effect.get("buffs", []):
            stat = buff.get("stat")
            value = buff.get("value", 0)
            duration = buff.get("duration", 1)
            if not stat:
                continue
            temp_id = f"temp_{getattr(item, 'id', item.name).lower()}_{stat}_{uuid.uuid4().hex[:6]}"
            temp_def = {
                "id": temp_id,
                "duration": duration,
                "stack": "refresh",
                "effects": {"flat": {stat: value}, "mult": {}, "resist": {}},
                "flags": []
            }
            self.buff_index[temp_id] = temp_def
            target.add_buff(temp_id)
            self.narrate(f"{target.name}'s {stat.upper()} increased by {value}!")

        # HP restoration (positive heal amount) ------------------------------
        hp_amount = effect.get("restore_hp", 0)
        if hp_amount > 0:
            max_hp = getattr(target, "total_max_hp", getattr(target, "max_hp", target.hp))
            old_hp = target.hp
            target.hp = min(max_hp, target.hp + hp_amount)
            healed = target.hp - old_hp
            if healed > 0:
                self.narrate(f"{target.name} recovered {healed} HP.")

        # Healing expressed as negative damage --------------------------------
        dmg = effect.get("damage")
        if dmg is not None:
            if dmg < 0:
                heal_amount = abs(dmg)
                max_hp = getattr(target, "total_max_hp", getattr(target, "max_hp", target.hp))
                old_hp = target.hp
                target.hp = min(max_hp, target.hp + heal_amount)
                healed = target.hp - old_hp
                if healed > 0:
                    self.narrate(f"{target.name} recovered {healed} HP.")
            elif dmg > 0:
                old_hp = target.hp
                target.hp = max(0, target.hp - dmg)
                dealt = old_hp - target.hp
                if dealt > 0:
                    self.narrate(f"{item.name} dealt {dealt} damage to {target.name}!")

        # RP restoration -----------------------------------------------------
        rp_amount = effect.get("restore_rp", 0)
        if rp_amount > 0:
            old_rp = getattr(target, "rp", None)
            if old_rp is not None:
                target.rp = min(getattr(target, 'max_rp', target.rp), target.rp + rp_amount)
                restored = target.rp - old_rp
                if restored > 0:
                    self.narrate(f"{target.name}'s Resonance was restored by {restored}.")

        # Status cleansing ---------------------------------------------------
        if "cure_status" in effect:
            status_to_cure = effect.get("cure_status")
            if status_to_cure == "all":
                target.status_effects.clear()
                self.narrate(f"{target.name}'s ailments were cleansed.")
            else:
                try:
                    st_enum = Status[status_to_cure.upper()]
                    if st_enum in target.status_effects:
                        del target.status_effects[st_enum]
                        self.narrate(f"{target.name} is no longer {status_to_cure}.")
                except KeyError:
                    pass

    def use_item(self, item, target=None):
        """Applies a consumable item's effect based on its JSON definition."""
        inventory_key = self._ensure_inventory_entry(item, getattr(item, '_original_key', None))
        if not inventory_key or self.inventory.get(inventory_key, 0) <= 0:
            self.narrate(f"You don't have any {item.name}s.")
            return

        effect = getattr(item, 'effect', {}) or {}
        if not effect:
            self.narrate(f"You can't use the {item.name} right now.")
            return

        # Determine targets based on the effect
        target_mode = effect.get("target", "single")
        targets = []
        if target_mode == "party":
            targets = [char for char in getattr(self, "party", []) if getattr(char, "hp", None) is not None]
        else:
            if target is None:
                self.narrate("Choose a target for that item.")
                return
            targets = [target]

        if not targets:
            self.narrate("There is no valid target for that item.")
            return

        # Consume the item
        self.inventory[inventory_key] -= 1
        if self.inventory[inventory_key] == 0:
            del self.inventory[inventory_key]

        # Effects that trigger regardless of target
        if "learn_schematic" in effect:
            schematic_id = effect["learn_schematic"]
            if hasattr(self, "learned_schematics"):
                if schematic_id not in self.learned_schematics:
                    self.learned_schematics.add(schematic_id)
                    self.narrate("New schematic learned!")
                else:
                    self.narrate("You already know this schematic.")

        # Apply the effect to each target
        for tgt in targets:
            self._apply_item_effect(item, tgt, effect)


        self.update_inventory_display()

    def begin_node(self, start_room_id: str):
        """
        Enter a node. If we've been here before, restore its minimap memory;
        otherwise initialize a fresh map anchored at the entry room.
        """
        # --- node-specific map store ---
        node_id = self.world_manager.current_node_id
        if not hasattr(self, "node_maps"):
            self.node_maps = {}

        if node_id in self.node_maps:
            # Restore previous exploration for this node
            data = self.node_maps[node_id]
            # in-memory types: tuples + set
            self.room_positions = {rid: tuple(xy) for rid, xy in data.get("room_positions", {}).items()}
            self.discovered     = set(tuple(p) for p in data.get("discovered", []))
            # safety: ensure entry room has a coordinate
            if start_room_id not in self.room_positions:
                self.room_positions[start_room_id] = (0, 0)
                self.discovered.add((0, 0))
        else:
            # First time in this node ??? create fresh mapping
            self.room_positions = {start_room_id: (0, 0)}
            self.discovered     = {(0, 0)}
            self.node_maps[node_id] = {
                "room_positions": dict(self.room_positions),
                "discovered": [list(p) for p in self.discovered],
            }

        # Enter the entry room (you can change to "last room" later if desired)
        self.current_room = self.world_manager.get_room(start_room_id)
        # AUDIO: initial music/ambience for this node entry
        AudioEvents.emit("room.enter", {
            "hub_id": self.world_manager.current_hub_id,
            "room_id": start_room_id,
        })

        self.clear_output_log()
        self.update_room()   # also refreshes the minimap
        
    def open_shop(self, shop_id: str):
        """Fancy transition into the Shop overlay, then switch the ScreenManager."""
        app = App.get_running_app()
        def _switch():
            sm = app.screen_manager
            from ui.shop_screen import ShopScreen
            scr = ShopScreen(shop_id)
            if not sm.has_screen(scr.name):
                sm.add_widget(scr)
            sm.current = scr.name
        app.tx_mgr.launch_system("shop", _switch, blur_time=1.0, fade_time=0.3)

    def open_tinkering(self):
            app = App.get_running_app()
            def _switch():
                sm = app.screen_manager
                # Corrected the import to point to the TinkeringScreen class
                from ui.tinkering_screen import TinkeringScreen 
                name = "tinkering"
                if not sm.has_screen(name):
                    # Correctly instantiate the TinkeringScreen
                    sm.add_widget(TinkeringScreen(name=name))
                sm.current = name
            app.tx_mgr.launch_system("tinkering", _switch, blur_time=1.0, fade_time=0.3)
    def open_cooking(self):
        app = App.get_running_app()
        def _switch():
            sm = app.screen_manager
            from ui.cooking_screen import CookingScreen
            name = "cooking"
            if not sm.has_screen(name):
                sm.add_widget(CookingScreen(name=name))
            sm.current = name
        app.tx_mgr.launch_system("cooking", _switch, blur_time=1.0, fade_time=0.3)

    def open_fishing(self, zone_id: str):
        app = App.get_running_app()
        def _switch():
            sm = app.screen_manager
            from ui.fishing_screen import FishingScreen
            name = "fishing"
            # fishing screens usually depend on the zone; reuse a single screen
            if not sm.has_screen(name):
                sm.add_widget(FishingScreen(zone_id, name=name))
            else:
                # update zone on the existing instance
                scr = sm.get_screen(name)
                scr.zone_id = zone_id
            sm.current = name
        app.tx_mgr.launch_system("fishing", _switch, blur_time=1.0, fade_time=0.3)
        
    def _style_popup(self, popup: Popup):
        """Apply the current theme???s colours to any Kivy Popup."""
        popup.background_color = self.theme_bg
        popup.separator_color  = self.theme_border
        popup.title_color      = self.theme_fg
        return popup

    # ?????? ONE???LINER FACTORY FOR CONSISTENT TITLED POP???UPS ???????????????????????????????????????????????????
    def _make_titled_popup(self, title, body, **overrides):
        """
        Builds a themed popup with:
        - centered title above a custom separator
        - floating ??? button at top???right
        - optional vertical centering of the body
        - styled Close footer button
        """
        # 1) merge defaults + overrides
        cfg = {**self.popup_defaults, **overrides}

        # 2) resolve any callables so cfg values are concrete
        for k, v in list(cfg.items()):
            if callable(v):
                cfg[k] = v(self)

        # 3) construct the Popup with NO native titlebar
        popup_kwargs = {
            "title":           "",
            "separator_height": 0,
            "title_size":      0,
            "size_hint":       cfg["size_hint"],
            "auto_dismiss":    cfg["auto_dismiss"],
        }
        if cfg.get("size") is not None:
            popup_kwargs["size"] = cfg["size"]
        popup = Popup(**popup_kwargs)

        # 4) header using FloatLayout so title is truly centered
        header = FloatLayout(size_hint_y=None, height=cfg["header_height"])

        title_lbl = Label(
            text=title,
            font_name=cfg["title_font"],
            font_size=cfg["title_size"],
            size_hint=(None, None),
            halign="center",
            valign="middle",
            color=self.theme_fg,
        )
        title_lbl.bind(texture_size=lambda w, ts: setattr(w, "size", ts))
        title_lbl.pos_hint = {"center_x": 0.5, "center_y": 0.6}
        header.add_widget(title_lbl)

        close_bg = cfg["btn_fg"][0:4] if isinstance(cfg["btn_fg"], (list, tuple)) else self.theme_bg
        close_fg = cfg["btn_bg"][0:4] if isinstance(cfg["btn_bg"], (list, tuple)) else self.theme_fg
        close_bg = list(close_bg) if isinstance(close_bg, (list, tuple)) else list(self.theme_bg)
        close_fg = list(close_fg) if isinstance(close_fg, (list, tuple)) else list(self.theme_fg)
        if len(close_bg) < 4:
            close_bg = close_bg + [1.0] * (4 - len(close_bg))
        if len(close_fg) < 4:
            close_fg = close_fg + [1.0] * (4 - len(close_fg))

        x_btn = ThemedButton(
            text="✕",
            size_hint=(None, None),
            size=(cfg["header_height"] * 0.92, cfg["header_height"] * 0.92),
            font_name=cfg["btn_font"],
            font_size=cfg["btn_font_size"] * 0.9,
            bg_color=[close_fg[0], close_fg[1], close_fg[2], min(0.95, close_fg[3])],
            color=[close_bg[0], close_bg[1], close_bg[2], 0.96],
            pos_hint={"right": 1, "center_y": 0.5},
        )
        x_btn.corner_radius = x_btn.height / 2.0
        x_btn.bind(height=lambda inst, h: setattr(inst, "corner_radius", max(1.0, h / 2.0)))
        x_btn.bind(on_release=popup.dismiss)
        header.add_widget(x_btn)

        # 5) custom separator line in theme color
        sep = Widget(
            size_hint=(0.85, None),          # 80% width of the popup
            height=dp(1),
            pos_hint={'center_x': 0.5},     # center it horizontally
        )
        with sep.canvas:
            Color(*cfg["sep_color"])
            rect = Rectangle(pos=sep.pos, size=sep.size)
        sep.bind(
            pos=lambda w,*a: setattr(rect, "pos", w.pos),
            size=lambda w,*a: setattr(rect, "size", w.size),
        )
        # 6) optionally vertically center the body
        content = body
        if cfg.get("center_content", False):
            wrapper = AnchorLayout(anchor_x="center", anchor_y="center")
            wrapper.add_widget(body)
            content = wrapper

        # 7) footer Close button
        footer_bg = cfg["btn_bg"]
        footer_fg = cfg["btn_fg"]
        if not isinstance(footer_bg, (list, tuple)):
            footer_bg = self.theme_fg
        if not isinstance(footer_fg, (list, tuple)):
            footer_fg = self.theme_bg
        if len(footer_bg) < 4:
            footer_bg = list(footer_bg) + [1.0] * (4 - len(footer_bg))
        if len(footer_fg) < 4:
            footer_fg = list(footer_fg) + [1.0] * (4 - len(footer_fg))

        footer = ThemedButton(
            text="Close",
            size_hint=(1, None),
            height=cfg["footer_height"],
            font_name=cfg["btn_font"],
            font_size=cfg["btn_font_size"],
            bg_color=list(footer_bg),
            color=list(footer_fg),
        )
        footer.corner_radius = footer.height / 2.0
        footer.bind(height=lambda inst, h: setattr(inst, "corner_radius", max(1.0, h / 2.0)))
        footer.bind(on_release=popup.dismiss)

        # 8) assemble root layout
        root = BoxLayout(orientation="vertical")
        root.add_widget(header)
        root.add_widget(sep)
        root.add_widget(content)
        root.add_widget(footer)

        popup.content = root
        return popup


    # misc state
    direction_vectors = {'north':(0,1),'south':(0,-1),
                         'east':(1,0),'west':(-1,0)}
    rooms, all_items, all_npcs = {}, {}, {}
    current_room = None
    inventory = {}
    room_positions = {}
    discovered = set()

    log_scroll = ObjectProperty(None)
    save_slot = 1

    # Shared Party Resources
    resonance_min = 0
    resonance_start_base = 0
    resonance = 0
    max_resonance = 100
    battle_screen = None
    credits = 500

    all_shops = {}
    
    # NEW: Input lock flag
    input_locked = False

    # -----------------------------------------------------------------------
    # Keyboard Movement (for testing)
    # -----------------------------------------------------------------------
    def _keyboard_closed(self):
        """Unbind the keyboard when it's no longer needed."""
        if hasattr(self, '_keyboard') and self._keyboard:
            self._keyboard.unbind(on_key_down=self._on_keyboard_down)
            self._keyboard = None

    def _on_keyboard_down(self, keyboard, keycode, text, modifiers):
        """Handle keyboard presses for movement."""
        if self.parent and self.parent.manager and self.parent.manager.current != 'explore':
            return False

        key = keycode[1]
        if key == 'up': self._attempt_move('north')
        elif key == 'down': self._attempt_move('south')
        elif key == 'left': self._attempt_move('west')
        elif key == 'right': self._attempt_move('east')
        return True

    # -----------------------------------------------------------------------
    # init
    # -----------------------------------------------------------------------
    def __init__(self, **kwargs):
        self._post_init_done = False
        super().__init__(**kwargs)
        self.leveling_manager = LevelingManager()
        self._skill_index: dict | None = None

        # Base visual components (created here, added later)
        self.environment_loader = EnvironmentLoader(size_hint=(1, 1))
        self.overlay_image = Image(source="", allow_stretch=True, keep_ratio=False,
                                size_hint=(1, 1), opacity=0)

        # Create weather now, but don't add yet
        self._weather = WeatherLayer(size_hint=(1, 1))  # covers the whole view

        Clock.schedule_once(self._post_init_setup)

        # --- ADD Keyboard Listener ---
        # Request the keyboard and bind our handler.
        self._keyboard = Window.request_keyboard(self._keyboard_closed, self, 'text')
        self._keyboard.bind(on_key_down=self._on_keyboard_down)

    # --- THIS IS THE FIX: The first _post_init_setup has been removed ---

    def _apply_room_weather(self):
        try:
            room = getattr(self, "current_room", None)
            if not room or not hasattr(self, "_weather"):
                return
            # Accept either attribute or state dict, fall back to '' to disable
            mode = getattr(room, "weather", None) or getattr(room, "state", {}).get("weather", "") or ""
            self._weather.set_mode(str(mode).lower())
        except Exception:
            # Fail soft; weather is purely cosmetic
            pass
        
    def _get_skill_node(self, node_id: str) -> dict | None:
        """Return the skill-tree node dict for ``node_id``."""
        if self._skill_index is None:
            self._skill_index = {}
            # We need to define BASE_DIR or use the path directly
            tree_dir = os.path.join(os.path.dirname(__file__), "skill_trees")
            if os.path.isdir(tree_dir):
                for fname in os.listdir(tree_dir):
                    if not fname.endswith(".json"):
                        continue
                    try:
                        with open(os.path.join(tree_dir, fname), encoding="utf8") as fp:
                            tree = json.load(fp)
                    except Exception:
                        continue
                    # The character-specific JSONs have a root "branches" key
                    for branch in tree.get("branches", {}).values():
                        for node in branch:
                            self._skill_index[node["id"]] = node
        return self._skill_index.get(node_id)

    def setup_game(self, load_slot: int | None = None):
        """This new method will initialize the game state AFTER the widget is built."""
        self.input_locked = False

        self._lp_ev = None
        self._touch_start = (0, 0)
        self.fullmap_lines = []

        # Initialize game systems
        self.audio = SoundManager()
        self.themes = ThemeManager()
        self.audio_router = AudioRouter(self.audio)
        self.cinematics = CinematicManager(self)
        self.milestones = MilestoneManager(self)
        self.dialogue_manager = DialogueManager(self)
        self.system_tutorials = SystemTutorialManager(self)
        self.fishing_manager = FishingManager()
        self.crafting_manager = CraftingManager(self)
        self.quest_manager = QuestManager(self)

        self.save_system = SaveSystem()
        
        self.dialogue_box = DialogueBox()
        self.add_widget(self.dialogue_box)

        self._init_hot_reload()
        self.load_item_and_npc_data()
        self.load_shop_data()
        self.load_character_definitions()
        self.load_event_data()
        # load the progression data
        self.progression_data = _load_list_json("data/progression.json")

        self.world_manager = WorldManager(self)
        self.rooms = self.world_manager.all_rooms

        self.event_manager = EventManager(self.events, self.world_manager.all_rooms, self)
        self.quest_popup_manager = QuestPopupManager(self, self.event_manager)

        if load_slot is not None and os.path.exists(f"save{load_slot}.json"):
            self._load_game_state(slot=load_slot)
            self.save_slot = int(load_slot)
        else:
            self._initialize_new_game_state()

        # Tutorials
        self.tutorials = TutorialManager(self)
        global current_game; current_game = self

        self.audio.play_music("ambient_music")
        # NOTE: We no longer call initial_display here. The App class will switch
        # to the correct starting screen (Hub or Exploration).
        # Fire room-enter events if loading into a room (e.g., from a save)
        try:
            if self.current_room:
                self.event_manager.enter_room(self.current_room.room_id)
                try:
                    if getattr(self, 'tutorials', None):
                        self.tutorials.on_room_enter(self.current_room.room_id)
                except Exception:
                    pass # Tutorials are optional
                # Fallback: directly trigger known debug VFX mapping too
                self._auto_vfx_debug_if_applicable()
        except Exception:
            pass

    def _init_hot_reload(self):
        """Check for preview requests from the editor."""
        self._preview_request_path = Path(os.path.dirname(__file__)) / "preview_request.json"
        self._last_preview_ts = 0
        Clock.schedule_interval(self._check_for_preview_request, 1.0) # check every second

    def _check_for_preview_request(self, dt):
        if not self._preview_request_path.exists():
            return
        try:
            content = self._preview_request_path.read_text(encoding="utf-8")
            if not content: return
            req = json.loads(content)
            ts = req.get("timestamp", 0)
            if ts > self._last_preview_ts:
                self._last_preview_ts = ts
                self._preview_request_path.write_text("", encoding="utf-8") # Clear file
                if req.get("action") == "play_cutscene" and "scene_id" in req:
                    self.cinematics.runner.reload()
                    self.cinematics.play(req["scene_id"])
        except Exception:
            pass # Ignore errors, clear file on next valid request

    # *** THIS IS THE FIX: Part 2 ***
    # This new method will run after __init__ is complete and the .kv rules have been applied.
    def _post_init_setup(self, dt):
        # Main content holder is the <FloatLayout id: viewport> from the KV rule
        if self._post_init_done:
            return
        self._post_init_done = True

        main_container = self.ids.viewport

        # 1) background / parallax layer ? absolute back
        main_container.add_widget(self.environment_loader, index=len(main_container.children))
        # 2) fade-overlay used by flash_transition() ? just above the bg
        main_container.add_widget(self.overlay_image, index=len(main_container.children) - 1)
        # 3) weather layer
        main_container.add_widget(self._weather, index=len(main_container.children) - 2)
        # World dark scrim: sits above background/parallax, below the rest of the UI
        try:
            from kivy.uix.widget import Widget
        except Exception:
            self._world_dark = None

        # App-level ThemeBands (may be None in debug)
        try:
            from kivy.app import App
            app = App.get_running_app()
            bands = getattr(app, 'bands', None)
        except Exception:
            bands = None

        # ---- SAFE 9:16 LAYOUT -------------------------------------------------
        # Keep the background and fade overlay pinned to the bands' safe rect
        def _layout_safe(*_):
            w = main_container.width
            h = main_container.height
            if bands is not None:
                x, y, sw, sh = bands.get_safe_rect()
            else:
                # Fallback: center a 9:16 viewport correctly (width and height computed by aspect)
                target_aspect = 9.0 / 16.0
                safe_h = w / target_aspect  # w * (16/9)
                safe_w = w
                if safe_h > h:
                    safe_h = h
                    safe_w = h * target_aspect
                sw = safe_w
                sh = safe_h
                x = (w - sw) / 2.0
                y = (h - sh) / 2.0
            try:
                self.environment_loader.size_hint = (None, None)
                self.environment_loader.size = (sw, sh)
                self.environment_loader.pos = (0, y)
            except Exception:
                pass

            try:
                self.overlay_image.size_hint = (None, None)
                self.overlay_image.size = (sw, sh)
                self.overlay_image.pos = (0, y)
            except Exception:
                pass
            # Size the world dark scrim to the safe rect so it never spills
            try:
                if getattr(self, '_world_dark', None) is not None:
                    self._world_dark.size_hint = (None, None)
                    self._world_dark.size = (sw, sh)
                    self._world_dark.pos = (0, y)
                    # keep rectangle in sync
                    self._world_dark_rect.pos = self._world_dark.pos
                    self._world_dark_rect.size = self._world_dark.size
            except Exception:
                pass

            # Apply global + per-room background nudge/bleed internally
            # Update band blur after layout as well (first frame and a couple retries)
            try:
                if bands is not None:
                    from kivy.clock import Clock
                    Clock.schedule_once(lambda *_: bands.update_blur_from(self.environment_loader), 0)
                    Clock.schedule_once(lambda *_: bands.update_blur_from(self.environment_loader), 0.12)
                    Clock.schedule_once(lambda *_: bands.update_blur_from(self.environment_loader), 0.35)
            except Exception:
                pass
            # Update band blur slices from the current background
            try:
                if bands is not None:
                    bands.update_blur_from(self.environment_loader)
            except Exception:
                pass
            try:
                # per-room (if set in update_room_display), else 0
                room_off = getattr(self, '_bg_offset_y', 0)
                room_bleed = getattr(self, '_bg_bleed', 0.0)
                # global baselines
                off = dp(BG_OFFSET_Y_BASE) + (room_off or 0)
                bleed = (room_bleed if room_bleed not in (None, 0) else BG_BLEED_BASE)
                self.environment_loader.set_bg_offset_y(off)
                self.environment_loader.set_bg_bleed(bleed)
            except Exception:
                pass

        main_container.bind(size=_layout_safe, pos=_layout_safe)
        _layout_safe()

        # ---- REHOME HUD INTO BANDS --------------------------------------------
        if bands is not None:
            pad = dp(16)
            title = self.ids.get('title_label')
            title_underline = self.ids.get('title_underline')
            minimap = self.ids.get('minimap')
            menu_btn = self.ids.get('menu_button')
            rth_btn = self.ids.get('return_hub_button')
            bottom_bar = self.ids.get('bottom_bar')

            def _safe_remove(w):
                try:
                    if w and w.parent:
                        w.parent.remove_widget(w)
                except Exception:
                    pass

            def _layout_top(*_):
                bw = bands.width
                bh = bands.top.height

                # Minimap sizing/placement (unchanged unless hidden for darkness)
                hidden_map = bool(getattr(minimap, '_hidden_in_dark', False)) if minimap else False
                if minimap:
                    mw, mh = minimap.size
                    if not hidden_map:
                        maxh = max(dp(44), bh - pad * 2)
                        scale = min(1.0, maxh / float(mh)) if mh else 1.0
                        minimap.size = (mw * scale, mh * scale)
                        mw, mh = minimap.size
                    minimap.pos = (bw - pad - mw, bands.top.y + (bh - mh) / 2)

                # ----- Title & Underline Logic -----
                if title:
                    right_pad = (minimap.width + pad * 2) if minimap and not hidden_map else pad
                    avail_w = max(0, bw - (pad + right_pad))
                    title.size_hint = (None, None)
                    # Set the widget's width. The height is now driven by the
                    # text content via _update_title_underline.
                    title.width = avail_w
                    # Vertically center the label within the band.
                    title.pos = (pad, bands.top.y + (bh - title.height) / 2)

            def _layout_bottom(*_):
                bw = bands.width
                bh = bands.bottom.height
                # two buttons centered; slight gap
                gap = dp(16)
                if menu_btn and rth_btn:
                    total = menu_btn.width + rth_btn.width + gap
                    left = (bw - total) / 2.0
                    # Swap positions: Return on the left, Menu on the right
                    rth_btn.pos = (left, bands.bottom.y + (bh - rth_btn.height) / 2)
                    menu_btn.pos = (left + rth_btn.width + gap, bands.bottom.y + (bh - menu_btn.height) / 2)
                elif menu_btn:
                    menu_btn.pos = ((bw - menu_btn.width) / 2.0, bands.bottom.y + (bh - menu_btn.height) / 2)
                elif rth_btn:
                    rth_btn.pos = ((bw - rth_btn.width) / 2.0, bands.bottom.y + (bh - rth_btn.height) / 2)

            def _layout_scroll(*_):
                sv = self.ids.get('details_scroll')
                if not sv:
                    return

                title = self.ids.get('title_label')

                if bands is not None:
                    x, y, sw, sh = bands.get_safe_rect()
                else:
                    # Fallback: full window fitted to 9:16 by width
                    sw = min(main_container.height, main_container.width * (16 / 9.0))
                    sh = sw
                    x = 0.0
                    y = (main_container.height - sh) / 2.0

                inset = dp(12)  # breathing room so text never sits on the band seam
                sv.size_hint = (None, None)
                sv.width = main_container.width

                safe_top = y + sh
                title_visible = bool(
                    title and getattr(title, 'opacity', 1.0) > 0.01 and getattr(title, 'text', '').strip()
                )
                anchor_top = title.y if title_visible else safe_top
                anchor_top = max(anchor_top, y + inset * 2)
                sv.height = max(0, anchor_top - y - inset * 2)
                sv.pos = (0, y + inset)

                # Optional: if you have a label id, add padding so the first/last lines
                # don't kiss the seam even when fonts change.
                lbl = self.ids.get('room_details_label')
                if lbl:
                    try:
                        # Properties are now set in KV for consistency
                        pass
                    except Exception:
                        pass

            if title:
                _safe_remove(title)
                title.size_hint = (None, None)
                bands.top.add_widget(title)
                # Bind to height changes to re-center vertically.
                title.bind(height=_layout_top)
            if title_underline:
                _safe_remove(title_underline)
                bands.top.add_widget(title_underline)
            if minimap:
                _safe_remove(minimap)
                minimap.size_hint = (None, None)
                bands.top.add_widget(minimap)
            for w in (menu_btn, rth_btn):
                if w:
                    _safe_remove(w)
                    w.size_hint = (None, None)
                    w.size = (dp(120), dp(96))
                    bands.bottom.add_widget(w)
            if bottom_bar:
                try:
                    bottom_bar.opacity = 0
                    bottom_bar.size_hint = (None, None)
                    bottom_bar.height = 0
                    if bottom_bar.parent:
                        bottom_bar.parent.remove_widget(bottom_bar)
                except Exception:
                    pass

            for w in (bands, bands.top, bands.bottom, main_container):
                w.bind(size=_layout_top, pos=_layout_top)
                w.bind(size=_layout_bottom, pos=_layout_bottom)
                w.bind(size=_layout_scroll, pos=_layout_scroll)

            _layout_top()
            _layout_bottom()
            _layout_scroll()
            # Ensure band dark overlays exist early so they can be driven by
            # update_room_display() and always span the full screen.
            try:
                self._ensure_band_overlays()
            except Exception:
                pass
        
    def _update_canvas_rects(self, instance, value):
        """Updates the position and size of the base background rectangle."""
        self._theme_bg_rect.pos = self.pos
        self._theme_bg_rect.size = self.size

    def _initialize_new_game_state(self):
        self.party = [self.party_defs["nova"]]
        self.inventory = {}
        
        # NEW: A new game now sets the hub, not a specific room.
        # The App class will use this to show the HubScreen.
        self.world_manager.current_world_id = "nova_prime"
        self.world_manager.current_hub_id = "mining_colony"
        self.world_manager.current_node_id = None
        self.current_room = None # No room is active when in a hub

        self.room_positions = {}
        self.discovered = set()
        # Initialize route access (spaceport routes): current world/hub available
        self.routes = {
            "worlds": {self.world_manager.current_world_id: True},
            "hubs":   {self.world_manager.current_hub_id: True},
            "nodes":  {}
        }
        
        # NEW: Initialize learned schematics for crafting/tinkering
        self.learned_schematics = set()

        # NEW: Track fired events and played cutscenes to prevent repeats
        self.fired_events: set[str] = set()
        self.played_cutscenes: set[str] = set() # This was a duplicate line, now corrected.

        # Start the introductory quest for a new game
        # if hasattr(self, "quest_manager"):
        #     self.quest_manager.start("W1-H1-MAIN-01")

        
    def load_item_and_npc_data(self):
        """Loads all item and NPC definitions into Bags."""
        self.all_items = Bag()
        
        # --- Start of Replacement Code ---
        
        for itm_def in _load_list_json("items.json"):
            name = itm_def.get("name")
            if not name:
                continue # Skip items without a name

            raw_aliases = itm_def.get("aliases", []) or []
            aliases = [str(a).lower() for a in raw_aliases]
            item_obj = Item(name, *aliases)

            # This generic loop loads ALL data from your JSON (including "subtype")
            # onto the item object, making it available to the rest of the game.
            for key, value in itm_def.items():
                if key in ['name', 'aliases']:  # We've already handled these
                    continue
                setattr(item_obj, key, value)
            # Default: allow item names to glow in dark unless explicitly disabled
            if not hasattr(item_obj, 'glow_when_dark'):
                setattr(item_obj, 'glow_when_dark', True)

            if name.lower() not in item_obj.aliases:
                item_obj.aliases.append(name.lower())

            item_id = itm_def.get("id")
            if item_id:
                alias_id = str(item_id).lower()
                if alias_id not in item_obj.aliases:
                    item_obj.aliases.append(alias_id)

            self.all_items.add(item_obj)
            
        # --- End of Replacement Code ---

        self.all_npcs = Bag()
        for npc_def in _load_list_json("npcs.json"):
            name = npc_def["name"]
            aliases  = npc_def.get("aliases", [])
            dlg = npc_def.get("dialogue", {})
            inters = npc_def.get("interactions", [])
            npc_obj = NPC(name, dlg, *aliases)
            npc_obj.interactions = inters
            # Default glow flag for NPC names
            setattr(npc_obj, 'glow_when_dark', npc_def.get('glow_when_dark', True))
            self.all_npcs.add(npc_obj)

        self.all_enemies = Bag()
        self.enemy_flavor = {}  # <<< FIX 1: Initialize the dictionary
        for en in _load_list_json("enemies.json"):
            # <<< FIX 2: Populate the dictionary with global flavor text
            flavor_text = (en.get("flavor") or "").strip()
            if flavor_text:
                self.enemy_flavor[en["id"]] = flavor_text

            obj = Item(en["name"], en["id"])
            obj.enemy_id = en["id"]
            obj.flavor = flavor_text
            setattr(obj, 'glow_when_dark', en.get('glow_when_dark', True))
            self.all_enemies.add(obj)

        self.buff_defs = _load_list_json("buffs.json")
        self.buff_index = {entry["id"]: entry for entry in (self.buff_defs or [])}

        # Load quest definitions and register them with the manager
        quest_defs = _load_list_json("quests.json")
        if hasattr(self, "quest_manager") and quest_defs:
            self.quest_manager.register_defs(quest_defs)

    def load_shop_data(self):
        """Loads all shop definitions from shops.json."""
        self.all_shops = _load_dict_json("shops.json")

    def load_character_definitions(self):
        """Loads all playable character definitions."""
        self.party_defs = {}
        stm = STM(self)
        for ch in _load_list_json("characters.json"):
            key = ch.get("id", ch["name"].lower())
            tree = stm.trees.get(key)
            declared = [node["id"] for branch in tree["branches"].values() for node in branch] if tree else []
            obj = Character(
                name=ch["name"],
                hp=ch["hp"], atk=ch.get("attack", 0), spd=ch.get("speed", 0),
                strength=ch.get("strength", 0), vitality=ch.get("vitality", 0),
                agility=ch.get("agility", 0), focus=ch.get("focus", 0), luck=ch.get("luck", 0),
                ability_points=ch.get("starting_ap", 0),
                skills=declared, unlocked_abilities=None  # unlocked_abilities will be set below
            )
            obj.id = key
            # Compute initial derived stats (e.g., update max_hp with vitality)
            obj.max_hp = ch["hp"]  # base HP already set; total_max_hp will add vitality contribution in use
            obj.hp = ch["hp"]
            initial_unlocks = [n["id"] for n in stm.available_nodes(obj) if not n.get("requires")]
            obj.unlocked_abilities = set(initial_unlocks)
            self.party_defs[key] = obj
            pass

    def load_event_data(self):
        self.events = _load_list_json("events.json")


    def load_global_dialogue(self):
        """
        Call this from __init__ to populate self.dialogue_data.
        """
        import json, os
        path = os.path.join(os.path.dirname(__file__), "dialogue.json")
        try:
            with open(path, "r", encoding="utf-8") as f:
                raw_list = json.load(f)
                return {entry["id"]: entry for entry in raw_list}
        except Exception as e:
            from kivy.uix.popup import Popup
            from kivy.uix.label import Label
            p = Popup(title="Dialogue Load Error",
                      content=Label(text=f"Failed to load dialogue.json:\n{e}"),
                      size_hint=(None, None), size=(400, 200),
                      auto_dismiss=True)
            p.open()
            return {}

    def play_dialogue_by_id(self, dlg_id):
        """
        Look up a dialogue entry in self.dialogue_data and show it.
        Once the player taps to close, we fire "trigger" and queue "next".
        """
        if not dlg_id:
            return

        entry = self.dialogue_data.get(dlg_id)
        if not entry:
            print(f"[Warning] dialogue ID '{dlg_id}' not found.")
            return

        # --- (1) Check condition, if any ---
        cond = entry.get("condition", "").strip()
        if cond:
            typ, _, val = cond.partition(":")
            if typ == "quest":
                q = self.quest_manager.quest_index.get(val)
                if not q or q.get("status") not in ("active", "complete"):
                    return
            elif typ == "milestone":
                if val not in self.milestones.completed:
                    return
            elif typ == "item":
                if self.inventory.get(val, 0) == 0:
                    return
            # ???extend with more condition types if needed???

        # --- (2) Prepare speaker & text ---
        speaker_id = entry.get("speaker", "").lower()
        text_block  = entry.get("text", "")

        # --- (3) Build the on_dismiss callback ---
        def _after_dismiss():
            # (3a) Fire any trigger side???effects
            trig = entry.get("trigger", "").strip()
            if trig:
                # A dialog trigger might be "give_item:wrench" or "start_quest:find_wrench"
                # We also allow comma???separated triggers, e.g. "give_item:wrench,complete_quest:return_wrench"
                for part in trig.split(","):
                    t_typ, _, t_val = part.partition(":")
                    t_typ = t_typ.strip()
                    t_val = t_val.strip()
                    if t_typ == "give_item":
                        self.inventory[t_val] = self.inventory.get(t_val, 0) + 1
                        self.narrate(f"You receive [b]{t_val}[/b].")
                        self._action_give_item_to_player({"item": t_val})
                    elif t_typ == "start_quest":
                        self.quest_manager.start_quest(t_val)
                    elif t_typ == "complete_quest":
                        self.quest_manager.complete_quest(t_val)
                    elif t_typ == "set_milestone":
                        if t_val in self.milestones.milestones:
                            self.milestones.completed.add(t_val)
                            # --- ADD THIS NEW CASE FOR LEARNING SKILLS FROM MILESTONES ---
                    elif t_typ == "learn_skill":
                        # Assumes format "learn_skill:character_id:skill_id"
                        char_id, _, skill_id = t_val.partition(":")
                        if character := self.party_defs.get(char_id):
                            if skill_id not in character.unlocked_abilities:
                                character.unlocked_abilities.add(skill_id)
                                skill_node = self._get_skill_node(skill_id)
                                skill_name = skill_node['name'] if skill_node else skill_id
                                self.narrate(f"[color=00ff00][b]{character.name} learned a unique skill: {skill_name}![/b][/color]")
                    # ???add extra trigger types here???

            # (3b) Queue the next line if there is one
            nxt = entry.get("next", "").strip()
            if nxt:
                Clock.schedule_once(lambda dt: self.play_dialogue_by_id(nxt), 0.2)

        # --- (4) Show the dialogue box ---
        self.dialogue_box.show_dialogue(speaker_id, text_block, on_dismiss=_after_dismiss)

    def on_touch_down(self, touch):
        # We must always record the starting touch position to avoid errors.
        self._touch_start = touch.pos

        # To ensure UI elements like the DialogueBox are always clickable, we 
        # must always pass the touch event down to child widgets. The `input_locked`
        # flag will be checked later in `on_touch_up` to prevent world movement.
        return super().on_touch_down(touch)

    def on_touch_up(self, touch):
        # First, check if input is locked (e.g., during a cinematic).
        # If so, we consume the touch event here to prevent any swipe-to-move
        # actions. This is the correct place to block world interaction.
        if self.input_locked:
            return True
            
        # Cancel any pending long-press for the minimap.
        if getattr(self, '_lp_ev', None):
            from kivy.clock import Clock
            Clock.unschedule(self._lp_ev)
            self._lp_ev = None

        # Swipe detection for room movement.
        sx, sy = self._touch_start
        ex, ey = touch.pos
        dx, dy = ex - sx, ey - sy
        threshold = dp(40)

        # Horizontal swipe? Using the original, correct (inverted) logic.
        if abs(dx) > abs(dy) and abs(dx) > threshold:
            # Swipe Left (dx < 0) is 'east'
            # Swipe Right (dx > 0) is 'west'
            self._attempt_move('east' if dx < 0 else 'west')
            return True

        # Vertical swipe? Using the original, correct (inverted) logic.
        if abs(dy) > abs(dx) and abs(dy) > threshold:
            # Swipe Down (dy < 0) is 'north'
            # Swipe Up (dy > 0) is 'south'
            self._attempt_move('north' if dy < 0 else 'south')
            return True

        return super().on_touch_up(touch)

    # -------------------------------------------------------------------
    #  Enemy temperament / ambush scheduling
    # -------------------------------------------------------------------
    def configure_enemy(self, enemy_item, *, behavior=None, alert_delay=None):
        """Initialize enemy state and schedule alert timers."""
        enemy_item.behavior    = behavior    or getattr(enemy_item, "behavior", BEH_PASSIVE)
        enemy_item.alert_delay = alert_delay or getattr(enemy_item, "alert_delay", 3)
        enemy_item.state       = BEH_PASSIVE
        enemy_item._timers     = []

        if enemy_item.behavior == BEH_ALERTABLE:
            ev = Clock.schedule_once(
                lambda dt, en=enemy_item: self._enemy_go_alert(en),
                enemy_item.alert_delay
            )
            enemy_item._timers.append(ev)
            if not hasattr(enemy_item, "_timer_fire_times"):
                enemy_item._timer_fire_times = {}
            enemy_item._timer_fire_times[ev] = Clock.get_time() + enemy_item.alert_delay

    def _attempt_move(self, direction):
        # only move if that exit exists
        dir_key = (direction or "").lower()
        if self.is_direction_blocked(dir_key):
            self._handle_blocked_direction(dir_key)
            return

        if self.current_room and dir_key in self.current_room.exits:
            # Gate westward movement from Nova's House until the light has been
            # turned on at least once (persisted via light_hint_done). Once set,
            # movement is never restricted again even if lights are turned off.
            try:
                if (getattr(self.current_room, 'room_id', '') == 'town_9'
                    and dir_key == 'west'
                    and not bool(getattr(self.current_room, 'state', {}).get('light_hint_done'))):
                    # Provide subtle feedback; don't advance rooms
                    try:
                        self.cinematics.screen_shake(magnitude=8, duration=0.25)
                    except Exception:
                        pass
                    return
            except Exception:
                pass
            # Slow down the swipe by increasing the transition duration
            App.get_running_app().tx_mgr.slide_transition(direction, lambda: go(dir_key), duration=0.50)
        else:
            self.cinematics.screen_shake(magnitude=10, duration=0.4)

    # -----------------------------------------------------------------------
    # Data loading (rooms + items + NPCs)
    # -----------------------------------------------------------------------
    def load_game_data(self):
        base = os.path.dirname(__file__)

        # ??? A ??? ROOMS
        with open(os.path.join(base, "rooms.json"), encoding="utf-8") as fp:
            room_defs = json.load(fp)

        # ??? A.5 - SHOPS
        with open(os.path.join(base, "shops.json"), encoding="utf-8") as fp:
            self.all_shops = json.load(fp)

        # ??? B ??? ITEMS
        self.all_items = Bag() # CORRECTED
        for itm_def in _load_list_json("items.json"):
            name = itm_def["name"]
            aliases = itm_def.get("aliases", [])
            item_obj = Item(name, *aliases)
            
            if name.lower() not in item_obj.aliases:
                item_obj.aliases.append(name.lower())
            
            item_obj.description = itm_def.get("description", f"A {name}.")
            
            item_obj.type = itm_def.get("type")
            if item_obj.type == "equippable":
                item_obj.equipment = itm_def.get("equipment", {})
                item_obj.mod_slots = itm_def.get("mod_slots", [])
            elif item_obj.type == "consumable":
                item_obj.effect = itm_def.get("effect", {})
            item_obj.buy_price = itm_def.get("buy_price")
            item_obj.sell_price = itm_def.get("sell_price")

            self.all_items.add(item_obj)

        # ??? C ??? NPCS
        self.all_npcs = Bag() # CORRECTED
        for npc_def in _load_list_json("npcs.json"):
            name     = npc_def["name"]
            aliases  = npc_def.get("aliases", [])
            dlg      = npc_def.get("dialogue", {})
            inters   = npc_def.get("interactions", [])
            npc_obj  = NPC(name, dlg, *aliases)
            npc_obj.interactions = inters
            self.all_npcs.add(npc_obj)

        # Build fast???lookup dict for talk()
        self.npcs = {}
        for npc in self.all_npcs:
            self.npcs[npc.name.lower()] = npc
            for alias in getattr(npc, "aliases", []):
                self.npcs[alias.lower()] = npc

        # ??? C2 ??? ENEMIES
        self.all_enemies  = Bag() # CORRECTED
        self.enemy_flavor = {}

        for en in _load_list_json("enemies.json"):
            flavor_text = (en.get("flavor") or "").strip()
            if flavor_text:
                self.enemy_flavor[en["id"]] = flavor_text
            obj = Item(en["name"], en["id"])
            obj.enemy_id    = en["id"]
            obj.behavior    = en.get("behavior", BEH_PASSIVE)
            obj.alert_delay = en.get("alert_delay", 3)
            obj.flavor      = flavor_text
            self.all_enemies.add(obj)

        # ??? D ??? Build rooms, attach items & NPCs
        self.rooms = {}
        for rd in room_defs:
            rid  = rd["id"]
            room = Room(rd.get("description", ""))
            room.env = rd.get("env", "default")
            room.room_id = rid
            room.title = rd.get("title", rid.replace("_", " ").title())
            room.blocked_directions = rd.get("blocked_directions", {})

            for k, v in rd.get("state", {}).items():
                setattr(room, k, v)
            room.state = rd.get("state", {})
            room.desc_raw_on    = rd.get("description_on")
            room.desc_raw_off   = rd.get("description_off")
            room.desc_raw       = rd.get("description")
            room.desc_raw_open  = rd.get("description_open")
            room.desc_raw_empty = rd.get("description_empty")

            for key, text in rd.items():
                if key.startswith("description_"):
                    setattr(room, key, text)

            # attach items
            room.items = Bag() # CORRECTED
            for it in rd.get("items", []):
                obj = self.all_items.find(it)
                if obj:
                    room.items.add(obj)
                # The logic to handle items not in items.json has been removed for clarity,
                # as the old file did not have it and it was causing issues.

            # attach NPCs
            room.npcs = Bag() # CORRECTED
            for np in rd.get("npcs", []):
                obj = self.all_npcs.find(np)
                if obj:
                    room.npcs.add(obj)

            # attach ENEMIES
            room.enemies = Bag() # CORRECTED
            for enemy_data in rd.get("enemies", []):
                # Support both old format (string) and new format (dict)
                if isinstance(enemy_data, str):
                    en_id = enemy_data
                    instance_overrides = {}
                elif isinstance(enemy_data, dict):
                    en_id = enemy_data.get("id")
                    # If no explicit id, treat the first party member as the lead enemy
                    if not en_id:
                        party_list = enemy_data.get("party", [])
                        if party_list:
                            en_id = party_list[0]
                    instance_overrides = enemy_data
                else:
                    continue
                # Attach per-room flavor maps (if any), used when rendering clickable rows
                room.item_flavor = rd.get("item_flavor", {})
                room.enemy_flavor_map = rd.get("enemy_flavor", {})

                if not en_id:
                    continue

                base = self.all_enemies.find(en_id)
                if base:
                    inst = _clone_enemy(base)
                    
                    # Get the template's behavior, then override with instance-specific value if it exists.
                    template_behavior = getattr(base, "behavior", BEH_PASSIVE)
                    template_delay = getattr(base, "alert_delay", 3)

                    inst.behavior = instance_overrides.get("behavior", template_behavior)
                    inst.alert_delay = instance_overrides.get("alert_delay", template_delay)
                    inst.party = instance_overrides.get("party", [en_id])

                    room.enemies.add(inst)
            room.actions = rd.get("actions", [])
            # Attach per-room flavor maps (if any), used when rendering clickable rows
            room.item_flavor = rd.get("item_flavor", {})
            room.enemy_flavor_map = rd.get("enemy_flavor", {})
            self.rooms[rid] = room

        # ??? D2 ??? EVENTS (player_action only for now)
        self.events = _load_list_json("events.json")

        # ??? E ??? Connect exits
        for rd in room_defs:
            r = self.rooms[rd["id"]]
            for d, dst in rd.get("connections", {}).items():
                if dst in self.rooms:
                    setattr(r, d.lower().replace(" ", "_"), self.rooms[dst])

    def _stash_current_node_map(self):
        """Save the current node's minimap state into the per-node cache."""
        node_id = self.world_manager.current_node_id
        if not node_id:
            return
        # Keep in-memory forms; convert for JSON in save_progress
        self.node_maps[node_id] = {
            "room_positions": dict(self.room_positions),
            "discovered": [list(p) for p in self.discovered],
        }

    def _build_save_payload(self) -> dict:
        """Builds the exact save dict your editor expects (unchanged layout)."""
        # Helper to get item name or None
        def get_item_name(item):
            return item.name if item else None

        # Characters
        characters_data = {}
        for cid, ch in self.party_defs.items():
            characters_data[cid] = {
                "ability_points": ch.ability_points,
                "unlocked_abilities": sorted(list(ch.unlocked_abilities)),
                "level": ch.level,
                "xp": ch.xp,
                "hp": ch.hp,
                "equipment": {
                    "weapon": get_item_name(ch.equipment.get("weapon")),
                    "armor": get_item_name(ch.equipment.get("armor")),
                    "accessory": get_item_name(ch.equipment.get("accessory")),
                }
            }

        # Current location
        current_room_id = self.current_room.room_id if self.current_room else None

        # Game state
        game_state = {
            "party": [p.id for p in self.party],
            "inventory": self.inventory,
            "credits": self.credits,
            "resonance": self.resonance,
            "resonance_min": self.resonance_min,
            "resonance_start_base": self.resonance_start_base,
            "resonance_max": self.max_resonance,
            "map": {
                "current_world_id": self.world_manager.current_world_id,
                "current_hub_id": self.world_manager.current_hub_id,
                "current_node_id": self.world_manager.current_node_id,
                "current_room_id": current_room_id,

                # legacy live view
                "room_positions": {rid: list(xy) for rid, xy in self.room_positions.items()},
                "discovered": [list(pos) for pos in self.discovered],

                # all nodes' maps
                "node_maps": {
                    nid: {
                        "room_positions": {rid: list(xy) for rid, xy in data.get("room_positions", {}).items()},
                        "discovered": [list(p) for p in data.get("discovered", [])],
                    }
                    for nid, data in (getattr(self, "node_maps", {}) or {}).items()
                }
            },
            "room_states": {rid: r.state for rid, r in self.rooms.items() if r.state},
            "quests": self.quest_manager.to_dict(),
            "routes": self.routes or {"worlds": {}, "hubs": {}, "nodes": {}},
            "learned_schematics": list(self.learned_schematics or set()),
            "fired_events": list(self.fired_events or set()),
            "played_cutscenes": list(self.played_cutscenes or set())
        }

        return {
            "characters": characters_data,
            "game_state": game_state,
            # Persist tutorials alongside core state
            "tutorials": self.tutorials.to_dict() if hasattr(self, 'tutorials') else {}
        }

    def _autosave(self, reason: str):
        """Small wrapper you can call anywhere to write autosave.json (throttled)."""
        if hasattr(self, "save_system"):
            self.save_system.maybe_autosave(self._build_save_payload(), context=reason)

    # -----------------------------------------------------------------------
    # Persistence helpers
    # -----------------------------------------------------------------------
    def save_progress(self, slot: int = 1):
        """Manual save to the numbered slot (keeps your file format & location)."""
        # Ensure node map is captured before serializing
        self._stash_current_node_map()
        full_save_data = self._build_save_payload()
        try:
            path = self.save_system.save(slot, full_save_data)
            self.narrate(f"[i]Game saved to slot {slot}.[/i]")
        except Exception as e:
            self.narrate(f"[color=ff0000]Error saving game: {e}[/color]")


    def _apply_loaded_state(self, data: dict, slot: int):
        """Apply a deserialized save dict to runtime state (pure, no file I/O)."""
        # --- Characters / equipment ---
        char_data = data.get("characters", {})
        for cid, info in char_data.items():
            if ch := self.party_defs.get(cid):
                ch.ability_points = info.get("ability_points", ch.ability_points)
                ch.unlocked_abilities = set(info.get("unlocked_abilities", []))
                ch.level = info.get("level", 1)
                ch.xp = info.get("xp", 0)
                ch.hp = info.get("hp", ch.max_hp)

                if "equipment" in info:
                    for equipment_slot, item_name in info["equipment"].items():
                        if item_name:
                            item_obj = self.all_items.find(item_name)
                            ch.equipment[equipment_slot] = item_obj if item_obj else None
                        else:
                            ch.equipment[equipment_slot] = None

        # --- Game state ---
        game_state = data.get("game_state", {})
        party_ids = game_state.get("party", ["nova"])
        self.party = [self.party_defs[pid] for pid in party_ids if pid in self.party_defs]

        self.inventory = game_state.get("inventory", {})
        self.credits = game_state.get("credits", 500)
        self.resonance_min = game_state.get("resonance_min", 0)
        self.resonance_start_base = game_state.get("resonance_start_base", getattr(self, "resonance_start_base", 0))
        self.max_resonance = game_state.get("resonance_max", 100)
        self.resonance = game_state.get("resonance", self.get_resonance_start())
        self.resonance = max(self.resonance_min, min(self.max_resonance, self.resonance))
        # Route access flags (optional in old saves)
        self.routes = game_state.get("routes", {}) or {}
        self.routes.setdefault("worlds", {})
        self.routes.setdefault("hubs", {})
        self.routes.setdefault("nodes", {})

        # Learned schematics
        self.learned_schematics = set(game_state.get("learned_schematics", []))

        # Fired events and cutscenes
        self.fired_events = set(game_state.get("fired_events", []))
        self.played_cutscenes = set(game_state.get("played_cutscenes", []))

        # Ensure equipped items exist in inventory (same behavior as before)
        for character in self.party:
            for item in character.equipment.values():
                if item and self.inventory.get(item.name, 0) == 0:
                    self.inventory[item.name] = 1

        mp = game_state.get("map", {})
        self.world_manager.current_world_id = mp.get("current_world_id", "nova_prime")
        self.world_manager.current_hub_id   = mp.get("current_hub_id", "mining_colony")
        self.world_manager.current_node_id  = mp.get("current_node_id")

        current_room_id = mp.get("current_room_id")
        self.current_room = self.world_manager.get_room(current_room_id) if current_room_id else None

        # Rebuild per-node maps
        self.node_maps = {}
        raw_node_maps = mp.get("node_maps", {})
        for nid, nd in raw_node_maps.items():
            rp = {rid: tuple(xy) for rid, xy in nd.get("room_positions", {}).items()}
            disc = set(tuple(p) for p in nd.get("discovered", []))
            self.node_maps[nid] = {"room_positions": rp, "discovered": [list(p) for p in disc]}

        # Legacy fields (back-compat)
        raw_positions = mp.get("room_positions", {current_room_id: [0, 0]} if current_room_id else {})
        legacy_rp = {rid: tuple(coords) for rid, coords in raw_positions.items()}
        legacy_disc = set(tuple(pos) for pos in mp.get("discovered", [[0, 0]]))

        cur_node = self.world_manager.current_node_id
        if cur_node and cur_node in self.node_maps:
            self.room_positions = {rid: tuple(xy) for rid, xy in self.node_maps[cur_node]["room_positions"].items()}
            self.discovered     = set(tuple(p) for p in self.node_maps[cur_node]["discovered"])
        else:
            self.room_positions = legacy_rp
            self.discovered     = legacy_disc

        # Room states
        rooms_state_data = game_state.get("room_states", {})
        breaker_state_touched = False

        def _boolish(v):
            if isinstance(v, bool):
                return v
            if isinstance(v, (int, float)):
                return v != 0
            if isinstance(v, str):
                s = v.strip().lower()
                if s in ("1", "true", "on", "yes"):
                    return True
                if s in ("0", "false", "off", "no"):
                    return False
            return bool(v)

        for room_id, states in rooms_state_data.items():
            room = self.rooms.get(room_id)
            if not room:
                continue
            normalized = {}
            for key, value in states.items():
                coerced = value
                if key in {"light_on", "dark", "breaker_on", "power_on"}:
                    coerced = _boolish(value)
                normalized[key] = coerced
                try:
                    setattr(room, key, coerced)
                except Exception:
                    pass
            if not hasattr(room, "state") or getattr(room, "state", None) is None:
                try:
                    room.state = {}
                except Exception:
                    continue
            try:
                room.state.update(normalized)
            except Exception:
                pass
            if "light_on" in normalized:
                try:
                    self.event_manager._sync_dark_light_flags(room, "light_on", normalized["light_on"])
                except Exception:
                    pass
            elif "dark" in normalized:
                try:
                    self.event_manager._sync_dark_light_flags(room, "dark", normalized["dark"])
                except Exception:
                    pass
            if room_id in {"mines_7", "mines_8", "mines_9", "mines_10"} and ("breaker_on" in normalized or "light_on" in normalized or "dark" in normalized):
                breaker_state_touched = True

        if breaker_state_touched:
            try:
                self.event_manager._update_switchback_gate()
            except Exception:
                pass

        # Quests: update statuses on the already-loaded quest objects
        saved_quests_data = game_state.get("quests", {})
        if saved_quests_data and hasattr(self.quest_manager, 'load_dict'):
            self.quest_manager.load_dict(saved_quests_data)

        # Refresh the minimap if applicable
        from kivy.clock import Clock
        Clock.schedule_once(lambda dt:
            (self.ids.minimap.refresh(self.room_positions, self.current_room.room_id)
            if self.current_room and hasattr(self.ids, "minimap") else None), 0)

        # AUDIO: apply correct music/ambience post-load
        hub_id = self.world_manager.current_hub_id
        AudioEvents.emit("room.enter", {
            "hub_id": hub_id,
            "room_id": self.current_room.room_id if self.current_room else None,
        })

        # Tutorials: apply persisted state
        try:
            if hasattr(self, 'tutorials') and 'tutorials' in data:
                self.tutorials.load_dict(data['tutorials'])
        except Exception as e:
            print(f"Error loading tutorial state: {e}")
            pass

    # -----------------------------------------------------------------------
    # Route access helpers (used by EventManager and Hub UI)
    # -----------------------------------------------------------------------
    def is_world_discovered(self, world_id: str) -> bool:
        return bool(self.routes.get("worlds", {}).get(world_id, True))

    def is_hub_discovered(self, hub_id: str) -> bool:
        default = True
        try:
            default = bool(self.world_manager.hubs.get(hub_id, {}).get("discovered", True))
        except Exception:
            default = True
        return bool(self.routes.get("hubs", {}).get(hub_id, default))

    def is_node_discovered(self, node_id: str) -> bool:
        default = True
        try:
            default = bool(self.world_manager.nodes.get(node_id, {}).get("discovered", True))
        except Exception:
            default = True
        return bool(self.routes.get("nodes", {}).get(node_id, default))

    def unlock_world(self, world_id: str):
        self.routes.setdefault("worlds", {})[world_id] = True
        self._autosave(f"unlock_world:{world_id}")

    def unlock_hub(self, hub_id: str):
        self.routes.setdefault("hubs", {})[hub_id] = True
        try:
            app = App.get_running_app()
            if getattr(app, "screen_manager", None) and app.screen_manager.has_screen("hub"):
                scr = app.screen_manager.get_screen("hub")
                if getattr(scr, "hub_id", None) == hub_id:
                    scr._build()
        except Exception:
            pass
        self._autosave(f"unlock_hub:{hub_id}")

    def unlock_node(self, node_id: str):
        self.routes.setdefault("nodes", {})[node_id] = True
        try:
            node = self.world_manager.nodes.get(node_id)
            app = App.get_running_app()
            if node and getattr(app, "screen_manager", None) and app.screen_manager.has_screen("hub"):
                scr = app.screen_manager.get_screen("hub")
                if getattr(scr, "hub_id", None) == node.get("hub_id"):
                    scr._build()
        except Exception:
            pass
        self._autosave(f"unlock_node:{node_id}")

    def _load_game_state(self, slot: int):
        """Load JSON via SaveSystem, then apply it."""
        data = self.save_system.load(slot)
        if not data:
            print(f"[WARN] Failed to load save slot {slot}. Starting new game.")
            self.narrate(f"[color=ff0000]Failed to load save file. Starting new game.[/color]")
            self._initialize_new_game_state()
            return
        self._apply_loaded_state(data, slot)

    # -----------------------------------------------------------------------
    # UI helpers
    # -----------------------------------------------------------------------
    def say(self, msg):
        """Append a message to the output log."""
        self.output_log_text += msg.strip() + "\n"

    def clear_output_log(self):
        self.output_log_text = ""

    def scroll_output_log_to_bottom(self):
        if self.ids.log_scroll:
            self.ids.log_scroll.scroll_y = 0

    # -------------------------------------------------------------------
    #  called by Combat after the player wins
    # -------------------------------------------------------------------
    def _on_enemy_victory(self, eid):
        """
        Cleanly remove *all* instances of the defeated enemy from the room,
        cancel any timers, notify the event system, then refresh the UI.
        """
        # Support passing a single ID or a list of IDs
        ids = [eid] if isinstance(eid, str) else list(eid)
        room = self.current_room

        to_remove = []
        for en in list(room.enemies):
            if en.enemy_id in ids:
                self._cancel_enemy_timers(en)
                to_remove.append(en)

        for en in to_remove:
            room.enemies.remove(en)

        for _id in ids:
            self.event_manager.enemy_defeated(_id)

        # 4) rebuild exploration panel (flavour text will be re-evaluated)
        self.update_room()
        # AUTOSAVE: battle win checkpoint
        self._autosave(f"victory:{','.join(ids)}")


    def flash_transition(self, move_cb):
        # 1) draw the flash quad
        with self.canvas.after:
            self._flash_color = Color(1,1,1,0)
            self._flash_rect  = Rectangle(pos=self.pos, size=self.size)

        # 2) build two animations: up???flash (0???.4) then fade???out (.4???0)
        anim_up   = Animation(a=0.8, duration=0.2)
        anim_down = Animation(a=0,   duration=0.2)

        # 3) when the ???up??? part completes, do the room change
        anim_up.bind(on_complete=lambda *_: move_cb())

        # 4) chain them and clean up after the full sequence
        seq = anim_up + anim_down
        seq.bind(on_complete=lambda *_: (
            self.canvas.after.remove(self._flash_color),
            self.canvas.after.remove(self._flash_rect)
        ))

        # 5) start the whole show  ??? NEW
        seq.start(self._flash_color)

    def _update_theme_bg(self, instance, _):
        self._theme_bg_rect.pos  = self.pos
        self._theme_bg_rect.size = self.size

    def toggle_radial_menu(self):
        """Opens the main menu overlay to the last-used tab."""
        app = App.get_running_app()
        if not app:
            return

        # If an overlay is already open, this will close it. Otherwise, it opens a new one.
        if getattr(app, "_menu_overlay", None):
            app._menu_overlay.dismiss()
        else:
            # Use the last tab, or default to 'inventory'
            last_tab = getattr(app, "_last_menu_tab", "inventory")
            app.open_menu(last_tab)

    # ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    # Pop???up helpers
    # ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    def show_action_popup(self, title, actions,
                          *, size_hint=(0.55, 0.6), auto_dismiss=True):
        # Determine if the room is dark to select the correct theme.
        is_dark = False
        try:
            room = getattr(self, 'current_room', None)
            st = getattr(room, 'state', {}) if room else {}
            if 'dark' in st:
                is_dark = bool(st.get('dark'))
            elif 'light_on' in st:
                is_dark = not bool(st.get('light_on'))
        except Exception:
            pass

        # Select the theme manager based on darkness
        if is_dark:
            # Create a temporary, separate theme manager for this popup
            # so we don't affect the main game's theme state.
            active_theme_mgr = ThemeManager()
            active_theme_mgr.use("dark_room")
        else:
            active_theme_mgr = self.themes

        # Define a style dictionary for the buttons inside the popup
        button_style = {
            'background_normal': 'images/ui/button_rounded_bg.png',
            'background_down': 'images/ui/button_rounded_bg_down.png',
            'background_color': active_theme_mgr.col('fg'),  # This tints the background_normal image.
            'color': active_theme_mgr.col('bg'),  # This sets the text color.
            'border': (dp(16), dp(16), dp(16), dp(16)), # For proper 9-patch scaling
        }

        # 1) Build the MenuPopup using the chosen ThemeManager
        popup = MenuPopup(
            title,
            actions=actions,
            size_hint=size_hint,
            autoclose=auto_dismiss,
            theme_mgr=active_theme_mgr,
            button_style=button_style
        )

        # 2) If dark, also hide the border for better readability.
        if is_dark and hasattr(popup, 'set_border_visible'):
            popup.set_border_visible(False)

        # 3) Style the native Kivy Popup chrome (background, separator, title color, etc.)
        self._style_popup(popup)
        # 4) Tutorials: if this is the Light Switch action, snooze the hint while open
        try:
            if str(title).strip().lower() == 'light switch' and getattr(self, 'tutorials', None):
                self.tutorials.pause_light_switch_hint()
                # Resume (if still applicable) shortly after dismiss
                popup.bind(on_dismiss=lambda *_: self.tutorials.resume_light_switch_hint_if_needed(
                    getattr(self.current_room, 'room_id', ''), delay=5.0))
        except Exception:
            pass
        # 5) Finally open it
        popup.open()

    def narrate(self, text: str, *, title: str | None = None, actions=None, autoclose=True, tap_to_dismiss=True):
        """Show a lightweight titleless narrative popup.
        Falls back to log text if construction fails.
        """
        try:
            from ui.narrative_popup import NarrativePopup
            popup = NarrativePopup.show(text, title=title, actions=actions, theme_mgr=self.themes,
                                        autoclose=autoclose, tap_to_dismiss=tap_to_dismiss)
            return popup
        except Exception as e:
            print(f"Narrate popup failed: {e}")
        return None

    def _close_overlay_cb(self, callback):
        if callback:
            callback()
        self._close_overlay()

    def _close_overlay(self):
        if hasattr(self, "_overlay_popup"):
            self.remove_widget(self._overlay_popup)
            del self._overlay_popup

    # ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    # Icon???bar handlers (all now share NPC fonts & button colours)
    # ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    # 1. INVENTORY
    def show_inventory(self):
        scroll = ScrollView(do_scroll_x=False, do_scroll_y=True)

        lbl = Label(
            text=self.inv_text,
            font_name=self.popup_defaults["body_font"],
            font_size=self.popup_defaults["body_font_size"],
            halign="left", valign="top",
            text_size=(dp(260), None),
            size_hint_y=None,
            color=self.theme_fg,
        )
        lbl.bind(texture_size=lambda inst, sz: setattr(inst, "height", sz[1]))
        scroll.add_widget(lbl)

        self._make_titled_popup(
            "Inventory",
            scroll,
            size_hint=(0.66, 0.66),
            auto_dismiss=True,
        ).open()


    # 2. JOURNAL
    def show_journal(self):
        """Displays both active and completed quests in labeled sections."""
        body = BoxLayout(orientation="vertical", spacing=dp(8), padding=(0, dp(8)))

        active_quests, completed_quests = self.quest_manager.get_journal_entries()

        # --- Active Quests Section ---
        body.add_widget(Label(
            text="[b]Active Quests[/b]", markup=True,
            font_name=self.popup_defaults["body_font"], font_size=self.popup_defaults["title_size"],
            size_hint_y=None, height=dp(30), color=self.theme_fg
        ))

        if not active_quests:
            body.add_widget(Label(
                text="  [i]No active quests.[/i]", markup=True,
                font_name=self.popup_defaults["body_font"], font_size=self.popup_defaults["body_font_size"],
                size_hint_y=None, height=dp(24), color=(.8,.8,.8,1)
            ))
        else:
            for q in active_quests:
                mark = "[ ]"
                title_text = f"{mark}  {q['title']}"
                current_stage_idx = q.get("current_stage_idx", 0)
                objective_text = ""
                if "stages" in q and len(q["stages"]) > current_stage_idx:
                    objective = q["stages"][current_stage_idx].get("objective")
                    if objective:
                        objective_text = f"\n    [i]- {objective}[/i]"

                body.add_widget(Label(
                    text=title_text + objective_text, markup=True,
                    font_name=self.popup_defaults["body_font"], font_size=self.popup_defaults["body_font_size"],
                    size_hint_y=None, height=dp(48) if objective_text else dp(24),
                    color=self.theme_fg
                ))

        # --- Spacer ---
        body.add_widget(Widget(size_hint_y=None, height=dp(20)))

        # --- Completed Quests Section ---
        body.add_widget(Label(
            text="[b]Completed Quests[/b]", markup=True,
            font_name=self.popup_defaults["body_font"], font_size=self.popup_defaults["title_size"],
            size_hint_y=None, height=dp(30), color=self.theme_fg
        ))

        if not completed_quests:
            body.add_widget(Label(
                text="  [i]No completed quests yet.[/i]", markup=True,
                font_name=self.popup_defaults["body_font"], font_size=self.popup_defaults["body_font_size"],
                size_hint_y=None, height=dp(24), color=(.8,.8,.8,1)
            ))
        else:
            for q in completed_quests:
                # Use a green checkmark for completed quests
                mark = "[color=00ff00]???[/color]"
                title_text = f"  {mark}  {q['title']}"
                body.add_widget(Label(
                    text=title_text, markup=True,
                    # Make completed quests slightly greyed out
                    color=(0.7, 0.7, 0.7, 1),
                    font_name=self.popup_defaults["body_font"], font_size=self.popup_defaults["body_font_size"],
                    size_hint_y=None, height=dp(24)
                ))


        self._make_titled_popup(
            "Journal", body,
            size_hint=(0.8, 0.66),
            auto_dismiss=False,
        ).open()

    # 3. PARTY STATS
    def show_stats(self):
        lines = [f"{c.name}: HP {c.hp}/{c.total_max_hp}, ATK {c.total_atk}, SPD {int(c.total_spd)}"
                for c in self.party] or ["(no party members)"]

        lbl = Label(
            text="\n".join(lines),
            font_name=self.popup_defaults["body_font"],
            font_size=self.popup_defaults["body_font_size"],
            halign="left", valign="top",
            text_size=(dp(260), None),
            color=self.theme_fg,
        )

        self._make_titled_popup("Party", lbl, size_hint=(0.66, 0.66)).open()

    # 4. SETTINGS
    def show_settings(self):
        lbl = Label(
            text="Settings coming soon???",
            font_name=self.popup_defaults["body_font"],
            font_size=self.popup_defaults["body_font_size"],
            halign="center", valign="top",
            color=self.theme_fg,
        )

        self._make_titled_popup("Settings", lbl, size_hint=(0.66, 0.66)).open()

    # 4b. SAVE GAME
    def prompt_save_slot(self):
        actions = []
        for i in range(1, 4):
            label = f"Slot {i}"
            if os.path.exists(f"save{i}.json"):
                label += " (overwrite)"
            actions.append((label, lambda _=None, s=i: self._save_to_slot(s)))
        self.show_action_popup("Save Game", actions)

    def _save_to_slot(self, slot: int):
        self.save_slot = slot
        self.save_progress(slot)

    # -------- text formatting helpers --------
    def colourize(self, text, ref_name):
        """Clickable styling for nouns: cyan + bold + underline + chevrons.

        Chevrons make links pop even on busy backgrounds while remaining
        readable with the existing outline. UTF-8 guillemets are used here.
        """
        return (
            f"[ref={ref_name}]"
            f"[u][color={LIGHT_BLUE}][b]?{text}?[/b][/color][/u]"
            f"[/ref]"
        )

    def _hex_from_tuple(self, col):
        try:
            r, g, b = float(col[0]), float(col[1]), float(col[2])
        except Exception:
            r, g, b = 1.0, 1.0, 1.0
        return f"{int(max(0,min(255,r*255))):02x}{int(max(0,min(255,g*255))):02x}{int(max(0,min(255,b*255))):02x}"

    def accent_colourize(self, text, ref_name, *, glow: bool | None = None):
        """Clickable styling for nouns using theme accent.

        If the current room is dark and glow=True (or None defaults to True),
        use a brighter accent to simulate a glow emphasis. This works in tandem
        with our environment-only dark overlay so the text sits above darkness.
        """
        try:
            accent = self.themes.col('accent') if hasattr(self, 'themes') else (0.4, 0.8, 1.0, 1)
            # Always use the theme's base accent color without the "glow" effect.
            hexcol = self._hex_from_tuple(accent)
        except Exception:
            hexcol = LIGHT_BLUE
        return f"[ref={ref_name}][color={hexcol}][b]{text}[/b][/color][/ref]"

    def inject_clickables(self, desc: str, room):
        """
        Insert [ref] markup for NPCs, items, enemies, and actions into *desc*.
        Single-pass per entity (name + aliases together), and never nests [ref] tags.
        """
        import re

        # ----- room darkness (for your accent/glow helper) -----------------------
        is_dark = False
        try:
            st = getattr(room, 'state', {}) or {}
            if 'dark' in st:
                is_dark = bool(st.get('dark'))
            elif 'light_on' in st:
                is_dark = not bool(st.get('light_on'))
        except Exception:
            pass
        self._room_is_dark = is_dark

        # ----- helpers ------------------------------------------------------------
        def make_alt_pattern(words):
            uniq, seen = [], set()
            for w in (words or []):
                if not w:
                    continue
                k = w.casefold()
                if k not in seen:
                    seen.add(k)
                    uniq.append(w)
            if not uniq:
                return None
            # Longest-first so "Stellar Battery" wins over "Battery".
            uniq.sort(key=len, reverse=True)
            alt = "|".join(re.escape(w) for w in uniq)
            # Word boundaries; case-insensitive, but keep original case via callback.
            return re.compile(rf"(?i)\b(?:{alt})\b")

        def safe_sub(pattern, ref, text, glow=None):
            """
            Replace all matches with a [ref] wrapper, unless the match sits inside
            an existing [ref]...[/ref]. (Crude but effective balance check.)
            """
            if not pattern:
                return text

            def repl(m):
                start = m.start()
                pre = text[:start]
                # If we're inside a previously-inserted [ref], skip.
                if pre.count("[ref=") > pre.count("[/ref]"):
                    return m.group(0)
                return self.accent_colourize(m.group(0), ref, glow=glow)

            return pattern.sub(repl, text)

        # ----- NPCs ---------------------------------------------------------------
        for npc in getattr(room, "npcs", []):
            ref = f"npc_{str(getattr(npc, 'name', '')).lower().replace(' ', '_')}"
            pat = make_alt_pattern([getattr(npc, "name", "")])
            # RESTORED GLOW LOGIC
            glow = getattr(npc, "glow_when_dark", True) if is_dark else False
            desc = safe_sub(pat, ref, desc, glow=glow)

        # ----- Items --------------------------------------------------------------
        for itm in getattr(room, "items", []):
            variants = [getattr(itm, "name", "")] + list(getattr(itm, "aliases", []) or [])
            ref = f"pickup_{str(getattr(itm, 'name', '')).lower().replace(' ', '_')}"
            pat = make_alt_pattern(variants)
            # RESTORED GLOW LOGIC
            glow = getattr(itm, "glow_when_dark", True) if is_dark else False
            desc = safe_sub(pat, ref, desc, glow=glow)

        # ----- Enemies ------------------------------------------------------------
        for en in getattr(room, "enemies", []):
            variants = [getattr(en, "name", "")] + list(getattr(en, "aliases", []) or [])
            ref = f"attack_{getattr(en, 'enemy_id', str(getattr(en, 'name','')).lower().replace(' ','_'))}"
            pat = make_alt_pattern(variants)
            # RESTORED GLOW LOGIC
            glow = getattr(en, "glow_when_dark", True) if is_dark else False
            desc = safe_sub(pat, ref, desc, glow=glow)

        # ----- Data-driven actions ------------------------------------------------
        for act in getattr(room, "actions", []):
            name = act.get("name", "")
            if not name:
                continue
            ref = "action_" + re.sub(r"\s+", "_", name.lower())
            pat = make_alt_pattern([name])

            # RESTORED GLOW LOGIC
            glow = bool(act.get("glow_when_dark", True)) if is_dark else False

            # Special case for the tutorial light switch
            if name.lower() == 'light switch' and getattr(room, 'room_id', '') == 'town_9' and is_dark:
                try:
                    already_done = bool(room.state.get('light_hint_done'))
                except Exception:
                    already_done = False
                if not already_done:
                    # Use the special light blue color for the tutorial hint
                    desc = pat.sub(f"[ref={ref}][color={LIGHT_BLUE}][b]{name}[/b][/color][/ref]", desc, 1)
                    continue # Skip the default styling for this one case

            # Default styling for all other actions
            desc = safe_sub(pat, ref, desc, glow=glow)

        return desc

    def format_list(self, label, bag, clickable_prefix=""):
        if not bag:
            return ""
        out = f"[b]{label}:[/b]\n"
        for itm in bag:
            if clickable_prefix:
                ref = f"{clickable_prefix}_{itm.name.lower().replace(' ','_')}"
                out += f"- [ref={ref}][b]{itm.name}[/b][/ref]\n"
            else:
                out += f"- {itm.name}\n"
        return out

    def format_exits(self):
        exs = self.current_room.exits
        if not exs:
            return "[b]Exits:[/b] None\n"
        out = "[b]Exits:[/b]\n"
        for d in exs:
            ref = f"go_{d.lower().replace(' ','_')}"
            out += f"- {self.accent_colourize(d.capitalize(), ref)}\n"
        return out


    # -------- room / inventory display --------
    def update_room_display(self):
        room = self.current_room
        if not room:
            return

        hub_id = self.world_manager.current_hub_id
        hub = self.world_manager.hubs.get(hub_id, {})

        env = room.env or hub.get("theme", "default")
        self.themes.use(env)

        self.theme_bg = self.themes.col("bg")
        self.theme_fg = self.themes.col("fg")
        self.theme_border = self.themes.col("border")

        # --- NEW: Set weather based on room data ---
        try:
            # Read the 'weather' key from the room's data, default to 'none'.
            weather_mode = getattr(room, 'weather', 'none')
            app = App.get_running_app()
            if hasattr(app, 'weather_layer') and app.weather_layer is not None:
                app.weather_layer.set_mode(weather_mode)
        except Exception as e:
            print(f"Error setting weather: {e}")

        # --- BACKGROUND / DARKNESS LOGIC ---
        try:
            st = getattr(room, 'state', {}) or {}
            if 'dark' in st:
                is_dark = bool(st.get('dark'))
            elif 'light_on' in st:
                is_dark = not bool(st.get('light_on'))
            else:
                is_dark = False

            try:
                self._room_is_dark = bool(is_dark)
            except Exception:
                self._room_is_dark = False

            dark_alpha = 0.82
            from kivy.app import App as _App
            _app = _App.get_running_app()
            cur_a = getattr(getattr(_app, '_dark_col', None), 'a', 0.0) or 0.0

            if not hasattr(self, '_dark_state_initialized'):
                self._dark_state_initialized = False

            should_snap = bool(is_dark and (not self._dark_state_initialized or cur_a <= 0.01))

            # MODIFIED: Do not clear global dark if the app has forced it black
            if _app and hasattr(_app, 'set_global_dark') and not getattr(_app, '_force_black_screen', False):
                _app.set_global_dark(0.0, animate=False)

            bands = self._ensure_band_overlays()
            self.environment_loader.set_dark_mode(
                is_dark, desaturation=0.85, overlay_alpha=dark_alpha,
                animate=not should_snap, duration=0.25,
            )
            try:
                _app.set_edge_masks(0.0, animate=False)
            except Exception:
                pass
            try:
                if getattr(self, '_world_dark_col', None) is not None:
                    self._world_dark_col.a = 0.0
            except Exception:
                pass
            try:
                if bands and hasattr(bands, '_bd_top_col'):
                    bands._bd_top_col.a = 0.0
                    bands._bd_bot_col.a = 0.0
            except Exception:
                pass
            try:
                lbl = self.ids.get('room_details_label')
                if lbl:
                    lbl.use_backdrop_outline = not bool(is_dark)
            except Exception:
                pass
            self._dark_state_initialized = True
        except Exception:
            pass

        room_data = room.__dict__
        if 'layers' not in room_data:
            bg_path = getattr(room, 'background_image', None) or hub.get('background_image') or self.themes.get_asset("background_image")
            room_data['layers'] = [{'source': bg_path, 'depth': 0}]
        self.environment_loader.load_environment(room_data)
        try:
            self.environment_loader.on_dark_progress()
        except Exception:
            pass
        try:
            from kivy.app import App
            app = App.get_running_app()
            bands = getattr(app, 'bands', None)
            if bands is not None:
                first_img = next((getattr(lay, 'image', None) for lay in getattr(self.environment_loader, 'layers', []) if getattr(lay, 'image', None)), None)
                if first_img:
                    if getattr(first_img, 'texture', None):
                        bands.update_blur_from(self.environment_loader)
                    else:
                        first_img.bind(texture=lambda *_: bands.update_blur_from(self.environment_loader))
        except Exception:
            pass
        try:
            self._bg_offset_y = dp(getattr(room, "bg_offset_y", 0))
            self._bg_bleed = float(getattr(room, "bg_bleed", 0.0))
            self.environment_loader.set_bg_bleed(self._bg_bleed if self._bg_bleed not in (None, 0) else BG_BLEED_BASE)
            self.environment_loader.set_bg_offset_y(dp(BG_OFFSET_Y_BASE) + (self._bg_offset_y or 0))
        except Exception:
            pass

        from kivy.resources import resource_find
        overlay_path = resource_find(getattr(room, 'overlay_image', "")) or ""
        self.overlay_image.source = overlay_path
        self.overlay_image.reload()

        self.room_title = getattr(room, "title", room.room_id.capitalize())
        self._title_options = getattr(room, 'title_options', {})

        if "title_label" in self.ids:
            self._schedule_title_update(self.room_title)

        self._apply_dark_ui_state(bool(getattr(self, "_room_is_dark", False)))

        # --- SIMPLIFIED MAIN TEXT LOGIC ---

        # 1) Get the base description and prune sentences about NPCs not present.
        raw_desc = _select_desc(room)
        for npc_item in self.all_npcs:
            try:
                if room.npcs.find(npc_item.name) is None:
                    raw_desc = re.sub(
                        rf'[^.]*\b{re.escape(npc_item.name)}\b[^.]*\.',
                        '', raw_desc, flags=re.IGNORECASE
                    )
            except Exception:
                pass
        
        # 2) Gather flavor texts only if the room is NOT dark.
        flavor_texts = []
        if not self._room_is_dark:
            item_flavor_map = getattr(room, 'item_flavor', {})
            for item in getattr(room, 'items', []):
                # Prefer room-specific flavor, fall back to item's own description.
                flavor = item_flavor_map.get(str(getattr(item, 'name', '')))
                if not flavor:
                    flavor = getattr(item, 'description', None)
                if flavor:
                    flavor_texts.append(str(flavor).strip())

            enemy_flavor_map = getattr(room, 'enemy_flavor_map', None) or getattr(room, 'enemy_flavor', None) or {}
            for enemy in getattr(room, 'enemies', []):
                flavor = None
                if enemy_flavor_map:
                    candidate_keys = []
                    enemy_id = getattr(enemy, 'enemy_id', None)
                    enemy_name = getattr(enemy, 'name', None)
                    if enemy_id:
                        candidate_keys.extend([enemy_id, enemy_id.lower(), enemy_id.replace('_', ' ').lower(), enemy_id.replace('_', ' ').title()])
                    if enemy_name:
                        candidate_keys.extend([enemy_name, enemy_name.lower(), enemy_name.strip().lower(), enemy_name.strip().title()])
                    for key in candidate_keys:
                        if key in enemy_flavor_map:
                            flavor = enemy_flavor_map[key]
                            break
                if not flavor:
                    flavor = getattr(enemy, 'flavor', None)
                if not flavor:
                    flavor = self.enemy_flavor.get(getattr(enemy, 'enemy_id', None))
                flavor = (flavor or '').strip()
                if flavor:
                    flavor_texts.append(flavor)
        full_text_block = raw_desc.strip()
        if flavor_texts:
            # --- THIS IS THE FIX ---
            # Get the hex color for the border from the current theme
            border_hex = self._hex_from_tuple(self.theme_border)
            # This pattern uses common symbols to avoid font rendering issues.
            divider = f"\n\n[color={border_hex}][/color]\n\n"
            full_text_block += divider + "\n".join(flavor_texts)
            # --- END OF FIX ---

        # 4) Inject clickables ONCE over the entire combined block.
        final_markup = self.inject_clickables(full_text_block, room)
        self.room_details_text = final_markup

        # 5) Force reflow so widget height matches texture (prevents cropping)
        def _reflow(lbl):
            if not lbl:
                # --- NEW: Apply weather after text reflow ---
                self._apply_room_weather()
                return
            lbl.text_size = (lbl.width, None)
            lbl.texture_update()
            lbl.size_hint_y = None
            pad_y = getattr(lbl, 'padding_y', 0)
            lbl.height = (lbl.texture_size[1] or 0) + pad_y * 2

        rd = self.ids.get('room_details_label')
        _reflow(rd)
        Clock.schedule_once(lambda *_: _reflow(rd), 0)

        # --- NEW: Apply weather after display updates ---
        self._apply_room_weather()

    def update_inventory_display(self):
        self.inv_text = "\n".join(f"- {name} (x{qty})" for name, qty in self.inventory.items()) if self.inventory else "(empty)"

    def update_minimap(self):
        """
        Push the latest map state into the MinimapWidget.

        ???   Send *all* visited rooms ( self.room_positions )  
        ???   Always pass the full lookup so the widget can translate a
            room-id ??? (x, y) without guessing.
        """
        minimap = self.ids.get("minimap")
        if not minimap or not self.current_room:
            return

        # The minimap now uses the full room data to draw connections correctly.
        # We pass the `self.rooms` dictionary, which contains all room definitions.
        # The minimap widget will internally use `self.room_positions` to map IDs to coordinates.
        minimap.refresh( # This was a bug, it should be room_positions
            self.rooms,                    # all room definitions
            self.current_room.room_id,     # current room id
        )

    def ensure_direction_markers(self):
        """
        Lazily build a full-screen overlay layer and four edge-anchored markers.
        Call this before updating markers. Safe to call repeatedly.
        """
        if hasattr(self, "_marker_layer"):
            return

        # Fullscreen layer above your content
        self._marker_layer = FloatLayout(size_hint=(1, 1))
        # Make sure it draws on top
        self.add_widget(self._marker_layer)

        # Helper to make an anchor slot
        def _slot(anchor_x, anchor_y):
            a = AnchorLayout(anchor_x=anchor_x, anchor_y=anchor_y, size_hint=(1, 1), padding=dp(16))
            self._marker_layer.add_widget(a)
            return a

        self._marker_slots = {
            "north": _slot("center", "top"),
            "east":  _slot("right",  "center"),
            "south": _slot("center", "bottom"),
            "west":  _slot("left",   "center"),
        }

        # Direction markers (start hidden)
        self._dir_markers = {}
        for d in ("north", "east", "south", "west"):
            m = DirectionMarker(direction=d, opacity=0.0)
            # Non-interactive hint; if you want click-to-move later:
            # m.bind(on_touch_up=lambda inst, touch, dd=d: self._maybe_go_via_marker(dd, touch))
            self._marker_slots[d].add_widget(m)
            self._dir_markers[d] = m
        self._active_direction_blocks = {}


    def _unexplored_adjacent_directions(self):
        """
        Return a set of directions where an adjacent room EXISTS but is UNEXPLORED.
        Existence: via current_room.connections[dir]
        Unexplored: dest_id not present in self.room_positions for this node.
        """
        if not self.current_room:
            return set()

        # Prefer raw 'connections' (existence), not filtered exits();
        # connections come from the room JSON and were copied to the Room object.
        # WorldManager wires exits separately for runtime travel. 
        # We'll treat keys case-insensitively just in case.
        conns = getattr(self.current_room, "connections", {}) or {}
        conns = {str(k).lower(): v for k, v in conns.items()}

        unexplored = set()
        for d in ("north", "east", "south", "west"):
            dest_id = conns.get(d)
            if not dest_id:
                continue
            # If we've never assigned a coordinate to that room in this node, it's unexplored to the player.
            if dest_id not in self.room_positions:
                unexplored.add(d)
        return unexplored

    def _direction_block_info(self, room, direction: str):
        if not room:
            return None
        direction = (direction or "").lower()
        info = getattr(room, "blocked_directions", {}) or {}
        entry = info.get(direction)
        if not entry:
            return None

        typ = str(entry.get("type", "")).lower()

        if typ == "enemy":
            enemy_id = entry.get("enemy_id")
            def _room_has_enemy(r):
                return any(getattr(en, "enemy_id", None) == enemy_id for en in getattr(r, "enemies", []))

            if enemy_id:
                if _room_has_enemy(room):
                    return ("enemy", entry)
                dest_room = None
                try:
                    dest_room = room.exits.get(direction)
                except Exception:
                    dest_room = getattr(room, direction, None)
                if dest_room and _room_has_enemy(dest_room):
                    return ("enemy", entry)
            else:
                if getattr(room, "enemies", []):
                    return ("enemy", entry)
                dest_room = None
                try:
                    dest_room = room.exits.get(direction)
                except Exception:
                    dest_room = getattr(room, direction, None)
                if dest_room and getattr(dest_room, "enemies", []):
                    return ("enemy", entry)
            return None

        if typ == "lock":
            if entry.get("unlocked"):
                return None
            if self._are_lock_requirements_met(entry):
                return None
            return ("lock", entry)

        return None

    def _are_lock_requirements_met(self, entry: dict) -> bool:
        if entry.get("unlocked"):
            return True

        key_id = entry.get("key_id") or entry.get("key")
        if key_id:
            return False

        requires = entry.get("requires") or []
        if not requires:
            return False

        for req in requires:
            if isinstance(req, str):
                # simple milestone check by id shorthand
                milestones = getattr(self, "milestones", None)
                completed = getattr(milestones, "completed", set()) if milestones else set()
                if req not in completed:
                    return False
                continue

            if not isinstance(req, dict):
                return False

            if "milestone" in req:
                mid = req.get("milestone")
                milestones = getattr(self, "milestones", None)
                completed = getattr(milestones, "completed", set()) if milestones else set()
                if mid not in completed:
                    return False
                continue

            room_id = req.get("room_id") or req.get("room")
            if room_id:
                target = self.rooms.get(room_id)
                if not target:
                    return False
                key = req.get("state_key") or req.get("state")
                if not key:
                    return False
                expected = req.get("value", True)
                cur = getattr(target, key, None)
                if cur is None:
                    cur = (target.state or {}).get(key) if hasattr(target, "state") else None
                if bool(cur) != bool(expected):
                    return False
                continue

            return False

        return True

    def _clear_direction_block(self, room, direction: str, entry: dict | None = None):
        if not room:
            return
        direction = (direction or "").lower()
        block_map = getattr(room, "blocked_directions", None)
        if not block_map:
            return
        if entry is None:
            entry = block_map.get(direction)
        if direction in block_map:
            block_map.pop(direction, None)

        opposite = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}
        dest = None
        try:
            dest = room.exits.get(direction)
        except Exception:
            dest = getattr(room, direction, None)
        if not dest:
            return

        opp_dir = opposite.get(direction)
        if not opp_dir:
            return
        opp_map = getattr(dest, "blocked_directions", None)
        if not opp_map or opp_dir not in opp_map:
            return

        opp_entry = opp_map.get(opp_dir)
        if not opp_entry:
            return

        if entry is None:
            opp_map.pop(opp_dir, None)
            return

        if entry.get("type") != opp_entry.get("type"):
            return

        if entry.get("type") == "enemy":
            if opp_entry.get("enemy_id") == entry.get("enemy_id"):
                opp_map.pop(opp_dir, None)
        elif entry.get("type") == "lock":
            key_a = entry.get("key_id") or entry.get("key")
            key_b = opp_entry.get("key_id") or opp_entry.get("key")
            if key_a and key_b and key_a == key_b:
                opp_map.pop(opp_dir, None)
            elif not key_a and not key_b:
                opp_map.pop(opp_dir, None)

    def _find_inventory_key_for_item(self, item_obj, key_id: str):
        candidates = []
        if item_obj:
            candidates.append(getattr(item_obj, "name", ""))
            for alias in getattr(item_obj, "aliases", []) or []:
                if alias:
                    candidates.append(str(alias))
        if key_id:
            candidates.append(str(key_id))
        for cand in candidates:
            if not cand:
                continue
            if self.inventory.get(cand, 0) > 0:
                return cand
        return None

    def _try_unlock_lock(self, direction: str, entry: dict) -> bool:
        if not entry:
            return False
        key_id = entry.get("key_id") or entry.get("key")
        if key_id:
            return self._try_unlock_lock_with_key(direction, entry, key_id)

        locked_msg = entry.get("message_locked")
        if locked_msg:
            self.narrate(locked_msg)
        return False

    def _try_unlock_lock_with_key(self, direction: str, entry: dict, key_id: str) -> bool:
        item_obj = None
        try:
            item_obj = self.all_items.find(key_id)
        except Exception:
            item_obj = None

        item_name = getattr(item_obj, "name", None) or key_id.replace("_", " ").title()
        inv_key = self._find_inventory_key_for_item(item_obj, key_id)
        if not inv_key:
            locked_msg = entry.get("message_locked") or f"The {direction.title()} gate requires a {item_name}."
            self.narrate(locked_msg)
            return False

        consume = bool(entry.get("consume", True))

        def _use_key():
            if consume:
                self.inventory[inv_key] = self.inventory.get(inv_key, 0) - 1
                if self.inventory.get(inv_key, 0) <= 0:
                    self.inventory.pop(inv_key, None)
                self.update_inventory_display()

            entry["unlocked"] = True
            self._clear_direction_block(self.current_room, direction, entry=entry)
            self.update_direction_markers()
            Clock.schedule_once(lambda *_: self._attempt_move(direction), 0)

        msg = entry.get("message_unlock") or f"You use the [b]{item_name}[/b] to unlock the {direction.title()} passage."
        popup = self.narrate(msg, autoclose=True, tap_to_dismiss=True)
        if popup:
            popup.bind(on_dismiss=lambda *_: _use_key())
        else:
            _use_key()
        return True

    def is_direction_blocked(self, direction: str) -> bool:
        room = self.current_room
        return bool(self._direction_block_info(room, direction))

    def _handle_blocked_direction(self, direction: str):
        dir_key = (direction or "").lower()
        info = self._direction_block_info(self.current_room, dir_key)
        if not info:
            dest = None
            try:
                dest = self.current_room.exits.get(dir_key)
            except Exception:
                dest = getattr(self.current_room, dir_key, None)
            if dest:
                opposite = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}
                info = self._direction_block_info(dest, opposite.get(dir_key))

        if info and info[0] == "lock":
            if self._try_unlock_lock(dir_key, info[1]):
                return

        try:
            if hasattr(self, "cinematics") and self.cinematics:
                self.cinematics.screen_shake(magnitude=10, duration=0.35)
        except Exception:
            pass

        self.ensure_direction_markers()
        marker = self._dir_markers.get(dir_key)
        if marker:
            if info:
                kind, _ = info
                marker.status = "enemy" if kind == "enemy" else "lock"
                marker.active = True
                marker.opacity = 1.0
            marker.flash_block()
            Clock.schedule_once(lambda *_: marker.active and marker._start_pulse(), 0.75)


    def update_direction_markers(self):
        """
        Compute and show/hide/pulse the edge markers based on unexplored neighbors.
        Call this from update_room().
        """
        self.ensure_direction_markers()

        show_unexplored = self._unexplored_adjacent_directions()
        self._active_direction_blocks = {}
        # Suppress the left (west) indicator in Nova's House until the light is on
        try:
            if getattr(self.current_room, 'room_id', '') == 'town_9':
                st = getattr(self.current_room, 'state', {}) or {}
                is_dark = False
                if 'dark' in st:
                    is_dark = bool(st.get('dark'))
                elif 'light_on' in st:
                    is_dark = not bool(st.get('light_on'))
                if is_dark:
                    show_unexplored.discard('west')
        except Exception:
            pass
        opposites = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}
        for d, marker in self._dir_markers.items():
            block_info = self._direction_block_info(self.current_room, d)
            if not block_info:
                dest = None
                try:
                    dest = self.current_room.exits.get(d)
                except Exception:
                    dest = getattr(self.current_room, d, None)
                if dest:
                    opp = opposites.get(d)
                    block_info = self._direction_block_info(dest, opp)
            if block_info:
                kind, data = block_info
                marker.status = "enemy" if kind == "enemy" else "lock"
                marker.active = True
                marker.opacity = 1.0
                self._active_direction_blocks[d] = kind
                if d in show_unexplored:
                    show_unexplored.discard(d)
            elif d in show_unexplored:
                marker.status = "unexplored"
                marker.active = True
                marker.opacity = 1.0
            else:
                marker.active = False
                marker.status = "unexplored"
                marker.opacity = 0.0

    def update_exit_buttons(self):
        # If the exit_buttons container has been removed from the KV, do nothing
        if 'exit_buttons' not in self.parent.ids:
            return
        grid = self.ids.exit_buttons
        grid.clear_widgets()
        exits = self.current_room.exits()
        for direction in ('north', 'east', 'south', 'west'):
            if direction in exits:
                btn = Button(
                    text=direction.capitalize(), 
                    font_name=fonts["button"]["name"],
                    size_hint=(1, 1),
                    background_color=(0.2, 0.2, 0.2, 0.8),
                    color=(0.56, 0.78, 1, 1),
                    font_size=fonts["button"]["size"]
                )
                btn.bind(on_release=lambda _, d=direction: go(d))
                grid.add_widget(btn)
            else:
                grid.add_widget(Widget(size_hint=(None, None), size=(dp(40), dp(40))))

    def update_room(self):
        self.update_room_display()
        self.update_inventory_display()
        self.update_minimap()
        # --- THIS IS THE FIX ---
        # The call to the non-existent update_exit_buttons() is removed.
        # self.update_exit_buttons()
        self._apply_room_weather()

        # NEW: Logic to show/hide the "Return to Hub" button
        wm = self.world_manager
        current_node_id = wm.current_node_id
        if current_node_id:
            node_data = wm.nodes.get(current_node_id)
            entry = node_data.get('entry_room') if node_data else None
            if node_data and self.current_room and self.current_room.room_id == entry:
                self.ids.return_hub_button.opacity = 1
                self.ids.return_hub_button.disabled = False
            else:
                self.ids.return_hub_button.opacity = 0
                self.ids.return_hub_button.disabled = True


        if hasattr(self.ids, "minimap") and self.current_room:
            self.ids.minimap.refresh(self.room_positions, self.current_room.room_id)
        else:
            # Should not be in a room if there is no current node, but as a fallback, hide it.
            self.ids.return_hub_button.opacity = 0
            self.ids.return_hub_button.disabled = True
        self.update_direction_markers()
        # AUTOSAVE: room updates are a good low-cost checkpoint
        self._autosave(f"room:{self.current_room.room_id if self.current_room else 'hub'}")

    def _keyboard_closed(self):
        """Unbind the keyboard when it's no longer needed."""
        if hasattr(self, '_keyboard') and self._keyboard:
            self._keyboard.unbind(on_key_down=self._on_keyboard_down)
            self._keyboard = None

    def _on_keyboard_down(self, keyboard, keycode, text, modifiers):
        """Handle keyboard presses for movement."""
        if self.parent and self.parent.manager and self.parent.manager.current != 'explore':
            return False

        key = keycode[1]
        if key == 'up':
            self._attempt_move('north')
        elif key == 'down':
            self._attempt_move('south')
        elif key == 'left':
            self._attempt_move('west')
        elif key == 'right':
            self._attempt_move('east')
        return True

    # ?????? helper callbacks ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    def _enemy_go_alert(self, enemy_item, *_):
        # still alive & in the current room?
        if not self.current_room.enemies.find(enemy_item.enemy_id):
            return
        if enemy_item.state != BEH_PASSIVE:
            return
        enemy_item.state = BEH_ALERTABLE
        self.narrate(f"The {enemy_item.name} focuses its sensors on you???")
        ev = Clock.schedule_once(
            partial(self._enemy_start_ambush, enemy_item),
            enemy_item.alert_delay
        )
        enemy_item._timers.append(ev) # FIX: Was appended twice
        if not hasattr(enemy_item, "_timer_fire_times"):
            enemy_item._timer_fire_times = {}
        enemy_item._timer_fire_times[ev] = Clock.get_time() + enemy_item.alert_delay

    def _enemy_start_ambush(self, enemy_item, *_):
        # Still in the same room and enemy still present?
        if self.current_room.enemies.find(enemy_item.enemy_id) is None:
            return
        self._cancel_enemy_timers(enemy_item)
        self.narrate(f"[color=ff7f50][b]{enemy_item.name} attacks![/b][/color]")
        self.start_battle(
            enemy_ids=list(enemy_item.party),
            victory_cb=lambda: self._on_enemy_victory(list(enemy_item.party))
        )
        enemy_item.state = "fighting"

    def _cancel_enemy_timers(self, enemy_item):
        if hasattr(enemy_item, "_timers"):
            for ev in enemy_item._timers:
                Clock.unschedule(ev)
            enemy_item._timers.clear()
        if hasattr(enemy_item, "_timer_fire_times"):
            enemy_item._timer_fire_times.clear()

    def pause_enemy_timers(self):
        if not getattr(self, 'current_room', None):
            return
        now = Clock.get_time()
        for en in getattr(self.current_room, 'enemies', []):
            if not getattr(en, "_timers", None):
                continue

            # There should only ever be one active timer per enemy.
            # We'll process the first one we find and then stop.
            for ev in list(en._timers):
                fire_times = getattr(en, "_timer_fire_times", {})
                fire_time = fire_times.get(ev)

                if fire_time is None:
                    # This is a fallback and shouldn't be needed, but is safe to have.
                    remaining = getattr(en, "alert_delay", 3)
                else:
                    remaining = max(0.0, fire_time - now)

                # Cancel the event and clear it from the lists.
                ev.cancel()
                en._timers.remove(ev)
                if ev in fire_times:
                    fire_times.pop(ev)

                # Store the state and remaining time for when we resume.
                en._paused_remaining = remaining
                en._paused_state = en.state
                break  # Stop after processing the first timer.

    def resume_enemy_timers(self):
        if not getattr(self, 'current_room', None):
            return
        now = Clock.get_time()
        for en in getattr(self.current_room, 'enemies', []):
            # Check if there's a paused state to resume from.
            if not hasattr(en, '_paused_remaining'):
                continue

            remaining = en._paused_remaining
            state = getattr(en, "_paused_state", None)

            # Determine which action to resume.
            callback = None
            if state == BEH_PASSIVE:
                callback = partial(self._enemy_go_alert, en)
            elif state == BEH_ALERTABLE:
                callback = partial(self._enemy_start_ambush, en)

            if callback:
                # CORE FIX: If time is up, fire the event almost immediately.
                # Otherwise, schedule it for the actual time remaining.
                if remaining <= 0:
                    Clock.schedule_once(lambda dt: callback(), 0.01)
                else:
                    ev = Clock.schedule_once(lambda dt: callback(), remaining)
                    en._timers.append(ev)
                    en._timer_fire_times[ev] = now + remaining

            # IMPORTANT: Clean up the temporary pause attributes so this
            # doesn't run again until the next pause.
            del en._paused_remaining
            if hasattr(en, '_paused_state'):
                del en._paused_state

    # -----------------------------------------------------------------------
    # UI events
    # -----------------------------------------------------------------------

    # In StarBorn/game.py -> MainWidget class

    def on_ref_press(self, ref):
        # --- any clickable link cancels pending long???press minimap ---
        if getattr(self, "_lp_ev", None) is not None:
            from kivy.clock import Clock
            Clock.unschedule(self._lp_ev)
            self._lp_ev = None

        # --- Handle Exits ---
        if ref.startswith("go_"):
            direction = ref.split("_")[1]
            self._attempt_move(direction)
            return True  # <-- ADD THIS LINE

        # --- Handle Item Pickups ---
        if ref.startswith("pickup_"):
            item_lookup = ref.split("pickup_")[1]
            item_name = item_lookup.replace("_", " ")
            obj = self.current_room.items.find(item_name) or self.all_items.find(item_name)
            if not obj:
                candidate = getattr(self, 'all_items', Bag()).find(item_name) if hasattr(self, 'all_items') else None
                if candidate and not any(getattr(existing, 'name', '').lower() == getattr(candidate, 'name', '').lower() for existing in self.current_room.items):
                    self.current_room.items.add(candidate)
                    obj = candidate
                else:
                    flavor_map = getattr(self.current_room, 'item_flavor', {}) or {}
                    display_name = None
                    flavor_text = None
                    for key, val in flavor_map.items():
                        if str(key).lower() == item_name:
                            display_name = key
                            flavor_text = val
                            break
                    if display_name is None:
                        display_name = item_name.title()
                    placeholder = getattr(self, 'all_items', Bag()).find(display_name) if hasattr(self, 'all_items') else None
                    if not placeholder:
                        placeholder = Item(display_name)
                        if flavor_text:
                            placeholder.description = flavor_text
                        elif not getattr(placeholder, 'description', None):
                            placeholder.description = f"A {display_name}."
                        if placeholder.name.lower() not in placeholder.aliases:
                            placeholder.aliases.append(placeholder.name.lower())
                        if item_name not in placeholder.aliases:
                            placeholder.aliases.append(item_name)
                        if hasattr(self, 'all_items'):
                            self.all_items.add(placeholder)
                    if not any(getattr(existing, 'name', '').lower() == placeholder.name.lower() for existing in self.current_room.items):
                        self.current_room.items.add(placeholder)
                    obj = placeholder
            if not obj:
                return True # <-- ADD THIS LINE

            def do_pickup():
                self.inventory[obj.name] = self.inventory.get(obj.name, 0) + 1
                self.current_room.items.remove(obj)
                canonical_name = re.sub(r'^(a|an|the)\s+', '', obj.name, flags=re.I).lower()
                self.event_manager.item_acquired(canonical_name)
                self.narrate(f"You take [b]{obj.name}[/b].")
                self.update_inventory_display()
                self.update_room_display()

            actions = [("Pick up", do_pickup)]
            self.show_action_popup(obj.name.title(), actions)
            return True # <-- ADD THIS LINE

        # --- Handle Data-Driven Room Actions ---
        if ref.startswith("action_"):
            act_key = ref[7:].replace('_', ' ').lower()
            room_act = next((a for a in self.current_room.actions if a["name"].lower() == act_key), None)
            if not room_act:
                self.narrate("(Nothing happens.)")
                return True # <-- ADD THIS LINE

            # --- Specific handler for the storage locker ---
            if act_key == "storage locker":
                if self.inventory.get("a small key", 0) > 0:
                    def _unlock():
                        self.inventory["a small key"] -= 1
                        if self.inventory["a small key"] == 0:
                            del self.inventory["a small key"]
                        self.update_inventory_display()
                        self.event_manager.player_action("unlock_locker")
                        self.narrate("You use the small key. The locker clicks open.")
                        self.update_room()
                    self.show_action_popup("Storage Locker", [("Use small key", _unlock)])
                else:
                    self.narrate(room_act.get("condition_unmet_message", "It's locked tight."))
                return True # <-- ADD THIS LINE

            atype = room_act.get("type")
            if atype == "toggle":
                state_key = room_act.get("state_key")
                base_event = room_act.get("action_event")
                event_on = room_act.get("action_event_on")
                event_off = room_act.get("action_event_off")
                if not state_key or not (base_event or event_on or event_off):
                    self.narrate("(Broken toggle action.)")
                    return True # <-- ADD THIS LINE

                st = getattr(self.current_room, 'state', {}) or {}
                cur_val = getattr(self.current_room, state_key, st.get(state_key, False))
                # Allow per-action label overrides
                label_on = room_act.get("label_on")
                label_off = room_act.get("label_off")
                if label_on or label_off:
                    label = label_off if cur_val else label_on
                else:
                    label = "Turn off" if cur_val else "Turn on"

                event_name = None
                if cur_val:
                    event_name = event_off or base_event
                else:
                    event_name = event_on or base_event

                if not event_name:
                    self.narrate("(Broken toggle action event.)")
                    return True

                def _do_toggle():
                    # Click SFX when flipping the switch
                    try:
                        # Prefer a UI click if available; fallback to a bundled hit as placeholder
                        has_tag = False
                        try:
                            # _resolve_file returns a Path if present; safe to peek
                            has_tag = bool(self.audio._resolve_file("menu_select"))
                        except Exception:
                            has_tag = False
                        if has_tag:
                            AudioEvents.emit("sfx.play", {"tag": "menu_select", "bus": "UI"})
                        #else:
                        #    self.audio.play_sfx("hit_01.mp3", bus="UI")
                    except Exception:
                        pass
                    self.event_manager.player_action(event_name)
                    self.update_room()

                # If this is the Nova's House light switch, pause the light tutorial timer
                try:
                    if act_key == 'light switch' and getattr(self, 'tutorials', None):
                        self.tutorials.pause_light_switch_hint()
                except Exception:
                    pass

                self.show_action_popup(room_act.get("popup_title", "Toggle"), [(label, _do_toggle)])
                return True # <-- ADD THIS LINE
            elif atype == "container":
                state_key = room_act.get("state_key")
                if not state_key:
                    self.narrate("(Broken container action.)")
                    return True

                st = getattr(self.current_room, 'state', {}) or {}
                is_opened = getattr(self.current_room, state_key, st.get(state_key, False))

                if is_opened:
                    self.narrate("The container is empty.")
                    return True

                def _open_container():
                    items_to_give = room_act.get("items", [])
                    if not items_to_give:
                        self.narrate("The container is empty.")
                        return

                    for item_id in items_to_give:
                        item_obj = self.all_items.find(item_id)
                        if item_obj:
                            self.inventory[item_obj.name] = self.inventory.get(item_obj.name, 0) + 1
                            self.narrate(f"You found a [b]{item_obj.name}[/b].")

                    st[state_key] = True
                    setattr(self.current_room, state_key, True)
                    self.update_inventory_display()
                    self.update_room_display()

                self.show_action_popup(room_act.get("name", "Container"), [("Open", _open_container)])
                return True
            
            # --- Generic handler for other actions ------------------------------------
            elif atype == "tinkering":
                def _open():
                    # No local import needed, as TinkeringScreen is already imported.
                    sm = App.get_running_app().screen_manager
                    # The screen is already added at startup, so this check is good.
                    if not sm.has_screen("tinkering"):
                        sm.add_widget(TinkeringScreen(name="tinkering"))
                    sm.current = "tinkering"

                self.pause_enemy_timers()
                App.get_running_app().tx_mgr.launch_system("tinkering", _open)
                return True # <-- ADD THIS LINE

            elif atype == "cooking":
                def _open():
                    from ui.cooking_screen import CookingScreen
                    sm = App.get_running_app().screen_manager
                    if not sm.has_screen("cooking"):
                        sm.add_widget(CookingScreen(name="cooking"))
                    sm.current = "cooking"

                self.pause_enemy_timers()
                App.get_running_app().tx_mgr.launch_system("cooking", _open)
                return True # <-- ADD THIS LINE

            elif atype == "fishing":
                zone = self.current_room.state.get("zone", "forest")

                def _open():
                    from ui.fishing_screen import FishingScreen
                    sm = App.get_running_app().screen_manager
                    if not sm.has_screen("fishing"):
                        sm.add_widget(FishingScreen(name="fishing", zone_id=zone))
                    sm.current = "fishing"

                self.pause_enemy_timers()
                App.get_running_app().tx_mgr.launch_system("fishing", _open)
                return True # <-- ADD THIS LINE

            elif atype == "shop":
                shop_id = self.current_room.state.get("shop", "default")
                screen_name = f"shop_{shop_id}"

                def _open():
                    from ui.shop_screen import ShopScreen
                    sm = App.get_running_app().screen_manager
                    if not sm.has_screen(screen_name):
                        sm.add_widget(ShopScreen(name=screen_name, shop_id=shop_id))
                    sm.current = screen_name

                self.pause_enemy_timers()
                App.get_running_app().tx_mgr.launch_system("shop", _open)
                return True # <-- ADD THIS LINE

            # Fallback for any other unhandled actions
            self.event_manager.player_action(act_key)
            return True # <-- ADD THIS LINE



        # --- Handle NPC Clicks ---
        if ref.startswith("npc_"):
            npc_id = ref[4:].replace('_', ' ')
            npc_in_room = self.current_room.npcs.find(npc_id)
            if not npc_in_room:
                self.narrate(f"You don't see {npc_id} here.")
                return True # <-- ADD THIS LINE

            full_npc_def = self.all_npcs.find(npc_in_room.name)
            if not full_npc_def or not hasattr(full_npc_def, 'interactions'):
                self.narrate(f"{npc_in_room.name} has nothing to say.")
                return True # <-- ADD THIS LINE

            actions = []
            for inter_def in full_npc_def.interactions:
                label = inter_def.get("label")
                callback = partial(self.handle_npc_interaction, full_npc_def, inter_def)
                actions.append((label, callback))
            
            if actions:
                self.show_action_popup(full_npc_def.name, actions)
            return True # <-- ADD THIS LINE

        # --- Handle Enemy Clicks ---
        if ref.startswith("attack_"):
            eid = ref.split("_", 1)[1]
            enemy_item = self.current_room.enemies.find(eid)
            if not enemy_item:
                base_enemy = getattr(self, 'all_enemies', Bag()).find(eid) if hasattr(self, 'all_enemies') else None
                if base_enemy:
                    cloned = _clone_enemy(base_enemy)
                    if not getattr(cloned, 'party', None):
                        cloned.party = [getattr(cloned, 'enemy_id', eid)]
                    if not any(getattr(existing, 'enemy_id', '').lower() == getattr(cloned, 'enemy_id', '').lower() for existing in self.current_room.enemies):
                        self.current_room.enemies.add(cloned)
                    enemy_item = self.current_room.enemies.find(eid)
                if not enemy_item:
                    self.narrate("[i](That enemy is already defeated.)[/i]")
                    return True # <-- ADD THIS LINE

            def _start_encounter():
                self.start_battle(
                    enemy_ids=list(enemy_item.party),
                    victory_cb=lambda: self._on_enemy_victory(list(enemy_item.party))
                )

            actions = [("Attack", _start_encounter)]
            self.show_action_popup(enemy_item.name, actions)
            return True # <-- ADD THIS LINE

        # If no ref was handled, you can uncomment this for debugging
        # self.say(f"[i](Unhandled ref: {ref})[/i]")

    def play_dialogue(self, dialogue_id: str):
        """Plays a single dialogue entry by its ID."""
        entry = self.dialogue_manager.dialogue_data.get(dialogue_id)
        if not entry:
            return
        self.dialogue_box.show_dialogue(entry.get("speaker", ""), entry.get("text", ""))

    def handle_npc_interaction(self, npc, interaction_def):
        """Dispatches NPC interactions based on their definition in npcs.json."""
        itype = interaction_def.get("type")

        if itype == "talk" or itype == "quest_talk":
            # For both simple talk and quest talk, we trigger the event manager
            # and then let the dialogue manager find the right line based on state.
            self.event_manager.talk_to(npc.name.lower())
            dialogue_id = self.dialogue_manager.get_dialogue_for_npc(npc.name)
            if dialogue_id:
                self.dialogue_manager.play_dialogue(dialogue_id)
            else:
                self.narrate(f"{npc.name} has nothing to say right now.")
        
        elif itype == "shop":
            shop_id = interaction_def.get("shop_id")
            if shop_id:
                self.start_shop(shop_id)
            else:
                self.narrate(f"{npc.name} doesn't seem to be selling anything.")
        
        elif itype == "give_item":
            item_name_to_give = interaction_def.get("item")
            if not item_name_to_give:
                return

            item_obj = self.all_items.find(item_name_to_give)
            if not item_obj:
                 self.narrate(f"(DEBUG: Unknown item '{item_name_to_give}' in interaction def)")
                 return
            
            # Now check if the canonical item name is in inventory
            if self.inventory.get(item_obj.name, 0) > 0:
                self.inventory[item_obj.name] -= 1
                if self.inventory[item_obj.name] <= 0:
                    del self.inventory[item_obj.name]
                self.update_inventory_display()
                
                # Fire the event manager's `item_given` trigger
                self.event_manager.item_given(item_name=item_obj.name, npc_name=npc.name.lower())
            else:
                self.narrate(f"You don't have a {item_obj.name} to give.")
        
        elif itype == "recruit":
            self.recruit(npc.name.lower())
        
        else:
            self.narrate(f"(Unhandled interaction type: {itype})")

    def handle_npc_interaction(self, npc, interaction_def):
        """Dispatches NPC interactions based on their definition in npcs.json."""
        itype = interaction_def.get("type")

        if itype == "talk" or itype == "quest_talk":
            # For both simple talk and quest talk, we trigger the event manager
            # and then let the dialogue manager find the right line based on state.
            self.event_manager.talk_to(npc.name.lower())
            dialogue_id = self.dialogue_manager.get_dialogue_for_npc(npc.name)
            if dialogue_id:
                self.dialogue_manager.play_dialogue(dialogue_id)
            else:
                self.narrate(f"{npc.name} has nothing to say right now.")
        
        elif itype == "shop":
            shop_id = interaction_def.get("shop_id")
            if shop_id:
                self.start_shop(shop_id)
            else:
                self.narrate(f"{npc.name} doesn't seem to be selling anything.")
        
        elif itype == "give_item":
            item_name_to_give = interaction_def.get("item")
            if not item_name_to_give:
                return
            self.event_manager.item_given(item_name=item_name_to_give, npc_name=npc.name.lower())
        
        elif itype == "recruit":
            self.recruit(npc.name.lower())
        
        else:
            self.narrate(f"(Unhandled interaction type: {itype})")


    def change_resonance(self, delta: int) -> None:
        """Adjust party Resonance and update the battle UI if active."""
        self.resonance = max(self.resonance_min, min(self.max_resonance, self.resonance + delta))
        if getattr(self, "battle_screen", None):
            self.battle_screen.res_bar.set(self.resonance)

    def _calculate_resonance_start_bonus(self) -> float:
        bonus = 0.0
        for member in getattr(self, "party", []):
            if hasattr(member, "get_stat_with_buffs"):
                try:
                    bonus += member.get_stat_with_buffs("resonance_start")
                except Exception:
                    continue
        return bonus

    def get_resonance_start(self) -> int:
        base = getattr(self, "resonance_start_base", 0)
        start = base + self._calculate_resonance_start_bonus()
        start = max(self.resonance_min, start)
        start = min(self.max_resonance, start)
        return int(round(start))

    def _apply_growth(self, character):
        """Applies stat gains to a character when they level up."""
        table = GROWTH_TABLE.get("default")
        # Level - 2 because level 2 uses the first entry (index 0)
        if not table or character.level - 2 >= len(table):
            # Fallback gains should also use primary attributes
            gains = {"hp": 5, "str": 1, "vit": 1, "agi": 1, "fcs": 1}
        else:
            gains = table[character.level - 2]
        
        # CORRECT: Increase the base max_hp value
        character.max_hp += gains.get("hp", 0)

        # REMOVED: Do NOT modify atk, spd, or defense directly.
        # They are calculated automatically from the stats below.
        
        # CORRECT: Increase the primary attributes
        character.strength += gains.get("str", 0)
        character.vitality += gains.get("vit", 0)
        character.agility += gains.get("agi", 0)
        character.focus += gains.get("fcs", 0)
        character.luck += gains.get("lck", 0)
        
        # CORRECT: Heal the character to their new total max HP
        character.hp = character.total_max_hp
        
    def gain_xp(self, character, amount):
        if not hasattr(character, 'xp') or not hasattr(character, 'level'):
            return

        old_level = character.level
        character.xp += amount
        new_level = self.leveling_manager.get_level_for_xp(character.xp)

        if new_level > old_level:
            # Grant AP and apply stat growth for each level gained
            for level_gained in range(old_level + 1, new_level + 1):
                character.level = level_gained
                character.ability_points += 1 # +1 AP per level 
                self._apply_growth(character)
                
                # --- NEW: Check for and award level-up skills ---
                level_skills = self.progression_data.get("level_up_skills", {})
                char_skills = level_skills.get(character.id, {})
                skill_to_learn = char_skills.get(str(level_gained))
                
                if skill_to_learn and skill_to_learn not in character.unlocked_abilities:
                    character.unlocked_abilities.add(skill_to_learn)
                    # We use _get_skill_node to find the friendly name for the message
                    skill_node = self._get_skill_node(skill_to_learn)
                    skill_name = skill_node['name'] if skill_node else skill_to_learn
                    self.narrate(f"[color=00ff00][b]{character.name} learned {skill_name}![/b][/color]")

    def start_battle(self,
                    enemy_ids: list[str] | None = None,
                    *,
                    victory_cb=None,
                    background_override: str = None,
                    formation: list[str] | None = None):
        
        self.pause_enemy_timers()
        app = App.get_running_app()
        
        # This function now accepts the final background texture from the transition
        def switch_to_battle_screen(final_bg_texture):
            enemy_ids_list = enemy_ids or ["security_drone"]
            players = list(self.party)
            enemies = [load_enemy(eid) for eid in enemy_ids_list]

            def _on_victory():
                total_xp = sum(getattr(e, "xp_reward", 0) for e in enemies)
                if total_xp > 0:
                    for ch in self.party:
                        if ch.is_alive():
                            self.gain_xp(ch, total_xp)
                if victory_cb:
                    victory_cb()

            def _on_defeat():
                self.narrate("[i]The party wakes up back in the living quarters???[/i]")
                for p in players:
                    p.hp = p.max_hp # This was a bug, it should be total_max_hp

            # --- THIS IS THE FIX ---
            # Reset resonance to its minimum value BEFORE creating the BattleScreen.
            self.resonance = self.get_resonance_start()

            # Create the battle screen instance.
            # Pass the final background texture to the BattleScreen
            battle = BattleScreen(
                app.screen_manager, players=players, enemies=enemies, audio=self.audio,
                on_victory=_on_victory, on_defeat=_on_defeat, formation=formation,
                final_bg_texture=final_bg_texture
            )
            
            if app.screen_manager.has_screen("battle"):
                 app.screen_manager.remove_widget(app.screen_manager.get_screen("battle"))
            
            app.screen_manager.add_widget(battle)
            app.screen_manager.current = "battle"
            self.battle_screen = battle
            
            self.milestones.on_battle_end(enemy_ids_list[0]) # This can stay here.

        # FIX: Call the new, all-in-one transition function
        app.transition_manager.play_combined_combat_entry(switch_to_battle_screen)


    def start_shop(self, shop_id: str):
        """Launch the shop screen for a given shop ID (with cinematic transition)."""
        if shop_id not in self.all_shops:
            self.narrate("This shop is not open right now.")
            return

        app = App.get_running_app()

        def _switch():
            sm = app.screen_manager
            from ui.shop_screen import ShopScreen
            name = f"shop_{shop_id}"
            if not sm.has_screen(name):
                sm.add_widget(ShopScreen(name=name, shop_id=shop_id))
            sm.current = name

        # Pause world logic & run the transition
        self.pause_enemy_timers()
        app.tx_mgr.launch_system("shop", _switch, blur_time=1.0, fade_time=0.3)

    def recruit(self, member_key: str):
        # Add Character to the party
        member = self.party_defs.get(member_key)
        if not member or member in self.party:
            return
        self.party.append(member)

        # Remove the NPC version from every room so they no longer appear
        npc_item = self.all_npcs.find(member_key)
        if npc_item:
            for room in self.rooms.values():
                if room.npcs.find(member_key):
                    room.npcs.remove(npc_item)

        # --- THIS IS THE FIX: Use a narrative popup for the announcement ---
        self.narrate(
            f"[b]{member.name}[/b] has joined your party!",
            title="New Party Member"
        )
        # --------------------------------------------------------------------
        self.update_room_display()

class MainMenuButton(Button):
    """A custom-styled button for the main menu with a rounded rectangle design."""
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        # Disable default button graphics
        self.background_normal = ''
        self.background_down = ''
        self.background_color = (0, 0, 0, 0) # Make default background transparent
        self.border = (0, 0, 0, 0)

        # --- NEW: Add a dark outline for text readability ---
        self.outline_width = dp(1.5)
        self.outline_color = (0, 0, 0, 0.85)
        # ----------------------------------------------------

        # Define colors for different states
        self.color_normal = (0.1, 0.15, 0.2, 0.2)  # More transparent background
        self.color_down = (0.2, 0.3, 0.4, 0.3)    # Brighter, but still transparent on press
        self.border_color_normal = (0.6, 0.8, 1.0, 0.45) # Subtler border
        self.border_color_down = (0.8, 0.95, 1.0, 0.9)   # Brighter border on press

        with self.canvas.before:
            # --- NEW: Add a subtle shadowbox behind the button ---
            self.shadow_color = Color(0, 0, 0, 0.35) # Made the shadow darker
            self.shadow_rect = RoundedRectangle(pos=self.pos, size=self.size, radius=[dp(12)])
            # ----------------------------------------------------

            # Background
            self.bg_color = Color(rgba=self.color_normal)
            self.bg_rect = RoundedRectangle(pos=self.pos, size=self.size, radius=[dp(12)])
            # Border
            self.border_color = Color(rgba=self.border_color_normal)
            self.border_line = Line(rounded_rectangle=(self.x, self.y, self.width, self.height, dp(12)), width=dp(1.5))

        self.bind(pos=self._update_graphics, size=self._update_graphics, state=self._update_graphics)

    def _update_graphics(self, *args):
        """Update the button's appearance based on its state (normal/down)."""
        is_down = self.state == 'down'
        self.bg_color.rgba = self.color_down if is_down else self.color_normal
        self.border_color.rgba = self.border_color_down if is_down else self.border_color_normal

        self.shadow_rect.pos = (self.x, self.y - dp(2)) # Offset shadow slightly down
        self.shadow_rect.size = self.size

        self.bg_rect.pos = self.pos
        self.bg_rect.size = self.size
        self.border_line.rounded_rectangle = (self.x, self.y, self.width, self.height, dp(12))


class MainMenu(FloatLayout):
    """Simple title screen with New/Load/Settings/Quit."""

    def __init__(self, **kw):
        super().__init__(**kw)

        # 1. Adds the background image from your assets
        with self.canvas.before:
            from kivy.graphics import Rectangle, Color
            Color(1, 1, 1, 1) # Ensure image is not tinted
            self.bg = Rectangle(source='images/main_menu_background.png',
                                 pos=self.pos,
                                 size=self.size)
        self.bind(pos=self.update_bg, size=self.update_bg)

        # 2. This layout is moved lower and styled to better fit the new button design
        layout = BoxLayout(
            orientation="vertical",
            spacing=dp(10),
            size_hint=(0.6, 0.5), # Made the layout narrower for a tighter look
            pos_hint={"center_x": 0.5, "center_y": 0.4}, # Moved buttons down
        )

        for text, cb in [
            ("New Game", self._new_game),
            ("Load Game", self._load_game),
            ("Settings", self._settings),
            ("Quit", self._quit),
        ]:
            # 3. These are the new, creatively styled buttons
            btn = MainMenuButton(text=text, size_hint=(1, None), height=dp(65), font_name=fonts["menu_button"]["name"], font_size=fonts["menu_button"]["size"])
            btn.bind(on_release=cb)
            layout.add_widget(btn)
        self.add_widget(layout)

    def update_bg(self, *args):
        # Helper to make sure the background resizes with the window
        self.bg.pos = self.pos
        self.bg.size = self.size

    def _new_game(self, *_):
        App.get_running_app().start_new_game()

    def _load_game(self, *_):
        App.get_running_app().prompt_load_slot()

    def _settings(self, *_):
        # Open the real tabbed Settings inside the MenuOverlay
        App.get_running_app().open_menu('settings')

    def _quit(self, *_):
        App.get_running_app().stop()

class LoadGameScreen(Screen):
    """A dedicated screen for loading saved games with an improved layout."""
    def __init__(self, **kwargs):
       # Inside the LoadGameScreen __init__ method
        super().__init__(**kwargs)

        # 1. Background image
        with self.canvas.before:
            from kivy.graphics import Rectangle, Color
            Color(1, 1, 1, 1)
            self.bg = Rectangle(source='images/load_game_background.png',
                                 pos=self.pos,
                                 size=self.size)
        self.bind(pos=self.update_bg, size=self.update_bg)

        # 2. Main container, framed and positioned at the top
        self.container = BorderedFrame(
            orientation="vertical",
            spacing=dp(15),
            padding=(dp(15), dp(20)),
            size_hint=(0.8, None),
            pos_hint={"center_x": 0.5, "top": 0.9},
            border_color=(0.5, 0.7, 1.0, 1.0)
        )
        self.container.bind(minimum_height=self.container.setter('height'))

        # Add a semi-transparent background for readability
        with self.container.canvas.before:
            Color(0.07, 0.07, 0.1, 0.85)
            self.container_bg = Rectangle(pos=self.container.pos, size=self.container.size)
        self.container.bind(pos=lambda i, p: setattr(self.container_bg, 'pos', p),
                            size=lambda i, s: setattr(self.container_bg, 'size', s))

        # 3. Title Label
        title = Label(text="Load Game",
                      font_name=fonts["menu_title"]["name"],
                      font_size=fonts["menu_title"]["size"],
                      size_hint_y=None,
                      height=dp(50))
        self.container.add_widget(title)

        # 4. Grid for save slots (will be populated dynamically)
        self.slots_grid = GridLayout(cols=1, spacing=dp(10), size_hint_y=None)
        self.slots_grid.bind(minimum_height=self.slots_grid.setter('height'))
        self.container.add_widget(self.slots_grid)
        
        # 5. Spacer
        self.container.add_widget(Widget(size_hint_y=None, height=dp(10)))

        # 6. Back Button
        back_btn = MainMenuButton(
            text="Back to Main Menu",
            size_hint_y=None,
            height=dp(65),
            font_name=fonts["menu_button"]["name"],
            font_size=fonts["menu_button"]["size"],
        )
        back_btn.bind(on_release=self.go_back)
        self.container.add_widget(back_btn)

        self.add_widget(self.container)

    def on_pre_enter(self, *args):
        """Populate the save slots every time the screen is shown."""
        # If a game is already running you may want to pause its enemy timers
        # (or resume when returning). For now just ignore if no game.
        from kivy.app import App
        app = App.get_running_app()
        # optional: pause timers so nothing happens while choosing a slot
        if getattr(app, "current_game", None):
            app.current_game.pause_enemy_timers()

        self.slots_grid.clear_widgets()
        for i in range(1, 4):
            path = f"save{i}.json"
            if os.path.exists(path):
                save_time = os.path.getmtime(path)
                time_str = datetime.datetime.fromtimestamp(save_time).strftime('%Y-%m-%d %H:%M')
                label = f"Save Slot {i}\n[size={int(sp(14))}]{time_str}[/size]"
                is_disabled = False
            else:
                label = f"Save Slot {i}\n[size={int(sp(14))}](Empty)[/size]"
                is_disabled = True

            btn = MainMenuButton(
                text=label,
                markup=True,
                size_hint_y=None,
                height=dp(65),
                font_name=fonts["menu_button"]["name"],
                font_size=fonts["menu_button"]["size"],
                disabled=is_disabled
            )
            btn.bind(on_release=partial(self.load_slot, i))
            self.slots_grid.add_widget(btn)

    def update_bg(self, *args):
        self.bg.pos = self.pos
        self.bg.size = self.size

    def load_slot(self, slot_number, *args):
        App.get_running_app()._start_game(slot_number)

    def go_back(self, *args):
        self.manager.current = 'menu'


# ---------------------------------------------------------------------------
# App bootstrap
# ---------------------------------------------------------------------------
class StarbornApp(App):
    # *** FIX: Make fonts a property of the App class ***
    fonts = DictProperty(fonts)

    def build(self):
        self.current_game = None
        self._last_menu_tab = "inventory"  # Default menu tab
        self.tx_mgr = TransitionManager()
        self.transition_manager = self.tx_mgr  
        Builder.load_string(KV)

        # 1) logical 360??640 ???canvas???
        sm = ScreenManager(size=(BASE_WIDTH, BASE_HEIGHT),
                           size_hint=(None, None))
        # store it on the App so start_battle can find it:
        self.screen_manager = sm
        
        # --- NEW: Add all screens at startup ---
        self.explore_screen = ExplorationScreen(name="explore")
        sm.add_widget(self.explore_screen)
        
        self.hub_screen = HubScreen(name="hub")
        sm.add_widget(self.hub_screen)

        menu_screen = Screen(name="menu")
        menu_screen.add_widget(MainMenu())
        sm.add_widget(menu_screen)
        sm.current = "menu"

        load_screen = LoadGameScreen(name="load_game")
        sm.add_widget(load_screen)
        
        # Add the TinkeringScreen here with its name
        tinkering_screen = TinkeringScreen(name="tinkering")
        sm.add_widget(tinkering_screen)
        
        
        # 2) wrap in a Scatter for scaling
        self._scatter = Scatter(
            size=(BASE_WIDTH, BASE_HEIGHT),
            size_hint=(None, None),
            do_translation=False,
            do_rotation=False,
            do_scale=False
        )
        self._scatter.add_widget(sm)

        # 3) Root layout with theme bands overlay
        root = FloatLayout(size_hint=(1, 1))
        # Content scatter sits below
        root.add_widget(self._scatter)
        # Themed bands overlay on top of everything (do not swallow touches)
        try:
            self.bands = ThemeBands()
            # Append so it renders above the scatter/content
            root.add_widget(self.bands)
            root.bind(size=lambda *_: self.bands.update_layout(), pos=lambda *_: self.bands.update_layout())
            self.bands.update_layout()
        except Exception:
            self.bands = None

        # Global dark scrim drawn in root.canvas.after so it's above everything
        try:
            from kivy.graphics import Color, Rectangle
            from kivy.core.window import Window
            with root.canvas.after:
                self._dark_col = Color(0, 0, 0, 0)
                self._dark_rect = Rectangle(pos=(0, 0), size=(Window.width, Window.height))
                # Edge masks (top/bottom) that can darken areas above UI
                self._edge_top_col = Color(0, 0, 0, 0)
                self._edge_top_rect = Rectangle(pos=(0, 0), size=(Window.width, 0))
                self._edge_bot_col = Color(0, 0, 0, 0)
                self._edge_bot_rect = Rectangle(pos=(0, 0), size=(Window.width, 0))
            def _sync_scrim(*_):
                self._dark_rect.pos = (0, 0)
                self._dark_rect.size = (Window.width, Window.height)
                # Keep edge masks sized to the band regions
                try:
                    bands = getattr(self, 'bands', None)
                    if bands is not None:
                        top_h = bands.top.height
                        bot_h = bands.bottom.height
                        self._edge_bot_rect.pos = (0, 0)
                        self._edge_bot_rect.size = (Window.width, bot_h)
                        self._edge_top_rect.pos = (0, Window.height - top_h)
                        self._edge_top_rect.size = (Window.width, top_h)
                except Exception:
                    pass
            root.bind(pos=_sync_scrim, size=_sync_scrim)
            Window.bind(on_resize=lambda *_: _sync_scrim())
            _sync_scrim()
        except Exception:
            pass

        # 4) bind resize
        Window.bind(on_resize=self._on_resize)
        # Bands also need window resize to refresh safe geometry during scatter scaling
        if self.bands:
            Window.bind(on_resize=lambda *_: self.bands.update_layout())
            # Also keep edge masks synced when bands relayout
            try:
                self.bands.bind(size=lambda *_: self._sync_edge_masks(), pos=lambda *_: self._sync_edge_masks())
            except Exception:
                pass

        # --- ADD the DebugPanel to the ScreenManager ---
        from ui.debug_panel import DebugPanel
        self.screen_manager.add_widget(DebugPanel(name="debug_panel"))

        # --- FINAL: Add Weather Layer to the absolute top ---
        # It's added to the root, after the bands, so it draws over everything.
        # Its default settings do not block touch input.
        self.weather_layer = WeatherLayer(size_hint=(1, 1))
        root.add_widget(self.weather_layer)


        return root

    # Update edge masks sizes (top/bottom) from current bands geometry
    def _sync_edge_masks(self):
        try:
            from kivy.core.window import Window
            bands = getattr(self, 'bands', None)
            if not bands:
                return
            top_h = bands.top.height
            bot_h = bands.bottom.height
            self._edge_bot_rect.pos = (0, 0)
            self._edge_bot_rect.size = (Window.width, bot_h)
            self._edge_top_rect.pos = (0, Window.height - top_h)
            self._edge_top_rect.size = (Window.width, top_h)
        except Exception:
            pass

    # Allow any system to set a full-window dark scrim alpha (0..1)
    def set_global_dark(self, alpha: float, *, animate: bool = False, duration: float = 0.25):
        # NEW: If force_black is active, don't allow fades to clear it.
        if getattr(self, '_force_black_screen', False) and alpha < 1.0:
            return
        try:
            from kivy.animation import Animation
            target = max(0.0, min(1.0, float(alpha)))
            if not hasattr(self, '_dark_col') or self._dark_col is None:
                return
            if animate:
                Animation.cancel_all(self._dark_col, 'a')
                Animation(a=target, duration=max(0.0, float(duration))).start(self._dark_col)
            else:
                Animation.cancel_all(self._dark_col, 'a')
                self._dark_col.a = target
        except Exception as e:
            print(f"[ERROR] set_global_dark failed: {e}")
            pass

    # Edge masks control (top and bottom over UI)
    def set_edge_masks(self, alpha: float, *, animate: bool = False, duration: float = 0.25):
        try:
            from kivy.animation import Animation
            target = max(0.0, min(1.0, float(alpha)))
            if animate:
                Animation.cancel_all(self._edge_top_col, 'a')
                Animation.cancel_all(self._edge_bot_col, 'a')
                Animation(a=target, duration=max(0.0, float(duration))).start(self._edge_top_col)
                Animation(a=target, duration=max(0.0, float(duration))).start(self._edge_bot_col)
            else:
                self._edge_top_col.a = target
                self._edge_bot_col.a = target
        except Exception:
            pass

    # ---------------------------------------------------------------------
    #  Slide-up overlay menu (map, journal, stats, inventory, settings ???)
    # ---------------------------------------------------------------------
    def open_menu(self, section: str):
        print(">>> REAL open_menu CALLED  <<<", section)
        # Store the last opened section
        self._last_menu_tab = section

        from traceback import print_exc
        print(f"[DEBUG-open_menu] entry section={section}")
        try:
            # -------------- original body starts --------------
            app = App.get_running_app()
            # lock world input AND pause enemy timers
            app = App.get_running_app()
            game = getattr(app, "current_game", None)
            # Lock input; only pause timers if we're actually in a room (exploration)
            if game:
                game.input_locked = True
                if getattr(game, "current_room", None):
                    game.pause_enemy_timers()

            # close any existing overlay
            if getattr(self, "_menu_overlay", None):
                self._menu_overlay.dismiss()

            # build + add new overlay
            overlay = MenuOverlay(default_tab=section)

            def on_menu_dismiss(*args):
                """
                Callback to run when the menu is closed.
                Resumes timers and unlocks input.
                """
                if app.current_game:
                    app.current_game.input_locked = False
                    if getattr(app.current_game, "current_room", None):
                        app.current_game.resume_enemy_timers()

                if hasattr(self, "_menu_overlay"):
                    self._menu_overlay = None

            # NOTE: This relies on your MenuOverlay widget firing an 'on_dismiss'
            # event when it closes, similar to a Kivy Popup.
            overlay.bind(on_dismiss=on_menu_dismiss)

            from kivy.core.window import Window
            Window.add_widget(overlay)     # <- use Window layer, not self.root
            self._menu_overlay = overlay
            # -------------- original body ends ----------------
        except Exception as exc:
            print("[DEBUG-open_menu] EXCEPTION:")
            print_exc()
            game = getattr(App.get_running_app(), "current_game", None)
            if game:
                game.input_locked = False
                if getattr(game, "current_room", None):
                    game.resume_enemy_timers()

    # -----------------------------------------------------------------
    #  Game start/load/save helpers
    # -----------------------------------------------------------------
    def start_new_game(self):
        self._start_game(None)

    def prompt_load_slot(self):
        """Switches to the dedicated Load Game screen."""
        self.screen_manager.current = 'load_game'

    def _start_game(self, slot):
        # If a game screen already exists, remove it to ensure a clean start.
        if self.screen_manager.has_screen('explore'):
            self.screen_manager.remove_widget(self.screen_manager.get_screen('explore'))

        # 1. Create a new ExplorationScreen. Kivy automatically builds its children
        #    (the MainWidget) based on the KV rule.
        self.explore_screen = ExplorationScreen(name="explore")

        # 2. Add the fully constructed screen to the manager.
        self.screen_manager.add_widget(self.explore_screen)

        # 3. Now we can safely access the main_widget, which Kivy created for us.
        main_widget = self.explore_screen.ids.main_widget_instance

        # 4. Call the setup method to initialize game logic.
        main_widget.setup_game(load_slot=slot)

        # 5. Update app state and switch to the correct starting screen
        self.current_game = main_widget

        if slot is None:
            # This is a brand new game. Always play the intro.
            # Ensure the UI root (explore) is visible so VFX overlays render
            self.screen_manager.current = "explore"
            def _enter_start_room_after_intro(*_):
                # Spawn in Rusthaven ? Homestead Quarter ? Nova's House
                wm = main_widget.world_manager
                wm.current_node_id = "town"  # Homestead Quarter node id
                try:
                    # Initialize the node map anchored at Nova's House
                    self.current_game.begin_node("town_9")  # "Nova's House"
                    # Fire room-enter events for the starting room
                    try:
                        self.current_game.event_manager.enter_room("town_9")
                    except Exception:
                        pass
                    # Tutorials: schedule hints for first room
                    try:
                        tm = getattr(self.current_game, 'tutorials', None)
                        if tm:
                            tm.on_room_enter("town_9")
                    except Exception:
                        pass
                    # Fade in the very first room more slowly for impact
                    # Allow abort-on-light only for this initial long fade
                    try:
                        self.tx_mgr.abort_on_light_switch = True
                    except Exception:
                        pass
                    self.tx_mgr.fade_in_exploration(18)
                    # Now show the exploration view behind the scrim
                    self.screen_manager.current = "explore"
                except Exception:
                    # Fallback to hub if anything goes wrong
                    self.hub_screen.hub_id = wm.current_hub_id
                    self.screen_manager.current = "hub"
            main_widget.cinematics.play("intro_prologue", on_complete=_enter_start_room_after_intro)

        elif main_widget.current_room:
            # Loaded a save that was inside a room
            self.tx_mgr.fade_in_exploration(1.0)
            self.screen_manager.current = "explore"
        else:
            # Loaded a save that was in a hub
            self.hub_screen.hub_id = main_widget.world_manager.current_hub_id
            self.screen_manager.current = "hub"
            AudioEvents.emit("room.enter", {
                "hub_id": main_widget.world_manager.current_hub_id,
                "room_id": None,
            })

        # (Removed: tooling scene bootstrap via STARBORN_PLAY_SCENE)
        # Dev: optional start location via env vars
        try:
            import os as _os
            w = _os.environ.get('STARBORN_START_WORLD')
            h = _os.environ.get('STARBORN_START_HUB')
            n = _os.environ.get('STARBORN_START_NODE')
            r = _os.environ.get('STARBORN_START_ROOM')
            if all([w, h, n, r]):
                self.world_manager.set_location(w, h, n, r)
                self.screen_manager.current = "explore"
                try:
                    self.event_manager.enter_room(r)
                except Exception:
                    pass
        except Exception:
            pass
        
    def enter_node(self, node_data):
        """Called from HubScreen when a node is clicked."""
        wm = self.current_game.world_manager
        wm.current_node_id = node_data.get('id')

        entry = node_data.get('entry_room')
        if not entry:
            print(f"[WARN] node missing entry_room: {wm.current_node_id}")
            return

        # Guard: verify the room exists
        room_obj = wm.get_room(entry) if hasattr(wm, "get_room") else None
        if room_obj is None:
            print(f"[WARN] node '{wm.current_node_id}' points to unknown room '{entry}'")
            self.hub_screen.hub_id = wm.current_hub_id
            self.screen_manager.current = "hub"
            return

        # Enter the node
        self.current_game.begin_node(entry)

        # Fade in the entry room UI when entering from the hub
        # Start fade immediately (before switching screens) to prevent flash
        self.tx_mgr.fade_in_exploration(1.0)
        # Show exploration (screen is named 'explore' in this app) behind the scrim
        self.screen_manager.transition.direction = 'left'
        self.screen_manager.current = "explore"

    def return_to_hub(self):
        """Called from the 'Return to Hub' button in the exploration screen."""
        if not self.current_game:
            return

        # Persist the node-specific minimap BEFORE clearing context
        self.current_game._stash_current_node_map()

        # Disable the button (if present)
        btn = self.current_game.ids.get("return_hub_button")
        if btn:
            btn.opacity = 0
            btn.disabled = True

        # Clear current room/node (do NOT clear hub)
        self.current_game.current_room = None
        self.current_game.world_manager.current_node_id = None

        # AUDIO: moved back to hub; stop room ambience and use hub music (if mapped)
        AudioEvents.emit("room.enter", {
            "hub_id": self.current_game.world_manager.current_hub_id,
            "room_id": None,
        })

        # Switch to hub (and pass along which hub to show)
        self.hub_screen.hub_id = self.current_game.world_manager.current_hub_id
        self.screen_manager.transition.direction = 'right'
        self.screen_manager.current = "hub"

    def on_start(self):
        # --- ADD Settings Loading ---
        self.load_settings() # settings dict is now initialized in build()
        # window is visible now???do the initial layout
     # window is visible now???do the initial layout
        self._on_resize(Window, *Window.size)
        # Auto-start new game in debug/sandbox if env hints provided
        try:
            import os as _os
            if _os.environ.get('STARBORN_START_WORLD') and \
               _os.environ.get('STARBORN_START_HUB') and \
               _os.environ.get('STARBORN_START_NODE') and \
               _os.environ.get('STARBORN_START_ROOM'):
                Clock.schedule_once(lambda *_: self.start_new_game(), 0)
        except Exception:
            pass

    def on_stop(self):
        # --- ADD Settings Saving ---
        self.save_settings()
        # --- Unbind keyboard on close ---
        if hasattr(self, 'current_game') and self.current_game and hasattr(self.current_game, '_keyboard_closed'):
            self.current_game._keyboard_closed()

        if getattr(self, "current_game", None):
            slot = getattr(self.current_game, "save_slot", 1)
            self.current_game.save_progress(slot=slot)

    def refresh_ui_screens(self):
        """Calls the refresh method on any active, relevant UI screens."""
        # Find the JournalScreen and tell it to refresh its content
        if self.screen_manager.has_screen('journal'):
            journal_screen = self.screen_manager.get_screen('journal')
            if hasattr(journal_screen, 'refresh'):
                journal_screen.refresh()
        
        # We can add other screens here in the future if they also need refreshing
        if self.screen_manager.has_screen('inventory'):
             bag_screen = self.screen_manager.get_screen('inventory')
             if hasattr(bag_screen, 'refresh'):
                 bag_screen.refresh()

    def load_settings(self):
        try:
            with open("settings.json", "r") as f:
                self.settings = json.load(f)
        except (FileNotFoundError, json.JSONDecodeError, TypeError, AttributeError):
            self.settings = {'screenshake': True, 'flashes': True, 'haptics': True}

    def save_settings(self):
        try:
            with open("settings.json", "w") as f:
                json.dump(self.settings, f, indent=4)
        except Exception as e:
            print(f"Error saving settings: {e}")

    def _on_resize(self, window, width, height):
        # Pillar-box or letter-box to fit the screen while maintaining aspect ratio
        scale_x = width / BASE_WIDTH
        scale_y = height / BASE_HEIGHT
        scale = min(scale_x, scale_y)
        
        self._scatter.scale = scale
        
        # Center the scaled canvas
        self._scatter.pos = (
            (width - BASE_WIDTH * scale) / 2,
            (height - BASE_HEIGHT * scale) / 2
        )
        # Update bands geometry
        # Bands (if present) update themselves with their container size

    def enter_node(self, node_data: dict):
        """Switch from a hub into an exploration node."""
        # Make sure we have a valid node entry room
        start_room_id = node_data.get("entry_room") or node_data.get("start_room")
        if not start_room_id:
            print(f"No entry room for node {node_data.get('id')}")
            return

        # Store current node in world manager
        self.current_game.world_manager.current_node_id = node_data.get("id")

        # Begin the node in the MainWidget
        self.current_game.begin_node(start_room_id)

        # Fire enter-room events and tutorials for the spawn room
        try:
            self.current_game.event_manager.enter_room(start_room_id)
        except Exception:
            pass
        try:
            tm = getattr(self.current_game, 'tutorials', None)
            if tm:
                tm.on_room_enter(start_room_id)
        except Exception:
            pass

        # Enable the 'Return to Hub' button
        btn = self.current_game.ids.get("return_hub_button")
        if btn:
            btn.opacity = 1
            btn.disabled = False

        # Switch to exploration screen
        if not self.screen_manager.has_screen("explore"):
            self.screen_manager.add_widget(ExplorationScreen(name="explore"))
        self.screen_manager.current = "explore"

if __name__ == "__main__":
    StarbornApp().run() 
