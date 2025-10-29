#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations

import json, random
from pathlib import Path
from typing import Dict, List, Any, Optional, Tuple

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (
    QWidget, QSplitter, QHBoxLayout, QVBoxLayout, QFormLayout, QLineEdit,
    QListWidget, QListWidgetItem, QPushButton, QTableWidget, QTableWidgetItem,
    QHeaderView, QAbstractItemView, QGroupBox, QLabel, QSpinBox, QMessageBox,
    QComboBox, QToolButton, QCompleter
)

from data_core import detect_project_root, json_load, json_save
from editor_bus import goto as studio_goto, refresh_references as studio_refresh

ITEMS_FILE = "items.json"
RECIPES_FILE = "recipes_fishing.json"
RARITIES = ["common", "uncommon", "rare", "epic", "legendary"]

def _root_from_hint(hint: Optional[Path] = None) -> Path:
    try:
        return detect_project_root(hint)
    except Exception:
        return (Path(__file__).resolve().parent.parent)

def _read_items(root: Path) -> List[Dict[str, Any]]:
    return json_load(root / ITEMS_FILE, [])

def _item_names(root: Path) -> List[str]:
    items = _read_items(root)
    names = []
    for it in items:
        n = it.get("name") or it.get("id")
        if n: names.append(n)
    return sorted(set(names))

def _read_data(root: Path) -> Dict[str, Any]:
    data = json_load(root / RECIPES_FILE, {})
    data.setdefault("rods", [])
    data.setdefault("lures", [])
    data.setdefault("zones", {})
    return data

def _write_data(root: Path, data: Dict[str, Any]):
    json_save(root / RECIPES_FILE, data)

def _add_item_skeleton(root: Path, name: str, i_type: str = "component") -> bool:
    if not name.strip():
        return False
    items = _read_items(root)
    for it in items:
        if it.get("name") == name or it.get("id") == name:
            return True
    items.append({"id": name.lower().replace(" ", "_"), "name": name, "type": i_type, "price": 0})
    json_save(root / ITEMS_FILE, items)
    return True

class FishingEditor(QWidget):
    """
    Starborn — Fishing Editor
    Matches fishing_screen schema: rods[], lures[], zones{zone:[{item,weight,rarity}]}.
    """
    def __init__(self, root_dir: Optional[Path] = None):
        super().__init__()
        self.root = _root_from_hint(root_dir)
        self.setWindowTitle("Fishing Editor")
        self.resize(1200, 800)

        self.data: Dict[str, Any] = _read_data(self.root)
        self._items_cache: List[str] = _item_names(self.root)

        self._build_ui()
        self._reload_lists()

    # ---- Studio hooks ----
    def save(self) -> bool:
        # Commit zone table edits into self.data
        self._commit_zone_table()
        _write_data(self.root, self.data)
        return True

    def refresh_refs(self):
        self._items_cache = _item_names(self.root)
        self._apply_completer_to_table()
        self._apply_completer_to_line_edits()

    # ---- UI ----
    def _build_ui(self):
        splitter = QSplitter(self)
        main = QHBoxLayout(self)
        main.addWidget(splitter)
        self.setLayout(main)

        # Left: rods + lures
        left = QWidget()
        lv = QVBoxLayout(left)

        # Rods
        rod_box = QGroupBox("Rods (strings; must match item names/types you define)")
        rod_v = QVBoxLayout(rod_box)
        self.rod_list = QListWidget()
        rod_v.addWidget(self.rod_list)
        r_row = QHBoxLayout()
        self.inp_rod = QLineEdit(); self.inp_rod.setPlaceholderText("New rod name…")
        self.btn_add_rod = QPushButton("+ Add"); self.btn_del_rod = QPushButton("– Remove")
        self.btn_open_rod = QPushButton("Open"); self.btn_quick_rod = QPushButton("Quick Create")
        r_row.addWidget(self.inp_rod); r_row.addWidget(self.btn_add_rod); r_row.addWidget(self.btn_del_rod)
        r_row.addWidget(self.btn_open_rod); r_row.addWidget(self.btn_quick_rod)
        rod_v.addLayout(r_row)
        lv.addWidget(rod_box)

        # Lures
        lure_box = QGroupBox("Lures")
        lure_v = QVBoxLayout(lure_box)
        self.lure_list = QListWidget()
        lure_v.addWidget(self.lure_list)
        l_row = QHBoxLayout()
        self.inp_lure = QLineEdit(); self.inp_lure.setPlaceholderText("New lure name…")
        self.btn_add_lure = QPushButton("+ Add"); self.btn_del_lure = QPushButton("– Remove")
        self.btn_open_lure = QPushButton("Open"); self.btn_quick_lure = QPushButton("Quick Create")
        l_row.addWidget(self.inp_lure); l_row.addWidget(self.btn_add_lure); l_row.addWidget(self.btn_del_lure)
        l_row.addWidget(self.btn_open_lure); l_row.addWidget(self.btn_quick_lure)
        lure_v.addLayout(l_row)
        lv.addWidget(lure_box)

        splitter.addWidget(left)

        # Right: zones + loot table
        right = QWidget()
        rv = QVBoxLayout(right)

        z_row = QHBoxLayout()
        self.zone_pick = QComboBox()
        self.btn_add_zone = QPushButton("+ Add Zone")
        self.btn_del_zone = QPushButton("– Remove Zone")
        z_row.addWidget(QLabel("Zone:"))
        z_row.addWidget(self.zone_pick, 1)
        z_row.addWidget(self.btn_add_zone)
        z_row.addWidget(self.btn_del_zone)
        rv.addLayout(z_row)

        self.tbl = QTableWidget(0, 4)
        self.tbl.setHorizontalHeaderLabels(["Item", "Weight", "Rarity", ""])
        self.tbl.horizontalHeader().setSectionResizeMode(0, QHeaderView.Stretch)
        self.tbl.horizontalHeader().setSectionResizeMode(1, QHeaderView.ResizeToContents)
        self.tbl.horizontalHeader().setSectionResizeMode(2, QHeaderView.ResizeToContents)
        self.tbl.horizontalHeader().setSectionResizeMode(3, QHeaderView.ResizeToContents)
        self.tbl.setSelectionBehavior(QAbstractItemView.SelectRows)
        rv.addWidget(self.tbl)

        t_row = QHBoxLayout()
        self.btn_add = QPushButton("+ Add Row")
        self.btn_del = QPushButton("– Remove Row")
        self.btn_norm = QPushButton("Normalize Weights")
        self.btn_sim = QPushButton("Simulate 1000 Catches")
        t_row.addWidget(self.btn_add); t_row.addWidget(self.btn_del); t_row.addStretch(1)
        t_row.addWidget(self.btn_norm); t_row.addWidget(self.btn_sim)
        rv.addLayout(t_row)

        self.lbl_stats = QLabel("—"); rv.addWidget(self.lbl_stats)

        # bottom bar
        b_row = QHBoxLayout()
        b_row.addStretch(1)
        self.btn_revert = QPushButton("Revert")
        self.btn_save = QPushButton("Save")
        b_row.addWidget(self.btn_revert); b_row.addWidget(self.btn_save)
        rv.addLayout(b_row)

        splitter.addWidget(right); splitter.setStretchFactor(1, 1)

        # signals — left
        self.btn_add_rod.clicked.connect(lambda: self._add_to_list(self.rod_list, self.inp_rod, "rods"))
        self.btn_del_rod.clicked.connect(lambda: self._del_from_list(self.rod_list, "rods"))
        self.btn_open_rod.clicked.connect(lambda: self._goto(self._current_text(self.rod_list)))
        self.btn_quick_rod.clicked.connect(lambda: self._quick_create(self._current_or_field(self.rod_list, self.inp_rod), "fishing_rod"))

        self.btn_add_lure.clicked.connect(lambda: self._add_to_list(self.lure_list, self.inp_lure, "lures"))
        self.btn_del_lure.clicked.connect(lambda: self._del_from_list(self.lure_list, "lures"))
        self.btn_open_lure.clicked.connect(lambda: self._goto(self._current_text(self.lure_list)))
        self.btn_quick_lure.clicked.connect(lambda: self._quick_create(self._current_or_field(self.lure_list, self.inp_lure), "component"))

        # signals — zones
        self.zone_pick.currentTextChanged.connect(self._load_zone_table)
        self.btn_add_zone.clicked.connect(self._add_zone)
        self.btn_del_zone.clicked.connect(self._del_zone)

        # table signals
        self.btn_add.clicked.connect(lambda: self._add_row("", 1, "common"))
        self.btn_del.clicked.connect(self._del_rows)
        self.btn_norm.clicked.connect(self._normalize_weights)
        self.btn_sim.clicked.connect(lambda: self._simulate(1000))

        # bottom signals
        self.btn_revert.clicked.connect(self._reload_lists)
        self.btn_save.clicked.connect(lambda: self.save() and QMessageBox.information(self, "Saved", "recipes_fishing.json updated."))

    # ---- helpers ----
    def _reload_lists(self):
        self.data = _read_data(self.root)
        self.rod_list.clear(); self.lure_list.clear()
        for s in self.data.get("rods", []): self.rod_list.addItem(s)
        for s in self.data.get("lures", []): self.lure_list.addItem(s)

        self.zone_pick.blockSignals(True)
        self.zone_pick.clear()
        zones = sorted(self.data.get("zones", {}).keys())
        self.zone_pick.addItems(zones)
        self.zone_pick.blockSignals(False)

        if zones:
            self.zone_pick.setCurrentIndex(0)
            self._load_zone_table()
        else:
            self.tbl.setRowCount(0)
            self.lbl_stats.setText("—")
        self._apply_completer_to_line_edits()
        self._apply_completer_to_table()

    def _apply_completer_to_line_edits(self):
        comp = QCompleter(self._items_cache); comp.setCaseSensitivity(Qt.CaseInsensitive)
        self.inp_rod.setCompleter(comp)
        self.inp_lure.setCompleter(comp)

    def _apply_completer_to_table(self):
        comp = QCompleter(self._items_cache); comp.setCaseSensitivity(Qt.CaseInsensitive)
        for r in range(self.tbl.rowCount()):
            w = self.tbl.cellWidget(r, 0)
            if isinstance(w, QLineEdit):
                w.setCompleter(comp)

    def _add_to_list(self, lw: QListWidget, field: QLineEdit, key: str):
        name = (field.text() or "").strip()
        if not name: return
        if name in self.data[key]:
            QMessageBox.information(self, "Exists", f"'{name}' already in {key}.")
            return
        self.data[key].append(name); lw.addItem(name); field.setText("")

    def _del_from_list(self, lw: QListWidget, key: str):
        it = lw.currentItem()
        if not it: return
        name = it.text()
        self.data[key] = [s for s in self.data[key] if s != name]
        lw.takeItem(lw.currentRow())

    def _current_text(self, lw: QListWidget) -> str:
        it = lw.currentItem()
        return it.text() if it else ""

    def _current_or_field(self, lw: QListWidget, field: QLineEdit) -> str:
        t = self._current_text(lw)
        if t: return t
        return (field.text() or "").strip()

    # ---- zones / table ----
    def _add_zone(self):
        base = "New Zone"
        z = base; i = 1
        while z in self.data["zones"]:
            i += 1; z = f"{base} {i}"
        self.data["zones"][z] = []
        self.zone_pick.addItem(z)
        self.zone_pick.setCurrentText(z)
        self._load_zone_table()

    def _del_zone(self):
        z = self.zone_pick.currentText()
        if not z: return
        if QMessageBox.question(self, "Delete Zone", f"Delete zone '{z}'?") != QMessageBox.Yes:
            return
        self.data["zones"].pop(z, None)
        self._reload_lists()

    def _load_zone_table(self):
        self._commit_zone_table()
        z = self.zone_pick.currentText()
        self.tbl.setRowCount(0)
        if not z: return
        rows = self.data["zones"].get(z, [])
        for row in rows:
            self._add_row(row.get("item",""), int(row.get("weight",1)), row.get("rarity","common"))
        self._update_stats_label()

    def _commit_zone_table(self):
        z = self.zone_pick.currentText()
        if not z: return
        rows = []
        for r in range(self.tbl.rowCount()):
            w_item = self.tbl.cellWidget(r, 0)
            w_weight = self.tbl.cellWidget(r, 1)
            w_rarity = self.tbl.cellWidget(r, 2)
            if not (isinstance(w_item, QLineEdit) and isinstance(w_weight, QSpinBox) and isinstance(w_rarity, QComboBox)):
                continue
            nm = (w_item.text() or "").strip()
            wt = int(w_weight.value())
            rr = w_rarity.currentText() or "common"
            if nm and wt > 0:
                rows.append({"item": nm, "weight": wt, "rarity": rr})
        self.data["zones"][z] = rows

    def _add_row(self, item: str, weight: int, rarity: str):
        r = self.tbl.rowCount()
        self.tbl.insertRow(r)
        le = QLineEdit(); le.setText(item)
        comp = QCompleter(self._items_cache); comp.setCaseSensitivity(Qt.CaseInsensitive)
        le.setCompleter(comp)
        self.tbl.setCellWidget(r, 0, le)

        sp = QSpinBox(); sp.setRange(1, 999); sp.setValue(weight)
        self.tbl.setCellWidget(r, 1, sp)

        cb = QComboBox(); cb.addItems(RARITIES); 
        if rarity in RARITIES: cb.setCurrentText(rarity)
        self.tbl.setCellWidget(r, 2, cb)

        cell = QWidget(); lay = QHBoxLayout(cell); lay.setContentsMargins(0,0,0,0)
        btn_open = QToolButton(); btn_open.setText("Open")
        btn_quick = QToolButton(); btn_quick.setText("Create")
        lay.addWidget(btn_open); lay.addWidget(btn_quick); lay.addStretch(1)
        self.tbl.setCellWidget(r, 3, cell)

        def _open():
            nm = (le.text() or "").strip()
            if nm: studio_goto("item", nm)
        def _quick():
            nm = (le.text() or "").strip()
            if not nm:
                QMessageBox.warning(self, "Quick Create", "Enter a name first."); return
            if _add_item_skeleton(self.root, nm, i_type="component"):
                studio_refresh(); studio_goto("item", nm); self.refresh_refs()
        btn_open.clicked.connect(_open); btn_quick.clicked.connect(_quick)

    def _del_rows(self):
        rows = sorted({ix.row() for ix in self.tbl.selectedIndexes()}, reverse=True)
        for r in rows:
            self.tbl.removeRow(r)
        self._update_stats_label()

    def _normalize_weights(self):
        # Make weights sum to 100 (rounded)
        total = 0
        for r in range(self.tbl.rowCount()):
            w = self.tbl.cellWidget(r, 1)
            if isinstance(w, QSpinBox): total += int(w.value())
        if total <= 0: return
        # scale
        target = 100
        for r in range(self.tbl.rowCount()):
            w = self.tbl.cellWidget(r, 1)
            if isinstance(w, QSpinBox):
                val = int(round((int(w.value()) / total) * target))
                w.setValue(max(val, 1))
        self._update_stats_label()

    def _simulate(self, n: int):
        # Roll weighted choice n times
        items, weights = [], []
        for r in range(self.tbl.rowCount()):
            nm = self.tbl.cellWidget(r, 0).text().strip()
            wt = int(self.tbl.cellWidget(r, 1).value())
            if nm and wt > 0:
                items.append(nm); weights.append(wt)
        if not items:
            QMessageBox.information(self, "Simulate", "No entries to simulate."); return
        pulls = random.choices(items, weights=weights, k=n)
        counts: Dict[str,int] = {}
        for p in pulls: counts[p] = counts.get(p, 0) + 1
        top = sorted(counts.items(), key=lambda x: -x[1])[:8]
        msg = ", ".join([f"{k} {v/n:.1%}" for k,v in top])
        self.lbl_stats.setText(f"Top (n={n}): {msg}")

    def _update_stats_label(self):
        # show sum of weights
        total = 0
        for r in range(self.tbl.rowCount()):
            w = self.tbl.cellWidget(r, 1)
            if isinstance(w, QSpinBox): total += int(w.value())
        self.lbl_stats.setText(f"Total weight: {total}")

    # ---- item helpers ----
    def _goto(self, name: str):
        nm = (name or "").strip()
        if nm: studio_goto("item", nm)

    def _quick_create(self, name: str, i_type: str):
        nm = (name or "").strip()
        if not nm:
            QMessageBox.warning(self, "Quick Create", "Enter a name first."); return
        if _add_item_skeleton(self.root, nm, i_type=i_type):
            studio_refresh(); studio_goto("item", nm); self.refresh_refs()

if __name__ == "__main__":
    from PyQt5.QtWidgets import QApplication
    import sys
    app = QApplication(sys.argv)
    w = FishingEditor()
    w.show()
    sys.exit(app.exec_())
