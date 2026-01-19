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

