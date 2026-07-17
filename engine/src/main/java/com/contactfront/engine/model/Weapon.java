package com.contactfront.engine.model;

public final class Weapon {
    public final WeaponProfile profile;
    public int ammo;
    public final int maxAmmo;

    public Weapon(WeaponProfile profile, int ammo) {
        this.profile = profile;
        this.maxAmmo = ammo;
        this.ammo = ammo;
    }

    public Weapon(WeaponProfile profile) {
        this(profile, profile.maxAmmo());
    }
}
