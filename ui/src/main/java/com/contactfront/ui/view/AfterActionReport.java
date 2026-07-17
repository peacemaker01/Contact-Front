package com.contactfront.ui.view;

import com.contactfront.engine.model.GameState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class AfterActionReport {
    private AfterActionReport() {}

    public static StackPane create(GameState s, Runnable onRestart) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color:#151a23; -fx-border-color:#3a5067;");
        card.setMaxWidth(420);

        String result = s.victory == null ? "Inconclusive" : (s.victory ? "VICTORY" : "DEFEAT");
        Label head = new Label("After-Action Report — " + result);
        head.setStyle("-fx-text-fill:" + (s.victory == Boolean.TRUE ? "#69f0ae" : "#ff5252")
                + "; -fx-font-size:18px; -fx-font-weight:bold;");

        Label stats = new Label(
                "Time: " + (s.elapsedMs / 1000) + "s\n" +
                "Enemy KIA: " + s.enemyKia + "\n" +
                "Friendly KIA: " + s.friendlyKia + "\n" +
                "Vehicles lost: " + s.vehiclesLost + "\n" +
                "Ammo expended: " + s.ammoExpended + "\n" +
                "Time under suppression: " + (s.turnsUnderSuppression * 500) + "ms");
        stats.setStyle("-fx-text-fill:#e0e6ed;");

        Button again = new Button("New Battle");
        again.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82;");
        again.setOnAction(e -> onRestart.run());

        card.getChildren().addAll(head, stats, again);

        StackPane overlay = new StackPane(card);
        overlay.setStyle("-fx-background-color:rgba(5,7,10,0.82);");
        overlay.setAlignment(Pos.CENTER);
        return overlay;
    }
}
