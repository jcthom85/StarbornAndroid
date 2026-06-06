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
from typing import Any, Dict, List, Optional, Set, Tuple

from devkit_paths import resolve_paths
from scope_utils import ScopeIndex, scope_prefix, scoped_id

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
        "fields": [
            ("action", "text", None, "Action name, e.g. unlock_locker"),
            ("item_id", "combo_or_text", "items", "Optional item id")
        ]
    },
    "talk_to": {
        "label": "Talk To",
        "fields": [("npc", "combo_or_text", "npcs", "NPC id or name")]
    },
    "item_acquired": {
        "label": "Item Acquired",
        "fields": [("item_id", "combo_or_text", "items", "Item id")]
    },
    "item_given": {
        "label": "Item Given To NPC",
        "fields": [("item_id", "combo_or_text", "items", "Item id"),
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
    "dialogue_closed": {
        "label": "Dialogue Closed",
        "fields": [("npc", "combo_or_text", "npcs", "NPC id or name")]
    },
    # Extra trigger kinds present in events.json (fallback as text fields)
    "encounter_victory": {
        "label": "Encounter Victory",
        "fields": [("encounter_id", "combo_or_text", "encounters", "Encounter id")]
    },
    "encounter_defeat": {
        "label": "Encounter Defeat",
        "fields": [("encounter_id", "combo_or_text", "encounters", "Encounter id")]
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
    "advance_quest": {
        "label": "Advance Quest",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id"),
                   ("to_stage_id", "text", None, "Stage id")]
    },
    "advance_quest_if_active": {
        "label": "Advance Quest If Active",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id"),
                   ("to_stage_id", "text", None, "Stage id"),
                   ("condition", "text", None, "Optional condition expression")]
    },
    "advance_quest_stage": {
        "label": "Advance Quest Stage",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id"),
                   ("to_stage_id", "text", None, "Stage id")]
    },
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
        "fields": [("item_id", "combo_or_text", "items", "Item id"),
                   ("quantity", "text", None, "Quantity")]
    },
    "give_item": {
        "label": "Give Item",
        "fields": [("item_id", "combo_or_text", "items", "Item id"),
                   ("quantity", "text", None, "Quantity"),
                   ("to_player", "checkbox", None, "Give directly to player"),
                   ("note", "text", None, "Optional note")]
    },
    "give_xp": {
        "label": "Give XP",
        "fields": [("xp", "text", None, "XP amount")]
    },
    "grant_reward": {
        "label": "Grant Reward Bundle",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id (optional)"),
                   ("xp", "text", None, "XP amount"),
                   ("credits", "text", None, "Credits amount"),
                   ("ap", "text", None, "AP amount"),
                   ("items", "text", None, "Items JSON array")]
    },
    "start_quest": {
        "label": "Start Quest",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id"),
                   ("start_quest", "combo_or_text", "quests", "Legacy start_quest id")]
    },
    "complete_quest": {
        "label": "Complete Quest",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id")]
    },
    "track_quest": {
        "label": "Track Quest",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id")]
    },
    "set_quest_task_done": {
        "label": "Set Quest Task Done",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id"),
                   ("task_id", "text", None, "Task id")]
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
    "trigger_cutscene": {
        "label": "Trigger Cutscene",
        "fields": [("cutscene_id", "combo_or_text", "cinematics", "Cutscene id")]
    },
    "set_room_state": {
        "label": "Set Room State",
        "fields": [("room_id", "combo_or_text", "rooms", "Room id"),
                   ("state_key", "text", None, "Room state key (e.g., dark)"),
                   ("value", "text", None, "true/false or on/off")]
    },
    "set_milestone": {
        "label": "Set Milestone",
        "fields": [("milestone", "combo_or_text", "milestones", "Milestone id")]
    },
    "show_message": {
        "label": "Show Message",
        "fields": [("message", "text", None, "Message")]
    },
    "narrate": {
        "label": "Narrate (Overlay Text)",
        "fields": [("text", "text", None, "Narration text"),
                   ("tap_to_dismiss", "checkbox", None, "Tap to dismiss")]
    },
    "system_tutorial": {
        "label": "System Tutorial",
        "fields": [("scene_id", "combo_or_text", "cinematics", "Tutorial scene id"),
                   ("context", "text", None, "Context / label")]
    },
    "player_action": {
        "label": "Player Action",
        "fields": [("action", "text", None, "Action id")]
    },
    "spawn_encounter": {
        "label": "Spawn Encounter",
        "fields": [("encounter_id", "combo_or_text", "encounters", "Encounter id"),
                   ("room_id", "combo_or_text", "rooms", "Room id"),
                   ("condition", "text", None, "Optional condition expression")]
    },
    "unlock_room_search": {
        "label": "Unlock Room Search",
        "fields": [("room_id", "combo_or_text", "rooms", "Room id"),
                   ("note", "text", None, "Optional note")]
    },
    "reveal_hidden_item": {
        "label": "Reveal Hidden Item",
        "fields": [("room_id", "combo_or_text", "rooms", "Room id"),
                   ("item_id", "combo_or_text", "items", "Item id"),
                   ("quantity", "text", None, "Quantity"),
                   ("note", "text", None, "Optional note")]
    },
    "take_item": {
        "label": "Take Item",
        "fields": [("item_id", "combo_or_text", "items", "Item id"),
                   ("quantity", "text", None, "Quantity")]
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
    "if_quest_not_started": {
        "label": "IF Quest Not Started",
        "fields": [("quest_id", "combo_or_text", "quests", "Quest id")],
        "is_conditional": True
    },
    "if_milestone_set": {
        "label": "IF Milestone Set",
        "fields": [("milestone", "combo_or_text", "milestones", "Milestone id")],
        "is_conditional": True
    },
    "if_milestone_not_set": {
        "label": "IF Milestone Not Set",
        "fields": [("milestone", "combo_or_text", "milestones", "Milestone id")],
        "is_conditional": True
    },
    "if_milestones_set": {
        "label": "IF Milestones Set",
        "fields": [("milestones", "multi_combo_text", "milestones", "Milestone ids (comma-separated)")],
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
                if preset:
                    val = preset.get(key, "")
                    if key == "items" and isinstance(val, list):
                        try:
                            w.setText(json.dumps(val, ensure_ascii=False))
                        except Exception:
                            w.setText(str(val))
                    else:
                        w.setText(str(val))
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
        fields = list(meta.get("fields", []))
        if not fields and tkey in self._extra_type_fields:
            fields = self._extra_type_fields[tkey]
        for key, kind, _rsrc_key, _tip in fields:
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
        fields = list(meta.get("fields", []))
        if not fields and akey in self._extra_type_fields:
            fields = self._extra_type_fields[akey]
        for key, kind, _rsrc_key, _tip in fields:
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
            elif key == "items" and isinstance(val, str):
                # allow JSON array for reward items
                if not val:
                    out[key] = []
                else:
                    try:
                        parsed = json.loads(val)
                        out[key] = parsed if isinstance(parsed, list) else val
                    except Exception:
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

        self.scope_index = ScopeIndex.from_assets(self.root)
        self.world_ids = self.scope_index.world_ids
        self.hubs_by_world = self.scope_index.hubs_by_world

        # Resources for combos
        self.resources: Dict[str, List[str]] = {
            "rooms": [], "items": [], "enemies": [], "quests": [], "npcs": [],
            "cinematics": [], "milestones": [], "encounters": []
        }
        self._load_resources()

        # Data
        self.events: Dict[str, Dict[str, Any]] = {}
        self.current_id: Optional[str] = None
        self._load_events()
        # Make event IDs available for the trigger dropdown
        self.resources["events"] = sorted(self.events.keys())

        self.undo_manager = UndoManager()
        self._build_ui()
        self._wire_undo()
        self._refresh_list()

    # ---------- Undo ----------
    def _wire_undo(self):
        um = self.undo_manager
        um.watch_line_edit(self.id_edit)
        um.watch_line_edit(self.desc_edit)
        um.watch_checkbox(self.repeat_chk)
        um.watch_line_edit(self.on_msg)
        um.watch_line_edit(self.off_msg)
        um.watch_plain_text(self.conditions_edit)

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
            ids = []
            for it in items:
                ident = it.get("id") or it.get("name")
                if ident:
                    ids.append(ident)
            self.resources["items"] = ids

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
        elif isinstance(cinematics, list):
            self.resources["cinematics"] = [c.get("id") for c in cinematics if isinstance(c, dict) and c.get("id")]

        milestones = load_json_safe(os.path.join(self.root, "milestones.json"), [])
        if isinstance(milestones, list):
            self.resources["milestones"] = [m.get("id") for m in milestones if isinstance(m, dict) and m.get("id")]

        encounters = load_json_safe(os.path.join(self.root, "encounters.json"), [])
        if isinstance(encounters, list):
            self.resources["encounters"] = [e.get("id") for e in encounters if isinstance(e, dict) and e.get("id")]
        elif isinstance(encounters, dict):
            self.resources["encounters"] = list(encounters.keys())

        # NPC list: prefer npcs.json, fall back to rooms.json inference
        npcs = set()
        npcs_data = load_json_safe(os.path.join(self.root, "npcs.json"), [])
        if isinstance(npcs_data, list):
            for n in npcs_data:
                if isinstance(n, dict):
                    ident = n.get("id") or n.get("name")
                    if ident:
                        npcs.add(ident)
        if not npcs and isinstance(rooms, dict):
            for _rid, r in rooms.items():
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
        self._refresh_list()

    def _collect_event_rooms(self, payload: Any) -> Set[str]:
        rooms: Set[str] = set()

        def _scan(obj: Any):
            if isinstance(obj, dict):
                for key, val in obj.items():
                    if key in ("room", "room_id", "target_room", "entry_room"):
                        if isinstance(val, str) and val:
                            rooms.add(val)
                    elif isinstance(val, (dict, list)):
                        _scan(val)
            elif isinstance(obj, list):
                for entry in obj:
                    _scan(entry)

        _scan(payload)
        return rooms

    def _event_hubs(self, event: Dict[str, Any]) -> Set[str]:
        rooms = set()
        rooms.update(self._collect_event_rooms(event.get("trigger", {})))
        rooms.update(self._collect_event_rooms(event.get("actions", [])))
        hubs = {self.scope_index.room_to_hub.get(r) for r in rooms}
        return {h for h in hubs if h}

    def _event_matches_scope(self, event_id: str, event: Dict[str, Any]) -> bool:
        world_id, hub_id = self._current_scope_filter()
        if not world_id and not hub_id:
            return True
        hubs = self._event_hubs(event)
        if hub_id:
            if hub_id in hubs:
                return True
            prefix = self._scope_prefix()
            return bool(prefix and event_id.startswith(prefix))
        if world_id:
            for hid in hubs:
                if self.scope_index.hub_to_world.get(hid) == world_id:
                    return True
            prefix = self._scope_prefix()
            return bool(prefix and event_id.startswith(prefix))
        return True

    # ---------- UI ----------
    def _build_ui(self):
        root = QHBoxLayout(self)

        # Left list
        left = QVBoxLayout()
        filter_row = QHBoxLayout()
        self.scope_world = QComboBox()
        self.scope_world.addItems(["All"] + self.world_ids)
        self.scope_hub = QComboBox()
        self.scope_world.currentTextChanged.connect(self._on_scope_filter_changed)
        self.scope_hub.currentTextChanged.connect(self._refresh_list)
        filter_row.addWidget(QLabel("World"))
        filter_row.addWidget(self.scope_world, 1)
        filter_row.addWidget(QLabel("Hub"))
        filter_row.addWidget(self.scope_hub, 1)
        left.addLayout(filter_row)
        self._refresh_hub_filter()

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
        basic_l.addRow("ID:", self.id_edit)
        basic_l.addRow("Description:", self.desc_edit)
        basic_l.addRow("Repeatable:", self.repeat_chk)
        self.tabs.addTab(basic, "Basics")

        # Trigger
        trig = QWidget(); trig_l = QVBoxLayout(trig)
        self.trigger_form = TriggerForm(self.resources)
        trig_l.addWidget(self.trigger_form)
        self.tabs.addTab(trig, "Trigger")

        # Conditions
        cond = QWidget(); cond_l = QVBoxLayout(cond)
        self.conditions_edit = QTextEdit()
        self.conditions_edit.setPlaceholderText("JSON array of condition objects…")
        cond_l.addWidget(self.conditions_edit, 1)
        self.tabs.addTab(cond, "Conditions")

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
            if not self._event_matches_scope(eid, e):
                continue
            line = eid
            if e.get("description"):
                line += f" — {e['description']}"
            if not ft or ft in line.lower():
                self.event_list.addItem(QListWidgetItem(line))

    def select_id(self, ident: str):
        """Select an event by ID. Called by Studio Pro goto."""
        ident = (ident or "").strip()
        if not ident or ident not in self.events:
            return
        for i in range(self.event_list.count()):
            item = self.event_list.item(i)
            if item and item.text().split(" — ")[0] == ident:
                self.event_list.setCurrentItem(item)
                break

    def _on_select(self, item: QListWidgetItem):
        # extract id (before " — ")
        self.undo_manager.stack.clear()
        text = item.text()
        eid = text.split(" — ")[0]
        self._load_into_form(eid)

    def _on_add(self):
        prefix = self._scope_prefix()
        eid = scoped_id(prefix, "evt", "new", self.events.keys())
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
        self.conditions_edit.setPlainText("[]")
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
        try:
            self.conditions_edit.setPlainText(json.dumps(e.get("conditions", []) or [], ensure_ascii=False, indent=2))
        except Exception:
            self.conditions_edit.setPlainText("[]")
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
        prefix = self._scope_prefix()
        if prefix and not eid_new.startswith(prefix):
            QMessageBox.warning(
                self,
                "Validation",
                f"Event ID must start with '{prefix}' for the current scope.",
            )
            return
        if self.current_id and eid_new != self.current_id and eid_new in self.events:
            QMessageBox.warning(self, "Validation", f"Event id '{eid_new}' already exists.")
            return

        # Parse conditions JSON
        try:
            cond_text = self.conditions_edit.toPlainText().strip()
            conditions = json.loads(cond_text) if cond_text else []
            if conditions is None:
                conditions = []
            if not isinstance(conditions, list):
                raise ValueError("Conditions must be a JSON array.")
        except Exception as exc:
            QMessageBox.warning(self, "Validation", f"Invalid conditions JSON:\n{exc}")
            return

        out = {
            "id": eid_new,
            "description": self.desc_edit.text().strip(),
            "trigger": self.trigger_form.value(),
            "conditions": conditions,
            "actions": self._serialize_actions_tree(),
        }
        if self.repeat_chk.isChecked(): out["repeatable"] = True
        if self.on_msg.text().strip(): out["on_message"] = self.on_msg.text().strip()
        if self.off_msg.text().strip(): out["off_message"] = self.off_msg.text().strip()

        # Update dict key on rename
        if self.current_id and eid_new != self.current_id:
            del self.events[self.current_id]
        self.events[eid_new] = out
        self.current_id = eid_new
        self._refresh_list()
        QMessageBox.information(self, "Saved", f"Event '{eid_new}' updated in memory.\nClick 'Save File' to write events.json.")

    # ---------- Actions tree helpers ----------
    def _summarize_action(self, act: Dict[str, Any]) -> str:
        keys = [k for k in act.keys() if k not in ("type", "do", "else", "on_complete")]
        parts = []
        for k in keys:
            v = act[k]
            if isinstance(v, list):
                parts.append(f"{k}=[{', '.join(map(str,v))}]")
            else:
                parts.append(f"{k}={v}")
        return ", ".join(parts)

    def _is_conditional_action(self, act: Dict[str, Any]) -> bool:
        t = act.get("type")
        if ACTION_DEFS.get(t, {}).get("is_conditional"):
            return True
        return isinstance(act.get("do"), list)

    def _add_action_item(self, parent_item: Optional[QTreeWidgetItem], act: Dict[str, Any]):
        label = act.get("type","")
        item = QTreeWidgetItem([label, self._summarize_action(act)])
        item.setData(0, Qt.UserRole, act)
        if parent_item:
            parent_item.addChild(item)
        else:
            self.actions_tree.addTopLevelItem(item)

        # If conditional, add children from "do"
        if self._is_conditional_action(act) and isinstance(act.get("do"), list):
            for child in act["do"]:
                self._add_action_item(item, child)
            item.setExpanded(True)

    def _collect_action_from_item(self, item: QTreeWidgetItem) -> Dict[str, Any]:
        act = item.data(0, Qt.UserRole)
        t = act.get("type")
        if self._is_conditional_action(act):
            do_list = []
            for i in range(item.childCount()):
                do_list.append(self._collect_action_from_item(item.child(i)))
            out = dict(act)
            out["do"] = do_list
            return out
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
        if not self._is_conditional_action(act):
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
            # Preserve nested branches and reward item arrays when not edited
            for k in ("do", "else", "on_complete", "items"):
                if k in act and k not in new_act:
                    new_act[k] = act[k]
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
