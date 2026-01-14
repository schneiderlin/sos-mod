# LLM Instructions for Continuing Development

## Project Context

This is a modding project for **Songs of Syx** game. The goal is to programmatically control the game using Clojure code.

**Important**: Read only the documentation relevant to your current task. Don't read everything upfront - it wastes tokens.

## Project Structure

```
sos-mod/
├── sos-src/              # Game source code (Java) - reference when needed
├── src/
│   ├── repl/            # Clojure REPL scripts
│   │   ├── tutorial1.clj # Main tutorial code
│   │   ├── core.clj     # Reference examples
│   │   └── utils.clj    # Utilities (update-once, reflection helpers)
│   └── game/            # Game interaction code (organized by feature)
│       ├── animal.clj   # Animal finding and hunting
│       ├── warehouse.clj # Warehouse management
│       ├── refiner.clj  # Smelter and refiner room creation
│       ├── hearth.clj   # Hearth (火炉) creation
│       └── common.clj   # Shared utilities (get-building-material, etc.)
└── doc/
    └── src-code/        # Feature documentation (read as needed)
        ├── camera_and_building.md
        ├── animals_and_hunting.md
        ├── warehouses_and_storage.md
        └── furnace_and_refiner.md
```

## Feature Modules (Read Only What You Need)

### Camera Control
- **Code**: `src/repl/tutorial1.clj` (camera functions)
- **Docs**: `doc/src-code/camera_and_building.md` (camera section)
- **Key**: `VIEW/s().getWindow()` for camera access

### Building Creation
- **Code**: `src/game/warehouse.clj`, `src/game/hearth.clj`, `src/game/refiner.clj`
- **Docs**: `doc/src-code/camera_and_building.md`, `doc/src-code/furnace_and_refiner.md`
- **Key**: `ConstructionInit` requires `TBuilding` (use `game.common/get-building-material`)
- **Pattern**: Always use `utils/update-once` for construction

### Animal Finding and Hunting
- **Code**: `src/game/animal.clj`
- **Docs**: `doc/src-code/animals_and_hunting.md`
- **Key**: `SETT/ANIMALS` for animal management, `SETT/ENTITIES` for entity access

### Warehouse and Storage Management
- **Code**: `src/game/warehouse.clj`
- **Docs**: `doc/src-code/warehouses_and_storage.md`
- **Key**: Stockpile system, crate allocation, resource management

### Hearth and Refiner Rooms
- **Code**: `src/game/hearth.clj`, `src/game/refiner.clj`
- **Docs**: `doc/src-code/furnace_and_refiner.md`
- **Key**: Hearth (火炉) vs Smelter (冶金厂) distinction, furniture placement

## Common Utilities

### Essential Functions
- `src/repl/utils.clj` - `update-once` (required for any game state modification)
- `src/game/common.clj` - `get-building-material` (Structure → TBuilding conversion)

### Getting Game Objects
```clojure
(VIEW/s)                    ; Settlement view
(SETT/ROOMS)                ; All room types
(SETT/TERRAIN)              ; Terrain/BUILDINGS
(SETT/ANIMALS)              ; Animal management
(SETT/ENTITIES)             ; All entities
```

## When Starting a New Task

1. **Identify the feature** you're working on (camera, building, animals, etc.)
2. **Read only the relevant documentation** from `doc/src-code/`
3. **Check the relevant code file** in `src/game/` or `src/repl/`
4. **Search `sos-src/`** for Java source code only when needed
5. **Test in REPL** - require only the namespaces you need

**Example**: If working on hunting:
- Read: `doc/src-code/animals_and_hunting.md`
- Check: `src/game/animal.clj`
- Search: `sos-src/settlement/entity/animal/` if needed
- **Don't read**: building or warehouse docs unless relevant

## Important Notes

- Game source is in `sos-src/` (Java) - reference when needed
- Mod code is in `src/` (Clojure)
- Always test in REPL connected to running game
- Use `update-once` for any code that modifies game state
- Read documentation files for detailed explanations and examples

