package com.contactfront.engine;

import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.*;

public final class TestSupport {
    private TestSupport() {}

    public static GameState grid(int w, int h, Terrain t) {
        GameState s = new GameState();
        s.grid = new Tile[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) s.grid[y][x] = new Tile(t, x, y);
        s.ensureVisibility();
        return s;
    }

    public static void setTerrain(GameState s, int x, int y, Terrain t) {
        s.grid[y][x] = new Tile(t, x, y);
    }

    public static Unit unit(GameState s, Profiles p, String id, Faction f, int x, int y, int uid) {
        Unit u = new Unit(uid, f, p.unit(id), x, y, p);
        if (f == s.playerFaction) s.friendlyUnits.add(u);
        else s.enemyUnits.add(u);
        return u;
    }

    public static Profiles customRoster() {
        Profiles p = new Profiles();
        p.add(new com.contactfront.engine.model.WeaponProfile("wpn_light", "L", 12, DamageClass.LIGHT, 1, 2, 20, java.util.Set.of(TargetType.GROUND)));
        p.add(new com.contactfront.engine.model.WeaponProfile("wpn_at", "AT", 12, DamageClass.AT, 1, 2, 20, java.util.Set.of(TargetType.GROUND)));
        p.add(new com.contactfront.engine.model.WeaponProfile("wpn_heavy", "H", 12, DamageClass.HEAVY, 1, 4, 20, java.util.Set.of(TargetType.GROUND)));
        p.add(new UnitProfile("shooter", "S", UnitCategory.INFANTRY, java.util.List.of("wpn_light", "wpn_at", "wpn_heavy"), 3, ArmorClass.NONE, 4, 200, 10, java.util.List.of(), 1.0, 0.5, 0, false));
        p.add(new UnitProfile("tank", "T", UnitCategory.ARMOR, java.util.List.of("wpn_heavy"), 5, ArmorClass.HEAVY, 4, 50, 10, java.util.List.of(), 30.0, 0.7, 300, true));
        p.add(new UnitProfile("grunt", "G", UnitCategory.INFANTRY, java.util.List.of("wpn_light"), 3, ArmorClass.NONE, 4, 75, 10, java.util.List.of(), 1.0, 0.3, 0, false));
        return p;
    }
}
