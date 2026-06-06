#!/usr/bin/env python3
"""
Subprocess launcher for the Kivy game preview.
Starts Starborn_Python/game.py with --assets-dir pointing at the
current project's assets, allowing live preview of edited data.
"""
from __future__ import annotations

import json
import subprocess
import sys
import time
from pathlib import Path
from typing import Optional


class PreviewLauncher:
    """Manages a game.py subprocess for desktop preview."""

    def __init__(self, game_py_path: Path, assets_dir: Path):
        self.game_py = game_py_path
        self.assets_dir = assets_dir
        self._proc: Optional[subprocess.Popen] = None

    # ------------------------------------------------------------------
    def launch(self) -> str:
        """
        Spawn game.py as a subprocess.
        Returns an error message string, or empty string on success.
        """
        if self.is_running():
            return "Preview is already running."
        if not self.game_py.exists():
            return f"game.py not found at {self.game_py}"

        try:
            self._proc = subprocess.Popen(
                [sys.executable, str(self.game_py),
                 "--assets-dir", str(self.assets_dir)],
                cwd=str(self.game_py.parent),
            )
            return ""
        except Exception as e:
            return f"Failed to launch preview: {e}"

    # ------------------------------------------------------------------
    def is_running(self) -> bool:
        if self._proc is None:
            return False
        return self._proc.poll() is None

    # ------------------------------------------------------------------
    def stop(self):
        if self._proc is not None and self._proc.poll() is None:
            self._proc.terminate()
            try:
                self._proc.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self._proc.kill()
        self._proc = None

    # ------------------------------------------------------------------
    def send_request(self, action: str, **kwargs):
        """
        Write a preview request that game.py's hot-reload watcher picks up.
        The request file goes into the assets_dir.
        """
        req = {"action": action, "timestamp": time.time()}
        req.update(kwargs)
        req_path = self.assets_dir / "preview_request.json"
        try:
            req_path.write_text(json.dumps(req, indent=2), encoding="utf-8")
        except Exception:
            pass
