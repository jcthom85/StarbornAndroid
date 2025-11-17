#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Starborn — Encounter Editor (multi-wave + structured conditions)

Works from /tools or root. Auto-finds project root containing encounters.json or rooms.json.
Features:
- Basics tab: id, type, room, player starts, first-enter-only, pre/post cinematics.
- Combat semantics: initiative_bonus, preempt_damage_pct, start_status.
- Repeatability/state: max_repeats, once_per_save, respawn_cooldown_sec, encounter_state_id.
- Spoils & modifiers: spoils_profile, enemy_hp_pct, enemy_atk_pct, enemy_speed_pct.
- Waves tab: true multi-wave editing with per-wave delay_ms and spawn_effect; members table (enemy id + qty) with autocompletion.
- Conditions tab: text DSL ("milestone:met_mechanic", "!item:wrench") + structured parse to objects.
- Pickers: room ids (rooms.json), enemy ids (enemies.json), cinematics ids (cinematics.json keys).
- Validate: unknown ids, empty waves, qty>0, numeric ranges, etc.
- Saves sorted, with .bak backup. Exposes save() and validate() for Studio.
"""

from __future__ import annotations
import os, sys, json, shutil
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (
    QApplication, QWidget, QSplitter, QListWidget, QListWidgetItem, QLineEdit, QTextEdit,
    QVBoxLayout, QHBoxLayout, QFormLayout, QLabel, QPushButton, QComboBox, QSpinBox,
    QMessageBox, QCheckBox, QTabWidget, QGroupBox, QTableWidget, QTableWidgetItem,
    QAbstractItemView, QListWidget as QtListWidget, QListWidgetItem as QtListItem, QStyledItemDelegate
)
from PyQt5.QtWidgets import QInputDialog, QToolButton
from PyQt5.QtGui import QIcon
from PyQt5.QtCore import QSize
from theme_kit import ThemeManager         # optional if you want per-editor theme flips
from data_core import detect_project_root, json_load, json_save, unique_id
from editor_undo import UndoManager
from ui_common import attach_status_bar, flash_status, attach_hotkeys, attach_list_context_menu, mark_invalid, clear_invalid
from editor_bus import goto as studio_goto, refresh_references as studio_refresh

ALLOWED_TYPES = ["single","party","wave","patrol","ambush"]

# -----------------------------
#  Project root resolution
# -----------------------------
def find_project_root(start: Path, targets=("encounters.json", "rooms.json"), max_up: int = 5) -> Path:
    cur = start.resolve()
    for _ in range(max_up + 1):
        for t in targets:
            if (cur / t).exists():
                return cur
        cur = cur.parent
    return start.resolve()

# -----------------------------
#  File helpers
# -----------------------------
def _load_json(path: Path):
    try:
        with path.open("r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return None

def _load_json_list(path: Path) -> list:
    data = _load_json(path)
    return data if isinstance(data, list) else []

def _load_json_dict(path: Path) -> dict:
    data = _load_json(path)
    return data if isinstance(data, dict) else {}

def _safe_backup_write(path: Path, payload: Any):
    if path.exists():
        shutil.copy2(path, path.with_suffix(path.suffix + ".bak"))
    with path.open("w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=4)

# -----------------------------
#  Lookups
# -----------------------------
def collect_room_ids(root: Path) -> List[str]:
    rooms = _load_json_list(root / "rooms.json")
    out = set()
    for r in rooms:
        rid = (r.get("id") or r.get("name") or "").strip()
        if rid:
            out.add(rid)
    return sorted(out, key=str.lower)

def collect_enemy_ids(root: Path) -> List[str]:
    enemies = _load_json_list(root / "enemies.json")
    out = set()
    for e in enemies:
        eid = (e.get("id") or e.get("name") or "").strip()
        if eid:
            out.add(eid)
    return sorted(out, key=str.lower)

def collect_cinematic_ids(root: Path) -> List[str]:
    data = _load_json_dict(root / "cinematics.json")
    return sorted(data.keys(), key=str.lower)

# -----------------------------
#  Conditions parse/format
#   DSL lines like:
#     milestone:met_mechanic
#     !item:wrench
#     item:distress_beacon
#     flag:some_flag
# -----------------------------
def parse_conditions_text(text: str) -> Tuple[List[dict], List[str]]:
    conds: List[dict] = []
    errs: List[str] = []
    lines = [ln.strip() for ln in (text or "").splitlines()]
    for ln in lines:
        if not ln or ln.startswith("#"):
            continue
        neg = ln.startswith("!")
        body = ln[1:].strip() if neg else ln
        if ":" in body:
            ctype, cid = body.split(":", 1)
            ctype = ctype.strip().lower()
            cid = cid.strip()
        else:
            # allow bare flag name: interpreted as flag
            ctype, cid = "flag", body.strip()
        if not cid:
            errs.append(f"Bad condition (missing id): '{ln}'")
            continue
        if ctype not in ("milestone","item","flag"):
            # we keep it, but tag as 'expr'
            conds.append({"type":"expr","expr":ln})
        else:
            conds.append({"type":ctype, "id":cid, "not":bool(neg)})
    return conds, errs

def conditions_to_text(conds: List[dict]) -> str:
    out: List[str] = []
    for c in conds or []:
        if c.get("type") == "expr":
            out.append(c.get("expr",""))
        else:
            prefix = "!" if c.get("not") else ""
            out.append(f"{prefix}{c.get('type')}:{c.get('id')}")
    return "\n".join(out)

# -----------------------------
#  Table delegate for completer
# -----------------------------
from PyQt5.QtWidgets import QLineEdit as QtLineEdit, QCompleter, QWidget as QtWidget
class EnemyIDDelegate(QStyledItemDelegate):
    def __init__(self, enemy_ids: List[str], parent=None):
        super().__init__(parent)
        self.enemy_ids = enemy_ids
        self._completer = QCompleter(self.enemy_ids)
        self._completer.setCaseSensitivity(Qt.CaseInsensitive)

    def createEditor(self, parent, option, index):
        editor = QtLineEdit(parent)
        editor.setCompleter(self._completer)
        return editor

# -----------------------------
#  Encounter Editor
# -----------------------------
class EncounterEditor(QWidget):
    def __init__(self, start_dir: Optional[Path]=None):
        super().__init__()
        self.setWindowTitle("Starborn Encounter Editor")

        if start_dir is None:
            start_dir = Path(__file__).parent
        self.start_dir = Path(start_dir)
        self.root: Path = find_project_root(self.start_dir)

        # data
        self.encounters: Dict[str, dict] = {}
        self.current_id: Optional[str] = None

        # lookups
        self.room_ids: List[str] = collect_room_ids(self.root)
        self.enemy_ids: List[str] = collect_enemy_ids(self.root)
        self.cinematic_ids: List[str] = collect_cinematic_ids(self.root)

        # ui refs
        self.search_box: QLineEdit = None  # type: ignore
        self.list_widget: QListWidget = None  # type: ignore

        # Basics widgets
        self.id_label: QLabel = None  # type: ignore
        self.type_combo: QComboBox = None  # type: ignore
        self.room_combo: QComboBox = None  # type: ignore
        self.chk_player_starts: QCheckBox = None  # type: ignore
        self.chk_first_enter_only: QCheckBox = None  # type: ignore
        self.pre_cine_combo: QComboBox = None  # type: ignore
        self.post_cine_combo: QComboBox = None  # type: ignore
        self.init_bonus_spin: QSpinBox = None  # type: ignore
        self.preempt_pct_spin: QSpinBox = None  # type: ignore
        self.start_status_edit: QLineEdit = None  # type: ignore
        self.max_repeats_spin: QSpinBox = None  # type: ignore
        self.once_per_save_chk: QCheckBox = None  # type: ignore
        self.respawn_sec_spin: QSpinBox = None  # type: ignore
        self.state_id_edit: QLineEdit = None  # type: ignore
        self.spoils_edit: QLineEdit = None  # type: ignore
        self.hp_pct_spin: QSpinBox = None  # type: ignore
        self.atk_pct_spin: QSpinBox = None  # type: ignore
        self.spd_pct_spin: QSpinBox = None  # type: ignore

        # Waves widgets
        self.wave_list: QtListWidget = None  # type: ignore
        self.wave_delay_spin: QSpinBox = None  # type: ignore
        self.wave_spawn_edit: QLineEdit = None  # type: ignore
        self.members_table: QTableWidget = None  # type: ignore

        # Conditions widgets
        self.cond_text: QTextEdit = None  # type: ignore
        self.cond_preview: QtListWidget = None  # type: ignore

        self.loadEncounters()
        self.initUI()
        self.refreshEncounterList()

    # ------------- IO -----------------
    def encounters_path(self) -> Path:
        return self.root / "encounters.json"

    def loadEncounters(self):
        self.encounters.clear()
        data = _load_json(self.encounters_path())
        if data is None:
            # start empty
            return
        if isinstance(data, list):
            for e in data:
                eid = (e.get("id") or "").strip()
                if not eid:
                    continue
                self.encounters[eid] = e
        elif isinstance(data, dict):
            for k, v in data.items():
                v = dict(v)
                v["id"] = v.get("id") or k
                self.encounters[v["id"]] = v

    def saveEncounters(self):
        out = sorted(self.encounters.values(), key=lambda e: (e.get("id") or "").lower())
        _safe_backup_write(self.encounters_path(), out)
        QMessageBox.information(self, "Saved", "encounters.json saved successfully.")

    # Studio-friendly
    def save(self):
        self.onSave()

    # ------------- UI -----------------
    def initUI(self):
        split = QSplitter(Qt.Horizontal, self)

        # Left panel: search + list + buttons
        left = QWidget()
        left_layout = QVBoxLayout(left)

        self.search_box = QLineEdit()
        self.search_box.setPlaceholderText("Search by ID or Room…")
        self.search_box.textChanged.connect(self.refreshEncounterList)
        left_layout.addWidget(self.search_box)

        self.list_widget = QListWidget()
        self.list_widget.itemSelectionChanged.connect(self._on_select_changed)
        left_layout.addWidget(self.list_widget, 1)

        row = QHBoxLayout()
        btn_add = QPushButton("Add")
        btn_dup = QPushButton("Duplicate")
        btn_del = QPushButton("Delete")
        btn_add.clicked.connect(self.onAdd)
        btn_dup.clicked.connect(self.onDuplicate)
        btn_del.clicked.connect(self.onDelete)
        row.addWidget(btn_add); row.addWidget(btn_dup); row.addWidget(btn_del)
        left_layout.addLayout(row)

        split.addWidget(left)

        # Right panel: tabs
        tabs = QTabWidget()
        tabs.addTab(self._build_tab_basics(), "Basics")
        tabs.addTab(self._build_tab_waves(), "Waves")
        tabs.addTab(self._build_tab_conditions(), "Conditions")

        # bottom row buttons
        bottom = QWidget()
        bottom_layout = QHBoxLayout(bottom)
        btn_save = QPushButton("Save")
        btn_reload = QPushButton("Reload")
        btn_validate = QPushButton("Validate")
        btn_save.clicked.connect(self.onSave)
        btn_reload.clicked.connect(self.onReload)
        btn_validate.clicked.connect(self.onValidate)
        bottom_layout.addWidget(btn_save); bottom_layout.addWidget(btn_reload); bottom_layout.addWidget(btn_validate)

        right = QWidget()
        right_layout = QVBoxLayout(right)
        right_layout.addWidget(tabs, 1)
        right_layout.addWidget(bottom, 0)

        split.addWidget(right)
        split.setStretchFactor(0, 1)
        split.setStretchFactor(1, 3)

        lay = QHBoxLayout(self)
        lay.addWidget(split)
        self.setLayout(lay)

    def _build_tab_basics(self) -> QWidget:
        w = QWidget()
        form = QFormLayout(w)

        self.id_label = QLabel("-")
        form.addRow("ID:", self.id_label)

        self.type_combo = QComboBox(); self.type_combo.addItems(ALLOWED_TYPES)
        form.addRow("Type:", self.type_combo)

        self.room_combo = QComboBox(); self.room_combo.setEditable(True)
        self.room_combo.addItems([""] + self.room_ids)
        form.addRow("Room:", self.room_combo)

        self.chk_player_starts = QCheckBox("Player initiates (preemptive)")
        form.addRow(self.chk_player_starts)

        self.chk_first_enter_only = QCheckBox("Trigger only on first enter")
        form.addRow(self.chk_first_enter_only)

        # Cinematics
        self.pre_cine_combo = QComboBox(); self.pre_cine_combo.setEditable(True)
        self.pre_cine_combo.addItems([""] + self.cinematic_ids)
        form.addRow("Pre cinematic:", self.pre_cine_combo)

        self.post_cine_combo = QComboBox(); self.post_cine_combo.setEditable(True)
        self.post_cine_combo.addItems([""] + self.cinematic_ids)
        form.addRow("Post cinematic:", self.post_cine_combo)

        # Combat semantics
        self.init_bonus_spin = QSpinBox(); self.init_bonus_spin.setRange(-100, 100)
        form.addRow("Initiative bonus (enemy side):", self.init_bonus_spin)

        self.preempt_pct_spin = QSpinBox(); self.preempt_pct_spin.setRange(0,100)
        form.addRow("Preempt damage % to player:", self.preempt_pct_spin)

        self.start_status_edit = QLineEdit()
        self.start_status_edit.setPlaceholderText("e.g., staggered, stunned (optional)")
        form.addRow("Start status on player:", self.start_status_edit)

        # Repeatability/state
        self.max_repeats_spin = QSpinBox(); self.max_repeats_spin.setRange(0, 999)
        form.addRow("Max repeats (0=unlimited):", self.max_repeats_spin)

        self.once_per_save_chk = QCheckBox("Once per save")
        form.addRow(self.once_per_save_chk)

        self.respawn_sec_spin = QSpinBox(); self.respawn_sec_spin.setRange(0, 86400)
        form.addRow("Respawn cooldown (sec):", self.respawn_sec_spin)

        self.state_id_edit = QLineEdit()
        form.addRow("Encounter state id:", self.state_id_edit)

        # Spoils / modifiers
        self.spoils_edit = QLineEdit()
        form.addRow("Spoils profile:", self.spoils_edit)

        self.hp_pct_spin = QSpinBox(); self.hp_pct_spin.setRange(10, 1000); self.hp_pct_spin.setValue(100)
        form.addRow("Enemy HP %:", self.hp_pct_spin)

        self.atk_pct_spin = QSpinBox(); self.atk_pct_spin.setRange(10, 1000); self.atk_pct_spin.setValue(100)
        form.addRow("Enemy ATK %:", self.atk_pct_spin)

        self.spd_pct_spin = QSpinBox(); self.spd_pct_spin.setRange(10, 1000); self.spd_pct_spin.setValue(100)
        form.addRow("Enemy SPEED %:", self.spd_pct_spin)

        return w

    def _build_tab_waves(self) -> QWidget:
        w = QWidget()
        hl = QHBoxLayout(w)

        # Left: wave list and controls
        left = QWidget(); ll = QVBoxLayout(left)
        self.wave_list = QtListWidget()
        self.wave_list.itemSelectionChanged.connect(self._on_wave_selected)
        ll.addWidget(self.wave_list, 1)

        ctrl = QHBoxLayout()
        btn_add = QPushButton("+ Wave"); btn_add.clicked.connect(self.onAddWave)
        btn_rem = QPushButton("− Wave"); btn_rem.clicked.connect(self.onRemoveWave)
        btn_up  = QPushButton("↑"); btn_up.clicked.connect(self.onMoveWaveUp)
        btn_dn  = QPushButton("↓"); btn_dn.clicked.connect(self.onMoveWaveDown)
        ctrl.addWidget(btn_add); ctrl.addWidget(btn_rem); ctrl.addWidget(btn_up); ctrl.addWidget(btn_dn)
        ll.addLayout(ctrl)
        hl.addWidget(left, 1)

        # Right: wave editor (options + members table)
        right = QWidget(); rv = QVBoxLayout(right)

        wf = QFormLayout()
        self.wave_delay_spin = QSpinBox(); self.wave_delay_spin.setRange(0, 600000)
        wf.addRow("Delay before spawn (ms):", self.wave_delay_spin)
        self.wave_spawn_edit = QLineEdit(); self.wave_spawn_edit.setPlaceholderText("spawn_effect id (optional)")
        wf.addRow("Spawn effect:", self.wave_spawn_edit)
        right.setLayout(rv)
        rv.addLayout(wf)

        # members table
        self.members_table = QTableWidget(0, 2)
        self.members_table.setHorizontalHeaderLabels(["Enemy ID", "Qty"])
        self.members_table.horizontalHeader().setStretchLastSection(True)
        self.members_table.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.members_table.setSelectionMode(QAbstractItemView.SingleSelection)
        self.members_table.setItemDelegateForColumn(0, EnemyIDDelegate(self.enemy_ids, self.members_table))
        rv.addWidget(self.members_table, 1)

        mctrl = QHBoxLayout()
        m_add = QPushButton("+ Member"); m_add.clicked.connect(self.onAddMember)
        m_rem = QPushButton("− Member"); m_rem.clicked.connect(self.onRemoveMember)
        mctrl.addWidget(m_add); mctrl.addWidget(m_rem)
        rv.addLayout(mctrl)

        hl.addWidget(right, 2)
        return w

    def _build_tab_conditions(self) -> QWidget:
        w = QWidget()
        v = QVBoxLayout(w)

        self.cond_text = QTextEdit()
        self.cond_text.setPlaceholderText("One condition per line, e.g.\nmilestone:met_mechanic\n!item:wrench")
        self.cond_text.textChanged.connect(self._on_conditions_changed)
        v.addWidget(self.cond_text, 2)

        self.cond_preview = QtListWidget()
        v.addWidget(self.cond_preview, 1)

        # Quick add row
        row = QHBoxLayout()
        for label, tmpl in [
            ("+ Milestone", "milestone:"),
            ("+ Not Milestone", "!milestone:"),
            ("+ Item", "item:"),
            ("+ Not Item", "!item:"),
            ("+ Flag", "flag:")
        ]:
            btn = QPushButton(label)
            btn.clicked.connect(lambda _=None, t=tmpl: self._append_condition_template(t))
            row.addWidget(btn)
        v.addLayout(row)

        return w

    def _append_condition_template(self, template: str):
        cur = self.cond_text.toPlainText()
        cur = (cur + ("\n" if cur and not cur.endswith("\n") else "") + template)
        self.cond_text.setPlainText(cur)
        self.cond_text.moveCursor(self.cond_text.textCursor().End)

    # ------------- List refresh / selection -------------
    def refreshEncounterList(self):
        sel = self.current_id
        self.list_widget.clear()
        q = (self.search_box.text() or "").strip().lower()
        for eid in sorted(self.encounters.keys(), key=str.lower):
            e = self.encounters[eid]
            room = (e.get("room") or e.get("room_id") or "")
            text = eid
            if room:
                text += f"  ·  {room}"
            if q and (q not in eid.lower()) and (q not in str(room).lower()):
                continue
            item = QListWidgetItem(text)
            item.setData(Qt.UserRole, eid)
            self.list_widget.addItem(item)
            if eid == sel:
                item.setSelected(True)

        if sel and sel in self.encounters:
            self._load_into_form(sel)
        elif self.list_widget.count() and not self.list_widget.selectedItems():
            self.list_widget.setCurrentRow(0)
        else:
            self._clear_form()

    def _on_select_changed(self):
        items = self.list_widget.selectedItems()
        if not items:
            self.current_id = None
            self._clear_form()
            return
        eid = items[0].data(Qt.UserRole)
        self.current_id = eid
        self._load_into_form(eid)

    # ----------------- Form load/clear -------------------
    def _clear_form(self):
        # basics
        self.id_label.setText("-")
        self.type_combo.setCurrentText("single")
        self.room_combo.setCurrentText("")
        self.chk_player_starts.setChecked(True)
        self.chk_first_enter_only.setChecked(False)
        self.pre_cine_combo.setCurrentText("")
        self.post_cine_combo.setCurrentText("")
        self.init_bonus_spin.setValue(0)
        self.preempt_pct_spin.setValue(0)
        self.start_status_edit.setText("")
        self.max_repeats_spin.setValue(0)
        self.once_per_save_chk.setChecked(False)
        self.respawn_sec_spin.setValue(0)
        self.state_id_edit.setText("")
        self.spoils_edit.setText("")
        self.hp_pct_spin.setValue(100)
        self.atk_pct_spin.setValue(100)
        self.spd_pct_spin.setValue(100)

        # waves
        self.wave_list.clear()
        self.members_table.setRowCount(0)
        self.wave_delay_spin.setValue(0)
        self.wave_spawn_edit.setText("")

        # conditions
        self.cond_text.setPlainText("")
        self.cond_preview.clear()

    def _load_into_form(self, eid: str):
        e = dict(self.encounters.get(eid, {}))

        # basics
        self.id_label.setText(eid)
        self.type_combo.setCurrentText(e.get("type","single") or "single")
        self.room_combo.setCurrentText(e.get("room") or e.get("room_id") or "")

        self.chk_player_starts.setChecked(bool(e.get("player_starts") or e.get("player_initiates") or False))
        self.chk_first_enter_only.setChecked(bool(e.get("first_enter_only", False)))

        self.pre_cine_combo.setCurrentText(e.get("pre_cinematic","") or "")
        self.post_cine_combo.setCurrentText(e.get("post_cinematic","") or "")

        self.init_bonus_spin.setValue(int(e.get("initiative_bonus", 0) or 0))
        self.preempt_pct_spin.setValue(int(e.get("preempt_damage_pct", 0) or 0))
        self.start_status_edit.setText(e.get("start_status","") or "")

        self.max_repeats_spin.setValue(int(e.get("max_repeats", 0) or 0))
        self.once_per_save_chk.setChecked(bool(e.get("once_per_save", False)))
        self.respawn_sec_spin.setValue(int(e.get("respawn_cooldown_sec", 0) or 0))
        self.state_id_edit.setText(e.get("encounter_state_id","") or "")

        self.spoils_edit.setText(e.get("spoils_profile","") or "")
        mods = e.get("modifiers", {}) or {}
        self.hp_pct_spin.setValue(int(mods.get("enemy_hp_pct", 100) or 100))
        self.atk_pct_spin.setValue(int(mods.get("enemy_atk_pct", 100) or 100))
        self.spd_pct_spin.setValue(int(mods.get("enemy_speed_pct", 100) or 100))

        # waves (support legacy 'members' -> single wave)
        waves = e.get("waves")
        if not isinstance(waves, list):
            members = e.get("members", [])
            if isinstance(members, list) and members:
                waves = [{"members": members}]
            else:
                waves = [{"members": []}]
        self._current_waves = waves  # keep in memory until save

        self.wave_list.clear()
        for idx, _w in enumerate(waves, start=1):
            self.wave_list.addItem(QtListItem(f"Wave {idx}"))
        if self.wave_list.count():
            self.wave_list.setCurrentRow(0)
        else:
            self._show_wave(None)

        # conditions
        if "conditions_text" in e:
            text = e.get("conditions_text") or ""
            conds, errs = parse_conditions_text(text)
        else:
            conds = e.get("conditions") or []
            text = conditions_to_text(conds)
        self._current_conditions = conds
        self.cond_text.setPlainText(text)
        self._render_conditions_preview()

    # ------------- Waves helpers -------------
    def _active_wave_index(self) -> Optional[int]:
        sel = self.wave_list.selectedIndexes()
        if not sel:
            return None
        return sel[0].row()

    def _show_wave(self, idx: Optional[int]):
        if idx is None or idx < 0 or idx >= len(self._current_waves):
            # clear
            self.wave_delay_spin.setValue(0)
            self.wave_spawn_edit.setText("")
            self.members_table.setRowCount(0)
            return
        w = self._current_waves[idx] or {}
        self.wave_delay_spin.setValue(int(w.get("delay_ms",0) or 0))
        self.wave_spawn_edit.setText(w.get("spawn_effect","") or "")

        # Connect signals to save changes when they are made
        self.wave_delay_spin.valueChanged.connect(self._save_active_wave_details)
        self.wave_spawn_edit.editingFinished.connect(self._save_active_wave_details)

        members = w.get("members", [])
        self.members_table.setRowCount(0)
        for m in members:
            self._append_member_row(m.get("id",""), int(m.get("qty",1) or 1))

    def _append_member_row(self, enemy_id: str, qty: int):
        r = self.members_table.rowCount()
        self.members_table.insertRow(r)
        it_id = QTableWidgetItem(enemy_id)
        it_qty = QTableWidgetItem(str(qty))
        self.members_table.setItem(r, 0, it_id)
        self.members_table.setItem(r, 1, it_qty)

    def _read_members_from_table(self) -> List[dict]:
        out = []
        for r in range(self.members_table.rowCount()):
            mid = self.members_table.item(r,0)
            qit = self.members_table.item(r,1)
            eid = (mid.text().strip() if mid else "")
            try:
                qty = int(qit.text()) if qit and qit.text() else 1
            except Exception:
                qty = 1
            if eid:
                out.append({"id": eid, "qty": max(1, qty)})
        return out

    def _save_active_wave_details(self):
        """Saves the delay and spawn effect of the currently selected wave."""
        idx = self._active_wave_index()
        if idx is None or not hasattr(self, "_current_waves") or not (0 <= idx < len(self._current_waves)):
            return
        
        wave_data = self._current_waves[idx]
        wave_data["delay_ms"] = int(self.wave_delay_spin.value())
        wave_data["spawn_effect"] = self.wave_spawn_edit.text().strip()

    # ------------- Waves actions -------------
    def _on_wave_selected(self):
        idx = self._active_wave_index()
        self._show_wave(idx)

    def onAddWave(self):
        if not hasattr(self, "_current_waves"):
            self._current_waves = []
        self._current_waves.append({"delay_ms":0,"spawn_effect":"","members":[]})
        self.wave_list.addItem(QtListItem(f"Wave {len(self._current_waves)}"))
        self.wave_list.setCurrentRow(self.wave_list.count()-1)

    def onRemoveWave(self):
        idx = self._active_wave_index()
        if idx is None:
            return
        self._current_waves.pop(idx)
        self.wave_list.takeItem(idx)
        # re-label
        for i in range(self.wave_list.count()):
            self.wave_list.item(i).setText(f"Wave {i+1}")
        self.wave_list.setCurrentRow(min(idx, self.wave_list.count()-1))

    def onMoveWaveUp(self):
        idx = self._active_wave_index()
        if idx is None or idx <= 0:
            return
        self._current_waves[idx-1], self._current_waves[idx] = self._current_waves[idx], self._current_waves[idx-1]
        self.refresh_wave_labels_and_select(idx-1)

    def onMoveWaveDown(self):
        idx = self._active_wave_index()
        if idx is None or idx >= len(self._current_waves)-1:
            return
        self._current_waves[idx+1], self._current_waves[idx] = self._current_waves[idx], self._current_waves[idx+1]
        self.refresh_wave_labels_and_select(idx+1)

    def refresh_wave_labels_and_select(self, sel_idx: int):
        for i in range(self.wave_list.count()):
            self.wave_list.item(i).setText(f"Wave {i+1}")
        self.wave_list.setCurrentRow(sel_idx)

    def onAddMember(self):
        self.members_table.insertRow(self.members_table.rowCount())
        self.members_table.setItem(self.members_table.rowCount()-1, 0, QTableWidgetItem(""))
        self.members_table.setItem(self.members_table.rowCount()-1, 1, QTableWidgetItem("1"))

    def onRemoveMember(self):
        r = self.members_table.currentRow()
        if r >= 0:
            self.members_table.removeRow(r)

    # ------------- Conditions -------------
    def _on_conditions_changed(self):
        text = self.cond_text.toPlainText()
        conds, errs = parse_conditions_text(text)
        self._current_conditions = conds
        self._render_conditions_preview(errs)

    def _render_conditions_preview(self, errs: Optional[List[str]]=None):
        self.cond_preview.clear()
        for c in self._current_conditions or []:
            if c.get("type") == "expr":
                s = f"EXPR: {c.get('expr')}"
            else:
                neg = "NOT " if c.get("not") else ""
                s = f"{neg}{c.get('type').upper()}: {c.get('id')}"
            self.cond_preview.addItem(QtListItem(s))
        if errs:
            for e in errs:
                it = QtListItem(f"ERROR: {e}")
                self.cond_preview.addItem(it)

    # ------------- Add/Duplicate/Delete -------------
    def onAdd(self):
        new_id, ok = QInputDialog.getText(self, "New Encounter", "Enter encounter ID:")
        if not ok or not new_id:
            return
        new_id = new_id.strip()
        if not new_id:
            return
        if new_id in self.encounters:
            QMessageBox.warning(self, "Add Encounter", "ID already exists.")
            return
        self.encounters[new_id] = {
            "id": new_id,
            "type": "single",
            "room": "",
            "player_starts": True,
            "first_enter_only": False,
            "waves": [{"members": []}],
            "conditions": [],
            "conditions_text": "",
            "modifiers": {"enemy_hp_pct": 100, "enemy_atk_pct": 100, "enemy_speed_pct": 100}
        }
        self.current_id = new_id
        self.refreshEncounterList()

    def onDuplicate(self):
        if not self.current_id:
            return
        src = json.loads(json.dumps(self.encounters[self.current_id]))
        new_id, ok = QInputDialog.getText(self, "Duplicate Encounter", "Enter new ID:")
        if not ok or not new_id:
            return
        new_id = new_id.strip()
        if not new_id or new_id in self.encounters:
            QMessageBox.warning(self, "Duplicate", "Invalid or duplicate ID.")
            return
        src["id"] = new_id
        self.encounters[new_id] = src
        self.current_id = new_id
        self.refreshEncounterList()

    def onDelete(self):
        if not self.current_id:
            return
        if QMessageBox.question(self, "Delete Encounter", f"Delete '{self.current_id}'?") != QMessageBox.Yes:
            return
        self.encounters.pop(self.current_id, None)
        self.current_id = None
        self.refreshEncounterList()

    # ------------- Save/Reload/Validate -------------
    def onSave(self):
        if not self.current_id:
            QMessageBox.warning(self, "Save", "No encounter selected.")
            return
        eid = self.current_id
        e = self.encounters[eid]

        # basics
        e["id"] = eid
        e["type"] = self.type_combo.currentText()
        e["room"] = self.room_combo.currentText().strip()
        e["player_starts"] = bool(self.chk_player_starts.isChecked())
        e["first_enter_only"] = bool(self.chk_first_enter_only.isChecked())
        e["pre_cinematic"] = self.pre_cine_combo.currentText().strip()
        e["post_cinematic"] = self.post_cine_combo.currentText().strip()
        e["initiative_bonus"] = int(self.init_bonus_spin.value())
        e["preempt_damage_pct"] = int(self.preempt_pct_spin.value())
        e["start_status"] = self.start_status_edit.text().strip()
        e["max_repeats"] = int(self.max_repeats_spin.value())
        e["once_per_save"] = bool(self.once_per_save_chk.isChecked())
        e["respawn_cooldown_sec"] = int(self.respawn_sec_spin.value())
        e["encounter_state_id"] = self.state_id_edit.text().strip()
        e["spoils_profile"] = self.spoils_edit.text().strip()
        e["modifiers"] = {
            "enemy_hp_pct": int(self.hp_pct_spin.value()),
            "enemy_atk_pct": int(self.atk_pct_spin.value()),
            "enemy_speed_pct": int(self.spd_pct_spin.value())
        }

        # waves
        idx = self._active_wave_index()
        if idx is not None and 0 <= idx < len(self._current_waves):
            # write back edited members/options of active wave
            w = self._current_waves[idx]
            w["delay_ms"] = int(self.wave_delay_spin.value())
            w["spawn_effect"] = self.wave_spawn_edit.text().strip()
            w["members"] = self._read_members_from_table()
        e["waves"] = self._current_waves

        # conditions
        text = self.cond_text.toPlainText()
        conds, _errs = parse_conditions_text(text)
        e["conditions"] = conds
        e["conditions_text"] = text

        self.saveEncounters()
        self.refreshEncounterList()

    def onReload(self):
        self.room_ids = collect_room_ids(self.root)
        self.enemy_ids = collect_enemy_ids(self.root)
        self.cinematic_ids = collect_cinematic_ids(self.root)
        self.loadEncounters()
        self.refreshEncounterList()

    def onValidate(self):
        issues = self.validate()
        if issues:
            QMessageBox.warning(self, "Validation", "\n".join(issues[:200]))
        else:
            QMessageBox.information(self, "Validation", "Looks good!")

    # Studio-friendly
    def validate(self) -> List[str]:
        issues: List[str] = []

        room_set = set(self.room_ids)
        enemy_set = set(self.enemy_ids)
        cine_set = set(self.cinematic_ids)

        # duplicate id paranoia
        if len(set(self.encounters.keys())) != len(self.encounters):
            issues.append("Duplicate encounter IDs detected.")

        for eid, e in self.encounters.items():
            if not eid:
                issues.append("Encounter without 'id'.")
                continue
            t = e.get("type","single")
            if t not in ALLOWED_TYPES:
                issues.append(f"{eid}: unknown type '{t}'")

            room = e.get("room") or e.get("room_id") or ""
            if room_set and room and room not in room_set:
                issues.append(f"{eid}: unknown room '{room}'")

            pre = e.get("pre_cinematic","")
            post = e.get("post_cinematic","")
            if pre and cine_set and pre not in cine_set:
                issues.append(f"{eid}: unknown pre_cinematic '{pre}'")
            if post and cine_set and post not in cine_set:
                issues.append(f"{eid}: unknown post_cinematic '{post}'")

            waves = e.get("waves")
            if not isinstance(waves, list) or not waves:
                # support legacy 'members'
                members = e.get("members", [])
                if isinstance(members, list) and members:
                    waves = [{"members": members}]
                else:
                    issues.append(f"{eid}: has no waves/members")
                    continue

            for wi, w in enumerate(waves):
                mem = w.get("members", [])
                if not mem:
                    issues.append(f"{eid}: wave {wi+1} has no members")
                for m in mem:
                    mid = (m.get("id") or "").strip()
                    qty = int(m.get("qty") or 0)
                    if not mid:
                        issues.append(f"{eid}: wave {wi+1} has member with empty id")
                    if qty <= 0:
                        issues.append(f"{eid}: wave {wi+1} member {mid or '?'} qty must be > 0")
                    if enemy_set and mid and mid not in enemy_set:
                        issues.append(f"{eid}: wave {wi+1} unknown enemy '{mid}'")

            # numeric sanity
            for key, lo, hi in [
                ("initiative_bonus", -100, 100),
                ("preempt_damage_pct", 0, 100),
                ("max_repeats", 0, 999),
                ("respawn_cooldown_sec", 0, 86400),
            ]:
                v = int(e.get(key, 0) or 0)
                if v < lo or v > hi:
                    issues.append(f"{eid}: {key} out of range [{lo},{hi}]")

            mods = e.get("modifiers", {}) or {}
            for key, lo, hi in [
                ("enemy_hp_pct", 10, 1000),
                ("enemy_atk_pct", 10, 1000),
                ("enemy_speed_pct", 10, 1000),
            ]:
                v = int(mods.get(key, 100) or 100)
                if v < lo or v > hi:
                    issues.append(f"{eid}: modifiers.{key} out of range [{lo},{hi}]")

        return issues

# -----------------------------
# Entrypoint
# -----------------------------
def _standalone_main():
    app = QApplication(sys.argv)
    editor = EncounterEditor(Path(__file__).parent)
    editor.resize(1200, 760)
    editor.show()
    sys.exit(app.exec_())

if __name__ == "__main__":
    _standalone_main()
