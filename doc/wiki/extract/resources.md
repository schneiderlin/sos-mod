# Resources Extraction

## Overview

Resources are all items/materials in the game - from raw materials (stone, wood) to processed goods (bread, tools).

## Implementation Status

| Component | Location | Status |
|-----------|----------|--------|
| Game API | `src/game/resource.clj` | ✅ Done |
| Extraction | `src/extract/resource_extract.clj` | ✅ Done |
| Icon Export | - | ⏳ Pending |

## Java Classes

```
init.resources.RESOURCES     - Resource registry (static access)
init.resources.RESOURCE      - Individual resource (extends INFO)
init.resources.Minable       - Mineable terrain resources
init.resources.Growable      - Growable crops/plants
init.resources.ResGDrink     - Drinkable resources
init.resources.ResGEat       - Edible resources
init.resources.Sprite        - Resource sprite (carry, lay, color)
util.info.INFO               - Name, names, description
```

## Clojure API

### Game Functions (`game.resource`)

```clojure
(require '[game.resource :as res])

;; Get all resources
(res/all-resources)        ; => LIST<RESOURCE>
(res/resource-count)       ; => int

;; Special resources
(res/stone)                ; => RESOURCE
(res/wood)                 ; => RESOURCE
(res/livestock)            ; => RESOURCE

;; Get by key
(res/get-resource "BREAD") ; => RESOURCE or nil

;; Resource properties
(res/resource-key res)           ; => "BREAD"
(res/resource-name res)          ; => "Bread"
(res/resource-desc res)          ; => "Basic food..."
(res/resource-category res)      ; => 0-9
(res/resource-degrade-speed res) ; => 0.0-1.0

;; Groups
(res/minable-list)         ; => all minables
(res/growable-list)        ; => all growables  
(res/edible-list)          ; => all edibles
(res/drink-list)           ; => all drinkables

;; Predicates
(res/edible? res)          ; => boolean
(res/drinkable? res)       ; => boolean

;; Data conversion
(res/resource->map res)    ; => Clojure map
(res/all-resources-as-maps) ; => vector of maps
```

### Extraction Functions (`extract.resource`)

```clojure
(require '[extract.resource :as extract])

;; Quick summary
(extract/extract-resources-summary)

;; Extract to EDN
(extract/extract-resources-edn)           ; default: output/wiki/data
(extract/extract-resources-edn "my-dir")  ; custom dir

;; Queries
(extract/find-resource "BREAD")
(extract/list-resources-by-category)
(extract/list-edibles)

;; Full extraction (data + sprites)
(extract/extract-all)
```

## Data Fields

### RESOURCE

| Field | Method | Type | Description |
|-------|--------|------|-------------|
| key | `.key` | String | Unique ID (e.g., "BREAD") |
| index | `.index()` | int | Index in ALL list |
| name | `.name` (INFO) | CharSequence | Display name |
| names | `.names` (INFO) | CharSequence | Plural name |
| desc | `.desc` (INFO) | CharSequence | Description |
| category | `.category` | int | Category 0-9 |
| degradeSpeed | `.degradeSpeed()` | double | Decay rate/year |
| priceCapDef | `.priceCapDef` | double | Default price cap |
| priceMulDef | `.priceMulDef` | double | Default price mul |
| icon | `.icon()` | Icon | UI icon reference |

### Minable

| Field | Type | Description |
|-------|------|-------------|
| key | String | Unique ID |
| index | int | Index |
| resource | RESOURCE | Produced resource |
| name | CharSequence | Display name |
| onEverymap | boolean | Appears on all maps |
| occurence | double | Occurrence rate |
| fertilityIncrease | double | Fertility bonus |

### Growable

| Field | Type | Description |
|-------|------|-------------|
| key | String | Unique ID |
| index | int | Index |
| resource | RESOURCE | Produced resource |
| seasonalOffset | double | Growth season offset |
| growthValue | double | Growth rate |

## Output Schema

### resources.edn

```edn
{:version "1.0"
 :extracted-at "2026-01-15T..."
 
 :summary
 {:total-resources 45
  :total-minables 8
  :total-growables 12
  :total-edibles 15
  :total-drinkables 6
  :categories 4}
 
 :resources
 [{:key "_STONE"
   :index 0
   :name "Stone"
   :names "Stone"
   :description "Building material..."
   :category 0
   :degrade-speed 0.0
   :price-cap 1.0
   :price-mul 1.0
   :edible false
   :drinkable false
   :icon-path "sprites/resources/_STONE/icon.png"
   :sprite-path "sprites/resources/_STONE/lay.png"}
  ;; ...more resources
  ]
 
 :minables
 [{:key "STONE"
   :index 0
   :resource-key "_STONE"
   :name "Stone Deposits"
   :on-every-map true
   :occurence 1.0
   :fertility-increase 0.0}
  ;; ...more minables
  ]
 
 :growables
 [{:key "WHEAT"
   :index 0
   :resource-key "GRAIN"
   :seasonal-offset 0.5
   :growth-value 0.8}
  ;; ...more growables
  ]
 
 :edibles
 [{:key "BREAD"
   :index 0
   :resource-key "BREAD"
   :serve true}
  ;; ...more edibles
  ]
 
 :drinkables
 [{:key "WINE"
   :index 0
   :resource-key "WINE"
   :serve true}
  ;; ...more drinkables
  ]}
```

## Quick Start

```clojure
;; In REPL with game running:

;; 1. Require namespaces
(require '[game.resource :as res])
(require '[extract.resource :as extract])

;; 2. Check connection
(res/resource-count)  ; Should return number > 0

;; 3. Print summary
(extract/extract-resources-summary)

;; 4. Extract to file
(extract/extract-resources-edn)
;; => Saves to output/wiki/data/resources.edn

;; 5. View specific resource
(extract/find-resource "BREAD")
```

## Progress

- [x] Create `game.resource` namespace with accessor functions
- [x] Create `wiki.extract.resource` namespace for extraction
- [x] Resource properties extraction
- [x] Minable extraction
- [x] Growable extraction
- [x] Edible/Drinkable extraction
- [x] EDN output with summary
- [ ] Resource icon export (requires icon extraction support)
- [ ] Resource sprite export (lay/carry sprites)
- [ ] JSON output option
- [ ] Category name mapping

## Notes

1. **Fixed resources**: `_STONE`, `_WOOD`, `_LIVESTOCK` use underscore prefix
2. **Categories**: Numbers 0-9, need to map to actual names
3. **Resource sprites** have multiple parts:
   - `carry` - When being carried (8 directions)
   - `lay` - When on ground (16 variants by amount)
   - `debris` - Scattered debris effect
4. **Icons** come from `SPRITES.icons()`, need separate extraction
5. Game must be running/loaded to extract data
