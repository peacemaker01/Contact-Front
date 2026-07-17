package com.contactfront.engine;

import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.Faction;
import com.contactfront.engine.terrain.ScenarioGenerator;
import com.contactfront.engine.terrain.ScenarioGenerator.Generated;
import com.contactfront.engine.terrain.ScenarioGenerator.ScenarioSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScenarioGenerationTest {

    private ScenarioSpec spec() {
        return new ScenarioSpec(12345L, 28, 20, Faction.USA, Faction.RUSSIA,
                22, 22, List.of("aa_team"),
                List.of("river_crossing", "settlement"), 60);
    }

    @Test
    void sameSeedProducesIdenticalScenario() {
        Profiles p = Profiles.load();
        Generated g1 = ScenarioGenerator.generate(spec(), p);
        Generated g2 = ScenarioGenerator.generate(spec(), p);
        assertEquals(g1.effectiveSeed(), g2.effectiveSeed());

        int h = g1.state().height(), w = g1.state().width();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                assertEquals(g1.state().grid[y][x].type, g2.state().grid[y][x].type,
                        "terrain must match at " + x + "," + y);

        assertEquals(g1.state().friendlyUnits.size(), g2.state().friendlyUnits.size());
        for (int i = 0; i < g1.state().friendlyUnits.size(); i++) {
            var a = g1.state().friendlyUnits.get(i);
            var b = g2.state().friendlyUnits.get(i);
            assertEquals(a.profile.id(), b.profile.id());
            assertEquals(a.x, b.x);
            assertEquals(a.y, b.y);
        }
        assertEquals(g1.state().objectives.get(0).x, g2.state().objectives.get(0).x);
    }

    @Test
    void satisfiesConstraintsAndRequiredForces() {
        Profiles p = Profiles.load();
        Generated g = ScenarioGenerator.generate(spec(), p);
        boolean hasRiver = false, hasSettlement = false;
        for (var row : g.state().grid)
            for (var t : row) {
                if (t.type == com.contactfront.engine.model.Terrain.FORD) hasRiver = true;
                if (t.type == com.contactfront.engine.model.Terrain.BUILDING) hasSettlement = true;
            }
        assertTrue(hasRiver, "must contain a river crossing");
        assertTrue(hasSettlement, "must contain a settlement");
        assertTrue(g.state().enemyUnits.stream().anyMatch(u -> u.profile.id().equals("aa_team")),
                "defender force must include an AA unit; got=" + g.state().enemyUnits.stream().map(u -> u.profile.id()).toList());
        assertFalse(g.state().objectives.isEmpty());
    }
}
