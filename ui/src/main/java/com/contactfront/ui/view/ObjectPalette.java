package com.contactfront.ui.view;

import com.contactfront.engine.model.Terrain;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.function.Consumer;

public class ObjectPalette {
    private final VBox root;
    private final Consumer<PaletteItem> onItemDragged;
    private final ScenarioBuilder.TerrainSelectionListener terrainListener;

    public record PaletteItem(String name, String type, Object data, String iconColor) {}

    public ObjectPalette(Consumer<PaletteItem> onItemDragged, 
                         ScenarioBuilder.TerrainSelectionListener terrainListener) {
        this.onItemDragged = onItemDragged;
        this.terrainListener = terrainListener;
        this.root = buildPalette();
    }

    public VBox getNode() {
        return root;
    }

    private VBox buildPalette() {
        VBox palette = new VBox(10);
        palette.setPadding(new Insets(10));
        palette.setAlignment(Pos.TOP_CENTER);
        palette.setStyle("-fx-background-color: #151a23; -fx-border-color: #3a5067;");
        palette.setPrefWidth(180);

        Label title = new Label("Object Palette");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4fc3f7;");

        // Terrain Types Section
        Label terrainLabel = new Label("Terrain Types");
        terrainLabel.setStyle("-fx-text-fill: #e0e6ed; -fx-font-weight: bold;");

        HBox terrainBox = new HBox(5);
        terrainBox.setAlignment(Pos.CENTER);
        terrainBox.getChildren().addAll(
            createDraggableItem("Grass", "terrain", Terrain.OPEN, "#467832"),
            createDraggableItem("Dirt", "terrain", Terrain.DIRT, "#82643C"),
            createDraggableItem("Rock", "terrain", Terrain.HILL, "#787878"),
            createDraggableItem("Water", "terrain", Terrain.WATER, "#1E3C78")
        );

        // Vegetation Section
        Label vegLabel = new Label("Vegetation");
        vegLabel.setStyle("-fx-text-fill: #e0e6ed; -fx-font-weight: bold;");

        HBox vegBox = new HBox(5);
        vegBox.setAlignment(Pos.CENTER);
        vegBox.getChildren().addAll(
            createDraggableItem("Tree", "vegetation", "tree", "#284628"),
            createDraggableItem("Bush", "vegetation", "bush", "#325028"),
            createDraggableItem("Scrub", "vegetation", "scrub", "#607030")
        );

        // Structures Section
        Label structLabel = new Label("Structures");
        structLabel.setStyle("-fx-text-fill: #e0e6ed; -fx-font-weight: bold;");

        HBox structBox = new HBox(5);
        structBox.setAlignment(Pos.CENTER);
        structBox.getChildren().addAll(
            createDraggableItem("Building", "structure", "building", "#646464"),
            createDraggableItem("Road", "structure", "road", "#505050"),
            createDraggableItem("Bridge", "structure", "bridge", "#707070")
        );

        palette.getChildren().addAll(
            title, new Separator(),
            terrainLabel, terrainBox,
            vegLabel, vegBox,
            structLabel, structBox
        );

        return palette;
    }

    private Button createDraggableItem(String name, String type, Object data, String colorHex) {
        Button btn = new Button(name);
        btn.setPrefWidth(60);
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-size: 11px;");

        btn.setOnMousePressed((MouseEvent e) -> {
            if (e.isPrimaryButtonDown()) {
                PaletteItem item = new PaletteItem(name, type, data, colorHex);
                onItemDragged.accept(item);
            }
        });

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + adjustColor(colorHex, 20) + "; -fx-text-fill: white; -fx-font-size: 11px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-size: 11px;"));

        return btn;
    }

    private String adjustColor(String hex, int delta) {
        int r = Math.max(0, Math.min(255, Integer.parseInt(hex.substring(1, 3), 16) + delta));
        int g = Math.max(0, Math.min(255, Integer.parseInt(hex.substring(3, 5), 16) + delta));
        int b = Math.max(0, Math.min(255, Integer.parseInt(hex.substring(5, 7), 16) + delta));
        return String.format("#%02x%02x%02x", r, g, b);
    }
}