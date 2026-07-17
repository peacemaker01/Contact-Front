package com.contactfront.engine.data;

import com.contactfront.engine.model.ArmorClass;
import com.contactfront.engine.model.DamageClass;

public final class DamageMatrix {
    private DamageMatrix() {}

    public static double multiplier(DamageClass dc, ArmorClass ac) {
        return switch (dc) {
            case LIGHT -> switch (ac) {
                case NONE -> 1.0;
                case LIGHT -> 0.5;
                case MEDIUM -> 0.25;
                case HEAVY -> 0.08;
            };
            case AT -> switch (ac) {
                case NONE -> 0.6;
                case LIGHT -> 1.1;
                case MEDIUM -> 1.0;
                case HEAVY -> 1.4;
            };
            case HEAVY -> switch (ac) {
                case NONE -> 1.3;
                case LIGHT -> 1.1;
                case MEDIUM -> 1.0;
                case HEAVY -> 0.85;
            };
        };
    }

    public static double armorMitigation(ArmorClass ac) {
        return switch (ac) {
            case NONE -> 0.0;
            case LIGHT -> 0.10;
            case MEDIUM -> 0.20;
            case HEAVY -> 0.35;
        };
    }

    public static double baseDamage(DamageClass dc) {
        return switch (dc) {
            case LIGHT -> 10.0;
            case AT -> 22.0;
            case HEAVY -> 34.0;
        };
    }
}
