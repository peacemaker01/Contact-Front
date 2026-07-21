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

        @Override public boolean useEcheonDefense(Unit u, Unit target) {
            return u.strength < 60 && target != null && u.category() == UnitCategory.INFANTRY;
        }

        @Override public boolean shouldMassArmor(Unit u) {
            return true;
        }

        @Override public Stance combineArmsStance(Unit u, Unit armor, Unit infantry) {
            if (armor != null && manhattan(u.x, u.y, armor.x, armor.y) <= 3) {
                return Stance.OVERWATCH;
            }
            return getDefaultStance();
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
            return u.movementPoints >= 4;
        }

        @Override public Stance getDefaultStance() {
            return Stance.AGGRESSIVE;
        }

        @Override public boolean useEcheonDefense(Unit u, Unit target) {
            return false;
        }

        @Override public boolean shouldMassArmor(Unit u) {
            return false;
        }

        @Override public Stance combineArmsStance(Unit u, Unit armor, Unit infantry) {
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

        @Override public boolean useEcheonDefense(Unit u, Unit target) {
            return false;
        }

        @Override public boolean shouldMassArmor(Unit u) {
            return true;
        }

        @Override public Stance combineArmsStance(Unit u, Unit armor, Unit infantry) {
            if (infantry != null && u.category() == UnitCategory.ARMOR) {
                return Stance.OVERWATCH;
            }
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

        @Override public boolean useEcheonDefense(Unit u, Unit target) {
            return false;
        }

        @Override public boolean shouldMassArmor(Unit u) {
            return true;
        }

        @Override public Stance combineArmsStance(Unit u, Unit armor, Unit infantry) {
            return Stance.AGGRESSIVE;
        }
    };

    public abstract void apply(Unit u);
    public abstract boolean shouldFlank(Unit u);
    public abstract Stance getDefaultStance();

    public boolean useEcheonDefense(Unit u, Unit target) {
        return false;
    }

    public boolean shouldMassArmor(Unit u) {
        return false;
    }

    public Stance combineArmsStance(Unit u, Unit armor, Unit infantry) {
        return getDefaultStance();
    }

    public static Doctrine fromFaction(Faction f) {
        return switch (f) {
            case USA -> Doctrine.NATO;
            case RUSSIA -> Doctrine.RUSSIAN;
            case IRAN -> Doctrine.IRANIAN;
            case CHINA -> Doctrine.CHINESE;
            default -> Doctrine.NATO;
        };
    }

    private static int manhattan(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }
}