#!/usr/bin/env python3
"""
Starborn NPC Editor -- with NPC-scoped Dialogue Editor
=====================================================
What's new in this build:
- Dialogue tab ALWAYS filters to the currently selected NPC (by Speaker).
- Adding dialogue pre-fills Speaker with the current NPC, and suggests ids like "<npc>_1".
- Renaming an NPC updates 'speaker' in dialogue.json for that NPC's lines.
- Keeps previous features: interactions editing, placement, validation,
  dialogue id rename + propagation to NPC interactions, assign-to-row, etc.
"""

import sys, os, json
from pathlib import Path
from copy import deepcopy
from typing import List

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (
    QApplication, QWidget, QVBoxLayout, QHBoxLayout, QListWidget, QListWidgetItem,
    QLineEdit, QPushButton, QLabel, QFormLayout, QTextEdit, QMessageBox,
    QInputDialog, QSplitter, QTabWidget, QTableWidget, QTableWidgetItem, QComboBox,
    QAbstractItemView, QTreeWidget, QTreeWidgetItem, QHeaderView
)
from theme_kit import ThemeManager         # optional if you want per-editor theme flips
from devkit_paths import resolve_paths
from editor_undo import UndoManager
from ui_common import attach_status_bar, flash_status, attach_hotkeys, attach_list_context_menu, mark_invalid, clear_invalid
from editor_bus import goto as studio_goto, refresh_references as studio_refresh

# ------------------------
# Helpers / constants
# ------------------------

INTERACTION_TYPES = ["talk", "shop", "event", "cinematic", "custom"]

def _load_json_list(path):
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    if isinstance(data, list):
        return data
    raise ValueError(f"{os.path.basename(path)} must be a JSON list.")

def _save_json_list(path, data_list):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data_list, f, indent=4)

def _index_by_id(lst):
    return {d["id"]: d for d in lst if isinstance(d, dict) and "id" in d}

def _as_list(value):
    if value is None: return []
    return value if isinstance(value, list) else [value]

def _slug(s: str) -> str:
    return "".join(ch.lower() if ch.isalnum() else "_" for ch in (s or "")).strip("_")


# ==========================================================
# Main Editor
# ==========================================================

class NPCEditor(QWidget):
    def __init__(self, root_dir=None):
        super().__init__()
        paths = resolve_paths(Path(root_dir) if root_dir else Path(__file__).parent)
        self.project_root = paths.project_root
        self.root = paths.assets_dir
        self.setWindowTitle("Starborn NPC Editor")
        self.resize(1280, 760)

        # Data
        self.npcs = {}              # key: lower(name) -> npc dict
        self.current_key = None
        self.rooms = []             # rooms.json list
        self._rooms_dirty = False

        # Dialogue data
        self.dialogue_index = {}    # id -> entry
        self.current_dlg_id = None  # currently selected dialogue ID in Dialogue tab
        self.shops = {}             # id -> shop definition
        self.shop_ids = []          # cached list of shop ids

        self._load_all()
        self._init_ui()

    # ---------- paths ----------
    @property
    def npcs_path(self):     return os.path.join(self.root, "npcs.json")
    @property
    def rooms_path(self):    return os.path.join(self.root, "rooms.json")
    @property
    def dialogue_path(self): return os.path.join(self.root, "dialogue.json")
    @property
    def shops_path(self): return os.path.join(self.root, "shops.json")

    # ---------- IO ----------
    def _load_all(self):
        # NPCs
        try:
            raw = _load_json_list(self.npcs_path)
            self.npcs = {n["name"].lower(): n for n in raw if isinstance(n, dict) and "name" in n}
        except Exception as e:
            QMessageBox.critical(self, "Load Error", f"Failed to load npcs.json:\n{e}")
            self.npcs = {}

        # Rooms
        try:
            self.rooms = _load_json_list(self.rooms_path)
        except Exception:
            self.rooms = []

        # Dialogue
        self._reload_dialogue()

        # Shops
        self._load_shops()

    def _reload_dialogue(self):
        try:
            dlg_list = _load_json_list(self.dialogue_path)
            self.dialogue_index = _index_by_id(dlg_list)
        except Exception:
            self.dialogue_index = {}

    def _load_shops(self):
        try:
            with open(self.shops_path, "r", encoding="utf-8") as f:
                data = json.load(f)
            if isinstance(data, dict):
                self.shops = data
                self.shop_ids = sorted(data.keys())
            else:
                self.shops = {}
                self.shop_ids = []
        except Exception:
            self.shops = {}
            self.shop_ids = []

    def _save_npcs(self):
        try:
            _save_json_list(self.npcs_path, list(self.npcs.values()))
            return True
        except Exception as e:
            QMessageBox.critical(self, "Save Error", f"Failed to save npcs.json:\n{e}")
            return False

    def _save_rooms_if_dirty(self):
        if not self._rooms_dirty:
            return True
        try:
            _save_json_list(self.rooms_path, self.rooms)
            self._rooms_dirty = False
            return True
        except Exception as e:
            QMessageBox.critical(self, "Save Error", f"Failed to save rooms.json:\n{e}")
            return False

    def _save_dialogue(self):
        try:
            out = [self.dialogue_index[k] for k in sorted(self.dialogue_index.keys())]
            _save_json_list(self.dialogue_path, out)
            return True
        except Exception as e:
            QMessageBox.critical(self, "Save Error", f"Failed to save dialogue.json:\n{e}")
            return False

    # ---------- UI ----------
    def _init_ui(self):
        outer = QHBoxLayout()
        self.setLayout(outer)
        split = QSplitter(Qt.Horizontal)
        outer.addWidget(split, 1)

        # LEFT PANE: NPC list + search + buttons
        leftw = QWidget(); left = QVBoxLayout(leftw)

        self.search = QLineEdit()
        self.search.setPlaceholderText("Search NPCs by name or alias...")
        self.search.textChanged.connect(self._refresh_npc_list)
        left.addWidget(self.search)

        self.npc_list = QListWidget()
        self.npc_list.itemSelectionChanged.connect(self._on_select_npc)
        self.npc_list.setSelectionMode(QAbstractItemView.SingleSelection)
        left.addWidget(self.npc_list, 1)

        row = QHBoxLayout()
        b_add = QPushButton("Add");       b_add.clicked.connect(self._on_add_npc);        row.addWidget(b_add)
        b_dup = QPushButton("Duplicate"); b_dup.clicked.connect(self._on_duplicate_npc);  row.addWidget(b_dup)
        b_del = QPushButton("Delete");    b_del.clicked.connect(self._on_delete_npc);     row.addWidget(b_del)
        left.addLayout(row)

        split.addWidget(leftw)

        # RIGHT PANE: tabs
        self.tabs = QTabWidget()
        split.addWidget(self.tabs)
        split.setStretchFactor(1, 3)

        self._build_tab_basics()
        self._build_tab_interactions()
        self._build_tab_placement()
        self._build_tab_validation()
        self._build_tab_dialogue()     # NPC-scoped Dialogue editor

        self._refresh_npc_list()

    # ---------- Tabs ----------
    def _build_tab_basics(self):
        self.tab_basics = QWidget()
        form = QFormLayout(self.tab_basics)

        self.name_edit = QLineEdit()
        form.addRow("Name:", self.name_edit)

        self.aliases_edit = QLineEdit()
        self.aliases_edit.setPlaceholderText("Comma-separated")
        form.addRow("Aliases:", self.aliases_edit)

        self.portrait_edit = QLineEdit()
        self.portrait_edit.setPlaceholderText("e.g., images/npcs/ollie.png or ollie_portrait")
        form.addRow("Portrait:", self.portrait_edit)

        self.emotes_blob = QTextEdit()
        self.emotes_blob.setPlaceholderText('{"angry": "ollie_emote_angry", "sad": "ollie_emote_sad"}')
        form.addRow("Emotes (JSON map):", self.emotes_blob)

        self.dialogue_blob = QTextEdit()
        self.dialogue_blob.setPlaceholderText("NPC-local Dialogue (legacy; not used by engine).")
        form.addRow("NPC-local Dialogue (legacy; not used by engine):", self.dialogue_blob)

        b_save = QPushButton("Save NPC")
        b_save.clicked.connect(self._on_save_clicked)
        form.addRow(b_save)

        self.tabs.addTab(self.tab_basics, "Basics")

    def _build_tab_interactions(self):
        self.tab_inter = QWidget()
        v = QVBoxLayout(self.tab_inter)

        self.inter_table = QTableWidget(0, 5)
        self.inter_table.setHorizontalHeaderLabels(["Label", "Type", "Value", "Extra JSON", ""])
        self.inter_table.horizontalHeader().setSectionResizeMode(0, QHeaderView.Stretch)
        self.inter_table.horizontalHeader().setSectionResizeMode(1, QHeaderView.ResizeToContents)
        self.inter_table.horizontalHeader().setSectionResizeMode(2, QHeaderView.Stretch)
        self.inter_table.horizontalHeader().setSectionResizeMode(3, QHeaderView.Stretch)
        self.inter_table.horizontalHeader().setSectionResizeMode(4, QHeaderView.ResizeToContents)
        self.inter_table.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.inter_table.setEditTriggers(QAbstractItemView.DoubleClicked | QAbstractItemView.SelectedClicked | QAbstractItemView.EditKeyPressed)
        v.addWidget(self.inter_table, 1)

        row = QHBoxLayout()
        b_add = QPushButton("Add Row"); b_add.clicked.connect(self._add_interaction_row); row.addWidget(b_add)
        b_rem = QPushButton("Remove Row"); b_rem.clicked.connect(self._remove_interaction_row); row.addWidget(b_rem)
        b_up  = QPushButton("Move Up"); b_up.clicked.connect(lambda: self._move_interaction(-1)); row.addWidget(b_up)
        b_dn  = QPushButton("Move Down"); b_dn.clicked.connect(lambda: self._move_interaction(1)); row.addWidget(b_dn)
        b_pick= QPushButton("Pick Dialogue..."); b_pick.clicked.connect(self._pick_dialogue_for_selected_row); row.addWidget(b_pick)
        v.addLayout(row)

        quick = QHBoxLayout()
        b_add_talk = QPushButton("Add Talk Line"); b_add_talk.clicked.connect(self._add_talk_with_dialogue); quick.addWidget(b_add_talk)
        b_add_shop = QPushButton("Add Shop Row"); b_add_shop.clicked.connect(self._add_shop_row); quick.addWidget(b_add_shop)
        b_reload_shop = QPushButton("Reload Shops"); b_reload_shop.clicked.connect(self._on_reload_shops_clicked); quick.addWidget(b_reload_shop)
        quick.addStretch(1)
        v.addLayout(quick)

        # Linked dialogue preview
        self.dlg_preview = QTextEdit(); self.dlg_preview.setReadOnly(True)
        self.dlg_preview.setPlaceholderText("Select a 'talk' row to preview its dialogue from dialogue.json.")
        v.addWidget(self.dlg_preview, 1)

        self.inter_table.currentCellChanged.connect(lambda *_: self._update_dialogue_preview())
        self.inter_table.itemChanged.connect(lambda *_: self._update_dialogue_preview())

        b_save = QPushButton("Save Interactions")
        b_save.clicked.connect(self._on_save_clicked)
        v.addWidget(b_save)

        self.tabs.addTab(self.tab_inter, "Interactions")

    def _build_tab_placement(self):
        self.tab_place = QWidget()
        v = QVBoxLayout(self.tab_place)

        top = QHBoxLayout()
        self.room_filter = QLineEdit(); self.room_filter.setPlaceholderText("Filter rooms by id/title...")
        self.room_filter.textChanged.connect(self._refresh_room_checks)
        top.addWidget(self.room_filter)

        b_reload = QPushButton("Reload rooms.json"); b_reload.clicked.connect(self._reload_rooms)
        top.addWidget(b_reload)
        v.addLayout(top)

        self.rooms_box = QTreeWidget()
        self.rooms_box.setHeaderLabels(["Room", "In Room?"])
        self.rooms_box.header().setSectionResizeMode(0, QHeaderView.Stretch)
        self.rooms_box.header().setSectionResizeMode(1, QHeaderView.ResizeToContents)
        self.rooms_box.itemChanged.connect(self._on_room_checkbox_changed)
        v.addWidget(self.rooms_box, 1)

        b_save = QPushButton("Save Rooms Placement"); b_save.clicked.connect(self._save_rooms_if_dirty)
        v.addWidget(b_save)

        self.tabs.addTab(self.tab_place, "Placement")

    def _build_tab_validation(self):
        self.tab_val = QWidget()
        v = QVBoxLayout(self.tab_val)
        self.val_output = QTextEdit(); self.val_output.setReadOnly(True)
        v.addWidget(self.val_output, 1)
        b_val = QPushButton("Run Validation"); b_val.clicked.connect(self._run_validation)
        v.addWidget(b_val)
        self.tabs.addTab(self.tab_val, "Validation")

    # ---- Dialogue tab (NPC-scoped) ----
    def _build_tab_dialogue(self):
        self.tab_dlg = QWidget()
        v = QVBoxLayout(self.tab_dlg)

        top = QHBoxLayout()
        self.dlg_filter = QLineEdit(); self.dlg_filter.setPlaceholderText("Search this NPC's dialogue by id/text...")
        self.dlg_filter.textChanged.connect(self._refresh_dialogue_list)
        top.addWidget(self.dlg_filter)

        self.lbl_current_speaker = QLabel("-")
        # self.lbl_current_speaker.setStyleSheet("color: #777;")
        top_bar.addWidget(self.lbl_current_speaker)

        b_reload = QPushButton("Reload dialogue.json"); b_reload.clicked.connect(self._on_reload_dialogue_clicked)
        top.addWidget(b_reload)
        v.addLayout(top)

        # Split: list | editor
        split = QSplitter(Qt.Horizontal); v.addWidget(split, 1)

        # Left list
        self.dlg_list = QListWidget()
        self.dlg_list.itemSelectionChanged.connect(self._on_pick_dialogue_from_list)
        leftw = QWidget(); left = QVBoxLayout(leftw)
        left.addWidget(self.dlg_list, 1)

        btns = QHBoxLayout()
        b_new = QPushButton("Add"); b_new.clicked.connect(self._on_add_dialogue); btns.addWidget(b_new)
        b_dup = QPushButton("Duplicate"); b_dup.clicked.connect(self._on_duplicate_dialogue); btns.addWidget(b_dup)
        b_del = QPushButton("Delete"); b_del.clicked.connect(self._on_delete_dialogue); btns.addWidget(b_del)
        left.addLayout(btns)

        split.addWidget(leftw)

        # Right editor
        rightw = QWidget(); form = QFormLayout(rightw)

        self.dlg_id_edit = QLineEdit(); form.addRow("ID:", self.dlg_id_edit)
        self.dlg_speaker = QLineEdit(); form.addRow("Speaker:", self.dlg_speaker)
        self.dlg_text = QTextEdit(); form.addRow("Text:", self.dlg_text)
        self.dlg_emote = QLineEdit(); self.dlg_emote.setPlaceholderText("e.g., angry, sad"); form.addRow("Emote:", self.dlg_emote)
        self.dlg_condition = QLineEdit(); form.addRow("Condition (e.g., quest:id, milestone:id, item:Name):", self.dlg_condition)
        self.dlg_trigger = QLineEdit(); form.addRow("Trigger (e.g., give_item:id, start_quest:id):", self.dlg_trigger)
        self.dlg_next = QComboBox(); self.dlg_next.setEditable(True); form.addRow("Next ID:", self.dlg_next)

        btnrow = QHBoxLayout()
        b_save_entry = QPushButton("Save Entry"); b_save_entry.clicked.connect(self._on_save_dialogue_entry); btnrow.addWidget(b_save_entry)
        b_save_all = QPushButton("Save All"); b_save_all.clicked.connect(self._on_save_dialogue_all); btnrow.addWidget(b_save_all)
        b_assign = QPushButton("Assign to selected talk row"); b_assign.clicked.connect(self._assign_current_dialogue_to_row); btnrow.addWidget(b_assign)
        b_chain = QPushButton("Create Next Line"); b_chain.clicked.connect(self._on_create_next_dialogue); btnrow.addWidget(b_chain)
        form.addRow(btnrow)

        split.addWidget(rightw)
        split.setStretchFactor(1, 2)

        self.tabs.addTab(self.tab_dlg, "Dialogue")

        self._refresh_dialogue_list()

    # ==========================================================
    # NPC list / selection
    # ==========================================================
    def _refresh_npc_list(self):
        self.npc_list.clear()
        ft = (self.search.text() or "").lower()
        for key in sorted(self.npcs.keys()):
            npc = self.npcs[key]
            aliases = [a.lower() for a in npc.get("aliases", [])]
            if ft in key or any(ft in a for a in aliases):
                self.npc_list.addItem(QListWidgetItem(npc.get("name", key)))
        if self.current_key:
            for i in range(self.npc_list.count()):
                if self.npc_list.item(i).text().lower() == self.current_key:
                    self.npc_list.setCurrentRow(i)
                    break

    def _on_select_npc(self):
        item = self.npc_list.currentItem()
        if not item:
            self.current_key = None
            self._clear_forms()
            self._refresh_dialogue_list()
            return
        key = item.text().lower()
        if key not in self.npcs:
            for k, v in self.npcs.items():
                if v.get("name", "").lower() == key:
                    key = k; break
        self.current_key = key
        self._load_npc_into_forms()
        self._refresh_dialogue_tab_header()
        self._refresh_dialogue_list()

    def _clear_forms(self):
        self.name_edit.setText("")
        self.aliases_edit.setText("")
        self.portrait_edit.setText("")
        self.emotes_blob.setPlainText("")
        self.dialogue_blob.setPlainText("")
        self._load_interactions([])
        self._refresh_room_checks()

    def _load_npc_into_forms(self):
        npc = self.npcs.get(self.current_key, {})
        self.name_edit.setText(npc.get("name", ""))
        self.aliases_edit.setText(", ".join(npc.get("aliases", [])))
        self.portrait_edit.setText(npc.get("portrait", "") or "")
        self.emotes_blob.setPlainText(json.dumps(npc.get("emotes", {}), indent=4))
        self.dialogue_blob.setPlainText(json.dumps(npc.get("dialogue", {}), indent=4))
        self._load_interactions(npc.get("interactions", []))
        self._refresh_room_checks()

    # ==========================================================
    # Save basics / add / delete / duplicate NPCs
    # ==========================================================
    def _on_save_clicked(self):
        if not self.current_key:
            QMessageBox.information(self, "No NPC", "Select or add an NPC first.")
            return

        npc = self.npcs[self.current_key]
        old_name = npc.get("name", "")

        new_name = self.name_edit.text().strip()
        if not new_name:
            QMessageBox.warning(self, "Invalid", "Name cannot be empty.")
            return
        new_key = new_name.lower()

        if new_key != self.current_key:
            if new_key in self.npcs:
                QMessageBox.warning(self, "Exists", f"An NPC named '{new_name}' already exists.")
                return
            # rename in rooms
            self._rename_npc_in_rooms(self.current_key, new_name)
            # rename in dialogue speakers
            self._rename_speaker_in_dialogue(old_name, new_name)

            old = self.npcs.pop(self.current_key)
            old["name"] = new_name
            self.npcs[new_key] = old
            self.current_key = new_key
            npc = old

        aliases = [a.strip() for a in self.aliases_edit.text().split(",") if a.strip()]
        aliases = sorted({a for a in aliases if a.lower() != new_key})
        npc["aliases"] = aliases

        npc["portrait"] = self.portrait_edit.text().strip()

        try:
            txt = self.emotes_blob.toPlainText().strip() or "{}"
            obj = json.loads(txt)
            if not isinstance(obj, dict):
                raise ValueError("Must be a JSON object")
            npc["emotes"] = obj
        except Exception as e:
            QMessageBox.warning(self, "Invalid JSON", f"Emotes JSON invalid:\n{e}")
            return

        # Legacy per-NPC dialogue JSON (kept)
        try:
            txt = self.dialogue_blob.toPlainText().strip() or "{}"
            obj = json.loads(txt)
            if not isinstance(obj, dict):
                raise ValueError("Must be a JSON object")
            npc["dialogue"] = obj
        except Exception as e:
            QMessageBox.warning(self, "Invalid JSON", f"Dialogue JSON invalid:\n{e}")
            return

        # interactions from table
        try:
            npc["interactions"] = self._collect_interactions_from_table()
        except Exception as e:
            QMessageBox.warning(self, "Invalid Interactions", str(e))
            return

        if not self._save_npcs(): return
        if not self._save_rooms_if_dirty(): return

        QMessageBox.information(self, "Saved", "NPC and (if changed) placement saved.")
        self._refresh_npc_list()
        self._refresh_dialogue_tab_header()
        self._refresh_dialogue_list()

    def _on_add_npc(self):
        text, ok = QInputDialog.getText(self, "Add NPC", "Enter new NPC name:")
        if not (ok and text.strip()):
            return
        key = text.strip().lower()
        if key in self.npcs:
            QMessageBox.warning(self, "Exists", f"NPC '{text}' already exists.")
            return
        self.npcs[key] = {
            "name": text.strip(),
            "aliases": [],
            "portrait": "",
            "emotes": {},
            "dialogue": {},
            "interactions": []
        }
        self._save_npcs()
        self._refresh_npc_list()
        self._select_by_key(key)

    def _on_duplicate_npc(self):
        if not self.current_key: return
        base = deepcopy(self.npcs[self.current_key])
        new_name, ok = QInputDialog.getText(self, "Duplicate NPC", "New NPC name:", text=base.get("name", "") + " Copy")
        if not (ok and new_name.strip()): return
        new_key = new_name.strip().lower()
        if new_key in self.npcs:
            QMessageBox.warning(self, "Exists", f"NPC '{new_name}' already exists.")
            return
        base["name"] = new_name.strip()
        self.npcs[new_key] = base
        self._save_npcs()
        self._refresh_npc_list()
        self._select_by_key(new_key)

    def _on_delete_npc(self):
        # Nothing selected?
        if not self.current_key:
            QMessageBox.information(self, "No NPC", "Select an NPC to delete.")
            return

        # Resolve the correct dict key in case current_key is a display name
        key = self.current_key
        if key not in self.npcs:
            for k, v in self.npcs.items():
                if v.get("name", "").lower() == key:
                    key = k
                    break

        npc = self.npcs.get(key)
        if not npc:
            QMessageBox.warning(self, "Not found",
                                "The selected NPC no longer exists. Refreshing list.")
            self._refresh_npc_list()
            return

        name = npc.get("name", key)
        resp = QMessageBox.question(
            self, "Delete NPC",
            f"Delete NPC '{name}'?\n\n"
            f"(This will also remove '{name}' from any room placements.)",
            QMessageBox.Yes | QMessageBox.No
        )
        if resp != QMessageBox.Yes:
            return

        # Remove NPC from rooms' 'npcs' lists (by name)
        removed_from_rooms = False
        for rd in self.rooms:
            lst = rd.get("npcs", [])
            if not isinstance(lst, list):
                continue
            new_lst = [nm for nm in lst if str(nm).lower() != str(name).lower()]
            if new_lst != lst:
                rd["npcs"] = new_lst
                removed_from_rooms = True

        if removed_from_rooms:
            self._rooms_dirty = True
            self._save_rooms_if_dirty()

        # Remove from NPC dict
        self.npcs.pop(key, None)

        # Clear current + save and refresh UI
        self.current_key = None
        self._save_npcs()
        self._refresh_npc_list()
        self._clear_forms()
        self._refresh_dialogue_tab_header()
        self._refresh_dialogue_list()

    def _select_by_key(self, key_lower):
        for i in range(self.npc_list.count()):
            if self.npc_list.item(i).text().lower() == key_lower:
                self.npc_list.setCurrentRow(i); break

    # ==========================================================
    # Interactions table
    # ==========================================================
    def _load_interactions(self, inter_list):
        self.inter_table.blockSignals(True)
        self.inter_table.setRowCount(0)
        for inter in inter_list:
            self._add_interaction_row(inter)
        self.inter_table.blockSignals(False)
        self._update_dialogue_preview()

    def _add_interaction_row(self, inter=None):
        row = self.inter_table.rowCount()
        self.inter_table.insertRow(row)

        # Label
        lbl_item = QTableWidgetItem((inter or {}).get("label", ""))
        self.inter_table.setItem(row, 0, lbl_item)

        # Type
        typ_combo = QComboBox(); typ_combo.addItems(INTERACTION_TYPES)
        typ = (inter or {}).get("type", "talk")
        typ_combo.setCurrentText(typ if typ in INTERACTION_TYPES else "custom")
        typ_combo.currentTextChanged.connect(lambda new_typ, row=row: self._on_interaction_type_changed(row, new_typ))
        self.inter_table.setCellWidget(row, 1, typ_combo)

        # Value field (dialogue_id / shop_id / event_id / scene_id / value)
        val = ""
        if inter:
            if typ == "talk":       val = inter.get("dialogue_id", "")
            elif typ == "shop":     val = inter.get("shop_id", "")
            elif typ == "cinematic":val = inter.get("scene_id", "")
            elif typ == "event":    val = inter.get("event_id", "")
            else:                   val = inter.get("value", "")
        self._set_value_widget(row, typ, val)

        # Extra JSON
        extra = (inter or {}).get("data", {})
        self.inter_table.setItem(row, 3, QTableWidgetItem(json.dumps(extra) if extra else ""))

        # Row delete button
        btn = QPushButton("X")
        btn.clicked.connect(lambda *_: self._remove_specific_row(row))
        self.inter_table.setCellWidget(row, 4, btn)

    def _remove_specific_row(self, row):
        if 0 <= row < self.inter_table.rowCount():
            self.inter_table.removeRow(row)

    def _remove_interaction_row(self):
        r = self.inter_table.currentRow()
        if r >= 0: self.inter_table.removeRow(r)

    def _move_interaction(self, direction):
        r = self.inter_table.currentRow()
        if r < 0: return
        target = r + direction
        if target < 0 or target >= self.inter_table.rowCount(): return
        try:
            current = self._collect_interactions_from_table()
        except Exception as e:
            QMessageBox.warning(self, "Fix Errors First", str(e)); return
        current[r], current[target] = current[target], current[r]
        self._load_interactions(current)
        self.inter_table.setCurrentCell(target, 0)

    def _pick_dialogue_for_selected_row(self):
        # Use the Dialogue tab as the picker; it's scoped to this NPC.
        if self.tabs.currentWidget() is not self.tab_dlg:
            self.tabs.setCurrentWidget(self.tab_dlg)
        QMessageBox.information(self, "Pick Dialogue",
                                "Use the Dialogue tab: select an entry and click 'Assign to selected talk row'.")




    def _get_cell_text(self, row, col):
        widget = self.inter_table.cellWidget(row, col)
        if isinstance(widget, QComboBox):
            return widget.currentText()
        if isinstance(widget, QLineEdit):
            return widget.text()
        it = self.inter_table.item(row, col)
        return it.text() if it else ""

    def _collect_interactions_from_table(self):
        out = []
        for r in range(self.inter_table.rowCount()):
            label = (self._get_cell_text(r, 0) or "").strip()
            typ_widget = self.inter_table.cellWidget(r, 1)
            typ = typ_widget.currentText() if typ_widget else "custom"
            value = (self._get_cell_text(r, 2) or "").strip()
            extra_raw = (self._get_cell_text(r, 3) or "").strip()

            inter = {"label": label, "type": typ}

            if typ == "talk":
                if not value:
                    raise ValueError(f"Row {r+1}: talk requires dialogue_id")
                inter["dialogue_id"] = value
            elif typ == "shop":
                if not value:
                    raise ValueError(f"Row {r+1}: shop requires shop_id")
                inter["shop_id"] = value
            elif typ == "cinematic":
                if not value:
                    raise ValueError(f"Row {r+1}: cinematic requires scene_id")
                inter["scene_id"] = value
            elif typ == "event":
                if not value:
                    raise ValueError(f"Row {r+1}: event requires event_id")
                inter["event_id"] = value
            else:
                if value:
                    inter["value"] = value

            if extra_raw:
                try:
                    data_obj = json.loads(extra_raw)
                    if not isinstance(data_obj, dict):
                        raise ValueError("Extra JSON must be an object")
                    inter["data"] = data_obj
                except Exception as e:
                    raise ValueError(f"Row {r+1}: Extra JSON invalid: {e}")

            out.append(inter)
        return out

    def _dialogue_ids_for_current_npc(self) -> List[str]:
        if not self.dialogue_index:
            return []
        if not self.current_key:
            return sorted(self.dialogue_index.keys())
        npc_name = self.npcs.get(self.current_key, {}).get("name", "")
        if not npc_name:
            return sorted(self.dialogue_index.keys())
        return sorted([
            did for did, entry in self.dialogue_index.items()
            if str(entry.get("speaker", "")).lower() == npc_name.lower()
        ])

    def _set_value_widget(self, row: int, typ: str, value: str):
        if row < 0 or row >= self.inter_table.rowCount():
            return
        existing = self.inter_table.cellWidget(row, 2)
        if existing is not None:
            self.inter_table.removeCellWidget(row, 2)
        self.inter_table.setItem(row, 2, None)
        if typ == "talk":
            combo = QComboBox(); combo.setEditable(True)
            combo.addItem("")
            combo.addItems(self._dialogue_ids_for_current_npc())
            combo.setCurrentText(value)
            combo.currentTextChanged.connect(lambda *_: self._update_dialogue_preview())
            self.inter_table.setCellWidget(row, 2, combo)
        elif typ == "shop":
            combo = QComboBox(); combo.setEditable(True)
            combo.addItem("")
            combo.addItems(self.shop_ids)
            combo.setCurrentText(value)
            combo.currentTextChanged.connect(lambda *_: self._update_dialogue_preview())
            self.inter_table.setCellWidget(row, 2, combo)
        else:
            line = QLineEdit(value)
            line.editingFinished.connect(lambda *_: self._update_dialogue_preview())
            self.inter_table.setCellWidget(row, 2, line)

    def _on_interaction_type_changed(self, row: int, new_typ: str):
        if row < 0 or row >= self.inter_table.rowCount():
            return
        value = self._get_cell_text(row, 2)
        self._set_value_widget(row, new_typ, value)
        self._update_dialogue_preview()

    def _refresh_interaction_value_widgets(self):
        if not hasattr(self, "inter_table"):
            return
        for row in range(self.inter_table.rowCount()):
            typ_widget = self.inter_table.cellWidget(row, 1)
            typ = typ_widget.currentText() if typ_widget else "custom"
            value = self._get_cell_text(row, 2)
            self._set_value_widget(row, typ, value)

    def _add_talk_with_dialogue(self):
        if not self.current_key:
            QMessageBox.information(self, "No NPC", "Select an NPC first.")
            return
        npc = self.npcs.get(self.current_key, {})
        npc_name = npc.get("name", "")
        dlg_id = self._suggest_dialogue_id(npc_name)
        self.dialogue_index[dlg_id] = {
            "id": dlg_id,
            "speaker": npc_name,
            "text": "",
            "condition": "",
            "trigger": "",
            "next": ""
        }
        if not self._save_dialogue():
            self.dialogue_index.pop(dlg_id, None)
            return
        self._refresh_dialogue_list()
        inter = {"label": "Talk", "type": "talk", "dialogue_id": dlg_id}
        self._add_interaction_row(inter)
        row = self.inter_table.rowCount() - 1
        if row >= 0:
            self.inter_table.setCurrentCell(row, 0)
        self._select_dialogue(dlg_id)
        self._load_dialogue_into_editor(dlg_id)
        self._update_dialogue_preview()
        self._refresh_interaction_value_widgets()

    def _add_shop_row(self):
        if not self.shop_ids:
            QMessageBox.warning(self, "No shops", "No shops found in shops.json.")
        inter = {"label": "Shop", "type": "shop"}
        if self.shop_ids:
            inter["shop_id"] = self.shop_ids[0]
        self._add_interaction_row(inter)
        row = self.inter_table.rowCount() - 1
        if row >= 0:
            self.inter_table.setCurrentCell(row, 0)
        self._update_dialogue_preview()

    def _on_reload_shops_clicked(self):
        self._load_shops()
        self._refresh_interaction_value_widgets()
        QMessageBox.information(self, "Shops", "Reloaded shops.json.")

    def _update_dialogue_dropdowns(self):
        if not hasattr(self, "dlg_next"):
            return
        current = self.dlg_next.currentText() if isinstance(self.dlg_next, QComboBox) else ""
        self.dlg_next.blockSignals(True)
        self.dlg_next.clear()
        self.dlg_next.addItem("")
        self.dlg_next.addItems(self._dialogue_ids_for_current_npc())
        self.dlg_next.setCurrentText(current)
        self.dlg_next.blockSignals(False)
        self._refresh_interaction_value_widgets()

    def _on_create_next_dialogue(self):
        if not self.current_key or not self.current_dlg_id:
            QMessageBox.information(self, "No dialogue selected", "Select a dialogue entry first.")
            return
        entry = self.dialogue_index.get(self.current_dlg_id)
        if not entry:
            return
        entry["speaker"] = self.dlg_speaker.text().strip()
        entry["text"] = self.dlg_text.toPlainText()
        entry["emote"] = self.dlg_emote.text().strip()
        entry["condition"] = self.dlg_condition.text().strip()
        entry["trigger"] = self.dlg_trigger.text().strip()
        entry["next"] = self.dlg_next.currentText().strip()
        if entry.get("next"):
            if QMessageBox.question(self, "Replace next", "This dialogue already has a next entry. Replace it?",
                                    QMessageBox.Yes | QMessageBox.No) != QMessageBox.Yes:
                return
        npc_name = self.npcs.get(self.current_key, {}).get("name", "")
        base = entry.get("speaker") or npc_name or "dialogue"
        new_id = self._suggest_dialogue_id(base)
        self.dialogue_index[new_id] = {
            "id": new_id,
            "speaker": base,
            "text": "",
            "emote": "",
            "condition": "",
            "trigger": "",
            "next": ""
        }
        entry["next"] = new_id
        if not self._save_dialogue():
            self.dialogue_index.pop(new_id, None)
            entry["next"] = ""
            return
        self._refresh_dialogue_list()
        self._select_dialogue(new_id)
        self._load_dialogue_into_editor(new_id)
        self._update_dialogue_preview()

    




    def _update_dialogue_preview(self):
        self.dlg_preview.setPlainText("")
        r = self.inter_table.currentRow()
        if r < 0:
            return
        typ_widget = self.inter_table.cellWidget(r, 1)
        typ = typ_widget.currentText() if typ_widget else ""
        value = (self._get_cell_text(r, 2) or "").strip()

        if typ == "talk":
            if not value:
                self.dlg_preview.setPlainText("(No dialogue_id on this row.)")
                return
            entry = self.dialogue_index.get(value)
            if not entry:
                self.dlg_preview.setPlainText(f"(dialogue_id '{value}' not found in dialogue.json)")
                return
            speaker = entry.get("speaker", "")
            text_body = entry.get("text", "")
            emote = entry.get("emote", "")
            cond = entry.get("condition", "")
            trig = entry.get("trigger", "")
            nxt = entry.get("next", "")
            lines = [
                f"id: {value}",
                f"speaker: {speaker}",
                f"emote: {emote}",
                f"condition: {cond}",
                f"trigger: {trig}",
                f"next: {nxt}",
                "",
                "text:",
                text_body,
            ]
            self.dlg_preview.setPlainText("\n".join(lines))
            return

        if typ == "shop":
            if not value:
                self.dlg_preview.setPlainText("(No shop_id on this row.)")
                return
            shop = self.shops.get(value) if isinstance(self.shops, dict) else None
            if not shop:
                self.dlg_preview.setPlainText(f"(shop_id '{value}' not found in shops.json)")
                return
            name = shop.get("name", "")
            sells = shop.get("sells", {}) if isinstance(shop, dict) else {}
            items = []
            if isinstance(sells, dict):
                raw_items = sells.get("items", [])
                if isinstance(raw_items, list):
                    items = [str(it) for it in raw_items]
            lines = [f"shop_id: {value}"]
            if name:
                lines.append(f"name: {name}")
            if items:
                preview_items = items[:10]
                lines.append("items: " + ", ".join(preview_items))
                if len(items) > 10:
                    lines.append(f"... plus {len(items) - 10} more")
            pricing = shop.get("pricing")
            if isinstance(pricing, dict):
                lines.append("pricing: " + ", ".join(f"{k}={v}" for k, v in pricing.items()))
            self.dlg_preview.setPlainText("\n".join(lines))
            return

        if typ in {"event", "cinematic", "custom"}:
            if value:
                self.dlg_preview.setPlainText(f"{typ} value: {value}")
            else:
                self.dlg_preview.setPlainText(f"({typ} row has no value set.)")
            return

        if value:
            self.dlg_preview.setPlainText(f"{typ} value: {value}")
        else:
            self.dlg_preview.setPlainText("(Select a row to preview details.)")

    # Placement / rooms
    # ==========================================================
    def _reload_rooms(self):
        try:
            self.rooms = _load_json_list(self.rooms_path)
            self._rooms_dirty = False
            self._refresh_room_checks()
        except Exception as e:
            QMessageBox.critical(self, "Load Error", f"Failed to load rooms.json:\n{e}")

    def _refresh_room_checks(self):
        self.rooms_box.blockSignals(True)
        self.rooms_box.clear()
        filt = (self.room_filter.text() or "").lower()
        name = self.npcs.get(self.current_key, {}).get("name", "") if self.current_key else ""
        for rd in self.rooms:
            room_id = rd.get("id", ""); title = rd.get("title", room_id)
            label = f"{room_id} -- {title}"
            if filt and (filt not in room_id.lower() and filt not in (title or "").lower()):
                continue
            item = QTreeWidgetItem([label])
            item.setFlags(item.flags() | Qt.ItemIsUserCheckable)
            present = name and name in _as_list(rd.get("npcs", []))
            item.setCheckState(0, Qt.Checked if present else Qt.Unchecked)
            item.setData(0, Qt.UserRole, room_id)
            self.rooms_box.addTopLevelItem(item)
        self.rooms_box.blockSignals(False)

    def _on_room_checkbox_changed(self, item, col):
        if not self.current_key or col != 0: return
        room_id = item.data(0, Qt.UserRole)
        npc = self.npcs.get(self.current_key)
        if not npc:
            return
        npc_name = npc.get("name", "")
        if not npc_name:
            return
        rd = next((r for r in self.rooms if r.get("id") == room_id), None)
        if not rd: return
        npclist = rd.get("npcs", [])
        if not isinstance(npclist, list): npclist = []
        if item.checkState(0) == Qt.Checked:
            if npc_name not in npclist:
                npclist.append(npc_name); rd["npcs"] = npclist; self._rooms_dirty = True
        else:
            if npc_name in npclist:
                npclist.remove(npc_name); rd["npcs"] = npclist; self._rooms_dirty = True

    def _rename_npc_in_rooms(self, old_key_lower, new_name):
        old_name = None
        if old_key_lower in self.npcs:
            old_name = self.npcs[old_key_lower].get("name", None)
        if not old_name: old_name = old_key_lower
        for rd in self.rooms:
            lst = rd.get("npcs", [])
            if not isinstance(lst, list): continue
            changed = False
            for i, nm in enumerate(lst):
                if str(nm).lower() == str(old_name).lower():
                    lst[i] = new_name; changed = True
            if changed:
                rd["npcs"] = lst; self._rooms_dirty = True

    def _rename_speaker_in_dialogue(self, old_name: str, new_name: str):
        """On NPC rename, update dialogue entries whose speaker matches that NPC."""
        if not old_name or not new_name: return
        changed = False
        for d in self.dialogue_index.values():
            if str(d.get("speaker", "")).lower() == str(old_name).lower():
                d["speaker"] = new_name
                changed = True
        if changed:
            self._save_dialogue()
            self._refresh_dialogue_tab_header()
            self._refresh_dialogue_list()

    # ==========================================================
    # Validation
    # ==========================================================
    def _run_validation(self):
        if not self.current_key:
            self.val_output.setPlainText("No NPC selected."); return
        npc = self.npcs[self.current_key]; msgs = []
        name = npc.get("name", "").strip()
        if not name: msgs.append("ERROR: Name is empty.")
        aliases = npc.get("aliases", [])
        if any(a.lower() == name.lower() for a in aliases):
            msgs.append("WARN: Aliases include NPC's own name (removed on save).")

        for i, inter in enumerate(npc.get("interactions", []), start=1):
            typ = inter.get("type")
            if typ == "talk":
                dlg_id = inter.get("dialogue_id", "")
                if not dlg_id:
                    msgs.append(f"ERROR: Row {i} talk missing dialogue_id.")
                elif self.dialogue_index and dlg_id not in self.dialogue_index:
                    msgs.append(f"WARN: dialogue_id '{dlg_id}' not found in dialogue.json.")
            elif typ == "shop":
                if not inter.get("shop_id"):
                    msgs.append(f"ERROR: Row {i} shop missing shop_id.")
            elif typ == "cinematic":
                if not inter.get("scene_id"):
                    msgs.append(f"ERROR: Row {i} cinematic missing scene_id.")
            elif typ == "event":
                if not inter.get("event_id"):
                    msgs.append(f"ERROR: Row {i} event missing event_id.")

        if not self.rooms:
            msgs.append("INFO: rooms.json not loaded -- placement checks skipped.")
        else:
            placed = [rd.get("id","?") for rd in self.rooms if name in _as_list(rd.get("npcs", []))]
            if not placed:
                msgs.append("INFO: This NPC is not placed in any rooms.")
            else:
                msgs.append(f"OK: NPC appears in rooms: {', '.join(sorted(placed))}")

        self.val_output.setPlainText("\n".join(msgs) if msgs else "All checks passed.")

    # ==========================================================
    # Dialogue tab handlers (NPC-scoped)
    # ==========================================================
    def _on_reload_dialogue_clicked(self):
        self._reload_dialogue()
        self._refresh_dialogue_list()
        self._update_dialogue_preview()

    def _refresh_dialogue_tab_header(self):
        cur_name = self.npcs.get(self.current_key, {}).get("name", "") if self.current_key else ""
        self.lbl_current_speaker.setText(f"Speaker: {cur_name or '--'}  (showing only)")


    def _refresh_dialogue_list(self):
        """Show ONLY entries whose speaker matches the current NPC."""
        self.dlg_list.clear()
        cur_name = self.npcs.get(self.current_key, {}).get("name", "") if self.current_key else ""
        if not cur_name:
            # No NPC selected: nothing to show
            self._update_dialogue_dropdowns()
            return

        ft = (self.dlg_filter.text() or "").lower()
        for did in sorted(self.dialogue_index.keys()):
            entry = self.dialogue_index[did]
            speaker = (entry.get("speaker") or "")
            if speaker.lower() != cur_name.lower():
                continue
            text = (entry.get("text") or "")
            hay = f"{did} {text}".lower()
            if ft and ft not in hay:
                continue
            self.dlg_list.addItem(QListWidgetItem(did))

        # keep selection
        if self.current_dlg_id:
            for i in range(self.dlg_list.count()):
                if self.dlg_list.item(i).text() == self.current_dlg_id:
                    self.dlg_list.setCurrentRow(i)
                    break
        self._update_dialogue_dropdowns()

    def _on_pick_dialogue_from_list(self):
        item = self.dlg_list.currentItem()
        if not item:
            self.current_dlg_id = None
            self._clear_dialogue_editor()
            return
        self.current_dlg_id = item.text()
        self._load_dialogue_into_editor(self.current_dlg_id)

    def _clear_dialogue_editor(self):
        self.dlg_id_edit.setText("")
        self.dlg_speaker.setText("")
        self.dlg_text.setPlainText("")
        self.dlg_emote.setText("")
        self.dlg_condition.setText("")
        self.dlg_trigger.setText("")
        self.dlg_next.setEditText("")

    def _load_dialogue_into_editor(self, did):
        d = self.dialogue_index.get(did, {})
        self.dlg_id_edit.setText(d.get("id", did))
        self.dlg_speaker.setText(d.get("speaker", ""))
        self.dlg_text.setPlainText(d.get("text", ""))
        self.dlg_emote.setText(d.get("emote", ""))
        self.dlg_condition.setText(d.get("condition", ""))
        self.dlg_trigger.setText(d.get("trigger", ""))
        self.dlg_next.setEditText(d.get("next", ""))

    def _suggest_dialogue_id(self, npc_name: str) -> str:
        base = _slug(npc_name) or "dialogue"
        i = 1
        while f"{base}_{i}" in self.dialogue_index:
            i += 1
        return f"{base}_{i}"

    def _on_add_dialogue(self):
        if not self.current_key:
            QMessageBox.information(self, "No NPC", "Select an NPC first.")
            return
        npc_name = self.npcs[self.current_key].get("name", "")
        default_id = self._suggest_dialogue_id(npc_name)
        new_id, ok = QInputDialog.getText(self, "Add Dialogue", "Enter new dialogue ID:", text=default_id)
        if not (ok and new_id.strip()):
            return
        if new_id in self.dialogue_index:
            QMessageBox.warning(self, "Exists", "Dialogue ID already exists."); return
        self.dialogue_index[new_id] = {
            "id": new_id,
            "speaker": npc_name,  # <-- prefill to current NPC
            "text": "",
            "emote": "",
            "condition": "",
            "trigger": "",
            "next": ""
        }
        self._save_dialogue()
        self._refresh_dialogue_list()
        self._select_dialogue(new_id)
        self._load_dialogue_into_editor(new_id)

    def _on_duplicate_dialogue(self):
        if not self.current_dlg_id: return
        base = deepcopy(self.dialogue_index[self.current_dlg_id])
        new_id, ok = QInputDialog.getText(self, "Duplicate Dialogue", "New dialogue ID:",
                                          text=self._suggest_dialogue_id(base.get("speaker","") or "dialogue"))
        if not (ok and new_id.strip()): return
        if new_id in self.dialogue_index:
            QMessageBox.warning(self, "Exists", "Dialogue ID already exists."); return
        base["id"] = new_id
        self.dialogue_index[new_id] = base
        self._save_dialogue()
        self._refresh_dialogue_list()
        self._select_dialogue(new_id)

    def _on_delete_dialogue(self):
        if not self.current_dlg_id: return
        if QMessageBox.question(self, "Delete Dialogue", f"Delete '{self.current_dlg_id}'?") != QMessageBox.Yes:
            return
        del self.dialogue_index[self.current_dlg_id]
        self.current_dlg_id = None
        self._save_dialogue()
        self._refresh_dialogue_list()
        self._clear_dialogue_editor()

    def _on_save_dialogue_entry(self):
        if not self.current_dlg_id:
            QMessageBox.information(self, "No selection", "Pick a dialogue entry to save."); return

        old_id = self.current_dlg_id
        new_id = self.dlg_id_edit.text().strip() or old_id

        # rename id (with propagation) if changed
        if new_id != old_id:
            if new_id in self.dialogue_index:
                QMessageBox.warning(self, "Exists", f"A dialogue with id '{new_id}' already exists.")
                return
            entry = self.dialogue_index.pop(old_id)
            entry["id"] = new_id
            self.dialogue_index[new_id] = entry
            # propagate to all NPC interactions
            changed = False
            for nk, npc in self.npcs.items():
                for inter in npc.get("interactions", []):
                    if inter.get("type") == "talk" and inter.get("dialogue_id") == old_id:
                        inter["dialogue_id"] = new_id
                        changed = True
            if changed:
                self._save_npcs()
            self.current_dlg_id = new_id

        # write fields
        d = self.dialogue_index[self.current_dlg_id]
        d["speaker"]   = self.dlg_speaker.text().strip()
        d["text"]      = self.dlg_text.toPlainText()
        d["emote"]     = self.dlg_emote.text().strip()
        d["condition"] = self.dlg_condition.text().strip()
        d["trigger"]   = self.dlg_trigger.text().strip()
        d["next"]      = self.dlg_next.currentText().strip()

        if not self._save_dialogue():
            return
        self._refresh_dialogue_list()
        self._update_dialogue_preview()
        QMessageBox.information(self, "Saved", "Dialogue entry saved.")

    def _on_save_dialogue_all(self):
        if self._save_dialogue():
            self._refresh_dialogue_list()
            self._update_dialogue_preview()
            QMessageBox.information(self, "Saved", "dialogue.json saved.")

    def _assign_current_dialogue_to_row(self):
        if not self.current_dlg_id:
            QMessageBox.information(self, "No dialogue selected", "Choose a dialogue on the Dialogue tab first.")
            return
        r = self.inter_table.currentRow()
        if r < 0:
            QMessageBox.information(self, "No row selected", "Select a 'talk' row in Interactions."); return
        typ_widget = self.inter_table.cellWidget(r, 1)
        if not typ_widget or typ_widget.currentText() != "talk":
            QMessageBox.information(self, "Wrong type", "The selected row is not of type 'talk'."); return
        value_widget = self.inter_table.cellWidget(r, 2)
        if isinstance(value_widget, QComboBox):
            value_widget.setCurrentText(self.current_dlg_id)
        elif isinstance(value_widget, QLineEdit):
            value_widget.setText(self.current_dlg_id)
        else:
            self.inter_table.setItem(r, 2, QTableWidgetItem(self.current_dlg_id))
        self._update_dialogue_preview()
        QMessageBox.information(self, "Assigned", f"Set dialogue_id = {self.current_dlg_id} on row {r+1}.")

    def _select_dialogue(self, did):
        for i in range(self.dlg_list.count()):
            if self.dlg_list.item(i).text() == did:
                self.dlg_list.setCurrentRow(i); break

    # ==========================================================
    # Misc
    # ==========================================================


# ---------- CLI ----------
if __name__ == "__main__":
    app = QApplication(sys.argv)
    w = NPCEditor(Path(__file__).parent)
    w.show()
    sys.exit(app.exec_())
