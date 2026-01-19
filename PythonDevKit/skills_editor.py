#!/usr/bin/env python3
# tools/skills_editor.py — Skills + Skill Trees editor (grid + edges + drag)
from __future__ import annotations
import json, shutil
from pathlib import Path
from typing import Any, Dict, List, Optional, Set, Tuple

from devkit_paths import resolve_paths

from PyQt5.QtCore import Qt, QPointF
from PyQt5.QtGui import QPainter, QColor, QPen, QBrush
from PyQt5.QtWidgets import (
    QApplication, QWidget, QTabWidget, QHBoxLayout, QVBoxLayout, QListWidget, QListWidgetItem,
    QLabel, QLineEdit, QComboBox, QSpinBox, QTextEdit, QPushButton, QFormLayout,
    QMessageBox, QGroupBox, QTableWidget, QTableWidgetItem, QAbstractItemView,
    QInputDialog, QSplitter, QDialog, QDialogButtonBox, QCheckBox, QHeaderView,
    QListView
)

# ------------------------------------------------------------
# Project root + I/O helpers (root-aware, with .bak backup)
# ------------------------------------------------------------
def _find_root(start: Optional[Path] = None) -> Path:
    return resolve_paths(start or Path(__file__).parent).assets_dir

def _read_json(p: Path, default):
    try:
        if not p.exists():
            return default
        return json.loads(p.read_text("utf-8"))
    except Exception:
        return default

def _write_json(p: Path, data, *, title: str = "json") -> bool:
    p.parent.mkdir(parents=True, exist_ok=True)
    try:
        if p.exists():
            shutil.copy2(p, p.with_suffix(p.suffix + ".bak"))
        p.write_text(json.dumps(data, indent=2, ensure_ascii=False), "utf-8")
        return True
    except Exception as e:
        QMessageBox.critical(None, f"Save Error ({title})", f"Failed to save {p.name}:\n{e}")
        return False


# ============================================================
# Skills Tab (flat /skills.json editor) — polished
#  - Preserves unknown keys
#  - Adds optional fields for max_rank, scaling, combat_tags
# ============================================================
class SkillsTab(QWidget):
    """Flat list of skill definitions (id/name/type/etc.)."""
    def __init__(self, root: Path):
        super().__init__()
        self.root = root
        self.path_skills = self.root / "skills.json"
        self.data: List[Dict[str, Any]] = _read_json(self.path_skills, [])
        self._build_ui()
        self._reload_list()

    # ---------- UI ----------
    def _build_ui(self):
        layout = QHBoxLayout(self)

        # list
        self.list = QListWidget()
        self.list.currentRowChanged.connect(self._on_select)
        layout.addWidget(self.list, 2)

        # inspector
        right = QVBoxLayout()
        form = QFormLayout()

        self.f_id = QLineEdit()
        self.f_name = QLineEdit()
        self.f_char = QLineEdit()
        self.f_type = QComboBox(); self.f_type.addItems(["active", "passive"])
        self.f_power = QSpinBox(); self.f_power.setRange(0, 99999)
        self.f_cooldown = QSpinBox(); self.f_cooldown.setRange(0, 99)
        self.f_max_rank = QSpinBox(); self.f_max_rank.setRange(0, 99)
        self.f_scaling = QLineEdit()
        self.f_conditions = QLineEdit()    # execution conditions
        self.f_tags = QLineEdit()          # generic tags
        self.f_cbtags = QLineEdit()        # combat_tags
        self.f_desc = QTextEdit()

        form.addRow("ID", self.f_id)
        form.addRow("Name", self.f_name)
        form.addRow("Character", self.f_char)
        form.addRow("Type", self.f_type)
        form.addRow("Base Power", self.f_power)
        form.addRow("Cooldown", self.f_cooldown)
        form.addRow("Max Rank", self.f_max_rank)
        form.addRow("Scaling", self.f_scaling)
        form.addRow("Conditions", self.f_conditions)
        form.addRow("Tags (comma)", self.f_tags)
        form.addRow("Combat Tags (comma)", self.f_cbtags)
        form.addRow(QLabel("Description"))
        form.addRow(self.f_desc)

        btns = QHBoxLayout()
        b_new = QPushButton("New"); b_dup = QPushButton("Duplicate"); b_del = QPushButton("Delete")
        b_ren = QPushButton("Rename…")
        b_val = QPushButton("Validate"); b_save = QPushButton("Save")
        b_new.clicked.connect(self._on_new)
        b_dup.clicked.connect(self._on_duplicate)
        b_del.clicked.connect(self._on_delete)
        b_ren.clicked.connect(self._on_rename)
        b_val.clicked.connect(self._on_validate_click)
        b_save.clicked.connect(self.save)
        for b in (b_new,b_dup,b_del,b_ren): btns.addWidget(b)
        btns.addStretch(1)
        for b in (b_val,b_save): btns.addWidget(b)

        right.addLayout(form)
        right.addLayout(btns)
        layout.addLayout(right, 5)

        # shortcuts
        b_save.setShortcut("Ctrl+S")
        b_val.setShortcut("Ctrl+Shift+V")

    def _reload_list(self):
        self.list.clear()
        for s in self.data:
            QListWidgetItem(f"{s.get('id','?<id>')} — {s.get('name','')}", self.list)
        if self.data:
            self.list.setCurrentRow(0)

    def _current(self) -> Optional[Dict[str, Any]]:
        r = self.list.currentRow()
        if r < 0 or r >= len(self.data): return None
        return self.data[r]

    def _collect(self) -> Dict[str, Any]:
        # Merge known fields onto existing record to preserve unknown keys
        base = dict(self._current() or {})
        base.update({
            "id": self.f_id.text().strip(),
            "name": self.f_name.text().strip(),
            "character": self.f_char.text().strip(),
            "type": self.f_type.currentText(),
            "base_power": int(self.f_power.value()),
            "cooldown": int(self.f_cooldown.value()),
            "max_rank": int(self.f_max_rank.value()) if self.f_max_rank.value() else base.get("max_rank", 0),
            "scaling": self.f_scaling.text().strip() or base.get("scaling",""),
            "conditions": self.f_conditions.text().strip(),
            "tags": [t.strip() for t in self.f_tags.text().split(",") if t.strip()],
            "combat_tags": [t.strip() for t in self.f_cbtags.text().split(",") if t.strip()],
            "description": self.f_desc.toPlainText().strip(),
        })
        # Drop empty arrays for cleanliness
        if not base.get("tags"): base.pop("tags", None)
        if not base.get("combat_tags"): base.pop("combat_tags", None)
        if not base.get("scaling"): base.pop("scaling", None)
        if not base.get("max_rank"): base.pop("max_rank", None)
        if not base.get("conditions"): base.pop("conditions", None)
        return base

    def _load(self, s: Dict[str, Any]):
        self.f_id.setText(s.get("id",""))
        self.f_name.setText(s.get("name",""))
        self.f_char.setText(s.get("character",""))
        self.f_type.setCurrentText(s.get("type","active"))
        self.f_power.setValue(int(s.get("base_power",0)))
        self.f_cooldown.setValue(int(s.get("cooldown",0)))
        self.f_max_rank.setValue(int(s.get("max_rank",0)))
        self.f_scaling.setText(str(s.get("scaling","")))
        self.f_conditions.setText(str(s.get("conditions", "")))
        self.f_tags.setText(", ".join(s.get("tags",[])))
        self.f_cbtags.setText(", ".join(s.get("combat_tags",[])))
        self.f_desc.setPlainText(s.get("description",""))

    # ---------- actions ----------
    def _on_select(self, row: int):
        s = self._current()
        if not s: return
        self._load(s)

    def _on_new(self):
        used = {s.get("id","") for s in self.data}
        base, i = "new_skill", 1
        nid = base
        while nid in used:
            i += 1; nid = f"{base}_{i}"
        new = {"id":nid,"name":"New Skill","character":"","type":"active",
               "base_power":0,"cooldown":0,"tags":[],"description":""}
        self.data.append(new); self._reload_list(); self.list.setCurrentRow(len(self.data)-1)

    def _on_duplicate(self):
        cur = self._current()
        if not cur: return
        dup = json.loads(json.dumps(cur))
        base = cur.get("id","skill") or "skill"
        used = {s.get("id","") for s in self.data}
        i, new_id = 1, f"{base}_copy"
        while new_id in used:
            i += 1; new_id = f"{base}_copy{i}"
        dup["id"] = new_id
        self.data.append(dup); self._reload_list(); self.list.setCurrentRow(len(self.data)-1)

    def _on_delete(self):
        r = self.list.currentRow()
        if r < 0: return
        if QMessageBox.question(self, "Delete?", "Delete this skill?", QMessageBox.Yes|QMessageBox.No) != QMessageBox.Yes:
            return
        del self.data[r]; self._reload_list()

    def _on_rename(self):
        cur = self._current()
        if not cur: return
        new_id, ok = QInputDialog.getText(self, "Rename Skill", "New ID:", text=cur.get("id",""))
        if not ok: return
        new_id = str(new_id).strip()
        if not new_id:
            QMessageBox.warning(self,"Invalid","ID cannot be empty."); return
        if any(s.get("id")==new_id for s in self.data if s is not cur):
            QMessageBox.warning(self,"Invalid","ID already exists."); return
        cur["id"] = new_id
        self._reload_list()

    def save(self) -> bool:
        r = self.list.currentRow()
        if 0 <= r < len(self.data):
            self.data[r] = self._collect()
        issues = self.validate()
        if issues:
            QMessageBox.warning(self, "Skills: Validate", "\n".join(issues[:200]))
            return False
        if _write_json(self.path_skills, self.data, title="skills"):
            QMessageBox.information(self, "Skills", "Saved.")
            self._reload_list()
            return True
        return False

    def validate(self) -> List[str]:
        issues: List[str] = []
        seen: Set[str] = set()
        for s in self.data:
            sid = (s.get("id") or "").strip()
            if not sid:
                issues.append("Missing id")
            if sid in seen:
                issues.append(f"Duplicate id: {sid}")
            seen.add(sid)

            t = s.get("type","active")
            if t not in ("active","passive"):
                issues.append(f"{sid}: bad type '{t}'")

            cd = int(s.get("cooldown",0))
            if cd < 0:
                issues.append(f"{sid}: cooldown must be >= 0")
        return issues

    def _on_validate_click(self):
        issues = self.validate()
        if issues:
            QMessageBox.warning(self, "Skills", "\n".join(issues[:200]))
        else:
            QMessageBox.information(self, "Skills", "No issues found.")


# ============================================================
# Skill Trees Tab
#  - character switcher -> branches -> nodes
#  - grid view with edges, drag-to-move, zoom buttons
#  - inspector for node + effect dict
# ============================================================
class NodeGridView(QWidget):
    """
    Grid preview for a branch:
    - (0,0) is top-left cell, integers only
    - click to select a node (via callbacks)
    - drag to move node; snaps to integer coords and updates via callback
    - draws 'requires' edges for links within the same branch
    """
    def __init__(self, parent=None):
        super().__init__(parent)
        self.setMinimumHeight(300)
        self.setMouseTracking(True)
        self._cell = 48      # pixels per grid cell (base)
        self._zoom = 1.0
        self._margin = 16
        self._nodes: List[Dict[str, Any]] = []
        self._edges: List[Tuple[str,str]] = []   # (from_id -> to_id) (req -> node)
        self._id_to_index: Dict[str, int] = {}
        self._selected_id: Optional[str] = None
        self._drag_id: Optional[str] = None
        self._on_select = None
        self._on_move = None

    # ----- public API -----
    def set_callbacks(self, on_select, on_move):
        self._on_select = on_select
        self._on_move = on_move

    def set_nodes_and_edges(self, nodes: List[Dict[str, Any]], edges: List[Tuple[str,str]]):
        self._nodes = nodes or []
        self._edges = edges or []
        self._id_to_index = { (n.get("id") or ""): i for i, n in enumerate(self._nodes) }
        self.update()

    def set_selected(self, node_id: Optional[str]):
        self._selected_id = node_id
        self.update()

    def zoom_in(self):  self._zoom = min(2.5, self._zoom + 0.1); self.update()
    def zoom_out(self): self._zoom = max(0.4, self._zoom - 0.1); self.update()
    def zoom_reset(self): self._zoom = 1.0; self.update()

    # ----- helpers -----
    def _grid_to_px(self, gx: int, gy: int) -> QPointF:
        cs = self._cell * self._zoom
        x = self._margin + gx * cs
        y = self._margin + gy * cs
        return QPointF(x, y)

    def _px_to_grid(self, x: float, y: float) -> Tuple[int, int]:
        cs = self._cell * self._zoom
        gx = int(round((x - self._margin) / cs))
        gy = int(round((y - self._margin) / cs))
        return gx, gy

    def _node_center_px(self, n: Dict[str,Any]) -> Tuple[float,float,float]:
        cs = self._cell * self._zoom
        pos = n.get("pos", [0,0])
        gx, gy = int(pos[0]), int(pos[1])
        px = self._margin + gx * cs
        py = self._margin + gy * cs
        r = cs * 0.35
        return px, py, r

    def _hit_test(self, x: float, y: float) -> Optional[str]:
        cs = self._cell * self._zoom
        r = cs * 0.35
        for n in self._nodes:
            nid = (n.get("id") or "")
            px, py, _ = self._node_center_px(n)
            if (x - px) ** 2 + (y - py) ** 2 <= (r * 1.15) ** 2:
                return nid
        return None

    # ----- events -----
    def mousePressEvent(self, ev):
        if ev.button() != Qt.LeftButton:
            return
        nid = self._hit_test(ev.x(), ev.y())
        if nid:
            self._drag_id = nid
            self.set_selected(nid)
            if self._on_select:
                self._on_select(nid)

    def mouseMoveEvent(self, ev):
        if self._drag_id is None:
            return
        nid = self._drag_id
        gx, gy = self._px_to_grid(ev.x(), ev.y())
        if self._on_move:
            self._on_move(nid, gx, gy)
        self.update()

    def mouseReleaseEvent(self, ev):
        self._drag_id = None

    def paintEvent(self, ev):
        p = QPainter(self)
        w, h = self.width(), self.height()
        cs = self._cell * self._zoom

        # grid
        p.fillRect(0, 0, w, h, QColor(20, 25, 32))
        pen = QPen(QColor(36, 42, 52)); p.setPen(pen)
        for x in range(self._margin, w, int(cs)):
            p.drawLine(x, 0, x, h)
        for y in range(self._margin, h, int(cs)):
            p.drawLine(0, y, w, y)

        # row gate labels (every row on the left)
        p.setPen(QPen(QColor(70, 90, 110)))
        for gy in range(0, int((h - self._margin) / cs) + 1):
            y = self._margin + gy * cs
            p.drawText(2, int(y - 2), f"r{gy} (>{gy*5} AP)")

        # edges (requires)
        p.setPen(QPen(QColor(90, 120, 160), 2))
        for req_id, to_id in self._edges:
            i_from = self._id_to_index.get(req_id, -1)
            i_to   = self._id_to_index.get(to_id, -1)
            if i_from < 0 or i_to < 0:  # only draw edges inside this branch
                continue
            px1, py1, _ = self._node_center_px(self._nodes[i_from])
            px2, py2, _ = self._node_center_px(self._nodes[i_to])
            p.drawLine(int(px1), int(py1), int(px2), int(py2))

        # nodes
        for n in self._nodes:
            nid = n.get("id","")
            px, py, r = self._node_center_px(n)
            sel = (nid == self._selected_id)
            p.setBrush(QBrush(QColor(90, 170, 255) if sel else QColor(140, 160, 200)))
            p.setPen(QPen(QColor(10, 30, 40) if sel else QColor(30, 44, 64), 2))
            p.drawEllipse(QPointF(px, py), r, r)
            # id label
            p.setPen(QPen(QColor(230, 240, 255)))
            p.drawText(int(px - r), int(py - r - 4), int(r*2), int(r),
                       Qt.AlignCenter, nid)


class TreesTab(QWidget):
    """Editor for /skill_trees/*.json per-character trees."""
    def __init__(self, root: Path):
        super().__init__()
        self.root = root
        self.dir_trees = self.root / "skill_trees"
        self.dir_trees.mkdir(parents=True, exist_ok=True)
        self.path_skills = self.root / "skills.json"

        self._skills_index: Set[str] = set()
        self._trees: Dict[str, Dict[str, Any]] = {}  # char -> tree dict
        self._char_order: List[str] = []
        self._current_char: Optional[str] = None
        self._current_branch: Optional[str] = None
        self._current_node_id: Optional[str] = None

        self._build_ui()
        self.reload_lookups()
        self._load_all_trees()
        self._refresh_char_list()

    # ---------- load ----------
    def reload_lookups(self):
        # known skill IDs from /skills.json (optional cross-check)
        skills = _read_json(self.path_skills, [])
        self._skills_index = { s.get("id","") for s in skills if s.get("id") }

    def _load_all_trees(self):
        self._trees.clear()
        self._char_order.clear()
        for p in sorted(self.dir_trees.glob("*.json")):
            data = _read_json(p, {})
            if not data: continue
            ch = data.get("character")
            if not ch:  # skip bad file
                continue
            self._trees[ch] = data
            self._char_order.append(ch)

    # ---------- UI ----------
    def _build_ui(self):
        root = QVBoxLayout(self)

        # top bar: character switcher + save/validate/new
        top = QHBoxLayout()
        top.addWidget(QLabel("Character:"))
        self.cmb_char = QComboBox()
        self.cmb_char.setEditable(False)
        self.cmb_char.currentTextChanged.connect(self._on_char_changed)
        top.addWidget(self.cmb_char, 1)
        self.btn_new_tree = QPushButton("New Tree…"); self.btn_dup_tree = QPushButton("Duplicate…")
        self.btn_del_tree = QPushButton("Delete Tree")
        self.btn_save_tree = QPushButton("Save Tree"); self.btn_val_tree = QPushButton("Validate")
        self.btn_new_tree.clicked.connect(self._new_tree)
        self.btn_dup_tree.clicked.connect(self._dup_tree)
        self.btn_del_tree.clicked.connect(self._del_tree)
        self.btn_save_tree.clicked.connect(self.save)
        self.btn_val_tree.clicked.connect(self._on_validate)
        for b in (self.btn_new_tree, self.btn_dup_tree, self.btn_del_tree):
            top.addWidget(b)
        top.addStretch(1)
        for b in (self.btn_val_tree, self.btn_save_tree):
            top.addWidget(b)
        root.addLayout(top)

        # main split: left (branches + nodes table) | right (grid + inspector)
        split = QSplitter()
        split.setOrientation(Qt.Horizontal)
        root.addWidget(split, 1)

        left = QWidget(); left_l = QVBoxLayout(left)
        # Branches
        br_bar = QHBoxLayout()
        br_bar.addWidget(QLabel("Branches"))
        self.btn_add_branch = QPushButton("Add")
        self.btn_ren_branch = QPushButton("Rename")
        self.btn_del_branch = QPushButton("Delete")
        self.btn_add_branch.clicked.connect(self._add_branch)
        self.btn_ren_branch.clicked.connect(self._ren_branch)
        self.btn_del_branch.clicked.connect(self._del_branch)
        for b in (self.btn_add_branch, self.btn_ren_branch, self.btn_del_branch): br_bar.addWidget(b)
        left_l.addLayout(br_bar)

        self.list_branches = QListWidget()
        self.list_branches.currentTextChanged.connect(self._on_branch_changed)
        left_l.addWidget(self.list_branches, 1)

        # Nodes table
        nt_bar = QHBoxLayout()
        nt_bar.addWidget(QLabel("Nodes"))
        self.btn_new_node = QPushButton("New")
        self.btn_dup_node = QPushButton("Duplicate")
        self.btn_del_node = QPushButton("Delete")
        self.btn_new_node.clicked.connect(self._new_node)
        self.btn_dup_node.clicked.connect(self._dup_node)
        self.btn_del_node.clicked.connect(self._del_node)
        for b in (self.btn_new_node, self.btn_dup_node, self.btn_del_node): nt_bar.addWidget(b)
        left_l.addLayout(nt_bar)

        self.tbl_nodes = QTableWidget(0, 7)
        self.tbl_nodes.setHorizontalHeaderLabels(["ID","Name","X","Y","AP","Requires","Effect.type"])
        self.tbl_nodes.horizontalHeader().setSectionResizeMode(QHeaderView.Stretch)
        self.tbl_nodes.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.tbl_nodes.setEditTriggers(QAbstractItemView.NoEditTriggers)
        self.tbl_nodes.itemSelectionChanged.connect(self._on_table_select)
        left_l.addWidget(self.tbl_nodes, 2)

        split.addWidget(left)

        # Right: grid + inspector
        right = QWidget(); right_l = QVBoxLayout(right)

        # Grid controls
        grid_bar = QHBoxLayout()
        grid_bar.addWidget(QLabel("Branch Layout"))
        b_in = QPushButton("+"); b_out = QPushButton("–"); b_1 = QPushButton("1:1")
        b_in.clicked.connect(lambda: self.grid.zoom_in())
        b_out.clicked.connect(lambda: self.grid.zoom_out())
        b_1.clicked.connect(lambda: self.grid.zoom_reset())
        grid_bar.addStretch(1)
        for b in (b_out, b_1, b_in): grid_bar.addWidget(b)
        right_l.addLayout(grid_bar)

        self.grid = NodeGridView()
        self.grid.set_callbacks(self._grid_select, self._grid_move)
        right_l.addWidget(self.grid, 3)

        # Inspector
        box = QGroupBox("Node Inspector")
        form = QFormLayout(box)
        self.f_n_id = QLineEdit()
        self.f_n_name = QLineEdit()
        self.f_n_x = QSpinBox(); self.f_n_x.setRange(-99, 999)
        self.f_n_y = QSpinBox(); self.f_n_y.setRange(0, 999)
        self.f_n_ap = QSpinBox(); self.f_n_ap.setRange(0, 99)

        # requires: list editor
        req_row = QHBoxLayout()
        self.list_req = QListWidget(); self.list_req.setMaximumHeight(90)
        self.btn_req_add = QPushButton("Add…")
        self.btn_req_del = QPushButton("Remove")
        self.btn_req_add.clicked.connect(self._req_add)
        self.btn_req_del.clicked.connect(self._req_del)
        req_controls = QVBoxLayout(); req_controls.addWidget(self.btn_req_add); req_controls.addWidget(self.btn_req_del); req_controls.addStretch(1)
        req_row.addWidget(self.list_req, 1); req_row.addLayout(req_controls)

        # effect: generic small KV table
        eff_row = QVBoxLayout()
        self.tbl_eff = QTableWidget(0, 2)
        self.tbl_eff.setHorizontalHeaderLabels(["Key","Value"])
        self.tbl_eff.horizontalHeader().setSectionResizeMode(QHeaderView.Stretch)
        eff_btns = QHBoxLayout()
        self.btn_eff_add = QPushButton("Add KV"); self.btn_eff_del = QPushButton("Delete KV")
        self.btn_eff_add.clicked.connect(self._eff_add)
        self.btn_eff_del.clicked.connect(self._eff_del)
        eff_btns.addStretch(1); eff_btns.addWidget(self.btn_eff_add); eff_btns.addWidget(self.btn_eff_del)
        eff_row.addWidget(self.tbl_eff); eff_row.addLayout(eff_btns)

        form.addRow("ID", self.f_n_id)
        form.addRow("Name", self.f_n_name)
        form.addRow("Grid X", self.f_n_x)
        form.addRow("Grid Y (row)", self.f_n_y)
        form.addRow("AP Cost", self.f_n_ap)
        form.addRow(QLabel("Prerequisites (requires)"))
        form.addRow(req_row)
        form.addRow(QLabel("Effect (free-form JSON object)"))
        form.addRow(eff_row)

        # writeback/save node
        node_btns = QHBoxLayout()
        self.btn_apply = QPushButton("Apply Node Changes")
        self.btn_apply.clicked.connect(self._apply_node)
        node_btns.addStretch(1); node_btns.addWidget(self.btn_apply)
        right_l.addWidget(box)
        right_l.addLayout(node_btns)

        split.addWidget(right)
        split.setSizes([420, 860])

    # ---------- helpers ----------
    def _refresh_char_list(self):
        self.cmb_char.blockSignals(True)
        self.cmb_char.clear()
        for ch in self._char_order:
            self.cmb_char.addItem(ch)
        self.cmb_char.blockSignals(False)
        if self._char_order:
            self.cmb_char.setCurrentText(self._char_order[0])

    def _tree(self) -> Optional[Dict[str,Any]]:
        if not self._current_char: return None
        return self._trees.get(self._current_char)

    def _branch_nodes(self) -> List[Dict[str,Any]]:
        t = self._tree()
        if not t: return []
        b = self._current_branch
        if not b: return []
        return list(t.get("branches",{}).get(b, []))

    def _set_branch_nodes(self, nodes: List[Dict[str,Any]]):
        t = self._tree()
        if not t or not self._current_branch: return
        t.setdefault("branches", {})[self._current_branch] = nodes

    def _rebuild_edges(self, nodes: List[Dict[str,Any]]) -> List[Tuple[str,str]]:
        ids = {n.get("id","") for n in nodes}
        edges: List[Tuple[str,str]] = []
        for n in nodes:
            to_id = n.get("id","")
            for req in n.get("requires", []) or []:
                if req in ids:
                    edges.append((req, to_id))
        return edges

    def _populate_nodes_table(self):
        nodes = self._branch_nodes()
        self.tbl_nodes.setRowCount(len(nodes))
        for r, n in enumerate(nodes):
            def _set(c, text):
                it = QTableWidgetItem(str(text))
                it.setFlags(Qt.ItemIsSelectable|Qt.ItemIsEnabled)
                self.tbl_nodes.setItem(r, c, it)
            _set(0, n.get("id",""))
            _set(1, n.get("name",""))
            pos = n.get("pos",[0,0]); gx, gy = (int(pos[0]), int(pos[1])) if isinstance(pos,(list,tuple)) and len(pos)>=2 else (0,0)
            _set(2, gx); _set(3, gy)
            _set(4, n.get("cost_ap",0))
            _set(5, ", ".join(n.get("requires",[]) or []))
            eff = n.get("effect", {})
            _set(6, eff.get("type",""))
        self.tbl_nodes.resizeRowsToContents()

    def _load_node_into_inspector(self, nid: str):
        nodes = self._branch_nodes()
        cur = next((n for n in nodes if n.get("id")==nid), None)
        if not cur:
            return
        self._current_node_id = nid
        self.f_n_id.setText(cur.get("id",""))
        self.f_n_name.setText(cur.get("name",""))
        pos = cur.get("pos",[0,0])
        self.f_n_x.setValue(int(pos[0]) if isinstance(pos,(list,tuple)) and len(pos)>=2 else 0)
        self.f_n_y.setValue(int(pos[1]) if isinstance(pos,(list,tuple)) and len(pos)>=2 else 0)
        self.f_n_ap.setValue(int(cur.get("cost_ap",0)))
        # requires
        self.list_req.clear()
        for r in (cur.get("requires",[]) or []):
            QListWidgetItem(r, self.list_req)
        # effect K/V
        self.tbl_eff.setRowCount(0)
        eff = cur.get("effect", {}) or {}
        for k, v in eff.items():
            rr = self.tbl_eff.rowCount(); self.tbl_eff.insertRow(rr)
            self.tbl_eff.setItem(rr, 0, QTableWidgetItem(str(k)))
            self.tbl_eff.setItem(rr, 1, QTableWidgetItem(json.dumps(v) if not isinstance(v,(str,int,float,bool)) else str(v)))
        self.grid.set_selected(nid)

    def _apply_node(self):
        nid_before = self._current_node_id
        if not nid_before: return
        nodes = self._branch_nodes()
        i = next((i for i,n in enumerate(nodes) if n.get("id")==nid_before), -1)
        if i < 0: return
        n = dict(nodes[i])  # copy

        # gather
        new_id = self.f_n_id.text().strip()
        n["id"] = new_id or nid_before
        n["name"] = self.f_n_name.text().strip()
        n["pos"]  = [int(self.f_n_x.value()), int(self.f_n_y.value())]
        n["cost_ap"] = int(self.f_n_ap.value())
        # requires
        reqs = []
        for r in range(self.list_req.count()):
            reqs.append(self.list_req.item(r).text().strip())
        n["requires"] = reqs
        # effect
        eff: Dict[str,Any] = {}
        for r in range(self.tbl_eff.rowCount()):
            k = (self.tbl_eff.item(r,0).text() if self.tbl_eff.item(r,0) else "").strip()
            vtxt = (self.tbl_eff.item(r,1).text() if self.tbl_eff.item(r,1) else "").strip()
            if not k: continue
            # parse JSON if looks like JSON, else keep string/number/bool
            try:
                eff[k] = json.loads(vtxt)
            except Exception:
                # try to coerce to int/float/bool
                low = vtxt.lower()
                if low in ("true","false"): eff[k] = (low=="true")
                else:
                    try: eff[k] = int(vtxt)
                    except Exception:
                        try: eff[k] = float(vtxt)
                        except Exception:
                            eff[k] = vtxt
        n["effect"] = eff

        # writeback
        nodes[i] = n
        self._set_branch_nodes(nodes)
        self._current_node_id = n["id"]
        self._populate_nodes_table()
        self._load_node_into_inspector(n["id"])
        # refresh grid (nodes and edges)
        self.grid.set_nodes_and_edges(nodes, self._rebuild_edges(nodes))

    # ---------- UI handlers ----------
    def _on_char_changed(self, ch: str):
        self._current_char = ch or None
        self._current_branch = None
        self._current_node_id = None
        self._refresh_branches()
        self._populate_nodes_table()
        self.grid.set_nodes_and_edges([], [])

    def _refresh_branches(self):
        self.list_branches.blockSignals(True)
        self.list_branches.clear()
        t = self._tree()
        if not t: 
            self.list_branches.blockSignals(False); 
            return
        for b in t.get("branches", {}).keys():
            QListWidgetItem(b, self.list_branches)
        self.list_branches.blockSignals(False)
        if self.list_branches.count():
            self.list_branches.setCurrentRow(0)

    def _on_branch_changed(self, bname: str):
        self._current_branch = bname or None
        self._current_node_id = None
        nodes = self._branch_nodes()
        self._populate_nodes_table()
        self.grid.set_nodes_and_edges(nodes, self._rebuild_edges(nodes))

    def _on_table_select(self):
        rows = self.tbl_nodes.selectionModel().selectedRows()
        if not rows: return
        r = rows[0].row()
        nid = self.tbl_nodes.item(r,0).text()
        self._load_node_into_inspector(nid)

    def _grid_select(self, node_id: str):
        self._current_node_id = node_id
        # also select the table row
        for r in range(self.tbl_nodes.rowCount()):
            if self.tbl_nodes.item(r,0).text() == node_id:
                self.tbl_nodes.selectRow(r); break
        self._load_node_into_inspector(node_id)

    def _grid_move(self, node_id: str, gx: int, gy: int):
        # snap: gy must be >= 0
        if gy < 0: gy = 0
        nodes = self._branch_nodes()
        changed = False
        for n in nodes:
            if n.get("id")==node_id:
                n["pos"] = [gx, gy]
                changed = True
                break
        if changed:
            self._set_branch_nodes(nodes)
            self._populate_nodes_table()
            self.grid.set_nodes_and_edges(nodes, self._rebuild_edges(nodes))

    # ---- Branch mgmt ----
    def _add_branch(self):
        t = self._tree()
        if not t:
            return
        name, ok = QInputDialog.getText(self, "Add Branch", "Branch name:")
        if not ok: return
        name = str(name).strip()
        if not name:
            return
        branches = t.setdefault("branches", {})
        if name in branches:
            QMessageBox.warning(self, "Branch", "Name already exists.")
            return
        branches[name] = []
        self._refresh_branches()
        # set current to new
        items = self.list_branches.findItems(name, Qt.MatchExactly)
        if items:
            self.list_branches.setCurrentItem(items[0])

    def _ren_branch(self):
        t = self._tree()
        b = self._current_branch
        if not t or not b:
            return
        name, ok = QInputDialog.getText(self, "Rename Branch", "New name:", text=b)
        if not ok: return
        name = str(name).strip()
        if not name or name == b: return
        branches = t.get("branches", {})
        if name in branches:
            QMessageBox.warning(self, "Branch", "Name already exists.")
            return
        branches[name] = branches.pop(b)
        self._current_branch = name
        self._refresh_branches()

    def _del_branch(self):
        t = self._tree()
        b = self._current_branch
        if not t or not b:
            return
        if QMessageBox.question(self, "Delete Branch", f"Delete branch '{b}'?", QMessageBox.Yes|QMessageBox.No) != QMessageBox.Yes:
            return
        branches = t.get("branches", {})
        branches.pop(b, None)
        self._current_branch = None
        self._refresh_branches()
        self._populate_nodes_table()
        self.grid.set_nodes_and_edges([], [])

    # ---- Node mgmt ----
    def _new_node(self):
        if not self._current_branch:
            QMessageBox.warning(self, "Node", "Select a branch first.")
            return
        nodes = self._branch_nodes()
        used = {n.get("id","") for n in nodes}
        base, i = f"{self._current_char}_skill", 1
        nid = base
        while nid in used:
            i += 1; nid = f"{base}_{i}"
        n = {
            "id": nid, "name": "New Node",
            "pos": [1, 0], "cost_ap": 1,
            "requires": [],
            "effect": {"type": "buff", "buff_type": "attack", "value": 1}
        }
        nodes.append(n); self._set_branch_nodes(nodes)
        self._populate_nodes_table()
        self.grid.set_nodes_and_edges(nodes, self._rebuild_edges(nodes))
        self._load_node_into_inspector(nid)

    def _dup_node(self):
        nodes = self._branch_nodes()
        if not nodes or not self._current_node_id: return
        base = self._current_node_id
        used = {n.get("id","") for n in nodes}
        i, new_id = 1, f"{base}_copy"
        while new_id in used:
            i += 1; new_id = f"{base}_copy{i}"
        cur = next(n for n in nodes if n.get("id")==self._current_node_id)
        dup = json.loads(json.dumps(cur))
        dup["id"] = new_id
        nodes.append(dup); self._set_branch_nodes(nodes)
        self._populate_nodes_table()
        self.grid.set_nodes_and_edges(nodes, self._rebuild_edges(nodes))
        self._load_node_into_inspector(new_id)

    def _del_node(self):
        nodes = self._branch_nodes()
        if not nodes or not self._current_node_id: return
        if QMessageBox.question(self, "Delete Node", f"Delete '{self._current_node_id}'?", QMessageBox.Yes|QMessageBox.No) != QMessageBox.Yes:
            return
        nodes = [n for n in nodes if n.get("id") != self._current_node_id]
        self._set_branch_nodes(nodes)
        self._current_node_id = None
        self._populate_nodes_table()
        self.grid.set_nodes_and_edges(nodes, self._rebuild_edges(nodes))

    # ---- requires editor ----
    def _req_add(self):
        # Build a pick list of all node IDs across this character's tree
        t = self._tree()
        if not t: return
        all_ids = []
        for blist in t.get("branches",{}).values():
            for n in blist:
                nid = n.get("id","")
                if nid: all_ids.append(nid)
        all_ids = sorted(set(all_ids))
        # Dialog with filterable list + "Custom…" option
        choices = ["<Custom…>"] + all_ids
        item, ok = QInputDialog.getItem(self, "Add prerequisite", "Pick a node (or choose Custom… to type):", choices, 0, False)
        if not ok: return
        if item == "<Custom…>":
            txt, ok2 = QInputDialog.getText(self, "Custom prerequisite", "Enter ID (node or milestone id):")
            if not ok2: return
            item = str(txt).strip()
            if not item: return
        # Add to list
        QListWidgetItem(item, self.list_req)

    def _req_del(self):
        row = self.list_req.currentRow()
        if row >= 0:
            self.list_req.takeItem(row)

    # ---- effect editor ----
    def _eff_add(self):
        r = self.tbl_eff.rowCount()
        self.tbl_eff.insertRow(r)
        self.tbl_eff.setItem(r,0,QTableWidgetItem(""))
        self.tbl_eff.setItem(r,1,QTableWidgetItem(""))

    def _eff_del(self):
        rows = self.tbl_eff.selectionModel().selectedRows()
        for m in sorted((r.row() for r in rows), reverse=True):
            self.tbl_eff.removeRow(m)

    # ---- tree file mgmt ----
    def save(self) -> bool:
        ch = self._current_char
        if not ch:
            QMessageBox.information(self, "Skill Trees", "No character selected.")
            return False
        t = self._tree() or {"character": ch, "branches": {}}
        t["character"] = ch
        # clean: ensure arrays and fields well-formed
        for bname, blist in list(t.get("branches", {}).items()):
            cleaned = []
            for n in blist:
                if not n.get("id"): continue
                n.setdefault("name", n["id"])
                pos = n.get("pos",[0,0])
                try:
                    gx = int(pos[0]); gy = int(pos[1])
                except Exception:
                    gx, gy = 0, 0
                n["pos"] = [gx, max(0, gy)]
                n["cost_ap"] = int(n.get("cost_ap",1))
                reqs = [str(r).strip() for r in (n.get("requires",[]) or []) if str(r).strip()]
                n["requires"] = reqs
                n["effect"] = n.get("effect", {}) or {}
                cleaned.append(n)
            t["branches"][bname] = cleaned

        path = self.dir_trees / f"{ch}.json"
        if _write_json(path, t, title=f"skill tree ({ch})"):
            QMessageBox.information(self, "Skill Trees", f"Saved {path.name}.")
            # reload to keep memory aligned with on-disk
            self._load_all_trees()
            self._refresh_char_list()
            self.cmb_char.setCurrentText(ch)
            return True
        return False

    def validate(self) -> List[str]:
        issues: List[str] = []
        t = self._tree()
        if not t:
            return ["No character/tree loaded."]
        ch = t.get("character")
        if not ch:
            issues.append("Tree missing 'character'.")
        branches = t.get("branches", {})
        if not isinstance(branches, dict) or not branches:
            issues.append("No branches found.")
            return issues

        # duplicate id checks (per-character tree)
        all_ids: Set[str] = set()
        for bname, blist in branches.items():
            for n in blist:
                nid = (n.get("id") or "").strip()
                if not nid: issues.append(f"[{bname}] node with missing id")
                if nid in all_ids: issues.append(f"Duplicate node id: {nid}")
                all_ids.add(nid)
                # pos
                pos = n.get("pos",[0,0])
                if not (isinstance(pos,(list,tuple)) and len(pos)>=2):
                    issues.append(f"{nid} has invalid pos")
                else:
                    try:
                        gx, gy = int(pos[0]), int(pos[1])
                        if gy < 0: issues.append(f"{nid} has negative row (y)")
                    except Exception:
                        issues.append(f"{nid} pos not ints")
                # cost_ap
                try:
                    ap = int(n.get("cost_ap",1))
                    if ap < 0: issues.append(f"{nid} cost_ap must be >= 0")
                except Exception:
                    issues.append(f"{nid} cost_ap invalid")
                # requires dangling?
                for r in (n.get("requires",[]) or []):
                    if r and (r not in all_ids):
                        # may be milestone; warn but do not error hard
                        issues.append(f"{nid} requires '{r}' not found in this branch/tree (ok if milestone).")
                # effect presence
                if "effect" not in n:
                    issues.append(f"{nid} missing effect")
        return issues

    def _on_validate(self):
        issues = self.validate()
        if issues:
            QMessageBox.warning(self, "Skill Trees", "\n".join(issues[:200]))
        else:
            QMessageBox.information(self, "Skill Trees", "No issues found.")

    # ---- new/dup/del trees ----
    def _new_tree(self):
        ch, ok = QInputDialog.getText(self, "New Skill Tree", "Character id (e.g., nova):")
        if not ok: return
        ch = str(ch).strip()
        if not ch: return
        if ch in self._trees:
            QMessageBox.warning(self, "Tree", "A tree for this character already exists.")
            return
        self._trees[ch] = {"character": ch, "branches": {}}
        self._char_order.append(ch)
        self._refresh_char_list()
        self.cmb_char.setCurrentText(ch)

    def _dup_tree(self):
        src = self._current_char
        if not src: return
        dst, ok = QInputDialog.getText(self, "Duplicate Tree", "New character id:")
        if not ok: return
        dst = str(dst).strip()
        if not dst or dst == src: return
        if dst in self._trees:
            QMessageBox.warning(self, "Tree", "Destination character already exists.")
            return
        self._trees[dst] = json.loads(json.dumps(self._tree()))
        self._trees[dst]["character"] = dst
        self._char_order.append(dst)
        self._refresh_char_list()
        self.cmb_char.setCurrentText(dst)

    def _del_tree(self):
        ch = self._current_char
        if not ch: return
        if QMessageBox.question(self, "Delete Tree", f"Delete on disk file {ch}.json?", QMessageBox.Yes|QMessageBox.No) != QMessageBox.Yes:
            return
        path = self.dir_trees / f"{ch}.json"
        try:
            if path.exists(): path.unlink()
        except Exception as e:
            QMessageBox.critical(self, "Delete", f"Failed to delete {path.name}:\n{e}")
            return
        self._trees.pop(ch, None)
        self._char_order = [x for x in self._char_order if x != ch]
        self._refresh_char_list()

# ============================================================
# App wrapper
# ============================================================
class SkillsEditor(QWidget):
    """Hosts both tabs and exposes save/validate/reload_lookups for Studio toolbar."""
    def __init__(self, project_root: Optional[str] = None):
        super().__init__()
        self.setWindowTitle("Starborn — Skills")
        self.root = _find_root(Path(project_root) if project_root else None)

        tabs = QTabWidget(self)
        root_layout = QVBoxLayout(self); root_layout.addWidget(tabs)

        self.tab_skills = SkillsTab(self.root)
        tabs.addTab(self.tab_skills, "Skills")

        self.tab_trees = TreesTab(self.root)
        tabs.addTab(self.tab_trees, "Skill Trees")

    # For Studio adapter compatibility:
    def save(self) -> bool:
        ok1 = self.tab_skills.save()
        ok2 = self.tab_trees.save()
        return bool(ok1 and ok2)

    def validate(self) -> List[str]:
        return self.tab_skills.validate() + self.tab_trees.validate()

    def reload_lookups(self):
        self.tab_trees.reload_lookups()


if __name__ == "__main__":
    import sys
    app = QApplication(sys.argv)
    w = SkillsEditor()
    w.resize(1200, 720)
    w.show()
    sys.exit(app.exec_())
