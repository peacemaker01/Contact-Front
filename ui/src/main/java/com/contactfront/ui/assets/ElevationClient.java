package com.contactfront.ui.assets;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class ElevationClient {
    private static final Logger LOGGER = Logger.getLogger(ElevationClient.class.getName());
    private static final HttpClient client = HttpClient.newHttpClient();
    
    private ElevationClient() {}
    
    /**
     * Fetches SRTM DEM data from OpenTopography API.
     * Returns a GeoTIFF that can be read by GeoTools gt-geotiff module.
     */
    public static byte[] fetchSrtm(double minLat, double minLon, double maxLat, double maxLon) 
            throws IOException, InterruptedException {
        String url = String.format(
            "https://portal.opentopography.org/API/globaldem?demtype=SRTMGL3&south=%.6f&north=%.6f&west=%.6f&east=%.6f&outputFormat=GTiff",
            minLat, maxLat, minLon, maxLon
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .header("Accept", "image/tiff")
            .build();
        
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        
        if (response.statusCode() != 200) {
            throw new IOException("OpenTopography API error: " + response.statusCode());
        }
        
        return response.body();
    }
    
    /**
     * Cached fetch - stores GeoTIFF locally by bounding box hash.
     */
    public static byte[] fetchCachedSrtm(double minLat, double minLon, double maxLat, double maxLon) 
            throws IOException, InterruptedException {
        String cacheKey = String.format("dem_%.4f_%.4f_%.4f_%.4f.tif", 
            minLat, minLon, maxLat, maxLon);
        Path cacheDir = Path.of("cache/dem");
        Path cacheFile = cacheDir.resolve(cacheKey);
        
        if (Files.exists(cacheFile) && Files.size(cacheFile) > 1000) {
            return Files.readAllBytes(cacheFile);
        }
        
        Files.createDirectories(cacheDir);
        byte[] tiffData = fetchSrtm(minLat, minLon, maxLat, maxLon);
        
        try (OutputStream out = Files.newOutputStream(cacheFile)) {
            out.write(tiffData);
        }
        
        return tiffData;
    }
    
    /**
     * Placeholder for procedural elevation generation when real API unavailable.
     */
    public static double[][] generateProceduralElevation(int width, int height, long seed) {
        double[][] elevation = new double[height][width];
        java.util.Random rng = new java.util.Random(seed);
        
        // Multi-octave noise simulation
        for (int octave = 4; octave >= 1; octave--) {
            double scale = 1 << octave;
            double amp = (octave == 1) ? 100 : (octave == 2) ? 50 : (octave == 3) ? 25 : 10;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double noise = rng.nextDouble() * amp;
                    elevation[y][x] += noise / scale;
                }
            }
        }
        
        return elevation;
    }
}