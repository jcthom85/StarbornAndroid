#!/usr/bin/env python3
# save_editor.py  — Starborn Save Editor (PyQt5) — envelope-aware
# Keeps all your existing features, adds support for /saves/*.json envelopes.
from __future__ import annotations

import json, os, sys
from pathlib import Path
from typing import Any, Optional

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (
    QApplication, QWidget, QFileDialog, QMessageBox, QMainWindow,
    QHBoxLayout, QVBoxLayout, QListWidget, QListWidgetItem,
    QLabel, QLineEdit, QPushButton, QTabWidget, QComboBox, QSpinBox,
    QTableWidget, QTableWidgetItem, QHeaderView, QAbstractItemView,
    QToolButton, QFormLayout, QGroupBox, QPlainTextEdit
)

from theme_kit import ThemeManager
from data_core import detect_project_root
# (You weren’t using json_load/json_save/UndoManager/EditorBus helpers here, so we omit them.)

# ----------------------------
# Helpers / file I/O
# ----------------------------
def _safe_load_json(path: Path, default):
    if not path.exists():
        return default
    try:
        with path.open("r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        QMessageBox.critical(None, "Load error", f"Failed to load {path.name}:\n{e}")
        return default

def _save_json(path: Path, data: Any) -> bool:
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        return True
    except Exception as e:
        QMessageBox.critical(None, "Save error", f"Failed to save {path.name}:\n{e}")
        return False

def _dict_get(d: dict, path: list[str], default=None):
    cur = d
    for k in path:
        if not isinstance(cur, dict) or k not in cur:
            return default
        cur = cur[k]
    return cur

def _dict_ensure(d: dict, path: list[str], default_factory):
    cur = d
    for k in path[:-1]:
        if k not in cur or not isinstance(cur[k], dict):
            cur[k] = {}
        cur = cur[k]
    last = path[-1]
    if last not in cur:
        cur[last] = default_factory()
    return cur[last]

# ----------------------------
# Lightweight Level Curve
# ----------------------------
class LevelCurve:
    def __init__(self, leveling_path: Path):
        data = _safe_load_json(leveling_path, {"level_curve": {1: 0}})
        raw = data.get("level_curve", {})
        self.curve = {int(k): int(v) for k, v in raw.items()}
        self.max_level = max(self.curve) if self.curve else 1

    def level_for_xp(self, total_xp: int) -> int:
        lvl = 1
        for level in sorted(self.curve):
            if total_xp >= self.curve[level]:
                lvl = level
            else:
                break
        return lvl

    def next_level_xp(self, level: int) -> Optional[int]:
        if level >= self.max_level:
            return None
        return self.curve.get(level + 1)

    def bounds_for_level(self, level: int) -> tuple[int, Optional[int]]:
        return self.curve.get(level, 0), self.next_level_xp(level)

# ----------------------------
# Small widgets
# ----------------------------
class ListEditor(QWidget):
    """Simple list[str] editor with + / – / up / down."""
    def __init__(self, title: str = "", placeholder: str = "new value"):
        super().__init__()
        v = QVBoxLayout(self)
        if title:
            v.addWidget(QLabel(title))
        self.list = QListWidget()
        self.list.setSelectionMode(QAbstractItemView.SingleSelection)
        v.addWidget(self.list, 1)
        row = QHBoxLayout()
        self.add_btn = QToolButton(); self.add_btn.setText("+")
        self.del_btn = QToolButton(); self.del_btn.setText("–")
        self.up_btn  = QToolButton(); self.up_btn.setText("↑")
        self.dn_btn  = QToolButton(); self.dn_btn.setText("↓")
        for b in (self.add_btn, self.del_btn, self.up_btn, self.dn_btn):
            row.addWidget(b)
        v.addLayout(row)
        self.placeholder = placeholder
        self.add_btn.clicked.connect(self._add)
        self.del_btn.clicked.connect(self._del)
        self.up_btn.clicked.connect(lambda: self._move(-1))
        self.dn_btn.clicked.connect(lambda: self._move(+1))

    def set_list(self, vals: list[str] | None):
        self.list.clear()
        for s in (vals or []):
            self.list.addItem(s)

    def get_list(self) -> list[str]:
        return [self.list.item(i).text() for i in range(self.list.count())]

    def _add(self):
        from PyQt5.QtWidgets import QInputDialog
        text, ok = QInputDialog.getText(self, "Add", self.placeholder)
        if ok and text.strip():
            self.list.addItem(text.strip())

    def _del(self):
        r = self.list.currentRow()
        if r >= 0:
            self.list.takeItem(r)

    def _move(self, delta: int):
        r = self.list.currentRow()
        if r < 0: return
        nr = r + delta
        if 0 <= nr < self.list.count():
            it = self.list.takeItem(r)
            self.list.insertItem(nr, it)
            self.list.setCurrentRow(nr)

# ----------------------------
# Main Editor
# ----------------------------
class SaveEditor(QMainWindow):
    def __init__(self, project_root: Optional[str] = None):
        super().__init__()
        self.setWindowTitle("Starborn Save Editor")
        self.resize(1200, 780)

        # Resolve project root
        if project_root:
            self.project_root = Path(project_root).resolve()
        else:
            here = Path(__file__).resolve()
            self.project_root = here.parents[1] if here.parent.name == "tools" else here.parent

        # Useful locations
        self.saves_dir = self.project_root / "saves"

        # Data cache
        self.save_path: Optional[Path] = None
        self.save_data: dict = {}        # payload only
        self._enveloped: bool = False    # whether the file has {meta,payload}
        self._meta: dict = {}            # meta when enveloped

        # Data sources (for auto-complete etc.)
        self.items = _safe_load_json(self.project_root / "items.json", [])
        self.rooms = _safe_load_json(self.project_root / "rooms.json", [])
        self.skills = _safe_load_json(self.project_root / "skills.json", [])
        self.progression = _safe_load_json(self.project_root / "progression.json", {})
        self.level_curve = LevelCurve(self.project_root / "leveling_data.json")

        # Indices
        self.equippables = sorted([i.get("name","") for i in self.items if i.get("type") == "equippable"])
        self.all_item_names = sorted([i.get("name","") for i in self.items if i.get("name")])
        self.room_ids = [r.get("id") for r in self.rooms if isinstance(r, dict) and r.get("id")]
        self.skills_by_char = {}
        for s in self.skills:
            cid = s.get("character"); sid = s.get("id")
            if cid and sid:
                self.skills_by_char.setdefault(cid, []).append(sid)

        # UI skeleton
        self.tabs = QTabWidget()
        self.setCentralWidget(self.tabs)

        self._build_toolbar()
        self._build_characters_tab()
        self._build_gamestate_tab()
        self._build_inventory_tab()
        self._build_quests_tab()
        self._build_raw_tab()
        self._build_meta_tab()

        self._refresh_all(disable=True)

    # ---------------- Toolbar
    def _build_toolbar(self):
        bar = self.addToolBar("File")
        bar.setMovable(False)

        btn_open = QPushButton("Open Save…")
        btn_open_saves = QPushButton("Open from /saves…")
        btn_open_latest = QPushButton("Open Latest Autosave")
        btn_open_quick = QPushButton("Open Quicksave")
        btn_save = QPushButton("Save")
        btn_save_as = QPushButton("Save As…")

        btn_open.clicked.connect(self._on_open)
        btn_open_saves.clicked.connect(self._on_open_from_saves)
        btn_open_latest.clicked.connect(self._on_open_latest_autosave)
        btn_open_quick.clicked.connect(self._on_open_quicksave)
        btn_save.clicked.connect(self._on_save)
        btn_save_as.clicked.connect(self._on_save_as)

        for b in (btn_open, btn_open_saves, btn_open_latest, btn_open_quick, btn_save, btn_save_as):
            bar.addWidget(b)

    # ---------------- Characters Tab
    def _build_characters_tab(self):
        w = QWidget(); self.tabs.addTab(w, "Characters")
        h = QHBoxLayout(w)

        left = QVBoxLayout(); h.addLayout(left, 1)
        self.char_list = QListWidget()
        self.char_list.itemSelectionChanged.connect(self._on_char_select)
        left.addWidget(QLabel("Characters"))
        left.addWidget(self.char_list, 1)

        right = QVBoxLayout(); h.addLayout(right, 2)
        form = QFormLayout()
        self.char_id_lbl = QLabel("-")
        self.lvl = QSpinBox(); self.lvl.setRange(1, 999)
        self.xp  = QSpinBox(); self.xp.setRange(0, 10_000_000)
        self.hp  = QSpinBox(); self.hp.setRange(0, 999_999)
        self.ap  = QSpinBox(); self.ap.setRange(0, 999)

        self.weapon = QComboBox(); self.weapon.setEditable(True); self.weapon.addItem(""); self.weapon.addItems(self.equippables)
        self.armor  = QComboBox(); self.armor.setEditable(True); self.armor.addItem(""); self.armor.addItems(self.equippables)
        self.access = QComboBox(); self.access.setEditable(True); self.access.addItem(""); self.access.addItems(self.equippables)

        form.addRow("Char ID:", self.char_id_lbl)
        form.addRow("Level:", self.lvl)
        form.addRow("XP:", self.xp)
        form.addRow("HP:", self.hp)
        form.addRow("Ability Points:", self.ap)
        form.addRow("Weapon:", self.weapon)
        form.addRow("Armor:", self.armor)
        form.addRow("Accessory:", self.access)
        right.addLayout(form)

        self.unlocked = ListEditor("Unlocked Abilities", "ability_id")
        right.addWidget(self.unlocked, 1)

        btns = QHBoxLayout()
        self.btn_before_lvl = QPushButton("Set XP → one less than next level")
        self.btn_before_lvl.clicked.connect(self._set_xp_before_next_level)
        self.btn_recalc_lvl = QPushButton("Recalculate level from XP")
        self.btn_recalc_lvl.clicked.connect(self._recalc_level_from_xp)
        self.btn_apply_char = QPushButton("Apply to Save (this character)")
        self.btn_apply_char.clicked.connect(self._apply_character_changes)

        btns.addWidget(self.btn_before_lvl)
        btns.addWidget(self.btn_recalc_lvl)
        btns.addWidget(self.btn_apply_char)
        right.addLayout(btns)

    # ---------------- Game State Tab
    def _build_gamestate_tab(self):
        w = QWidget(); self.tabs.addTab(w, "Game State")
        v = QVBoxLayout(w)

        # Party
        party_box = QGroupBox("Party Order")
        pv = QVBoxLayout(party_box)
        self.party_list = QListWidget()
        self.party_list.setSelectionMode(QAbstractItemView.SingleSelection)
        self.btn_party_up = QToolButton(); self.btn_party_up.setText("↑")
        self.btn_party_dn = QToolButton(); self.btn_party_dn.setText("↓")
        row = QHBoxLayout(); row.addWidget(self.btn_party_up); row.addWidget(self.btn_party_dn)
        pv.addWidget(self.party_list, 1); pv.addLayout(row)
        self.btn_party_up.clicked.connect(lambda: self._move_row(self.party_list, -1))
        self.btn_party_dn.clicked.connect(lambda: self._move_row(self.party_list, +1))

        # Economy / Resonance
        econ_box = QGroupBox("Economy & Resonance")
        ef = QFormLayout(econ_box)
        self.credits = QSpinBox(); self.credits.setRange(0, 999_999_999)
        self.res_cur = QSpinBox(); self.res_min = QSpinBox(); self.res_start = QSpinBox(); self.res_max = QSpinBox()
        for s in (self.res_cur, self.res_min, self.res_start, self.res_max): s.setRange(-9999, 9999)
        ef.addRow("Credits:", self.credits)
        ef.addRow("Resonance:", self.res_cur)
        ef.addRow("Resonance Min:", self.res_min)
        ef.addRow("Resonance Start Base:", self.res_start)
        ef.addRow("Resonance Max:", self.res_max)

        # Location / Teleport
        loc_box = QGroupBox("Location / Teleport")
        lf = QFormLayout(loc_box)
        self.cur_world = QLineEdit(); self.cur_hub = QLineEdit()
        self.cur_node = QLineEdit(); self.cur_room = QComboBox(); self.cur_room.setEditable(True); self.cur_room.addItems(self.room_ids)
        lf.addRow("World ID:", self.cur_world)
        lf.addRow("Hub ID:", self.cur_hub)
        lf.addRow("Node ID:", self.cur_node)
        lf.addRow("Room ID (teleport):", self.cur_room)
        self.btn_apply_gs = QPushButton("Apply to Save (Game State)")
        self.btn_apply_gs.clicked.connect(self._apply_gamestate_changes)

        # Room states quick view
        states_box = QGroupBox("Room States (JSON text per room)")
        sv = QVBoxLayout(states_box)
        self.room_states_table = QTableWidget(0, 2)
        self.room_states_table.setHorizontalHeaderLabels(["Room ID", "State (JSON object)"])
        self.room_states_table.horizontalHeader().setSectionResizeMode(0, QHeaderView.ResizeToContents)
        self.room_states_table.horizontalHeader().setSectionResizeMode(1, QHeaderView.Stretch)
        sv.addWidget(self.room_states_table, 1)

        v.addWidget(party_box)
        v.addWidget(econ_box)
        v.addWidget(loc_box)
        v.addWidget(self.btn_apply_gs)
        v.addWidget(states_box, 1)

    # ---------------- Inventory Tab
    def _build_inventory_tab(self):
        w = QWidget(); self.tabs.addTab(w, "Inventory")
        v = QVBoxLayout(w)

        row = QHBoxLayout()
        self.search = QLineEdit(); self.search.setPlaceholderText("Filter item names…")
        self.search.textChanged.connect(self._filter_inventory_table)
        self.btn_add_row = QPushButton("Add Row")
        self.btn_del_row = QPushButton("Delete Row")
        self.btn_add_row.clicked.connect(self._inv_add_row)
        self.btn_del_row.clicked.connect(self._inv_del_row)
        row.addWidget(self.search, 1); row.addWidget(self.btn_add_row); row.addWidget(self.btn_del_row)
        v.addLayout(row)

        self.inv_table = QTableWidget(0, 2)
        self.inv_table.setHorizontalHeaderLabels(["Item Name", "Qty"])
        self.inv_table.horizontalHeader().setSectionResizeMode(0, QHeaderView.Stretch)
        self.inv_table.horizontalHeader().setSectionResizeMode(1, QHeaderView.ResizeToContents)
        v.addWidget(self.inv_table, 1)

        self.btn_apply_inv = QPushButton("Apply to Save (Inventory)")
        self.btn_apply_inv.clicked.connect(self._apply_inventory_changes)
        v.addWidget(self.btn_apply_inv)

    # ---------------- Quests Tab
    def _build_quests_tab(self):
        w = QWidget(); self.tabs.addTab(w, "Quests")
        h = QHBoxLayout(w)

        left = QVBoxLayout(); h.addLayout(left, 1)
        self.quest_list = QListWidget()
        self.quest_list.itemSelectionChanged.connect(self._on_quest_select)
        left.addWidget(QLabel("Quests")); left.addWidget(self.quest_list, 1)

        right = QVBoxLayout(); h.addLayout(right, 2)
        form = QFormLayout()
        self.q_id = QLabel("-"); self.q_title = QLineEdit()
        self.q_status = QComboBox(); self.q_status.addItems(["inactive", "active", "complete"])
        self.q_stage = QSpinBox(); self.q_stage.setRange(0, 9999)
        form.addRow("ID:", self.q_id)
        form.addRow("Title:", self.q_title)
        form.addRow("Status:", self.q_status)
        form.addRow("Current Stage Idx:", self.q_stage)
        right.addLayout(form)

        self.btn_apply_q = QPushButton("Apply to Save (Quest)")
        self.btn_apply_q.clicked.connect(self._apply_quest_changes)
        right.addWidget(self.btn_apply_q); right.addStretch(1)

    # ---------------- Raw JSON Tab
    def _build_raw_tab(self):
        w = QWidget(); self.tabs.addTab(w, "Raw JSON")
        v = QVBoxLayout(w)
        self.raw = QPlainTextEdit()
        v.addWidget(self.raw, 1)
        apply = QPushButton("Apply raw JSON → working copy (no file write)")
        apply.clicked.connect(self._apply_raw)
        v.addWidget(apply)

    # ---------------- Meta Tab (read-only for envelopes)
    def _build_meta_tab(self):
        w = QWidget(); self.tabs.addTab(w, "Meta")
        v = QVBoxLayout(w)
        self.meta_view = QPlainTextEdit()
        self.meta_view.setReadOnly(True)
        v.addWidget(self.meta_view, 1)

    # ---------------- File ops
    def _open_path(self, path: Path):
        raw = _safe_load_json(path, {})
        self.save_path = path
        # envelope detection
        if isinstance(raw, dict) and "meta" in raw and "payload" in raw and isinstance(raw["payload"], dict):
            self._enveloped = True
            self._meta = raw.get("meta") or {}
            self.save_data = raw["payload"] or {}
        else:
            self._enveloped = False
            self._meta = {}
            self.save_data = raw if isinstance(raw, dict) else {}
        self._refresh_all()

    def _on_open(self):
        # Prefer project root for legacy saves; user can browse anywhere
        start_dir = str(self.project_root)
        path, _ = QFileDialog.getOpenFileName(self, "Open Save JSON", start_dir, "JSON (*.json)")
        if not path: return
        self._open_path(Path(path))

    def _on_open_from_saves(self):
        start_dir = str(self.saves_dir if self.saves_dir.exists() else self.project_root)
        path, _ = QFileDialog.getOpenFileName(self, "Open from /saves", start_dir, "JSON (*.json)")
        if not path: return
        self._open_path(Path(path))

    def _on_open_latest_autosave(self):
        if not self.saves_dir.exists():
            QMessageBox.information(self, "No autosaves", "The /saves folder does not exist yet.")
            return
        autos = sorted(self.saves_dir.glob("autosave-*.json"))
        if not autos:
            QMessageBox.information(self, "No autosaves", "No autosave-*.json found in /saves.")
            return
        self._open_path(autos[-1])

    def _on_open_quicksave(self):
        p = self.saves_dir / "quicksave.json"
        if not p.exists():
            QMessageBox.information(self, "No quicksave", "quicksave.json not found in /saves.")
            return
        self._open_path(p)

    def _write_current(self, path: Path) -> bool:
        if self._enveloped:
            # Recompute hash for payload
            try:
                from hashlib import sha256
                b = json.dumps(self.save_data, ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode("utf-8")
                meta = dict(self._meta)
                meta["hash"] = sha256(b).hexdigest()
                env = {"meta": meta, "payload": self.save_data}
            except Exception as e:
                QMessageBox.critical(self, "Meta error", f"Failed to prepare envelope meta:\n{e}")
                return False
            ok = _save_json(path, env)
        else:
            ok = _save_json(path, self.save_data)
        if ok:
            QMessageBox.information(self, "Saved", f"Saved to {path.name}")
        return ok

    def _on_save(self):
        if not self.save_path:
            return self._on_save_as()
        self._write_current(self.save_path)

    def _on_save_as(self):
        # If enveloped, default into /saves; otherwise project root
        default_dir = self.saves_dir if (self._enveloped and self.saves_dir.exists()) else self.project_root
        path, _ = QFileDialog.getSaveFileName(self, "Save As", str(default_dir), "JSON (*.json)")
        if not path: return
        self.save_path = Path(path)
        self._on_save()

    # ---------------- Refresh
    def _refresh_all(self, disable: bool=False):
        self._fill_characters()
        self._fill_gamestate()
        self._fill_inventory()
        self._fill_quests()
        self.raw.setPlainText(json.dumps(self.save_data or {}, ensure_ascii=False, indent=2))
        # meta tab
        if self._enveloped:
            self.meta_view.setPlainText(json.dumps(self._meta or {}, ensure_ascii=False, indent=2))
        else:
            self.meta_view.setPlainText("// Not an enveloped save (legacy plain JSON).")
        self.tabs.setEnabled(not disable)

    # --- Characters
    def _fill_characters(self):
        self.char_list.clear()
        chars = _dict_get(self.save_data, ["characters"], {}) or {}
        for cid in sorted(chars.keys()):
            QListWidgetItem(cid, self.char_list)
        if self.char_list.count():
            self.char_list.setCurrentRow(0)

    def _on_char_select(self):
        it = self.char_list.currentItem()
        if not it: return
        cid = it.text()
        c = _dict_get(self.save_data, ["characters", cid], {}) or {}
        self.char_id_lbl.setText(cid)
        self.lvl.setValue(int(c.get("level", 1)))
        self.xp.setValue(int(c.get("xp", 0)))
        self.hp.setValue(int(c.get("hp", 0)))
        self.ap.setValue(int(c.get("ability_points", 0)))
        eq = c.get("equipment", {}) or {}
        self.weapon.setCurrentText(eq.get("weapon") or "")
        self.armor.setCurrentText(eq.get("armor") or "")
        self.access.setCurrentText(eq.get("accessory") or "")
        self.unlocked.set_list(list(c.get("unlocked_abilities", [])))

    def _apply_character_changes(self):
        it = self.char_list.currentItem()
        if not it: return
        cid = it.text()
        c = _dict_ensure(self.save_data, ["characters", cid], dict)
        c["level"] = int(self.lvl.value())
        c["xp"] = int(self.xp.value())
        c["hp"] = int(self.hp.value())
        c["ability_points"] = int(self.ap.value())
        c["unlocked_abilities"] = self.unlocked.get_list()
        c["equipment"] = {
            "weapon": self.weapon.currentText() or None,
            "armor": self.armor.currentText() or None,
            "accessory": self.access.currentText() or None,
        }
        QMessageBox.information(self, "OK", f"Updated character '{cid}' in working copy.")

    def _set_xp_before_next_level(self):
        it = self.char_list.currentItem()
        if not it: return
        cur_level = int(self.lvl.value())
        nxt = self.level_curve.next_level_xp(cur_level)
        if nxt is None:
            QMessageBox.information(self, "Max level", "Already at or above max level curve.")
            return
        self.xp.setValue(max(0, nxt - 1))

    def _recalc_level_from_xp(self):
        lvl = self.level_curve.level_for_xp(int(self.xp.value()))
        self.lvl.setValue(lvl)

    # --- Game State
    def _fill_gamestate(self):
        gs = _dict_get(self.save_data, ["game_state"], {}) or {}
        # Party
        self.party_list.clear()
        for cid in gs.get("party", []):
            self.party_list.addItem(cid)
        # Econ
        self.credits.setValue(int(gs.get("credits", 0)))
        self.res_cur.setValue(int(gs.get("resonance", 0)))
        self.res_min.setValue(int(gs.get("resonance_min", 0)))
        self.res_start.setValue(int(gs.get("resonance_start_base", 0)))
        self.res_max.setValue(int(gs.get("resonance_max", 100)))
        # Location
        mp = gs.get("map", {}) or {}
        self.cur_world.setText(str(mp.get("current_world_id", "")))
        self.cur_hub.setText(str(mp.get("current_hub_id", "")))
        self.cur_node.setText(str(mp.get("current_node_id", "")))
        cur_room = str(mp.get("current_room_id", ""))
        self.cur_room.setCurrentText(cur_room)

        # Room states
        states = gs.get("room_states", {}) or {}
        self.room_states_table.setRowCount(0)
        for rid in sorted(states.keys()):
            r = self.room_states_table.rowCount()
            self.room_states_table.insertRow(r)
            self.room_states_table.setItem(r, 0, QTableWidgetItem(rid))
            self.room_states_table.setItem(r, 1, QTableWidgetItem(json.dumps(states[rid], ensure_ascii=False)))

    def _move_row(self, listw: QListWidget, delta: int):
        r = listw.currentRow()
        if r < 0: return
        nr = r + delta
        if 0 <= nr < listw.count():
            it = listw.takeItem(r)
            listw.insertItem(nr, it)
            listw.setCurrentRow(nr)

    def _apply_gamestate_changes(self):
        gs = _dict_ensure(self.save_data, ["game_state"], dict)

        # Party
        gs["party"] = [self.party_list.item(i).text() for i in range(self.party_list.count())]

        # Econ
        gs["credits"] = int(self.credits.value())
        gs["resonance"] = int(self.res_cur.value())
        gs["resonance_min"] = int(self.res_min.value())
        gs["resonance_start_base"] = int(self.res_start.value())
        gs["resonance_max"] = int(self.res_max.value())

        # Location / Teleport
        mp = gs.get("map") or {}
        mp["current_world_id"] = self.cur_world.text().strip()
        mp["current_hub_id"]   = self.cur_hub.text().strip()
        mp["current_node_id"]  = self.cur_node.text().strip()
        mp["current_room_id"]  = self.cur_room.currentText().strip()
        gs["map"] = mp

        # Room states (parse JSON per row)
        states = {}
        for r in range(self.room_states_table.rowCount()):
            rid = (self.room_states_table.item(r, 0).text() if self.room_states_table.item(r, 0) else "").strip()
            raw = self.room_states_table.item(r, 1).text() if self.room_states_table.item(r, 1) else "{}"
            if not rid:
                continue
            try:
                states[rid] = json.loads(raw) if raw.strip() else {}
            except Exception as e:
                QMessageBox.warning(self, "Room state parse", f"{rid}: {e}")
        gs["room_states"] = states

        QMessageBox.information(self, "OK", "Updated Game State in working copy.")

    # --- Inventory
    def _fill_inventory(self):
        gs = _dict_get(self.save_data, ["game_state"], {}) or {}
        inv = gs.get("inventory", {}) or {}
        self.inv_table.setRowCount(0)
        for name in sorted(inv.keys()):
            qty = int(inv.get(name, 0))
            self._inv_append_row(name, qty)

    def _inv_append_row(self, name: str, qty: int):
        r = self.inv_table.rowCount()
        self.inv_table.insertRow(r)
        item = QTableWidgetItem(name); item.setFlags(item.flags() | Qt.ItemIsEditable)
        self.inv_table.setItem(r, 0, item)
        qty_item = QTableWidgetItem(str(int(qty))); qty_item.setFlags(qty_item.flags() | Qt.ItemIsEditable)
        self.inv_table.setItem(r, 1, qty_item)

    def _inv_add_row(self):
        self._inv_append_row("", 1)

    def _inv_del_row(self):
        rows = sorted({i.row() for i in self.inv_table.selectedIndexes()}, reverse=True)
        for r in rows:
            self.inv_table.removeRow(r)

    def _filter_inventory_table(self):
        needle = (self.search.text() or "").lower().strip()
        for r in range(self.inv_table.rowCount()):
            name = (self.inv_table.item(r, 0).text() if self.inv_table.item(r, 0) else "")
            visible = (needle in name.lower())
            self.inv_table.setRowHidden(r, not visible)

    def _apply_inventory_changes(self):
        inv = {}
        for r in range(self.inv_table.rowCount()):
            name = (self.inv_table.item(r, 0).text() if self.inv_table.item(r, 0) else "").strip()
            qty_txt = (self.inv_table.item(r, 1).text() if self.inv_table.item(r, 1) else "0").strip()
            if not name:
                continue
            try:
                inv[name] = int(qty_txt)
            except ValueError:
                inv[name] = 0
        gs = _dict_ensure(self.save_data, ["game_state"], dict)
        gs["inventory"] = inv
        QMessageBox.information(self, "OK", f"Inventory updated ({len(inv)} items).")

    # --- Quests
    def _fill_quests(self):
        self.quest_list.clear()
        qs = _dict_get(self.save_data, ["game_state", "quests"], {}) or {}
        for qid in sorted(qs.keys()):
            QListWidgetItem(qid, self.quest_list)
        if self.quest_list.count():
            self.quest_list.setCurrentRow(0)

    def _on_quest_select(self):
        it = self.quest_list.currentItem()
        if not it: return
        qid = it.text()
        q = _dict_get(self.save_data, ["game_state", "quests", qid], {}) or {}
        self.q_id.setText(qid)
        self.q_title.setText(str(q.get("title", "")))
        self.q_status.setCurrentText(str(q.get("status", "inactive")))
        self.q_stage.setValue(int(q.get("current_stage_idx", 0)))

    def _apply_quest_changes(self):
        it = self.quest_list.currentItem()
        if not it: return
        qid = it.text()
        q = _dict_ensure(self.save_data, ["game_state", "quests", qid], dict)
        q["title"] = self.q_title.text().strip()
        q["status"] = self.q_status.currentText()
        q["current_stage_idx"] = int(self.q_stage.value())
        QMessageBox.information(self, "OK", f"Updated quest '{qid}'.")

    # --- Raw JSON apply
    def _apply_raw(self):
        try:
            self.save_data = json.loads(self.raw.toPlainText())
            self._refresh_all()
            QMessageBox.information(self, "OK", "Applied raw JSON to working copy.")
        except Exception as e:
            QMessageBox.critical(self, "Parse error", f"{e}")

# Standalone launch
if __name__ == "__main__":
    app = QApplication(sys.argv)
    root = Path(__file__).resolve().parents[1]  # project root (one above /tools)
    w = SaveEditor(str(root))
    w.show()
    sys.exit(app.exec_())
