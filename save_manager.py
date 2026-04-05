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
        "turn": state.turn, "difficulty": state.difficulty, "game_mode": state.game_mode,
        "game_over": state.game_over, "winner": state.winner, "history": state.history,
        "last_narrative": state.last_narrative, "weather": state.weather,
        "objective_zone": state.objective_zone, "mortar_rounds": state.mortar_rounds,
        "fpv_drones": state.fpv_drones, "recon_active": state.recon_active,
        "terrain_description": state.terrain_description, "detected_zones": state.detected_zones,
        "obstacles": state.obstacles, "player_faction": state.player_faction, "enemy_faction": state.enemy_faction,
        "enemy_posture": state.enemy_posture, "enemy_ammo_estimate": state.enemy_ammo_estimate,
        "enemy_morale_trend": state.enemy_morale_trend, "visible_damage": state.visible_damage,
        "strategic_scenario": state.strategic_scenario, "civilian_casualties": state.civilian_casualties,
        "global_tension": state.global_tension, "threat_matrix": state.threat_matrix,
        "economic_impact": state.economic_impact, "diplomatic_pressure": state.diplomatic_pressure,
        "enemy_doctrine": state.enemy_doctrine, "escalation_risk": state.escalation_risk,
        "force_effectiveness": state.force_effectiveness, "supply_points": state.supply_points,
        "war_economy": state.war_economy, "production_points": state.production_points,
        "sanctions": state.sanctions, "ied_threat": state.ied_threat,
        "command_delays": state.command_delays, "last_intel_turn": state.last_intel_turn,
        "last_ew_turn": state.last_ew_turn, "sigint_level": state.sigint_level,
        "jammer_active": state.jammer_active, "deception_active": state.deception_active,
        "cyber_offense": state.cyber_offense, "cyber_defense": state.cyber_defense,
        "space_satellites": state.space_satellites, "space_attack": state.space_attack,
        "influence_ops": state.influence_ops, "guerrilla_presence": state.guerrilla_presence,
        "civilian_density": state.civilian_density, "human_terrain": state.human_terrain,
        "media_attention": state.media_attention, "war_crimes_allegations": state.war_crimes_allegations,
        "units": []
    }
    for u in state.units:
        data["units"].append({
            "id": u.id, "name": u.name, "type": u.type, "side": u.side,
            "zone": u.zone, "faction": u.faction, "status": u.status,
            "morale": u.morale, "ammo": u.ammo, "hits": u.hits, "description": u.description,
            "leadership": u.leadership, "supply": u.supply, "is_leader": u.is_leader
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
            "space_assets": state.strategic.space_assets
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
            "space_assets": state.enemy_strategic.space_assets
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
        objective_zone=data["objective_zone"], mortar_rounds=data["mortar_rounds"],
        fpv_drones=data["fpv_drones"], recon_active=data["recon_active"],
        terrain_description=data["terrain_description"], detected_zones=data["detected_zones"],
        obstacles=data["obstacles"], player_faction=data["player_faction"], enemy_faction=data["enemy_faction"],
        enemy_posture=data["enemy_posture"], enemy_ammo_estimate=data["enemy_ammo_estimate"],
        enemy_morale_trend=data["enemy_morale_trend"], visible_damage=data["visible_damage"],
        strategic_scenario=data["strategic_scenario"], civilian_casualties=data["civilian_casualties"],
        global_tension=data["global_tension"], threat_matrix=data["threat_matrix"],
        economic_impact=data["economic_impact"], diplomatic_pressure=data["diplomatic_pressure"],
        enemy_doctrine=data["enemy_doctrine"], escalation_risk=data["escalation_risk"],
        force_effectiveness=data["force_effectiveness"], supply_points=data["supply_points"],
        war_economy=data["war_economy"], production_points=data["production_points"],
        sanctions=data["sanctions"], ied_threat=data["ied_threat"], command_delays=data["command_delays"],
        last_intel_turn=data["last_intel_turn"], last_ew_turn=data["last_ew_turn"],
        sigint_level=data["sigint_level"], jammer_active=data["jammer_active"],
        deception_active=data["deception_active"], cyber_offense=data["cyber_offense"],
        cyber_defense=data["cyber_defense"], space_satellites=data["space_satellites"],
        space_attack=data["space_attack"], influence_ops=data["influence_ops"],
        guerrilla_presence=data["guerrilla_presence"], civilian_density=data["civilian_density"],
        human_terrain=data["human_terrain"], media_attention=data["media_attention"],
        war_crimes_allegations=data["war_crimes_allegations"]
    )
    for u_data in data["units"]:
        state.units.append(Unit(**u_data))
    if "strategic" in data and data["strategic"]:
        state.strategic = StrategicAsset(**data["strategic"])
    if "enemy_strategic" in data and data["enemy_strategic"]:
        state.enemy_strategic = StrategicAsset(**data["enemy_strategic"])
    return state

def list_saves():
    ensure_dir()
    return sorted([f for f in os.listdir(SAVE_DIR) if f.endswith(".json")], reverse=True)
