package com.contactfront.engine.rules;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Tile;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.Visibility;

public final class Visibility {
    private Visibility() {}

    public static void computePlayerVisibility(GameState s) {
        s.ensureVisibility();
        for (int y = 0; y < s.height(); y++) {
            for (int x = 0; x < s.width(); x++) {
                if (s.visibility[y][x] == Visibility.VISIBLE) s.visibility[y][x] = Visibility.PREVIOUSLY_SEEN;
            }
        }
        for (Unit f : s.friendlyUnits) {
            if (f.destroyed) continue;
            revealAround(s, f.x, f.y, f.reconRadius);
        }
        for (Unit e : s.enemyUnits) {
            if (e.destroyed) continue;
            if (s.visibility[e.y][e.x] == Visibility.VISIBLE) {
                e.knownToPlayer = true;
                e.lastKnownX = e.x;
                e.lastKnownY = e.y;
                e.lastSeenTurn = s.turn;
            }
        }
    }

    private static void revealAround(GameState s, int cx, int cy, int radius) {
        for (int y = Math.max(0, cy - radius); y <= Math.min(s.height() - 1, cy + radius); y++) {
            for (int x = Math.max(0, cx - radius); x <= Math.min(s.width() - 1, cx + radius); x++) {
                if (chebyshev(cx, cy, x, y) > radius) continue;
                if (LineOfSight.hasLineOfSight(cx, cy, x, y, s.grid)) {
                    s.visibility[y][x] = Visibility.VISIBLE;
                }
            }
        }
    }

    public static boolean enemySees(GameState s, Unit enemy, Unit friendly) {
        if (friendly.destroyed) return false;
        if (chebyshev(enemy.x, enemy.y, friendly.x, friendly.y) > enemy.reconRadius) return false;
        return LineOfSight.hasLineOfSight(enemy.x, enemy.y, friendly.x, friendly.y, s.grid);
    }

    private static int chebyshev(int x0, int y0, int x1, int y1) {
        return Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
    }
}
