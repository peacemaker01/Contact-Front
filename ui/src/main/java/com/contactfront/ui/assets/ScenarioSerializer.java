package com.contactfront.ui.assets;

import com.contactfront.engine.model.CommandMode;
import com.contactfront.engine.model.Doctrine;
import com.contactfront.engine.model.Faction;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.Tile;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.UnitProfile;
import com.contactfront.engine.data.Profiles;
import com.contactfront.ui.view.ScenarioBuilder.ScenarioBuilderData;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public final class ScenarioSerializer {
    private static final String JSON_INDENT = "  ";

    private ScenarioSerializer() {}

    public static void saveScenario(ScenarioBuilderData data, GameState state, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        appendString(json, "name", data.scenarioName());
        appendString(json, "description", data.description());
        appendString(json, "playerFaction", data.playerFaction().name());
        appendString(json, "enemyFaction", data.enemyFaction().name());
        appendString(json, "commandMode", data.commandMode().name());
        appendString(json, "playerDoctrine", data.playerDoctrine().name());
        appendString(json, "enemyDoctrine", data.enemyDoctrine().name());
        appendString(json, "locationName", data.locationName());
        appendNumber(json, "latitude", data.latitude());
        appendNumber(json, "longitude", data.longitude());
        appendNumber(json, "width", data.width());
        appendNumber(json, "height", data.height());
        appendString(json, "notes", data.notes());
        
        json.append(JSON_INDENT).append("\"tiles\": [\n");
        if (state.grid != null) {
            boolean first = true;
            for (int y = 0; y < state.height(); y++) {
                for (int x = 0; x < state.width(); x++) {
                    Tile t = state.grid[y][x];
                    if (t != null) {
                        if (!first) json.append(",\n");
                        json.append(JSON_INDENT).append(JSON_INDENT)
                            .append("{\"x\": ").append(x)
                            .append(", \"y\": ").append(y)
                            .append(", \"terrain\": \"").append(t.type.name()).append("\"}");
                        first = false;
                    }
                }
            }
        }
        json.append("\n").append(JSON_INDENT).append("],\n");
        
        json.append(JSON_INDENT).append("\"units\": [\n");
        boolean firstUnit = true;
        List<Unit> unitsToSave = state.placedUnits.isEmpty() ? state.friendlyUnits : state.placedUnits;
        for (Unit u : unitsToSave) {
            if (!firstUnit) json.append(",\n");
            json.append(JSON_INDENT).append(JSON_INDENT)
                .append("{\"faction\": \"").append(u.faction.name()).append("\"")
                .append(", \"profileId\": \"").append(u.profile.id()).append("\"")
                .append(", \"x\": ").append(u.x)
                .append(", \"y\": ").append(u.y)
                .append(", \"id\": ").append(u.id)
                .append(", \"destroyed\": ").append(u.destroyed).append("}");
            firstUnit = false;
        }
        json.append("\n").append(JSON_INDENT).append("],\n");
        
        json.append(JSON_INDENT).append("\"objectives\": [\n");
        boolean firstObj = true;
        for (var obj : state.objectives) {
            if (!firstObj) json.append(",\n");
            json.append(JSON_INDENT).append(JSON_INDENT)
                .append("{\"name\": \"").append(obj.name).append("\"")
                .append(", \"type\": \"").append(obj.type).append("\"")
                .append(", \"x\": ").append(obj.x)
                .append(", \"y\": ").append(obj.y);
            if (obj.requiredTurns > 0) {
                json.append(", \"requiredTurns\": ").append(obj.requiredTurns);
            }
            if (obj.targetUnitId != 0) {
                json.append(", \"targetUnitId\": ").append(obj.targetUnitId);
            }
            json.append("}");
            firstObj = false;
        }
        json.append("\n").append(JSON_INDENT).append("]\n");
        
        json.append("}\n");
        
        try (Writer writer = new FileWriter(file.toFile())) {
            writer.write(json.toString());
        }
    }

    public static ScenarioLoadResult loadScenario(Path file) throws IOException {
        String content = Files.readString(file);
        
        ScenarioBuilderData scenarioData = parseScenarioData(content);
        GameState state = parseGameState(content, scenarioData);
        
        return new ScenarioLoadResult(scenarioData, state);
    }

    private static ScenarioBuilderData parseScenarioData(String json) {
        return new ScenarioBuilderData(
            extractString(json, "name", "Loaded Scenario"),
            extractString(json, "description", ""),
            Faction.valueOf(extractString(json, "playerFaction", "USA")),
            Faction.valueOf(extractString(json, "enemyFaction", "RUSSIA")),
            CommandMode.valueOf(extractString(json, "commandMode", "EXPLICIT")),
            Doctrine.valueOf(extractString(json, "playerDoctrine", "NATO")),
            Doctrine.valueOf(extractString(json, "enemyDoctrine", "RUSSIAN")),
            extractString(json, "locationName", "Unknown"),
            extractNumber(json, "latitude", 35.0),
            extractNumber(json, "longitude", -120.0),
            (int) extractNumber(json, "width", 40),
            (int) extractNumber(json, "height", 30),
            extractString(json, "notes", "")
        );
    }

    private static GameState parseGameState(String json, ScenarioBuilderData data) {
        GameState state = new GameState();
        state.latitude = data.latitude();
        state.longitude = data.longitude();
        state.locationName = data.locationName();
        state.commandMode = data.commandMode();
        
        state.factionDoctrines.put(data.playerFaction(), data.playerDoctrine());
        state.factionDoctrines.put(data.enemyFaction(), data.enemyDoctrine());
        
        int w = data.width();
        int h = data.height();
        state.grid = new Tile[h][w];
        
        List<String> tilesSection = extractArray(json, "tiles");
        for (String tileJson : tilesSection) {
            int x = (int) extractNumber(tileJson, "x", 0);
            int y = (int) extractNumber(tileJson, "y", 0);
            String terrainStr = extractString(tileJson, "terrain", "OPEN");
            Terrain terrain = Terrain.valueOf(terrainStr);
            if (y >= 0 && y < h && x >= 0 && x < w) {
                state.grid[y][x] = new Tile(terrain, x, y);
            }
        }
        
        parseUnits(json, state, data);
        parseObjectives(json, state);
        
        return state;
    }
    
    private static void parseObjectives(String json, GameState state) {
        List<String> objSection = extractArray(json, "objectives");
        for (String objJson : objSection) {
            String name = extractString(objJson, "name", "Objective");
            String type = extractString(objJson, "type", "capture");
            int x = (int) extractNumber(objJson, "x", 0);
            int y = (int) extractNumber(objJson, "y", 0);
            int requiredTurns = (int) extractNumber(objJson, "requiredTurns", 3);
            int targetUnitId = (int) extractNumber(objJson, "targetUnitId", 0);
            
            com.contactfront.engine.model.Objective obj = new com.contactfront.engine.model.Objective(
                "OBJ_" + state.objectives.size(), name, x, y, type);
            obj.requiredTurns = requiredTurns;
            obj.targetUnitId = targetUnitId;
            state.objectives.add(obj);
        }
    }
    
    private static void parseUnits(String json, GameState state, ScenarioBuilderData data) {
        List<String> unitsSection = extractArray(json, "units");
        Profiles profiles = Profiles.load();
        int nextId = 1;
        
        for (String unitJson : unitsSection) {
            String factionStr = extractString(unitJson, "faction", "USA");
            Faction placedFaction = Faction.valueOf(factionStr);
            String profileId = extractString(unitJson, "profileId", "inf_squad");
            int x = (int) extractNumber(unitJson, "x", 0);
            int y = (int) extractNumber(unitJson, "y", 0);
            
            UnitProfile profile = profiles.unit(profileId);
            if (profile == null) profile = profiles.unit("inf_squad");
            
            Unit unit = new Unit(nextId++, placedFaction, profile, x, y, profiles);
            unit.destroyed = extractString(unitJson, "destroyed", "false").equals("true");
            
            state.placedUnits.add(unit);
        }
        
        for (Unit u : state.placedUnits) {
            if (u.faction == data.playerFaction()) {
                state.friendlyUnits.add(u);
            } else {
                state.enemyUnits.add(u);
            }
        }
    }

    private static String extractString(String json, String key, String defaultValue) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return defaultValue;
        int start = json.indexOf(":", idx) + 1;
        int quoteStart = json.indexOf("\"", start);
        if (quoteStart < 0) return defaultValue;
        int quoteEnd = json.indexOf("\"", quoteStart + 1);
        if (quoteEnd < 0) return defaultValue;
        return json.substring(quoteStart + 1, quoteEnd);
    }

    private static double extractNumber(String json, String key, double defaultValue) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return defaultValue;
        int start = json.indexOf(":", idx) + 1;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-' || json.charAt(end) == 'e' || json.charAt(end) == 'E' || json.charAt(end) == '+' || json.charAt(end) == '-')) {
            end++;
        }
        if (end <= start) return defaultValue;
        try {
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static List<String> extractArray(String json, String key) {
        List<String> result = new ArrayList<>();
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return result;
        int start = json.indexOf("[", idx);
        if (start < 0) return result;
        int end = json.indexOf("]", start);
        if (end < 0) return result;
        
        String arrayContent = json.substring(start + 1, end);
        int braceCount = 0;
        int itemStart = 0;
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;
            if (braceCount == 0 && c == '}') {
                result.add(arrayContent.substring(itemStart, i + 1));
                itemStart = i + 2;
            }
        }
        
        return result;
    }

    private static void appendString(StringBuilder sb, String key, String value) {
        sb.append(JSON_INDENT).append("\"").append(key).append("\": \"").append(value).append("\"\n");
    }

    private static void appendNumber(StringBuilder sb, String key, double value) {
        sb.append(JSON_INDENT).append("\"").append(key).append("\": ").append(value).append("\n");
    }

    public static List<Path> listSavedScenarios() throws IOException {
        Path scenariosDir = Path.of("scenarios");
        if (!Files.exists(scenariosDir)) {
            Files.createDirectories(scenariosDir);
            return Collections.emptyList();
        }
        
        try (Stream<Path> stream = Files.list(scenariosDir)) {
            return stream
                .filter(p -> p.toString().endsWith(".json"))
                .sorted(Comparator.reverseOrder())
                .toList();
        }
    }

    public static void deleteScenario(Path file) throws IOException {
        Files.deleteIfExists(file);
    }

    public record ScenarioLoadResult(ScenarioBuilderData metadata, GameState state) {}
}