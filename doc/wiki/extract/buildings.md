# Buildings/Rooms Extraction

This document describes the data extraction for Buildings (Rooms) in Songs of Syx.

## Overview

Buildings in Songs of Syx are called "Rooms" internally. They include all placeable structures from farms and mines to services like taverns and hospitals.

## Source Code Locations

### Game Classes
- `settlement.room.main.ROOMS` - Main registry containing all room types
- `settlement.room.main.RoomBlueprint` - Base class for room blueprints
- `settlement.room.main.RoomBlueprintImp` - Implementation with detailed info (name, desc, category, icon)
- `settlement.room.main.RoomBlueprintIns` - Instance-based blueprints
- `settlement.room.main.furnisher.Furnisher` - Construction info (resources, costs)
- `settlement.room.main.category.RoomCategories` - Category definitions
- `settlement.room.industry.module.Industry` - Production info (inputs/outputs)

### Clojure Files
- **Game API**: `src/game/building.clj`
- **Extraction**: `src/extract/building.clj`

## Data Available

### Room Properties

| Property | Source | Type | Description |
|----------|--------|------|-------------|
| key | `RoomBlueprint.key` | String | Unique identifier (e.g., "FARM", "REFINER_SMELTER") |
| index | `RoomBlueprint.index()` | Integer | Index in ALL list |
| name | `RoomBlueprintImp.info.name` | String | Display name |
| description | `RoomBlueprintImp.info.desc` | String | Description text |
| type | `RoomBlueprintImp.type` | String | Room type (e.g., "REFINER", "FARM") |
| type-index | `RoomBlueprintImp.typeIndex()` | Integer | Index within type |
| degrade-rate | `RoomBlueprintImp.degradeRate()` | Double | Decay rate (0-1) |
| bonus | `RoomBlueprintImp.bonus()` | Boostable | Associated boostable (if any) |

### Category Properties

| Property | Source | Type | Description |
|----------|--------|------|-------------|
| sub-name | `RoomCategorySub.name` | String | Sub-category name |
| main-name | `RoomCategoryMain.name` | String | Main category name |
| rooms | `RoomCategorySub.rooms()` | LIST | Rooms in category |

### Construction Properties (Furnisher)

| Property | Source | Type | Description |
|----------|--------|------|-------------|
| resources | `Furnisher.resource(i)` | RESOURCE[] | Required resources |
| area-cost | `Furnisher.areaCost(i, upgrade)` | Double[] | Cost per area unit |
| uses-area | `Furnisher.usesArea()` | Boolean | Area-based construction |
| must-be-indoors | `Furnisher.mustBeIndoors()` | Boolean | Requires roof |
| must-be-outdoors | `Furnisher.mustBeOutdoors()` | Boolean | Open air only |
| floors | `Furnisher.floors` | LIST | Available floor types |

### Production Properties (Industry)

| Property | Source | Type | Description |
|----------|--------|------|-------------|
| inputs | `Industry.ins()` | LIST | Input resources |
| outputs | `Industry.outs()` | LIST | Output resources |
| rate-per-second | `IndustryResource.rateSeconds` | Double | Production rate |
| ai-multiplier | `Industry.AIMul` | Double | AI bonus multiplier |

## Room Categories

### Main Categories
1. **Agriculture** - Farms, Husbandry, Aquaculture
2. **Work** - Mines, Refiners, Crafting
3. **Service** - Religion, Distribution, Health, Entertainment, Death, Home
4. **Government** - Admin, Law, Military, Breeding, Logistics, Water, Decor

### Sub-Categories
- Mines, Refiners, Crafting
- Farms, Husbandry (Pastures), Aquaculture (Fisheries)
- Law, Military, Admin, Breeding
- Health, Entertainment, Religion, Death
- Logistics, Water, Decor, Home

## Room Types

Common room types include:
- `FARM` - Agricultural farms
- `REFINER` - Resource refiners (smelters, etc.)
- `WORKSHOP` - Crafting workshops
- `MINE` - Mining operations
- `PASTURE` - Animal pastures
- `FISHERY` - Fishing operations
- And many more...

## Usage Examples

### Basic Access

```clojure
(require '[game.building :as build])

;; Get all room blueprints
(build/all-blueprints)
(build/blueprint-count)

;; Get detailed room implementations
(build/all-blueprint-imps)
(build/blueprint-imp-count)

;; Get first room info
(let [room (first (build/all-blueprint-imps))]
  (println "Key:" (build/blueprint-key room))
  (println "Name:" (build/room-name room))
  (println "Type:" (build/room-type room)))
```

### Convert to Map

```clojure
;; Single room
(build/room-imp->map (first (build/all-blueprint-imps)))

;; All rooms
(build/all-rooms-as-maps)
```

### Queries

```clojure
;; Find specific room
(build/find-room-by-key "FARM")

;; Find rooms by type
(build/find-rooms-by-type "REFINER")

;; Get production rooms
(build/production-rooms)

;; Find producers of a resource
(build/find-rooms-producing "BREAD")

;; Find consumers of a resource
(build/find-rooms-consuming "_STONE")
```

### Grouping

```clojure
;; By category
(build/rooms-by-category)

;; By type
(build/rooms-by-type)
```

## Extraction

### Run Extraction

```clojure
(require '[extract.building :as build-extract])

;; Summary
(build-extract/extract-buildings-summary)

;; Production report
(build-extract/production-report)

;; Extract to files
(build-extract/extract-all "output/wiki")
```

### Output Files

- `output/wiki/data/buildings.edn` - All buildings data
- `output/wiki/data/production.edn` - Production-focused data

## Output Schema

### buildings.edn

```clojure
{:version "1.0"
 :extracted-at "2026-01-15T..."
 :summary {:total-blueprints 100
           :total-rooms 95
           :production-rooms 40
           :categories-count 20
           :types-count 15}
 :categories [{:name "Farms"
               :room-count 5
               :room-keys ["FARM" "FARM_COTTON" ...]}
              ...]
 :types [{:type "REFINER"
          :room-count 8
          :room-keys ["REFINER_SMELTER" ...]}
         ...]
 :rooms [{:key "FARM"
          :index 0
          :name "Farm"
          :description "..."
          :type "FARM"
          :type-index 0
          :degrade-rate 0.75
          :has-bonus true
          :category {:name "Farms"
                     :main-name "Agriculture"
                     :room-count 5}
          :construction {:uses-area true
                         :must-be-indoors false
                         :must-be-outdoors true
                         :resources [{:resource-key "_STONE"
                                      :area-cost 0.5}]}
          :industries nil}
         {:key "REFINER_SMELTER"
          ...
          :industries [{:index 0
                        :ai-multiplier 1.0
                        :inputs [{:resource-key "COAL"
                                  :rate-per-second 0.01}
                                 {:resource-key "IRON_ORE"
                                  :rate-per-second 0.01}]
                        :outputs [{:resource-key "METAL"
                                   :rate-per-second 0.005}]}]}
         ...]}
```

## Notes

- Room data requires the game to be initialized (not just launched)
- Production data is only available for rooms implementing `INDUSTRY_HASER`
- Some rooms have multiple industries (recipes) - like workshops
- Construction costs are per-area for area-based rooms
- The `bonus` boostable connects rooms to the boosting system

## Related Documentation

- `doc/src-code/warehouses_and_storage.md` - Storage system
- `doc/src-code/furnace_and_refiner.md` - Refiner details
- `doc/wiki/extract/resources.md` - Resource extraction
- `doc/wiki/extract/technologies.md` - Technology extraction

