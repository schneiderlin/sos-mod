# Building a Furnace (火炉) / Smelter

This guide explains how to programmatically build a furnace (smelter) in Songs of Syx using Clojure code. In the game, furnaces are part of the "Refiner" room category, specifically the `ROOM_REFINER_SMELTER`.

## Overview

Furnaces (smelters) in Songs of Syx are refiner rooms that process raw materials (like ore) into refined products (like metal). They are part of the `ROOM_REFINER` system, which includes several types:

- **SMELTER** - Furnace for smelting ore into metal (火炉)
- **BAKERY** - Bakes bread
- **BREWERY** - Brews beverages
- **COALER** - Processes coal
- **WEAVER** - Weaves fabric

## Key Classes

- `settlement.room.industry.refiner.ROOM_REFINER` - Base refiner room blueprint class
- `settlement.room.industry.refiner.RefinerInstance` - Individual refiner instance
- `settlement.room.industry.refiner.Constructor` - Constructor for refiner placement
- `settlement.main.SETT.ROOMS().REFINERS` - List of all refiner room types
- `settlement.main.SETT.ROOMS().collection` - Collection for looking up rooms by key

## Finding the Smelter Room Blueprint

Refiner rooms are stored in `SETT.ROOMS().REFINERS` as a `LIST<ROOM_REFINER>`. To find the smelter specifically, you need to search by key:

```clojure
(ns game.refiner
  (:import 
   [settlement.main SETT]))

;; Find a specific refiner by key (e.g., "SMELTER", "BAKERY", "BREWERY")
(defn find-refiner-by-key [key-name]
  (let [refiners (.-REFINERS (SETT/ROOMS))
        key-upper (.toUpperCase key-name)]
    (first (filter #(= key-upper (.key %)) refiners))))

;; Get the smelter specifically
(defn get-smelter []
  (find-refiner-by-key "SMELTER"))

;; Alternative: Use the collection lookup
(defn get-smelter-via-collection []
  (let [rooms (SETT/ROOMS)
        collection (.-collection rooms)]
    (.tryGet collection "ROOM_REFINER_SMELTER")))
```

**Source References:**
- `settlement.main.SETT.ROOMS().REFINERS` - Line 287 in `ROOMS.java`
- `settlement.main.SETT.ROOMS().collection` - Line 552 in `ROOMS.java`
- `settlement.room.main.RoomBlueprint.key()` - Returns the room's key string
- Room keys follow the pattern: `"ROOM_REFINER_" + type` (e.g., `"ROOM_REFINER_SMELTER"`)

## Understanding Refiner Room Requirements

Refiner rooms (including smelters) have specific requirements:

1. **Size**: Refiners use area-based placement (minimum 3x3 tiles)
2. **Indoors**: Must be built indoors (requires walls/roof)
3. **Furniture**: Requires specific furniture items (machines, work stations, storage)
4. **Materials**: Can be built with different materials (wood, stone, etc.)

**Source References:**
- `settlement.room.industry.refiner.Constructor` - Lines 45-47 in `Constructor.java`
  - Constructor extends `Furnisher` with dimensions 3x3 minimum
- `Furnisher.mustBeIndoors()` - Check if room must be indoors
- `Furnisher.usesArea()` - Check if room uses area-based placement

## Creating a Smelter

Here's a complete example of creating a smelter (furnace):

```clojure
(ns game.refiner
  (:require 
   [repl.utils :as utils])
  (:import 
   [settlement.main SETT]
   [settlement.room.main.construction ConstructionInit]
   [init.structure STRUCTURES]
   [init.resources RESOURCES]))

;; Get building material (same as warehouse creation)
(defn get-building-material [material-name]
  (let [material-upper (.toUpperCase material-name)
        resource (case material-upper
                   "WOOD" (RESOURCES/WOOD)
                   "STONE" (RESOURCES/STONE)
                   (throw (Exception. (str "Unknown material: " material-name ". Supported: WOOD, STONE"))))
        all-structures (STRUCTURES/all)
        structure (first (filter #(= resource (.-resource %)) all-structures))]
    (if structure
      (let [buildings (.-BUILDINGS (SETT/TERRAIN))]
        (.get buildings structure))
      (throw (Exception. (str "Could not find structure for material: " material-name))))))

;; Find refiner by key
(defn find-refiner-by-key [key-name]
  (let [refiners (.-REFINERS (SETT/ROOMS))
        key-upper (.toUpperCase key-name)]
    (first (filter #(= key-upper (.key %)) refiners))))

;; Create a smelter at specified location
;; center-x, center-y: center tile coordinates
;; width, height: dimensions of the smelter (minimum 3x3)
;; material-name: building material name (e.g., "WOOD", "STONE")
;; upgrade: upgrade level (default 0)
(defn create-smelter [center-x center-y width height & {:keys [material-name upgrade] 
                                                        :or {material-name "WOOD" upgrade 0}}]
  (let [rooms (SETT/ROOMS)
        ;; Find the smelter room blueprint
        smelter-blueprint (find-refiner-by-key "SMELTER")
        _ (when (nil? smelter-blueprint)
            (throw (Exception. "Could not find SMELTER refiner. Make sure the game has loaded.")))
        smelter-constructor (.constructor smelter-blueprint)
        tbuilding (get-building-material material-name)  ; Returns TBuilding, not Structure
        degrade 0  ; No degradation
        state nil  ; No special state
        
        ;; Create ConstructionInit (note: third parameter is TBuilding, not Structure)
        construction-init (ConstructionInit. upgrade smelter-constructor tbuilding degrade state)
        
        ;; Get temporary area
        tmp (.tmpArea rooms "smelter")]
    
    ;; Set the building area
    (let [start-x (- center-x (quot width 2))
          start-y (- center-y (quot height 2))]
      (doseq [y (range height)
              x (range width)]
        (.set tmp (+ start-x x) (+ start-y y))))
    
    ;; Note: Refiner rooms may have furniture that needs to be placed
    ;; The game may auto-place some furniture, but you can manually place it if needed
    ;; See the furniture placement section below
    
    ;; Create the construction site
    (.createClean (.construction rooms) tmp construction-init)
    
    ;; Clear temporary area
    (.clear tmp)
    
    {:success true
     :center-x center-x
     :center-y center-y
     :width width
     :height height
     :room-type "SMELTER"}))

;; Create a smelter using update-once (ensures it happens in a single frame)
(defn create-smelter-once [center-x center-y width height & {:keys [material-name upgrade] 
                                                               :or {material-name "WOOD" upgrade 0}}]
  (utils/update-once 
   (fn [_ds]
     (create-smelter center-x center-y width height 
                    :material-name material-name 
                    :upgrade upgrade))))
```

**Important Notes:**
1. **Use `create-smelter-once`** - This ensures the construction happens in a single frame
2. **Minimum size** - Smelters require at least 3x3 tiles
3. **Must be indoors** - Smelters must be built inside with walls/roof
4. **Coordinates are tile coordinates** - Not pixel coordinates
5. **Construction happens asynchronously** - Workers will complete the construction

## Generic Refiner Creation Function

You can create a generic function to build any type of refiner:

```clojure
;; Create any refiner type (SMELTER, BAKERY, BREWERY, COALER, WEAVER)
(defn create-refiner [refiner-type center-x center-y width height & {:keys [material-name upgrade] 
                                                                        :or {material-name "WOOD" upgrade 0}}]
  (let [rooms (SETT/ROOMS)
        ;; Find the refiner by type
        refiner-blueprint (find-refiner-by-key refiner-type)
        _ (when (nil? refiner-blueprint)
            (throw (Exception. (str "Could not find refiner type: " refiner-type))))
        refiner-constructor (.constructor refiner-blueprint)
        tbuilding (get-building-material material-name)
        construction-init (ConstructionInit. upgrade refiner-constructor tbuilding 0 nil)
        tmp (.tmpArea rooms (str "refiner-" refiner-type))]
    
    ;; Set building area
    (let [start-x (- center-x (quot width 2))
          start-y (- center-y (quot height 2))]
      (doseq [y (range height)
              x (range width)]
        (.set tmp (+ start-x x) (+ start-y y))))
    
    ;; Create construction
    (.createClean (.construction rooms) tmp construction-init)
    (.clear tmp)
    
    {:success true
     :center-x center-x
     :center-y center-y
     :width width
     :height height
     :room-type refiner-type}))

;; Create using update-once
(defn create-refiner-once [refiner-type center-x center-y width height & {:keys [material-name upgrade] 
                                                                           :or {material-name "WOOD" upgrade 0}}]
  (utils/update-once 
   (fn [_ds]
     (create-refiner refiner-type center-x center-y width height 
                    :material-name material-name 
                    :upgrade upgrade))))
```

## Furniture Placement for Refiners

Refiner rooms have specific furniture requirements:

1. **Work stations** - Where workers perform the refining
2. **Storage areas** - Where input materials are stored
3. **Fetch areas** - Where output materials are placed
4. **Machines** - The main refining equipment

The refiner constructor defines these furniture items. You can inspect them:

```clojure
;; Get furniture information for a refiner
(defn get-refiner-furniture-info [refiner-type]
  (let [refiner-blueprint (find-refiner-by-key refiner-type)
        constructor (.constructor refiner-blueprint)
        pgroups (.pgroups constructor)]
    {:refiner-type refiner-type
     :num-groups (.size pgroups)
     :groups (map-indexed
              (fn [i group]
                {:group-index i
                 :num-items (try (.size group) (catch Exception _e 0))})
              pgroups)}))

;; Example: Get smelter furniture info
(comment
  (get-refiner-furniture-info "SMELTER")
  :rcf)
```

**Source References:**
- `settlement.room.industry.refiner.Constructor` - Lines 100-450 in `Constructor.java`
- `Furnisher.pgroups()` - Returns placement groups for furniture
- `FurnisherItemGroup.item()` - Gets furniture items from groups

## Example Usage

```clojure
(comment
  ;; Create a 5x5 smelter at tile (200, 200) using wood
  (create-smelter-once 200 200 5 5)
  
  ;; Create a 3x3 smelter using stone
  (create-smelter-once 250 250 3 3 :material-name "STONE")
  
  ;; Create a bakery (another refiner type)
  (create-refiner-once "BAKERY" 300 300 4 4)
  
  ;; Create a brewery
  (create-refiner-once "BREWERY" 350 350 4 4)
  
  ;; List all available refiner types
  (let [refiners (.-REFINERS (SETT/ROOMS))]
    (map #(.key %) refiners))
  ;; => ("ROOM_REFINER_SMELTER" "ROOM_REFINER_BAKERY" ...)
  
  :rcf)
```

## Available Refiner Types

Based on the booster list, the following refiner types are available:

- `ROOM_REFINER_SMELTER` - Furnace for smelting ore (火炉)
- `ROOM_REFINER_BAKERY` - Bakes bread
- `ROOM_REFINER_BREWERY` - Brews beverages
- `ROOM_REFINER_COALER` - Processes coal
- `ROOM_REFINER_WEAVER` - Weaves fabric

To find all available refiners at runtime:

```clojure
;; Get all refiner types
(defn all-refiner-types []
  (let [refiners (.-REFINERS (SETT/ROOMS))]
    (map #(.key %) refiners)))

;; Get refiner info
(defn all-refiner-info []
  (let [refiners (.-REFINERS (SETT/ROOMS))]
    (map (fn [r]
           {:key (.key r)
            :name (.toString (.. r info name))
            :desc (.toString (.. r info desc))})
         refiners)))
```

## Building Walls Around the Smelter

Since smelters must be indoors, you'll need to build walls around them. You can use the same wall-building pattern as warehouses:

```clojure
(ns game.refiner
  (:import 
   [settlement.room.main.placement UtilWallPlacability]))

;; Build walls around a smelter (similar to warehouse wall building)
(defn build-walls-around-smelter [center-x center-y width height & {:keys [material-name] 
                                                                      :or {material-name "WOOD"}}]
  (let [tbuilding (get-building-material material-name)
        start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))
        ;; Find edge tiles (same function as warehouse)
        edge-tiles (find-edge-tiles start-x start-y width height)]
    ;; Build walls on all edge tiles
    (doseq [[x y] edge-tiles]
      (when (UtilWallPlacability/wallCanBe x y)
        (UtilWallPlacability/wallBuild x y tbuilding)))))

;; Create smelter with walls
(defn create-smelter-with-walls-once [center-x center-y width height & {:keys [material-name upgrade] 
                                                                         :or {material-name "WOOD" upgrade 0}}]
  (utils/update-once 
   (fn [_ds]
     ;; Build walls first
     (build-walls-around-smelter center-x center-y width height :material-name material-name)
     ;; Then create the smelter
     (create-smelter center-x center-y width height 
                    :material-name material-name 
                    :upgrade upgrade))))
```

**Note:** You'll need the `find-edge-tiles` function from `tutorial1.clj` for this to work.

## Source Code References

- `sos-src/settlement/room/main/ROOMS.java` - Lines 287-295 (REFINERS list)
- `sos-src/settlement/room/industry/refiner/ROOM_REFINER.java` - Main refiner class
- `sos-src/settlement/room/industry/refiner/Constructor.java` - Refiner constructor
- `sos-src/settlement/room/industry/refiner/RefinerInstance.java` - Refiner instance
- `sos-src/settlement/room/main/construction/ConstructionInit.java` - Construction parameters
- `doc/booster/boosters_all.md` - Line 103 (ROOM_REFINER_SMELTER)

## Common Patterns

### Pattern 1: Simple Smelter Creation
```clojure
(create-smelter-once 200 200 5 5)
```

### Pattern 2: Smelter with Custom Material
```clojure
(create-smelter-once 200 200 5 5 :material-name "STONE")
```

### Pattern 3: Generic Refiner Creation
```clojure
(create-refiner-once "SMELTER" 200 200 5 5)
(create-refiner-once "BAKERY" 250 250 4 4)
```

### Pattern 4: Finding Refiners
```clojure
;; Find by key
(find-refiner-by-key "SMELTER")

;; List all types
(all-refiner-types)
```

## Troubleshooting

### Smelter Not Found
- Make sure the game has fully loaded
- Check that the key is correct: `"SMELTER"` (not `"ROOM_REFINER_SMELTER"` when using `find-refiner-by-key`)
- Try using `collection.tryGet("ROOM_REFINER_SMELTER")` instead

### Construction Not Appearing
- Ensure coordinates are within settlement bounds
- Check that the area is clear and suitable
- Verify that workers are available
- Make sure you're using `create-smelter-once` or wrapping in `update-once`

### Room Must Be Indoors Error
- Build walls around the smelter first
- Ensure there's a roof (walls provide roof automatically)
- Check that `Furnisher.mustBeIndoors()` returns `true` for refiners

## Next Steps

1. **Furniture placement** - Manually place furniture items for optimal layout
2. **Wall and door placement** - Automate wall building around smelters
3. **Refiner management** - Query refiner instances, check production status
4. **Resource management** - Check input/output resources for refiners
5. **Automation** - Create automated smelter placement scripts

