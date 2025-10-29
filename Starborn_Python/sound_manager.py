# sound_manager.py
from __future__ import annotations

import random
from pathlib import Path
from typing import Dict, List, Optional, Any

import pygame

try:
    # Prefer shared helpers if running inside the project
    from tools.data_core import detect_project_root, json_load
except Exception:
    # Minimal fallbacks so this module works standalone
    def detect_project_root(start: Optional[Path] = None) -> Path:
        p = Path(start or ".").resolve()
        for _ in range(8):
            if (p / "sfx.json").exists():
                return p
            if (p / ".git").exists():
                return p
            p = p.parent
        return Path(".").resolve()

    def json_load(path: Path, default=None):
        import json
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            return default


BUS_NAMES = ["Master", "Music", "SFX", "UI", "Ambience", "Voice"]
DEFAULT_BUS = "SFX"

DEFAULT_META = {
    "bus": DEFAULT_BUS,
    "category": "",
    "volume": 1.0,
    "loop": False,
    "fade_in_ms": 0,
    "fade_out_ms": 0,
    "vol_jitter": 0.0,
    "pan": 0.0,
}


class SoundManager:
    """
    Unified runtime audio service.

    Backward-compatible:
      • Uses sfx.json for tag -> file(s)
      • Optionally reads sfx_meta.json for per-tag defaults (made by tools/sound_editor.py)
    """

    def __init__(self, project_root: Optional[Path] = None, mixer_ready: bool = False):
        self.root = detect_project_root(project_root)
        self.sfx_path = self.root / "sfx.json"
        self.meta_path = self.root / "sfx_meta.json"
        self.sfx_dir = self.root / "sfx"

        # Lazy-init pygame mixer if caller hasn’t done so
        if not mixer_ready and not pygame.mixer.get_init():
            pygame.mixer.init(frequency=44100, size=-16, channels=2)

        self._sfx: Dict[str, List[str]] = {}
        self._meta: Dict[str, Dict[str, Any]] = {}

        # Simple bus gains (0..1). Extend/serialize later if needed.
        self._bus: Dict[str, float] = {name: 1.0 for name in BUS_NAMES}

        # Dedicated ambience channel handle
        self._ambience_channel: Optional[pygame.mixer.Channel] = None

        self.reload()

    # -------- Loading / config --------
    def reload(self):
        self._load_sfx()
        self._load_meta()

    def _load_sfx(self):
        raw = json_load(self.sfx_path, default={}) or {}
        self._sfx.clear()
        for tag, v in raw.items():
            if isinstance(v, str):
                self._sfx[tag] = [v]
            elif isinstance(v, list):
                self._sfx[tag] = [str(x) for x in v if isinstance(x, (str,))]
        # ok if empty

    def _load_meta(self):
        raw = json_load(self.meta_path, default={}) or {}
        self._meta.clear()
        for tag in self._sfx.keys():
            m = dict(DEFAULT_META)
            m.update({k: v for k, v in (raw.get(tag) or {}).items() if k in DEFAULT_META})
            # clamp/sanity
            m["volume"] = float(max(0.0, min(1.0, m.get("volume", 1.0))))
            m["vol_jitter"] = float(max(0.0, min(0.9, m.get("vol_jitter", 0.0))))
            m["pan"] = float(max(-1.0, min(1.0, m.get("pan", 0.0))))
            m["loop"] = bool(m.get("loop", False))
            m["fade_in_ms"] = int(m.get("fade_in_ms", 0))
            m["fade_out_ms"] = int(m.get("fade_out_ms", 0))
            bus = m.get("bus", DEFAULT_BUS)
            m["bus"] = bus if bus in BUS_NAMES else DEFAULT_BUS
            self._meta[tag] = m

    # -------- Bus mixing --------
    def set_bus_volume(self, bus: str, gain: float):
        if bus in self._bus:
            self._bus[bus] = max(0.0, min(1.0, float(gain)))

    def get_bus_volume(self, bus: str) -> float:
        return self._bus.get(bus, 1.0)

    # -------- Resolve files --------
    def _resolve_file(self, tag_or_file: str) -> Optional[Path]:
        """
        If argument is a tag found in sfx.json -> pick a random file.
        Otherwise treat it as a filename (relative to sfx/ or absolute).
        """
        if tag_or_file in self._sfx:
            files = self._sfx[tag_or_file]
            if not files:
                return None
            choice = random.choice(files)
            p = (self.sfx_dir / choice)
            return p if p.exists() else None

        # As filename
        p = Path(tag_or_file)
        if not p.is_absolute():
            p = self.sfx_dir / tag_or_file
        return p if p.exists() else None

    # -------- SFX / one-shots --------
    def play_sfx(
        self,
        tag_or_file: str,
        *,
        volume: Optional[float] = None,
        loop: Optional[bool] = None,
        fade_in_ms: Optional[int] = None,
        pan: Optional[float] = None,
        bus: Optional[str] = None,
    ) -> Optional[pygame.mixer.Channel]:
        """
        Play a short one-shot or a looped effect on a free channel.
        Honors per-tag meta if available; explicit args override meta.
        """
        path = self._resolve_file(tag_or_file)
        if not path:
            return None

        meta = self._meta.get(tag_or_file, DEFAULT_META)

        # base volume with jitter
        base_vol = float(meta.get("volume", 1.0))
        jitter = float(meta.get("vol_jitter", 0.0))
        if jitter > 0.0:
            base_vol *= random.uniform(max(0.0, 1.0 - jitter), 1.0 + jitter)

        b = (bus or meta.get("bus", DEFAULT_BUS))
        bus_vol = self.get_bus_volume(b)
        master = self.get_bus_volume("Master")
        eff_vol = base_vol * bus_vol * master

        if volume is not None:
            eff_vol = float(volume) * bus_vol * master

        loop_ct = -1 if (loop if loop is not None else bool(meta.get("loop", False))) else 0
        fin = int(fade_in_ms if fade_in_ms is not None else meta.get("fade_in_ms", 0))
        p = float(pan if pan is not None else meta.get("pan", 0.0))

        try:
            snd = pygame.mixer.Sound(str(path))
            ch = snd.play(loops=loop_ct, fade_ms=fin)
            if ch is None:
                return None
            self._apply_pan_and_volume(ch, p, eff_vol)
            return ch
        except Exception:
            return None

    # -------- Ambience --------
    def play_ambience(self, tag_or_file: str, *, fade_in_ms: int = 300):
        """Looping ambience on a dedicated channel."""
        path = self._resolve_file(tag_or_file)
        if not path:
            return
        meta = self._meta.get(tag_or_file, DEFAULT_META)
        vol = float(meta.get("volume", 1.0))
        b = meta.get("bus", "Ambience")
        eff = vol * self.get_bus_volume(b) * self.get_bus_volume("Master")
        pan = float(meta.get("pan", 0.0))
        try:
            snd = pygame.mixer.Sound(str(path))
            ch = pygame.mixer.find_channel()
            if ch is None:
                return
            self._ambience_channel = ch
            ch.play(snd, loops=-1, fade_ms=fade_in_ms)
            self._apply_pan_and_volume(ch, pan, eff)
        except Exception:
            pass

    def stop_ambience(self, *, fade_out_ms: int = 200):
        if self._ambience_channel:
            try:
                self._ambience_channel.fadeout(fade_out_ms)
            except Exception:
                pass
            self._ambience_channel = None

    # -------- Music (mixer.music) --------
    def play_music(self, tag_or_file: str, *, loop: bool = True, fade_in_ms: int = 500):
        """Start music using pygame.mixer.music."""
        path = self._resolve_file(tag_or_file)
        if not path:
            return
        try:
            pygame.mixer.music.load(str(path))
            pygame.mixer.music.set_volume(self.get_bus_volume("Music") * self.get_bus_volume("Master"))
            pygame.mixer.music.play(-1 if loop else 0, fade_ms=fade_in_ms)
        except Exception:
            pass

    def stop_music(self, *, fade_out_ms: int = 500):
        try:
            pygame.mixer.music.fadeout(fade_out_ms)
        except Exception:
            pass

    def crossfade_music(self, tag_or_file: str, ms: int = 800):
        """Soft crossfade: fade out current, then fade in next."""
        self.stop_music(fade_out_ms=ms)
        self.play_music(tag_or_file, loop=True, fade_in_ms=ms)

    # -------- Helpers --------
    @staticmethod
    def _apply_pan_and_volume(ch: pygame.mixer.Channel, pan: float, vol: float):
        # pan -1..1 => (L, R)
        pan = max(-1.0, min(1.0, float(pan)))
        left = vol * (1.0 - max(0.0, pan))
        right = vol * (1.0 + min(0.0, pan))
        left = max(0.0, min(1.0, left))
        right = max(0.0, min(1.0, right))
        try:
            ch.set_volume(left, right)
        except TypeError:
            ch.set_volume(vol)
