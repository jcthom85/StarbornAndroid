# save_system.py
from __future__ import annotations

import json, os, sys, time, shutil, hashlib
from pathlib import Path
from typing import Any, Dict, Optional

# --- Import bootstrapping: make sure we can import data_core even when launched from tools/ or elsewhere ---
from tools.data_core import detect_project_root

def _rotate_backups(path: Path, keep: int = 3) -> None:
    """
    Keep up to `keep` timestamped backups next to `path`.
    Produces files like: save1.json.2025-08-20-141259.bak
    """
    if not path.exists():
        return
    ts = time.strftime("%Y-%m-%d-%H%M%S")
    bak = path.with_suffix(path.suffix + f".{ts}.bak")
    try:
        shutil.copy2(path, bak)
    except Exception:
        pass
    # prune older backups
    try:
        baks = sorted(path.parent.glob(path.name + ".*.bak"))
        for old in baks[:-keep]:
            try: old.unlink()
            except Exception: pass
    except Exception:
        pass

def _json_write(path: Path, data: Any) -> bool:
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        _rotate_backups(path)
        with path.open("w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        return True
    except Exception:
        return False

def _json_read(path: Path) -> Optional[dict]:
    try:
        with path.open("r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return None

def _fingerprint(payload: Dict[str, Any]) -> str:
    """
    Compute a compact hash of the *important* parts of the save for autosave deduping.
    We include:
      - world/hub/node/room ids
      - inventory contents
      - character levels / hp
      - quest statuses
    """
    gs = payload.get("game_state", {})
    mp = gs.get("map", {})
    inv = gs.get("inventory", {})
    chars = payload.get("characters", {})
    quests = gs.get("quests", {})
    routes = gs.get("routes", {})

    parts: list[str] = []
    parts.append(str(mp.get("current_world_id")))
    parts.append(str(mp.get("current_hub_id")))
    parts.append(str(mp.get("current_node_id")))
    parts.append(str(mp.get("current_room_id")))

    # inventory stable order
    for k in sorted(inv):
        parts.append(f"I:{k}:{inv[k]}")

    # character basic
    for cid in sorted(chars):
        c = chars[cid]
        parts.append(f"C:{cid}:{c.get('level',1)}:{c.get('hp',0)}")
        
        # quest status only (stable id order) - handles new QuestManager format
        quest_list = quests.get("quests", []) if isinstance(quests, dict) else []
        if isinstance(quest_list, list):
            # Sort by 'id' to ensure stable order for fingerprinting
            for q in sorted(quest_list, key=lambda x: x.get('id', '')):
                qid = q.get('id')
                if qid:
                    parts.append(f"Q:{qid}:{q.get('status','inactive')}:{q.get('stage_index',0)}")

    # include route-discovery flags (stable order)
    for scope in ("worlds", "hubs", "nodes"):
        m = routes.get(scope, {}) if isinstance(routes, dict) else {}
        for rid in sorted(m):
            parts.append(f"R:{scope}:{rid}:{1 if m.get(rid) else 0}")

    digest = hashlib.sha1("|".join(parts).encode("utf-8")).hexdigest()
    return digest

class SaveSystem:
    """
    Slot saves compatible with your existing files (save1.json, save2.json, save3.json),
    plus autosave (autosave.json) and quicksave (quicksave.json). Safe backups are kept
    alongside each write.

    Use:
        ss = SaveSystem()
        ss.save(1, payload)            # writes save1.json (+ .bak rotation)
        data = ss.load(1)              # dict or None
        ss.autosave(payload, context="room_change")
        ss.quicksave(payload)
    """
    def __init__(
        self,
        project_root: Optional[Path] = None,
        saves_dir: Optional[Path] = None,
        *,
        autosave_name: str = "autosave.json",
        quicksave_name: str = "quicksave.json",
        min_autosave_interval: float = 10.0
    ) -> None:
        self.root = detect_project_root(project_root)
        # Keep saves at repo root to match existing UI (LoadGameScreen looks for saveN.json there)
        self.saves_dir = Path(saves_dir) if saves_dir else self.root
        self.autosave_path = self.saves_dir / autosave_name
        self.quicksave_path = self.saves_dir / quicksave_name
        self.min_autosave_interval = float(min_autosave_interval)
        self._last_autosave_ts: float = 0.0
        self._last_autosave_fp: Optional[str] = None

    # ---------- basic slots ----------
    def save_path(self, slot: int) -> Path:
        return self.saves_dir / f"save{int(slot)}.json"

    def save(self, slot: int, payload: Dict[str, Any]) -> Path:
        path = self.save_path(slot)
        _json_write(path, payload)
        return path

    def load(self, slot: int) -> Optional[Dict[str, Any]]:
        path = self.save_path(slot)
        return _json_read(path)

    def delete(self, slot: int) -> None:
        try:
            self.save_path(slot).unlink()
        except Exception:
            pass

    # ---------- quick / auto ----------
    def quicksave(self, payload: Dict[str, Any]) -> Path:
        _json_write(self.quicksave_path, payload)
        return self.quicksave_path

    def autosave(self, payload: Dict[str, Any], *, context: str | None = None, throttle: bool = True) -> Optional[Path]:
        """
        Write autosave.json if either:
         - enough time has passed since last autosave, OR
         - the fingerprint changed (different room, inventory, quests, etc.)
        """
        now = time.time()
        fp = _fingerprint(payload)

        if throttle:
            # Time gate
            if (now - self._last_autosave_ts) < self.min_autosave_interval and fp == self._last_autosave_fp:
                return None

        ok = _json_write(self.autosave_path, payload)
        if ok:
            self._last_autosave_ts = now
            self._last_autosave_fp = fp
            return self.autosave_path
        return None

    # Nice alias you can call freely
    def maybe_autosave(self, payload: Dict[str, Any], *, context: str | None = None) -> Optional[Path]:
        return self.autosave(payload, context=context, throttle=True)
