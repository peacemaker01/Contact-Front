import re
from typing import Optional, Dict, Any
from tactical.map_engine import line_of_sight_any

class CommandParser:
    def __init__(self, llm_engine=None):
        pass

    def parse(self, player_input: str, game_state) -> Optional[Dict[str, Any]]:
        cmd = player_input.strip()
        if not cmd:
            return None

        if cmd.startswith('/'):
            return self._parse_structured_command(cmd[1:].strip(), game_state)

        return self._parse_military_command(cmd, game_state)

    # ------------------------------------------------------------------
    # MILITARY COMMAND PARSER
    # ------------------------------------------------------------------
    def _parse_military_command(self, cmd: str, game_state) -> Optional[Dict[str, Any]]:
        norm = cmd.lower().replace(',', ' ')
        stopwords = {'to', 'at', 'on', 'the', 'a', 'an', 'of', 'for', 'with', 'by', 'and', 'or', 'enemy'}
        words = norm.split()
        filtered = [w for w in words if w not in stopwords]
        if not filtered:
            return None

        cmd_map = {
            'move': 'move', 'go': 'move', 'advance': 'move',
            'fire': 'fire', 'shoot': 'fire', 'attack': 'fire', 'engage': 'fire',
            'suppress': 'suppress', 'pin': 'suppress',
            'arty': 'call_arty', 'artillery': 'call_arty', 'shell': 'call_arty',
            'firemission': 'call_arty',
            'cas': 'call_cas', 'close air support': 'call_cas',
            'recon': 'recon', 'scout': 'recon',
            'drone': 'deploy_recon_drone', 'deploy': 'deploy_recon_drone',
            'fpv': 'fpv_attack', 'kamikaze': 'fpv_attack',
            'hold': 'hold', 'stop': 'hold'
        }

        def unit_id(token: str) -> Optional[int]:
            m = re.match(r'^([a-z])(\d+)$', token)
            if m:
                return int(m.group(2))
            return None

        def coords(token: str) -> Optional[tuple]:
            if ',' in token:
                parts = token.split(',')
                if len(parts) == 2 and parts[0].isdigit() and parts[1].isdigit():
                    return (int(parts[0]), int(parts[1]))
            return None

        dir_map = {
            'north': (0,-1), 'n': (0,-1), 'south': (0,1), 's': (0,1),
            'east': (1,0), 'e': (1,0), 'west': (-1,0), 'w': (-1,0)
        }

        first = filtered[0]
        if first in cmd_map:
            action = cmd_map[first]
            if action in ['call_arty', 'recon', 'call_cas']:
                u_id = 1
                rest = filtered[1:]
            else:
                if len(filtered) < 2:
                    return None
                second = filtered[1]
                if unit_id(second) is not None and any(u.id == unit_id(second) and not u.destroyed for u in game_state.friendly_units):
                    u_id = unit_id(second)
                    rest = filtered[2:]
                else:
                    u_id = 1
                    rest = filtered[1:]
        else:
            u_id = unit_id(first)
            if u_id is None:
                return None
            if len(filtered) < 2 or filtered[1] not in cmd_map:
                return None
            action = cmd_map[filtered[1]]
            rest = filtered[2:]

        # Validate unit exists (skip for artillery/CAS)
        if action not in ['call_arty', 'call_cas'] and not any(u.id == u_id and not u.destroyed for u in game_state.friendly_units):
            return None

        # ----- MOVE -----
        if action == 'move':
            if not rest:
                return None
            # Absolute coordinates (two numbers)
            if len(rest) >= 2 and rest[0].isdigit() and rest[1].isdigit():
                x, y = int(rest[0]), int(rest[1])
                # Return action even if out of bounds – game will handle error
                return {"action_type": "move", "unit_id": u_id, "target_tile": (x, y), "target_unit_id": None, "parameters": {}, "narrative_reason": f"Move to ({x},{y})"}
            if coords(rest[0]):
                x, y = coords(rest[0])
                return {"action_type": "move", "unit_id": u_id, "target_tile": (x, y), "target_unit_id": None, "parameters": {}, "narrative_reason": f"Move to ({x},{y})"}
            # Relative movement
            if rest[0] in dir_map:
                direction = rest[0]
                distance = 1
                if len(rest) > 1 and rest[1].isdigit():
                    distance = int(rest[1])
                dx, dy = dir_map[direction]
                unit = next((u for u in game_state.friendly_units if u.id == u_id and not u.destroyed), None)
                if unit:
                    new_x = unit.x + dx * distance
                    new_y = unit.y + dy * distance
                    new_x = max(0, min(new_x, len(game_state.map_grid[0])-1))
                    new_y = max(0, min(new_y, len(game_state.map_grid)-1))
                    return {"action_type": "move", "unit_id": u_id, "target_tile": (new_x, new_y), "target_unit_id": None, "parameters": {}, "narrative_reason": f"Move {distance} {direction}"}
            if rest[0].isdigit() and len(rest) > 1 and rest[1] in dir_map:
                distance = int(rest[0])
                direction = rest[1]
                dx, dy = dir_map[direction]
                unit = next((u for u in game_state.friendly_units if u.id == u_id and not u.destroyed), None)
                if unit:
                    new_x = unit.x + dx * distance
                    new_y = unit.y + dy * distance
                    new_x = max(0, min(new_x, len(game_state.map_grid[0])-1))
                    new_y = max(0, min(new_y, len(game_state.map_grid)-1))
                    return {"action_type": "move", "unit_id": u_id, "target_tile": (new_x, new_y), "target_unit_id": None, "parameters": {}, "narrative_reason": f"Move {distance} {direction}"}
            return None

        # ----- FIRE -----
        if action == 'fire':
            if not rest:
                return None
            target_id = unit_id(rest[0])
            if target_id is None:
                return None
            return {"action_type": "fire", "unit_id": u_id, "target_tile": None, "target_unit_id": target_id, "parameters": {}, "narrative_reason": f"Fire at enemy {target_id}"}

        # ----- SUPPRESS -----
        if action == 'suppress':
            if not rest:
                return None
            target_id = unit_id(rest[0])
            if target_id is None:
                return None
            return {"action_type": "suppress", "unit_id": u_id, "target_tile": None, "target_unit_id": target_id, "parameters": {}, "narrative_reason": f"Suppress enemy {target_id}"}

        # ----- ARTILLERY / FIREMISSION -----
        if action == 'call_arty':
            if not rest:
                return None
            if len(rest) >= 2 and rest[0].isdigit() and rest[1].isdigit():
                x, y = int(rest[0]), int(rest[1])
                return {"action_type": "call_arty", "unit_id": 1, "target_tile": (x, y), "target_unit_id": None, "parameters": {"rounds": 4}, "narrative_reason": f"Artillery at ({x},{y})"}
            if coords(rest[0]):
                x, y = coords(rest[0])
                return {"action_type": "call_arty", "unit_id": 1, "target_tile": (x, y), "target_unit_id": None, "parameters": {"rounds": 4}, "narrative_reason": f"Artillery at ({x},{y})"}
            return None

        # ----- CAS -----
        if action == 'call_cas':
            if not rest:
                return None
            if len(rest) >= 2 and rest[0].isdigit() and rest[1].isdigit():
                x, y = int(rest[0]), int(rest[1])
                return {"action_type": "call_cas", "unit_id": 1, "target_tile": (x, y), "target_unit_id": None, "parameters": {}, "narrative_reason": f"CAS at ({x},{y})"}
            if coords(rest[0]):
                x, y = coords(rest[0])
                return {"action_type": "call_cas", "unit_id": 1, "target_tile": (x, y), "target_unit_id": None, "parameters": {}, "narrative_reason": f"CAS at ({x},{y})"}
            return None

        # ----- RECON -----
        if action == 'recon':
            coords_target = None
            if rest:
                if len(rest) >= 2 and rest[0].isdigit() and rest[1].isdigit():
                    coords_target = (int(rest[0]), int(rest[1]))
                elif coords(rest[0]):
                    coords_target = coords(rest[0])
            if coords_target:
                x, y = coords_target
                return {"action_type": "recon", "unit_id": u_id, "target_tile": (x, y), "target_unit_id": None, "parameters": {"radius": 3}, "narrative_reason": f"Recon at ({x},{y})"}
            else:
                return {"action_type": "recon", "unit_id": u_id, "target_tile": None, "target_unit_id": None, "parameters": {"radius": 3}, "narrative_reason": "General recon"}

        # ----- DEPLOY RECON DRONE -----
        if action == 'deploy_recon_drone':
            return {"action_type": "deploy_recon_drone", "unit_id": u_id, "target_tile": None, "target_unit_id": None, "parameters": {"radius": 5}, "narrative_reason": "Deploy recon drone"}

        # ----- FPV ATTACK -----
        if action == 'fpv_attack':
            if not rest:
                return None
            target_id = unit_id(rest[0])
            if target_id is None:
                return None
            return {"action_type": "fpv_attack", "unit_id": u_id, "target_tile": None, "target_unit_id": target_id, "parameters": {}, "narrative_reason": f"FPV attack on enemy {target_id}"}

        # ----- HOLD -----
        if action == 'hold':
            return {"action_type": "hold", "unit_id": u_id, "target_tile": None, "target_unit_id": None, "parameters": {}, "narrative_reason": "Hold position"}

        return None

    # ------------------------------------------------------------------
    # STRUCTURED /COMMAND PARSER
    # ------------------------------------------------------------------
    def _parse_structured_command(self, cmd: str, game_state) -> Optional[Dict[str, Any]]:
        parts = cmd.split()
        if not parts:
            return None
        action = parts[0].lower()

        def unit_id(s: str) -> Optional[int]:
            m = re.match(r'^([A-Za-z])(\d+)$', s)
            if m:
                return int(m.group(2))
            return None

        def coords(s: str) -> Optional[tuple]:
            if ',' in s:
                xy = s.split(',')
                if len(xy) == 2 and xy[0].isdigit() and xy[1].isdigit():
                    return (int(xy[0]), int(xy[1]))
            return None

        dir_map = {
            'north': (0,-1), 'south': (0,1), 'east': (1,0), 'west': (-1,0),
            'n': (0,-1), 's': (0,1), 'e': (1,0), 'w': (-1,0)
        }

        # MOVE
        if action in ["move", "m"] and len(parts) >= 3:
            u_id = unit_id(parts[1])
            if u_id is None:
                return None
            third = parts[2]
            if coords(third):
                x, y = coords(third)
                return {"action_type": "move", "unit_id": u_id, "target_tile": (x, y), "target_unit_id": None, "parameters": {}, "narrative_reason": f"/move to ({x},{y})"}
            if third in dir_map:
                direction = third
                distance = 1
                if len(parts) >= 4 and parts[3].isdigit():
                    distance = int(parts[3])
                dx, dy = dir_map[direction]
                unit = next((u for u in game_state.friendly_units if u.id == u_id and not u.destroyed), None)
                if unit:
                    new_x = unit.x + dx * distance
                    new_y = unit.y + dy * distance
                    new_x = max(0, min(new_x, len(game_state.map_grid[0])-1))
                    new_y = max(0, min(new_y, len(game_state.map_grid)-1))
                    return {"action_type": "move", "unit_id": u_id, "target_tile": (new_x, new_y), "target_unit_id": None, "parameters": {}, "narrative_reason": f"/move {distance} {direction}"}
            if third.isdigit() and len(parts) >= 4 and parts[3] in dir_map:
                distance = int(third)
                direction = parts[3]
                dx, dy = dir_map[direction]
                unit = next((u for u in game_state.friendly_units if u.id == u_id and not u.destroyed), None)
                if unit:
                    new_x = unit.x + dx * distance
                    new_y = unit.y + dy * distance
                    new_x = max(0, min(new_x, len(game_state.map_grid[0])-1))
                    new_y = max(0, min(new_y, len(game_state.map_grid)-1))
                    return {"action_type": "move", "unit_id": u_id, "target_tile": (new_x, new_y), "target_unit_id": None, "parameters": {}, "narrative_reason": f"/move {distance} {direction}"}
            return None

        # FIRE
        if action in ["fire", "f", "attack", "shoot"] and len(parts) >= 3:
            shooter_id = unit_id(parts[1])
            target_id = unit_id(parts[2])
            if shooter_id is None or target_id is None:
                return None
            if any(u.id == target_id and not u.destroyed for u in game_state.enemy_units) and any(u.id == shooter_id and not u.destroyed for u in game_state.friendly_units):
                return {"action_type": "fire", "unit_id": shooter_id, "target_tile": None, "target_unit_id": target_id, "parameters": {}, "narrative_reason": f"/fire at {parts[2]}"}
            return None

        # SUPPRESS
        if action in ["suppress", "sup"] and len(parts) >= 3:
            shooter_id = unit_id(parts[1])
            target_id = unit_id(parts[2])
            if shooter_id is None or target_id is None:
                return None
            if any(u.id == target_id and not u.destroyed for u in game_state.enemy_units) and any(u.id == shooter_id and not u.destroyed for u in game_state.friendly_units):
                return {"action_type": "suppress", "unit_id": shooter_id, "target_tile": None, "target_unit_id": target_id, "parameters": {}, "narrative_reason": f"/suppress {parts[2]}"}
            return None

        # ARTILLERY / FIREMISSION
        if action in ["arty", "artillery", "shell", "firemission"] and len(parts) >= 2:
            xy = coords(parts[1])
            if xy:
                x, y = xy
                return {"action_type": "call_arty", "unit_id": 1, "target_tile": (x, y), "target_unit_id": None, "parameters": {"rounds": 4}, "narrative_reason": f"/arty at ({x},{y})"}
            return None

        # CAS
        if action in ["cas", "close air support"] and len(parts) >= 2:
            xy = coords(parts[1])
            if xy:
                x, y = xy
                return {"action_type": "call_cas", "unit_id": 1, "target_tile": (x, y), "target_unit_id": None, "parameters": {}, "narrative_reason": f"/cas at ({x},{y})"}
            return None

        # RECON
        if action in ["recon", "scout"]:
            u_id = unit_id(parts[1]) if len(parts) >= 2 else 1
            if u_id is None:
                u_id = 1
            return {"action_type": "recon", "unit_id": u_id, "target_tile": None, "target_unit_id": None, "parameters": {"radius": 3}, "narrative_reason": "/recon"}

        # DEPLOY RECON DRONE
        if action in ["drone", "deploy_drone"]:
            u_id = unit_id(parts[1]) if len(parts) >= 2 else 1
            if u_id is None:
                u_id = 1
            return {"action_type": "deploy_recon_drone", "unit_id": u_id, "target_tile": None, "target_unit_id": None, "parameters": {"radius": 5}, "narrative_reason": "/drone"}

        # FPV / KAMIKAZE
        if action in ["fpv", "kamikaze"] and len(parts) >= 3:
            drone_id = unit_id(parts[1])
            target_id = unit_id(parts[2])
            if drone_id is None or target_id is None:
                return None
            if any(u.id == target_id and not u.destroyed for u in game_state.enemy_units) and any(u.id == drone_id and not u.destroyed for u in game_state.friendly_units):
                return {"action_type": "fpv_attack", "unit_id": drone_id, "target_tile": None, "target_unit_id": target_id, "parameters": {}, "narrative_reason": f"/fpv at {parts[2]}"}
            return None

        # HOLD
        if action in ["hold", "h"] and len(parts) >= 2:
            u_id = unit_id(parts[1])
            if u_id is None:
                return None
            return {"action_type": "hold", "unit_id": u_id, "target_tile": None, "target_unit_id": None, "parameters": {}, "narrative_reason": "/hold"}

        # DEBUG
        if action == "positions":
            return {"action_type": "debug_positions", "unit_id": 0, "target_tile": None, "target_unit_id": None, "parameters": {}, "narrative_reason": "show positions"}

        return None
