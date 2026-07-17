package com.contactfront.ui.view;

import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.Tile;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.UnitProfile;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MapEditor {
    private final Canvas canvas;
    private final int tileSize = 32;
    private final int width;
    private final int height;
    private final Consumer<MapEditEvent> onEvent;
    private final Tile[][] grid;
    private ObjectPalette currentTool;

    public enum ToolType { TERRAIN, UNIT, STRUCTURE, NONE }

    public record MapEditEvent(ToolType tool, int x, int y, Object data) {}

    public MapEditor(int width, int height, Consumer<MapEditEvent> onEvent) {
        this.width = width;
        this.height = height;
        this.onEvent = onEvent;
        this.grid = new Tile[height][width];
        this.canvas = new Canvas(width * tileSize, height * tileSize);
        initializeGrid();
        setupCanvas();
    }

    private void initializeGrid() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = new Tile(Terrain.OPEN, x, y);
            }
        }
    }

    private void setupCanvas() {
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleClick);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleDrag);
    }

    private void handleClick(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) return;
        
        int tx = (int) (e.getX() / tileSize);
        int ty = (int) (e.getY() / tileSize);
        
        if (tx < 0 || tx >= width || ty < 0 || ty >= height) return;

        if (currentTool != null) {
            onEvent.accept(new MapEditEvent(ToolType.TERRAIN, tx, ty, currentTool));
        }
    }

    private void handleDrag(MouseEvent e) {
        int tx = (int) (e.getX() / tileSize);
        int ty = (int) (e.getY() / tileSize);
        
        if (tx < 0 || tx >= width || ty < 0 || ty >= height) return;

        if (currentTool != null) {
            onEvent.accept(new MapEditEvent(ToolType.TERRAIN, tx, ty, currentTool));
        }
    }

    public void setTool(ObjectPalette palette) {
        this.currentTool = palette;
    }

    public void setToolType(ToolType type) {
        // Tool type is now determined by the palette selection
    }

    public void setTerrain(int x, int y, Terrain terrain) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            grid[y][x] = new Tile(terrain, x, y);
            redraw();
        }
    }

    public void addUnit(int x, int y, UnitProfile profile) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            // Unit placement logic
            redraw();
        }
    }

    public VBox getNode() {
        VBox container = new VBox();
        container.setAlignment(Pos.CENTER);
        container.getChildren().add(canvas);
        return container;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public Tile[][] getGrid() {
        return grid;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Terrain t = grid[y][x].type;
                drawTile(gc, x, y, t);
            }
        }
    }

    private void drawTile(GraphicsContext gc, int x, int y, Terrain terrain) {
        double px = x * tileSize;
        double py = y * tileSize;

        Color fillColor = switch (terrain) {
            case OPEN -> Color.LIGHTGREEN;
            case DIRT -> Color.SADDLEBROWN;
            case HILL -> Color.DARKGRAY;
            case WATER -> Color.DEEPSKYBLUE;
            case FOREST -> Color.DARKGREEN;
            case BUSH -> Color.FORESTGREEN;
            case SCRUB -> Color.YELLOWGREEN;
            case BUILDING -> Color.SIENNA;
            case ROAD -> Color.SLATEGRAY;
            default -> Color.LIGHTGRAY;
        };

        gc.setFill(fillColor);
        gc.fillRect(px, py, tileSize, tileSize);

        gc.setStroke(Color.BLACK);
        gc.strokeRect(px, py, tileSize, tileSize);
    }

    public Point2D screenToTile(double screenX, double screenY) {
        return new Point2D((int) (screenX / tileSize), (int) (screenY / tileSize));
    }

    public double tileToScreenX(int tileX) {
        return tileX * tileSize;
    }

    public double tileToScreenY(int tileY) {
        return tileY * tileSize;
    }
}