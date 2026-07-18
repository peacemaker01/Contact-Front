package com.contactfront.ui.view;

import com.contactfront.engine.data.LocationProfile;
import com.contactfront.engine.data.LocationRegistry;
import com.contactfront.ui.Log;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

public class LocationSelector {
    private final Stage owner;
    private final Stage stage;
    private final Consumer<LocationSelection> onLocationSelected;

    public record LocationSelection(double latitude, double longitude, String locationName, LocationProfile profile) {}

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

        Label presetsLabel = new Label("Preset Locations (Curated Pool):");
        presetsLabel.setStyle("-fx-text-fill: #e0e6ed;");

        LocationRegistry registry = new LocationRegistry();
        List<LocationProfile> locations = registry.allLocations();

        FlowPane presetButtons = new FlowPane(10, 10);
        presetButtons.setAlignment(Pos.TOP_LEFT);
        for (LocationProfile loc : locations) {
            Button btn = new Button(loc.name());
            btn.setPrefWidth(200);
            btn.setOnAction(e -> {
                LocationSelection sel = new LocationSelection(
                    loc.boundingBox().centerLat(),
                    loc.boundingBox().centerLon(),
                    loc.name(),
                    loc
                );
                onLocationSelected.accept(sel);
                stage.close();
            });
            stylePresetButton(btn);
            presetButtons.getChildren().add(btn);
        }

        VBox content = new VBox(15);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.TOP_CENTER);
        content.getChildren().addAll(
            title,
            new Separator(),
            presetsLabel,
            presetButtons
        );

        return content;
    }

    private void stylePresetButton(Button btn) {
        btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #5a6e82;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #4fc3f7; -fx-text-fill: #000000; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #2c4258;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 14px; -fx-pref-height: 35px; -fx-border-color: #5a6e82;"));
    }
}