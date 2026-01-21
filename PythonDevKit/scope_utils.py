#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple

from devkit_paths import resolve_paths

SCOPE_SEPARATOR = "__"


def _read_list(path: Path) -> List[dict]:
    if not path.exists():
        return []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return []
    if isinstance(data, list):
        return [row for row in data if isinstance(row, dict)]
    if isinstance(data, dict):
        return [row for row in data.values() if isinstance(row, dict)]
    return []


@dataclass
class ScopeIndex:
    worlds: Dict[str, dict]
    hubs: Dict[str, dict]
    hub_to_world: Dict[str, str]
    room_to_hub: Dict[str, str]
    hubs_by_world: Dict[str, List[str]]

    @property
    def world_ids(self) -> List[str]:
        return sorted(self.worlds.keys())

    @property
    def hub_ids(self) -> List[str]:
        return sorted(self.hubs.keys())

    @classmethod
    def from_assets(cls, root: Path | str) -> "ScopeIndex":
        paths = resolve_paths(Path(root))
        assets = paths.assets_dir

        worlds_list = _read_list(assets / "worlds.json")
        hubs_list = _read_list(assets / "hubs.json")
        nodes_list = _read_list(assets / "hub_nodes.json")

        worlds = {w.get("id"): w for w in worlds_list if w.get("id")}
        hubs = {h.get("id"): h for h in hubs_list if h.get("id")}

        hub_to_world: Dict[str, str] = {}
        hubs_by_world: Dict[str, List[str]] = {}
        for hid, hub in hubs.items():
            wid = str(hub.get("world_id") or "").strip()
            if not wid:
                continue
            hub_to_world[hid] = wid
            hubs_by_world.setdefault(wid, []).append(hid)
        for wid in hubs_by_world:
            hubs_by_world[wid] = sorted(set(hubs_by_world[wid]))

        room_to_hub: Dict[str, str] = {}
        for node in nodes_list:
            hid = str(node.get("hub_id") or "").strip()
            rooms = node.get("rooms") or []
            if not hid or not isinstance(rooms, list):
                continue
            for rid in rooms:
                if isinstance(rid, str) and rid:
                    room_to_hub[rid] = hid

        return cls(
            worlds=worlds,
            hubs=hubs,
            hub_to_world=hub_to_world,
            room_to_hub=room_to_hub,
            hubs_by_world=hubs_by_world,
        )


def scope_prefix(world_id: Optional[str], hub_id: Optional[str]) -> str:
    if world_id and hub_id:
        return f"{world_id}{SCOPE_SEPARATOR}{hub_id}{SCOPE_SEPARATOR}"
    if world_id:
        return f"{world_id}{SCOPE_SEPARATOR}"
    return ""


def ensure_prefix(value: str, prefix: str) -> str:
    if prefix and value and not value.startswith(prefix):
        return f"{prefix}{value}"
    return value


def infer_prefix(value: str, index: ScopeIndex) -> str:
    if not value:
        return ""
    for world_id in index.world_ids:
        world_prefix = f"{world_id}{SCOPE_SEPARATOR}"
        if not value.startswith(world_prefix):
            continue
        for hub_id in index.hubs_by_world.get(world_id, []):
            hub_prefix = f"{world_id}{SCOPE_SEPARATOR}{hub_id}{SCOPE_SEPARATOR}"
            if value.startswith(hub_prefix):
                return hub_prefix
        return world_prefix
    return ""


def scoped_id(prefix: str, kind: str, base: str, existing: Iterable[str]) -> str:
    existing_set = set(existing)
    stem = f"{kind}_{base}" if kind else base
    candidate = f"{prefix}{stem}"
    if candidate not in existing_set:
        return candidate
    n = 2
    while True:
        candidate = f"{prefix}{stem}_{n}"
        if candidate not in existing_set:
            return candidate
        n += 1
