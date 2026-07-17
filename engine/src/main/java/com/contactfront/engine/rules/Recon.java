package com.contactfront.engine.rules;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;

public final class Recon {
    private Recon() {}

    public static int reveal(GameState s, Unit u, int radius) {
        int revealed = 0;
        for (Unit e : s.enemyUnits) {
            if (e.destroyed) continue;
            int d = Math.abs(u.x - e.x) + Math.abs(u.y - e.y);
            if (d <= radius && LineOfSight.hasLineOfSight(u.x, u.y, e.x, e.y, s)) {
                e.knownToPlayer = true;
                e.lastKnownX = e.x;
                e.lastKnownY = e.y;
                e.lastSeenTurn = s.turn;
                revealed++;
            }
        }
        s.log("intel", u.profile.name() + " recon sweep revealed " + revealed + " contact(s).");
        return revealed;
    }
}
