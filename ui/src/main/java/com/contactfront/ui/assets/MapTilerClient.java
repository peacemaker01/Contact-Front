package com.contactfront.ui.assets;

import com.contactfront.ui.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class MapTilerClient {
    private static final Logger LOGGER = Logger.getLogger(MapTilerClient.class.getName());
    private static final HttpClient client = HttpClient.newHttpClient();
    private static String apiKey = "";
    private static Path cacheDir = Path.of("cache/maps");
    
    private MapTilerClient() {}
    
    public static void setApiKey(String key) {
        apiKey = key != null ? key.trim() : "";
        Log.info("MapTilerClient API key " + (apiKey.isEmpty() ? "cleared" : "configured"));
    }
    
    public static void setCacheDir(Path dir) {
        cacheDir = dir;
        Log.info("MapTilerClient cache directory set: " + dir);
    }
    
    public static String getApiKey() {
        return apiKey;
    }
    
    public static boolean validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            Log.error("MapTilerClient validation failed: key is empty");
            return false;
        }
        try {
            String testUrl = "https://api.maptiler.com/maps/streets-v2/static/0,0,1,1/1x1.png?key=" + key.trim();
            Log.info("MapTilerClient requesting validation URL...");
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(testUrl))
                .GET()
                .header("User-Agent", "ContactFront/1.0")
                .build();
            HttpResponse<byte[]> resp = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofByteArray());
            int status = resp.statusCode();
            Log.info("MapTilerClient validation response: " + status);
            if (status == 200) {
                Log.info("MapTilerClient API key validation successful");
                return true;
            }
            if (status == 401) {
                Log.error("MapTilerClient validation failed: unauthorized (401)");
                return false;
            }
            if (status == 403) {
                byte[] body = resp.body();
                String responseBody = new String(body);
                if (responseBody.contains("Invalid API key") || responseBody.contains("\"error\"") && responseBody.contains("invalid")) {
                    Log.error("MapTilerClient validation failed: invalid key (403)");
                    return false;
                }
                Log.info("MapTilerClient validation passed (403 but likely quota exceeded for free tier)");
                return true;
            }
            Log.warning("MapTilerClient validation returned status: " + status);
            return status < 400;
        } catch (Exception e) {
            Log.error("MapTilerClient validation error: " + e.getMessage());
            return false;
        }
    }
    
    public static Path getCacheDir() {
        return cacheDir;
    }
    
    public static record SatelliteImage(byte[] data, int width, int height) {}
    
    public static SatelliteImage downloadSatelliteImage(double minLat, double minLon, double maxLat, double maxLon, int imageWidth, int imageHeight) 
            throws IOException, InterruptedException {
        Log.info(String.format("MapTilerClient downloading satellite: bbox=%.4f,%.4f - %.4f,%.4f size=%dx%d", 
            minLat, minLon, maxLat, maxLon, imageWidth, imageHeight));
        if (apiKey.isEmpty()) {
            Log.error("MapTilerClient download failed: API key not configured");
            throw new IOException("MapTiler API key not configured");
        }
        
        String url = String.format(
            "https://api.maptiler.com/maps/satellite/static/%.6f,%.6f,%.6f,%.6f/%dx%d.png?key=%s",
            minLon, minLat, maxLon, maxLat, imageWidth, imageHeight, apiKey
        );
        
        Log.info("MapTilerClient requesting: " + url.replace(apiKey, "***"));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .header("Accept", "image/png")
            .build();
        
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        
        if (response.statusCode() != 200) {
            String body = response.body().length > 500 ? new String(response.body(), 0, 500) : new String(response.body());
            Log.error("MapTiler Static API error " + response.statusCode() + ": " + body);
            throw new IOException("MapTiler Static API error: " + response.statusCode());
        }
        
        Log.info("MapTilerClient download complete: " + response.body().length + " bytes");
        return new SatelliteImage(response.body(), imageWidth, imageHeight);
    }
    
    public static SatelliteImage fetchCachedSatelliteImage(double minLat, double minLon, double maxLat, double maxLon, int imageWidth, int imageHeight) 
            throws IOException, InterruptedException {
        if (apiKey.isEmpty()) {
            Log.info("MapTilerClient: No API key - skipping satellite fetch");
            return null;
        }
        String cacheKey = String.format("static_%.4f_%.4f_%.4f_%.4f_%dx%d.png", 
            minLat, minLon, maxLat, maxLon, imageWidth, imageHeight);
        Path cacheFile = cacheDir.resolve(cacheKey);
        
        if (Files.exists(cacheFile) && Files.size(cacheFile) > 1000) {
            Log.info("MapTilerClient cache hit: " + cacheKey + " (" + Files.size(cacheFile) + " bytes)");
            byte[] cached = Files.readAllBytes(cacheFile);
            return new SatelliteImage(cached, imageWidth, imageHeight);
        }
        
        Log.info("MapTilerClient cache miss, fetching fresh image...");
        Files.createDirectories(cacheDir);
        SatelliteImage image = downloadSatelliteImage(minLat, minLon, maxLat, maxLon, imageWidth, imageHeight);
        
        try (OutputStream out = Files.newOutputStream(cacheFile)) {
            out.write(image.data);
            Log.info("MapTilerClient cached image to: " + cacheFile);
        }
        
        return image;
    }
    
    public static boolean hasCachedSatellite(double minLat, double minLon, double maxLat, double maxLon, int imageWidth, int imageHeight) {
        String cacheKey = String.format("static_%.4f_%.4f_%.4f_%.4f_%dx%d.png", 
            minLat, minLon, maxLat, maxLon, imageWidth, imageHeight);
        Path cacheFile = cacheDir.resolve(cacheKey);
        try {
            boolean has = Files.exists(cacheFile) && Files.size(cacheFile) > 1000;
            Log.info("MapTilerClient hasCached check: " + cacheKey + " = " + has);
            return has;
        } catch (IOException e) {
            return false;
        }
    }
}