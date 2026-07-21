package com.contactfront.ui.model;

import com.contactfront.engine.model.Faction;

public record SetupData(Faction playerFaction, Faction enemyFaction, Difficulty difficulty,
                        boolean isNight, boolean isRaining, boolean isWindy, Route route) {

    public enum Difficulty { EASY(1.5), NORMAL(1.0), HARD(0.6);
        public final double aiReactionMultiplier;
        Difficulty(double mult) { this.aiReactionMultiplier = mult; }
    }

    public enum Route { RANDOM_LOCATION, CURATED_LOCATION, PROCEDURAL }
}