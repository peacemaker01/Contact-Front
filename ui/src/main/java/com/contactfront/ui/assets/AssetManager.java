package com.contactfront.ui.assets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class AssetManager {
    private static final Map<String, Material.PBRMaterial> materialCache = new HashMap<>();
    private static final Map<String, ModelLoader.Model> modelCache = new HashMap<>();
    private static final Map<String, Texture.TextureData> textureCache = new HashMap<>();

    private AssetManager() {}

    public static void initialize(Path assetRoot) throws IOException {
        AssetLoader.setAssetRoot(assetRoot);
        PlaceholderGenerator.generateTerrainTextures();
        PlaceholderGenerator.generateRoadTextures();
        PlaceholderGenerator.generateVegetationTextures();
        PlaceholderGenerator.generateBuildingTextures();
    }

    public static Material.PBRMaterial getMaterial(String name) {
        return materialCache.computeIfAbsent(name, n -> {
            try {
                if (n.equals("grass")) return Material.createGrass();
                if (n.equals("dirt")) return Material.createDirt();
                if (n.equals("rock")) return Material.createRock();
                if (n.equals("water")) return Material.createWater();
                if (n.equals("asphalt") || n.equals("road")) return Material.createAsphalt();
                if (n.equals("building")) return Material.createBuilding();
                if (n.equals("tree")) return Material.createTree();
                if (n.equals("bush")) return Material.createBush();
                
                Texture.TextureData albedo = AssetLoader.loadTexture(n + "_albedo.png");
                Texture.TextureData normal = AssetLoader.loadTexture(n + "_normal.png");
                return new Material.PBRMaterial(n, albedo, normal, 0.0, 0.5, 0.0);
            } catch (IOException e) {
                return Material.createPlaceholder(n);
            }
        });
    }

    public static ModelLoader.Model getModel(String name) {
        return modelCache.computeIfAbsent(name, n -> {
            try {
                if (n.endsWith(".obj")) {
                    return ModelLoader.loadModel(n);
                }
                if (n.endsWith(".glb") || n.endsWith(".gltf")) {
                    return convertToModel(GLTFLoader.loadGLTF(n));
                }
                return ModelLoader.loadModel(n + ".obj");
            } catch (IOException e) {
                return ModelLoader.createPlaceholderModel(n);
            }
        });
    }

    private static ModelLoader.Model convertToModel(GLTFLoader.GLTFModel gltf) {
        return new ModelLoader.Model(new ModelLoader.MeshData[0], gltf.name());
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
        materialCache.clear();
        modelCache.clear();
        textureCache.clear();
        AssetLoader.clearCache();
    }
}