package com.contactfront.engine.model;

public enum UnitCategory {
    INFANTRY,
    RECON,
    ARMOR,
    ARTILLERY,
    DRONE,
    ENGINEER,
    AIR_DEFENSE,
    LOGISTICS;

    public char glyph() {
        return switch (this) {
            case INFANTRY -> 'i';
            case RECON -> 'r';
            case ARMOR -> 'a';
            case ARTILLERY -> 'm';
            case DRONE -> 'd';
            case ENGINEER -> 'e';
            case AIR_DEFENSE -> 'w';
            case LOGISTICS -> 'l';
        };
    }
}
