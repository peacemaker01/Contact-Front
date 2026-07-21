package com.contactfront.engine.model;

public enum Terrain {
    OPEN('.', 0, 1.0, false),
    SCRUB('s', 10, 1.2, false),
    BUSH('b', 25, 1.0, false),
    FOREST('T', 35, 2.0, true),
    WETLAND('w', 0, 999.0, false),
    HILL('^', 20, 2.0, true),
    RUIN('R', 50, 1.5, true),
    BUILDING('B', 60, 999.0, true),
    ROAD('-', 0, 0.5, false),
    ROAD_VERT('|', 0, 0.5, false),
    ROAD_CROSS('+', 0, 0.5, false),
    WATER('~', 0, 999.0, false),
    FORD('f', 0, 3.0, false),
    WATERWAY('W', 0, 999.0, false),
    CHECKPOINT('C', 40, 3.0, false),
    CRATER('X', 25, 1.5, false),
    OBJECTIVE('*', 10, 1.0, false),
    FIRE('F', -10, 2.0, false),
    MINEFIELD('M', 0, 1.0, false),
    DIRT('d', 0, 1.1, false),
    SNOW('S', 0, 1.5, false);

    public final char glyph;
    public final int coverBonus;
    public final double movementCost;
    public final boolean blocksLos;

    Terrain(char glyph, int coverBonus, double movementCost, boolean blocksLos) {
        this.glyph = glyph;
        this.coverBonus = coverBonus;
        this.movementCost = movementCost;
        this.blocksLos = blocksLos;
    }

    public static double roadSpeedMultiplier(RoadSegment.RoadType type, UnitCategory category) {
        if (type == null) return 1.0;
        return switch (type) {
            case MOTORWAY -> (category == UnitCategory.ARMOR || category == UnitCategory.LOGISTICS) ? 0.90 : 1.00;
            case TRUNK, PRIMARY, SECONDARY -> (category == UnitCategory.ARMOR || category == UnitCategory.LOGISTICS) ? 0.85 : 0.90;
            case TERTIARY, UNCLASSIFIED -> (category == UnitCategory.ARMOR || category == UnitCategory.LOGISTICS) ? 0.75 : 0.80;
            case RESIDENTIAL, SERVICE -> (category == UnitCategory.ARMOR || category == UnitCategory.LOGISTICS) ? 0.60 : 0.70;
        };
    }
}
