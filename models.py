# models.py

from dataclasses import dataclass, field
from typing import List, Optional

@dataclass
class Unit:
    id: str
    name: str
    type: str
    side: str
    zone: str
    faction: str = ""
    status: str = "active"
    morale: int = 100
    ammo: int = 30
    hits: int = 0
    description: str = ""
    leadership: int = 0
    supply: int = 0
    is_leader: bool = False
    exp: int = 0
    kills: int = 0

    def is_alive(self) -> bool:
        return self.status not in ["kia", "routed"]

    def is_combat_effective(self) -> bool:
        return self.is_alive() and self.status != "suppressed" and self.ammo > 0


@dataclass
class StrategicAsset:
    nukes: int = 0
    ballistic_missiles: int = 0
    cruise_missiles: int = 0
    warships: int = 0
    intelligence_level: int = 0
    air_defense: int = 0
    electronic_warfare: int = 0
    hypersonic_missiles: int = 0
    loitering_munitions: int = 0
    anti_ship_missiles: int = 0
    cyber_warfare: int = 0
    space_assets: int = 0
    # Targetable asset categories
    missile_silos: int = 0
    infrastructure: int = 0
    command_centers: int = 0
    nuclear_sites: int = 0


@dataclass
class GameState:
    turn: int = 1
    difficulty: str = "normal"
    game_mode: str = "tactical"
    game_over: bool = False
    winner: str = ""
    history: List[str] = field(default_factory=list)
    last_narrative: str = ""
    player_side: str = "attacker"   # attacker or defender
    # Tactical fields
    weather: str = "clear"
    objective_zone: str = "medium"
    mortar_rounds: int = 0
    fpv_drones: int = 0
    recon_active: int = 0
    units: List[Unit] = field(default_factory=list)
    terrain_description: str = ""
    detected_zones: List[str] = field(default_factory=list)
    detected_timer: dict = field(default_factory=dict)  # zone -> turn last detected
    obstacles: List[str] = field(default_factory=list)
    player_faction: str = ""
    enemy_faction: str = ""
    building_damage: int = 0

    # Tactical intelligence
    enemy_posture: str = "unknown"
    enemy_ammo_estimate: str = "unknown"
    enemy_morale_trend: str = "steady"
    visible_damage: List[str] = field(default_factory=list)

    # Strategic fields
    strategic: Optional[StrategicAsset] = None
    enemy_strategic: Optional[StrategicAsset] = None
    strategic_scenario: str = ""
    civilian_casualties: int = 0
    global_tension: int = 0
    enemy_war_economy: int = 50

    # Strategic intelligence (descriptive strings shown in intel report)
    threat_matrix: str = ""
    economic_impact: str = ""
    enemy_doctrine: str = ""
    escalation_risk: str = ""
    force_effectiveness: str = ""

    # FIX: diplomatic_pressure was `str = ""` (always falsy) so sanctions
    # always returned "Diplomatic climate not suitable." Changed to bool = True
    # so sanctions work by default, and can be set False by scenario modifiers.
    diplomatic_pressure: bool = True

    # Logistics and war economy
    supply_points: int = 100
    war_economy: int = 50
    production_points: int = 10
    sanctions: int = 0
    ied_threat: int = 0
    civilian_density: int = 30
    media_attention: int = 0
    war_crimes_allegations: int = 0
    roe: str = "restricted"

    # Command & control
    command_delays: dict = field(default_factory=dict)

    # Cooldowns
    last_intel_turn: int = 0
    last_ew_turn: int = 0

    # Electronic warfare and intelligence
    sigint_level: int = 0
    jammer_active: bool = False
    deception_active: bool = False
    cyber_offense: int = 0
    cyber_defense: int = 0
    space_satellites: int = 0
    space_attack: int = 0

    # FIX: influence_ops kept as int = 0 (was already correct in source)
    # strategic_psyops checks this before allowing psyops action
    influence_ops: int = 0

    # Asymmetric warfare
    guerrilla_presence: int = 0
    human_terrain: str = ""

