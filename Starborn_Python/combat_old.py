# StarBorn/combat.py

import json, os, random
from typing import List, Callable
from theme_manager import ThemeManager
from kivy.app            import App
from kivy.clock          import Clock
from kivy.metrics        import dp, sp
from kivy.properties     import NumericProperty
from kivy.graphics       import Color, Rectangle, Ellipse, RoundedRectangle
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
from kivy.uix.floatlayout  import FloatLayout
from kivy.uix.scatter import Scatter
from kivy.uix.behaviors import ButtonBehavior
from kivy.resources import resource_find
from ui.menu_popup import MenuPopup
from ui.weather_layer import WeatherLayer
from entities      import Character, Enemy, Status, Element
from sound_manager import SoundManager
import math
from functools import partial
from font_manager import fonts
from environment import EnvironmentLoader
from ui.victory_screen import VictoryScreen
from collections import defaultdict
from constants import CRIT_DAMAGE_MULT
from kivy.graphics.texture import Texture
from kivy.core.window import Window
from data.leveling_manager import LevelingManager
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
    "lightning": (1.0, 1.0, 0.2, 1),
}


class ClickableImage(ButtonBehavior, Image):
    """An Image that can be clicked (fires on_release)."""
    pass

class DamageLabel(Label):
    """Floating damage number that self-destructs after a short animation."""
    def __init__(self, text: str, color=(1, 1, 1, 1), **kwargs):
        super().__init__(
            text=text,
            color=color,
            font_name=fonts["damage_popup"]["name"],
            font_size=fonts["damage_popup"]["size"],
            size_hint=(None, None),
            markup=True,
            **kwargs,
        )
        self.bind(texture_size=self.setter("size"))

    def bounce(self, parent: Widget):
        """Animates the label upward, then fades it out."""
        from kivy.animation import Animation

        start_y = self.y
        anim = (
            Animation(y=start_y + dp(16), duration=0.25, t="out_quad")
            + Animation(y=start_y + dp(8), duration=0.2, t="out_bounce")
            + Animation(opacity=0, duration=0.2)
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
        self.sprite = ClickableImage(source=sprite_path, size=(spr_w, spr_h), size_hint=(None, None))
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
            lbl = ShadowLabel(text=battler.name.upper(), font_name=fonts["combat_name"]["name"],
                              font_size=fonts["combat_name"]["size"], size_hint_y=None,
                              height=sp(24), use_outline=True, outline_color=(0,0,0,0.8), outline_width=dp(1.5),
                              halign='center', align_to_text=True)
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
                                width=spr_w, height=sp(12), halign="center", valign="top",
                                use_outline=True, outline_color=(0,0,0,0.8), outline_width=dp(1.5),
                                align_to_text=True)
            self.hp_txt.text_size = (spr_w, None)
            self.add_widget(_wrap(self.hp_txt))

        # Common final setup
        self.bind(minimum_width=self.setter("width"), minimum_height=self.setter("height"))
        self.sprite_color = self.sprite.color[:]

    def update_stack_display(self):
        if hasattr(self, "stack_disp"): self.stack_disp.update_display()

    def flash_damage(self):
        # This line is now corrected to only return if flashes are disabled.
        if not App.get_running_app().settings.get('flashes', True):
            return
        Animation.cancel_all(self.sprite, 'color')
        self.sprite.color = self.sprite_color[:]
        (Animation(color=[1, 0.3, 0.3, 1], d=0.1) + Animation(color=self.sprite_color, d=0.2)).start(self.sprite)

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
        self._enemy_turn_queue = []
        self.auto_battle_active = False
        self._tap_window_active = False
        self._timed_bonus = 1.0
        self._pending_action = None
        self.action_chosen = False

        self._target_brackets = None
        self._target_panel = None

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
                          color=(1,1,1,0.88), size_hint=(1,None), height=dp(24),
                          use_outline=True, outline_color=(0,0,0,0.8), outline_width=dp(1.5),
                          halign='center', align_to_text=True)
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
        
        # from ui.combat_fx import TurnTimeline  # keep your other imports at top if you prefer

        for panel in all_panels:
            panel.atb.set(0)
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

    # Call from your click handler:
    def _on_enemy_click(self, enemy):
        # ... your existing selection logic ...
        # find panel:
        panel = next((p for p in self._enemy_panels if p and getattr(p, 'battler', getattr(p, 'is_enemy', True)) and p.sprite), None)
        # If you track which enemy maps to which panel, prefer that
        if panel: self._show_target_brackets(panel)

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
        if self.action_in_progress:
            # --- FIX: Prevent players from targeting other players with an attack ---
            # If the pending action is `_execute_attack`, it means we are in "Attack" mode.
            # In this mode, we should only be able to target enemies, not other players.
            if self._pending_action == self._execute_attack:
                self.say("[i]You must target an enemy.[/i]")
                return
            if self._pending_action:
                self._pending_action(player)
                self._pending_action = None
            return
        if not player.is_alive() or panel.atb.curr < panel.atb.max_val:
            return
        self.action_in_progress = True
        self._active = player
        self.say(f"[b]{self._active.name.upper()}[/b]'s turn.")
        self._show_action_menu(player)

    def _on_enemy_click(self, enemy) -> None:
        if self._pending_action:
            self._pending_action(enemy)
            # --- FIX: Clear the pending action after it has been executed. ---
            self._pending_action = None

    def _show_action_menu(self, player):
        actions = [
            ("Attack", self.on_attack),
            ("Skill",  self.on_skill),
            ("Item",   self.on_item),
            ("Defend", self.on_defend),
            ("Auto-Battle", self.toggle_auto_battle)
        ]
        game = App.get_running_app().current_game
        popup = MenuPopup(player.name.upper(), actions=actions, size_hint=(0.6, 0.6), autoclose=True, theme_mgr=game.themes)
        popup.bind(on_dismiss=lambda *_: self._cancel_action())
        game._style_popup(popup)
        popup.open()

    def _cancel_action(self, *args):
        Clock.schedule_once(self._deferred_cancel_action, 0)

    def _deferred_cancel_action(self, dt):
        # NEW LOGIC: If a pending action has been set (like waiting for a 
        # target for an item), we must NOT cancel the turn.
        if self._pending_action:
            self.action_chosen = False # Reset the flag for the next turn
            return

        # This code will now only run on a true cancellation (e.g., closing
        # the main action menu without choosing anything).
        self.action_in_progress = False
        self._active = None
        self._pending_action = None
        self._refresh_ui()
        
    def toggle_auto_battle(self, *args):
        self.auto_battle_active = not self.auto_battle_active
        self.say(f"Auto-Battle [color=00ff00]Enabled[/color]" if self.auto_battle_active else f"Auto-Battle [color=ff0000]Disabled[/color]")
        if self.auto_battle_active and isinstance(self._active, Character):
            self._execute_auto_action(0)
    
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
                    self._enemy_turn_queue.append(battler)
        if not self.action_in_progress and self._enemy_turn_queue:
            self._active = self._enemy_turn_queue.pop(0)
            self._enemy_turn()
        elif not self.action_in_progress and self.auto_battle_active:
            for player, panel in zip(self.players, self._party_panels):
                if player.is_alive() and panel.atb.curr >= panel.atb.max_val:
                    self._active = player
                    Clock.schedule_once(self._execute_auto_action, 0.1)
                    break

    def _enemy_turn(self, *_) -> None:
        en = self._active
        if not en.is_alive():
            self._after_action()
            return
        living_players = [p for p in self.players if p.hp > 0]
        if not living_players:
            self._check_end()
            self._after_action()
            return
        tgt = random.choice(living_players)
        self.say(f"{en.name} attacks {tgt.name.upper()}!")

        # --- NEW: Enemy Lunge Animation ---
        attacker_panel = self._enemy_panels[self.enemies.index(en)]
        target_panel = self._party_panels[self.players.index(tgt)]
        sprite = attacker_panel.sprite
        original_pos = sprite.pos[:]

        lunge_factor = 0.35
        lunge_x = original_pos[0] + (target_panel.center_x - sprite.center_x) * lunge_factor
        lunge_y = original_pos[1] + (target_panel.center_y - sprite.center_y) * lunge_factor

        lunge_anim = Animation(pos=(lunge_x, lunge_y), duration=0.15, t='out_quad') + \
                     Animation(pos=original_pos, duration=0.20, t='in_quad')
        lunge_anim.bind(on_complete=lambda *_: self._resolve_enemy_attack(en, tgt))
        lunge_anim.start(sprite)

    def _resolve_enemy_attack(self, enemy, target_player):
        self.shake_screen()
        self.audio.play_sfx("hit")
        # This now calls the main, unified attack resolution function.
        # All detailed calculations (hit, crit, guard, damage, etc.) are handled there.
        self._resolve_attack(enemy, target_player)
        # The original _after_action() call is removed from here because _resolve_attack now handles it.

    def _execute_auto_action(self, dt):
        if not self._active or not self._active.last_action:
            self.on_defend()
            return
        action = self._active.last_action
        action_type = action.get('type')
        if action_type == 'attack':
            target = action.get('target')
            if not target or not target.is_alive():
                target = random.choice([e for e in self.enemies if e.is_alive()])
            self._execute_attack(target)
        elif action_type == 'defend':
            self.on_defend()
        else:
            self.on_defend()

    def on_attack(self, *_) -> None:
        self._pending_action = self._execute_attack
        self.say("Choose a target.")

    # 2. UPDATE the on_defend method to pass the active character.
    def on_defend(self, *_) -> None:
        if isinstance(self._active, Enemy): return
        self._active.last_action = {'type': 'defend'}
        self.audio.play_sfx("block")
        # This line is in your file as self._active.defend(), which is not a method.
        # The correct way is to set the defending flag.
        self._active.defending = True 
        self.say(f"{self._active.name.upper()} braces for impact.")

    def on_skill(self, *args):
        """Handles the 'Skill' action from the combat menu."""
        if not isinstance(self._active, Character): return
        self._pending_action = True
        skills = getattr(self._active, "usable_skills", [])
        if not skills:
            self.say("[i]No skills available.[/i]")
            self._pending_action = None
            return
        actions = []
        for skill_id in skills:
            node = self.game._get_skill_node(skill_id)
            if node:
                skill_name = node.get("name", skill_id.replace('_',' ').title())
                actions.append((skill_name, partial(self._select_skill_target, skill_id)))
        popup = MenuPopup("Use Skill", actions=actions, size_hint=(0.6, 0.6), theme_mgr=self.game.themes)
        popup.bind(on_dismiss=lambda *_: self._cancel_action())
        self.game._style_popup(popup)
        popup.open()

    def on_item(self, *args):
        if not isinstance(self._active, Character): return
        
        # BUG: This line incorrectly sets the pending action to True. Remove it.
        # self._pending_action = True 
        
        game = App.get_running_app().current_game
        consumables = []
        for item_name, qty in game.inventory.items():
            item = game.all_items.find(item_name)
            if item and getattr(item, 'type', None) == 'consumable' and qty > 0:
                consumables.append((item, qty))
        if not consumables:
            self.say("[i]No usable items available.[/i]")
            # self._pending_action = None # This line is also safe to remove.
            return
        actions = [(f"{item.name} (x{qty})", partial(self._prompt_for_any_target, item, self._active)) 
                   for item, qty in consumables]
        popup = MenuPopup("Use Item", actions=actions, size_hint=(0.7, 0.7), theme_mgr=game.themes)
        popup.bind(on_dismiss=lambda *_: self._cancel_action())
        game._style_popup(popup)
        popup.open()

    def _prompt_for_any_target(self, item_obj, actor, *args): # FIX: Add '*args' here
        self.action_chosen = True
        self.say(f"Use {item_obj.name} on?")
        self._pending_action = lambda target: self._apply_item_effect(item_obj, target, actor)

    def _execute_attack(self, target):
        attacker = self._active
        attacker.last_action = {'type': 'attack', 'target': target}
        self.say(f"{attacker.name.upper()} attacks {target.name.upper()}...")

        # Find the attacker's panel to get their sprite
        try:
            # This logic is for player characters only
            if not isinstance(attacker, Character):
                # For non-characters, just resolve the attack after a delay
                Clock.schedule_once(lambda dt: self._resolve_attack(attacker, target), 0.4)
                return
            attacker_panel = self._party_panels[self.players.index(attacker)]
        except (ValueError, IndexError):
            # Fallback if panel not found, though this is unlikely
            Clock.schedule_once(lambda dt: self._resolve_attack(attacker, target), 0.4)
            return
 
        sprite = attacker_panel.sprite
        original_pos = sprite.pos[:] # Store original position
 
        # --- NEW: Emote Swap ---
        original_source = sprite.source
        angry_emote_path = f"images/characters/emotes/{attacker.id}_angry.png"
         
        # Use resource_find to safely check if the image exists before changing it
        if resource_find(angry_emote_path):
            sprite.source = angry_emote_path
        # --- END NEW ---
 
        # --- REVISED: Lunge towards the target instead of hopping ---
        # Find the target's sprite to calculate the lunge direction.
        target_panel = self._enemy_panels[self.enemies.index(target)]
        target_sprite = target_panel.sprite

        # Calculate a point partway to the target.
        lunge_factor = 0.35 # How far to lunge (35% of the distance)
        lunge_x = original_pos[0] + (target_sprite.center_x - sprite.center_x) * lunge_factor
        lunge_y = original_pos[1] + (target_sprite.center_y - sprite.center_y) * lunge_factor

        # Define the new lunge animation: move towards target, then back.
        lunge_anim = (
            Animation(pos=(lunge_x, lunge_y), duration=0.15, t='out_quad') +
            Animation(pos=original_pos, duration=0.20, t='in_quad')
        )
 
        # --- MODIFIED: on_complete callback ---
        # We create a dedicated function for the callback to make it cleaner
        def on_attack_finish(*args):
            # Revert to original sprite source
            sprite.source = original_source
            # Then resolve the attack logic
            self._resolve_attack(attacker, target)
 
        # When the lunge is finished, call our new function.
        lunge_anim.bind(on_complete=on_attack_finish)
         
        # Start the timed-hit window at the same time as the lunge.
        lunge_anim.start(sprite)

    def camera_zoom_on_target(self, target_sprite: Widget, duration: float = 0.4, scale: float = 1.15, shake_intensity: float = 0.0):
        """This effect has been disabled to remove screen shake on attacks."""
        pass

    def _resolve_attack(self, attacker, target):
        """Resolve an attack action from attacker to target."""
        # Determine hit chance
        attacker_acc = attacker.total_accuracy if hasattr(attacker, 'total_accuracy') else 100.0
        target_eva = target.total_evasion if hasattr(target, 'total_evasion') else 0.0
        hit_chance = attacker_acc - target_eva
        if hit_chance < 0:
            hit_chance = 0.0
        # Roll for hit/miss
        roll = random.randint(1, 100)
        target_panel = (self._party_panels[self.players.index(target)] if isinstance(target, Character)
                        else self._enemy_panels[self.enemies.index(target)])
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

        # Calculate base damage using attacker's attack and target's defense
        if isinstance(attacker, Character):
            base_atk = attacker.total_atk
        else:
            base_atk = attacker.total_atk
        # Use target's total defense if available
        target_def = target.total_def if hasattr(target, 'total_def') else 0
        # Random variance -1 to +2
        variance = random.randint(-1, 2)
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

        # Show damage popup
        if target_panel:
            # --- MODIFIED: Use the new combined zoom-and-shake effect ---
            # 1. Impact ring for visual weight.
            self._fx(ImpactRing(pos=target_panel.sprite.center, radius=dp(30 if crit else 20)))
            # 2. Hit sparks for a satisfying crunch.
            self._fx(HitSparks(pos=target_panel.sprite.center, n=15 if crit else 8))
            # 3. Camera zoom on the target, with a shake effect on impact.
            shake_intensity = dp(6) if crit else dp(2.5)
            self.camera_zoom_on_target(target_panel.sprite, duration=0.35, scale=1.08, shake_intensity=shake_intensity)

            # --- FIX: Show the weapon attack decal on the target ---
            # This was missing. It gets the attacker's weapon type and displays the effect.
            if isinstance(attacker, Character):
                weapon = attacker.equipment.get('weapon')
                if weapon and hasattr(weapon, 'equipment'):
                    weapon_type = weapon.equipment.get('weapon_type')
                    if weapon_type: self._show_attack_effect(target_panel.sprite, weapon_type)

            dtype_for_popup = dtype if mult < 0 else dtype  # could adjust color for healing if needed
            self._spawn_damage_popup(target_panel.sprite, "MISS" if damage is None else abs(damage), dtype_for_popup)
            target_panel.flash_damage()
            
            # --- MODIFIED: Trigger the attack effect visualization ---
            # This is now handled in _execute_attack to sync with the lunge.
        
        # --- ADD THIS LOGIC ---
        self._after_action(attacker)

    def _after_action(self, actor, *args):
        """
        Core cleanup function to run after any character action.
        Resets the active character's ATB, checks for battle end conditions,
        and unlocks the battle to proceed.
        """
        # --- FIX: Only clear the active character if they are the one who just acted ---
        if actor and self._active == actor:
            try:
                panel = (self._party_panels[self.players.index(actor)] if isinstance(actor, Character)
                         else self._enemy_panels[self.enemies.index(actor)])
                panel.atb.set(0)
                panel.set_ready(False)
            except (ValueError, IndexError):
                pass # Battler might have died and been removed

        self._handle_enemy_deaths()
        if not self._check_end():
            if self._active == actor: self._active = None
            if self._active is None: self.action_in_progress = False
            self._refresh_ui()

    def _select_skill_target(self, skill: str) -> None:
        """Sets up the game to wait for a target for the selected skill."""
        self.action_chosen = True
        self._pending_action = lambda target: self._execute_skill(skill, target)
        self.say(f"Tap a target for {skill.replace('_',' ').title()}.")

    def _execute_skill(self, skill, target):
        node = self.game._get_skill_node(skill)
        if not node: return
        effect, rp_cost, name = node.get("effect", {}), node.get("effect", {}).get("rp_cost", 0), node.get("name", skill.replace('_', ' ').title())
        game = App.get_running_app().current_game
        if game.resonance < rp_cost:
            self.say("[i]Not enough resonance![/i]"); self._after_action(self._active); return
        self.change_resonance(-rp_cost)
        etype = effect.get("type")
        if etype == "damage":
            mult = effect.get("mult", 1.0)
            dmg = max(1, int((self._active.atk + random.randint(-1, 2)) * mult))
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
        else: self.say(f"{self._active.name.upper()} uses {name}, but nothing happens.")

    def _check_end(self) -> bool:
        """Checks for victory or defeat. Returns True if the battle has ended."""
        if all(e.hp <= 0 for e in self.enemies):
            self.say("[i]All enemies defeated![/i]"); self.audio.play_sfx("win"); Clock.schedule_once(lambda *_: self._finish(True), 0.6); return True
        if all(p.hp <= 0 for p in self.players):
            self.say("[i]The party has fallen…[/i]"); Clock.schedule_once(lambda *_: self._finish(False), 0.6); return True
        return False

    def _handle_enemy_deaths(self) -> None:
        from kivy.animation import Animation
        for idx in reversed(range(len(self.enemies))):
            if self.enemies[idx].hp > 0: continue
            dead_panel = self._enemy_panels[idx]
            if dead_panel:
                fade = Animation(opacity=0, duration=0.4, t='in_quad')
                dead_panel.sprite.disabled = True
                fade.start(dead_panel)
                def _hide(dt, panel=dead_panel):
                    # This removes the panel from the layout entirely.
                    if panel.parent:
                        panel.parent.remove_widget(panel)
                    self._reset_order()

                Clock.schedule_once(lambda dt, p=dead_panel: _hide(dt, p), 0.3)

    # ──────────────────────────────────────────────────────────────
    #  SPOILS CALCULATION / PRESENTATION
    # ──────────────────────────────────────────────────────────────
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
            leveling_manager = LevelingManager()
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