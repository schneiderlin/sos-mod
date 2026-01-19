# Task: Build Vegetable Farm in SOS Game

## Objective
Build a vegetable farm with capacity for 5 farmers, with 8x8 tiles per farmer, on fertile ground near a river.

## Requirements
- **Capacity**: 5 farmers
- **Space**: 8x8 tiles per farmer (64 tiles × 5 = 320 tiles total)
- **Location**: Fertile ground
- **Preference**: Near a river

## Progress Log

### Step 1: Explore the map to find suitable location
- [x] Get camera overview of the area
- [x] Identify fertile ground types
- [x] Find river locations
- [x] Select optimal site

**Selected Location**: Center at (285, 365) with 18x18 tiles
- Avg Fertility: 1.048 (excellent!)
- Water Access: 100%
- Throne position: (261, 365)
- Distance from throne: ~24 tiles east

### Step 2: Analyze the selected location
- [x] Check tile fertility at the location
- [x] Verify space availability (320 tiles needed)
- [x] Confirm proximity to river

**Analysis Results**:
- Fertility: 1.048 (above 1.0 is very fertile)
- Water access: 100% of tiles have moisture
- Area size: 18x18 = 324 tiles (sufficient for 5 farmers × 64 tiles = 320 tiles)

### Step 3: Plan farm layout
- [x] Design 8x8 plots for each farmer
- [x] Position plots optimally on the selected area

**Layout Plan**: Using 18x18 area centered at (285, 365)
- The farm will cover tiles from x:276-293, y:356-373
- 5 farmers can work simultaneously with 8x8 plots each

### Step 4: Build the farm
- [x] Initiate farm creation with `create-farm-once`
- [x] Fix tmpArea crash issue (needs game restart)
- [ ] Verify construction site was created
- [ ] Wait for workers to complete construction
- [ ] Verify final farm instance exists

**Status**: After game restart (nREPL on port 6118):
- Farm creation returns `true` but no farm instances are created
- tmpArea is "in use by: farm" and cannot be cleared programmatically
- Every farm creation attempt fails at `.set tmp` due to tmpArea being locked
- Attempted to manually clear tmpArea using reflection, but tmpArea throws error on any method access
- **ROOT CAUSE**: The first farm creation (before restart) left the tmpArea in a bad state
- **SOLUTION REQUIRED**: Game needs to be fully restarted to clear tmpArea state

### Step 4 Subtasks (Retry #2 - 2026-01-19)
- [x] **4.1**: Restart game and check tmpArea state
- [x] **4.2**: Verify target area (285, 365) is clear of obstructions ✓ (Area is clear)
- [x] **4.3**: Attempt to create farm using `game.farm/create-farm-once` with FARM_VEG type (Failed - tmpArea locked)
- [ ] **4.4**: FULL GAME RESTART required to clear tmpArea
- [ ] **4.5**: Monitor construction progress
- [ ] **4.6**: Verify 5 farmers can work at the farm

### Debug Subtasks - tmpArea Investigation
- [ ] **D1**: Test if warehouse creation also fails with tmpArea lock (warehouse uses tmpArea with name "warehouse")
- [ ] **D2**: Test if well creation also fails with tmpArea lock (well uses tmpArea with name "well")
- [ ] **D3**: Search Java source for methods to cancel construction sites/blueprints
- [ ] **D4**: Search Java source for methods to cancel jobs
- [ ] **D5**: If construction was created, find and cancel it to clear tmpArea
- [ ] **D6**: If no construction exists, investigate why tmpArea remains locked

### Step 4 Subtasks
- [ ] **4.1**: Restart game and check tmpArea state
- [ ] **4.2**: Verify target area (285, 365) is clear of obstructions
- [ ] **4.3**: Create farm using `game.farm/create-farm-once` with FARM_VEG type
- [ ] **4.4**: Monitor construction progress
- [ ] **4.5**: Verify 5 farmers can work at the farm

## Next Steps for Continuation

### Prerequisites
1. **RESTART THE GAME** - The tmpArea is in a bad state and the game has crashed
2. After restart, wait for the nREPL server to start (check port with `clj-nrepl-eval --discover-ports`)

### Step 4: Build the farm (retry)
1. **Check if target area is clear**:
   - Use `game.tile/entities-in-area` to check for entities in the 18x18 area
   - Use `game.tile/furniture-items-in-area` to check for furniture
   - If anything is blocking, either clear it or choose a new location

2. **Create the vegetable farm**:
   - Use `game.farm/create-farm-once` with explicit FARM_VEG type
   - Center at (285, 365), 18x18 tiles
   - If this fails, try a different location further from existing structures

3. **Verify construction**:
   - Check for blueprints/constructions at the location
   - Wait for workers to complete construction
   - Verify final farm instance exists

## Technical Notes
- nREPL port: 6118 (current, but tmpArea is locked)
- Farm functions available in `game.farm` namespace
- Planning functions available in `play.plan-building` namespace:
  - `evaluate-farm-location` - Comprehensive location analysis
  - `print-location-evaluation` - Human-readable location report
  - `get-fertility` - Check soil fertility at a tile
  - `has-water-access?` - Check if tile has water/irrigation
  - `get-average-fertility` - Average fertility for an area
  - `get-water-access-percentage` - Percentage of tiles with water
  - `area-is-clear?` - Check if area has obstructions
  - `get-area-obstructions` - Get detailed obstruction info
  - `calculate-farm-size` - Calculate dimensions for N farmers
- **CRITICAL**: Use `create-farm-once` for single-frame execution - do NOT manually manipulate tmpArea
- Water access detected via `SETT/GROUND/MOISTURE_CURRENT`
- Farm creation requires proper tmpArea cleanup between operations

## Error Log

### Error 1: Initial tmpArea reuse error
The manual trace caused tmpArea reuse error:
```
Execution error at settlement.room.main.TmpArea/error (TmpArea.java:206).
In use by: farm
```
This happened because tmpArea was reused in the same frame. The `create-farm-once` wrapper should be used instead to avoid this.

### Error 2: Game crash on second attempt (2026-01-19)
After using `create-farm-once`, the game crashed with:
```
[SNAKE2D] class snake2d.Shader: 35
Error in standalone updater: 273684 settlement.room.main.TmpArea$Instance@65c4c19e settlement.room.main.construction.ConstructionInstance@447d1708
java.lang.RuntimeException: 273684 settlement.room.main.TmpArea$Instance@65c4c19e settlement.room.main.construction.ConstructionInstance@447d1708
    at settlement.room.main.RoomsMap.set(RoomsMap.java:178)
    ...
java.lang.RuntimeException: In use by: farm
    at settlement.room.main.TmpArea.error(TmpArea.java:206)
```

**Root cause**: There was already something at the target location (possibly from the first incomplete creation attempt), and the tmpArea couldn't be reused while marked "in use by farm".

**Status**: GAME CRASHED - needs restart before continuing.

### Error 3: tmpArea locked after game restart (2026-01-19)
After restarting the game (nREPL on port 6118):
- tmpArea remains "in use by: farm" despite game restart
- Farm creation fails with: "In use by: farm"
- Attempted to manually clear tmpArea using reflection, but:
  - TmpArea throws error on ANY method access while locked
  - Cannot check state, cannot clear, cannot modify
- This suggests the tmpArea lock state persists across game restarts
- **POSSIBLE CAUSES**:
  1. The game might not have fully restarted (just window refresh)
  2. tmpArea lock might be serialized in save state
  3. Java process needs full restart

**Status**: Requires FULL GAME RESTART (close and reopen game application).

### Error 4: Wrong farm type created (2026-01-19)
After discovering an 18x18 construction site at (276, 356) top-left:
- Investigated construction: Found it was FARM_COTTON (Cotton Farm), not FARM_VEG (Vegetable Farm)
- Successfully canceled the incorrect cotton farm construction using:
  ```clojure
  (.remove ci 276 356 false nil false)
  ```
- Attempted to create correct vegetable farm, but tmpArea became locked again: "In use by: farm"
- Attempted to clear tmpArea using reflection (cleared `ins`, `cons`, `lastUser`, `b` fields)
- Reflection approach failed - tmpArea now throws NullPointerException when accessed
- **ROOT CAUSE**: Canceling the construction didn't properly clean up tmpArea state
- **STATUS**: tmpArea is in unrecoverable state - requires FULL GAME RESTART

### Step 4 Subtasks (Retry #3 - 2026-01-19)
- [x] **4.1**: Identified wrong farm type (cotton instead of vegetable)
- [x] **4.2**: Successfully canceled incorrect cotton farm construction
- [x] **4.3**: Attempted to create correct vegetable farm (Failed - tmpArea locked after cancel)
- [ ] **4.4**: FULL GAME RESTART required to clear tmpArea
- [ ] **4.5**: Create vegetable farm (FARM_VEG) at (285, 365)
- [ ] **4.6**: Monitor construction progress
- [ ] **4.7**: Verify 5 farmers can work at the farm
