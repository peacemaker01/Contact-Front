package com.contactfront.engine;

import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.*;
import com.contactfront.engine.rules.LineOfSight;
import com.contactfront.engine.rules.Movement;
import com.contactfront.engine.model.RoadSegment.RoadType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RoadMovementTest {

    @Test
    void motorwayFasterThanTrackForTracked() {
        GameState s = TestSupport.grid(3, 1, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit tank = TestSupport.unit(s, p, "tank", Faction.USA, 0, 0, 1);
        
        Tile motorwayTile = s.grid[0][1];
        motorwayTile.type = Terrain.ROAD;
        motorwayTile.roadType = RoadType.MOTORWAY;
        
        Tile openTile = s.grid[0][2];
        openTile.type = Terrain.OPEN;
        
        double costMotorway = Movement.pathCost(s.grid, 0, 0, 1, 0, UnitCategory.ARMOR);
        double costOpen = Movement.pathCost(s.grid, 0, 0, 2, 0, UnitCategory.ARMOR);
        assertTrue(costMotorway < costOpen, "Motorway should be faster for tracked vehicles than open terrain");
    }

    @Test
    void motorwayFasterForWheeled() {
        GameState s = TestSupport.grid(3, 1, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit tank = TestSupport.unit(s, p, "tank", Faction.USA, 0, 0, 1);
        
        Tile motorwayTile = s.grid[0][1];
        motorwayTile.type = Terrain.ROAD;
        motorwayTile.roadType = RoadType.MOTORWAY;
        
        double costMotorway = Movement.pathCost(s.grid, 0, 0, 1, 0, UnitCategory.LOGISTICS);
        assertTrue(costMotorway < 1.2, "Motorway should be fast for wheeled vehicles");
    }

    @Test
    void wetlandImpassable() {
        GameState s = TestSupport.grid(3, 1, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit infantry = TestSupport.unit(s, p, "grunt", Faction.USA, 0, 0, 1);
        
        Tile wetland = s.grid[0][1];
        wetland.type = Terrain.WETLAND;
        wetland.movementCost = 999.0;
        
        double cost = Movement.pathCost(s.grid, 0, 0, 2, 0, UnitCategory.INFANTRY);
        assertTrue(Double.isInfinite(cost) || cost >= 999.0, "Wetland should be impassable");
    }

    @Test
    void forestBlocksLOS() {
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
            forest.movementCost = 2.0;
        }
        
        assertFalse(LineOfSight.hasLineOfSight(0, 2, 4, 2, s.grid));
    }

    @Test
    void buildingBlocksLOS() {
        GameState s = TestSupport.grid(5, 1, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit observer = TestSupport.unit(s, p, "grunt", Faction.USA, 0, 0, 1);
        Unit target = TestSupport.unit(s, p, "grunt", Faction.RUSSIA, 4, 0, 2);
        
        TestSupport.setTerrain(s, 2, 0, Terrain.BUILDING);
        
        assertFalse(LineOfSight.hasLineOfSight(0, 0, 4, 0, s));
    }
}