package com.contactfront.engine.model;

public record FactionBlueprint(
    Faction faction,
    NetworkTopology networkTopology,
    SensorEmission sensorEmission,
    DamageModel damageModel,
    DroneInterface droneInterface,
    double networkShieldMultiplier
) {
    public static FactionBlueprint defaults(Faction f) {
        return new FactionBlueprint(
            f,
            NetworkTopology.Type_A,
            SensorEmission.Active_RF,
            DamageModel.Bustle_Protected,
            DroneInterface.Direct_PiP,
            1.0
        );
    }
}