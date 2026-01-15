(ns game.booster
  "Booster access functions for Songs of Syx.
   Boosters are stat modifiers that affect various game aspects.
   
   This namespace provides reusable functions for:
   - Accessing boostable registry
   - Getting boostable properties
   - Querying boostable categories"
  (:import
   [game.boosting BOOSTING BOOSTABLES Boostable BoostableCat]))

;; ============================================
;; Boostable Registry Access
;; ============================================

(defn all-boostables
  "Get all boostables in the game.
   Returns: LIST<Boostable>"
  []
  (BOOSTING/ALL))

(defn boostable-count
  "Get total number of boostables."
  []
  (.size (all-boostables)))

(defn boostable-map
  "Get the boostable map (key -> Boostable).
   Returns: RMAP<Boostable>"
  []
  (BOOSTING/MAP))

(defn get-boostable
  "Get a boostable by key (e.g., \"PHYSICS_SPEED\").
   Returns nil if not found."
  [key]
  (let [m (boostable-map)]
    (when (.containsKey (.map m) key)
      (.get (.map m) key))))

;; ============================================
;; Category Access
;; ============================================

(defn all-collections
  "Get all boostable collections/categories.
   Returns: LIST<BoostableCat>"
  []
  (BOOSTABLES/colls))

(defn collection-count
  "Get number of boostable categories."
  []
  (.size (all-collections)))

(defn physics-category
  "Get Physics boostables category."
  []
  (BOOSTABLES/PHYSICS))

(defn battle-category
  "Get Battle boostables category."
  []
  (BOOSTABLES/BATTLE))

(defn behaviour-category
  "Get Behaviour boostables category."
  []
  (BOOSTABLES/BEHAVIOUR))

(defn activity-category
  "Get Activity boostables category."
  []
  (BOOSTABLES/ACTIVITY))

(defn civic-category
  "Get Civic boostables category."
  []
  (BOOSTABLES/CIVICS))

(defn noble-category
  "Get Noble/Personality boostables category."
  []
  (BOOSTABLES/NOBLE))

(defn rooms-category
  "Get Rooms/Buildings boostables category."
  []
  (BOOSTABLES/ROOMS))

;; ============================================
;; Specific Physics Boostables
;; ============================================

(defn physics-mass
  "Get the Mass boostable."
  []
  (.-MASS (physics-category)))

(defn physics-stamina
  "Get the Stamina boostable."
  []
  (.-STAMINA (physics-category)))

(defn physics-speed
  "Get the Speed boostable."
  []
  (.-SPEED (physics-category)))

(defn physics-acceleration
  "Get the Acceleration boostable."
  []
  (.-ACCELERATION (physics-category)))

(defn physics-health
  "Get the Health boostable."
  []
  (.-HEALTH (physics-category)))

(defn physics-death-age
  "Get the Death Age (Lifespan) boostable."
  []
  (.-DEATH_AGE (physics-category)))

(defn physics-heat-resistance
  "Get the Heat Resistance boostable."
  []
  (.-RESISTANCE_HOT (physics-category)))

(defn physics-cold-resistance
  "Get the Cold Resistance boostable."
  []
  (.-RESISTANCE_COLD (physics-category)))

(defn physics-soiling
  "Get the Soiling boostable."
  []
  (.-SOILING (physics-category)))

;; ============================================
;; Specific Battle Boostables
;; ============================================

(defn battle-offence
  "Get the Offence boostable."
  []
  (.-OFFENCE (battle-category)))

(defn battle-defence
  "Get the Defence boostable."
  []
  (.-DEFENCE (battle-category)))

(defn battle-morale
  "Get the Battle Morale boostable."
  []
  (.-MORALE (battle-category)))

;; ============================================
;; Specific Behaviour Boostables
;; ============================================

(defn behaviour-lawfulness
  "Get the Lawfulness boostable."
  []
  (.-LAWFULNESS (behaviour-category)))

(defn behaviour-submission
  "Get the Submission boostable."
  []
  (.-SUBMISSION (behaviour-category)))

(defn behaviour-loyalty
  "Get the Loyalty boostable."
  []
  (.-LOYALTY (behaviour-category)))

(defn behaviour-happiness
  "Get the Happiness boostable."
  []
  (.-HAPPI (behaviour-category)))

(defn behaviour-sanity
  "Get the Sanity boostable."
  []
  (.-SANITY (behaviour-category)))

;; ============================================
;; Specific Civic Boostables
;; ============================================

(defn civic-maintenance
  "Get the Maintenance/Robustness boostable."
  []
  (.-MAINTENANCE (civic-category)))

(defn civic-spoilage
  "Get the Spoilage/Conservation boostable."
  []
  (.-SPOILAGE (civic-category)))

(defn civic-accident
  "Get the Accident/Safety boostable."
  []
  (.-ACCIDENT (civic-category)))

(defn civic-immigration
  "Get the Immigration boostable."
  []
  (.-IMMIGRATION (civic-category)))

(defn civic-innovation
  "Get the Innovation boostable."
  []
  (.-INNOVATION (civic-category)))

(defn civic-diplomacy
  "Get the Diplomacy/Emissary boostable."
  []
  (.-DIPLOMACY (civic-category)))

;; ============================================
;; Boostable Properties
;; ============================================

(defn boostable-key
  "Get boostable unique key (e.g., \"PHYSICS_SPEED\")."
  [^Boostable bo]
  (.-key bo))

(defn boostable-index
  "Get boostable index in ALL list."
  [^Boostable bo]
  (.index bo))

(defn boostable-name
  "Get boostable display name."
  [^Boostable bo]
  (str (.name bo)))

(defn boostable-desc
  "Get boostable description."
  [^Boostable bo]
  (str (.desc bo)))

(defn boostable-base-value
  "Get boostable base/default value."
  [^Boostable bo]
  (.-baseValue bo))

(defn boostable-min-value
  "Get boostable minimum value."
  [^Boostable bo]
  (.-minValue bo))

(defn boostable-category
  "Get the BoostableCat this boostable belongs to."
  [^Boostable bo]
  (.-cat bo))

(defn boostable-icon
  "Get boostable icon."
  [^Boostable bo]
  (.-icon bo))

;; ============================================
;; BoostableCat Properties
;; ============================================

(defn category-prefix
  "Get category key prefix (e.g., \"PHYSICS_\")."
  [^BoostableCat cat]
  (.-prefix cat))

(defn category-name
  "Get category display name."
  [^BoostableCat cat]
  (str (.-name cat)))

(defn category-desc
  "Get category description."
  [^BoostableCat cat]
  (str (.-desc cat)))

(defn category-type-mask
  "Get category type mask (TYPE_SETT, TYPE_WORLD, etc.)."
  [^BoostableCat cat]
  (.-typeMask cat))

(defn category-boostables
  "Get all boostables in this category.
   Returns: LIST<Boostable>"
  [^BoostableCat cat]
  (.all cat))

;; ============================================
;; Type Constants
;; ============================================

(def TYPE_CRAP BoostableCat/TYPE_CRAP)
(def TYPE_WORLD BoostableCat/TYPE_WORLD)
(def TYPE_SETT BoostableCat/TYPE_SETT)

(defn type-mask->keywords
  "Convert type mask to keyword set."
  [mask]
  (cond-> #{}
    (pos? (bit-and mask TYPE_CRAP)) (conj :crap)
    (pos? (bit-and mask TYPE_WORLD)) (conj :world)
    (pos? (bit-and mask TYPE_SETT)) (conj :settlement)))

;; ============================================
;; Data Conversion
;; ============================================

(defn boostable->map
  "Convert Boostable to Clojure map."
  [^Boostable bo]
  (let [cat (boostable-category bo)]
    {:key (boostable-key bo)
     :index (boostable-index bo)
     :name (boostable-name bo)
     :description (boostable-desc bo)
     :base-value (boostable-base-value bo)
     :min-value (boostable-min-value bo)
     :category-prefix (category-prefix cat)
     :category-name (category-name cat)
     :type-mask (category-type-mask cat)
     :types (type-mask->keywords (category-type-mask cat))}))

(defn category->map
  "Convert BoostableCat to Clojure map."
  [^BoostableCat cat]
  {:prefix (category-prefix cat)
   :name (category-name cat)
   :description (category-desc cat)
   :type-mask (category-type-mask cat)
   :types (type-mask->keywords (category-type-mask cat))
   :boostable-count (.size (category-boostables cat))
   :boostable-keys (mapv boostable-key (category-boostables cat))})

(defn category->map-full
  "Convert BoostableCat to Clojure map with full boostable data."
  [^BoostableCat cat]
  {:prefix (category-prefix cat)
   :name (category-name cat)
   :description (category-desc cat)
   :type-mask (category-type-mask cat)
   :types (type-mask->keywords (category-type-mask cat))
   :boostables (mapv boostable->map (category-boostables cat))})

;; ============================================
;; Batch Operations
;; ============================================

(defn all-boostables-as-maps
  "Get all boostables as Clojure maps."
  []
  (mapv boostable->map (all-boostables)))

(defn all-categories-as-maps
  "Get all categories as Clojure maps."
  []
  (mapv category->map (all-collections)))

(defn all-categories-full
  "Get all categories with full boostable data."
  []
  (mapv category->map-full (all-collections)))

;; ============================================
;; Queries
;; ============================================

(defn boostables-by-category
  "Get all boostables grouped by category prefix."
  []
  (->> (all-boostables)
       (group-by #(category-prefix (boostable-category %)))
       (into (sorted-map))))

(defn boostables-by-type
  "Get all boostables grouped by type (settlement, world, etc.)."
  []
  (let [all-bo (all-boostables-as-maps)]
    {:settlement (filter #(contains? (:types %) :settlement) all-bo)
     :world (filter #(contains? (:types %) :world) all-bo)
     :crap (filter #(contains? (:types %) :crap) all-bo)}))

(defn find-boostables-by-prefix
  "Find all boostables whose key starts with prefix."
  [prefix]
  (filter #(.startsWith (boostable-key %) prefix) (all-boostables)))

(defn physics-boostables
  "Get all physics boostables."
  []
  (category-boostables (physics-category)))

(defn battle-boostables
  "Get all battle boostables."
  []
  (category-boostables (battle-category)))

(defn behaviour-boostables
  "Get all behaviour boostables."
  []
  (category-boostables (behaviour-category)))

(defn activity-boostables
  "Get all activity boostables."
  []
  (category-boostables (activity-category)))

(defn civic-boostables
  "Get all civic boostables."
  []
  (category-boostables (civic-category)))

(defn noble-boostables
  "Get all noble/personality boostables."
  []
  (category-boostables (noble-category)))

(defn room-boostables
  "Get all room/building boostables."
  []
  (category-boostables (rooms-category)))

(comment
  ;; === Usage Examples ===
  
  ;; Get all boostables
  (all-boostables)
  (boostable-count)
  
  ;; Get categories
  (all-collections)
  (collection-count)
  
  ;; Get specific boostable
  (def speed (physics-speed))
  (boostable-name speed)
  (boostable-desc speed)
  (boostable-base-value speed)
  
  ;; Get by key
  (get-boostable "PHYSICS_SPEED")
  
  ;; Get category info
  (category-name (physics-category))
  (count (physics-boostables))
  
  ;; Convert to map
  (boostable->map (physics-speed))
  (category->map (physics-category))
  
  ;; Batch convert
  (take 3 (all-boostables-as-maps))
  (all-categories-as-maps)
  
  ;; Query by category
  (keys (boostables-by-category))
  
  ;; Query by type
  (count (:settlement (boostables-by-type)))
  (count (:world (boostables-by-type)))
  
  :rcf)

