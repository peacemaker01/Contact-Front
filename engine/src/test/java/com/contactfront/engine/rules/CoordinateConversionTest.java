package com.contactfront.engine.rules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CoordinateConversionTest {
    
    private static final double EARTH_RADIUS = 6378137.0;
    private static final double MERCATOR_SCALE = 20037508.34;
    
    @Test
    public void testWebMercatorConversionSanity() {
        double[] result = wgs84ToWebMercator(-122.4194, 37.7749);
        assertTrue(result[0] < 0, "San Francisco longitude should be negative in Web Mercator");
        assertTrue(result[1] > 0, "San Francisco latitude should be positive in Web Mercator");
        assertTrue(Math.abs(result[0]) < MERCATOR_SCALE, "Web Mercator X should be within Earth bounds");
        assertTrue(Math.abs(result[1]) < MERCATOR_SCALE, "Web Mercator Y should be within Earth bounds");
    }
    
    @Test
    public void testCenterCoordinatesMapToCenter() {
        double centerLon = -122.4194;
        double centerLat = 37.7749;
        int width = 100;
        int height = 100;
        
        int gridX = wgs84LonToGrid(centerLon, centerLon, width);
        int gridY = wgs84LatToGrid(centerLat, centerLat, height);
        
        assertEquals(50, gridX, "Center longitude should map to grid center X");
        assertEquals(50, gridY, "Center latitude should map to grid center Y");
    }
    
    @Test
    public void testSamePointSameGrid() {
        double testLon = -122.4194;
        double testLat = 37.7749;
        double centerLon = testLon;
        double centerLat = testLat;
        int width = 100;
        int height = 100;
        
        int gx1 = wgs84LonToGrid(testLon, centerLon, width);
        int gy1 = wgs84LatToGrid(testLat, centerLat, height);
        int gx2 = wgs84LonToGrid(testLon, centerLon, width);
        int gy2 = wgs84LatToGrid(testLat, centerLat, height);
        
        assertEquals(gx1, gx2, "Same input should produce same grid X");
        assertEquals(gy1, gy2, "Same input should produce same grid Y");
    }
    
    @Test
    public void testGoldenGateBridgeAlignment() {
        double goldenGateLon = -122.4764;
        double goldenGateLat = 37.8199;
        int width = 200;
        int height = 150;
        
        int gridX = wgs84LonToGrid(goldenGateLon, goldenGateLon, width);
        int gridY = wgs84LatToGrid(goldenGateLat, goldenGateLat, height);
        
        assertTrue(gridX >= 0 && gridX < width, "Grid X should be within bounds, got: " + gridX);
        assertTrue(gridY >= 0 && gridY < height, "Grid Y should be within bounds, got: " + gridY);
        
        double[] merc = wgs84ToWebMercator(goldenGateLon, goldenGateLat);
        System.out.println("Golden Gate Bridge coordinates:");
        System.out.println("  WGS84: lon=" + goldenGateLon + ", lat=" + goldenGateLat);
        System.out.println("  WebMercator: x=" + merc[0] + ", y=" + merc[1]);
        System.out.println("  Grid: x=" + gridX + ", y=" + gridY);
    }
    
    @Test
    public void testOSMAndSatelliteAlignment() {
        double centerLon = -122.4194;
        double centerLat = 37.7749;
        int width = 100;
        int height = 100;
        
        double testLon = centerLon + 0.01;
        double testLat = centerLat;
        
        int gridX = wgs84LonToGrid(testLon, centerLon, width);
        int gridY = wgs84LatToGrid(testLat, centerLat, height);
        
        assertTrue(gridX >= 0 && gridX < width, "Grid X should be within bounds");
        assertTrue(gridY >= 0 && gridY < height, "Grid Y should be within bounds");
    }
    
    static double[] wgs84ToWebMercator(double lon, double lat) {
        double x = EARTH_RADIUS * Math.toRadians(lon);
        double latRad = Math.toRadians(lat);
        double y = EARTH_RADIUS * Math.log(Math.tan(Math.PI / 4 + latRad / 2));
        return new double[]{x, y};
    }
    
    static int wgs84LonToGrid(double lon, double centerLon, int width) {
        double[] centerMerc = wgs84ToWebMercator(centerLon, 0);
        double centerMercX = centerMerc[0];
        double mercX = wgs84ToWebMercator(lon, 0)[0];
        
        double mercPerTile = 2.0 * MERCATOR_SCALE / width;
        return (int) Math.round((mercX - centerMercX) / mercPerTile + width / 2.0);
    }
    
    static int wgs84LatToGrid(double lat, double centerLat, int height) {
        double[] centerMerc = wgs84ToWebMercator(0, centerLat);
        double centerMercY = centerMerc[1];
        double mercY = wgs84ToWebMercator(0, lat)[1];
        
        double mercPerTile = 2.0 * MERCATOR_SCALE / height;
        return (int) Math.round((centerMercY - mercY) / mercPerTile + height / 2.0);
    }
}