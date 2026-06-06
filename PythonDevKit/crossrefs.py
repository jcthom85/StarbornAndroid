#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations
from pathlib import Path
from typing import Any, Dict, List
import json

from devkit_paths import resolve_paths

DATA_FILES = {
    "items": "items.json",
    "enemies": "enemies.json",
    "npcs": "npcs.json",
    "dialogue": "dialogue.json",
    "quests": "quests.json",
    "events": "events.json",
    "milestones": "milestones.json",
    "encounters": "encounters.json",
    "cinematics": "cinematics.json",
    "rooms": "rooms.json",
    "worlds": "worlds.json",
    "shops": "shops.json",
}

def _load(path: Path):
    if not path.exists(): return None
    with path.open("r", encoding="utf-8") as f: return json.load(f)

def load_all(project_root: Path | None = None) -> Dict[str, Any]:
    paths = resolve_paths(project_root or Path(__file__).parent)
    root = paths.assets_dir
    return {k: _load(root / v) for k, v in DATA_FILES.items()}

def _index(lst, key1="id", key2="name") -> Dict[str, dict]:
    if not isinstance(lst, list): return {}
    out = {}
    for o in lst:
        if not isinstance(o, dict): continue
        k = str(o.get(key1) or o.get(key2) or "")
        if k: out[k] = o
    return out

def validate_all(project_root: Path | None = None) -> List[dict]:
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
    cinematics = D.get("cinematics") or []

    item_ids = set(_index(items).keys())
    enemy_ids = set(_index(enemies).keys())
    npc_ids   = set(_index(npcs).keys())
    dlg_ids   = set(_index(dialogue).keys())
    quest_ids = set(_index(quests).keys())
    ms_ids    = set(_index(milestones).keys())
    room_ids  = set(_index(rooms).keys())
    cin_ids   = set(_index(cinematics).keys())
    enc_ids   = set(_index(encounters).keys())

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
        opts = (d or {}).get("options") or []
        if isinstance(opts, list):
            for idx, opt in enumerate(opts):
                if isinstance(opt, dict):
                    onext = opt.get("next")
                    if isinstance(onext, str) and onext and onext not in dlg_ids:
                        _push("Dialogue", "error", f"Line '{did}' option[{idx}] next '{onext}' not found", {"goto":("dialogue", did)})

    # Quests: npc/item/quest refs
    for q in quests:
        qid = str((q or {}).get("id") or "")
        giver = (q or {}).get("giver") or (q or {}).get("npc")
        if giver and giver not in npc_ids:
            _push("Quests", "error", f"Quest '{qid}' giver '{giver}' not found", {"goto":("quest", qid)})
        prereq = (q or {}).get("prereq_quest_id")
        if prereq and prereq not in quest_ids:
            _push("Quests", "error", f"Quest '{qid}' prereq '{prereq}' not found", {"goto":("quest", qid)})
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
        # Rewards (new schema)
        rewards = (q or {}).get("rewards") or []
        if isinstance(rewards, list):
            for ridx, r in enumerate(rewards):
                if isinstance(r, dict):
                    rtype = r.get("type")
                    if rtype == "item":
                        iid = r.get("item_id")
                        if iid and iid not in item_ids:
                            _push("Quests", "error", f"Quest '{qid}' reward[{ridx}] unknown item '{iid}'", {"goto":("quest", qid)})
                    elif rtype == "xp":
                        amt = r.get("amount")
                        if amt is None:
                            _push("Quests", "error", f"Quest '{qid}' reward[{ridx}] missing xp amount", {"goto":("quest", qid)})
                    elif rtype:
                        _push("Quests", "error", f"Quest '{qid}' reward[{ridx}] unknown type '{rtype}'", {"goto":("quest", qid)})

    # Events: best-effort payload refs
    for ev in events:
        evid = str((ev or {}).get("id") or "")
        def _chk_payload(payload):
            if not isinstance(payload, dict):
                return
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
            for k in ("encounter_id",):
                v = payload.get(k)
                if isinstance(v, str) and v and v not in enc_ids:
                    _push("Events", "error", f"Event '{evid}' unknown encounter '{v}'", {"goto":("event", evid)})
            for k in ("scene_id","cutscene_id"):
                v = payload.get(k)
                if isinstance(v, str) and v and v not in cin_ids:
                    _push("Events", "error", f"Event '{evid}' unknown cinematic '{v}'", {"goto":("event", evid)})
            for k in ("milestone",):
                v = payload.get(k)
                if isinstance(v, str) and v and v not in ms_ids:
                    _push("Events", "error", f"Event '{evid}' unknown milestone '{v}'", {"goto":("event", evid)})
            for k in ("milestones",):
                v = payload.get(k)
                if isinstance(v, list):
                    for mid in v:
                        if mid and mid not in ms_ids:
                            _push("Events", "error", f"Event '{evid}' unknown milestone '{mid}'", {"goto":("event", evid)})
            # reward items array
            if isinstance(payload.get("items"), list):
                for it in payload.get("items") or []:
                    if isinstance(it, dict):
                        iid = it.get("item_id")
                        if iid and iid not in item_ids:
                            _push("Events", "error", f"Event '{evid}' unknown item '{iid}'", {"goto":("event", evid)})

        def _walk(payload):
            if isinstance(payload, dict):
                _chk_payload(payload)
                for key in ("do","else","on_complete"):
                    if isinstance(payload.get(key), list):
                        for child in payload[key]:
                            _walk(child)
            elif isinstance(payload, list):
                for row in payload:
                    _walk(row)

        trg = (ev or {}).get("trigger", {})
        _walk(trg)
        conds = (ev or {}).get("conditions") or []
        if isinstance(conds, list):
            for c in conds:
                if isinstance(c, dict):
                    ctype = c.get("type")
                    if ctype in ("milestone","milestone_set","milestone_not_set") and c.get("milestone") and c.get("milestone") not in ms_ids:
                        _push("Events", "error", f"Event '{evid}' unknown milestone '{c.get('milestone')}'", {"goto":("event", evid)})
                    if c.get("quest_id") and c.get("quest_id") not in quest_ids:
                        _push("Events", "error", f"Event '{evid}' unknown quest '{c.get('quest_id')}'", {"goto":("event", evid)})
                    if c.get("item_id") and c.get("item_id") not in item_ids:
                        _push("Events", "error", f"Event '{evid}' unknown item '{c.get('item_id')}'", {"goto":("event", evid)})
        acts = (ev or {}).get("action") or (ev or {}).get("actions") or []
        _walk(acts)

    # Encounters: enemies exist
    for en in encounters:
        enid = str((en or {}).get("id") or "")
        for w in (en or {}).get("waves", []) or []:
            for ent in (w or {}).get("enemies", []) or []:
                eid = ent if isinstance(ent, str) else ent.get("id") if isinstance(ent, dict) else None
                if eid and eid not in enemy_ids:
                    _push("Encounters", "error", f"Encounter '{enid}' unknown enemy '{eid}'", {"goto":("encounter", enid)})

    # Schema validation (if jsonschema is installed)
    _validate_schemas(project_root, D, out)

    return out


# ---------- Schema validation ----------

_SCHEMA_MAP = {
    "dialogue.schema.json": "dialogue",
    "events.schema.json": "events",
    "quests.schema.json": "quests",
}


def _validate_schemas(project_root: Path | None, data: dict, out: list):
    try:
        from jsonschema import Draft202012Validator
    except ImportError:
        out.append({
            "editor": "Schema",
            "severity": "warning",
            "message": "jsonschema not installed — schema validation skipped. Run: pip install jsonschema",
            "payload": {},
        })
        return

    paths = resolve_paths(project_root or Path(__file__).parent)
    schemas_dir = paths.project_root / "docs" / "schemas"
    if not schemas_dir.is_dir():
        return

    for schema_file, data_key in _SCHEMA_MAP.items():
        schema_path = schemas_dir / schema_file
        if not schema_path.exists():
            continue
        file_data = data.get(data_key)
        if file_data is None:
            continue
        try:
            schema = json.loads(schema_path.read_text(encoding="utf-8"))
        except Exception:
            continue

        validator = Draft202012Validator(schema)
        for error in validator.iter_errors(file_data):
            path_str = " > ".join(str(p) for p in error.absolute_path) or "(root)"
            # Try to extract entry ID for goto payload
            entry_id = ""
            if len(error.absolute_path) >= 1:
                idx = error.absolute_path[0]
                if isinstance(idx, int) and isinstance(file_data, list) and idx < len(file_data):
                    entry = file_data[idx]
                    if isinstance(entry, dict):
                        entry_id = str(entry.get("id", ""))
            editor_name = data_key.title()
            goto_type = data_key if data_key == "dialogue" else data_key.rstrip("s")
            out.append({
                "editor": f"Schema:{editor_name}",
                "severity": "error",
                "message": f"[{data_key}.json] {path_str}: {error.message}",
                "payload": {"goto": (goto_type, entry_id)} if entry_id else {},
            })
