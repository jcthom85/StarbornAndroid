#!/usr/bin/env python3
"""
Starborn — Event Editor (schema-aware)
- Matches events.json used by event_manager.py
- Build typed triggers and an actions tree with nested conditional "do" blocks
- Supports on_message/off_message (for toggle events), xp_reward, repeatable, description, notes
- Pulls known IDs from rooms.json, items.json, enemies.json, quests.json, cinematics.json when available

Usage:
  python event_editor.py [PROJECT_ROOT]

Studio integration:
  - import event_editor; event_editor.launch_from_studio(root_dir)
"""

import os, sys, json
from typing import Any, Dict, List, Optional

from devkit_paths import resolve_paths

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (
    QApplication, QWidget, QHBoxLayout, QVBoxLayout, QFormLayout, QGridLayout,
    QListWidget, QListWidgetItem, QLabel, QLineEdit, QTextEdit, QCheckBox,
    QPushButton, QComboBox, QSpinBox, QTabWidget, QMessageBox, QTreeWidget,
    QTreeWidgetItem, QDialog, QDialogButtonBox, QFileDialog
)
from theme_kit import ThemeManager         # optional if you want per-editor theme flips
from data_core import json_load, json_save, unique_id
from editor_undo import UndoManager
from ui_common import attach_status_bar, flash_status, attach_hotkeys, attach_list_context_menu, mark_invalid, clear_invalid
from editor_bus import goto as studio_goto, refresh_references as studio_refresh

# --------------------------
# Helpers to load resources
# --------------------------

def load_json_safe(path: str, default):
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return default

def root_path_from_argv() -> str:
    """Return the project root regardless of where the editor is launched.
    - If an argument is provided, run detect_project_root from that path.
    - Otherwise, detect from the current working directory.
    """
    try:
        if len(sys.argv) > 1 and os.path.isdir(sys.argv[1]):
            return str(resolve_paths(sys.argv[1]).assets_dir)
        return str(resolve_paths(os.getcwd()).assets_dir)
    except Exception:
        return os.getcwd()

# --------------------------
# Supported schema
# --------------------------

TRIGGER_TYPES: Dict[str, Dict[str, Any]] = {
    # type: { label: str, fields: [(key, widget_kind, choices_key_or_None, tip)] }
    "player_action": {
        "label": "Player Action",
        "fields": [("action", "text", None, "Action name, e.g. unlock_locker")]
    },
    "talk_to": {
        "label": "Talk To",
        "fields": [("npc", "combo_or_text", "npcs", "NPC id or name")]
    },
    "item_acquired": {
        "label": "Item Acquired",
        "fields": [("item", "combo_or_text", "items", "Item name (case-insensitive match in engine)")]
    },
    "item_given": {
        "label": "Item Given To NPC",
        "fields": [("item", "combo_or_text", "items", "Item name"),
                   ("npc", "combo_or_text", "npcs", "NPC id or name")]
    },
    "enemy_defeated": {
        "label": "Enemy Defeated",
        "fields": [("enemy", "combo_or_text", "enemies", "Enemy id")]
    },
    "enter_room": {
        "label": "Enter Room",
        "fields": [("room", "combo_or_text", "rooms", "Room id")]
    },
    "complete_quest": {
        "label": "Quest Completed (as trigger)",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id")]
    },
    "complete_event": {
        "label": "Event Completed",
        "fields": [("event_id", "combo_or_text", "events", "ID of the event that just completed")]
    },
    # Extra trigger kinds present in events.json (fallback as text fields)
    "encounter_victory": {
        "label": "Encounter Victory",
        "fields": [("encounter_id", "text", None, "Encounter id")]
    },
    "quest_stage_complete": {
        "label": "Quest Stage Complete",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id"),
                   ("stage_id", "text", None, "Stage id")]
    },
    "npc_interaction": {
        "label": "NPC Interaction",
        "fields": [("npc_id", "combo_or_text", "npcs", "NPC id"),
                   ("interaction_type", "text", None, "interaction type")]
    },
}

# Actions supported by event_manager.py and your events.json today
ACTION_DEFS: Dict[str, Dict[str, Any]] = {
    "toggle_room_state": {
        "label": "Toggle Room State",
        "fields": [("room_id", "combo_or_text", "rooms", "Room id"),
                   ("state_key", "text", None, "Attribute on the room to toggle (bool)")]
    },
    "spawn_item": {
        "label": "Spawn Item In Room",
        "fields": [("room_id", "combo_or_text", "rooms", "Room id"),
                   ("item", "combo_or_text", "items", "Item name")]
    },
    "give_item_to_player": {
        "label": "Give Item To Player",
        "fields": [("item", "combo_or_text", "items", "Item name")]
    },
    "start_quest": {
        "label": "Start Quest",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id")]
    },
    "complete_quest": {
        "label": "Complete Quest",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id")]
    },
    "advance_quest_stage": {
        "label": "Advance Quest Stage",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id")]
    },
    "update_npc_dialogue": {
        "label": "Update NPC Dialogue",
        "fields": [("npc", "combo_or_text", "npcs", "NPC id or name"),
                   ("dialogue", "text", None, "Dialogue id or inline text")]
    },
    "start_battle": {
        "label": "Start Battle",
        "fields": [("enemy_ids", "multi_combo_text", "enemies", "One or more enemy ids (comma-separated)")]
    },
    "play_cinematic": {
        "label": "Play Cinematic",
        "fields": [("scene_id", "combo_or_text", "cinematics", "Cinematic scene id")]
    },
    "set_room_state": {
        "label": "Set Room State",
        "fields": [("room_id", "combo_or_text", "rooms", "Room id"),
                   ("state_key", "text", None, "Room state key (e.g., dark)"),
                   ("value", "text", None, "true/false or on/off")]
    },
    "set_global_dark": {
        "label": "Set Global Dark (Fade)",
        "fields": [("alpha", "text", None, "Target darkness (0=clear, 1=black)"),
                   ("duration", "text", None, "Fade duration in seconds (0=instant)"),
                   ("force", "checkbox", None, "Force black screen (overrides room lighting)")]
    },
    # Conditionals (nesting)
    "if_quest_active": {
        "label": "IF Quest Active",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id")],
        "is_conditional": True
    },
    "if_quest_status_is": {
        "label": "IF Quest Status Is",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id"),
                   ("status", "text", None, "Single status or comma-separated list")],
        "is_conditional": True
    },
    "begin_node": {
        "label": "Begin Node (Spawn Player)",
        "fields": [("room_id", "combo_or_text", "rooms", "The room ID to spawn the player in")]
    },
    "rebuild_ui": {
        "label": "Rebuild UI Layout",
        "fields": []
    },
    "wait_for_draw": {
        "label": "Wait For Next Frame",
        "fields": []
    },
}

# --------------------------
# Small UI primitives
# --------------------------

class LabeledTip(QLabel):
    def __init__(self, text: str, tip: str = ""):
        super().__init__(text)
        if tip:
            self.setToolTip(tip)

def combo_or_text(value_list: List[str]) -> QComboBox:
    cb = QComboBox()
    cb.setEditable(True)
    cb.addItems(sorted(set([v for v in value_list if isinstance(v, str)])))
    return cb

# --------------------------
# Dialogs for Trigger/Action
# --------------------------

class TriggerForm(QWidget):
    def __init__(self, resources: Dict[str, List[str]], trigger: Optional[Dict[str, Any]] = None):
        super().__init__()
        self.resources = resources
        self.type_cb = QComboBox()
        for key, meta in TRIGGER_TYPES.items():
            self.type_cb.addItem(f"{meta['label']} ({key})", key)

        self.form_layout = QFormLayout()
        self.field_widgets: Dict[str, Any] = {}
        # Allow dynamic, ad-hoc types for unknown triggers
        self._extra_type_fields: Dict[str, List] = {}

        lay = QVBoxLayout(self)
        lay.addWidget(LabeledTip("Trigger Type:"))
        lay.addWidget(self.type_cb)
        lay.addLayout(self.form_layout)

        self.type_cb.currentIndexChanged.connect(self._rebuild_fields)

        if trigger and isinstance(trigger, dict) and "type" in trigger:
            key = trigger["type"]
            # If this trigger type is unknown, add it as a custom entry and
            # infer its fields from the preset keys.
            items = [i for i in range(self.type_cb.count()) if self.type_cb.itemData(i) == key]
            if not items:
                # Build simple text fields for any present keys (except 'type')
                fields = []
                for k in trigger.keys():
                    if k == "type":
                        continue
                    fields.append((k, "text", None, ""))
                self._extra_type_fields[key] = fields
                self.type_cb.addItem(f"Custom ({key})", key)
            # Select the matching type
            idx = next((i for i in range(self.type_cb.count()) if self.type_cb.itemData(i) == key), 0)
            self.type_cb.setCurrentIndex(idx)
            self._rebuild_fields(preset=trigger)
        else:
            self._rebuild_fields()

    def _rebuild_fields(self, *_args, preset: Optional[Dict[str, Any]] = None):
        # clear
        while self.form_layout.count():
            item = self.form_layout.takeAt(0)
            w = item.widget()
            if w:
                w.deleteLater()
        self.field_widgets.clear()

        tkey = self.type_cb.currentData()
        meta = TRIGGER_TYPES.get(tkey, {})
        fields = list(meta.get("fields", []))
        # If this is a custom/unknown type, pull inferred fields
        if not fields and tkey in self._extra_type_fields:
            fields = self._extra_type_fields[tkey]
        for key, kind, rsrc_key, tip in fields:
            if kind == "text":
                w = QLineEdit()
                if preset: w.setText(str(preset.get(key, "")))
            elif kind == "combo_or_text":
                options = self.resources.get(rsrc_key, [])
                w = combo_or_text(options)
                if preset and key in preset:
                    w.setEditText(str(preset[key]))
            else:
                w = QLineEdit()
            self.field_widgets[key] = w
            self.form_layout.addRow(LabeledTip(f"{key}:", tip), w)

    def value(self) -> Dict[str, Any]:
        tkey = self.type_cb.currentData()
        out = {"type": tkey}
        meta = TRIGGER_TYPES.get(tkey, {})
        for key, kind, _rsrc_key, _tip in meta.get("fields", []):
            w = self.field_widgets[key]
            out[key] = w.currentText() if isinstance(w, QComboBox) else w.text()
            if isinstance(out[key], str):
                out[key] = out[key].strip()
        return out


class ActionDialog(QDialog):
    def __init__(self, resources: Dict[str, List[str]], existing: Optional[Dict[str, Any]] = None, parent=None):
        super().__init__(parent)
        self.setWindowTitle("Action")
        self.resources = resources

        self.type_cb = QComboBox()
        for key, meta in ACTION_DEFS.items():
            self.type_cb.addItem(f"{meta['label']} ({key})", key)

        lay = QVBoxLayout(self)
        lay.addWidget(LabeledTip("Action Type:"))
        lay.addWidget(self.type_cb)

        self.form_layout = QFormLayout()
        self.field_widgets: Dict[str, Any] = {}
        lay.addLayout(self.form_layout)

        self.buttons = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        self.buttons.accepted.connect(self.accept)
        self.buttons.rejected.connect(self.reject)
        lay.addWidget(self.buttons)

        self.type_cb.currentIndexChanged.connect(self._rebuild_fields)
        # For unknown types, we store ad-hoc field lists here
        self._extra_type_fields: Dict[str, List] = {}

        if existing and "type" in existing:
            key = existing["type"]
            items = [i for i in range(self.type_cb.count()) if self.type_cb.itemData(i) == key]
            if not items:
                # Infer fields from existing keys (excluding 'type' and 'do')
                fields = []
                for k in existing.keys():
                    if k in ("type", "do"):
                        continue
                    fields.append((k, "text", None, ""))
                self._extra_type_fields[key] = fields
                self.type_cb.addItem(f"Custom ({key})", key)
            idx = next((i for i in range(self.type_cb.count()) if self.type_cb.itemData(i) == key), 0)
            self.type_cb.setCurrentIndex(idx)
            self._rebuild_fields(preset=existing)
        else:
            self._rebuild_fields()

    def _rebuild_fields(self, *_args, preset: Optional[Dict[str, Any]] = None):
        # clear
        while self.form_layout.count():
            item = self.form_layout.takeAt(0)
            w = item.widget()
            if w:
                w.deleteLater()
        self.field_widgets.clear()

        akey = self.type_cb.currentData()
        meta = ACTION_DEFS.get(akey, {})
        fields = list(meta.get("fields", []))
        if not fields and akey in self._extra_type_fields:
            fields = self._extra_type_fields[akey]
        for key, kind, rsrc_key, tip in fields:
            if kind == "text":
                w = QLineEdit()
                if preset: w.setText(str(preset.get(key, "")))
            elif kind == "combo_or_text":
                options = self.resources.get(rsrc_key, [])
                w = combo_or_text(options)
                if preset and key in preset:
                    w.setEditText(str(preset[key]))
            elif kind == "multi_combo_text":
                options = self.resources.get(rsrc_key, [])
                w = QLineEdit()
                if options:
                    # Provide hint in tooltip
                    w.setPlaceholderText("Comma-separated. Known: " + ", ".join(options[:10]) + ("..." if len(options) > 10 else ""))
                if preset:
                    val = preset.get(key, [])
                    if isinstance(val, list):
                        w.setText(", ".join(val))
                    else:
                        w.setText(str(val))
            else:
                w = QLineEdit()
            if kind == "checkbox":
                w = QCheckBox()
                if preset: w.setChecked(bool(preset.get(key, False)))
            self.field_widgets[key] = w
            self.form_layout.addRow(LabeledTip(f"{key}:", tip), w)

    def value(self) -> Dict[str, Any]:
        akey = self.type_cb.currentData()
        out = {"type": akey}
        meta = ACTION_DEFS.get(akey, {})
        for key, kind, _rsrc_key, _tip in meta.get("fields", []):
            w = self.field_widgets[key]
            if isinstance(w, QComboBox):
                val = w.currentText()
            elif isinstance(w, QCheckBox):
                val = w.isChecked()
            else:
                val = w.text()
            val = val.strip() if isinstance(val, str) else val
            if kind == "multi_combo_text":
                # split on commas
                if isinstance(val, str):
                    parts = [p.strip() for p in val.split(",") if p.strip()]
                    out[key] = parts
                else:
                    out[key] = val
            else:
                out[key] = val
        return out

# --------------------------
# Main Editor Widget
# --------------------------

class EventEditor(QWidget):
    def __init__(self, root_dir: str):
        super().__init__()
        paths = resolve_paths(root_dir)
        self.project_root = paths.project_root
        self.root = str(paths.assets_dir)
        self.setWindowTitle("Starborn Event Editor")
        self.resize(1200, 760)

        # Resources for combos
        self.resources: Dict[str, List[str]] = {
            "rooms": [], "items": [], "enemies": [], "quests": [], "npcs": [], "cinematics": []
        }
        self._load_resources()

        # Data
        self.events: Dict[str, Dict[str, Any]] = {}
        self.current_id: Optional[str] = None
        self._load_events()
        # Make event IDs available for the trigger dropdown
        self.resources["events"] = sorted(self.events.keys())

        self._build_ui()
        self._refresh_list()

    # ---------- IO ----------
    def _load_resources(self):
        # Rooms: accept list[dict] or dict[id->room]
        rooms = load_json_safe(os.path.join(self.root, "rooms.json"), [])
        room_ids: List[str] = []
        if isinstance(rooms, dict):
            room_ids = list(rooms.keys())
        elif isinstance(rooms, list):
            for rd in rooms:
                rid = rd.get("id") if isinstance(rd, dict) else None
                if rid:
                    room_ids.append(rid)
        self.resources["rooms"] = room_ids

        items = load_json_safe(os.path.join(self.root, "items.json"), [])
        if isinstance(items, list):
            names = []
            for it in items:
                name = it.get("name") or it.get("id")
                if name: names.append(name)
            self.resources["items"] = names

        enemies = load_json_safe(os.path.join(self.root, "enemies.json"), [])
        enemy_ids: List[str] = []
        if isinstance(enemies, dict):
            enemy_ids = list(enemies.keys())
        elif isinstance(enemies, list):
            for ed in enemies:
                eid = ed.get("id") if isinstance(ed, dict) else None
                if eid:
                    enemy_ids.append(eid)
        self.resources["enemies"] = enemy_ids

        quests = load_json_safe(os.path.join(self.root, "quests.json"), [])
        if isinstance(quests, list):
            self.resources["quests"] = [q.get("id","") for q in quests if q.get("id")]

        cinematics = load_json_safe(os.path.join(self.root, "cinematics.json"), {})
        if isinstance(cinematics, dict):
            self.resources["cinematics"] = list(cinematics.keys())

        # NPC list is not centralized yet; try to infer from rooms.json where possible
        # fall back to free text
        npcs = set()
        if isinstance(rooms, dict):
            for rid, r in rooms.items():
                for k in ("npcs", "NPCs", "characters"):
                    if isinstance(r.get(k), list):
                        for n in r[k]:
                            if isinstance(n, str):
                                npcs.add(n)
        self.resources["npcs"] = sorted(npcs)

    def _load_events(self):
        path = os.path.join(self.root, "events.json")
        try:
            with open(path, "r", encoding="utf-8") as f:
                raw = json.load(f)
            self.events = {e["id"]: e for e in raw}
        except Exception as e:
            QMessageBox.critical(self, "Load Error", f"Failed to load events.json:\n{e}")
            self.events = {}

    def _save_events(self):
        path = os.path.join(self.root, "events.json")
        try:
            with open(path, "w", encoding="utf-8") as f:
                out = list(self.events.values())
                json.dump(out, f, indent=4)
            QMessageBox.information(self, "Saved", "events.json saved successfully.")
        except Exception as e:
            QMessageBox.critical(self, "Save Error", f"Failed to save events.json:\n{e}")

    # ---------- UI ----------
    def _build_ui(self):
        root = QHBoxLayout(self)

        # Left list
        left = QVBoxLayout()
        self.search = QLineEdit(); self.search.setPlaceholderText("Search events (id/desc)")
        self.search.textChanged.connect(self._refresh_list)
        left.addWidget(self.search)

        self.event_list = QListWidget()
        self.event_list.itemClicked.connect(self._on_select)
        left.addWidget(self.event_list, 1)

        row = QHBoxLayout()
        add_btn = QPushButton("Add"); add_btn.clicked.connect(self._on_add)
        dup_btn = QPushButton("Duplicate"); dup_btn.clicked.connect(self._on_duplicate)
        del_btn = QPushButton("Remove"); del_btn.clicked.connect(self._on_remove)
        row.addWidget(add_btn); row.addWidget(dup_btn); row.addWidget(del_btn)
        left.addLayout(row)

        root.addLayout(left, 1)

        # Right tabs
        self.tabs = QTabWidget()

        # Basics
        basic = QWidget(); basic_l = QFormLayout(basic)
        self.id_edit = QLineEdit(); self.id_edit.setPlaceholderText("unique_event_id")
        self.desc_edit = QLineEdit()
        self.repeat_chk = QCheckBox("Repeatable")
        self.xp_spin = QSpinBox(); self.xp_spin.setRange(0, 999999)
        self.notes_edit = QTextEdit()
        basic_l.addRow("ID:", self.id_edit)
        basic_l.addRow("Description:", self.desc_edit)
        basic_l.addRow("Repeatable:", self.repeat_chk)
        basic_l.addRow("XP Reward:", self.xp_spin)
        basic_l.addRow("Notes:", self.notes_edit)
        self.tabs.addTab(basic, "Basics")

        # Trigger
        trig = QWidget(); trig_l = QVBoxLayout(trig)
        self.trigger_form = TriggerForm(self.resources)
        trig_l.addWidget(self.trigger_form)
        self.tabs.addTab(trig, "Trigger")

        # Actions tab
        act = QWidget(); act_l = QVBoxLayout(act)
        self.actions_tree = QTreeWidget()
        self.actions_tree.setColumnCount(2)
        self.actions_tree.setHeaderLabels(["Action", "Summary"])
        act_l.addWidget(self.actions_tree, 1)

        btns = QHBoxLayout()
        add_action = QPushButton("Add Action"); add_action.clicked.connect(self._add_action)
        add_child = QPushButton("Add Child to Conditional"); add_child.clicked.connect(self._add_child_action)
        edit_action = QPushButton("Edit"); edit_action.clicked.connect(self._edit_action)
        remove_action = QPushButton("Remove"); remove_action.clicked.connect(self._remove_action)
        up_btn = QPushButton("▲"); up_btn.clicked.connect(lambda: self._move_action(-1))
        down_btn = QPushButton("▼"); down_btn.clicked.connect(lambda: self._move_action(1))
        btns.addWidget(add_action); btns.addWidget(add_child); btns.addWidget(edit_action)
        btns.addWidget(remove_action); btns.addWidget(up_btn); btns.addWidget(down_btn)
        act_l.addLayout(btns)
        self.tabs.addTab(act, "Actions")

        # Messages
        msg = QWidget(); msg_l = QFormLayout(msg)
        self.on_msg = QLineEdit(); self.off_msg = QLineEdit()
        msg_l.addRow("on_message:", self.on_msg)
        msg_l.addRow("off_message:", self.off_msg)
        self.tabs.addTab(msg, "Messages")

        # Footer row save/export
        right = QVBoxLayout()
        right.addWidget(self.tabs, 1)

        save_row = QHBoxLayout()
        save_btn = QPushButton("Save File"); save_btn.clicked.connect(self._save_events)
        apply_btn = QPushButton("Save Current Event"); apply_btn.clicked.connect(self._apply_current)
        save_row.addWidget(save_btn); save_row.addWidget(apply_btn)
        right.addLayout(save_row)

        root.addLayout(right, 3)

    # ---------- List ops ----------
    def _refresh_list(self):
        self.event_list.clear()
        ft = (self.search.text() or "").lower().strip()
        for eid in sorted(self.events.keys()):
            e = self.events[eid]
            line = eid
            if e.get("description"):
                line += f" — {e['description']}"
            if not ft or ft in line.lower():
                self.event_list.addItem(QListWidgetItem(line))

    def _on_select(self, item: QListWidgetItem):
        # extract id (before " — ")
        text = item.text()
        eid = text.split(" — ")[0]
        self._load_into_form(eid)

    def _on_add(self):
        base = "new_event"
        n = 1
        eid = base
        while eid in self.events:
            n += 1
            eid = f"{base}_{n}"
        self.events[eid] = {
            "id": eid,
            "description": "",
            "trigger": {"type": "player_action", "action": ""},
            "actions": [],
        }
        self._refresh_list()
        # auto-select
        matches = self.event_list.findItems(eid, Qt.MatchStartsWith)
        if matches:
            self.event_list.setCurrentItem(matches[0])
            self._load_into_form(eid)

    def _on_duplicate(self):
        if not self.current_id: return
        src = self.events[self.current_id]
        base = f"{self.current_id}_copy"
        n, eid = 1, f"{base}"
        while eid in self.events:
            n += 1
            eid = f"{base}_{n}"
        dup = json.loads(json.dumps(src))
        dup["id"] = eid
        self.events[eid] = dup
        self._refresh_list()

    def _on_remove(self):
        if not self.current_id: return
        if QMessageBox.question(self, "Delete", f"Delete event '{self.current_id}'?",
                                QMessageBox.Yes | QMessageBox.No) == QMessageBox.Yes:
            del self.events[self.current_id]
            self.current_id = None
            self._refresh_list()
            # clear form
            self._load_into_form(None)

    # ---------- Form load/save ----------
    def _load_into_form(self, eid: Optional[str]):
        self.current_id = eid
        # clear UI to defaults
        self.id_edit.setText(eid or "")
        self.desc_edit.setText("")
        self.repeat_chk.setChecked(False)
        self.xp_spin.setValue(0)
        self.notes_edit.setPlainText("")
        self.on_msg.setText("")
        self.off_msg.setText("")
        # trigger
        self.trigger_form = TriggerForm(self.resources)
        self.tabs.widget(1).layout().replaceWidget(self.tabs.widget(1).layout().itemAt(0).widget(), self.trigger_form)

        # actions tree
        self.actions_tree.clear()

        if not eid: return
        e = self.events[eid]
        self.id_edit.setText(e.get("id",""))
        self.desc_edit.setText(e.get("description",""))
        self.repeat_chk.setChecked(bool(e.get("repeatable", False)))
        self.xp_spin.setValue(int(e.get("xp_reward", 0) or 0))
        self.notes_edit.setPlainText(e.get("notes",""))
        self.on_msg.setText(e.get("on_message","") or "")
        self.off_msg.setText(e.get("off_message","") or "")

        trig = e.get("trigger", {"type":"player_action","action":""})
        self.trigger_form = TriggerForm(self.resources, trig)
        self.tabs.widget(1).layout().replaceWidget(self.tabs.widget(1).layout().itemAt(0).widget(), self.trigger_form)

        # actions
        for act in e.get("actions", []):
            self._add_action_item(None, act)

    def _apply_current(self):
        if not self.id_edit.text().strip():
            QMessageBox.warning(self, "Validation", "Event ID cannot be empty.")
            return

        eid_new = self.id_edit.text().strip()
        if self.current_id and eid_new != self.current_id and eid_new in self.events:
            QMessageBox.warning(self, "Validation", f"Event id '{eid_new}' already exists.")
            return

        out = {
            "id": eid_new,
            "description": self.desc_edit.text().strip(),
            "trigger": self.trigger_form.value(),
            "actions": self._serialize_actions_tree(),
        }
        if self.repeat_chk.isChecked(): out["repeatable"] = True
        xp = int(self.xp_spin.value())
        if xp > 0: out["xp_reward"] = xp
        if self.on_msg.text().strip(): out["on_message"] = self.on_msg.text().strip()
        if self.off_msg.text().strip(): out["off_message"] = self.off_msg.text().strip()
        if self.notes_edit.toPlainText().strip(): out["notes"] = self.notes_edit.toPlainText().strip()

        # Update dict key on rename
        if self.current_id and eid_new != self.current_id:
            del self.events[self.current_id]
        self.events[eid_new] = out
        self.current_id = eid_new
        self._refresh_list()
        QMessageBox.information(self, "Saved", f"Event '{eid_new}' updated in memory.\nClick 'Save File' to write events.json.")

    # ---------- Actions tree helpers ----------
    def _summarize_action(self, act: Dict[str, Any]) -> str:
        t = act.get("type","")
        if t in ("if_quest_active","if_quest_status_is"):
            return ", ".join([f"{k}={v}" for k,v in act.items() if k not in ("type","do")])
        else:
            keys = [k for k in act.keys() if k != "type"]
            parts = []
            for k in keys:
                v = act[k]
                if isinstance(v, list):
                    parts.append(f"{k}=[{', '.join(map(str,v))}]")
                else:
                    parts.append(f"{k}={v}")
            return ", ".join(parts)

    def _add_action_item(self, parent_item: Optional[QTreeWidgetItem], act: Dict[str, Any]):
        label = act.get("type","")
        item = QTreeWidgetItem([label, self._summarize_action(act)])
        item.setData(0, Qt.UserRole, act)
        if parent_item:
            parent_item.addChild(item)
        else:
            self.actions_tree.addTopLevelItem(item)

        # If conditional, add children from "do"
        if act.get("type") in ("if_quest_active","if_quest_status_is") and isinstance(act.get("do"), list):
            for child in act["do"]:
                self._add_action_item(item, child)
            item.setExpanded(True)

    def _collect_action_from_item(self, item: QTreeWidgetItem) -> Dict[str, Any]:
        act = item.data(0, Qt.UserRole)
        t = act.get("type")
        if t in ("if_quest_active","if_quest_status_is"):
            do_list = []
            for i in range(item.childCount()):
                do_list.append(self._collect_action_from_item(item.child(i)))
            out = dict(act)
            out["do"] = do_list
            return out
        else:
            return dict(act)

    def _serialize_actions_tree(self) -> List[Dict[str, Any]]:
        out = []
        for i in range(self.actions_tree.topLevelItemCount()):
            out.append(self._collect_action_from_item(self.actions_tree.topLevelItem(i)))
        return out

    # ---------- Actions toolbar slots ----------
    def _add_action(self):
        dlg = ActionDialog(self.resources, parent=self)
        if dlg.exec_() == QDialog.Accepted:
            act = dlg.value()
            # if conditional, ensure it has "do"
            if ACTION_DEFS.get(act["type"],{}).get("is_conditional"):
                act.setdefault("do", [])
            self._add_action_item(None, act)

    def _add_child_action(self):
        sel = self.actions_tree.currentItem()
        if not sel:
            QMessageBox.information(self, "Info", "Select a conditional action first.")
            return
        act = sel.data(0, Qt.UserRole) or {}
        if act.get("type") not in ("if_quest_active","if_quest_status_is"):
            QMessageBox.information(self, "Info", "Selected action is not conditional.")
            return
        dlg = ActionDialog(self.resources, parent=self)
        if dlg.exec_() == QDialog.Accepted:
            child = dlg.value()
            self._add_action_item(sel, child)
            sel.setExpanded(True)

    def _edit_action(self):
        sel = self.actions_tree.currentItem()
        if not sel:
            return
        act = sel.data(0, Qt.UserRole) or {}
        dlg = ActionDialog(self.resources, existing=act, parent=self)
        if dlg.exec_() == QDialog.Accepted:
            new_act = dlg.value()
            if ACTION_DEFS.get(new_act["type"],{}).get("is_conditional"):
                new_act.setdefault("do", [])
            sel.setData(0, Qt.UserRole, new_act)
            sel.setText(0, new_act.get("type",""))
            sel.setText(1, self._summarize_action(new_act))

    def _remove_action(self):
        sel = self.actions_tree.currentItem()
        if not sel:
            return
        parent = sel.parent()
        if parent:
            parent.removeChild(sel)
        else:
            idx = self.actions_tree.indexOfTopLevelItem(sel)
            self.actions_tree.takeTopLevelItem(idx)

    def _move_action(self, delta: int):
        sel = self.actions_tree.currentItem()
        if not sel:
            return
        parent = sel.parent()
        if parent:
            idx = parent.indexOfChild(sel)
            new_idx = idx + delta
            if 0 <= new_idx < parent.childCount():
                parent.takeChild(idx)
                parent.insertChild(new_idx, sel)
        else:
            idx = self.actions_tree.indexOfTopLevelItem(sel)
            new_idx = idx + delta
            if 0 <= new_idx < self.actions_tree.topLevelItemCount():
                self.actions_tree.takeTopLevelItem(idx)
                self.actions_tree.insertTopLevelItem(new_idx, sel)

# -------------- launchers --------------

def launch_from_studio(root_dir: str):
    app = QApplication.instance() or QApplication(sys.argv)
    w = EventEditor(root_dir)
    w.show()
    app.exec_()

if __name__ == "__main__":
    root = root_path_from_argv()
    app = QApplication(sys.argv)
    w = EventEditor(root)
    w.show()
    sys.exit(app.exec_())
