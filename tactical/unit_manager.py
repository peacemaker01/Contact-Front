import random
from game_state import Unit
from factions import FACTIONS

def create_units_for_faction(faction_name, side, start_positions, unit_types=None):
    faction = FACTIONS[faction_name]
    units = []
    if not unit_types:
        available_types = list(faction["unit_templates"].keys())
        available_types = [t for t in available_types if t not in ["recon_drone", "fpv_kamikaze"]]
        unit_types = available_types[:len(start_positions)]
        while len(unit_types) < len(start_positions):
            unit_types.append(unit_types[-1])
    used_last = set()
    for i, (x, y) in enumerate(start_positions[:len(unit_types)]):
        template_key = unit_types[i % len(unit_types)]
        if template_key not in faction["unit_templates"]:
            template_key = list(faction["unit_templates"].keys())[0]
        template = faction["unit_templates"][template_key]
        rank = template.get("rank", "Soldier")
        avail = [ln for ln in faction["last_names"] if ln not in used_last]
        if not avail:
            avail = faction["last_names"]
        last = random.choice(avail)
        used_last.add(last)
        full_name = f"{rank} {last}"
        unit = Unit(
            id=i+1, name=full_name, type=template_key, type_code=template["type_code"],
            faction=faction_name, x=x, y=y, strength=100, morale=template["base_morale"],
            ammo_he=template["ammo_he"], ammo_ap=template["ammo_ap"],
            max_ammo_he=template["ammo_he"], max_ammo_ap=template["ammo_ap"],
            armor=template["armor"],
            movement=template["movement"], movement_points=template["movement"],
            accuracy_base=template["accuracy_base"] + faction["accuracy_bonus"],
            suppress_threshold=template["suppress_threshold"], emoji=template["emoji"],
            range_tiles=template.get("range_tiles", 15),
            radio_range=10
        )
        units.append(unit)

    drone_templates = ["recon_drone", "fpv_kamikaze"]
    next_id = len(units) + 1
    for idx, dt in enumerate(drone_templates):
        if dt in faction["unit_templates"]:
            template = faction["unit_templates"][dt]
            start_x = start_positions[0][0]
            start_y = max(0, start_positions[0][1] - 1 - idx)
            drone = Unit(
                id=next_id, name=f"{dt.replace('_',' ').title()}", type=dt, type_code=template["type_code"],
                faction=faction_name, x=start_x, y=start_y, strength=100, morale=100,
                ammo_he=template["ammo_he"], ammo_ap=template["ammo_ap"],
                max_ammo_he=template["ammo_he"], max_ammo_ap=template["ammo_ap"],
                armor=0,
                movement=template["movement"], movement_points=template["movement"],
                accuracy_base=template["accuracy_base"], suppress_threshold=0,
                emoji=template["emoji"], range_tiles=template.get("range_tiles", 0),
                radio_range=10,
                is_spotter=(dt == "recon_drone")
            )
            units.append(drone)
            next_id += 1
    return units

def resolve_fire(attacker, target, terrain_cover, action_type, game_state):
    faction_bonus = FACTIONS.get(attacker.faction, {}).get("accuracy_bonus", 0)
    hit_chance = attacker.accuracy_base + faction_bonus - terrain_cover + random.randint(-10,10)
    hit = random.randint(1,100) <= hit_chance
    damage = 0
    was_destroyed = False
    if hit:
        is_armored = target.armor > 50
        if is_armored:
            if attacker.ammo_ap <= 0:
                hit = False
            else:
                attacker.ammo_ap -= 1
                damage_mult = 1.2
        else:
            if attacker.ammo_he <= 0:
                hit = False
            else:
                attacker.ammo_he -= 1
                damage_mult = 0.8
        if hit:
            damage = random.uniform(15, 35) * damage_mult * (1 - target.armor/100)
            target.strength -= damage
            target.morale -= damage * 0.5
            if target.strength > 0 and target.faction == game_state.player_faction:
                game_state.friendly_wia += 1
    else:
        target.morale -= 5
    if target.morale <= attacker.suppress_threshold:
        target.suppressed = True
    if target.strength <= 0 and not target.destroyed:
        target.destroyed = True
        was_destroyed = True
        if target.faction == game_state.player_faction:
            game_state.friendly_kia += 1
            if target.type in ["m1a2_tank", "t90m_tank", "bradley_ifv", "bmp3_ifv", "type99a_tank", "t72s_tank"]:
                game_state.vehicles_lost += 1
        else:
            game_state.enemy_kia += 1
    return {"hit": hit, "damage_dealt": damage, "target_morale_loss": target.morale, "suppression_effect": "suppressed" if target.suppressed else "none", "destroyed": was_destroyed}

def resolve_artillery(battery, target_tile, rounds, game_state, cep_meters=50):
    scatter_x = int(random.gauss(0, cep_meters / 50))
    scatter_y = int(random.gauss(0, cep_meters / 50))
    actual_x = target_tile[0] + scatter_x
    actual_y = target_tile[1] + scatter_y
    actual_x = max(0, min(actual_x, len(game_state.map_grid[0])-1))
    actual_y = max(0, min(actual_y, len(game_state.map_grid)-1))
    for unit in game_state.enemy_units:
        if abs(unit.x - actual_x) <= 2 and abs(unit.y - actual_y) <= 2:
            unit.strength -= random.uniform(10, 40)
            if unit.strength <= 0 and not unit.destroyed:
                unit.destroyed = True
                game_state.enemy_kia += 1
    return {"assets_launched": rounds, "damage_pct": 30, "impact_tile": (actual_x, actual_y)}

def check_rout(unit, game_state):
    if unit.morale <= 15 and not unit.destroyed and not unit.is_routed:
        unit.is_routed = True
        min_dist = float('inf')
        nearest = None
        for enemy in game_state.enemy_units:
            if enemy.destroyed:
                continue
            d = abs(unit.x - enemy.x) + abs(unit.y - enemy.y)
            if d < min_dist:
                min_dist = d
                nearest = enemy
        if nearest:
            dx = unit.x - nearest.x
            dy = unit.y - nearest.y
            if dx != 0: dx = dx // abs(dx)
            if dy != 0: dy = dy // abs(dy)
            new_x = unit.x + dx * 3
            new_y = unit.y + dy * 3
            new_x = max(0, min(new_x, len(game_state.map_grid[0])-1))
            new_y = max(0, min(new_y, len(game_state.map_grid)-1))
            unit.x, unit.y = new_x, new_y
        unit.suppressed = True
        return True
    return False

def apply_ew_effects(game_state):
    pass
