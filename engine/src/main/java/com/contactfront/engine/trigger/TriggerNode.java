package com.contactfront.engine.trigger;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;

import java.util.ArrayList;
import java.util.List;

public final class TriggerNode {
    public enum NodeType { CONDITION, ACTION }
    public enum ConditionType { 
        ENTER_AREA("Enter Area"),
        TIMER("Timer"),
        UNIT_DESTROYED("Unit Destroyed"),
        STRENGTH_THRESHOLD("Strength Threshold");

        public final String displayName;
        ConditionType(String displayName) { this.displayName = displayName; }
    }
    public enum ActionType {
        SPAWN_UNITS("Spawn Units"),
        ARTILLERY_STRIKE("Artillery Strike"),
        AMMO_RESUPPLY("Ammo Resupply"),
        MESSAGE("Display Message");

        public final String displayName;
        ActionType(String displayName) { this.displayName = displayName; }
    }

    private final String id;
    private final NodeType nodeType;
    private final int x, y;
    private final String description;
    private final List<Connection> inputs = new ArrayList<>();
    private final List<Connection> outputs = new ArrayList<>();

    private ConditionType conditionType;
    private ActionType actionType;
    private Object parameter;

    public TriggerNode(String id, NodeType type, int x, int y) {
        this(id, type, x, y, "");
    }

    public TriggerNode(String id, NodeType type, int x, int y, String description) {
        this.id = id;
        this.nodeType = type;
        this.x = x;
        this.y = y;
        this.description = description;
    }

    public String id() { return id; }
    public NodeType nodeType() { return nodeType; }
    public int x() { return x; }
    public int y() { return y; }
    public String description() { return description; }

    public ConditionType conditionType() { return conditionType; }
    public void setConditionType(ConditionType type) { this.conditionType = type; }

    public ActionType actionType() { return actionType; }
    public void setActionType(ActionType type) { this.actionType = type; }

    public Object parameter() { return parameter; }
    public void setParameter(Object parameter) { this.parameter = parameter; }

    public List<Connection> inputs() { return inputs; }
    public List<Connection> outputs() { return outputs; }

    public boolean evaluate(GameState s) {
        if (conditionType == null) return false;
        return switch (conditionType) {
            case ENTER_AREA -> checkEnterArea(s);
            case TIMER -> checkTimer(s);
            case UNIT_DESTROYED -> checkUnitDestroyed(s);
            case STRENGTH_THRESHOLD -> checkStrengthThreshold(s);
        };
    }

    private boolean checkEnterArea(GameState s) {
        if (parameter instanceof AreaTriggerParams params) {
            for (Unit u : s.friendlyUnits) {
                if (!u.destroyed && isPointInArea(u.x, u.y, params)) return true;
            }
            for (Unit u : s.enemyUnits) {
                if (!u.destroyed && isPointInArea(u.x, u.y, params)) return true;
            }
        }
        return false;
    }

    private boolean isPointInArea(int px, int py, AreaTriggerParams params) {
        return px >= params.minX && px <= params.maxX &&
               py >= params.minY && py <= params.maxY;
    }

    private boolean checkTimer(GameState s) {
        if (parameter instanceof Integer ticks) {
            return s.turn >= ticks;
        }
        return false;
    }

    private boolean checkUnitDestroyed(GameState s) {
        if (parameter instanceof Integer unitId) {
            return s.friendlyUnits.stream().noneMatch(u -> u.id == unitId && !u.destroyed) &&
                   s.enemyUnits.stream().noneMatch(u -> u.id == unitId && !u.destroyed);
        }
        return false;
    }

    private boolean checkStrengthThreshold(GameState s) {
        if (parameter instanceof StrengthTriggerParams params) {
            for (Unit u : s.friendlyUnits) {
                if (!u.destroyed && u.strength <= params.threshold) return true;
            }
            for (Unit u : s.enemyUnits) {
                if (!u.destroyed && u.strength <= params.threshold) return true;
            }
        }
        return false;
    }

    public void execute(GameState s) {
        if (actionType == null) return;
        switch (actionType) {
            case SPAWN_UNITS -> spawnUnits(s);
            case ARTILLERY_STRIKE -> artilleryStrike(s);
            case AMMO_RESUPPLY -> ammoResupply(s);
            case MESSAGE -> displayMessage(s);
        }
    }

    private void spawnUnits(GameState s) { /* TODO */ }
    private void artilleryStrike(GameState s) { /* TODO */ }
    private void ammoResupply(GameState s) { /* TODO */ }
    private void displayMessage(GameState s) { /* TODO */ }

    public record Connection(String fromId, String toId, String connectionType) {}
    public record AreaTriggerParams(int minX, int minY, int maxX, int maxY, String faction) {}
    public record StrengthTriggerParams(int unitId, double threshold) {}
}