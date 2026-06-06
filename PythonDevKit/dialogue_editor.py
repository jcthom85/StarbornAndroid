#!/usr/bin/env python3
"""
Starborn — Dialogue Editor (v3.2)
• Unified UX: search, add/dup/delete/rename, per-entry form + raw JSON tab.
• Smart lookups: NPC names, quest/milestone/item ids for helper builders.
• Validate: unknown speakers, missing text, dangling next, dups, basic loops.
• Safe saves with .bak; pretty-printed, sorted by id.
• Exposes .save() and .validate() for Studio integration.
"""
from __future__ import annotations
import os, sys, json, shutil
from pathlib import Path
from typing import Dict, List, Any, Optional, Tuple

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (
    QApplication, QWidget, QHBoxLayout, QVBoxLayout, QSplitter,
    QListWidget, QListWidgetItem, QLineEdit, QPushButton, QLabel,
    QFormLayout, QTextEdit, QMessageBox, QComboBox, QTabWidget,
    QInputDialog, QGroupBox, QGridLayout, QDoubleSpinBox
)

# Local helpers (no "file_manager.py" used)
from starborn_data import (
    find_project_root, read_list_json, write_json_with_backup,
    collect_npc_names, collect_item_names, collect_quest_ids, collect_milestone_ids,
    validate_dialogue, collect_dialogue_ids
)
from scope_utils import ScopeIndex, scope_prefix, scoped_id
from editor_undo import UndoManager

class DialogueEditor(QWidget):
    def __init__(self, project_root: Optional[str] = None):
        super().__init__()
        self.setWindowTitle("Starborn — Dialogue")
        self.root = find_project_root(Path(project_root) if project_root else Path(__file__).parent)

        self.scope_index = ScopeIndex.from_assets(self.root)
        self.world_ids = self.scope_index.world_ids
        self.hubs_by_world = self.scope_index.hubs_by_world

        self.dialogue: Dict[str, dict] = {}
        self.current_id: Optional[str] = None

        self.npc_names: List[str] = []
        self.quest_ids: List[str] = []
        self.milestone_ids: List[str] = []
        self.item_names: List[str] = []

        self.undo_manager = UndoManager()
        self._load_all()
        self._build_ui()
        self._wire_undo()
        self._reload_list()

    # -------- Undo --------
    def _wire_undo(self):
        um = self.undo_manager
        um.watch_combo(self.f_speaker)
        um.watch_plain_text(self.f_text)
        um.watch_line_edit(self.f_emote)
        um.watch_line_edit(self.f_portrait)
        um.watch_line_edit(self.f_vo_cue)
        um.watch_spin(self.f_pitch)
        um.watch_spin(self.f_resonance)
        um.watch_line_edit(self.f_chord)
        um.watch_line_edit(self.f_condition)
        um.watch_line_edit(self.f_trigger)
        um.watch_combo(self.f_next)
        um.watch_plain_text(self.f_options)

    # -------- I/O --------
    @property
    def dialogue_path(self) -> Path: return self.root / "dialogue.json"

    def _load_all(self):
        raw = read_list_json(self.dialogue_path)
        self.dialogue = {}
        for d in raw:
            did = (d.get("id") or "").strip()
            if did:
                self.dialogue[did] = d
        self.npc_names = collect_npc_names(self.root)
        self.quest_ids = collect_quest_ids(self.root)
        self.milestone_ids = collect_milestone_ids(self.root)
        self.item_names = collect_item_names(self.root)

    def save(self) -> bool:
        """Studio calls this."""
        return self._save_all()

    def _save_all(self) -> bool:
        out = [self.dialogue[k] for k in sorted(self.dialogue.keys(), key=str.lower)]
        ok = write_json_with_backup(self.dialogue_path, out)
        if ok:
            QMessageBox.information(self, "Saved", "dialogue.json saved.")
        else:
            QMessageBox.critical(self, "Save error", "Failed to save dialogue.json (see console).")
        return ok

    def validate(self) -> List[str]:
        """Studio calls this."""
        all_ids = list(self.dialogue.keys())
        return validate_dialogue(list(self.dialogue.values()), self.npc_names, all_ids)

    def _current_scope_filter(self) -> Tuple[str, str]:
        world_id = ""
        hub_id = ""
        if hasattr(self, "scope_world"):
            world_id = self.scope_world.currentText()
            if world_id == "All":
                world_id = ""
        if hasattr(self, "scope_hub"):
            hub_id = self.scope_hub.currentText()
            if hub_id == "All":
                hub_id = ""
        if hub_id and not world_id:
            world_id = self.scope_index.hub_to_world.get(hub_id, "")
        return world_id, hub_id

    def _scope_prefix(self) -> str:
        world_id, hub_id = self._current_scope_filter()
        return scope_prefix(world_id or None, hub_id or None)

    def _refresh_hub_filter(self):
        if not hasattr(self, "scope_hub"):
            return
        world_id = self.scope_world.currentText() if hasattr(self, "scope_world") else ""
        if world_id == "All":
            world_id = ""
        hubs = ["All"]
        if world_id and self.hubs_by_world:
            hubs += self.hubs_by_world.get(world_id, [])
        else:
            hubs += self.scope_index.hub_ids
        prev = self.scope_hub.currentText() if self.scope_hub.count() else "All"
        self.scope_hub.blockSignals(True)
        self.scope_hub.clear()
        self.scope_hub.addItems(hubs)
        if prev in hubs:
            self.scope_hub.setCurrentText(prev)
        self.scope_hub.blockSignals(False)

    def _on_scope_filter_changed(self):
        self._refresh_hub_filter()
        self._reload_list()

    # -------- UI --------
    def _build_ui(self):
        split = QSplitter(Qt.Horizontal, self)
        root_layout = QHBoxLayout(self)
        root_layout.addWidget(split)

        # LEFT: search + list + row of buttons
        left = QWidget(); l = QVBoxLayout(left)

        filter_row = QHBoxLayout()
        self.scope_world = QComboBox()
        self.scope_world.addItems(["All"] + self.world_ids)
        self.scope_hub = QComboBox()
        self.scope_world.currentTextChanged.connect(self._on_scope_filter_changed)
        self.scope_hub.currentTextChanged.connect(self._reload_list)
        filter_row.addWidget(QLabel("World"))
        filter_row.addWidget(self.scope_world, 1)
        filter_row.addWidget(QLabel("Hub"))
        filter_row.addWidget(self.scope_hub, 1)
        l.addLayout(filter_row)
        self._refresh_hub_filter()

        self.search_box = QLineEdit(); self.search_box.setPlaceholderText("Search by id / speaker / text…")
        self.search_box.textChanged.connect(self._reload_list)
        l.addWidget(self.search_box)

        self.list = QListWidget()
        self.list.itemSelectionChanged.connect(self._on_select)
        l.addWidget(self.list, 1)

        row = QHBoxLayout()
        b_new = QPushButton("New"); b_dup = QPushButton("Duplicate"); b_del = QPushButton("Delete")
        b_ren = QPushButton("Rename…"); b_val = QPushButton("Validate"); b_save = QPushButton("Save")
        b_new.clicked.connect(self._on_new)
        b_dup.clicked.connect(self._on_duplicate)
        b_del.clicked.connect(self._on_delete)
        b_ren.clicked.connect(self._on_rename)
        b_val.clicked.connect(self._on_validate)
        b_save.clicked.connect(self._save_all)
        for b in (b_new,b_dup,b_del,b_ren): row.addWidget(b)
        row.addStretch(1); row.addWidget(b_val); row.addWidget(b_save)
        l.addLayout(row)

        split.addWidget(left)

        # RIGHT: tabs
        self.tabs = QTabWidget()
        split.addWidget(self.tabs)
        split.setStretchFactor(1, 3)

        # ---- Tab: Inspector
        self.tab_form = QWidget(); form = QFormLayout(self.tab_form)
        self.f_id_label = QLabel("-")
        self.f_speaker = QComboBox(); self.f_speaker.setEditable(True); self.f_speaker.addItems(self.npc_names)
        self.f_text = QTextEdit(); self.f_text.setPlaceholderText("Dialogue text…")
        self.f_emote = QLineEdit(); self.f_emote.setPlaceholderText("e.g., angry, sad")
        self.f_portrait = QLineEdit(); self.f_portrait.setPlaceholderText("portrait id or asset key")
        
        # Cosmic Resonance Fields
        cosmic_grp = QGroupBox("Cosmic Resonance / Audio")
        cg = QFormLayout(cosmic_grp)
        self.f_vo_cue = QLineEdit(); self.f_vo_cue.setPlaceholderText("voice id or asset key")
        self.f_pitch = QDoubleSpinBox(); self.f_pitch.setRange(0.1, 5.0); self.f_pitch.setValue(1.0); self.f_pitch.setSingleStep(0.1)
        self.f_resonance = QDoubleSpinBox(); self.f_resonance.setRange(0.0, 1.0); self.f_resonance.setValue(0.0); self.f_resonance.setSingleStep(0.05)
        self.f_chord = QLineEdit(); self.f_chord.setPlaceholderText("e.g. C#m7, ocean_swell")
        cg.addRow("VO Cue:", self.f_vo_cue)
        cg.addRow("Pitch:", self.f_pitch)
        cg.addRow("Resonance:", self.f_resonance)
        cg.addRow("Chord / Aura:", self.f_chord)

        self.f_condition = QLineEdit(); self.f_condition.setPlaceholderText("quest:repair_generator | milestone:locker_open | item:Medkit")
        self.f_trigger   = QLineEdit(); self.f_trigger.setPlaceholderText("start_quest:id | give_item:Name | set_milestone:id")
        self.f_next = QComboBox(); self.f_next.setEditable(True)
        self.f_options = QTextEdit(); self.f_options.setPlaceholderText("JSON array of options…")

        form.addRow(QLabel("<b>ID</b>:"), self.f_id_label)
        form.addRow("Speaker:", self.f_speaker)
        form.addRow(QLabel("Text:")); form.addRow(self.f_text)
        form.addRow("Emote:", self.f_emote)
        form.addRow("Portrait:", self.f_portrait)
        form.addRow(cosmic_grp)
        form.addRow("Condition:", self.f_condition)
        form.addRow("Trigger:",   self.f_trigger)
        form.addRow("Next ID:",   self.f_next)
        form.addRow("Options:", self.f_options)

        # helpers for condition/trigger composition
        helper = QGroupBox("Helpers"); g = QGridLayout(helper)
        self.cb_cond_type = QComboBox(); self.cb_cond_type.addItems(["quest:", "milestone:", "item:", "custom:"])
        self.cb_cond_val  = QComboBox(); self.cb_cond_val.setEditable(True)
        self.cb_trig_type = QComboBox(); self.cb_trig_type.addItems(["start_quest:", "complete_quest:", "set_milestone:", "give_item:", "custom:"])
        self.cb_trig_val  = QComboBox(); self.cb_trig_val.setEditable(True)
        b_add_cond = QPushButton("Append Condition"); b_add_trig = QPushButton("Append Trigger")
        b_add_cond.clicked.connect(self._append_condition)
        b_add_trig.clicked.connect(self._append_trigger)
        g.addWidget(QLabel("Condition type"), 0,0); g.addWidget(self.cb_cond_type, 0,1)
        g.addWidget(QLabel("Value"),          0,2); g.addWidget(self.cb_cond_val, 0,3)
        g.addWidget(b_add_cond,               0,4)
        g.addWidget(QLabel("Trigger type"),   1,0); g.addWidget(self.cb_trig_type, 1,1)
        g.addWidget(QLabel("Value"),          1,2); g.addWidget(self.cb_trig_val, 1,3)
        g.addWidget(b_add_trig,               1,4)

        form.addRow(helper)
        self.tabs.addTab(self.tab_form, "Inspector")

        # ---- Tab: Raw JSON
        self.tab_raw = QWidget(); v = QVBoxLayout(self.tab_raw)
        self.raw_edit = QTextEdit(); v.addWidget(self.raw_edit, 1)
        self.tabs.addTab(self.tab_raw, "Raw JSON")

    # -------- List / selection --------
    def _reload_list(self):
        self.list.clear()
        ft = (self.search_box.text() or "").lower().strip()
        prefix = self._scope_prefix()
        next_ids = collect_dialogue_ids(self.root)
        self.f_next.clear(); self.f_next.addItems(next_ids)

        for did in sorted(self.dialogue.keys(), key=str.lower):
            if prefix and not did.startswith(prefix):
                continue
            d = self.dialogue[did]
            speaker = (d.get("speaker") or "")
            text = (d.get("text") or "")
            hay = f"{did} {speaker} {text}".lower()
            if ft and ft not in hay:
                continue
            item = QListWidgetItem(did)
            self.list.addItem(item)

        # keep selection if possible
        if self.current_id:
            for i in range(self.list.count()):
                if self.list.item(i).text() == self.current_id:
                    self.list.setCurrentRow(i)
                    break

        # update helper values
        self.cb_cond_val.clear(); self.cb_trig_val.clear()
        # fill with union of ids/names we know
        self.cb_cond_val.addItems(self.quest_ids + self.milestone_ids + self.item_names)
        self.cb_trig_val.addItems(self.quest_ids + self.milestone_ids + self.item_names)

    def _on_select(self):
        if not self.list.selectedItems(): return
        # before switching, push form → current record
        self._save_current_form_to_memory()
        self.undo_manager.stack.clear()

        did = self.list.selectedItems()[0].text()
        self.current_id = did
        d = self.dialogue.get(did, {})
        # populate form
        self.f_id_label.setText(did)
        self.f_speaker.setEditText(d.get("speaker",""))
        self.f_text.setPlainText(d.get("text",""))
        self.f_emote.setText(d.get("emote",""))
        self.f_portrait.setText(d.get("portrait",""))
        
        # Cosmic fields population (map legacy 'voice' to vo_cue)
        self.f_vo_cue.setText(d.get("vo_cue", d.get("voice", "")))
        self.f_pitch.setValue(float(d.get("pitch", 1.0)))
        self.f_resonance.setValue(float(d.get("resonance", 0.0)))
        self.f_chord.setText(d.get("chord", ""))

        self.f_condition.setText(d.get("condition",""))
        self.f_trigger.setText(d.get("trigger",""))
        self.f_next.setEditText(d.get("next",""))
        try:
            self.f_options.setPlainText(json.dumps(d.get("options") or [], ensure_ascii=False, indent=2))
        except Exception:
            self.f_options.setPlainText("[]")
        self.raw_edit.setPlainText(json.dumps(d, ensure_ascii=False, indent=4))

    def _save_current_form_to_memory(self):
        if not self.current_id or self.current_id not in self.dialogue: return
        d = self.dialogue[self.current_id]
        d["id"] = self.current_id
        d["speaker"] = self.f_speaker.currentText().strip()
        d["text"] = self.f_text.toPlainText()
        d["emote"] = self.f_emote.text().strip()
        
        if self.f_portrait.text().strip():
            d["portrait"] = self.f_portrait.text().strip()
        else:
            d.pop("portrait", None)

        # Cosmic fields saving
        if self.f_vo_cue.text().strip():
            d["vo_cue"] = self.f_vo_cue.text().strip()
            d.pop("voice", None) # Clean up legacy
        else:
            d.pop("vo_cue", None)
            d.pop("voice", None)
            
        d["pitch"] = round(self.f_pitch.value(), 2)
        d["resonance"] = round(self.f_resonance.value(), 2)
        
        if self.f_chord.text().strip():
            d["chord"] = self.f_chord.text().strip()
        else:
            d.pop("chord", None)

        d["condition"] = self.f_condition.text().strip()
        d["trigger"] = self.f_trigger.text().strip()
        d["next"] = self.f_next.currentText().strip()
        # options JSON
        opt_text = self.f_options.toPlainText().strip()
        if opt_text:
            try:
                parsed = json.loads(opt_text)
                if not isinstance(parsed, list):
                    raise ValueError("Options must be a JSON array.")
                d["options"] = parsed
            except Exception as exc:
                QMessageBox.warning(self, "Options JSON", f"Could not parse options:\n{exc}")
        else:
            d.pop("options", None)
        # keep raw tab in sync
        self.raw_edit.setPlainText(json.dumps(d, ensure_ascii=False, indent=4))

    # -------- Buttons --------
    def _on_new(self):
        prefix = self._scope_prefix()
        did = scoped_id(prefix, "dlg", "line", self.dialogue.keys())
        self.dialogue[did] = {"id": did, "speaker": "", "text": "", "condition": "", "trigger":"", "next":""}
        self._reload_list()
        self._select_id(did)

    def _on_duplicate(self):
        if not self.current_id: return
        src = self.dialogue[self.current_id]
        base = f"{self.current_id}_copy"
        i = 1
        new_id = base
        while new_id in self.dialogue:
            i += 1; new_id = f"{base}{i}"
        dup = json.loads(json.dumps(src))
        dup["id"] = new_id
        self.dialogue[new_id] = dup
        self._reload_list()
        self._select_id(new_id)

    def _on_delete(self):
        if not self.current_id: return
        did = self.current_id
        if QMessageBox.question(self, "Delete", f"Delete dialogue '{did}'? This will not auto-fix any 'next' references.",
                                QMessageBox.Yes|QMessageBox.No) == QMessageBox.Yes:
            self.dialogue.pop(did, None)
            self.current_id = None
            self._reload_list()

    def _on_rename(self):
        if not self.current_id: return
        old = self.current_id
        new, ok = QInputDialog.getText(self, "Rename ID", "New id:", text=old)
        new = (new or "").strip()
        if not ok or not new or new == old: return
        prefix = self._scope_prefix()
        if prefix and not new.startswith(prefix):
            QMessageBox.warning(self, "Scope", f"Dialogue ID must start with '{prefix}' for the current scope.")
            return
        if new in self.dialogue:
            QMessageBox.warning(self, "Exists", f"'{new}' already exists.")
            return
        rec = self.dialogue.pop(old)
        rec["id"] = new
        self.dialogue[new] = rec
        # propagate simple 'next' references
        for d in self.dialogue.values():
            if (d.get("next") or "") == old:
                d["next"] = new
            opts = d.get("options") or []
            if isinstance(opts, list):
                for opt in opts:
                    if isinstance(opt, dict) and (opt.get("next") or "") == old:
                        opt["next"] = new
        self.current_id = new
        self._reload_list()
        self._select_id(new)

    def _on_validate(self):
        self._save_current_form_to_memory()
        issues = self.validate()
        if not issues:
            QMessageBox.information(self, "OK", "No issues found.")
        else:
            QMessageBox.warning(self, "Validation", "• " + "\n• ".join(issues))

    def _append_condition(self):
        t = self.cb_cond_type.currentText()
        v = self.cb_cond_val.currentText().strip()
        if t == "custom:": t = ""
        val = (f"{t}{v}").strip()
        if not val: return
        cur = self.f_condition.text().strip()
        self.f_condition.setText((cur + " " + val).strip())

    def _append_trigger(self):
        t = self.cb_trig_type.currentText()
        v = self.cb_trig_val.currentText().strip()
        if t == "custom:": t = ""
        val = (f"{t}{v}").strip()
        if not val: return
        cur = self.f_trigger.text().strip()
        self.f_trigger.setText((cur + " " + val).strip())

    def _on_save_dialogue_entry(self):
        """Saves the currently edited dialogue entry to memory and then to file."""
        if not self.current_id:
            QMessageBox.information(self, "No selection", "Pick a dialogue entry to save.")
            return

        # Apply form changes to the in-memory model
        self._save_current_form_to_memory()

        # The ID might have been changed in the form, but the key in `self.dialogue` is still the old one.
        # We need to handle the rename.
        old_id = self.current_id
        new_id = self.dialogue[old_id].get("id", old_id)

        if new_id != old_id:
            if new_id in self.dialogue and self.dialogue[new_id] is not self.dialogue[old_id]:
                QMessageBox.warning(self, "Exists", f"A dialogue with id '{new_id}' already exists.")
                self.dialogue[old_id]["id"] = old_id # revert
                return
            self.dialogue[new_id] = self.dialogue.pop(old_id)
            self.current_id = new_id

        self._save_all()

    def _select_id(self, did: str):
        for i in range(self.list.count()):
            if self.list.item(i).text() == did:
                self.list.setCurrentRow(i)
                break

# ----- Entrypoint -----
def run(project_root: Optional[str] = None):
    app = QApplication(sys.argv)
    w = DialogueEditor(project_root)
    w.resize(1200, 700)
    w.show()
    sys.exit(app.exec_())

if __name__ == "__main__":
    run(sys.argv[1] if len(sys.argv) > 1 else None)
