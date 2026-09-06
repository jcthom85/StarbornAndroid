#!/usr/bin/env python3
"""Rebuild cold open audio assets with full punch, cavern reverberation, and visceral glass strike.

Addresses user feedback:
1. "when i asked you to add reverb to the opening pounding sound, you just made it softer."
   - Fix: Keep 100% dry impact level (no attenuation of transients). Add high-density
     parallel cavern impulse response reverb (RT60 ~ 2.0s) without phase cancellation.
   - Master to True Peak -0.1 dB with high RMS across all hits.
2. "the final sound played (the source beast smacking the glass after orion gets in the stasis pod, is not audible."
   - Fix: Rebuild sfx_intro_beast_strike with sharp, high-presence brittle glass spiderweb
     fractures (1.5kHz - 8kHz) + massive concussive body punch (80 - 250Hz) + monster screech
     + resonant pod shudder.
   - Master to True Peak -0.1 dB with high loudness across all audible frequencies.
"""

from __future__ import annotations

import os
import re
import subprocess
import tempfile
import wave
from pathlib import Path
import numpy as np

ROOT = Path(__file__).resolve().parent.parent
RAW_DIR = ROOT / "app/src/main/res/raw"
SR = 44100

def read_wav_stereo(path: Path | str, target_sr: int = 44100) -> np.ndarray:
    with tempfile.TemporaryDirectory() as tmp:
        wpath = os.path.join(tmp, "conv.wav")
        subprocess.run(
            ["ffmpeg", "-y", "-i", str(path), "-ac", "2", "-ar", str(target_sr), wpath],
            check=True, capture_output=True
        )
        with wave.open(wpath) as w:
            frames = w.readframes(w.getnframes())
            data = np.frombuffer(frames, dtype=np.int16).astype(np.float64) / 32768.0
            return data.reshape(-1, 2)

def pad_or_cut(arr: np.ndarray, n: int) -> np.ndarray:
    if len(arr) >= n:
        return arr[:n]
    res = np.zeros((n, arr.shape[1]))
    res[:len(arr)] = arr
    return res

def resample_stereo(x: np.ndarray, rate: float) -> np.ndarray:
    if abs(rate - 1.0) < 1e-6:
        return x
    n = int(len(x) / rate)
    t_old = np.arange(len(x))
    t_new = np.linspace(0, len(x) - 1, n)
    out = np.zeros((n, x.shape[1]))
    for ch in range(x.shape[1]):
        out[:, ch] = np.interp(t_new, t_old, x[:, ch])
    return out

def generate_cavern_ir(sr: int = 44100, duration: float = 2.0) -> np.ndarray:
    """Generates a high-density, diffuse stereo impulse response of an underground bunker."""
    t = np.linspace(0, duration, int(sr * duration), endpoint=False)
    rng = np.random.RandomState(1337)
    noise_l = rng.randn(len(t))
    noise_r = rng.randn(len(t))
    
    # Exponential decay RT60 ~ 2.0s
    env = np.exp(-t / 0.32)
    ir_l = noise_l * env
    ir_r = noise_r * env
    
    # Discrete early reflections
    early_t = [0.012, 0.024, 0.040, 0.062, 0.088, 0.120, 0.160]
    early_g = [0.75,  0.62,  0.50,  0.40,  0.32,  0.25,  0.18]
    for i, (et, eg) in enumerate(zip(early_t, early_g)):
        idx = int(et * sr)
        if idx < len(t):
            pan = 0.35 if i % 2 == 0 else 0.65
            ir_l[idx] += eg * (1.0 - pan)
            ir_r[idx] += eg * pan
            
    # Gentle high-frequency damping simulating air/concrete absorption
    kernel = np.array([0.18, 0.64, 0.18])
    ir_l = np.convolve(ir_l, kernel, mode="same")
    ir_r = np.convolve(ir_r, kernel, mode="same")
    
    ir = np.column_stack([ir_l, ir_r])
    ir /= np.abs(ir).max()
    return ir

def fft_convolve2d(x: np.ndarray, ir: np.ndarray) -> np.ndarray:
    n = len(x) + len(ir) - 1
    n_fft = 1 << (n - 1).bit_length()
    out = np.zeros((n, 2))
    for ch in range(2):
        X = np.fft.rfft(x[:, ch], n_fft)
        H = np.fft.rfft(ir[:, ch], n_fft)
        out[:, ch] = np.fft.irfft(X * H, n_fft)[:n]
    return out

def save_mastered_mp3(data: np.ndarray, out_path: Path, ffmpeg_filter: str | None = None) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        wpath = Path(tmp) / "pre.wav"
        with wave.open(str(wpath), "wb") as w:
            w.setnchannels(2)
            w.setsampwidth(2)
            w.setframerate(SR)
            peak = np.abs(data).max()
            norm = data * (0.95 / max(peak, 1e-6))
            w.writeframes((norm * 32767).astype(np.int16).tobytes())
            
        mpath = Path(tmp) / "mastered.wav"
        flt = ffmpeg_filter or "alimiter=limit=0.99:attack=5:release=50:asc=1"
        subprocess.run(["ffmpeg", "-y", "-i", str(wpath), "-af", flt, str(mpath)], check=True, capture_output=True)
        
        out_path.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run([
            "ffmpeg", "-y", "-i", str(mpath),
            "-codec:a", "libmp3lame", "-b:a", "192k", "-ac", "2", str(out_path)
        ], check=True, capture_output=True)
    print(f"Mastered and saved: {out_path.name}")

def rebuild_door_buckle() -> None:
    print("\n--- Rebuilding sfx_intro_door_buckle.mp3 ---")
    # Load original dry impact
    dry_src = ROOT / "scratch_buckle_old.mp3"
    dry_data = read_wav_stereo(dry_src)
    hit = dry_data[:int(SR * 0.75)].copy()
    fade_len = int(SR * 0.08)
    hit[-fade_len:] *= np.linspace(1.0, 0.0, fade_len)[:, None]
    
    HITS = [
        (0.00, 0.74, 1.00),
        (0.85, 0.82, 0.98),
        (1.60, 0.90, 1.02),
        (2.20, 0.96, 0.99),
        (2.70, 1.00, 0.94),
    ]
    
    total_len = int(SR * 3.4)
    dry_track = np.zeros((total_len, 2))
    for at, gain, rate in HITS:
        shaped = resample_stereo(hit, rate) * gain
        start = int(SR * at)
        stop = min(start + len(shaped), total_len)
        dry_track[start:stop] += shaped[:stop - start]
        
    ir = generate_cavern_ir(SR, duration=1.8)
    wet = fft_convolve2d(dry_track, ir)[:total_len]
    
    # Full dry transient punch preserved at 1.0 + parallel wet reverberation at 0.40
    mixed = dry_track * 1.0 + wet * 0.40
    
    out_file = RAW_DIR / "sfx_intro_door_buckle.mp3"
    save_mastered_mp3(mixed, out_file, "compand=attacks=0.002:decays=0.08:points=-80/-80|-30/-18|-15/-8|0/-0.1:gain=2,alimiter=limit=0.99:attack=4:release=45:asc=1")

def rebuild_door_collapse() -> None:
    print("\n--- Rebuilding sfx_intro_door_collapse.mp3 ---")
    collapse_src = ROOT / "scratch_collapse_old.mp3"
    dry = read_wav_stereo(collapse_src)
    total_len = int(SR * 3.4)
    dry = pad_or_cut(dry, total_len)
    
    ir = generate_cavern_ir(SR, duration=2.0)
    wet = fft_convolve2d(dry, ir)[:total_len]
    
    mixed = dry * 1.0 + wet * 0.35
    out_file = RAW_DIR / "sfx_intro_door_collapse.mp3"
    save_mastered_mp3(mixed, out_file, "alimiter=limit=0.99:attack=5:release=50:asc=1")

def rebuild_beast_strike() -> None:
    print("\n--- Rebuilding sfx_intro_beast_strike.mp3 ---")
    total_len = int(SR * 3.4)
    t = np.linspace(0, 3.4, total_len, endpoint=False)
    
    beast_body = pad_or_cut(read_wav_stereo(ROOT / "scratch_beast_old.mp3"), total_len)
    crash = pad_or_cut(read_wav_stereo(RAW_DIR / "sfx_crash_impact.mp3"), total_len)
    screech = pad_or_cut(read_wav_stereo(RAW_DIR / "sfx_combat_enemy_screech.mp3"), total_len)
    
    # 1. Visceral, brittle glass strike transient (1kHz - 8kHz)
    rng = np.random.RandomState(42)
    white_noise = rng.randn(total_len)
    
    crack_env = np.exp(-t / 0.035) * (1.0 - np.exp(-t / 0.0003))
    crack_env += 0.85 * np.exp(-np.maximum(0, t - 0.012) / 0.022) * (t >= 0.012)
    crack_env += 0.70 * np.exp(-np.maximum(0, t - 0.028) / 0.028) * (t >= 0.028)
    crack_env += 0.55 * np.exp(-np.maximum(0, t - 0.055) / 0.035) * (t >= 0.055)
    crack_env += 0.40 * np.exp(-np.maximum(0, t - 0.095) / 0.045) * (t >= 0.095)
    
    glass_modes = [1650, 2180, 2850, 3720, 4850, 6100, 7800]
    glass_ring = np.zeros(total_len)
    for i, f in enumerate(glass_modes):
        decay = 0.32 / (1.0 + i * 0.2)
        env = np.exp(-t / decay) * (1.0 - np.exp(-t / 0.0005))
        glass_ring += np.sin(2 * np.pi * f * t) * env * (1.0 / (1.0 + 0.15 * i))
        
    fracture_hp = np.diff(white_noise * crack_env, prepend=0) * 2.5
    glass_stereo = np.column_stack([
        fracture_hp * 0.95 + glass_ring * 0.42,
        fracture_hp * 0.90 - glass_ring * 0.40
    ])
    
    # 2. Concussive physical body punch
    punch_env = np.exp(-t / 0.8) * (1.0 - np.exp(-t / 0.002))
    punch_layer = (beast_body * 2.2 + crash * 1.6) * punch_env[:, None]
    
    # 3. Terrifying close-up monster screech
    screech_env = np.exp(-t / 0.75) * (1.0 - np.exp(-t / 0.01))
    screech_layer = screech * screech_env[:, None] * 1.1
    
    # 4. Low-frequency stasis pod structural shudder
    pod_shudder = np.sin(2 * np.pi * (160.0 * np.exp(-t / 0.2) + 50.0) * t) * np.exp(-t / 0.7)
    pod_layer = np.column_stack([pod_shudder, pod_shudder]) * 0.8
    
    combined = glass_stereo * 1.6 + punch_layer + screech_layer + pod_layer
    
    out_file = RAW_DIR / "sfx_intro_beast_strike.mp3"
    save_mastered_mp3(
        combined, out_file,
        "compand=attacks=0.002:decays=0.08:points=-80/-80|-35/-18|-15/-6|0/-0.1:gain=4,alimiter=limit=0.99:attack=3:release=40:asc=1"
    )

if __name__ == "__main__":
    rebuild_door_buckle()
    rebuild_door_collapse()
    rebuild_beast_strike()
    print("\nAll cold open audio assets successfully rebuilt.")
