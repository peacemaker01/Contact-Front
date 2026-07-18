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
    void testPlaceholderGeneration() throws Exception {
        AssetManager.initialize(tempDir.resolve("assets"));
        
        Path grassFile = tempDir.resolve("assets/terrain/grass_albedo.png");
        assertTrue(java.nio.file.Files.exists(grassFile));
        
        Path asphaltFile = tempDir.resolve("assets/roads/asphalt_albedo.png");
        assertTrue(java.nio.file.Files.exists(asphaltFile));
    }

    @Test
    void testTextureCache() throws Exception {
        AssetManager.initialize(tempDir.resolve("assets"));
        
        Texture.TextureData t1 = AssetManager.getTexture("grass_albedo.png");
        Texture.TextureData t2 = AssetManager.getTexture("grass_albedo.png");
        
        assertSame(t1, t2);
    }
}