from factions import FACTIONS
from tactical.map_engine import generate_tactical_map, render_ascii_map
from tactical.unit_manager import create_units_for_faction, resolve_fire, resolve_artillery
from tactical.command_parser import CommandParser
from tactical.objective import check_objectives
from tactical.scenarios import ScenarioGenerator
from llm_engine import LLMEngine
from hud import render_full_tactical_hud, clear_screen, ANSI
from game_state import TacticalGameState

class TacticalGame:
    def __init__(self, player_faction, submode, difficulty):
        self.player_faction = player_faction
        self.submode = submode
        self.difficulty = difficulty
        self.llm = LLMEngine()
        self.parser = CommandParser(self.llm)
        self.scenario_gen = ScenarioGenerator(self.llm)
        self.state = None
        self._init_game()

    def _init_game(self):
        briefing = self.scenario_gen.get_scenario(self.player_faction, self.submode, use_llm=bool(self.llm.api_key))
        enemy_faction = briefing.get("enemy_faction", "RUSSIA")
        map_grid, obj_coords = generate_tactical_map(width=50, height=14)
        if self.submode == "attacker":
            start_pos = [(3, y) for y in range(3, 12, 3)]
            enemy_pos = [(47, y) for y in range(3, 12, 3)]
        else:
            start_pos = [(47, y) for y in range(3, 12, 3)]
            enemy_pos = [(3, y) for y in range(3, 12, 3)]
        friendly = create_units_for_faction(self.player_faction, "ALPHA", start_pos)
        enemy = create_units_for_faction(enemy_faction, "ENEMY", enemy_pos)
        self.state = TacticalGameState(
            turn=1, max_turns=briefing.get("time_limit_turns", 10), mode=self.submode,
            player_faction=self.player_faction, enemy_faction=enemy_faction,
            scenario_id=briefing.get("title", "default"), map_grid=map_grid,
            friendly_units=friendly, enemy_units=enemy,
            artillery_fires_remaining=3, cas_available=True, smoke_grenades=2,
            objectives=[{"desc": briefing.get("victory_conditions", "Secure objective"), "coords": obj_coords}],
            action_log=[], narrative_log=[]
        )

    def run(self):
        while not self.state.game_over:
            render_full_tactical_hud(self.state, render_ascii_map(self.state))
            print(f"{ANSI['TITLE']}> {ANSI['RESET']}", end="", flush=True)
            cmd = input().strip()
            if cmd.lower() in ["quit", "exit"]:
                break
            if not cmd:
                self.state.narrative_log.append("No command entered.")
                continue

            action = self.parser.parse(cmd, self.state)
            if not action:
                self.state.narrative_log.append(f"Invalid command: '{cmd}'. Use 'move R1 to 10,5', 'fire at R3', 'arty at 15,8'.")
                self.state.turn += 1
                continue

            self._resolve_action(action)
            if not self.state.game_over:
                self._ai_turn()
                # Reset movement points for friendly units at start of next player turn
                for u in self.state.friendly_units:
                    if not u.destroyed:
                        u.movement_points = u.movement
            check_objectives(self.state)
        self._end_game()

    def _resolve_action(self, action):
        # Find the unit
        unit = None
        for u in self.state.friendly_units:
            if u.id == action["unit_id"] and not u.destroyed:
                unit = u
                break
        if not unit:
            self.state.narrative_log.append(f"Unit {action['unit_id']} not found or destroyed.")
            self.state.turn += 1
            return

        action_type = action.get("action_type")
        if action_type == "move" and action.get("target_tile"):
            tx, ty = action["target_tile"]
            # Calculate path cost using Manhattan distance with terrain costs
            steps = []
            cx, cy = unit.x, unit.y
            while cx != tx or cy != ty:
                if cx < tx: cx += 1
                elif cx > tx: cx -= 1
                elif cy < ty: cy += 1
                elif cy > ty: cy -= 1
                steps.append((cx, cy))
            total_cost = sum(self.state.map_grid[cy][cx].movement_cost for (cx, cy) in steps)
            if total_cost <= unit.movement_points:
                # Update unit position
                unit.x, unit.y = tx, ty
                unit.movement_points -= total_cost
                self.state.narrative_log.append(f"{unit.name} moves to ({tx},{ty}). {unit.movement_points} MP left.")
            else:
                self.state.narrative_log.append(f"Not enough MP. Need {total_cost}, have {unit.movement_points}.")
        elif action_type in ["fire", "suppress"]:
            target_id = action.get("target_unit_id")
            if target_id is None:
                self.state.narrative_log.append("No target specified.")
                return
            target = None
            for u in self.state.enemy_units:
                if u.id == target_id and not u.destroyed:
                    target = u
                    break
            if not target:
                self.state.narrative_log.append(f"Enemy unit {target_id} not found.")
                return
            tile = self.state.map_grid[target.y][target.x]
            result = resolve_fire(unit, target, tile.cover_bonus, action_type)
            narr = self.llm.narrate_outcome(action, result, self.player_faction)
            self.state.narrative_log.append(narr)
        elif action_type == "call_arty" and self.state.artillery_fires_remaining > 0:
            target_tile = action.get("target_tile")
            if not target_tile:
                self.state.narrative_log.append("No target tile for artillery.")
                return
            result = resolve_artillery(unit, target_tile, action.get("parameters", {}).get("rounds", 4), self.state)
            self.state.artillery_fires_remaining -= 1
            self.state.narrative_log.append(f"Artillery strike on ({target_tile[0]},{target_tile[1]}) - {result['damage_pct']}% damage, impact at {result['impact_tile']}.")
        else:
            self.state.narrative_log.append(f"Action '{action_type}' not implemented.")
        # Increment turn after player action
        self.state.turn += 1

    def _ai_turn(self):
        # Simple AI: each enemy unit fires at the closest friendly unit
        for enemy in self.state.enemy_units:
            if enemy.destroyed:
                continue
            closest = None
            min_dist = float('inf')
            for friend in self.state.friendly_units:
                if friend.destroyed:
                    continue
                dist = abs(enemy.x - friend.x) + abs(enemy.y - friend.y)
                if dist < min_dist:
                    min_dist = dist
                    closest = friend
            if closest and min_dist <= 10:
                tile = self.state.map_grid[closest.y][closest.x]
                result = resolve_fire(enemy, closest, tile.cover_bonus, "fire")
                self.state.narrative_log.append(f"Enemy {enemy.name} fires at {closest.name}.")
        # No extra turn increment here – the player's turn increment already happened

    def _end_game(self):
        clear_screen()
        if self.state.victory:
            print(f"{FACTIONS[self.player_faction]['ansi_color']}VICTORY! {self.player_faction} achieved objectives.{ANSI['RESET']}")
        else:
            print(f"{ANSI['WARNING']}DEFEAT. Mission failed.{ANSI['RESET']}")
        input("Press Enter to exit.")
