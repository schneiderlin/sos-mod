(ns game.building
  "Building/Room access functions for Songs of Syx.
   Buildings (rooms) are all placeable structures in the settlement.
   
   This namespace provides reusable functions for:
   - Accessing room registry (all blueprints)
   - Getting room properties (name, desc, category, etc.)
   - Querying room categories
   - Accessing construction costs and production info
   - Accessing room icons for sprite extraction"
  (:import
   [settlement.room.main RoomBlueprint RoomBlueprintImp]
   [settlement.room.main.category RoomCategorySub RoomCategories$RoomCategoryMain]
   [settlement.room.main.furnisher Furnisher]
   [settlement.room.industry.module Industry IndustryResource INDUSTRY_HASER]
   [settlement.main SETT]
   [init.resources RESOURCE]
   [init.sprite.UI Icon]))

;; ============================================
;; Room Registry Access
;; ============================================

(defn- get-static-field
  "Get a static field value using reflection.
   Used for accessing package-private static fields."
  [^Class class field-name]
  (let [field (.getDeclaredField class field-name)]
    (.setAccessible field true)
    (.get field nil)))

(defn rooms-instance
  "Get the ROOMS instance from settlement.
   Note: Requires game to be loaded with a settlement."
  []
  (SETT/ROOMS))

(defn all-blueprints
  "Get all room blueprints in the game.
   Returns: LIST<RoomBlueprint>"
  []
  (get-static-field RoomBlueprint "ALL"))

(comment
  (all-blueprints)
  :rcf)

(defn all-blueprint-imps
  "Get all RoomBlueprintImp instances (rooms with detailed info).
   Returns: LIST<RoomBlueprintImp>"
  []
  (get-static-field RoomBlueprintImp "IMPS"))

(defn blueprint-count
  "Get total number of room blueprints."
  []
  (.size (all-blueprints)))

(defn blueprint-imp-count
  "Get total number of RoomBlueprintImp instances."
  []
  (.size (all-blueprint-imps)))

;; ============================================
;; RoomBlueprint Properties
;; ============================================

(defn blueprint-key
  "Get room blueprint unique key (e.g., \"FARM\", \"REFINER_SMELTER\")."
  [^RoomBlueprint bp]
  (.key bp))

(defn blueprint-index
  "Get room blueprint index in ALL list."
  [^RoomBlueprint bp]
  (.index bp))

;; ============================================
;; RoomBlueprintImp Properties (detailed rooms)
;; ============================================

(defn room-name
  "Get room display name."
  [^RoomBlueprintImp room]
  (str (.name (.info room))))

(defn room-desc
  "Get room description."
  [^RoomBlueprintImp room]
  (str (.desc (.info room))))

(defn room-type
  "Get room type string (e.g., \"REFINER\", \"FARM\")."
  [^RoomBlueprintImp room]
  (.-type room))

(defn room-category-sub
  "Get room's sub-category (RoomCategorySub)."
  [^RoomBlueprintImp room]
  (.-cat room))

(defn room-degrade-rate
  "Get room degradation rate (0-1)."
  [^RoomBlueprintImp room]
  (.degradeRate room))

(defn room-type-index
  "Get room's index within its type."
  [^RoomBlueprintImp room]
  (.typeIndex room))

(defn room-has-bonus?
  "Check if room has a bonus boostable."
  [^RoomBlueprintImp room]
  (some? (.bonus room)))

(defn room-bonus
  "Get room's bonus boostable (or nil)."
  [^RoomBlueprintImp room]
  (.bonus room))

;; ============================================
;; Category Properties
;; ============================================

(defn category-sub-name
  "Get sub-category display name."
  [^RoomCategorySub cat]
  (str (.name cat)))

(defn category-sub-rooms
  "Get all rooms in this sub-category."
  [^RoomCategorySub cat]
  (.rooms cat))

(defn category-main
  "Get the main category for a sub-category."
  [^RoomCategorySub cat]
  (.main cat))

(defn category-main-name
  "Get main category display name."
  [^RoomCategories$RoomCategoryMain cat]
  (str (.-name cat)))

(defn category-main-subs
  "Get all sub-categories in this main category."
  [^RoomCategories$RoomCategoryMain cat]
  (.-subs cat))

(defn category-main-all-rooms
  "Get all rooms in this main category (across all subs)."
  [^RoomCategories$RoomCategoryMain cat]
  (.all cat))

;; ============================================
;; Furnisher (Constructor) Properties
;; ============================================

(defn room-furnisher
  "Get room's furnisher/constructor."
  [^RoomBlueprintImp room]
  (.constructor room))

(defn furnisher-resource-count
  "Get number of resources used in construction."
  [^Furnisher furn]
  (.resources furn))

(defn furnisher-resource
  "Get construction resource at index."
  [^Furnisher furn index]
  (.resource furn index))

(defn furnisher-area-cost
  "Get area cost for resource at index with upgrade level."
  [^Furnisher furn index upgrade]
  (.areaCost furn index upgrade))

(defn furnisher-area-cost-flat
  "Get flat area cost for resource at index."
  [^Furnisher furn index]
  (.areaCostFlat furn index))

(defn furnisher-uses-area?
  "Check if furnisher uses area-based construction."
  [^Furnisher furn]
  (.usesArea furn))

(defn furnisher-must-be-indoors?
  "Check if room must be built indoors."
  [^Furnisher furn]
  (.mustBeIndoors furn))

(defn furnisher-must-be-outdoors?
  "Check if room must be built outdoors."
  [^Furnisher furn]
  (.mustBeOutdoors furn))

(defn furnisher-floors
  "Get list of available floor types."
  [^Furnisher furn]
  (.-floors furn))

(defn furnisher-stats
  "Get list of furnisher stats."
  [^Furnisher furn]
  (.stats furn))

;; ============================================
;; Industry Properties (for production rooms)
;; ============================================

(defn room-has-industries?
  "Check if room implements INDUSTRY_HASER (has production)."
  [room]
  (instance? INDUSTRY_HASER room))

(defn room-industries
  "Get list of industries for a production room.
   Returns nil if room doesn't have industries."
  [room]
  (when (room-has-industries? room)
    (.industries ^INDUSTRY_HASER room)))

(defn industry-inputs
  "Get input resources for an industry."
  [^Industry ind]
  (.ins ind))

(defn industry-outputs
  "Get output resources for an industry."
  [^Industry ind]
  (.outs ind))

(defn industry-resource-key
  "Get resource key from IndustryResource."
  [^IndustryResource ir]
  (.key (.-resource ir)))

(defn industry-resource-rate
  "Get production rate from IndustryResource."
  [^IndustryResource ir]
  (.-rateSeconds ir))

(defn industry-ai-mul
  "Get AI production multiplier."
  [^Industry ind]
  (.-AIMul ind))

;; ============================================
;; Room Icon Access
;; ============================================

(defn room-icon
  "Get room's icon (Icon object).
   Returns the Icon from RoomBlueprintImp."
  [^RoomBlueprintImp room]
  (.-icon room))

(defn room-icon-big
  "Get room's big icon (scaled to 32x32)."
  [^RoomBlueprintImp room]
  (.iconBig room))

(defn icon-size
  "Get the native size of an icon (16, 24, or 32)."
  [^Icon icon]
  (when icon
    (.-size icon)))

(defn icon-size-key
  "Get the sheet key (:small, :medium, :large) based on icon size."
  [icon]
  (case (icon-size icon)
    16 :small
    24 :medium
    32 :large
    nil))

(defn- get-icon-sheet-private
  "Get the internal IconSheet from an Icon using reflection.
   Icons may wrap an IconSheet internally."
  [icon]
  (when icon
    (try
      (let [icon-class (Class/forName "init.sprite.UI.Icon")
            sprite-field (.getDeclaredField icon-class "sprite")]
        (.setAccessible sprite-field true)
        (.get sprite-field icon))
      (catch Exception _ nil))))

(defn- get-tile-from-iconsheet
  "Extract tile index from an IconSheet object."
  [iconsheet]
  (when iconsheet
    (try
      (let [icon-sheet-class (Class/forName "init.sprite.UI.Icon$IconSheet")]
        (when (= icon-sheet-class (class iconsheet))
          (let [tile-field (.getDeclaredField icon-sheet-class "tile")]
            (.setAccessible tile-field true)
            (.get tile-field iconsheet))))
      (catch Exception _ nil))))

(defn icon-tile-index
  "Get the tile index from an Icon.
   Works by getting the inner sprite and extracting tile if it's an IconSheet."
  [icon]
  (when icon
    (try
      (let [icon-class (class icon)
            icon-sheet-class (Class/forName "init.sprite.UI.Icon$IconSheet")]
        (cond
          ;; Direct IconSheet instance
          (= icon-sheet-class icon-class)
          (get-tile-from-iconsheet icon)
          
          ;; Icon wrapping a sprite - check if it has an internal IconSheet
          :else
          (let [inner-sprite (get-icon-sheet-private icon)]
            (when (= icon-sheet-class (class inner-sprite))
              (get-tile-from-iconsheet inner-sprite)))))
      (catch Exception _ nil))))

(defn icon-is-composite?
  "Check if an icon is a composite icon (BG + FG) rather than a simple IconSheet.
   Composite icons can't be extracted by tile index."
  [icon]
  (when icon
    (let [inner-sprite (get-icon-sheet-private icon)
          icon-sheet-class (try (Class/forName "init.sprite.UI.Icon$IconSheet") (catch Exception _ nil))]
      (and inner-sprite
           (not= icon-sheet-class (class inner-sprite))))))

(defn icon-inner-class-name
  "Get the class name of the inner sprite for debugging."
  [icon]
  (when icon
    (when-let [inner (get-icon-sheet-private icon)]
      (.getName (class inner)))))

(defn room-icon-info
  "Get icon information for a room.
   Returns map with :key, :name, :icon-size, :tile-index, etc."
  [^RoomBlueprintImp room]
  (let [icon (room-icon room)
        size (icon-size icon)
        tile-index (icon-tile-index icon)]
    {:key (blueprint-key room)
     :name (room-name room)
     :icon-size size
     :icon-size-key (icon-size-key icon)
     :tile-index tile-index
     :is-composite (icon-is-composite? icon)
     :inner-class (when (nil? tile-index) (icon-inner-class-name icon))}))

;; ============================================
;; Data Conversion
;; ============================================

(defn resource->key
  "Get resource key string."
  [^RESOURCE res]
  (.key res))

(defn furnisher->map
  "Convert Furnisher to Clojure map with construction info."
  [^Furnisher furn]
  (when furn
    (let [res-count (furnisher-resource-count furn)]
      {:uses-area (furnisher-uses-area? furn)
       :must-be-indoors (furnisher-must-be-indoors? furn)
       :must-be-outdoors (furnisher-must-be-outdoors? furn)
       :resources (when (pos? res-count)
                    (vec (for [i (range res-count)]
                           {:resource-key (resource->key (furnisher-resource furn i))
                            :area-cost (furnisher-area-cost-flat furn i)})))
       :stats-count (when-let [stats (furnisher-stats furn)]
                      (.size stats))})))

(defn industry-resource->map
  "Convert IndustryResource to Clojure map."
  [^IndustryResource ir]
  {:resource-key (industry-resource-key ir)
   :rate-per-second (industry-resource-rate ir)})

(defn industry->map
  "Convert Industry to Clojure map."
  [^Industry ind]
  {:index (.index ind)
   :ai-multiplier (industry-ai-mul ind)
   :inputs (mapv industry-resource->map (industry-inputs ind))
   :outputs (mapv industry-resource->map (industry-outputs ind))})

(defn category-sub->map
  "Convert RoomCategorySub to Clojure map."
  [^RoomCategorySub cat]
  (let [main (category-main cat)]
    {:name (category-sub-name cat)
     :main-name (when main (category-main-name main))
     :room-count (.size (category-sub-rooms cat))}))

(defn room-imp->map
  "Convert RoomBlueprintImp to Clojure map."
  [^RoomBlueprintImp room]
  (let [cat (room-category-sub room)
        furn (room-furnisher room)]
    {:key (blueprint-key room)
     :index (blueprint-index room)
     :name (room-name room)
     :description (room-desc room)
     :type (room-type room)
     :type-index (room-type-index room)
     :degrade-rate (room-degrade-rate room)
     :has-bonus (room-has-bonus? room)
     :category (when cat (category-sub->map cat))
     :construction (furnisher->map furn)
     :industries (when (room-has-industries? room)
                   (mapv industry->map (room-industries room)))}))

;; ============================================
;; Batch Operations
;; ============================================

(defn all-rooms-as-maps
  "Get all RoomBlueprintImp as Clojure maps."
  []
  (mapv room-imp->map (all-blueprint-imps)))

(defn rooms-by-category
  "Get rooms grouped by sub-category name."
  []
  (->> (all-blueprint-imps)
       (filter #(room-category-sub %))
       (group-by #(category-sub-name (room-category-sub %)))
       (into (sorted-map))))

(defn rooms-by-type
  "Get rooms grouped by type."
  []
  (->> (all-blueprint-imps)
       (filter #(room-type %))
       (group-by room-type)
       (into (sorted-map))))

(defn production-rooms
  "Get all rooms that have industries (production)."
  []
  (filter room-has-industries? (all-blueprint-imps)))

(defn production-rooms-as-maps
  "Get all production rooms as Clojure maps."
  []
  (mapv room-imp->map (production-rooms)))

;; ============================================
;; Queries
;; ============================================

(defn find-room-by-key
  "Find room by key."
  [key]
  (first (filter #(= (blueprint-key %) key) (all-blueprint-imps))))

(defn find-rooms-by-type
  "Find all rooms of a given type."
  [type]
  (filter #(= (room-type %) type) (all-blueprint-imps)))

(defn find-rooms-producing
  "Find rooms that produce a specific resource."
  [resource-key]
  (->> (production-rooms)
       (filter (fn [room]
                 (some (fn [ind]
                         (some #(= (industry-resource-key %) resource-key)
                               (industry-outputs ind)))
                       (room-industries room))))))

(defn find-rooms-consuming
  "Find rooms that consume a specific resource."
  [resource-key]
  (->> (production-rooms)
       (filter (fn [room]
                 (some (fn [ind]
                         (some #(= (industry-resource-key %) resource-key)
                               (industry-inputs ind)))
                       (room-industries room))))))

(comment
  ;; === Usage Examples ===
  
  ;; Get all blueprints
  (all-blueprints)
  (blueprint-count)
  
  ;; Get all room imps
  (all-blueprint-imps)
  (blueprint-imp-count)
  
  ;; Get first room info
  (let [room (first (all-blueprint-imps))]
    (println "Key:" (blueprint-key room))
    (println "Name:" (room-name room))
    (println "Type:" (room-type room))
    (println "Category:" (when-let [c (room-category-sub room)]
                           (category-sub-name c))))
  
  ;; Convert to map
  (room-imp->map (first (all-blueprint-imps)))
  
  ;; Find specific room
  (find-room-by-key "FARM")
  
  ;; Get rooms by type
  (keys (rooms-by-type))
  
  ;; Get rooms by category
  (keys (rooms-by-category))
  
  ;; Get production rooms
  (count (production-rooms))
  
  ;; Find rooms producing BREAD
  (find-rooms-producing "BREAD")
  
  :rcf)

