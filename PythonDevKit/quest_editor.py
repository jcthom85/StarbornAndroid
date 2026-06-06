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
    QInputDialog, QFileDialog
)
from theme_kit import ThemeManager         # optional if you want per-editor theme flips
from data_core import json_load, json_save, unique_id
from devkit_paths import resolve_paths
from editor_undo import UndoManager
from ui_common import attach_status_bar, flash_status, attach_hotkeys, attach_list_context_menu, mark_invalid, clear_invalid
from editor_bus import goto as studio_goto, refresh_references as studio_refresh

# -----------------------------
#  Project root resolution
# -----------------------------
def find_project_root(start: Path, target: str = "quests.json", max_up: int = 4) -> Path:
    """Resolve the assets directory that holds quests.json."""
    return resolve_paths(start).assets_dir

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

def _collect_hub_ids(root_dir: Path) -> List[str]:
    hubs_path = root_dir / "hubs.json"
    hubs = _load_json_list(hubs_path)
    seen = set()
    for h in hubs:
        hid = (h.get("id") or "").strip()
        if hid:
            seen.add(hid)
    return sorted(seen, key=str.lower)

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
        self.hub_ids: List[str] = []

        # UI refs
        self.search_box: QLineEdit = None  # type: ignore
        self.list_widget: QListWidget = None  # type: ignore

        # Form widgets
        self.id_label: QLabel = None  # type: ignore
        self.title_edit: QLineEdit = None  # type: ignore
        self.summary_edit: QTextEdit = None  # type: ignore
        self.desc_edit: QTextEdit = None  # type: ignore
        self.flavor_edit: QTextEdit = None  # type: ignore
        self.giver_combo: QComboBox = None  # type: ignore
        self.hub_combo: QComboBox = None  # type: ignore
        self.prereq_combo: QComboBox = None  # type: ignore
        self.rewards_edit: QTextEdit = None  # type: ignore
        self.stages_edit: QTextEdit = None  # type: ignore

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
        self.hub_ids = _collect_hub_ids(self.root)

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

        self.summary_edit = QTextEdit()
        self.summary_edit.setPlaceholderText("Quest summary…")
        form.addRow("Summary:", self.summary_edit)

        self.desc_edit = QTextEdit()
        self.desc_edit.setPlaceholderText("Quest description…")
        form.addRow("Description:", self.desc_edit)

        self.flavor_edit = QTextEdit()
        self.flavor_edit.setPlaceholderText("Flavor / VO direction / lore…")
        form.addRow("Flavor:", self.flavor_edit)

        # Giver / Hub / Prereq
        self.giver_combo = QComboBox(); self.giver_combo.setEditable(True)
        self.giver_combo.addItems([""] + self.npc_ids)
        self.hub_combo = QComboBox(); self.hub_combo.setEditable(True)
        self.hub_combo.addItems([""] + self.hub_ids)
        self.prereq_combo = QComboBox(); self.prereq_combo.setEditable(True)
        self.prereq_combo.addItems([""] + _collect_quest_ids(self.quests))
        form.addRow("Giver NPC:", self.giver_combo)
        form.addRow("Hub ID:", self.hub_combo)
        form.addRow("Prereq Quest:", self.prereq_combo)

        # Rewards / Stages (raw JSON)
        self.rewards_edit = QTextEdit()
        self.rewards_edit.setPlaceholderText("JSON array of rewards…")
        form.addRow("Rewards (JSON):", self.rewards_edit)
        self.stages_edit = QTextEdit()
        self.stages_edit.setPlaceholderText("JSON array of stages…")
        form.addRow("Stages (JSON):", self.stages_edit)

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
        self.summary_edit.textChanged.connect(self._mark_form_dirty)
        self.desc_edit.textChanged.connect(self._mark_form_dirty)
        self.flavor_edit.textChanged.connect(self._mark_form_dirty)
        self.giver_combo.currentTextChanged.connect(self._mark_form_dirty)
        self.hub_combo.currentTextChanged.connect(self._mark_form_dirty)
        self.prereq_combo.currentTextChanged.connect(self._mark_form_dirty)
        self.rewards_edit.textChanged.connect(self._mark_form_dirty)
        self.stages_edit.textChanged.connect(self._mark_form_dirty)

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

        # Update prereq dropdown with current quest ids
        if self.prereq_combo is not None:
            prev = self.prereq_combo.currentText()
            self.prereq_combo.blockSignals(True)
            self.prereq_combo.clear()
            self.prereq_combo.addItems([""] + _collect_quest_ids(self.quests))
            if prev:
                self.prereq_combo.setCurrentText(prev)
            self.prereq_combo.blockSignals(False)

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

    def _select_id(self, qid: str):
        for i in range(self.list_widget.count()):
            if self.list_widget.item(i).text() == qid:
                self.list_widget.blockSignals(True)
                self.list_widget.setCurrentRow(i)
                self.list_widget.blockSignals(False)
                return

    def _on_list_selection_changed(self):
        items = self.list_widget.selectedItems()
        if not items:
            # Apply pending edits to model for the old selection before clearing
            if not self._maybe_apply_current_form_to_model():
                # keep selection on current item if JSON invalid
                if self.current_id:
                    self._select_id(self.current_id)
                return
            self.current_id = None
            self._clear_form()
            return

        # Before switching, apply edits from the previous quest into the model (memory only)
        if not self._maybe_apply_current_form_to_model():
            if self.current_id:
                self._select_id(self.current_id)
            return

        qid = items[0].text()
        self.current_id = qid
        self._load_into_form(qid)

    # Apply current form edits to the in-memory model (does NOT write file)
    def _maybe_apply_current_form_to_model(self) -> bool:
        if not self.current_id or not self._form_dirty:
            return True
        ok = self._apply_form_to_model(self.current_id)
        if not ok:
            return False
        self._form_dirty = False
        self._mark_model_dirty()
        return True

    # -------------------------
    #  Form helpers
    # -------------------------
    def _clear_form(self):
        self.id_label.setText("-")
        self.title_edit.setText("")
        self.summary_edit.setPlainText("")
        self.desc_edit.setPlainText("")
        self.flavor_edit.setPlainText("")
        self.giver_combo.setCurrentText("")
        self.hub_combo.setCurrentText("")
        self.prereq_combo.setCurrentText("")
        self.rewards_edit.setPlainText("[]")
        self.stages_edit.setPlainText("[]")
        # Clearing the form isn't a data change
        self._form_dirty = False
        self._update_title_dirty()

    def _load_into_form(self, qid: str):
        q = self.quests.get(qid, {})
        self.id_label.setText(qid)
        self.title_edit.setText(q.get("title", ""))
        self.summary_edit.setPlainText(q.get("summary", ""))
        self.desc_edit.setPlainText(q.get("description", ""))
        self.flavor_edit.setPlainText(q.get("flavor", ""))

        self.giver_combo.setCurrentText(q.get("giver", "") or "")
        self.hub_combo.setCurrentText(q.get("hub_id", "") or "")
        self.prereq_combo.setCurrentText(q.get("prereq_quest_id", "") or "")

        try:
            self.rewards_edit.setPlainText(json.dumps(q.get("rewards", []) or [], ensure_ascii=False, indent=2))
        except Exception:
            self.rewards_edit.setPlainText("[]")
        try:
            self.stages_edit.setPlainText(json.dumps(q.get("stages", []) or [], ensure_ascii=False, indent=2))
        except Exception:
            self.stages_edit.setPlainText("[]")

        # Loaded form matches model → not dirty yet
        self._form_dirty = False
        self._update_title_dirty()

    def _apply_form_to_model(self, qid: str) -> bool:
        """Apply current form widgets into self.quests[qid]. Does NOT save to disk."""
        if qid not in self.quests:
            return False
        q = self.quests[qid]

        # Parse JSON fields
        try:
            rewards_text = self.rewards_edit.toPlainText().strip()
            rewards = json.loads(rewards_text) if rewards_text else []
            if rewards is None:
                rewards = []
            if not isinstance(rewards, list):
                raise ValueError("Rewards must be a JSON array.")
        except Exception as exc:
            QMessageBox.warning(self, "Invalid Rewards JSON", f"{exc}")
            return False

        try:
            stages_text = self.stages_edit.toPlainText().strip()
            stages = json.loads(stages_text) if stages_text else []
            if stages is None:
                stages = []
            if not isinstance(stages, list):
                raise ValueError("Stages must be a JSON array.")
        except Exception as exc:
            QMessageBox.warning(self, "Invalid Stages JSON", f"{exc}")
            return False

        # Basic fields
        q["id"] = qid
        q["title"] = self.title_edit.text().strip()
        q["summary"] = self.summary_edit.toPlainText()
        q["description"] = self.desc_edit.toPlainText()
        q["flavor"] = self.flavor_edit.toPlainText()

        # NPCs / Hub / Prereq
        q["giver"] = self.giver_combo.currentText().strip()
        q["hub_id"] = self.hub_combo.currentText().strip()
        prereq = self.prereq_combo.currentText().strip()
        if prereq:
            q["prereq_quest_id"] = prereq
        else:
            q.pop("prereq_quest_id", None)

        # Structured lists
        q["rewards"] = rewards
        q["stages"] = stages
        return True

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
        if not self._maybe_apply_current_form_to_model():
            return

        self.quests[new_id] = {
            "id": new_id,
            "title": "",
            "summary": "",
            "giver": "",
            "description": "",
            "flavor": "",
            "hub_id": "",
            "stages": [],
            "rewards": []
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

        if not self._maybe_apply_current_form_to_model():
            return

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
        if not self._maybe_apply_current_form_to_model():
            return

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
        if not self._maybe_apply_current_form_to_model():
            return

        self.quests.pop(qid, None)
        self.current_id = None
        self.refreshList()
        self._mark_model_dirty()   # manual save required

    def onSave(self):
        # Before writing, apply any pending edits from the current form to the model
        if not self._maybe_apply_current_form_to_model():
            return
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

            # NPC refs (allow free-typed if npcs.json is missing; warn if list exists and not found)
            g = (q.get("giver") or "").strip()
            if self.npc_ids:
                if g and g not in npcs:
                    issues.append(f"{qid}: unknown giver NPC '{g}'")
            # Hub / prereq refs
            hub_id = (q.get("hub_id") or "").strip()
            if hub_id and self.hub_ids and hub_id not in self.hub_ids:
                issues.append(f"{qid}: unknown hub_id '{hub_id}'")
            prereq = (q.get("prereq_quest_id") or "").strip()
            if prereq and prereq not in qids:
                issues.append(f"{qid}: unknown prereq_quest_id '{prereq}'")

            # Rewards list
            rewards = q.get("rewards")
            if not isinstance(rewards, list):
                issues.append(f"{qid}: rewards must be a list")
            else:
                for ridx, r in enumerate(rewards):
                    if not isinstance(r, dict):
                        issues.append(f"{qid}: reward[{ridx}] must be an object")
                        continue
                    rtype = r.get("type")
                    if rtype == "item":
                        iid = r.get("item_id")
                        if iid and items and iid not in items:
                            issues.append(f"{qid}: reward[{ridx}] unknown item '{iid}'")
                    elif rtype == "xp":
                        if r.get("amount") is None:
                            issues.append(f"{qid}: reward[{ridx}] missing amount")
                    elif rtype:
                        issues.append(f"{qid}: reward[{ridx}] unknown type '{rtype}'")

            # Stages/tasks
            stages = q.get("stages")
            if not isinstance(stages, list):
                issues.append(f"{qid}: stages must be a list")
            else:
                for sidx, st in enumerate(stages):
                    if not isinstance(st, dict):
                        issues.append(f"{qid}: stage[{sidx}] must be an object")
                        continue
                    sid = (st.get("id") or "").strip()
                    if not sid:
                        issues.append(f"{qid}: stage[{sidx}] missing id")
                    tasks = st.get("tasks") or []
                    if not isinstance(tasks, list):
                        issues.append(f"{qid}: stage[{sidx}] tasks must be a list")
                        continue
                    for tidx, task in enumerate(tasks):
                        if not isinstance(task, dict):
                            issues.append(f"{qid}: stage[{sidx}] task[{tidx}] must be an object")
                            continue
                        tid = (task.get("id") or "").strip()
                        if not tid:
                            issues.append(f"{qid}: stage[{sidx}] task[{tidx}] missing id")
                        ttxt = (task.get("text") or "").strip()
                        if not ttxt:
                            issues.append(f"{qid}: stage[{sidx}] task[{tidx}] missing text")

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
