package com.contactfront.engine.geo;

import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UnitFeatureManagerTest {

    @Test
    public void testCreateSchema() {
        SimpleFeatureType schema = UnitFeatureManager.createUnitSchema();
        assertEquals("TacticalUnit", schema.getTypeName());
        assertEquals(5, schema.getAttributeCount());
    }

    @Test
    public void testBuildAndStoreFeature() {
        SimpleFeatureType schema = UnitFeatureManager.createUnitSchema();
        var store = UnitFeatureManager.createMemoryStore(schema);
        SimpleFeature feature = UnitFeatureManager.buildFeature(schema, "u1", "SFGPUCI", "USA", 37.7749, -122.4194, 45.0);
        assertEquals("u1", feature.getAttribute("unitId"));
        assertEquals("SFGPUCI", feature.getAttribute("sidc"));
        assertEquals("USA", feature.getAttribute("faction"));
        assertEquals(45.0, feature.getAttribute("heading"));
        UnitFeatureManager.addUnit(store, feature);
        List<SimpleFeature> features = UnitFeatureManager.getFeatures(store);
        assertEquals(1, features.size());
        assertEquals("u1", features.get(0).getAttribute("unitId"));
    }

    @Test
    public void testMultipleFeatures() {
        SimpleFeatureType schema = UnitFeatureManager.createUnitSchema();
        var store = UnitFeatureManager.createMemoryStore(schema);
        UnitFeatureManager.addUnit(store, UnitFeatureManager.buildFeature(schema, "u1", "SFGPUCI", "USA", 37.7749, -122.4194, 0.0));
        UnitFeatureManager.addUnit(store, UnitFeatureManager.buildFeature(schema, "u2", "SFGPUCI", "USA", 37.7750, -122.4190, 90.0));
        List<SimpleFeature> features = UnitFeatureManager.getFeatures(store);
        assertEquals(2, features.size());
    }
}
