package com.contactfront.engine;

import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.Faction;
import com.contactfront.engine.model.Stance;
import com.contactfront.engine.rules.Movement;
import com.contactfront.engine.terrain.ScenarioGenerator;
import com.contactfront.engine.terrain.ScenarioGenerator.ScenarioSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class EngineIntegrationTest {

    @Test
    void fullGeneratedBattleRunsToCompletion() {
        Profiles p = Profiles.load();
        ScenarioSpec spec = new ScenarioSpec(777L, 28, 20, Faction.USA, Faction.RUSSIA,
                22, 22, List.of(), List.of("river_crossing"), 60);
        var gen = ScenarioGenerator.generate(spec, p);
        TacticalEngine engine = new TacticalEngine(gen.state(), new Random(777L));
        engine.start();

        int guard = 0;
        while (!gen.state().gameOver && guard++ < 120) {
            engine.tick();
        }
        assertTrue(gen.state().gameOver || gen.state().elapsedMs >= 60000, "battle should reach a terminal state or time limit");
    }
}
