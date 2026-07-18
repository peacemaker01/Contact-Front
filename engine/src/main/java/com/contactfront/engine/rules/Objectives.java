package com.contactfront.engine.rules;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Objective;
import com.contactfront.engine.model.Unit;

import java.util.HashMap;
import java.util.Map;

public final class Objectives {
    private Objectives() {}
    
    private static final Map<String, Integer> holdStartTurns = new HashMap<>();

    public static boolean check(GameState s) {
        boolean allRequiredCaptured = true;
        for (Objective o : s.objectives) {
            boolean captured = false;
            
            switch (o.type) {
                case "capture":
                    for (Unit u : s.friendlyUnits) {
                        if (!u.destroyed && u.x == o.x && u.y == o.y) {
                            captured = true;
                            break;
                        }
                    }
                    break;
                    
                case "hold":
                    for (Unit u : s.friendlyUnits) {
                        if (!u.destroyed && u.x == o.x && u.y == o.y) {
                            String key = o.id + "_" + u.id;
                            if (!holdStartTurns.containsKey(key)) {
                                holdStartTurns.put(key, s.turn);
                            }
                            int turnsHeld = s.turn - holdStartTurns.get(key);
                            if (turnsHeld >= o.requiredTurns) {
                                captured = true;
                            }
                            break;
                        }
                    }
                    if (!captured && holdStartTurns.containsKey(o.id + "_held")) {
                        holdStartTurns.remove(o.id + "_held");
                    }
                    break;
                    
                case "destroy":
                    if (o.targetUnitId != 0) {
                        Unit target = s.enemyById(o.targetUnitId);
                        captured = target == null || target.destroyed;
                    }
                    break;
                    
                default:
                    for (Unit u : s.friendlyUnits) {
                        if (!u.destroyed && u.x == o.x && u.y == o.y) {
                            captured = true;
                            break;
                        }
                    }
            }
            
            if (o.required && !captured) {
                allRequiredCaptured = false;
            }
        }
        
        if (allRequiredCaptured && !s.objectives.isEmpty()) {
            s.victory = true;
            s.gameOver = true;
            s.log("orders", "All objectives secured. Victory.");
            return true;
        }
        
        if (s.enemyUnits.stream().allMatch(u -> u.destroyed)) {
            s.victory = true; s.gameOver = true; s.log("orders", "All enemy forces eliminated. Victory.");
            return true;
        }
        if (s.friendlyUnits.stream().allMatch(u -> u.destroyed)) {
            s.victory = false; s.gameOver = true; s.log("orders", "Friendly forces wiped out. Defeat.");
            return true;
        }
        if (s.turn >= s.maxTurns) {
            s.victory = false; s.gameOver = true; s.log("orders", "Turn limit reached. Mission failed.");
            return true;
        }
        return false;
    }
}