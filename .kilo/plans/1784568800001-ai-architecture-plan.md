# Contact Front AI Architecture Implementation Plan

## Goal
Implement a Three-Tiered AI Decision Architecture with geospatial reasoning using GeoTools/JTS for authentic modern warfare tactical decisions.

## Architecture Overview

```
[Tactical Planner] ← Strategic Director (Utility-Based Resource Allocation)
         ↑
[Reactive Micro-Agent]
```

## Implementation Tasks

### Tier 1: Strategic Director (Macro Resource Allocation)
- [ ] Create `StrategicDirector.java` with Utility AI for objective prioritization
- [ ] Implement operational strength/casualty rate monitoring
- [ ] Add network connectivity evaluation
- [ ] Add global objective allocation (mechanized platoons, drone swarms)

### Tier 2: Tactical Planner (Geospatial Spatial Reasoning)

#### Spatial Infrastructure
- [ ] Add JTS STRtree spatial index for OSM geometries (buildings, forests, roads)
- [ ] Create `InfluenceMap.java` for threat/coverage/trafficability evaluation
- [ ] Implement `ThreatEvaluator.java` with facing-based dynamics

#### Core Formulas
- [ ] Static threat: `T(p) = Σ w_i · e^(-α·d(p,u_i))`
- [ ] Dynamic threat: `T_dynamic(p) = T(p) · max(0, v_i · u_ip)`
- [ ] Path cost with threat penalty: `C(a,b) = d(a,b) · (1 + β · T_dynamic(b))`

#### Influence Map Grid
- [ ] Create metric grid projection over active area of operations
- [ ] Implement safe corridor/chokepoint detection
- [ ] Add LOS masking cover identification

### Tier 3: Reactive Micro-Agent (Unit-Level Execution)

#### Behavior Systems
- [ ] Create `HtnPlanner.java` for structured military decisions
- [ ] Create `BehaviorTree.java` for rapid combat reflexes
- [ ] Implement `MicroAgent.java` for individual platform actions

#### Faction Doctrines
- [ ] US: GOAP-based Network-Centric (CJADC2 shared network)
- [ ] PLA: HTN System-Destruction Maneuver
- [ ] Russia: State-Machine Positional Defense + mine barriers
- [ ] Iran: Decentralized Utility AI + ambush tactics

## File Changes

### New Files
- `engine/src/main/java/com/contactfront/engine/ai/StrategicDirector.java`
- `engine/src/main/java/com/contactfront/engine/ai/TacticalPlanner.java`
- `engine/src/main/java/com/contactfront/engine/ai/ThreatEvaluator.java`
- `engine/src/main/java/com/contactfront/engine/ai/InfluenceMap.java`
- `engine/src/main/java/com/contactfront/engine/ai/MicroAgent.java`
- `engine/src/main/java/com/contactfront/engine/ai/HtnPlanner.java`
- `engine/src/main/java/com/contactfront/engine/ai/BehaviorTree.java`
- `engine/src/main/java/com/contactfront/engine/ai/Factions/USDoctrine.java`
- `engine/src/main/java/com/contactfront/engine/ai/Factions/PLADoctrine.java`
- `engine/src/main/java/com/contactfront/engine/ai/Factions/RussianDoctrine.java`
- `engine/src/main/java/com/contactfront/engine/ai/Factions/IranDoctrine.java`

### Modified Files
- `engine/src/main/java/com/contactfront/engine/rules/AiTurn.java` - Integrate new tiers
- `engine/src/main/java/com/contactfront/engine/TacticalEngine.java` - Time-slice AI loop
- `engine/src/main/java/com/contactfront/engine/model/Tile.java` - Add facing/orientation fields
- `engine/src/main/java/com/contactfront/engine/model/Unit.java` - Add weapon facing vector

## Dependencies
- JTS (already in use via GeoTools/geotools-core)
- STRtree spatial index

## Validation
- [ ] Test threat map generation with unit placement
- [ ] Test path finding avoids high-threat zones
- [ ] Test faction-specific behaviors (US GOAP, PLA HTN, etc.)
- [ ] Verify time-sliced AI maintains 500ms tick rate

## Open Questions
1. Should threat evaluation run every tick or time-sliced (1-3 frames)?
2. Do we need separate influence maps per faction or one shared map?
3. How should `v_i` (weapon facing) be stored - as Unit field or computed?