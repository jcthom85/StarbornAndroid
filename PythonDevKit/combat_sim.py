#!/usr/bin/env python3
"""
Turn-by-turn combat simulator for Starborn balance testing.

Uses the same formula backbone as balance_lab.py (Formulae, DerivedStats)
but resolves each turn individually with hit/crit/dodge rolls, skill
cooldowns, RP management, and speed-based turn ordering.
"""
from __future__ import annotations

import math
import random
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Tuple


# ---------------------------------------------------------------------------
# Data classes
# ---------------------------------------------------------------------------

@dataclass
class CombatUnit:
    name: str
    kind: str  # "character" or "enemy"
    max_hp: int
    atk: float
    spd: float
    defense: float
    crit_chance: float  # 0-1
    crit_mult: float
    dodge_chance: float  # 0-1
    skills: List[Dict[str, Any]] = field(default_factory=list)
    rp_max: int = 100
    rp_regen: int = 6


@dataclass
class SkillState:
    skill: Dict[str, Any]
    cooldown_left: int = 0


@dataclass
class FighterState:
    unit: CombatUnit
    hp: int = 0
    rp: int = 0
    skill_states: List[SkillState] = field(default_factory=list)

    def __post_init__(self):
        if self.hp == 0:
            self.hp = self.unit.max_hp
        if self.rp == 0:
            self.rp = self.unit.rp_max
        if not self.skill_states:
            self.skill_states = [SkillState(s) for s in self.unit.skills]

    @property
    def alive(self) -> bool:
        return self.hp > 0


@dataclass
class TurnLogEntry:
    turn: int
    actor: str
    action: str
    damage: int = 0
    crit: bool = False
    miss: bool = False
    hp_remaining: int = 0


@dataclass
class SimResult:
    winner: str  # "character" or "enemy"
    turns: int
    char_hp_remaining: int
    enemy_hp_remaining: int
    log: List[TurnLogEntry] = field(default_factory=list)


@dataclass
class SimSummary:
    char_name: str
    enemy_name: str
    runs: int
    wins: int
    losses: int
    win_rate: float
    avg_turns: float
    avg_char_hp_remaining: float
    avg_enemy_hp_remaining: float
    sample_log: List[TurnLogEntry] = field(default_factory=list)


# ---------------------------------------------------------------------------
# Simulator
# ---------------------------------------------------------------------------

class CombatSimulator:
    """Run N simulated combats between a character and an enemy."""

    def __init__(self, formulae_cfg: Dict[str, Any], defaults_cfg: Dict[str, Any]):
        f = formulae_cfg
        self.mit_per_def = float(f.get("mitigation_per_def_point", 0.004))
        self.mit_cap = float(f.get("mitigation_cap", 0.60))
        self.base_attack_mult = float(f.get("base_attack_mult", 1.0))
        self.max_turns = 200  # safety cap

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def simulate(
        self,
        char: CombatUnit,
        enemy: CombatUnit,
        runs: int = 100,
        seed: Optional[int] = None,
    ) -> SimSummary:
        rng = random.Random(seed)
        results: List[SimResult] = []
        for _ in range(runs):
            result = self._run_one(char, enemy, rng)
            results.append(result)

        wins = sum(1 for r in results if r.winner == "character")
        avg_turns = sum(r.turns for r in results) / max(1, len(results))
        avg_char_hp = sum(r.char_hp_remaining for r in results) / max(1, len(results))
        avg_enemy_hp = sum(r.enemy_hp_remaining for r in results) / max(1, len(results))

        # Use the first run as sample log
        sample_log = results[0].log if results else []

        return SimSummary(
            char_name=char.name,
            enemy_name=enemy.name,
            runs=runs,
            wins=wins,
            losses=runs - wins,
            win_rate=wins / max(1, runs),
            avg_turns=round(avg_turns, 2),
            avg_char_hp_remaining=round(avg_char_hp, 2),
            avg_enemy_hp_remaining=round(avg_enemy_hp, 2),
            sample_log=sample_log,
        )

    # ------------------------------------------------------------------
    # Internal
    # ------------------------------------------------------------------

    def _run_one(self, char: CombatUnit, enemy: CombatUnit, rng: random.Random) -> SimResult:
        cf = FighterState(char)
        ef = FighterState(enemy)
        log: List[TurnLogEntry] = []
        turn = 0

        while cf.alive and ef.alive and turn < self.max_turns:
            turn += 1
            # Speed-based ordering: higher speed goes first
            if cf.unit.spd >= ef.unit.spd:
                first, second = (cf, ef), (ef, cf)
            else:
                first, second = (ef, cf), (cf, ef)

            self._do_turn(turn, first[0], first[1], log, rng)
            if not first[1].alive:
                break
            self._do_turn(turn, second[0], second[1], log, rng)

        winner = "character" if cf.alive else "enemy"
        return SimResult(
            winner=winner,
            turns=turn,
            char_hp_remaining=max(0, cf.hp),
            enemy_hp_remaining=max(0, ef.hp),
            log=log,
        )

    def _do_turn(
        self,
        turn: int,
        actor: FighterState,
        target: FighterState,
        log: List[TurnLogEntry],
        rng: random.Random,
    ):
        if not actor.alive or not target.alive:
            return

        # RP regen
        actor.rp = min(actor.unit.rp_max, actor.rp + actor.unit.rp_regen)

        # Tick cooldowns
        for ss in actor.skill_states:
            if ss.cooldown_left > 0:
                ss.cooldown_left -= 1

        # Choose action: best available skill or basic attack
        chosen_skill = self._choose_action(actor, rng)

        if chosen_skill is not None:
            skill_def = chosen_skill.skill
            action_name = skill_def.get("name", skill_def.get("id", "skill"))
            rp_cost = int(skill_def.get("effect", {}).get("rp_cost", 0))
            actor.rp -= rp_cost
            chosen_skill.cooldown_left = int(skill_def.get("cooldown", skill_def.get("effect", {}).get("cooldown", 0)))
            mult = float(skill_def.get("effect", {}).get("mult", 1.0))
            base_power = float(skill_def.get("base_power", 100)) / 100.0
            raw_atk = actor.unit.atk * max(mult, base_power)
        else:
            action_name = "Attack"
            raw_atk = actor.unit.atk * self.base_attack_mult

        # Hit check
        if rng.random() < target.unit.dodge_chance:
            log.append(TurnLogEntry(
                turn=turn, actor=actor.unit.name, action=action_name,
                damage=0, miss=True, hp_remaining=target.hp,
            ))
            return

        # Damage variance (-2 to +2)
        variance = rng.randint(-2, 2)

        # Crit check
        crit = rng.random() < actor.unit.crit_chance
        crit_mult = actor.unit.crit_mult if crit else 1.0

        # Mitigation
        mit = min(target.unit.defense * self.mit_per_def, self.mit_cap)
        damage = max(1, int((raw_atk + variance) * crit_mult * (1.0 - mit)))

        target.hp -= damage

        log.append(TurnLogEntry(
            turn=turn, actor=actor.unit.name, action=action_name,
            damage=damage, crit=crit, hp_remaining=max(0, target.hp),
        ))

    def _choose_action(self, actor: FighterState, rng: random.Random) -> Optional[SkillState]:
        """Pick the highest-damage available skill, or None for basic attack."""
        available = []
        for ss in actor.skill_states:
            if ss.cooldown_left > 0:
                continue
            eff = ss.skill.get("effect", {})
            if eff.get("type") not in ("damage", None):
                continue
            rp_cost = int(eff.get("rp_cost", 0))
            if rp_cost > actor.rp:
                continue
            mult = float(eff.get("mult", 1.0))
            base_power = float(ss.skill.get("base_power", 100)) / 100.0
            score = max(mult, base_power)
            available.append((score, ss))

        if not available:
            return None

        # Pick best
        available.sort(key=lambda x: x[0], reverse=True)
        return available[0][1]
