package com.contactfront.engine.geo;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Geometry;

public final class CoordinateConverter {
    private CoordinateConverter() {}

    private static volatile MathTransform WGS84_TO_3857;
    private static volatile MathTransform METRIC_TO_WGS84;
    private static volatile boolean initialized;

    public static synchronized void ensureInitialized() {
        if (initialized) return;
        try {
            Hints longitudeFirst = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326", longitudeFirst);
            CoordinateReferenceSystem mercator = CRS.decode("EPSG:3857", longitudeFirst);
            WGS84_TO_3857 = CRS.findMathTransform(wgs84, mercator, true);
            METRIC_TO_WGS84 = WGS84_TO_3857.inverse();
            initialized = true;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize GeoTools CRS transforms", e);
        }
    }

    public static Geometry projectToMetric(Geometry wgs84Geometry) {
        ensureInitialized();
        try {
            return JTS.transform(wgs84Geometry, WGS84_TO_3857);
        } catch (Exception e) {
            throw new RuntimeException("Failed to project geometry to EPSG:3857", e);
        }
    }

    public static Geometry unprojectToWgs84(Geometry metricGeometry) {
        ensureInitialized();
        try {
            return JTS.transform(metricGeometry, METRIC_TO_WGS84);
        } catch (Exception e) {
            throw new RuntimeException("Failed to unproject geometry to EPSG:4326", e);
        }
    }
}
