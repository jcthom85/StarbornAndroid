# StarBorn/combat.py

import json, os, random
from typing import List, Callable
from theme_manager import ThemeManager
from kivy.app import App
from kivy.clock          import Clock
from kivy.metrics        import dp, sp
from kivy.properties     import NumericProperty
from kivy.graphics       import Color, Rectangle, Ellipse, RoundedRectangle, PushMatrix, PopMatrix, Scale
from kivy.uix.widget     import Widget
from kivy.uix.label      import Label
from kivy.uix.boxlayout    import BoxLayout
from kivy.uix.anchorlayout import AnchorLayout
from kivy.uix.image      import Image
from kivy.uix.gridlayout   import GridLayout
from kivy.uix.button      import Button
from kivy.uix.screenmanager import Screen, FadeTransition
from kivy.uix.popup         import Popup
from kivy.animation import Animation
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.scatter import Scatter
from kivy.uix.behaviors import ButtonBehavior
from kivy.resources import resource_find
from ui.menu_popup import MenuPopup
from ui.weather_layer import WeatherLayer
from entities      import Character, Enemy, Status, Element
from sound_manager import SoundManager
import math
from functools import partial, lru_cache
from kivy.properties import NumericProperty
from font_manager import fonts
from environment import EnvironmentLoader
from ui.victory_screen import VictoryScreen
from collections import defaultdict
from constants import CRIT_DAMAGE_MULT
from kivy.graphics.texture import Texture
from kivy.core.window import Window
from data.leveling_manager import LevelingManager as _LevelingManager
from ui.combat_fx import ImpactRing, HitSparks, SlashArc, TargetBrackets, TimedHitRings, TurnTimeline
from ui.combat_background import CombatBackground
from theme_manager import ThemeManager

from ui.shadow_label import ShadowLabel
BASE_DIR = os.path.dirname(__file__)
UI_SCALE  = 1.0
SPRITE_SCALE = 5.0
ss         = lambda px: dp(px * UI_SCALE)

# --- NEW: Mapping for weapon types to their attack effect images ---
# This dictionary connects the 'weapon_type' string from your character data
# to the corresponding image file for the attack animation.
ATTACK_EFFECT_MAP = {
    "sword": "images/ui/attack_sword.png",
    "gun": "images/ui/attack_gun.png",
    "pistol": "images/ui/attack_gun.png", # Add this line
    "gloves": "images/ui/attack_gloves.png",
    "pendant": "images/ui/attack_pendant.png",
}

# Color mapping for elemental damage popups
DAMAGE_COLORS = {
    "burn":  (1.0, 0.2, 0.2, 1),
    "freeze":(0.4, 0.6, 1.0, 1),
    "shock": (1.0, 1.0, 0.2, 1),
    "radiation": (1.0, 0.5, 0.2, 1),
    "poison":(0.3, 1.0, 0.3, 1),
    "psionic":(0.8, 0.6, 1.0, 1),
    "void":  (0.8, 0.8, 0.8, 1),
    "physical":(1.0, 1.0, 1.0, 1),
    "none": (1.0, 1.0, 1.0, 1),
    "fire": (1.0, 0.2, 0.2, 1),
    "ice": (0.4, 0.6, 1.0, 1),
    "lightning": (1.0, 1.0, 0.2, 1)
}


class ClickableImage(ButtonBehavior, Image):
    """An Image that can be clicked (fires on_release)."""
    def on_touch_down(self, touch):
        # This is the fix for mouse clicks not working.
        # We must call super() to allow the ButtonBehavior to process the
        # touch event. Without this, it only works for taps, not clicks.
        if self.collide_point(*touch.pos):
            # Let the ButtonBehavior handle the grab and state change.
            return super().on_touch_down(touch)
        return False

class DamageLabel(ShadowLabel):
    """Floating damage number that self-destructs after a short animation."""
    scale = NumericProperty(1.0)

    def __init__(self, text: str, color=(1, 1, 1, 1), **kwargs):
        super().__init__(
            text=text,
            color=color,
            font_name=fonts["damage_popup"]["name"],
            font_size=fonts["damage_popup"]["size"],
            # Add outline for readability
            use_outline=True,
            outline_color=(0, 0, 0, 0.9),
            outline_width=dp(2.2),
            size_hint=(None, None),
            markup=True,
            **kwargs,
        )
        self.bind(texture_size=self.setter("size"))
        # Bind scale to the canvas transform
        self.bind(scale=self._update_scale)

    def _update_scale(self, *_):
        self.canvas.before.clear()
        with self.canvas.before:
            PushMatrix()
            # Apply the scale transform
            Scale(self.scale, self.scale, 1, origin=self.center)
            # Restore the canvas state immediately after scaling
            PopMatrix()

    def bounce(self, parent: Widget):
        """Animates the label upward, scaling and fading out."""
        from kivy.animation import Animation
        anim = (
            Animation(scale=1.4, y=self.y + dp(30), duration=0.18, t='out_quad') +
            Animation(scale=1.0, y=self.y + dp(60), opacity=0, duration=0.5, t='in_sine')
        )

        def _cleanup(*_):
            if self.parent:
                parent.remove_widget(self)

        anim.bind(on_complete=_cleanup)
        anim.start(self)


def _load_json(fn):
    with open(os.path.join(BASE_DIR, fn), encoding="utf-8") as f:
        return json.load(f)

# Load JSON data
_char_defs  = _load_json("characters.json")
_enemy_defs = _load_json("enemies.json")
_buff_defs  = _load_json("buffs.json")  # Load buff definitions

def _pixel_filter(lbl):
    """
    Force a label’s texture to use nearest‑neighbour filtering
    the moment the texture exists.
    """
    def _apply(inst, tex):
        if tex:
            tex.mag_filter = 'nearest'
            tex.min_filter = 'nearest'
    _apply(lbl, lbl.texture)
    lbl.bind(on_texture=_apply)
    return lbl

def _sprite_for(name: str, is_enemy: bool=False) -> str:
    src = _enemy_defs if is_enemy else _char_defs
    rec = next((r for r in src
                if r.get("name")==name or r.get("id")==name.lower()), None)
    return "\n".join(rec.get("sprite", [])) if rec else ""

def load_enemy(eid: str) -> Enemy:
    d = next(e for e in _enemy_defs if e["id"] == eid)
    # Create Enemy with new stat parameters
    inst = Enemy(
        d["name"], d["hp"], d.get("attack", 0), d.get("speed", 0),
        strength=d.get("strength", 0), vitality=d.get("vitality", 0),
        agility=d.get("agility", 0), focus=d.get("focus", 0), luck=d.get("luck", 0)
    )
    inst.enemy_id = eid
    inst.id = eid
    inst.tier = d.get("tier", "standard")
    
    # --- THIS IS THE FIX ---
    # Set the base max_hp from the JSON file.
    inst.max_hp = d["hp"]
    # Set the current hp to the total_max_hp, which includes bonuses from vitality.
    inst.hp = inst.total_max_hp
    # -----------------------

    # Rewards
    inst.ap_reward = d.get("ap_reward", 0)
    inst.xp_reward = d.get("xp_reward", 0)
    inst.credit_reward = d.get("credit_reward", 0)
    inst.drops = d.get("drops", [])
    # Resistances (already converted to int format in data)
    inst.resistances = d.get("resistances", {})
    return inst

class Gauge(BoxLayout):
    """
    Simple filled-bar widget with an animatable foreground colour.
    """
    def __init__(self, max_val, fg_colour, *, width=dp(72), height=dp(6), radius=None, **kwargs):
        super().__init__(
            orientation="horizontal",
            size_hint=(None, None),
            width=width,
            height=height,
            **kwargs
        )
        self.max_val   = max_val
        self.curr      = max_val
        self._orig_col = list(fg_colour)

        # Default radius if not provided, making it half the height for a pill shape.
        if radius is None:
            self.radius = [height / 2]
        else:
            self.radius = radius

        with self.canvas:
            Color(.15, .15, .20, 1)
            self._bg = RoundedRectangle(pos=self.pos, size=self.size, radius=self.radius)
            self._fg_color = Color(*fg_colour)
            self._fg       = RoundedRectangle(pos=self.pos, size=self.size, radius=self.radius)

        self.bind(pos=self._update, size=self._update)

    def set(self, new_val):
        self.curr = max(0, min(new_val, self.max_val))
        self._update()

    def ready_flash(self):
        from kivy.animation import Animation

        flash = Animation(r=1, g=1, b=1, a=1, duration=0.12)
        glow  = (
            Animation(
                r=self._orig_col[0], g=self._orig_col[1],
                b=self._orig_col[2], a=1, duration=0.12
            )
            + Animation(a=0.85, duration=0.6)
            + Animation(a=1.0,  duration=0.6)
        )
        glow.repeat = True
        flash.bind(on_complete=lambda *_: glow.start(self._fg_color))
        flash.start(self._fg_color)

    def _update(self, *_):
        fill = (self.curr / self.max_val) if self.max_val else 0
        self._bg.pos, self._bg.size = self.pos, self.size
        self._fg.pos  = self.pos
        self._fg.size = (self.width * fill, self.height)

class ResonanceBar(Widget):
    """Animated gradient bar that pulses when full."""

    def __init__(self, maximum: int, *, initial: int = 0,
                 width: float = dp(320), height: float = dp(40), **kw):
        super().__init__(size_hint=(None, None), width=width, height=height, **kw)
        self.max_val = maximum
        self.value   = initial

        with self.canvas:
            # Track (subtle dark sci‑fi panel)
            Color(0.08, 0.08, 0.12, 0.92)
            self._bg = RoundedRectangle(pos=self.pos, size=self.size, radius=[dp(9),])

            # Fill (cyan‑to‑violet gradient simulated via two rects)
            Color(0.15, 0.85, 1.0, 1)   # left: cyan
            self._fg_l = RoundedRectangle(pos=self.pos, size=(0, self.height), radius=[dp(9),])
            Color(0.75, 0.35, 1.0, 1)   # right: violet
            self._fg_r = RoundedRectangle(pos=(self.x + self.width*0.5, self.y), size=(0, self.height), radius=[dp(9),])

            # Glow overlay – alpha animated when bar is full
            self._glow_col = Color(1, 1, 1, 0)   # <‑‑ store the Color instruction!
            self._glow     = RoundedRectangle(pos=self.pos, size=self.size, radius=[dp(9),])

        self.bind(pos=self._update, size=self._update)
        Clock.schedule_interval(self._animate, 1/60)

    # Public API -------------------------------------------------------------
    def set(self, new_val: int):
        self.value = max(0, min(new_val, self.max_val))
        self._update()

    # Internals --------------------------------------------------------------
    def _update(self, *_):
        """Resize/re-position the gradient rectangles so they always
        cover exactly the filled portion of the bar."""
        fill = (self.value / self.max_val) if self.max_val else 0

        # ── Common geometry ────────────────────────────────────────────
        start = self.x
        end   = self.x + self.width * fill        # right edge of the fill
        mid   = start + (end - start) * 0.5       # halfway point of the fill

        # Track (background)
        self._bg.pos  = self.pos
        self._bg.size = self.size

        # Left (cyan) half – full fill width
        self._fg_l.pos  = (start, self.y)
        self._fg_l.size = (end - start, self.height)

        # Right (violet) half – only the *second* half of the fill
        self._fg_r.pos  = (mid, self.y)
        self._fg_r.size = (end - mid, self.height)

        # Glow outline follows the bar
        self._glow.pos  = self.pos
        self._glow.size = self.size

    def _animate(self, _dt):
        # Soft pulse when bar is FULL
        if self.value >= self.max_val:
            t = (Clock.get_time()*2) % 1
            self._glow_col.a = 0.4 + 0.25*math.sin(t*math.pi*2)
        else:
            self._glow_col.a = 0
            
class ElementStackDisplay(Widget):
    """Displays elemental stacks as a series of colored dots."""
    def __init__(self, battler: Enemy, **kwargs):
        super().__init__(**kwargs)
        self.battler = battler
        self.bind(pos=self.update_display, size=self.update_display)

    def update_display(self, *args):
        self.canvas.clear()
        ELEMENT_ORDER = [Element.FIRE, Element.ICE, Element.LIGHTNING, Element.POISON, Element.RADIATION]
        with self.canvas:
            x_cursor = self.x
            spacing = dp(2)
            dot_size = self.height
            for element in ELEMENT_ORDER:
                stacks = self.battler.element_stacks.get(element, 0)
                if stacks > 0:
                    color_name = element.name.lower()
                    color = DAMAGE_COLORS.get(color_name, (1, 1, 1, 1))
                    Color(*color)
                    for _ in range(stacks):
                        if x_cursor + dot_size <= self.right:
                            Ellipse(pos=(x_cursor, self.y), size=(dot_size, dot_size))
                            x_cursor += dot_size + spacing

class BattlerPanel(BoxLayout):
    def __init__(self, battler, **kw):
        # Reduced spacing for a tighter look between bars and sprite
        super().__init__(orientation="vertical", spacing=dp(4), size_hint=(None, None), **kw)
        self.battler = battler
        
        def _wrap(widget):
            # This helper remains useful
            box = AnchorLayout(size_hint=(1, None), height=widget.height)
            box.add_widget(widget)
            return box

        # Determine properties based on battler type
        if battler.is_enemy:
            sprite_path = f"images/enemies/{battler.enemy_id}_combat.png"
            tier = getattr(battler, "tier", "standard")
            # --- NEW: Tier-based sprite scaling ---
            if tier == "boss":
                base_size = dp(112 * SPRITE_SCALE)
            elif tier == "mini": # Minibosses
                base_size = dp(96 * SPRITE_SCALE)
            elif tier == "elite":
                base_size = dp(88 * SPRITE_SCALE)
            else: # standard
                base_size = dp(80 * SPRITE_SCALE)
            spr_w, spr_h = (base_size, base_size)
            bar_width = spr_w * 0.45
            hp_color, atb_color = ((1, .25, .25, 1), (0.25, 0.55, 1.0, 1))
        else:  # is player
            sprite_path = f"images/characters/{battler.id}_combat.png"
            spr_w = spr_h = dp(48 * SPRITE_SCALE)
            bar_width = spr_w * 0.8
            hp_color, atb_color = ((0.25, 1.0, 0.25, 1), (0.25, 0.55, 1.0, 1))

        # Create all the widgets we might need
        sprite_wrapper = AnchorLayout(size_hint_y=None, height=spr_h)

        # --- NEW: Add contact shadow ---
        # This is a simple widget with an Ellipse drawn on its canvas.
        # It's added to the sprite_wrapper BEFORE the sprite itself.
        shadow = Widget(size_hint=(None, None))
        with shadow.canvas:
            Color(0, 0, 0, 0.65) # Dark, semi-transparent color for the shadow
            # Define the oval shape. Width is based on sprite size, height is much smaller.
            shadow_w = spr_w * 0.6
            shadow_h = shadow_w * 0.25
            self.shadow_ellipse = Ellipse(pos=(0,0), size=(shadow_w, shadow_h))
        # We'll position this shadow relative to the sprite later.
        sprite_wrapper.add_widget(shadow)
        # --- END NEW ---

        self.sprite = ClickableImage(source=sprite_path, size=(spr_w, spr_h), size_hint=(None, None))
        self.sprite.bind(pos=lambda *a: self._update_shadow_pos()) # Keep shadow position synced
        sprite_wrapper.add_widget(self.sprite)

        # Made bars slightly thinner for aesthetics
        self.atb = Gauge(100, atb_color, width=bar_width, height=dp(18), radius=[dp(9)])
        self.hp = Gauge(battler.total_max_hp, hp_color, width=bar_width, height=dp(18), radius=[dp(9)])
        self.hp.set(battler.hp)

        # NOW, add widgets in the correct order based on type
        if battler.is_enemy:
            # Bars and stacks first, so they appear ABOVE the sprite
            self.add_widget(_wrap(self.hp))
            self.add_widget(_wrap(self.atb))
            self.stack_disp = ElementStackDisplay(battler, size_hint=(None, None), size=(spr_w, dp(8)))
            self.add_widget(self.stack_disp)
            # --- MODIFIED: Reduce spacer height to bring bars closer to the sprite ---
            spacer = Widget(size_hint=(1, None), height=dp(-72))
            self.add_widget(spacer)
            # Then the sprite
            self.add_widget(sprite_wrapper)
        else:  # is player
            # Sprite first, so it appears ABOVE the bars
            self.add_widget(sprite_wrapper)
            # Then name, bars, and HP text below the sprite
            lbl = Label(text=battler.name.upper(), font_name=fonts["combat_name"]["name"],
                        font_size=fonts["combat_name"]["size"], size_hint_y=None,
                        height=sp(24))
            # --- MODIFIED: Convert to ShadowLabel for outline ---
            lbl = ShadowLabel(text=battler.name.upper(), font_name=fonts["combat_name"]["name"], font_size=fonts["combat_name"]["size"],
                              size_hint_y=None, height=sp(24), use_outline=True, outline_color=(0,0,0,0.8),
                              outline_width=dp(1.5), outline_offset=(0, dp(-4)), halign='center',
                              align_to_text=True)
            lbl.bind(width=lambda inst, w: setattr(inst, 'text_size', (w, None)))
            self.name_lbl = lbl
            self.add_widget(lbl)
            self.add_widget(_wrap(self.atb))
            self.add_widget(_wrap(self.hp))
            # --- NEW: Add vertical spacing between HP bar and HP text ---
            spacer = Widget(size_hint_y=None, height=dp(4))
            self.add_widget(spacer)
            # --- MODIFIED: Convert to ShadowLabel for outline ---
            self.hp_txt = ShadowLabel(text=f"{battler.hp}/{battler.total_max_hp}", font_name=fonts["combat_hp"]["name"],
                                font_size=fonts["combat_hp"]["size"], size_hint=(None, None),
                                width=spr_w, height=sp(12), halign="center", valign="top", use_shadow=False,
                                use_outline=True, outline_color=(0,0,0,0.8), outline_width=dp(1.5), outline_offset=(0, dp(12)),
                                align_to_text=True)
            self.hp_txt.text_size = (spr_w, None)
            self.add_widget(_wrap(self.hp_txt))

        # Common final setup
        self.bind(minimum_width=self.setter("width"), minimum_height=self.setter("height"))
        self.sprite_color = self.sprite.color[:]

    def _update_shadow_pos(self, *args):
        """Center the shadow ellipse under the sprite."""
        if not hasattr(self, 'shadow_ellipse'):
            return
        
        shadow_w, shadow_h = self.shadow_ellipse.size

        if not self.battler.is_enemy:
            # --- NEW: Hide the shadow for player characters ---
            # Setting the size to (0, 0) effectively makes it invisible.
            self.shadow_ellipse.size = (0, 0)
        else:
            # --- Keep original shadow logic for enemies ---
            y_offset = dp(4)
            # Center the shadow horizontally with the sprite, and place it at the sprite's "feet".
            self.shadow_ellipse.pos = (self.sprite.center_x - shadow_w / 2,
                                       self.sprite.y - shadow_h / 2 + y_offset)

    def update_stack_display(self):
        if hasattr(self, "stack_disp"): self.stack_disp.update_display()

    def flash_damage(self):
        # This line is now corrected to only return if flashes are disabled.
        app = App.get_running_app()
        if not getattr(app, 'settings', {}).get('flashes', True):
            return
        Animation.cancel_all(self.sprite, 'color')
        self.sprite.color = self.sprite_color[:]
        (Animation(color=[1, 1, 1, 1], d=0.07) + Animation(color=self.sprite_color, d=0.2)).start(self.sprite)

    def set_ready(self, flag: bool):
        if hasattr(self, 'name_lbl'):
            Animation.cancel_all(self.name_lbl, 'color')
            if flag:
                pulse = (Animation(color=(1, 1, 0.2, 1), d=0.18) + Animation(color=(1, 1, 1, 1), d=0.18))
                pulse.repeat = True
                pulse.start(self.name_lbl)
            else:
                self.name_lbl.color = (1, 1, 1, 1)

class ColoredBar(Widget):
    max_value = NumericProperty(1)
    value     = NumericProperty(0)
    def __init__(self, max_value, value, color, **kwargs):
        super().__init__(size_hint=(None,None), **kwargs)
        self.max_value, self.value = max_value, value
        with self.canvas:
            Color(*color)
            self._rect = Rectangle(pos=self.pos, size=(0, self.height))
        self.bind(pos=self._upd, size=self._upd,
                  value=self._upd, max_value=self._upd)
    def _upd(self, *a):
        ratio = (self.value/self.max_value) if self.max_value else 0
        ratio = min(max(ratio,0),1)
        self._rect.pos  = self.pos
        self._rect.size = (self.width*ratio, self.height)

class BattleScreen(Screen):
    def __init__(self, sm, players: List[Character], enemies: List[Enemy],
                 *,
                 audio:       SoundManager,
                 on_victory:  Callable = lambda *_: None,
                 on_defeat:   Callable = lambda *_: None,
                 background_override: str = None,
                 formation: list[str] | None = None,
                 environment_data: dict | None = None, # This is no longer used for the main BG
                 final_bg_texture: Texture = None,     # This is the new primary background
                 **kwargs):
        super().__init__(name="battle", **kwargs)
        
        # MODIFIED: Set initial opacity to 0 to allow for the fade-in animation.
        self.opacity = 0
        
        # --- CORE INITIALIZATION ---
        self.game = App.get_running_app().current_game
        self.resonance_max = self.game.max_resonance
        self.sm, self.audio = sm, audio
        self.players = players[:]
        # To ensure enemies always start a battle with full health and no
        # lingering state, we re-load them from their ID.
        self.enemies = [load_enemy(enemy.id) for enemy in enemies]
        self.on_victory, self.on_defeat = on_victory, on_defeat

        # --- NEW: Living Combat Background ---
        # This uses the new, quieter, theme-locked background.
        # It requires the full room and themes dictionaries to derive its palette.
        self.cbg = CombatBackground(
            room=self.game.current_room.__dict__ if hasattr(self.game.current_room, '__dict__') else {},
            themes_dict=self.game.themes._schemes if hasattr(self.game.themes, '_schemes') else {},
            snapshot_tex=final_bg_texture,
            enable_parallax=True
        )
        self.add_widget(self.cbg, index=0)
        
        # --- Ambient weather layer (above BG, below UI) ---
        self._weather = WeatherLayer(mode='none', intensity=0.7, size_hint=(1,1))
        self.add_widget(self._weather, index=max(0, len(self.children)-1))
        self.bind(size=self._weather.setter('size'), pos=self._weather.setter('pos'))

        # --- Subtle vignette/letterbox to quiet edges (above weather, below UI) ---
        self._vignette = Widget(size_hint=(1,1))
        with self._vignette.canvas:
            Color(0, 0, 0, 0.12)   # top bar
            self._vig_top = Rectangle(pos=(0, 0), size=(1, 1))
            Color(0, 0, 0, 0.15)   # bottom bar a bit stronger near party
            self._vig_bot = Rectangle(pos=(0, 0), size=(1, 1))
        self.add_widget(self._vignette, index=max(0, len(self.children)-1))

        def _layout_vignette(*_):
            # This function is now empty as the vignette is disabled.
            pass

        self.bind(size=_layout_vignette, pos=_layout_vignette)
        _layout_vignette()

        try:
            # --- THIS IS THE FIX: Directly get weather from the current room ---
            app = App.get_running_app()
            room = getattr(app.current_game, 'current_room', None)
            weather_mode = getattr(room, 'weather', 'none') if room else 'none'
            self._weather.set_mode(weather_mode)
        except Exception as e:
            print(f"[DEBUG] BattleScreen: Error setting weather: {e}")
            self._weather.set_mode('none')

        # --- FIX: Ensure weapon_type is set on characters when battle starts ---
        for player in self.players:
            weapon = player.equipment.get('weapon')
            if weapon and hasattr(weapon, 'equipment'):
                weapon_type = weapon.equipment.get('weapon_type')
                if weapon_type:
                    player.weapon_type = weapon_type
                elif hasattr(player, 'weapon_type'):
                    del player.weapon_type
            elif hasattr(player, 'weapon_type'):
                del player.weapon_type
        
        # --- The rest of your __init__ method is perfect and unchanged ---
        self._p_sprites = []
        self._order, self._turn_idx, self._active = [], -1, None
        self.action_in_progress = False
        self._enemy_turn_queue = set()
        self._action_queue = [] # NEW: Unified queue for player and enemy actions.
        self.auto_battle_active = False
        self._tap_window_active = False
        self._timed_bonus = 1.0
        self._pending_action = None
        self._pending_action_type = None
        self.action_chosen = False

        self._target_brackets = None
        self._target_panel = None
        self._focused_player = None

        # --- UI SETUP ---
        root = BoxLayout(orientation="vertical", padding=[ss(2), ss(2), ss(2), ss(12)], spacing=ss(2))
        self.add_widget(root)
        
        self._fx_layer = FloatLayout(size_hint=(1, 1))
        self.add_widget(self._fx_layer)

        # ENEMY LAYOUT LOGIC
        enemy_container = FloatLayout(size_hint=(1, 0.4), pos_hint={'top': 1})
        root.add_widget(enemy_container)

        if formation and len(formation) == len(self.enemies):
            enemy_formation = formation
        else:
            enemy_formation = ['front' if i % 2 == 0 else 'back' for i in range(len(self.enemies))]

        self._enemy_panels = [None] * len(self.enemies)
        num_enemies = len(self.enemies)
        # Increase this value to space enemies out more horizontally.
        spacing_factor = 0.22
        # Adjust these to change the vertical position of the enemy rows.
        # 0.0 is the bottom, 1.0 is the top of the enemy container.
        y_pos_front = 0.45
        y_pos_back = 0.6
        
        all_panels = []
        for i, enemy in enumerate(self.enemies):
            pan = BattlerPanel(enemy)
            y_pos = y_pos_front if enemy_formation[i] == 'front' else y_pos_back
            center_x = 0.5 + (i - (num_enemies - 1) / 2.0) * spacing_factor
            pan.pos_hint = {'center_x': center_x, 'center_y': y_pos}
            self._enemy_panels[i] = pan
            pan.sprite.bind(on_release=lambda _btn, e=enemy: self._on_enemy_click(e))
            all_panels.append((pan, enemy_formation[i]))

        for pan, row in all_panels:
            if row == 'back':
                enemy_container.add_widget(pan)
        for pan, row in all_panels:
            if row == 'front':
                enemy_container.add_widget(pan)

        # The message label is now added to the BattleScreen's FloatLayout, not the root BoxLayout
        self.msg_lbl = Label(size_hint=(1, None),
                             pos_hint={'center_x': 0.5, 'y': 0.5}, # Position it in the overlay
                             font_name=fonts["combat_message"]["name"],
                             halign="center", valign="middle", markup=True,
                             font_size=fonts["combat_message"]["size"])
        self.msg_lbl.bind(width=lambda *x: self.msg_lbl.setter('text_size')(self.msg_lbl, (self.msg_lbl.width, None)))
        self.msg_lbl.bind(texture_size=lambda instance, value: setattr(instance, 'height', value[1]))
        # Add it to the screen itself (which is a FloatLayout) instead of the BoxLayout
        self.add_widget(self.msg_lbl)

        # --- FIX: Define party container before it is used ---
        party_container = AnchorLayout(anchor_x="center", anchor_y="center", size_hint=(1, 0.48))
        root.add_widget(party_container)
        party = FloatLayout(size_hint=(1, 1))
        party_container.add_widget(party)

        self._party_panels = []
        # --- NEW: Add a spacer between the party and the resonance bar ---
        spacer = Widget(size_hint_y=None, height=dp(12))
        root.add_widget(spacer)

        _shape_pos = {
            1: [(.5, .5)],
            2: [(.3, .28), (.7, .28)],
            3: [(.3, .28), (.7, .28), (.3, .76)],
            4: [(.3, .76), (.7, .76), (.3, .28), (.7, .28)]
        }
        coords = _shape_pos.get(len(self.players), _shape_pos[4])
        for idx, pl in enumerate(self.players):
            pan = BattlerPanel(pl)
            pan.pos_hint = {"center_x": coords[idx][0], "center_y": coords[idx][1]}
            party.add_widget(pan)
            self._party_panels.append(pan)
            pan.sprite.bind(on_release=lambda _b, i=idx: self._on_player_click(i))


        # --- RESONANCE BAR (now sits just under the enemy sprites) ---
        res_wrap = AnchorLayout(anchor_x="center", anchor_y="center",
                                size_hint=(1, None), height=dp(36),
                                padding=(0, 0, 0, dp(48)))
        self.res_bar = ResonanceBar(self.resonance_max, initial=self.game.resonance, width=dp(480)) # NEW: Make bar longer
        res_wrap.add_widget(self.res_bar)

        # --- NEW: Combat Message Label ---
        # This label is part of the layout, ensuring it's always positioned correctly.
        # We will just change its text and opacity in the say() method.
        self.combat_message_label = ShadowLabel(
            text="", markup=True, opacity=0,
            font_name=fonts["combat_message"]["name"],
            font_size=fonts["combat_message"]["size"] * 1.2,
            halign="center", valign="middle",
            size_hint=(1, None), height=dp(48),
            use_shadow=True, shadow_color=(0,0,0,0.9), shadow_offset=(0,-2)
        )
        # --- FIX: Enable text wrapping and ensure height updates with content ---
        # Bind text_size to the label's width to enable wrapping.
        self.combat_message_label.bind(width=lambda *x: self.combat_message_label.setter('text_size')(self.combat_message_label, (self.combat_message_label.width, None)))
        # Bind the label's height to its texture_size so it grows vertically with wrapped text.
        self.combat_message_label.bind(texture_size=lambda instance, value: setattr(instance, 'height', value[1]))
 
        # Label (tiny, bold, futuristic font)
        # --- MODIFIED: Convert to ShadowLabel for outline ---
        lbl = ShadowLabel(text="RESONANCE", font_name=fonts["section_title"]["name"],
                          font_size=fonts["section_title"]["size"],
                          color=(1, 1, 1, 0.88), size_hint=(1, None), height=dp(24),
                          use_shadow=False, use_outline=True, outline_color=(0, 0, 0, 0.8), outline_width=dp(1.5),
                          halign='center', align_to_text=True, outline_offset=(0, dp(-1.5)))
        lbl.bind(width=lambda inst, w: setattr(inst, 'text_size', (w, None)))
        res_wrap.add_widget(lbl)

        # --- FIX: Create the message container and add it to the layout ---
        self.message_container = AnchorLayout(anchor_x='center', anchor_y='center',
                                              size_hint=(0.8, 0.15), pos_hint={'center_x': 0.5})
        self.message_container.add_widget(self.combat_message_label)
        
        root.add_widget(self.message_container, index=2) # Insert after enemies (index 0) and party (index 1)
        root.add_widget(res_wrap) # Add resonance bar at the end (bottom)

        # --- FINAL SETUP ---
        for w in self.walk(restrict=True):
            if isinstance(w, Label): _pixel_filter(w)

        self._refresh_ui()
        
        all_panels = self._party_panels + [p for p in self._enemy_panels if p is not None]
        
        # --- MODIFIED: Stagger initial enemy ATB gauges ---
        # Players start at 0 for fairness.
        for panel in self._party_panels:
            panel.atb.set(0)
            panel.set_ready(False)

        # Enemies get a small random boost to prevent them from acting in unison.
        for panel in self._enemy_panels:
            if panel:
                panel.atb.set(random.uniform(0, 20)) # Random start from 0% to 20%
                panel.set_ready(False)

        self.game.battle_screen = self
        Clock.schedule_interval(self._update_battle, 1.0 / 60.0)

    def _fx(self, widget):
        # Reuse your existing FX layer so effects render above sprites, below UI
        self._fx_layer.add_widget(widget)  # you already create _fx_layer; keeping consistent
        
    def fx_hit(self, pos, *, crit=False):
        # impact ring + sparks; respect your screenshake setting
        self._fx(ImpactRing(pos=pos, size=(1,1), color=(1,1,1,1 if not crit else 0.85)))
        self._fx(HitSparks(pos=pos))
        if crit:
            # slightly bigger ring for crits
            self._fx(ImpactRing(pos=pos, radius=dp(36), color=(1,1,0.3,0.9)))
        # you already have shake_screen() wired to settings; lean on it
        self.shake_screen(duration=0.16 if crit else 0.12, intensity=dp(6 if crit else 4))  # :contentReference[oaicite:3]{index=3}

    def fx_slash(self, start, end):
        self._fx(SlashArc(start, end))

    def fx_timed_window(self, center):
        self._fx(TimedHitRings(center))

    def _show_target_brackets(self, panel):
        # Remove old
        if self._target_brackets and self._target_brackets.parent:
            self._target_brackets.dismiss()
        # Compute rect around the sprite
        spr = panel.sprite
        rect = (spr.x, spr.y, spr.width, spr.height)
        tb = TargetBrackets(rect=rect)
        self._target_brackets = tb
        self._target_panel = panel
        self._fx(tb)
        # Keep following the sprite (layout changes, shakes, etc.)
        def _follow(dt):
            if not self._target_panel or not self._target_brackets or not self._target_brackets.parent:
                return False
            spr = self._target_panel.sprite
            self._target_brackets.set_rect((spr.x, spr.y, spr.width, spr.height))
            return True
        from kivy.clock import Clock
        Clock.schedule_interval(_follow, 1/30)

    def _panel_for_enemy(self, enemy):
        try:
            idx = self.enemies.index(enemy)
        except ValueError:
            return None
        if idx >= len(self._enemy_panels):
            return None
        return self._enemy_panels[idx]

    def _panel_for_player(self, player):
        try:
            idx = self.players.index(player)
        except ValueError:
            return None
        if idx >= len(self._party_panels):
            return None
        return self._party_panels[idx]

    def _clear_target_brackets(self):
        if self._target_brackets:
            if self._target_brackets.parent:
                self._target_brackets.dismiss()
            self._target_brackets = None
        self._target_panel = None
        self._focused_player = None

    @staticmethod
    def _normalize_item_key(name: str) -> str:
        """Lowercase alphanumeric version of an item name for fuzzy matches."""
        return "".join(ch for ch in name.lower() if ch.isalnum())

    def _resolve_inventory_item(self, raw_name: str):
        """Attempt to locate an item definition for an inventory entry."""
        game = App.get_running_app().current_game
        bag = getattr(game, "all_items", [])
        if not bag:
            return None

        item = bag.find(raw_name)
        if item:
            return item

        target_key = self._normalize_item_key(raw_name)
        for candidate in bag:
            if self._normalize_item_key(candidate.name) == target_key:
                return candidate
            for alias in getattr(candidate, "aliases", []):
                if self._normalize_item_key(alias) == target_key:
                    return candidate
        return None

    def _ensure_inventory_entry(self, item_obj, inventory_key: str | None) -> str | None:
        """Delegate to the game's inventory normalizer."""
        game = App.get_running_app().current_game
        if not game:
            return None
        return game._ensure_inventory_entry(item_obj, inventory_key)

    def _on_enemy_click(self, enemy):
        panel = self._panel_for_enemy(enemy)
        if panel:
            self._show_target_brackets(panel)
        if self._pending_action:
            # --- FIX: Clear the pending action *before* executing it. ---
            # This prevents the action from being re-triggered on subsequent clicks
            # if the execution involves animations or delays.
            action_to_run, self._pending_action = self._pending_action, None
            self._pending_action_type = None
            action_to_run(enemy)

    def _update_bg_rect(self, instance, value):
        # This function is now empty as the background is handled by the Image widget in the viewport
        pass

    def shake_screen(self, duration=0.2, intensity=dp(5)):
         """Applies a screen shake effect, respecting game settings."""
         if not App.get_running_app().settings.get('screenshake', True):
             return
 
         original_pos = self.pos[:]
         
         # A more intense, shorter shake
         anim = (
             Animation(x=original_pos[0] + intensity, y=original_pos[1] - intensity, duration=duration / 4, t='out_quad') +
             Animation(x=original_pos[0] - intensity, y=original_pos[1] + intensity, duration=duration / 4, t='in_quad') +
             Animation(x=original_pos[0] + intensity / 2, y=original_pos[1] - intensity / 2, duration=duration / 4, t='out_quad') +
             Animation(pos=original_pos, duration=duration / 4, t='in_out_elastic')
         )
         
         # Prevent stacking shakes
         Animation.cancel_all(self, 'x', 'y')
         anim.start(self)

    def on_enter(self, *args):
        # ADDED: Fade the entire battle screen (UI and all) into view.
        Animation(opacity=1, duration=0.7, t='in_quad').start(self)
        super().on_enter(*args)

    def on_pre_enter(self, *args):
        self.game = App.get_running_app().current_game
        if self.game:
            self.game.pause_enemy_timers()

    def on_leave(self, *_):
        app = App.get_running_app()
        app.tx_mgr.close()

        sm = app.screen_manager
        if sm:
            old = sm.transition
            sm.transition = FadeTransition(duration=1.0)
            sm.current = "explore"
            Clock.schedule_once(lambda *_: setattr(sm, "transition", old), 0)

    def on_touch_down(self, touch):
        """Monitors for screen taps during the timed-hit window."""
        if self._tap_window_active:
            self.say("[color=00ff00]Perfect![/color]")
            self._timed_bonus = 1.5
            self._end_tap_window()
            return True
        return super().on_touch_down(touch)
    
    def _start_tap_window(self, duration=0.25):
        """Opens the window for a timed hit/guard."""
        self._tap_window_active = True
        Clock.schedule_once(self._end_tap_window, duration)

    def _end_tap_window(self, *args):
        """Closes the timed input window."""
        self._tap_window_active = False
    
    def _reset_order(self):
        self._order = [p for p in self.players if p.hp > 0] + [e for e in self.enemies if e.hp > 0]
        if not self._order: self._order = self.players + self.enemies

    def _refresh_ui(self):
        for i, enemy in enumerate(self.enemies):
            pan = self._enemy_panels[i]
            if pan:
                pan.hp.set(enemy.hp)
                if hasattr(pan, 'hp_txt'):
                    pan.hp_txt.text = f"{enemy.hp}/{enemy.total_max_hp}"
        for pan, hero in zip(self._party_panels, self.players):
            pan.hp.set(hero.hp)
            if hasattr(pan, 'hp_txt'):
                pan.hp_txt.text = f"{hero.hp}/{hero.total_max_hp}"

    def say(self, msg):
        """Display a stylized, animated message in the center of the screen."""
        # Cancel any previous animation on the label
        Animation.cancel_all(self.combat_message_label)

        # Set the text and make it visible
        self.combat_message_label.text = msg
        self.combat_message_label.opacity = 1

        # Animate it fading out after a delay
        anim = Animation(duration=1.2) + Animation(opacity=0, duration=0.3, t='in_quad')
        
        # Clear the text after it fades out
        anim.bind(on_complete=lambda *a: setattr(self.combat_message_label, 'text', ''))
        anim.start(self.combat_message_label)

    def _spawn_damage_popup(self, sprite: Widget, amount: int, dtype: str = "physical"):
        """Show a floating damage number over *sprite*."""
        color = DAMAGE_COLORS.get(dtype.lower(), DAMAGE_COLORS["physical"])
        lbl = DamageLabel(str(amount), color=color)
        sx, sy = sprite.to_window(sprite.center_x, sprite.top)
        x, y = self._fx_layer.to_widget(sx, sy)
        lbl.center = (x + dp(8), y + dp(-4))
        self._fx_layer.add_widget(lbl)
        lbl.bounce(self._fx_layer)

    # --- NEW: Method to show weapon attack effects ---
    def _show_attack_effect(self, target_sprite: Widget, weapon_type: str):
        """
        Displays and animates a weapon's attack effect image over a target.
        The image fades in, shakes, holds, and then fades out.
        """
        # 1. Check if the weapon type is in our map and get the image path.
        #    This ensures that if a character has no weapon_type or an unknown one,
        #    the game doesn't crash.
        if weapon_type not in ATTACK_EFFECT_MAP:
            return
        image_path = ATTACK_EFFECT_MAP[weapon_type]

        # 2. Create the Image widget for the effect.
        effect_image = Image(
            source=image_path,
            size_hint=(None, None),
            size=(target_sprite.width * 0.7, target_sprite.height * 0.7), # Sized relative to the target
            allow_stretch=True,
            keep_ratio=True,
            opacity=0 # Start invisible to fade in smoothly
        )

        # 3. Position the effect over the center of the target's sprite.
        #    We convert the target's coordinates to window space, then to our FX layer's space.
        sx, sy = target_sprite.to_window(target_sprite.center_x, target_sprite.center_y)
        effect_image.center = self._fx_layer.to_widget(sx, sy)
        
        # 4. Add the effect image to the top-level effects layer.
        self._fx_layer.add_widget(effect_image)

        # 5. Define the animation sequence.
        #    Fade in quickly.
        anim = Animation(opacity=1, duration=0.05)

        #    Shake animation: a series of small, rapid movements.
        intensity = dp(4)
        shake_duration = 0.04
        shake_sequence = (
            Animation(center_x=effect_image.center_x + intensity, center_y=effect_image.center_y - intensity, duration=shake_duration) +
            Animation(center_x=effect_image.center_x - intensity, center_y=effect_image.center_y + intensity, duration=shake_duration) +
            Animation(center_x=effect_image.center_x, center_y=effect_image.center_y, duration=shake_duration)
        )
        #    --- FIX: Repeat the shake by chaining the animation, not multiplying. ---
        anim += shake_sequence + shake_sequence

        #    Hold the image for a moment so the player can see it.
        anim += Animation(duration=0.1)

        #    Fade out.
        anim += Animation(opacity=0, duration=0.2)

        # 6. Define a cleanup function to remove the widget after the animation.
        def _cleanup(*_):
            if effect_image.parent:
                effect_image.parent.remove_widget(effect_image)

        anim.bind(on_complete=_cleanup)
        
        # 7. Start the full animation sequence.
        anim.start(effect_image)

    def _clear_msg(self, *_): self.msg_lbl.text = ""
    def _on_player_click(self, idx: int) -> None:
        player, panel = self.players[idx], self._party_panels[idx]
        self._focused_player = player
        if self.action_in_progress:
            # --- FIX: Prevent players from targeting other players with an attack ---
            # If the pending action is `_execute_attack`, it means we are in "Attack" mode.
            # In this mode, we should only be able to target enemies, not other players.
            if self._pending_action_type == "attack":
                self.say("[i]You must target an enemy.[/i]")
                return
            if self._pending_action:
                action_to_run = self._pending_action
                self._pending_action = None
                self._pending_action_type = None
                action_to_run(player)
            return
        if not player.is_alive() or panel.atb.curr < panel.atb.max_val:
            return
        self.action_in_progress = True
        self._active = player
        self.say(f"[b]{self._active.name.upper()}[/b]'s turn.")
        self._show_action_menu(player)

    def _show_action_menu(self, player):
        actions = [
            ("Attack", partial(self.on_attack, player)),
            ("Skill",  partial(self.on_skill, player)),
            ("Item",   partial(self.on_item, player)),
            ("Defend", partial(self.on_defend, player)),
            ("Auto-Battle", partial(self.toggle_auto_battle, player))
        ]
        game = App.get_running_app().current_game
        popup = MenuPopup(player.name.upper(), actions=actions, size_hint=(0.6, 0.6), autoclose=True, theme_mgr=game.themes)
        popup.bind(on_dismiss=lambda *_: self._cancel_action())
        game._style_popup(popup)
        popup.open()

    def _cancel_action(self, *args):
        """
        This function is now ONLY for explicit cancellations (e.g., closing the menu).
        It should not be called when an action has been chosen and is waiting for a target.
        """
        # If an action has been chosen (like 'Attack' or 'Skill'), the turn is
        # proceeding. We should not cancel it. The turn will end via _after_action.
        if self.action_chosen:
            return

        # If no action was chosen, it's a true cancellation. Reset the state fully.
        self.action_in_progress = False
        self._active = None
        # --- FIX: Also reset the pending action and action_chosen flag. ---
        # This ensures that if the player opens and closes the action menu
        # without choosing anything, the state is fully reset.
        self._pending_action = None
        self._pending_action_type = None
        self._focused_player = None
        self._is_ending_turn = False
        self._battle_is_over = False
        # ----------------------------------------------------------------
        self._clear_target_brackets()
        self._refresh_ui()

    def _deferred_cancel_action(self, dt):
        """
        DEPRECATED. The logic has been moved into _cancel_action for clarity.
        This method is no longer called but is kept to prevent crashes if
        any old bindings still reference it. It now does nothing.
        """
        pass
        
    def toggle_auto_battle(self, actor=None, *args):
        self.auto_battle_active = not self.auto_battle_active
        self.say(f"Auto-Battle [color=00ff00]Enabled[/color]" if self.auto_battle_active else f"Auto-Battle [color=ff0000]Disabled[/color]")
        actor = actor or self._active or self._focused_player
        if self.auto_battle_active and isinstance(actor, Character):
            self._active = actor
            self.action_chosen = True
            Clock.schedule_once(lambda dt, p=actor: self._execute_auto_action(dt, p), 0)
    
    def _apply_overtime(self, battler):
        if battler.has_status(Status.BURN):
            dmg = max(1, battler.max_hp//16)
            battler.hp = max(0, battler.hp - dmg)
            panel = (self._enemy_panels[self.enemies.index(battler)] if isinstance(battler, Enemy) else self._party_panels[self.players.index(battler)])
            self._spawn_damage_popup(panel.sprite, dmg, "burn")
            self.say(f"{battler.name} is scorched by flames!")
        if battler.has_status(Status.POISON):
            dmg = max(1, battler.max_hp//10)
            battler.hp = max(0, battler.hp - dmg)
            panel = (self._enemy_panels[self.enemies.index(battler)] if isinstance(battler, Enemy) else self._party_panels[self.players.index(battler)])
            self._spawn_damage_popup(panel.sprite, dmg, "poison")
            self.say(f"{battler.name} suffers poison damage!")
        if battler.has_status(Status.REGEN):
            healed = max(1, battler.max_hp//12)
            battler.hp = min(battler.max_hp, battler.hp + healed)
            self.say(f"{battler.name} regenerates {healed} HP!")

    STACK_CHANCES = { Element.FIRE: 0.25, Element.ICE: 0.25, Element.LIGHTNING: 0.30, Element.POISON: 0.30, Element.RADIATION: 0.20, }
    STACK_FLAVOUR = { Element.FIRE: "catches fire!", Element.ICE: "starts to freeze!", Element.LIGHTNING: "crackles with electricity!", Element.POISON: "is envenomed!", Element.RADIATION: "begins to glow!", }

    def _apply_element_stack(self, target: Enemy, element: Element) -> None:
        if not isinstance(target, Enemy) or element == Element.NONE: return
        current = target.element_stacks.get(element, 0)
        if current == 0:
            if random.random() > self.STACK_CHANCES.get(element, 0.25): return
            current = 1
            self.say(f"{target.name} {self.STACK_FLAVOUR.get(element, 'is affected!')}")
        else:
            current += 1
        if current >= 3:
            self._trigger_final_effect(target, element)
            target.reset_element_stack(element)
        else:
            target.element_stacks[element] = current
        try:
            panel = self._enemy_panels[self.enemies.index(target)]
            panel.update_stack_display()
        except (ValueError, IndexError): pass

    def _trigger_final_effect(self, target: Enemy, element: Element) -> None:
        if element == Element.FIRE:
            self.say(f"{target.name} explodes in flames!")
            dmg = max(1, target.max_hp // 10)
            others = [e for e in self.enemies if e is not target and e.is_alive()]
            for en in others:
                en.hp = max(0, en.hp - dmg)
                panel = self._enemy_panels[self.enemies.index(en)]
                panel.flash_damage()
                self._spawn_damage_popup(panel.sprite, dmg, "burn")
                panel.update_stack_display()
        elif element == Element.ICE:
            target.add_status(Status.FREEZE, 2)
            self.say(f"{target.name} is frozen solid!")
        elif element == Element.LIGHTNING:
            dmg = max(1, target.max_hp // 10)
            target.hp = max(0, target.hp - dmg)
            target.add_status(Status.SHOCK, 1)
            panel = self._enemy_panels[self.enemies.index(target)]
            # --- FIX: Check if panel exists before using it ---
            if not panel:
                self._after_action(self._active)
                return
            panel.flash_damage()
            self._spawn_damage_popup(panel.sprite, dmg, "shock")
            self.say(f"{target.name} is shocked and can't act!")
        elif element == Element.POISON:
            target.add_status(Status.POISON, 99)
            self.say(f"{target.name} is severely poisoned!")
        elif element == Element.RADIATION:
            self.say(f"{target.name} melts down in radiation!")
            dmg = max(1, target.max_hp // 10)
            all_targets = [b for b in self.players + self.enemies if b is not target and b.is_alive()]
            for battler in all_targets:
                battler.hp = max(0, battler.hp - dmg)
                if isinstance(battler, Enemy):
                    if random.random() <= self.STACK_CHANCES[Element.RADIATION]:
                        battler.element_stacks[Element.RADIATION] = 1
                panel = (self._enemy_panels[self.enemies.index(battler)] if isinstance(battler, Enemy) else self._party_panels[self.players.index(battler)])
                panel.flash_damage()
                self._spawn_damage_popup(panel.sprite, dmg, "radiation")
                if isinstance(battler, Enemy):
                    panel.update_stack_display()

    def _queue_action(self, action, *args, unlock=False):
        """Append an action to the unified queue."""
        self._action_queue.append((action, args))
        if unlock:
            # Allow the queue processor to pick up the newly queued action.
            self.action_in_progress = False

    def _update_battle(self, dt):
        """Main update loop, runs every frame."""
        # --- FIX: Manually update the combat background animation. ---
        if hasattr(self, 'cbg') and self.cbg:
            self.cbg.update(dt)

        if self.action_in_progress: return
        all_battlers = self.players + self.enemies
        for i, battler in enumerate(all_battlers):
            panel = self._party_panels[i] if i < len(self.players) else self._enemy_panels[i - len(self.players)]
            if not panel or not battler.is_alive() or panel.atb.curr >= panel.atb.max_val: continue
            old_atb = panel.atb.curr
            panel.atb.set(old_atb + battler.total_spd * dt)
            if panel.atb.curr >= panel.atb.max_val:
                panel.set_ready(True)
                panel.atb.ready_flash() # <-- ADD THIS LINE
                self.audio.play_sfx("blip_ready")
                if isinstance(battler, Enemy) and battler not in self._enemy_turn_queue:
                    # --- REVISED: Enemy AI Decision ---
                    # When an enemy is ready, it decides its action and adds it to the
                    # main action queue after a short "thinking" delay.
                    self._enemy_turn_queue.add(battler)
                    delay = random.uniform(0.5, 1.2) # Staggered thinking time
                    Clock.schedule_once(lambda dt, b=battler: self._enemy_decide_action(b), delay)

        # --- NEW: Action Queue Processor ---
        # If no action is currently animating and there's an action waiting in the queue,
        # execute the next action.
        if not self.action_in_progress and self._action_queue:
            action, args = self._action_queue.pop(0)
            action(*args) # Execute the action (e.g., self._execute_attack(attacker, target))

        # --- REVISED: Auto-battle logic now also uses the action queue ---
        elif not self.action_in_progress and self.auto_battle_active:
            for player, panel in zip(self.players, self._party_panels):
                if player.is_alive() and panel.atb.curr >= panel.atb.max_val:
                    # --- FIX: Explicitly pass the acting player to the auto-action method. ---
                    # This avoids relying on the volatile `self._active` state.
                    Clock.schedule_once(lambda dt, p=player: self._execute_auto_action(dt, p), 0.1)
                    break

    def _enemy_decide_action(self, enemy: Enemy):
        """Enemy AI logic. Decides action and adds it to the main queue."""
        self._enemy_turn_queue.discard(enemy)
        if not enemy.is_alive():
            return

        # Simple AI: always attack a random living player.
        living_players = [p for p in self.players if p.is_alive()]
        if living_players:
            target = random.choice(living_players)
            # Add the chosen action to the unified queue.
            self._queue_action(self._execute_attack, enemy, target)

    def _resolve_enemy_attack(self, enemy, target_player):
        self.shake_screen()
        self.audio.play_sfx("hit")
        # This now calls the main, unified attack resolution function.
        # All detailed calculations (hit, crit, guard, damage, etc.) are handled there.
        self._resolve_attack(enemy, target_player)
        # The original _after_action() call is removed from here because _resolve_attack now handles it.

    def _execute_auto_action(self, dt, player=None):
        """Auto-battle AI for a single player character."""
        # --- FIX: The `player` is now passed directly and is guaranteed to be the correct actor. ---
        # We no longer fall back to `self._active`.
        if not player or not player.is_alive():
            # If the player was defeated between scheduling and execution, do nothing.
            return

        # Simple AI: Use last action or default to attack.
        action = getattr(player, 'last_action', None) or {'type': 'attack'}
        action_type = action.get('type', 'attack')
        unlock = self.action_in_progress
        if action_type == 'attack':
            target = action.get('target')
            if not target or not target.is_alive():
                target = random.choice([e for e in self.enemies if e.is_alive()]) if self.enemies else None
            # --- FIX: Only queue the attack if a valid target was found. ---
            if target:
                self._queue_action(self._execute_attack, player, target, unlock=unlock)
                if unlock:
                    self.action_chosen = False
        elif action_type == 'defend':
            self._queue_action(self._execute_defend, player, unlock=unlock)
            if unlock:
                self.action_chosen = False
        else: # Fallback
            self._queue_action(self._execute_defend, player, unlock=unlock)
            if unlock:
                self.action_chosen = False

    def on_attack(self, actor=None, *_) -> None:
        # Set a pending action that waits for a target, then adds the
        # final action to the main queue.
        actor = actor or self._active or self._focused_player
        if not actor or not actor.is_alive():
            self.say("[i]No one is ready to attack.[/i]")
            return
        self._active = actor
        self.action_in_progress = True
        self.action_chosen = True

        self._pending_action_type = "attack"
        self._pending_action = lambda target, _actor=actor: self._commit_attack_target(_actor, target)
        self.say("Choose a target.")

    def _commit_attack_target(self, actor, target):
        """Finalize target selection for a basic attack and enqueue it."""
        if not actor or not actor.is_alive():
            # Actor was defeated or otherwise invalidated before targeting completed.
            self.action_in_progress = False
            self.action_chosen = False
            self._pending_action = None
            self._pending_action_type = None
            return

        if not target or not target.is_alive():
            # Keep waiting for a valid target.
            self.say("[i]Choose a living target.[/i]")
            self._pending_action = lambda new_target, _actor=actor: self._commit_attack_target(_actor, new_target)
            self._pending_action_type = "attack"
            return

        panel = self._panel_for_enemy(target)
        if panel:
            self._show_target_brackets(panel)

        self._pending_action = None
        self._pending_action_type = None
        self.action_chosen = False
        self._focused_player = None
        self._queue_action(self._execute_attack, actor, target, unlock=True)

    # 2. UPDATE the on_defend method to pass the active character.
    def on_defend(self, actor=None, *_) -> None:
        """Queues a defend action for the currently active player."""
        actor = actor or self._active or self._focused_player
        if not isinstance(actor, Character) or not actor.is_alive():
            self.say("[i]No one is ready to defend.[/i]")
            return

        self._active = actor
        self.action_in_progress = True
        self.action_chosen = True  # Mark that an action was taken

        def _commit_defend(_dt):
            if not actor or not actor.is_alive():
                self.action_in_progress = False
                self.action_chosen = False
                return
            self.action_chosen = False
            self._pending_action = None
            self._pending_action_type = None
            self._focused_player = None
            self._queue_action(self._execute_defend, actor, unlock=True)

        Clock.schedule_once(_commit_defend, 0)

    def _execute_defend(self, actor: Character):
        """Executes the defend action for a character."""
        if not actor or not actor.is_alive(): return
        self._active = actor
        self.action_in_progress = True
        actor.last_action = {'type': 'defend'}
        actor.defending = True
        self.audio.play_sfx("block")
        self.say(f"{actor.name.upper()} braces for impact.")
        self._after_action(actor)
    def on_skill(self, actor=None, *args):
        """Handles the 'Skill' action from the combat menu."""
        actor = actor or self._active or self._focused_player
        if not isinstance(actor, Character) or not actor.is_alive():
            self.say("[i]No one is ready to use a skill.[/i]")
            return
        self._active = actor
        self.action_in_progress = True
        self.action_chosen = True
        skills = getattr(actor, "usable_skills", [])
        if not skills:
            self.say("[i]No skills available.[/i]")
            self._cancel_action() # No action can be taken, so cancel the turn.
            return
        actions = []
        for skill_id in skills:
            node = self.game._get_skill_node(skill_id)
            if node:
                skill_name = node.get("name", skill_id.replace('_',' ').title())
                actions.append((skill_name, partial(self._select_skill_target, skill_id)))
        
        popup = MenuPopup("Use Skill", actions=actions, size_hint=(0.6, 0.6), theme_mgr=self.game.themes, autoclose=True)
        popup.bind(on_dismiss=lambda *_: self._cancel_action())
        self.game._style_popup(popup)
        popup.open()

    def on_item(self, actor=None, *args):
        """Handles the 'Item' action from the combat menu."""
        actor = actor or self._active or self._focused_player
        if not isinstance(actor, Character) or not actor.is_alive():
            self.say("[i]No one is ready to use an item.[/i]")
            return

        game = App.get_running_app().current_game
        consumables = []
        for item_name, qty in game.inventory.items():
            if qty <= 0:
                continue
            item = self._resolve_inventory_item(item_name)
            if item and getattr(item, 'type', None) == 'consumable':
                try:
                    item._original_key = item_name
                except Exception:
                    pass
                consumables.append((item, qty, item_name))
        if not consumables:
            self.say("[i]No usable items available.[/i]")
            self._pending_action = None
            self.action_chosen = False
            self._focused_player = actor
            self.action_in_progress = False
            Clock.schedule_once(lambda *_: self._show_action_menu(actor), 0)
            self._pending_action_type = None
            return
        self._active = actor
        self.action_in_progress = True
        self.action_chosen = True

        actions = []
        for item, qty, item_key in consumables:
            label = f"{item.name} (x{qty})"
            effect = getattr(item, "effect", {}) or {}
            if effect.get("target") == "party":
                actions.append((label, partial(self._use_party_item, actor, item, item_key)))
            else:
                actions.append((label, partial(self._prompt_for_any_target, item, actor, item_key)))
        
        popup = MenuPopup("Use Item", actions=actions, size_hint=(0.7, 0.7), theme_mgr=game.themes, autoclose=True)
        popup.bind(on_dismiss=lambda *_: self._cancel_action())
        self.game._style_popup(popup)
        popup.open()

    def _use_party_item(self, actor, item_obj, inventory_key, *args):
        """Queue a party-target item without requiring a manual selection."""
        if not actor or not actor.is_alive():
            self.say("[i]No one is ready to use an item.[/i]")
            self.action_chosen = False
            self.action_in_progress = False
            return
        self._pending_action = None
        self._pending_action_type = None
        self.action_chosen = False # This was a bug, it should be False
        self._focused_player = None
        self._queue_action(self._execute_item_use, actor, item_obj, inventory_key, None, unlock=True)

    def _prompt_for_any_target(self, item_obj, actor, inventory_key, *args):
        """Wait for the player to choose any battler as the item target."""
        if not actor or not actor.is_alive():
            self.say("[i]No one is ready to use an item.[/i]")
            self.action_chosen = False
            self.action_in_progress = False
            return

        self._active = actor
        self.action_in_progress = True
        self.action_chosen = True
        self._pending_action_type = "item"

        def _select_target(target, *, _actor=actor, _item=item_obj, _item_key=inventory_key):
            if not target or not target.is_alive():
                self.say("[i]Choose a living target.[/i]")
                self._pending_action = lambda new_target: _select_target(new_target, _actor=_actor, _item=_item, _item_key=_item_key)
                self._pending_action_type = "item"
                return
            self._pending_action = None
            self._pending_action_type = None
            self.action_chosen = False
            self._focused_player = None
            self._queue_action(self._execute_item_use, _actor, _item, _item_key, target, unlock=True)

        self._pending_action = _select_target
        self.say(f"Use {item_obj.name} on?")

    def _execute_item_use(self, actor, item_obj, inventory_key, target):
        """Consume an item and resolve its effect via the game logic."""
        self.action_in_progress = True
        game = App.get_running_app().current_game

        try:
            canonical_key = self._ensure_inventory_entry(item_obj, inventory_key)
            if canonical_key is None:
                self.say(f"[i]You don't have any {item_obj.name}.[/i]")
                self.action_chosen = False
                self._focused_player = actor
                self.action_in_progress = False
                self._pending_action = None
                self._pending_action_type = None
                Clock.schedule_once(lambda *_: self._show_action_menu(actor), 0)
                return

            game.use_item(item_obj, target)
            self.say(f"{actor.name.upper()} uses {item_obj.name}!")
        except Exception as exc:
            self.say(f"[i]Couldn't use {item_obj.name}.[/i]")
            print(f"[DEBUG] Item use failed: {exc}")

        self._after_action(actor)

    def camera_zoom_on_target(self, target_sprite: Widget, duration: float = 0.4, scale: float = 1.15, shake_intensity: float = 0.0):
        """This effect has been disabled to remove screen shake on attacks."""
        # This method is called but is intentionally left empty to disable the effect.
        pass

    def _execute_attack(self, attacker, target):
        """Main entry point for any attack. Sets flags and starts animation."""
        if not attacker or not target:
            return
        if not attacker.is_alive() or not target.is_alive():
            return

        self._active = attacker
        self.action_in_progress = True
        attacker.last_action = {'type': 'attack', 'target': target}
        self.say(f"{attacker.name.upper()} attacks {target.name.upper()}...")

        # Find the correct panel for the attacker
        if isinstance(attacker, Character):
            panel = self._party_panels[self.players.index(attacker)]
        else:
            try:
                panel = self._enemy_panels[self.enemies.index(attacker)]
            except (ValueError, IndexError):
                self._after_action(attacker, delay=2.0) # Attacker not found, end turn
                return

        # Animate the lunge
        sprite = panel.sprite
        # --- NEW: Emote Swap ---
        # Store original sprite and swap to angry emote if it exists.
        original_source = sprite.source
        angry_emote_path = f"images/characters/emotes/{attacker.id}_angry.png"
        if isinstance(attacker, Character) and resource_find(angry_emote_path):
            sprite.source = angry_emote_path
        # --- END NEW ---
        original_pos = sprite.pos[:]
        try:
            target_panel = self._party_panels[self.players.index(target)] if isinstance(target, Character) else self._enemy_panels[self.enemies.index(target)]
        except (ValueError, IndexError):
            self._after_action(attacker, delay=2.0) # Target not found, end turn
            return

        lunge_x = original_pos[0] + (target_panel.center_x - sprite.center_x) * 0.35
        lunge_y = original_pos[1] + (target_panel.center_y - sprite.center_y) * 0.35
        (Animation(pos=(lunge_x, lunge_y), d=0.15, t='out_quad') + Animation(pos=original_pos, d=0.2, t='in_quad')).start(sprite)

        # --- MODIFIED: on_complete callback ---
        def on_attack_finish(*args):
            sprite.source = original_source # Revert to original sprite
            self._resolve_attack(attacker, target)
        Clock.schedule_once(on_attack_finish, 0.15) # Resolve damage mid-lunge

    def _resolve_attack(self, attacker, target):
        """Resolve an attack action from attacker to target."""
        # Determine hit chance
        attacker_acc = attacker.total_accuracy if hasattr(attacker, 'total_accuracy') else 100.0
        target_eva = target.total_evasion if hasattr(target, 'total_evasion') else 0.0
        hit_chance = attacker_acc - target_eva
        if hit_chance < 0:
            hit_chance = 0.0
        roll = random.randint(1, 100)
        try:
            target_panel = (self._party_panels[self.players.index(target)] if isinstance(target, Character)
                            else self._enemy_panels[self.enemies.index(target)])
        except (ValueError, IndexError):
            target_panel = None

        if roll > hit_chance:
            # --- MODIFIED: Animate a dodge on miss ---
            if target_panel:
                sprite = target_panel.sprite
                original_pos = sprite.pos[:]
                # Quick side-step animation
                dodge_anim = (
                    Animation(x=original_pos[0] + dp(20), duration=0.1, t='out_quad') +
                    Animation(pos=original_pos, duration=0.2, t='in_out_quad')
                )
                dodge_anim.start(sprite)
                self._spawn_damage_popup(target_panel.sprite, "MISS", "physical")

            self.say(f"{attacker.name.upper()}'s attack missed!")
            # --- FIX: Call _after_action to end the turn on a miss. ---
            self._after_action(attacker, delay=2.0)
            return

        # If hit:
        # --- NEW: Animate recoil on hit ---
        if target_panel:
            sprite = target_panel.sprite
            original_pos = sprite.pos[:]

            # Player characters recoil DOWN, enemies recoil UP.
            if isinstance(target, Character):
                recoil_y_offset = -dp(15) # Lunge down
            else:
                recoil_y_offset = dp(15)  # Lunge up

            recoil_anim = Animation(y=original_pos[1] + recoil_y_offset, duration=0.08, t='out_quad') + \
                          Animation(pos=original_pos, duration=0.3, t='out_elastic')
            recoil_anim.start(sprite)

        # --- FIX: Initialize min/max damage before the logic branches ---
        # Default to a small variance for enemies or characters without weapons.
        min_dmg, max_dmg = -1, 2

        if isinstance(attacker, Character):
            weapon = attacker.equipment.get('weapon')
            if weapon and hasattr(weapon, 'equipment'):
                # Use the weapon's damage range if available
                min_dmg = weapon.equipment.get('damage_min', -1)
                max_dmg = weapon.equipment.get('damage_max', 2)
            base_atk = attacker.total_atk
        else:
            base_atk = attacker.total_atk

        # Use target's total defense if available
        target_def = target.total_def if hasattr(target, 'total_def') else 0
        
        # Calculate damage with the new variance
        variance = random.randint(min_dmg, max_dmg)
        damage = base_atk + variance - target_def
        if target.defending:
            damage = damage // 2
            target.defending = False

        # --- MODIFIED: Timed Guard Logic ---
        # Check if this attack was against a player who could guard
        is_guarded = False
        if isinstance(target, Character) and self._timed_bonus > 1.0:
            damage = int(damage * 0.5)  # 50% damage reduction for a successful guard
            is_guarded = True
            self.say(f"{target.name} guarded the attack!")
        # Reset bonus after use, regardless of whether it was against a player
        self._timed_bonus = 1.0

        # Minimum 1 damage before resist
        if damage < 1:
            damage = 1
        # Elemental resistance multiplier
        # --- MODIFIED: Use weapon_type to determine element for VFX ---
        dtype = "physical" # Default
        if isinstance(attacker, Character):
            weapon = attacker.equipment.get('weapon')
            if weapon and hasattr(weapon, 'equipment'):
                dtype = weapon.equipment.get('element', 'physical').lower()
        elif hasattr(attacker, "element") and attacker.element != Element.NONE:
             dtype = attacker.element.name.lower()

        # If attack function passes element separately (e.g., skill attacks), handle accordingly:
        # (In a skill effect, we will integrate element and status differently.)

        mult = 1.0
        if dtype:
            res_val = target.get_resistance_value(dtype)
            mult = max(0.0, (100 - res_val) / 100.0)
        damage = int(damage * mult)
        # Critical hit check
        crit = False
        crit_roll = random.randint(1, 100)
        crit_chance = attacker.total_crit_rate if hasattr(attacker, 'total_crit_rate') else 0.0
        if crit_roll <= crit_chance:
            crit = True
            damage = int(damage * CRIT_DAMAGE_MULT)
            self.say("Critical hit!")

        # Ensure at least 1 damage after all calculations (unless it was turned into healing)
        if damage == 0 and mult >= 0:
            damage = 1
        # Apply damage (negative damage will heal target)
        if damage < 0:
            target.hp = min(target.total_max_hp, target.hp - damage)
        else:
            target.hp = max(0, target.hp - damage)
            # --- THIS IS THE FIX: Grant resonance when dealing damage ---
            # We only grant resonance for damage dealt to enemies, not friendly fire.
            if isinstance(target, Enemy):
                self.change_resonance(max(1, damage // 10))

        # Show damage popup
        if target_panel:
            # --- MODIFIED: Use the new combined zoom-and-shake effect ---
            # This camera effect was disabled, but the logic remains.
            shake_intensity = dp(6) if crit else dp(2.5)
            self.camera_zoom_on_target(target_panel.sprite, duration=0.35, scale=1.08, shake_intensity=shake_intensity)

            # --- FIX: Show the weapon attack decal on the target ---
            if isinstance(attacker, Character):
                weapon = attacker.equipment.get('weapon')
                if weapon and hasattr(weapon, 'equipment'):
                    weapon_type = weapon.equipment.get('weapon_type')
                    if weapon_type: self._show_attack_effect(target_panel.sprite, weapon_type)

            dtype_for_popup = dtype if mult >= 0 else "heal"
            self._spawn_damage_popup(target_panel.sprite, "MISS" if damage is None else abs(damage), dtype_for_popup)
            target_panel.flash_damage()
        
        # --- FIX: Always call _after_action to end the turn after a successful hit. ---
        self._after_action(attacker, delay=2.0)

    def _after_action(self, actor, *, delay: float = 0.0):
        """
        Core cleanup function to run after any character action.
        Resets the acting battler's ATB, checks for battle end conditions,
        and unlocks the battle so the queue can continue processing.
        """
        if getattr(self, "_is_ending_turn", False):
            return

        self._is_ending_turn = True

        if actor and self._active == actor:
            try:
                panel = (
                    self._party_panels[self.players.index(actor)]
                    if isinstance(actor, Character)
                    else self._enemy_panels[self.enemies.index(actor)]
                )
                if panel:
                    panel.atb.set(0)
                    panel.set_ready(False)
            except (ValueError, IndexError):
                pass  # Actor might have been defeated or removed.

        self._handle_enemy_deaths()
        battle_over = self._check_end()

        if battle_over:
            self._clear_target_brackets()
            self._focused_player = None
            self._refresh_ui()
            self._is_ending_turn = False
            return

        if self._active == actor:
            self._active = None
        self.action_chosen = False
        self._pending_action = None
        self._pending_action_type = None
        self._focused_player = None

        self._clear_target_brackets()
        self._refresh_ui()

        def _finish_unlock(*_):
            self.action_in_progress = False
            self._is_ending_turn = False

        if delay > 0:
            Clock.schedule_once(_finish_unlock, delay)
        else:
            _finish_unlock()

    def _select_skill_target(self, skill: str) -> None:
        """Sets up the game to wait for a target for the selected skill."""
        self.action_chosen = True
        self._pending_action = lambda target: self._execute_skill(skill, target)
        self._pending_action_type = "skill"
        self.say(f"Tap a target for {skill.replace('_',' ').title()}.")

    def _execute_skill(self, skill, target):
        node = self.game._get_skill_node(skill)
        if not node: return
        self.action_in_progress = True
        effect, rp_cost, name = node.get("effect", {}), node.get("effect", {}).get("rp_cost", 0), node.get("name", skill.replace('_', ' ').title())
        game = App.get_running_app().current_game
        if game.resonance < rp_cost:
            self.say("[i]Not enough resonance![/i]"); self._after_action(self._active); return
        self.change_resonance(-rp_cost)
        etype = effect.get("type")
        if etype == "damage":
            mult = effect.get("mult", 1.0)
            dmg = max(1, int((self._active.atk + random.randint(-1, 2)) * mult))
            # --- FIX: Check if target exists before proceeding ---
            if not target:
                self._after_action(self._active)
                return
            if target.defending: dmg //= 2; target.defending = False
            target.hp = max(0, target.hp - dmg)
            self.audio.play_sfx("hit")
            self._spawn_damage_popup((self._party_panels[self.players.index(target)].sprite if isinstance(target, Character) else self._enemy_panels[self.enemies.index(target)].sprite), dmg, "physical")
            self.change_resonance(max(1, dmg // 10))
            target.hp = max(0, target.hp - dmg); self.audio.play_sfx("hit")
        elif etype == "heal":
            amt = int(effect.get("value", 0)); target.hp = min(target.total_max_hp, target.hp + amt); self.say(f"{self._active.name.upper()} restores {amt} HP to {target.name.upper()}.")
        elif etype == "buff":
            buff_type, val = effect.get("buff_type", ""), effect.get("value", 0); buffs = getattr(target, "buffs", {}); buffs[buff_type] = buffs.get(buff_type, 0) + val; target.buffs = buffs; self.say(f"{target.name.upper()} gains {val} {buff_type.replace('_',' ')}.")
        elif etype == "utility":
            self.say(f"{self._active.name.upper()} activates {name}. ({effect.get('subtype', '')})")
        else:
            self.say(f"{self._active.name.upper()} uses {name}, but nothing happens.")
        # --- FIX: Call _after_action to end the turn ---
        self._after_action(self._active)

    def _start_victory_sequence(self, dt):
        """Waits 1 second, then triggers the victory pose."""
        Clock.schedule_once(self._on_victory_emote, 0.7)

    def _on_victory_emote(self, dt):
        """Swaps party sprites to their 'cool' emote and holds before showing victory screen."""
        for player, panel in zip(self.players, self._party_panels):
            if player.is_alive():
                panel.sprite.original_source = panel.sprite.source
                cool_emote_path = f"images/characters/emotes/{player.id}_cool.png"
                if resource_find(cool_emote_path):
                    panel.sprite.source = cool_emote_path
        
        # Schedule the victory screen to appear after a 1-second hold on the cool pose.
        Clock.schedule_once(self._show_victory_screen, 1.4)

    def _show_victory_screen(self, dt):
        """Calls the final _finish method to display the victory screen."""
        self._finish(True)

    def _check_end(self) -> bool:
        # --- FIX: Add a guard to prevent multiple calls to _finish. ---
        if getattr(self, '_battle_is_over', False):
            return True # Already ending, don't re-evaluate

        """Checks for victory or defeat. Returns True if the battle has ended."""
        if all(e.hp <= 0 for e in self.enemies): self._battle_is_over = True; self.say("[i]All enemies defeated![/i]"); self.audio.play_sfx("win"); Clock.schedule_once(self._start_victory_sequence, 0.6); return True
        if all(p.hp <= 0 for p in self.players): self._battle_is_over = True; self.say("[i]The party has fallen…[/i]"); Clock.schedule_once(lambda *_: self._finish(False), 0.6); return True
        return False

    def _handle_enemy_deaths(self) -> None:
        from kivy.animation import Animation

        was_kill = False
        panels_to_remove = []
        for idx in reversed(range(len(self.enemies))):
            if self.enemies[idx].hp > 0: continue
            dead_panel = self._enemy_panels[idx]
            if dead_panel:
                was_kill = True
                self.last_target = self.enemies[idx] # Store the last defeated target
                panels_to_remove.append(dead_panel)
                fade = Animation(opacity=0, duration=0.4, t='in_quad')
                dead_panel.sprite.disabled = True

                def _hide(panel_to_hide):
                    if panel_to_hide.parent:
                        panel_to_hide.parent.remove_widget(panel_to_hide)

                fade.bind(on_complete=lambda anim, panel=dead_panel: _hide(panel))
                fade.start(dead_panel)
        return was_kill
    def _prepare_victory(self) -> dict:
        """
        Collect spoils and check for level-ups.
        Returns a dictionary containing spoils, level-up data, and new skills.
        """
        game = App.get_running_app().current_game
        total_xp = 0
        total_credits = 0
        item_bucket = defaultdict(int)

        for en in self.enemies:
            total_xp += getattr(en, "xp_reward", 0)
            total_credits += getattr(en, "credit_reward", 0)
            for drop in getattr(en, "drops", []):
                if random.random() <= drop.get("chance", 1.0):
                    item_bucket[drop["id"]] += drop.get("quantity", 1)

        level_up_data = []
        new_skill_data = []
        living = [p for p in self.players if p.is_alive()]
        if living:
            leveling_manager = _LevelingManager()
            level_up_skills = game.progression_data.get("level_up_skills", {})
            
            share, extra = divmod(total_xp, len(living))
            for idx, pl in enumerate(living):
                gain = share + (1 if idx < extra else 0)
                if gain > 0:
                    old_level = pl.level
                    pl.xp += gain
                    
                    new_level = leveling_manager.get_level_for_xp(pl.xp)
                    
                    if new_level > old_level:
                        pl.level = new_level
                        level_up_data.append({"character": pl.name, "new_level": new_level})
                        
                        # Check for skills learned between old and new level
                        char_skills_by_level = level_up_skills.get(pl.id, {})
                        for i in range(old_level + 1, new_level + 1):
                            skill_id = char_skills_by_level.get(str(i))
                            if skill_id:
                                # Add the skill to the character
                                pl.unlocked_abilities.add(skill_id)
                                # Find the skill's friendly name for the popup
                                skill_node = game._get_skill_node(skill_id)
                                skill_name = skill_node['name'] if skill_node else skill_id.replace('_', ' ').title()
                                new_skill_data.append({"character": pl.name, "skill_name": skill_name})

        game.credits = getattr(game, "credits", 0) + total_credits
        inv = game.inventory
        for itm, qty in item_bucket.items():
            inv[itm] = inv.get(itm, 0) + qty

        spoils = []
        if total_xp:
            spoils.append({"name": "Experience", "quantity": total_xp})
        if total_credits:
            spoils.append({"name": "Credits", "quantity": total_credits})
        for itm, qty in item_bucket.items():
            item_obj = game.all_items.find(itm)
            spoils.append({
                "name": item_obj.name if item_obj else itm.title(),
                "quantity": qty,
                "rarity": getattr(item_obj, "rarity", None),
                "flavor_text": getattr(item_obj, "description", None)
            })
        
        return {
            "spoils": spoils,
            "level_ups": level_up_data,
            "new_skills": new_skill_data
        }
    
    # ------------------------------------------------------------------
    #  Battle wrap-up
    # ------------------------------------------------------------------
    def _finish(self, victory: bool) -> None:
        app = App.get_running_app()
        
        def _cleanup_and_return():
            self.opacity = 1
            if self.sm.current == self.name:
                self.sm.current = "explore"
                if self.game:
                    self.game.update_room()
            if self.parent:
                self.sm.remove_widget(self)
            if getattr(self.game, "battle_screen", None) is self:
                self.game.battle_screen = None

        def do_fade_out():
            fade_out_anim = Animation(opacity=0, duration=0.7, t='out_quad')
            fade_out_anim.bind(on_complete=lambda *_: _cleanup_and_return())
            fade_out_anim.start(self)

        if victory:
            victory_data = self._prepare_victory()

            def _after_all_popups_closed(*_):
                if self.on_victory:
                    self.on_victory()
                do_fade_out()

            def _show_levelup_popup(*_):
                has_levelups = victory_data.get("level_ups") or victory_data.get("new_skills")
                if has_levelups:
                    # FIX: Create the popup, bind its dismiss event, then open it.
                    popup = VictoryScreen(
                        victory_data,
                        mode='levelup',
                        on_close=_after_all_popups_closed
                    )
                    popup.open()
                else:
                    _after_all_popups_closed()

            # FIX: Create the popup, bind its dismiss event, then open it.
            spoils_popup = VictoryScreen(
                victory_data,
                mode='spoils',
                on_close=_show_levelup_popup
            )
            spoils_popup.open()

        else:  # Defeat
            if self.on_defeat:
                self.on_defeat()
            do_fade_out()

    def change_resonance(self, delta):
        # 1) update the underlying Game object
        self.game.change_resonance(delta)
        # 2) sync the bar
        self.res_bar.set(self.game.resonance)
