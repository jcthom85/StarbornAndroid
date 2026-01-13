#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations
import json, shutil, re
from pathlib import Path
from typing import Any, List, Optional

from devkit_paths import resolve_paths

def detect_project_root(hint: Optional[Path]=None) -> Path:
    """
    Resolve the Android project root (contains app/src/main/assets).
    """
    return resolve_paths(hint).project_root

def detect_assets_dir(hint: Optional[Path]=None) -> Path:
    """
    Resolve the assets directory where JSON files live (app/src/main/assets).
    """
    return resolve_paths(hint).assets_dir

def detect_skill_trees_dir(hint: Optional[Path]=None) -> Path:
    """
    Resolve the skill tree directory (app/src/main/assets/skill_trees).
    """
    return resolve_paths(hint).skill_trees_dir

def asset_path(root: Optional[Path]=None, *parts: str) -> Path:
    """
    Build a path under the assets directory (app/src/main/assets).
    """
    base = resolve_paths(root).assets_dir
    return base.joinpath(*parts)

def json_load(path: Path, default: Any=None) -> Any:
    path = Path(path)
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)

def _backup(path: Path):
    try:
        if path.exists():
            shutil.copy2(path, path.with_suffix(path.suffix + ".bak"))
    except Exception:
        pass

def json_save(path: Path, data: Any, *, sort_obj=False, indent=2) -> None:
    """
    Pretty JSON with .bak. If list of dicts, sorts by 'name' or 'id' when sort_obj=True.
    """
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    serial = data
    if sort_obj and isinstance(data, list) and all(isinstance(x, dict) for x in data):
        key = "name" if any("name" in x for x in data) else ("id" if any("id" in x for x in data) else None)
        if key:
            serial = sorted(data, key=lambda x: str(x.get(key, "")).lower())
    _backup(path)
    with path.open("w", encoding="utf-8") as f:
        json.dump(serial, f, ensure_ascii=False, indent=indent)

def unique_id(base: str, existing: List[str]) -> str:
    """
    Makes base, base_copy, base_copy2, ... that doesn't collide.
    """
    base = re.sub(r"\s+", "_", base.strip())
    if base not in existing: return base
    if f"{base}_copy" not in existing: return f"{base}_copy"
    n = 2
    while True:
        cand = f"{base}_copy{n}"
        if cand not in existing: return cand
        n += 1
