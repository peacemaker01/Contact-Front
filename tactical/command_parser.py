import re

class CommandParser:
    def __init__(self, llm_engine):
        self.llm = llm_engine

    def parse(self, player_input, game_state):
        cmd = player_input.lower().strip()
        if not cmd:
            return None

        # Helper: extract unit ID from any command
        def extract_unit_id(text):
            m = re.search(r'\b([a-z][0-9]+)\b', text)
            if m:
                return int(m.group(1)[1:])
            # Also try plain digit
            m = re.search(r'\b(\d+)\b', text)
            if m:
                return int(m.group(1))
            return None

        # ----- RELATIVE MOVEMENT (e.g., "move I3 two tiles east") -----
        rel_patterns = [
            r'move\s+([a-z][0-9]+)\s+(?:(\d+)\s*(?:tile|step|space)s?\s*)?(north|south|east|west|ne|nw|se|sw|n|s|e|w)',
            r'go\s+([a-z][0-9]+)\s+(?:(\d+)\s*(?:tile|step|space)s?\s*)?(north|south|east|west|ne|nw|se|sw|n|s|e|w)',
            r'advance\s+([a-z][0-9]+)\s+(?:(\d+)\s*(?:tile|step|space)s?\s*)?(north|south|east|west|ne|nw|se|sw|n|s|e|w)',
        ]
        dir_map = {
            'north': (0, -1), 'n': (0, -1), 'south': (0, 1), 's': (0, 1),
            'east': (1, 0), 'e': (1, 0), 'west': (-1, 0), 'w': (-1, 0),
            'ne': (1, -1), 'northeast': (1, -1), 'nw': (-1, -1), 'northwest': (-1, -1),
            'se': (1, 1), 'southeast': (1, 1), 'sw': (-1, 1), 'southwest': (-1, 1)
        }
        for pattern in rel_patterns:
            m = re.search(pattern, cmd)
            if m:
                unit_id = int(m.group(1)[1:])
                distance = int(m.group(2)) if m.group(2) else 1
                direction = m.group(3)
                dx, dy = dir_map.get(direction, (0, 0))
                unit = next((u for u in game_state.friendly_units if u.id == unit_id and not u.destroyed), None)
                if unit:
                    new_x = unit.x + (dx * distance)
                    new_y = unit.y + (dy * distance)
                    # Clamp to map bounds
                    new_x = max(0, min(new_x, len(game_state.map_grid[0]) - 1))
                    new_y = max(0, min(new_y, len(game_state.map_grid) - 1))
                    return {
                        "action_type": "move",
                        "unit_id": unit_id,
                        "target_tile": (new_x, new_y),
                        "target_unit_id": None,
                        "parameters": {},
                        "narrative_reason": f"Move {distance} tile(s) {direction}"
                    }

        # ----- ABSOLUTE MOVEMENT (e.g., "move R1 to 10,5" or "move 3 to 5 7") -----
        abs_patterns = [
            r'move\s+([a-z][0-9]+)\s+(?:to\s+)?(\d+)[,\s]+(\d+)',
            r'move\s+(\d+)\s+(?:to\s+)?(\d+)[,\s]+(\d+)',
            r'([a-z][0-9]+)\s+move\s+(?:to\s+)?(\d+)[,\s]+(\d+)',
            r'(\d+)\s+move\s+(?:to\s+)?(\d+)[,\s]+(\d+)',
        ]
        for pattern in abs_patterns:
            m = re.search(pattern, cmd)
            if m:
                unit_str = m.group(1)
                try:
                    unit_id = int(unit_str) if unit_str.isdigit() else int(unit_str[1:])
                except:
                    continue
                x, y = int(m.group(2)), int(m.group(3))
                if 0 <= x < len(game_state.map_grid[0]) and 0 <= y < len(game_state.map_grid):
                    return {
                        "action_type": "move",
                        "unit_id": unit_id,
                        "target_tile": (x, y),
                        "target_unit_id": None,
                        "parameters": {},
                        "narrative_reason": f"Move unit {unit_id} to ({x},{y})"
                    }

        # ----- FIRE / SUPPRESS -----
        fire_patterns = [
            r'(?:fire|shoot|engage)\s+(?:at\s+)?([a-z][0-9]+)',
            r'([a-z][0-9]+)\s+(?:fire|shoot|engage)\s+(?:at\s+)?([a-z][0-9]+)',
            r'suppress\s+([a-z][0-9]+)',
        ]
        for pattern in fire_patterns:
            m = re.search(pattern, cmd)
            if m:
                if len(m.groups()) == 2:
                    shooter_str, target_str = m.groups()
                    try:
                        shooter_id = int(shooter_str[1:]) if shooter_str[0].isalpha() else int(shooter_str)
                        target_id = int(target_str[1:]) if target_str[0].isalpha() else int(target_str)
                    except:
                        continue
                else:
                    target_str = m.group(1)
                    try:
                        target_id = int(target_str[1:]) if target_str[0].isalpha() else int(target_str)
                    except:
                        continue
                    shooter_id = extract_unit_id(cmd) or 1
                # Validate target exists
                if any(u.id == target_id for u in game_state.enemy_units if not u.destroyed):
                    action_type = "suppress" if "suppress" in cmd else "fire"
                    return {
                        "action_type": action_type,
                        "unit_id": shooter_id,
                        "target_tile": None,
                        "target_unit_id": target_id,
                        "parameters": {},
                        "narrative_reason": f"Unit {shooter_id} fires at enemy {target_id}"
                    }

        # ----- ARTILLERY -----
        arty_patterns = [
            r'(?:artillery|arty|shell)\s+(?:on|at|strike)?\s*(\d+)[,\s]+(\d+)',
            r'call\s+(?:artillery|arty)\s+(?:on|at)?\s*(\d+)[,\s]+(\d+)',
        ]
        for pattern in arty_patterns:
            m = re.search(pattern, cmd)
            if m:
                x, y = int(m.group(1)), int(m.group(2))
                if 0 <= x < len(game_state.map_grid[0]) and 0 <= y < len(game_state.map_grid):
                    return {
                        "action_type": "call_arty",
                        "unit_id": 1,
                        "target_tile": (x, y),
                        "target_unit_id": None,
                        "parameters": {"rounds": 4},
                        "narrative_reason": f"Artillery strike on ({x},{y})"
                    }

        # ----- RECON -----
        if "recon" in cmd or "scout" in cmd:
            m = re.search(r'(\d+)[,\s]+(\d+)', cmd)
            if m:
                x, y = int(m.group(1)), int(m.group(2))
                return {
                    "action_type": "recon",
                    "unit_id": 1,
                    "target_tile": (x, y),
                    "target_unit_id": None,
                    "parameters": {"radius": 3},
                    "narrative_reason": f"Recon area around ({x},{y})"
                }
            else:
                unit_id = extract_unit_id(cmd) or 1
                return {
                    "action_type": "recon",
                    "unit_id": unit_id,
                    "target_tile": None,
                    "target_unit_id": None,
                    "parameters": {"radius": 3},
                    "narrative_reason": "General recon"
                }

        # ----- FALLBACK: LLM (if available) -----
        if self.llm and self.llm.api_key:
            summary = {
                "turn": game_state.turn,
                "friendly_units": [u.to_summary() for u in game_state.friendly_units if not u.destroyed],
                "known_enemy": [u.to_summary() for u in game_state.enemy_units if not u.destroyed],
                "resources": {"artillery": game_state.artillery_fires_remaining, "cas": game_state.cas_available}
            }
            action = self.llm.parse_tactical_command(player_input, summary, game_state.player_faction)
            if action and action.get("action_type") != "invalid":
                return action

        return None
