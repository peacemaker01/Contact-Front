package com.contactfront.engine.rules;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Tile;
import com.contactfront.engine.model.Unit;

/** Bresenham line-of-sight over the semantic grid (blocking tiles stop LOS). */
public final class LineOfSight {
    private LineOfSight() {}

    public static boolean hasLineOfSight(int x0, int y0, int x1, int y1, Tile[][] grid) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0, y = y0;
        boolean first = true;
        while (!(x == x1 && y == y1)) {
            if (!first && grid[y][x].blocksLos) {
                return false;
            }
            first = false;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx)  { err += dx; y += sy; }
        }
        return true;
    }

    public static boolean hasLineOfSight(int x0, int y0, int x1, int y1, GameState s) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0, y = y0;
        boolean first = true;
        while (!(x == x1 && y == y1)) {
            if (!first && (s.grid[y][x].blocksLos || s.hasSmoke(x, y))) {
                return false;
            }
            first = false;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx)  { err += dx; y += sy; }
        }
        return true;
    }

    public static boolean visibleByAnyFriendly(GameState s, int ex, int ey) {
        for (Unit u : s.friendlyUnits) {
            if (!u.destroyed && hasLineOfSight(u.x, u.y, ex, ey, s)) {
                return true;
            }
        }
        return false;
    }
}
