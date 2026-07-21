package com.contactfront.engine.terrain;

import com.contactfront.engine.Log;
import com.contactfront.engine.data.LocationProfile;
import com.contactfront.engine.data.LocationRegistry;
import com.contactfront.engine.model.Faction;
import com.contactfront.engine.model.GameState;

import java.util.Random;

public final class GeoScenarioProvider {
    private final LocationRegistry registry;
    private final long seed;
    private final int width;
    private final int height;
    private final Faction playerFaction;
    private final Faction enemyFaction;

    private LocationProfile selectedLocation;
    private GameState state;

    public GeoScenarioProvider(long seed, int width, int height, Faction playerFaction, Faction enemyFaction) {
        this(seed, width, height, playerFaction, enemyFaction, new LocationRegistry(new Random(seed)));
    }

    public GeoScenarioProvider(long seed, int width, int height, Faction playerFaction, Faction enemyFaction, 
                                LocationRegistry registry) {
        Log.info("GeoScenarioProvider: Creating with seed=" + seed + " dims=" + width + "x" + height);
        this.seed = seed;
        this.width = width;
        this.height = height;
        this.playerFaction = playerFaction;
        this.enemyFaction = enemyFaction;
        this.registry = registry;
        this.selectedLocation = selectRandomLocation();
        Log.info("GeoScenarioProvider: Selected location " + selectedLocation.name() + " at (" + selectedLocation.boundingBox().centerLat() + "," + selectedLocation.boundingBox().centerLon() + ")");
    }

    public LocationProfile selectRandomLocation() {
        LocationProfile loc = registry.selectBySeed(seed);
        Log.info("GeoScenarioProvider.selectRandomLocation: " + loc.name());
        return loc;
    }

    public LocationProfile getSelectedLocation() {
        return selectedLocation;
    }

    public LocationProfile.BoundingBox getBoundingBox() {
        return selectedLocation.boundingBox();
    }

    public long getSeed() {
        return seed;
    }

    /**
     * Returns the latitude/longitude bounds for the selected location.
     */
    public double[] getLatLngBounds() {
        var bbox = selectedLocation.boundingBox();
        return new double[]{bbox.minLat(), bbox.minLon(), bbox.maxLat(), bbox.maxLon()};
    }

    /**
     * Calculate image dimensions for static map at given zoom level.
     * Returns width, height in pixels.
     */
    public int[] calculateImageDimensions(int zoom) {
        var bbox = selectedLocation.boundingBox();
        double lonSpan = bbox.maxLon() - bbox.minLon();
        double latSpan = bbox.maxLat() - bbox.minLat();
        
        // Approximate: at zoom level, ~156543 meters per pixel
        double metersPerPixel = 156543.0 / Math.pow(2, zoom);
        
        // Convert degrees to meters (rough approximation)
        double lonMeters = lonSpan * 111320 * Math.cos(Math.toRadians(bbox.centerLat()));
        double latMeters = latSpan * 111000;
        
        int imgWidth = (int) Math.ceil(lonMeters / metersPerPixel);
        int imgHeight = (int) Math.ceil(latMeters / metersPerPixel);
        
        // Clamp to MapTiler limits (max 1024 for free tier)
        imgWidth = Math.min(1024, Math.max(256, imgWidth));
        imgHeight = Math.min(1024, Math.max(256, imgHeight));
        
        return new int[]{imgWidth, imgHeight};
    }
    
    public GeoScenarioResult generate() {
        if (selectedLocation == null) {
            throw new IllegalStateException("No location selected");
        }

        LocationProfile.BoundingBox bbox = selectedLocation.boundingBox();
        double centerLat = bbox.centerLat();
        double centerLon = bbox.centerLon();

        GameState state = new GameState();
        state.latitude = centerLat;
        state.longitude = centerLon;
        state.playerFaction = playerFaction;
        state.enemyFaction = enemyFaction;
        this.state = state;

        return new GeoScenarioResult(state, selectedLocation, bbox);
    }

    public static record GeoScenarioResult(
        GameState state,
        LocationProfile location,
        LocationProfile.BoundingBox boundingBox
    ) {}
}