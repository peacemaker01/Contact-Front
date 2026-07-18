package com.contactfront.ui.view;

import com.contactfront.ui.Log;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class OptionsDialog {
    private final Stage stage;
    private final Consumer<OptionsData> onSave;

    public record OptionsData(String googleMapsApiKey, String openStreetMapUrl, int offlineCacheSizeMb, 
                               boolean enableRealtimeMovement, double gameSpeed) {}

    public OptionsDialog(Stage owner, Consumer<OptionsData> onSave) {
        this.stage = new Stage();
        this.stage.initOwner(owner);
        this.stage.initModality(Modality.WINDOW_MODAL);
        this.onSave = onSave;
    }

    public void show() {
        VBox root = buildOptions();
        Scene scene = new Scene(root, 600, 500);
        stage.setScene(scene);
        stage.setTitle("Game Options");
        stage.centerOnScreen();
        stage.show();
    }

    private VBox buildOptions() {
        Label title = new Label("Game Options");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4fc3f7;");

        TextField googleMapsKeyField = new TextField();
        googleMapsKeyField.setPromptText("Google Maps Static API Key");

        TextField osmUrlField = new TextField("https://tile.openstreetmap.org/{z}/{x}/{y}.png");
        osmUrlField.setPromptText("OpenStreetMap Tile URL");

        Spinner<Integer> cacheSizeSpinner = new Spinner<>(1, 1000, 500);
        cacheSizeSpinner.setPrefWidth(100);

        CheckBox realtimeCb = new CheckBox("Enable Real-Time Movement");
        realtimeCb.setSelected(true);

        Label speedLabel = new Label("Game Speed (1x = normal):");
        Spinner<Double> speedSpinner = new Spinner<>(0.1, 5.0, 1.0, 0.1);
        speedSpinner.setPrefWidth(100);

        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");

        saveBtn.setOnAction(e -> {
            Log.info("Options save clicked");
            OptionsData data = new OptionsData(
                googleMapsKeyField.getText().trim(),
                osmUrlField.getText().trim(),
                cacheSizeSpinner.getValue(),
                realtimeCb.isSelected(),
                speedSpinner.getValue()
            );
            onSave.accept(data);
            Log.info("Options save callback completed, closing dialog");
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        content.getChildren().addAll(
            title,
            new Separator(),
            new Label("Satellite Imagery Sources:"),
            new HBox(10, new Label("Google Maps API Key:"), googleMapsKeyField),
            new HBox(10, new Label("OSM Tile URL:"), osmUrlField),
            new HBox(10, new Label("Offline Cache (MB):"), cacheSizeSpinner),
            new Separator(),
            new Label("Gameplay Settings:"),
            realtimeCb,
            new HBox(10, speedLabel, speedSpinner),
            new Separator(),
            new HBox(20, saveBtn, cancelBtn)
        );

        styleButton(saveBtn);
        styleButton(cancelBtn);

        return content;
    }

    private void styleButton(Button btn) {
        btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #5a6e82;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #4fc3f7; -fx-text-fill: #000000; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #2c4258;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #5a6e82;"));
    }
}