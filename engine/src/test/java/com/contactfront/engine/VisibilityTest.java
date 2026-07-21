package com.contactfront.engine;

import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.*;
import com.contactfront.engine.rules.Visibility;
import com.contactfront.engine.rules.Movement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VisibilityTest {

    @Test
    void thermalDegradationThroughForest() {
        GameState s = TestSupport.grid(5, 5, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit observer = TestSupport.unit(s, p, "grunt", Faction.USA, 0, 2, 1);
        Unit target = TestSupport.unit(s, p, "grunt", Faction.RUSSIA, 4, 2, 2);
        
        for (int i = 0; i < 5; i++) {
            Tile forest = s.grid[2][i];
            forest.type = Terrain.FOREST;
            forest.blocksLos = true;
            forest.forestDensity = 0.5;
        }
        
        assertFalse(Visibility.enemySees(s, observer, target));
    }

    @Test
    void clearSightThroughOpenTerrain() {
        GameState s = TestSupport.grid(5, 5, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit observer = TestSupport.unit(s, p, "grunt", Faction.USA, 0, 2, 1);
        Unit target = TestSupport.unit(s, p, "grunt", Faction.RUSSIA, 4, 2, 2);
        
        assertTrue(Visibility.enemySees(s, observer, target));
    }

    @Test
    void wetlandImpassable() {
        GameState s = TestSupport.grid(5, 1, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit infantry = TestSupport.unit(s, p, "grunt", Faction.USA, 0, 0, 1);
        
        TestSupport.setTerrain(s, 2, 0, Terrain.WETLAND);
        s.grid[0][2].movementCost = 999.0;
        
        double cost = Movement.pathCost(s.grid, 0, 0, 4, 0, UnitCategory.INFANTRY);
        assertTrue(Double.isInfinite(cost) || cost >= 999.0, "Wetland should be impassable");
    }

    @Test
    void fordPassable() {
        GameState s = TestSupport.grid(5, 1, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit infantry = TestSupport.unit(s, p, "grunt", Faction.USA, 0, 0, 1);
        
        TestSupport.setTerrain(s, 2, 0, Terrain.FORD);
        double cost = Movement.pathCost(s.grid, 0, 0, 4, 0, UnitCategory.INFANTRY);
        assertTrue(cost < 10.0, "Ford should be passable");
    }

    @Test
    void waterwayImpassable() {
        GameState s = TestSupport.grid(5, 1, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit infantry = TestSupport.unit(s, p, "grunt", Faction.USA, 0, 0, 1);
        
        TestSupport.setTerrain(s, 2, 0, Terrain.WATERWAY);
        s.grid[0][2].movementCost = 999.0;
        
        double cost = Movement.pathCost(s.grid, 0, 0, 4, 0, UnitCategory.INFANTRY);
        assertTrue(Double.isInfinite(cost) || cost >= 999.0, "Waterway should be impassable");
    }

    @Test
    void radarClutterBlocksLowAltitude() {
        GameState s = TestSupport.grid(5, 5, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        
        for (int i = 0; i < 5; i++) {
            Tile building = s.grid[2][i];
            building.type = Terrain.BUILDING;
            building.blocksLos = true;
        }
        
        Unit sam = TestSupport.unit(s, p, "tank", Faction.USA, 2, 2, 1);
        Unit target = TestSupport.unit(s, p, "grunt", Faction.RUSSIA, 0, 0, 2);
        
        assertFalse(Visibility.radarCanAcquire(s, sam, target));
    }
}