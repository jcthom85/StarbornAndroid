#!/usr/bin/env python3
"""
Starborn — Enemy Editor (v3.1)
• Project-root auto-detect; loads enemies.json + items.json for drop validation.
• Tabs: Basics • Stats • Resistances • Drops • Raw JSON
• Add / Duplicate / Delete; ID rename-safe duplication.
• Validate: required fields, ranges, allowed enums, drops exist, chance [0..1].
• Sorted, pretty-printed saves with .bak backup and clear errors.
• Exposes .save() and .validate() for Studio.
"""
from __future__ import annotations
import sys, json, os
from pathlib import Path
from typing import Any, Dict, List, Optional

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (
    QApplication, QWidget, QTabWidget, QHBoxLayout, QVBoxLayout, QSplitter,
    QListWidget, QListWidgetItem, QLabel, QLineEdit, QComboBox, QSpinBox, QFileDialog, QCompleter,
    QPushButton, QFormLayout, QGroupBox, QTableWidget, QTableWidgetItem,
    QAbstractItemView, QMessageBox, QDoubleSpinBox, QTextEdit
)

# Local helpers
if __name__ == "__main__":
    sys.path.append(str(Path(__file__).resolve().parent / "tools"))
from tools.starborn_data import (
    find_project_root, read_list_json, write_json_with_backup,
    collect_item_names, validate_enemy, ELEMENTS, TIERS
)

def _as_list(val):
    if val is None: return []
    return val if isinstance(val, list) else [val]

class ListEditor(QWidget):
    """A simple reusable list editor for string lists."""
    def __init__(self, title: str, placeholder: str = "value"):
        super().__init__()
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        if title:
            layout.addWidget(QLabel(title))
        self.list_widget = QListWidget()
        layout.addWidget(self.list_widget)
        
        btn_layout = QHBoxLayout()
        add_btn = QPushButton("Add")
        del_btn = QPushButton("Delete")
        add_btn.clicked.connect(self._add_item)
        del_btn.clicked.connect(self._del_item)
        btn_layout.addWidget(add_btn)
        btn_layout.addWidget(del_btn)
        btn_layout.addStretch()
        layout.addLayout(btn_layout)
        self.placeholder = placeholder

    def _add_item(self):
        self.list_widget.addItem(self.placeholder)
        self.list_widget.editItem(self.list_widget.item(self.list_widget.count() - 1))

    def _del_item(self):
        for item in self.list_widget.selectedItems():
            self.list_widget.takeItem(self.list_widget.row(item))

    def get_list(self) -> List[str]:
        return [self.list_widget.item(i).text() for i in range(self.list_widget.count())]

    def set_list(self, values: List[str]):
        self.list_widget.clear()
        if values:
            self.list_widget.addItems(values)

class EnemyEditor(QWidget):
    """Exposes .save() and .validate() for Studio integration."""
    def __init__(self, project_root: Optional[str] = None):
        super().__init__()
        self.setWindowTitle("Starborn — Enemies")
        self.root = Path(project_root).resolve() if project_root else find_project_root(Path(__file__).parent)

        # Data
        self.enemies: List[Dict[str, Any]] = read_list_json(self.root / "enemies.json")
        self.items:   List[Dict[str, Any]] = read_list_json(self.root / "items.json")
        self.item_names = collect_item_names(self.root)

        self.current_idx: Optional[int] = None

        # UI
        self._build_ui()
        self._reload_list()

    # ---- paths
    @property
    def enemies_path(self) -> Path: return self.root / "enemies.json"

    # ---- Studio API
    def save(self) -> bool: return self._save_all()
    def validate(self) -> List[str]:
        issues = []
        ids = set()
        for e in self.enemies:
            issues += validate_enemy(e, self.item_names)
            eid = e.get("id")
            if eid in ids: issues.append(f"Duplicate id: {eid}")
            ids.add(eid)
        return issues

    # ---------------- UI ----------------
    def _build_ui(self):
        split = QSplitter(Qt.Horizontal, self)
        root = QHBoxLayout(self); root.addWidget(split)

        # LEFT: list + toolbar
        left = QWidget(); l = QVBoxLayout(left)
        self.search = QLineEdit(); self.search.setPlaceholderText("Search by id / name / ability…")
        self.search.textChanged.connect(self._reload_list)
        l.addWidget(self.search)

        self.list = QListWidget()
        self.list.itemSelectionChanged.connect(self._on_select)
        l.addWidget(self.list, 1)

        row = QHBoxLayout()
        b_new = QPushButton("New"); b_dup = QPushButton("Duplicate"); b_del = QPushButton("Delete")
        b_val = QPushButton("Validate"); b_save = QPushButton("Save")
        b_new.clicked.connect(self._on_new)
        b_dup.clicked.connect(self._on_duplicate)
        b_del.clicked.connect(self._on_delete)
        b_val.clicked.connect(self._on_validate_click)
        b_save.clicked.connect(self._save_all)
        for b in (b_new, b_dup, b_del): row.addWidget(b)
        row.addStretch(1); row.addWidget(b_val); row.addWidget(b_save)
        l.addLayout(row)

        split.addWidget(left)

        # RIGHT: tabs
        self.tabs = QTabWidget()
        split.addWidget(self.tabs)
        split.setStretchFactor(1, 3)

        # --- Tab: Basics
        self.tab_basic = QWidget(); fb = QFormLayout(self.tab_basic)
        self.f_id = QLineEdit(); self.f_name = QLineEdit()
        self.f_tier = QComboBox(); self.f_tier.addItems(TIERS)
        self.f_elem = QComboBox(); self.f_elem.addItems(ELEMENTS)

        # --- NEW: Portrait field with file browser ---
        portrait_layout = QHBoxLayout()
        self.f_portrait = QLineEdit(); self.f_portrait.setPlaceholderText("e.g. images/portraits/enemies/drone.png")
        portrait_browse_btn = QPushButton("…")
        portrait_browse_btn.setToolTip("Browse for portrait image")
        portrait_browse_btn.setFixedWidth(30)
        portrait_browse_btn.clicked.connect(self._browse_for_portrait)
        portrait_layout.addWidget(self.f_portrait); portrait_layout.addWidget(portrait_browse_btn)
        # --- END NEW ---
        
        self.f_description = QTextEdit(); self.f_description.setPlaceholderText("Flavor text for the enemy...")
        self.f_sprite_editor = ListEditor("Sprite Paths", "images/enemies/new.png")
        self.f_abilities = QLineEdit(); self.f_abilities.setPlaceholderText("ability_id, another_ability_id …")
        fb.addRow("ID", self.f_id)
        fb.addRow("Name", self.f_name)
        fb.addRow("Description", self.f_description)
        fb.addRow("Tier", self.f_tier)
        fb.addRow("Element", self.f_elem)
        fb.addRow("Portrait (path)", portrait_layout)
        fb.addRow(self.f_sprite_editor)
        fb.addRow("Abilities (comma)", self.f_abilities)
        self.tabs.addTab(self.tab_basic, "Basics")

        # --- Tab: Stats
        self.tab_stats = QWidget(); fs = QFormLayout(self.tab_stats)
        self.f_hp = QSpinBox(); self.f_hp.setRange(1, 999999); self.f_hp.setValue(30)
        self.f_speed = QSpinBox(); self.f_speed.setRange(1, 999); self.f_speed.setValue(20)
        self.f_attack = QSpinBox(); self.f_attack.setRange(0, 9999); self.f_attack.setValue(10)
        self.f_strength = QSpinBox(); self.f_strength.setRange(0, 999)
        self.f_vitality = QSpinBox(); self.f_vitality.setRange(0, 999)
        self.f_agility = QSpinBox(); self.f_agility.setRange(0, 999)
        self.f_focus = QSpinBox(); self.f_focus.setRange(0, 999)
        self.f_luck = QSpinBox(); self.f_luck.setRange(0, 999)
        self.f_xp = QSpinBox(); self.f_xp.setRange(0, 999999)
        self.f_ap = QSpinBox(); self.f_ap.setRange(0, 9999)
        self.f_credits = QSpinBox(); self.f_credits.setRange(0, 9999999)
        fs.addRow("HP", self.f_hp)
        fs.addRow("Speed", self.f_speed)
        fs.addRow("Attack", self.f_attack)
        fs.addRow("Strength", self.f_strength); fs.addRow("Vitality", self.f_vitality)
        fs.addRow("Agility", self.f_agility);   fs.addRow("Focus", self.f_focus)
        fs.addRow("Luck", self.f_luck)
        fs.addRow("XP Reward", self.f_xp)
        fs.addRow("AP Reward", self.f_ap)
        fs.addRow("Credit Reward", self.f_credits)
        self.tabs.addTab(self.tab_stats, "Stats")

        # --- Tab: Resistances
        self.tab_resist = QWidget()
        vr = QVBoxLayout(self.tab_resist)
        self.res_rows: Dict[str, QSpinBox] = {}
        top = QHBoxLayout()
        top.addWidget(QLabel("Set resistances in % (−100..+100); positive = resistant"))
        vr.addLayout(top)
        for el in ELEMENTS[1:]:  # skip "none"
            row = QHBoxLayout()
            sp = QSpinBox(); sp.setRange(-100, 100); sp.setValue(0)
            self.res_rows[el] = sp
            row.addWidget(QLabel(el.capitalize())); row.addWidget(sp, 1)
            vr.addLayout(row)
        self.tabs.addTab(self.tab_resist, "Resistances")

        # --- Tab: Drops
        self.tab_drops = QWidget()
        vd = QVBoxLayout(self.tab_drops)
        self.drop_table = QTableWidget(0, 4)
        self.drop_table.setHorizontalHeaderLabels(["Item name", "Chance (0..1)", "Qty min", "Qty max"])
        self.drop_table.setSelectionBehavior(QAbstractItemView.SelectRows)
        vd.addWidget(self.drop_table, 1)
        row2 = QHBoxLayout()
        b_add = QPushButton("Add Row"); b_add.clicked.connect(self._add_drop_row); row2.addWidget(b_add)
        b_rem = QPushButton("Remove Row"); b_rem.clicked.connect(self._remove_drop_row); row2.addWidget(b_rem)
        vd.addLayout(row2)
        self.tabs.addTab(self.tab_drops, "Drops")

        # --- Tab: Raw
        self.tab_raw = QWidget(); vraw = QVBoxLayout(self.tab_raw)
        self.raw_edit = QTextEdit(); vraw.addWidget(self.raw_edit, 1)
        self.tabs.addTab(self.tab_raw, "Raw JSON")

    def _browse_for_portrait(self):
        """Open a file dialog to select a portrait image."""
        # Start browsing in a sensible directory, like 'images/portraits' or the project root.
        start_dir = self.root / "images" / "portraits"
        if not start_dir.exists():
            start_dir = self.root

        path, _ = QFileDialog.getOpenFileName(self, "Select Portrait Image", str(start_dir), "Images (*.png *.jpg *.jpeg)")
        if path:
            try:
                rel_path = os.path.relpath(path, str(self.root)).replace("\\", "/")
                self.f_portrait.setText(rel_path)
            except ValueError:
                self.f_portrait.setText(path) # Fallback to absolute path if on a different drive
    # ---------------- Data ↔ UI ----------------
    def _reload_list(self):
        self.list.clear()
        ft = (self.search.text() or "").lower().strip()
        for i, e in enumerate(sorted(self.enemies, key=lambda d: (d.get("id") or ""))):
            eid = e.get("id","")
            nm = e.get("name","")
            abilities = ",".join(_as_list(e.get("abilities", [])))
            hay = f"{eid} {nm} {abilities}".lower()
            if ft and ft not in hay: continue
            it = QListWidgetItem(eid or f"(untitled {i})")
            it.setData(Qt.UserRole, self._index_of_id(eid))
            self.list.addItem(it)
        if self.current_idx is not None and 0 <= self.current_idx < self.list.count():
            self.list.setCurrentRow(self.current_idx)

    def _on_select(self):
        sel = self.list.selectedItems()
        if not sel: return
        # push current to memory first
        self._save_current_to_memory()
        idx = sel[0].data(Qt.UserRole)
        self.current_idx = idx if isinstance(idx, int) else self._index_of_id(sel[0].text())
        e = self.enemies[self.current_idx]
        # Basics
        self.f_id.setText(e.get("id","")); self.f_name.setText(e.get("name",""))
        self.f_description.setPlainText(e.get("description", e.get("flavor", "")))
        self.f_tier.setCurrentText(e.get("tier","standard"))
        self.f_elem.setCurrentText(e.get("element","none"))
        self.f_portrait.setText(e.get("portrait",""))
        self.f_sprite_editor.set_list(_as_list(e.get("sprite", [])))
        self.f_abilities.setText(", ".join(_as_list(e.get("abilities", []))))
        # Stats
        self.f_hp.setValue(int(e.get("hp", 30)))
        self.f_speed.setValue(int(e.get("speed", 20)))
        self.f_attack.setValue(int(e.get("attack", 10)))
        self.f_strength.setValue(int(e.get("strength", 0)))
        self.f_vitality.setValue(int(e.get("vitality", 0)))
        self.f_agility.setValue(int(e.get("agility", 0)))
        self.f_focus.setValue(int(e.get("focus", 0)))
        self.f_luck.setValue(int(e.get("luck", 0)))
        self.f_xp.setValue(int(e.get("xp_reward", 0)))
        self.f_ap.setValue(int(e.get("ap_reward", 0)))
        self.f_credits.setValue(int(e.get("credit_reward", 0)))
        # Resistances
        res = e.get("resistances", {})
        for k, sp in self.res_rows.items():
            sp.setValue(int(res.get(k, 0)))
        # Drops
        self._load_drops(e.get("drops", []))
        # Raw
        self.raw_edit.setPlainText(json.dumps(e, ensure_ascii=False, indent=4))

    def _save_current_to_memory(self):
        if self.current_idx is None: return
        e = self.enemies[self.current_idx]
        e["id"] = self.f_id.text().strip()
        e["name"] = self.f_name.text().strip()
        e["description"] = self.f_description.toPlainText().strip()
        e["tier"] = self.f_tier.currentText()
        e["element"] = self.f_elem.currentText()
        e["portrait"] = self.f_portrait.text().strip()
        e["sprite"] = self.f_sprite_editor.get_list()
        e["abilities"] = [a.strip() for a in self.f_abilities.text().split(",") if a.strip()]
        e["hp"] = self.f_hp.value()
        e["speed"] = self.f_speed.value()
        e["attack"] = self.f_attack.value()
        e["strength"] = self.f_strength.value()
        e["vitality"] = self.f_vitality.value()
        e["agility"] = self.f_agility.value()
        e["focus"] = self.f_focus.value()
        e["luck"] = self.f_luck.value()
        e["xp_reward"] = self.f_xp.value()
        e["ap_reward"] = self.f_ap.value()
        e["credit_reward"] = self.f_credits.value()
        e["resistances"] = {k: sp.value() for k, sp in self.res_rows.items()}
        e["drops"] = self._gather_drops()
        self.raw_edit.setPlainText(json.dumps(e, ensure_ascii=False, indent=4))

    # ---------------- Drops table helpers ----------------
    def _load_drops(self, drops: List[dict]):
        self.drop_table.setRowCount(0)
        for dr in drops:
            r = self.drop_table.rowCount(); self.drop_table.insertRow(r)
            
            # Item Name ComboBox
            combo = QComboBox(); combo.setEditable(True)
            combo.addItems([""] + self.item_names)
            combo.setCurrentText(dr.get("id", ""))
            self.drop_table.setCellWidget(r, 0, combo)
            
            self.drop_table.setItem(r, 1, QTableWidgetItem(str(dr.get("chance", 1))))
            self.drop_table.setItem(r, 2, QTableWidgetItem(str(dr.get("qty_min", 1))))
            self.drop_table.setItem(r, 3, QTableWidgetItem(str(dr.get("qty_max", 1))))

    def _gather_drops(self) -> List[dict]:
        out = []
        for r in range(self.drop_table.rowCount()):
            name_widget = self.drop_table.cellWidget(r, 0)
            name = (name_widget.currentText() if isinstance(name_widget, QComboBox) else "").strip()
            chance = float(self.drop_table.item(r,1).text()) if self.drop_table.item(r,1) else 1.0
            qmin = int(self.drop_table.item(r,2).text()) if self.drop_table.item(r,2) else 1
            qmax = int(self.drop_table.item(r,3).text()) if self.drop_table.item(r,3) else qmin
            if not name: continue
            out.append({"id": name, "chance": chance, "qty_min": qmin, "qty_max": qmax})
        return out

    def _add_drop_row(self):
        r = self.drop_table.rowCount(); self.drop_table.insertRow(r)
        
        combo = QComboBox(); combo.setEditable(True)
        combo.addItems([""] + self.item_names)
        completer = QCompleter(self.item_names); completer.setCaseSensitivity(Qt.CaseInsensitive)
        combo.setCompleter(completer)
        self.drop_table.setCellWidget(r, 0, combo)

        self.drop_table.setItem(r, 1, QTableWidgetItem("1"))
        self.drop_table.setItem(r, 2, QTableWidgetItem("1"))
        self.drop_table.setItem(r, 3, QTableWidgetItem("1"))

    def _remove_drop_row(self):
        rows = sorted({i.row() for i in self.drop_table.selectedIndexes()}, reverse=True)
        for r in rows: self.drop_table.removeRow(r)

    # ---------------- Toolbar actions ----------------
    def _on_new(self):
        base = "enemy"
        i = 1
        ids = {e.get("id") for e in self.enemies}
        while f"{base}_{i}" in ids: i += 1
        e = {
            "id": f"{base}_{i}", "name": f"Enemy {i}",
            "tier": "standard", "element": "none",
            "hp": 30, "speed": 20, "attack": 10,
            "strength":0,"vitality":0,"agility":0,"focus":0,"luck":0,
            "xp_reward":0,"ap_reward":0,"credit_reward":0,
            "sprite": [], "portrait":"", "abilities": [],
            "resistances": {}, "drops": []
        }
        self.enemies.append(e)
        self._reload_list()
        self._select_id(e["id"])

    def _on_duplicate(self):
        if self.current_idx is None: return
        src = json.loads(json.dumps(self.enemies[self.current_idx]))
        base = f"{src.get('id','enemy')}_copy"
        ids = {e.get("id") for e in self.enemies}
        i = 1; new_id = base
        while new_id in ids: i += 1; new_id = f"{base}{i}"
        src["id"] = new_id
        self.enemies.append(src)
        self._reload_list()
        self._select_id(new_id)

    def _on_delete(self):
        if self.current_idx is None: return
        eid = self.enemies[self.current_idx].get("id","(untitled)")
        if QMessageBox.question(self, "Delete", f"Delete enemy '{eid}'?",
                                QMessageBox.Yes|QMessageBox.No) == QMessageBox.Yes:
            del self.enemies[self.current_idx]
            self.current_idx = None
            self._reload_list()

    def _on_validate_click(self):
        self._save_current_to_memory()
        issues = self.validate()
        if not issues:
            QMessageBox.information(self, "OK", "No issues found.")
        else:
            QMessageBox.warning(self, "Validation", "• " + "\n• ".join(issues))

    def _save_all(self) -> bool:
        self._save_current_to_memory()
        out = sorted(self.enemies, key=lambda d: (d.get("id") or ""))
        ok = write_json_with_backup(self.enemies_path, out)
        if ok:
            QMessageBox.information(self, "Saved", "enemies.json saved.")
        else:
            QMessageBox.critical(self, "Save error", "Failed to save enemies.json.")
        return ok

    # ---------------- util ----------------
    def _index_of_id(self, eid: str) -> int:
        for i, e in enumerate(self.enemies):
            if e.get("id") == eid: return i
        return 0

    def _select_id(self, eid: str):
        for i in range(self.list.count()):
            if self.list.item(i).text() == eid:
                self.list.setCurrentRow(i)
                break

# ----- Entrypoint -----
def run(project_root: Optional[str] = None):
    app = QApplication(sys.argv)
    w = EnemyEditor(project_root)
    w.resize(1200, 720)
    w.show()
    sys.exit(app.exec_())

if __name__ == "__main__":
    run(sys.argv[1] if len(sys.argv) > 1 else None)
