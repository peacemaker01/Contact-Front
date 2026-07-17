package com.contactfront.ui.assets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class AssetLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void testAssetManagerInitialization() throws Exception {
        AssetManager.initialize(tempDir.resolve("assets"));
        
        Material.PBRMaterial grass = AssetManager.getMaterial("grass");
        assertNotNull(grass);
        assertEquals("grass", grass.name());
        assertNotNull(grass.albedo());
        assertNotNull(grass.normal());
    }

    @Test
    void testMaterialCache() throws Exception {
        AssetManager.initialize(tempDir.resolve("assets"));
        
        Material.PBRMaterial m1 = AssetManager.getMaterial("dirt");
        Material.PBRMaterial m2 = AssetManager.getMaterial("dirt");
        
        assertSame(m1, m2);
    }

    @Test
    void testTextureLoading() throws Exception {
        AssetManager.initialize(tempDir.resolve("assets"));
        
        Texture.TextureData tex = AssetManager.getTexture("grass_albedo.png");
        assertNotNull(tex);
        assertTrue(tex.width() > 0);
        assertTrue(tex.height() > 0);
        assertTrue(tex.pixels().length > 0);
    }

    @Test
    void testModelLoading() throws Exception {
        AssetManager.initialize(tempDir.resolve("assets"));
        
        ModelLoader.Model model = AssetManager.getModel("tree.obj");
        assertNotNull(model);
        assertTrue(model.meshes().length > 0);
    }

    @Test
    void testPlaceholderGeneration() throws Exception {
        AssetManager.initialize(tempDir.resolve("assets"));
        
        Path grassFile = tempDir.resolve("assets/terrain/grass_albedo.png");
        assertTrue(java.nio.file.Files.exists(grassFile));
        
        Path asphaltFile = tempDir.resolve("assets/roads/asphalt_albedo.png");
        assertTrue(java.nio.file.Files.exists(asphaltFile));
    }

    @Test
    void testBushMaterial() throws Exception {
        AssetManager.initialize(tempDir.resolve("assets"));
        
        Material.PBRMaterial bush = AssetManager.getMaterial("bush");
        assertNotNull(bush);
        assertEquals("bush", bush.name());
    }

    @Test
    void testAsphaltMaterial() throws Exception {
        AssetManager.initialize(tempDir.resolve("assets"));
        
        Material.PBRMaterial road = AssetManager.getMaterial("asphalt");
        assertNotNull(road);
        assertEquals("asphalt", road.name());
    }
}