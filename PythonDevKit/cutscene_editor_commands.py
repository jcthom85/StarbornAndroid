# -*- coding: utf-8 -*-
# tools/cutscene_editor_commands.py
from __future__ import annotations

from typing import Any, Dict, List, TYPE_CHECKING
from PyQt5.QtWidgets import QUndoCommand

if TYPE_CHECKING:
    from cutscene_editor import CutsceneEditor


class ChangeValueCmd(QUndoCommand):
    """Undo/Redo for changing a value in a step's form."""
    def __init__(self, editor: "CutsceneEditor", step_index: int, key: str, old_val: Any, new_val: Any, label: str):
        super().__init__(label)
        self.editor = editor
        self.idx = step_index
        self.key = key
        self.old_val = old_val
        self.new_val = new_val

    def undo(self):
        scene = self.editor._scenes.get(self.editor._current_id)
        if scene and 0 <= self.idx < len(scene):
            scene[self.idx][self.key] = self.old_val
            self.editor._refresh_selected_row_summary()
            self.editor._build_step_form(scene[self.idx])
            self.editor._update_raw(scene[self.idx])
            self.editor._set_dirty(True)

    def redo(self):
        scene = self.editor._scenes.get(self.editor._current_id)
        if scene and 0 <= self.idx < len(scene):
            scene[self.idx][self.key] = self.new_val
            self.editor._refresh_selected_row_summary()
            self.editor._build_step_form(scene[self.idx])
            self.editor._update_raw(scene[self.idx])
            self.editor._set_dirty(True)


class AddStepCmd(QUndoCommand):
    """Undo/Redo for adding a step."""
    def __init__(self, editor: "CutsceneEditor", scene_id: str, index: int, step: Dict[str, Any]):
        super().__init__("Add step")
        self.editor = editor
        self.sid = scene_id
        self.idx = index
        self.step = step

    def undo(self):
        self.editor._scenes[self.sid].pop(self.idx)
        self.editor._populate_steps_table()
        self.editor.steps.selectRow(self.idx - 1)
        self.editor._set_dirty(True)

    def redo(self):
        self.editor._scenes[self.sid].insert(self.idx, self.step)
        self.editor._populate_steps_table()
        self.editor.steps.selectRow(self.idx)
        self.editor._set_dirty(True)


class DeleteStepCmd(QUndoCommand):
    """Undo/Redo for deleting a step."""
    def __init__(self, editor: "CutsceneEditor", scene_id: str, index: int, step: Dict[str, Any]):
        super().__init__("Delete step")
        self.editor = editor
        self.sid = scene_id
        self.idx = index
        self.step = step

    def undo(self):
        self.editor._scenes[self.sid].insert(self.idx, self.step)
        self.editor._populate_steps_table()
        self.editor.steps.selectRow(self.idx)
        self.editor._set_dirty(True)

    def redo(self):
        self.editor._scenes[self.sid].pop(self.idx)
        self.editor._populate_steps_table()
        self.editor.steps.selectRow(min(self.idx, self.editor.steps.rowCount() - 1))
        self.editor._set_dirty(True)


class MoveStepCmd(QUndoCommand):
    """Undo/Redo for moving a step up/down."""
    def __init__(self, editor: "CutsceneEditor", scene_id: str, from_idx: int, to_idx: int):
        super().__init__("Move step")
        self.editor = editor
        self.sid = scene_id
        self.from_idx = from_idx
        self.to_idx = to_idx

    def undo(self):
        steps = self.editor._scenes[self.sid]
        steps[self.from_idx], steps[self.to_idx] = steps[self.to_idx], steps[self.from_idx]
        self.editor._populate_steps_table()
        self.editor.steps.selectRow(self.from_idx)

    def redo(self):
        steps = self.editor._scenes[self.sid]
        steps[self.from_idx], steps[self.to_idx] = steps[self.to_idx], steps[self.from_idx]
        self.editor._populate_steps_table()
        self.editor.steps.selectRow(self.to_idx)