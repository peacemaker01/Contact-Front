package com.contactfront.engine.model;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    public int turn = 1;
    public int maxTurns = 999;
    public long elapsedMs = 0;
    public String mode = "realtime";
    public Faction playerFaction;
    public Faction enemyFaction;
    public String scenarioId = "default";

    public CommandMode commandMode = CommandMode.DOCTRINE;
    public java.util.Map<Faction, Doctrine> factionDoctrines = new java.util.HashMap();

    public double latitude = 35.0;
    public double longitude = -120.0;
    public String locationName = "Generated Location";

    public Tile[][] grid;
    public Visibility[][] visibility;
    public final List<Unit> friendlyUnits = new ArrayList<>();
    public final List<Unit> enemyUnits = new ArrayList<>();
    public final List<Unit> placedUnits = new ArrayList<>();

    public int artilleryFiresRemaining = 3;
    public int casAvailable = 2;
    public int smokeGrenades = 2;

    public int enemyArtilleryFiresRemaining = 3;
    public int enemyCasAvailable = 2;

    public final List<int[]> supplyDepots = new ArrayList<>();
    public final List<Objective> objectives = new ArrayList<>();
    public final List<String> narrativeLog = new ArrayList<>();
    public final List<String> actionLog = new ArrayList<>();

    /** Structured, channel-tagged event log for the M11 split feed (combat/intel/orders). */
    public final List<LogEntry> eventLog = new ArrayList<>();

    /** Normalized 0..1 heightmap (M13 baking). Null when not generated. */
    public double[][] elevation;
    /** Normalized 0..1 moisture field (M13 baking). Null when not generated. */
    public double[][] moisture;

    /** OSM-derived road segments for semantic grid overlay. */
    public final List<RoadSegment> roadSegments = new ArrayList<>();
    /** OSM-derived building footprints for 3D extrusion. */
    public final List<Building> buildings = new ArrayList<>();

    public static final class LogEntry {
        public final String channel;
        public final String text;
        public final long elapsedMs;
        public LogEntry(String channel, String text, long elapsedMs) {
            this.channel = channel;
            this.text = text;
            this.elapsedMs = elapsedMs;
        }
    }

    public int friendlyKia = 0;
    public int friendlyWia = 0;
    public int vehiclesLost = 0;
    public int enemyKia = 0;
    public int ammoExpended = 0;
    public int turnsUnderSuppression = 0;
    public int friendlyCasualties = 0;
    public int enemyCasualties = 0;

    public boolean isNight = false;
    public boolean isRaining = false;
    public boolean isWindy = false;
    public boolean ewGpsJammed = false;
    public boolean ewCommsJammed = false;

    public List<DelayedOrder> delayedOrders = new ArrayList<>();

    public boolean gameOver = false;
    public Boolean victory = null;

    public long seed = 0;

    public int width() {
        return grid == null || grid.length == 0 ? 0 : grid[0].length;
    }

    public int height() {
        return grid == null ? 0 : grid.length;
    }

    public void ensureVisibility() {
        if (visibility == null || visibility.length != height() || visibility[0].length != width()) {
            visibility = new Visibility[height()][width()];
            for (int y = 0; y < height(); y++) {
                for (int x = 0; x < width(); x++) visibility[y][x] = Visibility.UNSEEN;
            }
        }
    }

    public Unit friendlyUnitAt(int x, int y) {
        for (Unit u : friendlyUnits) if (!u.destroyed && u.x == x && u.y == y) return u;
        return null;
    }

    public Unit enemyUnitAt(int x, int y) {
        for (Unit u : enemyUnits) if (!u.destroyed && u.x == x && u.y == y) return u;
        return null;
    }

    public Unit enemyById(int id) {
        for (Unit u : enemyUnits) if (u.id == id) return u;
        return null;
    }

    public Unit friendlyById(int id) {
        for (Unit u : friendlyUnits) if (u.id == id) return u;
        return null;
    }

    public void log(String message) {
        narrativeLog.add(message);
        trimLog();
    }

    public void log(String channel, String message) {
        narrativeLog.add(message);
        eventLog.add(new LogEntry(channel, message, elapsedMs));
        trimLog();
        if (eventLog.size() > 400) eventLog.subList(0, eventLog.size() - 400).clear();
    }

    private void trimLog() {
        if (narrativeLog.size() > 200) narrativeLog.subList(0, narrativeLog.size() - 200).clear();
    }

    public int[][] smokeGrid;

    public boolean hasSmoke(int x, int y) {
        return smokeGrid != null && y >= 0 && y < height() && x >= 0 && x < width() && smokeGrid[y][x] > 0;
    }

    public void addSmoke(int x, int y, int turns) {
        if (smokeGrid == null) smokeGrid = new int[height()][width()];
        if (y >= 0 && y < height() && x >= 0 && x < width()) {
            smokeGrid[y][x] = Math.max(smokeGrid[y][x], turns);
        }
    }

    public void tickSmoke() {
        if (smokeGrid == null) return;
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                if (smokeGrid[y][x] > 0) smokeGrid[y][x]--;
            }
        }
    }
}