# LLM Instructions for Continuing Development

## Project Context

This is a modding project for **Songs of Syx** game. The goal is to programmatically control the game using Clojure code.

## Key Files to Read First

1. **`src/repl/tutorial1.clj`** - Main working code with camera control and building creation functions
2. **`doc/src-code/camera_and_building.md`** - Complete documentation of what we've learned
3. **`src/repl/core.clj`** - Reference implementation and examples
4. **`src/repl/utils.clj`** - Utility functions (update-once, reflection helpers)

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

## Common Patterns

### Getting Game Objects
```clojure
(VIEW/s)                    ; Settlement view
(SETT/ROOMS)                ; All room types
(SETT/TERRAIN)              ; Terrain/BUILDINGS
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

## When Starting a New Task

1. Read `src/repl/tutorial1.clj` to understand current capabilities
2. Read `doc/src-code/camera_and_building.md` for detailed explanations
3. Check `src/repl/core.clj` for reference examples
4. Search `sos-src/` for relevant Java classes
5. Test in REPL using `(require 'repl.tutorial1)` and call functions

## Important Notes

- Game source is in `sos-src/` (Java)
- Mod code is in `src/` (Clojure)
- Always test in REPL connected to running game
- Use `update-once` for any code that modifies game state
- Check source code line numbers in documentation for exact references

