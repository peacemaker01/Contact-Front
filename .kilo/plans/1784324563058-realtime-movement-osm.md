# Real-Time Movement & OSM Integration Plan

## Current State Analysis

### Movement
- `Movement.applyMove()` teleports units instantly (`u.x = tx; u.y = ty`)
- `GameController.click()` immediately calls `engine.resolveAction(new MoveAction(...), false)`
- No gradual movement state - units have no destination tracking

### OSM/Google Maps  
- `App.handleLocationSelection`: fetches OSM data, catches ALL exceptions silently
- No user feedback when OSM fetch fails
- Satellite imagery and OSM are separate - both conditional on API key

## Key Decisions

### Movement: Option B (Selected)
**Action queue with delayed execution**
- MoveAction added to `delayedOrders` with execution turn
- Units move incrementally each tick until destination reached
- Uses existing `DelayedOrder` infrastructure

### Movement Speed Scaling
- Each tick = 500ms game time
- Movement cost determines ticks needed: 1 tile = `movementCost` ticks
- Roads (cost=1) = 1 tick per tile, difficult terrain (cost=2) = 2 ticks per tile
- Stance modifies: `DEFENSIVE` ×0.7, `ASSAULT` ×1.2, `RETREAT` ×0.9

### OSM Error Handling: Option A (Selected)
- Show status message when fetch fails
- Continue procedural fallback silently
- No retry (user can change coordinates)

## Implementation Tasks

### Task 1: Modify MoveAction for Gradual Movement
- Add `stepsRemaining` field to `DelayedOrder` for MoveAction
- Add `int targetX, targetY, stepsRemaining` to Unit (or use path field)

### Task 2: Update Movement.applyMove()
- When called with MoveAction, set unit's targetX/targetY
- Calculate `stepsRemaining` based on pathCost and movementCost scaling
- Move 1 tile per tick toward destination

### Task 3: Update TacticalEngine.processDelayedOrders()
- For MoveAction: call incremental movement logic instead of full teleport
- Decrease `stepsRemaining` each tick
- Clear target when `stepsRemaining <= 0`

### Task 4: Update GameController.click()
- Remove `selection.clear()` - selection persists during movement
- Only clear selection on ESC or new selection

### Task 5: Verify MapView.drawGhosts()
- Already draws MoveAction destination lines
- Ensure it shows for player orders

### Task 6: OSM Integration Improvements
- Add `Log.info/error` calls in `OverpassApiClient.fetchBbox()`
- Add toast notification in `App.handleLocationSelection()` on failure
- Verify `OsmSemanticGrid.apply()` correctly converts lat/lon to grid coordinates

### Task 7: Unit Path Tracking
- Add `int destX=-1, destY=-1` to Unit ✓ DONE
- Add `double progressX, progressY` for smooth animation (optional)
- Update rendering to show units at interpolated position

### Task 8: Selection Persistence
- Selection should persist during movement
- ESC should cancel movement (clear dest) and selection

## Validation Steps
1. Run game with no API key → procedural terrain ✓
2. Run game with invalid OSM coordinates → fallback with logging ✓
3. Select unit, click distant tile → observe gradual movement over 2-4 seconds
4. Verify ghost line shows destination
5. Verify selection persists during movement ✓
6. Verify roads reduce movement cost appropriately

## Implementation Status
- Task 1-2: Unit destination tracking and gradual movement - DONE
- Task 3-4: TacticalEngine tick integration and GameController clearSelection - DONE
- Task 6: OSM logging - DONE

## Risks & Edge Cases
- Units on roads but moving across difficult terrain: should use destination terrain cost
- Multiple units with same target tile: need collision handling during movement
- Unit destroyed during movement: clear target and remove from queue
- Selection persistence: ESC should cancel movement, not just clear selection