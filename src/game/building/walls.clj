(ns game.building.walls
  "Wall and door placement operations for complex buildings in Songs of Syx.

   This namespace provides functions for:
   - Building door openings
   - Building walls around rectangular areas
   - Calculating edge tiles for wall placement
   - Finding optimal door positions

   Used by buildings that require walls: warehouses, homes, workshops, etc."

  (:import
   [settlement.room.main.util RoomAreaWrapper]
   [settlement.tilemap.terrain.Terrain TerrainTile]
   [settlement.main SETT]
   [settlement.room.main ROOMA]
   [settlement.tilemap.terrain.TBuilding UtilWallPlacability]))

;; ============================================================================
;; Door Placement
;; ============================================================================

(defn build-door-opening
  "Build a door opening (no wall) at specific coordinates.

   Creates an opening where a wall would normally be placed,
   allowing access to the building.

   Parameters:
   - x: Tile X coordinate
   - y: Tile Y coordinate
   - tbuilding: TBuilding material (WOOD, STONE, etc.)

   Note: Only builds if openingCanBe() returns true for the location.
   Silently fails if the location can't have an opening."
  [x y tbuilding]
  (when (UtilWallPlacability/openingCanBe x y)
    (UtilWallPlacability/openingBuild x y tbuilding)))

(comment
  (def tbuilding (game.common/get-building-material "WOOD"))
  (build-door-opening 100 100 tbuilding)
  :rcf)

(defn build-door-opening-safe
  "Build a door opening with error handling.

   Parameters:
   - x: Tile X coordinate
   - y: Tile Y coordinate
   - tbuilding: TBuilding material

   Throws: Exception if opening cannot be built"
  [x y tbuilding]
  (when-not (UtilWallPlacability/openingCanBe x y)
    (let [tile (SETT/TERRAIN().get x y)]
      (throw (Exception. (str "Cannot build door opening at (" x "," y "): "
                                     (when tile
                                       (str "Tile occupied by " tile)
                                       "Tile is null")))))
  (build-door-opening x y tbuilding)))

(comment
  (def tbuilding (game.common/get-building-material "WOOD"))
  (build-door-opening-safe 100 100 tbuilding)
  :rcf)

;; ============================================================================
;; Wall Placement
;; ============================================================================

(defn build-wall
  "Build a wall at specific coordinates.

   Parameters:
   - x: Tile X coordinate
   - y: Tile Y coordinate
   - tbuilding: TBuilding material (WOOD, STONE, etc.)

   Note: Only builds if wallCanBe() returns true for the location.
   Silently fails if the location can't have a wall."
  [x y tbuilding]
  (when (UtilWallPlacability/wallCanBe x y)
    (UtilWallPlacability/wallBuild x y tbuilding)))

(comment
  (def tbuilding (game.common/get-building-material "STONE"))
  (build-wall 100 99 tbuilding)
  :rcf)

(defn build-wall-safe
  "Build a wall with error handling.

   Parameters:
   - x: Tile X coordinate
   - y: Tile Y coordinate
   - tbuilding: TBuilding material

   Throws: Exception if wall cannot be built"
  [x y tbuilding]
  (when-not (UtilWallPlacability/wallCanBe x y)
    (let [tile (SETT/TERRAIN().get x y)]
      (throw (Exception. (str "Cannot build wall at (" x "," y "): "
                                     (when tile
                                       (str "Tile occupied by " tile)
                                       "Tile is null")))))
  (build-wall x y tbuilding)))

(comment
  (def tbuilding (game.common/get-building-material "STONE"))
  (build-wall-safe 100 99 tbuilding)
  :rcf)

;; ============================================================================
;; Edge Tile Calculation
;; ============================================================================

(defn find-edge-tiles-rect
  "Find all edge tiles around a rectangular area.

   Returns coordinates for tiles that are:
   - Outside the rectangle
   - Adjacent to the rectangle (8 directions: N, S, E, W, NE, NW, SE, SW)

   Parameters:
   - start-x: Top-left X coordinate
   - start-y: Top-left Y coordinate
   - width: Width of rectangle
   - height: Height of rectangle

   Returns: Vector of [x y] coordinates for edge tiles

   Note: This is used for wall placement around rooms."
  [start-x start-y width height]
  (let [edge-tiles (atom [])]
    ;; Iterate over area including 1-tile border
    (doseq [y (range (- start-y 1) (+ start-y height 1))
            x (range (- start-x 1) (+ start-x width 1))]
      (let [is-inside (and (>= x start-x) (< x (+ start-x width))
                       (>= y start-y) (< y (+ start-y height)))
            ;; All 8 directions for adjacency check
            directions [[0 -1] [0 1] [1 0] [-1 0]
                     [1 -1] [-1 -1] [1 1] [-1 1]]
            is-adjacent (some (fn [[dx dy]]
                                   (let [adj-x (+ x dx)
                                         adj-y (+ y dy)]
                                     (and (>= adj-x start-x) (< adj-x (+ start-x width))
                                          (>= adj-y start-y) (< adj-y (+ start-y height)))))
                                 directions)
            ;; Edge if outside and adjacent to inside
            is-edge (and (not is-inside) is-adjacent)]
        (when is-edge
          (swap! edge-tiles conj [x y]))))
    @edge-tiles))

(comment
  (find-edge-tiles-rect 100 100 5 5)
  (find-edge-tiles-rect 100 100 3 3)
  :rcf)

;; ============================================================================
;; Complete Wall Building
;; ============================================================================

(defn build-walls-around-rect
  "Build walls around a rectangular area with a single door opening.

   Steps:
   1. Build door opening at specified location
   2. Build walls on all other edge tiles

   Parameters:
   - start-x: Top-left X coordinate
   - start-y: Top-left Y coordinate
   - width: Width of rectangle
   - height: Height of rectangle
   - door-x: X coordinate for door
   - door-y: Y coordinate for door
   - tbuilding: TBuilding material"
  [start-x start-y width height door-x door-y tbuilding]
  (let [edge-tiles (find-edge-tiles-rect start-x start-y width height)]
    ;; Build door first
    (build-door-opening door-x door-y tbuilding)
    ;; Build walls on all other edge tiles
    (doseq [[x y] edge-tiles]
      (when-not (= [x y] [door-x door-y])
        (build-wall x y tbuilding)))))

(comment
  (def tbuilding (game.common/get-building-material "WOOD"))
  (build-walls-around-rect 100 100 5 5 103 100 tbuilding)
  :rcf)

(defn build-walls-from-room-tiles
  "Build walls around a room defined by its tile coordinates.

   Uses the room's tile set to calculate edge and build walls.

   Parameters:
   - room-tiles: Set of [x y] coordinates belonging to the room
   - door-x: X coordinate for door
   - door-y: Y coordinate for door
   - tbuilding: TBuilding material

   Returns: Number of walls built"
  [room-tiles door-x door-y tbuilding]
  (let [;; Find bounding box of room
        xs (map first room-tiles)
        ys (map second room-tiles)
        min-x (apply min xs)
        max-x (apply max xs)
        min-y (apply min ys)
        max-y (apply max ys)
        width (+ 1 (- max-x min-x))
        height (+ 1 (- max-y min-y))]
    
    ;; Build walls around rectangle
    (build-walls-around-rect min-x min-y width height door-x door-y tbuilding)
    ;; Approximate wall count (edge tiles - 1 door)
    (let [edge-tiles (find-edge-tiles-rect min-x min-y width height)]
      (count edge-tiles))))

(comment
  (def room-tiles #{[100 100] [101 100] [100 101] [101 101]})
  (build-walls-from-room-tiles room-tiles 103 100 (game.common/get-building-material "WOOD"))
  :rcf)

;; ============================================================================
;; Door Finding
;; ============================================================================

(defn find-edge-tiles-around-set
  "Find edge tiles around an irregular shape defined by a tile set.

   Unlike find-edge-tiles-rect which works on rectangles, this works
   on arbitrary shapes.

   Parameters:
   - room-tiles: Set of [x y] coordinates

   Returns: Set of [x y] coordinates for edge tiles"
  [room-tiles]
  (let [xs (map first room-tiles)
        ys (map second room-tiles)
        min-x (apply min xs)
        max-x (apply max xs)
        min-y (apply min ys)
        max-y (apply max ys)

        ;; Check all 8 directions
        directions [[0 -1] [0 1] [1 0] [-1 0]
                    [1 -1] [-1 -1] [1 1] [-1 1]]

        ;; Get all tiles in bounding box + 1 tile border
        all-candidates (atom #{})]

    (doseq [y (range (- min-y 1) (+ max-y 2))
            x (range (- min-x 1) (+ max-x 2))]
      (let [is-in-room (contains? room-tiles [x y])

            ;; Check all 8 directions
            has-room-adjacent (some (fn [[dx dy]]
                                      (contains? room-tiles
                                                 [(+ x dx) (+ y dy)]))
                                    directions)
            is-edge (and (not is-in-room) has-room-adjacent)]
        ;; Edge if: outside room AND adjacent to room tile 
        (when is-edge
          (swap! all-candidates conj [x y]))))
    @all-candidates))

(defn find-door-position
  "Find optimal door position for a room based on entrance tiles.

   Prefer door positions adjacent to entrance tiles (tiles marked with noWalls=true).

   Parameters:
   - room-tiles: Set of [x y] coordinates belonging to the room
   - entrance-tiles: Set of [x y] coordinates that are entrances
   - preferred-side: Optional keyword (:top, :bottom, :left, :right)

   Returns: [x y] coordinates for door, or nil if no entrance tiles

   Logic:
   1. Get edge tiles
   2. Find edge tiles adjacent to entrance tiles
   3. Sort by preference (preferred side first, then distance)"
  [room-tiles entrance-tiles & {:keys [preferred-side]
                                :or {preferred-side :top}}]
  (let [edge-tiles (find-edge-tiles-around-set room-tiles)

        ;; Check orthogonally adjacent (N, S, E, W) only
        ortho-directions [[0 -1] [0 1] [1 0] [-1 0]]

        ;; Find edge tiles adjacent to entrance tiles
        door-candidates (filter (fn [[ex ey]]
                                  (some (fn [[tx ty]]
                                          (and (= (+ ex (first tx))
                                                  (= (+ ey (second ty)))
                                                  (contains? entrance-tiles
                                                             [(+ ex (first tx))
                                                              (+ ey (second ty))]))))
                                        ortho-directions))
                                edge-tiles)]

    (if (seq door-candidates)
      ;; Sort by preference: on preferred side first, then by distance from preferred position
      (let [;; Calculate preferred door position (center of preferred side)
            preferred-x (case preferred-side
                          :top (/ (+ (apply min (map first room-tiles)) (apply max (map first room-tiles))) 2)
                          :bottom (/ (+ (apply min (map first room-tiles)) (apply max (map first room-tiles))) 2)
                          :left (/ (+ (apply min (map second room-tiles)) (apply max (map second room-tiles))) 2)
                          :right (/ (+ (apply min (map second room-tiles)) (apply max (map second room-tiles))) 2))
            preferred-y (case preferred-side
                          :top (- (apply min (map second room-tiles)) 1)
                          :bottom (+ (apply max (map second room-tiles)) 1)
                          :left (/ (+ (apply min (map first room-tiles)) (apply max (map first room-tiles))) 2)
                          :right (/ (+ (apply min (map first room-tiles)) (apply max (map first room-tiles))) 2))

            ;; Sort candidates: on-preferred-side first (0), then by distance (1000 + distance)
            sorted-candidates (sort-by (fn [[x y]]
                                         (let [on-preferred-side? (case preferred-side
                                                                    :top (= y (dec preferred-y))
                                                                    :bottom (= y (inc preferred-y))
                                                                    :left (= x (dec preferred-x))
                                                                    :right (= x (inc preferred-x))
                                                                    false)
                                               distance (Math/sqrt (+ (Math/pow (- x preferred-x) 2)
                                                                      (Math/pow (- y preferred-y) 2)))]
                                           (if on-preferred-side? 0 (+ 1000 distance))))
                                       door-candidates)]
        (first sorted-candidates))
      nil)))



(comment
  (def room-tiles #{[100 100] [101 100] [100 101] [101 101]})
  (find-edge-tiles-around-set room-tiles)
  :rcf)

;; ============================================================================
;; Complete Example Usage
;; ============================================================================

(comment
  "=== Example: Building walls around a room ===

   1. Define room tiles
   2. Find edge tiles
   3. Determine door position
   4. Build walls with door opening"

  (require '[game.common :as common])

  (def room-tiles #{[100 100] [101 100] [100 101] [101 101]})
  (def entrance-tiles #{[100 100]})
  (def tbuilding (common/get-building-material "WOOD"))

  ;; Find edge tiles
  (def edge-tiles (find-edge-tiles-around-set room-tiles))
  
  ;; Find door position (prefer top side)
  (def door-pos (find-door-position room-tiles entrance-tiles :preferred-side :top))
  
  ;; Build walls with door
  (when door-pos
    (let [[door-x door-y] door-pos]
      (build-walls-around-rect 100 100 2 2 door-x door-y tbuilding)))

  :done)
