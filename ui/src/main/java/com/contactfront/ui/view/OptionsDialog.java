package com.contactfront.ui.view;

import com.contactfront.ui.Log;
import com.contactfront.ui.assets.MapTilerClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class OptionsDialog {
    private final Stage stage;
    private final Consumer<OptionsData> onSave;
    private Label statusLabel = new Label();

    public record OptionsData(String mapTilerApiKey, String openStreetMapUrl, int offlineCacheSizeMb, 
                               boolean enableRealtimeMovement, double gameSpeed) {}

    public OptionsDialog(Stage owner, Consumer<OptionsData> onSave) {
        this.stage = new Stage();
        this.stage.initOwner(owner);
        this.stage.initModality(Modality.WINDOW_MODAL);
        this.onSave = onSave;
    }

    public void show() {
        VBox root = buildOptions();
        Scene scene = new Scene(root, 620, 540);
        stage.setScene(scene);
        stage.setTitle("Game Options");
        stage.centerOnScreen();
        loadSavedApiKey();
        stage.show();
    }

    private void loadSavedApiKey() {
        try {
            Path settingsFile = Path.of("config", "settings.dat");
            if (Files.exists(settingsFile)) {
                String[] lines = Files.readString(settingsFile).split("\n");
                for (String line : lines) {
                    if (line.startsWith("mapTilerApiKey=")) {
                        String savedKey = line.substring("mapTilerApiKey=".length());
                        stage.getScene().lookup(".text-field").focusedProperty().addListener((obs, old, focused) -> {
                            if (!focused) return;
                            TextField tf = (TextField) stage.getScene().lookup("TextField");
                        });
                    }
                }
            }
        } catch (Exception e) {
            Log.error("Failed to load saved API key: " + e.getMessage());
        }
    }

    private VBox buildOptions() {
        Label title = new Label("Game Options");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4fc3f7;");

        TextField mapTilerKeyField = new TextField(MapTilerClient.getApiKey());
        mapTilerKeyField.setPromptText("MapTiler API Key");

        TextField osmUrlField = new TextField("https://tile.openstreetmap.org/{z}/{x}/{y}.png");
        osmUrlField.setPromptText("OpenStreetMap Tile URL");

        Spinner<Integer> cacheSizeSpinner = new Spinner<>(1, 1000, 500);
        cacheSizeSpinner.setPrefWidth(100);

        CheckBox realtimeCb = new CheckBox("Enable Real-Time Movement");
        realtimeCb.setSelected(true);

        Label speedLabel = new Label("Game Speed (1x = normal):");
        Spinner<Double> speedSpinner = new Spinner<>(0.1, 5.0, 1.0, 0.1);
        speedSpinner.setPrefWidth(100);

        Button validateBtn = new Button("Validate");
        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");

        validateBtn.setOnAction(e -> validateApiKey(mapTilerKeyField.getText().trim()));

        saveBtn.setOnAction(e -> {
            Log.info("Options save clicked");
            OptionsData data = new OptionsData(
                mapTilerKeyField.getText().trim(),
                osmUrlField.getText().trim(),
                cacheSizeSpinner.getValue(),
                realtimeCb.isSelected(),
                speedSpinner.getValue()
            );
            saveApiKeyToFile(data.mapTilerApiKey);
            onSave.accept(data);
            Log.info("Options save callback completed, closing dialog");
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(20, validateBtn, saveBtn, cancelBtn);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        content.getChildren().addAll(
            title,
            new Separator(),
            new Label("Satellite Imagery Sources:"),
            new HBox(10, new Label("MapTiler API Key:"), mapTilerKeyField),
            new HBox(10, new Label("OSM Tile URL:"), osmUrlField),
            new HBox(10, new Label("Offline Cache (MB):"), cacheSizeSpinner),
            statusLabel,
            new Separator(),
            new Label("Gameplay Settings:"),
            realtimeCb,
            new HBox(10, speedLabel, speedSpinner),
            new Separator(),
            buttonBox
        );

        styleButton(validateBtn);
        styleButton(saveBtn);
        styleButton(cancelBtn);
        statusLabel.setStyle("-fx-text-fill: #e04545; -fx-font-size: 12px;");

        return content;
    }

    private void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            statusLabel.setText("Please enter an API key");
            return;
        }
        statusLabel.setText("Validating...");
        Thread t = new Thread(() -> {
            try {
                boolean valid = MapTilerClient.validateApiKey(apiKey);
                javafx.application.Platform.runLater(() -> {
                    if (valid) {
                        statusLabel.setText("API key configured ✓");
                    } else {
                        statusLabel.setText("Invalid API key or quota exceeded");
                    }
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> statusLabel.setText("Validation failed: " + ex.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void saveApiKeyToFile(String apiKey) {
        try {
            Path configDir = Path.of("config");
            Files.createDirectories(configDir);
            Path settingsFile = configDir.resolve("settings.dat");
            Files.writeString(settingsFile, "mapTilerApiKey=" + apiKey + "\n");
        } catch (Exception e) {
            Log.error("Failed to save API key: " + e.getMessage());
        }
    }

    private void styleButton(Button btn) {
        btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #5a6e82;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #4fc3f7; -fx-text-fill: #000000; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #2c4258;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #5a6e82;"));
    }
}