# cinematics.py — Back-compat shim that exposes CinematicManager while delegating to vfx + cutscene_runner
from __future__ import annotations

from typing import Optional, Callable
from cutscene_runner import CutsceneRunner
from vfx import VFX

class CinematicManager:
    """
    Compatibility wrapper so existing imports keep working.
    Provides two things:
      1) play(scene_id, on_complete) to run JSON-defined cutscenes
      2) direct VFX helpers (flash, screen_shake, fade, letterbox, etc.) you can call programmatically
    """
    def __init__(self, ui_root):
        self.ui = ui_root
        self.vfx = VFX(ui_root)
        self.runner = CutsceneRunner(ui_root, vfx=self.vfx)

    # ---------- cutscene ----------
    def play(self, scene_id: str, on_complete: Optional[Callable] = None):
        self.runner.play(scene_id, on_complete=on_complete)

    def get_scene(self, scene_id: str):
        try:
            scene = self.runner._scenes.get(scene_id)
            if not scene:
                return []
            # Return a shallow copy so callers can mutate safely
            return [dict(step) for step in scene]
        except Exception:
            return []

    # ---------- direct vfx helpers ----------
    def flash(self, *args, **kwargs):            return self.vfx.flash(*args, **kwargs)
    def fade(self, *args, **kwargs):             return self.vfx.fade(*args, **kwargs)
    def letterbox(self, *args, **kwargs):        return self.vfx.letterbox(*args, **kwargs)
    def color_filter(self, *args, **kwargs):     return self.vfx.color_filter(*args, **kwargs)
    def flashback_overlay(self, *a, **k):        return self.vfx.flashback_overlay(*a, **k)
    def screen_shake(self, *args, **kwargs):     return self.vfx.screen_shake(*args, **kwargs)
    def camera_zoom(self, *args, **kwargs):      return self.vfx.camera_zoom(*args, **kwargs)
    def camera_pan(self, *args, **kwargs):       return self.vfx.camera_pan(*args, **kwargs)
    def caption(self, *args, **kwargs):          return self.vfx.caption(*args, **kwargs)
    def particles(self, *args, **kwargs):        return self.vfx.particles(*args, **kwargs)
    def play_final_blow(self, *args, **kwargs):  return self.vfx.play_final_blow(*args, **kwargs)
