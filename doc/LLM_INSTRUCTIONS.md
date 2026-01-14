# LLM Instructions for Continuing Development

## Project Context

This is a modding project for **Songs of Syx** game. The goal is to programmatically control the game using Clojure code.

## Key Files to Read First

1. **`src/repl/tutorial1.clj`** - Main working code with camera control and building creation functions
2. **`doc/src-code/camera_and_building.md`** - Complete documentation of what we've learned
3. **`doc/src-code/animals_and_hunting.md`** - Documentation for finding and hunting wild animals
4. **`doc/src-code/warehouses_and_storage.md`** - Documentation for warehouse and storage management
5. **`doc/src-code/furnace_and_refiner.md`** - Documentation for building furnaces (smelters) and other refiner rooms
6. **`src/repl/core.clj`** - Reference implementation and examples
7. **`src/repl/utils.clj`** - Utility functions (update-once, reflection helpers)
8. **`src/game/animal.clj`** - Animal finding and hunting functions
9. **`src/game/warehouse.clj`** - Warehouse and storage management functions
10. **`src/game/refiner.clj`** - Furnace and refiner room creation functions

## Project Structure

```
sos-mod/
├── sos-src/              # Game source code (Java)
├── src/
│   ├── repl/            # Clojure REPL scripts
│   │   ├── tutorial1.clj # Main tutorial code
│   │   ├── core.clj     # Reference examples
│   │   └── utils.clj    # Utilities
│   └── game/            # Game interaction code
│       ├── animal.clj   # Animal finding and hunting
│       ├── warehouse.clj # Warehouse management
│       └── refiner.clj  # Furnace and refiner room creation
└── doc/
    └── src-code/        # Documentation
        ├── camera_and_building.md
        ├── animals_and_hunting.md
        ├── warehouses_and_storage.md
        └── furnace_and_refiner.md
```

## What's Been Accomplished

### Camera Control
- ✅ Move camera to position/tile
- ✅ Move camera by delta
- ✅ Directional movement (WASD-like)
- ✅ Zoom in/out control

### Building Creation
- ✅ Get building materials (Structure → TBuilding conversion)
- ✅ Create warehouses/stockpiles programmatically
- ✅ Understanding of ConstructionInit (needs TBuilding, not Structure)
- ✅ Warehouse construction with automatic furniture placement
- ✅ Automatic wall placement around warehouses
- ✅ Automatic door placement (adjacent to free inner tiles)
- ✅ Pure functions for calculating furniture positions, occupied tiles, edge tiles, and door positions

### Animal Finding and Hunting
- ✅ Find all animals in settlement (wild and domesticated)
- ✅ Filter wild animals (non-domesticated)
- ✅ Get animal information (species, position, hunting status)
- ✅ Mark animals for hunting programmatically
- ✅ Hunt animals in specific areas
- ✅ Find nearest animals to a location

### Warehouse and Storage Management
- ✅ Get all warehouses and warehouse information
- ✅ Query crate allocations for resources
- ✅ Aggregate crate counts across multiple warehouses
- ✅ Find warehouses in specific areas
- ✅ Set material types for crates (allocate crates to resources)
- ✅ Get and set special per-crate limits
- ✅ Understand the stockpile tally system

### Furnace and Refiner Room Creation
- ✅ Find refiner room types (SMELTER, BAKERY, BREWERY, COALER, WEAVER)
- ✅ Create smelters (furnaces) programmatically
- ✅ Create any refiner type programmatically
- ✅ Get refiner information and furniture details
- ✅ Understand refiner room requirements (indoor, minimum size)

### Key Learnings
- `ConstructionInit` requires `TBuilding`, not `Structure`
- Convert using: `BUILDINGS.get(Structure)` where `BUILDINGS = SETT.TERRAIN().BUILDINGS`
- Always use `update-once` for construction to ensure single-frame execution
- Camera uses `GameWindow` class accessed via `VIEW/s().getWindow()`
- Warehouse furniture placement: Use `calculate-furniture-positions` to plan placement, then `calculate-occupied-tiles` to check conflicts
- Wall placement: Use `UtilWallPlacability/wallBuild` and `UtilWallPlacability/openingBuild` for doors
- Door placement: Must be adjacent to a free inner tile (not blocked by furniture)
- Pure functions: Extract calculation logic into pure functions for easier testing in REPL

## Important Source Code References

- `sos-src/view/subview/GameWindow.java` - Camera implementation
- `sos-src/settlement/room/main/construction/ConstructionInit.java` - Building construction
- `sos-src/settlement/tilemap/terrain/TBuilding.java` - Building materials
- `sos-src/init/structure/Structure.java` - Structure definitions
- `sos-src/settlement/entity/animal/Animal.java` - Animal entity class
- `sos-src/settlement/entity/animal/Animals.java` - Animal management system
- `sos-src/settlement/job/JobClears.java` - Hunting job implementation
- `sos-src/settlement/room/main/placement/UtilWallPlacability.java` - Wall and door placement utilities
- `sos-src/settlement/room/main/placement/RoomPlacer.java` - Room placement management
- `sos-src/settlement/room/industry/refiner/ROOM_REFINER.java` - Refiner room blueprint (furnaces, smelters)
- `sos-src/settlement/room/industry/refiner/Constructor.java` - Refiner constructor

## Common Patterns

### Getting Game Objects
```clojure
(VIEW/s)                    ; Settlement view
(SETT/ROOMS)                ; All room types
(SETT/TERRAIN)              ; Terrain/BUILDINGS
(SETT/ANIMALS)              ; Animal management system
(SETT/ENTITIES)             ; All entities (including animals)
(STRUCTURES/all)            ; All structures
(RESOURCES/WOOD)            ; Resource types
```

### Construction Pattern
```clojure
(utils/update-once 
 (fn [_ds]
   ;; Construction code here - must be single frame
   ))
```

### Warehouse Creation Pattern
```clojure
;; Create a warehouse with automatic furniture and wall placement
(create-warehouse-once center-x center-y width height 
                       :material-name "WOOD" 
                       :upgrade 0
                       :place-furniture true)

;; Pure functions for testing in REPL:
(calculate-furniture-positions center-x center-y width height item-width item-height)
(calculate-occupied-tiles furniture-positions item-width item-height)
(find-edge-tiles start-x start-y width height)
(find-door-position center-x center-y width height occupied-tiles :preferred-side :top)
```

### Furnace/Smelter Creation Pattern
```clojure
;; Create a smelter (furnace) at specified location
(create-smelter-once center-x center-y width height 
                     :material-name "WOOD" 
                     :upgrade 0)

;; Create any refiner type (SMELTER, BAKERY, BREWERY, COALER, WEAVER)
(create-refiner-once "SMELTER" center-x center-y width height)

;; List all available refiner types
(all-refiner-types)
(all-refiner-info)
```

## Next Steps / Potential Tasks

1. **Extend building creation** - Support more room types (homes, workshops, etc.) with furniture and wall placement
2. **Entity interaction** - Control NPCs, get entity information
3. **Resource management** - Check warehouse contents, manage resources
4. **Game state queries** - Population, happiness, tech research
5. **Automation** - Create automated gameplay scripts
6. **Advanced hunting** - Automated hunting patrols, hunting efficiency analysis
7. **Room layout optimization** - Improve furniture placement algorithms for better pathfinding

## When Starting a New Task

1. Read `src/repl/tutorial1.clj` to understand current capabilities
2. Read `doc/src-code/camera_and_building.md` for detailed explanations
3. Read `doc/src-code/animals_and_hunting.md` for animal and hunting functionality
4. Read `doc/src-code/warehouses_and_storage.md` for warehouse management
5. Read `doc/src-code/furnace_and_refiner.md` for furnace and refiner room creation
6. Check `src/repl/core.clj` for reference examples
7. Check `src/game/animal.clj` for animal-related functions
8. Check `src/game/warehouse.clj` for warehouse-related functions
9. Check `src/game/refiner.clj` for refiner-related functions
10. Search `sos-src/` for relevant Java classes
11. Test in REPL using `(require 'repl.tutorial1)`, `(require 'game.animal)`, `(require 'game.warehouse)`, and `(require 'game.refiner)` and call functions

## Important Notes

- Game source is in `sos-src/` (Java)
- Mod code is in `src/` (Clojure)
- Always test in REPL connected to running game
- Use `update-once` for any code that modifies game state
- Check source code line numbers in documentation for exact references

