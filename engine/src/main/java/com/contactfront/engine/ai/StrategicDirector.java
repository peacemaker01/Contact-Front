package com.contactfront.engine.ai;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.UnitCategory;
import com.contactfront.engine.model.Faction;

import java.util.ArrayList;
import java.util.List;

public final class StrategicDirector {
    private StrategicDirector() {}

    private static final int EVAL_INTERVAL = 5;

    public static List<StrategicTask> evaluate(GameState s) {
        List<StrategicTask> tasks = new ArrayList<>();
        double usStrength = evaluateFactionStrength(s, Faction.USA);
        double enemyStrength = evaluateFactionStrength(s, s.enemyFaction);

        for (Unit u : s.enemyUnits) {
            if (u.destroyed) continue;
            Unit nearest = findNearestThreat(u, s.friendlyUnits);
            if (nearest != null) {
                tasks.add(new StrategicTask(u, nearest.x, nearest.y, TaskPriority.HIGH));
            }
        }

        return tasks;
    }

    private static double evaluateFactionStrength(GameState s, Faction f) {
        double total = 0.0;
        List<Unit> units = f == s.playerFaction ? s.friendlyUnits : s.enemyUnits;
        for (Unit u : units) {
            if (u.destroyed) continue;
            double weight = switch (u.profile.category()) {
                case ARMOR -> 1.0;
                case INFANTRY -> 0.3;
                case ARTILLERY, AIR_DEFENSE -> 0.8;
                default -> 0.5;
            };
            total += weight * (u.strength / 100.0);
        }
        return total;
    }

    private static Unit findNearestThreat(Unit from, List<Unit> candidates) {
        Unit nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Unit u : candidates) {
            if (u.destroyed) continue;
            double dist = Math.abs(u.x - from.x) + Math.abs(u.y - from.y);
            if (dist < minDist) {
                minDist = dist;
                nearest = u;
            }
        }
        return nearest;
    }

    public record StrategicTask(Unit assignee, int targetX, int targetY, TaskPriority priority) {}
    public enum TaskPriority { LOW, MEDIUM, HIGH, CRITICAL }
}