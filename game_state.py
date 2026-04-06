from dataclasses import dataclass, field
from typing import List, Dict, Optional, Any

@dataclass
class Unit:
    id: int
    name: str
    type: str
    type_code: str
    faction: str
    x: int
    y: int
    strength: float
    morale: float
    ammo: int
    max_ammo: int
    armor: int
    movement: int
    movement_points: int
    accuracy_base: int
    suppress_threshold: int
    emoji: str
    destroyed: bool = False
    suppressed: bool = False

    def to_summary(self):
        return {
            "id": self.id, "name": self.name, "type": self.type,
            "type_code": self.type_code, "x": self.x, "y": self.y,
            "strength": self.strength, "morale": self.morale,
            "ammo": self.ammo, "destroyed": self.destroyed
        }

@dataclass
class Tile:
    type: str
    emoji: str
    cover_bonus: int
    movement_cost: float
    units: List[Unit] = field(default_factory=list)

@dataclass
class TacticalGameState:
    turn: int
    max_turns: int
    mode: str
    player_faction: str
    enemy_faction: str
    scenario_id: str
    map_grid: List[List[Tile]]
    friendly_units: List[Unit]
    enemy_units: List[Unit]
    artillery_fires_remaining: int
    cas_available: bool
    smoke_grenades: int
    objectives: List[Dict]
    action_log: List[str]
    narrative_log: List[str]
    friendly_kia: int = 0
    friendly_wia: int = 0
    vehicles_lost: int = 0
    enemy_kia: int = 0
    game_over: bool = False
    victory: Optional[bool] = None

@dataclass
class StrategicGameState:
    turn: int
    max_turns: int
    player_faction: str
    opponent_faction: str
    player_assets: Dict[str, int]
    opponent_assets: Dict[str, int]
    active_ew_effects: List[Dict]
    satellites_operational: Dict[str, bool]
    escalation_level: int
    nuclear_authorized: bool
    known_opponent_actions: List[str]
    strike_log: List[Dict]
    narrative_log: List[str]
    enemy_asset_last_seen: Dict[str, int] = field(default_factory=dict)
    game_over: bool = False
    victory: Optional[bool] = None
