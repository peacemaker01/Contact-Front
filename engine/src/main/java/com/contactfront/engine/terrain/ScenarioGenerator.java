package com.contactfront.engine.terrain;

import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ScenarioGenerator {

    private ScenarioGenerator() {}

    private static final Map<String, Integer> COST = new HashMap<>();
    static {
        COST.put("inf_squad", 2);
        COST.put("mech_inf", 3);
        COST.put("recon_team", 3);
        COST.put("mbt", 6);
        COST.put("ifv", 5);
        COST.put("mortar_section", 3);
        COST.put("fpv_unit", 3);
        COST.put("engineer_squad", 3);
        COST.put("aa_team", 3);
        COST.put("supply_truck", 2);
    }

    private static final List<String> PLAYER_POOL = List.of(
            "mbt", "ifv", "inf_squad", "mech_inf", "recon_team",
            "mortar_section", "fpv_unit", "engineer_squad", "aa_team", "supply_truck");
    private static final List<String> ENEMY_POOL = List.of(
            "mbt", "ifv", "inf_squad", "mech_inf", "recon_team",
            "mortar_section", "fpv_unit", "engineer_squad", "aa_team", "supply_truck");

    public record ScenarioSpec(
            long seed,
            int width,
            int height,
            Faction playerFaction,
            Faction enemyFaction,
            int playerBudget,
            int enemyBudget,
            List<String> requiredEnemyUnits,
            List<String> terrainConstraints,
            int maxAttempts
    ) {
        public ScenarioSpec {
            if (terrainConstraints == null) terrainConstraints = List.of();
            if (requiredEnemyUnits == null) requiredEnemyUnits = List.of();
            if (maxAttempts <= 0) maxAttempts = 40;
            if (width <= 0) width = 28;
            if (height <= 0) height = 20;
        }
    }

    public record Generated(long effectiveSeed, GameState state) {}

    public static Generated generate(ScenarioSpec spec, Profiles profiles) {
        long effective = spec.seed;
        TerrainGenerator.GenResult gen = null;
        for (int attempt = 0; attempt < spec.maxAttempts; attempt++) {
            effective = spec.seed + attempt * 7919L;
            TerrainGenerator.GenResult candidate = TerrainGenerator.generate(effective, spec.width, spec.height);
            if (meetsConstraints(candidate, spec.terrainConstraints)) {
                gen = candidate;
                break;
            }
        }
        if (gen == null) {
            effective = spec.seed;
            gen = TerrainGenerator.generate(effective, spec.width, spec.height);
        }

        GameState s = new GameState();
        s.seed = effective;
        s.grid = gen.grid();
        s.elevation = gen.elevation();
        s.moisture = gen.moisture();
        s.playerFaction = spec.playerFaction;
        s.enemyFaction = spec.enemyFaction;
        s.mode = "attacker";
        s.ensureVisibility();

        int idCounter = 1;
        int px = 2;
        int py = spec.height / 2;
        List<UnitProfile> playerForce = buyForce(spec.playerBudget, List.of(), PLAYER_POOL, new Random(effective ^ 0x1111), profiles);
        for (UnitProfile up : playerForce) {
            Unit u = spawn(s, up, spec.playerFaction, px, py, idCounter++, profiles);
            if (u != null) s.friendlyUnits.add(u);
        }
        s.supplyDepots.add(new int[]{px, py});

        int[] obj = gen.settlement() != null ? gen.settlement() : (gen.riverCrossing() != null ? gen.riverCrossing() : new int[]{spec.width / 2, spec.height / 2});
        int ex = spec.width - 3;
        int ey = obj[1];
        List<UnitProfile> enemyForce = buyForce(spec.enemyBudget, spec.requiredEnemyUnits, ENEMY_POOL, new Random(effective ^ 0x2222), profiles);
        for (UnitProfile up : enemyForce) {
            Unit u = spawn(s, up, spec.enemyFaction, ex, ey, idCounter++, profiles);
            if (u != null) s.enemyUnits.add(u);
        }
        s.supplyDepots.add(new int[]{ex, ey});

        String objName = gen.riverCrossing() != null ? "Control the Bridge" : (gen.settlement() != null ? "Secure the Town" : "Seize the Crossroads");
        s.objectives.add(new Objective("OBJ1", objName, obj[0], obj[1], "capture"));

        return new Generated(effective, s);
    }

    private static boolean meetsConstraints(TerrainGenerator.GenResult gen, List<String> constraints) {
        for (String c : constraints) {
            if (c.equals("river_crossing") && gen.riverCrossing() == null) return false;
            if (c.equals("settlement") && gen.settlement() == null) return false;
        }
        return true;
    }

    private static List<UnitProfile> buyForce(int budget, List<String> required, List<String> pool, Random rnd, Profiles profiles) {
        List<UnitProfile> out = new ArrayList<>();
        int remaining = budget;
        for (String r : required) {
            if (profiles.hasUnit(r)) {
                out.add(profiles.unit(r));
                remaining -= COST.getOrDefault(r, 3);
            }
        }
        List<String> choices = new ArrayList<>(pool);
        int guard = 0;
        while (remaining > 0 && guard++ < 200) {
            if (choices.isEmpty()) break;
            String pick = choices.get(rnd.nextInt(choices.size()));
            int cost = COST.getOrDefault(pick, 99);
            if (cost <= remaining && profiles.hasUnit(pick)) {
                out.add(profiles.unit(pick));
                remaining -= cost;
            }
            if (cost > remaining) { final int left = remaining; choices.removeIf(c -> COST.getOrDefault(c, 99) > left); }
        }
        return out;
    }

    private static Unit spawn(GameState s, UnitProfile up, Faction faction, int ax, int ay, int id, Profiles profiles) {
        int[] spot = findFree(s, ax, ay);
        if (spot == null) return null;
        return new Unit(id, faction, up, spot[0], spot[1], profiles);
    }

    private static int[] findFree(GameState s, int ax, int ay) {
        int best = -1, bestX = 0, bestY = 0;
        for (int y = 0; y < s.height(); y++) {
            for (int x = 0; x < s.width(); x++) {
                Tile t = s.grid[y][x];
                if (t.impassable() || t.type == Terrain.BUILDING) continue;
                if (occupied(s, x, y)) continue;
                int d = Math.abs(x - ax) + Math.abs(y - ay);
                if (best < 0 || d < best) { best = d; bestX = x; bestY = y; }
            }
        }
        return best < 0 ? null : new int[]{bestX, bestY};
    }

    private static boolean occupied(GameState s, int x, int y) {
        for (Unit u : s.friendlyUnits) if (!u.destroyed && u.x == x && u.y == y) return true;
        for (Unit u : s.enemyUnits) if (!u.destroyed && u.x == x && u.y == y) return true;
        return false;
    }
}
