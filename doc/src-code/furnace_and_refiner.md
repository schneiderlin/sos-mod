# Building a Hearth (火炉), Well (水井), and Smelter (冶金厂)

This guide explains how to programmatically build a hearth (火炉), well (水井), and smelter (冶金厂) in Songs of Syx using Clojure code.

## Overview

**Important Distinction:**
- **火炉 (Hearth)** - A health service room (`ROOM_HEARTH`) that provides warmth and comfort. Located in the "健康" (Health) category.
- **水井 (Well)** - A health service room (`ROOM_WELL`) that provides water and hygiene. Located in the "健康" (Health) category. **Fixed at 3x3, must use STONE material.**
- **冶金厂 (Smelter)** - An industrial refiner room (`ROOM_REFINER_SMELTER`) that processes ore into metal. Located in the "Refiner" category.

### Hearth (火炉) - Health Service Room

The hearth is a health service room that provides warmth and comfort to citizens. It supports multiple sizes (5x3, 7x7, etc.) and can use different materials (WOOD, STONE). It's part of the health service system.

### Well (水井) - Health Service Room

The well is a health service room that provides water and hygiene to citizens. It's **fixed at 3x3** and **must use STONE material**. It's part of the health service system.

### Smelter (冶金厂) - Refiner Room

Smelters are refiner rooms that process raw materials (like ore) into refined products (like metal). They are part of the `ROOM_REFINER` system, which includes several types:

- **SMELTER** - Smelter for smelting ore into metal (冶金厂)
- **BAKERY** - Bakes bread
- **BREWERY** - Brews beverages
- **COALER** - Processes coal
- **WEAVER** - Weaves fabric

## Key Classes

### For Hearth (火炉)
- `settlement.room.service.hearth.ROOM_HEARTH` - Hearth room blueprint class
- `settlement.room.service.hearth.HearthInstance` - Individual hearth instance
- `settlement.room.service.hearth.Constructor` - Constructor for hearth placement
- `settlement.main.SETT.ROOMS().HEARTH` - The hearth room blueprint

### For Well (水井)
- `settlement.room.service.hygine.well.ROOM_WELL` - Well room blueprint class
- `settlement.room.service.hygine.well.WellInstance` - Individual well instance
- `settlement.room.service.hygine.well.Constructor` - Constructor for well placement
- `settlement.main.SETT.ROOMS().WELLS` - List of well room blueprints (typically one)

### For Smelter (冶金厂)
- `settlement.room.industry.refiner.ROOM_REFINER` - Base refiner room blueprint class
- `settlement.room.industry.refiner.RefinerInstance` - Individual refiner instance
- `settlement.room.industry.refiner.Constructor` - Constructor for refiner placement
- `settlement.main.SETT.ROOMS().REFINERS` - List of all refiner room types
- `settlement.main.SETT.ROOMS().collection` - Collection for looking up rooms by key

## Building a Hearth (火炉)

The hearth is a health service room that provides warmth and comfort. It's accessed directly via `SETT.ROOMS().HEARTH`. Unlike simple 1x1 rooms, hearths support multiple sizes and automatically place furniture based on the area size.

### Creating a Hearth

```clojure
(ns game.hearth
  (:require 
   [repl.utils :as utils]
   [game.common :refer [get-building-material]])
  (:import 
   [settlement.main SETT]
   [settlement.room.main.construction ConstructionInit]))

;; Get the hearth room blueprint
(defn get-hearth []
  (let [rooms (SETT/ROOMS)]
    (.-HEARTH rooms)))

;; Select furniture size based on area
;; Hearth has 3 furniture sizes: 6x6 (size 0), 10x10 (size 1), 14x14 (size 2)
;; We choose the largest size that fits within the area
(defn select-hearth-furniture-size [area-width area-height]
  ;; Furniture sizes: 6x6=36, 10x10=100, 14x14=196
  (let [sizes [{:size 0 :width 6 :height 6}
               {:size 1 :width 10 :height 10}
               {:size 2 :width 14 :height 14}]
        fitting-sizes (filter (fn [{:keys [width height]}]
                                (and (<= width area-width) (<= height area-height)))
                              sizes)]
    (if (empty? fitting-sizes)
      0  ; Default to smallest size
      (:size (last (sort-by :size fitting-sizes))))))

;; Create a hearth at specified location
;; center-x, center-y: center tile coordinates
;; width, height: dimensions of the hearth (e.g., 5x3)
;; The function automatically selects and places the appropriate furniture item based on area size
;; material-name: building material name (e.g., "WOOD", "STONE")
;; upgrade: upgrade level (default 0)
(defn create-hearth [center-x center-y width height & {:keys [material-name upgrade] 
                                                        :or {material-name "WOOD" upgrade 0}}]
  (let [rooms (SETT/ROOMS)
        hearth-blueprint (get-hearth)
        _ (when (nil? hearth-blueprint)
            (throw (Exception. "Could not find HEARTH room. Make sure the game has loaded.")))
        hearth-constructor (.constructor hearth-blueprint)
        tbuilding (get-building-material material-name)
        construction-init (ConstructionInit. upgrade hearth-constructor tbuilding 0 nil)
        tmp (.tmpArea rooms "hearth")
        
        ;; Get furniture group and select appropriate size
        furnisher-groups (.pgroups hearth-constructor)
        first-group (when (> (.size furnisher-groups) 0)
                      (.get furnisher-groups 0))
        furniture-size (select-hearth-furniture-size width height)
        furnisher-item (when first-group
                         (.item first-group furniture-size 0))  ; rot=0
        _ (when (nil? furnisher-item)
            (throw (Exception. "Could not get furniture item for hearth")))
        
        ;; Calculate furniture placement position (center of the area)
        start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))
        furniture-x (- center-x (quot (.width furnisher-item) 2))
        furniture-y (- center-y (quot (.height furnisher-item) 2))]
    
    ;; Set the building area
    (doseq [y (range height)
            x (range width)]
      (.set tmp (+ start-x x) (+ start-y y)))
    
    ;; Place furniture BEFORE createClean (this is the key step!)
    ;; This matches how the game menu creates hearths - furniture is placed first
    (let [fdata (.fData rooms)
          room-instance (.room tmp)]
      (.itemSet fdata furniture-x furniture-y furnisher-item room-instance))
    
    ;; Create the construction site
    (.createClean (.construction rooms) tmp construction-init)
    
    ;; Clear temporary area
    (.clear tmp)
    
    {:success true
     :center-x center-x
     :center-y center-y
     :width width
     :height height
     :room-type "HEARTH"
     :furniture-size furniture-size
     :furniture-width (.width furnisher-item)
     :furniture-height (.height furnisher-item)}))

;; Create using update-once
(defn create-hearth-once [center-x center-y width height & {:keys [material-name upgrade] 
                                                              :or {material-name "WOOD" upgrade 0}}]
  (utils/update-once 
   (fn [_ds]
     (create-hearth center-x center-y width height 
                   :material-name material-name 
                   :upgrade upgrade))))
```

**Source References:**
- `settlement.main.SETT.ROOMS().HEARTH` - Line 165 in `ROOMS.java`
- `settlement.room.service.hearth.ROOM_HEARTH` - Hearth room class
- `settlement.room.service.hearth.Constructor` - Constructor (lines 112-144 show multiple furniture items of different sizes)
  - 6x6 furniture item (size 0) - 3x3 tiles
  - 10x10 furniture item (size 1) - 5x5 tiles
  - 14x14 furniture item (size 2) - 7x7 tiles
  - 28x28 furniture item (size 3) - 7x7 tiles, wider pattern (not used in placement groups)
- `settlement.room.main.placement.PlacerItemSingle.place()` - Shows how menu places furniture before creating room

**Important Notes:**
- **Furniture Placement**: Unlike some rooms, hearths require furniture to be placed **before** calling `createClean()`. This matches the game menu's behavior.
- **Automatic Size Selection**: The function automatically selects the largest furniture size that fits within the specified area:
  - Size 0 (6x6) for areas >= 6x6
  - Size 1 (10x10) for areas >= 10x10
  - Size 2 (14x14) for areas >= 14x14
- **Multiple Sizes**: Hearth supports multiple sizes - you can create 5x3, 7x7, or any size that fits the furniture
- Even though `usesArea()` returns `false`, you still need to set an area in `TmpArea`
- Common sizes: 5x3 (uses 10x10 furniture item), 7x7 (uses 14x14 furniture item), etc.
- Hearth is a **health service room** - Provides warmth and comfort
- Can be built **outdoors** - `mustBeIndoors()` returns `false`

### Example Usage

```clojure
;; Create a 5x3 hearth at center (271, 430) using wood
;; Automatically selects size 1 (10x10 furniture item) and places it
(create-hearth-once 271 430 5 3)
;; Returns: {:success true, :furniture-size 1, :furniture-width 10, :furniture-height 10, ...}

;; Create a larger hearth (7x7)
;; Automatically selects size 2 (14x14 furniture item) and places it
(create-hearth-once 300 400 7 7)
;; Returns: {:success true, :furniture-size 2, :furniture-width 14, :furniture-height 14, ...}

;; Create a hearth using stone
(create-hearth-once 250 250 5 3 :material-name "STONE")

;; Create a small hearth (6x6)
;; Automatically selects size 0 (6x6 furniture item)
(create-hearth-once 200 200 6 6)
;; Returns: {:success true, :furniture-size 0, :furniture-width 6, :furniture-height 6, ...}
```

### How It Works

The hearth creation process follows the same pattern as the game menu:

1. **Select Furniture Size**: Based on the area dimensions, the function selects the largest furniture item that fits (6x6, 10x10, or 14x14)
2. **Set Building Area**: The `TmpArea` is set to cover the specified width and height
3. **Place Furniture**: **Before** creating the construction site, the furniture is placed using `fData.itemSet()`. This is crucial - furniture must be placed first!
4. **Create Construction**: Finally, `createClean()` is called to create the construction site with the furniture already in place

This matches the behavior of `PlacerItemSingle.place()` in the game's placement system, ensuring that programmatically created hearths behave identically to menu-created ones.

## Building a Well (水井)

The well is a health service room that provides water and hygiene to citizens. It's accessed via `SETT.ROOMS().WELLS` (a list, typically containing one well type). Unlike hearths, wells are **fixed at 3x3** and **must use STONE material**.

### Important Notes About Wells

- **Fixed Size**: Wells are always 3x3 - you cannot specify different dimensions
- **Required Material**: Wells must use STONE material (not WOOD or other materials)
- **Game Bug**: The game's `Constructor.java` defines 3 furniture sizes (3x3, 5x5, 6x6), but wells should only be 3x3. The game appears to have a bug where it allows 5x5 and 6x6 wells, but this is incorrect behavior. Our code enforces 3x3 only.
- **Health Service Room**: Provides water and hygiene services to citizens
- **Can be built outdoors**: `mustBeIndoors()` returns `false`

### Creating a Well

```clojure
(ns game.well
  (:require 
   [repl.utils :as utils]
   [game.common :refer [get-building-material]])
  (:import 
   [settlement.main SETT]
   [settlement.room.main.construction ConstructionInit]))

;; Get the first well room blueprint
;; WELLS is a LIST<ROOM_WELL>, typically contains one well type
(defn get-well []
  (let [rooms (SETT/ROOMS)
        wells (.-WELLS rooms)]
    (when (> (.size wells) 0)
      (.get wells 0))))

;; Create a well at specified location
;; center-x, center-y: center tile coordinates
;; material-name: building material name (must be "STONE", default "STONE")
;; upgrade: upgrade level (default 0)
;;
;; NOTE: Well is fixed at 3x3 and must use STONE material.
;; The game's Constructor.java has a bug where it allows 5x5 and 6x6 wells,
;; but this is incorrect - wells should only be 3x3. We enforce this restriction.
(defn create-well [center-x center-y & {:keys [material-name upgrade] 
                                         :or {material-name "STONE" upgrade 0}}]
  ;; Validate material - must be STONE
  (when-not (= (.toUpperCase material-name) "STONE")
    (throw (Exception. (str "Well must use STONE material, got " material-name))))
  
  (let [width 3  ; Fixed size
        height 3  ; Fixed size
        rooms (SETT/ROOMS)
        well-blueprint (get-well)
        _ (when (nil? well-blueprint)
            (throw (Exception. "Could not find WELL room. Make sure the game has loaded.")))
        well-constructor (.constructor well-blueprint)
        tbuilding (get-building-material material-name)
        construction-init (ConstructionInit. upgrade well-constructor tbuilding 0 nil)
        tmp (.tmpArea rooms "well")
        
        ;; Get furniture group - always use size 0 (3x3 furniture)
        furnisher-groups (.pgroups well-constructor)
        first-group (when (> (.size furnisher-groups) 0)
                      (.get furnisher-groups 0))
        furniture-size 0  ; Always 3x3
        furnisher-item (when first-group
                         (.item first-group furniture-size 0))  ; rot=0
        _ (when (nil? furnisher-item)
            (throw (Exception. "Could not get furniture item for well")))
        
        ;; Calculate furniture placement position (center of the area)
        start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))
        furniture-x (- center-x (quot (.width furnisher-item) 2))
        furniture-y (- center-y (quot (.height furnisher-item) 2))]
    
    ;; Set the building area (always 3x3)
    (doseq [y (range height)
            x (range width)]
      (.set tmp (+ start-x x) (+ start-y y)))
    
    ;; Place furniture BEFORE createClean (this is the key step!)
    (let [fdata (.fData rooms)
          room-instance (.room tmp)]
      (.itemSet fdata furniture-x furniture-y furnisher-item room-instance))
    
    ;; Create the construction site
    (.createClean (.construction rooms) tmp construction-init)
    
    ;; Clear temporary area
    (.clear tmp)
    
    {:success true
     :center-x center-x
     :center-y center-y
     :width width
     :height height
     :room-type "WELL"
     :furniture-size furniture-size
     :furniture-width (.width furnisher-item)
     :furniture-height (.height furnisher-item)}))

;; Create a well using update-once (ensures it happens in a single frame)
;; Well is fixed at 3x3 and must use STONE material
(defn create-well-once [center-x center-y & {:keys [material-name upgrade] 
                                              :or {material-name "STONE" upgrade 0}}]
  (utils/update-once 
   (fn [_ds]
     (create-well center-x center-y 
                  :material-name material-name 
                  :upgrade upgrade))))
```

**Source References:**
- `settlement.main.SETT.ROOMS().WELLS` - Line 415 in `ROOMS.java`
- `settlement.room.service.hygine.well.ROOM_WELL` - Well room class
- `settlement.room.service.hygine.well.Constructor` - Constructor (defines 3 furniture sizes but only 3x3 should be used)
- `settlement.room.service.hygine.well.WellInstance` - Individual well instance

**Important Notes:**
- **Fixed Size**: Wells are always 3x3 - no width/height parameters needed
- **Required Material**: Must use STONE material (enforced by validation)
- **Furniture Placement**: Like hearths, wells require furniture to be placed **before** calling `createClean()`
- **Game Bug**: The game's constructor allows 5x5 and 6x6 wells, but this is a bug. Our code enforces 3x3 only.
- **Health Service Room**: Provides water and hygiene services
- **Can be built outdoors**: `mustBeIndoors()` returns `false`

### Example Usage

```clojure
;; Create a well at center (277, 433) using stone (required)
;; Well is fixed at 3x3 and must use STONE material
(create-well-once 277 433)

;; Create a well with explicit stone material (redundant but allowed)
(create-well-once 281 433 :material-name "STONE")

;; Get well info
(let [well (get-well)
      info (.-info well)
      constructor (.constructor well)
      pgroups (.pgroups constructor)]
  {:name (.toString (.-name info))
   :desc (.toString (.desc info))
   :key (.key well)
   :uses-area (.usesArea constructor)
   :must-be-indoors (.mustBeIndoors constructor)
   :num-furniture-groups (.size pgroups)})
```

### How It Works

The well creation process is simpler than hearths because wells are fixed size:

1. **Validate Material**: Ensure STONE material is used (required)
2. **Set Building Area**: The `TmpArea` is set to 3x3 (fixed size)
3. **Select Furniture**: Always use size 0 (3x3 furniture item)
4. **Place Furniture**: **Before** creating the construction site, the furniture is placed using `fData.itemSet()`
5. **Create Construction**: Finally, `createClean()` is called to create the construction site

## Building a Smelter (冶金厂)

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

### Pattern 1: Simple Hearth Creation
```clojure
;; Create a 5x3 hearth at center coordinates
(create-hearth-once 200 200 5 3)
```

### Pattern 2: Hearth with Custom Material
```clojure
(create-hearth-once 200 200 5 3 :material-name "STONE")
```

### Pattern 3: Larger Hearth
```clojure
;; Create a 7x7 hearth
(create-hearth-once 250 250 7 7)
```

### Pattern 3: Simple Smelter Creation
```clojure
(create-smelter-once 200 200 5 5)
```

### Pattern 4: Smelter with Custom Material
```clojure
(create-smelter-once 200 200 5 5 :material-name "STONE")
```

### Pattern 5: Generic Refiner Creation
```clojure
(create-refiner-once "REFINER_SMELTER" 200 200 5 5)
(create-refiner-once "REFINER_BAKERY" 250 250 4 4)
```

### Pattern 6: Finding Rooms
```clojure
;; Get hearth
(get-hearth)

;; Find refiner by key
(find-refiner-by-key "REFINER_SMELTER")

;; List all refiner types
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

