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

### Known Issues
- Google Maps caching: `cacheImage()` caches indefinitely without expiration - this may violate Google Static Maps ToS
- Buildings are impassable (movementCost=999) - forecloses room-clearing mechanics
- Terrain.ROAD_VERT reference exists but roads are all horizontal from `OsmSemanticGrid`