#!/usr/bin/env python3
"""Starborn Soundtrack Production Manager (Phases 1-8).

Manages:
- CoreAudio MIDI -> WAV/MP3 reference audio rendering (including bespoke pilot refs)
- Persistent production manifest & resumable batch orchestration
- Computational audio + motif analysis wrapper & Hydrogen/Argon eval import/export
- Review loop (A/B/C/Reroll/Revise/Hold) & learned style preferences prompt refinement
- Smart batch download matcher (`match-incoming`), non-destructive source archive, & safe integration
- Production dashboard & Suno Premier credit / licensing audit
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import tempfile
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

import mido

import audio_analysis

ROOT_DIR = Path(__file__).resolve().parent.parent
STUDIO_DIR = ROOT_DIR / "audio_studio"
MIDI_DIR = STUDIO_DIR / "midi_motifs"
REFS_DIR = STUDIO_DIR / "reference_audio"
SOURCES_DIR = STUDIO_DIR / "sources"
EVALS_DIR = STUDIO_DIR / "evaluations"
MANIFEST_PATH = STUDIO_DIR / "production_manifest.json"
PREFS_PATH = STUDIO_DIR / "style_preferences.json"
SWIFT_RENDERER_SRC = ROOT_DIR / "scripts" / "render_midi_ref.swift"
SWIFT_RENDERER_BIN = ROOT_DIR / ".tools" / "render_midi_ref"

PILOT_IDS = [
    "music_w1_homestead_explore",
    "music_w1_combat",
    "music_astra_common_room",
    "music_w1_boss_warden",
]

# Track IDs categorized cleanly so Batch 0 (4) + Batches 1..6 (~10 each) = 58 primary score cues,
# while Batch 7 tracks the 8 remaining nightmare/boss-phase-2 variants + 5 stingers (13 total).
OPTIONAL_EXTENSION_IDS = {
    "music_victory_boss",
    "music_w4_conditioning",
    "music_w6_nightmare_zeke",
    "music_w6_nightmare_gh0st",
    "music_w6_nightmare_orion",
    "music_w3_boss_administrator",
    "music_w5_boss_compliance",
    "music_w6_boss_vale_p2",
    "stinger_motif_nova",
    "stinger_motif_jed",
    "stinger_motif_elara",
    "stinger_motif_vale",
    "stinger_motif_source",
}


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


def slugify(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", text.lower()).strip("_")


# ---------------------------------------------------------------------------
# Preferences & Prompt Refinement
# ---------------------------------------------------------------------------
def load_preferences() -> Dict[str, Any]:
    default = {
        "version": "1.0.0",
        "updated_at": datetime.now().isoformat(),
        "global_likes": [],
        "global_dislikes": ["vocals", "singing"],
        "category_likes": {},
        "category_dislikes": {},
        "recent_feedback": [],
    }
    prefs = load_json(PREFS_PATH, default=default)
    for k, v in default.items():
        prefs.setdefault(k, v)
    return prefs


def build_refined_prompt(entry: Dict[str, Any], prefs: Dict[str, Any], manifest_cue: Optional[Dict[str, Any]] = None) -> Tuple[str, str]:
    """Build Style Prompt + Exclude Styles applying learned user likes/dislikes."""
    base_prompt = entry.get("prompt", "").strip()
    cat = entry.get("category", "")
    is_vocal = "vocal" in entry.get("tags", [])

    likes = list(prefs.get("global_likes", [])) + list(prefs.get("category_likes", {}).get(cat, []))
    dislikes = ([] if is_vocal else list(prefs.get("global_dislikes", []))) + list(
        prefs.get("category_dislikes", {}).get(cat, [])
    )

    if manifest_cue and manifest_cue.get("revision_notes"):
        last_rev = manifest_cue["revision_notes"][-1]
        base_prompt = f"{base_prompt} Refinement direction: {last_rev}."

    if likes:
        base_prompt = f"{base_prompt} Emphasize preferred qualities: {', '.join(dict.fromkeys(likes))}."

    neg_parts = [s.strip() for s in entry.get("negative_prompt", "").split(",") if s.strip()]
    for d in dislikes:
        if d not in neg_parts:
            neg_parts.append(d)
    if is_vocal:
        neg_parts = [p for p in neg_parts if p.lower() not in ("vocals", "singing")]

    return base_prompt, ", ".join(neg_parts)


def apply_feedback_to_preferences(
    prefs: Dict[str, Any],
    cue_id: str,
    category: str,
    decision: str,
    feedback: str,
    likes: List[str],
    dislikes: List[str],
) -> Dict[str, Any]:
    """Update persistent style preferences from review feedback without overriding explicit rules."""
    for item in likes:
        clean = item.strip()
        if clean and clean not in prefs["global_likes"]:
            prefs["global_likes"].append(clean)
    for item in dislikes:
        clean = item.strip()
        if clean and clean not in prefs["global_dislikes"]:
            prefs["global_dislikes"].append(clean)

    # Extract lightweight explicit avoid hints from freeform feedback (e.g., "avoid saxophone", "no trap drums")
    if feedback:
        for pat in (r"\bavoid\s+([a-z0-9\s\-]{3,32})", r"\bno\s+([a-z0-9\s\-]{3,32})", r"\btoo\s+(harsh|muddy|loud|bright|chaotic|repetitive|robotic|generic)"):
            for m in re.finditer(pat, feedback.lower()):
                hint = m.group(1).strip(" .,;:")
                c_list = prefs["category_dislikes"].setdefault(category, [])
                if hint and hint not in c_list:
                    c_list.append(hint)

        prefs["recent_feedback"].append(
            {
                "timestamp": datetime.now().isoformat(),
                "cue_id": cue_id,
                "category": category,
                "decision": decision,
                "feedback": feedback,
            }
        )
        prefs["recent_feedback"] = prefs["recent_feedback"][-40:]

    prefs["updated_at"] = datetime.now().isoformat()
    save_json(PREFS_PATH, prefs)
    return prefs


# ---------------------------------------------------------------------------
# MIDI Reference Rendering (macOS CoreAudio DLS -> normalized WAV + MP3)
# ---------------------------------------------------------------------------
def ensure_swift_renderer() -> Path:
    if SWIFT_RENDERER_BIN.exists() and SWIFT_RENDERER_BIN.stat().st_mtime >= SWIFT_RENDERER_SRC.stat().st_mtime:
        return SWIFT_RENDERER_BIN
    SWIFT_RENDERER_BIN.parent.mkdir(parents=True, exist_ok=True)
    proc = subprocess.run(
        ["swiftc", "-O", str(SWIFT_RENDERER_SRC), "-o", str(SWIFT_RENDERER_BIN)],
        capture_output=True,
        text=True,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(f"Failed compiling MIDI renderer: {proc.stderr}")
    return SWIFT_RENDERER_BIN


def render_single_midi_to_ref(
    mid_path: Path,
    out_stem: Path,
    chord_program: int,
    melody_program: int,
    ffmpeg_exe: str,
) -> Tuple[Path, Path]:
    renderer = ensure_swift_renderer()
    out_stem.parent.mkdir(parents=True, exist_ok=True)
    wav_path = out_stem.with_suffix(".wav")
    mp3_path = out_stem.with_suffix(".mp3")

    with tempfile.TemporaryDirectory() as tmp:
        raw_wav = Path(tmp) / "raw.wav"
        p = subprocess.run(
            [str(renderer), str(mid_path), str(raw_wav), str(chord_program), str(melody_program)],
            capture_output=True,
            text=True,
            check=False,
        )
        if p.returncode != 0 or not raw_wav.exists():
            raise RuntimeError(f"CoreAudio MIDI render failed for {mid_path.name}: {p.stderr}")

        # Pass 1: measure loudnorm so Suno gets crisp -16 LUFS 16-bit 44.1kHz stereo WAV
        p1 = subprocess.run(
            [ffmpeg_exe, "-y", "-i", str(raw_wav), "-af", "loudnorm=I=-16:TP=-1.0:LRA=9:print_format=json", "-f", "null", "-"],
            capture_output=True,
            text=True,
            check=False,
        )
        m = re.search(r"\{[^{}]*\"input_i\"[^{}]*\}", p1.stderr, re.DOTALL)
        if m:
            stats = json.loads(m.group(0))
            flt = (
                f"loudnorm=I=-16:TP=-1.0:LRA=9:linear=true:"
                f"measured_I={stats['input_i']}:measured_TP={stats['input_tp']}:"
                f"measured_LRA={stats['input_lra']}:measured_thresh={stats['input_thresh']}:"
                f"offset={stats['target_offset']},alimiter=limit=0.89:attack=3:release=40"
            )
        else:
            flt = "alimiter=limit=0.89:attack=3:release=40"

        subprocess.run(
            [ffmpeg_exe, "-y", "-i", str(raw_wav), "-af", flt, "-ar", "44100", "-ac", "2", "-c:a", "pcm_s16le", str(wav_path)],
            capture_output=True,
            check=True,
        )
        subprocess.run(
            [ffmpeg_exe, "-y", "-i", str(wav_path), "-ar", "44100", "-ac", "2", "-c:a", "libmp3lame", "-b:a", "192k", str(mp3_path)],
            capture_output=True,
            check=True,
        )
    return wav_path, mp3_path


def write_pilot_reference_midis(bible: Dict[str, Any]) -> List[Dict[str, Any]]:
    """Create tempo- and key-matched pilot arrangement MIDI seeds tailored to the 4 pilot cues."""
    motifs = bible.get("motifs", {})
    nova = motifs["motif_nova"]["midi"]
    jed = motifs["motif_jed"]["midi"]
    vale = motifs["motif_vale"]["midi"]

    # Map pilot cues onto clean MIDI arrangements keeping canonical motif intervals.
    pilot_specs = [
        {
            "id": "pilot_music_w1_homestead_explore",
            "bpm": 86,
            "chord_prog": 48,
            "mel_prog": 25,  # Steel-string resonator guitar
            "chords": nova["chords_midi"],
            "melody": nova["melody_midi"],
            "note": "Nova D Minor -> D Major acoustic motif at 86 BPM for quiet Rusthaven exploration",
        },
        {
            "id": "pilot_music_w1_combat",
            "bpm": 138,
            "chord_prog": 18,  # Rock organ / sustained power pad
            "mel_prog": 29,  # Overdriven guitar
            "chords": nova["chords_midi"],
            "melody": nova["melody_midi"],
            "note": "Nova ascending D-F-G-A hook propelled to 138 BPM battle arrangement",
        },
        {
            "id": "pilot_music_astra_common_room",
            "bpm": 76,
            "chord_prog": 4,   # Rhodes EP backing
            "mel_prog": 24,  # Nylon guitar / Jed warmth (D->G compatible phrasing, 2-part seed)
            "chords": jed["chords_midi"][:4] + nova["chords_midi"][:4],
            "melody": jed["melody_midi"][:9] + nova["melody_midi"][:12],
            "note": "Jed G Major comfort transitioning into Nova D Minor hearth reflection (76 BPM)",
        },
        {
            "id": "pilot_music_w1_boss_warden",
            "bpm": 142,
            "chord_prog": 55,  # Orchestra Hit / Heavy Strings
            "mel_prog": 30,  # Distortion Guitar
            "chords": vale["chords_midi"][:4] + nova["chords_midi"][:4],
            "melody": vale["melody_midi"] + nova["melody_midi"][:12],
            "note": "Vale chromatic C-B-Bb-A threat colliding with Nova defiant D-F-G-A hook at 142 BPM",
        },
    ]

    tpb = 480
    out_specs: List[Dict[str, Any]] = []
    for p in pilot_specs:
        mid = mido.MidiFile(ticks_per_beat=tpb)
        tempo = mido.bpm2tempo(int(p["bpm"]))

        t_chords = mido.MidiTrack()
        mid.tracks.append(t_chords)
        t_chords.append(mido.MetaMessage("track_name", name=f"{p['id']}_chords", time=0))
        t_chords.append(mido.MetaMessage("set_tempo", tempo=tempo, time=0))
        t_chords.append(mido.Message("program_change", channel=1, program=p["chord_prog"], time=0))
        for chord in p["chords"]:
            dur = int(round(float(chord["beats"]) * tpb))
            for n in chord["notes"]:
                t_chords.append(mido.Message("note_on", channel=1, note=int(n), velocity=68, time=0))
            for idx, n in enumerate(chord["notes"]):
                t_chords.append(mido.Message("note_off", channel=1, note=int(n), velocity=0, time=dur if idx == 0 else 0))

        t_mel = mido.MidiTrack()
        mid.tracks.append(t_mel)
        t_mel.append(mido.MetaMessage("track_name", name=f"{p['id']}_melody", time=0))
        t_mel.append(mido.Message("program_change", channel=0, program=p["mel_prog"], time=0))
        for note in p["melody"]:
            dur = int(round(float(note["beats"]) * tpb))
            t_mel.append(mido.Message("note_on", channel=0, note=int(note["pitch"]), velocity=int(note.get("vel", 95)), time=0))
            t_mel.append(mido.Message("note_off", channel=0, note=int(note["pitch"]), velocity=0, time=dur))

        mid_path = MIDI_DIR / f"{p['id']}.mid"
        mid.save(str(mid_path))
        out_specs.append({**p, "mid_path": mid_path})
    return out_specs


def cmd_render_refs(bible: Dict[str, Any], ffmpeg_exe: str) -> None:
    """Render all canonical motif MIDIs and pilot arrangement MIDIs to WAV + MP3."""
    REFS_DIR.mkdir(parents=True, exist_ok=True)
    rendered = []

    for mid_id, motif in bible.get("motifs", {}).items():
        mid_path = MIDI_DIR / f"{mid_id}.mid"
        if not mid_path.exists():
            continue
        mel_prog = int(motif.get("midi", {}).get("program", 25))
        chord_prog = 48 if mid_id != "motif_source" else 89
        wav_p, mp3_p = render_single_midi_to_ref(mid_path, REFS_DIR / mid_id, chord_prog, mel_prog, ffmpeg_exe)
        rendered.append((mid_id, wav_p, mp3_p))
        print(f"[REF WAV] {wav_p.relative_to(ROOT_DIR)} (prog={mel_prog})")

    pilot_specs = write_pilot_reference_midis(bible)
    for spec in pilot_specs:
        wav_p, mp3_p = render_single_midi_to_ref(
            spec["mid_path"],
            REFS_DIR / spec["id"],
            spec["chord_prog"],
            spec["mel_prog"],
            ffmpeg_exe,
        )
        rendered.append((spec["id"], wav_p, mp3_p))
        print(f"[PILOT REF WAV] {wav_p.relative_to(ROOT_DIR)} ({spec['bpm']} BPM)")

    print(f"Rendered {len(rendered)} reference WAV/MP3 pairs in {REFS_DIR.relative_to(ROOT_DIR)}")


# ---------------------------------------------------------------------------
# Persistent Production Manifest & Batches
# ---------------------------------------------------------------------------
def assign_batch_id(cue_id: str, index_in_primary: int) -> str:
    if cue_id in PILOT_IDS:
        return "batch_0_pilot"
    if cue_id in OPTIONAL_EXTENSION_IDS:
        return "batch_7_extensions_and_stingers"
    # Group 54 remaining primary tracks into batches of ~9-10 cues (batch_1 .. batch_6)
    batch_num = 1 + (index_in_primary // 10)
    return f"batch_{batch_num}"


def load_or_init_manifest(
    bible: Dict[str, Any],
    backup_manifest: Optional[Dict[str, Any]],
    raw_dir: Path,
    takes_dir: Path,
    mastered_dir: Path,
    credits_allowance: int = 10000,
) -> Dict[str, Any]:
    """Initialize or non-destructively synchronize audio_studio/production_manifest.json."""
    existing = load_json(MANIFEST_PATH, default={})
    cues_existing: Dict[str, Any] = existing.get("cues", {})

    tracks = bible.get("tracks", [])
    primary_list = [t for t in tracks if t["id"] not in PILOT_IDS and t["id"] not in OPTIONAL_EXTENSION_IDS]
    primary_idx = {t["id"]: i for i, t in enumerate(primary_list)}

    new_cues: Dict[str, Any] = {}
    for t in tracks:
        cid = t["id"]
        prev = cues_existing.get(cid, {})
        batch_id = prev.get("batch_id") or assign_batch_id(cid, primary_idx.get(cid, 0))
        scope = "pilot" if cid in PILOT_IDS else ("extension_or_stinger" if cid in OPTIONAL_EXTENSION_IDS else "primary_58")
        has_live = (raw_dir / f"{cid}.mp3").exists()
        has_master = (mastered_dir / f"{cid}.mp3").exists()

        # Infer default status safely without clobbering reviewed/approved states
        default_status = "pending"
        if prev.get("status"):
            default_status = prev["status"]
        elif has_master:
            default_status = "mastered_ready"

        cue_rec = {
            "id": cid,
            "title": t.get("title", cid),
            "category": t.get("category", ""),
            "scope": scope,
            "batch_id": batch_id,
            "recommended_provider": t.get("recommended_provider", "suno"),
            "key": t.get("key", ""),
            "bpm": t.get("bpm", 0),
            "loop": bool(t.get("loop", True)),
            "motif_refs": t.get("motif_refs", []),
            "reference_wav": (
                f"audio_studio/reference_audio/pilot_{cid}.wav"
                if (REFS_DIR / f"pilot_{cid}.wav").exists()
                else (
                    f"audio_studio/reference_audio/{t['motif_refs'][0]}.wav"
                    if t.get("motif_refs") and (REFS_DIR / f"{t['motif_refs'][0]}.wav").exists()
                    else None
                )
            ),
            "status": default_status,
            "approved_candidate_id": prev.get("approved_candidate_id"),
            "approved_take": prev.get("approved_take"),
            "commercial_rights_verified": bool(prev.get("commercial_rights_verified", False)),
            "commercial_use_flagged": bool(prev.get("commercial_use_flagged", False)),
            "in_game_verified": bool(prev.get("in_game_verified", False)),
            "existing_live_asset": has_live,
            "revision_notes": prev.get("revision_notes", []),
            "candidates": prev.get("candidates", []),
            "shortlist": prev.get("shortlist", {}),
            "rejected_ids_preserved": prev.get("rejected_ids_preserved", []),
        }
        new_cues[cid] = cue_rec

    # Preserve lock metadata for immutable Great Frontier tapes
    gf_locks = {}
    if backup_manifest:
        for fname, info in backup_manifest.get("gf_tracks_immutable", {}).items():
            gf_locks[fname] = {
                "sha256": info["sha256"],
                "status": "immutable_locked",
                "never_modify": True,
            }

    manifest = {
        "version": "1.0.0",
        "updated_at": datetime.now().isoformat(),
        "suno_plan": {
            "tier": existing.get("suno_plan", {}).get("tier", "premier_pending_verification"),
            "monthly_credit_allowance": existing.get("suno_plan", {}).get("monthly_credit_allowance", credits_allowance),
            "credits_consumed": existing.get("suno_plan", {}).get("credits_consumed", 0),
            "estimated_credits_per_generation_2_outputs": existing.get("suno_plan", {}).get("estimated_credits_per_generation_2_outputs", 10),
            "web_download_monthly_cap": 60,
            "studio_exports_unlimited_on_premier": True,
            "mcp_server_url": "https://api.suno.com/mcp",
        },
        "target_scope_summary": {
            "pilot_cues": len(PILOT_IDS),
            "primary_score_cues": sum(1 for c in new_cues.values() if c["scope"] in ("pilot", "primary_58")),
            "extension_and_stinger_cues": sum(1 for c in new_cues.values() if c["scope"] == "extension_or_stinger"),
            "total_bible_music_and_stingers": len(new_cues),
            "gf_tapes_immutable": len(gf_locks),
        },
        "immutable_gf_tapes": gf_locks,
        "cues": new_cues,
    }
    save_json(MANIFEST_PATH, manifest)
    load_preferences()
    return manifest


def recompute_credits(manifest: Dict[str, Any]) -> None:
    total = 0
    for c in manifest.get("cues", {}).values():
        seen_gen_ids = set()
        for cand in c.get("candidates", []):
            gid = cand.get("generation_group_id") or cand.get("candidate_id")
            if gid and gid not in seen_gen_ids:
                seen_gen_ids.add(gid)
                total += int(cand.get("credits_cost", 0))
    manifest["suno_plan"]["credits_consumed"] = total


def register_candidate(
    manifest: Dict[str, Any],
    cue_id: str,
    provider: str,
    suno_clip_id: str = "",
    suno_url: str = "",
    title_in_suno: str = "",
    prompt_used: str = "",
    credits_cost: int = 0,
    generation_group_id: str = "",
    commercial_rights_verified: bool = False,
    local_raw_path: Optional[str] = None,
    take_num: Optional[int] = None,
    notes: str = "",
) -> Dict[str, Any]:
    if cue_id not in manifest["cues"]:
        raise RuntimeError(f"Unknown cue ID '{cue_id}' in production_manifest.json")
    cue = manifest["cues"][cue_id]
    cands = cue.setdefault("candidates", [])
    idx = len(cands) + 1
    cand_id = f"{cue_id}_cand_{idx:02d}"
    if suno_clip_id and not suno_url:
        suno_url = f"https://suno.com/song/{suno_clip_id}"

    entry = {
        "candidate_id": cand_id,
        "take_num": take_num,
        "provider": provider,
        "created_at": datetime.now().isoformat(),
        "suno_clip_id": suno_clip_id,
        "suno_url": suno_url,
        "title_in_suno": title_in_suno or cue["title"],
        "generation_group_id": generation_group_id,
        "prompt_used": prompt_used,
        "credits_cost": int(credits_cost),
        "commercial_rights_verified": bool(commercial_rights_verified),
        "local_raw_path": local_raw_path,
        "mastered_path": None,
        "computational": None,
        "ai_eval_hydrogen": None,
        "ai_eval_argon": None,
        "rank": None,
        "shortlist_label": None,  # A, B, C, or rejected
        "notes": notes,
    }
    cands.append(entry)
    if cue["status"] == "pending":
        cue["status"] = "candidates_registered"
    recompute_credits(manifest)
    manifest["updated_at"] = datetime.now().isoformat()
    save_json(MANIFEST_PATH, manifest)
    return entry


# ---------------------------------------------------------------------------
# Batch Incoming Download Matcher (Phase 2 Automated Fallback)
# ---------------------------------------------------------------------------
def match_incoming_files(
    manifest: Dict[str, Any],
    bible: Dict[str, Any],
    incoming_dir: Path,
    provider: str,
    ingest_fn: Any,
    master_fn: Any,
    auto_master: bool = True,
) -> List[Dict[str, Any]]:
    """Match downloaded Suno WAV/MP3 files in `incoming_dir` to manifest cues by UUID, cue ID, or title."""
    if not incoming_dir.exists():
        print(f"Directory does not exist: {incoming_dir}")
        return []

    files = sorted(
        [p for p in incoming_dir.iterdir() if p.is_file() and p.suffix.lower() in (".wav", ".mp3", ".flac", ".m4a")]
    )
    if not files:
        print(f"No audio files found in {incoming_dir}")
        return []

    cues = manifest.get("cues", {})
    # Build lookup indices
    by_suno_id: Dict[str, Tuple[str, Dict[str, Any]]] = {}
    by_cue_id = {cid.lower(): cid for cid in cues}
    by_title = {slugify(c["title"]): cid for cid in cues if c.get("title")}

    for cid, c in cues.items():
        for cand in c.get("candidates", []):
            if cand.get("suno_clip_id"):
                by_suno_id[cand["suno_clip_id"].lower()] = (cid, cand)
            if cand.get("title_in_suno"):
                by_title[slugify(cand["title_in_suno"])] = cid

    matched_results = []
    for fp in files:
        stem_lower = fp.stem.lower()
        stem_slug = slugify(fp.stem)
        matched_cid: Optional[str] = None
        matched_cand: Optional[Dict[str, Any]] = None

        # 1. Exact Suno UUID substring match
        for sid, (cid, cand) in by_suno_id.items():
            if sid in stem_lower:
                matched_cid, matched_cand = cid, cand
                break
        # 2. Exact cue ID prefix/substring match (prefer longest match so music_w1_boss_warden > music_w1)
        if matched_cid is None:
            for cid_low in sorted(by_cue_id.keys(), key=len, reverse=True):
                if cid_low in stem_lower or cid_low in stem_slug:
                    matched_cid = by_cue_id[cid_low]
                    break
        # 3. Suno song title slug match
        if matched_cid is None:
            for tslug in sorted(by_title.keys(), key=len, reverse=True):
                if len(tslug) >= 5 and tslug in stem_slug:
                    matched_cid = by_title[tslug]
                    break

        if matched_cid is None:
            print(f"[UNMATCHED] {fp.name} -> rename to contain <cue_id> or Suno clip UUID and re-run match-incoming.")
            continue

        # Archive pristine source non-destructively
        src_archive_dir = SOURCES_DIR / matched_cid
        src_archive_dir.mkdir(parents=True, exist_ok=True)
        shutil.copy2(fp, src_archive_dir / fp.name)

        take_dir = ingest_fn(
            source_path=fp,
            cue_id=matched_cid,
            provider=provider,
            bible=bible,
            notes=f"Matched from {fp.name}",
            extra_meta={"matched_suno_clip_id": matched_cand["suno_clip_id"] if matched_cand else None},
        )
        take_num = int(take_dir.name.split("_")[-1])
        rel_raw = str((take_dir / f"raw{fp.suffix.lower()}").relative_to(ROOT_DIR))

        if matched_cand is None:
            matched_cand = register_candidate(
                manifest,
                matched_cid,
                provider=provider,
                local_raw_path=rel_raw,
                take_num=take_num,
                notes=f"Matched file {fp.name}",
            )
        else:
            matched_cand["take_num"] = take_num
            matched_cand["local_raw_path"] = rel_raw

        if auto_master:
            out_m = master_fn(matched_cid, bible, take_num=take_num)
            matched_cand["mastered_path"] = str(out_m.relative_to(ROOT_DIR))

        cue = manifest["cues"][matched_cid]
        if cue["status"] in ("pending", "candidates_registered", "reroll_requested"):
            cue["status"] = "candidates_ready"
        matched_results.append({"file": fp.name, "cue_id": matched_cid, "take": take_num})

    save_json(MANIFEST_PATH, manifest)
    print(f"Matched {len(matched_results)}/{len(files)} file(s).")
    return matched_results


# ---------------------------------------------------------------------------
# Phase 4: Computational Evaluation + Hydrogen / Argon Evaluation Dossier
# ---------------------------------------------------------------------------
def evaluate_cue_computational(
    manifest: Dict[str, Any],
    bible: Dict[str, Any],
    cue_id: str,
    takes_dir: Path,
    decode_fn: Any,
    measure_fn: Any,
    find_raw_fn: Any,
) -> Dict[str, Any]:
    """Run computational audio/motif/loop analysis on every take of `cue_id` and generate eval dossier."""
    cdir = takes_dir / cue_id
    if not cdir.exists():
        print(f"[SKIP ANALYZE] No takes found for {cue_id}")
        return {}

    cue = manifest["cues"].get(cue_id)
    if cue is None:
        raise RuntimeError(f"Cue '{cue_id}' missing from manifest")
    bible_entry = next((t for t in bible.get("tracks", []) if t["id"] == cue_id), None)
    if bible_entry is None:
        raise RuntimeError(f"Cue '{cue_id}' missing from musical_bible.json")

    motif_midis = {
        m: (MIDI_DIR / f"{m}.mid")
        for m in bible_entry.get("motif_refs", [])
        if (MIDI_DIR / f"{m}.mid").exists()
    }

    cand_by_take = {c.get("take_num"): c for c in cue.get("candidates", []) if c.get("take_num") is not None}
    take_reports = []
    for td in sorted(cdir.iterdir()):
        if not td.is_dir() or not td.name.startswith("take_"):
            continue
        take_num = int(td.name.split("_")[-1])
        raw_p = find_raw_fn(td)
        samples = decode_fn(raw_p)
        metrics = measure_fn(raw_p)
        report = audio_analysis.analyze_audio_candidate(raw_p, samples, metrics, bible_entry, motif_midis)
        save_json(td / "analysis.json", report)
        take_reports.append({"take": take_num, "path": str(raw_p), "report": report})

        cand = cand_by_take.get(take_num)
        if cand is None:
            prov = load_json(td / "provenance.json", default={})
            cand = register_candidate(
                manifest,
                cue_id,
                provider=prov.get("provider", "unknown"),
                local_raw_path=str(raw_p.relative_to(ROOT_DIR)),
                take_num=take_num,
                suno_clip_id=prov.get("matched_suno_clip_id") or "",
            )
            cand_by_take[take_num] = cand
        cand["computational"] = report

    # Preliminary computational ordering (purely technical until Hydrogen listening evals refine it!)
    take_reports.sort(key=lambda r: r["report"]["computational_score"]["score"], reverse=True)
    for rank_idx, item in enumerate(take_reports, start=1):
        cand = cand_by_take.get(item["take"])
        if cand and cand.get("ai_eval_hydrogen") is None:
            cand["rank"] = rank_idx
            cand["shortlist_label"] = ["A", "B", "C"][rank_idx - 1] if rank_idx <= 3 else "rejected"

    EVALS_DIR.mkdir(parents=True, exist_ok=True)
    pilot_ref_mp3 = REFS_DIR / f"pilot_{cue_id}.mp3"
    motif_ref_mp3 = (
        REFS_DIR / f"{bible_entry['motif_refs'][0]}.mp3"
        if bible_entry.get("motif_refs")
        else None
    )
    ref_audio_path = (
        str(pilot_ref_mp3)
        if pilot_ref_mp3.exists()
        else (str(motif_ref_mp3) if motif_ref_mp3 and motif_ref_mp3.exists() else None)
    )
    dossier = {
        "cue_id": cue_id,
        "title": bible_entry["title"],
        "category": bible_entry["category"],
        "target_key": bible_entry.get("key"),
        "target_bpm": bible_entry.get("bpm"),
        "loop_required": bool(bible_entry.get("loop", True)),
        "motif_refs": bible_entry.get("motif_refs", []),
        "reference_audio_mp3": ref_audio_path,
        "scene_prompt": bible_entry.get("prompt"),
        "takes_evaluated": [
            {
                "take": r["take"],
                "audio_path": r["path"],
                "computational_score": r["report"]["computational_score"]["score"],
                "estimated_keys": r["report"]["estimated_keys"],
                "tempo": r["report"]["tempo"],
                "motif_alignment": r["report"]["motif_alignment"],
                "mix_and_loop": r["report"]["mix_and_loop"],
            }
            for r in take_reports
        ],
    }
    dossier_path = EVALS_DIR / f"{cue_id}_eval_dossier.json"
    save_json(dossier_path, dossier)
    save_json(MANIFEST_PATH, manifest)
    print(f"[ANALYZED] {cue_id}: {len(take_reports)} take(s) -> {dossier_path.relative_to(ROOT_DIR)}")
    return dossier


def apply_ai_evaluations(manifest: Dict[str, Any], eval_json_path: Path) -> None:
    """Import Hydrogen/Argon qualitative listening evaluations & A/B/C/rejected rankings safely."""
    data = load_json(eval_json_path)
    if not data:
        raise RuntimeError(f"Unreadable eval file: {eval_json_path}")
    entries = data if isinstance(data, list) else data.get("cues", [data])
    for item in entries:
        cid = item["cue_id"]
        cue = manifest["cues"].get(cid)
        if not cue:
            continue
        by_take = {c.get("take_num"): c for c in cue.get("candidates", [])}
        by_cid = {c["candidate_id"]: c for c in cue.get("candidates", [])}
        shortlist: Dict[str, Any] = {}
        rejected: List[Dict[str, Any]] = []

        for c_eval in item.get("candidates", []):
            cand = by_cid.get(c_eval.get("candidate_id")) or by_take.get(c_eval.get("take_num"))
            if not cand:
                continue
            if c_eval.get("hydrogen_eval"):
                cand["ai_eval_hydrogen"] = c_eval["hydrogen_eval"]
            if c_eval.get("argon_eval"):
                cand["ai_eval_argon"] = c_eval["argon_eval"]
            cand["rank"] = c_eval.get("rank")
            label = c_eval.get("shortlist_label") or ("rejected" if (cand["rank"] or 99) > 3 else ["A", "B", "C"][cand["rank"] - 1])
            cand["shortlist_label"] = label
            if label in ("A", "B", "C"):
                shortlist[label] = {
                    "candidate_id": cand["candidate_id"],
                    "take_num": cand.get("take_num"),
                    "suno_url": cand.get("suno_url"),
                    "rationale": c_eval.get("rationale", ""),
                }
            else:
                rejected.append(
                    {
                        "candidate_id": cand["candidate_id"],
                        "take_num": cand.get("take_num"),
                        "suno_clip_id": cand.get("suno_clip_id"),
                        "suno_url": cand.get("suno_url"),
                        "reason": c_eval.get("rationale", "Ranked outside top 3"),
                    }
                )
        cue["shortlist"] = shortlist
        cue["rejected_ids_preserved"] = rejected
        if shortlist and cue["status"] not in ("approved", "integrated", "verified_in_game"):
            cue["status"] = "ready_for_review"
    save_json(MANIFEST_PATH, manifest)
    print(f"Applied AI evaluations from {eval_json_path.name}")


# ---------------------------------------------------------------------------
# Phase 5 & 6: Review Loop & Safe Integration
# ---------------------------------------------------------------------------
def record_review_decision(
    manifest: Dict[str, Any],
    bible: Dict[str, Any],
    cue_id: str,
    decision: str,
    candidate_choice: Optional[str] = None,
    feedback: str = "",
    likes: Optional[List[str]] = None,
    dislikes: Optional[List[str]] = None,
) -> None:
    cue = manifest["cues"].get(cue_id)
    if not cue:
        raise RuntimeError(f"Unknown cue '{cue_id}'")
    decision_norm = decision.strip().lower()
    if decision_norm in ("a", "b", "c"):
        candidate_choice = decision_norm.upper()
        decision_norm = "approve"

    if decision_norm == "approve":
        chosen = None
        if candidate_choice:
            choice_up = candidate_choice.strip().upper()
            if choice_up in cue.get("shortlist", {}):
                chosen_id = cue["shortlist"][choice_up]["candidate_id"]
                chosen = next((c for c in cue["candidates"] if c["candidate_id"] == chosen_id), None)
            if chosen is None:
                chosen = next(
                    (
                        c
                        for c in cue["candidates"]
                        if c["candidate_id"] == candidate_choice
                        or (c.get("shortlist_label") or "").upper() == choice_up
                        or f"TAKE_{c.get('take_num'):02d}" == choice_up
                        or str(c.get("take_num")) == candidate_choice
                    ),
                    None,
                )
        if chosen is None and cue.get("shortlist", {}).get("A"):
            chosen_id = cue["shortlist"]["A"]["candidate_id"]
            chosen = next((c for c in cue["candidates"] if c["candidate_id"] == chosen_id), None)
        if chosen is None and cue.get("candidates"):
            chosen = cue["candidates"][-1]
        if chosen is None:
            raise RuntimeError(f"No candidate found to approve for '{cue_id}'")

        cue["status"] = "approved"
        cue["approved_candidate_id"] = chosen["candidate_id"]
        cue["approved_take"] = chosen.get("take_num")
        cue["commercial_rights_verified"] = bool(chosen.get("commercial_rights_verified", False))
        cue["commercial_use_flagged"] = not cue["commercial_rights_verified"]
    elif decision_norm == "reroll":
        cue["status"] = "reroll_requested"
        if feedback:
            cue.setdefault("revision_notes", []).append(f"[REROLL] {feedback}")
    elif decision_norm == "revise":
        cue["status"] = "revision_requested"
        if feedback:
            cue.setdefault("revision_notes", []).append(feedback)
    elif decision_norm == "hold":
        cue["status"] = "on_hold"
        if feedback:
            cue.setdefault("revision_notes", []).append(f"[HOLD] {feedback}")
    else:
        raise RuntimeError(f"Unsupported review decision: '{decision}'")

    prefs = load_preferences()
    apply_feedback_to_preferences(
        prefs,
        cue_id=cue_id,
        category=cue.get("category", ""),
        decision=decision_norm,
        feedback=feedback,
        likes=likes or [],
        dislikes=dislikes or [],
    )
    save_json(MANIFEST_PATH, manifest)
    print(f"[REVIEW] {cue_id} -> status={cue['status']} (take={cue.get('approved_take')})")


def integrate_approved_cue(
    manifest: Dict[str, Any],
    bible: Dict[str, Any],
    cue_id: str,
    master_fn: Any,
    promote_fn: Any,
    verify_fn: Any,
    sync_bindings: bool = True,
    force: bool = False,
) -> None:
    cue = manifest["cues"].get(cue_id)
    if not cue:
        raise RuntimeError(f"Unknown cue '{cue_id}'")
    if cue["status"] in ("integrated", "verified_in_game") and not force:
        raise RuntimeError(
            f"SAFETY STOP: '{cue_id}' is already integrated/approved. Pass --force to overwrite."
        )
    if cue["status"] not in ("approved", "integrated", "verified_in_game"):
        raise RuntimeError(f"Cue '{cue_id}' is status='{cue['status']}'. Approve a candidate first.")

    take_num = cue.get("approved_take")
    master_fn(cue_id, bible, take_num=take_num)
    promote_fn(cue_id=cue_id, bible=bible, sync_bindings=sync_bindings, force=force)
    verify_fn()

    cue["status"] = "integrated"
    cue["integrated_at"] = datetime.now().isoformat()
    if not cue.get("commercial_rights_verified"):
        cue["commercial_use_flagged"] = True
        print(f"  [LICENSE FLAG] {cue_id}: integrated cleanly, but commercial_rights_verified=False (confirm Premier tier before ship).")
    save_json(MANIFEST_PATH, manifest)


# ---------------------------------------------------------------------------
# Dashboard (Phase 8)
# ---------------------------------------------------------------------------
def cmd_dashboard(manifest: Dict[str, Any]) -> None:
    cues = list(manifest.get("cues", {}).values())
    primary = [c for c in cues if c["scope"] in ("pilot", "primary_58")]
    total_primary = len(primary)
    by_status: Dict[str, int] = {}
    for c in cues:
        by_status[c["status"]] = by_status.get(c["status"], 0) + 1

    approved_plus = sum(1 for c in primary if c["status"] in ("approved", "integrated", "verified_in_game"))
    integrated_n = sum(1 for c in primary if c["status"] in ("integrated", "verified_in_game"))
    verified_n = sum(1 for c in primary if c["status"] == "verified_in_game")
    reroll_n = sum(1 for c in cues if c["status"] in ("reroll_requested", "revision_requested"))
    hold_n = sum(1 for c in cues if c["status"] == "on_hold")
    cand_total = sum(len(c.get("candidates", [])) for c in cues)
    flagged_lic = [c["id"] for c in cues if c.get("commercial_use_flagged")]

    suno_cfg = manifest.get("suno_plan", {})
    allowance = int(suno_cfg.get("monthly_credit_allowance", 10000))
    consumed = int(suno_cfg.get("credits_consumed", 0))
    pct = (approved_plus / float(total_primary) * 100.0) if total_primary else 0.0

    print("=== Starborn Soundtrack Production Dashboard ===")
    print(f"Scope: {total_primary} primary cues (Batch 0 Pilot + Batches 1-6) | {len(cues)} total planned incl. extensions/stingers | {len(manifest.get('immutable_gf_tapes', {}))} GF tapes locked")
    print(f"Completion (Primary): {approved_plus}/{total_primary} ({pct:.1f}%) approved | {integrated_n} integrated | {verified_n} in-game verified")
    print(f"Candidates Generated: {cand_total} | Reroll/Revise queue: {reroll_n} | On Hold: {hold_n}")
    print(f"Statuses (all): {by_status}")
    print(f"Suno Credits: {consumed} used / {allowance} monthly allowance (~{max(0, allowance - consumed)} est. remaining)")
    print(f"Suno MCP Server: {suno_cfg.get('mcp_server_url')} | Studio exports on Premier: unlimited vs 60/mo standard web cap")
    if flagged_lic:
        print(f"Commercial License Check Needed ({len(flagged_lic)}): {', '.join(flagged_lic[:10])}")

    # Batch summary
    batches: Dict[str, List[Dict[str, Any]]] = {}
    for c in cues:
        batches.setdefault(c["batch_id"], []).append(c)
    print("\n--- Batches ---")
    for bname in sorted(batches.keys()):
        items = batches[bname]
        done_b = sum(1 for c in items if c["status"] in ("approved", "integrated", "verified_in_game"))
        print(f"  {bname:32s}: {done_b}/{len(items)} approved ({sum(len(c.get('candidates',[])) for c in items)} candidates)")
