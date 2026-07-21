package com.contactfront.engine.model;

public enum DroneInterface {
    Direct_PiP("Direct PiP", "Piloted-in-Picture manual control", 0.0, 0.0, false),
    Recon_Linked("Recon Linked", "Recon-targeted auto-strike", 0.0, 0.0, true),
    Waypoint_Saturation("Waypoint Saturation", "Fire-and-forget waypoint planning", 50.0, 0.5, false);

    private final String name;
    private final String description;
    private final double insDriftRate; // meters per second under GPS jamming
    private final double weatherSensitivity; // affects signal/thermal
    private final boolean requiresSpotter;

    DroneInterface(String name, String description, double insDriftRate, double weatherSensitivity, boolean requiresSpotter) {
        this.name = name;
        this.description = description;
        this.insDriftRate = insDriftRate;
        this.weatherSensitivity = weatherSensitivity;
        this.requiresSpotter = requiresSpotter;
    }

    public String id() { return name; }
    public String description() { return description; }
    public double insDriftRate() { return insDriftRate; }
    public double weatherSensitivity() { return weatherSensitivity; }
    public boolean requiresSpotter() { return requiresSpotter; }

    public static DroneInterface fromId(String id) {
        for (DroneInterface di : values()) {
            if (di.name.equalsIgnoreCase(id)) return di;
        }
        return Direct_PiP;
    }
}