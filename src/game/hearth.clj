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

(comment
  (get-hearth)
  :rcf)

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
;; The game will automatically select the appropriate furniture item based on area size
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

;; Create a hearth using update-once (ensures it happens in a single frame)
(defn create-hearth-once [center-x center-y width height & {:keys [material-name upgrade] 
                                                              :or {material-name "WOOD" upgrade 0}}]
  (utils/update-once 
   (fn [_ds]
     (create-hearth center-x center-y width height 
                   :material-name material-name 
                   :upgrade upgrade))))

(comment
  ;; Example usage:
  
  ;; Create a 5x3 hearth at center (271, 430) using wood
  ;; The game will automatically select the appropriate furniture item based on area size
  (create-hearth-once 271 430 5 3)
  
  ;; Create a larger hearth (7x7)
  (create-hearth-once 300 400 7 7)
  
  ;; Create a hearth using stone
  (create-hearth-once 250 250 5 3 :material-name "STONE")
  
  ;; Get hearth info
  (let [hearth (get-hearth)
        info (.-info hearth)
        constructor (.constructor hearth)
        pgroups (.pgroups constructor)]
    {:name (.toString (.-name info))
     :desc (.toString (.desc info))
     :key (.key hearth)
     :uses-area (.usesArea constructor)
     :must-be-indoors (.mustBeIndoors constructor)
     :num-furniture-groups (.size pgroups)})
  
  :rcf)

