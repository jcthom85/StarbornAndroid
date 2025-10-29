# entities.py
import random
from typing import Dict, List
from collections import defaultdict
from enum import Enum, auto
from kivy.app import App
from constants import (BASE_ACC, BASE_CRT, BASE_EVA, 
                       ATK_STR_MULT, DEF_VIT_MULT, HP_PER_VIT, SPD_AGI_MULT,
                       EVA_PER_AGI, ACC_PER_FOC, CRT_PER_FOC, RES_PER_FOC, DROP_PER_LUCK)

# Allow multiple stat keys (atk vs attack, etc.) to map to the same derived stat.
STAT_SYNONYMS: Dict[str, tuple[str, ...]] = {
    "strength": ("str",),
    "vitality": ("vit",),
    "agility": ("agi",),
    "focus": ("fcs", "int"),
    "luck": ("lck",),
    "atk": ("attack",),
    "def": ("defense", "armor"),
    "spd": ("speed",),
    "max_hp": ("hp", "health"),
    "accuracy": ("acc",),
    "evasion": ("eva", "avoid"),
    "crit_rate": ("crit", "crt"),
    "resonance_start": ("starting_resonance", "resonance_init", "resonance_initial", "start_resonance"),
}

# Growth table for character level-ups.
# The list index corresponds to the level gained (e.g., index 0 is for level 2).
GROWTH_TABLE = {
    "default": [
        # Level 2
        {"hp": 15, "str": 1, "vit": 1, "agi": 1, "fcs": 1, "lck": 0},
        # Level 3
        {"hp": 15, "str": 1, "vit": 1, "agi": 1, "fcs": 1, "lck": 1},
        # Level 4
        {"hp": 20, "str": 2, "vit": 1, "agi": 1, "fcs": 1, "lck": 1},
        # Level 5
        {"hp": 20, "str": 1, "vit": 2, "agi": 2, "fcs": 1, "lck": 1},
        # ... you can add more levels here
    ]
}
class Element(Enum):
    NONE      = auto()
    FIRE      = auto()
    ICE       = auto()
    LIGHTNING = auto()
    POISON    = auto()
    RADIATION = auto()
    PSYCHIC   = auto()
    VOID      = auto()

class Status(Enum):
    BURN      = auto()
    POISON    = auto()
    SHOCK     = auto()
    FREEZE    = auto()
    REGEN     = auto()
    WEAK      = auto()
    SHIELD    = auto()
    # (We can extend this with sleep, paralysis, etc., as needed)

class Buff:
    """Represents an active buff or debuff on an entity."""
    def __init__(self, buff_def: dict):
        self.id: str = buff_def["id"]
        self.duration: int = buff_def.get("duration", 0)
        self.effects: dict = buff_def.get("effects", {})  # {'flat':{}, 'mult':{}, 'resist':{}}
        self.flags: List[str] = buff_def.get("flags", [])
        self.stack_rule: str = buff_def.get("stack", "refresh")

class ResistMixin:
    """Provides resistance calculation methods for an entity."""
    def get_resistance_value(self, res_type: str) -> int:
        # Base resistance from the entity's resistances dict (0 if not present)
        base_resist = getattr(self, "resistances", {}).get(res_type, 0)
        # Add general resistance from Focus (Resist General)
        general_resist = 0
        # Use total_focus if available (Character), otherwise focus attribute
        focus_val = self.total_focus if hasattr(self, "total_focus") else getattr(self, "focus", 0)
        general_resist = int(focus_val * RES_PER_FOC)
        base_resist += general_resist
        # Add resist modifiers from active buffs
        resist_bonus = 0
        for buff in getattr(self, "active_buffs", []):
            if "resist" in buff.effects:
                resist_bonus += buff.effects["resist"].get(res_type, 0)
        return base_resist + resist_bonus

class Entity(ResistMixin):
    is_enemy = False
    def __init__(self, name: str, hp: int, atk: int, spd: int):
        self.name = name
        # Base stats
        self.max_hp = hp        # base HP (will be augmented by Vitality)
        self.hp = hp            # current HP
        self.atk = atk          # base attack (may be recalculated from STR)
        self.spd = spd          # base speed (intrinsic)
        # Status and battle flags
        self.defending = False
        self.status_effects: Dict[Status, int] = defaultdict(int)
        self.resistances: Dict[str, int] = {}   # resistances dict (int percentages)
        # Buff system
        self.active_buffs: List[Buff] = []      # list of Buff instances currently on entity

    def is_alive(self) -> bool:
        return self.hp > 0

    def heal_full(self):
        self.hp = self.max_hp

    def add_status(self, status: Status, turns: int = 2):
        # Add a status effect for given duration (overwrites with longer duration if already present)
        self.status_effects[status] = max(self.status_effects.get(status, 0), turns)

    def has_status(self, status: Status) -> bool:
        return self.status_effects.get(status, 0) > 0

    def clear_statuses(self):
        self.status_effects.clear()

    def tick_statuses(self):
        # Decrement status durations each turn
        for st in list(self.status_effects.keys()):
            self.status_effects[st] -= 1
            if self.status_effects[st] <= 0:
                del self.status_effects[st]

    def add_buff(self, buff_id: str):
        """Apply a buff/debuff to this entity by buff ID."""
        game = App.get_running_app().current_game
        buff_def = game.buff_index.get(buff_id)
        if not buff_def:
            return  # Buff ID not found
        new_buff = Buff(buff_def)
        # Handle stacking rules
        for existing in self.active_buffs:
            if existing.id == buff_id:
                if new_buff.stack_rule == "refresh":
                    existing.duration = new_buff.duration  # reset duration
                elif new_buff.stack_rule == "ignore":
                    # do nothing, keep the existing buff
                    pass
                elif new_buff.stack_rule == "additive":
                    # allow stacking: we'll add the new buff separately
                    continue
                # In any case except additive, we do not add a duplicate buff
                return
        # Add new buff (for additive or if no existing buff of same ID)
        self.active_buffs.append(new_buff)

    def tick_buffs(self):
        """Reduce durations of active buffs by 1 and remove expired ones."""
        expired = []
        for buff in self.active_buffs:
            buff.duration -= 1
            if buff.duration <= 0:
                expired.append(buff)
        # Remove expired buffs
        for buff in expired:
            self.active_buffs.remove(buff)
        # (Optional: could handle flag removal or trigger end-of-buff effects here)

    def get_stat_with_buffs(self, stat_name: str) -> float:
        """
        Compute base + equip + flat buffs for a given stat, 
        and return (multipliers will be applied separately if needed).
        """
        def _resolve(mapping: Dict[str, float], key: str) -> float:
            if not mapping:
                return 0
            if key in mapping:
                return mapping[key]
            for alias in STAT_SYNONYMS.get(key, ()):
                if alias in mapping:
                    return mapping[alias]
            return 0

        # Base value (attribute on self if exists, else 0)
        base_val = getattr(self, stat_name, None)
        if base_val is None:
            for alias in STAT_SYNONYMS.get(stat_name, ()):
                base_val = getattr(self, alias, None)
                if base_val is not None:
                    break
        if base_val is None:
            base_val = 0

        # Equipment bonuses (if character with equipment)
        equip_bonus = 0
        if hasattr(self, "equipment"):
            for item in self.equipment.values():
                if item:
                    # Equipment stats
                    if hasattr(item, 'equipment'):
                        equip_bonus += _resolve(item.equipment.get("stats", {}), stat_name)
                    # Mod attachments on equipment (if any)
                    if hasattr(item, 'mods'):
                        for mod in item.mods.values():
                            if mod and 'effects' in mod:
                                equip_bonus += _resolve(mod['effects'].get("stats", {}), stat_name)
        # Buff flat bonuses
        flat_bonus = 0
        for buff in self.active_buffs:
            flat_bonus += _resolve(buff.effects.get("flat", {}), stat_name)
        return base_val + equip_bonus + flat_bonus

    @staticmethod
    def _get_multiplier_for(buff: Buff, stat_name: str) -> float:
        mults = buff.effects.get("mult", {})
        if stat_name in mults:
            return mults[stat_name]
        for alias in STAT_SYNONYMS.get(stat_name, ()):
            if alias in mults:
                return mults[alias]
        return 1.0


class Character(Entity):
    is_enemy = False
    def __init__(self, name: str, hp: int, atk: int, spd: int,
                 *, strength: int = 5, vitality: int = 5, agility: int = 5,
                 focus: int = 5, luck: int = 5, rp: int = 20,
                 resistances: dict = None, skills=None, unlocked_abilities=None,
                 ability_points: int = 0, level: int = 1, xp: int = 0,
                 mini_icon_path: str = "assets/avatars/default_32.png"):
        super().__init__(name, hp, atk, spd)
        # Primary attributes
        self.strength = strength
        self.vitality = vitality
        self.agility = agility
        self.focus = focus
        self.luck = luck
        # Set base resistances if provided
        self.resistances = resistances or {}
        # Resource points (mana/energy)
        self.max_rp = rp
        self.rp = rp
        # Progression
        self.level = level
        self.xp = xp
        self.ability_points = ability_points
        # Skills
        self.skills = skills or []
        self.unlocked_abilities: set[str] = set(unlocked_abilities or [])
        # UI icon
        self.mini_icon_path = mini_icon_path
        # Equipment slots
        self.equipment = {"weapon": None, "armor": None, "accessory": None}
        # (We do not use self.defense, self.evasion, self.crit_chance as fixed stats;
        # they will be derived from attributes and buffs.)
        self.last_action = None

    # Derived stats properties including buffs and equipment
    @property
    def total_strength(self) -> int:
        return int(self.get_stat_with_buffs("strength"))
    @property
    def total_vitality(self) -> int:
        return int(self.get_stat_with_buffs("vitality"))
    @property
    def total_agility(self) -> int:
        return int(self.get_stat_with_buffs("agility"))
    @property
    def total_focus(self) -> int:
        return int(self.get_stat_with_buffs("focus"))
    @property
    def total_luck(self) -> int:
        return int(self.get_stat_with_buffs("luck"))

    @property
    def total_atk(self) -> int:
        # Attack = floor(STR * 2) + weapon atk + flat buffs, then * multipliers
        base_attack = int(self.total_strength * ATK_STR_MULT)
        # Include any direct equipment or flat buff contributions to "atk"
        base_attack += self.get_stat_with_buffs("atk")  # weapon atk bonus is included here
        # Apply any multiplicative buffs to attack
        mult_factor = 1.0
        for buff in self.active_buffs:
            mult_factor *= self._get_multiplier_for(buff, "atk")
        return int(base_attack * mult_factor)

    @property
    def total_def(self) -> int:
        # Defense = floor(VIT * 1.5) + armor + flat buffs, then * multipliers
        base_def = int(self.total_vitality * DEF_VIT_MULT)
        base_def += self.get_stat_with_buffs("def")  # armor defense bonus
        mult_factor = 1.0
        for buff in self.active_buffs:
            mult_factor *= self._get_multiplier_for(buff, "def")
        return int(base_def * mult_factor)

    @property
    def total_max_hp(self) -> int:
        # Max HP = base max_hp + VIT * 10 + flat bonuses, then * multipliers
        base_max = self.max_hp + (self.total_vitality * HP_PER_VIT)
        base_max += self.get_stat_with_buffs("max_hp")
        mult_factor = 1.0
        for buff in self.active_buffs:
            mult_factor *= self._get_multiplier_for(buff, "max_hp")
        return int(base_max * mult_factor)

    @property
    def total_spd(self) -> float:
        # Speed = base spd + AGI * 1.2 + flat bonuses, then * multipliers
        base_speed = self.spd + (self.total_agility * SPD_AGI_MULT)
        base_speed += self.get_stat_with_buffs("spd")
        mult_factor = 1.0
        for buff in self.active_buffs:
            mult_factor *= self._get_multiplier_for(buff, "spd")
        return base_speed * mult_factor

    @property
    def total_accuracy(self) -> float:
        # Accuracy % = BASE_ACC + FOC * 0.2% + flat, then * multipliers
        acc = BASE_ACC + (self.total_focus * ACC_PER_FOC)
        # Incorporate buff effects on accuracy (buff uses key "accuracy")
        acc += self.get_stat_with_buffs("accuracy")
        mult_factor = 1.0
        for buff in self.active_buffs:
            mult_factor *= self._get_multiplier_for(buff, "accuracy")
        # Clamp to [0, 100] as a percentage
        acc = acc * mult_factor
        if acc < 0: acc = 0
        if acc > 100: acc = 100
        return acc

    @property
    def total_evasion(self) -> float:
        # Evasion % = BASE_EVA + AGI * 0.15% + flat, then * multipliers
        eva = BASE_EVA + (self.total_agility * EVA_PER_AGI)
        eva += self.get_stat_with_buffs("evasion")
        mult_factor = 1.0
        for buff in self.active_buffs:
            mult_factor *= self._get_multiplier_for(buff, "evasion")
        eva = eva * mult_factor
        if eva < 0: eva = 0
        if eva > 100: eva = 100
        return eva

    @property
    def total_crit_rate(self) -> float:
        # Critical % = BASE_CRT + FOC * 0.15% + flat, then * multipliers
        crt = BASE_CRT + (self.total_focus * CRT_PER_FOC)
        crt += self.get_stat_with_buffs("crit_rate")
        mult_factor = 1.0
        for buff in self.active_buffs:
            mult_factor *= self._get_multiplier_for(buff, "crit_rate")
        crt = crt * mult_factor
        if crt < 0: crt = 0
        if crt > 100: crt = 100
        return crt

    @property
    def total_drop_bonus(self) -> float:
        # Drop rate bonus percentage from Luck
        return self.total_luck * DROP_PER_LUCK

    def spend_ap(self, cost: int) -> bool:
        if cost <= self.ability_points:
            self.ability_points -= cost
            return True
        return False

    @property
    def usable_skills(self):
        # Only return skills that have been unlocked and not sealed by "no_skills" flag
        if any("no_skills" in buff.flags for buff in self.active_buffs):
            return []  # no skills can be used under "disrupt" effect
        return [sid for sid in self.skills if sid in self.unlocked_abilities]

class Enemy(Entity):
    is_enemy = True
    def __init__(self, name: str, hp: int, atk: int, spd: int,
                 strength: int = 0, vitality: int = 0, agility: int = 0,
                 focus: int = 0, luck: int = 0):
        super().__init__(name, hp, atk, spd)
        # Assign primary attributes for the enemy
        self.strength = strength
        self.vitality = vitality
        self.agility = agility
        self.focus = focus
        self.luck = luck
        # (Enemy resistances will be set after instantiation from JSON if provided)
        self.element_stacks: Dict[Element, int] = {e: 0 for e in Element if e != Element.NONE}

    # We can leverage Character's properties by computing similarly for Enemy:
    @property
    def total_strength(self): return int(self.get_stat_with_buffs("strength"))
    @property
    def total_vitality(self): return int(self.get_stat_with_buffs("vitality"))
    @property
    def total_agility(self): return int(self.get_stat_with_buffs("agility"))
    @property
    def total_focus(self): return int(self.get_stat_with_buffs("focus"))
    @property
    def total_luck(self): return int(self.get_stat_with_buffs("luck"))

    @property
    def total_max_hp(self) -> int:
        # Max HP = base max_hp + VIT * 10 + flat bonuses, then * multipliers
        base_max = self.max_hp + (self.total_vitality * HP_PER_VIT)
        base_max += self.get_stat_with_buffs("max_hp")
        mult_factor = 1.0
        for buff in self.active_buffs:
            mult_factor *= self._get_multiplier_for(buff, "max_hp")
        return int(base_max * mult_factor)
    
    @property
    def total_atk(self) -> int:
        # Enemies typically don't have equipment, but buffs can affect atk
        base_attack = int(self.total_strength * ATK_STR_MULT)
        base_attack += self.get_stat_with_buffs("atk")
        mult_factor = 1.0
        for buff in self.active_buffs:
            mult_factor *= self._get_multiplier_for(buff, "atk")
        return int(base_attack * mult_factor)

    @property
    def total_def(self) -> int:
        base_def = int(self.total_vitality * DEF_VIT_MULT)
        base_def += self.get_stat_with_buffs("def")
        mult_factor = 1.0
        for buff in self.active_buffs:
            mult_factor *= self._get_multiplier_for(buff, "def")
        return int(base_def * mult_factor)

    @property
    def total_spd(self) -> float:
        # Speed = base spd + AGI * 1.2 + flat bonuses, then * multipliers
        base_speed = self.spd + (self.total_agility * SPD_AGI_MULT)
        base_speed += self.get_stat_with_buffs("spd")
        mult_factor = 1.0
        for buff in self.active_buffs:
            mult_factor *= self._get_multiplier_for(buff, "spd")
        return base_speed * mult_factor

    @property
    def total_accuracy(self) -> float:
        # Enemies use same formula for accuracy
        acc = BASE_ACC + (self.total_focus * ACC_PER_FOC)
        # buffs to enemy accuracy:
        acc += self.get_stat_with_buffs("accuracy")
        mult = 1.0
        for buff in self.active_buffs:
            mult *= self._get_multiplier_for(buff, "accuracy")
        acc = acc * mult
        if acc < 0: acc = 0
        if acc > 100: acc = 100
        return acc

    @property
    def total_evasion(self) -> float:
        eva = BASE_EVA + (self.total_agility * EVA_PER_AGI)
        eva += self.get_stat_with_buffs("evasion")
        mult = 1.0
        for buff in self.active_buffs:
            mult *= self._get_multiplier_for(buff, "evasion")
        eva = eva * mult
        if eva < 0: eva = 0
        if eva > 100: eva = 100
        return eva

    @property
    def total_crit_rate(self) -> float:
        crt = BASE_CRT + (self.total_focus * CRT_PER_FOC)
        crt += self.get_stat_with_buffs("crit_rate")
        mult = 1.0
        for buff in self.active_buffs:
            mult *= self._get_multiplier_for(buff, "crit_rate")
        crt = crt * mult
        if crt < 0: crt = 0
        if crt > 100: crt = 100
        return crt

    # (Enemies typically don't have luck affecting drops individually; drop bonus is computed from party luck)
