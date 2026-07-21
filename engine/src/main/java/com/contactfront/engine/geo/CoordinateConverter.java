package com.contactfront.engine.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

public final class CoordinateConverter {
    private CoordinateConverter() {}

    private static final double EARTH_RADIUS = 6378137.0;
    private static final double MAX_LAT = 85.0511287798;
    private static final double MAX_MERCATOR = 20037508.342789244;

    public static Geometry projectToMetric(Geometry wgs84Geometry) {
        if (wgs84Geometry == null || wgs84Geometry.isEmpty()) return wgs84Geometry;
        GeometryFactory factory = wgs84Geometry.getFactory();
        Coordinate[] src = wgs84Geometry.getCoordinates();
        Coordinate[] projected = new Coordinate[src.length];
        for (int i = 0; i < src.length; i++) {
            projected[i] = projectPoint(src[i]);
        }
        if (wgs84Geometry instanceof Point) {
            return factory.createPoint(projected[0]);
        } else if (wgs84Geometry instanceof LineString) {
            return factory.createLineString(projected);
        } else if (wgs84Geometry instanceof org.locationtech.jts.geom.Polygon polygon) {
            org.locationtech.jts.geom.LinearRing shell = factory.createLinearRing(projected);
            int holes = polygon.getNumInteriorRing();
            org.locationtech.jts.geom.LinearRing[] holeRings = new org.locationtech.jts.geom.LinearRing[holes];
            for (int h = 0; h < holes; h++) {
                Coordinate[] holeCoords = polygon.getInteriorRingN(h).getCoordinates();
                Coordinate[] holeProjected = new Coordinate[holeCoords.length];
                for (int i = 0; i < holeCoords.length; i++) {
                    holeProjected[i] = projectPoint(holeCoords[i]);
                }
                holeRings[h] = factory.createLinearRing(holeProjected);
            }
            return factory.createPolygon(shell, holeRings);
        } else if (wgs84Geometry instanceof org.locationtech.jts.geom.MultiPoint mp) {
            return factory.createMultiPoint(projected);
        } else if (wgs84Geometry instanceof org.locationtech.jts.geom.MultiLineString mls) {
            int n = mls.getNumGeometries();
            LineString[] lines = new LineString[n];
            for (int i = 0; i < n; i++) {
                LineString ls = (LineString) mls.getGeometryN(i);
                Coordinate[] lsProjected = new Coordinate[ls.getCoordinates().length];
                for (int j = 0; j < lsProjected.length; j++) {
                    lsProjected[j] = projectPoint(ls.getCoordinates()[j]);
                }
                lines[i] = factory.createLineString(lsProjected);
            }
            return factory.createMultiLineString(lines);
        } else if (wgs84Geometry instanceof org.locationtech.jts.geom.MultiPolygon mp) {
            int n = mp.getNumGeometries();
            org.locationtech.jts.geom.Polygon[] polys = new org.locationtech.jts.geom.Polygon[n];
            for (int i = 0; i < n; i++) {
                polys[i] = (org.locationtech.jts.geom.Polygon) projectToMetric(mp.getGeometryN(i));
            }
            return factory.createMultiPolygon(polys);
        } else if (wgs84Geometry instanceof org.locationtech.jts.geom.GeometryCollection gc) {
            int n = gc.getNumGeometries();
            Geometry[] geoms = new Geometry[n];
            for (int i = 0; i < n; i++) {
                geoms[i] = projectToMetric(gc.getGeometryN(i));
            }
            return factory.createGeometryCollection(geoms);
        }
        return wgs84Geometry.copy();
    }

    public static Geometry unprojectToWgs84(Geometry metricGeometry) {
        if (metricGeometry == null || metricGeometry.isEmpty()) return metricGeometry;
        GeometryFactory factory = metricGeometry.getFactory();
        Coordinate[] src = metricGeometry.getCoordinates();
        Coordinate[] unprojected = new Coordinate[src.length];
        for (int i = 0; i < src.length; i++) {
            unprojected[i] = unprojectPoint(src[i]);
        }
        if (metricGeometry instanceof Point) {
            return factory.createPoint(unprojected[0]);
        } else if (metricGeometry instanceof LineString) {
            return factory.createLineString(unprojected);
        } else if (metricGeometry instanceof org.locationtech.jts.geom.Polygon polygon) {
            org.locationtech.jts.geom.LinearRing shell = factory.createLinearRing(unprojected);
            int holes = polygon.getNumInteriorRing();
            org.locationtech.jts.geom.LinearRing[] holeRings = new org.locationtech.jts.geom.LinearRing[holes];
            for (int h = 0; h < holes; h++) {
                Coordinate[] holeCoords = polygon.getInteriorRingN(h).getCoordinates();
                Coordinate[] holeUnprojected = new Coordinate[holeCoords.length];
                for (int i = 0; i < holeCoords.length; i++) {
                    holeUnprojected[i] = unprojectPoint(holeCoords[i]);
                }
                holeRings[h] = factory.createLinearRing(holeUnprojected);
            }
            return factory.createPolygon(shell, holeRings);
        } else if (metricGeometry instanceof org.locationtech.jts.geom.MultiPoint mp) {
            return factory.createMultiPoint(unprojected);
        } else if (metricGeometry instanceof org.locationtech.jts.geom.MultiLineString mls) {
            int n = mls.getNumGeometries();
            LineString[] lines = new LineString[n];
            for (int i = 0; i < n; i++) {
                LineString ls = (LineString) mls.getGeometryN(i);
                Coordinate[] lsUnprojected = new Coordinate[ls.getCoordinates().length];
                for (int j = 0; j < lsUnprojected.length; j++) {
                    lsUnprojected[j] = unprojectPoint(ls.getCoordinates()[j]);
                }
                lines[i] = factory.createLineString(lsUnprojected);
            }
            return factory.createMultiLineString(lines);
        } else if (metricGeometry instanceof org.locationtech.jts.geom.MultiPolygon mp) {
            int n = mp.getNumGeometries();
            org.locationtech.jts.geom.Polygon[] polys = new org.locationtech.jts.geom.Polygon[n];
            for (int i = 0; i < n; i++) {
                polys[i] = (org.locationtech.jts.geom.Polygon) unprojectToWgs84(mp.getGeometryN(i));
            }
            return factory.createMultiPolygon(polys);
        } else if (metricGeometry instanceof org.locationtech.jts.geom.GeometryCollection gc) {
            int n = gc.getNumGeometries();
            Geometry[] geoms = new Geometry[n];
            for (int i = 0; i < n; i++) {
                geoms[i] = unprojectToWgs84(gc.getGeometryN(i));
            }
            return factory.createGeometryCollection(geoms);
        }
        return metricGeometry.copy();
    }

    private static Coordinate projectPoint(Coordinate lonLat) {
        double lon = lonLat.x;
        double lat = Math.max(-MAX_LAT, Math.min(MAX_LAT, lonLat.y));
        double x = EARTH_RADIUS * Math.toRadians(lon);
        double y = EARTH_RADIUS * Math.log(Math.tan(Math.PI / 4.0 + Math.toRadians(lat) / 2.0));
        return new Coordinate(x, y);
    }

    private static Coordinate unprojectPoint(Coordinate xy) {
        double lon = Math.toDegrees(xy.x / EARTH_RADIUS);
        double lat = Math.toDegrees(2.0 * Math.atan(Math.exp(xy.y / EARTH_RADIUS)) - Math.PI / 2.0);
        return new Coordinate(lon, lat);
    }
}
