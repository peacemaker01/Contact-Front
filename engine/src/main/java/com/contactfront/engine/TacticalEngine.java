package com.contactfront.engine;

import com.contactfront.engine.model.*;
import com.contactfront.engine.rules.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class TacticalEngine {
    private final GameState state;
    private final Random rng;

    public TacticalEngine(GameState state, Random rng) {
        this.state = state;
        this.rng = rng;
    }

    public GameState state() {
        return state;
    }

    public void start() {
        for (Unit u : state.friendlyUnits) if (!u.destroyed) u.movementPoints = u.movement;
        for (Unit u : state.enemyUnits) if (!u.destroyed) u.movementPoints = u.movement;
        com.contactfront.engine.rules.Visibility.computePlayerVisibility(state);
    }

    public ActionResult issue(Action action) {
        if (state.gameOver) return ActionResult.reject("game over");

        int uid = action.unitId();
        if (uid != 0) {
            Unit u = state.friendlyById(uid);
            if (u != null && !u.destroyed) {
                Unit commander = state.friendlyById(1);
                if (commander != null && !commander.destroyed && commander != u) {
                    int dist = Math.abs(u.x - commander.x) + Math.abs(u.y - commander.y);
                    if (dist > u.radioRange && u.orderDelayTurns == 0) {
                        u.orderDelayTurns = 1;
                        state.delayedOrders.add(new DelayedOrder(action, state.turn + 1, uid));
                        state.log("orders", u.profile.name() + " order delayed (radio range exceeded).");
                        return ActionResult.delayed();
                    }
                }
            }
        }

        processDelayedOrders();

        boolean executed = resolveAction(action, false);
        if (!executed) return ActionResult.reject("could not execute order");

        if (!state.gameOver) {
            AiTurn.run(state, rng);
            processDelayedOrders();
            if (state.turn % 5 == 0) Resupply.resupplyPhase(state);
            Suppression.tick(state);
            resetMovementPoints();
            com.contactfront.engine.rules.Visibility.computePlayerVisibility(state);
            Objectives.check(state);
            if (!state.gameOver) state.turn++;
        }
        return ActionResult.ok();
    }

    public void processDelayedOrders() {
        List<DelayedOrder> remaining = new ArrayList<>();
        for (DelayedOrder order : state.delayedOrders) {
            if (order.executionTurn <= state.turn) {
                if (order.command instanceof Action a) resolveAction(a, true);
                else if (order.command instanceof EnemyStrike es) applyEnemyStrike(es);
            } else {
                remaining.add(order);
            }
        }
        state.delayedOrders = remaining;
    }

    public void runEnemyTurn() {
        AiTurn.run(state, rng);
        processDelayedOrders();
        Suppression.tick(state);
        resetMovementPoints();
        com.contactfront.engine.rules.Visibility.computePlayerVisibility(state);
        Objectives.check(state);
    }

    /** Resolve a full player turn: apply every staged order, then the enemy turn. */
    public ActionResult resolvePlayerTurn(java.util.List<Action> actions) {
        if (state.gameOver) return ActionResult.reject("game over");
        processDelayedOrders();
        state.log("orders", "Player committed " + actions.size() + " order(s) — turn " + state.turn + ".");
        for (Action a : actions) {
            resolveAction(a, false);
            if (state.gameOver) break;
        }
        if (!state.gameOver) {
            AiTurn.run(state, rng);
            processDelayedOrders();
            if (state.turn % 5 == 0) Resupply.resupplyPhase(state);
            Suppression.tick(state);
            state.tickSmoke();
            for (Unit u : state.friendlyUnits) {
                if (!u.destroyed && u.stance == com.contactfront.engine.model.Stance.DEFENSIVE) {
                    u.entrenchment = Math.min(3, u.entrenchment + 1);
                }
            }
            for (Unit u : state.enemyUnits) {
                if (!u.destroyed && u.stance == com.contactfront.engine.model.Stance.DEFENSIVE) {
                    u.entrenchment = Math.min(3, u.entrenchment + 1);
                }
            }
            resetMovementPoints();
            com.contactfront.engine.rules.Visibility.computePlayerVisibility(state);
            Objectives.check(state);
            if (!state.gameOver) state.turn++;
        }
        return ActionResult.ok();
    }

    public boolean resolveAction(Action action, boolean immediate) {
        if (action instanceof MoveAction a) {
            Unit u = state.friendlyById(a.unitId());
            if (u == null || u.destroyed) return false;
            return Movement.applyMove(state, u, a.targetX(), a.targetY());
        }
        if (action instanceof AttackAction a) {
            Unit u = state.friendlyById(a.unitId());
            Unit target = state.enemyById(a.targetUnitId());
            if (u == null || u.destroyed || target == null || target.destroyed) return false;
            if (u.hasSpecial("kamikaze")) {
                FpvAttack.strike(state, u, target, rng);
                return true;
            }
            return Combat.resolveFire(state, u, target, rng).executed();
        }
        if (action instanceof ReconAction a) {
            Unit u = state.friendlyById(a.unitId());
            if (u == null || u.destroyed) return false;
            Recon.reveal(state, u, a.radius());
            return true;
        }
        if (action instanceof ResupplyAction a) {
            Unit u = state.friendlyById(a.unitId());
            if (u == null || u.destroyed) return false;
            return Resupply.requestResupply(state, u);
        }
        if (action instanceof CallCasAction a) {
            if (immediate) {
                CloseAirSupport.applyCas(state, a.targetX(), a.targetY(), rng);
                return true;
            }
            if (state.casAvailable <= 0) return false;
            state.casAvailable--;
            state.delayedOrders.add(new DelayedOrder(a, state.turn + 1, 0));
            state.log("orders", "CAS requested on (" + a.targetX() + "," + a.targetY() + ").");
            return true;
        }
        if (action instanceof CallArtilleryAction a) {
            if (immediate) {
                Artillery.resolve(state, a.targetX(), a.targetY(), 3, rng, state.ewGpsJammed ? 100 : 50);
                return true;
            }
            if (state.artilleryFiresRemaining <= 0) return false;
            state.artilleryFiresRemaining--;
            state.delayedOrders.add(new DelayedOrder(a, state.turn + 1, 0));
            state.log("orders", "Artillery strike requested on (" + a.targetX() + "," + a.targetY() + ").");
            return true;
        }
        if (action instanceof CallSmokeAction a) {
            if (immediate) {
                for (int sy = a.targetY() - 1; sy <= a.targetY() + 1; sy++) {
                    for (int sx = a.targetX() - 1; sx <= a.targetX() + 1; sx++) {
                        state.addSmoke(sx, sy, 3);
                    }
                }
                state.log("orders", "Smoke deployed at (" + a.targetX() + "," + a.targetY() + ").");
                return true;
            }
            if (state.smokeGrenades <= 0) return false;
            state.smokeGrenades--;
            state.delayedOrders.add(new DelayedOrder(a, state.turn + 1, 0));
            state.log("orders", "Smoke requested on (" + a.targetX() + "," + a.targetY() + ").");
            return true;
        }
        if (action instanceof SetStanceAction a) {
            Unit u = state.friendlyById(a.unitId());
            if (u == null || u.destroyed) return false;
            u.stance = a.stance();
            return true;
        }
        return false;
    }

    private void applyEnemyStrike(EnemyStrike es) {
        if (EnemyStrike.ARTY.equals(es.kind())) {
            int cep = state.ewGpsJammed ? 100 : 50;
            Artillery.resolve(state, es.x(), es.y(), es.rounds(), rng, cep);
        } else if (EnemyStrike.CAS.equals(es.kind())) {
            CloseAirSupport.applyCas(state, es.x(), es.y(), rng);
        }
    }

    private void resetMovementPoints() {
        for (Unit u : state.friendlyUnits) if (!u.destroyed) u.movementPoints = u.movement;
        for (Unit u : state.enemyUnits) if (!u.destroyed) u.movementPoints = u.movement;
    }
}
