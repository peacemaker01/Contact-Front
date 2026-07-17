package com.contactfront.ui.assets;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GoogleMapsClient {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static String apiKey = "";
    private static Path cacheDir = Path.of("cache/maps");

    private GoogleMapsClient() {}

    public static void setApiKey(String key) {
        apiKey = key != null ? key.trim() : "";
    }

    public static void setCacheDir(Path dir) {
        cacheDir = dir;
    }

    public static String getApiKey() {
        return apiKey;
    }

    public static Path getCacheDir() {
        return cacheDir;
    }

    public static record SatelliteImage(byte[] data, int width, int height, double latitude, double longitude) {}

    public static SatelliteImage downloadSatelliteImage(double lat, double lon, int zoom, int size) throws IOException, InterruptedException {
        if (apiKey.isEmpty()) {
            throw new IOException("Google Maps API key not configured");
        }

        String url = String.format(
            "https://maps.googleapis.com/maps/api/staticmap?center=%f,%f&zoom=%d&size=%dx%d&maptype=satellite&key=%s",
            lat, lon, zoom, size, size, apiKey
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException("Google Maps API error: " + response.statusCode());
        }

        return new SatelliteImage(response.body(), size, size, lat, lon);
    }

    public static Path cacheImage(double lat, double lon, int zoom, int size) throws IOException, InterruptedException {
        Path cacheFile = cacheDir.resolve(String.format("satellite_%.4f_%.4f_z%d_%d.png", lat, lon, zoom, size));

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
        Path cacheFile = cacheDir.resolve(String.format("satellite_%.4f_%.4f_z%d_%d.png", lat, lon, zoom, size));
        return Files.exists(cacheFile);
    }
}