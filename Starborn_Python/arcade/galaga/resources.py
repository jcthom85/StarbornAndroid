"""On-demand generation of bespoke audio cues for the Galaga mini game."""
import math
import os
import wave
from array import array
from typing import Iterable, Sequence

ASSET_ROOT = os.path.join(os.path.dirname(__file__), "assets")
AUDIO_ROOT = os.path.join(ASSET_ROOT, "audio")

# Slight detune values keep the sounds feeling handmade.
_AUDIO_RECIPES = {
    "laser.wav": {"partials": (880, 1320), "duration": 0.18, "volume": 0.55, "sweep": -420},
    "explosion.wav": {"partials": (110, 220, 440), "duration": 0.6, "volume": 0.9, "noise": 0.35},
    "tractor.wav": {"partials": (360, 720), "duration": 1.8, "volume": 0.5, "sweep": -160, "vibrato": 8},
    "bonus.wav": {"partials": (640, 960, 1280), "duration": 0.9, "volume": 0.6, "sweep": 220},
    "impact.wav": {"partials": (260, 420), "duration": 0.35, "volume": 0.7, "noise": 0.2},
}

SAMPLE_RATE = 44100


def _render_wave(path: str, samples: Sequence[float]) -> None:
    """Write mono 16-bit PCM samples."""
    buf = array("h")
    for sample in samples:
        sample = max(-1.0, min(1.0, sample))
        buf.append(int(sample * 32767))
    with wave.open(path, "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(SAMPLE_RATE)
        wav.writeframes(buf.tobytes())


def _synth_tone(recipe: dict) -> Iterable[float]:
    duration = recipe.get("duration", 0.5)
    volume = recipe.get("volume", 0.6)
    partials = recipe.get("partials", (440,))
    noise = recipe.get("noise", 0.0)
    sweep = recipe.get("sweep", 0.0)
    vibrato = recipe.get("vibrato", 0.0)

    total_samples = int(SAMPLE_RATE * duration)
    for n in range(total_samples):
        t = n / SAMPLE_RATE
        tone = 0.0
        for idx, f in enumerate(partials):
            sweep_ratio = 1.0 + (sweep * (1 - t / duration) / max(f, 1))
            hz = f * sweep_ratio
            phase = 2 * math.pi * hz * t
            if vibrato:
                phase += math.sin(2 * math.pi * vibrato * t) * 0.35 * (idx + 1)
            tone += math.sin(phase)
        tone /= max(len(partials), 1)
        if noise:
            tone += noise * (2 * (hash((n, len(partials))) % 2) - 1)
        env = _envelope(t, duration)
        yield tone * volume * env


def _envelope(t: float, duration: float) -> float:
    attack = min(0.03, duration * 0.2)
    release = min(0.12, duration * 0.4)
    if t < attack:
        return t / max(attack, 1e-6)
    if t > duration - release:
        remaining = duration - t
        return max(0.0, remaining / max(release, 1e-6))
    return 1.0


def ensure_galaga_assets(root: str | None = None) -> dict:
    """Generate audio assets on demand and return their file paths."""
    base_root = root or ASSET_ROOT
    audio_root = os.path.join(base_root, "audio")
    os.makedirs(audio_root, exist_ok=True)
    paths = {}
    for filename, recipe in _AUDIO_RECIPES.items():
        path = os.path.join(audio_root, filename)
        if not os.path.exists(path):
            samples = list(_synth_tone(recipe))
            _render_wave(path, samples)
        paths[filename[:-4]] = path
    return {"audio": paths, "root": base_root}

