import os
import random
import shutil

ANSI = {
    "USA_BLUE": "\033[94m", "RUS_RED": "\033[91m", "CHN_YELLOW": "\033[93m", "IRN_GREEN": "\033[92m",
    "HEALTH_HIGH": "\033[92m", "HEALTH_MED": "\033[93m", "HEALTH_LOW": "\033[91m", "DEAD": "\033[90m",
    "BORDER": "\033[37m", "TITLE": "\033[1;37m", "INTEL": "\033[96m", "WARNING": "\033[1;91m",
    "NUCLEAR": "\033[1;95m", "RESET": "\033[0m"
}

def clear_screen():
    os.system("clear")

def render_progress_bar(value, max_value, width=6):
    filled = int((value / max_value) * width)
    bar = "█" * filled + "░" * (width - filled)
    pct = int((value / max_value) * 100)
    color = ANSI["HEALTH_HIGH"] if pct > 70 else ANSI["HEALTH_MED"] if pct > 30 else ANSI["HEALTH_LOW"]
    return f"{color}{bar}{ANSI['RESET']} {pct}%"

def render_full_tactical_hud(game_state, map_render):
    clear_screen()
    cols, _ = shutil.get_terminal_size((80, 24))
    faction_color = ANSI.get(f"{game_state.player_faction}_BLUE", ANSI["BORDER"])
    print("╔" + "═" * (cols - 2) + "╗")
    header = f"║ CONTACT FRONT │ TACTICAL │ {faction_color}{game_state.player_faction}{ANSI['RESET']} — {game_state.mode.upper()} │ TURN: {game_state.turn}/{game_state.max_turns} │ ☀️ DAY "
    print(header + " " * (cols - len(header) - 1) + "║")
    print("╠" + "═" * (cols - 2) + "╣")
    obj_desc = game_state.objectives[0].get('desc', 'Secure objective') if game_state.objectives else 'Capture area'
    obj_line = f"║ OBJECTIVE: {obj_desc} (★) │ {game_state.max_turns - game_state.turn} turns remaining "
    print(obj_line + " " * (cols - len(obj_line) - 1) + "║")
    print("╠" + "═" * (cols - 2) + "╣")
    map_lines = map_render.split('\n')
    for line in map_lines:
        max_line_len = cols - 4
        if len(line) > max_line_len:
            line = line[:max_line_len]
        print("║  " + line + " " * (max_line_len - len(line)) + "  ║")
    print("╠" + "═" * (cols - 2) + "╣")

    # Unit panel (left)
    friendly = [u for u in game_state.friendly_units if not u.destroyed]
    unit_lines = []
    for u in friendly[:4]:
        unit_lines.append(f"[{u.id}] {u.type_code}{u.id} | {u.name[:20]}")
        unit_lines.append(f"    STR:{render_progress_bar(u.strength,100,6)}")
        unit_lines.append(f"    MOR:{render_progress_bar(u.morale,100,6)}")
        unit_lines.append(f"    AMMO:{render_progress_bar(u.ammo,u.max_ammo,5)}")
        unit_lines.append("")
    while len(unit_lines) < 15:
        unit_lines.append("")

    # Resources panel (center) – real casualties
    res_lines = [
        f" Artillery: {game_state.artillery_fires_remaining} fires",
        f" CAS: {game_state.cas_available}",
        f" Smoke: {game_state.smoke_grenades} grenades",
        "",
        " CASUALTIES",
        f" KIA: {game_state.friendly_kia} / WIA: {game_state.friendly_wia}",
        f" Vehicles lost: {game_state.vehicles_lost}",
        "",
        "",
        ""
    ]
    while len(res_lines) < 15:
        res_lines.append("")

    # Enemy intel panel (right)
    visible_enemies = len([e for e in game_state.enemy_units if not e.destroyed])
    enemy_lines = [
        f" ⚠️ {visible_enemies} squad{'s' if visible_enemies != 1 else ''} est.",
        " ❓ No armor seen",
        f" 📍 R3 at grid {random.randint(10,15)}-{random.randint(2,8)}",
        f" ?? at grid {random.randint(15,20)}-{random.randint(6,10)}",
        "",
        "",
        "",
        "",
        "",
        ""
    ]
    while len(enemy_lines) < 15:
        enemy_lines.append("")

    # Print three columns
    for i in range(15):
        left = unit_lines[i][:28] if i < len(unit_lines) else ""
        center = res_lines[i][:26] if i < len(res_lines) else ""
        right = enemy_lines[i][:24] if i < len(enemy_lines) else ""
        print(f"║ {left:<28} │ {center:<26} │ {right:<24} ║")

    print("╠" + "═" * (cols - 2) + "╣")
    print(f"║ > Your command: {'_'*40} ║")
    print("╠" + "═" * (cols - 2) + "╣")
    narrative = game_state.narrative_log[-1] if game_state.narrative_log else "Awaiting orders."
    max_narrative_len = cols - 4
    for i in range(0, len(narrative), max_narrative_len):
        chunk = narrative[i:i+max_narrative_len]
        print(f"║ {chunk:<{max_narrative_len}} ║")
    print("╚" + "═" * (cols - 2) + "╝")

def render_strategic_hud(game_state, map_render):
    clear_screen()
    cols, _ = shutil.get_terminal_size((80, 24))
    faction_color = ANSI.get(f"{game_state.player_faction}_BLUE", ANSI["BORDER"])
    nuclear_posture = "N/A" if game_state.player_faction == "IRAN" else ("LOCKED" if not game_state.nuclear_authorized else "AUTHORIZED")
    print("╔" + "═" * (cols - 2) + "╗")
    header = f"║ CONTACT FRONT │ STRATEGIC │ {faction_color}{game_state.player_faction}{ANSI['RESET']} │ TURN: {game_state.turn}/{game_state.max_turns} │ 🌙 NIGHT "
    print(header + " " * (cols - len(header) - 1) + "║")
    esc_line = f"║ ESCALATION: {'█' * game_state.escalation_level}{'░' * (6-game_state.escalation_level)} LVL {game_state.escalation_level}/6  │  ALERT: DEFCON {4-game_state.escalation_level}        │  NUCLEAR POSTURE: {nuclear_posture} "
    print(esc_line + " " * (cols - len(esc_line) - 1) + "║")
    print("╠" + "═" * (cols - 2) + "╣")
    map_lines = map_render.split('\n')
    for line in map_lines:
        max_line_len = cols - 4
        if len(line) > max_line_len:
            line = line[:max_line_len]
        print("║  " + line + " " * (max_line_len - len(line)) + "  ║")
    print("╠" + "═" * (cols - 2) + "╣")

    # Asset panel (generic – will be faction‑aware in full implementation)
    asset_lines = [
        "[M] Missile battery", "  Tehran (2 units)", "[A] Air defense", "  Tehran (1 unit)",
        "[D] Drone swarm", "  Qom (hidden)", "", "", "", ""
    ]
    enemy_lines = []
    if "carrier" in game_state.enemy_asset_last_seen:
        age = game_state.turn - game_state.enemy_asset_last_seen["carrier"]
        enemy_lines.append(f"{{C}} Carrier (T{age})")
    else:
        enemy_lines.append("{C} Carrier (unknown)")
    if "patriot" in game_state.enemy_asset_last_seen:
        age = game_state.turn - game_state.enemy_asset_last_seen["patriot"]
        enemy_lines.append(f"{{P}} Patriot (T{age})")
    else:
        enemy_lines.append("{P} Patriot (unknown)")
    enemy_lines.append("(?) Possible AD (T2)")
    while len(enemy_lines) < 6:
        enemy_lines.append("")
    intel_lines = ["T0 = this turn", "T1 = last turn", "T2+ = decayed / removed", "", "Next recon: T+2", "(satellite overflight)"]
    while len(intel_lines) < 10:
        intel_lines.append("")

    max_lines = max(len(asset_lines), len(enemy_lines), len(intel_lines))
    for i in range(max_lines):
        left = asset_lines[i] if i < len(asset_lines) else ""
        center = enemy_lines[i] if i < len(enemy_lines) else ""
        right = intel_lines[i] if i < len(intel_lines) else ""
        print(f"║ {left:<30} │ {center:<25} │ {right:<25} ║")

    print("╠" + "═" * (cols - 2) + "╣")
    ew_effects = ", ".join([e['type'] for e in game_state.active_ew_effects]) if game_state.active_ew_effects else "None"
    space_status = f"GPS: {'OK' if game_state.satellites_operational.get('gps',True) else 'DEGRADED'}, No IRN recon sat"
    ew_line = f"║ ACTIVE EW: {ew_effects}  │ SPACE: {space_status} "
    print(ew_line + " " * (cols - len(ew_line) - 1) + "║")
    print("╠" + "═" * (cols - 2) + "╣")
    if game_state.strike_log:
        last = game_state.strike_log[-1].get('narrative', '')
        max_len = cols - 4
        for i in range(0, len(last), max_len):
            print(f"║ {last[i:i+max_len]:<{max_len}} ║")
    else:
        print(f"║ No strikes yet.                                                                 ║")
    print("╠" + "═" * (cols - 2) + "╣")
    print(f"║ > Your command: {'_'*40} ║")
    print("╚" + "═" * (cols - 2) + "╝")
