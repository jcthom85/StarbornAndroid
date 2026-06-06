#!/usr/bin/env python3
"""
Cross-file ID rename propagation for Starborn assets.
Given an entity type and old/new ID, finds and updates all references
across JSON data files.
"""
from __future__ import annotations
import json
from pathlib import Path
from typing import Any, Dict, List, Tuple

from data_core import json_load, json_save


# Each rule: (file, list of (leaf_key, description) pairs)
# The walker visits every dict recursively; when it finds a key matching
# leaf_key with a string value equal to old_id, it replaces with new_id.
# A leaf_key of "*list" means the file is a flat list and bare string
# elements should be matched.

RENAME_RULES: Dict[str, List[Tuple[str, List[Tuple[str, str]]]]] = {
    "item": [
        ("enemies.json", [
            ("item", "enemy drop item"),
            ("id", "enemy drop id"),
        ]),
        ("quests.json", [
            ("item_id", "quest reward/requirement item_id"),
            ("requires_item", "quest requires_item"),
            ("required_item", "quest required_item"),
            ("reward_item", "quest reward_item"),
        ]),
        ("events.json", [
            ("item", "event action item"),
            ("item_id", "event item_id"),
            ("give_item", "event give_item"),
        ]),
        ("shops.json", [
            ("item_id", "shop item_id"),
            ("item", "shop item"),
            ("id", "shop item id"),
        ]),
        ("recipes_tinkering.json", [
            ("item", "tinkering recipe item"),
            ("result", "tinkering recipe result"),
        ]),
        ("recipes_cooking.json", [
            ("item", "cooking recipe item"),
            ("result", "cooking recipe result"),
        ]),
    ],
    "quest": [
        ("quests.json", [
            ("prereq_quest_id", "quest prerequisite"),
        ]),
        ("events.json", [
            ("quest_id", "event quest_id"),
            ("quest", "event quest"),
            ("start_quest", "event start_quest"),
            ("complete_quest", "event complete_quest"),
        ]),
        ("dialogue.json", [
            # conditions like "quest:old_id" handled separately
        ]),
    ],
    "enemy": [
        ("encounters.json", [
            ("id", "encounter enemy id"),
        ]),
    ],
    "npc": [
        ("dialogue.json", [
            ("speaker", "dialogue speaker"),
        ]),
        ("rooms.json", [
            # NPCs in room arrays are bare strings — handled by *list
        ]),
        ("quests.json", [
            ("giver", "quest giver"),
            ("receiver", "quest receiver"),
        ]),
        ("events.json", [
            ("npc", "event npc"),
            ("npc_id", "event npc_id"),
        ]),
    ],
    "milestone": [
        ("events.json", [
            ("milestone", "event milestone"),
        ]),
    ],
    "room": [
        ("events.json", [
            ("room", "event room"),
            ("room_id", "event room_id"),
            ("target_room", "event target_room"),
        ]),
    ],
}


def _walk_and_replace(data: Any, keys: set, old_val: str, new_val: str) -> int:
    """Recursively walk data, replacing string values at matching keys."""
    count = 0
    if isinstance(data, dict):
        for k in list(data.keys()):
            v = data[k]
            if k in keys and isinstance(v, str) and v == old_val:
                data[k] = new_val
                count += 1
            elif isinstance(v, (dict, list)):
                count += _walk_and_replace(v, keys, old_val, new_val)
    elif isinstance(data, list):
        for i, item in enumerate(data):
            if isinstance(item, str) and item == old_val:
                # Bare string in list (e.g., NPC name in room npcs array)
                data[i] = new_val
                count += 1
            elif isinstance(item, (dict, list)):
                count += _walk_and_replace(item, keys, old_val, new_val)
    return count


def _count_matches(data: Any, keys: set, old_val: str) -> int:
    """Count without modifying."""
    count = 0
    if isinstance(data, dict):
        for k, v in data.items():
            if k in keys and isinstance(v, str) and v == old_val:
                count += 1
            elif isinstance(v, (dict, list)):
                count += _count_matches(v, keys, old_val)
    elif isinstance(data, list):
        for item in data:
            if isinstance(item, str) and item == old_val:
                count += 1
            elif isinstance(item, (dict, list)):
                count += _count_matches(item, keys, old_val)
    return count


def preview_rename(
    entity_type: str, old_id: str, new_id: str, assets_dir: Path
) -> List[Dict[str, Any]]:
    """
    Dry-run: returns list of {"file": str, "description": str, "count": int}
    for each file that would be affected.
    """
    rules = RENAME_RULES.get(entity_type, [])
    results = []
    for filename, key_descs in rules:
        path = assets_dir / filename
        data = json_load(path, None)
        if data is None:
            continue
        keys = {kd[0] for kd in key_descs}
        n = _count_matches(data, keys, old_id)
        if n > 0:
            desc = ", ".join(kd[1] for kd in key_descs if kd[0] in keys)
            results.append({"file": filename, "description": desc, "count": n})
    return results


def apply_rename(
    entity_type: str, old_id: str, new_id: str, assets_dir: Path
) -> List[str]:
    """
    Apply the rename across all relevant files.
    Returns list of human-readable change descriptions.
    """
    rules = RENAME_RULES.get(entity_type, [])
    messages = []
    for filename, key_descs in rules:
        path = assets_dir / filename
        data = json_load(path, None)
        if data is None:
            continue
        keys = {kd[0] for kd in key_descs}
        n = _walk_and_replace(data, keys, old_id, new_id)
        if n > 0:
            json_save(path, data)
            messages.append(f"{filename}: {n} reference(s) updated")
    return messages
