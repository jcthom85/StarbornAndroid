#!/usr/bin/env python3
"""Rebuild the cold open's containment-door impact from the original single hit.

The shipped cue was one dry impact with a 0.6s decay sitting in a 2.0s file, played under
"SOURCE BEAST CONTAINMENT BREACH. FAR DOOR PRESSURE RISING." One hit does not read as
pressure rising; it reads as a thud.

This builds an accelerating sequence of five impacts inside a large, damped chamber:

- **Accelerating spacing.** The gaps shorten (0.95s -> 0.45s) and the gain climbs. Something
  is testing the door and losing patience. Even spacing reads as machinery, not threat.
- **Per-hit pitch variation.** Each impact is resampled slightly so the repeats do not read
  as one sample pasted five times. The last hit is pitched down hardest, which is what makes
  it land as the heaviest.
- **Schroeder reverb** (four parallel combs into two series allpasses) with a pre-delay and
  high-frequency damping, so the tails overlap and the room sounds big and stone-dead rather
  than bright. The overlap is the point: by the fourth hit the previous tails have not
  finished, which is what builds the sense of pressure.

Source: app/src/main/res/raw/sfx_intro_door_buckle.mp3 (kept as the dry hit)
Output: the same path, rewritten. Requires ffmpeg on PATH.
"""

from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parent.parent
TARGET = ROOT / "app/src/main/res/raw/sfx_intro_door_buckle.mp3"
SR = 44100

# (time in seconds, gain, playback rate). Gaps shorten and gain climbs.
HITS = [
    (0.00, 0.60, 1.00),
    (0.95, 0.70, 0.97),
    (1.75, 0.82, 1.04),
    (2.35, 0.91, 0.99),
    (2.80, 1.00, 0.93),
]
# Long enough to carry the last tail out, short enough to leave no dead air at the end.
TAIL_SECONDS = 0.75
DRY_HIT_SECONDS = 0.75


def read_mp3_mono(path: Path) -> np.ndarray:
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "in.wav"
        subprocess.run(
            ["ffmpeg", "-v", "error", "-y", "-i", str(path), "-ac", "1", "-ar", str(SR), str(wav)],
            check=True,
        )
        with wave.open(str(wav)) as w:
            frames = w.readframes(w.getnframes())
    return np.frombuffer(frames, dtype=np.int16).astype(np.float64) / 32768.0


def comb(x: np.ndarray, delay: int, feedback: float) -> np.ndarray:
    """y[n] = x[n] + feedback * y[n-delay], evaluated a delay-block at a time."""
    y = x.copy()
    for start in range(delay, len(y), delay):
        stop = min(start + delay, len(y))
        y[start:stop] += feedback * y[start - delay : start - delay + (stop - start)]
    return y


def allpass(x: np.ndarray, delay: int, gain: float) -> np.ndarray:
    """y[n] = -gain*x[n] + x[n-delay] + gain*y[n-delay]."""
    y = -gain * x
    y[delay:] += x[:-delay]
    for start in range(delay, len(y), delay):
        stop = min(start + delay, len(y))
        y[start:stop] += gain * y[start - delay : start - delay + (stop - start)]
    return y


def damp(x: np.ndarray, window: int) -> np.ndarray:
    """Cheap high-frequency damping: a short moving average. A big room is not bright."""
    kernel = np.ones(window) / window
    return np.convolve(x, kernel, mode="same")


def reverb(x: np.ndarray, predelay_ms: float = 22.0) -> np.ndarray:
    pre = int(SR * predelay_ms / 1000.0)
    wet = np.concatenate([np.zeros(pre), x])[: len(x)]
    # Mutually prime-ish delays so the comb resonances do not stack into a pitch.
    combs = [(1687, 0.84), (1601, 0.83), (2053, 0.82), (2251, 0.81)]
    out = np.zeros_like(wet)
    for delay, feedback in combs:
        out += comb(wet, delay, feedback)
    out /= len(combs)
    out = damp(out, 9)
    out = allpass(out, 389, 0.7)
    out = allpass(out, 127, 0.7)
    return out


def resample(x: np.ndarray, rate: float) -> np.ndarray:
    if abs(rate - 1.0) < 1e-6:
        return x
    n = int(len(x) / rate)
    return np.interp(np.linspace(0, len(x) - 1, n), np.arange(len(x)), x)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=TARGET)
    parser.add_argument("--out", type=Path, default=TARGET)
    parser.add_argument("--wet", type=float, default=0.58, help="Reverb mix, 0..1")
    args = parser.parse_args()

    if shutil.which("ffmpeg") is None:
        sys.exit("ffmpeg is required and was not found on PATH.")

    audio = read_mp3_mono(args.source)
    hit = audio[: int(SR * DRY_HIT_SECONDS)]
    # Taper the dry hit so a resampled copy cannot click at its tail.
    fade = int(SR * 0.05)
    hit[-fade:] *= np.linspace(1.0, 0.0, fade)

    total = int(SR * (HITS[-1][0] + DRY_HIT_SECONDS + TAIL_SECONDS))
    dry = np.zeros(total)
    for at, gain, rate in HITS:
        shaped = resample(hit, rate) * gain
        start = int(SR * at)
        stop = min(start + len(shaped), total)
        dry[start:stop] += shaped[: stop - start]

    mixed = (1.0 - args.wet) * dry + args.wet * reverb(dry)
    peak = np.abs(mixed).max()
    if peak > 0:
        mixed *= 0.89 / peak

    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "out.wav"
        with wave.open(str(wav), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes((mixed * 32767).astype(np.int16).tobytes())
        args.out.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(
            ["ffmpeg", "-v", "error", "-y", "-i", str(wav), "-codec:a", "libmp3lame",
             "-b:a", "128k", "-ac", "1", str(args.out)],
            check=True,
        )

    print(f"wrote {args.out.relative_to(ROOT)}: {len(HITS)} impacts, "
          f"{total / SR:.2f}s, {args.out.stat().st_size / 1024:.0f} KB")


if __name__ == "__main__":
    main()
