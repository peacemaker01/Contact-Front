package com.contactfront.engine.rules;

import com.contactfront.engine.Log;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.Tile;
import com.contactfront.engine.model.Terrain;

public final class Visibility {
    private Visibility() {}

    private static final double FOREST_THERMAL_DECAY = 0.02;
    private static final double FOREST_THERMAL_THRESHOLD = 0.3;
    private static final double RADAR_CLUTTER_BASE = 1.0;
    private static final double RADAR_CLUTTER_COEFFICIENT = 0.15;
    private static final double RADAR_CLUTTER_THRESHOLD = 2.5;

    public static void computePlayerVisibility(GameState s) {
        Log.info("Visibility.computePlayerVisibility: computing for " + s.friendlyUnits.size() + " friendly units");
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
                        && LineOfSight.hasLineOfSight(f.x, f.y, e.x, e.y, s)
                        && canDetectThermal(s, f, e)) {
                    f.lastContactEnemyId = e.id;
                    f.lastContactName = e.profile.name();
                    f.lastContactX = e.x;
                    f.lastContactY = e.y;
                    f.lastContactElapsedMs = s.elapsedMs;
                }
            }
        }
        int newlySeen = 0;
        for (Unit e : s.enemyUnits) {
            if (e.destroyed) continue;
            boolean vis = s.visibility[e.y][e.x] == com.contactfront.engine.model.Visibility.VISIBLE;
            if (vis) {
                if (!e.knownToPlayer) {
                    newlySeen++;
                    s.log("intel", "New contact: " + e.profile.name() + " at (" + e.x + "," + e.y + ").");
                }
                e.knownToPlayer = true;
                e.lastKnownX = e.x;
                e.lastKnownY = e.y;
                e.lastSeenTurn = (int) (s.elapsedMs / 1000);
            } else if (e.knownToPlayer && e.lastSeenTurn < s.elapsedMs / 1000) {
                s.log("intel", "Lost contact: " + e.profile.name() + " last seen (" + e.lastKnownX + "," + e.lastKnownY + ").");
            }
        }
        Log.info("Visibility.computePlayerVisibility: " + newlySeen + " units newly seen");
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

    private static int countBuildingsInRange(GameState s, int x, int y, int range) {
        int count = 0;
        for (int by = Math.max(0, y - range); by <= Math.min(s.height() - 1, y + range); by++) {
            for (int bx = Math.max(0, x - range); bx <= Math.min(s.width() - 1, x + range); bx++) {
                if (s.grid[by][bx].type == Terrain.BUILDING) {
                    count++;
                }
            }
        }
        return count;
    }

    public static boolean enemySees(GameState s, Unit enemy, Unit friendly) {
        if (friendly.destroyed) return false;
        if (chebyshev(enemy.x, enemy.y, friendly.x, friendly.y) > enemy.reconRadius) return false;
        if (!LineOfSight.hasLineOfSight(enemy.x, enemy.y, friendly.x, friendly.y, s)) return false;
        if (friendly.sensorEmission == com.contactfront.engine.model.SensorEmission.Active_RF && s.ewCommsJammed) {
            int dist = manhattan(enemy.x, enemy.y, friendly.x, friendly.y);
            double jamEffect = 1.0 / Math.max(1.0, dist * dist / friendly.networkShieldMultiplier);
            if (jamEffect < 0.5) return false;
        }
        if (!canDetectThermal(s, enemy, friendly)) return false;
        return true;
    }

    private static boolean canDetectThermal(GameState s, Unit observer, Unit target) {
        double totalDistance = 0.0;
        int dx = Integer.compare(target.x, observer.x);
        int dy = Integer.compare(target.y, observer.y);
        int steps = Math.max(Math.abs(target.x - observer.x), Math.abs(target.y - observer.y));
        if (steps == 0) return true;

        for (int i = 1; i <= steps; i++) {
            int ix = (int) Math.round(observer.x + dx * i);
            int iy = (int) Math.round(observer.y + dy * i);
            if (ix >= 0 && ix < s.width() && iy >= 0 && iy < s.height()) {
                Tile t = s.grid[iy][ix];
                if (t.type == Terrain.FOREST && t.forestDensity > 0) {
                    totalDistance += t.forestDensity;
                }
            }
        }

        double effectiveSignature = 1.0 - (FOREST_THERMAL_DECAY * totalDistance);
        return effectiveSignature >= FOREST_THERMAL_THRESHOLD;
    }

    public static boolean radarCanAcquire(GameState s, Unit sam, Unit target) {
        if (sam.destroyed || target.destroyed) return false;
        int dist = manhattan(sam.x, sam.y, target.x, target.y);
        int range = sam.profile.category() == com.contactfront.engine.model.UnitCategory.AIR_DEFENSE ? 6 : 3;
        if (dist > range) return false;

        int buildingsInFov = countBuildingsInRange(s, sam.x, sam.y, Math.max(s.width(), s.height()) / 2);
        double scr = (RADAR_CLUTTER_BASE + RADAR_CLUTTER_COEFFICIENT * buildingsInFov) / 1.0;

        if (scr < RADAR_CLUTTER_THRESHOLD && target.y < s.height() / 3) {
            Log.info("Visibility.radarCanAcquire: clutter blocks low-altitude lock (" + buildingsInFov + " buildings)");
            return false;
        }
        return true;
    }

    private static int manhattan(int x0, int y0, int x1, int y1) {
        return Math.abs(x1 - x0) + Math.abs(y1 - y0);
    }

    private static int chebyshev(int x0, int y0, int x1, int y1) {
        return Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
    }
}
