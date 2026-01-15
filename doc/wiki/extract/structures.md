# Structures Extraction

This document describes the data extraction for Structures (building materials) in Songs of Syx.

## Overview

Structures in Songs of Syx are building materials used for walls, floors, and ceilings. They include materials like stone, wood, and mud with varying durability and construction requirements.

## Source Code Locations

### Game Classes
- `init.structure.STRUCTURES` - Main registry containing all structure types
- `init.structure.Structure` - Structure data class with properties

### Clojure Files
- **Game API**: `src/game/structure.clj`
- **Extraction**: `src/extract/structure.clj`

## Data Available

### Structure Properties

| Property | Source | Type | Description |
|----------|--------|------|-------------|
| key | `Structure.key` | String | Unique identifier (e.g., "_STONE", "_WOOD") |
| index | `Structure.index()` | Integer | Index in ALL list |
| name | `Structure.name` | String | Display name |
| description | `Structure.desc` | String | Description text |
| nameWall | `Structure.nameWall` | String | Wall variant name (e.g., "Stone Wall") |
| nameCeiling | `Structure.nameCeiling` | String | Ceiling variant name (e.g., "Stone Ceiling") |
| durability | `Structure.durability` | Double | Structural durability (0-1 scaled by tile size) |
| constructTime | `Structure.constructTime` | Double | Build time (0-10000) |
| resource | `Structure.resource` | RESOURCE | Required resource (nullable) |
| resAmount | `Structure.resAmount` | Integer | Resource amount per unit (0-16) |
| tint | `Structure.tint` | PlayerColor | Building tint color |
| miniColor | `Structure.miniColor` | COLOR | Minimap display color |

## Structure Types

Common structure types include:
- `_MUD` - Basic mud structure (default/fallback)
- `_WOOD` - Wooden structure
- `_STONE` - Stone structure
- And potentially modded structures...

## Usage Examples

### Basic Access

```clojure
(require '[game.structure :as struct])

;; Get all structures
(struct/all-structures)
(struct/structure-count)

;; Get specific structure
(def stone (struct/get-structure "_STONE"))
(struct/structure-name stone)
(struct/structure-durability stone)

;; Get MUD structure (default)
(struct/mud-structure)
```

### Convert to Map

```clojure
;; Single structure
(struct/structure->map (struct/get-structure "_STONE"))

;; All structures
(struct/all-structures-as-maps)
```

### Queries

```clojure
;; Find by key
(struct/get-structure "_WOOD")

;; Find structures using a specific resource
(struct/find-structures-by-resource "_STONE")

;; Sort by durability (strongest first)
(struct/structure-by-durability)

;; Sort by construction time (fastest first)
(struct/structure-by-construct-time)
```

### Filtering

```clojure
;; Get structures that require resources
(struct/structures-with-resources)
(struct/structures-with-resources-as-maps)
```

## Extraction

### Run Extraction

```clojure
(require '[extract.structure :as struct-extract])

;; Summary
(struct-extract/extract-structures-summary)

;; Extract to files
(struct-extract/extract-all "output/wiki")
```

### Output Files

- `output/wiki/data/structures.edn` - All structures data

## Output Schema

### structures.edn

```clojure
{:version "1.0"
 :extracted-at "2026-01-15T..."
 :summary {:total-structures 5
           :with-resources 4
           :unique-resources 3
           :avg-durability 12.5
           :avg-construct-time 100.0}
 :by-resource {"_STONE" [{...}]
               "_WOOD" [{...}]
               nil [{...}]}    ;; Structures with no resource
 :structures [{:key "_MUD"
               :index 0
               :name "Mud"
               :description "Basic mud construction"
               :name-wall "Mud Wall"
               :name-ceiling "Mud Ceiling"
               :durability 5.0
               :construct-time 50.0
               :resource-key nil
               :resource-amount 0
               :has-resource false
               :minimap-color {:red 139 :green 90 :blue 43}
               :icon-path "sprites/structures/_MUD/icon.png"}
              {:key "_STONE"
               :index 1
               :name "Stone"
               :description "Durable stone construction"
               :name-wall "Stone Wall"
               :name-ceiling "Stone Ceiling"
               :durability 20.0
               :construct-time 150.0
               :resource-key "_STONE"
               :resource-amount 4
               :has-resource true
               :minimap-color {:red 128 :green 128 :blue 128}
               :icon-path "sprites/structures/_STONE/icon.png"}
              ...]}
```

## Utility Functions

### Comparison

```clojure
;; Compare two structures
(struct-extract/compare-structures "_STONE" "_WOOD")
;; => {:structure-1 {...}
;;     :structure-2 {...}
;;     :comparison {:durability-diff 5.0
;;                  :time-diff 25.0}}

;; List by durability (highest first)
(struct-extract/list-structures-by-durability)

;; List by construction time (fastest first)
(struct-extract/list-structures-by-construct-time)
```

## Notes

- Structure data requires the game to be initialized
- The `_MUD` structure is always present as a fallback/default
- Structures are loaded from `PATHS.SETT().folder("structure")`
- The `durability` value is scaled by `C.TILE_SIZE` constant
- Some structures may have no resource requirement (like `_MUD`)
- Structures define both wall and ceiling names for UI display
- The `tint` color is used for building customization in-game
- The `miniColor` determines how structures appear on the minimap

## Related Documentation

- `doc/wiki/extract/resources.md` - Resource extraction (materials used by structures)
- `doc/wiki/extract/buildings.md` - Building extraction (rooms that use structures)
- `doc/src-code/camera_and_building.md` - Building placement system

