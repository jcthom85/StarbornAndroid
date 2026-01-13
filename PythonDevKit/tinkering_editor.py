#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations

import json, re
from pathlib import Path
from typing import Dict, List, Any, Optional, Tuple

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (
    QWidget, QSplitter, QHBoxLayout, QVBoxLayout, QFormLayout,
    QListWidget, QListWidgetItem, QLineEdit, QPushButton, QTextEdit,
    QGroupBox, QLabel, QMessageBox, QToolButton, QCompleter, QComboBox
)

from data_core import detect_assets_dir, json_load, json_save
from editor_bus import goto as studio_goto, refresh_references as studio_refresh

ITEMS_FILE = "items.json"
RECIPES_FILE = "recipes_tinkering.json"

def _slug(s: str) -> str:
    return re.sub(r"[^a-z0-9_]+", "_", s.strip().lower())

def _root_from_hint(hint: Optional[Path] = None) -> Path:
    try:
        return detect_assets_dir(Path(__file__).resolve())
    except Exception:
        return (Path(__file__).resolve().parent.parent)

def _read_items(root: Path) -> List[Dict[str, Any]]:
    # Items are at the project root, not in /data
    return json_load(root / ITEMS_FILE, [])

def _item_names(root: Path) -> List[str]:
    items = _read_items(root)
    names = []
    for it in items:
        n = it.get("name") or it.get("id")
        if n: names.append(n)
    return sorted(set(names))

def _read_recipes(root: Path) -> List[Dict[str, Any]]:
    return json_load(root / RECIPES_FILE, [])

def _write_recipes(root: Path, data: List[Dict[str, Any]]):
    json_save(root / RECIPES_FILE, data)

def _add_item_skeleton(root: Path, name: str, i_type: str = "component") -> Tuple[bool,str]:
    if not name.strip():
        return False, "Name required"
    items_path = root / ITEMS_FILE
    items = json_load(items_path, [])
    for it in items:
        if it.get("name") == name or it.get("id") == name:
            return True, it.get("id") or it.get("name")
    new_id = _slug(name)
    items.append({"id": new_id, "name": name, "type": i_type, "price": 0})
    json_save(items_path, items)
    return True, new_id

class TinkeringEditor(QWidget):
    """
    Starborn — Tinkering Editor
    Matches tinkering_screen: id, name, base, components[], result, description, success_message.
    """
    def __init__(self, root_dir: Optional[Path] = None):
        super().__init__()
        self.root = _root_from_hint(root_dir)
        self.setWindowTitle("Tinkering Editor")
        self.resize(1100, 760)

        self.current_idx: Optional[int] = None
        self.recipes: List[Dict[str, Any]] = _read_recipes(self.root)
        self._items_cache: List[str] = _item_names(self.root)

        self._build_ui()
        self._reload_list()

    # ---- Studio hooks ----
    def save(self) -> bool:
        # Save the currently displayed form data into the self.recipes list
        self._apply_form_to_memory()
        # Now write the entire (potentially modified) list to disk
        _write_recipes(self.root, self.recipes)
        return True

    def refresh_refs(self):
        self._items_cache = _item_names(self.root)
        self._apply_completers()
        # Re-select to refresh any data that might have changed
        if self.current_idx is not None:
            self._load_form(self.current_idx)

    # ---- UI ----
    def _build_ui(self):
        splitter = QSplitter(self)
        main = QHBoxLayout(self)
        main.addWidget(splitter)
        self.setLayout(main)

        # Left: list + search
        left = QWidget()
        lv = QVBoxLayout(left)
        self.search = QLineEdit(); self.search.setPlaceholderText("Search tinkering recipes…")
        self.search.textChanged.connect(self._reload_list)
        self.list = QListWidget()
        self.list.currentRowChanged.connect(self._load_form)
        self.list.itemSelectionChanged.connect(self._on_selection_changed)
        lv.addWidget(self.search)
        lv.addWidget(self.list)
        row = QHBoxLayout()
        self.btn_new = QPushButton("New")
        self.btn_dup = QPushButton("Duplicate"); self.btn_dup.setToolTip("Duplicate the selected recipe")
        self.btn_del = QPushButton("Delete")
        row.addWidget(self.btn_new); row.addWidget(self.btn_dup); row.addWidget(self.btn_del)
        lv.addLayout(row)
        splitter.addWidget(left)

        # Right: form
        right = QWidget()
        rv = QVBoxLayout(right)

        # GroupBox for Core Details
        core_box = QGroupBox("Core Details")
        form = QFormLayout(core_box)
        self.inp_id = QLineEdit()
        self.inp_name = QLineEdit()
        form.addRow("ID", self.inp_id)
        form.addRow("Name", self.inp_name)
        rv.addWidget(core_box)

        # GroupBox for Items
        items_box = QGroupBox("Recipe Items")
        items_form = QFormLayout(items_box)
        self.inp_base = QComboBox(); self.inp_base.setEditable(True)
        self.btn_open_base = QToolButton(); self.btn_open_base.setText("…"); self.btn_open_base.setToolTip("Open in Item Editor")
        self.btn_quick_base = QToolButton(); self.btn_quick_base.setText("+"); self.btn_quick_base.setToolTip("Quick-Create Item")
        base_row = QHBoxLayout(); base_row.addWidget(self.inp_base); base_row.addWidget(self.btn_open_base); base_row.addWidget(self.btn_quick_base)

        self.inp_comp1 = QComboBox(); self.inp_comp1.setEditable(True)
        self.inp_comp2 = QComboBox(); self.inp_comp2.setEditable(True)
        self.btn_open_c1 = QToolButton(); self.btn_open_c1.setText("…"); self.btn_open_c1.setToolTip("Open in Item Editor")
        self.btn_quick_c1 = QToolButton(); self.btn_quick_c1.setText("+"); self.btn_quick_c1.setToolTip("Quick-Create Item")
        self.btn_open_c2 = QToolButton(); self.btn_open_c2.setText("…"); self.btn_open_c2.setToolTip("Open in Item Editor")
        self.btn_quick_c2 = QToolButton(); self.btn_quick_c2.setText("+"); self.btn_quick_c2.setToolTip("Quick-Create Item")
        c1_row = QHBoxLayout(); c1_row.addWidget(self.inp_comp1); c1_row.addWidget(self.btn_open_c1); c1_row.addWidget(self.btn_quick_c1)
        c2_row = QHBoxLayout(); c2_row.addWidget(self.inp_comp2); c2_row.addWidget(self.btn_open_c2); c2_row.addWidget(self.btn_quick_c2)

        self.inp_result = QComboBox(); self.inp_result.setEditable(True)
        self.btn_open_result = QToolButton(); self.btn_open_result.setText("…"); self.btn_open_result.setToolTip("Open in Item Editor")
        self.btn_quick_result = QToolButton(); self.btn_quick_result.setText("+"); self.btn_quick_result.setToolTip("Quick-Create Item")
        res_row = QHBoxLayout(); res_row.addWidget(self.inp_result); res_row.addWidget(self.btn_open_result); res_row.addWidget(self.btn_quick_result)

        items_form.addRow("Base Item", base_row)
        items_form.addRow("Component 1", c1_row)
        items_form.addRow("Component 2", c2_row)
        items_form.addRow("Result Item", res_row)
        rv.addWidget(items_box)

        # GroupBox for Text
        text_box = QGroupBox("Text")
        text_form = QFormLayout(text_box)
        self.inp_desc = QTextEdit()
        self.inp_success = QLineEdit()
        text_form.addRow("Description", self.inp_desc)
        text_form.addRow("Success Message", self.inp_success)
        rv.addWidget(text_box)

        rv.addStretch(1)
        # bottom bar
        bar = QHBoxLayout()
        bar.addStretch(1)
        self.btn_revert = QPushButton("Revert")
        self.btn_save = QPushButton("Save")
        bar.addWidget(self.btn_revert); bar.addWidget(self.btn_save)
        rv.addLayout(bar)

        splitter.addWidget(right); splitter.setStretchFactor(1, 1)

        # signals
        self.btn_new.clicked.connect(self._new)
        self.btn_dup.clicked.connect(self._dup)
        self.btn_del.clicked.connect(self._del)
        self.btn_revert.clicked.connect(self._on_revert)
        self.btn_save.clicked.connect(lambda: self.save() and QMessageBox.information(self, "Saved", "recipes_tinkering.json updated."))

        self.btn_open_base.clicked.connect(lambda: self._goto(self.inp_base.currentText()))
        self.btn_quick_base.clicked.connect(lambda: self._quick(self.inp_base, "component"))
        self.btn_open_c1.clicked.connect(lambda: self._goto(self.inp_comp1.currentText()))
        self.btn_quick_c1.clicked.connect(lambda: self._quick(self.inp_comp1, "component"))
        self.btn_open_c2.clicked.connect(lambda: self._goto(self.inp_comp2.currentText()))
        self.btn_quick_c2.clicked.connect(lambda: self._quick(self.inp_comp2, "component"))
        self.btn_open_result.clicked.connect(lambda: self._goto(self.inp_result.currentText()))
        self.btn_quick_result.clicked.connect(lambda: self._quick(self.inp_result, "equippable"))

        self._apply_completers()

    def _apply_completers(self):
        comp = QCompleter(self._items_cache); comp.setCaseSensitivity(Qt.CaseInsensitive)
        for combo in (self.inp_base, self.inp_comp1, self.inp_comp2, self.inp_result):
            combo.addItems([""] + self._items_cache)
            combo.setCompleter(comp)
            # Ensure the completer works on the line edit part of the combo box
            if isinstance(combo.lineEdit(), QLineEdit):
                combo.lineEdit().setCompleter(comp)

    # ---- list ----
    def _reload_list(self):
        txt = (self.search.text() or "").strip().lower()
        self.list.clear()
        for i, r in enumerate(self.recipes):
            rid = r.get("id",""); nm = r.get("name","")
            label = f"{rid} — {nm}" if rid else nm
            if not txt or txt in label.lower():
                item = QListWidgetItem(label)
                item.setData(Qt.UserRole, i) # Store original index
                self.list.addItem(item)
        if self.list.count() and self.list.currentRow() == -1:
            self.list.setCurrentRow(0)
        else: self._clear_form()

    def _on_revert(self):
        if QMessageBox.question(self, "Revert", "Discard all unsaved changes and reload from file?",
                                QMessageBox.Yes | QMessageBox.No) == QMessageBox.Yes:
            self.recipes = _read_recipes(self.root)
            self.current_idx = None
            self._reload_list()
            QMessageBox.information(self, "Reverted", "Recipes reloaded from file.")

    def _new(self):
        payload = {"id":"", "name":"New Tinker", "base":"", "components":[], "result":"", "description":"", "success_message":""}
        self.recipes.append(payload)
        self._reload_list()
        self._select_text("New Tinker")

    def _dup(self):
        if self.current_idx is None: return
        self._apply_form_to_memory() # Save current form first
        idx = self.current_idx
        src = json.loads(json.dumps(self.recipes[idx]))
        src["id"] = f"{src.get('id', 'new')}_copy"
        src["name"] = f"{src.get('name','')} (Copy)"
        self.recipes.append(src)
        self._reload_list()
        self._select_text("(Copy)")

    def _del(self):
        it = self.list.currentItem()
        if not it: return
        idx = it.data(Qt.UserRole)
        if QMessageBox.question(self, "Delete", f"Delete '{it.text()}'?") != QMessageBox.Yes:
            return
        self.recipes.pop(idx)
        self.current_idx = None
        self._reload_list()

    def _select_text(self, token: str):
        for i in range(self.list.count()):
            if token in self.list.item(i).text():
                self.list.setCurrentRow(i); return

    def _on_selection_changed(self):
        # Save previous form before loading new one
        self._apply_form_to_memory()

    # ---- form ----
    def _clear_form(self):
        self.current_idx = None
        self.inp_id.setText(""); self.inp_name.setText("")
        self.inp_base.setCurrentText(""); self.inp_comp1.setCurrentText(""); self.inp_comp2.setCurrentText("")
        self.inp_result.setCurrentText(""); self.inp_desc.setPlainText(""); self.inp_success.setText("")

    def _load_form(self, row: int):
        item = self.list.item(row)
        if not item:
            self._clear_form(); return
        idx = item.data(Qt.UserRole)
        if idx < 0 or idx >= len(self.recipes):
            self._clear_form(); return
        r = self.recipes[idx]
        self.inp_id.setText(r.get("id",""))
        self.inp_name.setText(r.get("name",""))
        self.inp_base.setCurrentText(r.get("base",""))
        comps = r.get("components") or []
        self.inp_comp1.setCurrentText(comps[0] if len(comps)>0 else "")
        self.inp_comp2.setCurrentText(comps[1] if len(comps)>1 else "")
        self.inp_result.setCurrentText(r.get("result",""))
        self.inp_desc.setPlainText(r.get("description",""))
        self.inp_success.setText(r.get("success_message",""))
        self.current_idx = idx

    def _apply_form_to_memory(self):
        if self.current_idx is None or self.current_idx >= len(self.recipes):
            return
        payload = self._gather_form()
        if payload:
            self.recipes[self.current_idx] = payload

    def _gather_form(self) -> Optional[Dict[str, Any]]:
        rid = (self.inp_id.text() or "").strip()
        nm  = (self.inp_name.text() or "").strip()
        base = (self.inp_base.currentText() or "").strip()
        c1 = (self.inp_comp1.currentText() or "").strip()
        c2 = (self.inp_comp2.currentText() or "").strip()
        res = (self.inp_result.currentText() or "").strip()
        desc = self.inp_desc.toPlainText().strip()
        succ = (self.inp_success.text() or "").strip()

        if not nm: QMessageBox.warning(self, "Validation", "Name is required."); return None
        if not base: QMessageBox.warning(self, "Validation", "Base item is required."); return None
        if not res: QMessageBox.warning(self, "Validation", "Result item is required."); return None

        # Auto-id if empty
        if not rid:
            rid = _slug(nm)

        # Guidance on unknowns (non-blocking)
        unknowns = [x for x in [base, c1, c2, res] if x.strip() and x not in self._items_cache]
        if unknowns:
            QMessageBox.information(self, "Heads up", f"Unknown item(s): {', '.join(unknowns)}\nYou can still save, or Quick Create/Open them.")
        comps: List[str] = [x for x in [c1, c2] if x]
        return {
            "id": rid, "name": nm, "base": base, "components": comps,
            "result": res, "description": desc, "success_message": succ
        }

    # ---- item helpers ----
    def _goto(self, name: str):
        nm = (name or "").strip()
        if nm: studio_goto("item", nm)

    def _quick(self, line: QLineEdit, i_type: str):
        nm = (line.currentText() or "").strip()
        if not nm:
            QMessageBox.warning(self, "Quick Create", "Enter a name first.")
            return
        ok, _ = _add_item_skeleton(self.root, nm, i_type=i_type)
        if ok:
            studio_refresh(); studio_goto("item", nm); self.refresh_refs()

if __name__ == "__main__":
    from PyQt5.QtWidgets import QApplication
    import sys
    app = QApplication(sys.argv)
    w = TinkeringEditor()
    w.show()
    sys.exit(app.exec_())
