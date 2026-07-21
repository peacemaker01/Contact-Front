package com.contactfront.engine.geo;

import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.List;

public final class UnitFeatureManager {
    private UnitFeatureManager() {}

    private static final String SCHEMA_DEFINITION = "position:Point:srid=4326,unitId:String,sidc:String,faction:String,heading:Double";

    public static SimpleFeatureType createUnitSchema() {
        try {
            return DataUtilities.createType("TacticalUnit", SCHEMA_DEFINITION);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create TacticalUnit schema", e);
        }
    }

    public static MemoryDataStore createMemoryStore(SimpleFeatureType schema) {
        MemoryDataStore store = new MemoryDataStore();
        try {
            store.createSchema(schema);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create MemoryDataStore schema", e);
        }
        return store;
    }

    public static SimpleFeature buildFeature(SimpleFeatureType schema, String id, String sidc, String faction, double lat, double lon, double heading) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Point geomPoint = geometryFactory.createPoint(new Coordinate(lon, lat));
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);
        builder.add(geomPoint);
        builder.add(id);
        builder.add(sidc);
        builder.add(faction);
        builder.add(heading);
        return builder.buildFeature(id);
    }

    public static void addUnit(MemoryDataStore store, SimpleFeature feature) {
        try {
            store.addFeature(feature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add feature to MemoryDataStore", e);
        }
    }

    public static List<SimpleFeature> getFeatures(MemoryDataStore store) {
        List<SimpleFeature> features = new ArrayList<>();
        try {
            String typeName = store.getTypeNames()[0];
            SimpleFeatureStore featureStore = (SimpleFeatureStore) store.getFeatureSource(typeName);
            try (var iter = featureStore.getFeatures().features()) {
                while (iter.hasNext()) {
                    features.add(iter.next());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to query features from MemoryDataStore", e);
        }
        return features;
    }
}
