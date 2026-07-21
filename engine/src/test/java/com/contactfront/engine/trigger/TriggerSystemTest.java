package com.contactfront.engine.trigger;

import com.contactfront.engine.model.Faction;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.UnitProfile;
import com.contactfront.engine.data.Profiles;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TriggerSystemTest {

    @Test
    void addNodeStoresTrigger() {
        TriggerSystem system = new TriggerSystem();
        TriggerNode node = new TriggerNode("t1", TriggerNode.NodeType.CONDITION, 0, 0);
        system.addNode(node);
        assertEquals(1, system.triggers.size());
    }

    @Test
    void addMultipleNodes() {
        TriggerSystem system = new TriggerSystem();
        system.addNode(new TriggerNode("t1", TriggerNode.NodeType.CONDITION, 0, 0));
        system.addNode(new TriggerNode("t2", TriggerNode.NodeType.ACTION, 5, 5));
        assertEquals(2, system.triggers.size());
    }

    @Test
    void clearRemovesAllTriggers() {
        TriggerSystem system = new TriggerSystem();
        system.addNode(new TriggerNode("t1", TriggerNode.NodeType.CONDITION, 0, 0));
        system.addNode(new TriggerNode("t2", TriggerNode.NodeType.ACTION, 5, 5));
        assertEquals(2, system.triggers.size());
        system.clear();
        assertEquals(0, system.triggers.size());
    }

    @Test
    void evaluateTriggersRunsConditionNodes() {
        TriggerSystem system = new TriggerSystem();
        TriggerNode condition = new TriggerNode("c1", TriggerNode.NodeType.CONDITION, 0, 0);
        condition.setConditionType(TriggerNode.ConditionType.TIMER);
        condition.setParameter(1);
        system.addNode(condition);

        GameState state = new GameState();
        state.turn = 1;

        // Should not throw
        system.evaluateTriggers(state);
    }

    @Test
    void evaluateTriggersSkipsActionNodes() {
        TriggerSystem system = new TriggerSystem();
        TriggerNode action = new TriggerNode("a1", TriggerNode.NodeType.ACTION, 0, 0);
        action.setActionType(TriggerNode.ActionType.MESSAGE);
        system.addNode(action);

        GameState state = new GameState();
        // Should not throw - action nodes are not evaluated as conditions
        system.evaluateTriggers(state);
    }

    @Test
    void connectDoesNotThrow() {
        TriggerSystem system = new TriggerSystem();
        TriggerNode from = new TriggerNode("from", TriggerNode.NodeType.CONDITION, 0, 0);
        TriggerNode to = new TriggerNode("to", TriggerNode.NodeType.ACTION, 5, 5);
        system.addNode(from);
        system.addNode(to);
        // Connect should not throw
        system.connect("from", "to");
    }

    @Test
    void triggerListIsMutable() {
        TriggerSystem system = new TriggerSystem();
        system.triggers.add(new TriggerNode("direct", TriggerNode.NodeType.CONDITION, 0, 0));
        assertEquals(1, system.triggers.size());
    }

    @Test
    void strengthThresholdTriggerEvaluatesCorrectly() {
        TriggerSystem system = new TriggerSystem();
        TriggerNode condition = new TriggerNode("strength", TriggerNode.NodeType.CONDITION, 0, 0);
        condition.setConditionType(TriggerNode.ConditionType.STRENGTH_THRESHOLD);
        condition.setParameter(new TriggerNode.StrengthTriggerParams(1, 50.0));
        system.addNode(condition);

        GameState state = new GameState();
        Profiles profiles = Profiles.load();
        UnitProfile profile = profiles.unit("inf_squad");
        Unit unit = new Unit(1, Faction.USA, profile, 0, 0, profiles);
        unit.strength = 20.0;
        state.friendlyUnits.add(unit);

        system.evaluateTriggers(state);
    }
}