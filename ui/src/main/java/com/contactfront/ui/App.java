package com.contactfront.ui;

import com.contactfront.engine.TacticalEngine;
import com.contactfront.engine.TickProcessor;
import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.terrain.GeoScenarioProvider;
import com.contactfront.engine.model.CommandMode;
import com.contactfront.engine.model.Doctrine;
import com.contactfront.engine.model.Faction;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.Tile;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.UnitProfile;
import com.contactfront.ui.assets.ElevationClient;
import com.contactfront.ui.assets.MapTilerClient;
import com.contactfront.ui.assets.MapTilerClient.SatelliteImage;
import com.contactfront.ui.assets.OverpassApiClient;
import com.contactfront.ui.assets.OverpassApiClient.OsmData;
import com.contactfront.ui.assets.SatelliteImageProcessor;
import com.contactfront.ui.controller.GameController;
import com.contactfront.ui.view.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.Random;

public class App extends Application {
    private GameController ctrl;
    private MapView mapView;
    private SidePanel sidePanel;
    private TopStatus topStatus;
    private EventLog eventLog;
    private ContactsPanel contacts;
    private final StackPane sceneRoot = new StackPane();
    private Node aarOverlay;
    private Stage primaryStage;
    private Faction playerFaction = Faction.USA;
    private Faction enemyFaction = Faction.RUSSIA;
    private TickProcessor tickProcessor;
    private static final long TICK_MS = 500;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showMainMenu();
    }

    private void showMainMenu() {
        Log.info("Showing main menu");
        MainMenu menu = new MainMenu(primaryStage, 
            () -> { Log.info("New Game clicked"); startNewGame(playerFaction, enemyFaction); },
            this::doLoad,
            this::showOptions,
            this::showScenarioBuilder,
            this::handleLocationSelection);
        menu.show();
    }

    private void handleLocationSelection(LocationSelector.LocationSelection loc) {
        Log.info("App: Location selection made - " + loc);
        String savedKey = MapTilerClient.getApiKey();
        if (savedKey.isEmpty()) {
            Log.error("App: MapTiler API key required - showing error");
            System.err.println("MapTiler API key required for real-world tactical maps. Configure in Options.");
            return;
        }

        try {
            Log.info("App: Loading real-world location via GeoScenarioProvider...");
            long locationSeed = System.currentTimeMillis();
            var provider = new GeoScenarioProvider(locationSeed, 28, 20, playerFaction, enemyFaction);
            var result = provider.generate();
            var bbox = result.boundingBox();
            Log.info("App: Selected location: " + result.location().name() + " bbox=" + bbox.minLat() + "," + bbox.minLon() + " - " + bbox.maxLat() + "," + bbox.maxLon());

            int[] imgDims = provider.calculateImageDimensions(16);
            Log.info("App: Calculated image dimensions: " + imgDims[0] + "x" + imgDims[1]);

            SatelliteImage satImg = MapTilerClient.fetchCachedSatelliteImage(
                bbox.minLat(), bbox.minLon(), bbox.maxLat(), bbox.maxLon(),
                imgDims[0], imgDims[1]);
            Log.info("App: Satellite image loaded: " + satImg.data().length + " bytes");

            SatelliteImageProcessor.SatelliteTerrainData terrainData =
                SatelliteImageProcessor.processSatelliteImage(satImg.data(), 28, 20);
            Log.info("App: Satellite terrain processed");

            OsmData osmData = OverpassApiClient.fetchBbox(bbox.minLat(), bbox.minLon(), bbox.maxLat(), bbox.maxLon());
            Log.info("App: OSM data fetched: " + osmData.roads().size() + " roads, " + osmData.buildings().size() + " buildings, " + osmData.forests().size() + " forests");

            ctrl = new GameController();
            ctrl.profiles = Profiles.load();
            ctrl.onUpdate = this::refreshAll;
            ctrl.setPlayerFaction(playerFaction);
            ctrl.enemyFaction = enemyFaction;

            int gameW = 28, gameH = 20;
            GameState state = new GameState();
            state.latitude = bbox.centerLat();
            state.longitude = bbox.centerLon();
            state.locationName = result.location().name();
            state.satelliteImageData = satImg.data();
            state.playerFaction = playerFaction;
            state.enemyFaction = enemyFaction;
            state.elevation = terrainData.elevation();
            state.moisture = terrainData.moisture();
            state.commandMode = CommandMode.DOCTRINE;
            state.factionDoctrines.put(playerFaction, Doctrine.NATO);
            state.factionDoctrines.put(enemyFaction, Doctrine.RUSSIAN);

            Tile[][] grid = new Tile[gameH][gameW];
            Terrain[][] classedTerrain = terrainData.terrain();
            for (int y = 0; y < gameH; y++) {
                for (int x = 0; x < gameW; x++) {
                    grid[y][x] = new Tile(classedTerrain[y][x], x, y);
                }
            }
            state.grid = grid;
            state.ensureVisibility();

            com.contactfront.ui.assets.OsmSemanticGrid.apply(state,
                osmData.roads(), osmData.buildings());
            com.contactfront.ui.assets.OsmSemanticGrid.applyForests(state, osmData.forests());
            state.roadSegments.addAll(osmData.roads());
            state.buildings.addAll(osmData.buildings());

            placeUnits(state, ctrl);

            state.objectives.add(new com.contactfront.engine.model.Objective("OBJ1", "Secure Area", gameW/2, gameH/2, "capture"));

            ctrl.state = state;
            ctrl.seed = locationSeed;
            ctrl.engine = new TacticalEngine(ctrl.state, new Random(locationSeed ^ 0x5151));
            ctrl.engine.start();

            showGame();
        } catch (Exception e) {
            System.err.println("Real-world location load failed: " + e.getMessage());
        }
    }
    
    private void showGame() {
        Log.info("Showing game - state: " + (ctrl.state != null ? "ready w=" + ctrl.state.width() : "null"));
        mapView = new MapView(ctrl, 30);
        if (ctrl.state != null && ctrl.state.satelliteImageData != null && !MapTilerClient.getApiKey().isEmpty()) {
            try {
                mapView.setSatelliteImage(new javafx.scene.image.Image(
                    new java.io.ByteArrayInputStream(ctrl.state.satelliteImageData)));
            } catch (Exception e) {
                Log.error("Failed to load satellite image for toggle: " + e.getMessage());
            }
        }
        sidePanel = new SidePanel(ctrl);
        topStatus = new TopStatus();
        eventLog = new EventLog();
        contacts = new ContactsPanel();

        ScrollPane mapScroll = mapView.scrollPane();
        mapScroll.setStyle("-fx-background-color:#0e1117;");

        VBox right = new VBox(sidePanel.node(), contacts.node());

        BorderPane bp = new BorderPane();
        bp.setTop(buildTopBar());
        bp.setCenter(mapScroll);
        bp.setRight(right);
        bp.setBottom(eventLog.node());
        bp.setPadding(new Insets(8));
        bp.setStyle("-fx-background-color:#0e1117;");

        sceneRoot.getChildren().clear();
        sceneRoot.getChildren().add(bp);
        Scene scene = new Scene(sceneRoot);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Contact Front — Real-Time Tactical Mode");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        mapView.redraw();
        scene.setOnKeyPressed(e -> handleKey(e));
        startTickProcessor();
        refreshAll();
    }
    
    private void startTickProcessor() {
        if (tickProcessor != null) {
            tickProcessor.stop();
        }
        
        tickProcessor = new TickProcessor(ctrl.engine, TICK_MS, new TickProcessor.TickListener() {
            @Override
            public void onTick(long tickNumber, long elapsedMs) {
                Platform.runLater(App.this::refreshAll);
            }
            
            @Override
            public void onRunning() {
                Log.info("TickProcessor started - tick interval: " + TICK_MS + "ms");
            }
            
            @Override
            public void onStopped() {
                Log.info("TickProcessor stopped");
            }
        });
        tickProcessor.start();
    }
    
    private Unit spawnUnit(GameState s, String profileId, Faction f, int ax, int ay, int id, Profiles profiles) {
        int[] spot = findFreeTile(s, ax, ay);
        if (spot == null) return null;
        UnitProfile up = profiles.unit(profileId);
        return new Unit(id, f, up, spot[0], spot[1], profiles);
    }
    
    private int[] findFreeTile(GameState s, int ax, int ay) {
        int best = -1, bestX = 0, bestY = 0;
        for (int y = 0; y < s.height(); y++) {
            for (int x = 0; x < s.width(); x++) {
                Tile t = s.grid[y][x];
                if (t.impassable() || t.type == Terrain.BUILDING) continue;
                if (occupied(s, x, y)) continue;
                int d = Math.abs(x - ax) + Math.abs(y - ay);
                if (best < 0 || d < best) { best = d; bestX = x; bestY = y; }
            }
        }
        return best < 0 ? null : new int[]{bestX, bestY};
    }
    
    private boolean occupied(GameState s, int x, int y) {
        for (Unit u : s.friendlyUnits) if (!u.destroyed && u.x == x && u.y == y) return true;
        for (Unit u : s.enemyUnits) if (!u.destroyed && u.x == x && u.y == y) return true;
        return false;
    }

    private void startNewGame(Faction playerFaction, Faction enemyFaction) {
        Log.info("App: Starting new game (procedural deprecated - using real-world)");
        startNewGameWithSeed(playerFaction, enemyFaction, System.currentTimeMillis());
    }

    private void startNewGameWithSeed(Faction playerFaction, Faction enemyFaction, long seed) {
        Log.info("App: Starting new battle with seed=" + seed);
        ctrl = new GameController();
        ctrl.profiles = Profiles.load();
        ctrl.onUpdate = this::refreshAll;
        ctrl.setPlayerFaction(playerFaction);
        ctrl.enemyFaction = enemyFaction;

        int gameW = 28, gameH = 20;
        int zoom = 16;

        String savedKey = MapTilerClient.getApiKey();
        if (savedKey.isEmpty()) {
            Log.error("App: MapTiler API key required - returning to main menu");
            System.err.println("MapTiler API key required for real-world tactical maps. Configure in Options.");
            showMainMenu();
            return;
        }

        try {
            Log.info("App: Loading real-world location via GeoScenarioProvider...");
            long locationSeed = seed;
            var provider = new GeoScenarioProvider(locationSeed, gameW, gameH, playerFaction, enemyFaction);
            var result = provider.generate();
            var bbox = result.boundingBox();
            Log.info("App: Selected location: " + result.location().name() + " center=" + bbox.centerLat() + "," + bbox.centerLon());

            int[] imgDims = provider.calculateImageDimensions(zoom);
            Log.info("App: Calculated image dimensions: " + imgDims[0] + "x" + imgDims[1]);

            SatelliteImage satImg = MapTilerClient.fetchCachedSatelliteImage(
                bbox.minLat(), bbox.minLon(), bbox.maxLat(), bbox.maxLon(),
                imgDims[0], imgDims[1]);
            Log.info("App: Satellite image: " + satImg.data().length + " bytes");

            SatelliteImageProcessor.SatelliteTerrainData terrainData =
                SatelliteImageProcessor.processSatelliteImage(satImg.data(), gameW, gameH);
            Log.info("App: Terrain baked");

            OsmData osmData = OverpassApiClient.fetchBbox(bbox.minLat(), bbox.minLon(), bbox.maxLat(), bbox.maxLon());
            Log.info("App: OSM: " + osmData.roads().size() + " roads, " + osmData.buildings().size() + " buildings, " + osmData.forests().size() + " forests");

            GameState state = new GameState();
            state.latitude = bbox.centerLat();
            state.longitude = bbox.centerLon();
            state.locationName = result.location().name();
            state.satelliteImageData = satImg.data();
            state.playerFaction = playerFaction;
            state.enemyFaction = enemyFaction;
            state.elevation = terrainData.elevation();
            state.moisture = terrainData.moisture();
            state.commandMode = CommandMode.DOCTRINE;
            state.factionDoctrines.put(playerFaction, Doctrine.NATO);
            state.factionDoctrines.put(enemyFaction, Doctrine.RUSSIAN);

            Tile[][] grid = new Tile[gameH][gameW];
            Terrain[][] classedTerrain = terrainData.terrain();
            for (int y = 0; y < gameH; y++) {
                for (int x = 0; x < gameW; x++) {
                    grid[y][x] = new Tile(classedTerrain[y][x], x, y);
                }
            }
            state.grid = grid;
            state.ensureVisibility();

            com.contactfront.ui.assets.OsmSemanticGrid.apply(state,
                osmData.roads(), osmData.buildings());
            com.contactfront.ui.assets.OsmSemanticGrid.applyForests(state, osmData.forests());
            state.roadSegments.addAll(osmData.roads());
            state.buildings.addAll(osmData.buildings());

            Log.info("App: Grid populated with " + state.grid.length + "x" + state.grid[0].length + " tiles");

            placeUnits(state, ctrl);

            state.objectives.add(new com.contactfront.engine.model.Objective("OBJ1", "Secure Area", gameW/2, gameH/2, "capture"));

            ctrl.state = state;
            ctrl.seed = seed;
            ctrl.engine = new TacticalEngine(ctrl.state, new Random(seed ^ 0x5151));
            ctrl.engine.start();
            Log.info("App: TacticalEngine started");

            showGame();
            Log.info("App: Game UI ready");
        } catch (Exception e) {
            Log.error("App: Real-world location load failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void placeUnits(GameState state, GameController ctrl) {
        Log.info("App: Placing units on tactical grid...");
        int id = 1;
        int gameW = state.width();
        int gameH = state.height();
        int px = 2, py = gameH / 2;
        Unit u1 = spawnUnit(state, "m1a2_abrams", state.playerFaction, px, py, id++, ctrl.profiles);
        if (u1 != null) { Log.info("App: Spawned " + u1.profile.name() + " at " + px + "," + py); u1.applyDoctrine(Doctrine.NATO); state.friendlyUnits.add(u1); }
        Unit u2 = spawnUnit(state, "bradley", state.playerFaction, px, py, id++, ctrl.profiles);
        if (u2 != null) { Log.info("App: Spawned " + u2.profile.name() + " at " + px + "," + py); u2.applyDoctrine(Doctrine.NATO); state.friendlyUnits.add(u2); }
        Unit u3 = spawnUnit(state, "inf_squad", state.playerFaction, px, py, id++, ctrl.profiles);
        if (u3 != null) { Log.info("App: Spawned " + u3.profile.name() + " at " + px + "," + py); u3.applyDoctrine(Doctrine.NATO); state.friendlyUnits.add(u3); }

        int ex = gameW - 3, ey = gameH / 2;
        Unit u4 = spawnUnit(state, "t90m", state.enemyFaction, ex, ey, id++, ctrl.profiles);
        if (u4 != null) { Log.info("App: Spawned " + u4.profile.name() + " at " + ex + "," + ey); u4.applyDoctrine(Doctrine.RUSSIAN); state.enemyUnits.add(u4); }
        Unit u5 = spawnUnit(state, "motostrelki", state.enemyFaction, ex, ey, id++, ctrl.profiles);
        if (u5 != null) { Log.info("App: Spawned " + u5.profile.name() + " at " + ex + "," + ey); u5.applyDoctrine(Doctrine.RUSSIAN); state.enemyUnits.add(u5); }
        Log.info("App: Unit placement complete");
    }

    private Node buildTopBar() {
        TextField seedField = new TextField(Long.toString(new Random().nextLong()));
        seedField.setPrefWidth(140);
        seedField.setStyle("-fx-background-color:#151a23; -fx-text-fill:#e0e6ed; -fx-border-color:#3a5067; -fx-border-width:1;");
        Button newBtn = new Button("New Battle");
        newBtn.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82;");
        newBtn.setOnAction(e -> {
            long seed;
            try { seed = Long.parseLong(seedField.getText().trim()); }
            catch (NumberFormatException ex) { seed = new Random().nextLong(); }
            newBattle(seed);
        });

        Button menuBtn = new Button("Main Menu");
        menuBtn.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82;");
        menuBtn.setOnAction(e -> {
            if (tickProcessor != null) tickProcessor.stop();
            sceneRoot.getChildren().clear();
            aarOverlay = null;
            showMainMenu();
        });
        
        Button pauseBtn = new Button("Pause");
        pauseBtn.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82;");
        pauseBtn.setOnAction(e -> {
            if (tickProcessor != null && tickProcessor.isRunning()) {
                tickProcessor.stop();
            } else {
                tickProcessor.resume();
            }
        });

        MenuBar menu = new MenuBar();
        Menu game = new Menu("Game");
        MenuItem save = new MenuItem("Save...");
        save.setOnAction(e -> doSave());
        MenuItem load = new MenuItem("Load...");
        load.setOnAction(e -> doLoad());
        MenuItem exitToMenu = new MenuItem("Exit to Main Menu");
        exitToMenu.setOnAction(e -> {
            if (tickProcessor != null) tickProcessor.stop();
            sceneRoot.getChildren().clear();
            aarOverlay = null;
            showMainMenu();
        });
        game.getItems().addAll(save, load, new MenuItem("---"), exitToMenu);
        menu.getMenus().add(game);

        HBox bar = new HBox(10, menuBtn, pauseBtn, new Label("Seed:"), seedField, newBtn, menu,
                new Label("  L-drag=select · R-click=order · wheel=zoom · 1-9=groups (Ctrl+#=assign) · S=smoke · A=arty"));
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 8, 4, 8));
        bar.setStyle("-fx-background-color:#151a23; -fx-border-color:#3a5067;");

        VBox top = new VBox(0, topStatus.node(), bar);
        top.setStyle("-fx-background-color:#0e1117;");
        return top;
    }

    private void newBattle(long seed) {
        if (tickProcessor != null) tickProcessor.stop();
        if (aarOverlay != null) { sceneRoot.getChildren().remove(aarOverlay); aarOverlay = null; }
        startNewGameWithSeed(playerFaction, enemyFaction, seed);
    }

    private void refreshAll() {
        mapView.redraw();
        sidePanel.update(ctrl.state);
        topStatus.update(ctrl.state);
        eventLog.update(ctrl.state);
        contacts.update(ctrl.state);
        if (ctrl.state != null && ctrl.state.gameOver) showAar();
    }

    private void handleKey(KeyEvent e) {
        if (ctrl.state == null || ctrl.state.gameOver) return;
        String name = e.getCode().name();
        int n = -1;
        if (name.startsWith("DIGIT")) n = Integer.parseInt(name.substring(5));
        else if (name.startsWith("NUMPAD")) n = Integer.parseInt(name.substring(7));
        if (n >= 1 && n <= 9) {
            if (e.isControlDown()) ctrl.assignGroup(n); else ctrl.recallGroup(n);
            e.consume();
            return;
        }
        switch (e.getCode()) {
            case EQUALS, ADD, PLUS -> { mapView.setZoom(mapView.getZoom() * 1.1); e.consume(); }
            case MINUS, SUBTRACT -> { mapView.setZoom(mapView.getZoom() * 0.9); e.consume(); }
            case ESCAPE -> { ctrl.clearSelection(); e.consume(); }
        }
    }

    private void showAar() {
        if (aarOverlay != null) return;
        aarOverlay = AfterActionReport.create(ctrl.state, () -> newBattle(new Random().nextLong()));
        sceneRoot.getChildren().add(aarOverlay);
    }

    private void doSave() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Battle");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File f = fc.showSaveDialog(primaryStage);
        if (f == null) return;
        try {
            SaveManager.save(ctrl.state, ctrl.profiles, ctrl.seed, f);
        } catch (Exception ex) {
            ctrl.state.log("Save failed: " + ex.getMessage());
        }
    }

    private void doLoad() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load Battle");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File f = fc.showOpenDialog(primaryStage);
        if (f == null) return;
        try {
            SaveManager.Loaded loaded = SaveManager.load(f, Profiles.load());
            ctrl.state = loaded.state();
            ctrl.seed = loaded.seed();
            ctrl.engine = new TacticalEngine(loaded.state(), new Random(loaded.seed() ^ 0x5151));
            ctrl.selected = null;
            ctrl.selection.clear();
            if (aarOverlay != null) { sceneRoot.getChildren().remove(aarOverlay); aarOverlay = null; }
            ctrl.engine.start();
            startTickProcessor();
            refreshAll();
        } catch (Exception ex) {
            System.err.println("Load failed: " + ex.getMessage());
        }
    }

    private void showOptions() {
        Log.info("Opening options dialog");
        OptionsDialog dialog = new OptionsDialog(primaryStage, data -> {
            MapTilerClient.setApiKey(data.mapTilerApiKey());
            MapTilerClient.setCacheDir(Path.of("cache/maps"));
            Log.info("Options saved - API key: " + (data.mapTilerApiKey().isEmpty() ? "none" : "***"));
        });
        dialog.show();
    }

    private void showScenarioBuilder() {
        GameController editorCtrl = new GameController();
        ScenarioEditor editor = new ScenarioEditor(primaryStage, editorCtrl, data -> {
            handleScenarioCreated(data);
        });
        editor.show();
    }

    private void handleScenarioCreated(ScenarioEditor.ScenarioEditorData data) {
        ctrl = new GameController();
        ctrl.profiles = Profiles.load();
        ctrl.onUpdate = this::refreshAll;
        
        Faction playerFaction = this.playerFaction;
        Faction enemyFaction = this.enemyFaction;
        
        ctrl.state = data.state();
        ctrl.state.mode = "scenario_editor";
        ctrl.state.commandMode = data.commandMode();
        ctrl.state.playerFaction = playerFaction;
        ctrl.state.enemyFaction = enemyFaction;
        ctrl.state.factionDoctrines.put(playerFaction, data.playerDoctrine());
        ctrl.state.factionDoctrines.put(enemyFaction, data.enemyDoctrine());
        
        for (Unit u : ctrl.state.placedUnits) {
            if (u.faction == playerFaction) {
                ctrl.state.friendlyUnits.add(u);
            } else {
                ctrl.state.enemyUnits.add(u);
            }
        }
        ctrl.state.placedUnits.clear();
        
        ctrl.engine = new TacticalEngine(ctrl.state, new Random());
        ctrl.engine.start();
        startScenarioGame();
    }

    private void startScenarioGame() {
        mapView = new MapView(ctrl, 30);
        if (ctrl.state != null && ctrl.state.satelliteImageData != null && !MapTilerClient.getApiKey().isEmpty()) {
            try {
                mapView.setSatelliteImage(new javafx.scene.image.Image(
                    new java.io.ByteArrayInputStream(ctrl.state.satelliteImageData)));
            } catch (Exception e) {
                Log.error("Failed to load satellite image for toggle: " + e.getMessage());
            }
        }
        sidePanel = new SidePanel(ctrl);
        topStatus = new TopStatus();
        eventLog = new EventLog();
        contacts = new ContactsPanel();

        ScrollPane mapScroll = mapView.scrollPane();
        mapScroll.setStyle("-fx-background-color:#0e1117;");

        VBox right = new VBox(sidePanel.node(), contacts.node());

        BorderPane bp = new BorderPane();
        bp.setTop(buildTopBar());
        bp.setCenter(mapScroll);
        bp.setRight(right);
        bp.setBottom(eventLog.node());
        bp.setPadding(new Insets(8));
        bp.setStyle("-fx-background-color:#0e1117;");

        sceneRoot.getChildren().clear();
        sceneRoot.getChildren().add(bp);
        Scene scene = new Scene(sceneRoot);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Contact Front — Scenario Game");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        mapView.redraw();
        scene.setOnKeyPressed(e -> handleKey(e));
        startTickProcessor();
        refreshAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}