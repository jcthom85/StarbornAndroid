#!/usr/bin/env python3
# tools/context_index.py
from __future__ import annotations
import json
from pathlib import Path
from typing import Any, Dict, Optional
import os

def _find_root(start: Path) -> Path:
    cur = start.resolve()
    for _ in range(6):
        if (cur / "rooms.json").exists() or (cur / "worlds.json").exists():
            return cur
        cur = cur.parent
    return start.resolve()

    
def _read_json(path: Path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return None

def _read_list(path: Path) -> list:
    v = _read_json(path); return v if isinstance(v, list) else []

class ContextIndex:
    def __init__(self, project_root: Optional[str|Path] = None):
        self.root = _find_root(Path(project_root) if project_root else Path(__file__).parent)
    # Intentionally do not load or reference the large GDD docx to keep context lean.

    # --- Simple accessors expected by tools like ai_assistant.py ---
    def worlds(self) -> list:
        return _read_list(self.root / "worlds.json")

    def hubs(self) -> list:
        return _read_list(self.root / "hubs.json")

    def nodes(self) -> list:
        return _read_list(self.root / "nodes.json")

    def rooms(self) -> list:
        return _read_list(self.root / "rooms.json")

    def pack_global(self):
        # Root-relative safe readers
        def _abs(p: os.PathLike | str | Path) -> Path:
            p = Path(p)
            return p if p.is_absolute() else (self.root / p)

        def _read_any(relpath: os.PathLike | str, fallback):
            try:
                p = _abs(relpath)
                with open(p, encoding="utf-8") as f:
                    return json.load(f)
            except Exception:
                return fallback

        def _read_dict(relpath: os.PathLike | str) -> Dict[str, Any]:
            v = _read_any(relpath, {})
            return v if isinstance(v, dict) else {}

        def _read_list_safe(relpath: os.PathLike | str) -> list:
            v = _read_any(relpath, [])
            return v if isinstance(v, list) else []

        def _read_text(relpath: os.PathLike | str) -> str:
            try:
                return _abs(relpath).read_text(encoding="utf-8")
            except Exception:
                return ""

        def _read_skill_trees() -> Dict[str, Any]:
            trees: Dict[str, Any] = {}
            st_dir = _abs("skill_trees")
            if st_dir.exists() and st_dir.is_dir():
                for fp in sorted(st_dir.glob("*.json")):
                    try:
                        trees[fp.stem] = json.loads(fp.read_text(encoding="utf-8"))
                    except Exception:
                        trees[fp.stem] = {}
            return trees

        return {
            # World topology
            "worlds": _read_list_safe("worlds.json"),
            "hubs": _read_list_safe("hubs.json"),
            "nodes": _read_list_safe("nodes.json"),
            "rooms": _read_list_safe("rooms.json"),
            "room_templates": _read_any("room_templates.json", []),

            # Core content
            "items": _read_list_safe("items.json"),
            "skills": _read_list_safe("skills.json"),
            "npcs": _read_list_safe("npcs.json"),
            "quests": _read_list_safe("quests.json"),
            "enemies": _read_list_safe("enemies.json"),
            "events": _read_list_safe("events.json"),
            "cinematics": _read_any("cinematics.json", {}),  # may be dict or list in some projects
            "dialogue": _read_list_safe("dialogue.json"),
            "characters": _read_list_safe("characters.json"),
            "milestones": _read_list_safe("milestones.json"),
            "shops": _read_list_safe("shops.json"),
            "encounters": _read_list_safe("encounters.json"),

            # Systems / config
            "buffs": _read_dict("buffs.json"),
            "themes": _read_dict("themes.json"),
            "sfx": _read_dict("sfx.json"),
            "audio_bindings": _read_dict("audio_bindings.json"),

            # Saves / settings
            "settings": _read_any("settings.json", {}),
            "save1": _read_any("save1.json", {}),
            "save2": _read_any("save2.json", {}),

            # Tools data
            "templates": _read_dict(os.path.join("tools", "templates.json")),
            "balance_targets": _read_dict(os.path.join("tools", "balance_targets.json")),

            # Data folder extras (optional; safe fallbacks)
            "progression": _read_dict(os.path.join("data", "progression.json")),
            "leveling_data": _read_dict(os.path.join("data", "leveling_data.json")),
            "components": _read_dict(os.path.join("data", "components.json")),
            "recipes_cooking": _read_dict(os.path.join("data", "recipes_cooking.json")),
            "recipes_tinkering": _read_dict(os.path.join("data", "recipes_tinkering.json")),
            "recipes_fishing": _read_dict(os.path.join("data", "recipes_fishing.json")),

            # Skill trees (folder aggregate)
            "skill_trees": _read_skill_trees(),

            # Design/briefing (text only; no docx loaded)
            "assistant_briefing": _read_text(os.path.join("data", "assistant_briefing.md")),
        }
    
    def pack_for_node(self, node_id: str) -> Dict[str, Any]:
        nodes = {str(n.get("id")): n for n in self.nodes()}
        node = nodes.get(str(node_id))
        if not node:
            return self.pack_global()

        room_map = {str(r.get("id")): r for r in _read_list(self.root / "rooms.json")}
        rooms_in_node = [room_map[rid] for rid in (node.get("rooms") or []) if rid in room_map]

        # Provide both keys for compatibility: 'rooms_in_node' and 'rooms'
        base = self.pack_global()
        base.update({
            "node": node,
            "rooms_in_node": rooms_in_node,
            "rooms": rooms_in_node,   # alias expected by some generators
        })
        return base
    