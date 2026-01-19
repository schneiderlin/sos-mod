(ns play.perception
  "Game state perception and overview functions for SOS settlement management.
   Provides quick summaries of population, resources, and settlement status.")

(require '[game.settlement :as settlement])
(require '[game.humanoid :as humanoid])
(require '[game.warehouse :as warehouse])
(require '[game.building :as building])
(require '[game.animal :as animal])
(require '[game.throne :as throne])
(import '[init.resources RESOURCES]
        '[settlement.main SETT]
        '[settlement.stats STATS])

(defn settlement-overview
  "Get a comprehensive overview of the settlement status.
   Returns a map with population, resources, buildings, animals, and people status."
  []
  (let [entities (settlement/all-entities)
        ;; humanoid-info returns nil for non-humanoids, so filter those out
        entity-infos (filter identity (map humanoid/humanoid-info entities))

        ;; Count people by status
        count-status #(count (filter true? (map % entity-infos)))]

    {:population {:total (count entity-infos)
                  :adults (count (filter :isAdult entity-infos))
                  :cold (count-status :isCold)
                  :in-danger (count-status :inDanger)
                  :critical (count-status :critical)
                  :will-die (count-status :willDie)}

     :resources {:wood (warehouse/total-amount-for-resource (RESOURCES/WOOD))
                 :stone (warehouse/total-amount-for-resource (RESOURCES/STONE))
                 :food {:bread (warehouse/total-amount-for-resource-key "BREAD")
                        :meat (warehouse/total-amount-for-resource-key "MEAT")
                        :vegetable (warehouse/total-amount-for-resource-key "VEGETABLE")
                        :fruit (warehouse/total-amount-for-resource-key "FRUIT")
                        :fish (warehouse/total-amount-for-resource-key "FISH")
                        :grain (warehouse/total-amount-for-resource-key "GRAIN")
                        :mushroom (warehouse/total-amount-for-resource-key "MUSHROOM")
                        :ration (warehouse/total-amount-for-resource-key "RATION")}}

     :materials {:coal (warehouse/total-amount-for-resource-key "COAL")
                 :ore (warehouse/total-amount-for-resource-key "ORE")
                 :metal (warehouse/total-amount-for-resource-key "METAL")
                 :leather (warehouse/total-amount-for-resource-key "LEATHER")
                 :cloth (warehouse/total-amount-for-resource-key "CLOTHES")}

     :drinks {:wine (warehouse/total-amount-for-resource-key "ALCO_WINE")
              :beer (warehouse/total-amount-for-resource-key "ALCO_BEER")}

     :buildings {:total (building/blueprint-imp-count)
                 :by-category (building/rooms-by-category)}

     :animals {:wild (count (animal/wild-animals))
               :domesticated (count (animal/domesticated-animals))}}))

(defn print-settlement-overview
  "Print a human-readable overview of the settlement status."
  []
  (let [overview (settlement-overview)]
    (println "=== SETTLEMENT OVERVIEW ===")
    (println)

    ;; Population
    (println "👥 Population:")
    (println "  Total:" (:total (:population overview)))
    (println "  Adults:" (:adults (:population overview)))
    (when (pos? (:cold (:population overview)))
      (println "  ⚠️  Cold:" (:cold (:population overview))))
    (when (pos? (:in-danger (:population overview)))
      (println "  🚨 In Danger:" (:in-danger (:population overview))))
    (when (pos? (:critical (:population overview)))
      (println "  💀 Critical:" (:critical (:population overview))))
    (println)

    ;; Food Resources
    (println "🍞 Food Resources:")
    (let [food (:food (:resources overview))
          total-food (reduce + 0 (vals food))]
      (if (zero? total-food)
        (println "  ⚠️  NO FOOD - SETTLEMENT WILL STARVE!")
        (doseq [[food-name amount] food]
          (when (pos? amount)
            (println "  " (name food-name) ":" amount))))
      (println "  Total Food:" (reduce + 0 (vals food))))
    (println)

    ;; Basic Resources
    (println "🪵 Basic Resources:")
    (println "  Wood:" (:wood (:resources overview)))
    (println "  Stone:" (:stone (:resources overview)))
    (println)

    ;; Materials
    (println "⚒️  Materials:")
    (println "  Coal:" (:coal (:materials overview)))
    (println "  Ore:" (:ore (:materials overview)))
    (println "  Metal:" (:metal (:materials overview)))
    (println "  Leather:" (:leather (:materials overview)))
    (println "  Clothes:" (:cloth (:materials overview)))
    (println)

    ;; Drinks
    (println "🍺 Drinks:")
    (println "  Wine:" (:wine (:drinks overview)))
    (println "  Beer:" (:beer (:drinks overview)))
    (println)

    ;; Buildings
    (println "🏠 Buildings:" (:total (:buildings overview)))
    (println)

    ;; Animals
    (println "🦌 Animals:")
    (println "  Wild:" (:wild (:animals overview)))
    (println "  Domesticated:" (:domesticated (:animals overview)))

    overview))

(comment
  (print-settlement-overview) 
  :rcf)

(defn quick-status
  "Get a quick one-line status summary of critical issues."
  []
  (let [overview (settlement-overview)
        pop (:total (:population overview))
        cold (:cold (:population overview))
        danger (:in-danger (:population overview))
        food-total (reduce + 0 (vals (:food (:resources overview))))]

    (str "Pop:" pop
         (when (pos? cold) (str " | ❄️ " cold " cold"))
         (when (pos? danger) (str " | 🚨 " danger " danger"))
         (when (zero? food-total) " | ⚠️ NO FOOD!")
         (when (pos? food-total) (str " | 🍞 " food-total " food")))))

(comment
  (quick-status)
  :rcf)

;; ============================================================================
;; Ground Items and Area Scanning
;; ============================================================================



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

(defn entities-near-throne
  "Get all entities within a radius of the throne."
  [radius]
  (let [{:keys [x y]} (throne/throne-position)]
    (entities-in-area (- x radius) (- y radius) (* 2 radius) (* 2 radius))))

(comment
  (entities-near-throne 10)
  :rcf)


(defn stockpiles-near-throne
  "Get all stockpiles/warehouses within radius of throne.
   Stockpiles are where items are stored before being moved to warehouses."
  [radius]
  (let [{:keys [x y]} (throne/throne-position)
        warehouses (warehouse/all-warehouses)]
    (filter #(warehouse/warehouse-in-area? % (- x radius) (- y radius) (* 2 radius) (* 2 radius))
            warehouses)))

(defn stockpile-contents-summary
  "Get a summary of all stockpile/warehouse contents.
   This shows items that are actually stored, not just available."
  []
  (let [warehouses (warehouse/all-warehouses)
        all-crates (warehouse/crates-by-material-warehouses-named warehouses)]
    all-crates))

(defn items-on-ground-in-area
  "Scan an area for items on the ground (not yet stored).
   Returns entity information for non-humanoid, non-animal entities."
  [start-x start-y width height]
  (let [entities (entities-in-area start-x start-y width height)
        ;; Filter out humanoids and animals to find items/resources
        items (filter #(not (or (instance? settlement.entity.humanoid.Humanoid %)
                               (instance? settlement.entity.animal.Animal %)))
                     entities)]
    {:count (count items)
     :entities (take 20 items)}))  ; Limit to first 20 for readability

(defn scan-area-around-throne
  "Scan the area around the throne for resources, items, and stockpiles.
   Returns a summary of what's found."
  [radius]
  (let [{:keys [x y]} (throne/throne-position)
        scan-x (- x radius)
        scan-y (- y radius)
        scan-size (* 2 radius)]
    {:throne-position {:x x :y y}
     :scan-area {:start-x scan-x :start-y scan-y :width scan-size :height scan-size}
     :entities-near-throne (count-entities-in-area scan-x scan-y scan-size scan-size)
     :nearby-stockpiles (count (stockpiles-near-throne radius))
     :items-on-ground (items-on-ground-in-area scan-x scan-y scan-size scan-size)}))

(comment
  ;; Get throne position
  (throne/throne-position)

  ;; Check entities at specific tile
  (entities-at-tile 324 576)

  ;; Count entities in an area
  (count-entities-in-area 300 550 50 50)

  ;; Scan area around throne (radius 20 tiles)
  (scan-area-around-throne 20)

  ;; Get all stockpile contents
  (stockpile-contents-summary)

  :rcf)

;; ============================================================================
;; Furniture Item Inspection
;; ============================================================================

(defn get-furniture-data
  "Get the furniture data instance from the settlement."
  []
  (let [rooms (SETT/ROOMS)]
    (.fData rooms)))

(defn get-furniture-item
  "Get furniture item at a specific tile coordinate."
  [tx ty]
  (let [fdata (get-furniture-data)]
    (.get (.item fdata) tx ty)))

(defn has-furniture-item?
  "Check if a tile has a furniture item."
  [tx ty]
  (some? (get-furniture-item tx ty)))

(defn furniture-items-in-area
  "Find all tiles with furniture items in an area.
   Returns a list of tile coordinates and their items."
  [start-x start-y width height]
  (for [x (range start-x (+ start-x width))
        y (range start-y (+ start-y height))
        :when (has-furniture-item? x y)]
    {:tile [x y]
     :item (get-furniture-item x y)}))

(defn scan-furniture-near-throne
  "Scan for furniture items near the throne.
   This can find crates, resources, and other items."
  [radius]
  (let [{:keys [x y]} (throne/throne-position)
        items (furniture-items-in-area (- x radius) (- y radius) (* 2 radius) (* 2 radius))]
    {:throne-position {:x x :y y}
     :furniture-items-found (count items)
     :sample-items (take 10 items)}))

(comment
  ;; Scan for furniture items near throne
  (scan-furniture-near-throne 30)

  ;; Check specific tile for furniture
  (has-furniture-item? 324 576)
  (get-furniture-item 324 576)

  ;; Count furniture in area
  (count (furniture-items-in-area 300 550 50 50))

  :rcf)

;; ============================================================================
;; Ground/Tile Resource Investigation
;; ============================================================================

(defn get-ground-tile
  "Get ground tile data at a specific coordinate.
   The GROUND.MAP holds terrain data including resources on ground."
  [tx ty]
  (let [ground-map (.. SETT GROUND MAP)]
    (.get ground-map tx ty)))

(comment
  (get-ground-tile 325 578)
  :rcf)


(defn explore-ground-tile
  "Explore all available data from a ground tile.
   Returns a map of field names to values."
  [tx ty]
  (let [tile (get-ground-tile tx ty)]
    (when tile
      (try
        {:tile-class (str (class tile))
         ;; Known fields from farm.clj
         :farm-fertility (try (.-farm tile) (catch Exception _ nil))
         ;; Try to explore other fields
         :bean (try (bean tile) (catch Exception _ nil))}
        (catch Exception e
          {:error (.getMessage e)})))))

(defn get-tally-global-amount
  "Try to get global resource amount from tally (not per-warehouse).
   The tally might have global accessors."
  [resource]
  (let [tally (warehouse/get-stockpile-tally)
        amount (.amount tally)]
    ;; Try to get total amount across all warehouses
    (try
      ;; Method 1: Try calling .total or similar
      (let [warehouses (warehouse/all-warehouses)]
        (if (empty? warehouses)
          ;; No warehouses - try to find global storage
          {:warehouses 0
           :try-global (try (.get amount resource nil) (catch Exception e {:error (.getMessage e)}))}
          ;; Sum across warehouses
          {:warehouses (count warehouses)
           :total-in-warehouses (reduce + 0 (map #(.get amount resource %) warehouses))}))
      (catch Exception e
        {:error (.getMessage e)}))))

(defn explore-sett-structure
  "Explore the SETT class to find resource-related fields/methods.
   This helps discover how ground items might be stored."
  []
  (let [sett-class (class SETT)]
    {:class-name (.getName sett-class)
     ;; List all public static fields
     :static-methods (->> (.getMethods sett-class)
                          (filter #(java.lang.reflect.Modifier/isStatic (.getModifiers %)))
                          (map #(.getName %))
                          (take 50))
     ;; List all public fields  
     :static-fields (->> (.getFields sett-class)
                         (filter #(java.lang.reflect.Modifier/isStatic (.getModifiers %)))
                         (map #(.getName %))
                         (take 50))}))

(defn list-all-entity-classes-in-area
  "List all unique entity class types in an area.
   This helps identify what kinds of entities exist (humanoids, animals, items, etc.)."
  [start-x start-y width height]
  (let [entities (entities-in-area start-x start-y width height)]
    {:count (count entities)
     :classes (->> entities
                   (map #(str (class %)))
                   (frequencies))}))

(defn explore-tile-deeply
  "Deep exploration of a specific tile to find all data layers."
  [tx ty]
  {:tile [tx ty]
   :entities (entities-at-tile tx ty)
   :furniture (get-furniture-item tx ty)
   :ground-tile (explore-ground-tile tx ty)})

(defn explore-things
  "Explore SETT.THINGS() - may contain loose items/objects on ground."
  []
  (try
    (let [things (SETT/THINGS)]
      {:class (str (class things))
       :bean (try (bean things) (catch Exception _ "bean failed"))
       :methods (->> (.getMethods (class things))
                     (map #(.getName %))
                     (filter #(not (.startsWith % "wait")))
                     (take 30)
                     (sort))})
    (catch Exception e
      {:error (.getMessage e)})))

(defn explore-tile-map
  "Explore SETT.TILE_MAP() - may have per-tile resource information."
  []
  (try
    (let [tile-map (SETT/TILE_MAP)]
      {:class (str (class tile-map))
       :methods (->> (.getMethods (class tile-map))
                     (map #(.getName %))
                     (filter #(not (.startsWith % "wait")))
                     (take 30)
                     (sort))})
    (catch Exception e
      {:error (.getMessage e)})))

(defn get-tile-map-data
  "Get data from TILE_MAP at a specific coordinate."
  [tx ty]
  (try
    (let [tile-map (SETT/TILE_MAP)]
      {:tile [tx ty]
       :tile-map-class (str (class tile-map))
       ;; Try .get method if exists
       :get-result (try (.get tile-map tx ty) (catch Exception e {:error (.getMessage e)}))})
    (catch Exception e
      {:error (.getMessage e)})))

(defn explore-stats-stored
  "Explore STATS.STORED() - may track global resource storage."
  []
  (try
    (let [stored (STATS/STORED)]
      {:class (str (class stored))
       :methods (->> (.getMethods (class stored))
                     (map #(.getName %))
                     (filter #(not (.startsWith % "wait")))
                     (take 30)
                     (sort))
       :bean (try (bean stored) (catch Exception _ "bean failed"))})
    (catch Exception e
      {:error (.getMessage e)})))

(defn get-stored-amount
  "Get stored amount for a resource from STATS.STORED()."
  [resource]
  (try
    (let [stored (STATS/STORED)]
      ;; Try different methods to get the amount
      {:resource-name (str (.name resource))
       :get-result (try (.get stored resource) (catch Exception e {:error (.getMessage e)}))
       :getD-result (try (.getD stored resource) (catch Exception e {:error (.getMessage e)}))})
    (catch Exception e
      {:error (.getMessage e)})))

(defn global-resource-amounts
  "Get global resource amounts for common resources using STATS.STORED()."
  []
  {:wood (get-stored-amount (RESOURCES/WOOD))
   :stone (get-stored-amount (RESOURCES/STONE))
   :bread (try (get-stored-amount (.get (RESOURCES/map) "BREAD" nil)) (catch Exception _ nil))
   :meat (try (get-stored-amount (.get (RESOURCES/map) "MEAT" nil)) (catch Exception _ nil))})

(defn explore-tally-totals
  "Explore tally system to see if there are global totals (not per-warehouse).
   The tally might have methods to get totals across all storage."
  []
  (try
    (let [tally (warehouse/get-stockpile-tally)
          wood (RESOURCES/WOOD)]
      {:tally-class (str (class tally))
       :methods (->> (.getMethods (class tally))
                     (map #(.getName %))
                     (filter #(or (.contains % "otal")
                                 (.contains % "ll")
                                 (.contains % "sum")))
                     (take 20)
                     (sort))
       ;; Try getting total without warehouse
       :try-total-nil (try (.get (.amount tally) wood nil) (catch Exception e {:error (.getMessage e)}))
       :amount-class (str (class (.amount tally)))
       :amount-methods (->> (.getMethods (class (.amount tally)))
                           (map #(.getName %))
                           (take 20)
                           (sort))})
    (catch Exception e
      {:error (.getMessage e)})))

(defn explore-halfents
  "Explore SETT.HALFENTS() - might contain half-entities like items."
  []
  (try
    (let [halfents (SETT/HALFENTS)]
      {:class (str (class halfents))
       :methods (->> (.getMethods (class halfents))
                     (map #(.getName %))
                     (filter #(not (.startsWith % "wait")))
                     (take 30)
                     (sort))})
    (catch Exception e
      {:error (.getMessage e)})))

(defn explore-minerals
  "Explore SETT.MINERALS() - might have resource deposit info."
  []
  (try
    (let [minerals (SETT/MINERALS)]
      {:class (str (class minerals))
       :methods (->> (.getMethods (class minerals))
                     (map #(.getName %))
                     (filter #(not (.startsWith % "wait")))
                     (take 30)
                     (sort))})
    (catch Exception e
      {:error (.getMessage e)})))

(defn get-minerals-at-tile
  "Get mineral data at a specific tile."
  [tx ty]
  (try
    (let [minerals (SETT/MINERALS)]
      {:tile [tx ty]
       :get-result (try (.get minerals tx ty) (catch Exception e {:error (.getMessage e)}))})
    (catch Exception e
      {:error (.getMessage e)})))

(defn comprehensive-tile-scan
  "Do a comprehensive scan of what's at a tile using ALL known systems."
  [tx ty]
  {:tile [tx ty]
   ;; Entity system
   :entities (let [ents (entities-at-tile tx ty)]
               {:count (count (or ents []))
                :classes (when ents (map #(str (class %)) ents))})
   ;; Furniture system
   :furniture (get-furniture-item tx ty)
   ;; Ground/terrain
   :ground (explore-ground-tile tx ty)
   ;; Tile map
   :tile-map (get-tile-map-data tx ty)
   ;; Minerals
   :minerals (get-minerals-at-tile tx ty)})

(comment
  ;; =====================================================
  ;; INVESTIGATION: Finding Ground Items
  ;; Target tile: (325, 578) reportedly has 100 wood
  ;; =====================================================
  
  ;; 1. Explore SETT structure to find resource-related methods
  (explore-sett-structure)
  
  ;; 2. Deep explore the target tile
  (explore-tile-deeply 325 578)
  
  ;; 3. Check ground tile data
  (explore-ground-tile 325 578)
  
  ;; 4. Check global tally for wood
  (get-tally-global-amount (RESOURCES/WOOD))
  
  ;; 5. List all entity classes near throne
  (let [{:keys [x y]} (throne/throne-position)]
    (list-all-entity-classes-in-area (- x 10) (- y 10) 20 20))
  
  ;; 6. Check specific area around reported wood location
  (list-all-entity-classes-in-area 320 573 10 10)
  
  ;; =====================================================
  ;; NEW EXPLORATION: THINGS, TILE_MAP, STATS.STORED
  ;; =====================================================
  
  ;; 7. Explore SETT.THINGS() - might contain loose items
  (explore-things)
  
  ;; 8. Explore SETT.TILE_MAP() - per-tile data
  (explore-tile-map)
  (get-tile-map-data 325 578)
  
  ;; 9. Explore STATS.STORED() - global resource tracking
  (explore-stats-stored)
  (get-stored-amount (RESOURCES/WOOD))
  (global-resource-amounts)
  
  ;; 10. Explore tally totals without warehouse
  (explore-tally-totals)
  
  ;; 11. Explore HALFENTS - might have loose items
  (explore-halfents)
  
  ;; 12. Explore MINERALS - resource deposits
  (explore-minerals)
  (get-minerals-at-tile 325 578)
  
  ;; 13. Comprehensive tile scan - use ALL systems
  (comprehensive-tile-scan 325 578)
  
  :rcf)

