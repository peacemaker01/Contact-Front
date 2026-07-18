package com.contactfront.ui.assets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class AssetManager {
    private static final Map<String, Texture.TextureData> textureCache = new HashMap<>();

    private AssetManager() {}

    public static void initialize(Path assetRoot) throws IOException {
        AssetLoader.setAssetRoot(assetRoot);
        PlaceholderGenerator.generateTerrainTextures();
        PlaceholderGenerator.generateRoadTextures();
        PlaceholderGenerator.generateVegetationTextures();
        PlaceholderGenerator.generateBuildingTextures();
    }

    public static Texture.TextureData getTexture(String name) {
        return textureCache.computeIfAbsent(name, n -> {
            try {
                return AssetLoader.loadTexture(n);
            } catch (IOException e) {
                return Texture.createPlaceholder(n);
            }
        });
    }

    public static void clearCache() {
        textureCache.clear();
        AssetLoader.clearCache();
    }
}