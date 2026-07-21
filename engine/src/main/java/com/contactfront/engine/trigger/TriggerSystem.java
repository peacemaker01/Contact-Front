package com.contactfront.engine.trigger;

import com.contactfront.engine.Log;
import com.contactfront.engine.model.GameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerSystem {
    public final List<TriggerNode> triggers = new ArrayList<>();

    public void addNode(TriggerNode node) {
        triggers.add(node);
    }

    public void connect(String fromId, String toId) {
        // Connection established via TriggerNode.inputs/outputs lists
    }

    public void evaluateTriggers(GameState s) {
        for (TriggerNode node : triggers) {
            if (node.nodeType() == TriggerNode.NodeType.CONDITION) {
                boolean triggered = node.evaluate(s);
                if (triggered) {
                    Log.info("TriggerSystem: Condition node " + node.id() + " triggered");
                    for (TriggerNode.Connection conn : node.outputs()) {
                        triggers.stream()
                            .filter(t -> t.id().equals(conn.toId()))
                            .filter(t -> t.nodeType() == TriggerNode.NodeType.ACTION)
                            .forEach(t -> t.execute(s));
                    }
                }
            }
        }
    }

    public void clear() {
        triggers.clear();
    }
}