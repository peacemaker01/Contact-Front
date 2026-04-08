from factions import FACTIONS
from tactical.map_engine import generate_tactical_map, render_ascii_map, line_of_sight_any
from tactical.unit_manager import create_units_for_faction, resolve_fire, resolve_artillery, check_rout, apply_ew_effects
from tactical.command_parser import CommandParser
from tactical.objective import check_objectives
from tactical.scenarios import ScenarioGenerator
from llm_engine import LLMEngine
from hud import render_full_tactical_hud, clear_screen, ANSI
from game_state import TacticalGameState
from terminal_utils import TerminalInfo
import random
import sys
import math

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
        briefing = self.scenario_gen.get_scenario(self.player_faction, self.submode)
        enemy_faction = briefing.get("enemy_faction", "RUSSIA")
        if enemy_faction not in FACTIONS:
            enemy_faction = "RUSSIA" if self.player_faction != "RUSSIA" else "USA"
        term = TerminalInfo()
        map_w, map_h = term.get_map_size()
        map_grid, obj_coords = generate_tactical_map(width=map_w, height=map_h)
        if self.submode == "attacker":
            start_pos = [(3, y) for y in range(3, map_h - 2, 3)]
            enemy_pos = [(map_w - 3, y) for y in range(3, map_h - 2, 3)]
        else:
            start_pos = [(map_w - 3, y) for y in range(3, map_h - 2, 3)]
            enemy_pos = [(3, y) for y in range(3, map_h - 2, 3)]
        friendly = create_units_for_faction(self.player_faction, "ALPHA", start_pos)
        enemy = create_units_for_faction(enemy_faction, "ENEMY", enemy_pos)
        is_night = random.random() < 0.2
        is_raining = random.random() < 0.15
        # Place supply depot near player start
        supply_depots = [(start_pos[0][0] + 2, start_pos[0][1])]
        self.state = TacticalGameState(
            turn=1, max_turns=briefing.get("time_limit_turns", 10), mode=self.submode,
            player_faction=self.player_faction, enemy_faction=enemy_faction,
            scenario_id=briefing.get("title", "default"), map_grid=map_grid,
            friendly_units=friendly, enemy_units=enemy,
            artillery_fires_remaining=3, cas_available=2, smoke_grenades=2,
            objectives=[{"desc": briefing.get("victory_conditions", "Secure objective"), "coords": obj_coords}],
            action_log=[], narrative_log=[], friendly_kia=0, friendly_wia=0, vehicles_lost=0, enemy_kia=0,
            is_night=is_night, is_raining=is_raining,
            enemy_artillery_fires_remaining=3, enemy_cas_available=2,
            supply_depots=supply_depots
        )

    def run(self):
        try:
            while not self.state.game_over:
                # Process delayed orders (C2)
                self._process_delayed_orders()
                render_full_tactical_hud(self.state, render_ascii_map(self.state))
                print(f"{ANSI['TITLE']}> {ANSI['RESET']}", end="", flush=True)
                cmd = input().strip()
                if cmd.lower() in ["quit", "exit"]:
                    break
                if not cmd:
                    continue
                action = self.parser.parse(cmd, self.state)
                if not action:
                    self._log(f"Command not understood: '{cmd}'")
                    continue
                # Check radio range for order delay
                unit_id = action.get("unit_id", 0)
                if unit_id != 0:
                    unit = next((u for u in self.state.friendly_units if u.id == unit_id and not u.destroyed), None)
                    if unit:
                        # Find commander (unit ID 1) as HQ
                        commander = next((u for u in self.state.friendly_units if u.id == 1 and not u.destroyed), None)
                        if commander:
                            dist = abs(unit.x - commander.x) + abs(unit.y - commander.y)
                            if dist > unit.radio_range and unit.order_delay_turns == 0:
                                unit.order_delay_turns = 1
                                self._log(f"Order to {unit.name} delayed due to radio range ({dist} > {unit.radio_range}).")
                                self.state.delayed_orders.append({
                                    "action": action,
                                    "execution_turn": self.state.turn + unit.order_delay_turns,
                                    "unit_id": unit_id
                                })
                                continue  # do not execute now
                # If no delay, execute immediately
                turn_consumed = self._resolve_action(action, immediate=True)
                if turn_consumed and not self.state.game_over:
                    self._ai_turn()
                    if self.state.turn % 5 == 0:
                        self._resupply()
                    for u in self.state.friendly_units:
                        if not u.destroyed:
                            u.movement_points = u.movement
                    check_objectives(self.state)
                    self.state.turn += 1
        except KeyboardInterrupt:
            print(f"\n{ANSI['WARNING']}Game interrupted.{ANSI['RESET']}")
        self._end_game()

    def _process_delayed_orders(self):
        """Execute orders whose execution turn has arrived."""
        remaining = []
        for order in self.state.delayed_orders:
            if order["execution_turn"] <= self.state.turn:
                unit = next((u for u in self.state.friendly_units if u.id == order["unit_id"] and not u.destroyed), None)
                if unit:
                    self._log(f"Executing delayed order for {unit.name}.")
                    self._resolve_action(order["action"], immediate=True)
                # else unit dead, order lost
            else:
                remaining.append(order)
        self.state.delayed_orders = remaining

    def _resolve_action(self, action, immediate=False) -> bool:
        unit = None
        if action["unit_id"] != 0:
            unit = next((u for u in self.state.friendly_units if u.id == action["unit_id"] and not u.destroyed), None)
            if not unit:
                self._log(f"Unit {action['unit_id']} not found.")
                return False

        act = action["action_type"]

        # ----- MOVE -----
        if act == "move" and action.get("target_tile"):
            tx, ty = action["target_tile"]
            if not (0 <= tx < len(self.state.map_grid[0]) and 0 <= ty < len(self.state.map_grid)):
                self._log(f"Target ({tx},{ty}) is outside map bounds.")
                return False
            old_x, old_y = unit.x, unit.y
            if (tx, ty) == (old_x, old_y):
                self._log(f"{unit.name} is already at ({tx},{ty}).")
                return False
            path_cost = self._movement_cost(old_x, old_y, tx, ty)
            if path_cost == float('inf'):
                self._log("Impassable terrain in path.")
                return False
            if path_cost > unit.movement_points:
                self._log(f"Not enough MP. Need {path_cost:.1f}, have {unit.movement_points:.0f}.")
                return False
            unit.x, unit.y = tx, ty
            unit.movement_points -= path_cost
            self._log(f"{unit.name} moves to ({tx},{ty}). {unit.movement_points:.0f} MP left.")
            return True

        # ----- FIRE / SUPPRESS with ammo types -----
        if act in ["fire", "suppress"]:
            target_id = action.get("target_unit_id")
            target = next((u for u in self.state.enemy_units if u.id == target_id and not u.destroyed), None)
            if not target:
                self._log(f"Enemy {target_id} not found.")
                return False
            dist = abs(unit.x - target.x) + abs(unit.y - target.y)
            max_range = getattr(unit, 'range_tiles', 15)
            if dist > max_range:
                self._log(f"Target out of range ({dist} tiles, max {max_range}).")
                return False
            # Choose ammo type based on target (AP for armored, HE for infantry)
            is_armored = target.armor > 50
            if is_armored:
                if unit.ammo_ap <= 0:
                    self._log(f"No AP ammo for {unit.name}.")
                    return False
                ammo_type = "AP"
                unit.ammo_ap -= 1
                damage_mult = 1.2
            else:
                if unit.ammo_he <= 0:
                    self._log(f"No HE ammo for {unit.name}.")
                    return False
                ammo_type = "HE"
                unit.ammo_he -= 1
                damage_mult = 0.8
            penalty = 0
            if self.state.is_night:
                penalty += 20
            if self.state.is_raining:
                penalty += 10
            if self.state.enemy_faction == "RUSSIA" and random.random() < 0.3:
                penalty += 15
            tile = self.state.map_grid[target.y][target.x]
            hit_chance = unit.accuracy_base - tile.cover_bonus - penalty + random.randint(-10,10)
            hit = random.randint(1,100) <= hit_chance
            if hit:
                base_damage = random.uniform(15, 35) * damage_mult
                damage = base_damage * (1 - target.armor/100)
                target.strength -= damage
                target.morale -= damage * 0.5
                # Vehicle damage states
                if target.armor > 0:
                    if not target.mobility_kill and random.random() < 0.3:
                        target.mobility_kill = True
                        self._log(f"{target.name} mobility killed! (movement halved)")
                        target.movement = max(1, target.movement // 2)
                    if not target.firepower_kill and random.random() < 0.2:
                        target.firepower_kill = True
                        self._log(f"{target.name} firepower killed! (accuracy -50)")
                        target.accuracy_base = max(0, target.accuracy_base - 50)
                if target.strength > 0 and target.faction == self.state.player_faction:
                    self.state.friendly_wia += 1
            else:
                target.morale -= 5
            if target.morale <= unit.suppress_threshold:
                target.suppressed = True
            if target.strength <= 0 and not target.destroyed:
                target.destroyed = True
                if target.faction == self.state.player_faction:
                    self.state.friendly_kia += 1
                    if target.type in ["m1a2_tank", "t90m_tank", "bradley_ifv", "bmp3_ifv", "type99a_tank", "t72s_tank"]:
                        self.state.vehicles_lost += 1
                else:
                    self.state.enemy_kia += 1
            self._log(f"{unit.name} fires {ammo_type} at {target.name} – {'hit' if hit else 'miss'}.")
            if check_rout(target, self.state):
                self._log(f"{target.name} panics and routs!")
            return True

        # ----- ARTILLERY (with delay and friendly fire) -----
        if act == "call_arty" and self.state.artillery_fires_remaining > 0:
            if not immediate:
                self._log("Artillery strike scheduled for next turn.")
                self.state.delayed_orders.append({
                    "action": action,
                    "execution_turn": self.state.turn + 1,
                    "unit_id": 0
                })
                self.state.artillery_fires_remaining -= 1
                return True
            target_tile = action.get("target_tile")
            if not target_tile:
                self._log("No target tile.")
                return False
            cep = 50
            if self.state.ew_gps_jammed:
                cep = 100
            result = resolve_artillery(None, target_tile, 4, self.state, cep)
            # Friendly fire: damage any friendly units in blast radius
            for friend in self.state.friendly_units:
                if friend.destroyed:
                    continue
                if abs(friend.x - result["impact_tile"][0]) <= 2 and abs(friend.y - result["impact_tile"][1]) <= 2:
                    dmg = random.uniform(10, 30)
                    friend.strength -= dmg
                    self._log(f"FRIENDLY FIRE: Artillery hits {friend.name} for {dmg:.0f} damage!")
                    if friend.strength <= 0:
                        friend.destroyed = True
                        self.state.friendly_kia += 1
            self._log(f"Artillery strike at {target_tile} – scatter to {result['impact_tile']}.")
            return True

        # ----- CAS (with delay and friendly fire) -----
        if act == "call_cas" and self.state.cas_available > 0:
            if not immediate:
                self._log("CAS strike scheduled for next turn.")
                self.state.delayed_orders.append({
                    "action": action,
                    "execution_turn": self.state.turn + 1,
                    "unit_id": 0
                })
                self.state.cas_available -= 1
                return True
            target_tile = action.get("target_tile")
            if not target_tile:
                self._log("No target tile for CAS.")
                return False
            # Friendly fire
            for friend in self.state.friendly_units:
                if friend.destroyed:
                    continue
                if abs(friend.x - target_tile[0]) <= 3 and abs(friend.y - target_tile[1]) <= 3:
                    dmg = random.uniform(30, 60)
                    friend.strength -= dmg
                    self._log(f"FRIENDLY FIRE: CAS hits {friend.name} for {dmg:.0f} damage!")
                    if friend.strength <= 0:
                        friend.destroyed = True
                        self.state.friendly_kia += 1
            self._call_cas(target_tile)
            return True

        # ----- RECON -----
        if act == "deploy_recon_drone":
            self._apply_recon_reveal(unit, action["parameters"].get("radius", 5))
            return True

        if act == "recon":
            self._apply_recon_reveal(unit, action["parameters"].get("radius", 3))
            return True

        # ----- FPV ATTACK -----
        if act == "fpv_attack":
            target_id = action.get("target_unit_id")
            target = next((u for u in self.state.enemy_units if u.id == target_id and not u.destroyed), None)
            if not target:
                self._log("FPV target not found.")
                return False
            self._fpv_attack(unit, target)
            return True

        # ----- HOLD -----
        if act == "hold":
            self._log(f"{unit.name} holds position.")
            return True

        # ----- DEBUG -----
        if act == "debug_positions":
            msg = "Positions: " + ", ".join([f"{u.name} ({u.type_code}{u.id}) at ({u.x},{u.y})" for u in self.state.friendly_units if not u.destroyed])
            self._log(msg)
            return True

        self._log(f"Unknown action: {act}")
        return False

    def _call_cas(self, target_tile):
        x, y = target_tile
        for enemy in self.state.enemy_units:
            if enemy.destroyed:
                continue
            if abs(enemy.x - x) <= 3 and abs(enemy.y - y) <= 3:
                damage = random.uniform(40, 80)
                enemy.strength -= damage
                self._log(f"CAS hits {enemy.name} for {damage:.0f} damage!")
                if enemy.strength <= 0:
                    enemy.destroyed = True
                    self.state.enemy_kia += 1
                    self._log(f"{enemy.name} destroyed.")

    def _apply_recon_reveal(self, unit, radius):
        revealed = []
        for enemy in self.state.enemy_units:
            if enemy.destroyed:
                continue
            dist = abs(unit.x - enemy.x) + abs(unit.y - enemy.y)
            if dist <= radius:
                enemy.last_seen_by_player = self.state.turn
                revealed.append(f"{enemy.type_code}{enemy.id} at ({enemy.x},{enemy.y})")
        if revealed:
            self._log(f"Recon reveals: {', '.join(revealed)}")
        else:
            self._log("Recon finds nothing.")

    def _resupply(self):
        # Logistics: resupply from depots within 5 tiles
        for depot in self.state.supply_depots:
            for unit in self.state.friendly_units:
                if unit.destroyed:
                    continue
                dist = abs(unit.x - depot[0]) + abs(unit.y - depot[1])
                if dist <= 5:
                    unit.ammo_he = min(unit.max_ammo_he, unit.ammo_he + unit.max_ammo_he // 2)
                    unit.ammo_ap = min(unit.max_ammo_ap, unit.ammo_ap + unit.max_ammo_ap // 2)
                    self._log(f"{unit.name} resupplied from depot at {depot}.")
        self._log("Resupply phase complete.")

    def _movement_cost(self, sx, sy, tx, ty):
        steps = max(abs(tx - sx), abs(ty - sy))
        if steps == 0:
            return 0.0
        cost = 0.0
        for i in range(1, steps + 1):
            ix = round(sx + (tx - sx) * i / steps)
            iy = round(sy + (ty - sy) * i / steps)
            tile = self.state.map_grid[iy][ix]
            if tile.movement_cost >= 999:
                return float('inf')
            cost += tile.movement_cost
        return cost

    def _log(self, message: str):
        self.state.narrative_log.append(message)
        if len(self.state.narrative_log) > 50:
            self.state.narrative_log = self.state.narrative_log[-50:]

    def _ai_turn(self):
        # Determine AI goal
        objective_tile = self.state.objectives[0]["coords"] if self.state.objectives else None
        player_is_attacker = (self.state.mode == "attacker")

        # --- Enemy artillery and CAS usage (with delay) ---
        player_positions = [(u.x, u.y) for u in self.state.friendly_units if not u.destroyed]
        if player_positions:
            # Find cluster center
            cluster_center = None
            best_count = 0
            for (x, y) in player_positions:
                count = sum(1 for (px, py) in player_positions if abs(px-x) <= 2 and abs(py-y) <= 2)
                if count > best_count:
                    best_count = count
                    cluster_center = (x, y)
            # Artillery on clusters (with delay)
            if self.state.enemy_artillery_fires_remaining > 0 and best_count >= 2:
                self.state.enemy_artillery_fires_remaining -= 1
                # Schedule for next turn
                self.state.delayed_orders.append({
                    "action": {"action_type": "call_arty", "target_tile": cluster_center},
                    "execution_turn": self.state.turn + 1,
                    "unit_id": 0
                })
                self._log("Enemy artillery scheduled for next turn.")
            # CAS
            if self.state.enemy_cas_available > 0 and best_count >= 1:
                self.state.enemy_cas_available -= 1
                self.state.delayed_orders.append({
                    "action": {"action_type": "call_cas", "target_tile": cluster_center},
                    "execution_turn": self.state.turn + 1,
                    "unit_id": 0
                })
                self._log("Enemy CAS scheduled for next turn.")

        # --- Move and fire for each enemy unit ---
        for enemy in self.state.enemy_units:
            if enemy.destroyed or enemy.is_routed:
                continue

            # Kamikaze drones
            if enemy.type_code == 'K':
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
                    self._fpv_attack(enemy, closest)
                    continue

            # Decide movement target
            if player_is_attacker and objective_tile:
                target_x, target_y = objective_tile
            else:
                closest = None
                min_dist = float('inf')
                for friend in self.state.friendly_units:
                    if friend.destroyed:
                        continue
                    dist = abs(enemy.x - friend.x) + abs(enemy.y - friend.y)
                    if dist < min_dist:
                        min_dist = dist
                        closest = friend
                if closest:
                    target_x, target_y = closest.x, closest.y
                else:
                    target_x, target_y = enemy.x, enemy.y

            # Enemy fallback logic: if outnumbered 3:1, retreat to secondary defensive line
            friendly_count = sum(1 for u in self.state.friendly_units if not u.destroyed)
            enemy_count = sum(1 for u in self.state.enemy_units if not u.destroyed)
            if enemy_count > 0 and friendly_count / enemy_count >= 3 and objective_tile:
                # Retreat away from objective
                retreat_x = target_x - 5 if target_x > enemy.x else target_x + 5
                retreat_y = target_y - 5 if target_y > enemy.y else target_y + 5
                target_x = max(0, min(retreat_x, len(self.state.map_grid[0])-1))
                target_y = max(0, min(retreat_y, len(self.state.map_grid)-1))
                self._log(f"Enemy {enemy.name} falls back due to heavy losses.")

            # Movement step
            if (enemy.x, enemy.y) != (target_x, target_y):
                dx = 0
                dy = 0
                if target_x > enemy.x:
                    dx = 1
                elif target_x < enemy.x:
                    dx = -1
                if target_y > enemy.y:
                    dy = 1
                elif target_y < enemy.y:
                    dy = -1
                if abs(target_x - enemy.x) > abs(target_y - enemy.y):
                    dy = 0
                else:
                    dx = 0
                new_x = enemy.x + dx
                new_y = enemy.y + dy
                if 0 <= new_x < len(self.state.map_grid[0]) and 0 <= new_y < len(self.state.map_grid):
                    tile = self.state.map_grid[new_y][new_x]
                    if tile.movement_cost <= enemy.movement_points:
                        enemy.x, enemy.y = new_x, new_y
                        enemy.movement_points -= tile.movement_cost
                        self._log(f"Enemy {enemy.name} moves to ({new_x},{new_y}).")
                    else:
                        self._log(f"Enemy {enemy.name} cannot move – insufficient MP.")
                else:
                    self._log(f"Enemy {enemy.name} cannot move – out of bounds.")
            else:
                self._log(f"Enemy {enemy.name} holds position.")

            # Fire at closest friend in range
            closest_friend = None
            min_dist = float('inf')
            for friend in self.state.friendly_units:
                if friend.destroyed:
                    continue
                dist = abs(enemy.x - friend.x) + abs(enemy.y - friend.y)
                if dist < min_dist:
                    min_dist = dist
                    closest_friend = friend
            if closest_friend and min_dist <= 15:
                tile = self.state.map_grid[closest_friend.y][closest_friend.x]
                penalty = 0
                if self.state.is_night:
                    penalty += 20
                if self.state.is_raining:
                    penalty += 10
                # Use simple fire resolution (no ammo types for AI for simplicity)
                hit_chance = enemy.accuracy_base - tile.cover_bonus - penalty + random.randint(-10,10)
                hit = random.randint(1,100) <= hit_chance
                if hit:
                    damage = random.uniform(15, 35) * (1 - closest_friend.armor/100)
                    closest_friend.strength -= damage
                    closest_friend.morale -= damage * 0.5
                    if closest_friend.strength > 0 and closest_friend.faction == self.state.player_faction:
                        self.state.friendly_wia += 1
                else:
                    closest_friend.morale -= 5
                if closest_friend.morale <= enemy.suppress_threshold:
                    closest_friend.suppressed = True
                if closest_friend.strength <= 0 and not closest_friend.destroyed:
                    closest_friend.destroyed = True
                    self.state.friendly_kia += 1
                self._log(f"Enemy {enemy.name} fires at {closest_friend.name} – {'hit' if hit else 'miss'}.")
                check_rout(closest_friend, self.state)

        # Reset enemy movement points
        for u in self.state.enemy_units:
            if not u.destroyed:
                u.movement_points = u.movement

    def _fpv_attack(self, attacker, target):
        if attacker.ammo_he <= 0 and attacker.ammo_ap <= 0:  # FPV uses HE by default
            self._log("No FPV drones available.")
            return
        dist = abs(attacker.x - target.x) + abs(attacker.y - target.y)
        if dist > 10:
            self._log(f"FPV drone out of range ({dist} tiles).")
            return
        hit_chance = attacker.accuracy_base
        hit = random.randint(1,100) <= hit_chance
        if hit:
            damage = random.uniform(30, 60)
            target.strength -= damage
            self._log(f"FPV drone hits {target.name} for {damage:.0f} damage!")
            if target.strength <= 0 and not target.destroyed:
                target.destroyed = True
                if target.faction == self.state.player_faction:
                    self.state.friendly_kia += 1
                    if target.type in ["m1a2_tank", "t90m_tank", "bradley_ifv", "bmp3_ifv", "type99a_tank", "t72s_tank"]:
                        self.state.vehicles_lost += 1
                else:
                    self.state.enemy_kia += 1
                self._log(f"{target.name} destroyed.")
            # Splash damage
            for unit in self.state.enemy_units + self.state.friendly_units:
                if unit.destroyed or unit == target:
                    continue
                splash_dist = abs(unit.x - target.x) + abs(unit.y - target.y)
                if splash_dist <= 1:
                    splash_dmg = random.uniform(10, 20)
                    unit.strength -= splash_dmg
                    self._log(f"Splash damage hits {unit.name} for {splash_dmg:.0f} damage!")
                    if unit.strength <= 0 and not unit.destroyed:
                        unit.destroyed = True
                        if unit.faction == self.state.player_faction:
                            self.state.friendly_kia += 1
                            if unit.type in ["m1a2_tank", "t90m_tank", "bradley_ifv", "bmp3_ifv", "type99a_tank", "t72s_tank"]:
                                self.state.vehicles_lost += 1
                        else:
                            self.state.enemy_kia += 1
                        self._log(f"{unit.name} destroyed by splash.")
        else:
            self._log(f"FPV drone misses {target.name}.")
        # Attacker destroyed
        attacker.destroyed = True
        self._log(f"{attacker.name} destroyed in explosion.")

    def _end_game(self):
        clear_screen()
        if self.state.victory:
            print(f"{FACTIONS[self.player_faction]['ansi_color']}VICTORY!{ANSI['RESET']}")
        else:
            print(f"{ANSI['WARNING']}DEFEAT.{ANSI['RESET']}")
        input("Press Enter.")
