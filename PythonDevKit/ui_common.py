#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations
from typing import Callable, Optional, Dict, List, Any, Tuple
from PyQt5.QtCore import Qt, QSize, QTimer
from PyQt5.QtGui import QColor, QKeyEvent
from PyQt5.QtWidgets import (
    QStatusBar, QShortcut, QMenu, QListWidget, QTableWidget, QAction,
    QWidget, QDialog, QVBoxLayout, QLineEdit, QListWidgetItem, QLabel,
    QApplication, QGraphicsDropShadowEffect, QGroupBox, QFormLayout, QHBoxLayout
)

# -----------------------------------------------------------------------------
# Command Palette
# -----------------------------------------------------------------------------
class CommandPalette(QDialog):
    """
    A VS Code-style command palette (Ctrl+P).
    Usage:
        palette = CommandPalette(parent_window)
        palette.set_actions([
            ("File: Save All", lambda: win.save_all()),
            ("Editor: Toggle Theme", lambda: win.toggle_theme()),
        ])
        palette.exec_()
    """
    def __init__(self, parent=None):
        super().__init__(parent)
        self.setWindowFlags(Qt.Popup | Qt.FramelessWindowHint | Qt.NoDropShadowWindowHint)
        self.setAttribute(Qt.WA_DeleteOnClose)
        self.resize(600, 400)
        
        # Center on parent
        if parent:
            geo = parent.geometry()
            self.move(geo.center().x() - 300, geo.center().y() - 200)

        # Styling
        self.setStyleSheet("""
            QDialog {
                background-color: #2f3136;
                border: 1px solid #202225;
                border-radius: 6px;
            }
            QLineEdit {
                background-color: #202225;
                border: 1px solid #5865F2;
                border-radius: 4px;
                padding: 8px;
                font-size: 12pt;
                color: white;
            }
            QListWidget {
                background-color: #2f3136;
                border: none;
            }
            QListWidget::item {
                padding: 8px;
                border-radius: 4px;
                color: #dcddde;
            }
            QListWidget::item:selected {
                background-color: #5865F2;
                color: white;
            }
        """)

        layout = QVBoxLayout(self)
        layout.setContentsMargins(10, 10, 10, 10)
        
        self.search_box = QLineEdit()
        self.search_box.setPlaceholderText("Type a command...")
        self.search_box.textChanged.connect(self._filter)
        self.search_box.installEventFilter(self)
        layout.addWidget(self.search_box)

        self.list_widget = QListWidget()
        self.list_widget.itemActivated.connect(self._execute)
        layout.addWidget(self.list_widget)

        self._actions: List[Tuple[str, Callable]] = []
        
        # Drop shadow for depth
        shadow = QGraphicsDropShadowEffect(self)
        shadow.setBlurRadius(20)
        shadow.setXOffset(0)
        shadow.setYOffset(5)
        shadow.setColor(QColor(0, 0, 0, 100))
        self.setGraphicsEffect(shadow)

    def set_actions(self, actions: List[Tuple[str, Callable]]):
        """
        actions: List of (display_name, callback) tuples.
        """
        self._actions = actions
        self._filter("")

    def _filter(self, text: str):
        self.list_widget.clear()
        text = text.lower()
        # Simple fuzzy-ish sort: starts with > contains
        matches = []
        for name, cb in self._actions:
            n_low = name.lower()
            if text in n_low:
                score = 0 if n_low.startswith(text) else 1
                matches.append((score, name, cb))
        
        matches.sort(key=lambda x: (x[0], x[1]))
        
        for _, name, cb in matches:
            item = QListWidgetItem(name)
            item.setData(Qt.UserRole, cb)
            self.list_widget.addItem(item)
            
        if self.list_widget.count() > 0:
            self.list_widget.setCurrentRow(0)

    def _execute(self, item: QListWidgetItem):
        cb = item.data(Qt.UserRole)
        self.close()
        if cb:
            # Run later to allow dialog to close fully
            QTimer.singleShot(50, cb)

    def eventFilter(self, obj, event):
        # Route arrow keys from line edit to list widget
        if obj == self.search_box and event.type() == QKeyEvent.KeyPress:
            if event.key() in (Qt.Key_Down, Qt.Key_Up, Qt.Key_PageUp, Qt.Key_PageDown):
                self.list_widget.keyPressEvent(event)
                return True
            if event.key() == Qt.Key_Return:
                if self.list_widget.currentItem():
                    self._execute(self.list_widget.currentItem())
                return True
        return super().eventFilter(obj, event)

# -----------------------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------------------

def attach_status_bar(win) -> QStatusBar:
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
                   search_cb: Optional[Callable]=None,
                   palette_cb: Optional[Callable]=None):
    
    def _try(seq, cb):
        if cb:
            sc = QShortcut(seq, win)
            sc.activated.connect(cb)

    _try("Ctrl+S", save_cb or getattr(win, "save", None))
    _try("Ctrl+D", duplicate_cb or getattr(win, "duplicate_selected", None))
    _try("Delete", delete_cb or getattr(win, "delete_selected", None))
    _try("Ctrl+F", search_cb or getattr(win, "focus_search", None))
    _try("Ctrl+P", palette_cb) # Command Palette
    
    # Undo/Redo logic
    um = getattr(win, "undo_manager", None)
    stack = getattr(um, "stack", None)
    if stack:
        _try("Ctrl+Z", stack.undo)
        _try("Ctrl+Y", stack.redo)
        _try("Ctrl+Shift+Z", stack.redo)

def attach_list_context_menu(widget, *,
                             on_new: Optional[Callable]=None,
                             on_dup: Optional[Callable]=None,
                             on_del: Optional[Callable]=None):
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

def mark_invalid(widget: QWidget, msg: str):
    widget.setToolTip(msg)
    # Using generic error color that works with dark theme
    widget.setStyleSheet("border: 1px solid #ed4245; border-radius: 4px; background-color: #350b0b;")

def clear_invalid(widget: QWidget):
    widget.setToolTip("")
    widget.setStyleSheet("")


# -----------------------------------------------------------------------------
# Collapsible Section
# -----------------------------------------------------------------------------
class CollapsibleSection(QGroupBox):
    """A QGroupBox that can be toggled open/closed by clicking its title checkbox."""

    def __init__(self, title: str, parent=None, initially_open: bool = True):
        super().__init__(title, parent)
        self.setCheckable(True)
        self.setChecked(initially_open)
        self._content = QWidget()
        self._form = QFormLayout(self._content)
        self._form.setContentsMargins(4, 4, 4, 4)
        try:
            self._form.setFieldGrowthPolicy(QFormLayout.AllNonFixedFieldsGrow)
            self._form.setRowWrapPolicy(QFormLayout.DontWrapRows)
            self._form.setLabelAlignment(Qt.AlignRight)
            self._form.setFormAlignment(Qt.AlignTop | Qt.AlignLeft)
        except Exception:
            pass
        inner = QVBoxLayout()
        inner.setContentsMargins(2, 2, 2, 2)
        inner.addWidget(self._content)
        self.setLayout(inner)
        self.toggled.connect(self._content.setVisible)
        self._content.setVisible(initially_open)

    def add_row(self, label: str, widget):
        self._form.addRow(label, widget)