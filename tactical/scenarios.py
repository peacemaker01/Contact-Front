from factions import FACTIONS

class ScenarioGenerator:
    def __init__(self, llm_engine):
        self.llm = llm_engine

    def get_scenario(self, faction, submode):
        if self.llm and self.llm.api_key:
            briefing = self.llm.generate_scenario_briefing(faction, submode, "random")
            enemy = briefing.get("enemy_faction", "RUSSIA")
            if enemy not in FACTIONS:
                enemy = "RUSSIA" if faction != "RUSSIA" else "USA"
                briefing["enemy_faction"] = enemy
            return briefing
        else:
            # Fallback scenario – but still no regex; just a hardcoded default
            return {
                "title": f"{faction} {submode} Operation",
                "location_description": "Generic battlefield.",
                "hq_orders": f"{'Capture' if submode=='attacker' else 'Defend'} the objective.",
                "enemy_faction": "RUSSIA" if faction != "RUSSIA" else "USA",
                "enemy_strength_hint": "Unknown",
                "time_limit_turns": 10,
                "objective_type": "capture",
                "victory_conditions": "Hold objective or destroy all enemies",
                "defeat_conditions": "Lose all units",
                "special_conditions": []
            }
