(ns game.animal
  (:require
   [repl.utils :as utils])
  (:import
   [settlement.main SETT]
   [settlement.entity.animal Animal]))

;; Get all animals in the settlement (both wild and domesticated)
(defn all-animals []
  (let [entities-instance (SETT/ENTITIES)
        entities (.getAllEnts entities-instance)]
    (filter #(instance? Animal %) entities)))

(comment
  (-> (all-animals)
      count)
  :rcf)

;; Get all wild (non-domesticated) animals in the settlement
(defn wild-animals []
  (filter #(not (.domesticated %)) (all-animals)))

(comment 
  (-> (wild-animals)
      count)
  :rcf)

;; Get all domesticated animals in the settlement
(defn domesticated-animals []
  (filter #(.domesticated %) (all-animals)))

(comment
  (-> (domesticated-animals)
      count)
  :rcf)

;; Get all animals at a specific tile coordinate
(defn animals-at-tile [tx ty]
  (first (filter #(instance? Animal %)
                 (.getAtTile (SETT/ENTITIES) tx ty))))

(comment
  (animals-at-tile 50 50)
  (animals-at-tile 218 252)
  (animals-at-tile 209 251)
  :rcf)

;; Get all animals at a specific pixel coordinate
(defn animals-at-point [x y]
  (filter #(instance? Animal %) 
          (.getAtPointL (SETT/ENTITIES) x y)))

;; Get all animals within radius tiles of an entity
(defn animals-near [entity radius]
  (filter #(instance? Animal %) 
          (.getInProximity (SETT/ENTITIES) entity radius)))

;; Get information about an animal
(defn animal-info [animal]
  (when (instance? Animal animal)
    (let [body (.body animal)
          physics (.physics animal)
          species (.species animal)]
      {:species-name (.toString (.name species))
       :species-key (.key species)
       :domesticated (.domesticated animal)
       :cub (.cub animal)
       :hunt-marked (.huntMarkedIs animal)
       :hunt-can-mark (.huntMarkedCan animal)
       :position {:x (.cX body)
                  :y (.cY body)}
       :tile {:x (.x (.tileC physics))
              :y (.y (.tileC physics))}
       :mass (.getMass physics)})))

(comment
  (-> (animals-at-tile 218 252)
      (animal-info))
  :rcf)

;; Get species information for an animal
(defn species-info [animal]
  (when (instance? Animal animal)
    (let [species (.species animal)]
      {:name (.toString (.name species))
       :key (.key species)
       :resources (.resources species)
       :icon (.icon species)
       :desc (.toString (.desc species))})))

(comment
  (-> (animals-at-tile 218 252)
      (species-info))
  :rcf)

;; Get the resources that will be obtained from hunting this animal
(defn animal-resources [animal]
  (when (instance? Animal animal)
    (let [species (.species animal)
          resources (.resources species)
          mass (.getMass (.physics animal))]
      (map-indexed 
       (fn [i resource]
         {:resource resource
          :amount (.resAmount species i mass)
          :resource-name (.toString (.name resource))})
       resources))))

(comment
  (-> (animals-at-tile 218 252)
      (animal-resources))
  :rcf)

;; Get all animals within radius tiles of the given tile coordinate.
;; Returns a list of Animal objects.
(defn animals-in-area
  "Get all animals within radius tiles of the given tile coordinate.
   Returns a list of Animal objects (both wild and domesticated)."
  [tx ty radius]
  (let [all-animals (all-animals)]
    (filter
     (fn [animal]
       (when (instance? Animal animal)
         (let [atx (.x (.tileC (.physics animal)))
               aty (.y (.tileC (.physics animal)))
               ;; Calculate Euclidean distance in tile space
               dx (- atx tx)
               dy (- aty ty)
               tile-distance (Math/sqrt (+ (* dx dx) (* dy dy)))]
           (<= tile-distance radius))))
     all-animals)))

(comment
  ;; Get all animals in area
  (animals-in-area 336 190 10)
  ;; Get only wild animals in area
  (filter #(not (.domesticated %)) (animals-in-area 336 190 10))
  :rcf)

(defn hunt-animal
  "Mark a single animal for hunting. Returns true if successful."
  [animal]
  (when (and (instance? Animal animal)
             (.huntMarkedCan animal))
    (utils/update-once
     (fn [_ds]
       (.huntMark animal true)))
    true))

(comment
  (-> (animals-at-tile 218 252)
      (hunt-animal))
  :rcf)

;; Unmark an animal from hunting. Returns true if successful.
(defn unmark-hunt-animal 
  "Unmark an animal from hunting. Returns true if successful."
  [animal]
  (when (and (instance? Animal animal)
             (.huntMarkedIs animal))
    (utils/update-once
     (fn [_ds]
       (.huntMark animal false)))
    true))

(comment
  (-> (animals-at-tile 218 252)
      (unmark-hunt-animal))
  :rcf)

;; Find the nearest wild animal to the given tile coordinates.
;; Returns the animal and its distance in tiles.
(defn find-nearest-wild-animal [tx ty]
  (let [wild-animals (wild-animals)
        with-distance (map
                       (fn [animal]
                         (let [atx (.x (.tileC (.physics animal)))
                               aty (.y (.tileC (.physics animal)))
                               distance (+ (Math/abs (- atx tx))
                                          (Math/abs (- aty ty)))]
                           {:animal animal
                            :distance distance
                            :tile {:x atx :y aty}}))
                       wild-animals)
        sorted (sort-by :distance with-distance)]
    (first sorted)))

;; Get all available animal species in the game
(defn all-animal-species []
  (let [animals-instance (SETT/ANIMALS)
        species (.-species animals-instance)]
    (map-indexed
     (fn [i s]
       {:index i
        :key (.key s)
        :name (.toString (.name s))
        :desc (.toString (.desc s))
        :icon (.icon s)})
     species)))

(comment
  ;; Examples:
  
  ;; Get all wild animals
  (def wild (wild-animals))
  (count wild)
  
  ;; Get info about first wild animal
  (def first-animal (first wild))
  (animal-info first-animal)
  (species-info first-animal)
  (animal-resources first-animal)
  
  ;; Mark animals near a location (example: mark all wild animals in area)
  (let [wild-in-area (->> (animals-in-area 50 50 10)
                          (filter #(not (.domesticated %)))
                          (filter #(.huntMarkedCan %)))]
    (doseq [animal wild-in-area]
      (hunt-animal animal))
    (count wild-in-area))
  
  ;; Find nearest animal
  (find-nearest-wild-animal 50 50)
  
  ;; Get all animal species
  (all-animal-species)
  
  :rcf)

