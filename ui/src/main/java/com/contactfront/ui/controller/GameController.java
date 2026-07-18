package com.contactfront.ui.controller;

import com.contactfront.engine.TacticalEngine;
import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.*;
import com.contactfront.engine.rules.LineOfSight;
import com.contactfront.engine.rules.Movement;
import com.contactfront.engine.terrain.ScenarioGenerator;
import com.contactfront.engine.terrain.ScenarioGenerator.ScenarioSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class GameController {
    public GameState state;
    public TacticalEngine engine;
    public Profiles profiles;
    public Unit selected;
    public final List<Unit> selection = new ArrayList<>();
    public final Map<Integer, List<Integer>> groups = new HashMap<>();
    public long seed;
    public Runnable onUpdate;
    public Faction playerFaction = Faction.USA;
    public Faction enemyFaction = Faction.RUSSIA;

    private void syncSelected() { if (selection.isEmpty()) selected = null; else selected = selection.get(0); }

    public void setPlayerFaction(Faction faction) {
        this.playerFaction = faction;
    }

    public void clearSelection() {
        selection.clear();
        selected = null;
        refresh();
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
    }

    public void click(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= state.width() || ty >= state.height()) return;
        if (selection.isEmpty()) {
            selectAt(tx, ty);
            return;
        }
        // In real-time mode, execute instantly (no staging)
        Unit enemy = state.enemyUnitAt(tx, ty);
        Unit friendly = state.friendlyUnitAt(tx, ty);
        if (enemy != null && enemy.knownToPlayer && !enemy.destroyed) {
            for (Unit u : selection) {
                if (LineOfSight.hasLineOfSight(u.x, u.y, enemy.x, enemy.y, state)) {
                    int dist = Math.abs(u.x - enemy.x) + Math.abs(u.y - enemy.y);
                    boolean inRange = false;
                    for (Weapon w : u.weapons) {
                        if (w.ammo > 0 && w.profile.range() >= dist && w.profile.canTarget(TargetType.GROUND)) {
                            inRange = true;
                            break;
                        }
                    }
                    if (inRange) {
                        engine.resolveAction(new AttackAction(u.id, enemy.id), false);
                    }
                }
            }
        } else if (friendly != null && friendly.hasSpecial("resupply_source") && !friendly.destroyed) {
            for (Unit u : selection) {
                int dist = Math.abs(u.x - friendly.x) + Math.abs(u.y - friendly.y);
                if (dist <= 1 && u.totalAmmo() < u.weapons.stream().mapToInt(w -> w.maxAmmo).sum()) {
                    engine.resolveAction(new ResupplyAction(u.id), false);
                }
            }
        } else {
            int idx = 0;
            for (Unit u : selection) {
                int[] dest = spreadTarget(u, tx, ty, idx++);
                if (dest != null) {
                    engine.resolveAction(new MoveAction(u.id, dest[0], dest[1]), false);
                }
            }
        }
        selection.clear();
        syncSelected();
        refresh();
    }

    private void selectAt(int tx, int ty) {
        Unit u = state.friendlyUnitAt(tx, ty);
        selection.clear();
        if (u != null && !u.destroyed) selection.add(u);
        syncSelected();
        refresh();
    }

    public boolean canAct() {
        return selected != null && !selected.destroyed;
    }

    public void recon() {
        if (canAct()) { engine.resolveAction(new ReconAction(selected.id, selected.reconRadius), false); refresh(); }
    }

    public void resupply() {
        if (canAct()) { engine.resolveAction(new ResupplyAction(selected.id), false); refresh(); }
    }

    public void setStance(Stance s) {
        if (selected != null && !selected.destroyed) {
            engine.resolveAction(new SetStanceAction(selected.id, s), false);
            refresh();
        }
    }

    public void callCas(int tx, int ty) {
        if (canAct() && state.casAvailable > 0) {
            engine.resolveAction(new CallCasAction(selected.id, tx, ty), false);
            refresh();
        }
    }

    public void callArty(int tx, int ty) {
        if (canAct() && state.artilleryFiresRemaining > 0) {
            engine.resolveAction(new CallArtilleryAction(selected.id, tx, ty), false);
            refresh();
        }
    }

    public void callSmoke(int tx, int ty) {
        if (canAct() && state.smokeGrenades > 0) {
            engine.resolveAction(new CallSmokeAction(selected.id, tx, ty), false);
            refresh();
        }
    }

    public List<int[]> reachableTiles() {
        if (selected != null && !selected.destroyed) return Movement.reachable(state, selected);
        return new ArrayList<>();
    }

    public void selectInBox(int x0, int y0, int x1, int y1) {
        selection.clear();
        for (Unit u : state.friendlyUnits) {
            if (!u.destroyed && u.x >= x0 && u.x <= x1 && u.y >= y0 && u.y <= y1) selection.add(u);
        }
        syncSelected();
        refresh();
    }

    public void assignGroup(int n) {
        groups.put(n, selection.stream().map(u -> u.id).collect(Collectors.toList()));
        refresh();
    }

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

    public void contextOrder(int tx, int ty) {
        if (selection.isEmpty() || state == null) return;
        if (tx < 0 || ty < 0 || tx >= state.width() || ty >= state.height()) return;
        Unit enemy = state.enemyUnitAt(tx, ty);
        Unit friendly = state.friendlyUnitAt(tx, ty);
        if (enemy != null && enemy.knownToPlayer && !enemy.destroyed) {
            for (Unit u : selection) {
                if (LineOfSight.hasLineOfSight(u.x, u.y, enemy.x, enemy.y, state)) {
                    int dist = Math.abs(u.x - enemy.x) + Math.abs(u.y - enemy.y);
                    boolean inRange = false;
                    for (Weapon w : u.weapons) {
                        if (w.ammo > 0 && w.profile.range() >= dist && w.profile.canTarget(TargetType.GROUND)) {
                            inRange = true;
                            break;
                        }
                    }
                    if (inRange) {
                        engine.resolveAction(new AttackAction(u.id, enemy.id), false);
                    }
                }
            }
        } else if (friendly != null && friendly.hasSpecial("resupply_source") && !friendly.destroyed) {
            for (Unit u : selection) {
                int dist = Math.abs(u.x - friendly.x) + Math.abs(u.y - friendly.y);
                if (dist <= 1 && u.totalAmmo() < u.weapons.stream().mapToInt(w -> w.maxAmmo).sum()) {
                    engine.resolveAction(new ResupplyAction(u.id), false);
                }
            }
        } else {
            int idx = 0;
            for (Unit u : selection) {
                int[] dest = spreadTarget(u, tx, ty, idx++);
                if (dest != null) {
                    engine.resolveAction(new MoveAction(u.id, dest[0], dest[1]), false);
                }
            }
        }
        selection.clear();
        syncSelected();
        refresh();
    }

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
