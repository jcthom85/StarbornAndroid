"""Constants that govern pacing and layout for the Galaga mini game."""

ASPECT_WIDTH = 9
ASPECT_HEIGHT = 21
DESIGN_WIDTH = 900
DESIGN_HEIGHT = 2100

PLAYER_WIDTH_RATIO = 0.12
PLAYER_HEIGHT_RATIO = 0.065
PLAYER_Y_OFFSET_RATIO = 0.08
PLAYER_BASE_SPEED_PER_HEIGHT = 0.95
PLAYER_MAX_BULLETS = 3
PLAYER_BULLET_SPEED_PER_HEIGHT = 1.45
PLAYER_RESPAWN_TIME = 2.2
PLAYER_INVULN_TIME = 2.0

ENEMY_BASE_SPEED_PER_HEIGHT = 0.32
ENEMY_DIVE_SPEED_PER_HEIGHT = 1.5
ENEMY_BULLET_SPEED_PER_HEIGHT = 0.7

FORMATION_COLUMNS = 11
FORMATION_ROW_SPACING_RATIO = 0.065
FORMATION_COLUMN_SPACING_RATIO = 0.07
FORMATION_OFFSET_X_RATIO = 0.5
FORMATION_OFFSET_Y_RATIO = 0.62

WAVE_CONFIG = [
    {
        "id": 1,
        "label": "Stage 1",
        "enemy_rows": [
            {"type": "drone", "count": 11},
            {"type": "drone", "count": 11},
            {"type": "hornet", "count": 10},
            {"type": "hornet", "count": 10},
            {"type": "boss", "count": 4},
        ],
        "dive_delay": 5.0,
    },
    {
        "id": 2,
        "label": "Stage 2",
        "enemy_rows": [
            {"type": "drone", "count": 11},
            {"type": "hornet", "count": 11},
            {"type": "hornet", "count": 11},
            {"type": "boss", "count": 4},
            {"type": "boss", "count": 2},
        ],
        "dive_delay": 4.5,
    },
    {
        "id": 3,
        "label": "Stage 3",
        "enemy_rows": [
            {"type": "hornet", "count": 11},
            {"type": "hornet", "count": 11},
            {"type": "boss", "count": 6},
            {"type": "boss", "count": 2},
        ],
        "dive_delay": 4.0,
    },
]

CHALLENGE_STAGE_TEMPLATE = {
    "label": "Challenging!",
    "enemy_rows": [
        {"type": "drone", "count": 11},
        {"type": "hornet", "count": 11},
        {"type": "ace", "count": 8},
    ],
    "dive_delay": 2.5,
}

BOSS_CAPTURE_COOLDOWN = 14.0
TRACTOR_BEAM_DURATION = 4.0

SCORE_VALUES = {
    "drone": 50,
    "hornet": 80,
    "ace": 150,
    "boss": 150,
    "boss_with_captive": 400,
    "challenge_bonus": 1000,
}

LIFE_SCORE_THRESHOLD = 20000

STARFIELD_LAYER_COUNT = 3

COLOR_PALETTE = {
    "background_top": (0.04, 0.01, 0.12, 1),
    "background_bottom": (0.06, 0.12, 0.28, 1),
    "player_primary": (0.94, 0.33, 0.53, 1),
    "player_secondary": (0.28, 0.78, 0.94, 1),
    "drone": (0.98, 0.84, 0.18, 1),
    "hornet": (0.52, 0.97, 0.42, 1),
    "ace": (0.72, 0.56, 0.94, 1),
    "boss": (1.0, 0.46, 0.12, 1),
    "tractor_beam": (0.43, 0.82, 1.0, 0.65),
    "laser": (0.94, 0.94, 0.94, 1),
    "enemy_laser": (1.0, 0.35, 0.27, 1),
    "hud_gold": (1.0, 0.78, 0.3, 1),
    "hud_white": (0.96, 0.96, 0.96, 1),
}

