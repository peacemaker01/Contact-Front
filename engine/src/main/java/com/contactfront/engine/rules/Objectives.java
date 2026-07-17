package com.contactfront.engine.rules;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Objective;
import com.contactfront.engine.model.Unit;

public final class Objectives {
    private Objectives() {}

    public static boolean check(GameState s) {
        for (Objective o : s.objectives) {
            for (Unit u : s.friendlyUnits) {
                if (!u.destroyed && u.x == o.x && u.y == o.y) {
                    s.victory = true;
                    s.gameOver = true;
                    s.log("orders", "Objective " + o.name + " secured. Victory.");
                    return true;
                }
            }
        }
        if (s.enemyUnits.stream().allMatch(u -> u.destroyed)) {
            s.victory = true; s.gameOver = true; s.log("orders", "All enemy forces eliminated. Victory.");
            return true;
        }
        if (s.friendlyUnits.stream().allMatch(u -> u.destroyed)) {
            s.victory = false; s.gameOver = true; s.log("orders", "Friendly forces wiped out. Defeat.");
            return true;
        }
        if (s.turn >= s.maxTurns) {
            s.victory = false; s.gameOver = true; s.log("orders", "Turn limit reached. Mission failed.");
            return true;
        }
        return false;
    }
}
