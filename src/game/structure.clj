(ns game.structure
  "Structure access functions for Songs of Syx.
   Structures are world building materials (stone, wood, etc.)
   that define walls, floors, and ceilings.
   
   This namespace provides reusable functions for:
   - Accessing structure registry
   - Getting structure properties
   - Querying structure data"
  (:import
   [init.structure STRUCTURES Structure]
   [init.resources RESOURCE]))

;; ============================================
;; Structure Registry Access
;; ============================================

(defn all-structures
  "Get all structures in the game.
   Returns: LIST<Structure>"
  []
  (STRUCTURES/all))

(defn structure-count
  "Get total number of structures."
  []
  (.size (all-structures)))

(defn get-structure
  "Get a structure by key (e.g., \"_STONE\", \"_WOOD\").
   Returns nil if not found."
  [key]
  (.get (STRUCTURES/map) key nil))

(defn mud-structure
  "Get the MUD structure (default/fallback structure)."
  []
  (STRUCTURES/mud))

;; ============================================
;; Structure Properties
;; ============================================

(defn structure-key
  "Get structure unique key (e.g., \"_STONE\")."
  [^Structure s]
  (.-key s))

(defn structure-index
  "Get structure index in ALL list."
  [^Structure s]
  (.index s))

(defn structure-name
  "Get structure display name."
  [^Structure s]
  (str (.name s)))

(defn structure-desc
  "Get structure description."
  [^Structure s]
  (str (.desc s)))

(defn structure-name-wall
  "Get wall name (e.g., 'Stone Wall')."
  [^Structure s]
  (str (.-nameWall s)))

(defn structure-name-ceiling
  "Get ceiling name (e.g., 'Stone Ceiling')."
  [^Structure s]
  (str (.-nameCeiling s)))

(defn structure-durability
  "Get structure durability (0-1 scaled by tile size)."
  [^Structure s]
  (.-durability s))

(defn structure-construct-time
  "Get construction time (0-10000)."
  [^Structure s]
  (.-constructTime s))

(defn structure-resource
  "Get the RESOURCE required to build this structure.
   Returns nil if no resource required."
  [^Structure s]
  (.-resource s))

(defn structure-resource-amount
  "Get amount of resource required per unit (0-16)."
  [^Structure s]
  (.-resAmount s))

(defn structure-tint
  "Get building tint color."
  [^Structure s]
  (.-tint s))

(defn structure-minimap-color
  "Get minimap color."
  [^Structure s]
  (.-miniColor s))

;; ============================================
;; Color Conversion Helpers
;; ============================================

(defn color->rgb
  "Convert a COLOR to RGB map."
  [color]
  (when color
    {:red (.red color)
     :green (.green color)
     :blue (.blue color)}))

;; ============================================
;; Data Conversion
;; ============================================

(defn structure->map
  "Convert Structure to Clojure map."
  [^Structure s]
  (let [res (structure-resource s)]
    {:key (structure-key s)
     :index (structure-index s)
     :name (structure-name s)
     :description (structure-desc s)
     :name-wall (structure-name-wall s)
     :name-ceiling (structure-name-ceiling s)
     :durability (structure-durability s)
     :construct-time (structure-construct-time s)
     :resource-key (when res (.key ^RESOURCE res))
     :resource-amount (structure-resource-amount s)
     :minimap-color (color->rgb (structure-minimap-color s))}))

;; ============================================
;; Batch Operations
;; ============================================

(defn all-structures-as-maps
  "Get all structures as Clojure maps."
  []
  (mapv structure->map (all-structures)))

(defn structures-with-resources
  "Get structures that require resources."
  []
  (filter structure-resource (all-structures)))

(defn structures-with-resources-as-maps
  "Get structures with resources as maps."
  []
  (mapv structure->map (structures-with-resources)))

;; ============================================
;; Query Functions
;; ============================================

(defn find-structures-by-resource
  "Find all structures that use a specific resource key."
  [resource-key]
  (filter #(= resource-key (when-let [r (structure-resource %)]
                             (.key ^RESOURCE r)))
          (all-structures)))

(defn structure-by-durability
  "Get structures sorted by durability (descending)."
  []
  (sort-by #(- (structure-durability %)) (all-structures)))

(defn structure-by-construct-time
  "Get structures sorted by construction time (ascending)."
  []
  (sort-by structure-construct-time (all-structures)))

(comment
  ;; === Usage Examples ===
  
  ;; Get all structures
  (all-structures)
  (structure-count)
  
  ;; Get specific structure
  (def stone (get-structure "_STONE"))
  (structure-name stone)
  (structure-desc stone)
  (structure-durability stone)
  
  ;; Get MUD structure
  (structure-name (mud-structure))
  
  ;; Convert to map
  (structure->map (get-structure "_STONE"))
  
  ;; Batch convert
  (take 3 (all-structures-as-maps))
  
  ;; Find structures using stone resource
  (find-structures-by-resource "_STONE")
  
  ;; Sort by durability
  (mapv structure-name (structure-by-durability))
  
  :rcf)

