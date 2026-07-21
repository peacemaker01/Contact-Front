package com.contactfront.ui.assets;

import com.contactfront.ui.Log;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class TileFetcher {
    private TileFetcher() {}

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String TILE_URL = "https://api.maptiler.com/tiles/satellite/{z}/{x}/{y}.jpg?key={key}";
    private static volatile Path cacheDir;
    private static volatile String apiKey;

    public static void configure(String key, Path dir) {
        apiKey = key;
        cacheDir = dir;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            Log.warning("TileFetcher: cannot create cache dir " + dir + ": " + e.getMessage());
        }
    }

    public static BufferedImage fetchTile(int z, int x, int y) {
        if (apiKey == null || apiKey.isEmpty()) return null;
        String key = z + "/" + x + "/" + y;
        Path cached = cacheDir.resolve(key);
        try {
            if (Files.exists(cached)) {
                return ImageIO.read(Files.newInputStream(cached));
            }
        } catch (IOException e) {
            Log.warning("TileFetcher: cache read failed for " + key);
        }
        String url = TILE_URL.replace("{z}", Integer.toString(z))
                .replace("{x}", Integer.toString(x))
                .replace("{y}", Integer.toString(y))
                .replace("{key}", apiKey);
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                Log.warning("TileFetcher: HTTP " + resp.statusCode() + " for " + key);
                return null;
            }
            BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(resp.body()));
            if (img != null && cacheDir != null) {
                try {
                    Files.createDirectories(cached.getParent());
                    Files.write(cached, resp.body());
                } catch (IOException e) {
                    Log.warning("TileFetcher: cache write failed for " + key);
                }
            }
            return img;
        } catch (Exception e) {
            Log.warning("TileFetcher: fetch failed for " + key + ": " + e.getMessage());
            return null;
        }
    }
}
