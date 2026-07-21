package com.contactfront.engine.rules;

import com.contactfront.engine.geo.CoordinateConverter;
import com.contactfront.engine.geo.RangeCalculator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import static org.junit.jupiter.api.Assertions.*;

public class CoordinateConversionTest {

    @Test
    public void testWgs84ToWebMercatorRoundTrip() {
        double lon = -122.4194;
        double lat = 37.7749;
        Point wgs84 = new GeometryFactory().createPoint(new Coordinate(lon, lat));
        Geometry metric = CoordinateConverter.projectToMetric(wgs84);
        Point back = (Point) CoordinateConverter.unprojectToWgs84(metric);
        assertEquals(lon, back.getX(), 1e-6);
        assertEquals(lat, back.getY(), 1e-6);
    }

    @Test
    public void testEquatorPointProjectsToZeroY() {
        Point wgs84 = new GeometryFactory().createPoint(new Coordinate(0.0, 0.0));
        Point metric = (Point) CoordinateConverter.projectToMetric(wgs84);
        assertEquals(0.0, metric.getX(), 1e-6);
        assertEquals(0.0, metric.getY(), 1e-6);
    }

    @Test
    public void testWebMercatorXInExpectedRange() {
        Point wgs84 = new GeometryFactory().createPoint(new Coordinate(-122.4194, 37.7749));
        Point metric = (Point) CoordinateConverter.projectToMetric(wgs84);
        assertTrue(metric.getX() < 0, "San Francisco longitude should be negative in Web Mercator");
        assertTrue(Math.abs(metric.getX()) < 20037508.34, "Web Mercator X should be within Earth bounds");
    }

    @Test
    public void testRangeCalculatorKnownDistance() {
        Coordinate start = new Coordinate(0.0, 0.0);
        Coordinate end = new Coordinate(0.0, 1.0);
        double distance = RangeCalculator.orthodromicDistance(start, end);
        assertTrue(distance > 100000 && distance < 120000, "Distance should be ~111km, got: " + distance);
    }

    @Test
    public void testRangeCalculatorHaversiveFallback() {
        double distance = RangeCalculator.haversine(0.0, 0.0, 0.0, 1.0);
        assertTrue(distance > 100000 && distance < 120000, "Haversine distance should be ~111km, got: " + distance);
    }
}
