#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations

import sys, importlib, os, traceback
from pathlib import Path
from typing import Dict, Tuple, Optional, List

from devkit_paths import resolve_paths

# --- Paths so imports work when running from /tools ---
_THIS_FILE = Path(__file__).resolve()
_TOOLS_DIR = _THIS_FILE.parent
_PROJECT_ROOT = _TOOLS_DIR.parent
if str(_TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(_TOOLS_DIR))
if str(_PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(_PROJECT_ROOT))
# ------------------------------------------------------

# Lazy theme import so we don't create any QWidget before QApplication exists.
ThemeManager = None
def _ensure_theme():
    global ThemeManager
    if ThemeManager is None:
        from theme_kit import ThemeManager as _TM
        ThemeManager = _TM
    return ThemeManager

# These are set in main() *after* QApplication exists.
detect_project_root = None
EditorBus = None
crossrefs = None

# Register editors here: (module_name, class_name, display_name, type_key)
PLUGINS = [
    ("theme_editor",      "ThemeEditor",     "Themes",            "theme"),
    ("world_editor",      "WorldBuilder",    "Worlds/Hubs/Nodes", "world"),
    ("item_editor",       "ItemEditor",      "Items",             "item"),
    ("shop_editor",       "ShopEditor",      "Shops",             "shop"),
    ("narrative_studio",  "NarrativeStudio", "Narrative Studio",  "narrative_studio"),
    ("quest_editor",      "QuestEditor",     "Quests",            "quest"),
    ("npc_editor",        "NPCEditor",       "NPCs",              "npc"),
    ("dialogue_editor",   "DialogueEditor",  "Dialogue",          "dialogue"),
    ("cutscene_editor",   "CutsceneEditor",  "Cutscenes",         "cutscene"),
    ("enemy_editor",      "EnemyEditor",     "Enemies",           "enemy"),
    ("encounter_editor",  "EncounterEditor", "Encounters",        "encounter"),
    ("event_editor",      "EventEditor",     "Events",            "event"),
    ("skills_editor",     "SkillsEditor",    "Skills",            "skill"),
    ("milestone_editor",  "MilestoneEditor", "Milestones",        "milestone"),
    ("sound_editor",      "SoundEditor",     "Audio/SFX",         "audio"),
    ("cooking_editor",    "CookingEditor",   "Cooking Editor",    "cooking"),
    ("tinkering_editor",  "TinkeringEditor", "Tinkering Editor",  "tinkering"),
    ("save_editor",       "SaveEditor",      "Saves (Dev)",       "save"),
    ("ai_art_tool",       "AIArtTool",       "AI Art",            "ai"),
    ("ai_assistant",      "AIAssistant",     "AI Assistant",      "ai"),
    ("balance_lab_ui",    "BalanceLabPanel", "Balance Lab",       "balance"),
]

def main():
    import sys, importlib, os, traceback
    from pathlib import Path
    from typing import Dict, Tuple, Optional, List

    # Import PyQt5/6 and your project modules *after* QApplication is created.
    from PyQt5.QtCore import Qt, QSize
    from PyQt5.QtGui import QIcon
    from PyQt5.QtWidgets import (
        QApplication, QMainWindow, QWidget, QTabWidget, QToolBar, QAction, QFileDialog,
        QDockWidget, QListWidget, QListWidgetItem, QInputDialog, QLineEdit, QVBoxLayout,
        QLabel, QPushButton, QGridLayout, QFrame, QStyle, QSizePolicy, QMessageBox
    )
    
    from ui_common import CommandPalette, attach_hotkeys

    # Lazy theme import now defined within main.
    ThemeManager = None
    def _ensure_theme():
        nonlocal ThemeManager
        if ThemeManager is None:
            from theme_kit import ThemeManager as _TM
            ThemeManager = _TM
        return ThemeManager

    # --- Start of execution: construct QApplication first. ---
    os.environ.setdefault("STUDIO_MANAGED", "1")
    app = QApplication(sys.argv)

    # --- Import project modules that might touch Qt widgets. ---
    global detect_project_root, EditorBus, crossrefs
    from data_core import detect_project_root as _detect_project_root
    detect_project_root = _detect_project_root
    from editor_bus import EditorBus as _EditorBus
    EditorBus = _EditorBus
    import crossrefs as _crossrefs
    crossrefs = _crossrefs

    # --- Home Dashboard Widget ---
    class StudioHome(QWidget):
        def __init__(self, studio):
            super().__init__()
            self.studio = studio
            
            layout = QVBoxLayout(self)
            layout.setAlignment(Qt.AlignCenter)
            
            # Logo / Title
            title = QLabel("Starborn Studio Pro")
            title.setStyleSheet("font-size: 32px; font-weight: bold; color: #5865F2; margin-bottom: 20px;")
            title.setAlignment(Qt.AlignCenter)
            layout.addWidget(title)
            
            # Quick Actions Grid
            grid_frame = QFrame()
            grid = QGridLayout(grid_frame)
            grid.setSpacing(15)
            
            actions = [
                ("New Item", "Create a new item", "item"),
                ("New Quest", "Create a new quest", "quest"),
                ("Edit Dialogue", "Open dialogue tree", "dialogue"),
                ("World Map", "Edit world nodes", "world"),
                ("Manage NPCs", "Edit NPC data", "npc"),
                ("Project Settings", "Edit theme/config", "theme"),
            ]
            
            for i, (name, desc, key) in enumerate(actions):
                btn = QPushButton(name)
                btn.setToolTip(desc)
                btn.setMinimumSize(180, 80)
                btn.setStyleSheet("""
                    QPushButton {
                        font-size: 14px; 
                        text-align: left; 
                        padding: 15px;
                        background-color: #2f3136;
                    }
                    QPushButton:hover {
                        background-color: #36393e;
                        border-left: 3px solid #5865F2;
                    }
                """)
                btn.clicked.connect(lambda checked, k=key: self.studio._on_goto(k, ""))
                
                # Add sub-label hack (Qt's standard buttons are limited, but this works for now)
                # Ideally we'd use a custom widget, but keeping it simple.
                
                row, col = i // 3, i % 3
                grid.addWidget(btn, row, col)
                
            layout.addWidget(grid_frame)

            # Recently Edited
            lbl = QLabel("Recently Edited")
            lbl.setStyleSheet("font-size: 14px; color: #b9bbbe; margin-top: 20px;")
            layout.addWidget(lbl)
            self.recent_list = QListWidget()
            self.recent_list.setMaximumHeight(220)
            self.recent_list.setStyleSheet("""
                QListWidget { background: #2f3136; border: 1px solid #3e4147; }
                QListWidget::item { padding: 6px; }
                QListWidget::item:hover { background: #36393e; }
                QListWidget::item:selected { background: #5865F2; }
            """)
            self.recent_list.itemActivated.connect(self._on_recent_clicked)
            layout.addWidget(self.recent_list)
            self._refresh_recent()

            layout.addStretch()

        def _refresh_recent(self):
            from studio_config import get_recent
            self.recent_list.clear()
            for entry in get_recent(15):
                item = QListWidgetItem(entry.get("label", "?"))
                item.setData(Qt.UserRole, entry)
                self.recent_list.addItem(item)

        def _on_recent_clicked(self, item):
            data = item.data(Qt.UserRole)
            if data:
                self.studio._on_goto(data["type"], data["id"])

    # --- Main Window ---
    class Studio(QMainWindow):
        def __init__(self, project_root: Path, assets_root: Path, theme="dark"):
            super().__init__()
            self.project_root = project_root
            self.assets_root = assets_root
            self.setWindowTitle("Starborn Studio Pro")
            self.resize(1400, 900)
            
            # Apply Theme immediately
            _ensure_theme().apply(self._app(), theme)
            
            # Main Layout
            self.tabs = QTabWidget()
            self.tabs.setDocumentMode(True) 
            self.tabs.setMovable(True)
            self.setCentralWidget(self.tabs)
            
            # Docks
            self._dock = QDockWidget("Results / Output", self)
            self._list = QListWidget()
            self._dock.setWidget(self._list)
            self.addDockWidget(Qt.BottomDockWidgetArea, self._dock)
            self._dock.hide()
            self._list.itemActivated.connect(self._open_result)
            
            # --- NEW: Project Explorer Dock ---
            self._explorer_dock = QDockWidget("Project Explorer", self)
            self._explorer_dock.setAllowedAreas(Qt.LeftDockWidgetArea | Qt.RightDockWidgetArea)
            
            from PyQt5.QtWidgets import QTreeView, QFileSystemModel
            self._fs_model = QFileSystemModel()
            self._fs_model.setRootPath(str(self.project_root))
            
            self._tree = QTreeView()
            self._tree.setModel(self._fs_model)
            self._tree.setRootIndex(self._fs_model.index(str(self.project_root)))
            self._tree.setAnimated(False)
            self._tree.setIndentation(12)
            self._tree.setSortingEnabled(True)
            self._tree.setColumnHidden(1, True) # Hide Size
            self._tree.setColumnHidden(2, True) # Hide Type
            self._tree.setColumnHidden(3, True) # Hide Date
            self._tree.setHeaderHidden(True)
            
            self._explorer_dock.setWidget(self._tree)
            self.addDockWidget(Qt.LeftDockWidgetArea, self._explorer_dock)
            # ----------------------------------
            
            self._pages: Dict[str, Tuple[QWidget, str]] = {}
            
            # Initialize Actions & Toolbar
            self._build_toolbar()
            
            # Load Home
            self.home = StudioHome(self)
            self.tabs.addTab(self.home, "Home")
            
            # Load Plugins
            self._load_plugins()
            
            # Subscriptions
            if EditorBus is not None:
                EditorBus.subscribe("goto", self._on_goto)
                EditorBus.subscribe("refresh_refs", self._refresh_refs)
                
            # Global Hotkeys
            attach_hotkeys(self, 
                           save_cb=self._save_all,
                           palette_cb=self._show_command_palette)

        def _app(self) -> QApplication:
            return QApplication.instance()

        def _build_toolbar(self):
            tb = QToolBar("Main")
            tb.setMovable(False)
            tb.setIconSize(QSize(20, 20))
            self.addToolBar(tb)
            
            style = self.style()
            
            # Actions with standard icons fallback
            a_open = QAction(style.standardIcon(QStyle.SP_DirIcon), "Open Project", self)
            a_open.triggered.connect(self._open_project)
            tb.addAction(a_open)
            
            a_save = QAction(style.standardIcon(QStyle.SP_DriveHDIcon), "Save All", self)
            a_save.setShortcut("Ctrl+S")
            a_save.triggered.connect(self._save_all)
            tb.addAction(a_save)
            
            tb.addSeparator()
            
            a_val = QAction(style.standardIcon(QStyle.SP_DialogApplyButton), "Validate", self)
            a_val.triggered.connect(self._validate)
            tb.addAction(a_val)
            
            a_find = QAction(style.standardIcon(QStyle.SP_FileDialogDetailedView), "Global Search", self)
            a_find.triggered.connect(self._global_search)
            tb.addAction(a_find)
            
            tb.addSeparator()
            
            a_palette = QAction(style.standardIcon(QStyle.SP_ComputerIcon), "Command Palette", self)
            a_palette.setToolTip("Ctrl+P")
            a_palette.triggered.connect(self._show_command_palette)
            tb.addAction(a_palette)

            tb.addSeparator()

            a_preview = QAction(style.standardIcon(QStyle.SP_MediaPlay), "Desktop Preview", self)
            a_preview.setToolTip("Launch Kivy game preview with current assets")
            a_preview.triggered.connect(self._launch_preview)
            tb.addAction(a_preview)

            # Spacer
            empty = QWidget()
            empty.setSizePolicy(QSizePolicy.Expanding, QSizePolicy.Preferred)
            tb.addWidget(empty)
            
            a_theme = QAction("Switch Theme", self)
            a_theme.triggered.connect(self._toggle_theme)
            tb.addAction(a_theme)

        def _toggle_theme(self):
            pal = self._app().palette()
            if pal.window().color().value() < 80:
                _ensure_theme().apply(self._app(), "light")
            else:
                _ensure_theme().apply(self._app(), "dark")

        def _show_command_palette(self):
            cp = CommandPalette(self)
            
            # Build actions list
            actions = [
                ("Project: Open...", self._open_project),
                ("File: Save All", self._save_all),
                ("Project: Validate All", self._validate),
                ("Project: Global Search", self._global_search),
                ("View: Toggle Theme", self._toggle_theme),
                ("View: Home", lambda: self.tabs.setCurrentWidget(self.home)),
            ]
            
            # Add navigation commands
            for key, (w, label) in self._pages.items():
                actions.append((f"Go to: {label}", lambda k=key: self._on_goto(k, "")))
                
            cp.set_actions(actions)
            cp.exec_()

        def _open_project(self):
            d = QFileDialog.getExistingDirectory(self, "Open Starborn Project", str(self.project_root))
            if not d:
                return
            paths = resolve_paths(Path(d))
            self.project_root = paths.project_root
            self.assets_root = paths.assets_dir
            self._reload_plugins()

        def _load_plugins(self):
            for mod_name, cls_name, display, key in PLUGINS:
                try:
                    mod = importlib.import_module(mod_name)
                    cls = getattr(mod, cls_name)
                    try:
                        w = cls(self.assets_root)
                    except TypeError:
                        try:
                            w = cls(self.project_root)
                        except TypeError:
                            w = cls()
                    self.tabs.addTab(w, display)
                    self._pages[key] = (w, display)
                except BaseException as e:
                    print(f"Failed to load {display}: {e}")
                    print(traceback.format_exc())

        def _reload_plugins(self):
            # Remove all tabs except Home
            while self.tabs.count() > 1:
                self.tabs.removeTab(1)
            self._pages.clear()
            self._load_plugins()

        def _save_all(self):
            errs: List[str] = []
            for i in range(self.tabs.count()):
                w = self.tabs.widget(i)
                if hasattr(w, "save"):
                    try:
                        ok = w.save()
                        if ok is False:
                            errs.append(self.tabs.tabText(i))
                    except Exception as e:
                        errs.append(f"{self.tabs.tabText(i)}: {e}")
            if errs:
                self._list.clear()
                for e in errs:
                    self._list.addItem(f"[Save] {e}")
                self._dock.setWindowTitle("Save Errors")
                self._dock.show()
            else:
                self.statusBar().showMessage("All editors saved.", 1500)

        def _validate(self):
            if crossrefs is None:
                self.statusBar().showMessage("Crossrefs not available.", 1500)
                return
            rows = crossrefs.validate_all(self.project_root)
            self._list.clear()
            if not rows:
                self._dock.hide()
                self.statusBar().showMessage("No issues found.", 1500)
                return
            for r in rows:
                it = QListWidgetItem(f"[{r.get('editor','?')}] {r.get('message','')}")
                it.setData(Qt.UserRole, r.get("payload"))
                self._list.addItem(it)
            self._dock.setWindowTitle("Validation")
            self._dock.show()

        def _launch_preview(self):
            from preview_launcher import PreviewLauncher
            if hasattr(self, "_previewer") and self._previewer.is_running():
                reply = QMessageBox.question(
                    self, "Preview Running",
                    "A preview is already running. Restart it?",
                    QMessageBox.Yes | QMessageBox.No,
                )
                if reply == QMessageBox.Yes:
                    self._previewer.stop()
                else:
                    return
            game_py = self.project_root / "Starborn_Python" / "game.py"
            if not game_py.exists():
                QMessageBox.warning(self, "Not Found",
                    f"Could not find game.py at:\n{game_py}")
                return
            self._previewer = PreviewLauncher(game_py, self.assets_root)
            err = self._previewer.launch()
            if err:
                QMessageBox.warning(self, "Preview Error", err)
            else:
                self.statusBar().showMessage("Desktop preview launched.", 2000)

        def _open_result(self, it: QListWidgetItem):
            payload = it.data(Qt.UserRole) or {}
            goto = payload.get("goto")
            if isinstance(goto, (list, tuple)) and len(goto) == 2:
                self._on_goto(goto[0], goto[1])

        def _global_search(self):
            text, ok = QInputDialog.getText(
                self, "Global Search",
                "Search all JSON files (prefix with type:stem to filter, e.g. type:items sword):",
                QLineEdit.Normal, "",
            )
            if not ok or not text.strip():
                return

            query = text.strip()
            file_filter = None
            if query.startswith("type:"):
                parts = query.split(None, 1)
                file_filter = parts[0][5:].lower()
                query = parts[1] if len(parts) > 1 else ""
            if not query:
                return

            hits = []
            seen: set = set()
            search_dirs = [self.assets_root, self.project_root]
            for search_dir in search_dirs:
                if not search_dir.is_dir():
                    continue
                for p in search_dir.rglob("*.json"):
                    rp = str(p)
                    if rp in seen:
                        continue
                    seen.add(rp)
                    if file_filter and file_filter not in p.stem.lower():
                        continue
                    try:
                        lines = p.read_text(encoding="utf-8").splitlines()
                    except Exception:
                        continue
                    try:
                        rel = p.relative_to(self.project_root)
                    except ValueError:
                        rel = p.name
                    for i, line in enumerate(lines, 1):
                        if query.lower() in line.lower():
                            hits.append((str(rel), i, line.strip()))

            self._list.clear()
            if not hits:
                self._dock.hide()
                self.statusBar().showMessage("No matches.", 1500)
                return
            for name, ln, line in hits[:500]:
                self._list.addItem(f"[Search] {name}:{ln} — {line[:200]}")
            self._dock.setWindowTitle(f"Search Results ({len(hits)} matches)")
            self._dock.show()

        def _refresh_refs(self):
            for i in range(self.tabs.count()):
                w = self.tabs.widget(i)
                for meth in ("refresh_refs", "refresh_references", "reload_lookups"):
                    if hasattr(w, meth):
                        try:
                            getattr(w, meth)()
                        except Exception:
                            pass
            self.statusBar().showMessage("Reference lists refreshed.", 1200)

        def _on_goto(self, target_type: str, ident: str):
            entry = self._pages.get(target_type)
            if not entry:
                self.statusBar().showMessage(f"No editor for '{target_type}'.", 2000)
                return
            w, label = entry
            idx = self.tabs.indexOf(w)
            if idx >= 0:
                self.tabs.setCurrentIndex(idx)
            # Track recently visited
            if ident:
                from studio_config import add_recent
                add_recent(target_type, ident, f"{label}: {ident}")
                if hasattr(self, "home"):
                    self.home._refresh_recent()
            for meth in ("select_id", "select_name", "focus_id", "focus_name", "select"):
                if hasattr(w, meth):
                    try:
                        getattr(w, meth)(ident)
                        return
                    except Exception:
                        pass
            self.statusBar().showMessage(f"Switched to {label} (no selection hook).", 2000)

    # --- Run the application. ---
    _ensure_theme().apply(app, "dark")
    root_arg = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    paths = resolve_paths(root_arg or Path(__file__).parent)
    project_root = paths.project_root
    assets_root = paths.assets_dir
    win = Studio(project_root, assets_root, theme="dark")
    win.statusBar().showMessage("Ready", 1000)
    win.showMaximized()
    sys.exit(app.exec_())


if __name__ == "__main__":
    # These global variables must be declared outside of the function.
    detect_project_root = None
    EditorBus = None
    crossrefs = None
    main()