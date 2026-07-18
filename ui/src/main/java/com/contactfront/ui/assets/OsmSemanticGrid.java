package com.contactfront.ui.assets;

import com.contactfront.engine.model.Building;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.RoadSegment;
import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.Tile;

import java.util.List;

public final class OsmSemanticGrid {
    private static final double EARTH_RADIUS = 6378137.0;
    private static final double MERCATOR_MAX = 20037508.34;
    
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
    
    public static void applyForests(GameState s, List<double[][]> forests) {
        if (forests == null) return;
        for (double[][] forest : forests) {
            if (forest != null) {
                fillPolygon(s, forest, Terrain.FOREST);
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
        double mercX = lonToWebMercatorX(lon);
        double centerMercX = lonToWebMercatorX(s.longitude);
        double mercPerTile = 2.0 * MERCATOR_MAX / (double) s.width();
        int gx = (int) Math.round((mercX - centerMercX) / mercPerTile + s.width() / 2.0);
        return Math.max(0, Math.min(s.width() - 1, gx));
    }

    private static int latToGrid(GameState s, double lat) {
        double mercY = latToWebMercatorY(lat);
        double centerMercY = latToWebMercatorY(s.latitude);
        double mercPerTile = 2.0 * MERCATOR_MAX / (double) s.height();
        int gy = (int) Math.round((centerMercY - mercY) / mercPerTile + s.height() / 2.0);
        return Math.max(0, Math.min(s.height() - 1, gy));
    }
    
    private static double lonToWebMercatorX(double lon) {
        return EARTH_RADIUS * Math.toRadians(lon);
    }

    private static double latToWebMercatorY(double lat) {
        double latRad = Math.toRadians(lat);
        return EARTH_RADIUS * Math.log(Math.tan(Math.PI / 4 + latRad / 2));
    }

    private static void fillPolygon(GameState s, double[][] points, Terrain type) {
        if (points.length < 3) return;
        
        // Simple scanline fill for small polygons
        int minY = s.height(), maxY = -1, minX = s.width(), maxX = -1;
        int[] xs = new int[points.length];
        int[] ys = new int[points.length];
        
        for (int i = 0; i < points.length; i++) {
            xs[i] = lonToGrid(s, points[i][0]);
            ys[i] = latToGrid(s, points[i][1]);
            minX = Math.min(minX, xs[i]);
            maxX = Math.max(maxX, xs[i]);
            minY = Math.min(minY, ys[i]);
            maxY = Math.max(maxY, ys[i]);
        }
        
        // Bound to map
        minX = Math.max(0, minX - 1);
        maxX = Math.min(s.width() - 1, maxX + 1);
        minY = Math.max(0, minY - 1);
        maxY = Math.min(s.height() - 1, maxY + 1);
        
        // Simple point-in-polygon fill
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (pointInPolygon(x, y, xs, ys)) {
                    Tile t = s.grid[y][x];
                    if (t != null && t.type != Terrain.BUILDING) {
                        t.type = type;
                        t.movementCost = type == Terrain.FOREST ? 2.0 : t.movementCost;
                        t.coverBonus = type == Terrain.FOREST ? 50 : t.coverBonus;
                        t.blocksLos = type == Terrain.FOREST;
                    }
                }
            }
        }
    }
    
    private static boolean pointInPolygon(int px, int py, int[] xs, int[] ys) {
        boolean inside = false;
        for (int i = 0, j = xs.length - 1; i < xs.length; j = i++) {
            if (((ys[i] > py) != (ys[j] > py)) &&
                (px < (xs[j] - xs[i]) * (py - ys[i]) / (ys[j] - ys[i] + 0.001) + xs[i])) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static void drawLine(GameState s, double lon0, double lat0, double lon1, double lat1, Terrain type) {
        int x0 = lonToGrid(s, lon0);
        int y0 = latToGrid(s, lat0);
        int x1 = lonToGrid(s, lon1);
        int y1 = latToGrid(s, lat1);
        
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0, y = y0;
        while (true) {
            if (x >= 0 && x < s.width() && y >= 0 && y < s.height()) {
                Tile t = s.grid[y][x];
                if (t != null && t.type != Terrain.BUILDING) {
                    t.type = type;
                    t.movementCost = 0.5;
                    t.coverBonus = 0;
                    t.blocksLos = false;
                }
            }
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx) { err += dx; y += sy; }
        }
    }
}