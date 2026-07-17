package com.contactfront.ui.assets;

import com.contactfront.engine.model.Building;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.RoadSegment;
import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.Tile;

import java.util.List;

public final class OsmSemanticGrid {
    private OsmSemanticGrid() {}

    public static void apply(GameState s, List<RoadSegment> roads, List<Building> buildings) {
        if (roads != null) {
            for (RoadSegment road : roads) {
                applyRoad(s, road);
            }
        }
        if (buildings != null) {
            for (Building bldg : buildings) {
                applyBuilding(s, bldg);
            }
        }
    }

    private static void applyRoad(GameState s, RoadSegment road) {
        if (road.points() == null || road.points().size() < 2) return;
        for (int i = 0; i < road.points().size() - 1; i++) {
            double[] p0 = road.points().get(i);
            double[] p1 = road.points().get(i + 1);
            drawLine(s, p0[0], p0[1], p1[0], p1[1], Terrain.ROAD);
        }
    }

    private static void applyBuilding(GameState s, Building bldg) {
        if (bldg.footprint == null) return;
        for (double[] pt : bldg.footprint) {
            int gx = lonToGrid(s, pt[0]);
            int gy = latToGrid(s, pt[1]);
            if (gx >= 0 && gx < s.width() && gy >= 0 && gy < s.height()) {
                Tile t = s.grid[gy][gx];
                if (t != null) {
                    t.type = Terrain.BUILDING;
                    t.movementCost = 999.0;
                    t.coverBonus = 75;
                    t.blocksLos = true;
                }
            }
        }
    }

    private static int lonToGrid(GameState s, double lon) {
        double lonPerTile = 360.0 / s.width();
        int gx = (int) Math.round((lon - s.longitude + 180) / lonPerTile);
        return Math.max(0, Math.min(s.width() - 1, gx));
    }

    private static int latToGrid(GameState s, double lat) {
        double latPerTile = 180.0 / s.height();
        int gy = (int) Math.round((s.latitude - lat + 90) / latPerTile);
        return Math.max(0, Math.min(s.height() - 1, gy));
    }

    private static void drawLine(GameState s, double x0, double y0, double x1, double y1, Terrain type) {
        double dx = Math.abs(x1 - x0);
        double dy = Math.abs(y1 - y0);
        double sx = x0 < x1 ? 1 : -1;
        double sy = y0 < y1 ? 1 : -1;
        double err = dx - dy;
        double x = x0, y = y0;
        while (true) {
            int gx = lonToGrid(s, x);
            int gy = latToGrid(s, y);
            if (gx >= 0 && gx < s.width() && gy >= 0 && gy < s.height()) {
                Tile t = s.grid[gy][gx];
                if (t != null && t.type != Terrain.BUILDING) {
                    t.type = type;
                    t.movementCost = 0.5;
                    t.coverBonus = 0;
                    t.blocksLos = false;
                }
            }
            if (x == x1 && y == y1) break;
            double e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx) { err += dx; y += sy; }
        }
    }
}