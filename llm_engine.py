import json
import time
import requests
from config import OPENROUTER_CONFIG

FALLBACK_RESPONSE = '{"action_type": "hold", "unit_id": 1, "target_tile": null, "target_unit_id": null, "parameters": {}, "narrative_reason": "Fallback due to LLM error"}'

class LLMEngine:
    def __init__(self, api_key=None, model=None):
        self.api_key = api_key or OPENROUTER_CONFIG["api_key"]
        self.model = model or OPENROUTER_CONFIG["model"]
        self.base_url = OPENROUTER_CONFIG["base_url"]

    def _call(self, messages, temperature=None):
        if not self.api_key:
            return FALLBACK_RESPONSE
        payload = {
            "model": self.model,
            "messages": messages,
            "temperature": temperature or OPENROUTER_CONFIG["temperature"],
            "max_tokens": OPENROUTER_CONFIG["max_tokens"]
        }
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
            **OPENROUTER_CONFIG["headers"]
        }
        for attempt in range(3):
            try:
                resp = requests.post(self.base_url, json=payload, headers=headers, timeout=OPENROUTER_CONFIG["timeout"])
                resp.raise_for_status()
                content = resp.json()["choices"][0]["message"]["content"]
                if content.startswith("```json"):
                    content = content[7:]
                if content.endswith("```"):
                    content = content[:-3]
                return content.strip()
            except Exception as e:
                print(f"[LLM] Attempt {attempt+1} failed: {e}")
                time.sleep(1)
        return FALLBACK_RESPONSE

    def parse_tactical_command(self, player_input, game_state_summary, faction):
        system_prompt = f"""You are a military command interpreter for CONTACT FRONT.
Convert the player's command into JSON.

Game state: {json.dumps(game_state_summary, indent=2)}

Action types: "move", "fire", "suppress", "call_arty", "recon", "deploy_recon_drone", "fpv_attack", "hold".

Rules:
- "move": requires "unit_id" (int) and "target_tile" [x,y] (ints, 0-indexed).
- "fire"/"suppress"/"fpv_attack": require "unit_id" and "target_unit_id".
- "call_arty": requires "target_tile".
- "deploy_recon_drone": requires "unit_id".
- "hold": just "unit_id".

Output ONLY valid JSON. No extra text.

Player command: "{player_input}"
"""
        messages = [{"role": "system", "content": system_prompt}]
        resp = self._call(messages, temperature=0.2)
        try:
            action = json.loads(resp)
            if action.get("action_type") not in ["move","fire","suppress","call_arty","recon","deploy_recon_drone","fpv_attack","hold"]:
                return None
            return action
        except:
            return None

    def generate_ai_turn(self, game_state_summary, enemy_faction, difficulty):
        system_prompt = f"""You are the enemy commander for {enemy_faction} (difficulty: {difficulty}).
Current state: {json.dumps(game_state_summary, indent=2)}
You may issue one action per enemy unit. Output a JSON list of actions (same schema as parse_tactical_command).
If a unit should not act, use "hold".
Think tactically. Output ONLY valid JSON. No extra text.
"""
        messages = [{"role": "system", "content": system_prompt}]
        resp = self._call(messages, temperature=0.6)
        try:
            if resp.startswith("```json"):
                resp = resp[7:]
            if resp.endswith("```"):
                resp = resp[:-3]
            actions = json.loads(resp)
            if isinstance(actions, dict):
                actions = [actions]
            return actions
        except:
            return []

    def narrate_outcome(self, action, result, faction):
        prompt = f"""Write a short (2‑3 sentences) military narrative for this outcome:
Action: {json.dumps(action)}
Result: {json.dumps(result)}
Faction: {faction}
Be concise, realistic, and engaging.
"""
        messages = [{"role": "user", "content": prompt}]
        resp = self._call(messages, temperature=0.7)
        return resp[:200] if resp else "Action completed."

    def generate_scenario_briefing(self, faction, submode, scenario_id):
        prompt = f"""Generate a unique tactical scenario for {faction} as {submode}. 
Output JSON with: title, location_description, hq_orders, enemy_faction (choose from USA, RUSSIA, CHINA, IRAN), enemy_strength_hint, time_limit_turns (8-15), objective_type, victory_conditions, defeat_conditions, special_conditions (list).
Be creative but realistic. Output ONLY valid JSON.
"""
        messages = [{"role": "user", "content": prompt}]
        resp = self._call(messages, temperature=0.7)
        try:
            if resp.startswith("```json"):
                resp = resp[7:]
            if resp.endswith("```"):
                resp = resp[:-3]
            return json.loads(resp)
        except:
            return {
                "title": f"{faction} {submode} Operation",
                "location_description": "Generic battlefield",
                "hq_orders": f"{'Capture' if submode=='attacker' else 'Defend'} the objective.",
                "enemy_faction": "RUSSIA" if faction != "RUSSIA" else "USA",
                "enemy_strength_hint": "Unknown",
                "time_limit_turns": 10,
                "objective_type": "capture",
                "victory_conditions": "Hold objective or destroy all enemies",
                "defeat_conditions": "Lose all units",
                "special_conditions": []
            }
