def check_objectives(game_state):
    # Check each objective tile
    for obj in game_state.objectives:
        ox, oy = obj.get("coords", (None, None))
        if ox is None:
            continue
        for u in game_state.friendly_units:
            if not u.destroyed and u.x == ox and u.y == oy:
                game_state.victory = True
                game_state.game_over = True
                return True
    # Check enemy wipe
    if all(u.destroyed for u in game_state.enemy_units):
        game_state.victory = True
        game_state.game_over = True
        return True
    # Check friendly wipe
    if all(u.destroyed for u in game_state.friendly_units):
        game_state.victory = False
        game_state.game_over = True
        return True
    # Check turn limit
    if game_state.turn >= game_state.max_turns:
        game_state.victory = False
        game_state.game_over = True
        return True
    return False
