# Contact Front - Development Guide

## Commands
- Build: `mvn compile`
- Test: `mvn test`
- Package: `mvn package` (in ui module)

## Debugging
- Log file: `logs/contactfront.log` - application-wide logging
- Use `Log.info()`/`Log.error()` for diagnostics
- Console output visible when running from terminal

## Architecture Notes

### Execution Model (RTS)
- Real-time continuous loop via `AnimationTimer` (500ms ticks) in `App.startGameLoop()`
- Player actions execute instantly (no staging, no endTurn) via `GameController.click()` and `GameController.contextOrder()`
- Enemy AI runs continuously in `AiTurn.continuousRun()` each tick

### Real-World Data Pipeline
- `OverpassApiClient.fetchBbox()` - Fetches OSM roads/buildings via Overpass API
- `OsmSemanticGrid.apply()` - Converts geographic coordinates to grid and applies to tiles
- `ElevationClient.fetchSrtm()` - Placeholder (generates procedural noise, not real SRTM)

### Map Views
- MapView has a satellite toggle button (top-right) to switch between:
  - 2D flat tactical map (procedural or satellite-baked terrain with OSM overlays)
  - Satellite view (Google Maps Static API raster with translucent OSM overlays)
- OSM roads/buildings drawn as flat shapes in `MapView.drawOsmOverlays()`

### Known Issues
- Google Maps caching: `cacheImage()` caches indefinitely without expiration - this may violate Google Static Maps ToS
- Buildings are impassable (movementCost=999) - forecloses room-clearing mechanics
- Terrain.ROAD_VERT reference exists but roads are all horizontal from `OsmSemanticGrid`

## Milestone 13 (Replacement) — 2D Map with Satellite Toggle
- 3D rendering (LWJGL/mesh/PBR/extrusion) removed as out of scope
- TerrainMesh.java and PBRMaterial.java deleted
- Flat 2D rendering uses `MapView` with canvas overlay