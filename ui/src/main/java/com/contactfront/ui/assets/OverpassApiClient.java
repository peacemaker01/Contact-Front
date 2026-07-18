package com.contactfront.ui.assets;

import com.contactfront.engine.model.Building;
import com.contactfront.engine.model.RoadSegment;
import com.contactfront.engine.model.RoadSegment.RoadType;
import com.contactfront.ui.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public final class OverpassApiClient {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String OVERPASS_ENDPOINT = "https://overpass-api.de/api/interpreter";

    private OverpassApiClient() {}

    public static record OsmData(List<RoadSegment> roads, List<Building> buildings) {}

    public static OsmData fetchBbox(double minLat, double minLon, double maxLat, double maxLon) throws IOException, InterruptedException {
        Log.info(String.format("Fetching OSM data for bbox: lat=%.4f-%.4f, lon=%.4f-%.4f", minLat, maxLat, minLon, maxLon));
        String query = String.format("""
            [out:json][timeout:25];
            (
              way["highway"](%f,%f,%f,%f);
              way["building"](%f,%f,%f,%f);
            );
            out geom;
            """, minLat, minLon, maxLat, maxLon, minLat, minLon, maxLat, maxLon);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OVERPASS_ENDPOINT))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("data=" + query.replace("\n", "").replace(" ", "+")))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            Log.error("Overpass API error: " + response.statusCode() + " " + response.body());
            throw new IOException("Overpass API error: " + response.statusCode());
        }

        OsmData data = parseOsm(response.body());
        Log.info(String.format("OSM fetch complete: %d roads, %d buildings", data.roads.size(), data.buildings.size()));
        return data;
    }

    private static OsmData parseOsm(String json) {
        List<RoadSegment> roads = new ArrayList<>();
        List<Building> buildings = new ArrayList<>();

        JSONObject root = new JSONObject(json);
        JSONArray elements = root.getJSONArray("elements");

        for (int i = 0; i < elements.length(); i++) {
            JSONObject elem = elements.getJSONObject(i);
            if (!"way".equals(elem.optString("type"))) continue;

            String highway = elem.optString("highway", "");
            if (!highway.isEmpty()) {
                roads.add(parseRoad(elem, highway));
            }

            String buildingVal = elem.optString("building", "");
            if (!buildingVal.isEmpty()) {
                buildings.add(parseBuilding(elem));
            }
        }

        return new OsmData(roads, buildings);
    }

    private static RoadSegment parseRoad(JSONObject elem, String highway) {
        JSONArray coords = elem.optJSONArray("geometry");
        List<double[]> points = new ArrayList<>();
        if (coords != null) {
            for (int i = 0; i < coords.length(); i++) {
                JSONObject c = coords.getJSONObject(i);
                double lon = c.getDouble("lon");
                double lat = c.getDouble("lat");
                points.add(new double[]{lon, lat});
            }
        }
        RoadType type = switch (highway) {
            case "motorway" -> RoadType.MOTORWAY;
            case "trunk" -> RoadType.TRUNK;
            case "primary" -> RoadType.PRIMARY;
            case "secondary" -> RoadType.SECONDARY;
            case "tertiary" -> RoadType.TERTIARY;
            case "residential" -> RoadType.RESIDENTIAL;
            case "service" -> RoadType.SERVICE;
            default -> RoadType.UNCLASSIFIED;
        };
        return new RoadSegment(points, type);
    }

    private static Building parseBuilding(JSONObject elem) {
        JSONArray coords = elem.optJSONArray("geometry");
        List<double[]> points = new ArrayList<>();
        if (coords != null) {
            for (int i = 0; i < coords.length(); i++) {
                JSONObject c = coords.getJSONObject(i);
                double lon = c.getDouble("lon");
                double lat = c.getDouble("lat");
                points.add(new double[]{lon, lat});
            }
        }
        double height = elem.optDouble("height", elem.optDouble("building:levels", 10.0));
        return new Building(points, height);
    }
}