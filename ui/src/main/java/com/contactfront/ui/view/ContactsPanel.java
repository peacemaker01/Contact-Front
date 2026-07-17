package com.contactfront.ui.view;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.Visibility;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class ContactsPanel {
    private final VBox box = new VBox(3);
    private final ScrollPane root = new ScrollPane(box);

    public ContactsPanel() {
        root.setStyle("-fx-background-color:#0e1117; -fx-border-color:#3a5067;");
        root.setPrefHeight(180);
        root.setFitToWidth(true);
        box.setPadding(new Insets(6));
        Label head = new Label("CONTACTS — FORCE PICTURE");
        head.setStyle("-fx-text-fill:#e0e6ed; -fx-font-weight:bold; -fx-font-size:11px;");
        VBox wrap = new VBox(2, head, root);
        wrap.setStyle("-fx-background-color:#151a23; -fx-border-color:#3a5067;");
        wrap.setPrefWidth(240);
        node = wrap;
    }

    private Node node;
    public Node node() { return node; }

    public void update(GameState s) {
        if (s == null) return;
        box.getChildren().clear();
        int live = 0, stale = 0;
        for (Unit e : s.enemyUnits) {
            if (e.destroyed || !e.knownToPlayer) continue;
            boolean isLive = s.visibility[e.y][e.x] == Visibility.VISIBLE;
            if (isLive) live++; else stale++;
            int px = isLive ? e.x : e.lastKnownX;
            int py = isLive ? e.y : e.lastKnownY;
            Text t = new Text(
                (isLive ? "◉ " : "○ ") + e.profile.name() +
                "  (" + px + "," + py + ")" +
                (isLive ? "" : "  last T" + e.lastSeenTurn));
            t.setFont(Font.font("Consolas", 11));
            t.setFill(isLive ? Color.web("#ff5252") : Color.web("#8695aa"));
            box.getChildren().add(t);
        }
        if (live == 0 && stale == 0) {
            Text t = new Text("— no known enemy contacts —");
            t.setFont(Font.font("Consolas", 11));
            t.setFill(Color.web("#8695aa"));
            box.getChildren().add(t);
        }
        Label foot = new Label("live: " + live + "   stale: " + stale);
        foot.setStyle("-fx-text-fill:#8695aa; -fx-font-size:10px;");
        box.getChildren().add(foot);
    }
}
