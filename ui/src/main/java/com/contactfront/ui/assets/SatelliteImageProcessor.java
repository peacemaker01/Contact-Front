package com.contactfront.ui.assets;

import com.contactfront.engine.model.Terrain;
import com.contactfront.ui.Log;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class SatelliteImageProcessor {
    private SatelliteImageProcessor() {}

    public static record SatelliteTerrainData(double[][] elevation, double[][] moisture, Terrain[][] terrain) {}

    public static SatelliteTerrainData processSatelliteImage(byte[] imageData, int targetWidth, int targetHeight) throws IOException {
        Log.info("SatelliteImageProcessor: Processing " + imageData.length + " bytes to " + targetWidth + "x" + targetHeight + " grid");
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
        
        int imgW = img.getWidth(), imgH = img.getHeight();
        Log.info("SatelliteImageProcessor: Image decoded " + imgW + "x" + imgH);
        
        double[][] elevation = new double[targetHeight][targetWidth];
        double[][] moisture = new double[targetHeight][targetWidth];
        Terrain[][] terrain = new Terrain[targetHeight][targetWidth];
        
        int waterCount = 0, hillCount = 0, openCount = 0;
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
                
                if (brightness < 0.15) { terrain[y][x] = Terrain.WATER; waterCount++; }
                else if (brightness > 0.7) { terrain[y][x] = Terrain.HILL; hillCount++; }
                else { terrain[y][x] = Terrain.OPEN; openCount++; }
            }
        }
        Log.info("SatelliteImageProcessor: Classification: " + waterCount + " water, " + hillCount + " hill, " + openCount + " open");
        
        return new SatelliteTerrainData(elevation, moisture, terrain);
    }
}