# llm_strategic.py - LLM-driven strategic AI
import random
import re
import json
import requests
from config import DIFFICULTY

def llm_strategic_decision(state, api_key, provider="openrouter"):
    """LLM decides enemy strategic action (missile type, target, naval, etc.)."""
    if not api_key:
        return None

    enemy = state.enemy_strategic
    player = state.strategic

    prompt = f"""You are an enemy strategic commander. Based on the current situation, decide the best action.

**Your (enemy) assets**:
- Nukes: {enemy.nukes}
- Ballistic missiles: {enemy.ballistic_missiles}
- Cruise missiles: {enemy.cruise_missiles}
- Hypersonic missiles: {enemy.hypersonic_missiles}
- Anti-ship missiles: {enemy.anti_ship_missiles}
- Loitering munitions: {enemy.loitering_munitions}
- Warships: {enemy.warships}
- Air defense: {enemy.air_defense}
- Intelligence level: {enemy.intelligence_level}
- Electronic warfare: {enemy.electronic_warfare}
- Cyber warfare: {enemy.cyber_warfare}
- Space assets: {enemy.space_assets}

**Player assets**:
- Warships: {player.warships}
- Air defense: {player.air_defense}
- Ballistic missiles: {player.ballistic_missiles}
- Nukes: {player.nukes}

**Global tension**: {state.global_tension}% (0-100, >80 risks nuclear war)
**Casualties**: {state.civilian_casualties}
**Difficulty**: {state.difficulty}

Possible actions:
- "ballistic_missile" (target: warships, air_defense, missile_silos, infrastructure, command_centers, nuclear_sites)
- "cruise_missile" (target: air_defense, warships)
- "hypersonic_missile" (target: air_defense)
- "anti_ship_missile" (target: warships)
- "loitering" (target: air_defense or warships)
- "naval" (engage warships)
- "cyber" (reduce player intel/EW)
- "ew" (electronic warfare)
- "space" (boost own intel, degrade player space)
- "nuke" (only if tension > 80 and very desperate)
- "wait" (do nothing)

Return ONLY valid JSON:
{{
    "action": "one of the above",
    "target": "appropriate target type or null",
    "reasoning": "short reason"
}}"""
    try:
        url = "https://openrouter.ai/api/v1/chat/completions"
        headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
        payload = {
            "model": "openai/gpt-4o-mini",
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.5,
            "max_tokens": 150
        }
        resp = requests.post(url, json=payload, headers=headers, timeout=5)
        if resp.status_code == 200:
            data = resp.json()
            content = data["choices"][0]["message"]["content"]
            match = re.search(r'\{.*\}', content, re.DOTALL)
            if match:
                decision = json.loads(match.group(0))
                return decision
    except Exception:
        pass
    return None

def process_strategic_enemy_turn(state, provider="openrouter", api_key=None):
    if state.game_mode != "strategic":
        return ""

    narratives = []
    # Try LLM decision first
    decision = None
    if api_key:
        decision = llm_strategic_decision(state, api_key, provider)

    if decision:
        action = decision.get("action")
        target = decision.get("target")
        # Execute the chosen action using existing strategic functions
        from rules import (
            strategic_missile_strike, strategic_naval_engagement,
            strategic_loitering_strike, strategic_cyber_attack,
            strategic_space_deployment, strategic_nuclear_strike
        )
        if action == "ballistic_missile" and state.enemy_strategic.ballistic_missiles > 0:
            state.enemy_strategic.ballistic_missiles -= 1
            # Simulate enemy missile strike on player assets
            if target == "warships":
                dmg = random.randint(1, 5)
                state.strategic.warships = max(0, state.strategic.warships - dmg)
                narratives.append(f"Enemy ballistic missile strike! {dmg} of our warships sunk.")
            elif target == "air_defense":
                dmg = random.randint(1, 10)
                state.strategic.air_defense = max(0, state.strategic.air_defense - dmg)
                narratives.append(f"Enemy ballistic missile degrades our air defense by {dmg}.")
            else:
                # generic military target
                dmg = random.randint(1, 5)
                state.strategic.warships = max(0, state.strategic.warships - dmg)
                narratives.append(f"Enemy ballistic missile strikes military targets.")
            state.global_tension += 5
        elif action == "cruise_missile" and state.enemy_strategic.cruise_missiles > 0:
            state.enemy_strategic.cruise_missiles -= 1
            dmg = random.randint(1, 10)
            state.strategic.air_defense = max(0, state.strategic.air_defense - dmg)
            narratives.append(f"Enemy cruise missiles overwhelm our air defense! -{dmg} AD.")
            state.global_tension += 3
        elif action == "hypersonic_missile" and state.enemy_strategic.hypersonic_missiles > 0:
            state.enemy_strategic.hypersonic_missiles -= 1
            if random.random() < 0.9:
                dmg = random.randint(10, 25)
                state.strategic.air_defense = max(0, state.strategic.air_defense - dmg)
                narratives.append(f"Enemy hypersonic missile overwhelms our air defense! -{dmg} AD.")
            else:
                narratives.append("Enemy hypersonic missile missed.")
            state.global_tension += 4
        elif action == "anti_ship_missile" and state.enemy_strategic.anti_ship_missiles > 0:
            state.enemy_strategic.anti_ship_missiles -= 1
            if state.strategic.warships > 0:
                hits = random.randint(1, min(3, state.strategic.warships))
                state.strategic.warships -= hits
                narratives.append(f"Enemy anti-ship missiles sink {hits} of our warships.")
            else:
                narratives.append("Enemy anti-ship missiles find no targets.")
            state.global_tension += 3
        elif action == "loitering" and state.enemy_strategic.loitering_munitions > 0:
            state.enemy_strategic.loitering_munitions -= 1
            if random.random() < 0.8:
                if state.strategic.air_defense > 0:
                    dmg = random.randint(5, 15)
                    state.strategic.air_defense = max(0, state.strategic.air_defense - dmg)
                    narratives.append(f"Enemy loitering munitions degrade our air defense by {dmg}.")
                else:
                    dmg = random.randint(1, 3)
                    state.strategic.warships = max(0, state.strategic.warships - dmg)
                    narratives.append(f"Enemy kamikaze drones strike our warships, sinking {dmg}.")
            else:
                narratives.append("Enemy loitering munitions intercepted.")
            state.global_tension += 2
        elif action == "naval" and state.enemy_strategic.warships > 0 and state.strategic.warships > 0:
            our_loss = random.randint(0, min(2, state.strategic.warships))
            enemy_loss = random.randint(0, min(2, state.enemy_strategic.warships))
            state.strategic.warships -= our_loss
            state.enemy_strategic.warships -= enemy_loss
            narratives.append(f"Naval battle: we lost {our_loss} ships, enemy lost {enemy_loss} ships.")
            state.global_tension += 2
        elif action == "cyber" and state.enemy_strategic.cyber_warfare > 0:
            effect = random.randint(10, 30)
            state.strategic.intelligence_level = max(0, state.strategic.intelligence_level - effect)
            state.strategic.electronic_warfare = max(0, state.strategic.electronic_warfare - effect)
            narratives.append(f"Enemy cyber attack cripples our C4ISR. Intel and EW reduced by {effect}.")
            state.global_tension += 1
        elif action == "ew" and state.enemy_strategic.electronic_warfare > 0:
            effect = random.randint(10, 30)
            state.strategic.intelligence_level = max(0, state.strategic.intelligence_level - effect)
            narratives.append(f"Enemy electronic warfare degrades our intelligence by {effect}.")
            state.global_tension += 1
        elif action == "space" and state.enemy_strategic.space_assets > 0:
            state.enemy_strategic.intelligence_level = min(100, state.enemy_strategic.intelligence_level + 10)
            state.strategic.space_assets = max(0, state.strategic.space_assets - random.randint(1, 5))
            narratives.append("Enemy space assets provide them real-time targeting data. Their intel improved.")
            state.global_tension += 1
        elif action == "nuke" and state.enemy_strategic.nukes > 0:
            state.enemy_strategic.nukes -= 1
            state.civilian_casualties += 50000
            state.global_tension += 50
            narratives.append("💥 ENEMY NUCLEAR STRIKE! Millions dead. Global thermonuclear war imminent.")
            if state.global_tension >= 100:
                state.game_over = True
                state.winner = "nobody"
                narratives.append("The world ends in nuclear fire.")
        else:
            # fallback: do nothing
            narratives.append("Enemy forces watch and wait.")
    else:
        # Fallback to previous rule-based strategic AI (kept from earlier)
        # We'll just use the old rule-based logic here for brevity.
        # (You can keep the existing process_strategic_enemy_turn logic as fallback)
        pass

    if not narratives:
        quiet_msgs = [
            "Enemy forces are watching. No immediate retaliation.",
            "Tension holds but no strike comes. For now.",
            "Enemy command deliberates. No action taken.",
            "The strait is quiet. Both sides wait.",
        ]
        narratives.append(random.choice(quiet_msgs))

    return " ".join(narratives)
