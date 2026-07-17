package com.contactfront.ui.view;

import com.contactfront.engine.model.CommandMode;
import com.contactfront.engine.model.Doctrine;
import com.contactfront.engine.model.Faction;
import com.contactfront.engine.model.Terrain;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class ScenarioBuilder {
    private final Stage stage;
    private final Consumer<ScenarioBuilderData> onScenarioCreated;
    private final TerrainSelectionListener selectionListener;

    public interface TerrainSelectionListener {
        void onTerrainSelected(Terrain terrain);
    }

    public record ScenarioBuilderData(
        String scenarioName,
        String description,
        Faction playerFaction,
        Faction enemyFaction,
        CommandMode commandMode,
        Doctrine playerDoctrine,
        Doctrine enemyDoctrine,
        String locationName,
        double latitude,
        double longitude,
        int width,
        int height,
        String notes
    ) {}

    public ScenarioBuilder(Stage stage, Consumer<ScenarioBuilderData> onScenarioCreated, TerrainSelectionListener listener) {
        this.stage = stage;
        this.onScenarioCreated = onScenarioCreated;
        this.selectionListener = listener;
    }

    public void show() {
        show(Faction.USA, Faction.RUSSIA);
    }

    public void show(Faction defaultPlayer, Faction defaultEnemy) {
        VBox root = buildBuilder(defaultPlayer, defaultEnemy);
        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.setTitle("Scenario Builder -- Eden Editor");
        stage.centerOnScreen();
        stage.show();
    }

    private VBox buildBuilder(Faction defaultPlayer, Faction defaultEnemy) {
        Label title = new Label("Scenario Builder");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #4fc3f7;");

        TextField nameField = new TextField("New Scenario");
        nameField.setPromptText("Scenario Name");

        TextArea descArea = new TextArea("");
        descArea.setPromptText("Description");
        descArea.setPrefHeight(80);

        HBox factionBox = new HBox(10);
        factionBox.setAlignment(Pos.CENTER_LEFT);
        ComboBox<Faction> playerFactionCombo = createFactionSelector(defaultPlayer);
        ComboBox<Faction> enemyFactionCombo = createFactionSelector(defaultEnemy);
        factionBox.getChildren().addAll(
            new Label("Player Faction:"), playerFactionCombo,
            new Label("Enemy Faction:"), enemyFactionCombo
        );

        HBox commandModeBox = new HBox(10);
        commandModeBox.setAlignment(Pos.CENTER_LEFT);
        ComboBox<CommandMode> commandModeCombo = createCommandModeSelector(CommandMode.EXPLICIT);
        commandModeBox.getChildren().addAll(
            new Label("Command Mode:"), commandModeCombo
        );

        HBox doctrineBox = new HBox(10);
        doctrineBox.setAlignment(Pos.CENTER_LEFT);
        ComboBox<Doctrine> playerDoctrineCombo = createDoctrineSelector(Doctrine.NATO);
        ComboBox<Doctrine> enemyDoctrineCombo = createDoctrineSelector(Doctrine.RUSSIAN);
        doctrineBox.getChildren().addAll(
            new Label("Player Doctrine:"), playerDoctrineCombo,
            new Label("Enemy Doctrine:"), enemyDoctrineCombo
        );

        HBox locationBox = new HBox(10);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        TextField locNameField = new TextField("Location");
        TextField latField = new TextField("35.0");
        TextField lonField = new TextField("-120.0");
        locationBox.getChildren().addAll(
            new Label("Location:"), locNameField,
            new Label("Lat:"), latField,
            new Label("Lon:"), lonField
        );

        HBox mapSizeBox = new HBox(10);
        mapSizeBox.setAlignment(Pos.CENTER_LEFT);
        Spinner<Integer> widthSpinner = new Spinner<>(20, 100, 40);
        Spinner<Integer> heightSpinner = new Spinner<>(10, 60, 30);
        mapSizeBox.getChildren().addAll(
            new Label("Map Size:"),
            new Label("Width:"), widthSpinner,
            new Label("Height:"), heightSpinner
        );

        Button createBtn = new Button("Create Scenario");
        Button cancelBtn = new Button("Cancel");

        createBtn.setOnAction(e -> {
            ScenarioBuilderData data = new ScenarioBuilderData(
                nameField.getText().trim(),
                descArea.getText().trim(),
                playerFactionCombo.getValue(),
                enemyFactionCombo.getValue(),
                commandModeCombo.getValue(),
                playerDoctrineCombo.getValue(),
                enemyDoctrineCombo.getValue(),
                locNameField.getText().trim(),
                parseDouble(latField.getText(), 35.0),
                parseDouble(lonField.getText(), -120.0),
                widthSpinner.getValue(),
                heightSpinner.getValue(),
                descArea.getText().trim()
            );
            onScenarioCreated.accept(data);
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        content.getChildren().addAll(
            title,
            new Separator(),
            new Label("Scenario Settings:"),
            nameField,
            descArea,
            factionBox,
            commandModeBox,
            doctrineBox,
            locationBox,
            mapSizeBox,
            new Separator(),
            new HBox(20, createBtn, cancelBtn)
        );

        styleButton(createBtn);
        styleButton(cancelBtn);

        return content;
    }

    private ComboBox<Faction> createFactionSelector(Faction defaultFaction) {
        ComboBox<Faction> combo = new ComboBox<>();
        combo.getItems().addAll(Faction.values());
        combo.setValue(defaultFaction);
        combo.setPrefWidth(120);
        return combo;
    }

    private ComboBox<CommandMode> createCommandModeSelector(CommandMode defaultMode) {
        ComboBox<CommandMode> combo = new ComboBox<>();
        combo.getItems().addAll(CommandMode.values());
        combo.setValue(defaultMode);
        combo.setPrefWidth(120);
        return combo;
    }

    private ComboBox<Doctrine> createDoctrineSelector(Doctrine defaultDoctrine) {
        ComboBox<Doctrine> combo = new ComboBox<>();
        combo.getItems().addAll(Doctrine.values());
        combo.setValue(defaultDoctrine);
        combo.setPrefWidth(120);
        return combo;
    }

    private double parseDouble(String text, double def) {
        try { return Double.parseDouble(text.trim()); }
        catch (NumberFormatException ex) { return def; }
    }

    private void styleButton(Button btn) {
        btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #5a6e82;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #4fc3f7; -fx-text-fill: #000000; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #2c4258;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #5a6e82;"));
    }
}