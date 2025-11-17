#!/usr/bin/env python3
"""
Starborn — Sound Editor (feature-rich, backward-compatible)

Adds:
  • Mix buses (Master/Music/SFX/UI/Ambience/Voice) with a small mixer panel
  • Per-tag playback options: volume, loop, fade in/out, volume jitter, pan
  • Categories + category filter for quick scoping
  • Multi-file import, tag rename, “Play Tag” random variation preview
  • Missing-file highlighting in the file list

Backward compatibility:
  • sfx.json stays exactly as before (tag -> string | list[str])
  • New metadata is saved to sfx_meta.json (tag -> meta)
  • Runtime SoundManager remains unchanged

Expected project structure (unchanged):
  PROJECT_ROOT/
    sfx.json        # legacy files map
    sfx_meta.json   # NEW: per-tag metadata (created automatically)
    sfx/            # audio files
"""

from __future__ import annotations

import random
import shutil
from pathlib import Path
from typing import Dict, List, Any, Optional

from PyQt5.QtCore import Qt
from PyQt5.QtGui import QColor, QBrush
from PyQt5.QtWidgets import (
    QWidget, QHBoxLayout, QVBoxLayout, QSplitter,
    QListWidget, QLineEdit, QComboBox,
    QPushButton, QLabel, QFormLayout, QTableWidget,
    QTableWidgetItem, QHeaderView, QAbstractItemView,
    QFileDialog, QMessageBox, QGroupBox, QSlider, QCheckBox, QSpinBox,
    QInputDialog
)

import pygame

from tools.data_core import detect_project_root, json_load, json_save, unique_id


# ---------------------------
# Defaults / constants
# ---------------------------
BUS_NAMES = ["Master", "Music", "SFX", "UI", "Ambience", "Voice"]
DEFAULT_BUS = "SFX"
DEFAULT_CATEGORY = ""  # empty means uncategorized

# Meta defaults per tag
DEFAULT_META = {
    "bus": DEFAULT_BUS,     # one of BUS_NAMES
    "category": DEFAULT_CATEGORY,
    "volume": 1.0,          # 0..1
    "loop": False,
    "fade_in_ms": 0,
    "fade_out_ms": 0,       # currently preview-only info (use later in runtime)
    "vol_jitter": 0.0,      # 0..0.5 (0..50%) recommended
    "pan": 0.0,             # -1..1 (left..right)
}


class SoundEditor(QWidget):
    """Audio/SFX editor integrated with Studio Pro."""
    def __init__(self, project_root: Optional[Path] = None):
        super().__init__()
        self.setWindowTitle("Starborn — Audio/SFX")
        self.root: Path = detect_project_root(project_root)
        self.sfx_path = self.root / "sfx.json"
        self.meta_path = self.root / "sfx_meta.json"
        self.sfx_dir = self.root / "sfx"

        # pygame mixer for preview
        if not pygame.mixer.get_init():
            pygame.mixer.init(frequency=44100, size=-16, channels=2)

        # Data
        self.sfx: Dict[str, List[str]] = {}        # tag -> list[str]
        self.meta: Dict[str, Dict[str, Any]] = {}  # tag -> meta dict
        self.current_tag: Optional[str] = None

        # Mixer state (preview only)
        self.bus_gains: Dict[str, float] = {name: 1.0 for name in BUS_NAMES}

        self._load()
        self._build_ui()
        self._reload_list()

    # ---------------------------
    # Data I/O
    # ---------------------------
    def _load(self):
        # Load legacy file mapping
        raw = json_load(self.sfx_path, default={})
        self.sfx = {}
        for tag, value in (raw or {}).items():
            if isinstance(value, list):
                self.sfx[tag] = list(value)
            elif isinstance(value, str):
                self.sfx[tag] = [value]
            # ignore other types

        # Load meta (may not exist yet)
        raw_meta = json_load(self.meta_path, default={})
        self.meta = {}
        for tag in self.sfx.keys():  # ensure only tags that exist are included
            m = raw_meta.get(tag, {})
            # apply defaults
            merged = dict(DEFAULT_META)
            merged.update({k: v for k, v in m.items() if k in DEFAULT_META})
            # sanity bounds
            merged["volume"] = max(0.0, min(1.0, float(merged["volume"])))
            merged["pan"] = max(-1.0, min(1.0, float(merged["pan"])))
            merged["vol_jitter"] = max(0.0, min(0.9, float(merged["vol_jitter"])))
            if merged.get("bus") not in BUS_NAMES:
                merged["bus"] = DEFAULT_BUS
            merged["category"] = str(merged.get("category", DEFAULT_CATEGORY))
            merged["loop"] = bool(merged.get("loop", False))
            merged["fade_in_ms"] = int(merged.get("fade_in_ms", 0))
            merged["fade_out_ms"] = int(merged.get("fade_out_ms", 0))
            self.meta[tag] = merged

    def _write_files_map(self) -> bool:
        """
        Write legacy sfx.json (tag -> string | list[str]).
        Converts 1-length lists back to string to preserve the old format.
        """
        out: Dict[str, Any] = {}
        for tag, files in self.sfx.items():
            if not files:
                continue
            out[tag] = files[0] if len(files) == 1 else list(files)
        try:
            json_save(self.sfx_path, out, sort_obj=True, indent=2)
            return True
        except Exception as e:
            print(f"[SoundEditor] Failed to save sfx.json: {e}")
            return False

    def _write_meta(self) -> bool:
        """
        Write sfx_meta.json (tag -> meta). Only tags present in sfx.json are saved.
        """
        out: Dict[str, Any] = {}
        for tag in self.sfx.keys():
            m = self.meta.get(tag, dict(DEFAULT_META))
            # Re-bound and clean
            out[tag] = {
                "bus": m.get("bus", DEFAULT_BUS) if m.get("bus") in BUS_NAMES else DEFAULT_BUS,
                "category": str(m.get("category", DEFAULT_CATEGORY)),
                "volume": max(0.0, min(1.0, float(m.get("volume", 1.0)))),
                "loop": bool(m.get("loop", False)),
                "fade_in_ms": int(m.get("fade_in_ms", 0)),
                "fade_out_ms": int(m.get("fade_out_ms", 0)),
                "vol_jitter": max(0.0, min(0.9, float(m.get("vol_jitter", 0.0)))),
                "pan": max(-1.0, min(1.0, float(m.get("pan", 0.0)))),
            }
        try:
            json_save(self.meta_path, out, sort_obj=True, indent=2)
            return True
        except Exception as e:
            print(f"[SoundEditor] Failed to save sfx_meta.json: {e}")
            return False

    def save(self) -> bool:
        """Studio calls this to persist changes—saves both files."""
        ok1 = self._write_files_map()
        ok2 = self._write_meta()
        if ok1 and ok2:
            QMessageBox.information(self, "Saved", "sfx.json and sfx_meta.json saved.")
            return True
        QMessageBox.critical(self, "Save error", "Failed to save audio data.")
        return False

    def validate(self) -> List[str]:
        """Return validation warnings/errors."""
        errs: List[str] = []
        for tag, files in self.sfx.items():
            if not files:
                errs.append(f"[Audio] Tag '{tag}' has no files assigned.")
                continue
            for filename in files:
                path = self.sfx_dir / filename
                if not path.exists():
                    errs.append(f"[Audio] File '{filename}' for tag '{tag}' is missing.")
        # meta sanity
        for tag, m in self.meta.items():
            if m.get("bus") not in BUS_NAMES:
                errs.append(f"[Audio] Tag '{tag}' has invalid bus '{m.get('bus')}'.")
        return errs

    # ---------------------------
    # UI
    # ---------------------------
    def _build_ui(self):
        split = QSplitter(Qt.Horizontal, self)
        root_layout = QHBoxLayout(self)
        root_layout.addWidget(split)

        # ----- Left side -----
        left = QWidget(); left_v = QVBoxLayout(left)

        # Filter row: text + category filter
        filt_row = QHBoxLayout()
        self.search_box = QLineEdit(); self.search_box.setPlaceholderText("Search tags…")
        self.search_box.textChanged.connect(self._reload_list)
        filt_row.addWidget(self.search_box)

        self.category_filter = QComboBox()
        self.category_filter.setEditable(False)
        self._refresh_category_filter()  # fills with "All" + categories
        self.category_filter.currentIndexChanged.connect(self._reload_list)
        filt_row.addWidget(self.category_filter, 0)

        left_v.addLayout(filt_row)

        self.list = QListWidget()
        self.list.itemSelectionChanged.connect(self._on_select)
        left_v.addWidget(self.list, 1)

        # Tag-level buttons
        row = QHBoxLayout()
        b_new = QPushButton("New")
        b_dup = QPushButton("Duplicate")
        b_ren = QPushButton("Rename")
        b_del = QPushButton("Delete")
        b_save = QPushButton("Save")
        b_new.clicked.connect(self._on_new)
        b_dup.clicked.connect(self._on_dup)
        b_ren.clicked.connect(self._on_rename)
        b_del.clicked.connect(self._on_delete)
        b_save.clicked.connect(self.save)
        for w in (b_new, b_dup, b_ren, b_del):
            row.addWidget(w)
        row.addStretch(1)
        row.addWidget(b_save)
        left_v.addLayout(row)

        # Mixer panel (preview only)
        mix = QGroupBox("Mixer (Preview Only)")
        mix_l = QVBoxLayout(mix)
        self.master_slider = self._make_bus_slider("Master", mix_l)
        self.music_slider = self._make_bus_slider("Music", mix_l)
        self.sfx_slider = self._make_bus_slider("SFX", mix_l)
        self.ui_slider = self._make_bus_slider("UI", mix_l)
        self.amb_slider = self._make_bus_slider("Ambience", mix_l)
        self.voice_slider = self._make_bus_slider("Voice", mix_l)
        left_v.addWidget(mix)

        split.addWidget(left)

        # ----- Right side -----
        self.detail = QWidget(); detail_v = QVBoxLayout(self.detail)

        # Basic tag info
        form = QFormLayout()
        self.f_tag_label = QLabel("-")
        form.addRow(QLabel("Tag:"), self.f_tag_label)

        # Tag meta controls (bus, category)
        meta_group = QGroupBox("Tag Settings")
        meta_l = QFormLayout(meta_group)

        self.bus_combo = QComboBox()
        self.bus_combo.addItems(BUS_NAMES)
        self.bus_combo.currentIndexChanged.connect(self._on_meta_changed)

        self.category_combo = QComboBox()
        self.category_combo.setEditable(True)
        self._fill_category_combo()
        self.category_combo.editTextChanged.connect(self._on_meta_changed)
        self.category_combo.currentIndexChanged.connect(self._on_meta_changed)

        meta_l.addRow("Bus:", self.bus_combo)
        meta_l.addRow("Category:", self.category_combo)

        # Playback options (volume, loop, fades, jitter, pan)
        pb_group = QGroupBox("Playback Options (Preview honors these)")
        pb_l = QFormLayout(pb_group)

        self.vol_slider = QSlider(Qt.Horizontal); self.vol_slider.setRange(0, 100); self.vol_slider.setValue(100)
        self.vol_slider.valueChanged.connect(self._on_meta_changed)

        self.loop_chk = QCheckBox("Loop")
        self.loop_chk.stateChanged.connect(self._on_meta_changed)

        self.fadein_spin = QSpinBox(); self.fadein_spin.setRange(0, 5000); self.fadein_spin.setSingleStep(50)
        self.fadein_spin.valueChanged.connect(self._on_meta_changed)

        self.fadeout_spin = QSpinBox(); self.fadeout_spin.setRange(0, 5000); self.fadeout_spin.setSingleStep(50)
        self.fadeout_spin.valueChanged.connect(self._on_meta_changed)

        self.jitter_slider = QSlider(Qt.Horizontal); self.jitter_slider.setRange(0, 50); self.jitter_slider.setValue(0)
        self.jitter_slider.valueChanged.connect(self._on_meta_changed)

        self.pan_slider = QSlider(Qt.Horizontal); self.pan_slider.setRange(-100, 100); self.pan_slider.setValue(0)
        self.pan_slider.valueChanged.connect(self._on_meta_changed)

        pb_l.addRow("Volume:", self.vol_slider)
        pb_l.addRow("", self.loop_chk)
        pb_l.addRow("Fade in (ms):", self.fadein_spin)
        pb_l.addRow("Fade out (ms):", self.fadeout_spin)
        pb_l.addRow("Vol jitter (%):", self.jitter_slider)
        pb_l.addRow("Pan (L/R):", self.pan_slider)

        detail_v.addLayout(form)
        detail_v.addWidget(meta_group)
        detail_v.addWidget(pb_group)

        # Files table
        self.table = QTableWidget(0, 2)
        self.table.setHorizontalHeaderLabels(["Audio File", "Actions"])
        self.table.horizontalHeader().setSectionResizeMode(0, QHeaderView.Stretch)
        self.table.horizontalHeader().setSectionResizeMode(1, QHeaderView.ResizeToContents)
        self.table.verticalHeader().setVisible(False)
        self.table.setSelectionBehavior(QAbstractItemView.SelectRows)
        detail_v.addWidget(self.table, 1)

        # File ops
        ops = QHBoxLayout()
        self.add_file_btn = QPushButton("Add File…")
        self.remove_file_btn = QPushButton("Remove File")
        self.play_file_btn = QPushButton("Play Selected")
        self.play_tag_btn = QPushButton("Play Tag")
        self.add_file_btn.clicked.connect(self._on_add_file)
        self.remove_file_btn.clicked.connect(self._on_remove_file)
        self.play_file_btn.clicked.connect(self._on_play_file)
        self.play_tag_btn.clicked.connect(self._on_play_tag)
        for w in (self.add_file_btn, self.remove_file_btn, self.play_file_btn, self.play_tag_btn):
            ops.addWidget(w)
        ops.addStretch(1)
        detail_v.addLayout(ops)

        split.addWidget(self.detail)
        split.setStretchFactor(1, 2)

        self.detail.setDisabled(True)

    def _make_bus_slider(self, name: str, parent_layout: QVBoxLayout) -> QSlider:
        row = QHBoxLayout()
        row.addWidget(QLabel(f"{name}:"))
        slider = QSlider(Qt.Horizontal)
        slider.setRange(0, 100); slider.setValue(100)
        slider.valueChanged.connect(lambda v, n=name: self._on_bus_gain_changed(n, v))
        row.addWidget(slider)
        parent_layout.addLayout(row)
        return slider

    # ---------------------------
    # List + filters
    # ---------------------------
    def _refresh_category_filter(self):
        """(Re)populate left-side category filter."""
        current = self.category_filter.currentText() if hasattr(self, "category_filter") else "All"
        cats = sorted({self.meta.get(t, {}).get("category", DEFAULT_CATEGORY) for t in self.meta} - {""}, key=str.lower)
        self.category_filter.clear()
        self.category_filter.addItem("All")
        for c in cats:
            self.category_filter.addItem(c)
        # restore selection if possible
        idx = self.category_filter.findText(current)
        if idx >= 0:
            self.category_filter.setCurrentIndex(idx)

    def _fill_category_combo(self):
        """Fill the editable category combo with known categories."""
        cats = sorted({self.meta.get(t, {}).get("category", DEFAULT_CATEGORY) for t in self.meta} - {""}, key=str.lower)
        self.category_combo.clear()
        for c in cats:
            self.category_combo.addItem(c)
        self.category_combo.setEditText("")

    def _reload_list(self):
        """Refresh the list of tags based on search and category filter."""
        filt_text = self.search_box.text().strip().lower()
        cat_sel = self.category_filter.currentText() if self.category_filter.count() else "All"
        self.list.clear()
        for tag in sorted(self.sfx.keys(), key=str.lower):
            if filt_text and filt_text not in tag.lower():
                continue
            if cat_sel != "All":
                if self.meta.get(tag, DEFAULT_META).get("category", DEFAULT_CATEGORY) != cat_sel:
                    continue
            self.list.addItem(tag)
        if self.current_tag:
            matches = self.list.findItems(self.current_tag, Qt.MatchExactly)
            if matches:
                self.list.setCurrentItem(matches[0])

    # ---------------------------
    # Selection + loading
    # ---------------------------
    def _on_select(self):
        items = self.list.selectedItems()
        if not items:
            self.current_tag = None
            self.detail.setDisabled(True)
            return

        tag = items[0].text()
        self.current_tag = tag
        self.f_tag_label.setText(tag)
        self._load_tag_ui(tag)
        self.detail.setDisabled(False)

    def _load_tag_ui(self, tag: str):
        """Load meta + files into UI for the selected tag."""
        # files
        files = self.sfx.get(tag, [])
        self.table.setRowCount(0)
        for fname in files:
            self._append_file_row(fname)

        # meta
        m = self.meta.get(tag, dict(DEFAULT_META))
        # bus
        bi = self.bus_combo.findText(m.get("bus", DEFAULT_BUS))
        self.bus_combo.setCurrentIndex(bi if bi >= 0 else self.bus_combo.findText(DEFAULT_BUS))
        # category list
        self._fill_category_combo()
        cat = m.get("category", DEFAULT_CATEGORY)
        if cat:
            ci = self.category_combo.findText(cat)
            if ci >= 0:
                self.category_combo.setCurrentIndex(ci)
            else:
                self.category_combo.setEditText(cat)
        else:
            self.category_combo.setEditText("")
        # options
        self.vol_slider.setValue(int(round(float(m.get("volume", 1.0)) * 100)))
        self.loop_chk.setChecked(bool(m.get("loop", False)))
        self.fadein_spin.setValue(int(m.get("fade_in_ms", 0)))
        self.fadeout_spin.setValue(int(m.get("fade_out_ms", 0)))
        self.jitter_slider.setValue(int(round(float(m.get("vol_jitter", 0.0)) * 100)))
        self.pan_slider.setValue(int(round(float(m.get("pan", 0.0)) * 100)))

    def _append_file_row(self, filename: str):
        row = self.table.rowCount()
        self.table.insertRow(row)
        item = QTableWidgetItem(filename)
        exists = (self.sfx_dir / filename).exists()
        if not exists:
            item.setForeground(QBrush(QColor("red")))
        self.table.setItem(row, 0, item)

    # ---------------------------
    # Tag ops
    # ---------------------------
    def _on_new(self):
        base = "new_sound"
        existing = list(self.sfx.keys())
        new_name = unique_id(base, existing)
        self.sfx[new_name] = []
        self.meta[new_name] = dict(DEFAULT_META)
        self._reload_list()
        matches = self.list.findItems(new_name, Qt.MatchExactly)
        if matches:
            self.list.setCurrentItem(matches[0])

    def _on_dup(self):
        if not self.current_tag:
            return
        existing = list(self.sfx.keys())
        new_name = unique_id(self.current_tag, existing)
        self.sfx[new_name] = list(self.sfx.get(self.current_tag, []))
        self.meta[new_name] = dict(self.meta.get(self.current_tag, DEFAULT_META))
        self._reload_list()
        matches = self.list.findItems(new_name, Qt.MatchExactly)
        if matches:
            self.list.setCurrentItem(matches[0])

    def _on_delete(self):
        if not self.current_tag:
            return
        tag = self.current_tag
        reply = QMessageBox.question(self, "Delete Tag", f"Delete tag '{tag}'?",
                                     QMessageBox.Yes | QMessageBox.No)
        if reply == QMessageBox.Yes:
            self.sfx.pop(tag, None)
            self.meta.pop(tag, None)
            self.current_tag = None
            self._reload_list()
            self.detail.setDisabled(True)
            self._refresh_category_filter()

    def _on_rename(self):
        if not self.current_tag:
            return
        current = self.current_tag
        new_name, ok = QInputDialog.getText(self, "Rename Tag", "New name:", text=current)
        if not ok:
            return
        new_name = new_name.strip()
        if not new_name or new_name == current:
            return
        if new_name in self.sfx:
            QMessageBox.warning(self, "Rename error", f"Tag '{new_name}' already exists.")
            return
        # move both maps
        self.sfx[new_name] = self.sfx.pop(current, [])
        self.meta[new_name] = self.meta.pop(current, dict(DEFAULT_META))
        self.current_tag = new_name
        self._reload_list()
        matches = self.list.findItems(new_name, Qt.MatchExactly)
        if matches:
            self.list.setCurrentItem(matches[0])
        self._refresh_category_filter()

    # ---------------------------
    # File ops
    # ---------------------------
    def _on_add_file(self):
        if not self.current_tag:
            return
        self.sfx_dir.mkdir(parents=True, exist_ok=True)
        fnames, _ = QFileDialog.getOpenFileNames(
            self, "Choose Audio Files", str(self.root),
            "Audio Files (*.wav *.mp3 *.ogg *.flac);;All Files (*)"
        )
        if not fnames:
            return
        for fname in fnames:
            src = Path(fname)
            dest = self.sfx_dir / src.name
            try:
                if not dest.exists():
                    shutil.copy2(src, dest)
            except Exception as e:
                QMessageBox.critical(self, "Copy error", f"Failed to copy file:\n{e}")
                continue
            self.sfx.setdefault(self.current_tag, []).append(src.name)
            self._append_file_row(src.name)

    def _on_remove_file(self):
        if not self.current_tag:
            return
        row = self.table.currentRow()
        if row < 0:
            return
        fname_item = self.table.item(row, 0)
        fname = fname_item.text() if fname_item else ""
        files = self.sfx.get(self.current_tag, [])
        if fname in files:
            files.remove(fname)
        self.table.removeRow(row)

    # ---------------------------
    # Playback / preview
    # ---------------------------
    def _effective_preview_volume(self, tag: str) -> float:
        m = self.meta.get(tag, DEFAULT_META)
        vol = float(m.get("volume", 1.0))
        jitter = float(m.get("vol_jitter", 0.0))
        if jitter > 0:
            vol *= random.uniform(max(0.0, 1.0 - jitter), 1.0 + jitter)
        # bus + master
        bus = m.get("bus", DEFAULT_BUS)
        bus_gain = self.bus_gains.get(bus, 1.0)
        master = self.bus_gains.get("Master", 1.0)
        return max(0.0, min(1.0, vol * bus_gain * master))

    def _apply_pan(self, channel: Optional[pygame.mixer.Channel], pan: float, volume: float):
        """Apply stereo panning by setting left/right channel volume."""
        if channel is None:
            return
        # pan -1..1 -> left/right gains
        pan = max(-1.0, min(1.0, pan))
        left = volume * (1.0 - max(0.0, pan))
        right = volume * (1.0 + min(0.0, pan))
        # clamp
        left = max(0.0, min(1.0, left))
        right = max(0.0, min(1.0, right))
        try:
            channel.set_volume(left, right)
        except TypeError:
            # Fallback if stereo control unavailable; set overall volume
            channel.set_volume(volume)

    def _on_play_file(self):
        row = self.table.currentRow()
        if row < 0 or not self.current_tag:
            return
        fname_item = self.table.item(row, 0)
        fname = fname_item.text() if fname_item else ""
        if not fname:
            return
        path = self.sfx_dir / fname
        if not path.exists():
            QMessageBox.warning(self, "Missing file", f"File '{fname}' does not exist.")
            return

        m = self.meta.get(self.current_tag, DEFAULT_META)
        vol = self._effective_preview_volume(self.current_tag)
        fade_in = int(m.get("fade_in_ms", 0))
        pan = float(m.get("pan", 0.0))

        try:
            sound = pygame.mixer.Sound(str(path))
            chan = sound.play(loops=0, fade_ms=fade_in)
            # Apply pan/volume
            if chan is not None:
                self._apply_pan(chan, pan, vol)
            else:
                sound.set_volume(vol)
        except Exception as e:
            QMessageBox.critical(self, "Playback error", f"Failed to play audio:\n{e}")

    def _on_play_tag(self):
        if not self.current_tag:
            return
        files = self.sfx.get(self.current_tag, [])
        if not files:
            QMessageBox.information(self, "Play Tag", "This tag has no audio files assigned.")
            return
        fname = random.choice(files)
        path = self.sfx_dir / fname
        if not path.exists():
            QMessageBox.warning(self, "Missing file", f"File '{fname}' does not exist.")
            return

        m = self.meta.get(self.current_tag, DEFAULT_META)
        loops = -1 if bool(m.get("loop", False)) else 0
        vol = self._effective_preview_volume(self.current_tag)
        fade_in = int(m.get("fade_in_ms", 0))
        pan = float(m.get("pan", 0.0))

        try:
            sound = pygame.mixer.Sound(str(path))
            chan = sound.play(loops=loops, fade_ms=fade_in)
            if chan is not None:
                self._apply_pan(chan, pan, vol)
            else:
                sound.set_volume(vol)
        except Exception as e:
            QMessageBox.critical(self, "Playback error", f"Failed to play audio:\n{e}")

    # ---------------------------
    # Meta / mixer events
    # ---------------------------
    def _on_meta_changed(self, *args, **kwargs):
        """Collect current UI fields and update the meta for current tag."""
        if not self.current_tag:
            return
        m = self.meta.setdefault(self.current_tag, dict(DEFAULT_META))
        m["bus"] = self.bus_combo.currentText() if self.bus_combo.currentText() in BUS_NAMES else DEFAULT_BUS

        # Category: from combo box edit/current text
        cat = self.category_combo.currentText().strip()
        m["category"] = cat

        # options
        m["volume"] = self.vol_slider.value() / 100.0
        m["loop"] = self.loop_chk.isChecked()
        m["fade_in_ms"] = int(self.fadein_spin.value())
        m["fade_out_ms"] = int(self.fadeout_spin.value())
        m["vol_jitter"] = self.jitter_slider.value() / 100.0
        m["pan"] = self.pan_slider.value() / 100.0

        # Update filters’ source of truth when categories change
        self._refresh_category_filter()

    def _on_bus_gain_changed(self, bus_name: str, slider_val: int):
        self.bus_gains[bus_name] = max(0.0, min(1.0, slider_val / 100.0))

