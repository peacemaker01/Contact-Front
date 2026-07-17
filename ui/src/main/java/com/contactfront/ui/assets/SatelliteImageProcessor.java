package com.contactfront.ui.assets;

import com.contactfront.engine.model.Terrain;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class SatelliteImageProcessor {
    private SatelliteImageProcessor() {}

    public static record SatelliteTerrainData(double[][] elevation, double[][] moisture, Terrain[][] terrain) {}

    public static SatelliteTerrainData processSatelliteImage(byte[] imageData, int targetWidth, int targetHeight) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
        
        double[][] elevation = new double[targetHeight][targetWidth];
        double[][] moisture = new double[targetHeight][targetWidth];
        Terrain[][] terrain = new Terrain[targetHeight][targetWidth];
        
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
                
                double brightness = (r + g + b) / 3.0 / 255.0;
                
                elevation[y][x] = brightness;
                moisture[y][x] = b / 255.0;
                
                // Simple classification - roads/buildings require OSM data
                if (brightness < 0.15) terrain[y][x] = Terrain.WATER;
                else if (brightness > 0.7) terrain[y][x] = Terrain.HILL;
                else terrain[y][x] = Terrain.OPEN;
            }
        }
        
        return new SatelliteTerrainData(elevation, moisture, terrain);
    }
}