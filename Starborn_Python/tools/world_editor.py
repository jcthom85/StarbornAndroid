#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Starborn World Builder - v6.0
=============================

This version upgrades the unified World/Hub/Node/Room editor with:
• Multi-select pickers for Room Items & NPCs (with search filter).
• Connection pickers & completers (choose valid rooms quickly).
• Project-wide Validate (catch broken refs, orphans, bad ranges).
• Snap-to-grid toggle for room layout.
• Tree search filter (find worlds/hubs/nodes/rooms fast).
• Ctrl+S to Save All, visual "dirty" dot in title.
• PNG snapshot export of the current canvas (hub or node mode).
• Safer renaming with ID remapping where relevant.
• Numerous UX refinements.

Files used: worlds.json, hubs.json, nodes.json, rooms.json, room_templates.json
Optional lookups: items.json, npcs.json, enemies.json, shops.json (if present)

Usage:
    pip install PyQt5
    python world_editor.py  [optional_project_root]
"""

from __future__ import annotations
import json, os, sys, copy, re, subprocess
from typing import Dict, List, Optional, Set, Tuple

from PyQt5.QtCore import Qt, QPointF, QRect, QRectF, pyqtSignal, QEvent, QSize, QMimeData, QSizeF, QSettings
from PyQt5.QtGui import (
    QPainter, QPen, QBrush, QColor, QCursor, QPixmap, QFontMetrics, QImage,
    QDrag, QIcon, QKeySequence, QFont, QFontDatabase
)
from PyQt5.QtWidgets import (
    QApplication, QWidget, QHBoxLayout, QVBoxLayout, QTreeWidget, QTreeWidgetItem,
    QListWidget, QListWidgetItem, QLineEdit, QPushButton, QLabel, QGroupBox,
    QFormLayout, QPlainTextEdit, QScrollArea, QSpinBox, QComboBox,
    QInputDialog, QMessageBox, QDialog, QMenu, QAction, QFileDialog, QFrame, QCheckBox,
    QMainWindow, QSplitter, QStackedWidget, QDialogButtonBox, QTextBrowser, QTabWidget,
    QDoubleSpinBox, QToolTip, QTreeWidgetItemIterator, QCompleter, QShortcut
)

from context_index import ContextIndex
from ai_layout import generate_node_layout, LayoutOptions

# ---------- Visual tuning ----------
ROOM_SIZE = 56
GRID_STEP = ROOM_SIZE + 2

# --- Hub scene logical size (must match in-game) ---
HUB_SCENE_W = 1080   # 9:21 base width
HUB_SCENE_H = 2520   # 9:21 base height

# ---------- Env color palette ----------
ENV_COLORS_PRESET = {
    "forest": QColor(70, 140, 80), "cave": QColor(80, 80, 110),
    "desert": QColor(170, 130, 60), "city": QColor(80, 120, 160),
    "ice": QColor(120, 160, 200), "volcano": QColor(170, 70, 60),
    "space": QColor(110, 80, 150), "spaceship": QColor(100, 110, 120),
    "mine": QColor(130, 100, 80), "market": QColor(160, 140, 80)
}

try:
    from dotenv import load_dotenv; load_dotenv()
except Exception:
    pass

def _env_color(env: str) -> QColor:
    if not env: return QColor(70, 130, 180)
    key = (env or "").strip().lower()
    if key in ENV_COLORS_PRESET: return ENV_COLORS_PRESET[key]
    h = (abs(hash(key)) % 360)
    c = QColor(); c.setHsl(h, 180, 130); return c

# --------------------------------------------------------
# Utilities
# --------------------------------------------------------
def _safe_read_json(path: str, fallback):
    if not os.path.exists(path): return fallback
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        QMessageBox.critical(None, "Load Error", f"Failed to load {os.path.basename(path)}:\n{e}")
        return fallback

def _safe_write_json(path: str, data, title: str):
    try:
        # write pretty, no backup rotation here to keep code self-contained
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=4, ensure_ascii=False)
        return True
    except Exception as e:
        QMessageBox.critical(None, "Save Error", f"Failed to save {title}:\n{e}")
        return False

def _node_room_key(node: dict) -> str:
    for k in ("rooms", "room_ids", "room_list"):
        if k in node and isinstance(node[k], list): return k
    return "rooms"

def _detect_project_root() -> str:
    if len(sys.argv) > 1 and os.path.isdir(sys.argv[1]):
        candidate = sys.argv[1]
        if os.path.exists(os.path.join(candidate, "rooms.json")): return candidate
    cwd = os.getcwd()
    if os.path.exists(os.path.join(cwd, "rooms.json")): return cwd
    here = os.path.dirname(os.path.abspath(__file__)); parent = os.path.dirname(here)
    if os.path.exists(os.path.join(parent, "rooms.json")): return parent
    dlg = QFileDialog(); dlg.setFileMode(QFileDialog.Directory)
    dlg.setOption(QFileDialog.ShowDirsOnly, True)
    dlg.setWindowTitle("Choose Starborn project root (contains rooms.json)")
    if dlg.exec_():
        picked = dlg.selectedFiles()[0]
        if os.path.exists(os.path.join(picked, "rooms.json")): return picked
    return cwd

def _sanitize_id(text: str) -> str:
    return re.sub(r'[^a-zA-Z0-9_./-]', '', text.strip().lower().replace(' ', '_'))

def _sorted_vals(d: dict, key="id"):
    try:
        return [d[k] for k in sorted(d.keys(), key=str.lower)]
    except Exception:
        return list(d.values())

def _to_id_map(records, id_key='id', alt_keys=()):
    """
    Normalize a JSON payload into {id: record}.

    Accepts:
      - list[dict]: picks id_key (or first present alt_keys) as the key
      - dict[str, dict]: treats top-level keys as IDs; injects id into value if missing

    Returns:
      dict[str, dict]
    """
    out = {}

    if isinstance(records, dict):
        for k, v in records.items():
            if isinstance(v, dict):
                v = dict(v)  # shallow copy
                v.setdefault(id_key, k)
                out[k] = v
        return out

    if isinstance(records, list):
        for v in records:
            if not isinstance(v, dict):
                continue
            key = v.get(id_key)
            if not key and alt_keys:
                for ak in alt_keys:
                    if v.get(ak):
                        key = v[ak]
                        break
            if key:
                out[key] = v
        return out

    return out

# --------------------------------------------------------
# Structured JSON Editor Dialogs
# --------------------------------------------------------
class JsonEditorDialog(QDialog):
    """Base class for simple structured editors."""
    def __init__(self, parent=None):
        super().__init__(parent)
        self.setWindowTitle("Edit Data")
        self.layout = QVBoxLayout(self)
        self.form_layout = QFormLayout()
        self.layout.addLayout(self.form_layout)
        self.buttons = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        self.buttons.accepted.connect(self.accept)
        self.buttons.rejected.connect(self.reject)
        self.layout.addWidget(self.buttons)
        self.widgets = {}

    def add_field(self, key, label, widget):
        self.form_layout.addRow(label, widget)
        self.widgets[key] = widget

    def get_data(self) -> dict:
        data = {}
        for key, widget in self.widgets.items():
            if isinstance(widget, QLineEdit): data[key] = widget.text()
            elif isinstance(widget, QSpinBox): data[key] = widget.value()
            elif isinstance(widget, QComboBox): data[key] = widget.currentData() or widget.currentText()
            elif isinstance(widget, QCheckBox): data[key] = widget.isChecked()
        return data

    def set_data(self, data: dict):
        for key, widget in self.widgets.items():
            if key in data:
                if isinstance(widget, QLineEdit): widget.setText(str(data[key]))
                elif isinstance(widget, QSpinBox): widget.setValue(int(data.get(key, 0)))
                elif isinstance(widget, QComboBox):
                    idx = widget.findData(data[key])
                    if idx != -1: widget.setCurrentIndex(idx)
                    else: widget.setCurrentText(str(data[key]))
                elif isinstance(widget, QCheckBox): widget.setChecked(bool(data.get(key, False)))

class EditEnemyDialog(JsonEditorDialog):
    def __init__(self, all_enemies: List[str], parent=None):
        super().__init__(parent)
        self.setWindowTitle("Edit Enemy")
        id_combo = QComboBox(); id_combo.addItems(sorted(all_enemies)); id_combo.setEditable(True)
        self.all_enemies = all_enemies

        self.add_field("id", "Enemy ID:", id_combo)
        self.add_field("behavior", "Behavior:", QLineEdit())

        # --- NEW: Picker for Party field ---
        party_layout = QHBoxLayout()
        self.party_edit = QLineEdit()
        self.party_edit.setPlaceholderText("e.g., drone_a, drone_b")
        party_picker_btn = QPushButton("…")
        party_picker_btn.setFixedWidth(30)
        party_picker_btn.clicked.connect(self._pick_party)
        party_layout.addWidget(self.party_edit)
        party_layout.addWidget(party_picker_btn)
        self.form_layout.addRow("Party:", party_layout)
        # We need to manually add party_edit to self.widgets for get/set data
        self.widgets['party'] = self.party_edit
        # --- END NEW ---

    # MODIFIED: get_data now reads from the correct widget
    def get_data(self) -> dict:
        data = super().get_data()
        if data.get('id') and not data.get('behavior') and not data.get('party'):
            return data['id']
        if data.get('party'):
            data['party'] = [s.strip() for s in data['party'].split(',') if s.strip()]
        return data

    # MODIFIED: set_data now handles the party list correctly
    def set_data(self, data):
        if isinstance(data, str):
            data = {'id': data}
        super().set_data(data)
        if 'party' in data and isinstance(data['party'], list):
            self.widgets['party'].setText(", ".join(data['party']))

    # --- NEW: Party picker dialog logic ---
    def _pick_party(self):
        """Opens a multi-select dialog to choose party members."""
        current_party = [s.strip() for s in self.party_edit.text().split(',') if s.strip()]
        
        # The dialog takes a list of options and a list of currently selected ones.
        dlg = MultiSelectDialog("Select Party Members", self.all_enemies, current_party, self)
        
        if dlg.exec_():
            selected_enemies = dlg.result_list()
            self.party_edit.setText(", ".join(selected_enemies))
    # --- END NEW ---

class EditActionDialog(JsonEditorDialog):
    def __init__(self, parent=None):
        super().__init__(parent)
        self.setWindowTitle("Edit Action")
        self.add_field("name", "Name (in text):", QLineEdit())
        type_combo = QComboBox()
        type_combo.addItems(["", "tinkering", "cooking", "fishing", "toggle", "shop", "player_action", "container"])
        self.add_field("type", "Type:", type_combo)
        self.add_field("state_key", "State Key (toggle/container):", QLineEdit())
        self.add_field("items", "Items (container, comma-separated):", QLineEdit())
        self.add_field("zone_id", "Zone ID (fishing):", QLineEdit())
        self.add_field("shop_id", "Shop ID (shop):", QLineEdit())
        self.add_field("action_event", "Event Name (toggle):", QLineEdit())
        self.add_field("condition_unmet_message", "Locked Message:", QLineEdit())

    def get_data(self) -> dict:
        data = super().get_data()
        if data.get('items'):
            data['items'] = [s.strip() for s in data['items'].split(',') if s.strip()]
        return data

    def set_data(self, data: dict):
        if 'items' in data and isinstance(data['items'], list):
            data['items'] = ", ".join(data['items'])
        super().set_data(data)

class EditStateDialog(JsonEditorDialog):
    def __init__(self, parent=None):
        super().__init__(parent)
        self.setWindowTitle("Edit State Variable")
        self.add_field("key", "Key:", QLineEdit())
        self.add_field("value", "Value (JSON):", QLineEdit())

    def get_data(self):
        key = self.widgets['key'].text()
        try:
            val = json.loads(self.widgets['value'].text())
        except json.JSONDecodeError:
            val = self.widgets['value'].text()
        return key, val

    def set_data(self, key, value):
        self.widgets['key'].setText(key)
        self.widgets['value'].setText(json.dumps(value))

class EditFlavorTextDialog(JsonEditorDialog):
    """A dialog for editing a key-value pair where the key is chosen from a list."""
    def __init__(self, choices: List[str], parent=None):
        super().__init__(parent)
        self.setWindowTitle("Edit Flavor Text")
        self.key_combo = QComboBox()
        self.key_combo.addItems(sorted(choices))
        self.value_edit = QLineEdit()

        self.form_layout.addRow("Item/Enemy:", self.key_combo)
        self.form_layout.addRow("Flavor Text:", self.value_edit)

    def get_data(self):
        key = self.key_combo.currentText()
        value = self.value_edit.text()
        return key, value

    def set_data(self, key, value):
        # Find the key in the combo box and select it
        index = self.key_combo.findText(key)
        if index != -1:
            self.key_combo.setCurrentIndex(index)
        # Set the value text
        self.value_edit.setText(str(value))

class JsonListEditorWidget(QWidget):
    """A generic widget to manage a list of JSON items."""
    dataChanged = pyqtSignal()
    def __init__(self, dialog_class, dialog_args_factory, parent=None):
        super().__init__(parent)
        self.dialog_class = dialog_class
        self.dialog_args_factory = dialog_args_factory
        self.data = []

        layout = QHBoxLayout(self)
        layout.setContentsMargins(0,0,0,0)
        self.list_widget = QListWidget()
        self.list_widget.itemDoubleClicked.connect(self.edit_item)
        layout.addWidget(self.list_widget)

        btn_layout = QVBoxLayout()
        add_btn = QPushButton("Add"); add_btn.clicked.connect(self.add_item)
        edit_btn = QPushButton("Edit"); edit_btn.clicked.connect(self.edit_item)
        rem_btn = QPushButton("Remove"); rem_btn.clicked.connect(self.remove_item)
        btn_layout.addWidget(add_btn); btn_layout.addWidget(edit_btn); btn_layout.addWidget(rem_btn)
        btn_layout.addStretch()
        layout.addLayout(btn_layout)

    def _display_text_for_item(self, item_data):
        if isinstance(item_data, str): return item_data
        if isinstance(item_data, dict):
            return item_data.get('id') or item_data.get('name') or json.dumps(item_data)
        return str(item_data)

    def _refresh_list(self):
        self.list_widget.clear()
        for item in self.data:
            self.list_widget.addItem(self._display_text_for_item(item))

    def set_data(self, data_list: list):
        self.data = copy.deepcopy(data_list or [])
        self._refresh_list()

    def get_data(self) -> list:
        return self.data

    def add_item(self):
        dialog = self.dialog_class(*self.dialog_args_factory())
        if dialog.exec_():
            self.data.append(dialog.get_data())
            self._refresh_list()
            self.dataChanged.emit()

    def edit_item(self):
        selected = self.list_widget.selectedItems()
        if not selected: return
        idx = self.list_widget.row(selected[0])
        item_data = self.data[idx]
        dialog = self.dialog_class(*self.dialog_args_factory())
        dialog.set_data(item_data)
        if dialog.exec_():
            self.data[idx] = dialog.get_data()
            self._refresh_list()
            self.dataChanged.emit()

    def remove_item(self):
        selected = self.list_widget.selectedItems()
        if not selected: return
        idx = self.list_widget.row(selected[0])
        del self.data[idx]
        self._refresh_list()
        self.dataChanged.emit()

class JsonDictEditorWidget(JsonListEditorWidget):
    """Dictionary editor (key/value pairs) using EditStateDialog."""
    def _display_text_for_item(self, key, value):
        return f"{key}: {json.dumps(value)}"

    def _refresh_list(self):
        self.list_widget.clear()
        for k, v in self.data.items():
            self.list_widget.addItem(self._display_text_for_item(k,v))

    def set_data(self, data_dict: dict):
        self.data = copy.deepcopy(data_dict or {})
        self._refresh_list()

    def add_item(self):
        dialog = self.dialog_class(*self.dialog_args_factory())
        if dialog.exec_():
            key, value = dialog.get_data()
            if key:
                self.data[key] = value
                self._refresh_list()
                self.dataChanged.emit()

    def edit_item(self):
        selected = self.list_widget.selectedItems()
        if not selected: return
        key = selected[0].text().split(':')[0]
        value = self.data[key]
        dialog = self.dialog_class(*self.dialog_args_factory())
        dialog.set_data(key, value)
        if dialog.exec_():
            new_key, new_value = dialog.get_data()
            if new_key != key: del self.data[key]
            self.data[new_key] = new_value
            self._refresh_list()
            self.dataChanged.emit()

    def remove_item(self):
        selected = self.list_widget.selectedItems()
        if not selected: return
        key = selected[0].text().split(':')[0]
        if key in self.data:
            del self.data[key]
        self._refresh_list()
        self.dataChanged.emit()

class BlockedDirectionsEditorWidget(QWidget):
    dataChanged = pyqtSignal()

    def __init__(self, builder, parent=None):
        super().__init__(parent)
        self.builder = builder
        layout = QFormLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        self._rows = {}
        enemy_ids = sorted((builder.enemies_by_id or {}).keys())

        for direction in ("north", "east", "south", "west"):
            container = QWidget()
            row_layout = QHBoxLayout(container)
            row_layout.setContentsMargins(0, 0, 0, 0)
            row_layout.setSpacing(6)

            type_combo = QComboBox()
            type_combo.addItem("None", "none")
            type_combo.addItem("Enemy Block", "enemy")
            type_combo.addItem("Lock (Key)", "lock_key")
            type_combo.addItem("Lock (Event Requirements)", "lock_event")

            enemy_combo = QComboBox()
            enemy_combo.setEditable(True)
            enemy_combo.addItem("")
            for eid in enemy_ids:
                enemy_combo.addItem(eid)

            key_edit = QLineEdit()
            key_edit.setPlaceholderText("key item id (e.g. mine_key)")
            consume_check = QCheckBox("Consume key")
            consume_check.setChecked(True)

            requires_edit = QLineEdit()
            requires_edit.setPlaceholderText('[{"room_id":"...", "state_key":"...", "value": true}]')

            def _emit_change(*_): self.dataChanged.emit()
            for widget in (type_combo, enemy_combo, key_edit, consume_check, requires_edit):
                if hasattr(widget, 'textChanged'):
                    widget.textChanged.connect(_emit_change)
                elif hasattr(widget, 'currentIndexChanged'):
                    widget.currentIndexChanged.connect(_emit_change)
                elif hasattr(widget, 'stateChanged'):
                    widget.stateChanged.connect(_emit_change)

            def _make_updater(combo):
                def _update_visibility():
                    mode = combo.currentData()
                    enemy_combo.setVisible(mode == "enemy")
                    key_edit.setVisible(mode == "lock_key")
                    consume_check.setVisible(mode == "lock_key")
                    requires_edit.setVisible(mode == "lock_event")
                return _update_visibility

            updater = _make_updater(type_combo)
            type_combo.currentIndexChanged.connect(updater)
            updater()

            row_layout.addWidget(type_combo, 1)
            row_layout.addWidget(enemy_combo, 1)
            row_layout.addWidget(key_edit, 1)
            row_layout.addWidget(consume_check, 0)
            row_layout.addWidget(requires_edit, 2)

            layout.addRow(direction.capitalize(), container)
            self._rows[direction] = {
                "type": type_combo,
                "enemy": enemy_combo,
                "key": key_edit,
                "consume": consume_check,
                "requires": requires_edit,
                "update": updater,
            }

    def set_data(self, data: dict | None):
        data = data or {}
        for direction, controls in self._rows.items():
            entry = data.get(direction) or {}
            mode = "none"
            if entry.get("type") == "enemy":
                mode = "enemy"
            elif entry.get("type") == "lock":
                if entry.get("key_id") or entry.get("key"):
                    mode = "lock_key"
                elif entry.get("requires"):
                    mode = "lock_event"
            controls["type"].setCurrentIndex(controls["type"].findData(mode))
            controls["enemy"].setCurrentText(entry.get("enemy_id", ""))
            controls["key"].setText(entry.get("key_id") or entry.get("key") or "")
            controls["consume"].setChecked(bool(entry.get("consume", True)))
            requires = entry.get("requires")
            controls["requires"].setText(json.dumps(requires) if requires else "")
            controls["update"]()

    def get_data(self) -> dict:
        result = {}
        for direction, controls in self._rows.items():
            mode = controls["type"].currentData()
            if mode == "enemy":
                enemy_id = controls["enemy"].currentText().strip()
                if enemy_id:
                    result[direction] = {"type": "enemy", "enemy_id": enemy_id}
            elif mode == "lock_key":
                key_id = controls["key"].text().strip()
                if key_id:
                    entry = {"type": "lock", "key_id": key_id}
                    entry["consume"] = bool(controls["consume"].isChecked())
                    result[direction] = entry
            elif mode == "lock_event":
                raw = controls["requires"].text().strip()
                try:
                    requires = json.loads(raw) if raw else []
                    if requires:
                        result[direction] = {"type": "lock", "requires": requires}
                except json.JSONDecodeError as e:
                    QMessageBox.warning(self, "Blocked Directions", f"Invalid JSON for {direction.title()} requirements:\n{e}")
                    raise
        return result

# --------------------------------------------------------
# Picker dialogs (NEW)
# --------------------------------------------------------
class MultiSelectDialog(QDialog):
    """Searchable multi-select for Items/NPCs."""
    def __init__(self, title: str, options: List[str], selected: List[str], parent=None):
        super().__init__(parent)
        self.setWindowTitle(title)
        v = QVBoxLayout(self)
        self.search = QLineEdit(); self.search.setPlaceholderText("Search…")
        v.addWidget(self.search)
        self.list = QListWidget()
        self.list.setSelectionMode(QListWidget.MultiSelection)
        for opt in options:
            it = QListWidgetItem(opt)
            self.list.addItem(it)
            if opt in selected:
                it.setSelected(True)
        v.addWidget(self.list, 1)
        btns = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        v.addWidget(btns)
        btns.accepted.connect(self.accept); btns.rejected.connect(self.reject)
        self.search.textChanged.connect(self._filter)

    def _filter(self, txt: str):
        t = (txt or "").lower()
        for i in range(self.list.count()):
            it = self.list.item(i)
            it.setHidden(t not in it.text().lower())

    def result_list(self) -> List[str]:
        return [self.list.item(i).text()
                for i in range(self.list.count())
                if self.list.item(i).isSelected()]

# --------------------------------------------------------
# Main Visual Canvas
# --------------------------------------------------------
class VisualCanvas(QWidget):
    # Signals
    node_selected = pyqtSignal(str)
    node_geometry_changed = pyqtSignal()
    room_selected = pyqtSignal(str)
    room_dragged = pyqtSignal(str, float, float)
    room_added_at = pyqtSignal(float, float)
    room_duplicated = pyqtSignal(str)
    room_deleted = pyqtSignal(str)
    connection_requested = pyqtSignal(str, str)
    snapshot_requested = pyqtSignal(set)
    room_dropped_on = pyqtSignal(str, str)  # room_id, dropped_item_id

    MODE_HUB = 0
    MODE_NODE = 1

    def __init__(self, controller, parent=None):
        super().__init__(parent)
        self.controller = controller
        self.setMouseTracking(True)
        self.setAcceptDrops(True)
        self.setMinimumSize(640, 480)
        self.setFocusPolicy(Qt.StrongFocus)

        # Data refs
        self.hubs_by_id = {}
        self.nodes_by_id = {}
        self.rooms_by_id = {}
        self.current_hub_id: Optional[str] = None
        self.visible_room_ids: Set[str] = set()

        # Mode & Selection
        self.mode = self.MODE_HUB
        self.selected_node_id: Optional[str] = None
        self.selected_room_id: Optional[str] = None

        # Multi-select support (node mode)
        self.selected_room_ids: Set[str] = set()
        # Rubber-band selection state
        self._rubber_selecting: bool = False
        self._rubber_start: Optional[QPointF] = None
        self._rubber_end: Optional[QPointF] = None

        # View state
        self.zoom = 1.0
        self.offset = QPointF(0, 0)
        self._bg_pix: Optional[QPixmap] = None
        self._bg_size: QSize = QSize(1, 1)
        self._node_pix_cache: Dict[str, QPixmap] = {}

        # Interaction
        self._panning = False; self._drag_start = QPointF(0, 0)
        # -- Hub Mode --
        self._dragging_node = False; self._resizing_node = False
        self._resize_handle: Optional[str] = None
        self._drag_start_scene = QPointF(0,0)
        self._drag_start_node_center = QPointF(0,0); self._drag_start_node_size = QSizeF(0,0)
        self._hover_node_id: Optional[str] = None
        # -- Node Mode --
        self._dragging_room_id: Optional[str] = None; self._last_mouse = QPointF(0, 0)
        self.in_connection_mode = False; self.connection_start_id: Optional[str] = None
        self.live_connection_end_pos: Optional[QPointF] = None
        self.in_add_mode = False
        self._hover_room_id: Optional[str] = None
        self.hover_card = RoomHoverCard(self)
        self.quick_mode = False

        # Fonts (match in-game Hub node label styling if available)
        self._node_label_qfont: Optional[QFont] = None

    def _ensure_node_label_font(self) -> QFont:
        """Load and cache the QFont that matches the game's node label font.

        Uses font_manager.fonts["node_label"] if available, attempting to load the
        corresponding TTF from the project's fonts/ directory. Falls back gracefully.
        """
        if self._node_label_qfont is not None:
            return self._node_label_qfont

        # Defaults
        fm_name = "Oxanium-Bold"
        fm_size_px = 38
        try:
            # Pull desired font name/size from the game font map
            from font_manager import fonts as game_fonts  # type: ignore
            node_style = game_fonts.get("node_label", {}) if isinstance(game_fonts, dict) else {}
            fm_name = str(node_style.get("name", fm_name))
            # Kivy's sp() returns pixel-like units on desktop; convert to int
            fm_size_px = int(float(node_style.get("size", fm_size_px)))
        except Exception:
            pass

        # Try to load a matching TTF from fonts/ folder (e.g., Oxanium-Bold.ttf)
        family_loaded: Optional[str] = None
        try:
            # The project root is available via controller.root
            fonts_dir = os.path.join(self.controller.root, "fonts")
            candidate_ttf = os.path.join(fonts_dir, f"{fm_name}.ttf")
            # Some names may not include style; try a couple of variants
            alt_candidates = [
                candidate_ttf,
                os.path.join(fonts_dir, f"{fm_name}.otf"),
                os.path.join(fonts_dir, f"{fm_name.replace('-', ' ')}.ttf"),
            ]
            for path in alt_candidates:
                if os.path.exists(path):
                    fid = QFontDatabase.addApplicationFont(path)
                    if fid != -1:
                        fams = QFontDatabase.applicationFontFamilies(fid)
                        if fams:
                            family_loaded = fams[0]
                            break
        except Exception:
            pass

        qf = QFont(family_loaded or fm_name.replace('-', ' '))
        # Bold if indicated by style name
        if "Bold" in fm_name:
            qf.setBold(True)
        qf.setPixelSize(max(8, int(fm_size_px)))
        self._node_label_qfont = qf
        return qf

    def _fit_keep_aspect(self, outer: QRectF, inner_w: float, inner_h: float) -> QRectF:
        """Return a dest rect that fits inner_wÃ—inner_h into outer without distortion."""
        if inner_w <= 0 or inner_h <= 0 or outer.width() <= 0 or outer.height() <= 0:
            return QRectF(outer)
        ar_in = inner_w / inner_h
        ar_out = outer.width() / outer.height()
        if ar_in > ar_out:
            # fit by width
            w = outer.width()
            h = w / ar_in
            x = outer.left()
            y = outer.top() + (outer.height() - h) / 2.0
        else:
            # fit by height
            h = outer.height()
            w = h * ar_in
            x = outer.left() + (outer.width() - w) / 2.0
            y = outer.top()
        return QRectF(x, y, w, h)

    def _compute_hub_bounds(self) -> QRectF:
        """
        Compute a reasonable scene rect when there's no hub background.
        Uses node positions in the current hub; falls back to a default canvas.
        """
        have_any = False
        minx = miny = float("inf")
        maxx = maxy = float("-inf")

        for nid, node in self.nodes_by_id.items():
            if node.get("hub_id") != self.current_hub_id:
                continue
            rect = self._get_node_rect_scene(node)
            minx = min(minx, rect.left())
            miny = min(miny, rect.top())
            maxx = max(maxx, rect.right())
            maxy = max(maxy, rect.bottom())
            have_any = True

        if not have_any:
            # default "workspace" if hub has no nodes yet
            return QRectF(0, 0, 1920, 1080)

        pad = 200.0
        return QRectF(minx - pad, miny - pad, (maxx - minx) + pad * 2, (maxy - miny) + pad * 2)


    def _paint_checkerboard(self, p: QPainter, rect: QRectF, tile: int = 64):
        """
        Theme-aware checkerboard for hub mode when no background image.
        """
        pal = self.palette()
        bg = pal.window().color()
        is_dark = bg.value() < 80

        # Two subtle tones depending on theme
        c1 = QColor(40, 40, 48) if is_dark else QColor(236, 238, 242)
        c2 = QColor(50, 50, 58) if is_dark else QColor(246, 248, 252)

        # Align tiles to a grid so panning/zooming feels stable
        import math
        x0 = int(math.floor(rect.left() / tile) * tile)
        y0 = int(math.floor(rect.top() / tile) * tile)
        x1 = int(math.ceil(rect.right() / tile) * tile)
        y1 = int(math.ceil(rect.bottom() / tile) * tile)

        y = y0
        row = 0
        while y < y1:
            x = x0
            col = 0
            while x < x1:
                p.fillRect(QRectF(x, y, tile, tile), c1 if ((row + col) % 2 == 0) else c2)
                x += tile
                col += 1
            y += tile
            row += 1

    # API
    def set_data(self, hubs, nodes, rooms):
        self.hubs_by_id = hubs; self.nodes_by_id = nodes; self.rooms_by_id = rooms; self.update()

    def set_mode(self, mode: int, hub_id: Optional[str] = None, visible_room_ids: Optional[Set[str]] = None):
        self.mode = mode
        self.current_hub_id = hub_id
        self.visible_room_ids = visible_room_ids or set()
        self._bg_pix = None
        # Default to the game's 9:21 logical stage size; prefer the actual background dimensions when available.
        self._bg_size = QSize(HUB_SCENE_W, HUB_SCENE_H)

        if self.mode == self.MODE_HUB and self.current_hub_id:
            hub = self.hubs_by_id.get(self.current_hub_id)
            if hub and hub.get('background_image'):
                path = os.path.join(self.controller.root, hub['background_image'])
                if os.path.exists(path):
                    pix = QPixmap(path)
                    if not pix.isNull():
                        self._bg_pix = pix
                        self._bg_size = pix.size()

        self.fit_to_view()
        self.update()

    def set_connection_mode(self, enabled: bool):
        self.in_connection_mode = enabled; self.connection_start_id = None
        self.setCursor(Qt.CrossCursor if enabled else Qt.ArrowCursor); self.update()

    def set_add_mode(self, enabled: bool):
        self.in_add_mode = enabled
        self.setCursor(Qt.PointingHandCursor if enabled else Qt.ArrowCursor); self.update()

    def set_quick_mode(self, enabled: bool):
        """Enable/disable quick-create mode (RMB=create, LMB-drag=connect)."""
        self.quick_mode = bool(enabled)
        if enabled:
            # ensure other modal tools are off
            self.in_connection_mode = False
            self.in_add_mode = False
        self.setCursor(Qt.ArrowCursor)
        self.update()

    def center_on_room(self, room_id: str, animate: bool = False):
        """Center the view on the given room ID, keeping the current zoom."""
        r = self.rooms_by_id.get(room_id)
        if not r:
            return
        pos = r.get("pos", [0, 0])
        self.offset = QPointF(
            -float(pos[0]) * GRID_STEP * self.zoom,
            float(pos[1]) * GRID_STEP * self.zoom
        )
        self.update()

    # Coordinates & View
    def view_to_scene(self, p: QPointF) -> QPointF:
        if self.zoom == 0: return QPointF(0,0)
        return QPointF((p.x() - self.offset.x()) / self.zoom, (p.y() - self.offset.y()) / self.zoom)

    def screen_to_world_node(self, px: float, py: float) -> Tuple[float, float]:
        if self.zoom == 0: return 0, 0
        wx = (px - self.width() * 0.5 - self.offset.x()) / (GRID_STEP * self.zoom)
        wy = -(py - self.height() * 0.5 - self.offset.y()) / (GRID_STEP * self.zoom)
        return (wx, wy)

    def world_to_screen_node(self, wx: float, wy: float) -> QPointF:
        """
        Convert world coords (node/room grid) -> view (widget) coords.
        Mirrors screen_to_world_node().
        """
        return QPointF(
            (self.width() * 0.5) + self.offset.x() + (wx * GRID_STEP * self.zoom),
            (self.height() * 0.5) + self.offset.y() - (wy * GRID_STEP * self.zoom)
        )
    
    def fit_to_view(self):
        if self.mode == self.MODE_HUB:
            view_w = max(1, self.width() - 10)
            view_h = max(1, self.height() - 10)
            scene_w = float(self._bg_size.width() or HUB_SCENE_W)
            scene_h = float(self._bg_size.height() or HUB_SCENE_H)
            sx = view_w / scene_w
            sy = view_h / scene_h
            self.zoom = max(0.1, min(8.0, min(sx, sy)))
            self.offset = QPointF((view_w - scene_w * self.zoom) / 2.0,
                                (view_h - scene_h * self.zoom) / 2.0)
        elif self.mode == self.MODE_NODE:
            pts = []
            for rid in self.visible_room_ids:
                r = self.rooms_by_id.get(rid)
                if r: pts.append(tuple(float(v) for v in r.get("pos", [0, 0])))
            if not pts: return
            xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
            minx, maxx = min(xs), max(xs); miny, maxy = min(ys), max(ys)
            w_world = max(1.0, (maxx - minx) + 2.5); h_world = max(1.0, (maxy - miny) + 2.5)
            view_w = max(1, self.width()); view_h = max(1, self.height())
            z_x = (view_w * 0.8) / (w_world * 100.0); z_y = (view_h * 0.8) / (h_world * 100.0)
            self.zoom = max(0.1, min(5.0, min(z_x, z_y)))
            cx = (minx + maxx) * 0.5; cy = (miny + maxy) * 0.5
            sx = cx * 100.0 * self.zoom
            sy = -cy * 100.0 * self.zoom
            # Center the average of the visible rooms at the view center.
            # IMPORTANT: do NOT add width/height halves here â€” _get_room_center_view()
            # already adds them when converting world->view coordinates.
            self.offset = QPointF(-sx, -sy)

        self.update()

    # Hit testing / geometry
    def _hit_test_node(self, p_scene: QPointF) -> Optional[str]:
        for nid, node in reversed(list(self.nodes_by_id.items())):
            if node.get("hub_id") != self.current_hub_id: continue
            if self._get_node_rect_scene(node).contains(p_scene): return nid
        return None

    def _hit_test_room(self, pt: QPointF) -> Optional[str]:
        size = ROOM_SIZE * self.zoom
        for rid in self.visible_room_ids:
            center = self._get_room_center_view(rid)
            if not center: continue
            rect = QRectF(center.x() - size/2, center.y() - size/2, size, size)
            if rect.contains(pt): return rid
        return None
    
    def _view_rect_for_room(self, rid: str) -> QRectF:
        size = ROOM_SIZE * self.zoom
        center = self._get_room_center_view(rid)
        if not center:
            return QRectF()
        return QRectF(center.x() - size/2, center.y() - size/2, size, size)

    def _update_rubber_selection(self):
        if not (self._rubber_start and self._rubber_end):
            return
        rect = QRectF(self._rubber_start, self._rubber_end).normalized()
        base = set(self.selected_room_ids) if (QApplication.keyboardModifiers() & Qt.ControlModifier) else set()
        found = {rid for rid in self.visible_room_ids if rect.intersects(self._view_rect_for_room(rid))}
        self.selected_room_ids = base | found

    def _get_node_rect_scene(self, node: dict) -> QRectF:
        ph = node.get("pos_hint", {}); w = self._bg_size.width(); h = self._bg_size.height()
        cx = float(ph.get("center_x", 0.5)) * w
        cy = (1.0 - float(ph.get("center_y", 0.5))) * h
        sz_w, sz_h = node.get("size", [256, 256])
        return QRectF(cx - sz_w/2, cy - sz_h/2, sz_w, sz_h)
    
    def _node_image_key(self, node: dict) -> Optional[str]:
        """Best-effort key for node art; supports several field names."""
        return node.get("icon_image") or node.get("image") or node.get("icon")

    def _load_node_pixmap(self, node: dict) -> Optional[QPixmap]:
        """Load & cache the QPixmap for a nodeâ€™s image path, if any."""
        key = self._node_image_key(node)
        if not key:
            return None
        # Cache by absolute path for stability
        path = os.path.join(self.controller.root, key)
        pm = self._node_pix_cache.get(path)
        if pm is None:
            pm = QPixmap(path) if os.path.exists(path) else QPixmap()
            self._node_pix_cache[path] = pm
        return None if pm.isNull() else pm

    def _get_room_center_view(self, room_id: str) -> Optional[QPointF]:
        r = self.rooms_by_id.get(room_id)
        if not r: return None
        pos = r.get("pos", [0, 0])
        cx = (pos[0] * 100) * self.zoom + self.offset.x() + self.width() * 0.5
        cy = (-pos[1] * 100) * self.zoom + self.offset.y() + self.height() * 0.5
        return QPointF(cx, cy)

    # Painting
    def paintEvent(self, event):
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)
        p.setRenderHint(QPainter.SmoothPixmapTransform, True)

        # Theme-aware background: match the app/window palette.
        pal = self.palette()
        bg_color = pal.window().color()
        p.fillRect(self.rect(), bg_color)

        if self.mode == self.MODE_HUB:
            self._paint_hub_mode(p)
        elif self.mode == self.MODE_NODE:
            self._paint_node_mode(p)

    def _paint_hub_mode(self, p: QPainter):
        p.save()
        p.translate(self.offset)
        p.scale(self.zoom, self.zoom)

        # Always draw to the logical scene rect (stretched if needed)
        scene_rect = QRectF(0.0, 0.0, float(self._bg_size.width()), float(self._bg_size.height()))
        if self._bg_pix:
            src = QRectF(0, 0, self._bg_pix.width(), self._bg_pix.height())
            p.drawPixmap(scene_rect, self._bg_pix, src)
        else:
            # No bg image? Fill with a neutral background so you can still place nodes.
            pal = self.palette()
            p.fillRect(scene_rect, pal.window().color())

        # Draw nodes (use real images if available; fallback to rounded rects)
        for nid, node in self.nodes_by_id.items():
            if node.get("hub_id") != self.current_hub_id:
                continue

            rect = self._get_node_rect_scene(node)
            selected = (nid == self.selected_node_id)

            # Try to use node icon image (if you added _load_node_pixmap previously)
            pix = self._load_node_pixmap(node) if hasattr(self, "_load_node_pixmap") else None
            if pix is not None:
                src = QRectF(0, 0, pix.width(), pix.height())
                dest = self._fit_keep_aspect(rect, pix.width(), pix.height())  # preserves aspect
                p.drawPixmap(dest, pix, src)
            else:
                color = QColor(100, 180, 250, 180) if selected else QColor(90, 140, 210, 150)
                p.setBrush(QBrush(color))
                p.setPen(QPen(QColor(20, 20, 30), 2))
                p.drawRoundedRect(rect, 10, 10)

            # Title text (centered, wrap if needed, allow per-node vertical offsets)
            title = node.get('title') or node.get('name') or nid
            qf = self._ensure_node_label_font()
            p.setFont(qf)
            fm = QFontMetrics(qf)

            # Replicate Kivy label's fixed height at the bottom of the node's rect, plus the gap
            label_h = fm.height() + 2
            text_margin = 6.0
            gap = float(node.get("title_gap", 0))
            text_rect = QRectF(rect.left(), rect.bottom() - label_h + gap, rect.width(), label_h)

            p.setPen(Qt.white)
            p.drawText(text_rect, Qt.AlignVCenter | Qt.AlignHCenter, fm.elidedText(title, Qt.ElideRight, int(max(0.0, rect.width() - text_margin * 2))))

            # Selection border + handles
            if selected:
                p.setBrush(Qt.NoBrush)
                p.setPen(QPen(QColor(100, 180, 255, 220), 2))
                p.drawRoundedRect(rect, 10, 10)
                s = 6
                p.setPen(Qt.NoPen); p.setBrush(QBrush(Qt.black))
                p.drawRect(QRectF(rect.left()-s,  rect.top()-s,    s*2, s*2))
                p.drawRect(QRectF(rect.right()-s, rect.top()-s,    s*2, s*2))
                p.drawRect(QRectF(rect.left()-s,  rect.bottom()-s, s*2, s*2))
                p.drawRect(QRectF(rect.right()-s, rect.bottom()-s, s*2, s*2))

        p.restore()

    def _paint_node_mode(self, p: QPainter):
        # Theme-aware grid
        pal = self.palette()
        bg = pal.window().color()
        is_dark = bg.value() < 80  # similar heuristic as Studio

        grid_color = QColor(50, 50, 55) if is_dark else QColor(230, 230, 230)
        p.setPen(QPen(grid_color))

        step = GRID_STEP * self.zoom
        x = self.offset.x() % step; y = self.offset.y() % step
        for gx in range(int(-step), self.width() + int(step), int(step)): p.drawLine(int(gx + x), 0, int(gx + x), self.height())
        for gy in range(int(-step), self.height() + int(step), int(step)): p.drawLine(0, int(gy + y), self.width(), int(gy + y))
        # Connections
        conn_pen = QPen(QColor(100, 100, 140, 150), 2, Qt.DashLine); p.setPen(conn_pen)
        drawn = set()
        for rid in self.visible_room_ids:
            start_center = self._get_room_center_view(rid)
            if not start_center: continue
            room = self.rooms_by_id.get(rid, {})
            for dest_id in room.get("connections", {}).values():
                if dest_id not in self.visible_room_ids: continue
                pair = tuple(sorted((rid, dest_id)))
                if pair in drawn: continue
                end_center = self._get_room_center_view(dest_id)
                if end_center: p.drawLine(start_center, end_center); drawn.add(pair)
        # Rooms
        size = ROOM_SIZE * self.zoom
        for rid in self.visible_room_ids:
            room = self.rooms_by_id.get(rid)
            if not room: continue
            center = self._get_room_center_view(rid)
            if not center: continue
            rect = QRectF(center.x() - size/2, center.y() - size/2, size, size)
            base_color = _env_color(room.get("env", ""))
            p.setBrush(QBrush(base_color)); p.setPen(QPen(QColor(15, 15, 15), 2))
            p.drawRoundedRect(rect, 6, 6)
            is_selected = (rid == self.selected_room_id) or (rid in self.selected_room_ids)
            is_connect_start = (self.in_connection_mode and rid == self.connection_start_id)
            if is_connect_start:
                p.setBrush(Qt.NoBrush); p.setPen(QPen(QColor("#98c379"), 3))
                p.drawRoundedRect(rect.adjusted(1,1,-1,-1), 6, 6)
            elif is_selected:
                p.setBrush(Qt.NoBrush); p.setPen(QPen(QColor("#61afef"), 3))
                p.drawRoundedRect(rect.adjusted(1,1,-1,-1), 6, 6)
            p.setPen(Qt.black)
            title = (room.get("title") or rid or "").strip() or rid
            p.drawText(rect, Qt.AlignCenter, p.fontMetrics().elidedText(title, Qt.ElideRight, int(size - 8)))
        if self.in_connection_mode and self.connection_start_id and self.live_connection_end_pos:
            start = self._get_room_center_view(self.connection_start_id)
            if start:
                p.setPen(QPen(QColor(120, 240, 140, 200), 3))
                p.drawLine(start, self.live_connection_end_pos)

        # Rubber-band rectangle (view coords) â€” always draw after rooms/line
        if self._rubber_selecting and self._rubber_start and self._rubber_end:
            r = QRectF(self._rubber_start, self._rubber_end).normalized()
            p.setPen(QPen(QColor(255, 255, 255, 180), 1, Qt.DashLine))
            p.setBrush(QBrush(QColor(100, 160, 255, 40)))
            p.drawRect(r)

    def keyPressEvent(self, ev):
        if self.mode != self.MODE_NODE:
            return super().keyPressEvent(ev)

        # Delete / Backspace -> delete selected rooms
        if ev.key() in (Qt.Key_Delete, Qt.Key_Backspace):
            to_delete = (self.selected_room_ids.copy()
                         if self.selected_room_ids else ({self.selected_room_id} if self.selected_room_id else set()))
            if to_delete:
                self.controller.on_delete_rooms_bulk(list(to_delete))
                self.selected_room_ids.clear()
                self.selected_room_id = None
                self.update()
                ev.accept()
                return

        # Ctrl+D -> duplicate selected rooms
        if ev.key() == Qt.Key_D and (ev.modifiers() & Qt.ControlModifier):
            to_dup = (self.selected_room_ids.copy()
                      if self.selected_room_ids else ({self.selected_room_id} if self.selected_room_id else set()))
            if to_dup:
                for rid in list(to_dup):
                    self.room_duplicated.emit(rid)
                ev.accept()
                return

        super().keyPressEvent(ev)

    # Mouse & wheel
    def mousePressEvent(self, ev):
        self.hover_card.hide()
        # Middle-mouse or Alt+LMB = Pan
        if ev.button() == Qt.MiddleButton or (ev.button() == Qt.LeftButton and (QApplication.keyboardModifiers() & Qt.AltModifier)):
            self._panning = True; self._drag_start = ev.pos(); self.setCursor(Qt.ClosedHandCursor); return
        if self.mode == self.MODE_HUB: self._mousePressHub(ev)
        elif self.mode == self.MODE_NODE: self._mousePressNode(ev)

    def _mousePressHub(self, ev):
        # Allow only left/right; middle is handled earlier for panning
        if ev.button() not in (Qt.LeftButton, Qt.RightButton):
            return

        p_scene = self.view_to_scene(ev.pos())
        nid = self._hit_test_node(p_scene)

        if nid:
            node = self.nodes_by_id[nid]
            rect = self._get_node_rect_scene(node)
            handle = self._resize_handle_hit(rect, p_scene, pad=8 / self.zoom)

            if ev.button() == Qt.LeftButton:
                # LEFT CLICK: select (and allow resize if a handle was hit)
                self.selected_node_id = nid
                if hasattr(self.controller, 'on_canvas_node_clicked'):
                    self.controller.on_canvas_node_clicked(nid)
                # Do NOT emit node_selected here; that's for double-click now.
                if handle:
                    # resize only on left
                    self._resizing_node = True
                    self._resize_handle = handle
                    self._dragging_node = False # Ensure not dragging when resizing
                    self._drag_start_scene = p_scene
                    self._drag_start_node_center = rect.center()
                    self._drag_start_node_size = rect.size()
                else:
                    # If no handle, it's a drag
                    self._resizing_node = False
                    self._dragging_node = True

                self._drag_start_scene = p_scene
                self._drag_start_node_center = rect.center()
                self._drag_start_node_size = rect.size()
            else:
                # RIGHT CLICK: begin a drag WITHOUT emitting node_selected,
                # so we stay in Hub mode and can reposition the node.
                self.selected_node_id = nid           # keep highlight local
                self._resizing_node = False
                self._dragging_node = True
                self._drag_start_scene = p_scene
                self._drag_start_node_center = rect.center()
                self._drag_start_node_size = rect.size()
        else:
            # Clicked empty space
            if ev.button() == Qt.LeftButton:
                self.selected_node_id = None
                self.node_selected.emit("")

        self.update()

    def _mousePressNode(self, ev):
        # Quick-create mode:
        #   LMB on room: start connect-drag
        #   LMB on empty: pan
        #   RMB on empty: create room here (auto-id)
        #   RMB on room: right-drag to reposition (existing behavior)
        if self.quick_mode:
            if ev.button() == Qt.LeftButton:
                rid = self._hit_test_room(ev.pos())
                if rid:
                    self.connection_start_id = rid
                    self.live_connection_end_pos = ev.pos()
                else:
                    self._panning = True
                    self._drag_start = ev.pos()
                self.update()
                return
            if ev.button() == Qt.RightButton:
                rid = self._hit_test_room(ev.pos())
                if rid:
                    self._dragging_room_id = rid
                    self._last_mouse = ev.pos()
                else:
                    wx, wy = self.screen_to_world_node(ev.pos().x(), ev.pos().y())
                    self.controller.quick_add_room(wx, wy)
                return

        # Normal connection mode
        if self.in_connection_mode and ev.button() == Qt.LeftButton:
            rid = self._hit_test_room(ev.pos())
            if not rid:
                return
            if not self.connection_start_id:
                self.connection_start_id = rid
                self.live_connection_end_pos = ev.pos()
            else:
                if rid != self.connection_start_id:
                    self.connection_requested.emit(self.connection_start_id, rid)
                self.connection_start_id = None
                self.live_connection_end_pos = None
            self.update()
            return

        # Normal add mode
        if self.in_add_mode and ev.button() == Qt.LeftButton:
            rid = self._hit_test_room(ev.pos())
            if rid:
                self.selected_room_id = rid
                self.room_selected.emit(rid)
                self.update()
            else:
                wx, wy = self.screen_to_world_node(ev.pos().x(), ev.pos().y())
                self.room_added_at.emit(wx, wy)
            return

        # Select / pan / rubber-band
        if ev.button() == Qt.LeftButton:
            rid = self._hit_test_room(ev.pos())
            ctrl = bool(ev.modifiers() & Qt.ControlModifier)

            if rid:
                # Ctrl-click toggles; plain click selects single
                if ctrl:
                    if rid in self.selected_room_ids:
                        self.selected_room_ids.remove(rid)
                    else:
                        self.selected_room_ids.add(rid)
                    # keep last-focused for the inspector
                    self.selected_room_id = rid if rid in self.selected_room_ids else (self.selected_room_id if self.selected_room_ids else None)
                else:
                    self.selected_room_ids = {rid}
                    self.selected_room_id = rid
                # notify inspector (use last-focused)
                if self.selected_room_id:
                    self.room_selected.emit(self.selected_room_id)
            else:
                # Start rubber-band selection on empty space
                self._rubber_selecting = True
                self._rubber_start = ev.pos()
                self._rubber_end = ev.pos()
                if not ctrl:
                    # plain-drag clears old selection
                    self.selected_room_ids.clear()
                    self.selected_room_id = None

            self.update()
            return

        if ev.button() == Qt.RightButton:
            rid = self._hit_test_room(ev.pos())
            if rid:
                self._dragging_room_id = rid
                self._last_mouse = ev.pos()

    def mouseMoveEvent(self, ev):
        if self._panning:
            delta = ev.pos() - self._drag_start
            self.offset += delta; self._drag_start = ev.pos(); self.update(); return

        if self.mode == self.MODE_HUB: self._mouseMoveHub(ev)
        elif self.mode == self.MODE_NODE: self._mouseMoveNode(ev)

    def _mouseMoveHub(self, ev):
        p_now = self.view_to_scene(ev.pos())
        if self._dragging_node and self.selected_node_id:
            node = self.nodes_by_id[self.selected_node_id]
            delta = p_now - self._drag_start_scene
            new_center = self._drag_start_node_center + delta
            w, h = self._bg_size.width(), self._bg_size.height()
            node["pos_hint"] = {
                "center_x": round(max(0, min(w, new_center.x())) / w, 6),
                "center_y": round(1.0 - (max(0, min(h, new_center.y())) / h), 6)
            }
            self.node_geometry_changed.emit(); self.controller._set_dirty(True); self.update()
        elif self._resizing_node and self.selected_node_id:
            node = self.nodes_by_id[self.selected_node_id]

            aspect_ratio = 0.0
            pix = self._load_node_pixmap(node)
            if pix and not pix.isNull():
                aspect_ratio = pix.width() / float(pix.height() or 1)

            # Get the fixed corner opposite to the handle being dragged
            if self._resize_handle == "br": fixed_corner = self._drag_start_node_center - QPointF(self._drag_start_node_size.width()/2, self._drag_start_node_size.height()/2)
            elif self._resize_handle == "bl": fixed_corner = self._drag_start_node_center + QPointF(self._drag_start_node_size.width()/2, -self._drag_start_node_size.height()/2)
            elif self._resize_handle == "tr": fixed_corner = self._drag_start_node_center + QPointF(-self._drag_start_node_size.width()/2, self._drag_start_node_size.height()/2)
            else: fixed_corner = self._drag_start_node_center + QPointF(self._drag_start_node_size.width()/2, self._drag_start_node_size.height()/2)

            # New width and height from mouse pos relative to fixed corner
            new_w = abs(p_now.x() - fixed_corner.x())
            new_h = abs(p_now.y() - fixed_corner.y())

            if aspect_ratio > 0:
                # Adjust one dimension to match aspect ratio, driven by the larger mouse delta
                if new_w / aspect_ratio > new_h:
                    new_h = new_w / aspect_ratio
                else:
                    new_w = new_h * aspect_ratio

            # Construct the new rectangle from the fixed corner and new size
            if self._resize_handle in ("br", "tr"): new_x = fixed_corner.x()
            else: new_x = fixed_corner.x() - new_w
            if self._resize_handle in ("br", "bl"): new_y = fixed_corner.y()
            else: new_y = fixed_corner.y() - new_h

            new_rect = QRectF(new_x, new_y, new_w, new_h)

            node["size"] = [max(16, int(new_rect.width())), max(16, int(new_rect.height()))]
            new_center = new_rect.center()
            w, h = self._bg_size.width(), self._bg_size.height()
            node["pos_hint"] = {
                "center_x": round(max(0, min(w, new_center.x())) / w, 6),
                "center_y": round(1.0 - (max(0, min(h, new_center.y())) / h), 6)
            }
            self.node_geometry_changed.emit(); self.controller._set_dirty(True); self.update()
        else:
            nid = self._hit_test_node(p_now)
            if nid and nid != self._hover_node_id:
                self._hover_node_id = nid
                info = self.controller.get_node_hover_info(nid)
                QToolTip.showText(ev.globalPos(), info, self)
            elif not nid:
                self._hover_node_id = None
                QToolTip.hideText()

    def _mouseMoveNode(self, ev):
        # live connection line in quick mode or normal connection mode
        if self.quick_mode and self.connection_start_id:
            self.live_connection_end_pos = ev.pos()
            self.update()
            return
        if self.in_connection_mode and self.connection_start_id:
            self.live_connection_end_pos = ev.pos()
            self.update()
            return
        # Rubber-band in progress (update rectangle only)
        if self._rubber_selecting:
            self._rubber_end = ev.pos()
            self.update()
            return

        # right-dragging a room to move it (single or multi)
        if self._dragging_room_id:
            dx = ev.pos().x() - self._last_mouse.x()
            dy = ev.pos().y() - self._last_mouse.y()
            if self.zoom > 0:
                wx = dx / (GRID_STEP * self.zoom)
                wy = -dy / (GRID_STEP * self.zoom)

                # If the dragged room is part of a multi-selection, move the whole set.
                moving_ids = (self.selected_room_ids.copy()
                              if self._dragging_room_id in self.selected_room_ids and len(self.selected_room_ids) > 1
                              else {self._dragging_room_id})

                for rid in moving_ids:
                    room = self.rooms_by_id.get(rid)
                    if not room:
                        continue
                    pos = room.get("pos", [0, 0])
                    pos[0] = (pos[0] if isinstance(pos[0], (int, float)) else 0) + wx
                    pos[1] = (pos[1] if isinstance(pos[1], (int, float)) else 0) + wy
                    room["pos"] = [pos[0], pos[1]]
                    # Tell the controller live (so the inspector stays in sync)
                    self.room_dragged.emit(rid, pos[0], pos[1])

                self._last_mouse = ev.pos()
                self.update()
                return

        # hover card
        rid = self._hit_test_room(ev.pos())
        if rid and rid in self.visible_room_ids:
            if rid != self._hover_room_id:
                self._hover_room_id = rid
                self.hover_card.set_text(self._summary_for_room(rid))
            self.hover_card.show_near(ev.globalPos())
        else:
            self._hover_room_id = None
            self.hover_card.hide()

    def mouseReleaseEvent(self, ev):
        if self._panning:
            self._panning = False
            self.setCursor(Qt.ArrowCursor)

        if self.mode == self.MODE_HUB:
            self._dragging_node = False
            self._resizing_node = False
        elif self.mode == self.MODE_NODE:
            # finish quick-mode LMB connect
            if self.quick_mode and ev.button() == Qt.LeftButton and self.connection_start_id:
                rid_end = self._hit_test_room(ev.pos())
                if rid_end and rid_end != self.connection_start_id:
                    self.controller.quick_connect_rooms(self.connection_start_id, rid_end)
                self.connection_start_id = None
                self.live_connection_end_pos = None
                self.update()
            # finish rubber-band selection
            if self._rubber_selecting and ev.button() == Qt.LeftButton:
                box = QRectF(self._rubber_start, self._rubber_end).normalized()
                picked: Set[str] = set()
                # choose rooms whose centers fall inside the box (fast & robust)
                for rid in self.visible_room_ids:
                    room = self.rooms_by_id.get(rid)
                    if not room: 
                        continue
                    pos = room.get("pos", [0, 0])
                    c = self.world_to_screen_node(pos[0], pos[1])
                    if box.contains(c):
                        picked.add(rid)

                ctrl = bool(ev.modifiers() & Qt.ControlModifier)
                if ctrl:
                    # toggling behavior: symmetric difference
                    for rid in picked:
                        if rid in self.selected_room_ids:
                            self.selected_room_ids.remove(rid)
                        else:
                            self.selected_room_ids.add(rid)
                else:
                    # replace selection
                    self.selected_room_ids = picked

                # update "last-focused" for inspector
                if picked:
                    # choose the last hovered or any; simplest: the last added
                    self.selected_room_id = next(reversed(list(picked)))
                    self.room_selected.emit(self.selected_room_id)
                elif not ctrl:
                    self.selected_room_id = None
                    self.room_selected.emit("")

                # clear rubber state
                self._rubber_selecting = False
                self._rubber_start = None
                self._rubber_end = None
                self.update()

            # finish right-drag move (snap if enabled)
            if ev.button() == Qt.RightButton and self._dragging_room_id:
                # Decide whether we moved a group or a single room
                moved_ids = (self.selected_room_ids.copy()
                             if self._dragging_room_id in self.selected_room_ids and len(self.selected_room_ids) > 1
                             else {self._dragging_room_id})

                if self.controller.snap_to_grid:
                    for rid in moved_ids:
                        room = self.rooms_by_id.get(rid)
                        if not room:
                            continue
                        pos = room.get("pos", [0, 0])
                        pos[0] = round(pos[0]); pos[1] = round(pos[1])
                        room["pos"] = [pos[0], pos[1]]
                        self.room_dragged.emit(rid, pos[0], pos[1])

                self._dragging_room_id = None
                self.update()

    def mouseDoubleClickEvent(self, ev):
        if self.mode == self.MODE_HUB and ev.button() == Qt.LeftButton:
            p_scene = self.view_to_scene(ev.pos())
            nid = self._hit_test_node(p_scene)
            if nid:
                self.node_selected.emit(nid)
                ev.accept()
                return
        super().mouseDoubleClickEvent(ev)

    def wheelEvent(self, ev):
        angle = ev.angleDelta().y()
        if angle == 0:
            return

        # Zoom factor (smooth; 120 "ticks" â‰ˆ 18% change)
        factor = 1.0 + (0.0015 * angle)
        new_zoom = max(0.1, min(self.zoom * factor, 8.0))

        mouse = ev.pos()

        if self.mode == self.MODE_HUB:
            # HUB MODE: simple scene transform -> already uses view_to_scene()
            scene_before = self.view_to_scene(mouse)
            self.zoom = new_zoom
            self.offset = QPointF(
                mouse.x() - scene_before.x() * self.zoom,
                mouse.y() - scene_before.y() * self.zoom,
            )
        else:
            # NODE MODE: world transform is different (origin at view center).
            # Keep the world point under the cursor fixed while zooming.
            wx, wy = self.screen_to_world_node(mouse.x(), mouse.y())  # world coords before zoom
            self.zoom = new_zoom
            self.offset = QPointF(
                mouse.x() - (self.width() * 0.5) - (wx * GRID_STEP * self.zoom),
                mouse.y() - (self.height() * 0.5) + (wy * GRID_STEP * self.zoom)
            )

        self.update()

    # Drag & drop
    def dragEnterEvent(self, event: QEvent):
        if self.mode == self.MODE_NODE and event.mimeData().hasText():
            event.acceptProposedAction()

    def dragMoveEvent(self, event: QEvent):
        if self.mode == self.MODE_NODE and event.mimeData().hasText():
            event.acceptProposedAction()

    def dropEvent(self, event: QEvent):
        if self.mode == self.MODE_NODE:
            room_id = self._hit_test_room(event.pos())
            if room_id and event.mimeData().hasText():
                data = event.mimeData().text()
                self.room_dropped_on.emit(room_id, data)
                event.acceptProposedAction()

    # Context menu
    def contextMenuEvent(self, event):
        # Never show a context menu in Node mode while Quick Create is enabled.
        # RMB is reserved for "create here" / right-drag in quick mode.
        if self.mode == self.MODE_NODE and getattr(self, "quick_mode", False):
            return

        self.hover_card.hide()
        menu = QMenu(self)

        if self.mode == self.MODE_HUB:
            p_scene = self.view_to_scene(event.pos())
            nid = self._hit_test_node(p_scene)
            if nid:
                act = menu.addAction("Edit Node Details")
                act.triggered.connect(lambda *_, n=nid: self.controller._select_in_tree("node", n))

                act = menu.addAction("Zoom to Node")
                act.triggered.connect(lambda *_, r=self._get_node_rect_scene(self.nodes_by_id[nid]): self._zoom_to_rect(r))

                menu.addSeparator()
                act = menu.addAction("Delete Node")
                act.triggered.connect(lambda *_,: self.controller._delete_selected())
            else:
                act = menu.addAction("Add Node Here")
                act.triggered.connect(lambda *_, p=p_scene: self.controller._add_node(pos_hint=p))

        elif self.mode == self.MODE_NODE:
            rid = self._hit_test_room(event.pos())

            if rid:
                act = menu.addAction("Mark as Starting Room")
                act.triggered.connect(lambda *_, r=rid: self.controller.set_node_entry_room(r))

                act = menu.addAction("Duplicate")
                act.triggered.connect(lambda *_, r=rid: self.room_duplicated.emit(r))

                act = menu.addAction("Delete")
                act.triggered.connect(lambda *_, r=rid: self.room_deleted.emit(r))

                menu.addSeparator()
                act = menu.addAction("Save as Template.")
                act.triggered.connect(lambda *_, r=rid: self.controller._save_room_as_template(r))
            else:
                wx, wy = self.screen_to_world_node(event.pos().x(), event.pos().y())

                act = menu.addAction("Add Room Here.")
                act.triggered.connect(lambda *_, x=wx, y=wy: self.room_added_at.emit(x, y))

                if getattr(self.controller, "room_templates", {}):
                    tpl_menu = menu.addMenu("Create from Template")
                    for name, _tpl in self.controller.room_templates.items():
                        a = tpl_menu.addAction(name)
                        # Accept any extra args from Qt (checked, QAction*, etc.)
                        a.triggered.connect(lambda *_, n=name, x=wx, y=wy: self.controller._create_room_from_template(n, x, y))
            # If there is a multi-selection, offer bulk actions
            if self.selected_room_ids:
                menu.addSeparator()
                a = menu.addAction(f"Duplicate Selected ({len(self.selected_room_ids)})")
                a.triggered.connect(lambda *_, ids=list(self.selected_room_ids):
                                    [self.room_duplicated.emit(r) for r in ids])
                a = menu.addAction(f"Delete Selected ({len(self.selected_room_ids)})")
                a.triggered.connect(lambda *_, ids=list(self.selected_room_ids):
                                                    self.controller.on_delete_rooms_bulk(ids))

        menu.addSeparator()
        a = menu.addAction("Fit to View")
        a.triggered.connect(lambda *_,: self.fit_to_view())

        a = menu.addAction("📸 Export PNG")
        a.triggered.connect(lambda *_, ids=(self.visible_room_ids if self.mode == self.MODE_NODE else set()): self.snapshot_requested.emit(ids))

        menu.exec_(self.mapToGlobal(event.pos()))

    def _resize_handle_hit(self, rect: QRectF, p_scene: QPointF, pad: float) -> Optional[str]:
        corners = {"tl": rect.topLeft(), "tr": rect.topRight(), "bl": rect.bottomLeft(), "br": rect.bottomRight()}
        for k, cp in corners.items():
            if QRectF(cp.x()-pad, cp.y()-pad, pad*2, pad*2).contains(p_scene): return k
        return None

    def _zoom_to_rect(self, rect_scene: QRectF):
        view_w = max(1, self.width() - 20); view_h = max(1, self.height() - 20)
        sx = view_w / rect_scene.width(); sy = view_h / rect_scene.height()
        self.zoom = min(sx, sy)
        center_scene = rect_scene.center()
        self.offset = QPointF(
            self.width()/2 - center_scene.x() * self.zoom,
            self.height()/2 - center_scene.y() * self.zoom,
        )
        self.update()

    def _summary_for_room(self, rid: str) -> str:
        r = self.rooms_by_id.get(rid, {})
        title = r.get("title") or rid; env = r.get("env") or ""
        desc = r.get("description", "")
        desc = " ".join(str(desc).split())
        if len(desc) > 140: desc = desc[:137] + "…"
        items = r.get("items", []); shown_items = ", ".join([str(x) for x in items[:4]])
        if len(items) > 4: shown_items += f" (+{len(items)-4} more)"
        enemies = r.get("enemies", []); enemy_strs = []
        if isinstance(enemies, list):
            for e in enemies[:4]:
                if isinstance(e, dict): enemy_strs.append(f"{(e.get('name') or e.get('id'))}x{e.get('count',1)}")
                else: enemy_strs.append(str(e))
            if len(enemies) > 4: enemy_strs.append(f"+{len(enemies)-4} more")
        shown_enemies = ", ".join(enemy_strs)
        npcs = r.get("npcs", []); shown_npcs = ", ".join([str(x) for x in npcs[:4]])
        if len(npcs) > 4: shown_npcs += f" (+{len(npcs)-4})"
        lines = [f"<b>{title}</b>" + (f" â€” <i>{env}</i>" if env else "")]
        if desc: lines.append(desc)
        if shown_items: lines.append(f"<b>Items:</b> {shown_items}")
        if shown_enemies: lines.append(f"<b>Enemies:</b> {shown_enemies}")
        if shown_npcs: lines.append(f"<b>NPCs:</b> {shown_npcs}")
        return "<br>".join(lines)

# --------------------------------------------------------
# Hover helpers
# --------------------------------------------------------
class ImageHoverPreview(QFrame):
    def __init__(self, parent=None):
        super().__init__(parent, Qt.Tool | Qt.FramelessWindowHint | Qt.WindowStaysOnTopHint)
        self.setAttribute(Qt.WA_TransparentForMouseEvents, True)
        self.setStyleSheet("QFrame { background: rgba(0,0,0,200); border: 1px solid #333; }")
        self.label = QLabel(self); self.label.setAlignment(Qt.AlignCenter)
        lay = QVBoxLayout(self); lay.setContentsMargins(6,6,6,6); lay.addWidget(self.label); self.hide()
    def set_pixmap(self, pm: Optional[QPixmap]):
        if not pm or pm.isNull(): self.hide(); return
        scaled = pm.scaled(540, 320, Qt.KeepAspectRatio, Qt.SmoothTransformation)
        self.label.setPixmap(scaled); self.resize(scaled.width() + 12, scaled.height() + 12)
    def show_near(self, global_pos: QPointF):
        self.move(int(global_pos.x() + 16), int(global_pos.y() + 16)); self.show()

class RoomHoverCard(QFrame):
    def __init__(self, parent=None):
        super().__init__(parent, Qt.Tool | Qt.FramelessWindowHint | Qt.WindowStaysOnTopHint)
        self.setAttribute(Qt.WA_TransparentForMouseEvents, True)
        self.setStyleSheet("QFrame { background: rgba(12,12,12,230); border: 1px solid #333; border-radius: 6px; } QLabel { color: #e9e9e9; padding: 8px; }")
        self.label = QLabel(self); self.label.setAlignment(Qt.AlignLeft | Qt.AlignTop); self.label.setWordWrap(True)
        lay = QVBoxLayout(self); lay.setContentsMargins(0,0,0,0); lay.addWidget(self.label); self._max_w = 360; self.hide()
    def set_text(self, text: str):
        self.label.setText(text); self.label.setFixedWidth(self._max_w); self.label.adjustSize()
        self.resize(self.label.sizeHint().width(), self.label.sizeHint().height())
    def show_near(self, global_pos: QPointF):
        self.move(int(global_pos.x() + 18), int(global_pos.y() + 18)); self.show()

# --------------------------------------------------------
# Main World Builder Application
# --------------------------------------------------------
class WorldBuilder(QMainWindow):
    def __init__(self, root_dir: str):
        super().__init__()
        # Normalize to plain strings to avoid Qt path-type issues
        self.project_root = str(root_dir)
        self.root = str(root_dir)
        self.setWindowTitle("Starborn World Builder")
        self.resize(1600, 900)

        # ---- Data ----
        self.worlds_by_id = {}; self.hubs_by_id = {}; self.nodes_by_id = {}; self.rooms_by_id = {}
        self.items_by_id = {}; self.npcs_by_id = {}; self.enemies_by_id = {}; self.shops_by_id = {}

        # Templates
        self.templates_by_id = {}
        self._load_templates()

        self.current_selection = (None, None)
        self._pending_node_mode = False
        self.snap_to_grid = True
        self._dirty = False

        self._load_all_data()

        # UI prefs (persist last-used image directory for file pickers)
        try:
            self._settings = QSettings("Starborn", "WorldBuilder")
            last_dir = self._settings.value("last_image_dir", self.root)
            self._last_image_dir = str(last_dir) if last_dir else self.root
        except Exception:
            self._settings = None
            self._last_image_dir = self.root

        # Optional external data caches (for richer hover info)
        self._quests_cache = None  # type: Optional[Dict[str, dict]]
        self._events_cache = None  # type: Optional[List[dict]]

        # ---- Main UI ----
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        root_layout = QHBoxLayout(central_widget)
        splitter = QSplitter(Qt.Horizontal)
        try:
            splitter.setHandleWidth(8)  # easier to grab + resize
        except Exception:
            pass
        root_layout.addWidget(splitter)

        # -- Left: Hierarchy Tree + search --
        left_panel = QWidget(); left_layout = QVBoxLayout(left_panel)
        try:
            left_panel.setMinimumWidth(160)
        except Exception:
            pass
        self.tree_search = QLineEdit(); self.tree_search.setPlaceholderText("Filter tree…  (id/title)")
        self.tree_search.textChanged.connect(self._rebuild_tree)
        left_layout.addWidget(self.tree_search)
        self.tree = QTreeWidget(); self.tree.setHeaderLabels(["World Atlas"])
        self.tree.itemSelectionChanged.connect(self._on_tree_selection_changed)
        self.tree.itemDoubleClicked.connect(self._on_tree_item_double_clicked)
        left_layout.addWidget(self.tree, 1)
        # Tree Buttons
        tree_btn_layout = QHBoxLayout()
        add_world_btn = QPushButton("ï¼‹ World"); add_world_btn.clicked.connect(self._add_world)
        add_hub_btn = QPushButton("ï¼‹ Hub"); add_hub_btn.clicked.connect(self._add_hub)
        add_node_btn = QPushButton("ï¼‹ Node"); add_node_btn.clicked.connect(lambda: self._add_node())
        tree_btn_layout.addWidget(add_world_btn); tree_btn_layout.addWidget(add_hub_btn); tree_btn_layout.addWidget(add_node_btn)
        left_layout.addLayout(tree_btn_layout)
        del_btn = QPushButton("🗑 Delete Selected"); del_btn.clicked.connect(self._delete_selected)
        left_layout.addWidget(del_btn)
        splitter.addWidget(left_panel)

        # -- Center: Visual Canvas + Controls --
        center_panel = QWidget(); center_layout = QVBoxLayout(center_panel)
        try:
            center_panel.setMinimumWidth(220)
        except Exception:
            pass
        # Top Controls
        top_ctrl_layout = QHBoxLayout()
        self.canvas_mode_label = QLabel("Mode: Hub"); top_ctrl_layout.addWidget(self.canvas_mode_label)
        self.connect_btn = QPushButton("🔗 Connect Rooms"); self.connect_btn.setCheckable(True); self.connect_btn.toggled.connect(self._toggle_connect_mode)
        self.add_room_btn = QPushButton("➕ Add Room Mode"); self.add_room_btn.setCheckable(True); self.add_room_btn.toggled.connect(self._toggle_add_mode)
        self.quick_btn = QPushButton("⚡ Quick Create"); self.quick_btn.setCheckable(True); self.quick_btn.toggled.connect(self._toggle_quick_mode)
        self.snap_box = QCheckBox("Snap to Grid"); self.snap_box.setChecked(self.snap_to_grid); self.snap_box.toggled.connect(lambda v: setattr(self, "snap_to_grid", bool(v)))
        self.fit_btn = QPushButton("⤢ Fit to View"); self.fit_btn.clicked.connect(lambda: self.canvas.fit_to_view())
        self.repair_btn = QPushButton("🛠 Repair Links…"); self.repair_btn.clicked.connect(self._repair_links_dialog)
        self.orphans_btn = QPushButton("🧩 Orphan Rooms…"); self.orphans_btn.clicked.connect(self._show_orphans_for_current_node)
        self.templates_btn = QPushButton("📦 Templates…"); self.templates_btn.clicked.connect(self._open_templates_dialog); top_ctrl_layout.addWidget(self.templates_btn)
        self.procgen_btn = QPushButton("✨ Procedural Gen…"); self.procgen_btn.clicked.connect(self._open_procgen_dialog); top_ctrl_layout.addWidget(self.procgen_btn)
        self.validate_btn = QPushButton("✓ Validate"); self.validate_btn.clicked.connect(self._validate_all)
        top_ctrl_layout.addStretch()
        top_ctrl_layout.addWidget(self.connect_btn); top_ctrl_layout.addWidget(self.add_room_btn); top_ctrl_layout.addWidget(self.quick_btn); top_ctrl_layout.addWidget(self.snap_box)
        top_ctrl_layout.addWidget(self.repair_btn); top_ctrl_layout.addWidget(self.orphans_btn); top_ctrl_layout.addWidget(self.validate_btn); top_ctrl_layout.addWidget(self.fit_btn)
        center_layout.addLayout(top_ctrl_layout)
        # Canvas
        self.canvas = VisualCanvas(self, self)
        self.canvas.set_data(self.hubs_by_id, self.nodes_by_id, self.rooms_by_id)
        # Connect canvas signals
        self.canvas.node_selected.connect(self._on_canvas_node_double_clicked)
        self.canvas.node_geometry_changed.connect(self._on_node_geom_changed)
        self.canvas.room_selected.connect(lambda rid: self._select_in_tree("room", rid) if rid else None)
        self.canvas.room_dragged.connect(self._on_room_dragged)
        self.canvas.room_added_at.connect(self.on_add_room_at)
        self.canvas.room_duplicated.connect(self.on_duplicate_room)
        self.canvas.room_deleted.connect(self.on_delete_room)
        self.canvas.connection_requested.connect(self._create_connection_dialog)
        self.canvas.snapshot_requested.connect(self.export_snapshot)
        self.canvas.room_dropped_on.connect(self._on_item_dropped_on_room)
        center_layout.addWidget(self.canvas)
        # Bottom Controls
        bottom_ctrl_layout = QHBoxLayout()
        self.play_btn = QPushButton("▶ Play from Here"); self.play_btn.clicked.connect(self._play_from_here)
        save_all_btn = QPushButton("💾 Save All"); save_all_btn.clicked.connect(self._save_all_data)
        bottom_ctrl_layout.addWidget(self.play_btn); bottom_ctrl_layout.addStretch(); bottom_ctrl_layout.addWidget(save_all_btn)
        center_layout.addLayout(bottom_ctrl_layout)
        splitter.addWidget(center_panel)

        # -- Right: Palettes & Inspector --
        right_panel = QSplitter(Qt.Vertical)
        # Palettes
        palette_box = QGroupBox("Palettes")
        palette_layout = QVBoxLayout(palette_box)
        palette_tabs = QTabWidget()
        self.item_palette = DraggableListWidget("item"); self.npc_palette = DraggableListWidget("npc"); self.enemy_palette = DraggableListWidget("enemy")
        palette_tabs.addTab(self.item_palette, "Items"); palette_tabs.addTab(self.npc_palette, "NPCs"); palette_tabs.addTab(self.enemy_palette, "Enemies")
        self._populate_palettes()
        palette_layout.addWidget(palette_tabs)
        right_panel.addWidget(palette_box)
        # Inspector
        self.inspector_stack = QStackedWidget()
        self._init_inspectors()  # move this line up
        self.inspector_scroll = QScrollArea()
        self.inspector_scroll.setWidgetResizable(True)
        self.inspector_scroll.setFrameShape(QFrame.NoFrame)
        self.inspector_scroll.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff)
        self.inspector_scroll.setWidget(self.inspector_stack)
        right_panel.addWidget(self.inspector_scroll)
        splitter.addWidget(right_panel)

        # Keep references for view helpers
        self._main_splitter = splitter
        self._right_splitter = right_panel

        # Give the right panel a bit more breathing room by default
        # Wider default inspector and more headroom for resizing
        splitter.setSizes([480, 920, 440])
        right_panel.setSizes([220, 680])
        # Prefer center canvas growth, keep inspector reasonably sized
        splitter.setStretchFactor(0, 0)
        splitter.setStretchFactor(1, 1)
        splitter.setStretchFactor(2, 1)
        # Allow collapsing neighbors so the inspector can grow as wide as needed
        try:
            splitter.setCollapsible(0, True)
            splitter.setCollapsible(1, True)
            splitter.setCollapsible(2, True)
        except Exception:
            pass
        right_panel.setStretchFactor(0, 0)   # palettes
        right_panel.setStretchFactor(1, 1)   # inspector
        try:
            right_panel.setMinimumWidth(380)
        except Exception:
            pass

        self._rebuild_tree()

        # Shortcuts & title dirty mark
        QShortcut(QKeySequence("Ctrl+S"), self, activated=self._save_all_data)
        # View convenience shortcuts
        QShortcut(QKeySequence("Alt+]"), self, activated=lambda: self._nudge_inspector(+80))
        QShortcut(QKeySequence("Alt+["), self, activated=lambda: self._nudge_inspector(-80))
        QShortcut(QKeySequence("Alt+P"), self, activated=self._toggle_palettes)
        self._update_title()

    def _open_procgen_dialog(self):
        from PyQt5.QtWidgets import QMessageBox
        try:
            from procedural_gen import open_procgen_dialog
        except Exception as e:
            QMessageBox.critical(self, "Procedural Gen", f"Failed to load procedural_gen.py:\n{e}")
            return
        try:
            open_procgen_dialog(self)
        except Exception as e:
            QMessageBox.critical(self, "Procedural Gen", f"Generation failed:\n{e}")

    # ---- Dirty flag ----
    def _set_dirty(self, flag: bool):
        self._dirty = bool(flag)
        self._update_title()

    def _update_title(self):
        dot = " â€¢" if self._dirty else ""
        self.setWindowTitle(f"Starborn World Builder{dot}")

    # --- View helpers: adjust right panel width / hide palettes ---
    def _nudge_inspector(self, delta: int):
        """Grow/shrink the right inspector by delta pixels (taken from center)."""
        try:
            sizes = self._main_splitter.sizes()
            if len(sizes) != 3:
                return
            left, center, right = sizes
            new_right = max(300, right + int(delta))
            borrow = new_right - right
            new_center = max(200, center - borrow)
            if new_center == center and borrow > 0:
                return
            self._main_splitter.setSizes([left, new_center, new_right])
        except Exception:
            pass

    def _toggle_palettes(self):
        """Show/hide the top Palettes box to free vertical space for the inspector."""
        try:
            w0 = self._right_splitter.widget(0)
            w1 = self._right_splitter.widget(1)
            if not w0 or not w1:
                return
            if w0.isVisible():
                w0.hide()
                self._right_splitter.setSizes([0, max(1, sum(self._right_splitter.sizes()))])
            else:
                w0.show()
                total = max(1, sum(self._right_splitter.sizes()))
                self._right_splitter.setSizes([min(260, int(total*0.25)), max(1, int(total*0.75))])
        except Exception:
            pass
    # --- Inspector UI Initialization ---
    def _init_inspectors(self):
        # Index 0: Welcome/Empty
        self.inspector_stack.addWidget(QLabel("Select an item in the World Atlas to edit its properties."))

        # Index 1: World
        self.w_id = QLineEdit(); self.w_title = QLineEdit(); self.w_desc = QLineEdit()
        self.inspector_stack.addWidget(self._create_inspector_panel("World", {"ID": self.w_id, "Title": self.w_title, "Description": self.w_desc}))

        # Index 2: Hub
        self.h_id = QLineEdit(); self.h_title = QLineEdit(); self.h_world = QComboBox(); self.h_bg = QLineEdit()
        self.h_discovered = QCheckBox("Discovered by default")
        self.h_bg_browse = QPushButton("..."); self.h_bg_browse.clicked.connect(lambda: self._browse_for_image(self.h_bg))
        bg_layout = QHBoxLayout(); bg_layout.addWidget(self.h_bg); bg_layout.addWidget(self.h_bg_browse)
        self.inspector_stack.addWidget(self._create_inspector_panel("Hub", {"ID": self.h_id, "Title": self.h_title, "World": self.h_world, "Background": bg_layout, "Discovery": self.h_discovered}))

        # Index 3: Node
        self.n_id = QLineEdit(); self.n_title = QLineEdit(); self.n_entry = QComboBox(); self.n_icon = QLineEdit()
        self.n_discovered = QCheckBox("Discovered by default")
        self.n_icon_browse = QPushButton("..."); self.n_icon_browse.clicked.connect(lambda: self._browse_for_image(self.n_icon))
        icon_layout = QHBoxLayout(); icon_layout.addWidget(self.n_icon); icon_layout.addWidget(self.n_icon_browse)
        self.n_cx = QDoubleSpinBox(); self.n_cx.setRange(0.0, 1.0); self.n_cx.setDecimals(4); self.n_cx.setSingleStep(0.01)
        self.n_cy = QDoubleSpinBox(); self.n_cy.setRange(0.0, 1.0); self.n_cy.setDecimals(4); self.n_cy.setSingleStep(0.01)
        self.n_w = QSpinBox(); self.n_w.setRange(8, 4096); self.n_h = QSpinBox(); self.n_h.setRange(8, 4096)
        self.n_title_gap = QSpinBox(); self.n_title_gap.setRange(-200, 400); self.n_title_gap.setSingleStep(1)
        self.n_title_gap.valueChanged.connect(self._on_node_title_gap_changed)
        pos_layout = QHBoxLayout(); pos_layout.addWidget(QLabel("CX:")); pos_layout.addWidget(self.n_cx); pos_layout.addWidget(QLabel("CY:")); pos_layout.addWidget(self.n_cy)
        size_layout = QHBoxLayout(); size_layout.addWidget(QLabel("W:")); size_layout.addWidget(self.n_w); size_layout.addWidget(QLabel("H:")); size_layout.addWidget(self.n_h)
        self.inspector_stack.addWidget(self._create_inspector_panel("Node", {"ID": self.n_id, "Title": self.n_title, "Entry Room": self.n_entry, "Icon": icon_layout, "Position": pos_layout, "Size": size_layout, "Title Gap (px)": self.n_title_gap, "Discovery": self.n_discovered}))

        # Index 4: Room
        self.r_id = QLabel(); self.r_title = QLineEdit(); self.r_env = QLineEdit(); self.r_bg = QLineEdit()
        self.r_bg_browse = QPushButton("..."); self.r_bg_browse.clicked.connect(lambda: self._browse_for_image(self.r_bg))
        self.r_bg.installEventFilter(self); self.bg_preview = ImageHoverPreview(self)
        # --- NEW: Weather dropdown for rooms ---
        self.r_weather = QComboBox()
        self.r_weather.addItems(["", "none", "cave_drip", "dust", "rain", "snow", "starfall", "storm"])
        # --- END NEW ---
        r_bg_layout = QHBoxLayout(); r_bg_layout.addWidget(self.r_bg); r_bg_layout.addWidget(self.r_bg_browse)

        self.r_pos_x = QSpinBox(); self.r_pos_x.setRange(-2000, 2000)
        self.r_pos_y = QSpinBox(); self.r_pos_y.setRange(-2000, 2000)
        r_pos_layout = QHBoxLayout(); r_pos_layout.addWidget(QLabel("X:")); r_pos_layout.addWidget(self.r_pos_x); r_pos_layout.addWidget(QLabel("Y:")); r_pos_layout.addWidget(self.r_pos_y)

        # Items/NPCs with pickers
        self.r_items = QLineEdit(); self.r_npcs = QLineEdit()
        self.btn_items = QPushButton("…"); self.btn_items.clicked.connect(self._pick_items)
        self.btn_npcs = QPushButton("…"); self.btn_npcs.clicked.connect(self._pick_npcs)
        items_row = QHBoxLayout(); items_row.addWidget(self.r_items, 1); items_row.addWidget(self.btn_items, 0)
        npcs_row = QHBoxLayout(); npcs_row.addWidget(self.r_npcs, 1); npcs_row.addWidget(self.btn_npcs, 0)

        # Structured editors
        self.r_enemies_editor = JsonListEditorWidget(EditEnemyDialog, lambda: [list(self.enemies_by_id.keys())])
        self.r_actions_editor = JsonListEditorWidget(EditActionDialog, lambda: [])
        self.r_state_editor = JsonDictEditorWidget(EditStateDialog, lambda: [])
        
        # NEW: Flavor text editors (now using a proper key-value editor)
        # The lambda function dynamically gets the current items/enemies in the room
        # to populate the dropdown in the new dialog.
        item_choices_factory = lambda: [[s.strip() for s in self.r_items.text().split(',') if s.strip()]]
        enemy_choices_factory = lambda: [[(e['id'] if isinstance(e, dict) else e) for e in self.r_enemies_editor.get_data()]]

        self.r_item_flavor_editor = JsonDictEditorWidget(EditFlavorTextDialog, item_choices_factory)
        self.r_enemy_flavor_editor = JsonDictEditorWidget(EditFlavorTextDialog, enemy_choices_factory)
        self.r_blocked_editor = BlockedDirectionsEditorWidget(self)


        # Description Tabs
        self.desc_tabs = QTabWidget()
        self.desc_preview = QTextBrowser()
        self.desc_edit = QPlainTextEdit()
        self.desc_tabs.addTab(self.desc_edit, "Edit")
        self.desc_tabs.addTab(self.desc_preview, "Preview")

        # Connections (with pick buttons + completers)
        self.r_conn_n = QLineEdit(); self.r_conn_s = QLineEdit(); self.r_conn_e = QLineEdit(); self.r_conn_w = QLineEdit()
        self.r_conn_n_btn = QPushButton("…"); self.r_conn_s_btn = QPushButton("…"); self.r_conn_e_btn = QPushButton("…"); self.r_conn_w_btn = QPushButton("…")
        self.r_conn_n_btn.clicked.connect(lambda: self._pick_connection(self.r_conn_n))
        self.r_conn_s_btn.clicked.connect(lambda: self._pick_connection(self.r_conn_s))
        self.r_conn_e_btn.clicked.connect(lambda: self._pick_connection(self.r_conn_e))
        self.r_conn_w_btn.clicked.connect(lambda: self._pick_connection(self.r_conn_w))
        conn_layout = QFormLayout()
        rowN = QHBoxLayout(); rowN.addWidget(self.r_conn_n, 1); rowN.addWidget(self.r_conn_n_btn, 0)
        rowS = QHBoxLayout(); rowS.addWidget(self.r_conn_s, 1); rowS.addWidget(self.r_conn_s_btn, 0)
        rowE = QHBoxLayout(); rowE.addWidget(self.r_conn_e, 1); rowE.addWidget(self.r_conn_e_btn, 0)
        rowW = QHBoxLayout(); rowW.addWidget(self.r_conn_w, 1); rowW.addWidget(self.r_conn_w_btn, 0)
        conn_layout.addRow("North", rowN); conn_layout.addRow("South", rowS); conn_layout.addRow("East", rowE); conn_layout.addRow("West", rowW)
        conn_box = QGroupBox("Connections"); conn_box.setLayout(conn_layout)

        self.r_is_entry = QCheckBox("Make this the node start")

        # Final Room Inspector Assembly
        room_inspector_widgets = {
            "ID": self.r_id, "Title": self.r_title, "Environment": self.r_env,
            "Weather": self.r_weather, "Background": r_bg_layout, "Position": r_pos_layout, "Description": self.desc_tabs,
            "Items": items_row, "NPCs": npcs_row,
            "Connections": conn_box, "Starting Room": self.r_is_entry,
            "Enemies": self.r_enemies_editor, "Actions": self.r_actions_editor, "State": self.r_state_editor,
            "Blocked Directions": self.r_blocked_editor,
            "Item Flavor": self.r_item_flavor_editor,
            "Enemy Flavor": self.r_enemy_flavor_editor
        }
        self.inspector_stack.addWidget(self._create_inspector_panel("Room", room_inspector_widgets, has_apply=False))

    def _create_inspector_panel(self, title: str, widgets: Dict, has_apply=True) -> QWidget:
        panel = QWidget(); layout = QVBoxLayout(panel)
        box = QGroupBox(title); form = QFormLayout(box)
        try:
            form.setFieldGrowthPolicy(QFormLayout.AllNonFixedFieldsGrow)
            form.setRowWrapPolicy(QFormLayout.DontWrapRows)
            form.setLabelAlignment(Qt.AlignRight)
            form.setFormAlignment(Qt.AlignTop | Qt.AlignLeft)
        except Exception:
            pass
        for label, widget in widgets.items():
            if isinstance(widget, QWidget) or isinstance(widget, QHBoxLayout): form.addRow(label, widget)
            else: form.addRow(label, widget)
        layout.addWidget(box)
        if has_apply:
            apply_btn = QPushButton("Apply Changes"); apply_btn.clicked.connect(self._apply_inspector_changes)
            layout.addWidget(apply_btn)
        layout.addStretch()
        return panel

    # ===================== TEMPLATES: load/save/resolve =====================

    def _templates_path(self) -> str:
        base = getattr(self, "project_root", None) or getattr(self, "root", "")
        return os.path.join(base, "templates.json")

    def _load_templates(self):
        """Load template library from templates.json into self.templates_by_id"""
        self.templates_by_id = {}
        path = self._templates_path()
        if not os.path.exists(path):
            return
        try:
            import json
            data = json.load(open(path, "r", encoding="utf-8"))
            for t in data.get("templates", []):
                if isinstance(t, dict) and t.get("id"):
                    self.templates_by_id[t["id"]] = t
        except Exception as e:
            print("Failed to load templates:", e)

    def _save_templates(self):
        """Persist self.templates_by_id to templates.json"""
        try:
            import json
            path = self._templates_path()
            data = {"templates": list(self.templates_by_id.values())}
            with open(path, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
        except Exception as e:
            QMessageBox.warning(self, "Templates", f"Failed to save templates: {e}")

    def _apply_template_defaults(self, room: dict, tpl: dict, args: dict | None = None, mode: str = "link"):
        """
        Merge template defaults into room. If mode='link', attach template metadata and keep only deltas in room.
        If mode='stamp', we materialize fields and clear template metadata.
        """
        args = args or {}
        defaults = dict(tpl.get("defaults", {}))
        # merge: args override defaults
        merged = {**defaults, **args}
        # write into room fields (non-graph fields only)
        for k, v in merged.items():
            room[k] = v
        if mode == "link":
            room["template"] = {"id": tpl["id"], "args": args, "mode": "link"}
        else:
            room.pop("template", None)

    def _reapply_template_to_room(self, rid: str):
        """
        Re-merge a linked template into room, preserving current args delta.
        """
        room = self.rooms_by_id.get(rid)
        if not room:
            return
        tmeta = room.get("template") or {}
        if tmeta.get("mode") != "link":
            return
        tpl = self.templates_by_id.get(tmeta.get("id", ""))
        if not tpl:
            QMessageBox.information(self, "Template", f"Template '{tmeta.get('id')}' not found.")
            return
        args = dict(tmeta.get("args") or {})
        self._apply_template_defaults(room, tpl, args, mode="link")
        self._set_dirty(True)
        self.canvas.update()

    def _unlink_template_from_room(self, rid: str, bake_args: bool = True):
        """Break link: optionally bake the resolved values into the room and clear template meta."""
        room = self.rooms_by_id.get(rid)
        if not room:
            return
        tmeta = room.get("template") or {}
        if not tmeta:
            return
        if bake_args:
            # bake current resolved state; since room already holds values, just drop meta
            pass
        room.pop("template", None)
        self._set_dirty(True)
        self.canvas.update()

    def _stamp_template_as_new_room(self, node_id: str, tpl_id: str) -> str:
        """
        Create a brand new room from template (copy-on-write) and add it to node.
        """
        tpl = self.templates_by_id.get(tpl_id)
        if not tpl:
            raise ValueError(f"Template '{tpl_id}' not found")
        new_id, _ = self._next_room_id_pair(node_id)
        room = {"id": new_id, "title": tpl.get("title", new_id)}
        self._apply_template_defaults(room, tpl, args={}, mode="stamp")
        self.rooms_by_id[new_id] = room
        node = self.nodes_by_id[node_id]
        key = _node_room_key(node)
        node.setdefault(key, []).append(new_id)
        self._set_dirty(True)
        self._rebuild_tree()
        self.canvas.update()
        return new_id

    def _save_room_as_template(self, rid: str, tpl_id: str | None = None):
        """
        Convert a room into a template entry (no graph data).
        """
        import re
        room = self.rooms_by_id.get(rid)
        if not room:
            return
        # derive id if not provided
        if not tpl_id:
            base = room.get("title") or room.get("id") or "room"
            base = re.sub(r"[^a-zA-Z0-9_]+", "_", base).strip("_").lower()
            tpl_id = f"tpl_{base}"
        # strip graph-y fields and keep presentational ones
        keep_keys = {"title", "env", "background_image", "description"}
        defaults = {k: v for k, v in room.items() if k in keep_keys}
        tpl = {
            "id": tpl_id,
            "title": room.get("title", tpl_id),
            "defaults": defaults,
            "sockets": ["north","south","east","west"],
            "tags": []
        }
        self.templates_by_id[tpl_id] = tpl
        self._save_templates()
        QMessageBox.information(self, "Templates", f"Saved as template '{tpl_id}'.")

    # --- Data Loading / Saving ---
    def _load_all_data(self):
        # Main data files
        worlds = _safe_read_json(os.path.join(self.root, "worlds.json"), [])
        hubs = _safe_read_json(os.path.join(self.root, "hubs.json"), [])
        nodes = _safe_read_json(os.path.join(self.root, "nodes.json"), [])
        rooms = _safe_read_json(os.path.join(self.root, "rooms.json"), [])
        self.worlds_by_id = _to_id_map(worlds, id_key='id')
        self.hubs_by_id   = _to_id_map(hubs,   id_key='id')
        self.nodes_by_id  = _to_id_map(nodes,  id_key='id')
        self.rooms_by_id  = _to_id_map(rooms,  id_key='id')

        # Palette / lookup data files
        items   = _safe_read_json(os.path.join(self.root, "items.json"),   [])
        npcs    = _safe_read_json(os.path.join(self.root, "npcs.json"),    [])
        enemies = _safe_read_json(os.path.join(self.root, "enemies.json"), [])
        shops   = _safe_read_json(os.path.join(self.root, "shops.json"),   [])

        # Items & NPCs may use name as fallback key
        self.items_by_id   = _to_id_map(items,   id_key='id', alt_keys=('name',))
        self.npcs_by_id    = _to_id_map(npcs,    id_key='id', alt_keys=('name',))
        self.enemies_by_id = _to_id_map(enemies, id_key='id')
        self.shops_by_id   = _to_id_map(shops,   id_key='id')

        # Templates
        self.room_templates = _safe_read_json(os.path.join(self.root, "room_templates.json"), {}) or {}

    # --- Data Loading / Saving ---
    def _save_all_data(self, *, show_toast: bool = True):
        # Ensure pending changes from the active inspector are applied
        self._apply_inspector_changes()
        ok = True
        # sort lists by id before saving for stable diffs
        if not _safe_write_json(os.path.join(self.root, "worlds.json"), sorted(self.worlds_by_id.values(), key=lambda x: x.get("id","")), "Worlds"): ok=False
        if not _safe_write_json(os.path.join(self.root, "hubs.json"),   sorted(self.hubs_by_id.values(),   key=lambda x: x.get("id","")), "Hubs"): ok=False
        if not _safe_write_json(os.path.join(self.root, "nodes.json"),  sorted(self.nodes_by_id.values(),  key=lambda x: x.get("id","")), "Nodes"): ok=False
        if not _safe_write_json(os.path.join(self.root, "rooms.json"),  sorted(self.rooms_by_id.values(),  key=lambda x: x.get("id","")), "Rooms"): ok=False
        if not _safe_write_json(os.path.join(self.root, "room_templates.json"), self.room_templates, "Room Templates"): ok=False
        if ok:
            if show_toast:
                QMessageBox.information(self, "Success", "All data files saved successfully.")
            self._set_dirty(False)

    # --- Tree Management ---
    def _rebuild_tree(self):
        q = (self.tree_search.text() or "").strip().lower()
        self.tree.clear()
        world_items = {}
        for wid, w in sorted(self.worlds_by_id.items()):
            label = f"🌍 {w.get('title', wid)}"
            if q and (q not in (w.get('id',"").lower()) and q not in (w.get('title',"").lower())):
                # still include if any descendant matches -> postpone
                pass
            item = QTreeWidgetItem([label])
            item.setData(0, Qt.UserRole, ("world", wid))
            world_items[wid] = item

        # Build hierarchy, applying filter
        def room_matches(rid):
            r = self.rooms_by_id.get(rid, {})
            return (q == "" or q in rid.lower() or q in (r.get("title","").lower()))

        def node_matches(nid):
            n = self.nodes_by_id.get(nid, {})
            if q == "" or q in nid.lower() or q in (n.get("title","").lower()):
                return True
            key = _node_room_key(n)
            return any(room_matches(rid) for rid in n.get(key, []))

        def hub_matches(hid):
            h = self.hubs_by_id.get(hid, {})
            if q == "" or q in hid.lower() or q in (h.get("title","").lower()):
                return True
            return any(node_matches(nid) for nid, n in self.nodes_by_id.items() if n.get('hub_id') == hid)

        for wid, w in sorted(self.worlds_by_id.items()):
            w_item = world_items[wid]
            world_pass = (q == "" or q in wid.lower() or q in (w.get("title","").lower()))
            for hid, h in sorted(self.hubs_by_id.items()):
                if h.get('world_id') != wid: continue
                if not hub_matches(hid) and not world_pass:  # skip hub entirely
                    continue
                h_item = QTreeWidgetItem([f"🏙 {h.get('title', hid)}"])
                h_item.setData(0, Qt.UserRole, ("hub", hid))
                w_item.addChild(h_item)
                for nid, n in sorted(self.nodes_by_id.items()):
                    if n.get('hub_id') != hid: continue
                    if not node_matches(nid) and not hub_matches(hid) and not world_pass: continue
                    n_item = QTreeWidgetItem([f"🧭 {n.get('title', nid)}"])
                    n_item.setData(0, Qt.UserRole, ("node", nid))
                    h_item.addChild(n_item)
                    room_key = _node_room_key(n)
                    for rid in sorted(n.get(room_key, [])):
                        if not room_matches(rid) and not node_matches(nid) and not hub_matches(hid) and not world_pass:
                            continue
                        r = self.rooms_by_id.get(rid)
                        if not r: continue
                        r_item = QTreeWidgetItem([f"🚪 {r.get('title', rid)}"])
                        r_item.setData(0, Qt.UserRole, ("room", rid))
                        n_item.addChild(r_item)
            # add world only if it or its descendants matched
            if w_item.childCount() > 0 or world_pass:
                self.tree.addTopLevelItem(w_item)

        self.tree.expandAll()

    # --- Selection handling ---
    def _on_tree_selection_changed(self):
        items = self.tree.selectedItems()
        if not items:
            self.current_selection = (None, None)
            self._pending_node_mode = False
            self.inspector_stack.setCurrentIndex(0)
            self.canvas.set_mode(self.canvas.MODE_HUB)
            return

        typ, _id = items[0].data(0, Qt.UserRole)
        self.current_selection = (typ, _id)

        if typ == "world":
            self._pending_node_mode = False
            self.inspector_stack.setCurrentIndex(1)
            self._sync_world_inspector()
            self.canvas.set_mode(self.canvas.MODE_HUB)
            self.canvas_mode_label.setText("Mode: Hub")
        elif typ == "hub":
            self._pending_node_mode = False
            self.inspector_stack.setCurrentIndex(2)
            self._sync_hub_inspector()
            self.canvas.set_mode(self.canvas.MODE_HUB, hub_id=_id)
            self.canvas.selected_node_id = None
            self.canvas_mode_label.setText("Mode: Hub")
        elif typ == "node":
            force_node_mode = self._pending_node_mode or bool(QApplication.keyboardModifiers() & Qt.ControlModifier)
            self._pending_node_mode = False
            self._focus_node(_id, force_node_mode)
        elif typ == "room":
            self._pending_node_mode = False
            self.inspector_stack.setCurrentIndex(4)
            parent_node_id = None
            parent_item = items[0].parent()
            if parent_item:
                _, parent_node_id = parent_item.data(0, Qt.UserRole)
            if parent_node_id:
                node = self.nodes_by_id[parent_node_id]
                hub_id = node.get('hub_id')
                room_key = _node_room_key(node)
                visible_rooms = set(node.get(room_key, []))
                # Set node mode first
                self.canvas.set_mode(self.canvas.MODE_NODE, hub_id=hub_id, visible_room_ids=visible_rooms)
                self.canvas_mode_label.setText("Mode: Node")
            self.canvas.selected_room_id = _id
            self._sync_room_inspector()
            # Center on the selected room
            self.canvas.center_on_room(_id)

    def _select_in_tree(self, kind: str, _id: str, force_emit: bool = False):
        if not _id: return
        current_item = self.tree.currentItem()
        current_matches = False
        if current_item:
            cur_data = current_item.data(0, Qt.UserRole) or (None, None)
            current_matches = (cur_data[0] == kind and cur_data[1] == _id)
        it = QTreeWidgetItemIterator(self.tree)
        while it.value():
            item = it.value()
            typ, id_val = item.data(0, Qt.UserRole) or (None, None)
            if typ == kind and id_val == _id:
                self.tree.setCurrentItem(item)
                self.tree.scrollToItem(item, QTreeWidget.PositionAtCenter)
                if force_emit and current_matches:
                    self.current_selection = (kind, _id)
                    self._on_tree_selection_changed()
                return
            it += 1

    def _focus_node(self, node_id: str, force_node_mode: bool):
        node = self.nodes_by_id.get(node_id)
        if not node:
            return
        self.inspector_stack.setCurrentIndex(3)
        # self._sync_node_inspector()
        hub_id = node.get('hub_id')
        room_key = _node_room_key(node)
        visible_rooms = set(node.get(room_key, []))
        if force_node_mode:
            self.canvas.set_mode(self.canvas.MODE_NODE, hub_id=hub_id, visible_room_ids=visible_rooms)
            self.canvas_mode_label.setText("Mode: Node")
        else:
            if self.canvas.mode != self.canvas.MODE_HUB or self.canvas.current_hub_id != hub_id:
                self.canvas.set_mode(self.canvas.MODE_HUB, hub_id=hub_id)
            self.canvas_mode_label.setText("Mode: Hub")
        self.canvas.selected_node_id = node_id
        self.canvas.selected_room_id = None
        if hasattr(self.canvas, 'selected_room_ids'):
            self.canvas.selected_room_ids.clear()
        self.canvas.update()

    def _on_canvas_node_double_clicked(self, node_id: str):
        if not node_id:
            return
        self._pending_node_mode = True
        self._select_in_tree("node", node_id, force_emit=True)

    def on_canvas_node_clicked(self, node_id: str):
        if not node_id:
            return
        self._pending_node_mode = False
        current_item = self.tree.currentItem()
        if current_item:
            cur_type, cur_id = current_item.data(0, Qt.UserRole) or (None, None)
            if cur_type == "node" and cur_id == node_id:
                self.current_selection = ("node", node_id)
                self._focus_node(node_id, False)
                return
        self._select_in_tree("node", node_id, force_emit=True)

    def _on_tree_item_double_clicked(self, item, column):
        data = item.data(0, Qt.UserRole) if item else None
        if not data:
            return
        typ, _id = data
        if typ in ("node", "room"):
            self._pending_node_mode = True
            self._on_tree_selection_changed()

    # --- Inspector Sync Methods ---
    def _sync_world_inspector(self):
        _id = self.current_selection[1]
        w = self.worlds_by_id.get(_id, {})
        self.w_id.setText(w.get("id", ""))
        self.w_title.setText(w.get("title", ""))
        self.w_desc.setText(w.get("description", ""))

    def _sync_hub_inspector(self):
        _id = self.current_selection[1]
        h = self.hubs_by_id.get(_id, {})
        self.h_id.setText(h.get("id", ""))
        self.h_title.setText(h.get("title", ""))
        self.h_bg.setText(h.get("background_image", ""))
        # discovery flag (default True if missing)
        try:
            self.h_discovered.setChecked(bool(h.get("discovered", True)))
        except Exception:
            self.h_discovered.setChecked(True)
        self.h_world.blockSignals(True); self.h_world.clear()
        for wid, w in self.worlds_by_id.items(): self.h_world.addItem(f"{w.get('title', wid)} ({wid})", wid)
        idx = self.h_world.findData(h.get("world_id"))
        self.h_world.setCurrentIndex(max(0, idx))
        self.h_world.blockSignals(False)

    def _sync_node_inspector(self):
        _id = self.current_selection[1]
        n = self.nodes_by_id.get(_id, {})
        self.n_id.setText(n.get("id", ""))
        self.n_title.setText(n.get("title", ""))
        self.n_icon.setText(n.get("icon_image", ""))
        sz = n.get("size", [128, 128]); self.n_w.setValue(int(sz[0])); self.n_h.setValue(int(sz[1]))
        ph = n.get("pos_hint", {"center_x": 0.5, "center_y": 0.5}); self.n_cx.setValue(float(ph.get("center_x", 0.5))); self.n_cy.setValue(float(ph.get("center_y", 0.5)))
        # discovery flag (default True if missing)
        try:
            self.n_discovered.setChecked(bool(n.get("discovered", True)))
        except Exception:
            self.n_discovered.setChecked(True)
        self.n_title_gap.blockSignals(True)
        try:
            self.n_title_gap.setValue(int(n.get("title_gap", 0)))
        except Exception:
            self.n_title_gap.setValue(0)
        finally:
            self.n_title_gap.blockSignals(False)
        # Entry room combo from node rooms
        self.n_entry.blockSignals(True); self.n_entry.clear()
        room_key = _node_room_key(n); room_ids = n.get(room_key, [])
        self.n_entry.addItems([""] + sorted(room_ids))
        idx = self.n_entry.findText(n.get("entry_room", ""))
        self.n_entry.setCurrentIndex(max(0, idx))
        self.n_entry.blockSignals(False)

    def _make_room_completer(self, line_edit: QLineEdit, candidate_ids: List[str]):
        comp = QCompleter(sorted(candidate_ids)); comp.setCaseSensitivity(Qt.CaseInsensitive)
        comp.setFilterMode(Qt.MatchContains); line_edit.setCompleter(comp)

    def _sync_room_inspector(self):
        _id = self.current_selection[1]
        r = self.rooms_by_id.get(_id, {})
        if not r: self._clear_room_inspector(_id); return

        # Block signals while populating
        widgets_signals = [
            (self.r_title, 'editingFinished'), (self.r_env, 'editingFinished'),
            # Background: also watch textChanged so the '.' browser apply is captured immediately
            (self.r_bg, 'editingFinished'), (self.r_bg, 'textChanged'),
            (self.r_pos_x, 'valueChanged'), (self.r_pos_y, 'valueChanged'),
            (self.desc_edit, 'textChanged'),
            (self.r_conn_n, 'editingFinished'), (self.r_conn_s, 'editingFinished'),
            (self.r_conn_e, 'editingFinished'), (self.r_conn_w, 'editingFinished'),
            (self.r_items, 'editingFinished'), (self.r_npcs, 'editingFinished'),
            (self.r_enemies_editor, 'dataChanged'), (self.r_actions_editor, 'dataChanged'),
            (self.r_state_editor, 'dataChanged'),
            (self.r_item_flavor_editor, 'dataChanged'),
            (self.r_enemy_flavor_editor, 'dataChanged'),
            (self.r_blocked_editor, 'dataChanged')
        ]
        for w, _ in widgets_signals:
            try: w.blockSignals(True)
            except Exception: pass

        # Populate
        self.r_id.setText(_id)
        self.r_title.setText(r.get("title", ""))
        self.r_env.setText(r.get("env", ""))
        self.r_weather.setCurrentText(r.get("weather", ""))
        self.r_bg.setText(r.get("background_image", ""))
        pos = r.get("pos", [0,0]); self.r_pos_x.setValue(int(pos[0])); self.r_pos_y.setValue(int(pos[1]))
        self.desc_edit.setPlainText(r.get("description", "")); self._update_kivy_preview()
        conns = r.get("connections", {})
        self.r_conn_n.setText(conns.get("north", "")); self.r_conn_s.setText(conns.get("south", "")); self.r_conn_e.setText(conns.get("east", "")); self.r_conn_w.setText(conns.get("west", ""))
        self.r_items.setText(", ".join(r.get("items", []))); self.r_npcs.setText(", ".join(r.get("npcs", [])))
        self.r_enemies_editor.set_data(r.get("enemies", [])); self.r_actions_editor.set_data(r.get("actions", [])); self.r_state_editor.set_data(r.get("state", {}))
        self.r_item_flavor_editor.set_data(r.get("item_flavor", {}))
        self.r_enemy_flavor_editor.set_data(r.get("enemy_flavor", {}))
        self.r_blocked_editor.set_data(r.get("blocked_directions", {}))

        # Completers for connections (only rooms in the current node)
        # Find node containing this room
        parent_node_id = None
        for nid, n in self.nodes_by_id.items():
            key = _node_room_key(n)
            if _id in n.get(key, []):
                parent_node_id = nid; break
        candidate_ids = []
        if parent_node_id:
            node = self.nodes_by_id[parent_node_id]
            candidate_ids = list(node.get(_node_room_key(node), []))
        for le in (self.r_conn_n, self.r_conn_s, self.r_conn_e, self.r_conn_w):
            self._make_room_completer(le, candidate_ids)
        # Starting room checkbox reflects the node's entry_room
        parent_node_id = None
        for nid, n in self.nodes_by_id.items():
            key = _node_room_key(n)
            if _id in n.get(key, []):
                parent_node_id = nid
                break

        is_start = False
        if parent_node_id:
            node = self.nodes_by_id[parent_node_id]
            is_start = (node.get("entry_room") == _id)

        try:
            self.r_is_entry.blockSignals(True)
        except Exception:
            pass
        self.r_is_entry.setChecked(is_start)
        try:
            self.r_is_entry.blockSignals(False)
        except Exception:
            pass

        # Reconnect change handlers now that fields are populated
        for w, sig_name in widgets_signals:
            try:
                # unblock and reconnect safely
                w.blockSignals(False)
                sig = getattr(w, sig_name, None)
                if sig is None:
                    continue
                try:
                    sig.disconnect()
                except Exception:
                    pass
                # Route to room apply on any change
                sig.connect(self._apply_room_inspector_changes)
            except Exception:
                pass
        try:
            # Connect starting room checkbox separately
            self.r_is_entry.toggled.disconnect()
        except Exception:
            pass
        try:
            self.r_is_entry.toggled.connect(self._on_toggle_starting_room)
        except Exception:
            pass

    def _clear_room_inspector(self, current_id=None):
        self.r_id.setText(current_id or "—"); self.r_title.clear(); self.r_env.clear(); self.r_bg.clear(); self.r_weather.setCurrentText("")
        self.r_pos_x.setValue(0); self.r_pos_y.setValue(0); self.desc_edit.clear(); self.desc_preview.clear()
        self.r_conn_n.clear(); self.r_conn_s.clear(); self.r_conn_e.clear(); self.r_conn_w.clear()
        self.r_items.clear(); self.r_npcs.clear(); self.r_enemies_editor.set_data([]); self.r_actions_editor.set_data([]); self.r_state_editor.set_data({}); self.r_blocked_editor.set_data({})
        self.r_item_flavor_edit.clear(); self.r_enemy_flavor_edit.clear()
    def _on_toggle_starting_room(self, checked: bool):
        rid = self.current_selection[1]
        nid = self._find_parent_node_id_for_room(rid)
        if not nid:
            return
        node = self.nodes_by_id[nid]
        if checked:
            node["entry_room"] = rid
        else:
            if node.get("entry_room") == rid:
                node["entry_room"] = ""
        self._set_dirty(True)
        if self.current_selection == ("room", rid):
            self._sync_room_inspector()
        if self.current_selection == ("node", nid):
            self._sync_node_inspector()

    # --- Inspector Apply/Update Methods ---
    def _apply_inspector_changes(self):
        typ, _id = self.current_selection
        if not _id: return
        self._set_dirty(True)
        if typ == "world":
            w = self.worlds_by_id[_id]; new_id = _sanitize_id(self.w_id.text())
            if new_id and new_id != _id: self._remap_id("world", _id, new_id); _id = new_id
            w = self.worlds_by_id[_id]; w["title"] = self.w_title.text(); w["description"] = self.w_desc.text()
        elif typ == "hub":
            h = self.hubs_by_id[_id]; new_id = _sanitize_id(self.h_id.text())
            if new_id and new_id != _id: self._remap_id("hub", _id, new_id); _id = new_id
            h = self.hubs_by_id[_id]
            h["title"] = self.h_title.text()
            h["world_id"] = self.h_world.currentData()
            h["background_image"] = self.h_bg.text()
            h["discovered"] = bool(self.h_discovered.isChecked())
        elif typ == "node":
            n = self.nodes_by_id[_id]
            new_id = _sanitize_id(self.n_id.text())
            if new_id and new_id != _id:
                self._remap_id("node", _id, new_id)
                _id = new_id
            n = self.nodes_by_id[_id]
            n["title"] = self.n_title.text()
            n["entry_room"] = self.n_entry.currentText()
            n["icon_image"] = self.n_icon.text()
            n["discovered"] = bool(self.n_discovered.isChecked())
            n["size"] = [self.n_w.value(), self.n_h.value()]  # â† persist W/H
            n["pos_hint"] = {"center_x": self.n_cx.value(), "center_y": self.n_cy.value()}
            try:
                n["title_gap"] = int(self.n_title_gap.value())
            except Exception:
                n["title_gap"] = 0
        elif typ == "room":
            # Apply room inspector fields even if focus hasn't left controls
            self._apply_room_inspector_changes()
        self._rebuild_tree(); self._select_in_tree(typ, self.current_selection[1]); self.canvas.update()

    def _apply_room_inspector_changes(self, *args):
        typ, _id = self.current_selection
        if typ != 'room' or not _id or _id not in self.rooms_by_id: return
        self._set_dirty(True)
        r = self.rooms_by_id[_id]
        r["title"] = self.r_title.text(); r["env"] = self.r_env.text()
        weather = self.r_weather.currentText().strip()
        # --- NEW: Handle weather property ---
        if weather and weather != "none": r["weather"] = weather
        else: r.pop("weather", None) # Clean up if set to none/empty
        # --- END NEW ---
        r["background_image"] = self.r_bg.text()
        r["pos"] = [self.r_pos_x.value(), self.r_pos_y.value()]
        r["description"] = self.desc_edit.toPlainText(); self._update_kivy_preview()
        conns = {}
        if self.r_conn_n.text(): conns["north"] = self.r_conn_n.text()
        if self.r_conn_s.text(): conns["south"] = self.r_conn_s.text()
        if self.r_conn_e.text(): conns["east"] = self.r_conn_e.text()
        if self.r_conn_w.text(): conns["west"] = self.r_conn_w.text()
        r["connections"] = conns
        r["items"] = [s.strip() for s in self.r_items.text().split(',') if s.strip()]
        r["npcs"] = [s.strip() for s in self.r_npcs.text().split(',') if s.strip()]
        r["enemies"] = self.r_enemies_editor.get_data()
        r["actions"] = self.r_actions_editor.get_data()
        r["state"] = self.r_state_editor.get_data()
        r["item_flavor"] = self.r_item_flavor_editor.get_data()
        r["enemy_flavor"] = self.r_enemy_flavor_editor.get_data()
        try:
            blocked_data = self.r_blocked_editor.get_data()
        except Exception as exc:
            QMessageBox.warning(self, 'Blocked Directions', f'Invalid blocked direction data: {exc}')
            return
        if blocked_data:
            r["blocked_directions"] = blocked_data
        else:
            r.pop("blocked_directions", None)
        self.canvas.update()

    # --- Add/Delete / Canvas Callbacks ---
    def _add_world(self):
        new_id, ok = QInputDialog.getText(self, "New World", "Enter new World ID:")
        if not ok or not new_id: return
        new_id = _sanitize_id(new_id)
        if new_id in self.worlds_by_id: QMessageBox.warning(self, "Error", "World ID already exists."); return
        self.worlds_by_id[new_id] = {"id": new_id, "title": new_id.replace("_", " ").title(), "description": ""}
        self._set_dirty(True); self._rebuild_tree(); self._select_in_tree("world", new_id)

    def _add_hub(self):
        typ, _id = self.current_selection
        if typ != "world": QMessageBox.warning(self, "Error", "Please select a World to add a Hub to."); return
        world_id = _id
        new_id, ok = QInputDialog.getText(self, "New Hub", "Enter new Hub ID:")
        if not ok or not new_id: return
        new_id = _sanitize_id(new_id)
        if new_id in self.hubs_by_id: QMessageBox.warning(self, "Error", "Hub ID already exists."); return
        self.hubs_by_id[new_id] = {"id": new_id, "world_id": world_id, "title": new_id.replace("_", " ").title(), "discovered": True}
        self._set_dirty(True); self._rebuild_tree(); self._select_in_tree("hub", new_id)

    def _add_node(self, pos_hint: Optional[QPointF] = None):
        typ, _id = self.current_selection
        if typ not in ["hub", "node", "room"]: QMessageBox.warning(self, "Error", "Please select a Hub to add a Node to."); return
        hub_id = self.hubs_by_id[_id]['id'] if typ == "hub" else self.nodes_by_id[_id]['hub_id'] if typ == "node" else \
                 next(n['hub_id'] for n in self.nodes_by_id.values() if _id in n.get(_node_room_key(n),[]))
        new_id, ok = QInputDialog.getText(self, "New Node", "Enter new Node ID:")
        if not ok or not new_id: return
        new_id = _sanitize_id(new_id)
        if new_id in self.nodes_by_id: QMessageBox.warning(self, "Error", "Node ID already exists."); return
        new_node = {"id": new_id, "hub_id": hub_id, "title": new_id.replace("_", " ").title(), "rooms": [], "size":[128,128], "pos_hint":{"center_x":0.5,"center_y":0.5}, "discovered": True}
        if pos_hint and self.canvas._bg_size.width() and self.canvas._bg_size.height():
            w, h = self.canvas._bg_size.width(), self.canvas._bg_size.height()
            new_node["pos_hint"] = {"center_x": round(pos_hint.x() / w, 6), "center_y": round(1.0 - (pos_hint.y() / h), 6)}
        self.nodes_by_id[new_id] = new_node
        self._set_dirty(True); self._rebuild_tree(); self._select_in_tree("node", new_id)

    def _delete_selected(self):
        typ, _id = self.current_selection
        if not _id:
            return
        reply = QMessageBox.question(
            self, "Confirm Delete",
            f"Are you sure you want to delete '{_id}' and all its children?",
            QMessageBox.Yes | QMessageBox.No
        )
        if reply != QMessageBox.Yes:
            return

        # Decide where to keep focus after deletion
        focus_role, focus_id = (None, None)
        if typ == "room":
            focus_id = self._find_parent_node_id_for_room(_id)
            focus_role = "node" if focus_id else None
        elif typ == "node":
            hub_id = self.nodes_by_id.get(_id, {}).get("hub_id")
            focus_role, focus_id = ("hub", hub_id) if hub_id else (None, None)
        elif typ == "hub":
            world_id = self.hubs_by_id.get(_id, {}).get("world_id")
            focus_role, focus_id = ("world", world_id) if world_id else (None, None)
        else:
            # world delete â†’ no sensible parent to focus
            pass

        # Perform deletion
        if typ == "world":
            hubs_to_del = {hid for hid, h in self.hubs_by_id.items() if h.get('world_id') == _id}
            nodes_to_del = {nid for nid, n in self.nodes_by_id.items() if n.get('hub_id') in hubs_to_del}
            for nid in nodes_to_del:
                self.nodes_by_id.pop(nid, None)
            for hid in hubs_to_del:
                self.hubs_by_id.pop(hid, None)
            self.worlds_by_id.pop(_id, None)
        elif typ == "hub":
            nodes_to_del = {nid for nid, n in self.nodes_by_id.items() if n.get('hub_id') == _id}
            for nid in nodes_to_del:
                self.nodes_by_id.pop(nid, None)
            self.hubs_by_id.pop(_id, None)
        elif typ == "node":
            self.nodes_by_id.pop(_id, None)
        elif typ == "room":
            for n in self.nodes_by_id.values():
                key = _node_room_key(n)
                if key in n and _id in n[key]:
                    n[key].remove(_id)
            try:
                self._ask_prune_after_room_delete(_id)
            except Exception:
                pass

        self._set_dirty(True)
        self._rebuild_tree()

        # Keep focus where it makes sense (and stay in the grid for room deletion)
        if focus_role and focus_id and (
            (focus_role == "world" and focus_id in self.worlds_by_id) or
            (focus_role == "hub" and focus_id in self.hubs_by_id) or
            (focus_role == "node" and focus_id in self.nodes_by_id)
        ):
            self._select_in_tree(focus_role, focus_id)
        # else: leave selection as-is

        self.canvas.update()

    def on_add_room_at(self, wx: float, wy: float):
        typ, node_id = self.current_selection
        if typ not in ["node", "room"]:
            QMessageBox.warning(self, "Error", "Select a node to add rooms to.")
            return
        if typ == "room":
            node_id = self._find_parent_node_id_for_room(self.current_selection[1])
        self._auto_create_room_at(node_id, wx, wy)

    def on_duplicate_room(self, rid: str):
        import copy
        base = copy.deepcopy(self.rooms_by_id[rid])
        pos = base.get("pos", [0,0])
        # ensure fresh fields
        for k in ("id", "pos", "connections"):
            if k in base:
                del base[k]
        wx, wy = pos[0] + 1, pos[1]
        typ, node_id = self.current_selection
        if typ == "room":
            node_id = self._find_parent_node_id_for_room(rid)
        self._auto_create_room_at(node_id, wx, wy, template=base)

    def on_delete_room(self, rid: str):
        """
        Delete from map flow â€” ask user WHAT to delete:
        â€¢ Remove from node only (keep JSON)  â†’ room becomes orphan
        â€¢ Delete everywhere (JSON + links)   â†’ permanent purge
        â€¢ Cancel
        """
        if not rid or rid not in self.rooms_by_id:
            return

        dlg = QMessageBox(self)
        dlg.setWindowTitle("Delete Room")
        dlg.setIcon(QMessageBox.Warning)
        dlg.setText(f"Delete room '{rid}' â€” what should happen?")
        remove_node_only = dlg.addButton("Remove from Node (keep JSON)", QMessageBox.AcceptRole)
        delete_all = dlg.addButton("Delete Everywhere (JSON + links)", QMessageBox.DestructiveRole)
        cancel_btn = dlg.addButton(QMessageBox.Cancel)
        dlg.exec_()

        clicked = dlg.clickedButton()
        if clicked is cancel_btn:
            return

        # Remember parent node for focus
        nid_for_focus = self._find_parent_node_id_for_room(rid)

        # Always remove from node lists
        for n in self.nodes_by_id.values():
            key = _node_room_key(n)
            if key in n and rid in n[key]:
                n[key].remove(rid)

        if clicked is remove_node_only:
            try:
                self._ask_prune_after_room_delete(rid)  # offer to prune inbound links
            except Exception:
                pass
            self._set_dirty(True)
            self._rebuild_tree()
            # Keep the node grid in view
            if nid_for_focus and nid_for_focus in self.nodes_by_id:
                self._select_in_tree("node", nid_for_focus)
            self.canvas.update()
            return

        if clicked is delete_all:
            removed = self._delete_room_everywhere(rid, fix_inbound=True, reassign_entry=True)
            QMessageBox.information(self, "Deleted",
                                    f"Deleted room '{rid}'. Removed {removed} inbound link(s).")
            self._rebuild_tree()
            # Keep the node grid in view
            if nid_for_focus and nid_for_focus in self.nodes_by_id:
                self._select_in_tree("node", nid_for_focus)
            self.canvas.update()

    def on_delete_rooms_bulk(self, room_ids: list):
        """
        Bulk delete rooms (from canvas context menu or Delete key).
        Preserves focus on the current node grid.
        """
        if not room_ids:
            return
        # Choose a node to keep focused: current node if visible, else parent of first room
        nid_for_focus = None
        typ, sel_id = self.current_selection
        if typ == "node":
            nid_for_focus = sel_id
        elif typ == "room":
            nid_for_focus = self._find_parent_node_id_for_room(sel_id)
        if not nid_for_focus:
            for rid in room_ids:
                nid_for_focus = self._find_parent_node_id_for_room(rid)
                if nid_for_focus:
                    break

        # Delete each room from the node list and prune inbound links
        for rid in list(room_ids):
            for n in self.nodes_by_id.values():
                key = _node_room_key(n)
                if key in n and rid in n[key]:
                    n[key].remove(rid)
            try:
                self._ask_prune_after_room_delete(rid)
            except Exception:
                pass

        self._set_dirty(True)
        self._rebuild_tree()
        if nid_for_focus and nid_for_focus in self.nodes_by_id:
            self._select_in_tree("node", nid_for_focus)
        self.canvas.selected_room_ids.clear()
        self.canvas.selected_room_id = None
        self.canvas.update()

    # --- Helpers: auto room IDs & quick ops ---
    def _find_parent_node_id_for_room(self, rid: str) -> str:
        for nid, n in self.nodes_by_id.items():
            key = _node_room_key(n)
            if rid in n.get(key, []):
                return nid
        return ""

    def _next_room_id_for_node(self, node_id: str):
        """
        Return (next_room_id_str, index_int), e.g. ("mines_4", 4).

        We consider both:
        â€¢ the nodeâ€™s own room list, and
        â€¢ any existing rooms_by_id keys that start with node_id_,
        so we donâ€™t collide with already-renamed/adopted rooms.
        """
        import re
        node = self.nodes_by_id.get(node_id, {})
        key = _node_room_key(node)
        rx = re.compile(rf"^{re.escape(node_id)}_(\d+)$")

        # Collect all ids that look like node_id_N
        seen = set(str(rid) for rid in node.get(key, []))
        seen.update(str(rid) for rid in self.rooms_by_id.keys() if str(rid).startswith(f"{node_id}_"))

        max_n = 0
        for rid in seen:
            m = rx.match(rid)
            if m:
                try:
                    n = int(m.group(1))
                    if n > max_n:
                        max_n = n
                except Exception:
                    pass

        n = max_n + 1
        return f"{node_id}_{n}", n

    def _next_room_id_pair(self, node_id: str):
        """
        Always returns (room_id, index). Works even if older code returns a string.
        """
        import re
        res = self._next_room_id_for_node(node_id)
        if isinstance(res, tuple) and len(res) == 2:
            rid, n = res
            return str(rid), int(n)

        rid = str(res)
        m = re.match(rf"^{re.escape(node_id)}_(\d+)$", rid)
        if m:
            try:
                return rid, int(m.group(1))
            except Exception:
                pass

        # Fallback: compute next index from what exists
        rx = re.compile(rf"^{re.escape(node_id)}_(\d+)$")
        max_n = 0
        node = self.nodes_by_id.get(node_id, {})
        key = _node_room_key(node)
        seen = set(str(r) for r in node.get(key, []))
        seen.update(str(r) for r in self.rooms_by_id.keys() if str(r).startswith(f"{node_id}_"))
        for r in seen:
            m = rx.match(r)
            if m:
                try:
                    n = int(m.group(1))
                    if n > max_n:
                        max_n = n
                except Exception:
                    pass
        n = max_n + 1
        return f"{node_id}_{n}", n

    def _auto_create_room_at(self, node_id: str, wx: float, wy: float, template: dict | None = None, base_title: str | None = None) -> str:
        import copy
        rid, n = self._next_room_id_pair(node_id)
        if template is None:
            new_room = {
                "id": rid, "title": base_title or f"{node_id} {n}", "env": node_id,
                "background_image": "", "description": "", "pos": [round(wx), round(wy)],
                "connections": {}, "items": [], "npcs": [], "enemies": [], "actions": [], "state": {}
            }
        else:
            new_room = copy.deepcopy(template)
            for k in ("id", "pos", "connections"):
                if k in new_room:
                    del new_room[k]
            new_room["id"] = rid
            new_room["title"] = base_title or new_room.get("title") or f"{node_id} {n}"
            new_room.setdefault("env", node_id)
            new_room["pos"] = [round(wx), round(wy)]
            new_room.setdefault("connections", {})
            new_room.setdefault("items", []); new_room.setdefault("npcs", []); new_room.setdefault("enemies", [])
            new_room.setdefault("actions", []); new_room.setdefault("state", {})

        self.rooms_by_id[rid] = new_room
        node = self.nodes_by_id[node_id]; key = _node_room_key(node)
        node.setdefault(key, []).append(rid)
        if not node.get("entry_room"):
            node["entry_room"] = rid
        self._set_dirty(True)
        self._rebuild_tree()
        self._select_in_tree("room", rid)
        self.canvas.selected_room_id = rid
        self.canvas.update()
        return rid

    def quick_add_room(self, wx: float, wy: float):
        typ, node_id = self.current_selection
        if typ == "room":
            node_id = self._find_parent_node_id_for_room(self.current_selection[1])
        elif typ == "node":
            pass
        else:
            QMessageBox.information(self, "Quick Create", "Select a node (or any room inside it) to quick-create rooms.")
            return
        if not node_id:
            QMessageBox.warning(self, "Quick Create", "Unable to determine node for new room.")
            return
        self._auto_create_room_at(node_id, wx, wy)

    def quick_connect_rooms(self, start_id: str, end_id: str):
        a = self.rooms_by_id.get(start_id); b = self.rooms_by_id.get(end_id)
        if not a or not b: return
        ax, ay = a.get("pos", [0,0]); bx, by = b.get("pos", [0,0])
        dx, dy = (bx - ax), (by - ay)
        if abs(dx) >= abs(dy):
            d1 = "east" if dx > 0 else "west"
        else:
            d1 = "north" if dy > 0 else "south"
        opposite = {"north":"south","south":"north","east":"west","west":"east"}
        d2 = opposite[d1]
        a.setdefault("connections", {})[d1] = end_id
        b.setdefault("connections", {})[d2] = start_id
        self._set_dirty(True)
        if self.current_selection == ("room", start_id) or self.current_selection == ("room", end_id):
            self._sync_room_inspector()
        self.canvas.update()

    def set_node_entry_room(self, rid: str):
        nid = self._find_parent_node_id_for_room(rid)
        if not nid: return
        node = self.nodes_by_id[nid]
        node["entry_room"] = rid
        self._set_dirty(True)
        if self.current_selection == ("node", nid):
            self._sync_node_inspector()
        if self.current_selection == ("room", rid):
            self._sync_room_inspector()
        self.canvas.update()

    # --- Orphan handling & renaming ---
    def _get_orphan_room_ids(self):
        in_nodes = set()
        for n in self.nodes_by_id.values():
            key = _node_room_key(n)
            in_nodes.update(n.get(key, []))
        return [rid for rid in self.rooms_by_id.keys() if rid not in in_nodes]

    def _next_room_id_for_node(self, node_id: str) -> str:
        prefix = f"{node_id}_"
        used = set()
        for rid in self.rooms_by_id.keys():
            if rid.startswith(prefix):
                tail = rid[len(prefix):]
                if tail.isdigit():
                    used.add(int(tail))
        i = 1
        while i in used:
            i += 1
        return f"{node_id}_{i}"

    def _delete_room_everywhere(self, rid: str, *, fix_inbound: bool = True, reassign_entry: bool = True) -> int:
        """
        Permanently remove a room from the project:
        â€¢ Delete from rooms_by_id
        â€¢ Remove from all nodes' room lists
        â€¢ Fix nodes' entry_room if they pointed to rid
        â€¢ Remove all inbound links from other rooms (if fix_inbound=True)
        Returns: number of inbound links removed.
        """
        if rid not in self.rooms_by_id:
            return 0

        # 1) Remove from nodes
        for n in self.nodes_by_id.values():
            key = _node_room_key(n)
            if key in n and rid in n[key]:
                n[key] = [x for x in n[key] if x != rid]
            if reassign_entry and n.get("entry_room") == rid:
                rooms = n.get(key, [])
                n["entry_room"] = rooms[0] if rooms else ""

        # 2) Remove inbound references from other rooms
        removed = 0
        if fix_inbound:
            removed = self._delete_inbound_links_to_room(rid)

        # 3) Delete the room object itself
        try:
            del self.rooms_by_id[rid]
        except KeyError:
            pass

        self._set_dirty(True)
        self._rebuild_tree()
        self.canvas.update()
        return removed

    def _edit_room_dialog(self, rid: str):
        """
        Minimal room editor (dialog) for quick fixes:
        â€¢ Title, Env, Background, Description
        â€¢ Raw JSON tab (power users)
        â€¢ If JSON 'id' changes, we perform a full-safe rename.
        """
        room = self.rooms_by_id.get(rid)
        if not room:
            QMessageBox.information(self, "Edit Room", f"Room '{rid}' not found.")
            return

        dlg = QDialog(self)
        dlg.setWindowTitle(f"Edit Room â€” {rid}")
        v = QVBoxLayout(dlg)

        tabs = QTabWidget()
        v.addWidget(tabs)

        # Form tab
        formw = QWidget(); form = QFormLayout(formw)
        title = QLineEdit(room.get("title", ""))
        env = QLineEdit(room.get("env", ""))
        bg = QLineEdit(room.get("background_image", ""))
        desc = QPlainTextEdit(room.get("description", ""))
        desc.setMinimumHeight(120)

        form.addRow("Title:", title)
        form.addRow("Env:", env)
        form.addRow("Background Image:", bg)
        form.addRow("Description:", desc)
        tabs.addTab(formw, "Form")

        # Raw JSON tab
        import json as _json
        raww = QWidget(); rawv = QVBoxLayout(raww)
        raw = QPlainTextEdit(_json.dumps(room, indent=2, ensure_ascii=False))
        raw.setMinimumHeight(240)
        rawv.addWidget(raw)
        tabs.addTab(raww, "Raw JSON")

        btns = QDialogButtonBox(QDialogButtonBox.Save | QDialogButtonBox.Cancel)
        v.addWidget(btns)

        def do_save():
            idx = tabs.currentIndex()
            if idx == 1:
                # Save from Raw JSON
                try:
                    doc = _json.loads(raw.toPlainText())
                    if not isinstance(doc, dict):
                        raise ValueError("JSON must be an object")
                except Exception as e:
                    QMessageBox.warning(dlg, "Invalid JSON", str(e)); return

                new_id = str(doc.get("id", rid))
                if new_id != rid:
                    new_id = self._rename_room_id(rid, new_id)
                    QMessageBox.information(dlg, "Renamed", f"Room id is now '{new_id}'.")
                    # Refresh binding to renamed room
                    rid_local = new_id
                else:
                    rid_local = rid

                self.rooms_by_id[rid_local].clear()
                self.rooms_by_id[rid_local].update(doc)
            else:
                # Save from form
                room["title"] = title.text()
                room["env"] = env.text()
                room["background_image"] = bg.text()
                room["description"] = desc.toPlainText()

            self._set_dirty(True)
            self._rebuild_tree()
            self.canvas.update()
            dlg.accept()

        btns.accepted.connect(do_save)
        btns.rejected.connect(dlg.reject)
        dlg.exec_()

    def _rename_room_id(self, old_id: str, new_id: str):
        if old_id == new_id:
            return new_id
        if new_id in self.rooms_by_id:
            base = new_id
            node_id = base.rsplit("_", 1)[0] if "_" in base else base
            new_id = self._next_room_id_for_node(node_id)
        r = self.rooms_by_id.get(old_id)
        if not r:
            return new_id
        r["id"] = new_id
        self.rooms_by_id[new_id] = r
        del self.rooms_by_id[old_id]
        for n in self.nodes_by_id.values():
            key = _node_room_key(n)
            if old_id in n.get(key, []):
                n[key] = [new_id if x == old_id else x for x in n.get(key, [])]
            if n.get("entry_room") == old_id:
                n["entry_room"] = new_id
        for _rid, _room in self.rooms_by_id.items():
            conns = _room.get("connections") or {}
            for d, dst in list(conns.items()):
                if dst == old_id:
                    conns[d] = new_id
        self._set_dirty(True)
        return new_id

    def _show_orphans_for_current_node(self):
        """
        Orphan Rooms dialog:
        â€¢ Search/filter orphan rooms (not in any node)
        â€¢ Edit selected room (simple form + raw JSON tab)
        â€¢ Rename selected room (full reference update)
        â€¢ Adopt â†’ adds to this node, optional auto-rename to node_id_N
        â€¢ Delete Permanently â†’ purge from JSON + all references
        """
        typ, node_id = self.current_selection or (None, None)
        if typ != "node" or not node_id:
            QMessageBox.information(self, "Orphans", "Select a node in the World Atlas first.")
            return

        # Compute orphan list
        def orphan_ids():
            in_nodes = set()
            for n in self.nodes_by_id.values():
                key = _node_room_key(n)
                in_nodes.update(n.get(key, []))
            return sorted(rid for rid in self.rooms_by_id.keys() if rid not in in_nodes)

        orphans = orphan_ids()

        dlg = QDialog(self)
        dlg.setWindowTitle(f"Orphan Rooms â†’ {node_id}")
        v = QVBoxLayout(dlg)

        v.addWidget(QLabel(f"Rooms not assigned to any node. Working node: <b>{node_id}</b>"))

        # Search/filter
        search = QLineEdit(); search.setPlaceholderText("Filter by id/title…")
        v.addWidget(search)

        # List
        listw = QListWidget()
        listw.setSelectionMode(QListWidget.ExtendedSelection)

        def populate(filter_text=""):
            listw.clear()
            for rid in orphans:
                room = self.rooms_by_id.get(rid, {})
                title = room.get("title", "")
                label = f"{rid} â€” {title}" if title else rid
                if filter_text.lower() in label.lower():
                    it = QListWidgetItem(label)
                    it.setData(Qt.UserRole, rid)
                    listw.addItem(it)

        populate()
        search.textChanged.connect(lambda t: populate(t))
        v.addWidget(listw)

        # Controls
        rename_cb = QCheckBox(f"Rename to '{node_id}_N' on adopt")
        rename_cb.setChecked(True)

        h = QHBoxLayout()
        edit_btn = QPushButton("Edit…")
        rename_btn = QPushButton("Rename…")
        delete_btn = QPushButton("Delete Permanently")
        adopt_btn = QPushButton("Adopt Selected â†’")
        h.addWidget(edit_btn); h.addWidget(rename_btn); h.addWidget(delete_btn); h.addStretch(1); h.addWidget(rename_cb); h.addWidget(adopt_btn)
        v.addLayout(h)

        # --- helpers for actions ---
        def selected_ids():
            return [it.data(Qt.UserRole) for it in listw.selectedItems()]

        def do_edit():
            ids = selected_ids()
            if not ids:
                return
            # Edit only the first selected (simple)
            rid = ids[0]
            self._edit_room_dialog(rid)
            # refresh label
            populate(search.text())

        def do_rename():
            ids = selected_ids()
            if not ids:
                return
            if len(ids) > 1:
                QMessageBox.information(dlg, "Rename", "Select a single room to rename.")
                return
            rid = ids[0]
            new_id, ok = QInputDialog.getText(dlg, "Rename Room", f"New id for '{rid}':", text=rid)
            if not ok or not new_id.strip():
                return
            new_id = new_id.strip()
            final = self._rename_room_id(rid, new_id)
            if final != new_id:
                QMessageBox.information(dlg, "Adjusted", f"ID '{new_id}' was in use. Renamed to '{final}'.")
            # refresh orphan list (id may have changed)
            nonlocal orphans
            orphans = orphan_ids()
            populate(search.text())

        def do_delete():
            ids = selected_ids()
            if not ids:
                return
            confirm = QMessageBox.question(dlg, "Delete Permanently",
                                        f"Delete {len(ids)} room(s) from JSON and remove all references?",
                                        QMessageBox.Yes | QMessageBox.No, QMessageBox.No)
            if confirm != QMessageBox.Yes:
                return
            removed_links_total = 0
            for rid in ids:
                removed_links_total += self._delete_room_everywhere(rid, fix_inbound=True, reassign_entry=True)
            QMessageBox.information(dlg, "Deleted",
                                    f"Deleted {len(ids)} room(s). Removed {removed_links_total} inbound link(s).")
            # refresh
            nonlocal orphans
            orphans = orphan_ids()
            populate(search.text())

        def do_adopt():
            ids = selected_ids()
            if not ids:
                return
            node = self.nodes_by_id[node_id]
            key = _node_room_key(node)
            adopted = 0
            for old_id in ids:
                new_id = old_id
                if rename_cb.isChecked():
                    # Use the next sequential ID and global-safe rename
                    new_id, _ = self._next_room_id_pair(node_id)
                    new_id = self._rename_room_id(old_id, new_id)
                node.setdefault(key, [])
                if new_id not in node[key]:
                    node[key].append(new_id)
                adopted += 1
            self._set_dirty(True)
            self._rebuild_tree()
            # refresh orphans (adopted are no longer orphans)
            nonlocal orphans
            orphans = orphan_ids()
            populate(search.text())
            QMessageBox.information(dlg, "Adopted",
                                    f"Assigned {adopted} room(s) to node '{node_id}'.")

        edit_btn.clicked.connect(do_edit)
        rename_btn.clicked.connect(do_rename)
        delete_btn.clicked.connect(do_delete)
        adopt_btn.clicked.connect(do_adopt)

        # Double-click to edit
        def on_dblclick(item: QListWidgetItem):
            rid = item.data(Qt.UserRole)
            self._edit_room_dialog(rid)
            populate(search.text())
        listw.itemDoubleClicked.connect(on_dblclick)

        dlg.exec_()

    # --- Palettes, remap, play, preview, dropping ---
    def _populate_palettes(self):
        self.item_palette.clear(); self.npc_palette.clear(); self.enemy_palette.clear()
        for pid in sorted(self.items_by_id.keys()): self.item_palette.addItem(f"I: {pid}")
        for pid in sorted(self.npcs_by_id.keys()): self.npc_palette.addItem(f"N: {pid}")
        for pid in sorted(self.enemies_by_id.keys()): self.enemy_palette.addItem(f"E: {pid}")

    def _remap_id(self, typ, old, new):
        if not new: return
        if typ == "world":
            if old not in self.worlds_by_id: return
            obj = self.worlds_by_id.pop(old); obj["id"] = new; self.worlds_by_id[new] = obj
            for h in self.hubs_by_id.values():
                if h.get("world_id") == old: h["world_id"] = new
        elif typ == "hub":
            if old not in self.hubs_by_id: return
            obj = self.hubs_by_id.pop(old); obj["id"] = new; self.hubs_by_id[new] = obj
            for n in self.nodes_by_id.values():
                if n.get("hub_id") == old: n["hub_id"] = new
        elif typ == "node":
            if old not in self.nodes_by_id: return
            obj = self.nodes_by_id.pop(old); obj["id"] = new; self.nodes_by_id[new] = obj
        self.current_selection = (typ, new)

    def _play_from_here(self):
        typ, _id = self.current_selection
        if typ != "room":
            QMessageBox.information(self, "Play from Here", "Please select a room to start from.")
            return
        cmd = f'python game.py --start-room {_id}'
        msg = ("This feature launches your Kivy game directly into the selected room for rapid testing.\n\n"
               "To implement this yourself, use Python's `subprocess` module to run the command:\n\n"
               f"{cmd}")
        QMessageBox.information(self, "Play from Here (Placeholder)", msg)

    def _update_kivy_preview(self):
        if not self.desc_tabs.isVisible(): return
        raw_text = self.desc_edit.toPlainText()
        html = raw_text.replace('\n', '<br>')
        html = re.sub(r'\[b\](.*?)\[/b\]', r'<b>\1</b>', html)
        html = re.sub(r'\[i\](.*?)\[/i\]', r'<i>\1</i>', html)
        html = re.sub(r'\[color=([a-fA-F0-9]{6})\](.*?)\[/color\]', r"<span style='color: #\1;'>\2</span>", html)
        html = re.sub(r'\[ref=.*?\](.*?)\[/ref\]', r"<span style='color: #1d4ed8; text-decoration: underline;'>\1</span>", html)
        self.desc_preview.setHtml(f"<html><body style='color: #111827;'>{html}</body></html>")
        self.desc_preview.append("\n\n<hr><i style='color: #666'>This is a simplified HTML preview of Kivy markup. Actual in-game rendering may differ.</i>")

    def _on_item_dropped_on_room(self, room_id, dropped_data):
        prefix, _, item_id = dropped_data.partition(': ')
        room = self.rooms_by_id.get(room_id)
        if not room:
            return

        if prefix == "I":
            room.setdefault('items', [])
            if item_id not in room['items']:
                room['items'].append(item_id)
                room['items'].sort()

        elif prefix == "N":
            room.setdefault('npcs', [])
            if item_id not in room['npcs']:
                room['npcs'].append(item_id)
                room['npcs'].sort()

        elif prefix == "E":
            room.setdefault('enemies', [])
            # enemies list can be strings or dicts; check both forms
            existing = [e if isinstance(e, str) else e.get('id') for e in room['enemies']]
            if item_id not in existing:
                room['enemies'].append(item_id)

        self._set_dirty(True)
        self._select_in_tree("room", room_id)  # refresh inspector
        self.canvas.update()

    def _toggle_connect_mode(self, checked):
        if checked: self.add_room_btn.setChecked(False)
        self.canvas.set_connection_mode(checked)

    def _toggle_quick_mode(self, checked):
        if checked:
            self.connect_btn.setChecked(False)
            self.add_room_btn.setChecked(False)
        self.canvas.set_quick_mode(checked)

    def _toggle_add_mode(self, checked):
        if checked:
            self.connect_btn.setChecked(False)
            self.quick_btn.setChecked(False)
        self.canvas.set_add_mode(checked)

    def _on_node_title_gap_changed(self, value: int):
        if not self.current_selection or self.current_selection[0] != "node":
            return
        node_id = self.current_selection[1]
        node = self.nodes_by_id.get(node_id)
        if not node:
            return
        node["title_gap"] = int(value)
        self._set_dirty(True)
        self.canvas.update()

    def _on_node_geom_changed(self):
        if self.current_selection[0] == 'node': self._sync_node_inspector()
        self._set_dirty(True)

    def _on_room_dragged(self, rid, x, y):
        if rid in self.rooms_by_id: self.rooms_by_id[rid]['pos'] = [x, y]
        if self.current_selection == ('room', rid): self._sync_room_inspector()
        self._set_dirty(True)

    def get_node_hover_info(self, node_id):
        node = self.nodes_by_id.get(node_id, {})
        key = _node_room_key(node); room_ids = set(node.get(key, []))
        num_rooms = len(room_ids)
        num_quests = self._estimate_quests_for_rooms(room_ids)
        title = node.get('title') or node.get('name') or node_id
        return f"<b>{title}</b><br>{num_rooms} Rooms<br>{num_quests} Quests"

    def _load_quests_events_if_needed(self):
        if self._quests_cache is None:
            self._quests_cache = {}
            try:
                qp = os.path.join(self.root, 'quests.json')
                if os.path.exists(qp) and os.path.getsize(qp) > 0:
                    with open(qp, 'r', encoding='utf-8') as f:
                        data = json.load(f)
                    if isinstance(data, dict):
                        self._quests_cache = {k: v for k, v in data.items() if isinstance(v, dict)}
                    elif isinstance(data, list):
                        for q in data:
                            if isinstance(q, dict) and q.get('id'):
                                self._quests_cache[q['id']] = q
            except Exception:
                self._quests_cache = {}

        if self._events_cache is None:
            self._events_cache = []
            try:
                ep = os.path.join(self.root, 'events.json')
                if os.path.exists(ep) and os.path.getsize(ep) > 0:
                    with open(ep, 'r', encoding='utf-8') as f:
                        data = json.load(f)
                    if isinstance(data, list):
                        self._events_cache = [e for e in data if isinstance(e, dict)]
            except Exception:
                self._events_cache = []

    def _estimate_quests_for_rooms(self, room_ids: Set[str]) -> int:
        """Heuristic quest count for a set of rooms.

        Prefer quests.json if present; fall back to events.json by counting event
        triggers tied to these rooms (e.g., enter_room).
        """
        self._load_quests_events_if_needed()
        ridset = set(room_ids)

        # First try quests.json with a light heuristic
        qcount = 0
        try:
            for q in (self._quests_cache or {}).values():
                if not isinstance(q, dict):
                    continue
                found = False
                for k in ("start_room", "target_room", "room", "rooms"):
                    v = q.get(k)
                    if isinstance(v, str) and v in ridset:
                        found = True; break
                    if isinstance(v, list) and any(isinstance(x, str) and x in ridset for x in v):
                        found = True; break
                if not found:
                    for k in ("steps", "tasks", "objectives"):
                        steps = q.get(k, [])
                        if isinstance(steps, list):
                            for s in steps:
                                if isinstance(s, dict):
                                    rv = s.get("room") or s.get("target_room")
                                    if (isinstance(rv, str) and rv in ridset) or (
                                        isinstance(rv, list) and any(isinstance(x, str) and x in ridset for x in rv)
                                    ):
                                        found = True; break
                            if found:
                                break
                if found:
                    qcount += 1
        except Exception:
            qcount = 0

        # Fallback to events.json when no quests are defined
        if qcount == 0:
            try:
                for e in (self._events_cache or []):
                    trig = e.get('trigger', {}) if isinstance(e, dict) else {}
                    room = None
                    if isinstance(trig, dict):
                        room = trig.get('room') or trig.get('target_room')
                    if isinstance(room, str) and room in ridset:
                        qcount += 1
            except Exception:
                pass

        return qcount

    def _create_connection_dialog(self, start_id, end_id):
        start_title = self.rooms_by_id.get(start_id,{}).get('title', start_id)
        end_title = self.rooms_by_id.get(end_id,{}).get('title', end_id)
        directions = ["north", "south", "east", "west", "up", "down"]
        forward_dir, ok1 = QInputDialog.getItem(self, "Create Connection", f"Direction from '{start_title}' to '{end_title}':", directions, 0, False)
        if not ok1: return
        return_dir, ok2 = QInputDialog.getItem(self, "Create Return Connection", f"Return direction from '{end_title}' to '{start_title}':\n(Cancel for one-way)", directions, 0, False)
        self.rooms_by_id[start_id].setdefault("connections", {})[forward_dir] = end_id
        if ok2 and return_dir: self.rooms_by_id[end_id].setdefault("connections", {})[return_dir] = start_id
        if self.current_selection[1] in [start_id, end_id]: self._sync_room_inspector()
        self._set_dirty(True)
        self.canvas.update()

    def _browse_for_image(self, line_edit_widget):
        # Start in last-used image directory if available, else project root
        start_dir = self._last_image_dir if (hasattr(self, "_last_image_dir") and os.path.isdir(self._last_image_dir)) else self.root
        path, _ = QFileDialog.getOpenFileName(
            self,
            "Select Image",
            str(start_dir),
            "Images (*.png *.jpg *.jpeg *.gif *.webp *.bmp)"
        )
        if path:
            # Remember this folder for next time (and next session)
            try:
                self._last_image_dir = os.path.dirname(path)
                if getattr(self, "_settings", None):
                    self._settings.setValue("last_image_dir", self._last_image_dir)
            except Exception:
                pass

            # Always prefer storing paths relative to the project root when possible
            try:
                rel_path = os.path.relpath(path, self.root).replace("\\", "/")
                line_edit_widget.setText(rel_path)
            except ValueError:
                # Different drive on Windows; fall back to absolute
                line_edit_widget.setText(path)

    def _save_room_as_template(self, room_id):
        name, ok = QInputDialog.getText(self, "Save Template", "Enter template name:")
        if not ok or not name: return
        template_data = copy.deepcopy(self.rooms_by_id[room_id])
        for key in ["id", "title", "pos", "connections"]:
            if key in template_data: del template_data[key]
        self.room_templates[name] = template_data
        self._set_dirty(True)
        QMessageBox.information(self, "Success", f"Template '{name}' saved. It will be stored when you Save All.")

    def _create_room_from_template(self, template_name, wx, wy):
        template = self.room_templates[template_name]
        typ, node_id = self.current_selection
        if typ == "room":
            node_id = self._find_parent_node_id_for_room(self.current_selection[1])
        self._auto_create_room_at(node_id, wx, wy, template=template)

    # --- Items/NPCs pickers & connection picker ---
    def _pick_items(self):
        it = self.rooms_by_id.get(self.current_selection[1], {})
        options = sorted(self.items_by_id.keys())
        current = [s.strip() for s in self.r_items.text().split(",") if s.strip()]
        dlg = MultiSelectDialog("Pick Items", options, current, self)
        if dlg.exec_():
            items = dlg.result_list()
            self.r_items.setText(", ".join(items))
            it["items"] = items
            self._set_dirty(True)
            self.canvas.update()

    def _pick_npcs(self):
        it = self.rooms_by_id.get(self.current_selection[1], {})
        options = sorted(self.npcs_by_id.keys())
        current = [s.strip() for s in self.r_npcs.text().split(",") if s.strip()]
        dlg = MultiSelectDialog("Pick NPCs", options, current, self)
        if dlg.exec_():
            npcs = dlg.result_list()
            self.r_npcs.setText(", ".join(npcs))
            it["npcs"] = npcs
            self._set_dirty(True)
            self.canvas.update()

    def _pick_connection(self, line_edit: QLineEdit):
        # rooms only from the same node
        rid = self.current_selection[1]
        parent_node_id = None
        for nid, n in self.nodes_by_id.items():
            key = _node_room_key(n)
            if rid in n.get(key, []):
                parent_node_id = nid; break
        options = []
        if parent_node_id:
            node = self.nodes_by_id[parent_node_id]
            options = [r for r in node.get(_node_room_key(node), []) if r != rid]
        if not options:
            QMessageBox.information(self, "No rooms", "No other rooms in this node.")
            return
        choice, ok = QInputDialog.getItem(self, "Pick room", "Connect to:", sorted(options), 0, False)
        if ok:
            line_edit.setText(choice)
            self._apply_room_inspector_changes()


    # --- Link Doctor / Repairs ---
    def _opposite_dir(self, d: str) -> str:
        d = (d or "").lower()
        table = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "down", "down": "up"}
        return table.get(d, "")

    def _scan_graph_issues(self):
        """
        Returns a dict of issues:
          - dangling_connections: [(src, dir, dst)]
          - one_way_connections: [(a, dir, b)]  # missing reciprocal
          - mismatched_return: [(a, dir, b, bdir_actual)]  # reciprocal points wrong way
          - bad_entry_rooms: [node_id]
          - orphan_rooms: [room_id]
        """
        issues = {"dangling_connections": [], "one_way_connections": [], "mismatched_return": [],
                  "bad_entry_rooms": [], "orphan_rooms": []}

        # quick lookup for room -> node membership
        room_to_node = {}
        for nid, n in self.nodes_by_id.items():
            key = _node_room_key(n)
            for rid in n.get(key, []):
                room_to_node[rid] = nid

        # rooms: dangling & one-way/mismatched
        for aid, a in self.rooms_by_id.items():
            conns = (a.get("connections") or {})
            for d, bid in list(conns.items()):
                if bid not in self.rooms_by_id:
                    issues["dangling_connections"].append((aid, d, bid))
                    continue
                b = self.rooms_by_id[bid]
                b_conns = (b.get("connections") or {})
                od = self._opposite_dir(d)
                if not od:
                    continue
                back = b_conns.get(od)
                if back is None:
                    # missing reciprocal
                    issues["one_way_connections"].append((aid, d, bid))
                elif back != aid:
                    # present but pointing elsewhere
                    issues["mismatched_return"].append((aid, d, bid, od))

        # nodes: bad entry rooms
        for nid, n in self.nodes_by_id.items():
            key = _node_room_key(n)
            er = n.get("entry_room")
            if er and er not in n.get(key, []):
                issues["bad_entry_rooms"].append(nid)

        # orphan rooms: exist in rooms.json but not in any node
        all_node_rooms = set(rid for n in self.nodes_by_id.values() for rid in n.get(_node_room_key(n), []))
        for rid in self.rooms_by_id.keys():
            if rid not in all_node_rooms:
                issues["orphan_rooms"].append(rid)

        return issues

    def _apply_repairs(self, remove_dangling=True, fix_one_way=True, fix_mismatched=True, fix_entry_rooms=True):
        """
        Applies selected repairs, returns a summary dict of counts changed.
        """
        summary = {"removed_links": 0, "added_links": 0, "fixed_entries": 0}
        issues = self._scan_graph_issues()

        # Remove any links pointing to missing rooms
        if remove_dangling:
            for aid, d, bid in issues["dangling_connections"]:
                a = self.rooms_by_id.get(aid)
                if a and "connections" in a and d in a["connections"] and a["connections"][d] == bid:
                    del a["connections"][d]
                    if not a["connections"]:
                        del a["connections"]
                    summary["removed_links"] += 1

        # Fix one-way (add reciprocal) where both rooms exist
        if fix_one_way:
            for aid, d, bid in issues["one_way_connections"]:
                a = self.rooms_by_id.get(aid); b = self.rooms_by_id.get(bid)
                if not a or not b: 
                    continue
                od = self._opposite_dir(d)
                if not od: 
                    continue
                b.setdefault("connections", {})
                # Only add if the slot is free
                if od not in b["connections"]:
                    b["connections"][od] = aid
                    summary["added_links"] += 1

        # Fix mismatched return (force proper opposite to point back)
        if fix_mismatched:
            for aid, d, bid, od in issues["mismatched_return"]:
                b = self.rooms_by_id.get(bid)
                if not b:
                    continue
                b.setdefault("connections", {})
                b["connections"][od] = aid
                summary["added_links"] += 1

        # Fix bad entry rooms (set to first available, else clear)
        if fix_entry_rooms:
            for nid in issues["bad_entry_rooms"]:
                n = self.nodes_by_id.get(nid)
                if not n:
                    continue
                key = _node_room_key(n)
                rooms = n.get(key, [])
                if rooms:
                    n["entry_room"] = rooms[0]
                else:
                    n["entry_room"] = ""
                summary["fixed_entries"] += 1

        self._set_dirty(True)
        self.canvas.update()
        return summary

    def _repair_links_dialog(self):
        # Build a small dialog with options and live counts.
        dlg = QDialog(self)
        dlg.setWindowTitle("Link Doctor")
        v = QVBoxLayout(dlg)
        info = QLabel("Choose what to repair. A scan runs automatically and updates counts.")
        v.addWidget(info)

        # Options
        opt_remove = QCheckBox("Remove connections that point to missing rooms")
        opt_oneway = QCheckBox("Auto-add missing reciprocal links (N/S/E/W)")
        opt_mismatch = QCheckBox("Correct mismatched return directions")
        opt_entry = QCheckBox("Fix nodes whose entry_room isn't in their room list")
        for cb in (opt_remove, opt_oneway, opt_mismatch, opt_entry):
            cb.setChecked(True); v.addWidget(cb)

        # Counts
        counts = QLabel("Scanning…")
        v.addWidget(counts)

        def refresh_counts():
            iss = self._scan_graph_issues()
            msg = (f"Dangling connections: <b>{len(iss['dangling_connections'])}</b><br>"
                   f"One-way links: <b>{len(iss['one_way_connections'])}</b><br>"
                   f"Mismatched returns: <b>{len(iss['mismatched_return'])}</b><br>"
                   f"Bad entry rooms: <b>{len(iss['bad_entry_rooms'])}</b><br>"
                   f"Orphan rooms (FYI): <b>{len(iss['orphan_rooms'])}</b>")
            counts.setText(msg)

        refresh_counts()

        btns = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        v.addWidget(btns)
        btns.accepted.connect(dlg.accept)
        btns.rejected.connect(dlg.reject)

        if dlg.exec_():
            summary = self._apply_repairs(remove_dangling=opt_remove.isChecked(),
                                          fix_one_way=opt_oneway.isChecked(),
                                          fix_mismatched=opt_mismatch.isChecked(),
                                          fix_entry_rooms=opt_entry.isChecked())
            QMessageBox.information(self, "Link Doctor",
                                    f"Removed links: {summary['removed_links']}\n"
                                    f"Added/updated links: {summary['added_links']}\n"
                                    f"Fixed entry rooms: {summary['fixed_entries']}")

    def _delete_inbound_links_to_room(self, rid: str) -> int:
        """Remove any connections in any room that point to rid. Returns count of removals."""
        removed = 0
        for sid, s in self.rooms_by_id.items():
            conns = s.get("connections") or {}
            to_del = [d for d, dst in conns.items() if dst == rid]
            for d in to_del:
                del conns[d]
                removed += 1
            if conns:
                s["connections"] = conns
            elif "connections" in s:
                del s["connections"]
        if removed:
            self._set_dirty(True)
            self.canvas.update()
        return removed

    def _ask_prune_after_room_delete(self, rid: str):
        # Count inbound links to rid
        inbound = []
        for sid, s in self.rooms_by_id.items():
            for d, dst in (s.get("connections") or {}).items():
                if dst == rid:
                    inbound.append((sid, d))
        if not inbound:
            return
        reply = QMessageBox.question(self, "Remove Links?",
                                     f"{len(inbound)} connection(s) in other rooms lead to '{rid}'.\\n\\n"
                                     "Do you want to remove those links now?",
                                     QMessageBox.Yes | QMessageBox.No, QMessageBox.Yes)
        if reply == QMessageBox.Yes:
            n = self._delete_inbound_links_to_room(rid)
            QMessageBox.information(self, "Pruned", f"Removed {n} inbound link(s) to '{rid}'.")
    # --- Validation ---
    def _validate_all(self):
        issues = []

        # Worlds
        for wid, w in self.worlds_by_id.items():
            if not w.get("id"): issues.append(f"world: missing id")
            if not w.get("title"): issues.append(f"{wid}: missing title")

        # Hubs
        for hid, h in self.hubs_by_id.items():
            if h.get("world_id") not in self.worlds_by_id: issues.append(f"{hid}: world_id '{h.get('world_id')}' not found")
            bg = h.get("background_image","")
            if bg and not os.path.exists(os.path.join(self.root, bg)):
                issues.append(f"{hid}: background image not found: {bg}")

        # Nodes
        for nid, n in self.nodes_by_id.items():
            if n.get("hub_id") not in self.hubs_by_id: issues.append(f"{nid}: hub_id '{n.get('hub_id')}' not found")
            ph = n.get("pos_hint", {})
            cx, cy = float(ph.get("center_x", 0.5)), float(ph.get("center_y", 0.5))
            if not (0.0 <= cx <= 1.0) or not (0.0 <= cy <= 1.0): issues.append(f"{nid}: pos_hint out of range [0,1]")
            key = _node_room_key(n)
            for rid in n.get(key, []):
                if rid not in self.rooms_by_id: issues.append(f"{nid}: references missing room '{rid}'")
            er = n.get("entry_room")
            if er and er not in n.get(key, []): issues.append(f"{nid}: entry_room '{er}' is not in node rooms list")

        # Rooms
        all_node_rooms = set(rid for n in self.nodes_by_id.values() for rid in n.get(_node_room_key(n), []))
        for rid, r in self.rooms_by_id.items():
            pos = r.get("pos", [0,0])
            if not (isinstance(pos, list) and len(pos)==2): issues.append(f"{rid}: pos must be [x,y]")
            bg = r.get("background_image","")
            if bg and not os.path.exists(os.path.join(self.root, bg)):
                issues.append(f"{rid}: background image not found: {bg}")
            # connections
            for d, dst in (r.get("connections") or {}).items():
                if dst not in self.rooms_by_id: issues.append(f"{rid}: connection '{d}' -> '{dst}' missing")
            # items/npcs
            for it in r.get("items", []):
                if self.items_by_id and it not in self.items_by_id: issues.append(f"{rid}: item '{it}' unknown")
            for npc in r.get("npcs", []):
                if self.npcs_by_id and npc not in self.npcs_by_id: issues.append(f"{rid}: npc '{npc}' unknown")
            # enemies
            for e in r.get("enemies", []):
                eid = e if isinstance(e, str) else e.get("id")
                if eid and self.enemies_by_id and eid not in self.enemies_by_id:
                    issues.append(f"{rid}: enemy '{eid}' unknown")
            # actions â€” validate simple shop link if present
            for a in r.get("actions", []):
                if isinstance(a, dict) and a.get("type") == "shop":
                    sid = a.get("shop_id")
                    if sid and self.shops_by_id and sid not in self.shops_by_id:
                        issues.append(f"{rid}: action.shop_id '{sid}' not found in shops.json")
            # orphan check
            if rid not in all_node_rooms:
                issues.append(f"{rid}: room is not referenced by any node")


        # Extra graph checks: one-way / mismatched returns
        _iss = self._scan_graph_issues()
        for (a, d, b) in _iss["one_way_connections"]:
            issues.append(f"{a}: connection '{d}' -> '{b}' has no reciprocal (expected '{self._opposite_dir(d)}')")
        for (a, d, b, od) in _iss["mismatched_return"]:
            issues.append(f"{a}: connection '{d}' -> '{b}' has wrong/mismatched return on '{od}'")

        if issues:
            QMessageBox.warning(self, "Validation", "\n".join(issues[:400]))
        else:
            QMessageBox.information(self, "Validation", "No issues found.")

    # --- Event filter (bg preview) ---
    def eventFilter(self, obj, event):
        if obj is self.r_bg:
            if event.type() in (QEvent.Enter, QEvent.MouseMove):
                path = os.path.join(self.root, self.r_bg.text())
                if os.path.exists(path):
                    self.bg_preview.set_pixmap(QPixmap(path)); self.bg_preview.show_near(event.globalPos())
                else: self.bg_preview.hide()
            elif event.type() in (QEvent.Leave, QEvent.FocusOut):
                self.bg_preview.hide()
        return super().eventFilter(obj, event)

    # --- Export snapshot ---
    def export_snapshot(self, ids: Set[str]):
        # Grab canvas as pixmap (works in both modes)
        pm = self.canvas.grab()
        path, _ = QFileDialog.getSaveFileName(self, "Save Snapshot", "world_snapshot.png", "PNG Image (*.png)")
        if not path: return
        if not pm.save(path, "PNG"):
            QMessageBox.critical(self, "Export", "Failed to save snapshot.")
        else:
            QMessageBox.information(self, "Export", f"Snapshot saved:\n{path}")

    def _open_templates_dialog(self):
        """Templates Manager: create/edit/delete, apply/stamp to current selection."""
        dlg = QDialog(self); dlg.setWindowTitle("Templates")
        v = QVBoxLayout(dlg)

        # Top row: actions
        top = QHBoxLayout()
        new_btn = QPushButton("New…"); save_btn = QPushButton("Save Selected Room as Template…")
        del_btn = QPushButton("Delete Template"); apply_btn = QPushButton("Apply (Link)"); stamp_btn = QPushButton("Stamp (Copy)")
        top.addWidget(new_btn); top.addWidget(save_btn); top.addStretch(1); top.addWidget(apply_btn); top.addWidget(stamp_btn); top.addWidget(del_btn)
        v.addLayout(top)

        # List + details
        search = QLineEdit(); search.setPlaceholderText("Filter templates…")
        v.addWidget(search)
        listw = QListWidget(); v.addWidget(listw)

        def refresh_list(filter_text=""):
            listw.clear()
            for tid, t in sorted((self.templates_by_id or {}).items()):
                label = f"{tid} â€” {t.get('title','')}"
                if filter_text.lower() in label.lower():
                    it = QListWidgetItem(label); it.setData(Qt.UserRole, tid); listw.addItem(it)

        refresh_list()
        search.textChanged.connect(lambda t: refresh_list(t))

        # Button handlers
        def get_current_tpl_id():
            it = listw.currentItem()
            return it.data(Qt.UserRole) if it else None

        def on_new():
            tid, ok = QInputDialog.getText(dlg, "New Template", "Template id (e.g. tpl_newroom):")
            if not ok or not tid.strip(): return
            tid = tid.strip()
            if tid in self.templates_by_id:
                QMessageBox.warning(dlg, "Templates", "ID already exists."); return
            self.templates_by_id[tid] = {"id": tid, "title": tid, "defaults": {}, "sockets": ["north","south","east","west"], "tags":[]}
            self._save_templates(); refresh_list(search.text())

        def on_delete():
            tid = get_current_tpl_id()
            if not tid: return
            if QMessageBox.question(dlg, "Delete Template", f"Delete template '{tid}'?", QMessageBox.Yes|QMessageBox.No, QMessageBox.No) != QMessageBox.Yes:
                return
            self.templates_by_id.pop(tid, None)
            self._save_templates(); refresh_list(search.text())

        def on_save_selected_room():
            sel = self.current_selection or (None, None)
            typ, rid = sel
            if typ != "room" or not rid:
                QMessageBox.information(dlg, "Templates", "Select a room in the editor first."); return
            tid, ok = QInputDialog.getText(dlg, "Save as Template", "Template id:", text=f"tpl_{rid}")
            if not ok or not tid.strip(): return
            self._save_room_as_template(rid, tid.strip())
            refresh_list(search.text())

        def on_apply():
            tid = get_current_tpl_id()
            sel = self.current_selection or (None, None)
            typ, rid = sel
            if not tid:
                QMessageBox.information(dlg, "Templates", "Select a template first."); return
            if typ != "room" or not rid:
                QMessageBox.information(dlg, "Templates", "Select a target room in the editor (Room Inspector)."); return
            tpl = self.templates_by_id.get(tid)
            if not tpl:
                return
            # Link mode: keep template reference with args
            args = {}  # could prompt for args later
            self._apply_template_defaults(self.rooms_by_id[rid], tpl, args=args, mode="link")
            self._set_dirty(True); self.canvas.update()
            QMessageBox.information(dlg, "Templates", f"Linked '{rid}' to template '{tid}'.")

        def on_stamp():
            tid = get_current_tpl_id()
            sel = self.current_selection or (None, None)
            if not tid:
                QMessageBox.information(dlg, "Templates", "Select a template first."); return
            typ, node_or_room = sel
            # Stamp into current node if node selected; if room selected, stamp into its parent node (if known)
            if typ == "node":
                node_id = node_or_room
            elif typ == "room":
                node_id = self._find_parent_node_id_for_room(node_or_room) or ""
            else:
                node_id = ""
            if not node_id:
                QMessageBox.information(dlg, "Templates", "Select a Node (or a Room within a Node) to stamp into."); return
            new_id = self._stamp_template_as_new_room(node_id, tid)
            QMessageBox.information(dlg, "Stamped", f"Created room '{new_id}' from template '{tid}'.")

        new_btn.clicked.connect(on_new)
        del_btn.clicked.connect(on_delete)
        save_btn.clicked.connect(on_save_selected_room)
        apply_btn.clicked.connect(on_apply)
        stamp_btn.clicked.connect(on_stamp)

        dlg.resize(700, 500)
        dlg.exec_()

# --------------------------------------------------------
# Drag source list
# --------------------------------------------------------
class DraggableListWidget(QListWidget):
    def __init__(self, item_type_prefix: str, parent=None):
        super().__init__(parent)
        self.item_type_prefix = item_type_prefix
        self.setDragEnabled(True)

    def startDrag(self, supportedActions):
        item = self.currentItem()
        if item:
            mime_data = QMimeData()
            mime_data.setText(item.text())
            drag = QDrag(self); drag.setMimeData(mime_data)
            drag.exec_(Qt.CopyAction)

# --------------------------------------------------------
# Entry Point
# --------------------------------------------------------
if __name__ == "__main__":
    app = QApplication(sys.argv)
    PROJECT_ROOT = _detect_project_root()
    editor = WorldBuilder(PROJECT_ROOT)
    editor.show()
    sys.exit(app.exec_())