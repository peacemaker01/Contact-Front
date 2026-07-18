package com.contactfront.engine.rules;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Tile;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.UnitCategory;
import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.MoveAction;
import com.contactfront.engine.model.Stance;

import java.util.ArrayList;
import java.util.List;

public final class Movement {
    private Movement() {}
    private static final long TICK_MS = 500;

    public static double pathCost(Tile[][] grid, int sx, int sy, int tx, int ty) {
        return pathCost(grid, sx, sy, tx, ty, UnitCategory.INFANTRY);
    }

    public static double pathCost(Tile[][] grid, int sx, int sy, int tx, int ty, UnitCategory category) {
        int steps = Math.max(Math.abs(tx - sx), Math.abs(ty - sy));
        if (steps == 0) return 0.0;
        double cost = 0.0;
        for (int i = 1; i <= steps; i++) {
            int ix = (int) Math.round(sx + (tx - sx) * (i / (double) steps));
            int iy = (int) Math.round(sy + (ty - sy) * (i / (double) steps));
            Tile t = grid[iy][ix];
            if (t.impassableForGround()) return Double.POSITIVE_INFINITY;
            if (category == UnitCategory.DRONE || category == UnitCategory.RECON) {
                if (t.type == Terrain.WATER) continue;
            }
            cost += t.movementCost;
        }
        return cost;
    }

    public static boolean applyMove(GameState s, Unit u, int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= s.width() || ty >= s.height()) return false;
        if (tx == u.x && ty == u.y) return false;
        double budget = u.effectiveMovementPoints() * u.stance.moveMult;
        double cost = pathCost(s.grid, u.x, u.y, tx, ty, u.profile.category());
        if (cost == Double.POSITIVE_INFINITY) return false;
        if (cost > budget) return false;
        if (occupied(s, tx, ty, u)) return false;
        u.x = tx;
        u.y = ty;
        u.movementPoints -= cost;
        u.entrenchment = 0;
        return true;
    }

    private static double ticksPerTile(Unit u) {
        double baseTicks = switch (u.profile.category()) {
            case ARMOR, LOGISTICS -> 2.0;
            case RECON -> 1.5;
            case INFANTRY -> 6.0;
            case ENGINEER -> 7.0;
            case AIR_DEFENSE, ARTILLERY -> 8.0;
            default -> 6.0;
        };
        return baseTicks / (u.stance.moveMult > 0 ? u.stance.moveMult : 1.0);
    }

    public static boolean startMove(GameState s, Unit u, int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= s.width() || ty >= s.height()) return false;
        if (tx == u.x && ty == u.y) return false;
        double cost = pathCost(s.grid, u.x, u.y, tx, ty, u.profile.category());
        if (cost == Double.POSITIVE_INFINITY) return false;

        u.destX = tx;
        u.destY = ty;
        double totalTicks = Math.max(1, (double) cost * ticksPerTile(u));
        u.stepsRemaining = (int) Math.ceil(totalTicks);
        return true;
    }

    public static void tickMove(GameState s, Unit u) {
        if (u.destX < 0 || u.destroyed) return;

        int dx = Integer.compare(u.destX, u.x);
        int dy = Integer.compare(u.destY, u.y);
        if (dx == 0 && dy == 0) {
            u.destX = -1;
            u.destY = -1;
            u.stepsRemaining = 0;
            return;
        }

        int nx = u.x + dx;
        int ny = u.y + dy;

        if (nx < 0 || ny < 0 || nx >= s.width() || ny >= s.height()) {
            u.destX = -1;
            u.destY = -1;
            u.stepsRemaining = 0;
            return;
        }
        if (occupied(s, nx, ny, u)) {
            u.destX = -1;
            u.destY = -1;
            u.stepsRemaining = 0;
            return;
        }

        u.x = nx;
        u.y = ny;
        u.stepsRemaining--;

        if (u.stepsRemaining <= 0 || (u.x == u.destX && u.y == u.destY)) {
            u.destX = -1;
            u.destY = -1;
            u.stepsRemaining = 0;
        }
    }

    private static boolean occupied(GameState s, int x, int y, Unit self) {
        for (Unit u : s.friendlyUnits) if (u != self && !u.destroyed && u.x == x && u.y == y) return true;
        for (Unit u : s.enemyUnits) if (u != self && !u.destroyed && u.x == x && u.y == y) return true;
        return false;
    }

    public static List<int[]> reachable(GameState s, Unit u) {
        List<int[]> out = new ArrayList<>();
        int maxTicks = (int) (ticksPerTile(u) * 120);
        int maxTiles = maxTicks;
        for (int y = 0; y < s.height(); y++) {
            for (int x = 0; x < s.width(); x++) {
                if (x == u.x && y == u.y) continue;
                int dist = Math.abs(x - u.x) + Math.abs(y - u.y);
                if (dist > maxTiles) continue;
                if (s.grid[y][x].impassable()) continue;
                out.add(new int[]{x, y});
            }
        }
        return out;
    }

    private static double tsPerTile(Unit u) {
        return switch (u.profile.category()) {
            case ARMOR, LOGISTICS -> 2.0;
            case RECON -> 1.5;
            case INFANTRY -> 6.0;
            case ENGINEER -> 7.0;
            case AIR_DEFENSE, ARTILLERY -> 8.0;
            default -> 6.0;
        };
    }

    public static boolean isInMotion(Unit u) {
        return u.destX >= 0 && !u.destroyed;
    }
}