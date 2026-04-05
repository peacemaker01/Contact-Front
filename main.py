# main.py - Contact Front (Complete with all fixes)
import os
import time
import random
from config import COLORS, ZONE_MAP, COVER_VALUES, DIFFICULTY
from models import GameState, StrategicAsset
from scenarios import generate_mission, generate_strategic_scenario
from save_manager import save_game, load_game, list_saves
from llm_client import parse_player_intent, generate_narrative, validate_intent
from rules import (
    resolve_mortar, resolve_fpv_drone, resolve_infantry_fire,
    resolve_advance, resolve_surrender, resolve_recon, check_victory,
    area_suppression, morale_cascade, apply_command_delay,
    recover_suppression, apply_leader_boost, tactical_resupply,
    change_weather, civilian_casualty_check, decay_detection,
    refresh_detection, apply_civilian_tension,
    strategic_missile_strike, strategic_naval_engagement,
    strategic_loitering_strike, strategic_cyber_attack,
    strategic_space_deployment, strategic_psyops,
    strategic_resupply, strategic_produce_missiles,
    strategic_sanctions, strategic_nuclear_strike
)
from enemy_ai import process_enemy_turn, process_strategic_enemy_turn, apply_guerrilla_tactics
from factions import FACTIONS, get_faction_banner, get_faction_color
from campaign import save_campaign, load_campaign, apply_campaign_bonus

def print_c(text, color="RESET"):
    print(f"{COLORS.get(color, COLORS['RESET'])}{text}{COLORS['RESET']}")

def draw_header():
    print_c("="*65, "HEADER")
    print_c("     ______ CONTACT FRONT: TERMINAL WARGAME ______", "BOLD")
    print_c("="*65, "HEADER")

def morale_bar(value, max_val=100, width=10):
    filled = int(width * value / max_val)
    bar = "█" * filled + "░" * (width - filled)
    return f"[{bar}] {value}%"

def draw_tactical_map(state):
    zones = ["extreme", "long", "medium", "close"]
    print_c("┌─────────────────────────────────────────────────┐", "DIM")
    print_c("│                    BATTLEFIELD                  │", "BOLD")
    print_c("├─────────┬──────────┬──────────┬─────────────────┤", "DIM")
    print_c("│ ZONE    │ COVER    │ FRIENDLY │ ENEMY           │", "BOLD")
    print_c("├─────────┼──────────┼──────────┼─────────────────┤", "DIM")
    for zone in zones:
        cover = COVER_VALUES.get(zone, 0) * 100
        cover_str = f"{cover:3.0f}%"
        friend_icons = []
        for u in state.units:
            if u.side == "attacker" and u.zone == zone and u.is_alive():
                icon = "⚔️" if u.type == "lmg" else "🔫"
                if u.is_leader:
                    icon = "⭐" + icon
                friend_icons.append(icon)
        enemy_icons = []
        if state.recon_active > 0:
            for u in state.units:
                if u.side == "defender" and u.zone == zone and u.is_alive():
                    icon = "⚡" if u.type == "lmg" else "🔫"
                    enemy_icons.append(icon)
        else:
            if any(u.side == "defender" and u.zone == zone for u in state.units):
                enemy_icons = ["?"]
        friendly_str = " ".join(friend_icons) if friend_icons else "-"
        enemy_str = " ".join(enemy_icons) if enemy_icons else "-"
        print_c(f"│ {zone.upper():<7} │ {cover_str:<8} │ {friendly_str:<8} │ {enemy_str:<15} │", "RESET")
    print_c("└─────────┴──────────┴──────────┴─────────────────┘", "DIM")

def draw_strategic_map(state):
    tension = state.global_tension
    enemy_warships = state.enemy_strategic.warships
    enemy_missiles = state.enemy_strategic.ballistic_missiles
    map_lines = []
    map_lines.append("   ┌─────┬─────┬─────┬─────┬─────┐")
    map_lines.append("   │  N  │  W  │  C  │  E  │  S  │")
    map_lines.append("   ├─────┼─────┼─────┼─────┼─────┤")
    rows = [
        ("WARSHIPS", enemy_warships, "🚢"),
        ("MISSILES", enemy_missiles, "🚀"),
        ("AIR DEF", state.enemy_strategic.air_defense, "🛡️"),
        ("NUKES", state.enemy_strategic.nukes, "☢️"),
        ("TENSION", tension, "🔥")
    ]
    for i, (label, value, icon) in enumerate(rows):
        bar_len = min(5, value // 20 if value > 0 else 0)
        bar = "█" * bar_len + "░" * (5 - bar_len)
        if i == 0:
            map_lines.append(f"│{label:<7} │ {bar} │ {icon} {value} │")
        else:
            map_lines.append(f"├─────┼─────┼─────┼─────┼─────┤")
            map_lines.append(f"│{label:<7} │ {bar} │ {icon} {value} │")
    map_lines.append("   └─────┴─────┴─────┴─────┴─────┘")
    for line in map_lines:
        print_c(line, "CYAN" if "WARSHIPS" in line else "YELLOW" if "TENSION" in line else "RESET")

def show_aar(state):
    print_c("\n" + "═"*65, "HEADER")
    print_c("                AFTER-ACTION REPORT", "BOLD")
    print_c("═"*65, "HEADER")
    attacker_losses = len([u for u in state.units if u.side == "attacker" and u.status == "kia"])
    defender_losses = len([u for u in state.units if u.side == "defender" and u.status == "kia"])
    total_shots = sum(30 - u.ammo for u in state.units if u.side == "attacker" and u.type == "rifleman")
    print_c(f" Friendly KIA: {attacker_losses}", "RED")
    print_c(f" Enemy KIA:    {defender_losses}", "GREEN")
    print_c(f" Rounds fired: {total_shots}", "CYAN")
    print_c(f" Turns played: {state.turn-1}", "CYAN")
    print_c(f" Final objective status: {'SECURED' if state.winner == 'attacker' else 'LOST'}", "YELLOW")
    lessons = [
        "Use recon before advancing.",
        "Mortars are most effective against suppressed targets.",
        "FPV drones can eliminate high-value targets.",
        "Suppression pins enemies, making them easy targets.",
        "Morale breaks faster than bodies."
    ]
    print_c("\n Lessons Learned:", "BOLD")
    print_c(f" • {random.choice(lessons)}", "GREEN")
    input("\nPress Enter to continue...")

def show_help():
    print_c("\n=== TACTICAL COMMANDS ===", "CYAN")
    print_c('  "mortar the medium zone with two rounds"', "GREEN")
    print_c('  "send an fpv drone after the heavy gunner"', "GREEN")
    print_c('  "advance to close"', "GREEN")
    print_c('  "launch recon drone"', "GREEN")
    print_c('  "suppress the building"', "GREEN")
    print_c('  "demand surrender"', "GREEN")
    print_c('  "resupply"', "GREEN")
    print_c('  "roe"', "GREEN")
    print_c('  "what does the drone see?" (info, no turn)', "GREEN")
    print_c("\n=== STRATEGIC COMMANDS ===", "CYAN")
    print_c('  "targets" - List all targetable enemy assets', "GREEN")
    print_c('  "launch ballistic missiles at warships"', "RED")
    print_c('  "fire cruise missiles at air defense"', "RED")
    print_c('  "launch hypersonic missiles at missile silos"', "RED")
    print_c('  "deploy loitering munitions"', "RED")
    print_c('  "fire anti-ship missiles"', "RED")
    print_c('  "deploy warships"', "RED")
    print_c('  "gather intelligence"', "RED")
    print_c('  "use electronic warfare"', "RED")
    print_c('  "cyber attack"', "RED")
    print_c('  "deploy space assets"', "RED")
    print_c('  "psyops"', "RED")
    print_c('  "resupply"', "RED")
    print_c('  "produce missiles"', "RED")
    print_c('  "impose sanctions"', "RED")
    print_c('  "clear IEDs"', "RED")
    print_c('  "authorize nuclear strike" (extreme)', "RED")
    print_c("\n=== SYSTEM ===", "CYAN")
    print_c('  "save", "history", "exit"', "GREEN")
    print_c("")

def show_history(state):
    print_c("\n=== MISSION HISTORY ===", "HEADER")
    for i, h in enumerate(state.history[-10:]):
        print_c(f"Turn {i+1}: {h}", "YELLOW")
    print_c("")

def display_hud(state):
    print_c(f"\n{'='*65}", "HEADER")
    if state.game_mode == "tactical":
        print_c(f" TURN {state.turn} | OBJ: {state.objective_zone.upper()} | {state.weather.upper()}", "BOLD")
        print_c(f" MORTARS: {state.mortar_rounds} | FPV: {state.fpv_drones} | RECON: {state.recon_active}t", "CYAN")
        print_c(f" SUPPLY: {state.supply_points} | IED THREAT: {state.ied_threat}%", "CYAN")
        if state.building_damage > 0:
            bar = "█" * (state.building_damage // 10) + "░" * (10 - state.building_damage // 10)
            print_c(f" BUILDING DAMAGE: [{bar}] {state.building_damage}%", "YELLOW")
        print_c(f"{'-'*65}", "HEADER")
        if state.player_faction:
            print_c(get_faction_banner(state.player_faction), FACTIONS[state.player_faction]["color"])
            print_c("VS", "BOLD")
            print_c(get_faction_banner(state.enemy_faction), FACTIONS[state.enemy_faction]["color"])
        if state.terrain_description and state.turn == 1:
            print_c(f" TERRAIN: {state.terrain_description[:100]}...", "YELLOW")
        draw_tactical_map(state)
        print_c(" FRIENDLY:", "GREEN")
        for u in state.units:
            if u.side == "attacker" and u.is_alive():
                sc = "GREEN" if u.status == "active" else "WARNING"
                leader_mark = " [L]" if u.is_leader else ""
                print_c(f"   [{u.zone.upper():<7}] {u.name}{leader_mark:<15} | {u.status.upper()} | AMMO:{u.ammo}", sc)
                print_c(f"      Morale: {morale_bar(u.morale)}", "YELLOW")
        print_c("\n ENEMY:", "RED")
        if state.recon_active > 0:
            for u in state.units:
                if u.side == "defender" and u.is_alive():
                    sc = "FAIL" if u.status == "active" else "WARNING"
                    note = " (PANICKING)" if u.morale < 30 else " (SHAKEN)" if u.morale < 50 else ""
                    print_c(f"   [{u.zone.upper():<7}] {u.name:<15} | {u.status.upper()} | MORALE:{u.morale}{note}", sc)
        else:
            print_c("   No direct visual – recon drone recommended", "DIM")
    else:  # strategic mode
        print_c(f" TURN {state.turn} | TENSION: {state.global_tension}% | CASUALTIES: {state.civilian_casualties:,}", "BOLD")
        print_c(f" NUKES: {state.strategic.nukes} | BALLISTIC: {state.strategic.ballistic_missiles} | CRUISE: {state.strategic.cruise_missiles}", "CYAN")
        print_c(f" HYPERSONIC: {state.strategic.hypersonic_missiles} | LOITERING: {state.strategic.loitering_munitions} | ASM: {state.strategic.anti_ship_missiles}", "CYAN")
        print_c(f" WARSHIPS: {state.strategic.warships} | INTEL: {state.strategic.intelligence_level} | EW: {state.strategic.electronic_warfare}", "CYAN")
        print_c(f" CYBER: {state.strategic.cyber_warfare} | SPACE: {state.strategic.space_assets}", "CYAN")
        print_c(f" ECONOMY: {state.war_economy}% | PRODUCTION: {state.production_points} | SUPPLY: {state.supply_points}", "CYAN")
        print_c(f"{'-'*65}", "HEADER")
        print_c(f" ENEMY ASSETS:", "RED")
        print_c(f"   NUKES: {state.enemy_strategic.nukes} | BALLISTIC: {state.enemy_strategic.ballistic_missiles} | WARSHIPS: {state.enemy_strategic.warships}", "RED")
        print_c(f"   HYPERSONIC: {state.enemy_strategic.hypersonic_missiles} | LOITERING: {state.enemy_strategic.loitering_munitions} | ASM: {state.enemy_strategic.anti_ship_missiles}", "RED")
        print_c(f"   INTEL: {state.enemy_strategic.intelligence_level} | EW: {state.enemy_strategic.electronic_warfare}", "RED")
        draw_strategic_map(state)
        if state.terrain_description:
            print_c(f" SITUATION: {state.terrain_description[:150]}", "YELLOW")
    print_c(f"{'='*65}\n", "HEADER")

def generate_tactical_intel(state):
    defenders = [u for u in state.units if u.side == "defender" and u.is_alive()]
    if not defenders:
        state.enemy_posture = "none"
        state.enemy_ammo_estimate = "none"
        state.enemy_morale_trend = "none"
        return
    if any(d.morale > 70 and d.status != "suppressed" for d in defenders):
        state.enemy_posture = "aggressive"
    elif any(d.morale < 30 for d in defenders):
        state.enemy_posture = "routed"
    else:
        state.enemy_posture = "defensive"
    avg_ammo = sum(d.ammo for d in defenders) / len(defenders)
    if avg_ammo > 20:
        state.enemy_ammo_estimate = "plentiful"
    elif avg_ammo > 10:
        state.enemy_ammo_estimate = "moderate"
    else:
        state.enemy_ammo_estimate = "low"
    avg_morale = sum(d.morale for d in defenders) / len(defenders)
    if avg_morale > 70:
        state.enemy_morale_trend = "high"
    elif avg_morale > 40:
        state.enemy_morale_trend = "steady"
    else:
        state.enemy_morale_trend = "breaking"

def check_strategic_victory(state):
    if state.game_over:
        return
    if (state.enemy_strategic.ballistic_missiles <= 0 and
        state.enemy_strategic.warships <= 0 and
        state.global_tension < 50):
        state.game_over = True
        state.winner = state.player_faction
        state.last_narrative = "Enemy strategic capability destroyed. Victory!"
    elif (state.strategic.ballistic_missiles <= 0 and state.strategic.warships <= 0):
        state.game_over = True
        state.winner = state.enemy_faction
        state.last_narrative = "Your strategic forces are annihilated. Defeat."
    elif state.global_tension >= 100:
        state.game_over = True
        state.winner = "nobody"
        state.last_narrative = "Global thermonuclear war. Humanity loses."

def setup_menu():
    try:
        with open('.env', 'r') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#'):
                    key, value = line.split('=', 1)
                    os.environ[key] = value
    except:
        pass
    draw_header()
    api_key = os.environ.get("LLM_API_KEY")
    if api_key:
        print_c("  LLM: ENABLED (Natural language with AI)", "GREEN")
    else:
        print_c("  LLM: DISABLED (local parser only)", "WARNING")
    print_c("\n[1] TACTICAL MODE (Infantry, mortars, drones)", "CYAN")
    print_c("[2] STRATEGIC MODE (Missiles, warships, nukes)", "CYAN")
    print_c("[3] RESUME MISSION", "CYAN")
    mode_choice = input("\nSelect > ").strip()
    if mode_choice == "3":
        saves = list_saves()
        if not saves:
            print_c("No saves found.", "WARNING")
            time.sleep(1)
            return setup_menu()
        print("\nSaves:")
        for i, s in enumerate(saves):
            print(f"  {i+1}. {s}")
        idx = int(input("Select save: ")) - 1
        return load_game(f"saves/{saves[idx]}")
    
    if mode_choice == "2":
        print_c("\nSTRATEGIC SCENARIOS:", "HEADER")
        print("1. Strait of Hormuz (Naval confrontation)")
        print("2. Missile Exchange (Conventional strike)")
        print("3. Nuclear Brinkmanship (High tension)")
        sc_choice = input("> ").strip()
        scenario_map = {"1": "Strait of Hormuz", "2": "Missile Exchange", "3": "Nuclear Brinkmanship"}
        scenario = scenario_map.get(sc_choice, "Strait of Hormuz")
        factions_list = list(FACTIONS.keys())
        print_c("\nSELECT YOUR FACTION:", "HEADER")
        for i, f in enumerate(factions_list, 1):
            print_c(f"  {i}. {get_faction_banner(f)}", FACTIONS[f]["color"])
        f_choice = input("> ").strip()
        try:
            player_faction = factions_list[int(f_choice)-1]
        except:
            player_faction = "USA"
        print_c("\nSELECT ENEMY FACTION:", "HEADER")
        for i, f in enumerate(factions_list, 1):
            print_c(f"  {i}. {get_faction_banner(f)}", FACTIONS[f]["color"])
        e_choice = input("> ").strip()
        try:
            enemy_faction = factions_list[int(e_choice)-1]
        except:
            enemy_faction = "IRAN"
        print_c("\nDIFFICULTY:", "HEADER")
        print("1. Easy  2. Normal  3. Hard  4. Realistic")
        diff_map = {"1": "easy", "2": "normal", "3": "hard", "4": "realistic"}
        diff = diff_map.get(input("> "), "normal")
        state = generate_strategic_scenario(scenario, diff, player_faction, enemy_faction)
        return state
    else:
        print_c("\n[1] OPERATION: BROKEN ANVIL (Farmhouse assault)", "CYAN")
        print_c("[2] OPERATION: SILENT SWEEP (Night raid)", "CYAN")
        mission_choice = input("Select mission > ").strip()
        mission = "night_raid" if mission_choice == "2" else "broken_anvil"
        print_c("\nDIFFICULTY:", "HEADER")
        print("1. Easy  2. Normal  3. Hard  4. Realistic")
        diff_map = {"1": "easy", "2": "normal", "3": "hard", "4": "realistic"}
        diff = diff_map.get(input("> "), "normal")
        factions_list = list(FACTIONS.keys())
        print_c("\nSELECT YOUR FACTION:", "HEADER")
        for i, f in enumerate(factions_list, 1):
            print_c(f"  {i}. {get_faction_banner(f)}", FACTIONS[f]["color"])
        f_choice = input("> ").strip()
        try:
            player_faction = factions_list[int(f_choice)-1]
        except:
            player_faction = "NATO"
        print_c("\nSELECT ENEMY FACTION:", "HEADER")
        for i, f in enumerate(factions_list, 1):
            print_c(f"  {i}. {get_faction_banner(f)}", FACTIONS[f]["color"])
        e_choice = input("> ").strip()
        try:
            enemy_faction = factions_list[int(e_choice)-1]
        except:
            enemy_faction = "RUSSIA"
        print_c("\nCHOOSE YOUR SIDE:", "HEADER")
        print("1. Attacker  2. Defender")
        side_choice = input("> ").strip()
        player_side = "attacker" if side_choice == "1" else "defender"
        provider = os.environ.get("LLM_PROVIDER", "openrouter")
        api_key = os.environ.get("LLM_API_KEY")
        state = generate_mission(mission, diff, provider, api_key, player_faction, enemy_faction, player_side)
        return state

def main():
    state = setup_menu()
    if state.game_mode == "tactical":
        apply_campaign_bonus(state)
    print_c("\n" + "="*65, "CYAN")
    print_c("                     MISSION BRIEF", "BOLD")
    print_c("="*65, "CYAN")
    print_c(f"\n{state.last_narrative}\n", "YELLOW")
    print_c("="*65, "CYAN")
    input("\nPress Enter to begin...")
    print("\n")

    provider = os.environ.get("LLM_PROVIDER", "openrouter")
    api_key = os.environ.get("LLM_API_KEY")

    while not state.game_over:
        print_c(f'GM: "{state.last_narrative}"', "YELLOW")
        display_hud(state)
        try:
            cmd = input("COMMANDER > ").strip()
        except KeyboardInterrupt:
            break
        if not cmd:
            continue
        if cmd.lower() == "help":
            show_help()
            continue
        if cmd.lower() == "history":
            show_history(state)
            continue
        if cmd.lower() == "save":
            fname = save_game(state)
            print_c(f"Game saved to {fname}", "GREEN")
            continue
        if cmd.lower() == "exit":
            break

        intent = parse_player_intent(cmd, provider, api_key)
        print_c(f"\n--- EXECUTING ORDERS ({intent.get('type','UNKNOWN').upper()}) ---", "CYAN")

        # Validate intent
        valid, err = validate_intent(intent)
        if not valid:
            print_c(f"Invalid command: {err}", "WARNING")
            continue

        if intent["type"] == "info":
            print_c("\n(Current battlefield information is shown in the HUD above.)", "CYAN")
            continue

        if state.game_mode == "tactical":
            # IED ambush
            if random.random() < state.ied_threat / 100:
                print_c("\n⚠️ IED EXPLOSION! ⚠️", "RED")
                attackers = [u for u in state.units if u.side == "attacker" and u.is_alive()]
                if attackers:
                    target = random.choice(attackers)
                    target.hits += 10
                    target.morale -= 30
                    if target.hits >= 15:
                        target.status = "kia"
                        print_c(f"IED kills {target.name}!", "RED")
                    else:
                        print_c(f"IED wounds {target.name}!", "RED")
                    morale_cascade(state, target)
            narrative = ""
            if intent["type"] == "mortar":
                narrative = resolve_mortar(state, intent.get("zone","medium"), intent.get("quantity",1))
            elif intent["type"] == "fpv":
                narrative = resolve_fpv_drone(state, intent.get("target"))
            elif intent["type"] == "recon":
                narrative = resolve_recon(state)
            elif intent["type"] == "advance":
                narrative = resolve_advance(state, intent.get("zone","medium"))
            elif intent["type"] == "attack":
                narrative = resolve_infantry_fire(state, "attacker", intent.get("zone","medium"))
            elif intent["type"] == "surrender":
                narrative = resolve_surrender(state)
            elif intent["type"] == "resupply":
                narrative = tactical_resupply(state)
            elif intent["type"] == "roe":
                print_c(f"Current ROE: {state.roe.upper()}", "CYAN")
                print_c(f"Civilian density: {state.civilian_density}%", "YELLOW")
                print_c(f"Media attention: {state.media_attention}", "YELLOW")
                print_c(f"War crimes allegations: {state.war_crimes_allegations}", "YELLOW")
                continue
            else:
                narrative = "Command not recognized. Try 'help'."

            print_c(f"\n{narrative}", "GREEN")
            # Civilian casualty check
            if intent["type"] in ["mortar", "attack"]:
                civ_msg = civilian_casualty_check(state, intent.get("zone","medium"))
                if civ_msg:
                    print_c(civ_msg, "RED")
            check_victory(state)
            enemy_narrative = ""
            if not state.game_over:
                print_c("\n--- ENEMY TURN ---", "RED")
                enemy_narrative = process_enemy_turn(state, provider, api_key)
                print_c(f"{enemy_narrative}", "RED")
                check_victory(state)
            # Recovery and leader boost
            rec_msg = recover_suppression(state)
            if rec_msg:
                print_c(rec_msg, "GREEN")
            lead_msg = apply_leader_boost(state)
            if lead_msg:
                print_c(lead_msg, "CYAN")
            weather_msg = change_weather(state)
            if weather_msg:
                print_c(weather_msg, "YELLOW")
            # Detection decay
            decay_detection(state)
            # Apply civilian tension
            civ_tension = apply_civilian_tension(state)
            if civ_tension:
                print_c(civ_tension, "RED")
            state.supply_points = min(200, state.supply_points + random.randint(1, 3))
            generate_tactical_intel(state)
            combined = f"{narrative} {enemy_narrative}"
            state.last_narrative = generate_narrative(combined, state, provider, api_key)
            state.history.append(f"Turn {state.turn}: {narrative[:80]}")
            state.turn += 1
            if state.recon_active > 0:
                state.recon_active -= 1
            if state.ied_threat > 0:
                state.ied_threat = max(0, state.ied_threat - random.randint(0, 5))
        else:  # strategic mode
            # Confirmation for nuclear strike
            if intent["type"] == "nuke":
                print_c("⚠️  NUCLEAR STRIKE AUTHORIZATION REQUIRED ⚠️", "RED")
                confirm = input("Type 'CONFIRM' to launch nuclear weapon: ").strip()
                if confirm != "CONFIRM":
                    print_c("Nuclear strike cancelled.", "GREEN")
                    continue
            # Route to strategic rule functions
            narrative = ""
            if intent["type"] == "missile":
                narrative = strategic_missile_strike(state, intent.get("missile_type", "ballistic"), intent.get("target_asset", "military"))
            elif intent["type"] == "naval":
                narrative = strategic_naval_engagement(state)
            elif intent["type"] == "loitering":
                narrative = strategic_loitering_strike(state)
            elif intent["type"] == "cyber":
                narrative = strategic_cyber_attack(state)
            elif intent["type"] == "space":
                narrative = strategic_space_deployment(state)
            elif intent["type"] == "psyops":
                narrative = strategic_psyops(state)
            elif intent["type"] == "resupply":
                narrative = strategic_resupply(state)
            elif intent["type"] == "produce":
                narrative = strategic_produce_missiles(state)
            elif intent["type"] == "sanction":
                narrative = strategic_sanctions(state)
            elif intent["type"] == "nuke":
                narrative = strategic_nuclear_strike(state)
            elif intent["type"] == "intel":
                gain = random.randint(10, 30)
                state.strategic.intelligence_level = min(100, state.strategic.intelligence_level + gain)
                narrative = f"Intelligence gathered. Intel level increased to {state.strategic.intelligence_level}."
                narrative += "\nTargetable enemy assets:"
                narrative += f"\n  • Warships: {state.enemy_strategic.warships}"
                narrative += f"\n  • Air Defense: {state.enemy_strategic.air_defense}"
                narrative += f"\n  • Missile Silos: {state.enemy_strategic.missile_silos}"
                narrative += f"\n  • Infrastructure: {state.enemy_strategic.infrastructure}"
                narrative += f"\n  • Command Centers: {state.enemy_strategic.command_centers}"
                narrative += f"\n  • Nuclear Sites: {state.enemy_strategic.nuclear_sites}"
            elif intent["type"] == "ew":
                # Electronic warfare with cooldown (keep existing logic)
                if not hasattr(state, 'last_ew_turn'):
                    state.last_ew_turn = 0
                current_turn = state.turn
                if current_turn - state.last_ew_turn < 2:
                    narrative = "Electronic warfare systems recharging. Wait a turn."
                else:
                    if state.strategic.electronic_warfare > 0:
                        effect = random.randint(10, 30)
                        state.enemy_strategic.intelligence_level = max(0, state.enemy_strategic.intelligence_level - effect)
                        narrative = f"Electronic attack degrades enemy sensors. Their intel reduced by {effect}."
                        state.last_ew_turn = current_turn
                    else:
                        narrative = "No EW assets available."
            elif intent["type"] == "targets":
                narrative = "Targetable enemy assets:"
                narrative += f"\n  • Warships: {state.enemy_strategic.warships}"
                narrative += f"\n  • Air Defense: {state.enemy_strategic.air_defense}"
                narrative += f"\n  • Missile Silos: {state.enemy_strategic.missile_silos}"
                narrative += f"\n  • Infrastructure: {state.enemy_strategic.infrastructure}"
                narrative += f"\n  • Command Centers: {state.enemy_strategic.command_centers}"
                narrative += f"\n  • Nuclear Sites: {state.enemy_strategic.nuclear_sites}"
            else:
                narrative = "Command not recognized in strategic mode."

            print_c(f"\n{narrative}", "GREEN")
            # Apply civilian tension
            civ_tension = apply_civilian_tension(state)
            if civ_tension:
                print_c(civ_tension, "RED")
            if not state.game_over:
                print_c("\n--- ENEMY STRATEGIC RESPONSE ---", "RED")
                enemy_narrative = process_strategic_enemy_turn(state)
                print_c(f"{enemy_narrative}", "RED")
                check_strategic_victory(state)
            # War economy update
            state.production_points += max(0, 5 + (state.war_economy - 50) // 10 - state.sanctions // 10)
            if state.production_points >= 20:
                state.strategic.ballistic_missiles += 1
                state.production_points -= 20
            if (state.strategic.nukes > 0 or state.enemy_strategic.nukes > 0) and random.random() < 0.3:
                state.global_tension = min(100, state.global_tension + random.randint(0, 2))
            state.history.append(f"Turn {state.turn}: {narrative[:80]}")
            state.turn += 1

        print_c("\n" + "-"*40, "DIM")
        time.sleep(0.5)

    # After game over
    if state.game_mode == "tactical" and state.winner == "attacker":
        save_campaign(state)
    show_aar(state)
    print_c("\nThank you for playing CONTACT FRONT.", "GREEN")

if __name__ == "__main__":
    main()
