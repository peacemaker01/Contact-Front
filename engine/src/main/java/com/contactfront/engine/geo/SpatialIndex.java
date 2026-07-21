package com.contactfront.engine.geo;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;

public final class SpatialIndex {
    private SpatialIndex() {}

    static {
        System.setProperty("org.geotools.referencing.forceXY", "true");
    }

    private static volatile boolean initialized;
    private static volatile CoordinateReferenceSystem WGS84_CRS;
    private static volatile MathTransform WGS84_TO_3857;
    private static volatile MathTransform METRIC_TO_WGS84;

    public static synchronized void ensureInitialized() {
        if (initialized) return;
        try {
            WGS84_CRS = CRS.decode("EPSG:4326");
            CoordinateReferenceSystem mercator = CRS.decode("EPSG:3857");
            WGS84_TO_3857 = CRS.findMathTransform(WGS84_CRS, mercator, true);
            METRIC_TO_WGS84 = WGS84_TO_3857.inverse();
            initialized = true;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize SpatialIndex CRS", e);
        }
    }

    public static STRtree buildIndex(Iterable<? extends Geometry> wgs84Geometries) {
        ensureInitialized();
        STRtree tree = new STRtree();
        try {
            for (Geometry geom : wgs84Geometries) {
                if (geom == null || geom.isEmpty()) continue;
                Geometry projected = JTS.transform(geom, WGS84_TO_3857);
                tree.insert(projected.getEnvelopeInternal(), projected);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to build spatial index", e);
        }
        tree.build();
        return tree;
    }

    public static Envelope projectEnvelope(Envelope wgs84Envelope) {
        ensureInitialized();
        try {
            Geometry geom = new org.locationtech.jts.geom.GeometryFactory().toGeometry(wgs84Envelope);
            Geometry projected = JTS.transform(geom, WGS84_TO_3857);
            return projected.getEnvelopeInternal();
        } catch (Exception e) {
            throw new RuntimeException("Failed to project envelope", e);
        }
    }
}
