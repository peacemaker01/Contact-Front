import random
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
        while not self.state.game_over and self.state.turn <= self.state.max_turns:
            map_render = render_strategic_map(self.state)
            render_strategic_hud(self.state, map_render)
            cmd = input().strip()
            if cmd.lower() == "quit":
                break
            if cmd.lower().startswith("strike"):
                self._resolve_strike(cmd)
            elif cmd.lower().startswith("ew"):
                self._apply_ew(cmd)
            elif "nuclear" in cmd.lower() or "nuke" in cmd.lower():
                self._nuclear_prompt(cmd)
            else:
                self.state.narrative_log.append("Command not recognized. Use 'strike <count> <asset> at <target>', 'ew <type>', or nuclear commands.")
            self._opponent_turn()
            self._decay_ew_effects()
            self.state.turn += 1
        self._end_game()

    def _resolve_strike(self, cmd):
        parts = cmd.split()
        try:
            count = int(parts[1])
            asset = parts[2]
            target = " ".join(parts[4:]) if len(parts) > 4 else "unknown"
            if asset in self.state.player_assets and self.state.player_assets[asset] >= count:
                self.state.player_assets[asset] -= count
                # Intercept calculation
                ad_batteries = self.state.opponent_assets.get("patriot_pac3", 0) + self.state.opponent_assets.get("s400_triumf", 0)
                intercept_rate = min(90, 50 + ad_batteries)
                if any(e.get('type') == 'radar_sup' for e in self.state.active_ew_effects):
                    intercept_rate -= 30
                intercepted = int(count * (intercept_rate / 100))
                hits = count - intercepted
                damage = hits * random.uniform(5, 15)
                self.state.strike_log.append({"narrative": f"Struck {target} with {count} {asset}. {intercepted} intercepted, {hits} hits, {damage:.1f}% damage.", "damage": damage})
                self.state.narrative_log.append(f"Strike on {target}: {hits} hits, {damage:.1f}% damage.")
                # Record enemy asset sightings
                if "carrier" in target.lower():
                    self.state.enemy_asset_last_seen["carrier"] = self.state.turn
                if "patriot" in target.lower() or "air defense" in target.lower():
                    self.state.enemy_asset_last_seen["patriot"] = self.state.turn
                if "missile" in target.lower() or "silo" in target.lower():
                    self.state.enemy_asset_last_seen["missile_silo"] = self.state.turn
                self._update_escalation("strike_on_homeland" if "capital" in target.lower() else "strike")
            else:
                self.state.narrative_log.append("Insufficient assets.")
        except Exception as e:
            self.state.narrative_log.append(f"Invalid strike command: {e}")

    def _apply_ew(self, cmd):
        ew_type = cmd.split()[1] if len(cmd.split()) > 1 else "gps_jam"
        self.state.active_ew_effects.append({"type": ew_type, "duration": 2})
        self.state.narrative_log.append(f"Electronic warfare activated: {ew_type} for 2 turns.")

    def _nuclear_prompt(self, cmd):
        if self.state.escalation_level < 3:
            self.state.narrative_log.append("Nuclear weapons not yet authorized. Escalate further first.")
            return
        print(f"{ANSI['NUCLEAR']}!!! NUCLEAR LAUNCH DETECTED !!!{ANSI['RESET']}")
        print("Type 'CONFIRM NUCLEAR' to authorize, or anything else to cancel.")
        confirm = input("> ")
        if confirm.upper() == "CONFIRM NUCLEAR":
            print("FINAL CONFIRMATION: Type the target coordinates or name to proceed.")
            target = input("> ")
            if target:
                self._execute_nuclear_strike(target)
        else:
            print("Nuclear strike cancelled.")

    def _execute_nuclear_strike(self, target):
        self.state.narrative_log.append(f"NUCLEAR STRIKE on {target}!")
        self._update_escalation("tactical_nuke")
        for asset in list(self.state.opponent_assets.keys()):
            self.state.opponent_assets[asset] = max(0, self.state.opponent_assets[asset] - random.randint(50, 90))
        self.state.strike_log.append({"narrative": f"Nuclear strike on {target}. Enemy assets devastated."})

    def _update_escalation(self, trigger):
        triggers = {
            "strike_on_homeland": 2,
            "strike": 1,
            "tactical_nuke": 4,
            "asat_attack": 1
        }
        delta = triggers.get(trigger, 1)
        self.state.escalation_level = min(6, self.state.escalation_level + delta)
        self.state.narrative_log.append(f"Escalation increased to level {self.state.escalation_level} (+{delta})")
        if self.state.escalation_level >= 6:
            self.state.game_over = True
            self.state.victory = False
            self.state.narrative_log.append("NUCLEAR EXCHANGE - GAME OVER")

    def _opponent_turn(self):
        # Opponent retaliation and asset revelation
        if random.random() < 0.4 and self.state.opponent_assets.get("kalibr_cruise_missile", 0) > 0:
            self.state.opponent_assets["kalibr_cruise_missile"] -= 1
            self.state.narrative_log.append(f"{self.opponent_faction} launches a cruise missile strike.")
            # Reveal enemy missile silo location
            self.state.enemy_asset_last_seen["missile_silo"] = self.state.turn
            self._update_escalation("strike")

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
