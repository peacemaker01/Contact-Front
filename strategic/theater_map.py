from factions import FACTIONS

def render_strategic_map(game_state):
    width, height = 60, 18
    grid = [[" " for _ in range(width)] for __ in range(height)]

    # ---- Static geography ----
    zones = {
        "BAGHDAD": (10, 3, "🏛BAGHDAD"),
        "TEHRAN": (24, 5, "🕌TEHRAN"),
        "RIYADH": (8, 8, "🏛RIYADH"),
        "DOHA": (20, 14, "🏛DOHA"),
        "MOSCOW": (40, 2, "🏙MOSCOW"),
        "BEIJING": (52, 3, "🏯BEIJING"),
        "WASHINGTON": (2, 1, "🏛WASHINGTON"),
    }
    for name, (x, y, icon) in zones.items():
        for i, ch in enumerate(icon):
            if x + i < width:
                grid[y][x + i] = ch

    # Water
    for y in range(4, 15):
        for x in range(15, 28):
            grid[y][x] = "≈"
    for y in range(5, 12):
        for x in range(38, 44):
            grid[y][x] = "≈"
    for y in range(1, 6):
        for x in range(1, 5):
            grid[y][x] = "≈"

    # Roads
    for x in range(12, 24):
        grid[8][x] = "─"
    for x in range(10, 22):
        grid[5][x] = "─"
    for y in range(6, 9):
        grid[y][12] = "│"
    for y in range(4, 8):
        grid[y][22] = "│"
    grid[5][12] = "┼"
    grid[8][12] = "┼"
    grid[5][22] = "┼"
    grid[8][22] = "┼"

    # ---- Friendly assets (dynamic) ----
    capitals = {
        "USA": (2, 1), "RUSSIA": (40, 2), "CHINA": (52, 3), "IRAN": (24, 5)
    }
    cap_x, cap_y = capitals.get(game_state.player_faction, (24, 5))

    # Missile batteries
    missile_assets = [k for k in game_state.player_assets.keys() if 'missile' in k or 'ballistic' in k]
    if missile_assets and game_state.player_assets.get(missile_assets[0], 0) > 0:
        grid[cap_y][cap_x-2] = "["
        grid[cap_y][cap_x-1] = "M"
        grid[cap_y][cap_x] = "]"
        if game_state.player_assets.get(missile_assets[0], 0) > 50:
            grid[cap_y][cap_x+1] = "["
            grid[cap_y][cap_x+2] = "M"
            grid[cap_y][cap_x+3] = "]"

    # Air defense
    ad_assets = [k for k in game_state.player_assets.keys() if 'sam' in k or 'patriot' in k or 's400' in k or 'bavar' in k]
    if ad_assets and game_state.player_assets.get(ad_assets[0], 0) > 0:
        grid[cap_y+1][cap_x-1] = "["
        grid[cap_y+1][cap_x] = "A"
        grid[cap_y+1][cap_x+1] = "]"

    # Drone swarms (Iran/China)
    if "shahed136_loitering" in game_state.player_assets and game_state.player_assets["shahed136_loitering"] > 0:
        grid[cap_y+2][cap_x-2] = "["
        grid[cap_y+2][cap_x-1] = "D"
        grid[cap_y+2][cap_x] = "]"

    # ---- Enemy assets with intel decay ----
    current_turn = game_state.turn
    for asset_id, last_seen in game_state.enemy_asset_last_seen.items():
        age = current_turn - last_seen
        if age <= 1:
            marker = f"{{{asset_id.upper()[0]}}}"
        elif age == 2:
            marker = f"({asset_id.upper()[0]})"
        else:
            continue
        # Place marker based on asset type
        if asset_id == "carrier":
            grid[10][30] = marker[0]
            grid[10][31] = marker[1] if len(marker) > 1 else " "
        elif asset_id == "patriot":
            grid[9][28] = marker[0]
            grid[9][29] = marker[1] if len(marker) > 1 else " "
        elif asset_id == "missile_silo":
            opp_cap = capitals.get(game_state.opponent_faction, (40, 2))
            grid[opp_cap[1]][opp_cap[0]-2] = marker[0]
            grid[opp_cap[1]][opp_cap[0]-1] = marker[1] if len(marker) > 1 else " "

    # ---- Legend ----
    legend = "LAND: .  WATER: ≈  ROAD: -  CAPITAL: 🏛  OIL: 🛢"
    lines = [legend]
    lines.append("╠" + "═" * width + "╣")
    for y in range(height):
        line = "║"
        for x in range(width):
            line += grid[y][x]
        line += "║"
        lines.append(line)
    return "\n".join(lines)
