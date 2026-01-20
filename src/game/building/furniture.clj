(ns game.building.furniture
  "Furniture placement operations for all building types in Songs of Syx.

   This namespace provides functions for:
   - Getting furniture items with validation
   - Placing single or multiple furniture items
   - Extracting room layout information from furniture

   All furniture placement is done BEFORE createClean() is called."

  (:import
   [settlement.main SETT]
   [settlement.room.main.furnisher FurnisherItem]))

;; ============================================================================
;; Furniture Item Retrieval with Validation
;; ============================================================================

(defn get-furniture-item-safe
  "Get furniture item from Furnisher with full validation.

   This function wraps the unsafe (.item pgroups group variation rotation)
   call with proper error handling and descriptive error messages.

   Parameters:
   - constructor: The Furnisher/constructor from room blueprint
   - group-index: Index of furniture group (e.g., 0, 1, 2)
   - variation: Size variation (e.g., 0, 1, 2)
   - rotation: Rotation (0-3)

   Returns: FurnisherItem instance

   Throws: Exception with descriptive message if furniture cannot be retrieved"
  [constructor group-index variation rotation]
  (let [pgroups (.pgroups constructor)
        num-groups (when pgroups (.size pgroups))]

    ;; Validate constructor has groups
    (when-not pgroups
      (throw (Exception. "Constructor has no pgroups (null)")))

    (when-not num-groups
      (throw (Exception. (str "pgroups has size 0")))
    
    ;; Validate group index
    (when-not (< group-index num-groups)
      (throw (Exception. (str "Furniture group index " group-index 
                                   " out of range (0-" (dec num-groups) "). "
                                   "Available groups: " num-groups)))

    ;; Get group
    (let [group (.get pgroups group-index)]
      (when-not group
        (throw (Exception. (str "Furniture group at index " group-index " is null")))
      
      ;; Get furniture item
      (let [item (.item group variation rotation)]
        (when-not item
          (throw (Exception. (str "Furniture item is nil: "
                                         "group=" group-index 
                                         " variation=" variation 
                                         " rotation=" rotation 
                                         ". Check if these values are valid for this room type.")))
        item))))))))

(comment
  
  :rcf)

;; ============================================================================
;; Single Furniture Placement
;; ============================================================================

(defn place-furniture-once
  "Place a single furniture item at specific coordinates.

   CRITICAL: Furniture must be placed BEFORE .createClean() is called.

   Parameters:
   - x: Tile X coordinate
   - y: Tile Y coordinate
   - furnisher-item: FurnisherItem instance to place
   - room-instance: The room instance from tmp.room()

   Throws: Exception if placement fails (wraps Java exceptions)"
  [x y furnisher-item room-instance]
  (let [fdata (.fData (SETT/ROOMS))]
    (try
      (.itemSet fdata x y furnisher-item room-instance)
      (catch Exception e
        (throw (Exception. (str "Failed to place furniture at (" x "," y "): "
                                       (.getMessage e))))))))

;; ============================================================================
;; Multiple Furniture Placement
;; ============================================================================

(defn place-furniture-items
  "Place multiple furniture items at their respective coordinates.

   Iterates over a sequence of [x y item] tuples and places each.

   Parameters:
   - furniture-items: Sequence of [x y furnisher-item] tuples
   - room-instance: The room instance from tmp.room()

   Throws: Exception if any placement fails (stops at first failure)"
  [furniture-items room-instance]
  (doseq [[x y item] furniture-items]
    (place-furniture-once x y item room-instance)))

(comment
  
  :rcf)

;; ============================================================================
;; Furniture Layout Extraction
;; ============================================================================

(defn furniture-dimensions
  "Get dimensions of a furniture item.

   Parameters:
   - furnisher-item: FurnisherItem instance

   Returns: Map with :width, :height, :area"
  [furnisher-item]
  {:width (.width furnisher-item)
   :height (.height furnisher-item)
   :area (* (.width furnisher-item) (.height furnisher-item))})

(comment
  :rcf)

(defn calculate-furniture-center
  "Calculate the center coordinates for a furniture item.

   When furniture is placed, it's typically centered at a specific location.
   This function calculates where the furniture's top-left should be to achieve
   that center.

   Parameters:
   - center-x: Desired center X coordinate
   - center-y: Desired center Y coordinate
   - furnisher-item: FurnisherItem instance

   Returns: [x y] top-left coordinates for placement"
  [center-x center-y furnisher-item]
  (let [dims (furniture-dimensions furnisher-item)
        half-width (quot (:width dims) 2)
        half-height (quot (:height dims) 2)]
    [(- center-x half-width) (- center-y half-height)]))

(comment
  
  :rcf)

(defn collect-room-tiles-from-furniture
  "Collect all tile coordinates from a furniture item layout.

   Iterates over the furniture item's grid and returns a set
   of [x y] coordinates that have content.

   Parameters:
   - furnisher-item: FurnisherItem instance
   - start-x: Top-left X coordinate where furniture is placed
   - start-y: Top-left Y coordinate where furniture is placed

   Returns: Set of [x y] coordinates for tiles with furniture content"
  [furnisher-item start-x start-y]
  (let [width (.width furnisher-item)
        height (.height furnisher-item)
        tiles (atom #{})]
    (doseq [y (range height)
            x (range width)]
      (when-let [tile (.get furnisher-item x y)]
        (swap! tiles conj [(+ start-x x) (+ start-y y)])))
    @tiles))

(comment
  
  :rcf)

(defn extract-entrance-tiles
  "Extract entrance tiles from furniture layout.

   Entrance tiles are those marked with noWalls=true in the furniture grid.

   Parameters:
   - furnisher-item: FurnisherItem instance
   - start-x: Top-left X coordinate
   - start-y: Top-left Y coordinate

   Returns: Set of [x y] coordinates that are entrance tiles"
  [furnisher-item start-x start-y]
  (let [width (.width furnisher-item)
        height (.height furnisher-item)
        tiles (atom #{})]
    (doseq [y (range height)
            x (range width)]
      (when-let [tile (.get furnisher-item x y)]
        (when (.-noWalls tile)
          (swap! tiles conj [(+ start-x x) (+ start-y y)]))))
    @tiles))

(comment
  
  :rcf)

;; ============================================================================
;; Utility Functions
;; ============================================================================

(defn furniture-area-size
  "Get total area covered by a furniture item.

   Parameters:
   - furnisher-item: FurnisherItem instance

   Returns: Number of tiles covered (width * height)"
  [furnisher-item]
  (* (.width furnisher-item) (.height furnisher-item)))

(comment
  
  :rcf)

(defn furniture-occupancy-map
  "Create a map of furniture items to their occupied tile coordinates.

   Useful for calculating wall placement and door locations.

   Parameters:
   - furniture-items: Map of {item-key [[x y] coordinates]}

   Returns: Map of {item-key (set of [x y])}"
  [furniture-items]
  (into {}
        (map (fn [[item-key coords]]
               [item-key (set coords)])
             furniture-items)))

(comment
  
  :rcf)

;; ============================================================================
;; Complete Example Usage
;; ============================================================================

(comment
  "=== Example: Placing multiple furniture items ===

   1. Get constructor
   2. Get furniture items with validation
   3. Calculate placement positions
   4. Place all items"

  (require '[game.building.core :as core])

  (def rooms (SETT/ROOMS))
  
  :done)
