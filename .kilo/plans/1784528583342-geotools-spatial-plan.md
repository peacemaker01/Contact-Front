# GeoTools Spatial Systems Integration Plan

## Current State
- `engine/geo/` package created with `CoordinateConverter` (direct Web Mercator math), `RangeCalculator`, `UnitFeatureManager`, `TerrainRasterLoader`.
- `engine/pom.xml` has `gt-referencing`, `gt-main`, `gt-metadata`, `gt-coverage`, `gt-geotiff`, `gt-epsg-hsql` (all v30.1).
- 109 engine tests pass.
- `ui/assets/` already has `MapTilerClient`, `OverpassApiClient`, `OsmSemanticGrid`, `SatelliteImageProcessor` — but they are UI-layer, not engine-pure, and have no spatial indexing.
- No JTS `STRtree` usage anywhere.
- No tile fetch/stitch pipeline beyond single-image `MapTilerClient`.
- No global XY-axis enforcement for GeoTools.
- No explicit render/physics tick separation.

## Goal
Finish the 5-service architecture from the technical guide:
1. Project Referencing Service — coordinate isolation + XY safety
2. Vector Physics World — OSM → metric JTS shapes + STRtree indexes
3. Visual Render Pipeline — tile fetch/cache/stitch in `ui/`
4. Scenario Serializer/UI Controller — drag-drop + JSON
5. Interactive Tactical Loop — JTS intersection for LOS/trafficability

## Decisions

1. **XY axis order**: add `System.setProperty("org.geotools.referencing.forceXY", "true")` in `App` startup before any GeoTools decode. This makes all `CRS.decode("EPSG:4326")` return lon-first, eliminating swap bugs.
2. **Coordinate isolation**: `GameState` stores WGS84 lat/lon. All JTS ops happen in projected metric via `CoordinateConverter`. No rule class may call `JTS.orthodromicDistance` directly; they must go through `RangeCalculator`.
3. **Spatial index ownership**: `STRtree` indexes live in `engine/` because LOS/trafficability are engine concerns. OSM parsing stays in `ui/assets/` for network I/O, but it returns plain JTS geometries; the engine builds the index from them.
4. **Tile pipeline**: stays in `ui/assets/`. Fetch 256×256 tiles, cache to `cache/maps/`, stitch on a background `Task`/thread. Do not block the `AnimationTimer` game loop.
5. **Feature schema**: keep `TacticalUnit` SimpleFeatureType in `engine/geo/` for the scenario editor only. Runtime `Unit` model is unchanged.

## Task List

### Service 1 — Project Referencing
- [ ] Add `System.setProperty("org.geotools.referencing.forceXY", "true")` in `App` init.
- [ ] Simplify `CoordinateConverter` back to `CRS.findMathTransform` + `JTS.transform` now that XY order is guaranteed. Remove direct-math fallback.
- [ ] Add `CoordinateReferenceService` wrapper if needed for testability.

### Service 2 — Vector Physics World
- [ ] Create `engine/src/main/java/com/contactfront/engine/geo/SpatialIndex.java`:
  - `STRtree` for buildings, roads, forests.
  - `index(GameState)` method that ingests OSM-derived shapes and builds the tree.
  - `query(Envelope)` / `query(Point)` helpers.
- [ ] Update `ui/assets/OverpassApiClient` or add engine-side adapter to convert OSM JSON → JTS `Polygon`/`LineString` in EPSG:4326.
- [ ] Update `Movement.java` / `LineOfSight.java` to use `SpatialIndex` for trafficability and LOS checks instead of scanning `Tile[][]` only.

### Service 3 — Visual Render Pipeline
- [ ] Create `ui/src/main/java/com/contactfront/ui/assets/TileFetcher.java`:
  - Slippy-map URL template for MapTiler.
  - Async fetch with `java.net.http.HttpClient`.
  - Disk cache under `cache/maps/` keyed by `z/x/y`.
- [ ] Create `ui/src/main/java/com/contactfront/ui/assets/TileStitcher.java`:
  - Given viewport bbox + zoom, fetch needed tiles, stitch into a single `BufferedImage`.
  - Run on background thread; update `ImageView` on JavaFX FX thread.
- [ ] Wire into `MapView` so satellite view uses stitched tiles instead of single static image.

### Service 4 — Scenario Serializer / UI Controller
- [ ] Confirm `ScenarioSerializer` covers full mission state: map location, unit placements with faction/SIDC/heading, objectives.
- [ ] Confirm `ScenarioEditor` uses `UnitFeatureManager` for drag-drop placement.
- [ ] Add save/load buttons in `ScenarioEditor` that call `ScenarioSerializer`.

### Service 5 — Interactive Tactical Loop
- [ ] Add `JTS.intersects` / `JTS.within` checks in `Combat.java` for road trafficability when `GameState` has projected geometries.
- [ ] Add `JTS.orthodromicDistance` path in `Visibility.java` for long-range LOS when lat/lon bounds are set.
- [ ] Ensure `AiTurn` uses `SpatialIndex` for target acquisition instead of `enemyUnits` linear scan.

## Validation
- All existing tests continue to pass.
- New tests: `CoordinateConverterTest` round-trip, `SpatialIndexTest` query correctness, `TileFetcherTest` cache hit/miss, `ScenarioSerializerTest` full mission round-trip.
- `mvn test` green in both `engine` and `ui` modules.
- EXE rebuilt and launches.

## Out of Scope
- 3D extrusion, LWJGL, PBR.
- LLM/NLP order parsing.
- Strategic mode.
- Steam/networking.
- Replacing runtime `Unit` model with `SimpleFeature`.
