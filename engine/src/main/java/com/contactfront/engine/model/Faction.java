package com.contactfront.engine.model;

/** A playable faction. Mirrors the doctrine bonuses from the proven Python source. */
public enum Faction {
    USA("United States Armed Forces", "USA", 8, 10),
    RUSSIA("Russian Armed Forces", "RUS", -3, -5),
    CHINA("People's Liberation Army", "PRC", 5, 5),
    IRAN("Islamic Revolutionary Guard Corps", "IRN", -10, 15);

    public final String name;
    public final String abbreviation;
    public final int accuracyBonus;
    public final int moraleBonus;

    Faction(String name, String abbreviation, int accuracyBonus, int moraleBonus) {
        this.name = name;
        this.abbreviation = abbreviation;
        this.accuracyBonus = accuracyBonus;
        this.moraleBonus = moraleBonus;
    }
}
