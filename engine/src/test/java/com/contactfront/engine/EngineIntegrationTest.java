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
        while (!gen.state().gameOver && guard++ < 60) {
            var u = gen.state().friendlyUnits.stream().filter(x -> !x.destroyed).findFirst();
            if (u.isEmpty()) break;
            var reach = Movement.reachable(gen.state(), u.get());
            if (!reach.isEmpty()) {
                int[] t = reach.get(0);
                engine.issue(new com.contactfront.engine.model.MoveAction(u.get().id, t[0], t[1]));
            } else {
                engine.issue(new com.contactfront.engine.model.SetStanceAction(u.get().id, Stance.OVERWATCH));
            }
        }
        assertTrue(gen.state().gameOver, "battle should reach a terminal state");
        assertTrue(gen.state().turn >= 1);
    }
}
