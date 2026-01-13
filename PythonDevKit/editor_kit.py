# tools/editor_kit.py
from __future__ import annotations
import os, json, shutil, time
from pathlib import Path
from typing import Any, Iterable, List, Dict, Optional

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import QApplication, QMessageBox, QLineEdit, QCompleter, QWidget, QShortcut
from PyQt5.QtGui import QKeySequence

# ---------- Root discovery ----------
def find_root(anchor_files: Iterable[str] = ("rooms.json","items.json","worlds.json")) -> Path:
    # 1) env override
    for k in ("STARBORN_ROOT","STARBORN_PROJECT_ROOT"):
        v = os.environ.get(k)
        if v and all(os.path.exists(os.path.join(v, f)) for f in anchor_files):
            return Path(v)
    # 2) walk up from CWD, then __file__
    for start in [Path.cwd(), Path(__file__).resolve().parent]:
        p = start
        for _ in range(5):
            if any((p/f).exists() for f in anchor_files):
                return p
            p = p.parent
    # 3) fallback
    return Path.cwd()

# ---------- JSON I/O ----------
def read_json(path: Path, default):
    try:
        if not path.exists(): return default
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as e:
        QMessageBox.critical(None, "Load Error", f"Failed to load {path.name}:\n{e}")
        return default

def write_json(path: Path, data) -> bool:
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        # .bak rotation (keep last 3)
        if path.exists():
            ts = time.strftime("%Y%m%d-%H%M%S")
            bak = path.with_suffix(path.suffix + f".{ts}.bak")
            bak.write_text(path.read_text(encoding="utf-8"), encoding="utf-8")
            # prune oldest >3
            baks = sorted(path.parent.glob(path.name + ".*.bak"))[:-3]
            for b in baks: 
                try: b.unlink()
                except: pass
        path.write_text(json.dumps(data, ensure_ascii=False, indent=4), encoding="utf-8")
        return True
    except Exception as e:
        QMessageBox.critical(None, "Save Error", f"Failed to save {path.name}:\n{e}")
        return False

# ---------- ID helpers ----------
def collect_ids(objs: List[dict], *keys) -> List[str]:
    ids = []
    for o in objs:
        for k in (keys or ("id","name")):
            v = (o.get(k) or "").strip()
            if v:
                ids.append(v)
                break
    return sorted(set(ids), key=str.lower)

def make_completer(values: Iterable[str]) -> QCompleter:
    comp = QCompleter(sorted({v for v in values if v}, key=str.lower))
    comp.setFilterMode(Qt.MatchContains)
    comp.setCaseSensitivity(Qt.CaseInsensitive)
    return comp

# ---------- Shortcuts / dirty-state ----------
def install_std_shortcuts(widget: QWidget, on_save=None, on_find=None):
    if on_save:
        QShortcut(QKeySequence("Ctrl+S"), widget, activated=on_save)
    if on_find:
        QShortcut(QKeySequence("Ctrl+F"), widget, activated=on_find)

def set_dirty(widget: QWidget, dirty: bool, base_title: str):
    widget.setWindowTitle(f"{base_title}{' •' if dirty else ''}")
