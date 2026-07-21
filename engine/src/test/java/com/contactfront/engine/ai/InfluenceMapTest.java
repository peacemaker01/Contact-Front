package com.contactfront.engine.ai;

import com.contactfront.engine.model.Faction;
import com.contactfront.engine.TestSupport;
import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InfluenceMapTest {

    @Test
    void influenceMapInitializesCorrectly() {
        GameState s = TestSupport.grid(10, 10, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        TestSupport.unit(s, p, "tank", Faction.RUSSIA, 5, 5, 1);

        InfluenceMap map = new InfluenceMap(s);
        map.updateThreatMap(s);

        assertEquals(10, map.getThreatMap().length);
        assertEquals(10, map.getThreatMap()[0].length);
    }

    @Test
    void threatMapUpdatesAfterEnemyPlacement() {
        GameState s = TestSupport.grid(10, 10, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        
        InfluenceMap map = new InfluenceMap(s);
        double threatBefore = map.threatAt(5, 5);
        
        TestSupport.unit(s, p, "tank", Faction.RUSSIA, 7, 7, 1);
        map.updateThreatMap(s);
        
        double threatAfter = map.threatAt(5, 5);
        assertTrue(threatAfter > threatBefore, "Threat should increase after enemy placement");
    }

    @Test
    void coverMapReturnsZeroForOpenTerrain() {
        GameState s = TestSupport.grid(10, 10, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;

        InfluenceMap map = new InfluenceMap(s);
        map.updateThreatMap(s);

        assertEquals(0.0, map.coverAt(5, 5), 0.001);
    }

    @Test
    void coverMapReturnsPositiveForForest() {
        GameState s = TestSupport.grid(10, 10, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;

        TestSupport.setTerrain(s, 5, 5, Terrain.FOREST);
        s.grid[5][5].forestDensity = 0.7;
        
        InfluenceMap map = new InfluenceMap(s);
        map.updateThreatMap(s);

        assertTrue(map.coverAt(5, 5) > 0, "Forest should provide cover value");
    }
}