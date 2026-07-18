package com.contactfront.ui.assets;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.logging.Logger;

public final class MapTilerClient {
    private static final Logger LOGGER = Logger.getLogger(MapTilerClient.class.getName());
    private static final HttpClient client = HttpClient.newHttpClient();
    private static String apiKey = "";
    private static Path cacheDir = Path.of("cache/maps");
    private static final String TILE_URL_TEMPLATE = 
        "https://api.maptiler.com/maps/satellite-v3/{z}/{x}/{y}.jpg?key=%s";
    
    private MapTilerClient() {}
    
    public static void setApiKey(String key) {
        apiKey = key != null ? key.trim() : "";
    }
    
    public static void setCacheDir(Path dir) {
        cacheDir = dir;
    }
    
    public static String getApiKey() {
        return apiKey;
    }
    
    public static boolean validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        try {
            String testUrl = "https://api.maptiler.com/maps/streets-v2/0/0/0.jpg?key=" + key.trim();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(testUrl))
                .GET()
                .header("User-Agent", "ContactFront/1.0")
                .build();
            HttpResponse<byte[]> resp = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofByteArray());
            int status = resp.statusCode();
            LOGGER.fine("MapTiler validation status: " + status);
            if (status == 200) return true;
            if (status == 401) return false;
            if (status == 403) {
                byte[] body = resp.body();
                String responseBody = new String(body);
                if (responseBody.contains("Invalid API key") || responseBody.contains("invalid")) {
                    return false;
                }
                return true;
            }
            return status < 400;
        } catch (Exception e) {
            LOGGER.warning("MapTiler validation error: " + e.getMessage());
            return false;
        }
    }
    
    public static Path getCacheDir() {
        return cacheDir;
    }
    
    public static record SatelliteImage(byte[] data, int width, int height, double latitude, double longitude) {}
    
    public static SatelliteImage downloadSatelliteImage(double lat, double lon, int zoom, int size) 
            throws IOException, InterruptedException {
        if (apiKey.isEmpty()) {
            throw new IOException("MapTiler API key not configured");
        }
        
        double[] tileCoords = latLonToTile(lat, lon, zoom);
        int tileX = (int) tileCoords[0];
        int tileY = (int) tileCoords[1];
        int tileSize = 256;
        
        String url = String.format(TILE_URL_TEMPLATE, apiKey).replace("{z}", String.valueOf(zoom))
                .replace("{x}", String.valueOf(tileX)).replace("{y}", String.valueOf(tileY));
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .header("Accept", "image/jpeg")
            .build();
        
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        
        if (response.statusCode() != 200) {
            throw new IOException("MapTiler API error: " + response.statusCode());
        }
        
        byte[] imageData = response.body();
        double[] bounds = tileBounds(lon, lat, zoom);
        
        return new SatelliteImage(imageData, tileSize, tileSize, lat, lon);
    }
    
    public static Path cacheImage(double lat, double lon, int zoom, int size) throws IOException, InterruptedException {
        String cacheKey = String.format("satellite_%.4f_%.4f_z%d_%d.jpg", lat, lon, zoom, size);
        Path cacheFile = cacheDir.resolve(cacheKey);
        
        if (Files.exists(cacheFile) && Files.size(cacheFile) > 1000) {
            return cacheFile;
        }
        
        Files.createDirectories(cacheDir);
        SatelliteImage image = downloadSatelliteImage(lat, lon, zoom, size);
        
        try (OutputStream out = Files.newOutputStream(cacheFile)) {
            out.write(image.data);
        }
        
        return cacheFile;
    }
    
    public static boolean hasCachedImage(double lat, double lon, int zoom, int size) {
        String cacheKey = String.format("satellite_%.4f_%.4f_z%d_%d.jpg", lat, lon, zoom, size);
        Path cacheFile = cacheDir.resolve(cacheKey);
        return Files.exists(cacheFile);
    }
    
    public static double[] latLonToTile(double lat, double lon, int zoom) {
        double sinLat = Math.sin(Math.toRadians(lat));
        int x = (int) ((lon + 180) / 360 * (1 << zoom));
        int y = (int) ((1 - (Math.log(Math.tan(Math.PI / 4 + Math.toRadians(lat)) + sinLat) / Math.PI)) / 2 * (1 << zoom));
        return new double[]{x, y};
    }
    
    public static double[] tileBounds(double centerLon, double centerLat, int zoom) {
        double[] tile = latLonToTile(centerLat, centerLon, zoom);
        int x = (int) tile[0];
        int y = (int) tile[1];
        
        double n = 1 << zoom;
        double lonDeg0 = x / n * 360 - 180;
        double lonDeg1 = (x + 1) / n * 360 - 180;
        double latRad0 = 2 * Math.atan(Math.exp(Math.PI * (1 - 2 * y / n))) - Math.PI / 2;
        double latRad1 = 2 * Math.atan(Math.exp(Math.PI * (1 - 2 * (y + 1) / n))) - Math.PI / 2;
        
        return new double[]{lonDeg0, Math.toDegrees(latRad1), lonDeg1, Math.toDegrees(latRad0)};
    }
    
    public static double[][] fetchTileMatrix(double lat, double lon, int zoom, int width, int height) {
        double[] tile = latLonToTile(lat, lon, zoom);
        int tileX = (int) tile[0];
        int tileY = (int) tile[1];
        
        double[][] result = new double[height][width];
        double[] bounds = tileBounds(lon, lat, zoom);
        
        double lonStep = (bounds[2] - bounds[0]) / width;
        double latStep = (bounds[3] - bounds[1]) / height;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = Math.sqrt(x * x + y * y);
            }
        }
        
        return result;
    }
}