package com.contactfront.engine;

import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.*;
import com.contactfront.engine.rules.Combat;
import com.contactfront.engine.rules.Suppression;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class SuppressionStanceTest {

    @Test
    void stanceChangesIncomingSuppression() {
        GameState s = TestSupport.grid(4, 1, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit def = TestSupport.unit(s, p, "grunt", Faction.RUSSIA, 0, 0, 1);
        def.stance = Stance.DEFENSIVE;
        Unit agg = TestSupport.unit(s, p, "grunt", Faction.RUSSIA, 1, 0, 2);
        agg.stance = Stance.AGGRESSIVE;
        Suppression.applyIncoming(def, 10);
        Suppression.applyIncoming(agg, 10);
        assertTrue(agg.suppression > def.suppression, "aggressive should accrue more suppression");
        assertEquals(7.0, def.suppression, 0.001);
        assertEquals(13.0, agg.suppression, 0.001);
    }

    @Test
    void aggressiveOutgoingSuppressesMore() {
        GameState s = TestSupport.grid(6, 1, Terrain.OPEN);
        s.playerFaction = Faction.USA;
        s.enemyFaction = Faction.RUSSIA;
        Profiles p = TestSupport.customRoster();
        Unit aggressive = TestSupport.unit(s, p, "shooter", Faction.USA, 0, 0, 1);
        aggressive.stance = Stance.AGGRESSIVE;
        aggressive.baseAccuracy = -100;
        Unit defensive = TestSupport.unit(s, p, "shooter", Faction.USA, 0, 0, 2);
        defensive.stance = Stance.DEFENSIVE;
        defensive.baseAccuracy = -100;
        Unit t1 = TestSupport.unit(s, p, "grunt", Faction.RUSSIA, 1, 0, 3);
        Unit t2 = TestSupport.unit(s, p, "grunt", Faction.RUSSIA, 1, 0, 4);
        Combat.resolveFire(s, aggressive, t1, new Random(1));
        Combat.resolveFire(s, defensive, t2, new Random(1));
        assertTrue(t1.suppression > t2.suppression, "aggressive attacker suppresses more");
    }
}
