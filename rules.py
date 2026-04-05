# rules.py - Combat and strategic resolution (Complete)
import random
from config import ZONE_MAP, HIT_CHANCE, DIFFICULTY, COVER_VALUES

def roll_dice(sides=6, count=2):
    return sum(random.randint(1, sides) for _ in range(count))

def add_detection(state, zone):
    zones = ["close", "medium", "long", "extreme"]
    if zone not in zones:
        return
    if zone not in state.detected_zones:
        state.detected_zones.append(zone)
    idx = zones.index(zone)
    if idx > 0:
        adj = zones[idx-1]
        if adj not in state.detected_zones:
            state.detected_zones.append(adj)
    if idx < 3:
        adj = zones[idx+1]
        if adj not in state.detected_zones:
            state.detected_zones.append(adj)

# Ammo types and penetration
AMMO_TYPES = {
    "ball": {"penetration": 1, "damage_dice": "2d6", "suppression": 15},
    "ap": {"penetration": 3, "damage_dice": "2d6", "suppression": 10},
    "he": {"penetration": 0, "damage_dice": "3d6", "suppression": 25}
}

def calculate_penetration(cover_thickness, ammo_type, angle=0):
    pen = AMMO_TYPES[ammo_type]["penetration"]
    if angle > 60:
        pen *= 0.5
    return pen > cover_thickness

def area_suppression(state, zone):
    for unit in state.units:
        if unit.zone == zone and unit.side != "attacker" and unit.is_alive():
            unit.morale -= 10
            if unit.morale < 30:
                unit.status = "suppressed"
    return "The area is saturated with suppressing fire."

def morale_cascade(state, start_unit):
    """Fixed: uses visited set to prevent infinite loops."""
    if start_unit.morale >= 20 or start_unit.status == "routed":
        return None
    queue = [start_unit]
    visited = set()
    messages = []
    while queue:
        u = queue.pop()
        if u.id in visited:
            continue
        visited.add(u.id)
        if u.status == "routed":
            continue
        u.status = "routed"
        messages.append(f"{u.name} panics and routs!")
        zones = ["close", "medium", "long", "extreme"]
        idx = ZONE_MAP[u.zone]
        for offset in [-1, 0, 1]:
            if 0 <= idx + offset < len(zones):
                adj_zone = zones[idx + offset]
                for other in state.units:
                    if other.side == u.side and other.zone == adj_zone and other.is_alive() and other.id not in visited:
                        other.morale -= 15
                        if other.morale < 20 and other.status != "routed":
                            queue.append(other)
    if messages:
        return " " + " ".join(messages)
    return None

def calculate_hit(attacker_zone, target_zone, target_cover, weather, target_suppressed, wound_penalty=0):
    zone_diff = abs(ZONE_MAP[attacker_zone] - ZONE_MAP[target_zone])
    base = HIT_CHANCE.get(zone_diff, 0.1)
    if weather in ["rain", "fog", "night"]:
        base *= 0.8
    if target_suppressed:
        base += 0.15
    base -= target_cover
    base -= wound_penalty
    return max(0.05, min(0.95, base))

def line_of_sight(state, from_zone, to_zone):
    key = f"{from_zone}-{to_zone}"
    return key not in state.obstacles

def resolve_mortar(state, target_zone, rounds=1):
    if state.mortar_rounds < rounds:
        return f"Only {state.mortar_rounds} mortar rounds left."
    state.mortar_rounds -= rounds
    add_detection(state, "long")
    narratives = []
    for _ in range(rounds):
        actual_zone = target_zone
        if random.random() < 0.25:
            zones = ["close", "medium", "long", "extreme"]
            idx = zones.index(target_zone)
            offset = random.choice([-1, 1])
            if 0 <= idx + offset < len(zones):
                actual_zone = zones[idx + offset]
                narratives.append(f"Mortar shells scatter, impacting {actual_zone} zone.")
        targets = [u for u in state.units if u.side == "defender" and u.zone == actual_zone and u.is_alive()]
        if not targets:
            narratives.append(f"Shells land in {actual_zone} zone but find no targets.")
            continue
        target = random.choice(targets)
        damage = roll_dice(6, 3)
        target.hits += damage
        target.morale -= damage + 20
        if target.hits >= 15:
            target.status = "kia"
            narratives.append(f"Direct hit! {target.name} is torn apart.")
        else:
            target.status = "suppressed"
            narratives.append(f"Round impacts near {target.name}. He is pinned.")
        if actual_zone == "medium":
            state.building_damage = min(100, state.building_damage + random.randint(5, 15))
            narratives.append(f"The farmhouse sustains damage ({state.building_damage}%).")
        mc = morale_cascade(state, target)
        if mc:
            narratives.append(mc)
    return " ".join(narratives)

def resolve_fpv_drone(state, target_name=None):
    if state.fpv_drones < 1:
        return "No FPV drones available."
    state.fpv_drones -= 1
    add_detection(state, "long")
    targets = [u for u in state.units if u.side == "defender" and u.is_alive()]
    if not targets:
        return "Drone buzzes overhead but finds no enemy."
    if target_name:
        target = next((u for u in targets if target_name.lower() in u.name.lower()), targets[0])
    else:
        target = random.choice(targets)
    if random.random() < 0.9:
        target.status = "kia"
        target.morale = 0
        narrative = f"FPV drone strikes {target.name}. Eliminated."
    else:
        target.status = "suppressed"
        target.morale -= 30
        narrative = f"Drone detonates close. {target.name} suppressed."
    mc = morale_cascade(state, target)
    if mc:
        narrative += " " + mc
    return narrative

def resolve_infantry_fire(state, side, target_zone):
    attackers = [u for u in state.units if u.side == side and u.is_combat_effective()]
    if not attackers:
        return f"No combat-effective {side} units."
    if side == "attacker":
        for a in attackers:
            add_detection(state, a.zone)
    target_side = "defender" if side == "attacker" else "attacker"
    targets = [u for u in state.units if u.side == target_side and u.zone == target_zone and u.is_alive()]
    if not targets:
        return f"Small arms fire sweeps {target_zone} zone but finds no enemies."
    difficulty_mod = DIFFICULTY[state.difficulty]["enemy_accuracy"] if side == "defender" else 1.0
    narratives = []
    for attacker in attackers[:2]:
        target = random.choice(targets)
        attacker.ammo -= 3 if attacker.type == "lmg" else 1
        if attacker.ammo < 0:
            attacker.ammo = 0
            continue
        wound_penalty = 0
        if attacker.hits > 10:
            wound_penalty = 0.2
        elif attacker.hits > 5:
            wound_penalty = 0.1
        cover = COVER_VALUES.get(target.zone, 0)
        hit_chance = calculate_hit(attacker.zone, target.zone, cover, state.weather, target.status == "suppressed", wound_penalty)
        hit_chance *= difficulty_mod
        cover_thickness = {"close":2, "medium":3, "long":1, "extreme":0}.get(target.zone, 0)
        ammo_type = "ball"
        if calculate_penetration(cover_thickness, ammo_type):
            if random.random() < hit_chance:
                damage = roll_dice(6, 2)
                target.hits += damage
                target.morale -= damage + 10
                if target.hits >= 15:
                    target.status = "kia"
                    attacker.exp += 10
                    attacker.kills += 1
                    narratives.append(f"{attacker.name} kills {target.name}! (+10 exp)")
                else:
                    target.status = "suppressed"
                    narratives.append(f"{attacker.name} hits {target.name} for {damage} damage.")
                mc = morale_cascade(state, target)
                if mc:
                    narratives.append(mc)
            else:
                narratives.append(f"{attacker.name} misses {target.name}.")
        else:
            narratives.append(f"Bullets ricochet off cover. No effect.")
            if random.random() < 0.3:
                area_suppression(state, target_zone)
    return " ".join(narratives)

def resolve_advance(state, target_zone):
    valid_zones = ["close", "medium", "long", "extreme"]
    if target_zone not in valid_zones:
        target_zone = "medium"
    for unit in state.units:
        if unit.side == "attacker" and unit.is_alive():
            unit.zone = target_zone
    return f"Squad advances to {target_zone.upper()} zone."

def resolve_recon(state):
    if state.recon_active > 0:
        return "Recon drone already active."
    defenders = [u for u in state.units if u.side == "defender" and u.is_combat_effective()]
    if defenders and random.random() < 0.3:
        return "Recon drone shot down by enemy fire."
    if state.weather == "night":
        state.recon_active = 2
        return "Recon drone launched but night limits visibility. Enemy positions partially revealed."
    else:
        state.recon_active = 3
        return "Recon drone launched. Enemy positions revealed."

def resolve_surrender(state):
    defenders = [u for u in state.units if u.side == "defender" and u.is_alive()]
    avg_morale = sum(u.morale for u in defenders) / len(defenders) if defenders else 0
    if avg_morale < 30 or len(defenders) <= 2:
        for u in defenders:
            u.status = "routed"
        state.game_over = True
        state.winner = "attacker"
        return "Enemy surrenders! Victory!"
    else:
        return "Enemy refuses to surrender."

def check_victory(state):
    if state.game_over:
        return True
    attackers_alive = [u for u in state.units if u.side == "attacker" and u.is_alive()]
    defenders_alive = [u for u in state.units if u.side == "defender" and u.is_alive()]
    if not attackers_alive:
        state.game_over = True
        state.winner = "defender"
        state.last_narrative = "Your forces are destroyed. Defeat."
        return True
    if not defenders_alive:
        state.game_over = True
        state.winner = "attacker"
        state.last_narrative = "Enemy wiped out. Victory!"
        return True
    defenders_in_obj = [u for u in defenders_alive if u.zone == state.objective_zone]
    attackers_in_obj = [u for u in attackers_alive if u.zone == state.objective_zone]
    if not defenders_in_obj and attackers_in_obj:
        state.game_over = True
        state.winner = "attacker"
        state.last_narrative = f"Objective {state.objective_zone.upper()} secured. Victory!"
        return True
    return False

def apply_command_delay(state, unit_id):
    if state.game_mode == "tactical":
        delay = random.randint(0, 2)
        if delay > 0:
            state.command_delays[unit_id] = delay
            return f"Order to {unit_id} delayed by {delay} turn(s)."
    return None

def recover_suppression(state):
    messages = []
    for unit in state.units:
        if unit.status == "suppressed" and unit.side == "attacker":
            unit.morale += random.randint(1, 5)
            if unit.morale >= 50:
                unit.status = "active"
                messages.append(f"{unit.name} recovers from suppression.")
    return " ".join(messages) if messages else None

def apply_leader_boost(state):
    messages = []
    for unit in state.units:
        if unit.is_leader and unit.is_alive():
            for other in state.units:
                if other.side == unit.side and other.zone == unit.zone and other != unit:
                    old = other.morale
                    other.morale = min(100, other.morale + unit.leadership // 10)
                    if other.morale > old:
                        messages.append(f"{unit.name}'s leadership boosts {other.name}'s morale.")
    return " ".join(messages) if messages else None

def tactical_resupply(state):
    if state.supply_points >= 20:
        state.supply_points -= 20
        for unit in state.units:
            if unit.side == "attacker" and unit.is_alive():
                if unit.type == "rifleman":
                    unit.ammo = min(30, unit.ammo + 15)
                elif unit.type == "lmg":
                    unit.ammo = min(100, unit.ammo + 50)
        return "Resupply complete. Ammo replenished."
    else:
        return f"Insufficient supply points (need 20, have {state.supply_points})."

def change_weather(state):
    if random.random() < 0.1:
        weathers = ["clear", "rain", "fog", "night"]
        new = random.choice([w for w in weathers if w != state.weather])
        state.weather = new
        return f"Weather changes to {new.upper()}."
    return None

def civilian_casualty_check(state, zone):
    if state.civilian_density > 50 and random.random() < 0.3:
        state.media_attention += 10
        state.war_crimes_allegations += 5
        state.global_tension += 5
        return "⚠️ Civilian casualties reported! International outcry."
    return None

def decay_detection(state):
    """Remove detected zones that haven't been refreshed within 2 turns."""
    if not hasattr(state, 'detected_timer'):
        state.detected_timer = {}
    current_turn = state.turn
    new_detected = []
    for zone in state.detected_zones:
        last_seen = state.detected_timer.get(zone, 0)
        if current_turn - last_seen <= 2:
            new_detected.append(zone)
    state.detected_zones = new_detected

def refresh_detection(state, zone):
    """Call when a zone is detected to reset its timer."""
    if not hasattr(state, 'detected_timer'):
        state.detected_timer = {}
    state.detected_timer[zone] = state.turn
    if zone not in state.detected_zones:
        state.detected_zones.append(zone)

def apply_civilian_tension(state):
    increment = state.civilian_casualties // 10000
    if increment > 0:
        state.global_tension = min(100, state.global_tension + increment)
        state.civilian_casualties %= 10000
        return f"Civilian casualties mount. Tension +{increment}."
    return None

# ========== Strategic Actions ==========
def strategic_missile_strike(state, missile_type, target_asset):
    narrative = ""
    if missile_type == "ballistic" and state.strategic.ballistic_missiles > 0:
        state.strategic.ballistic_missiles -= 1
        hit = random.random() < 0.7
        if hit:
            damage = random.randint(1, 5)
            if target_asset == "warships":
                state.enemy_strategic.warships = max(0, state.enemy_strategic.warships - damage)
                narrative = f"Ballistic missile strike hits enemy warships. {damage} sunk."
            elif target_asset == "air_defense":
                state.enemy_strategic.air_defense = max(0, state.enemy_strategic.air_defense - damage*2)
                narrative = "Ballistic missile strike degrades enemy air defense."
            elif target_asset == "missile_silos":
                state.enemy_strategic.missile_silos = max(0, state.enemy_strategic.missile_silos - damage)
                narrative = "Ballistic missile strike hits enemy missile silos."
            else:
                state.enemy_strategic.warships = max(0, state.enemy_strategic.warships - damage)
                narrative = "Ballistic missile strike hits enemy military targets."
        else:
            narrative = "Ballistic missile intercepted or missed."
        state.global_tension += 5
    elif missile_type == "cruise" and state.strategic.cruise_missiles > 0:
        state.strategic.cruise_missiles -= 1
        hit = random.random() < 0.8
        if hit:
            damage = random.randint(1, 10)
            if target_asset == "air_defense":
                state.enemy_strategic.air_defense = max(0, state.enemy_strategic.air_defense - damage)
                narrative = "Cruise missiles overwhelm enemy air defense."
            else:
                state.enemy_strategic.warships = max(0, state.enemy_strategic.warships - damage//2)
                narrative = "Cruise missiles strike enemy positions."
        else:
            narrative = "Cruise missiles shot down."
        state.global_tension += 3
    elif missile_type == "hypersonic" and state.strategic.hypersonic_missiles > 0:
        state.strategic.hypersonic_missiles -= 1
        if random.random() < 0.9:
            state.enemy_strategic.air_defense = max(0, state.enemy_strategic.air_defense - random.randint(10, 25))
            narrative = "Hypersonic missile overwhelms enemy air defense. AD severely degraded."
        else:
            narrative = "Hypersonic missile missed."
        state.global_tension += 4
    elif missile_type == "anti_ship" and state.strategic.anti_ship_missiles > 0:
        state.strategic.anti_ship_missiles -= 1
        if state.enemy_strategic.warships > 0:
            hits = random.randint(1, min(3, state.enemy_strategic.warships))
            state.enemy_strategic.warships -= hits
            narrative = f"Anti‑ship missiles sink {hits} enemy warships."
        else:
            narrative = "No enemy warships to target."
        state.global_tension += 3
    else:
        narrative = "No missiles of that type available."
    return narrative

def strategic_naval_engagement(state):
    if state.strategic.warships > 0 and state.enemy_strategic.warships > 0:
        our_loss = random.randint(0, min(3, state.strategic.warships))
        enemy_loss = random.randint(0, min(3, state.enemy_strategic.warships))
        state.strategic.warships -= our_loss
        state.enemy_strategic.warships -= enemy_loss
        return f"Naval engagement: we lost {our_loss} ships, enemy lost {enemy_loss} ships."
    elif state.strategic.warships > 0:
        return "Enemy has no warships. We control the sea."
    else:
        return "No warships available."

def strategic_loitering_strike(state):
    if state.strategic.loitering_munitions > 0:
        state.strategic.loitering_munitions -= 1
        if random.random() < 0.8:
            if state.enemy_strategic.air_defense > 0:
                state.enemy_strategic.air_defense -= random.randint(5, 15)
                return "Loitering munitions saturate enemy air defenses. Their AD degraded."
            else:
                state.enemy_strategic.warships -= random.randint(1, 3)
                return "Kamikaze drones strike enemy warships. Naval losses."
        else:
            return "Loitering munitions intercepted or jammed."
    else:
        return "No loitering munitions available."

def strategic_cyber_attack(state):
    if state.strategic.cyber_warfare > 0:
        effect = random.randint(10, 30)
        state.enemy_strategic.intelligence_level = max(0, state.enemy_strategic.intelligence_level - effect)
        state.enemy_strategic.electronic_warfare = max(0, state.enemy_strategic.electronic_warfare - effect)
        state.global_tension += 1
        return f"Cyber attack cripples enemy C4ISR. Intel and EW reduced by {effect}."
    else:
        return "No cyber warfare assets."

def strategic_space_deployment(state):
    if state.strategic.space_assets > 0:
        state.strategic.intelligence_level = min(100, state.strategic.intelligence_level + 10)
        state.enemy_strategic.space_assets = max(0, state.enemy_strategic.space_assets - random.randint(1, 5))
        state.global_tension += 1
        return "Space assets provide real‑time targeting data. Our intel improved."
    else:
        return "No space assets available."

def strategic_psyops(state):
    if state.influence_ops > 0:
        state.enemy_strategic.intelligence_level = max(0, state.enemy_strategic.intelligence_level - 15)
        state.global_tension += 1
        return "Psychological operations sow confusion among enemy ranks. Their intel reduced."
    else:
        return "No psyops capacity."

def strategic_resupply(state):
    if state.supply_points >= 20:
        state.supply_points -= 20
        return "Resupply convoy arrives. Strategic assets replenished."
    else:
        return f"Insufficient supply points (need 20, have {state.supply_points})."

def strategic_produce_missiles(state):
    if state.production_points >= 20:
        state.production_points -= 20
        state.strategic.ballistic_missiles += 1
        return "Production facility outputs new ballistic missiles."
    else:
        return "Insufficient production points."

def strategic_sanctions(state):
    if state.diplomatic_pressure:
        state.sanctions += 10
        state.enemy_war_economy = max(0, state.enemy_war_economy - 5)
        return "Sanctions imposed on enemy. Their war economy suffers."
    else:
        return "Diplomatic climate not suitable for sanctions."

def strategic_nuclear_strike(state):
    if state.strategic.nukes > 0:
        state.strategic.nukes -= 1
        state.global_tension += 50
        state.civilian_casualties += 100000
        narrative = "💥 NUCLEAR DETONATION. The world will never be the same. You have crossed the ultimate line."
        if state.global_tension >= 100:
            state.game_over = True
            state.winner = "nobody"
            narrative += " Global thermonuclear war ensues. Civilization collapses."
        return narrative
    else:
        return "No nuclear weapons available."
