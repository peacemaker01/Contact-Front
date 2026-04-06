import random

class ScenarioGenerator:
    def __init__(self, llm_engine):
        self.llm = llm_engine

    def get_scenario(self, faction, submode, use_llm=True):
        if use_llm and self.llm.api_key:
            return self.llm.generate_scenario_briefing(faction, submode, "random")
        else:
            return self._fallback_scenario(faction, submode)

    def _fallback_scenario(self, faction, submode):
        return {
            "title": f"{faction} {submode.title()} Operation",
            "location_description": "Generic battlefield with scattered buildings and a central road.",
            "hq_orders": f"{'Capture' if submode=='attacker' else 'Defend'} the objective.",
            "enemy_faction": "RUSSIA" if faction != "RUSSIA" else "USA",
            "enemy_strength_hint": "Estimated 2-3 squads with possible armor support.",
            "time_limit_turns": 10,
            "objective_type": "capture",
            "victory_conditions": "Hold objective at turn limit or destroy all enemies.",
            "defeat_conditions": "Lose all friendly units.",
            "special_conditions": []
        }
