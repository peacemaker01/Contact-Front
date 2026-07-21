package com.contactfront.ui.view;

import armyc2.c5isr.renderer.SinglePointRenderer;
import armyc2.c5isr.renderer.utilities.ImageInfo;
import com.contactfront.engine.model.SidcCode;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public final class SymbolRenderer {
    private SymbolRenderer() {}

    private static final SinglePointRenderer renderer = new SinglePointRenderer();

    public static void drawSymbol(GraphicsContext g, SidcCode sidc, int cx, int cy, int size, Color color) {
        try {
            Map<String, String> props = new HashMap<>();
            props.put("pixelSize", String.valueOf(size));
            Map<String, String> mods = new HashMap<>();

            ImageInfo info = renderer.render(sidc.code(), props, mods);
            if (info != null) {
                BufferedImage img = info.getImage();
                if (img != null) {
                    Image fxImg = SwingFXUtils.toFXImage(img, null);
                    g.drawImage(fxImg, cx - size/2, cy - size/2);
                    return;
                }
            }
        } catch (Exception e) {
            // fall through to fallback
        }
        drawFallbackSymbol(g, sidc, cx, cy, size, color);
    }

    private static void drawFallbackSymbol(GraphicsContext g, SidcCode sidc, int cx, int cy, int size, Color color) {
        boolean hostile = sidc.isHostile();
        int r = size / 2 - 4;

        g.setStroke(color);
        g.setLineWidth(1.5);

        if (hostile) {
            g.beginPath();
            g.moveTo(cx, cy - r);
            g.lineTo(cx + r, cy);
            g.lineTo(cx, cy + r);
            g.lineTo(cx - r, cy);
            g.closePath();
            g.stroke();
        } else {
            g.strokeRect(cx - r, cy - r, r * 2, r * 2);
        }
    }
}