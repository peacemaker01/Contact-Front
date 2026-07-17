package com.contactfront.ui;

import com.contactfront.engine.TacticalEngine;
import com.contactfront.engine.data.Profiles;
import com.contactfront.engine.model.CommandMode;
import com.contactfront.engine.model.Doctrine;
import com.contactfront.engine.model.Faction;
import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.Tile;
import com.contactfront.engine.model.Unit;
import com.contactfront.engine.model.UnitProfile;
import com.contactfront.ui.assets.GoogleMapsClient;
import com.contactfront.ui.assets.SatelliteImageProcessor;
import com.contactfront.ui.controller.GameController;
import com.contactfront.ui.view.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.Random;
import javafx.animation.AnimationTimer;

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
    private AnimationTimer gameLoop;
    private static final long TICK_MS = 500;
    private long lastTickNs = 0;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showMainMenu();
    }

    private void showMainMenu() {
        MainMenu menu = new MainMenu(primaryStage, 
            () -> startNewGame(playerFaction, enemyFaction),
            this::doLoad,
            this::showOptions,
            this::showScenarioBuilder,
            this::handleLocationSelection);
        menu.show();
    }

    private void handleLocationSelection(LocationSelector.LocationSelection loc) {
        try {
            // Try to load satellite imagery and create terrain from it
            if (!GoogleMapsClient.getApiKey().isEmpty()) {
                GoogleMapsClient.SatelliteImage satImg = GoogleMapsClient.downloadSatelliteImage(
                    loc.latitude(), loc.longitude(), 16, 512);
                int gameW = 28, gameH = 20;
                SatelliteImageProcessor.SatelliteTerrainData terrainData = 
                    SatelliteImageProcessor.processSatelliteImage(satImg.data(), gameW, gameH);
                
                // Create GameState with satellite-derived terrain
                ctrl = new GameController();
                ctrl.onUpdate = this::refreshAll;
                ctrl.setPlayerFaction(playerFaction);
                ctrl.enemyFaction = enemyFaction;
                
                GameState state = new GameState();
                state.latitude = loc.latitude();
                state.longitude = loc.longitude();
                state.locationName = loc.locationName();
                state.playerFaction = playerFaction;
                state.enemyFaction = enemyFaction;
                state.elevation = terrainData.elevation();
                state.moisture = terrainData.moisture();
                
                // Apply default doctrines
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
                
                // Place units manually since we're bypassing ScenarioGenerator
                int id = 1;
                int px = 2, py = gameH / 2;
                Unit u1 = spawnUnit(state, "mbt", playerFaction, px, py, id++, ctrl.profiles);
                if (u1 != null) { u1.applyDoctrine(Doctrine.NATO); state.friendlyUnits.add(u1); }
                Unit u2 = spawnUnit(state, "ifv", playerFaction, px, py, id++, ctrl.profiles);
                if (u2 != null) { u2.applyDoctrine(Doctrine.NATO); state.friendlyUnits.add(u2); }
                Unit u3 = spawnUnit(state, "inf_squad", playerFaction, px, py, id++, ctrl.profiles);
                if (u3 != null) { u3.applyDoctrine(Doctrine.NATO); state.friendlyUnits.add(u3); }
                
                int ex = gameW - 3, ey = gameH / 2;
                Unit u4 = spawnUnit(state, "mbt", enemyFaction, ex, ey, id++, ctrl.profiles);
                if (u4 != null) { u4.applyDoctrine(Doctrine.RUSSIAN); state.enemyUnits.add(u4); }
                Unit u5 = spawnUnit(state, "inf_squad", enemyFaction, ex, ey, id++, ctrl.profiles);
                if (u5 != null) { u5.applyDoctrine(Doctrine.RUSSIAN); state.enemyUnits.add(u5); }
                
                state.objectives.add(new com.contactfront.engine.model.Objective("OBJ1", "Secure Area", gameW/2, gameH/2, "capture"));
                
                ctrl.state = state;
                ctrl.seed = System.currentTimeMillis();
                ctrl.engine = new TacticalEngine(ctrl.state, new Random(ctrl.seed ^ 0x5151));
                ctrl.engine.start();
                
                showGame();
                return;
            }
        } catch (Exception e) {
            System.err.println("Satellite terrain load failed, falling back to procedural: " + e.getMessage());
        }
        
        // Fallback to procedural terrain
        startNewGame(playerFaction, enemyFaction);
    }
    
    private void showGame() {
        mapView = new MapView(ctrl, 30);
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
        primaryStage.centerOnScreen();
        
        primaryStage.show();

        scene.setOnKeyPressed(e -> handleKey(e));
        startGameLoop();
        refreshAll();
    }
    
    private void startGameLoop() {
        if (gameLoop != null) gameLoop.stop();
        lastTickNs = System.nanoTime();
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsedNs = now - lastTickNs;
                if (elapsedNs >= TICK_MS * 1_000_000) {
                    if (ctrl != null && ctrl.state != null && !ctrl.state.gameOver) {
                        ctrl.engine.tick();
                    }
                    lastTickNs = now;
                }
                mapView.redraw();
            }
        };
        gameLoop.start();
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
        startNewGameWithLocation(playerFaction, enemyFaction, null, null);
    }

    private void startNewGameWithLocation(Faction playerFaction, Faction enemyFaction, Double lat, Double lon) {
        // Generate random location if not provided
        double latitude = lat != null ? lat : (Math.random() * 160 - 80); // -80 to 80 (valid lat range)
        double longitude = lon != null ? lon : (Math.random() * 360 - 180); // -180 to 180 (valid lon range)
        
        try {
            if (!GoogleMapsClient.getApiKey().isEmpty()) {
                // Use satellite terrain for RTS
                int gameW = 28, gameH = 20;
                GoogleMapsClient.SatelliteImage satImg = GoogleMapsClient.downloadSatelliteImage(
                    latitude, longitude, 16, 512);
                SatelliteImageProcessor.SatelliteTerrainData terrainData = 
                    SatelliteImageProcessor.processSatelliteImage(satImg.data(), gameW, gameH);
                
                ctrl = new GameController();
                ctrl.onUpdate = this::refreshAll;
                ctrl.setPlayerFaction(playerFaction);
                ctrl.enemyFaction = enemyFaction;
                
                GameState state = new GameState();
                state.latitude = latitude;
                state.longitude = longitude;
                state.locationName = String.format("%.4f, %.4f", latitude, longitude);
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
                
                // Place units
                int id = 1;
                int px = 2, py = gameH / 2;
                Unit u1 = spawnUnit(state, "mbt", playerFaction, px, py, id++, ctrl.profiles);
                if (u1 != null) { u1.applyDoctrine(Doctrine.NATO); state.friendlyUnits.add(u1); }
                Unit u2 = spawnUnit(state, "ifv", playerFaction, px, py, id++, ctrl.profiles);
                if (u2 != null) { u2.applyDoctrine(Doctrine.NATO); state.friendlyUnits.add(u2); }
                Unit u3 = spawnUnit(state, "inf_squad", playerFaction, px, py, id++, ctrl.profiles);
                if (u3 != null) { u3.applyDoctrine(Doctrine.NATO); state.friendlyUnits.add(u3); }
                
                int ex = gameW - 3, ey = gameH / 2;
                Unit u4 = spawnUnit(state, "mbt", enemyFaction, ex, ey, id++, ctrl.profiles);
                if (u4 != null) { u4.applyDoctrine(Doctrine.RUSSIAN); state.enemyUnits.add(u4); }
                Unit u5 = spawnUnit(state, "inf_squad", enemyFaction, ex, ey, id++, ctrl.profiles);
                if (u5 != null) { u5.applyDoctrine(Doctrine.RUSSIAN); state.enemyUnits.add(u5); }
                
                state.objectives.add(new com.contactfront.engine.model.Objective("OBJ1", "Secure Area", gameW/2, gameH/2, "capture"));
                
                ctrl.state = state;
                ctrl.seed = System.currentTimeMillis();
                ctrl.engine = new TacticalEngine(ctrl.state, new Random(ctrl.seed ^ 0x5151));
                ctrl.engine.start();
                
                showGame();
                return;
            }
        } catch (Exception e) {
            System.err.println("Satellite terrain load failed: " + e.getMessage());
        }
        
        // Fallback to procedural terrain
        ctrl = new GameController();
        ctrl.onUpdate = this::refreshAll;
        ctrl.setPlayerFaction(playerFaction);
        ctrl.enemyFaction = enemyFaction;
        
        ctrl.newGame(System.nanoTime());
        showGame();
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
            if (gameLoop != null) gameLoop.stop();
            sceneRoot.getChildren().clear();
            aarOverlay = null;
            showMainMenu();
        });
        
        Button pauseBtn = new Button("Pause");
        pauseBtn.setStyle("-fx-background-color:#3a5067; -fx-text-fill:#e0e6ed; -fx-border-color:#5a6e82;");
        pauseBtn.setOnAction(e -> {
            if (gameLoop != null) {
                gameLoop.stop();
                gameLoop = null;
            } else {
                startGameLoop();
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
            if (gameLoop != null) gameLoop.stop();
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
        if (aarOverlay != null) { sceneRoot.getChildren().remove(aarOverlay); aarOverlay = null; }
        ctrl.newGame(seed);
        refreshAll();
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
            case S -> { ctrl.beginSmoke(); e.consume(); }
            case A -> { ctrl.beginArty(); e.consume(); }
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
            ctrl.mode = com.contactfront.ui.controller.GameController.Mode.NONE;
            ctrl.staged.clear();
            ctrl.stagedUnits.clear();
            if (aarOverlay != null) { sceneRoot.getChildren().remove(aarOverlay); aarOverlay = null; }
            refreshAll();
        } catch (Exception ex) {
            System.err.println("Load failed: " + ex.getMessage());
        }
    }

    private void showOptions() {
        OptionsDialog dialog = new OptionsDialog(primaryStage, data -> {
            GoogleMapsClient.setApiKey(data.googleMapsApiKey());
            GoogleMapsClient.setCacheDir(Path.of("cache/maps"));
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
        ctrl.onUpdate = this::refreshAll;
        ctrl.state = data.state;
        ctrl.state.mode = "scenario_editor";
        ctrl.state.commandMode = data.commandMode();
        ctrl.state.playerFaction = data.playerFaction();
        ctrl.state.enemyFaction = data.enemyFaction();
        ctrl.state.factionDoctrines.put(data.playerFaction(), data.playerDoctrine());
        ctrl.state.factionDoctrines.put(data.enemyFaction(), data.enemyDoctrine());
        ctrl.engine = new TacticalEngine(ctrl.state, new Random());
        ctrl.engine.start();
        startScenarioGame();
    }

    private void startScenarioGame() {
        mapView = new MapView(ctrl, 30);
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
        primaryStage.centerOnScreen();

        primaryStage.show();

        scene.setOnKeyPressed(e -> handleKey(e));
        startGameLoop();
        refreshAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}