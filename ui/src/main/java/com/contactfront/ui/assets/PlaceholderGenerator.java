package com.contactfront.ui.assets;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class PlaceholderGenerator {
    private PlaceholderGenerator() {}

    public static void generateTerrainTextures() throws IOException {
        Path root = AssetLoader.getAssetRoot();
        createPNG(root.resolve("terrain/grass_albedo.png"), 256, 256, 
            new Color(70, 120, 50), new Color(90, 140, 70), new Color(50, 100, 40));
        createPNG(root.resolve("terrain/grass_normal.png"), 256, 256, 
            new Color(128, 128, 255), new Color(128, 128, 255));
        createPNG(root.resolve("terrain/grass_roughness.png"), 256, 256, 192);
        
        createPNG(root.resolve("terrain/dirt_albedo.png"), 256, 256, 
            new Color(130, 100, 60), new Color(150, 120, 80), new Color(110, 80, 50));
        createPNG(root.resolve("terrain/dirt_normal.png"), 256, 256, 
            new Color(128, 128, 255), new Color(128, 128, 255));
        createPNG(root.resolve("terrain/dirt_roughness.png"), 256, 256, 160);
        
        createPNG(root.resolve("terrain/rock_albedo.png"), 256, 256, 
            new Color(120, 120, 120), new Color(140, 140, 140), new Color(100, 100, 100));
        createPNG(root.resolve("terrain/rock_normal.png"), 256, 256, 
            new Color(128, 128, 255), new Color(128, 128, 255));
        createPNG(root.resolve("terrain/rock_roughness.png"), 256, 256, 128);
        
        createPNG(root.resolve("terrain/water_albedo.png"), 256, 256, 
            new Color(30, 60, 120), new Color(40, 80, 160), new Color(20, 50, 100));
        createPNG(root.resolve("terrain/water_normal.png"), 256, 256, 
            new Color(128, 128, 255), new Color(128, 128, 255));
        createPNG(root.resolve("terrain/water_roughness.png"), 256, 256, 32);
    }

    public static void generateRoadTextures() throws IOException {
        Path root = AssetLoader.getAssetRoot();
        createPNG(root.resolve("roads/asphalt_albedo.png"), 256, 256, 
            new Color(50, 50, 50), new Color(70, 70, 70), new Color(40, 40, 40));
        createPNG(root.resolve("roads/asphalt_normal.png"), 256, 256, 
            new Color(128, 128, 255), new Color(128, 128, 255));
        createPNG(root.resolve("roads/asphalt_roughness.png"), 256, 256, 224);
    }

    public static void generateVegetationTextures() throws IOException {
        Path root = AssetLoader.getAssetRoot();
        createPNG(root.resolve("vegetation/tree_albedo.png"), 256, 256, 
            new Color(40, 70, 40), new Color(60, 90, 60), new Color(30, 60, 30));
        createPNG(root.resolve("vegetation/tree_normal.png"), 256, 256, 
            new Color(128, 128, 255), new Color(128, 128, 255));
        createPNG(root.resolve("vegetation/tree_roughness.png"), 256, 256, 160);
        
        createPNG(root.resolve("vegetation/bush_albedo.png"), 256, 256, 
            new Color(50, 80, 40), new Color(70, 100, 60), new Color(40, 70, 30));
        createPNG(root.resolve("vegetation/bush_normal.png"), 256, 256, 
            new Color(128, 128, 255), new Color(128, 128, 255));
        createPNG(root.resolve("vegetation/bush_roughness.png"), 256, 256, 176);
    }

    public static void generateBuildingTextures() throws IOException {
        Path root = AssetLoader.getAssetRoot();
        createPNG(root.resolve("buildings/building_albedo.png"), 256, 256, 
            new Color(100, 100, 100), new Color(120, 120, 120), new Color(80, 80, 80));
        createPNG(root.resolve("buildings/building_normal.png"), 256, 256, 
            new Color(128, 128, 255), new Color(128, 128, 255));
        createPNG(root.resolve("buildings/building_roughness.png"), 256, 256, 128);
    }

    private static void createPNG(Path path, int width, int height, Color... colors) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float fx = (float) x / width;
                float fy = (float) y / height;
                Color c = interpolateColors(fx, fy, colors);
                img.setRGB(x, y, c.getRGB());
            }
        }
        
        File out = path.toFile();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "PNG", out);
    }

    private static void createPNG(Path path, int width, int height, int gray) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Color c = new Color(gray, gray, gray);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, c.getRGB());
            }
        }
        
        File out = path.toFile();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "PNG", out);
    }

    private static Color interpolateColors(float fx, float fy, Color... colors) {
        if (colors.length == 1) return colors[0];
        if (colors.length == 2) {
            int r = (int)(colors[0].getRed() * (1 - fx) + colors[1].getRed() * fx);
            int g = (int)(colors[0].getGreen() * (1 - fy) + colors[1].getGreen() * fy);
            int b = (int)(colors[0].getBlue() * (1 - fx) + colors[1].getBlue() * fx);
            return new Color(r, g, b);
        }
        return colors[0];
    }
}