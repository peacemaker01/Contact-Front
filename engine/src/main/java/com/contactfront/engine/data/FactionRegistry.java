package com.contactfront.engine.data;

import com.contactfront.engine.model.*;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class FactionRegistry {
    private static final Map<Faction, FactionBlueprint> cache = new HashMap<>();

    static {
        loadBlueprints();
    }

    private FactionRegistry() {}

    private static void loadBlueprints() {
        for (Faction f : Faction.values()) {
            try {
                String path = "/com/contactfront/engine/data/factions/" + f.name().toLowerCase() + ".json";
                InputStream in = FactionRegistry.class.getResourceAsStream(path);
                if (in != null) {
                    JSONObject json = new JSONObject(new JSONTokener(in));
                    FactionBlueprint bp = parseBlueprint(json, f);
                    cache.put(f, bp);
                } else {
                    cache.put(f, FactionBlueprint.defaults(f));
                }
            } catch (Exception e) {
                cache.put(f, FactionBlueprint.defaults(f));
            }
        }
    }

    private static FactionBlueprint parseBlueprint(JSONObject json, Faction f) {
        NetworkTopology nt = NetworkTopology.fromId(json.optString("network_topology", "Centralized"));
        SensorEmission se = SensorEmission.fromId(json.optString("sensor_emission", "Active RF"));
        DamageModel dm = DamageModel.fromId(json.optString("damage_model", "Bustle Protected"));
        DroneInterface di = DroneInterface.fromId(json.optString("drone_interface", "Direct PiP"));
        double shield = json.optDouble("network_shield_multiplier", 1.0);

        return new FactionBlueprint(f, nt, se, dm, di, shield);
    }

    public static FactionBlueprint getBlueprint(Faction f) {
        return cache.getOrDefault(f, FactionBlueprint.defaults(f));
    }

    public static Map<Faction, FactionBlueprint> getAllBlueprints() {
        return new HashMap<>(cache);
    }
}