package com.contactfront.engine.ai;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.UnitCategory;

public final class ThreatEvaluator {
    private ThreatEvaluator() {}

    private static final double DEFAULT_ALPHA = 0.001;
    private static final double DEFAULT_RANGE_DECAY = 0.05;

    public static double staticThreatAtPoint(GameState s, int x, int y) {
        double total = 0.0;
        for (Unit u : s.enemyUnits) {
            if (u.destroyed) continue;
            double dist = Math.sqrt(Math.pow(u.x - x, 2) + Math.pow(u.y - y, 2));
            double weight = getOffensiveWeight(u);
            total += weight * Math.exp(-DEFAULT_ALPHA * dist);
        }
        return total;
    }

    public static double dynamicThreatAtPoint(GameState s, int x, int y, Unit observer) {
        double staticThreat = staticThreatAtPoint(s, x, y);
        double facingFactor = 1.0;
        for (Unit u : s.enemyUnits) {
            if (u.destroyed) continue;
            double dist = Math.sqrt(Math.pow(u.x - x, 2) + Math.pow(u.y - y, 2));
            if (dist < 0.1) continue;
            double dx = (x - u.x) / dist;
            double dy = (y - u.y) / dist;
            double facingX = u.weaponFacingX;
            double facingY = u.weaponFacingY;
            double dot = facingX * dx + facingY * dy;
            facingFactor *= Math.max(0, dot);
        }
        return staticThreat * facingFactor;
    }

    public static double pathCostWithThreat(GameState s, int tx, int ty, double baseCost, Unit observer) {
        double dynamicThreat = dynamicThreatAtPoint(s, tx, ty, observer);
        double beta = getVulnerabilityMultiplier(observer);
        return baseCost * (1.0 + beta * dynamicThreat);
    }

    private static double getOffensiveWeight(Unit u) {
        double baseWeight = switch (u.profile.category()) {
            case ARMOR -> 1.0;
            case INFANTRY -> 0.3;
            case ARTILLERY, AIR_DEFENSE -> 0.8;
            case RECON -> 0.2;
            default -> 0.5;
        };
        return baseWeight * (u.strength / 100.0);
    }

    private static double getVulnerabilityMultiplier(Unit u) {
        if (u.profile.category() == UnitCategory.ARMOR) return 0.3;
        if (u.profile.category() == UnitCategory.LOGISTICS) return 1.0;
        if (u.profile.category() == UnitCategory.INFANTRY) return 0.6;
        return 0.5;
    }
}