(ns game.well
  (:require 
   [repl.utils :as utils]
   [game.common :refer [get-building-material]])
  (:import 
   [settlement.main SETT]
   [settlement.room.main.construction ConstructionInit]))

;; Get the first well room blueprint
;; WELLS is a LIST<ROOM_WELL>, typically contains one well type
(defn get-well []
  (let [rooms (SETT/ROOMS)
        wells (.-WELLS rooms)]
    (when (> (.size wells) 0)
      (.get wells 0))))

(comment
  (get-well)
  :rcf)

;; Select furniture size - well is fixed at 3x3
;; NOTE: The game's Constructor.java defines 3 furniture sizes (3x3, 5x5, 6x6),
;; but wells should only be 3x3. The game appears to have a bug where it allows
;; 5x5 and 6x6 wells, but this is incorrect behavior. We enforce 3x3 only.
(defn select-well-furniture-size []
  0)  ; Always use size 0 (3x3 furniture)

;; Create a well at specified location
;; center-x, center-y: center tile coordinates
;; material-name: building material name (must be "STONE", default "STONE")
;; upgrade: upgrade level (default 0)
;;
;; NOTE: Well is fixed at 3x3 and must use STONE material.
;; The game's Constructor.java has a bug where it allows 5x5 and 6x6 wells,
;; but this is incorrect - wells should only be 3x3. We enforce this restriction.
(defn create-well [center-x center-y & {:keys [material-name upgrade] 
                                         :or {material-name "STONE" upgrade 0}}]
  ;; Validate material - must be STONE
  (when-not (= (.toUpperCase material-name) "STONE")
    (throw (Exception. (str "Well must use STONE material, got " material-name))))
  
  (let [width 3  ; Fixed size
        height 3  ; Fixed size
        rooms (SETT/ROOMS)
        well-blueprint (get-well)
        _ (when (nil? well-blueprint)
            (throw (Exception. "Could not find WELL room. Make sure the game has loaded.")))
        well-constructor (.constructor well-blueprint)
        tbuilding (get-building-material material-name)
        construction-init (ConstructionInit. upgrade well-constructor tbuilding 0 nil)
        tmp (.tmpArea rooms "well")
        
        ;; Get furniture group and select appropriate size
        furnisher-groups (.pgroups well-constructor)
        first-group (when (> (.size furnisher-groups) 0)
                      (.get furnisher-groups 0))
        furniture-size (select-well-furniture-size)
        furnisher-item (when first-group
                         (.item first-group furniture-size 0))  ; rot=0
        _ (when (nil? furnisher-item)
            (throw (Exception. "Could not get furniture item for well")))
        
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
     :room-type "WELL"
     :furniture-size furniture-size
     :furniture-width (.width furnisher-item)
     :furniture-height (.height furnisher-item)}))

;; Create a well using update-once (ensures it happens in a single frame)
;; Well is fixed at 3x3 and must use STONE material
(defn create-well-once [center-x center-y & {:keys [material-name upgrade] 
                                              :or {material-name "STONE" upgrade 0}}]
  (utils/update-once 
   (fn [_ds]
     (create-well center-x center-y 
                  :material-name material-name 
                  :upgrade upgrade))))

(comment
  ;; Example usage:
  
  ;; Create a well at center (277, 433) using stone (required)
  ;; Well is fixed at 3x3 and must use STONE material
  (create-well-once 277 433)
  
  
  ;; NOTE: The game's Constructor.java has a bug where it allows 5x5 and 6x6 wells,
  ;; but this is incorrect behavior. Our code enforces 3x3 only and doesn't
  ;; accept width/height parameters.
  
  ;; Get well info
  (let [well (get-well)
        info (.-info well)
        constructor (.constructor well)
        pgroups (.pgroups constructor)]
    {:name (.toString (.-name info))
     :desc (.toString (.desc info))
     :key (.key well)
     :uses-area (.usesArea constructor)
     :must-be-indoors (.mustBeIndoors constructor)
     :num-furniture-groups (.size pgroups)})
  
  :rcf)

