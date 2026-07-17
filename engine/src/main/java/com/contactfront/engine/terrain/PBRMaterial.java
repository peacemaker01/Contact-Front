package com.contactfront.engine.terrain;

import com.contactfront.engine.model.Terrain;

import java.util.List;

public final class PBRMaterial {
    private PBRMaterial() {}

    public record TextureSet(
        int[] diffusePixels,
        int[] normalPixels,
        int[] roughnessPixels,
        int width,
        int height
    ) {}

    public record BlendWeights(
        double grass,
        double dirt,
        double rock,
        double sand,
        double canopy,
        double farm,
        double scrub,
        double snow
    ) {
        public BlendWeights {
            double sum = grass + dirt + rock + sand + canopy + farm + scrub + snow;
            if (sum > 0) {
                grass = grass / sum;
                dirt = dirt / sum;
                rock = rock / sum;
                sand = sand / sum;
                canopy = canopy / sum;
                farm = farm / sum;
                scrub = scrub / sum;
                snow = snow / sum;
            }
        }
    }

    public static BlendWeights fromBiome(double moisture, double slope, double elevation) {
        double grass = 0.0, dirt = 0.0, rock = 0.0, sand = 0.0;
        double canopy = 0.0, farm = 0.0, scrub = 0.0, snow = 0.0;

        if (elevation > 0.9) {
            snow = 1.0;
        } else if (elevation > 0.75) {
            rock = 0.6;
            grass = 0.2;
            dirt = 0.2;
        } else {
            grass = 0.6;
            dirt = 0.2;
        }

        grass += moisture * 0.3;
        canopy += moisture * 0.2;
        scrub += moisture * 0.15;
        dirt += (1 - moisture) * 0.4;

        rock += Math.min(0.7, slope * 8.0);
        sand += Math.max(0, elevation - 0.65) * 0.9;

        if (slope < 0.15 && elevation < 0.75) {
            double fieldType = (Math.sin(0.45) * 0.35 + Math.cos(0.31 * 0.25 + 4.2) * 0.25 + 
                               Math.cos(0.27 * 0.15 + 0.9) * 0.25);
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

        return new BlendWeights(grass, dirt, rock, sand, canopy, farm, scrub, snow);
    }

    public static BlendWeights fromTerrain(int terrainCode) {
        switch (terrainCode) {
            case 0: return new BlendWeights(0.6, 0.2, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            case 1: return new BlendWeights(0.5, 0.3, 0.1, 0.0, 0.0, 0.0, 0.1, 0.0);
            case 2: return new BlendWeights(0.3, 0.2, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0);
            case 3: return new BlendWeights(0.1, 0.1, 0.6, 0.2, 0.0, 0.0, 0.0, 0.0);
            case 4: return new BlendWeights(0.1, 0.2, 0.3, 0.1, 0.2, 0.0, 0.2, 0.0);
            case 5: return new BlendWeights(0.4, 0.4, 0.1, 0.0, 0.0, 0.0, 0.0, 0.0);
            case 6: return new BlendWeights(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0);
            case 7: return new BlendWeights(0.3, 0.3, 0.2, 0.1, 0.0, 0.0, 0.0, 0.0);
            case 8: return new BlendWeights(0.2, 0.3, 0.1, 0.1, 0.0, 0.0, 0.1, 0.0);
            case 9: return new BlendWeights(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0);
            default: return new BlendWeights(0.5, 0.3, 0.1, 0.1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static int blendDiffuse(BlendWeights w, int[] grass, int[] dirt, int[] rock, 
                                   int[] sand, int[] canopy, int[] farm, int[] scrub, int[] snow) {
        int r = (int)(
            (getR(grass[graze(w.grass, grass.length)]) * w.grass) +
            (getR(dirt[graze(w.dirt, dirt.length)]) * w.dirt) +
            (getR(rock[graze(w.rock, rock.length)]) * w.rock) +
            (getR(sand[graze(w.sand, sand.length)]) * w.sand) +
            (getR(canopy[graze(w.canopy, canopy.length)]) * w.canopy) +
            (getR(farm[graze(w.farm, farm.length)]) * w.farm) +
            (getR(scrub[graze(w.scrub, scrub.length)]) * w.scrub) +
            (getR(snow[graze(w.snow, snow.length)]) * w.snow)
        );
        int g = (int)(
            (getG(grass[graze(w.grass, grass.length)]) * w.grass) +
            (getG(dirt[graze(w.dirt, dirt.length)]) * w.dirt) +
            (getG(rock[graze(w.rock, rock.length)]) * w.rock) +
            (getG(sand[graze(w.sand, sand.length)]) * w.sand) +
            (getG(canopy[graze(w.canopy, canopy.length)]) * w.canopy) +
            (getG(farm[graze(w.farm, farm.length)]) * w.farm) +
            (getG(scrub[graze(w.scrub, scrub.length)]) * w.scrub) +
            (getG(snow[graze(w.snow, snow.length)]) * w.snow)
        );
        int b = (int)(
            (getB(grass[graze(w.grass, grass.length)]) * w.grass) +
            (getB(dirt[graze(w.dirt, dirt.length)]) * w.dirt) +
            (getB(rock[graze(w.rock, rock.length)]) * w.rock) +
            (getB(sand[graze(w.sand, sand.length)]) * w.sand) +
            (getB(canopy[graze(w.canopy, canopy.length)]) * w.canopy) +
            (getB(farm[graze(w.farm, farm.length)]) * w.farm) +
            (getB(scrub[graze(w.scrub, scrub.length)]) * w.scrub) +
            (getB(snow[graze(w.snow, snow.length)]) * w.snow)
        );
        return (r << 16) | (g << 8) | b;
    }

    public static int blendNormal(BlendWeights w, int[] grass, int[] dirt, int[] rock,
                                  int[] sand, int[] canopy, int[] farm, int[] scrub, int[] snow) {
        return blendDiffuse(w, grass, dirt, rock, sand, canopy, farm, scrub, snow);
    }

    public static int blendRoughness(BlendWeights w, int[] grass, int[] dirt, int[] rock,
                                     int[] sand, int[] canopy, int[] farm, int[] scrub, int[] snow) {
        return blendDiffuse(w, grass, dirt, rock, sand, canopy, farm, scrub, snow);
    }

    private static int graze(double weight, int length) {
        return (int)(weight * (length - 1));
    }

    private static int getR(int rgb) { return (rgb >> 16) & 0xFF; }
    private static int getG(int rgb) { return (rgb >> 8) & 0xFF; }
    private static int getB(int rgb) { return rgb & 0xFF; }

    public static List<TerrainMaterial> getTerrainMaterials() {
        return List.of(
            new TerrainMaterial(Terrain.OPEN, "grass", 0.8, 0.7, 0.9),
            new TerrainMaterial(Terrain.SCRUB, "scrub", 0.7, 0.5, 0.8),
            new TerrainMaterial(Terrain.BUSH, "bush", 0.6, 0.6, 0.7),
            new TerrainMaterial(Terrain.FOREST, "forest", 0.6, 0.4, 0.7),
            new TerrainMaterial(Terrain.HILL, "rock", 0.9, 0.3, 0.6),
            new TerrainMaterial(Terrain.RUIN, "ruin", 0.85, 0.4, 0.5),
            new TerrainMaterial(Terrain.BUILDING, "building", 0.95, 0.1, 0.3),
            new TerrainMaterial(Terrain.ROAD, "asphalt", 0.0, 0.9, 0.1),
            new TerrainMaterial(Terrain.ROAD_VERT, "asphalt", 0.0, 0.9, 0.1),
            new TerrainMaterial(Terrain.ROAD_CROSS, "asphalt", 0.0, 0.9, 0.1),
            new TerrainMaterial(Terrain.WATER, "water", 0.1, 0.95, 0.05),
            new TerrainMaterial(Terrain.FORD, "ford", 0.15, 0.8, 0.1),
            new TerrainMaterial(Terrain.CHECKPOINT, "dirt", 0.5, 0.6, 0.7),
            new TerrainMaterial(Terrain.CRATER, "dirt", 0.5, 0.6, 0.7),
            new TerrainMaterial(Terrain.OBJECTIVE, "objective", 0.7, 0.5, 0.8),
            new TerrainMaterial(Terrain.FIRE, "fire", 0.9, 0.1, 0.2),
            new TerrainMaterial(Terrain.MINEFIELD, "mine", 0.4, 0.8, 0.6),
            new TerrainMaterial(Terrain.DIRT, "dirt", 0.5, 0.6, 0.7),
            new TerrainMaterial(Terrain.SNOW, "snow", 0.1, 0.9, 0.5)
        );
    }

    public record TerrainMaterial(Terrain terrain, String albedoMap, double metallic, double roughness, double ao) {}
}