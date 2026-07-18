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

public class LocationSelector {
    private final Stage owner;
    private final Stage stage;
    private final Consumer<LocationSelection> onLocationSelected;

    public record LocationSelection(double latitude, double longitude, String locationName) {}

    public LocationSelector(Stage owner, Consumer<LocationSelection> onLocationSelected) {
        this.owner = owner;
        this.stage = new Stage();
        this.stage.initOwner(owner);
        this.stage.initModality(Modality.WINDOW_MODAL);
        this.onLocationSelected = onLocationSelected;
    }

    public void show() {
        VBox root = buildSelector();
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Select Location -- Contact Front");
        stage.centerOnScreen();
        Log.info("Location selector shown");
        stage.show();
    }

    private VBox buildSelector() {
        Label title = new Label("Select Battle Location");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4fc3f7;");

        Label coordsLabel = new Label("Enter Coordinates (WGS84):");
        coordsLabel.setStyle("-fx-text-fill: #e0e6ed;");

        HBox coordInputs = new HBox(10);
        TextField latField = new TextField("35.0");
        latField.setPrefWidth(100);
        TextField lonField = new TextField("-120.0");
        lonField.setPrefWidth(100);

        coordInputs.getChildren().addAll(latField, lonField);

        TextField nameField = new TextField("Selected Location");
        nameField.setPrefWidth(300);

        Button searchBtn = new Button("Search Location");
        searchBtn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed;");

        Label presetsLabel = new Label("Preset Locations:");
        presetsLabel.setStyle("-fx-text-fill: #e0e6ed;");

        HBox presetButtons = new HBox(10);
        presetButtons.getChildren().addAll(
            createPresetButton("Afghan Valley", 33.0, 65.0),
            createPresetButton("Korean DMZ", 38.0, 127.0),
            createPresetButton("Syria Border", 35.0, 40.0),
            createPresetButton("Eastern Europe", 50.0, 30.0)
        );

        Button okBtn = new Button("OK");
        Button cancelBtn = new Button("Cancel");

        okBtn.setOnAction(e -> {
            double lat = parseCoordinate(latField.getText(), true);
            double lon = parseCoordinate(lonField.getText(), false);
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Selected Location";

            onLocationSelected.accept(new LocationSelection(lat, lon, name));
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(20, okBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(15);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.TOP_CENTER);
        content.getChildren().addAll(
            title,
            new Separator(),
            coordsLabel,
            coordInputs,
            new Label("Location Name:"),
            nameField,
            searchBtn,
            new Separator(),
            presetsLabel,
            presetButtons,
            new Separator(),
            buttonBox
        );

        styleButton(okBtn);
        styleButton(cancelBtn);
        styleButton(searchBtn);

        return content;
    }

    private Button createPresetButton(String name, double lat, double lon) {
        Button btn = new Button(name);
        btn.setPrefWidth(120);
        return btn;
    }

    private double parseCoordinate(String text, boolean isLatitude) {
        try {
            double val = Double.parseDouble(text.trim());
            if (isLatitude && (val < -90 || val > 90)) {
                throw new NumberFormatException("Latitude must be between -90 and 90");
            }
            if (!isLatitude && (val < -180 || val > 180)) {
                throw new NumberFormatException("Longitude must be between -180 and 180");
            }
            return val;
        } catch (NumberFormatException ex) {
            return isLatitude ? 35.0 : -120.0;
        }
    }

    private void styleButton(Button btn) {
        btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #5a6e82;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #4fc3f7; -fx-text-fill: #000000; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #2c4258;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #5a6e82;"));
    }
}