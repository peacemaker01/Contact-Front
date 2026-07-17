package com.contactfront.ui;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Terrain;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public final class TerrainBaker {

    private TerrainBaker() {}

    // Photorealistic color palette (sRGB, 0xRRGGBB)
    private static final int DEEP_WATER     = 0x0d1b2a;  // Deep ocean
    private static final int SHALLOW_WATER  = 0x1b4f72;  // Coastal water
    private static final int SAND_WET       = 0x8b7355;  // Wet sand
    private static final int SAND_DRY       = 0xd4c4a8;  // Dry sand
    private static final int MUD            = 0x4a3f35;  // River mud
    private static final int GRASS_DRY      = 0x4a5d3a;  // Dry grassland
    private static final int GRASS_GREEN    = 0x3a6b2f;  // Healthy grass
    private static final int GRASS_LUSH     = 0x2d5a1e;  // Very lush
    private static final int SCRUB_LIGHT    = 0x5a6b4a;  // Light scrub
    private static final int SCRUB_DENSE    = 0x3d4d2e;  // Dense scrub
    private static final int FOREST_CANOPY  = 0x1b3d1b;  // Pine/conifer canopy
    private static final int FOREST_DECID   = 0x2a5a2a;  // Deciduous canopy
    private static final int FARMLAND_GREEN = 0x4a7c2e;  // Crops
    private static final int FARMLAND_BROWN = 0x8b7355;  // Fallow/tilled
    private static final int DIRT_PATH      = 0x7a6b52;  // Dirt road
    private static final int DIRT           = 0x5a4f3a;  // Dirt/soil
    private static final int ASPHALT_NEW    = 0x3a3a3c;  // Fresh asphalt
    private static final int ASPHALT_OLD    = 0x5a5a5c;  // Weathered
    private static final int CONCRETE       = 0x8a8a8c;  // Concrete
    private static final int ROOF_RED       = 0x8b2e2e;  // Red tile roof
    private static final int ROOF_GRAY      = 0x4a4a4a;  // Gray roof
    private static final int ROOF_BROWN     = 0x5a3e2e;  // Brown roof
    private static final int ROOF_BLUE      = 0x2e3e5a;  // Industrial blue
    private static final int ROCK_LIGHT     = 0x8a8a8a;  // Light rock
    private static final int ROCK_DARK      = 0x5a5a5a;  // Dark rock
    private static final int SNOW           = 0xf0f0f0;  // Snow
    private static final int SHADOW         = 0x0a0a0a;  // Deep shadow

    public static WritableImage bake(GameState s, int tile) {
        int w = s.width(), h = s.height();
        int imgW = w * tile, imgH = h * tile;
        WritableImage img = new WritableImage(imgW, imgH);
        PixelWriter px = img.getPixelWriter();

        double[][] elev = s.elevation;
        double[][] moist = s.moisture;
        boolean hasField = elev != null && moist != null && elev.length == h && elev[0].length == w;

        // Sun direction (from top-left, 45 deg elevation)
        double sunX = -0.707, sunY = -0.707, sunZ = 0.707;

        // First pass: base terrain
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                Terrain t = s.grid[ty][tx].type;
                for (int py = 0; py < tile; py++) {
                    for (int pxx = 0; pxx < tile; pxx++) {
                        double wx = tx + (pxx + 0.5) / tile;
                        double wy = ty + (py + 0.5) / tile;

                        int rgb;
                        if (t == Terrain.WATER || t == Terrain.FORD) {
                            rgb = shadeWater(wx, wy, hasField ? sample(elev, wx, wy) : 0.4);
                        } else if (t == Terrain.ROAD || t == Terrain.ROAD_VERT || t == Terrain.ROAD_CROSS) {
                            rgb = shadeRoad(tx, ty, pxx, py, tile, t);
                        } else if (t == Terrain.BUILDING) {
                            rgb = shadeBuilding(tx, ty, pxx, py, tile);
                        } else {
                            rgb = shadeLand(s, t, wx, wy, hasField, elev, moist, sunX, sunY, sunZ);
                        }
                        px.setArgb(tx * tile + pxx, ty * tile + py, 0xFF000000 | rgb);
                    }
                }
            }
        }

        // Second pass: detail overlays
        drawRoadDetail(img, s, tile);
        drawBuildingDetail(img, s, tile);
        drawVegetationDetail(img, s, tile);
        drawWaterDetail(img, s, tile);
        drawGlobalShadows(img, s, tile);
        drawAtmosphere(img);

        return img;
    }

    // ========== SAMPLING ==========
    private static double sample(double[][] f, double x, double y) {
        int w = f[0].length, h = f.length;
        double fx = Math.max(0, Math.min(w - 1.001, x));
        double fy = Math.max(0, Math.min(h - 1.001, y));
        int x0 = (int) fx, y0 = (int) fy;
        double sx = fx - x0, sy = fy - y0;
        double a = f[y0][x0], b = f[y0][x0 + 1], c = f[y0 + 1][x0], d = f[y0 + 1][x0 + 1];
        return (a * (1 - sx) + b * sx) * (1 - sy) + (c * (1 - sx) + d * sx) * sy;
    }

    // ========== WATER ==========
    private static int shadeWater(double wx, double wy, double e) {
        double depth = clamp01((0.35 - e) / 0.35);
        
        // Wave pattern
        double wave1 = Math.sin(wx * 1.5 + wy * 0.8) * 0.3;
        double wave2 = Math.sin(wx * 0.7 - wy * 1.3) * 0.2;
        double wave3 = Math.sin(wx * 2.1 + wy * 2.1) * 0.1;
        double wave = wave1 + wave2 + wave3;
        
        // Depth color
        int r = lerp(chan(SHALLOW_WATER, 0), chan(DEEP_WATER, 0), depth);
        int g = lerp(chan(SHALLOW_WATER, 1), chan(DEEP_WATER, 1), depth);
        int b = lerp(chan(SHALLOW_WATER, 2), chan(DEEP_WATER, 2), depth);
        
        // Specular highlights
        double spec = Math.max(0, Math.sin(wx * 3.0 + wy * 2.0) * 0.5 + 0.3) * (1.0 - depth) * 0.4;
        r = clamp255(r + (int)(spec * 60));
        g = clamp255(g + (int)(spec * 40));
        b = clamp255(b + (int)(spec * 20));
        
        return grade(r + (int)(wave * 8), g + (int)(wave * 5), b + (int)(wave * 3));
    }

    private static void drawWaterDetail(WritableImage img, GameState s, int tile) {
        var pr = img.getPixelReader();
        PixelWriter px = img.getPixelWriter();
        
        for (int y = 0; y < s.height(); y++) {
            for (int x = 0; x < s.width(); x++) {
                if (s.grid[y][x].type != Terrain.WATER) continue;
                
                // Foam at edges
                boolean nearLand = false;
                for (int dy = -1; dy <= 1 && !nearLand; dy++)
                    for (int dx = -1; dx <= 1; dx++)
                        if (x+dx >= 0 && x+dx < s.width() && y+dy >= 0 && y+dy < s.height())
                            if (s.grid[y+dy][x+dx].type != Terrain.WATER && s.grid[y+dy][x+dx].type != Terrain.FORD)
                                nearLand = true;
                
                if (nearLand) {
                    for (int py = 0; py < tile; py++) {
                        for (int pxx = 0; pxx < tile; pxx++) {
                            if (nextRand((x * 73856093L ^ y * 19349663L) + pxx * 1000 + py) < 0.02) {
                                int pxX = x * tile + pxx;
                                int pxY = y * tile + py;
                                int cur = pr.getArgb(pxX, pxY);
                                int nc = grade(clamp255(chan(cur,0)+40), clamp255(chan(cur,1)+40), clamp255(chan(cur,2)+30));
                                px.setArgb(pxX, pxY, 0xFF000000 | nc);
                            }
                        }
                    }
                }
            }
        }
    }

    // ========== ROADS ==========
    private static int shadeRoad(int tx, int ty, int pxx, int py, int tile, Terrain t) {
        boolean isHorizontal = (t == Terrain.ROAD || t == Terrain.ROAD_CROSS);
        boolean isVertical = (t == Terrain.ROAD_VERT || t == Terrain.ROAD_CROSS);
        
        int cx = tile / 2;
        double dist = 0;
        if (isHorizontal && isVertical) {
            dist = Math.min(Math.abs(pxx - cx), Math.abs(py - cx));
        } else if (isHorizontal) {
            dist = Math.abs(py - cx);
        } else if (isVertical) {
            dist = Math.abs(pxx - cx);
        }
        
        // Road surface width
        double roadWidth = tile * 0.35;
        double edgeFade = 1.0 - clamp01(dist / roadWidth);
        
        // Cracking/wear noise
        long seed = (tx * 73856093L ^ ty * 19349663L) + pxx * 1000 + py;
        double wear = nextRand(seed) * 0.15;
        
        int r = lerp(chan(ASPHALT_OLD, 0), chan(ASPHALT_NEW, 0), edgeFade) - (int)(wear * 30);
        int g = lerp(chan(ASPHALT_OLD, 1), chan(ASPHALT_NEW, 1), edgeFade) - (int)(wear * 30);
        int b = lerp(chan(ASPHALT_OLD, 2), chan(ASPHALT_NEW, 2), edgeFade) - (int)(wear * 30);
        
        // Center line (dashed)
        if (isHorizontal && Math.abs(py - cx) < 1) {
            double dash = Math.sin(pxx * 0.5) * 0.5 + 0.5;
            if (dash > 0.7 && dist < 2) {
                r = 200; g = 180; b = 40; // Yellow line
            }
        }
        if (isVertical && Math.abs(pxx - cx) < 1) {
            double dash = Math.sin(py * 0.5) * 0.5 + 0.5;
            if (dash > 0.7 && dist < 2) {
                r = 200; g = 180; b = 40;
            }
        }
        
        return grade(r, g, b);
    }

    private static void drawRoadDetail(WritableImage img, GameState s, int tile) {
        var pr = img.getPixelReader();
        PixelWriter px = img.getPixelWriter();
        
        for (int y = 0; y < s.height(); y++) {
            for (int x = 0; x < s.width(); x++) {
                Terrain t = s.grid[y][x].type;
                if (t != Terrain.ROAD && t != Terrain.ROAD_VERT && t != Terrain.ROAD_CROSS) continue;
                
                // Road edge dirt/grass transition
                for (int py = 0; py < tile; py++) {
                    for (int pxx = 0; pxx < tile; pxx++) {
                        boolean isHorizontal = (t == Terrain.ROAD || t == Terrain.ROAD_CROSS);
                        boolean isVertical = (t == Terrain.ROAD_VERT || t == Terrain.ROAD_CROSS);
                        int cx = tile / 2;
                        double dist = 0;
                        if (isHorizontal && isVertical) dist = Math.min(Math.abs(pxx - cx), Math.abs(py - cx));
                        else if (isHorizontal) dist = Math.abs(py - cx);
                        else if (isVertical) dist = Math.abs(pxx - cx);
                        
                        if (dist > tile * 0.32 && dist < tile * 0.42) {
                            int pxX = x * tile + pxx;
                            int pxY = y * tile + py;
                            int cur = pr.getArgb(pxX, pxY);
                            long seed = (x * 73856093L ^ y * 19349663L) + pxx * 1000 + py;
                            if (nextRand(seed) < 0.3) {
                                int nc = grade(clamp255(chan(cur,0)-20), clamp255(chan(cur,1)-20), clamp255(chan(cur,2)-20));
                                px.setArgb(pxX, pxY, 0xFF000000 | nc);
                            }
                        }
                    }
                }
            }
        }
    }

    // ========== BUILDINGS ==========
    private static int shadeBuilding(int tx, int ty, int pxx, int py, int tile) {
        int border = tile / 10;
        boolean isEdge = pxx < border || py < border || pxx >= tile - border || py >= tile - border;
        
        if (isEdge) return 0x3d3934; // Wall shadow
        
        long seed = (tx * 73856093L ^ ty * 19349663L);
        int roofType = (int)(nextRand(seed) * 4);
        int roofColor;
        switch (roofType) {
            case 0: roofColor = ROOF_RED; break;
            case 1: roofColor = ROOF_GRAY; break;
            case 2: roofColor = ROOF_BROWN; break;
            default: roofColor = ROOF_BLUE;
        }
        
        // Roof detail
        int cx = tile / 2, cy = tile / 2;
        int dx = pxx - cx, dy = py - cy;
        double distCenter = Math.sqrt(dx * dx + dy * dy) / (tile * 0.5);
        
        // Vent/AC unit
        int ventRad = tile / 6;
        if (Math.abs(dx) < ventRad && Math.abs(dy) < ventRad) {
            return 0x22211f;
        }
        
        // Roof slope shading
        double slope = (dy * 0.5 / (tile * 0.5)) * 0.2;
        int r = clamp255(chan(roofColor, 0) + (int)(slope * 30));
        int g = clamp255(chan(roofColor, 1) + (int)(slope * 30));
        int b = clamp255(chan(roofColor, 2) + (int)(slope * 30));
        
        // Texture noise
        double tex = (hash(pxx * 0.1, py * 0.1) - 0.5) * 15;
        return grade(r + (int)tex, g + (int)tex, b + (int)tex);
    }

    private static void drawBuildingDetail(WritableImage img, GameState s, int tile) {
        var pr = img.getPixelReader();
        PixelWriter px = img.getPixelWriter();
        
        for (int y = 0; y < s.height(); y++) {
            for (int x = 0; x < s.width(); x++) {
                if (s.grid[y][x].type != Terrain.BUILDING) continue;
                
                // Window reflections
                int cx = x * tile + tile / 2;
                int cy = y * tile + tile / 2;
                long seed = (x * 73856093L ^ y * 19349663L);
                
                for (int i = 0; i < 4; i++) {
                    int wx = cx - tile/3 + (i % 2) * tile * 2/3;
                    int wy = cy - tile/3 + (i / 2) * tile * 2/3;
                    if (nextRand(seed + i * 100) < 0.6) {
                        int rad = tile / 8;
                        fillCircle(pr, px, (int)img.getWidth(), (int)img.getHeight(),
                                   wx, wy, rad, 0x1a3a5a, 180); // Blue glass
                    }
                }
                
                // Building shadow (already in drawGlobalShadows)
            }
        }
    }

    // ========== LAND / VEGETATION ==========
    private static int shadeLand(GameState s, Terrain t, double wx, double wy,
                                  boolean hasField, double[][] elev, double[][] moist,
                                  double lx, double ly, double lz) {
        double e = hasField ? sample(elev, wx, wy) : 0.5;
        double m = hasField ? sample(moist, wx, wy) : 0.5;

        // Domain warping for natural variation
        double warpX = Math.sin(wx * 0.45 + 1.7) * 0.35 + Math.sin(wy * 0.31 + 4.2) * 0.25;
        double warpY = Math.cos(wy * 0.4 + 2.3) * 0.35 + Math.cos(wx * 0.27 + 0.9) * 0.25;
        double we = clamp01(e + warpX * 0.15);
        double wm = clamp01(m + warpY * 0.15);

        // Slope from elevation gradient
        double ex = sample(elev, wx + 0.5, wy) - sample(elev, wx - 0.5, wy);
        double ey = sample(elev, wx, wy + 0.5) - sample(elev, wx, wy - 0.5);
        double slope = Math.sqrt(ex * ex + ey * ey);
        double aspect = Math.atan2(ey, ex);

        // Base biome weights
        double grass = 0.0, dirt = 0.0, rock = 0.0, sand = 0.0, canopy = 0.0, farm = 0.0, scrub = 0.0, snow = 0.0;
        
        switch (t) {
            case FOREST -> { canopy = 0.8; dirt = 0.1; grass = 0.1; }
            case SCRUB -> { scrub = 0.7; grass = 0.2; dirt = 0.1; }
            case HILL -> { rock = 0.6; grass = 0.2; dirt = 0.2; }
            case RUIN -> { dirt = 0.6; rock = 0.2; grass = 0.1; scrub = 0.1; }
            case OPEN -> { grass = 0.6; dirt = 0.2; }
            default -> { grass = 0.5; dirt = 0.3; }
        }
        
        // Moisture influence
        grass += wm * 0.3;
        canopy += wm * 0.2;
        scrub += wm * 0.15;
        dirt += (1 - wm) * 0.4;
        
        // Elevation influence
        rock += Math.min(0.7, slope * 8.0);
        sand += Math.max(0, we - 0.65) * 0.9;
        if (e > 0.75) { rock += (e - 0.75) * 2.0; grass = 0; }
        if (e > 0.9) { snow = Math.min(1.0, (e - 0.9) * 5.0); }
        
        // Farmland patterns on open flat land
        if (t == Terrain.OPEN && slope < 0.15) {
            int fx = (int)(wx / 8.0);
            int fy = (int)(wy / 8.0);
            long fieldSeed = (long)(fx * 73856093L ^ fy * 19349663L);
            double fieldType = nextRand(fieldSeed);
            if (fieldType < 0.4) {
                farm = 0.7;
                grass = 0.1;
                dirt = 0.2;
            } else if (fieldType < 0.6) {
                farm = 0.5;
                dirt = 0.4;
                grass = 0.1;
            }
        }

        // Blend base colors
        int[] cols = {GRASS_GREEN, DIRT, ROCK_DARK, SAND_DRY, FOREST_CANOPY, FARMLAND_GREEN, SCRUB_DENSE, SNOW};
        double[] ws = {grass, dirt, rock, sand, canopy, farm, scrub, snow};
        double sum = 0;
        for (double w : ws) sum += w;
        sum = Math.max(sum, 1e-6);
        
        int r = 0, g = 0, b = 0;
        for (int i = 0; i < cols.length; i++) {
            r += chan(cols[i], 0) * ws[i];
            g += chan(cols[i], 1) * ws[i];
            b += chan(cols[i], 2) * ws[i];
        }
        r = (int)(r / sum); g = (int)(g / sum); b = (int)(b / sum);

        // Hill shading
        double nz = 1.0 / (1.0 + slope * 10.0);
        double nx = -ex * 6.0, ny = -ey * 6.0;
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz) + 1e-6;
        double shade = (nx * lx + ny * ly + nz * lz) / len;
        shade = 0.65 + 0.45 * clamp01(shade);
        
        // Micro-detail noise
        double grain = (hash(wx * 41.0, wy * 59.0) - 0.5) * 18.0;
        
        // Farmland row pattern
        if (farm > 0.3) {
            int fx = (int)(wx / 8.0);
            int fy = (int)(wy / 8.0);
            long fieldSeed = (long)(fx * 73856093L ^ fy * 19349663L);
            int orientation = (int)(nextRand(fieldSeed) * 2);
            double cropDensity = 5.0 + nextRand(fieldSeed + 1) * 8.0;
            double stripe = Math.sin((orientation == 0 ? wx : wy) * cropDensity) * 0.5 + 0.5;
            shade *= 0.85 + 0.2 * stripe;
        }
        
        r = (int)(r * shade + grain);
        g = (int)(g * shade + grain);
        b = (int)(b * shade + grain);
        
        return grade(r, g, b);
    }

    // ========== VEGETATION DETAIL ==========
    private static void drawVegetationDetail(WritableImage img, GameState s, int tile) {
        var pr = img.getPixelReader();
        PixelWriter px = img.getPixelWriter();
        
        for (int y = 0; y < s.height(); y++) {
            for (int x = 0; x < s.width(); x++) {
                Terrain t = s.grid[y][x].type;
                long seed = (x * 73856093L ^ y * 19349663L);
                
                if (t == Terrain.FOREST) {
                    drawForestTrees(img, s, x, y, tile, px, pr, seed);
                } else if (t == Terrain.SCRUB) {
                    drawScrubBushes(img, s, x, y, tile, px, pr, seed);
                } else if (t == Terrain.OPEN) {
                    // Occasional lone trees
                    if (nextRand(seed) < 0.02) {
                        int cx = x * tile + tile / 2;
                        int cy = y * tile + tile / 2;
                        int rad = tile / 3;
                        fillCircle(pr, px, (int)img.getWidth(), (int)img.getHeight(),
                                   cx, cy, rad, FOREST_CANOPY, 80);
                    }
                }
            }
        }
    }

private static void drawForestTrees(WritableImage img, GameState s, int tx, int ty, int tile,
                                          PixelWriter px, javafx.scene.image.PixelReader pr, long seed) {
        int cx = tx * tile + tile / 2;
        int cy = ty * tile + tile / 2;
        
        int treeCount = 4 + (int)(nextRand(seed) * 5);
        for (int k = 0; k < treeCount; k++) {
            double r = 0.12 + nextRand(seed + k * 101) * 0.25;
            double a = nextRand(seed + k * 201 + 17) * Math.PI * 2;
            int ox = (int)(Math.cos(a) * tile * 0.4);
            int oy = (int)(Math.sin(a) * tile * 0.4);
            int rad = (int)(tile * r);
            
            int treeType = (int)(nextRand(seed + k * 301) * 3);
            int color = (treeType == 0) ? FOREST_CANOPY : (treeType == 1 ? FOREST_DECID : 0x1a4a1a);
            int opacity = 60 + (int)(nextRand(seed + k * 401) * 40);
            
            // Tree shadow (slightly offset)
            int sx = cx + ox + (int)(tile * 0.08);
            int sy = cy + oy + (int)(tile * 0.08);
            fillCircle(pr, px, (int)img.getWidth(), (int)img.getHeight(),
                       sx, sy, rad, 0x000000, 40);
            
            // Tree canopy
            fillCircle(pr, px, (int)img.getWidth(), (int)img.getHeight(),
                       cx + ox, cy + oy, rad, color, opacity);
        }
    }

    private static void drawScrubBushes(WritableImage img, GameState s, int tx, int ty, int tile,
                                         PixelWriter px, javafx.scene.image.PixelReader pr, long seed) {
        int cx = tx * tile + tile / 2;
        int cy = ty * tile + tile / 2;
        
        int bushCount = 6 + (int)(nextRand(seed) * 8);
        for (int k = 0; k < bushCount; k++) {
            double r = 0.05 + nextRand(seed + k * 51) * 0.12;
            double a = nextRand(seed + k * 151 + 11) * Math.PI * 2;
            int ox = (int)(Math.cos(a) * tile * 0.45);
            int oy = (int)(Math.sin(a) * tile * 0.45);
            int rad = (int)(tile * r);
            
            fillCircle(pr, px, (int)img.getWidth(), (int)img.getHeight(),
                       cx + ox, cy + oy, rad, SCRUB_DENSE, 70);
        }
    }

    // ========== GLOBAL SHADOWS ==========
    private static void drawGlobalShadows(WritableImage img, GameState s, int tile) {
        var pr = img.getPixelReader();
        PixelWriter px = img.getPixelWriter();
        int w = (int)img.getWidth(), h = (int)img.getHeight();
        
        // Building shadows
        int shadowDx = (int)(tile * 0.18);
        int shadowDy = (int)(tile * 0.18);
        
        for (int y = 0; y < s.height(); y++) {
            for (int x = 0; x < s.width(); x++) {
                if (s.grid[y][x].type != Terrain.BUILDING) continue;
                
                int sx = x * tile + shadowDx;
                int sy = y * tile + shadowDy;
                
                int x0 = Math.max(0, sx), x1 = Math.min(w, sx + tile);
                int y0 = Math.max(0, sy), y1 = Math.min(h, sy + tile);
                
                for (int py = y0; py < y1; py++) {
                    for (int pxx = x0; pxx < x1; pxx++) {
                        int rx = pxx - x * tile;
                        int ry = py - y * tile;
                        if (rx >= 0 && rx < tile && ry >= 0 && ry < tile) continue;
                        
                        int cur = pr.getArgb(pxx, py);
                        double shadowStrength = 0.55 + 0.15 * hash(pxx * 0.01, py * 0.01);
                        int nr = clamp255((int)(chan(cur, 0) * (1.0 - shadowStrength)));
                        int ng = clamp255((int)(chan(cur, 1) * (1.0 - shadowStrength)));
                        int nb = clamp255((int)(chan(cur, 2) * (1.0 - shadowStrength)));
                        px.setArgb(pxx, py, 0xFF000000 | grade(nr, ng, nb));
                    }
                }
            }
        }
        
        // Tree shadows (for forest)
        for (int y = 0; y < s.height(); y++) {
            for (int x = 0; x < s.width(); x++) {
                if (s.grid[y][x].type != Terrain.FOREST) continue;
                
                int sx = x * tile + shadowDx;
                int sy = y * tile + shadowDy;
                int cx = x * tile + tile / 2;
                int cy = y * tile + tile / 2;
                long seed = (x * 73856093L ^ y * 19349663L);
                
                for (int k = 0; k < 3; k++) {
                    double r = 0.25 + nextRand(seed + k * 111) * 0.2;
                    double a = nextRand(seed + k * 211 + 19) * Math.PI * 2;
                    int ox = (int)(Math.cos(a) * tile * 0.35);
                    int oy = (int)(Math.sin(a) * tile * 0.35);
                    int rad = (int)(tile * r);
                    
                    int tsx = cx + ox + shadowDx + (int)(tile * 0.08);
                    int tsy = cy + oy + shadowDy + (int)(tile * 0.08);
                    
                    int x0 = Math.max(0, tsx - rad), x1 = Math.min(w, tsx + rad);
                    int y0 = Math.max(0, tsy - rad), y1 = Math.min(h, tsy + rad);
                    
                    for (int py = y0; py < y1; py++) {
                        for (int pxx = x0; pxx < x1; pxx++) {
                            int dx = pxx - tsx, dy = py - tsy;
                            if (dx * dx + dy * dy > rad * rad) continue;
                            
                            int cur = pr.getArgb(pxx, py);
                            double ss = 0.35 + 0.1 * hash(pxx * 0.01, py * 0.01);
                            int nr = clamp255((int)(chan(cur, 0) * (1.0 - ss)));
                            int ng = clamp255((int)(chan(cur, 1) * (1.0 - ss)));
                            int nb = clamp255((int)(chan(cur, 2) * (1.0 - ss)));
                            px.setArgb(pxx, py, 0xFF000000 | grade(nr, ng, nb));
                        }
                    }
                }
            }
        }
    }

    // ========== ATMOSPHERE ==========
    private static void drawAtmosphere(WritableImage img) {
        var pr = img.getPixelReader();
        PixelWriter px = img.getPixelWriter();
        int w = (int)img.getWidth(), h = (int)img.getHeight();
        
        // Haze/fog in valleys (low elevation)
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double nx = x * 0.002;
                double ny = y * 0.002;
                // Large scale cloud/haze pattern
                double haze = Math.sin(nx * 0.5) * Math.cos(ny * 0.5) * 0.5
                            + Math.sin(nx * 0.3 + 1.0) * Math.cos(ny * 0.4 - 0.5) * 0.3
                            + Math.sin(nx * 0.8 - 0.5) * Math.cos(ny * 0.6 + 1.0) * 0.2;
                haze = (haze + 1.0) * 0.5;
                
                if (haze > 0.85) {
                    double strength = Math.min(0.12, (haze - 0.85) * 0.6);
                    int cur = pr.getArgb(x, y);
                    // Bluish atmospheric haze
                    int nr = clamp255((int)(chan(cur, 0) * (1.0 - strength) + 180 * strength));
                    int ng = clamp255((int)(chan(cur, 1) * (1.0 - strength) + 190 * strength));
                    int nb = clamp255((int)(chan(cur, 2) * (1.0 - strength) + 210 * strength));
                    px.setArgb(x, y, 0xFF000000 | grade(nr, ng, nb));
                }
            }
        }
        
        // Vignette
        double cx = w * 0.5, cy = h * 0.5;
        double maxDist = Math.sqrt(cx * cx + cy * cy);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double dx = x - cx, dy = y - cy;
                double dist = Math.sqrt(dx * dx + dy * dy) / maxDist;
                if (dist > 0.7) {
                    double vignette = Math.min(0.15, (dist - 0.7) * 0.5);
                    int cur = pr.getArgb(x, y);
                    int nr = clamp255((int)(chan(cur, 0) * (1.0 - vignette)));
                    int ng = clamp255((int)(chan(cur, 1) * (1.0 - vignette)));
                    int nb = clamp255((int)(chan(cur, 2) * (1.0 - vignette)));
                    px.setArgb(x, y, 0xFF000000 | grade(nr, ng, nb));
                }
            }
        }
    }

    // ========== HELPERS ==========
    private static double nextRand(long s) {
        s = (s ^ (s >>> 33)) * 0xff51afd7ed558ccdL;
        s = (s ^ (s >>> 33)) * 0xc4ceb9fe1a85ec53L;
        s = s ^ (s >>> 33);
        return ((s >>> 11) & 0x1fffff) / (double) 0x1fffff;
    }

    private static double hash(double x, double y) {
        long h = (long)(x * 374761393L + y * 668265263L) & 0x7fffffffffffffffL;
        h = (h ^ (h >>> 30)) * 0xbf58476d1ce4e5b9L;
        h = (h ^ (h >>> 27)) * 0x94d049bb133111ebL;
        return ((h ^ (h >>> 31)) & 0xffffff) / (double) 0xffffff;
    }

    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }

    private static int lerp(int a, int b, double t) { return (int)(a + (b - a) * t); }

    private static int chan(int rgb, int i) { return (rgb >> (i * 8)) & 0xFF; }

    private static int grade(int r, int g, int b) {
        r = clamp255(r); g = clamp255(g); b = clamp255(b);
        // Desaturate slightly for aerial photo look
        int lum = (int)(0.299 * r + 0.587 * g + 0.114 * b);
        int sr = (int)(lum * 0.82 + r * 0.18);
        int sg = (int)(lum * 0.82 + g * 0.18);
        int sb = (int)(lum * 0.82 + b * 0.18);
        // Contrast adjustment
        sr = clamp255((int)((sr - 128) * 1.08 + 128));
        sg = clamp255((int)((sg - 128) * 1.08 + 128));
        sb = clamp255((int)((sb - 128) * 1.08 + 128));
        return (sr << 16) | (sg << 8) | sb;
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }

    private static void fillCircle(javafx.scene.image.PixelReader pr, PixelWriter px, int w, int h, 
                                    int cx, int cy, int rad, int rgb, int alpha) {
        int x0 = Math.max(0, cx - rad), x1 = Math.min(w, cx + rad);
        int y0 = Math.max(0, cy - rad), y1 = Math.min(h, cy + rad);
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int dx = x - cx, dy = y - cy;
                if (dx * dx + dy * dy > rad * rad) continue;
                
                double dist = Math.sqrt(dx * dx + dy * dy) / (double) rad;
                double angle = Math.atan2(dy, dx);
                double dot = Math.cos(angle - (-2.35));
                double shade = 1.0 + dot * 0.2 * dist;
                
                int targetR = clamp255((int)(chan(rgb, 0) * shade));
                int targetG = clamp255((int)(chan(rgb, 1) * shade));
                int targetB = clamp255((int)(chan(rgb, 2) * shade));
                
                int cur = pr.getArgb(x, y);
                int r = (chan(cur, 0) * (255 - alpha) + targetR * alpha) / 255;
                int g = (chan(cur, 1) * (255 - alpha) + targetG * alpha) / 255;
                int b = (chan(cur, 2) * (255 - alpha) + targetB * alpha) / 255;
                px.setArgb(x, y, 0xFF000000 | grade(r, g, b));
            }
        }
    }
}