# StarBorn/font_manager.py
# Central definition of font names and baseline sizes used throughout the UI.

from __future__ import annotations

from typing import Dict

from kivy.metrics import sp

from ui import scaling

# ---------------------------------------------------------------------------
# Base font definitions (sizes expressed in logical sp units at scale 1.0)
# ---------------------------------------------------------------------------
_FONT_DEFINITIONS: Dict[str, Dict[str, float]] = {
    # UI Elements
    "title": {"name": "Russo One", "size": 92},
    "room_title": {"name": "Oxanium-Bold", "size": 68},
    "button": {"name": "SourceSans3", "size": 32},
    "popup_title": {"name": "Oxanium-Bold", "size": 26},
    "popup_button": {"name": "SourceSans3", "size": 18},

    # Menu Screens
    "menu_title": {"name": "Oxanium-Bold", "size": 30},
    "menu_button": {"name": "Russo One", "size": 24},
    "overlay_header": {"name": "Oxanium-Bold", "size": 36},
    "small_button": {"name": "SourceSans3", "size": 16},
    "tiny_button": {"name": "SourceSans3", "size": 14},
    "medium_text": {"name": "SourceSans3", "size": 18},
    "small_text": {"name": "SourceSans3", "size": 14},

    # Dialogue
    "dialogue_name": {"name": "Russo One", "size": 56},
    "dialogue_text": {"name": "SourceSans3", "size": 38},

    # In-game Text
    "description": {"name": "SourceSans3", "size": 38},
    "section_title": {"name": "Oxanium-Bold", "size": 22},
    "minimap": {"name": "Press Start 2P", "size": 52},
    "node_label": {"name": "Oxanium-Bold", "size": 38},

    # Character & Combat
    "combat_name": {"name": "Oxanium-Bold", "size": 28},
    "combat_hp": {"name": "SourceSans3", "size": 24},
    "damage_popup": {"name": "Oxanium-Bold", "size": 44},
    "combat_message": {"name": "Oxanium-Bold", "size": 44},
    "exploration_log": {"name": "SourceSans3", "size": 36},
}


fonts: Dict[str, Dict[str, float]] = {}
"""Runtime cache of fonts with sizes converted to device-independent sp."""


def _rebuild_fonts(_: float) -> None:
    """Refresh the exported font dictionary after a scale change."""
    fonts.clear()
    for key, data in _FONT_DEFINITIONS.items():
        fonts[key] = {
            "name": data["name"],
            "size": sp(data["size"])
        }


# Populate immediately at import time, then react to future scale updates.
_rebuild_fonts(scaling.get_scale())
scaling.bind(_rebuild_fonts)
