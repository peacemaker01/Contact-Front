# OSM Combat Mechanics Integration Plan

## Current State Analysis

### Implemented
- `OverpassApiClient.java`: Fetches OSM roads, buildings, forests via Overpass API
- `OsmSemanticGrid.java`: Rasterizes OSM data to game grid (roads→ROAD terrain, buildings→BUILDING tiles)
- `RoadSegment.java`: Captures road types (MOTORWAY, TRUNK, PRIMARY, etc.) but NOT used for combat modifiers
- `MapTilerClient.java`: Satellite imagery fetching (MapTiler Static API)
- `ElevationClient.java`: Placeholder procedural elevation generation
- `LineOfSight.java`: Bresenham-based LOS with `blocksLos` property checks
- `Movement.java`: Path cost calculation based on tile `movementCost` only

### Missing / Incomplete
- Road-type speed multipliers (μ_tracked, μ_wheeled) per OSM highway tag
- Wetland/marsh terrain type with extreme mobility penalties
- Waterway/ford/bridge logic for river crossing
- Forest thermal signature degradation (ΔT_eff = ΔT * e^(-λ*d))
- Radar clutter model (SCR = (P_clutter + α*N_buildings) / P_signal)

---

## Implementation Plan

### Phase 1: Vehicle Mobility & Trafficability

**Problem**: Current movement uses fixed `movementCost=0.5` for all roads. Spec requires speed coefficients based on `highway=*` tag.

**Files to modify:**
- `engine/src/main/java/com/contactfront/engine/model/Terrain.java` - Add ROAD_VARIANT enum with multipliers
- `engine/src/main/java/com/contactfront/engine/rules/Movement.java` - Add road type lookup in pathCost()
- `engine/src/main/java/com/contactfront/engine/model/RoadSegment.java` - Add road width threshold for "on road" detection
- `ui/src/main/java/com/contactfront/ui/assets/OsmSemanticGrid.java` - Store road type per tile during rasterization

**Design decisions:**
1. Tiles store both terrain type AND road type (new field `RoadSegment.RoadType roadType`)
2. `Movement.pathCost()` checks if unit is within 5m of road segment, applies μ coefficient per road type
3. Speed coefficients: MOTORWAY=0.90/1.00, TRACK=0.85/0.60, PRIMARY/SECONDARY=0.75/0.85, RESIDENTIAL/SERVICE=0.60/0.70

### Phase 2: Terrain Expansion - Wetlands

**Problem**: No `natural=wetland` representation for mud/marsh penalties (μ=0.40/0.15).

**Files to modify:**
- `engine/src/main/java/com/contactfront/engine/model/Terrain.java` - Add WETLAND terrain type
- `ui/src/main/java/com/contactfront/ui/assets/OverpassApiClient.java` - Parse wetland tag
- `ui/src/main/java/com/contactfront/ui/assets/OsmSemanticGrid.java` - Apply wetland terrain during rasterization

### Phase 3: Waterway & Bridge Logic

**Problem**: No river/ford/bridge handling. Spec requires bridge check for waterway crossings.

**Files to modify:**
- `engine/src/main/java/com/contactfront/engine/model/Terrain.java` - Add WATERWAY and FORD types
- `engine/src/main/java/com/contactfront/engine/rules/Movement.java` - Add bridge detection
- `ui/src/main/java/com/contactfront/ui/assets/OverpassApiClient.java` - Parse waterway tags and bridge=yes

**Design:**
- Waterway cells: `movementCost=999` (impassable)
- FORD cells: `movementCost=3.0` (shallow crossing)
- Check if road segment has `bridge=yes` tag before treating as impassable

### Phase 4: Forest Thermal Degradation

**Problem**: Forests block LOS but don't degrade thermal signatures.

**Files to modify:**
- `engine/src/main/java/com/contactfront/engine/model/Terrain.java` - Add forest density coefficient
- `engine/src/main/java/com/contactfront/engine/rules/Visibility.java` - Implement ΔT_eff decay
- `ui/src/main/java/com/contactfront/ui/assets/OsmSemanticGrid.java` - Store tree density per forest tile

**Design:**
- Each forest tile stores `density` (0.0-1.0) based on OSM forest size/attributes
- `Visibility.enemySees()` includes thermal degradation check
- If `ΔT_eff < threshold`, unit becomes invisible to thermal optics

### Phase 5: Radar Clutter Model

**Problem**: No radar blindness in urban environments for SAM units.

**Files to modify:**
- `engine/src/main/java/com/contactfront/engine/rules/Visibility.java` - Add radar clutter calculation
- `engine/src/main/java/com/contactfront/engine/model/SensorEmission.java` - Extend with radar-specific data
- `engine/src/main/java/com/contactfront/engine/model/Unit.java` - Add radar emission property

**Design:**
- For each enemy unit with `Active_RF` sensor, count buildings in radar FOV
- `SCR = (P_clutter + α * N_buildings) / P_signal`
- If SCR below threshold, fall back to optical tracking only

---

## Data Flow

```
[OSM Raw] → OverpassApiClient.parseOsm() → OsmData(roads, buildings, forests)
               ↓
OsmSemanticGrid.apply() → GameState.grid[tile].roadType, roadWidth, forestDensity
               ↓
Movement.pathCost() → checks roadType and applies μ_coefficient
               ↓
Visibility.enemySees() → checks radar clutter, thermal degradation
```

## Validation

1. Unit on motorway moves faster than unit on dirt track
2. Infantry in wetland moves extremely slowly (μ=0.40)
3. Vehicles cannot cross river without bridge or ford
4. Units in forest become harder to detect via thermal
5. SAM units in dense urban area lose radar lock on low-flying targets

## Open Questions

1. Should road width threshold (5m) be configurable or per-road-type derived?
2. What is the thermal signature threshold value? Needs playtesting.
3. What are α and P_clutter values for radar model? Document defaults.
4. Should wetlands be represented as separate terrain or as movement modifier on existing terrain?

## Dependencies

- GeoTools for coordinate transforms (optional enhancement, web Mercator currently sufficient)
- No new external dependencies required for core implementation