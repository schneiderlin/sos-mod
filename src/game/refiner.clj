(ns game.refiner
  (:require 
   [repl.utils :as utils]
   [game.common :refer [get-building-material]])
  (:import 
   [settlement.main SETT]
   [settlement.room.main.construction ConstructionInit]))

;; Find refiner by key (e.g., "REFINER_SMELTER", "REFINER_BAKERY", "REFINER_BREWERY")
(defn find-refiner-by-key [key-name]
  (let [refiners (.-REFINERS (SETT/ROOMS))
        key-upper (.toUpperCase key-name)]
    (first (filter #(= key-upper (.key %)) refiners))))

;; Get the smelter specifically
(defn get-smelter []
  (find-refiner-by-key "REFINER_SMELTER"))

(comment
  (get-smelter)
  :rcf)

;; Alternative: Use the collection lookup
(defn get-smelter-via-collection []
  (let [rooms (SETT/ROOMS)
        collection (.-collection rooms)]
    (.tryGet collection "REFINER_SMELTER")))

(comment
  (get-smelter-via-collection)
  :rcf)

;; Get all refiner types
(defn all-refiner-types []
  (let [refiners (.-REFINERS (SETT/ROOMS))]
    (map #(.key %) refiners)))

(comment
  (all-refiner-types)
  :rcf)

;; Get refiner info
(defn all-refiner-info []
  (let [refiners (.-REFINERS (SETT/ROOMS))]
    (map (fn [r]
           {:key (.key r)
            :name (.toString (.. r info name))
            :desc (.toString (.. r info desc))})
         refiners)))

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

;; Create a smelter at specified location
(defn create-smelter [center-x center-y width height & {:keys [material-name upgrade] 
                                                        :or {material-name "WOOD" upgrade 0}}]
  (create-refiner "SMELTER" center-x center-y width height 
                 :material-name material-name 
                 :upgrade upgrade))

;; Create using update-once (ensures it happens in a single frame)
(defn create-refiner-once [refiner-type center-x center-y width height & {:keys [material-name upgrade] 
                                                                           :or {material-name "WOOD" upgrade 0}}]
  (utils/update-once 
   (fn [_ds]
     (create-refiner refiner-type center-x center-y width height 
                    :material-name material-name 
                    :upgrade upgrade))))

;; Create a smelter using update-once
(defn create-smelter-once [center-x center-y width height & {:keys [material-name upgrade] 
                                                               :or {material-name "WOOD" upgrade 0}}]
  (create-refiner-once "SMELTER" center-x center-y width height 
                      :material-name material-name 
                      :upgrade upgrade))

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

(comment
  ;; Example usage:
  
  ;; List all available refiner types
  (all-refiner-types)
  (all-refiner-info)
  
  ;; Find the smelter
  (get-smelter)
  (get-smelter-via-collection)
  
  ;; Get furniture info
  (get-refiner-furniture-info "SMELTER")
  
  ;; Create a 5x5 smelter at tile (200, 200) using wood
  (create-smelter-once 200 200 5 5)
  
  ;; Create a 3x3 smelter using stone
  (create-smelter-once 250 250 3 3 :material-name "STONE")
  
  ;; Create a bakery (another refiner type)
  (create-refiner-once "BAKERY" 300 300 4 4)
  
  ;; Create a brewery
  (create-refiner-once "BREWERY" 350 350 4 4)
  
  ;; Create a coaler
  (create-refiner-once "COALER" 400 400 4 4)
  
  ;; Create a weaver
  (create-refiner-once "WEAVER" 450 450 4 4)
  
  :rcf)

