package com.contactfront.ui;

import javafx.scene.paint.Color;

public final class Palette {
    private Palette() {}

    public static final Color FRIENDLY = Color.web("#4fc3f7");
    public static final Color HOSTILE = Color.web("#ff5252");
    public static final Color CAUTION = Color.web("#ffb74d");
    public static final Color SUCCESS = Color.web("#69f0ae");
    public static final Color BACKGROUND = Color.web("#0e1117");
    public static final Color PANEL = Color.web("#151a23");
    public static final Color BORDER = Color.web("#3a5067");
    public static final Color TEXT = Color.web("#e0e6ed");
    public static final Color TEXT_DIM = Color.web("#8695aa");

    public static Color terrain(String t) {
        return switch (t) {
            case "OPEN" -> Color.web("#1c241c");
            case "FOREST" -> Color.web("#1f3320");
            case "HILL" -> Color.web("#33301f");
            case "RUIN" -> Color.web("#2b2b2b");
            case "BUILDING" -> Color.web("#3a3a3a");
            case "ROAD", "ROAD_VERT", "ROAD_CROSS" -> Color.web("#3d3a2c");
            case "WATER" -> Color.web("#16314a");
            case "FORD" -> Color.web("#1f5a5a");
            case "CHECKPOINT" -> Color.web("#2c2c1c");
            case "CRATER" -> Color.web("#262626");
            case "OBJECTIVE" -> Color.web("#2c2418");
            case "FIRE" -> Color.web("#5a2a10");
            case "MINEFIELD" -> Color.web("#3a1f1f");
            default -> Color.web("#1c241c");
        };
    }
}
