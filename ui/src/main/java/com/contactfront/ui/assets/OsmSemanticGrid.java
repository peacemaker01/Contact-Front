package com.contactfront.ui.assets;

import com.contactfront.engine.model.Building;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.RoadSegment;
import com.contactfront.engine.model.RoadSegment.RoadType;
import com.contactfront.engine.model.UnitCategory;
import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.Tile;
import com.contactfront.ui.assets.OverpassApiClient.WaterwaySegment;
import com.contactfront.ui.Log;

import java.util.List;
import java.util.ArrayList;

public final class OsmSemanticGrid {
    private static final double EARTH_RADIUS = 6378137.0;
    private static final double MERCATOR_MAX = 20037508.34;

    private OsmSemanticGrid() {}

    public static void apply(GameState s, List<RoadSegment> roads, List<Building> buildings) {
        Log.info("OsmSemanticGrid.apply: Processing " + (roads != null ? roads.size() : 0) + " roads, " + (buildings != null ? buildings.size() : 0) + " buildings");
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
        Log.info("OsmSemanticGrid.apply: Complete");
    }

    public static void applyForests(GameState s, List<double[][]> forests) {
        Log.info("OsmSemanticGrid.applyForests: Processing " + (forests != null ? forests.size() : 0) + " forest polygons");
        if (forests == null) return;
        for (double[][] forest : forests) {
            if (forest != null) {
                fillPolygon(s, forest, Terrain.FOREST, 0.7);
            }
        }
        Log.info("OsmSemanticGrid.applyForests: Complete");
    }

    public static void applyWetlands(GameState s, List<double[][]> wetlands) {
        Log.info("OsmSemanticGrid.applyWetlands: Processing " + (wetlands != null ? wetlands.size() : 0) + " wetland polygons");
        if (wetlands == null) return;
        for (double[][] wetland : wetlands) {
            if (wetland != null) {
                fillPolygon(s, wetland, Terrain.WETLAND, 0.0);
            }
        }
        Log.info("OsmSemanticGrid.applyWetlands: Complete");
    }

    public static void applyWaterways(GameState s, List<WaterwaySegment> waterways) {
        Log.info("OsmSemanticGrid.applyWaterways: Processing " + (waterways != null ? waterways.size() : 0) + " waterway segments");
        if (waterways == null) return;
        for (WaterwaySegment waterway : waterways) {
            if (waterway.bridge()) {
                drawLine(s, waterway.points(), Terrain.ROAD, RoadType.RESIDENTIAL);
            } else if (waterway.ford()) {
                drawLine(s, waterway.points(), Terrain.FORD, RoadType.RESIDENTIAL);
            } else {
                drawLine(s, waterway.points(), Terrain.WATERWAY, null);
            }
        }
        Log.info("OsmSemanticGrid.applyWaterways: Complete");
    }

    private static void applyRoad(GameState s, RoadSegment road) {
        if (road.points() == null || road.points().size() < 2) return;
        drawLine(s, road.points(), Terrain.ROAD, road.type());
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

    private static void drawLine(GameState s, List<double[]> points, Terrain type, RoadType roadType) {
        if (points == null || points.size() < 2) return;
        for (int i = 0; i < points.size() - 1; i++) {
            double[] p0 = points.get(i);
            double[] p1 = points.get(i + 1);
            bresenhamLine(s, p0[0], p0[1], p1[0], p1[1], type, roadType);
        }
    }

    private static void bresenhamLine(GameState s, double lon0, double lat0, double lon1, double lat1, Terrain type, RoadType roadType) {
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
                    if (type == Terrain.WATERWAY) {
                        t.type = Terrain.WATERWAY;
                        t.movementCost = 999.0;
                        t.isWaterway = true;
                        t.blocksLos = false;
                    } else if (type == Terrain.FORD) {
                        t.type = Terrain.FORD;
                        t.movementCost = 3.0;
                        t.isWaterway = false;
                        t.blocksLos = false;
                    } else {
                        t.type = type;
                        t.roadType = roadType;
                        double baseCost = 1.0;
                        if (roadType != null) {
                            baseCost = 1.0 / Terrain.roadSpeedMultiplier(roadType, UnitCategory.INFANTRY);
                        }
                        t.movementCost = baseCost;
                        t.coverBonus = 0;
                        t.blocksLos = false;
                    }
                }
            }
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx) { err += dx; y += sx; }
        }
    }

    private static void fillPolygon(GameState s, double[][] points, Terrain type, double density) {
        if (points.length < 3) return;

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

        minX = Math.max(0, minX - 1);
        maxX = Math.min(s.width() - 1, maxX + 1);
        minY = Math.max(0, minY - 1);
        maxY = Math.min(s.height() - 1, maxY + 1);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (pointInPolygon(x, y, xs, ys)) {
                    Tile t = s.grid[y][x];
                    if (t != null && t.type != Terrain.BUILDING && t.type != Terrain.ROAD) {
                        t.type = type;
                        if (type == Terrain.FOREST) {
                            t.forestDensity = density;
                            t.movementCost = 2.0;
                            t.coverBonus = 50;
                            t.blocksLos = true;
                        } else if (type == Terrain.WETLAND) {
                            t.movementCost = 999.0;
                            t.coverBonus = 0;
                            t.blocksLos = false;
                        }
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
}