package com.contactfront.engine;

import com.contactfront.engine.data.FactionRegistry;
import com.contactfront.engine.model.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FactionRegistryTest {

    @Test
    void usaLoadsCentralizedNetwork() {
        FactionBlueprint bp = FactionRegistry.getBlueprint(Faction.USA);
        assertEquals(Faction.USA, bp.faction());
        assertEquals(NetworkTopology.Type_A, bp.networkTopology());
        assertEquals(SensorEmission.Active_RF, bp.sensorEmission());
        assertEquals(DamageModel.Bustle_Protected, bp.damageModel());
        assertEquals(DroneInterface.Direct_PiP, bp.droneInterface());
    }

    @Test
    void russiaLoadsWithHighCatastrophic() {
        FactionBlueprint bp = FactionRegistry.getBlueprint(Faction.RUSSIA);
        assertEquals(NetworkTopology.Type_A, bp.networkTopology());
        assertEquals(DamageModel.Hull_Carousel, bp.damageModel());
        assertEquals(DroneInterface.Recon_Linked, bp.droneInterface());
    }

    @Test
    void chinaLoadsHierarchical() {
        FactionBlueprint bp = FactionRegistry.getBlueprint(Faction.CHINA);
        assertEquals(NetworkTopology.Type_B, bp.networkTopology());
        assertEquals(SensorEmission.Passive_EO_IR, bp.sensorEmission());
        assertEquals(DamageModel.Hull_Carousel, bp.damageModel());
        assertEquals(DroneInterface.Waypoint_Saturation, bp.droneInterface());
    }

    @Test
    void iranLoadsDecentralized() {
        FactionBlueprint bp = FactionRegistry.getBlueprint(Faction.IRAN);
        assertEquals(NetworkTopology.Type_C, bp.networkTopology());
        assertEquals(SensorEmission.Passive_EO_IR, bp.sensorEmission());
        assertEquals(DroneInterface.Waypoint_Saturation, bp.droneInterface());
    }

    @Test
    void unitReceivesFactionComponents() {
        com.contactfront.engine.data.Profiles p = TestSupport.customRoster();
        GameState s = TestSupport.grid(10, 10, com.contactfront.engine.model.Terrain.OPEN);
        s.playerFaction = Faction.USA;
        Unit u = new Unit(1, Faction.USA, p.unit("tank"), 5, 5, p);

        assertEquals(NetworkTopology.Type_A, u.networkTopology);
        assertEquals(SensorEmission.Active_RF, u.sensorEmission);
        assertEquals(DamageModel.Bustle_Protected, u.damageModel);
    }
}