Contact Front — Tactical Mode v2 Build Plan (Defense-Contractor Standard)

This document is the single source of truth for the development of "Contact Front," a 2D real-time tactical wargame simulator.

1. Executive Philosophy: The Defense Standard

Doctrinal Fidelity: AI behavior is determined by faction-specific Doctrine profiles, simulating real-world operational habits.

Deterministic Simulation: Identical seed = identical location + identical scenario generation. Crucial for After Action Review (AAR).

Geo-Referenced Reality: The simulation does not use procedural noise. It uses real-world data (OSM/MapTiler/DEM) to define the tactical environment.

Traceability: Every tick must be loggable. We must be able to explain why an AI unit took an action based on its Utility Weightings.

2. Architecture: Decoupled & Modular

engine/: The core simulation. Pure Java. Contains TickProcessor, RulesEngine, FactionRegistry, and DoctrineEngine.

geo/: The new Data Orchestration Layer. Handles MapTilerClient, OverpassApiClient, and GeoTools alignment.

ui/: JavaFX frontend. Uses a SymbolRenderer Strategy pattern for NATO/Russian symbology.

app/: Distribution packaging (jpackage).

3. The Scalable Data Engine (Milestone 2)

Factions, Units, and Weapons are defined in data (JSON), loaded into a central Registry at runtime.

Registry Pattern: FactionRegistry loads files from data/factions/*.json.

Namespaced IDs: Assets use namespace:id (e.g., us:m1a2, pla:type_99, iran:karrar).

Attritional Combat: Combat is resolved via WeaponClass vs ArmorClass lookup tables.

4. Real-World Scenario Orchestration (Milestone 3 - Pivot)

"New Game" logic is now location-based orchestration, not procedural.

LocationRegistry: A curated JSON list of viable tactical bounding boxes (Lat/Lon).

Constraint: Prevents the engine from picking "dead" locations (empty ocean/desert).

Data Orchestrator:

MapTilerClient: Fetches raster imagery for the selected bounding box.

OverpassApiClient: Parses OSM XML/JSON to generate the TacticalGrid (roads, buildings, vegetation).

GeoTools: Aligns the Raster image to the OSM Vector grid.

Deterministic Seed: The "Random Seed" for a New Game now selects a Location and Scenario Parameters. Re-running with the same seed pulls the same real-world data, ensuring perfect save-game stability.

5. AI & Doctrine Engine (Milestone 5)

AI is Utility-based.

Utility AI: Units score candidate actions (Engage, Suppress, Retreat, Reload) each tick.

Doctrine Weights: Faction-specific JSON modifies these utility scores.

6. Development Milestones

M1: Engine Foundation: Build the headless, tick-based TickProcessor.

M2: Registry & Data: Implement JSON schema loaders for factions/units/weapons.

M3: Geo-Orchestration: Implement LocationRegistry and OSM/MapTiler integration via GeoTools.

M4: Combat/Suppression: Implement Attrition Lookup Tables.

M5: AI Doctrine: Implement Utility AI and Doctrine weighting.

M6: UI/Symbology: Implement the Strategy-pattern renderer for NATO vs. Russian symbology.

M7: AAR & Persistence: Implement save/load (using seed + state snapshot).

M8: Packaging: jpackage native distribution.

7. Constraints (Non-Negotiable)

No 3D/LWJGL: 2D canvas/GeoTools rendering only.

No LLM Logic: AI must remain deterministic.

No WebView: Use GeoTools, not browser-based mapping.

Curated Pool: "Random" generation must be constrained to pre-vetted location pools to guarantee gameplay viability.