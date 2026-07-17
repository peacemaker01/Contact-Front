package com.contactfront.engine.terrain;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.Tile;

import java.util.ArrayList;
import java.util.List;

public final class DetailedMeshGenerator {
    private DetailedMeshGenerator() {}

    public record RoadSegment(double x1, double y1, double x2, double y2, double width) {}
    public record BuildingFootprint(int x, int y, int width, int height, int buildingType) {}

    public static List<RoadSegment> generateRoadSegments(GameState state) {
        List<RoadSegment> segments = new ArrayList<>();
        int w = state.width();
        int h = state.height();
        double roadWidth = 0.4;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Terrain t = state.grid[y][x].type;
                if (t != Terrain.ROAD && t != Terrain.ROAD_VERT && t != Terrain.ROAD_CROSS) continue;

                if (t == Terrain.ROAD || t == Terrain.ROAD_CROSS) {
                    if (x + 1 < w && (state.grid[y][x + 1].type == Terrain.ROAD || 
                        state.grid[y][x + 1].type == Terrain.ROAD_CROSS)) {
                        segments.add(new RoadSegment(x + 0.5, y, x + 1.5, y, roadWidth));
                    }
                }
                if (t == Terrain.ROAD_VERT || t == Terrain.ROAD_CROSS) {
                    if (y + 1 < h && (state.grid[y + 1][x].type == Terrain.ROAD_VERT ||
                        state.grid[y + 1][x].type == Terrain.ROAD_CROSS)) {
                        segments.add(new RoadSegment(x, y + 0.5, x, y + 1.5, roadWidth));
                    }
                }
            }
        }
        return segments;
    }

    public static List<BuildingFootprint> generateBuildingFootprints(GameState state) {
        List<BuildingFootprint> buildings = new ArrayList<>();
        int w = state.width();
        int h = state.height();
        int buildingId = 1;

        boolean[][] placed = new boolean[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (state.grid[y][x].type != Terrain.BUILDING) continue;
                if (placed[y][x]) continue;

                int maxWidth = 1;
                while (x + maxWidth < w && state.grid[y][x + maxWidth].type == Terrain.BUILDING && !placed[y][x + maxWidth]) {
                    maxWidth++;
                }

                int maxHeight = 1;
                while (y + maxHeight < h && state.grid[y + maxHeight][x].type == Terrain.BUILDING && !placed[y + maxHeight][x]) {
                    boolean canExtend = true;
                    for (int dy = 0; dy < maxHeight; dy++) {
                        if (state.grid[y + dy][x + maxWidth] != null && 
                            state.grid[y + dy][x + maxWidth].type == Terrain.BUILDING) {
                            canExtend = false;
                            break;
                        }
                    }
                    if (!canExtend) break;
                    maxHeight++;
                }

                for (int dy = 0; dy < maxHeight; dy++) {
                    for (int dx = 0; dx < maxWidth; dx++) {
                        placed[y + dy][x + dx] = true;
                    }
                }

                buildings.add(new BuildingFootprint(x, y, maxWidth, maxHeight, buildingId++));
            }
        }
        return buildings;
    }

    public static List<BushesCluster> generateBushClusters(GameState state) {
        List<BushesCluster> clusters = new ArrayList<>();
        int w = state.width();
        int h = state.height();
        long seed = state.seed;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (shouldPlaceBush(seed, x, y, state.grid[y][x].type)) {
                    int clusterSize = 1 + (int)(Math.random() * 4);
                    List<int[]> positions = new ArrayList<>();
                    positions.add(new int[]{x, y});

                    for (int i = 1; i < clusterSize; i++) {
                        int px = x + (int)(Math.random() * 3 - 1);
                        int py = y + (int)(Math.random() * 3 - 1);
                        if (px >= 0 && px < w && py >= 0 && py < h &&
                            state.grid[py][px].type == Terrain.OPEN) {
                            positions.add(new int[]{px, py});
                        }
                    }
                    clusters.add(new BushesCluster(positions));
                }
            }
        }
        return clusters;
    }

    private static boolean shouldPlaceBush(long seed, int x, int y, Terrain t) {
        if (t != Terrain.OPEN && t != Terrain.SCRUB) return false;
        long hash = seed + x * 73856093L + y * 19349663L;
        hash = (hash ^ (hash >>> 33)) * 0xff51afd7ed558ccdL;
        hash = (hash ^ (hash >>> 33)) * 0xc4ceb9fe1a85ec53L;
        hash = hash ^ (hash >>> 33);
        double rand = ((hash >>> 11) & 0x1fffff) / (double) 0x1fffff;
        return rand < 0.08;
    }

    public static List<TreeCluster> generateTreeClusters(GameState state) {
        List<TreeCluster> clusters = new ArrayList<>();
        int w = state.width();
        int h = state.height();
        long seed = state.seed;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (shouldPlaceTree(seed, x, y, state.grid[y][x].type)) {
                    int clusterSize = 3 + (int)(Math.random() * 5);
                    List<int[]> positions = new ArrayList<>();
                    positions.add(new int[]{x, y});

                    for (int i = 1; i < clusterSize; i++) {
                        int px = x + (int)(Math.random() * 5 - 2);
                        int py = y + (int)(Math.random() * 5 - 2);
                        if (px >= 0 && px < w && py >= 0 && py < h &&
                            state.grid[py][px].type == Terrain.FOREST) {
                            positions.add(new int[]{px, py});
                        }
                    }
                    clusters.add(new TreeCluster(positions));
                }
            }
        }
        return clusters;
    }

    private static boolean shouldPlaceTree(long seed, int x, int y, Terrain t) {
        if (t != Terrain.FOREST) return false;
        long hash = seed + x * 73856093L + y * 19349663L + 999;
        hash = (hash ^ (hash >>> 33)) * 0xff51afd7ed558ccdL;
        hash = (hash ^ (hash >>> 33)) * 0xc4ceb9fe1a85ec53L;
        hash = hash ^ (hash >>> 33);
        double rand = ((hash >>> 11) & 0x1fffff) / (double) 0x1fffff;
        return rand < 0.15;
    }

    public record BushesCluster(List<int[]> positions) {}
    public record TreeCluster(List<int[]> positions) {}
}