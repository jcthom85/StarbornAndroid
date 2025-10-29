"""ui/minimap_widget.py – v4.9 (Data Structure Fix)
============================================================
* FIX: Resolved a `KeyError: 0` crash during map refresh.
* CAUSE: The widget was passed a dictionary of room positions where each coordinate was a dictionary (e.g., {'x': 0, 'y': 0}) instead of a tuple. The previous fix did not account for this data structure.
* SOLUTION: The `refresh` method is now significantly more robust. It inspects the incoming data and correctly parses coordinates whether they are provided as tuples/lists OR as dictionaries with 'x' and 'y' keys. This mirrors the logic of the original, stable widget.
"""

from __future__ import annotations

from typing import Dict, List, Optional, Tuple, Any

from kivy.animation import Animation
from kivy.app import App
from kivy.clock import Clock
from kivy.graphics import (Color, Ellipse, Fbo, Line, Rectangle)
from kivy.metrics import dp
from kivy.properties import NumericProperty
from kivy.uix.widget import Widget

from ui import scaling

__all__ = ["MinimapWidget", "MinimapAssetCache"]

Coord = Tuple[int, int]


class MinimapAssetCache:  # stub for full_map_screen import
    frame = room = player = None

class AnimableWidget(Widget):
    scale = NumericProperty(1.0)


class MinimapWidget(Widget):
    """A functional, holographic minimap with a 3x3 view and corrected drawing logic."""

    # --- PALETTE ---
    _CLR_BACKGROUND = (0.05, 0.1, 0.15, 0.85)
    _CLR_BORDER = (0.3, 0.8, 1.0, 1.0)
    _CLR_BORDER_ACCENT = (0.6, 0.9, 1.0, 0.8)
    _CLR_GRID = (0.3, 0.8, 1.0, 0.15)
    _CLR_TILE = (0.6, 0.85, 1.0, 0.7)
    _CLR_TILE_GLOW = (0.9, 1.0, 1.0, 1.0)
    _CLR_PLAYER = (1.0, 0.9, 0.3, 1.0)

    def __init__(self, **kwargs):
        self._base_size = kwargs.pop('base_size', 180)
        kwargs.setdefault('size', (dp(self._base_size), dp(self._base_size)))
        super().__init__(**kwargs)

        self.fbo = Fbo(size=self.size, with_stencilbuffer=True, clear_color=(0, 0, 0, 0))
        with self.canvas:
            Color(1, 1, 1, 1)
            self._fbo = Rectangle(pos=self.pos, size=self.size)

        self._discovered: List[Coord] = []
        self._cur: Coord = (0, 0)
        # Maps for ID<->position resolution so we can validate true connections
        self._id_to_pos: Dict[str, Coord] = {}
        self._pos_to_id: Dict[Coord, str] = {}
        
        self._player_anim_widget = AnimableWidget()
        self._start_player_pulse()

        scaling.bind(self._on_scale_changed)

        self.bind(size=self._sync, pos=self._sync)
        Clock.schedule_once(self._draw, 0)

    def _on_scale_changed(self, scale: float) -> None:
        if not hasattr(self, '_cur'):
            return
        new_edge = dp(self._base_size)
        self.size = (new_edge, new_edge)
        self.fbo.size = self.size
        self._fbo.size = self.size
        self._draw()

    def refresh(self, room_positions: Dict[str, Any], current_room_id: str, *_):
        """
        Refreshes the minimap, robustly handling multiple coordinate data structures.
        """
        safe_positions = {}
        if room_positions:
            # Check the type of the first value to determine the structure
            sample_value = next(iter(room_positions.values()))

            if isinstance(sample_value, dict):
                # Handle {'x': 0, 'y': 0} structure
                safe_positions = {k: (int(v.get('x', 0)), int(v.get('y', 0))) for k, v in room_positions.items()}
            elif isinstance(sample_value, (list, tuple)):
                # Handle ('0', '0') structure
                safe_positions = {k: (int(v[0]), int(v[1])) for k, v in room_positions.items()}

        self._id_to_pos = safe_positions
        # Build reverse map (if duplicates exist, last one wins; acceptable for UI)
        self._pos_to_id = {pos: rid for rid, pos in safe_positions.items()}
        self._discovered = list(safe_positions.values())
        self._cur = safe_positions.get(current_room_id, (0, 0))
        
        self._draw()

    def _draw(self, *_):
        w, h = self.size
        base = min(w, h)
        cx, cy = w / 2, h / 2
        curx, cury = self._cur

        # --- Define geometry ---
        g = base / 7
        pad = g / 2.7
        step = g + pad
        radius = base * 0.15
        padding = base * 0.1

        self.fbo.clear()
        with self.fbo:
            # --- Base Panel ---
            Color(*self._CLR_BACKGROUND)
            Rectangle(pos=(0, 0), size=self.size, radius=(radius, radius, radius, radius))

            # --- 3x3 Grid ---
            Color(*self._CLR_GRID)
            grid_start_x = cx - 1.5 * step
            grid_start_y = cy - 1.5 * step
            for i in range(4):
                Line(points=[grid_start_x + i * step, grid_start_y, grid_start_x + i * step, grid_start_y + 3 * step], width=dp(1))
                Line(points=[grid_start_x, grid_start_y + i * step, grid_start_x + 3 * step, grid_start_y + i * step], width=dp(1))

            # --- Stylized Border ---
            Color(*self._CLR_BORDER)
            Line(rounded_rectangle=(1, 1, w - 2, h - 2, radius), width=dp(1))
            
            # --- Corner accent details ---
            corner_size = padding * 1.5
            Color(*self._CLR_BORDER_ACCENT)
            Line(points=[padding, h - corner_size, padding, h - padding, corner_size, h - padding], width=dp(1.5))
            Line(points=[w - corner_size, h - padding, w - padding, h - padding, w - padding, h - corner_size], width=dp(1.5))
            Line(points=[padding, corner_size, padding, padding, corner_size, padding], width=dp(1.5))
            Line(points=[w - padding, corner_size, w - padding, padding, w - corner_size, padding], width=dp(1.5))

            # --- Connection lines (only where rooms are truly connected) ---
            if self._discovered:
                s = set(self._discovered)

                def _connected(rx: int, ry: int, dx: int, dy: int) -> bool:
                    """Return True if room at (rx,ry) has a connection in (dx,dy) to the neighbor."""
                    src_pos = (rx, ry)
                    dst_pos = (rx + dx, ry + dy)
                    src_id = self._pos_to_id.get(src_pos)
                    dst_id = self._pos_to_id.get(dst_pos)
                    if not src_id or not dst_id:
                        return False
                    try:
                        app = App.get_running_app()
                        game = getattr(app, 'current_game', None)
                        rooms = getattr(game, 'rooms', {}) if game else {}
                        room = rooms.get(src_id)
                        if not room:
                            return False
                        # Prefer JSON-provided 'connections' mapping of id->id
                        conns = getattr(room, 'connections', None)
                        if isinstance(conns, dict):
                            dir_key = { (1,0): 'east', (-1,0): 'west', (0,1): 'north', (0,-1): 'south' }.get((dx, dy))
                            if not dir_key:
                                return False
                            return conns.get(dir_key) == dst_id
                        # Fallback: use exits dict (direction->Room) if present
                        exits = getattr(room, 'exits', None)
                        if isinstance(exits, dict):
                            dir_key = { (1,0): 'east', (-1,0): 'west', (0,1): 'north', (0,-1): 'south' }.get((dx, dy))
                            dest_room = exits.get(dir_key)
                            return bool(getattr(dest_room, 'room_id', None) == dst_id)
                    except Exception:
                        pass
                    return False

                Color(*self._CLR_GRID[:3], 0.6)
                for rx, ry in list(s):
                    if abs(rx - curx) <= 1 and abs(ry - cury) <= 1:
                        # Only draw to east and north to avoid duplicates
                        for dx, dy in ((1, 0), (0, 1)):
                            nx, ny = rx + dx, ry + dy
                            if (nx, ny) in s and abs(nx - curx) <= 1 and abs(ny - cury) <= 1:
                                if not _connected(rx, ry, dx, dy):
                                    continue
                                # Map both room centers to pixels
                                x1 = cx + (rx - curx) * step
                                y1 = cy + (ry - cury) * step
                                x2 = cx + (nx - curx) * step
                                y2 = cy + (ny - cury) * step
                                Line(points=[x1, y1, x2, y2], width=dp(1))

            # --- Rooms ---
            if self._discovered:
                for rx, ry in self._discovered:
                    if abs(rx - curx) <= 1 and abs(ry - cury) <= 1:
                        px = cx - g/2 + (rx - curx) * step
                        py = cy - g/2 + (ry - cury) * step
                        
                        is_current = (rx, ry) == (curx, cury)
                        pip_color = self._CLR_TILE_GLOW if is_current else self._CLR_TILE
                        pip_size = g * (0.9 if is_current else 0.6)
                        
                        Color(*pip_color)
                        Ellipse(pos=(px + g/2 - pip_size/2, py + g/2 - pip_size/2), size=(pip_size, pip_size))

            # --- Player Indicator ---
            self._draw_player(cx, cy, g)

        self.fbo.draw()
        self._fbo.texture = self.fbo.texture

    def _draw_player(self, cx, cy, tile_size):
        size = tile_size * 0.6
        
        glow_size = size * (1 + self._player_anim_widget.scale * 0.5)
        Color(self._CLR_PLAYER[0], self._CLR_PLAYER[1], self._CLR_PLAYER[2], 0.3 * (1 - self._player_anim_widget.scale))
        Ellipse(pos=(cx - glow_size/2, cy - glow_size/2), size=(glow_size, glow_size))
        
        Color(*self._CLR_PLAYER)
        hair_length = size / 2
        line_width = dp(2)
        Line(points=[cx - hair_length, cy, cx + hair_length, cy], width=line_width)
        Line(points=[cx, cy - hair_length, cx, cy + hair_length], width=line_width)

    def _start_player_pulse(self):
        self._player_anim_widget.scale = 1.0
        anim = (
            Animation(scale=0.1, duration=1.2, t='out_quad') +
            Animation(scale=1.0, duration=1.2, t='in_out_quad')
        )
        anim.repeat = True
        anim.start(self._player_anim_widget)
        self._player_anim_widget.bind(scale=self._draw)

    def _sync(self, *_):
        self.fbo.size = self.size
        self._fbo.pos = self.pos
        self._fbo.size = self.size
        self._draw()

    def on_touch_down(self, touch):
        if self.collide_point(*touch.pos):
            from kivy.app import App
            print("[Minimap] tapped – opening menu(map)")
            App.get_running_app().open_menu("map")
            return True
        return super().on_touch_down(touch)
