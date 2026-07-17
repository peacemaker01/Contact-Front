package com.contactfront.ui.assets;

public final class Texture {
    private Texture() {}

    public record TextureData(int[] pixels, int width, int height, String path) {}

    public static TextureData createPlaceholder(String name) {
        int size = 256;
        int[] pixels = new int[size * size];
        
        for (int i = 0; i < pixels.length; i++) {
            if (name.contains("grass")) {
                pixels[i] = 0xFF467832;
            } else if (name.contains("dirt") || name.contains("road")) {
                pixels[i] = 0xFF82643C;
            } else if (name.contains("rock") || name.contains("hill")) {
                pixels[i] = 0xFF787878;
            } else if (name.contains("water")) {
                pixels[i] = 0xFF1E3C78;
            } else if (name.contains("bush") || name.contains("vegetation")) {
                pixels[i] = 0xFF325028;
            } else if (name.contains("building")) {
                pixels[i] = 0xFF646464;
            } else if (name.contains("tree") || name.contains("forest")) {
                pixels[i] = 0xFF284628;
            } else if (name.contains("asphalt")) {
                pixels[i] = 0xFF333333;
            } else {
                pixels[i] = 0xFF808080;
            }
        }
        
        return new TextureData(pixels, size, size, "placeholder:" + name);
    }

    public static int rgba(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int premultiply(int rgba) {
        int a = (rgba >> 24) & 0xFF;
        int r = ((rgba >> 16) & 0xFF) * a / 255;
        int g = ((rgba >> 8) & 0xFF) * a / 255;
        int b = (rgba & 0xFF) * a / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}