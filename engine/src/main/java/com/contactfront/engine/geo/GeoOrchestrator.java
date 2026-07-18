package com.contactfront.engine.geo;

import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.MathTransform;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKBReader;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * GeoOrchestrator - Aligns raster tiles from MapTiler with vector data from OSM.
 * 
 * This class will use GeoTools to:
 * 1. Transform WGS84 coordinates to Web Mercator (EPSG:3857)
 * 2. Align raster tile coordinates with vector geometry coordinates
 * 3. Provide unified coordinate system for the tactical grid
 */
public class GeoOrchestrator {
    private static final Logger LOGGER = Logger.getLogger(GeoOrchestrator.class.getName());
    
    private static final String WGS84 = "EPSG:4326";
    private static final String WEB_MERCATOR = "EPSG:3857";
    
    private MathTransform wgs84ToMercator;
    private GeometryFactory geometryFactory;
    private WKBReader wkbReader;
    
    public GeoOrchestrator() {
        try {
            this.wgs84ToMercator = CRS.findMathTransform(WGS84, WEB_MERCATOR, true);
            this.geometryFactory = new GeometryFactory();
            this.wkbReader = new WKBReader(geometryFactory);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize GeoTools CRS transform: " + e.getMessage(), e);
        }
    }
    
    /**
     * Transform WGS84 coordinates to Web Mercator.
     * @param lon Longitude in degrees
     * @param lat Latitude in degrees
     * @return Array of [x, y] in Web Mercator meters
     */
    public double[] transformWgs84ToMercator(double lon, double lat) {
        try {
            Coordinate src = new Coordinate(lon, lat);
            Coordinate dest = new Coordinate();
            wgs84ToMercator.transform(src, dest);
            return new double[]{dest.x, dest.y};
        } catch (Exception e) {
            throw new RuntimeException("CRS transform failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculate tile coordinates from lat/lon and zoom level.
     * Uses the standard Web Mercator tile numbering scheme.
     * 
     * @param lat Latitude in degrees
     * @param lon Longitude in degrees
     * @param zoom Zoom level (0-22)
     * @return Array of [tileX, tileY]
     */
    public int[] latLonToTile(double lat, double lon, int zoom) {
        double sinLat = Math.sin(Math.toRadians(lat));
        int tileX = (int) ((lon + 180) / 360 * (1 << zoom));
        int tileY = (int) ((1 - (Math.log(Math.tan(Math.PI / 4 + Math.toRadians(lat)) + sinLat) / Math.PI)) / 2 * (1 << zoom));
        return new int[]{tileX, tileY};
    }
    
    /**
     * Align OSM road segment with raster tile grid.
     * This method will be used to ensure roads from OSM appear at correct positions
     * when overlaid on satellite imagery.
     * 
     * @param roadGeometry WKB-encoded OSM road geometry
     * @param tileBounds Bounding box of the current tile in Web Mercator
     * @return Aligned geometry coordinates
     */
    public Geometry alignOsmGeometry(byte[] roadGeometry, double[] tileBounds) {
        try {
            Geometry geom = wkbReader.read(roadGeometry);
            
            // Transform coordinates to Web Mercator
            Coordinate[] coords = geom.getCoordinates();
            for (Coordinate c : coords) {
                double[] mercator = transformWgs84ToMercator(c.x, c.y);
                c.x = mercator[0];
                c.y = mercator[1];
            }
            
            return geometryFactory.createGeometry(coords);
        } catch (Exception e) {
            LOGGER.warning("Failed to align OSM geometry: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Calculate pixel coordinates within a tile for a given lat/lon.
     * 
     * @param lat Latitude in degrees
     * @param lon Longitude in degrees
     * @param zoom Zoom level
     * @param tileWidth Width of tile in pixels
     * @param tileHeight Height of tile in pixels
     * @return Array of [pixelX, pixelY] within the tile
     */
    public int[] latLonToPixelInTile(double lat, double lon, int zoom, int tileWidth, int tileHeight) {
        int[] tile = latLonToTile(lat, lon, zoom);
        
        double[] mercator = transformWgs84ToMercator(lon, lat);
        double mercatorPerPixel = 2.0 * 20037508.34 / tileWidth;
        
        // Calculate pixel position within the tile
        double tileSize = 256.0;
        double pixelX = (mercator[0] / tileSize) - Math.floor((mercator[0] / tileSize));
        double pixelY = (mercator[1] / tileSize) - Math.floor((mercator[1] / tileSize));
        
        return new int[]{(int) (pixelX * tileWidth), (int) (pixelY * tileHeight)};
    }
}