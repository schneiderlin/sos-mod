(ns game.building.validation
  "Building location validation for Songs of Syx.

   This namespace provides functions for:
   - Checking if an area is clear of obstructions
   - Getting detailed information about what's blocking an area
   - Validating building locations before construction

   Used BEFORE building to prevent crashes on occupied areas."

  (:require
   [game.tile :as tile]))

;; ============================================================================
;; Area Clearance Check
;; ============================================================================

(defn area-is-clear?
  "Check if an area is completely clear of obstructions.

   Returns true only if the area has NO:
   - Entities (people, animals, etc.)
   - Furniture items
   - Construction sites

   Parameters:
   - start-x: Top-left X coordinate
   - start-y: Top-left Y coordinate
   - width: Width in tiles
   - height: Height in tiles

   Returns: Boolean (true = completely clear, false = has obstructions)"
  [start-x start-y width height]
  (let [entities (tile/entities-in-area start-x start-y width height)
        furniture (tile/furniture-items-in-area start-x start-y width height)
        constructions (tile/constructions-in-area start-x start-y width height)]
    (and (empty? entities) (empty? furniture) (empty? constructions))))

(comment
  (area-is-clear? 100 100 5 5)
  (area-is-clear? 250 250 10 10)
  :rcf)

(defn area-is-clear-centered?
  "Check if area is clear using center coordinates (more intuitive).

   Calculates top-left from center and checks clearance.

   Parameters:
   - center-x: Center X coordinate
   - center-y: Center Y coordinate
   - width: Width in tiles
   - height: Height in tiles

   Returns: Boolean (true = completely clear)"
  [center-x center-y width height]
  (let [start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))]
    (area-is-clear? start-x start-y width height)))

(comment
  (area-is-clear-centered? 150 150 5 5)
  (area-is-clear-centered? 200 200 10 10)
  :rcf)

;; ============================================================================
;; Obstruction Details
;; ============================================================================

(defn get-area-obstructions
  "Get detailed information about what's blocking an area.

   Returns a map with counts and sample items for each obstruction type.

   Parameters:
   - start-x: Top-left X coordinate
   - start-y: Top-left Y coordinate
   - width: Width in tiles
   - height: Height in tiles

   Returns: Map with:
   - :entity-count - Number of entities
   - :furniture-count - Number of furniture items
   - :construction-count - Number of construction sites
   - :entity-sample - Up to 5 entity coordinates
   - :furniture-sample - Up to 5 furniture coordinates
   - :construction-sample - Up to 5 construction coordinates
   - :clear? - Boolean, true if all counts are 0"
  [start-x start-y width height]
  (let [entities (tile/entities-in-area start-x start-y width height)
        furniture (tile/furniture-items-in-area start-x start-y width height)
        constructions (tile/constructions-in-area start-x start-y width height)]
    {:entity-count (count entities)
     :furniture-count (count furniture)
     :construction-count (count constructions)
     :entity-sample (take 5 entities)
     :furniture-sample (take 5 furniture)
     :construction-sample (take 5 constructions)
     :clear? (and (empty? entities) (empty? furniture) (empty? constructions))}))

(comment
  (get-area-obstructions 100 100 5 5)
  (get-area-obstructions 250 250 10 10)
  :rcf)

;; ============================================================================
;; Building Location Validation
;; ============================================================================

(defn validate-building-location
  "Validate that a location is clear for building.

   Throws an exception with descriptive error message if the area is not clear.
   This should be called BEFORE creating any TmpArea.

   Parameters:
   - center-x: Center X coordinate
   - center-y: Center Y coordinate
   - width: Width in tiles
   - height: Height in tiles
   - building-name: String name for error message (e.g., 'well', 'home')

   Throws: Exception if area is not clear"
  [center-x center-y width height building-name]
  (let [start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))
        obstructions (get-area-obstructions start-x start-y width height)]
    
    (when-not (:clear? obstructions)
      (let [issues (concat
                      (when (> (:entity-count obstructions) 0)
                        [(str (:entity-count obstructions) " entities present")])
                      (when (> (:furniture-count obstructions) 0)
                        [(str (:furniture-count obstructions) " furniture items present")])
                      (when (> (:construction-count obstructions) 0)
                        [(str (:construction-count obstructions) " construction sites present")]))
            issue-str (clojure.string/join ", " issues)] 
        (throw (Exception. (str "Cannot build " building-name 
                                       " at (" center-x "," center-y 
                                       "): " issue-str ". "
                                       "Use get-area-obstructions to see details.")))))))

(comment
  (validate-building-location 150 150 5 5 "well")
  (validate-building-location 200 200 10 10 "home")
  :rcf)

(defn validate-building-location-or-nil
  "Validate building location, returning nil if clear.

   Same as validate-building-location but returns nil instead of throwing
   when the area is clear. Useful for conditional logic.

   Parameters:
   - center-x: Center X coordinate
   - center-y: Center Y coordinate
   - width: Width in tiles
   - height: Height in tiles
   - building-name: String name for error message

   Returns: nil if clear, throws exception if not clear"
  [center-x center-y width height building-name]
  (try
    (validate-building-location center-x center-y width height building-name)
    nil
    (catch Exception e
      e)))

(comment
  (validate-building-location-or-nil 150 150 5 5 "well")
  (validate-building-location-or-nil 200 200 10 10 "home")
  :rcf)

;; ============================================================================
;; Area Search Utilities
;; ============================================================================

(defn find-clear-area-in-region
  "Search for a clear area within a region using grid pattern.

   Scans a region from top-left to bottom-right looking for a clear
   area of specified dimensions.

   Parameters:
   - start-x: Region top-left X coordinate
   - start-y: Region top-left Y coordinate
   - region-width: Region width in tiles
   - region-height: Region height in tiles
   - building-width: Required building width
   - building-height: Required building height
   - spacing: Grid spacing between check points (default 5)

   Returns: Map with:
   - :found - Boolean, true if clear area found
   - :center-x - X coordinate of clear area center
   - :center-y - Y coordinate of clear area center
   - :checked - Total positions checked"
  [start-x start-y region-width region-height building-width building-height 
   & {:keys [spacing] :or {spacing 5}}]
  (let [half-w (quot building-width 2)
        half-h (quot building-height 2)
        
        ;; Calculate search grid with spacing
        end-x (+ start-x region-width building-width)
        end-y (+ start-y region-height building-height)
        
        ;; Search in grid pattern
        results (atom nil)]
    
    (loop [sx start-x
           sy start-y
           checked 0]
      (when (<= sy (- end-y half-h))
        (loop [cx sx
               checked-idx checked]
          (when (<= cx (- end-x half-w))
            (let [center-x (+ cx half-w)
                  center-y (+ sy half-h)
                  clear? (area-is-clear-centered? center-x center-y building-width building-height)]
              
              (when (and clear? (nil? @results))
                (reset! results {:found true
                                 :center-x center-x
                                 :center-y center-y
                                 :checked (+ checked-idx 1)}))
              
              (if (nil? @results)
                ;; Continue search
                (recur (+ cx spacing) (+ checked-idx 1))
                ;; Found or reached end of row
                checked-idx)))))
      
      ;; Return result or not-found with count
      (if @results
        @results
        {:found false
         :checked checked}))))

(comment
  ;; Search for a 3x3 clear area in a 50x50 region
  (find-clear-area-in-region 100 100 50 50 3 3)
  
  ;; Search with custom spacing
  (find-clear-area-in-region 100 100 50 50 5 5 :spacing 10)
  :rcf)

;; ============================================================================
;; Print Utilities
;; ============================================================================

(defn print-area-status
  "Print a human-readable status of an area's clearance.

   Useful for debugging and understanding why a location is blocked.

   Parameters:
   - center-x: Center X coordinate
   - center-y: Center Y coordinate
   - width: Width in tiles
   - height: Height in tiles

   Returns: The obstructions map for further inspection"
  [center-x center-y width height]
  (let [start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))
        obstructions (get-area-obstructions start-x start-y width height)]
    
    (println "=== Area Clearance Status ===")
    (println "Center: (" center-x "," center-y ")")
    (println "Area: " width "x" height " tiles")
    (println "Coordinates: x" start-x "-" (+ start-x width) ", y" start-y "-" (+ start-y height))
    (println)
    
    (when (:clear? obstructions)
      (println "Status: ✓ CLEAR - Ready for building")
      (do
        (println "Entities: 0")
        (println "Furniture: 0")
        (println "Constructions: 0"))
      (do
        (println "Status: ✗ NOT CLEAR - Cannot build here")
        (when (> (:entity-count obstructions) 0)
          (println "Entities:" (:entity-count obstructions))
          (println "  Sample:" (:entity-sample obstructions)))
        (when (> (:furniture-count obstructions) 0)
          (println "Furniture:" (:furniture-count obstructions))
          (println "  Sample:" (:furniture-sample obstructions)))
        (when (> (:construction-count obstructions) 0)
          (println "Constructions:" (:construction-count obstructions))
          (println "  Sample:" (:construction-sample obstructions)))))
    
    (println)
    obstructions))

(comment
  (print-area-status 150 150 5 5)
  (print-area-status 200 200 10 10)
  :rcf)

;; ============================================================================
;; Complete Example Usage
;; ============================================================================

(comment
  "=== Example: Validate and inspect a building location ===

   1. Check if area is clear
   2. Get detailed obstructions
   3. Print status for debugging"

  (require '[game.building.core :as core])

  ;; Simple check
  (println "Is (100,100) clear for 5x5?" (area-is-clear? 100 100 5 5))
  
  ;; Check with center coordinates
  (println "Is (150,150) clear for 5x5?" (area-is-clear-centered? 150 150 5 5))
  
  ;; Get obstructions
  (println "Obstructions at (200,200):" (get-area-obstructions 200 200 10 10))
  
  ;; Print detailed status
  (print-area-status 250 250 10 10)
  
  ;; Validate (throws if not clear)
  (println "Validating (150,150)...")
  (validate-building-location 150 150 5 5 "well")
  
  ;; Validate (returns nil if clear)
  (println "Validating (300,300)...")
  (if (validate-building-location-or-nil 300 300 5 5 "home")
    (println "Location is clear!")
    (println "Location is NOT clear!"))
  
  ;; Search for clear area in region
  (println "Searching for 3x3 clear area...")
  

  :done)
