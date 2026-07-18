package com.contactfront.ui.view;

import com.contactfront.engine.model.*;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.UnitProfile;
import com.contactfront.engine.model.Faction;
import com.contactfront.ui.controller.GameController;
import com.contactfront.ui.Palette;
import com.contactfront.ui.TerrainBaker;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.TextAlignment;

public class MapView {
    private final GameController ctrl;
    private final int tile;
    private double zoom = 1.0;
    private final java.util.function.BiConsumer<Integer, Integer> onMapClick;

    private final Canvas canvas;
    private final StackPane stack;
    private final javafx.scene.control.ScrollPane scroll;
    private final ToggleButton satelliteToggle;

    private WritableImage terrainImg;
    private WritableImage satelliteImg;
    private GameState bakedState;
    private boolean showSatellite = false;

    // drag-select state
    private int dragX0 = -1, dragY0 = -1, dragX1 = -1, dragY1 = -1;
    private boolean dragging = false;
    private boolean pressOnUnit = false;

    public MapView(GameController ctrl, int tileSize) {
        this(ctrl, tileSize, null);
    }
    
    public MapView(GameController ctrl, int tileSize, java.util.function.BiConsumer<Integer, Integer> onMapClick) {
        this.ctrl = ctrl;
        this.tile = tileSize;
        this.onMapClick = onMapClick;
        this.canvas = new Canvas();
        this.satelliteToggle = new ToggleButton("Satellite");
        styleToggleButton(satelliteToggle);
        satelliteToggle.setOnAction(e -> {
            showSatellite = satelliteToggle.isSelected();
            redraw();
        });
        this.stack = new StackPane(canvas, satelliteToggle);
        this.stack.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        StackPane.setAlignment(satelliteToggle, javafx.geometry.Pos.TOP_RIGHT);
        StackPane.setMargin(satelliteToggle, new javafx.geometry.Insets(8));
        this.scroll = new javafx.scene.control.ScrollPane(stack);
        this.scroll.setStyle("-fx-background-color:#0e1117;");
        this.scroll.setPannable(false);
        this.scroll.setFitToWidth(true);
        this.scroll.setFitToHeight(true);
        wireMouse();
    }

    private void styleToggleButton(ToggleButton btn) {
        btn.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-font-size:12px; -fx-pref-height:28px; -fx-border-color:#5a6e82;");
        btn.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) {
                btn.setStyle("-fx-background-color:#4fc3f7; -fx-text-fill:#000000; -fx-font-size:12px; -fx-pref-height:28px; -fx-border-color:#2c4258;");
            } else {
                btn.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-font-size:12px; -fx-pref-height:28px; -fx-border-color:#5a6e82;");
            }
        });
    }

    public javafx.scene.control.ScrollPane scrollPane() { return scroll; }
    public double getZoom() { return zoom; }

    public void setSatelliteImage(javafx.scene.image.Image img) {
        this.satelliteImg = img != null ? TerrainBaker.toWritableImage(img) : null;
    }

    public void setZoom(double z) {
        zoom = Math.max(0.5, Math.min(2.5, z));
        redraw();
    }

    private int ts() { return (int) (tile * zoom); }

    private int tileAt(double coord) { return (int) (coord / ts()); }

    private void wireMouse() {
        canvas.setOnMousePressed(e -> {
            int tx = tileAt(e.getX()), ty = tileAt(e.getY());
            if (e.getButton() == MouseButton.SECONDARY) {
                ctrl.contextOrder(tx, ty);
                return;
            }
            // Check for editor mode click
            if (onMapClick != null) {
                onMapClick.accept(tx, ty);
                return;
            }
            // primary
            dragX0 = tx; dragY0 = ty; dragX1 = tx; dragY1 = ty;
            dragging = false;
            Unit u = ctrl.state != null ? ctrl.state.friendlyUnitAt(tx, ty) : null;
            pressOnUnit = (u != null && !u.destroyed);
        });

        canvas.setOnMouseDragged(e -> {
            int tx = tileAt(e.getX()), ty = tileAt(e.getY());
            dragX1 = tx; dragY1 = ty;
            if (Math.abs(tx - dragX0) + Math.abs(ty - dragY0) > 1) dragging = true;
            redraw();
        });

        canvas.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.SECONDARY) return;
            int tx = tileAt(e.getX()), ty = tileAt(e.getY());
            if (dragging) {
                ctrl.selectInBox(Math.min(dragX0, dragX1), Math.min(dragY0, dragY1),
                                  Math.max(dragX0, dragX1), Math.max(dragY0, dragY1));
            } else {
                ctrl.click(tx, ty);
            }
            dragging = false; dragX0 = dragY0 = dragX1 = dragY1 = -1;
            redraw();
        });

        canvas.setOnScroll((ScrollEvent e) -> {
            if (e.isControlDown() || e.getDeltaY() != 0) {
                double factor = e.getDeltaY() > 0 ? 1.1 : 0.9;
                setZoom(zoom * factor);
                e.consume();
            }
        });

        // Lightweight edge-pan.
        canvas.setOnMouseMoved(e -> {
            double mx = e.getX(), my = e.getY();
            int tx = tileAt(mx), ty = tileAt(my);
            if (tx != hoverX || ty != hoverY) {
                hoverX = tx; hoverY = ty;
                redraw();
            }
            Bounds vb = scroll.getViewportBounds();
            double w = scroll.getWidth(), h = scroll.getHeight();
            double margin = 28 * zoom;
            if (mx < margin) scroll.setHvalue(scroll.getHvalue() - 0.02);
            else if (mx > w - margin) scroll.setHvalue(scroll.getHvalue() + 0.02);
            if (my < margin) scroll.setVvalue(scroll.getVvalue() - 0.02);
            else if (my > h - margin) scroll.setVvalue(scroll.getVvalue() + 0.02);
        });
    }

    private int hoverX = -1;
    private int hoverY = -1;

    public void drawGridOverlay() {}

    private void drawGrid(GraphicsContext g, GameState s, int ts) {
        g.setStroke(Color.web("#4fc3f7", 0.5));
        g.setLineWidth(0.5);
        
        for (int i = 0; i <= s.width(); i++) {
            g.strokeLine(i * ts, 0, i * ts, s.height() * ts);
        }
        for (int i = 0; i <= s.height(); i++) {
            g.strokeLine(0, i * ts, s.width() * ts, i * ts);
        }
        
        g.setFill(Color.web("#4fc3f7", 0.7));
        g.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        
        g.fillText("  ", -ts + 25, 15);
        for (int x = 0; x < s.width(); x += Math.max(1, s.width() / 10)) {
            String label = coordLabel(x, 0);
            g.fillText(label, x * ts + ts/2, 12);
        }
        
        for (int y = 0; y < s.height(); y += Math.max(1, s.height() / 10)) {
            String label = coordLabel(0, y);
            g.fillText(label, -ts + 25, y * ts + ts/2 + 4);
        }
    }

    private String coordLabel(int x, int y) {
        char rowLetter = (char)('A' + (y % 26));
        if (y >= 26) {
            rowLetter = (char)('A' + (y / 26) - 1);
            return String.format("%c%03d", rowLetter, x);
        }
        return String.format("%c%03d", rowLetter, x);
    }

    public void redraw() {
        GameState s = ctrl.state;
        if (s == null || s.grid == null) return;
        if (s.elevation != null && !showSatellite) {
            terrainImg = TerrainBaker.bake(s, tile);
            bakedState = s;
        } else if (showSatellite) {
            terrainImg = null;
            bakedState = s;
        }
        int w = s.width() * ts(), h = s.height() * ts();
        if ((int) canvas.getWidth() != w) canvas.setWidth(w);
        if ((int) canvas.getHeight() != h) canvas.setHeight(h);
        satelliteToggle.setVisible(s.elevation != null || satelliteImg != null);
        draw(s);
    }

    private void draw(GameState s) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        int ts = ts();
        g.setFill(Palette.BACKGROUND);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (showSatellite && satelliteImg != null) {
            g.drawImage(satelliteImg, 0, 0, canvas.getWidth(), canvas.getHeight());
        } else if (terrainImg != null) {
            g.drawImage(terrainImg, 0, 0, canvas.getWidth(), canvas.getHeight());
        } else {
            flatTerrain(g, s, ts);
        }

        if (!showSatellite) {
            drawOsmOverlays(g, s, ts);
        }

        if (s.smokeGrid != null) {
            for (int y = 0; y < s.height(); y++) {
                for (int x = 0; x < s.width(); x++) {
                    if (s.smokeGrid[y][x] > 0) {
                        g.setFill(Color.web("#a8b3af", 0.6 + 0.1 * (s.smokeGrid[y][x] % 2)));
                        g.fillOval(x * ts - ts/4, y * ts - ts/4, ts + ts/2, ts + ts/2);
                    }
                }
            }
        }

        for (int y = 0; y < s.height(); y++) {
            for (int x = 0; x < s.width(); x++) {
                Visibility v = s.visibility[y][x];
                if (v == Visibility.UNSEEN) {
                    g.setFill(Color.web("#05070a", 0.93));
                    g.fillRect(x * ts, y * ts, ts, ts);
                } else if (v == Visibility.PREVIOUSLY_SEEN) {
                    g.setFill(Color.web("#000000", 0.42));
                    g.fillRect(x * ts, y * ts, ts, ts);
                }
            }
        }

        drawGrid(g, s, ts);

        for (int[] r : ctrl.reachableTiles()) {
            g.setFill(Color.web("#69f0ae", 0.22));
            g.fillRect(r[0] * ts, r[1] * ts, ts, ts);
        }

        // Detection ring around selected unit (M10).
        for (Unit u : ctrl.selection) {
            if (u.destroyed) continue;
            g.setStroke(Color.web("#4fc3f7", 0.28));
            g.setLineWidth(1.5);
            g.strokeOval(u.x * ts + ts / 2 - u.reconRadius * ts,
                         u.y * ts + ts / 2 - u.reconRadius * ts,
                         u.reconRadius * 2 * ts, u.reconRadius * 2 * ts);
        }

        for (Unit u : s.friendlyUnits) if (!u.destroyed) drawUnit(g, u, false, s, ts);
        for (Unit u : s.enemyUnits) if (!u.destroyed && u.knownToPlayer) drawUnit(g, u, true, s, ts);

        for (Objective o : s.objectives) {
            g.setStroke(Palette.CAUTION);
            g.setLineWidth(2);
            g.strokeRect(o.x * ts + 3, o.y * ts + 3, ts - 6, ts - 6);
        }
        for (int[] d : s.supplyDepots) {
            g.setFill(Palette.SUCCESS);
            g.fillRect(d[0] * ts + ts / 2 - 4, d[1] * ts + ts - 6, 8, 5);
        }

        drawGhosts(g, s, ts);

        for (DelayedOrder d : s.delayedOrders) {
            Command a = d.command;
            int tx = -1, ty = -1;
            if (a instanceof CallArtilleryAction ca) { tx = ca.targetX(); ty = ca.targetY(); }
            else if (a instanceof CallCasAction ca) { tx = ca.targetX(); ty = ca.targetY(); }
            if (tx >= 0 && ty >= 0) {
                double pulse = 0.3 + 0.2 * Math.sin(System.currentTimeMillis() / 150.0);
                g.setFill(Color.web("#e04545", pulse));
                g.fillOval(tx * ts - ts, ty * ts - ts, ts * 3, ts * 3);
                g.setStroke(Color.web("#e04545", 0.8));
                g.setLineWidth(2.0);
                g.strokeOval(tx * ts - ts, ty * ts - ts, ts * 3, ts * 3);
            }
        }

        if (dragging && dragX0 >= 0) {
            int x = Math.min(dragX0, dragX1) * ts, y = Math.min(dragY0, dragY1) * ts;
            int ww = (Math.abs(dragX1 - dragX0) + 1) * ts, hh = (Math.abs(dragY1 - dragY0) + 1) * ts;
            g.setStroke(Color.web("#4fc3f7", 0.8));
            g.setLineWidth(1.5);
            g.setFill(Color.web("#4fc3f7", 0.12));
            g.fillRect(x, y, ww, hh);
            g.strokeRect(x, y, ww, hh);
        }

        if (ctrl.selected != null) {
            Unit u = ctrl.selected;
            g.setStroke(Palette.CAUTION);
            g.setLineWidth(2.5);
            g.strokeRect(u.x * ts + 1, u.y * ts + 1, ts - 2, ts - 2);
        }

        if (hoverX >= 0 && hoverX < s.width() && hoverY >= 0 && hoverY < s.height()) {
            Tile t = s.grid[hoverY][hoverX];
            String text = t.type.name() + "\nCover: " + t.coverBonus + "% Move: " + t.movementCost;
            if (ctrl.selected != null && !ctrl.selected.destroyed) {
                Unit e = s.enemyUnitAt(hoverX, hoverY);
                if (e != null && e.knownToPlayer && !e.destroyed) {
                    text += "\nTarget: " + e.profile.name();
                }
            }
            g.setFill(Color.web("#0e1117", 0.85));
            g.setStroke(Palette.BORDER);
            g.setLineWidth(1);
            g.fillRect(hoverX * ts + ts + 5, hoverY * ts - 15, 120, 50);
            g.strokeRect(hoverX * ts + ts + 5, hoverY * ts - 15, 120, 50);
            g.setFill(Palette.TEXT);
            g.fillText(text, hoverX * ts + ts + 10, hoverY * ts);
        }
    }

    private void flatTerrain(GraphicsContext g, GameState s, int ts) {
        for (int y = 0; y < s.height(); y++) {
            for (int x = 0; x < s.width(); x++) {
                g.setFill(Palette.terrain(s.grid[y][x].type.name()));
                g.fillRect(x * ts, y * ts, ts, ts);
            }
        }
    }

    private void drawOsmOverlays(GraphicsContext g, GameState s, int ts) {
        drawRoads(g, s, ts);
        drawBuildings(g, s, ts);
    }

    private void drawRoads(GraphicsContext g, GameState s, int ts) {
        for (RoadSegment road : s.roadSegments) {
            if (road.points() == null || road.points().size() < 2) continue;
            int cx = -1, cy = -1;
            for (double[] pt : road.points()) {
                int gx = lonToGrid(s, pt[0]);
                int gy = latToGrid(s, pt[1]);
                if (cx >= 0 && gy >= 0 && gy < s.height()) {
                    g.setStroke(Color.web("#3a3a3c", 0.9));
                    g.setLineWidth(ts * 0.35);
                    g.strokeLine(cx * ts + ts / 2, cy * ts + ts / 2, gx * ts + ts / 2, gy * ts + ts / 2);
                }
                cx = gx;
                cy = gy;
            }
        }
    }

    private void drawBuildings(GraphicsContext g, GameState s, int ts) {
        for (Building bldg : s.buildings) {
            if (bldg.footprint == null) continue;
            int[] xs = new int[bldg.footprint.size()];
            int[] ys = new int[bldg.footprint.size()];
            for (int i = 0; i < bldg.footprint.size(); i++) {
                double[] pt = bldg.footprint.get(i);
                xs[i] = lonToGrid(s, pt[0]) * ts;
                ys[i] = latToGrid(s, pt[1]) * ts;
            }
            if (xs.length >= 3) {
                g.setFill(Color.web("#4a4a4a", 0.85));
                g.fillPolygon(xs, ys, xs.length);
                g.setStroke(Color.web("#22211f", 0.9));
                g.setLineWidth(1);
                g.strokePolygon(xs, ys, xs.length);
            }
        }
    }

    private int lonToGrid(GameState s, double lon) {
        double lonPerTile = 360.0 / s.width();
        return (int) Math.round((lon - s.longitude + 180) / lonPerTile);
    }

    private int latToGrid(GameState s, double lat) {
        double latPerTile = 180.0 / s.height();
        return (int) Math.round((s.latitude - lat + 90) / latPerTile);
    }

    private void drawUnit(GraphicsContext g, Unit u, boolean enemy, GameState s, int ts) {
        int cx = u.x * ts + ts / 2, cy = u.y * ts + ts / 2;
        boolean stale = enemy && s.visibility[u.y][u.x] != Visibility.VISIBLE;
        Color fill = enemy ? (stale ? Color.web("#8695aa") : Palette.HOSTILE) : Palette.FRIENDLY;

        boolean isWestern = u.faction == Faction.USA || u.faction == Faction.CHINA;
        boolean isRussianStyle = u.faction == Faction.RUSSIA || u.faction == Faction.IRAN;

        g.setLineWidth(2);
        g.setStroke(fill);
        int r = ts / 2 - 4;

        if (enemy) {
            if (isRussianStyle) {
                g.beginPath();
                g.moveTo(cx, cy - r);
                g.lineTo(cx + r * 0.85, cy - r * 0.15);
                g.lineTo(cx + r, cy + r * 0.55);
                g.lineTo(cx + r * 0.15, cy + r);
                g.lineTo(cx - r, cy + r * 0.35);
                g.lineTo(cx - r, cy - r * 0.15);
                g.closePath();
                g.stroke();
            } else {
                g.beginPath();
                g.moveTo(cx, cy - r); g.lineTo(cx + r, cy); g.lineTo(cx, cy + r); g.lineTo(cx - r, cy);
                g.closePath(); g.stroke();
            }
        } else {
            if (isRussianStyle) {
                g.strokeOval(cx - r, cy - r, r * 2, r * 2);
            } else {
                g.strokeRect(u.x * ts + 4, u.y * ts + 4, ts - 8, ts - 8);
            }
        }

        Color glyphColor = enemy ? (stale ? Color.web("#a0b0b8") : fill) : fill;
        drawGlyph(g, u.profile.category(), cx, cy, r, glyphColor, enemy, isRussianStyle);

        int bw = ts - 8;
        int by = u.y * ts + ts - 7;
        g.setFill(Color.web("#000000", 0.5)); g.fillRect(u.x * ts + 4, by, bw, 2);
        g.setFill(healthColor(u.strength)); g.fillRect(u.x * ts + 4, by, (int) (bw * clamp01(u.strength / 100.0)), 2);
        int sy = by + 3;
        g.setFill(Color.web("#000000", 0.5)); g.fillRect(u.x * ts + 4, sy, bw, 2);
        g.setFill(Palette.CAUTION); g.fillRect(u.x * ts + 4, sy, (int) (bw * clamp01(u.suppression / 100.0)), 2);

        if (u.suppression >= 40) {
            g.setFill(Palette.CAUTION);
            g.fillRect(u.x * ts + ts - 9, u.y * ts + 3, 5, 5);
        }
        if (lowAmmo(u)) {
            g.setFill(u.totalAmmo() == 0 ? Palette.HOSTILE : Palette.CAUTION);
            g.fillRect(u.x * ts + ts - 9, u.y * ts + ts - 10, 5, 5);
        }
        if (u.entrenchment > 0) {
            g.setFill(Color.web("#8695aa"));
            g.fillRect(u.x * ts + 3, u.y * ts + ts - 14, u.entrenchment * 4, 4);
        }
    }

    private void drawGlyph(GraphicsContext g, UnitCategory cat, int cx, int cy, int r, Color c, boolean enemy, boolean russianStyle) {
        g.setStroke(c);
        g.setLineWidth(1.5);
        double s = r * 0.6;

        if (russianStyle) {
            drawRussianGlyph(g, cat, cx, cy, s, c);
        } else {
            drawNatoGlyph(g, cat, cx, cy, s, c);
        }
    }

    private void drawNatoGlyph(GraphicsContext g, UnitCategory cat, int cx, int cy, double s, Color c) {
        switch (cat) {
            case INFANTRY -> {
                g.strokeOval(cx - s * 0.28, cy - s * 0.55, s * 0.55, s * 0.55);
                g.beginPath(); g.moveTo(cx, cy - s * 0.1); g.lineTo(cx, cy + s * 0.6); g.stroke();
                g.beginPath(); g.moveTo(cx, cy + s * 0.1); g.lineTo(cx - s * 0.5, cy + s * 0.6); g.stroke();
                g.beginPath(); g.moveTo(cx, cy + s * 0.1); g.lineTo(cx + s * 0.5, cy + s * 0.6); g.stroke();
            }
            case RECON -> {
                g.beginPath(); g.moveTo(cx - s * 0.6, cy + s * 0.4);
                g.lineTo(cx, cy - s * 0.5); g.lineTo(cx + s * 0.6, cy + s * 0.4); g.stroke();
            }
            case ARMOR -> {
                g.beginPath(); g.moveTo(cx - s * 0.6, cy + s * 0.5); g.lineTo(cx + s * 0.6, cy + s * 0.5);
                g.lineTo(cx, cy - s * 0.6); g.closePath(); g.stroke();
            }
            case ARTILLERY -> {
                g.strokeOval(cx - s * 0.35, cy - s * 0.35, s * 0.7, s * 0.7);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    g.beginPath(); g.moveTo(cx + Math.cos(a) * s * 0.45, cy + Math.sin(a) * s * 0.45);
                    g.lineTo(cx + Math.cos(a) * s * 0.7, cy + Math.sin(a) * s * 0.7); g.stroke();
                }
            }
            case DRONE -> {
                g.strokeOval(cx - s * 0.6, cy - s * 0.6, s * 0.35, s * 0.35);
                g.strokeOval(cx + s * 0.25, cy - s * 0.6, s * 0.35, s * 0.35);
                g.strokeOval(cx - s * 0.6, cy + s * 0.25, s * 0.35, s * 0.35);
                g.strokeOval(cx + s * 0.25, cy + s * 0.25, s * 0.35, s * 0.35);
                g.beginPath(); g.moveTo(cx - s * 0.42, cy - s * 0.42); g.lineTo(cx + s * 0.42, cy + s * 0.42);
                g.moveTo(cx + s * 0.42, cy - s * 0.42); g.lineTo(cx - s * 0.42, cy + s * 0.42); g.stroke();
            }
            case ENGINEER -> {
                g.beginPath(); g.moveTo(cx - s * 0.6, cy - s * 0.1); g.lineTo(cx + s * 0.6, cy - s * 0.1);
                g.moveTo(cx - s * 0.1, cy - s * 0.6); g.lineTo(cx - s * 0.1, cy + s * 0.6); g.stroke();
                g.strokeOval(cx - s * 0.22, cy - s * 0.22, s * 0.22, s * 0.22);
            }
            case AIR_DEFENSE -> {
                g.beginPath(); g.moveTo(cx - s * 0.6, cy + s * 0.4); g.lineTo(cx, cy - s * 0.5);
                g.lineTo(cx + s * 0.6, cy + s * 0.4);
                g.moveTo(cx, cy - s * 0.5); g.lineTo(cx, cy + s * 0.6); g.stroke();
            }
            case LOGISTICS -> {
                g.strokeRect(cx - s * 0.5, cy - s * 0.4, s, s * 0.8);
                g.beginPath(); g.moveTo(cx - s * 0.5, cy - s * 0.1); g.lineTo(cx + s * 0.5, cy - s * 0.1); g.stroke();
            }
        }
    }

    private void drawRussianGlyph(GraphicsContext g, UnitCategory cat, int cx, int cy, double s, Color c) {
        switch (cat) {
            case INFANTRY -> {
                g.strokeRect(cx - s * 0.4, cy - s * 0.5, s * 0.8, s * 1.0);
                g.strokeLine(cx - s * 0.3, cy + s * 0.3, cx + s * 0.3, cy + s * 0.3);
            }
            case RECON -> {
                g.beginPath(); g.moveTo(cx - s * 0.5, cy - s * 0.4);
                g.lineTo(cx, cy - s * 0.7); g.lineTo(cx + s * 0.5, cy - s * 0.4); g.stroke();
            }
            case ARMOR -> {
                g.beginPath(); g.moveTo(cx - s * 0.5, cy + s * 0.4); g.lineTo(cx + s * 0.5, cy + s * 0.4);
                g.lineTo(cx, cy - s * 0.5); g.closePath(); g.stroke();
            }
            case ARTILLERY -> {
                g.strokeOval(cx - s * 0.4, cy - s * 0.4, s * 0.8, s * 0.8);
                g.strokeLine(cx - s * 0.2, cy, cx + s * 0.2, cy);
                g.strokeLine(cx, cy - s * 0.2, cx, cy + s * 0.2);
            }
            case DRONE -> {
                g.strokeOval(cx - s * 0.35, cy - s * 0.35, s * 0.7, s * 0.7);
                g.strokeRect(cx - s * 0.15, cy - s * 0.15, s * 0.3, s * 0.3);
            }
            case ENGINEER -> {
                g.beginPath(); g.moveTo(cx - s * 0.5, cy - s * 0.1); g.lineTo(cx + s * 0.5, cy - s * 0.1);
                g.moveTo(cx - s * 0.1, cy - s * 0.5); g.lineTo(cx - s * 0.1, cy + s * 0.5); g.stroke();
                g.strokeOval(cx - s * 0.2, cy - s * 0.2, s * 0.25, s * 0.25);
            }
            case AIR_DEFENSE -> {
                g.beginPath(); g.moveTo(cx - s * 0.5, cy + s * 0.3); g.lineTo(cx, cy - s * 0.5);
                g.lineTo(cx + s * 0.5, cy + s * 0.3); g.stroke();
                g.strokeRect(cx - s * 0.15, cy - s * 0.3, s * 0.3, s * 0.6);
            }
            case LOGISTICS -> {
                g.strokeRect(cx - s * 0.4, cy - s * 0.3, s * 0.8, s * 0.6);
                g.strokeLine(cx - s * 0.3, cy, cx + s * 0.3, cy);
            }
        }
    }

    private void drawGhosts(GraphicsContext g, GameState s, int ts) {
        for (var order : s.delayedOrders) {
            var a = order.command;
            if (a instanceof MoveAction m) {
                Unit u = s.friendlyById(m.unitId());
                if (u == null) continue;
                g.setLineWidth(1.5);
                g.setStroke(Color.web("#d1a34f", 0.5));
                g.strokeLine(u.x * ts + ts / 2, u.y * ts + ts / 2, m.targetX() * ts + ts / 2, m.targetY() * ts + ts / 2);
                g.strokeRect(m.targetX() * ts + 4, m.targetY() * ts + 4, ts - 8, ts - 8);
            } else if (a instanceof AttackAction at) {
                Unit u = s.friendlyById(at.unitId());
                Unit e = s.enemyById(at.targetUnitId());
                if (u == null || e == null) continue;
                g.setLineWidth(1.5);
                g.setStroke(Color.web("#d1594f", 0.55));
                g.strokeLine(u.x * ts + ts / 2, u.y * ts + ts / 2, e.x * ts + ts / 2, e.y * ts + ts / 2);
                reticle(g, e.x * ts + ts / 2, e.y * ts + ts / 2, ts / 2 - 3);
            } else if (a instanceof ReconAction rc) {
                Unit u = s.friendlyById(rc.unitId());
                if (u == null) continue;
                g.setLineWidth(1.5);
                g.setStroke(Color.web("#6fbf73", 0.5));
                g.strokeOval(u.x * ts + ts / 2 - rc.radius() * ts, u.y * ts + ts / 2 - rc.radius() * ts,
                             rc.radius() * 2 * ts, rc.radius() * 2 * ts);
            } else if (a instanceof CallCasAction c) {
                g.setLineWidth(1.5);
                g.setStroke(Color.web("#d1594f", 0.5));
                reticle(g, c.targetX() * ts + ts / 2, c.targetY() * ts + ts / 2, ts / 2 - 3);
            } else if (a instanceof ResupplyAction rs) {
                Unit u = s.friendlyById(rs.unitId());
                if (u == null) continue;
                g.setLineWidth(1.5);
                g.setStroke(Color.web("#6fbf73", 0.5));
                g.strokeRect(u.x * ts + 6, u.y * ts + 6, ts - 12, ts - 12);
            }
        }
    }

    private void reticle(GraphicsContext g, int cx, int cy, int r) {
        g.strokeLine(cx - r, cy, cx + r, cy);
        g.strokeLine(cx, cy - r, cx, cy + r);
    }

    private boolean lowAmmo(Unit u) {
        int max = u.weapons.stream().mapToInt(w -> w.maxAmmo).sum();
        if (max == 0) return false;
        return (double) u.totalAmmo() / max < 0.25;
    }

    private Color healthColor(double strength) {
        if (strength > 60) return Palette.SUCCESS;
        if (strength > 30) return Palette.CAUTION;
        return Palette.HOSTILE;
    }

    private double clamp01(double d) { return Math.max(0, Math.min(1, d)); }
}
