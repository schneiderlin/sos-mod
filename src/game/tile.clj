(ns game.tile
  "Tile-level data access for SOS settlement map.
   Provides functions to query entities, furniture, and terrain at specific tile coordinates."
  (:import [settlement.main SETT]))

(defn entities-at-tile
  "Get all entities at a specific tile coordinate.
   Returns a Clojure sequence of entities."
  [tx ty]
  (seq (.getAtTile (SETT/ENTITIES) tx ty)))

(defn entities-in-area
  "Get all entities within a rectangular area.
   Area is defined by start-x, start-y (top-left corner) and width, height in tiles."
  [start-x start-y width height]
  (for [x (range start-x (+ start-x width))
        y (range start-y (+ start-y height))
        entity (entities-at-tile x y)
        :when entity]
    entity))

(defn count-entities-in-area
  "Count total entities in a rectangular area."
  [start-x start-y width height]
  (count (entities-in-area start-x start-y width height)))

(defn get-furniture-data
  "Get the furniture data instance from the settlement."
  []
  (let [rooms (SETT/ROOMS)]
    (.fData rooms)))

(defn furniture-item
  "Get furniture item at a specific tile coordinate."
  [tx ty]
  (let [fdata (get-furniture-data)]
    (.get (.item fdata) tx ty)))

(defn has-furniture-item?
  "Check if a tile has a furniture item."
  [tx ty]
  (some? (furniture-item tx ty)))

(defn furniture-items-in-area
  "Find all tiles with furniture items in an area.
   Returns a list of tile coordinates and their items."
  [start-x start-y width height]
  (for [x (range start-x (+ start-x width))
        y (range start-y (+ start-y height))
        :when (has-furniture-item? x y)]
    {:tile [x y]
     :item (furniture-item x y)}))

(defn get-ground
  "Get ground tile data at a specific coordinate.
   The GROUND.MAP holds terrain data including resources on ground."
  [tx ty]
  (let [ground-map (.. SETT GROUND MAP)]
    (.get ground-map tx ty)))

(defn minerals-at-tile
  "Get mineral data at a specific tile."
  [tx ty]
  (try
    (let [minerals (SETT/MINERALS)]
      {:tile [tx ty]
       :get-result (try (.get minerals tx ty) (catch Exception e {:error (.getMessage e)}))})
    (catch Exception e
      {:error (.getMessage e)})))

(defn get-construction-blueprint
  "Get the construction blueprint from the settlement.
   Returns the ConstructionBlueprint which has get(tx, ty) method.
   Uses reflection because the 'construction' field is private."
  []
  (let [constructions (.construction (SETT/ROOMS))
        field (.getDeclaredField (class constructions) "construction")]
    (.setAccessible field true)
    (.get field constructions)))

(comment
  (get-construction-blueprint)
  :rcf)

(defn construction-at-tile
  "Get construction instance at a specific tile coordinate.
   Returns nil if no construction exists at that tile."
  [tx ty]
  (let [blueprint (get-construction-blueprint)]
    (.get blueprint tx ty)))

(defn has-construction?
  "Check if a tile has a construction site."
  [tx ty]
  (some? (construction-at-tile tx ty)))

(defn constructions-in-area
  "Find all tiles with construction sites in an area.
   Returns a list of tile coordinates and their construction instances."
  [start-x start-y width height]
  (for [x (range start-x (+ start-x width))
        y (range start-y (+ start-y height))
        :when (has-construction? x y)]
    {:tile [x y]
     :construction (construction-at-tile x y)}))
