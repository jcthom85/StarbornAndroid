#!/usr/bin/env python3
# tools/balance_lab.py
from __future__ import annotations
import json, os, csv, math, argparse, glob, copy
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Tuple

ROOT = os.path.dirname(os.path.dirname(__file__))  # repo root (this file is tools/*)
DEF_CFG = os.path.join(ROOT, "data", "balance_targets.json")

def _load_json(path: str, default):
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return copy.deepcopy(default)

def _ensure_dir(path: str):
    os.makedirs(path, exist_ok=True)

def _clamp(x, lo, hi):
    return max(lo, min(hi, x))

@dataclass
class BalanceConfig:
    raw: Dict[str, Any]
    paths: Dict[str, str]
    defaults: Dict[str, Any]
    formulas: Dict[str, Any]
    targets: Dict[str, Any]
    tuning: Dict[str, Any]

    @classmethod
    def from_file(cls, cfg_path: str) -> "BalanceConfig":
        raw = _load_json(cfg_path, {})
        return cls(
            raw=raw,
            paths=raw.get("paths", {}),
            defaults=raw.get("defaults", {}),
            formulas=raw.get("formulas", {}),
            targets=raw.get("targets", {}),
            tuning=raw.get("tuning", {})
        )

    def path(self, key: str) -> str:
        rel = self.paths.get(key)
        return os.path.join(ROOT, rel) if rel else ""

@dataclass
class GameData:
    characters: List[Dict[str, Any]] = field(default_factory=list)
    enemies: List[Dict[str, Any]] = field(default_factory=list)
    skills: Dict[str, Any] = field(default_factory=dict)
    skill_trees: Dict[str, Any] = field(default_factory=dict)
    leveling: Dict[str, Any] = field(default_factory=dict)
    progression: Dict[str, Any] = field(default_factory=dict)

class DataLoader:
    def __init__(self, cfg: BalanceConfig):
        self.cfg = cfg

    def load(self) -> GameData:
        chars = _load_json(self.cfg.path("characters"), [])
        enemies = _load_json(self.cfg.path("enemies"), [])
        skills = _load_json(self.cfg.path("skills"), {})
        leveling = _load_json(self.cfg.path("leveling"), {})
        progression = _load_json(self.cfg.path("progression"), {})
        trees_dir = self.cfg.path("skill_trees_dir")
        trees: Dict[str, Any] = {}
        if trees_dir and os.path.isdir(trees_dir):
            for fp in glob.glob(os.path.join(trees_dir, "*.json")):
                try:
                    name = os.path.splitext(os.path.basename(fp))[0]
                    trees[name] = _load_json(fp, {})
                except Exception:
                    pass
        return GameData(chars, enemies, skills, trees, leveling, progression)

@dataclass
class DerivedStats:
    hp: float
    atk: float
    spd: float
    defense: float
    crit_chance: float
    crit_mult: float
    dodge_chance: float

class Formulae:
    def __init__(self, cfg: BalanceConfig):
        f = cfg.formulas; d = cfg.defaults
        self.hp_per_vit = f.get("hp_per_vit", 10)
        self.atk_per_str = f.get("atk_per_str", 1.0)
        self.spd_per_agi = f.get("spd_per_agi", 1.0)
        self.def_per_vit = f.get("def_per_vit", 0.5)
        self.mitigation_per_def = f.get("mitigation_per_def_point", 0.004)
        self.mit_cap = f.get("mitigation_cap", 0.60)
        self.base_attack_mult = f.get("base_attack_mult", 1.0)
        self.crit_chance = d.get("crit_chance", 0.05)
        self.crit_mult = d.get("crit_mult", 1.5)
        self.dodge_chance = d.get("dodge_chance", 0.03)

    def derive(self, base: Dict[str, Any]) -> DerivedStats:
        hp_base = float(base.get("hp", 30))
        str_ = float(base.get("strength", base.get("attack", 0)))
        agi = float(base.get("agility", base.get("speed", 0)))
        vit = float(base.get("vitality", 0))
        atk_base = float(base.get("attack", 0))
        spd_base = float(base.get("speed", 0))
        hp = hp_base + vit * self.hp_per_vit
        atk = atk_base + str_ * self.atk_per_str
        spd = spd_base + agi * self.spd_per_agi
        defense = vit * self.def_per_vit
        return DerivedStats(hp, atk, spd, defense, self.crit_chance, self.crit_mult, self.dodge_chance)

    def mitigation(self, defense: float) -> float:
        return _clamp(defense * self.mitigation_per_def, 0.0, self.mit_cap)

    def ehp(self, hp: float, defense: float) -> float:
        mit = self.mitigation(defense)
        return hp / max(0.01, (1.0 - mit))

@dataclass
class SkillEval:
    id: str
    name: str
    power_score: float
    dpr_estimate: float
    rp_cost: float
    notes: str

class SkillAnalyzer:
    def __init__(self, cfg: BalanceConfig, formulae: Formulae):
        self.cfg = cfg; self.f = formulae
        w = cfg.formulas.get("skill_power_weight", {})
        self.w_damage_mult = float(w.get("damage_mult", 1.0))
        self.w_heal_value  = float(w.get("heal_value", 0.6))
        self.w_buff_flat   = float(w.get("buff_flat_point", 0.15))
        self.w_buff_mult   = float(w.get("buff_mult_point", 1.0))
        self.w_rp          = float(w.get("rp_cost_weight", 0.06))

    def _expected_damage_from_mult(self, atk: float, mult: float) -> float:
        crit = 1 + (self.f.crit_mult - 1) * self.f.crit_chance
        hit  = 1 - self.f.dodge_chance
        return atk * mult * crit * hit

    def score_skill(self, node: Dict[str, Any], atk: float) -> Optional[SkillEval]:
        eff = node.get("effect") or {}
        rp = float(eff.get("rp_cost", 0))
        name = node.get("name", node.get("id", "skill"))
        sid = node.get("id", name)
        if eff.get("type") == "damage":
            mult = float(eff.get("mult", 1.0))
            dmg = self._expected_damage_from_mult(atk, mult)
            dpr = dmg
            score = dmg * self.w_damage_mult - rp * self.w_rp
            return SkillEval(sid, name, score, dpr, rp, "damage")
        if eff.get("type") == "heal":
            val = float(eff.get("value", 0))
            score = val * self.w_heal_value - rp * self.w_rp
            return SkillEval(sid, name, score, 0.0, rp, "heal")
        if eff.get("type") == "buff":
            val = float(eff.get("value", 0))
            btype = eff.get("buff_type", "attack")
            score = val * (self.w_buff_mult if btype in ("attack","defense") else self.w_buff_flat) - rp * self.w_rp
            return SkillEval(sid, name, score, 0.0, rp, f"buff:{btype}")
        if eff.get("type") == "utility":
            return SkillEval(sid, name, -rp * self.w_rp * 0.5, 0.0, rp, eff.get("subtype", "utility"))
        return None

    def best_single_target_dpr(self, skills: List[SkillEval], base_atk_dpr: float) -> Tuple[SkillEval, float]:
        if not skills:
            return SkillEval("basic_attack","Basic Attack", base_atk_dpr, base_atk_dpr, 0,"basic"), base_atk_dpr
        dmg_skills = [s for s in skills if s.notes == "damage"]
        if not dmg_skills:
            return SkillEval("basic_attack","Basic Attack", base_atk_dpr, base_atk_dpr, 0,"basic"), base_atk_dpr
        regen = float(self.cfg.defaults.get("rp_regen_per_turn", 6))
        best = max(dmg_skills, key=lambda s: s.dpr_estimate * (min(1.0, regen / max(1.0, s.rp_cost)) if s.rp_cost>0 else 1.0))
        availability = 1.0 if best.rp_cost <= 0 else min(1.0, regen / best.rp_cost)
        return best, max(base_atk_dpr, best.dpr_estimate * availability)

@dataclass
class UnitSnapshot:
    name: str
    kind: str
    hp: float
    atk: float
    spd: float
    defense: float
    ehp: float
    dpr: float
    notes: str = ""

class BalanceAnalyzer:
    def __init__(self, cfg: BalanceConfig, data: GameData):
        self.cfg = cfg; self.data = data
        self.formulae = Formulae(cfg)
        self.skillz = SkillAnalyzer(cfg, self.formulae)

    def _collect_character_skills(self, char_id: str, atk: float) -> List[SkillEval]:
        out: List[SkillEval] = []
        tree = self.data.skill_trees.get(char_id)
        if not tree:
            for k,v in self.data.skill_trees.items():
                if k.lower() == str(char_id).lower():
                    tree = v; break
        if tree and "branches" in tree:
            for branch in tree["branches"].values():
                for node in branch:
                    se = self.skillz.score_skill(node, atk)
                    if se: out.append(se)
        for sid, sdef in (self.data.skills or {}).items():
            node = {"id": sid, "name": sdef.get("name", sid), "effect": sdef}
            se = self.skillz.score_skill(node, atk)
            if se: out.append(se)
        dedup: Dict[str, SkillEval] = {}
        for s in out: dedup.setdefault(s.id, s)
        return list(dedup.values())

    def _basic_attack_dpr(self, atk: float) -> float:
        return self.skillz._expected_damage_from_mult(atk, self.formulae.base_attack_mult)

    def snapshot_character(self, ch_def: Dict[str, Any]) -> UnitSnapshot:
        name = ch_def.get("name", ch_def.get("id", "character"))
        cid  = ch_def.get("id", name.lower())
        stats = self.formulae.derive(ch_def)
        base_dpr = self._basic_attack_dpr(stats.atk)
        skills = self._collect_character_skills(cid, stats.atk) if self.cfg.defaults.get("assume_best_single_target_skill", True) else []
        best_skill, dpr = self.skillz.best_single_target_dpr(skills, base_dpr)
        return UnitSnapshot(name, "character", stats.hp, stats.atk, stats.spd, stats.defense, self.formulae.ehp(stats.hp, stats.defense), dpr, f"Best:{best_skill.name}")

    def snapshot_enemy(self, en_def: Dict[str, Any]) -> UnitSnapshot:
        name = en_def.get("name", en_def.get("id", "enemy"))
        stats = self.formulae.derive(en_def)
        base_dpr = self._basic_attack_dpr(stats.atk)
        return UnitSnapshot(name, "enemy", stats.hp, stats.atk, stats.spd, stats.defense, self.formulae.ehp(stats.hp, stats.defense), base_dpr, "(basic attack assumed)")

    def compute_ttk(self, attacker: UnitSnapshot, defender: UnitSnapshot) -> float:
        dpr = max(0.01, attacker.dpr)
        turns = defender.ehp / dpr
        rel = _clamp(attacker.spd / max(1.0, defender.spd), 0.5, 1.5)
        return turns / rel

    def analyze(self) -> Dict[str, Any]:
        report: Dict[str, Any] = {"characters": [], "enemies": [], "pairwise": [], "leveling": {}}
        chars = [self.snapshot_character(c) for c in self.data.characters]
        enemies = [self.snapshot_enemy(e) for e in self.data.enemies]
        for c in chars: report["characters"].append(c.__dict__)
        for e in enemies: report["enemies"].append(e.__dict__)

        baseline_ttk = float(self.cfg.defaults.get("baseline_ttk_turns", 4.0))
        baseline_survive = float(self.cfg.defaults.get("baseline_survive_turns", 4.0))
        tolerance = float(self.cfg.tuning.get("tolerance_turns", 0.25))

        flags: List[Dict[str, Any]] = []
        for c in chars:
            for e in enemies:
                ttk = self.compute_ttk(c, e)
                stk = self.compute_ttk(e, c)
                row = {
                    "character": c.name, "enemy": e.name,
                    "ttk_turns": round(ttk,2), "survive_turns": round(stk,2),
                    "ttk_delta": round(ttk - baseline_ttk,2),
                    "survive_delta": round(stk - baseline_survive,2)
                }
                if abs(ttk - baseline_ttk) > tolerance:
                    row["flag_ttk"] = "over" if ttk < baseline_ttk else "under"
                if abs(stk - baseline_survive) > tolerance:
                    row["flag_survive"] = "fragile" if stk < baseline_survive else "tanky"
                report["pairwise"].append(row)
                if "flag_ttk" in row or "flag_survive" in row:
                    flags.append(row)
        report["flags"] = flags

        lvl = self.data.leveling or {}
        xp_table = lvl.get("levels") or lvl.get("xp_table") or []
        deltas, last = [], 0
        for x in xp_table:
            deltas.append(x - last); last = x
        report["leveling"] = {"xp_table": xp_table, "deltas": deltas, "avg_delta": (sum(deltas)/len(deltas)) if deltas else 0}

        suggestion = self._suggest_global_tuning(chars, enemies, baseline_ttk)
        if suggestion: report["tuning_suggestion"] = suggestion
        return report

    def _suggest_global_tuning(self, chars: List[UnitSnapshot], enemies: List[UnitSnapshot], target_ttk: float) -> Optional[Dict[str, Any]]:
        if not chars or not enemies: return None
        ttks = [self.compute_ttk(c, e) for c in chars for e in enemies]
        if not ttks: return None
        avg_ttk = sum(ttks)/len(ttks)
        k = target_ttk / max(0.01, avg_ttk)
        step_cap = float(self.cfg.tuning.get("max_global_scale_step", 0.15))
        limited = 1.0 + _clamp(k - 1.0, -step_cap, step_cap)
        return {
            "metric": "avg_ttk",
            "current": round(avg_ttk,3),
            "target": target_ttk,
            "proposed_change": {"enemy_hp_scale": round(limited,3)},
            "full_needed_scale": round(k,3),
            "note": f"Apply enemy_hp_scale ×{limited} (cap {step_cap}). Re-run and iterate."
        }

class Reporter:
    def __init__(self, cfg: BalanceConfig):
        self.cfg = cfg
        self.out_dir = os.path.join(ROOT, self.cfg.paths.get("reports_dir") or "reports")
        _ensure_dir(self.out_dir)

    def write_all(self, report: Dict[str, Any]):
        jpath = os.path.join(self.out_dir, "balance_report.json")
        with open(jpath, "w", encoding="utf-8") as f: json.dump(report, f, indent=2)
        self._write_csv(report)
        self._write_md_summary(report)
        print(f"[BalanceLab] Wrote:\n- {jpath}\n- {os.path.join(self.out_dir,'pairwise.csv')}\n- {os.path.join(self.out_dir,'SUMMARY.md')}")

    def _write_csv(self, report: Dict[str, Any]):
        rows = report.get("pairwise", [])
        if not rows: return
        cpath = os.path.join(self.out_dir, "pairwise.csv")
        cols = ["character","enemy","ttk_turns","survive_turns","ttk_delta","survive_delta","flag_ttk","flag_survive"]
        with open(cpath, "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=cols); w.writeheader()
            for r in rows: w.writerow({k: r.get(k) for k in cols})

    def _write_md_summary(self, report: Dict[str, Any]):
        mpath = os.path.join(self.out_dir, "SUMMARY.md")
        flags = report.get("flags", [])
        tune = report.get("tuning_suggestion")
        leveling = report.get("leveling", {})
        with open(mpath, "w", encoding="utf-8") as f:
            f.write("# Starborn — Balance Report (v0)\n\n")
            if tune:
                f.write("## Global Tuning Suggestion\n")
                f.write(f"- Metric: **{tune['metric']}**\n")
                f.write(f"- Current: **{tune['current']}**  → Target: **{tune['target']}**\n")
                for k, v in tune["proposed_change"].items():
                    f.write(f"- Proposed: **{k} ×{v}**\n")
                f.write(f"- Full scale needed: ×{tune['full_needed_scale']}\n")
                f.write(f"- Note: {tune['note']}\n\n")
            f.write("## Top Imbalance Flags\n")
            if not flags:
                f.write("- None. All pairwise matchups are within tolerance.\n\n")
            else:
                for r in flags[:40]:
                    ft = r.get("flag_ttk","-"); fs = r.get("flag_survive","-")
                    f.write(f"- {r['character']} vs {r['enemy']}: TTK {r['ttk_turns']} ({ft}), Survive {r['survive_turns']} ({fs})\n")
                if len(flags) > 40:
                    f.write(f"... and {len(flags)-40} more.\n\n")
            if leveling:
                f.write("## Level Curve\n")
                ad = leveling.get("avg_delta", 0)
                f.write(f"- Average XP delta per level: **{round(ad,2)}**\n")

class Patcher:
    def __init__(self, cfg: BalanceConfig): self.cfg = cfg
    def apply(self, report: Dict[str, Any]) -> List[str]:
        tune = report.get("tuning_suggestion"); 
        if not tune: return ["No tuning suggestion to apply."]
        scale = tune.get("proposed_change", {}).get("enemy_hp_scale")
        if not scale: return ["No supported change (v0 supports enemy_hp_scale only)."]
        ep = self.cfg.path("enemies"); enemies = _load_json(ep, [])
        if not enemies: return [f"Could not load enemies file: {ep}"]
        bak = ep + ".bak"
        with open(bak, "w", encoding="utf-8") as f: json.dump(enemies, f, indent=2)
        for e in enemies:
            base_hp = float(e.get("hp", 30))
            e["hp"] = round(base_hp * float(scale), 3)
        with open(ep, "w", encoding="utf-8") as f: json.dump(enemies, f, indent=2)
        return [f"Applied enemy_hp_scale ×{scale} to {len(enemies)} enemies.", f"Backup written: {bak}"]

def main():
    ap = argparse.ArgumentParser(description="Starborn Balance Lab")
    ap.add_argument("--config", default=DEF_CFG, help="Path to data/balance_targets.json")
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("analyze", help="Analyze and write reports")
    sp = sub.add_parser("tune", help="Analyze and propose global tuning")
    sp.add_argument("--apply", action="store_true", help="Apply safe changes (v0: enemy_hp_scale)")
    args = ap.parse_args()
    cfg = BalanceConfig.from_file(args.config)
    data = DataLoader(cfg).load()
    analyzer = BalanceAnalyzer(cfg, data)
    report = analyzer.analyze()
    Reporter(cfg).write_all(report)
    if args.cmd == "tune" and args.apply:
        msgs = Patcher(cfg).apply(report)
        print("\n".join(msgs))

if __name__ == "__main__":
    main()
