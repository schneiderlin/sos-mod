# LLM Instructions for Continuing Development

## Project Context

This is a modding project for **Songs of Syx** game. The goal is to programmatically control the game using Clojure code.

## Key Files to Read First

1. **`src/repl/tutorial1.clj`** - Main working code with camera control and building creation functions
2. **`doc/src-code/camera_and_building.md`** - Complete documentation of what we've learned
3. **`doc/src-code/animals_and_hunting.md`** - Documentation for finding and hunting wild animals
4. **`src/repl/core.clj`** - Reference implementation and examples
5. **`src/repl/utils.clj`** - Utility functions (update-once, reflection helpers)
6. **`src/game/animal.clj`** - Animal finding and hunting functions

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
└── doc/
    └── src-code/        # Documentation
        └── camera_and_building.md
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

### Animal Finding and Hunting
- ✅ Find all animals in settlement (wild and domesticated)
- ✅ Filter wild animals (non-domesticated)
- ✅ Get animal information (species, position, hunting status)
- ✅ Mark animals for hunting programmatically
- ✅ Hunt animals in specific areas
- ✅ Find nearest animals to a location

### Key Learnings
- `ConstructionInit` requires `TBuilding`, not `Structure`
- Convert using: `BUILDINGS.get(Structure)` where `BUILDINGS = SETT.TERRAIN().BUILDINGS`
- Always use `update-once` for construction to ensure single-frame execution
- Camera uses `GameWindow` class accessed via `VIEW/s().getWindow()`

## Important Source Code References

- `sos-src/view/subview/GameWindow.java` - Camera implementation
- `sos-src/settlement/room/main/construction/ConstructionInit.java` - Building construction
- `sos-src/settlement/tilemap/terrain/TBuilding.java` - Building materials
- `sos-src/init/structure/Structure.java` - Structure definitions
- `sos-src/settlement/entity/animal/Animal.java` - Animal entity class
- `sos-src/settlement/entity/animal/Animals.java` - Animal management system
- `sos-src/settlement/job/JobClears.java` - Hunting job implementation

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

## Next Steps / Potential Tasks

1. **Extend building creation** - Support more room types (homes, workshops, etc.)
2. **Entity interaction** - Control NPCs, get entity information
3. **Resource management** - Check warehouse contents, manage resources
4. **Game state queries** - Population, happiness, tech research
5. **Automation** - Create automated gameplay scripts
6. **Advanced hunting** - Automated hunting patrols, hunting efficiency analysis

## When Starting a New Task

1. Read `src/repl/tutorial1.clj` to understand current capabilities
2. Read `doc/src-code/camera_and_building.md` for detailed explanations
3. Read `doc/src-code/animals_and_hunting.md` for animal and hunting functionality
4. Check `src/repl/core.clj` for reference examples
5. Check `src/game/animal.clj` for animal-related functions
6. Search `sos-src/` for relevant Java classes
7. Test in REPL using `(require 'repl.tutorial1)` and `(require 'game.animal)` and call functions

## Important Notes

- Game source is in `sos-src/` (Java)
- Mod code is in `src/` (Clojure)
- Always test in REPL connected to running game
- Use `update-once` for any code that modifies game state
- Check source code line numbers in documentation for exact references

