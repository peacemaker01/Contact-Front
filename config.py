# config.py
COLORS = {
    "HEADER": "\033[95m",
    "BLUE": "\033[94m",
    "CYAN": "\033[96m",
    "GREEN": "\033[92m",
    "WARNING": "\033[93m",
    "FAIL": "\033[91m",
    "RESET": "\033[0m",
    "BOLD": "\033[1m",
    "YELLOW": "\033[33m",
    "RED": "\033[31m",
    "DIM": "\033[2m"
}

ZONES = ["close", "medium", "long", "extreme"]
ZONE_MAP = {z: i for i, z in enumerate(ZONES)}

HIT_CHANCE = {0: 0.75, 1: 0.50, 2: 0.25, 3: 0.10}

COVER_VALUES = {
    "close": 0.2,
    "medium": 0.1,
    "long": 0.05,
    "extreme": 0.0
}

DIFFICULTY = {
    "easy": {"enemy_accuracy": 0.7, "enemy_morale_penalty": 20, "strategic_aggression": 0.2},
    "normal": {"enemy_accuracy": 1.0, "enemy_morale_penalty": 0, "strategic_aggression": 0.4},
    "hard": {"enemy_accuracy": 1.2, "enemy_morale_penalty": -20, "strategic_aggression": 0.6},
    "realistic": {"enemy_accuracy": 1.1, "enemy_morale_penalty": -30, "strategic_aggression": 0.8}
}

# Elevation bonuses
ELEVATION_BONUS = {0: 0.0, 1: 0.1, 2: 0.2}
ELEVATION_PENALTY = {0: 0.0, 1: -0.1, 2: -0.2}

# Day/night visibility modifiers
DAY_NIGHT_VISIBILITY = {
    "day": 1.0,
    "dusk": 0.7,
    "dawn": 0.7,
    "night": 0.4
}
