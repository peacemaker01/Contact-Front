package com.contactfront.ui.controller;

import com.contactfront.engine.TacticalEngine;
import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.*;
import com.contactfront.engine.rules.Movement;
import com.contactfront.engine.terrain.ScenarioGenerator;
import com.contactfront.engine.terrain.ScenarioGenerator.ScenarioSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class GameController {
    public enum Mode { NONE, MOVE, ATTACK, CAS, SMOKE, ARTY }

    public GameState state;
    public TacticalEngine engine;
    public Profiles profiles;
    public Unit selected;
    public Mode mode = Mode.NONE;
    public final List<Action> staged = new ArrayList<>();
    public final Set<Integer> stagedUnits = new HashSet<>();
    public final List<Unit> selection = new ArrayList<>();
    public final Map<Integer, List<Integer>> groups = new HashMap<>();
    public long seed;
    public Runnable onUpdate;
    public Faction playerFaction = Faction.USA;
    public Faction enemyFaction = Faction.RUSSIA;

    private void syncSelected() { selected = selection.isEmpty() ? null : selection.get(0); }

    public void setPlayerFaction(Faction faction) {
        this.playerFaction = faction;
    }

    public void newGame(long seed) {
        this.seed = seed;
        profiles = Profiles.load();
        ScenarioSpec spec = new ScenarioSpec(seed, 28, 20, playerFaction, enemyFaction,
                22, 22, java.util.List.of("aa_team"),
                java.util.List.of("river_crossing", "settlement"), 60);
        var gen = ScenarioGenerator.generate(spec, profiles);
        state = gen.state();
        engine = new TacticalEngine(state, new Random(seed ^ 0x5151));
        engine.start();
        selected = null;
        selection.clear();
        groups.clear();
        mode = Mode.NONE;
        staged.clear();
        stagedUnits.clear();
        refresh();
    }

    public void click(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= state.width() || ty >= state.height()) return;
        switch (mode) {
            case MOVE -> stageMove(tx, ty);
            case ATTACK -> stageAttack(tx, ty);
            case CAS -> stageCas(tx, ty);
            case SMOKE -> stageSmoke(tx, ty);
            case ARTY -> stageArty(tx, ty);
            default -> selectAt(tx, ty);
        }
    }

    private void selectAt(int tx, int ty) {
        Unit u = state.friendlyUnitAt(tx, ty);
        selection.clear();
        if (u != null && !u.destroyed) selection.add(u);
        syncSelected();
        refresh();
    }

    public boolean canAct() {
        return selected != null && !selected.destroyed && !stagedUnits.contains(selected.id);
    }

    public void beginMove() { if (canAct()) { mode = Mode.MOVE; refresh(); } }
    public void beginAttack() { if (canAct()) { mode = Mode.ATTACK; refresh(); } }
    public void beginCas() { if (canAct()) { mode = Mode.CAS; refresh(); } }
    public void beginSmoke() { if (canAct()) { mode = Mode.SMOKE; refresh(); } }
    public void beginArty() { if (canAct()) { mode = Mode.ARTY; refresh(); } }

    public void recon() {
        if (canAct()) { staged.add(new ReconAction(selected.id, selected.reconRadius)); markStaged(); }
    }

    public void resupply() {
        if (canAct()) { staged.add(new ResupplyAction(selected.id)); markStaged(); }
    }

    public void setStance(Stance s) {
        if (selected != null && !selected.destroyed) {
            engine.resolveAction(new SetStanceAction(selected.id, s), false);
            refresh();
        }
    }

    private void markStaged() {
        for (Unit u : selection) stagedUnits.add(u.id);
        mode = Mode.NONE;
        refresh();
    }

    private void stageMove(int tx, int ty) {
        if (!canAct()) return;
        for (int[] r : Movement.reachable(state, selected)) {
            if (r[0] == tx && r[1] == ty) {
                staged.add(new MoveAction(selected.id, tx, ty));
                markStaged();
                return;
            }
        }
    }

    private void stageAttack(int tx, int ty) {
        if (!canAct()) return;
        Unit e = state.enemyUnitAt(tx, ty);
        if (e != null && !e.destroyed && e.knownToPlayer) {
            staged.add(new AttackAction(selected.id, e.id));
            markStaged();
        }
    }

    private void stageCas(int tx, int ty) {
        if (!canAct()) return;
        staged.add(new CallCasAction(selected.id, tx, ty));
        markStaged();
    }

    private void stageSmoke(int tx, int ty) {
        if (!canAct()) return;
        staged.add(new CallSmokeAction(selected.id, tx, ty));
        markStaged();
    }

    private void stageArty(int tx, int ty) {
        if (!canAct()) return;
        staged.add(new CallArtilleryAction(selected.id, tx, ty));
        markStaged();
    }

    public List<int[]> reachableTiles() {
        if (mode == Mode.MOVE && canAct()) return Movement.reachable(state, selected);
        return new ArrayList<>();
    }

    public void endTurn() {
        engine.resolvePlayerTurn(new ArrayList<>(staged));
        staged.clear();
        stagedUnits.clear();
        mode = Mode.NONE;
        selected = null;
        refresh();
    }

    public void clearSelection() {
        selection.clear();
        syncSelected();
        refresh();
    }

    // --- M12: RTS interaction layer (selection, groups, context orders) ---

    /** Drag-box select of all friendly units within the tile rectangle. */
    public void selectInBox(int x0, int y0, int x1, int y1) {
        selection.clear();
        for (Unit u : state.friendlyUnits) {
            if (!u.destroyed && u.x >= x0 && u.x <= x1 && u.y >= y0 && u.y <= y1) selection.add(u);
        }
        syncSelected();
        refresh();
    }

    /** Assign the current selection to control group n (1..9). */
    public void assignGroup(int n) {
        groups.put(n, selection.stream().map(u -> u.id).collect(Collectors.toList()));
        refresh();
    }

    /** Recall control group n into the selection. */
    public void recallGroup(int n) {
        List<Integer> ids = groups.get(n);
        selection.clear();
        if (ids != null) {
            for (int id : ids) {
                Unit u = state.friendlyById(id);
                if (u != null && !u.destroyed) selection.add(u);
            }
        }
        syncSelected();
        refresh();
    }

    /** Right-click context order on a tile: attack enemy, resupply at logistics, else move. */
    public void contextOrder(int tx, int ty) {
        if (selection.isEmpty() || state == null) return;
        if (tx < 0 || ty < 0 || tx >= state.width() || ty >= state.height()) return;
        Unit enemy = state.enemyUnitAt(tx, ty);
        Unit friendly = state.friendlyUnitAt(tx, ty);
        if (enemy != null && enemy.knownToPlayer && !enemy.destroyed) {
            for (Unit u : selection) if (!stagedUnits.contains(u.id)) staged.add(new AttackAction(u.id, enemy.id));
            markStaged();
        } else if (friendly != null && friendly.hasSpecial("resupply_source") && !friendly.destroyed) {
            for (Unit u : selection) if (!stagedUnits.contains(u.id)) staged.add(new ResupplyAction(u.id));
            markStaged();
        } else {
            int idx = 0;
            for (Unit u : selection) {
                if (stagedUnits.contains(u.id)) continue;
                int[] dest = spreadTarget(u, tx, ty, idx++);
                if (dest != null) staged.add(new MoveAction(u.id, dest[0], dest[1]));
            }
            markStaged();
        }
    }

    /** Pick a reachable, free destination near the target so multiple units don't stack. */
    private int[] spreadTarget(Unit u, int tx, int ty, int idx) {
        int[][] off = {{0,0},{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,-1},{1,-1},{-1,1},{2,0},{-2,0},{0,2},{0,-2}};
        double budget = u.effectiveMovementPoints() * u.stance.moveMult;
        for (int k = idx; k < off.length + idx; k++) {
            int[] o = off[k % off.length];
            int nx = tx + o[0], ny = ty + o[1];
            if (nx < 0 || ny < 0 || nx >= state.width() || ny >= state.height()) continue;
            if (state.grid[ny][nx].impassable()) continue;
            if (state.friendlyUnitAt(nx, ny) != null || state.enemyUnitAt(nx, ny) != null) continue;
            double c = Movement.pathCost(state.grid, u.x, u.y, nx, ny);
            if (c != Double.POSITIVE_INFINITY && c <= budget) return new int[]{nx, ny};
        }
        return null;
    }

    public void refresh() {
        if (onUpdate != null) onUpdate.run();
    }
}
