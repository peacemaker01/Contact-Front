package com.contactfront.engine.model;

import java.util.Set;

public record WeaponProfile(
        String id,
        String name,
        int range,
        DamageClass damageClass,
        int rof,
        int suppressionValue,
        int maxAmmo,
        Set<TargetType> targetTypes
) {
    public WeaponProfile {
        if (targetTypes == null) targetTypes = Set.of(TargetType.GROUND);
        if (targetTypes.isEmpty()) targetTypes = Set.of(TargetType.GROUND);
    }

    public boolean canTarget(TargetType t) {
        return targetTypes.contains(t);
    }
}
