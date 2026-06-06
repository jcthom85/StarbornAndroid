#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations
from PyQt5.QtGui import QPalette, QColor, QFont
from PyQt5.QtWidgets import QApplication

class ThemeManager:
    @staticmethod
    def apply(app: QApplication, mode: str = "dark", *, font_family: str = "Segoe UI", base_pt: int = 10):
        app.setStyle("Fusion")
        
        # Set the palette for standard widgets that don't fully respect QSS or for fallback
        if mode.lower() == "dark":
            app.setPalette(ThemeManager._dark_palette())
            app.setStyleSheet(ThemeManager._dark_stylesheet(font_family, base_pt))
        else:
            app.setPalette(ThemeManager._light_palette())
            app.setStyleSheet(ThemeManager._light_stylesheet(font_family, base_pt))
            
        app.setFont(QFont(font_family, base_pt))

    @staticmethod
    def _dark_palette() -> QPalette:
        pal = QPalette()
        # Deep Gunmetal Grey Theme
        win   = QColor(30, 33, 36)      # Main background
        base  = QColor(25, 27, 30)      # Text inputs / Lists
        text  = QColor(220, 221, 222)   # Main text
        mid   = QColor(44, 47, 51)      # Borders / Separators
        hi    = QColor(88, 101, 242)    # Accent Blue
        
        pal.setColor(QPalette.Window, win)
        pal.setColor(QPalette.WindowText, text)
        pal.setColor(QPalette.Base, base)
        pal.setColor(QPalette.AlternateBase, win)
        pal.setColor(QPalette.ToolTipBase, base)
        pal.setColor(QPalette.ToolTipText, text)
        pal.setColor(QPalette.Text, text)
        pal.setColor(QPalette.Button, mid)
        pal.setColor(QPalette.ButtonText, text)
        pal.setColor(QPalette.Highlight, hi)
        pal.setColor(QPalette.HighlightedText, QColor(255, 255, 255))
        pal.setColor(QPalette.Link, hi)
        return pal

    @staticmethod
    def _light_palette() -> QPalette:
        return QApplication.style().standardPalette()

    @staticmethod
    def _dark_stylesheet(font: str, pt: int) -> str:
        # Define colors for easy interpolation
        bg_main = "#1e2124"
        bg_sec  = "#282b30"  # Panels / Cards
        bg_ter  = "#36393e"  # Borders / Inputs
        accent  = "#5865F2"  # Blurple
        text    = "#dcddde"
        red     = "#ed4245"
        green   = "#3ba55c"

        return f"""
        QWidget {{
            background-color: {bg_main};
            color: {text};
            font-family: "{font}";
            font-size: {pt}pt;
            selection-background-color: {accent};
            selection-color: white;
        }}

        /* --- Main Window & Containers --- */
        QMainWindow::separator {{
            background: {bg_ter};
            width: 2px; height: 2px;
        }}
        QGroupBox {{
            background-color: {bg_sec};
            border: 1px solid {bg_ter};
            border-radius: 6px;
            margin-top: 1.2em;
            padding-top: 10px;
        }}
        QGroupBox::title {{
            subcontrol-origin: margin;
            subcontrol-position: top left;
            padding: 0 5px;
            left: 10px;
            color: {accent};
            font-weight: bold;
        }}
        QTabWidget::pane {{
            border: 1px solid {bg_ter};
            background: {bg_main};
            border-radius: 4px;
        }}
        
        /* --- Buttons --- */
        QPushButton {{
            background-color: {bg_ter};
            border: none;
            border-radius: 4px;
            padding: 6px 12px;
            font-weight: 500;
        }}
        QPushButton:hover {{
            background-color: #42454a;
        }}
        QPushButton:pressed {{
            background-color: {accent};
            color: white;
        }}
        QPushButton:disabled {{
            background-color: #2f3136;
            color: #72767d;
        }}
        QToolButton {{
            background: transparent;
            border: none;
            border-radius: 4px;
            padding: 4px;
        }}
        QToolButton:hover {{
            background-color: {bg_ter};
        }}

        /* --- Inputs --- */
        QLineEdit, QTextEdit, QPlainTextEdit, QSpinBox, QDoubleSpinBox {{
            background-color: {bg_sec};
            border: 1px solid {bg_ter};
            border-radius: 4px;
            padding: 4px;
            selection-background-color: {accent};
        }}
        QLineEdit:focus, QTextEdit:focus, QSpinBox:focus {{
            border: 1px solid {accent};
            background-color: #202225;
        }}
        QComboBox {{
            background-color: {bg_sec};
            border: 1px solid {bg_ter};
            border-radius: 4px;
            padding: 4px;
        }}
        QComboBox::drop-down {{
            subcontrol-origin: padding;
            subcontrol-position: top right;
            width: 20px;
            border-left: 1px solid {bg_ter};
        }}

        /* --- Lists & Tables --- */
        QListWidget, QTableWidget, QTreeWidget {{
            background-color: {bg_sec};
            border: 1px solid {bg_ter};
            border-radius: 4px;
            outline: none;
        }}
        QListWidget::item, QTableWidget::item {{
            padding: 4px;
            border-bottom: 1px solid #2f3136;
        }}
        QListWidget::item:selected, QTableWidget::item:selected {{
            background-color: {accent};
            color: white;
            border-radius: 2px;
        }}
        QListWidget::item:hover:!selected {{
            background-color: {bg_ter};
        }}
        QHeaderView::section {{
            background-color: {bg_main};
            border: none;
            border-right: 1px solid {bg_ter};
            border-bottom: 1px solid {bg_ter};
            padding: 4px;
            font-weight: bold;
        }}

        /* --- Scrollbars (Minimalist) --- */
        QScrollBar:vertical {{
            border: none;
            background: {bg_main};
            width: 10px;
            margin: 0px 0px 0px 0px;
        }}
        QScrollBar::handle:vertical {{
            background: {bg_ter};
            min-height: 20px;
            border-radius: 5px;
        }}
        QScrollBar::add-line:vertical, QScrollBar::sub-line:vertical {{
            height: 0px;
        }}
        QScrollBar::handle:vertical:hover {{
            background: #4f545c;
        }}

        /* --- Tabs --- */
        QTabBar::tab {{
            background: {bg_main};
            border: 1px solid {bg_ter};
            color: #b9bbbe;
            padding: 8px 16px;
            margin-right: 2px;
            border-top-left-radius: 4px;
            border-top-right-radius: 4px;
        }}
        QTabBar::tab:selected {{
            background: {bg_sec};
            color: white;
            border-bottom: 2px solid {accent};
        }}
        QTabBar::tab:hover:!selected {{
            background: {bg_ter};
        }}

        /* --- Menu & Tooltips --- */
        QMenu {{
            background-color: {bg_sec};
            border: 1px solid {bg_ter};
        }}
        QMenu::item {{
            padding: 6px 24px;
        }}
        QMenu::item:selected {{
            background-color: {accent};
            color: white;
        }}
        QToolTip {{
            background-color: black;
            color: white;
            border: 1px solid {bg_ter};
            padding: 2px;
        }}
        """

    @staticmethod
    def _light_stylesheet(font: str, pt: int) -> str:
        # Basic cleanup for light mode too, though mostly relies on Fusion
        return f"""
        QWidget {{
            font-family: "{font}";
            font-size: {pt}pt;
        }}
        QGroupBox {{
            font-weight: bold;
        }}
        """