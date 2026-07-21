package com.contactfront.ui.view;

import com.contactfront.engine.model.Faction;
import com.contactfront.ui.Log;
import com.contactfront.ui.assets.MapTilerClient;
import com.contactfront.ui.model.SetupData;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class SetupScreen {
    private final Stage stage;
    private final Consumer<SetupData> onConfirm;
    private final Runnable onCancel;

    public SetupScreen(Stage owner, Consumer<SetupData> onConfirm, Runnable onCancel) {
        this.stage = new Stage();
        this.stage.initOwner(owner);
        this.stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    public void show() {
        VBox root = buildSetup();
        Scene scene = new Scene(root, 500, 500);
        stage.setScene(scene);
        stage.setTitle("Battle Setup");
        stage.centerOnScreen();
        stage.show();
    }

    private VBox buildSetup() {
        Label title = new Label("BATTLE SETUP");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4fc3f7;");

        // Faction selectors
        ComboBox<Faction> playerFactionBox = new ComboBox<>();
        playerFactionBox.getItems().addAll(Faction.values());
        playerFactionBox.setValue(Faction.USA);
        playerFactionBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Faction f) { return f == null ? "" : f.name(); }
            @Override public Faction fromString(String s) { return Faction.valueOf(s); }
        });

        ComboBox<Faction> enemyFactionBox = new ComboBox<>();
        enemyFactionBox.getItems().addAll(Faction.values());
        enemyFactionBox.setValue(Faction.RUSSIA);
        enemyFactionBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Faction f) { return f == null ? "" : f.name(); }
            @Override public Faction fromString(String s) { return Faction.valueOf(s); }
        });

        // Prevent same faction
        enemyFactionBox.getItems().removeIf(f -> f == playerFactionBox.getValue());
        playerFactionBox.valueProperty().addListener((obs, old, neu) -> {
            if (neu != null && enemyFactionBox.getValue() == neu) {
                if (neu == Faction.USA) enemyFactionBox.setValue(Faction.RUSSIA);
                else if (neu == Faction.RUSSIA) enemyFactionBox.setValue(Faction.USA);
                else enemyFactionBox.setValue(Faction.USA);
            }
            enemyFactionBox.getItems().remove(neu);
            if (old != null) enemyFactionBox.getItems().add(old);
        });

        // Scenario mode
        ToggleGroup routeGroup = new ToggleGroup();
        RadioButton randomLoc = new RadioButton("Random Location");
        RadioButton curatedLoc = new RadioButton("Choose Location");
        RadioButton procedural = new RadioButton("Procedural Terrain");
        randomLoc.setToggleGroup(routeGroup);
        curatedLoc.setToggleGroup(routeGroup);
        procedural.setToggleGroup(routeGroup);
        randomLoc.setUserData(SetupData.Route.RANDOM_LOCATION);
        curatedLoc.setUserData(SetupData.Route.CURATED_LOCATION);
        procedural.setUserData(SetupData.Route.PROCEDURAL);
        randomLoc.setSelected(true);

        // Difficulty
        ComboBox<SetupData.Difficulty> diffBox = new ComboBox<>();
        diffBox.getItems().addAll(SetupData.Difficulty.values());
        diffBox.setValue(SetupData.Difficulty.NORMAL);

        // Environment
        CheckBox nightCb = new CheckBox("Night");
        CheckBox rainCb = new CheckBox("Rain");
        CheckBox windCb = new CheckBox("Wind");
        rainCb.selectedProperty().addListener((obs, old, neu) -> {
            if (neu) Log.info("Setup: Rain selected - affects visibility and suppression decay");
        });

        // MapTiler status
        Label mapStatus = new Label();
        mapStatus.setStyle("-fx-text-fill: #4fc3f7; -fx-font-size: 11px;");
        if (MapTilerClient.getApiKey().isEmpty()) {
            mapStatus.setText("Satellite imagery unavailable - will use procedural terrain");
        } else {
            mapStatus.setText("Satellite imagery ready");
        }

        Button confirmBtn = new Button("Confirm");
        Button cancelBtn = new Button("Cancel");

        confirmBtn.setOnAction(e -> {
            SetupData data = new SetupData(
                playerFactionBox.getValue(),
                enemyFactionBox.getValue(),
                diffBox.getValue(),
                nightCb.isSelected(),
                rainCb.isSelected(),
                windCb.isSelected(),
                (SetupData.Route) routeGroup.getSelectedToggle().getUserData()
            );
            Log.info("SetupScreen: Confirmed " + data.route() + " " + data.playerFaction() + " vs " + data.enemyFaction());
            onConfirm.accept(data);
            stage.close();
        });

        cancelBtn.setOnAction(e -> {
            onCancel.run();
            stage.close();
        });

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: #0e1117;");
        content.getChildren().addAll(
            title,
            new Separator(),
            new Label("Factions"),
            new HBox(10, new Label("Player:"), playerFactionBox),
            new HBox(10, new Label("Enemy:"), enemyFactionBox),
            new Separator(),
            new Label("Scenario"),
            randomLoc, curatedLoc, procedural,
            new Separator(),
            new Label("Difficulty"),
            diffBox,
            new Separator(),
            new Label("Environment"),
            nightCb, rainCb, windCb,
            new Separator(),
            mapStatus,
            new Separator(),
            new HBox(10, confirmBtn, cancelBtn)
        );

        styleButton(confirmBtn);
        styleButton(cancelBtn);

        return content;
    }

    private void styleButton(Button btn) {
        btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 14px; -fx-pref-width: 100px; -fx-border-color: #5a6e82;");
    }
}