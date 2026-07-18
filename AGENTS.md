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
- Real-time continuous loop via `AnimationTimer` (500ms ticks) in `App.startTickProcessor()`
- Player actions execute instantly (no staging, no endTurn) via `GameController.click()` and `GameController.contextOrder()`
- Enemy AI runs continuously in `AiTurn.continuousRun()` each tick

### Real-World Data Pipeline
- `MapTilerClient.fetchCachedSatelliteImage()` - Fetches satellite imagery via MapTiler Static Maps API
- `OverpassApiClient.fetchBbox()` - Fetches OSM roads/buildings/forests via Overpass API
- `OsmSemanticGrid.apply()` - Converts geographic coordinates to grid and applies to tiles
- `ElevationClient.generateProceduralElevation()` - Procedural noise placeholder (OpenTopography API stub available)

### Map Views
- MapView has a satellite toggle button (top-right) to switch between:
  - 2D flat tactical map (procedural terrain with OSM overlays)
  - Satellite view (MapTiler Static API raster with translucent OSM overlays)

### Known Issues
- Buildings are impassable (movementCost=999) - forecloses room-clearing mechanics
- Terrain.ROAD_VERT reference exists but roads are all horizontal from `OsmSemanticGrid`
- GeoOrchestrator requires gt-epsg-hsql for CRS initialization (stub in place)

### Policy
- All new games require MapTiler API key configured in Options
- Locations selected from curated pool via LocationRegistry (seed determines location)
- No procedural terrain fallback - always uses real OSM/MapTiler data

## Milestone 13 (Geo-Orchestration) — Real-World Tactical Maps
- MapTiler Static Maps API integrated for satellite imagery
- LocationRegistry provides curated tactical locations  
- Caching ensures deterministic saves and avoids redundant API calls
- Overpass API now includes forest/landuse queries