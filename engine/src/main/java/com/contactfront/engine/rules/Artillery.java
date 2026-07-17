package com.contactfront.engine.rules;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Indirect fire: gaussian scatter around the target, area damage, friendly fire. */
public final class Artillery {
    private Artillery() {}

    private static final double WIND_EFFECT = 0.15;

    /** Resolve a strike at {@code (tx,ty)}. {@code cep} scales scatter (100 = jammed GPS). */
    public static int[] resolve(GameState s, int tx, int ty, int rounds, Random rng, int cep) {
        double windFactor = 1.0;
        if (s.isRaining) windFactor += WIND_EFFECT * (rng.nextDouble() - 0.5) * 2;
        
        int sx = (int) Math.round(rng.nextGaussian() * cep / 50.0 * windFactor);
        int sy = (int) Math.round(rng.nextGaussian() * cep / 50.0 * windFactor);
        int ix = Math.max(0, Math.min(tx + sx, s.width() - 1));
        int iy = Math.max(0, Math.min(ty + sy, s.height() - 1));

        for (Unit e : s.enemyUnits) {
            if (e.destroyed) continue;
            if (Math.abs(e.x - ix) <= 2 && Math.abs(e.y - iy) <= 2) {
                double dmg = 15 + rng.nextDouble() * 35;
                if (rng.nextDouble() < 0.25) {
                    dmg *= 1.5;
                    s.log("combat", "Critical hit on " + e.profile.name() + "!");
                }
                e.strength -= dmg;
                if (e.strength <= 0 && !e.destroyed) Combat.destroy(s, e);
            }
        }
        for (Unit f : s.friendlyUnits) {
            if (f.destroyed) continue;
            if (Math.abs(f.x - ix) <= 2 && Math.abs(f.y - iy) <= 2) {
                double dmg = 10 + rng.nextDouble() * 20;
                f.strength -= dmg;
                s.log("combat", "FRIENDLY FIRE: Artillery hits " + f.profile.name() + " for " + (int) dmg + " damage!");
                if (f.strength <= 0 && !f.destroyed) Combat.destroy(s, f);
            }
        }
        s.log("combat", "Artillery strike at (" + tx + "," + ty + ") — scatter to (" + ix + "," + iy + ").");
        return new int[]{ix, iy};
    }
}
