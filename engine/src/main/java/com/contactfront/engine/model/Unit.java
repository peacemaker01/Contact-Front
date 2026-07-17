package com.contactfront.engine.model;

import com.contactfront.engine.data.Profiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Unit {
    public final int id;
    public final Faction faction;
    public final UnitProfile profile;
    public final List<Weapon> weapons;

    public int x;
    public int y;

    public int movement;
    public int movementPoints;
    public final ArmorClass armorClass;
    public final int reconRadius;
    public final int radioRange;
    public int baseAccuracy;

    public double strength = 100;
    public double morale = 100;
    public double suppression = 0;

    public Stance stance = Stance.DEFENSIVE;
    public Stance defaultStance = Stance.DEFENSIVE;
    public int entrenchment = 0;
    public boolean destroyed = false;
    public boolean routed = false;
    public boolean mobilityKill = false;
    public boolean firepowerKill = false;

    public boolean prefersDispersion = false;
    public boolean usesMassedAttacks = false;
    public double incomingSuppressionMult = 1.0;
    public int moraleThresholdOverride = -1;
    public boolean engageOnlyAtOptimalRange = false;
    public boolean engageAtAnyRange = false;

    public int orderDelayTurns = 0;
    public int turnsSuppressed = 0;
    public boolean underFireThisTurn = false;
    public boolean knownToPlayer = false;
    public int lastKnownX = -1;
    public int lastKnownY = -1;
    public int lastSeenTurn = -1;

    public long lastContactElapsedMs = -1;
    public int lastContactEnemyId = -1;
    public int lastContactX = -1;
    public int lastContactY = -1;
    public String lastContactName = "";

    public boolean wasSuppressed = false;

    public Unit(int id, Faction faction, UnitProfile profile, int x, int y, Profiles roster) {
        this.id = id;
        this.faction = faction;
        this.profile = profile;
        this.weapons = roster.instantiateWeapons(profile);
        this.x = x;
        this.y = y;
        this.movement = profile.move();
        this.movementPoints = profile.move();
        this.armorClass = profile.armorClass();
        this.reconRadius = profile.reconRadius();
        this.radioRange = profile.radioRange();
        this.baseAccuracy = profile.baseAccuracy();
    }

    public char typeCode() {
        return profile.category().glyph();
    }

    public boolean alive() {
        return !destroyed;
    }

    public boolean isArmored() {
        return armorClass == ArmorClass.MEDIUM || armorClass == ArmorClass.HEAVY;
    }

    public int totalAmmo() {
        int n = 0;
        for (Weapon w : weapons) n += w.ammo;
        return n;
    }

    public boolean hasAmmo() {
        return totalAmmo() > 0;
    }

    public boolean isSuppressed() {
        return suppression >= 40;
    }

    public boolean canTargetAir() {
        return weapons.stream().anyMatch(w -> w.ammo > 0 && w.profile.canTarget(TargetType.AIR));
    }

    public Weapon bestWeaponVs(Unit target, boolean air) {
        return weapons.stream()
                .filter(w -> w.ammo > 0)
                .filter(w -> air ? w.profile.canTarget(TargetType.AIR) : w.profile.canTarget(TargetType.GROUND))
                .max(Comparator.comparingDouble(
                        w -> com.contactfront.engine.data.DamageMatrix.multiplier(w.profile.damageClass(), target.armorClass)))
                .orElse(null);
    }

    public double effectiveMovementPoints() {
        if (suppression >= 50) return movementPoints * 0.5;
        if (suppression >= 25) return movementPoints * 0.75;
        return movementPoints;
    }

    public boolean hasSpecial(String s) {
        return profile.hasSpecial(s);
    }

    public void applyDoctrine(Doctrine doctrine) {
        doctrine.apply(this);
    }

    public int getMoraleThreshold() {
        return moraleThresholdOverride > 0 ? moraleThresholdOverride : 25;
    }
}
