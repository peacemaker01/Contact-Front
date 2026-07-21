# GeoTools Spatial Systems Integration Plan

## Current State
- `engine/pom.xml` already includes `gt-referencing` and `gt-main` (v30.1).
- `CoordinateConversionTest.java` exists but uses hand-rolled Mercator math; no GeoTools runtime usage yet.
- `GeoScenarioProvider.java` already selects real locations and sets `GameState.latitude/longitude`.
- `GameState` already carries `latitude`, `longitude`, `elevation[][]`, `satelliteImageData`.
- Rules (`Combat`, `Movement`, `Visibility`) use grid-based Manhattan/Chebyshev distance only.
- 117 tests pass. No `geo/` package exists.

## Goal
Introduce a pure-engine `geo/` package that uses GeoTools to translate between WGS84 and metric space on the fly, replacing ad-hoc coordinate math and enabling real-world distance/range calculations.

## Decisions

1. **Package location**: `engine/src/main/java/com/contactfront/engine/geo/` — pure engine, no JavaFX.
2. **Canonical CRS**: WGS84 (`EPSG:4326`) in save files/scenario state. Convert to `EPSG:3857` for runtime metric math.
3. **Feature store**: `UnitFeatureManager` wraps `MemoryDataStore` for scenario-builder editing only; does not replace runtime `Unit` model.
4. **Raster path**: `TerrainRasterLoader` reads GeoTIFF DEM via `gt-coverage`; existing `SatelliteImageProcessor` stays for RGB texture.
5. **Viewport**: `ReferencedEnvelope` helper in `ui/` only; no viewport state in `GameState`.
6. **Dependency additions**: add `gt-metadata`, `gt-coverage`, and `gt-epsg-hsql` to `engine/pom.xml`.

## Task List

1. **Add GeoTools deps to `engine/pom.xml`**
   - `gt-metadata:30.1`, `gt-coverage:30.1`, `gt-epsg-hsql:30.1`

2. **Create `engine/src/main/java/com/contactfront/engine/geo/CoordinateConverter.java`**
   - `CRS.decode("EPSG:4326")` → `CRS.decode("EPSG:3857")`
   - `project(Geometry)` / `unproject(Geometry)` via `JTS.transform`
   - Cache `MathTransform`; throw clear error if GeoTools not on classpath

3. **Create `engine/src/main/java/com/contactfront/engine/geo/RangeCalculator.java`**
   - `orthodromicDistance(Coordinate, Coordinate)` via `JTS.orthodromicDistance(..., CRS.decode("EPSG:4326"))`
   - Fallback haversine if GeoTools unavailable

4. **Create `engine/src/main/java/com/contactfront/engine/geo/UnitFeatureManager.java`**
   - `DataUtilities.createType("TacticalUnit", "position:Point:srid=4326,unitId:String,sidc:String,faction:String,heading:Double")`
   - `MemoryDataStore` + `SimpleFeatureBuilder`
   - `addUnit(...)` / `removeUnit(...)` / `getFeatures()`

5. **Create `engine/src/main/java/com/contactfront/engine/geo/TerrainRasterLoader.java`**
   - `GeoTiffReader` with `Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER`
   - `Operations.DEFAULT.resample(coverage, targetCRS)`
   - `sampleAsElevation(lat, lon)` → double

6. **Update `CoordinateConversionTest.java`**
   - Replace manual Mercator assertions with GeoTools round-trip tests
   - Add `RangeCalculatorTest` (known distance ~111km per degree at equator)
   - Add `UnitFeatureManagerTest` (add/remove/list)
   - Add `TerrainRasterLoaderTest` (requires a small test GeoTIFF in test resources)

7. **Wire into rules (minimal, non-breaking)**
   - Add optional `distanceMeters(Unit, Unit)` helper using `RangeCalculator` when `GameState` has valid lat/lon; keep existing Manhattan as fallback for generated/scenario-less games.

## Validation
- All existing tests must pass.
- New tests headless.
- `mvn test` green in engine module.

## Out of Scope
- 3D extrusion, LWJGL, PBR.
- LLM/NLP.
- Strategic mode.
- Steam/networking.
- Replacing `Unit` model with `SimpleFeature`.
