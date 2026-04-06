# scenarios.py
import os
import json
import re
import random
import requests
from models import GameState, Unit, StrategicAsset
from config import DIFFICULTY
from factions import FACTIONS

# ----------------------------------------------------------------------
# Strategic scenario generator
# ----------------------------------------------------------------------
def generate_strategic_scenario(scenario_name, difficulty, player_faction, enemy_faction):
    state = GameState(game_mode="strategic", difficulty=difficulty, player_faction=player_faction, enemy_faction=enemy_faction)

    def assign_assets(faction):
        assets = FACTIONS[faction]["assets"]
        return StrategicAsset(
            nukes=assets["nukes"],
            ballistic_missiles=assets["ballistic_missiles"],
            cruise_missiles=assets["cruise_missiles"],
            warships=assets["warships"],
            intelligence_level=assets["intelligence_level"],
            air_defense=assets["air_defense"],
            electronic_warfare=assets["electronic_warfare"],
            hypersonic_missiles=assets["hypersonic_missiles"],
            loitering_munitions=assets["loitering_munitions"],
            anti_ship_missiles=assets["anti_ship_missiles"],
            cyber_warfare=assets["cyber_warfare"],
            space_assets=assets["space_assets"],
            missile_silos=assets.get("missile_silos", 30),
            infrastructure=assets.get("infrastructure", 100),
            command_centers=assets.get("command_centers", 20),
            nuclear_sites=assets.get("nuclear_sites", 10)
        )
    state.strategic = assign_assets(player_faction)
    state.enemy_strategic = assign_assets(enemy_faction)
    state.enemy_war_economy = 50
    state.player_side = "attacker"   # strategic mode always attacker

    # Scenario-specific modifications
    if scenario_name == "Strait of Hormuz":
        state.terrain_description = "Narrow strait crowded with oil tankers. Iranian fast attack craft lurk near islands."
        state.strategic.warships = min(state.strategic.warships, 10)
        state.enemy_strategic.warships = 50
        state.last_narrative = "Tensions in the Gulf. Iranian forces threaten to close the strait. Ensure freedom of navigation without triggering a full war."
    elif scenario_name == "Missile Exchange":
        state.terrain_description = "Regional conflict escalates. Both sides have launched conventional missile strikes."
        state.strategic.ballistic_missiles = max(0, state.strategic.ballistic_missiles - 50)
        state.enemy_strategic.ballistic_missiles = max(0, state.enemy_strategic.ballistic_missiles - 50)
        state.last_narrative = "The first salvo has been exchanged. Civilian areas are at risk. De‑escalate or retaliate?"
    elif scenario_name == "Nuclear Brinkmanship":
        state.terrain_description = "A nuclear‑capable power threatens use of atomic weapons. Global tension at an all‑time high."
        state.global_tension = 85
        if player_faction == "IRAN":
            state.strategic.nukes = 1
        state.last_narrative = "Intelligence suggests the enemy is preparing a tactical nuclear strike. Your move could decide the fate of millions."

    # Initialise strategic economy and intel fields
    state.supply_points = 100
    state.war_economy = 50
    state.production_points = 10
    state.sanctions = 0
    state.ied_threat = random.randint(0, 30)
    state.civilian_density = random.randint(20, 80)
    state.media_attention = 10
    state.war_crimes_allegations = 0
    state.roe = "restricted"
    return state

# ----------------------------------------------------------------------
# LLM scenario generator (placeholder)
# ----------------------------------------------------------------------
def generate_scenario_via_llm(mission_type, difficulty, provider, api_key):
    if not api_key:
        return None
    # Your existing LLM prompt code here. For brevity, return None.
    return None

def build_state_from_llm_data(data, difficulty):
    # Placeholder
    return GameState(difficulty=difficulty)

# ----------------------------------------------------------------------
# Static tactical scenarios – now supports attacker/defender
# ----------------------------------------------------------------------
def generate_broken_anvil_static(difficulty, player_faction="NATO", enemy_faction="RUSSIA", player_side="attacker"):
    state = GameState(difficulty=difficulty, weather="fog", objective_zone="medium", mortar_rounds=6, fpv_drones=2)
    state.terrain_description = "Ruins of Donetsk airport: broken concrete, burned vehicles, a standing control tower. Fog reduces visibility."
    state.obstacles = ["close-medium"]
    state.player_faction = player_faction
    state.enemy_faction = enemy_faction
    state.player_side = player_side

    # Determine zones based on player side
    if player_side == "attacker":
        player_start_zone = "long"
        enemy_start_zone = "medium"
    else:  # defender
        player_start_zone = "medium"   # defending the objective
        enemy_start_zone = "long"      # attackers start further away

    # Player units (6 units: 4 rifle + 2 LMG)
    faction_data = FACTIONS[player_faction]
    ranks = faction_data["ranks"]
    names = faction_data["personnel_names"]
    player_unit_types = [
        ("rifleman", 0, 0, 30, 85),
        ("rifleman", 1, 1, 30, 80),
        ("rifleman", 2, 2, 30, 75),
        ("rifleman", 3, 3, 30, 75),
        ("lmg",      4, 4, 80, 85),
        ("lmg",      0, 5, 80, 80),
    ]
    for i, (utype, rank_idx, name_idx, ammo, morale) in enumerate(player_unit_types):
        full_name = f"{ranks[rank_idx % len(ranks)]} {names[name_idx % len(names)]}"
        state.units.append(Unit(id=f"att_{i}", name=full_name, type=utype, side=player_side,
                                zone=player_start_zone, ammo=ammo, morale=morale, faction=player_faction))

    # Enemy units – extra militia, RPG gunner
    mp = DIFFICULTY[difficulty]["enemy_morale_penalty"]
    enemy_units = [
        ("Militia 1", "rifleman", 30, 90+mp, "Experienced fighter"),
        ("Militia 2", "rifleman", 30, 90+mp, "Hidden in rubble"),
        ("Militia 3", "rifleman", 30, 85+mp, "Fanatical"),
        ("Militia 4", "rifleman", 30, 85+mp, "Nervous but determined"),
        ("Heavy Gunner", "lmg", 150, 110+mp, "PKM gunner in terminal window", 5),
        ("RPG Gunner", "rifleman", 30, 95+mp, "Carries RPG-7", 0, ["rpg"]),
    ]
    for i, (name, typ, ammo, morale, desc, *extra) in enumerate(enemy_units):
        unit = Unit(id=f"def_{i}", name=name, type=typ, side=("attacker" if player_side == "defender" else "defender"),
                    zone=enemy_start_zone, ammo=ammo, morale=morale, description=desc, faction=enemy_faction)
        if len(extra) > 0 and isinstance(extra[0], int):
            unit.hits = extra[0]
        if len(extra) > 1:
            unit.special_equipment = extra[1]
        state.units.append(unit)

    # Initialise fields
    state.building_damage = 0
    state.supply_points = 80
    state.war_economy = 50
    state.production_points = 10
    state.sanctions = 0
    state.ied_threat = random.randint(10, 40)
    state.civilian_density = random.randint(10, 60)
    state.media_attention = 0
    state.war_crimes_allegations = 0
    state.roe = "restricted"
    if player_side == "attacker":
        state.last_narrative = f"Fog clings to the ruins. Your {player_faction} squad prepares to assault the terminal held by {enemy_faction} separatists. Mortars are limited – use them wisely."
    else:
        state.last_narrative = f"Fog clings to the ruins. Your {player_faction} squad defends the terminal against {enemy_faction} attackers. Hold the line."
    return state

def generate_night_raid_static(difficulty, player_faction="NATO", enemy_faction="RUSSIA", player_side="attacker"):
    state = GameState(difficulty=difficulty, weather="night", objective_zone="close", mortar_rounds=0, fpv_drones=2)
    state.terrain_description = "Mosul compound: narrow alleys, collapsed buildings, a mosque minaret. Moonless night."
    state.obstacles = []
    state.player_faction = player_faction
    state.enemy_faction = enemy_faction
    state.player_side = player_side

    if player_side == "attacker":
        player_start_zone = "long"
        enemy_start_zone = "medium"
    else:
        player_start_zone = "medium"   # defender holds the compound
        enemy_start_zone = "long"

    # Player units – 4 operators
    faction_data = FACTIONS[player_faction]
    ranks = faction_data["ranks"]
    names = faction_data["personnel_names"]
    for i in range(4):
        full_name = f"{ranks[i % len(ranks)]} {names[i % len(names)]}"
        state.units.append(Unit(id=f"sog_{i}", name=full_name, type="rifleman", side=player_side,
                                zone=player_start_zone, ammo=45, morale=95, faction=player_faction))

    # Enemy units – 3 guards + sniper
    mp = DIFFICULTY[difficulty]["enemy_morale_penalty"]
    enemy_units = [
        ("Militant", "rifleman", 30, 80+mp, "Patrol"),
        ("Militant", "rifleman", 30, 80+mp, "Lookout"),
        ("Militant", "rifleman", 30, 80+mp, "Guard"),
        ("Sniper", "rifleman", 20, 90+mp, "Hidden in minaret", 0, ["sniper"]),
    ]
    for i, (name, typ, ammo, morale, desc, *extra) in enumerate(enemy_units):
        zone = "close" if i == 3 else enemy_start_zone
        unit = Unit(id=f"def_{i}", name=name, type=typ, side=("attacker" if player_side == "defender" else "defender"),
                    zone=zone, ammo=ammo, morale=morale, description=desc, faction=enemy_faction)
        if len(extra) > 1:
            unit.special_equipment = extra[1]
        state.units.append(unit)

    # Initialise fields
    state.building_damage = 0
    state.supply_points = 100
    state.war_economy = 50
    state.production_points = 10
    state.sanctions = 0
    state.ied_threat = random.randint(0, 30)
    state.civilian_density = random.randint(10, 60)
    state.media_attention = 0
    state.war_crimes_allegations = 0
    state.roe = "restricted"
    if player_side == "attacker":
        state.last_narrative = f"Moonless night. Your {player_faction} operators move silently. The compound is held by {enemy_faction} militants. A sniper is in the minaret."
    else:
        state.last_narrative = f"Moonless night. Your {player_faction} squad defends the compound against {enemy_faction} attackers. The sniper is your ally."
    return state

def generate_mission(mission_type, difficulty, provider, api_key, player_faction, enemy_faction, player_side="attacker"):
    llm_data = generate_scenario_via_llm(mission_type, difficulty, provider, api_key)
    if llm_data:
        state = build_state_from_llm_data(llm_data, difficulty)
        state.player_faction = player_faction
        state.enemy_faction = enemy_faction
        state.player_side = player_side
    else:
        if mission_type == "night_raid":
            state = generate_night_raid_static(difficulty, player_faction, enemy_faction, player_side)
        else:
            state = generate_broken_anvil_static(difficulty, player_faction, enemy_faction, player_side)
    return state

# Backward compatibility
def generate_broken_anvil(difficulty, provider=None, api_key=None, player_faction="NATO", enemy_faction="RUSSIA", player_side="attacker"):
    return generate_mission("broken_anvil", difficulty, provider, api_key, player_faction, enemy_faction, player_side)

def generate_night_raid(difficulty, provider=None, api_key=None, player_faction="NATO", enemy_faction="RUSSIA", player_side="attacker"):
    return generate_mission("night_raid", difficulty, provider, api_key, player_faction, enemy_faction, player_side)
