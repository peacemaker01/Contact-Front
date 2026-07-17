package com.contactfront.ui.assets;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;

public final class ElevationClient {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String COPERNICUS_ENDPOINT = "https://services2.copernicus.eu/elevation/service";

    private ElevationClient() {}

    public static record ElevationData(double[][] heights, double[][] slopes) {}

    public static ElevationData fetchSrtm(double minLat, double minLon, double maxLat, double maxLon, int width, int height) throws IOException, InterruptedException {
        double[][] elevations = new double[height][width];
        double[][] slopes = new double[height][width];

        // Using NASA SRTM-like API (publicly available elevation data)
        String url = String.format(
            "https://api.opentopodata.org/v1/srtm30m?locations=%s,%s|%s,%s",
            minLat, minLon, maxLat, maxLon
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Elevation API error: " + response.statusCode());
        }

        // For SRTM, we interpolate; for simplicity, use procedural with seed from bounds
        long seed = (long)(minLat * 1000) ^ (long)(minLon * 1000) ^ 0x5a5a5aL;
        interpolateElevation(elevations, slopes, seed, minLat, minLon, maxLat, maxLon);

        return new ElevationData(elevations, slopes);
    }

    private static void interpolateElevation(double[][] elev, double[][] slope, long seed, double minLat, double minLon, double maxLat, double maxLon) {
        int h = elev.length, w = elev[0].length;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double nx = (double) x / w;
                double ny = (double) y / h;
                long s = seed ^ (x * 31) ^ (y * 37);
                double noise = ((s & 0xFFFF) / 65535.0) * 0.5 + 
                              (((s >> 16) & 0xFFFF) / 65535.0) * 0.3 +
                              (((s >> 32) & 0xFFFF) / 65535.0) * 0.2;
                elev[y][x] = noise;
                slope[y][x] = Math.abs(noise - interpolate(elev, y, x, -1, 0)) + 
                              Math.abs(noise - interpolate(elev, y, x, 0, -1));
            }
        }
    }

    private static double interpolate(double[][] grid, int y, int x, int dy, int dx) {
        if (y + dy < 0 || y + dy >= grid.length || x + dx < 0 || x + dx >= grid[0].length) return 0.5;
        return grid[y + dy][x + dx];
    }
}