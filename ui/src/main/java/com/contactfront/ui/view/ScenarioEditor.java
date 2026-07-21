package com.contactfront.ui.view;

import com.contactfront.engine.model.*;
import com.contactfront.engine.model.Obstacle.ObstacleType;
import com.contactfront.engine.model.TacticalGraphics.GraphicType;
import com.contactfront.engine.data.Profiles;
import com.contactfront.ui.Log;
import com.contactfront.ui.controller.GameController;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ScenarioEditor {
    private final Stage owner;
    private final Stage stage;
    private final Consumer<ScenarioEditorData> onScenarioCreated;
    
    public record ScenarioEditorData(
        String scenarioName,
        String description,
        Faction playerFaction,
        Faction enemyFaction,
        CommandMode commandMode,
        Doctrine playerDoctrine,
        Doctrine enemyDoctrine,
        GameState state
    ) {}
    
    private GameController ctrl;
    private Faction placingFaction = Faction.USA;
    private String placingUnitType = "inf_squad";
    private boolean editMode = true;
    private MapView mapView;
    private EditorMode editorMode = EditorMode.UNIT_PLACEMENT;
    private List<double[]> drawingPoints = new ArrayList<>();

    public enum EditorMode {
        UNIT_PLACEMENT, OBSTACLE_BRUSH, TACTICAL_DRAWING
    }

    public ScenarioEditor(Stage owner, GameController ctrl, Consumer<ScenarioEditorData> onScenarioCreated) {
        this.owner = owner;
        this.stage = new Stage();
        this.stage.initOwner(owner);
        this.stage.initModality(Modality.WINDOW_MODAL);
        this.ctrl = ctrl;
        this.onScenarioCreated = onScenarioCreated;
    }

    public void show() {
        try {
            BorderPane root = buildEditor();
            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.setTitle("Scenario Editor");
            Log.info("Scenario editor shown");
            stage.show();
            stage.toFront();
            stage.requestFocus();
        } catch (Exception ex) {
            Log.error("Scenario editor failed to open: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

private BorderPane buildEditor() {
        VBox toolbar = new VBox(5);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.TOP_CENTER);
        toolbar.setStyle("-fx-background-color:#151a23;");

        Label title = new Label("Editor");
        title.setStyle("-fx-text-fill:#4fc3f7; -fx-font-size:16px;");

        ToggleGroup modeGroup = new ToggleGroup();
        ToggleButton unitModeBtn = new ToggleButton("Units");
        unitModeBtn.setToggleGroup(modeGroup);
        unitModeBtn.setSelected(true);
        unitModeBtn.setOnAction(e -> editorMode = EditorMode.UNIT_PLACEMENT);

        ToggleButton obstacleModeBtn = new ToggleButton("Obstacles");
        obstacleModeBtn.setToggleGroup(modeGroup);
        obstacleModeBtn.setOnAction(e -> editorMode = EditorMode.OBSTACLE_BRUSH);

        ToggleButton drawModeBtn = new ToggleButton("Draw");
        drawModeBtn.setToggleGroup(modeGroup);
        drawModeBtn.setOnAction(e -> editorMode = EditorMode.TACTICAL_DRAWING);

        Button finishDrawBtn = new Button("Finish Draw");
        finishDrawBtn.setOnAction(e -> finishDrawing());

        ToggleButton playerToggle = new ToggleButton("Side A");
        playerToggle.setSelected(true);
        playerToggle.setOnAction(e -> placingFaction = Faction.USA);
        ToggleButton enemyToggle = new ToggleButton("Side B");
        enemyToggle.setOnAction(e -> placingFaction = Faction.RUSSIA);

        VBox unitsBox = new VBox(3);
        if (ctrl.profiles == null) {
            ctrl.profiles = Profiles.load();
        }
        String[] unitTypes = ctrl.profiles.allUnits().stream()
            .map(p -> p.id())
            .toArray(String[]::new);
        for (String type : unitTypes) {
            ToggleButton btn = new ToggleButton(type.replace("_", " "));
            btn.setUserData(type);
            btn.setMinWidth(100);
            btn.setOnAction(e -> placingUnitType = (String) btn.getUserData());
            if (type.equals("inf_squad")) btn.setSelected(true);
            unitsBox.getChildren().add(btn);
        }
        if (!unitsBox.getChildren().isEmpty()) {
            unitsBox.getChildren().get(0).setStyle("-fx-background-color:#4fc3f7;");
        }

        Button doneBtn = new Button("Done");
        doneBtn.setOnAction(e -> finishEditing());

        toolbar.getChildren().addAll(title, new Label("Mode:"), unitModeBtn, obstacleModeBtn, drawModeBtn,
                finishDrawBtn, new Label("Faction:"), playerToggle, enemyToggle,
                new Label("Units:"), unitsBox, doneBtn);

        if (ctrl == null) {
            ctrl = new GameController();
            ctrl.profiles = Profiles.load();
        }
        try {
            ctrl.newGame(System.nanoTime());
        } catch (Exception ex) {
            Log.error("ScenarioEditor: newGame failed: " + ex.getMessage());
            ex.printStackTrace();
        }
        mapView = new MapView(ctrl, 30, (x, y) -> placeUnit(x, y));
        Log.info("ScenarioEditor: mapView created, state=" + (ctrl.state != null ? "ready w=" + ctrl.state.width() : "null"));

        ScrollPane mapScroll = mapView.scrollPane();
        mapScroll.setStyle("-fx-background-color:#0e1117;");

        BorderPane bp = new BorderPane();
        bp.setLeft(toolbar);
        bp.setCenter(mapScroll);
        return bp;
    }
    
    private void placeUnit(int tx, int ty) {
        if (!editMode || ctrl.state == null) return;
        if (tx < 0 || ty < 0 || tx >= ctrl.state.width() || ty >= ctrl.state.height()) return;

        if (editorMode == EditorMode.OBSTACLE_BRUSH) {
            placeObstacle(tx, ty);
            return;
        }
        if (editorMode == EditorMode.TACTICAL_DRAWING) {
            addDrawingPoint(tx, ty);
            return;
        }

        Tile tile = ctrl.state.grid[ty][tx];
        if (tile.impassable()) return;

        // Check placed units too (editor mode)
        if (ctrl.state.friendlyUnitAt(tx, ty) != null || ctrl.state.enemyUnitAt(tx, ty) != null) return;
        boolean hasPlacedUnit = false;
        for (Unit u : ctrl.state.placedUnits) {
            if (u.x == tx && u.y == ty) { hasPlacedUnit = true; break; }
        }
        if (hasPlacedUnit) return;

        UnitProfile profile = ctrl.profiles.unit(placingUnitType);
        if (profile == null) return;

        int id = ctrl.state.placedUnits.size() + 1;
        Unit unit = new Unit(id, placingFaction, profile, tx, ty, ctrl.profiles);
        unit.sidcCode = SymbolRegistry.getSidcForUnit(placingUnitType, placingFaction).code();

        ctrl.state.placedUnits.add(unit);

        mapView.redraw();
    }

    private void placeObstacle(int tx, int ty) {
        if (ctrl.state == null) return;
        ctrl.state.obstacles.add(new Obstacle(tx, ty, ObstacleType.MINEFIELD));
        mapView.redraw();
    }

    private void addDrawingPoint(int tx, int ty) {
        drawingPoints.add(new double[]{tx, ty});
    }

    private void finishDrawing() {
        if (drawingPoints.size() >= 2 && ctrl.state != null) {
            ctrl.state.tacticalGraphics.addPolygon(GraphicType.PHASE_LINE, drawingPoints);
        }
        drawingPoints.clear();
    }
    
    private void finishEditing() {
        Log.info("Scenario editor finished - units placed: " + ctrl.state.placedUnits.size());
        
        ScenarioEditorData data = new ScenarioEditorData(
            "Custom Scenario",
            "Created in editor",
            placingFaction,
            null,
            CommandMode.DOCTRINE,
            Doctrine.NATO,
            Doctrine.RUSSIAN,
            ctrl.state
        );
        onScenarioCreated.accept(data);
        stage.close();
    }
}