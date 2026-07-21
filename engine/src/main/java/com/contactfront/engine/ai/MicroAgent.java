package com.contactfront.engine.ai;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;

import java.util.Random;

public final class MicroAgent {
    private MicroAgent() {}

    public static void execute(Unit u, GameState s, Random rng) {
        if (u.destroyed || u.stepsRemaining > 0) return;

        Unit target = findBestTarget(u, s);
        if (target != null && com.contactfront.engine.rules.Visibility.enemySees(s, u, target)) {
            int dist = Math.abs(u.x - target.x) + Math.abs(u.y - target.y);
            int range = bestRange(u);
            if (dist <= range) {
                com.contactfront.engine.rules.Combat.resolveFire(s, u, target, rng);
            }
        }
    }

    private static Unit findBestTarget(Unit u, GameState s) {
        Unit best = null;
        double bestScore = -1.0;
        var enemies = u.faction == s.playerFaction ? s.enemyUnits : s.friendlyUnits;
        for (Unit e : enemies) {
            if (e.destroyed) continue;
            double score = evaluateTarget(u, e);
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    private static double evaluateTarget(Unit u, Unit e) {
        double dist = Math.abs(e.x - u.x) + Math.abs(e.y - u.y);
        double weight = switch (e.profile.category()) {
            case ARMOR -> 1.5;
            case ARTILLERY, AIR_DEFENSE -> 1.2;
            case RECON -> 0.5;
            default -> 1.0;
        };
        return weight * e.strength / (1.0 + dist * 0.1);
    }

    private static int bestRange(Unit u) {
        int max = 0;
        for (var w : u.weapons) max = Math.max(max, w.profile.range());
        return Math.max(1, max);
    }
}