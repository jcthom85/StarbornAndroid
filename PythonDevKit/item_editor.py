#!/usr/bin/env python3
"""
Starborn — Item Editor (v4.0, PyQt5)

This version replaces the free-form JSON text fields with a fully structured,
type-aware UI for all major item categories.

New Features:
• Type-specific tabs for "Consumable" and "Equippable" properties.
• **Consumable Editor**: Structured fields for healing, damage, buffs, and status cures.
• **Equippable Editor**:
    - Dedicated fields for weapon/armor stats (damage, defense, crit, etc.).
    - Tables for defining stat bonuses and equipment requirements.
    - A structured editor for "on-hit" effects.
    - A full "Resistances" tab with sliders for elemental and status resists.
• **Core Properties**: Added fields for rarity, weight, repair cost, icon/audio hooks, and tags.
• **Data Safety**: Auto-sanitizes data on save (e.g., ensures healing values are positive).
• **Validation**: The validation logic is updated to check all new structured fields.

Usage:
    pip install PyQt5
    python tools/item_editor.py  [optional_project_root]
"""

from __future__ import annotations
import os, sys, json, shutil, re
from pathlib import Path
from typing import Any, Dict, List, Optional

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (
    QApplication, QWidget, QSplitter, QHBoxLayout, QVBoxLayout, QTabWidget,
    QListWidget, QListWidgetItem, QLineEdit, QPushButton, QComboBox, QLabel,
    QFormLayout, QTextEdit, QMessageBox, QSpinBox, QToolButton, QGroupBox,
    QHeaderView, QAbstractItemView, QDialog, QDialogButtonBox, QTableWidget, QTableWidgetItem, QCheckBox,
    QFileDialog, QSlider, QDoubleSpinBox, QGridLayout, QInputDialog
)
from theme_kit import ThemeManager         # optional if you want per-editor theme flips
from data_core import json_load, json_save, unique_id
from devkit_paths import resolve_paths
from editor_undo import UndoManager
from ui_common import attach_status_bar, flash_status, attach_hotkeys, attach_list_context_menu, mark_invalid, clear_invalid
from editor_bus import goto as studio_goto, refresh_references as studio_refresh

# -----------------------------
# Config / enums
# -----------------------------
ITEM_TYPES = [
    "consumable", "equippable",
    "weapon", "armor", "fishing_rod", "tool",
    "ingredient", "component", "key_item",
    "junk", "summon", "misc"
]
EQUIP_LIKE_TYPES = {"equippable", "weapon", "armor", "fishing_rod", "tool"}

CONSUMABLE_TARGETS = ["self", "ally", "party", "enemy", "all_enemies", "any"]
STATUS_EFFECTS = ["burn", "freeze", "shock", "poison", "radiation", "psychic", "void", "weak", "regen", "shield"]
CURE_STATUS_CHOICES = ["", "all"] + STATUS_EFFECTS + ["custom…"]

STAT_CHOICES = ["", "hp", "max_hp", "rp", "max_rp", "atk", "def", "spd", "strength", "vitality", "agility", "focus", "luck", "accuracy", "evasion", "crit_rate"]

EQUIP_SLOTS  = ["weapon", "armor", "accessory"]
WEAPON_TYPES = ["blade", "pistol", "rifle", "launcher", "staff", "unarmed", "rod", "other"]
RARITY_LEVELS = ["", "common", "uncommon", "rare", "epic", "legendary", "mythic"]

def _sanitize_id(s: str) -> str:
    if not s: return ""
    return re.sub(r'[^a-z0-9_]+', '_', s.strip().lower())


# -----------------------------
# Root + JSON I/O helpers
# -----------------------------
def _find_root(start: Optional[Path] = None) -> Path:
    """Resolve the assets directory containing items.json."""
    return resolve_paths(start or Path(__file__).parent).assets_dir

def _read_list_json(path: Path, title: str = "") -> List[dict]:
    if not path.exists():
        return []
    try:
        with path.open("r", encoding="utf-8") as f:
            data = json.load(f)
        if isinstance(data, list):
            return data
        QMessageBox.critical(None, f"Load error {title}", f"{path.name} must be a JSON array.")
        return []
    except Exception as e:
        QMessageBox.critical(None, f"Load error {title}", f"Failed to load {path.name}:\n{e}")
        return []

def _write_json_with_backup(path: Path, data: Any, title: str) -> bool:
    """Pretty-print and backup existing file to .bak before writing."""
    try:
        if path.exists():
            shutil.copy2(path, path.with_suffix(path.suffix + ".bak"))
        with path.open("w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=4)
        return True
    except Exception as e:
        QMessageBox.critical(None, f"Save error ({title})", f"Failed to save {path.name}:\n{e}")
        return False

def _clean_empty(x: Any) -> Any:
    """Remove empty dict/list/str/None recursively."""
    if isinstance(x, dict):
        return {k: _clean_empty(v) for k, v in x.items() if v not in ({}, [], "", None)}
    if isinstance(x, list):
        return [_clean_empty(v) for v in x if v not in ({}, [], "", None)]
    return x

# -----------------------------
# Small widgets
# -----------------------------
class DictEditor(QWidget):
    """Two-column key/value editor. Supports dropdown for keys."""
    def __init__(self, title: str, value_is_int: bool = False, key_choices: Optional[List[str]] = None):
        super().__init__()
        self.value_is_int = value_is_int
        self.key_choices = key_choices
        v = QVBoxLayout(self)
        if title: v.addWidget(QLabel(title))
        self.table = QTableWidget(0, 2)
        self.table.setHorizontalHeaderLabels(["Key", "Value"])
        self.table.horizontalHeader().setSectionResizeMode(0, QHeaderView.Stretch)
        self.table.horizontalHeader().setSectionResizeMode(1, QHeaderView.ResizeToContents)
        self.table.verticalHeader().setVisible(False)
        self.table.setSelectionBehavior(QAbstractItemView.SelectRows)
        v.addWidget(self.table, 1)
        row = QHBoxLayout()
        self.add_btn = QToolButton(); self.add_btn.setText("+")
        self.del_btn = QToolButton(); self.del_btn.setText("–")
        row.addWidget(self.add_btn); row.addWidget(self.del_btn); row.addStretch(1)
        v.addLayout(row)
        self.add_btn.clicked.connect(self._add)
        self.del_btn.clicked.connect(self._del)


    def _add(self):
        r = self.table.rowCount()
        self.table.insertRow(r)
        if self.key_choices:
            combo = QComboBox()
            combo.addItems(self.key_choices)
            self.table.setCellWidget(r, 0, combo)
        else:
            self.table.setItem(r, 0, QTableWidgetItem(""))
        self.table.setItem(r, 1, QTableWidgetItem("0" if self.value_is_int else ""))

    def _del(self):
        r = self.table.currentRow()
        if r >= 0: self.table.removeRow(r)

    def set_dict(self, data: dict | None):
        self.table.setRowCount(0)
        for k, val in (data or {}).items():
            r = self.table.rowCount()
            self.table.insertRow(r)
            if self.key_choices:
                combo = QComboBox()
                combo.addItems(self.key_choices)
                combo.setCurrentText(str(k))
                self.table.setCellWidget(r, 0, combo)
            else:
                self.table.setItem(r, 0, QTableWidgetItem(str(k)))
            self.table.setItem(r, 1, QTableWidgetItem(str(val)))

    def get_dict(self) -> dict:
        out: dict[str, Any] = {}
        for r in range(self.table.rowCount()):
            w = self.table.cellWidget(r, 0)
            if isinstance(w, QComboBox):
                key = w.currentText()
            else:
                k = self.table.item(r,0)
                key = (k.text() or "").strip() if k else ""
            v = self.table.item(r,1)
            if not key: continue
            raw = (v.text() if v else "").strip()
            out[key] = int(raw) if self.value_is_int and raw else (raw if raw != "" else 0)
        return out

class ListEditor(QWidget):
    """List[str] with add/remove/up/down controls."""
    def __init__(self, title: str = "", placeholder: str = "value"):
        super().__init__()
        v = QVBoxLayout(self)
        if title: v.addWidget(QLabel(title))
        self.list = QListWidget()
        self.list.setSelectionMode(QAbstractItemView.SingleSelection)
        # Allow items to be edited in-place
        self.list.setEditTriggers(QAbstractItemView.DoubleClicked | QAbstractItemView.SelectedClicked)
        v.addWidget(self.list, 1)
        row = QHBoxLayout()
        self.add_btn = QToolButton(); self.add_btn.setText("+")
        self.del_btn = QToolButton(); self.del_btn.setText("–")
        self.up_btn  = QToolButton(); self.up_btn.setText("↑")
        self.down_btn= QToolButton(); self.down_btn.setText("↓")
        row.addWidget(self.add_btn); row.addWidget(self.del_btn)
        row.addWidget(self.up_btn);  row.addWidget(self.down_btn); row.addStretch(1)
        v.addLayout(row)
        self.placeholder = placeholder
        self.add_btn.clicked.connect(self._add)
        self.del_btn.clicked.connect(self._del)
        self.up_btn.clicked.connect(self._up)
        self.down_btn.clicked.connect(self._down)

    def _add(self):
        item = QListWidgetItem(self.placeholder, self.list)
        item.setFlags(item.flags() | Qt.ItemIsEditable)
        self.list.editItem(item) # Immediately start editing the new item

    def _del(self):
        r = self.list.currentRow()
        if r >= 0: self.list.takeItem(r)

    def _up(self):
        r = self.list.currentRow()
        if r <= 0: return
        it = self.list.takeItem(r)
        self.list.insertItem(r-1, it); self.list.setCurrentRow(r-1)

    def _down(self):
        r = self.list.currentRow()
        if r < 0 or r >= self.list.count()-1: return
        it = self.list.takeItem(r)
        self.list.insertItem(r+1, it); self.list.setCurrentRow(r+1)

    def set_list(self, values: List[str] | None):
        self.list.clear()
        for v in (values or []):
            item = QListWidgetItem(str(v), self.list)
            # Make existing items editable
            item.setFlags(item.flags() | Qt.ItemIsEditable)

    def get_list(self) -> List[str]:
        return [(self.list.item(i).text() or "").strip() for i in range(self.list.count()) if (self.list.item(i).text() or "").strip()]


# -----------------------------
# Type-specific editors
# -----------------------------
class ConsumableEditor(QWidget):
    def __init__(self, parent_editor: "ItemEditor"):
        super().__init__()
        self.parent_editor = parent_editor
        form = QFormLayout(self)
        self.target = QComboBox(); self.target.setEditable(True)
        self.target.addItems(CONSUMABLE_TARGETS)
        form.addRow("effect.target", self.target)

        self.restore_hp = QSpinBox(); self.restore_hp.setRange(0, 99999)
        self.restore_rp = QSpinBox(); self.restore_rp.setRange(0, 99999)
        self.damage     = QSpinBox(); self.damage.setRange(0, 99999)
        form.addRow("effect.restore_hp", self.restore_hp)
        form.addRow("effect.restore_rp", self.restore_rp)

        form.addRow("effect.damage",     self.damage)

        self.cure_status = QComboBox(); self.cure_status.setEditable(True)
        self.cure_status.addItems(CURE_STATUS_CHOICES)
        form.addRow("effect.cure_status", self.cure_status)

        # --- NEW: Buffs table for multiple stat buffs ---
        buffs_box = QGroupBox("effect.buffs (optional)")
        buffs_layout = QVBoxLayout(buffs_box)
        self.buffs_table = QTableWidget(0, 3)
        self.buffs_table.setHorizontalHeaderLabels(["Stat", "Value", "Duration (battles)"])
        self.buffs_table.horizontalHeader().setSectionResizeMode(0, QHeaderView.Stretch)
        self.buffs_table.horizontalHeader().setSectionResizeMode(1, QHeaderView.ResizeToContents)
        self.buffs_table.horizontalHeader().setSectionResizeMode(2, QHeaderView.ResizeToContents)
        self.buffs_table.setSelectionBehavior(QAbstractItemView.SelectRows)
        buffs_layout.addWidget(self.buffs_table)

        buff_btns = QHBoxLayout()
        add_buff_btn = QPushButton("Add Buff")
        add_buff_btn.setToolTip("Add a new stat buff to this consumable.")
        del_buff_btn = QPushButton("Remove Buff")
        del_buff_btn.setToolTip("Remove the selected buff from the table.")
        add_buff_btn.clicked.connect(lambda: self._add_buff_row())
        del_buff_btn.clicked.connect(lambda: self._remove_buff_row())
        self.buffs_table.itemChanged.connect(self.parent_editor._apply_to_current)
        buff_btns.addWidget(add_buff_btn)
        buff_btns.addWidget(del_buff_btn)
        buffs_layout.addLayout(buff_btns)
        form.addRow(buffs_box)
        # --- END NEW ---

        from PyQt5.QtWidgets import QLineEdit
        self.utility = QLineEdit()
        form.addRow("effect.utility", self.utility)

    def _add_buff_row(self, stat: str = "", value: int = 0, duration: int = 1, from_load: bool = False):
        """Adds a row to the buffs table. If not from loading, triggers a save."""
        # Block signals during programmatic add so the editor doesn't reload immediately
        previously_blocked = self.buffs_table.blockSignals(True)

        row = self.buffs_table.rowCount()
        self.buffs_table.insertRow(row)

        combo = QComboBox()
        combo.addItems(STAT_CHOICES)
        combo.setCurrentText(str(stat))
        combo.currentTextChanged.connect(self._on_buff_combo_changed)
        self.buffs_table.setCellWidget(row, 0, combo)

        self.buffs_table.setItem(row, 1, QTableWidgetItem(str(value)))
        self.buffs_table.setItem(row, 2, QTableWidgetItem(str(duration)))

        self.buffs_table.blockSignals(previously_blocked)

    def _remove_buff_row(self):
        rows = sorted({i.row() for i in self.buffs_table.selectedIndexes()}, reverse=True)
        for r in rows:
            self.buffs_table.removeRow(r)
        # Manually trigger apply *after* rows are removed
        if rows:
            if not self.parent_editor._loading: self.parent_editor._apply_to_current()

    def _on_buff_combo_changed(self, _text: str):
        if not self.parent_editor._loading:
            self.parent_editor._apply_to_current()

    def set_value(self, effect: dict | None):
        e = effect or {}
        self.target.setEditText(e.get("target", ""))
        try: self.restore_hp.setValue(abs(int(e.get("restore_hp", e.get("restore", 0)))))
        except Exception: self.restore_hp.setValue(0)
        try: self.restore_rp.setValue(abs(int(e.get("restore_rp", 0))))
        except Exception: self.restore_rp.setValue(0)
        try: self.damage.setValue(abs(int(e.get("damage", 0))))
        except Exception: self.damage.setValue(0)
        self.cure_status.setEditText(e.get("cure_status",""))

        # Load buffs into the table
        self.buffs_table.setRowCount(0)
        buffs = e.get("buffs", [])
        if isinstance(buffs, list):
            for buff in buffs:
                if isinstance(buff, dict):
                    self._add_buff_row(
                        buff.get("stat", ""),
                        buff.get("value", 0),
                        buff.get("duration", 1),
                        from_load=True
                    )

        self.utility.setText(e.get("utility",""))

    def get_value(self) -> dict:
        out = {
            "target": self.target.currentText().strip(),
            "restore_hp": int(self.restore_hp.value()),
            "restore_rp": int(self.restore_rp.value()),
            "damage": int(self.damage.value()),
        }
        cur = (self.cure_status.currentText() or "").strip()
        if cur == "custom…":
            pass
        elif cur:
            out["cure_status"] = cur

        util = (self.utility.text() or "").strip()
        if util: out["utility"] = util

        # Collect buffs from the table
        buffs = []
        for r in range(self.buffs_table.rowCount()):
            w = self.buffs_table.cellWidget(r, 0)
            stat = w.currentText().strip() if isinstance(w, QComboBox) else ""
            try: value = int(self.buffs_table.item(r, 1).text() if self.buffs_table.item(r, 1) else "0")
            except ValueError: value = 0
            try: duration = int(self.buffs_table.item(r, 2).text() if self.buffs_table.item(r, 2) else "1")
            except ValueError: duration = 1

            if stat and duration > 0:
                buffs.append({"stat": stat, "value": value, "duration": duration})
        if buffs:
            out["buffs"] = buffs

        return _clean_empty(out)


class EquippableEditor(QWidget):
    def __init__(self, parent_editor: "ItemEditor"):
        super().__init__()
        self.parent_editor = parent_editor
        v = QVBoxLayout(self)

        tabs = QTabWidget()
        v.addWidget(tabs)

        # --- Main Tab ---
        main_tab = QWidget(); main_layout = QVBoxLayout(main_tab)
        tabs.addTab(main_tab, "Main")

        eq_box = QGroupBox("Equipment"); eq_form = QFormLayout(eq_box)
        self.slot = QComboBox(); self.slot.setEditable(True); self.slot.addItems(EQUIP_SLOTS)
        self.weapon_type = QComboBox(); self.weapon_type.setEditable(True); self.weapon_type.addItems(WEAPON_TYPES)
        eq_form.addRow("equipment.slot", self.slot)
        eq_form.addRow("equipment.weapon_type", self.weapon_type)
        main_layout.addWidget(eq_box)

        # --- Weapon/Armor Stats ---
        stats_box = QGroupBox("Combat Stats"); stats_form = QFormLayout(stats_box)
        self.dmg_min = QSpinBox(); self.dmg_min.setRange(0, 9999)
        self.dmg_max = QSpinBox(); self.dmg_max.setRange(0, 9999)
        self.speed = QSpinBox(); self.speed.setRange(-100, 100)
        self.crit_rate = QDoubleSpinBox(); self.crit_rate.setRange(0, 100); self.crit_rate.setSuffix("%")
        self.accuracy = QSpinBox(); self.accuracy.setRange(0, 200)
        self.defense = QSpinBox(); self.defense.setRange(0, 999)
        self.block = QDoubleSpinBox(); self.block.setRange(0, 100); self.block.setSuffix("%")
        stats_form.addRow("Damage (Min/Max)", self._hbox(self.dmg_min, self.dmg_max))
        stats_form.addRow("Speed Bonus", self.speed)
        stats_form.addRow("Crit Rate Bonus", self.crit_rate)
        stats_form.addRow("Accuracy Bonus", self.accuracy)
        stats_form.addRow("Defense", self.defense)
        stats_form.addRow("Block Chance", self.block)
        main_layout.addWidget(stats_box)

        # --- Stat Bonuses & Requirements ---
        row = QHBoxLayout()
        self.stats = DictEditor("equipment.stats (key->int)", value_is_int=True, key_choices=STAT_CHOICES)
        self.reqs = DictEditor("requirements (stat->value)", value_is_int=True, key_choices=STAT_CHOICES)
        row.addWidget(self.stats); row.addWidget(self.reqs)
        main_layout.addLayout(row)

        # --- On-Hit Effects ---
        on_hit_box = QGroupBox("On-Hit Effect"); on_hit_form = QFormLayout(on_hit_box)
        self.on_hit_chance = QDoubleSpinBox(); self.on_hit_chance.setRange(0, 1); self.on_hit_chance.setSingleStep(0.05)
        self.on_hit_status = QComboBox(); self.on_hit_status.setEditable(True); self.on_hit_status.addItems([""] + STATUS_EFFECTS)
        self.on_hit_duration = QSpinBox(); self.on_hit_duration.setRange(0, 99)
        on_hit_form.addRow("Chance (0-1)", self.on_hit_chance)
        on_hit_form.addRow("Inflict Status", self.on_hit_status)
        on_hit_form.addRow("Duration (turns)", self.on_hit_duration)
        main_layout.addWidget(on_hit_box)

        # --- Resistances Tab ---
        resist_tab = QWidget(); resist_layout = QVBoxLayout(resist_tab)
        tabs.addTab(resist_tab, "Resistances")
        self.resist_sliders: Dict[str, QSlider] = {}
        self.resist_labels: Dict[str, QLabel] = {}
        
        grid = QGridLayout()
        all_resists = ["fire", "ice", "lightning", "poison", "radiation", "psychic", "void"] + STATUS_EFFECTS
        for i, res_name in enumerate(all_resists):
            row, col = i % ((len(all_resists) + 1) // 2), i // ((len(all_resists) + 1) // 2)
            
            label = QLabel(f"{res_name.title()}:")
            slider = QSlider(Qt.Horizontal); slider.setRange(-100, 100); slider.setValue(0)
            val_label = QLabel("0%")
            slider.valueChanged.connect(lambda v, l=val_label: l.setText(f"{v}%"))
            
            self.resist_sliders[res_name] = slider
            self.resist_labels[res_name] = val_label
            
            grid.addWidget(label, row, col * 3)
            grid.addWidget(slider, row, col * 3 + 1)
            grid.addWidget(val_label, row, col * 3 + 2)
        resist_layout.addLayout(grid)

        # --- Mod Slots & Scrap ---
        mod_scrap_tab = QWidget(); ms_layout = QHBoxLayout(mod_scrap_tab)
        tabs.addTab(mod_scrap_tab, "Mods & Scrap")
        self.mod_slots = ListEditor("mod_slots", "slot_id")
        self.scrap     = DictEditor("scrap_yield (component->qty)", value_is_int=True)
        ms_layout.addWidget(self.mod_slots, 1)
        ms_layout.addWidget(self.scrap, 1)

    def _hbox(self, *widgets):
        h = QHBoxLayout()
        for w in widgets:
            h.addWidget(w)
        return h

    def set_value(self, equipment: dict | None, mod_slots: List[str] | None, scrap_yield: dict | None):
        eq = equipment or {}
        self.slot.setEditText(eq.get("slot",""))
        self.weapon_type.setEditText(eq.get("weapon_type",""))
        self.stats.set_dict(eq.get("stats", {}))

        # Weapon/Armor stats
        self.dmg_min.setValue(int(eq.get("damage_min", 0)))
        self.dmg_max.setValue(int(eq.get("damage_max", 0)))
        self.speed.setValue(int(eq.get("speed", 0)))
        self.crit_rate.setValue(float(eq.get("crit_rate", 0.0)))
        self.accuracy.setValue(int(eq.get("accuracy", 0)))
        self.defense.setValue(int(eq.get("defense", 0)))
        self.block.setValue(float(eq.get("block_chance", 0.0)))

        # Requirements
        self.reqs.set_dict(eq.get("requirements", {}))

        # On-hit
        on_hit = eq.get("on_hit", {})
        self.on_hit_chance.setValue(float(on_hit.get("chance", 0.0)))
        self.on_hit_status.setEditText(on_hit.get("status", ""))
        self.on_hit_duration.setValue(int(on_hit.get("duration", 0)))

        # Resistances
        resists = eq.get("resistances", {})
        for key, slider in self.resist_sliders.items():
            val = int(resists.get(key, 0))
            slider.setValue(val)
            self.resist_labels[key].setText(f"{val}%")

        self.mod_slots.set_list(mod_slots or [])
        self.scrap.set_dict(scrap_yield or {})

    def get_value(self) -> tuple[dict, List[str], dict]:
        equipment = {
            "slot": self.slot.currentText().strip(),
            "weapon_type": self.weapon_type.currentText().strip()
        }
        # Combat stats
        for w, k in [(self.dmg_min, "damage_min"), (self.dmg_max, "damage_max"), (self.speed, "speed"),
                     (self.crit_rate, "crit_rate"), (self.accuracy, "accuracy"), (self.defense, "defense"),
                     (self.block, "block_chance")]:
            if w.value() != 0: equipment[k] = w.value()

        equipment["stats"] = self.stats.get_dict()
        equipment["requirements"] = self.reqs.get_dict()

        if self.on_hit_chance.value() > 0 and self.on_hit_status.currentText():
            equipment["on_hit"] = {
                "chance": self.on_hit_chance.value(),
                "status": self.on_hit_status.currentText(),
                "duration": self.on_hit_duration.value()
            }
        equipment["resistances"] = {k: s.value() for k, s in self.resist_sliders.items() if s.value() != 0}
        return (_clean_empty(equipment), self.mod_slots.get_list(), self.scrap.get_dict())

# -----------------------------
# ItemEditor (main)
# -----------------------------
class ItemEditor(QWidget):
    def __init__(self, project_root: Optional[str] = None):
        super().__init__()
        self.setWindowTitle("Starborn — Items")
        self.root = _find_root(Path(project_root) if project_root else None)
        self.items: List[Dict[str, Any]] = _read_list_json(self.root / "items.json", "items")
        self.current: Optional[Dict[str, Any]] = None
        self._loading = False
        self._last_filter = ""

        # sanitize once on load (non-destructive; only adjusts negatives/legacy restore)
        self._sanitize_all()

        self._build_ui()
        self._refresh_type_filter()
        self._reload_list()

    # ---------- Paths ----------
    @property
    def items_path(self) -> Path: return self.root / "items.json"

    # ---------- UI ----------
    def _build_ui(self):
        split = QSplitter(Qt.Horizontal, self)
        root = QHBoxLayout(self); root.addWidget(split)

        # LEFT: search + filter + list + CRUD + file ops
        left = QWidget(); l = QVBoxLayout(left)

        rowf = QHBoxLayout()
        self.search = QLineEdit(); self.search.setPlaceholderText("Search by name/desc/type…")
        self.type_filter = QComboBox(); self.type_filter.addItems(["All"] + ITEM_TYPES)
        self.type_filter.currentIndexChanged.connect(self._reload_list)
        self.search.textChanged.connect(self._reload_list)
        rowf.addWidget(self.search, 2); rowf.addWidget(self.type_filter, 1)
        l.addLayout(rowf)

        self.list = QListWidget()
        self.list.itemSelectionChanged.connect(self._on_select)
        l.addWidget(self.list, 1)

        row = QHBoxLayout()
        b_new = QPushButton("New"); b_dup = QPushButton("Duplicate"); b_del = QPushButton("Delete")
        b_ren = QPushButton("Rename…")
        b_new.clicked.connect(self._on_new)
        b_dup.clicked.connect(self._on_duplicate)
        b_del.clicked.connect(self._on_delete)
        b_ren.clicked.connect(self._on_rename)
        for b in (b_new, b_dup, b_del, b_ren): row.addWidget(b)
        l.addLayout(row)

        row2 = QHBoxLayout()
        b_val = QPushButton("Validate"); b_save = QPushButton("Save")
        b_imp = QPushButton("Import…"); b_exp = QPushButton("Export selection…")
        b_val.clicked.connect(self._on_validate)
        b_save.clicked.connect(self._on_save)
        b_imp.clicked.connect(self._on_import)
        b_exp.clicked.connect(self._on_export_selected)
        row2.addWidget(b_val); row2.addWidget(b_save)
        row2.addStretch(1)
        row2.addWidget(b_imp); row2.addWidget(b_exp)
        l.addLayout(row2)

        split.addWidget(left)

        # RIGHT: tabs -> Form / Raw / Where Used
        self.tabs = QTabWidget()
        split.addWidget(self.tabs)
        split.setStretchFactor(1, 3)

        # ---- FORM TAB (with scroll area) ----
        self.tab_form = QWidget()
        from PyQt5.QtWidgets import QScrollArea
        scroll_area = QScrollArea()
        scroll_area.setWidgetResizable(True)
        scroll_area.setFrameShape(QScrollArea.NoFrame)

        scroll_content = QWidget()
        scroll_area.setWidget(scroll_content)

        fp = QVBoxLayout(scroll_content) # The form layout now goes inside the scroll content

        # Common group
        g = QGroupBox("Common")
        form = QFormLayout(g)
        self.id_label  = QLabel("-")
        self.name_edit = QLineEdit()
        self.aliases   = ListEditor("Aliases", "alias")
        self.desc_edit = QTextEdit(); self.desc_edit.setPlaceholderText("Description…")
        self.type_combo = QComboBox(); self.type_combo.setEditable(True); self.type_combo.addItems(ITEM_TYPES)
        self.value_spin = QSpinBox(); self.value_spin.setRange(0, 9999999)
        self.buy_spin   = QSpinBox(); self.buy_spin.setRange(0, 9999999)

        self.unsellable = QCheckBox("Cannot be sold")
        # --- NEW: Add the missing widgets from the feature request ---
        self.rarity = QComboBox(); self.rarity.addItems(RARITY_LEVELS)
        self.icon = QLineEdit()
        self.swing_sfx = QLineEdit()
        self.hit_sfx = QLineEdit()
        self.tags = QLineEdit(); self.tags.setPlaceholderText("e.g. two_handed, unique")
        # -------------------------------------------------------------

        form.addRow(QLabel("<b>name</b>:"), self.name_edit)
        form.addRow("type:", self.type_combo)
        form.addRow("value (sell):", self.value_spin)
        form.addRow("buy_price:", self.buy_spin)
        form.addRow("", self.unsellable)
        fp.addWidget(g)
        form.addRow("rarity:", self.rarity)
        self.type_combo.currentTextChanged.connect(self._on_type_changed)
        form.addRow("Icon:", self.icon)
        form.addRow("swing_sfx:", self.swing_sfx)
        form.addRow("hit_sfx:", self.hit_sfx)
        form.addRow("tags:", self.tags)

        fp.addWidget(self.desc_edit, 1)

        fp.addWidget(self.aliases)

        # Type-specific stack (simple tabs)
        self.type_tabs = QTabWidget()
        self.page_consumable = ConsumableEditor(self)
        self.page_equippable = EquippableEditor(self)
        self.type_tabs.addTab(self.page_consumable, "Consumable")
        self.type_tabs.addTab(self.page_equippable, "Equippable")
        fp.addWidget(self.type_tabs)

        # Apply button
        b_apply = QPushButton("Apply changes to current item")
        b_apply.clicked.connect(self._apply_to_current)
        fp.addWidget(b_apply)

        # Add the scroll area to the main form tab
        form_tab_layout = QVBoxLayout(self.tab_form)
        form_tab_layout.setContentsMargins(0,0,0,0)
        form_tab_layout.addWidget(scroll_area)

        self.tabs.addTab(self.tab_form, "Form")

        # ---- RAW TAB
        self.tab_raw = QWidget(); vr = QVBoxLayout(self.tab_raw); vr.setContentsMargins(0,0,0,0)
        self.raw = QTextEdit(); self.raw.setPlaceholderText("Raw JSON for this item.")
        vr.addWidget(self.raw, 1)
        rrow = QHBoxLayout()
        b_from = QPushButton("Format from Fields"); b_to = QPushButton("Apply to Fields")
        b_from.clicked.connect(self._raw_from_fields)
        b_to.clicked.connect(self._raw_to_fields)
        rrow.addWidget(b_from); rrow.addWidget(b_to); rrow.addStretch(1)
        vr.addLayout(rrow)
        self.tabs.addTab(self.tab_raw, "Raw JSON")

        # ---- WHERE USED TAB (now inside a scroll area)
        self.tab_used = QWidget(); vu = QVBoxLayout(self.tab_used); vu.setContentsMargins(0,0,0,0)
        from PyQt5.QtWidgets import QScrollArea
        scroll = QScrollArea(); scroll.setWidgetResizable(True)
        vu.addWidget(scroll)
        scroll_content = QWidget(); scroll.setWidget(scroll_content); s_layout = QVBoxLayout(scroll_content)
        self.used_list = QListWidget()
        vu.addWidget(QLabel("References across project"))
        vu.addWidget(self.used_list, 1)
        self.tabs.addTab(self.tab_used, "Where Used")

    # ---------- List & selection ----------
    def _refresh_type_filter(self):
        self.type_filter.blockSignals(True)
        self.type_filter.clear()
        self.type_filter.addItems(["All"] + ITEM_TYPES)
        self.type_filter.blockSignals(False)

    def _row_text(self, it: dict) -> str:
        t = it.get("type", "")
        v = it.get("value", 0)
        return f"{it.get('name','<name>')} — {t} — value:{v}"

    def _reload_list(self, select_name: Optional[str] = None):
        self.list.clear()
        q = (self.search.text() or "").strip().lower()
        tf = self.type_filter.currentText()
        for i, e in enumerate(sorted(self.items, key=lambda d: (d.get("name") or ""))):
            nm = (e.get("name") or "")
            if q and (q not in nm.lower() and q not in (e.get("description","").lower())):
                continue
            if tf != "All" and (e.get("type") != tf):
                continue
            QListWidgetItem(self._row_text(e), self.list)
        if select_name:
            self._reselect(select_name)
        elif self.list.count() and self.list.currentRow() < 0:
            self.list.setCurrentRow(0)

    def _get_selected(self) -> Optional[dict]:
        r = self.list.currentRow()
        if r < 0 or r >= len(self.items): return None
        # safer: re-find by name prefix
        txt = self.list.item(r).text()
        name = txt.split(" — ", 1)[0]
        for it in self.items:
            if it.get("name") == name:
                return it
        return None

    def _on_select(self):
        self.current = self._get_selected()
        self._load_current()

    # ---------- Load/collect ----------
    def _load_current(self):
        self._loading = True
        try:
            it = self.current
            if not it:
                self.id_label.setText("-")
                self.name_edit.setText("")
                self.aliases.set_list([])
                self.desc_edit.setPlainText("")
                self.type_combo.setEditText("")
                self.value_spin.setValue(0)
                self.buy_spin.setValue(0)
                self.page_consumable.set_value(None)
                self.page_equippable.set_value(None, None, None)
                self.raw.setPlainText("{}")
                self.used_list.clear()
                return
            self.name_edit.setText(it.get("name",""))
            self.aliases.set_list(it.get("aliases", []))

            item_type = it.get("type", "")
            is_key_item = (item_type == "key_item")
            self.unsellable.setChecked(it.get("unsellable", False) or is_key_item)
            self.unsellable.setEnabled(not is_key_item)

            self.desc_edit.setPlainText(it.get("description",""))
            self.type_combo.setEditText(it.get("type",""))
            self.value_spin.setValue(int(it.get("value", 0)))
            self.buy_spin.setValue(int(it.get("buy_price", 0)))
            self.rarity.setCurrentText(it.get("rarity", ""))
            self.icon.setText(it.get("icon", ""))
            self.swing_sfx.setText(it.get("swing_sfx", ""))
            self.hit_sfx.setText(it.get("hit_sfx", ""))
            tags = it.get("tags", [])
            self.tags.setText(", ".join(tags) if isinstance(tags, list) else "")

            # type-specific fields
            t = item_type
            if t == "consumable":
                self.type_tabs.setCurrentWidget(self.page_consumable)
            elif t in EQUIP_LIKE_TYPES:
                self.type_tabs.setCurrentWidget(self.page_equippable)
            # load (both set so swapping types doesn't drop data)
            self.page_consumable.set_value(it.get("effect"))
            self.page_equippable.set_value(it.get("equipment"), it.get("mod_slots"), it.get("scrap_yield"))

            # Raw tab
            self.raw.setPlainText(json.dumps(it, ensure_ascii=False, indent=4))

            # Where used
            self._refresh_where_used(it.get("name",""))

        finally:
            self._loading = False

    def _on_type_changed(self, new_type: str):
        if self._loading: return
        is_key_item = (new_type == "key_item")
        self.unsellable.setChecked(is_key_item or self.unsellable.isChecked())
        self.unsellable.setEnabled(not is_key_item)

    def _collect(self) -> dict:
        name = (self.name_edit.text() or "").strip()
        it = {
            "id": _sanitize_id(name), "name": name,
            "description": self.desc_edit.toPlainText(),
            "aliases": self.aliases.get_list(),
            "type": (self.type_combo.currentText() or "").strip(),
            "value": int(self.value_spin.value()) if self.value_spin.value() > 0 else 0,
            "buy_price": int(self.buy_spin.value()),
            "unsellable": self.unsellable.isChecked(),
            "rarity": self.rarity.currentText(),
            "icon": self.icon.text().strip(),
            "swing_sfx": self.swing_sfx.text().strip(),
            "hit_sfx": self.hit_sfx.text().strip(),
            "tags": [t.strip() for t in self.tags.text().split(",") if t.strip()],
        }
        # Type specifics
        t = it["type"]
        if t == "consumable":
            it["effect"] = self.page_consumable.get_value()
        elif t in EQUIP_LIKE_TYPES:
            eq, mod_slots, scrap = self.page_equippable.get_value()
            it["equipment"] = eq
            if mod_slots:   it["mod_slots"] = mod_slots
            if scrap:       it["scrap_yield"] = scrap
        else:
            pass # Other types have no special fields in this editor

        return _clean_empty(it)

    # ---------- Sanitize ----------
    def _sanitize_all(self):
        """Make legacy/dirty data safe in memory so validation doesn't bark."""
        for it in self.items or []:
            if (it.get("type") or "") == "consumable":
                eff = it.get("effect")
                if isinstance(eff, dict):
                    # normalize legacy 'restore' and ensure non-negative ints
                    if "restore" in eff and "restore_hp" not in eff:
                        eff["restore_hp"] = eff.pop("restore")
                    for key in ("restore_hp", "restore_rp", "damage"):
                        try:
                            eff[key] = abs(int(eff.get(key, 0) or 0))
                        except Exception:
                            eff[key] = 0

                    buff = eff.get("buff_stat")
                    if isinstance(buff, dict):
                        buff["stat"] = str(buff.get("stat", "")).strip()
                        try:
                            buff["value"] = int(buff.get("value", 0) or 0)
                        except Exception:
                            buff["value"] = 0
                        try:
                            buff["duration"] = max(0, int(buff.get("duration", 0) or 0))
                        except Exception:
                            buff["duration"] = 0
                        if not buff["stat"] or buff["duration"] <= 0:
                            eff.pop("buff_stat", None)

    # ---------- Apply/Save/Validate ----------
    def _apply_to_current(self):
        if not self.current: return
        new_data = self._collect()
        self.current.clear()
        self.current.update(new_data)
        # reflect to raw + list
        self.raw.setPlainText(json.dumps(self.current, ensure_ascii=False, indent=4))
        self._reload_list(select_name=self.current.get("name",""))
        self._reselect(self.current.get("name",""))
        self._refresh_where_used(self.current.get("name",""))

    def _on_save(self):
        # commit current row first
        if self.current:
            self.current.clear()
            self.current.update(self._collect())

        # sanitize all before validate/save so legacy negatives stop warning
        self._sanitize_all()

        issues = self._validate()
        if issues:
            QMessageBox.warning(self, "Items: Validate", "\n".join(issues[:200]))
            return
        out = sorted(self.items, key=lambda x: (x.get("name") or "").lower())
        ok = _write_json_with_backup(self.items_path, out, "items")
        if ok:
            flash_status(self, "items.json saved.")
            self._reload_list()

    def _on_validate(self):
        # sanitize first so the dialog reflects current rules
        self._sanitize_all()
        issues = self._validate()
        if issues:
            QMessageBox.warning(self, "Items: Validate", "\n".join(issues[:200]))
        else:
            QMessageBox.information(self, "Items: Validate", "No issues found.")

    def _validate(self) -> List[str]:
        issues: List[str] = []
        names_seen = set()
        for it in self.items:
            name = (it.get("name") or "").strip()
            if not name:
                issues.append("Item missing name.")
                continue
            if name in names_seen:
                issues.append(f"Duplicate name: {name}")
            names_seen.add(name)

            t = (it.get("type") or "").strip()
            if not t:
                issues.append(f"{name}: missing type")
            elif t not in ITEM_TYPES:
                issues.append(f"{name}: unknown type '{t}'")

            # value/buy_price
            try:
                v = int(it.get("value", 0)); b = int(it.get("buy_price", 0))
                if v < 0 or b < 0: # type: ignore
                    issues.append(f"{name}: value/buy_price cannot be negative")
            except Exception:
                issues.append(f"{name}: value/buy_price must be integers")

            # type-specific
            if t == "consumable":
                eff = it.get("effect", {})
                if not isinstance(eff, dict):
                    issues.append(f"{name}: effect must be an object")
                else:
                    # after sanitize these are non-negative; keep a guard anyway
                    try:
                        rhp = int(eff.get("restore_hp", 0) or 0)
                        rrp = int(eff.get("restore_rp", 0) or 0)
                        dmg = int(eff.get("damage", 0) or 0)
                        if rhp < 0 or rrp < 0 or dmg < 0:
                            issues.append(f"{name}: effect restore/damage cannot be negative")
                    except Exception:
                        issues.append(f"{name}: effect values must be integers")
                    buff = eff.get("buff_stat")
                    if buff is not None:
                        if not isinstance(buff, dict):
                            issues.append(f"{name}: effect.buff_stat must be an object")
                        else:
                            stat = (buff.get("stat") or "").strip()
                            if not stat:
                                issues.append(f"{name}: effect.buff_stat.stat is required")
                            for key in ("value", "duration"):
                                try:
                                    int(buff.get(key, 0) or 0)
                                except Exception:
                                    issues.append(f"{name}: effect.buff_stat.{key} must be int")
                            try:
                                if int(buff.get("duration", 0) or 0) <= 0:
                                    issues.append(f"{name}: effect.buff_stat.duration must be > 0")
                            except Exception:
                                pass
            elif t in EQUIP_LIKE_TYPES:
                eq = it.get("equipment", {})
                if not isinstance(eq, dict) or not (eq.get("slot") or "").strip():
                    issues.append(f"{name}: equipment.slot required for {t}")
                # stats must be ints
                stats = (eq.get("stats") or {})
                for k, v in stats.items():
                    try:
                        int(v)
                    except Exception:
                        issues.append(f"{name}: equipment.stats.{k} must be int")

                # mod_slots list -> strings
                m = it.get("mod_slots", [])
                if m is not None and not isinstance(m, list):
                    issues.append(f"{name}: mod_slots must be a list")

                # scrap_yield values -> ints >=0
                sy = it.get("scrap_yield", {})
                for comp, qty in sy.items():
                    try:
                        q = int(qty)
                        if q < 0: issues.append(f"{name}: scrap_yield.{comp} cannot be negative")
                    except Exception:
                        issues.append(f"{name}: scrap_yield.{comp} must be int")
        return issues

    # Public alias so Studio can call editor.validate()
    def validate(self) -> List[str]:
        self._sanitize_all()
        return self._validate()

    # ---------- CRUD ----------
    def _on_new(self):
        new = {
            "id": "new_item",
            "name": "New Item",
            "description": "",
            "aliases": [],
            "type": "",
            "value": 0,
            "buy_price": 0,
        }
        self.items.append(new)
        self.current = new
        self._reload_list()
        self._reselect(new.get("name", ""))
        self._load_current()

    def _on_duplicate(self):
        it = self.current or self._get_selected()
        if not it: return
        base = it.get("name", "item")
        candidate = f"{base} copy"
        used = {x.get("name","") for x in self.items}
        i = 2
        while candidate in used:
            candidate = f"{base} copy {i}"
            i += 1
        dup = json.loads(json.dumps(it))
        dup["name"] = candidate
        self.items.append(dup)
        self.current = dup
        self._reload_list(select_name=candidate)
        self._reselect(candidate)
        self._load_current()

    def _on_delete(self):
        it = self.current or self._get_selected()
        if not it: return
        if QMessageBox.question(self, "Delete", f"Delete '{it.get('name')}'?",
                                QMessageBox.Yes|QMessageBox.No) == QMessageBox.Yes:
            self.items.remove(it)
            self.current = None
            self._reload_list(select_name=None)
            self._load_current()

    def _on_rename(self):
        it = self.current or self._get_selected()
        if not it: return
        from PyQt5.QtWidgets import QInputDialog
        new_name, ok = QInputDialog.getText(self, "Rename item", "New name:", text=it.get("name",""))
        if not ok: return
        nn = (new_name or "").strip()
        if not nn:
            QMessageBox.warning(self, "Rename", "Name cannot be empty.")
            return
        if any(x for x in self.items if (x is not it and (x.get("name") or "").strip().lower() == nn.lower())):
            QMessageBox.warning(self, "Rename", f"An item named '{nn}' already exists.")
            return
        it["name"] = nn; it["id"] = _sanitize_id(nn)
        self._reload_list()
        self._reselect(nn)
        self._load_current()
        self._refresh_where_used(nn)

    def _reselect(self, name: str):
        for i in range(self.list.count()):
            if self.list.item(i).text().startswith(name + " —"):
                self.list.setCurrentRow(i)
                break

    # ---------- Import/Export ----------
    def _on_import(self):
        path, _ = QFileDialog.getOpenFileName(self, "Import items.json…", str(self.root), "JSON files (*.json)")
        if not path: return
        incoming = _read_list_json(Path(path), "import")
        if not incoming:
            QMessageBox.information(self, "Import", "No items to import.")
            return

        mode, ok = QInputDialog.getItem(self, "Import mode", "How to import?",
                                        ["Replace all", "Append"], 0, False)
        if not ok: return
        if mode == "Replace all":
            self.items = incoming
        else:
            self.items.extend(incoming)
        self.current = None
        # sanitize newly imported content once
        self._sanitize_all()
        self._reload_list(select_name=None)
        self._load_current()

    def _on_export_selected(self):
        it = self.current or self._get_selected()
        if not it:
            QMessageBox.information(self, "Export", "No item selected.")
            return
        path, _ = QFileDialog.getSaveFileName(self, "Export item as…", f"{it.get('name','item')}.json", "JSON files (*.json)")
        if not path: return
        try:
            with open(path, "w", encoding="utf-8") as f:
                json.dump(it, f, ensure_ascii=False, indent=4)
            QMessageBox.information(self, "Export", f"Saved: {path}")
        except Exception as e:
            QMessageBox.critical(self, "Export", f"Failed to export:\n{e}")

    # ---------- Raw JSON ----------
    def _raw_from_fields(self):
        self.raw.setPlainText(json.dumps(self._collect(), ensure_ascii=False, indent=4))

    def _raw_to_fields(self):
        try:
            d = json.loads(self.raw.toPlainText())
            if not isinstance(d, dict): raise ValueError("Root must be an object.")
        except Exception as e:
            QMessageBox.critical(self, "Raw JSON", f"Invalid JSON:\n{e}")
            return
        if self.current is None:
            self.items.append(d); self.current = d
        else:
            self.current.clear(); self.current.update(d)
        # sanitize what was pasted (e.g., negative restore)
        self._sanitize_all()
        self._reload_list()
        self._reselect(self.current.get("name",""))
        self._load_current()

    # ---------- Where Used ----------
    def _read_safe(self, filename: str) -> Any:
        try: # type: ignore
            return json.loads((self.root/filename).read_text(encoding="utf-8"))
        except Exception:
            return None

    def _refresh_where_used(self, item_name: str):
        self.used_list.clear()
        if not item_name: return
        name_l = item_name.lower()

        rooms  = self._read_safe("rooms.json") or []
        enemies= self._read_safe("enemies.json") or []
        shops  = self._read_safe("shops.json") or {}
        quests = self._read_safe("quests.json") or []

        # rooms: room.items (list of names)
        if rooms:
            for r in rooms:
                arr = r.get("items", [])
                if isinstance(arr, list) and any(str(x).lower()==name_l for x in arr):
                    QListWidgetItem(f"room: {r.get('id') or r.get('title') or '?'}", self.used_list)

        # enemies: drops[].id
        if enemies:
            for e in enemies:
                for d in (e.get("drops") or []):
                    if str(d.get("id","")).lower() == name_l:
                        QListWidgetItem(f"enemy drop: {e.get('id','?')}", self.used_list)

        # shops: inventory[].item_id
        if shops:
            for s in shops.values():
                for ent in (s.get("inventory") or []):
                    if str(ent.get("item_id","")).lower() == name_l:
                        QListWidgetItem(f"shop: {s.get('id','?')}", self.used_list)

        # quests: rewards (list of item names or objects w/id)
        if quests:
            for q in quests:
                rw = q.get("rewards", [])
                if isinstance(rw, list):
                    for r in rw:
                        if (str(r).lower() == name_l) or (isinstance(r, dict) and str(r.get("id","")).lower()==name_l):
                            QListWidgetItem(f"quest reward: {q.get('id','?')}", self.used_list)

# ---------------- Entrypoint ----------------
def main():
    app = QApplication(sys.argv)
    root = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else _find_root()
    w = ItemEditor(str(root) if root else None)
    w.resize(1280, 780)
    w.show()
    sys.exit(app.exec_())

if __name__ == "__main__":
    main()
