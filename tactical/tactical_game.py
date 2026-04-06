from factions import FACTIONS
from tactical.map_engine import generate_tactical_map, render_ascii_map
from tactical.unit_manager import create_units_for_faction, resolve_fire, resolve_artillery
from tactical.command_parser import CommandParser
from tactical.objective import check_objectives
from tactical.scenarios import ScenarioGenerator
from llm_engine import LLMEngine
from hud import render_full_tactical_hud, clear_screen, ANSI
from game_state import TacticalGameState
import random

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
            action_log=[], narrative_log=[], friendly_kia=0, friendly_wia=0, vehicles_lost=0, enemy_kia=0
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
                self.state.narrative_log.append(f"Invalid command: '{cmd}'")
                self.state.turn += 1
                continue
            self._resolve_action(action)
            if not self.state.game_over:
                self._ai_turn()
                for u in self.state.friendly_units:
                    if not u.destroyed:
                        u.movement_points = u.movement
            check_objectives(self.state)
        self._end_game()

    def _resolve_action(self, action):
        unit = None
        for u in self.state.friendly_units:
            if u.id == action["unit_id"] and not u.destroyed:
                unit = u
                break
        if not unit:
            self.state.narrative_log.append(f"Unit {action['unit_id']} not found.")
            self.state.turn += 1
            return

        act = action["action_type"]
        if act == "move" and action.get("target_tile"):
            tx, ty = action["target_tile"]
            old_x, old_y = unit.x, unit.y
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
                unit.x, unit.y = tx, ty
                unit.movement_points -= total_cost
                self.state.narrative_log.append(f"{unit.name} moves to ({tx},{ty}). {unit.movement_points} MP left.")
            else:
                self.state.narrative_log.append(f"Not enough MP. Need {total_cost}, have {unit.movement_points}.")
        elif act in ["fire", "suppress"]:
            target_id = action.get("target_unit_id")
            target = None
            for u in self.state.enemy_units:
                if u.id == target_id and not u.destroyed:
                    target = u
                    break
            if not target:
                self.state.narrative_log.append(f"Enemy {target_id} not found.")
                return
            tile = self.state.map_grid[target.y][target.x]
            result = resolve_fire(unit, target, tile.cover_bonus, act, self.state)
            narr = self.llm.narrate_outcome(action, result, self.player_faction)
            self.state.narrative_log.append(narr)
        elif act == "call_arty" and self.state.artillery_fires_remaining > 0:
            target_tile = action.get("target_tile")
            if target_tile:
                result = resolve_artillery(unit, target_tile, 4, self.state)
                self.state.artillery_fires_remaining -= 1
                self.state.narrative_log.append(f"Artillery strike at {target_tile} – {result['damage_pct']}% damage.")
        elif act == "deploy_recon_drone":
            self._deploy_recon_drone(unit, action["parameters"].get("radius", 5))
        elif act == "fpv_attack":
            target_id = action.get("target_unit_id")
            target = None
            for u in self.state.enemy_units:
                if u.id == target_id and not u.destroyed:
                    target = u
                    break
            if target:
                self._fpv_attack(unit, target)
            else:
                self.state.narrative_log.append("Target not found.")
        elif act == "recon":
            radius = action["parameters"].get("radius", 3)
            self.state.narrative_log.append(f"Recon reveals enemies within {radius} tiles.")
        else:
            self.state.narrative_log.append(f"Unknown action: {act}")
        self.state.turn += 1

    def _ai_turn(self):
        if self.llm.api_key:
            summary = {
                "turn": self.state.turn,
                "enemy_units": [u.to_summary() for u in self.state.enemy_units if not u.destroyed],
                "friendly_units": [u.to_summary() for u in self.state.friendly_units if not u.destroyed],
                "resources": {"artillery": self.state.artillery_fires_remaining}
            }
            actions = self.llm.generate_ai_turn(summary, self.state.enemy_faction, self.difficulty)
            for act in actions:
                if act.get("action_type") == "fire":
                    unit_id = act.get("unit_id")
                    target_id = act.get("target_unit_id")
                    enemy = None
                    for u in self.state.enemy_units:
                        if u.id == unit_id and not u.destroyed:
                            enemy = u
                            break
                    if not enemy:
                        continue
                    target = None
                    for u in self.state.friendly_units:
                        if u.id == target_id and not u.destroyed:
                            target = u
                            break
                    if target:
                        tile = self.state.map_grid[target.y][target.x]
                        resolve_fire(enemy, target, tile.cover_bonus, "fire", self.state)
                        self.state.narrative_log.append(f"Enemy {enemy.name} fires at {target.name}.")
        else:
            # Fallback: simple AI (fire at closest)
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
                    resolve_fire(enemy, closest, tile.cover_bonus, "fire", self.state)
                    self.state.narrative_log.append(f"Enemy {enemy.name} fires at {closest.name}.")

    def _deploy_recon_drone(self, unit, radius):
        self.state.narrative_log.append(f"Recon drone deployed. Enemy positions revealed within {radius} tiles.")

    def _fpv_attack(self, attacker, target):
        if attacker.ammo <= 0:
            self.state.narrative_log.append("No FPV drones available.")
            return
        hit_chance = attacker.accuracy_base
        if random.randint(1,100) <= hit_chance:
            damage = random.uniform(30, 60)
            target.strength -= damage
            self.state.narrative_log.append(f"FPV drone hits {target.name} for {damage:.0f} damage!")
            if target.strength <= 0 and not target.destroyed:
                target.destroyed = True
                self.state.enemy_kia += 1
                self.state.narrative_log.append(f"{target.name} destroyed.")
        else:
            self.state.narrative_log.append(f"FPV drone misses {target.name}.")
        attacker.ammo -= 1
        attacker.destroyed = True

    def _end_game(self):
        clear_screen()
        if self.state.victory:
            print(f"{FACTIONS[self.player_faction]['ansi_color']}VICTORY!{ANSI['RESET']}")
        else:
            print(f"{ANSI['WARNING']}DEFEAT.{ANSI['RESET']}")
        input("Press Enter.")
