from __future__ import annotations

import math
import random
from typing import Dict, List, Optional, Tuple

from kivy.clock import Clock
from kivy.core.audio import SoundLoader
from kivy.graphics import (
    Color,
    Ellipse,
    InstructionGroup,
    Line,
    Rectangle,
    Triangle,
)
from kivy.properties import BooleanProperty, NumericProperty, StringProperty
from kivy.uix.floatlayout import FloatLayout

from . import config
from .entities import Bullet, Enemy, Explosion, Player
from .resources import ensure_galaga_assets


class GalagaGame(FloatLayout):
    """Interactive widget that encapsulates the Galaga-inspired gameplay."""

    score = NumericProperty(0)
    high_score = NumericProperty(0)
    lives = NumericProperty(3)
    stage = NumericProperty(0)
    state_text = StringProperty("Tap to Start")
    sub_state_text = StringProperty("Swipe to move, hold to fire")
    challenge_hits = NumericProperty(0)
    paused = BooleanProperty(False)

    def __init__(self, **kwargs):
        kwargs.setdefault("size_hint", (1, 1))
        super().__init__(**kwargs)
        self.assets = ensure_galaga_assets()
        self._sound_cache: Dict[str, object] = {}
        self._touch_id: Optional[int] = None
        self._touch_x = 0.0
        self._fire_held = False
        self._time = 0.0
        self._game_over = False
        self._in_challenge = False
        self._challenge_target = 0

        self._player = Player(0.0, 0.0, 0.0, 0.0)
        self._enemies: List[Enemy] = []
        self._player_shots: List[Bullet] = []
        self._enemy_shots: List[Bullet] = []
        self._explosions: List[Explosion] = []

        self._current_wave: Optional[dict] = None
        self._wave_queue: List[dict] = []
        self._dive_timer = 0.0

        self._background_group = InstructionGroup()
        self._bg_rectangles: List[Rectangle] = []
        self._bg_glow: Optional[Rectangle] = None
        self.canvas.before.add(self._background_group)

        self._star_canvas = InstructionGroup()
        self._enemy_canvas = InstructionGroup()
        self._bullet_canvas = InstructionGroup()
        self._player_canvas = InstructionGroup()
        self._explosion_canvas = InstructionGroup()

        self.canvas.add(self._star_canvas)
        self.canvas.add(self._enemy_canvas)
        self.canvas.add(self._bullet_canvas)
        self.canvas.add(self._player_canvas)
        self.canvas.add(self._explosion_canvas)

        self._star_layers: List[List[dict]] = []
        self._enemy_graphics: Dict[int, dict] = {}
        self._bullet_graphics: Dict[int, dict] = {}
        self._explosion_graphics: Dict[int, dict] = {}
        self._player_graphics: Dict[str, object] = {}

        self.bind(pos=self._on_size, size=self._on_size)
        self._build_background()
        self._build_starfield()
        self._reset_game_state()

        self._update_ev = Clock.schedule_interval(self._update, 1.0 / 60.0)
    def _reset_game_state(self) -> None:
        self.score = 0
        self.stage = 0
        self.challenge_hits = 0
        self.lives = 3
        self.state_text = "Tap to Start"
        self.sub_state_text = "Swipe to move, hold to fire"
        self._game_over = False
        self._time = 0.0
        self._touch_id = None
        self._touch_x = self.x + self.width * 0.5 if self.width else self.x
        self._fire_held = False

        self._player = Player(
            x=self.x + self.width * 0.5,
            y=self.y + self.height * config.PLAYER_Y_OFFSET_RATIO,
            width=self.width * config.PLAYER_WIDTH_RATIO,
            height=self.height * config.PLAYER_HEIGHT_RATIO,
        )
        self._player.lives = self.lives - 1
        self._player.alive = True
        self._player.captured = False
        self._player.dual_mode = False
        self._player.pending_dual = False
        self._player.respawn_timer = 0.0
        self._player.fire_cooldown = 0.0
        self._player.invuln_timer = 0.0
        self._player.score = 0
        self._player.high_score = self.high_score
        self._player.next_life_score = config.LIFE_SCORE_THRESHOLD

        self._enemies.clear()
        self._player_shots.clear()
        self._enemy_shots.clear()
        self._explosions.clear()
        self._enemy_canvas.clear()
        self._bullet_canvas.clear()
        self._explosion_canvas.clear()
        self._enemy_graphics.clear()
        self._bullet_graphics.clear()
        self._explosion_graphics.clear()
        self._player_canvas.clear()
        self._player_graphics.clear()

        self._current_wave = None
        self._wave_queue = list(config.WAVE_CONFIG)
        self._dive_timer = 0.0
        self._in_challenge = False
        self._challenge_target = 0

        self._draw_player(force=True)

    def reset(self) -> None:
        self._reset_game_state()

    def start(self) -> None:
        if self._game_over:
            self._reset_game_state()
        if self.stage == 0:
            self.state_text = ""
            self.sub_state_text = "Stage 1"
            self._advance_wave()

    def toggle_pause(self) -> None:
        if self.stage == 0 or self._game_over:
            return
        self.paused = not self.paused
        self.state_text = "Paused" if self.paused else ""
    def _on_size(self, *_args) -> None:
        self._update_background_rects()
        if self.width > 0 and self.height > 0:
            self._build_starfield()
        if self._player:
            self._player.width = self.width * config.PLAYER_WIDTH_RATIO
            self._player.height = self.height * config.PLAYER_HEIGHT_RATIO
            self._player.y = self.y + self.height * config.PLAYER_Y_OFFSET_RATIO
            half = self._player.width * 0.5
            if self._player.x == 0.0:
                self._player.x = self.x + self.width * 0.5
            else:
                self._player.x = max(self.x + half, min(self.x + self.width - half, self._player.x))
        for enemy in self._enemies:
            self._apply_enemy_dimensions(enemy)
        self._draw_player(force=True)

    def _build_background(self) -> None:
        self._background_group.clear()
        self._bg_rectangles = []
        bottom = config.COLOR_PALETTE["background_bottom"]
        top = config.COLOR_PALETTE["background_top"]
        layers = 6
        for idx in range(layers):
            t = idx / max(layers - 1, 1)
            color = tuple(bottom[i] * (1.0 - t) + top[i] * t for i in range(4))
            self._background_group.add(Color(*color))
            rect = Rectangle(pos=self.pos, size=self.size)
            self._background_group.add(rect)
            self._bg_rectangles.append(rect)
        glow_color = config.COLOR_PALETTE["hud_white"]
        self._background_group.add(Color(glow_color[0], glow_color[1], glow_color[2], 0.08))
        self._bg_glow = Rectangle(pos=self.pos, size=self.size)
        self._background_group.add(self._bg_glow)
        self._update_background_rects()

    def _update_background_rects(self) -> None:
        if not self._bg_rectangles or self.width <= 0 or self.height <= 0:
            return
        layers = len(self._bg_rectangles)
        segment_h = self.height / max(layers, 1)
        for idx, rect in enumerate(self._bg_rectangles):
            rect.pos = (self.x, self.y + idx * segment_h)
            rect.size = (self.width, segment_h + 1)
        if self._bg_glow is not None:
            self._bg_glow.pos = (self.x, self.y + self.height * 0.46)
            self._bg_glow.size = (self.width, self.height * 0.14)
    def _build_starfield(self) -> None:
        if self.width <= 0 or self.height <= 0:
            return
        self._star_canvas.clear()
        self._star_layers = []
        rng = random.Random(1339)
        for layer_idx in range(config.STARFIELD_LAYER_COUNT):
            stars: List[dict] = []
            density = 36 + layer_idx * 24
            size = 1.6 + layer_idx * 1.0
            speed = 25 + layer_idx * 35
            tint = config.COLOR_PALETTE["hud_white"]
            alpha = 0.32 + layer_idx * 0.22
            for _ in range(density):
                group = InstructionGroup()
                color = Color(tint[0], max(0.0, tint[1] - layer_idx * 0.16), tint[2], alpha)
                ellipse = Ellipse(size=(size, size))
                group.add(color)
                group.add(ellipse)
                self._star_canvas.add(group)
                ellipse.pos = (
                    self.x + rng.random() * self.width,
                    self.y + rng.random() * self.height,
                )
                stars.append({"group": group, "ellipse": ellipse, "speed": speed + rng.random() * 18})
            self._star_layers.append(stars)

    def _update_starfield(self, dt: float) -> None:
        if not self._star_layers:
            return
        for stars in self._star_layers:
            for star in stars:
                ellipse = star["ellipse"]
                x, y = ellipse.pos
                y -= star["speed"] * dt
                if y < self.y - ellipse.size[1]:
                    y = self.y + self.height + ellipse.size[1]
                    x = self.x + random.random() * self.width
                ellipse.pos = (x, y)
    def _update(self, dt: float) -> None:
        self._update_starfield(dt)
        if self.paused:
            return
        self._time += dt
        if self.stage == 0:
            return
        if self._player.respawn_timer > 0.0:
            self._player.respawn_timer -= dt
            if self._player.respawn_timer <= 0.0:
                self._player.alive = True
                self._player.invuln_timer = config.PLAYER_INVULN_TIME
                if self._player.pending_dual:
                    self._player.dual_mode = True
                    self._player.pending_dual = False
            else:
                return
        self._update_player(dt)
        self._update_enemies(dt)
        self._update_bullets(dt)
        self._update_explosions(dt)
        self._handle_collisions()
        self._cleanup_entities()

    def _update_player(self, dt: float) -> None:
        if not self._player.alive:
            return
        if self._touch_id is not None:
            dx = self._touch_x - self._player.x
            max_step = self.height * config.PLAYER_BASE_SPEED_PER_HEIGHT * dt
            if dx > max_step:
                dx = max_step
            elif dx < -max_step:
                dx = -max_step
            self._player.x += dx
            half = self._player.width * 0.5
            self._player.x = max(self.x + half, min(self.x + self.width - half, self._player.x))
        if self._fire_held:
            self._player.fire_cooldown -= dt
            if self._player.fire_cooldown <= 0.0:
                self._fire_player()
        else:
            self._player.fire_cooldown = max(0.0, self._player.fire_cooldown - dt)
        if self._player.invuln_timer > 0.0:
            self._player.invuln_timer = max(0.0, self._player.invuln_timer - dt)
        if self._player.pending_dual and self._player.invuln_timer <= 0.0:
            self._player.dual_mode = True
            self._player.pending_dual = False
        self._draw_player()

    def _fire_player(self) -> None:
        active = sum(1 for b in self._player_shots if b.owner == "player")
        max_bullets = config.PLAYER_MAX_BULLETS + (1 if self._player.dual_mode else 0)
        if active >= max_bullets:
            return
        offsets = [0.0]
        if self._player.dual_mode:
            offsets = [-self._player.width * 0.28, self._player.width * 0.28]
        bullet_height = self.height * 0.04
        for off in offsets:
            bullet = Bullet(
                x=self._player.x + off,
                y=self._player.top,
                width=self._player.width * 0.12,
                height=bullet_height,
                vy=self.height * config.PLAYER_BULLET_SPEED_PER_HEIGHT,
            )
            self._player_shots.append(bullet)
            self._draw_bullet(bullet, enemy=False)
        self._player.fire_cooldown = 0.16 if not self._player.dual_mode else 0.1
        self._play_sound("laser")
    def _draw_player(self, force: bool = False) -> None:
        if force:
            self._player_canvas.clear()
            self._player_graphics.clear()
        gfx = self._player_graphics
        if "body" not in gfx:
            body_color = Color(*config.COLOR_PALETTE["player_primary"])
            body = Triangle()
            accent_color = Color(*config.COLOR_PALETTE["player_secondary"])
            accent = Triangle()
            wing_color = Color(1.0, 1.0, 1.0, 0.25)
            wing = Line(width=2.0, cap="round")
            self._player_canvas.add(body_color)
            self._player_canvas.add(body)
            self._player_canvas.add(accent_color)
            self._player_canvas.add(accent)
            self._player_canvas.add(wing_color)
            self._player_canvas.add(wing)
            gfx.update(
                {
                    "body_color": body_color,
                    "body": body,
                    "accent_color": accent_color,
                    "accent": accent,
                    "wing_color": wing_color,
                    "wing": wing,
                }
            )
        body = gfx["body"]
        accent = gfx["accent"]
        wing = gfx["wing"]
        x = self._player.x
        y = self._player.y
        w = self._player.width
        h = self._player.height
        body.points = [
            x, y + h * 0.6,
            x - w * 0.5, y - h * 0.45,
            x + w * 0.5, y - h * 0.45,
        ]
        accent.points = [
            x, y + h * 0.42,
            x - w * 0.18, y - h * 0.12,
            x + w * 0.18, y - h * 0.12,
        ]
        wing.points = [
            x - w * 0.7, y - h * 0.15,
            x - w * 0.2, y + h * 0.1,
            x + w * 0.2, y + h * 0.1,
            x + w * 0.7, y - h * 0.15,
        ]
        alpha = 1.0
        if self._player.invuln_timer > 0.0:
            alpha = 0.35 + 0.65 * (0.5 + 0.5 * math.sin(self._time * 12.0))
        gfx["body_color"].a = alpha
        gfx["accent_color"].a = alpha
        gfx["wing_color"].a = alpha * 0.75
        if self._player.dual_mode:
            if "partner" not in gfx:
                partner_color = Color(*config.COLOR_PALETTE["player_secondary"])
                partner = Triangle()
                self._player_canvas.add(partner_color)
                self._player_canvas.add(partner)
                gfx["partner_color"] = partner_color
                gfx["partner"] = partner
            partner = gfx["partner"]
            partner.points = [
                x, y + h * 0.35,
                x - w * 0.45, y - h * 0.55,
                x + w * 0.45, y - h * 0.55,
            ]
            gfx["partner_color"].a = alpha * 0.9
        elif "partner" in gfx:
            try:
                self._player_canvas.remove(gfx["partner"])
                self._player_canvas.remove(gfx["partner_color"])
            except ValueError:
                pass
            gfx.pop("partner")
            gfx.pop("partner_color")
    def _apply_enemy_dimensions(self, enemy: Enemy) -> None:
        if enemy.kind == "boss":
            width = self.width * 0.12
            height = width * 0.6
        elif enemy.kind == "hornet":
            width = self.width * 0.09
            height = width * 0.7
        elif enemy.kind == "ace":
            width = self.width * 0.085
            height = width * 0.6
        else:
            width = self.width * 0.08
            height = width * 0.7
        enemy.width = width
        enemy.height = height
        if enemy.state == "formation":
            enemy.x, enemy.y = enemy.formation_pos

    def _update_enemies(self, dt: float) -> None:
        if not self._enemies and self.stage > 0:
            if self._in_challenge and self.challenge_hits >= self._challenge_target > 0:
                self._increment_score(config.SCORE_VALUES.get("challenge_bonus", 1000))
                self.sub_state_text = "Perfect! +Bonus!"
            self._advance_wave()
            return
        self._dive_timer -= dt
        for enemy in list(self._enemies):
            if enemy.launch_delay > 0.0:
                enemy.launch_delay -= dt
                continue
            if enemy.state == "launching":
                self._advance_launch(enemy, dt)
            elif enemy.state == "formation":
                self._update_formation_enemy(enemy, dt)
            elif enemy.state == "diving":
                self._advance_dive(enemy, dt)
            elif enemy.state == "returning":
                self._advance_return(enemy, dt)
            self._draw_enemy(enemy)
            if enemy.state == "formation" and random.random() < 0.0015 + 0.0004 * self.stage:
                self._fire_enemy(enemy)
        if self._dive_timer <= 0.0 and self._enemies:
            self._launch_dive_wave()

    def _advance_launch(self, enemy: Enemy, dt: float) -> None:
        if not enemy.dive_path:
            enemy.state = "formation"
            enemy.x, enemy.y = enemy.formation_pos
            return
        enemy.dive_progress += dt * 0.55
        x, y = self._sample_path(enemy.dive_path, enemy.dive_progress)
        enemy.x, enemy.y = x, y
        if enemy.dive_progress >= 1.0:
            enemy.state = "formation"
            enemy.dive_progress = 0.0
            enemy.x, enemy.y = enemy.formation_pos

    def _advance_return(self, enemy: Enemy, dt: float) -> None:
        target = enemy.target_slot or enemy.formation_pos
        dx = target[0] - enemy.x
        dy = target[1] - enemy.y
        enemy.x += dx * dt * 1.6
        enemy.y += dy * dt * 1.6
        if abs(dx) < 4 and abs(dy) < 4:
            enemy.state = "formation"
            enemy.carrying_player = False
            enemy.tractor.active = False
            enemy.capture_cooldown = config.BOSS_CAPTURE_COOLDOWN
            enemy.formation_pos = target

    def _update_formation_enemy(self, enemy: Enemy, dt: float) -> None:
        enemy.capture_cooldown = max(0.0, enemy.capture_cooldown - dt)
        sway = math.sin(self._time * 1.4 + enemy.phase) * (self.width * 0.015)
        bob = math.sin(self._time * 3.6 + enemy.phase * 0.7) * (self.height * 0.008)
        base_x, base_y = enemy.formation_pos
        enemy.x = base_x + sway
        enemy.y = base_y + bob

    def _advance_dive(self, enemy: Enemy, dt: float) -> None:
        enemy.dive_progress += dt * 0.9
        x, y = self._sample_path(enemy.dive_path, enemy.dive_progress)
        enemy.x, enemy.y = x, y
        if enemy.kind == "boss":
            self._update_tractor_beam(enemy, dt)
        if enemy.dive_progress >= 1.0 or enemy.y <= self.y:
            enemy.state = "returning"
            enemy.target_slot = enemy.formation_pos
            enemy.dive_progress = 0.0
            enemy.tractor.active = False
    def _update_tractor_beam(self, enemy: Enemy, dt: float) -> None:
        beam = enemy.tractor
        beam.timer -= dt
        if beam.active:
            if beam.timer <= 0.0:
                beam.active = False
            else:
                self._ensure_beam_graphics(enemy)
                data = self._enemy_graphics.get(id(enemy))
                if data and "beam" in data:
                    beam_line = data["beam"]
                    beam_line.points = [
                        enemy.x, enemy.y,
                        enemy.x - enemy.width * 0.35, self.y + self.height * 0.12,
                        enemy.x + enemy.width * 0.35, self.y + self.height * 0.08,
                    ]
                    data["beam_color"].a = 0.45 + 0.35 * math.sin(self._time * 10.0)
                if (
                    not self._player.captured
                    and self._player.invuln_timer <= 0.0
                    and abs(enemy.x - self._player.x) < enemy.width * 0.65
                    and enemy.y > self._player.y
                ):
                    self._capture_player(enemy)
        else:
            if (
                enemy.capture_cooldown <= 0.0
                and not enemy.carrying_player
                and enemy.y <= self.y + self.height * 0.65
            ):
                beam.active = True
                beam.duration = config.TRACTOR_BEAM_DURATION
                beam.timer = config.TRACTOR_BEAM_DURATION
                self._play_sound("tractor")
            else:
                data = self._enemy_graphics.get(id(enemy))
                if data and "beam" in data:
                    data["beam"].points = []
                    data["beam_color"].a = 0.0

    def _ensure_beam_graphics(self, enemy: Enemy) -> None:
        key = id(enemy)
        data = self._enemy_graphics.get(key)
        if not data or "beam" in data:
            return
        beam_color = Color(*config.COLOR_PALETTE["tractor_beam"])
        beam = Line(points=[], width=6.0, cap="round", joint="round")
        self._enemy_canvas.add(beam_color)
        self._enemy_canvas.add(beam)
        data["beam_color"] = beam_color
        data["beam"] = beam

    def _capture_player(self, enemy: Enemy) -> None:
        enemy.carrying_player = True
        enemy.tractor.active = False
        self._player.captured = True
        self._player.alive = False
        self._player.respawn_timer = config.PLAYER_RESPAWN_TIME
        if self.lives > 0:
            self.lives = max(0, self.lives - 1)
        self._play_sound("impact")
        if self.lives <= 0:
            self._trigger_game_over()

    def _launch_dive_wave(self) -> None:
        formation = [e for e in self._enemies if e.state == "formation" and e.launch_delay <= 0.0]
        if not formation:
            self._dive_timer = 2.0
            return
        batch = min(3, len(formation))
        selected = random.sample(formation, batch)
        for idx, enemy in enumerate(selected):
            enemy.state = "diving"
            enemy.dive_progress = 0.0
            enemy.dive_path = self._build_dive_path(enemy, idx)
        self._dive_timer = max(1.5, self._current_wave.get("dive_delay", 4.0) - 0.25 * self.stage)

    def _build_dive_path(self, enemy: Enemy, offset: int = 0) -> List[Tuple[float, float]]:
        start = enemy.formation_pos
        direction = -1 if (enemy.column + offset) % 2 == 0 else 1
        swing = self.width * (0.28 + 0.08 * random.random())
        p1 = (start[0] + direction * swing, start[1] + self.height * 0.18)
        p2 = (
            self.x + self.width * 0.5 + direction * swing * 0.3,
            self.y + self.height * random.uniform(0.25, 0.5),
        )
        end = (
            self.x + self.width * random.uniform(0.15, 0.85),
            self.y - self.height * 0.2,
        )
        return [start, p1, p2, end]

    def _build_launch_path(self, enemy: Enemy, row: int, column: int, count: int) -> List[Tuple[float, float]]:
        start_side = self.x - self.width * 0.25 if column % 2 == 0 else self.x + self.width * 1.25
        start_y = self.y + self.height + self.height * (0.08 + row * 0.04)
        ctrl1 = (
            self.x + self.width * random.uniform(0.2, 0.8),
            self.y + self.height * random.uniform(0.65, 1.1),
        )
        ctrl2 = (
            enemy.formation_pos[0] + self.width * random.uniform(-0.12, 0.12),
            enemy.formation_pos[1] + self.height * 0.3,
        )
        return [
            (start_side, start_y),
            ctrl1,
            ctrl2,
            enemy.formation_pos,
        ]

    @staticmethod
    def _sample_path(points: List[Tuple[float, float]], t: float) -> Tuple[float, float]:
        if not points or len(points) < 4:
            return points[-1] if points else (0.0, 0.0)
        t = max(0.0, min(1.0, t))
        u = 1.0 - t
        p0, p1, p2, p3 = points
        x = (
            u ** 3 * p0[0]
            + 3 * u ** 2 * t * p1[0]
            + 3 * u * t ** 2 * p2[0]
            + t ** 3 * p3[0]
        )
        y = (
            u ** 3 * p0[1]
            + 3 * u ** 2 * t * p1[1]
            + 3 * u * t ** 2 * p2[1]
            + t ** 3 * p3[1]
        )
        return x, y
    def _update_bullets(self, dt: float) -> None:
        for bullet in list(self._player_shots):
            bullet.y += bullet.vy * dt
            if bullet.y > self.y + self.height + bullet.height:
                bullet.alive = False
            self._draw_bullet(bullet, enemy=False)
        for bullet in list(self._enemy_shots):
            bullet.y += bullet.vy * dt
            if bullet.y < self.y - bullet.height:
                bullet.alive = False
            self._draw_bullet(bullet, enemy=True)

    def _draw_bullet(self, bullet: Bullet, enemy: bool) -> None:
        key = id(bullet)
        data = self._bullet_graphics.get(key)
        if data is None:
            palette_key = "enemy_laser" if enemy else "laser"
            color = Color(*config.COLOR_PALETTE.get(palette_key, (1, 1, 1, 1)))
            rect = Rectangle()
            self._bullet_canvas.add(color)
            self._bullet_canvas.add(rect)
            data = {"color": color, "rect": rect}
            self._bullet_graphics[key] = data
        rect = data["rect"]
        rect.size = (bullet.width, bullet.height)
        rect.pos = (bullet.x - bullet.width * 0.5, bullet.y - bullet.height * 0.5)

    def _fire_enemy(self, enemy: Enemy) -> None:
        bullet = Bullet(
            x=enemy.x,
            y=enemy.bottom,
            width=enemy.width * 0.2,
            height=enemy.height * 0.35,
            vy=-self.height * config.ENEMY_BULLET_SPEED_PER_HEIGHT,
            owner="enemy",
        )
        self._enemy_shots.append(bullet)
        self._draw_bullet(bullet, enemy=True)
    def _draw_enemy(self, enemy: Enemy) -> None:
        key = id(enemy)
        data = self._enemy_graphics.get(key)
        if data is None:
            palette = config.COLOR_PALETTE.get(enemy.kind, config.COLOR_PALETTE["hornet"])
            body_color = Color(*palette)
            if enemy.kind == "boss":
                body = Rectangle()
                accent_color = Color(*config.COLOR_PALETTE["player_secondary"])
                accent = Ellipse()
                wing_color = Color(1.0, 0.9, 0.3, 0.5)
                wing = Line(width=3.2, cap="round")
                self._enemy_canvas.add(body_color)
                self._enemy_canvas.add(body)
                self._enemy_canvas.add(accent_color)
                self._enemy_canvas.add(accent)
                self._enemy_canvas.add(wing_color)
                self._enemy_canvas.add(wing)
                data = {
                    "body_color": body_color,
                    "body": body,
                    "accent_color": accent_color,
                    "accent": accent,
                    "wing_color": wing_color,
                    "wing": wing,
                }
            elif enemy.kind == "hornet":
                body = Rectangle()
                accent_color = Color(*config.COLOR_PALETTE["ace"])
                accent = Line(width=2.5, cap="round")
                self._enemy_canvas.add(body_color)
                self._enemy_canvas.add(body)
                self._enemy_canvas.add(accent_color)
                self._enemy_canvas.add(accent)
                data = {
                    "body_color": body_color,
                    "body": body,
                    "accent_color": accent_color,
                    "accent": accent,
                }
            else:
                body = Ellipse()
                accent_color = Color(*config.COLOR_PALETTE["player_secondary"])
                accent = Line(width=2.0, cap="round")
                self._enemy_canvas.add(body_color)
                self._enemy_canvas.add(body)
                self._enemy_canvas.add(accent_color)
                self._enemy_canvas.add(accent)
                data = {
                    "body_color": body_color,
                    "body": body,
                    "accent_color": accent_color,
                    "accent": accent,
                }
            self._enemy_graphics[key] = data
        body = data["body"]
        if isinstance(body, Rectangle):
            body.pos = (enemy.left, enemy.bottom)
            body.size = (enemy.width, enemy.height)
        else:
            body.pos = (enemy.left, enemy.bottom)
            body.size = (enemy.width, enemy.height)
        if enemy.kind == "boss":
            accent = data["accent"]
            accent.size = (enemy.width * 0.4, enemy.height * 0.6)
            accent.pos = (enemy.x - accent.size[0] * 0.5, enemy.y - accent.size[1] * 0.2)
            wing = data["wing"]
            wing.points = [
                enemy.left, enemy.y,
                enemy.x, enemy.top + enemy.height * 0.2,
                enemy.right, enemy.y,
            ]
            if enemy.carrying_player:
                capture_color = data.get("capture_color")
                capture_shape = data.get("capture_shape")
                if capture_color is None:
                    capture_color = Color(*config.COLOR_PALETTE["player_secondary"])
                    capture_shape = Triangle()
                    self._enemy_canvas.add(capture_color)
                    self._enemy_canvas.add(capture_shape)
                    data["capture_color"] = capture_color
                    data["capture_shape"] = capture_shape
                capture_shape.points = [
                    enemy.x, enemy.bottom - enemy.height * 0.6,
                    enemy.x - enemy.width * 0.35, enemy.bottom - enemy.height * 0.1,
                    enemy.x + enemy.width * 0.35, enemy.bottom - enemy.height * 0.1,
                ]
                capture_color.a = 0.9
            elif "capture_shape" in data:
                try:
                    self._enemy_canvas.remove(data["capture_shape"])
                    self._enemy_canvas.remove(data["capture_color"])
                except ValueError:
                    pass
                data.pop("capture_shape")
                data.pop("capture_color")
        else:
            accent = data["accent"]
            if enemy.kind == "hornet":
                accent.points = [
                    enemy.left, enemy.y,
                    enemy.x, enemy.top,
                    enemy.right, enemy.y,
                    enemy.x, enemy.bottom,
                    enemy.left, enemy.y,
                ]
            else:
                accent.points = [
                    enemy.x - enemy.width * 0.35, enemy.y,
                    enemy.x, enemy.top,
                    enemy.x + enemy.width * 0.35, enemy.y,
                ]
        if key in self._enemy_graphics and enemy.kind != "boss":
            data = self._enemy_graphics[key]
            if "beam" in data:
                try:
                    self._enemy_canvas.remove(data["beam"])
                    self._enemy_canvas.remove(data["beam_color"])
                except ValueError:
                    pass
                data.pop("beam")
                data.pop("beam_color")
    def _handle_collisions(self) -> None:
        player_box = self._player.aabb()
        if self._player.alive and self._player.invuln_timer <= 0.0:
            for bullet in list(self._enemy_shots):
                if bullet.alive and self._intersects(player_box, bullet.aabb()):
                    if self._player.dual_mode:
                        self._player.dual_mode = False
                        bullet.alive = False
                        self._play_sound("impact")
                    else:
                        self._player_hit()
                        bullet.alive = False
        for enemy in list(self._enemies):
            enemy_box = enemy.aabb()
            for bullet in list(self._player_shots):
                if bullet.alive and self._intersects(enemy_box, bullet.aabb()):
                    bullet.alive = False
                    self._destroy_enemy(enemy)
                    break
            if self._player.alive and self._intersects(player_box, enemy_box):
                if enemy.kind == "boss" and enemy.carrying_player:
                    self._player.pending_dual = True
                else:
                    self._player_hit()
                self._destroy_enemy(enemy, add_score=False)

    def _destroy_enemy(self, enemy: Enemy, add_score: bool = True) -> None:
        if enemy not in self._enemies:
            return
        self._enemies.remove(enemy)
        self._remove_enemy_graphics(enemy)
        color = config.COLOR_PALETTE.get(enemy.kind, config.COLOR_PALETTE["laser"])
        explosion = Explosion(enemy.x, enemy.y, enemy.width * 0.8, 0.6, color)
        self._explosions.append(explosion)
        self._draw_explosion(explosion)
        if add_score:
            key = "boss_with_captive" if enemy.kind == "boss" and enemy.carrying_player else enemy.kind
            self._increment_score(config.SCORE_VALUES.get(key, 50))
            if self._in_challenge:
                self.challenge_hits += 1
        if enemy.kind == "boss" and enemy.carrying_player:
            self._player.captured = False
            if self._player.alive:
                self._player.pending_dual = True
            else:
                self._player.pending_dual = True
        self._play_sound("explosion")

    def _remove_enemy_graphics(self, enemy: Enemy) -> None:
        data = self._enemy_graphics.pop(id(enemy), None)
        if not data:
            return
        for inst in list(data.values()):
            try:
                self._enemy_canvas.remove(inst)
            except ValueError:
                pass
    def _add_explosion(self, x: float, y: float, radius: float, color: Tuple[float, float, float, float]) -> None:
        explosion = Explosion(x, y, radius, 0.6, color)
        self._explosions.append(explosion)
        self._draw_explosion(explosion)

    def _draw_explosion(self, explosion: Explosion) -> None:
        key = id(explosion)
        data = self._explosion_graphics.get(key)
        if data is None:
            color = Color(*explosion.color)
            ellipse = Ellipse()
            self._explosion_canvas.add(color)
            self._explosion_canvas.add(ellipse)
            data = {"color": color, "ellipse": ellipse}
            self._explosion_graphics[key] = data
        ellipse = data["ellipse"]
        ellipse.size = (explosion.radius * 2, explosion.radius * 2)
        ellipse.pos = (explosion.x - explosion.radius, explosion.y - explosion.radius)

    def _update_explosions(self, dt: float) -> None:
        for explosion in list(self._explosions):
            explosion.ttl -= dt
            data = self._explosion_graphics.get(id(explosion))
            if data:
                alpha = max(0.0, explosion.ttl / 0.6)
                data["color"].a = alpha
                radius = explosion.radius * (1.1 - 0.3 * alpha)
                data["ellipse"].size = (radius * 2, radius * 2)
                data["ellipse"].pos = (explosion.x - radius, explosion.y - radius)

    def _cleanup_entities(self) -> None:
        for bullet in list(self._player_shots):
            if not bullet.alive:
                self._remove_bullet(bullet)
                self._player_shots.remove(bullet)
        for bullet in list(self._enemy_shots):
            if not bullet.alive:
                self._remove_bullet(bullet)
                self._enemy_shots.remove(bullet)
        for explosion in list(self._explosions):
            if explosion.ttl <= 0.0:
                self._remove_explosion(explosion)
                self._explosions.remove(explosion)

    def _remove_bullet(self, bullet: Bullet) -> None:
        data = self._bullet_graphics.pop(id(bullet), None)
        if not data:
            return
        try:
            self._bullet_canvas.remove(data["rect"])
            self._bullet_canvas.remove(data["color"])
        except ValueError:
            pass

    def _remove_explosion(self, explosion: Explosion) -> None:
        data = self._explosion_graphics.pop(id(explosion), None)
        if not data:
            return
        try:
            self._explosion_canvas.remove(data["ellipse"])
            self._explosion_canvas.remove(data["color"])
        except ValueError:
            pass
    def _player_hit(self) -> None:
        if self._player.invuln_timer > 0.0:
            return
        if self._player.dual_mode:
            self._player.dual_mode = False
            self._player.invuln_timer = 0.6
            self._play_sound("impact")
            return
        self._player.alive = False
        self._player.respawn_timer = config.PLAYER_RESPAWN_TIME
        self._add_explosion(self._player.x, self._player.y, self._player.width, config.COLOR_PALETTE["player_primary"])
        self._play_sound("explosion")
        if self.lives > 0:
            self.lives -= 1
        if self.lives <= 0:
            self._trigger_game_over()

    def _trigger_game_over(self) -> None:
        self.state_text = "Game Over"
        self.sub_state_text = "Tap to play again"
        self._game_over = True
        self.stage = 0
        self._fire_held = False
        self._touch_id = None
        for enemy in list(self._enemies):
            self._remove_enemy_graphics(enemy)
        self._enemies.clear()
        for bullet in list(self._player_shots):
            self._remove_bullet(bullet)
        self._player_shots.clear()
        for bullet in list(self._enemy_shots):
            self._remove_bullet(bullet)
        self._enemy_shots.clear()

    def _advance_wave(self) -> None:
        self.stage += 1
        if self.stage % 4 == 0:
            self._in_challenge = True
            wave = self._build_wave_from_template(config.CHALLENGE_STAGE_TEMPLATE, self.stage)
            self._challenge_target = sum(min(config.FORMATION_COLUMNS, row["count"]) for row in wave["enemy_rows"])
            self.challenge_hits = 0
        else:
            self._in_challenge = False
            base = config.WAVE_CONFIG[(self.stage - 1) % len(config.WAVE_CONFIG)]
            wave = self._scale_wave(base, self.stage)
            self._challenge_target = 0
        self._current_wave = wave
        self.state_text = wave.get("label", f"Stage {self.stage}")
        self.sub_state_text = "Formation incoming"
        self._spawn_wave(wave)
        self._dive_timer = wave.get("dive_delay", 4.0)

    def _scale_wave(self, base: dict, stage: int) -> dict:
        wave = {
            "id": base.get("id", stage),
            "label": base.get("label", f"Stage {stage}"),
            "enemy_rows": [],
            "dive_delay": max(2.6, base.get("dive_delay", 4.0) * (0.96 ** max(stage - 1, 0))),
        }
        growth = 1 + (stage - 1) * 0.08
        for row in base.get("enemy_rows", []):
            count = min(config.FORMATION_COLUMNS, round(row.get("count", config.FORMATION_COLUMNS) * growth))
            row_type = row.get("type", "drone")
            if stage >= 3 and row_type == "hornet" and random.random() < 0.25:
                row_type = "ace"
            wave["enemy_rows"].append({"type": row_type, "count": max(3, count)})
        return wave

    def _build_wave_from_template(self, template: dict, stage: int) -> dict:
        wave = {
            "id": template.get("id", f"C{stage}"),
            "label": template.get("label", f"Challenge {stage}"),
            "enemy_rows": [],
            "dive_delay": template.get("dive_delay", 3.0),
        }
        for row in template.get("enemy_rows", []):
            wave["enemy_rows"].append({"type": row.get("type", "drone"), "count": row.get("count", config.FORMATION_COLUMNS)})
        return wave

    def _spawn_wave(self, wave: dict) -> None:
        self._enemies.clear()
        self._enemy_canvas.clear()
        self._enemy_graphics.clear()
        rows = wave.get("enemy_rows", [])
        row_spacing = self.height * config.FORMATION_ROW_SPACING_RATIO
        col_spacing = self.width * config.FORMATION_COLUMN_SPACING_RATIO
        base_y = self.y + self.height * 0.45
        for row_idx, row in enumerate(rows):
            count = min(config.FORMATION_COLUMNS, row.get("count", config.FORMATION_COLUMNS))
            kind = row.get("type", "drone")
            width_span = col_spacing * max(count - 1, 1)
            start_x = self.x + self.width * 0.5 - width_span * 0.5
            y = base_y + row_idx * row_spacing
            for column in range(count):
                x = start_x + column * col_spacing
                enemy = Enemy(
                    x=x,
                    y=self.y + self.height + row_idx * row_spacing,
                    width=0.0,
                    height=0.0,
                    kind=kind,
                    row=row_idx,
                    column=column,
                    formation_pos=(x, y),
                    state="launching",
                )
                self._apply_enemy_dimensions(enemy)
                enemy.phase = random.random() * math.tau
                enemy.worth = config.SCORE_VALUES.get(kind, 50)
                enemy.launch_delay = row_idx * 0.35 + column * 0.04
                enemy.capture_cooldown = random.uniform(4.0, config.BOSS_CAPTURE_COOLDOWN)
                enemy.dive_path = self._build_launch_path(enemy, row_idx, column, count)
                self._enemies.append(enemy)

    def _increment_score(self, amount: int) -> None:
        if amount <= 0:
            return
        self.score += amount
        self._player.score = self.score
        if self.score > self.high_score:
            self.high_score = self.score
            self._player.high_score = self.high_score
        if self.score >= self._player.next_life_score:
            self.lives += 1
            self._player.next_life_score += config.LIFE_SCORE_THRESHOLD
            self._play_sound("bonus")
    def _play_sound(self, name: str) -> None:
        path = self.assets.get("audio", {}).get(name)
        if not path:
            return
        sound = self._sound_cache.get(name)
        if sound is None:
            sound = SoundLoader.load(path)
            if not sound:
                return
            sound.volume = 0.6
            self._sound_cache[name] = sound
        sound.stop()
        sound.play()

    @staticmethod
    def _intersects(a: Tuple[float, float, float, float], b: Tuple[float, float, float, float]) -> bool:
        return not (a[2] < b[0] or a[0] > b[2] or a[3] < b[1] or a[1] > b[3])

    def on_touch_down(self, touch):
        if not self.collide_point(*touch.pos):
            return super().on_touch_down(touch)
        lx, _ = self.to_local(*touch.pos)
        if self.stage == 0:
            self.start()
        self._touch_id = touch.uid
        self._touch_x = lx
        self._fire_held = True
        self._player.fire_cooldown = 0.0
        touch.grab(self)
        return True

    def on_touch_move(self, touch):
        if touch.grab_current is not self:
            return super().on_touch_move(touch)
        lx, _ = self.to_local(*touch.pos)
        self._touch_x = lx
        return True

    def on_touch_up(self, touch):
        if touch.grab_current is not self:
            return super().on_touch_up(touch)
        touch.ungrab(self)
        self._touch_id = None
        self._fire_held = False
        return True
