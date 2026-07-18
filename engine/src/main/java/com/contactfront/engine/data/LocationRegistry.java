package com.contactfront.engine.data;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class LocationRegistry {
    private final List<LocationProfile> locations = new ArrayList<>();
    private final Random rng;

    public LocationRegistry() {
        this(new Random());
    }

    public LocationRegistry(Random rng) {
        this.rng = rng;
        load();
    }

    public List<LocationProfile> allLocations() {
        return new ArrayList<>(locations);
    }

    public LocationProfile getRandomLocation() {
        if (locations.isEmpty()) {
            throw new IllegalStateException("No locations available in registry");
        }
        return locations.get(rng.nextInt(locations.size()));
    }

    public LocationProfile selectBySeed(long seed) {
        if (locations.isEmpty()) {
            throw new IllegalStateException("No locations available in registry");
        }
        Random seededRng = new Random(seed);
        return locations.get(seededRng.nextInt(locations.size()));
    }

    public int size() {
        return locations.size();
    }

    private void load() {
        try (InputStream in = getClass().getResourceAsStream("/com/contactfront/engine/data/location_profiles.json")) {
            if (in == null) {
                throw new IllegalStateException("Missing location_profiles.json");
            }
            JSONObject root = new JSONObject(new JSONTokener(in));
            JSONArray arr = root.getJSONArray("locations");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject j = arr.getJSONObject(i);
                locations.add(new LocationProfile(
                    j.getString("name"),
                    parseBoundingBox(j.getJSONObject("bounding_box")),
                    jsonArrayToTags(j.getJSONArray("tags")),
                    j.optLong("seed_hint", System.currentTimeMillis())
                ));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load location profiles: " + e.getMessage(), e);
        }
    }

    private LocationProfile.BoundingBox parseBoundingBox(JSONObject obj) {
        return new LocationProfile.BoundingBox(
            obj.getDouble("min_lat"),
            obj.getDouble("max_lat"),
            obj.getDouble("min_lon"),
            obj.getDouble("max_lon")
        );
    }

    private List<String> jsonArrayToTags(JSONArray arr) {
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            tags.add(arr.getString(i));
        }
        return tags;
    }
}