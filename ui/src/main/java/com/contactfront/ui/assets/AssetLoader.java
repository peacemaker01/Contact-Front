package com.contactfront.ui.assets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;

public final class AssetLoader {
    private static Map<String, Texture.TextureData> cache = new HashMap<>();
    private static Path assetRoot = Path.of("assets");

    private AssetLoader() {}

    public static void setAssetRoot(Path root) {
        assetRoot = root;
    }

    public static Path getAssetRoot() {
        return assetRoot;
    }

    public static Texture.TextureData loadTexture(String name) throws IOException {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }

        Path file = assetRoot.resolve(name);
        Texture.TextureData data = null;

        if (Files.exists(file)) {
            data = loadFromFile(file);
        } else {
            InputStream is = AssetLoader.class.getResourceAsStream("/assets/" + name);
            if (is != null) {
                data = loadFromResource(is);
            }
        }

        if (data == null) {
            data = createPlaceholderTexture(name);
        }

        cache.put(name, data);
        return data;
    }

    private static Texture.TextureData loadFromFile(Path file) throws IOException {
        BufferedImage img = ImageIO.read(file.toFile());
        return imageToTextureData(img, file.toString());
    }

    private static Texture.TextureData loadFromResource(InputStream is) throws IOException {
        BufferedImage img = ImageIO.read(is);
        return imageToTextureData(img, "resource:" + is.toString());
    }

    private static Texture.TextureData imageToTextureData(BufferedImage img, String path) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] pixels = new int[w * h];
        img.getRGB(0, 0, w, h, pixels, 0, w);
        return new Texture.TextureData(pixels, w, h, path);
    }

    private static Texture.TextureData createPlaceholderTexture(String name) {
        int size = 64;
        int[] pixels = new int[size * size];

        if (name.contains("grass")) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = new Color(70, 120, 50).getRGB();
            }
        } else if (name.contains("dirt") || name.contains("road")) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = new Color(130, 100, 60).getRGB();
            }
        } else if (name.contains("rock") || name.contains("hill")) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = new Color(120, 120, 120).getRGB();
            }
        } else if (name.contains("water")) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = new Color(30, 60, 120).getRGB();
            }
        } else if (name.contains("bush") || name.contains("vegetation")) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = new Color(50, 80, 40).getRGB();
            }
        } else if (name.contains("building")) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = new Color(100, 100, 100).getRGB();
            }
        } else if (name.contains("tree") || name.contains("forest")) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = new Color(40, 70, 40).getRGB();
            }
        } else {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = new Color(128, 128, 128).getRGB();
            }
        }

        return new Texture.TextureData(pixels, size, size, "placeholder:" + name);
    }

    public static void clearCache() {
        cache.clear();
    }
}