"""Create credential-free, tracked-source Gemini review folders (not runnable builds)."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
ROOTS = {"app", "desktopApp", "gameRuntime", "shared", "scripts", "tools", "docs",
         "data", "gradle", ".github", "playtests", "starborn-observatory", "world_assets"}
TEXT_EXTENSIONS = {".kt", ".kts", ".md", ".py", ".ps1", ".json", ".xml", ".html",
                   ".toml", ".gradle", ".properties", ".proto", ".yml", ".yaml",
                   ".js", ".ts", ".tsx", ".jsx", ".css", ".csv", ".txt", ".sh",
                   ".bat", ".frag", ".vert", ".sql"}
MEDIA_EXTENSIONS = {".png", ".jpg", ".jpeg", ".webp", ".gif", ".svg", ".mp3",
                    ".wav", ".ogg", ".flac", ".mp4", ".webm", ".ttf", ".otf"}
BLOCKED_DIRS = {"build", "backups", "node_modules", "__pycache__", ".idea", ".gradle",
                ".kotlin", ".git", "captures", "artifacts", "reports", "keystore"}
SECRET_PATTERNS = [
    ("private-key block", re.compile(r"-----BEGIN (?:[A-Z0-9 ]+ )?PRIVATE KEY-----")),
    ("service-account key", re.compile(r'"private_key"\s*:\s*"[^"\s]{12,}')),
    ("Google API key", re.compile(r"AIza[0-9A-Za-z_-]{30,}")),
    ("GitHub token", re.compile(r"\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{30,})\b")),
    ("OpenAI-style key", re.compile(r"\bsk-(?:proj-|svcacct-)?[A-Za-z0-9_-]{24,}\b")),
    ("AWS access key", re.compile(r"\b(?:AKIA|ASIA)[A-Z0-9]{16}\b")),
    ("literal credential", re.compile(
        r'''(?im)(?:api[_-]?key|access[_-]?token|client[_-]?secret|password|authorization)\s*[=:]\s*["'](?:Bearer\s+)?[A-Za-z0-9_+/=-]{24,}["']''')),
]
MAX_FILES = 5000
MAX_BYTES = 90 * 1024 * 1024  # leave headroom below the web app's 100 MB limit


def git(*args: str) -> bytes:
    return subprocess.check_output(["git", "-C", str(ROOT), *args])


def exclusion(path: str) -> str | None:
    p = PurePosixPath(path)
    if p.is_absolute() or ".." in p.parts or "\\" in path:
        return "unsafe path"
    name = p.name.lower()
    if any(part.lower() in BLOCKED_DIRS for part in p.parts[:-1]):
        return "generated/local directory"
    if (name.startswith(".env") or name in {"google-services.json", "local.properties", "keystore.properties"}
            or re.search(r"(?:service[-_]account|credential|api[-_]key)", name)
            or p.suffix.lower() in {".jks", ".keystore", ".pem", ".p12", ".pfx", ".key"}):
        return "credential/local configuration"
    if re.fullmatch(r"(?:autosave|save\d*)\.json", name):
        return "personal/demo save"
    if len(p.parts) > 1 and p.parts[0] not in ROOTS:
        return "outside source allowlist"
    if len(p.parts) == 1 and name not in {
        "settings.gradle.kts", "build.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat",
        "readme.md", "license", "license.md", ".gitignore", "requirements.txt", "package.json",
        "package-lock.json", "pyproject.toml",
    }:
        return "outside root allowlist"
    if p.suffix.lower() not in TEXT_EXTENSIONS and name not in {"gradlew", "license", ".gitignore"}:
        return "binary/media/archive"
    return None


def secret_findings(text: str) -> list[str]:
    return [label for label, pattern in SECRET_PATTERNS if pattern.search(text)]


def focus_member(path: str) -> bool:
    """First task: dialogue eligibility and autosave completeness, with their dependencies."""
    return (
        path.startswith("gameRuntime/src/") and any(segment in path for segment in (
            "/domain/dialogue/", "/domain/session/", "/domain/quest/", "/domain/event/",
            "/domain/inventory/", "/domain/cinematic/", "/domain/model/"))
        or any(name in path for name in (
            "ExplorationController", "AppServices", "GameSaveRepository", "DesktopSaveManager",
            "SessionInventoryBridge", "LegalRouteCampaignRunnerTest", "Dialogue", "Autosave",
            "GameSession", "EventManagerTest", "QuestRuntimeManagerTest"))
        or path.startswith("app/src/main/assets/") and PurePosixPath(path).name in {
            "dialogue.json", "events.json", "quests.json", "milestones.json", "tutorials.json",
            "items.json", "characters.json", "cinematics.json", "rooms.json", "progression.json"}
        or path.startswith("docs/desktop/") and PurePosixPath(path).name in {
            "PUBLIC_CAMPAIGN_WORLD_6.md", "CAMPAIGN_SYSTEMS_ROUTE_AUDIT.md"}
        or path.startswith("docs/") and any(word in path.lower() for word in ("save", "dialogue", "quest"))
        or PurePosixPath(path).suffix in {".kts", ".proto", ".toml"}
    )


def write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def write_package(destination: Path, sources: dict[str, bytes], manifest: dict,
                  exclusions: list[dict], assets: list[dict], focused: bool) -> None:
    destination.mkdir(parents=True, exist_ok=False)
    files = []
    for relative, content in sorted(sources.items()):
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)
        files.append({"path": relative, "bytes": len(content),
                      "sha256": hashlib.sha256(content).hexdigest()})
    write_json(destination / "REVIEW_MANIFEST.json", {
        **manifest, "package": "dialogue-autosave" if focused else "full-source",
        "included_files": files, "excluded_files": exclusions,
        "scope_note": "Focused subset; omitted dependencies are available in full-source." if focused
                      else "Tracked working-tree text only. Binary assets and local/untracked files omitted.",
        "secret_scan": {"result": "PASS", "patterns": [label for label, _ in SECRET_PATTERNS],
                        "note": "Heuristic checks, not a guarantee that every possible secret is detectable."},
    })
    write_json(destination / "ASSET_PATHS.json", assets)
    (destination / "START_HERE.md").write_text(
        "# Starborn Gemini review snapshot\n\n"
        f"Source branch: `{manifest['branch']}`\n\nSource commit: `{manifest['commit']}`\n\n"
        "Use **Import Code > Upload folder** for this folder. It is a review snapshot, not a buildable checkout. "
        "Binary assets, credentials, save-slot JSON, caches, and untracked files are excluded. "
        "ASSET_PATHS.json inventories omitted media; REVIEW_MANIFEST.json records file hashes and exclusions. "
        "Refresh this export after source changes. Never use the older starborn_source_clean.zip.\n\n"
        "Read REVIEW_PROMPT.md first. The initial task is dialogue conditions and autosave coverage. "
        "Read actual call sites and tests before concluding that a helper is the production path. "
        "Android and desktop share a JVM runtime; this review does not require an iOS migration. "
        "Levels unlock skills; automatic stat growth is a separate design change. "
        "No Player.speed field exists. Combat uses ATB, including runtime speed buffs.\n\n"
        "The six-world seeded campaign now finishes all 30 main quests using public actions and real combat. "
        "That establishes one route, not universal correctness, balance, or human playtest quality. "
        "Validation history is recorded in docs/desktop/PUBLIC_CAMPAIGN_WORLD_6.md.\n\n"
        "Return findings and proposed changes to the main Starborn workspace for compilation, tests, and review. "
        "Do not execute generated bulk replacement scripts.\n", encoding="utf-8")
    (destination / "REVIEW_PROMPT.md").write_text(
        "# Initial review task\n\n"
        "Review dialogue condition handling and Android autosave change detection in this exact snapshot.\n\n"
        "1. Confirm you can read DialogueService.kt, GameSessionState.kt, and AppServices.kt. "
        "Quote a short relevant function excerpt from each. If unavailable or truncated, stop and identify missing files.\n"
        "2. Trace displayed, skipped, and chosen dialogue lines through production trigger dispatch. "
        "Check unmet-condition side effects and skipped-node cycles. Do not assume the fallback trigger helper is the normal path.\n"
        "3. Trace fingerprint(), autosave scheduling, throttle/cancellation, battle checkpoints, and persistence. "
        "Determine which meaningful state changes can be missed. Distinguish manual save parity from autosave coverage.\n"
        "4. For each finding give path, symbol, observed behavior, minimal reproduction, impact, confidence, "
        "and a targeted regression test. Separate confirmed defects, hypotheses, and intentional rules.\n"
        "5. Propose a minimal unified diff against these files and explain test commands. "
        "Avoid stat growth, damage/crit changes, module migration, and broad refactors in this task. "
        "Preserve dialogue ordering, quest handoffs, inventory synchronization, and battle recovery semantics.\n"
        "6. Do not claim code compiled, tests passed, or gameplay improved unless you actually ran and observed it. "
        "List any dependencies or content you could not inspect. If you need more files, request exact paths.\n",
        encoding="utf-8")
    final_files = [p for p in destination.rglob("*") if p.is_file()]
    total = sum(p.stat().st_size for p in final_files)
    if len(final_files) > MAX_FILES or total > MAX_BYTES:
        raise RuntimeError("Package exceeds the configured file/size limit; do not upload it.")
    for path in final_files:
        if secret_findings(path.read_text(encoding="utf-8")):
            raise RuntimeError(f"Final secret scan rejected {path.relative_to(destination)}; do not upload.")
    print(f"{destination}: {len(final_files)} files, {total / 1024 / 1024:.2f} MiB; secret scan PASS")


def write_text_uploads(destination: Path, sources: dict[str, bytes], manifest: dict,
                       prompt: str) -> None:
    """Ordinary-file upload fallback, keeping each source file intact and labeled."""
    destination.mkdir(parents=True, exist_ok=False)
    header = (
        "STARBORN REVIEW SNAPSHOT\n"
        f"Branch: {manifest['branch']}\nCommit: {manifest['commit']}\n"
        "Tracked working-tree text only; credentials, saves, media and builds excluded.\n"
        "Paths below refer to the original repository. FILE END marks complete contents.\n"
        "This upload is review context, not a runnable checkout. Do not assume all parts fit the model context.\n\n"
    )
    priority = {"DialogueService.kt": 0, "GameSessionState.kt": 1, "AppServices.kt": 2,
                "DesktopAppServices.kt": 3, "ExplorationController.kt": 4}
    ordered = sorted(sources, key=lambda path: (priority.get(PurePosixPath(path).name, 5), path))
    parts: list[str] = []
    current = header
    for relative in ordered:
        content = sources[relative].decode("utf-8")
        entry = (f"\n=== FILE BEGIN: {relative} ===\n"
                 f"SHA256: {hashlib.sha256(sources[relative]).hexdigest()}\n"
                 f"{content}\n=== FILE END: {relative} ===\n")
        if len(entry.encode("utf-8")) > 3 * 1024 * 1024:
            raise RuntimeError(f"Source file too large for text fallback: {relative}")
        if len(current.encode("utf-8")) + len(entry.encode("utf-8")) > 3 * 1024 * 1024:
            parts.append(current)
            current = header
        current += entry
    parts.append(current)
    index = []
    for number, text in enumerate(parts, 1):
        name = f"starborn-{destination.name}-{number:02d}-of-{len(parts):02d}.txt"
        path = destination / name
        path.write_text(text, encoding="utf-8")
        index.append(f"{name}: {path.stat().st_size} bytes")
    (destination / "REVIEW_PROMPT.txt").write_text(prompt, encoding="utf-8")
    (destination / "UPLOAD_INSTRUCTIONS.txt").write_text(
        "Use ordinary Upload files, not folder import or ZIP.\n"
        "Start with dialogue-autosave for the initial focused task.\n"
        "Attach all numbered TXT parts of your chosen package and paste REVIEW_PROMPT.txt into the chat.\n"
        "Ask Gemini to confirm it can read the three requested functions before reviewing.\n"
        "These complete-source bundles still may exceed model context; use smaller tasks if warned.\n\n"
        + "\n".join(index) + "\n", encoding="utf-8")
    if len(parts) > 10:
        raise RuntimeError("Text fallback requires more than ten attachments; narrow the scope.")
    for path in destination.iterdir():
        if secret_findings(path.read_text(encoding="utf-8")):
            raise RuntimeError(f"Final text scan rejected {path.name}; do not upload.")
    print(f"{destination}: {len(parts)} numbered TXT upload(s); secret scan PASS")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-parent", type=Path, default=ROOT / "output" / "gemini-review")
    args = parser.parse_args()
    parent = args.output_parent.resolve()
    # Only create unique folders; never remove or overwrite an existing export.
    if parent == ROOT or ROOT in parent.parents and parent.parts[len(ROOT.parts)] in ROOTS:
        parser.error("Output must not be the repository root or a source directory.")
    tracked = sorted(set(git("ls-files", "-z").decode("utf-8").split("\0")) - {""})
    sources: dict[str, bytes] = {}
    exclusions: list[dict] = []
    assets: list[dict] = []
    blocked: list[str] = []
    for relative in tracked:
        reason = exclusion(relative)
        source = ROOT / relative
        if source.is_symlink() or ROOT not in source.resolve().parents:
            reason = "symlink or path outside repository"
        if reason:
            exclusions.append({"path": relative, "reason": reason})
            if reason == "binary/media/archive" and source.is_file() and source.suffix.lower() in MEDIA_EXTENSIONS:
                assets.append({"path": relative, "bytes": source.stat().st_size})
            continue
        if not source.is_file():
            exclusions.append({"path": relative, "reason": "missing in working tree"})
            continue
        content = source.read_bytes()
        try:
            text = content.decode("utf-8-sig")
        except UnicodeDecodeError:
            exclusions.append({"path": relative, "reason": "not UTF-8 text"})
            continue
        findings = secret_findings(text)
        if findings:
            blocked.append(f"{relative}: {', '.join(findings)}")
        else:
            sources[relative] = text.encode("utf-8")
    if blocked:
        raise SystemExit("Export blocked by credential scan (values withheld):\n" + "\n".join(blocked))
    manifest = {
        "commit": git("rev-parse", "HEAD").decode().strip(),
        "branch": git("rev-parse", "--abbrev-ref", "HEAD").decode().strip(),
        "generated_utc": datetime.now(timezone.utc).isoformat(),
        "tracked_working_tree_changes": git("diff", "HEAD", "--name-only").decode().splitlines(),
        "untracked_files_included": False,
    }
    destination = parent / datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    write_package(destination / "full-source", sources, manifest, exclusions, assets, False)
    focused = {path: content for path, content in sources.items() if focus_member(path)}
    focused_exclusions = exclusions + [{"path": path, "reason": "outside focused task"}
                                      for path in sorted(sources) if path not in focused]
    write_package(destination / "dialogue-autosave", focused, manifest, focused_exclusions, assets, True)
    prompt = (destination / "dialogue-autosave" / "REVIEW_PROMPT.md").read_text(encoding="utf-8")
    write_text_uploads(destination / "text-uploads" / "dialogue-autosave", focused, manifest, prompt)
    write_text_uploads(destination / "text-uploads" / "full-source", sources, manifest, prompt)


if __name__ == "__main__":
    main()
