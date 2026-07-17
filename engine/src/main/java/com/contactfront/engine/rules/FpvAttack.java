package com.contactfront.engine.rules;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.Weapon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class FpvAttack {
    private FpvAttack() {}

    public static boolean strike(GameState s, Unit attacker, Unit target, Random rng) {
        Weapon w = attacker.weapons.stream().filter(x -> x.ammo > 0).findFirst().orElse(null);
        if (w == null) {
            s.log("combat", attacker.profile.name() + " has no FPV munitions left.");
            return false;
        }
        int dist = Math.abs(attacker.x - target.x) + Math.abs(attacker.y - target.y);
        if (dist > w.profile.range()) {
            s.log("combat", attacker.profile.name() + " drone out of range of " + target.profile.name() + ".");
            return false;
        }
        w.ammo--;
        boolean hit = rng.nextInt(100) < attacker.baseAccuracy;
        if (hit) {
            double dmg = 30 + rng.nextDouble() * 30;
            target.strength -= dmg;
            s.log("combat", attacker.profile.name() + " drone strikes " + target.profile.name() + " for " + (int) dmg + " damage!");
            if (target.strength <= 0 && !target.destroyed) Combat.destroy(s, target);
            List<Unit> all = new ArrayList<>(s.enemyUnits);
            all.addAll(s.friendlyUnits);
            for (Unit u : all) {
                if (u.destroyed || u == target) continue;
                if (Math.abs(u.x - target.x) + Math.abs(u.y - target.y) <= 1) {
                    double sd = 10 + rng.nextDouble() * 10;
                    u.strength -= sd;
                    s.log("combat", "Splash damage hits " + u.profile.name() + " for " + (int) sd + ".");
                    if (u.strength <= 0 && !u.destroyed) Combat.destroy(s, u);
                }
            }
        } else {
            s.log("combat", attacker.profile.name() + " drone misses " + target.profile.name() + ".");
        }
        attacker.destroyed = true;
        s.log("combat", attacker.profile.name() + " expended in explosion.");
        return true;
    }
}
