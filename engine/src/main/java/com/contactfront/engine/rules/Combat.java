package com.contactfront.engine.rules;

import com.contactfront.engine.data.DamageMatrix;
import com.contactfront.engine.model.Faction;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Tile;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.Weapon;

import java.util.Random;

public final class Combat {
    private Combat() {}

    public record FireResult(boolean executed, boolean hit, double damage, String weaponId) {}

    public static FireResult resolveFire(GameState s, Unit attacker, Unit target, Random rng) {
        return resolveFire(s, attacker, target, rng, false);
    }

    public static FireResult resolveFire(GameState s, Unit attacker, Unit target, Random rng, boolean air) {
        if (target == null || target.destroyed) return new FireResult(false, false, 0, null);
        int dist = Math.abs(attacker.x - target.x) + Math.abs(attacker.y - target.y);
        Weapon weapon = attacker.bestWeaponVs(target, air);
        if (weapon == null) {
            s.log("combat", attacker.profile.name() + " has no usable weapon vs " + target.profile.name() + ".");
            return new FireResult(false, false, 0, null);
        }
        if (dist > weapon.profile.range()) {
            s.log("combat", attacker.profile.name() + " out of range of " + target.profile.name() + ".");
            return new FireResult(false, false, 0, null);
        }

        int cover = s.grid[target.y][target.x].coverBonus;
        double coverBonus = (cover * target.stance.coverReceivedMult) + (target.entrenchment * 15);
        int envPenalty = 0;
        if (s.isNight) envPenalty += 20;
        if (s.isRaining) envPenalty += 10;
        double attackerSupp = Math.min(30.0, attacker.suppression * 0.3);

        int accuracy = attacker.baseAccuracy
                + attacker.faction.accuracyBonus
                + attacker.stance.accuracyMod
                - (int) coverBonus
                - envPenalty
                - (int) attackerSupp;

        if (air && target.faction == Faction.USA && attacker.profile.category() == com.contactfront.engine.model.UnitCategory.DRONE) {
            accuracy -= 30;
        }
        if (attacker.faction == Faction.USA) {
            accuracy += 8;
        }

        boolean anyHit = false;
        double totalDamage = 0;

        for (int shot = 0; shot < weapon.profile.rof() && weapon.ammo > 0; shot++) {
            weapon.ammo--;
            if (attacker.faction == s.playerFaction) s.ammoExpended++;
            int hitChance = clamp(accuracy + (rng.nextInt(21) - 10), 5, 95);
            boolean hit = rng.nextInt(100) < hitChance;
            if (hit) {
                double dmg = DamageMatrix.baseDamage(weapon.profile.damageClass())
                        * DamageMatrix.multiplier(weapon.profile.damageClass(), target.armorClass)
                        * (1.0 - DamageMatrix.armorMitigation(target.armorClass));
                if (target.isArmored()) {
                    double penetration = rng.nextDouble();
                    if (penetration < 0.3) {
                        dmg *= 0.5;
                        s.log("combat", "Armor degradation on " + target.profile.name() + ".");
                    }
                }
                target.strength -= dmg;
                target.morale -= dmg * 0.5;
                totalDamage += dmg;
                anyHit = true;
                applyVehicleDamage(s, target, rng);
            }
        }

        double incomingSuppression = weapon.profile.suppressionValue() * attacker.stance.outgoingSuppressionMult;
        incomingSuppression = Math.max(0, incomingSuppression - (target.entrenchment * 8));
        if (attacker.suppression >= 50) {
            incomingSuppression *= 1.3;
        }
        incomingSuppression *= target.incomingSuppressionMult;
        Suppression.applyIncoming(target, incomingSuppression);

        if (totalDamage > 0 && target.faction == s.playerFaction) s.friendlyWia++;
        if (target.strength <= 0 && !target.destroyed) destroy(s, target);
        checkRout(target, s);

        return new FireResult(true, anyHit, totalDamage, weapon.profile.id());
    }

    private static void applyVehicleDamage(GameState s, Unit target, Random rng) {
        if (!target.isArmored()) return;
        if (!target.mobilityKill && rng.nextDouble() < 0.3) {
            target.mobilityKill = true;
            target.movement = Math.max(1, target.movement / 2);
            s.log("combat", target.profile.name() + " mobility killed (movement halved).");
        }
        if (!target.firepowerKill && rng.nextDouble() < 0.2) {
            target.firepowerKill = true;
            target.baseAccuracy = Math.max(0, target.baseAccuracy - 30);
            s.log("combat", target.profile.name() + " firepower killed (accuracy reduced).");
        }
    }

    public static void destroy(GameState s, Unit u) {
        if (u.destroyed) return;
        u.destroyed = true;
        if (u.faction == s.playerFaction) {
            s.friendlyKia++;
            if (u.isArmored()) s.vehiclesLost++;
        } else {
            s.enemyKia++;
        }
        s.log("combat", u.profile.name() + " destroyed.");
    }

    public static boolean checkRout(Unit u, GameState s) {
        if (u.morale > u.getMoraleThreshold() || u.destroyed || u.routed) return false;
        u.routed = true;
        Unit nearest = null;
        int min = Integer.MAX_VALUE;
        for (Unit e : s.enemyUnits) {
            if (e.faction == u.faction || e.destroyed) continue;
            int d = Math.abs(u.x - e.x) + Math.abs(u.y - e.y);
            if (d < min) { min = d; nearest = e; }
        }
        if (nearest != null) {
            int dx = u.x - nearest.x;
            int dy = u.y - nearest.y;
            if (dx != 0) dx /= Math.abs(dx);
            if (dy != 0) dy /= Math.abs(dy);
            u.x = Math.max(0, Math.min(u.x + dx * 3, s.width() - 1));
            u.y = Math.max(0, Math.min(u.y + dy * 3, s.height() - 1));
        }
        u.suppression = 100;
        s.log("combat", u.profile.name() + " panics and routs!");
        return true;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
