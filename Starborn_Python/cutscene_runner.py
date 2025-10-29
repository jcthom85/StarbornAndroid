# cutscene_runner.py — Data-driven cutscene sequencing for Starborn
from __future__ import annotations

import os, json
from typing import Callable, Optional, Dict, Any, List
from kivy.clock import Clock
from kivy.uix.floatlayout import FloatLayout

# External services expected on ui root:
#   - ui.dialogue_box.show_dialogue(speaker, text, on_dismiss=fn)
#   - ui.audio (SoundManager) with stop_music() optionally used here
#   - ui.input_locked (bool)
#   - ui._scatter (kivy.uix.scatter.Scatter) for camera ops
from vfx import VFX

# AUDIO: route cutscene audio via the same event path as the rest of the game
from audio_router import AudioEvents

DEFAULT_JSON_PATH = os.path.join(os.path.dirname(__file__), "cinematics.json")


class CutsceneRunner:
    def __init__(self, ui_root, json_path: str = DEFAULT_JSON_PATH, vfx: VFX | None = None):
        self.ui = ui_root
        self.vfx = vfx or VFX(ui_root)
        # Use provided path (env override removed)
        self.json_path = json_path
        self._scenes: Dict[str, List[Dict[str, Any]]] = {}
        self._labels: Dict[str, int] = {}
        self._current: List[Dict[str, Any]] = []
        self._idx: int = 0
        self._timer_ev = None
        self._waiting_click = False
        self._wait_action_cb = None
        self.on_complete: Optional[Callable] = None
        self.reload()

    # ------------------------------- loading ---------------------------------
    def reload(self):
        try:
            with open(self.json_path, "r", encoding="utf-8") as f:
                self._scenes = json.load(f)
        except Exception as e:
            print(f"[CutsceneRunner] Failed to load '{self.json_path}': {e}")
            self._scenes = {}

    # --------------------------------- play ----------------------------------
    def play(self, scene_id: str, on_complete: Optional[Callable] = None):
        scene = self._scenes.get(scene_id)
        if not scene:
            print(f"[CutsceneRunner] Scene '{scene_id}' not found.")
            if on_complete: on_complete()
            return

        # --- Check for repeatable flag ---
        # The scene itself is a list; the metadata is in a special first step.
        meta = scene[0] if scene and isinstance(scene[0], dict) and scene[0].get("type") == "scene_meta" else {}
        is_repeatable = meta.get("repeatable", False)
        if scene_id in self.ui.played_cutscenes and not is_repeatable:
            if on_complete: on_complete()
            return

        self.on_complete = on_complete
        self._current = scene
        self._idx = 0
        self._waiting_click = False
        self._clear_wait_for_action()
        self.vfx.enter_cinematic()
        self._index_labels()
        # Defer first step one frame so overlay widgets attach and sizes settle
        Clock.schedule_once(lambda *_: self._next(), 0)

    def _index_labels(self):
        """Collect optional label indices for branch targets."""
        self._labels.clear()
        for i, step in enumerate(self._current):
            if step.get("type") == "label" and "id" in step:
                self._labels[step["id"]] = i

    # --------------------------------- flow ----------------------------------
    def _next(self, *_):
        # guard for click waits
        if self._waiting_click:
            return

        if self._idx >= len(self._current):
            self._finish()
            return

        step = self._current[self._idx]
        self._idx += 1
        typ = step.get("type")
        handler = getattr(self, f"_do_{typ}", None)
        if handler:
            handler(step)
        else:
            print(f"[CutsceneRunner] Unknown step type '{typ}', skipping.")
            Clock.schedule_once(self._next, 0)

    def _finish(self):
        self.vfx.set_wait_for_click(False, None)
        self.vfx.letterbox(0, duration=0.2)
        self.vfx.exit_cinematic()
        self._clear_wait_for_action()

        # Mark as played
        scene_id = self._get_current_scene_id()
        if scene_id:
            self.ui.played_cutscenes.add(scene_id)

        if self.on_complete:
            cb, self.on_complete = self.on_complete, None
            cb()

    # ------------------------------- handlers --------------------------------
    def _do_fade_in_exploration(self, step: dict):
        """
        Specialized command to trigger the exploration UI fade-in.
        """
        try:
            duration = float(step.get("duration", 1.0))
            # The transition manager is on the app object
            from kivy.app import App
            app = App.get_running_app()
            if hasattr(app, 'tx_mgr'):
                app.tx_mgr.fade_in_exploration(duration=duration)
        except Exception as e:
            print(f"Error in _do_fade_in_exploration: {e}")
        finally:
            # We must still call _next to continue the cinematic sequence
            # after the fade duration.
            Clock.schedule_once(self._next, float(step.get("duration", 1.0)))

    def _do_set_global_dark(self, step: dict):
        """
        Sets the global screen darkness via the app's main method.
        This is more robust than the VFX fade for full-screen transitions.
        """
        try:
            from kivy.app import App
            app = App.get_running_app()
            if not app: return

            alpha = float(step.get("alpha", 0.0))
            duration = float(step.get("duration", 0.25))
            force = bool(step.get("force", False))
            app._force_black_screen = force
            app.set_global_dark(alpha, animate=(duration > 0), duration=duration)
        finally:
            Clock.schedule_once(self._next, max(0.01, float(step.get("duration", 0.25))))    
    
    def _do_action(self, step):
        action = step.get("action")

        em = getattr(self.ui, "event_manager", None)
        if not em:
            self._next()
            return

        if action == "start_quest":
            quest_id = step.get("quest_id")
            if quest_id:
                em._action_start_quest({"quest_id": quest_id})
        elif action == "complete_quest":
            quest_id = step.get("quest_id")
            if quest_id:
                em._action_complete_quest({"quest_id": quest_id})
        elif action == "give_item_to_player":
            item = step.get("item")
            if item:
                em._action_give_item_to_player({"item": item})
        elif action == "set_milestone":
            milestone = step.get("milestone")
            if milestone:
                em._action_set_milestone({"milestone": milestone})
        elif action == "player_action":
            player_action = step.get("player_action")
            if player_action:
                em.player_action(player_action)
        elif action == "advance_quest_stage":
            quest_id = step.get("quest_id")
            if quest_id:
                em._action_advance_quest_stage({"quest_id": quest_id})
        elif action == "take_item":
            item = step.get("item")
            if item:
                em.dialogue_trigger("take_item", item)
        elif action == "give_credits":
            amount = step.get("amount")
            try:
                amount_int = int(amount)
            except (TypeError, ValueError):
                amount_int = 0
            if amount_int:
                em._action_give_credits({"amount": amount_int})
        elif action == "narrate":
            text = step.get("text")
            if text:
                em._action_narrate({"text": text})
        elif action == "set_quest_task_done":
            quest_id = step.get("quest_id")
            task_id = step.get("task_id")
            if quest_id and task_id:
                em._action_set_quest_task_done({"quest_id": quest_id, "task_id": task_id})
        
        self._next()

    def _do_wait(self, step):
        dur = float(step.get("duration", 0))
        Clock.schedule_once(self._next, max(0.0, dur))

    def _get_current_scene_id(self) -> Optional[str]:
        """Helper to find the ID of the currently playing scene."""
        for scene_id, scene_data in self._scenes.items():
            if scene_data is self._current:
                return scene_id
        return None

    def _do_wait_for_click(self, step):
        self._waiting_click = True
        def _resume():
            if not self._waiting_click:
                return
            self._waiting_click = False
            self.vfx.set_wait_for_click(False, None)
            Clock.schedule_once(self._next, 0)
        self.vfx.set_wait_for_click(True, _resume)

    def _clear_wait_for_action(self):
        if self._wait_action_cb:
            event_mgr = getattr(self.ui, "event_manager", None)
            if event_mgr and hasattr(event_mgr, "unsubscribe"):
                try:
                    event_mgr.unsubscribe("player_action", self._wait_action_cb)
                except Exception:
                    pass
            self._wait_action_cb = None

    def _do_wait_for_player_action(self, step):
        expected_action = (step.get("action") or "").lower().strip()
        expected_item = step.get("item_id")
        if isinstance(expected_item, str):
            expected_item = expected_item.lower().strip()

        event_mgr = getattr(self.ui, "event_manager", None)
        if not event_mgr or not hasattr(event_mgr, "subscribe"):
            Clock.schedule_once(self._next, 0)
            return

        self._clear_wait_for_action()

        def _handler(payload: Dict[str, Any] | None = None):
            data = payload or {}
            action = str(data.get("action", "")).lower().strip()
            if expected_action and action != expected_action:
                return
            item_id = data.get("item_id") or data.get("item")
            if isinstance(item_id, str):
                item_id = item_id.lower().strip()
            if expected_item and item_id != expected_item:
                return
            self._clear_wait_for_action()
            Clock.schedule_once(self._next, 0)

        self._wait_action_cb = _handler
        event_mgr.subscribe("player_action", _handler)

    def _do_dialogue(self, step):
        dialogue_id = step.get("dialogue_id")
        if dialogue_id and hasattr(self.ui, "dialogue_manager"):
            entry = self.ui.dialogue_manager.dialogue_data.get(dialogue_id)
            if entry:
                speaker = entry.get("speaker", "")
                text = entry.get("text", "")
                self.ui.dialogue_box.show_dialogue(speaker, text, on_dismiss=self._next)
                return

        actor = step.get("actor") or ""
        line  = step.get("line") or ""
        if hasattr(self.ui, "dialogue_box"):
            self.ui.dialogue_box.show_dialogue(actor, line, on_dismiss=lambda *_: self._next())
        else:
            print(f"[CutsceneRunner] (no dialogue_box) {actor}: {line}")
            Clock.schedule_once(self._next, 0)

    def _do_fade(self, step):
        alpha = float(step.get("alpha", 1.0))
        duration = float(step.get("duration", 0.5))
        color = tuple(step.get("color", [0,0,0]))
        self.vfx.fade(alpha=alpha, duration=duration, color=color, on_complete=lambda: self._next())

    def _do_letterbox(self, step):
        h = float(step.get("height", 80))
        duration = float(step.get("duration", 0.3))
        self.vfx.letterbox(h, duration, on_complete=lambda: self._next())

    def _do_flash(self, step):
        color = tuple(step.get("color", [1,1,1]))
        duration = float(step.get("duration", 0.15))
        self.vfx.flash(color=color, duration=duration, on_complete=lambda: self._next())

    def _do_color_filter(self, step):
        color = tuple(step.get("color", [0,0,0]))
        alpha = float(step.get("alpha", 1.0))
        duration = float(step.get("duration", 0.5))
        self.vfx.color_filter(color=color, alpha=alpha, duration=duration, on_complete=lambda: self._next())

    def _do_flashback_overlay(self, step):
        alpha = float(step.get("alpha", 0.35))
        duration = float(step.get("duration", 0.4))
        self.vfx.flashback_overlay(alpha=alpha, duration=duration, on_complete=lambda: self._next())

    def _do_screen_shake(self, step):
        mag = float(step.get("magnitude", 10.0))
        dur = float(step.get("duration", 0.35))
        axis = step.get("axis", "both")
        self.vfx.screen_shake(magnitude=mag, duration=dur, axis=axis, on_complete=lambda: self._next())

    def _do_zoom_camera(self, step):
        scale = float(step.get("scale", 1.0))
        duration = float(step.get("duration", 1.0))
        ease = step.get("ease", "in_out_quad")
        pos_hint = step.get("pos_hint")  # [x, y] in 0..1 ui space
        self.vfx.camera_zoom(
            scale=scale, duration=duration, t=ease,
            pos_hint=tuple(pos_hint) if pos_hint else None,
            on_complete=lambda: self._next()
        )

    def _do_pan_camera(self, step):
        dx = float(step.get("x", 0.0))
        dy = float(step.get("y", 0.0))
        duration = float(step.get("duration", 1.0))
        ease = step.get("ease", "in_out_quad")
        self.vfx.camera_pan(dx=dx, dy=dy, duration=duration, t=ease, on_complete=lambda: self._next())

    def _do_particles(self, step):
        # Allow both legacy flat fields and normalized {"config": {...}}
        cfg = step.get("config") if isinstance(step.get("config"), dict) else None
        if cfg is None:
            cfg = dict(step)
            cfg.pop("type", None)
            cfg.pop("config", None)
        self.vfx.particles(cfg, on_complete=lambda: self._next())

    def _do_caption(self, step):
        text = step.get("text", "")
        duration = float(step.get("duration", 1.5))
        # Keep cutscene captions independent of DialogueBox
        self.vfx.caption(text, duration, on_complete=lambda: self._next())

    def _do_narration(self, step):
        """Styled narration panel for cutscenes (no portrait, cinematic look)."""
        text = step.get("text", "")
        duration = float(step.get("duration", 2.0))
        pos = step.get("position") or step.get("pos") or 'bottom'
        width = float(step.get("width", 0.82))
        fade_in = float(step.get("fade_in", 0.18))
        fade_out = float(step.get("fade_out", 0.25))
        self.vfx.narration(text,
                           duration=duration,
                           position=pos,
                           width=width,
                           fade_in=fade_in,
                           fade_out=fade_out,
                           on_complete=lambda: self._next())

    def _do_cinematic_text(self, step):
        """Full-screen stylized text bar with dimmed background."""
        text = step.get("text", "")
        duration = float(step.get("duration", 2.2))
        position = step.get("position", "center")
        width = float(step.get("width", 0.9))
        dim_alpha = float(step.get("dim_alpha", 0.28))
        self.vfx.cinematic_text(
            text,
            duration=duration,
            position=position,
            width=width,
            dim_alpha=dim_alpha,
            on_complete=lambda: self._next(),
        )

    # -------------------------- audio: sound + music --------------------------
    def _do_sound(self, step: Dict[str, Any]):
        """
        Cutscene step: play a sound effect by SFX tag.

        JSON (from editor):
          { "type": "sound", "id": "<sfx_tag>", "volume": 0..1, "fade": <sec>, "pan": -1..1 }

        Notes:
          • 'id' is required. Other fields are optional.
          • Routed via AudioEvents -> AudioRouter -> SoundManager for consistency.
        """
        tag = step.get("id") or step.get("tag")
        if tag:
            payload: Dict[str, Any] = {"tag": tag}
            # optional fields
            v = step.get("volume")
            if v is not None:
                try: payload["volume"] = float(v)
                except Exception: pass
            f = step.get("fade")
            if f is not None:
                try: payload["fade_in_ms"] = int(float(f) * 1000)
                except Exception: pass
            p = step.get("pan")
            if p is not None:
                try: payload["pan"] = float(p)
                except Exception: pass

            AudioEvents.emit("sfx.play", payload)

        # advance immediately (SFX are fire-and-forget)
        Clock.schedule_once(self._next, 0)

    def _do_music(self, step: Dict[str, Any]):
        """
        Cutscene step: control background music.

        JSON (from editor):
          { "type": "music", "id": "<music_tag>", "action": "play"|"stop", "fade": <sec>, "volume": 0..1 }

        Behavior:
          • play  -> emit sfx.play with bus="Music" so it shares metadata/bus with game-wide audio
          • stop  -> call SoundManager.stop_music (if available on ui.audio)
        """
        action = (step.get("action") or "play").lower()
        fade_ms = 0
        try:
            fade_ms = int(float(step.get("fade") or 0.0) * 1000)
        except Exception:
            fade_ms = 0

        tag = step.get("id") or step.get("tag")

        if action == "play" and tag:
            payload: Dict[str, Any] = {"tag": tag, "bus": "Music"}
            v = step.get("volume")
            if v is not None:
                try: payload["volume"] = float(v)
                except Exception: pass
            if fade_ms:
                payload["fade_in_ms"] = fade_ms

            AudioEvents.emit("sfx.play", payload)

        elif action == "stop":
            # Use SoundManager if present (ok if not)
            try:
                if hasattr(self.ui, "audio") and hasattr(self.ui.audio, "stop_music"):
                    self.ui.audio.stop_music(fade_out_ms=fade_ms)
            except Exception:
                pass

        # advance immediately
        Clock.schedule_once(self._next, 0)

    # ----------------------------- branching ops ------------------------------
    def _do_label(self, step):  # no-op, used for goto targets
        Clock.schedule_once(self._next, 0)

    def _do_scene_meta(self, step): # no-op, used for scene metadata
        Clock.schedule_once(self._next, 0)

    def _do_play_scene(self, step: dict):
        """Plays another scene as a subroutine."""
        scene_id = step.get("scene_id")
        if scene_id:
            # We pass the original `on_complete` so it fires when the
            # sub-scene finishes.
            self.play(scene_id, on_complete=self.on_complete)
        else:
            print("[CutsceneRunner] play_scene step is missing 'scene_id'")
            Clock.schedule_once(self._next, 0) # Continue if no ID

    def _do_goto(self, step):
        target = step.get("target")
        if target in self._labels:
            self._idx = self._labels[target] + 1
        else:
            print(f"[CutsceneRunner] goto target '{target}' not found.")
        Clock.schedule_once(self._next, 0)

    # ------------------------------- extra VFX --------------------------------
    def _do_speed_lines(self, step: Dict[str, Any]):
        self.vfx.speed_lines(count=int(step.get('count', 36)),
                             duration=float(step.get('duration', 0.5)),
                             color=tuple(step.get('color', [1,1,1,0.45])),
                             thickness=float(step.get('thickness', 1.2)),
                             on_complete=lambda: self._next())

    def _do_ring(self, step: Dict[str, Any]):
        self.vfx.ring(start_radius=float(step.get('start', 24.0)),
                      end_radius=float(step.get('end', 480.0)),
                      duration=float(step.get('duration', 0.5)),
                      color=tuple(step.get('color', [1,1,1,1])),
                      thickness=float(step.get('thickness', 3.0)),
                      on_complete=lambda: self._next())

    def _do_vignette(self, step: Dict[str, Any]):
        self.vfx.vignette(alpha=float(step.get('alpha', 0.35)),
                          inset=float(step.get('inset', 120.0)),
                          color=tuple(step.get('color', [0,0,0])),
                          duration=float(step.get('duration', 0.8)),
                          fade_in=float(step.get('fade_in', 0.18)),
                          fade_out=float(step.get('fade_out', 0.25)),
                          on_complete=lambda: self._next())

    def _do_bar_wipe(self, step: Dict[str, Any]):
        self.vfx.bar_wipe(orientation=step.get('orientation', 'vertical'),
                          direction=step.get('direction', 'in'),
                          duration=float(step.get('duration', 0.5)),
                          color=tuple(step.get('color', [0,0,0,1])),
                          on_complete=lambda: self._next())

    def _do_tilt_camera(self, step: Dict[str, Any]):
        self.vfx.camera_tilt(angle=float(step.get('angle', 6.0)),
                             duration=float(step.get('duration', 0.25)),
                             restore=bool(step.get('restore', True)),
                             on_complete=lambda: self._next())

    # ------------------------------- new VFX -------------------------------
    def _do_shockwave(self, step: Dict[str, Any]):
        self.vfx.shockwave(duration=float(step.get('duration', 0.4)),
                           start_scale=float(step.get('start_scale', 0.98)),
                           end_scale=float(step.get('end_scale', 1.25)),
                           alpha=float(step.get('alpha', 0.45)),
                           on_complete=lambda: self._next())

    def _do_glitch(self, step: Dict[str, Any]):
        self.vfx.chromatic_glitch(duration=float(step.get('duration', 0.18)),
                                  jitter=float(step.get('jitter', 2.0)),
                                  on_complete=lambda: self._next())

    def _do_weather(self, step: Dict[str, Any]):
        kind = (step.get('kind') or 'rain').lower()
        duration = float(step.get('duration', 2.0))
        intensity = step.get('intensity', 'medium')
        if kind == 'snow':
            self.vfx.weather_snow(duration=duration, intensity=intensity)
        elif kind == 'cave_drip':
            self.vfx.weather_cave_drip(duration=duration, intensity=intensity)
        else:
            self.vfx.weather_rain(duration=duration, intensity=intensity)
        Clock.schedule_once(lambda *_: self._next(), 0)

    def _do_depth_focus(self, step: Dict[str, Any]):
        pos_hint = step.get('center') or step.get('pos')
        center = tuple(pos_hint) if isinstance(pos_hint, (list, tuple)) and len(pos_hint) == 2 else None
        self.vfx.depth_focus(center=center,
                             radius=float(step.get('radius', 120.0)),
                             blur=int(step.get('blur', 4)),
                             dim_alpha=float(step.get('dim_alpha', 0.0)),
                             duration=float(step.get('duration', 0.8)),
                             fade_in=float(step.get('fade_in', 0.15)),
                             fade_out=float(step.get('fade_out', 0.25)),
                             on_complete=lambda: self._next())

    def _do_hit_stop(self, step: Dict[str, Any]):
        self.vfx.hit_stop(duration=float(step.get('duration', 0.12)),
                          zoom=float(step.get('zoom', 1.03)),
                          on_complete=lambda: self._next())
