package com.contactfront.engine;

import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.rules.Movement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MovementTest {

    @Test
    void movesAndSpendsMovementPoints() {
        GameState s = TestSupport.grid(10, 10, Terrain.OPEN);
        Profiles p = TestSupport.customRoster();
        s.playerFaction = com.contactfront.engine.model.Faction.USA;
        Unit u = TestSupport.unit(s, p, "shooter", s.playerFaction, 0, 0, 1);
        u.movementPoints = u.movement;
        assertTrue(Movement.applyMove(s, u, 3, 0));
        assertEquals(3, u.x);
        assertTrue(u.movementPoints < u.movement);
    }

    @Test
    void blockedByImpassableTerrain() {
        GameState s = TestSupport.grid(10, 1, Terrain.OPEN);
        TestSupport.setTerrain(s, 5, 0, Terrain.WATER);
        Profiles p = TestSupport.customRoster();
        s.playerFaction = com.contactfront.engine.model.Faction.USA;
        Unit u = TestSupport.unit(s, p, "shooter", s.playerFaction, 0, 0, 1);
        u.movementPoints = 20;
        assertFalse(Movement.applyMove(s, u, 9, 0));
        assertEquals(0, u.x);
    }

    @Test
    void suppressionHalvesEffectiveMovement() {
        GameState s = TestSupport.grid(10, 1, Terrain.OPEN);
        Profiles p = TestSupport.customRoster();
        s.playerFaction = com.contactfront.engine.model.Faction.USA;
        Unit u = TestSupport.unit(s, p, "shooter", s.playerFaction, 0, 0, 1);
        u.movementPoints = 10;
        u.suppression = 60;
        assertFalse(Movement.applyMove(s, u, 6, 0));
        u.suppression = 0;
        assertTrue(Movement.applyMove(s, u, 6, 0));
    }
}
