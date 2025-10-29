#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations
from PyQt5.QtWidgets import QUndoStack, QUndoCommand, QWidget

class _SetValueCmd(QUndoCommand):
    def __init__(self, widget: QWidget, old, new, label="Edit"):
        super().__init__(label)
        self.w, self.old, self.new = widget, old, new
    def undo(self):
        self._apply(self.old)
    def redo(self):
        self._apply(self.new)
    def _apply(self, v):
        # Supports common widget APIs
        if hasattr(self.w, "setText"): self.w.setText(str(v))
        elif hasattr(self.w, "setPlainText"): self.w.setPlainText(str(v))
        elif hasattr(self.w, "setValue"): self.w.setValue(v)
        elif hasattr(self.w, "setChecked"): self.w.setChecked(bool(v))

class UndoManager:
    """
    Per-editor undo stack. Call: um.watch_line_edit(le); um.watch_spin(spin), etc.
    """
    def __init__(self):
        self.stack = QUndoStack()

    def watch_line_edit(self, le):
        le.editingFinished.connect(lambda: self._record_if_changed(le, lambda: le.text()))

    def watch_plain_text(self, te):
        te.focusOutEvent_ = te.focusOutEvent
        def _wrap(ev):
            self._record_if_changed(te, lambda: te.toPlainText())
            te.focusOutEvent_(ev)
        te.focusOutEvent = _wrap

    def watch_spin(self, sp):
        sp.editingFinished.connect(lambda: self._record_if_changed(sp, lambda: sp.value()))

    def watch_checkbox(self, cb):
        cb.toggled.connect(lambda _: self._record_if_changed(cb, lambda: cb.isChecked()))

    def _record_if_changed(self, w, getter):
        new = getter()
        old = getattr(w, "_undo_last", None)
        if old is None:
            setattr(w, "_undo_last", new)
            return
        if new != old:
            cmd = _SetValueCmd(w, old, new)
            self.stack.push(cmd)
            setattr(w, "_undo_last", new)
