package com.contactfront.ui.view;

import com.contactfront.engine.model.Stance;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.Visibility;
import com.contactfront.ui.Palette;
import com.contactfront.ui.controller.GameController;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Node;

public class SidePanel {
    private final GameController ctrl;
    private final Label title = new Label("No unit selected");
    private final Label sub = new Label("");
    private final ProgressBar health = new ProgressBar();
    private final ProgressBar ammo = new ProgressBar();
    private final ProgressBar morale = new ProgressBar();
    private final ProgressBar suppress = new ProgressBar();
    private final ToggleGroup stanceGroup = new ToggleGroup();
    private final Button bMove = new Button("Move");
    private final Button bAttack = new Button("Attack");
    private final Button bRecon = new Button("Recon");
    private final Button bResupply = new Button("Resupply");
    private final Button bArty = new Button("Call Arty");
    private final Button bCas = new Button("Call CAS");
    private final Button bEnd = new Button("End Turn");
    private final HBox groupRow = new HBox(2);
    private final Label contactLine = new Label("");

    public SidePanel(GameController ctrl) {
        this.ctrl = ctrl;
        title.setStyle("-fx-text-fill:#e0e6ed; -fx-font-weight:bold;");
        sub.setStyle("-fx-text-fill:#8695aa;");

        VBox bars = new VBox(4,
                labeled("Health", health), labeled("Ammo", ammo),
                labeled("Morale", morale), labeled("Suppression", suppress));
        bars.setPadding(new Insets(6));

        HBox stances = new HBox(4,
                stanceBtn("Aggressive", Stance.AGGRESSIVE),
                stanceBtn("Defensive", Stance.DEFENSIVE),
                stanceBtn("Overwatch", Stance.OVERWATCH));

        VBox orders = new VBox(4, bMove, bAttack, bRecon, bResupply, bArty, bCas);
        orders.setPadding(new Insets(6));

        bMove.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82;");
        bAttack.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82;");
        bRecon.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82;");
        bResupply.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82;");
        bArty.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82;");
        bCas.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82;");
        bEnd.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82;");

        bMove.setOnAction(e -> ctrl.beginMove());
        bAttack.setOnAction(e -> ctrl.beginAttack());
        bRecon.setOnAction(e -> ctrl.recon());
        bResupply.setOnAction(e -> ctrl.resupply());
        bArty.setOnAction(e -> ctrl.beginArty());
        bCas.setOnAction(e -> ctrl.beginCas());
        bEnd.setOnAction(e -> ctrl.endTurn());

        for (int n = 1; n <= 9; n++) {
            final int nn = n;
            Button gb = new Button(String.valueOf(n));
            gb.setStyle("-fx-text-fill:#e0e6ed; -fx-font-size:10px; -fx-padding:2 4; -fx-background-color:#2c4258;");
            gb.setOnAction(e -> ctrl.recallGroup(nn));
            groupRow.getChildren().add(gb);
        }

        contactLine.setStyle("-fx-text-fill:#8695aa; -fx-font-size:11px;");
        VBox root = new VBox(8, title, sub, bars,
                new Label("Stance"), stances,
                new Label("Groups  (Ctrl+# = assign, # = recall)"), groupRow,
                new Label("Orders"), orders, contactLine, bEnd);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color:#151a23; -fx-border-color:#3a5067;");
        root.setPrefWidth(240);
        node = root;
    }

    public Node node() { return node; }

    private Node node;

    private HBox labeled(String name, ProgressBar bar) {
        bar.setPrefWidth(150);
        return new HBox(8, new Label(name) {{ setStyle("-fx-text-fill:#8695aa;"); setPrefWidth(80); }}, bar);
    }

    private ToggleButton stanceBtn(String name, Stance s) {
        ToggleButton tb = new ToggleButton(name);
        tb.setToggleGroup(stanceGroup);
        tb.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82; -fx-font-weight:normal;");
        tb.setOnAction(e -> ctrl.setStance(s));
        tb.selectedProperty().addListener((obs, old, newV) -> {
            if (newV) {
                tb.setStyle("-fx-background-color:#4fc3f7; -fx-text-fill:#000000; -fx-border-color:#2c4258; -fx-font-weight:bold;");
            } else {
                tb.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82; -fx-font-weight:normal;");
            }
        });
        return tb;
    }

    public void update() {
        Unit u = ctrl.selected;
        boolean act = ctrl.canAct();
        int selCount = ctrl.selection.size();
        if (u == null) {
            title.setText("No unit selected");
            sub.setText(selCount == 0 ? "Click a friendly unit." : selCount + " units selected.");
            health.setProgress(0); ammo.setProgress(0); morale.setProgress(0); suppress.setProgress(0);
            setBarsVisible(false);
            contactLine.setText("");
            contactLine.setVisible(false);
        } else {
            updateContactLine(u);
            setBarsVisible(true);
            title.setText(u.profile.name() + " #" + u.id + (selCount > 1 ? "  (+" + (selCount - 1) + ")" : ""));
            sub.setText(u.profile.category() + " @ " + u.x + "," + u.y + "  (" + u.stance + ")");
            health.setProgress(clamp(u.strength / 100.0));
            int max = u.weapons.stream().mapToInt(w -> w.maxAmmo).sum();
            ammo.setProgress(max == 0 ? 0 : clamp((double) u.totalAmmo() / max));
            morale.setProgress(clamp(u.morale / 100.0));
            suppress.setProgress(clamp(u.suppression / 100.0));
            health.setStyle(barColor(u.strength / 100.0));
            suppress.setStyle(barColor(1 - u.suppression / 100.0));
            for (javafx.scene.control.Toggle t : stanceGroup.getToggles())
                ((ToggleButton) t).setSelected(false);
            stanceGroup.getToggles().stream()
                    .filter(t -> ((ToggleButton) t).getText().equalsIgnoreCase(u.stance.name()))
                    .forEach(t -> ((ToggleButton) t).setSelected(true));
        }
        bMove.setDisable(!act);
        bAttack.setDisable(!act);
        bRecon.setDisable(!act);
        bResupply.setDisable(!act);
        bCas.setDisable(!act);
    }

    private void setBarsVisible(boolean v) {
        health.setVisible(v); ammo.setVisible(v); morale.setVisible(v); suppress.setVisible(v);
    }

    private double clamp(double d) { return Math.max(0, Math.min(1, d)); }

    private String barColor(double good) {
        if (good > 0.6) return "-fx-accent:#69f0ae;";
        if (good > 0.3) return "-fx-accent:#ffb74d;";
        return "-fx-accent:#ff5252;";
    }

    private void updateContactLine(Unit u) {
        contactLine.setVisible(true);
        if (u.lastContactTurn < 0) {
            contactLine.setText("Last contact: none");
            contactLine.setStyle("-fx-text-fill:#8695aa; -fx-font-size:11px;");
            return;
        }
        boolean live = u.lastContactTurn == ctrl.state.turn
                && u.lastContactX >= 0 && u.lastContactY >= 0
                && u.lastContactY < ctrl.state.visibility.length
                && u.lastContactX < ctrl.state.visibility[0].length
                && ctrl.state.visibility[u.lastContactY][u.lastContactX] == Visibility.VISIBLE;
        String text = "Last contact: " + u.lastContactName + " @ (" + u.lastContactX + "," + u.lastContactY + ")  T" + u.lastContactTurn
                + (live ? "" : "  (stale)");
        contactLine.setText(text);
        contactLine.setStyle("-fx-font-size:11px; -fx-text-fill:"
                + (live ? "#ff5252" : "#8695aa") + ";");
    }
}
