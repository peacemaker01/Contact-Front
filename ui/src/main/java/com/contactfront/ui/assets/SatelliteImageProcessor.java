package com.contactfront.ui.assets;

import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.terrain.SatelliteClassifier;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class SatelliteImageProcessor {
    private SatelliteImageProcessor() {}

    public static record SatelliteTerrainData(double[][] elevation, double[][] moisture, Terrain[][] terrain) {}

    public static SatelliteTerrainData processSatelliteImage(byte[] imageData, int targetWidth, int targetHeight) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
        
        // Sample image to target dimensions
        double[][] elevation = new double[targetHeight][targetWidth];
        double[][] moisture = new double[targetHeight][targetWidth];
        double[][] ndvi = new double[targetHeight][targetWidth];
        double[][] ndwi = new double[targetHeight][targetWidth];
        double[][] ndsi = new double[targetHeight][targetWidth];
        
        int imgW = img.getWidth();
        int imgH = img.getHeight();
        
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int sx = (x * imgW) / targetWidth;
                int sy = (y * imgH) / targetHeight;
                int rgb = img.getRGB(sx, sy) & 0xFFFFFF;
                
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // Extract features from RGB:
                // - Bright surfaces (roads, buildings) → low vegetation
                // - Dark areas → water or dense vegetation
                // - Gray surfaces → bare terrain/dirt
                
                double brightness = (r + g + b) / 3.0 / 255.0;
                double nir = (r + g) / 2.0 / 255.0; // Approximate NIR
                double red = r / 255.0;
                
                // Elevation: lighter = higher, darker = lower
                elevation[y][x] = brightness;
                
                // Moisture: blue channel dominance indicates water/moisture
                moisture[y][x] = b / 255.0;
                
                // NDVI approximation
                ndvi[y][x] = SatelliteClassifier.calculateNDVI(nir, red);
                
                // NDWI approximation
                ndwi[y][x] = SatelliteClassifier.calculateNDWI(g / 255.0, nir);
                
                // NDSI approximation
                ndsi[y][x] = SatelliteClassifier.calculateNDSI(nir, g / 255.0);
            }
        }
        
        // Classify terrain using the extracted indices
        SatelliteClassifier.SatelliteData data = new SatelliteClassifier.SatelliteData(elevation, ndvi, ndwi, ndsi);
        Terrain[][] terrain = SatelliteClassifier.classifyMap(data);
        
        return new SatelliteTerrainData(elevation, moisture, terrain);
    }
}