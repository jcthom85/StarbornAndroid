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
# This data is safe to keep at the module level.
PLUGINS = [
    ("theme_editor",      "ThemeEditor",     "Themes",            "theme"),
    ("world_editor",      "WorldBuilder",    "Worlds/Hubs/Nodes", "world"),
    ("item_editor",       "ItemEditor",      "Items",             "item"),
    ("shop_editor",       "ShopEditor",      "Shops",             "shop"),
    ("narrative_builder", "NarrativeBuilder","Narrative Builder", "narrative"),
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
    ("fishing_editor",    "FishingEditor",   "Fishing Editor",    "fishing"),
    ("save_editor",       "SaveEditor",      "Saves (Dev)",       "save"),
    ("ai_art_tool",       "AIArtTool",       "AI Art",            "ai"),
    ("ai_assistant",      "AIAssistant",     "AI Assistant",      "ai"),
    ("balance_lab_ui",    "BalanceLabPanel", "Balance Lab",       "balance"),
]


def main():
    import sys, importlib, os, traceback
    from pathlib import Path
    from typing import Dict, Tuple, Optional, List

    # --- Paths so imports work when running from /tools --- 
    _THIS_FILE = Path(__file__).resolve()
    _TOOLS_DIR = _THIS_FILE.parent
    _PROJECT_ROOT = _TOOLS_DIR.parent
    if str(_TOOLS_DIR) not in sys.path:
        sys.path.insert(0, str(_TOOLS_DIR))
    if str(_PROJECT_ROOT) not in sys.path:
        sys.path.insert(0, str(_PROJECT_ROOT))
    # ------------------------------------------------------

    # Import PyQt5 and your project modules *after* QApplication is created.
    from PyQt5.QtCore import Qt
    from PyQt5.QtWidgets import (
        QApplication, QMainWindow, QWidget, QTabWidget, QToolBar, QAction, QFileDialog,
        QDockWidget, QListWidget, QListWidgetItem, QInputDialog, QLineEdit
    )

    # Lazy theme import now defined within main.
    ThemeManager = None
    def _ensure_theme():
        nonlocal ThemeManager
        if ThemeManager is None:
            from theme_kit import ThemeManager as _TM
            ThemeManager = _TM
        return ThemeManager

    # Register editors here: (module_name, class_name, display_name, type_key)
    PLUGINS = [
        ("world_editor", "WorldBuilder", "Worlds/Hubs/Nodes", "world"),
        ("theme_editor", "ThemeEditor", "Themes", "theme"),
        ("event_editor", "EventEditor", "Events", "event"),
        ("milestone_editor", "MilestoneEditor", "Milestones", "milestone"),
        ("narrative_builder", "NarrativeBuilder", "Narrative Builder", "narrative"),
        ("narrative_studio", "NarrativeStudio", "Narrative Studio", "narrative_studio"),
        ("quest_editor", "QuestEditor", "Quests", "quest"),
        ("item_editor", "ItemEditor", "Items", "item"),
        ("shop_editor", "ShopEditor", "Shops", "shop"),
        ("npc_editor", "NPCEditor", "NPCs", "npc"),
        ("dialogue_editor", "DialogueEditor", "Dialogue", "dialogue"),
        ("cutscene_editor", "CutsceneEditor", "Cutscenes", "cutscene"),
        ("enemy_editor", "EnemyEditor", "Enemies", "enemy"),
        ("encounter_editor", "EncounterEditor", "Encounters", "encounter"),
        ("sound_editor", "SoundEditor", "Audio/SFX", "audio"),
        ("tinkering_editor", "TinkeringEditor", "Tinkering Editor", "tinkering"),
        ("cooking_editor", "CookingEditor", "Cooking Editor", "cooking"),
        ("fishing_editor", "FishingEditor", "Fishing Editor", "fishing"),
        ("skills_editor", "SkillsEditor", "Skills", "skill"),
        ("ai_art_tool", "AIArtTool", "AI Art", "ai"),
        ("ai_assistant", "AIAssistant", "AI Assistant", "ai"),
        ("balance_lab_ui", "BalanceLabPanel", "Balance Lab", "balance"),
        ("save_editor", "SaveEditor", "Saves (Dev)", "save")
    ]

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

    # --- Define the main window class now that dependencies are imported. ---
    class Studio(QMainWindow):
        def __init__(self, project_root: Path, assets_root: Path, theme="light"):
            super().__init__()
            self.project_root = project_root
            self.assets_root = assets_root
            self.setWindowTitle("Starborn Studio Pro")
            self.resize(1280, 860)
            self.tabs = QTabWidget()
            self.setCentralWidget(self.tabs)
            self._dock = QDockWidget("Results", self)
            self._list = QListWidget()
            self._dock.setWidget(self._list)
            self.addDockWidget(Qt.BottomDockWidgetArea, self._dock)
            self._dock.hide()
            self._list.itemActivated.connect(self._open_result)
            self._pages: Dict[str, Tuple[QWidget, str]] = {}
            self._build_toolbar()
            _ensure_theme().apply(self._app(), theme)
            self._load_plugins()
            if EditorBus is not None:
                EditorBus.subscribe("goto", self._on_goto)
                EditorBus.subscribe("refresh_refs", self._refresh_refs)

        def _app(self) -> QApplication:
            return QApplication.instance()

        def _build_toolbar(self):
            tb = QToolBar("Main")
            self.addToolBar(tb)
            a_open = QAction("Open…", self); a_open.triggered.connect(self._open_project); tb.addAction(a_open)
            tb.addSeparator()
            a_save = QAction("Save All", self); a_save.triggered.connect(self._save_all); tb.addAction(a_save)
            a_val  = QAction("Validate All", self); a_val.triggered.connect(self._validate); tb.addAction(a_val)
            a_xref = QAction("Cross-Refs", self); a_xref.triggered.connect(self._validate); tb.addAction(a_xref)
            a_find = QAction("Global Search", self); a_find.triggered.connect(self._global_search); tb.addAction(a_find)
            a_ref  = QAction("Refresh Refs", self); a_ref.triggered.connect(self._refresh_refs); tb.addAction(a_ref)
            tb.addSeparator()
            a_theme = QAction("Theme: Light/Dark", self); a_theme.triggered.connect(self._toggle_theme); tb.addAction(a_theme)

        def _toggle_theme(self):
            pal = self._app().palette()
            if pal.window().color().value() < 80:
                _ensure_theme().apply(self._app(), "light")
            else:
                _ensure_theme().apply(self._app(), "dark")

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
            while self.tabs.count():
                self.tabs.removeTab(0)
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

        def _open_result(self, it: QListWidgetItem):
            payload = it.data(Qt.UserRole) or {}
            goto = payload.get("goto")
            if isinstance(goto, (list, tuple)) and len(goto) == 2:
                self._on_goto(goto[0], goto[1])

        def _global_search(self):
            text, ok = QInputDialog.getText(self, "Search", "Text to find in *.json:", QLineEdit.Normal, "")
            if not ok or not text.strip():
                return
            hits = []
            for p in self.project_root.glob("*.json"):
                try:
                    lines = p.read_text(encoding="utf-8").splitlines()
                except Exception:
                    continue
                for i, line in enumerate(lines, 1):
                    if text.lower() in line.lower():
                        hits.append((p.name, i, line.strip()))
            self._list.clear()
            if not hits:
                self._dock.hide()
                self.statusBar().showMessage("No matches.", 1500)
                return
            for name, ln, line in hits:
                self._list.addItem(f"[Search] {name}:{ln} — {line}")
            self._dock.setWindowTitle("Search Results")
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
