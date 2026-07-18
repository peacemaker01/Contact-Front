package com.contactfront.engine.rules;

import com.contactfront.engine.model.*;
import com.contactfront.ui.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class AiTurn {
    private AiTurn() {}

    private static class Node implements Comparable<Node> {
        int x, y;
        double g, f;
        Node parent;
        Node(int x, int y, double g, double f, Node parent) {
            this.x = x; this.y = y;
            this.g = g; this.f = f;
            this.parent = parent;
        }
        public int compareTo(Node o) { return Double.compare(this.f, o.f); }
    }

    private static List<int[]> findPath(GameState s, Unit u, int tx, int ty) {
        int w = s.width(), h = s.height();
        double[][] dist = new double[h][w];
        for (int y = 0; y < h; y++) java.util.Arrays.fill(dist[y], Double.POSITIVE_INFINITY);

        java.util.PriorityQueue<Node> open = new java.util.PriorityQueue<>();
        open.add(new Node(u.x, u.y, 0.0, manhattan(u.x, u.y, tx, ty), null));
        dist[u.y][u.x] = 0.0;

        Node endNode = null;
        while (!open.isEmpty()) {
            Node curr = open.poll();
            if (curr.g > dist[curr.y][curr.x]) continue;
            if (curr.x == tx && curr.y == ty) {
                endNode = curr;
                break;
            }

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = curr.x + dx, ny = curr.y + dy;
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;

                    Tile tile = s.grid[ny][nx];
                    if (tile.impassable()) continue;

                    boolean occupied = false;
                    for (Unit other : s.friendlyUnits) {
                        if (!other.destroyed && other.x == nx && other.y == ny) { occupied = true; break; }
                    }
                    for (Unit other : s.enemyUnits) {
                        if (other != u && !other.destroyed && other.x == nx && other.y == ny) { occupied = true; break; }
                    }
                    if (occupied && (nx != tx || ny != ty)) continue;

                    double cost = tile.movementCost;

if (u.profile.category() == UnitCategory.ARMOR) {
                        if (tile.type == Terrain.ROAD || tile.type == Terrain.ROAD_VERT || tile.type == Terrain.ROAD_CROSS) {
                            cost *= 0.5;
                        }
                        if (tile.type == Terrain.BUILDING) {
                            cost += 2.0;
                        }
                    } else if (u.profile.category() == UnitCategory.INFANTRY
                            || u.profile.category() == UnitCategory.RECON
                            || u.profile.category() == UnitCategory.ENGINEER) {
                        if (u.strength < 50 || u.isSuppressed()) {
                            if (tile.coverBonus > 0) cost *= 0.5;
                            else cost += 2.0;
                        } else {
                            if (tile.coverBonus > 0) cost *= 0.8;
                        }
                    }

                    double nextG = curr.g + cost;
                    if (nextG < dist[ny][nx]) {
                        dist[ny][nx] = nextG;
                        open.add(new Node(nx, ny, nextG, nextG + manhattan(nx, ny, tx, ty), curr));
                    }
                }
            }
        }

        if (endNode == null) return null;

        List<int[]> path = new ArrayList<>();
        Node c = endNode;
        while (c != null) {
            path.add(0, new int[]{c.x, c.y});
            c = c.parent;
        }
        return path;
    }

    private static int[] findBackLineCover(GameState s) {
        int w = s.width(), h = s.height();
        int bestX = w - 2, bestY = h / 2;
        int maxCover = -99;
        for (int y = 0; y < h; y++) {
            for (int x = w - 5; x < w; x++) {
                Tile t = s.grid[y][x];
                if (t.impassable()) continue;
                if (t.coverBonus > maxCover) {
                    maxCover = t.coverBonus;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        return new int[]{bestX, bestY};
    }

    private static int[] findNearbyCoverWithinRange(GameState s, Unit u, Unit target, int range) {
        List<int[]> reachable = Movement.reachable(s, u);
        int bestX = -1, bestY = -1;
        int maxCover = -99;
        if (s.grid[u.y][u.x].coverBonus > maxCover) {
            maxCover = s.grid[u.y][u.x].coverBonus;
            bestX = u.x;
            bestY = u.y;
        }
        for (int[] p : reachable) {
            int dist = manhattan(p[0], p[1], target.x, target.y);
            if (dist <= range) {
                Tile t = s.grid[p[1]][p[0]];
                if (t.coverBonus > maxCover) {
                    maxCover = t.coverBonus;
                    bestX = p[0];
                    bestY = p[1];
                }
            }
        }
        return bestX != -1 ? new int[]{bestX, bestY} : null;
    }

    private static int[] findNearestCover(GameState s, Unit u) {
        List<int[]> reachable = Movement.reachable(s, u);
        int bestX = -1, bestY = -1;
        int maxCover = 0;
        int minDist = Integer.MAX_VALUE;
        for (int[] p : reachable) {
            Tile t = s.grid[p[1]][p[0]];
            if (t.coverBonus > maxCover) {
                maxCover = t.coverBonus;
                bestX = p[0];
                bestY = p[1];
                minDist = manhattan(u.x, u.y, p[0], p[1]);
            } else if (t.coverBonus == maxCover && maxCover > 0) {
                int d = manhattan(u.x, u.y, p[0], p[1]);
                if (d < minDist) {
                    minDist = d;
                    bestX = p[0];
                    bestY = p[1];
                }
            }
        }
        return bestX != -1 ? new int[]{bestX, bestY} : null;
    }

    public static void continuousRun(GameState s, Random rng) {
        // Director evaluation for support assets
        Director.evaluate(s, s.turn);

        for (Unit enemy : new ArrayList<>(s.enemyUnits)) {
            if (enemy.destroyed || enemy.routed) continue;

            if (enemy.hasSpecial("kamikaze")) {
                Unit t = closestVisibleFriendly(s, enemy);
                if (t != null && manhattan(enemy.x, enemy.y, t.x, t.y) <= enemyReconOrWeapon(s, enemy, t)) {
                    FpvAttack.strike(s, enemy, t, rng);
                    continue;
                }
            }

            // Use Director scoring for utility-based decisions
            var actionScores = Director.scoreUnitActions(s, enemy);
            String chosenAction = actionScores.stream()
                .max((a, b) -> Double.compare(a.weight(), b.weight()))
                .map(Director.ActionScore::action)
                .orElse("hold_position");

            Unit target = closestVisibleFriendly(s, enemy);
            int tx = s.width() / 2;
            int ty = s.height() / 2;

            boolean isSupport = enemy.profile.category() == UnitCategory.ARTILLERY 
                             || enemy.profile.category() == UnitCategory.LOGISTICS;

            if (isSupport) {
                int[] backCover = findBackLineCover(s);
                tx = backCover[0];
                ty = backCover[1];
                enemy.stance = Stance.DEFENSIVE;
            } else if (target != null) {
                int dist = manhattan(enemy.x, enemy.y, target.x, target.y);
                int bestRange = enemyBestRange(s, enemy);

                if (chosenAction.equals("retreat") && enemy.strength < 30) {
                    int[] retreatPoint = findRetreatPoint(s, enemy);
                    if (retreatPoint != null) {
                        tx = retreatPoint[0];
                        ty = retreatPoint[1];
                        enemy.stance = Stance.DEFENSIVE;
                    }
                } else if (chosenAction.equals("seek_cover") && enemy.isSuppressed()) {
                    int[] coverTile = findNearestCover(s, enemy);
                    if (coverTile != null) {
                        tx = coverTile[0];
                        ty = coverTile[1];
                        enemy.stance = Stance.DEFENSIVE;
                    }
                } else if (chosenAction.equals("flank") && dist > 3) {
                    int[] flankPos = findFlankPosition(s, enemy, target);
                    if (flankPos != null) {
                        tx = flankPos[0];
                        ty = flankPos[1];
                    }
                } else if (chosenAction.equals("engage")) {
                    if (dist <= bestRange) {
                        if (enemy.profile.category() == UnitCategory.ARMOR) {
                            int[] coverTile = findNearbyCoverWithinRange(s, enemy, target, bestRange);
                            if (coverTile != null) {
                                tx = coverTile[0];
                                ty = coverTile[1];
                            }
                        } else {
                            int[] coverTile = findNearestCover(s, enemy);
                            if (coverTile != null) {
                                tx = coverTile[0];
                                ty = coverTile[1];
                            } else {
                                tx = target.x;
                                ty = target.y;
                            }
                        }
                    } else {
                        tx = target.x;
                        ty = target.y;
                    }
                } else {
                    tx = target.x;
                    ty = target.y;
                }
            } else if (!s.objectives.isEmpty()) {
                tx = s.objectives.get(0).x;
                ty = s.objectives.get(0).y;
            }

            if (!isSupport) {
                chooseStance(enemy, s);
            }

            if (enemy.x != tx || enemy.y != ty) {
                List<int[]> path = findPath(s, enemy, tx, ty);
                if (path != null && path.size() > 1) {
                    boolean pathBlocked = false;
                    for (int i = 1; i < path.size(); i++) {
                        int[] step = path.get(i);
                        if (s.hasSmoke(step[0], step[1])) {
                            pathBlocked = true;
                            break;
                        }
                        boolean hasFriendlyBlocking = false;
                        for (Unit f : s.friendlyUnits) {
                            if (!f.destroyed && f.x == step[0] && f.y == step[1]) {
                                hasFriendlyBlocking = true;
                                break;
                            }
                        }
                        if (hasFriendlyBlocking) {
                            pathBlocked = true;
                            break;
                        }
                        boolean success = Movement.applyMove(s, enemy, step[0], step[1]);
                        if (success) {
                            overwatchFire(s, enemy, rng);
                            if (enemy.destroyed || enemy.routed) break;
                        } else {
                            break;
                        }
                    }
                    if (pathBlocked && enemy.strength > 40) {
                        boolean shouldFlank = true;
                        if (s.commandMode == CommandMode.DOCTRINE) {
                            Doctrine doctrine = s.factionDoctrines.get(enemy.faction);
                            if (doctrine != null) {
                                shouldFlank = doctrine.shouldFlank(enemy);
                            }
                        }
                        if (shouldFlank) {
                            int[] flankPath = findFlankPath(s, enemy, tx, ty);
                            if (flankPath != null) {
                                boolean success = Movement.applyMove(s, enemy, flankPath[0], flankPath[1]);
                                if (success) {
                                    overwatchFire(s, enemy, rng);
                                }
                            }
                        }
                    }
                }
            }

            if (enemy.destroyed || enemy.routed) continue;

            Unit fireTarget = closestVisibleFriendly(s, enemy);
            if (fireTarget != null && manhattan(enemy.x, enemy.y, fireTarget.x, fireTarget.y) <= enemyBestRange(s, enemy)) {
                Combat.resolveFire(s, enemy, fireTarget, rng);
                s.log("combat", enemy.profile.name() + " engages " + fireTarget.profile.name() + ".");
            }
        }

        processEnemyDelayedOrders(s, rng);
    }

    private static void processEnemyDelayedOrders(GameState s, Random rng) {
        for (DelayedOrder order : s.delayedOrders) {
            if (order.command instanceof EnemyStrike es) {
                s.delayedOrders.remove(order);
                if (es.kind().equals(EnemyStrike.ARTY)) {
                    int cep = s.ewGpsJammed ? 100 : 50;
                    Artillery.resolve(s, es.x(), es.y(), es.rounds(), rng, cep);
                } else if (es.kind().equals(EnemyStrike.CAS)) {
                    CloseAirSupport.applyCas(s, es.x(), es.y(), rng);
                }
            }
        }
    }

    public static void run(GameState s, Random rng) {
        if (s.commandMode == CommandMode.DOCTRINE) {
            for (Unit enemy : s.enemyUnits) {
                if (!enemy.destroyed) {
                    Doctrine doctrine = s.factionDoctrines.get(enemy.faction);
                    if (doctrine != null) {
                        enemy.applyDoctrine(doctrine);
                    }
                }
            }
        }
        indirectFire(s, rng);

        for (Unit enemy : new ArrayList<>(s.enemyUnits)) {
            if (enemy.destroyed || enemy.routed) continue;

            if (enemy.hasSpecial("kamikaze")) {
                Unit t = closestVisibleFriendly(s, enemy);
                if (t != null && manhattan(enemy.x, enemy.y, t.x, t.y) <= enemyReconOrWeapon(s, enemy, t)) {
                    FpvAttack.strike(s, enemy, t, rng);
                    continue;
                }
            }

            Unit target = closestVisibleFriendly(s, enemy);
            int tx = s.width() / 2;
            int ty = s.height() / 2;

            boolean isSupport = enemy.profile.category() == UnitCategory.ARTILLERY 
                             || enemy.profile.category() == UnitCategory.LOGISTICS;

            if (isSupport) {
                int[] backCover = findBackLineCover(s);
                tx = backCover[0];
                ty = backCover[1];
                enemy.stance = Stance.DEFENSIVE;
            } else if (target != null) {
                int dist = manhattan(enemy.x, enemy.y, target.x, target.y);
                int bestRange = enemyBestRange(s, enemy);

                if (enemy.profile.category() == UnitCategory.ARMOR) {
                    if (dist <= bestRange) {
                        int[] coverTile = findNearbyCoverWithinRange(s, enemy, target, bestRange);
                        if (coverTile != null) {
                            tx = coverTile[0];
                            ty = coverTile[1];
                        } else {
                            tx = enemy.x;
                            ty = enemy.y;
                        }
                    } else {
                        tx = target.x;
                        ty = target.y;
                    }
                } else {
                    if (enemy.strength < 50 || enemy.isSuppressed()) {
                        int[] coverTile = findNearestCover(s, enemy);
                        if (coverTile != null) {
                            tx = coverTile[0];
                            ty = coverTile[1];
                            enemy.stance = Stance.DEFENSIVE;
                        } else {
                            tx = target.x;
                            ty = target.y;
                        }
                    } else {
                        if (enemy.stance != Stance.OVERWATCH) {
                            Unit overwatchTarget = findOverwatchThreat(s, enemy);
                            if (overwatchTarget != null && manhattan(enemy.x, enemy.y, overwatchTarget.x, overwatchTarget.y) <= bestRange) {
                                tx = overwatchTarget.x;
                                ty = overwatchTarget.y;
                            } else {
                                tx = target.x;
                                ty = target.y;
                            }
                        } else {
                            tx = target.x;
                            ty = target.y;
                        }
                    }
                }
            } else if (!s.objectives.isEmpty()) {
                tx = s.objectives.get(0).x;
                ty = s.objectives.get(0).y;
            }

            if (!isSupport) {
                chooseStance(enemy, s);
            }

            if (enemy.x != tx || enemy.y != ty) {
                List<int[]> path = findPath(s, enemy, tx, ty);
                if (path != null && path.size() > 1) {
                    boolean pathBlocked = false;
                    for (int i = 1; i < path.size(); i++) {
                        int[] step = path.get(i);
                        if (s.hasSmoke(step[0], step[1])) {
                            pathBlocked = true;
                            break;
                        }
                        boolean hasFriendlyBlocking = false;
                        for (Unit f : s.friendlyUnits) {
                            if (!f.destroyed && f.x == step[0] && f.y == step[1]) {
                                hasFriendlyBlocking = true;
                                break;
                            }
                        }
                        if (hasFriendlyBlocking) {
                            pathBlocked = true;
                            break;
                        }
                        boolean success = Movement.applyMove(s, enemy, step[0], step[1]);
                        if (success) {
                            overwatchFire(s, enemy, rng);
                            if (enemy.destroyed || enemy.routed) break;
                        } else {
                            break;
                        }
                    }
                    if (pathBlocked && enemy.strength > 40) {
                        boolean shouldFlank = true;
                        if (s.commandMode == CommandMode.DOCTRINE) {
                            Doctrine doctrine = s.factionDoctrines.get(enemy.faction);
                            if (doctrine != null) {
                                shouldFlank = doctrine.shouldFlank(enemy);
                            }
                        }
                        if (shouldFlank) {
                            int[] flankPath = findFlankPath(s, enemy, tx, ty);
                            if (flankPath != null) {
                                boolean success = Movement.applyMove(s, enemy, flankPath[0], flankPath[1]);
                                if (success) {
                                    overwatchFire(s, enemy, rng);
                                }
                            }
                        }
                    }
                }
            }

            if (enemy.destroyed || enemy.routed) continue;

            Unit fireTarget = closestVisibleFriendly(s, enemy);
            if (fireTarget != null && manhattan(enemy.x, enemy.y, fireTarget.x, fireTarget.y) <= enemyBestRange(s, enemy)) {
                Combat.resolveFire(s, enemy, fireTarget, rng);
                s.log("combat", enemy.profile.name() + " engages " + fireTarget.profile.name() + ".");
            }
        }

        for (Unit u : s.enemyUnits) if (!u.destroyed) u.movementPoints = u.movement;
    }

    private static int enemyReconOrWeapon(GameState s, Unit enemy, Unit t) {
        int max = 0;
        for (var w : enemy.weapons) max = Math.max(max, w.profile.range());
        return Math.max(enemy.reconRadius, max);
    }

    private static int enemyBestRange(GameState s, Unit enemy) {
        int max = 0;
        for (var w : enemy.weapons) max = Math.max(max, w.profile.range());
        return Math.max(1, max);
    }

    private static void chooseStance(Unit enemy, GameState s) {
        if (s.commandMode == CommandMode.DOCTRINE) {
            Doctrine doctrine = s.factionDoctrines.get(enemy.faction);
            if (doctrine != null) {
                enemy.stance = doctrine.getDefaultStance();
                return;
            }
        }
        if (enemy.strength < 40 || enemy.isSuppressed()) {
            enemy.stance = Stance.DEFENSIVE;
        } else {
            enemy.stance = Stance.AGGRESSIVE;
        }
    }

    private static void overwatchFire(GameState s, Unit enemy, Random rng) {
        for (Unit f : s.friendlyUnits) {
            if (f.destroyed || f.stance != Stance.OVERWATCH) continue;
            if (!f.hasAmmo()) continue;
            if (!LineOfSight.hasLineOfSight(f.x, f.y, enemy.x, enemy.y, s)) continue;
            if (manhattan(f.x, f.y, enemy.x, enemy.y) > friendlyBestRange(s, f)) continue;
            Combat.resolveFire(s, f, enemy, rng);
        }
    }

    private static int friendlyBestRange(GameState s, Unit f) {
        int max = 0;
        for (var w : f.weapons) max = Math.max(max, w.profile.range());
        return Math.max(1, max);
    }

    private static void indirectFire(GameState s, Random rng) {
        List<int[]> visible = new ArrayList<>();
        for (Unit f : s.friendlyUnits) {
            if (f.destroyed) continue;
            boolean seen = false;
            for (Unit e : s.enemyUnits) {
                if (!e.destroyed && Visibility.enemySees(s, e, f)) { seen = true; break; }
            }
            if (seen) visible.add(new int[]{f.x, f.y});
        }
        if (visible.isEmpty()) return;
        int[] center = clusterCenter(visible);
        int best = 0;
        for (int[] p : visible) {
            if (Math.abs(p[0] - center[0]) <= 2 && Math.abs(p[1] - center[1]) <= 2) best++;
        }
        if (s.enemyArtilleryFiresRemaining > 0 && best >= 2) {
            s.enemyArtilleryFiresRemaining--;
            s.delayedOrders.add(new com.contactfront.engine.model.DelayedOrder(
                    new com.contactfront.engine.model.EnemyStrike(
                            com.contactfront.engine.model.EnemyStrike.ARTY, center[0], center[1], 4),
                    s.turn + 1, 0));
            s.log("orders", "Enemy artillery scheduled for next turn.");
        }
        if (s.enemyCasAvailable > 0 && best >= 1) {
            s.enemyCasAvailable--;
            s.delayedOrders.add(new com.contactfront.engine.model.DelayedOrder(
                    new com.contactfront.engine.model.EnemyStrike(
                            com.contactfront.engine.model.EnemyStrike.CAS, center[0], center[1], 0),
                    s.turn + 1, 0));
            s.log("orders", "Enemy CAS scheduled for next turn.");
        }
    }

    private static Unit closestVisibleFriendly(GameState s, Unit from) {
        Unit best = null;
        int min = Integer.MAX_VALUE;
        for (Unit f : s.friendlyUnits) {
            if (f.destroyed) continue;
            if (!Visibility.enemySees(s, from, f)) continue;
            int d = manhattan(from.x, from.y, f.x, f.y);
            if (d < min) { min = d; best = f; }
        }
        return best;
    }

    private static int[] clusterCenter(List<int[]> positions) {
        int[] best = positions.get(0);
        int bestCount = 0;
        for (int[] p : positions) {
            int c = 0;
            for (int[] q : positions) {
                if (Math.abs(p[0] - q[0]) <= 2 && Math.abs(p[1] - q[1]) <= 2) c++;
            }
            if (c > bestCount) { bestCount = c; best = p; }
        }
        return best;
    }

    private static int manhattan(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    private static Unit findOverwatchThreat(GameState s, Unit enemy) {
        Unit bestThreat = null;
        int bestRange = 0;
        for (Unit f : s.friendlyUnits) {
            if (f.destroyed || f.stance != Stance.OVERWATCH) continue;
            int dist = manhattan(enemy.x, enemy.y, f.x, f.y);
            if (dist <= bestRange) continue;
            if (!LineOfSight.hasLineOfSight(f.x, f.y, enemy.x, enemy.y, s)) continue;
            int fRange = friendlyBestRange(s, f);
            if (dist <= fRange) {
                bestRange = dist;
                bestThreat = f;
            }
        }
        return bestThreat;
    }

    private static int[] findFlankPath(GameState s, Unit enemy, int tx, int ty) {
        int w = s.width(), h = s.height();
        int[] best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = enemy.x + dx * 3;
                int ny = enemy.y + dy * 3;
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (s.grid[ny][nx].impassable()) continue;
                boolean occupied = false;
                for (Unit u : s.friendlyUnits) if (!u.destroyed && u.x == nx && u.y == ny) { occupied = true; break; }
                for (Unit u : s.enemyUnits) if (!u.destroyed && u != enemy && u.x == nx && u.y == ny) { occupied = true; break; }
                if (occupied) continue;
                int dist = Math.abs(nx - tx) + Math.abs(ny - ty);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new int[]{nx, ny};
                }
            }
        }
        return best;
    }

    private static int[] findRetreatPoint(GameState s, Unit enemy) {
        int w = s.width(), h = s.height();
        int[] best = null;
        int bestDist = Integer.MAX_VALUE;
        int bestCover = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Tile t = s.grid[y][x];
                if (t.impassable()) continue;
                boolean occupied = false;
                for (Unit u : s.enemyUnits) if (!u.destroyed && u.x == x && u.y == y) { occupied = true; break; }
                if (occupied) continue;
                int dist = manhattan(enemy.x, enemy.y, x, y);
                if (dist < bestDist || (dist == bestDist && t.coverBonus > bestCover)) {
                    bestDist = dist;
                    bestCover = t.coverBonus;
                    best = new int[]{x, y};
                }
            }
        }
        return best;
    }

    private static int[] findFlankPosition(GameState s, Unit enemy, Unit target) {
        int w = s.width(), h = s.height();
        int[] best = null;
        int bestDistToTarget = Integer.MAX_VALUE;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int nx = target.x + dx;
                int ny = target.y + dy;
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                Tile t = s.grid[ny][nx];
                if (t.impassable()) continue;
                boolean occupied = false;
                for (Unit u : s.enemyUnits) if (!u.destroyed && u.x == nx && u.y == ny) { occupied = true; break; }
                if (occupied) continue;
                for (Unit u : s.friendlyUnits) if (!u.destroyed && u.x == nx && u.y == ny) { occupied = true; break; }
                if (occupied) continue;
                int dist = manhattan(enemy.x, enemy.y, nx, ny);
                if (dist < bestDistToTarget) {
                    bestDistToTarget = dist;
                    best = new int[]{nx, ny};
                }
            }
        }
        return best;
    }

    private record ObjectiveLike(int x, int y) {}
}
