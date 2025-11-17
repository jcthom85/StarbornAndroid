#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations
from PyQt5.QtGui import QPalette, QColor, QFont
from PyQt5.QtWidgets import QApplication, QStyleFactory

class ThemeManager:
    @staticmethod
    def apply(app: QApplication, mode: str = "light", *, font_family: str = "Segoe UI", base_pt: int = 10):
        app.setStyle("Fusion")
        if mode.lower() == "dark":
            app.setPalette(ThemeManager._dark_palette())
        else:
            app.setPalette(ThemeManager._light_palette())
        # global font sizing (propagates to most widgets)
        app.setFont(QFont(font_family, base_pt))

    @staticmethod
    def _light_palette() -> QPalette:
        return QApplication.style().standardPalette()

    @staticmethod
    def _dark_palette() -> QPalette:
        pal = QPalette()
        win   = QColor(36, 39, 44)
        base  = QColor(27, 29, 33)
        text  = QColor(225, 228, 232)
        mid   = QColor(55, 59, 66)
        hi    = QColor(102, 153, 255)

        pal.setColor(QPalette.Window, win)
        pal.setColor(QPalette.WindowText, text)
        pal.setColor(QPalette.Base, base)
        pal.setColor(QPalette.AlternateBase, win)
        pal.setColor(QPalette.ToolTipBase, text)
        pal.setColor(QPalette.ToolTipText, text)
        pal.setColor(QPalette.Text, text)
        pal.setColor(QPalette.Button, mid)
        pal.setColor(QPalette.ButtonText, text)
        pal.setColor(QPalette.Highlight, hi)
        pal.setColor(QPalette.HighlightedText, QColor(255,255,255))
        return pal
