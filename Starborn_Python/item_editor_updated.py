#!/usr/bin/env python3
"""
Item Editor for Starborn – v2.0
================================
A PyQt5-based tool for maintaining items.json.
- Full support for all item types: equippable, consumable, key items, etc.
- Handles complex nested data like stats, effects, and scrap yields via JSON text fields.
"""

import json
import os
import sys
from collections import Counter
from PyQt5.QtWidgets import (
    QApplication, QWidget, QHBoxLayout, QVBoxLayout, QListWidget,
    QListWidgetItem, QLineEdit, QLabel, QPushButton, QFormLayout,
    QGroupBox, QMessageBox, QPlainTextEdit, QFileDialog, QSpinBox,
    QComboBox
)

PROJECT_ROOT = os.path.abspath(os.path.dirname(__file__))
ITEMS_PATH = os.path.join(PROJECT_ROOT, "items.json")

def _load_items(path: str) -> list[dict]:
    if not os.path.exists(path): return []
    try:
        with open(path, encoding="utf-8") as fp:
            data = json.load(fp)
            return data if isinstance(data, list) else []
    except Exception as e:
        QMessageBox.critical(None, "Load error", f"Failed to load items.json:\n{e}")
        return []

def _save_items(path: str, items: list[dict]):
    try:
        with open(path, "w", encoding="utf-8") as fp:
            json.dump(items, fp, ensure_ascii=False, indent=4)
        QMessageBox.information(None, "Saved", "items.json saved successfully.")
    except Exception as e:
        QMessageBox.critical(None, "Save error", f"Failed to save items.json:\n{e}")

class ItemEditor(QWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Starborn Item Editor v2.0")
        self.resize(1024, 768)
        self.items: list[dict] = _load_items(ITEMS_PATH)
        self.current: dict | None = None
        self.initUI()
        self._refresh_list()

    def initUI(self):
        main_layout = QHBoxLayout(self)
        left_box = QVBoxLayout()
        self.search = QLineEdit()
        self.search.setPlaceholderText("Search items…")
        self.search.textChanged.connect(self._refresh_list)
        left_box.addWidget(self.search)
        self.list = QListWidget()
        self.list.itemClicked.connect(self._on_select)
        left_box.addWidget(self.list, 1)
        btn_row = QHBoxLayout()
        add_btn, dup_btn, rm_btn = QPushButton("Add"), QPushButton("Duplicate"), QPushButton("Delete")
        add_btn.clicked.connect(self._add_item)
        dup_btn.clicked.connect(self._dup_item)
        rm_btn.clicked.connect(self._delete_item)
        for btn in [add_btn, dup_btn, rm_btn]: btn_row.addWidget(btn)
        left_box.addLayout(btn_row)
        main_layout.addLayout(left_box, 1)

        # Inspector
        right_box = QVBoxLayout()
        form_group = QGroupBox("Item Properties")
        form = QFormLayout()

        self.name_edit = QLineEdit()
        self.alias_edit = QLineEdit()
        self.desc_edit = QPlainTextEdit()
        self.item_type_combo = QComboBox()
        self.item_type_combo.addItems(["", "consumable", "equippable", "key_item", "ingredient", "junk", "weapon", "armor"])
        self.value_spin = QSpinBox()
        self.value_spin.setRange(0, 99999)
        self.buy_price_spin = QSpinBox()
        self.buy_price_spin.setRange(0, 99999)
        self.equipment_edit = QPlainTextEdit()
        self.effect_edit = QPlainTextEdit()
        self.scrap_yield_edit = QPlainTextEdit()
        
        form.addRow("Name:", self.name_edit)
        form.addRow("Aliases:", self.alias_edit)
        form.addRow("Description:", self.desc_edit)
        form.addRow("Type:", self.item_type_combo)
        form.addRow("Base Value:", self.value_spin)
        form.addRow("Buy Price:", self.buy_price_spin)
        form.addRow("Equipment Data (JSON):", self.equipment_edit)
        form.addRow("Effect Data (JSON):", self.effect_edit)
        form.addRow("Scrap Yield (JSON):", self.scrap_yield_edit)
        
        form_group.setLayout(form)
        right_box.addWidget(form_group)

        save_btn = QPushButton("Save All to items.json")
        save_btn.clicked.connect(self._save_file)
        right_box.addWidget(save_btn)
        right_box.addStretch()
        main_layout.addLayout(right_box, 2)

    def _refresh_list(self):
        self.list.clear()
        filter_txt = self.search.text().lower()
        for itm in sorted(self.items, key=lambda x: x["name"].lower()):
            if filter_txt in itm["name"].lower():
                self.list.addItem(QListWidgetItem(itm["name"]))
        if self.current:
            items = [self.list.item(i) for i in range(self.list.count())]
            match = next((i for i in items if i.text() == self.current["name"]), None)
            if match: self.list.setCurrentItem(match)

    def _on_select(self, item: QListWidgetItem):
        self._save_current_fields()
        self.current = next((i for i in self.items if i["name"] == item.text()), None)
        self._load_current()

    def _load_current(self):
        if not self.current:
            for w in [self.name_edit, self.alias_edit, self.desc_edit, self.equipment_edit, self.effect_edit, self.scrap_yield_edit]: w.clear()
            for w in [self.value_spin, self.buy_price_spin]: w.setValue(0)
            self.item_type_combo.setCurrentIndex(0)
            return

        self.name_edit.setText(self.current.get("name", ""))
        self.alias_edit.setText(", ".join(self.current.get("aliases", [])))
        self.desc_edit.setPlainText(self.current.get("description", ""))
        self.item_type_combo.setCurrentText(self.current.get("type", ""))
        self.value_spin.setValue(self.current.get("value", 0))
        self.buy_price_spin.setValue(self.current.get("buy_price", 0))
        
        self.equipment_edit.setPlainText(json.dumps(self.current.get("equipment", {}), indent=2))
        self.effect_edit.setPlainText(json.dumps(self.current.get("effect", {}), indent=2))
        self.scrap_yield_edit.setPlainText(json.dumps(self.current.get("scrap_yield", {}), indent=2))

    def _save_current_fields(self):
        if not self.current: return
        self.current["name"] = self.name_edit.text().strip()
        self.current["aliases"] = [a.strip() for a in self.alias_edit.text().split(",") if a.strip()]
        self.current["description"] = self.desc_edit.toPlainText().strip()
        self.current["type"] = self.item_type_combo.currentText()
        if self.value_spin.value() > 0: self.current["value"] = self.value_spin.value()
        if self.buy_price_spin.value() > 0: self.current["buy_price"] = self.buy_price_spin.value()
        
        try:
            self.current["equipment"] = json.loads(self.equipment_edit.toPlainText() or "{}")
            self.current["effect"] = json.loads(self.effect_edit.toPlainText() or "{}")
            self.current["scrap_yield"] = json.loads(self.scrap_yield_edit.toPlainText() or "{}")
        except json.JSONDecodeError as e:
            QMessageBox.warning(self, "JSON Error", f"Invalid JSON in one of the fields: {e}")
            return False
        return True

    def _add_item(self):
        self._save_current_fields()
        new = {"name": "new_item", "description": "", "aliases": [], "type": ""}
        self.items.append(new)
        self.current = new
        self._refresh_list()
        self._load_current()

    def _dup_item(self):
        if not self.current: return
        self._save_current_fields()
        clone = json.loads(json.dumps(self.current))
        clone["name"] = f"{clone['name']}_copy"
        self.items.append(clone)
        self.current = clone
        self._refresh_list()
        self._load_current()

    def _delete_item(self):
        if not self.current: return
        reply = QMessageBox.question(self, "Delete?", f"Remove '{self.current['name']}'?", QMessageBox.Yes | QMessageBox.No)
        if reply == QMessageBox.Yes:
            self.items.remove(self.current)
            self.current = None
            self._refresh_list()
            self._load_current()

    def _save_file(self):
        if self._save_current_fields():
             _save_items(ITEMS_PATH, self.items)
             self._refresh_list()

if __name__ == "__main__":
    app = QApplication(sys.argv)
    win = ItemEditor()
    win.show()
    sys.exit(app.exec_())