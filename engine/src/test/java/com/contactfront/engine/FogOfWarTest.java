package com.contactfront.engine;

import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.*;
import com.contactfront.engine.rules.AiTurn;
import com.contactfront.engine.rules.Visibility;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class FogOfWarTest {

    @Test
    void lineOfSightBlockedByBuilding() {
        GameState s = TestSupport.grid(5, 1, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit friendly = TestSupport.unit(s, p, "grunt", Faction.USA, 0, 0, 1);
        Unit enemy = TestSupport.unit(s, p, "shooter", Faction.RUSSIA, 4, 0, 2);
        TestSupport.setTerrain(s, 2, 0, Terrain.BUILDING);
        assertFalse(Visibility.enemySees(s, enemy, friendly));
        TestSupport.setTerrain(s, 2, 0, Terrain.OPEN);
        assertTrue(Visibility.enemySees(s, enemy, friendly));
    }

    @Test
    void computeVisibilityMarksKnownEnemies() {
        GameState s = TestSupport.grid(12, 1, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit scout = TestSupport.unit(s, p, "shooter", Faction.USA, 0, 0, 1);
        Unit near = TestSupport.unit(s, p, "grunt", Faction.RUSSIA, 2, 0, 2);
        Unit far = TestSupport.unit(s, p, "grunt", Faction.RUSSIA, 9, 0, 3);
        Visibility.computePlayerVisibility(s);
        assertTrue(near.knownToPlayer);
        assertFalse(far.knownToPlayer);
    }

    @Test
    void aiNeverTargetsHiddenUnit() {
        GameState s = TestSupport.grid(11, 1, Terrain.OPEN);
        TestSupport.setTerrain(s, 5, 0, Terrain.BUILDING);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit hidden = TestSupport.unit(s, p, "grunt", Faction.USA, 0, 0, 1);
        Unit exposed = TestSupport.unit(s, p, "grunt", Faction.USA, 8, 0, 2);
        Unit enemy = TestSupport.unit(s, p, "shooter", Faction.RUSSIA, 10, 0, 3);
        enemy.movement = 0;
        enemy.movementPoints = 0;
        for (int i = 0; i < 5; i++) AiTurn.run(s, new Random(i + 1));
        assertEquals(100.0, hidden.strength, "hidden unit must never be engaged by AI");
        assertFalse(Visibility.enemySees(s, enemy, hidden));
    }

    @Test
    void smokeBlocksLineOfSight() {
        GameState s = TestSupport.grid(5, 1, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit friendly = TestSupport.unit(s, p, "grunt", Faction.USA, 0, 0, 1);
        Unit enemy = TestSupport.unit(s, p, "shooter", Faction.RUSSIA, 4, 0, 2);
        assertTrue(Visibility.enemySees(s, enemy, friendly));
        s.addSmoke(2, 0, 3);
        assertFalse(Visibility.enemySees(s, enemy, friendly));
    }
}
