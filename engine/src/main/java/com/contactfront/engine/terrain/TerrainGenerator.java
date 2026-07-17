package com.contactfront.engine.terrain;

import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.Tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class TerrainGenerator {

    private TerrainGenerator() {}

    public record GenResult(Tile[][] grid, double[][] elevation, double[][] moisture,
                             int[] riverCrossing, int[] settlement, List<int[]> roads) {}

    public static GenResult generate(long seed, int width, int height) {
        Random rnd = new Random(seed);
        Noise elev = new Noise(seed);
        Noise moist = new Noise(seed ^ 0x9e3779b9L);
        Noise river = new Noise(seed ^ 0x85ebca6bL);

        double[][] e = new double[height][width];
        double[][] m = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                e[y][x] = elev.fbm(x, y, 4, 12);
                m[y][x] = moist.fbm(x + 100, y + 100, 3, 10);
            }
        }

        Tile[][] grid = new Tile[height][width];
        boolean[][] isWater = new boolean[height][width];
        boolean[][] isBuild = new boolean[height][width];

        int[] riverCrossing = computeRiver(grid, isWater, river, rnd, width, height);
        int[] settlement = computeSettlement(grid, isBuild, isWater, rnd, width, height);
        computeRoads(grid, riverCrossing, settlement, rnd, width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (grid[y][x] != null) continue;
                double ev = e[y][x];
                double mv = m[y][x];
                Terrain t;
                if (ev < 0.30) t = Terrain.WATER;
                else if (ev > 0.74) t = Terrain.HILL;
                else if (mv > 0.62) t = Terrain.FOREST;
                else if (mv > 0.40) t = Terrain.SCRUB;
                else t = Terrain.OPEN;
                grid[y][x] = new Tile(t, x, y);
            }
        }

        List<int[]> roads = new ArrayList<>();
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (grid[y][x].type == Terrain.ROAD || grid[y][x].type == Terrain.ROAD_CROSS) roads.add(new int[]{x, y});

        return new GenResult(grid, e, m, riverCrossing, settlement, roads);
    }

    private static int[] computeRiver(Tile[][] grid, boolean[][] isWater, Noise river, Random rnd, int w, int h) {
        int baseX = w / 2 + (rnd.nextInt(w / 3) - w / 6);
        int crossY = h / 2 + (rnd.nextInt(Math.max(1, h / 6)) - h / 12);
        for (int y = 0; y < h; y++) {
            double p = river.noise(0, y * 0.25) * 2 - 1;
            int x = Math.max(1, Math.min(w - 2, baseX + (int) (p * (w / 5))));
            for (int dx = -1; dx <= 1; dx++) {
                int xx = x + dx;
                if (xx < 0 || xx >= w) continue;
                grid[y][xx] = new Tile(Terrain.WATER, xx, y);
                isWater[y][xx] = true;
            }
            if (y == crossY) {
                grid[y][x] = new Tile(Terrain.FORD, x, y);
                isWater[y][x] = false;
                int rx = x + 1 < w ? x + 1 : x - 1;
                grid[y][rx] = new Tile(Terrain.ROAD_CROSS, rx, y);
                crossY = -1;
                return new int[]{x, y};
            }
        }
        return null;
    }

    private static int[] computeSettlement(Tile[][] grid, boolean[][] isBuild, boolean[][] isWater, Random rnd, int w, int h) {
        int cx = Math.max(3, Math.min(w - 4, w / 2 + (rnd.nextInt(w / 3) - w / 6)));
        int cy = Math.max(3, Math.min(h - 4, h / 2 + (rnd.nextInt(h / 3) - h / 6)));
        if (isWater[cy][cx]) {
            for (int r = 1; r < 4 && isWater[cy][cx]; r++) {
                if (cx + r < w - 1 && !isWater[cy][cx + r]) cx += r;
            }
        }
        int count = 6 + rnd.nextInt(5);
        int placed = 0;
        for (int y = cy - 2; y <= cy + 2 && placed < count; y++) {
            for (int x = cx - 2; x <= cx + 2 && placed < count; x++) {
                if (x < 0 || y < 0 || x >= w || y >= h) continue;
                if (isWater[y][x]) continue;
                if (grid[y][x] != null) continue;
                if ((x + y) % 2 == 0) {
                    grid[y][x] = new Tile(Terrain.BUILDING, x, y);
                    isBuild[y][x] = true;
                    placed++;
                }
            }
        }
        if (placed == 0) {
            grid[cy][cx] = new Tile(Terrain.BUILDING, cx, cy);
            placed = 1;
        }
        return placed > 0 ? new int[]{cx, cy} : null;
    }

    private static void computeRoads(Tile[][] grid, int[] crossing, int[] settlement, Random rnd, int w, int h) {
        int sy = settlement != null ? settlement[1] : h / 2;
        for (int x = 0; x < w; x++) {
            Tile t = grid[sy][x];
            if (t == null) continue;
            if (t.type == Terrain.WATER || t.type == Terrain.FORD
                    || t.type == Terrain.BUILDING || t.type == Terrain.ROAD_CROSS) continue;
            grid[sy][x] = new Tile(Terrain.ROAD, x, sy);
        }
        if (crossing != null) {
            int cx = crossing[0];
            for (int y = 0; y < h; y++) {
                Tile t = grid[y][cx];
                if (t == null) continue;
                if (t.type == Terrain.WATER || t.type == Terrain.FORD
                        || t.type == Terrain.BUILDING || t.type == Terrain.ROAD_CROSS) continue;
                grid[y][cx] = new Tile(Terrain.ROAD, cx, y);
            }
        }

        addSecondaryRoads(grid, w, h, rnd);
        addBuildingClusters(grid, w, h, rnd);
        addTreeLines(grid, w, h, rnd);
    }

    private static void addSecondaryRoads(Tile[][] grid, int w, int h, Random rnd) {
        int roadCount = 2 + rnd.nextInt(3);
        for (int i = 0; i < roadCount; i++) {
            boolean horizontal = rnd.nextBoolean();
            if (horizontal) {
                int y = rnd.nextInt(h);
                for (int x = 0; x < w; x++) {
                    Tile t = grid[y][x];
                    if (t == null) continue;
                    if (t.type == Terrain.WATER || t.type == Terrain.FORD
                            || t.type == Terrain.BUILDING || t.type == Terrain.ROAD_CROSS) continue;
                    if (rnd.nextDouble() < 0.7) grid[y][x] = new Tile(Terrain.ROAD, x, y);
                }
            } else {
                int x = rnd.nextInt(w);
                for (int y = 0; y < h; y++) {
                    Tile t = grid[y][x];
                    if (t == null) continue;
                    if (t.type == Terrain.WATER || t.type == Terrain.FORD
                            || t.type == Terrain.BUILDING || t.type == Terrain.ROAD_CROSS) continue;
                    if (rnd.nextDouble() < 0.7) grid[y][x] = new Tile(Terrain.ROAD, x, y);
                }
            }
        }
    }

    private static void addBuildingClusters(Tile[][] grid, int w, int h, Random rnd) {
        int clusters = 2 + rnd.nextInt(3);
        for (int i = 0; i < clusters; i++) {
            int cx = 2 + rnd.nextInt(w - 4);
            int cy = 2 + rnd.nextInt(h - 4);
            if (grid[cy][cx] != null && grid[cy][cx].type == Terrain.WATER) continue;
            int count = 3 + rnd.nextInt(4);
            int placed = 0;
            for (int y = cy - 2; y <= cy + 2 && placed < count; y++) {
                for (int x = cx - 2; x <= cx + 2 && placed < count; x++) {
                    if (x < 0 || y < 0 || x >= w || y >= h) continue;
                    if (grid[y][x] != null && grid[y][x].type != Terrain.OPEN) continue;
                    if ((x + y) % 3 == 0) {
                        grid[y][x] = new Tile(Terrain.BUILDING, x, y);
                        placed++;
                    }
                }
            }
        }
    }

    private static void addTreeLines(Tile[][] grid, int w, int h, Random rnd) {
        int lines = 3 + rnd.nextInt(4);
        for (int i = 0; i < lines; i++) {
            boolean horizontal = rnd.nextBoolean();
            if (horizontal) {
                int y = rnd.nextInt(h);
                for (int x = 0; x < w; x++) {
                    if (grid[y][x] == null || grid[y][x].type == Terrain.OPEN || grid[y][x].type == Terrain.FOREST) {
                        if (rnd.nextDouble() < 0.3) grid[y][x] = new Tile(Terrain.FOREST, x, y);
                    }
                }
            } else {
                int x = rnd.nextInt(w);
                for (int y = 0; y < h; y++) {
                    if (grid[y][x] == null || grid[y][x].type == Terrain.OPEN || grid[y][x].type == Terrain.FOREST) {
                        if (rnd.nextDouble() < 0.3) grid[y][x] = new Tile(Terrain.FOREST, x, y);
                    }
                }
            }
        }
    }
}
