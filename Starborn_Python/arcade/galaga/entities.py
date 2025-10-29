"""Light-weight data structures for the Galaga mini game."""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import List, Tuple


@dataclass(slots=True)
class Entity:
    x: float
    y: float
    width: float
    height: float
    vx: float = 0.0
    vy: float = 0.0
    alive: bool = True

    @property
    def left(self) -> float:
        return self.x - self.width * 0.5

    @property
    def right(self) -> float:
        return self.x + self.width * 0.5

    @property
    def top(self) -> float:
        return self.y + self.height * 0.5

    @property
    def bottom(self) -> float:
        return self.y - self.height * 0.5

    def aabb(self) -> Tuple[float, float, float, float]:
        return self.left, self.bottom, self.right, self.top


@dataclass(slots=True)
class Bullet(Entity):
    owner: str = "player"  # 'player' or 'enemy'
    damage: int = 1


@dataclass(slots=True)
class Player(Entity):
    lives: int = 3
    dual_mode: bool = False
    captured: bool = False
    respawn_timer: float = 0.0
    invuln_timer: float = 0.0
    fire_cooldown: float = 0.0
    score: int = 0
    high_score: int = 0
    streak: int = 0
    bonus_hits: int = 0
    next_life_score: int = 20000
    pending_dual: bool = False


@dataclass(slots=True)
class TractorBeam:
    active: bool = False
    timer: float = 0.0
    duration: float = 0.0


@dataclass(slots=True)
class Enemy(Entity):
    kind: str = "drone"
    row: int = 0
    column: int = 0
    formation_pos: Tuple[float, float] = (0.0, 0.0)
    state: str = "formation"  # formation, launching, diving, returning, retreat
    phase: float = 0.0
    dive_path: List[Tuple[float, float]] = field(default_factory=list)
    dive_progress: float = 0.0
    target_slot: Tuple[float, float] | None = None
    worth: int = 50
    tractor: TractorBeam = field(default_factory=TractorBeam)
    capture_cooldown: float = 0.0
    carrying_player: bool = False
    dive_group: int = 0
    launch_delay: float = 0.0


@dataclass(slots=True)
class Explosion:
    x: float
    y: float
    radius: float
    ttl: float
    color: Tuple[float, float, float, float]
