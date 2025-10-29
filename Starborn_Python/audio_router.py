# audio_router.py
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable, Dict, List, Any, Optional

from sound_manager import SoundManager

# Prefer shared helpers when running inside the project, but be resilient
try:
    from tools.data_core import json_load, json_save, detect_project_root
except Exception:
    import json

    def json_load(path: Path, default=None):
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            return default

    def json_save(path: Path, data, sort_obj: bool = True, indent: int = 2):
        import json
        text = json.dumps(data, sort_keys=sort_obj, indent=indent)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")

    def detect_project_root(start: Optional[Path] = None) -> Path:
        p = Path(start or ".").resolve()
        for _ in range(8):
            if (p / "sfx.json").exists() or (p / ".git").exists():
                return p
            p = p.parent
        return Path(".").resolve()


# -------------------------------------------------------------------
# Minimal, game-wide event bus just for audio
# -------------------------------------------------------------------
class EventBus:
    def __init__(self):
        self._subs: Dict[str, List[Callable[[Dict[str, Any]], None]]] = {}

    def subscribe(self, topic: str, fn: Callable[[Dict[str, Any]], None]):
        self._subs.setdefault(topic, []).append(fn)

    def emit(self, topic: str, payload: Optional[Dict[str, Any]] = None):
        for fn in list(self._subs.get(topic, [])):
            try:
                fn(payload or {})
            except Exception:
                # Never let audio events crash the game
                pass


# Global bus instance you can import anywhere
AudioEvents = EventBus()


# -------------------------------------------------------------------
# AudioRouter: translates simple game events into sound/music/ambience
# -------------------------------------------------------------------
@dataclass
class AudioRouter:
    sm: SoundManager
    project_root: Optional[Path] = None

    root: Path = field(init=False)
    bindings_path: Path = field(init=False)
    _bindings: Dict[str, Any] = field(init=False, default_factory=dict)

    def __post_init__(self):
        self.root = detect_project_root(self.project_root)
        self.bindings_path = self.root / "audio_bindings.json"
        self._bindings = self._load_bindings()

        # Wire default topics (extend anytime)
        # UI
        AudioEvents.subscribe("ui.click", self._ui_click)
        AudioEvents.subscribe("ui.confirm", self._ui_confirm)
        AudioEvents.subscribe("ui.error", self._ui_error)
        AudioEvents.subscribe("ui.back", self._ui_back)

        # Battle flow
        AudioEvents.subscribe("battle.start", self._battle_start)
        AudioEvents.subscribe("battle.victory", self._battle_victory)
        AudioEvents.subscribe("battle.defeat", self._battle_defeat)

        # World/rooms
        AudioEvents.subscribe("room.enter", self._room_enter)

        # Generic SFX passthrough
        AudioEvents.subscribe("sfx.play", self._sfx_play)

    # ------------------ Bindings I/O ------------------
    def _default_bindings(self) -> Dict[str, Any]:
        # Kept tiny on purpose; you’ll author real data in step 3/6
        return {
            "music": {},     # { "hub_id": "music_tag" }
            "ambience": {},  # { "room_id": "amb_tag" }
            "ui": {          # sane defaults you can override in the JSON
                "click": "ui_click",
                "confirm": "ui_confirm",
                "error": "ui_error",
                "back": "ui_back"
            },
            "battle": {
                "start": "battle_start",
                "victory": "victory_fanfare",
                "defeat": "defeat_sting"
            }
        }

    def _load_bindings(self) -> Dict[str, Any]:
        data = json_load(self.bindings_path, default=None)
        if not isinstance(data, dict):
            # Don’t write a file yet—step 3/6 will add the JSON.
            return self._default_bindings()
        # Fill any missing groups with defaults
        defaults = self._default_bindings()
        out = {}
        for k in defaults.keys():
            v = data.get(k, defaults[k])
            out[k] = v if isinstance(v, dict) else defaults[k]
        return out

    def reload_bindings(self):
        """Call if you edit audio_bindings.json at runtime and want to refresh."""
        self._bindings = self._load_bindings()

    # ------------------ Topic handlers ------------------
    # UI
    def _ui_click(self, _):
        tag = self._bindings.get("ui", {}).get("click")
        if tag: self.sm.play_sfx(tag, bus="UI")

    def _ui_confirm(self, _):
        tag = self._bindings.get("ui", {}).get("confirm")
        if tag: self.sm.play_sfx(tag, bus="UI")

    def _ui_error(self, _):
        tag = self._bindings.get("ui", {}).get("error")
        if tag: self.sm.play_sfx(tag, bus="UI")

    def _ui_back(self, _):
        tag = self._bindings.get("ui", {}).get("back")
        if tag: self.sm.play_sfx(tag, bus="UI")

    # Battle
    def _battle_start(self, _):
        tag = self._bindings.get("battle", {}).get("start")
        if tag: self.sm.play_sfx(tag)

    def _battle_victory(self, _):
        self.sm.stop_music(fade_out_ms=400)
        tag = self._bindings.get("battle", {}).get("victory")
        if tag: self.sm.play_sfx(tag)

    def _battle_defeat(self, _):
        tag = self._bindings.get("battle", {}).get("defeat")
        if tag: self.sm.play_sfx(tag)

    # World/rooms
    def _room_enter(self, payload: Dict[str, Any]):
        """
        Expected payload: {'hub_id': str, 'room_id': str}
        - Crossfades music by hub_id (if mapping exists)
        - Starts/stops ambience by room_id (if mapping exists)
        """
        hub_id = (payload or {}).get("hub_id")
        room_id = (payload or {}).get("room_id")

        # Music by hub
        if hub_id is not None:
            music_tag = self._bindings.get("music", {}).get(str(hub_id))
            if music_tag:
                self.sm.crossfade_music(music_tag, ms=800)

        # Ambience by room
        if room_id is not None:
            amb_tag = self._bindings.get("ambience", {}).get(str(room_id))
            if amb_tag:
                self.sm.play_ambience(amb_tag, fade_in_ms=300)
            else:
                self.sm.stop_ambience(fade_out_ms=200)

    # Generic passthrough: AudioEvents.emit("sfx.play", {"tag": "hit", ...})
    def _sfx_play(self, payload: Dict[str, Any]):
        if not payload:
            return
        tag = payload.get("tag")
        if not tag:
            return
        self.sm.play_sfx(
            tag,
            volume=payload.get("volume"),
            loop=payload.get("loop"),
            fade_in_ms=payload.get("fade_in_ms"),
            pan=payload.get("pan"),
            bus=payload.get("bus"),
        )
