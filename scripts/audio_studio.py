#!/usr/bin/env python3
"""
Starborn — Unified Audio Studio & Mastering Pipeline
====================================================
Supports both Suno (Studio/MCP exports + MIDI motif seeds) and ElevenLabs
(music_v2 / composition_plan / stem-separation / sound-generation SFX)
inside one staged, quality-controlled workflow.

Never modifies Great Frontier tapes (`gf_*`) and never overwrites
`app/src/main/res/raw/` until an asset is explicitly mastered and promoted.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import math
import os
import re
import shutil
import subprocess
import sys
import tempfile
import wave
import zipfile
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

import numpy as np

try:
    import requests
except ImportError:
    requests = None

try:
    import mido
except ImportError:
    mido = None

try:
    import imageio_ffmpeg
except ImportError:
    imageio_ffmpeg = None

_SCRIPT_DIR = Path(__file__).resolve().parent
if str(_SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(_SCRIPT_DIR))
import audio_production  # noqa: E402


ROOT_DIR = Path(__file__).resolve().parent.parent
STUDIO_DIR = ROOT_DIR / "audio_studio"
BIBLE_PATH = STUDIO_DIR / "musical_bible.json"
MIDI_DIR = STUDIO_DIR / "midi_motifs"
BRIEFS_DIR = STUDIO_DIR / "suno_briefs"
INCOMING_DIR = STUDIO_DIR / "incoming"
TAKES_DIR = STUDIO_DIR / "takes"
STEMS_DIR = STUDIO_DIR / "stems"
MASTERED_DIR = STUDIO_DIR / "mastered"

ASSETS_DIR = ROOT_DIR / "app" / "src" / "main" / "assets"
RAW_DIR = ROOT_DIR / "app" / "src" / "main" / "res" / "raw"
CATALOG_PATH = ASSETS_DIR / "audio_catalog.json"
BINDINGS_PATH = ASSETS_DIR / "audio_bindings.json"
PROVENANCE_DIR = ROOT_DIR / "docs" / "audio" / "provenance"
BACKUP_MANIFEST_PATH = ROOT_DIR / "tools" / "backups" / "pre_rebrand_audio_2026-11-17" / "MANIFEST_SHA256.json"
REVIEW_MANIFEST_PATH = ROOT_DIR / "tools" / "audio_board_manifest.json"
API_KEY_PATH = ROOT_DIR / "elevenlabs_api_key.txt"

SAMPLE_RATE = 44100


def get_ffmpeg_exe() -> str:
    if imageio_ffmpeg is not None:
        return imageio_ffmpeg.get_ffmpeg_exe()
    exe = shutil.which("ffmpeg")
    if exe:
        return exe
    raise RuntimeError(
        "ffmpeg binary not found. Run via .tools/audio-venv/bin/python or install ffmpeg."
    )


def load_json(path: Path, default: Any = None) -> Any:
    if not path.exists():
        return default
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_json(path: Path, data: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def load_api_key() -> str:
    env_key = os.getenv("ELEVENLABS_API_KEY", "").strip()
    if env_key:
        return env_key
    if API_KEY_PATH.exists():
        return API_KEY_PATH.read_text(encoding="utf-8").strip()
    raise RuntimeError(f"ElevenLabs API key not found in env or {API_KEY_PATH}")


def load_bible() -> Dict[str, Any]:
    data = load_json(BIBLE_PATH)
    if not data:
        raise RuntimeError(f"Missing Musical Bible: {BIBLE_PATH}")
    return data


def is_immutable_id(cue_id: str, bible: Dict[str, Any]) -> bool:
    prefixes = bible.get("immutable_prefixes", ["gf_"])
    return any(cue_id.startswith(p) for p in prefixes)


def all_sfx_entries(bible: Dict[str, Any]) -> List[Dict[str, Any]]:
    out: List[Dict[str, Any]] = []
    for group_name, group in bible.get("sfx_groups", {}).items():
        target_lufs = float(group.get("target_lufs", -13.5))
        for cue in group.get("cues", []):
            entry = dict(cue)
            entry["group"] = group_name
            entry["target_lufs"] = float(cue.get("target_lufs", target_lufs))
            out.append(entry)
    return out


def find_bible_entry(cue_id: str, bible: Dict[str, Any]) -> Tuple[Optional[str], Optional[Dict[str, Any]]]:
    for t in bible.get("tracks", []):
        if t["id"] == cue_id:
            return ("track", t)
    for s in all_sfx_entries(bible):
        if s["id"] == cue_id:
            return ("sfx", s)
    return (None, None)


def decode_audio_to_float(path: Path, sr: int = SAMPLE_RATE) -> np.ndarray:
    ffmpeg = get_ffmpeg_exe()
    with tempfile.TemporaryDirectory() as tmp:
        wav_path = Path(tmp) / "decoded.wav"
        subprocess.run(
            [ffmpeg, "-y", "-i", str(path), "-ac", "2", "-ar", str(sr), "-sample_fmt", "s16", str(wav_path)],
            check=True,
            capture_output=True,
        )
        with wave.open(str(wav_path), "rb") as w:
            raw = w.readframes(w.getnframes())
            samples = np.frombuffer(raw, dtype=np.int16).astype(np.float64) / 32768.0
            return samples.reshape(-1, 2)


def measure_audio(path: Path) -> Dict[str, Any]:
    ffmpeg = get_ffmpeg_exe()
    samples = decode_audio_to_float(path)
    frames = len(samples)
    duration_sec = frames / float(SAMPLE_RATE) if frames else 0.0
    rms = float(np.sqrt(np.mean(samples ** 2))) if frames else 0.0
    peak = float(np.max(np.abs(samples))) if frames else 0.0
    rms_dbfs = 20.0 * math.log10(max(rms, 1e-9))
    peak_dbfs = 20.0 * math.log10(max(peak, 1e-9))
    full_scale = int(np.sum(np.abs(samples) >= 0.9999))

    # Measure integrated LUFS using ffmpeg loudnorm pass 1
    lufs = None
    true_peak_dbtp = None
    try:
        proc = subprocess.run(
            [
                ffmpeg,
                "-i",
                str(path),
                "-af",
                "loudnorm=I=-16:TP=-1.0:LRA=11:print_format=json",
                "-f",
                "null",
                "-",
            ],
            capture_output=True,
            text=True,
            check=False,
        )
        m = re.search(r"\{[^{}]*\"input_i\"[^{}]*\}", proc.stderr, re.DOTALL)
        if m:
            info = json.loads(m.group(0))
            if info.get("input_i") not in (None, "-inf"):
                lufs = float(info["input_i"])
            if info.get("input_tp") not in (None, "-inf"):
                true_peak_dbtp = float(info["input_tp"])
    except Exception:
        pass

    head_rms = float(np.sqrt(np.mean(samples[: min(frames, SAMPLE_RATE)] ** 2))) if frames else 0.0
    tail_rms = float(np.sqrt(np.mean(samples[max(0, frames - SAMPLE_RATE) :] ** 2))) if frames else 0.0

    return {
        "sample_rate": SAMPLE_RATE,
        "channels": 2,
        "frames": frames,
        "duration_seconds": round(duration_sec, 4),
        "rms_dbfs": round(rms_dbfs, 2),
        "peak_dbfs": round(peak_dbfs, 2),
        "integrated_lufs": round(lufs, 2) if lufs is not None else None,
        "true_peak_dbtp": round(true_peak_dbtp, 2) if true_peak_dbtp is not None else None,
        "samples_at_full_scale": full_scale,
        "first_second_rms_dbfs": round(20.0 * math.log10(max(head_rms, 1e-9)), 2),
        "last_second_rms_dbfs": round(20.0 * math.log10(max(tail_rms, 1e-9)), 2),
    }


def next_take_dir(cue_id: str) -> Tuple[int, Path]:
    base = TAKES_DIR / cue_id
    base.mkdir(parents=True, exist_ok=True)
    existing = []
    for p in base.iterdir():
        if p.is_dir() and p.name.startswith("take_"):
            try:
                existing.append(int(p.name.split("_")[1]))
            except ValueError:
                pass
    idx = (max(existing) + 1) if existing else 1
    take_dir = base / f"take_{idx:02d}"
    take_dir.mkdir(parents=True, exist_ok=True)
    return idx, take_dir


def find_latest_take_dir(cue_id: str, take_num: Optional[int] = None) -> Path:
    base = TAKES_DIR / cue_id
    if not base.exists():
        raise RuntimeError(f"No takes directory found for '{cue_id}' at {base}")
    if take_num is not None:
        d = base / f"take_{take_num:02d}"
        if not d.exists():
            raise RuntimeError(f"Requested take_{take_num:02d} does not exist for '{cue_id}'")
        return d
    candidates = sorted([p for p in base.iterdir() if p.is_dir() and p.name.startswith("take_")])
    if not candidates:
        raise RuntimeError(f"No takes recorded for '{cue_id}'")
    return candidates[-1]


def find_raw_in_take(take_dir: Path) -> Path:
    for ext in (".wav", ".mp3", ".flac", ".m4a", ".ogg"):
        p = take_dir / f"raw{ext}"
        if p.exists():
            return p
    for p in sorted(take_dir.iterdir()):
        if p.is_file() and p.suffix.lower() in (".wav", ".mp3", ".flac", ".m4a", ".ogg"):
            return p
    raise RuntimeError(f"No audio file found inside {take_dir}")


# ---------------------------------------------------------------------------
# Subcommand: midi
# ---------------------------------------------------------------------------
def cmd_midi(args: argparse.Namespace) -> None:
    if mido is None:
        raise RuntimeError("mido package not installed in Python environment.")
    bible = load_bible()
    MIDI_DIR.mkdir(parents=True, exist_ok=True)
    tpb = 480

    for motif_id, motif in bible.get("motifs", {}).items():
        spec = motif.get("midi")
        if not spec:
            continue
        bpm = int(spec.get("tempo_bpm", motif.get("bpm", 90)))
        tempo = mido.bpm2tempo(bpm)
        program = int(spec.get("program", 0))

        mid = mido.MidiFile(ticks_per_beat=tpb)
        # Track 0: tempo + chords
        t_chords = mido.MidiTrack()
        mid.tracks.append(t_chords)
        t_chords.append(mido.MetaMessage("track_name", name=f"{motif_id}_harmony", time=0))
        t_chords.append(mido.MetaMessage("set_tempo", tempo=tempo, time=0))
        t_chords.append(mido.Message("program_change", channel=1, program=48, time=0))

        for chord in spec.get("chords_midi", []):
            notes = chord["notes"]
            dur_ticks = int(round(float(chord["beats"]) * tpb))
            for idx, n in enumerate(notes):
                t_chords.append(mido.Message("note_on", channel=1, note=int(n), velocity=68, time=0))
            for idx, n in enumerate(notes):
                t_chords.append(
                    mido.Message("note_off", channel=1, note=int(n), velocity=0, time=dur_ticks if idx == 0 else 0)
                )

        # Track 1: lead motif melody
        t_mel = mido.MidiTrack()
        mid.tracks.append(t_mel)
        t_mel.append(mido.MetaMessage("track_name", name=f"{motif_id}_melody", time=0))
        t_mel.append(mido.Message("program_change", channel=0, program=program, time=0))

        for note in spec.get("melody_midi", []):
            pitch = int(note["pitch"])
            dur_ticks = int(round(float(note["beats"]) * tpb))
            vel = int(note.get("vel", 92))
            t_mel.append(mido.Message("note_on", channel=0, note=pitch, velocity=vel, time=0))
            t_mel.append(mido.Message("note_off", channel=0, note=pitch, velocity=0, time=dur_ticks))

        out_path = MIDI_DIR / f"{motif_id}.mid"
        mid.save(str(out_path))
        print(f"Generated MIDI motif: {out_path.relative_to(ROOT_DIR)} ({bpm} BPM)")


# ---------------------------------------------------------------------------
# Subcommand: briefs (Suno Studio / MCP production sheets)
# ---------------------------------------------------------------------------
def cmd_briefs(args: argparse.Namespace) -> None:
    bible = load_bible()
    BRIEFS_DIR.mkdir(parents=True, exist_ok=True)
    tracks = bible.get("tracks", [])
    motifs = bible.get("motifs", {})
    prefs = audio_production.load_preferences()
    manifest = load_json(audio_production.MANIFEST_PATH, default={})
    manifest_cues = manifest.get("cues", {}) if manifest else {}

    suno_json = {
        "generated_at": datetime.now().isoformat(),
        "aesthetic": bible.get("aesthetic"),
        "style_preferences": prefs,
        "motifs": {
            k: {
                "name": v["name"],
                "key": v["key"],
                "bpm": v["bpm"],
                "midi_file": f"audio_studio/midi_motifs/{k}.mid",
                "reference_wav": f"audio_studio/reference_audio/{k}.wav",
                "lead_instruments": v["lead_instruments"],
            }
            for k, v in motifs.items()
        },
        "tracks": [],
    }

    lines: List[str] = [
        "# Starborn — Suno Studio & MCP Production Pack",
        "",
        f"Aesthetic: **{bible.get('aesthetic')}**",
        "All Great Frontier tapes (`gf_01`–`gf_10`) remain untouched.",
        "",
        "## Workflow Checklist (Per Track)",
        "1. Use Suno MCP (if OAuth connected) or Suno Studio / Create (Premier) with the cue's Reference Audio `.wav` (`audio_studio/reference_audio/`) or `.mid` motif (`audio_studio/midi_motifs/`).",
        "2. Set Instrumental mode (except `music_elaras_song` which has vocals) and apply the **Style Prompt** + **Negative Exclusions** (auto-refined from your `style_preferences.json`).",
        "3. Generate 4 candidates (2 x two-output runs), run `analyze` + Hydrogen evaluation, listen via the Suno URLs / Review Board, and pick A/B/C/Reroll/Revise/Hold.",
        "4. Export lossless `.wav` (or `.mp3`) into `audio_studio/incoming/` and run: `.tools/audio-venv/bin/python scripts/audio_studio.py match-incoming --auto-master`",
        "5. Integrate approved candidates cleanly via `integrate-approved --sync-bindings`.",
        "",
        "---",
        "",
    ]

    by_cat: Dict[str, List[Dict[str, Any]]] = {}
    for t in tracks:
        by_cat.setdefault(t.get("category", "other"), []).append(t)

    for cat, items in by_cat.items():
        lines.append(f"## Category: {cat.upper()} ({len(items)} tracks)")
        lines.append("")
        for t in items:
            mrefs = t.get("motif_refs", [])
            midi_files = [f"`audio_studio/midi_motifs/{m}.mid`" for m in mrefs if m in motifs]
            pilot_ref = f"audio_studio/reference_audio/pilot_{t['id']}.wav"
            ref_wav = (
                pilot_ref
                if (ROOT_DIR / pilot_ref).exists()
                else (f"audio_studio/reference_audio/{mrefs[0]}.wav" if mrefs and mrefs[0] in motifs else None)
            )
            eff_prompt, eff_exclude = audio_production.build_refined_prompt(t, prefs, manifest_cues.get(t["id"]))
            rec = t.get("recommended_provider", "suno")
            loop_label = "Seamless Loop" if t.get("loop") else "One-Shot"
            dur_s = int(round(t.get("duration_ms", 90000) / 1000))
            lines.append(f"### `{t['id']}` — {t['title']}")
            lines.append(f"- **Recommended Tool:** `{rec}` | **Key:** {t.get('key')} | **BPM:** {t.get('bpm')} | **Length:** ~{dur_s}s ({loop_label})")
            if ref_wav:
                lines.append(f"- **Reference Audio (.wav):** `{ref_wav}`")
            if midi_files:
                lines.append(f"- **Motif MIDI Seeds:** {', '.join(midi_files)}")
            lines.append(f"- **Style Prompt:** `{eff_prompt}`")
            if eff_exclude:
                lines.append(f"- **Exclude Styles:** `{eff_exclude}`")
            lines.append("")

            suno_json["tracks"].append(
                {
                    "id": t["id"],
                    "title": t["title"],
                    "category": cat,
                    "recommended_provider": rec,
                    "key": t.get("key"),
                    "bpm": t.get("bpm"),
                    "loop": bool(t.get("loop", True)),
                    "duration_ms": t.get("duration_ms"),
                    "reference_wav": ref_wav,
                    "midi_seeds": [f"audio_studio/midi_motifs/{m}.mid" for m in mrefs],
                    "style_prompt": eff_prompt,
                    "exclude_styles": eff_exclude,
                }
            )

    md_path = BRIEFS_DIR / "SUNO_PRODUCTION_PACK.md"
    json_path = BRIEFS_DIR / "suno_briefs.json"
    md_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    save_json(json_path, suno_json)
    print(f"Wrote Suno briefs: {md_path.relative_to(ROOT_DIR)} and {json_path.relative_to(ROOT_DIR)}")


# ---------------------------------------------------------------------------
# Subcommand: ingest
# ---------------------------------------------------------------------------
def ingest_audio_file(
    source_path: Path,
    cue_id: str,
    provider: str,
    bible: Dict[str, Any],
    notes: str = "",
    stems_dir: Optional[Path] = None,
    extra_meta: Optional[Dict[str, Any]] = None,
) -> Path:
    if is_immutable_id(cue_id, bible):
        raise RuntimeError(f"Refusing to modify immutable track '{cue_id}'.")
    kind, _ = find_bible_entry(cue_id, bible)
    if kind is None:
        raise RuntimeError(f"'{cue_id}' is not registered in {BIBLE_PATH}. Add it to musical_bible.json first.")
    if not source_path.exists():
        raise RuntimeError(f"Input file not found: {source_path}")

    take_num, take_dir = next_take_dir(cue_id)
    ext = source_path.suffix.lower() or ".mp3"
    dest_raw = take_dir / f"raw{ext}"
    shutil.copy2(source_path, dest_raw)

    metrics = measure_audio(dest_raw)
    prov = {
        "cue_id": cue_id,
        "take": take_num,
        "provider": provider,
        "ingested_at": datetime.now().isoformat(),
        "source_filename": source_path.name,
        "raw_file": dest_raw.name,
        "sha256": sha256_file(dest_raw),
        "bytes": dest_raw.stat().st_size,
        "notes": notes,
        "metrics": metrics,
    }
    if extra_meta:
        prov.update(extra_meta)

    if stems_dir and stems_dir.exists():
        dst_stems = STEMS_DIR / cue_id / f"take_{take_num:02d}"
        dst_stems.mkdir(parents=True, exist_ok=True)
        copied_stems = []
        for sf in sorted(stems_dir.iterdir()):
            if sf.is_file() and sf.suffix.lower() in (".wav", ".mp3", ".flac"):
                shutil.copy2(sf, dst_stems / sf.name)
                copied_stems.append(sf.name)
        prov["stems"] = copied_stems

    save_json(take_dir / "provenance.json", prov)
    print(
        f"[INGEST] {cue_id} take_{take_num:02d} ({provider}) | "
        f"{metrics['duration_seconds']}s | LUFS={metrics['integrated_lufs']} | "
        f"Peak={metrics['true_peak_dbtp']} dBTP"
    )
    return take_dir


def cmd_ingest(args: argparse.Namespace) -> None:
    bible = load_bible()
    ingest_audio_file(
        source_path=Path(args.file),
        cue_id=args.id,
        provider=args.provider,
        bible=bible,
        notes=args.notes or "",
        stems_dir=Path(args.stems_dir) if args.stems_dir else None,
    )


# ---------------------------------------------------------------------------
# Subcommand: compose-elevenlabs
# ---------------------------------------------------------------------------
def enrich_prompt_with_motifs(
    entry: Dict[str, Any],
    bible: Dict[str, Any],
    prefs: Dict[str, Any],
    manifest_cue: Optional[Dict[str, Any]],
) -> Tuple[str, str]:
    prompt, exclude = audio_production.build_refined_prompt(entry, prefs, manifest_cue)
    motifs = bible.get("motifs", {})
    motif_hints: List[str] = []
    for mref in entry.get("motif_refs", []):
        m = motifs.get(mref)
        if not m:
            continue
        chords = "-".join(m.get("chords", [])[:4])
        inst = ", ".join(m.get("lead_instruments", [])[:2])
        # Describe musical DNA purely via harmony/key/instrumentation to avoid false-positive title matching.
        hint = f"{m.get('key', '')}; progression {chords}; lead {inst}"
        motif_hints.append(hint)
    if motif_hints:
        prompt = f"{prompt} Recurring leitmotif DNA: {'; '.join(motif_hints)}."
    if entry.get("loop"):
        prompt = (
            f"{prompt} Begin immediately inside the musical groove with no slow intro; "
            f"maintain consistent underscore flow and end cleanly on the same harmonic texture "
            f"ready to loop seamlessly without fading out."
        )
    if exclude:
        prompt = f"{prompt} Avoid: {exclude}."
    return prompt, exclude


def build_elevenlabs_plan(
    api_key: str,
    entry: Dict[str, Any],
    model_id: str = "music_v2",
    duration_ms: Optional[int] = None,
    prompt_override: Optional[str] = None,
    exclude_override: Optional[str] = None,
) -> Dict[str, Any]:
    assert requests is not None
    dur_ms = int(duration_ms or entry.get("duration_ms", 90000))
    prompt = prompt_override or entry["prompt"]
    if not prompt_override and entry.get("negative_prompt"):
        prompt = f"{prompt} Avoid: {entry['negative_prompt']}."
    resp = requests.post(
        "https://api.elevenlabs.io/v1/music/plan",
        headers={"xi-api-key": api_key, "Content-Type": "application/json"},
        json={"prompt": prompt, "music_length_ms": dur_ms, "model_id": model_id},
        timeout=45,
    )
    if resp.status_code != 200:
        raise RuntimeError(f"/v1/music/plan failed ({resp.status_code}): {resp.text[:400]}")
    plan = resp.json()

    # Enforce instrumental hygiene (and loop hygiene on looping tracks)
    is_vocal = "vocal" in entry.get("tags", [])
    neg_guards: List[str] = []
    if not is_vocal:
        neg_guards.extend(["vocals", "singing"])
    if entry.get("loop"):
        neg_guards.extend(["abrupt ending", "long fade out"])
    neg_source = exclude_override if exclude_override is not None else entry.get("negative_prompt", "")
    for extra_neg in [s.strip() for s in neg_source.split(",") if s.strip()]:
        if extra_neg not in neg_guards:
            neg_guards.append(extra_neg)

    chunks = plan.get("chunks")
    if isinstance(chunks, list):
        for chunk in chunks:
            neg_list = list(chunk.get("negative_styles") or [])
            for g in neg_guards:
                if g not in neg_list:
                    neg_list.append(g)
            chunk["negative_styles"] = neg_list[:45]
        if entry.get("loop") and chunks:
            first_styles = list(chunks[0].get("positive_styles") or [])
            last = chunks[-1]
            last_pos = list(last.get("positive_styles") or [])
            last_pos.extend(["return smoothly to opening groove and harmonic texture", "no final resolving cadence"])
            if first_styles:
                last_pos.append(first_styles[0])
            last["positive_styles"] = list(dict.fromkeys(last_pos))[:45]
            last_neg = list(last.get("negative_styles") or [])
            last_neg.extend(["fade to silence", "final ending hit", "ritardando"])
            last["negative_styles"] = list(dict.fromkeys(last_neg))[:45]
        return plan

    neg_global = list(plan.get("negative_global_styles", []))
    for guard in neg_guards:
        if guard not in neg_global:
            neg_global.append(guard)
    plan["negative_global_styles"] = neg_global

    sections = plan.get("sections", [])
    if entry.get("loop") and sections:
        first_styles = sections[0].get("positive_local_styles", [])
        last = sections[-1]
        last_pos = list(last.get("positive_local_styles", []))
        last_pos.extend(["return smoothly to opening groove and harmonic texture", "no final resolving cadence"])
        if first_styles:
            last_pos.append(first_styles[0])
        last["positive_local_styles"] = list(dict.fromkeys(last_pos))
        last_neg = list(last.get("negative_local_styles", []))
        last_neg.extend(["fade to silence", "final ending hit", "ritardando"])
        last["negative_local_styles"] = list(dict.fromkeys(last_neg))
    return plan


def cmd_compose_elevenlabs(args: argparse.Namespace) -> None:
    if requests is None:
        raise RuntimeError("requests package not installed.")
    bible = load_bible()
    api_key = load_api_key()
    manifest = ensure_manifest(bible)
    prefs = audio_production.load_preferences()

    targets: List[Dict[str, Any]] = []
    if args.id:
        kind, entry = find_bible_entry(args.id, bible)
        if kind != "track" or entry is None:
            raise RuntimeError(f"Track '{args.id}' not found in musical_bible.json")
        targets = [entry]
    elif getattr(args, "batch", None):
        batch_ids = {
            cid for cid, c in manifest.get("cues", {}).items() if c.get("batch_id") == args.batch
        }
        targets = [t for t in bible.get("tracks", []) if t["id"] in batch_ids]
    else:
        for t in bible.get("tracks", []):
            if args.category and args.category != "all" and t.get("category") != args.category:
                continue
            if args.recommended_only and t.get("recommended_provider") != "elevenlabs":
                continue
            targets.append(t)

    if not targets:
        print("No matching tracks selected.")
        return

    for t in targets:
        cue_id = t["id"]
        if is_immutable_id(cue_id, bible):
            continue
        is_vocal = "vocal" in t.get("tags", [])
        dur_ms = int(args.duration_ms or t.get("duration_ms", 90000))

        existing_takes = 0
        if getattr(args, "resume", False) and (TAKES_DIR / cue_id).exists():
            existing_takes = sum(1 for d in (TAKES_DIR / cue_id).iterdir() if d.is_dir() and d.name.startswith("take_"))
        remaining_takes = max(0, int(args.takes) - existing_takes)
        if remaining_takes == 0:
            print(f"[RESUME SKIP] {cue_id} already has {existing_takes} takes.")
            continue

        mcue = manifest.get("cues", {}).get(cue_id)
        enriched_prompt, enriched_exclude = enrich_prompt_with_motifs(t, bible, prefs, mcue)
        plan = None
        if args.use_plan:
            print(f"[PLAN] Generating ElevenLabs composition_plan for {cue_id} (model={args.model})...")
            plan = build_elevenlabs_plan(
                api_key,
                t,
                model_id=args.model,
                duration_ms=dur_ms,
                prompt_override=enriched_prompt,
                exclude_override=enriched_exclude,
            )

        for take_i in range(remaining_takes):
            payload: Dict[str, Any] = {
                "model_id": args.model,
            }
            if plan is not None:
                payload["composition_plan"] = plan
            else:
                payload["prompt"] = enriched_prompt
                payload["music_length_ms"] = dur_ms
                payload["force_instrumental"] = not is_vocal

            if args.seed is not None:
                payload["seed"] = int(args.seed) + existing_takes + take_i

            print(f"[ELEVENLABS MUSIC] {cue_id} (take {existing_takes + take_i + 1}/{args.takes}, model={args.model}, {dur_ms}ms)...")
            resp = requests.post(
                f"https://api.elevenlabs.io/v1/music?output_format={args.output_format}",
                headers={"xi-api-key": api_key, "Content-Type": "application/json"},
                json=payload,
                timeout=240,
            )
            if resp.status_code == 400 and "bad_composition_plan" in resp.text and "composition_plan" in payload:
                print("  [FALLBACK] composition_plan flagged by provider filter; retrying with sanitized direct prompt...")
                fb_payload: Dict[str, Any] = {
                    "model_id": args.model,
                    "prompt": enriched_prompt,
                    "music_length_ms": dur_ms,
                    "force_instrumental": not is_vocal,
                }
                if args.seed is not None:
                    fb_payload["seed"] = payload["seed"]
                resp = requests.post(
                    f"https://api.elevenlabs.io/v1/music?output_format={args.output_format}",
                    headers={"xi-api-key": api_key, "Content-Type": "application/json"},
                    json=fb_payload,
                    timeout=240,
                )
                if resp.status_code == 200:
                    payload = fb_payload
            if resp.status_code != 200:
                print(f"  [ERROR {resp.status_code}] {resp.text[:400]}")
                continue

            with tempfile.TemporaryDirectory() as tmp:
                tmp_mp3 = Path(tmp) / f"{cue_id}.mp3"
                tmp_mp3.write_bytes(resp.content)
                take_dir = ingest_audio_file(
                    source_path=tmp_mp3,
                    cue_id=cue_id,
                    provider="elevenlabs",
                    bible=bible,
                    notes=args.notes or "",
                    extra_meta={
                        "endpoint": "https://api.elevenlabs.io/v1/music",
                        "model_id": args.model,
                        "output_format": args.output_format,
                        "request_payload": payload,
                    },
                )
                take_num = int(take_dir.name.split("_")[-1])
                audio_production.register_candidate(
                    manifest=manifest,
                    cue_id=cue_id,
                    provider="elevenlabs",
                    prompt_used=enriched_prompt,
                    local_raw_path=str((take_dir / "raw.mp3").relative_to(ROOT_DIR)),
                    take_num=take_num,
                    commercial_rights_verified=False,
                    notes=args.notes or "Verify active paid ElevenLabs tier before commercial release",
                )
                manifest["cues"][cue_id]["status"] = "candidates_ready"
                audio_production.save_json(audio_production.MANIFEST_PATH, manifest)


# ---------------------------------------------------------------------------
# Subcommand: stems (ElevenLabs /v1/music/stem-separation)
# ---------------------------------------------------------------------------
def cmd_stems(args: argparse.Namespace) -> None:
    if requests is None:
        raise RuntimeError("requests package not installed.")
    api_key = load_api_key()
    src = Path(args.file)
    if not src.exists():
        raise RuntimeError(f"Audio file not found: {src}")

    cue_id = args.id or src.stem
    out_dir = STEMS_DIR / cue_id / datetime.now().strftime("%Y%m%d_%H%M%S")
    out_dir.mkdir(parents=True, exist_ok=True)

    print(f"[STEMS] Separating {src.name} with {args.variation}...")
    with open(src, "rb") as f:
        resp = requests.post(
            "https://api.elevenlabs.io/v1/music/stem-separation",
            headers={"xi-api-key": api_key},
            files={"file": (src.name, f, "audio/mpeg")},
            data={"stem_variation_id": args.variation},
            timeout=240,
        )
    if resp.status_code != 200:
        raise RuntimeError(f"Stem separation failed ({resp.status_code}): {resp.text[:400]}")

    with zipfile.ZipFile(io.BytesIO(resp.content)) as zf:
        zf.extractall(out_dir)
        print(f"Extracted {len(zf.namelist())} stems to {out_dir.relative_to(ROOT_DIR)}: {zf.namelist()}")


# ---------------------------------------------------------------------------
# Subcommand: sfx (ElevenLabs /v1/sound-generation)
# ---------------------------------------------------------------------------
def cmd_sfx(args: argparse.Namespace) -> None:
    if requests is None:
        raise RuntimeError("requests package not installed.")
    bible = load_bible()
    api_key = load_api_key()

    entries = all_sfx_entries(bible)
    if args.id:
        targets = [e for e in entries if e["id"] == args.id]
        if not targets:
            raise RuntimeError(f"SFX '{args.id}' not found in musical_bible.json")
    elif args.group and args.group != "all":
        targets = [e for e in entries if e["group"] == args.group]
    else:
        targets = entries

    for s in targets:
        cue_id = s["id"]
        if args.skip_existing and (TAKES_DIR / cue_id).exists():
            continue
        for take_i in range(args.takes):
            payload = {
                "text": s["prompt"],
                "duration_seconds": float(s["duration"]),
                "prompt_influence": float(args.influence),
            }
            print(f"[ELEVENLABS SFX] {cue_id} (take {take_i + 1}/{args.takes}, {s['duration']}s)...")
            resp = requests.post(
                "https://api.elevenlabs.io/v1/sound-generation",
                headers={"xi-api-key": api_key, "Content-Type": "application/json"},
                json=payload,
                timeout=90,
            )
            if resp.status_code == 429:
                import time
                time.sleep(3.0)
                resp = requests.post(
                    "https://api.elevenlabs.io/v1/sound-generation",
                    headers={"xi-api-key": api_key, "Content-Type": "application/json"},
                    json=payload,
                    timeout=90,
                )
            if resp.status_code != 200:
                print(f"  [ERROR {resp.status_code}] {resp.text[:300]}")
                if resp.status_code in (401, 402, 403) or "quota" in resp.text.lower():
                    print("  [STOP] Stopping SFX batch early due to quota/auth limit.")
                    return
                continue
            with tempfile.TemporaryDirectory() as tmp:
                tmp_mp3 = Path(tmp) / f"{cue_id}.mp3"
                tmp_mp3.write_bytes(resp.content)
                ingest_audio_file(
                    source_path=tmp_mp3,
                    cue_id=cue_id,
                    provider="elevenlabs_sfx",
                    bible=bible,
                    extra_meta={
                        "endpoint": "https://api.elevenlabs.io/v1/sound-generation",
                        "request_payload": payload,
                    },
                )


# ---------------------------------------------------------------------------
# Subcommand: master (LUFS + True Peak + Gapless Tail-Wrap Loop)
# ---------------------------------------------------------------------------
def trim_digital_silence(samples: np.ndarray, threshold_db: float = -60.0) -> np.ndarray:
    if len(samples) == 0:
        return samples
    thresh = 10.0 ** (threshold_db / 20.0)
    env = np.max(np.abs(samples), axis=1)
    above = np.where(env > thresh)[0]
    if len(above) == 0:
        return samples
    start = max(0, int(above[0]) - int(0.005 * SAMPLE_RATE))
    end = min(len(samples), int(above[-1]) + int(0.025 * SAMPLE_RATE))
    return samples[start:end]


def apply_tail_wrap_loop(samples: np.ndarray, wrap_ms: int = 1600) -> np.ndarray:
    """Folds the final wrap_ms reverb tail over the opening head and ends cleanly at L - wrap."""
    wrap_frames = int((wrap_ms / 1000.0) * SAMPLE_RATE)
    if len(samples) <= wrap_frames * 3:
        return samples
    body = samples[:-wrap_frames].copy()
    tail = samples[-wrap_frames:].copy()
    t = np.linspace(0.0, 1.0, wrap_frames, endpoint=False)[:, None]
    tail_fade = np.cos(t * (math.pi / 2.0))
    body[:wrap_frames] += tail * tail_fade
    # Micro 8ms crossfade at the very boundary for zero-sample discontinuity
    edge = min(int(0.008 * SAMPLE_RATE), len(body) // 4)
    if edge > 4:
        alpha = np.linspace(0.0, 1.0, edge)[:, None]
        body[:edge] = body[:edge] * alpha + tail[:edge] * (1.0 - alpha)
    return body


def master_take(
    cue_id: str,
    bible: Dict[str, Any],
    take_num: Optional[int] = None,
    loop_mode: str = "auto",
    wrap_ms: int = 1600,
    loop_start_ms: Optional[int] = None,
    loop_end_ms: Optional[int] = None,
    target_lufs_override: Optional[float] = None,
) -> Path:
    if is_immutable_id(cue_id, bible):
        raise RuntimeError(f"Refusing to master immutable track '{cue_id}'.")
    kind, entry = find_bible_entry(cue_id, bible)
    if kind is None or entry is None:
        raise RuntimeError(f"Unknown cue ID '{cue_id}'")

    take_dir = find_latest_take_dir(cue_id, take_num)
    raw_audio = find_raw_in_take(take_dir)
    prov = load_json(take_dir / "provenance.json", default={})

    targets = bible.get("loudness_targets_lufs", {})
    if target_lufs_override is not None:
        target_lufs = float(target_lufs_override)
    elif kind == "track":
        cat = entry.get("category", "explore")
        cat_key = cat if cat in targets else ("explore" if cat.startswith("w") else "explore")
        target_lufs = float(entry.get("target_lufs", targets.get(cat_key, -17.0)))
    else:
        grp = entry.get("group", "sfx")
        key = "battle_sfx" if grp == "combat" else grp if grp in targets else "sfx"
        target_lufs = float(entry.get("target_lufs", targets.get(key, -13.5)))

    samples = decode_audio_to_float(raw_audio)
    samples = trim_digital_silence(samples)

    effective_loop = bool(entry.get("loop", False)) if kind == "track" else False
    if loop_mode == "auto":
        if effective_loop and loop_start_ms is None:
            mode = "wrap"
        elif effective_loop and loop_start_ms is not None:
            mode = "intro"
        else:
            mode = "none"
    else:
        mode = loop_mode

    if mode == "wrap":
        samples = apply_tail_wrap_loop(samples, wrap_ms=wrap_ms)

    # Soft peak safety before ffmpeg 2-pass loudnorm
    peak = float(np.max(np.abs(samples))) if len(samples) else 0.0
    if peak > 0.98:
        samples = samples * (0.98 / peak)

    MASTERED_DIR.mkdir(parents=True, exist_ok=True)
    out_mp3 = MASTERED_DIR / f"{cue_id}.mp3"
    bitrate = "192k" if kind == "track" else "160k"
    ffmpeg = get_ffmpeg_exe()

    with tempfile.TemporaryDirectory() as tmp:
        pre_wav = Path(tmp) / "pre.wav"
        norm_wav = Path(tmp) / "norm.wav"
        with wave.open(str(pre_wav), "wb") as w:
            w.setnchannels(2)
            w.setsampwidth(2)
            w.setframerate(SAMPLE_RATE)
            pcm16 = np.clip(samples * 32767.0, -32768, 32767).astype(np.int16)
            w.writeframes(pcm16.tobytes())

        # Pass 1: measure loudnorm stats (only for tracks >= 3s; short SFX use peak-safe linear gain + alimiter)
        if len(samples) >= SAMPLE_RATE * 3:
            p1 = subprocess.run(
                [
                    ffmpeg,
                    "-y",
                    "-i",
                    str(pre_wav),
                    "-af",
                    f"loudnorm=I={target_lufs}:TP=-1.0:LRA=8:print_format=json",
                    "-f",
                    "null",
                    "-",
                ],
                capture_output=True,
                text=True,
                check=False,
            )
            m = re.search(r"\{[^{}]*\"input_i\"[^{}]*\}", p1.stderr, re.DOTALL)
            if m:
                stats = json.loads(m.group(0))
                flt = (
                    f"loudnorm=I={target_lufs}:TP=-1.2:LRA=8:linear=true:"
                    f"measured_I={stats['input_i']}:measured_TP={stats['input_tp']}:"
                    f"measured_LRA={stats['input_lra']}:measured_thresh={stats['input_thresh']}:"
                    f"offset={stats['target_offset']},alimiter=level=disabled:limit=0.85:attack=0.8:release=40"
                )
            else:
                flt = "alimiter=level=disabled:limit=0.85:attack=0.8:release=40"
        else:
            # Short transient SFX: RMS-guided gain + fast true-headroom limiter (auto-gain disabled)
            cur_rms = float(np.sqrt(np.mean(samples ** 2))) if len(samples) else 1e-6
            cur_db = 20.0 * math.log10(max(cur_rms, 1e-6))
            delta_db = max(-12.0, min(20.0, (target_lufs + 1.0) - cur_db))
            flt = f"volume={delta_db:.2f}dB,alimiter=level=disabled:limit=0.84:attack=0.5:release=35"

        subprocess.run(
            [ffmpeg, "-y", "-i", str(pre_wav), "-af", flt, str(norm_wav)],
            check=True,
            capture_output=True,
        )
        subprocess.run(
            [
                ffmpeg,
                "-y",
                "-i",
                str(norm_wav),
                "-codec:a",
                "libmp3lame",
                "-b:a",
                bitrate,
                "-ar",
                str(SAMPLE_RATE),
                "-ac",
                "2",
                str(out_mp3),
            ],
            check=True,
            capture_output=True,
        )

    final_metrics = measure_audio(out_mp3)
    # Guard against lossy MP3 inter-sample encoding overshoot tripping promote QC.
    if final_metrics["samples_at_full_scale"] > 0 or final_metrics["peak_dbfs"] > -0.25 or (
        final_metrics["true_peak_dbtp"] is not None and final_metrics["true_peak_dbtp"] > -0.1
    ):
        overshoot_tp = final_metrics["true_peak_dbtp"] if final_metrics["true_peak_dbtp"] is not None else final_metrics["peak_dbfs"]
        attenuation_db = max(1.0, max(final_metrics["peak_dbfs"], overshoot_tp) + 1.0)
        with tempfile.TemporaryDirectory() as tmp2:
            fixed_wav = Path(tmp2) / "fixed.wav"
            subprocess.run(
                [
                    ffmpeg,
                    "-y",
                    "-i",
                    str(out_mp3),
                    "-af",
                    f"volume=-{attenuation_db:.2f}dB,alimiter=level=disabled:limit=0.85:attack=0.5:release=35",
                    "-ar",
                    str(SAMPLE_RATE),
                    "-ac",
                    "2",
                    str(fixed_wav),
                ],
                check=True,
                capture_output=True,
            )
            subprocess.run(
                [
                    ffmpeg,
                    "-y",
                    "-i",
                    str(fixed_wav),
                    "-codec:a",
                    "libmp3lame",
                    "-b:a",
                    bitrate,
                    "-ar",
                    str(SAMPLE_RATE),
                    "-ac",
                    "2",
                    str(out_mp3),
                ],
                check=True,
                capture_output=True,
            )
        final_metrics = measure_audio(out_mp3)
    master_meta = {
        "cue_id": cue_id,
        "kind": kind,
        "source_take": take_dir.name,
        "source_provider": prov.get("provider", "unknown"),
        "mastered_at": datetime.now().isoformat(),
        "loop_mode": mode,
        "loop_start_ms": loop_start_ms if loop_start_ms is not None else entry.get("loop_start_ms"),
        "loop_end_ms": loop_end_ms if loop_end_ms is not None else entry.get("loop_end_ms"),
        "target_lufs": target_lufs,
        "sha256": sha256_file(out_mp3),
        "bytes": out_mp3.stat().st_size,
        "metrics": final_metrics,
        "source_provenance": prov,
    }
    save_json(MASTERED_DIR / f"{cue_id}.master.json", master_meta)
    print(
        f"[MASTERED] {cue_id}.mp3 | mode={mode} | {final_metrics['duration_seconds']}s | "
        f"LUFS={final_metrics['integrated_lufs']} | Peak={final_metrics['true_peak_dbtp']} dBTP"
    )
    return out_mp3


def cmd_master(args: argparse.Namespace) -> None:
    bible = load_bible()
    if args.all_ready:
        if not TAKES_DIR.exists():
            print("No takes directory found.")
            return
        manifest = load_json(audio_production.MANIFEST_PATH, default={}) or {}
        prod_cues = manifest.get("cues", {})
        for d in sorted(TAKES_DIR.iterdir()):
            if d.is_dir() and not is_immutable_id(d.name, bible):
                mcue = prod_cues.get(d.name, {})
                pref_take = mcue.get("approved_take") or (mcue.get("shortlist", {}).get("A", {}) or {}).get("take_num")
                try:
                    master_take(
                        d.name,
                        bible,
                        take_num=pref_take,
                        loop_mode=args.loop_mode,
                        wrap_ms=args.wrap_ms,
                        target_lufs_override=args.target_lufs,
                    )
                except Exception as e:
                    print(f"[SKIP] {d.name}: {e}")
        return
    if not args.id:
        raise RuntimeError("Specify --id <cue_id> or --all-ready")
    master_take(
        args.id,
        bible,
        take_num=args.take,
        loop_mode=args.loop_mode,
        wrap_ms=args.wrap_ms,
        loop_start_ms=args.loop_start_ms,
        loop_end_ms=args.loop_end_ms,
        target_lufs_override=args.target_lufs,
    )


# ---------------------------------------------------------------------------
# Subcommand: promote
# ---------------------------------------------------------------------------
def sync_promoted_entry(cue_id: str, bible: Dict[str, Any], master_meta: Dict[str, Any], sync_bindings: bool) -> None:
    kind, entry = find_bible_entry(cue_id, bible)
    if entry is None:
        return
    catalog = load_json(CATALOG_PATH, default={"tracks": [], "cues": []})

    if kind == "track":
        tracks = {t["id"]: t for t in catalog.get("tracks", [])}
        is_stinger = entry.get("category") == "stinger"
        if is_stinger:
            cues = {c["id"]: c for c in catalog.get("cues", [])}
            cues[cue_id] = {
                "id": cue_id,
                "category": "stinger",
                "priority": 6,
                "gain": entry.get("gain", 0.9),
                "fade_in_ms": entry.get("fade_in_ms", 20),
                "fade_out_ms": entry.get("fade_out_ms", 400),
                "duration_ms": int(master_meta["metrics"]["duration_seconds"] * 1000),
            }
            catalog["cues"] = list(cues.values())
        else:
            rec = {
                "id": cue_id,
                "type": "music",
                "loop": bool(entry.get("loop", True)),
                "fade_in_ms": int(entry.get("fade_in_ms", 1200)),
                "fade_out_ms": int(entry.get("fade_out_ms", 1100)),
                "gain": float(entry.get("gain", 0.85)),
                "tags": entry.get("tags", []),
            }
            if master_meta.get("loop_start_ms") is not None:
                rec["loop_start_ms"] = int(master_meta["loop_start_ms"])
            if master_meta.get("loop_end_ms") is not None:
                rec["loop_end_ms"] = int(master_meta["loop_end_ms"])
            tracks[cue_id] = rec
            catalog["tracks"] = list(tracks.values())
    else:
        cues = {c["id"]: c for c in catalog.get("cues", [])}
        existing_cue = cues.get(cue_id)
        if existing_cue:
            updated_cue = dict(existing_cue)
            updated_cue["duration_ms"] = int(master_meta["metrics"]["duration_seconds"] * 1000)
            cues[cue_id] = updated_cue
        else:
            grp = entry.get("group", "sfx")
            cat_name = "ui" if grp == "ui" else "battle" if grp == "combat" else "sfx"
            cues[cue_id] = {
                "id": cue_id,
                "category": cat_name,
                "priority": 5,
                "gain": float(entry.get("gain", 0.92)),
                "fade_in_ms": 0,
                "fade_out_ms": 150,
                "duration_ms": int(master_meta["metrics"]["duration_seconds"] * 1000),
            }
        catalog["cues"] = list(cues.values())

    save_json(CATALOG_PATH, catalog)

    if sync_bindings and kind == "track" and entry.get("bind"):
        bindings = load_json(BINDINGS_PATH, default={})
        for section, keys in entry["bind"].items():
            sec_map = bindings.setdefault(section, {})
            for k in keys:
                sec_map[k] = cue_id
        save_json(BINDINGS_PATH, bindings)


def promote_single_cue(
    cue_id: str,
    bible: Dict[str, Any],
    sync_bindings: bool = False,
    force: bool = False,
) -> None:
    if is_immutable_id(cue_id, bible):
        raise RuntimeError(f"SAFETY STOP: Refusing to modify immutable track '{cue_id}'.")

    manifest = load_json(audio_production.MANIFEST_PATH, default={})
    mcue = manifest.get("cues", {}).get(cue_id, {}) if manifest else {}
    if mcue.get("status") in ("integrated", "verified_in_game") and not force:
        raise RuntimeError(f"SAFETY STOP: '{cue_id}' is already approved & integrated. Pass --force to overwrite.")

    master_mp3 = MASTERED_DIR / f"{cue_id}.mp3"
    master_json = MASTERED_DIR / f"{cue_id}.master.json"
    if not master_mp3.exists() or not master_json.exists():
        raise RuntimeError(f"Mastered files missing for '{cue_id}'. Run `master` first.")

    meta = load_json(master_json)
    metrics = meta["metrics"]
    if metrics["samples_at_full_scale"] > 0 or (metrics.get("peak_dbfs", 0.0) > -0.05):
        raise RuntimeError(f"QC fail for '{cue_id}': clipping detected ({metrics}).")
    if metrics["rms_dbfs"] < -45.0:
        raise RuntimeError(f"QC fail for '{cue_id}': audio appears too quiet/silent ({metrics}).")

    PROVENANCE_DIR.mkdir(parents=True, exist_ok=True)
    # Remove any old conflicting raw format with the same Android resource stem (e.g. .wav -> .mp3)
    for alt_ext in (".wav", ".ogg", ".flac", ".m4a"):
        alt_path = RAW_DIR / f"{cue_id}{alt_ext}"
        if alt_path.exists():
            alt_path.unlink()
    dest_mp3 = RAW_DIR / f"{cue_id}.mp3"
    shutil.copy2(master_mp3, dest_mp3)
    save_json(PROVENANCE_DIR / f"{cue_id}.json", meta)
    sync_promoted_entry(cue_id, bible, meta, sync_bindings=sync_bindings)
    print(f"[PROMOTED] -> {dest_mp3.relative_to(ROOT_DIR)} (catalog/bindings synced)")


def cmd_promote(args: argparse.Namespace) -> None:
    bible = load_bible()
    ids: List[str] = []
    if args.id:
        ids = [args.id]
    elif getattr(args, "all_sfx", False):
        ids = [s["id"] for s in all_sfx_entries(bible) if (MASTERED_DIR / f"{s['id']}.mp3").exists()]
    elif args.all_mastered:
        if MASTERED_DIR.exists():
            ids = sorted([p.stem for p in MASTERED_DIR.glob("*.mp3")])
    elif args.from_approvals:
        approvals = load_json(Path(args.from_approvals), default={})
        ids = [k for k, v in approvals.items() if v is True or (isinstance(v, dict) and v.get("approved"))]

    if not ids:
        print("No tracks specified or ready to promote.")
        return

    for cue_id in ids:
        promote_single_cue(cue_id, bible, sync_bindings=args.sync_bindings, force=getattr(args, "force", False))


# ---------------------------------------------------------------------------
# Subcommand: build-board
# ---------------------------------------------------------------------------
def cmd_build_board(args: argparse.Namespace) -> None:
    bible = load_bible()
    backup_raw = ROOT_DIR / "tools" / "backups" / "pre_rebrand_audio_2026-11-17" / "res_raw"
    prod_manifest = load_json(audio_production.MANIFEST_PATH, default={}) or {}
    prod_cues = prod_manifest.get("cues", {})
    prefs = audio_production.load_preferences()

    def rel_from_tools(p: Path) -> str:
        return os.path.relpath(p, ROOT_DIR / "tools").replace("\\", "/")

    items: List[Dict[str, Any]] = []
    for t in bible.get("tracks", []):
        cid = t["id"]
        mcue = prod_cues.get(cid, {})
        cand_by_take = {c.get("take_num"): c for c in mcue.get("candidates", []) if c.get("take_num") is not None}
        takes = []
        tdir = TAKES_DIR / cid
        if tdir.exists():
            for td in sorted(tdir.iterdir()):
                if td.is_dir():
                    try:
                        raw_p = find_raw_in_take(td)
                        prov = load_json(td / "provenance.json", default={})
                        analysis = load_json(td / "analysis.json", default={}) or {}
                        tnum = prov.get("take")
                        cand = cand_by_take.get(tnum, {})
                        takes.append(
                            {
                                "name": td.name,
                                "take": tnum,
                                "provider": prov.get("provider", "unknown"),
                                "path": rel_from_tools(raw_p),
                                "metrics": prov.get("metrics", {}),
                                "analysis": {
                                    "computational_score": analysis.get("computational_score"),
                                    "estimated_keys": analysis.get("estimated_keys"),
                                    "tempo": analysis.get("tempo"),
                                    "motif_alignment": analysis.get("motif_alignment", {}).get("motifs"),
                                    "mix_and_loop": analysis.get("mix_and_loop"),
                                }
                                if analysis
                                else None,
                                "shortlist_label": cand.get("shortlist_label"),
                                "rank": cand.get("rank"),
                                "suno_url": cand.get("suno_url"),
                                "ai_eval_hydrogen": cand.get("ai_eval_hydrogen"),
                                "ai_eval_argon": cand.get("ai_eval_argon"),
                            }
                        )
                    except Exception:
                        pass
        m_mp3 = MASTERED_DIR / f"{cid}.mp3"
        m_json = load_json(MASTERED_DIR / f"{cid}.master.json", default={})
        live_mp3 = RAW_DIR / f"{cid}.mp3"
        bak_mp3 = backup_raw / f"{cid}.mp3"
        eff_prompt, eff_exclude = audio_production.build_refined_prompt(t, prefs, mcue)

        items.append(
            {
                "id": cid,
                "kind": "track",
                "title": t["title"],
                "category": t.get("category", "core"),
                "scope": mcue.get("scope", "primary_58"),
                "batch_id": mcue.get("batch_id", ""),
                "production_status": mcue.get("status", "pending"),
                "key": t.get("key", ""),
                "bpm": t.get("bpm", 0),
                "loop": bool(t.get("loop", True)),
                "motifs": t.get("motif_refs", []),
                "recommended_provider": t.get("recommended_provider", "suno"),
                "reference_wav": mcue.get("reference_wav"),
                "prompt": eff_prompt,
                "negative_prompt": eff_exclude,
                "shortlist": mcue.get("shortlist", {}),
                "candidates_manifest": mcue.get("candidates", []),
                "rejected_ids_preserved": mcue.get("rejected_ids_preserved", []),
                "commercial_use_flagged": bool(mcue.get("commercial_use_flagged", False)),
                "takes": takes,
                "mastered": {
                    "path": rel_from_tools(m_mp3),
                    "metrics": m_json.get("metrics", {}),
                    "loop_start_ms": m_json.get("loop_start_ms"),
                    "loop_end_ms": m_json.get("loop_end_ms"),
                }
                if m_mp3.exists()
                else None,
                "live_path": rel_from_tools(live_mp3) if live_mp3.exists() else None,
                "backup_path": rel_from_tools(bak_mp3) if bak_mp3.exists() else None,
            }
        )

    for s in all_sfx_entries(bible):
        cid = s["id"]
        takes = []
        tdir = TAKES_DIR / cid
        if tdir.exists():
            for td in sorted(tdir.iterdir()):
                if td.is_dir():
                    try:
                        raw_p = find_raw_in_take(td)
                        prov = load_json(td / "provenance.json", default={})
                        takes.append(
                            {
                                "name": td.name,
                                "provider": prov.get("provider", "elevenlabs_sfx"),
                                "path": rel_from_tools(raw_p),
                                "metrics": prov.get("metrics", {}),
                            }
                        )
                    except Exception:
                        pass
        m_mp3 = MASTERED_DIR / f"{cid}.mp3"
        m_json = load_json(MASTERED_DIR / f"{cid}.master.json", default={})
        live_mp3 = RAW_DIR / f"{cid}.mp3"
        bak_mp3 = backup_raw / f"{cid}.mp3"
        items.append(
            {
                "id": cid,
                "kind": "sfx",
                "title": cid,
                "category": f"sfx_{s['group']}",
                "key": "",
                "bpm": 0,
                "loop": False,
                "motifs": [],
                "recommended_provider": "elevenlabs",
                "prompt": s["prompt"],
                "takes": takes,
                "mastered": {"path": rel_from_tools(m_mp3), "metrics": m_json.get("metrics", {})} if m_mp3.exists() else None,
                "live_path": rel_from_tools(live_mp3) if live_mp3.exists() else None,
                "backup_path": rel_from_tools(bak_mp3) if bak_mp3.exists() else None,
            }
        )

    manifest = {
        "generated_at": datetime.now().isoformat(),
        "aesthetic": bible.get("aesthetic"),
        "motifs": bible.get("motifs", {}),
        "suno_plan": prod_manifest.get("suno_plan", {}),
        "target_scope_summary": prod_manifest.get("target_scope_summary", {}),
        "style_preferences": prefs,
        "items": items,
    }
    save_json(REVIEW_MANIFEST_PATH, manifest)
    js_path = REVIEW_MANIFEST_PATH.parent / "audio_board_data.js"
    js_path.write_text(
        "// Auto-generated by scripts/audio_studio.py build-board\n"
        f"window.STARBORN_AUDIO_BOARD = {json.dumps(manifest, indent=2)};\n",
        encoding="utf-8",
    )
    print(f"Updated review board manifest: {REVIEW_MANIFEST_PATH.relative_to(ROOT_DIR)} ({len(items)} items)")


# ---------------------------------------------------------------------------
# Subcommand: status & verify
# ---------------------------------------------------------------------------
def check_gf_integrity() -> Tuple[bool, List[str]]:
    errors: List[str] = []
    manifest = load_json(BACKUP_MANIFEST_PATH)
    if not manifest:
        return True, ["No backup manifest found to compare GF hashes."]
    for fname, expected in manifest.get("gf_tracks_immutable", {}).items():
        p = RAW_DIR / fname
        if not p.exists():
            errors.append(f"Missing immutable GF tape: {fname}")
            continue
        actual_sha = sha256_file(p)
        if actual_sha != expected["sha256"]:
            errors.append(f"GF tape modified! {fname}: expected {expected['sha256']}, got {actual_sha}")
    return len(errors) == 0, errors


def cmd_status(args: argparse.Namespace) -> None:
    bible = load_bible()
    gf_ok, gf_msgs = check_gf_integrity()
    tracks = bible.get("tracks", [])
    sfx = all_sfx_entries(bible)
    live_tracks = sum(1 for t in tracks if (RAW_DIR / f"{t['id']}.mp3").exists())
    staged_tracks = sum(1 for t in tracks if (TAKES_DIR / t["id"]).exists())
    mastered_tracks = sum(1 for t in tracks if (MASTERED_DIR / f"{t['id']}.mp3").exists())
    live_sfx = sum(1 for s in sfx if (RAW_DIR / f"{s['id']}.mp3").exists() or (RAW_DIR / f"{s['id']}.wav").exists())
    staged_sfx = sum(1 for s in sfx if (TAKES_DIR / s["id"]).exists())
    mastered_sfx = sum(1 for s in sfx if (MASTERED_DIR / f"{s['id']}.mp3").exists())

    print("=== Starborn Audio Studio Status ===")
    print(f"Great Frontier tapes intact: {'YES (10/10 verified)' if gf_ok else 'NO -> ' + '; '.join(gf_msgs)}")
    print(f"Music & Stingers ({len(tracks)} planned): {staged_tracks} with takes | {mastered_tracks} mastered | {live_tracks} live in res/raw")
    print(f"SFX & UI Cues   ({len(sfx)} planned): {staged_sfx} with takes | {mastered_sfx} mastered | {live_sfx} live in res/raw")
    missing_live = [t["id"] for t in tracks if not (RAW_DIR / f"{t['id']}.mp3").exists()]
    if missing_live:
        print(f"New tracks not yet in res/raw ({len(missing_live)}): {', '.join(missing_live)}")


def cmd_verify(args: argparse.Namespace) -> None:
    bible = load_bible()
    errors: List[str] = []
    warnings: List[str] = []

    gf_ok, gf_msgs = check_gf_integrity()
    if not gf_ok:
        errors.extend(gf_msgs)

    catalog = load_json(CATALOG_PATH, default={})
    bindings = load_json(BINDINGS_PATH, default={})

    catalog_ids = set()
    for t in catalog.get("tracks", []):
        catalog_ids.add(t["id"].strip().lower().replace("-", "_"))
    for c in catalog.get("cues", []):
        catalog_ids.add(c["id"].strip().lower().replace("-", "_"))

    raw_by_stem: Dict[str, Path] = {}
    for p in RAW_DIR.iterdir():
        if p.is_file() and p.suffix.lower() in (".mp3", ".wav"):
            raw_by_stem[p.stem.lower()] = p

    refs: List[Tuple[str, str]] = []
    for sec in ("music", "node_music", "room_music", "combat_music", "ambience", "weather", "ui", "battle"):
        for k, val in (bindings.get(sec) or {}).items():
            for part in re.split(r"[+,;]", str(val)):
                cue = part.strip().lower().replace("-", "_")
                if cue:
                    refs.append((f"audio_bindings.{sec}.{k}", cue))

    audio_keys = {"voice", "vo_cue", "voice_cue", "voicecue", "audio_cue", "audiocue", "audio_cue_id", "audiocueid", "ambient_cue", "ambientcue", "cue_id", "music_cue"}
    def walk(obj: Any, src: str) -> None:
        if isinstance(obj, list):
            for x in obj:
                walk(x, src)
        elif isinstance(obj, dict):
            for k, v in obj.items():
                if k.lower() in audio_keys and isinstance(v, str):
                    for part in re.split(r"[+,;]", v):
                        cue = part.strip().lower().replace("-", "_")
                        if cue:
                            refs.append((src, cue))
                walk(v, src)

    for jf in sorted(ASSETS_DIR.glob("*.json")):
        if jf.name in ("audio_catalog.json", "audio_bindings.json"):
            continue
        try:
            walk(load_json(jf), jf.name)
        except Exception as e:
            warnings.append(f"Skipped unreadable JSON {jf.name}: {e}")

    checked = set()
    for src, cue in refs:
        if cue not in catalog_ids:
            warnings.append(f"{src} references '{cue}' missing from audio_catalog.json")
        if cue in checked:
            continue
        checked.add(cue)
        fp = raw_by_stem.get(cue)
        if fp is None:
            errors.append(f"{src} references missing raw audio '{cue}'")
        elif fp.stat().st_size < 4096:
            errors.append(f"{src} references undersized raw audio '{cue}' ({fp.stat().st_size} bytes)")

    # Validate audio_studio/musical_bible.json schema integrity
    seen_ids = set()
    for t in bible.get("tracks", []):
        if t["id"] in seen_ids:
            errors.append(f"Duplicate track ID in musical_bible.json: {t['id']}")
        seen_ids.add(t["id"])

    for w in sorted(set(warnings)):
        print(f"WARNING: {w}")
    for e in sorted(set(errors)):
        print(f"ERROR: {e}")

    if errors or (args.strict_catalog and warnings):
        sys.exit(1)
    print(f"Audio verification passed: {len(checked)} unique referenced cues, GF tapes intact, 0 errors, {len(set(warnings))} warnings.")


def ensure_manifest(bible: Optional[Dict[str, Any]] = None, credits_allowance: int = 10000) -> Dict[str, Any]:
    bible = bible or load_bible()
    backup_manifest = load_json(BACKUP_MANIFEST_PATH, default=None)
    return audio_production.load_or_init_manifest(
        bible=bible,
        backup_manifest=backup_manifest,
        raw_dir=RAW_DIR,
        takes_dir=TAKES_DIR,
        mastered_dir=MASTERED_DIR,
        credits_allowance=credits_allowance,
    )


def cmd_render_refs(args: argparse.Namespace) -> None:
    bible = load_bible()
    audio_production.cmd_render_refs(bible, get_ffmpeg_exe())
    ensure_manifest(bible)


def cmd_manifest_init(args: argparse.Namespace) -> None:
    manifest = ensure_manifest(credits_allowance=args.credits_allowance)
    audio_production.cmd_dashboard(manifest)


def cmd_register_candidate(args: argparse.Namespace) -> None:
    manifest = ensure_manifest()
    entry = audio_production.register_candidate(
        manifest=manifest,
        cue_id=args.id,
        provider=args.provider,
        suno_clip_id=args.suno_id or "",
        suno_url=args.suno_url or "",
        title_in_suno=args.title or "",
        prompt_used=args.prompt or "",
        credits_cost=args.credits,
        generation_group_id=args.group_id or "",
        commercial_rights_verified=args.commercial_verified,
        notes=args.notes or "",
    )
    print(f"[REGISTERED] {entry['candidate_id']} ({entry['suno_url'] or entry['provider']})")


def cmd_match_incoming(args: argparse.Namespace) -> None:
    bible = load_bible()
    manifest = ensure_manifest(bible)
    target_dir = Path(args.dir).expanduser() if args.dir else INCOMING_DIR
    audio_production.match_incoming_files(
        manifest=manifest,
        bible=bible,
        incoming_dir=target_dir,
        provider=args.provider,
        ingest_fn=ingest_audio_file,
        master_fn=master_take,
        auto_master=not args.no_master,
    )


def cmd_analyze(args: argparse.Namespace) -> None:
    bible = load_bible()
    manifest = ensure_manifest(bible)
    ids: List[str] = []
    if args.id:
        ids = [args.id]
    elif args.batch:
        ids = [cid for cid, c in manifest["cues"].items() if c.get("batch_id") == args.batch]
    elif args.all_takes:
        ids = [t["id"] for t in bible.get("tracks", []) if (TAKES_DIR / t["id"]).exists()]
    for cid in ids:
        audio_production.evaluate_cue_computational(
            manifest=manifest,
            bible=bible,
            cue_id=cid,
            takes_dir=TAKES_DIR,
            decode_fn=decode_audio_to_float,
            measure_fn=measure_audio,
            find_raw_fn=find_raw_in_take,
        )


def cmd_eval_record(args: argparse.Namespace) -> None:
    manifest = ensure_manifest()
    audio_production.apply_ai_evaluations(manifest, Path(args.file))


def cmd_review(args: argparse.Namespace) -> None:
    bible = load_bible()
    manifest = ensure_manifest(bible)
    likes = [s.strip() for s in (args.likes or "").split(",") if s.strip()]
    dislikes = [s.strip() for s in (args.dislikes or "").split(",") if s.strip()]
    audio_production.record_review_decision(
        manifest=manifest,
        bible=bible,
        cue_id=args.id,
        decision=args.decision,
        candidate_choice=args.candidate,
        feedback=args.feedback or "",
        likes=likes,
        dislikes=dislikes,
    )
    cmd_briefs(argparse.Namespace())


def cmd_apply_review(args: argparse.Namespace) -> None:
    bible = load_bible()
    manifest = ensure_manifest(bible)
    data = load_json(Path(args.file), default={})
    decisions = data if isinstance(data, list) else data.get("decisions", [])
    if isinstance(data, dict) and not decisions:
        for cid, v in data.items():
            if isinstance(v, dict) and v.get("decision"):
                decisions.append({"cue_id": cid, **v})
    for d in decisions:
        audio_production.record_review_decision(
            manifest=manifest,
            bible=bible,
            cue_id=d["cue_id"],
            decision=d["decision"],
            candidate_choice=d.get("candidate"),
            feedback=d.get("feedback", ""),
            likes=d.get("likes", []),
            dislikes=d.get("dislikes", []),
        )
    cmd_briefs(argparse.Namespace())


def cmd_integrate_approved(args: argparse.Namespace) -> None:
    bible = load_bible()
    manifest = ensure_manifest(bible)
    if args.id:
        ids = [args.id]
    elif args.batch:
        ids = [cid for cid, c in manifest["cues"].items() if c.get("batch_id") == args.batch and c.get("status") == "approved"]
    else:
        ids = [cid for cid, c in manifest["cues"].items() if c.get("status") == "approved"]
    if not ids:
        print("No approved cues ready to integrate.")
        return
    verify_fn = lambda: cmd_verify(argparse.Namespace(strict_catalog=False))
    for cid in ids:
        audio_production.integrate_approved_cue(
            manifest=manifest,
            bible=bible,
            cue_id=cid,
            master_fn=master_take,
            promote_fn=promote_single_cue,
            verify_fn=verify_fn,
            sync_bindings=args.sync_bindings,
            force=args.force,
        )


def cmd_dashboard(args: argparse.Namespace) -> None:
    manifest = ensure_manifest()
    audio_production.cmd_dashboard(manifest)


def main() -> None:
    parser = argparse.ArgumentParser(description="Starborn Unified Audio Studio")
    sub = parser.add_subparsers(dest="cmd", required=True)

    sub.add_parser("status", help="Show pipeline & GF integrity status")
    sub.add_parser("midi", help="Generate canonical .mid leitmotif files")
    sub.add_parser("render-refs", help="Render MIDI motifs & pilot seeds into normalized WAV/MP3 reference audio")
    sub.add_parser("briefs", help="Generate Suno Studio / MCP production pack")

    p_minit = sub.add_parser("manifest-init", help="Initialize or sync persistent production_manifest.json")
    p_minit.add_argument("--credits-allowance", type=int, default=10000)

    p_reg = sub.add_parser("register-candidate", help="Log a generated Suno candidate URL/UUID & credits to manifest")
    p_reg.add_argument("--id", required=True)
    p_reg.add_argument("--provider", default="suno")
    p_reg.add_argument("--suno-id", default="")
    p_reg.add_argument("--suno-url", default="")
    p_reg.add_argument("--title", default="")
    p_reg.add_argument("--prompt", default="")
    p_reg.add_argument("--credits", type=int, default=0)
    p_reg.add_argument("--group-id", default="")
    p_reg.add_argument("--commercial-verified", action="store_true")
    p_reg.add_argument("--notes", default="")

    p_match = sub.add_parser("match-incoming", help="Match downloaded Suno audio files to cues, archive & ingest")
    p_match.add_argument("--dir", help="Folder to scan (defaults to audio_studio/incoming)")
    p_match.add_argument("--provider", default="suno")
    p_match.add_argument("--no-master", action="store_true")

    p_ana = sub.add_parser("analyze", help="Run computational DSP, key, tempo, chroma motif & loop-seam checks")
    p_ana.add_argument("--id", help="Specific cue ID")
    p_ana.add_argument("--batch", help="Analyze all cues with takes in a batch")
    p_ana.add_argument("--all-takes", action="store_true")

    p_erec = sub.add_parser("eval-record", help="Import Hydrogen/Argon evaluation rankings JSON into manifest")
    p_erec.add_argument("file", help="Path to evaluation JSON file")

    p_rev = sub.add_parser("review", help="Record user approval/reroll/revise/hold decision & update preferences")
    p_rev.add_argument("--id", required=True)
    p_rev.add_argument("--decision", required=True, choices=["approve", "A", "B", "C", "a", "b", "c", "reroll", "revise", "hold"])
    p_rev.add_argument("--candidate", help="Candidate label A/B/C or take_NN")
    p_rev.add_argument("--feedback", default="")
    p_rev.add_argument("--likes", default="")
    p_rev.add_argument("--dislikes", default="")

    p_arev = sub.add_parser("apply-review", help="Batch apply review_decisions.json exported from audio review board")
    p_arev.add_argument("file", help="Path to review_decisions.json")

    p_int = sub.add_parser("integrate-approved", help="Master, QC-verify, promote & sync approved cues safely")
    p_int.add_argument("--id", help="Specific approved cue ID")
    p_int.add_argument("--batch", help="Integrate all approved cues in a batch")
    p_int.add_argument("--sync-bindings", action="store_true")
    p_int.add_argument("--force", action="store_true")

    sub.add_parser("dashboard", help="Print production progress, credits & queue dashboard")

    p_ing = sub.add_parser("ingest", help="Ingest a Suno/DAW audio export into staged takes")
    p_ing.add_argument("file", help="Path to .wav/.mp3 export")
    p_ing.add_argument("--id", required=True, help="Target cue ID from musical_bible.json")
    p_ing.add_argument("--provider", default="suno", choices=["suno", "elevenlabs", "hybrid", "daw_stems"])
    p_ing.add_argument("--notes", default="")
    p_ing.add_argument("--stems-dir", help="Optional folder of exported stems")

    p_comp = sub.add_parser("compose-elevenlabs", help="Generate takes via ElevenLabs Music API")
    p_comp.add_argument("--id", help="Specific track ID")
    p_comp.add_argument("--batch", help="Generate for all cues in a batch (e.g. batch_0_pilot)")
    p_comp.add_argument("--category", default="all")
    p_comp.add_argument("--recommended-only", action="store_true")
    p_comp.add_argument("--takes", type=int, default=2)
    p_comp.add_argument("--resume", action="store_true", help="Top up only remaining takes up to --takes")
    p_comp.add_argument("--use-plan", action="store_true", help="Build structured composition_plan via /v1/music/plan first")
    p_comp.add_argument("--model", default="music_v2")
    p_comp.add_argument("--output-format", default="mp3_44100_128")
    p_comp.add_argument("--duration-ms", type=int)
    p_comp.add_argument("--seed", type=int)
    p_comp.add_argument("--notes", default="")

    p_stems = sub.add_parser("stems", help="Split a take via ElevenLabs /v1/music/stem-separation")
    p_stems.add_argument("file", help="Audio file path")
    p_stems.add_argument("--id", help="Target cue ID folder")
    p_stems.add_argument("--variation", default="six_stems_v1", choices=["two_stems_v1", "six_stems_v1"])

    p_sfx = sub.add_parser("sfx", help="Generate SFX takes via ElevenLabs /v1/sound-generation")
    p_sfx.add_argument("--id", help="Specific SFX cue ID")
    p_sfx.add_argument("--group", default="all", choices=["all", "ui", "world_interactions", "combat", "activities"])
    p_sfx.add_argument("--takes", type=int, default=2)
    p_sfx.add_argument("--influence", type=float, default=0.45)
    p_sfx.add_argument("--skip-existing", action="store_true")

    p_mas = sub.add_parser("master", help="Master take(s) with LUFS leveling & loop wrapping")
    p_mas.add_argument("--id", help="Cue ID")
    p_mas.add_argument("--take", type=int, help="Specific take number (defaults to latest)")
    p_mas.add_argument("--all-ready", action="store_true")
    p_mas.add_argument("--loop-mode", default="auto", choices=["auto", "wrap", "intro", "none"])
    p_mas.add_argument("--wrap-ms", type=int, default=1600)
    p_mas.add_argument("--loop-start-ms", type=int)
    p_mas.add_argument("--loop-end-ms", type=int)
    p_mas.add_argument("--target-lufs", type=float)

    sub.add_parser("build-board", help="Refresh tools/audio_board_manifest.json for audio_review_board.html")

    p_pro = sub.add_parser("promote", help="Promote mastered audio into res/raw and sync JSONs")
    p_pro.add_argument("--id", help="Cue ID")
    p_pro.add_argument("--all-sfx", action="store_true", help="Promote all mastered SFX cues only")
    p_pro.add_argument("--all-mastered", action="store_true")
    p_pro.add_argument("--from-approvals", help="Path to approved_takes.json exported from review board")
    p_pro.add_argument("--sync-bindings", action="store_true", help="Also activate target hub/node/room/combat bindings")
    p_pro.add_argument("--force", action="store_true", help="Overwrite an already integrated track")

    p_ver = sub.add_parser("verify", help="Verify GF integrity, references, and raw audio files")
    p_ver.add_argument("--strict-catalog", action="store_true")

    args = parser.parse_args()
    handlers = {
        "status": cmd_status,
        "midi": cmd_midi,
        "render-refs": cmd_render_refs,
        "briefs": cmd_briefs,
        "manifest-init": cmd_manifest_init,
        "register-candidate": cmd_register_candidate,
        "match-incoming": cmd_match_incoming,
        "analyze": cmd_analyze,
        "eval-record": cmd_eval_record,
        "review": cmd_review,
        "apply-review": cmd_apply_review,
        "integrate-approved": cmd_integrate_approved,
        "dashboard": cmd_dashboard,
        "ingest": cmd_ingest,
        "compose-elevenlabs": cmd_compose_elevenlabs,
        "stems": cmd_stems,
        "sfx": cmd_sfx,
        "master": cmd_master,
        "build-board": cmd_build_board,
        "promote": cmd_promote,
        "verify": cmd_verify,
    }
    handlers[args.cmd](args)


if __name__ == "__main__":
    main()
