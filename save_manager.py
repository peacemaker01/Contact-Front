# save_manager.py
import os
import json
import time
from models import GameState, Unit, StrategicAsset

SAVE_DIR = "saves"

def ensure_dir():
    if not os.path.exists(SAVE_DIR):
        os.makedirs(SAVE_DIR)

def save_game(state):
    ensure_dir()
    filename = f"{SAVE_DIR}/save_{int(time.time())}.json"
    data = {
        "turn": state.turn,
        "difficulty": state.difficulty,
        "game_mode": state.game_mode,
        "game_over": state.game_over,
        "winner": state.winner,
        "history": state.history,
        "last_narrative": state.last_narrative,
        "weather": state.weather,
        "time_of_day": state.time_of_day,
        "objective_zone": state.objective_zone,
        "mortar_rounds": state.mortar_rounds,
        "fpv_drones": state.fpv_drones,
        "recon_active": state.recon_active,
        "units": [],
        "terrain_description": state.terrain_description,
        "detected_zones": state.detected_zones,
        "obstacles": state.obstacles,
        "player_faction": state.player_faction,
        "enemy_faction": state.enemy_faction,
        "building_damage": state.building_damage,
        "elevation_map": state.elevation_map,
        "enemy_posture": state.enemy_posture,
        "enemy_ammo_estimate": state.enemy_ammo_estimate,
        "enemy_morale_trend": state.enemy_morale_trend,
        "visible_damage": state.visible_damage,
        "strategic_scenario": state.strategic_scenario,
        "civilian_casualties": state.civilian_casualties,
        "global_tension": state.global_tension,
        "enemy_war_economy": state.enemy_war_economy,
        "threat_matrix": state.threat_matrix,
        "economic_impact": state.economic_impact,
        "diplomatic_pressure": state.diplomatic_pressure,
        "enemy_doctrine": state.enemy_doctrine,
        "escalation_risk": state.escalation_risk,
        "force_effectiveness": state.force_effectiveness,
        "supply_points": state.supply_points,
        "war_economy": state.war_economy,
        "production_points": state.production_points,
        "sanctions": state.sanctions,
        "ied_threat": state.ied_threat,
        "civilian_density": state.civilian_density,
        "media_attention": state.media_attention,
        "war_crimes_allegations": state.war_crimes_allegations,
        "roe": state.roe,
        "command_delays": state.command_delays,
        "last_intel_turn": state.last_intel_turn,
        "last_ew_turn": state.last_ew_turn,
        "sigint_level": state.sigint_level,
        "jammer_active": state.jammer_active,
        "deception_active": state.deception_active,
        "cyber_offense": state.cyber_offense,
        "cyber_defense": state.cyber_defense,
        "space_satellites": state.space_satellites,
        "space_attack": state.space_attack,
        "influence_ops": state.influence_ops,
        "guerrilla_presence": state.guerrilla_presence,
        "human_terrain": state.human_terrain,
        "supply_line_active": state.supply_line_active,
        "supply_route_zone": state.supply_route_zone,
        "prisoners": []
    }
    for u in state.units:
        data["units"].append({
            "id": u.id, "name": u.name, "type": u.type, "side": u.side,
            "zone": u.zone, "faction": u.faction, "status": u.status,
            "morale": u.morale, "ammo": u.ammo, "hits": u.hits, "description": u.description,
            "leadership": u.leadership, "supply": u.supply, "is_leader": u.is_leader,
            "exp": u.exp, "kills": u.kills, "overwatch": u.overwatch, "overwatch_zone": u.overwatch_zone,
            "is_medic": u.is_medic, "elevation": u.elevation, "prisoner": u.prisoner
        })
    for p in state.prisoners:
        data["prisoners"].append({
            "id": p.id, "name": p.name, "type": p.type, "side": p.side,
            "zone": p.zone, "faction": p.faction, "status": p.status,
            "morale": p.morale, "ammo": p.ammo, "hits": p.hits, "description": p.description,
            "special_equipment": getattr(p, "special_equipment", [])
        })
    if state.strategic:
        data["strategic"] = {
            "nukes": state.strategic.nukes,
            "ballistic_missiles": state.strategic.ballistic_missiles,
            "cruise_missiles": state.strategic.cruise_missiles,
            "warships": state.strategic.warships,
            "intelligence_level": state.strategic.intelligence_level,
            "air_defense": state.strategic.air_defense,
            "electronic_warfare": state.strategic.electronic_warfare,
            "hypersonic_missiles": state.strategic.hypersonic_missiles,
            "loitering_munitions": state.strategic.loitering_munitions,
            "anti_ship_missiles": state.strategic.anti_ship_missiles,
            "cyber_warfare": state.strategic.cyber_warfare,
            "space_assets": state.strategic.space_assets,
            "missile_silos": state.strategic.missile_silos,
            "infrastructure": state.strategic.infrastructure,
            "command_centers": state.strategic.command_centers,
            "nuclear_sites": state.strategic.nuclear_sites
        }
    if state.enemy_strategic:
        data["enemy_strategic"] = {
            "nukes": state.enemy_strategic.nukes,
            "ballistic_missiles": state.enemy_strategic.ballistic_missiles,
            "cruise_missiles": state.enemy_strategic.cruise_missiles,
            "warships": state.enemy_strategic.warships,
            "intelligence_level": state.enemy_strategic.intelligence_level,
            "air_defense": state.enemy_strategic.air_defense,
            "electronic_warfare": state.enemy_strategic.electronic_warfare,
            "hypersonic_missiles": state.enemy_strategic.hypersonic_missiles,
            "loitering_munitions": state.enemy_strategic.loitering_munitions,
            "anti_ship_missiles": state.enemy_strategic.anti_ship_missiles,
            "cyber_warfare": state.enemy_strategic.cyber_warfare,
            "space_assets": state.enemy_strategic.space_assets,
            "missile_silos": state.enemy_strategic.missile_silos,
            "infrastructure": state.enemy_strategic.infrastructure,
            "command_centers": state.enemy_strategic.command_centers,
            "nuclear_sites": state.enemy_strategic.nuclear_sites
        }
    with open(filename, "w") as f:
        json.dump(data, f, indent=2)
    return filename

def load_game(filename):
    with open(filename, "r") as f:
        data = json.load(f)
    state = GameState(
        turn=data["turn"], difficulty=data["difficulty"], game_mode=data["game_mode"],
        game_over=data["game_over"], winner=data["winner"], history=data["history"],
        last_narrative=data["last_narrative"], weather=data["weather"],
        time_of_day=data.get("time_of_day", "day"), objective_zone=data["objective_zone"],
        mortar_rounds=data["mortar_rounds"], fpv_drones=data["fpv_drones"],
        recon_active=data["recon_active"], terrain_description=data["terrain_description"],
        detected_zones=data["detected_zones"], obstacles=data["obstacles"],
        player_faction=data["player_faction"], enemy_faction=data["enemy_faction"],
        building_damage=data.get("building_damage", 0), elevation_map=data.get("elevation_map", {}),
        enemy_posture=data["enemy_posture"], enemy_ammo_estimate=data["enemy_ammo_estimate"],
        enemy_morale_trend=data["enemy_morale_trend"], visible_damage=data["visible_damage"],
        strategic_scenario=data["strategic_scenario"], civilian_casualties=data["civilian_casualties"],
        global_tension=data["global_tension"], enemy_war_economy=data["enemy_war_economy"],
        threat_matrix=data["threat_matrix"], economic_impact=data["economic_impact"],
        diplomatic_pressure=data["diplomatic_pressure"], enemy_doctrine=data["enemy_doctrine"],
        escalation_risk=data["escalation_risk"], force_effectiveness=data["force_effectiveness"],
        supply_points=data["supply_points"], war_economy=data["war_economy"],
        production_points=data["production_points"], sanctions=data["sanctions"],
        ied_threat=data["ied_threat"], civilian_density=data["civilian_density"],
        media_attention=data["media_attention"], war_crimes_allegations=data["war_crimes_allegations"],
        roe=data["roe"], command_delays=data["command_delays"],
        last_intel_turn=data["last_intel_turn"], last_ew_turn=data["last_ew_turn"],
        sigint_level=data["sigint_level"], jammer_active=data["jammer_active"],
        deception_active=data["deception_active"], cyber_offense=data["cyber_offense"],
        cyber_defense=data["cyber_defense"], space_satellites=data["space_satellites"],
        space_attack=data["space_attack"], influence_ops=data["influence_ops"],
        guerrilla_presence=data["guerrilla_presence"], human_terrain=data["human_terrain"],
        supply_line_active=data.get("supply_line_active", True),
        supply_route_zone=data.get("supply_route_zone", "extreme"),
        prisoners=[]
    )
    for u_data in data["units"]:
        state.units.append(Unit(**u_data))
    for p_data in data.get("prisoners", []):
        state.prisoners.append(Unit(**p_data))
    if "strategic" in data and data["strategic"]:
        state.strategic = StrategicAsset(**data["strategic"])
    if "enemy_strategic" in data and data["enemy_strategic"]:
        state.enemy_strategic = StrategicAsset(**data["enemy_strategic"])
    return state

def list_saves():
    ensure_dir()
    return sorted([f for f in os.listdir(SAVE_DIR) if f.endswith(".json")], reverse=True)
