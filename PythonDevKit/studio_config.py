#!/usr/bin/env python3
"""
Persistent config for Starborn Studio Pro.
Reads/writes to the same ~/.starborn_devkit.json used by devkit_paths.py,
merging keys so project_root and other settings coexist.
"""
from __future__ import annotations
import json, time
from pathlib import Path
from typing import Any, Dict, List, Optional

CONFIG_PATH = Path.home() / ".starborn_devkit.json"


def load_config() -> Dict[str, Any]:
    if CONFIG_PATH.exists():
        try:
            return json.loads(CONFIG_PATH.read_text(encoding="utf-8"))
        except Exception:
            return {}
    return {}


def save_config(updates: Dict[str, Any]):
    cfg = load_config()
    cfg.update(updates)
    CONFIG_PATH.parent.mkdir(parents=True, exist_ok=True)
    CONFIG_PATH.write_text(json.dumps(cfg, indent=2, ensure_ascii=False), encoding="utf-8")


# ---------- Recent Items ----------

def get_recent(max_items: int = 15) -> List[Dict[str, str]]:
    """Return list of {type, id, label, ts} dicts, newest first."""
    cfg = load_config()
    return (cfg.get("recent") or [])[:max_items]


def add_recent(target_type: str, ident: str, label: str):
    """Push a recently-visited entry to the front of the list."""
    if not ident:
        return
    cfg = load_config()
    recent: list = cfg.get("recent") or []
    # deduplicate
    recent = [r for r in recent if not (r.get("type") == target_type and r.get("id") == ident)]
    recent.insert(0, {
        "type": target_type,
        "id": ident,
        "label": label,
        "ts": time.time(),
    })
    cfg["recent"] = recent[:30]
    save_config(cfg)
