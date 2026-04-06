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
            except Exception:
                time.sleep(1)
        return FALLBACK_RESPONSE

    def parse_tactical_command(self, player_input, game_state_summary, faction):
        system_prompt = f"""You are CONTACT FRONT military command interpreter.
Map state: {json.dumps(game_state_summary)}
Player command: "{player_input}"
Output JSON: action_type (move|fire|suppress|assault|call_arty|call_cas|fortify|recon|medic|fallback), unit_id (int), target_tile ([col,row] or null), target_unit_id (int or null), parameters (dict), narrative_reason (str). Invalid -> action_type="invalid"."""
        messages = [{"role": "system", "content": system_prompt}]
        resp = self._call(messages)
        try:
            return json.loads(resp)
        except:
            return json.loads(FALLBACK_RESPONSE)

    def generate_ai_turn(self, game_state_summary, enemy_faction, difficulty):
        system_prompt = f"""You command {enemy_faction} forces. Role: {"aggressive" if difficulty=="easy" else "defensive"}. Game state: {json.dumps(game_state_summary)}. Output JSON list of actions."""
        messages = [{"role": "system", "content": system_prompt}]
        resp = self._call(messages, temperature=0.6)
        try:
            actions = json.loads(resp)
            if isinstance(actions, dict) and "action_type" in actions:
                actions = [actions]
            return actions
        except:
            return []

    def narrate_outcome(self, action, result, faction):
        prompt = f"Narrate result: {action}. Result: {result}. Faction: {faction}. Two sentences."
        messages = [{"role": "user", "content": prompt}]
        return self._call(messages, temperature=0.7)[:200]

    def generate_scenario_briefing(self, faction, submode, scenario_id):
        prompt = f"""Generate tactical scenario for {faction} as {submode}. Output JSON: title, location_description, hq_orders, enemy_faction, enemy_strength_hint, time_limit_turns (8-15), objective_type, victory_conditions, defeat_conditions, special_conditions list."""
        messages = [{"role": "user", "content": prompt}]
        resp = self._call(messages)
        try:
            return json.loads(resp)
        except:
            return {"title": "Default Battle", "location_description": "Open field", "hq_orders": "Capture objective", "enemy_faction": "RUSSIA", "enemy_strength_hint": "Unknown", "time_limit_turns": 10, "objective_type": "capture", "victory_conditions": "Hold objective", "defeat_conditions": "Lose all units", "special_conditions": []}
