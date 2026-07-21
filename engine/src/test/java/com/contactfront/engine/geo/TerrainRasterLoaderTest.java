package com.contactfront.engine.geo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TerrainRasterLoaderTest {

    @Test
    public void testLoadMissingFileThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                TerrainRasterLoader.loadAndReproject("nonexistent.tif", "EPSG:3857"));
    }
}
