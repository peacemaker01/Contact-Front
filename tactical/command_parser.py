import json
from typing import Optional, Dict, Any

class CommandParser:
    def __init__(self, llm_engine):
        self.llm = llm_engine

    def parse(self, player_input: str, game_state) -> Optional[Dict[str, Any]]:
        """
        Parse natural language command using only the LLM.
        Returns None if LLM is unavailable or fails.
        """
        cmd = player_input.strip()
        if not cmd:
            return None

        # Ensure LLM is available
        if not self.llm or not self.llm.api_key:
            print("[Parser] No LLM API key available. Cannot interpret command.")
            return None

        try:
            # Build a minimal game state summary for the LLM
            summary = {
                "turn": game_state.turn,
                "friendly_units": [
                    {
                        "id": u.id,
                        "type_code": u.type_code,
                        "x": u.x,
                        "y": u.y,
                        "movement_points": u.movement_points,
                        "destroyed": u.destroyed
                    }
                    for u in game_state.friendly_units if not u.destroyed
                ],
                "known_enemy": [
                    {
                        "id": u.id,
                        "type_code": u.type_code,
                        "x": u.x,
                        "y": u.y,
                        "destroyed": u.destroyed
                    }
                    for u in game_state.enemy_units if not u.destroyed
                ],
                "map_width": len(game_state.map_grid[0]),
                "map_height": len(game_state.map_grid)
            }
            action = self.llm.parse_tactical_command(cmd, summary, game_state.player_faction)
            if action and self._validate_action(action, game_state):
                return action
            else:
                print(f"[Parser] LLM returned invalid action: {action}")
                return None
        except Exception as e:
            print(f"[Parser] LLM error: {e}")
            return None

    def _validate_action(self, action: dict, game_state) -> bool:
        """Ensure the action has required fields and references valid units/coordinates."""
        if not isinstance(action, dict):
            return False
        if "action_type" not in action or "unit_id" not in action:
            return False
        unit_id = action["unit_id"]
        # Unit must exist and be friendly
        if not any(u.id == unit_id and not u.destroyed for u in game_state.friendly_units):
            return False
        act = action["action_type"]
        if act == "move":
            if "target_tile" not in action:
                return False
            tx, ty = action["target_tile"]
            if not (0 <= tx < len(game_state.map_grid[0]) and 0 <= ty < len(game_state.map_grid)):
                return False
        elif act in ["fire", "suppress", "fpv_attack"]:
            if "target_unit_id" not in action:
                return False
        elif act == "call_arty":
            if "target_tile" not in action:
                return False
        # Other actions (recon, deploy_recon_drone, hold) don't need extra validation
        return True
