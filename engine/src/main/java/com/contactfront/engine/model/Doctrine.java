package com.contactfront.engine.model;

public enum Doctrine {
    NATO {
        @Override public void apply(Unit u) {
            u.prefersDispersion = true;
            u.incomingSuppressionMult = 0.8;
            u.defaultStance = Stance.DEFENSIVE;
            u.engageOnlyAtOptimalRange = true;
        }

        @Override public boolean shouldFlank(Unit u) {
            return false;
        }

        @Override public Stance getDefaultStance() {
            return Stance.DEFENSIVE;
        }
    },
    RUSSIAN {
        @Override public void apply(Unit u) {
            u.usesMassedAttacks = true;
            u.moraleThresholdOverride = 10;
            u.incomingSuppressionMult = 1.1;
            u.defaultStance = Stance.AGGRESSIVE;
        }

        @Override public boolean shouldFlank(Unit u) {
            return true;
        }

        @Override public Stance getDefaultStance() {
            return Stance.AGGRESSIVE;
        }
    },
    CHINESE {
        @Override public void apply(Unit u) {
            u.prefersDispersion = false;
            u.incomingSuppressionMult = 1.0;
            u.defaultStance = Stance.AGGRESSIVE;
            u.engageAtAnyRange = true;
        }

        @Override public boolean shouldFlank(Unit u) {
            return false;
        }

        @Override public Stance getDefaultStance() {
            return Stance.AGGRESSIVE;
        }
    },
    IRANIAN {
        @Override public void apply(Unit u) {
            u.prefersDispersion = false;
            u.incomingSuppressionMult = 1.2;
            u.defaultStance = Stance.AGGRESSIVE;
            u.usesMassedAttacks = true;
        }

        @Override public boolean shouldFlank(Unit u) {
            return false;
        }

        @Override public Stance getDefaultStance() {
            return Stance.AGGRESSIVE;
        }
    };

    public abstract void apply(Unit u);
    public abstract boolean shouldFlank(Unit u);
    public abstract Stance getDefaultStance();
}