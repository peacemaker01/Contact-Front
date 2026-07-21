# Pre-Game Setup Screen Implementation Plan

## Goal
Create a setup screen that presents before gameplay where users can configure factions, scenario type, difficulty, and environment before the tactical map loads.

## Current State: COMPLETED
- Main Menu "New Game" → SetupScreen → launches game with selected settings
- Location selection integrated into setup flow (not separate menu item)
- Faction selection available (USA, RUSSIA, IRAN, CHINA)
- Procedural fallback available when no MapTiler API key configured

## Implementation Completed

### Task 1: Create SetupData.java (model) ✅
**File**: `ui/src/main/java/com/contactfront/ui/model/SetupData.java`
- Record containing: playerFaction, enemyFaction, difficulty, isNight, isRaining, isWindy, route
- Enums: Difficulty (EASY/NORMAL/HARD with AI reaction multipliers), Route (RANDOM_LOCATION/CURATED_LOCATION/PROCEDURAL)

### Task 2: Create SetupScreen.java ✅
**File**: `ui/src/main/java/com/contactfront/ui/view/SetupScreen.java`
- Player/enemy faction ComboBoxes with XOR constraint (cannot select same faction)
- Scenario mode RadioButtons: Random Location / Curated Location / Procedural
- Difficulty ComboBox: Easy / Normal / Hard
- Weather checkboxes: Night, Rain, Wind
- MapTiler status label (shows if key configured or unavailable)
- Confirm/Cancel buttons

### Task 3: Modify MainMenu.java ✅
**File**: `ui/src/main/java/com/contactfront/ui/view/MainMenu.java`
- "Choose Location" button removed
- "New Game" now calls `showSetup()` instead of direct game start

### Task 4: Update App.java ✅
**File**: `ui/src/main/java/com/contactfront/ui/App.java`
- `showSetup()`: Display SetupScreen
- `launchGame(SetupData data)`: Route to appropriate game mode
- `startRealWorldGame(SetupData data, LocationSelection loc)`: Real-world flow
- `startProceduralGame(..., SetupData data)`: Includes environment settings
- Procedural fallback automatically used when no API key

### Task 5: Add Doctrine.fromFaction() helper ✅
**File**: `engine/src/main/java/com/contactfront/engine/model/Doctrine.java`
- Maps Faction enum to appropriate Doctrine (USA→NATO, RUSSIA→RUSSIAN, CHINA→CHINESE, IRAN→IRANIAN)

## Validation Performed
- All 27 tests pass (engine + ui modules)
- EXE builds successfully
- Procedural fallback works without MapTiler API key
- Doctrines correctly assigned based on faction selection

## Remaining Work (Future)
- **Real-world location flow**: Currently falls back to procedural; implement full MapTiler + OSM integration
- **Difficulty scaling**: Currently not applied to AI behavior (per v2 plan, this is M5)
- **Environment effects**: Weather flags applied to GameState but not yet used in Suppression/LOS calculations