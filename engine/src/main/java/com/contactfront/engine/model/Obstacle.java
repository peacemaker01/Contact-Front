package com.contactfront.engine.model;

import java.util.ArrayList;
import java.util.List;

public class Obstacle {
    public enum ObstacleType {
        MINEFIELD(3.0, 999.0),
        FENCE(1.5, 2.0),
        BARRIER(2.0, 999.0),
        BOOBYTRAP(5.0, 100.0);

        public final double movementCostMultiplier;
        public final double damageFactor;

        ObstacleType(double movementCostMultiplier, double damageFactor) {
            this.movementCostMultiplier = movementCostMultiplier;
            this.damageFactor = damageFactor;
        }
    }

    private final List<double[]> footprint = new ArrayList<>();
    private final ObstacleType type;
    private final int x, y;

    public Obstacle(int x, int y, ObstacleType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.footprint.add(new double[]{x, y});
    }

    public Obstacle(List<double[]> footprint, ObstacleType type) {
        this.x = (int) footprint.get(0)[0];
        this.y = (int) footprint.get(0)[1];
        this.type = type;
        this.footprint.addAll(footprint);
    }

    public List<double[]> footprint() { return footprint; }
    public ObstacleType type() { return type; }
    public int x() { return x; }
    public int y() { return y; }
}