package com.contactfront.engine.model;

import java.util.List;

public record UnitProfile(
        String id,
        String name,
        UnitCategory category,
        List<String> weapons,
        int move,
        ArmorClass armorClass,
        int reconRadius,
        int baseAccuracy,
        int radioRange,
        List<String> special,
        double rcsM2,
        double thermalSignature,
        int armorThickness,
        boolean eraPresent
) {
    public UnitProfile {
        if (weapons == null) weapons = List.of();
        if (special == null) special = List.of();
    }

    public boolean hasSpecial(String s) {
        return special.contains(s);
    }
}
