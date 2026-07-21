package com.contactfront.ui;

import com.contactfront.engine.model.*;
import com.contactfront.engine.trigger.TriggerNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public final class ScenarioSerializer {
    private ScenarioSerializer() {}

    public static void save(GameState s, File file) throws IOException {
        JSONObject root = new JSONObject();

        JSONObject metadata = new JSONObject();
        metadata.put("name", s.scenarioId);
        metadata.put("description", "Scenario exported from Contact Front");
        metadata.put("playerFaction", s.playerFaction != null ? s.playerFaction.name() : "USA");
        metadata.put("enemyFaction", s.enemyFaction != null ? s.enemyFaction.name() : "RUSSIA");
        metadata.put("locationName", s.locationName != null ? s.locationName : "");
        metadata.put("latitude", s.latitude);
        metadata.put("longitude", s.longitude);
        metadata.put("minLat", s.minLat);
        metadata.put("maxLat", s.maxLat);
        metadata.put("minLon", s.minLon);
        metadata.put("maxLon", s.maxLon);
        root.put("scenario_metadata", metadata);

        JSONObject environment = new JSONObject();
        environment.put("isNight", s.isNight);
        environment.put("isRaining", s.isRaining);
        environment.put("isWindy", s.isWindy);
        root.put("environment", environment);

        JSONArray unitPlacements = new JSONArray();
        List<Unit> all = s.allUnits();
        for (Unit u : all) {
            JSONObject uj = new JSONObject();
            uj.put("id", u.id);
            uj.put("faction", u.faction.name());
            uj.put("profile", u.profile.id());
            uj.put("sidc", u.sidcCode != null ? u.sidcCode : 
                   SymbolRegistry.getSidcForUnit(u.profile.id(), u.faction).code());
            uj.put("x", u.x);
            uj.put("y", u.y);
            uj.put("heading", Math.toDegrees(Math.atan2(u.weaponFacingY, u.weaponFacingX)));
            unitPlacements.put(uj);
        }
        root.put("unit_placements", unitPlacements);

        JSONArray obstacles = new JSONArray();
        for (Obstacle o : s.obstacles) {
            JSONObject oj = new JSONObject();
            oj.put("type", o.type().name());
            JSONArray fp = new JSONArray();
            for (double[] p : o.footprint()) {
                fp.put(p[0]).put(p[1]);
            }
            oj.put("footprint", fp);
            obstacles.put(oj);
        }
        root.put("obstacles", obstacles);

        JSONArray graphics = new JSONArray();
        if (s.tacticalGraphics != null) {
            for (var g : s.tacticalGraphics.getAllGraphics()) {
                JSONObject gj = new JSONObject();
                gj.put("type", g.type().name());
                JSONArray pts = new JSONArray();
                for (double[] p : g.points()) {
                    pts.put(p[0]).put(p[1]);
                }
                gj.put("points", pts);
                graphics.put(gj);
            }
        }
        root.put("tactical_graphics", graphics);

        JSONArray triggers = new JSONArray();
        if (s.triggers != null && s.triggers.triggers != null) {
            for (var t : s.triggers.triggers) {
                JSONObject tj = new JSONObject();
                tj.put("id", t.id());
                tj.put("description", t.description());
                triggers.put(tj);
            }
        }
        root.put("triggers", triggers);

        JSONArray objectives = new JSONArray();
        for (Objective o : s.objectives) {
            JSONObject oj = new JSONObject();
            oj.put("id", o.id);
            oj.put("name", o.name);
            oj.put("x", o.x);
            oj.put("y", o.y);
            oj.put("type", o.type);
            objectives.put(oj);
        }
        root.put("objectives", objectives);

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(root.toString(2));
        }
    }
}