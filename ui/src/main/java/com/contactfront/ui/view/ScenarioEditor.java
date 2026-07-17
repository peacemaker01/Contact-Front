package com.contactfront.ui.view;

import com.contactfront.engine.model.CommandMode;
import com.contactfront.engine.model.Doctrine;
import com.contactfront.engine.model.Faction;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Tile;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.UnitProfile;
import com.contactfront.engine.data.Profiles;
import com.contactfront.ui.controller.GameController;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class ScenarioEditor {
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
    
    public ScenarioEditor(Stage stage, GameController ctrl, Consumer<ScenarioEditorData> onScenarioCreated) {
        this.stage = stage;
        this.ctrl = ctrl;
        this.onScenarioCreated = onScenarioCreated;
    }

    public void show() {
        BorderPane root = buildEditor();
        Scene scene = new Scene(root, 1200, 800);
        stage.setScene(scene);
        stage.setTitle("Contact Front — Scenario Editor");
        stage.centerOnScreen();
        stage.show();
    }

    private BorderPane buildEditor() {
        // Toolbar with unit types
        VBox toolbar = new VBox(5);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.TOP_CENTER);
        toolbar.setStyle("-fx-background-color:#151a23;");
        
        Label title = new Label("Editor");
        title.setStyle("-fx-text-fill:#4fc3f7; -fx-font-size:16px;");
        
        ToggleButton playerToggle = new ToggleButton("Player");
        playerToggle.setSelected(true);
        playerToggle.setOnAction(e -> placingFaction = Faction.USA);
        ToggleButton enemyToggle = new ToggleButton("Enemy");
        enemyToggle.setOnAction(e -> placingFaction = Faction.RUSSIA);
        
        String[] unitTypes = {"inf_squad", "mbt", "ifv", "aa_team", "engineer_squad"};
        
        VBox unitsBox = new VBox(3);
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
        
        toolbar.getChildren().addAll(title, new Label("Faction:"), playerToggle, enemyToggle, 
                                   new Label("Units:"), unitsBox, doneBtn);
        
        // Initialize controller if needed
        if (ctrl == null) {
            ctrl = new GameController();
        }
        ctrl.newGame(System.nanoTime());
        mapView = new MapView(ctrl, 30, (x, y) -> placeUnit(x, y));
        
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
        
        Tile tile = ctrl.state.grid[ty][tx];
        if (tile.impassable()) return;
        
        // Check if unit already there
        if (ctrl.state.friendlyUnitAt(tx, ty) != null || ctrl.state.enemyUnitAt(tx, ty) != null) return;
        
        UnitProfile profile = ctrl.profiles.unit(placingUnitType);
        if (profile == null) return;
        
        int id = ctrl.state.friendlyUnits.size() + ctrl.state.enemyUnits.size() + 1;
        Unit unit = new Unit(id, placingFaction, profile, tx, ty, ctrl.profiles);
        
        if (placingFaction == ctrl.state.playerFaction) {
            ctrl.state.friendlyUnits.add(unit);
        } else {
            ctrl.state.enemyUnits.add(unit);
        }
        
        mapView.redraw();
    }
    
    private void finishEditing() {
        ScenarioEditorData data = new ScenarioEditorData(
            "Custom Scenario",
            "Created in editor",
            ctrl.state.playerFaction,
            ctrl.state.enemyFaction,
            CommandMode.DOCTRINE,
            Doctrine.NATO,
            Doctrine.RUSSIAN,
            ctrl.state
        );
        onScenarioCreated.accept(data);
        stage.close();
    }
}