#!/usr/bin/env python3
"""Validate dialogue/cinematic/shop portrait and emote asset references.

This is intentionally content-driven: NPC emotes are only required when a line
actually uses that emote. Characters use the conventional
images/characters/emotes/<character_id>_<emote>.png path.
"""
from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

DEFAULT_PORTRAIT = "images/characters/communicator_portrait.png"
STANDARD_CHARACTER_EMOTES = {
    "angry",
    "confident",
    "cool",
    "crying",
    "gasping",
    "happy",
    "idle",
    "laughing",
    "puzzled",
    "sad",
    "scared",
    "surprised",
    "worried",
}


@dataclass(frozen=True)
class SpeakerRef:
    kind: str
    id: str
    name: str
    portrait: str | None
    emotes: dict[str, str]


@dataclass(frozen=True)
class EmoteUse:
    source: str
    line_id: str | None
    speaker: str
    emote: str


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def norm_key(value: str | None) -> str:
    return (value or "").strip().lower()


def asset_key(value: str | None) -> str:
    return re.sub(r"[^a-z0-9_]", "", norm_key(value).replace("-", "_"))


def normalize_emote(value: str | None) -> str:
    return norm_key(value).replace("-", "_")


def normalize_path(path: str | None, default_dir: str = "images/characters") -> str | None:
    if not path or not path.strip():
        return None
    raw = path.strip().replace("\\", "/")
    if "/" in raw:
        return raw
    name = raw if raw.lower().endswith(".png") else f"{asset_key(raw)}.png"
    return f"{default_dir}/{name}"


def existing_asset(root: Path, asset_path: str | None) -> bool:
    return bool(asset_path) and (root / "app" / "src" / "main" / "assets" / asset_path).is_file()


def speaker_keys(name: str | None, speaker_id: str | None = None, aliases: Iterable[str] = ()) -> set[str]:
    keys = {norm_key(name), norm_key(speaker_id)}
    keys.update(norm_key(alias) for alias in aliases)
    if name:
        keys.add(norm_key(name.split()[-1]))
        keys.add(norm_key(name.split()[0]))
    return {key for key in keys if key}


def build_speaker_index(root: Path) -> dict[str, SpeakerRef]:
    characters = load_json(root / "app" / "src" / "main" / "assets" / "characters.json")
    npcs = load_json(root / "app" / "src" / "main" / "assets" / "npcs.json")
    index: dict[str, SpeakerRef] = {}

    for character in characters:
        cid = character.get("id") or asset_key(character.get("name"))
        ref = SpeakerRef(
            kind="character",
            id=cid,
            name=character.get("name") or cid,
            portrait=character.get("mini_icon_path"),
            emotes={emote: f"images/characters/emotes/{asset_key(cid)}_{emote}.png" for emote in STANDARD_CHARACTER_EMOTES},
        )
        for key in speaker_keys(ref.name, ref.id):
            index[key] = ref

    player = characters[0] if characters else None
    if player:
        ref = index.get(norm_key(player.get("id")))
        if ref:
            index["player"] = ref

    for npc in npcs:
        nid = npc.get("id") or asset_key(npc.get("name"))
        emotes = {
            normalize_emote(key): normalize_path(value, default_dir="images/npcs/emotes") or ""
            for key, value in (npc.get("emotes") or {}).items()
            if normalize_emote(key)
        }
        ref = SpeakerRef(
            kind="npc",
            id=nid,
            name=npc.get("name") or nid,
            portrait=npc.get("portrait"),
            emotes=emotes,
        )
        for key in speaker_keys(ref.name, ref.id, npc.get("aliases") or []):
            index[key] = ref

    return index


def collect_emote_uses(data: Any, source: str) -> list[EmoteUse]:
    uses: list[EmoteUse] = []

    def walk(node: Any, inherited_speaker: str | None = None) -> None:
        if isinstance(node, dict):
            speaker = node.get("speaker") or inherited_speaker
            emote = node.get("emote")
            if speaker and emote:
                uses.append(EmoteUse(source, node.get("id"), str(speaker), str(emote)))
            for value in node.values():
                walk(value, speaker)
        elif isinstance(node, list):
            for item in node:
                walk(item, inherited_speaker)

    walk(data)
    return uses


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate Starborn dialogue portrait/emote references.")
    parser.add_argument("--root", default=".", help="Repo root")
    parser.add_argument("--fail-on-missing", action="store_true")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    speaker_index = build_speaker_index(root)
    missing: list[str] = []

    for key, ref in sorted({ref.id: ref for ref in speaker_index.values()}.items()):
        portrait = normalize_path(ref.portrait, default_dir="images/npcs" if ref.kind == "npc" else "images/characters")
        if portrait and not existing_asset(root, portrait):
            missing.append(f"base portrait missing: {ref.kind} {ref.name} -> {portrait}")

    sources = [
        root / "app" / "src" / "main" / "assets" / "dialogue.json",
        root / "app" / "src" / "main" / "assets" / "cinematics.json",
        root / "app" / "src" / "main" / "assets" / "shops.json",
    ]
    uses: list[EmoteUse] = []
    for source in sources:
        if source.is_file():
            uses.extend(collect_emote_uses(load_json(source), source.name))

    for use in uses:
        ref = speaker_index.get(norm_key(use.speaker)) or speaker_index.get(norm_key(use.speaker).split()[-1])
        emote = normalize_emote(use.emote)
        label = f"{use.source}:{use.line_id or '<unknown>'} {use.speaker} emote={emote}"
        if ref is None:
            missing.append(f"unknown speaker for emote: {label}")
            continue
        path = ref.emotes.get(emote)
        if not path:
            missing.append(f"missing emote map entry: {label} ({ref.kind} {ref.id})")
            continue
        if not existing_asset(root, path):
            missing.append(f"missing emote asset: {label} -> {path}")

    if missing:
        print("Dialogue portrait/emote validation found missing assets:")
        for item in missing:
            print(f"- {item}")
    else:
        print("Dialogue portrait/emote validation passed: no missing used emotes.")

    print(f"Checked {len(uses)} used emote reference(s).")
    return 1 if missing and args.fail_on_missing else 0


if __name__ == "__main__":
    raise SystemExit(main())