package com.contactfront.engine.rules;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;

import java.util.Random;

/** Close air support: area damage with friendly-fire risk (1-turn delayed). */
public final class CloseAirSupport {
    private CloseAirSupport() {}

    public static void applyCas(GameState s, int tx, int ty, Random rng) {
        Unit aaTarget = findAaTarget(s, tx, ty);
        if (aaTarget != null) {
            double interceptChance = 0.4 + (aaTarget.profile.category() == com.contactfront.engine.model.UnitCategory.AIR_DEFENSE ? 0.4 : 0.2);
            if (rng.nextDouble() < interceptChance) {
                double dmg = 40 + rng.nextDouble() * 40;
                aaTarget.strength -= dmg;
                s.log("combat", "AA fire downing enemy CAS on (" + tx + "," + ty + ") - " + aaTarget.profile.name() + " takes " + (int) dmg + " damage!");
                if (aaTarget.strength <= 0 && !aaTarget.destroyed) Combat.destroy(s, aaTarget);
                return;
            }
        }

        for (Unit e : s.enemyUnits) {
            if (e.destroyed) continue;
            if (Math.abs(e.x - tx) <= 3 && Math.abs(e.y - ty) <= 3) {
                double dmg = 35 + rng.nextDouble() * 35;
                if (e.isArmored()) dmg *= 0.8;
                if (rng.nextDouble() < 0.15) {
                    dmg *= 1.8;
                    s.log("combat", "Critical bomb hit on " + e.profile.name() + "!");
                }
                e.strength -= dmg;
                s.log("combat", "CAS hits " + e.profile.name() + " for " + (int) dmg + " damage!");
                if (e.strength <= 0 && !e.destroyed) Combat.destroy(s, e);
            }
        }
        for (Unit f : s.friendlyUnits) {
            if (f.destroyed) continue;
            if (Math.abs(f.x - tx) <= 3 && Math.abs(f.y - ty) <= 3) {
                double dmg = 25 + rng.nextDouble() * 25;
                f.strength -= dmg;
                s.log("combat", "FRIENDLY FIRE: CAS hits " + f.profile.name() + " for " + (int) dmg + " damage!");
                if (f.strength <= 0 && !f.destroyed) Combat.destroy(s, f);
            }
        }
    }

    private static Unit findAaTarget(GameState s, int tx, int ty) {
        Unit best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Unit u : s.friendlyUnits) {
            if (u.destroyed) continue;
            if (u.profile.category() != com.contactfront.engine.model.UnitCategory.AIR_DEFENSE) continue;
            if (!u.hasAmmo()) continue;
            int dist = Math.abs(u.x - tx) + Math.abs(u.y - ty);
            if (dist < bestDist) {
                bestDist = dist;
                best = u;
            }
        }
        return best;
    }
}
