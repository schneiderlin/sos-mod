# Finding and Hunting Wild Animals

This document explains how to find wild animals in the settlement and how to hunt them programmatically.

## Overview

Animals in Songs of Syx are managed through the `SETT.ANIMALS()` system. Wild animals can be found roaming the settlement, and they can be marked for hunting using the game's hunting job system.

## Key Classes

- `settlement.entity.animal.Animal` - Individual animal entity
- `settlement.entity.animal.Animals` - Animal management system
- `settlement.entity.animal.AnimalSpecies` - Animal species definitions
- `settlement.job.JobClears` - Contains hunting job (`hunt`)

## Finding Animals

### Getting All Animals

Animals are entities in the game, so you can find them by filtering all entities:

```clojure
(ns repl.animals
  (:import
   [settlement.main SETT]
   [settlement.entity.animal Animal]))

;; Get all entities and filter for animals
(defn all-animals []
  (let [entities-instance (SETT/ENTITIES)
        entities (.getAllEnts entities-instance)]
    (filter #(instance? Animal %) entities)))

;; Get only wild (non-domesticated) animals
(defn wild-animals []
  (filter #(not (.domesticated %)) (all-animals)))
```

### Getting Animals at a Specific Location

You can get animals at a specific tile or point:

```clojure
;; Get animals at a specific tile
(defn animals-at-tile [tx ty]
  (filter #(instance? Animal %) 
          (.getAtTile (SETT/ENTITIES) tx ty)))

;; Get animals at a specific point (pixel coordinates)
(defn animals-at-point [x y]
  (filter #(instance? Animal %) 
          (.getAtPointL (SETT/ENTITIES) x y)))
```

### Getting Animals in Proximity

```clojure
;; Get animals within proximity of an entity (5 tiles by default)
(defn animals-near [entity]
  (filter #(instance? Animal %) 
          (.getInProximity (SETT/ENTITIES) entity 5)))
```

## Animal Information

### Animal Properties

Each `Animal` instance has the following key methods:

```clojure
(defn animal-info [animal]
  {:species (.species animal)           ; AnimalSpecies object
   :domesticated (.domesticated animal)  ; boolean - true if in pasture
   :cub (.cub animal)                   ; boolean - true if baby
   :hunt-marked (.huntMarkedIs animal)   ; boolean - marked for hunting
   :hunt-can-mark (.huntMarkedCan animal) ; boolean - can be marked
   :position {:x (.cX (.body animal))   ; pixel X coordinate
              :y (.cY (.body animal))}   ; pixel Y coordinate
   :tile {:x (.x (.tileC (.physics animal))) ; tile X
          :y (.y (.tileC (.physics animal)))} ; tile Y
   :mass (.getMass (.physics animal))}) ; animal mass
```

### Animal Species Information

```clojure
(defn species-info [animal]
  (let [species (.species animal)]
    {:name (.toString (.name species))      ; Species name
     :key (.key species)                   ; Species key (e.g., "DEER")
     :resources (.resources species)       ; Resources this animal provides
     :icon (.icon species)                 ; Species icon
     :desc (.toString (.desc species))}))  ; Description
```

### Getting Resources from Animal

Animals provide resources when hunted. You can check what resources an animal will yield:

```clojure
(defn animal-resources [animal]
  (let [species (.species animal)
        resources (.resources species)
        mass (.getMass (.physics animal))]
    (map-indexed 
     (fn [i resource]
       {:resource resource
        :amount (.resAmount species i mass)})
     resources)))
```

## Hunting Animals

### Marking Animals for Hunting

To hunt an animal, you must first mark it for hunting:

```clojure
;; Mark a single animal for hunting
(defn mark-animal-for-hunt [animal]
  (when (.huntMarkedCan animal)
    (.huntMark animal true)))

;; Mark multiple animals for hunting
(defn mark-animals-for-hunt [animals]
  (doseq [animal animals]
    (mark-animal-for-hunt animal)))

;; Unmark an animal from hunting
(defn unmark-animal-from-hunt [animal]
  (when (.huntMarkedIs animal)
    (.huntMark animal false)))
```

### Using the Hunting Job

The game has a built-in hunting job that citizens can perform. You can access it via:

```clojure
(def hunting-job (-> (SETT/JOBS)
                     (.clearss)
                     (.hunt)))
```

However, marking animals is usually sufficient - the game's AI will automatically assign hunters to marked animals.

### Complete Hunting Function

Here's a complete function to find and mark wild animals for hunting:

```clojure
(ns repl.animals
  (:import
   [settlement.main SETT]
   [settlement.entity.animal Animal]
   [repl.utils :as utils]))

(defn find-wild-animals []
  "Find all wild (non-domesticated) animals in the settlement"
  (filter #(and (instance? Animal %)
                (not (.domesticated %)))
          (.getAllEnts (SETT/ENTITIES))))

(defn mark-wild-animals-for-hunt []
  "Mark all wild animals for hunting"
  (let [wild-animals (find-wild-animals)]
    (doseq [animal wild-animals]
      (when (.huntMarkedCan animal)
        (.huntMark animal true)))
    (count wild-animals)))

(defn hunt-animals-in-area [tx ty radius]
  "Mark all wild animals within radius tiles of the given tile"
  (utils/update-once
   (fn [_ds]
     (let [entities-instance (SETT/ENTITIES)
           center-x (* tx 32)  ; C.TILE_SIZE = 32
           center-y (* ty 32)]
       (doseq [entity (.getAllEnts entities-instance)]
         (when (instance? Animal entity)
           (let [animal entity
                 ax (.cX (.body animal))
                 ay (.cY (.body animal))
                 distance (Math/sqrt (+ (Math/pow (- ax center-x) 2)
                                       (Math/pow (- ay center-y) 2)))
                 tile-distance (/ distance 32)]
             (when (and (not (.domesticated animal))
                        (<= tile-distance radius)
                        (.huntMarkedCan animal))
               (.huntMark animal true)))))))))
```

## Animal States

Animals have different states related to hunting:

- **`huntMarkedCan()`** - Returns `true` if the animal can be marked for hunting (not domesticated, not already marked)
- **`huntMarkedIs()`** - Returns `true` if the animal is currently marked for hunting
- **`huntReservable()`** - Returns `true` if the animal is marked and available for a hunter to reserve
- **`huntReserved()`** - Returns `true` if the animal is marked and reserved by a hunter

## Example Usage

```clojure
;; Find all wild animals
(def wild-animals (find-wild-animals))
(count wild-animals)  ; How many wild animals are there?

;; Get info about the first wild animal
(def first-animal (first wild-animals))
(animal-info first-animal)
(species-info first-animal)
(animal-resources first-animal)

;; Mark all wild animals for hunting
(mark-wild-animals-for-hunt)

;; Mark animals near a specific location (e.g., tile 50, 50, within 10 tiles)
(hunt-animals-in-area 50 50 10)

;; Find animals at a specific tile
(def animals-here (animals-at-tile 50 50))
```

## Source Code References

- **`sos-src/settlement/entity/animal/Animal.java`** - Animal entity class
  - Line 476: `domesticated()` method
  - Line 556: `huntMarkedIs()` method
  - Line 560: `huntMarkedCan()` method
  - Line 564: `huntMark(boolean)` method
  - Line 323: `species()` method

- **`sos-src/settlement/entity/animal/Animals.java`** - Animal management
  - Line 42: `spawn` - Animal spawning system
  - Line 43: `map` - Animal species map
  - Line 44: `species` - List of all animal species

- **`sos-src/settlement/job/JobClears.java`** - Hunting job
  - Line 892: `hunt` - PlacableMulti for marking animals
  - Line 855: `huntundo` - PlacableMulti for unmarking animals

## Important Notes

1. **Domesticated vs Wild**: Only non-domesticated animals can be hunted. Domesticated animals are those in pastures.

2. **Hunting Process**: 
   - Mark animals using `huntMark(true)`
   - Citizens with hunting job will automatically hunt marked animals
   - Animals are killed and provide resources when successfully hunted

3. **Animal Spawning**: Wild animals spawn at `AnimalSpawnSpot` locations managed by `SETT.ANIMALS().spawn`

4. **Animal Lifecycle**: Wild animals have a lifespan (about 5 years) and will die of old age if not hunted

5. **Safety**: Hunting can be dangerous - hunters might get injured by aggressive animals

6. **Use `update-once`**: When marking multiple animals or performing batch operations, wrap in `update-once` to ensure single-frame execution

## Integration with Existing Code

You can integrate animal hunting with the existing camera and building functions:

```clojure
;; Move camera to first wild animal and mark it for hunting
(defn find-and-hunt-nearest-animal [start-tx start-ty]
  (let [wild-animals (find-wild-animals)
        nearest (first (sort-by 
                        (fn [animal]
                          (let [tx (.x (.tileC (.physics animal)))
                                ty (.y (.tileC (.physics animal)))]
                            (+ (Math/abs (- tx start-tx))
                               (Math/abs (- ty start-ty)))))
                        wild-animals))]
    (when nearest
      (let [tx (.x (.tileC (.physics nearest)))
            ty (.y (.tileC (.physics nearest)))]
        (move-camera-to-tile tx ty)
        (utils/update-once
         (fn [_ds]
           (.huntMark nearest true)))))))
```

