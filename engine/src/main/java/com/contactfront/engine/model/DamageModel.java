package com.contactfront.engine.model;

public enum DamageModel {
    Bustle_Protected("Bustle Protected", "Protected ammunition storage, reduced catastrophic kill", 0.5, 0.3, 1.0),
    Hull_Carousel("Hull Carousel", "Carousel autoloader in hull, elevated catastrophic kill risk", 1.0, 0.35, 1.0),
    Front_Engine_Enhanced("Front Engine Enhanced", "Engine block armor enhancement, reduced mobility kill", 1.0, 0.35, 0.8);

    private final String name;
    private final String description;
    private final double cookoffMultiplier;
    private final double catastrophicThreshold;
    private final double mobilityKillMultiplier;

    DamageModel(String name, String description, double cookoffMultiplier, double catastrophicThreshold, double mobilityKillMultiplier) {
        this.name = name;
        this.description = description;
        this.cookoffMultiplier = cookoffMultiplier;
        this.catastrophicThreshold = catastrophicThreshold;
        this.mobilityKillMultiplier = mobilityKillMultiplier;
    }

    public String id() { return name; }
    public String description() { return description; }
    public double cookoffMultiplier() { return cookoffMultiplier; }
    public double catastrophicThreshold() { return catastrophicThreshold; }
    public double mobilityKillMultiplier() { return mobilityKillMultiplier; }

    public static DamageModel fromId(String id) {
        for (DamageModel dm : values()) {
            if (dm.name.equalsIgnoreCase(id)) return dm;
        }
        return Bustle_Protected;
    }
}