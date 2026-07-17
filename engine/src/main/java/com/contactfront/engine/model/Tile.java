package com.contactfront.engine.model;

import java.util.ArrayList;
import java.util.List;

/** A single map cell. Mutable; the grid is the engine's terrain state. */
public class Tile {
    public final int x;
    public final int y;
    public Terrain type;
    public int coverBonus;
    public double movementCost;
    public boolean blocksLos;
    public final List<Unit> units = new ArrayList<>();

    public Tile(Terrain type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.coverBonus = type.coverBonus;
        this.movementCost = type.movementCost;
        this.blocksLos = type.blocksLos;
    }

    public boolean impassable() {
        return movementCost >= 999.0;
    }

    public boolean impassableForGround() {
        return movementCost >= 999.0;
    }
}
