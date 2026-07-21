package com.contactfront.engine.geo;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;

public final class RangeCalculator {
    private RangeCalculator() {}

    static {
        System.setProperty("org.geotools.referencing.forceXY", "true");
    }

    private static volatile CoordinateReferenceSystem WGS84_CRS;
    private static volatile boolean initialized;

    public static synchronized void ensureInitialized() {
        if (initialized) return;
        try {
            WGS84_CRS = CRS.decode("EPSG:4326");
            initialized = true;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize GeoTools WGS84 CRS", e);
        }
    }

    public static double orthodromicDistance(Coordinate start, Coordinate end) {
        ensureInitialized();
        try {
            return JTS.orthodromicDistance(start, end, WGS84_CRS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute orthodromic distance", e);
        }
    }

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(rLat1) * Math.cos(rLat2)
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6378137.0 * c;
    }
}
