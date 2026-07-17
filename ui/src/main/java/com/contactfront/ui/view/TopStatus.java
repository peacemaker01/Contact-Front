package com.contactfront.ui.view;

import com.contactfront.engine.model.GameState;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class TopStatus {
    private final HBox root = new HBox(24);
    private final Label mission = new Label();
    private final Label turn = new Label();
    private final Label faction = new Label();
    private final Label seed = new Label();

    public TopStatus() {
        root.setPadding(new javafx.geometry.Insets(8));
        root.setStyle("-fx-background-color:#151a23; -fx-border-color:#3a5067;");
        for (Label l : new Label[]{mission, turn, faction, seed}) {
            l.setStyle("-fx-text-fill:#e0e6ed; -fx-font-weight:bold;");
            root.getChildren().add(l);
        }
    }

    public HBox node() { return root; }

    public void update(GameState s) {
        if (s == null) return;
        String m = s.objectives.isEmpty() ? "—" : s.objectives.get(0).name;
        mission.setText("Mission: " + m);
        turn.setText("Turn: " + s.turn + " / " + s.maxTurns);
        faction.setText("You: " + (s.playerFaction != null ? s.playerFaction.name : "—"));
        seed.setText("Seed: " + s.seed);
    }
}
