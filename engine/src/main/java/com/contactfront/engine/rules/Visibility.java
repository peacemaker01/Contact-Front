package com.contactfront.engine.rules;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;

public final class Visibility {
    private Visibility() {}

    public static void computePlayerVisibility(GameState s) {
        s.ensureVisibility();
        for (int y = 0; y < s.height(); y++) {
            for (int x = 0; x < s.width(); x++) {
                if (s.visibility[y][x] == com.contactfront.engine.model.Visibility.VISIBLE) {
                    s.visibility[y][x] = com.contactfront.engine.model.Visibility.PREVIOUSLY_SEEN;
                }
            }
        }
        for (Unit f : s.friendlyUnits) {
            if (f.destroyed) continue;
            revealAround(s, f.x, f.y, f.reconRadius);
            for (Unit e : s.enemyUnits) {
                if (e.destroyed) continue;
                if (s.visibility[e.y][e.x] == com.contactfront.engine.model.Visibility.VISIBLE
                        && chebyshev(f.x, f.y, e.x, e.y) <= f.reconRadius
                        && LineOfSight.hasLineOfSight(f.x, f.y, e.x, e.y, s)) {
                    f.lastContactEnemyId = e.id;
                    f.lastContactName = e.profile.name();
                    f.lastContactX = e.x;
                    f.lastContactY = e.y;
                    f.lastContactElapsedMs = s.elapsedMs;
                }
            }
        }
        for (Unit e : s.enemyUnits) {
            if (e.destroyed) continue;
            boolean vis = s.visibility[e.y][e.x] == com.contactfront.engine.model.Visibility.VISIBLE;
            if (vis) {
                if (!e.knownToPlayer) s.log("intel", "New contact: " + e.profile.name() + " at (" + e.x + "," + e.y + ").");
                e.knownToPlayer = true;
                e.lastKnownX = e.x;
                e.lastKnownY = e.y;
                e.lastSeenTurn = (int) (s.elapsedMs / 1000);
            } else if (e.knownToPlayer && e.lastSeenTurn < s.elapsedMs / 1000) {
                s.log("intel", "Lost contact: " + e.profile.name() + " last seen (" + e.lastKnownX + "," + e.lastKnownY + ").");
            }
        }
    }

    private static void revealAround(GameState s, int cx, int cy, int radius) {
        for (int y = Math.max(0, cy - radius); y <= Math.min(s.height() - 1, cy + radius); y++) {
            for (int x = Math.max(0, cx - radius); x <= Math.min(s.width() - 1, cx + radius); x++) {
                if (chebyshev(cx, cy, x, y) > radius) continue;
                if (LineOfSight.hasLineOfSight(cx, cy, x, y, s)) {
                    s.visibility[y][x] = com.contactfront.engine.model.Visibility.VISIBLE;
                }
            }
        }
    }

    public static boolean enemySees(GameState s, Unit enemy, Unit friendly) {
        if (friendly.destroyed) return false;
        if (chebyshev(enemy.x, enemy.y, friendly.x, friendly.y) > enemy.reconRadius) return false;
        return LineOfSight.hasLineOfSight(enemy.x, enemy.y, friendly.x, friendly.y, s);
    }

    private static int chebyshev(int x0, int y0, int x1, int y1) {
        return Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
    }
}
