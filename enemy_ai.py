# enemy_ai.py - LLM-driven tactical AI
import random
import re
import json
import requests
from config import ZONE_MAP, DIFFICULTY
from rules import resolve_infantry_fire, resolve_rpg_attack, resolve_sniper_attack, area_suppression, morale_cascade

def llm_enemy_decision(state, defender, api_key, provider="openrouter"):
    """Ask LLM for full tactical decision: action, target zone, etc."""
    if not api_key:
        return None

    # Build rich context
    detected = list(state.detected_zones)
    attacker_zones = list(set(u.zone for u in state.units if u.side == state.player_side and u.is_alive()))
    avg_enemy_morale = sum(u.morale for u in state.units if u.side != state.player_side and u.is_alive()) / max(1, len([u for u in state.units if u.side != state.player_side and u.is_alive()]))
    special = getattr(defender, "special_equipment", [])
    special_str = ", ".join(special) if special else "none"

    # Add nearby enemies and allies for more context
    same_zone_allies = [u.name for u in state.units if u.side == defender.side and u.zone == defender.zone and u.is_alive() and u.id != defender.id]
    same_zone_enemies = [u.name for u in state.units if u.side == state.player_side and u.zone == defender.zone and u.is_alive()]

    prompt = f"""You are an enemy commander in a tactical wargame. Decide the best action for this unit.

**Unit**: {defender.name}
- Role: {defender.type}
- Morale: {defender.morale}/100
- Ammo: {defender.ammo}
- Position: {defender.zone}
- Special equipment: {special_str}
- Status: {defender.status}
- Health (hits): {defender.hits}/? (>=15 = dead)

**Battlefield**:
- Weather: {state.weather}
- Objective zone: {state.objective_zone}
- Detected enemy zones: {detected if detected else 'none'}
- Known enemy positions (if recon active): {attacker_zones if state.recon_active > 0 else 'unknown'}
- Average enemy morale: {avg_enemy_morale:.0f}
- Same zone allies: {same_zone_allies if same_zone_allies else 'none'}
- Same zone enemies: {same_zone_enemies if same_zone_enemies else 'none'}

**Available actions**:
- "fire" (shoot at a target zone) – must be in detected zones.
- "move" (change zone to close/medium/long/extreme) – cannot if suppressed.
- "hide" (take cover, become suppressed voluntarily).
- "retreat" (move one zone away from objective).
- "rpg" (if has RPG) – high damage, less accurate.
- "sniper" (if has sniper) – high accuracy, high damage.

Return ONLY valid JSON:
{{
    "action": "fire|move|hide|retreat|rpg|sniper",
    "target_zone": "close|medium|long|extreme|null",
    "reasoning": "short reason"
}}"""
    try:
        url = "https://openrouter.ai/api/v1/chat/completions"
        headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
        payload = {
            "model": "openai/gpt-4o-mini",
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.4,
            "max_tokens": 150
        }
        resp = requests.post(url, json=payload, headers=headers, timeout=5)
        if resp.status_code == 200:
            data = resp.json()
            content = data["choices"][0]["message"]["content"]
            match = re.search(r'\{.*\}', content, re.DOTALL)
            if match:
                decision = json.loads(match.group(0))
                if decision.get("action") not in ["fire", "move", "hide", "retreat", "rpg", "sniper"]:
                    return None
                return decision
    except Exception:
        pass
    return None

def apply_guerrilla_tactics(state):
    for unit in state.units:
        if (unit.side != state.player_side and unit.faction == "IRAN"
                and unit.is_combat_effective()):
            if random.random() < 0.2:
                zones = ["close", "medium", "long", "extreme"]
                current_idx = ZONE_MAP[unit.zone]
                new_idx = max(0, min(3, current_idx + random.choice([-1, 1])))
                unit.zone = zones[new_idx]
                return f"Guerrilla unit {unit.name} repositions to {unit.zone}!"
    return None

def process_enemy_turn(state, provider="openrouter", api_key=None):
    guerrilla_note = apply_guerrilla_tactics(state)

    defenders = [u for u in state.units if u.side != state.player_side and u.is_combat_effective()]
    if not defenders:
        return "The enemy positions are silent. No one fires back."

    valid_zones = set(state.detected_zones)
    attacker_zones = set(u.zone for u in state.units if u.side == state.player_side and u.is_alive())
    valid_zones = valid_zones.intersection(attacker_zones)

    narratives = []

    for defender in defenders:
        if not defender.is_combat_effective():
            continue

        # Low morale – force retreat (override LLM)
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

        # Try LLM decision
        decision = None
        if api_key:
            decision = llm_enemy_decision(state, defender, api_key, provider)

        if decision:
            action = decision.get("action")
            target_zone = decision.get("target_zone")
            # Validate action
            if action == "fire":
                if target_zone and target_zone in valid_zones:
                    result = resolve_infantry_fire(state, defender.side, target_zone)
                    narratives.append(result)
                else:
                    if valid_zones:
                        result = resolve_infantry_fire(state, defender.side, random.choice(list(valid_zones)))
                        narratives.append(result)
                    else:
                        narratives.append(f"{defender.name} has no targets and holds position.")
            elif action == "move" and defender.status != "suppressed":
                if target_zone and target_zone in ["close","medium","long","extreme"]:
                    defender.zone = target_zone
                    narratives.append(f"{defender.name} moves to {target_zone.upper()}.")
                else:
                    # move toward objective (closer zone)
                    zones = ["close", "medium", "long", "extreme"]
                    current = zones.index(defender.zone)
                    target = zones[min(current+1, 3)] if defender.zone != state.objective_zone else zones[max(current-1,0)]
                    defender.zone = target
                    narratives.append(f"{defender.name} advances to {target.upper()}.")
            elif action == "hide":
                defender.status = "suppressed"
                narratives.append(f"{defender.name} takes cover and stops firing.")
            elif action == "retreat":
                zones = ["close", "medium", "long", "extreme"]
                idx = zones.index(defender.zone)
                if idx < 3:
                    defender.zone = zones[idx + 1]
                    narratives.append(f"{defender.name} retreats to {defender.zone}.")
                else:
                    defender.status = "routed"
                    narratives.append(f"{defender.name} abandons the fight.")
            elif action == "rpg" and "rpg" in getattr(defender, "special_equipment", []):
                if valid_zones:
                    target = random.choice(list(valid_zones))
                    result = resolve_rpg_attack(state, defender, target)
                    narratives.append(result)
                else:
                    narratives.append(f"{defender.name} has no target for RPG.")
            elif action == "sniper" and "sniper" in getattr(defender, "special_equipment", []):
                if valid_zones:
                    target = random.choice(list(valid_zones))
                    result = resolve_sniper_attack(state, defender, target)
                    narratives.append(result)
                else:
                    narratives.append(f"{defender.name} has no target for sniper.")
            else:
                # fallback
                if valid_zones:
                    result = resolve_infantry_fire(state, defender.side, random.choice(list(valid_zones)))
                    narratives.append(result)
                else:
                    narratives.append(f"{defender.name} holds position.")
        else:
            # No LLM – simple rule-based fallback
            if valid_zones:
                target_zone = random.choice(list(valid_zones))
                if "rpg" in getattr(defender, "special_equipment", []) and random.random() < 0.3:
                    result = resolve_rpg_attack(state, defender, target_zone)
                elif "sniper" in getattr(defender, "special_equipment", []) and random.random() < 0.4:
                    result = resolve_sniper_attack(state, defender, target_zone)
                else:
                    result = resolve_infantry_fire(state, defender.side, target_zone)
                narratives.append(result)
            else:
                narratives.append(f"{defender.name} has no targets and holds position.")

    if defenders and (sum(d.morale for d in defenders) / len(defenders)) < 40:
        narratives.append("Panicked shouts from enemy lines. Their morale is breaking.")

    if guerrilla_note:
        narratives.append(guerrilla_note)

    return " ".join(narratives)

# Keep strategic AI unchanged for now – we'll move to separate file
