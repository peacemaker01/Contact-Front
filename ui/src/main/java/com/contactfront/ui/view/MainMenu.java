package com.contactfront.ui.view;

import com.contactfront.engine.model.Faction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Screen;

import java.util.function.Consumer;

public class MainMenu {
    private final Stage stage;
    private final Runnable onNewGame;
    private final Consumer<Faction> onFactionSelect;
    private final Runnable onLoadGame;
    private final Runnable onOptions;
    private final Runnable onScenarioBuilder;
    private final Consumer<LocationSelector.LocationSelection> onLocationSelect;

    public MainMenu(Stage stage, Runnable onNewGame, Consumer<Faction> onFactionSelect, 
                    Runnable onLoadGame, Runnable onOptions, Runnable onScenarioBuilder,
                    Consumer<LocationSelector.LocationSelection> onLocationSelect) {
        this.stage = stage;
        this.onNewGame = onNewGame;
        this.onFactionSelect = onFactionSelect;
        this.onLoadGame = onLoadGame;
        this.onOptions = onOptions;
        this.onScenarioBuilder = onScenarioBuilder;
        this.onLocationSelect = onLocationSelect;
    }

    public void show() {
        VBox menu = buildMenu();
        Scene scene = new Scene(menu, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Contact Front — Main Menu");
        stage.centerOnScreen();
        stage.show();
    }

    private VBox buildMenu() {
        Label title = new Label("CONTACT FRONT");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #4fc3f7;");

        Button newGameBtn = new Button("New Game");
        newGameBtn.setOnAction(e -> showFactionSelect());

        Button loadGameBtn = new Button("Load Game");
        loadGameBtn.setOnAction(e -> onLoadGame.run());

        Button scenarioBtn = new Button("Scenario Builder");
        scenarioBtn.setOnAction(e -> onScenarioBuilder.run());

        Button locationBtn = new Button("Load Real Location");
        locationBtn.setOnAction(e -> showLocationSelect());

        Button optionsBtn = new Button("Options");
        optionsBtn.setOnAction(e -> onOptions.run());

        Button exitBtn = new Button("Exit");
        exitBtn.setOnAction(e -> stage.close());

        VBox buttons = new VBox(10, newGameBtn, loadGameBtn, scenarioBtn, locationBtn, optionsBtn, exitBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox menuBox = new VBox(30, title, buttons);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setPadding(new Insets(40));
        menuBox.setStyle("-fx-background-color: #0e1117;");

        styleButton(newGameBtn);
        styleButton(loadGameBtn);
        styleButton(scenarioBtn);
        styleButton(locationBtn);
        styleButton(optionsBtn);
        styleButton(exitBtn);

        return menuBox;
    }

    private void styleButton(Button btn) {
        btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 16px; -fx-pref-width: 180px; -fx-pref-height: 40px; -fx-border-color: #5a6e82;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #4fc3f7; -fx-text-fill: #000000; -fx-font-size: 16px; -fx-pref-width: 180px; -fx-pref-height: 40px; -fx-border-color: #2c4258;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 16px; -fx-pref-width: 180px; -fx-pref-height: 40px; -fx-border-color: #5a6e82;"));
    }

    private void showFactionSelect() {
        VBox factionBox = new VBox(20);
        factionBox.setAlignment(Pos.CENTER);
        factionBox.setPadding(new Insets(40));
        factionBox.setStyle("-fx-background-color: #0e1117;");

        Label title = new Label("Select Faction");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #4fc3f7;");

        Button usBtn = createFactionButton("US Army", Faction.USA);
        Button plaBtn = createFactionButton("PLA", Faction.CHINA);
        Button rusBtn = createFactionButton("Russian Armed Forces", Faction.RUSSIA);
        Button irgBtn = createFactionButton("IRGC", Faction.IRAN);

        factionBox.getChildren().addAll(title, usBtn, plaBtn, rusBtn, irgBtn);

        Scene scene = new Scene(factionBox, 800, 600);
        stage.setScene(scene);
    }

    private void showLocationSelect() {
        LocationSelector selector = new LocationSelector(stage, loc -> {
            stage.setScene(new Scene(new VBox(10), 800, 600));
            onLocationSelect.accept(loc);
        });
        selector.show();
    }

    private Button createFactionButton(String name, Faction faction) {
        Button btn = new Button(name);
        btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 16px; -fx-pref-width: 200px; -fx-pref-height: 40px; -fx-border-color: #5a6e82;");
        btn.setOnAction(e -> {
            onFactionSelect.accept(faction);
        });
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #4fc3f7; -fx-text-fill: #000000; -fx-font-size: 16px; -fx-pref-width: 200px; -fx-pref-height: 40px; -fx-border-color: #2c4258;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 16px; -fx-pref-width: 200px; -fx-pref-height: 40px; -fx-border-color: #5a6e82;"));
        return btn;
    }
}