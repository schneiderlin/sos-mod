(ns game.resource
  "Resource access functions for Songs of Syx.
   Resources are all items/materials in the game.
   
   This namespace provides reusable functions for:
   - Accessing resource registry
   - Getting resource properties
   - Querying resource groups (edibles, drinkables, etc.)"
  (:import
   [init.resources RESOURCES RESOURCE Minable Growable ResGDrink ResGEat]
   [util.info INFO]))

;; ============================================
;; Resource Registry Access
;; ============================================

(defn all-resources
  "Get all resources in the game.
   Returns: LIST<RESOURCE>"
  []
  (RESOURCES/ALL))

(defn resource-count
  "Get total number of resources."
  []
  (.size (all-resources)))

(defn get-resource
  "Get a resource by key (e.g., \"BREAD\", \"_STONE\").
   Returns nil if not found."
  [key]
  (.get (RESOURCES/map) key nil))

(defn stone
  "Get the Stone resource."
  []
  (RESOURCES/STONE))

(defn wood
  "Get the Wood resource."
  []
  (RESOURCES/WOOD))

(defn livestock
  "Get the Livestock resource."
  []
  (RESOURCES/LIVESTOCK))

;; ============================================
;; Resource Properties
;; ============================================

(defn resource-key
  "Get resource unique key (e.g., \"BREAD\")."
  [^RESOURCE res]
  (.key res))

(defn resource-index
  "Get resource index in ALL list."
  [^RESOURCE res]
  (.index res))

(defn resource-name
  "Get resource display name."
  [^RESOURCE res]
  (str (.name res)))

(defn resource-names
  "Get resource plural name."
  [^RESOURCE res]
  (str (.names res)))

(defn resource-desc
  "Get resource description."
  [^RESOURCE res]
  (str (.desc res)))

(defn resource-category
  "Get resource category (0-9)."
  [^RESOURCE res]
  (.category res))

(defn resource-degrade-speed
  "Get resource decay/spoilage rate per year."
  [^RESOURCE res]
  (.degradeSpeed res))

(defn resource-price-cap
  "Get default price cap."
  [^RESOURCE res]
  (.-priceCapDef res))

(defn resource-price-mul
  "Get default price multiplier."
  [^RESOURCE res]
  (.-priceMulDef res))

(defn resource-icon
  "Get resource UI icon."
  [^RESOURCE res]
  (.icon res))

;; ============================================
;; Resource Groups
;; ============================================

(defn all-minables
  "Get all minable resources (terrain deposits).
   Returns: RMAP<Minable>"
  []
  (RESOURCES/minables))

(defn minable-list
  "Get list of all minables."
  []
  (.all (all-minables)))

(defn all-growables
  "Get all growable resources (crops/plants).
   Returns: GrowableGroup"
  []
  (RESOURCES/growable))

(defn growable-list
  "Get list of all growables."
  []
  (.all (all-growables)))

(defn all-drinks
  "Get all drinkable resources.
   Returns: ResGroup<ResGDrink>"
  []
  (RESOURCES/DRINKS))

(defn drink-list
  "Get list of all drinkables."
  []
  (.all (all-drinks)))

(defn all-edibles
  "Get all edible resources.
   Returns: ResGroup<ResGEat>"
  []
  (RESOURCES/EDI))

(defn edible-list
  "Get list of all edibles."
  []
  (.all (all-edibles)))

(defn edible?
  "Check if resource is edible."
  [^RESOURCE res]
  (.is (all-edibles) res))

(defn drinkable?
  "Check if resource is drinkable."
  [^RESOURCE res]
  (.is (all-drinks) res))

(defn categories-count
  "Get number of resource categories."
  []
  (RESOURCES/CATEGORIES))

;; ============================================
;; Minable Properties
;; ============================================

(defn minable-key
  "Get minable key."
  [^Minable m]
  (.key m))

(defn minable-index
  "Get minable index."
  [^Minable m]
  (.-index m))

(defn minable-resource
  "Get the RESOURCE this minable produces."
  [^Minable m]
  (.-resource m))

(defn minable-name
  "Get minable display name."
  [^Minable m]
  (str (.-name m)))

(defn minable-on-every-map?
  "Check if minable appears on every map."
  [^Minable m]
  (.-onEverymap m))

(defn minable-occurence
  "Get minable occurrence rate."
  [^Minable m]
  (.-occurence m))

(defn minable-fertility-increase
  "Get fertility increase from this minable."
  [^Minable m]
  (.-fertilityIncrease m))

;; ============================================
;; Growable Properties
;; ============================================

(defn growable-key
  "Get growable key."
  [^Growable g]
  (.key g))

(defn growable-index
  "Get growable index."
  [^Growable g]
  (.index g))

(defn growable-resource
  "Get the RESOURCE this growable produces."
  [^Growable g]
  (.-resource g))

(defn growable-seasonal-offset
  "Get seasonal offset (0-1)."
  [^Growable g]
  (.-seasonalOffset g))

(defn growable-growth-value
  "Get growth value (0-1)."
  [^Growable g]
  (.-growthValue g))

;; ============================================
;; Data Conversion
;; ============================================

(defn resource->map
  "Convert RESOURCE to Clojure map."
  [^RESOURCE res]
  {:key (resource-key res)
   :index (resource-index res)
   :name (resource-name res)
   :names (resource-names res)
   :description (resource-desc res)
   :category (resource-category res)
   :degrade-speed (resource-degrade-speed res)
   :price-cap (resource-price-cap res)
   :price-mul (resource-price-mul res)
   :edible (edible? res)
   :drinkable (drinkable? res)})

(defn minable->map
  "Convert Minable to Clojure map."
  [^Minable m]
  {:key (minable-key m)
   :index (minable-index m)
   :resource-key (resource-key (minable-resource m))
   :name (minable-name m)
   :on-every-map (minable-on-every-map? m)
   :occurence (minable-occurence m)
   :fertility-increase (minable-fertility-increase m)})

(defn growable->map
  "Convert Growable to Clojure map."
  [^Growable g]
  {:key (growable-key g)
   :index (growable-index g)
   :resource-key (resource-key (growable-resource g))
   :seasonal-offset (growable-seasonal-offset g)
   :growth-value (growable-growth-value g)})

(defn drink->map
  "Convert ResGDrink to Clojure map."
  [^ResGDrink d]
  {:key (.key d)
   :index (.index d)
   :resource-key (resource-key (.-resource d))
   :serve (.-serve d)})

(defn edible->map
  "Convert ResGEat to Clojure map."
  [^ResGEat e]
  {:key (.key e)
   :index (.index e)
   :resource-key (resource-key (.-resource e))
   :serve (.-serve e)})

;; ============================================
;; Batch Operations
;; ============================================

(defn all-resources-as-maps
  "Get all resources as Clojure maps."
  []
  (mapv resource->map (all-resources)))

(defn all-minables-as-maps
  "Get all minables as Clojure maps."
  []
  (mapv minable->map (minable-list)))

(defn all-growables-as-maps
  "Get all growables as Clojure maps."
  []
  (mapv growable->map (growable-list)))

(defn all-drinks-as-maps
  "Get all drinkables as Clojure maps."
  []
  (mapv drink->map (drink-list)))

(defn all-edibles-as-maps
  "Get all edibles as Clojure maps."
  []
  (mapv edible->map (edible-list)))

(comment
  ;; === Usage Examples ===
  
  ;; Get all resources
  (all-resources)
  (resource-count)
  
  ;; Get specific resource
  (def bread (get-resource "BREAD"))
  (resource-name bread)
  (resource-desc bread)
  (edible? bread)
  
  ;; Get special resources
  (resource-name (stone))
  (resource-name (wood))
  
  ;; Get groups
  (count (minable-list))
  (count (growable-list))
  (count (drink-list))
  (count (edible-list))
  
  ;; Convert to map
  (resource->map (stone))
  
  ;; Batch convert
  (take 3 (all-resources-as-maps))
  
  :rcf)

