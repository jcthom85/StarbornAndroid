# -*- coding: utf-8 -*-
# tools/cutscene_editor.py
from __future__ import annotations

import json, time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

from PyQt5.QtCore import Qt
from PyQt5.QtGui import QColor, QIcon, QKeySequence
from PyQt5.QtWidgets import (
    QWidget, QSplitter, QListWidget, QListWidgetItem, QVBoxLayout, QHBoxLayout,
    QPushButton, QMessageBox, QLineEdit, QLabel, QFormLayout, QComboBox, QShortcut, QInputDialog,
    QDoubleSpinBox, QSpinBox, QTextEdit, QTableWidget, QTableWidgetItem,
    QAbstractItemView, QFileDialog, QCheckBox, QColorDialog, QUndoStack
)
from PyQt5.QtWidgets import QGroupBox
from data_core import detect_project_root, json_load, json_save, unique_id
from ui_common import attach_hotkeys, attach_list_context_menu, flash_status
from tools.cutscene_editor_commands import ChangeValueCmd, AddStepCmd, DeleteStepCmd, MoveStepCmd

# --------------------------- Step schema ---------------------------

@dataclass
class FieldSpec:
    label: str
    kind: str                      # 'str','text','float','int','enum','enum_opt','bool','vec2','color','color4','json','opt_float','opt_color','opt_color4','opt_str'
    json_key: Optional[str] = None # If None, derived from label
    enum: Tuple[str, ...] = ()
    default: Any = None

    def __post_init__(self):
        if self.json_key is None:
            # Auto-derive json_key from label, e.g., "Duration (s)" -> "duration"
            self.json_key = self.label.split(" ")[0].split("(")[0].lower()


STEP_SPECS: Dict[str, List[FieldSpec]] = { # label, kind, json_key (optional), enum, default
    "dialogue": [
        FieldSpec(label="Actor", kind="str",  json_key="actor", default=""),
        FieldSpec(label="Line",  kind="text", json_key="line",  default=""),
        FieldSpec(label="Hint Placement", kind="enum_opt", json_key="placement", enum=("top","middle","bottom")),
        FieldSpec(label="Hint Width (0..1)", kind="opt_float", json_key="width_ratio", default=None),
        FieldSpec(label="Hint Margin (px)", kind="opt_float", json_key="vertical_margin", default=None),
    ],
    "wait": [
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=1.0),
    ],
    "fade": [
        FieldSpec(label="Alpha (0..1)", kind="float", json_key="alpha", default=1.0),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=0.5),
        FieldSpec(label="Color (r,g,b)", kind="color", json_key="color", default=[0,0,0]),
        FieldSpec(label="From Alpha (opt)", kind="opt_float", json_key="from_alpha"),
        FieldSpec(label="From Color (opt)", kind="opt_color", json_key="from_color"),
    ],
    "letterbox": [
        FieldSpec(label="Height (px)", kind="int", json_key="height", default=80),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=0.3),
    ],
    "screen_shake": [
        FieldSpec(label="Magnitude (px)", kind="float", json_key="magnitude", default=12.0),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=0.35),
        FieldSpec(label="Axis", kind="enum",  json_key="axis", enum=("x","y","both"), default="both"),
    ],
    "zoom_camera": [
        FieldSpec(label="Scale", kind="float", json_key="scale", default=1.0),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=1.0),
        FieldSpec(label="Ease", kind="str", json_key="ease", default="in_out_quad"),
        FieldSpec(label="Pos Hint (0..1, 0..1)", kind="vec2", json_key="pos_hint", default=None),
    ],
    "pan_camera": [
        FieldSpec(label="Î”X (px)", kind="float", json_key="x", default=0.0),
        FieldSpec(label="Î”Y (px)", kind="float", json_key="y", default=0.0),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=1.0),
        FieldSpec(label="Ease", kind="str", json_key="ease", default="in_out_quad"),
    ],
    "flash": [
        FieldSpec(label="Color (r,g,b)", kind="color", json_key="color", default=[1,1,1]),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=0.15),
    ],
    "flashback_overlay": [
        FieldSpec(label="Alpha (0..1)", kind="float", json_key="alpha", default=0.8),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=0.4),
        FieldSpec(label="Hold (s)", kind="float", json_key="hold", default=1.0),
    ],
    "color_filter": [
        FieldSpec(label="Color (r,g,b)", kind="color", json_key="color", default=[0,0,0]),
        FieldSpec(label="Alpha (0..1)", kind="float", json_key="alpha", default=1.0),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=0.5),
    ],
    "particles": [
        # Editor exposes a JSON box for config â€“ we normalize on save to {"config": {...}}
        FieldSpec(label="Config (dict)", kind="json", json_key="config", default={"pos_hint":[0.5,0.5]}),
    ],
    "caption": [
        FieldSpec(label="Text", kind="str",   json_key="text", default=""),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=1.5),
    ]
    ,
    "fade_in_exploration": [
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=1.8),
    ]
    ,
    # New cinematic-only steps (previewable via game hot-load)
    "narration": [
        FieldSpec(label="Text (markup)", kind="text",  json_key="text", default="[i]Narration panel.[/i]"),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=2.0),
        FieldSpec(label="Position", kind="enum",  json_key="position", enum=("top","center","bottom"), default="bottom"),
        FieldSpec(label="Width (0..1)", kind="float", json_key="width", default=0.82),
        FieldSpec(label="Fade In (s)", kind="float", json_key="fade_in", default=0.18),
        FieldSpec(label="Fade Out (s)", kind="float", json_key="fade_out", default=0.25),
    ],
    "cinematic_text": [
        FieldSpec(label="Text (markup)", kind="text",  json_key="text", default="[i]Cinematic text.[/i]"),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=2.2),
        FieldSpec(label="Position", kind="enum",  json_key="position", enum=("top","center","bottom"), default="center"),
        FieldSpec(label="Width (0..1)", kind="float", json_key="width", default=0.9),
        FieldSpec(label="Dim Alpha", kind="float", json_key="dim_alpha", default=0.28),
    ],
    "wait_for_player_action": [
        FieldSpec(label="Action", kind="str", json_key="action", default=""),
        FieldSpec(label="Item ID (opt)", kind="opt_str", json_key="item_id", default=""),
        FieldSpec(label="Hint Text", kind="text", json_key="hint", default=""),
        FieldSpec(label="Hint Placement", kind="enum_opt", json_key="placement", enum=("top","middle","bottom")),
        FieldSpec(label="Hint Width (0..1)", kind="opt_float", json_key="width_ratio", default=None),
        FieldSpec(label="Hint Margin (px)", kind="opt_float", json_key="vertical_margin", default=None),
    ],
    "speed_lines": [
        FieldSpec(label="Count", kind="int",     json_key="count", default=36),
        FieldSpec(label="Duration (s)", kind="float",   json_key="duration", default=0.5),
        FieldSpec(label="Thickness", kind="opt_float", json_key="thickness", default=None),
        FieldSpec(label="Color (r,g,b,a)", kind="opt_color4", json_key="color", default=None),
    ],
    "ring": [
        FieldSpec(label="Start Radius", kind="float", json_key="start", default=24.0),
        FieldSpec(label="End Radius", kind="float", json_key="end", default=480.0),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=0.5),
        FieldSpec(label="Color (r,g,b,a)", kind="opt_color4", json_key="color", default=None),
        FieldSpec(label="Thickness", kind="opt_float", json_key="thickness", default=None),
    ],
    "vignette": [
        FieldSpec(label="Alpha (0..1)", kind="float", json_key="alpha", default=0.35),
        FieldSpec(label="Inset (px)", kind="int",   json_key="inset", default=120),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=0.8),
    ],
    "bar_wipe": [
        FieldSpec(label="Orientation", kind="enum",  json_key="orientation", enum=("vertical","horizontal"), default="vertical"),
        FieldSpec(label="Direction", kind="enum",  json_key="direction", enum=("in","out"), default="in"),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=0.5),
        FieldSpec(label="Color (r,g,b,a)", kind="opt_color4", json_key="color", default=None),
    ],
    "tilt_camera": [
        FieldSpec(label="Angle (deg)", kind="float", json_key="angle", default=6.0),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=0.25),
        FieldSpec(label="Restore", kind="bool",  json_key="restore", default=True),
    ],

    # Input / flow helpers
    "wait_for_click": [
        FieldSpec(label="Note", kind="opt_str", json_key="note", default="")
    ],
    "label": [ FieldSpec(label="Id", kind="str", json_key="id", default="label1") ],
    "goto":  [ FieldSpec(label="Target", kind="str", json_key="target", default="label1") ],

    # Audio
    "sound": [
        FieldSpec(label="SFX Tag", kind="str",       json_key="id", default=""),
        FieldSpec(label="Volume", kind="opt_float", json_key="volume", default=None),
        FieldSpec(label="Fade (s)", kind="opt_float", json_key="fade", default=None),
        FieldSpec(label="Pan (-1..1)", kind="opt_float", json_key="pan", default=None),
    ],
    "music": [
        FieldSpec(label="Action", kind="enum",      json_key="action", enum=("play","stop"), default="play"),
        FieldSpec(label="Music Tag", kind="opt_str",   json_key="id", default=""),
        FieldSpec(label="Volume", kind="opt_float", json_key="volume", default=None),
        FieldSpec(label="Fade (s)", kind="opt_float", json_key="fade", default=None),
    ],

    # New VFX library
    "shockwave": [
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=0.4),
        FieldSpec(label="Start Scale", kind="float", json_key="start_scale", default=0.98),
        FieldSpec(label="End Scale", kind="float", json_key="end_scale", default=1.25),
        FieldSpec(label="Alpha", kind="float", json_key="alpha", default=0.45),
    ],
    "glitch": [
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=0.18),
        FieldSpec(label="Jitter (px)", kind="float", json_key="jitter", default=2.0),
    ],
    "weather": [
        FieldSpec(label="Kind", kind="enum",  json_key="kind", enum=("rain","snow"), default="rain"),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=2.0),
        FieldSpec(label="Intensity", kind="enum",  json_key="intensity", enum=("low","medium","high"), default="medium"),
    ],
    "depth_focus": [
        FieldSpec(label="Center (0..1, 0..1)", kind="vec2",  json_key="center", default=None),
        FieldSpec(label="Radius (px)", kind="float", json_key="radius", default=120.0),
        FieldSpec(label="Blur", kind="int",   json_key="blur", default=4),
        FieldSpec(label="Dim Alpha", kind="float", json_key="dim_alpha", default=0.0),
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=0.8),
        FieldSpec(label="Fade In (s)", kind="float", json_key="fade_in", default=0.15),
        FieldSpec(label="Fade Out (s)", kind="float", json_key="fade_out", default=0.25),
    ],
    "hit_stop": [
        FieldSpec(label="Duration (s)", kind="float", json_key="duration", default=0.12),
        FieldSpec(label="Zoom", kind="float", json_key="zoom", default=1.03),
    ],
}

# Simple, readable summariess for the steps list
def summarize_step(step: Dict[str, Any]) -> str:
    t = step.get("type","?")
    if t == "dialogue":
        return f'{step.get("actor","?")}: {step.get("line","")[:60]}'
    if t == "wait":
        return f'{step.get("duration", 0)}s'
    if t == "wait_for_player_action":
        act = step.get("action", "")
        item = step.get("item_id", "")
        suffix = f":{item}" if item else ""
        return f'{act}{suffix}'
    if t == "fade":
        return f'Î±={step.get("alpha",1)} for {step.get("duration",0.5)}s'
    if t == "letterbox":
        return f'h={step.get("height",0)} for {step.get("duration",0.3)}s'
    if t == "screen_shake":
        return f'mag={step.get("magnitude",10)}, {step.get("duration",0.3)}s, axis={step.get("axis","both")}'
    if t == "zoom_camera":
        return f'scale={step.get("scale",1)} dur={step.get("duration",1)}'
    if t == "pan_camera":
        return f'({step.get("x",0)}, {step.get("y",0)}) {step.get("duration",1)}s'
    if t == "flash":
        return f'color={step.get("color",[1,1,1])} {step.get("duration",0.15)}s'
    if t == "flashback_overlay":
        return f'Î±={step.get("alpha",0.8)} hold={step.get("hold",1)}'
    if t == "color_filter":
        return f'color={step.get("color",[0,0,0])} Î±={step.get("alpha",1)}'
    if t == "particles":
        return "config" if "config" in step else "config (implicit)"
    if t == "caption":
        return f'"{step.get("text","")[:32]}" {step.get("duration",1.5)}s'
    if t == "narration":
        return f'"{step.get("text","")[:32]}" {step.get("duration",2.0)}s @ {step.get("position","bottom")}'
    if t == "cinematic_text":
        return f'"{step.get("text","")[:32]}" {step.get("duration",2.2)}s dim={step.get("dim_alpha",0.28)}'
    if t == "speed_lines":
        return f'count={step.get("count",36)} {step.get("duration",0.5)}s'
    if t == "ring":
        return f'{step.get("start",24)}->{step.get("end",480)} {step.get("duration",0.5)}s'
    if t == "vignette":
        return f'alpha={step.get("alpha",0.35)} inset={step.get("inset",120)} {step.get("duration",0.8)}s'
    if t == "bar_wipe":
        return f'{step.get("orientation","vertical")} {step.get("direction","in")} {step.get("duration",0.5)}s'
    if t == "tilt_camera":
        return f'{step.get("angle",6)}deg {step.get("duration",0.25)}s restore={bool(step.get("restore",True))}'
    if t == "wait_for_click":
        return 'wait for click'
    if t == "label":
        return f'label:{step.get("id","?")}'
    if t == "goto":
        return f'goto:{step.get("target","?")}'
    if t == "sound":
        return f'sfx:{step.get("id", step.get("tag","")) or "?"}'
    if t == "music":
        return f'music {step.get("action","play")} {step.get("id", step.get("tag","")) or ""}'
    if t == "shockwave":
        return f'{step.get("duration",0.4)}s scale {step.get("start_scale",0.98)}->{step.get("end_scale",1.25)}'
    if t == "glitch":
        return f'{step.get("duration",0.18)}s jitter={step.get("jitter",2.0)}'
    if t == "weather":
        return f'{step.get("kind","rain")} {step.get("intensity","medium")} {step.get("duration",2.0)}s'
    if t == "depth_focus":
        return f'r={step.get("radius",120)} blur={step.get("blur",4)} {step.get("duration",0.8)}s'
    if t == "hit_stop":
        return f'{step.get("duration",0.12)}s z={step.get("zoom",1.03)}'
    # fallback
    return json.dumps({k:v for k,v in step.items() if k != "type"})

# --------------------------- Editor widget ---------------------------

class CutsceneEditor(QWidget):
    """
    Studio Pro plugin: edits cinematics.json (dict of scene_id -> list of steps).
    """
    TITLE = "Cutscene Editor"

    def __init__(self, project_root: Optional[Path]=None):
        super().__init__()
        self.setObjectName("CutsceneEditor")
        self.root = detect_project_root(project_root)
        self.path = self.root / "cinematics.json"
        self.undo_stack = QUndoStack(self)
        self._dirty = False

        self._scenes: Dict[str, List[Dict[str, Any]]] = {}
        self._scene_order: List[str] = []  # maintain stable order in UI
        self._current_id: Optional[str] = None
        self._current_step_index: int = -1

        # audio resources for pickers
        self._sfx_tags: List[str] = []
        self._music_tags: List[str] = []
        self._load_audio_resources()

        self._build_ui()
        self._load()
        # Undo/Redo shortcuts
        QShortcut(QKeySequence.Undo, self, self.undo_stack.undo)
        QShortcut(QKeySequence.Redo, self, self.undo_stack.redo)
        # Other standard shortcuts
        attach_hotkeys(self, save_cb=self.save, search_cb=self._focus_scene_filter)


    # ---------------------- UI Construction ----------------------
    def _build_ui(self):
        layout = QVBoxLayout(self)
        header = QHBoxLayout()
        self.path_lbl = QLabel(str(self.path))
        btn_pick = QPushButton("â€¦")
        btn_pick.setToolTip("Choose a different cinematics.json")
        btn_pick.clicked.connect(self._choose_json)
        header.addWidget(QLabel("File:"))
        header.addWidget(self.path_lbl, 1)
        header.addWidget(btn_pick)
        try: btn_pick.setText("…")
        except Exception: pass
        layout.addLayout(header)

        splitter = QSplitter(self); splitter.setOrientation(Qt.Horizontal)
        layout.addWidget(splitter, 1)

        # Left: scene list + toolbar
        left = QWidget(); lv = QVBoxLayout(left)
        filt_row = QHBoxLayout()
        self.scene_filter = QLineEdit(); self.scene_filter.setPlaceholderText("Filter scenesâ€¦")
        self.scene_filter.textChanged.connect(self._filter_scenes)
        try: self.scene_filter.setPlaceholderText("Filter scenes…")
        except Exception: pass
        filt_row.addWidget(self.scene_filter, 1)
        lv.addLayout(filt_row)

        self.scene_list = QListWidget()
        self.scene_list.itemSelectionChanged.connect(self._on_scene_selected)
        lv.addWidget(self.scene_list, 1)

        btns = QHBoxLayout()
        b_new = QPushButton("New");      b_new.clicked.connect(self._new_scene)
        b_dup = QPushButton("Duplicate"); b_dup.clicked.connect(self._dup_scene)
        b_ren = QPushButton("Rename");    b_ren.clicked.connect(self._rename_scene)
        b_del = QPushButton("Delete");    b_del.clicked.connect(self._del_scene)
        btns.addWidget(b_new); btns.addWidget(b_dup); btns.addWidget(b_ren); btns.addWidget(b_del)
        lv.addLayout(btns)

        attach_list_context_menu(self.scene_list, on_new=self._new_scene, on_dup=self._dup_scene, on_del=self._del_scene)
        splitter.addWidget(left)

        # Right: steps + step editor only
        right = QSplitter(self); right.setOrientation(Qt.Vertical)
        splitter.addWidget(right); splitter.setStretchFactor(1, 1)


        # Steps table
        steps_box = QWidget(); sb = QVBoxLayout(steps_box)
        row1 = QHBoxLayout()
        self.add_type_combo = QComboBox()
        for t in sorted(STEP_SPECS.keys()):
            self.add_type_combo.addItem(t)
        row1.addWidget(QLabel("Add step:")); row1.addWidget(self.add_type_combo)
        b_add = QPushButton("Add");      b_add.clicked.connect(self._add_step)
        b_dup = QPushButton("Duplicate"); b_dup.clicked.connect(self._dup_step)
        b_del = QPushButton("Delete");    b_del.clicked.connect(self._del_step)
        b_up  = QPushButton("â–²");         b_up.clicked.connect(lambda: self._move_step(-1))
        b_dn  = QPushButton("â–¼");         b_dn.clicked.connect(lambda: self._move_step(+1))
        b_play = QPushButton("Play in Game"); b_play.clicked.connect(self._play_in_game)
        row1.addWidget(b_add); row1.addWidget(b_dup); row1.addWidget(b_del); row1.addStretch(1); row1.addWidget(b_play); row1.addWidget(b_up); row1.addWidget(b_dn)
        # Normalize arrow labels in case of encoding issues
        try:
            b_add.setIcon(QIcon.fromTheme("list-add"))
            b_del.setIcon(QIcon.fromTheme("list-remove"))
            b_up.setText("▲")
            b_dn.setText("▼")
            b_play.setIcon(QIcon.fromTheme("media-playback-start"))
        except Exception:
            pass
        sb.addLayout(row1)

        self.steps = QTableWidget(0, 2)
        self.steps.setHorizontalHeaderLabels(["Type", "Summary"])
        self.steps.verticalHeader().setVisible(True)
        self.steps.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.steps.setEditTriggers(QAbstractItemView.NoEditTriggers)
        self.steps.itemSelectionChanged.connect(self._on_step_selected)
        sb.addWidget(self.steps, 1)
        right.addWidget(steps_box)
        
        # Dynamic step editor
        self.step_form_box = QWidget()
        self._step_form_layout = QFormLayout(self.step_form_box)
        
        # Scene-level metadata
        meta_box = QGroupBox("Scene Metadata")
        meta_form = QFormLayout(meta_box)
        self.repeatable_chk = QCheckBox("Repeatable")
        self.scene_notes = QTextEdit()
        self.scene_notes.setPlaceholderText("Notes about this scene's purpose, triggers, etc.")
        meta_form.addRow(self.repeatable_chk)
        meta_form.addRow(QLabel("Notes:"), self.scene_notes)
        
        # raw JSON view (for individual steps)
        self.raw_json = QTextEdit(); self.raw_json.setPlaceholderText("Raw step JSON (read/write).")
        self.raw_json.textChanged.connect(self._raw_changed)
        right.addWidget(self.step_form_box)
        right.addWidget(meta_box)
        right.addWidget(self.raw_json)
        right.setStretchFactor(0, 3)
        right.setStretchFactor(1, 2)
        right.setStretchFactor(2, 2)

        # Footer
        bar = QHBoxLayout()
        self.btn_save = QPushButton("Save")
        self.btn_save.clicked.connect(self.save)
        bar.addStretch(1)
        bar.addWidget(self.btn_save)
        layout.addLayout(bar)

        # Connect metadata signals
        self.repeatable_chk.toggled.connect(self._on_meta_changed)
        self.scene_notes.textChanged.connect(self._on_meta_changed)

    # ---------------------- Loading / Saving ----------------------
    def _load(self):
        data = json_load(self.path, default={}) # type: ignore
        if not isinstance(data, dict):
            data = {}
        self._scenes = {k: (v if isinstance(v, list) else []) for k, v in data.items()}
        self._scene_order = sorted(self._scenes.keys(), key=str.lower)
        self._refresh_scene_list()
        self._set_dirty(False)

    def save(self) -> bool:
        if self._current_id is not None:
            self._flush_current_editor_into_scene()

        # normalize particle steps: ensure {"config": {...}}
        for sid, steps in self._scenes.items():
            # Ensure scene metadata is up-to-date from the form
            if sid == self._current_id:
                meta_step = steps[0] if steps and isinstance(steps[0], dict) and steps[0].get("type") == "scene_meta" else None
                if meta_step:
                    meta_step["repeatable"] = self.repeatable_chk.isChecked()
                    meta_step["notes"] = self.scene_notes.toPlainText()

            # Normalize particle steps
            for s in steps:
                if s.get("type") == "particles":
                    if "config" not in s:
                        # gather any top-level fields commonly used and tuck into config
                        cfg = s.get("config", {})
                        for k in ("preset","pos_hint","count","life","size","velocity","angle","color","ttl"):
                            if k in s and k not in cfg:
                                cfg[k] = s.pop(k)
                        s["config"] = cfg

        ok = json_save(self.path, self._scenes, sort_obj=True, indent=2)
        if ok is None:  # json_save returns None on success in our helper
            ok = True
        if ok:
            flash_status(self, "Saved cinematics.json")
            self._set_dirty(False)
            return True
        QMessageBox.critical(self, "Save Error", f"Failed to save:\n{self.path}")
        return False

    def _play_in_game(self):
        if not self._current_id:
            QMessageBox.information(self, "Preview", "Select a scene to preview.")
            return
        # Save all changes first to ensure preview is up-to-date
        if not self.save():
            QMessageBox.warning(self, "Preview", "Could not save changes. Preview aborted.")
            return
        # Write the request
        preview_file = self.root / "preview_request.json"
        request = {"action": "play_cutscene", "scene_id": self._current_id, "timestamp": time.time()}
        preview_file.write_text(json.dumps(request), encoding="utf-8")
        flash_status(self, f"Sent '{self._current_id}' to game for preview.")

    # ---------------------- Scene list ops ----------------------
    def _refresh_scene_list(self):
        self.scene_list.clear()
        q = self.scene_filter.text().strip().lower()
        for sid in self._scene_order:
            if q and q not in sid.lower():
                continue
            it = QListWidgetItem(sid)
            self.scene_list.addItem(it)
        if self.scene_list.count() and self.scene_list.currentRow() < 0:
            self.scene_list.setCurrentRow(0)

    def _filter_scenes(self, _):
        self._refresh_scene_list()

    def _focus_scene_filter(self):
        self.scene_filter.setFocus(Qt.ShortcutFocusReason)

    def _load_audio_resources(self):
        try:
            sfx = json_load(self.root / "sfx.json", default={})
            if isinstance(sfx, dict):
                self._sfx_tags = sorted([str(k) for k in sfx.keys()])
        except Exception:
            self._sfx_tags = []
        try:
            ab = json_load(self.root / "audio_bindings.json", default={})
            mus = []
            if isinstance(ab, dict):
                m = ab.get("music")
                if isinstance(m, dict):
                    for v in m.values():
                        if isinstance(v, str): mus.append(v)
            # include sfx tags too; music tags normally live in sfx.json as playable assets
            self._music_tags = sorted(set(mus + self._sfx_tags))
        except Exception:
            self._music_tags = list(self._sfx_tags)

    def _on_scene_selected(self):
        it = self.scene_list.currentItem()
        if not it: 
            self._current_id = None
            self._clear_steps()
            return
        # flush any form edits into previous scene first
        self._flush_current_editor_into_scene()
        self._current_id = it.text()
        self.undo_stack.clear()
        self._populate_steps_table()

        # Load scene metadata
        scene = self._scenes.get(self._current_id, [])
        meta = scene[0] if scene and isinstance(scene[0], dict) and scene[0].get("type") == "scene_meta" else {}
        self.repeatable_chk.setChecked(bool(meta.get("repeatable", False)))
        self.scene_notes.setPlainText(meta.get("notes", ""))

        if self.steps.rowCount() > 0:
            self.steps.selectRow(0)
        else:
            self._build_step_form(None)
            self._update_raw(None)
        self._set_dirty(False)

    def _new_scene(self):
        base = "new_scene"
        sid = unique_id(base, self._scene_order)
        self._scene_order.append(sid)
        # Add a default metadata step to new scenes
        self._scenes[sid] = [{"type": "scene_meta", "repeatable": False, "notes": ""}]
        self._refresh_scene_list()
        self._select_scene(sid)
        self._set_dirty(True)

    def _dup_scene(self):
        it = self.scene_list.currentItem()
        if not it: return
        orig = it.text()
        new_id = unique_id(orig, self._scene_order)
        self._scene_order.append(new_id)
        self._scenes[new_id] = json.loads(json.dumps(self._scenes.get(orig, [])))
        self._refresh_scene_list()
        self._select_scene(new_id)
        self._set_dirty(True)

    def _rename_scene(self):
        it = self.scene_list.currentItem()
        if not it: return
        old = it.text()
        new, ok = QInputDialogWithText.get(self, "Rename Scene", "New id:", old)
        if not ok or not new.strip(): return
        new = new.strip()
        if new in self._scenes and new != old:
            QMessageBox.warning(self, "Exists", f"A scene named '{new}' already exists.")
            return
        self._scenes[new] = self._scenes.pop(old)
        self._scene_order = [new if x == old else x for x in self._scene_order]
        self._refresh_scene_list()
        self._select_scene(new)
        self._set_dirty(True)

    def _del_scene(self):
        it = self.scene_list.currentItem()
        if not it: return
        sid = it.text()
        if QMessageBox.question(self, "Delete Scene", f"Delete scene '{sid}'?", QMessageBox.Yes|QMessageBox.No) != QMessageBox.Yes:
            return
        self._scenes.pop(sid, None)
        self._scene_order = [x for x in self._scene_order if x != sid]
        self._refresh_scene_list()
        self._set_dirty(True)

    def _select_scene(self, sid: str):
        for i in range(self.scene_list.count()):
            if self.scene_list.item(i).text() == sid:
                self.scene_list.setCurrentRow(i)
                return

    # ---------------------- Steps table ops ----------------------
    def _clear_steps(self):
        self.steps.setRowCount(0)
        self.scene_notes.setPlainText("")
        self.repeatable_chk.setChecked(False)
        self._build_step_form(None)

    def _populate_steps_table(self):
        self.steps.setRowCount(0)
        # Skip the metadata step when populating the table
        scene_steps = self._scenes.get(self._current_id, [])
        for s in scene_steps:
            if s.get("type") == "scene_meta":
                continue
            self._append_step_row(s)

        # Load scene metadata into the dedicated widgets
        meta_step = scene_steps[0] if scene_steps and isinstance(scene_steps[0], dict) and scene_steps[0].get("type") == "scene_meta" else {}
        self.repeatable_chk.blockSignals(True)
        self.scene_notes.blockSignals(True)
        self.repeatable_chk.setChecked(bool(meta_step.get("repeatable", False)))
        self.scene_notes.setPlainText(meta_step.get("notes", ""))
        self.repeatable_chk.blockSignals(False)
        self.scene_notes.blockSignals(False)

        if self.steps.rowCount():
            self.steps.selectRow(0)
        else:
            self._build_step_form(None)

    def _append_step_row(self, step: Dict[str, Any]):
        r = self.steps.rowCount()
        self.steps.insertRow(r)
        self.steps.setItem(r, 0, QTableWidgetItem(step.get("type","?")))
        self.steps.setItem(r, 1, QTableWidgetItem(summarize_step(step)))

    def _on_step_selected(self):
        step = self._get_selected_step()
        self._build_step_form(step)
        self._update_raw(step)

    def _get_selected_step(self) -> Optional[Dict[str, Any]]:
        sid = self._current_id
        if not sid: return None
        sel = self.steps.currentRow()
        if sel < 0: return None
        # Adjust index to account for the hidden metadata step
        steps = [s for s in self._scenes.get(sid, []) if s.get("type") != "scene_meta"]
        if sel >= len(steps): return None
        return steps[sel]

    def _add_step(self):
        sid = self._current_id
        if not sid: return
        t = self.add_type_combo.currentText()
        step = self._make_default_step(t)
        
        # The visual index in the table
        visual_insert_at = self.steps.currentRow() + 1 if self.steps.currentRow() >= 0 else self.steps.rowCount()
        # The actual index in the scene list (accounting for metadata)
        actual_insert_at = visual_insert_at + 1

        self.undo_stack.push(AddStepCmd(self, sid, actual_insert_at, step))
        self._populate_steps_table()
        self.steps.selectRow(visual_insert_at)
        self._set_dirty(True)

    def _dup_step(self):
        sid = self._current_id
        step = self._get_selected_step()
        if not (sid and step): return
        i = self.steps.currentRow()
        new_step = json.loads(json.dumps(step))
        # Adjust index for metadata
        actual_insert_at = i + 2
        self.undo_stack.push(AddStepCmd(self, sid, actual_insert_at, new_step))
        self._populate_steps_table()
        self.steps.selectRow(i+1)
        self._set_dirty(True)

    def _del_step(self):
        sid = self._current_id
        if not sid: return
        i = self.steps.currentRow()
        if i < 0: return
        # Adjust index for metadata
        actual_index = i + 1
        step = self._scenes[sid][actual_index]
        self.undo_stack.push(DeleteStepCmd(self, sid, actual_index, step))
        self._populate_steps_table()
        self.steps.selectRow(min(i, self.steps.rowCount()-1))
        self._set_dirty(True)

    def _move_step(self, delta: int):
        sid = self._current_id
        if not sid: return
        i = self.steps.currentRow()
        if i < 0: return
        # Adjust indices for metadata
        actual_i = i + 1
        actual_j = actual_i + delta
        steps = self._scenes[sid]
        if 1 <= actual_j < len(steps): # Ensure we don't move into the metadata slot
            self.undo_stack.push(MoveStepCmd(self, sid, actual_i, actual_j))
            self._populate_steps_table()
            self.steps.selectRow(i + delta)
            self._set_dirty(True)

    def _on_meta_changed(self):
        if not self._current_id:
            return
        scene = self._scenes.get(self._current_id, [])
        if not scene or not (isinstance(scene[0], dict) and scene[0].get("type") == "scene_meta"):
            # No meta step, create one
            meta_step = {"type": "scene_meta", "repeatable": False, "notes": ""}
            scene.insert(0, meta_step)
        else:
            meta_step = scene[0]
        meta_step["repeatable"] = self.repeatable_chk.isChecked()
        meta_step["notes"] = self.scene_notes.toPlainText()
        self._set_dirty(True)

    # ---------------------- Step form ----------------------
    def _make_default_step(self, t: str) -> Dict[str, Any]:
        s: Dict[str, Any] = {"type": t}
        for fs in STEP_SPECS.get(t, []):
            if fs.default is not None:
                s[fs.json_key] = json.loads(json.dumps(fs.default))
        return s

    def _build_step_form(self, step: Optional[Dict[str, Any]]):
        # clear
        while self._step_form_layout.count():
            item = self._step_form_layout.takeAt(0)
            w = item.widget()
            if w is not None:
                w.deleteLater()

        if not step:
            self._step_form_layout.addRow(QLabel("Select a step to edit."))
            return

        t = step.get("type","?")
        self._step_form_layout.addRow(QLabel(f"<b>Type:</b> {t}"))

        specs = STEP_SPECS.get(t)
        if not specs:
            self._step_form_layout.addRow(QLabel("(Unknown type - edit in Raw JSON below)"))
            return

        # create widgets according to spec
        self._edit_widgets: Dict[str, QWidget] = {}
        for fs in specs:
            key = fs.json_key or "unknown"
            optional_kind = fs.kind.startswith("opt_") or fs.kind == "enum_opt"
            val = step.get(key, None if optional_kind else fs.default)

            if fs.kind in ("str","opt_str"):
                # Audio tag pickers when applicable
                if t == "sound" and key == "id" and hasattr(self, "_sfx_tags"):
                    cb = QComboBox(); cb.setEditable(True)
                    for tag in getattr(self, "_sfx_tags", []):
                        cb.addItem(tag)
                    if isinstance(val, str) and val:
                        cb.setCurrentText(val)
                    cb.currentTextChanged.connect(lambda _t, k=key, ww=cb: self._set_field(k, ww.currentText()))
                    w = cb
                elif t == "music" and key == "id" and hasattr(self, "_music_tags"):
                    cb = QComboBox(); cb.setEditable(True)
                    for tag in getattr(self, "_music_tags", []):
                        cb.addItem(tag)
                    if isinstance(val, str) and val:
                        cb.setCurrentText(val)
                    cb.currentTextChanged.connect(lambda _t, k=key, ww=cb: self._set_field(k, ww.currentText()))
                    w = cb
                else:
                    w = QLineEdit(str(val) if val is not None else "")
                    w.editingFinished.connect(lambda k=key, ww=w: self._set_field(k, ww.text()))
            elif fs.kind == "text":
                w = QTextEdit(str(val) if val is not None else "")
                w.textChanged.connect(lambda k=key, ww=w: self._set_field(k, ww.toPlainText()))
            elif fs.kind in ("float","opt_float"):
                w = QDoubleSpinBox(); w.setRange(-999999, 999999); w.setDecimals(3); w.setSingleStep(0.1)
                w.setValue(float(val or 0))
                w.valueChanged.connect(lambda _v, k=key, ww=w: self._set_field(k, float(ww.value()), optional=fs.kind.startswith("opt_")))
            elif fs.kind == "int":
                w = QSpinBox(); w.setRange(-999999, 999999)
                w.setValue(int(val or 0))
                w.valueChanged.connect(lambda _v, k=key, ww=w: self._set_field(k, int(ww.value())))
            elif fs.kind in ("enum","enum_opt"):
                w = QComboBox()
                options = list(fs.enum) if fs.enum else []
                if fs.kind == "enum_opt":
                    if "" not in options:
                        options = [""] + options
                for opt in options:
                    if fs.kind == "enum_opt" and (opt == "" or opt is None):
                        w.addItem("(auto)", "")
                    else:
                        w.addItem(str(opt), opt)
                if fs.kind == "enum_opt":
                    current = "" if val in (None, "") else val
                    idx = w.findData(current)
                    if idx < 0:
                        idx = 0
                    w.setCurrentIndex(idx)
                    w.currentIndexChanged.connect(lambda _t, k=key, ww=w: self._set_field(k, ww.currentData(), optional=True))
                else:
                    current = val if val is not None else fs.default
                    idx = w.findData(current)
                    if idx < 0:
                        idx = 0
                    w.setCurrentIndex(idx)
                    w.currentIndexChanged.connect(lambda _t, k=key, ww=w: self._set_field(k, ww.currentData()))
            elif fs.kind == "bool":
                w = QCheckBox()
                w.setChecked(bool(val))
                w.toggled.connect(lambda checked, k=key: self._set_field(k, bool(checked)))
            elif fs.kind == "vec2":
                w = QWidget(); hb = QHBoxLayout(w); hb.setContentsMargins(0,0,0,0)
                x = QDoubleSpinBox(); x.setRange(0,1); x.setSingleStep(0.05); x.setDecimals(3)
                y = QDoubleSpinBox(); y.setRange(0,1); y.setSingleStep(0.05); y.setDecimals(3)
                if isinstance(val, (list,tuple)) and len(val)==2:
                    x.setValue(float(val[0])); y.setValue(float(val[1]))
                hb.addWidget(QLabel("x:")); hb.addWidget(x); hb.addWidget(QLabel(" y:")); hb.addWidget(y)
                def _push_vec(_=None, k=key, xx=x, yy=y): self._set_field(k, [float(xx.value()), float(yy.value())], optional=True)
                x.valueChanged.connect(_push_vec); y.valueChanged.connect(_push_vec)
            elif fs.kind in ("color","opt_color"):
                w = QPushButton()
                is_opt = fs.kind.startswith("opt_")
                def _update_btn_color(btn, color_val):
                    if color_val is None:
                        btn.setText("None")
                        btn.setStyleSheet("")
                    else:
                        c = QColor.fromRgbF(color_val[0], color_val[1], color_val[2])
                        btn.setText(c.name())
                        btn.setStyleSheet(f"background-color: {c.name()}; color: {'white' if c.lightnessF() < 0.5 else 'black'};")
                def _pick_color(btn=w, k=key, optional=is_opt):
                    current_val = step.get(k)
                    current_qcolor = QColor.fromRgbF(*current_val) if isinstance(current_val, list) else QColor(0,0,0)
                    color = QColorDialog.getColor(current_qcolor, self, "Pick Color")
                    if color.isValid():
                        new_val = [color.redF(), color.greenF(), color.blueF()]
                        self._set_field(k, new_val, optional=optional)
                        _update_btn_color(btn, new_val)
                w.clicked.connect(_pick_color)
                _update_btn_color(w, val)
            elif fs.kind in ("color4","opt_color4"):
                w = QWidget(); hb = QHBoxLayout(w); hb.setContentsMargins(0,0,0,0)
                r = QDoubleSpinBox(); g = QDoubleSpinBox(); b = QDoubleSpinBox(); a = QDoubleSpinBox()
                for comp in (r,g,b,a):
                    comp.setRange(0,1); comp.setSingleStep(0.05); comp.setDecimals(3)
                if isinstance(val,(list,tuple)) and len(val)==4:
                    cr, cg, cb, ca = val
                else:
                    cr = cg = cb = 0.0; ca = 1.0
                r.setValue(float(cr)); g.setValue(float(cg)); b.setValue(float(cb)); a.setValue(float(ca))
                hb.addWidget(QLabel("r:")); hb.addWidget(r); hb.addWidget(QLabel(" g:")); hb.addWidget(g); hb.addWidget(QLabel(" b:")); hb.addWidget(b); hb.addWidget(QLabel(" a:")); hb.addWidget(a)
                def _push_col4(_=None, k=key, rr=r, gg=g, bb=b, aa=a):
                    self._set_field(k, [float(rr.value()), float(gg.value()), float(bb.value()), float(aa.value())], optional=fs.kind.startswith("opt_"))
                r.valueChanged.connect(_push_col4); g.valueChanged.connect(_push_col4); b.valueChanged.connect(_push_col4); a.valueChanged.connect(_push_col4)
            elif fs.kind == "json":
                w = QTextEdit()
                try:
                    w.setPlainText(json.dumps(val if isinstance(val, dict) else (val or {}), ensure_ascii=False, indent=2))
                except Exception:
                    w.setPlainText("{ }")
                w.textChanged.connect(lambda k=key, ww=w: self._set_json_field(k, ww.toPlainText()))
            else:
                w = QLabel(f"(Unsupported field kind: {fs.kind})")
            self._edit_widgets[key] = w
            self._step_form_layout.addRow(QLabel(fs.label), w)

        # extra hint for particles
        if t == "particles":
            hint = QLabel("<i>Note:</i> will be saved as <code>{\"type\":\"particles\",\"config\":{...}}</code>.")
            self._step_form_layout.addRow(hint)

    def _set_field(self, key: str, value: Any, *, optional: bool=False):
        step = self._get_selected_step()
        if not step: return

        old_value = step.get(key)
        if old_value == value:
            return

        # Adjust index for metadata
        actual_index = self.steps.currentRow() + 1
        cmd = ChangeValueCmd(self, actual_index, key, old_value, value, f"Change {key}")
        self.undo_stack.push(cmd)

        if optional:
            empty = False
            if value is None or value == "":
                empty = True
            elif isinstance(value, (list, tuple)):
                try:
                    empty = all(abs(float(v)) < 1e-6 for v in value)
                except Exception:
                    empty = False
            if empty:
                step.pop(key, None)
            else:
                step[key] = value
        else:
            step[key] = value



    # (Preview/play functionality removed at user request.)
    def _set_json_field(self, key: str, text: str):
        step = self._get_selected_step()
        if not step: return
        try:
            obj = json.loads(text or "{}")
            if not isinstance(obj, dict): raise ValueError("must be a JSON object")
        except Exception:
            # don't dirty; keep old until parseable
            return

        old_value = step.get(key)
        if old_value == obj:
            return
        # Adjust index for metadata
        actual_index = self.steps.currentRow() + 1
        cmd = ChangeValueCmd(self, actual_index, key, old_value, obj, f"Change {key}")
        self.undo_stack.push(cmd)

    def _refresh_selected_row_summary(self):
        r = self.steps.currentRow()
        s = self._get_selected_step()
        if r >= 0 and s:
            self.steps.item(r, 0).setText(s.get("type","?"))
            self.steps.item(r, 1).setText(summarize_step(s))

    def _update_raw(self, step: Optional[Dict[str,Any]]):
        if not step:
            self.raw_json.setPlainText("")
            return
        self.raw_json.blockSignals(True)
        self.raw_json.setPlainText(json.dumps(step, ensure_ascii=False, indent=2))
        self.raw_json.blockSignals(False)

    def _raw_changed(self):
        step = self._get_selected_step()
        if not step: return
        try:
            obj = json.loads(self.raw_json.toPlainText())
            if not isinstance(obj, dict) or "type" not in obj:
                return
        except Exception:
            return
        # replace step in list with parsed obj
        sid = self._current_id
        i = self.steps.currentRow()
        if sid is None or i < 0: return
        # Adjust index for metadata
        actual_index = i + 1
        self._scenes[sid][actual_index] = obj
        self._refresh_selected_row_summary()
        self._build_step_form(obj)
        self._set_dirty(True)

    def _flush_current_editor_into_scene(self):
        # raw JSON is the source of truth; try parsing and writing back
        step = self._get_selected_step()
        if not step: return
        self._raw_changed()

    # ---------------------- Misc helpers ----------------------
    def _choose_json(self):
        fn, _ = QFileDialog.getOpenFileName(self, "Open cinematics.json", str(self.root), "JSON (*.json)")
        if not fn: return
        self.path = Path(fn) # type: ignore
        self.path_lbl.setText(str(self.path))
        self._load()

    def _set_dirty(self, dirty: bool):
        self._dirty = dirty
        self.setWindowTitle(f"{self.TITLE}  {' •' if dirty else ''}")


    # (Embedded preview helpers removed at user request.)

# Small helper for rename prompt
class QInputDialogWithText:
    @staticmethod
    def get(parent, title: str, label: str, text: str) -> Tuple[str, bool]:
        # The previous implementation with a QMessageBox was a bit confusing.
        # A standard input dialog is more direct for a rename operation.
        return QInputDialog.getText(parent, title, label, text=text)
