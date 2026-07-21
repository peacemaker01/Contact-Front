package com.contactfront.engine.ai;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Unit;

import java.util.function.BiFunction;

public final class BehaviorTree {
    private BehaviorTree() {}

    public static final class Result {
        public final boolean success;
        public final boolean completed;
        public Result(boolean success, boolean completed) {
            this.success = success;
            this.completed = completed;
        }
    }

    public interface Node {
        Result execute(GameState s, Unit u);
    }

    public static Node sequence(Node... nodes) {
        return (s, u) -> {
            for (Node n : nodes) {
                Result r = n.execute(s, u);
                if (!r.success) return r;
            }
            return new Result(true, true);
        };
    }

    public static Node selector(Node... nodes) {
        return (s, u) -> {
            for (Node n : nodes) {
                Result r = n.execute(s, u);
                if (r.success) return r;
            }
            return new Result(false, true);
        };
    }

    public static Node condition(double threshold, BiFunction<GameState, Unit, Double> evaluator) {
        return (s, u) -> {
            Double value = evaluator.apply(s, u);
            return new Result(value >= threshold, false);
        };
    }

    public static Node action(Runnable action) {
        return (s, u) -> {
            action.run();
            return new Result(true, true);
        };
    }
}