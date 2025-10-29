"""Utility for normalising UI scale across devices.

This module computes a global scale factor from the current window size
relative to a baseline design resolution.  It also exposes helpers for
binding to scale changes so other modules (fonts, minimap, etc.) can
recompute cached values when the window or orientation changes.
"""

from __future__ import annotations

from typing import Callable, List, Optional

from kivy.core.window import Window
from kivy.metrics import MetricsBase

# ---------------------------------------------------------------------------
# Baseline configuration (defaults, will be overwritten via configure())
# ---------------------------------------------------------------------------
_BASE_WIDTH: float = 1080.0
_BASE_HEIGHT: float = 2400.0
_FONT_SCALE: float = 1.0
_MIN_SCALE: float = 0.6
_MAX_SCALE: float = 1.0
_SCALE_MODE: str = 'min'
_UI_SCALE: float = 1.5
_CONFIGURED: bool = False

# Allow callers to react to scale changes (font cache, widgets, etc.)
_SCALE_OBSERVERS: List[Callable[[float], None]] = []


def configure(*, base_width: Optional[float] = None,
              base_height: Optional[float] = None,
              font_scale: Optional[float] = None,
              min_scale: Optional[float] = None,
              max_scale: Optional[float] = None,
              scale_mode: Optional[str] = None) -> None:
    """Initialise scaling with the supplied baseline values.

    Should be called as early as possible (before UI modules that cache
    dp/sp values are imported) so that the density override is already in
    effect when they compute sizes.
    """
    global _BASE_WIDTH, _BASE_HEIGHT, _FONT_SCALE, _MIN_SCALE, _MAX_SCALE, _SCALE_MODE, _CONFIGURED

    if base_width:
        _BASE_WIDTH = float(base_width)
    if base_height:
        _BASE_HEIGHT = float(base_height)
    if font_scale:
        _FONT_SCALE = float(font_scale)
    if min_scale is not None:
        _MIN_SCALE = float(min_scale)
    if max_scale is not None:
        _MAX_SCALE = float(max_scale)
        if _MIN_SCALE > _MAX_SCALE:
            _MIN_SCALE = _MAX_SCALE
    if scale_mode:
        mode = scale_mode.lower()
        if mode in {'min', 'max', 'width', 'height'}:
            _SCALE_MODE = mode

    if not _CONFIGURED:
        Window.bind(size=_on_window_resize)
        _CONFIGURED = True

    _recompute_scale()


def bind(callback: Callable[[float], None]) -> None:
    """Register a callback that receives the effective UI scale."""
    if callback not in _SCALE_OBSERVERS:
        _SCALE_OBSERVERS.append(callback)
        callback(_UI_SCALE)


def get_scale() -> float:
    """Return the current UI scale factor."""
    return _UI_SCALE


# ---------------------------------------------------------------------------
# Internal helpers
# ---------------------------------------------------------------------------

def _on_window_resize(*_) -> None:
    _recompute_scale()


def _recompute_scale() -> None:
    global _UI_SCALE

    win_w, win_h = Window.size
    if not win_w or not win_h:
        return

    if _SCALE_MODE == 'width':
        raw_scale = win_w / _BASE_WIDTH if _BASE_WIDTH else 1.0
    elif _SCALE_MODE == 'height':
        raw_scale = win_h / _BASE_HEIGHT if _BASE_HEIGHT else 1.0
    elif _SCALE_MODE == 'max':
        raw_scale = max(win_w / _BASE_WIDTH if _BASE_WIDTH else 1.0,
                        win_h / _BASE_HEIGHT if _BASE_HEIGHT else 1.0)
    else:
        raw_scale = min(win_w / _BASE_WIDTH if _BASE_WIDTH else 1.0,
                        win_h / _BASE_HEIGHT if _BASE_HEIGHT else 1.0)

    # Clamp so extremely small viewports remain usable and desktops do not
    # blow things up beyond the baseline size.
    effective_scale = raw_scale
    if _MAX_SCALE is not None:
        effective_scale = min(effective_scale, _MAX_SCALE)
    if _MIN_SCALE is not None:
        effective_scale = max(effective_scale, _MIN_SCALE)

    _UI_SCALE = effective_scale
    _apply_metrics(effective_scale)
    _notify_observers()


def _apply_metrics(scale: float) -> None:
    # Override Kivy's perceived density so dp()/sp() stay consistent across
    # devices.  Use 160 (Android baseline) as the logical DPI unit.
    MetricsBase.density = scale
    MetricsBase.dpi = 160.0 * scale
    MetricsBase.fontscale = scale * _FONT_SCALE


def _notify_observers() -> None:
    for callback in list(_SCALE_OBSERVERS):
        try:
            callback(_UI_SCALE)
        except Exception:
            # Keep scaling robust even if a listener misbehaves.
            pass

