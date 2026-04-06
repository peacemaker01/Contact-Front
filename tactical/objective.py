def check_objectives(game_state):
    obj_tile = game_state.objectives[0]["coords"] if game_state.objectives else (None, None)
    if obj_tile[0] is not None:
        for u in game_state.friendly_units:
            if not u.destroyed and u.x == obj_tile[0] and u.y == obj_tile[1]:
                game_state.victory = True
                game_state.game_over = True
                return True
    if all(u.destroyed for u in game_state.enemy_units):
        game_state.victory = True
        game_state.game_over = True
        return True
    if game_state.turn >= game_state.max_turns:
        game_state.victory = False
        game_state.game_over = True
        return True
    return False
