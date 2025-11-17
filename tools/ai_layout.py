# tools/ai_layout.py
# Centralized "AI Auto-Layout" generator for Starborn nodes.
# Uses AIService.complete_json with a strict schema, then normalizes/repairs output.

from __future__ import annotations
from typing import Any, Dict, List, Tuple, Optional
from dataclasses import dataclass, asdict
import re

# Local deps (ship alongside your tools)
from ai_service import AIService
from context_index import ContextIndex

DIRECTIONS = ("north", "south", "east", "west")

# X is standard: +x = east, -x = west
DX = {"east": 1, "west": -1, "north": 0, "south": 0}

# Y is UP in your world: +y = north, -y = south
DY = {"east": 0, "west": 0, "north": 1, "south": -1}


@dataclass
class LayoutOptions:
    target_rooms: int = 8
    style: str = "branching"        # linear|branching|labyrinth|ring|hub-and-spoke
    sprawl: int = 2                 # 0..3 (0 tight, 3 very sprawling)
    grid_step: int = 64
    allow_cycles: bool = True
    max_width: int = 8              # grid width (in cells), not pixels
    max_height: int = 8             # grid height (in cells)
    prefer_secrets: bool = True     # allow cul-de-sacs for secrets
    start_room_hint: str = ""       # optional human label (e.g., "Entrance")
    end_room_hint: str = ""         # optional human label (e.g., "Boss", "Exit")
    reuse_existing: bool = True     # allow reusing existing rooms in the node
    id_prefix: str = ""             # prefix for new room ids, default derived from node id

    def to_json(self) -> Dict[str, Any]:
        d = asdict(self)
        return d

# ----- JSON schema we ask the model to follow -----
ROOM_SCHEMA = {
    "id": "string",                    # unique in node
    "title": "string",
    "description": "string",
    "env": "string?",
    "pos": ["int","int"],              # grid coords in cells (not pixels)
    "connections": {                   # NSEW only, to other room ids
        "north": "string?",
        "south": "string?",
        "east":  "string?",
        "west":  "string?"
    },
    "tags": ["string?"],               # e.g., ["entrance","boss","secret"]
}

SCHEMA_HINT = {
    "rooms": [ROOM_SCHEMA],
    "meta": {
        "notes": "string?",
        "starting_room_id": "string?",
        "boss_room_id": "string?",
        "exit_room_id": "string?"
    }
}

SYSTEM_GOAL = (
    "Design a creative, play-tested-feeling room layout for a single NODE in a JRPG-like text exploration game. "
    "The layout must use strict NSEW connections that match adjacent grid coordinates. "
    "Favor readability, a couple of loops if allowed, and thematic variety that fits the node context."
)

def _slug(s: str) -> str:
    return re.sub(r"[^a-zA-Z0-9]+", "_", s.strip().lower()).strip("_") or "r"

def _ensure_unique_id(base: str, used: set[str]) -> str:
    if base not in used:
        used.add(base)
        return base
    i = 2
    while True:
        cand = f"{base}_{i}"
        if cand not in used:
            used.add(cand)
            return cand
        i += 1

def _normalize_model_output(obj: Dict[str, Any]) -> Dict[str, Any]:
    rooms = obj.get("rooms") or []
    meta = obj.get("meta") or {}
    norm: List[Dict[str,Any]] = []
    for r in rooms:
        if not isinstance(r, dict): 
            continue
        rid = str(r.get("id") or "").strip()
        if not rid: 
            rid = "room"
        title = str(r.get("title") or rid).strip()
        desc = str(r.get("description") or "").strip()
        pos = r.get("pos") or [0,0]
        if not (isinstance(pos, (list,tuple)) and len(pos) == 2):
            pos = [0,0]
        x,y = int(pos[0]), int(pos[1])
        con = r.get("connections") or {}
        if not isinstance(con, dict): 
            con = {}
        con = {k:str(v) for k,v in con.items() if k in DIRECTIONS and v}
        tags = r.get("tags") or []
        if isinstance(tags, list):
            tags = [str(t) for t in tags if t]
        else:
            tags = []
        env = r.get("env") or ""
        norm.append({
            "id": rid, "title": title, "description": desc, "env": env,
            "pos": [x,y], "connections": con, "tags": tags
        })
    return {"rooms": norm, "meta": meta}

def _symmetrize_connections(rooms: List[Dict[str,Any]]) -> None:
    # Ensure A->B implies B->A in the correct opposite direction
    by_id = {r["id"]: r for r in rooms}
    for r in rooms:
        con = r.get("connections") or {}
        for d, tid in list(con.items()):
            if tid not in by_id:
                # drop dangling
                con.pop(d, None)
                continue
            back = {"north":"south","south":"north","east":"west","west":"east"}[d]
            t = by_id[tid]
            t.setdefault("connections", {})
            if t["connections"].get(back) != r["id"]:
                t["connections"][back] = r["id"]

def _enforce_grid_adjacency(rooms: List[Dict[str,Any]]) -> None:
    pos = {r["id"]: (int(r["pos"][0]), int(r["pos"][1])) for r in rooms}
    for r in rooms:
        cx, cy = pos[r["id"]]
        con = r.get("connections") or {}
        for d, tid in list(con.items()):
            if tid not in pos:
                con.pop(d, None); continue
            tx, ty = pos[tid]
            ok = (
                (d == "north" and tx == cx and ty == cy + 1) or   # +y is north
                (d == "south" and tx == cx and ty == cy - 1) or   # -y is south
                (d == "east"  and tx == cx + 1 and ty == cy) or
                (d == "west"  and tx == cx - 1 and ty == cy)
            )
            if not ok:
                con.pop(d, None)
        r["connections"] = con
    _symmetrize_connections(rooms)

def _autowire_neighbors_if_missing(rooms: List[Dict[str,Any]]) -> None:
    by_pos = {(int(r["pos"][0]), int(r["pos"][1])): r for r in rooms}

    def link(a: dict, b: dict):
        ax, ay = int(a["pos"][0]), int(a["pos"][1])
        bx, by = int(b["pos"][0]), int(b["pos"][1])
        dx, dy = (bx - ax), (by - ay)
        if abs(dx) + abs(dy) != 1:
            return
        # Y is UP: +dy = north, -dy = south
        if dx == 1:        d1, d2 = "east",  "west"
        elif dx == -1:     d1, d2 = "west",  "east"
        elif dy == 1:      d1, d2 = "north", "south"
        else:              d1, d2 = "south", "north"
        a.setdefault("connections", {}); b.setdefault("connections", {})
        a["connections"][d1] = b["id"];   b["connections"][d2] = a["id"]

    for (gx, gy), a in by_pos.items():
        for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
            b = by_pos.get((gx+dx, gy+dy))
            if b: link(a, b)

def _grid_pack(layout: Dict[str,Any], grid_step: int) -> Dict[str,Any]:
    # Return world units (cells). The editor multiplies by GRID_STEP when drawing.
    out = {"rooms": [], "meta": layout.get("meta", {})}
    for r in layout["rooms"]:
        x, y = int(r["pos"][0]), int(r["pos"][1])
        rr = dict(r)
        rr["pos"] = [float(x), float(y)]  # <-- world units, not pixels
        out["rooms"].append(rr)
    return out

def generate_node_layout(cx: ContextIndex, node_id: str, options: Optional[LayoutOptions] = None, model: str = "gpt-5-mini", use_openai: bool = True) -> Dict[str,Any]:
    """
    Returns a normalized layout object:
    {
      "rooms": [ {id,title,description,env,pos:[px,py],connections{...},tags[]}... ],
      "meta": {...}
    }
    """
    options = options or LayoutOptions()
    pack = cx.pack_for_node(str(node_id))

    # Build task instruction to steer model
    task = (
        f"Create a {options.style} room map with about {options.target_rooms} rooms for the NODE shown in PACK. "
        f"Use a {options.max_width}x{options.max_height} grid of CELLS (not pixels). "
        f"Sprawl={options.sprawl} (0 tight, 3 very sprawling). "
        f"{'Allow cycles/loops.' if options.allow_cycles else 'Avoid cycles; keep it a tree.'} "
        "Constraints:\n"
        "- Coordinates in rooms[*].pos are CELL coordinates (integers), not pixel values.\n"
        "- Each NSEW link must go to the cell that is exactly one step in that direction.\n"
        "- Do not output diagonal links.\n"
        "- All room ids must be unique within this layout.\n"
        "- Use tags like ['entrance','exit','boss','secret'] when relevant.\n"
        "If this node is small, fewer rooms are fine; if it is expansive, lean into it. Be creative but playable."
    )

    svc = AIService(use_openai=use_openai, model=model, temperature=0.7)
    obj, _raw = svc.complete_json(
        system_goal=SYSTEM_GOAL,
        json_schema_hint=SCHEMA_HINT,
        task_instructions=task,
        context_pack=pack,
    )

    if not isinstance(obj, dict):
        raise RuntimeError("Model did not return a JSON object.")

    layout = _normalize_model_output(obj)

    # ---- ensure env is set on every room (fallback to node id) ----
    for r in layout["rooms"]:
        if not r.get("env"):
            r["env"] = str(node_id)

    # Normalize ids (prefix) and ensure uniqueness
    node_slug = _slug(str(node_id))
    prefix = options.id_prefix or f"n_{node_slug}"
    used: set[str] = set()
    for r in layout["rooms"]:
        base = _slug(r["id"]) or "r"
        r["id"] = _ensure_unique_id(f"{prefix}_{base}", used)

    # Enforce constraints (adjacency/symmetry), auto-wire if empty
    _enforce_grid_adjacency(layout["rooms"])
    if sum(len((r.get("connections") or {})) for r in layout["rooms"]) == 0:
        _autowire_neighbors_if_missing(layout["rooms"])

    # Convert to pixel coords matching rooms.json convention
    return _grid_pack(layout, options.grid_step)
