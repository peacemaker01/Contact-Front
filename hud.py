import os
import re
import random
import shutil
from tactical.map_engine import line_of_sight_any
from terminal_utils import TerminalInfo

ANSI = {
    "USA_BLUE": "\033[94m", "RUS_RED": "\033[91m", "CHN_YELLOW": "\033[93m", "IRN_GREEN": "\033[92m",
    "HEALTH_HIGH": "\033[92m", "HEALTH_MED": "\033[93m", "HEALTH_LOW": "\033[91m", "DEAD": "\033[90m",
    "BORDER": "\033[37m", "TITLE": "\033[1;37m", "INTEL": "\033[96m", "WARNING": "\033[1;91m",
    "NUCLEAR": "\033[1;95m", "RESET": "\033[0m"
}

FACTION_COLOR_MAP = {
    "USA": ANSI["USA_BLUE"], "RUSSIA": ANSI["RUS_RED"],
    "CHINA": ANSI["CHN_YELLOW"], "IRAN": ANSI["IRN_GREEN"],
}

_ANSI_RE = re.compile(r'\033\[[0-9;]*m')

def visible_len(s: str) -> int:
    return len(_ANSI_RE.sub('', s))

def pad_to(s: str, width: int) -> str:
    return s + " " * max(0, width - visible_len(s))

def clear_screen():
    os.system("clear")

def render_progress_bar(value, max_value, width=6):
    if max_value <= 0:
        return " " * width + "  N/A"
    filled = int((value / max_value) * width)
    bar = "█" * filled + "░" * (width - filled)
    pct = int((value / max_value) * 100)
    color = ANSI["HEALTH_HIGH"] if pct > 70 else ANSI["HEALTH_MED"] if pct > 30 else ANSI["HEALTH_LOW"]
    return f"{color}{bar}{ANSI['RESET']} {pct}%"

def _build_enemy_intel(game_state):
    try:
        visible = [u for u in game_state.enemy_units if not u.destroyed and line_of_sight_any(game_state, u.x, u.y)]
        suspected = [u for u in game_state.enemy_units if not u.destroyed and not line_of_sight_any(game_state, u.x, u.y) and game_state.turn - u.last_seen_by_player <= 2]
        hidden = [u for u in game_state.enemy_units if not u.destroyed and not line_of_sight_any(game_state, u.x, u.y) and game_state.turn - u.last_seen_by_player > 2]
    except Exception:
        visible, suspected, hidden = [], [], []
    lines = [f" ⚠️  CONFIRMED: {len(visible)}"]
    for u in visible[:3]:
        lines.append(f"   {u.type_code}{u.id} @ ({u.x},{u.y})")
    if suspected:
        lines.append(f" ❓ SUSPECTED: {len(suspected)}")
    if hidden:
        lines.append(f" ◉  HIDDEN: {len(hidden)}")
    return lines

def _build_strategic_asset_lines(game_state):
    faction_capitals = {"USA": "Washington D.C.", "RUSSIA": "Moscow", "CHINA": "Beijing", "IRAN": "Tehran"}
    capital = faction_capitals.get(game_state.player_faction, "Capital")
    lines = []
    for asset, count in game_state.player_assets.items():
        if count > 0:
            label = asset.replace("_", " ").title()
            lines.append(f"[{label[:18]}]")
            lines.append(f"  {capital}: {count}")
    return lines or [" No assets remaining."]

def render_full_tactical_hud(game_state, map_render):
    clear_screen()
    term = TerminalInfo()
    cols = term.width
    faction_color = FACTION_COLOR_MAP.get(game_state.player_faction, ANSI["BORDER"])
    time_str = "☀️ DAY"
    if game_state.is_night:
        time_str = "🌙 NIGHT"
    if game_state.is_raining:
        time_str += " ☔"
    print("╔" + "═" * (cols - 2) + "╗")
    header = f"║ CONTACT FRONT │ TACTICAL │ {faction_color}{game_state.player_faction}{ANSI['RESET']} — {game_state.mode.upper()} │ TURN: {game_state.turn}/{game_state.max_turns} │ {time_str} "
    print(pad_to(header, cols - 1) + "║")
    print("╠" + "═" * (cols - 2) + "╣")
    obj_desc = game_state.objectives[0].get('desc', 'Secure objective') if game_state.objectives else 'Capture area'
    if isinstance(obj_desc, list):
        obj_desc = "; ".join(obj_desc[:2])
    obj_line = f"║ OBJECTIVE: {obj_desc[:50]} (★) │ {game_state.max_turns - game_state.turn} turns remaining "
    print(pad_to(obj_line, cols - 1) + "║")
    print("╠" + "═" * (cols - 2) + "╣")
    map_lines = map_render.split('\n')
    for line in map_lines:
        inner_width = cols - 4
        print("║  " + pad_to(line[:inner_width], inner_width) + "  ║")
    print("╠" + "═" * (cols - 2) + "╣")

    friendly = [u for u in game_state.friendly_units if not u.destroyed]
    left_w, center_w, right_w = term.get_panel_widths()
    unit_lines = []
    for u in friendly[:4]:
        unit_lines.append(f"[{u.id}] {u.type_code}{u.id} ({u.x},{u.y}) | {u.name[:18]}")
        unit_lines.append(f"    STR:{render_progress_bar(u.strength, 100, 6)}")
        unit_lines.append(f"    MOR:{render_progress_bar(u.morale, 100, 6)}")
        # Combined ammo (HE + AP) for simplicity
        total_ammo = u.ammo_he + u.ammo_ap
        total_max = u.max_ammo_he + u.max_ammo_ap
        if total_max > 0:
            unit_lines.append(f"    AMMO:{render_progress_bar(total_ammo, total_max, 5)}")
        else:
            unit_lines.append("    AMMO: N/A")
        unit_lines.append(f"    MP:{render_progress_bar(u.movement_points, u.movement, 4)}")
        unit_lines.append("")
    while len(unit_lines) < 15:
        unit_lines.append("")

    res_lines = [
        f" Artillery: {game_state.artillery_fires_remaining} fires",
        f" CAS: {game_state.cas_available} strikes",
        f" Smoke: {game_state.smoke_grenades} grenades",
        "",
        " CASUALTIES",
        f" KIA: {game_state.friendly_kia} / WIA: {game_state.friendly_wia}",
        f" Vehicles lost: {game_state.vehicles_lost}",
        "", "", ""
    ]
    while len(res_lines) < 15:
        res_lines.append("")

    enemy_lines = _build_enemy_intel(game_state)
    while len(enemy_lines) < 15:
        enemy_lines.append("")

    for i in range(15):
        left = unit_lines[i][:left_w] if i < len(unit_lines) else ""
        center = res_lines[i][:center_w] if i < len(res_lines) else ""
        right = enemy_lines[i][:right_w] if i < len(enemy_lines) else ""
        print(f"║ {left:<{left_w}} │ {center:<{center_w}} │ {right:<{right_w}} ║")

    print("╠" + "═" * (cols - 2) + "╣")
    prompt_width = term.get_command_prompt_width()
    print(f"║ > Your command: {'_' * (prompt_width - 2)} ║")
    print("╠" + "═" * (cols - 2) + "╣")
    narrative = game_state.narrative_log[-1] if game_state.narrative_log else "Awaiting orders."
    inner_w = cols - 4
    for i in range(0, len(narrative), inner_w):
        chunk = narrative[i:i+inner_w]
        print(f"║ {chunk:<{inner_w}} ║")
    print("╚" + "═" * (cols - 2) + "╝")

def render_strategic_hud(game_state, map_render):
    clear_screen()
    term = TerminalInfo()
    cols = term.width
    faction_color = FACTION_COLOR_MAP.get(game_state.player_faction, ANSI["BORDER"])
    nuclear_posture = "N/A" if game_state.player_faction == "IRAN" else ("LOCKED" if not game_state.nuclear_authorized else "AUTHORIZED")
    print("╔" + "═" * (cols - 2) + "╗")
    header = f"║ CONTACT FRONT │ STRATEGIC │ {faction_color}{game_state.player_faction}{ANSI['RESET']} │ TURN: {game_state.turn}/{game_state.max_turns} │ 🌙 NIGHT "
    print(pad_to(header, cols - 1) + "║")
    esc_line = f"║ ESCALATION: {'█' * game_state.escalation_level}{'░' * (6-game_state.escalation_level)} LVL {game_state.escalation_level}/6  │  ALERT: DEFCON {4-game_state.escalation_level}        │  NUCLEAR POSTURE: {nuclear_posture} "
    print(pad_to(esc_line, cols - 1) + "║")
    print("╠" + "═" * (cols - 2) + "╣")
    map_lines = map_render.split('\n')
    for line in map_lines:
        inner_width = cols - 4
        print("║  " + pad_to(line[:inner_width], inner_width) + "  ║")
    print("╠" + "═" * (cols - 2) + "╣")

    asset_lines = _build_strategic_asset_lines(game_state)
    while len(asset_lines) < 10:
        asset_lines.append("")

    enemy_lines = []
    for asset_id, last_seen in game_state.enemy_asset_last_seen.items():
        age = game_state.turn - last_seen
        marker = f"{{ {asset_id.upper()} }}" if age <= 1 else f"( {asset_id.upper()} )"
        enemy_lines.append(f"{marker} (T{age})")
    while len(enemy_lines) < 6:
        enemy_lines.append("")

    intel_lines = ["T0 = this turn", "T1 = last turn", "T2+ = decayed / removed", "", "Next recon: T+2", "(satellite overflight)"]
    while len(intel_lines) < 10:
        intel_lines.append("")

    left_w, center_w, right_w = term.get_panel_widths()
    max_lines = max(len(asset_lines), len(enemy_lines), len(intel_lines))
    for i in range(max_lines):
        left = asset_lines[i][:left_w] if i < len(asset_lines) else ""
        center = enemy_lines[i][:center_w] if i < len(enemy_lines) else ""
        right = intel_lines[i][:right_w] if i < len(intel_lines) else ""
        print(f"║ {left:<{left_w}} │ {center:<{center_w}} │ {right:<{right_w}} ║")

    print("╠" + "═" * (cols - 2) + "╣")
    ew_effects = ", ".join([e['type'] for e in game_state.active_ew_effects]) if game_state.active_ew_effects else "None"
    space_status = f"GPS: {'OK' if game_state.satellites_operational.get('gps',True) else 'DEGRADED'}, No IRN recon sat"
    ew_line = f"║ ACTIVE EW: {ew_effects}  │ SPACE: {space_status} "
    print(pad_to(ew_line, cols - 1) + "║")
    print("╠" + "═" * (cols - 2) + "╣")
    if game_state.strike_log:
        last = game_state.strike_log[-1].get('narrative', '')
        max_len = cols - 4
        for i in range(0, len(last), max_len):
            print(f"║ {last[i:i+max_len]:<{max_len}} ║")
    else:
        print(f"║ No strikes yet.                                                                 ║")
    print("╠" + "═" * (cols - 2) + "╣")
    prompt_width = term.get_command_prompt_width()
    print(f"║ > Your command: {'_' * (prompt_width - 2)} ║")
    print("╚" + "═" * (cols - 2) + "╝")
