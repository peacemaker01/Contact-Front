package com.contactfront.engine.ai;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Tile;
import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.UnitCategory;
import com.contactfront.engine.ai.StrategicDirector.StrategicTask;

import java.util.List;
import java.util.ArrayList;

public final class TacticalPlanner {
    private TacticalPlanner() {}

    private static final int PLANNING_RANGE = 20;

    public static void planPaths(GameState s, List<StrategicTask> tasks) {
        for (StrategicTask task : tasks) {
            Unit u = task.assignee();
            if (u.destroyed) continue;
            if (u.stepsRemaining > 0) continue;
            planAndExecute(s, u, task.targetX(), task.targetY());
        }
    }

    private static void planAndExecute(GameState s, Unit u, int tx, int ty) {
        int dist = Math.abs(tx - u.x) + Math.abs(ty - u.y);
        if (dist > PLANNING_RANGE) return;
        if (u.movement <= 0) return;

        com.contactfront.engine.rules.Movement.startMove(s, u, tx, ty);
    }

    public static void updateThreatAwareness(GameState s, InfluenceMap map) {
        for (Unit u : s.enemyUnits) {
            if (u.destroyed || u.stepsRemaining > 0) continue;
            double threat = map.threatAt(u.x, u.y);
            if (threat > 0.5 && u.movement > 0) {
                withdrawFromThreat(s, u);
            }
        }
    }

    private static void withdrawFromThreat(GameState s, Unit u) {
        int bestX = u.x, bestY = u.y;
        double minThreat = Double.MAX_VALUE;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int nx = u.x + dx, ny = u.y + dy;
                if (nx < 0 || ny < 0 || nx >= s.width() || ny >= s.height()) continue;
                Tile t = s.grid[ny][nx];
                if (t == null || t.impassable()) continue;
                double threat = ThreatEvaluator.staticThreatAtPoint(s, nx, ny);
                if (threat < minThreat) {
                    minThreat = threat;
                    bestX = nx;
                    bestY = ny;
                }
            }
        }
        if (minThreat < ThreatEvaluator.staticThreatAtPoint(s, u.x, u.y)) {
            com.contactfront.engine.rules.Movement.startMove(s, u, bestX, bestY);
        }
    }
}