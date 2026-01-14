(ns repl.tutorial1
  (:require
   [repl.utils :as utils]
   [game.common :refer [get-building-material]])
  (:import
   [game GAME]
   [view.main VIEW]
   [settlement.main SETT]
   [settlement.room.main.construction ConstructionInit]
   [settlement.room.main.placement UtilWallPlacability]
   [settlement.room.main.throne THRONE]))

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

;; Move camera to the throne position
;; Gets the throne's coordinate (in tiles) and moves the camera to center on it
(defn move-to-throne []
  (let [throne-coo (THRONE/coo)
        window (get-game-window)
        tile-x (.x throne-coo)
        tile-y (.y throne-coo)]
    (.centerAtTile window tile-x tile-y)))

(comment
  ;; Move camera to throne
  (move-to-throne)
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
