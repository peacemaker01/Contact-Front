package com.contactfront.engine.terrain;

import com.contactfront.engine.model.Terrain;

public final class SatelliteClassifier {
    private SatelliteClassifier() {}

    public static Terrain classifyFromSatellite(double elevation, double ndvi, double ndwi, double ndsi) {
        if (ndwi > 0.1) return Terrain.WATER;
        if (ndsi > 0.4) return Terrain.SNOW;
        if (ndvi > 0.6) return Terrain.FOREST;
        if (ndvi > 0.3) return Terrain.BUSH;
        if (ndvi > 0.15) return Terrain.SCRUB;
        if (elevation > 0.7) return Terrain.HILL;
        if (elevation > 0.4) return Terrain.DIRT;
        return Terrain.OPEN;
    }

    public static double calculateNDVI(double nir, double red) {
        if (nir + red == 0) return 0;
        return (nir - red) / (nir + red);
    }

    public static double calculateNDWI(double green, double nir) {
        if (green + nir == 0) return 0;
        return (green - nir) / (green + nir);
    }

    public static double calculateNDSI(double swir, double green) {
        if (swir + green == 0) return 0;
        return (green - swir) / (green + swir);
    }

    public static class SatelliteData {
        public final double[][] elevation;
        public final double[][] ndvi;
        public final double[][] ndwi;
        public final double[][] ndsi;

        public SatelliteData(double[][] elevation, double[][] ndvi, double[][] ndwi, double[][] ndsi) {
            this.elevation = elevation;
            this.ndvi = ndvi;
            this.ndwi = ndwi;
            this.ndsi = ndsi;
        }
    }

    public static Terrain[][] classifyMap(SatelliteData data) {
        int h = data.elevation.length;
        int w = data.elevation[0].length;
        Terrain[][] result = new Terrain[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                result[y][x] = classifyFromSatellite(
                    data.elevation[y][x],
                    data.ndvi[y][x],
                    data.ndwi[y][x],
                    data.ndsi[y][x]
                );
            }
        }
        return result;
    }
}