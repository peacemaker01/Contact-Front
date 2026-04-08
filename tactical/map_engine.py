import random
from game_state import Tile

# Terrain definitions with very low movement costs
TERRAIN = {
    '.': (".", None, 0, 0.1, "open"),
    'T': ("🌲", "🌲", 35, 0.2, "forest"),
    'H': ("⛰️", "⛰️", 20, 0.2, "hill"),
    'B': ("🏚️", "🏚️", 50, 0.15, "ruin"),
    'U': ("🏢", "🏢", 60, 0.2, "building"),
    'R': ("-", None, 0, 0.05, "road_horiz"),
    'R|': ("|", None, 0, 0.05, "road_vert"),
    'R+': ("+", None, 0, 0.05, "road_cross"),
    'W': ("~", None, 0, 999, "water"),
    '~': ("~", None, 0, 0.5, "ford"),
    'C': ("+", None, 40, 0.2, "checkpoint"),
    'X': ("X", "💥", 25, 0.1, "crater"),
    'O': ("★", "★", 10, 0.05, "objective"),
    'F': ("#", "🔥", -10, 0.2, "fire"),
    'M': ("M", "💣", 0, 0.3, "minefield")
}

def generate_tactical_map(width=50, height=14):
    grid = [[None for _ in range(width)] for __ in range(height)]
    for y in range(height):
        for x in range(width):
            grid[y][x] = Tile(type='.', emoji=TERRAIN['.'][0],
                              cover_bonus=TERRAIN['.'][2], movement_cost=TERRAIN['.'][3])
    # Main horizontal road
    road_y = height // 2
    for x in range(5, width-5):
        grid[road_y][x] = Tile(type='R', emoji=TERRAIN['R'][0],
                               cover_bonus=TERRAIN['R'][2], movement_cost=TERRAIN['R'][3])
    # Vertical road to objective
    for y in range(road_y, height-3):
        grid[y][width-7] = Tile(type='R|', emoji=TERRAIN['R|'][0],
                                cover_bonus=TERRAIN['R|'][2], movement_cost=TERRAIN['R|'][3])
    # Scatter buildings
    for _ in range(20):
        bx = random.randint(4, width-4)
        by = random.randint(2, height-3)
        if not (by == road_y and 5 <= bx <= width-5) and not (bx == width-7 and by >= road_y):
            grid[by][bx] = Tile(type='U', emoji=TERRAIN['U'][1],
                                cover_bonus=TERRAIN['U'][2], movement_cost=TERRAIN['U'][3])
    # Forest patches
    for y in range(2, height-2):
        for x in [1, 2, width-3, width-2]:
            if random.random() > 0.3:
                grid[y][x] = Tile(type='T', emoji=TERRAIN['T'][1],
                                  cover_bonus=TERRAIN['T'][2], movement_cost=TERRAIN['T'][3])
    # River
    river_y = road_y + 3
    for x in range(8, 14):
        grid[river_y][x] = Tile(type='W', emoji=TERRAIN['W'][0],
                                cover_bonus=TERRAIN['W'][2], movement_cost=TERRAIN['W'][3])
    # Objective
    obj_x, obj_y = width-5, height-4
    grid[obj_y][obj_x] = Tile(type='O', emoji=TERRAIN['O'][1],
                              cover_bonus=TERRAIN['O'][2], movement_cost=TERRAIN['O'][3])
    return grid, (obj_x, obj_y)

def line_of_sight(x0, y0, x1, y1, grid):
    dx = abs(x1 - x0)
    dy = abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx - dy
    x, y = x0, y0
    while (x, y) != (x1, y1):
        if grid[y][x].type in ('U', 'B', 'H', 'T'):
            return False
        e2 = 2 * err
        if e2 > -dy:
            err -= dy
            x += sx
        if e2 < dx:
            err += dx
            y += sy
    return True

def line_of_sight_any(game_state, ex, ey):
    grid = game_state.map_grid
    for u in game_state.friendly_units:
        if not u.destroyed and line_of_sight(u.x, u.y, ex, ey, grid):
            return True
    return False

def render_ascii_map(game_state, fog_of_war=True):
    grid = game_state.map_grid
    width = len(grid[0])
    height = len(grid)
    friendly_pos = {(u.x, u.y): u for u in game_state.friendly_units if not u.destroyed}
    enemy_pos = {(u.x, u.y): u for u in game_state.enemy_units if not u.destroyed}
    ANSI_GREEN = "\033[92m"
    ANSI_RED = "\033[91m"
    ANSI_RESET = "\033[0m"

    lines = []
    # Column header (every 5 columns)
    col_header = "   "
    for x in range(width):
        if x % 5 == 0:
            col_header += f"{x//5}" if x == 0 else f"   {x//5}"
        else:
            col_header += " "
    col_header = col_header[:width+3]
    lines.append(col_header)
    lines.append("   " + "─" * width)

    for y in range(height):
        row_label = f"{y:2d} "
        line = row_label
        for x in range(width):
            if (x, y) in friendly_pos:
                unit = friendly_pos[(x, y)]
                marker = f"{unit.type_code}{unit.id}"
                if unit.suppressed:
                    marker += "!"
                line += f"{ANSI_GREEN}{marker}{ANSI_RESET}"
            elif (x, y) in enemy_pos and (not fog_of_war or line_of_sight_any(game_state, x, y)):
                unit = enemy_pos[(x, y)]
                marker = f"{unit.type_code}{unit.id}"
                line += f"{ANSI_RED}{marker}{ANSI_RESET}"
            elif (x, y) in enemy_pos and fog_of_war:
                line += f"{ANSI_RED}??{ANSI_RESET}"
            else:
                tile = grid[y][x]
                if tile.emoji in ["🌲", "⛰️", "🏚️", "🏢", "★", "🔥", "💣"]:
                    line += tile.emoji
                else:
                    simple = {'.': '.', '-': '-', '|': '|', '+': '+', '~': '~', 'X': 'X', 'M': 'M'}
                    char = simple.get(tile.emoji, tile.emoji)
                    line += char
        lines.append(line)

    legend = "  Units: R=Rifle T=Tank I=IFV H=Helo A=Arty D=Drone K=Kamikaze P=Proxy S=SAM  Green=Friend Red=Enemy  ! = Suppressed"
    lines.append(legend)
    lines.append("  Coordinates: Column numbers at top (every 5), row numbers on left. Use 'move T2 3 east' for relative movement.")
    return "\n".join(lines)
