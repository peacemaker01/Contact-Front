package com.contactfront.engine.rules;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;

public final class Resupply {
    private Resupply() {}

    private static boolean nearDepot(GameState s, Unit u) {
        for (int[] depot : s.supplyDepots) {
            if (Math.abs(u.x - depot[0]) + Math.abs(u.y - depot[1]) <= 5) return true;
        }
        return false;
    }

    public static boolean requestResupply(GameState s, Unit u) {
        if (u.destroyed) return false;
        if (!nearDepot(s, u)) {
            s.log("orders", u.profile.name() + " is not within range of a supply depot.");
            return false;
        }
        refill(u);
        s.log("orders", u.profile.name() + " resupplied from depot.");
        return true;
    }

    public static void resupplyPhase(GameState s) {
        for (Unit u : s.friendlyUnits) {
            if (!u.destroyed && nearDepot(s, u)) refill(u);
        }
        s.log("orders", "Resupply phase complete.");
    }

    private static void refill(Unit u) {
        for (var w : u.weapons) w.ammo = w.maxAmmo;
        if (u.hasSpecial("repair")) u.strength = Math.min(100.0, u.strength + 25.0);
    }
}
