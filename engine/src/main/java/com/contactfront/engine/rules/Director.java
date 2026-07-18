package com.contactfront.engine.rules;

import com.contactfront.engine.Log;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Faction;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.Doctrine;

import java.util.List;

public final class Director {
    private Director() {}

    private static int lastCasCheckTurn = 0;
    private static int lastArtilleryCheckTurn = 0;

    public static void evaluate(GameState s, int currentTurn) {
        Log.info("Director.evaluate: turn " + currentTurn + " (player=" + s.friendlyUnits.size() + ", enemy=" + s.enemyUnits.size() + ")");
        if (currentTurn - lastCasCheckTurn >= 60) {
            lastCasCheckTurn = currentTurn;
            maybeCallCas(s);
        }

        if (currentTurn - lastArtilleryCheckTurn >= 90) {
            lastArtilleryCheckTurn = currentTurn;
            maybeCallArtillery(s);
        }
    }

    private static void maybeCallCas(GameState s) {
        int friendlyStrength = s.friendlyUnits.stream()
            .filter(u -> !u.destroyed)
            .mapToInt(u -> (int)u.strength)
            .sum();
        int enemyStrength = s.enemyUnits.stream()
            .filter(u -> !u.destroyed)
            .mapToInt(u -> (int)u.strength)
            .sum();

        double ratio = (double) friendlyStrength / Math.max(1, enemyStrength);
        Log.info("Director.maybeCallCas: ratio=" + String.format("%.2f", ratio) + " cas=" + s.enemyCasAvailable);
        if (ratio < 0.7 && s.enemyCasAvailable > 0) {
            double avgX = s.enemyUnits.stream()
                .filter(u -> !u.destroyed)
                .mapToInt(u -> u.x)
                .average()
                .orElse(s.width() / 2.0);
            double avgY = s.enemyUnits.stream()
                .filter(u -> !u.destroyed)
                .mapToInt(u -> u.y)
                .average()
                .orElse(s.height() / 2.0);
            int foeX = (int) Math.round(avgX);
            int foeY = (int) Math.round(avgY);

            s.delayedOrders.add(new com.contactfront.engine.model.DelayedOrder(
                new com.contactfront.engine.model.CallCasAction(0, foeX, foeY),
                s.turn + 2,
                0
            ));
            s.enemyCasAvailable--;
            s.log("orders", "Director calls for CAS on enemy cluster.");
            Log.info("Director.maybeCallCas: CAS scheduled on (" + foeX + "," + foeY + ")");
        }
    }

    private static void maybeCallArtillery(GameState s) {
        int friendlyStrength = s.friendlyUnits.stream()
            .filter(u -> !u.destroyed)
            .mapToInt(u -> (int)u.strength)
            .sum();
        int enemyStrength = s.enemyUnits.stream()
            .filter(u -> !u.destroyed)
            .mapToInt(u -> (int)u.strength)
            .sum();

        double ratio = (double) friendlyStrength / Math.max(1, enemyStrength);
        Log.info("Director.maybeCallArtillery: ratio=" + String.format("%.2f", ratio) + " arty=" + s.enemyArtilleryFiresRemaining);
        if (ratio < 0.5 && s.enemyArtilleryFiresRemaining > 0) {
            double avgX = s.enemyUnits.stream()
                .filter(u -> !u.destroyed)
                .mapToInt(u -> u.x)
                .average()
                .orElse(s.width() / 2.0);
            double avgY = s.enemyUnits.stream()
                .filter(u -> !u.destroyed)
                .mapToInt(u -> u.y)
                .average()
                .orElse(s.height() / 2.0);
            int foeX = (int) Math.round(avgX);
            int foeY = (int) Math.round(avgY);

            s.delayedOrders.add(new com.contactfront.engine.model.DelayedOrder(
                new com.contactfront.engine.model.CallArtilleryAction(0, foeX, foeY),
                s.turn + 2,
                0
            ));
            s.enemyArtilleryFiresRemaining--;
            s.log("orders", "Director calls for artillery on enemy cluster.");
            Log.info("Director.maybeCallArtillery: Artillery scheduled on (" + foeX + "," + foeY + ")");
        }
    }

    /** Utility-based action scoring for unit-level AI decisions. */
    public static record ActionScore(String action, double weight) {}

    public static List<ActionScore> scoreUnitActions(GameState s, Unit u) {
        List<ActionScore> scores = new java.util.ArrayList<>();

        // Find nearest known enemy
        Unit nearest = findNearestKnownEnemy(s, u);
        if (nearest == null) {
            // No visible targets - consider moving to objective or seeking cover
            scores.add(new ActionScore("hold_position", 10.0));
            return scores;
        }

        int dist = Math.abs(u.x - nearest.x) + Math.abs(u.y - nearest.y);

        // Engage if in range and weapon available
        boolean canEngage = u.weapons.stream()
            .anyMatch(w -> w.ammo > 0 && w.profile.range() >= dist);
        if (canEngage) {
            double score = 20.0 - dist * 0.5;
            // Suppression affects decision making
            if (u.isSuppressed()) score *= 0.3;
            scores.add(new ActionScore("engage", score));
        }

        // Seek cover if suppressed
        if (u.isSuppressed() || u.strength < 50) {
            scores.add(new ActionScore("seek_cover", 15.0));
        }

        // Flank if doctrine allows and enemy not too close
        if (s.factionDoctrines.getOrDefault(u.faction, Doctrine.NATO).shouldFlank(u) && dist > 3) {
            scores.add(new ActionScore("flank", 12.0));
        }

        // Retreat if severely damaged
        if (u.strength < 30) {
            scores.add(new ActionScore("retreat", 18.0));
        }

        // Call for fire support if available
        if (s.artilleryFiresRemaining > 0 && dist < 8) {
            scores.add(new ActionScore("call_arty", 8.0));
        }

        return scores;
    }

    private static Unit findNearestKnownEnemy(GameState s, Unit from) {
        // Find enemies that are known to the player (or to the opposing force)
        if (from.faction == s.playerFaction) {
            return s.enemyUnits.stream()
                .filter(e -> !e.destroyed)
                .min((a, b) -> Integer.compare(
                    Math.abs(a.x - from.x) + Math.abs(a.y - from.y),
                    Math.abs(b.x - from.x) + Math.abs(b.y - from.y)))
                .orElse(null);
        } else {
            return s.friendlyUnits.stream()
                .filter(e -> !e.destroyed)
                .min((a, b) -> Integer.compare(
                    Math.abs(a.x - from.x) + Math.abs(a.y - from.y),
                    Math.abs(b.x - from.x) + Math.abs(b.y - from.y)))
                .orElse(null);
        }
    }
}