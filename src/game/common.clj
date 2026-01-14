(ns game.common
  (:import
   [view.main VIEW]
   [snake2d.util.sets ArrayListResize LIST]
   [settlement.main SETT]
   [init.structure STRUCTURES]
   [init.resources RESOURCES]))

(defn array-list->vec [^LIST array-list]
  (let [size (.size array-list)
        indices (range size)]
    (map (fn [idx]
           (.get array-list idx))
         indices)))

(defn array-list-resize->vec [^ArrayListResize array-list]
  (let [size (.size array-list)
        indices (range size)]
    (map (fn [idx]
           (.get array-list idx))
         indices)))

(defn focus [{:keys [cX cY]}]
  (let [sett-view (VIEW/s)
        game-window (.getWindow sett-view)]
    (.centerAt game-window cX cY)))

(defn focus-entity [entity]
  (let [body (.body entity)]
    (focus {:cX (.cX body) :cY (.cY body)})))

;; Get building materials (e.g., wood, stone)
;; Returns the TBuilding for the material (ConstructionInit needs TBuilding, not Structure)
;; material-name should be a resource name like "WOOD", "STONE", etc.
;; This finds the structure by matching the resource type, then converts to TBuilding
(defn get-building-material [material-name]
  (let [material-upper (.toUpperCase material-name)
        resource (case material-upper
                   "WOOD" (RESOURCES/WOOD)
                   "STONE" (RESOURCES/STONE)
                   (throw (Exception. (str "Unknown material: " material-name ". Supported: WOOD, STONE"))))
        all-structures (STRUCTURES/all)
        structure (first (filter #(= resource (.-resource %)) all-structures))]
    (if structure
      ;; Convert Structure to TBuilding using BUILDINGS.get(Structure)
      (let [buildings (.-BUILDINGS (SETT/TERRAIN))]
        (.get buildings structure))
      (throw (Exception. (str "Could not find structure for material: " material-name))))))

(comment
  
(require '[game.humanoid :as humanoid])

  (-> (humanoid/all-entities)
      (nth 0)
      (focus-entity))
  :rcf)
