package com.contactfront.engine.model;

public enum SensorEmission {
    Active_RF("Active RF", "Emits radio frequency for targeting, visible to ESM/ARM", true, 1.0),
    Passive_EO_IR("Passive EO/IR", "Optical/thermal only, LOS required, stealthier", false, 0.5);

    private final String name;
    private final String description;
    private final boolean exposesToEsm;
    private final double weatherDegradation;

    SensorEmission(String name, String description, boolean exposesToEsm, double weatherDegradation) {
        this.name = name;
        this.description = description;
        this.exposesToEsm = exposesToEsm;
        this.weatherDegradation = weatherDegradation;
    }

    public String id() { return name; }
    public String description() { return description; }
    public boolean exposesToEsm() { return exposesToEsm; }
    public double weatherDegradation() { return weatherDegradation; }

    public static SensorEmission fromId(String id) {
        for (SensorEmission se : values()) {
            if (se.name.equalsIgnoreCase(id)) return se;
        }
        return Active_RF;
    }
}