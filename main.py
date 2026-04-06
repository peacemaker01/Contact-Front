# main.py - Contact Front (Complete with 10 new features)
import os
import time
import random
from config import COLORS, ZONE_MAP, COVER_VALUES, DIFFICULTY
from models import GameState, StrategicAsset
from scenarios import generate_mission, generate_strategic_scenario
from save_manager import save_game, load_game, list_saves
from llm_client import parse_player_intent, generate_narrative, validate_intent, generate_llm_intel
from rules import (
    resolve_mortar, resolve_fpv_drone, resolve_infantry_fire,
    resolve_advance, resolve_surrender, resolve_recon,
    check_victory_tactical, apply_command_delay,
    recover_suppression, apply_leader_boost, tactical_resupply,
    change_weather, civilian_casualty_check, decay_detection,
    refresh_detection, apply_civilian_tension,
    strategic_missile_strike, strategic_naval_engagement,
    strategic_loitering_strike, strategic_cyber_attack,
    strategic_space_deployment, strategic_psyops,
    strategic_resupply, strategic_produce_missiles,
    strategic_sanctions, strategic_nuclear_strike,
    set_overwatch, process_overwatch_fire, medic_heal,
    capture_prisoner, interrogate_prisoner, check_supply_line,
    advance_time
)
from enemy_ai import process_enemy_turn
from llm_strategic import process_strategic_enemy_turn
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
    # Placeholder: original simple map, can be replaced later
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
            if u.side == state.player_side and u.zone == zone and u.is_alive():
                icon = "⚔️" if u.type == "lmg" else "🔫"
                if u.is_leader:
                    icon = "⭐" + icon
                friend_icons.append(icon)
        enemy_icons = []
        if state.recon_active > 0:
            for u in state.units:
                if u.side != state.player_side and u.zone == zone and u.is_alive():
                    icon = "⚡" if u.type == "lmg" else "🔫"
                    enemy_icons.append(icon)
        else:
            if any(u.side != state.player_side and u.zone == zone for u in state.units):
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
    player_losses = len([u for u in state.units if u.side == state.player_side and u.status == "kia"])
    enemy_losses = len([u for u in state.units if u.side != state.player_side and u.status == "kia"])
    total_shots = sum(30 - u.ammo for u in state.units if u.side == state.player_side and u.type == "rifleman")
    print_c(f" Friendly KIA: {player_losses}", "RED")
    print_c(f" Enemy KIA:    {enemy_losses}", "GREEN")
    print_c(f" Rounds fired: {total_shots}", "CYAN")
    print_c(f" Turns played: {state.turn-1}", "CYAN")
    print_c(f" Final objective status: {'SECURED' if state.winner == state.player_side else 'LOST'}", "YELLOW")
    lessons = [
        "Use recon before advancing.",
        "Mortars are most effective against suppressed targets.",
        "FPV drones can eliminate high-value targets.",
        "Suppression pins enemies, making them easy targets.",
        "Morale breaks faster than bodies.",
        "Overwatch can stop enemy advances.",
        "Medics preserve experienced troops.",
        "Prisoners provide valuable intelligence."
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
    print_c('  "advise" - Get AI tactical recommendation', "GREEN")
    print_c('  "intel" - Detailed enemy intelligence report', "GREEN")
    print_c('  "overwatch <zone>" - Set unit to cover a zone', "GREEN")
    print_c('  "heal <medic> <target>" - Heal a wounded soldier', "GREEN")
    print_c('  "interrogate" - Question a prisoner for intel', "GREEN")
    print_c('  "supply" - Check supply line status', "GREEN")
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
        print_c(f" TURN {state.turn} | OBJ: {state.objective_zone.upper()} | {state.weather.upper()} | {state.time_of_day.upper()}", "BOLD")
        recon_label = f"{state.recon_active} turn{'s' if state.recon_active != 1 else ''} remaining" if state.recon_active > 0 else "inactive"
        print_c(f" MORTARS: {state.mortar_rounds} | FPV: {state.fpv_drones} | RECON: {recon_label}", "CYAN")
        print_c(f" SUPPLY: {state.supply_points} | IED THREAT: {state.ied_threat}%", "CYAN")
        print_c(f" SUPPLY LINE: {'ACTIVE' if state.supply_line_active else 'CUT'}", "CYAN")
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
            if u.side == state.player_side and u.is_alive():
                sc = "GREEN" if u.status == "active" else "WARNING"
                leader_mark = " [L]" if u.is_leader else ""
                medic_mark = " [M]" if u.is_medic else ""
                overwatch_mark = " [OW]" if u.overwatch else ""
                print_c(f"   [{u.zone.upper():<7}] {u.name}{leader_mark}{medic_mark}{overwatch_mark:<15} | {u.status.upper()} | AMMO:{u.ammo}", sc)
                print_c(f"      Morale: {morale_bar(u.morale)}", "YELLOW")
        print_c("\n ENEMY:", "RED")
        if state.recon_active > 0:
            for u in state.units:
                if u.side != state.player_side and u.is_alive():
                    sc = "FAIL" if u.status == "active" else "WARNING"
                    note = " (PANICKING)" if u.morale < 30 else " (SHAKEN)" if u.morale < 50 else ""
                    print_c(f"   [{u.zone.upper():<7}] {u.name:<15} | {u.status.upper()} | MORALE:{u.morale}{note}", sc)
        else:
            print_c("   No direct visual – recon drone recommended", "DIM")
        if state.prisoners:
            print_c(" PRISONERS:", "CYAN")
            for p in state.prisoners:
                print_c(f"   {p.name} (captured)", "CYAN")
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
    enemies = [u for u in state.units if u.side != state.player_side and u.is_alive()]
    if not enemies:
        state.enemy_posture = "none"
        state.enemy_ammo_estimate = "none"
        state.enemy_morale_trend = "none"
        return
    if any(d.morale > 70 and d.status != "suppressed" for d in enemies):
        state.enemy_posture = "aggressive"
    elif any(d.morale < 30 for d in enemies):
        state.enemy_posture = "routed"
    else:
        state.enemy_posture = "defensive"
    avg_ammo = sum(d.ammo for d in enemies) / len(enemies)
    if avg_ammo > 20:
        state.enemy_ammo_estimate = "plentiful"
    elif avg_ammo > 10:
        state.enemy_ammo_estimate = "moderate"
    else:
        state.enemy_ammo_estimate = "low"
    avg_morale = sum(d.morale for d in enemies) / len(enemies)
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
    print_c("[4] RANDOM MISSION (Procedurally generated)", "CYAN")
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
        state.player_side = "attacker"
        return state

    else:  # tactical mode (including random)
        if mode_choice == "4":
            mission_type = "random"
            print_c("\nRANDOM MISSION - Procedurally generated", "CYAN")
        else:
            print_c("\n[1] OPERATION: BROKEN ANVIL (Farmhouse assault)", "CYAN")
            print_c("[2] OPERATION: SILENT SWEEP (Night raid)", "CYAN")
            mission_choice = input("Select mission > ").strip()
            mission_type = "night_raid" if mission_choice == "2" else "broken_anvil"

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
        state = generate_mission(mission_type, diff, provider, api_key, player_faction, enemy_faction, player_side)
        return state

def find_unit_by_name(state, name):
    """Helper to find a unit by name (partial match)."""
    if not name:
        return None
    for u in state.units:
        if name.lower() in u.name.lower():
            return u
    return None

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

        valid, err = validate_intent(intent)
        if not valid:
            print_c(f"Invalid command: {err}", "WARNING")
            continue

        if intent["type"] in ["unknown", "compound"]:
            reason = intent.get("reason", "Command not recognized. Try 'help'.")
            print_c(reason, "WARNING")
            continue

        if intent["type"] == "info":
            print_c("\n(Current battlefield information is shown in the HUD above.)", "CYAN")
            continue

        if intent["type"] == "advise":
            if not api_key:
                print_c("Advice requires an LLM API key.", "WARNING")
            else:
                print_c("Consulting AI advisor...", "CYAN")
                prompt = f"""You are a tactical advisor. Based on the following situation, give ONE short sentence of advice (max 20 words).

Objective zone: {state.objective_zone}
Weather: {state.weather}
Time: {state.time_of_day}
Your units: {len([u for u in state.units if u.side == state.player_side and u.is_alive()])} alive
Enemy units: {len([u for u in state.units if u.side != state.player_side and u.is_alive()])} alive
Recon active: {state.recon_active > 0}
Mortars left: {state.mortar_rounds}
FPV drones left: {state.fpv_drones}
Supply line: {'active' if state.supply_line_active else 'cut'}
"""
                try:
                    import requests
                    response = requests.post(
                        "https://openrouter.ai/api/v1/chat/completions",
                        headers={"Authorization": f"Bearer {api_key}"},
                        json={
                            "model": "openai/gpt-4o-mini",
                            "messages": [{"role": "user", "content": prompt}],
                            "max_tokens": 40,
                            "temperature": 0.7
                        },
                        timeout=5
                    )
                    if response.status_code == 200:
                        advice = response.json()["choices"][0]["message"]["content"].strip()
                        print_c(f"Advisor: \"{advice}\"", "CYAN")
                    else:
                        print_c("Advisor unavailable.", "WARNING")
                except Exception as e:
                    print_c(f"Advisor error: {e}", "WARNING")
            continue

        if intent["type"] == "intel":
            print_c("\n--- INTELLIGENCE REPORT ---", "CYAN")
            llm_report = generate_llm_intel(state, api_key, provider)
            if llm_report:
                print_c(llm_report, "YELLOW")
            else:
                report = generate_tactical_intel(state)
                print_c(report, "YELLOW")
            continue

        if state.game_mode == "tactical":
            # Check supply line at start of turn
            supply_msg = check_supply_line(state)
            if supply_msg:
                print_c(supply_msg, "YELLOW")

            # IED check (only if supply line active)
            if random.random() < state.ied_threat / 100:
                print_c("\n⚠️  IED EXPLOSION! ⚠️", "RED")
                friendlies = [u for u in state.units if u.side == state.player_side and u.is_alive()]
                if friendlies:
                    target = random.choice(friendlies)
                    target.hits += 10
                    target.morale -= 30
                    if target.hits >= 15:
                        target.status = "kia"
                        print_c(f"IED kills {target.name}!", "RED")
                    else:
                        print_c(f"IED wounds {target.name}!", "RED")
                    from rules import morale_cascade
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
                narrative = resolve_infantry_fire(state, state.player_side, intent.get("zone","medium"))
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
            elif intent["type"] == "overwatch":
                zone = intent.get("zone", "medium")
                # For simplicity, we set the first available unit to overwatch
                # In a real implementation, you'd specify which unit. We'll assume the player means "the squad"
                # For now, we set all active units? Better to set one. We'll use the first rifleman.
                unit = next((u for u in state.units if u.side == state.player_side and u.is_combat_effective()), None)
                if unit:
                    narrative = set_overwatch(state, unit, zone)
                else:
                    narrative = "No combat-effective unit to set on overwatch."
            elif intent["type"] == "heal":
                # Parse medic and target from command – we'll use simple assumption: first medic heals first wounded
                medic = next((u for u in state.units if u.is_medic and u.side == state.player_side and u.is_combat_effective()), None)
                target = next((u for u in state.units if u.side == state.player_side and u.hits > 0 and u.is_alive()), None)
                if medic and target:
                    narrative = medic_heal(state, medic, target)
                else:
                    narrative = "No medic available or no wounded soldier."
            elif intent["type"] == "interrogate":
                narrative = interrogate_prisoner(state)
            elif intent["type"] == "supply":
                narrative = f"Supply line: {'ACTIVE' if state.supply_line_active else 'CUT'}. Supply points: {state.supply_points}"
            else:
                narrative = "Command not recognized in tactical mode. Try 'help'."

            print_c(f"\n{narrative}", "GREEN")

            # Capture prisoners from routed enemies after player action
            for enemy in [u for u in state.units if u.side != state.player_side and u.status == "routed" and u.is_alive()]:
                capture_msg = capture_prisoner(state, enemy)
                if capture_msg:
                    print_c(capture_msg, "CYAN")

            # Civilian casualty check
            if intent["type"] in ["mortar", "attack"]:
                civ_msg = civilian_casualty_check(state, intent.get("zone","medium"))
                if civ_msg:
                    print_c(civ_msg, "RED")

            check_victory_tactical(state)

            enemy_narrative = ""
            if not state.game_over:
                print_c("\n--- ENEMY TURN ---", "RED")
                # Process overwatch fire before enemy actions
                ow_fire = process_overwatch_fire(state)
                if ow_fire:
                    print_c(ow_fire, "CYAN")
                enemy_narrative = process_enemy_turn(state, provider, api_key)
                print_c(f"{enemy_narrative}", "RED")
                check_victory_tactical(state)

            # End-of-turn updates
            rec_msg = recover_suppression(state)
            if rec_msg:
                print_c(rec_msg, "GREEN")
            lead_msg = apply_leader_boost(state)
            if lead_msg:
                print_c(lead_msg, "CYAN")
            weather_msg = change_weather(state)
            if weather_msg:
                print_c(weather_msg, "YELLOW")
            time_msg = advance_time(state)
            if time_msg:
                print_c(time_msg, "YELLOW")
            decay_detection(state)
            civ_tension = apply_civilian_tension(state)
            if civ_tension:
                print_c(civ_tension, "RED")
            state.supply_points = min(200, state.supply_points + random.randint(0, 2))
            generate_tactical_intel(state)

            combined = f"{narrative} {enemy_narrative}"
            state.last_narrative = generate_narrative(combined, state, provider, api_key)
            state.history.append(f"Turn {state.turn}: {narrative[:80]}")
            state.turn += 1
            if state.recon_active > 0:
                state.recon_active -= 1
            if state.ied_threat > 0:
                state.ied_threat = max(0, state.ied_threat - random.randint(0, 3))

        else:  # strategic mode
            if intent["type"] == "nuke":
                print_c("⚠️  NUCLEAR STRIKE AUTHORIZATION REQUIRED ⚠️", "RED")
                confirm = input("Type 'CONFIRM' to launch nuclear weapon: ").strip()
                if confirm != "CONFIRM":
                    print_c("Nuclear strike cancelled.", "GREEN")
                    continue

            narrative = ""
            if intent["type"] == "missile":
                narrative = strategic_missile_strike(state, intent.get("missile_type","ballistic"), intent.get("target_asset","military"))
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
                narrative = f"Intelligence gathered. Intel level increased to {state.strategic.intelligence_level}.\nTargetable enemy assets:\n"
                narrative += f"  • Warships: {state.enemy_strategic.warships}\n"
                narrative += f"  • Air Defense: {state.enemy_strategic.air_defense}\n"
                narrative += f"  • Missile Silos: {state.enemy_strategic.missile_silos}\n"
                narrative += f"  • Infrastructure: {state.enemy_strategic.infrastructure}\n"
                narrative += f"  • Command Centers: {state.enemy_strategic.command_centers}\n"
                narrative += f"  • Nuclear Sites: {state.enemy_strategic.nuclear_sites}"
            elif intent["type"] == "ew":
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
                narrative = "Targetable enemy assets:\n"
                narrative += f"  • Warships: {state.enemy_strategic.warships}\n"
                narrative += f"  • Air Defense: {state.enemy_strategic.air_defense}\n"
                narrative += f"  • Missile Silos: {state.enemy_strategic.missile_silos}\n"
                narrative += f"  • Infrastructure: {state.enemy_strategic.infrastructure}\n"
                narrative += f"  • Command Centers: {state.enemy_strategic.command_centers}\n"
                narrative += f"  • Nuclear Sites: {state.enemy_strategic.nuclear_sites}"
            else:
                narrative = "Command not recognized in strategic mode. Try 'help'."

            print_c(f"\n{narrative}", "GREEN")

            civ_tension = apply_civilian_tension(state)
            if civ_tension:
                print_c(civ_tension, "RED")

            if not state.game_over:
                print_c("\n--- ENEMY STRATEGIC RESPONSE ---", "RED")
                enemy_narrative = process_strategic_enemy_turn(state, provider, api_key)
                print_c(f"{enemy_narrative}", "RED")
                check_strategic_victory(state)

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

    if state.game_mode == "tactical" and state.winner == state.player_side:
        save_campaign(state)
    show_aar(state)
    print_c("\nThank you for playing CONTACT FRONT.", "GREEN")

if __name__ == "__main__":
    main()
