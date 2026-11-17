#!/usr/bin/env python3
"""Computational audio analysis & heuristic leitmotif verification for Starborn."""

from __future__ import annotations

import math
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

import numpy as np

try:
    import mido
except ImportError:
    mido = None

NOTE_NAMES = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]

# Krumhansl-Schmuckler tonal key profiles (C major and C minor rooted at index 0)
KRUMHANSL_MAJOR = np.array([6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88], dtype=np.float64)
KRUMHANSL_MINOR = np.array([6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17], dtype=np.float64)


def cosine_sim(a: np.ndarray, b: np.ndarray) -> float:
    na = float(np.linalg.norm(a))
    nb = float(np.linalg.norm(b))
    if na <= 1e-12 or nb <= 1e-12:
        return 0.0
    return float(np.dot(a, b) / (na * nb))


def midi_file_to_chroma(mid_path: Path) -> Tuple[np.ndarray, List[int]]:
    """Extract normalized 12-bin pitch-class distribution & melody pitch-classes from a .mid file."""
    chroma = np.zeros(12, dtype=np.float64)
    melody_pcs: List[int] = []
    if mido is None or not mid_path.exists():
        return chroma, melody_pcs

    mid = mido.MidiFile(str(mid_path))
    for track_idx, track in enumerate(mid.tracks):
        weight = 2.0 if track_idx >= 1 else 1.0
        for msg in track:
            if msg.type == "note_on" and msg.velocity > 0:
                pc = int(msg.note) % 12
                chroma[pc] += weight * (float(msg.velocity) / 127.0)
                if track_idx >= 1 or len(mid.tracks) == 1:
                    melody_pcs.append(pc)

    total = float(np.sum(chroma))
    if total > 0:
        chroma /= total
    return chroma, melody_pcs


def compute_audio_chroma(samples: np.ndarray, sr: int = 44100, max_frames: int = 42) -> Tuple[np.ndarray, np.ndarray]:
    """Compute a 12-bin pitch-class chromagram across MIDI notes C2..B6 using Hann-windowed FFTs."""
    if len(samples) == 0:
        return np.zeros(12, dtype=np.float64), np.zeros((0, 12), dtype=np.float64)

    mono = np.mean(samples, axis=1) if samples.ndim == 2 else samples
    n_fft = 32768
    if len(mono) < n_fft:
        mono = np.pad(mono, (0, n_fft - len(mono)))

    step = max(n_fft // 2, (len(mono) - n_fft) // max(1, max_frames - 1))
    starts = list(range(0, max(1, len(mono) - n_fft + 1), step))[:max_frames]

    window = np.hanning(n_fft)
    freqs = np.fft.rfftfreq(n_fft, d=1.0 / float(sr))
    valid_mask = (freqs >= 62.0) & (freqs <= 2100.0)
    valid_freqs = freqs[valid_mask]
    midi_bins = np.round(69.0 + 12.0 * np.log2(valid_freqs / 440.0)).astype(int)
    pc_bins = midi_bins % 12

    frame_chromas: List[np.ndarray] = []
    for s in starts:
        seg = mono[s : s + n_fft]
        if float(np.sqrt(np.mean(seg ** 2))) < 1e-4:
            continue
        mag = np.abs(np.fft.rfft(seg * window))[valid_mask]
        power = mag ** 1.5
        chroma = np.bincount(pc_bins, weights=power, minlength=12).astype(np.float64)
        total = float(np.sum(chroma))
        if total > 0:
            chroma /= total
            frame_chromas.append(chroma)

    if not frame_chromas:
        return np.zeros(12, dtype=np.float64), np.zeros((0, 12), dtype=np.float64)

    mat = np.vstack(frame_chromas)
    glob = np.mean(mat, axis=0)
    gsum = float(np.sum(glob))
    if gsum > 0:
        glob /= gsum
    return glob, mat


def estimate_keys(global_chroma: np.ndarray, top_k: int = 3) -> List[Dict[str, Any]]:
    """Estimate musical key using Krumhansl-Schmuckler major/minor template Pearson correlations."""
    if float(np.sum(global_chroma)) <= 1e-9:
        return []
    results: List[Dict[str, Any]] = []
    for shift in range(12):
        maj_rot = np.roll(KRUMHANSL_MAJOR, shift)
        min_rot = np.roll(KRUMHANSL_MINOR, shift)
        r_maj = float(np.corrcoef(global_chroma, maj_rot)[0, 1])
        r_min = float(np.corrcoef(global_chroma, min_rot)[0, 1])
        results.append({"key": f"{NOTE_NAMES[shift]} Major", "correlation": round(r_maj, 3)})
        results.append({"key": f"{NOTE_NAMES[shift]} Minor", "correlation": round(r_min, 3)})
    results.sort(key=lambda x: x["correlation"], reverse=True)
    return results[:top_k]


def compare_chroma_to_motifs(
    global_chroma: np.ndarray,
    frame_chromas: np.ndarray,
    motif_midis: Dict[str, Path],
) -> Dict[str, Any]:
    """Compare audio chroma against target motif .mid pitch-class distributions."""
    out: Dict[str, Any] = {
        "method": "pitch_class_chromagram_cosine_and_sliding_window",
        "disclaimer": (
            "Heuristic tonal/pitch-class alignment against MIDI motif; indicates harmonic "
            "and pitch-center kinship, not proof of note-for-note melodic transcription."
        ),
        "motifs": {},
    }
    if float(np.sum(global_chroma)) <= 1e-9:
        return out

    for motif_id, mid_path in motif_midis.items():
        m_chroma, _ = midi_file_to_chroma(mid_path)
        if float(np.sum(m_chroma)) <= 1e-9:
            continue
        direct = cosine_sim(global_chroma, m_chroma)
        best_shift = 0
        best_trans = direct
        for shift in range(12):
            c = cosine_sim(global_chroma, np.roll(m_chroma, shift))
            if c > best_trans:
                best_trans = c
                best_shift = shift

        # Check sliding 4-frame windows for passages where the motif shines cleanly.
        peak_window = direct
        if len(frame_chromas) >= 3:
            win_len = min(4, len(frame_chromas))
            for i in range(0, len(frame_chromas) - win_len + 1):
                w_mean = np.mean(frame_chromas[i : i + win_len], axis=0)
                c = cosine_sim(w_mean, m_chroma)
                if c > peak_window:
                    peak_window = c

        signed_shift = best_shift if best_shift <= 6 else best_shift - 12
        out["motifs"][motif_id] = {
            "exact_key_chroma_cosine": round(direct, 3),
            "peak_passage_chroma_cosine": round(peak_window, 3),
            "best_transposed_chroma_cosine": round(best_trans, 3),
            "best_shift_semitones": int(signed_shift),
        }
    return out


def estimate_bpm(samples: np.ndarray, sr: int = 44100, target_bpm: Optional[float] = None) -> Dict[str, Any]:
    """Estimate tempo via spectral-flux onset autocorrelation across the central section."""
    if len(samples) < sr * 4:
        return {"estimated_bpm": None, "confidence": 0.0}

    mono = np.mean(samples, axis=1) if samples.ndim == 2 else samples
    # Analyze up to 45s from near start for steady beat detection.
    max_len = min(len(mono), sr * 45)
    seg = mono[:max_len]

    hop = 512
    win = 2048
    n_frames = max(0, (len(seg) - win) // hop)
    if n_frames < 40:
        return {"estimated_bpm": None, "confidence": 0.0}

    hann = np.hanning(win)
    prev_mag: Optional[np.ndarray] = None
    flux = np.zeros(n_frames, dtype=np.float64)
    for idx in range(n_frames):
        s = idx * hop
        mag = np.abs(np.fft.rfft(seg[s : s + win] * hann))
        if prev_mag is not None:
            diff = mag - prev_mag
            flux[idx] = float(np.sum(np.maximum(diff, 0.0)))
        prev_mag = mag

    flux = flux - np.mean(flux)
    std = float(np.std(flux))
    if std <= 1e-9:
        return {"estimated_bpm": None, "confidence": 0.0}
    flux /= std

    env_sr = float(sr) / float(hop)
    min_lag = max(1, int(round(env_sr * 60.0 / 175.0)))
    max_lag = min(len(flux) // 2, int(round(env_sr * 60.0 / 55.0)))
    if max_lag <= min_lag:
        return {"estimated_bpm": None, "confidence": 0.0}

    ac = np.correlate(flux, flux, mode="full")[len(flux) - 1 :]
    ac /= max(float(ac[0]), 1e-9)
    lags = np.arange(min_lag, max_lag + 1)
    scores = ac[lags]
    best_idx = int(np.argmax(scores))
    raw_bpm = 60.0 * env_sr / float(lags[best_idx])
    conf = float(scores[best_idx])

    adjusted_bpm = raw_bpm
    if target_bpm and target_bpm > 0:
        candidates = [raw_bpm * 0.5, raw_bpm, raw_bpm * 2.0]
        adjusted_bpm = min(candidates, key=lambda b: abs(b - target_bpm))

    delta = (adjusted_bpm - target_bpm) if target_bpm else None
    return {
        "estimated_bpm": round(adjusted_bpm, 1),
        "raw_peak_bpm": round(raw_bpm, 1),
        "confidence": round(conf, 3),
        "delta_from_target": round(delta, 1) if delta is not None else None,
    }


def analyze_mix_and_loop(samples: np.ndarray, sr: int = 44100, is_loop: bool = True) -> Dict[str, Any]:
    """Measure stereo field, spectral energy bands, dynamics, and loop/ending boundaries."""
    frames = len(samples)
    if frames == 0:
        return {}

    mono = np.mean(samples, axis=1) if samples.ndim == 2 else samples
    rms = float(np.sqrt(np.mean(mono ** 2)))
    peak = float(np.max(np.abs(samples)))
    rms_db = 20.0 * math.log10(max(rms, 1e-9))
    peak_db = 20.0 * math.log10(max(peak, 1e-9))
    crest = peak_db - rms_db

    # Stereo correlation & Mid/Side width
    corr = 1.0
    width = 0.0
    if samples.ndim == 2 and samples.shape[1] == 2:
        l_ch, r_ch = samples[:, 0], samples[:, 1]
        denom = float(np.linalg.norm(l_ch) * np.linalg.norm(r_ch))
        if denom > 1e-9:
            corr = float(np.dot(l_ch, r_ch) / denom)
        mid = 0.5 * (l_ch + r_ch)
        side = 0.5 * (l_ch - r_ch)
        mid_rms = float(np.sqrt(np.mean(mid ** 2)))
        side_rms = float(np.sqrt(np.mean(side ** 2)))
        width = side_rms / max(mid_rms, 1e-9)

    # 4-band spectral power average across 24 slices
    n_fft = 16384
    band_totals = np.zeros(4, dtype=np.float64)
    if frames >= n_fft:
        freqs = np.fft.rfftfreq(n_fft, d=1.0 / float(sr))
        m_sub = freqs < 100.0
        m_warm = (freqs >= 100.0) & (freqs < 400.0)
        m_mid = (freqs >= 400.0) & (freqs < 4000.0)
        m_air = freqs >= 4000.0
        hann = np.hanning(n_fft)
        step = max(n_fft, (frames - n_fft) // 23)
        for s in range(0, frames - n_fft + 1, step):
            p = np.abs(np.fft.rfft(mono[s : s + n_fft] * hann)) ** 2
            band_totals[0] += float(np.sum(p[m_sub]))
            band_totals[1] += float(np.sum(p[m_warm]))
            band_totals[2] += float(np.sum(p[m_mid]))
            band_totals[3] += float(np.sum(p[m_air]))
    bsum = float(np.sum(band_totals))
    bands = (band_totals / bsum * 100.0) if bsum > 0 else np.zeros(4)

    # Loop boundary & ending energy inspection
    win_2s = min(frames, sr * 2)
    start_rms = float(np.sqrt(np.mean(mono[:win_2s] ** 2)))
    end_rms = float(np.sqrt(np.mean(mono[-win_2s:] ** 2)))
    last_500ms = float(np.sqrt(np.mean(mono[-min(frames, sr // 2) :] ** 2)))
    start_db = 20.0 * math.log10(max(start_rms, 1e-9))
    end_db = 20.0 * math.log10(max(end_rms, 1e-9))
    last_half_sec_db = 20.0 * math.log10(max(last_500ms, 1e-9))

    # Measure contiguous trailing digital silence (<-55 dBFS) in 50ms chunks
    chunk_sz = sr // 20
    dead_chunks = 0
    for pos in range(frames - chunk_sz, max(0, frames - sr * 15), -chunk_sz):
        c_rms = float(np.sqrt(np.mean(mono[pos : pos + chunk_sz] ** 2)))
        if 20.0 * math.log10(max(c_rms, 1e-9)) < -55.0:
            dead_chunks += 1
        else:
            break
    dead_tail_sec = round(dead_chunks * 0.05, 2)

    seam_delta_db = abs(end_db - start_db)
    fade_drop_db = rms_db - last_half_sec_db

    flags: List[str] = []
    if corr < 0.0:
        flags.append("Negative stereo phase correlation (mono cancellation risk)")
    if crest < 5.0:
        flags.append(f"Over-compressed dynamics (crest={crest:.1f} dB)")
    if is_loop:
        if dead_tail_sec > 0.6:
            flags.append(f"Dead silence tail at end ({dead_tail_sec:.2f}s) weakens raw loop")
        if fade_drop_db > 14.0:
            flags.append(f"Ending drops {fade_drop_db:.1f} dB below track RMS (possible fade-out ending)")
        if seam_delta_db > 8.0:
            flags.append(f"Start/end loudness mismatch ({seam_delta_db:.1f} dB)")
    if bands[3] > 28.0:
        flags.append(f"Bright/harsh upper spectrum (>4kHz = {bands[3]:.1f}%)")

    return {
        "crest_factor_db": round(crest, 2),
        "stereo_correlation": round(corr, 3),
        "stereo_width_ratio": round(width, 3),
        "spectral_bands_pct": {
            "sub_lt_100hz": round(float(bands[0]), 1),
            "warmth_100_400hz": round(float(bands[1]), 1),
            "mid_400_4khz": round(float(bands[2]), 1),
            "air_gt_4khz": round(float(bands[3]), 1),
        },
        "first_2s_rms_db": round(start_db, 2),
        "last_2s_rms_db": round(end_db, 2),
        "last_500ms_rms_db": round(last_half_sec_db, 2),
        "dead_tail_seconds": dead_tail_sec,
        "end_fade_drop_db": round(fade_drop_db, 2),
        "loop_seam_delta_db": round(seam_delta_db, 2),
        "warnings": flags,
    }


def score_computational(
    mix: Dict[str, Any],
    tempo: Dict[str, Any],
    motif_cmp: Dict[str, Any],
    entry: Dict[str, Any],
    metrics: Dict[str, Any],
) -> Dict[str, Any]:
    """Compute a transparent 0..100 technical & alignment pre-score from objective metrics only."""
    score = 72.0
    notes: List[str] = []

    dur = float(metrics.get("duration_seconds", 0.0))
    target_dur = float(entry.get("duration_ms", 90000)) / 1000.0
    if dur < max(4.0, target_dur * 0.45):
        score -= 18.0
        notes.append(f"Short duration ({dur:.1f}s vs target {target_dur:.0f}s)")
    elif dur >= target_dur * 0.75:
        score += 4.0

    # Tempo check
    delta_bpm = tempo.get("delta_from_target")
    if delta_bpm is not None:
        if abs(delta_bpm) <= 3.0:
            score += 6.0
            notes.append(f"Tempo locked near target ({tempo['estimated_bpm']} BPM)")
        elif abs(delta_bpm) > 12.0:
            score -= 6.0
            notes.append(f"Tempo drift ({tempo['estimated_bpm']} vs target {entry.get('bpm')})")

    # Motif chroma alignment
    motifs = motif_cmp.get("motifs", {})
    if motifs:
        best_exact = max(m["exact_key_chroma_cosine"] for m in motifs.values())
        best_peak = max(m["peak_passage_chroma_cosine"] for m in motifs.values())
        if best_exact >= 0.85 or best_peak >= 0.90:
            score += 10.0
            notes.append(f"Strong chroma alignment with target motif (exact={best_exact:.2f}, passage={best_peak:.2f})")
        elif best_exact >= 0.72:
            score += 5.0
            notes.append(f"Moderate chroma alignment with target motif ({best_exact:.2f})")
        else:
            score -= 5.0
            notes.append(f"Weak exact-key motif chroma ({best_exact:.2f})")

    # Mix & loop penalties
    warnings = mix.get("warnings", [])
    score -= min(24.0, 6.0 * len(warnings))
    if not warnings:
        score += 6.0
        notes.append("Clean mix & loop boundaries")

    if metrics.get("samples_at_full_scale", 0) > 0:
        score -= 10.0
        notes.append("Full-scale clipping samples detected")

    score = max(0.0, min(100.0, score))
    return {
        "score_type": "computational_metrics_only",
        "score": round(score, 1),
        "highlights": notes,
        "warnings": warnings,
    }


def analyze_audio_candidate(
    path: Path,
    samples: np.ndarray,
    metrics: Dict[str, Any],
    entry: Dict[str, Any],
    motif_midis: Dict[str, Path],
    sr: int = 44100,
) -> Dict[str, Any]:
    """Run complete DSP, key/tempo estimation, motif chroma check, and mix boundary audit."""
    glob_chroma, frame_chromas = compute_audio_chroma(samples, sr=sr)
    keys = estimate_keys(glob_chroma)
    target_bpm = float(entry["bpm"]) if entry.get("bpm") else None
    tempo = estimate_bpm(samples, sr=sr, target_bpm=target_bpm)
    motif_cmp = compare_chroma_to_motifs(glob_chroma, frame_chromas, motif_midis)
    is_loop = bool(entry.get("loop", True))
    mix = analyze_mix_and_loop(samples, sr=sr, is_loop=is_loop)
    comp_score = score_computational(mix, tempo, motif_cmp, entry, metrics)

    return {
        "file": path.name,
        "metrics": metrics,
        "estimated_keys": keys,
        "tempo": tempo,
        "motif_alignment": motif_cmp,
        "mix_and_loop": mix,
        "computational_score": comp_score,
    }
