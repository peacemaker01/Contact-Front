# Real-World Doctrine-Based Combat Mechanics Plan

## Goal
Implement faction-specific doctrine behavior for AI units, with a toggle between explicit player command and doctrine-driven AI.

## Decisions Made
- **Player Control**: Toggle between explicit command (player directs all actions) and doctrine-driven (AI executes faction-appropriate tactics)
- **Current AI**: Generic behavior in `AiTurn.java` - seeks cover, engages at optimal range, uses overwatch
- **Doctrinal Differences**: NATO (dispersion, precision, defensive), Russian (mass, attrition, aggressive)

## Key Changes Required

### 1. Add Doctrine Enum
File: `engine/src/main/java/com/contactfront/engine/model/Doctrine.java`
```java
public enum Doctrine {
    NATO {
        @Override void apply(Unit u, GameState s) {
            u.prefersDispersion = true;
            u.accuracyBonus = 10;
            u.incomingSuppressionMult = 0.8;
            u.defaultStance = Stance.DEFENSIVE;
            u.engageOnlyAtOptimalRange = true;
            u.usesOverwatch = true;
        }
    },
    RUSSIAN {
        @Override void apply(Unit u, GameState s) {
            u.usesMassedAttacks = true;
            u.moraleThreshold = 10;
            u.incomingSuppressionMult = 1.1;
            u.defaultStance = Stance.AGGRESSIVE;
            u.engageAtAnyRange = true;
            u.seeksContact = true;
        }
    };
    abstract void apply(Unit u, GameState s);
}
```

### 2. Add Command Mode Enum  
File: `engine/src/main/java/com/contactfront/engine/model/CommandMode.java`
```java
public enum CommandMode {
    EXPLICIT,   // Player controls all actions
    DOCTRINE    // AI executes faction doctrine
}
```

### 3. Extend Unit Model
File: `engine/src/main/java/com/contactfront/engine/model/Unit.java`
Add doctrine-aware fields:
- `boolean prefersDispersion`
- `boolean usesMassedAttacks`
- `double accuracyBonus`
- `double incomingSuppressionMult`
- `Stance defaultStance`
- `boolean engageOnlyAtOptimalRange`
- `boolean engageAtAnyRange`
- `boolean seesOverwatch`
- `boolean seeksContact`

### 4. Extend GameState
File: `engine/src/main/java/com/contactfront/engine/model/GameState.java`
- Add `CommandMode commandMode` field
- Add `Map<Faction, Doctrine> factionDoctrines` field

### 5. Modify AiTurn for Doctrine
File: `engine/src/main/java/com/contactfront/engine/rules/AiTurn.java`
- Check `GameState.commandMode == DOCTRINE`
- **NATO behavior**: Spread units, hold position, use overwatch, engage only at optimal range
- **Russian behavior**: Concentrate units, aggressive attacks, accept casualties, seek contact
- Modify `chooseStance()` to use doctrine default
- Modify `findFlankPath()` - NATO avoids, Russian seeks

### 6. Add UI Toggle
File: `ui/src/main/java/com/contactfront/ui/view/CommandModeToggle.java`
- Dropdown: "Explicit Command" / "Doctrine-Driven"
- Persists to scenario file
- Shows doctrine behavior description

## Implementation Steps

1. Add doctrine fields to `Unit.java`
2. Create `Doctrine.java` enum with NATO/Russian modifiers
3. Create `CommandMode.java` enum (EXPLICIT, DOCTRINE)
4. Update `GameState.java` with commandMode and factionDoctrines fields
5. Modify `AiTurn.java` to check commandMode and apply doctrine
6. Create `CommandModeToggle.java` UI component
7. Update `ScenarioSerializer.java` to save/load command mode
8. Update `ScenarioBuilder.java` with doctrine selection UI
9. Test doctrine differences: NATO dispersion vs Russian mass attacks

## Validation
- [x] NATO units spread out, hold defensive stances, use overwatch
- [x] Russian units attack aggressively, accept contact, use AGGRESSIVE stance
- [x] Explicit mode: player controls all unit actions
- [x] Doctrine mode: AI follows faction tactics
- [x] Command mode persists in scenario files

## Out of Scope (for Phase 1)
- Combined arms coordination (infantry-tank cooperation)
- Electronic warfare effects
- Logistics (fuel/ammo resupply)
- Intelligence gathering (recon missions)

## Implementation Summary

### Created Files
- `engine/src/main/java/com/contactfront/engine/model/Doctrine.java` - NATO, RUSSIAN, CHINESE, IRANIAN doctrines
- `engine/src/main/java/com/contactfront/engine/model/CommandMode.java` - EXPLICIT/DOCTRINE enum

### Modified Files
- `Unit.java` - Added doctrine fields (prefersDispersion, usesMassedAttacks, incomingSuppressionMult, defaultStance, etc.)
- `GameState.java` - Added commandMode and factionDoctrines
- `AiTurn.java` - Doctrine-aware stance selection and flanking behavior
- `Combat.java` - Uses incomingSuppressionMult and morale threshold from doctrine
- `ScenarioBuilder.java` - Added command mode and doctrine selection UI
- `ScenarioSerializer.java` - Saves/loads command mode and doctrines
- `SaveManager.java` - Persists command mode and doctrines to save files
- `App.java` - Handles new scenario data fields
- `MapView.java` - Fixed IRA→IRAN faction reference

### Build Status
- Build: SUCCESS
- Tests: PASSED (12/12)
- Run: Application launches successfully