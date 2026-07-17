package com.contactfront.engine.rules;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;

public final class Suppression {
    private Suppression() {}

    public static final double DECAY_PER_TURN = 15.0;

    public static void applyIncoming(Unit target, double baseValue) {
        target.underFireThisTurn = true;
        target.suppression = Math.min(100.0, target.suppression + baseValue * target.stance.incomingSuppressionMult);
    }

    public static void tick(GameState s) {
        for (Unit u : s.friendlyUnits) decay(s, u);
        for (Unit u : s.enemyUnits) decay(s, u);
    }

    public static void decay(GameState s, Unit u) {
        if (u.destroyed) return;
        if (!u.underFireThisTurn) {
            double decayAmount = DECAY_PER_TURN;
            if (s.isRaining) decayAmount *= 0.7;
            u.suppression = Math.max(0.0, u.suppression - decayAmount);
        }
        u.underFireThisTurn = false;
        if (u.suppression > 0) {
            u.turnsSuppressed++;
            if (u.faction == s.playerFaction) s.turnsUnderSuppression++;
        }
        boolean suppressedNow = u.isSuppressed();
        if (suppressedNow && !u.wasSuppressed) {
            s.log("combat", u.profile.name() + " is suppressed (" + (int) u.suppression + "%).");
        }
        u.wasSuppressed = suppressedNow;
    }
}
