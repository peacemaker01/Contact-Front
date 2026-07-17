package com.contactfront.engine;

import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.*;
import com.contactfront.engine.rules.Combat;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class CombatTest {

    @Test
    void bestWeaponSelectsHardCounter() {
        Profiles p = TestSupport.customRoster();
        Unit shooter = new Unit(1, Faction.USA, p.unit("shooter"), 0, 0, p);
        Unit tank = new Unit(2, Faction.RUSSIA, p.unit("tank"), 1, 0, p);
        Unit grunt = new Unit(3, Faction.RUSSIA, p.unit("grunt"), 1, 0, p);
        assertEquals("wpn_at", shooter.bestWeaponVs(tank, false).profile.id());
        assertEquals("wpn_heavy", shooter.bestWeaponVs(grunt, false).profile.id());
    }

    @Test
    void fireAppliesDamageSuppressionAndTracksAmmo() {
        GameState s = TestSupport.grid(8, 1, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit shooter = TestSupport.unit(s, p, "shooter", Faction.USA, 0, 0, 1);
        shooter.baseAccuracy = 200;
        Unit tank = TestSupport.unit(s, p, "tank", Faction.RUSSIA, 1, 0, 2);
        int ammoBefore = shooter.totalAmmo();

        Random rng = new Random(7);
        int guard = 0;
        while (tank.strength >= 100 && guard++ < 40) {
            Combat.resolveFire(s, shooter, tank, rng);
        }
        assertTrue(tank.strength < 100, "tank should have taken damage");
        assertTrue(shooter.totalAmmo() < ammoBefore, "ammo should be consumed");
        assertTrue(tank.suppression > 0, "target should be suppressed");
        assertTrue(s.ammoExpended > 0, "player ammo expended should be tracked");
    }
}
