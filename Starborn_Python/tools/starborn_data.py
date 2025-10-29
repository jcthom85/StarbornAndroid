#!/usr/bin/env python3
# tools/starborn_data.py
from __future__ import annotations
import json, shutil
from pathlib import Path
from typing import Any, Dict, List, Optional

# ---------- Project root discovery ----------
def find_project_root(start: Optional[Path] = None, markers: tuple[str, ...] = (
    "items.json","npcs.json","dialogue.json","quests.json","enemies.json",
)) -> Path:
    start = Path(start or __file__).resolve()
    candidates = [start, start.parent, start.parents[1], Path.cwd(), Path.cwd().parent]
    for base in candidates:
        for m in markers:
            if (base / m).exists():
                return base
    # tools/ layout
    maybe = (start.parent / "..").resolve()
    for m in markers:
        if (maybe / m).exists():
            return maybe
    return Path.cwd().resolve()

# ---------- JSON helpers ----------
def read_list_json(path: Path) -> list:
    if not path.exists(): return []
    try:
        with path.open("r", encoding="utf-8") as f:
            data = json.load(f)
            return data if isinstance(data, list) else []
    except Exception:
        return []

def read_dict_json(path: Path) -> dict:
    if not path.exists(): return {}
    try:
        with path.open("r", encoding="utf-8") as f:
            data = json.load(f)
            return data if isinstance(data, dict) else {}
    except Exception:
        return {}

def write_json_with_backup(path: Path, data: Any) -> bool:
    try:
        if path.exists():
            shutil.copy2(path, path.with_suffix(path.suffix + ".bak"))
        with path.open("w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=4)
        return True
    except Exception:
        return False

# ---------- Lookups used by editors ----------
def collect_npc_names(root: Path) -> List[str]:
    out = set()
    for n in read_list_json(root / "npcs.json"):
        nm = (n.get("name") or n.get("id") or "").strip()
        if nm: out.add(nm)
        for a in n.get("aliases", []):
            a = str(a).strip()
            if a: out.add(a)
    return sorted(out, key=str.lower)

def collect_item_names(root: Path) -> List[str]:
    out = set()
    for it in read_list_json(root / "items.json"):
        nm = (it.get("name") or it.get("id") or "").strip()
        if nm: out.add(nm)
        for a in it.get("aliases", []):
            a = str(a).strip()
            if a: out.add(a)
    return sorted(out, key=str.lower)

def collect_dialogue_ids(root: Path) -> List[str]:
    ids = []
    for d in read_list_json(root / "dialogue.json"):
        did = (d.get("id") or "").strip()
        if did: ids.append(did)
    return sorted(set(ids), key=str.lower)

def collect_quest_ids(root: Path) -> List[str]:
    ids = []
    for q in read_list_json(root / "quests.json"):
        qid = (q.get("id") or "").strip()
        if qid: ids.append(qid)
    return sorted(set(ids), key=str.lower)

def collect_milestone_ids(root: Path) -> List[str]:
    ids = []
    for m in read_list_json(root / "milestones.json"):
        mid = (m.get("id") or "").strip()
        if mid: ids.append(mid)
    return sorted(set(ids), key=str.lower)

# ---------- Validation bits ----------
ELEMENTS = ["none","fire","ice","lightning","poison","radiation","psychic","void"]
TIERS    = ["minion","standard","elite","boss"]

def validate_dialogue(dialogue: List[dict], npc_names: List[str], all_ids: List[str]) -> List[str]:
    issues = []
    id_set = set()
    for d in dialogue:
        did = d.get("id")
        if not did:
            issues.append("Dialogue entry missing id.")
            continue
        if did in id_set:
            issues.append(f"Duplicate id: {did}")
        id_set.add(did)
        sp = (d.get("speaker") or "").strip()
        if sp and npc_names and sp not in npc_names:
            issues.append(f"{did}: unknown speaker '{sp}'")
        txt = (d.get("text") or "").strip()
        if not txt:
            issues.append(f"{did}: missing text")
        nxt = (d.get("next") or "").strip()
        if nxt and nxt not in all_ids:
            issues.append(f"{did}: next references unknown id '{nxt}'")
    # basic cycle/unreachable scan (optional light heuristic)
    # build edges
    edges = {d.get("id"): (d.get("next") or "").strip() for d in dialogue if d.get("id")}
    # detect tiny self-loop & dead-ends
    for k, v in edges.items():
        if v == k:
            issues.append(f"{k}: self-references as next")
    return issues

def validate_enemy(e: dict, item_names: List[str]) -> List[str]:
    issues = []
    if not (e.get("id") and e.get("name")):
        issues.append("Enemy missing id or name.")
    if e.get("tier") not in TIERS:
        issues.append(f"{e.get('id','?')}: invalid tier '{e.get('tier')}', expected {TIERS}")
    if e.get("element") not in ELEMENTS:
        issues.append(f"{e.get('id','?')}: invalid element '{e.get('element')}', expected {ELEMENTS}")
    for k in ("hp","speed"):
        v = e.get(k)
        if not isinstance(v, int) or v <= 0:
            issues.append(f"{e.get('id','?')}: {k} must be positive int")
    # drops
    for dr in e.get("drops", []):
        nm = (dr.get("id") or "").strip()
        if nm and nm not in item_names:
            issues.append(f"{e.get('id','?')}: drops unknown item '{nm}'")
        ch = dr.get("chance", 1)
        if not (isinstance(ch,(int,float)) and 0 <= ch <= 1):
            issues.append(f"{e.get('id','?')}: drop '{nm}' has chance outside [0..1]")
        qmin, qmax = dr.get("qty_min",1), dr.get("qty_max",1)
        if not (isinstance(qmin,int) and isinstance(qmax,int) and 1 <= qmin <= qmax):
            issues.append(f"{e.get('id','?')}: drop '{nm}' qty_min/max invalid")
    return issues
