package com.contactfront.ui;

import com.contactfront.engine.model.*;
import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.trigger.TriggerNode;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioSerializerTest {

    @TempDir
    Path tempDir;

    @Test
    void saveCreatesValidJson() throws IOException {
        GameState state = new GameState();
        state.scenarioId = "test_scenario";
        state.playerFaction = Faction.USA;
        state.enemyFaction = Faction.RUSSIA;

        File file = tempDir.resolve("test.json").toFile();
        ScenarioSerializer.save(state, file);

        assertTrue(file.exists());
        String content = java.nio.file.Files.readString(file.toPath());
        JSONObject json = new JSONObject(content);
        assertTrue(json.has("scenario_metadata"));
        assertEquals("test_scenario", json.getJSONObject("scenario_metadata").getString("name"));
    }

    @Test
    void saveIncludesUnitPlacements() throws IOException {
        GameState state = new GameState();
        state.scenarioId = "units_test";
        state.playerFaction = Faction.USA;
        state.enemyFaction = Faction.RUSSIA;

        // Create a minimal grid
        state.grid = new Tile[][]{
            {new Tile(Terrain.OPEN, 0, 0)},
            {new Tile(Terrain.OPEN, 0, 1)}
        };
        state.ensureVisibility();

        Profiles profiles = Profiles.load();
        UnitProfile profile = profiles.unit("inf_squad");
        Unit unit = new Unit(1, Faction.USA, profile, 0, 0, profiles);
        unit.sidcCode = "SFGPU------****";
        state.friendlyUnits.add(unit);

        File file = tempDir.resolve("units.json").toFile();
        ScenarioSerializer.save(state, file);

        String content = java.nio.file.Files.readString(file.toPath());
        JSONObject json = new JSONObject(content);
        assertTrue(json.has("unit_placements"));
        assertEquals(1, json.getJSONArray("unit_placements").length());
    }

    @Test
    void saveIncludesObstacles() throws IOException {
        GameState state = new GameState();
        state.scenarioId = "obs_test";
        state.playerFaction = Faction.USA;
        state.enemyFaction = Faction.RUSSIA;

        state.grid = new Tile[][]{
            {new Tile(Terrain.OPEN, 0, 0)}
        };
        state.ensureVisibility();

        state.obstacles.add(new Obstacle(2, 3, Obstacle.ObstacleType.MINEFIELD));

        File file = tempDir.resolve("obstacles.json").toFile();
        ScenarioSerializer.save(state, file);

        String content = java.nio.file.Files.readString(file.toPath());
        JSONObject json = new JSONObject(content);
        assertTrue(json.has("obstacles"));
        assertEquals(1, json.getJSONArray("obstacles").length());
    }

    @Test
    void saveIncludesTacticalGraphics() throws IOException {
        GameState state = new GameState();
        state.scenarioId = "graph_test";
        state.playerFaction = Faction.USA;
        state.enemyFaction = Faction.RUSSIA;

        state.grid = new Tile[][]{
            {new Tile(Terrain.OPEN, 0, 0)}
        };
        state.ensureVisibility();

        state.tacticalGraphics.addLine(TacticalGraphics.GraphicType.PHASE_LINE, java.util.List.of(
            new double[]{0, 0},
            new double[]{5, 5}
        ));

        File file = tempDir.resolve("graphics.json").toFile();
        ScenarioSerializer.save(state, file);

        String content = java.nio.file.Files.readString(file.toPath());
        JSONObject json = new JSONObject(content);
        assertTrue(json.has("tactical_graphics"));
        assertEquals(1, json.getJSONArray("tactical_graphics").length());
    }

    @Test
    void saveIncludesTriggers() throws IOException {
        GameState state = new GameState();
        state.scenarioId = "trigger_test";
        state.playerFaction = Faction.USA;
        state.enemyFaction = Faction.RUSSIA;

        state.grid = new Tile[][]{
            {new Tile(Terrain.OPEN, 0, 0)}
        };
        state.ensureVisibility();

        TriggerNode node = new TriggerNode("t1", TriggerNode.NodeType.CONDITION, 0, 0, "Test trigger");
        state.triggers.addNode(node);

        File file = tempDir.resolve("triggers.json").toFile();
        ScenarioSerializer.save(state, file);

        String content = java.nio.file.Files.readString(file.toPath());
        JSONObject json = new JSONObject(content);
        assertTrue(json.has("triggers"));
    }

    @Test
    void saveIncludesEnvironment() throws IOException {
        GameState state = new GameState();
        state.scenarioId = "env_test";
        state.isNight = true;
        state.isRaining = false;
        state.isWindy = true;

        state.grid = new Tile[][]{
            {new Tile(Terrain.OPEN, 0, 0)}
        };
        state.ensureVisibility();

        File file = tempDir.resolve("env.json").toFile();
        ScenarioSerializer.save(state, file);

        String content = java.nio.file.Files.readString(file.toPath());
        JSONObject json = new JSONObject(content);
        assertTrue(json.has("environment"));
        JSONObject env = json.getJSONObject("environment");
        assertTrue(env.getBoolean("isNight"));
        assertFalse(env.getBoolean("isRaining"));
        assertTrue(env.getBoolean("isWindy"));
    }

    @Test
    void sidcCodeIsSavedForUnits() throws IOException {
        GameState state = new GameState();
        state.scenarioId = "sidc_test";
        state.playerFaction = Faction.USA;
        state.enemyFaction = Faction.RUSSIA;

        state.grid = new Tile[][]{
            {new Tile(Terrain.OPEN, 0, 0)}
        };
        state.ensureVisibility();

        Profiles profiles = Profiles.load();
        UnitProfile profile = profiles.unit("inf_squad");
        Unit unit = new Unit(1, Faction.USA, profile, 0, 0, profiles);
        unit.sidcCode = "SFGPU------****";
        state.friendlyUnits.add(unit);

        File file = tempDir.resolve("sidc.json").toFile();
        ScenarioSerializer.save(state, file);

        String content = java.nio.file.Files.readString(file.toPath());
        JSONObject json = new JSONObject(content);
        assertEquals("SFGPU------****",
            json.getJSONArray("unit_placements").getJSONObject(0).getString("sidc"));
    }

    @Test
    void missingSidcDefaultsToRegistry() throws IOException {
        GameState state = new GameState();
        state.scenarioId = "default_sidc_test";
        state.playerFaction = Faction.USA;
        state.enemyFaction = Faction.RUSSIA;

        state.grid = new Tile[][]{
            {new Tile(Terrain.OPEN, 0, 0)}
        };
        state.ensureVisibility();

        Profiles profiles = Profiles.load();
        UnitProfile profile = profiles.unit("inf_squad");
        Unit unit = new Unit(1, Faction.USA, profile, 0, 0, profiles);
        // No sidcCode set - should default from registry
        state.friendlyUnits.add(unit);

        File file = tempDir.resolve("default_sidc.json").toFile();
        ScenarioSerializer.save(state, file);

        String content = java.nio.file.Files.readString(file.toPath());
        JSONObject json = new JSONObject(content);
        String sidc = json.getJSONArray("unit_placements").getJSONObject(0).getString("sidc");
        assertNotNull(sidc);
        assertFalse(sidc.isEmpty());
    }
}