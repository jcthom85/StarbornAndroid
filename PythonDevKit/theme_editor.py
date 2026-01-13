
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations
import os, sys, json, shutil
from pathlib import Path
from typing import Any, Dict, List, Optional, Union

from PyQt5.QtCore import Qt
from PyQt5.QtGui import QColor, QPalette
from PyQt5.QtWidgets import (
    QApplication, QWidget, QSplitter, QHBoxLayout, QVBoxLayout,
    QListWidget, QListWidgetItem, QLineEdit, QPushButton, QColorDialog,
    QFormLayout, QLabel, QMessageBox, QGroupBox
)

from data_core import json_load, json_save, unique_id
from devkit_paths import resolve_paths
from editor_undo import UndoManager
from ui_common import attach_status_bar, flash_status, attach_hotkeys, attach_list_context_menu

class ColorPicker(QWidget):
    def __init__(self, key: str, value: Union[str, List[float]]):
        super().__init__()
        self.key = key
        self.is_list_format = isinstance(value, list)

        if self.is_list_format:
            self.color_val = QColor.fromRgbF(*value)
            hex_code = self.color_val.name()
        else:
            self.color_val = QColor(value)
            hex_code = value

        layout = QHBoxLayout(self)
        self.label = QLabel(key)
        self.color_btn = QPushButton()
        self.hex_label = QLineEdit(hex_code)

        self.color_btn.setFixedSize(24, 24)
        self.hex_label.setFixedWidth(80)

        layout.addWidget(self.label)
        layout.addStretch(1)
        layout.addWidget(self.color_btn)
        layout.addWidget(self.hex_label)

        self.color_btn.clicked.connect(self.pick_color)
        self.hex_label.textChanged.connect(self.update_color_from_text)

        self.update_button_color()

    def pick_color(self):
        color = QColorDialog.getColor(self.color_val, self)
        if color.isValid():
            self.color_val = color
            self.hex_label.setText(self.color_val.name())
            self.update_button_color()

    def update_color_from_text(self, text):
        color = QColor(text)
        if color.isValid():
            self.color_val = color
            self.update_button_color()

    def update_button_color(self):
        self.color_btn.setStyleSheet(f"background-color: {self.color_val.name()}; border: 1px solid #888;")

    def get_value(self) -> Union[str, List[float]]:
        if self.is_list_format:
            return list(self.color_val.getRgbF())
        else:
            return self.hex_label.text()

class StringEditor(QWidget):
    def __init__(self, key: str, value: str):
        super().__init__()
        self.key = key
        layout = QHBoxLayout(self)
        self.label = QLabel(key)
        self.edit = QLineEdit(value)
        layout.addWidget(self.label)
        layout.addWidget(self.edit)

    def get_value(self) -> str:
        return self.edit.text()

class ThemeEditor(QWidget):
    def __init__(self, project_root: Optional[str] = None):
        super().__init__()
        self.setWindowTitle("Starborn — Themes")
        paths = resolve_paths(Path(project_root) if project_root else Path(__file__).parent)
        self.root = paths.assets_dir
        self.themes: Dict[str, Dict[str, str]] = self._read_themes()
        self.current_theme_name: Optional[str] = None
        self._loading = False

        self._build_ui()
        self._reload_list()

    def _read_themes(self) -> Dict[str, Dict[str, str]]:
        themes_path = self.root / "themes.json"
        if themes_path.exists():
            return json_load(themes_path)
        return {}

    def _write_themes(self):
        themes_path = self.root / "themes.json"
        json_save(themes_path, self.themes)

    def _build_ui(self):
        split = QSplitter(Qt.Horizontal, self)
        root_layout = QHBoxLayout(self)
        root_layout.addWidget(split)

        # Left side: List of themes
        left_widget = QWidget()
        left_layout = QVBoxLayout(left_widget)
        self.list_widget = QListWidget()
        self.list_widget.itemSelectionChanged.connect(self._on_select_theme)
        left_layout.addWidget(self.list_widget)

        btn_layout = QHBoxLayout()
        new_btn = QPushButton("New")
        new_btn.clicked.connect(self._on_new_theme)
        delete_btn = QPushButton("Delete")
        delete_btn.clicked.connect(self._on_delete_theme)
        rename_btn = QPushButton("Rename")
        rename_btn.clicked.connect(self._on_rename_theme)
        btn_layout.addWidget(new_btn)
        btn_layout.addWidget(delete_btn)
        btn_layout.addWidget(rename_btn)
        left_layout.addLayout(btn_layout)
        
        save_btn = QPushButton("Save Themes")
        save_btn.clicked.connect(self._on_save)
        left_layout.addWidget(save_btn)

        split.addWidget(left_widget)

        # Right side: Color editor
        right_widget = QWidget()
        self.right_layout = QVBoxLayout(right_widget)
        split.addWidget(right_widget)
        
        split.setStretchFactor(1, 2)

    def _reload_list(self):
        self.list_widget.clear()
        for theme_name in sorted(self.themes.keys()):
            self.list_widget.addItem(QListWidgetItem(theme_name))

    def _on_select_theme(self):
        if self._loading:
            return
        
        selected_items = self.list_widget.selectedItems()
        if not selected_items:
            self.current_theme_name = None
            self._clear_color_editor()
            return

        self.current_theme_name = selected_items[0].text()
        self._load_theme_colors()

    def _clear_color_editor(self):
        while self.right_layout.count():
            child = self.right_layout.takeAt(0)
            if child.widget():
                child.widget().deleteLater()

    def _load_theme_colors(self):
        self._loading = True
        self._clear_color_editor()

        if not self.current_theme_name:
            self._loading = False
            return

        theme_data = self.themes.get(self.current_theme_name, {})
        
        self.editor_area = QWidget()
        form_layout = QFormLayout(self.editor_area)

        for key, value in theme_data.items():
            if isinstance(value, list):
                picker = ColorPicker(key, value)
                form_layout.addRow(picker)
            else:
                editor = StringEditor(key, str(value))
                form_layout.addRow(editor)

        self.right_layout.addWidget(self.editor_area)
        
        apply_btn = QPushButton("Apply Changes")
        apply_btn.clicked.connect(self._on_apply_changes)
        self.right_layout.addWidget(apply_btn)
        
        self.right_layout.addStretch(1)
        self._loading = False

    def _on_new_theme(self):
        from PyQt5.QtWidgets import QInputDialog
        name, ok = QInputDialog.getText(self, "New Theme", "Enter theme name:")
        if ok and name:
            if name in self.themes:
                QMessageBox.warning(self, "Error", "Theme with this name already exists.")
                return
            self.themes[name] = {
                "background": [0.13, 0.13, 0.13, 1],
                "foreground": [0.93, 0.93, 0.93, 1],
                "primary": [0.0, 0.47, 1.0, 1],
                "secondary": [0.42, 0.46, 0.49, 1],
                "success": [0.16, 0.65, 0.27, 1],
                "danger": [0.86, 0.21, 0.27, 1],
                "warning": [1.0, 0.75, 0.03, 1],
                "info": [0.09, 0.63, 0.72, 1],
            }
            self._reload_list()
            for i in range(self.list_widget.count()):
                if self.list_widget.item(i).text() == name:
                    self.list_widget.setCurrentRow(i)
                    break

    def _on_delete_theme(self):
        if not self.current_theme_name:
            return
        
        reply = QMessageBox.question(self, "Delete Theme", f"Are you sure you want to delete '{self.current_theme_name}'?",
                                     QMessageBox.Yes | QMessageBox.No, QMessageBox.No)
        
        if reply == QMessageBox.Yes:
            del self.themes[self.current_theme_name]
            self.current_theme_name = None
            self._reload_list()
            self._clear_color_editor()

    def _on_rename_theme(self):
        if not self.current_theme_name:
            return

        from PyQt5.QtWidgets import QInputDialog
        new_name, ok = QInputDialog.getText(self, "Rename Theme", "Enter new name:", text=self.current_theme_name)
        if ok and new_name and new_name != self.current_theme_name:
            if new_name in self.themes:
                QMessageBox.warning(self, "Error", "Theme with this name already exists.")
                return
            
            self.themes[new_name] = self.themes.pop(self.current_theme_name)
            self.current_theme_name = new_name
            self._reload_list()
            for i in range(self.list_widget.count()):
                if self.list_widget.item(i).text() == new_name:
                    self.list_widget.setCurrentRow(i)
                    break
    
    def _on_apply_changes(self):
        if not self.current_theme_name:
            return
            
        new_values = {}
        form_layout = self.editor_area.layout()
        for i in range(form_layout.rowCount()):
            editor = form_layout.itemAt(i, QFormLayout.FieldRole).widget()
            if isinstance(editor, (ColorPicker, StringEditor)):
                new_values[editor.key] = editor.get_value()
        
        self.themes[self.current_theme_name] = new_values
        QMessageBox.information(self, "Applied", "Changes have been applied in memory. Hit 'Save Themes' to write to file.")

    def _on_save(self):
        self._write_themes()
        QMessageBox.information(self, "Saved", "themes.json has been saved.")

if __name__ == "__main__":
    app = QApplication(sys.argv)
    editor = ThemeEditor()
    editor.show()
    sys.exit(app.exec_())
