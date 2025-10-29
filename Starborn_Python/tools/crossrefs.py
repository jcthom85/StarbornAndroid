#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations
from pathlib import Path
from typing import Any, Dict, List
import json

DATA_FILES = {
    "items": "items.json",
    "enemies": "enemies.json",
    "npcs": "npcs.json",
    "dialogue": "dialogue.json",
    "quests": "quests.json",
    "events": "events.json",
    "milestones": "milestones.json",
    "encounters": "encounters.json",
    "rooms": "rooms.json",
    "worlds": "worlds.json",
    "shops": "shops.json",
}

def _load(path: Path):
    if not path.exists(): return None
    with path.open("r", encoding="utf-8") as f: return json.load(f)

def load_all(project_root: Path) -> Dict[str, Any]:
    return {k: _load(project_root / v) for k, v in DATA_FILES.items()}

def _index(lst, key1="id", key2="name") -> Dict[str, dict]:
    if not isinstance(lst, list): return {}
    out = {}
    for o in lst:
        if not isinstance(o, dict): continue
        k = str(o.get(key1) or o.get(key2) or "")
        if k: out[k] = o
    return out

def validate_all(project_root: Path) -> List[dict]:
    D = load_all(project_root)
    out: List[dict] = []

    items = D.get("items") or []
    enemies = D.get("enemies") or []
    npcs = D.get("npcs") or []
    dialogue = D.get("dialogue") or []
    quests = D.get("quests") or []
    events = D.get("events") or []
    milestones = D.get("milestones") or []
    encounters = D.get("encounters") or []
    rooms = D.get("rooms") or []

    item_ids = set(_index(items).keys())
    enemy_ids = set(_index(enemies).keys())
    npc_ids   = set(_index(npcs).keys())
    dlg_ids   = set(_index(dialogue).keys())
    quest_ids = set(_index(quests).keys())
    ms_ids    = set(_index(milestones).keys())
    room_ids  = set(_index(rooms).keys())

    def _push(editor, severity, message, payload=None):
        out.append({"editor": editor, "severity": severity, "message": message, "payload": payload or {}})

    # Items: unique ids
    seen = set()
    for it in items:
        ident = str((it or {}).get("id") or (it or {}).get("name") or "").strip()
        if not ident:
            _push("Items", "error", "Item missing id/name.")
        elif ident in seen:
            _push("Items", "error", f"Duplicate item '{ident}'", {"goto":("item", ident)})
        else:
            seen.add(ident)

    # Enemies: drops reference items
    for e in enemies:
        eid = str((e or {}).get("id") or (e or {}).get("name") or "")
        for d in (e or {}).get("drops", []) or []:
            it = d.get("item") if isinstance(d, dict) else None
            if it and it not in item_ids:
                _push("Enemies", "error", f"Enemy '{eid}' drops unknown item '{it}'", {"goto":("enemy", eid)})

    # Dialogue: speaker exists; next exists
    for d in dialogue:
        did = str((d or {}).get("id") or "")
        sp  = (d or {}).get("speaker")
        if sp and sp not in npc_ids:
            _push("Dialogue", "error", f"Line '{did}' speaker '{sp}' not found", {"goto":("dialogue", did)})
        nxt = (d or {}).get("next")
        if isinstance(nxt, str) and nxt and nxt not in dlg_ids:
            _push("Dialogue", "error", f"Line '{did}' next '{nxt}' not found", {"goto":("dialogue", did)})
        if isinstance(nxt, list):
            for nid in nxt:
                if nid and nid not in dlg_ids:
                    _push("Dialogue", "error", f"Line '{did}' branch '{nid}' not found", {"goto":("dialogue", did)})

    # Quests: npc/item/quest refs
    for q in quests:
        qid = str((q or {}).get("id") or "")
        giver = (q or {}).get("giver") or (q or {}).get("npc")
        if giver and giver not in npc_ids:
            _push("Quests", "error", f"Quest '{qid}' giver '{giver}' not found", {"goto":("quest", qid)})
        receiver = (q or {}).get("receiver")
        if receiver and receiver not in npc_ids:
            _push("Quests", "error", f"Quest '{qid}' receiver '{receiver}' not found", {"goto":("quest", qid)})
        for key in ("requires_item","required_item","required_items"):
            v = (q or {}).get(key)
            if isinstance(v, str) and v and v not in item_ids:
                _push("Quests", "error", f"Quest '{qid}' requires unknown item '{v}'", {"goto":("quest", qid)})
            if isinstance(v, list):
                for rid in v:
                    if rid and rid not in item_ids:
                        _push("Quests", "error", f"Quest '{qid}' requires unknown item '{rid}'", {"goto":("quest", qid)})
        for key in ("reward_item","reward_items"):
            v = (q or {}).get(key)
            if isinstance(v, str) and v and v not in item_ids:
                _push("Quests", "error", f"Quest '{qid}' rewards unknown item '{v}'", {"goto":("quest", qid)})
            if isinstance(v, list):
                for rid in v:
                    if rid and rid not in item_ids:
                        _push("Quests", "error", f"Quest '{qid}' rewards unknown item '{rid}'", {"goto":("quest", qid)})
        for key in ("requires","requires_quests","prerequisites"):
            v = (q or {}).get(key)
            if isinstance(v, str) and v and v not in quest_ids:
                _push("Quests", "error", f"Quest '{qid}' requires unknown quest '{v}'", {"goto":("quest", qid)})
            if isinstance(v, list):
                for rq in v:
                    if rq and rq not in quest_ids:
                        _push("Quests", "error", f"Quest '{qid}' requires unknown quest '{rq}'", {"goto":("quest", qid)})

    # Events: best-effort payload refs
    for ev in events:
        evid = str((ev or {}).get("id") or "")
        def _chk(payload):
            if not isinstance(payload, dict): return
            for k in ("quest","quest_id","start_quest","complete_quest"):
                v = payload.get(k)
                if isinstance(v, str) and v and v not in quest_ids:
                    _push("Events", "error", f"Event '{evid}' unknown quest '{v}'", {"goto":("event", evid)})
            for k in ("item","item_id","give_item"):
                v = payload.get(k)
                if isinstance(v, str) and v and v not in item_ids:
                    _push("Events", "error", f"Event '{evid}' unknown item '{v}'", {"goto":("event", evid)})
            for k in ("room","room_id","target_room"):
                v = payload.get(k)
                if isinstance(v, str) and v and v not in room_ids:
                    _push("Events", "error", f"Event '{evid}' unknown room '{v}'", {"goto":("event", evid)})
        trg = (ev or {}).get("trigger", {})
        acts = (ev or {}).get("action") or (ev or {}).get("actions") or []
        _chk(trg)
        if isinstance(acts, dict): _chk(acts)
        if isinstance(acts, list):
            for a in acts: _chk(a if isinstance(a, dict) else {})

    # Encounters: enemies exist
    for en in encounters:
        enid = str((en or {}).get("id") or "")
        for w in (en or {}).get("waves", []) or []:
            for ent in (w or {}).get("enemies", []) or []:
                eid = ent if isinstance(ent, str) else ent.get("id") if isinstance(ent, dict) else None
                if eid and eid not in enemy_ids:
                    _push("Encounters", "error", f"Encounter '{enid}' unknown enemy '{eid}'", {"goto":("encounter", enid)})
    return out
