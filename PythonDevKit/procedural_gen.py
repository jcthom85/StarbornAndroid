#!/usr/bin/env python3
"""
Starborn — Procedural Generation Module (v1.0)
==============================================

What this file provides
-----------------------
• A self‑contained procedural generation backend + a PyQt5 dialog for inputs.
• Two modes:
    1) Full Node Generation — create a brand new node + a bundle of rooms.
    2) Cluster Generation  — add a room cluster to an existing node inside a bounding box.
• Optional OpenAI usage for titles/descriptions (gracefully falls back if not configured).
• Clean integration hook: call `open_procgen_dialog(world_editor_instance)` from world_editor.py.

Placement
---------
Put this file alongside your editors (e.g. Starborn/tools/procedural_gen.py).

Integration (world_editor.py)
-----------------------------
1) Add a button in the top controls:
    self.procgen_btn = QPushButton("✨ Procedural Gen…")
    self.procgen_btn.clicked.connect(self._open_procgen_dialog)
    top_ctrl_layout.addWidget(self.procgen_btn)

2) Add this method on WorldBuilder:
    def _open_procgen_dialog(self):
        try:
            from procedural_gen import open_procgen_dialog
        except Exception as e:
            QMessageBox.critical(self, "Procedural Gen", f"Failed to load: {e}")
            return
        open_procgen_dialog(self)

3) No other edits required. The dialog will read data from your editor and apply changes in‑memory,
   then mark the project dirty so Save All writes hub_nodes.json/rooms.json.

Notes
-----
• Uses your editor’s internal helpers when possible:
    - `_auto_create_room_at(node_id, x, y, template=None, base_title=None)`
    - `quick_connect_rooms(room_id_a, room_id_b)`
    - `_set_dirty(True)`, `_rebuild_tree()`, `canvas.update()`
• Creates connections with cardinal directions based on relative positions (via quick_connect_rooms).
• Respects your node room id scheme (`node_id_N`).

"""

from __future__ import annotations
import os, math, random, traceback
from dataclasses import dataclass
import json
from pathlib import Path
from typing import Any, Dict, List, Tuple, Optional

from devkit_paths import resolve_paths

# --- JSON-level helper for the AI Assistant (no editor instance required) ---------
# Reuses this module's pattern generators + AITextClient. Produces a pure payload
# that the assistant can diff/preview before writing rooms.json/hub_nodes.json.

def _pg_read_list(path: Path) -> list:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return []

def _pg_find_root(start: Path) -> Path:
    return resolve_paths(start).assets_dir

def _pg_next_room_index(node_id: str, rooms: List[Dict[str, Any]]) -> int:
    # existing room ids look like "<node>_<n>"
    import re
    pat = re.compile(rf"^{str(node_id)}_(\d+)$")
    mx = 0
    for r in rooms:
        rid = str((r or {}).get("id") or "")
        m = pat.match(rid)
        if m:
            try:
                mx = max(mx, int(m.group(1)))
            except Exception:
                pass
    return mx + 1

def _pg_dir_from(ax: int, ay: int, bx: int, by: int) -> Optional[str]:
    if bx == ax + 1 and by == ay: return "east"
    if bx == ax - 1 and by == ay: return "west"
    if by == ay + 1 and bx == ax: return "south"
    if by == ay - 1 and bx == ax: return "north"
    return None

def compute_cluster_payload(project_root: str | Path,
                            node_id: str,
                            *,
                            count: int = 6,
                            pattern: str = "loop",
                            theme: str = "",
                            use_ai: bool = False,
                            seed: Optional[int] = None,
                            model: Optional[str] = None) -> Dict[str, Any]:
    """
    Pure function (file-read only) that returns a JSON-ready cluster for node_id:

    {
      "rooms": [ {id,title,description,pos:[x,y],connections:{rid:dir,...}}, ... ],
      "node_append_rooms": ["<rid>", ...]
    }

    The caller (AI Assistant) will merge + diff these into rooms.json and hub_nodes.json.
    """
    # Ensure required globals exist in this module:
    # - PATTERNS, _pattern_positions_edges: your layout logic
    # - AITextClient: your title/description pass (optional via use_ai)

    root = _pg_find_root(Path(project_root) if project_root else Path(__file__).parent)
    rooms_path = root / "rooms.json"
    nodes_path = root / "hub_nodes.json"

    all_rooms: List[Dict[str, Any]] = _pg_read_list(rooms_path)
    all_nodes: List[Dict[str, Any]] = _pg_read_list(nodes_path)
    node_map: Dict[str, Dict[str, Any]] = {str((n or {}).get("id")): n for n in all_nodes}
    node = node_map.get(str(node_id))
    if not node:
        raise ValueError(f"Node '{node_id}' not found in hub_nodes.json.")

    # Use your existing pattern engine
    if pattern not in PATTERNS:
        pattern = "loop"
    grid_positions: List[Tuple[int,int]]
    edges: List[Tuple[int,int]]
    grid_positions, edges = _pattern_positions_edges(pattern, max(1, int(count)))

    # Normalize positions onto a 1-grid
    pos_abs: List[Tuple[int,int]] = [(int(x), int(y)) for (x, y) in grid_positions]

    rng = random.Random(seed if seed is not None else (hash(str(node_id)) & 0xFFFFFFFF))

    # Your AI text client (respects the model if provided)
    try:
        ai = AITextClient(enabled=bool(use_ai), model=(model or "gpt-5-mini"))
    except TypeError:
        # Fallback in case your AITextClient doesn't accept model yet
        ai = AITextClient(enabled=bool(use_ai))

    # Determine next index for this node
    base_idx = _pg_next_room_index(str(node_id), all_rooms)

    rooms_new: List[Dict[str, Any]] = []
    to_append: List[str] = []

    # Make rooms
    for i, (gx, gy) in enumerate(pos_abs, start=0):
        idx = base_idx + i
        rid = f"{node_id}_{idx}"
        title, desc = ai.room_title_desc(theme or str(node.get('title') or node_id).title(), i+1)
        room: Dict[str, Any] = {
            "id": rid,
            "title": title,
            "description": desc,
            "pos": [gx, gy],
            "connections": {}
        }
        rooms_new.append(room)
        to_append.append(rid)

    # Wire connections using edges (indexes in rooms_new)
    for (a, b) in edges:
        if 0 <= a < len(rooms_new) and 0 <= b < len(rooms_new):
            ax, ay = rooms_new[a]["pos"]; bx, by = rooms_new[b]["pos"]
            da = _pg_dir_from(ax, ay, bx, by)
            db = _pg_dir_from(bx, by, ax, ay)
            if da: rooms_new[a]["connections"][rooms_new[b]["id"]] = da
            if db: rooms_new[b]["connections"][rooms_new[a]["id"]] = db

    return {"rooms": rooms_new, "node_append_rooms": to_append}

# ---- Optional OpenAI text support --------------------------------------------------

class AITextClient:
    """
    Minimal wrapper that *optionally* uses OpenAI for titles/descriptions.
    Set OPENAI_API_KEY env var to enable. If not set (or errors), returns sane fallbacks.
    """
    def __init__(self, enabled: bool, model: str = "gpt-5-mini"):
        self.enabled = bool(enabled and os.environ.get("OPENAI_API_KEY"))
        self.model = model or "gpt-5-mini"
        self._client = None
        if self.enabled:
            try:
                # Lazy import to avoid hard dependency
                from openai import OpenAI  # type: ignore
                self._client = OpenAI()
            except Exception:
                self.enabled = False

    def room_title_desc(self, theme: str, room_index: int) -> Tuple[str, str]:
        # deterministic fallback if not enabled
        t = (theme or "Area").strip() or "Area"
        if not self.enabled or self._client is None:
            return (f"{t} — Room {room_index}",
                    f"A section of the {t.lower()} with signs of recent activity.")

        try:
            sys_msg = (
                "You help generate room titles+descriptions for a game. "
                "Return ONLY strict JSON with exactly two string fields: "
                '{"title": "...", "description": "..."}. '
                "Keep description to one or two short sentences."
            )
            user_msg = f"Theme/context: {t!r}. Room index: {room_index}."

            # Newer SDK supports response_format json_object; fall back is still parsed below if needed.
            resp = self._client.chat.completions.create(  # type: ignore[attr-defined]
                model=self.model,
                messages=[
                    {"role": "system", "content": sys_msg},
                    {"role": "user", "content": user_msg},
                ],
                temperature=0.6,
                response_format={"type": "json_object"},  # ignore if model doesn't support; server will handle
            )
            text = (resp.choices[0].message.content or "").strip()  # type: ignore[index]

            # Parse JSON robustly
            try:
                data = json.loads(text)
            except Exception:
                # fallback: extract the first {...} block
                start = text.find("{"); end = text.rfind("}")
                if start != -1 and end > start:
                    data = json.loads(text[start:end+1])
                else:
                    data = {}

            title = str(data.get("title") or "").strip()
            desc  = str(data.get("description") or "").strip()

            if not title:
                title = f"{t} — Room {room_index}"
            if not desc:
                desc = f"A section of the {t.lower()} with echoing footsteps."

            # keep it tidy
            if len(title) > 96:  title = title[:93] + "…"
            if len(desc)  > 240: desc  = desc[:237] + "…"

            return title, desc

        except Exception:
            return (f"{t} — Room {room_index}",
                    f"A section of the {t.lower()} with echoing footsteps.")
        
# ---- Graph + layout primitives -----------------------------------------------------

@dataclass
class ProcGenParams:
    mode: str                        # "full_node" | "cluster"
    hub_id: Optional[str] = None     # required for full_node
    node_id: Optional[str] = None    # required for cluster (target node)
    new_node_id: Optional[str] = None
    new_node_title: Optional[str] = None
    theme: str = ""
    pattern: str = "linear"          # linear|branch|hub|loop|maze
    room_count: int = 8
    use_ai: bool = False
    # cluster bounds
    bbox_x: int = 0
    bbox_y: int = 0
    bbox_w: int = 20
    bbox_h: int = 10
    attach_room_id: Optional[str] = None  # where to connect cluster to existing node

@dataclass
class ProcGenOutput:
    new_node_id: Optional[str]
    new_room_ids: List[str]

# ---------- Pattern generators (return positions + undirected edges) ----------------

def gen_linear(n: int) -> Tuple[List[Tuple[int,int]], List[Tuple[int,int]]]:
    pos = [(i, 0) for i in range(n)]
    edges = [(i, i+1) for i in range(n-1)]
    return pos, edges

def gen_branch(n: int, branch_prob: float = 0.35) -> Tuple[List[Tuple[int,int]], List[Tuple[int,int]]]:
    if n <= 2: return gen_linear(n)
    trunk_len = max(2, n // 2)
    pos = [(i, 0) for i in range(trunk_len)]
    edges = [(i, i+1) for i in range(trunk_len-1)]
    # Add branches off trunk
    idx = trunk_len
    for i in range(trunk_len):
        if idx >= n: break
        if random.random() < branch_prob:
            # place branch up or down at x=i, y=±1
            y = 1 if (i % 2 == 0) else -1
            pos.append((i, y))
            edges.append((i, idx))
            idx += 1
    # If still need rooms, extend the trunk
    while idx < n:
        pos.append((idx, 0))
        edges.append((idx-1, idx))
        idx += 1
    return pos, edges

def gen_hub(n: int, spokes: int = 4) -> Tuple[List[Tuple[int,int]], List[Tuple[int,int]]]:
    if n <= 1: return ([(0,0)], [])
    hub_pos = (0,0)
    pos = [hub_pos]
    edges: List[Tuple[int,int]] = []
    # Distribute remaining nodes across spokes (N,S,E,W)
    dirs = [(1,0), (-1,0), (0,1), (0,-1)]
    spokes = max(1, min(spokes, len(dirs)))
    remaining = n - 1
    # allocate roughly evenly
    alloc = [remaining // spokes] * spokes
    for i in range(remaining % spokes):
        alloc[i] += 1
    idx = 1
    for s, (dx,dy) in enumerate(dirs[:spokes]):
        last = 0  # hub index
        for k in range(alloc[s]):
            pos.append((dx*(k+1), dy*(k+1)))
            edges.append((last, idx))
            last = idx
            idx += 1
    return pos, edges

def gen_loop(n: int) -> Tuple[List[Tuple[int,int]], List[Tuple[int,int]]]:
    if n <= 2: return gen_linear(n)
    # simple square/rectangle loop when possible
    side = max(2, int(math.sqrt(n)))
    pos: List[Tuple[int,int]] = []
    edges: List[Tuple[int,int]] = []
    # build rectangle perimeter
    coords = []
    w, h = side, max(2, (n + side - 1) // side)
    # generate a ring path up to n nodes
    x,y = 0,0
    path = []
    # rectangle perimeter coordinates
    perim = []
    for i in range(w): perim.append((i,0))
    for j in range(1,h): perim.append((w-1,j))
    for i in range(w-2, -1, -1): perim.append((i,h-1))
    for j in range(h-2, 0, -1): perim.append((0,j))
    perim = perim[:n]
    pos = perim
    edges = [(i, (i+1) % len(pos)) for i in range(len(pos))]
    return pos, edges

def gen_maze(n: int) -> Tuple[List[Tuple[int,int]], List[Tuple[int,int]]]:
    """
    Very lightweight imperfect grid maze: carve a spanning tree then add a couple extra edges.
    We carve cells in a grid sized ~sqrt(n) and return first n cells.
    """
    side = max(2, int(math.sqrt(n)))
    w = h = side
    # list of grid cells
    cells = [(x,y) for y in range(h) for x in range(w)]
    random.shuffle(cells)
    cells = cells[:n]
    # Build a naive tree by linking to nearest previously placed cell
    pos = [cells[0]]
    edges: List[Tuple[int,int]] = []
    for i in range(1, len(cells)):
        # connect to nearest previous cell
        (cx,cy) = cells[i]
        nearest = min(range(i), key=lambda j: abs(cells[j][0]-cx)+abs(cells[j][1]-cy))
        edges.append((nearest, i))
        pos.append(cells[i])
    # Add a couple extra edges to create loops
    for _ in range(max(1, n//8)):
        a, b = random.sample(range(len(pos)), 2)
        if a != b and (a,b) not in edges and (b,a) not in edges:
            edges.append((a,b))
    return pos, edges

PATTERNS = {
    "linear": gen_linear,
    "branch": gen_branch,
    "hub":    gen_hub,
    "loop":   gen_loop,
    "maze":   gen_maze,
}

# ---- Coordinate fitting ------------------------------------------------------------

def fit_positions_into_bbox(pts: List[Tuple[int,int]], x: int, y: int, w: int, h: int) -> List[Tuple[int,int]]:
    """
    Normalize pts to start around (0,0), scale (if needed) and translate to fit into bbox.
    For our node canvas, 'scale' is just spacing (grid step). We'll spread across the bbox extents.
    """
    if not pts: return []
    xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
    minx, maxx = min(xs), max(xs); miny, maxy = min(ys), max(ys)
    spanx = max(1, maxx - minx); spany = max(1, maxy - miny)

    # Number of steps available inside bbox (minus a small margin)
    steps_x = max(1, w - 2); steps_y = max(1, h - 2)

    def map_pt(px, py):
        nx = (px - minx) / spanx if spanx else 0.5
        ny = (py - miny) / spany if spany else 0.5
        gx = x + 1 + int(round(nx * steps_x))
        gy = y + 1 + int(round(ny * steps_y))
        return (gx, gy)

    return [map_pt(px, py) for (px,py) in pts]

# ---- Application to Editor ---------------------------------------------------------

def _unique_node_id(base: str, existing: Dict[str, dict]) -> str:
    base = (base or "node").strip().lower().replace(" ", "_")
    if base not in existing: return base
    i = 2
    while f"{base}_{i}" in existing:
        i += 1
    return f"{base}_{i}"

def _pattern_positions_edges(pattern: str, n: int) -> Tuple[List[Tuple[int,int]], List[Tuple[int,int]]]:
    fn = PATTERNS.get(pattern, gen_linear)
    return fn(max(1, int(n)))

def _pick_bbox_around(editor, attach_rid: str, w: int, h: int) -> Tuple[int,int,int,int]:
    """If user didn't enter bbox, place cluster near the attachment room."""
    room = editor.rooms_by_id.get(attach_rid, {})
    px, py = (room.get("pos") or [0,0])
    return int(px - w//4), int(py - h//4), int(w), int(h)

def apply_full_node(editor, params: ProcGenParams) -> ProcGenOutput:
    assert params.hub_id, "hub_id required for full node"
    # Resolve node id + title
    node_id = (params.new_node_id or params.theme or "new_node").strip().lower().replace(" ", "_")
    node_id = _unique_node_id(node_id, editor.nodes_by_id)
    node_title = params.new_node_title or (params.theme.title() if params.theme else node_id.title())
    # Create node entry
    node = {
        "id": node_id,
        "hub_id": params.hub_id,
        "title": node_title,
        "entry_room": "",
        "icon_image": "",  # user can change later
        "pos_hint": {"center_x": 0.5, "center_y": 0.5},
        "size": [256, 256],
        "rooms": []
    }
    editor.nodes_by_id[node_id] = node

    # Build graph & place positions
    rel_pos, edges = _pattern_positions_edges(params.pattern, params.room_count)
    # Fit into a sensible default bbox near origin
    abs_pos = fit_positions_into_bbox(rel_pos, x=0, y=0, w=max(10, params.room_count), h=max(6, params.room_count//2))

    ai = AITextClient(enabled=params.use_ai)
    new_room_ids: List[str] = []
    # Create rooms
    for i, (wx,wy) in enumerate(abs_pos, start=1):
        title, desc = ai.room_title_desc(params.theme or node_title, i)
        rid = editor._auto_create_room_at(node_id, wx, wy, template=None, base_title=title)
        room = editor.rooms_by_id.get(rid, {})
        room["description"] = desc
        # ensure env
        room["env"] = node_id
        editor.rooms_by_id[rid] = room
        new_room_ids.append(rid)

    # Connect per edges
    for (a,b) in edges:
        if a < len(new_room_ids) and b < len(new_room_ids):
            editor.quick_connect_rooms(new_room_ids[a], new_room_ids[b])

    # Set entry room
    if new_room_ids and node.get("entry_room") in ("", None):
        node["entry_room"] = new_room_ids[0]

    # Finalize
    editor._set_dirty(True)
    try:
        editor._rebuild_tree()
        editor.canvas.update()
    except Exception:
        pass
    return ProcGenOutput(new_node_id=node_id, new_room_ids=new_room_ids)

def apply_cluster(editor, params: ProcGenParams) -> ProcGenOutput:
    assert params.node_id, "node_id required for cluster mode"
    node_id = params.node_id
    # Determine bbox
    if params.bbox_w <= 0 or params.bbox_h <= 0:
        bx, by, bw, bh = _pick_bbox_around(editor, params.attach_room_id or "", 12, 8)
    else:
        bx, by, bw, bh = params.bbox_x, params.bbox_y, params.bbox_w, params.bbox_h

    rel_pos, edges = _pattern_positions_edges(params.pattern, params.room_count)
    abs_pos = fit_positions_into_bbox(rel_pos, bx, by, bw, bh)

    ai = AITextClient(enabled=params.use_ai)
    new_room_ids: List[str] = []
    for i, (wx,wy) in enumerate(abs_pos, start=1):
        title, desc = ai.room_title_desc(params.theme or node_id.title(), i)
        rid = editor._auto_create_room_at(node_id, wx, wy, template=None, base_title=title)
        room = editor.rooms_by_id.get(rid, {})
        room["description"] = desc
        room["env"] = node_id
        editor.rooms_by_id[rid] = room
        new_room_ids.append(rid)

    for (a,b) in edges:
        if a < len(new_room_ids) and b < len(new_room_ids):
            editor.quick_connect_rooms(new_room_ids[a], new_room_ids[b])

    # Attach to existing room if provided
    if params.attach_room_id:
        # connect the *first* generated room to the attachment
        if new_room_ids:
            editor.quick_connect_rooms(params.attach_room_id, new_room_ids[0])

    editor._set_dirty(True)
    try:
        editor._rebuild_tree()
        editor.canvas.update()
    except Exception:
        pass
    return ProcGenOutput(new_node_id=None, new_room_ids=new_room_ids)

# ---- PyQt5 Dialog (UI wrapper) -----------------------------------------------------

def _get_qt():
    # Late import to keep this module importable without PyQt5 in non-UI contexts
    from PyQt5.QtCore import Qt
    from PyQt5.QtWidgets import (
        QDialog, QVBoxLayout, QHBoxLayout, QFormLayout, QLabel, QLineEdit, QComboBox,
        QSpinBox, QCheckBox, QTextEdit, QPushButton, QWidget, QMessageBox
    )
    return Qt, QDialog, QVBoxLayout, QHBoxLayout, QFormLayout, QLabel, QLineEdit, QComboBox, QSpinBox, QCheckBox, QTextEdit, QPushButton, QWidget, QMessageBox

class ProcGenDialog:
    """
    Thin wrapper to avoid subclassing QDialog in module scope (to keep imports lazy).
    """
    def __init__(self, editor):
        Qt, QDialog, QVBoxLayout, QHBoxLayout, QFormLayout, QLabel, QLineEdit, QComboBox, QSpinBox, QCheckBox, QTextEdit, QPushButton, QWidget, QMessageBox = _get_qt()
        self.QMessageBox = QMessageBox
        self.editor = editor
        self.dlg = QDialog(editor)
        self.dlg.setWindowTitle("Procedural Generation")
        root = QVBoxLayout(self.dlg)

        # --- Mode
        form = QFormLayout()
        root.addLayout(form)

        self.mode = QComboBox(); self.mode.addItems(["Full Node", "Cluster in Existing Node"])
        form.addRow("Mode:", self.mode)

        # --- Hub / Node
        # hubs
        self.hub = QComboBox()
        hubs = sorted(editor.hubs_by_id.values(), key=lambda h: h.get("title",""))
        for h in hubs:
            self.hub.addItem(f'{h.get("title","")}  [{h.get("id")}]', h.get("id"))
        form.addRow("Hub (for new Node):", self.hub)

        # nodes
        self.node = QComboBox()
        nodes = sorted(editor.nodes_by_id.values(), key=lambda n: n.get("title",""))
        for n in nodes:
            self.node.addItem(f'{n.get("title","")}  [{n.get("id")}]', n.get("id"))
        form.addRow("Target Node (for Cluster):", self.node)

        # attachment room
        self.attach = QComboBox(); self.attach.setEditable(True)
        # will be filled on node change
        self._reload_rooms_for_selected_node()
        self.node.currentIndexChanged.connect(self._reload_rooms_for_selected_node)
        form.addRow("Attach to Room (optional):", self.attach)

        # --- New node id/title
        self.new_node_id = QLineEdit(); self.new_node_title = QLineEdit()
        form.addRow("New Node ID:", self.new_node_id)
        form.addRow("New Node Title:", self.new_node_title)

        # --- Theme & AI
        self.theme = QLineEdit(); self.theme.setPlaceholderText("e.g. Haunted Mine, Research Outpost…")
        self.use_ai = QCheckBox("Use OpenAI for titles/descriptions (requires OPENAI_API_KEY)")
        form.addRow("Theme / Context:", self.theme); form.addRow("", self.use_ai)

        # --- Pattern & room count
        self.pattern = QComboBox(); self.pattern.addItems(["linear","branch","hub","loop","maze"])
        self.count = QSpinBox(); self.count.setRange(1, 999); self.count.setValue(12)
        form.addRow("Layout Pattern:", self.pattern)
        form.addRow("Room Count:", self.count)

        # --- Cluster bbox
        bbox_row = QHBoxLayout()
        self.bx = QSpinBox(); self.by = QSpinBox(); self.bw = QSpinBox(); self.bh = QSpinBox()
        for sb, val, lab in [(self.bx, 0, "X"), (self.by, 0, "Y"), (self.bw, 20, "W"), (self.bh, 10, "H")]:
            sb.setRange(-9999, 9999); sb.setValue(val)
            w = QWidget(); w_l = QHBoxLayout(w); w_l.setContentsMargins(0,0,0,0)
            w_l.addWidget(QLabel(lab)); w_l.addWidget(sb)
            bbox_row.addWidget(w)
        form.addRow("Cluster BBox (grid units):", bbox_row)

        # --- Buttons
        btn_row = QHBoxLayout()
        self.go_btn = QPushButton("Generate")
        self.cancel_btn = QPushButton("Cancel")
        btn_row.addStretch(1); btn_row.addWidget(self.go_btn); btn_row.addWidget(self.cancel_btn)
        root.addLayout(btn_row)

        self.go_btn.clicked.connect(self._on_generate)
        self.cancel_btn.clicked.connect(self.dlg.reject)

        # Smart defaults based on current selection
        self._smart_defaults_from_selection()

    def _smart_defaults_from_selection(self):
        sel = self.editor.current_selection if hasattr(self.editor, "current_selection") else (None, None)
        typ, _id = sel
        if typ == "hub":
            idx = max(0, self.hub.findData(_id))
            self.hub.setCurrentIndex(idx)
        elif typ == "node":
            # match hub + node
            node = self.editor.nodes_by_id.get(_id, {})
            hub_id = node.get("hub_id")
            if hub_id:
                hidx = max(0, self.hub.findData(hub_id)); self.hub.setCurrentIndex(hidx)
            nidx = max(0, self.node.findData(_id)); self.node.setCurrentIndex(nidx)
        elif typ == "room":
            # set node + attachment
            for nid, nd in self.editor.nodes_by_id.items():
                key = "rooms"
                if _id in nd.get(key, []):
                    nidx = max(0, self.node.findData(nid)); self.node.setCurrentIndex(nidx)
                    break
            self._reload_rooms_for_selected_node()
            aidx = max(0, self.attach.findData(_id)); self.attach.setCurrentIndex(aidx)

    def _reload_rooms_for_selected_node(self):
        self.attach.clear()
        nid = self.node.currentData()
        if not nid:
            return
        node = self.editor.nodes_by_id.get(nid, {})
        for rid in node.get("rooms", []):
            self.attach.addItem(rid, rid)

    def _collect_params(self) -> ProcGenParams:
        mode_txt = self.mode.currentText()
        mode = "full_node" if "Full" in mode_txt else "cluster"
        return ProcGenParams(
            mode=mode,
            hub_id=self.hub.currentData(),
            node_id=self.node.currentData(),
            new_node_id=self.new_node_id.text().strip() or None,
            new_node_title=self.new_node_title.text().strip() or None,
            theme=self.theme.text().strip(),
            pattern=self.pattern.currentText(),
            room_count=int(self.count.value()),
            use_ai=bool(self.use_ai.isChecked()),
            bbox_x=int(self.bx.value()),
            bbox_y=int(self.by.value()),
            bbox_w=int(self.bw.value()),
            bbox_h=int(self.bh.value()),
            attach_room_id=self.attach.currentData() or None,
        )

    def _on_generate(self):
        try:
            p = self._collect_params()
            if p.mode == "full_node":
                if not p.hub_id:
                    self.QMessageBox.warning(self.dlg, "Full Node", "Choose a Hub for the new node.")
                    return
                out = apply_full_node(self.editor, p)
                self.QMessageBox.information(self.dlg, "Generated",
                    f"Created node '{out.new_node_id}' with {len(out.new_room_ids)} rooms.")
                self.dlg.accept()
            else:
                if not p.node_id:
                    self.QMessageBox.warning(self.dlg, "Cluster", "Choose a target Node for the cluster.")
                    return
                out = apply_cluster(self.editor, p)
                self.QMessageBox.information(self.dlg, "Generated",
                    f"Added {len(out.new_room_ids)} rooms to node '{p.node_id}'.")
                self.dlg.accept()
        except Exception as e:
            traceback.print_exc()
            self.QMessageBox.critical(self.dlg, "Error", f"Generation failed:\n{e}")

    def exec_(self) -> int:
        return self.dlg.exec_()
    
    # --- JSON-level helper for the AI Assistant (no editor instance required) ---------
    # Reuses this module's pattern generators + AITextClient. Produces a pure payload
    # that the assistant can diff/preview before writing rooms.json/hub_nodes.json.

    def _pg_read_list(path: Path) -> list:
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            return []

    def _pg_find_root(start: Path) -> Path:
        return resolve_paths(start).assets_dir

    def _pg_next_room_index(node_id: str, rooms: list) -> int:
        # existing room ids look like "<node>_<n>"
        import re
        pat = re.compile(rf"^{str(node_id)}_(\d+)$")
        mx = 0
        for r in rooms:
            rid = str((r or {}).get("id") or "")
            m = pat.match(rid)
            if m:
                try:
                    mx = max(mx, int(m.group(1)))
                except Exception:
                    pass
        return mx + 1

    def _pg_dir_from(ax: int, ay: int, bx: int, by: int) -> Optional[str]:
        if bx == ax + 1 and by == ay: return "east"
        if bx == ax - 1 and by == ay: return "west"
        if by == ay + 1 and bx == ax: return "south"
        if by == ay - 1 and bx == ax: return "north"
        return None

    def compute_cluster_payload(project_root: str | Path,
                                node_id: str,
                                *,
                                count: int = 6,
                                pattern: str = "loop",
                                theme: str = "",
                                use_ai: bool = False,
                                seed: Optional[int] = None,
                                model: Optional[str] = None) -> Dict[str, Any]:

        """
        Pure function (file-read only) that returns a JSON-ready cluster for node_id:

        {
        "rooms": [ {id,title,description,pos:[x,y],connections:{rid:dir,...}}, ... ],
        "node_append_rooms": ["<rid>", ...]
        }

        The caller (AI Assistant) will merge + diff these into rooms.json and hub_nodes.json.
        """
        root = _pg_find_root(Path(project_root) if project_root else Path(__file__).parent)
        rooms_path = root / "rooms.json"
        nodes_path = root / "hub_nodes.json"

        all_rooms = _pg_read_list(rooms_path)
        all_nodes = _pg_read_list(nodes_path)
        node_map = {str((n or {}).get("id")): n for n in all_nodes}
        node = node_map.get(str(node_id))
        if not node:
            raise ValueError(f"Node '{node_id}' not found in hub_nodes.json.")

        # Generate relative layout using this module's patterns
        if pattern not in PATTERNS:
            pattern = "loop"
        pos_rel, edges = _pattern_positions_edges(pattern, max(1, int(count)))

        # Normalize positions around (0,0); 1-grid spacing
        # We use relative grid directly as absolute; world_editor can reposition later.
        pos_abs = [(int(x), int(y)) for (x, y) in pos_rel]

        rng = random.Random(seed if seed is not None else (hash(str(node_id)) & 0xFFFFFFFF))
        ai = AITextClient(enabled=bool(use_ai), model=(model or "gpt-5-mini"))

        # Determine next index for this node
        base_idx = _pg_next_room_index(str(node_id), all_rooms)

        rooms_new: List[dict] = []
        to_append: List[str] = []

        for i, (gx, gy) in enumerate(pos_abs, start=0):
            idx = base_idx + i
            rid = f"{node_id}_{idx}"
            title, desc = ai.room_title_desc(theme or str(node.get('title') or node_id).title(), i+1)
            room = {
                "id": rid,
                "title": title,
                "description": desc,
                "pos": [gx, gy],
                "connections": {}
            }
            rooms_new.append(room)
            to_append.append(rid)

        # Wire connections using edges (indexes in rooms_new)
        for (a, b) in edges:
            if 0 <= a < len(rooms_new) and 0 <= b < len(rooms_new):
                ax, ay = rooms_new[a]["pos"]; bx, by = rooms_new[b]["pos"]
                da = _pg_dir_from(ax, ay, bx, by)
                db = _pg_dir_from(bx, by, ax, ay)
                if da: rooms_new[a]["connections"][rooms_new[b]["id"]] = da
                if db: rooms_new[b]["connections"][rooms_new[a]["id"]] = db

        return {"rooms": rooms_new, "node_append_rooms": to_append}

# ---- Public entry point ------------------------------------------------------------

def open_procgen_dialog(editor) -> None:
    """
    Public API for world_editor.py. Spawns the dialog and applies changes.
    """
    dlg = ProcGenDialog(editor)
    dlg.exec_()
