package com.contactfront.ui;

import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.*;
import com.contactfront.engine.rules.Movement;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class SaveManager {
    private SaveManager() {}

    public record Loaded(GameState state, long seed) {}

    public static void save(GameState s, Profiles profiles, long seed, File file) throws IOException {
        JSONObject root = new JSONObject();
        root.put("seed", seed);
        root.put("turn", s.turn);
        root.put("maxTurns", s.maxTurns);
        root.put("mode", s.mode);
        root.put("playerFaction", s.playerFaction != null ? s.playerFaction.name() : "USA");
        root.put("enemyFaction", s.enemyFaction != null ? s.enemyFaction.name() : "RUSSIA");
        root.put("commandMode", s.commandMode != null ? s.commandMode.name() : "EXPLICIT");
        JSONObject doctrinesObj = new JSONObject();
        for (var entry : s.factionDoctrines.entrySet()) {
            doctrinesObj.put(entry.getKey().name(), entry.getValue().name());
        }
        root.put("factionDoctrines", doctrinesObj);
        root.put("artilleryFiresRemaining", s.artilleryFiresRemaining);
        root.put("casAvailable", s.casAvailable);
        root.put("smokeGrenades", s.smokeGrenades);
        root.put("enemyArtilleryFiresRemaining", s.enemyArtilleryFiresRemaining);
        root.put("enemyCasAvailable", s.enemyCasAvailable);
        root.put("isNight", s.isNight);
        root.put("isRaining", s.isRaining);
        root.put("ewGpsJammed", s.ewGpsJammed);
        root.put("ewCommsJammed", s.ewCommsJammed);
        root.put("gameOver", s.gameOver);
        root.put("victory", s.victory == null ? JSONObject.NULL : s.victory);

        JSONArray grid = new JSONArray();
        for (int y = 0; y < s.height(); y++) {
            JSONArray row = new JSONArray();
            for (int x = 0; x < s.width(); x++) row.put(s.grid[y][x].type.name());
            grid.put(row);
        }
        root.put("grid", grid);

        JSONArray visibility = new JSONArray();
        for (int y = 0; y < s.height(); y++) {
            JSONArray row = new JSONArray();
            for (int x = 0; x < s.width(); x++) row.put(s.visibility[y][x].code);
            visibility.put(row);
        }
        root.put("visibility", visibility);

        JSONArray units = new JSONArray();
        List<Unit> all = new ArrayList<>(s.friendlyUnits);
        all.addAll(s.enemyUnits);
        for (Unit u : all) {
            if (u.destroyed) continue;
            JSONObject ju = new JSONObject();
            ju.put("id", u.id);
            ju.put("faction", u.faction.name());
            ju.put("profile", u.profile.id());
            ju.put("x", u.x);
            ju.put("y", u.y);
            ju.put("strength", u.strength);
            ju.put("morale", u.morale);
            ju.put("suppression", u.suppression);
            ju.put("stance", u.stance.name());
            ju.put("movementPoints", u.movementPoints);
            ju.put("routed", u.routed);
            ju.put("mobilityKill", u.mobilityKill);
            ju.put("firepowerKill", u.firepowerKill);
            ju.put("orderDelayTurns", u.orderDelayTurns);
            ju.put("turnsSuppressed", u.turnsSuppressed);
            ju.put("underFireThisTurn", u.underFireThisTurn);
            ju.put("knownToPlayer", u.knownToPlayer);
            ju.put("lastKnownX", u.lastKnownX);
            ju.put("lastKnownY", u.lastKnownY);
            ju.put("lastSeenTurn", u.lastSeenTurn);
            JSONArray wpn = new JSONArray();
            for (Weapon w : u.weapons) {
                JSONObject jw = new JSONObject();
                jw.put("id", w.profile.id());
                jw.put("ammo", w.ammo);
                wpn.put(jw);
            }
            ju.put("weapons", wpn);
            units.put(ju);
        }
        root.put("units", units);

        JSONArray objs = new JSONArray();
        for (Objective o : s.objectives) {
            JSONObject jo = new JSONObject();
            jo.put("id", o.id);
            jo.put("name", o.name);
            jo.put("x", o.x);
            jo.put("y", o.y);
            jo.put("type", o.type);
            jo.put("required", o.required);
            objs.put(jo);
        }
        root.put("objectives", objs);

        JSONArray depots = new JSONArray();
        for (int[] d : s.supplyDepots) {
            JSONArray a = new JSONArray();
            a.put(d[0]); a.put(d[1]);
            depots.put(a);
        }
        root.put("supplyDepots", depots);

        JSONArray log = new JSONArray();
        for (String l : s.narrativeLog) log.put(l);
        root.put("log", log);

        try (FileWriter fw = new FileWriter(file)) { fw.write(root.toString(2)); }
    }

    public static Loaded load(File file, Profiles profiles) throws IOException {
        String text = new String(Files.readAllBytes(file.toPath()));
        JSONObject root = new JSONObject(text);
        long seed = root.getLong("seed");
        GameState s = new GameState();
        s.seed = seed;
        s.turn = root.getInt("turn");
        s.maxTurns = root.getInt("maxTurns");
        s.mode = root.getString("mode");
        s.playerFaction = Faction.valueOf(root.getString("playerFaction"));
        s.enemyFaction = Faction.valueOf(root.getString("enemyFaction"));
        s.commandMode = CommandMode.valueOf(root.optString("commandMode", "EXPLICIT"));
        if (root.has("factionDoctrines")) {
            JSONObject doctrinesObj = root.getJSONObject("factionDoctrines");
            for (String key : doctrinesObj.keySet()) {
                Faction f = Faction.valueOf(key);
                Doctrine d = Doctrine.valueOf(doctrinesObj.getString(key));
                s.factionDoctrines.put(f, d);
            }
        }
        s.artilleryFiresRemaining = root.optInt("artilleryFiresRemaining", 3);
        s.casAvailable = root.optInt("casAvailable", 2);
        s.smokeGrenades = root.optInt("smokeGrenades", 2);
        s.enemyArtilleryFiresRemaining = root.optInt("enemyArtilleryFiresRemaining", 3);
        s.enemyCasAvailable = root.optInt("enemyCasAvailable", 2);
        s.isNight = root.optBoolean("isNight", false);
        s.isRaining = root.optBoolean("isRaining", false);
        s.ewGpsJammed = root.optBoolean("ewGpsJammed", false);
        s.ewCommsJammed = root.optBoolean("ewCommsJammed", false);
        s.gameOver = root.optBoolean("gameOver", false);
        s.victory = root.isNull("victory") ? null : root.getBoolean("victory");

        JSONArray grid = root.getJSONArray("grid");
        int h = grid.length();
        int w = grid.getJSONArray(0).length();
        s.grid = new Tile[h][w];
        for (int y = 0; y < h; y++) {
            JSONArray row = grid.getJSONArray(y);
            for (int x = 0; x < w; x++) {
                s.grid[y][x] = new Tile(Terrain.valueOf(row.getString(x)), x, y);
            }
        }
        s.ensureVisibility();
        if (root.has("visibility")) {
            JSONArray vis = root.getJSONArray("visibility");
            for (int y = 0; y < h; y++) {
                JSONArray row = vis.getJSONArray(y);
                for (int x = 0; x < w; x++) {
                    s.visibility[y][x] = Visibility.of(row.getInt(x));
                }
            }
        }

        JSONArray units = root.getJSONArray("units");
        for (Object o : units) {
            JSONObject ju = (JSONObject) o;
            UnitProfile up = profiles.unit(ju.getString("profile"));
            Faction f = Faction.valueOf(ju.getString("faction"));
            Unit u = new Unit(ju.getInt("id"), f, up, ju.getInt("x"), ju.getInt("y"), profiles);
            u.strength = ju.getDouble("strength");
            u.morale = ju.getDouble("morale");
            u.suppression = ju.getDouble("suppression");
            u.stance = Stance.valueOf(ju.getString("stance"));
            u.movementPoints = ju.getInt("movementPoints");
            u.routed = ju.getBoolean("routed");
            u.mobilityKill = ju.getBoolean("mobilityKill");
            u.firepowerKill = ju.getBoolean("firepowerKill");
            u.orderDelayTurns = ju.optInt("orderDelayTurns", 0);
            u.turnsSuppressed = ju.optInt("turnsSuppressed", 0);
            u.underFireThisTurn = ju.optBoolean("underFireThisTurn", false);
            u.knownToPlayer = ju.optBoolean("knownToPlayer", false);
            u.lastKnownX = ju.optInt("lastKnownX", -1);
            u.lastKnownY = ju.optInt("lastKnownY", -1);
            u.lastSeenTurn = ju.optInt("lastSeenTurn", -1);
            JSONArray wpn = ju.getJSONArray("weapons");
            for (int i = 0; i < wpn.length() && i < u.weapons.size(); i++) {
                u.weapons.get(i).ammo = ((JSONObject) wpn.get(i)).getInt("ammo");
            }
            if (f == s.playerFaction) s.friendlyUnits.add(u); else s.enemyUnits.add(u);
        }

        JSONArray objs = root.getJSONArray("objectives");
        for (Object o : objs) {
            JSONObject jo = (JSONObject) o;
            Objective ob = new Objective(jo.getString("id"), jo.getString("name"), jo.getInt("x"), jo.getInt("y"), jo.getString("type"));
            ob.required = jo.optBoolean("required", true);
            s.objectives.add(ob);
        }

        JSONArray depots = root.getJSONArray("supplyDepots");
        for (Object o : depots) {
            JSONArray a = (JSONArray) o;
            s.supplyDepots.add(new int[]{a.getInt(0), a.getInt(1)});
        }

        JSONArray log = root.getJSONArray("log");
        for (Object l : log) s.narrativeLog.add(l.toString());

        return new Loaded(s, seed);
    }
}
