# Building Rooms with Walls and Doors

This document explains the pattern for programmatically creating rooms/structures with proper wall and door placement in Songs of Syx.

## Key Insight: Single Door Position

When building rooms, the furnisher item may mark multiple tiles as `noWalls=true` (entrance tiles). However, you should **not** create openings for all these tiles. Instead:

1. Use entrance tiles only as a **preference hint** for where the door should be
2. Calculate a **single door position** using pure functions
3. Build walls on all edge tiles **except** the single door tile

## Pure Functions Pattern

### 1. Find Edge Tiles

Edge tiles are tiles **outside** the room that are **adjacent** to room tiles. These are where walls will be built.

```clojure
(defn- find-room-edge-tiles [room-tiles]
  (let [directions [[-1 -1] [0 -1] [1 -1]
                    [-1 0]         [1 0]
                    [-1 1]  [0 1]  [1 1]]
        edge-tiles (atom #{})]
    (doseq [[tx ty] room-tiles
            [dx dy] directions]
      (let [adj-x (+ tx dx)
            adj-y (+ ty dy)]
        (when-not (contains? room-tiles [adj-x adj-y])
          (swap! edge-tiles conj [adj-x adj-y]))))
    @edge-tiles))
```

For rectangular rooms, you can also use `find-edge-tiles` which takes start coordinates and dimensions.

### 2. Find Single Door Position

Find ONE door position from the edge tiles. Prefer positions adjacent to entrance tiles (tiles marked `noWalls=true` in the furnisher item).

```clojure
(defn- find-home-door-position [room-tiles entrance-tiles]
  (let [edge-tiles (find-room-edge-tiles room-tiles)
        ortho-directions [[0 -1] [0 1] [1 0] [-1 0]]
        ;; Filter edge tiles to those orthogonally adjacent to entrance tiles
        door-candidates (filter (fn [[ex ey]]
                                  (some (fn [[dx dy]]
                                          (contains? entrance-tiles [(+ ex dx) (+ ey dy)]))
                                        ortho-directions))
                                edge-tiles)]
    (first door-candidates)))
```

For warehouses with furniture, use `find-door-position` which also considers occupied tiles (furniture) to ensure the door is adjacent to a free inner tile.

### 3. Build Walls with Single Door

```clojure
(defn- build-walls-around-room [room-tiles door-tile tbuilding]
  (let [edge-tiles (find-room-edge-tiles room-tiles)]
    ;; Build door opening at the single door location
    (when door-tile
      (let [[door-x door-y] door-tile]
        (when (UtilWallPlacability/openingCanBe door-x door-y)
          (UtilWallPlacability/openingBuild door-x door-y tbuilding))))
    
    ;; Build walls on all edge tiles except the door
    (doseq [[x y] edge-tiles]
      (when (and (UtilWallPlacability/wallCanBe x y)
                 (not= [x y] door-tile))
        (UtilWallPlacability/wallBuild x y tbuilding)))))
```

## Room Creation Order

When creating a room, follow this order:

1. **Collect room tiles** - Iterate over furnisher item, collect tiles where content exists
2. **Collect entrance tiles** - Track tiles where `noWalls=true` (for door preference)
3. **Set tmp area** - Mark tiles in the temporary area
4. **Place furniture** - Use `(.itemSet fData ...)` BEFORE creating construction
5. **Calculate door position** - Use pure function with room tiles and entrance tiles
6. **Build walls** - Build walls on edge tiles, opening at door position
7. **Create construction** - Call `(.createClean construction tmp construction-init)`

## Example: Home Creation

```clojure
;; Collect tiles
(doseq [y (range height) x (range width)]
  (when-let [tile (.get furnisher-item x y)]
    (let [world-x (+ start-x x)
          world-y (+ start-y y)]
      (.set tmp world-x world-y)
      (swap! room-tiles conj [world-x world-y])
      (when (.-noWalls tile)
        (swap! entrance-tiles conj [world-x world-y])))))

;; Place furniture BEFORE construction
(.itemSet (.fData rooms) start-x start-y furnisher-item (.room tmp))

;; Find SINGLE door position
(let [door-tile (find-home-door-position @room-tiles @entrance-tiles)]
  (build-walls-around-room @room-tiles door-tile tbuilding))

;; Create construction
(.createClean (.construction rooms) tmp construction-init)
```

## Common Mistakes

### ❌ Wrong: Multiple doors from entrance tiles

```clojure
;; BAD: Creates 3 doors for a 3x3 house
(doseq [[ox oy] entrance-tiles]
  (UtilWallPlacability/openingBuild ox oy tbuilding))
```

### ✅ Correct: Single door position

```clojure
;; GOOD: Creates exactly 1 door
(let [door-tile (find-home-door-position room-tiles entrance-tiles)]
  (when door-tile
    (UtilWallPlacability/openingBuild (first door-tile) (second door-tile) tbuilding)))
```

## Key Classes

- `UtilWallPlacability` - Check and build walls/openings
  - `wallCanBe(x, y)` / `wallShouldBuild` - Check if wall can be placed
  - `wallBuild(x, y, tbuilding)` - Build a wall
  - `openingCanBe(x, y)` / `openingShouldBuild` - Check if opening can be placed
  - `openingBuild(x, y, tbuilding)` - Build an opening (roof only, no wall)
- `ConstructionInit` - Initialize construction with upgrade level, constructor, material
- `FurnisherItem` - Room layout template with tiles
- `FurnisherTile` - Individual tile info, including `noWalls` flag

## Applying to Other Room Types

When implementing new room types:

1. Get the room's constructor: `(.constructor (.ROOM_NAME (SETT/ROOMS)))`
2. Get furnisher groups: `(.pgroups constructor)`
3. Get furnisher item: `(.item group variation rotation)`
4. Follow the same pattern: collect tiles → find door → build walls → create construction

## Task: Building Dirt Roads Near Houses

After building houses, you may want to connect them with dirt roads for better pathfinding and aesthetics.

### Road Building System

Roads in Songs of Syx are built using the `JobBuildRoad` system:

- **Road Types**: Available through `SETT.FLOOR().roads` - a list of all road floor types
- **Default Road**: `SETT.FLOOR().defaultRoad` - typically the basic dirt road
- **Road Jobs**: `SETT.JOBS().roads` - contains road building jobs
- **Placer**: Each road job has a `.placer()` method to place roads

### Building Roads Programmatically

```clojure
(require '[repl.utils :as utils])

;; Get the default dirt road job
(defn get-default-road-job []
  (let [roads (SETT/JOBS)
        road-jobs (.roads roads)]
    ;; The first road job is typically the default dirt road
    (.get (.all road-jobs) 0)))

;; Build a single road tile
(defn build-road-tile-once [tx ty]
  (utils/update-once
   (fn [_ds]
     (let [road-job (get-default-road-job)
           placer (.placer road-job)]
       ;; Check if road can be placed, then place it
       (when (nil? (.isPlacable placer tx ty nil nil))
         (.place placer tx ty nil nil))))))

;; Build multiple road tiles in a line or pattern
(defn build-road-tiles-once [tiles]
  (utils/update-once
   (fn [_ds]
     (let [road-job (get-default-road-job)
           placer (.placer road-job)]
       (doseq [[tx ty] tiles]
         (when (nil? (.isPlacable placer tx ty nil nil))
           (.place placer tx ty nil nil)))))))
```

### Example: Build 10 Tiles of Dirt Road Near Houses

```clojure
;; After building houses, build roads connecting them
(defn build-roads-near-houses-once [house-positions]
  (utils/update-once
   (fn [_ds]
     (let [road-job (get-default-road-job)
           placer (.placer road-job)
           ;; Calculate road tiles - connect houses with a simple path
           road-tiles (calculate-road-path house-positions)]
       ;; Build up to 10 road tiles
       (doseq [[tx ty] (take 10 road-tiles)]
         (when (nil? (.isPlacable placer tx ty nil nil))
           (.place placer tx ty nil nil))))))

;; Helper: Calculate a simple road path between house positions
(defn calculate-road-path [house-positions]
  (let [road-tiles (atom [])]
    ;; Simple approach: create a path connecting house doors
    ;; For each house, add a few tiles extending from the door
    (doseq [[house-x house-y door-tile] house-positions]
      (when door-tile
        (let [[door-x door-y] door-tile
              ;; Add 3-4 tiles extending outward from door
              path-tiles (for [i (range 1 4)]
                          [door-x (+ door-y i)])]
          (swap! road-tiles into path-tiles))))
    @road-tiles))

;; Usage: After creating houses
(let [houses [{:center-x 100 :center-y 100 :door-tile [100 99]}
              {:center-x 110 :center-y 100 :door-tile [110 99]}
              {:center-x 120 :center-y 100 :door-tile [120 99]}]]
  ;; Build roads connecting the houses
  (build-roads-near-houses-once 
   (map (fn [h] [(:center-x h) (:center-y h) (:door-tile h)]) houses)))
```

### Alternative: Build Roads in a Grid Pattern

```clojure
;; Build roads in a simple grid pattern near houses
(defn build-road-grid-once [start-x start-y width height]
  (utils/update-once
   (fn [_ds]
     (let [road-job (get-default-road-job)
           placer (.placer road-job)
           ;; Build horizontal road
           horizontal-tiles (for [x (range start-x (+ start-x width))]
                              [x start-y])
           ;; Build vertical road
           vertical-tiles (for [y (range start-y (+ start-y height))]
                          [start-x y])
           all-tiles (concat horizontal-tiles vertical-tiles)]
       ;; Build up to 10 tiles
       (doseq [[tx ty] (take 10 all-tiles)]
         (when (nil? (.isPlacable placer tx ty nil nil))
           (.place placer tx ty nil nil))))))
```

### Key Classes for Roads

- `SETT.JOBS().roads` - Road building jobs system
  - `.all` - List of all road job types
  - `.placer()` - Get placer for a specific road job
- `SETT.FLOOR().roads` - List of road floor types
- `SETT.FLOOR().defaultRoad` - Default dirt road floor type
- `JobBuildRoad` - Road building job class
- `Placable` - Interface for checking if road can be placed
  - `.isPlacable(x, y, ...)` - Returns `null` if can be placed, error message otherwise
  - `.place(x, y, ...)` - Place the road at coordinates

### Notes

- Roads are placed as **floor tiles**, not structures
- Use `update-once` to ensure road building happens in a single game frame
- Check `isPlacable` before placing to avoid errors
- The default road (index 0) is typically the dirt road
- Roads improve pathfinding and movement speed for subjects

