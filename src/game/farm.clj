(ns game.farm
  (:require 
   [repl.utils :as utils]
   [game.common :refer [get-building-material array-list->vec array-list-resize->vec]])
  (:import 
   [settlement.main SETT]
   [settlement.room.main.construction ConstructionInit]))

;; Get all farm types
(defn all-farm-types []
  (let [rooms (SETT/ROOMS)
        farms (.-FARMS rooms)]
    (array-list->vec farms)))

(comment 
  (all-farm-types)
  :rcf)

;; Get farm info
(defn all-farm-info []
  (let [farms (all-farm-types)]
    (map (fn [farm]
           {:key (.key farm)
            :name (.toString (.. farm info name))
            :desc (.toString (.. farm info desc))
            :crop (.toString (.. farm crop resource name))})
         farms)))

(comment
  (all-farm-info)
  :rcf)

;; Get farm by key (e.g., "FARM_VEG", "FARM_COTTON")
(defn get-farm-by-key [key]
  (let [farms (all-farm-types)]
    (first (filter #(= (.key %) key) farms))))

(comment
  (get-farm-by-key "FARM_VEG")
  (get-farm-by-key "FARM_COTTON")
  :rcf)

;; Get vegetable farm (FARM_VEG) - the most common farm type
(defn get-vegetable-farm []
  (get-farm-by-key "FARM_VEG"))

(comment
  (get-vegetable-farm)
  :rcf)

;; Get cotton farm (FARM_COTTON)
(defn get-cotton-farm []
  (get-farm-by-key "FARM_COTTON"))

(comment
  (get-cotton-farm)
  :rcf)

;; Get the first farm type (for backward compatibility)
;; NOTE: This returns an arbitrary farm type - use get-vegetable-farm or get-farm-by-key for specific types
(defn get-farm []
  (let [farms (all-farm-types)]
    (first farms)))

(comment
  (get-farm)
  :rcf)

;; Get farm constructor
(defn get-farm-constructor [farm]
  (.constructor farm))

(comment
  (let [farm (get-farm)]
    (get-farm-constructor farm))
  :rcf)

;; ============================================================================
;; Fertility and Water Checking Functions
;; ============================================================================

;; Get soil fertility at a specific tile
;; Returns a double value (higher is better, typically 0.0 to 1.0+)
(defn get-fertility [tx ty]
  (let [ground-map (.. SETT GROUND MAP)
        ground-tile (.get ground-map tx ty)]
    (.-farm ground-tile)))

(comment
  (get-fertility 200 200)
  :rcf)

;; Get average fertility for an area
;; start-x, start-y: top-left corner
;; width, height: dimensions
(defn get-average-fertility [start-x start-y width height]
  (let [fertilities (atom [])]
    (doseq [y (range height)
            x (range width)]
      (let [tx (+ start-x x)
            ty (+ start-y y)
            fertility (get-fertility tx ty)]
        (swap! fertilities conj fertility)))
    (if (empty? @fertilities)
      0.0
      (/ (reduce + @fertilities) (count @fertilities)))))

(comment
  (get-average-fertility 200 200 10 10)
  :rcf)

;; Check if a tile has water access (for irrigation)
;; Returns true if the tile has water nearby
(defn has-water-access? [tx ty]
  (let [moisture-current (.. SETT GROUND MOISTURE_CURRENT)
        moisture (.get moisture-current tx ty)]
    (> moisture 0.0)))

(comment
  (has-water-access? 200 200)
  :rcf)

;; Check if an area has water access
;; Returns the percentage of tiles with water access
(defn get-water-access-percentage [start-x start-y width height]
  (let [total-tiles (* width height)
        water-tiles (atom 0)]
    (doseq [y (range height)
            x (range width)]
      (let [tx (+ start-x x)
            ty (+ start-y y)]
        (when (has-water-access? tx ty)
          (swap! water-tiles inc))))
    (if (zero? total-tiles)
      0.0
      (/ @water-tiles total-tiles))))

(comment
  (get-water-access-percentage 200 200 10 10)
  :rcf)

;; Get farm location quality info
;; Returns a map with fertility and water access information
(defn get-farm-location-quality [center-x center-y width height]
  (let [start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))
        avg-fertility (get-average-fertility start-x start-y width height)
        water-percentage (get-water-access-percentage start-x start-y width height)]
    {:center-x center-x
     :center-y center-y
     :width width
     :height height
     :start-x start-x
     :start-y start-y
     :average-fertility avg-fertility
     :water-access-percentage water-percentage
     :has-good-fertility (>= avg-fertility 0.5)
     :has-water-access (>= water-percentage 0.1)}))

(comment
  (get-farm-location-quality 200 200 10 10)
  :rcf)

;; ============================================================================
;; Farm Creation Functions
;; ============================================================================

;; Create a farm at specified location
;; center-x, center-y: center tile coordinates
;; width, height: dimensions of the farm (in tiles)
;; material-name: building material name (e.g., "WOOD", "STONE") - not used for farms, but kept for consistency
;; upgrade: upgrade level (default 0)
;; farm-type: optional farm type (if nil, defaults to vegetable farm FARM_VEG)
(defn create-farm [center-x center-y width height & {:keys [material-name upgrade farm-type]
                                                      :or {material-name "WOOD" upgrade 0}}]
  (let [rooms (SETT/ROOMS)
        ;; Get farm type (use provided or default to vegetable farm)
        farm (or farm-type (get-vegetable-farm))
        _ (when (nil? farm)
            (throw (Exception. "No farm types available - FARM_VEG not found")))
        farm-constructor (get-farm-constructor farm)
        ;; Note: Farms typically don't use building materials like warehouses,
        ;; but we'll use WOOD as default for consistency with other room types
        tbuilding (get-building-material material-name)
        construction-init (ConstructionInit. upgrade farm-constructor tbuilding 0 nil)
        tmp (.tmpArea rooms "farm")
        
        ;; Calculate start coordinates
        start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))]
    
    ;; Set the building area
    (doseq [y (range height)
            x (range width)]
      (.set tmp (+ start-x x) (+ start-y y)))
    
    ;; Create the construction site
    (.createClean (.construction rooms) tmp construction-init)
    
    ;; Clear temporary area
    (.clear tmp)
    
    {:success true
     :center-x center-x
     :center-y center-y
     :width width
     :height height
     :room-type "FARM"
     :farm-key (.key farm)
     :crop-name (.toString (.. farm crop resource name))
     :location-quality (get-farm-location-quality center-x center-y width height)}))

;; Create a farm using update-once (ensures it happens in a single frame)
(defn create-farm-once [center-x center-y width height & {:keys [material-name upgrade farm-type] 
                                                           :or {material-name "WOOD" upgrade 0}}]
  (utils/update-once 
   (fn [_ds]
     (create-farm center-x center-y width height 
                 :material-name material-name 
                 :upgrade upgrade
                 :farm-type farm-type))))

(comment
  ;; Example usage:

  ;; Check location quality before building
  (get-farm-location-quality 200 200 10 10)

  ;; Get specific farm types
  (get-vegetable-farm)
  (get-cotton-farm)
  (get-farm-by-key "FARM_GRAIN")

  ;; Create a vegetable farm (default behavior)
  (create-farm-once 200 200 10 10)

  ;; Create a cotton farm explicitly
  (create-farm-once 250 250 15 15 :farm-type (get-cotton-farm))

  ;; Create a grain farm using key
  (create-farm-once 300 300 12 12 :farm-type (get-farm-by-key "FARM_GRAIN"))

  ;; Get all available farm types
  (all-farm-info)

  ;; Get fertility at a specific tile
  (get-fertility 200 200)

  ;; Get average fertility for an area
  (get-average-fertility 200 200 10 10)

  ;; Check water access
  (has-water-access? 200 200)
  (get-water-access-percentage 200 200 10 10)

  :rcf)

;; ============================================================================
;; Farm Management Functions
;; ============================================================================

;; Get all farm instances
(defn all-farms []
  (let [farms (all-farm-types)
        all-instances (atom [])]
    (doseq [farm farms]
      (let [instances (array-list-resize->vec (.all farm))]
        (doseq [instance instances]
          (swap! all-instances conj instance))))
    @all-instances))

(comment
  (all-farms)
  :rcf)

;; Get farm at a specific tile position
;; Returns the farm instance if one exists at (x, y), nil otherwise
(defn farm-at [x y]
  (let [farms (all-farm-types)]
    (some (fn [farm]
            (let [getter (.-getter farm)]
              (.get getter x y)))
          farms)))

(comment
  (farm-at 200 200)
  :rcf)

