#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Starborn — Narrative Studio

A ground-up narrative workbench for stitching together story moments across:
  - Quests + stages (quests.json)
  - Dialogue lines (dialogue.json)
  - Events (events.json)
  - Cinematic scenes (cinematics.json)
  - Milestones (milestones.json)
  - Stage beat flows (narrative_flows.json) — dev-only authoring map

Design goals:
  • "Moments first": author an ordered beat flow per quest stage (setup → twist → payoff).
  • Fast linking: pick/validate IDs and jump to specialized editors via Studio Pro.
  • Safe by default: no surprise schema conversions; preserves list-based asset files.
  • Useful standalone: can run as a normal PyQt tool, or as a Studio Pro plugin.

Usage (standalone):
    pip install PyQt5
    python PythonDevKit/narrative_studio.py
"""

from __future__ import annotations

import copy
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Sequence, Tuple

from PyQt5.QtCore import Qt
from PyQt5.QtGui import QKeySequence
from PyQt5.QtWidgets import (
    QApplication,
    QAbstractItemView,
    QComboBox,
    QCompleter,
    QDialog,
    QDialogButtonBox,
    QFileDialog,
    QFormLayout,
    QGroupBox,
    QHBoxLayout,
    QCheckBox,
    QLabel,
    QLineEdit,
    QListWidget,
    QListWidgetItem,
    QMenu,
    QMessageBox,
    QPlainTextEdit,
    QPushButton,
    QSplitter,
    QStatusBar,
    QTabWidget,
    QTableWidget,
    QTableWidgetItem,
    QToolButton,
    QTreeWidget,
    QTreeWidgetItem,
    QVBoxLayout,
    QWidget,
    QShortcut,
)

from data_core import json_load, json_save, unique_id
from devkit_paths import resolve_paths
from editor_bus import EditorBus, goto as studio_goto
from ui_common import attach_hotkeys, flash_status


BEAT_TYPES: Tuple[str, ...] = ("dialogue", "event", "cutscene", "tutorial", "milestone", "note")
FLOW_FILENAME = "narrative_flows.json"

# Current runtime support (see app/src/main/java/.../EventManager.kt).
EVENT_TRIGGER_TYPES: Tuple[str, ...] = (
    "talk_to",
    "npc_interaction",
    "dialogue_closed",
    "dialogue_dismissed",
    "enter_room",
    "player_action",
    "quest_stage_complete",
    "encounter_victory",
    "encounter_defeat",
    "encounter_retreat",
    "item_acquired",
)

EVENT_CONDITION_TYPES: Tuple[str, ...] = (
    "milestone_not_set",
    "milestone_set",
    "quest_active",
    "quest_not_started",
    "quest_completed",
    "quest_not_completed",
    "quest_failed",
    "quest_stage",
    "quest_stage_not",
    "quest_task_done",
    "quest_task_not_done",
    "event_completed",
    "event_not_completed",
    "tutorial_completed",
    "tutorial_not_completed",
    "item",
    "item_not",
)

EVENT_ACTION_TYPES: Tuple[str, ...] = (
    "if_quest_active",
    "if_quest_not_started",
    "if_quest_completed",
    "if_quest_not_completed",
    "if_quest_failed",
    "if_milestone_set",
    "if_milestone_not_set",
    "if_milestones_set",
    "set_milestone",
    "clear_milestone",
    "start_quest",
    "complete_quest",
    "fail_quest",
    "track_quest",
    "untrack_quest",
    "play_cinematic",
    "trigger_cutscene",
    "show_message",
    "give_reward",
    "grant_reward",
    "set_room_state",
    "toggle_room_state",
    "spawn_encounter",
    "give_item",
    "give_item_to_player",
    "take_item",
    "reveal_hidden_item",
    "spawn_item_on_ground",
    "player_action",
    "add_party_member",
    "give_xp",
    "set_quest_task_done",
    "advance_quest",
    "advance_quest_if_active",
    "advance_quest_stage",
    "begin_node",
    "system_tutorial",
    "audio_layer",
    "unlock_room_search",
    "rebuild_ui",
    "wait_for_draw",
    "narrate",
)

# Standalone fallback (when not embedded in Studio Pro).
# NOTE: cutscenes are intentionally omitted: our project currently stores cinematics as a list of
# {"id","title","steps"} objects, while the dedicated CutsceneEditor expects a different schema.
_STANDALONE_EDITOR_SPECS: Dict[str, Tuple[str, str]] = {
    "quest": ("quest_editor", "QuestEditor"),
    "dialogue": ("dialogue_editor", "DialogueEditor"),
    "event": ("event_editor", "EventEditor"),
    "milestone": ("milestone_editor", "MilestoneEditor"),
}


def _slugify(text: str) -> str:
    text = (text or "").strip().lower()
    text = re.sub(r"[^a-z0-9]+", "_", text)
    text = re.sub(r"_+", "_", text).strip("_")
    return text or "untitled"


def _safe_list_json(path: Path) -> List[dict]:
    try:
        data = json_load(path, default=[])
    except Exception:
        return []
    if isinstance(data, list):
        return [x for x in data if isinstance(x, dict)]
    if isinstance(data, dict):
        # Best-effort support for older dict formats.
        vals = []
        for k, v in data.items():
            if isinstance(v, dict):
                row = dict(v)
                row.setdefault("id", k)
                vals.append(row)
        return vals
    return []


def _index_by_id(rows: Iterable[dict]) -> Dict[str, dict]:
    out: Dict[str, dict] = {}
    for row in rows:
        rid = str((row or {}).get("id") or "").strip()
        if rid:
            out[rid] = row
    return out


def _sorted_ids(d: Dict[str, Any]) -> List[str]:
    return sorted(d.keys(), key=str.lower)


def _ensure_str(v: Any) -> str:
    return str(v) if v is not None else ""


def _fmt_one_line(text: str, limit: int = 90) -> str:
    t = " ".join((text or "").strip().split())
    if len(t) <= limit:
        return t
    return t[: max(0, limit - 1)].rstrip() + "…"


def _make_completer(values: Sequence[str]) -> QCompleter:
    comp = QCompleter(list(values))
    comp.setCaseSensitivity(Qt.CaseInsensitive)
    comp.setFilterMode(Qt.MatchContains)
    return comp


def _wrap_layout(layout) -> QWidget:
    """
    Convenience: allow inserting a layout into a QFormLayout row.
    """
    w = QWidget()
    w.setLayout(layout)
    return w


class FlowListWidget(QListWidget):
    """
    QListWidget with InternalMove enabled that notifies after a reorder.
    """

    def __init__(self, parent: Optional[QWidget] = None):
        super().__init__(parent)
        self.on_reordered = None  # type: ignore[assignment]
        self.setSelectionMode(QAbstractItemView.SingleSelection)
        self.setAlternatingRowColors(True)
        self.setDragEnabled(True)
        self.setAcceptDrops(True)
        self.setDropIndicatorShown(True)
        self.setDragDropMode(QAbstractItemView.InternalMove)

    def dropEvent(self, event):  # type: ignore[override]
        super().dropEvent(event)
        if callable(self.on_reordered):
            try:
                self.on_reordered()
            except Exception:
                pass


class ActionTreeWidget(QTreeWidget):
    """
    QTreeWidget with InternalMove enabled that notifies after a reorder.
    """

    def __init__(self, parent: Optional[QWidget] = None):
        super().__init__(parent)
        self.on_reordered = None  # type: ignore[assignment]
        self.setDragEnabled(True)
        self.setAcceptDrops(True)
        self.setDropIndicatorShown(True)
        self.setDragDropMode(QAbstractItemView.InternalMove)

    def dropEvent(self, event):  # type: ignore[override]
        # Only allow actions to be dropped:
        #   - at root (top-level order), or
        #   - inside a group node (do/elseDo/on_complete)
        target = self.itemAt(event.pos())
        pos = self.dropIndicatorPosition()
        parent: Optional[QTreeWidgetItem]
        if pos == QAbstractItemView.OnItem:
            parent = target
        elif pos in (QAbstractItemView.AboveItem, QAbstractItemView.BelowItem):
            parent = target.parent() if target else None
        else:
            parent = None

        if parent is not None:
            meta = parent.data(0, Qt.UserRole)
            if not isinstance(meta, dict) or meta.get("kind") != "group":
                event.ignore()
                return

        super().dropEvent(event)
        if callable(self.on_reordered):
            try:
                self.on_reordered()
            except Exception:
                pass


class DialogueQuickEdit(QDialog):
    _COND_TYPES: Tuple[str, ...] = (
        "quest",
        "quest_active",
        "quest_completed",
        "quest_not_started",
        "quest_failed",
        "quest_stage",
        "quest_stage_not",
        "milestone",
        "milestone_not_set",
        "item",
        "item_not",
        "event_completed",
        "event_not_completed",
        "tutorial_completed",
        "tutorial_not_completed",
    )
    _TRIG_TYPES: Tuple[str, ...] = (
        "start_quest",
        "complete_quest",
        "fail_quest",
        "track_quest",
        "untrack_quest",
        "set_milestone",
        "clear_milestone",
        "give_item",
        "take_item",
        "give_credits",
        "give_xp",
        "set_quest_task_done",
        "advance_quest_stage",
        "advance_quest",
        "recruit",
        "system_tutorial",
        "play_cinematic",
        "player_action",
    )

    def __init__(
        self,
        parent: QWidget,
        *,
        existing_ids: Iterable[str],
        data: Optional[dict] = None,
        npc_names: Sequence[str] = (),
        quest_ids: Sequence[str] = (),
        milestone_ids: Sequence[str] = (),
        item_ids: Sequence[str] = (),
        event_ids: Sequence[str] = (),
        tutorial_ids: Sequence[str] = (),
        cinematic_ids: Sequence[str] = (),
        quest_stage_keys: Sequence[str] = (),
        quest_task_keys: Sequence[str] = (),
        player_action_ids: Sequence[str] = (),
    ):
        super().__init__(parent)
        self.setWindowTitle("Quick Edit — Dialogue")
        self.resize(760, 560)
        self._existing = set(existing_ids)
        self._orig_id = (data or {}).get("id") if isinstance(data, dict) else None
        self._data = dict(data) if isinstance(data, dict) else {}
        self._event_ids = sorted({str(x).strip() for x in existing_ids if str(x).strip()}, key=str.lower)

        self._npc_names = list(npc_names)
        self._quest_ids = list(quest_ids)
        self._milestone_ids = list(milestone_ids)
        self._item_ids = list(item_ids)
        self._event_ids = list(event_ids)
        self._tutorial_ids = list(tutorial_ids)
        self._cinematic_ids = list(cinematic_ids)
        self._quest_stage_keys = list(quest_stage_keys)
        self._quest_task_keys = list(quest_task_keys)
        self._player_action_ids = list(player_action_ids)

        layout = QVBoxLayout(self)
        form = QFormLayout()

        self.id_edit = QLineEdit(_ensure_str(self._data.get("id")))
        self.speaker_edit = QLineEdit(_ensure_str(self._data.get("speaker")))
        self.emote_edit = QLineEdit(_ensure_str(self._data.get("emote")))
        self.text_edit = QPlainTextEdit(_ensure_str(self._data.get("text")))
        self.next_edit = QLineEdit(_ensure_str(self._data.get("next")))
        self.condition_edit = QLineEdit(_ensure_str(self._data.get("condition")))
        self.trigger_edit = QLineEdit(_ensure_str(self._data.get("trigger")))

        if self._npc_names:
            self.speaker_edit.setCompleter(_make_completer(self._npc_names))
        next_ids = sorted(set(existing_ids), key=str.lower)
        if next_ids:
            self.next_edit.setCompleter(_make_completer(next_ids))

        form.addRow("ID:", self.id_edit)
        form.addRow("Speaker:", self.speaker_edit)
        form.addRow("Emote (optional):", self.emote_edit)
        form.addRow("Text:", self.text_edit)
        form.addRow("Next (optional):", self.next_edit)
        form.addRow("Condition (optional):", self.condition_edit)
        form.addRow("Trigger (optional):", self.trigger_edit)
        layout.addLayout(form)

        helpers = QGroupBox("Token Helpers (append to condition/trigger)")
        helpers_form = QFormLayout(helpers)

        # Condition helper row
        self.cond_type = QComboBox()
        self.cond_type.addItems(list(self._COND_TYPES))
        self.cond_value = QComboBox()
        self.cond_value.setEditable(True)
        self.cond_value.setInsertPolicy(QComboBox.NoInsert)
        b_add_cond = QPushButton("Append")
        cond_row = QHBoxLayout()
        cond_row.addWidget(self.cond_type)
        cond_row.addWidget(self.cond_value, 1)
        cond_row.addWidget(b_add_cond)
        helpers_form.addRow("Condition:", _wrap_layout(cond_row))

        # Trigger helper row
        self.trig_type = QComboBox()
        self.trig_type.addItems(list(self._TRIG_TYPES))
        self.trig_value = QComboBox()
        self.trig_value.setEditable(True)
        self.trig_value.setInsertPolicy(QComboBox.NoInsert)
        b_add_trig = QPushButton("Append")
        trig_row = QHBoxLayout()
        trig_row.addWidget(self.trig_type)
        trig_row.addWidget(self.trig_value, 1)
        trig_row.addWidget(b_add_trig)
        helpers_form.addRow("Trigger:", _wrap_layout(trig_row))

        layout.addWidget(helpers)

        self.cond_type.currentTextChanged.connect(self._refresh_cond_value_choices)
        self.trig_type.currentTextChanged.connect(self._refresh_trig_value_choices)
        b_add_cond.clicked.connect(self._append_condition_token)
        b_add_trig.clicked.connect(self._append_trigger_token)
        self._refresh_cond_value_choices(self.cond_type.currentText())
        self._refresh_trig_value_choices(self.trig_type.currentText())

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self._accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def _cond_values_for(self, cond_type: str) -> List[str]:
        t = (cond_type or "").strip()
        if t in ("quest", "quest_active", "quest_completed", "quest_not_started", "quest_failed"):
            return list(self._quest_ids)
        if t in ("quest_stage", "quest_stage_not"):
            return list(self._quest_stage_keys)
        if t in ("milestone", "milestone_not_set"):
            return list(self._milestone_ids)
        if t in ("item", "item_not"):
            return list(self._item_ids)
        if t in ("event_completed", "event_not_completed"):
            return list(self._event_ids)
        if t in ("tutorial_completed", "tutorial_not_completed"):
            return list(self._tutorial_ids)
        return []

    def _trig_values_for(self, trig_type: str) -> List[str]:
        t = (trig_type or "").strip()
        if t in ("start_quest", "complete_quest", "fail_quest", "track_quest", "advance_quest"):
            return list(self._quest_ids)
        if t in ("set_milestone", "clear_milestone"):
            return list(self._milestone_ids)
        if t in ("give_item", "take_item"):
            return list(self._item_ids)
        if t in ("set_quest_task_done",):
            return list(self._quest_task_keys)
        if t in ("advance_quest_stage",):
            return list(self._quest_stage_keys)
        if t in ("system_tutorial",):
            return list(self._tutorial_ids)
        if t in ("play_cinematic",):
            return list(self._cinematic_ids)
        if t in ("player_action",):
            return list(self._player_action_ids)
        return []

    def _refresh_cond_value_choices(self, cond_type: str):
        values = [""] + self._cond_values_for(cond_type)
        cur = self.cond_value.currentText()
        self.cond_value.blockSignals(True)
        try:
            self.cond_value.clear()
            self.cond_value.addItems(values)
            self.cond_value.setCurrentText(cur)
            self.cond_value.setCompleter(_make_completer(values))
        finally:
            self.cond_value.blockSignals(False)

    def _refresh_trig_value_choices(self, trig_type: str):
        values = [""] + self._trig_values_for(trig_type)
        cur = self.trig_value.currentText()
        self.trig_value.blockSignals(True)
        try:
            self.trig_value.clear()
            self.trig_value.addItems(values)
            self.trig_value.setCurrentText(cur)
            self.trig_value.setCompleter(_make_completer(values))
        finally:
            self.trig_value.blockSignals(False)

    def _append_condition_token(self):
        ctype = self.cond_type.currentText().strip()
        value = self.cond_value.currentText().strip()
        if not value:
            QMessageBox.information(self, "Condition", "Pick or type a value to append.")
            return
        token = f"{ctype}:{value}"
        cur = (self.condition_edit.text() or "").strip()
        self.condition_edit.setText(f"{cur}, {token}" if cur else token)

    def _append_trigger_token(self):
        ttype = self.trig_type.currentText().strip()
        value = self.trig_value.currentText().strip()
        requires_value = ttype not in ("untrack_quest",)
        if requires_value and not value:
            QMessageBox.information(self, "Trigger", "Pick or type a value to append.")
            return
        token = f"{ttype}:{value}" if value else ttype
        cur = (self.trigger_edit.text() or "").strip()
        self.trigger_edit.setText(f"{cur}, {token}" if cur else token)

    def _accept(self):
        did = self.id_edit.text().strip()
        if not did:
            QMessageBox.warning(self, "Missing ID", "Dialogue ID is required.")
            return
        if did != self._orig_id and did in self._existing:
            QMessageBox.warning(self, "Duplicate ID", f"Dialogue '{did}' already exists.")
            return
        speaker = self.speaker_edit.text().strip()
        if not speaker:
            QMessageBox.warning(self, "Missing Speaker", "Speaker is required (usually the NPC name).")
            return
        text = self.text_edit.toPlainText().strip()
        if not text:
            QMessageBox.warning(self, "Missing Text", "Dialogue text cannot be empty.")
            return

        base = dict(self._data)
        base["id"] = did
        base["speaker"] = speaker
        base["text"] = text

        emote = self.emote_edit.text().strip()
        if emote:
            base["emote"] = emote
        else:
            base.pop("emote", None)

        nxt = self.next_edit.text().strip()
        if nxt:
            base["next"] = nxt
        else:
            base.pop("next", None)

        cond = self.condition_edit.text().strip()
        if cond:
            base["condition"] = cond
        else:
            base.pop("condition", None)

        trig = self.trigger_edit.text().strip()
        if trig:
            base["trigger"] = trig
        else:
            base.pop("trigger", None)

        self._data = base
        self.accept()

    def value(self) -> dict:
        return dict(self._data)


class MilestoneQuickEdit(QDialog):
    def __init__(self, parent: QWidget, *, existing_ids: Iterable[str], data: Optional[dict] = None):
        super().__init__(parent)
        self.setWindowTitle("Quick Edit — Milestone")
        self.resize(560, 420)
        self._existing = set(existing_ids)
        self._orig_id = (data or {}).get("id") if isinstance(data, dict) else None
        self._data = dict(data) if isinstance(data, dict) else {}

        layout = QVBoxLayout(self)
        form = QFormLayout()

        self.id_edit = QLineEdit(_ensure_str(self._data.get("id")))
        self.name_edit = QLineEdit(_ensure_str(self._data.get("name")))
        self.desc_edit = QPlainTextEdit(_ensure_str(self._data.get("description")))
        self.toast_edit = QLineEdit(_ensure_str(self._data.get("toast")))

        form.addRow("ID:", self.id_edit)
        form.addRow("Name:", self.name_edit)
        form.addRow("Description:", self.desc_edit)
        form.addRow("Toast (optional):", self.toast_edit)
        layout.addLayout(form)

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self._accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def _accept(self):
        mid = self.id_edit.text().strip()
        if not mid:
            QMessageBox.warning(self, "Missing ID", "Milestone ID is required.")
            return
        if mid != self._orig_id and mid in self._existing:
            QMessageBox.warning(self, "Duplicate ID", f"Milestone '{mid}' already exists.")
            return
        name = self.name_edit.text().strip()
        if not name:
            QMessageBox.warning(self, "Missing Name", "Milestone name is required.")
            return
        base = dict(self._data)
        base["id"] = mid
        base["name"] = name
        base["description"] = self.desc_edit.toPlainText().strip()
        toast = self.toast_edit.text().strip()
        if toast:
            base["toast"] = toast
        else:
            base.pop("toast", None)
        self._data = base
        self.accept()

    def value(self) -> dict:
        return dict(self._data)


class EventQuickEdit(QDialog):
    def __init__(
        self,
        parent: QWidget,
        *,
        existing_ids: Iterable[str],
        data: Optional[dict] = None,
        quest_ids: Sequence[str] = (),
        milestone_ids: Sequence[str] = (),
        item_ids: Sequence[str] = (),
        npc_ids: Sequence[str] = (),
        room_ids: Sequence[str] = (),
        cinematic_ids: Sequence[str] = (),
        tutorial_ids: Sequence[str] = (),
        quest_stage_keys: Sequence[str] = (),
        quest_task_keys: Sequence[str] = (),
        player_action_ids: Sequence[str] = (),
    ):
        super().__init__(parent)
        self.setWindowTitle("Quick Edit — Event")
        self.resize(980, 680)
        self._existing = set(existing_ids)
        self._orig_id = (data or {}).get("id") if isinstance(data, dict) else None
        self._data = dict(data) if isinstance(data, dict) else {}
        self._event_ids = sorted({str(x).strip() for x in existing_ids if str(x).strip()}, key=str.lower)

        self._quest_ids = list(quest_ids)
        self._milestone_ids = list(milestone_ids)
        self._item_ids = list(item_ids)
        self._npc_ids = list(npc_ids)
        self._room_ids = list(room_ids)
        self._cinematic_ids = list(cinematic_ids)
        self._tutorial_ids = list(tutorial_ids)
        self._quest_stage_keys = list(quest_stage_keys)
        self._quest_task_keys = list(quest_task_keys)
        self._player_action_ids = list(player_action_ids)

        self._actions: List[dict] = []
        self._conditions: List[dict] = []

        layout = QVBoxLayout(self)
        self.tabs = QTabWidget()
        layout.addWidget(self.tabs, 1)

        self._build_tab_overview()
        self._build_tab_trigger()
        self._build_tab_conditions()
        self._build_tab_actions()
        self._build_tab_raw()

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self._accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

        self._load_from_data(self._data)
        self._rebuild_raw_from_form()

    # -------------------- Tabs --------------------

    def _build_tab_overview(self):
        w = QWidget()
        l = QVBoxLayout(w)
        form = QFormLayout()

        self.id_edit = QLineEdit()
        self.desc_edit = QPlainTextEdit()
        self.repeatable_chk = QCheckBox("Repeatable")
        self.on_msg_edit = QLineEdit()
        self.off_msg_edit = QLineEdit()
        self.summary_label = QLabel("")
        # self.summary_label.setStyleSheet("color: #6f6f6f;")

        form.addRow("ID:", self.id_edit)
        form.addRow("Description:", self.desc_edit)
        form.addRow("", self.repeatable_chk)
        form.addRow("On Message (optional):", self.on_msg_edit)
        form.addRow("Off Message (optional):", self.off_msg_edit)
        form.addRow("Summary:", self.summary_label)
        l.addLayout(form)
        l.addStretch(1)

        self.tabs.addTab(w, "Main")

    def _build_tab_trigger(self):
        w = QWidget()
        l = QVBoxLayout(w)
        form = QFormLayout()

        self.trig_type_combo = QComboBox()
        self.trig_type_combo.addItems(list(EVENT_TRIGGER_TYPES))

        self.trig_npc_combo = QComboBox()
        self.trig_npc_combo.setEditable(True)
        self.trig_npc_combo.setInsertPolicy(QComboBox.NoInsert)
        npc_vals = [""] + list(self._npc_ids)
        self.trig_npc_combo.addItems(npc_vals)
        self.trig_npc_combo.setCompleter(_make_completer(npc_vals))

        self.trig_room_combo = QComboBox()
        self.trig_room_combo.setEditable(True)
        self.trig_room_combo.setInsertPolicy(QComboBox.NoInsert)
        room_vals = [""] + list(self._room_ids)
        self.trig_room_combo.addItems(room_vals)
        self.trig_room_combo.setCompleter(_make_completer(room_vals))

        self.trig_quest_combo = QComboBox()
        self.trig_quest_combo.setEditable(True)
        self.trig_quest_combo.setInsertPolicy(QComboBox.NoInsert)
        quest_vals = [""] + list(self._quest_ids)
        self.trig_quest_combo.addItems(quest_vals)
        self.trig_quest_combo.setCompleter(_make_completer(quest_vals))

        self.trig_action_combo = QComboBox()
        self.trig_action_combo.setEditable(True)
        self.trig_action_combo.setInsertPolicy(QComboBox.NoInsert)
        action_vals = [""] + list(self._player_action_ids)
        self.trig_action_combo.addItems(action_vals)
        self.trig_action_combo.setCompleter(_make_completer(action_vals))

        self.trig_item_combo = QComboBox()
        self.trig_item_combo.setEditable(True)
        self.trig_item_combo.setInsertPolicy(QComboBox.NoInsert)
        item_vals = [""] + list(self._item_ids)
        self.trig_item_combo.addItems(item_vals)
        self.trig_item_combo.setCompleter(_make_completer(item_vals))

        self.trig_enemies_edit = QLineEdit()
        self.trig_enemies_edit.setPlaceholderText("comma-separated enemy ids (optional)")

        form.addRow("Type:", self.trig_type_combo)
        form.addRow("NPC (talk/dialogue):", self.trig_npc_combo)
        form.addRow("Room (enter_room):", self.trig_room_combo)
        form.addRow("Quest (quest_stage_complete):", self.trig_quest_combo)
        form.addRow("Action (player_action):", self.trig_action_combo)
        form.addRow("Item (optional):", self.trig_item_combo)
        form.addRow("Enemies (optional list):", self.trig_enemies_edit)
        l.addLayout(form)

        self.trig_type_combo.currentTextChanged.connect(self._update_trigger_field_enables)
        self.trig_type_combo.currentTextChanged.connect(lambda _t: self._update_summary())
        for wdg in (self.trig_npc_combo, self.trig_room_combo, self.trig_quest_combo, self.trig_action_combo, self.trig_item_combo, self.trig_enemies_edit):
            try:
                wdg.currentTextChanged.connect(lambda _v: self._update_summary())  # type: ignore[attr-defined]
            except Exception:
                pass
        self._update_trigger_field_enables(self.trig_type_combo.currentText())

        l.addStretch(1)
        self.tabs.addTab(w, "Trigger")

    def _build_tab_conditions(self):
        w = QWidget()
        l = QVBoxLayout(w)

        self.cond_list = QListWidget()
        self.cond_list.setSelectionMode(QAbstractItemView.SingleSelection)
        self.cond_list.setAlternatingRowColors(True)
        self.cond_list.itemDoubleClicked.connect(lambda _it: self._edit_selected_condition())
        l.addWidget(self.cond_list, 1)

        btns = QHBoxLayout()
        b_add = QPushButton("Add…")
        b_edit = QPushButton("Edit…")
        b_del = QPushButton("Delete")
        b_up = QPushButton("▲")
        b_dn = QPushButton("▼")
        btns.addWidget(b_add)
        btns.addWidget(b_edit)
        btns.addWidget(b_del)
        btns.addStretch(1)
        btns.addWidget(b_up)
        btns.addWidget(b_dn)
        l.addLayout(btns)

        b_add.clicked.connect(self._add_condition)
        b_edit.clicked.connect(self._edit_selected_condition)
        b_del.clicked.connect(self._delete_selected_condition)
        b_up.clicked.connect(lambda: self._move_selected_condition(-1))
        b_dn.clicked.connect(lambda: self._move_selected_condition(+1))

        self.tabs.addTab(w, "Conditions")

    def _build_tab_actions(self):
        w = QWidget()
        l = QVBoxLayout(w)

        self.actions_preview = QPlainTextEdit()
        self.actions_preview.setReadOnly(True)
        self.actions_preview.setPlaceholderText("Actions preview appears here.")
        l.addWidget(self.actions_preview, 1)

        btns = QHBoxLayout()
        b_edit = QPushButton("Edit Actions…")
        b_add = QPushButton("Quick Add Action…")
        btns.addWidget(b_edit)
        btns.addWidget(b_add)
        btns.addStretch(1)
        l.addLayout(btns)

        b_edit.clicked.connect(self._edit_actions_structured)
        b_add.clicked.connect(self._add_action_quick)

        self.tabs.addTab(w, "Actions")

    def _build_tab_raw(self):
        w = QWidget()
        l = QVBoxLayout(w)

        self.raw_edit = QPlainTextEdit()
        l.addWidget(self.raw_edit, 1)

        btns = QHBoxLayout()
        b_apply = QPushButton("Apply To Form")
        b_rebuild = QPushButton("Rebuild From Form")
        b_format = QPushButton("Format JSON")
        btns.addWidget(b_apply)
        btns.addWidget(b_rebuild)
        btns.addStretch(1)
        btns.addWidget(b_format)
        l.addLayout(btns)

        b_apply.clicked.connect(self._apply_raw_to_form)
        b_rebuild.clicked.connect(self._rebuild_raw_from_form)
        b_format.clicked.connect(self._format_raw_json)

        self.tabs.addTab(w, "Raw JSON")

    # -------------------- Trigger --------------------

    def _update_trigger_field_enables(self, t: str):
        t = (t or "").strip().lower()
        needs_npc = t in ("talk_to", "npc_interaction", "dialogue_closed", "dialogue_dismissed")
        needs_room = t == "enter_room"
        needs_quest = t == "quest_stage_complete"
        needs_action = t == "player_action"
        allows_item = t in ("player_action", "item_acquired")
        allows_enemies = t in ("encounter_victory", "encounter_defeat", "encounter_retreat")
        self.trig_npc_combo.setEnabled(needs_npc)
        self.trig_room_combo.setEnabled(needs_room)
        self.trig_quest_combo.setEnabled(needs_quest)
        self.trig_action_combo.setEnabled(needs_action)
        self.trig_item_combo.setEnabled(allows_item)
        self.trig_enemies_edit.setEnabled(allows_enemies)

    def _trigger_from_form(self, *, validate: bool) -> Optional[dict]:
        t = self.trig_type_combo.currentText().strip()
        if not t:
            if validate:
                QMessageBox.warning(self, "Missing Type", "Trigger type is required.")
            return None
        base: Dict[str, Any] = {"type": t}

        t_low = t.lower()
        npc = self.trig_npc_combo.currentText().strip()
        room = self.trig_room_combo.currentText().strip()
        quest_id = self.trig_quest_combo.currentText().strip()
        action = self.trig_action_combo.currentText().strip()
        item_id = self.trig_item_combo.currentText().strip()
        enemies_raw = self.trig_enemies_edit.text().strip()

        if t_low in ("talk_to", "npc_interaction", "dialogue_closed", "dialogue_dismissed"):
            if validate and not npc:
                QMessageBox.warning(self, "Missing NPC", f"Trigger '{t}' requires an npc id.")
                return None
            if npc:
                base["npc"] = npc

        if t_low == "enter_room":
            if validate and not room:
                QMessageBox.warning(self, "Missing Room", "enter_room requires a room id.")
                return None
            if room:
                base["room_id"] = room

        if t_low == "quest_stage_complete":
            if validate and not quest_id:
                QMessageBox.warning(self, "Missing Quest", "quest_stage_complete requires quest_id.")
                return None
            if quest_id:
                base["quest_id"] = quest_id

        if t_low == "player_action":
            if validate and not action:
                QMessageBox.warning(self, "Missing Action", "player_action requires an action id.")
                return None
            if action:
                base["action"] = action
            if item_id:
                base["item_id"] = item_id
        elif t_low == "item_acquired":
            if item_id:
                base["item_id"] = item_id

        if t_low in ("encounter_victory", "encounter_defeat", "encounter_retreat"):
            if enemies_raw:
                base["enemies"] = [e.strip() for e in enemies_raw.split(",") if e.strip()]

        return base

    # -------------------- Conditions --------------------

    def _cond_summary(self, cond: dict) -> str:
        t = str(cond.get("type") or "").strip()
        tl = t.lower()
        if tl in ("milestone_set", "milestone_not_set"):
            return f"{t}:{cond.get('milestone') or ''}"
        if tl.startswith("quest_") and tl not in ("quest_stage", "quest_stage_not", "quest_task_done", "quest_task_not_done"):
            return f"{t}:{cond.get('quest_id') or ''}"
        if tl in ("quest_stage", "quest_stage_not"):
            q = str(cond.get("quest_id") or "")
            s = str(cond.get("stage_id") or "")
            return f"{t}:{q}:{s}".strip(":")
        if tl in ("quest_task_done", "quest_task_not_done"):
            q = str(cond.get("quest_id") or "")
            task = str(cond.get("task_id") or "")
            return f"{t}:{q}:{task}".strip(":")
        if tl in ("event_completed", "event_not_completed"):
            return f"{t}:{cond.get('event_id') or ''}"
        if tl in ("tutorial_completed", "tutorial_not_completed"):
            return f"{t}:{cond.get('tutorial_id') or ''}"
        if tl in ("item", "item_not"):
            item_id = cond.get("item_id") or cond.get("item") or ""
            qty = cond.get("quantity")
            return f"{t}:{item_id} x{qty}" if qty else f"{t}:{item_id}"
        return t

    def _refresh_conditions_list(self):
        self.cond_list.blockSignals(True)
        try:
            sel = self.cond_list.currentRow()
            self.cond_list.clear()
            for i, cond in enumerate(self._conditions):
                if not isinstance(cond, dict):
                    continue
                it = QListWidgetItem(self._cond_summary(cond))
                it.setData(Qt.UserRole, i)
                self.cond_list.addItem(it)
            if 0 <= sel < self.cond_list.count():
                self.cond_list.setCurrentRow(sel)
            elif self.cond_list.count() > 0:
                self.cond_list.setCurrentRow(0)
        finally:
            self.cond_list.blockSignals(False)
        self._update_summary()

    def _selected_condition_index(self) -> int:
        it = self.cond_list.currentItem()
        idx = it.data(Qt.UserRole) if it else None
        return int(idx) if isinstance(idx, int) else -1

    def _add_condition(self):
        dialog = EventConditionEditDialog(
            self,
            data={"type": "quest_active"},
            quest_ids=self._quest_ids,
            milestone_ids=self._milestone_ids,
            item_ids=self._item_ids,
            event_ids=self._event_ids,
            tutorial_ids=self._tutorial_ids,
            quest_stage_keys=self._quest_stage_keys,
            quest_task_keys=self._quest_task_keys,
        )
        if dialog.exec_() != QDialog.Accepted:
            return
        self._conditions.append(dialog.value())
        self._refresh_conditions_list()
        self.cond_list.setCurrentRow(self.cond_list.count() - 1)

    def _edit_selected_condition(self):
        idx = self._selected_condition_index()
        if not (0 <= idx < len(self._conditions)):
            return
        dialog = EventConditionEditDialog(
            self,
            data=self._conditions[idx],
            quest_ids=self._quest_ids,
            milestone_ids=self._milestone_ids,
            item_ids=self._item_ids,
            event_ids=self._event_ids,
            tutorial_ids=self._tutorial_ids,
            quest_stage_keys=self._quest_stage_keys,
            quest_task_keys=self._quest_task_keys,
        )
        if dialog.exec_() != QDialog.Accepted:
            return
        self._conditions[idx] = dialog.value()
        self._refresh_conditions_list()
        self.cond_list.setCurrentRow(idx)

    def _delete_selected_condition(self):
        idx = self._selected_condition_index()
        if not (0 <= idx < len(self._conditions)):
            return
        self._conditions.pop(idx)
        self._refresh_conditions_list()

    def _move_selected_condition(self, delta: int):
        idx = self._selected_condition_index()
        if not (0 <= idx < len(self._conditions)):
            return
        new_idx = idx + delta
        if not (0 <= new_idx < len(self._conditions)):
            return
        self._conditions[idx], self._conditions[new_idx] = self._conditions[new_idx], self._conditions[idx]
        self._refresh_conditions_list()
        self.cond_list.setCurrentRow(new_idx)

    # -------------------- Actions --------------------

    def _refresh_actions_preview(self):
        self.actions_preview.setPlainText(json.dumps(self._actions, ensure_ascii=False, indent=2))
        self._update_summary()

    def _add_action_quick(self):
        dialog = EventActionQuickAdd(
            self,
            quest_ids=self._quest_ids,
            milestone_ids=self._milestone_ids,
            item_ids=self._item_ids,
            npc_ids=self._npc_ids,
            room_ids=self._room_ids,
            cinematic_ids=self._cinematic_ids,
            tutorial_ids=self._tutorial_ids,
            quest_stage_keys=self._quest_stage_keys,
            quest_task_keys=self._quest_task_keys,
            player_action_ids=self._player_action_ids,
        )
        if dialog.exec_() != QDialog.Accepted:
            return
        act = dialog.value()
        if isinstance(act, dict):
            self._actions.append(act)
            self._refresh_actions_preview()

    def _edit_actions_structured(self):
        dialog = EventActionsStructuredEdit(
            self,
            actions=self._actions,
            quest_ids=self._quest_ids,
            milestone_ids=self._milestone_ids,
            item_ids=self._item_ids,
            room_ids=self._room_ids,
            cinematic_ids=self._cinematic_ids,
            tutorial_ids=self._tutorial_ids,
            quest_stage_keys=self._quest_stage_keys,
            quest_task_keys=self._quest_task_keys,
            player_action_ids=self._player_action_ids,
        )
        if dialog.exec_() != QDialog.Accepted:
            return
        self._actions = dialog.value()
        self._refresh_actions_preview()

    # -------------------- Raw JSON --------------------

    def _format_raw_json(self):
        try:
            obj = json.loads(self.raw_edit.toPlainText() or "{}")
        except Exception as exc:
            QMessageBox.warning(self, "Format JSON", f"Raw JSON is invalid:\n\n{exc}")
            return
        self.raw_edit.setPlainText(json.dumps(obj, ensure_ascii=False, indent=2))

    def _rebuild_raw_from_form(self):
        self.raw_edit.setPlainText(json.dumps(self._event_from_form(validate=False) or {}, ensure_ascii=False, indent=2))

    def _apply_raw_to_form(self):
        try:
            obj = json.loads(self.raw_edit.toPlainText() or "{}")
        except Exception as exc:
            QMessageBox.warning(self, "Apply Raw JSON", f"Raw JSON must be valid.\n\n{exc}")
            return
        if not isinstance(obj, dict):
            QMessageBox.warning(self, "Apply Raw JSON", "Raw JSON must be an object.")
            return
        self._data = obj
        self._load_from_data(self._data)

    # -------------------- Load/save --------------------

    def _load_from_data(self, data: dict):
        # Overview
        self.id_edit.setText(_ensure_str(data.get("id")))
        self.desc_edit.setPlainText(_ensure_str(data.get("description")))
        self.repeatable_chk.setChecked(bool(data.get("repeatable", False)))
        self.on_msg_edit.setText(_ensure_str(data.get("on_message")))
        self.off_msg_edit.setText(_ensure_str(data.get("off_message")))

        # Trigger
        trigger = data.get("trigger")
        if not isinstance(trigger, dict):
            trigger = {"type": "player_action", "action": "TODO"}
        self.trig_type_combo.setCurrentText(str(trigger.get("type") or "").strip())
        self.trig_npc_combo.setCurrentText(str(trigger.get("npc") or "").strip())
        self.trig_room_combo.setCurrentText(str(trigger.get("room_id") or trigger.get("room") or "").strip())
        self.trig_quest_combo.setCurrentText(str(trigger.get("quest_id") or trigger.get("questId") or "").strip())
        self.trig_action_combo.setCurrentText(str(trigger.get("action") or "").strip())
        self.trig_item_combo.setCurrentText(str(trigger.get("item_id") or trigger.get("item") or "").strip())
        enemies = trigger.get("enemies") or []
        if isinstance(enemies, list):
            self.trig_enemies_edit.setText(", ".join([str(x) for x in enemies if str(x).strip()]))
        else:
            self.trig_enemies_edit.setText(str(enemies or ""))
        self._update_trigger_field_enables(self.trig_type_combo.currentText())

        # Conditions
        conditions = data.get("conditions") or []
        if not isinstance(conditions, list):
            conditions = []
        self._conditions = [c for c in copy.deepcopy(conditions) if isinstance(c, dict)]
        self._refresh_conditions_list()

        # Actions
        actions = data.get("actions") or []
        if not isinstance(actions, list):
            actions = []
        self._actions = [a for a in copy.deepcopy(actions) if isinstance(a, dict)]
        self._refresh_actions_preview()

        self._update_summary()

    def _event_from_form(self, *, validate: bool) -> Optional[dict]:
        eid = self.id_edit.text().strip()
        if validate:
            if not eid:
                QMessageBox.warning(self, "Missing ID", "Event ID is required.")
                return None
            if eid != self._orig_id and eid in self._existing:
                QMessageBox.warning(self, "Duplicate ID", f"Event '{eid}' already exists.")
                return None

        trigger = self._trigger_from_form(validate=validate)
        if trigger is None:
            return None

        if validate:
            if any(not isinstance(a, dict) for a in self._actions):
                QMessageBox.warning(self, "Invalid Actions", "Actions must be a list of objects.")
                return None
            if any(not isinstance(c, dict) for c in self._conditions):
                QMessageBox.warning(self, "Invalid Conditions", "Conditions must be a list of objects.")
                return None

        base = dict(self._data)
        base["id"] = eid
        base["description"] = self.desc_edit.toPlainText().strip()
        base["trigger"] = trigger
        base["repeatable"] = bool(self.repeatable_chk.isChecked())
        base["actions"] = copy.deepcopy(self._actions)
        base["conditions"] = copy.deepcopy(self._conditions)

        on_msg = self.on_msg_edit.text().strip()
        if on_msg:
            base["on_message"] = on_msg
        else:
            base.pop("on_message", None)

        off_msg = self.off_msg_edit.text().strip()
        if off_msg:
            base["off_message"] = off_msg
        else:
            base.pop("off_message", None)

        return base

    def _update_summary(self):
        try:
            t = self.trig_type_combo.currentText().strip()
        except Exception:
            t = ""
        self.summary_label.setText(f"trigger={t or '(none)'} | conditions={len(self._conditions)} | actions={len(self._actions)}")

    def _accept(self):
        base = self._event_from_form(validate=True)
        if base is None:
            return
        self._data = base
        self.accept()

    def value(self) -> dict:
        return dict(self._data)


class EventTriggerQuickEdit(QDialog):
    _TRIGGER_TYPES: Tuple[str, ...] = EVENT_TRIGGER_TYPES

    def __init__(
        self,
        parent: QWidget,
        *,
        data: Optional[dict] = None,
        npc_ids: Sequence[str] = (),
        room_ids: Sequence[str] = (),
        quest_ids: Sequence[str] = (),
        item_ids: Sequence[str] = (),
        player_action_ids: Sequence[str] = (),
    ):
        super().__init__(parent)
        self.setWindowTitle("Build Trigger")
        self.resize(640, 360)
        self._data = dict(data) if isinstance(data, dict) else {}

        layout = QVBoxLayout(self)
        form = QFormLayout()

        self.type_combo = QComboBox()
        self.type_combo.addItems(list(self._TRIGGER_TYPES))
        self.type_combo.setCurrentText(str(self._data.get("type") or "").strip())

        self.npc_combo = QComboBox()
        self.npc_combo.setEditable(True)
        self.npc_combo.setInsertPolicy(QComboBox.NoInsert)
        npc_vals = [""] + list(npc_ids)
        self.npc_combo.addItems(npc_vals)
        self.npc_combo.setCurrentText(str(self._data.get("npc") or "").strip())
        self.npc_combo.setCompleter(_make_completer(npc_vals))

        self.room_combo = QComboBox()
        self.room_combo.setEditable(True)
        self.room_combo.setInsertPolicy(QComboBox.NoInsert)
        room_vals = [""] + list(room_ids)
        self.room_combo.addItems(room_vals)
        self.room_combo.setCurrentText(str(self._data.get("room_id") or self._data.get("room") or "").strip())
        self.room_combo.setCompleter(_make_completer(room_vals))

        self.quest_combo = QComboBox()
        self.quest_combo.setEditable(True)
        self.quest_combo.setInsertPolicy(QComboBox.NoInsert)
        quest_vals = [""] + list(quest_ids)
        self.quest_combo.addItems(quest_vals)
        self.quest_combo.setCurrentText(str(self._data.get("quest_id") or "").strip())
        self.quest_combo.setCompleter(_make_completer(quest_vals))

        self.action_combo = QComboBox()
        self.action_combo.setEditable(True)
        self.action_combo.setInsertPolicy(QComboBox.NoInsert)
        action_vals = [""] + list(player_action_ids)
        self.action_combo.addItems(action_vals)
        self.action_combo.setCurrentText(str(self._data.get("action") or "").strip())
        self.action_combo.setCompleter(_make_completer(action_vals))

        self.item_combo = QComboBox()
        self.item_combo.setEditable(True)
        self.item_combo.setInsertPolicy(QComboBox.NoInsert)
        item_vals = [""] + list(item_ids)
        self.item_combo.addItems(item_vals)
        self.item_combo.setCurrentText(str(self._data.get("item_id") or self._data.get("item") or "").strip())
        self.item_combo.setCompleter(_make_completer(item_vals))

        self.enemies_edit = QLineEdit()
        enemies = self._data.get("enemies") or []
        if isinstance(enemies, list):
            self.enemies_edit.setText(", ".join([str(x) for x in enemies if str(x).strip()]))
        else:
            self.enemies_edit.setText(str(enemies or ""))

        form.addRow("Type:", self.type_combo)
        form.addRow("NPC (talk/dialogue):", self.npc_combo)
        form.addRow("Room (enter_room):", self.room_combo)
        form.addRow("Quest (quest_stage_complete):", self.quest_combo)
        form.addRow("Action (player_action):", self.action_combo)
        form.addRow("Item (optional):", self.item_combo)
        form.addRow("Enemies (optional list):", self.enemies_edit)
        layout.addLayout(form)

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self._accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

        self.type_combo.currentTextChanged.connect(self._update_field_enables)
        self._update_field_enables(self.type_combo.currentText())

    def _update_field_enables(self, t: str):
        t = (t or "").strip().lower()
        needs_npc = t in ("talk_to", "npc_interaction", "dialogue_closed", "dialogue_dismissed")
        needs_room = t == "enter_room"
        needs_quest = t == "quest_stage_complete"
        needs_action = t == "player_action"
        allows_item = t in ("player_action", "item_acquired")
        allows_enemies = t in ("encounter_victory", "encounter_defeat", "encounter_retreat")
        self.npc_combo.setEnabled(needs_npc)
        self.room_combo.setEnabled(needs_room)
        self.quest_combo.setEnabled(needs_quest)
        self.action_combo.setEnabled(needs_action)
        self.item_combo.setEnabled(allows_item)
        self.enemies_edit.setEnabled(allows_enemies)

    def _accept(self):
        t = self.type_combo.currentText().strip()
        if not t:
            QMessageBox.warning(self, "Missing Type", "Trigger type is required.")
            return
        base = dict(self._data)
        base["type"] = t

        t_low = t.lower()
        npc = self.npc_combo.currentText().strip()
        room = self.room_combo.currentText().strip()
        quest_id = self.quest_combo.currentText().strip()
        action = self.action_combo.currentText().strip()
        item_id = self.item_combo.currentText().strip()
        enemies_raw = self.enemies_edit.text().strip()

        if t_low in ("talk_to", "npc_interaction", "dialogue_closed", "dialogue_dismissed"):
            if not npc:
                QMessageBox.warning(self, "Missing NPC", f"Trigger '{t}' requires an npc id.")
                return
            base["npc"] = npc
        else:
            base.pop("npc", None)

        if t_low == "enter_room":
            if not room:
                QMessageBox.warning(self, "Missing Room", "enter_room requires a room id.")
                return
            base["room_id"] = room
        else:
            base.pop("room_id", None)
            base.pop("room", None)

        if t_low == "quest_stage_complete":
            if not quest_id:
                QMessageBox.warning(self, "Missing Quest", "quest_stage_complete requires quest_id.")
                return
            base["quest_id"] = quest_id
        else:
            base.pop("quest_id", None)

        if t_low == "player_action":
            if not action:
                QMessageBox.warning(self, "Missing Action", "player_action requires an action id.")
                return
            base["action"] = action
            if item_id:
                base["item_id"] = item_id
            else:
                base.pop("item_id", None)
                base.pop("item", None)
        elif t_low == "item_acquired":
            if item_id:
                base["item_id"] = item_id
            else:
                base.pop("item_id", None)
                base.pop("item", None)
            base.pop("action", None)
        else:
            base.pop("action", None)
            base.pop("item_id", None)
            base.pop("item", None)

        if t_low in ("encounter_victory", "encounter_defeat", "encounter_retreat"):
            if enemies_raw:
                enemies = [e.strip() for e in enemies_raw.split(",") if e.strip()]
                base["enemies"] = enemies
            else:
                base.pop("enemies", None)
        else:
            base.pop("enemies", None)

        self._data = base
        self.accept()

    def value(self) -> dict:
        return dict(self._data)


class EventActionQuickAdd(QDialog):
    _ACTION_TYPES: Tuple[str, ...] = (
        "show_message",
        "narrate",
        "play_cinematic",
        "trigger_cutscene",
        "system_tutorial",
        "start_quest",
        "complete_quest",
        "fail_quest",
        "track_quest",
        "untrack_quest",
        "advance_quest",
        "advance_quest_if_active",
        "set_milestone",
        "clear_milestone",
        "set_quest_task_done",
        "advance_quest_stage",
        "give_item",
        "give_item_to_player",
        "take_item",
        "reveal_hidden_item",
        "spawn_item_on_ground",
        "give_xp",
        "spawn_encounter",
        "begin_node",
        "unlock_room_search",
        "player_action",
        "set_room_state",
        "toggle_room_state",
        "rebuild_ui",
        "wait_for_draw",
        "if_quest_active",
        "if_quest_not_started",
        "if_quest_completed",
        "if_quest_not_completed",
        "if_quest_failed",
        "if_milestone_set",
        "if_milestone_not_set",
        "if_milestones_set",
    )

    def __init__(
        self,
        parent: QWidget,
        *,
        quest_ids: Sequence[str] = (),
        milestone_ids: Sequence[str] = (),
        item_ids: Sequence[str] = (),
        npc_ids: Sequence[str] = (),
        room_ids: Sequence[str] = (),
        cinematic_ids: Sequence[str] = (),
        tutorial_ids: Sequence[str] = (),
        quest_stage_keys: Sequence[str] = (),
        quest_task_keys: Sequence[str] = (),
        player_action_ids: Sequence[str] = (),
    ):
        super().__init__(parent)
        self.setWindowTitle("Add Action")
        self.resize(720, 520)
        self._data: dict = {}

        self._quest_stage_keys = list(quest_stage_keys)
        self._quest_task_keys = list(quest_task_keys)

        layout = QVBoxLayout(self)
        form = QFormLayout()

        self.type_combo = QComboBox()
        self.type_combo.addItems(list(self._ACTION_TYPES))

        self.message_edit = QPlainTextEdit()
        self.tap_chk = QCheckBox("Tap to dismiss (narrate)")

        self.quest_combo = QComboBox()
        self.quest_combo.setEditable(True)
        self.quest_combo.setInsertPolicy(QComboBox.NoInsert)
        quest_vals = [""] + list(quest_ids)
        self.quest_combo.addItems(quest_vals)
        self.quest_combo.setCompleter(_make_completer(quest_vals))

        self.milestone_combo = QComboBox()
        self.milestone_combo.setEditable(True)
        self.milestone_combo.setInsertPolicy(QComboBox.NoInsert)
        ms_vals = [""] + list(milestone_ids)
        self.milestone_combo.addItems(ms_vals)
        self.milestone_combo.setCompleter(_make_completer(ms_vals))

        self.milestones_edit = QLineEdit()
        self.milestones_edit.setPlaceholderText("comma-separated milestone ids")

        self.quest_stage_combo = QComboBox()
        self.quest_stage_combo.setEditable(True)
        self.quest_stage_combo.setInsertPolicy(QComboBox.NoInsert)
        qs_vals = [""] + list(self._quest_stage_keys)
        self.quest_stage_combo.addItems(qs_vals)
        self.quest_stage_combo.setCompleter(_make_completer(qs_vals))

        self.quest_task_combo = QComboBox()
        self.quest_task_combo.setEditable(True)
        self.quest_task_combo.setInsertPolicy(QComboBox.NoInsert)
        qt_vals = [""] + list(self._quest_task_keys)
        self.quest_task_combo.addItems(qt_vals)
        self.quest_task_combo.setCompleter(_make_completer(qt_vals))

        self.item_combo = QComboBox()
        self.item_combo.setEditable(True)
        self.item_combo.setInsertPolicy(QComboBox.NoInsert)
        item_vals = [""] + list(item_ids)
        self.item_combo.addItems(item_vals)
        self.item_combo.setCompleter(_make_completer(item_vals))
        self.qty_edit = QLineEdit("1")

        self.cinematic_combo = QComboBox()
        self.cinematic_combo.setEditable(True)
        self.cinematic_combo.setInsertPolicy(QComboBox.NoInsert)
        cine_vals = [""] + list(cinematic_ids)
        self.cinematic_combo.addItems(cine_vals)
        self.cinematic_combo.setCompleter(_make_completer(cine_vals))

        self.tutorial_combo = QComboBox()
        self.tutorial_combo.setEditable(True)
        self.tutorial_combo.setInsertPolicy(QComboBox.NoInsert)
        tut_vals = [""] + list(tutorial_ids)
        self.tutorial_combo.addItems(tut_vals)
        self.tutorial_combo.setCompleter(_make_completer(tut_vals))
        self.context_edit = QLineEdit()

        self.action_combo = QComboBox()
        self.action_combo.setEditable(True)
        self.action_combo.setInsertPolicy(QComboBox.NoInsert)
        act_vals = [""] + list(player_action_ids)
        self.action_combo.addItems(act_vals)
        self.action_combo.setCompleter(_make_completer(act_vals))
        self.action_item_combo = QComboBox()
        self.action_item_combo.setEditable(True)
        self.action_item_combo.setInsertPolicy(QComboBox.NoInsert)
        self.action_item_combo.addItems(item_vals)
        self.action_item_combo.setCompleter(_make_completer(item_vals))

        self.room_combo = QComboBox()
        self.room_combo.setEditable(True)
        self.room_combo.setInsertPolicy(QComboBox.NoInsert)
        room_vals = [""] + list(room_ids)
        self.room_combo.addItems(room_vals)
        self.room_combo.setCompleter(_make_completer(room_vals))
        self.state_key_edit = QLineEdit()
        self.state_value_chk = QCheckBox("Value (checked = true)")
        self.encounter_edit = QLineEdit()
        self.xp_edit = QLineEdit()
        self.include_else_chk = QCheckBox("Include else branch (empty)")

        form.addRow("Type:", self.type_combo)
        form.addRow("Message/Text:", self.message_edit)
        form.addRow("", self.tap_chk)
        form.addRow("Quest ID:", self.quest_combo)
        form.addRow("Milestone:", self.milestone_combo)
        form.addRow("Milestones (list):", self.milestones_edit)
        form.addRow("Quest Stage (quest:stage):", self.quest_stage_combo)
        form.addRow("Quest Task (quest:task):", self.quest_task_combo)
        form.addRow("Item ID:", self.item_combo)
        form.addRow("Quantity:", self.qty_edit)
        form.addRow("Cinematic Scene ID:", self.cinematic_combo)
        form.addRow("Tutorial Script ID:", self.tutorial_combo)
        form.addRow("Tutorial Context:", self.context_edit)
        form.addRow("Player Action ID:", self.action_combo)
        form.addRow("Player Action Item (optional):", self.action_item_combo)
        form.addRow("Room ID:", self.room_combo)
        form.addRow("Room State Key:", self.state_key_edit)
        form.addRow("", self.state_value_chk)
        form.addRow("Encounter ID:", self.encounter_edit)
        form.addRow("XP:", self.xp_edit)
        form.addRow("", self.include_else_chk)
        layout.addLayout(form)

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self._accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

        self.type_combo.currentTextChanged.connect(self._update_field_enables)
        self._update_field_enables(self.type_combo.currentText())

    def _update_field_enables(self, t: str):
        t = (t or "").strip().lower()
        is_message = t in ("show_message", "narrate", "unlock_room_search")
        is_quest = t in (
            "start_quest",
            "complete_quest",
            "fail_quest",
            "track_quest",
            "advance_quest",
            "advance_quest_if_active",
        ) or t.startswith("if_quest_")
        is_milestone = t in ("set_milestone", "clear_milestone", "if_milestone_set", "if_milestone_not_set")
        is_milestones_list = t == "if_milestones_set"
        is_task = t == "set_quest_task_done"
        is_stage = t == "advance_quest_stage"
        is_item = t in ("give_item", "give_item_to_player", "take_item", "reveal_hidden_item", "spawn_item_on_ground")
        is_cinematic = t in ("play_cinematic", "trigger_cutscene")
        is_tutorial = t == "system_tutorial"
        is_player_action = t == "player_action"
        is_room_state = t in ("set_room_state", "toggle_room_state")
        is_spawn_encounter = t == "spawn_encounter"
        is_give_xp = t == "give_xp"
        is_begin_node = t == "begin_node"
        is_conditional = t.startswith("if_")

        self.message_edit.setEnabled(is_message)
        self.tap_chk.setEnabled(t == "narrate")
        self.quest_combo.setEnabled(is_quest or is_task or is_stage)
        self.milestone_combo.setEnabled(is_milestone)
        self.milestones_edit.setEnabled(is_milestones_list)
        self.quest_task_combo.setEnabled(is_task)
        self.quest_stage_combo.setEnabled(is_stage)
        self.item_combo.setEnabled(is_item)
        self.qty_edit.setEnabled(is_item)
        self.cinematic_combo.setEnabled(is_cinematic)
        self.tutorial_combo.setEnabled(is_tutorial)
        self.context_edit.setEnabled(is_tutorial)
        self.action_combo.setEnabled(is_player_action)
        self.action_item_combo.setEnabled(is_player_action)
        self.room_combo.setEnabled(is_room_state)
        self.state_key_edit.setEnabled(is_room_state)
        self.state_value_chk.setEnabled(t == "set_room_state")
        self.encounter_edit.setEnabled(is_spawn_encounter)
        self.xp_edit.setEnabled(is_give_xp)
        self.include_else_chk.setEnabled(is_conditional)
        if is_begin_node or is_spawn_encounter or t in ("unlock_room_search", "reveal_hidden_item", "spawn_item_on_ground"):
            self.room_combo.setEnabled(True)

    def _accept(self):
        t = self.type_combo.currentText().strip()
        if not t:
            QMessageBox.warning(self, "Missing Type", "Action type is required.")
            return
        t_low = t.lower()
        data: Dict[str, Any] = {"type": t}

        if t_low in ("show_message", "narrate"):
            msg = self.message_edit.toPlainText().strip()
            if not msg:
                QMessageBox.warning(self, "Missing Text", f"Action '{t}' requires text.")
                return
            data["message"] = msg
            if t_low == "narrate":
                data["tap_to_dismiss"] = bool(self.tap_chk.isChecked())

        if t_low == "unlock_room_search":
            note = self.message_edit.toPlainText().strip()
            if note:
                data["note"] = note
            room_id = self.room_combo.currentText().strip()
            if not room_id:
                QMessageBox.warning(self, "Missing Room", "unlock_room_search requires a room id.")
                return
            data["room_id"] = room_id

        if t_low in ("play_cinematic", "trigger_cutscene"):
            scene_id = self.cinematic_combo.currentText().strip()
            if not scene_id:
                QMessageBox.warning(self, "Missing Scene", f"Action '{t}' requires a cinematic scene id.")
                return
            data["scene_id"] = scene_id

        if t_low == "system_tutorial":
            scene_id = self.tutorial_combo.currentText().strip()
            if scene_id:
                data["scene_id"] = scene_id
            ctx = self.context_edit.text().strip()
            if ctx:
                data["context"] = ctx

        if t_low in ("start_quest", "complete_quest", "fail_quest", "track_quest", "advance_quest", "advance_quest_if_active"):
            qid = self.quest_combo.currentText().strip()
            if not qid:
                QMessageBox.warning(self, "Missing Quest", f"Action '{t}' requires quest_id.")
                return
            data["quest_id"] = qid

        if t_low in ("set_milestone", "clear_milestone"):
            mid = self.milestone_combo.currentText().strip()
            if not mid:
                QMessageBox.warning(self, "Missing Milestone", f"Action '{t}' requires a milestone id.")
                return
            data["milestone"] = mid

        if t_low == "set_quest_task_done":
            qt = self.quest_task_combo.currentText().strip()
            parts = qt.split(":", 1)
            qid = parts[0].strip() if parts else ""
            tid = parts[1].strip() if len(parts) > 1 else ""
            if not qid or not tid:
                QMessageBox.warning(self, "Missing Task", "Pick a quest:task value for set_quest_task_done.")
                return
            data["quest_id"] = qid
            data["task_id"] = tid

        if t_low == "advance_quest_stage":
            qs = self.quest_stage_combo.currentText().strip()
            parts = qs.split(":", 1)
            qid = parts[0].strip() if parts else ""
            sid = parts[1].strip() if len(parts) > 1 else ""
            if not qid or not sid:
                QMessageBox.warning(self, "Missing Stage", "Pick a quest:stage value for advance_quest_stage.")
                return
            data["quest_id"] = qid
            data["to_stage_id"] = sid

        if t_low in ("give_item", "give_item_to_player", "take_item"):
            item_id = self.item_combo.currentText().strip()
            if not item_id:
                QMessageBox.warning(self, "Missing Item", f"Action '{t}' requires an item id.")
                return
            try:
                qty = int(float(self.qty_edit.text().strip() or "1"))
            except Exception:
                qty = 1
            data["item_id"] = item_id
            data["quantity"] = max(1, qty)

        if t_low in ("reveal_hidden_item", "spawn_item_on_ground"):
            item_id = self.item_combo.currentText().strip()
            if not item_id:
                QMessageBox.warning(self, "Missing Item", f"Action '{t}' requires an item id.")
                return
            try:
                qty = int(float(self.qty_edit.text().strip() or "1"))
            except Exception:
                qty = 1
            data["item_id"] = item_id
            data["quantity"] = max(1, qty)
            room_id = self.room_combo.currentText().strip()
            if room_id:
                data["room_id"] = room_id

        if t_low == "give_xp":
            try:
                xp = int(float(self.xp_edit.text().strip() or "0"))
            except Exception:
                xp = 0
            if xp <= 0:
                QMessageBox.warning(self, "Missing XP", "give_xp requires a positive xp amount.")
                return
            data["xp"] = xp

        if t_low == "spawn_encounter":
            enc = self.encounter_edit.text().strip()
            if not enc:
                QMessageBox.warning(self, "Missing Encounter", "spawn_encounter requires an encounter_id.")
                return
            data["encounter_id"] = enc
            room_id = self.room_combo.currentText().strip()
            if room_id:
                data["room_id"] = room_id

        if t_low == "begin_node":
            room_id = self.room_combo.currentText().strip()
            if not room_id:
                QMessageBox.warning(self, "Missing Room", "begin_node requires a room id.")
                return
            data["room_id"] = room_id

        if t_low == "player_action":
            action_id = self.action_combo.currentText().strip()
            if not action_id:
                QMessageBox.warning(self, "Missing Action", "player_action requires an action id.")
                return
            data["action"] = action_id
            item_id = self.action_item_combo.currentText().strip()
            if item_id:
                data["item_id"] = item_id

        if t_low in ("set_room_state", "toggle_room_state"):
            state_key = self.state_key_edit.text().strip()
            if not state_key:
                QMessageBox.warning(self, "Missing State Key", f"Action '{t}' requires a state_key.")
                return
            room_id = self.room_combo.currentText().strip()
            if room_id:
                data["room_id"] = room_id
            data["state_key"] = state_key
            if t_low == "set_room_state":
                data["value"] = bool(self.state_value_chk.isChecked())

        if t_low.startswith("if_"):
            qid = self.quest_combo.currentText().strip()
            mid = self.milestone_combo.currentText().strip()
            milestones_raw = self.milestones_edit.text().strip()
            if "quest" in t_low:
                if not qid:
                    QMessageBox.warning(self, "Missing Quest", f"Action '{t}' requires quest_id.")
                    return
                data["quest_id"] = qid
            if "milestones" in t_low:
                milestones = [m.strip() for m in milestones_raw.split(",") if m.strip()]
                if not milestones:
                    QMessageBox.warning(self, "Missing Milestones", f"Action '{t}' requires milestones list.")
                    return
                data["milestones"] = milestones
            elif "milestone" in t_low:
                if not mid:
                    QMessageBox.warning(self, "Missing Milestone", f"Action '{t}' requires milestone.")
                    return
                data["milestone"] = mid
            data["do"] = []
            if self.include_else_chk.isChecked():
                data["elseDo"] = []

        self._data = data
        self.accept()

    def value(self) -> dict:
        return dict(self._data)


class EventActionEditDialog(QDialog):
    def __init__(
        self,
        parent: QWidget,
        *,
        data: Optional[dict] = None,
        quest_ids: Sequence[str] = (),
        milestone_ids: Sequence[str] = (),
        item_ids: Sequence[str] = (),
        room_ids: Sequence[str] = (),
        cinematic_ids: Sequence[str] = (),
        tutorial_ids: Sequence[str] = (),
        quest_stage_keys: Sequence[str] = (),
        quest_task_keys: Sequence[str] = (),
        player_action_ids: Sequence[str] = (),
    ):
        super().__init__(parent)
        self.setWindowTitle("Edit Action")
        self.resize(760, 620)
        self._data = dict(data) if isinstance(data, dict) else {}

        layout = QVBoxLayout(self)

        self.tabs = QTabWidget()
        layout.addWidget(self.tabs, 1)

        # --- Form tab ---
        form_tab = QWidget()
        form_l = QVBoxLayout(form_tab)
        form = QFormLayout()

        self.type_combo = QComboBox()
        self.type_combo.setEditable(True)
        self.type_combo.setInsertPolicy(QComboBox.NoInsert)
        type_vals = list(EVENT_ACTION_TYPES)
        self.type_combo.addItems(type_vals)
        self.type_combo.setCurrentText(str(self._data.get("type") or "").strip())
        self.type_combo.setCompleter(_make_completer([""] + type_vals))

        self.message_edit = QPlainTextEdit()
        self.message_edit.setPlaceholderText("Message / narration text / note (depends on type)")
        self.message_edit.setPlainText(str(self._data.get("message") or self._data.get("text") or "").strip())
        self.tap_chk = QCheckBox("Tap to dismiss (narrate)")
        self.tap_chk.setChecked(bool(self._data.get("tap_to_dismiss") or False))

        self.quest_combo = QComboBox()
        self.quest_combo.setEditable(True)
        self.quest_combo.setInsertPolicy(QComboBox.NoInsert)
        quest_vals = [""] + list(quest_ids)
        self.quest_combo.addItems(quest_vals)
        self.quest_combo.setCurrentText(str(self._data.get("quest_id") or "").strip())
        self.quest_combo.setCompleter(_make_completer(quest_vals))

        self.milestone_combo = QComboBox()
        self.milestone_combo.setEditable(True)
        self.milestone_combo.setInsertPolicy(QComboBox.NoInsert)
        ms_vals = [""] + list(milestone_ids)
        self.milestone_combo.addItems(ms_vals)
        self.milestone_combo.setCurrentText(str(self._data.get("milestone") or "").strip())
        self.milestone_combo.setCompleter(_make_completer(ms_vals))

        milestones = self._data.get("milestones") or []
        if not isinstance(milestones, list):
            milestones = []
        self.milestones_edit = QLineEdit(", ".join([str(x) for x in milestones if str(x).strip()]))

        self.quest_stage_combo = QComboBox()
        self.quest_stage_combo.setEditable(True)
        self.quest_stage_combo.setInsertPolicy(QComboBox.NoInsert)
        qs_vals = [""] + list(quest_stage_keys)
        self.quest_stage_combo.addItems(qs_vals)
        to_stage_id = str(self._data.get("to_stage_id") or "").strip()
        # Best-effort: if quest_id+to_stage_id match a key, show it.
        qid = str(self._data.get("quest_id") or "").strip()
        self.quest_stage_combo.setCurrentText(f"{qid}:{to_stage_id}" if qid and to_stage_id else "")
        self.quest_stage_combo.setCompleter(_make_completer(qs_vals))

        self.quest_task_combo = QComboBox()
        self.quest_task_combo.setEditable(True)
        self.quest_task_combo.setInsertPolicy(QComboBox.NoInsert)
        qt_vals = [""] + list(quest_task_keys)
        self.quest_task_combo.addItems(qt_vals)
        task_id = str(self._data.get("task_id") or "").strip()
        self.quest_task_combo.setCurrentText(f"{qid}:{task_id}" if qid and task_id else "")
        self.quest_task_combo.setCompleter(_make_completer(qt_vals))

        self.item_combo = QComboBox()
        self.item_combo.setEditable(True)
        self.item_combo.setInsertPolicy(QComboBox.NoInsert)
        item_vals = [""] + list(item_ids)
        self.item_combo.addItems(item_vals)
        self.item_combo.setCurrentText(str(self._data.get("item_id") or self._data.get("item") or "").strip())
        self.item_combo.setCompleter(_make_completer(item_vals))

        self.qty_edit = QLineEdit(str(self._data.get("quantity") or "1"))

        self.cinematic_combo = QComboBox()
        self.cinematic_combo.setEditable(True)
        self.cinematic_combo.setInsertPolicy(QComboBox.NoInsert)
        cine_vals = [""] + list(cinematic_ids)
        self.cinematic_combo.addItems(cine_vals)
        self.cinematic_combo.setCurrentText(str(self._data.get("scene_id") or "").strip())
        self.cinematic_combo.setCompleter(_make_completer(cine_vals))

        self.tutorial_combo = QComboBox()
        self.tutorial_combo.setEditable(True)
        self.tutorial_combo.setInsertPolicy(QComboBox.NoInsert)
        tut_vals = [""] + list(tutorial_ids)
        self.tutorial_combo.addItems(tut_vals)
        self.tutorial_combo.setCurrentText(str(self._data.get("scene_id") or self._data.get("tutorial_id") or "").strip())
        self.tutorial_combo.setCompleter(_make_completer(tut_vals))

        self.context_edit = QLineEdit(str(self._data.get("context") or "").strip())

        self.action_combo = QComboBox()
        self.action_combo.setEditable(True)
        self.action_combo.setInsertPolicy(QComboBox.NoInsert)
        act_vals = [""] + list(player_action_ids)
        self.action_combo.addItems(act_vals)
        self.action_combo.setCurrentText(str(self._data.get("action") or "").strip())
        self.action_combo.setCompleter(_make_completer(act_vals))

        self.action_item_combo = QComboBox()
        self.action_item_combo.setEditable(True)
        self.action_item_combo.setInsertPolicy(QComboBox.NoInsert)
        self.action_item_combo.addItems(item_vals)
        self.action_item_combo.setCurrentText(str(self._data.get("item_id") or "").strip())
        self.action_item_combo.setCompleter(_make_completer(item_vals))

        self.room_combo = QComboBox()
        self.room_combo.setEditable(True)
        self.room_combo.setInsertPolicy(QComboBox.NoInsert)
        room_vals = [""] + list(room_ids)
        self.room_combo.addItems(room_vals)
        self.room_combo.setCurrentText(str(self._data.get("room_id") or "").strip())
        self.room_combo.setCompleter(_make_completer(room_vals))

        self.state_key_edit = QLineEdit(str(self._data.get("state_key") or "").strip())
        self.state_value_chk = QCheckBox("Value (checked = true)")
        self.state_value_chk.setChecked(bool(self._data.get("value") if self._data.get("value") is not None else True))

        self.encounter_edit = QLineEdit(str(self._data.get("encounter_id") or "").strip())
        self.xp_edit = QLineEdit(str(self._data.get("xp") or "").strip())

        # Audio-layer fields (optional)
        self.audio_layer_edit = QLineEdit(str(self._data.get("layer") or "").strip())
        self.audio_cue_edit = QLineEdit(str(self._data.get("cue_id") or "").strip())
        self.audio_gain_edit = QLineEdit(str(self._data.get("gain") or "").strip())
        self.audio_fade_edit = QLineEdit(str(self._data.get("fade_ms") or "").strip())
        self.audio_loop_chk = QCheckBox("Loop")
        self.audio_loop_chk.setChecked(bool(self._data.get("loop") or False))
        self.audio_stop_chk = QCheckBox("Stop")
        self.audio_stop_chk.setChecked(bool(self._data.get("stop") or False))

        form.addRow("Type:", self.type_combo)
        form.addRow("Message/Text/Note:", self.message_edit)
        form.addRow("", self.tap_chk)
        form.addRow("Quest ID:", self.quest_combo)
        form.addRow("Milestone:", self.milestone_combo)
        form.addRow("Milestones (list):", self.milestones_edit)
        form.addRow("Quest Stage (quest:stage):", self.quest_stage_combo)
        form.addRow("Quest Task (quest:task):", self.quest_task_combo)
        form.addRow("Item ID:", self.item_combo)
        form.addRow("Quantity:", self.qty_edit)
        form.addRow("Cinematic Scene ID:", self.cinematic_combo)
        form.addRow("Tutorial Script ID:", self.tutorial_combo)
        form.addRow("Tutorial Context:", self.context_edit)
        form.addRow("Player Action ID:", self.action_combo)
        form.addRow("Player Action Item (optional):", self.action_item_combo)
        form.addRow("Room ID:", self.room_combo)
        form.addRow("Room State Key:", self.state_key_edit)
        form.addRow("", self.state_value_chk)
        form.addRow("Encounter ID:", self.encounter_edit)
        form.addRow("XP:", self.xp_edit)

        audio_row = QHBoxLayout()
        audio_row.addWidget(QLabel("Layer:"))
        audio_row.addWidget(self.audio_layer_edit, 1)
        audio_row.addWidget(QLabel("Cue:"))
        audio_row.addWidget(self.audio_cue_edit, 1)
        audio_row.addWidget(QLabel("Gain:"))
        audio_row.addWidget(self.audio_gain_edit)
        audio_row.addWidget(QLabel("Fade ms:"))
        audio_row.addWidget(self.audio_fade_edit)
        audio_row.addWidget(self.audio_loop_chk)
        audio_row.addWidget(self.audio_stop_chk)
        form.addRow("Audio Layer:", _wrap_layout(audio_row))

        form_l.addLayout(form)
        self.tabs.addTab(form_tab, "Form")

        # --- Raw JSON tab ---
        raw_tab = QWidget()
        raw_l = QVBoxLayout(raw_tab)
        self.raw_edit = QPlainTextEdit(json.dumps(self._data or {}, ensure_ascii=False, indent=2))
        raw_l.addWidget(self.raw_edit, 1)
        self.tabs.addTab(raw_tab, "Raw JSON")

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self._accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

        self.type_combo.currentTextChanged.connect(self._update_field_enables)
        self._update_field_enables(self.type_combo.currentText())

    def _update_field_enables(self, t: str):
        t = (t or "").strip().lower()
        is_message = t in ("show_message", "narrate")
        is_unlock = t == "unlock_room_search"
        is_quest = t in (
            "start_quest",
            "complete_quest",
            "fail_quest",
            "track_quest",
            "advance_quest",
            "advance_quest_if_active",
        ) or t.startswith("if_quest_")
        is_milestone = t in ("set_milestone", "clear_milestone", "if_milestone_set", "if_milestone_not_set")
        is_milestones_list = t == "if_milestones_set"
        is_task = t == "set_quest_task_done"
        is_stage = t == "advance_quest_stage"
        is_item = t in ("give_item", "give_item_to_player", "take_item", "reveal_hidden_item", "spawn_item_on_ground")
        is_cinematic = t in ("play_cinematic", "trigger_cutscene")
        is_tutorial = t == "system_tutorial"
        is_player_action = t == "player_action"
        is_room_state = t in ("set_room_state", "toggle_room_state")
        is_spawn_encounter = t == "spawn_encounter"
        is_give_xp = t == "give_xp"
        is_begin_node = t == "begin_node"
        is_audio = t == "audio_layer"

        self.message_edit.setEnabled(is_message or is_unlock)
        self.tap_chk.setEnabled(t == "narrate")
        self.quest_combo.setEnabled(is_quest or is_task or is_stage)
        self.milestone_combo.setEnabled(is_milestone)
        self.milestones_edit.setEnabled(is_milestones_list)
        self.quest_task_combo.setEnabled(is_task)
        self.quest_stage_combo.setEnabled(is_stage)
        self.item_combo.setEnabled(is_item)
        self.qty_edit.setEnabled(is_item)
        self.cinematic_combo.setEnabled(is_cinematic)
        self.tutorial_combo.setEnabled(is_tutorial)
        self.context_edit.setEnabled(is_tutorial)
        self.action_combo.setEnabled(is_player_action)
        self.action_item_combo.setEnabled(is_player_action)
        self.room_combo.setEnabled(is_room_state or is_begin_node or is_spawn_encounter or is_unlock or t in ("reveal_hidden_item", "spawn_item_on_ground"))
        self.state_key_edit.setEnabled(is_room_state)
        self.state_value_chk.setEnabled(t == "set_room_state")
        self.encounter_edit.setEnabled(is_spawn_encounter)
        self.xp_edit.setEnabled(is_give_xp)
        for w in (self.audio_layer_edit, self.audio_cue_edit, self.audio_gain_edit, self.audio_fade_edit, self.audio_loop_chk, self.audio_stop_chk):
            w.setEnabled(is_audio)

    def _accept(self):
        if self.tabs.currentWidget() is not None and self.tabs.currentIndex() == 1:
            raw = (self.raw_edit.toPlainText() or "").strip()
            try:
                obj = json.loads(raw or "{}")
            except Exception as exc:
                QMessageBox.warning(self, "Invalid JSON", f"Raw JSON must be valid.\n\n{exc}")
                return
            if not isinstance(obj, dict):
                QMessageBox.warning(self, "Invalid JSON", "Raw JSON must be an object.")
                return
            if not str(obj.get("type") or "").strip():
                QMessageBox.warning(self, "Invalid Action", "Action must have a non-empty 'type'.")
                return
            self._data = obj
            self.accept()
            return

        t = self.type_combo.currentText().strip()
        if not t:
            QMessageBox.warning(self, "Missing Type", "Action type is required.")
            return
        t_low = t.lower()

        base = dict(self._data)
        base["type"] = t

        # Clear fields we manage; preserve unknown keys and nested lists.
        for k in (
            "quest_id",
            "task_id",
            "to_stage_id",
            "milestone",
            "milestones",
            "scene_id",
            "message",
            "text",
            "tap_to_dismiss",
            "item_id",
            "item",
            "quantity",
            "action",
            "context",
            "room_id",
            "state_key",
            "value",
            "encounter_id",
            "note",
            "xp",
            "layer",
            "cue_id",
            "gain",
            "fade_ms",
            "loop",
            "stop",
        ):
            base.pop(k, None)

        msg = self.message_edit.toPlainText().strip()
        if t_low in ("show_message", "narrate"):
            if not msg:
                QMessageBox.warning(self, "Missing Text", f"Action '{t}' requires text.")
                return
            base["message"] = msg
            if t_low == "narrate":
                base["tap_to_dismiss"] = bool(self.tap_chk.isChecked())

        if t_low == "unlock_room_search":
            room_id = self.room_combo.currentText().strip()
            if not room_id:
                QMessageBox.warning(self, "Missing Room", "unlock_room_search requires a room id.")
                return
            base["room_id"] = room_id
            if msg:
                base["note"] = msg

        if t_low in (
            "start_quest",
            "complete_quest",
            "fail_quest",
            "track_quest",
            "advance_quest",
            "advance_quest_if_active",
        ) or t_low.startswith("if_quest_"):
            qid = self.quest_combo.currentText().strip()
            if not qid:
                QMessageBox.warning(self, "Missing Quest", f"Action '{t}' requires quest_id.")
                return
            base["quest_id"] = qid

        if t_low in ("set_milestone", "clear_milestone", "if_milestone_set", "if_milestone_not_set"):
            mid = self.milestone_combo.currentText().strip()
            if not mid:
                QMessageBox.warning(self, "Missing Milestone", f"Action '{t}' requires milestone.")
                return
            base["milestone"] = mid

        if t_low == "if_milestones_set":
            milestones = [m.strip() for m in (self.milestones_edit.text() or "").split(",") if m.strip()]
            if not milestones:
                QMessageBox.warning(self, "Missing Milestones", "if_milestones_set requires a milestones list.")
                return
            base["milestones"] = milestones

        if t_low == "set_quest_task_done":
            qt = self.quest_task_combo.currentText().strip()
            parts = qt.split(":", 1)
            qid = parts[0].strip() if parts else ""
            tid = parts[1].strip() if len(parts) > 1 else ""
            if not qid or not tid:
                QMessageBox.warning(self, "Missing Task", "Pick a quest:task value for set_quest_task_done.")
                return
            base["quest_id"] = qid
            base["task_id"] = tid

        if t_low == "advance_quest_stage":
            qs = self.quest_stage_combo.currentText().strip()
            parts = qs.split(":", 1)
            qid = parts[0].strip() if parts else ""
            sid = parts[1].strip() if len(parts) > 1 else ""
            if not qid or not sid:
                QMessageBox.warning(self, "Missing Stage", "Pick a quest:stage value for advance_quest_stage.")
                return
            base["quest_id"] = qid
            base["to_stage_id"] = sid

        if t_low in ("play_cinematic", "trigger_cutscene"):
            scene_id = self.cinematic_combo.currentText().strip()
            if scene_id:
                base["scene_id"] = scene_id

        if t_low == "system_tutorial":
            scene_id = self.tutorial_combo.currentText().strip()
            if scene_id:
                base["scene_id"] = scene_id
            ctx = self.context_edit.text().strip()
            if ctx:
                base["context"] = ctx

        if t_low in ("give_item", "give_item_to_player", "take_item", "reveal_hidden_item", "spawn_item_on_ground"):
            item_id = self.item_combo.currentText().strip()
            if not item_id:
                QMessageBox.warning(self, "Missing Item", f"Action '{t}' requires an item id.")
                return
            try:
                qty = int(float(self.qty_edit.text().strip() or "1"))
            except Exception:
                qty = 1
            base["item_id"] = item_id
            base["quantity"] = max(1, qty)
            if t_low in ("reveal_hidden_item", "spawn_item_on_ground"):
                room_id = self.room_combo.currentText().strip()
                if room_id:
                    base["room_id"] = room_id

        if t_low == "player_action":
            action_id = self.action_combo.currentText().strip()
            if not action_id:
                QMessageBox.warning(self, "Missing Action", "player_action requires an action id.")
                return
            base["action"] = action_id
            item_id = self.action_item_combo.currentText().strip()
            if item_id:
                base["item_id"] = item_id

        if t_low in ("set_room_state", "toggle_room_state"):
            state_key = self.state_key_edit.text().strip()
            if not state_key:
                QMessageBox.warning(self, "Missing State Key", f"Action '{t}' requires a state_key.")
                return
            room_id = self.room_combo.currentText().strip()
            if room_id:
                base["room_id"] = room_id
            base["state_key"] = state_key
            if t_low == "set_room_state":
                base["value"] = bool(self.state_value_chk.isChecked())

        if t_low == "spawn_encounter":
            enc = self.encounter_edit.text().strip()
            if not enc:
                QMessageBox.warning(self, "Missing Encounter", "spawn_encounter requires an encounter_id.")
                return
            base["encounter_id"] = enc
            room_id = self.room_combo.currentText().strip()
            if room_id:
                base["room_id"] = room_id

        if t_low == "begin_node":
            room_id = self.room_combo.currentText().strip()
            if not room_id:
                QMessageBox.warning(self, "Missing Room", "begin_node requires a room id.")
                return
            base["room_id"] = room_id

        if t_low == "give_xp":
            try:
                xp = int(float(self.xp_edit.text().strip() or "0"))
            except Exception:
                xp = 0
            if xp <= 0:
                QMessageBox.warning(self, "Missing XP", "give_xp requires a positive xp amount.")
                return
            base["xp"] = xp

        if t_low == "audio_layer":
            layer = self.audio_layer_edit.text().strip()
            cue = self.audio_cue_edit.text().strip()
            gain = self.audio_gain_edit.text().strip()
            fade = self.audio_fade_edit.text().strip()
            if layer:
                base["layer"] = layer
            if cue:
                base["cue_id"] = cue
            if gain:
                try:
                    base["gain"] = float(gain)
                except Exception:
                    pass
            if fade:
                try:
                    base["fade_ms"] = int(float(fade))
                except Exception:
                    pass
            if self.audio_loop_chk.isChecked():
                base["loop"] = True
            if self.audio_stop_chk.isChecked():
                base["stop"] = True

        # Conditional: ensure we have branch lists when switching types.
        if t_low.startswith("if_"):
            if not isinstance(base.get("do"), list):
                base["do"] = []
            if "elseDo" in base and not isinstance(base.get("elseDo"), list):
                base["elseDo"] = []

        self._data = base
        self.accept()

    def value(self) -> dict:
        return dict(self._data)


class EventActionsStructuredEdit(QDialog):
    def __init__(
        self,
        parent: QWidget,
        *,
        actions: Optional[list] = None,
        quest_ids: Sequence[str] = (),
        milestone_ids: Sequence[str] = (),
        item_ids: Sequence[str] = (),
        room_ids: Sequence[str] = (),
        cinematic_ids: Sequence[str] = (),
        tutorial_ids: Sequence[str] = (),
        quest_stage_keys: Sequence[str] = (),
        quest_task_keys: Sequence[str] = (),
        player_action_ids: Sequence[str] = (),
    ):
        super().__init__(parent)
        self.setWindowTitle("Edit Actions (Structured)")
        self.resize(980, 640)

        self._lookups = {
            "quest_ids": list(quest_ids),
            "milestone_ids": list(milestone_ids),
            "item_ids": list(item_ids),
            "room_ids": list(room_ids),
            "cinematic_ids": list(cinematic_ids),
            "tutorial_ids": list(tutorial_ids),
            "quest_stage_keys": list(quest_stage_keys),
            "quest_task_keys": list(quest_task_keys),
            "player_action_ids": list(player_action_ids),
        }

        base_actions = actions if isinstance(actions, list) else []
        self._actions: List[dict] = [a for a in copy.deepcopy(base_actions) if isinstance(a, dict)]
        self._normalize_actions_in_place(self._actions)

        layout = QVBoxLayout(self)
        self.tree = ActionTreeWidget()
        self.tree.setColumnCount(2)
        self.tree.setHeaderLabels(["Action", "Details"])
        self.tree.setAlternatingRowColors(True)
        self.tree.on_reordered = self._on_tree_reordered
        layout.addWidget(self.tree, 1)

        btns = QHBoxLayout()
        b_add = QPushButton("Add…")
        b_copy = QPushButton("Copy")
        b_paste = QPushButton("Paste")
        b_dup = QPushButton("Duplicate")
        b_edit = QPushButton("Edit…")
        b_del = QPushButton("Delete")
        b_up = QPushButton("▲")
        b_dn = QPushButton("▼")
        b_else = QPushButton("Add Else Branch")
        b_onc = QPushButton("Add On-Complete")
        btns.addWidget(b_add)
        btns.addWidget(b_copy)
        btns.addWidget(b_paste)
        btns.addWidget(b_dup)
        btns.addWidget(b_edit)
        btns.addWidget(b_del)
        btns.addStretch(1)
        btns.addWidget(b_else)
        btns.addWidget(b_onc)
        btns.addWidget(b_up)
        btns.addWidget(b_dn)
        layout.addLayout(btns)

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

        b_add.clicked.connect(self._add_action)
        b_copy.clicked.connect(self._copy_selected)
        b_paste.clicked.connect(self._paste_from_clipboard)
        b_dup.clicked.connect(self._duplicate_selected)
        b_edit.clicked.connect(self._edit_selected)
        b_del.clicked.connect(self._delete_selected)
        b_up.clicked.connect(lambda: self._move_selected(-1))
        b_dn.clicked.connect(lambda: self._move_selected(+1))
        b_else.clicked.connect(self._add_else_branch)
        b_onc.clicked.connect(self._add_on_complete)

        QShortcut(QKeySequence.Copy, self.tree, self._copy_selected)
        QShortcut(QKeySequence.Paste, self.tree, self._paste_from_clipboard)

        self._rebuild_tree(select_path="actions")

    def value(self) -> list:
        return copy.deepcopy(self._actions)

    def _normalize_actions_in_place(self, actions: Any):
        if not isinstance(actions, list):
            return
        for act in actions:
            if not isinstance(act, dict):
                continue
            if "else" in act and "elseDo" not in act and isinstance(act.get("else"), list):
                act["elseDo"] = act.get("else")
                act.pop("else", None)
            if "onComplete" in act and "on_complete" not in act and isinstance(act.get("onComplete"), list):
                act["on_complete"] = act.get("onComplete")
                act.pop("onComplete", None)
            if "cutscene_id" in act and "scene_id" not in act and isinstance(act.get("cutscene_id"), str):
                act["scene_id"] = act.get("cutscene_id")
                act.pop("cutscene_id", None)
            for k in ("do", "elseDo", "on_complete"):
                if k in act and not isinstance(act.get(k), list):
                    act.pop(k, None)
            for child_key in ("do", "elseDo", "on_complete"):
                child = act.get(child_key)
                if isinstance(child, list):
                    self._normalize_actions_in_place(child)

    def _action_summary(self, act: dict) -> str:
        t = str(act.get("type") or "").strip()
        tl = t.lower()
        if tl in ("show_message", "narrate"):
            return _fmt_one_line(str(act.get("message") or act.get("text") or ""))
        if tl in ("play_cinematic", "trigger_cutscene"):
            return str(act.get("scene_id") or "")
        if tl == "system_tutorial":
            scene = str(act.get("scene_id") or "")
            ctx = str(act.get("context") or "")
            return f"{scene}|{ctx}" if scene and ctx else (scene or ctx)
        if tl in ("set_milestone", "clear_milestone", "if_milestone_set", "if_milestone_not_set"):
            return str(act.get("milestone") or "")
        if tl == "if_milestones_set":
            ms = act.get("milestones") or []
            if isinstance(ms, list):
                return ", ".join([str(x) for x in ms if str(x).strip()])
        if tl in ("start_quest", "complete_quest", "fail_quest", "track_quest", "advance_quest", "advance_quest_if_active") or tl.startswith("if_quest_"):
            return str(act.get("quest_id") or "")
        if tl == "set_quest_task_done":
            q = str(act.get("quest_id") or "")
            ta = str(act.get("task_id") or "")
            return f"{q}:{ta}" if q and ta else (q or ta)
        if tl == "advance_quest_stage":
            q = str(act.get("quest_id") or "")
            st = str(act.get("to_stage_id") or "")
            return f"{q}:{st}" if q and st else (q or st)
        if tl in ("give_item", "give_item_to_player", "take_item", "reveal_hidden_item", "spawn_item_on_ground"):
            item_id = str(act.get("item_id") or act.get("item") or "")
            qty = act.get("quantity")
            if qty is None:
                return item_id
            return f"{item_id} x{qty}"
        if tl in ("set_room_state", "toggle_room_state"):
            room = str(act.get("room_id") or "")
            key = str(act.get("state_key") or "")
            if tl == "set_room_state":
                val = act.get("value")
                return f"{room} {key}={val}"
            return f"{room} {key}"
        if tl == "spawn_encounter":
            enc = str(act.get("encounter_id") or "")
            room = str(act.get("room_id") or "")
            return f"{enc} @ {room}".strip()
        if tl == "begin_node":
            return str(act.get("room_id") or "")
        if tl == "unlock_room_search":
            return _fmt_one_line(str(act.get("note") or ""))
        if tl == "give_xp":
            return str(act.get("xp") or "")
        if tl == "player_action":
            a = str(act.get("action") or "")
            it = str(act.get("item_id") or act.get("item") or "")
            return f"{a}:{it}" if it else a
        if tl.startswith("if_"):
            return "branch"
        return ""

    def _should_show_child_groups(self, act: dict, list_key: str) -> bool:
        t = str(act.get("type") or "").strip().lower()
        if list_key in ("do", "elseDo") and t.startswith("if_"):
            return True
        if list_key == "on_complete" and t in ("play_cinematic", "trigger_cutscene", "system_tutorial"):
            return True
        return isinstance(act.get(list_key), list)

    def _rebuild_tree(self, *, select_path: Optional[str] = None, select_action: Optional[dict] = None):
        self.tree.blockSignals(True)
        try:
            self.tree.clear()

            def add_actions(parent_item: Optional[QTreeWidgetItem], actions: List[dict], base_path: str):
                for idx, act in enumerate(actions):
                    if not isinstance(act, dict):
                        continue
                    path = f"{base_path}[{idx}]"
                    t = str(act.get("type") or "").strip() or "(missing type)"
                    item = QTreeWidgetItem([t, self._action_summary(act)])
                    item.setData(0, Qt.UserRole, {"kind": "action", "path": path, "act": act})
                    item.setFlags((item.flags() | Qt.ItemIsDragEnabled) & ~Qt.ItemIsDropEnabled)
                    if parent_item is None:
                        self.tree.addTopLevelItem(item)
                    else:
                        parent_item.addChild(item)

                    for list_key in ("do", "elseDo", "on_complete"):
                        if not self._should_show_child_groups(act, list_key):
                            continue
                        group_path = f"{path}.{list_key}"
                        child_list = act.get(list_key)
                        if not isinstance(child_list, list):
                            child_list = []
                        label = f"{list_key} ({len(child_list)})"
                        group = QTreeWidgetItem([label, ""])
                        group.setData(0, Qt.UserRole, {"kind": "group", "path": group_path, "list_key": list_key})
                        group.setFlags((group.flags() | Qt.ItemIsDropEnabled) & ~Qt.ItemIsDragEnabled)
                        item.addChild(group)
                        add_actions(group, child_list, group_path)

                    item.setExpanded(True)

            add_actions(None, self._actions, "actions")

            # Select requested path if available.
            it: Optional[QTreeWidgetItem] = None
            if select_action is not None:
                it = self._find_item_by_action_ref(select_action)
            if it is None and select_path:
                it = self._find_item_by_path(select_path)
            if it is not None:
                self.tree.setCurrentItem(it)
            else:
                if self.tree.topLevelItemCount() > 0:
                    self.tree.setCurrentItem(self.tree.topLevelItem(0))
        finally:
            self.tree.blockSignals(False)

    def _find_item_by_path(self, path: str) -> Optional[QTreeWidgetItem]:
        def walk(item: QTreeWidgetItem) -> Optional[QTreeWidgetItem]:
            meta = item.data(0, Qt.UserRole)
            if isinstance(meta, dict) and meta.get("path") == path:
                return item
            for i in range(item.childCount()):
                found = walk(item.child(i))
                if found is not None:
                    return found
            return None

        for i in range(self.tree.topLevelItemCount()):
            found = walk(self.tree.topLevelItem(i))
            if found is not None:
                return found
        return None

    def _find_item_by_action_ref(self, act_ref: dict) -> Optional[QTreeWidgetItem]:
        def walk(item: QTreeWidgetItem) -> Optional[QTreeWidgetItem]:
            meta = item.data(0, Qt.UserRole)
            if isinstance(meta, dict) and meta.get("kind") == "action" and meta.get("act") is act_ref:
                return item
            for i in range(item.childCount()):
                found = walk(item.child(i))
                if found is not None:
                    return found
            return None

        for i in range(self.tree.topLevelItemCount()):
            found = walk(self.tree.topLevelItem(i))
            if found is not None:
                return found
        return None

    def _selected_meta(self) -> Tuple[str, str]:
        it = self.tree.currentItem()
        meta = it.data(0, Qt.UserRole) if it else None
        if isinstance(meta, dict):
            kind = str(meta.get("kind") or "").strip()
            path = str(meta.get("path") or "").strip()
            if kind and path:
                return kind, path
        return "", ""

    def _action_from_tree_item(self, item: QTreeWidgetItem) -> Optional[dict]:
        meta = item.data(0, Qt.UserRole)
        act = meta.get("act") if isinstance(meta, dict) else None
        if not isinstance(act, dict):
            return None

        for i in range(item.childCount()):
            child = item.child(i)
            cmeta = child.data(0, Qt.UserRole)
            if not isinstance(cmeta, dict) or cmeta.get("kind") != "group":
                continue
            list_key = str(cmeta.get("list_key") or "").strip()
            if not list_key:
                continue
            new_list: List[dict] = []
            for j in range(child.childCount()):
                a = self._action_from_tree_item(child.child(j))
                if isinstance(a, dict):
                    new_list.append(a)
            act[list_key] = new_list

        return act

    def _actions_from_tree(self) -> List[dict]:
        out: List[dict] = []
        for i in range(self.tree.topLevelItemCount()):
            it = self.tree.topLevelItem(i)
            meta = it.data(0, Qt.UserRole)
            if not isinstance(meta, dict) or meta.get("kind") != "action":
                continue
            act = self._action_from_tree_item(it)
            if isinstance(act, dict):
                out.append(act)
        return out

    def _on_tree_reordered(self):
        selected_act: Optional[dict] = None
        it = self.tree.currentItem()
        meta = it.data(0, Qt.UserRole) if it else None
        if isinstance(meta, dict) and meta.get("kind") == "action" and isinstance(meta.get("act"), dict):
            selected_act = meta.get("act")

        self._actions = self._actions_from_tree()
        self._normalize_actions_in_place(self._actions)
        self._rebuild_tree(select_action=selected_act)

    def _parse_path(self, path: str) -> List[Tuple[str, Optional[int]]]:
        out: List[Tuple[str, Optional[int]]] = []
        for part in (path or "").split("."):
            part = part.strip()
            if not part:
                continue
            m = re.match(r"^([A-Za-z_]+)(?:\\[(\\d+)\\])?$", part)
            if not m:
                continue
            key = m.group(1)
            idx = int(m.group(2)) if m.group(2) is not None else None
            out.append((key, idx))
        return out

    def _get_action_at_path(self, path: str) -> Optional[dict]:
        segs = self._parse_path(path)
        if not segs:
            return None
        cur_list: Any = self._actions
        cur_action: Optional[dict] = None
        for key, idx in segs:
            if key == "actions":
                if idx is None or not isinstance(cur_list, list) or not (0 <= idx < len(cur_list)):
                    return None
                cur_action = cur_list[idx]
                if not isinstance(cur_action, dict):
                    return None
            else:
                if cur_action is None:
                    return None
                nxt = cur_action.get(key)
                if not isinstance(nxt, list):
                    return None
                if idx is None or not (0 <= idx < len(nxt)):
                    return None
                cur_list = nxt
                cur_action = nxt[idx]
                if not isinstance(cur_action, dict):
                    return None
        return cur_action

    def _get_list_at_group_path(self, path: str, *, create: bool = False) -> Optional[List[dict]]:
        segs = self._parse_path(path)
        if not segs:
            return None
        if segs == [("actions", None)]:
            return self._actions
        if segs[-1][1] is not None:
            return None
        list_key = segs[-1][0]
        parent_path = ".".join(
            [f"{k}[{i}]" if i is not None else k for k, i in segs[:-1]]
        )
        parent_action = self._get_action_at_path(parent_path)
        if parent_action is None:
            return None
        existing = parent_action.get(list_key)
        if isinstance(existing, list):
            return existing
        if create:
            parent_action[list_key] = []
            return parent_action[list_key]
        return None

    def _parent_group_path_for_action_path(self, action_path: str) -> str:
        parts = (action_path or "").split(".")
        if not parts:
            return "actions"
        if len(parts) == 1:
            return "actions"
        last = re.sub(r"\\[\\d+\\]$", "", parts[-1])
        return ".".join(parts[:-1] + [last])

    def _add_action(self):
        kind, path = self._selected_meta()
        if kind == "group":
            group_path = path
            insert_at: Optional[int] = None
        elif kind == "action":
            group_path = self._parent_group_path_for_action_path(path)
            parent_list, idx = self._parent_list_and_index(path)
            insert_at = idx + 1 if parent_list is not None else None
        else:
            group_path = "actions"
            insert_at = None

        target = self._get_list_at_group_path(group_path, create=True)
        if target is None:
            QMessageBox.warning(self, "Add Action", "Couldn't resolve the selected insertion point.")
            return

        dialog = EventActionEditDialog(self, data={"type": "show_message", "message": "TODO"}, **self._lookups)
        if dialog.exec_() != QDialog.Accepted:
            return
        new_act = dialog.value()
        if not isinstance(new_act, dict):
            return
        if insert_at is None or not (0 <= insert_at <= len(target)):
            target.append(new_act)
            insert_at = len(target) - 1
        else:
            target.insert(insert_at, new_act)

        self._normalize_actions_in_place(self._actions)
        self._rebuild_tree(select_path=f"{group_path}[{insert_at}]")

    def _copy_selected(self):
        kind, path = self._selected_meta()
        if kind == "action":
            act = self._get_action_at_path(path)
            if not isinstance(act, dict):
                return
            payload: Any = act
        elif kind == "group":
            lst = self._get_list_at_group_path(path, create=False)
            if not isinstance(lst, list):
                return
            payload = lst
        else:
            return
        QApplication.clipboard().setText(json.dumps(payload, ensure_ascii=False, indent=2))

    def _paste_from_clipboard(self):
        raw = QApplication.clipboard().text() or ""
        try:
            obj = json.loads(raw)
        except Exception as exc:
            QMessageBox.warning(self, "Paste", f"Clipboard doesn't contain valid JSON.\n\n{exc}")
            return

        acts: List[dict]
        if isinstance(obj, dict):
            acts = [obj]
        elif isinstance(obj, list):
            acts = [x for x in obj if isinstance(x, dict)]
            if not acts:
                QMessageBox.warning(self, "Paste", "Clipboard JSON list doesn't contain any objects.")
                return
        else:
            QMessageBox.warning(self, "Paste", "Clipboard JSON must be an object or a list of objects.")
            return

        acts = [copy.deepcopy(a) for a in acts]

        kind, path = self._selected_meta()
        if kind == "group":
            group_path = path
            insert_at: Optional[int] = None
        elif kind == "action":
            group_path = self._parent_group_path_for_action_path(path)
            parent_list, idx = self._parent_list_and_index(path)
            insert_at = idx + 1 if parent_list is not None else None
        else:
            group_path = "actions"
            insert_at = None

        target = self._get_list_at_group_path(group_path, create=True)
        if target is None:
            QMessageBox.warning(self, "Paste", "Couldn't resolve the selected insertion point.")
            return

        if insert_at is None or not (0 <= insert_at <= len(target)):
            insert_at = len(target)
            target.extend(acts)
        else:
            for i, a in enumerate(acts):
                target.insert(insert_at + i, a)

        self._normalize_actions_in_place(self._actions)
        self._rebuild_tree(select_path=f"{group_path}[{insert_at}]")

    def _parent_list_and_index(self, action_path: str) -> Tuple[Optional[List[dict]], int]:
        segs = self._parse_path(action_path)
        if not segs or segs[-1][1] is None:
            return None, -1
        idx = int(segs[-1][1])
        group_path = self._parent_group_path_for_action_path(action_path)
        lst = self._get_list_at_group_path(group_path, create=False)
        if lst is None:
            return None, -1
        return lst, idx

    def _edit_selected(self):
        kind, path = self._selected_meta()
        if kind != "action":
            return
        parent_list, idx = self._parent_list_and_index(path)
        if parent_list is None or not (0 <= idx < len(parent_list)):
            return
        act = parent_list[idx]
        if not isinstance(act, dict):
            return
        dialog = EventActionEditDialog(self, data=act, **self._lookups)
        if dialog.exec_() != QDialog.Accepted:
            return
        parent_list[idx] = dialog.value()
        self._normalize_actions_in_place(self._actions)
        self._rebuild_tree(select_path=path)

    def _duplicate_selected(self):
        kind, path = self._selected_meta()
        if kind != "action":
            return
        parent_list, idx = self._parent_list_and_index(path)
        if parent_list is None or not (0 <= idx < len(parent_list)):
            return
        act = parent_list[idx]
        if not isinstance(act, dict):
            return
        parent_list.insert(idx + 1, copy.deepcopy(act))
        self._normalize_actions_in_place(self._actions)
        group_path = self._parent_group_path_for_action_path(path)
        self._rebuild_tree(select_path=f"{group_path}[{idx + 1}]")

    def _delete_selected(self):
        kind, path = self._selected_meta()
        if kind != "action":
            return
        parent_list, idx = self._parent_list_and_index(path)
        if parent_list is None or not (0 <= idx < len(parent_list)):
            return
        parent_list.pop(idx)
        self._normalize_actions_in_place(self._actions)
        new_idx = min(idx, len(parent_list) - 1)
        group_path = self._parent_group_path_for_action_path(path)
        select_path = f"{group_path}[{new_idx}]" if new_idx >= 0 else group_path
        self._rebuild_tree(select_path=select_path)

    def _move_selected(self, delta: int):
        kind, path = self._selected_meta()
        if kind != "action":
            return
        parent_list, idx = self._parent_list_and_index(path)
        if parent_list is None or not (0 <= idx < len(parent_list)):
            return
        new_idx = idx + delta
        if not (0 <= new_idx < len(parent_list)):
            return
        parent_list[idx], parent_list[new_idx] = parent_list[new_idx], parent_list[idx]
        self._normalize_actions_in_place(self._actions)
        group_path = self._parent_group_path_for_action_path(path)
        self._rebuild_tree(select_path=f"{group_path}[{new_idx}]")

    def _add_else_branch(self):
        kind, path = self._selected_meta()
        if kind != "action":
            return
        act = self._get_action_at_path(path)
        if not isinstance(act, dict):
            return
        t = str(act.get("type") or "").strip().lower()
        if not t.startswith("if_"):
            QMessageBox.information(self, "Else Branch", "Else branches are only supported on if_* actions.")
            return
        if not isinstance(act.get("do"), list):
            act["do"] = []
        if not isinstance(act.get("elseDo"), list):
            act["elseDo"] = []
        self._rebuild_tree(select_path=f"{path}.elseDo")

    def _add_on_complete(self):
        kind, path = self._selected_meta()
        if kind != "action":
            return
        act = self._get_action_at_path(path)
        if not isinstance(act, dict):
            return
        if not isinstance(act.get("on_complete"), list):
            act["on_complete"] = []
        self._rebuild_tree(select_path=f"{path}.on_complete")


class EventConditionEditDialog(QDialog):
    def __init__(
        self,
        parent: QWidget,
        *,
        data: Optional[dict] = None,
        quest_ids: Sequence[str] = (),
        milestone_ids: Sequence[str] = (),
        item_ids: Sequence[str] = (),
        event_ids: Sequence[str] = (),
        tutorial_ids: Sequence[str] = (),
        quest_stage_keys: Sequence[str] = (),
        quest_task_keys: Sequence[str] = (),
    ):
        super().__init__(parent)
        self.setWindowTitle("Edit Condition")
        self.resize(660, 460)
        self._data = dict(data) if isinstance(data, dict) else {}

        layout = QVBoxLayout(self)
        self.tabs = QTabWidget()
        layout.addWidget(self.tabs, 1)

        # --- Form tab ---
        form_tab = QWidget()
        form_l = QVBoxLayout(form_tab)
        form = QFormLayout()

        self.type_combo = QComboBox()
        self.type_combo.setEditable(True)
        self.type_combo.setInsertPolicy(QComboBox.NoInsert)
        type_vals = list(EVENT_CONDITION_TYPES)
        self.type_combo.addItems(type_vals)
        self.type_combo.setCurrentText(str(self._data.get("type") or "").strip())
        self.type_combo.setCompleter(_make_completer([""] + type_vals))

        self.milestone_combo = QComboBox()
        self.milestone_combo.setEditable(True)
        self.milestone_combo.setInsertPolicy(QComboBox.NoInsert)
        ms_vals = [""] + list(milestone_ids)
        self.milestone_combo.addItems(ms_vals)
        self.milestone_combo.setCurrentText(str(self._data.get("milestone") or "").strip())
        self.milestone_combo.setCompleter(_make_completer(ms_vals))

        self.quest_combo = QComboBox()
        self.quest_combo.setEditable(True)
        self.quest_combo.setInsertPolicy(QComboBox.NoInsert)
        quest_vals = [""] + list(quest_ids)
        self.quest_combo.addItems(quest_vals)
        self.quest_combo.setCurrentText(str(self._data.get("quest_id") or "").strip())
        self.quest_combo.setCompleter(_make_completer(quest_vals))

        self.quest_stage_combo = QComboBox()
        self.quest_stage_combo.setEditable(True)
        self.quest_stage_combo.setInsertPolicy(QComboBox.NoInsert)
        qs_vals = [""] + list(quest_stage_keys)
        self.quest_stage_combo.addItems(qs_vals)
        qid = str(self._data.get("quest_id") or "").strip()
        sid = str(self._data.get("stage_id") or "").strip()
        self.quest_stage_combo.setCurrentText(f"{qid}:{sid}" if qid and sid else "")
        self.quest_stage_combo.setCompleter(_make_completer(qs_vals))

        self.quest_task_combo = QComboBox()
        self.quest_task_combo.setEditable(True)
        self.quest_task_combo.setInsertPolicy(QComboBox.NoInsert)
        qt_vals = [""] + list(quest_task_keys)
        self.quest_task_combo.addItems(qt_vals)
        tid = str(self._data.get("task_id") or "").strip()
        self.quest_task_combo.setCurrentText(f"{qid}:{tid}" if qid and tid else "")
        self.quest_task_combo.setCompleter(_make_completer(qt_vals))

        self.event_combo = QComboBox()
        self.event_combo.setEditable(True)
        self.event_combo.setInsertPolicy(QComboBox.NoInsert)
        ev_vals = [""] + list(event_ids)
        self.event_combo.addItems(ev_vals)
        self.event_combo.setCurrentText(str(self._data.get("event_id") or self._data.get("eventId") or "").strip())
        self.event_combo.setCompleter(_make_completer(ev_vals))

        self.tutorial_combo = QComboBox()
        self.tutorial_combo.setEditable(True)
        self.tutorial_combo.setInsertPolicy(QComboBox.NoInsert)
        tut_vals = [""] + list(tutorial_ids)
        self.tutorial_combo.addItems(tut_vals)
        self.tutorial_combo.setCurrentText(str(self._data.get("tutorial_id") or self._data.get("tutorialId") or "").strip())
        self.tutorial_combo.setCompleter(_make_completer(tut_vals))

        self.item_combo = QComboBox()
        self.item_combo.setEditable(True)
        self.item_combo.setInsertPolicy(QComboBox.NoInsert)
        item_vals = [""] + list(item_ids)
        self.item_combo.addItems(item_vals)
        self.item_combo.setCurrentText(str(self._data.get("item_id") or self._data.get("item") or "").strip())
        self.item_combo.setCompleter(_make_completer(item_vals))
        self.qty_edit = QLineEdit(str(self._data.get("quantity") or "1"))

        form.addRow("Type:", self.type_combo)
        form.addRow("Milestone:", self.milestone_combo)
        form.addRow("Quest ID:", self.quest_combo)
        form.addRow("Quest Stage (quest:stage):", self.quest_stage_combo)
        form.addRow("Quest Task (quest:task):", self.quest_task_combo)
        form.addRow("Event ID:", self.event_combo)
        form.addRow("Tutorial ID:", self.tutorial_combo)
        form.addRow("Item ID:", self.item_combo)
        form.addRow("Quantity:", self.qty_edit)

        form_l.addLayout(form)
        self.tabs.addTab(form_tab, "Form")

        # --- Raw JSON tab ---
        raw_tab = QWidget()
        raw_l = QVBoxLayout(raw_tab)
        self.raw_edit = QPlainTextEdit(json.dumps(self._data or {}, ensure_ascii=False, indent=2))
        raw_l.addWidget(self.raw_edit, 1)
        self.tabs.addTab(raw_tab, "Raw JSON")

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self._accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

        self.type_combo.currentTextChanged.connect(self._update_field_enables)
        self._update_field_enables(self.type_combo.currentText())

    def _update_field_enables(self, t: str):
        t = (t or "").strip().lower()
        is_milestone = t in ("milestone_set", "milestone_not_set")
        is_quest = t in ("quest_active", "quest_not_started", "quest_completed", "quest_not_completed", "quest_failed")
        is_stage = t in ("quest_stage", "quest_stage_not")
        is_task = t in ("quest_task_done", "quest_task_not_done")
        is_event = t in ("event_completed", "event_not_completed")
        is_tutorial = t in ("tutorial_completed", "tutorial_not_completed")
        is_item = t in ("item", "item_not")

        self.milestone_combo.setEnabled(is_milestone)
        self.quest_combo.setEnabled(is_quest)
        self.quest_stage_combo.setEnabled(is_stage)
        self.quest_task_combo.setEnabled(is_task)
        self.event_combo.setEnabled(is_event)
        self.tutorial_combo.setEnabled(is_tutorial)
        self.item_combo.setEnabled(is_item)
        self.qty_edit.setEnabled(is_item)

    def _accept(self):
        if self.tabs.currentIndex() == 1:
            raw = (self.raw_edit.toPlainText() or "").strip()
            try:
                obj = json.loads(raw or "{}")
            except Exception as exc:
                QMessageBox.warning(self, "Invalid JSON", f"Raw JSON must be valid.\n\n{exc}")
                return
            if not isinstance(obj, dict):
                QMessageBox.warning(self, "Invalid JSON", "Raw JSON must be an object.")
                return
            if not str(obj.get("type") or "").strip():
                QMessageBox.warning(self, "Invalid Condition", "Condition must have a non-empty 'type'.")
                return
            self._data = obj
            self.accept()
            return

        t = self.type_combo.currentText().strip()
        if not t:
            QMessageBox.warning(self, "Missing Type", "Condition type is required.")
            return
        t_low = t.lower()
        base: Dict[str, Any] = {"type": t}

        if t_low in ("milestone_set", "milestone_not_set"):
            mid = self.milestone_combo.currentText().strip()
            if not mid:
                QMessageBox.warning(self, "Missing Milestone", f"Condition '{t}' requires milestone.")
                return
            base["milestone"] = mid

        if t_low in ("quest_active", "quest_not_started", "quest_completed", "quest_not_completed", "quest_failed"):
            qid = self.quest_combo.currentText().strip()
            if not qid:
                QMessageBox.warning(self, "Missing Quest", f"Condition '{t}' requires quest_id.")
                return
            base["quest_id"] = qid

        if t_low in ("quest_stage", "quest_stage_not"):
            qs = self.quest_stage_combo.currentText().strip()
            parts = qs.split(":", 1)
            qid = parts[0].strip() if parts else ""
            sid = parts[1].strip() if len(parts) > 1 else ""
            if not qid or not sid:
                QMessageBox.warning(self, "Missing Stage", "Pick a quest:stage value for quest_stage conditions.")
                return
            base["quest_id"] = qid
            base["stage_id"] = sid

        if t_low in ("quest_task_done", "quest_task_not_done"):
            qt = self.quest_task_combo.currentText().strip()
            parts = qt.split(":", 1)
            qid = parts[0].strip() if parts else ""
            tid = parts[1].strip() if len(parts) > 1 else ""
            if not qid or not tid:
                QMessageBox.warning(self, "Missing Task", "Pick a quest:task value for quest_task conditions.")
                return
            base["quest_id"] = qid
            base["task_id"] = tid

        if t_low in ("event_completed", "event_not_completed"):
            eid = self.event_combo.currentText().strip()
            if not eid:
                QMessageBox.warning(self, "Missing Event", f"Condition '{t}' requires event_id.")
                return
            base["event_id"] = eid

        if t_low in ("tutorial_completed", "tutorial_not_completed"):
            tid = self.tutorial_combo.currentText().strip()
            if not tid:
                QMessageBox.warning(self, "Missing Tutorial", f"Condition '{t}' requires tutorial_id.")
                return
            base["tutorial_id"] = tid

        if t_low in ("item", "item_not"):
            item_id = self.item_combo.currentText().strip()
            if not item_id:
                QMessageBox.warning(self, "Missing Item", f"Condition '{t}' requires item_id.")
                return
            base["item_id"] = item_id
            try:
                qty = int(float(self.qty_edit.text().strip() or "1"))
            except Exception:
                qty = 1
            qty = max(1, qty)
            if qty != 1:
                base["quantity"] = qty

        self._data = base
        self.accept()

    def value(self) -> dict:
        return dict(self._data)


class EventConditionsStructuredEdit(QDialog):
    def __init__(
        self,
        parent: QWidget,
        *,
        conditions: Optional[list] = None,
        quest_ids: Sequence[str] = (),
        milestone_ids: Sequence[str] = (),
        item_ids: Sequence[str] = (),
        event_ids: Sequence[str] = (),
        tutorial_ids: Sequence[str] = (),
        quest_stage_keys: Sequence[str] = (),
        quest_task_keys: Sequence[str] = (),
    ):
        super().__init__(parent)
        self.setWindowTitle("Edit Conditions (Structured)")
        self.resize(860, 520)

        self._lookups = {
            "quest_ids": list(quest_ids),
            "milestone_ids": list(milestone_ids),
            "item_ids": list(item_ids),
            "event_ids": list(event_ids),
            "tutorial_ids": list(tutorial_ids),
            "quest_stage_keys": list(quest_stage_keys),
            "quest_task_keys": list(quest_task_keys),
        }

        base_conditions = conditions if isinstance(conditions, list) else []
        self._conditions: List[dict] = [c for c in copy.deepcopy(base_conditions) if isinstance(c, dict)]

        layout = QVBoxLayout(self)
        self.list = QListWidget()
        self.list.setSelectionMode(QAbstractItemView.SingleSelection)
        self.list.setAlternatingRowColors(True)
        layout.addWidget(self.list, 1)

        btns = QHBoxLayout()
        b_add = QPushButton("Add…")
        b_edit = QPushButton("Edit…")
        b_del = QPushButton("Delete")
        b_up = QPushButton("▲")
        b_dn = QPushButton("▼")
        btns.addWidget(b_add)
        btns.addWidget(b_edit)
        btns.addWidget(b_del)
        btns.addStretch(1)
        btns.addWidget(b_up)
        btns.addWidget(b_dn)
        layout.addLayout(btns)

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

        b_add.clicked.connect(self._add)
        b_edit.clicked.connect(self._edit)
        b_del.clicked.connect(self._delete)
        b_up.clicked.connect(lambda: self._move(-1))
        b_dn.clicked.connect(lambda: self._move(+1))
        self.list.itemDoubleClicked.connect(lambda _it: self._edit())

        self._refresh()

    def value(self) -> list:
        return copy.deepcopy(self._conditions)

    def _cond_summary(self, cond: dict) -> str:
        t = str(cond.get("type") or "").strip()
        tl = t.lower()
        if tl in ("milestone_set", "milestone_not_set"):
            return f"{t}:{cond.get('milestone') or ''}"
        if tl.startswith("quest_") and tl not in ("quest_stage", "quest_stage_not", "quest_task_done", "quest_task_not_done"):
            return f"{t}:{cond.get('quest_id') or ''}"
        if tl in ("quest_stage", "quest_stage_not"):
            q = str(cond.get("quest_id") or "")
            s = str(cond.get("stage_id") or "")
            return f"{t}:{q}:{s}".strip(":")
        if tl in ("quest_task_done", "quest_task_not_done"):
            q = str(cond.get("quest_id") or "")
            task = str(cond.get("task_id") or "")
            return f"{t}:{q}:{task}".strip(":")
        if tl in ("event_completed", "event_not_completed"):
            return f"{t}:{cond.get('event_id') or ''}"
        if tl in ("tutorial_completed", "tutorial_not_completed"):
            return f"{t}:{cond.get('tutorial_id') or ''}"
        if tl in ("item", "item_not"):
            item_id = cond.get("item_id") or cond.get("item") or ""
            qty = cond.get("quantity")
            return f"{t}:{item_id} x{qty}" if qty else f"{t}:{item_id}"
        return t

    def _refresh(self):
        self.list.blockSignals(True)
        try:
            sel = self.list.currentRow()
            self.list.clear()
            for cond in self._conditions:
                it = QListWidgetItem(self._cond_summary(cond))
                it.setData(Qt.UserRole, cond)
                self.list.addItem(it)
            if 0 <= sel < self.list.count():
                self.list.setCurrentRow(sel)
            elif self.list.count() > 0:
                self.list.setCurrentRow(0)
        finally:
            self.list.blockSignals(False)

    def _selected_index(self) -> int:
        return self.list.currentRow()

    def _add(self):
        dialog = EventConditionEditDialog(self, data={"type": "quest_active"}, **self._lookups)
        if dialog.exec_() != QDialog.Accepted:
            return
        self._conditions.append(dialog.value())
        self._refresh()
        self.list.setCurrentRow(self.list.count() - 1)

    def _edit(self):
        idx = self._selected_index()
        if not (0 <= idx < len(self._conditions)):
            return
        dialog = EventConditionEditDialog(self, data=self._conditions[idx], **self._lookups)
        if dialog.exec_() != QDialog.Accepted:
            return
        self._conditions[idx] = dialog.value()
        self._refresh()
        self.list.setCurrentRow(idx)

    def _delete(self):
        idx = self._selected_index()
        if not (0 <= idx < len(self._conditions)):
            return
        self._conditions.pop(idx)
        self._refresh()

    def _move(self, delta: int):
        idx = self._selected_index()
        if not (0 <= idx < len(self._conditions)):
            return
        new_idx = idx + delta
        if not (0 <= new_idx < len(self._conditions)):
            return
        self._conditions[idx], self._conditions[new_idx] = self._conditions[new_idx], self._conditions[idx]
        self._refresh()
        self.list.setCurrentRow(new_idx)


class CinematicQuickEdit(QDialog):
    def __init__(self, parent: QWidget, *, existing_ids: Iterable[str], data: Optional[dict] = None):
        super().__init__(parent)
        self.setWindowTitle("Quick Edit — Cinematic Scene")
        self.resize(860, 540)
        self._existing = set(existing_ids)
        self._orig_id = (data or {}).get("id") if isinstance(data, dict) else None
        self._data = dict(data) if isinstance(data, dict) else {}

        layout = QVBoxLayout(self)
        form = QFormLayout()

        self.id_edit = QLineEdit(_ensure_str(self._data.get("id")))
        self.title_edit = QLineEdit(_ensure_str(self._data.get("title")))
        form.addRow("ID:", self.id_edit)
        form.addRow("Title:", self.title_edit)
        layout.addLayout(form)

        self.steps_table = QTableWidget()
        self.steps_table.setColumnCount(5)
        self.steps_table.setHorizontalHeaderLabels(["type", "speaker", "emote", "text", "durationSeconds"])
        self.steps_table.horizontalHeader().setStretchLastSection(True)
        self.steps_table.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.steps_table.setSelectionMode(QAbstractItemView.SingleSelection)
        layout.addWidget(self.steps_table, 1)

        btn_row = QHBoxLayout()
        b_add = QPushButton("Add Step")
        b_dup = QPushButton("Duplicate")
        b_del = QPushButton("Delete")
        b_up = QPushButton("▲")
        b_dn = QPushButton("▼")
        btn_row.addWidget(b_add)
        btn_row.addWidget(b_dup)
        btn_row.addWidget(b_del)
        btn_row.addStretch(1)
        btn_row.addWidget(b_up)
        btn_row.addWidget(b_dn)
        layout.addLayout(btn_row)

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self._accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

        b_add.clicked.connect(self._add_step)
        b_dup.clicked.connect(self._dup_step)
        b_del.clicked.connect(self._del_step)
        b_up.clicked.connect(lambda: self._move_step(-1))
        b_dn.clicked.connect(lambda: self._move_step(+1))

        self._load_steps()

    def _load_steps(self):
        steps = self._data.get("steps") or []
        if not isinstance(steps, list):
            steps = []
        self.steps_table.setRowCount(0)
        for step in steps:
            if not isinstance(step, dict):
                continue
            self._append_step_row(step)
        if self.steps_table.rowCount() == 0:
            self._append_step_row({"type": "narration", "text": "TODO: write the moment."})

    def _append_step_row(self, step: dict):
        r = self.steps_table.rowCount()
        self.steps_table.insertRow(r)
        self.steps_table.setItem(r, 0, QTableWidgetItem(_ensure_str(step.get("type", "narration"))))
        self.steps_table.setItem(r, 1, QTableWidgetItem(_ensure_str(step.get("speaker", ""))))
        self.steps_table.setItem(r, 2, QTableWidgetItem(_ensure_str(step.get("emote", ""))))
        self.steps_table.setItem(r, 3, QTableWidgetItem(_ensure_str(step.get("text", step.get("line", "")))))
        self.steps_table.setItem(r, 4, QTableWidgetItem(_ensure_str(step.get("durationSeconds", ""))))
        self.steps_table.setCurrentCell(r, 0)

    def _add_step(self):
        self._append_step_row({"type": "narration", "text": "TODO"})

    def _dup_step(self):
        row = self.steps_table.currentRow()
        if row < 0:
            return
        step = {
            "type": self.steps_table.item(row, 0).text() if self.steps_table.item(row, 0) else "narration",
            "speaker": self.steps_table.item(row, 1).text() if self.steps_table.item(row, 1) else "",
            "emote": self.steps_table.item(row, 2).text() if self.steps_table.item(row, 2) else "",
            "text": self.steps_table.item(row, 3).text() if self.steps_table.item(row, 3) else "",
            "durationSeconds": self.steps_table.item(row, 4).text() if self.steps_table.item(row, 4) else "",
        }
        self._append_step_row(step)

    def _del_step(self):
        row = self.steps_table.currentRow()
        if row < 0:
            return
        self.steps_table.removeRow(row)

    def _move_step(self, delta: int):
        row = self.steps_table.currentRow()
        if row < 0:
            return
        new_row = row + delta
        if not (0 <= new_row < self.steps_table.rowCount()):
            return
        def _row_values(r: int) -> List[str]:
            return [
                self.steps_table.item(r, c).text() if self.steps_table.item(r, c) else ""
                for c in range(self.steps_table.columnCount())
            ]
        a = _row_values(row)
        b = _row_values(new_row)
        for c, v in enumerate(b):
            self.steps_table.setItem(row, c, QTableWidgetItem(v))
        for c, v in enumerate(a):
            self.steps_table.setItem(new_row, c, QTableWidgetItem(v))
        self.steps_table.setCurrentCell(new_row, 0)

    def _accept(self):
        sid = self.id_edit.text().strip()
        if not sid:
            QMessageBox.warning(self, "Missing ID", "Scene ID is required.")
            return
        if sid != self._orig_id and sid in self._existing:
            QMessageBox.warning(self, "Duplicate ID", f"Scene '{sid}' already exists.")
            return
        title = self.title_edit.text().strip()
        if not title:
            QMessageBox.warning(self, "Missing Title", "Scene title is required.")
            return

        steps: List[dict] = []
        for r in range(self.steps_table.rowCount()):
            t = self.steps_table.item(r, 0).text().strip() if self.steps_table.item(r, 0) else ""
            sp = self.steps_table.item(r, 1).text().strip() if self.steps_table.item(r, 1) else ""
            emote = self.steps_table.item(r, 2).text().strip() if self.steps_table.item(r, 2) else ""
            tx = self.steps_table.item(r, 3).text().strip() if self.steps_table.item(r, 3) else ""
            dur_raw = self.steps_table.item(r, 4).text().strip() if self.steps_table.item(r, 4) else ""
            if not t:
                t = "narration"
            if not tx:
                continue
            step: Dict[str, Any] = {"type": t, "text": tx}
            if sp:
                step["speaker"] = sp
            if emote:
                step["emote"] = emote
            if dur_raw:
                try:
                    step["durationSeconds"] = float(dur_raw)
                except Exception:
                    # keep as raw text if user typed something odd
                    step["durationSeconds"] = dur_raw
            steps.append(step)

        if not steps:
            if QMessageBox.question(self, "Empty Scene", "No valid steps found. Save empty scene anyway?") != QMessageBox.Yes:
                return

        base = dict(self._data)
        base["id"] = sid
        base["title"] = title
        base["steps"] = steps
        self._data = base
        self.accept()

    def value(self) -> dict:
        return dict(self._data)


class TutorialScriptQuickEdit(QDialog):
    def __init__(self, parent: QWidget, *, existing_ids: Iterable[str], data: Optional[dict] = None):
        super().__init__(parent)
        self.setWindowTitle("Quick Edit — Tutorial Script")
        self.resize(860, 560)
        self._existing = set(existing_ids)
        self._orig_id = (data or {}).get("id") if isinstance(data, dict) else None
        self._data = dict(data) if isinstance(data, dict) else {}

        layout = QVBoxLayout(self)
        form = QFormLayout()

        self.id_edit = QLineEdit(_ensure_str(self._data.get("id")))
        form.addRow("ID:", self.id_edit)
        layout.addLayout(form)

        self.steps_table = QTableWidget()
        self.steps_table.setColumnCount(4)
        self.steps_table.setHorizontalHeaderLabels(["key", "context", "message", "delay_ms"])
        self.steps_table.horizontalHeader().setStretchLastSection(True)
        self.steps_table.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.steps_table.setSelectionMode(QAbstractItemView.SingleSelection)
        layout.addWidget(self.steps_table, 1)

        btn_row = QHBoxLayout()
        b_add = QPushButton("Add Step")
        b_dup = QPushButton("Duplicate")
        b_del = QPushButton("Delete")
        b_up = QPushButton("▲")
        b_dn = QPushButton("▼")
        btn_row.addWidget(b_add)
        btn_row.addWidget(b_dup)
        btn_row.addWidget(b_del)
        btn_row.addStretch(1)
        btn_row.addWidget(b_up)
        btn_row.addWidget(b_dn)
        layout.addLayout(btn_row)

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self._accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

        b_add.clicked.connect(self._add_step)
        b_dup.clicked.connect(self._dup_step)
        b_del.clicked.connect(self._del_step)
        b_up.clicked.connect(lambda: self._move_step(-1))
        b_dn.clicked.connect(lambda: self._move_step(+1))

        self._load_steps()

    def _load_steps(self):
        steps = self._data.get("steps") or []
        if not isinstance(steps, list):
            steps = []
        self.steps_table.setRowCount(0)
        for step in steps:
            if not isinstance(step, dict):
                continue
            self._append_step_row(step)
        if self.steps_table.rowCount() == 0:
            self._append_step_row({"message": "TODO: write tutorial step.", "context": ""})

    def _append_step_row(self, step: dict):
        r = self.steps_table.rowCount()
        self.steps_table.insertRow(r)
        self.steps_table.setItem(r, 0, QTableWidgetItem(_ensure_str(step.get("key", ""))))
        self.steps_table.setItem(r, 1, QTableWidgetItem(_ensure_str(step.get("context", ""))))
        self.steps_table.setItem(r, 2, QTableWidgetItem(_ensure_str(step.get("message", ""))))
        self.steps_table.setItem(r, 3, QTableWidgetItem(_ensure_str(step.get("delay_ms", ""))))
        self.steps_table.setCurrentCell(r, 2)

    def _add_step(self):
        self._append_step_row({"message": "TODO", "context": ""})

    def _dup_step(self):
        row = self.steps_table.currentRow()
        if row < 0:
            return
        step = {
            "key": self.steps_table.item(row, 0).text() if self.steps_table.item(row, 0) else "",
            "context": self.steps_table.item(row, 1).text() if self.steps_table.item(row, 1) else "",
            "message": self.steps_table.item(row, 2).text() if self.steps_table.item(row, 2) else "",
            "delay_ms": self.steps_table.item(row, 3).text() if self.steps_table.item(row, 3) else "",
        }
        self._append_step_row(step)

    def _del_step(self):
        row = self.steps_table.currentRow()
        if row < 0:
            return
        self.steps_table.removeRow(row)

    def _move_step(self, delta: int):
        row = self.steps_table.currentRow()
        if row < 0:
            return
        new_row = row + delta
        if not (0 <= new_row < self.steps_table.rowCount()):
            return

        def _row_values(r: int) -> List[str]:
            return [
                self.steps_table.item(r, c).text() if self.steps_table.item(r, c) else ""
                for c in range(4)
            ]

        a = _row_values(row)
        b = _row_values(new_row)
        for c, v in enumerate(b):
            self.steps_table.setItem(row, c, QTableWidgetItem(v))
        for c, v in enumerate(a):
            self.steps_table.setItem(new_row, c, QTableWidgetItem(v))
        self.steps_table.setCurrentCell(new_row, 2)

    def _accept(self):
        tid = self.id_edit.text().strip()
        if not tid:
            QMessageBox.warning(self, "Missing ID", "Tutorial script ID is required.")
            return
        if tid != self._orig_id and tid in self._existing:
            QMessageBox.warning(self, "Duplicate ID", f"Tutorial script '{tid}' already exists.")
            return

        steps: List[dict] = []
        for r in range(self.steps_table.rowCount()):
            key = self.steps_table.item(r, 0).text().strip() if self.steps_table.item(r, 0) else ""
            ctx = self.steps_table.item(r, 1).text().strip() if self.steps_table.item(r, 1) else ""
            msg = self.steps_table.item(r, 2).text().strip() if self.steps_table.item(r, 2) else ""
            delay_raw = self.steps_table.item(r, 3).text().strip() if self.steps_table.item(r, 3) else ""
            if not msg:
                continue
            step: Dict[str, Any] = {"message": msg}
            if key:
                step["key"] = key
            if ctx:
                step["context"] = ctx
            if delay_raw:
                try:
                    step["delay_ms"] = int(float(delay_raw))
                except Exception:
                    step["delay_ms"] = delay_raw
            steps.append(step)

        if not steps:
            if QMessageBox.question(self, "Empty Script", "No valid steps found. Save empty script anyway?") != QMessageBox.Yes:
                return

        base = dict(self._data)
        base["id"] = tid
        base["steps"] = steps
        self._data = base
        self.accept()

    def value(self) -> dict:
        return dict(self._data)


class StageQuickEdit(QDialog):
    def __init__(self, parent: QWidget, *, data: dict):
        super().__init__(parent)
        self.setWindowTitle("Quick Edit — Quest Stage")
        self.resize(720, 520)
        self._data = dict(data) if isinstance(data, dict) else {}

        layout = QVBoxLayout(self)
        form = QFormLayout()

        self.id_edit = QLineEdit(_ensure_str(self._data.get("id")))
        self.id_edit.setReadOnly(True)
        self.title_edit = QLineEdit(_ensure_str(self._data.get("title")))
        self.desc_edit = QPlainTextEdit(_ensure_str(self._data.get("description")))

        form.addRow("Stage ID:", self.id_edit)
        form.addRow("Title:", self.title_edit)
        form.addRow("Description:", self.desc_edit)
        layout.addLayout(form)

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self._accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def _accept(self):
        title = self.title_edit.text().strip()
        if not title:
            QMessageBox.warning(self, "Missing Title", "Stage title is required.")
            return
        base = dict(self._data)
        base["title"] = title
        base["description"] = self.desc_edit.toPlainText().strip()
        self._data = base
        self.accept()

    def value(self) -> dict:
        return dict(self._data)


class TaskQuickEdit(QDialog):
    def __init__(
        self,
        parent: QWidget,
        *,
        existing_ids: Iterable[str],
        data: Optional[dict] = None,
        tutorial_ids: Sequence[str] = (),
    ):
        super().__init__(parent)
        self.setWindowTitle("Quick Edit — Quest Task")
        self.resize(720, 520)
        self._existing = set(existing_ids)
        self._orig_id = (data or {}).get("id") if isinstance(data, dict) else None
        self._data = dict(data) if isinstance(data, dict) else {}

        layout = QVBoxLayout(self)
        form = QFormLayout()

        self.id_edit = QLineEdit(_ensure_str(self._data.get("id")))
        self.text_edit = QPlainTextEdit(_ensure_str(self._data.get("text")))
        self.done_chk = QCheckBox("Done by default (dev-only)")
        self.done_chk.setChecked(bool(self._data.get("done", False)))

        self.tutorial_combo = QComboBox()
        self.tutorial_combo.setEditable(True)
        self.tutorial_combo.setInsertPolicy(QComboBox.NoInsert)
        values = [""] + list(tutorial_ids)
        self.tutorial_combo.addItems(values)
        self.tutorial_combo.setCurrentText(_ensure_str(self._data.get("tutorial_id")))
        self.tutorial_combo.setCompleter(_make_completer(values))

        form.addRow("Task ID:", self.id_edit)
        form.addRow("Text:", self.text_edit)
        form.addRow("Tutorial Script ID (optional):", self.tutorial_combo)
        form.addRow("", self.done_chk)
        layout.addLayout(form)

        buttons = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        buttons.accepted.connect(self._accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def _accept(self):
        tid = self.id_edit.text().strip()
        if not tid:
            QMessageBox.warning(self, "Missing ID", "Task ID is required.")
            return
        if tid != self._orig_id and tid in self._existing:
            QMessageBox.warning(self, "Duplicate ID", f"Task '{tid}' already exists in this quest.")
            return
        text = self.text_edit.toPlainText().strip()
        if not text:
            QMessageBox.warning(self, "Missing Text", "Task text cannot be empty.")
            return

        base = dict(self._data)
        base["id"] = tid
        base["text"] = text
        base["done"] = bool(self.done_chk.isChecked())

        tut = self.tutorial_combo.currentText().strip()
        if tut:
            base["tutorial_id"] = tut
        else:
            base.pop("tutorial_id", None)

        self._data = base
        self.accept()

    def value(self) -> dict:
        return dict(self._data)


@dataclass(frozen=True)
class RefJump:
    kind: str  # dialogue/event/cutscene/tutorial/milestone/quest
    ident: str


class NarrativeStudio(QWidget):
    """
    Studio Pro plugin widget.
    """

    def __init__(self, project_root: Optional[Path] = None):
        super().__init__()
        self._base_title = "Starborn Narrative Studio"
        self.setWindowTitle(self._base_title)
        self.resize(1500, 900)

        paths = resolve_paths(project_root or Path(__file__).parent)
        self.project_root = paths.project_root
        self.assets_root = paths.assets_dir

        # Paths
        self.quest_path = self.assets_root / "quests.json"
        self.dialogue_path = self.assets_root / "dialogue.json"
        self.events_path = self.assets_root / "events.json"
        self.cinematics_path = self.assets_root / "cinematics.json"
        self.milestones_path = self.assets_root / "milestones.json"
        self.tutorial_scripts_path = self.assets_root / "tutorial_scripts.json"
        self.items_path = self.assets_root / "items.json"
        self.npcs_path = self.assets_root / "npcs.json"
        self.rooms_path = self.assets_root / "rooms.json"
        self.flow_path = self.assets_root / FLOW_FILENAME

        # Data (in-memory indices)
        self.quests: List[dict] = []
        self.quests_by_id: Dict[str, dict] = {}
        self.dialogues_by_id: Dict[str, dict] = {}
        self.events_by_id: Dict[str, dict] = {}
        self.cinematics_by_id: Dict[str, dict] = {}
        self.milestones_by_id: Dict[str, dict] = {}
        self.tutorial_scripts_by_id: Dict[str, dict] = {}
        self.items_by_id: Dict[str, dict] = {}
        self.rooms_by_id: Dict[str, dict] = {}
        self.npc_names: List[str] = []
        self.npc_ids: List[str] = []
        self.player_action_ids: List[str] = []
        self.flows: Dict[str, Dict[str, List[dict]]] = {}

        self._dirty_any = False
        self._dirty: Dict[str, bool] = {
            "quests": False,
            "flows": False,
            "dialogue": False,
            "events": False,
            "cinematics": False,
            "milestones": False,
            "tutorial_scripts": False,
        }

        # Current focus
        self.current_quest_id: Optional[str] = None
        self.current_stage_id: Optional[str] = None

        # UI state guards
        self._updating_ui = False
        self._beat_form_dirty = False
        self._last_selected_beat: Optional[dict] = None

        # Standalone helpers (when not embedded in Studio Pro)
        self._standalone_windows: Dict[str, QWidget] = {}

        # Widgets (assigned in _build_ui)
        self.quest_search: QLineEdit = None  # type: ignore[assignment]
        self.quest_list: QListWidget = None  # type: ignore[assignment]
        self.stage_list: QListWidget = None  # type: ignore[assignment]
        self.flow_list: FlowListWidget = None  # type: ignore[assignment]
        self.focus_label: QLabel = None  # type: ignore[assignment]

        # Beat editor widgets
        self.beat_type_combo: QComboBox = None  # type: ignore[assignment]
        self.ref_combo: QComboBox = None  # type: ignore[assignment]
        self.beat_label_edit: QLineEdit = None  # type: ignore[assignment]
        self.note_edit: QPlainTextEdit = None  # type: ignore[assignment]
        self.beat_preview: QPlainTextEdit = None  # type: ignore[assignment]
        self.beat_apply_btn: QPushButton = None  # type: ignore[assignment]
        self.beat_open_btn: QPushButton = None  # type: ignore[assignment]
        self.beat_create_btn: QPushButton = None  # type: ignore[assignment]

        # Context widgets
        self.stage_desc: QPlainTextEdit = None  # type: ignore[assignment]
        self.task_table: QTableWidget = None  # type: ignore[assignment]
        self.stage_edit_btn: QPushButton = None  # type: ignore[assignment]
        self.task_add_btn: QPushButton = None  # type: ignore[assignment]
        self.task_edit_btn: QPushButton = None  # type: ignore[assignment]
        self.task_del_btn: QPushButton = None  # type: ignore[assignment]
        self.task_up_btn: QPushButton = None  # type: ignore[assignment]
        self.task_dn_btn: QPushButton = None  # type: ignore[assignment]
        self.task_tut_btn: QPushButton = None  # type: ignore[assignment]
        self.refs_list: QListWidget = None  # type: ignore[assignment]

        # Tools widgets
        self.validation_out: QPlainTextEdit = None  # type: ignore[assignment]
        self.events_normalize_btn: QPushButton = None  # type: ignore[assignment]
        self.where_kind_combo: QComboBox = None  # type: ignore[assignment]
        self.where_id_combo: QComboBox = None  # type: ignore[assignment]
        self.where_new_id_edit: QLineEdit = None  # type: ignore[assignment]
        self.where_find_btn: QPushButton = None  # type: ignore[assignment]
        self.where_rename_btn: QPushButton = None  # type: ignore[assignment]
        self.where_out: QListWidget = None  # type: ignore[assignment]

        self._load_all()
        self._build_ui()
        self._refresh_quest_list()
        self._update_title()

        attach_hotkeys(self, save_cb=self.save, search_cb=self._focus_quest_search)
        # Only bind Delete when the beat list has focus (avoid deleting beats while typing).
        QShortcut(QKeySequence.Delete, self.flow_list, self._delete_selected_beat)

    # -------------------- Studio hooks --------------------

    def save(self) -> bool:
        return self._save_dirty()

    def refresh_references(self):
        """
        Called by Studio Pro "Refresh Refs" toolbar action when present.
        """
        if self._dirty_any:
            flash_status(self, "Not reloading refs: you have unsaved changes.", 1800)
            return
        self._load_indices_only()
        self._refresh_flow_list()
        self._refresh_context()
        flash_status(self, "Narrative Studio: refreshed references.", 1200)

    def select_id(self, ident: str):
        """
        Best-effort selection hook for Studio goto.
        Accepts:
          - quest_id
          - quest_id#stage_id
          - quest_id:stage_id
        """
        ident = (ident or "").strip()
        if not ident:
            return
        quest_id, stage_id = ident, None
        for sep in ("#", ":"):
            if sep in ident:
                quest_id, stage_id = ident.split(sep, 1)
                quest_id, stage_id = quest_id.strip(), stage_id.strip()
                break
        if quest_id:
            self._select_quest_by_id(quest_id)
        if stage_id:
            self._select_stage_by_id(stage_id)

    # -------------------- Data loading/saving --------------------

    def _load_all(self):
        self._load_indices_only()
        try:
            flows = json_load(self.flow_path, default={}) or {}
        except Exception:
            flows = {}
        self.flows = flows if isinstance(flows, dict) else {}

    def _load_indices_only(self):
        quests_raw = _safe_list_json(self.quest_path)
        quests: List[dict] = []
        for q in quests_raw:
            qid = str(q.get("id") or "").strip()
            if qid:
                quests.append(q)
        self.quests = quests
        self.quests_by_id = {q["id"]: q for q in self.quests if isinstance(q, dict) and q.get("id")}

        self.dialogues_by_id = _index_by_id(_safe_list_json(self.dialogue_path))
        self.events_by_id = _index_by_id(_safe_list_json(self.events_path))
        self.cinematics_by_id = _index_by_id(_safe_list_json(self.cinematics_path))
        self.milestones_by_id = _index_by_id(_safe_list_json(self.milestones_path))
        self.tutorial_scripts_by_id = _index_by_id(_safe_list_json(self.tutorial_scripts_path))

        self.items_by_id = _index_by_id(_safe_list_json(self.items_path))

        rooms_raw = _safe_list_json(self.rooms_path)
        self.rooms_by_id = _index_by_id(rooms_raw)

        npc_names: set[str] = set()
        npc_ids: set[str] = set()
        for npc in _safe_list_json(self.npcs_path):
            nid = str((npc or {}).get("id") or "").strip()
            if nid:
                npc_ids.add(nid)
            nm = str((npc or {}).get("name") or nid or "").strip()
            if nm:
                npc_names.add(nm)
            aliases = (npc or {}).get("aliases") or []
            if isinstance(aliases, list):
                for a in aliases:
                    a = str(a).strip()
                    if a:
                        npc_names.add(a)
        self.npc_names = sorted(npc_names, key=str.lower)
        self.npc_ids = sorted(npc_ids, key=str.lower)

        player_actions: set[str] = set()
        for room in rooms_raw:
            if not isinstance(room, dict):
                continue
            actions = room.get("actions") or []
            if not isinstance(actions, list):
                continue
            for action in actions:
                if not isinstance(action, dict):
                    continue
                for k in ("action_event", "action_event_on", "action_event_off"):
                    v = action.get(k)
                    if isinstance(v, str) and v.strip():
                        player_actions.add(v.strip())
        self.player_action_ids = sorted(player_actions, key=str.lower)

    def _save_dirty(self) -> bool:
        if not self._dirty_any:
            flash_status(self, "Nothing to save.", 1200)
            return True

        try:
            if self._dirty.get("quests"):
                json_save(self.quest_path, self.quests, sort_obj=True, indent=2)
            if self._dirty.get("flows"):
                json_save(self.flow_path, self.flows, indent=2)
            if self._dirty.get("dialogue"):
                rows = [self.dialogues_by_id[k] for k in _sorted_ids(self.dialogues_by_id)]
                json_save(self.dialogue_path, rows, indent=2)
            if self._dirty.get("events"):
                rows = [self.events_by_id[k] for k in _sorted_ids(self.events_by_id)]
                json_save(self.events_path, rows, indent=2)
            if self._dirty.get("cinematics"):
                rows = [self.cinematics_by_id[k] for k in _sorted_ids(self.cinematics_by_id)]
                json_save(self.cinematics_path, rows, indent=2)
            if self._dirty.get("milestones"):
                rows = [self.milestones_by_id[k] for k in _sorted_ids(self.milestones_by_id)]
                json_save(self.milestones_path, rows, indent=2)
            if self._dirty.get("tutorial_scripts"):
                rows = [self.tutorial_scripts_by_id[k] for k in _sorted_ids(self.tutorial_scripts_by_id)]
                json_save(self.tutorial_scripts_path, rows, indent=2)
        except Exception as exc:
            QMessageBox.critical(self, "Save Failed", f"Could not save narrative data:\n{exc}")
            return False

        for k in list(self._dirty.keys()):
            self._dirty[k] = False
        self._dirty_any = False
        self._update_title()
        flash_status(self, "Saved narrative changes.", 1400)
        return True

    # -------------------- UI --------------------

    def _build_ui(self):
        root = QVBoxLayout(self)
        root.setContentsMargins(10, 10, 10, 10)
        root.setSpacing(8)

        # Header
        header = QHBoxLayout()
        title = QLabel("Narrative Studio")
        title.setStyleSheet("font-size: 20px; font-weight: 650;")
        sub = QLabel("Manage quests, dialogue, cutscenes, and events in one place.")
        # sub.setStyleSheet("color: #6f6f6f;")
        layout.addWidget(title)
        header_left = QVBoxLayout()
        header_left.addWidget(title)
        header_left.addWidget(sub)
        header.addLayout(header_left, 1)

        self.focus_label = QLabel("Focus: (select a quest stage)")
        # self.focus_label.setStyleSheet("color: #6f6f6f;")
        header.addWidget(self.focus_label)

        b_save = QPushButton("Save")
        b_save.setShortcut("Ctrl+S")
        b_save.clicked.connect(self.save)
        header.addWidget(b_save)

        root.addLayout(header)

        splitter = QSplitter(Qt.Horizontal)
        root.addWidget(splitter, 1)

        # Left: navigator
        nav = QWidget()
        nav.setMinimumWidth(330)
        nav_l = QVBoxLayout(nav)
        nav_l.setContentsMargins(0, 0, 0, 0)

        self.quest_search = QLineEdit()
        self.quest_search.setPlaceholderText("Search quests (id/title/summary)…")
        self.quest_search.textChanged.connect(self._refresh_quest_list)
        nav_l.addWidget(self.quest_search)

        self.quest_list = QListWidget()
        self.quest_list.itemSelectionChanged.connect(self._on_select_quest)
        nav_l.addWidget(self.quest_list, 1)

        nav_l.addWidget(QLabel("Stages:"))
        self.stage_list = QListWidget()
        self.stage_list.itemSelectionChanged.connect(self._on_select_stage)
        nav_l.addWidget(self.stage_list, 1)

        nav_btns = QHBoxLayout()
        b_open_quest = QPushButton("Open Quest Editor")
        b_open_quest.clicked.connect(self._goto_current_quest)
        nav_btns.addWidget(b_open_quest)
        nav_l.addLayout(nav_btns)

        splitter.addWidget(nav)

        # Middle: flow list
        flow_panel = QWidget()
        flow_l = QVBoxLayout(flow_panel)
        flow_l.setContentsMargins(0, 0, 0, 0)

        flow_hdr = QHBoxLayout()
        flow_hdr.addWidget(QLabel("Beat Flow"))
        flow_hdr.addStretch(1)

        b_add = QToolButton()
        b_add.setText("Add…")
        add_menu = QMenu(b_add)
        for t in BEAT_TYPES:
            add_menu.addAction(t.title(), lambda tt=t: self._add_beat(tt))
        b_add.setMenu(add_menu)
        b_add.setPopupMode(QToolButton.InstantPopup)
        flow_hdr.addWidget(b_add)

        b_dup = QPushButton("Duplicate")
        b_dup.clicked.connect(self._duplicate_selected_beat)
        flow_hdr.addWidget(b_dup)

        b_del = QPushButton("Delete")
        b_del.clicked.connect(self._delete_selected_beat)
        flow_hdr.addWidget(b_del)

        b_up = QPushButton("▲")
        b_dn = QPushButton("▼")
        b_up.clicked.connect(lambda: self._move_selected_beat(-1))
        b_dn.clicked.connect(lambda: self._move_selected_beat(+1))
        flow_hdr.addWidget(b_up)
        flow_hdr.addWidget(b_dn)

        flow_l.addLayout(flow_hdr)

        self.flow_list = FlowListWidget()
        self.flow_list.itemSelectionChanged.connect(self._on_select_beat)
        self.flow_list.itemDoubleClicked.connect(self._open_selected_reference)
        self.flow_list.on_reordered = self._on_flow_reordered
        flow_l.addWidget(self.flow_list, 1)

        splitter.addWidget(flow_panel)

        # Right: tabs (Beat / Context / Tools)
        right = QTabWidget()
        splitter.addWidget(right)
        splitter.setStretchFactor(1, 1)
        splitter.setStretchFactor(2, 1)

        right.addTab(self._build_tab_beat(), "Beat")
        right.addTab(self._build_tab_context(), "Context")
        right.addTab(self._build_tab_tools(), "Tools")

        # Status bar (visible even though we're a QWidget tab in Studio Pro)
        self._status_bar = QStatusBar()
        root.addWidget(self._status_bar)

    def _build_tab_beat(self) -> QWidget:
        w = QWidget()
        l = QVBoxLayout(w)

        box = QGroupBox("Selected Beat")
        form = QFormLayout(box)

        self.beat_type_combo = QComboBox()
        self.beat_type_combo.addItems(list(BEAT_TYPES))
        self.beat_type_combo.currentTextChanged.connect(self._on_beat_type_changed)

        self.ref_combo = QComboBox()
        self.ref_combo.setEditable(True)
        self.ref_combo.setInsertPolicy(QComboBox.NoInsert)
        self.ref_combo.lineEdit().textEdited.connect(self._on_beat_form_changed)  # type: ignore[union-attr]
        self.ref_combo.currentTextChanged.connect(self._on_beat_form_changed)

        self.beat_label_edit = QLineEdit()
        self.beat_label_edit.textEdited.connect(self._on_beat_form_changed)

        self.note_edit = QPlainTextEdit()
        self.note_edit.textChanged.connect(self._on_beat_form_changed)

        form.addRow("Type:", self.beat_type_combo)
        form.addRow("Ref ID:", self.ref_combo)
        form.addRow("Label:", self.beat_label_edit)
        form.addRow("Note Text:", self.note_edit)

        btns = QHBoxLayout()
        self.beat_apply_btn = QPushButton("Apply")
        self.beat_apply_btn.clicked.connect(self._apply_beat_form)
        self.beat_open_btn = QPushButton("Open…")
        self.beat_open_btn.clicked.connect(self._open_selected_reference)
        self.beat_create_btn = QPushButton("Create/Quick Edit…")
        self.beat_create_btn.clicked.connect(self._create_or_edit_reference)
        btns.addWidget(self.beat_apply_btn)
        btns.addWidget(self.beat_open_btn)
        btns.addWidget(self.beat_create_btn)
        btns.addStretch(1)
        l.addWidget(box)
        l.addLayout(btns)

        prev_box = QGroupBox("Preview")
        prev_l = QVBoxLayout(prev_box)
        self.beat_preview = QPlainTextEdit()
        self.beat_preview.setReadOnly(True)
        prev_l.addWidget(self.beat_preview)
        l.addWidget(prev_box, 1)

        self._set_beat_form_enabled(False)
        return w

    def _build_tab_context(self) -> QWidget:
        w = QWidget()
        l = QVBoxLayout(w)

        box = QGroupBox("Stage Snapshot")
        inner = QVBoxLayout(box)

        self.stage_desc = QPlainTextEdit()
        self.stage_desc.setReadOnly(True)
        self.stage_desc.setPlaceholderText("Select a quest stage to see its description.")
        desc_hdr = QHBoxLayout()
        desc_hdr.addWidget(QLabel("Description:"))
        desc_hdr.addStretch(1)
        self.stage_edit_btn = QPushButton("Edit Stage…")
        self.stage_edit_btn.clicked.connect(self._edit_current_stage)
        desc_hdr.addWidget(self.stage_edit_btn)
        inner.addLayout(desc_hdr)
        inner.addWidget(self.stage_desc, 1)

        self.task_table = QTableWidget()
        self.task_table.setColumnCount(3)
        self.task_table.setHorizontalHeaderLabels(["task_id", "text", "tutorial_id"])
        self.task_table.horizontalHeader().setStretchLastSection(True)
        self.task_table.setEditTriggers(QAbstractItemView.NoEditTriggers)
        self.task_table.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.task_table.setSelectionMode(QAbstractItemView.SingleSelection)
        self.task_table.cellDoubleClicked.connect(self._on_task_double_click)
        self.task_table.itemSelectionChanged.connect(self._update_task_buttons)
        tasks_hdr = QHBoxLayout()
        tasks_hdr.addWidget(QLabel("Tasks:"))
        tasks_hdr.addStretch(1)
        self.task_add_btn = QPushButton("Add…")
        self.task_edit_btn = QPushButton("Edit…")
        self.task_del_btn = QPushButton("Delete")
        self.task_tut_btn = QPushButton("Open Tutorial…")
        self.task_up_btn = QPushButton("▲")
        self.task_dn_btn = QPushButton("▼")
        self.task_add_btn.clicked.connect(self._add_task)
        self.task_edit_btn.clicked.connect(self._edit_selected_task)
        self.task_del_btn.clicked.connect(self._delete_selected_task)
        self.task_tut_btn.clicked.connect(self._open_selected_task_tutorial)
        self.task_up_btn.clicked.connect(lambda: self._move_selected_task(-1))
        self.task_dn_btn.clicked.connect(lambda: self._move_selected_task(+1))
        tasks_hdr.addWidget(self.task_add_btn)
        tasks_hdr.addWidget(self.task_edit_btn)
        tasks_hdr.addWidget(self.task_del_btn)
        tasks_hdr.addWidget(self.task_tut_btn)
        tasks_hdr.addWidget(self.task_up_btn)
        tasks_hdr.addWidget(self.task_dn_btn)
        inner.addLayout(tasks_hdr)
        inner.addWidget(self.task_table, 1)

        l.addWidget(box, 2)

        refs = QGroupBox("Referenced Assets (double-click to open)")
        refs_l = QVBoxLayout(refs)
        self.refs_list = QListWidget()
        self.refs_list.itemDoubleClicked.connect(self._open_ref_from_context)
        refs_l.addWidget(self.refs_list)
        l.addWidget(refs, 1)

        return w

    def _build_tab_tools(self) -> QWidget:
        w = QWidget()
        l = QVBoxLayout(w)

        btns = QHBoxLayout()
        b_val_stage = QPushButton("Validate Stage")
        b_val_all = QPushButton("Validate All Flows")
        self.events_normalize_btn = QPushButton("Normalize Events…")
        b_export = QPushButton("Export Stage Markdown…")
        b_val_stage.clicked.connect(self._validate_current_stage)
        b_val_all.clicked.connect(self._validate_all_flows)
        self.events_normalize_btn.clicked.connect(self._normalize_events_schema)
        b_export.clicked.connect(self._export_stage_markdown)
        btns.addWidget(b_val_stage)
        btns.addWidget(b_val_all)
        btns.addWidget(self.events_normalize_btn)
        btns.addStretch(1)
        btns.addWidget(b_export)
        l.addLayout(btns)

        where_box = QGroupBox("Where Used / Safe Rename")
        where_l = QVBoxLayout(where_box)
        row = QHBoxLayout()
        self.where_kind_combo = QComboBox()
        self.where_kind_combo.addItems(
            ["dialogue", "event", "cutscene", "tutorial", "milestone", "quest", "quest_stage", "quest_task", "room", "item", "npc", "player_action"]
        )
        self.where_id_combo = QComboBox()
        self.where_id_combo.setEditable(True)
        self.where_id_combo.setInsertPolicy(QComboBox.NoInsert)
        self.where_find_btn = QPushButton("Find")
        row.addWidget(QLabel("Type:"))
        row.addWidget(self.where_kind_combo)
        row.addWidget(QLabel("ID:"))
        row.addWidget(self.where_id_combo, 1)
        row.addWidget(self.where_find_btn)
        where_l.addLayout(row)

        row2 = QHBoxLayout()
        self.where_new_id_edit = QLineEdit()
        self.where_new_id_edit.setPlaceholderText("new id…")
        self.where_rename_btn = QPushButton("Rename")
        row2.addWidget(QLabel("New ID:"))
        row2.addWidget(self.where_new_id_edit, 1)
        row2.addWidget(self.where_rename_btn)
        where_l.addLayout(row2)

        self.where_out = QListWidget()
        self.where_out.setAlternatingRowColors(True)
        self.where_out.setMaximumHeight(190)
        where_l.addWidget(self.where_out)
        l.addWidget(where_box)

        self.validation_out = QPlainTextEdit()
        self.validation_out.setReadOnly(True)
        self.validation_out.setPlaceholderText("Validation output appears here.")
        l.addWidget(self.validation_out, 1)

        self.where_kind_combo.currentTextChanged.connect(self._where_used_kind_changed)
        self.where_find_btn.clicked.connect(self._where_used_find)
        self.where_rename_btn.clicked.connect(self._where_used_rename)
        self.where_out.itemDoubleClicked.connect(self._open_where_used_hit)
        self._where_used_kind_changed(self.where_kind_combo.currentText())
        return w

    def _where_used_kind_changed(self, kind: str):
        if not self.where_id_combo:
            return
        kind = (kind or "").strip().lower()
        if kind == "dialogue":
            ids = _sorted_ids(self.dialogues_by_id)
        elif kind == "event":
            ids = _sorted_ids(self.events_by_id)
        elif kind == "cutscene":
            ids = _sorted_ids(self.cinematics_by_id)
        elif kind == "tutorial":
            ids = _sorted_ids(self.tutorial_scripts_by_id)
        elif kind == "milestone":
            ids = _sorted_ids(self.milestones_by_id)
        elif kind == "quest":
            ids = _sorted_ids(self.quests_by_id)
        elif kind == "quest_stage":
            keys: List[str] = []
            for qid, q in self.quests_by_id.items():
                stages = (q or {}).get("stages") or []
                if not isinstance(stages, list):
                    continue
                for idx, st in enumerate(stages):
                    if not isinstance(st, dict):
                        continue
                    sid = str(st.get("id") or f"stage_{idx}").strip()
                    if sid:
                        keys.append(f"{qid}:{sid}")
            ids = sorted(set(keys), key=str.lower)
        elif kind == "quest_task":
            keys = []
            for qid, q in self.quests_by_id.items():
                stages = (q or {}).get("stages") or []
                if not isinstance(stages, list):
                    continue
                for st in stages:
                    if not isinstance(st, dict):
                        continue
                    tasks = (st or {}).get("tasks") or []
                    if not isinstance(tasks, list):
                        continue
                    for task in tasks:
                        if not isinstance(task, dict):
                            continue
                        tid = str(task.get("id") or "").strip()
                        if tid:
                            keys.append(f"{qid}:{tid}")
            ids = sorted(set(keys), key=str.lower)
        elif kind == "room":
            ids = _sorted_ids(self.rooms_by_id)
        elif kind == "item":
            ids = _sorted_ids(self.items_by_id)
        elif kind == "npc":
            ids = sorted(set(self.npc_ids), key=str.lower)
        elif kind == "player_action":
            ids = sorted(set(self.player_action_ids), key=str.lower)
        else:
            ids = []

        cur = self.where_id_combo.currentText().strip()
        self.where_id_combo.blockSignals(True)
        try:
            self.where_id_combo.clear()
            self.where_id_combo.addItems([""] + ids)
            if cur:
                self.where_id_combo.setCurrentText(cur)
            self.where_id_combo.setCompleter(_make_completer([""] + ids))
        finally:
            self.where_id_combo.blockSignals(False)
        if self.where_out:
            self.where_out.clear()
        if self.where_rename_btn:
            self.where_rename_btn.setEnabled(kind in ("dialogue", "event", "cutscene", "tutorial", "milestone"))

    def _where_used_find(self):
        self._commit_beat_form_if_needed()
        kind = self.where_kind_combo.currentText().strip().lower() if self.where_kind_combo else ""
        ident = self.where_id_combo.currentText().strip() if self.where_id_combo else ""
        if not kind or not ident:
            if self.where_out:
                self.where_out.clear()
                self.where_out.addItem("Pick a Type + ID first.")
            return
        hits = self._where_used_scan(kind, ident)
        if self.where_out:
            self.where_out.clear()
            if not hits:
                self.where_out.addItem("(no references found)")
                return
            for label, meta in hits:
                it = QListWidgetItem(label)
                if isinstance(meta, dict):
                    it.setData(Qt.UserRole, meta)
                self.where_out.addItem(it)

    def _where_used_rename(self):
        self._commit_beat_form_if_needed()
        kind = self.where_kind_combo.currentText().strip().lower() if self.where_kind_combo else ""
        old_id = self.where_id_combo.currentText().strip() if self.where_id_combo else ""
        new_id = self.where_new_id_edit.text().strip() if self.where_new_id_edit else ""
        if not kind or not old_id or not new_id:
            QMessageBox.information(self, "Safe Rename", "Type, ID, and New ID are required.")
            return
        if kind not in ("dialogue", "event", "cutscene", "tutorial", "milestone"):
            QMessageBox.information(
                self,
                "Safe Rename",
                "Safe rename is only supported for dialogue/event/cutscene/tutorial/milestone.\n\n"
                "Use Find to locate references for other types.",
            )
            return
        if old_id == new_id:
            return
        if not self._safe_rename_asset(kind, old_id, new_id):
            return

        flash_status(self, f"Renamed {kind} '{old_id}' → '{new_id}'", 2000)
        self._where_used_kind_changed(kind)
        self.where_id_combo.setCurrentText(new_id)
        self.where_new_id_edit.setText("")
        self._refresh_flow_list()
        self._refresh_context()
        self._update_beat_preview()
        self._where_used_find()

    def _open_where_used_hit(self, item: QListWidgetItem):
        meta = item.data(Qt.UserRole) if item else None
        if not isinstance(meta, dict):
            return
        action = str(meta.get("action") or "").strip()
        if not action:
            return
        self._commit_beat_form_if_needed()

        if action == "open_ref":
            kind = str(meta.get("kind") or "").strip()
            ident = str(meta.get("id") or "").strip()
            if kind and ident:
                self._open_reference(kind, ident)
            return

        if action == "focus_quest":
            qid = str(meta.get("quest_id") or "").strip()
            if qid:
                self._select_quest_by_id(qid)
            return

        if action in ("focus_stage", "focus_task"):
            qid = str(meta.get("quest_id") or "").strip()
            sid = str(meta.get("stage_id") or "").strip()
            if qid:
                self._select_quest_by_id(qid)
            if sid:
                self._select_stage_by_id(sid)
            # Ensure UI is populated for the stage.
            try:
                self._refresh_flow_list()
                self._refresh_context()
            except Exception:
                pass

            if action == "focus_stage":
                beat_index = meta.get("beat_index")
                if isinstance(beat_index, int):
                    try:
                        self.flow_list.setCurrentRow(max(0, beat_index))
                    except Exception:
                        pass
                return

            task_id = str(meta.get("task_id") or "").strip()
            if task_id and self.task_table:
                try:
                    for r in range(self.task_table.rowCount()):
                        it0 = self.task_table.item(r, 0)
                        if it0 and it0.text().strip() == task_id:
                            self.task_table.setCurrentCell(r, 0)
                            break
                except Exception:
                    pass
            return

    def _where_used_scan(self, kind: str, ident: str) -> List[Tuple[str, dict]]:
        kind = (kind or "").strip().lower()
        ident = (ident or "").strip()
        if not kind or not ident:
            return []

        hits: List[Tuple[str, dict]] = []
        seen: set[str] = set()

        def add(label: str, meta: dict):
            if not label:
                return
            if label in seen:
                return
            seen.add(label)
            hits.append((label, meta))

        # --- Flows (beats) ---
        if kind in ("dialogue", "event", "cutscene", "tutorial", "milestone"):
            beat_type = kind
            for qid, stages in (self.flows or {}).items():
                if not isinstance(stages, dict):
                    continue
                for sid, beats in stages.items():
                    if not isinstance(beats, list):
                        continue
                    for idx, beat in enumerate(beats):
                        if not isinstance(beat, dict):
                            continue
                        if str(beat.get("type") or "").strip() != beat_type:
                            continue
                        if str(beat.get("id") or "").strip() != ident:
                            continue
                        label = str(beat.get("label") or "").strip()
                        add(
                            f"flow {qid}#{sid} beat {idx + 1}: {beat_type} '{ident}'" + (f" ({label})" if label else ""),
                            {"action": "focus_stage", "quest_id": qid, "stage_id": sid, "beat_index": idx},
                        )

        # --- Dialogue references ---
        def iter_tokens(raw: str):
            toks = [t.strip() for t in (raw or "").split(",") if t.strip()]
            for tok in toks:
                parts = tok.split(":", 1)
                t_raw = parts[0].strip()
                v = parts[1].strip() if len(parts) > 1 else ""
                yield tok, t_raw, v

        def id_part(raw: str) -> str:
            raw = (raw or "").strip()
            if not raw:
                return ""
            for delim in ("*", "x", "|"):
                idx = raw.find(delim)
                if idx > 0:
                    return raw[:idx].strip()
            return raw

        def split_pair(raw: str) -> Tuple[str, str]:
            parts = (raw or "").split(":", 1)
            a = parts[0].strip() if parts else ""
            b = parts[1].strip() if len(parts) > 1 else ""
            return a, b

        qk_a, qk_b = split_pair(ident)  # useful for quest_stage/task lookups

        for did, dlg in self.dialogues_by_id.items():
            if not isinstance(dlg, dict):
                continue

            if kind == "dialogue" and str(dlg.get("next") or "").strip() == ident:
                add(f"dialogue '{did}': next -> '{ident}'", {"action": "open_ref", "kind": "dialogue", "id": did})

            if kind == "npc":
                speaker = str(dlg.get("speaker") or "").strip()
                if speaker and speaker.lower() == ident.lower():
                    add(f"dialogue '{did}': speaker '{speaker}'", {"action": "open_ref", "kind": "dialogue", "id": did})

            cond = str(dlg.get("condition") or "").strip()
            if cond:
                for tok, t_raw, v in iter_tokens(cond):
                    tl = t_raw.lower()
                    if kind == "event" and tl in ("event_completed", "event_not_completed") and v == ident:
                        add(f"dialogue '{did}': condition '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})
                    if kind == "milestone" and tl in ("milestone", "milestone_not_set") and v == ident:
                        add(f"dialogue '{did}': condition '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})
                    if kind == "tutorial" and tl in ("tutorial_completed", "tutorial_not_completed") and v == ident:
                        add(f"dialogue '{did}': condition '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})
                    if kind == "item" and tl in ("item", "item_not") and v == ident:
                        add(f"dialogue '{did}': condition '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})
                    if kind == "quest":
                        if tl in ("quest", "quest_active", "quest_completed", "quest_not_started", "quest_failed") and v == ident:
                            add(f"dialogue '{did}': condition '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})
                        if tl in ("quest_stage", "quest_stage_not") and v.startswith(f"{ident}:"):
                            add(f"dialogue '{did}': condition '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})
                    if kind == "quest_stage" and tl in ("quest_stage", "quest_stage_not") and v == ident:
                        add(f"dialogue '{did}': condition '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})

            trig = str(dlg.get("trigger") or "").strip()
            if trig:
                for tok, t_raw, v in iter_tokens(trig):
                    tl = t_raw.lower()
                    if kind == "milestone" and tl in ("set_milestone", "clear_milestone") and v == ident:
                        add(f"dialogue '{did}': trigger '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})
                    if kind == "cutscene" and tl == "play_cinematic" and v == ident:
                        add(f"dialogue '{did}': trigger '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})
                    if kind == "tutorial" and tl == "system_tutorial":
                        scene = v.split("|", 1)[0].strip() if v else ""
                        if scene == ident:
                            add(f"dialogue '{did}': trigger '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})
                    if kind == "quest":
                        if tl in ("start_quest", "complete_quest", "fail_quest", "track_quest", "advance_quest") and v == ident:
                            add(f"dialogue '{did}': trigger '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})
                        if tl in ("set_quest_task_done", "advance_quest_stage") and v.startswith(f"{ident}:"):
                            add(f"dialogue '{did}': trigger '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})
                    if kind == "quest_stage" and tl == "advance_quest_stage" and v == ident:
                        add(f"dialogue '{did}': trigger '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})
                    if kind == "quest_task" and tl == "set_quest_task_done" and v == ident:
                        add(f"dialogue '{did}': trigger '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})
                    if kind == "item" and tl in ("give_item", "take_item") and id_part(v) == ident:
                        add(f"dialogue '{did}': trigger '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})
                    if kind == "player_action" and tl == "player_action" and v == ident:
                        add(f"dialogue '{did}': trigger '{tok}'", {"action": "open_ref", "kind": "dialogue", "id": did})

        # --- Event references ---
        def walk_actions(actions: Any, base_path: str = "actions"):
            if not isinstance(actions, list):
                return
            for i, act in enumerate(actions):
                p = f"{base_path}[{i}]"
                if isinstance(act, dict):
                    yield act, p
                    for child_key in ("do", "elseDo", "else", "on_complete", "onComplete"):
                        child = act.get(child_key)
                        if isinstance(child, list):
                            yield from walk_actions(child, f"{p}.{child_key}")

        for eid, ev in self.events_by_id.items():
            if not isinstance(ev, dict):
                continue

            trigger = ev.get("trigger")
            if isinstance(trigger, dict):
                ttype = str(trigger.get("type") or "").strip().lower()
                if kind == "quest" and ttype == "quest_stage_complete":
                    qid = str(trigger.get("quest_id") or trigger.get("questId") or "").strip()
                    if qid == ident:
                        add(f"event '{eid}': trigger quest_stage_complete quest_id '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                if kind == "npc" and ttype in ("talk_to", "npc_interaction", "dialogue_closed", "dialogue_dismissed"):
                    npc = str(trigger.get("npc") or "").strip()
                    if npc == ident:
                        add(f"event '{eid}': trigger {ttype} npc '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                if kind == "room" and ttype == "enter_room":
                    room = str(trigger.get("room_id") or trigger.get("room") or "").strip()
                    if room == ident:
                        add(f"event '{eid}': trigger enter_room room '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                if kind == "player_action" and ttype == "player_action":
                    action_id = str(trigger.get("action") or "").strip()
                    if action_id == ident:
                        add(f"event '{eid}': trigger player_action action '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                if kind == "item":
                    item_id = str(trigger.get("item_id") or trigger.get("item") or "").strip()
                    if item_id and item_id == ident:
                        add(f"event '{eid}': trigger {ttype} item '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})

            conditions = ev.get("conditions") or []
            if isinstance(conditions, list):
                for i, cond in enumerate(conditions):
                    if not isinstance(cond, dict):
                        continue
                    ctype = str(cond.get("type") or "").strip().lower()
                    if kind == "milestone" and ctype in ("milestone_set", "milestone_not_set") and str(cond.get("milestone") or "").strip() == ident:
                        add(f"event '{eid}': conditions[{i}] {ctype} milestone '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                    if kind == "event" and ctype in ("event_completed", "event_not_completed") and str(cond.get("event_id") or cond.get("eventId") or "").strip() == ident:
                        add(f"event '{eid}': conditions[{i}] {ctype} event_id '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                    if kind == "tutorial" and ctype in ("tutorial_completed", "tutorial_not_completed") and str(cond.get("tutorial_id") or cond.get("tutorialId") or "").strip() == ident:
                        add(f"event '{eid}': conditions[{i}] {ctype} tutorial_id '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                    if kind == "quest" and (
                        ctype.startswith("quest_") or ctype in ("quest_stage", "quest_stage_not", "quest_task_done", "quest_task_not_done")
                    ):
                        qid = str(cond.get("quest_id") or cond.get("questId") or "").strip()
                        if qid == ident:
                            add(f"event '{eid}': conditions[{i}] {ctype} quest_id '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                    if kind == "quest_stage" and ctype in ("quest_stage", "quest_stage_not"):
                        qid = str(cond.get("quest_id") or cond.get("questId") or "").strip()
                        sid = str(cond.get("stage_id") or cond.get("stageId") or "").strip()
                        if qid == qk_a and sid == qk_b:
                            add(f"event '{eid}': conditions[{i}] {ctype} {ident}", {"action": "open_ref", "kind": "event", "id": eid})
                    if kind == "quest_task" and ctype in ("quest_task_done", "quest_task_not_done"):
                        qid = str(cond.get("quest_id") or cond.get("questId") or "").strip()
                        tid = str(cond.get("task_id") or cond.get("taskId") or "").strip()
                        if qid == qk_a and tid == qk_b:
                            add(f"event '{eid}': conditions[{i}] {ctype} {ident}", {"action": "open_ref", "kind": "event", "id": eid})
                    if kind == "item" and ctype in ("item", "item_not"):
                        item_id = str(cond.get("item_id") or cond.get("item") or cond.get("itemId") or "").strip()
                        if item_id == ident:
                            add(f"event '{eid}': conditions[{i}] {ctype} item '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})

            actions = ev.get("actions") or []
            for act, path in walk_actions(actions):
                atype = str(act.get("type") or "").strip().lower()
                if kind == "cutscene" and atype in ("play_cinematic", "trigger_cutscene"):
                    scene = str(act.get("scene_id") or act.get("cutscene_id") or act.get("sceneId") or "").strip()
                    if scene == ident:
                        add(f"event '{eid}': {path} {atype} scene_id '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                if kind == "tutorial" and atype == "system_tutorial":
                    scene = str(act.get("scene_id") or act.get("tutorial_id") or act.get("sceneId") or act.get("tutorialId") or "").strip()
                    if scene == ident:
                        add(f"event '{eid}': {path} system_tutorial scene_id '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                if kind == "milestone":
                    mid = str(act.get("milestone") or "").strip()
                    if mid == ident:
                        add(f"event '{eid}': {path} {atype} milestone '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                    for k in ("milestones", "set_milestones", "clear_milestones", "setMilestones", "clearMilestones"):
                        v = act.get(k)
                        if isinstance(v, list) and any(str(x).strip() == ident for x in v):
                            add(f"event '{eid}': {path} {atype} {k} contains '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                if kind == "quest":
                    qid = str(act.get("quest_id") or act.get("questId") or act.get("start_quest") or act.get("startQuest") or act.get("complete_quest") or act.get("completeQuest") or "").strip()
                    if qid == ident:
                        add(f"event '{eid}': {path} {atype} quest_id '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                if kind == "quest_stage" and atype == "advance_quest_stage":
                    qid = str(act.get("quest_id") or act.get("questId") or "").strip()
                    sid = str(act.get("to_stage_id") or act.get("toStageId") or "").strip()
                    if qid == qk_a and sid == qk_b:
                        add(f"event '{eid}': {path} advance_quest_stage {ident}", {"action": "open_ref", "kind": "event", "id": eid})
                if kind == "quest_task" and atype == "set_quest_task_done":
                    qid = str(act.get("quest_id") or act.get("questId") or "").strip()
                    tid = str(act.get("task_id") or act.get("taskId") or "").strip()
                    if qid == qk_a and tid == qk_b:
                        add(f"event '{eid}': {path} set_quest_task_done {ident}", {"action": "open_ref", "kind": "event", "id": eid})
                if kind == "room":
                    room_id = str(act.get("room_id") or act.get("roomId") or "").strip()
                    if room_id == ident:
                        add(f"event '{eid}': {path} {atype} room_id '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                if kind == "item":
                    item_id = str(act.get("item_id") or act.get("item") or act.get("itemId") or "").strip()
                    if item_id == ident:
                        add(f"event '{eid}': {path} {atype} item '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                    items_list = act.get("items")
                    if isinstance(items_list, list):
                        for j, it in enumerate(items_list):
                            if not isinstance(it, dict):
                                continue
                            iid = str(it.get("item_id") or it.get("itemId") or "").strip()
                            if iid == ident:
                                add(f"event '{eid}': {path} {atype} items[{j}] item_id '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                    reward = act.get("reward")
                    if isinstance(reward, dict):
                        r_items = reward.get("items") or []
                        if isinstance(r_items, list):
                            for j, it in enumerate(r_items):
                                if not isinstance(it, dict):
                                    continue
                                iid = str(it.get("item_id") or it.get("itemId") or "").strip()
                                if iid == ident:
                                    add(f"event '{eid}': {path} reward.items[{j}] item_id '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})
                if kind == "player_action" and atype == "player_action":
                    action_id = str(act.get("action") or "").strip()
                    if action_id == ident:
                        add(f"event '{eid}': {path} player_action action '{ident}'", {"action": "open_ref", "kind": "event", "id": eid})

        # --- Quest references ---
        if kind == "quest":
            if ident in self.quests_by_id:
                add(f"quest '{ident}': definition", {"action": "focus_quest", "quest_id": ident})
        if kind == "quest_stage":
            if qk_a and qk_b and qk_a in self.quests_by_id:
                add(f"quest '{qk_a}' stage '{qk_b}': definition", {"action": "focus_stage", "quest_id": qk_a, "stage_id": qk_b})
        if kind == "quest_task":
            if qk_a and qk_b and qk_a in self.quests_by_id:
                quest = self.quests_by_id.get(qk_a) or {}
                stages = quest.get("stages") or []
                if isinstance(stages, list):
                    for sidx, st in enumerate(stages):
                        if not isinstance(st, dict):
                            continue
                        sid = str(st.get("id") or f"stage_{sidx}").strip()
                        tasks = st.get("tasks") or []
                        if not isinstance(tasks, list):
                            continue
                        for task in tasks:
                            if not isinstance(task, dict):
                                continue
                            tid = str(task.get("id") or "").strip()
                            if tid == qk_b:
                                add(
                                    f"quest '{qk_a}' stage '{sid}' task '{qk_b}': definition",
                                    {"action": "focus_task", "quest_id": qk_a, "stage_id": sid, "task_id": qk_b},
                                )

        if kind == "tutorial":
            for q in self.quests:
                if not isinstance(q, dict):
                    continue
                qid = str(q.get("id") or "").strip()
                stages = q.get("stages") or []
                if not isinstance(stages, list):
                    continue
                for sidx, st in enumerate(stages):
                    if not isinstance(st, dict):
                        continue
                    sid = str(st.get("id") or f"stage_{sidx}").strip()
                    tasks = st.get("tasks") or []
                    if not isinstance(tasks, list):
                        continue
                    for tidx, task in enumerate(tasks):
                        if not isinstance(task, dict):
                            continue
                        t_id = str(task.get("id") or f"task_{tidx}").strip()
                        if str(task.get("tutorial_id") or "").strip() == ident:
                            add(
                                f"quest '{qid}' stage '{sid}' task '{t_id}': tutorial_id '{ident}'",
                                {"action": "focus_task", "quest_id": qid, "stage_id": sid, "task_id": t_id},
                            )

        return hits

    def _safe_rename_asset(self, kind: str, old_id: str, new_id: str) -> bool:
        kind = (kind or "").strip().lower()
        old_id = (old_id or "").strip()
        new_id = (new_id or "").strip()
        if not kind or not old_id or not new_id or old_id == new_id:
            return False

        if kind == "dialogue":
            if old_id not in self.dialogues_by_id:
                QMessageBox.warning(self, "Safe Rename", f"Dialogue '{old_id}' not found.")
                return False
            if new_id in self.dialogues_by_id:
                QMessageBox.warning(self, "Safe Rename", f"Dialogue '{new_id}' already exists.")
                return False
            dlg = self.dialogues_by_id.pop(old_id)
            if isinstance(dlg, dict):
                dlg["id"] = new_id
            self.dialogues_by_id[new_id] = dlg
            self._retarget_beats("dialogue", old_id, new_id)
            self._retarget_dialogue_next_ids(old_id, new_id)
            self._mark_dirty("dialogue")
            return True

        if kind == "event":
            if old_id not in self.events_by_id:
                QMessageBox.warning(self, "Safe Rename", f"Event '{old_id}' not found.")
                return False
            if new_id in self.events_by_id:
                QMessageBox.warning(self, "Safe Rename", f"Event '{new_id}' already exists.")
                return False
            ev = self.events_by_id.pop(old_id)
            if isinstance(ev, dict):
                ev["id"] = new_id
            self.events_by_id[new_id] = ev
            self._retarget_beats("event", old_id, new_id)
            self._retarget_dialogue_condition_tokens(("event_completed", "event_not_completed"), old_id, new_id)
            self._retarget_event_fields(("event_id", "eventId"), old_id, new_id)
            self._mark_dirty("events")
            return True

        if kind == "cutscene":
            if old_id not in self.cinematics_by_id:
                QMessageBox.warning(self, "Safe Rename", f"Cutscene '{old_id}' not found.")
                return False
            if new_id in self.cinematics_by_id:
                QMessageBox.warning(self, "Safe Rename", f"Cutscene '{new_id}' already exists.")
                return False
            sc = self.cinematics_by_id.pop(old_id)
            if isinstance(sc, dict):
                sc["id"] = new_id
            self.cinematics_by_id[new_id] = sc
            self._retarget_beats("cutscene", old_id, new_id)
            self._retarget_dialogue_trigger_tokens(("play_cinematic",), old_id, new_id)
            self._retarget_event_action_field_for_types(
                ("play_cinematic", "trigger_cutscene"),
                ("scene_id", "cutscene_id", "sceneId", "cutsceneId"),
                old_id,
                new_id,
            )
            self._mark_dirty("cinematics")
            return True

        if kind == "tutorial":
            if old_id not in self.tutorial_scripts_by_id:
                QMessageBox.warning(self, "Safe Rename", f"Tutorial '{old_id}' not found.")
                return False
            if new_id in self.tutorial_scripts_by_id:
                QMessageBox.warning(self, "Safe Rename", f"Tutorial '{new_id}' already exists.")
                return False
            ts = self.tutorial_scripts_by_id.pop(old_id)
            if isinstance(ts, dict):
                ts["id"] = new_id
            self.tutorial_scripts_by_id[new_id] = ts
            self._retarget_beats("tutorial", old_id, new_id)
            self._retarget_task_tutorial_ids(old_id, new_id)
            self._retarget_event_tutorial_ids(old_id, new_id)
            self._retarget_event_action_field_for_types(
                ("system_tutorial",),
                ("scene_id", "tutorial_id", "sceneId", "tutorialId"),
                old_id,
                new_id,
            )
            self._retarget_dialogue_trigger_tokens(("system_tutorial",), old_id, new_id, mode="scene_context")
            self._retarget_dialogue_condition_tokens(("tutorial_completed", "tutorial_not_completed"), old_id, new_id)
            self._mark_dirty("tutorial_scripts")
            return True

        if kind == "milestone":
            if old_id not in self.milestones_by_id:
                QMessageBox.warning(self, "Safe Rename", f"Milestone '{old_id}' not found.")
                return False
            if new_id in self.milestones_by_id:
                QMessageBox.warning(self, "Safe Rename", f"Milestone '{new_id}' already exists.")
                return False
            ms = self.milestones_by_id.pop(old_id)
            if isinstance(ms, dict):
                ms["id"] = new_id
            self.milestones_by_id[new_id] = ms
            self._retarget_beats("milestone", old_id, new_id)
            self._retarget_dialogue_condition_tokens(("milestone", "milestone_not_set"), old_id, new_id)
            self._retarget_dialogue_trigger_tokens(("set_milestone", "clear_milestone"), old_id, new_id)
            self._retarget_event_fields(
                ("milestone", "milestones", "set_milestones", "clear_milestones", "setMilestones", "clearMilestones"),
                old_id,
                new_id,
            )
            self._mark_dirty("milestones")
            return True

        QMessageBox.warning(self, "Safe Rename", f"Unsupported type '{kind}'.")
        return False

    # -------------------- Navigator --------------------

    def _focus_quest_search(self):
        self.quest_search.setFocus(Qt.ShortcutFocusReason)

    def _refresh_quest_list(self):
        q = (self.quest_search.text() or "").strip().lower()
        selected = self.current_quest_id
        self.quest_list.blockSignals(True)
        try:
            self.quest_list.clear()
            for quest in sorted(self.quests, key=lambda row: str(row.get("title") or row.get("id") or "").lower()):
                qid = str(quest.get("id") or "").strip()
                if not qid:
                    continue
                title = str(quest.get("title") or "").strip()
                summary = str(quest.get("summary") or "").strip()
                hay = " ".join([qid, title, summary]).lower()
                if q and q not in hay:
                    continue
                label = f"{title}  ({qid})" if title else qid
                item = QListWidgetItem(label)
                item.setData(Qt.UserRole, qid)
                if summary:
                    item.setToolTip(summary)
                self.quest_list.addItem(item)

            # restore selection
            if selected:
                for i in range(self.quest_list.count()):
                    it = self.quest_list.item(i)
                    if it and it.data(Qt.UserRole) == selected:
                        self.quest_list.setCurrentRow(i)
                        break
            if self.quest_list.currentRow() < 0 and self.quest_list.count() > 0:
                self.quest_list.setCurrentRow(0)
        finally:
            self.quest_list.blockSignals(False)
        # Keep the rest of the UI in sync even though signals were blocked.
        new_item = self.quest_list.currentItem()
        new_id = str(new_item.data(Qt.UserRole)) if new_item else ""
        if new_id and new_id != (self.current_quest_id or ""):
            self._on_select_quest()
        if not new_id and self.current_quest_id:
            self._on_select_quest()

    def _select_quest_by_id(self, quest_id: str):
        for i in range(self.quest_list.count()):
            it = self.quest_list.item(i)
            if it and it.data(Qt.UserRole) == quest_id:
                self.quest_list.setCurrentRow(i)
                return

    def _select_stage_by_id(self, stage_id: str):
        for i in range(self.stage_list.count()):
            it = self.stage_list.item(i)
            if it and it.data(Qt.UserRole) == stage_id:
                self.stage_list.setCurrentRow(i)
                return

    def _on_select_quest(self):
        if self._updating_ui:
            return
        self._commit_beat_form_if_needed()

        item = self.quest_list.currentItem()
        qid = item.data(Qt.UserRole) if item else None
        if not qid:
            self.current_quest_id = None
            self.stage_list.clear()
            self._set_focus_label()
            return

        self.current_quest_id = str(qid)
        self._refresh_stage_list()
        self._set_focus_label()

    def _refresh_stage_list(self):
        self.stage_list.blockSignals(True)
        try:
            self.stage_list.clear()
            quest = self._current_quest()
            if not quest:
                self.current_stage_id = None
                return
            stages = quest.get("stages") or []
            if not isinstance(stages, list):
                stages = []
            for idx, stage in enumerate(stages):
                if not isinstance(stage, dict):
                    continue
                sid = str(stage.get("id") or f"stage_{idx}").strip()
                title = str(stage.get("title") or "").strip()
                desc = str(stage.get("description") or "").strip()
                tasks = stage.get("tasks") or []
                tcount = len(tasks) if isinstance(tasks, list) else 0
                label = f"{idx + 1}. {title}  ({sid})" if title else f"{idx + 1}. {sid}"
                item = QListWidgetItem(label)
                item.setData(Qt.UserRole, sid)
                tip = desc or ""
                if tip:
                    item.setToolTip(tip)
                if tcount:
                    item.setToolTip((tip + "\n\n" if tip else "") + f"Tasks: {tcount}")
                self.stage_list.addItem(item)

            # keep stage selection if possible
            if self.current_stage_id:
                for i in range(self.stage_list.count()):
                    it = self.stage_list.item(i)
                    if it and it.data(Qt.UserRole) == self.current_stage_id:
                        self.stage_list.setCurrentRow(i)
                        break
            if self.stage_list.currentRow() < 0 and self.stage_list.count() > 0:
                self.stage_list.setCurrentRow(0)
        finally:
            self.stage_list.blockSignals(False)
        new_item = self.stage_list.currentItem()
        new_id = str(new_item.data(Qt.UserRole)) if new_item else ""
        if new_id and new_id != (self.current_stage_id or ""):
            self._on_select_stage()
        if not new_id and self.current_stage_id:
            self._on_select_stage()

    def _on_select_stage(self):
        if self._updating_ui:
            return
        self._commit_beat_form_if_needed()

        item = self.stage_list.currentItem()
        sid = item.data(Qt.UserRole) if item else None
        self.current_stage_id = str(sid) if sid else None

        self._set_focus_label()
        self._refresh_flow_list()
        self._refresh_context()

    def _set_focus_label(self):
        quest = self._current_quest()
        stage = self._current_stage()
        if not quest or not stage:
            self.focus_label.setText("Focus: (select a quest stage)")
            return
        qtitle = quest.get("title") or quest.get("id") or ""
        stitle = stage.get("title") or stage.get("id") or ""
        self.focus_label.setText(f"Focus: {qtitle} → {stitle}")

    def _goto_current_quest(self):
        if self.current_quest_id:
            self._goto_or_open_editor("quest", self.current_quest_id)

    # -------------------- Flow helpers --------------------

    def _current_quest(self) -> Optional[dict]:
        if not self.current_quest_id:
            return None
        return self.quests_by_id.get(self.current_quest_id)

    def _current_stage(self) -> Optional[dict]:
        quest = self._current_quest()
        if not quest or not self.current_stage_id:
            return None
        stages = quest.get("stages") or []
        if not isinstance(stages, list):
            return None
        for st in stages:
            if isinstance(st, dict) and str(st.get("id") or "").strip() == self.current_stage_id:
                return st
        # fallback: allow stage_id as synthetic
        for idx, st in enumerate(stages):
            if isinstance(st, dict) and self.current_stage_id == f"stage_{idx}":
                return st
        return None

    def _stage_flow(self) -> List[dict]:
        if not self.current_quest_id or not self.current_stage_id:
            return []
        stages = self.flows.get(self.current_quest_id)
        if not isinstance(stages, dict):
            stages = {}
            self.flows[self.current_quest_id] = stages
        beats = stages.get(self.current_stage_id)
        if not isinstance(beats, list):
            beats = []
            stages[self.current_stage_id] = beats
        # Keep it clean: beats must be objects
        if any(not isinstance(b, dict) for b in beats):
            beats = [b for b in beats if isinstance(b, dict)]
            stages[self.current_stage_id] = beats
        return beats

    def _refresh_flow_list(self):
        selected = self._selected_beat_obj()
        self.flow_list.blockSignals(True)
        try:
            self.flow_list.clear()
            for beat in self._stage_flow():
                self.flow_list.addItem(self._render_flow_item(beat))
            if selected:
                self._select_beat_obj(selected)
            if self.flow_list.currentRow() < 0 and self.flow_list.count() > 0:
                self.flow_list.setCurrentRow(0)
        finally:
            self.flow_list.blockSignals(False)
        self._refresh_refs_list()
        self._load_beat_into_form(self._selected_beat_obj())

    def _render_flow_item(self, beat: dict) -> QListWidgetItem:
        t = str((beat or {}).get("type") or "note").strip() or "note"
        ref = str((beat or {}).get("id") or "").strip()
        label = str((beat or {}).get("label") or "").strip()
        note_text = str((beat or {}).get("text") or "").strip()

        missing = False
        detail = ""

        if t == "dialogue":
            dlg = self.dialogues_by_id.get(ref)
            if not dlg:
                missing = bool(ref)
            else:
                detail = _fmt_one_line(str(dlg.get("text") or ""))
                sp = str(dlg.get("speaker") or "").strip()
                if sp:
                    detail = f"{sp}: {detail}" if detail else sp
        elif t == "event":
            ev = self.events_by_id.get(ref)
            if not ev:
                missing = bool(ref)
            else:
                detail = _fmt_one_line(str(ev.get("description") or ""))
        elif t == "cutscene":
            sc = self.cinematics_by_id.get(ref)
            if not sc:
                missing = bool(ref)
            else:
                detail = _fmt_one_line(str(sc.get("title") or ""))
        elif t == "tutorial":
            ts = self.tutorial_scripts_by_id.get(ref)
            if not ts:
                missing = bool(ref)
            else:
                steps = ts.get("steps") or []
                if isinstance(steps, list) and steps and isinstance(steps[0], dict):
                    detail = _fmt_one_line(str(steps[0].get("message") or ""))
        elif t == "milestone":
            ms = self.milestones_by_id.get(ref)
            if not ms:
                missing = bool(ref)
            else:
                detail = _fmt_one_line(str(ms.get("name") or ms.get("description") or ""))
        elif t == "note":
            detail = _fmt_one_line(note_text)

        if not label:
            if t == "note":
                label = detail or "(note)"
            else:
                label = ref or "(unlinked)"

        text = f"[{t}] {label}"
        if detail and detail.lower() != label.lower():
            text += f" — {detail}"

        item = QListWidgetItem(text)
        item.setData(Qt.UserRole, beat)
        if missing:
            item.setForeground(Qt.red)
            item.setToolTip("Missing reference: create or fix the referenced ID.")
        if t == "note":
            f = item.font()
            f.setItalic(True)
            item.setFont(f)
        return item

    def _on_flow_reordered(self):
        # Rebuild underlying list to match UI order.
        if not self.current_quest_id or not self.current_stage_id:
            return
        new_order: List[dict] = []
        for i in range(self.flow_list.count()):
            it = self.flow_list.item(i)
            beat = it.data(Qt.UserRole) if it else None
            if isinstance(beat, dict):
                new_order.append(beat)
        self.flows.setdefault(self.current_quest_id, {})[self.current_stage_id] = new_order
        self._mark_dirty("flows")
        self._refresh_flow_list()

    def _add_beat(self, beat_type: str):
        if beat_type not in BEAT_TYPES:
            return
        if not self.current_quest_id or not self.current_stage_id:
            QMessageBox.information(self, "Select a Stage", "Select a quest stage first.")
            return
        beat: Dict[str, Any] = {"type": beat_type}
        if beat_type == "note":
            beat["text"] = ""
            beat["label"] = "Note"
        else:
            beat["id"] = ""
            beat["label"] = ""
        self._stage_flow().append(beat)
        self._mark_dirty("flows")
        self._refresh_flow_list()
        self._select_beat_obj(beat)

    def _duplicate_selected_beat(self):
        beat = self._selected_beat_obj()
        if not beat:
            return
        clone = copy.deepcopy(beat)
        self._stage_flow().insert(self._selected_beat_index() + 1, clone)
        self._mark_dirty("flows")
        self._refresh_flow_list()
        self._select_beat_obj(clone)

    def _delete_selected_beat(self):
        idx = self._selected_beat_index()
        if idx < 0:
            return
        flow = self._stage_flow()
        if not (0 <= idx < len(flow)):
            return
        beat = flow[idx]
        label = str(beat.get("label") or beat.get("id") or beat.get("type") or "beat")
        if QMessageBox.question(self, "Delete Beat", f"Delete beat '{label}'?") != QMessageBox.Yes:
            return
        flow.pop(idx)
        self._mark_dirty("flows")
        self._refresh_flow_list()
        self._clear_beat_form()

    def _move_selected_beat(self, delta: int):
        idx = self._selected_beat_index()
        flow = self._stage_flow()
        if idx < 0 or not (0 <= idx < len(flow)):
            return
        new_idx = idx + delta
        if not (0 <= new_idx < len(flow)):
            return
        flow[idx], flow[new_idx] = flow[new_idx], flow[idx]
        self._mark_dirty("flows")
        self._refresh_flow_list()
        self.flow_list.setCurrentRow(new_idx)

    def _selected_beat_index(self) -> int:
        return self.flow_list.currentRow() if self.flow_list else -1

    def _selected_beat_obj(self) -> Optional[dict]:
        it = self.flow_list.currentItem() if self.flow_list else None
        beat = it.data(Qt.UserRole) if it else None
        return beat if isinstance(beat, dict) else None

    def _select_beat_obj(self, beat: dict):
        for i in range(self.flow_list.count()):
            it = self.flow_list.item(i)
            if it and it.data(Qt.UserRole) is beat:
                self.flow_list.setCurrentRow(i)
                return

    # -------------------- Beat form --------------------

    def _set_beat_form_enabled(self, enabled: bool):
        for w in (self.beat_type_combo, self.ref_combo, self.beat_label_edit, self.note_edit, self.beat_apply_btn, self.beat_open_btn, self.beat_create_btn):
            if w is not None:
                w.setEnabled(enabled)

    def _clear_beat_form(self):
        self._updating_ui = True
        try:
            self._last_selected_beat = None
            self._beat_form_dirty = False
            self.beat_type_combo.setCurrentIndex(0)
            self.ref_combo.clear()
            self.beat_label_edit.setText("")
            self.note_edit.setPlainText("")
            self.beat_preview.setPlainText("")
            self._set_beat_form_enabled(False)
        finally:
            self._updating_ui = False

    def _on_select_beat(self):
        if self._updating_ui:
            return
        self._commit_beat_form_if_needed()

        beat = self._selected_beat_obj()
        self._load_beat_into_form(beat)

    def _load_beat_into_form(self, beat: Optional[dict]):
        self._updating_ui = True
        try:
            self._last_selected_beat = beat
            self._beat_form_dirty = False

            if not beat:
                self._clear_beat_form()
                return

            self._set_beat_form_enabled(True)
            t = str(beat.get("type") or "note").strip() or "note"
            if t not in BEAT_TYPES:
                t = "note"
            self.beat_type_combo.setCurrentText(t)
            self._refresh_ref_combo_for_type(t)

            if t == "note":
                self.ref_combo.setCurrentText("")
                self.ref_combo.setEnabled(False)
                self.note_edit.setPlainText(str(beat.get("text") or ""))
            else:
                self.ref_combo.setEnabled(True)
                self.ref_combo.setCurrentText(str(beat.get("id") or ""))
                self.note_edit.setPlainText(str(beat.get("text") or ""))
            self.beat_label_edit.setText(str(beat.get("label") or ""))
        finally:
            self._updating_ui = False
        self._update_beat_preview()
        self._update_beat_buttons()

    def _on_beat_type_changed(self, new_type: str):
        if self._updating_ui:
            return
        t = (new_type or "").strip() or "note"
        if t not in BEAT_TYPES:
            t = "note"
        self._refresh_ref_combo_for_type(t)
        if t == "note":
            self.ref_combo.setCurrentText("")
        self._beat_form_dirty = True
        self._update_beat_preview()
        self._update_beat_buttons()

    def _on_beat_form_changed(self, *_args):
        if self._updating_ui:
            return
        self._beat_form_dirty = True
        self._update_beat_preview()
        self._update_beat_buttons()

    def _commit_beat_form_if_needed(self):
        if not self._beat_form_dirty or not self._last_selected_beat:
            return
        self._apply_beat_form()

    def _apply_beat_form(self):
        beat = self._last_selected_beat
        if not beat:
            return
        t = self.beat_type_combo.currentText().strip() or "note"
        if t not in BEAT_TYPES:
            t = "note"

        beat["type"] = t
        beat["label"] = self.beat_label_edit.text().strip()

        if t == "note":
            beat.pop("id", None)
            beat["text"] = self.note_edit.toPlainText()
        else:
            beat["id"] = self.ref_combo.currentText().strip()
            if self.note_edit.toPlainText().strip():
                beat["text"] = self.note_edit.toPlainText().strip()
            else:
                beat.pop("text", None)

        self._beat_form_dirty = False
        self._mark_dirty("flows")
        self._refresh_flow_list()
        self._select_beat_obj(beat)
        self._refresh_context()

    def _refresh_ref_combo_for_type(self, beat_type: str):
        beat_type = beat_type.strip()
        values: List[str] = [""]
        if beat_type == "dialogue":
            values += _sorted_ids(self.dialogues_by_id)
        elif beat_type == "event":
            values += _sorted_ids(self.events_by_id)
        elif beat_type == "cutscene":
            values += _sorted_ids(self.cinematics_by_id)
        elif beat_type == "tutorial":
            values += _sorted_ids(self.tutorial_scripts_by_id)
        elif beat_type == "milestone":
            values += _sorted_ids(self.milestones_by_id)

        cur = self.ref_combo.currentText()
        self.ref_combo.blockSignals(True)
        try:
            self.ref_combo.clear()
            self.ref_combo.addItems(values)
            self.ref_combo.setCurrentText(cur)
            self.ref_combo.setCompleter(_make_completer(values))
        finally:
            self.ref_combo.blockSignals(False)

        # Type-driven enable/disable
        self.ref_combo.setEnabled(beat_type != "note")

    def _update_beat_buttons(self):
        beat = self._last_selected_beat
        if not beat:
            self.beat_open_btn.setEnabled(False)
            self.beat_create_btn.setEnabled(False)
            return
        t = self.beat_type_combo.currentText().strip() or "note"
        ref = self.ref_combo.currentText().strip()

        can_open = False
        can_create = t in ("dialogue", "event", "cutscene", "tutorial", "milestone")
        if t == "dialogue":
            can_open = bool(ref)
        elif t == "event":
            can_open = bool(ref)
        elif t == "cutscene":
            can_open = bool(ref)
        elif t == "tutorial":
            can_open = bool(ref)
        elif t == "milestone":
            can_open = bool(ref)
        self.beat_open_btn.setEnabled(can_open)
        self.beat_create_btn.setEnabled(can_create)

    def _open_selected_reference(self):
        beat = self._last_selected_beat
        if not beat:
            return
        t = self.beat_type_combo.currentText().strip() or "note"
        ref = self.ref_combo.currentText().strip()
        if not ref:
            return
        self._open_reference(t, ref)

    def _create_or_edit_reference(self):
        beat = self._last_selected_beat
        if not beat:
            return
        self._commit_beat_form_if_needed()
        t = self.beat_type_combo.currentText().strip() or "note"
        if t not in ("dialogue", "event", "cutscene", "tutorial", "milestone"):
            return

        # Ensure a ref id exists (create suggestion if empty)
        ref = self.ref_combo.currentText().strip()
        if not ref:
            ref = self._suggest_new_ref_id(t)
            self.ref_combo.setCurrentText(ref)
            self._beat_form_dirty = True
            self._apply_beat_form()

        if t == "dialogue":
            self._quick_edit_dialogue(ref)
        elif t == "milestone":
            self._quick_edit_milestone(ref)
        elif t == "cutscene":
            self._quick_edit_cinematic(ref)
        elif t == "tutorial":
            self._quick_edit_tutorial_script(ref)
        elif t == "event":
            self._quick_edit_event(ref)

        self._refresh_ref_combo_for_type(t)
        self._refresh_flow_list()
        self._refresh_context()
        self._update_beat_preview()

    def _has_studio_goto(self) -> bool:
        subs = getattr(EditorBus, "_subs", None)
        return bool(subs.get("goto")) if isinstance(subs, dict) else False

    def _goto_or_open_editor(self, target_type: str, ident: str):
        """
        If running inside Studio Pro, publish a goto. Otherwise, open the editor as a standalone window.
        """
        ident = (ident or "").strip()
        if not ident:
            return
        if self._has_studio_goto():
            studio_goto(target_type, ident)
            return

        spec = _STANDALONE_EDITOR_SPECS.get(target_type)
        if not spec:
            return
        mod_name, cls_name = spec

        # Reuse an existing window if possible.
        w = self._standalone_windows.get(target_type)
        if w is None:
            try:
                import importlib

                mod = importlib.import_module(mod_name)
                cls = getattr(mod, cls_name)

                # Most editors accept an assets dir / project root path (str or Path).
                ctor_args = [str(self.assets_root), self.assets_root, str(self.project_root), self.project_root]
                w = None
                for arg in ctor_args:
                    try:
                        w = cls(arg)
                        break
                    except TypeError:
                        continue
                if w is None:
                    w = cls()
                self._standalone_windows[target_type] = w
            except Exception as exc:
                QMessageBox.critical(self, "Open Failed", f"Could not open {target_type} editor:\n{exc}")
                return

        try:
            w.show()
            w.raise_()
            w.activateWindow()
        except Exception:
            pass

        # Best-effort selection/focus.
        for meth in ("select_id", "select_name", "focus_id", "focus_name", "select"):
            if hasattr(w, meth):
                try:
                    getattr(w, meth)(ident)
                    return
                except Exception:
                    pass
        for field in ("search_box", "search", "filter", "filter_box", "scene_filter"):
            if hasattr(w, field):
                try:
                    getattr(w, field).setText(ident)
                    return
                except Exception:
                    pass

    def _open_reference(self, kind: str, ident: str):
        """
        Open an asset reference from the current UI.
        For missing refs, prefers creating via the quick editors.
        """
        kind = (kind or "").strip()
        ident = (ident or "").strip()
        if not kind or not ident:
            return

        # Cutscenes/cinematics: always use the built-in quick editor (list-based schema).
        if kind == "cutscene":
            self._commit_beat_form_if_needed()
            self._quick_edit_cinematic(ident)
            self._refresh_ref_combo_for_type("cutscene")
            self._refresh_flow_list()
            self._refresh_context()
            self._update_beat_preview()
            return

        if kind == "tutorial":
            self._commit_beat_form_if_needed()
            self._quick_edit_tutorial_script(ident)
            self._refresh_ref_combo_for_type("tutorial")
            self._refresh_flow_list()
            self._refresh_context()
            self._update_beat_preview()
            return

        if kind == "dialogue" and ident not in self.dialogues_by_id:
            self._commit_beat_form_if_needed()
            self._quick_edit_dialogue(ident)
            self._refresh_ref_combo_for_type("dialogue")
            self._refresh_flow_list()
            self._refresh_context()
            self._update_beat_preview()
            return

        if kind == "milestone" and ident not in self.milestones_by_id:
            self._commit_beat_form_if_needed()
            self._quick_edit_milestone(ident)
            self._refresh_ref_combo_for_type("milestone")
            self._refresh_flow_list()
            self._refresh_context()
            self._update_beat_preview()
            return

        if kind == "event" and ident not in self.events_by_id:
            self._commit_beat_form_if_needed()
            self._quick_edit_event(ident)
            self._refresh_ref_combo_for_type("event")
            self._refresh_flow_list()
            self._refresh_context()
            self._update_beat_preview()
            return

        if kind in ("dialogue", "event", "milestone", "quest"):
            self._goto_or_open_editor(kind, ident)
            return

    def _suggest_new_ref_id(self, beat_type: str) -> str:
        qid = self.current_quest_id or "quest"
        sid = self.current_stage_id or "stage"
        base_label = self.beat_label_edit.text().strip() or beat_type
        slug = _slugify(base_label)
        if beat_type == "dialogue":
            base = f"dlg_{qid}_{sid}_{slug}"
            return unique_id(base, _sorted_ids(self.dialogues_by_id))
        if beat_type == "event":
            base = f"evt_{qid}_{sid}_{slug}"
            return unique_id(base, _sorted_ids(self.events_by_id))
        if beat_type == "cutscene":
            base = f"scene_{qid}_{sid}_{slug}"
            return unique_id(base, _sorted_ids(self.cinematics_by_id))
        if beat_type == "tutorial":
            base = f"scene_{qid}_{sid}_{slug}"
            return unique_id(base, _sorted_ids(self.tutorial_scripts_by_id))
        if beat_type == "milestone":
            base = f"ms_{qid}_{sid}_{slug}"
            return unique_id(base, _sorted_ids(self.milestones_by_id))
        return unique_id(f"{beat_type}_{qid}_{sid}_{slug}", [])

    # -------------------- Quick edit / create --------------------

    def _quick_edit_dialogue(self, did: str):
        existing = set(self.dialogues_by_id.keys())
        dlg = self.dialogues_by_id.get(did)
        if not dlg:
            dlg = {"id": did, "speaker": "", "text": "TODO: write dialogue."}

        quest_ids = _sorted_ids(self.quests_by_id)
        milestone_ids = _sorted_ids(self.milestones_by_id)
        item_ids = _sorted_ids(self.items_by_id)
        event_ids = _sorted_ids(self.events_by_id)
        tutorial_ids = _sorted_ids(self.tutorial_scripts_by_id)
        cinematic_ids = _sorted_ids(self.cinematics_by_id)

        quest_stage_keys: set[str] = set()
        quest_task_keys: set[str] = set()
        for qid, q in self.quests_by_id.items():
            stages = (q or {}).get("stages") or []
            if not isinstance(stages, list):
                continue
            for idx, st in enumerate(stages):
                if not isinstance(st, dict):
                    continue
                sid = str(st.get("id") or f"stage_{idx}").strip()
                if sid:
                    quest_stage_keys.add(f"{qid}:{sid}")
                tasks = (st or {}).get("tasks") or []
                if not isinstance(tasks, list):
                    continue
                for t in tasks:
                    if not isinstance(t, dict):
                        continue
                    tid = str(t.get("id") or "").strip()
                    if tid:
                        quest_task_keys.add(f"{qid}:{tid}")

        dialog = DialogueQuickEdit(
            self,
            existing_ids=existing,
            data=dlg,
            npc_names=self.npc_names,
            quest_ids=quest_ids,
            milestone_ids=milestone_ids,
            item_ids=item_ids,
            event_ids=event_ids,
            tutorial_ids=tutorial_ids,
            cinematic_ids=cinematic_ids,
            quest_stage_keys=sorted(quest_stage_keys, key=str.lower),
            quest_task_keys=sorted(quest_task_keys, key=str.lower),
            player_action_ids=self.player_action_ids,
        )
        if dialog.exec_() != QDialog.Accepted:
            return
        new_data = dialog.value()
        old_id = did
        new_id = new_data.get("id")
        if new_id != old_id:
            self.dialogues_by_id.pop(old_id, None)
            self._retarget_beats("dialogue", old_id, new_id)
            self._retarget_dialogue_next_ids(old_id, new_id)
        self.dialogues_by_id[new_id] = new_data
        self._mark_dirty("dialogue")

    def _quick_edit_milestone(self, mid: str):
        existing = set(self.milestones_by_id.keys())
        ms = self.milestones_by_id.get(mid)
        if not ms:
            ms = {"id": mid, "name": "TODO", "description": "", "toast": ""}
        dialog = MilestoneQuickEdit(self, existing_ids=existing, data=ms)
        if dialog.exec_() != QDialog.Accepted:
            return
        new_data = dialog.value()
        old_id = mid
        new_id = new_data.get("id")
        if new_id != old_id:
            self.milestones_by_id.pop(old_id, None)
            self._retarget_beats("milestone", old_id, new_id)
            self._retarget_dialogue_condition_tokens(("milestone", "milestone_not_set"), old_id, new_id)
            self._retarget_dialogue_trigger_tokens(("set_milestone", "clear_milestone"), old_id, new_id)
            self._retarget_event_fields(
                ("milestone", "milestones", "set_milestones", "clear_milestones", "setMilestones", "clearMilestones"),
                old_id,
                new_id,
            )
        self.milestones_by_id[new_id] = new_data
        self._mark_dirty("milestones")

    def _quick_edit_cinematic(self, sid: str):
        existing = set(self.cinematics_by_id.keys())
        sc = self.cinematics_by_id.get(sid)
        if not sc:
            sc = {"id": sid, "title": "TODO", "steps": [{"type": "narration", "text": "TODO: write the moment."}]}
        dialog = CinematicQuickEdit(self, existing_ids=existing, data=sc)
        if dialog.exec_() != QDialog.Accepted:
            return
        new_data = dialog.value()
        old_id = sid
        new_id = new_data.get("id")
        if new_id != old_id:
            self.cinematics_by_id.pop(old_id, None)
            self._retarget_beats("cutscene", old_id, new_id)
            self._retarget_dialogue_trigger_tokens(("play_cinematic",), old_id, new_id)
            self._retarget_event_action_field_for_types(
                ("play_cinematic", "trigger_cutscene"),
                ("scene_id", "cutscene_id", "sceneId", "cutsceneId"),
                old_id,
                new_id,
            )
        self.cinematics_by_id[new_id] = new_data
        self._mark_dirty("cinematics")

    def _quick_edit_tutorial_script(self, tid: str):
        existing = set(self.tutorial_scripts_by_id.keys())
        ts = self.tutorial_scripts_by_id.get(tid)
        if not ts:
            ts = {"id": tid, "steps": [{"message": "TODO: write tutorial step.", "context": ""}]}
        dialog = TutorialScriptQuickEdit(self, existing_ids=existing, data=ts)
        if dialog.exec_() != QDialog.Accepted:
            return
        new_data = dialog.value()
        old_id = tid
        new_id = new_data.get("id")
        if new_id != old_id:
            self.tutorial_scripts_by_id.pop(old_id, None)
            self._retarget_beats("tutorial", old_id, new_id)
            self._retarget_task_tutorial_ids(old_id, new_id)
            self._retarget_event_tutorial_ids(old_id, new_id)
            self._retarget_event_action_field_for_types(
                ("system_tutorial",),
                ("scene_id", "tutorial_id", "sceneId", "tutorialId"),
                old_id,
                new_id,
            )
            self._retarget_dialogue_trigger_tokens(("system_tutorial",), old_id, new_id, mode="scene_context")
            self._retarget_dialogue_condition_tokens(("tutorial_completed", "tutorial_not_completed"), old_id, new_id)
        self.tutorial_scripts_by_id[new_id] = new_data
        self._mark_dirty("tutorial_scripts")

    def _quick_edit_event(self, eid: str):
        existing = set(self.events_by_id.keys())
        ev = self.events_by_id.get(eid)
        if not ev:
            ev = {
                "id": eid,
                "description": "TODO: describe what this event does.",
                "trigger": {"type": "player_action", "action": "TODO"},
                "repeatable": False,
                "actions": [],
            }

        quest_ids = _sorted_ids(self.quests_by_id)
        milestone_ids = _sorted_ids(self.milestones_by_id)
        item_ids = _sorted_ids(self.items_by_id)
        room_ids = _sorted_ids(self.rooms_by_id)
        cinematic_ids = _sorted_ids(self.cinematics_by_id)
        tutorial_ids = _sorted_ids(self.tutorial_scripts_by_id)

        quest_stage_keys: set[str] = set()
        quest_task_keys: set[str] = set()
        for qid, q in self.quests_by_id.items():
            stages = (q or {}).get("stages") or []
            if not isinstance(stages, list):
                continue
            for idx, st in enumerate(stages):
                if not isinstance(st, dict):
                    continue
                sid = str(st.get("id") or f"stage_{idx}").strip()
                if sid:
                    quest_stage_keys.add(f"{qid}:{sid}")
                tasks = (st or {}).get("tasks") or []
                if not isinstance(tasks, list):
                    continue
                for t in tasks:
                    if not isinstance(t, dict):
                        continue
                    tid = str(t.get("id") or "").strip()
                    if tid:
                        quest_task_keys.add(f"{qid}:{tid}")

        dialog = EventQuickEdit(
            self,
            existing_ids=existing,
            data=ev,
            quest_ids=quest_ids,
            milestone_ids=milestone_ids,
            item_ids=item_ids,
            npc_ids=self.npc_ids,
            room_ids=room_ids,
            cinematic_ids=cinematic_ids,
            tutorial_ids=tutorial_ids,
            quest_stage_keys=sorted(quest_stage_keys, key=str.lower),
            quest_task_keys=sorted(quest_task_keys, key=str.lower),
            player_action_ids=self.player_action_ids,
        )
        if dialog.exec_() != QDialog.Accepted:
            return
        new_data = dialog.value()
        old_id = eid
        new_id = new_data.get("id")
        if new_id != old_id:
            self.events_by_id.pop(old_id, None)
            self._retarget_beats("event", old_id, new_id)
            self._retarget_dialogue_condition_tokens(("event_completed", "event_not_completed"), old_id, new_id)
            self._retarget_event_fields(("event_id", "eventId"), old_id, new_id)
        self.events_by_id[new_id] = new_data
        self._mark_dirty("events")

    def _retarget_beats(self, beat_type: str, old_id: str, new_id: str):
        """
        Retarget beats across all flows when an asset ID changes.
        """
        if not old_id or not new_id or old_id == new_id:
            return
        for qid, stages in (self.flows or {}).items():
            if not isinstance(stages, dict):
                continue
            for sid, beats in stages.items():
                if not isinstance(beats, list):
                    continue
                for beat in beats:
                    if not isinstance(beat, dict):
                        continue
                    if str(beat.get("type") or "").strip() != beat_type:
                        continue
                    if str(beat.get("id") or "").strip() == old_id:
                        beat["id"] = new_id
                        self._dirty["flows"] = True
                        self._dirty_any = True

    def _retarget_dialogue_next_ids(self, old_id: str, new_id: str):
        if not old_id or not new_id or old_id == new_id:
            return
        changed = False
        for dlg in self.dialogues_by_id.values():
            if not isinstance(dlg, dict):
                continue
            if str(dlg.get("next") or "").strip() == old_id:
                dlg["next"] = new_id
                changed = True
        if changed:
            self._mark_dirty("dialogue")

    def _retarget_dialogue_condition_tokens(self, types: Iterable[str], old_id: str, new_id: str):
        if not old_id or not new_id or old_id == new_id:
            return
        type_set = {str(t).strip().lower() for t in types if str(t).strip()}
        if not type_set:
            return
        changed = False
        for dlg in self.dialogues_by_id.values():
            if not isinstance(dlg, dict):
                continue
            cond = str(dlg.get("condition") or "").strip()
            if not cond:
                continue
            tokens = [t.strip() for t in cond.split(",") if t.strip()]
            out: List[str] = []
            touched = False
            for tok in tokens:
                parts = tok.split(":", 1)
                t_raw = parts[0].strip()
                t_low = t_raw.lower()
                v = parts[1].strip() if len(parts) > 1 else ""
                if t_low in type_set and v == old_id:
                    v = new_id
                    touched = True
                out.append(f"{t_raw}:{v}" if v else t_raw)
            new_cond = ", ".join(out).strip()
            if touched and new_cond != cond:
                if new_cond:
                    dlg["condition"] = new_cond
                else:
                    dlg.pop("condition", None)
                changed = True
        if changed:
            self._mark_dirty("dialogue")

    def _retarget_dialogue_trigger_tokens(self, types: Iterable[str], old_id: str, new_id: str, *, mode: str = "plain"):
        if not old_id or not new_id or old_id == new_id:
            return
        type_set = {str(t).strip().lower() for t in types if str(t).strip()}
        if not type_set:
            return

        def _retarget_value(value: str) -> str:
            value = value or ""
            if mode == "plain":
                return new_id if value == old_id else value
            if mode == "scene_context":
                if not value:
                    return value
                parts = value.split("|", 1)
                scene = parts[0].strip()
                if scene != old_id:
                    return value
                if len(parts) == 1:
                    return new_id
                return f"{new_id}|{parts[1]}"
            if mode == "id_quantity":
                if not value:
                    return value
                for delim in ("*", "x", "|"):
                    idx = value.find(delim)
                    if idx > 0:
                        lead = value[:idx].strip()
                        if lead == old_id:
                            return f"{new_id}{value[idx:]}"
                        return value
                return new_id if value.strip() == old_id else value
            return value

        changed = False
        for dlg in self.dialogues_by_id.values():
            if not isinstance(dlg, dict):
                continue
            trig = str(dlg.get("trigger") or "").strip()
            if not trig:
                continue
            tokens = [t.strip() for t in trig.split(",") if t.strip()]
            out: List[str] = []
            touched = False
            for tok in tokens:
                parts = tok.split(":", 1)
                t_raw = parts[0].strip()
                t_low = t_raw.lower()
                v = parts[1].strip() if len(parts) > 1 else ""
                if t_low in type_set:
                    new_v = _retarget_value(v)
                    if new_v != v:
                        v = new_v
                        touched = True
                out.append(f"{t_raw}:{v}" if v else t_raw)
            new_trig = ", ".join(out).strip()
            if touched and new_trig != trig:
                if new_trig:
                    dlg["trigger"] = new_trig
                else:
                    dlg.pop("trigger", None)
                changed = True
        if changed:
            self._mark_dirty("dialogue")

    def _deep_replace_in_place(self, obj: Any, key_names: Iterable[str], old_id: str, new_id: str) -> bool:
        """
        Deeply replace string values for specific keys inside nested dict/list structures.

        Intended for events.json-like payloads where references are stored under stable key names.
        """
        if not old_id or not new_id or old_id == new_id:
            return False
        keys = set(key_names)
        if not keys:
            return False
        changed = False
        if isinstance(obj, dict):
            for k, v in list(obj.items()):
                if k in keys:
                    if isinstance(v, str) and v.strip() == old_id:
                        obj[k] = new_id
                        changed = True
                    elif isinstance(v, list):
                        for i, item in enumerate(v):
                            if isinstance(item, str) and item.strip() == old_id:
                                v[i] = new_id
                                changed = True
                if isinstance(v, (dict, list)):
                    if self._deep_replace_in_place(v, keys, old_id, new_id):
                        changed = True
        elif isinstance(obj, list):
            for item in obj:
                if isinstance(item, (dict, list)):
                    if self._deep_replace_in_place(item, keys, old_id, new_id):
                        changed = True
        return changed

    def _retarget_event_fields(self, key_names: Iterable[str], old_id: str, new_id: str):
        if not old_id or not new_id or old_id == new_id:
            return
        changed = False
        for ev in self.events_by_id.values():
            if not isinstance(ev, dict):
                continue
            if self._deep_replace_in_place(ev, key_names, old_id, new_id):
                changed = True
        if changed:
            self._mark_dirty("events")

    def _retarget_event_action_field_for_types(
        self,
        action_types: Iterable[str],
        field_keys: Iterable[str],
        old_id: str,
        new_id: str,
    ):
        if not old_id or not new_id or old_id == new_id:
            return
        type_set = {str(t).strip().lower() for t in action_types if str(t).strip()}
        key_set = {str(k).strip() for k in field_keys if str(k).strip()}
        if not type_set or not key_set:
            return
        changed = False

        def walk(obj: Any):
            nonlocal changed
            if isinstance(obj, list):
                for it in obj:
                    walk(it)
                return
            if not isinstance(obj, dict):
                return
            t = str(obj.get("type") or "").strip().lower()
            if t in type_set:
                for k in key_set:
                    v = obj.get(k)
                    if isinstance(v, str) and v.strip() == old_id:
                        obj[k] = new_id
                        changed = True
            for v in obj.values():
                if isinstance(v, (dict, list)):
                    walk(v)

        for ev in self.events_by_id.values():
            if isinstance(ev, dict):
                walk(ev)
        if changed:
            self._mark_dirty("events")

    def _retarget_event_tutorial_ids(self, old_id: str, new_id: str):
        self._retarget_event_fields({"tutorial_id"}, old_id, new_id)

    def _retarget_task_tutorial_ids(self, old_id: str, new_id: str):
        if not old_id or not new_id or old_id == new_id:
            return
        for q in self.quests:
            stages = (q or {}).get("stages") or []
            if not isinstance(stages, list):
                continue
            for st in stages:
                tasks = (st or {}).get("tasks") or []
                if not isinstance(tasks, list):
                    continue
                for t in tasks:
                    if isinstance(t, dict) and str(t.get("tutorial_id") or "").strip() == old_id:
                        t["tutorial_id"] = new_id
                        self._mark_dirty("quests")

    # -------------------- Stage / task editing --------------------

    def _edit_current_stage(self):
        stage = self._current_stage()
        if not stage:
            return
        dialog = StageQuickEdit(self, data=stage)
        if dialog.exec_() != QDialog.Accepted:
            return
        new_data = dialog.value()
        stage["title"] = str(new_data.get("title") or "").strip()
        stage["description"] = str(new_data.get("description") or "").strip()
        self._mark_dirty("quests")
        self._refresh_stage_list()
        self._refresh_context()
        self._set_focus_label()

    def _current_stage_tasks(self) -> Optional[list]:
        stage = self._current_stage()
        if not stage:
            return None
        tasks = stage.get("tasks")
        if tasks is None:
            tasks = []
            stage["tasks"] = tasks
        if not isinstance(tasks, list):
            tasks = []
            stage["tasks"] = tasks
        return tasks

    def _quest_task_ids(self, quest_id: str) -> set[str]:
        quest = self.quests_by_id.get(quest_id)
        if not isinstance(quest, dict):
            return set()
        out: set[str] = set()
        stages = quest.get("stages") or []
        if not isinstance(stages, list):
            return out
        for st in stages:
            if not isinstance(st, dict):
                continue
            tasks = st.get("tasks") or []
            if not isinstance(tasks, list):
                continue
            for t in tasks:
                if not isinstance(t, dict):
                    continue
                tid = str(t.get("id") or "").strip()
                if tid:
                    out.add(tid)
        return out

    def _task_index_for_row(self, row: int) -> Optional[int]:
        it = self.task_table.item(row, 0) if self.task_table else None
        if not it:
            return None
        idx = it.data(Qt.UserRole)
        return int(idx) if isinstance(idx, int) else None

    def _selected_task_index(self) -> Optional[int]:
        row = self.task_table.currentRow() if self.task_table else -1
        if row < 0:
            return None
        return self._task_index_for_row(row)

    def _update_task_buttons(self):
        stage = self._current_stage()
        has_stage = bool(stage)
        if self.stage_edit_btn:
            self.stage_edit_btn.setEnabled(has_stage)
        if self.task_add_btn:
            self.task_add_btn.setEnabled(has_stage)

        idx = self._selected_task_index() if has_stage else None
        has_sel = idx is not None
        for b in (self.task_edit_btn, self.task_del_btn, self.task_up_btn, self.task_dn_btn, self.task_tut_btn):
            if b:
                b.setEnabled(bool(has_sel))

        if self.task_tut_btn and has_sel:
            tut_item = self.task_table.item(self.task_table.currentRow(), 2)
            tut = tut_item.text().strip() if tut_item else ""
            self.task_tut_btn.setEnabled(bool(tut))

        if not has_sel:
            return

        tasks = self._current_stage_tasks() or []
        can_up = bool(has_sel and idx is not None and idx > 0)
        can_dn = bool(has_sel and idx is not None and idx < len(tasks) - 1)
        if self.task_up_btn:
            self.task_up_btn.setEnabled(can_up)
        if self.task_dn_btn:
            self.task_dn_btn.setEnabled(can_dn)

    def _suggest_new_task_id(self) -> str:
        qid = self.current_quest_id or "quest"
        stage = self._current_stage() or {}
        sid = str(stage.get("id") or "stage").strip() or "stage"
        base = f"{sid}_task"
        return unique_id(_slugify(base), sorted(self._quest_task_ids(qid), key=str.lower))

    def _add_task(self):
        if not self.current_quest_id:
            return
        tasks = self._current_stage_tasks()
        if tasks is None:
            return
        qid = self.current_quest_id
        existing = self._quest_task_ids(qid)
        new_task = {"id": self._suggest_new_task_id(), "text": "TODO", "done": False}
        dialog = TaskQuickEdit(self, existing_ids=existing, data=new_task, tutorial_ids=_sorted_ids(self.tutorial_scripts_by_id))
        if dialog.exec_() != QDialog.Accepted:
            return
        new_data = dialog.value()
        tasks.append(new_data)
        self._mark_dirty("quests")
        self._refresh_context()
        self._refresh_stage_list()
        self._select_task_by_id(str(new_data.get("id") or "").strip())

    def _select_task_by_id(self, task_id: str):
        if not self.task_table or not task_id:
            return
        for r in range(self.task_table.rowCount()):
            it = self.task_table.item(r, 0)
            if it and it.text().strip() == task_id:
                self.task_table.setCurrentCell(r, 0)
                return

    def _edit_selected_task(self):
        if not self.current_quest_id:
            return
        idx = self._selected_task_index()
        if idx is None:
            return
        tasks = self._current_stage_tasks()
        if tasks is None or not (0 <= idx < len(tasks)):
            return
        task = tasks[idx]
        if not isinstance(task, dict):
            return
        qid = self.current_quest_id
        existing = self._quest_task_ids(qid)
        dialog = TaskQuickEdit(self, existing_ids=existing, data=task, tutorial_ids=_sorted_ids(self.tutorial_scripts_by_id))
        if dialog.exec_() != QDialog.Accepted:
            return
        new_data = dialog.value()
        old_id = str(task.get("id") or "").strip()
        new_id = str(new_data.get("id") or "").strip()
        task.update(new_data)
        if old_id and new_id and new_id != old_id:
            self._retarget_quest_task_refs(qid, old_id, new_id)
        self._mark_dirty("quests")
        self._refresh_context()
        self._refresh_stage_list()
        self._select_task_by_id(new_id or old_id)

    def _delete_selected_task(self):
        idx = self._selected_task_index()
        if idx is None:
            return
        tasks = self._current_stage_tasks()
        if tasks is None or not (0 <= idx < len(tasks)):
            return
        task = tasks[idx] if isinstance(tasks[idx], dict) else {}
        label = str((task or {}).get("id") or "").strip() or f"task[{idx}]"
        if QMessageBox.question(self, "Delete Task", f"Delete task '{label}'?") != QMessageBox.Yes:
            return
        tasks.pop(idx)
        self._mark_dirty("quests")
        self._refresh_context()
        self._refresh_stage_list()

    def _move_selected_task(self, delta: int):
        idx = self._selected_task_index()
        if idx is None:
            return
        tasks = self._current_stage_tasks()
        if tasks is None:
            return
        new_idx = idx + delta
        if not (0 <= idx < len(tasks) and 0 <= new_idx < len(tasks)):
            return
        tasks[idx], tasks[new_idx] = tasks[new_idx], tasks[idx]
        moved = tasks[new_idx] if isinstance(tasks[new_idx], dict) else {}
        moved_id = str((moved or {}).get("id") or "").strip()
        self._mark_dirty("quests")
        self._refresh_context()
        self._refresh_stage_list()
        if moved_id:
            self._select_task_by_id(moved_id)

    def _open_selected_task_tutorial(self):
        row = self.task_table.currentRow() if self.task_table else -1
        if row < 0:
            return
        tut_item = self.task_table.item(row, 2)
        tutorial_id = tut_item.text().strip() if tut_item else ""
        if not tutorial_id:
            return
        self._quick_edit_tutorial_script(tutorial_id)
        self._refresh_context()

    def _retarget_quest_task_refs(self, quest_id: str, old_task_id: str, new_task_id: str):
        self._retarget_dialogue_trigger_task_ids(quest_id, old_task_id, new_task_id)
        self._retarget_event_task_ids(quest_id, old_task_id, new_task_id)

    def _retarget_dialogue_trigger_task_ids(self, quest_id: str, old_task_id: str, new_task_id: str):
        if not quest_id or not old_task_id or not new_task_id or old_task_id == new_task_id:
            return
        changed = False
        for dlg in self.dialogues_by_id.values():
            if not isinstance(dlg, dict):
                continue
            trig = str(dlg.get("trigger") or "").strip()
            if not trig:
                continue
            tokens = [t.strip() for t in trig.split(",") if t.strip()]
            out: List[str] = []
            touched = False
            for tok in tokens:
                parts = tok.split(":", 1)
                t_raw = parts[0].strip()
                t_low = t_raw.lower()
                v = parts[1].strip() if len(parts) > 1 else ""
                if t_low == "set_quest_task_done":
                    qp = v.split(":", 1)
                    q = qp[0].strip() if qp else ""
                    tid = qp[1].strip() if len(qp) > 1 else ""
                    if q == quest_id and tid == old_task_id:
                        v = f"{quest_id}:{new_task_id}"
                        touched = True
                out.append(f"{t_raw}:{v}" if v else t_raw)
            new_trig = ", ".join(out).strip()
            if touched and new_trig != trig:
                dlg["trigger"] = new_trig
                changed = True
        if changed:
            self._mark_dirty("dialogue")

    def _retarget_event_task_ids(self, quest_id: str, old_task_id: str, new_task_id: str):
        if not quest_id or not old_task_id or not new_task_id or old_task_id == new_task_id:
            return
        changed = False

        def walk(obj: Any):
            nonlocal changed
            if isinstance(obj, list):
                for it in obj:
                    walk(it)
                return
            if not isinstance(obj, dict):
                return

            q = str(obj.get("quest_id") or "").strip()
            tid = str(obj.get("task_id") or "").strip()
            if q == quest_id and tid == old_task_id:
                obj["task_id"] = new_task_id
                changed = True

            for v in obj.values():
                if isinstance(v, (dict, list)):
                    walk(v)

        for ev in self.events_by_id.values():
            if isinstance(ev, dict):
                walk(ev)
        if changed:
            self._mark_dirty("events")

    # -------------------- Context / refs --------------------

    def _refresh_context(self):
        self._refresh_stage_snapshot()
        self._refresh_refs_list()

    def _refresh_stage_snapshot(self):
        stage = self._current_stage()
        if not stage:
            self.stage_desc.setPlainText("")
            self.task_table.setRowCount(0)
            self._update_task_buttons()
            return

        desc = str(stage.get("description") or "").strip()
        self.stage_desc.setPlainText(desc)

        tasks = stage.get("tasks") or []
        if not isinstance(tasks, list):
            tasks = []
        self.task_table.setRowCount(0)
        for task_idx, task in enumerate(tasks):
            if not isinstance(task, dict):
                continue
            r = self.task_table.rowCount()
            self.task_table.insertRow(r)
            tid = str(task.get("id") or "").strip()
            txt = str(task.get("text") or "").strip()
            tut = str(task.get("tutorial_id") or "").strip()
            id_item = QTableWidgetItem(tid)
            id_item.setData(Qt.UserRole, task_idx)
            self.task_table.setItem(r, 0, id_item)
            self.task_table.setItem(r, 1, QTableWidgetItem(txt))
            self.task_table.setItem(r, 2, QTableWidgetItem(tut))
            if tut and tut not in self.tutorial_scripts_by_id:
                self.task_table.item(r, 2).setForeground(Qt.red)  # type: ignore[union-attr]

        self.task_table.resizeColumnsToContents()
        self._update_task_buttons()

    def _refresh_refs_list(self):
        self.refs_list.clear()

        refs: List[Tuple[str, str, bool]] = []
        # From beats
        for beat in self._stage_flow():
            if not isinstance(beat, dict):
                continue
            t = str(beat.get("type") or "").strip()
            rid = str(beat.get("id") or "").strip()
            if not rid:
                continue
            if t == "dialogue":
                refs.append((t, rid, rid not in self.dialogues_by_id))
            elif t == "event":
                refs.append((t, rid, rid not in self.events_by_id))
            elif t == "cutscene":
                refs.append((t, rid, rid not in self.cinematics_by_id))
            elif t == "tutorial":
                refs.append((t, rid, rid not in self.tutorial_scripts_by_id))
            elif t == "milestone":
                refs.append((t, rid, rid not in self.milestones_by_id))

        # From tasks (tutorial_id -> tutorial_scripts.json)
        stage = self._current_stage()
        tasks = (stage or {}).get("tasks") or []
        if isinstance(tasks, list):
            for task in tasks:
                if not isinstance(task, dict):
                    continue
                tut = str(task.get("tutorial_id") or "").strip()
                if tut:
                    refs.append(("tutorial", tut, tut not in self.tutorial_scripts_by_id))

        # Unique
        seen = set()
        for kind, ident, missing in refs:
            key = (kind, ident)
            if key in seen:
                continue
            seen.add(key)
            label = f"{kind}: {ident}"
            item = QListWidgetItem(label)
            item.setData(Qt.UserRole, RefJump(kind=kind, ident=ident))
            if missing:
                item.setForeground(Qt.red)
                item.setToolTip("Missing reference: create or fix the referenced ID.")
            self.refs_list.addItem(item)

    def _open_ref_from_context(self, item: QListWidgetItem):
        ref = item.data(Qt.UserRole)
        if not isinstance(ref, RefJump):
            return
        self._open_reference(ref.kind, ref.ident)

    def _on_task_double_click(self, row: int, col: int):
        if col == 2:
            tut_item = self.task_table.item(row, 2)
            tutorial_id = tut_item.text().strip() if tut_item else ""
            if tutorial_id:
                self._quick_edit_tutorial_script(tutorial_id)
                self._refresh_context()
                return
        # Otherwise, edit the task itself.
        try:
            self.task_table.setCurrentCell(row, 0)
        except Exception:
            pass
        self._edit_selected_task()

    # -------------------- Preview --------------------

    def _update_beat_preview(self):
        if self._updating_ui:
            return
        t = self.beat_type_combo.currentText().strip() or "note"
        ref = self.ref_combo.currentText().strip()
        label = self.beat_label_edit.text().strip()
        note = self.note_edit.toPlainText().strip()

        lines: List[str] = []
        lines.append(f"Type: {t}")
        if label:
            lines.append(f"Label: {label}")
        if t == "note":
            if note:
                lines.append("")
                lines.append(note)
            self.beat_preview.setPlainText("\n".join(lines))
            return

        lines.append(f"Ref: {ref or '(none)'}")
        if note:
            lines.append(f"Note: {_fmt_one_line(note, 140)}")

        if t == "dialogue" and ref:
            dlg = self.dialogues_by_id.get(ref)
            if dlg:
                lines.append("")
                sp = str(dlg.get("speaker") or "").strip()
                tx = str(dlg.get("text") or "").strip()
                if sp:
                    lines.append(f"{sp}:")
                if tx:
                    lines.append(tx)
                nxt = dlg.get("next")
                if nxt:
                    lines.append("")
                    lines.append(f"Next: {nxt}")
            else:
                lines.append("")
                lines.append("(Missing dialogue)")
        elif t == "event" and ref:
            ev = self.events_by_id.get(ref)
            if ev:
                lines.append("")
                lines.append(str(ev.get("description") or ""))
                trg = ev.get("trigger") or {}
                if isinstance(trg, dict):
                    lines.append("")
                    lines.append("Trigger:")
                    lines.append(json.dumps(trg, ensure_ascii=False, indent=2))
            else:
                lines.append("")
                lines.append("(Missing event)")
        elif t == "cutscene" and ref:
            sc = self.cinematics_by_id.get(ref)
            if sc:
                lines.append("")
                lines.append(str(sc.get("title") or ""))
                steps = sc.get("steps") or []
                if isinstance(steps, list) and steps:
                    lines.append("")
                    lines.append("Steps:")
                    for step in steps[:6]:
                        if not isinstance(step, dict):
                            continue
                        st = str(step.get("type") or "narration")
                        sp = str(step.get("speaker") or "").strip()
                        tx = str(step.get("text") or step.get("line") or "").strip()
                        tx = _fmt_one_line(tx, 90)
                        if sp:
                            lines.append(f"- {st}: {sp} — {tx}")
                        else:
                            lines.append(f"- {st}: {tx}")
                    if len(steps) > 6:
                        lines.append("…")
            else:
                lines.append("")
                lines.append("(Missing cutscene)")
        elif t == "tutorial" and ref:
            ts = self.tutorial_scripts_by_id.get(ref)
            if ts:
                steps = ts.get("steps") or []
                if isinstance(steps, list) and steps:
                    lines.append("")
                    lines.append("Steps:")
                    for step in steps[:6]:
                        if not isinstance(step, dict):
                            continue
                        msg = _fmt_one_line(str(step.get("message") or ""), 90)
                        ctx = str(step.get("context") or "").strip()
                        delay = step.get("delay_ms")
                        key = str(step.get("key") or "").strip()
                        extra = []
                        if key:
                            extra.append(f"key={key}")
                        if ctx:
                            extra.append(f"ctx={ctx}")
                        if delay:
                            extra.append(f"delay={delay}ms")
                        suffix = f" ({', '.join(extra)})" if extra else ""
                        lines.append(f"- {msg}{suffix}")
                    if len(steps) > 6:
                        lines.append("…")
            else:
                lines.append("")
                lines.append("(Missing tutorial script)")
        elif t == "milestone" and ref:
            ms = self.milestones_by_id.get(ref)
            if ms:
                lines.append("")
                lines.append(str(ms.get("name") or ""))
                d = str(ms.get("description") or "").strip()
                if d:
                    lines.append("")
                    lines.append(d)
            else:
                lines.append("")
                lines.append("(Missing milestone)")

        self.beat_preview.setPlainText("\n".join(lines))

    # -------------------- Validation / Export --------------------

    def _normalize_events_schema(self):
        """
        Fix common legacy key names in events.json so they match the Kotlin runtime models.

        - "else" -> "elseDo"
        - "cutscene_id" -> "scene_id"
        - "onComplete" -> "on_complete"
        """
        if QMessageBox.question(
            self,
            "Normalize Events",
            "This will rewrite in-memory event JSON to match the runtime schema (recommended).\n\nProceed?",
        ) != QMessageBox.Yes:
            return

        changes = 0
        for ev in self.events_by_id.values():
            if isinstance(ev, dict):
                changes += self._normalize_event_obj_in_place(ev)

        if changes:
            self._mark_dirty("events")
            if self.validation_out:
                self.validation_out.setPlainText(f"Normalized events schema: {changes} change(s) applied.\nSave to persist.")
            self._refresh_context()
            flash_status(self, f"Normalized events ({changes} changes).", 1800)
        else:
            flash_status(self, "Events already normalized.", 1400)

    def _normalize_event_obj_in_place(self, obj: Any) -> int:
        changes = 0
        if isinstance(obj, list):
            for it in obj:
                changes += self._normalize_event_obj_in_place(it)
            return changes
        if not isinstance(obj, dict):
            return 0

        if "else" in obj and "elseDo" not in obj:
            obj["elseDo"] = obj.pop("else")
            changes += 1
        if "cutscene_id" in obj and "scene_id" not in obj:
            obj["scene_id"] = obj.pop("cutscene_id")
            changes += 1
        if "onComplete" in obj and "on_complete" not in obj:
            obj["on_complete"] = obj.pop("onComplete")
            changes += 1

        for v in obj.values():
            if isinstance(v, (dict, list)):
                changes += self._normalize_event_obj_in_place(v)
        return changes

    def _quest_stage_task_maps(self) -> Tuple[Dict[str, set[str]], Dict[str, set[str]]]:
        stage_ids_by_quest: Dict[str, set[str]] = {}
        task_ids_by_quest: Dict[str, set[str]] = {}
        for q in self.quests:
            if not isinstance(q, dict):
                continue
            qid = str(q.get("id") or "").strip()
            if not qid:
                continue
            stage_ids: set[str] = set()
            task_ids: set[str] = set()
            stages = q.get("stages") or []
            if isinstance(stages, list):
                for idx, st in enumerate(stages):
                    if not isinstance(st, dict):
                        continue
                    sid = str(st.get("id") or f"stage_{idx}").strip()
                    if sid:
                        stage_ids.add(sid)
                    tasks = st.get("tasks") or []
                    if not isinstance(tasks, list):
                        continue
                    for t in tasks:
                        if not isinstance(t, dict):
                            continue
                        tid = str(t.get("id") or "").strip()
                        if tid:
                            task_ids.add(tid)
            stage_ids_by_quest[qid] = stage_ids
            task_ids_by_quest[qid] = task_ids
        return stage_ids_by_quest, task_ids_by_quest

    def _tutorial_completion_keys(self) -> set[str]:
        keys: set[str] = set(_sorted_ids(self.tutorial_scripts_by_id))
        for script in self.tutorial_scripts_by_id.values():
            if not isinstance(script, dict):
                continue
            steps = script.get("steps") or []
            if not isinstance(steps, list):
                continue
            for step in steps:
                if not isinstance(step, dict):
                    continue
                k = str(step.get("key") or "").strip()
                if k:
                    keys.add(k)
        return keys

    def _validate_dialogue_chain(
        self,
        start_id: str,
        ctx: str,
        stage_ids_by_quest: Dict[str, set[str]],
        task_ids_by_quest: Dict[str, set[str]],
        tutorial_keys: set[str],
        visited_global: set[str],
    ) -> List[str]:
        out: List[str] = []
        cur = (start_id or "").strip()
        if not cur:
            return out
        local_seen: set[str] = set()
        limit = 250
        for hop in range(limit):
            if cur in local_seen:
                out.append(f"{ctx}: dialogue chain loops at '{cur}'")
                return out
            local_seen.add(cur)
            if cur in visited_global:
                return out
            visited_global.add(cur)

            dlg = self.dialogues_by_id.get(cur)
            if not isinstance(dlg, dict):
                out.append(f"{ctx}: missing dialogue '{cur}' (referenced by chain)")
                return out
            speaker = str(dlg.get("speaker") or "").strip()
            text = str(dlg.get("text") or "").strip()
            if not speaker:
                out.append(f"{ctx}: dialogue '{cur}' missing speaker")
            if not text:
                out.append(f"{ctx}: dialogue '{cur}' missing text")

            cond = str(dlg.get("condition") or "").strip()
            trig = str(dlg.get("trigger") or "").strip()
            out.extend(self._validate_dialogue_condition(cond, f"{ctx}: dialogue '{cur}'", stage_ids_by_quest, tutorial_keys))
            out.extend(
                self._validate_dialogue_trigger(
                    trig,
                    f"{ctx}: dialogue '{cur}'",
                    stage_ids_by_quest,
                    task_ids_by_quest,
                    tutorial_keys,
                )
            )

            nxt = str(dlg.get("next") or "").strip()
            if nxt and nxt not in self.dialogues_by_id:
                out.append(f"{ctx}: dialogue '{cur}' next -> missing '{nxt}'")
                return out
            if not nxt:
                return out
            cur = nxt

        out.append(f"{ctx}: dialogue chain exceeded {limit} hops (possible loop)")
        return out

    def _validate_dialogue_condition(
        self,
        condition: str,
        ctx: str,
        stage_ids_by_quest: Dict[str, set[str]],
        tutorial_keys: set[str],
    ) -> List[str]:
        out: List[str] = []
        cond = (condition or "").strip()
        if not cond:
            return out

        valid_types = {t.lower() for t in DialogueQuickEdit._COND_TYPES}
        tokens = [t.strip() for t in cond.split(",") if t.strip()]
        for tok in tokens:
            parts = tok.split(":", 1)
            t_raw = parts[0].strip()
            t = t_raw.lower()
            value = parts[1].strip() if len(parts) > 1 else ""

            if t not in valid_types:
                out.append(f"{ctx}: unknown dialogue condition type '{t_raw}' ({tok})")
                continue
            if not value:
                out.append(f"{ctx}: dialogue condition '{t_raw}' missing value")
                continue

            if t in ("quest", "quest_active", "quest_completed", "quest_not_started", "quest_failed"):
                if value not in self.quests_by_id:
                    out.append(f"{ctx}: dialogue condition '{t_raw}' references missing quest '{value}'")
            elif t in ("quest_stage", "quest_stage_not"):
                qp = value.split(":", 1)
                qid = qp[0].strip() if qp else ""
                sid = qp[1].strip() if len(qp) > 1 else ""
                if not qid or not sid:
                    out.append(f"{ctx}: dialogue condition '{t_raw}' expects quest_id:stage_id (got '{value}')")
                    continue
                if qid not in self.quests_by_id:
                    out.append(f"{ctx}: dialogue condition '{t_raw}' references missing quest '{qid}'")
                    continue
                if sid not in stage_ids_by_quest.get(qid, set()):
                    out.append(f"{ctx}: dialogue condition '{t_raw}' references missing stage '{qid}:{sid}'")
            elif t in ("milestone", "milestone_not_set"):
                if value not in self.milestones_by_id:
                    out.append(f"{ctx}: dialogue condition '{t_raw}' references missing milestone '{value}'")
            elif t in ("item", "item_not"):
                if value not in self.items_by_id:
                    out.append(f"{ctx}: dialogue condition '{t_raw}' references missing item '{value}'")
            elif t in ("event_completed", "event_not_completed"):
                if value not in self.events_by_id:
                    out.append(f"{ctx}: dialogue condition '{t_raw}' references missing event '{value}'")
            elif t in ("tutorial_completed", "tutorial_not_completed"):
                if value not in tutorial_keys and not value.lower().startswith("system:"):
                    out.append(f"{ctx}: dialogue condition '{t_raw}' references unknown tutorial key '{value}'")
        return out

    def _validate_dialogue_trigger(
        self,
        trigger: str,
        ctx: str,
        stage_ids_by_quest: Dict[str, set[str]],
        task_ids_by_quest: Dict[str, set[str]],
        tutorial_keys: set[str],
    ) -> List[str]:
        out: List[str] = []
        trig = (trigger or "").strip()
        if not trig:
            return out

        valid_types = {t.lower() for t in DialogueQuickEdit._TRIG_TYPES}
        tokens = [t.strip() for t in trig.split(",") if t.strip()]
        for tok in tokens:
            parts = tok.split(":", 1)
            t_raw = parts[0].strip()
            t = t_raw.lower()
            value = parts[1].strip() if len(parts) > 1 else ""

            if t not in valid_types:
                out.append(f"{ctx}: unknown dialogue trigger type '{t_raw}' ({tok})")
                continue

            if t == "untrack_quest":
                if value:
                    out.append(f"{ctx}: trigger 'untrack_quest' should not have a value ({tok})")
                continue

            if not value:
                out.append(f"{ctx}: dialogue trigger '{t_raw}' missing value")
                continue

            if t in ("start_quest", "complete_quest", "fail_quest", "track_quest", "advance_quest"):
                if value not in self.quests_by_id:
                    out.append(f"{ctx}: dialogue trigger '{t_raw}' references missing quest '{value}'")
            elif t in ("set_milestone", "clear_milestone"):
                if value not in self.milestones_by_id:
                    out.append(f"{ctx}: dialogue trigger '{t_raw}' references missing milestone '{value}'")
            elif t in ("give_item", "take_item"):
                item_id = value
                for delim in ("*", "x", "|"):
                    if delim in value:
                        item_id = value.split(delim, 1)[0].strip()
                        break
                if item_id and item_id not in self.items_by_id:
                    out.append(f"{ctx}: dialogue trigger '{t_raw}' references missing item '{item_id}'")
            elif t in ("give_credits", "give_xp"):
                try:
                    amt = int(value)
                except Exception:
                    amt = 0
                if amt <= 0:
                    out.append(f"{ctx}: dialogue trigger '{t_raw}' expects a positive integer (got '{value}')")
            elif t == "set_quest_task_done":
                qp = value.split(":", 1)
                qid = qp[0].strip() if qp else ""
                tid = qp[1].strip() if len(qp) > 1 else ""
                if not qid or not tid:
                    out.append(f"{ctx}: trigger 'set_quest_task_done' expects quest_id:task_id (got '{value}')")
                    continue
                if qid not in self.quests_by_id:
                    out.append(f"{ctx}: trigger 'set_quest_task_done' references missing quest '{qid}'")
                    continue
                if tid not in task_ids_by_quest.get(qid, set()):
                    out.append(f"{ctx}: trigger 'set_quest_task_done' references missing task '{qid}:{tid}'")
            elif t == "advance_quest_stage":
                qp = value.split(":", 1)
                qid = qp[0].strip() if qp else ""
                sid = qp[1].strip() if len(qp) > 1 else ""
                if not qid or not sid:
                    out.append(f"{ctx}: trigger 'advance_quest_stage' expects quest_id:stage_id (got '{value}')")
                    continue
                if qid not in self.quests_by_id:
                    out.append(f"{ctx}: trigger 'advance_quest_stage' references missing quest '{qid}'")
                    continue
                if sid not in stage_ids_by_quest.get(qid, set()):
                    out.append(f"{ctx}: trigger 'advance_quest_stage' references missing stage '{qid}:{sid}'")
            elif t == "system_tutorial":
                scene_id = value.split("|", 1)[0].strip()
                if scene_id and scene_id not in self.tutorial_scripts_by_id and scene_id not in tutorial_keys:
                    out.append(f"{ctx}: trigger 'system_tutorial' references missing tutorial script '{scene_id}' (will fall back)")
            elif t == "play_cinematic":
                if value not in self.cinematics_by_id:
                    out.append(f"{ctx}: trigger 'play_cinematic' references missing cinematic '{value}'")
            elif t == "player_action":
                if value not in self.player_action_ids:
                    out.append(f"{ctx}: trigger 'player_action' references unknown action '{value}'")
        return out

    def _validate_event_asset(
        self,
        event_id: str,
        ctx: str,
        stage_ids_by_quest: Dict[str, set[str]],
        task_ids_by_quest: Dict[str, set[str]],
        tutorial_keys: set[str],
    ) -> List[str]:
        out: List[str] = []
        ev = self.events_by_id.get(event_id)
        if not isinstance(ev, dict):
            out.append(f"{ctx}: missing event '{event_id}'")
            return out

        # Trigger
        trigger = ev.get("trigger")
        if not isinstance(trigger, dict):
            out.append(f"{ctx}: event '{event_id}' trigger is not an object")
        else:
            t_raw = str(trigger.get("type") or "").strip()
            t = t_raw.lower()
            valid_triggers = {tt.lower() for tt in EVENT_TRIGGER_TYPES}
            if not t_raw:
                out.append(f"{ctx}: event '{event_id}' trigger missing type")
            elif t not in valid_triggers:
                out.append(f"{ctx}: event '{event_id}' trigger type '{t_raw}' is unknown (runtime may treat as always-matching)")

            if t in ("talk_to", "npc_interaction", "dialogue_closed", "dialogue_dismissed"):
                npc = str(trigger.get("npc") or "").strip()
                if not npc:
                    out.append(f"{ctx}: event '{event_id}' trigger '{t_raw}' missing npc")
                elif npc not in set(self.npc_ids):
                    out.append(f"{ctx}: event '{event_id}' trigger npc '{npc}' not found in npcs.json")
            elif t == "enter_room":
                room = str(trigger.get("room_id") or trigger.get("room") or "").strip()
                if room and room not in self.rooms_by_id:
                    out.append(f"{ctx}: event '{event_id}' trigger room '{room}' not found in rooms.json")
            elif t == "player_action":
                action_id = str(trigger.get("action") or "").strip()
                if action_id and action_id not in self.player_action_ids:
                    out.append(f"{ctx}: event '{event_id}' trigger action '{action_id}' not found in rooms.json actions")
                item_id = str(trigger.get("item_id") or trigger.get("item") or "").strip()
                if item_id and item_id not in self.items_by_id:
                    out.append(f"{ctx}: event '{event_id}' trigger item '{item_id}' not found in items.json")
            elif t == "item_acquired":
                item_id = str(trigger.get("item_id") or trigger.get("item") or "").strip()
                if item_id and item_id not in self.items_by_id:
                    out.append(f"{ctx}: event '{event_id}' trigger item '{item_id}' not found in items.json")
            elif t == "quest_stage_complete":
                qid = str(trigger.get("quest_id") or "").strip()
                if not qid:
                    out.append(f"{ctx}: event '{event_id}' trigger 'quest_stage_complete' missing quest_id")
                elif qid not in self.quests_by_id:
                    out.append(f"{ctx}: event '{event_id}' trigger quest_id '{qid}' not found in quests.json")

        # Conditions
        conditions = ev.get("conditions") or []
        if conditions and not isinstance(conditions, list):
            out.append(f"{ctx}: event '{event_id}' conditions is not a list")
            conditions = []
        valid_conditions = {ct.lower() for ct in EVENT_CONDITION_TYPES}
        for i, cond in enumerate(conditions):
            if not isinstance(cond, dict):
                out.append(f"{ctx}: event '{event_id}' conditions[{i}] is not an object")
                continue
            c_raw = str(cond.get("type") or "").strip()
            c = c_raw.lower()
            if not c_raw:
                out.append(f"{ctx}: event '{event_id}' conditions[{i}] missing type")
                continue
            if c not in valid_conditions:
                out.append(f"{ctx}: event '{event_id}' conditions[{i}] unknown type '{c_raw}' (runtime may treat as true)")

            if c in ("milestone_not_set", "milestone_set"):
                mid = str(cond.get("milestone") or "").strip()
                if not mid:
                    out.append(f"{ctx}: event '{event_id}' conditions[{i}] '{c_raw}' missing milestone (runtime may treat as true)")
                elif mid not in self.milestones_by_id:
                    out.append(f"{ctx}: event '{event_id}' conditions[{i}] milestone '{mid}' not found in milestones.json")
            elif c.startswith("quest_") or c in ("quest_stage", "quest_stage_not", "quest_task_done", "quest_task_not_done"):
                qid = str(cond.get("quest_id") or "").strip()
                if not qid:
                    out.append(f"{ctx}: event '{event_id}' conditions[{i}] '{c_raw}' missing quest_id")
                elif qid not in self.quests_by_id:
                    out.append(f"{ctx}: event '{event_id}' conditions[{i}] quest_id '{qid}' not found in quests.json")
                if c in ("quest_stage", "quest_stage_not"):
                    sid = str(cond.get("stage_id") or "").strip()
                    if not sid:
                        out.append(f"{ctx}: event '{event_id}' conditions[{i}] '{c_raw}' missing stage_id")
                    elif qid and sid not in stage_ids_by_quest.get(qid, set()):
                        out.append(f"{ctx}: event '{event_id}' conditions[{i}] stage '{qid}:{sid}' not found in quests.json")
                if c in ("quest_task_done", "quest_task_not_done"):
                    tid = str(cond.get("task_id") or "").strip()
                    if not tid:
                        out.append(f"{ctx}: event '{event_id}' conditions[{i}] '{c_raw}' missing task_id")
                    elif qid and tid not in task_ids_by_quest.get(qid, set()):
                        out.append(f"{ctx}: event '{event_id}' conditions[{i}] task '{qid}:{tid}' not found in quests.json")
            elif c in ("event_completed", "event_not_completed"):
                eid = str(cond.get("event_id") or "").strip()
                if not eid:
                    out.append(f"{ctx}: event '{event_id}' conditions[{i}] '{c_raw}' missing event_id")
                elif eid not in self.events_by_id:
                    out.append(f"{ctx}: event '{event_id}' conditions[{i}] event_id '{eid}' not found in events.json")
            elif c in ("tutorial_completed", "tutorial_not_completed"):
                tid = str(cond.get("tutorial_id") or "").strip()
                if not tid:
                    out.append(f"{ctx}: event '{event_id}' conditions[{i}] '{c_raw}' missing tutorial_id")
                elif tid not in tutorial_keys and not tid.lower().startswith("system:"):
                    out.append(f"{ctx}: event '{event_id}' conditions[{i}] tutorial_id '{tid}' not found in tutorial_scripts.json")
            elif c in ("item", "item_not"):
                item_id = str(cond.get("item_id") or cond.get("item") or "").strip()
                if not item_id:
                    out.append(f"{ctx}: event '{event_id}' conditions[{i}] '{c_raw}' missing item_id (runtime may treat as true for item_not)")
                elif item_id not in self.items_by_id:
                    out.append(f"{ctx}: event '{event_id}' conditions[{i}] item '{item_id}' not found in items.json")

        # Actions
        actions = ev.get("actions") or []
        if actions and not isinstance(actions, list):
            out.append(f"{ctx}: event '{event_id}' actions is not a list")
            return out
        out.extend(self._validate_event_actions(actions, f"{ctx}: event '{event_id}'", stage_ids_by_quest, task_ids_by_quest, tutorial_keys))
        return out

    def _validate_event_actions(
        self,
        actions: Any,
        ctx: str,
        stage_ids_by_quest: Dict[str, set[str]],
        task_ids_by_quest: Dict[str, set[str]],
        tutorial_keys: set[str],
        *,
        path: str = "actions",
    ) -> List[str]:
        out: List[str] = []
        if not actions:
            return out
        if not isinstance(actions, list):
            out.append(f"{ctx}: {path} is not a list")
            return out

        for i, act in enumerate(actions):
            p = f"{path}[{i}]"
            if not isinstance(act, dict):
                out.append(f"{ctx}: {p} is not an object")
                continue
            t_raw = str(act.get("type") or "").strip()
            if not t_raw:
                out.append(f"{ctx}: {p} missing type")
            t = t_raw.lower()
            if t_raw and t not in {at.lower() for at in EVENT_ACTION_TYPES}:
                out.append(f"{ctx}: {p} action type '{t_raw}' is unknown (runtime will likely skip it)")

            if "else" in act and "elseDo" not in act:
                out.append(f"{ctx}: {p} uses key 'else' (runtime expects 'elseDo')")
            if "cutscene_id" in act and "scene_id" not in act:
                out.append(f"{ctx}: {p} uses key 'cutscene_id' (runtime expects 'scene_id')")

            qid = str(act.get("quest_id") or "").strip()
            if qid and qid not in self.quests_by_id:
                out.append(f"{ctx}: {p} quest_id '{qid}' not found in quests.json")

            stage_id = str(act.get("to_stage_id") or act.get("stage_id") or "").strip()
            if stage_id and qid and stage_id not in stage_ids_by_quest.get(qid, set()):
                out.append(f"{ctx}: {p} stage '{qid}:{stage_id}' not found in quests.json")

            task_id = str(act.get("task_id") or "").strip()
            if task_id and qid and task_id not in task_ids_by_quest.get(qid, set()):
                out.append(f"{ctx}: {p} task '{qid}:{task_id}' not found in quests.json")

            if t in ("play_cinematic", "trigger_cutscene"):
                scene_id = str(act.get("scene_id") or act.get("cutscene_id") or "").strip()
                if not scene_id:
                    out.append(f"{ctx}: {p} '{t_raw}' missing scene_id")
                elif scene_id not in self.cinematics_by_id:
                    out.append(f"{ctx}: {p} cinematic '{scene_id}' not found in cinematics.json")
            elif t == "system_tutorial":
                scene_id = str(act.get("scene_id") or act.get("tutorial_id") or "").strip()
                if scene_id and scene_id not in self.tutorial_scripts_by_id:
                    out.append(
                        f"{ctx}: {p} tutorial script '{scene_id}' not found in tutorial_scripts.json (will fall back)"
                    )

            mid = str(act.get("milestone") or "").strip()
            if mid and mid not in self.milestones_by_id:
                out.append(f"{ctx}: {p} milestone '{mid}' not found in milestones.json")
            for list_key in ("milestones", "set_milestones", "clear_milestones"):
                vals = act.get(list_key)
                if not vals:
                    continue
                if not isinstance(vals, list):
                    out.append(f"{ctx}: {p} {list_key} is not a list")
                    continue
                for j, v in enumerate(vals):
                    vid = str(v or "").strip()
                    if vid and vid not in self.milestones_by_id:
                        out.append(f"{ctx}: {p} {list_key}[{j}] milestone '{vid}' not found in milestones.json")

            tut = str(act.get("tutorial_id") or "").strip()
            if t != "system_tutorial" and tut and tut not in tutorial_keys and not tut.lower().startswith("system:"):
                out.append(f"{ctx}: {p} tutorial_id '{tut}' not found in tutorial_scripts.json")

            item_id = str(act.get("item_id") or act.get("item") or "").strip()
            if item_id and item_id not in self.items_by_id:
                out.append(f"{ctx}: {p} item '{item_id}' not found in items.json")

            room_id = str(act.get("room_id") or "").strip()
            if room_id and room_id not in self.rooms_by_id:
                out.append(f"{ctx}: {p} room_id '{room_id}' not found in rooms.json")

            action_id = str(act.get("action") or "").strip()
            if action_id and action_id not in self.player_action_ids and t == "player_action":
                out.append(f"{ctx}: {p} player_action '{action_id}' not found in rooms.json actions")

            # Recurse: accept both runtime key and common JSON shorthands.
            for child_key in ("do", "else", "elseDo", "on_complete", "onComplete"):
                child = act.get(child_key)
                if child is not None:
                    out.extend(
                        self._validate_event_actions(
                            child,
                            ctx,
                            stage_ids_by_quest,
                            task_ids_by_quest,
                            tutorial_keys,
                            path=f"{p}.{child_key}",
                        )
                    )

        return out

    def _validate_current_stage(self):
        msgs = self._validate_stage(self.current_quest_id, self.current_stage_id)
        if not msgs:
            self.validation_out.setPlainText("OK — no issues found for current stage.")
        else:
            self.validation_out.setPlainText("\n".join(msgs))

    def _validate_all_flows(self):
        msgs: List[str] = []
        for qid, stages in sorted((self.flows or {}).items(), key=lambda x: str(x[0]).lower()):
            if not isinstance(stages, dict):
                continue
            for sid in sorted(stages.keys(), key=str.lower):
                msgs.extend(self._validate_stage(qid, sid))
        if not msgs:
            self.validation_out.setPlainText("OK — no issues found in narrative flows.")
        else:
            self.validation_out.setPlainText("\n".join(msgs))

    def _validate_stage(self, quest_id: Optional[str], stage_id: Optional[str]) -> List[str]:
        if not quest_id or not stage_id:
            return ["Select a quest stage first."]
        out: List[str] = []
        stage_ids_by_quest, task_ids_by_quest = self._quest_stage_task_maps()
        tutorial_keys = self._tutorial_completion_keys()
        validated_dialogue: set[str] = set()
        validated_events: set[str] = set()
        beats = self.flows.get(quest_id, {}).get(stage_id, []) if isinstance(self.flows.get(quest_id, {}), dict) else []
        if not isinstance(beats, list):
            beats = []
        for idx, beat in enumerate(beats):
            if not isinstance(beat, dict):
                out.append(f"{quest_id}#{stage_id} beat {idx + 1}: invalid beat (not an object)")
                continue
            t = str(beat.get("type") or "").strip() or "note"
            if t not in BEAT_TYPES:
                out.append(f"{quest_id}#{stage_id} beat {idx + 1}: unknown type '{t}'")
                continue
            if t == "note":
                continue
            rid = str(beat.get("id") or "").strip()
            if not rid:
                out.append(f"{quest_id}#{stage_id} beat {idx + 1}: '{t}' missing id")
                continue
            if t == "dialogue" and rid not in self.dialogues_by_id:
                out.append(f"{quest_id}#{stage_id} beat {idx + 1}: missing dialogue '{rid}'")
            if t == "dialogue" and rid in self.dialogues_by_id:
                out.extend(
                    self._validate_dialogue_chain(
                        rid,
                        f"{quest_id}#{stage_id} beat {idx + 1}",
                        stage_ids_by_quest,
                        task_ids_by_quest,
                        tutorial_keys,
                        validated_dialogue,
                    )
                )
            if t == "event" and rid not in self.events_by_id:
                out.append(f"{quest_id}#{stage_id} beat {idx + 1}: missing event '{rid}'")
            if t == "event" and rid in self.events_by_id and rid not in validated_events:
                validated_events.add(rid)
                out.extend(
                    self._validate_event_asset(
                        rid,
                        f"{quest_id}#{stage_id} beat {idx + 1}",
                        stage_ids_by_quest,
                        task_ids_by_quest,
                        tutorial_keys,
                    )
                )
            if t == "cutscene" and rid not in self.cinematics_by_id:
                out.append(f"{quest_id}#{stage_id} beat {idx + 1}: missing cutscene '{rid}'")
            if t == "tutorial" and rid not in self.tutorial_scripts_by_id:
                out.append(f"{quest_id}#{stage_id} beat {idx + 1}: missing tutorial script '{rid}'")
            if t == "milestone" and rid not in self.milestones_by_id:
                out.append(f"{quest_id}#{stage_id} beat {idx + 1}: missing milestone '{rid}'")

        # Tasks: tutorial_id references tutorial_scripts.json
        quest = self.quests_by_id.get(quest_id)
        stages = (quest or {}).get("stages") or []
        stage_obj: Optional[dict] = None
        if isinstance(stages, list):
            for st in stages:
                if isinstance(st, dict) and str(st.get("id") or "").strip() == stage_id:
                    stage_obj = st
                    break
            if stage_obj is None:
                for idx, st in enumerate(stages):
                    if isinstance(st, dict) and stage_id == f"stage_{idx}":
                        stage_obj = st
                        break
        tasks = (stage_obj or {}).get("tasks") or []
        if isinstance(tasks, list):
            for t in tasks:
                if not isinstance(t, dict):
                    continue
                tid = str(t.get("id") or "").strip() or "(task)"
                tut = str(t.get("tutorial_id") or "").strip()
                if tut and tut not in self.tutorial_scripts_by_id:
                    out.append(f"{quest_id}#{stage_id} task '{tid}': missing tutorial script '{tut}'")
        return out

    def _export_stage_markdown(self):
        quest = self._current_quest()
        stage = self._current_stage()
        if not quest or not stage or not self.current_quest_id or not self.current_stage_id:
            QMessageBox.information(self, "Export", "Select a quest stage first.")
            return
        default_name = f"{self.current_quest_id}__{self.current_stage_id}.md"
        default_dir = (self.project_root / "reports" / "narrative").resolve()
        default_dir.mkdir(parents=True, exist_ok=True)
        path_str, _ = QFileDialog.getSaveFileName(self, "Export Stage Markdown", str(default_dir / default_name), "Markdown (*.md)")
        if not path_str:
            return
        path = Path(path_str)
        md = self._stage_to_markdown(quest, stage, self._stage_flow())
        try:
            path.write_text(md, encoding="utf-8")
        except Exception as exc:
            QMessageBox.critical(self, "Export Failed", f"Could not write:\n{path}\n\n{exc}")
            return
        flash_status(self, f"Exported {path.name}", 1600)

    def _stage_to_markdown(self, quest: dict, stage: dict, beats: List[dict]) -> str:
        qid = str(quest.get("id") or "")
        qtitle = str(quest.get("title") or qid)
        sid = str(stage.get("id") or "")
        stitle = str(stage.get("title") or sid)
        sdesc = str(stage.get("description") or "").strip()

        lines: List[str] = []
        lines.append(f"# Quest: {qtitle} ({qid})")
        lines.append(f"## Stage: {stitle} ({sid})")
        if sdesc:
            lines.append("")
            lines.append(sdesc)

        tasks = stage.get("tasks") or []
        if isinstance(tasks, list) and tasks:
            lines.append("")
            lines.append("### Tasks")
            for t in tasks:
                if not isinstance(t, dict):
                    continue
                tid = str(t.get("id") or "").strip()
                txt = _fmt_one_line(str(t.get("text") or ""))
                tut = str(t.get("tutorial_id") or "").strip()
                extra = f" (tutorial: {tut})" if tut else ""
                lines.append(f"- `{tid}`: {txt}{extra}")

        lines.append("")
        lines.append("### Beat Flow")
        for i, beat in enumerate(beats, start=1):
            if not isinstance(beat, dict):
                continue
            t = str(beat.get("type") or "note").strip() or "note"
            lbl = str(beat.get("label") or "").strip()
            rid = str(beat.get("id") or "").strip()
            text = str(beat.get("text") or "").strip()
            if t == "note":
                head = lbl or "Note"
                lines.append(f"{i}. **[{t}]** {head}")
                if text:
                    lines.append("")
                    for ln in text.splitlines():
                        lines.append(f"   {ln}")
                continue
            head = lbl or rid or "(unlinked)"
            lines.append(f"{i}. **[{t}]** {head} (`{rid}`)")
            if text:
                lines.append(f"   - note: {text}")
        lines.append("")
        return "\n".join(lines)

    # -------------------- Dirty state --------------------

    def _mark_dirty(self, key: str):
        if key in self._dirty:
            self._dirty[key] = True
        self._dirty_any = True
        self._update_title()

    def _update_title(self):
        title = self._base_title
        if self._dirty_any:
            title += " *"
        self.setWindowTitle(title)


def main(argv: Optional[Sequence[str]] = None) -> int:
    import argparse

    parser = argparse.ArgumentParser(description="Starborn Narrative Studio (PyQt)")
    parser.add_argument("--root", type=str, help="Project root or assets dir (optional).")
    args = parser.parse_args(argv)

    app = QApplication([])
    w = NarrativeStudio(Path(args.root) if args.root else None)
    w.show()
    return int(app.exec_())


if __name__ == "__main__":
    raise SystemExit(main())
