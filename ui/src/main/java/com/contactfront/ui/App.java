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
import com.contactfront.engine.terrain.SatelliteClassifier;
import com.contactfront.ui.assets.GoogleMapsClient;
import com.contactfront.ui.assets.ScenarioSerializer;
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
import javafx.stage.Screen;
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

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showMainMenu();
    }

    private void showMainMenu() {
        MainMenu menu = new MainMenu(primaryStage, 
            () -> startNewGame(playerFaction, enemyFaction),
            faction -> {
                playerFaction = faction;
                showEnemySelect(faction);
            },
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
        primaryStage.setTitle("Contact Front — Tactical Mode (Satellite Terrain)");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(600);
        primaryStage.centerOnScreen();
        
        primaryStage.show();

        scene.setOnKeyPressed(e -> handleKey(e));
        refreshAll();
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

    private void showEnemySelect(Faction playerFaction) {
        EnemySelectDialog dialog = new EnemySelectDialog(primaryStage, playerFaction, this::startNewGame);
        dialog.show();
    }

    private void startNewGame(Faction playerFaction, Faction enemyFaction) {
        ctrl = new GameController();
        ctrl.onUpdate = this::refreshAll;
        ctrl.setPlayerFaction(playerFaction);
        ctrl.enemyFaction = enemyFaction;

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

        sceneRoot.getChildren().add(bp);
        Scene scene = new Scene(sceneRoot);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Contact Front — Tactical Mode");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(600);
        primaryStage.centerOnScreen();
        
        primaryStage.show();

        scene.setOnKeyPressed(e -> handleKey(e));
        newBattle(new Random().nextLong());
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
            sceneRoot.getChildren().clear();
            aarOverlay = null;
            showMainMenu();
        });

        MenuBar menu = new MenuBar();
        Menu game = new Menu("Game");
        MenuItem save = new MenuItem("Save...");
        save.setOnAction(e -> doSave());
        MenuItem load = new MenuItem("Load...");
        load.setOnAction(e -> doLoad());
        MenuItem exitToMenu = new MenuItem("Exit to Main Menu");
        exitToMenu.setOnAction(e -> {
            sceneRoot.getChildren().clear();
            aarOverlay = null;
            showMainMenu();
        });
        game.getItems().addAll(save, load, new MenuItem("---"), exitToMenu);
        menu.getMenus().add(game);

        HBox bar = new HBox(10, menuBtn, new Label("Seed:"), seedField, newBtn, menu,
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
        sidePanel.update();
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
        ScenarioBuilder builder = new ScenarioBuilder(primaryStage, this::handleScenarioCreated, terrain -> {
            ctrl.state.log("Terrain selected: " + terrain.name());
        });
        builder.show();
    }

    private void handleScenarioCreated(ScenarioBuilder.ScenarioBuilderData data) {
        ctrl.state.latitude = data.latitude();
        ctrl.state.longitude = data.longitude();
        ctrl.state.locationName = data.locationName();
        ctrl.state.mode = "scenario_builder";
        ctrl.state.commandMode = data.commandMode();
        ctrl.state.factionDoctrines.put(data.playerFaction(), data.playerDoctrine());
        ctrl.state.factionDoctrines.put(data.enemyFaction(), data.enemyDoctrine());
        startNewGame(data.playerFaction(), data.enemyFaction());
    }

    private void saveScenario() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Scenario");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Scenario Files", "*.json"));
        fc.setInitialFileName("new_scenario.json");
        File f = fc.showSaveDialog(primaryStage);
        if (f == null) return;
        try {
            ScenarioSerializer.saveScenario(
                new ScenarioBuilder.ScenarioBuilderData(
                    "Saved Scenario", "From editor", 
                    ctrl.state.playerFaction, ctrl.state.enemyFaction,
                    ctrl.state.commandMode,
                    ctrl.state.factionDoctrines.getOrDefault(ctrl.state.playerFaction, Doctrine.NATO),
                    ctrl.state.factionDoctrines.getOrDefault(ctrl.state.enemyFaction, Doctrine.RUSSIAN),
                    ctrl.state.locationName, ctrl.state.latitude, ctrl.state.longitude,
                    ctrl.state.width(), ctrl.state.height(), ""
                ), ctrl.state, f.toPath());
        } catch (Exception ex) {
            ctrl.state.log("Save failed: " + ex.getMessage());
        }
    }

    private void loadScenario() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load Scenario");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Scenario Files", "*.json"));
        File f = fc.showOpenDialog(primaryStage);
        if (f == null) return;
        try {
            ScenarioSerializer.ScenarioLoadResult result = ScenarioSerializer.loadScenario(f.toPath());
            ctrl.state = result.state();
            ctrl.state.playerFaction = result.metadata().playerFaction();
            ctrl.state.enemyFaction = result.metadata().enemyFaction();
            ctrl.engine = new TacticalEngine(ctrl.state, new Random());
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

    public static void main(String[] args) {
        launch(args);
    }
}