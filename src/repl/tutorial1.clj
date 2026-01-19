(ns repl.tutorial1
  (:require
   [repl.utils :as utils]
   [game.common :refer [get-building-material]]
   [game.throne :as throne])
  (:import
   [game GAME]
   [view.main VIEW]
   [settlement.main SETT]
   [settlement.room.main.construction ConstructionInit]
   [settlement.room.main.placement UtilWallPlacability]))

;; Get the game window for camera control
(defn get-game-window []
  (let [sett-view (VIEW/s)]
    (.getWindow sett-view)))

;; Move camera to a specific pixel position
(defn move-camera-to [x y]
  (let [window (get-game-window)]
    (.centerAt window x y)))

;; Move camera to a specific tile position
(defn move-camera-to-tile [tile-x tile-y]
  (let [window (get-game-window)]
    (.centerAtTile window tile-x tile-y)))

;; Move camera by a delta (increment)
(defn move-camera-by [dx dy]
  (let [window (get-game-window)]
    (.inc window dx dy)))

;; Move camera in a direction (mimicking WASD)
;; Directions: :up (W), :down (S), :left (A), :right (D)
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

(comment
  ;; Move camera to a specific position
  (move-camera-to 1000 1000)
  
  ;; Move camera to a specific tile
  (move-camera-to-tile 50 50)
  
  ;; Move camera by a delta (one-time movement)
  (move-camera-by 100 100)
  
  ;; Move camera in a direction once
  (move-camera-direction :up :speed 200)
  (move-camera-direction :down :speed 200)
  (move-camera-direction :left :speed 200)
  (move-camera-direction :right :speed 200)
  :rcf)

;; Get current zoom level
(defn get-zoom []
  (let [window (get-game-window)]
    (.zoomout window)))

;; Set zoom level (0 = normal, positive = zoomed out, negative = zoomed in)
(defn set-zoom [level]
  (let [window (get-game-window)]
    (.setZoomout window level)))

;; Zoom in (decrease zoom level by 1)
(defn zoom-in []
  (let [window (get-game-window)]
    (.zoomInc window -1)))

;; Zoom out (increase zoom level by 1)
(defn zoom-out []
  (let [window (get-game-window)]
    (.zoomInc window 1)))

;; Zoom by a delta amount (positive = zoom out, negative = zoom in)
(defn zoom-by [delta]
  (let [window (get-game-window)]
    (.zoomInc window delta)))

(comment
   ;; Zoom functions
  (get-zoom)                    ; Get current zoom level
  (set-zoom 0)                 ; Set zoom to normal (0)
  (set-zoom 1)                 ; Set zoom to level 1 (zoomed out)
  (set-zoom -1)                ; Set zoom to level -1 (zoomed in)
  (zoom-in)                    ; Zoom in by 1 level
  (zoom-out)                   ; Zoom out by 1 level
  (zoom-by 2)                  ; Zoom out by 2 levels
  (zoom-by -1)                 ; Zoom in by 1 level
  :rcf)

;; Warehouse/Stockpile creation functions

;; Get the stockpile room and constructor
(defn get-stockpile-room []
  (let [rooms (SETT/ROOMS)]
    (.-STOCKPILE rooms)))

(comment
  (get-stockpile-room)
  :rcf)

(defn get-stockpile-constructor []
  (let [stockpile-room (get-stockpile-room)]
    (.constructor stockpile-room)))

(comment
  (get-stockpile-constructor)
  :rcf)

(comment
  ;; Building material functions are now in game.common
  (require '[game.common :refer [get-building-material]])
  (get-building-material "WOOD")
  (get-building-material "STONE")
  :rcf)

;; ============================================================================
;; Pure functions for warehouse planning (testable in REPL)
;; ============================================================================

;; Find all edge tiles around a rectangular area
;; Returns a list of [x y] coordinates for tiles that are:
;; - Outside the area
;; - Adjacent to the area (orthogonally)
;; Pure function - no side effects
(defn find-edge-tiles [start-x start-y width height]
  (let [edge-tiles (atom [])]
    ;; Iterate over a 7x7 grid for a 5x5 area (includes all edge tiles)
    ;; Range needs to be inclusive of the outer edge
    ;; For width=5, we need x from start-x-1 to start-x+5 (7 values)
    (doseq [y (range (- start-y 1) (+ start-y height 1 1))
            x (range (- start-x 1) (+ start-x width 1 1))]
      (let [is-inside (and (>= x start-x) (< x (+ start-x width))
                           (>= y start-y) (< y (+ start-y height)))
            is-edge (not is-inside)
            ;; Check if this edge tile is adjacent to the area (orthogonally or diagonally)
            ;; Check all 8 directions: N, S, E, W, NE, NW, SE, SW
            adjacent-to-area (some (fn [[dx-offset dy-offset]]
                                     (let [dx (+ x dx-offset)
                                           dy (+ y dy-offset)]
                                       (and (>= dx start-x) (< dx (+ start-x width))
                                            (>= dy start-y) (< dy (+ start-y height)))))
                                   ;; All 8 directions: orthogonal + diagonal
                                   [[0 -1]   ; N
                                    [0 1]    ; S
                                    [1 0]    ; E
                                    [-1 0]   ; W
                                    [1 -1]   ; NE
                                    [-1 -1]  ; NW
                                    [1 1]    ; SE
                                    [-1 1]])] ; SW
        
        (when (and is-edge adjacent-to-area)
          (swap! edge-tiles conj [x y]))))
    @edge-tiles))

(comment
  (-> (find-edge-tiles 261 430 5 5)
      count)
  :rcf)

;; Calculate door position for a warehouse
;; side can be :top, :bottom, :left, :right
;; Returns [door-x door-y] coordinates
;; Pure function - no side effects
(defn calculate-door-position [start-x start-y width height & {:keys [side] :or {side :top}}]
  (case side
    :top [(+ start-x (quot width 2)) start-y]
    :bottom [(+ start-x (quot width 2)) (+ start-y height -1)]
    :left [start-x (+ start-y (quot height 2))]
    :right [(+ start-x width -1) (+ start-y (quot height 2))]
    ;; Default to top
    [(+ start-x (quot width 2)) start-y]))

(defn calculate-furniture-positions 
  "注意这个返回的 count 是家具的个数，不是 tile 的个数. 需要通过 calculate-occupied-tiles 来计算 occupied 的 tile 个数."
  [center-x center-y width height item-width item-height]
  (let [start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))
        ;; Spacing: leave at least 1 tile gap between items for paths
        spacing-x (max 2 (+ item-width 1))
        spacing-y (max 2 (+ item-height 1))
        positions (atom [])]
    (doseq [y (range start-y (+ start-y height) spacing-y)
            x (range start-x (+ start-x width) spacing-x)]
      (let [end-x (+ x item-width)
            end-y (+ y item-height)
            fits-width (<= end-x (+ start-x width))
            fits-height (<= end-y (+ start-y height))]
        (when (and fits-width fits-height)
          (swap! positions conj [x y]))))
    @positions))

(comment
  (calculate-furniture-positions 261 430 5 5 2 1)
  :rcf)

(defn calculate-occupied-tiles [furniture-positions item-width item-height]
  (let [occupied (atom #{})]
    (doseq [[x y] furniture-positions]
      (doseq [ty (range y (+ y item-height))
              tx (range x (+ x item-width))]
        (swap! occupied conj [tx ty])))
    @occupied))

(comment
  (let [furniture-positions (calculate-furniture-positions 261 430 5 5 2 1)]
    (calculate-occupied-tiles furniture-positions 2 1))
  :rcf)

(defn find-door-position [center-x center-y
                          width height
                          occupied-tiles &
                          {:keys [preferred-side]
                           :or {preferred-side :top}}]
  (let [start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))
        occupied-set (set occupied-tiles)
        edge-tiles (find-edge-tiles start-x start-y width height)
        ;; Check if an inner tile is free (not occupied)
        inner-tile-free? (fn [x y]
                           (and (>= x start-x) (< x (+ start-x width))
                                (>= y start-y) (< y (+ start-y height))
                                (not (contains? occupied-set [x y]))))
        ;; Check if an edge tile is adjacent to at least one free inner tile
        has-free-adjacent? (fn [[edge-x edge-y]]
                             ;; Check all 4 orthogonal directions from edge tile
                             (some (fn [[dx dy]]
                                     (inner-tile-free? (+ edge-x dx) (+ edge-y dy)))
                                   [[0 1]   ; N (if edge is north, inner is south)
                                    [0 -1]  ; S (if edge is south, inner is north)
                                    [1 0]   ; E (if edge is east, inner is west)
                                    [-1 0]])) ; W (if edge is west, inner is east)
        ;; Filter edge tiles to only those with free adjacent inner tiles
        valid-edge-tiles (filter has-free-adjacent? edge-tiles)
        ;; Sort by preference: prefer tiles on the preferred side, then by distance from center
        preferred-door-pos (calculate-door-position start-x start-y width height :side preferred-side)
        [pref-x pref-y] preferred-door-pos
        ;; Sort valid tiles: first by side preference, then by distance from preferred position
        sorted-tiles (sort-by (fn [[x y]]
                                (let [on-preferred-side? (case preferred-side
                                                           :top (= y (- start-y 1))
                                                           :bottom (= y (+ start-y height))
                                                           :left (= x (- start-x 1))
                                                           :right (= x (+ start-x width))
                                                           false)
                                      distance (Math/sqrt (+ (Math/pow (- x pref-x) 2)
                                                             (Math/pow (- y pref-y) 2)))]
                                  ;; Prefer tiles on preferred side, then by distance
                                  (if on-preferred-side? 0 (+ 1000 distance))))
                              valid-edge-tiles)]
    (first sorted-tiles)))

(comment
  ;; Test pure functions in REPL
  
  ;; Example: 5x5 warehouse at center (261, 430)
  (let [center-x 261
        center-y 430
        width 5
        height 5
        item-width 2
        item-height 1]
    
    ;; 1. Calculate furniture positions
    #_(calculate-furniture-positions center-x center-y width height item-width item-height)
    ;; => [[260 430] [262 430] [260 432] [262 432]]
    
    ;; 2. Calculate occupied tiles
    #_(let [furniture-positions (calculate-furniture-positions center-x center-y width height item-width item-height)]
      (calculate-occupied-tiles furniture-positions item-width item-height))
    ;; => #{[259 428]
;;   [263 430]
;;   [262 428]
;;   [263 428]
;;   [259 430]
;;   [259 432]
;;   [260 432]
;;   [263 432]
;;   [262 432]
;;   [262 430]
;;   [260 428]
;;   [260 430]}
    
    ;; 3. Find door position (adjacent to free inner tile)
    (let [furniture-positions (calculate-furniture-positions center-x center-y width height item-width item-height)
          occupied-tiles (calculate-occupied-tiles furniture-positions item-width item-height)]
      (find-door-position center-x center-y width height occupied-tiles :preferred-side :top))
    ;; => [263 429] or another valid edge tile adjacent to a free inner tile
    
    ;; 4. Visualize the complete plan
    #_(let [furniture-positions (calculate-furniture-positions center-x center-y width height item-width item-height)
          occupied-tiles (calculate-occupied-tiles furniture-positions item-width item-height)
          door-tile (find-door-position center-x center-y width height occupied-tiles :preferred-side :top)]
      {:furniture-positions furniture-positions
       :occupied-count (count occupied-tiles)
       :door-tile door-tile})
    ;; => {:furniture-positions [[260 430] ...], :occupied-count 8, :door-tile [263 429]}
  ) 
  :rcf)


;; ============================================================================
;; Warehouse creation function
;; ============================================================================

;; Create a warehouse/stockpile at the specified location
;; center-x, center-y: center tile coordinates
;; width, height: dimensions of the warehouse (in tiles)
;; material-name: building material name (e.g., "WOOD", "STONE")
;; upgrade: upgrade level (default 0)
;; place-furniture: if true, manually place furniture items (default true)
(defn create-warehouse [center-x center-y width height & {:keys [material-name upgrade place-furniture] 
                                                           :or {material-name "WOOD" upgrade 0 place-furniture true}}]
  (let [rooms (SETT/ROOMS)
        stockpile-constructor (get-stockpile-constructor)
        tbuilding (get-building-material material-name)  ; Returns TBuilding (not Structure)
        degrade 0  ; No degradation
        state nil  ; No special state
        
        ;; Create ConstructionInit (note: third parameter is TBuilding, not Structure)
        construction-init (ConstructionInit. upgrade stockpile-constructor tbuilding degrade state)
        
        ;; Get temporary area
        tmp (.tmpArea rooms "warehouse")
        
        ;; Calculate start coordinates
        start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))]
    
    ;; Set the building area
    (doseq [y (range height)
            x (range width)]
      (.set tmp (+ start-x x) (+ start-y y)))
    
    ;; Get furniture item once
    (let [furnisher-groups (.pgroups stockpile-constructor)
          first-group (when (> (.size furnisher-groups) 0)
                        (.get furnisher-groups 0))
          furnisher-item (when first-group
                           (try
                             (.item first-group 0 0)
                             (catch Exception _e nil)))
          
          ;; Calculate furniture and door positions using pure functions
          furniture-positions (when (and place-furniture furnisher-item)
                                (calculate-furniture-positions center-x center-y width height 
                                                             (.width furnisher-item) 
                                                             (.height furnisher-item)))
          occupied-tiles (when furniture-positions
                           (calculate-occupied-tiles furniture-positions 
                                                    (.width furnisher-item) 
                                                    (.height furnisher-item)))
          door-tile (find-door-position center-x center-y width height 
                                        (or occupied-tiles #{}) 
                                        :preferred-side :top)]
      
      ;; Place furniture (crates) - MUST be done before createClean
      (when (and place-furniture furniture-positions furnisher-item)
        (let [fdata (.fData rooms)
              room-instance (.room tmp)]
          ;; Place each furniture item at the calculated positions
          (doseq [[x y] furniture-positions]
            (try
              (.itemSet fdata x y furnisher-item room-instance)
              (catch Exception _e
                ;; Skip if placement fails
                nil)))))
      
      ;; Build walls around the warehouse with a door
      (let [edge-tiles (find-edge-tiles start-x start-y width height)
            is-door-location? (fn [[x y]]
                               (and door-tile
                                    (= x (first door-tile))
                                    (= y (second door-tile))))]
        
        ;; Place door at the door location (roof only, no wall)
        (when door-tile
          (let [[door-tx door-ty] door-tile]
            (when (UtilWallPlacability/openingCanBe door-tx door-ty)
              (UtilWallPlacability/openingBuild door-tx door-ty tbuilding))))
        
        ;; Build walls on all edge tiles (except door location)
        (doseq [[x y] edge-tiles]
          (when (and (UtilWallPlacability/wallCanBe x y)
                     (not (is-door-location? [x y])))
            (UtilWallPlacability/wallBuild x y tbuilding)))))
    
    ;; Create the construction site
    (.createClean (.construction rooms) tmp construction-init)
    
    ;; Clear temporary area
    (.clear tmp)
    
    {:success true
     :center-x center-x
     :center-y center-y
     :width width
     :height height
     :furniture-placed place-furniture}))

;; Create a warehouse using update-once (ensures it happens in a single frame)
(defn create-warehouse-once [center-x center-y width height & {:keys [material-name upgrade] 
                                                                 :or {material-name "WOOD" upgrade 0}}]
  (utils/update-once 
   (fn [_ds]
     (create-warehouse center-x center-y width height 
                      :material-name material-name 
                      :upgrade upgrade))))

;; Example usage:
(comment
  ;; Warehouse creation with furniture
  ;; Create a 5x5 warehouse at tile (261, 400) using wood (with furniture)
  (create-warehouse-once 251 430 5 5)
  
  ;; Create warehouse without furniture (furniture will be auto-placed by game)
  (create-warehouse-once 280 400 5 5 :place-furniture false)
  
  ;; Create a 3x3 warehouse at tile (120, 120) using stone
  (create-warehouse-once 120 120 3 3 :material-name "STONE")
  
  ;; Create a larger warehouse (7x7) at tile (130, 130)
  (create-warehouse-once 130 130 7 7)
  
  ;; Check furniture in the warehouse
  (warehouse-furniture-info 261 400 5 5)
  
  :rcf)

(comment
  ;; Move camera to throne
  (throne/move-to-throne)
  :rcf)

;; ============================================================================
;; Furniture Inspection Functions
;; ============================================================================

;; Get FurnisherData instance
(defn get-furniture-data []
  (let [rooms (SETT/ROOMS)]
    (.fData rooms)))

;; Get furniture item at a specific tile
(defn get-furniture-item [tx ty]
  (let [fdata (get-furniture-data)]
    (.get (.item fdata) tx ty)))

;; Get furniture tile at a specific tile
(defn get-furniture-tile [tx ty]
  (let [fdata (get-furniture-data)]
    (.get (.tile fdata) tx ty)))

;; Check if a tile has furniture
(defn has-furniture? [tx ty]
  (let [item (get-furniture-item tx ty)]
    (some? item)))

;; Check if a tile has a crate (for stockpiles)
(defn has-crate? [tx ty]
  (let [stockpile-constructor (get-stockpile-constructor)]
    (try
      ;; Try direct method call if available
      (.isCrate stockpile-constructor tx ty)
      (catch Exception _e
        ;; Fallback: check if tile is a crate tile
        (let [fdata (get-furniture-data)
              tile (.get (.tile fdata) tx ty)]
          (some? tile))))))

;; Get furniture info at a tile
(defn furniture-info [tx ty]
  (let [fdata (get-furniture-data)
        item (.get (.item fdata) tx ty)
        tile (.get (.tile fdata) tx ty)]
    {:has-item (some? item)
     :has-tile (some? tile)
     :item item
     :tile tile
     :item-width (when item (.width item))
     :item-height (when item (.height item))
     :item-area (when item (.-area item))
     :item-rotation (when item (.-rotation item))}))

;; Scan an area for furniture
(defn scan-furniture-area [start-x start-y width height]
  (let [results (atom [])]
    (doseq [y (range height)
            x (range width)]
      (let [tx (+ start-x x)
            ty (+ start-y y)
            info (furniture-info tx ty)]
        (when (or (:has-item info) (:has-tile info))
          (swap! results conj {:x tx :y ty :info info}))))
    @results))

;; Get furniture info for warehouse at center (261, 400) with 5x5 size
(defn warehouse-furniture-info [center-x center-y width height]
  (let [start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))
        furniture-map (atom {})]
    (doseq [y (range height)
            x (range width)]
      (let [tx (+ start-x x)
            ty (+ start-y y)
            info (furniture-info tx ty)
            crate? (has-crate? tx ty)]
        (when (or (:has-item info) (:has-tile info) crate?)
          (swap! furniture-map assoc [tx ty]
                 (assoc info :has-crate crate?)))))
    {:center-x center-x
     :center-y center-y
     :width width
     :height height
     :start-x start-x
     :start-y start-y
     :furniture @furniture-map
     :total-tiles-with-furniture (count @furniture-map)}))

(comment
  ;; Furniture inspection examples for warehouse at (261, 400)
  
  ;; Get furniture data instance
  (get-furniture-data)
  
  ;; Check a specific tile
  (furniture-info 259 398)  ; Top-left area of warehouse
  (furniture-info 261 400)  ; Center of warehouse
  (furniture-info 263 402)  ; Bottom-right area of warehouse
  
  ;; Check if tile has crate
  (has-crate? 259 398)
  (has-crate? 261 400)
  
  ;; Check if tile has any furniture
  (has-furniture? 259 398)
  
  ;; Get furniture item/tile directly
  (get-furniture-item 259 398)
  (get-furniture-tile 259 398)
  (get-furniture-item 261 400)
  (get-furniture-tile 261 400)
  
  ;; Scan the entire warehouse area (5x5, centered at 261, 400)
  (warehouse-furniture-info 261 400 5 5)
  
  ;; Scan a smaller area
  (scan-furniture-area 259 398 3 3)
  
  :rcf)

;; ============================================================================
;; Time Flow Control
;; ============================================================================

;; Get the GameSpeed instance
(defn get-game-speed []
  (GAME/SPEED))

;; Set game speed (0 = paused, 1 = normal, 5 = 5x, 25 = 25x, etc.)
(defn set-time-speed [speed]
  (let [game-speed (get-game-speed)]
    (.speedSet game-speed speed)))

;; Get current time speed
(defn get-time-speed []
  (let [game-speed (get-game-speed)]
    (.speed game-speed)))

;; Get target time speed (what speed is set to, before any adjustments)
(defn get-time-speed-target []
  (let [game-speed (get-game-speed)]
    (.speedTarget game-speed)))

;; Pause the game (sets speed to 0)
(defn pause-time []
  (set-time-speed 0))

;; Resume time at normal speed (1x)
(defn resume-time []
  (set-time-speed 1))

;; Toggle pause (if paused, resume to previous speed; if running, pause)
(defn toggle-pause []
  (let [game-speed (get-game-speed)]
    (.togglePause game-speed)))

;; Set time to specific speeds (matching the game's speed buttons)
(defn time-speed-0x
  "Pause (0x speed)"
  []
  (set-time-speed 0))

(defn time-speed-1x
  "Normal speed (1x)"
  []
  (set-time-speed 1))

(defn time-speed-5x
  "Fast speed (5x)"
  []
  (set-time-speed 5))

(defn time-speed-25x
  "Very fast speed (25x)"
  []
  (set-time-speed 25))

(comment
  ;; Time flow control examples
  (pause-time)                    ; Pause the game
  (resume-time)                   ; Resume at normal speed
  (toggle-pause)                  ; Toggle pause/resume
  
  ;; Set specific speeds
  (time-speed-0x)                  ; Pause (0x)
  (time-speed-1x)                  ; Normal (1x)
  (time-speed-5x)                  ; Fast (5x)
  (time-speed-25x)                 ; Very fast (25x)
  
  ;; Custom speed
  (set-time-speed 10)              ; Set to 10x speed
  
  ;; Get current speed
  (get-time-speed)                 ; Get actual current speed
  (get-time-speed-target)          ; Get target speed setting

  :rcf)

;; ============================================================================
;; Home Building Functions
;; ============================================================================

;; Get the home room blueprint
(defn get-home-room []
  (let [rooms (SETT/ROOMS)]
    (.HOME rooms)))

;; Get the home constructor
(defn get-home-constructor []
  (.constructor (get-home-room)))

;; Find edge tiles around a set of room tiles (for irregular shapes)
;; Returns a set of [x y] coordinates for tiles that are:
;; - Outside the room
;; - Adjacent to the room (8 directions)
;; Pure function - no side effects
(defn- find-room-edge-tiles [room-tiles]
  (let [directions [[-1 -1] [0 -1] [1 -1]
                    [-1 0]         [1 0]
                    [-1 1]  [0 1]  [1 1]]
        edge-tiles (atom #{})]
    (doseq [[tx ty] room-tiles
            [dx dy] directions]
      (let [adj-x (+ tx dx)
            adj-y (+ ty dy)]
        ;; If adjacent tile is outside the room, it's an edge tile
        (when-not (contains? room-tiles [adj-x adj-y])
          (swap! edge-tiles conj [adj-x adj-y]))))
    @edge-tiles))

;; Find a single door position for a room
;; entrance-tiles: tiles marked as noWalls in the furnisher item (preferred door adjacency)
;; room-tiles: all tiles belonging to the room
;; Returns a single [x y] coordinate for the door
;; Pure function - no side effects
(defn- find-home-door-position [room-tiles entrance-tiles]
  (let [edge-tiles (find-room-edge-tiles room-tiles)
        ;; Prefer edge tiles adjacent to entrance tiles
        ;; Check orthogonal adjacency only for door placement
        ortho-directions [[0 -1] [0 1] [1 0] [-1 0]]
        ;; Find edge tiles that are orthogonally adjacent to entrance tiles
        door-candidates (filter (fn [[ex ey]]
                                  (some (fn [[dx dy]]
                                          (contains? entrance-tiles [(+ ex dx) (+ ey dy)]))
                                        ortho-directions))
                                edge-tiles)]
    ;; Return the first candidate (could be improved with better heuristics)
    (first door-candidates)))

;; Build walls around a room with a single door
;; room-tiles: set of [x y] coordinates that belong to the room
;; door-tile: single [x y] coordinate for the door opening
;; tbuilding: the building material (TBuilding)
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

;; Create a home at specified center coordinates
;; Parameters:
;;   center-x, center-y: Center tile coordinates
;;   home-type: Home type (0 = 3x3 small, 1 = 3x5 medium, 2 = 5x6 large)
;;   variation: Size variation within type (0 = base, higher = wider homes)
;;   rotation: Rotation (0-3)
;;   upgrade: Upgrade level (default 0)
;; Returns: Map with creation result
(defn create-home [center-x center-y home-type & {:keys [variation rotation upgrade material-name]
                                                    :or {variation 0 rotation 0 upgrade 0 material-name "WOOD"}}]
  (let [rooms (SETT/ROOMS)
        home-constructor (get-home-constructor)
        tbuilding (get-building-material material-name)

        ;; Get the correct FurnisherItemGroup for the home type
        ;; Group 0 = 3x3 homes, Group 1 = 3x5 homes, Group 2 = 5x6 homes
        furnisher-group (.get (.pgroups home-constructor) home-type)

        ;; Get the FurnisherItem with specified variation and rotation
        furnisher-item (.item furnisher-group variation rotation)

        ;; Get actual dimensions from the item
        width (.width furnisher-item)
        height (.height furnisher-item)

        ;; Calculate start position from center
        start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))

        ;; Create tmp area
        tmp (.tmpArea rooms "home")

        ;; Collect room tiles and entrance tiles for wall building
        room-tiles (atom #{})
        entrance-tiles (atom #{})]

    ;; Set the building area - only tiles where the item has content
    (doseq [y (range height)
            x (range width)]
      (when-let [tile (.get furnisher-item x y)]
        (let [world-x (+ start-x x)
              world-y (+ start-y y)]
          (.set tmp world-x world-y)
          (swap! room-tiles conj [world-x world-y])
          ;; Track entrance tiles (noWalls = true) for door position preference
          (when (.-noWalls tile)
            (swap! entrance-tiles conj [world-x world-y])))))

    ;; Place FurnisherItem in fData BEFORE creating construction (critical!)
    (.itemSet (.fData rooms) start-x start-y furnisher-item (.room tmp))

    ;; Find a SINGLE door position (using entrance tiles as preference)
    (let [door-tile (find-home-door-position @room-tiles @entrance-tiles)]
      ;; Build walls around the room with single door (BEFORE creating construction)
      (build-walls-around-room @room-tiles door-tile tbuilding)

      ;; Create ConstructionInit with the structure (TBuilding) for walls
      (let [construction-init (ConstructionInit. upgrade home-constructor tbuilding 0 nil)]
        ;; Create the construction site
        (.createClean (.construction rooms) tmp construction-init))

      ;; tmp is cleared by createClean

      {:success true
       :center-x center-x
       :center-y center-y
       :start-x start-x
       :start-y start-y
       :width width
       :height height
       :home-type home-type
       :variation variation
       :rotation rotation
       :material material-name
       :door-tile door-tile})))

;; Create a home using update-once (ensures it happens in a single frame)
(defn create-home-once [center-x center-y home-type & {:keys [variation rotation upgrade material-name]
                                                        :or {variation 0 rotation 0 upgrade 0 material-name "WOOD"}}]
  (utils/update-once
   (fn [_ds]
     (create-home center-x center-y home-type
                  :variation variation
                  :rotation rotation
                  :upgrade upgrade
                  :material-name material-name))))

(comment
  ;; Home creation examples

  ;; Home types:
  ;;   0 = 3x3 small home
  ;;   1 = 3x5 medium home
  ;;   2 = 5x6 large home

  ;; Create a small home (3x3, type 0) at tile (290, 400)
  (create-home-once 290 400 0)

  ;; Create a medium home (3x5, type 1) at tile (300, 400)
  (create-home-once 300 400 1)

  ;; Create a large home (5x6, type 2) at tile (310, 400)
  (create-home-once 310 400 2)

  ;; Create a rotated small home (rotation 1 = 90 degrees)
  (create-home-once 320 400 0 :rotation 1)

  ;; Create a wider variation of small home (variation 1 = 2x width)
  (create-home-once 330 400 0 :variation 1)

  :rcf)

;; ============================================================================
;; Road Building Functions
;; ============================================================================

;; Get all road types info
(defn all-road-types-info []
  (let [floors (SETT/FLOOR)
        roads (.roads floors)
        jobs (SETT/JOBS)
        road-jobs (.roads jobs)]
    (map-indexed (fn [idx floor]
                   {:index idx
                    :key (.key floor)
                    :name (str (.name floor))
                    :job (.get (.all road-jobs) idx)})
                 roads)))

;; Get the default dirt road job (uses SETT.FLOOR().defaultRoad)
(defn get-default-road-job []
  (let [floors (SETT/FLOOR)
        default-road (.defaultRoad floors)
        jobs (SETT/JOBS)
        road-jobs (.roads jobs)
        ;; Find the index of defaultRoad in the roads list
        roads-list (.roads floors)
        default-index (loop [i 0]
                       (if (< i (.size roads-list))
                         (if (= (.get roads-list i) default-road)
                           i
                           (recur (inc i)))
                         nil))]
    (if default-index
      (.get (.all road-jobs) default-index)
      ;; Fallback to index 0 if default road not found
      (.get (.all road-jobs) 0))))

;; Get road job by index
(defn get-road-job-by-index [index]
  (let [jobs (SETT/JOBS)
        road-jobs (.roads jobs)]
    (when (< index (.size (.all road-jobs)))
      (.get (.all road-jobs) index))))

;; Get road job by key (e.g., "_DEFAULT_ROAD")
(defn get-road-job-by-key [key]
  (let [floors (SETT/FLOOR)
        roads (.roads floors)
        jobs (SETT/JOBS)
        road-jobs (.roads jobs)]
    (loop [i 0]
      (if (< i (.size roads))
        (let [floor (.get roads i)]
          (if (= (.key floor) key)
            (.get (.all road-jobs) i)
            (recur (inc i))))
        nil))))

(comment
  ;; Get all road types info
  (all-road-types-info)
  
  ;; Get default road job
  (get-default-road-job)
  
  ;; Get road job by index
  (get-road-job-by-index 0)
  (get-road-job-by-index 1)
  
  ;; Get road job by key
  (get-road-job-by-key "_DEFAULT_ROAD")
  
  :rcf)

;; Build a single road tile
;; tx, ty: tile coordinates
;; road-type: optional, can be:
;;   - nil or :default - uses default dirt road
;;   - integer index - uses road at that index
;;   - string key - uses road with that key (e.g., "_DEFAULT_ROAD")
(defn build-road-tile-once [tx ty & {:keys [road-type] :or {road-type :default}}]
  (utils/update-once
   (fn [_ds]
     (let [road-job (cond
                     (= road-type :default) (get-default-road-job)
                     (integer? road-type) (get-road-job-by-index road-type)
                     (string? road-type) (get-road-job-by-key road-type)
                     :else (get-default-road-job))
           placer (.placer road-job)]
       ;; Try to place the road (similar to clear-wood-once approach)
       ;; Roads may need to be placed directly without isPlacable check
       (try
         (.place placer tx ty nil nil)
         (catch Exception _
           ;; If placement fails, check why
           (let [error-msg (.isPlacable placer tx ty nil nil)]
             (println "Cannot place road at" tx ty ":" error-msg))))))))

;; Debug version: Check if road can be placed and print the result
(defn check-road-placement [tx ty]
  (let [road-job (get-default-road-job)
        placer (.placer road-job)
        result (.isPlacable placer tx ty nil nil)]
    (println "Road placement check at" tx ty ":" (if (nil? result) "OK" result))
    (nil? result)))

;; Alternative: Build road using the combo placer
(defn build-road-tile-combo-once [tx ty]
  (utils/update-once
   (fn [_ds]
     (let [jobs (SETT/JOBS)
           road-jobs (.roads jobs)
           combo-placer (.pla road-jobs)
           current-placer (.current combo-placer)]
       (if current-placer
         (try
           (.place current-placer tx ty nil nil)
           (catch Exception e
             (println "Error placing road:" (.getMessage e))))
         (println "No current placer available - try setting it first"))))))

;; Alternative: Build road directly using floor.placeFixed
;; This bypasses the job system and places the road floor directly
(defn build-road-tile-direct-once [tx ty]
  (utils/update-once
   (fn [_ds]
     (let [floors (SETT/FLOOR)
           default-road (.defaultRoad floors)]
       (try
         (.placeFixed default-road tx ty)
         (catch Exception e
           (println "Error placing road directly:" (.getMessage e))))))))

(comment
  ;; Debug: Check if road can be placed
  (check-road-placement 280 390)
  (check-road-placement 290 403)
  
  ;; Build roads with different types:
  ;; 1. Default dirt road (recommended)
  (build-road-tile-once 280 390 :road-type :default)
  (build-road-tile-once 290 403)  ;; :default is the default
  
  ;; 2. By index (check all-road-types-info first to see available indices)
  (build-road-tile-once 280 390 :road-type 0)
  (build-road-tile-once 280 390 :road-type 1)
  
  ;; 3. By key
  (build-road-tile-once 280 390 :road-type "_DEFAULT_ROAD")
  
  ;; Build multiple tiles with specific road type
  (build-road-tiles-once [[100 100] [100 101] [100 102]] :road-type :default)
  
  ;; Alternative approaches:
  ;; 1. Try using the combo placer
  (build-road-tile-combo-once 280 390)
  
  ;; 2. Try placing road directly using floor.placeFixed
  (build-road-tile-direct-once 290 403)
  
  :rcf)

;; Build multiple road tiles in a line or pattern
;; tiles: list of [x y] coordinates
;; road-type: optional, same as build-road-tile-once
(defn build-road-tiles-once [tiles & {:keys [road-type] :or {road-type :default}}]
  (utils/update-once
   (fn [_ds]
     (let [road-job (cond
                     (= road-type :default) (get-default-road-job)
                     (integer? road-type) (get-road-job-by-index road-type)
                     (string? road-type) (get-road-job-by-key road-type)
                     :else (get-default-road-job))
           placer (.placer road-job)]
       (doseq [[tx ty] tiles]
         (when (nil? (.isPlacable placer tx ty nil nil))
           (.place placer tx ty nil nil)))))))

;; Helper: Calculate a simple road path between house positions
;; house-positions: list of [house-x house-y door-tile] tuples
;; Returns: list of [x y] coordinates for road tiles
(defn calculate-road-path [house-positions]
  (let [road-tiles (atom #{})]  ;; Use set to avoid duplicates
    ;; Simple approach: create a path connecting house doors
    ;; For each house, add a few tiles extending outward from the door
    (doseq [[_ _ door-tile] house-positions]
      (when door-tile
        (let [[door-x door-y] door-tile
              ;; Add 3-4 tiles extending outward from door (south direction)
              path-tiles (for [i (range 1 4)]
                          [door-x (+ door-y i)])]
          (doseq [tile path-tiles]
            (swap! road-tiles conj tile)))))
    ;; If we have multiple houses, try to connect them with a horizontal path
    (when (> (count house-positions) 1)
      (let [door-tiles (filter some? (map (fn [[_ _ dt]] dt) house-positions))
            min-x (apply min (map first door-tiles))
            max-x (apply max (map first door-tiles))
            avg-y (int (Math/round (double (/ (reduce + (map second door-tiles)) (count door-tiles)))))]
        ;; Add horizontal connecting path
        (doseq [x (range min-x (inc max-x))]
          (swap! road-tiles conj [x avg-y]))))
    (vec @road-tiles)))

;; Build roads near houses
;; house-positions: list of [house-x house-y door-tile] tuples
;; Builds up to 10 tiles of road connecting the houses
;; road-type: optional, same as build-road-tile-once
(defn build-roads-near-houses-once [house-positions & {:keys [road-type] :or {road-type :default}}]
  (utils/update-once
   (fn [_ds]
     (let [road-job (cond
                     (= road-type :default) (get-default-road-job)
                     (integer? road-type) (get-road-job-by-index road-type)
                     (string? road-type) (get-road-job-by-key road-type)
                     :else (get-default-road-job))
           placer (.placer road-job)
           ;; Calculate road tiles - connect houses with a simple path
           road-tiles (calculate-road-path house-positions)]
       ;; Build up to 10 road tiles
       (doseq [[tx ty] (take 10 road-tiles)]
         (when (nil? (.isPlacable placer tx ty nil nil))
           (.place placer tx ty nil nil)))))))

;; Alternative: Build roads in a grid pattern
;; start-x, start-y: top-left corner
;; width, height: dimensions for the grid
;; road-type: optional, same as build-road-tile-once
(defn build-road-grid-once [start-x start-y width height & {:keys [road-type] :or {road-type :default}}]
  (utils/update-once
   (fn [_ds]
     (let [road-job (cond
                     (= road-type :default) (get-default-road-job)
                     (integer? road-type) (get-road-job-by-index road-type)
                     (string? road-type) (get-road-job-by-key road-type)
                     :else (get-default-road-job))
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
           (.place placer tx ty nil nil)))))))

(comment
  ;; Road building examples

  ;; Build a single road tile
  (build-road-tile-once 280 390)


  ;; Build multiple road tiles
  (build-road-tiles-once [[100 100] [100 101] [100 102] [101 102] [102 102]])

  ;; Build roads near houses (after creating houses)
  ;; Note: create-home-once returns immediately, so you may need to wait
  ;; or use the door-tile from the result if available
  (build-roads-near-houses-once 
   [[290 400 [290 399]]
    [300 400 [300 399]]
    [310 400 [310 399]]])

  ;; Build roads in a grid pattern
  (build-road-grid-once 280 390 30 20)

  (move-camera-to-tile 280 390)

  :rcf)

;; ============================================================================
;; Complete Example: Build Houses and Roads
;; ============================================================================

;; Build multiple houses and connect them with roads
;; center-x, center-y: starting position
;; count: number of houses to build
;; spacing: spacing between houses (default 10)
;; Returns: list of house creation results
(defn build-houses-with-roads-once [center-x center-y count & {:keys [spacing home-type]
                                                                :or {spacing 10 home-type 0}}]
  (utils/update-once
   (fn [_ds]
     (let [houses (atom [])
           house-positions (atom [])]
       ;; Build houses in a row
       (doseq [i (range count)]
         (let [x (+ center-x (* i spacing))
               y center-y
               result (create-home x y home-type)]
           (swap! houses conj result)
           ;; Collect door position for road building
           (when-let [door-tile (:door-tile result)]
             (swap! house-positions conj [x y door-tile]))))
       
       ;; Build roads connecting the houses (up to 10 tiles)
       (when (seq @house-positions)
         (let [road-job (get-default-road-job)
               placer (.placer road-job)
               road-tiles (calculate-road-path @house-positions)]
           (doseq [[tx ty] (take 10 road-tiles)]
             (when (nil? (.isPlacable placer tx ty nil nil))
               (.place placer tx ty nil nil)))))
       
       @houses))))

(comment
  ;; Complete example: Build 3 houses and connect them with roads
  (build-houses-with-roads-once 290 400 3 :spacing 10 :home-type 0)

  :rcf)
