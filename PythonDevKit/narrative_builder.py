#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Starborn — Narrative Builder

Unified authoring surface for narrative content:
  • Browse quests, stages, and tasks in one place.
  • Compose stage “beats” that link dialogue, events, cutscenes, and tutorial tasks.
  • Create and edit dialogue/event/cutscene assets with focused popovers.
  • Keep an optional flow map (narrative_flows.json) so related assets stay grouped.

The builder keeps existing specialized editors intact; it focuses on rapid planning
and lightweight editing so you can stay in the creative loop.
"""
from __future__ import annotations

import json
import os
import random
import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

from PyQt5.QtCore import Qt, QSize
from PyQt5.QtGui import QIntValidator, QKeySequence
from PyQt5.QtWidgets import (
    QApplication,
    QCheckBox,
    QDialog,
    QDialogButtonBox,
    QFileDialog,
    QFormLayout,
    QScrollArea,
    QGridLayout,
    QGroupBox,
    QHBoxLayout,
    QInputDialog,
    QLabel,
    QLineEdit,
    QListWidget,
    QListWidgetItem,
    QMainWindow,
    QMessageBox,
    QPushButton,
    QPlainTextEdit,
    QSizePolicy,
    QSplitter,
    QTableWidget,
    QTableWidgetItem,
    QTabWidget,
    QTextEdit,
    QVBoxLayout,
    QWidget,
    QMenu,
    QComboBox,
    QStackedWidget,
    QShortcut
)
from jinja2 import Environment, FileSystemLoader

from data_core import json_load, json_save, unique_id
from devkit_paths import resolve_paths
from ai_narrative import NarrativeContext, NarrativeGenerationError, StageDraft, generate_stage
from editor_bus import goto as studio_goto

try:
    # Reuse schema metadata from existing event editor if available.
    from event_editor import TRIGGER_TYPES, ACTION_DEFS, ActionDialog as EventActionDialog
except Exception:  # pragma: no cover - allow running without importing heavy editor
    TRIGGER_TYPES = {}
    ACTION_DEFS = {}

narrative_flow_filename = "narrative_flows.json"


def _read_json_list(path: Path) -> List[dict]:
    if not path.exists():
        return []
    with path.open("r", encoding="utf-8") as f:
        data = json.load(f)
        if isinstance(data, list):
            return data
        if isinstance(data, dict):
            return list(data.values())
    return []


def _read_json_dict(path: Path) -> Dict[str, Any]:
    if not path.exists():
        return {}
    with path.open("r", encoding="utf-8") as f:
        data = json.load(f)
        if isinstance(data, dict):
            return data
        if isinstance(data, list):
            # assume list of dicts with id field
            out: Dict[str, Any] = {}
            for row in data:
                if isinstance(row, dict):
                    rid = str(row.get("id") or row.get("name") or "").strip()
                    if rid:
                        out[rid] = row
            return out
    return {}


def _sorted_ids(data: Dict[str, Any]) -> List[str]:
    return sorted(data.keys(), key=str.lower)


@dataclass
class StageRef:
    quest_id: str
    stage_index: int


@dataclass
class FlowBeat:
    beat_type: str  # dialogue/event/cutscene/tutorial/note
    ref_id: str
    label: str = ""
    metadata: Dict[str, Any] = field(default_factory=dict)


class DialogueDialog(QDialog):
    def __init__(self, parent: QWidget, existing_ids: List[str], data: Optional[dict] = None):
        super().__init__(parent)
        self.setWindowTitle("Dialogue Beat")
        self.resize(520, 420)
        self._existing = set(existing_ids)
        self._data = data or {}

        layout = QVBoxLayout(self)
        form = QFormLayout()

        self.id_edit = QLineEdit(self._data.get("id", ""))
        self.speaker_edit = QLineEdit(self._data.get("speaker", ""))
        self.text_edit = QTextEdit()
        self.text_edit.setPlainText(self._data.get("text", ""))
        self.condition_edit = QLineEdit(self._data.get("condition", ""))
        self.trigger_edit = QLineEdit(self._data.get("trigger", ""))
        self.next_edit = QLineEdit(self._data.get("next", ""))

        form.addRow("ID:", self.id_edit)
        form.addRow("Speaker:", self.speaker_edit)
        form.addRow(QLabel("Text:"))
        form.addRow(self.text_edit)
        form.addRow("Condition:", self.condition_edit)
        form.addRow("Trigger:", self.trigger_edit)
        form.addRow("Next ID:", self.next_edit)

        layout.addLayout(form)

        self.button_box = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        self.button_box.accepted.connect(self._accept)
        self.button_box.rejected.connect(self.reject)
        layout.addWidget(self.button_box)

    def _accept(self):
        did = self.id_edit.text().strip()
        if not did:
            QMessageBox.warning(self, "Missing ID", "Dialogue ID is required.")
            return
        if did != self._data.get("id") and did in self._existing:
            QMessageBox.warning(self, "Duplicate ID", f"Dialogue '{did}' already exists.")
            return
        text = self.text_edit.toPlainText().strip()
        if not text:
            QMessageBox.warning(self, "Missing Text", "Dialogue text cannot be empty.")
            return
        self._data = {
            "id": did,
            "speaker": self.speaker_edit.text().strip(),
            "text": text,
        }
        if self.condition_edit.text().strip():
            self._data["condition"] = self.condition_edit.text().strip()
        if self.trigger_edit.text().strip():
            self._data["trigger"] = self.trigger_edit.text().strip()
        if self.next_edit.text().strip():
            self._data["next"] = self.next_edit.text().strip()
        self.accept()

    def data(self) -> dict:
        return dict(self._data)

class EventActionDialog(QDialog):
    def __init__(self, parent: QWidget, existing_ids: List[str], data: Optional[dict] = None):
        super().__init__(parent)
        self.setWindowTitle("Event Beat")
        self.resize(520, 420)
        self._existing = set(existing_ids)
        self._data = data or {}

        layout = QVBoxLayout(self)
        form = QFormLayout()

        self.id_edit = QLineEdit(self._data.get("id", ""))
        self.description_edit = QLineEdit(self._data.get("description", ""))
        self.trigger_type_combo = QComboBox()
        self.trigger_type_combo.addItems(sorted(TRIGGER_TYPES.keys()))
        self.trigger_type_combo.setCurrentText(self._data.get("trigger", {}).get("type", ""))

        form.addRow("ID:", self.id_edit)
        form.addRow("Description:", self.description_edit)
        form.addRow("Trigger Type:", self.trigger_type_combo)

        layout.addLayout(form)

        self.button_box = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        self.button_box.accepted.connect(self._accept)
        self.button_box.rejected.connect(self.reject)
        layout.addWidget(self.button_box)

    def _accept(self):
        did = self.id_edit.text().strip()
        if not did:
            QMessageBox.warning(self, "Missing ID", "Event ID is required.")
            return
        if did != self._data.get("id") and did in self._existing:
            QMessageBox.warning(self, "Duplicate ID", f"Event '{did}' already exists.")
            return
        self._data = {
            "id": did,
            "description": self.description_edit.text().strip(),
            "trigger": {
                "type": self.trigger_type_combo.currentText()
            }
        }
        self.accept()

    def data(self) -> dict:
        return dict(self._data)


class CutsceneDialog(QDialog):
    def __init__(self, parent: QWidget, existing_ids: List[str], scene_id: Optional[str] = None, steps: Optional[List[dict]] = None):
        super().__init__(parent)
        self.setWindowTitle("Cutscene Beat")
        self.resize(580, 500)
        self._existing = set(existing_ids)
        self._scene_id = scene_id or ""
        self._steps = steps or [{"type": "dialogue", "actor": "", "line": ""}]

        layout = QVBoxLayout(self)
        form = QFormLayout()
        self.id_edit = QLineEdit(self._scene_id)
        form.addRow("Scene ID:", self.id_edit)
        layout.addLayout(form)

        self.raw_edit = QPlainTextEdit()
        self.raw_edit.setPlaceholderText("Enter list of steps as JSON array…")
        self.raw_edit.setPlainText(json.dumps(self._steps, indent=2))
        layout.addWidget(self.raw_edit, 1)

        self.button_box = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        self.button_box.accepted.connect(self._accept)
        self.button_box.rejected.connect(self.reject)
        layout.addWidget(self.button_box)

    def _accept(self):
        sid = self.id_edit.text().strip()
        if not sid:
            QMessageBox.warning(self, "Missing ID", "Scene ID is required.")
            return
        if sid != self._scene_id and sid in self._existing:
            QMessageBox.warning(self, "Duplicate ID", f"Scene '{sid}' already exists.")
            return
        try:
            parsed = json.loads(self.raw_edit.toPlainText() or "[]")
            if not isinstance(parsed, list):
                raise ValueError("Expected a JSON array of steps.")
        except Exception as exc:
            QMessageBox.warning(self, "Invalid JSON", f"Could not parse steps:\n{exc}")
            return
        self._scene_id = sid
        self._steps = parsed
        self.accept()

    def data(self) -> Tuple[str, List[dict]]:
        return self._scene_id, list(self._steps)


class StageNotesDialog(QDialog):
    def __init__(self, parent: QWidget, text: str, flavor: str):
        super().__init__(parent)
        self.setWindowTitle("Stage Notes")
        self.resize(480, 480)
        layout = QVBoxLayout(self)
        self.edit = QTextEdit()
        self.edit.setPlainText(text)
        layout.addWidget(self.edit)

        self.flavor_edit = QTextEdit()
        self.flavor_edit.setPlaceholderText("Flavor text / VO direction / lore tidbits…")
        self.flavor_edit.setPlainText(flavor)
        layout.addWidget(self.flavor_edit)

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def value(self) -> Tuple[str, str]:
        return self.edit.toPlainText(), self.flavor_edit.toPlainText()


class TemplateVariablesDialog(QDialog):
    def __init__(self, parent: QWidget, variables: List[str]):
        super().__init__(parent)
        self.setWindowTitle("Template Variables")
        self.setMinimumWidth(400)
        self.variables = variables
        self.edits = {}

        layout = QVBoxLayout(self)
        form = QFormLayout()

        for var in self.variables:
            edit = QLineEdit()
            self.edits[var] = edit
            form.addRow(f"{{{{ {var} }}}}:", edit)

        layout.addLayout(form)

        self.button_box = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        self.button_box.accepted.connect(self.accept)
        self.button_box.rejected.connect(self.reject)
        layout.addWidget(self.button_box)

    def get_values(self) -> Dict[str, str]:
        return {var: edit.text() for var, edit in self.edits.items()}


class ConditionalQuestWizard(QDialog):
    def __init__(self, parent: QWidget, room_ids: List[str]):
        super().__init__(parent)
        self.setWindowTitle("Create Conditional Quest")
        self.setMinimumWidth(600)

        self.edits = {}
        layout = QVBoxLayout(self)
        form = QFormLayout()

        # Define fields for the wizard
        self.edits["npc_name"] = QLineEdit()
        self.edits["npc_name"].setPlaceholderText("e.g., Ollie")
        form.addRow("NPC Name", self.edits["npc_name"])

        self.edits["room_id"] = QComboBox()
        self.edits["room_id"].addItems([""] + room_ids)
        form.addRow("Room ID for Lure:", self.edits["room_id"])

        self.edits["quest_id"] = QLineEdit()
        self.edits["quest_id"].setPlaceholderText("e.g., talk_to_jed")
        form.addRow("Quest ID to Start", self.edits["quest_id"])

        self.edits["lure_dialogue"] = QLineEdit()
        self.edits["lure_dialogue"].setPlaceholderText("e.g., Nova! Go talk to Jed.")
        form.addRow("Lure Cinematic Dialogue", self.edits["lure_dialogue"])

        self.edits["quest_dialogue_text"] = QLineEdit()
        self.edits["quest_dialogue_text"].setPlaceholderText("e.g., Thanks for talking to me. Please go see Jed.")
        form.addRow("Quest-Giving Dialogue", self.edits["quest_dialogue_text"])

        self.edits["post_quest_dialogue_text"] = QLineEdit()
        self.edits["post_quest_dialogue_text"].setPlaceholderText("e.g., You should get going, Jed's waiting!")
        form.addRow("Post-Quest Dialogue", self.edits["post_quest_dialogue_text"])

        layout.addLayout(form)

        self.button_box = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        self.button_box.accepted.connect(self.accept)
        self.button_box.rejected.connect(self.reject)
        layout.addWidget(self.button_box)

    def get_values(self) -> Dict[str, str]:
        # Auto-generate IDs and milestones from the base names
        values = {}
        for key, widget in self.edits.items():
            if isinstance(widget, QLineEdit):
                values[key] = widget.text().strip()
            elif isinstance(widget, QComboBox):
                values[key] = widget.currentText()
        npc_id_safe = values["npc_name"].lower().replace(" ", "_")
        quest_id_safe = values["quest_id"].lower()

        values["npc_id"] = npc_id_safe
        values["event_id"] = f"evt_meet_{npc_id_safe}_for_{quest_id_safe}"
        values["cinematic_id"] = f"cin_{npc_id_safe}_{quest_id_safe}_lure"
        values["met_milestone"] = f"ms_met_{npc_id_safe}"
        values["quest_started_milestone"] = f"ms_{quest_id_safe}_started"
        values["quest_dialogue_id"] = f"dlg_{npc_id_safe}_{quest_id_safe}_prompt"
        values["post_quest_dialogue_id"] = f"dlg_{npc_id_safe}_{quest_id_safe}_post"

        return values


class FetchQuestWizard(QDialog):
    def __init__(self, parent: QWidget, item_ids: List[str], npc_ids: List[str]):
        super().__init__(parent)
        self.setWindowTitle("Create Fetch Quest")
        self.setMinimumWidth(600)

        self.edits = {}
        layout = QVBoxLayout(self)
        form = QFormLayout()

        self.edits["quest_name"] = QLineEdit()
        self.edits["quest_name"].setPlaceholderText("e.g., The Missing Relic")
        form.addRow("Quest Name", self.edits["quest_name"])

        self.edits["item_id"] = QComboBox()
        self.edits["item_id"].addItems([""] + item_ids)
        form.addRow("Item to Fetch", self.edits["item_id"])

        self.edits["quantity"] = QLineEdit("1")
        self.edits["quantity"].setValidator(QIntValidator())
        form.addRow("Quantity", self.edits["quantity"])

        self.edits["giver_npc_id"] = QComboBox()
        self.edits["giver_npc_id"].addItems([""] + npc_ids)
        form.addRow("Quest Giver NPC", self.edits["giver_npc_id"])

        self.edits["delivery_npc_id"] = QComboBox()
        self.edits["delivery_npc_id"].addItems([""] + npc_ids)
        form.addRow("Delivery NPC (Optional)", self.edits["delivery_npc_id"])

        layout.addLayout(form)

        self.button_box = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        self.button_box.accepted.connect(self.accept)
        self.button_box.rejected.connect(self.reject)
        layout.addWidget(self.button_box)

    def get_values(self) -> Dict[str, str]:
        values = {}
        for key, widget in self.edits.items():
            if isinstance(widget, QLineEdit):
                values[key] = widget.text().strip()
            elif isinstance(widget, QComboBox):
                values[key] = widget.currentText()

        quest_name_safe = values["quest_name"].lower().replace(" ", "_")
        item_id_safe = values["item_id"].lower()

        values["quest_id"] = f"quest_fetch_{quest_name_safe}"
        values["start_dialogue_id"] = f"dlg_fetch_{item_id_safe}_start"
        values["complete_dialogue_id"] = f"dlg_fetch_{item_id_safe}_complete"
        values["milestone_item_obtained"] = f"ms_fetch_{item_id_safe}_obtained"
        values["milestone_quest_started"] = f"ms_fetch_{item_id_safe}_started"

        return values


class KillQuestWizard(QDialog):
    def __init__(self, parent: QWidget, npc_ids: List[str], room_ids: List[str]):
        super().__init__(parent)
        self.setWindowTitle("Create Kill Quest")
        self.setMinimumWidth(600)

        self.edits = {}
        layout = QVBoxLayout(self)
        form = QFormLayout()

        self.edits["quest_name"] = QLineEdit()
        self.edits["quest_name"].setPlaceholderText("e.g., The Goblin Menace")
        form.addRow("Quest Name", self.edits["quest_name"])

        self.edits["target_enemy_id"] = QComboBox()
        self.edits["target_enemy_id"].addItems([""] + npc_ids) # Assuming enemies are NPCs for now
        form.addRow("Target Enemy", self.edits["target_enemy_id"])

        self.edits["quantity"] = QLineEdit("1")
        self.edits["quantity"].setValidator(QIntValidator())
        form.addRow("Quantity", self.edits["quantity"])

        self.edits["location_room_id"] = QComboBox()
        self.edits["location_room_id"].addItems([""] + room_ids)
        form.addRow("Location (Room ID)", self.edits["location_room_id"])

        self.edits["giver_npc_id"] = QComboBox()
        self.edits["giver_npc_id"].addItems([""] + npc_ids)
        form.addRow("Quest Giver NPC", self.edits["giver_npc_id"])

        layout.addLayout(form)

        self.button_box = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        self.button_box.accepted.connect(self.accept)
        self.button_box.rejected.connect(self.reject)
        layout.addWidget(self.button_box)

    def get_values(self) -> Dict[str, str]:
        values = {}
        for key, widget in self.edits.items():
            if isinstance(widget, QLineEdit):
                values[key] = widget.text().strip()
            elif isinstance(widget, QComboBox):
                values[key] = widget.currentText()

        quest_name_safe = values["quest_name"].lower().replace(" ", "_")
        target_enemy_id_safe = values["target_enemy_id"].lower()

        values["quest_id"] = f"quest_kill_{quest_name_safe}"
        values["start_dialogue_id"] = f"dlg_kill_{target_enemy_id_safe}_start"
        values["complete_dialogue_id"] = f"dlg_kill_{target_enemy_id_safe}_complete"
        values["milestone_enemies_killed"] = f"ms_kill_{target_enemy_id_safe}_count"
        values["milestone_quest_started"] = f"ms_kill_{target_enemy_id_safe}_started"

        return values


class QuestCompletionWizard(QDialog):
    def __init__(self, parent: QWidget, quests: List[str], npc_ids: List[str], room_ids: List[str], item_ids: List[str]):
        super().__init__(parent)
        self.setWindowTitle("Create Quest Completion Event")
        self.setMinimumWidth(500)

        layout = QVBoxLayout(self)
        form = QFormLayout()

        # Quest ID
        self.quest_combo = QComboBox()
        self.quest_combo.addItems(quests)
        form.addRow("Quest to Complete:", self.quest_combo)

        # Trigger Type
        self.trigger_combo = QComboBox()
        self.trigger_combo.addItems(["Talk to NPC", "Enter Room", "Player Action"])
        form.addRow("Trigger Type:", self.trigger_combo)

        # Dynamic Trigger Value
        self.trigger_stack = QStackedWidget()
        self.npc_combo = QComboBox()
        self.npc_combo.addItems([""] + npc_ids)
        self.room_combo = QComboBox()
        self.room_combo.addItems([""] + room_ids)
        self.action_edit = QLineEdit()
        self.action_edit.setPlaceholderText("e.g., boss_defeated")
        self.trigger_stack.addWidget(self.npc_combo)
        self.trigger_stack.addWidget(self.room_combo)
        self.trigger_stack.addWidget(self.action_edit)
        form.addRow("Trigger Value:", self.trigger_stack)

        self.trigger_combo.currentIndexChanged.connect(self.trigger_stack.setCurrentIndex)

        # Optional completion dialogue
        self.dialogue_edit = QLineEdit()
        self.dialogue_edit.setPlaceholderText("(Optional)")
        form.addRow("Completion Dialogue ID:", self.dialogue_edit)

        self.item_reward_combo = QComboBox()
        self.item_reward_combo.addItems([""] + item_ids)
        form.addRow("Item Reward:", self.item_reward_combo)

        layout.addLayout(form)

        self.button_box = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        self.button_box.accepted.connect(self.accept)
        self.button_box.rejected.connect(self.reject)
        layout.addWidget(self.button_box)

    def get_values(self) -> Dict[str, str]:
        quest_id = self.quest_combo.currentText()
        trigger_type_text = self.trigger_combo.currentText()
        dialogue_id = self.dialogue_edit.text().strip()

        trigger_value = ""
        current_widget = self.trigger_stack.currentWidget()
        if isinstance(current_widget, QComboBox):
            trigger_value = current_widget.currentText()
        elif isinstance(current_widget, QLineEdit):
            trigger_value = current_widget.text().strip()

        trigger_type_map = {
            "Talk to NPC": "talk_to",
            "Enter Room": "enter_room",
            "Player Action": "player_action"
        }
        trigger_key_map = {
            "talk_to": "npc",
            "enter_room": "room",
            "player_action": "action"
        }

        trigger_type = trigger_type_map[trigger_type_text]
        trigger_key = trigger_key_map[trigger_type]

        return {
            "quest_id": quest_id,
            "trigger_type": trigger_type,
            "trigger_key": trigger_key,
            "trigger_value": trigger_value,
            "dialogue_id": dialogue_id,
            "item_reward": self.item_reward_combo.currentText()
        }


class NarrativeBuilder(QWidget):
    def __init__(self, project_root: Optional[Path] = None):
        super().__init__()
        self.setWindowTitle("Starborn Narrative Builder")
        self.resize(1400, 860)
        paths = resolve_paths(project_root or Path(__file__).parent)
        self.project_root = paths.project_root
        self.assets_root = paths.assets_dir

        self.quest_path = self.assets_root / "quests.json"
        self.dialogue_path = self.assets_root / "dialogue.json"
        self.events_path = self.assets_root / "events.json"
        self.cinematics_path = self.assets_root / "cinematics.json"
        self.flow_path = self.assets_root / narrative_flow_filename
        self.npcs_path = self.assets_root / "npcs.json"
        self.hubs_path = self.assets_root / "hubs.json"
        self.nodes_path = self.assets_root / "hub_nodes.json"
        self.rooms_path = self.assets_root / "rooms.json"
        self.creative_prompts_path = self.project_root / "data" / "creative_prompts.json"

        self.quests: List[dict] = []
        self.dialogues: Dict[str, dict] = {}
        self.events: Dict[str, dict] = {}
        self.cinematics: Dict[str, List[dict]] = {}
        self.flows: Dict[str, Dict[str, List[dict]]] = {}
        self.npc_ids: List[str] = []
        self.hub_ids: List[str] = []
        self.node_ids: List[str] = []
        self.room_ids: List[str] = []

        self.current_quest_id: Optional[str] = None
        self.current_stage_index: Optional[int] = None
        self._updating_ui = False
        self._dirty = False

        self._load_all()
        self._build_ui()
        self._mark_dirty(False)
        self._refresh_quest_list()

    # ----------------------- data I/O -----------------------
    def _load_all(self):
        self.quests = json_load(self.quest_path, default=[]) or []
        self.dialogues = _read_json_dict(self.dialogue_path)
        events = json_load(self.events_path, default=[]) or []
        self.events = {row["id"]: row for row in events if isinstance(row, dict) and row.get("id")}
        self.cinematics = _read_json_dict(self.cinematics_path)
        self.flows = json_load(self.flow_path, default={}) or {}

        # Load data for dropdowns
        npcs = _read_json_list(self.npcs_path)
        self.npc_ids = sorted([npc.get("id", "") for npc in npcs if npc.get("id")])
        hubs = _read_json_dict(self.hubs_path)
        self.hub_ids = sorted(hubs.keys())
        nodes = _read_json_dict(self.nodes_path)
        self.node_ids = sorted(nodes.keys())
        rooms = _read_json_dict(self.rooms_path)
        self.room_ids = sorted(rooms.keys())

        if self.creative_prompts_path.exists():
            self.creative_prompts = json_load(self.creative_prompts_path, default=[])
        else:
            self.creative_prompts = [
                "What if the player character has a secret connection to the main antagonist of {quest}?",
                "How could this stage, {stage}, reveal a surprising new detail about the world's lore?",
                "What if a seemingly insignificant NPC in {stage} holds the key to resolving the main conflict of {quest}?",
                "How can the environment in {stage} be used to create a sense of foreshadowing for a future event in {quest}?",
                "What if the player's actions in a previous quest have an unexpected consequence in {stage}?",
            ]
            json_save(self.creative_prompts_path, self.creative_prompts, indent=2)

    def _update_window_title(self):
        quest = self._find_quest(self.current_quest_id)
        window_title = "Starborn Narrative Builder"
        if quest:
            label = quest.get("title") or quest.get("id") or ""
            if label:
                window_title += f" — {label}"
        if self._dirty:
            window_title += " *"
        self.setWindowTitle(window_title)
        if hasattr(self, "save_btn"):
            self.save_btn.setEnabled(self._dirty)

    def _mark_dirty(self, dirty: bool = True):
        self._dirty = dirty
        self._update_window_title()

    def _save_all(self):
        try:
            json_save(self.quest_path, self.quests, sort_obj=True, indent=2)
            json_save(self.dialogue_path, [self.dialogues[k] for k in _sorted_ids(self.dialogues)], indent=2)
            json_save(self.events_path, [self.events[k] for k in _sorted_ids(self.events)], indent=2)
            json_save(self.cinematics_path, self.cinematics, indent=2)
            json_save(self.flow_path, self.flows, indent=2)
        except Exception as exc:
            QMessageBox.critical(self, "Save failed", f"Could not save data:\n{exc}")
            return
        self._mark_dirty(False)
        QMessageBox.information(self, "Saved", "Narrative content saved.")

    # ------------------------ UI build ----------------------
    def _build_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(10, 10, 10, 10)
        layout.setSpacing(8)

        header_row = QHBoxLayout()
        header_text = QVBoxLayout()
        title = QLabel("Narrative Playground")
        title.setStyleSheet("font-size: 22px; font-weight: 600;")
        subtitle = QLabel("Sketch quests, stage emotional beats, and stitch dialogue without leaving the flow.")
        subtitle.setWordWrap(True)
        subtitle.setStyleSheet("color: #6f6f6f;")
        header_text.addWidget(title)
        header_text.addWidget(subtitle)
        header_row.addLayout(header_text)
        header_row.addStretch(1)
        self.save_btn = QPushButton("Save All")
        self.save_btn.setShortcut("Ctrl+S")
        self.save_btn.setToolTip("Save quests, dialogue, events, cinematics, and flow maps (Ctrl+S)")
        header_row.addWidget(self.save_btn)
        layout.addLayout(header_row)

        focus_row = QHBoxLayout()
        focus_label = QLabel("Current focus:")
        focus_label.setStyleSheet("font-weight: 500;")
        self.header_label = QLabel("Select a quest…")
        self.header_label.setStyleSheet("font-weight: 600;")
        focus_row.addWidget(focus_label)
        focus_row.addWidget(self.header_label, 1)
        self.status_hint_label = QLabel("Tip: double-click a beat to open its micro editor.")
        self.status_hint_label.setStyleSheet("color: #7a7a7a;")
        focus_row.addWidget(self.status_hint_label)
        layout.addLayout(focus_row)

        self.main_split = QSplitter(Qt.Horizontal)
        layout.addWidget(self.main_split, 1)

        self.library_panel = self._build_library_panel()
        self.canvas_panel = self._build_canvas_panel()
        self.stage_lab_panel = self._build_stage_lab_panel()

        self.main_split.addWidget(self.library_panel)
        self.main_split.addWidget(self.canvas_panel)
        self.main_split.addWidget(self.stage_lab_panel)
        self.main_split.setStretchFactor(0, 0)
        self.main_split.setStretchFactor(1, 1)
        self.main_split.setStretchFactor(2, 1)

        footer = QHBoxLayout()
        self.open_loc_btn = QPushButton("Reveal Data Files…")
        self.open_loc_btn.setToolTip("Open the underlying narrative JSON files for inspection.")
        self.launch_editor_btn = QPushButton("Open Specialized Editor…")
        self.launch_editor_btn.setToolTip("Jump to the detailed quest/dialogue/event editors.")
        footer.addWidget(self.open_loc_btn)
        footer.addWidget(self.launch_editor_btn)
        footer.addStretch(1)
        layout.addLayout(footer)

        self.save_btn.clicked.connect(self._save_all)
        self.open_loc_btn.clicked.connect(self._reveal_files)
        self.launch_editor_btn.clicked.connect(self._launch_editor_picker)

        self._refresh_stage_lab_header()
        self._refresh_creative_prompt(force=True)
        self._refresh_quest_outline()
        self._refresh_stage_outline()

    def _build_library_panel(self) -> QWidget:
        panel = QWidget()
        panel.setMinimumWidth(280)
        layout = QVBoxLayout(panel)
        layout.setContentsMargins(8, 0, 8, 0)
        layout.setSpacing(8)

        title = QLabel("Quest Library")
        title.setStyleSheet("font-size: 15px; font-weight: 600;")
        layout.addWidget(title)

        caption = QLabel("Browse arcs, filter for vibes, and dive into a quest with a single click.")
        caption.setWordWrap(True)
        caption.setStyleSheet("color: #7a7a7a;")
        layout.addWidget(caption)

        self.quest_search = QLineEdit()
        self.quest_search.setPlaceholderText("Search by title, ID, or mood…")
        self.quest_search.textChanged.connect(self._refresh_quest_list)
        layout.addWidget(self.quest_search)

        self.quest_list = QListWidget()
        self.quest_list.setAlternatingRowColors(True)
        self.quest_list.setSelectionMode(QListWidget.SingleSelection)
        self.quest_list.setSpacing(4)
        self.quest_list.currentItemChanged.connect(self._on_quest_selected)
        self.quest_list.itemDoubleClicked.connect(self._open_quest_in_editor)
        layout.addWidget(self.quest_list, 1)

        self._quest_delete_shortcut = QShortcut(QKeySequence(Qt.Key_Delete), self.quest_list)
        self._quest_delete_shortcut.activated.connect(self._delete_quest)

        actions_box = QGroupBox("Create & Remix")
        actions_layout = QGridLayout(actions_box)
        actions_layout.setSpacing(6)
        actions_layout.setContentsMargins(6, 6, 6, 6)

        self.add_quest_btn = QPushButton("Fresh Quest")
        self.add_quest_template_btn = QPushButton("From Template")
        self.add_cond_quest_btn = QPushButton("Conditional Quest Wizard")
        self.add_fetch_quest_btn = QPushButton("Fetch Quest Wizard")
        self.add_kill_quest_btn = QPushButton("Kill Quest Wizard")
        self.add_complete_quest_btn = QPushButton("Completion Event Wizard")
        self.dup_quest_btn = QPushButton("Duplicate")
        self.del_quest_btn = QPushButton("Delete Quest")

        actions_layout.addWidget(self.add_quest_btn, 0, 0)
        actions_layout.addWidget(self.add_quest_template_btn, 0, 1)
        actions_layout.addWidget(self.add_cond_quest_btn, 1, 0)
        actions_layout.addWidget(self.add_fetch_quest_btn, 2, 0)
        actions_layout.addWidget(self.add_kill_quest_btn, 2, 1)
        actions_layout.addWidget(self.add_complete_quest_btn, 1, 1)
        actions_layout.addWidget(self.dup_quest_btn, 2, 0)
        actions_layout.addWidget(self.del_quest_btn, 2, 1)
        layout.addWidget(actions_box)

        layout.addStretch(1)

        self.add_quest_btn.clicked.connect(self._add_quest)
        self.add_quest_template_btn.clicked.connect(self._add_quest_from_template)
        self.add_cond_quest_btn.clicked.connect(self._create_conditional_quest)
        self.add_fetch_quest_btn.clicked.connect(self._create_fetch_quest)
        self.add_kill_quest_btn.clicked.connect(self._create_kill_quest)
        self.add_complete_quest_btn.clicked.connect(self._create_quest_completion_event)
        self.dup_quest_btn.clicked.connect(self._duplicate_quest)
        self.del_quest_btn.clicked.connect(self._delete_quest)
        self.del_quest_btn.setToolTip("Delete the selected quest (confirmation required).")

        return panel

    def _build_canvas_panel(self) -> QWidget:
        panel = QWidget()
        layout = QVBoxLayout(panel)
        layout.setContentsMargins(12, 0, 12, 0)
        layout.setSpacing(10)

        quest_box = QGroupBox("Quest Canvas")
        quest_layout = QVBoxLayout(quest_box)
        quest_layout.setSpacing(6)

        info = QLabel("Capture the quest identity up top so every beat stays anchored in intent.")
        info.setWordWrap(True)
        info.setStyleSheet("color: #7a7a7a;")
        quest_layout.addWidget(info)

        quest_form = QFormLayout()
        quest_form.setLabelAlignment(Qt.AlignRight)
        quest_form.setSpacing(4)
        quest_form.setContentsMargins(0, 0, 0, 0)

        def _paired_row(*widgets):
            container = QWidget()
            row_layout = QHBoxLayout(container)
            row_layout.setContentsMargins(0, 0, 0, 0)
            row_layout.setSpacing(6)
            for entry in widgets:
                widget = entry
                stretch = 1
                if isinstance(entry, tuple):
                    widget, stretch = entry
                row_layout.addWidget(widget, stretch)
            return container

        self.quest_id_label = QPushButton("-")
        self.quest_id_label.setStyleSheet("text-align: left; border: none; color: blue; text-decoration: underline;")
        self.quest_id_label.clicked.connect(self._open_quest_in_editor)
        self.quest_title_edit = QLineEdit()
        self.quest_title_edit.setPlaceholderText("Quest title or emotional hook…")
        self.quest_summary_edit = QTextEdit()
        self.quest_summary_edit.setPlaceholderText("High-level summary for the quest…")
        self.quest_summary_edit.setMinimumHeight(60)
        self.quest_desc_edit = QTextEdit()
        self.quest_desc_edit.setPlaceholderText("What happens? Focus on the player fantasy and key beats.")
        self.quest_desc_edit.setMinimumHeight(110)
        self.quest_flavor_edit = QTextEdit() # This field was missing from the UI build
        self.quest_flavor_edit.setPlaceholderText("Flavor / VO direction / lore tidbits…") # This field was missing from the UI build
        self.quest_flavor_edit.setMinimumHeight(60) # This field was missing from the UI build
        self.quest_giver_edit = QComboBox()
        self.quest_giver_edit.addItems([""] + self.npc_ids)
        self.quest_hub_edit = QComboBox()
        self.quest_hub_edit.addItems([""] + self.hub_ids)
        self.quest_node_edit = QComboBox()
        self.quest_node_edit.addItems([""] + self.node_ids)
        self.quest_mood_edit = QLineEdit()
        self.quest_mood_edit.setPlaceholderText("e.g., mysterious, upbeat, tense")
        self.quest_rewards_edit = QLineEdit()
        # --- NEW: Add missing quest fields ---
        self.quest_type_combo = QComboBox()
        self.quest_type_combo.addItems(["", "main", "side", "character", "fetch", "explore", "talk", "defeat", "craft", "interact"])
        self.quest_status_combo = QComboBox()
        self.quest_status_combo.addItems(["inactive", "active", "complete", "failed"])
        for combo in (self.quest_type_combo, self.quest_status_combo, self.quest_giver_edit, self.quest_hub_edit, self.quest_node_edit):
            combo.setSizePolicy(QSizePolicy.Expanding, QSizePolicy.Fixed)
        self.quest_required_item_edit = QLineEdit()
        self.quest_start_dialogue_edit = QLineEdit()
        self.quest_completion_dialogue_edit = QLineEdit()
        self.quest_rewards_edit.setPlaceholderText("e.g., 100xp, 50gold, item_id")
        self.quest_autostart_checkbox = QCheckBox()

        quest_form.addRow("Quest ID:", self.quest_id_label)
        quest_form.addRow("Title:", self.quest_title_edit)
        quest_form.addRow(QLabel("Summary:"), self.quest_summary_edit)
        quest_form.addRow(QLabel("Description:"), self.quest_desc_edit)
        quest_form.addRow(QLabel("Flavor:"), self.quest_flavor_edit) # This field was missing from the UI build
        quest_form.addRow("Type / Status:", _paired_row(self.quest_type_combo, self.quest_status_combo))
        quest_form.addRow("Quest Giver / Hub:", _paired_row(self.quest_giver_edit, self.quest_hub_edit))
        quest_form.addRow("Node / Required Item:", _paired_row(self.quest_node_edit, self.quest_required_item_edit))
        quest_form.addRow("Mood / Rewards:", _paired_row(self.quest_mood_edit, self.quest_rewards_edit))
        quest_form.addRow("Start / Completion Dialogue:", _paired_row(self.quest_start_dialogue_edit, self.quest_completion_dialogue_edit))
        quest_form.addRow("Autostart:", self.quest_autostart_checkbox)
        quest_layout.addLayout(quest_form)
        layout.addWidget(quest_box)

        # --- NEW: Connect signals for new fields ---
        self.quest_type_combo.currentTextChanged.connect(self._quest_meta_changed)
        self.quest_status_combo.currentTextChanged.connect(self._quest_meta_changed)
        self.quest_required_item_edit.textChanged.connect(self._quest_meta_changed)
        self.quest_title_edit.textChanged.connect(self._quest_meta_changed)
        self.quest_summary_edit.textChanged.connect(self._quest_meta_changed)
        self.quest_desc_edit.textChanged.connect(self._quest_meta_changed)
        self.quest_flavor_edit.textChanged.connect(self._quest_meta_changed)
        self.quest_giver_edit.currentTextChanged.connect(self._quest_meta_changed)
        self.quest_hub_edit.currentTextChanged.connect(self._quest_meta_changed)
        self.quest_node_edit.currentTextChanged.connect(self._quest_meta_changed)
        self.quest_mood_edit.textChanged.connect(self._quest_meta_changed)
        self.quest_rewards_edit.textChanged.connect(self._quest_meta_changed)
        self.quest_start_dialogue_edit.textChanged.connect(self._quest_meta_changed)
        self.quest_completion_dialogue_edit.textChanged.connect(self._quest_meta_changed)
        self.quest_autostart_checkbox.stateChanged.connect(self._quest_meta_changed)

        stage_box = QGroupBox("Stage Timeline")
        stage_layout = QVBoxLayout(stage_box)
        stage_layout.setSpacing(6)

        stage_info = QLabel("Lay out the flow. Think in beats: setup, twist, payoff, aftermath.")
        stage_info.setWordWrap(True)
        stage_info.setStyleSheet("color: #7a7a7a;")
        stage_layout.addWidget(stage_info)

        search_row = QHBoxLayout()
        search_label = QLabel("Filter:")
        self.stage_search = QLineEdit()
        self.stage_search.setPlaceholderText("Search stages or tasks…")
        self.stage_search.setClearButtonEnabled(True)
        self.stage_search.textChanged.connect(self._refresh_stage_list)
        search_row.addWidget(search_label)
        search_row.addWidget(self.stage_search, 1)
        stage_layout.addLayout(search_row)

        self.stage_list = QListWidget()
        self.stage_list.setViewMode(QListWidget.ListMode)
        self.stage_list.setSpacing(8)
        self.stage_list.setUniformItemSizes(False)
        self.stage_list.setWordWrap(True)
        self.stage_list.setAlternatingRowColors(True)
        self.stage_list.setResizeMode(QListWidget.Adjust)
        self.stage_list.setMovement(QListWidget.Static)
        self.stage_list.setSelectionMode(QListWidget.SingleSelection)
        self.stage_list.currentItemChanged.connect(self._on_stage_selected)
        stage_layout.addWidget(self.stage_list, 1)

        self._stage_delete_shortcut = QShortcut(QKeySequence(Qt.Key_Delete), self.stage_list)
        self._stage_delete_shortcut.activated.connect(self._delete_stage)

        stage_buttons = QHBoxLayout()
        self.add_stage_btn = QPushButton("New Stage")
        self.dup_stage_btn = QPushButton("Duplicate")
        self.del_stage_btn = QPushButton("Delete")
        self.move_stage_left_btn = QPushButton("◀")
        self.move_stage_right_btn = QPushButton("▶")
        stage_buttons.addWidget(self.add_stage_btn)
        stage_buttons.addWidget(self.dup_stage_btn)
        stage_buttons.addWidget(self.del_stage_btn)
        stage_buttons.addStretch(1)
        stage_buttons.addWidget(self.move_stage_left_btn)
        stage_buttons.addWidget(self.move_stage_right_btn)
        stage_layout.addLayout(stage_buttons)
        layout.addWidget(stage_box, 1)

        outline_box = QGroupBox("Quest Outline")
        outline_layout = QVBoxLayout(outline_box)
        outline_layout.setSpacing(6)
        outline_hint = QLabel("Live document of every stage, task, and beat — perfect for reviews or export.")
        outline_hint.setWordWrap(True)
        outline_hint.setStyleSheet("color: #7a7a7a;")
        outline_layout.addWidget(outline_hint)
        self.quest_outline_edit = QPlainTextEdit()
        self.quest_outline_edit.setReadOnly(True)
        self.quest_outline_edit.setPlaceholderText("Select a quest to see its outline.")
        self.quest_outline_edit.setFixedHeight(180)
        outline_layout.addWidget(self.quest_outline_edit)
        outline_controls = QHBoxLayout()
        self.refresh_quest_outline_btn = QPushButton("Refresh Outline")
        self.copy_quest_outline_btn = QPushButton("Copy Outline")
        outline_controls.addWidget(self.refresh_quest_outline_btn)
        outline_controls.addWidget(self.copy_quest_outline_btn)
        outline_controls.addStretch(1)
        outline_layout.addLayout(outline_controls)
        layout.addWidget(outline_box)

        self.add_stage_btn.clicked.connect(self._add_stage)
        self.dup_stage_btn.clicked.connect(self._duplicate_stage)
        self.del_stage_btn.clicked.connect(self._delete_stage)
        self.move_stage_left_btn.clicked.connect(lambda: self._move_stage(-1))
        self.move_stage_right_btn.clicked.connect(lambda: self._move_stage(+1))

        self.refresh_quest_outline_btn.clicked.connect(self._refresh_quest_outline)
        self.copy_quest_outline_btn.clicked.connect(self._copy_quest_outline)

        return panel

    def _build_stage_lab_panel(self) -> QWidget:
        # --- FIX: Main panel is now just a container for the scroll area ---
        scroll_container = QWidget()
        layout = QVBoxLayout(scroll_container)
        scroll_container.setSizePolicy(QSizePolicy.Preferred, QSizePolicy.Expanding) # Allow vertical expansion
        layout.setContentsMargins(8, 0, 8, 0)
        layout.setSpacing(8)
        scroll_container.setMinimumWidth(340)

        # --- FIX: Create a scrollable area for the stage lab content ---
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setFrameShape(QScrollArea.NoFrame)
        scroll.setWidget(scroll_container)
        # The returned panel will contain just the scroll area
        panel = QWidget()
        panel_layout = QVBoxLayout(panel); panel_layout.setContentsMargins(0,0,0,0); panel_layout.addWidget(scroll)

        heading = QLabel("Stage Studio")
        heading.setStyleSheet("font-size: 15px; font-weight: 600;")
        layout.addWidget(heading)

        self.stage_header_label = QLabel("Pick a stage to begin sculpting its beats.")
        self.stage_header_label.setWordWrap(True)
        self.stage_header_label.setStyleSheet("color: #7a7a7a;")
        layout.addWidget(self.stage_header_label)

        idea_box = QGroupBox("Idea Spark")
        idea_layout = QVBoxLayout(idea_box)
        idea_layout.setSpacing(6)
        idea_intro = QLabel("Use the prompt to riff before you lock the beats. Shuffle whenever inspiration fades.")
        idea_intro.setWordWrap(True)
        idea_intro.setStyleSheet("color: #7a7a7a;")
        idea_layout.addWidget(idea_intro)
        self.prompt_box = QPlainTextEdit()
        self.prompt_box.setReadOnly(True)
        self.prompt_box.setPlaceholderText("Choose a quest to generate creative prompts.")
        self.prompt_box.setLineWrapMode(QPlainTextEdit.WidgetWidth)
        self.prompt_box.setFixedHeight(90)
        idea_layout.addWidget(self.prompt_box)
        prompt_controls = QHBoxLayout()
        self.shuffle_prompt_btn = QPushButton("Shuffle Prompt")
        prompt_controls.addWidget(self.shuffle_prompt_btn)
        prompt_controls.addStretch(1)
        idea_layout.addLayout(prompt_controls)
        layout.addWidget(idea_box)

        self.shuffle_prompt_btn.clicked.connect(lambda: self._refresh_creative_prompt(force=True))

        workflow_box = QGroupBox("Workflow Guide")
        workflow_layout = QVBoxLayout(workflow_box)
        workflow_layout.setSpacing(4)
        workflow_blurb = QLabel(
            "<b>1.</b> Pick or create a quest in the library.<br>"
            "<b>2.</b> Shape its identity in the Quest Canvas.<br>"
            "<b>3.</b> Build stages in the timeline, then dive here.<br>"
            "<b>4.</b> Use Overview → Tasks → Beat Flow to craft the story.<br>"
            "<b>5.</b> Refresh or copy the outline once the scenario sings."
        )
        workflow_blurb.setWordWrap(True)
        workflow_blurb.setTextFormat(Qt.RichText)
        workflow_blurb.setStyleSheet("color: #6a6a6a;")
        workflow_layout.addWidget(workflow_blurb)
        layout.addWidget(workflow_box)

        outline_box = QGroupBox("Stage Outline")
        outline_layout = QVBoxLayout(outline_box)
        outline_layout.setSpacing(6)
        outline_hint = QLabel("Auto-builds a scenario string from notes, tasks, and beats.")
        outline_hint.setWordWrap(True)
        outline_hint.setStyleSheet("color: #7a7a7a;")
        outline_layout.addWidget(outline_hint)
        self.stage_outline_edit = QPlainTextEdit()
        self.stage_outline_edit.setReadOnly(True)
        self.stage_outline_edit.setPlaceholderText("Select a quest and stage to generate the outline.")
        self.stage_outline_edit.setFixedHeight(160)
        outline_layout.addWidget(self.stage_outline_edit)
        outline_controls = QHBoxLayout()
        self.refresh_stage_outline_btn = QPushButton("Refresh Outline")
        self.copy_stage_outline_btn = QPushButton("Copy Outline")
        outline_controls.addWidget(self.refresh_stage_outline_btn)
        outline_controls.addWidget(self.copy_stage_outline_btn)
        outline_controls.addStretch(1)
        outline_layout.addLayout(outline_controls)
        layout.addWidget(outline_box)

        self.refresh_stage_outline_btn.clicked.connect(self._refresh_stage_outline)
        self.copy_stage_outline_btn.clicked.connect(self._copy_stage_outline)

        shortcuts_box = QGroupBox("World & Asset Shortcuts")
        shortcuts_layout = QGridLayout(shortcuts_box)
        shortcuts_layout.setSpacing(6)
        shortcuts_layout.setContentsMargins(6, 6, 6, 6)

        self.goto_quest_editor_btn = QPushButton("Quest Editor")
        self.goto_world_node_btn = QPushButton("Node in World Builder")
        self.goto_world_hub_btn = QPushButton("Hub in World Builder")
        self.goto_giver_npc_btn = QPushButton("Quest Giver (NPC Editor)")

        shortcuts_layout.addWidget(self.goto_quest_editor_btn, 0, 0)
        shortcuts_layout.addWidget(self.goto_world_node_btn, 0, 1)
        shortcuts_layout.addWidget(self.goto_world_hub_btn, 1, 0)
        shortcuts_layout.addWidget(self.goto_giver_npc_btn, 1, 1)

        layout.addWidget(shortcuts_box)

        self.goto_quest_editor_btn.clicked.connect(self._open_quest_in_editor)
        self.goto_world_node_btn.clicked.connect(self._open_node_in_world_editor)
        self.goto_world_hub_btn.clicked.connect(self._open_hub_in_world_editor)
        self.goto_giver_npc_btn.clicked.connect(self._open_giver_in_npc_editor)

        self.stage_tabs = QTabWidget()
        self.stage_tabs.addTab(self._build_stage_overview_tab(), "Stage Overview")
        self.stage_tabs.addTab(self._build_task_board_tab(), "Task Board")
        self.stage_tabs.addTab(self._build_flow_lab_tab(), "Beat Flow")
        layout.addWidget(self.stage_tabs, 1)

        self._update_integration_shortcuts()

        return panel

    def _build_stage_overview_tab(self) -> QWidget:
        widget = QWidget()
        layout = QVBoxLayout(widget)
        layout.setSpacing(8)

        info = QLabel("Name the beat, describe the fantasy, and capture raw notes for future you.")
        info.setWordWrap(True)
        info.setStyleSheet("color: #7a7a7a;")
        layout.addWidget(info)

        form = QFormLayout()
        form.setLabelAlignment(Qt.AlignRight)
        form.setSpacing(4)
        form.setContentsMargins(0, 0, 0, 0)

        self.stage_id_label = QPushButton("-")
        self.stage_id_label.setStyleSheet("text-align: left; border: none; color: blue; text-decoration: underline;")
        self.stage_id_label.clicked.connect(self._on_stage_id_clicked)
        self.stage_title_edit = QLineEdit()
        self.stage_title_edit.setPlaceholderText("Stage name or vibe (e.g., \"Sneak into the Observatory\")")
        self.stage_desc_edit = QTextEdit()
        self.stage_desc_edit.setPlaceholderText("What happens? What's the tension or revelation?")
        self.stage_desc_edit.setFixedHeight(120)
        form.addRow("Stage ID:", self.stage_id_label)
        form.addRow("Title:", self.stage_title_edit)
        form.addRow(QLabel("Description:"), self.stage_desc_edit)
        layout.addLayout(form)

        notes_row = QHBoxLayout()
        self.stage_notes_btn = QPushButton("Open Stage Notes…")
        notes_row.addWidget(self.stage_notes_btn)
        notes_row.addStretch(1)
        layout.addLayout(notes_row)

        self.stage_title_edit.textChanged.connect(self._stage_meta_changed)
        self.stage_desc_edit.textChanged.connect(self._stage_meta_changed)
        self.stage_notes_btn.clicked.connect(self._edit_stage_notes)

        return widget

    def _build_task_board_tab(self) -> QWidget:
        widget = QWidget()
        layout = QVBoxLayout(widget)
        layout.setSpacing(6)

        info = QLabel("Draft the player-facing tasks. Keep them clear, motivational, and flavorful.")
        info.setWordWrap(True)
        info.setStyleSheet("color: #7a7a7a;")
        layout.addWidget(info)

        self.task_table = QTableWidget(0, 4)
        self.task_table.setHorizontalHeaderLabels(["Task ID", "Task Text", "Tutorial", "Auto-start Event"])
        self.task_table.verticalHeader().setVisible(False)
        self.task_table.horizontalHeader().setStretchLastSection(True)
        layout.addWidget(self.task_table, 1)

        self._task_delete_shortcut = QShortcut(QKeySequence(Qt.Key_Delete), self.task_table)
        self._task_delete_shortcut.activated.connect(self._delete_task_row)

        controls = QHBoxLayout()
        self.add_task_btn = QPushButton("Add Task")
        self.dup_task_btn = QPushButton("Duplicate")
        self.del_task_btn = QPushButton("Delete")
        controls.addWidget(self.add_task_btn)
        controls.addWidget(self.dup_task_btn)
        controls.addWidget(self.del_task_btn)
        controls.addStretch(1)
        layout.addLayout(controls)

        self.task_table.cellChanged.connect(self._task_table_changed)
        self.task_table.cellDoubleClicked.connect(self._on_task_cell_double_clicked)
        self.add_task_btn.clicked.connect(self._add_task_row)
        self.dup_task_btn.clicked.connect(self._duplicate_task_row)
        self.del_task_btn.clicked.connect(self._delete_task_row)

        return widget

    def _build_flow_lab_tab(self) -> QWidget:
        widget = QWidget()
        layout = QVBoxLayout(widget)
        layout.setSpacing(6)

        info = QLabel("Assemble dialogue, events, cutscenes, and notes. Reorder beats to tune pacing.")
        info.setWordWrap(True)
        info.setStyleSheet("color: #7a7a7a;")
        layout.addWidget(info)

        self.flow_list = QListWidget()
        self.flow_list.setSelectionMode(QListWidget.SingleSelection)
        self.flow_list.setAlternatingRowColors(True)
        layout.addWidget(self.flow_list, 1)

        self._beat_delete_shortcut = QShortcut(QKeySequence(Qt.Key_Delete), self.flow_list)
        self._beat_delete_shortcut.activated.connect(self._remove_flow_beat)

        btn_row = QHBoxLayout()
        add_beat_btn = QPushButton("Add Beat…")
        add_menu = QMenu(self)
        add_menu.addAction("Dialogue", lambda: self._add_flow_beat("dialogue"))
        add_menu.addAction("Event", lambda: self._add_flow_beat("event"))
        add_menu.addAction("Cutscene", lambda: self._add_flow_beat("cutscene"))
        add_menu.addAction("Note", lambda: self._add_flow_beat("note"))
        add_beat_btn.setMenu(add_menu)

        add_template_btn = QPushButton("Template…")
        template_menu = QMenu(self)
        template_menu.addAction("Event from Template", lambda: self._add_flow_beat_from_template("event"))
        template_menu.addAction("Cutscene from Template", lambda: self._add_flow_beat_from_template("cutscene"))
        add_template_btn.setMenu(template_menu)

        self.ai_flow_btn = QPushButton("AI Draft")
        self.ai_flow_btn.setToolTip("Ask the AI helper to propose beats for this stage.")
        self.edit_flow_btn = QPushButton("Edit")
        self.remove_flow_btn = QPushButton("Delete")
        self.move_flow_up_btn = QPushButton("▲")
        self.move_flow_down_btn = QPushButton("▼")

        btn_row.addWidget(add_beat_btn)
        btn_row.addWidget(add_template_btn)
        btn_row.addWidget(self.ai_flow_btn)
        btn_row.addWidget(self.edit_flow_btn)
        btn_row.addWidget(self.remove_flow_btn)
        btn_row.addStretch(1)
        btn_row.addWidget(self.move_flow_up_btn)
        btn_row.addWidget(self.move_flow_down_btn)
        layout.addLayout(btn_row)

        self.ai_flow_btn.clicked.connect(self._ai_generate_stage_beats)
        self.remove_flow_btn.clicked.connect(self._remove_flow_beat)
        self.edit_flow_btn.clicked.connect(self._edit_flow_beat)
        self.move_flow_up_btn.clicked.connect(lambda: self._move_flow(-1))
        self.move_flow_down_btn.clicked.connect(lambda: self._move_flow(+1))
        self.flow_list.itemDoubleClicked.connect(lambda _: self._edit_flow_beat())
        self.flow_list.itemActivated.connect(lambda _: self._edit_flow_beat())

        return widget

    def _refresh_stage_lab_header(self):
        if not hasattr(self, "stage_header_label"):
            return
        quest = self._find_quest(self.current_quest_id)
        stage = self._current_stage()
        if quest and stage:
            quest_title = quest.get("title", quest.get("id", "Quest"))
            stage_title = stage.get("title", stage.get("id", "Stage"))
            self.stage_header_label.setText(f"{stage_title} — {quest_title}")
        elif quest:
            quest_title = quest.get("title", quest.get("id", "Quest"))
            self.stage_header_label.setText(f"{quest_title}: pick or add a stage to shape it.")
        else:
            self.stage_header_label.setText("Select a quest to begin sculpting stages.")

        if hasattr(self, "stage_tabs"):
            has_quest = quest is not None
            has_stage = stage is not None
            for index in range(self.stage_tabs.count()):
                if index == 0:
                    self.stage_tabs.setTabEnabled(index, has_quest)
                else:
                    self.stage_tabs.setTabEnabled(index, has_quest and has_stage)

    def _refresh_creative_prompt(self, force: bool = False):
        if not hasattr(self, "prompt_box"):
            return
        quest = self._find_quest(self.current_quest_id)
        stage = self._current_stage()
        if not quest:
            self.prompt_box.setPlainText("Choose a quest from the library to start generating creative prompts.")
            return
        if force or not hasattr(self, "_current_prompt") or not self._current_prompt:
            self._current_prompt = random.choice(self.creative_prompts)
        quest_title = quest.get("title", quest.get("id", "your quest"))
        stage_title = stage.get("title", stage.get("id", "this stage")) if stage else "the next stage"
        prompt_text = self._current_prompt.format(quest=quest_title, stage=stage_title)
        self.prompt_box.setPlainText(prompt_text)

    def _refresh_stage_outline(self):
        if not hasattr(self, "stage_outline_edit"):
            return
        quest = self._find_quest(self.current_quest_id)
        stage = self._current_stage()
        if not quest or not stage:
            self.stage_outline_edit.setPlainText("Select a quest and stage to generate the outline.")
            self._update_integration_shortcuts()
            return

        quest_title = quest.get("title", quest.get("id", "Quest"))
        stage_title = stage.get("title", stage.get("id", "Stage"))
        lines: List[str] = [f"{quest_title} — {stage_title}"]
        if stage.get("description"):
            lines.append("")
            lines.append("Synopsis:")
            lines.append(f"  {stage['description'].strip()}")
        if stage.get("notes"):
            lines.append("")
            lines.append("Notes:")
            for note_line in stage["notes"].splitlines():
                lines.append(f"  - {note_line}")

        tasks = stage.get("tasks", [])
        if tasks:
            lines.append("")
            lines.append("Tasks:")
            for task in tasks:
                task_id = task.get("id", "")
                task_text = (task.get("text") or "").strip() or "(Describe the objective)"
                suffix_bits: List[str] = []
                if task.get("tutorial_id"):
                    suffix_bits.append(f"Tutorial: {task['tutorial_id']}")
                if task.get("event_id"):
                    suffix_bits.append(f"Event: {task['event_id']}")
                suffix = f" ({'; '.join(suffix_bits)})" if suffix_bits else ""
                lines.append(f"  - {task_id or '(auto)'}: {task_text}{suffix}")

        flow = self._stage_flow()
        if flow:
            lines.append("")
            lines.append("Beat Flow:")
            for idx, beat in enumerate(flow, start=1):
                beat_type = beat.get("type", "").title() or "Beat"
                beat_id = beat.get("id", "")
                label = beat.get("label") or beat_id
                if beat.get("type") == "note":
                    label = (beat.get("text") or label or "").strip()
                extra = ""
                if beat.get("type") == "dialogue" and beat_id in self.dialogues:
                    speaker = self.dialogues[beat_id].get("speaker")
                    if speaker:
                        extra = f" — {speaker}"
                elif beat.get("type") == "event" and beat_id in self.events:
                    summary = self.events[beat_id].get("description") or self.events[beat_id].get("type", "")
                    if summary:
                        extra = f" — {summary}"
                lines.append(f"  {idx}. [{beat_type}] {label}{extra}")
        else:
            lines.append("")
            lines.append("Beat Flow:")
            lines.append("  (Add dialogue, events, cutscenes, or notes to build the scenario.)")

        self.stage_outline_edit.setPlainText("\n".join(lines))
        self._update_integration_shortcuts()

    def _refresh_quest_outline(self):
        if not hasattr(self, "quest_outline_edit"):
            return
        quest = self._find_quest(self.current_quest_id)
        if not quest:
            self.quest_outline_edit.setPlainText("Select a quest to see its outline.")
            self._update_integration_shortcuts()
            return

        title = quest.get("title", quest.get("id", "Quest"))
        lines: List[str] = [f"Quest: {title} ({quest.get('id', '').strip()})"]
        if quest.get("summary"):
            lines.append("")
            lines.append("Summary:")
            lines.append(f"  {quest['summary'].strip()}")
        if quest.get("description"):
            lines.append("")
            lines.append("Description:")
            lines.append(f"  {quest['description'].strip()}")
        if quest.get("flavor"):
            lines.append("")
            lines.append("Flavor:")
            for flavor_line in quest["flavor"].splitlines():
                lines.append(f"  - {flavor_line}")

        stages = quest.get("stages", [])
        if stages:
            lines.append("")
            lines.append("Stages:")
            flow_map = self.flows.get(quest.get("id", ""), {})
            for idx, stage in enumerate(stages, start=1):
                stage_title = stage.get("title", stage.get("id", f"Stage {idx}"))
                stage_id = stage.get("id", "")
                stage_line = f"  {idx}. {stage_title}"
                if stage_id:
                    stage_line += f" ({stage_id})"
                lines.append(stage_line)
                if stage.get("description"):
                    lines.append(f"     - Synopsis: {stage['description'].strip()}")
                if stage.get("notes"):
                    note_preview = stage["notes"].strip().splitlines()[0]
                    lines.append(f"     - Notes: {note_preview}")
                tasks = stage.get("tasks", [])
                if tasks:
                    lines.append("     - Tasks:")
                    for task in tasks:
                        task_text = (task.get("text") or "").strip()
                        lines.append(f"         • {task.get('id', '')}: {task_text}")
                beats = flow_map.get(stage_id, [])
                if beats:
                    lines.append("     - Beats:")
                    for b_idx, beat in enumerate(beats, start=1):
                        label = beat.get("label") or beat.get("id") or ""
                        if beat.get("type") == "note":
                            label = (beat.get("text") or label or "").strip()
                        lines.append(f"         • {b_idx}. [{beat.get('type', '').title()}] {label}")
        else:
            lines.append("")
            lines.append("Stages:")
            lines.append("  (No stages yet — add one to begin the journey.)")

        self.quest_outline_edit.setPlainText("\n".join(lines))
        self._update_integration_shortcuts()

    def _copy_stage_outline(self):
        if not hasattr(self, "stage_outline_edit"):
            return
        text = (self.stage_outline_edit.toPlainText() or "").strip()
        if not text:
            return
        QApplication.clipboard().setText(text)
        if hasattr(self, "status_hint_label"):
            self.status_hint_label.setText("Stage outline copied to clipboard.")

    def _copy_quest_outline(self):
        if not hasattr(self, "quest_outline_edit"):
            return
        text = (self.quest_outline_edit.toPlainText() or "").strip()
        if not text:
            return
        QApplication.clipboard().setText(text)
        if hasattr(self, "status_hint_label"):
            self.status_hint_label.setText("Quest outline copied to clipboard.")

    def _update_integration_shortcuts(self):
        if not hasattr(self, "goto_quest_editor_btn"):
            return
        quest = self._find_quest(self.current_quest_id)
        quest_id = (quest.get("id") if quest else "") or ""
        node_id = (quest.get("node_id") if quest else "") or ""
        hub_id = (quest.get("hub_id") if quest else "") or ""
        giver_id = (quest.get("giver") if quest else "") or ""

        self.goto_quest_editor_btn.setEnabled(bool(quest_id))
        self.goto_quest_editor_btn.setToolTip(
            f"Open quest '{quest_id}' in the dedicated quest editor." if quest_id else "Set a quest to enable this shortcut."
        )

        self.goto_world_node_btn.setEnabled(bool(node_id))
        self.goto_world_node_btn.setToolTip(
            f"Open node '{node_id}' in the world builder." if node_id else "Assign a Node ID on the Quest Canvas to enable."
        )

        self.goto_world_hub_btn.setEnabled(bool(hub_id))
        self.goto_world_hub_btn.setToolTip(
            f"Open hub '{hub_id}' in the world builder." if hub_id else "Assign a Hub ID on the Quest Canvas to enable."
        )

        self.goto_giver_npc_btn.setEnabled(bool(giver_id))
        self.goto_giver_npc_btn.setToolTip(
            f"Open NPC '{giver_id}' in the NPC editor." if giver_id else "Set a quest giver to enable this shortcut."
        )

    def _open_quest_in_editor(self):
        quest = self._find_quest(self.current_quest_id)
        quest_id = quest.get("id") if quest else ""
        if quest_id:
            studio_goto("quest", quest_id)

    def _open_node_in_world_editor(self):
        quest = self._find_quest(self.current_quest_id)
        node_id = quest.get("node_id") if quest else ""
        if node_id:
            studio_goto("world", node_id)

    def _open_hub_in_world_editor(self):
        quest = self._find_quest(self.current_quest_id)
        hub_id = quest.get("hub_id") if quest else ""
        if hub_id:
            studio_goto("world", hub_id)

    def _open_giver_in_npc_editor(self):
        quest = self._find_quest(self.current_quest_id)
        giver_id = quest.get("giver") if quest else ""
        if giver_id:
            studio_goto("npc", giver_id)

    def _on_task_cell_double_clicked(self, row: int, column: int):
        stage = self._current_stage()
        if not stage:
            return
        tasks = stage.get("tasks", [])
        if not (0 <= row < len(tasks)):
            return

        task = tasks[row]
        if column == 2:  # Tutorial
            tutorial_id = task.get("tutorial_id")
            if tutorial_id:
                studio_goto("tutorial", tutorial_id)
        elif column == 3:  # Auto-start Event
            event_id = task.get("event_id")
            if event_id:
                studio_goto("event", event_id)

    def _on_stage_id_clicked(self):
        stage = self._current_stage()
        if stage:
            stage_id = stage.get("id")
            if stage_id:
                studio_goto("stage", stage_id)

    # ---------------------- quest helpers ---------------------
    def _refresh_quest_list(self):
        if self._updating_ui:
            return
        self._updating_ui = True
        search = (self.quest_search.text() or "").strip().lower()
        self.quest_list.clear()
        for quest in self.quests:
            title = quest.get("title", quest.get("id", ""))
            if search and search not in (title or "").lower() and search not in quest.get("id", "").lower():
                continue
            item = QListWidgetItem(f"{quest.get('title', quest.get('id', ''))}")
            item.setData(Qt.UserRole, quest.get("id"))
            self.quest_list.addItem(item)
            if quest.get("id") == self.current_quest_id:
                self.quest_list.setCurrentItem(item)
        self._updating_ui = False

    def _find_quest(self, quest_id: Optional[str]) -> Optional[dict]:
        if not quest_id:
            return None
        for quest in self.quests:
            if quest.get("id") == quest_id:
                return quest
        return None

    def _on_quest_selected(self, current: Optional[QListWidgetItem]):
        if self._updating_ui:
            return
        quest_id = current.data(Qt.UserRole) if current else None
        self.current_quest_id = quest_id
        self.current_stage_index = None
        self._refresh_stage_list()
        self._populate_quest_fields()

    def _populate_quest_fields(self):
        quest = self._find_quest(self.current_quest_id)
        self._updating_ui = True
        if not quest:
            self.header_label.setText("Select a quest…")
            self.quest_id_label.setText("-")
            self.quest_title_edit.setText("")
            self.quest_summary_edit.setPlainText("")
            self.quest_desc_edit.setPlainText("")
            self.quest_flavor_edit.setPlainText("")
            self.quest_giver_edit.setCurrentIndex(-1)
            self.quest_hub_edit.setCurrentIndex(-1)
            self.quest_node_edit.setCurrentIndex(-1)
            self.quest_mood_edit.setText("")
            self.quest_type_combo.setCurrentIndex(0)
            self.quest_status_combo.setCurrentIndex(0)
            self.quest_required_item_edit.setText("")
            self.quest_start_dialogue_edit.setText("")
            self.quest_completion_dialogue_edit.setText("")
            self.quest_rewards_edit.setText("")
            self.quest_autostart_checkbox.setChecked(False)
            self.stage_list.clear()
            self._updating_ui = False
            self._update_window_title()
            self._refresh_stage_lab_header()
            self._refresh_creative_prompt()
            self._refresh_quest_outline()
            self._refresh_stage_outline()
            return
        self.header_label.setText(f"{quest.get('title', quest.get('id', 'Quest'))}")
        self.quest_id_label.setText(quest.get("id", "-"))
        self.quest_title_edit.setText(quest.get("title", ""))
        self.quest_summary_edit.setPlainText(quest.get("summary", ""))
        self.quest_desc_edit.setPlainText(quest.get("description", ""))
        self.quest_flavor_edit.setPlainText(quest.get("flavor", ""))
        self.quest_giver_edit.setCurrentText(quest.get("giver", ""))
        self.quest_hub_edit.setCurrentText(quest.get("hub_id", ""))
        self.quest_node_edit.setCurrentText(quest.get("node_id", ""))
        self.quest_mood_edit.setText(quest.get("mood", ""))
        self.quest_type_combo.setCurrentText(quest.get("type", ""))
        self.quest_status_combo.setCurrentText(quest.get("status", "inactive"))
        self.quest_required_item_edit.setText(quest.get("required_item", ""))
        self.quest_start_dialogue_edit.setText(quest.get("start_dialogue", ""))
        self.quest_completion_dialogue_edit.setText(quest.get("completion_dialogue", ""))

        # --- FIX: Handle complex reward structures ---
        rewards = quest.get("rewards", [])
        reward_parts = []
        for r in rewards:
            if isinstance(r, dict):
                # Format as "item_id(quantity)" for readability
                reward_parts.append(f"{r.get('item_id', '?')}({r.get('quantity', 1)})")
            elif isinstance(r, str):
                reward_parts.append(r)
        self.quest_rewards_edit.setText(", ".join(reward_parts))
        # --- END FIX ---

        self.quest_autostart_checkbox.setChecked(quest.get("autostart", False))
        self._updating_ui = False
        self._update_window_title()
        self._refresh_stage_lab_header()
        self._refresh_creative_prompt()
        self._refresh_quest_outline()
        self._refresh_stage_outline()

    def _quest_meta_changed(self):
        if self._updating_ui:
            return
        quest = self._find_quest(self.current_quest_id)
        if not quest:
            return
        quest["title"] = self.quest_title_edit.text().strip()
        quest["summary"] = self.quest_summary_edit.toPlainText().strip()
        quest["description"] = self.quest_desc_edit.toPlainText().strip()
        quest["flavor"] = self.quest_flavor_edit.toPlainText().strip()
        quest["giver"] = self.quest_giver_edit.currentText()
        quest["hub_id"] = self.quest_hub_edit.currentText()
        quest["node_id"] = self.quest_node_edit.currentText()
        quest["mood"] = self.quest_mood_edit.text().strip()
        quest["type"] = self.quest_type_combo.currentText()
        quest["status"] = self.quest_status_combo.currentText()
        quest["required_item"] = self.quest_required_item_edit.text().strip()
        quest["start_dialogue"] = self.quest_start_dialogue_edit.text().strip()
        quest["completion_dialogue"] = self.quest_completion_dialogue_edit.text().strip()

        # --- FIX: Parse complex reward structures ---
        rewards_text = self.quest_rewards_edit.text().strip()
        rewards = []
        if rewards_text:
            for part in rewards_text.split(","):
                part = part.strip()
                match = re.match(r"(.+)\((\d+)\)", part)
                if match:
                    rewards.append({"item_id": match.group(1), "quantity": int(match.group(2))})
                elif part:
                    rewards.append(part)
        quest["rewards"] = rewards
        # --- END FIX ---

        quest["autostart"] = self.quest_autostart_checkbox.isChecked()
        self._mark_dirty()
        self._refresh_quest_list()
        self._refresh_quest_outline()

    # ------------- quest CRUD --------------
    def _add_quest(self):
        new_id, ok = QInputDialog.getText(self, "New Quest", "Quest ID:", text="quest_new")
        if not ok or not new_id.strip():
            return
        new_id = new_id.strip()
        if any(q.get("id") == new_id for q in self.quests):
            QMessageBox.warning(self, "Duplicate", f"Quest '{new_id}' already exists.")
            return
        quest = {
            "id": new_id,
            "title": new_id.replace("_", " ").title(),
            "summary": "",
            "description": "",
            "flavor": "",
            "giver": "",
            "hub_id": "",
            "node_id": "",
            "mood": "",
            "rewards": [],
            "autostart": False,
        }
        self.quests.append(quest)
        self.current_quest_id = new_id
        self.current_stage_index = None
        self.flows.setdefault(new_id, {})
        self._mark_dirty()
        self._refresh_quest_list()
        self._refresh_stage_list()
        self._populate_quest_fields()
        self._refresh_quest_outline()
        self._refresh_stage_outline()

    def _add_quest_from_template(self):
        template_path, _ = QFileDialog.getOpenFileName(
            self, "Select Quest Template", str(self.project_root / "templates"), "Jinja2 Templates (*.json.j2 *.json)"
        )
        if not template_path:
            return

        try:
            with open(template_path, "r", encoding="utf-8") as f:
                template_content = f.read()
        except Exception as e:
            QMessageBox.critical(self, "Template Error", f"Failed to load template:\n{e}")
            return

        # Find all variables in the template
        variables = sorted(list(set(re.findall(r"\{\{\s*(\w+)\s*\}\}", template_content))))

        template_values = {}
        if variables:
            var_dialog = TemplateVariablesDialog(self, variables)
            if var_dialog.exec_() == QDialog.Accepted:
                template_values = var_dialog.get_values()
            else:
                return # User cancelled

        # Render the template
        try:
            jinja_env = Environment(loader=FileSystemLoader(os.path.dirname(template_path)))
            template = jinja_env.from_string(template_content)
            rendered_json = template.render(template_values)
            new_quest = json.loads(rendered_json)
        except Exception as e:
            QMessageBox.critical(self, "Template Error", f"Failed to render template or parse JSON:\n{e}")
            return

        # Get a unique ID for the new quest
        new_id_suggestion = new_quest.get("id", "new_quest")
        new_id, ok = QInputDialog.getText(self, "New Quest ID", "Enter new quest ID:", text=new_id_suggestion)
        if not ok or not new_id.strip():
            return
        new_id = new_id.strip()

        if any(q.get("id") == new_id for q in self.quests):
            QMessageBox.warning(self, "ID Error", "Quest ID already exists.")
            return

        new_quest["id"] = new_id

        self.quests.append(new_quest)
        self.current_quest_id = new_id
        self.current_stage_index = None
        self._mark_dirty()
        self._refresh_quest_list()
        self._populate_quest_fields()

    def _duplicate_quest(self):
        quest = self._find_quest(self.current_quest_id)
        if not quest:
            return
        clone = json.loads(json.dumps(quest))
        clone_id = unique_id(f"{quest['id']}_copy", [q["id"] for q in self.quests])
        clone["id"] = clone_id
        clone["title"] = f"{quest.get('title', quest['id'])} (Copy)"
        self.quests.append(clone)
        self.current_quest_id = clone_id
        self.current_stage_index = None
        self._mark_dirty()
        self._refresh_quest_list()
        self._populate_quest_fields()

    def _delete_quest(self):
        quest = self._find_quest(self.current_quest_id)
        if not quest:
            return
        if QMessageBox.question(self, "Delete quest", f"Delete quest '{quest.get('title', quest.get('id'))}'?") != QMessageBox.Yes:
            return
        self.quests = [q for q in self.quests if q.get("id") != quest.get("id")]
        self.flows.pop(quest["id"], None)
        if self.current_quest_id in self.flows:
            self.flows.pop(self.current_quest_id, None)
        self.current_quest_id = None
        self.current_stage_index = None
        self._mark_dirty()
        self._refresh_quest_list()
        self._populate_quest_fields()

    def _create_conditional_quest(self):
        wizard = ConditionalQuestWizard(self, self.room_ids)
        if wizard.exec_() != QDialog.Accepted:
            return

        values = wizard.get_values()
        template_path = self.project_root / "templates" / "pattern_conditional_quest.json.j2"

        try:
            with open(template_path, "r", encoding="utf-8") as f:
                template_content = f.read()
        except FileNotFoundError:
            QMessageBox.critical(self, "Template Error", f"Pattern template not found at {template_path}")
            return
        except Exception as e:
            QMessageBox.critical(self, "Template Error", f"Failed to load pattern template:\n{e}")
            return

        try:
            jinja_env = Environment()
            template = jinja_env.from_string(template_content)
            rendered_json = template.render(values)
            data = json.loads(rendered_json)
        except Exception as e:
            QMessageBox.critical(self, "Template Error", f"Failed to render pattern or parse JSON:\n{e}")
            return

        # Append the new data to the in-memory stores
        event_payload = data["event"]
        event_id = event_payload.get("id")
        if event_id:
            self.events[event_id] = event_payload
        cinematic_payload = data["cinematic"]
        self.cinematics[cinematic_payload["id"]] = cinematic_payload["steps"]
        for dialogue_entry in data["dialogues"]:
            dlg_id = dialogue_entry.get("id")
            if dlg_id:
                self.dialogues[dlg_id] = dialogue_entry

        self._mark_dirty()
        QMessageBox.information(self, "Success", f"Successfully created conditional quest for {values['npc_name']}.\nRemember to save your changes.")

    def _create_fetch_quest(self):
        items = _read_json_list(self.project_root / "items.json")
        item_ids = sorted([item.get("id", "") for item in items if item.get("id")])
        wizard = FetchQuestWizard(self, item_ids, self.npc_ids)
        if wizard.exec_() != QDialog.Accepted:
            return

        values = wizard.get_values()
        template_path = self.project_root / "templates" / "pattern_fetch_quest.json.j2"

        try:
            with open(template_path, "r", encoding="utf-8") as f:
                template_content = f.read()
        except FileNotFoundError:
            QMessageBox.critical(self, "Template Error", f"Pattern template not found at {template_path}")
            return
        except Exception as e:
            QMessageBox.critical(self, "Template Error", f"Failed to load pattern template:\n{e}")
            return

        try:
            jinja_env = Environment()
            template = jinja_env.from_string(template_content)
            rendered_json = template.render(values)
            data = json.loads(rendered_json)
        except Exception as e:
            QMessageBox.critical(self, "Template Error", f"Failed to render pattern or parse JSON:\n{e}")
            return

        # Append the new data to the in-memory stores
        quest_payload = data["quest"]
        quest_id = quest_payload.get("id")
        if quest_id:
            self.quests.append(quest_payload)
        for dialogue_entry in data["dialogues"]:
            dlg_id = dialogue_entry.get("id")
            if dlg_id:
                self.dialogues[dlg_id] = dialogue_entry
        for event_entry in data["events"]:
            evt_id = event_entry.get("id")
            if evt_id:
                self.events[evt_id] = event_entry

        self._mark_dirty()
        self._refresh_quest_list()
        QMessageBox.information(self, "Success", f"Successfully created fetch quest '{quest_id}'.\nRemember to save your changes.")

    def _create_kill_quest(self):
        wizard = KillQuestWizard(self, self.npc_ids, self.room_ids)
        if wizard.exec_() != QDialog.Accepted:
            return

        values = wizard.get_values()
        template_path = self.project_root / "templates" / "pattern_kill_quest.json.j2"

        try:
            with open(template_path, "r", encoding="utf-8") as f:
                template_content = f.read()
        except FileNotFoundError:
            QMessageBox.critical(self, "Template Error", f"Pattern template not found at {template_path}")
            return
        except Exception as e:
            QMessageBox.critical(self, "Template Error", f"Failed to load pattern template:\n{e}")
            return

        try:
            jinja_env = Environment()
            template = jinja_env.from_string(template_content)
            rendered_json = template.render(values)
            data = json.loads(rendered_json)
        except Exception as e:
            QMessageBox.critical(self, "Template Error", f"Failed to render pattern or parse JSON:\n{e}")
            return

        # Append the new data to the in-memory stores
        quest_payload = data["quest"]
        quest_id = quest_payload.get("id")
        if quest_id:
            self.quests.append(quest_payload)
        for dialogue_entry in data["dialogues"]:
            dlg_id = dialogue_entry.get("id")
            if dlg_id:
                self.dialogues[dlg_id] = dialogue_entry
        for event_entry in data["events"]:
            evt_id = event_entry.get("id")
            if evt_id:
                self.events[evt_id] = event_entry

        self._mark_dirty()
        self._refresh_quest_list()
        QMessageBox.information(self, "Success", f"Successfully created kill quest '{quest_id}'.\nRemember to save your changes.")

    def _create_quest_completion_event(self):
        quest_ids = [q.get("id", "") for q in self.quests if q.get("id")]
        items = _read_json_list(self.project_root / "items.json")
        item_ids = sorted([item.get("id", "") for item in items if item.get("id")])
        wizard = QuestCompletionWizard(self, quest_ids, self.npc_ids, self.room_ids, item_ids)
        if wizard.exec_() != QDialog.Accepted:
            return

        values = wizard.get_values()
        quest_id = values["quest_id"]

        new_event = {
            "id": f"evt_complete_{quest_id}",
            "description": f"Event to complete the quest '{quest_id}'.",
            "trigger": {
                "type": values["trigger_type"],
                values["trigger_key"]: values["trigger_value"]
            },
            "actions": [
                {
                    "type": "complete_quest",
                    "quest_id": quest_id
                }
            ],
            "conditions": [
                {
                    "type": "quest_active",
                    "quest_id": quest_id
                }
            ]
        }

        if values["dialogue_id"]:
            new_event["actions"].insert(0, {
                "type": "play_dialogue",
                "dialogue_id": values["dialogue_id"]
            })

        if values["item_reward"]:
            new_event["actions"].append({
                "type": "add_item",
                "item_id": values["item_reward"],
                "quantity": 1
            })

        self.events[new_event["id"]] = new_event
        self._mark_dirty()
        QMessageBox.information(self, "Success", f"Successfully created completion event for quest '{quest_id}'.\nRemember to save your changes.")

    # -------------------- stage helpers ----------------------
    def _refresh_stage_list(self):
        self.stage_list.clear()
        quest = self._find_quest(self.current_quest_id)
        if not quest:
            if hasattr(self, "status_hint_label"):
                self.status_hint_label.setText("Tip: double-click a beat to open its micro editor.")
            self._refresh_stage_outline()
            self._refresh_quest_outline()
            return
        stages = quest.get("stages", [])
        if hasattr(self, "status_hint_label"):
            if stages:
                self.status_hint_label.setText(f"{len(stages)} stage{'s' if len(stages) != 1 else ''} in this quest.")
            else:
                self.status_hint_label.setText("No stages yet — sketch your opening beat!")
        search = ""
        if hasattr(self, "stage_search") and self.stage_search:
            search = (self.stage_search.text() or "").strip().lower()
        selected_idx = self.current_stage_index
        selected_item: Optional[QListWidgetItem] = None

        for idx, stage in enumerate(stages):
            tasks = stage.get("tasks", [])
            haystack_parts = [
                stage.get("title", ""),
                stage.get("id", ""),
                stage.get("description", ""),
                stage.get("notes", ""),
            ]
            for task in tasks:
                haystack_parts.extend([
                    task.get("id", ""),
                    task.get("text", ""),
                    task.get("tutorial_id", ""),
                    task.get("event_id", ""),
                ])
            haystack = " ".join(str(part) for part in haystack_parts).lower()
            if search and search not in haystack:
                continue

            title = stage.get("title") or f"Stage {idx+1}"
            stage_id = stage.get("id", "")
            task_count = len(tasks)
            notes_text = (stage.get("notes") or "").strip()
            summary_line = ""
            if stage.get("description"):
                summary_line = stage["description"].strip().splitlines()[0]

            meta_bits: List[str] = []
            if stage_id:
                meta_bits.append(stage_id)
            if task_count:
                meta_bits.append(f"{task_count} task{'s' if task_count != 1 else ''}")
            if notes_text:
                meta_bits.append("notes")

            subtitle_bits = list(meta_bits)
            if summary_line:
                subtitle_bits.append(summary_line if len(summary_line) <= 80 else summary_line[:77] + "…")

            text = f"{idx+1:02d} — {title}"
            if subtitle_bits:
                text += "\n" + " • ".join(subtitle_bits)

            item = QListWidgetItem(text)
            item.setData(Qt.UserRole, idx)
            item.setSizeHint(QSize(0, 64 if subtitle_bits else 46))
            item.setTextAlignment(Qt.AlignLeft | Qt.AlignVCenter)

            tooltip_lines = [
                f"Stage ID: {stage_id or '(auto)'}",
                f"Title: {title or '(untitled)'}",
            ]
            if task_count:
                tooltip_lines.append(f"Tasks: {task_count}")
            if summary_line:
                tooltip_lines.append(f"Summary: {summary_line}")
            if notes_text:
                note_preview = notes_text.splitlines()[0]
                tooltip_lines.append(
                    f"Notes: {note_preview[:120] + '…' if len(note_preview) > 120 else note_preview}"
                )
            if tasks:
                task_preview = [t.get("text", "").strip() for t in tasks if t.get("text")]
                if task_preview:
                    joined_preview = ", ".join(task_preview)
                    tooltip_lines.append(
                        f"First tasks: {joined_preview[:120] + '…' if len(joined_preview) > 120 else joined_preview}"
                    )
            item.setToolTip("\n".join(tooltip_lines))
            if not tasks:
                item.setForeground(Qt.gray)
            if notes_text:
                font = item.font()
                font.setItalic(True)
                item.setFont(font)
            self.stage_list.addItem(item)
            if selected_idx is not None and idx == selected_idx:
                selected_item = item

        if self.stage_list.count() == 0:
            if self.current_stage_index is not None:
                self.current_stage_index = None
                self._populate_stage_fields()
                self._populate_flow_list()
            else:
                self._refresh_stage_outline()
            self._refresh_quest_outline()
            return

        if selected_item is not None:
            self.stage_list.setCurrentItem(selected_item)
        else:
            self.stage_list.setCurrentRow(0)
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    def _current_stage(self) -> Optional[dict]:
        quest = self._find_quest(self.current_quest_id)
        if not quest:
            return None
        stages = quest.setdefault("stages", [])
        if self.current_stage_index is None:
            return None
        if 0 <= self.current_stage_index < len(stages):
            return stages[self.current_stage_index]
        return None

    def _on_stage_selected(self, current: Optional[QListWidgetItem]):
        if self._updating_ui:
            return
        if current is None:
            self.current_stage_index = None
        else:
            self.current_stage_index = int(current.data(Qt.UserRole))
        self._populate_stage_fields()
        self._populate_flow_list()

    def _populate_stage_fields(self):
        stage = self._current_stage()
        self._updating_ui = True
        if not stage:
            self.stage_id_label.setText("-")
            self.stage_title_edit.setText("")
            self.stage_desc_edit.setPlainText("")
            self.task_table.setRowCount(0)
            self._updating_ui = False
            self._refresh_stage_lab_header()
            self._refresh_creative_prompt()
            if hasattr(self, "status_hint_label"):
                self.status_hint_label.setText("No stage selected — pick one from the timeline to start shaping beats.")
            self._refresh_stage_outline()
            self._refresh_quest_outline()
            return
        self.stage_id_label.setText(stage.get("id", f"s{self.current_stage_index or 0}"))
        self.stage_title_edit.setText(stage.get("title", ""))
        self.stage_desc_edit.setPlainText(stage.get("description", ""))

        tasks = stage.get("tasks", [])
        self.task_table.blockSignals(True)
        self.task_table.setRowCount(0)
        for task in tasks:
            row = self.task_table.rowCount()
            self.task_table.insertRow(row)
            self.task_table.setItem(row, 0, QTableWidgetItem(task.get("id", "")))
            self.task_table.setItem(row, 1, QTableWidgetItem(task.get("text", "")))
            self.task_table.setItem(row, 2, QTableWidgetItem(task.get("tutorial_id", "")))
            self.task_table.setItem(row, 3, QTableWidgetItem(task.get("event_id", "")))
        self.task_table.blockSignals(False)
        self._updating_ui = False
        self._refresh_stage_lab_header()
        self._refresh_creative_prompt()
        if hasattr(self, "status_hint_label"):
            idx = (self.current_stage_index or 0) + 1
            self.status_hint_label.setText(f"Stage {idx} in focus — craft tasks and beats on the right.")
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    def _stage_meta_changed(self):
        if self._updating_ui:
            return
        stage = self._current_stage()
        if not stage:
            return
        stage["title"] = self.stage_title_edit.text().strip()
        stage["description"] = self.stage_desc_edit.toPlainText().strip()
        self._mark_dirty()
        self._refresh_stage_list()
        self._refresh_stage_lab_header()
        self._refresh_creative_prompt()
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    def _task_table_changed(self, row: int, column: int):
        if self._updating_ui:
            return
        stage = self._current_stage()
        if not stage:
            return
        tasks = stage.setdefault("tasks", [])
        while len(tasks) <= row:
            tasks.append({"id": f"task_{row+1}", "text": ""})
        get_text = lambda c: (self.task_table.item(row, c).text().strip() if self.task_table.item(row, c) else "")
        tasks[row]["id"] = get_text(0) or f"task_{row+1}"
        tasks[row]["text"] = get_text(1)
        if get_text(2):
            tasks[row]["tutorial_id"] = get_text(2)
        else:
            tasks[row].pop("tutorial_id", None)
        if get_text(3):
            tasks[row]["event_id"] = get_text(3)
        else:
            tasks[row].pop("event_id", None)
        self._mark_dirty()
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    def _add_task_row(self):
        stage = self._current_stage()
        if not stage:
            return
        row = self.task_table.rowCount()
        self.task_table.insertRow(row)
        new_id = unique_id("task", [self.task_table.item(r, 0).text() if self.task_table.item(r, 0) else "" for r in range(row)])
        self.task_table.setItem(row, 0, QTableWidgetItem(new_id))
        self.task_table.setItem(row, 1, QTableWidgetItem("Describe the objective…"))
        self.task_table.setItem(row, 2, QTableWidgetItem(""))
        self.task_table.setItem(row, 3, QTableWidgetItem(""))
        stage.setdefault("tasks", []).append({"id": new_id, "text": "Describe the objective…"})
        self._mark_dirty()
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    def _duplicate_task_row(self):
        stage = self._current_stage()
        if not stage:
            return
        row = self.task_table.currentRow()
        if row < 0:
            return
        base_id = self.task_table.item(row, 0).text() if self.task_table.item(row, 0) else "task"
        new_id = unique_id(base_id, [self.task_table.item(r, 0).text() if self.task_table.item(r, 0) else "" for r in range(self.task_table.rowCount())])
        new_row = self.task_table.rowCount()
        self.task_table.insertRow(new_row)
        for col in range(4):
            text = self.task_table.item(row, col).text() if self.task_table.item(row, col) else ""
            if col == 0:
                text = new_id
            self.task_table.setItem(new_row, col, QTableWidgetItem(text))
        tasks = stage.setdefault("tasks", [])
        tasks.insert(new_row, {
            "id": new_id,
            "text": self.task_table.item(row, 1).text() if self.task_table.item(row, 1) else "",
            "tutorial_id": self.task_table.item(row, 2).text() if self.task_table.item(row, 2) else "",
            "event_id": self.task_table.item(row, 3).text() if self.task_table.item(row, 3) else "",
        })
        self._mark_dirty()
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    def _delete_task_row(self):
        stage = self._current_stage()
        if not stage:
            return
        row = self.task_table.currentRow()
        if row < 0:
            return
        self.task_table.removeRow(row)
        tasks = stage.setdefault("tasks", [])
        if 0 <= row < len(tasks):
            tasks.pop(row)
        self._mark_dirty()
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    def _edit_stage_notes(self):
        stage = self._current_stage()
        if not stage:
            return
        notes = stage.get("notes", "")
        flavor = stage.get("flavor", "")
        dlg = StageNotesDialog(self, notes, flavor)
        if dlg.exec_() == QDialog.Accepted:
            text, flavor_text = dlg.value()
            if text.strip():
                stage["notes"] = text
            else:
                stage.pop("notes", None)
            if flavor_text.strip():
                stage["flavor"] = flavor_text
            else:
                stage.pop("flavor", None)
            self._mark_dirty()
            self._refresh_stage_list()
            self._refresh_stage_outline()
            self._refresh_quest_outline()

    def _add_stage(self):
        quest = self._find_quest(self.current_quest_id)
        if not quest:
            return
        stages = quest.setdefault("stages", [])
        new_idx = len(stages)
        stage_id = unique_id("stage", [s.get("id", f"s{i}") for i, s in enumerate(stages)])
        stage = {
            "id": stage_id,
            "title": f"Stage {new_idx + 1}",
            "description": "",
            "tasks": [],
        }
        stages.append(stage)
        self.current_stage_index = new_idx
        self._mark_dirty()
        self._refresh_stage_list()
        self.stage_list.setCurrentRow(new_idx)
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    def _duplicate_stage(self):
        quest = self._find_quest(self.current_quest_id)
        if not quest:
            return
        if self.current_stage_index is None:
            return
        stages = quest.setdefault("stages", [])
        if not (0 <= self.current_stage_index < len(stages)):
            return
        clone_source = stages[self.current_stage_index]
        clone = json.loads(json.dumps(clone_source))
        source_id = clone_source.get("id", f"stage{self.current_stage_index}")
        existing_ids = [s.get("id", "") for s in stages]
        clone_id = unique_id(source_id, existing_ids)
        clone["id"] = clone_id
        stages.insert(self.current_stage_index + 1, clone)
        self.current_stage_index += 1
        flow_map = self.flows.setdefault(quest["id"], {})
        stage_key = clone_id
        flow_map[stage_key] = json.loads(json.dumps(flow_map.get(source_id, [])))
        self._mark_dirty()
        self._refresh_stage_list()
        self.stage_list.setCurrentRow(self.current_stage_index)
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    def _delete_stage(self):
        quest = self._find_quest(self.current_quest_id)
        if not quest:
            return
        if self.current_stage_index is None:
            return
        stages = quest.setdefault("stages", [])
        if not stages:
            return
        stage = stages[self.current_stage_index]
        if QMessageBox.question(self, "Delete stage", f"Delete stage '{stage.get('title', stage.get('id'))}'?") != QMessageBox.Yes:
            return
        stage_id = stage.get("id")
        stages.pop(self.current_stage_index)
        if stage_id and quest["id"] in self.flows:
            self.flows[quest["id"]].pop(stage_id, None)
        if self.current_stage_index >= len(stages):
            self.current_stage_index = len(stages) - 1 if stages else None
        self._mark_dirty()
        self._refresh_stage_list()
        if self.current_stage_index is not None:
            self.stage_list.setCurrentRow(self.current_stage_index)
        else:
            self._populate_stage_fields()
            self._populate_flow_list()
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    def _move_stage(self, delta: int):
        quest = self._find_quest(self.current_quest_id)
        if not quest or self.current_stage_index is None:
            return
        stages = quest.setdefault("stages", [])
        idx = self.current_stage_index
        new_idx = idx + delta
        if not (0 <= new_idx < len(stages)):
            return
        stages[idx], stages[new_idx] = stages[new_idx], stages[idx]
        self.current_stage_index = new_idx
        self._mark_dirty()
        self._refresh_stage_list()
        self.stage_list.setCurrentRow(new_idx)
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    # ---------------------- flow beats ------------------------
    def _current_flow_key(self) -> Optional[Tuple[str, str]]:
        quest = self._find_quest(self.current_quest_id)
        stage = self._current_stage()
        if not quest or not stage:
            return None
        return quest["id"], stage.get("id", "")

    def _stage_flow(self) -> List[dict]:
        key = self._current_flow_key()
        if not key:
            return []
        quest_id, stage_id = key
        quest_flow = self.flows.setdefault(quest_id, {})
        return quest_flow.setdefault(stage_id, [])

    def _populate_flow_list(self):
        self.flow_list.clear()
        key = self._current_flow_key()
        if not key:
            self._refresh_stage_outline()
            self._refresh_quest_outline()
            return
        data = self._stage_flow()
        for beat in data:
            beat_type = (beat.get("type") or "").strip() or "note"
            ref = beat.get("id", "")
            label = beat.get("label") or ref or "(untitled)"
            subtitle = ""
            missing = False

            if beat_type == "dialogue":
                dialogue = self.dialogues.get(ref)
                if dialogue:
                    speaker = (dialogue.get("speaker") or "").strip()
                    if speaker:
                        subtitle = speaker
                else:
                    missing = True
            elif beat_type == "event":
                missing = ref not in self.events
                if not missing:
                    action = self.events[ref].get("actions", [])
                    if action:
                        subtitle = action[0].get("type", "")
            elif beat_type == "cutscene":
                missing = ref not in self.cinematics
            elif beat_type == "note":
                note_text = (beat.get("text") or "").strip()
                if note_text and note_text != label:
                    subtitle = note_text.splitlines()[0]

            display = f"[{beat_type}] {label}"
            if subtitle and subtitle.lower() != (label or "").lower():
                display += f" — {subtitle}"

            item = QListWidgetItem(display)
            item.setData(Qt.UserRole, beat)

            tooltip_lines = [
                f"Type: {beat_type}",
                f"ID: {ref or '(new)'}",
            ]
            if subtitle:
                tooltip_lines.append(f"Detail: {subtitle}")
            if missing:
                tooltip_lines.append("Status: missing reference")
                item.setForeground(Qt.red)
            if beat_type == "note":
                font = item.font()
                font.setItalic(True)
                item.setFont(font)
            item.setToolTip("\n".join(tooltip_lines))
            self.flow_list.addItem(item)
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    def _ai_generate_stage_beats(self):
        quest = self._find_quest(self.current_quest_id)
        stage = self._current_stage()
        if not quest or not stage:
            QMessageBox.information(
                self,
                "AI Stage Draft",
                "Select a quest and stage before asking the assistant.",
            )
            return

        default_prompt = ""
        if hasattr(self, "prompt_box") and self.prompt_box:
            default_prompt = (self.prompt_box.toPlainText() or "").strip()
        if not default_prompt:
            default_prompt = (stage.get("description") or "").strip()
        if not default_prompt:
            quest_title = quest.get("title", quest.get("id", "the quest"))
            stage_title = stage.get("title", stage.get("id", "this stage"))
            default_prompt = (
                f"Draft 3-4 beats that carry the player through {stage_title} "
                f"during {quest_title}. Highlight twists or reveals."
            )

        prompt, ok = QInputDialog.getMultiLineText(
            self,
            "AI Stage Draft",
            "Describe what you want the assistant to propose:",
            default_prompt,
        )
        if not ok:
            return
        prompt = (prompt or "").strip()
        if not prompt:
            QMessageBox.information(
                self,
                "AI Stage Draft",
                "Please provide a short creative brief so the assistant has direction.",
            )
            return

        context = NarrativeContext(
            project_root=self.project_root,
            quests=self.quests,
            quest=quest,
            stage=stage,
            focus_stage_index=self.current_stage_index,
            stage_flow=list(self._stage_flow()),
            dialogues=self.dialogues,
            events=self.events,
            cinematics=self.cinematics,
        )

        QApplication.setOverrideCursor(Qt.WaitCursor)
        try:
            draft: StageDraft = generate_stage(prompt, context)
        except NarrativeGenerationError as exc:
            QMessageBox.critical(self, "AI Stage Draft", str(exc))
            return
        except Exception as exc:  # pragma: no cover - defensive safeguard
            QMessageBox.critical(self, "AI Stage Draft", f"Unexpected error: {exc}")
            return
        finally:
            QApplication.restoreOverrideCursor()

        stage_changed = False
        suggestion_lines: List[str] = []

        title_suggestion = (draft.stage_title or "").strip()
        if title_suggestion:
            if not (stage.get("title") or "").strip():
                stage["title"] = title_suggestion
                stage_changed = True
            else:
                suggestion_lines.append(f"Title suggestion: {title_suggestion}")

        desc_suggestion = (draft.stage_description or "").strip()
        if desc_suggestion:
            if not (stage.get("description") or "").strip():
                stage["description"] = desc_suggestion
                stage_changed = True
            else:
                snippet = desc_suggestion.splitlines()[0][:160]
                suggestion_lines.append(f"Description idea: {snippet}")

        notes_suggestion = (draft.stage_notes or "").strip()
        if notes_suggestion:
            existing_notes = (stage.get("notes") or "").strip()
            if notes_suggestion not in existing_notes:
                stage["notes"] = (
                    f"{existing_notes}\n\n{notes_suggestion}".strip()
                    if existing_notes
                    else notes_suggestion
                )
                stage_changed = True

        dialogue_count = len(draft.dialogue_updates)
        if dialogue_count:
            for key, payload in draft.dialogue_updates.items():
                if key:
                    self.dialogues[key] = payload

        event_count = len(draft.event_updates)
        if event_count:
            for key, payload in draft.event_updates.items():
                if key:
                    self.events[key] = payload

        cinematic_count = len(draft.cinematic_updates)
        if cinematic_count:
            for key, payload in draft.cinematic_updates.items():
                if key:
                    self.cinematics[key] = payload

        new_flow_entries = 0
        flow = self._stage_flow()
        existing_ids = [beat.get("id", "") for beat in flow if beat.get("id")]
        allowed_types = {"dialogue", "event", "cutscene", "tutorial", "note"}

        for raw_beat in draft.new_beats:
            if not isinstance(raw_beat, dict):
                continue
            beat = dict(raw_beat)
            beat_type = (beat.get("type") or "note").strip().lower() or "note"
            if beat_type not in allowed_types:
                beat_type = "note"
            beat["type"] = beat_type

            beat_id = (beat.get("id") or "").strip()
            if not beat_id:
                beat_id = unique_id(beat_type or "beat", existing_ids)
                beat["id"] = beat_id
                existing_ids.append(beat_id)

            if beat_type == "note":
                text = (beat.get("text") or "").strip()
                if text and not (beat.get("label") or "").strip():
                    beat["label"] = text.splitlines()[0][:90]
            else:
                if not (beat.get("label") or "").strip():
                    beat["label"] = beat.get("id", "") or beat_type.title()

            metadata = beat.get("metadata")
            if metadata is None:
                beat["metadata"] = {}
            elif not isinstance(metadata, dict):
                beat["metadata"] = {}

            flow.append(beat)
            new_flow_entries += 1

        made_changes = any(
            [
                stage_changed,
                new_flow_entries > 0,
                dialogue_count > 0,
                event_count > 0,
                cinematic_count > 0,
            ]
        )

        if made_changes:
            self._mark_dirty()

        if stage_changed:
            self._populate_stage_fields()
            self._refresh_stage_list()

        self._populate_flow_list()

        summary_lines: List[str] = []
        if new_flow_entries:
            plural = "s" if new_flow_entries != 1 else ""
            summary_lines.append(f"Added {new_flow_entries} beat{plural}.")
        if dialogue_count:
            plural = "s" if dialogue_count != 1 else ""
            summary_lines.append(f"{dialogue_count} dialogue update{plural}.")
        if event_count:
            plural = "s" if event_count != 1 else ""
            summary_lines.append(f"{event_count} event update{plural}.")
        if cinematic_count:
            plural = "s" if cinematic_count != 1 else ""
            summary_lines.append(f"{cinematic_count} cinematic update{plural}.")
        if stage_changed and not summary_lines:
            summary_lines.append("Stage details updated from AI draft.")
        summary_lines.extend(suggestion_lines)
        if draft.notes and draft.notes.strip():
            summary_lines.append(draft.notes.strip())

        status_text = (
            summary_lines[0]
            if summary_lines
            else "AI draft ready. Review the new beats in the flow."
        )
        if hasattr(self, "status_hint_label"):
            self.status_hint_label.setText(status_text)

        if summary_lines:
            QMessageBox.information(self, "AI Stage Draft", "\n".join(summary_lines))
        else:
            QMessageBox.information(
                self,
                "AI Stage Draft",
                "AI draft ready. Review the new beats in the flow.",
            )

    def _add_flow_beat(self, beat_type: str):
        key = self._current_flow_key()
        if not key:
            return
        flow = self._stage_flow()
        new_index: Optional[int] = None
        if beat_type == "dialogue":
            dlg = DialogueDialog(self, list(self.dialogues.keys()))
            if dlg.exec_() != QDialog.Accepted:
                return
            data = dlg.data()
            self.dialogues[data["id"]] = data
            beat = {"type": "dialogue", "id": data["id"], "label": data.get("speaker", "") or data["id"]}
            flow.append(beat)
            new_index = len(flow) - 1
        elif beat_type == "event":
            dlg = EventActionDialog(self, list(self.events.keys()))
            if dlg.exec_() != QDialog.Accepted:
                return
            data = dlg.data()
            self.events[data["id"]] = self._translate_event_dialog(data)
            beat = {"type": "event", "id": data["id"], "label": data.get("type", "")}
            flow.append(beat)
            new_index = len(flow) - 1
        elif beat_type == "cutscene":
            dlg = CutsceneDialog(self, list(self.cinematics.keys()))
            if dlg.exec_() != QDialog.Accepted:
                return
            scene_id, steps = dlg.data()
            self.cinematics[scene_id] = steps
            beat = {"type": "cutscene", "id": scene_id, "label": scene_id}
            flow.append(beat)
            new_index = len(flow) - 1
        elif beat_type == "note":
            text, ok = QInputDialog.getMultiLineText(self, "Add Note", "Note:", "")
            if not ok or not text.strip():
                return
            beat = {"type": "note", "id": unique_id("note", [b.get("id", "") for b in flow]), "label": text.strip()}
            beat["text"] = text.strip()
            flow.append(beat)
            new_index = len(flow) - 1
        else:
            return
        if new_index is None:
            return
        self._mark_dirty()
        self._populate_flow_list()
        self.flow_list.setCurrentRow(new_index)
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    def _add_flow_beat_from_template(self, beat_type: str):
        key = self._current_flow_key()
        if not key:
            return

        template_path, _ = QFileDialog.getOpenFileName(
            self, f"Select {beat_type.title()} Template", str(self.project_root / "templates"), "Jinja2 Templates (*.json.j2 *.json)"
        )
        if not template_path:
            return

        try:
            with open(template_path, "r", encoding="utf-8") as f:
                template_content = f.read()
        except Exception as e:
            QMessageBox.critical(self, "Template Error", f"Failed to load template:\n{e}")
            return

        # Find all variables in the template
        variables = sorted(list(set(re.findall(r"\{\{\s*(\w+)\s*\}\}", template_content))))

        template_values = {}
        if variables:
            var_dialog = TemplateVariablesDialog(self, variables)
            if var_dialog.exec_() == QDialog.Accepted:
                template_values = var_dialog.get_values()
            else:
                return # User cancelled

        # Render the template
        try:
            jinja_env = Environment(loader=FileSystemLoader(os.path.dirname(template_path)))
            template = jinja_env.from_string(template_content)
            rendered_json = template.render(template_values)
            new_beat_data = json.loads(rendered_json)
        except Exception as e:
            QMessageBox.critical(self, "Template Error", f"Failed to render template or parse JSON:\n{e}")
            return

        new_id_suggestion = new_beat_data.get("id", f"new_{beat_type}")
        new_id, ok = QInputDialog.getText(self, f"New {beat_type.title()} ID", f"Enter new {beat_type} ID:", text=new_id_suggestion)
        if not ok or not new_id.strip():
            return
        new_id = new_id.strip()

        if beat_type == "event":
            if new_id in self.events:
                QMessageBox.warning(self, "ID Error", f"Event ID '{new_id}' already exists.")
                return
            new_beat_data["id"] = new_id
            self.events[new_id] = new_beat_data
            label = new_id
        elif beat_type == "cutscene":
            if new_id in self.cinematics:
                QMessageBox.warning(self, "ID Error", f"Cutscene ID '{new_id}' already exists.")
                return
            # new_beat_data is the full scene, not a dict with a 'scenes' key
            self.cinematics[new_id] = new_beat_data
            label = new_id
        else:
            return

        flow = self._stage_flow()
        beat = {"type": beat_type, "id": new_id, "label": label}
        flow.append(beat)
        self._mark_dirty()
        self._populate_flow_list()
        self.flow_list.setCurrentRow(len(flow) - 1)
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    def _translate_event_dialog(self, data: dict) -> dict:
        # Convert generic arg placeholders to more concrete structure when possible
        evt = json.loads(json.dumps(data))
        for action in evt.get("actions", []):
            # Replace arg placeholders with known keys if ACTION_DEFS exists
            if ACTION_DEFS and action.get("type") in ACTION_DEFS:
                spec = ACTION_DEFS[action["type"]]
                fields = spec.get("fields", [])
                for idx, field in enumerate(fields):
                    key = field[0]
                    placeholder = action.pop(f"arg{idx+1}", None)
                    if placeholder:
                        action[key] = placeholder
            else:
                for idx in (1, 2, 3):
                    placeholder = action.pop(f"arg{idx}", None)
                    if placeholder:
                        action.setdefault(f"arg{idx}", placeholder)
        return evt

    def _remove_flow_beat(self):
        row = self.flow_list.currentRow()
        if row < 0:
            return
        beats = self._stage_flow()
        if 0 <= row < len(beats):
            beats.pop(row)
            self._mark_dirty()
            self._populate_flow_list()
            if self.flow_list.count():
                self.flow_list.setCurrentRow(min(row, self.flow_list.count() - 1))
            self._refresh_stage_outline()
            self._refresh_quest_outline()

    def _move_flow(self, delta: int):
        row = self.flow_list.currentRow()
        beats = self._stage_flow()
        if row < 0 or not (0 <= row < len(beats)):
            return
        new_row = row + delta
        if not (0 <= new_row < len(beats)):
            return
        beats[row], beats[new_row] = beats[new_row], beats[row]
        self._mark_dirty()
        self._populate_flow_list()
        self.flow_list.setCurrentRow(new_row)
        self._refresh_stage_outline()
        self._refresh_quest_outline()

    def _edit_flow_beat(self):
        row = self.flow_list.currentRow()
        beats = self._stage_flow()
        if row < 0 or not (0 <= row < len(beats)):
            return

        beat = beats[row]
        beat_type = beat.get("type")
        ref_id = beat.get("id")

        if not ref_id:
            if beat_type != "note":
                QMessageBox.information(self, "Cannot Jump", "This beat has no ID to jump to.")
                return

        if beat_type == "dialogue":
            data = self.dialogues.get(ref_id)
            dlg = DialogueDialog(self, list(self.dialogues.keys()), data=data)
            if dlg.exec_() == QDialog.Accepted:
                new_data = dlg.data()
                if ref_id and ref_id != new_data["id"]:
                    self.dialogues.pop(ref_id, None)
                self.dialogues[new_data["id"]] = new_data
                beat["id"] = new_data["id"]
                beat["label"] = new_data.get("speaker", "") or new_data["id"]
                self._mark_dirty()
        elif beat_type == "event":
            data = self.events.get(ref_id)
            dlg = EventActionDialog(self, list(self.events.keys()), data=data)
            if dlg.exec_() == QDialog.Accepted:
                new_data = dlg.data()
                if ref_id and ref_id != new_data["id"]:
                    self.events.pop(ref_id, None)
                self.events[new_data["id"]] = self._translate_event_dialog(new_data)
                beat["id"] = new_data["id"]
                beat["label"] = new_data.get("type", "")
                self._mark_dirty()
        elif beat_type == "cutscene":
            steps = self.cinematics.get(ref_id)
            dlg = CutsceneDialog(self, list(self.cinematics.keys()), scene_id=ref_id, steps=steps)
            if dlg.exec_() == QDialog.Accepted:
                scene_id, new_steps = dlg.data()
                if ref_id and ref_id != scene_id:
                    self.cinematics.pop(ref_id, None)
                self.cinematics[scene_id] = new_steps
                beat["id"] = scene_id
                beat["label"] = scene_id
                self._mark_dirty()

        elif beat_type == "note":
            # Notes are simple and can be edited inline
            text, ok = QInputDialog.getMultiLineText(self, "Edit Note", "Note:", beat.get("text", ""))
            if ok:
                beat["text"] = text
                beat["label"] = text.splitlines()[0] if text.strip() else beat.get("id", "")
                self._mark_dirty()
                self._populate_flow_list()
                if 0 <= row < self.flow_list.count():
                    self.flow_list.setCurrentRow(row)
                self._refresh_stage_outline()
                self._refresh_quest_outline()

    # -------------------- misc actions ------------------------
    def _reveal_files(self):
        QFileDialog.getOpenFileName(self, "Inspect narrative file", str(self.project_root), "JSON (*.json)")

    def _launch_editor_picker(self):
        menu = QMessageBox(self)
        menu.setWindowTitle("Open detailed editor")
        menu.setText("Choose which specialized editor to open.")
        buttons: Dict[Any, Tuple[str, str]] = {}
        quest = self._find_quest(self.current_quest_id)
        if quest:
            btn = menu.addButton("Quest Editor", QMessageBox.ActionRole)
            buttons[btn] = ("quest", quest.get("id", ""))
        row = self.flow_list.currentRow()
        beats = self._stage_flow()
        beat = beats[row] if 0 <= row < len(beats) else None
        if beat:
            beat_type = beat.get("type")
            ident = beat.get("id", "")
            if beat_type in ("dialogue", "event", "cutscene"):
                btn = menu.addButton(f"{beat_type.title()} Editor", QMessageBox.ActionRole)
                type_key = "cutscene" if beat_type == "cutscene" else beat_type
                buttons[btn] = (type_key, ident)
        menu.addButton(QMessageBox.Cancel)
        menu.exec_()
        chosen = menu.clickedButton()
        if chosen in buttons:
            type_key, ident = buttons[chosen]
            if ident:
                studio_goto(type_key, ident)

    # -------------------- API for Studio ----------------------
    def closeEvent(self, event):
        if self._dirty:
            reply = QMessageBox.question(self, "Unsaved changes", "Save changes before closing?", QMessageBox.Yes | QMessageBox.No | QMessageBox.Cancel)
            if reply == QMessageBox.Cancel:
                event.ignore()
                return
            if reply == QMessageBox.Yes:
                self._save_all()
        event.accept()


def launch_from_studio(project_root: Optional[str] = None):
    app = QApplication.instance() or QApplication([])
    widget = NarrativeBuilder(Path(project_root) if project_root else None)
    widget.show()
    return widget


def main():
    os.environ.setdefault("STUDIO_MANAGED", "1")
    app = QApplication.instance() or QApplication([])
    root = detect_project_root(Path(__file__).parent)
    view = NarrativeBuilder(root)
    view.show()
    app.exec_()


if __name__ == "__main__":
    main()
