#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations
from pathlib import Path
from typing import Callable, Optional
from PyQt5.QtCore import Qt
from PyQt5.QtGui import QIcon
from PyQt5.QtWidgets import (
    QStatusBar, QShortcut, QMenu, QListWidget, QTableWidget, QAction, QMessageBox, QWidget
)

def attach_status_bar(win) -> QStatusBar:
    """
    Ensures the editor window has a status bar and returns it.
    """
    sb = getattr(win, "_status_bar", None)
    if sb: return sb
    sb = QStatusBar()
    win.setStatusBar(sb) if hasattr(win, "setStatusBar") else None
    win._status_bar = sb
    return sb

def flash_status(win, msg: str, ms: int = 1800):
    sb = attach_status_bar(win)
    sb.showMessage(msg, ms)

def attach_hotkeys(win,
                   save_cb: Optional[Callable]=None,
                   delete_cb: Optional[Callable]=None,
                   duplicate_cb: Optional[Callable]=None,
                   search_cb: Optional[Callable]=None):
    """
    Adds common shortcuts if callbacks exist on the editor.
    """
    def _try(seq, cb):
        if cb:
            sc = QShortcut(seq, win)
            sc.activated.connect(cb)

    _try("Ctrl+S", save_cb or getattr(win, "save", None))
    _try("Ctrl+D", duplicate_cb or getattr(win, "duplicate_selected", None))
    _try("Delete", delete_cb or getattr(win, "delete_selected", None))
    _try("Ctrl+F", search_cb or getattr(win, "focus_search", None))
    _try("Ctrl+Z", getattr(getattr(win, "undo_manager", None), "stack", None).undo if hasattr(getattr(win, "undo_manager", None), "stack") else None)
    _try("Ctrl+Y", getattr(getattr(win, "undo_manager", None), "stack", None).redo if hasattr(getattr(win, "undo_manager", None), "stack") else None)

def attach_list_context_menu(widget, *,
                             on_new: Optional[Callable]=None,
                             on_dup: Optional[Callable]=None,
                             on_del: Optional[Callable]=None):
    """
    Right-click context menu for QListWidget or QTableWidget lists.
    """
    if not isinstance(widget, (QListWidget, QTableWidget)):
        return
    widget.setContextMenuPolicy(Qt.CustomContextMenu)
    def _open_menu(pos):
        m = QMenu(widget)
        if on_new: m.addAction(QAction("New", widget, triggered=on_new))
        if on_dup: m.addAction(QAction("Duplicate", widget, triggered=on_dup))
        if on_del: m.addAction(QAction("Delete", widget, triggered=on_del))
        if m.actions(): m.exec_(widget.mapToGlobal(pos))
    widget.customContextMenuRequested.connect(_open_menu)

# ---- Inline validation helpers ----

def mark_invalid(widget: QWidget, msg: str):
    widget.setToolTip(msg)
    widget.setStyleSheet("border: 1px solid #e5534b; border-radius: 3px;")

def clear_invalid(widget: QWidget):
    widget.setToolTip("")
    widget.setStyleSheet("")
