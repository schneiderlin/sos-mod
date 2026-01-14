(ns game.maintenance
  (:require 
   [repl.utils :as utils]
   [game.common :refer [get-building-material array-list-resize->vec]])
  (:import 
   [settlement.main SETT]
   [settlement.room.main.construction ConstructionInit]
   [settlement.room.main.placement UtilWallPlacability]
   [settlement.room.main.job ROOM_EMPLOY_AUTO]))

;; Get the janitor (maintenance station) room blueprint
(defn get-maintenance []
  (let [rooms (SETT/ROOMS)]
    (.-JANITOR rooms)))

(comment
  (get-maintenance)
  :rcf)

;; Get the maintenance station constructor
(defn get-maintenance-constructor []
  (let [maintenance-room (get-maintenance)]
    (.constructor maintenance-room)))

(comment
  (get-maintenance-constructor)
  :rcf)

;; ============================================================================
;; Pure functions for maintenance station planning
;; ============================================================================

;; Find all edge tiles around a rectangular area
;; Returns a list of [x y] coordinates for tiles that are:
;; - Outside the area
;; - Adjacent to the area (orthogonally or diagonally)
(defn find-edge-tiles [start-x start-y width height]
  (let [edge-tiles (atom [])]
    (doseq [y (range (- start-y 1) (+ start-y height 1 1))
            x (range (- start-x 1) (+ start-x width 1 1))]
      (let [is-inside (and (>= x start-x) (< x (+ start-x width))
                          (>= y start-y) (< y (+ start-y height)))
            is-edge (not is-inside)
            ;; Check if this edge tile is adjacent to the area
            adjacent-to-area (some (fn [[dx-offset dy-offset]]
                                     (let [dx (+ x dx-offset)
                                           dy (+ y dy-offset)]
                                       (and (>= dx start-x) (< dx (+ start-x width))
                                            (>= dy start-y) (< dy (+ start-y height)))))
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

;; Calculate door position for a maintenance station
;; side can be :top, :bottom, :left, :right
;; Returns [door-x door-y] coordinates
(defn calculate-door-position [start-x start-y width height & {:keys [side] :or {side :top}}]
  (case side
    :top [(+ start-x (quot width 2)) start-y]
    :bottom [(+ start-x (quot width 2)) (+ start-y height -1)]
    :left [start-x (+ start-y (quot height 2))]
    :right [(+ start-x width -1) (+ start-y (quot height 2))]
    ;; Default to top
    [(+ start-x (quot width 2)) start-y]))

;; Calculate positions for workbench furniture (5x1)
;; Returns a list of [x y] positions where workbenches can be placed
(defn calculate-workbench-positions [center-x center-y width height workbench-width workbench-height]
  (let [start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))
        ;; Spacing: leave at least 1 tile gap between items
        spacing-x (max 2 (+ workbench-width 1))
        spacing-y (max 2 (+ workbench-height 1))
        positions (atom [])]
    (doseq [y (range start-y (+ start-y height) spacing-y)
            x (range start-x (+ start-x width) spacing-x)]
      (let [end-x (+ x workbench-width)
            end-y (+ y workbench-height)
            fits-width (<= end-x (+ start-x width))
            fits-height (<= end-y (+ start-y height))]
        (when (and fits-width fits-height)
          (swap! positions conj [x y]))))
    @positions))

;; Calculate positions for tool furniture (2x1, 3x1, or 4x1)
;; Returns a list of [x y] positions where tools can be placed
(defn calculate-tool-positions [center-x center-y width height tool-width tool-height]
  (let [start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))
        ;; Spacing: leave at least 1 tile gap between items
        spacing-x (max 2 (+ tool-width 1))
        spacing-y (max 2 (+ tool-height 1))
        positions (atom [])]
    (doseq [y (range start-y (+ start-y height) spacing-y)
            x (range start-x (+ start-x width) spacing-x)]
      (let [end-x (+ x tool-width)
            end-y (+ y tool-height)
            fits-width (<= end-x (+ start-x width))
            fits-height (<= end-y (+ start-y height))]
        (when (and fits-width fits-height)
          (swap! positions conj [x y]))))
    @positions))

;; Calculate occupied tiles by furniture
(defn calculate-occupied-tiles [furniture-positions item-width item-height]
  (let [occupied (atom #{})]
    (doseq [[x y] furniture-positions]
      (doseq [ty (range y (+ y item-height))
              tx (range x (+ x item-width))]
        (swap! occupied conj [tx ty])))
    @occupied))

;; Find door position that is adjacent to a free inner tile
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
                            (some (fn [[dx dy]]
                                    (inner-tile-free? (+ edge-x dx) (+ edge-y dy)))
                                  [[0 1]   ; N
                                   [0 -1]  ; S
                                   [1 0]   ; E
                                   [-1 0]])) ; W
        ;; Filter edge tiles to only those with free adjacent inner tiles
        valid-edge-tiles (filter has-free-adjacent? edge-tiles)
        ;; Sort by preference
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

;; Select tool furniture size based on available space
;; Returns size index: 0 (2x1), 1 (3x1), or 2 (4x1)
(defn select-tool-furniture-size [area-width area-height]
  ;; Tool sizes: 2x1 (size 0), 3x1 (size 1), 4x1 (size 2)
  (let [sizes [{:size 0 :width 2 :height 1}
               {:size 1 :width 3 :height 1}
               {:size 2 :width 4 :height 1}]
        fitting-sizes (filter (fn [{:keys [width height]}]
                               (and (<= width area-width) (<= height area-height)))
                             sizes)]
    (if (empty? fitting-sizes)
      0  ; Default to smallest size
      (:size (last (sort-by :size fitting-sizes))))))

;; ============================================================================
;; Maintenance station creation function
;; ============================================================================

;; Create a maintenance station at specified location
;; center-x, center-y: center tile coordinates
;; width, height: dimensions of the maintenance station (in tiles)
;; material-name: building material name (e.g., "WOOD", "STONE")
;; upgrade: upgrade level (default 0)
;; place-furniture: if true, manually place furniture items (default true)
;; workbench-positions: optional list of [x y] positions for workbenches (if nil, auto-calculate)
;; tool-positions: optional list of [x y] positions for tools (if nil, auto-calculate)
(defn create-maintenance [center-x center-y width height & {:keys [material-name upgrade place-furniture workbench-positions tool-positions] 
                                                             :or {material-name "WOOD" upgrade 0 place-furniture true}}]
  (let [rooms (SETT/ROOMS)
        maintenance-constructor (get-maintenance-constructor)
        tbuilding (get-building-material material-name)
        construction-init (ConstructionInit. upgrade maintenance-constructor tbuilding 0 nil)
        tmp (.tmpArea rooms "maintenance")
        
        ;; Calculate start coordinates
        start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))
        
        ;; Get furniture groups
        furnisher-groups (.pgroups maintenance-constructor)
        workbench-group (when (> (.size furnisher-groups) 0)
                         (.get furnisher-groups 0))  ; Workbench group (index 0)
        tool-group (when (> (.size furnisher-groups) 1)
                    (.get furnisher-groups 1))  ; Tool group (index 1)
        
        ;; Get workbench furniture item (5x1, size 0, rot 0)
        workbench-item (when workbench-group
                        (try
                          (.item workbench-group 0 0)  ; size 0, rot 0
                          (catch Exception _e nil)))
        
        ;; Get tool furniture item (select appropriate size)
        tool-size (select-tool-furniture-size width height)
        tool-item (when tool-group
                   (try
                     (.item tool-group tool-size 0)  ; selected size, rot 0
                     (catch Exception _e nil)))]
    
    ;; Set the building area
    (doseq [y (range height)
            x (range width)]
      (.set tmp (+ start-x x) (+ start-y y)))
    
    ;; Calculate or use provided furniture positions
    (let [final-workbench-positions (or workbench-positions
                                       (when (and place-furniture workbench-item)
                                         (calculate-workbench-positions center-x center-y width height 
                                                                      (.width workbench-item) 
                                                                      (.height workbench-item))))
          final-tool-positions (or tool-positions
                                 (when (and place-furniture tool-item)
                                   (calculate-tool-positions center-x center-y width height 
                                                            (.width tool-item) 
                                                            (.height tool-item))))
          
          ;; Calculate all occupied tiles
          workbench-occupied (when final-workbench-positions
                              (calculate-occupied-tiles final-workbench-positions 
                                                        (.width workbench-item) 
                                                        (.height workbench-item)))
          tool-occupied (when final-tool-positions
                         (calculate-occupied-tiles final-tool-positions 
                                                   (.width tool-item) 
                                                   (.height tool-item)))
          all-occupied (into (or workbench-occupied #{}) 
                             (or tool-occupied #{}))
          
          ;; Find door position
          door-tile (find-door-position center-x center-y width height 
                                       all-occupied 
                                       :preferred-side :top)
          
          fdata (.fData rooms)
          room-instance (.room tmp)]
      
      ;; Place workbench furniture - MUST be done before createClean
      (when (and place-furniture final-workbench-positions workbench-item)
        (doseq [[x y] final-workbench-positions]
          (try
            (.itemSet fdata x y workbench-item room-instance)
            (catch Exception _e
              ;; Skip if placement fails
              nil))))
      
      ;; Place tool furniture - MUST be done before createClean
      (when (and place-furniture final-tool-positions tool-item)
        (doseq [[x y] final-tool-positions]
          (try
            (.itemSet fdata x y tool-item room-instance)
            (catch Exception _e
              ;; Skip if placement fails
              nil))))
      
      ;; Build walls around the maintenance station with a door
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
     :room-type "MAINTENANCE"
     :furniture-placed place-furniture
     :workbench-count (count (or workbench-positions []))
     :tool-count (count (or tool-positions []))}))

;; Create a maintenance station using update-once (ensures it happens in a single frame)
(defn create-maintenance-once [center-x center-y width height & {:keys [material-name upgrade place-furniture workbench-positions tool-positions] 
                                                                  :or {material-name "WOOD" upgrade 0 place-furniture true}}]
  (utils/update-once 
   (fn [_ds]
     (create-maintenance center-x center-y width height 
                        :material-name material-name 
                        :upgrade upgrade
                        :place-furniture place-furniture
                        :workbench-positions workbench-positions
                        :tool-positions tool-positions))))

(comment
  ;; Example usage:
  
  ;; Create a 7x5 maintenance station at center (271, 430) using wood
  ;; Furniture positions will be auto-calculated
  (create-maintenance-once 281 438 7 5)
  
  ;; Create a larger maintenance station (10x8)
  (create-maintenance-once 300 400 10 8)
  
  ;; Create a maintenance station using stone
  (create-maintenance-once 250 250 7 5 :material-name "STONE")
  
  ;; Create with custom furniture positions
  (create-maintenance-once 280 400 7 5 
                          :workbench-positions [[260 398] [265 398]]
                          :tool-positions [[260 400] [263 400] [266 400]])
  
  ;; Create without furniture (furniture will be auto-placed by game)
  (create-maintenance-once 280 400 7 5 :place-furniture false)
  
  ;; Get maintenance station info
  (let [maintenance (get-maintenance)
        info (.-info maintenance)
        constructor (.constructor maintenance)
        pgroups (.pgroups constructor)]
    {:name (.toString (.-name info))
     :desc (.toString (.desc info))
     :key (.key maintenance)
     :uses-area (.usesArea constructor)
     :must-be-indoors (.mustBeIndoors constructor)
     :num-furniture-groups (.size pgroups)})
  
  :rcf)

;; ============================================================================
;; Maintenance station management functions
;; ============================================================================

;; Get all maintenance station instances
(defn all-maintenance-stations []
  (let [maintenance-room (get-maintenance)]
    (array-list-resize->vec (.all maintenance-room))))

(comment
  (all-maintenance-stations)
  :rcf)

;; Get maintenance station at a specific tile position
;; Returns the maintenance station instance if one exists at (x, y), nil otherwise
(defn maintenance-station-at [x y]
  (let [maintenance-room (get-maintenance)
        getter (.-getter maintenance-room)]
    (.get getter x y)))

(comment
  (maintenance-station-at 281 438)
  :rcf)

;; Get maintenance station position (center coordinates)
;; Returns {:x x :y y} or nil if position cannot be determined
(defn maintenance-station-position [maintenance-instance]
  (try
    ;; Try to get center coordinates using mX() and mY() methods
    (let [mx (utils/invoke-method maintenance-instance "mX")
          my (utils/invoke-method maintenance-instance "mY")]
      (when (and mx my)
        {:x mx :y my}))
    (catch Exception _e
      nil)))

(comment
  (let [stations (all-maintenance-stations)
        first-station (first stations)]
    (maintenance-station-position first-station))
  :rcf)

;; Check if auto-employ is enabled for a maintenance station
(defn is-auto-employ-enabled? [maintenance-instance]
  (let [maintenance-room (get-maintenance)]
    (when (instance? ROOM_EMPLOY_AUTO maintenance-room)
      (.autoEmploy maintenance-room maintenance-instance))))

(comment
  (let [stations (all-maintenance-stations)
        first-station (first stations)]
    (is-auto-employ-enabled? first-station))
  :rcf)

;; Enable or disable auto-employ for a maintenance station
;; enabled: true to enable auto-employ, false to disable
(defn set-auto-employ [maintenance-instance enabled]
  (let [maintenance-room (get-maintenance)]
    (when (instance? ROOM_EMPLOY_AUTO maintenance-room)
      (.autoEmploy maintenance-room maintenance-instance enabled))))

;; Enable or disable auto-employ using update-once (ensures it happens in a single frame)
(defn set-auto-employ-once [maintenance-instance enabled]
  (utils/update-once
   (fn [_ds]
     (set-auto-employ maintenance-instance enabled))))

(comment
  ;; Example usage:
  
  ;; Get all maintenance stations
  (def stations (all-maintenance-stations))
  
  ;; Enable auto-employ for the first maintenance station
  (let [first-station (first stations)]
    (set-auto-employ-once first-station true))
  
  ;; Disable auto-employ for a specific station
  (let [station (maintenance-station-at 281 438)]
    (when station
      (set-auto-employ-once station false)))
  
  ;; Enable auto-employ for all maintenance stations
  (doseq [station (all-maintenance-stations)]
    (set-auto-employ-once station true))
  
  ;; Check auto-employ status for all stations
  (map (fn [station]
         {:position (maintenance-station-position station)
          :auto-employ (is-auto-employ-enabled? station)})
       (all-maintenance-stations))
  
  :rcf)

