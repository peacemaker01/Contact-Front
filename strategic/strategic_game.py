import random
import threading
import itertools
import sys
from factions import FACTIONS
from llm_engine import LLMEngine
from hud import render_strategic_hud, clear_screen, ANSI
from game_state import StrategicGameState
from strategic.theater_map import render_strategic_map

class StrategicGame:
    def __init__(self, player_faction, opponent_faction):
        self.player_faction = player_faction
        self.opponent_faction = opponent_faction
        self.llm = LLMEngine()
        self.state = None
        self._init_game()

    def _init_game(self):
        p_assets = {k: v["count"] for k,v in FACTIONS[self.player_faction]["strategic_assets"].items() if isinstance(v, dict) and "count" in v}
        o_assets = {k: v["count"] for k,v in FACTIONS[self.opponent_faction]["strategic_assets"].items() if isinstance(v, dict) and "count" in v}
        self.state = StrategicGameState(
            turn=1, max_turns=15, player_faction=self.player_faction, opponent_faction=self.opponent_faction,
            player_assets=p_assets, opponent_assets=o_assets, active_ew_effects=[],
            satellites_operational={"gps": True, "recon": True, "comms": True}, escalation_level=2,
            nuclear_authorized=False, known_opponent_actions=[], strike_log=[], narrative_log=[],
            enemy_asset_last_seen={}
        )

    def run(self):
        try:
            while not self.state.game_over and self.state.turn <= self.state.max_turns:
                map_render = render_strategic_map(self.state)
                render_strategic_hud(self.state, map_render)
                print(f"{ANSI['TITLE']}> {ANSI['RESET']}", end="", flush=True)
                cmd = input().strip()
                if cmd.lower() == "quit":
                    break
                # Use LLM to parse strategic command (E3)
                action = self._parse_strategic_command(cmd)
                if not action:
                    self.state.narrative_log.append("Command not understood. Use natural language.")
                    continue
                self._execute_strategic_action(action)
                self._opponent_turn()
                self._recon_satellite_pass()  # M11
                self._decay_ew_effects()
                self.state.turn += 1
        except KeyboardInterrupt:
            print(f"\n{ANSI['WARNING']}Game interrupted.{ANSI['RESET']}")
        self._end_game()

    def _parse_strategic_command(self, cmd):
        if not self.llm.api_key:
            # Fallback to simple keyword matching (minimal)
            if cmd.startswith("strike"):
                parts = cmd.split()
                if len(parts) >= 4:
                    try:
                        count = int(parts[1])
                        asset = parts[2]
                        target = " ".join(parts[4:])
                        return {"type": "strike", "asset": asset, "count": count, "target": target}
                    except:
                        pass
            elif cmd.startswith("ew"):
                return {"type": "ew", "effect": cmd.split()[1] if len(cmd.split())>1 else "gps_jam"}
            return None
        # Use LLM
        summary = {
            "turn": self.state.turn,
            "player_assets": self.state.player_assets,
            "opponent_assets": {k: v for k,v in self.state.opponent_assets.items() if v > 0},
            "escalation_level": self.state.escalation_level,
            "active_ew": [e['type'] for e in self.state.active_ew_effects]
        }
        prompt = f"""You are a strategic command interpreter. Convert the player's order into JSON.
Player command: "{cmd}"
Available assets: {list(self.state.player_assets.keys())}
Output JSON with fields: "type" ("strike", "ew", "recon", "nuclear"), "asset" (string), "count" (int), "target" (string), "effect" (for EW).
Example: {{"type": "strike", "asset": "bgm_109_tomahawk", "count": 10, "target": "airbase"}}
Output ONLY valid JSON.
"""
        messages = [{"role": "system", "content": prompt}]
        resp = self.llm._call(messages, temperature=0.2)
        try:
            if resp.startswith("```json"):
                resp = resp[7:]
            if resp.endswith("```"):
                resp = resp[:-3]
            return json.loads(resp)
        except:
            return None

    def _execute_strategic_action(self, action):
        atype = action.get("type")
        if atype == "strike":
            self._resolve_strike(action.get("asset"), action.get("count", 1), action.get("target", "unknown"))
        elif atype == "ew":
            self._apply_ew(action.get("effect", "gps_jam"))
        elif atype == "recon":
            self._recon_satellite_pass(forced=True)
        elif atype == "nuclear":
            self._nuclear_prompt(action.get("target", ""))
        else:
            self.state.narrative_log.append("Unknown action type.")

    def _resolve_strike(self, asset, count, target):
        if asset not in self.state.player_assets or self.state.player_assets[asset] < count:
            self.state.narrative_log.append("Insufficient assets.")
            return
        self.state.player_assets[asset] -= count
        # Intercept calculation – fixed M13
        ad_rate = 0.0
        for ad_key in ["patriot_pac3", "s400_triumf", "hq9_sam", "bavar373_sam"]:
            if self.state.opponent_assets.get(ad_key, 0) > 0:
                rate = {"patriot_pac3":0.85, "s400_triumf":0.90, "hq9_sam":0.80, "bavar373_sam":0.65}[ad_key]
                ad_rate = max(ad_rate, rate)
        if any(e.get('type') == 'radar_sup' for e in self.state.active_ew_effects):
            ad_rate = max(0.0, ad_rate - 0.30)
        intercepted = int(count * ad_rate)
        hits = count - intercepted
        damage = hits * random.uniform(5, 15)
        self.state.strike_log.append({"narrative": f"Strike on {target}: {count} {asset}, {intercepted} intercepted, {hits} hits, {damage:.1f}% damage."})
        self.state.narrative_log.append(f"Strike: {hits} hits, {damage:.1f}% damage.")
        if "carrier" in target.lower():
            self.state.enemy_asset_last_seen["carrier"] = self.state.turn
        if "patriot" in target.lower() or "air defense" in target.lower():
            self.state.enemy_asset_last_seen["patriot"] = self.state.turn
        self._update_escalation("strike_on_homeland" if "capital" in target.lower() else "strike")

    def _apply_ew(self, effect):
        self.state.active_ew_effects.append({"type": effect, "duration": 2})
        self.state.narrative_log.append(f"EW activated: {effect} for 2 turns.")

    def _recon_satellite_pass(self, forced=False):
        if not self.state.satellites_operational.get("recon", True):
            self.state.narrative_log.append("Recon satellite offline.")
            return
        if forced or self.state.turn % 2 == 0:
            visible_assets = [k for k in self.state.opponent_assets.keys() if self.state.opponent_assets[k] > 0]
            if visible_assets:
                asset = random.choice(visible_assets)
                self.state.enemy_asset_last_seen[asset] = self.state.turn
                self.state.narrative_log.append(f"Satellite overflight: {asset.replace('_',' ')} detected.")

    def _opponent_turn(self):
        # Dynamic asset selection – M5 fixed
        strike_assets = [k for k, v in self.state.opponent_assets.items() if v > 0 and any(w in k for w in ["missile", "tomahawk", "kalibr", "ballistic", "shahed", "df"])]
        if not strike_assets:
            return
        chosen = random.choice(strike_assets)
        if random.random() < 0.4:
            self.state.opponent_assets[chosen] -= 1
            self.state.narrative_log.append(f"{self.opponent_faction} launches {chosen.replace('_',' ')}.")
            self.state.enemy_asset_last_seen["missile_silo"] = self.state.turn
            self._update_escalation("strike")

    def _update_escalation(self, trigger):
        triggers = {"strike_on_homeland": 2, "strike": 1, "tactical_nuke": 4, "asat_attack": 1}
        delta = triggers.get(trigger, 1)
        self.state.escalation_level = min(6, self.state.escalation_level + delta)
        self.state.narrative_log.append(f"Escalation to level {self.state.escalation_level} (+{delta})")
        if self.state.escalation_level >= 6:
            self.state.game_over = True
            self.state.victory = False
            self.state.narrative_log.append("NUCLEAR EXCHANGE – GAME OVER")

    def _nuclear_prompt(self, target):
        if self.state.escalation_level < 3:
            self.state.narrative_log.append("Nuclear weapons not yet authorized.")
            return
        print(f"{ANSI['NUCLEAR']}!!! NUCLEAR LAUNCH DETECTED !!!{ANSI['RESET']}")
        print("Type 'CONFIRM NUCLEAR' to authorize, or anything else to cancel.")
        confirm = input("> ")
        if confirm.upper() == "CONFIRM NUCLEAR":
            print("FINAL CONFIRMATION: Type the target coordinates or name to proceed.")
            target2 = input("> ")
            if target2:
                self._execute_nuclear_strike(target2)
        else:
            print("Nuclear strike cancelled.")

    def _execute_nuclear_strike(self, target):
        self.state.narrative_log.append(f"NUCLEAR STRIKE on {target}!")
        self._update_escalation("tactical_nuke")
        for asset in list(self.state.opponent_assets.keys()):
            self.state.opponent_assets[asset] = max(0, self.state.opponent_assets[asset] - random.randint(50, 90))
        self.state.strike_log.append({"narrative": f"Nuclear strike on {target}. Enemy assets devastated."})

    def _decay_ew_effects(self):
        new_effects = []
        for e in self.state.active_ew_effects:
            e['duration'] -= 1
            if e['duration'] > 0:
                new_effects.append(e)
        self.state.active_ew_effects = new_effects

    def _end_game(self):
        clear_screen()
        if self.state.escalation_level >= 6:
            print(f"{ANSI['NUCLEAR']}NUCLEAR EXCHANGE - ALL LOSE{ANSI['RESET']}")
        elif self.state.turn > self.state.max_turns:
            print(f"{ANSI['INTEL']}Game ended. Escalation level {self.state.escalation_level}{ANSI['RESET']}")
        else:
            print(f"{ANSI['INTEL']}Game over.{ANSI['RESET']}")
        input("Press Enter.")
