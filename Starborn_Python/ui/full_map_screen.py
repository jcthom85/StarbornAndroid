"""
ui/full_map_screen.py – v2.1
* Adds auto-centering on the player’s current room.
"""

from __future__ import annotations

from kivy.app import App
from kivy.uix.screenmanager import Screen
from kivy.uix.scrollview import ScrollView
from kivy.uix.anchorlayout import AnchorLayout
from kivy.uix.floatlayout import FloatLayout
from kivy.metrics import dp
from kivy.graphics import Color, RoundedRectangle, Line, Rectangle
from kivy.clock import Clock

from font_manager import fonts
from ui.themed_button import ThemedButton
from .minimap_widget import MinimapAssetCache


class FullMapScreen(Screen):
    # ── Map geometry ───────────────────────────────────────────────────
    GRID  = dp(24)
    GAP   = dp(8)
    CELL  = GRID + GAP
    CANVAS = dp(2200)

    # --- MODIFIED: Added padding to make room for the recenter button ---
    # ── Frame / layout tuning ──────────────────────────────────────────
    FRAME_PAD_L  = dp(-20)
    FRAME_PAD_R  = dp(23)
    FRAME_PAD_B  = dp(40)
    HEADER_CLEAR = dp(60)
    FRAME_RADIUS = dp(18)

    # ───────────────────────────────────────────────────────────────────
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.assets = MinimapAssetCache()

        # viewport
        self.scroll = ScrollView(
            do_scroll_x=True,
            do_scroll_y=True,
            bar_width=0,
            scroll_type=['content'],
            size_hint=(None, None),
        )
        self.map_layout = FloatLayout(size_hint=(None, None),
                                      size=(self.CANVAS, self.CANVAS),
                                      pos_hint={'x': 0, 'y': 0})
        self.scroll.add_widget(self.map_layout)
        self.add_widget(self.scroll)

        # --- NEW: Recenter Button ---
        # This button will float at the bottom of the screen.
        recenter_container = AnchorLayout(
            anchor_x='center', anchor_y='bottom',
            padding=[0, 0, 0, dp(16)] # Add padding to lift it off the bottom edge
        )
        game = App.get_running_app().current_game
        tm = game.themes
        fg, bg = tm.col('fg'), tm.col('bg')
        recenter_btn = ThemedButton(
            text="Recenter",
            size_hint=(None, None), size=(dp(140), dp(44)),
            font_name=fonts["popup_button"]["name"],
            font_size=fonts["popup_button"]["size"],
            bg_color=fg, color=bg
        )
        recenter_btn.bind(on_release=self._center_on_current_room)
        recenter_container.add_widget(recenter_btn)
        self.add_widget(recenter_container)

        # frame gfx
        with self.canvas.before:
            self.frame_color = Color(0, 0, 0, 0.55)
            self.frame_rect  = RoundedRectangle(radius=[self.FRAME_RADIUS])
            self.border_col  = Color(1, 1, 1, 0.35)
            self.border_line = Line(width=dp(1.2),
                                    rounded_rectangle=(0, 0, 0, 0,
                                                       self.FRAME_RADIUS))

        self.bind(pos=self._sync_frame, size=self._sync_frame)
        Clock.schedule_once(self._sync_frame, 0)

    # ───────────────────────────────────────────────────────────────────
    #  Lifecycle
    # ───────────────────────────────────────────────────────────────────
    def on_pre_enter(self, *_):
        Clock.schedule_once(self._draw, 0.1)

    # ───────────────────────────────────────────────────────────────────
    #  Drawing helpers
    # ───────────────────────────────────────────────────────────────────
    def _draw(self, *_):
        self.map_layout.canvas.clear()
        self._draw_rooms()
        # centre AFTER the ScrollView knows its final size
        Clock.schedule_once(self._center_on_current_room, 0)

    def _draw_rooms(self):
        game = App.get_running_app().current_game
        if not game:
            return

        # Normalize discovered to tuples (in place) once
        if any(isinstance(p, list) for p in game.discovered):
            game.discovered = {tuple(p) for p in game.discovered}

        for rid, (x, y) in game.room_positions.items():
            pos = (x, y)
            if pos not in game.discovered:
                continue

        xs, ys = zip(*game.discovered)
        min_x, max_x = min(xs), max(xs)
        min_y, max_y = min(ys), max(ys)
        map_w = (max_x - min_x + 1) * self.CELL
        map_h = (max_y - min_y + 1) * self.CELL
        off_x = (self.CANVAS - map_w) / 2
        off_y = (self.CANVAS - map_h) / 2

        with self.map_layout.canvas:
            # Draw connection lines only for true directional neighbors
            try:
                Color(1, 1, 1, 0.28)
                discovered_pos = set(game.discovered) if getattr(game, 'discovered', None) else set()
                
                for room_id, room_pos in game.room_positions.items():
                    if room_pos not in discovered_pos:
                        continue

                    room_data = game.rooms.get(room_id)
                    if not room_data: continue

                    for direction, dest_id in room_data.connections.items():
                        dest_pos = game.room_positions.get(dest_id)
                        if dest_pos and dest_pos in discovered_pos:
                            # Ensure the dest is actually the adjacent neighbor in that direction
                            dxdy = {
                                'east':  (1, 0),
                                'west':  (-1, 0),
                                'north': (0, 1),
                                'south': (0, -1),
                            }.get(direction)
                            if not dxdy:
                                continue
                            exp = (room_pos[0] + dxdy[0], room_pos[1] + dxdy[1])
                            if dest_pos != exp:
                                continue
                            px_a = off_x + (room_pos[0] - min_x) * self.CELL
                            py_a = off_y + (room_pos[1] - min_y) * self.CELL
                            px_b = off_x + (dest_pos[0] - min_x) * self.CELL
                            py_b = off_y + (dest_pos[1] - min_y) * self.CELL
                            shrink = self.GAP * 0.5
                            if direction == "east":
                                    x1 = px_a + self.GRID - shrink
                                    y1 = py_a + self.GRID / 2
                                    x2 = px_b + shrink
                                    y2 = py_b + self.GRID / 2
                            elif direction == "north":
                                    x1 = px_a + self.GRID / 2
                                    y1 = py_a + self.GRID - shrink
                                    x2 = px_b + self.GRID / 2
                                    y2 = py_b + shrink
                            # For south and west, the connection is drawn by the other room
                            else:
                                continue
                            Line(points=[x1, y1, x2, y2], width=dp(1))
            except Exception:
                pass
            for room_id, pos in game.room_positions.items():
                if pos not in game.discovered:
                    continue
                px = off_x + (pos[0] - min_x) * self.CELL
                py = off_y + (pos[1] - min_y) * self.CELL
                tex = self.assets.room
                Color(1, 1, 1, 0.9 if tex else 0.3)
                Rectangle(texture=tex, pos=(px, py),
                          size=(self.GRID, self.GRID))
                if room_id == getattr(game.current_room, "room_id", None):
                    Color(1, 1, 1, 1)
                    Rectangle(texture=self.assets.player, pos=(px, py),
                              size=(self.GRID, self.GRID))

    # ───────────────────────────────────────────────────────────────────
    #  Centre on player
    # ───────────────────────────────────────────────────────────────────
    def _center_on_current_room(self, *_):
        app = App.get_running_app()
        game = getattr(app, "current_game", None)
        if not game or not game.current_room:
            return

        pos = game.room_positions.get(game.current_room.room_id)
        if not pos:
            return

        # same maths we used during drawing
        xs, ys = zip(*game.discovered)
        min_x, max_x = min(xs), max(xs)
        min_y, max_y = min(ys), max(ys)
        map_w = (max_x - min_x + 1) * self.CELL
        map_h = (max_y - min_y + 1) * self.CELL
        off_x = (self.CANVAS - map_w) / 2
        off_y = (self.CANVAS - map_h) / 2

        px = off_x + (pos[0] - min_x) * self.CELL + self.GRID / 2
        py = off_y + (pos[1] - min_y) * self.CELL + self.GRID / 2

        content_w, content_h = self.map_layout.size
        view_w,    view_h    = self.scroll.size

        target_x = max(0, px - view_w / 2)
        # --- THIS IS THE FIX: Add a horizontal "bump" to the centering ---
        # This nudges the map to the right slightly for better visual balance.
        target_x = max(0, px - view_w / 2 - dp(8))

        # --- THIS IS THE FIX: Add a vertical "bump" to the centering ---
        # This nudges the map up slightly to account for the header area.
        target_y = max(0, py - view_h / 2 + dp(0))

        max_x = max(0, content_w - view_w)
        max_y = max(0, content_h - view_h)

        # convert to 0-1 fractions for ScrollView
        self.scroll.scroll_x = 0 if max_x == 0 else target_x / max_x
        # Kivy’s Y axis: 0 = bottom, 1 = top
        self.scroll.scroll_y = 1 if max_y == 0 else 1 - target_y / max_y

    # ───────────────────────────────────────────────────────────────────
    #  Frame & viewport sync
    # ───────────────────────────────────────────────────────────────────
    def _sync_frame(self, *_):
        lx = self.x + self.FRAME_PAD_L
        ly = self.y + self.FRAME_PAD_B
        lw = self.width  - self.FRAME_PAD_L - self.FRAME_PAD_R
        lh = self.height - self.FRAME_PAD_B - self.HEADER_CLEAR

        self.frame_rect.pos  = (lx, ly)
        self.frame_rect.size = (lw, lh)
        self.border_line.rounded_rectangle = (lx, ly, lw, lh,
                                              self.FRAME_RADIUS)

        self.scroll.pos  = (lx, ly)
        self.scroll.size = (lw, lh)
