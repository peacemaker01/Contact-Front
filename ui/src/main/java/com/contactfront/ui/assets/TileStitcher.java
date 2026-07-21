package com.contactfront.ui.assets;

import com.contactfront.ui.Log;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TileStitcher {
    private TileStitcher() {}

    public static BufferedImage stitchTiles(int zoom, int minTileX, int minTileY, int maxTileX, int maxTileY, int tileSize) {
        int width = (maxTileX - minTileX + 1) * tileSize;
        int height = (maxTileY - minTileY + 1) * tileSize;
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        List<Thread> workers = new ArrayList<>();
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());
        for (int ty = minTileY; ty <= maxTileY; ty++) {
            final int y = ty;
            Thread t = new Thread(() -> {
                for (int tx = minTileX; tx <= maxTileX; tx++) {
                    final int x = tx;
                    try {
                        BufferedImage tile = TileFetcher.fetchTile(zoom, x, y);
                        if (tile != null) {
                            int dx = (x - minTileX) * tileSize;
                            int dy = (y - minTileY) * tileSize;
                            synchronized (g) {
                                g.drawImage(tile, dx, dy, null);
                            }
                        }
                    } catch (Exception e) {
                        errors.add(e);
                    }
                }
            });
            t.start();
            workers.add(t);
        }
        for (Thread t : workers) {
            try { t.join(); } catch (InterruptedException ignored) {}
        }
        g.dispose();
        if (!errors.isEmpty()) {
            Log.warning("TileStitcher: " + errors.size() + " tile errors");
        }
        return result;
    }
}
