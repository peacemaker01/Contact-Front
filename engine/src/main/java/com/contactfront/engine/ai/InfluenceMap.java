package com.contactfront.engine.ai;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Tile;
import com.contactfront.engine.model.Terrain;

public final class InfluenceMap {
    private InfluenceMap() {}

    private static final double FOREST_COVER_BONUS = 0.5;
    private static final double BUILDING_COVER_BONUS = 0.8;

    private double[][] threatMap;
    private double[][] coverMap;
    private int width, height;

    public InfluenceMap(GameState s) {
        this.width = s.width();
        this.height = s.height();
        this.threatMap = new double[height][width];
        this.coverMap = new double[height][width];
    }

    public void updateThreatMap(GameState s) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                threatMap[y][x] = ThreatEvaluator.staticThreatAtPoint(s, x, y);
                coverMap[y][x] = computeCoverValue(s.grid[y][x]);
            }
        }
    }

    private double computeCoverValue(Tile t) {
        if (t.type == Terrain.FOREST) return FOREST_COVER_BONUS;
        if (t.type == Terrain.BUSH || t.type == Terrain.BUILDING) return BUILDING_COVER_BONUS;
        if (t.type == Terrain.HILL) return 0.4;
        return 0.0;
    }

    public double threatAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return 0.0;
        return threatMap[y][x];
    }

    public double coverAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return 0.0;
        return coverMap[y][x];
    }

    public double[][] getThreatMap() { return threatMap; }
    public double[][] getCoverMap() { return coverMap; }
}