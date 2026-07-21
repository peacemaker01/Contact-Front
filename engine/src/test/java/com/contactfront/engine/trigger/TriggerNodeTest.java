package com.contactfront.engine.trigger;

import com.contactfront.engine.model.Faction;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.UnitProfile;
import com.contactfront.engine.data.Profiles;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TriggerNodeTest {

    @Test
    void nodeCreation() {
        TriggerNode node = new TriggerNode("test1", TriggerNode.NodeType.CONDITION, 10, 20);
        assertEquals("test1", node.id());
        assertEquals(TriggerNode.NodeType.CONDITION, node.nodeType());
        assertEquals(10, node.x());
        assertEquals(20, node.y());
        assertEquals("", node.description());
    }

    @Test
    void nodeCreationWithDescription() {
        TriggerNode node = new TriggerNode("test2", TriggerNode.NodeType.ACTION, 5, 15, "Spawn reinforcements");
        assertEquals("test2", node.id());
        assertEquals(TriggerNode.NodeType.ACTION, node.nodeType());
        assertEquals("Spawn reinforcements", node.description());
    }

    @Test
    void conditionTypeCanBeSet() {
        TriggerNode node = new TriggerNode("c1", TriggerNode.NodeType.CONDITION, 0, 0);
        assertNull(node.conditionType());
        node.setConditionType(TriggerNode.ConditionType.TIMER);
        assertEquals(TriggerNode.ConditionType.TIMER, node.conditionType());
    }

    @Test
    void actionTypeCanBeSet() {
        TriggerNode node = new TriggerNode("a1", TriggerNode.NodeType.ACTION, 0, 0);
        assertNull(node.actionType());
        node.setActionType(TriggerNode.ActionType.SPAWN_UNITS);
        assertEquals(TriggerNode.ActionType.SPAWN_UNITS, node.actionType());
    }

    @Test
    void conditionNodeEvaluatesFalseWhenNoConditionType() {
        TriggerNode node = new TriggerNode("c2", TriggerNode.NodeType.CONDITION, 0, 0);
        GameState state = new GameState();
        assertFalse(node.evaluate(state));
    }

    @Test
    void timerConditionEvaluatesTrueWhenTurnReached() {
        TriggerNode node = new TriggerNode("timer", TriggerNode.NodeType.CONDITION, 0, 0);
        node.setConditionType(TriggerNode.ConditionType.TIMER);
        node.setParameter(5);
        GameState state = new GameState();
        state.turn = 5;
        assertTrue(node.evaluate(state));
    }

    @Test
    void timerConditionEvaluatesFalseBeforeTurn() {
        TriggerNode node = new TriggerNode("timer", TriggerNode.NodeType.CONDITION, 0, 0);
        node.setConditionType(TriggerNode.ConditionType.TIMER);
        node.setParameter(10);
        GameState state = new GameState();
        state.turn = 5;
        assertFalse(node.evaluate(state));
    }

    @Test
    void strengthThresholdConditionEvaluatesTrue() {
        TriggerNode node = new TriggerNode("strength", TriggerNode.NodeType.CONDITION, 0, 0);
        node.setConditionType(TriggerNode.ConditionType.STRENGTH_THRESHOLD);
        node.setParameter(new TriggerNode.StrengthTriggerParams(1, 50.0));
        GameState state = new GameState();
        Profiles profiles = Profiles.load();
        UnitProfile profile = profiles.unit("inf_squad");
        Unit unit = new Unit(1, Faction.USA, profile, 0, 0, profiles);
        unit.strength = 30.0;
        state.friendlyUnits.add(unit);
        assertTrue(node.evaluate(state));
    }

    @Test
    void strengthThresholdConditionEvaluatesFalse() {
        TriggerNode node = new TriggerNode("strength", TriggerNode.NodeType.CONDITION, 0, 0);
        node.setConditionType(TriggerNode.ConditionType.STRENGTH_THRESHOLD);
        node.setParameter(new TriggerNode.StrengthTriggerParams(1, 50.0));
        GameState state = new GameState();
        Profiles profiles = Profiles.load();
        UnitProfile profile = profiles.unit("inf_squad");
        Unit unit = new Unit(1, Faction.USA, profile, 0, 0, profiles);
        unit.strength = 80.0;
        state.friendlyUnits.add(unit);
        assertFalse(node.evaluate(state));
    }

    @Test
    void enterAreaConditionEvaluatesTrue() {
        TriggerNode node = new TriggerNode("area", TriggerNode.NodeType.CONDITION, 0, 0);
        node.setConditionType(TriggerNode.ConditionType.ENTER_AREA);
        node.setParameter(new TriggerNode.AreaTriggerParams(5, 5, 10, 10, "USA"));
        GameState state = new GameState();
        Profiles profiles = Profiles.load();
        UnitProfile profile = profiles.unit("inf_squad");
        Unit unit = new Unit(1, Faction.USA, profile, 7, 7, profiles);
        state.friendlyUnits.add(unit);
        assertTrue(node.evaluate(state));
    }

    @Test
    void enterAreaConditionEvaluatesFalse() {
        TriggerNode node = new TriggerNode("area", TriggerNode.NodeType.CONDITION, 0, 0);
        node.setConditionType(TriggerNode.ConditionType.ENTER_AREA);
        node.setParameter(new TriggerNode.AreaTriggerParams(5, 5, 10, 10, "USA"));
        GameState state = new GameState();
        Profiles profiles = Profiles.load();
        UnitProfile profile = profiles.unit("inf_squad");
        Unit unit = new Unit(1, Faction.USA, profile, 2, 2, profiles);
        state.friendlyUnits.add(unit);
        assertFalse(node.evaluate(state));
    }

    @Test
    void inputsAndOutputsAreEmptyByDefault() {
        TriggerNode node = new TriggerNode("n1", TriggerNode.NodeType.CONDITION, 0, 0);
        assertTrue(node.inputs().isEmpty());
        assertTrue(node.outputs().isEmpty());
    }

    @Test
    void allConditionTypesExist() {
        TriggerNode.ConditionType[] types = TriggerNode.ConditionType.values();
        assertEquals(4, types.length);
        assertNotNull(TriggerNode.ConditionType.valueOf("ENTER_AREA"));
        assertNotNull(TriggerNode.ConditionType.valueOf("TIMER"));
        assertNotNull(TriggerNode.ConditionType.valueOf("UNIT_DESTROYED"));
        assertNotNull(TriggerNode.ConditionType.valueOf("STRENGTH_THRESHOLD"));
    }

    @Test
    void allActionTypesExist() {
        TriggerNode.ActionType[] types = TriggerNode.ActionType.values();
        assertEquals(4, types.length);
        assertNotNull(TriggerNode.ActionType.valueOf("SPAWN_UNITS"));
        assertNotNull(TriggerNode.ActionType.valueOf("ARTILLERY_STRIKE"));
        assertNotNull(TriggerNode.ActionType.valueOf("AMMO_RESUPPLY"));
        assertNotNull(TriggerNode.ActionType.valueOf("MESSAGE"));
    }

    @Test
    void connectionRecord() {
        TriggerNode.Connection conn = new TriggerNode.Connection("from1", "to1", "default");
        assertEquals("from1", conn.fromId());
        assertEquals("to1", conn.toId());
        assertEquals("default", conn.connectionType());
    }
}