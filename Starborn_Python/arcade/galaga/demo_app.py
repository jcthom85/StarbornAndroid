"""Standalone launcher for the Galaga mini game."""
from __future__ import annotations

import sys
from pathlib import Path

from kivy.app import App
from kivy.core.window import Window
from kivy.uix.floatlayout import FloatLayout

if __package__ in (None, ""):
    # Allow `python arcade/galaga/demo_app.py` execution by adding project root.
    project_root = Path(__file__).resolve().parents[2]
    if str(project_root) not in sys.path:
        sys.path.insert(0, str(project_root))
    from arcade.galaga.galaga_screen import GalagaScreen
else:
    from .galaga_screen import GalagaScreen


class GalagaDemo(App):
    """Minimal Kivy app that hosts the Galaga screen for quick testing."""

    def build(self):  # type: ignore[override]
        root = FloatLayout()
        screen = GalagaScreen(name="galaga_demo")
        screen.size_hint = (1, 1)
        root.add_widget(screen)
        screen.on_pre_enter()
        return root


if __name__ == "__main__":
    # Portrait aspect ratio friendly window for desktop testing.
    Window.size = (540, 1260)
    GalagaDemo().run()
