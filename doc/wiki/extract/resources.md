# Resources Extraction

## Overview

Resources are all items/materials in the game - from raw materials (stone, wood) to processed goods (bread, tools).

## Java Classes

```
init.resources.RESOURCES     - Resource registry (static access)
init.resources.RESOURCE      - Individual resource
init.resources.Minable       - Mineable terrain resources
init.resources.Growable      - Growable crops/plants
init.resources.ResGDrink     - Drinkable resources
init.resources.ResGEat       - Edible resources
init.resources.Sprite        - Resource sprite reference
```

## Accessing Resources

```clojure
(import '[init.resources RESOURCES RESOURCE])

;; Get all resources
(RESOURCES/ALL)  ; => LIST<RESOURCE>

;; Special resources
(RESOURCES/STONE)     ; => RESOURCE
(RESOURCES/WOOD)      ; => RESOURCE  
(RESOURCES/LIVESTOCK) ; => RESOURCE

;; Resource groups
(RESOURCES/minables)  ; => RMAP<Minable>
(RESOURCES/growable)  ; => GrowableGroup
(RESOURCES/DRINKS)    ; => ResGroup<ResGDrink>
(RESOURCES/EDI)       ; => ResGroup<ResGEat>

;; Get resource by key
(.get (RESOURCES/map) "BREAD" nil)
```

## Data Fields

### RESOURCE class

| Field/Method | Type | Description |
|--------------|------|-------------|
| `.key` | String | Unique identifier (e.g., "BREAD") |
| `.index()` | int | Index in ALL list |
| `.info` | INFO | Name and description |
| `.icon()` | Icon | UI icon reference |
| `.cat()` | int | Category (0-9) |
| `.isEdible()` | boolean | Can be eaten |
| `.drinkable` | boolean | Can be drunk |
| `.growable()` | Growable | If growable crop |
| `.minable()` | Minable | If mineable terrain |

### Extraction Exploration (REPL)

```clojure
;; Explore a resource
(def bread (.get (RESOURCES/map) "BREAD" nil))

(.key bread)          ; => "BREAD"
(.index bread)        ; => 15
(.info bread)         ; => INFO object
(.name (.info bread)) ; => "Bread"
(.desc (.info bread)) ; => "..."
(.cat bread)          ; => category number
(.isEdible bread)     ; => true
```

## Output Schema

### resources.edn

```edn
{:resources
 [{:key "STONE"
   :index 0
   :name "Stone"
   :description "Building material..."
   :category 0
   :edible false
   :drinkable false
   :icon-path "sprites/resources/STONE.png"}
  
  {:key "BREAD"
   :index 15
   :name "Bread"
   :description "Basic food..."
   :category 2
   :edible true
   :drinkable false
   :growable nil
   :icon-path "sprites/resources/BREAD.png"}
  ;; ...
  ]
 
 :categories
 [{:id 0 :name "Raw Materials"}
  {:id 1 :name "Processed"}
  ;; ...
  ]
 
 :minables
 [{:key "STONE" :terrain "rock"}
  ;; ...
  ]
 
 :growables
 [{:key "WHEAT" :growth-time 120}
  ;; ...
  ]}
```

## Extraction Code (TODO)

```clojure
(ns game.resource
  (:import [init.resources RESOURCES RESOURCE]))

(defn resource->map
  "Convert RESOURCE to Clojure map"
  [^RESOURCE res]
  {:key (.key res)
   :index (.index res)
   :name (str (.name (.info res)))
   :description (str (.desc (.info res)))
   :category (.cat res)
   :edible (.isEdible res)
   :drinkable (.-drinkable res)})

(defn extract-all-resources
  "Extract all resources to EDN"
  []
  {:resources (mapv resource->map (RESOURCES/ALL))})

(defn save-resources-edn
  "Save resources to file"
  [output-path]
  (spit output-path (pr-str (extract-all-resources))))
```

## Progress

- [ ] Understand RESOURCE class structure
- [ ] Basic extraction function
- [ ] Extract resource icons
- [ ] Handle minables
- [ ] Handle growables
- [ ] Export to EDN/JSON
- [ ] Add to batch export

## Notes

1. Resource categories are numbered 0-9, need to find category names
2. Fixed resources (`_STONE`, `_WOOD`, `_LIVESTOCK`) are loaded first
3. Icons are from sprite sheets, need to export individually
4. Growables have growth stages with different sprites

