package com.contactfront.engine.ai;

import com.contactfront.engine.model.Faction;
import com.contactfront.engine.TestSupport;
import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ThreatEvaluatorTest {

    @Test
    void staticThreatReturnsZeroWhenNoEnemies() {
        GameState s = TestSupport.grid(10, 10, com.contactfront.engine.model.Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        TestSupport.unit(s, p, "grunt", Faction.USA, 5, 5, 1);

        double threat = ThreatEvaluator.staticThreatAtPoint(s, 5, 5);
        assertEquals(0.0, threat, 0.001);
    }

    @Test
    void staticThreatIncreasesNearEnemies() {
        GameState s = TestSupport.grid(10, 10, com.contactfront.engine.model.Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        TestSupport.unit(s, p, "tank", Faction.RUSSIA, 5, 5, 1);

        double threatCenter = ThreatEvaluator.staticThreatAtPoint(s, 5, 5);
        double threatFar = ThreatEvaluator.staticThreatAtPoint(s, 0, 0);
        
        assertTrue(threatCenter > threatFar, "Threat should be higher near enemies");
    }

    @Test
    void threatDecaysWithDistance() {
        GameState s = TestSupport.grid(20, 1, com.contactfront.engine.model.Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        TestSupport.unit(s, p, "grunt", Faction.RUSSIA, 0, 0, 1);

        double threatNear = ThreatEvaluator.staticThreatAtPoint(s, 1, 0);
        double threatFar = ThreatEvaluator.staticThreatAtPoint(s, 10, 0);
        
        assertTrue(threatNear > threatFar, "Threat should decay with distance");
    }
}