package com.contactfront.ui.view;

import com.contactfront.engine.model.Faction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.function.BiConsumer;

public class EnemySelectDialog {
    private final Stage stage;
    private final Faction playerFaction;
    private final BiConsumer<Faction, Faction> onConfirm;

    public EnemySelectDialog(Stage stage, Faction playerFaction, BiConsumer<Faction, Faction> onConfirm) {
        this.stage = stage;
        this.playerFaction = playerFaction;
        this.onConfirm = onConfirm;
    }

    public void show() {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        box.setStyle("-fx-background-color: #0e1117;");

        Label title = new Label("Select Enemy Faction");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4fc3f7;");

        Button rusBtn = createButton("Russian Armed Forces", Faction.RUSSIA);
        Button chiBtn = createButton("People's Liberation Army", Faction.CHINA);
        Button iraBtn = createButton("Islamic Revolutionary Guard Corps", Faction.IRAN);

        box.getChildren().addAll(title, rusBtn, chiBtn, iraBtn);
        Scene scene = new Scene(box, 500, 400);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    private Button createButton(String name, Faction faction) {
        Button btn = new Button(name);
        btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 14px; -fx-pref-width: 220px; -fx-pref-height: 40px; -fx-border-color: #5a6e82;");
        btn.setOnAction(e -> {
            if (faction != playerFaction) {
                onConfirm.accept(playerFaction, faction);
            }
        });
        btn.setOnMouseEntered(ev -> btn.setStyle("-fx-background-color: #4fc3f7; -fx-text-fill: #000000; -fx-font-size: 14px; -fx-pref-width: 220px; -fx-pref-height: 40px; -fx-border-color: #2c4258;"));
        btn.setOnMouseExited(ev -> btn.setStyle("-fx-background-color: #3a5067; -fx-text-fill: #e0e6ed; -fx-font-size: 14px; -fx-pref-width: 220px; -fx-pref-height: 40px; -fx-border-color: #5a6e82;"));
        return btn;
    }
}