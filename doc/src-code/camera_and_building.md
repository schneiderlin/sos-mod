# Camera Control and Building Creation

This guide explains how to programmatically control the camera and create buildings (like warehouses) in Songs of Syx using Clojure code.

## Camera Control

The game camera is controlled through the `GameWindow` class, which is accessed via the `VIEW` singleton.

### Getting the Game Window

The game window is accessed through the settlement view:

```clojure
(ns my-mod
  (:import [view.main VIEW]))

(defn get-game-window []
  (let [sett-view (VIEW/s)]
    (.getWindow sett-view)))
```

**Source Reference:**
- `view.main.VIEW` - Main view singleton that provides access to different game views
- `view.subview.GameWindow` - The camera/window class that handles all camera operations
- See: `sos-src/view/subview/GameWindow.java`

### Moving the Camera

The `GameWindow` class provides several methods for camera movement:

#### 1. Move to Specific Position

```clojure
(defn move-camera-to [x y]
  (let [window (get-game-window)]
    (.centerAt window x y)))
```

This moves the camera to center on a specific pixel coordinate.

**Source Reference:**
- `GameWindow.centerAt(int x, int y)` - Lines 511-515 in `GameWindow.java`
- `GameWindow.centerAt(COORDINATE coo)` - Lines 523-525 in `GameWindow.java`

#### 2. Move to Tile Position

```clojure
(defn move-camera-to-tile [tile-x tile-y]
  (let [window (get-game-window)]
    (.centerAtTile window tile-x tile-y)))
```

This moves the camera to center on a specific tile coordinate (more convenient for game logic).

**Source Reference:**
- `GameWindow.centerAtTile(int tileX, int tileY)` - Lines 527-530 in `GameWindow.java`

#### 3. Move by Delta

```clojure
(defn move-camera-by [dx dy]
  (let [window (get-game-window)]
    (.inc window dx dy)))
```

This moves the camera by a relative amount (in pixels).

**Source Reference:**
- `GameWindow.inc(int x1, int y1)` - Lines 517-521 in `GameWindow.java`

#### 4. Directional Movement (WASD-like)

```clojure
(defn move-camera-direction [direction & {:keys [speed] :or {speed 100}}]
  (let [window (get-game-window)
        zoomout (.zoomout window)
        ;; Adjust speed based on zoom level (similar to how the game does it)
        adjusted-speed (* speed (Math/pow 2 zoomout))
        [dx dy] (case direction
                  :up [0 (- adjusted-speed)]
                  :down [0 adjusted-speed]
                  :left [(- adjusted-speed) 0]
                  :right [adjusted-speed 0]
                  [0 0])]
    (.inc window dx dy)))
```

**Source Reference:**
- The game's internal camera movement uses `KEYS.MAIN().SCROLL_UP/DOWN/LEFT/RIGHT` - Lines 301-319 in `GameWindow.java`
- Speed is automatically adjusted based on zoom level - Lines 295-296 in `GameWindow.java`

### Camera Zoom

The camera zoom is controlled through the `zoomout` level, where:
- `0` = normal zoom
- Positive values = zoomed out
- Negative values = zoomed in

#### Get Current Zoom

```clojure
(defn get-zoom []
  (let [window (get-game-window)]
    (.zoomout window)))
```

**Source Reference:**
- `GameWindow.zoomout()` - Line 218 in `GameWindow.java`

#### Set Zoom Level

```clojure
(defn set-zoom [level]
  (let [window (get-game-window)]
    (.setZoomout window level)))
```

**Source Reference:**
- `GameWindow.setZoomout(int pow2)` - Lines 180-208 in `GameWindow.java`

#### Zoom In/Out

```clojure
(defn zoom-in []
  (let [window (get-game-window)]
    (.zoomInc window -1)))

(defn zoom-out []
  (let [window (get-game-window)]
    (.zoomInc window 1)))

(defn zoom-by [delta]
  (let [window (get-game-window)]
    (.zoomInc window delta)))
```

**Source Reference:**
- `GameWindow.zoomInc()` - Line 210 in `GameWindow.java`
- `GameWindow.zoomInc(int delta)` - Lines 214-216 in `GameWindow.java`
- Zoom can also be controlled via keyboard: `KEYS.MAIN().ZOOM_IN/ZOOM_OUT` - Lines 286-291 in `GameWindow.java`

## Game Speed Control

The game speed (time flow) is controlled through the `GameSpeed` class, which is accessed via the `GAME` singleton.

### Getting the Game Speed Instance

The game speed instance is accessed through the `GAME` class:

```clojure
(ns my-mod
  (:import [game GAME]))

(defn get-game-speed []
  (GAME/SPEED))
```

**Source Reference:**
- `game.GAME.SPEED` - Static final field that provides access to GameSpeed instance
- `game.GameSpeed` - The game speed class that handles all time flow operations
- See: `sos-src/game/GameSpeed.java`

### Setting Game Speed

The `GameSpeed` class provides methods to control time flow:

#### 1. Set Speed Directly

```clojure
(defn set-time-speed [speed]
  (let [game-speed (GAME/SPEED)]
    (.speedSet game-speed speed)))
```

This sets the game speed to a specific value:
- `0` = Paused
- `1` = Normal speed (1x)
- `5` = Fast speed (5x)
- `25` = Very fast speed (25x)
- Any other value = Custom speed

**Source Reference:**
- `GameSpeed.speedSet(double speed)` - Lines 103-108 in `GameSpeed.java`
- Speed constants: `speed0 = 0`, `speed1 = 1`, `speed2 = 5`, `speed3 = 25`, `speed4 = 250` - Lines 19-24 in `GameSpeed.java`

#### 2. Get Current Speed

```clojure
(defn get-time-speed []
  (let [game-speed (GAME/SPEED)]
    (.speed game-speed)))
```

Returns the actual current speed (may differ from target speed due to gradual acceleration).

**Source Reference:**
- `GameSpeed.speed()` - Lines 95-97 in `GameSpeed.java`

#### 3. Get Target Speed

```clojure
(defn get-time-speed-target []
  (let [game-speed (GAME/SPEED)]
    (.speedTarget game-speed)))
```

Returns the target speed setting (what speed is set to, before any adjustments).

**Source Reference:**
- `GameSpeed.speedTarget()` - Lines 69-71 in `GameSpeed.java`

### Pause and Resume

#### Pause the Game

```clojure
(defn pause-time []
  (let [game-speed (GAME/SPEED)]
    (.speedSet game-speed 0)))
```

Sets speed to 0 (paused).

#### Resume at Normal Speed

```clojure
(defn resume-time []
  (let [game-speed (GAME/SPEED)]
    (.speedSet game-speed 1)))
```

Resumes at normal speed (1x).

#### Toggle Pause

```clojure
(defn toggle-pause []
  (let [game-speed (GAME/SPEED)]
    (.togglePause game-speed)))
```

Toggles between paused and running. If paused, resumes to the previous speed. If running, pauses and remembers the current speed.

**Source Reference:**
- `GameSpeed.togglePause()` - Lines 81-93 in `GameSpeed.java`

### Speed Button Functions

The game has speed buttons for common speeds. Here are functions that match those buttons:

```clojure
;; Pause (0x speed)
(defn time-speed-0x []
  (set-time-speed 0))

;; Normal speed (1x)
(defn time-speed-1x []
  (set-time-speed 1))

;; Fast speed (5x)
(defn time-speed-5x []
  (set-time-speed 5))

;; Very fast speed (25x)
(defn time-speed-25x []
  (set-time-speed 25))
```

**Source Reference:**
- Speed button handling: Lines 128-145 in `GameSpeed.java`
- Keyboard shortcuts: `KEYS.MAIN().SPEED0`, `SPEED1`, `SPEED2`, `SPEED3` - Lines 128-145 in `GameSpeed.java`

### Speed Limitations

The game automatically limits speed in certain situations:
- When enemies are present, speed is capped at 25x (line 38-39 in `GameSpeed.java`)
- Speed gradually accelerates/decelerates to the target speed (lines 41-55 in `GameSpeed.java`)

### Example Usage

```clojure
;; Pause/resume
(pause-time)
(resume-time)
(toggle-pause)

;; Set specific speeds
(time-speed-0x)    ; Pause
(time-speed-1x)    ; Normal
(time-speed-5x)    ; Fast
(time-speed-25x)   ; Very fast

;; Custom speed
(set-time-speed 10) ; 10x speed

;; Check current speed
(get-time-speed)    ; Get actual current speed
(get-time-speed-target) ; Get target speed setting
```

## Building Creation

Creating buildings programmatically requires understanding the construction system and room blueprints.

### Understanding the Building System

Buildings in Songs of Syx are created through the construction system:

1. **Room Blueprints** - Define what can be built (e.g., `ROOM_STOCKPILE` for warehouses)
2. **Constructors** - Define how rooms are placed and what they look like
3. **ConstructionInit** - Contains the parameters for a new construction (upgrade level, materials, etc.)
4. **TmpArea** - Temporary area used to define the building footprint

**Source References:**
- `settlement.room.main.RoomBlueprintIns` - Base class for room blueprints
- `settlement.room.main.construction.ConstructionInit` - Construction parameters
- `settlement.room.main.TmpArea` - Temporary area for defining building footprint
- `settlement.room.main.construction.Construction` - The construction system

### Getting Building Materials

Building materials need to be converted from `Structure` to `TBuilding` objects. `ConstructionInit` requires a `TBuilding`, not a `Structure`. Here's how to get a `TBuilding` for a material:

```clojure
(ns my-mod
  (:import [init.structure STRUCTURES]
           [init.resources RESOURCES]
           [settlement.main SETT]))

(defn get-building-material [material-name]
  (let [material-upper (.toUpperCase material-name)
        resource (case material-upper
                   "WOOD" (RESOURCES/WOOD)
                   "STONE" (RESOURCES/STONE)
                   (throw (Exception. (str "Unknown material: " material-name))))
        all-structures (STRUCTURES/all)
        structure (first (filter #(= resource (.-resource %)) all-structures))]
    (if structure
      ;; Convert Structure to TBuilding using BUILDINGS.get(Structure)
      (let [buildings (.-BUILDINGS (SETT/TERRAIN))]
        (.get buildings structure))
      (throw (Exception. (str "Could not find structure for material: " material-name))))))
```

**Key Points:**
- Structures are found by matching their `resource` field to the desired resource type
- `STRUCTURES.all()` returns all available structures
- Each `Structure` has a `resource` field of type `RESOURCE`
- **Important:** `ConstructionInit` requires a `TBuilding`, not a `Structure`
- Convert using `BUILDINGS.get(Structure)` - this is what `Structure.terrain()` does internally

**Source References:**
- `init.structure.STRUCTURES` - Singleton that manages all structures
- `init.structure.Structure` - Individual structure definition (see `sos-src/init/structure/Structure.java`)
- `Structure.resource` - The resource type this structure uses (Line 23 in `Structure.java`)
- `Structure.terrain()` - Returns the `TBuilding` for this structure (Lines 63-65 in `Structure.java`)
- `settlement.tilemap.terrain.TBuilding.TBuildings.get(Structure)` - Converts Structure to TBuilding (Lines 88-90 in `TBuilding.java`)
- `init.resources.RESOURCES` - Singleton for accessing resource types

### Creating a Warehouse/Stockpile

Here's a complete example of creating a warehouse:

```clojure
(ns my-mod
  (:require [repl.utils :as utils])
  (:import [settlement.main SETT]
           [settlement.room.main.construction ConstructionInit]
           [init.structure STRUCTURES]
           [init.resources RESOURCES]))

;; Get the stockpile room blueprint
(defn get-stockpile-room []
  (let [rooms (SETT/ROOMS)]
    (.-STOCKPILE rooms)))

;; Get the stockpile constructor
(defn get-stockpile-constructor []
  (let [stockpile-room (get-stockpile-room)]
    (.constructor stockpile-room)))

;; Create a warehouse at specified location
(defn create-warehouse [center-x center-y width height & {:keys [material-name upgrade] 
                                                           :or {material-name "WOOD" upgrade 0}}]
  (let [rooms (SETT/ROOMS)
        stockpile-constructor (get-stockpile-constructor)
        tbuilding (get-building-material material-name)  ; Returns TBuilding, not Structure
        degrade 0  ; No degradation
        state nil  ; No special state
        
        ;; Create ConstructionInit (note: third parameter is TBuilding, not Structure)
        construction-init (ConstructionInit. upgrade stockpile-constructor tbuilding degrade state)
        
        ;; Get temporary area
        tmp (.tmpArea rooms "warehouse")]
    
    ;; Set the building area
    (let [start-x (- center-x (quot width 2))
          start-y (- center-y (quot height 2))]
      (doseq [y (range height)
              x (range width)]
        (.set tmp (+ start-x x) (+ start-y y))))
    
    ;; Create the construction site
    (.createClean (.construction rooms) tmp construction-init)
    
    ;; Clear temporary area
    (.clear tmp)
    
    {:success true
     :center-x center-x
     :center-y center-y
     :width width
     :height height}))

;; Create using update-once (ensures it happens in a single frame)
(defn create-warehouse-once [center-x center-y width height & {:keys [material-name upgrade] 
                                                                 :or {material-name "WOOD" upgrade 0}}]
  (utils/update-once 
   (fn [_ds]
     (create-warehouse center-x center-y width height 
                      :material-name material-name 
                      :upgrade upgrade))))
```

**Important Notes:**
1. **Use `create-warehouse-once`** - This ensures the construction happens in a single frame, which is required for the game's update loop
2. **Coordinates are tile coordinates** - Not pixel coordinates
3. **Warehouses must be built indoors** - They require walls/roof
4. **Construction happens asynchronously** - Workers will complete the construction

**Source References:**
- `settlement.main.SETT.ROOMS()` - Access to all room types (Line 141 in `ROOMS.java`)
- `settlement.room.infra.stockpile.ROOM_STOCKPILE` - The stockpile/warehouse room blueprint
- `ROOM_STOCKPILE.constructor()` - Returns the `Furnisher` used for placement (Line 60 in `ROOM_STOCKPILE.java`)
- `settlement.room.main.construction.Construction.createClean()` - Creates a construction site
- `settlement.room.main.TmpArea` - Temporary area for defining building footprint

### Understanding ConstructionInit

The `ConstructionInit` class contains all the parameters needed to create a building:

```java
public ConstructionInit(int upgrade, Furnisher b, TBuilding structure, int degrade, RoomState state)
```

**Parameters:**
- `upgrade` - Upgrade level (0 = base level)
- `b` (furnisher) - The room constructor (defines placement rules and appearance)
- `structure` - **The building material as a `TBuilding`** (not `Structure`!) - This is the terrain building type (wood, stone, etc.)
- `degrade` - Degradation level (usually 0)
- `state` - Additional state (usually `null`)

**Important:** The third parameter is `TBuilding`, not `Structure`. You must convert from `Structure` to `TBuilding` using `BUILDINGS.get(Structure)`.

**Source Reference:**
- `settlement.room.main.construction.ConstructionInit` - See `sos-src/settlement/room/main/construction/ConstructionInit.java`
- Constructor signature: Line 26 in `ConstructionInit.java`
- Field definition: Line 20 in `ConstructionInit.java` - `public final TBuilding structure;`

### Example: Creating Different Types of Buildings

The same pattern can be used for other room types:

```clojure
;; Get any room type
(defn get-room [room-type]
  (let [rooms (SETT/ROOMS)]
    (case room-type
      :home (.-HOME rooms)
      :stockpile (.-STOCKPILE rooms)
      :throne (.-THRONE rooms)
      ;; Add more room types as needed
      )))

;; Generic room creation function
(defn create-room [room-type center-x center-y width height & {:keys [material-name upgrade] 
                                                                 :or {material-name "WOOD" upgrade 0}}]
  (let [rooms (SETT/ROOMS)
        room-blueprint (get-room room-type)
        constructor (.constructor room-blueprint)
        structure (get-building-material material-name)
        construction-init (ConstructionInit. upgrade constructor structure 0 nil)
        tmp (.tmpArea rooms (name room-type))]
    
    ;; Set building area
    (let [start-x (- center-x (quot width 2))
          start-y (- center-y (quot height 2))]
      (doseq [y (range height)
              x (range width)]
        (.set tmp (+ start-x x) (+ start-y y))))
    
    ;; Create construction
    (.createClean (.construction rooms) tmp construction-init)
    (.clear tmp)))
```

## Common Patterns and Best Practices

### 1. Always Use update-once for Construction

Construction must happen in a single frame to avoid rendering/update issues:

```clojure
(utils/update-once 
 (fn [_ds]
   ;; Your construction code here
   ))
```

**Source Reference:**
- `your.mod.InstanceScript.addConsumer()` - Adds a function to the game's update loop
- See `src/main/java/your/mod/InstanceScript.java` for the update mechanism

### 2. Understanding Coordinate Systems

- **Pixel coordinates** - Used for camera movement (`centerAt`)
- **Tile coordinates** - Used for building placement (`centerAtTile`)
- Tile size is defined in `init.constant.C.TILE_SIZE`

### 3. Room Requirements

Different rooms have different requirements:
- Some must be indoors (like stockpiles) - Check `Furnisher.mustBeIndoors()`
- Some use area-based placement - Check `Furnisher.usesArea()`
- Some are fixed-size - Check the room's constructor

**Source Reference:**
- `settlement.room.infra.stockpile.Constructor.mustBeIndoors()` - Returns `true` (Line 191 in `Constructor.java`)
- `settlement.room.infra.stockpile.Constructor.usesArea()` - Returns `true` (Line 186 in `Constructor.java`)

## Furniture Placement

Some buildings require furniture to be placed inside them. For example, warehouses (stockpiles) need crates (货箱) to function properly. The furniture system uses `FurnisherItem` and `FurnisherItemTile` objects.

### Understanding Furniture Components

1. **FurnisherItemTile** - Represents a single tile that furniture occupies (e.g., a crate tile, a bed tile)
2. **FurnisherItem** - Represents a complete furniture piece that can span multiple tiles (e.g., a 2x1 crate, a 3x2 bed)
3. **FurnisherData (fData)** - The storage system that tracks what furniture is placed where

### How Furniture Works

Furniture items are defined in the room's `Furnisher` (constructor). Each `FurnisherItem` contains:
- A 2D array of `FurnisherItemTile[][]` that defines which tiles it occupies
- Dimensions (width, height)
- Multipliers for costs and stats

**Example from Stockpile Constructor:**
```java
// Create a crate tile
FurnisherItemTile cr = new FurnisherItemTile(...);

// Create a misc tile  
FurnisherItemTile mm = new FurnisherItemTile(...);

// Create a 2x1 crate furniture item (2 crates + 1 misc)
new FurnisherItem(new FurnisherItemTile[][] {
    {cr, mm},
}, 2, 1);

// Create a 3x2 crate furniture item (3 crates + 2 misc)
new FurnisherItem(new FurnisherItemTile[][] {
    {cr, cr, mm},
}, 3, 2);
```

**Source Reference:**
- `settlement.room.infra.stockpile.Constructor` - Lines 98-171 in `Constructor.java`
- `settlement.room.main.furnisher.FurnisherItem` - Furniture item class
- `settlement.room.main.furnisher.FurnisherItemTile` - Furniture tile class

### Placing Furniture

To place furniture in a building, you need to:

1. **Get the furniture item from the constructor's groups:**
```clojure
(let [furnisher-groups (.get (.pgroups constructor) 0)  ; Get first placement group
      furnisher-item (.item furnisher-groups 0 0)]      ; Get first item from group
  ;; Use furnisher-item
  )
```

2. **Place the furniture using fData.itemSet:**
```clojure
(let [rooms (SETT/ROOMS)
      fdata (.fData rooms)
      tx 100  ; Tile x coordinate
      ty 100  ; Tile y coordinate
      room (.room tmp-area)]  ; Room instance from TmpArea
  (.itemSet fdata tx ty furnisher-item room))
```

**Important:** Furniture must be placed **before** creating the construction site, so the room instance knows about the furniture when it's constructed.

### Complete Example: Creating a Home with Furniture

```clojure
(defn create-home-with-furniture [center-x center-y & {:keys [material-name upgrade] 
                                                        :or {material-name "WOOD" upgrade 0}}]
  (let [rooms (SETT/ROOMS)
        home-constructor (.constructor (.-HOME rooms))
        tbuilding (get-building-material material-name)
        construction-init (ConstructionInit. upgrade home-constructor tbuilding 0 nil)
        tmp (.tmpArea rooms "home")]
    
    ;; Set 3x3 building area
    (let [start-x (- center-x 1)
          start-y (- center-y 1)]
      (doseq [y (range 3)
              x (range 3)]
        (.set tmp (+ start-x x) (+ start-y y))))
    
    ;; Place furniture (bed, etc.)
    (let [fdata (.fData rooms)
          furnisher-groups (.pgroups home-constructor)
          furnisher-item (.item (.get furnisher-groups 0) 0 0)  ; Get first furniture item
          start-x (- center-x 1)
          start-y (- center-y 1)]
      ;; Place furniture at the top-left corner of the room
      (.itemSet fdata start-x start-y furnisher-item (.room tmp)))
    
    ;; Create construction site
    (.createClean (.construction rooms) tmp construction-init)
    
    ;; Clear temporary area
    (.clear tmp)))
```

### Key Methods

- **`fData.itemSet(x, y, furnisherItem, room)`** - Places a furniture item at coordinates (x, y) in the room
- **`fData.item.get(x, y)`** - Gets the furniture item at coordinates (x, y)
- **`fData.itemClear(x, y, room)`** - Removes furniture item at coordinates (x, y)
- **`fData.tile.get(x, y)`** - Gets the furniture tile at coordinates (x, y)
- **`constructor.pgroups()`** - Returns list of placement groups (each group contains furniture items)
- **`furnisher-group.item(groupIndex, itemIndex)`** - Gets a furniture item from a group

**Source References:**
- `settlement.room.main.furnisher.Furnisher.pgroups()` - Line 230 in `Furnisher.java`
- `settlement.room.main.furnisher.FurnisherItemGroup.item()` - Method to get items from groups
- `settlement.room.main.placement.UtilHistory` - Shows usage of `itemSet` and `itemClear` (Lines 146-148)
- `settlement.room.main.copy.Copier` - Shows furniture copying logic (Lines 370-386)

### Notes

1. **Furniture placement order matters** - Place furniture **before** calling `createClean()`
2. **Some rooms auto-place furniture** - Stockpiles automatically place crates based on area
3. **Furniture items can span multiple tiles** - A single `FurnisherItem` can occupy a 2x1, 3x2, etc. area
4. **Room instance required** - `itemSet` needs the room instance from `TmpArea.room()`

## Troubleshooting

### Camera Not Moving

- Make sure you're calling the functions from within the game's update loop or from a REPL connected to a running game
- Check that `VIEW/s` returns a valid view (game must be loaded)

### Building Not Appearing

- Ensure coordinates are within the settlement bounds
- Check that the area is clear and suitable for the building type
- Verify that workers are available to complete construction
- Make sure you're using `create-warehouse-once` or wrapping in `update-once`

### Material Not Found

- Verify the material name is correct ("WOOD", "STONE", etc.)
- Check that structures exist for that resource type
- You can list all structures: `(STRUCTURES/all)`

## Further Reading

- `doc/howto/game_code.md` - General game code structure
- `sos-src/view/subview/GameWindow.java` - Complete camera implementation
- `sos-src/settlement/room/main/construction/` - Construction system
- `sos-src/settlement/room/infra/stockpile/` - Stockpile/warehouse implementation

