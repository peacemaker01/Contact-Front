# enemy_ai.py
import random
import re
import json
import requests
from config import ZONE_MAP, DIFFICULTY
from rules import resolve_infantry_fire, area_suppression, morale_cascade

def llm_suggest_target_zone(state, api_key, provider="openrouter"):
    if not api_key or not state.detected_zones:
        return None
    prompt = f"""You are an enemy commander. Based on detected enemy zones: {state.detected_zones}, choose ONE zone (close/medium/long/extreme) to fire at. Return only JSON: {{"zone": "zone_name"}}."""
    try:
        if provider == "openrouter":
            url = "https://openrouter.ai/api/v1/chat/completions"
            headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
            payload = {"model": "openai/gpt-4o-mini", "messages": [{"role": "user", "content": prompt}], "max_tokens": 30}
            resp = requests.post(url, json=payload, headers=headers, timeout=3)
            if resp.status_code == 200:
                data = resp.json()
                content = data["choices"][0]["message"]["content"]
                match = re.search(r'\{.*\}', content)
                if match:
                    return json.loads(match.group(0)).get("zone")
    except:
        pass
    return None

def apply_guerrilla_tactics(state):
    for unit in state.units:
        if unit.side == "defender" and unit.faction == "IRAN" and unit.is_combat_effective():
            if random.random() < 0.2:
                zones = ["close", "medium", "long", "extreme"]
                current_idx = ZONE_MAP[unit.zone]
                new_idx = max(0, min(3, current_idx + random.choice([-1, 1])))
                unit.zone = zones[new_idx]
                return f"Guerrilla unit {unit.name} repositions to {unit.zone}!"
    return None

def process_enemy_turn(state, provider="openrouter", api_key=None):
    # Asymmetric guerrilla tactics
    guerrilla_note = apply_guerrilla_tactics(state)
    defenders = [u for u in state.units if u.side == "defender" and u.is_combat_effective()]
    if not defenders:
        return "The enemy positions are silent. No one fires back."

    valid_zones = set(state.detected_zones)
    attacker_zones = set(u.zone for u in state.units if u.side == "attacker" and u.is_alive())
    valid_zones = valid_zones.intersection(attacker_zones)

    if not valid_zones:
        return "Enemy scans but sees no targets. They hold fire."

    suggested = llm_suggest_target_zone(state, api_key, provider)
    target_zone = suggested if suggested in valid_zones else random.choice(list(valid_zones))

    narratives = []
    for defender in defenders:
        if not defender.is_combat_effective():
            continue
        if defender.morale < 20:
            zones = ["close", "medium", "long", "extreme"]
            idx = zones.index(defender.zone)
            if idx < 3:
                defender.zone = zones[idx + 1]
                narratives.append(f"{defender.name} breaks and runs toward {defender.zone}!")
            else:
                defender.status = "routed"
                narratives.append(f"{defender.name} throws down his weapon and flees.")
            continue
        result = resolve_infantry_fire(state, "defender", target_zone)
        narratives.append(result)
        # area suppression may happen inside resolve_infantry_fire
    if defenders and (sum(d.morale for d in defenders) / len(defenders)) < 40:
        narratives.append("Panicked shouts from enemy lines. Their morale is breaking.")
    if guerrilla_note:
        narratives.append(guerrilla_note)
    return " ".join(narratives)

def process_strategic_enemy_turn(state):
    if state.game_mode != "strategic":
        return ""
    narratives = []
    enemy = state.enemy_strategic
    player = state.strategic
    aggression = DIFFICULTY.get(state.difficulty, {}).get("strategic_aggression", 0.4)

    # Ballistic missile retaliation
    if enemy.ballistic_missiles > 0 and state.global_tension > 50:
        if random.random() < aggression:
            enemy.ballistic_missiles -= 1
            dmg = random.randint(1, 5)
            player.warships = max(0, player.warships - dmg)
            narratives.append(f"Enemy ballistic missile strike! {dmg} of our warships sunk.")
            state.global_tension += 5
            state.civilian_casualties += random.randint(0, 1000)
    # Cruise missiles
    if enemy.cruise_missiles > 0 and state.global_tension > 30:
        if random.random() < aggression * 0.8:
            enemy.cruise_missiles -= 1
            dmg = random.randint(1, 10)
            player.air_defense = max(0, player.air_defense - dmg)
            narratives.append(f"Enemy cruise missiles overwhelm our air defense! -{dmg} AD.")
            state.global_tension += 3
    # Naval engagement
    if player.warships > 0 and enemy.warships > 0 and random.random() < aggression * 1.2:
        our_loss = random.randint(0, min(3, player.warships))
        enemy_loss = random.randint(0, min(3, enemy.warships))
        player.warships -= our_loss
        enemy.warships -= enemy_loss
        narratives.append(f"Naval battle: we lost {our_loss} ships, enemy lost {enemy_loss} ships.")
        state.global_tension += 2
    # Electronic warfare
    if enemy.electronic_warfare > 0 and state.global_tension > 20:
        if random.random() < aggression * 0.6:
            player.intelligence_level = max(0, player.intelligence_level - 10)
            narratives.append("Enemy electronic warfare degrades our intelligence!")
            state.global_tension += 1
    # Nuclear escalation
    if enemy.nukes > 0 and state.global_tension > 80 and random.random() < aggression * 0.3:
        enemy.nukes -= 1
        state.civilian_casualties += 50000
        state.global_tension += 50
        narratives.append("💥 ENEMY NUCLEAR STRIKE! Millions dead. Global thermonuclear war imminent.")
        if state.global_tension >= 100:
            state.game_over = True
            state.winner = "nobody"
            narratives.append("The world ends in nuclear fire.")
        return " ".join(narratives)
    if not narratives:
        narratives.append("Enemy forces are watching. No immediate retaliation.")
    return " ".join(narratives)
