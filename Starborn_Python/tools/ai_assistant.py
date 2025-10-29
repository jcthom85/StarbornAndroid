#!/usr/bin/env python3
# tools/ai_assistant.py
from __future__ import annotations
import json, difflib, re, math, random, inspect, os
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
from datetime import datetime

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (
    QApplication, QWidget, QTabWidget, QVBoxLayout, QHBoxLayout, QLabel, QLineEdit,
    QTextEdit, QPushButton, QComboBox, QSpinBox, QCheckBox, QMessageBox, QDialog,
    QListWidget, QListWidgetItem, QDialogButtonBox, QStackedWidget, QTableWidget,
    QTableWidgetItem, QAbstractItemView, QGroupBox, QFormLayout, QPlainTextEdit
)

# Make sure local 'tools' dir is importable even if launched from elsewhere
import sys
sys.path.insert(0, str(Path(__file__).parent))

# --------- Optional, but if missing we provide a fallback below ----------
try:
    from context_index import ContextIndex as _RealContextIndex  # your project helper (if exists)
except Exception:
    _RealContextIndex = None

# --------- Your existing service (we use only when "Use OpenAI" is checked) ----------
try:
    from ai_service import AIService
except Exception:
    class AIService:
        def __init__(self, use_openai=False, model="gpt-5-mini"): self.use_openai = False
        def ask_text(self, q: str, pack: dict) -> str: return "(AI disabled) " + q
        def complete_json(self, tag: str, schema_hint: dict, task: str, pack: dict):
            # Return a best-effort empty object so code downstream doesn't explode
            return ({}, "(AI disabled)")

# --------- Optional helper if you had one; we guard all usage ----------
try:
    import procedural_gen as PG
    _HAS_PG_HELPER = hasattr(PG, "compute_cluster_payload")
except Exception:
    PG = None
    _HAS_PG_HELPER = False

# ============================================================================== 
# Utilities
# ============================================================================== 
def _find_root(start: Path) -> Path:
    cur = start.resolve()
    for _ in range(8):
        if (cur / "rooms.json").exists() or (cur / "worlds.json").exists():
            return cur
        cur = cur.parent
    return start.resolve()

def _read_json_list(path: Path) -> list:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return []

def _write_json_with_backup(path: Path, payload: Any) -> Tuple[bool, str]:
    """
    Write JSON, making a .bak copy if the file already exists.
    Returns (ok, error_message). error_message is "" on success.
    """
    try:
        if path.exists():
            bak = path.with_suffix(path.suffix + ".bak")
            bak.write_text(path.read_text(encoding="utf-8"), encoding="utf-8")
        path.write_text(json.dumps(payload, ensure_ascii=False, indent=4), encoding="utf-8")
        return True, ""
    except Exception as e:
        return False, f"{type(e).__name__}: {e}"

def _normalize_json_payload(payload):
    """
    Ensure payload is a Python dict/list.
    Accepts dict/list (returns as-is), or str (attempts json.loads).
    Returns (normalized_obj, error_msg).
    """
    try:
        if isinstance(payload, (dict, list)):
            return payload, ""
        if isinstance(payload, str):
            return json.loads(payload), ""
        # Some generators hand back (text, obj). Prefer obj if present.
        if isinstance(payload, tuple) and len(payload) == 2:
            maybe_obj = payload[1]
            if isinstance(maybe_obj, (dict, list)):
                return maybe_obj, ""
            if isinstance(maybe_obj, str):
                return json.loads(maybe_obj), ""
        # Last resort: stringify then parse if it "looks" like JSON
        s = json.dumps(payload, ensure_ascii=False)
        return json.loads(s), ""
    except Exception as e:
        return None, f"Normalize error: {type(e).__name__}: {e}"

def _unified_diff(old_text: str, new_text: str, name: str) -> str:
    return "".join(difflib.unified_diff(
        old_text.splitlines(True), new_text.splitlines(True),
        fromfile=f"{name} (old)", tofile=f"{name} (new)"
    ))

def _slug(s: str) -> str:
    s = re.sub(r"[^a-zA-Z0-9]+", "_", s.strip().lower()).strip("_")
    return s or "item"

# ============================================================================== 
# Simple ContextIndex fallback (only used if your real one isn't available)
# ============================================================================== 
class _SimpleContextIndex:
    """
    Fallback project index + briefing loader for the AI context pack.
    Safe defaults for all keys used by the UI/AI so it never AttributeErrors.
    """
    def __init__(self, root: Path):
        self.root = root

        def _read_any(rel: str, default):
            p = root / rel
            if not p.exists():
                return default
            try:
                import json
                return json.loads(p.read_text(encoding="utf-8"))
            except Exception:
                return default

        def _read_text(rel: str) -> str:
            p = root / rel
            if not p.exists():
                return ""
            try:
                return p.read_text(encoding="utf-8")
            except Exception:
                return ""

        # Topology
        self._worlds = _read_any("worlds.json", [])
        self._hubs   = _read_any("hubs.json", [])
        self._nodes  = _read_any("nodes.json", [])
        self._rooms  = _read_any("rooms.json", [])

        # Lists / dicts used around the app (always provide something)
        self._room_templates = _read_any("room_templates.json", [])
        self._items      = _read_any("items.json", [])
        self._quests     = _read_any("quests.json", [])
        self._dialogue   = _read_any("dialogue.json", [])
        self._npcs       = _read_any("npcs.json", [])
        self._encounters = _read_any("encounters.json", [])
        self._skills     = _read_any("skills.json", [])
        self._enemies    = _read_any("enemies.json", [])
        self._shops      = _read_any("shops.json", [])
        self._events     = _read_any("events.json", [])
        self._cinematics = _read_any("cinematics.json", {})

        # Data folder
        self._components     = _read_any("data/components.json", {})
        self._progression    = _read_any("data/progression.json", {})
        self._leveling_data  = _read_any("data/leveling_data.json", {})
        self._milestones     = _read_any("data/milestones.json", {})

        # Systems
        self._buffs   = _read_any("buffs.json", {})
        self._themes  = _read_any("themes.json", {})
        self._sfx     = _read_any("sfx.json", {})
        self._balance_targets = _read_any("tools/balance_targets.json", {})

        # Crafting
        self._recipes_cooking   = _read_any("data/recipes_cooking.json", {})
        self._recipes_tinkering = _read_any("data/recipes_tinkering.json", {})
        self._recipes_fishing   = _read_any("data/recipes_fishing.json", {
            "meta": {}, "rods": [], "lures": [], "zones": [],
            "minigame_rules": {}, "victory_screen": {}
        })
        self._fishing_zones = self._recipes_fishing.get("zones", [])

        # Characters / trees
        self._characters = _read_any("characters.json", [])
        self._skill_trees = {}
        st_dir = root / "skill_trees"
        if st_dir.exists() and st_dir.is_dir():
            for fp in st_dir.glob("*.json"):
                try:
                    self._skill_trees[fp.stem] = json.loads(fp.read_text(encoding="utf-8"))
                except Exception:
                    self._skill_trees[fp.stem] = {}

        # Global briefing (live-reloaded)
        self._assistant_briefing_path = root / "data" / "assistant_briefing.md"
        self._assistant_briefing = _read_text("data/assistant_briefing.md")

    def nodes(self) -> List[dict]:
        return list(self._nodes)

    def pack_global(self) -> dict:
        # Always re-read the briefing so it's fresh
        try:
            self._assistant_briefing = self._assistant_briefing_path.read_text(encoding="utf-8")
        except Exception:
            pass

        return {
            "worlds": self._worlds, "hubs": self._hubs, "nodes": self._nodes, "rooms": self._rooms,
            "room_templates": self._room_templates,

            "items": self._items, "quests": self._quests, "dialogue": self._dialogue, "npcs": self._npcs,
            "characters": self._characters, "enemies": self._enemies, "encounters": self._encounters,

            "skills": self._skills, "skill_trees": self._skill_trees, "buffs": self._buffs,
            "leveling_data": self._leveling_data, "progression": self._progression, "milestones": self._milestones,

            "shops": self._shops, "events": self._events, "cinematics": self._cinematics,
            "recipes_cooking": self._recipes_cooking, "recipes_tinkering": self._recipes_tinkering,
            "recipes_fishing": self._recipes_fishing, "fishing_zones": self._fishing_zones,

            "themes": self._themes, "sfx": self._sfx, "balance_targets": self._balance_targets,
            "components": self._components,

            "assistant_briefing": self._assistant_briefing,
        }

    def pack_for_node(self, node_id: str) -> dict:
        node_rooms = []
        for r in self._rooms:
            rid = str(r.get("id") or "")
            if rid.startswith(f"{node_id}_"):
                node_rooms.append(r)
        return {"node": {"id": node_id}, "rooms": node_rooms, **self.pack_global()}
    
# ============================================================================== 
# Diff / Apply dialog
# ============================================================================== 
class DiffApplyDialog(QDialog):
    def __init__(self, root: Path, proposals: Dict[str, Any], parent=None):
        super().__init__(parent)
        self.setWindowTitle("Apply Changes")
        self.root = Path(root)
        self.proposals = proposals  # { "file.json": python_obj OR json_str }

        layout = QVBoxLayout(self)

        # Explain what's happening
        layout.addWidget(QLabel("Select which files to write:"))

        # File checklist
        self.file_list = QListWidget()
        self.file_list.setSelectionMode(QAbstractItemView.NoSelection)
        for name in sorted(self.proposals.keys()):
            item = QListWidgetItem(name)
            item.setFlags(item.flags() | Qt.ItemIsUserCheckable)
            item.setCheckState(Qt.Checked)  # default: write all
            self.file_list.addItem(item)
        layout.addWidget(self.file_list, 1)

        # Buttons
        row = QHBoxLayout()
        self.apply_btn = QPushButton("Apply Selected")
        self.cancel_btn = QPushButton("Cancel")
        row.addStretch(1)
        row.addWidget(self.apply_btn)
        row.addWidget(self.cancel_btn)
        layout.addLayout(row)

        self.apply_btn.clicked.connect(self._on_apply)
        self.cancel_btn.clicked.connect(self.reject)

        # Optional: preview area hint (you already have a main preview in the parent UI)

    def _selected_files(self) -> List[str]:
        names: List[str] = []
        for i in range(self.file_list.count()):
            it = self.file_list.item(i)
            if it.checkState() == Qt.Checked:
                names.append(it.text())
        return names

    def _on_apply(self):
        selected = self._selected_files()
        if not selected:
            QMessageBox.information(self, "No Selection", "No files selected.")
            return

        written: List[str] = []
        fails: List[Tuple[str, str]] = []

        for name in selected:
            payload = self.proposals.get(name)
            norm, norm_err = _normalize_json_payload(payload)
            if norm_err:
                fails.append((name, norm_err))
                continue

            ok, err = _write_json_with_backup(self.root / name, norm)
            if ok:
                written.append(name)
            else:
                fails.append((name, err or "Unknown error"))

        if fails:
            details = "\n".join([f"• {n} — {err}" for n, err in fails])
            QMessageBox.critical(self, "Apply failed",
                                 f"Some files could not be written:\n\n{details}")
            # Keep dialog open so you can adjust selection and retry
            return

        if not written:
            QMessageBox.information(self, "No changes", "Nothing was written.")
            return

        msg = "Updated the following files (backups *.bak created if originals existed):\n\n" + \
              "\n".join([f"• {n}" for n in written])
        QMessageBox.information(self, "Applied", msg)
        print("[Assistant] Wrote files:", ", ".join(written))
        self.accept()

# ============================================================================== 
# Build Pages (each returns proposals dict[str->json-serializable])
# ============================================================================== 
class _BuildPageBase(QWidget):
    def __init__(self, root: Path, cx, get_model, get_use_ai, parent=None):
        super().__init__(parent)
        self.root = root
        self.cx = cx
        self._get_model = get_model
        self._get_use_ai = get_use_ai

    # must be implemented
    def get_proposals(self) -> Dict[str, Any]:
        raise NotImplementedError

# ----- Room Cluster page ----- 
class RoomClusterPage(_BuildPageBase):
    def __init__(self, root: Path, cx, get_model, get_use_ai, status_lbl, parent=None):
        super().__init__(root, cx, get_model, get_use_ai, parent)
        self.status_lbl = status_lbl # Add this line
        v = QVBoxLayout(self)

        # row 1
        r1 = QHBoxLayout()
        r1.addWidget(QLabel("Node:"))
        self.node_pick = QComboBox()
        for n in self.cx.nodes():
            nid = str(n.get("id"))
            title = n.get("title") or n.get("name") or ""
            self.node_pick.addItem(f"{nid} — {title}", nid)
        r1.addWidget(self.node_pick, 1)

        r1.addWidget(QLabel("Pattern:"))
        self.pattern = QComboBox()
        self.pattern.addItem("Linear", "linear")
        self.pattern.addItem("Branching", "branch")
        self.pattern.addItem("Loop", "loop")
        self.pattern.addItem("Wheel (hub+ring)", "wheel")
        self.pattern.addItem("Maze", "maze")
        r1.addWidget(self.pattern)

        r1.addWidget(QLabel("Count:"))
        self.count = QSpinBox(); self.count.setRange(1, 200); self.count.setValue(6)
        r1.addWidget(self.count)

        r1.addWidget(QLabel("Seed:"))
        self.seed = QSpinBox(); self.seed.setRange(0, 2_147_483_647); self.seed.setValue(0)
        r1.addWidget(self.seed)

        v.addLayout(r1)

        # row 2
        r2 = QHBoxLayout()
        r2.addWidget(QLabel("Theme:"))
        self.theme = QLineEdit(); self.theme.setPlaceholderText("e.g. abandoned mining maintenance ring")
        r2.addWidget(self.theme, 1)
        self.text_pass = QCheckBox("AI text pass (titles/descriptions)")
        r2.addWidget(self.text_pass)
        v.addLayout(r2)

        # --- row 3: layout rules + AI permissions ---
        r3 = QHBoxLayout()
        self.grid_step = QSpinBox(); self.grid_step.setRange(8, 512); self.grid_step.setValue(56)  # match editor default-ish
        r3.addWidget(QLabel("Grid step:")); r3.addWidget(self.grid_step)

        self.enforce_nsew = QCheckBox("Enforce NSEW adjacency"); self.enforce_nsew.setChecked(True)
        r3.addWidget(self.enforce_nsew)

        self.enable_for_node = QCheckBox("Enable AI room autogen for this node")
        self.enable_for_node.setChecked(False)  # OFF by default
        r3.addWidget(self.enable_for_node, 1)

        v.addLayout(r3)

        # Per-type notes
        notes_box = QGroupBox("Notes for AI (room cluster)")
        nv = QVBoxLayout(notes_box)
        self.text_notes = QPlainTextEdit()
        self.text_notes.setPlaceholderText("Encounter pacing, loot, secrets, traversal rules…")
        nv.addWidget(self.text_notes)
        v.addWidget(notes_box)

        # Load notes
        self._rc_notes_path = self.root / "data" / "notes" / "room_cluster.md"
        if self._rc_notes_path.exists():
            try:
                self.text_notes.setPlainText(self._rc_notes_path.read_text(encoding="utf-8"))
            except Exception:
                pass

        # help text
        help_lb = QLabel("Patterns: Linear / Branching / Loop / Wheel (hub+ring) / Maze.\n"
                         "Rooms write to rooms.json and are appended to the node in nodes.json. IDs auto-increment as <node>_<N>.")
        help_lb.setStyleSheet("color:#888;")
        v.addWidget(help_lb)

    def get_proposals(self) -> Dict[str, Any]:
        nid = self.node_pick.currentData() or ""
        count = int(self.count.value())
        seed = int(self.seed.value())
        pattern = self.pattern.currentData() or "linear"
        theme = self.theme.text().strip()
        use_ai = bool(self._get_use_ai())
        model = self._get_model()
        notes = self.text_notes.toPlainText().strip()
        grid_step = int(self.grid_step.value())
        nsew_only = bool(self.enforce_nsew.isChecked())

        # persist notes
        try:
            self._rc_notes_path.parent.mkdir(parents=True, exist_ok=True)
            self._rc_notes_path.write_text(notes, encoding="utf-8")
        except Exception:
            pass

        # ---- Guard: node must explicitly allow AI autogen (off by default) ----
        nodes_path = self.root / "nodes.json"
        cur_nodes = _read_json_list(nodes_path)
        node_idx = next((i for i,n in enumerate(cur_nodes) if str(n.get("id")) == str(nid)), None)
        if node_idx is None:
            QMessageBox.warning(self, "Room Autogen", f"Node not found: {nid}")
            return {}

        node_rec = cur_nodes[node_idx]
        allowed = bool(node_rec.get("ai_room_autogen", False))
        if use_ai and not allowed and not self.enable_for_node.isChecked():
            QMessageBox.information(
                self, "AI Locked for Node",
                "This node has AI room autogen disabled.\n\n"
                "Check 'Enable AI room autogen for this node' to proceed, or enable it later in nodes.json."
            )
            return {}
        
        if self.enable_for_node.isChecked() and not allowed:
            node_rec["ai_room_autogen"] = True
            cur_nodes[node_idx] = node_rec
            # This updated nodes.json will be included in our proposals below.

        # --- Generate the core room layout (the "cluster") ---
        cluster: Dict[str, Any] = {}
        if use_ai:
            # NEW: Use AI for the entire layout process
            self.status_lbl.setText("Asking AI for creative room layout...")
            QApplication.processEvents()
            try:
                cluster = self._ai_generate_room_layout(str(nid), count, theme, notes, model)
            finally:
                self.status_lbl.setText("Ready.")
        
        else:
            # Fallback to existing geometric or procedural helper logic
            if _HAS_PG_HELPER:
                # ... (existing PG helper code remains unchanged)
                # ...
                # NOTE: This block is intentionally left as a placeholder for your existing _HAS_PG_HELPER logic
                # For this example, we'll assume it's not hit and we proceed to the else block.
                # If you have code here, it should remain.
                # The provided Gist doesn't have the PG helper, so we build the fallback path.
                
                # This is a reconstruction of the logic from your file for completeness.
                func = getattr(PG, "compute_cluster_payload")
                _all_kwargs = {
                    "project_root": self.root, "root": self.root, "node_id": str(nid), "count": count,
                    "pattern": pattern, "theme": theme, "use_ai": use_ai, "seed": seed, "model": model,
                    "notes": notes, "grid_step": grid_step, "nsew_only": nsew_only,
                }
                try:
                    sig = inspect.signature(func)
                    accepted = set(sig.parameters.keys())
                    kwargs = {k: v for k, v in _all_kwargs.items() if k in accepted}
                    params = sig.parameters
                    if "project_root" in params and "project_root" not in kwargs: kwargs["project_root"] = self.root
                    if "root" in params and "root" not in kwargs and "project_root" not in params: kwargs["root"] = self.root
                except Exception:
                    kwargs = { "node_id": str(nid), "count": count, "pattern": pattern, "theme": theme, "seed": seed }
                cluster = func(**kwargs)

            else:
                cluster = self._fallback_cluster_payload(
                    str(nid), count, theme, seed, pattern,
                    grid_step=grid_step, nsew_only=nsew_only
                )

        # --- Post-process the generated cluster to finalize IDs, positions, and connections ---
        final_rooms = []
        ai_id_map = {}
        
        # Find the next available room number suffix for this node
        cur_room_list = _read_json_list(self.root / "rooms.json")
        pat = re.compile(rf"^{re.escape(str(nid))}_(\d+)$")
        mx = 0
        for r in cur_room_list:
            m = pat.match(str(r.get("id") or ""))
            if m:
                try: mx = max(mx, int(m.group(1)))
                except: pass
        
        # Process rooms generated by either AI or the fallback generator
        for i, room_data in enumerate(cluster.get("rooms", [])):
            if not isinstance(room_data, dict): continue
            
            final_room = dict(room_data)

            # A) Finalize ID
            if use_ai:
                placeholder_id = final_room.get("id", f"room_{i}")
                new_id = f"{nid}_{mx + i + 1}"
                ai_id_map[placeholder_id] = new_id
                final_room["id"] = new_id
            else:
                # Fallback generator already created final IDs, which might not be processed if cluster came from PG helper.
                # For safety, let's ensure the ID is present from the fallback payload.
                new_id = final_room.get("id")
                if not new_id: # Handle case where PG helper might not return IDs.
                    new_id = f"{nid}_{mx + i + 1}"
                    final_room["id"] = new_id

            # B) Finalize Position (if not already in world coordinates)
            pos = final_room.get("pos", [0, 0])
            if use_ai: # AI gives grid coords, fallback gives world coords
                gx, gy = (pos[0] if pos and len(pos)>0 else 0), (pos[1] if pos and len(pos)>1 else 0)
                final_room["pos"] = [float(gx * grid_step), float(gy * grid_step)]
            
            # C) Ensure standard fields exist
            final_room.setdefault("title", new_id)
            final_room.setdefault("description", "Auto-generated room.")
            final_room.setdefault("items", []); final_room.setdefault("npcs", []); final_room.setdefault("enemies", [])
            final_room.setdefault("actions", []); final_room.setdefault("state", {}); final_room.setdefault("connections", {})
            
            final_rooms.append(final_room)

        # D) Remap connections if generated by AI
        if use_ai:
            for room in final_rooms:
                new_conns = {}
                for direction, placeholder_id in room.get("connections", {}).items():
                    if placeholder_id in ai_id_map:
                        new_conns[direction] = ai_id_map[placeholder_id]
                room["connections"] = new_conns
        
        newly_created_ids = [r["id"] for r in final_rooms]

        # --- Merge into project files ---
        rooms_path = self.root / "rooms.json"
        nodes_path = self.root / "nodes.json"

        existing_ids = {str(r.get("id")) for r in cur_room_list if isinstance(r, dict)}
        merged_rooms = cur_room_list + [r for r in final_rooms if str(r.get("id")) not in existing_ids]

        # --- Append newly-created room ids to the selected node's rooms list ---
        for i, n in enumerate(cur_nodes):
            if str(n.get("id")) == str(nid):
                room_list = list(n.get("rooms") or [])
                for rid in newly_created_ids:
                    if rid not in room_list:
                        room_list.append(rid)
                n["rooms"] = room_list
                cur_nodes[i] = n
                break
        
        return {"rooms.json": merged_rooms, "nodes.json": cur_nodes}

    # ------- AI-powered layout generation -------
    def _ai_generate_room_layout(self, node_id: str, count: int, theme: str, notes: str, model: str) -> Dict[str, Any]:
        """
        Asks the AI to generate a creative room layout based on context, theme, and notes.
        Returns a raw cluster payload with grid coordinates and placeholder IDs.
        """
        svc = AIService(use_openai=True, model=model)
        pack = self.cx.pack_for_node(str(node_id))

        schema_hint = {
            "rooms": [{
                "id": "string (short, descriptive, snake_case placeholder, e.g. 'main_hub' or 'reactor_core')",
                "title": "string (creative, in-universe title)",
                "description": "string (brief, atmospheric description)",
                "pos": "[integer, integer] (grid coordinates [x, y])",
                "connections": {
                    "north": "string (id of room to the north)",
                    "south": "string (id of room to the south)",
                    "east": "string (id of room to the east)",
                    "west": "string (id of room to the west)"
                }
            }]
        }

        task = (
            f"Generate a creative and spatially logical map layout of approximately {count} interconnected rooms for a game node. "
            f"The node's theme is: '{theme}'. The layout should feel like a real, playable game level.\n\n"
            "Key Instructions:\n"
            "1.  **Coordinate System:** Use integer grid coordinates for `pos`. +X is East, -X is West, +Y is South, -Y is North.\n"
            "2.  **Connections:** Rooms should only connect to adjacent neighbors (no diagonals). If room 'A' connects 'north' to 'B', then room 'B' MUST have a 'south' connection back to 'A'. All connections must be reciprocal.\n"
            "3.  **Layout:** The layout should be creative and reflect the theme. Avoid simple lines or squares. Create a sprawling, interesting, and believable space. "
            "Incorporate gameplay concepts: create a central hub, some branching paths, a few dead-ends for secrets, and natural choke-points. "
            "Use the designer notes for guidance on pacing and key locations.\n"
            "4.  **Content:** Generate a creative `title` and `description` for each room that fits the theme. The descriptions should be evocative and give clues about the room's purpose or history.\n"
            "5.  **IDs:** Use short, descriptive, lowercase placeholder strings for the `id` field. These will be replaced later.\n\n"
            f"Designer Notes:\n{notes}"
        )

        obj, _ = svc.complete_json("room_cluster_layout", schema_hint, task, pack)

        if isinstance(obj, dict) and isinstance(obj.get("rooms"), list):
            # Return the raw AI output for post-processing
            return {"rooms": obj["rooms"], "node_append_rooms": [], "_text_embellished": True}
        
        # Fallback if AI fails
        return {"rooms": [], "node_append_rooms": [], "_text_embellished": False}
    
    # ------- local generator using valid connections schema -------
    def _fallback_cluster_payload(
        self,
        node_id: str,
        count: int,
        theme: str,
        seed: int,
        pattern: str,
        *,
        grid_step: int = 56,
        nsew_only: bool = True
    ) -> Dict[str, Any]:
        rng = random.Random(seed or 0)

        cur_rooms = _read_json_list(self.root / "rooms.json")
        pat = re.compile(rf"^{re.escape(str(node_id))}_(\d+)$")
        mx = 0
        for r in cur_rooms:
            m = pat.match(str(r.get("id") or ""))
            if m:
                try: mx = max(mx, int(m.group(1)))
                except: pass

        def rid(i: int) -> str: return f"{node_id}_{mx + i}"

        def make_room(i: int, gx: int, gy: int) -> dict:
            # place exactly on grid
            return {
                "id": rid(i),
                "title": f"{theme or 'Sector'} {i}",
                "description": f"Auto-generated ({pattern}) room {i}.",
                "pos": [float(gx * grid_step), float(gy * grid_step)],
                "items": [], "npcs": [], "enemies": [],
                "actions": [], "state": {}, "connections": {}
            }

        def link(a: dict, b: dict):
            ax, ay = int(round(a["pos"][0] / grid_step)), int(round(a["pos"][1] / grid_step))
            bx, by = int(round(b["pos"][0] / grid_step)), int(round(b["pos"][1] / grid_step))
            dx, dy = (bx - ax), (by - ay)

            # Only allow cardinal neighbors one step away
            if abs(dx) + abs(dy) != 1: 
                return  # ignore diagonals or spaced-out placements

            # Y is up in world coords: +dy is NORTH, -dy is SOUTH
            if dx == 1:        d1, d2 = "east",  "west"
            elif dx == -1:     d1, d2 = "west",  "east"
            elif dy == 1:      d1, d2 = "north", "south"   # +y -> north
            else:              d1, d2 = "south", "north"   # -y -> south

            a["connections"][d1] = b["id"]
            b["connections"][d2] = a["id"]

        gen: List[dict] = []

        # --- Build grid coordinates first (integer gx, gy) ---
        coords: List[Tuple[int,int]] = []

        if pattern == "linear" or count <= 2:
            # Horizontal line centered at (0,0)
            start = -(count // 2)
            coords = [(start + i, 0) for i in range(count)]

        elif pattern == "loop":
            # Axis-aligned rectangle loop (no diagonals)
            # Compute perimeter nodes ~count; make a roughly square loop
            side = max(2, int(round((count / 4.0))))
            w, h = side, max(2, side)
            # generate rectangle perimeter coordinates
            ring = []
            for x in range(-w//2, w//2 + 1):          ring.append((x, -h//2))
            for y in range(-h//2 + 1, h//2 + 1):      ring.append((w//2, y))
            for x in range(w//2 - 1, -w//2 - 1, -1):  ring.append((x, h//2))
            for y in range(h//2 - 1, -h//2, -1):      ring.append((-w//2, y))
            # sample count distinct positions around loop
            step = max(1, len(ring) // count)
            coords = [ring[(i * step) % len(ring)] for i in range(count)]

        elif pattern == "wheel":
            # hub + spokes in cardinal-ish spread; rim is a rectangle loop
            hub = (0, 0)
            spokes = max(1, count - 1)
            # Make a small rectangular rim sized to spokes
            rim_len = max(4, spokes)
            half = rim_len // 4 + 1
            rim = []
            for x in range(-half, half+1):       rim.append((x, -half))
            for y in range(-half+1, half+1):     rim.append((half, y))
            for x in range(half-1, -half-1, -1): rim.append((x, half))
            for y in range(half-1, -half, -1):   rim.append((-half, y))
            step = max(1, len(rim)//spokes)
            coords = [hub] + [rim[(i*step)%len(rim)] for i in range(spokes)]

        else:
            # "branch" / "maze": simple DFS tree on grid with no overlaps
            # carve grid cells from (0,0), expanding randomly
            visited = set([(0,0)])
            order = [(0,0)]
            dirs = [(1,0),(-1,0),(0,1),(0,-1)]
            while len(order) < count:
                cx, cy = order[rng.randrange(len(order))]
                rng.shuffle(dirs)
                placed = False
                for dx, dy in dirs:
                    nx, ny = cx+dx, cy+dy
                    if (nx,ny) not in visited:
                        visited.add((nx,ny))
                        order.append((nx,ny))
                        placed = True
                        break
                if not placed and len(order) >= count:
                    break
            coords = order[:count]

        # --- Materialize rooms at coords, then link NSEW neighbors (1-step only) ---
        for i, (gx, gy) in enumerate(coords, start=1):
            gen.append(make_room(i, gx, gy))

        # index by grid pos
        by_pos = { (int(round(r["pos"][0]/grid_step)), int(round(r["pos"][1]/grid_step))): r for r in gen }

        # For each room, link to existing immediate neighbors (E,W,N,S) if present
        for gx, gy in list(by_pos.keys()):
            a = by_pos[(gx,gy)]
            for dx, dy in [(1,0),(-1,0),(0,1),(0,-1)]:
                nb = by_pos.get((gx+dx, gy+dy))
                if nb:
                    link(a, nb)

        return {
            "rooms": gen,
            "node_append_rooms": [r["id"] for r in gen],
            "_text_embellished": False
        }


# ----- Items page ----- 
class ItemsPage(_BuildPageBase):
    """
    Dynamic manual editor + quick generators + AI optional.
    Writes to items.json
    """
    COLS = ["Name", "Type", "Rarity", "Description"]

    def __init__(self, root: Path, cx, get_model, get_use_ai, parent=None):
        super().__init__(root, cx, get_model, get_use_ai, parent)
        v = QVBoxLayout(self)

        # top controls
        top = QHBoxLayout()
        top.addWidget(QLabel("Theme:"))
        self.theme = QLineEdit(); self.theme.setPlaceholderText("e.g. salvage, mining, contraband")
        top.addWidget(self.theme, 1)

        self.auto_id = QCheckBox("Auto-ID from name")
        self.auto_id.setChecked(True)
        top.addWidget(self.auto_id)

        v.addLayout(top)

        # manual table
        g = QGroupBox("Manual Items")
        gv = QVBoxLayout(g)

        self.table = QTableWidget(0, len(self.COLS))
        self.table.setHorizontalHeaderLabels(self.COLS)
        self.table.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.table.setEditTriggers(QAbstractItemView.AllEditTriggers)
        self.table.horizontalHeader().setStretchLastSection(True)
        gv.addWidget(self.table, 1)

        tb = QHBoxLayout()
        self.add_row_btn = QPushButton("Add Row")
        self.del_row_btn = QPushButton("Delete Selected")
        self.quick_gen_btn = QPushButton("Quick Generate 5")
        tb.addWidget(self.add_row_btn); tb.addWidget(self.del_row_btn); tb.addStretch(1); tb.addWidget(self.quick_gen_btn)
        gv.addLayout(tb)
        v.addWidget(g, 1)

        # AI generator (optional)
        ai_box = QGroupBox("Optional: AI Generation")
        form = QFormLayout(ai_box)
        self.ai_count = QSpinBox(); self.ai_count.setRange(1, 100); self.ai_count.setValue(5)
        form.addRow("Count:", self.ai_count)
        self.ai_hint = QLabel("If you don't add manual rows, Preview will try to generate items.\n"
                              "If 'Use OpenAI' is OFF, it uses an offline generator with your Theme.")
        self.ai_hint.setStyleSheet("color:#888;")
        form.addRow(self.ai_hint)
        v.addWidget(ai_box)

        # wire buttons
        self.add_row_btn.clicked.connect(lambda: self._add_row({"Name": "", "Type": "consumable", "Rarity": "common", "Description": ""}))
        self.del_row_btn.clicked.connect(self._delete_selected)
        self.quick_gen_btn.clicked.connect(self._quick_generate)

    def _add_row(self, data: Dict[str, str]):
        r = self.table.rowCount()
        self.table.insertRow(r)
        for c, key in enumerate(self.COLS):
            item = QTableWidgetItem(str(data.get(key, "")))
            if key in ("Type", "Rarity"):
                # allow typing; we keep it simple
                pass
            self.table.setItem(r, c, item)

    def _delete_selected(self):
        rows = sorted({i.row() for i in self.table.selectedIndexes()}, reverse=True)
        for r in rows:
            self.table.removeRow(r)

    def _quick_generate(self):
        base = self.theme.text().strip() or "utility"
        pool_types = ["consumable", "equippable", "material", "key"]
        pool_rarity = ["common", "uncommon", "rare", "epic"]
        for i in range(5):
            t = random.choice(pool_types)
            rr = random.choice(pool_rarity)
            name = f"{base.title()} {['Kit','Tonic','Weave','Patch','Core','Shard','Pack'][i % 7]}"
            desc = f"{base} themed {t}."
            self._add_row({"Name": name, "Type": t, "Rarity": rr, "Description": desc})

    def _table_items(self) -> List[dict]:
        items = []
        for r in range(self.table.rowCount()):
            row = {k: (self.table.item(r, i).text().strip() if self.table.item(r, i) else "")
                   for i, k in enumerate(self.COLS)}
            if not row["Name"]:
                continue
            iid = _slug(row["Name"]) if self.auto_id.isChecked() else _slug(row["Name"])  # keep safe id
            # minimal schema; you can extend here to your full item schema
            obj = {
                "id": iid,
                "name": row["Name"],
                "type": row["Type"] or "consumable",
                "rarity": row["Rarity"] or "common",
                "description": row["Description"] or ""
            }
            # type-specific stubs to keep your game happy
            if obj["type"] == "consumable":
                obj.setdefault("effects", {"restore_hp": 0, "restore_rp": 0})
            elif obj["type"] == "equippable":
                obj.setdefault("slot", "accessory")
                obj.setdefault("stats", {})
            items.append(obj)
        return items

    def _offline_generate(self, n: int) -> List[dict]:
        base = self.theme.text().strip() or "utility"
        pool_types = ["consumable", "equippable", "material", "key"]
        pool_rarity = ["common", "uncommon", "rare", "epic"]
        out = []
        for i in range(n):
            t = pool_types[i % len(pool_types)]
            rr = pool_rarity[(i*2) % len(pool_rarity)]
            name = f"{base.title()} {random.choice(['Ampoule','Patch','Weave','Core','Spool','Injector','Charm','Band'])}"
            iid = _slug(name)
            desc = f"{base} themed {t}."
            obj = {"id": iid, "name": name, "type": t, "rarity": rr, "description": desc}
            if t == "consumable":
                obj["effects"] = {"restore_hp": 10 if 'Ampoule' in name else 0, "restore_rp": 5 if 'Injector' in name else 0}
            elif t == "equippable":
                obj["slot"] = random.choice(["head","body","legs","accessory","weapon"])
                obj["stats"] = {}
            out.append(obj)
        return out

    def _ai_generate(self, n: int) -> List[dict]:
        """
        Ask the AI for item JSON. Respects the UI's OpenAI toggle and current model.
        Prints a quick PACK sanity line so we know the story briefing is actually loaded.
        """
        # Respect UI toggle/model
        svc = AIService(use_openai=True, model=self._get_model())

        # Build PACK first, then debug-print its briefing length
        pack = self.cx.pack_global()
        briefing_text = pack.get("assistant_briefing", "") or ""
        print("[AI DEBUG] assistant_briefing length:", len(briefing_text))

        # (Optional extra visibility)
        try:
            print("[AI DEBUG] worlds/hubs/nodes/rooms:",
                len(pack.get("worlds", [])),
                len(pack.get("hubs", [])),
                len(pack.get("nodes", [])),
                len(pack.get("rooms", [])))
        except Exception:
            pass

        # Schema + task
        schema_hint = {
            "items": [{
                "id": "string",
                "name": "string",
                "type": "consumable|equippable|material|key",
                "rarity": "string",
                "description": "string"
            }]
        }

        theme = (self.theme.text().strip() if hasattr(self, "theme") else "") or "general"
        task = (
            f"Create {n} game items themed '{theme}'. "
            "Use ONLY facts in PACK when you reference existing items, rooms, skills, or tone. "
            "IDs must be short, slug-like (lowercase_underscores). "
            "type must be one of: consumable, equippable, material, key. "
            "Keep descriptions punchy and in-universe."
        )

        try:
            obj, _ = svc.complete_json("items_generate", schema_hint, task, pack)
            if isinstance(obj, dict) and isinstance(obj.get("items"), list):
                return list(obj["items"])
        except Exception as e:
            print("[AI ERROR] _ai_generate:", e)

        return []

    def get_proposals(self) -> Dict[str, Any]:
        # 1) Prefer manual rows if provided
        manual = self._table_items()
        items_out: List[dict] = []
        if manual:
            items_out = manual
        else:
            # 2) No manual rows → try AI (if enabled) or offline
            n = int(self.ai_count.value())
            use_ai = bool(self._get_use_ai())
            items_out = self._ai_generate(n) if use_ai else self._offline_generate(n)

        # Merge into items.json (append, dedup by id)
        path = self.root / "items.json"
        base = _read_json_list(path)

        existing = {str(it.get("id")) for it in base}
        for it in items_out:
            if str(it.get("id")) in existing:
                # simple collision avoidance: add numeric suffix
                k = 2
                base_id = str(it.get("id")) or _slug(it.get("name") or "item")
                new_id = base_id
                while new_id in existing:
                    new_id = f"{base_id}_{k}"; k += 1
                it["id"] = new_id
            existing.add(str(it.get("id")))
            base.append(it)

        return {"items.json": base}

# ----- Generic small pages (basic manual + optional AI) ----- 
class _SimpleListPage(_BuildPageBase):
    """
    Reusable for Dialogue, NPCs, Encounters, Quests (skeleton).
    Each subclass sets file_name and column spec; AI task differs slightly.
    """
    file_name: str = ""
    cols: List[str] = []
    ai_tag: str = ""
    ai_schema: dict = {}
    ai_prompt_label: str = ""
    ai_count_label: str = "Count:"

    def __init__(self, root: Path, cx, get_model, get_use_ai, parent=None):
        super().__init__(root, cx, get_model, get_use_ai, parent)
        v = QVBoxLayout(self)

        # theme/prompt line
        top = QHBoxLayout()
        top.addWidget(QLabel(self.ai_prompt_label or "Theme/Prompt:"))
        self.theme = QLineEdit()
        top.addWidget(self.theme, 1)
        v.addLayout(top)

        # table
        self.table = QTableWidget(0, len(self.cols))
        self.table.setHorizontalHeaderLabels(self.cols)
        self.table.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.table.setEditTriggers(QAbstractItemView.AllEditTriggers)
        self.table.horizontalHeader().setStretchLastSection(True)
        v.addWidget(self.table, 1)

        # buttons
        tb = QHBoxLayout()
        self.add_btn = QPushButton("Add Row")
        self.del_btn = QPushButton("Delete Selected")
        tb.addWidget(self.add_btn); tb.addWidget(self.del_btn); tb.addStretch(1)
        v.addLayout(tb)

        # AI section
        ai_box = QGroupBox("Optional: AI Generation")
        form = QFormLayout(ai_box)
        self.ai_count = QSpinBox(); self.ai_count.setRange(1, 100); self.ai_count.setValue(5)
        form.addRow(self.ai_count_label, self.ai_count)
        self.hint = QLabel("If no manual rows are provided, Preview will generate using AI if enabled, "
                           "otherwise an offline placeholder.")
        self.hint.setStyleSheet("color:#888;")
        form.addRow(self.hint)
        v.addWidget(ai_box)

        # Per-type notes box
        notes_box = QGroupBox("Notes for AI (this type)")
        nv = QVBoxLayout(notes_box)
        self.notes = QPlainTextEdit()
        self.notes.setPlaceholderText(
            "Constraints, tone, naming/ID rules, stat ranges, cross-refs, examples…")
        nv.addWidget(self.notes)
        v.addWidget(notes_box)

        # Load per-type notes from data/notes/<kind>.md
        kind = Path(self.file_name).stem or "generic"
        self._notes_path = self.root / "data" / "notes" / f"{kind}.md"
        if self._notes_path.exists():
            try:
                self.notes.setPlainText(self._notes_path.read_text(encoding='utf-8'))
            except Exception:
                pass

        # wire
        self.add_btn.clicked.connect(lambda: self._add_row({k: "" for k in self.cols}))
        self.del_btn.clicked.connect(self._delete_selected)

    def _add_row(self, data: Dict[str, str]):
        r = self.table.rowCount(); self.table.insertRow(r)
        for c, key in enumerate(self.cols):
            self.table.setItem(r, c, QTableWidgetItem(str(data.get(key, ""))))

    def _delete_selected(self):
        rows = sorted({i.row() for i in self.table.selectedIndexes()}, reverse=True)
        for r in rows: self.table.removeRow(r)

    def _manual_rows(self) -> List[dict]:
        out = []
        for r in range(self.table.rowCount()):
            row = {k: (self.table.item(r, i).text().strip() if self.table.item(r, i) else "") for i, k in enumerate(self.cols)}
            if any(v for v in row.values()):
                out.append(row)
        return out

    def _save_notes(self):
        try:
            p = getattr(self, "_notes_path", None)
            if not p: return
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(self.notes.toPlainText(), encoding="utf-8")
        except Exception:
            pass

    def _offline_generate(self, n: int) -> List[dict]:
        # Very light placeholders; you can flesh these as needed
        theme = self.theme.text().strip() or "general"
        rows = []
        for i in range(n):
            rows.append({k: f"{theme} {k.lower()} {i+1}" for k in self.cols})
        return rows

    def _ai_generate(self, n: int) -> List[dict]:
        svc = AIService(use_openai=True, model=self._get_model())
        pack = self.cx.pack_global()
        notes = self.notes.toPlainText().strip()
        theme = self.theme.text().strip() or 'general'
        prompt = f"Create {n} entries for {self.file_name} themed '{theme}'. " \
                f"Use the project briefing in pack['assistant_briefing']."
        if notes:
            prompt += "\n\nDesigner Notes:\n" + notes
        obj, _ = svc.complete_json(self.ai_tag, self.ai_schema, prompt, pack)

        # Expect a dict with a top-level list named like the file core, e.g. {"dialogue":[...]}
        key = Path(self.file_name).stem
        if isinstance(obj, dict):
            for k, v in obj.items():
                if isinstance(v, list):
                    return v
        return []

    def get_proposals(self) -> Dict[str, Any]:
        self._save_notes()
        manual = self._manual_rows()
        if manual:
            rows = manual
        else:
            n = int(self.ai_count.value())
            rows = self._ai_generate(n) if self._get_use_ai() else self._offline_generate(n)
        # merge
        path = self.root / self.file_name
        base = _read_json_list(path)
        # Normalize keys: if manual rows miss ids, create lightweight ids
        if self.file_name == "quests.json":
            for q in rows:
                q.setdefault("id", _slug(q.get("title","quest")))
                q.setdefault("stages", [{"id":"1","text":q.get("objective","Stage 1")}])
                q.setdefault("rewards", [])
        elif self.file_name == "dialogue.json":
            for d in rows:
                d.setdefault("id", _slug(d.get("text","line")))
                d.setdefault("speaker", d.get("speaker") or "npc")
                d.setdefault("next", "")
        elif self.file_name == "npcs.json":
            for n in rows:
                n.setdefault("id", _slug(n.get("name","npc")))
                n.setdefault("role", n.get("role") or "ambient")
        elif self.file_name == "encounters.json":
            for e in rows:
                e.setdefault("id", _slug(e.get("notes","encounter")))
                e.setdefault("enemy_group", e.get("enemy_group") or [])
                e.setdefault("difficulty", e.get("difficulty") or "normal")

        # Dedup by id or 'text' fallback
        seen = set()
        out = list(base)
        for row in rows:
            key = row.get("id") or row.get("text")
            if not key: continue
            skey = str(key)
            if skey in seen or any((isinstance(x, dict) and (x.get("id")==key or x.get("text")==key)) for x in out):
                # naive uniquify
                k = 2; base_id = _slug(skey)
                new_id = f"{base_id}_{k}"
                while any((isinstance(x, dict) and (x.get("id")==new_id)) for x in out):
                    k += 1; new_id = f"{base_id}_{k}"
                row["id"] = new_id
            seen.add(row.get("id") or row.get("text"))
            out.append(row)

        return {self.file_name: out}


# ----- Whole-file JSON page (for dict-shaped files like recipes_fishing.json) ----- 
class _JsonWholeFilePage(_BuildPageBase):
    """
    A page that asks AI for a complete JSON object for a single target file.
    Good for compound dict shapes (e.g., fishing system config).
    """
    file_name: str = ""       # e.g., "data/recipes_fishing.json"
    ai_tag: str = ""          # tag string for AIService
    ai_schema: dict = {}      # expected top-level schema dict
    ai_prompt_label: str = "Theme/Prompt:"

    def __init__(self, root: Path, cx, get_model, get_use_ai, parent=None):
        super().__init__(root, cx, get_model, get_use_ai, parent)
        v = QVBoxLayout(self)

        top = QHBoxLayout()
        top.addWidget(QLabel(self.ai_prompt_label))
        self.theme = QLineEdit()
        top.addWidget(self.theme, 1)
        v.addLayout(top)

        self.note = QLabel("This generator produces the entire file. "
                           "Use Preview to see a full diff before applying.")
        self.note.setStyleSheet("color:#888;")
        v.addWidget(self.note)

    def get_proposals(self) -> Dict[str, Any]:
        # If AI is disabled, just return the current file (no-op) so the diff shows current.
        if not self._get_use_ai():
            p = self.root / self.file_name
            try:
                current = json.loads(p.read_text(encoding="utf-8")) if p.exists() else self.ai_schema
            except Exception:
                current = self.ai_schema
            return {self.file_name: current}

        svc = AIService(use_openai=True, model=self._get_model())
        pack = self.cx.pack_global()
        prompt = f"Produce a complete, coherent {self.file_name} for the game. " \
                 f"Theme: '{self.theme.text().strip() or 'general'}'. " \
                 f"Preserve and reuse existing ids and patterns from the context where appropriate."
        obj, _ = svc.complete_json(self.ai_tag, self.ai_schema, prompt, pack)
        if not isinstance(obj, dict):
            obj = self.ai_schema
        return {self.file_name: obj}

    def pack_for_node(self, node_id: str) -> dict:
        node_rooms = []
        for r in self._rooms:
            rid = str(r.get("id") or "")
            if rid.startswith(f"{node_id}_"):
                node_rooms.append(r)
        return {"node_id": node_id, "rooms": node_rooms, **self.pack_global()}
    
class QuestsPage(_SimpleListPage):
    file_name = "quests.json"
    cols = ["title", "giver", "receiver", "objective"]
    ai_tag = "quests_generate"
    ai_schema = {"quests":[{"id":"string","title":"string","giver":"string","receiver":"string","stages":[{"id":"string","text":"string"}],"rewards":["string"]}]}
    ai_prompt_label = "Theme:"

class DialoguePage(_SimpleListPage):
    file_name = "dialogue.json"
    cols = ["speaker", "text", "next"]
    ai_tag = "dialogue_generate"
    ai_schema = {"dialogue":[{"id":"string","speaker":"string","text":"string","next":"string"}]}
    ai_prompt_label = "Theme:"

class NPCsPage(_SimpleListPage):
    file_name = "npcs.json"
    cols = ["name", "role", "description"]
    ai_tag = "npcs_generate"
    ai_schema = {"npcs":[{"id":"string","name":"string","role":"string","description":"string"}]}
    ai_prompt_label = "Theme:"

class EncountersPage(_SimpleListPage):
    file_name = "encounters.json"
    cols = ["enemy_group", "difficulty", "notes"]
    ai_tag = "encounters_generate"
    ai_schema = {"encounters":[{"id":"string","enemy_group":["string"],"difficulty":"string","notes":"string"}]}
    ai_prompt_label = "Theme:"

# ----- Skills ----- 
class SkillsPage(_SimpleListPage):
    file_name = "skills.json"
    cols = ["id", "name", "type", "target", "cost", "power", "element", "description"]
    ai_tag = "skills_generate"
    ai_schema = {"skills":[
        {"id":"string","name":"string","type":"active|passive","target":"self|ally|enemy|all",
         "resource":"stamina|energy|mana","cost":0,"power":0,
         "element":"neutral|fire|ice|shock|poison|radiation|psychic|void",
         "status":"","description":"string"}
    ]}
    ai_prompt_label = "Theme:"

# ----- Enemies ----- 
class EnemiesPage(_SimpleListPage):
    file_name = "enemies.json"
    cols = ["id", "name", "level", "element", "notes"]
    ai_tag = "enemies_generate"
    ai_schema = {"enemies":[
        {"id":"string","name":"string","level":1,"hp":20,"atk":5,"def":5,"speed":5,
         "element":"neutral|fire|ice|shock|poison|radiation|psychic|void",
         "resistances":{"fire":0,"ice":0,"shock":0,"poison":0,"radiation":0,"psychic":0,"void":0},
         "drops":[{"id":"string","chance":0.2}],"notes":"string"}
    ]}
    ai_prompt_label = "Theme:"

# ----- Shops ----- 
class ShopsPage(_SimpleListPage):
    file_name = "shops.json"
    cols = ["id", "name", "location", "notes"]
    ai_tag = "shops_generate"
    ai_schema = {"shops":[
        {"id":"string","name":"string","location":"string",
         "inventory":[{"id":"string","price":100}], "notes":"string"}
    ]}
    ai_prompt_label = "Theme:"

# ----- Events ----- 
class EventsPage(_SimpleListPage):
    file_name = "events.json"
    cols = ["id", "trigger", "conditions", "actions"]
    ai_tag = "events_generate"
    ai_schema = {"events":[
        {"id":"string","trigger":"on_enter|on_interact|on_battle_end|on_flag",
         "conditions":["string"], "actions":["string"], "notes":"string"}
    ]}
    ai_prompt_label = "Theme:"

# ----- Cutscenes (Cinematics) ----- 
class CutscenesPage(_SimpleListPage):
    file_name = "cinematics.json"
    cols = ["id", "title", "beats"]
    ai_tag = "cinematics_generate"
    ai_schema = {"cinematics":[
        {"id":"string","title":"string",
         "steps":[
            {"type":"text","speaker":"string","text":"string"},
            {"type":"move","actor":"string","to":"room_id"},
            {"type":"effect","name":"string","params":{"strength":1.0}}
         ]}
    ]}
    ai_prompt_label = "Theme:"

# ----- Cooking recipes ----- 
class CookingPage(_SimpleListPage):
    file_name = "data/recipes_cooking.json"
    cols = ["id", "name", "ingredients", "output"]
    ai_tag = "recipes_cooking_generate"
    ai_schema = {"recipes_cooking":[
        {"id":"string","name":"string",
         "ingredients":[{"id":"string","qty":1}],
         "output":{"id":"string","qty":1},
         "time":5,"notes":"string"}
    ]}
    ai_prompt_label = "Theme:"

# ----- Tinkering recipes ----- 
class TinkeringPage(_SimpleListPage):
    file_name = "data/recipes_tinkering.json"
    cols = ["id", "name", "ingredients", "output"]
    ai_tag = "recipes_tinkering_generate"
    ai_schema = {"recipes_tinkering":[
        {"id":"string","name":"string",
         "ingredients":[{"id":"string","qty":1}],
         "output":{"id":"string","qty":1},
         "time":5,"notes":"string"}
    ]}
    ai_prompt_label = "Theme:"

# ----- Fishing (whole-file dict) ----- 
class FishingPage(_JsonWholeFilePage):
    file_name = "data/recipes_fishing.json"
    ai_tag = "recipes_fishing_generate"
    ai_schema = {
        "meta": {"version": 1, "author": "assistant"},
        "rods": [{"id":"string","name":"string","power":1,"stability":1,"notes":"string"}],
        "lures": [{"id":"string","name":"string","type":"spinner|bait|plug","rarity":"common|rare|epic","notes":"string"}],
        "zones": [{
            "id":"string","name":"string","biome":"river|lake|ocean|sewer|void",
            "loot_table":[{"id":"item_id_or_fish","weight":1}]
        }],
        "minigame_rules": {"base_speed": 1.0, "tension_decay": 0.05, "crit_window": 0.1},
        "victory_screen": {"music":"fanfare_catch","show_spoils": True}
    }
    ai_prompt_label = "Theme:"

# ----- Themes Page ----- 
class ThemesPage(_BuildPageBase):
    COLS = ["Name", "Background", "Foreground", "Border", "Accent", "Background Image"]

    def __init__(self, root: Path, cx, get_model, get_use_ai, parent=None):
        super().__init__(root, cx, get_model, get_use_ai, parent)
        v = QVBoxLayout(self)

        # top controls
        top = QHBoxLayout()
        top.addWidget(QLabel("Theme Prompt:"))
        self.theme_prompt = QLineEdit()
        self.theme_prompt.setPlaceholderText("e.g. cyberpunk, neon, dark fantasy")
        top.addWidget(self.theme_prompt, 1)
        v.addLayout(top)

        # manual table
        g = QGroupBox("Themes")
        gv = QVBoxLayout(g)

        self.table = QTableWidget(0, len(self.COLS))
        self.table.setHorizontalHeaderLabels(self.COLS)
        self.table.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.table.setEditTriggers(QAbstractItemView.AllEditTriggers)
        self.table.horizontalHeader().setStretchLastSection(True)
        gv.addWidget(self.table, 1)

        tb = QHBoxLayout()
        self.add_row_btn = QPushButton("Add Row")
        self.del_row_btn = QPushButton("Delete Selected")
        tb.addWidget(self.add_row_btn)
        tb.addWidget(self.del_row_btn)
        tb.addStretch(1)
        gv.addLayout(tb)
        v.addWidget(g, 1)

        # AI generator (optional)
        ai_box = QGroupBox("Optional: AI Generation")
        form = QFormLayout(ai_box)
        self.ai_count = QSpinBox()
        self.ai_count.setRange(1, 10)
        self.ai_count.setValue(1)
        form.addRow("Number of themes to generate:", self.ai_count)
        self.ai_gen_btn = QPushButton("Generate with AI")
        form.addRow(self.ai_gen_btn)
        v.addWidget(ai_box)

        # wire buttons
        self.add_row_btn.clicked.connect(lambda: self._add_row({}))
        self.del_row_btn.clicked.connect(self._delete_selected)
        self.ai_gen_btn.clicked.connect(self._ai_generate_and_add)

        self._load_themes()

    def _load_themes(self):
        themes = self.cx.pack_global().get("themes", {})
        for name, data in themes.items():
            row_data = {"Name": name, "Background": str(data.get("bg")), "Foreground": str(data.get("fg")), "Border": str(data.get("border")), "Accent": str(data.get("accent")), "Background Image": data.get("background_image", "")}
            self._add_row(row_data)

    def _add_row(self, data: Dict[str, str]):
        r = self.table.rowCount()
        self.table.insertRow(r)
        for c, key in enumerate(self.COLS):
            item = QTableWidgetItem(str(data.get(key, "")))
            self.table.setItem(r, c, item)

    def _delete_selected(self):
        rows = sorted({i.row() for i in self.table.selectedIndexes()}, reverse=True)
        for r in rows:
            self.table.removeRow(r)

    def get_proposals(self) -> Dict[str, Any]:
        themes = {}
        for r in range(self.table.rowCount()):
            row = {k: (self.table.item(r, i).text().strip() if self.table.item(r, i) else "") for i, k in enumerate(self.COLS)}
            if not row["Name"]:
                continue
            
            def parse_color(s):
                try:
                    return json.loads(s)
                except:
                    return [1.0, 1.0, 1.0, 1.0]

            themes[row["Name"]] = {
                "bg": parse_color(row["Background"]),
                "fg": parse_color(row["Foreground"]),
                "border": parse_color(row["Border"]),
                "accent": parse_color(row["Accent"]),
                "background_image": row["Background Image"]
            }
        return {"themes.json": themes}

    def _ai_generate(self, n: int, prompt: str) -> List[dict]:
        svc = AIService(use_openai=True, model=self._get_model())
        pack = self.cx.pack_global()
        
        schema_hint = {
            "themes": [{
                "name": "string",
                "bg": "[r,g,b,a]",
                "fg": "[r,g,b,a]",
                "border": "[r,g,b,a]",
                "accent": "[r,g,b,a]",
                "background_image": "string"
            }]
        }

        task = (
            f"Create {n} color themes for a game UI based on the prompt: '{prompt}'. "
            "Each theme should have a unique name. "
            "The colors should be in the format [r,g,b,a] with values between 0.0 and 1.0. "
            "The background_image can be an empty string."
        )

        try:
            obj, _ = svc.complete_json("themes_generate", schema_hint, task, pack)
            if isinstance(obj, dict) and isinstance(obj.get("themes"), list):
                return list(obj["themes"])
        except Exception as e:
            print("[AI ERROR] _ai_generate (themes):", e)

        return []

    def _ai_generate_and_add(self):
        n = int(self.ai_count.value())
        prompt = self.theme_prompt.text().strip()
        if not prompt:
            QMessageBox.warning(self, "AI Generation", "Please provide a theme prompt.")
            return

        if self._get_use_ai():
            themes = self._ai_generate(n, prompt)
            for theme in themes:
                row_data = {
                    "Name": theme.get("name"),
                    "Background": str(theme.get("bg")),
                    "Foreground": str(theme.get("fg")),
                    "Border": str(theme.get("border")),
                    "Accent": str(theme.get("accent")),
                    "Background Image": theme.get("background_image", "")
                }
                self._add_row(row_data)
        else:
            # offline generation
            for i in range(n):
                new_theme = {
                    "Name": f"{prompt}_{i+1}",
                    "Background": str([round(random.random(), 2) for _ in range(4)]),
                    "Foreground": str([round(random.random(), 2) for _ in range(4)]),
                    "Border": str([round(random.random(), 2) for _ in range(4)]),
                    "Accent": str([round(random.random(), 2) for _ in range(4)]),
                    "Background Image": ""
                }
                self._add_row(new_theme)

# ----- Quest Pack (multi-asset) page ----- 
class QuestPackPage(_BuildPageBase):
    """
    Plans a complete quest + all supporting assets (NPCs, items, dialogue, events, encounters, cutscenes),
    then returns a single multi-file proposals dict for Preview & Apply.

    Writes to (creates files if missing):
      - quests.json
      - npcs.json
      - items.json
      - dialogue.json
      - encounters.json
      - events.json
      - cinematics.json
      - (optional) rooms.json  -> NPC placements into chosen node rooms (by name)
    """

    def __init__(self, root: Path, cx, get_model, get_use_ai, parent=None):
        super().__init__(root, cx, get_model, get_use_ai, parent)
        v = QVBoxLayout(self)

        # Context row: which Node to target (for placements/room references)
        r1 = QHBoxLayout()
        r1.addWidget(QLabel("Node:"))
        self.node_pick = QComboBox()
        for n in self.cx.nodes():
            nid = str(n.get("id"))
            title = n.get("title") or n.get("name") or ""
            self.node_pick.addItem(f"{nid} — {title}", nid)
        r1.addWidget(self.node_pick, 1)

        r1.addWidget(QLabel("Difficulty:"))
        self.diff_pick = QComboBox()
        for d in ("story", "easy", "normal", "hard", "heroic"):
            self.diff_pick.addItem(d, d)
        r1.addWidget(self.diff_pick)

        r1.addWidget(QLabel("Length:"))
        self.length_pick = QComboBox()
        for d in ("short", "standard", "long", "arc"):
            self.length_pick.addItem(d, d)
        r1.addWidget(self.length_pick)

        r1.addWidget(QLabel("ID Prefix:"))
        self.id_prefix = QLineEdit("qpack_")
        self.id_prefix.setPlaceholderText("used to namespace all generated ids")
        r1.addWidget(self.id_prefix, 1)

        v.addLayout(r1)

        # Premise / Theme row
        r2 = QHBoxLayout()
        r2.addWidget(QLabel("Premise / Theme:"))
        self.premise = QLineEdit()
        self.premise.setPlaceholderText("e.g., Nova uncovers a contraband relay coil smuggling ring under Hub 1")
        r2.addWidget(self.premise, 1)
        v.addLayout(r2)

        # --- Optional: also build rooms & NPCs for this node (OFF by default) ---
        row_extra = QHBoxLayout()
        self.qp_make_rooms = QCheckBox("Also generate rooms & NPCs for this quest pack")
        self.qp_make_rooms.setChecked(False)
        row_extra.addWidget(self.qp_make_rooms)

        self.qp_grid_step = QSpinBox(); self.qp_grid_step.setRange(8, 512); self.qp_grid_step.setValue(56)
        row_extra.addWidget(QLabel("Grid step:")); row_extra.addWidget(self.qp_grid_step)

        self.qp_pattern = QComboBox()
        self.qp_pattern.addItem("branch/maze", "maze")
        self.qp_pattern.addItem("linear", "linear")
        self.qp_pattern.addItem("loop", "loop")
        self.qp_pattern.addItem("wheel", "wheel")
        row_extra.addWidget(QLabel("Layout:")); row_extra.addWidget(self.qp_pattern)

        v.addLayout(row_extra)

        # What to generate
        g = QGroupBox("Assets to generate")
        gf = QFormLayout(g)
        self.chk_npcs = QCheckBox("NPC(s)"); self.chk_npcs.setChecked(True)
        self.chk_items = QCheckBox("Item(s)"); self.chk_items.setChecked(True)
        self.chk_dialogue = QCheckBox("Dialogue"); self.chk_dialogue.setChecked(True)
        self.chk_events = QCheckBox("Event(s)"); self.chk_events.setChecked(True)
        self.chk_encounters = QCheckBox("Encounter(s)"); self.chk_encounters.setChecked(True)
        self.chk_cutscenes = QCheckBox("Cutscene(s)"); self.chk_cutscenes.setChecked(True)
        self.chk_place_npcs = QCheckBox("Place NPCs into node rooms (by name match)"); self.chk_place_npcs.setChecked(True)

        self.spin_npcs = QSpinBox(); self.spin_npcs.setRange(0, 20); self.spin_npcs.setValue(2)
        self.spin_items = QSpinBox(); self.spin_items.setRange(0, 50); self.spin_items.setValue(3)
        self.spin_encounters = QSpinBox(); self.spin_encounters.setRange(0, 20); self.spin_encounters.setValue(1)
        self.spin_dialogue = QSpinBox(); self.spin_dialogue.setRange(0, 200); self.spin_dialogue.setValue(12)

        gf.addRow(self.chk_npcs, self.spin_npcs)
        gf.addRow(self.chk_items, self.spin_items)
        gf.addRow(self.chk_encounters, self.spin_encounters)
        gf.addRow(self.chk_dialogue, self.spin_dialogue)
        gf.addRow(self.chk_events)
        gf.addRow(self.chk_cutscenes)
        gf.addRow(self.chk_place_npcs)
        v.addWidget(g)

        # Designer notes to persist
        notes_box = QGroupBox("Designer Notes for AI (quest pack)")
        nv = QVBoxLayout(notes_box)
        self.text_notes = QPlainTextEdit()
        self.text_notes.setPlaceholderText("Beats, fail states, gating, key items, shops/black markets, skill checks, etc.")
        nv.addWidget(self.text_notes)
        v.addWidget(notes_box)

        self._notes_path = self.root / "data" / "notes" / "quest_pack.md"
        if self._notes_path.exists():
            try:
                self.text_notes.setPlainText(self._notes_path.read_text(encoding="utf-8"))
            except Exception:
                pass

        help_lb = QLabel("Generates a cohesive set of assets and wires cross-references (ids). "
                         "Preview shows all proposed JSON updates across files before applying.")
        help_lb.setStyleSheet("color:#888;")
        v.addWidget(help_lb)

    # -------- helpers --------
    def _autogen_rooms_from_quest_pack(
        self,
        *,
        node_id: str,
        quests: Any,
        grid_step: int = 56,
        pattern: str = "maze",
        seed: int = 0
    ) -> Tuple[Dict[str, Any], List[Dict[str, Any]]]:
        """
        Produce a compact, grid-aligned cluster whose rooms vary by archetype
        and are chosen to fit each quest stage's intent.
        Returns (cluster_dict, npc_stubs).
        cluster_dict = {"rooms": [...], "node_append_rooms": [...]}
        """
        import random, re
        rng = random.Random(seed or 0)

        # ---- normalize quest list ----
        if isinstance(quests, dict):
            qlist = [quests]
        else:
            qlist = [q for q in (quests or []) if isinstance(q, dict)]

        # ---- stage parsing + NPC ids ----
        stage_specs: List[Dict[str, Any]] = []  # [{title, stage_id, intent}]
        npc_ids: List[str] = []

        def infer_intent(text: str) -> str:
            """Very light intent classifier (no AI call) to pick room archetypes."""
            t = (text or "").lower()
            # order matters (more specific first)
            if any(k in t for k in ["talk", "meet", "brief", "negotiate", "interview"]): return "talk"
            if any(k in t for k in ["search", "look", "scan", "investigate", "explore"]): return "search"
            if any(k in t for k in ["steal", "retrieve", "collect", "recover", "deliver", "return"]): return "retrieve"
            if any(k in t for k in ["fix", "repair", "calibrate", "assemble", "craft", "build"]): return "tinker"
            if any(k in t for k in ["defend", "guard", "protect", "survive", "ambush"]): return "defend"
            if any(k in t for k in ["sneak", "infiltrate", "bypass", "hack"]): return "sneak"
            if any(k in t for k in ["escort", "guide", "lead"]): return "escort"
            return "generic"

        for q in qlist:
            giver = str(q.get("giver_id") or q.get("giver") or "").strip()
            recv  = str(q.get("receiver_id") or q.get("receiver") or "").strip()
            if giver: npc_ids.append(giver)
            if recv:  npc_ids.append(recv)

            qtitle  = q.get("title") or "Quest"
            stages  = q.get("stages") or []

            # Anchor: briefing room (for the giver)
            stage_specs.append({
                "title": f"{qtitle} – Briefing",
                "stage_id": None,
                "intent": "talk"
            })
            for st in stages:
                st_label = st.get("title") or st.get("description") or st.get("id") or "Stage"
                intent   = infer_intent((st.get("title") or "") + " " + (st.get("description") or ""))
                stage_specs.append({
                    "title": f"{qtitle} – {st_label}",
                    "stage_id": st.get("id"),
                    "intent": intent
                })

        if not stage_specs:
            stage_specs = [{"title":"Quest Hub", "stage_id":None, "intent":"generic"}]

        # ---- id suffix progression for this node ----
        cur_rooms = _read_json_list(self.root / "rooms.json")
        pat = re.compile(rf"^{re.escape(str(node_id))}_(\d+)$")
        mx = 0
        for r in cur_room_list:
            m = pat.match(str(r.get("id") or ""))
            if m:
                try: mx = max(mx, int(m.group(1)))
                except: pass

        def rid(i: int) -> str: return f"{node_id}_{mx + i}"

        # ---- light archetype library ----
        # Each intent maps to one of several archetypes (picked randomly).
        ARCHETYPES: Dict[str, List[Dict[str, Any]]] = {
            "talk": [
                {"kind":"briefing_room", "title_hint":"Briefing Room", "desc":"A cramped office with flickering task lights, maps pinned to walls, and a beat-up desk."},
                {"kind":"canteen", "title_hint":"Canteen Corner", "desc":"A small canteen; clatter of dishes, smell of recycled coffee, low conversations."},
                {"kind":"lobby", "title_hint":"Operations Lobby", "desc":"A check-in kiosk and cracked info boards. People move with quiet urgency."},
            ],
            "search": [
                {"kind":"storage", "title_hint":"Storage Stacks", "desc":"Narrow aisles of crates and bins. Labels peeled, contents mismatched."},
                {"kind":"archives", "title_hint":"Archive Alcove", "desc":"Dusty cabinets and old terminals humming with cold data."},
                {"kind":"workfloor", "title_hint":"Maintenance Floor", "desc":"Tools everywhere; half-disassembled equipment and dangling cables."},
            ],
            "retrieve": [
                {"kind":"vault", "title_hint":"Secured Vault", "desc":"Thick doors. Laser trip-lines etched faintly in the haze."},
                {"kind":"locker", "title_hint":"Supply Lockers", "desc":"Rows of dented lockers with faded stencils and pry marks."},
                {"kind":"market", "title_hint":"Back-Channel Market", "desc":"Makeshift stalls, quiet bartering, wary glances."},
            ],
            "tinker": [
                {"kind":"workshop", "title_hint":"Workshop Bench", "desc":"Solder smoke, oscilloscopes, parts trays sorted by habit not logic."},
                {"kind":"lab", "title_hint":"Calibration Lab", "desc":"Clean benches, flickering diagnostics, sterile swabs and field kits."},
            ],
            "defend": [
                {"kind":"checkpoint", "title_hint":"Security Checkpoint", "desc":"Cover nooks, shields stacked near a barricade line."},
                {"kind":"choke", "title_hint":"Choke Corridor", "desc":"A narrow hallway reinforced with scrap plating and sandbags."},
            ],
            "sneak": [
                {"kind":"service", "title_hint":"Service Ducts", "desc":"Tight crawlspace, the hum of fans, cables brushing shoulders."},
                {"kind":"alley", "title_hint":"Shadow Alley", "desc":"Dim spill-light, condensation drips, and footfalls that aren’t yours."},
            ],
            "escort": [
                {"kind":"transit", "title_hint":"Transit Platform", "desc":"PA crackle, old posters, a maintenance tram that may still run."},
                {"kind":"dorms", "title_hint":"Dorm Corridor", "desc":"Thin doors, hard bunks, personal effects left in tidy rows."},
            ],
            "generic": [
                {"kind":"junction", "title_hint":"Power Junction", "desc":"Cables knot into conduits, warm ozone, status LEDs blinking unevenly."},
                {"kind":"atrium", "title_hint":"Service Atrium", "desc":"A central hollow with catwalks and quick exit routes."},
            ],
        }

        def pick_archetype(intent: str) -> Dict[str, Any]:
            sel = ARCHETYPES.get(intent or "generic") or ARCHETYPES["generic"]
            return rng.choice(sel)

        # ---- choose grid coords (NSEW adjacency) ----
        count = len(stage_specs)
        coords: List[Tuple[int,int]] = []

        if pattern == "linear" or count <= 2:
            start = -(count // 2)
            coords = [(start + i, 0) for i in range(count)]
        elif pattern == "loop":
            side = max(2, int(round((count/4.0))))
            w, h = side, max(2, side)
            ring = []
            for x in range(-w//2, w//2 + 1):          ring.append((x, -h//2))
            for y in range(-h//2 + 1, h//2 + 1):      ring.append((w//2, y))
            for x in range(w//2 - 1, -w//2 - 1, -1):  ring.append((x, h//2))
            for y in range(h//2 - 1, -h//2, -1):      ring.append((-w//2, y))
            step = max(1, len(ring)//count)
            coords = [ring[(i*step)%len(ring)] for i in range(count)]
        elif pattern == "wheel":
            hub = (0,0)
            spokes = max(1, count - 1)
            rim_len = max(4, spokes)
            half = rim_len // 4 + 1
            rim = []
            for x in range(-half, half+1):       rim.append((x, -half))
            for y in range(-half+1, half+1):     rim.append((half, y))
            for x in range(half-1, -half-1, -1): rim.append((x, half))
            for y in range(half-1, -half, -1):   rim.append((-half, y))
            step = max(1, len(rim)//spokes)
            coords = [hub] + [rim[(i*step)%len(rim)] for i in range(spokes)]
        else:
            # "maze" / branching DFS
            visited = {(0,0)}
            order = [(0,0)]
            for _ in range(count-1):
                cx, cy = order[rng.randrange(len(order))]
                for dx, dy in rng.sample([(1,0),(-1,0),(0,1),(0,-1)], 4):
                    nx, ny = cx+dx, cy+dy
                    if (nx,ny) not in visited:
                        visited.add((nx,ny))
                        order.append((nx,ny))
                        break
            coords = order[:count]

        # ---- materialize diverse rooms ----
        rooms: List[Dict[str,Any]] = []
        for i, (spec, (gx,gy)) in enumerate(zip(stage_specs, coords), start=1):
            arch = pick_archetype(spec["intent"])
            # Let stage title lead; append hint to avoid repetitive names
            title = spec["title"]
            if arch["title_hint"] not in title:
                title = f"{title} – {arch['title_hint']}"

            r = {
                "id": rid(i),
                "title": title,
                "description": f"{arch['desc']} (stage={spec['stage_id'] or 'briefing'}, intent={spec['intent']})",
                "pos": [float(gx*grid_step), float(gy*grid_step)],
                "items": [],
                "npcs": [],
                "enemies": [],
                "actions": [],
                "state": {},
                "connections": {},
                "tags": ["autogen", arch["kind"], spec["intent"]],
            }

            # light content seasoning (safe defaults; your editors can ignore or use)
            if spec["intent"] in ("search", "retrieve"):
                r["items"] = [{"id":"loot_crate_common","qty":1}]
            if spec["intent"] in ("defend", "sneak"):
                r["enemies"] = [{"id":"patrol_grunt","level":1}]
            if spec["intent"] == "tinker":
                r["actions"] = [{"id":"bench_craft_basic","kind":"craft"}]

            rooms.append(r)

        # ---- NSEW links to immediate neighbors only ----
        by_pos = { (int(round(r["pos"][0]/grid_step)), int(round(r["pos"][1]/grid_step))): r for r in rooms }
        def link(a: dict, b: dict):
            ax, ay = int(round(a["pos"][0]/grid_step)), int(round(a["pos"][1]/grid_step))
            bx, by = int(round(b["pos"][0]/grid_step)), int(round(b["pos"][1]/grid_step))
            dx, dy = (bx-ax), (by-ay)
            if abs(dx)+abs(dy) != 1: return
            if dx == 1:   d1, d2 = "east","west"
            elif dx == -1:d1, d2 = "west","east"
            elif dy == 1: d1, d2 = "south","north"
            else:         d1, d2 = "north","south"
            a["connections"][d1] = b["id"]
            b["connections"][d2] = a["id"]

        for (gx,gy), a in by_pos.items():
            for dx,dy in [(1,0),(-1,0),(0,1),(0,-1)]:
                b = by_pos.get((gx+dx, gy+dy))
                if b: link(a,b)

        # ---- NPC stubs (giver in first room; receiver in last stage room if present) ----
        npc_stubs: List[Dict[str,Any]] = []
        uniq_npcs = []
        for q in qlist:
            gid = str(q.get("giver_id") or q.get("giver") or "").strip()
            rid = str(q.get("receiver_id") or q.get("receiver") or "").strip()
            for val in [gid, rid]:
                if val and val not in uniq_npcs:
                    uniq_npcs.append(val)

        # map: giver -> first room; receiver -> last non-briefing room (if exists)
        briefing_room_id = rooms[0]["id"]
        last_stage_room_id = rooms[-1]["id"] if len(rooms) > 1 else briefing_room_id

        for q in qlist:
            gid = str(q.get("giver_id") or q.get("giver") or "").strip()
            rid = str(q.get("receiver_id") or q.get("receiver") or "").strip()
            if gid:
                npc_stubs.append({
                    "id": gid,
                    "name": gid.replace("_"," ").title(),
                    "title": "Quest Giver",
                    "node_id": node_id,
                    "room_id": briefing_room_id,
                    "dialogue": [],
                    "tags": ["autogen","giver"]
                })
            if rid:
                npc_stubs.append({
                    "id": rid,
                    "name": rid.replace("_"," ").title(),
                    "title": "Quest Receiver",
                    "node_id": node_id,
                    "room_id": last_stage_room_id,
                    "dialogue": [],
                    "tags": ["autogen","receiver"]
                })

        return {"rooms": rooms, "node_append_rooms": [r["id"] for r in rooms]}, npc_stubs

    def _save_notes(self):
        try:
            self._notes_path.parent.mkdir(parents=True, exist_ok=True)
            self._notes_path.write_text(self.text_notes.toPlainText(), encoding="utf-8")
        except Exception:
            pass

    def _read_list(self, name: str) -> list:
        return _read_json_list(self.root / name)

    def _merge_by_id(self, existing: list, proposed: list, id_key: str = "id") -> list:
        seen = {str(x.get(id_key)) for x in existing if isinstance(x, dict)}
        out = list(existing)
        for obj in proposed or []:
            if not isinstance(obj, dict): 
                continue
            oid = str(obj.get(id_key))
            if not oid or oid in seen: 
                continue
            seen.add(oid)
            out.append(obj)
        return out

    def _ensure_prefixed(self, rows: list, prefix: str, id_key: str = "id") -> list:
        if not prefix: 
            return rows or []
        out = []
        for r in rows or []:
            if isinstance(r, dict) and id_key in r and isinstance(r[id_key], str):
                if not r[id_key].startswith(prefix):
                    r = dict(r)
                    r[id_key] = f"{prefix}{r[id_key]}"
            out.append(r)
        return out

    def _offline_pack(self, nid: str, prefix: str) -> dict:
        # Reasonable, minimal “good default” when Use OpenAI is OFF
        qid = f"{prefix}missing_coil"
        quest = {
            "id": qid,
            "title": "The Missing Coil",
            "giver": f"{prefix}foreman",
            "receiver": f"{prefix}tech",
            "stages": [
                {"id": f"{qid}_1", "title": "Ask around", "goal": "Speak to the foreman in the node."},
                {"id": f"{qid}_2", "title": "Search storage", "goal": "Find the relay coil."},
                {"id": f"{qid}_3", "title": "Return the coil", "goal": "Bring it to the technician."} 
            ],
            "rewards": [{"type": "xp", "amount": 100}, {"type": "item", "id": f"{prefix}coil"}],
            "difficulty": "normal",
            "node": nid,
        }
        npcs = [
            {"id": f"{prefix}foreman", "name": "Grizzled Foreman", "role": "giver", "location": nid, "interactions": [{"type": "talk", "dialogue_id": f"{prefix}dlg_foreman_1"}]},
            {"id": f"{prefix}tech", "name": "Relay Technician", "role": "receiver", "location": nid, "interactions": [{"type": "talk", "dialogue_id": f"{prefix}dlg_tech_1"}]}
        ]
        items = [
            {"id": f"{prefix}coil", "name": "Relay Coil", "type": "quest", "rarity": "common", "description": "A scavenged coil for a relay array."}
        ]
        dialogue = [
            {"id": f"{prefix}dlg_foreman_1", "speaker": f"{prefix}foreman", "text": "Someone nicked the relay coil. Check the storage."},
            {"id": f"{prefix}dlg_tech_1", "speaker": f"{prefix}tech", "text": "Thanks for the coil. The array’s alive again."}
        ]
        encounters = []
        events = [
            {"id": f"{prefix}evt_pickup", "trigger": {"type": "enter_room", "room_id": ""}, "actions": [{"type": "advance_quest", "quest_id": qid, "stage_id": f"{qid}_2"}]}
        ]
        cutscenes = []
        return {"quests": [quest], "npcs": npcs, "items": items, "dialogue": dialogue,
                "encounters": encounters, "events": events, "cutscenes": cutscenes}

    # -------- main hook --------
    def get_proposals(self) -> Dict[str, Any]:
        import json
        self._save_notes()
        nid = str(self.node_pick.currentData() or "")
        prefix = self.id_prefix.text().strip()
        premise = self.premise.text().strip()
        diff = self.diff_pick.currentData() or "normal"
        length = self.length_pick.currentData() or "standard"

        want = {
            "npcs": self.chk_npcs.isChecked(), "items": self.chk_items.isChecked(),
            "dialogue": self.chk_dialogue.isChecked(), "events": self.chk_events.isChecked(),
            "encounters": self.chk_encounters.isChecked(), "cutscenes": self.chk_cutscenes.isChecked(),
            "place_npcs": self.chk_place_npcs.isChecked(),
            "n_npcs": int(self.spin_npcs.value()), "n_items": int(self.spin_items.value()),
            "n_encounters": int(self.spin_encounters.value()), "n_dialogue": int(self.spin_dialogue.value()),
        }

        # ---- context pack (rooms, etc.) ----
        pack = self.cx.pack_for_node(nid)

        # --- debug: show pack size ---
        try:
            pack_json = json.dumps(pack)
            char_len = len(pack_json)
            word_len = len(pack_json.split())
            # crude token estimate: 1 token ≈ 3.5–4 chars in English
            est_tokens = char_len // 4
            print(f"[AI DEBUG] Context pack length: {char_len:,} characters "
                f"(~{word_len:,} words, ≈{est_tokens:,} tokens)")
        except Exception as ex:
            print(f"[AI DEBUG] Could not measure pack size: {ex}")

        # ---- ask model (with robust fallback) ----
        use_ai = bool(self._get_use_ai())
        model = self._get_model()

        obj: Dict[str, Any] = {}
        if use_ai:
            try:
                svc = AIService(use_openai=True, model=model)
                task = (
                    f"Design a cohesive quest pack for node {nid}. Difficulty={diff}, Length={length}.\n"
                    f"Premise: {premise or 'designer will refine later'}\n"
                    f"Counts requested: NPCs={want['n_npcs']}, Items={want['n_items']}, "
                    f"Encounters={want['n_encounters']}, Dialogue lines≈{want['n_dialogue']}.\n"
                    f"Only include sections requested (npcs/items/dialogue/events/encounters/cutscenes). "
                    f"All ids MUST be unique and stable. Prefer referencing EXISTING room_ids from pack.rooms. "
                    f"Cross-link: quests reference stages; events/actions use those quest_ids/stage_ids; "
                    f"npc interactions link dialogue/cutscenes.\n"
                )
                cand, _usage = svc.complete_json("quest_pack_generate", {
                    "quests": [{"id": "string"}],
                    "npcs": [{"id": "string"}],
                    "items": [{"id": "string"}],
                    "dialogue": [{"id": "string"}],
                    "encounters": [{"id": "string"}],
                    "events": [{"id": "string"}],
                    "cutscenes": [{"id": "string", "steps": []}],
                }, task, pack)
                if isinstance(cand, dict):
                    obj = cand
            except Exception as ex:
                QMessageBox.critical(self, "OpenAI error", str(ex))
                obj = {}

        # Fallback if the model returned nothing meaningful
        def _has_any_payload(d: dict) -> bool:
            if not isinstance(d, dict): return False
            for k in ("quests","npcs","items","dialogue","events","encounters","cutscenes"):
                if isinstance(d.get(k), list) and d[k]:
                    return True
            return False

        if not _has_any_payload(obj):
            obj = self._offline_pack(nid, prefix or "qpack_")

        # Respect toggles
        for key in ("npcs", "items", "dialogue", "events", "encounters", "cutscenes"):
            if not want.get(key, False):
                obj[key] = []

        # Prefix ids for safety
        for key in ("quests","npcs","items","dialogue","encounters","events","cutscenes"):
            obj[key] = self._ensure_prefixed(obj.get(key) or [], prefix)

        # ---- Only propose files if they truly gain new IDs ----
        proposals: Dict[str, Any] = {}

        def _read_raw(path):
            try:
                return json.loads(path.read_text(encoding="utf-8"))
            except Exception:
                return None

        # List-shaped files
        list_files = {
            "quests.json": "quests",
            "npcs.json": "npcs",
            "items.json": "items",
            "dialogue.json": "dialogue",
            "encounters.json": "encounters",
            "events.json": "events",
        }

        for fname, key in list_files.items():
            path = self.root / fname
            existing_raw = _read_raw(path)
            if not isinstance(existing_raw, list):
                existing_raw = []  # treat as empty; we won't write unless we add
            merged = self._merge_by_id(existing_raw, obj.get(key) or [])
            if len(merged) > len(existing_raw):  # only if we truly add rows
                proposals[fname] = merged

        # Cinematics: preserve dict-shape { id: [steps...] }
        cin_path = self.root / "cinematics.json"
        existing_cin_raw = _read_raw(cin_path)
        existing_cin_is_dict = isinstance(existing_cin_raw, dict)

        # Normalize to list for merging-by-id
        existing_cin_list = []
        if existing_cin_is_dict:
            for cid, steps in existing_cin_raw.items():
                if isinstance(steps, list):
                    existing_cin_list.append({"id": cid, "steps": steps})
        elif isinstance(existing_cin_raw, list):
            existing_cin_list = existing_cin_raw[:]
        else:
            existing_cin_list = []

        proposed_cins = obj.get("cutscenes") or []
        merged_cins = self._merge_by_id(existing_cin_list, proposed_cins)

        # Convert back and only propose if there are new IDs
        if existing_cin_is_dict:
            if len(merged_cins) > len(existing_cin_list):
                out = dict(existing_cin_raw)  # copy
                for c in merged_cins:
                    cid = c.get("id")
                    steps = c.get("steps")
                    if cid and isinstance(steps, list) and cid not in out:
                        out[cid] = steps
                if out != existing_cin_raw:
                    proposals["cinematics.json"] = out
        else:
            if len(merged_cins) > len(existing_cin_list):
                proposals["cinematics.json"] = merged_cins

        # Optional: naive NPC placement in this node — propose only if it changes rooms
        if want["place_npcs"] and (proposals.get("npcs.json") or obj.get("npcs")):
            rooms_path = self.root / "rooms.json"
            rooms_raw = _read_raw(rooms_path)
            if isinstance(rooms_raw, list):
                by_id = {str(r.get("id")): dict(r) for r in rooms_raw if isinstance(r, dict)}
                baseline = json.dumps(by_id, sort_keys=True)
                node_room_ids = {str(r.get("id")) for r in (pack.get("rooms") or []) if isinstance(r, dict)}
                first_room_id = sorted(node_room_ids)[0] if node_room_ids else None
                if first_room_id:
                    for npc in (obj.get("npcs") or []):
                        if isinstance(npc, dict) and npc.get("location") == nid:
                            r = by_id.get(first_room_id)
                            if r is not None:
                                arr = list(r.get("npcs") or [])
                                if npc.get("name") and npc["name"] not in arr:
                                    arr.append(npc["name"])
                                    r["npcs"] = arr
                                by_id[first_room_id] = r
                after = json.dumps(by_id, sort_keys=True)
                if after != baseline:
                    proposals["rooms.json"] = list(by_id.values())

        return proposals

# ============================================================================== 
# Main Assistant
# ============================================================================== 
class AIAssistant(QWidget):
    def __init__(self, project_root: Optional[str] = None):
        super().__init__()
        self.setWindowTitle("AI Assistant")
        self.root = _find_root(Path(project_root) if project_root else Path(__file__).parent)

        # Context index (use project one if available, else fallback)
        if _RealContextIndex is not None:
            try:
                self.cx = _RealContextIndex(self.root)  # your richer index
            except Exception:
                self.cx = _SimpleContextIndex(self.root)
        else:
            self.cx = _SimpleContextIndex(self.root)

        outer = QVBoxLayout(self)

        # Model & toggle bar
        bar = QHBoxLayout()
        bar.addWidget(QLabel("Model:"))
        self.model_pick = QComboBox()
        self.model_pick.addItems(["gpt-5-mini", "gpt-5", "o4-mini"])
        self.model_pick.setCurrentIndex(0)
        bar.addWidget(self.model_pick)
        self.use_ai = QCheckBox("Use OpenAI")
        bar.addWidget(self.use_ai)
        # Auto-enable if a key is present
        if os.getenv("OPENAI_API_KEY") or os.getenv("OPENAI_API_KEY_STARBORN"):
            self.use_ai.setChecked(True)

        # Status + ping
        self.ai_status = QLabel()
        self.ai_status.setStyleSheet("color:#888")
        bar.addWidget(self.ai_status)

        self.ping_btn = QPushButton("Ping")
        bar.addWidget(self.ping_btn)
        self.ping_btn.clicked.connect(self._ping_ai)

        bar.addStretch(1)
        outer.addLayout(bar)

        self.status_lbl = QLabel("Ready.")
        
        # --- Global Briefing box (saved to data/assistant_briefing.md) ---
        brief = QGroupBox("Global Briefing for AI")
        bvl = QVBoxLayout(brief)
        self.brief_edit = QPlainTextEdit()
        self.brief_edit.setPlaceholderText(
            "World tone, factions, tech rules, ID conventions, balancing goals…\n"
            "Anything the AI should always know when building content.")
        bvl.addWidget(self.brief_edit)
        brow = QHBoxLayout()
        self.save_brief_btn = QPushButton("Save Briefing")
        brow.addStretch(1); brow.addWidget(self.save_brief_btn)
        bvl.addLayout(brow)
        outer.addWidget(brief)

        # Load existing briefing if present
        self._brief_path = self.root / "data" / "assistant_briefing.md"
        if self._brief_path.exists():
            try:
                self.brief_edit.setPlainText(self._brief_path.read_text(encoding="utf-8"))
            except Exception:
                pass

        def _save_briefing():
            p = self._brief_path
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(self.brief_edit.toPlainText(), encoding="utf-8")

        self.save_brief_btn.clicked.connect(_save_briefing)
        self._save_briefing = _save_briefing  # keep a handle for later

        # Tabs
        tabs = QTabWidget(); outer.addWidget(tabs, 1)

        # Ask tab
        ask = QWidget(); tabs.addTab(ask, "Ask")
        av = QVBoxLayout(ask)
        row = QHBoxLayout()
        row.addWidget(QLabel("Question:"))
        self.ask_in = QLineEdit(); row.addWidget(self.ask_in, 1)
        self.ask_btn = QPushButton("Ask"); row.addWidget(self.ask_btn)
        av.addLayout(row)
        self.ask_out = QTextEdit(); self.ask_out.setReadOnly(True); av.addWidget(self.ask_out, 1)
        self.ask_btn.clicked.connect(self._on_ask)

        # Build tab
        build = QWidget(); tabs.addTab(build, "Build")
        bv = QVBoxLayout(build)

        # kind picker
        krow = QHBoxLayout()
        krow.addWidget(QLabel("What to build:"))
        self.kind = QComboBox()
        # Order matters: indexes map to stacked pages
        kinds = [
            "Room Cluster (for a Node)",
            "Quest Pack (everything)",  
            "Item(s)",
            "Quest Skeleton(s)",
            "Dialogue Lines",
            "NPC(s)",
            "Encounter(s)",
            "Skills",
            "Enemies",
            "Shops",
            "Event(s)",
            "Cutscene(s)",
            "Cooking Recipes",
            "Tinkering Recipes",
            "Fishing System",
            "Themes"
        ]
        for k in kinds:
            self.kind.addItem(k)
        krow.addWidget(self.kind, 1)
        bv.addLayout(krow)

        # stacked dynamic area
        self.stack = QStackedWidget(); bv.addWidget(self.stack, 1)

        # pages (ORDER MATTERS: must match 'kinds' above)
        self.page_rooms = RoomClusterPage(self.root, self.cx, self._current_model, self._use_ai, status_lbl=self.status_lbl)
        self.page_items = ItemsPage(self.root, self.cx, self._current_model, self._use_ai)
        self.page_quests = QuestsPage(self.root, self.cx, self._current_model, self._use_ai)
        self.page_dialogue = DialoguePage(self.root, self.cx, self._current_model, self._use_ai)
        self.page_npcs = NPCsPage(self.root, self.cx, self._current_model, self._use_ai)
        self.page_encounters = EncountersPage(self.root, self.cx, self._current_model, self._use_ai)
        self.page_skills = SkillsPage(self.root, self.cx, self._current_model, self._use_ai)
        self.page_enemies = EnemiesPage(self.root, self.cx, self._current_model, self._use_ai)
        self.page_shops = ShopsPage(self.root, self.cx, self._current_model, self._use_ai)
        self.page_events = EventsPage(self.root, self.cx, self._current_model, self._use_ai)
        self.page_cutscenes = CutscenesPage(self.root, self.cx, self._current_model, self._use_ai)
        self.page_cooking = CookingPage(self.root, self.cx, self._current_model, self._use_ai)
        self.page_tinkering = TinkeringPage(self.root, self.cx, self._current_model, self._use_ai)
        self.page_fishing = FishingPage(self.root, self.cx, self._current_model, self._use_ai)
        self.page_questpack = QuestPackPage(self.root, self.cx, self._current_model, self._use_ai)
        self.page_themes = ThemesPage(self.root, self.cx, self._current_model, self._use_ai)

        for p in [
            self.page_rooms, self.page_questpack, self.page_items, self.page_quests, self.page_dialogue, self.page_npcs, self.page_encounters,
            self.page_skills, self.page_enemies, self.page_shops, self.page_events, self.page_cutscenes,
            self.page_cooking, self.page_tinkering, self.page_fishing, self.page_themes
        ]:
            self.stack.addWidget(p)

        self.kind.currentIndexChanged.connect(self.stack.setCurrentIndex)
        self.kind.setCurrentIndex(0)

        # preview/apply
        brow = QHBoxLayout()
        self.preview_btn = QPushButton("Preview")
        self.apply_btn = QPushButton("Apply…"); self.apply_btn.setEnabled(False)
        brow.addStretch(1); brow.addWidget(self.preview_btn); brow.addWidget(self.apply_btn)
        bv.addLayout(brow)

        self.preview = QTextEdit(); self.preview.setReadOnly(True)
        bv.addWidget(self.preview, 1)

        self._last_proposals: Dict[str, Any] = {}
        self.preview_btn.clicked.connect(self._on_preview)
        self.apply_btn.clicked.connect(self._on_apply)

        # Bottom status line
        outer.addWidget(self.status_lbl)
        self.resize(1200, 900)

    def _current_model(self) -> str:
        return self.model_pick.currentText().strip() or "gpt-5-mini"

    def _use_ai(self) -> bool:
        return bool(self.use_ai.isChecked())

    def _on_ask(self):
        q = self.ask_in.text().strip()
        if not q: return
        try:
            self._save_briefing()
        except Exception:
            pass
        svc = AIService(use_openai=self._use_ai(), model=self._current_model())
        self.ask_out.setPlainText(svc.ask_text(q, self.cx.pack_global()))

    def _on_preview(self):
        try:
            self._save_briefing()
        except Exception:
            pass

        page = self.stack.currentWidget()
        proposals = {}  # ensure defined even if generation raises
        try:
            proposals = page.get_proposals()
        except Exception as e:
            self.preview.setPlainText(f"Error preparing proposals:\n{e}")
            self.apply_btn.setEnabled(False)
            self._last_proposals = {}
            count = len(proposals)
            when = datetime.now().strftime("%H:%M:%S")
            self.status_lbl.setText(f"Preview ready at {when} • {count} file(s) staged")
            print("[Assistant] Preview staged", len(proposals), "file(s)")
            return

        if not proposals:
            self.preview.setPlainText("Nothing to preview.")
            self.apply_btn.setEnabled(False)
            self._last_proposals = {}
            return

        # Build diffs
        parts = []
        for name, payload in proposals.items():
            p = self.root / name
            old = p.read_text(encoding="utf-8") if p.exists() else ""
            new = json.dumps(payload, ensure_ascii=False, indent=4)
            diff = _unified_diff(old, new, name)
            parts.append(f"--- {name} ---\n{diff or '(no changes)'}\n")
        self.preview.setPlainText("\n".join(parts))
        self._last_proposals = proposals
        self.apply_btn.setEnabled(True)

    def _on_apply(self):
        if not self._last_proposals:
            self.preview.setPlainText("No pending changes. Click Preview first.")
            return
        dlg = DiffApplyDialog(self.root, self._last_proposals, self)
        if dlg.exec_():
            self._last_proposals = {}
            self.apply_btn.setEnabled(False)
            self.preview.setPlainText("Applied. Open your editors to review content.")
            when = datetime.now().strftime("%H:%M:%S")
            self.status_lbl.setText(f"Applied at {when} • All staged files written")
            print("[Assistant] Apply complete")

    def _refresh_ai_status(self):
        is_stub = (AIService.__module__ == __name__)
        has_key = bool(os.getenv("OPENAI_API_KEY") or os.getenv("OPENAI_API_KEY_STARBORN"))

        # Detect whether the OpenAI SDK is present
        try:
            import openai  # noqa: F401
            has_sdk = True
        except Exception:
            has_sdk = False

        if is_stub:
            msg = "AI: OFF (ai_service import failed → stub)"
        elif not has_sdk:
            msg = "AI: OFF (openai SDK missing)"
        elif not self._use_ai():
            msg = "AI: OFF (checkbox off)"
        elif not has_key:
            msg = "AI: OFF (no OPENAI_API_KEY)"
        else:
            msg = f"AI: Ready ({self._current_model()})"
        self.ai_status.setText(msg)

    def _ping_ai(self):
        import time
        t0 = time.time()

        # Gather diagnostics up-front so the message is actionable
        reasons = []
        is_stub = (AIService.__module__ == __name__)
        has_key = bool(os.getenv("OPENAI_API_KEY") or os.getenv("OPENAI_API_KEY_STARBORN"))

        try:
            import openai  # noqa: F401
            has_sdk = True
        except Exception:
            has_sdk = False
            reasons.append("OpenAI SDK not installed (pip install openai)")

        if is_stub:
            reasons.append("ai_service.py import failed (ensure tools/ is on PYTHONPATH)")
        if not has_key:
            reasons.append("OPENAI_API_KEY not set in environment")
        if not self._use_ai():
            reasons.append("'Use OpenAI' toggle is OFF")

        if reasons:
            QMessageBox.warning(self, "Ping", "Can't reach OpenAI:\n- " + "\n- ".join(reasons))
            self._refresh_ai_status()
            return

        # Everything looks wired; actually call the model once
        try:
            svc = AIService(use_openai=True, model=self._current_model())
            out = svc.ask_text("Return exactly: PONG", self.cx.pack_global())
            ms = int((time.time() - t0) * 1000)
            QMessageBox.information(self, "Ping", f"OpenAI responded in {ms} ms:\n\n{out[:200]}")
        except Exception as e:
            QMessageBox.critical(self, "Ping failed", str(e))
        finally:
            self._refresh_ai_status()

# ------------------------------------------------------------------------------
def main():
    import sys
    app = QApplication(sys.argv)
    w = AIAssistant()
    w.show()
    sys.exit(app.exec_())

if __name__ == "__main__":
    main()