#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Starborn — Quest Editor (manual Save, dirty marker, safe selection)

What this gives you:
- Manual Save only (no autosave). Add/Duplicate/Delete just mark the editor dirty.
- Dirty indicator: window title gains a '*' when there are unsaved changes.
- Switching selection applies your current form edits to the in-memory model
  (keeps dirty) so you don't lose work, but file isn't written until you press Save.
- Works from /tools or root (auto-resolves project root).
- Fields match your quests.json schema.
- Validate checks basic references.
- Fixes Qt layout warnings by setting top-level layout once.

Usage:
    pip install PyQt5
    python quest_editor.py
"""

from __future__ import annotations
import os, sys, json, shutil
from pathlib import Path
from typing import Dict, List, Any

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (
    QApplication, QWidget, QSplitter, QListWidget, QListWidgetItem, QLineEdit, QTextEdit,
    QVBoxLayout, QHBoxLayout, QFormLayout, QLabel, QPushButton, QMessageBox, QComboBox,
    QSpinBox, QInputDialog, QListWidget as QtListWidget, QListWidgetItem as QtListItem,
    QFileDialog
)
from theme_kit import ThemeManager         # optional if you want per-editor theme flips
from data_core import detect_project_root, json_load, json_save, unique_id
from editor_undo import UndoManager
from ui_common import attach_status_bar, flash_status, attach_hotkeys, attach_list_context_menu, mark_invalid, clear_invalid
from editor_bus import goto as studio_goto, refresh_references as studio_refresh

# -----------------------------
#  Project root resolution
# -----------------------------
def find_project_root(start: Path, target: str = "quests.json", max_up: int = 4) -> Path:
    """Walk up from start until we find a directory containing target."""
    cur = start.resolve()
    for _ in range(max_up + 1):
        if (cur / target).exists():
            return cur
        cur = cur.parent
    # Fallback to start if not found; saving will error with a nice message.
    return start.resolve()

# -----------------------------
#  Helpers for loading lists
# -----------------------------
def _load_json_list(path: Path) -> list:
    try:
        with path.open("r", encoding="utf-8") as f:
            data = json.load(f)
            return data if isinstance(data, list) else []
    except Exception:
        return []

def _collect_item_ids(root_dir: Path) -> List[str]:
    items_path = root_dir / "items.json"
    items = _load_json_list(items_path)
    seen = set()
    for it in items:
        # Prefer name, fallback to id; include aliases
        nm = (it.get("name") or it.get("id") or "").strip()
        if nm:
            seen.add(nm)
        for a in it.get("aliases", []):
            if a:
                seen.add(str(a).strip())
    return sorted(seen, key=str.lower)

def _collect_npc_ids(root_dir: Path) -> List[str]:
    # Optional file; allow free-type if not present
    npcs_path = root_dir / "npcs.json"
    npcs = _load_json_list(npcs_path)
    seen = set()
    for n in npcs:
        nm = (n.get("name") or n.get("id") or "").strip()
        if nm:
            seen.add(nm)
        for a in n.get("aliases", []):
            if a:
                seen.add(str(a).strip())
    return sorted(seen, key=str.lower)

def _collect_quest_ids(quests_by_id: Dict[str, dict]) -> List[str]:
    return sorted(quests_by_id.keys(), key=str.lower)

def _collect_dialogue_ids(root_dir: Path) -> List[str]:
    # Optional: load from dialogue.json if present; free-type otherwise
    dlg_path = root_dir / "dialogue.json"
    if not dlg_path.exists():
        return []
    data = _load_json_list(dlg_path)
    seen = set()
    for d in data:
        did = (d.get("id") or "").strip()
        if did:
            seen.add(did)
    return sorted(seen, key=str.lower)

ALLOWED_TYPES  = ["main", "side", "character", "fetch", "explore", "talk", "defeat", "craft", "interact"]
ALLOWED_STATUS = ["inactive", "active", "complete"]

# -----------------------------
#  Editor widget
# -----------------------------
class QuestEditor(QWidget):
    def __init__(self, start_dir: Path | str | None = None):
        super().__init__()
        self._base_title = "Starborn Quest Editor"
        self.setWindowTitle(self._base_title)

        # Resolve project root (works from /tools or root)
        if start_dir is None:
            start_dir = Path(__file__).parent
        self.start_dir = Path(start_dir)
        self.root: Path = find_project_root(self.start_dir)

        # Data
        self.quests: Dict[str, dict] = {}
        self.current_id: str | None = None

        # Dirty state
        self._dirty: bool = False                    # unsaved changes in the model (not written to file)
        self._form_dirty: bool = False               # edits in the current form not yet applied to the model

        # Lookups
        self.item_ids: List[str] = []
        self.npc_ids: List[str] = []
        self.dialogue_ids: List[str] = []

        # UI refs
        self.search_box: QLineEdit = None  # type: ignore
        self.list_widget: QListWidget = None  # type: ignore

        # Form widgets
        self.id_label: QLabel = None  # type: ignore
        self.title_edit: QLineEdit = None  # type: ignore
        self.desc_edit: QTextEdit = None  # type: ignore
        self.type_combo: QComboBox = None  # type: ignore
        self.status_combo: QComboBox = None  # type: ignore
        self.giver_combo: QComboBox = None  # type: ignore
        self.receiver_combo: QComboBox = None  # type: ignore
        self.req_item_combo: QComboBox = None  # type: ignore
        self.reward_item_combo: QComboBox = None  # type: ignore
        self.auto_item_combo: QComboBox = None  # type: ignore
        self.xp_spin: QSpinBox = None  # type: ignore
        self.requires_list: QtListWidget = None  # type: ignore
        self.start_dialogue_combo: QComboBox = None  # type: ignore
        self.completion_dialogue_combo: QComboBox = None  # type: ignore

        # Load & build UI
        self.reload_lookups()
        self.loadQuests()
        self.initUI()
        self.refreshList()

    # -------------------------
    #  IO
    # -------------------------
    def reload_lookups(self):
        self.item_ids = _collect_item_ids(self.root)
        self.npc_ids = _collect_npc_ids(self.root)
        self.dialogue_ids = _collect_dialogue_ids(self.root)

    def loadQuests(self):
        path = self.root / "quests.json"
        self.quests.clear()
        try:
            with path.open("r", encoding="utf-8") as f:
                data = json.load(f)
            if isinstance(data, dict):
                # support dict-format {id: quest}
                for k, v in data.items():
                    v["id"] = v.get("id") or k
                    self.quests[v["id"]] = v
            elif isinstance(data, list):
                for q in data:
                    qid = (q.get("id") or "").strip()
                    if not qid:
                        continue
                    self.quests[qid] = q
            else:
                raise ValueError("quests.json must be a list or dict")
        except Exception as e:
            QMessageBox.critical(self, "Load Error", f"Failed to load quests.json:\n{e}")

    def saveQuests(self):
        path = self.root / "quests.json"
        try:
            # backup existing
            if path.exists():
                shutil.copy2(path, path.with_suffix(".json.bak"))

            out = sorted(self.quests.values(), key=lambda q: (q.get("id") or "").lower())
            with path.open("w", encoding="utf-8") as f:
                json.dump(out, f, ensure_ascii=False, indent=4)

            self._dirty = False
            self._update_title_dirty()
            QMessageBox.information(self, "Saved", "quests.json saved successfully.")
        except Exception as e:
            QMessageBox.critical(self, "Save Error", f"Failed to save quests.json:\n{e}")

    # Expose a Studio-friendly alias
    def save(self):
        self.onSave()

    # -------------------------
    #  UI
    # -------------------------
    def initUI(self):
        root_split = QSplitter(Qt.Horizontal)

        # Left: search + list + buttons
        left_panel = QWidget()
        left_layout = QVBoxLayout()
        left_panel.setLayout(left_layout)

        self.search_box = QLineEdit()
        self.search_box.setPlaceholderText("Search by ID or Title…")
        self.search_box.textChanged.connect(self.refreshList)
        left_layout.addWidget(self.search_box)

        self.list_widget = QListWidget()
        self.list_widget.itemSelectionChanged.connect(self._on_list_selection_changed)
        left_layout.addWidget(self.list_widget, 1)

        row = QHBoxLayout()
        save_btn = QPushButton("Save")
        add_btn  = QPushButton("Add")
        template_btn = QPushButton("New from Template")
        dup_btn  = QPushButton("Duplicate")
        del_btn  = QPushButton("Delete")

        save_btn.clicked.connect(self.onSave)
        add_btn.clicked.connect(self.onAdd)
        template_btn.clicked.connect(self.onAddFromTemplate)
        dup_btn.clicked.connect(self.onDuplicate)
        del_btn.clicked.connect(self.onDelete)

        row.addWidget(save_btn)
        row.addWidget(add_btn)
        row.addWidget(template_btn)
        row.addWidget(dup_btn)
        row.addWidget(del_btn)
        left_layout.addLayout(row)

        root_split.addWidget(left_panel)

        # Right: form
        right_panel = QWidget()
        right_layout = QVBoxLayout()
        right_panel.setLayout(right_layout)

        form = QFormLayout()

        self.id_label = QLabel("-")
        form.addRow("ID:", self.id_label)

        self.title_edit = QLineEdit()
        form.addRow("Title:", self.title_edit)

        self.desc_edit = QTextEdit()
        self.desc_edit.setPlaceholderText("Quest description…")
        form.addRow("Description:", self.desc_edit)

        self.type_combo = QComboBox(); self.type_combo.addItems(ALLOWED_TYPES)
        form.addRow("Type:", self.type_combo)

        self.status_combo = QComboBox(); self.status_combo.addItems(ALLOWED_STATUS)
        form.addRow("Status:", self.status_combo)

        # Giver / Receiver (NPC pickers; editable)
        self.giver_combo = QComboBox(); self.giver_combo.setEditable(True)
        self.giver_combo.addItems([""] + self.npc_ids)
        self.receiver_combo = QComboBox(); self.receiver_combo.setEditable(True)
        self.receiver_combo.addItems([""] + self.npc_ids)
        form.addRow("Giver NPC:", self.giver_combo)
        form.addRow("Receiver NPC:", self.receiver_combo)

        # Items
        self.req_item_combo = QComboBox(); self.req_item_combo.setEditable(True)
        self.req_item_combo.addItems([""] + self.item_ids)
        self.reward_item_combo = QComboBox(); self.reward_item_combo.setEditable(True)
        self.reward_item_combo.addItems([""] + self.item_ids)
        form.addRow("Required Item:", self.req_item_combo)
        form.addRow("Reward Item:", self.reward_item_combo)

        # Auto-complete if player already has <item>
        self.auto_item_combo = QComboBox(); self.auto_item_combo.setEditable(True)
        self.auto_item_combo.addItems([""] + self.item_ids)
        form.addRow("Auto-Complete If Has:", self.auto_item_combo)

        # Requires (multi-select quests)
        self.requires_list = QtListWidget()
        self.requires_list.setSelectionMode(QtListWidget.MultiSelection)
        form.addRow("Requires Quests:", self.requires_list)

        # XP
        self.xp_spin = QSpinBox(); self.xp_spin.setRange(0, 999999); self.xp_spin.setSingleStep(10)
        form.addRow("XP Reward:", self.xp_spin)

        # Dialogue links (optional)
        self.start_dialogue_combo = QComboBox(); self.start_dialogue_combo.setEditable(True)
        self.start_dialogue_combo.addItems([""] + self.dialogue_ids)
        form.addRow("Start Dialogue:", self.start_dialogue_combo)

        self.completion_dialogue_combo = QComboBox(); self.completion_dialogue_combo.setEditable(True)
        self.completion_dialogue_combo.addItems([""] + self.dialogue_ids)
        form.addRow("Completion Dialogue:", self.completion_dialogue_combo)

        right_layout.addLayout(form)

        # Save/Validate/Reload buttons
        row2 = QHBoxLayout()
        reload_btn = QPushButton("Reload")
        validate_btn = QPushButton("Validate")
        reload_btn.clicked.connect(self.onReload)
        validate_btn.clicked.connect(self.onValidate)
        row2.addWidget(reload_btn)
        row2.addWidget(validate_btn)
        right_layout.addLayout(row2)

        root_split.addWidget(right_panel)
        root_split.setStretchFactor(0, 1)
        root_split.setStretchFactor(1, 2)

        # Top-level layout — create without parent, then set once (prevents Qt warnings)
        outer = QHBoxLayout()
        outer.addWidget(root_split)
        self.setLayout(outer)

        # Mark form dirty on edits
        self.title_edit.textChanged.connect(self._mark_form_dirty)
        self.desc_edit.textChanged.connect(self._mark_form_dirty)
        self.type_combo.currentTextChanged.connect(self._mark_form_dirty)
        self.status_combo.currentTextChanged.connect(self._mark_form_dirty)
        self.giver_combo.currentTextChanged.connect(self._mark_form_dirty)
        self.receiver_combo.currentTextChanged.connect(self._mark_form_dirty)
        self.req_item_combo.currentTextChanged.connect(self._mark_form_dirty)
        self.reward_item_combo.currentTextChanged.connect(self._mark_form_dirty)
        self.auto_item_combo.currentTextChanged.connect(self._mark_form_dirty)
        self.xp_spin.valueChanged.connect(self._mark_form_dirty)
        self.requires_list.itemSelectionChanged.connect(self._mark_form_dirty)
        self.start_dialogue_combo.currentTextChanged.connect(self._mark_form_dirty)
        self.completion_dialogue_combo.currentTextChanged.connect(self._mark_form_dirty)

    # -------------------------
    #  Dirty helpers
    # -------------------------
    def _mark_form_dirty(self, *args):
        self._form_dirty = True
        self._update_title_dirty()

    def _mark_model_dirty(self):
        self._dirty = True
        self._update_title_dirty()

    def _update_title_dirty(self):
        star = "*" if (self._dirty or self._form_dirty) else ""
        self.setWindowTitle(f"{self._base_title}{star}")

    # -------------------------
    #  List + selection
    # -------------------------
    def refreshList(self):
        """Refresh left list based on search filter; keep selection if possible."""
        sel = self.current_id
        self.list_widget.clear()

        q = (self.search_box.text() or "").strip().lower()
        for qid in _collect_quest_ids(self.quests):
            qobj = self.quests[qid]
            title = (qobj.get("title") or "").lower()
            if q and (q not in qid.lower()) and (q not in title):
                continue
            item = QListWidgetItem(qid)
            self.list_widget.addItem(item)
            if qid == sel:
                item.setSelected(True)

        # If nothing selected, clear the form
        if sel and sel in self.quests:
            self._load_into_form(sel)
        elif self.list_widget.count() and not self.list_widget.selectedItems():
            self.list_widget.setCurrentRow(0)
        else:
            self._clear_form()

    def _on_list_selection_changed(self):
        items = self.list_widget.selectedItems()
        if not items:
            # Apply pending edits to model for the old selection before clearing
            self._maybe_apply_current_form_to_model()
            self.current_id = None
            self._clear_form()
            return

        # Before switching, apply edits from the previous quest into the model (memory only)
        self._maybe_apply_current_form_to_model()

        qid = items[0].text()
        self.current_id = qid
        self._load_into_form(qid)

    # Apply current form edits to the in-memory model (does NOT write file)
    def _maybe_apply_current_form_to_model(self):
        if not self.current_id or not self._form_dirty:
            return
        self._apply_form_to_model(self.current_id)
        self._form_dirty = False
        self._mark_model_dirty()

    # -------------------------
    #  Form helpers
    # -------------------------
    def _clear_form(self):
        self.id_label.setText("-")
        self.title_edit.setText("")
        self.desc_edit.setPlainText("")
        self.type_combo.setCurrentText("side")
        self.status_combo.setCurrentText("inactive")
        self.giver_combo.setCurrentText("")
        self.receiver_combo.setCurrentText("")
        self.req_item_combo.setCurrentText("")
        self.reward_item_combo.setCurrentText("")
        self.auto_item_combo.setCurrentText("")
        self.xp_spin.setValue(0)
        self.requires_list.clear()
        # repopulate all quest IDs (no selection)
        all_ids = _collect_quest_ids(self.quests)
        for qx in all_ids:
            self.requires_list.addItem(QtListItem(qx))
        self.start_dialogue_combo.setCurrentText("")
        self.completion_dialogue_combo.setCurrentText("")
        # Clearing the form isn't a data change
        self._form_dirty = False
        self._update_title_dirty()

    def _load_into_form(self, qid: str):
        q = self.quests.get(qid, {})
        self.id_label.setText(qid)
        self.title_edit.setText(q.get("title", ""))
        self.desc_edit.setPlainText(q.get("description", ""))

        self.type_combo.setCurrentText(q.get("type", "side") or "side")
        self.status_combo.setCurrentText(q.get("status", "inactive") or "inactive")

        self.giver_combo.setCurrentText(q.get("giver", "") or "")
        self.receiver_combo.setCurrentText(q.get("receiver", "") or "")

        self.req_item_combo.setCurrentText(q.get("required_item", "") or "")
        self.reward_item_combo.setCurrentText(q.get("reward_item", "") or "")
        self.auto_item_combo.setCurrentText(q.get("auto_complete_if_has", "") or "")

        self.xp_spin.setValue(int(q.get("xp_reward", 0) or 0))

        # Requires: accept string or list
        requires = q.get("requires", [])
        if isinstance(requires, str):
            requires = [requires] if requires else []

        # Populate requires list with all quest IDs and select current ones
        self.requires_list.clear()
        all_ids = _collect_quest_ids(self.quests)
        for qx in all_ids:
            it = QtListItem(qx)
            it.setSelected(qx in requires)
            self.requires_list.addItem(it)

        # Dialogues (optional)
        self.start_dialogue_combo.setCurrentText(q.get("start_dialogue", "") or "")
        self.completion_dialogue_combo.setCurrentText(q.get("completion_dialogue", "") or "")

        # Loaded form matches model → not dirty yet
        self._form_dirty = False
        self._update_title_dirty()

    def _apply_form_to_model(self, qid: str):
        """Apply current form widgets into self.quests[qid]. Does NOT save to disk."""
        if qid not in self.quests:
            return
        q = self.quests[qid]

        # Basic fields
        q["id"] = qid
        q["title"] = self.title_edit.text().strip()
        q["description"] = self.desc_edit.toPlainText()
        q["type"] = self.type_combo.currentText()
        q["status"] = self.status_combo.currentText()

        # NPCs
        q["giver"] = self.giver_combo.currentText().strip()
        q["receiver"] = self.receiver_combo.currentText().strip()

        # Items
        q["required_item"] = self.req_item_combo.currentText().strip()
        q["reward_item"] = self.reward_item_combo.currentText().strip()
        auto_has = self.auto_item_combo.currentText().strip()
        if auto_has:
            q["auto_complete_if_has"] = auto_has
        else:
            q.pop("auto_complete_if_has", None)

        # XP
        q["xp_reward"] = int(self.xp_spin.value())

        # Requires
        selected = [
            self.requires_list.item(i).text()
            for i in range(self.requires_list.count())
            if self.requires_list.item(i).isSelected()
        ]
        if len(selected) == 0:
            q.pop("requires", None)
        elif len(selected) == 1:
            q["requires"] = selected[0]
        else:
            q["requires"] = selected

        # Dialogues (optional)
        sd = self.start_dialogue_combo.currentText().strip()
        cd = self.completion_dialogue_combo.currentText().strip()
        if sd:
            q["start_dialogue"] = sd
        else:
            q.pop("start_dialogue", None)
        if cd:
            q["completion_dialogue"] = cd
        else:
            q.pop("completion_dialogue", None)

    # -------------------------
    #  Actions
    # -------------------------
    def onAdd(self):
        new_id, ok = QInputDialog.getText(self, "Add Quest", "Enter new quest ID:")
        if not ok or not new_id:
            return
        new_id = new_id.strip()
        if not new_id:
            return
        if new_id in self.quests:
            QMessageBox.warning(self, "Add Quest", "Quest ID already exists.")
            return
        # Apply pending edits from current form before switching
        self._maybe_apply_current_form_to_model()

        self.quests[new_id] = {
            "id": new_id,
            "title": "",
            "status": "inactive",
            "type": "side",
            "giver": "",
            "receiver": "",
            "description": "",
            "xp_reward": 0,
            "required_item": "",
            "reward_item": ""
        }
        self.current_id = new_id
        self.refreshList()
        self._mark_model_dirty()   # manual save required

    def onAddFromTemplate(self):
        template_path, _ = QFileDialog.getOpenFileName(
            self, "Select Quest Template", str(self.root / "templates"), "JSON Files (*.json)"
        )
        if not template_path:
            return

        try:
            with open(template_path, "r", encoding="utf-8") as f:
                template_data = json.load(f)
        except Exception as e:
            QMessageBox.critical(self, "Template Error", f"Failed to load or parse template:\n{e}")
            return

        new_id, ok = QInputDialog.getText(self, "New Quest from Template", "Enter new quest ID:")
        if not ok or not new_id:
            return
        new_id = new_id.strip()
        if not new_id:
            return
        if new_id in self.quests:
            QMessageBox.warning(self, "ID Error", "Quest ID already exists.")
            return

        self._maybe_apply_current_form_to_model()

        new_quest = json.loads(json.dumps(template_data))  # Deep copy
        new_quest["id"] = new_id

        self.quests[new_id] = new_quest
        self.current_id = new_id
        self.refreshList()
        self._mark_model_dirty()

    def onDuplicate(self):
        if not self.current_id:
            return
        src = self.quests[self.current_id]
        new_id, ok = QInputDialog.getText(self, "Duplicate Quest", "Enter new quest ID:")
        if not ok or not new_id:
            return
        new_id = new_id.strip()
        if not new_id:
            return
        if new_id in self.quests:
            QMessageBox.warning(self, "Duplicate Quest", "Quest ID already exists.")
            return

        # Apply pending edits from current form to the source quest first
        self._maybe_apply_current_form_to_model()

        dup = json.loads(json.dumps(src))  # deep copy
        dup["id"] = new_id
        self.quests[new_id] = dup
        self.current_id = new_id
        self.refreshList()
        self._mark_model_dirty()   # manual save required

    def onDelete(self):
        if not self.current_id:
            QMessageBox.information(self, "Delete Quest", "Select a quest to delete.")
            return
        qid = self.current_id
        if QMessageBox.question(self, "Delete Quest", f"Delete '{qid}'?") != QMessageBox.Yes:
            return

        # Apply any pending edits to the model before removing (keeps consistency)
        self._maybe_apply_current_form_to_model()

        self.quests.pop(qid, None)
        self.current_id = None
        self.refreshList()
        self._mark_model_dirty()   # manual save required

    def onSave(self):
        # Before writing, apply any pending edits from the current form to the model
        self._maybe_apply_current_form_to_model()
        self.saveQuests()

    def onReload(self):
        # Discard unsaved model changes and reload from disk
        if (self._dirty or self._form_dirty) and QMessageBox.question(
            self, "Discard changes?",
            "Discard all unsaved changes and reload from disk?"
        ) != QMessageBox.Yes:
            return
        self._dirty = False
        self._form_dirty = False
        self.loadQuests()
        self.refreshList()
        self._update_title_dirty()

    def onValidate(self):
        issues = self.validate()
        if issues:
            QMessageBox.warning(self, "Validation", "\n".join(issues[:200]))
        else:
            QMessageBox.information(self, "Validation", "Looks good!")

    # Studio will call this if present
    def validate(self) -> List[str]:
        issues: List[str] = []

        items = set(self.item_ids)
        npcs = set(self.npc_ids)
        qids = set(self.quests.keys())

        # Duplicate ID check (paranoid)
        if len(qids) != len(self.quests):
            issues.append("Duplicate quest IDs detected (unexpected state).")

        for q in self.quests.values():
            qid = q.get("id") or ""
            if not qid:
                issues.append("Quest without 'id'.")
                continue

            # Title present
            if not (q.get("title") or "").strip():
                issues.append(f"{qid}: empty title")

            # Type/Status validity
            t = q.get("type", "side")
            if t not in ALLOWED_TYPES:
                issues.append(f"{qid}: unknown type '{t}' (allowed: {', '.join(ALLOWED_TYPES)})")
            s = q.get("status", "inactive")
            if s not in ALLOWED_STATUS:
                issues.append(f"{qid}: unknown status '{s}' (allowed: {', '.join(ALLOWED_STATUS)})")

            # NPC refs (allow free-typed if npcs.json is missing; warn if list exists and not found)
            g = (q.get("giver") or "").strip()
            r = (q.get("receiver") or "").strip()
            if self.npc_ids:
                if g and g not in npcs:
                    issues.append(f"{qid}: unknown giver NPC '{g}'")
                if r and r not in npcs:
                    issues.append(f"{qid}: unknown receiver NPC '{r}'")

            # Item refs
            req_it = (q.get("required_item") or "").strip()
            rew_it = (q.get("reward_item") or "").strip()
            auto   = (q.get("auto_complete_if_has") or "").strip()
            if req_it and items and req_it not in items:
                issues.append(f"{qid}: unknown required_item '{req_it}'")
            if rew_it and items and rew_it not in items:
                issues.append(f"{qid}: unknown reward_item '{rew_it}'")
            if auto and items and auto not in items:
                issues.append(f"{qid}: unknown auto_complete_if_has '{auto}'")

            # Requires quest id(s)
            req = q.get("requires")
            if isinstance(req, str):
                if req and req not in qids:
                    issues.append(f"{qid}: requires unknown quest '{req}'")
            elif isinstance(req, list):
                for rqq in req:
                    if rqq not in qids:
                        issues.append(f"{qid}: requires unknown quest '{rqq}'")
            elif req is not None:
                issues.append(f"{qid}: 'requires' must be string, list, or absent")

            # xp_reward numeric
            try:
                int(q.get("xp_reward", 0))
            except Exception:
                issues.append(f"{qid}: xp_reward must be an integer")

        return issues

# -----------------------------
#  Entrypoint
# -----------------------------
def _standalone_main():
    app = QApplication(sys.argv)
    editor = QuestEditor(Path(__file__).parent)
    editor.resize(1100, 700)
    editor.show()
    sys.exit(app.exec_())

if __name__ == "__main__":
    _standalone_main()
