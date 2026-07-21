# Scenario Builder Implementation Plan

## Goal
Build a comprehensive drag-and-drop scenario editor with NATO/APP-6 SIDC symbology, tactical drawing tools, and shareable JSON serialization.

## Current State Analysis
- `ScenarioEditor.java` exists (basic unit placement on grid)
- `SaveManager.java` handles serialization (game saves)
- GeoTools/JTS already integrated for spatial operations
- MapView has click handlers for unit placement

## Implementation Tasks

### 1. SIDC Symbology System
- [ ] Create `SymbolRenderer.java` - integrates mil-sym-java for NATO/APP-6 symbols
- [ ] Create `SidcCode.java` - represents 15-character Symbol Identification Code
- [ ] Create `SymbolRegistry.java` - maps unit types to SIDC codes
- [ ] Add mil-sym-java dependency to pom.xml

### 2. Enhanced Drag-and-Drop UI
- [ ] Update `ScenarioEditor.java` to use proper drag-drop from palette
- [ ] Add faction toggle with visual affiliation (blue/red frames)
- [ ] Add unit palette with SIDC-rendered icons

### 3. Tactical Drawing Tools
- [ ] Create `TacticalDrawingTool.java` - line/polygon drawing on map
- [ ] Create `TacticalGraphics.java` - stores drawn shapes as JTS geometries
- [ ] Add UI toolbar buttons for: Route, Phase Line, Attack Vector, Assembly Area

### 4. Obstacle Brush
- [ ] Create `ObstacleBrush.java` - paint minefields/fences on map
- [ ] Add `ObstacleType` enum (MINEFIELD, FENCE, BARRIER)
- [ ] Paint creates `Minefield` entities on tiles

### 5. Environment Configuration
- [ ] Extend `ScenarioEditorData` with environment fields
- [ ] Add time-of-day, weather, fog density sliders
- [ ] Map to `GameState.isNight`, `isRaining`, etc.

### 6. Trigger/Logic Editor
- [ ] Create `TriggerNode.java` - visual node for condition/action editing
- [ ] Create `TriggerConnection.java` - links nodes together
- [ ] Create `TriggerSystem.java` - evaluates triggers during game
- [ ] Create `TriggerEditorDialog.java` - visual node-based editor UI
- [ ] Add trigger types: Enter Area, Timer, Unit Strength, etc.
- [ ] Add action types: Spawn Units, Artillery Strike, Ammo Resupply

### 7. Scenario Serialization (.mms format)
- [ ] Create `ScenarioSerializer.java` - saves to custom JSON format
- [ ] Include: metadata, environment, units (with SIDC), obstacles, triggers
- [ ] Support loading `.mms` files via `ScenarioDeserializer.java`

### 8. AI Handoff Integration
- [ ] Extend `ScenarioEditorData` with AI behavior assignments
- [ ] Support: GOAP_Defensive_Hold, HTN_Shock_Breakthrough, etc.
- [ ] Pass to GameState's factionDoctrines

## File Changes

### New Files
- `ui/src/main/java/com/contactfront/ui/view/SymbolRenderer.java`
- `engine/src/main/java/com/contactfront/engine/model/SidcCode.java`
- `engine/src/main/java/com/contactfront/engine/model/SymbolRegistry.java`
- `ui/src/main/java/com/contactfront/ui/view/TacticalDrawingTool.java`
- `engine/src/main/java/com/contactfront/engine/model/TacticalGraphics.java`
- `ui/src/main/java/com/contactfront/ui/view/ObstacleBrush.java`
- `engine/src/main/java/com/contactfront/engine/model/Obstacle.java`
- `engine/src/main/java/com/contactfront/engine/trigger/TriggerNode.java`
- `engine/src/main/java/com/contactfront/engine/trigger/TriggerSystem.java`
- `ui/src/main/java/com/contactfront/ui/ScenarioSerializer.java`
- `ui/src/main/java/com/contactfront/ui/ScenarioDeserializer.java`

### Modified Files
- `ui/src/main/java/com/contactfront/ui/view/ScenarioEditor.java` - Enhanced with SIDC, toolbars
- `engine/src/main/java/com/contactfront/engine/model/GameState.java` - Add placedUnits, obstacles
- `ui/src/main/java/com/contactfront/ui/App.java` - Handle .mms file loading

## Dependencies
- JTS (already available)
- JavaFX Canvas for drawing overlay
- org.json for serialization

## Validation
- [ ] Test SIDC rendering for friendly/hostile units
- [ ] Test tactical drawing saves/loads correctly
- [ ] Test obstacle brush affects movement costs
- [ ] Test trigger system fires on conditions
- [ ] Verify .mms files are portable between installations