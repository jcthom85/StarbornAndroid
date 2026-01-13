#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Starborn — Cooking Recipe Editor (enhanced)

Key upgrades:
- Item-aware UX: autocompletion for ingredients/result from items.json
- Inline per-ingredient actions: Open in Item Editor / Quick Create Item
- Result actions: Open / Quick Create
- Search box on recipe list (+ selection preservation)
- Studio hooks: save() and refresh_refs() compatible with Studio Pro
- Non-blocking validation with clear guidance
- Keyboard shortcuts: Ctrl+N / Ctrl+D / Del / Ctrl+S / Ctrl+F
- Optional metadata editor (key/value) that preserves unknown fields
"""

from __future__ import annotations

import json, os, re
from pathlib import Path
from typing import Dict, List, Any, Optional, Tuple

from PyQt5.QtCore import Qt
from PyQt5.QtGui import QKeySequence
from PyQt5.QtWidgets import (
    QWidget, QHBoxLayout, QVBoxLayout, QFormLayout, QListWidget, QListWidgetItem,
    QLineEdit, QPushButton, QLabel, QTableWidget, QTableWidgetItem, QHeaderView,
    QSpinBox, QAbstractItemView, QMessageBox, QToolButton, QGroupBox, QShortcut,
    QMenu
)
from PyQt5.QtWidgets import QCompleter

# ---------- Project helpers ----------
try:
    # Preferred: inside tools package
    from .starborn_data import find_project_root, write_json_with_backup, collect_item_names
except Exception:
    # Fallback: standalone
    from starborn_data import find_project_root, write_json_with_backup, collect_item_names

# ---------- Studio bus (optional) ----------
def _noop(*a, **k): pass
try:
    from editor_bus import goto as studio_goto, refresh_references as studio_refresh
except Exception:
    studio_goto = _noop
    studio_refresh = _noop

CORE_KEYS = {"name", "ingredients", "result"}

# ---------- Utility ----------
def _slug(s: str) -> str:
    return re.sub(r"[^a-z0-9_]+", "_", (s or "").strip().lower())

def _read_json(path: Path, default):
    try:
        with path.open("r", encoding="utf-8") as f: return json.load(f)
    except Exception:
        return default

def _write_json(path: Path, data) -> bool:
    try:
        return write_json_with_backup(path, data)
    except Exception:
        try:
            with path.open("w", encoding="utf-8") as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
            return True
        except Exception:
            return False

def _load_items_list(root: Path) -> List[Dict[str, Any]]:
    return _read_json(root / "items.json", [])

def _collect_item_names(root: Path) -> List[str]:
    try:
        names = collect_item_names(root)
        if isinstance(names, list): return sorted(set(map(str, names)))
    except Exception:
        pass
    # fallback from items.json
    names: List[str] = []
    for it in _load_items_list(root):
        n = it.get("name") or it.get("id")
        if n: names.append(str(n))
    return sorted(set(names))

def _item_exists(root: Path, name: str) -> bool:
    if not name: return False
    nm = name.strip().lower()
    for it in _load_items_list(root):
        n = str(it.get("name") or it.get("id") or "").strip().lower()
        if nm == n: return True
        # also consider aliases if present
        for al in it.get("aliases", []) or []:
            if str(al).strip().lower() == nm: return True
    return False

def _quick_create_item(root: Path, name: str, i_type: str = "ingredient") -> Tuple[bool, str]:
    """
    Minimal, explicit item creator so you can stay in flow.
    Safe (no dupes), schema-light. Finish details in Item Editor.
    """
    name = (name or "").strip()
    if not name:
        return False, "Name required"
    items = _load_items_list(root)
    # dedupe
    for it in items:
        if str(it.get("name") or it.get("id")).strip().lower() == name.lower():
            return True, str(it.get("name") or it.get("id"))
    new = {
        "name": name,
        "type": i_type,
        "description": "",
        "value": 0
    }
    ok = _write_json(root / "items.json", items + [new])
    return (ok, name if ok else "Failed to write items.json")

# ---------- Reusable editors ----------
class ExtrasEditor(QWidget):
    """
    Simple key/value table to preserve/edit unknown recipe keys.
    Values stored as strings (JSON will still write numbers if you type numbers).
    """
    def __init__(self, title: str, parent: Optional[QWidget] = None) -> None:
        super().__init__(parent)
        self._keys_blocklist = set(CORE_KEYS)  # never let user shadow core keys
        layout = QVBoxLayout(self)
        if title:
            layout.addWidget(QLabel(title))
        self.table = QTableWidget(0, 2, self)
        self.table.setHorizontalHeaderLabels(["Key", "Value"])
        self.table.horizontalHeader().setSectionResizeMode(0, QHeaderView.Stretch)
        self.table.horizontalHeader().setSectionResizeMode(1, QHeaderView.Stretch)
        self.table.setSelectionBehavior(QAbstractItemView.SelectRows)
        layout.addWidget(self.table)
        btns = QHBoxLayout()
        self.btn_add = QPushButton("+ Add")
        self.btn_del = QPushButton("– Remove")
        btns.addWidget(self.btn_add); btns.addWidget(self.btn_del); btns.addStretch(1)
        layout.addLayout(btns)
        self.btn_add.clicked.connect(self._add_row)
        self.btn_del.clicked.connect(self._del_row)

    def set_data(self, data: Dict[str, Any]):
        self.table.setRowCount(0)
        for k, v in (data or {}).items():
            if k in self._keys_blocklist:  # safety
                continue
            r = self.table.rowCount()
            self.table.insertRow(r)
            self.table.setItem(r, 0, QTableWidgetItem(str(k)))
            self.table.setItem(r, 1, QTableWidgetItem(json.dumps(v) if not isinstance(v, str) else v))

    def get_data(self) -> Dict[str, Any]:
        out: Dict[str, Any] = {}
        for r in range(self.table.rowCount()):
            k = (self.table.item(r, 0).text() if self.table.item(r, 0) else "").strip()
            v = (self.table.item(r, 1).text() if self.table.item(r, 1) else "")
            if not k or k in self._keys_blocklist:
                continue
            # Try JSON parse; fallback to raw string
            try:
                out[k] = json.loads(v)
            except Exception:
                out[k] = v
        return out

    def _add_row(self):
        r = self.table.rowCount()
        self.table.insertRow(r)
        self.table.setItem(r, 0, QTableWidgetItem(""))
        self.table.setItem(r, 1, QTableWidgetItem(""))

    def _del_row(self):
        r = self.table.currentRow()
        if r >= 0:
            self.table.removeRow(r)

class DictEditor(QWidget):
    """
    Ingredient map editor with:
    - QLineEdit + QCompleter for ingredient names
    - QSpinBox for quantities
    - Per-row "Open" and "Create" actions
    """
    def __init__(self, title: str, parent: Optional[QWidget] = None) -> None:
        super().__init__(parent)
        self._completer_words: List[str] = []
        layout = QVBoxLayout(self)
        if title:
            layout.addWidget(QLabel(title))
        self.table = QTableWidget(0, 3, self)
        self.table.setHorizontalHeaderLabels(["Ingredient", "Qty", ""])
        self.table.horizontalHeader().setSectionResizeMode(0, QHeaderView.Stretch)
        self.table.horizontalHeader().setSectionResizeMode(1, QHeaderView.ResizeToContents)
        self.table.horizontalHeader().setSectionResizeMode(2, QHeaderView.ResizeToContents)
        self.table.setSelectionBehavior(QAbstractItemView.SelectRows)
        layout.addWidget(self.table)
        btns = QHBoxLayout()
        self.btn_add = QPushButton("+ Add")
        self.btn_del = QPushButton("– Remove")
        btns.addWidget(self.btn_add); btns.addWidget(self.btn_del); btns.addStretch(1)
        layout.addLayout(btns)
        self.btn_add.clicked.connect(lambda: self.add_row("", 1))
        self.btn_del.clicked.connect(self._del_row)

        # context menu on ingredient cells
        self.table.setContextMenuPolicy(Qt.CustomContextMenu)
        self.table.customContextMenuRequested.connect(self._table_menu)

    def set_completer_words(self, words: List[str]):
        self._completer_words = sorted(set(words))
        # update existing rows
        for r in range(self.table.rowCount()):
            w = self.table.cellWidget(r, 0)
            if isinstance(w, QLineEdit):
                comp = QCompleter(self._completer_words); comp.setCaseSensitivity(Qt.CaseInsensitive)
                w.setCompleter(comp)

    def set_dict(self, data: Optional[Dict[str, int]]) -> None:
        self.table.setRowCount(0)
        for key, value in (data or {}).items():
            self.add_row(str(key), int(value))

    def get_dict(self) -> Dict[str, int]:
        out: Dict[str, int] = {}
        for r in range(self.table.rowCount()):
            le = self.table.cellWidget(r, 0)
            sp = self.table.cellWidget(r, 1)
            if not isinstance(le, QLineEdit) or not isinstance(sp, QSpinBox):
                continue
            k = (le.text() or "").strip()
            v = int(sp.value())
            if k: out[k] = v
        return out

    def add_row(self, name: str, qty: int):
        r = self.table.rowCount()
        self.table.insertRow(r)

        le = QLineEdit(self)
        le.setText(name)
        comp = QCompleter(self._completer_words); comp.setCaseSensitivity(Qt.CaseInsensitive)
        le.setCompleter(comp)
        self.table.setCellWidget(r, 0, le)

        sp = QSpinBox(self); sp.setRange(1, 999); sp.setValue(int(qty))
        self.table.setCellWidget(r, 1, sp)

        cell = QWidget(self)
        lay = QHBoxLayout(cell); lay.setContentsMargins(0,0,0,0)
        btn_open = QToolButton(cell); btn_open.setText("Open")
        btn_quick = QToolButton(cell); btn_quick.setText("Create")
        lay.addWidget(btn_open); lay.addWidget(btn_quick); lay.addStretch(1)
        self.table.setCellWidget(r, 2, cell)

        def _do_open():
            nm = (le.text() or "").strip()
            if nm: studio_goto("item", nm)
        def _do_quick():
            nm = (le.text() or "").strip()
            if not nm:
                QMessageBox.information(self, "Quick Create", "Enter an ingredient name first.")
                return
            ok, msg = _quick_create_item(self.parent().root, nm, i_type="ingredient")
            if not ok:
                QMessageBox.warning(self, "Quick Create", str(msg))
                return
            studio_refresh()
            studio_goto("item", nm)
            # refresh completers globally
            self.parent().refresh_refs()

        btn_open.clicked.connect(_do_open)
        btn_quick.clicked.connect(_do_quick)

    def _del_row(self):
        r = self.table.currentRow()
        if r >= 0:
            self.table.removeRow(r)

    def _table_menu(self, pt):
        r = self.table.indexAt(pt).row()
        if r < 0: return
        w = self.table.cellWidget(r, 0)
        nm = (w.text() or "").strip() if isinstance(w, QLineEdit) else ""
        menu = QMenu(self)
        act_open = menu.addAction("Open in Item Editor")
        act_make = menu.addAction("Quick Create Item…")
        a = menu.exec_(self.table.mapToGlobal(pt))
        if a == act_open and nm:
            studio_goto("item", nm)
        elif a == act_make:
            if not nm:
                QMessageBox.information(self, "Quick Create", "Enter an ingredient name first.")
                return
            ok, msg = _quick_create_item(self.parent().root, nm, i_type="ingredient")
            if not ok:
                QMessageBox.warning(self, "Quick Create", str(msg)); return
            studio_refresh(); studio_goto("item", nm); self.parent().refresh_refs()

# ---------- Main editor ----------
class CookingEditor(QWidget):
    """Main widget class for editing cooking recipes."""

    def __init__(self, project_root: Optional[Path] = None, parent: Optional[QWidget] = None) -> None:
        super().__init__(parent)
        # Resolve project root
        try:
            self.root = find_project_root(Path(project_root) if project_root else None)
        except Exception:
            self.root = Path(__file__).resolve().parents[1]

        self.file_path = self.root / "recipes_cooking.json"

        # Data
        self.recipes: List[Dict[str, Any]] = _read_json(self.file_path, [])
        if not isinstance(self.recipes, list):
            self.recipes = []
        self.current: Optional[Dict[str, Any]] = None
        self._item_names: List[str] = _collect_item_names(self.root)

        # UI
        self._build_ui()
        self._reload_list(select_name=None)
        self._apply_completers()
        self._attach_shortcuts()

    # Studio hooks
    def save(self) -> bool:
        # Ensure current form is pushed into list
        if self.current is not None:
            idx = self.list.currentRow()
            if 0 <= idx < len(self.recipes):
                updated = self._gather_form()
                if not updated:
                    return False
                self.recipes[idx] = updated
                # update list label
                self.list.item(idx).setText(updated.get("name", "Unnamed"))
        return _write_json(self.file_path, self.recipes)

    def refresh_refs(self):
        """Studio will call this for 'Refresh Refs'."""
        self._item_names = _collect_item_names(self.root)
        self._apply_completers()

    # UI construction
    def _build_ui(self) -> None:
        layout = QHBoxLayout(self)

        # Left: search + list + buttons
        left = QVBoxLayout()
        self.search = QLineEdit(self); self.search.setPlaceholderText("Search recipes…")
        self.search.textChanged.connect(lambda: self._reload_list(select_name=self._current_name()))
        left.addWidget(self.search)

        self.list = QListWidget()
        self.list.itemSelectionChanged.connect(self._on_selection_changed)
        left.addWidget(self.list, 1)

        btn_row = QHBoxLayout()
        new_btn = QPushButton("New"); dup_btn = QPushButton("Duplicate"); del_btn = QPushButton("Delete")
        new_btn.clicked.connect(self._on_new); dup_btn.clicked.connect(self._on_duplicate); del_btn.clicked.connect(self._on_delete)
        btn_row.addWidget(new_btn); btn_row.addWidget(dup_btn); btn_row.addWidget(del_btn)
        left.addLayout(btn_row)

        layout.addLayout(left, 1)

        # Right: details
        right = QFormLayout()

        self.name_edit = QLineEdit()
        self.name_edit.editingFinished.connect(self._on_field_changed)
        right.addRow("Name", self.name_edit)

        # Result field with actions
        res_row = QHBoxLayout()
        self.result_edit = QLineEdit()
        self.result_edit.editingFinished.connect(self._on_field_changed)
        btn_res_open = QToolButton(); btn_res_open.setText("Open")
        btn_res_new  = QToolButton(); btn_res_new.setText("Create")
        res_row.addWidget(self.result_edit, 1)
        res_row.addWidget(btn_res_open)
        res_row.addWidget(btn_res_new)
        def _open_res():
            nm = (self.result_edit.text() or "").strip()
            if nm: studio_goto("item", nm)
        def _create_res():
            nm = (self.result_edit.text() or "").strip()
            if not nm:
                QMessageBox.information(self, "Quick Create", "Enter a result item name first."); return
            ok, msg = _quick_create_item(self.root, nm, i_type="consumable")
            if not ok: QMessageBox.warning(self, "Quick Create", str(msg)); return
            studio_refresh(); studio_goto("item", nm); self.refresh_refs()
        btn_res_open.clicked.connect(_open_res)
        btn_res_new.clicked.connect(_create_res)
        right.addRow("Result", res_row)

        # Ingredients table
        self.ing_editor = DictEditor("Ingredients")
        right.addRow(self.ing_editor)

        # Optional metadata group (preserves unknown keys)
        grp = QGroupBox("Extras (optional)")
        grp_l = QVBoxLayout(grp)
        self.extras = ExtrasEditor(title="")
        grp_l.addWidget(self.extras)
        right.addRow(grp)

        # Bottom toolbar
        b = QHBoxLayout()
        self.btn_revert = QPushButton("Revert")
        self.btn_save = QPushButton("Save")
        b.addStretch(1); b.addWidget(self.btn_revert); b.addWidget(self.btn_save)
        right.addRow(b)

        self.btn_revert.clicked.connect(lambda: self._reload_list(select_name=self._current_name()))
        self.btn_save.clicked.connect(lambda: self.save() and QMessageBox.information(self, "Saved", "recipes_cooking.json updated."))

        wrapper = QVBoxLayout()
        w = QWidget(); w.setLayout(right)
        layout.addWidget(w, 2)

    def _apply_completers(self):
        comp = QCompleter(self._item_names); comp.setCaseSensitivity(Qt.CaseInsensitive)
        self.result_edit.setCompleter(comp)
        self.ing_editor.set_completer_words(self._item_names)

    def _attach_shortcuts(self):
        QShortcut(QKeySequence("Ctrl+N"), self, activated=self._on_new)
        QShortcut(QKeySequence("Ctrl+D"), self, activated=self._on_duplicate)
        QShortcut(QKeySequence("Delete"), self, activated=self._on_delete)
        QShortcut(QKeySequence("Ctrl+S"), self, activated=lambda: self.save() and QMessageBox.information(self, "Saved", "recipes_cooking.json updated."))
        self.search.setFocusPolicy(Qt.StrongFocus)
        QShortcut(QKeySequence("Ctrl+F"), self, activated=lambda: self.search.setFocus())

    # List mgmt
    def _reload_list(self, select_name: Optional[str]):
        filter_txt = (self.search.text() or "").strip().lower()
        self.list.blockSignals(True)
        self.list.clear()
        # sort by name for sanity
        sorted_recipes = sorted(self.recipes, key=lambda r: (r.get("name") or "").lower())
        for rec in sorted_recipes:
            nm = rec.get("name", "Unnamed")
            if not filter_txt or filter_txt in nm.lower():
                QListWidgetItem(nm, self.list)
        self.list.blockSignals(False)

        # Try to restore selection
        if self.list.count():
            row = 0
            if select_name:
                for i in range(self.list.count()):
                    if self.list.item(i).text() == select_name:
                        row = i; break
            self.list.setCurrentRow(row)
            self._load_current()
        else:
            self._clear_form()
            self.current = None

    def _current_name(self) -> Optional[str]:
        it = self.list.currentItem()
        return it.text() if it else None

    def _on_selection_changed(self) -> None:
        item = self.list.currentItem()
        if not item:
            self.current = None
            self._clear_form()
            return
        # Find in original list by name (there may be filters)
        name = item.text()
        for r in self.recipes:
            if r.get("name") == name:
                self.current = r
                break
        else:
            self.current = None
        self._load_current()

    # Form binding
    def _clear_form(self) -> None:
        self.name_edit.setText("")
        self.result_edit.setText("")
        self.ing_editor.set_dict({})
        self.extras.set_data({})

    def _load_current(self) -> None:
        if not self.current:
            self._clear_form(); return
        self.name_edit.setText(self.current.get("name", ""))
        self.result_edit.setText(self.current.get("result", ""))
        self.ing_editor.set_dict(self.current.get("ingredients", {}))
        # extras = all non-core keys preserved
        extra = {k: v for k, v in self.current.items() if k not in CORE_KEYS}
        self.extras.set_data(extra)

    def _gather_form(self) -> Optional[Dict[str, Any]]:
        if self.current is None:
            return None
        name = (self.name_edit.text() or "").strip()
        result = (self.result_edit.text() or "").strip()
        ingredients = self.ing_editor.get_dict()
        extras = self.extras.get_data()

        if not name:
            QMessageBox.warning(self, "Validation", "Recipe name is required.")
            return None
        if not result:
            QMessageBox.warning(self, "Validation", "Result item is required.")
            return None
        if not ingredients:
            QMessageBox.warning(self, "Validation", "At least one ingredient is required.")
            return None
        # warn—not block—on unknown references
        unk = []
        if result and not _item_exists(self.root, result):
            unk.append(f"result '{result}'")
        for ing in ingredients.keys():
            if not _item_exists(self.root, ing):
                unk.append(f"ingredient '{ing}'")
        if unk:
            QMessageBox.information(
                self, "Heads up",
                "Unknown item references detected: " + ", ".join(unk) +
                "\nYou can still save, or use Open/Quick Create to resolve them."
            )

        payload = {"name": name, "result": result, "ingredients": ingredients}
        # merge extras (avoid shadowing core keys)
        for k, v in extras.items():
            if k in CORE_KEYS: continue
            payload[k] = v
        return payload

    def _on_field_changed(self):
        if self.current is None: return
        updated = self._gather_form()
        if not updated: return
        # enforce unique names
        nm = updated["name"]
        name_dupe = any((r is not self.current) and (r.get("name","").strip().lower() == nm.strip().lower())
                        for r in self.recipes)
        if name_dupe:
            QMessageBox.warning(self, "Duplicate Name", f"A recipe named '{nm}' already exists.")
            # revert name field to current
            self.name_edit.setText(self.current.get("name",""))
            return
        # commit
        self.current.clear()
        self.current.update(updated)
        # update list label if name changed
        it = self.list.currentItem()
        if it and it.text() != nm:
            it.setText(nm)

    # CRUD
    def _on_new(self) -> None:
        base = "New Recipe"
        existing = {r.get("name","") for r in self.recipes}
        name = base
        i = 2
        while name in existing:
            name = f"{base} {i}"; i += 1
        payload = {"name": name, "result": "", "ingredients": {}}
        self.recipes.append(payload)
        self._reload_list(select_name=name)

    def _on_duplicate(self) -> None:
        if not self.current: return
        src = json.loads(json.dumps(self.current))
        base = f"{src.get('name','Unnamed')} (Copy)"
        existing = {r.get("name","") for r in self.recipes}
        name = base; i = 2
        while name in existing:
            name = f"{base} {i}"; i += 1
        src["name"] = name
        self.recipes.append(src)
        self._reload_list(select_name=name)

    def _on_delete(self) -> None:
        it = self.list.currentItem()
        if not it: return
        nm = it.text()
        if QMessageBox.question(self, "Delete", f"Delete recipe '{nm}'?") != QMessageBox.Yes:
            return
        # remove from list and underlying
        self.recipes = [r for r in self.recipes if r.get("name") != nm]
        self._reload_list(select_name=None)

    # Close behavior: only prompt when run standalone
    def closeEvent(self, event) -> None:
        if os.getenv("STUDIO_MANAGED"):
            event.accept(); return
        if QMessageBox.question(self, "Save changes", "Save changes before closing?",
                                QMessageBox.Yes | QMessageBox.No) == QMessageBox.Yes:
            self.save()
        event.accept()


if __name__ == "__main__":
    from PyQt5.QtWidgets import QApplication
    import sys
    app = QApplication(sys.argv)
    editor = CookingEditor()
    editor.setWindowTitle("Cooking Recipe Editor")
    editor.resize(950, 540)
    editor.show()
    sys.exit(app.exec_())
