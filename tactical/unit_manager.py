import random
from game_state import Unit
from factions import FACTIONS

def create_units_for_faction(faction_name, side, start_positions, unit_types=None):
    faction = FACTIONS[faction_name]
    units = []
    if not unit_types:
        available_types = list(faction["unit_templates"].keys())
        unit_types = available_types[:len(start_positions)]
        while len(unit_types) < len(start_positions):
            unit_types.append(unit_types[-1])
    used_last = set()
    for i, (x,y) in enumerate(start_positions[:len(unit_types)]):
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
            ammo=template["ammo"], max_ammo=template["ammo"], armor=template["armor"],
            movement=template["movement"], movement_points=template["movement"],
            accuracy_base=template["accuracy_base"] + faction["accuracy_bonus"],
            suppress_threshold=template["suppress_threshold"], emoji=template["emoji"]
        )
        units.append(unit)
    return units

def resolve_fire(attacker, target, terrain_cover, action_type):
    hit_chance = attacker.accuracy_base - terrain_cover + random.randint(-10,10)
    hit = random.randint(1,100) <= hit_chance
    damage = 0
    if hit:
        damage = random.uniform(15, 35) * (1 - target.armor/100)
        target.strength -= damage
        target.morale -= damage * 0.5
    else:
        target.morale -= 5
    attacker.ammo -= 5
    if target.morale <= attacker.suppress_threshold:
        target.suppressed = True
    if target.strength <= 0:
        target.destroyed = True
    return {"hit": hit, "damage_dealt": damage, "target_morale_loss": target.morale, "suppression_effect": "suppressed" if target.suppressed else "none"}

def resolve_artillery(battery, target_tile, rounds, game_state, cep_meters=50):
    """Artillery with Gaussian scatter."""
    scatter_x = int(random.gauss(0, cep_meters / 50))
    scatter_y = int(random.gauss(0, cep_meters / 50))
    actual_x = target_tile[0] + scatter_x
    actual_y = target_tile[1] + scatter_y
    # Clamp to map bounds
    actual_x = max(0, min(actual_x, len(game_state.map_grid[0])-1))
    actual_y = max(0, min(actual_y, len(game_state.map_grid)-1))
    for unit in game_state.enemy_units:
        if abs(unit.x - actual_x) <= 2 and abs(unit.y - actual_y) <= 2:
            unit.strength -= random.uniform(10, 40)
            if unit.strength <= 0:
                unit.destroyed = True
    return {"assets_launched": rounds, "damage_pct": 30, "impact_tile": (actual_x, actual_y)}
