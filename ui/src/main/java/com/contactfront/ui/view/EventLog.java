package com.contactfront.ui.view;

import com.contactfront.engine.model.GameState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class EventLog {
    private final VBox box = new VBox(2);
    private final ScrollPane root = new ScrollPane(box);
    private final ToggleGroup channels = new ToggleGroup();
    private String filter = "all";

    public EventLog() {
        root.setStyle("-fx-background-color:#0e1117; -fx-border-color:#3a5067;");
        root.setPrefHeight(150);
        root.setFitToWidth(true);
        box.setPadding(new Insets(6));

        HBox bar = new HBox(4);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(2, 4, 2, 4));
        bar.setStyle("-fx-background-color:#151a23;");
        for (String[] c : new String[][]{ {"all","All"}, {"combat","Combat"}, {"intel","Intel"}, {"orders","Orders"} }) {
            ToggleButton tb = new ToggleButton(c[1]);
            tb.setToggleGroup(channels);
            tb.setUserData(c[0]);
            tb.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82; -fx-font-size:11px;");
            tb.setSelected(c[0].equals("all"));
            tb.setOnAction(e -> { filter = (String) tb.getUserData(); update(lastState); });
            tb.selectedProperty().addListener((obs, old, newV) -> {
                if (newV) {
                    tb.setStyle("-fx-background-color:#4fc3f7; -fx-text-fill:#000000; -fx-border-color:#2c4258; -fx-font-size:11px;");
                } else {
                    tb.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82; -fx-font-size:11px;");
                }
            });
            bar.getChildren().add(tb);
        }
        VBox wrap = new VBox(0, bar, root);
        wrap.setStyle("-fx-background-color:#0e1117;");
        node = wrap;
    }

    private Node node;
    public Node node() { return node; }

    private GameState lastState;
    public void update(GameState s) {
        lastState = s;
        if (s == null) return;
        box.getChildren().clear();
        if (!s.eventLog.isEmpty()) {
            int start = Math.max(0, s.eventLog.size() - 80);
            for (int i = start; i < s.eventLog.size(); i++) {
                GameState.LogEntry e = s.eventLog.get(i);
                if (filter.equals("all") || filter.equals(e.channel)) {
                    Text t = new Text("• [" + e.channel.substring(0,3).toUpperCase() + " T" + e.turn + "] " + e.text);
                    t.setFont(Font.font("Consolas", 12));
                    t.setFill(channelColor(e.channel));
                    box.getChildren().add(t);
                }
            }
        } else {
            int start = Math.max(0, s.narrativeLog.size() - 60);
            for (String line : s.narrativeLog.subList(start, s.narrativeLog.size())) {
                Text t = new Text("• " + line);
                t.setFont(Font.font("Consolas", 12));
                t.setFill(colorFor(line));
                box.getChildren().add(t);
            }
        }
        root.setVvalue(1.0);
    }

    private Color channelColor(String ch) {
        return switch (ch) {
            case "combat" -> Color.web("#ff5252");
            case "intel" -> Color.web("#69f0ae");
            case "orders" -> Color.web("#ffb74d");
            default -> Color.web("#8695aa");
        };
    }

    private Color colorFor(String line) {
        if (line.contains("destroyed")) return Color.web("#ff5252");
        if (line.contains("reveal") || line.contains("secured") || line.contains("Victory")) return Color.web("#69f0ae");
        if (line.contains("FRIENDLY FIRE")) return Color.web("#ffb74d");
        return Color.web("#8695aa");
    }
}
