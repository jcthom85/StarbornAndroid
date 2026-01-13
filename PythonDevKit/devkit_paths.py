#!/usr/bin/env python3
"""
Shared path resolution for the Starborn Python dev tools.

Goals:
- Point all JSON access to the Android assets directory: app/src/main/assets
- Allow users to pick a project directory once (env var or config file)
- Provide a single helper to return project_root, assets_dir, and skill_trees_dir
"""
from __future__ import annotations
import json, os
from dataclasses import dataclass
from pathlib import Path
from typing import Optional, Sequence

# Environment variable overrides (highest precedence)
ENV_KEYS: Sequence[str] = (
    "STARBORN_ANDROID_ROOT",
    "STARBORN_PROJECT_ROOT",
    "STARBORN_DEVKIT_ROOT",
)

# Config files we will read (first existing wins). We only write to the first by default.
CONFIG_PATHS = [
    Path.home() / ".starborn_devkit.json",
    Path(__file__).resolve().parent / "devkit_config.json",
]

# Core JSON markers expected inside assets/
ASSET_MARKERS = ("items.json", "quests.json", "enemies.json", "skills.json")


@dataclass
class DevkitPaths:
    project_root: Path   # Android project root (contains app/src/main/assets)
    assets_dir: Path     # app/src/main/assets
    skill_trees_dir: Path  # app/src/main/assets/skill_trees


def _normalize(path: Path) -> Path:
    return Path(path).expanduser().resolve()


def _assets_from_project_root(project_root: Path) -> Path:
    return _normalize(project_root) / "app" / "src" / "main" / "assets"


def _looks_like_assets_dir(path: Path) -> bool:
    if not path.is_dir():
        return False
    hits = sum((path / m).exists() for m in ASSET_MARKERS)
    return hits >= 2  # avoid overly strict checks while still rejecting false positives


def _project_from_assets_dir(assets_dir: Path) -> Optional[Path]:
    assets_dir = _normalize(assets_dir)
    parts = assets_dir.parts
    try:
        idx = parts.index("app")
    except ValueError:
        return None
    if idx + 3 < len(parts) and parts[idx + 1] == "src" and parts[idx + 2] == "main" and parts[idx + 3] == "assets":
        return Path(*parts[:idx]) if idx > 0 else assets_dir
    return None


def _coerce_to_project_root(candidate: Path) -> Optional[Path]:
    """
    Accepts either the project root or the assets directory and returns project root if valid.
    """
    candidate = _normalize(candidate)
    if candidate.is_file():
        candidate = candidate.parent

    if _looks_like_assets_dir(candidate):
        proj = _project_from_assets_dir(candidate)
        return proj or candidate

    assets_dir = _assets_from_project_root(candidate)
    if _looks_like_assets_dir(assets_dir):
        return candidate
    return None


def _load_saved_root() -> Optional[Path]:
    for cfg in CONFIG_PATHS:
        if not cfg.exists():
            continue
        try:
            data = json.loads(cfg.read_text(encoding="utf-8"))
            val = data.get("project_root") or data.get("root")
            if val:
                return Path(val)
        except Exception:
            continue
    return None


def _save_root(root: Path, dest: Optional[Path] = None) -> Path:
    target = dest or CONFIG_PATHS[0]
    target = _normalize(target)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps({"project_root": str(root)}, indent=2), encoding="utf-8")
    return target


def _walk_for_assets(seed: Path, depth: int = 6) -> Optional[Path]:
    cur = _normalize(seed)
    if cur.is_file():
        cur = cur.parent
    for _ in range(depth):
        if _looks_like_assets_dir(cur):
            proj = _project_from_assets_dir(cur)
            if proj:
                return proj
        assets_dir = _assets_from_project_root(cur)
        if _looks_like_assets_dir(assets_dir):
            return cur
        cur = cur.parent
    return None


def resolve_project_root(start: Optional[Path | str] = None) -> Path:
    """
    Best-effort resolution order:
    1) Explicit start (can be project root or assets dir)
    2) Env vars (STARBORN_ANDROID_ROOT / STARBORN_PROJECT_ROOT / STARBORN_DEVKIT_ROOT)
    3) Saved config (~/.starborn_devkit.json or devkit_config.json)
    4) Walk upward from start/cwd/this file looking for app/src/main/assets
    """
    if start:
        root = _coerce_to_project_root(Path(start))
        if root:
            return root

    for key in ENV_KEYS:
        val = os.getenv(key)
        if not val:
            continue
        root = _coerce_to_project_root(Path(val))
        if root:
            return root

    saved = _load_saved_root()
    if saved:
        root = _coerce_to_project_root(saved)
        if root:
            return root

    seeds = [
        Path(start) if start else None,
        Path.cwd(),
        Path(__file__).resolve().parent,
    ]
    for seed in seeds:
        if not seed:
            continue
        root = _walk_for_assets(seed)
        if root:
            return root

    # Last resort: accept cwd even if markers are missing
    return _normalize(start or Path.cwd())


def resolve_paths(start: Optional[Path | str] = None) -> DevkitPaths:
    project_root = resolve_project_root(start)
    assets_dir = _assets_from_project_root(project_root)

    # Support callers that pass assets/ directly
    if not _looks_like_assets_dir(assets_dir) and _looks_like_assets_dir(project_root):
        assets_dir = project_root
        maybe_root = _project_from_assets_dir(assets_dir)
        if maybe_root:
            project_root = maybe_root

    skill_trees_dir = assets_dir / "skill_trees"
    return DevkitPaths(project_root=project_root, assets_dir=assets_dir, skill_trees_dir=skill_trees_dir)


def remember_project_root(project_root: Path, dest: Optional[Path] = None) -> Path:
    """
    Persist the chosen project root so other tools can reuse it.
    """
    return _save_root(_normalize(project_root), dest)


def main(argv: Optional[Sequence[str]] = None) -> int:
    import argparse

    parser = argparse.ArgumentParser(description="Resolve Starborn project paths or save a project root for reuse.")
    parser.add_argument("--start", type=str, help="Optional path to start resolution from (project root or assets dir).")
    parser.add_argument("--set-root", type=str, help="Save this project root (or assets dir) for later runs.")
    parser.add_argument("--config-path", type=str, help="Optional config file path to write instead of the default.")
    parser.add_argument("--json", action="store_true", help="Output paths as JSON for scripting.")
    args = parser.parse_args(argv)

    if args.set_root:
        root = resolve_project_root(args.set_root)
        cfg = remember_project_root(root, Path(args.config_path) if args.config_path else None)
        print(f"Saved project root: {root}")
        print(f"Config: {cfg}")
        return 0

    paths = resolve_paths(args.start)
    if args.json:
        print(json.dumps({
            "project_root": str(paths.project_root),
            "assets_dir": str(paths.assets_dir),
            "skill_trees_dir": str(paths.skill_trees_dir),
        }, indent=2))
    else:
        print(f"Project root : {paths.project_root}")
        print(f"Assets dir   : {paths.assets_dir}")
        print(f"Skill trees  : {paths.skill_trees_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
