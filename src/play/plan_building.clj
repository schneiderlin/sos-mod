(ns play.plan-building
  "Farm and building site planning functions.
   Provides tools to find fertile land, check water access, and evaluate building locations.")

(require '[game.tile :as tile])
(require '[game.farm :as farm])
(require '[game.things :as things])
(import '[settlement.main SETT])

;; ============================================================================
;; Fertility and Water Detection Functions
;; ============================================================================
;; Note: get-fertility, has-water-access?, get-average-fertility, and
;; get-water-access-percentage are now in game.farm namespace

;; ============================================================================
;; Area Analysis Functions
;; ============================================================================

(defn area-is-clear?
  "Check if an area is clear of obstructions.
   Returns true if no entities, furniture items, or construction sites are present."
  [start-x start-y width height]
  (let [entities (tile/entities-in-area start-x start-y width height)
        furniture (tile/furniture-items-in-area start-x start-y width height)
        constructions (tile/constructions-in-area start-x start-y width height)]
    (and (empty? entities) (empty? furniture) (empty? constructions))))

(comment
  (area-is-clear? 276 356 18 18)
  (area-is-clear? 249 343 18 18)
  (area-is-clear? 273 347 5 5)

  :rcf)

(defn get-area-obstructions
  "Get a summary of what's blocking an area.
   Returns a map with :entity-count, :furniture-count, and :construction-count."
  [start-x start-y width height]
  (let [entities (tile/entities-in-area start-x start-y width height)
        furniture (tile/furniture-items-in-area start-x start-y width height)
        constructions (tile/constructions-in-area start-x start-y width height)]
    {:entity-count (count entities)
     :furniture-count (count furniture)
     :furniture-sample (take 5 furniture)
     :construction-count (count constructions)
     :construction-sample (take 5 constructions)
     :clear? (and (empty? entities) (empty? furniture) (empty? constructions))}))

;; ============================================================================
;; Farm Location Quality Evaluation
;; ============================================================================

(defn evaluate-farm-location
  "Evaluate a location for farm building.
   center-x, center-y: center tile coordinates
   width, height: dimensions of the farm

   Returns a map with:
   - :average-fertility - soil quality (higher is better)
   - :water-access-percentage - 0.0 to 1.0
   - :has-good-fertility - true if fertility >= 0.5
   - :has-water-access - true if >= 10% tiles have water
   - :is-clear - true if no obstructions
   - :recommended - true if location meets all criteria
   - :issues - list of any issues found"
  [center-x center-y width height]
  (let [start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))
        avg-fertility (farm/get-average-fertility start-x start-y width height)
        water-percentage (farm/get-water-access-percentage start-x start-y width height)
        obstructions (get-area-obstructions start-x start-y width height)
        has-good-fertility (>= avg-fertility 0.8)
        has-water-access (>= water-percentage 0.3)
        is-clear (:clear? obstructions)
        issues (concat
                 (when-not has-good-fertility ["Low fertility (< 0.8)"])
                 (when-not has-water-access ["Poor water access (< 30%)"])
                 (when-not is-clear ["Area has obstructions"]))]
    {:center-x center-x
     :center-y center-y
     :width width
     :height height
     :start-x start-x
     :start-y start-y
     :end-x (+ start-x width)
     :end-y (+ start-y height)
     :average-fertility avg-fertility
     :water-access-percentage water-percentage
     :has-good-fertility has-good-fertility
     :has-water-access has-water-access
     :is-clear is-clear
     :obstructions obstructions
     :recommended (and has-good-fertility has-water-access is-clear)
     :issues issues}))

(defn print-location-evaluation
  "Print a human-readable evaluation of a farm location."
  [center-x center-y width height]
  (let [eval (evaluate-farm-location center-x center-y width height)]
    (println "=== Farm Location Evaluation ===")
    (println "Center: (" (:center-x eval) ", " (:center-y eval) ")")
    (println "Area: " (:width eval) "x" (:height eval) "tiles")
    (println "Coordinates: x" (:start-x eval) "-" (:end-x eval) ", y" (:start-y eval) "-" (:end-y eval))
    (println)
    (println "Fertility:")
    (println "  Average:" (format "%.3f" (:average-fertility eval)))
    (println "  Good?" (if (:has-good-fertility eval) "✓ YES" "✗ NO"))
    (println)
    (println "Water Access:")
    (println "  Coverage:" (format "%.1f%%" (* 100 (:water-access-percentage eval))))
    (println "  Has water?" (if (:has-water-access eval) "✓ YES" "✗ NO"))
    (println)
    (println "Area Status:")
    (println "  Clear?" (if (:is-clear eval) "✓ YES" "✗ NO"))
    (when-not (:is-clear eval)
      (println "    Entities:" (:entity-count (:obstructions eval)))
      (println "    Furniture:" (:furniture-count (:obstructions eval))))
    (println)
    (println "Overall:")
    (println "  Recommended?" (if (:recommended eval) "✓ YES - Good location!" "✗ NO - Has issues"))
    (when (seq (:issues eval))
      (println "  Issues:")
      (doseq [issue (:issues eval)]
        (println "    -" issue)))
    eval))

;; ============================================================================
;; Farm Sizing Calculations
;; ============================================================================

(defn calculate-farm-size
  "Calculate the farm dimensions needed for a given number of farmers.
   Each farmer needs 8x8 tiles (64 tiles per farmer).

   Returns a map with :width, :height, and :total-tiles."
  [num-farmers & {:keys [tiles-per-farmer]
                  :or {tiles-per-farmer 64}}]
  (let [total-tiles (* num-farmers tiles-per-farmer)
        ;; Calculate dimensions (try to keep roughly square)
        side-length (int (Math/ceil (Math/sqrt total-tiles)))]
    {:num-farmers num-farmers
     :tiles-per-farmer tiles-per-farmer
     :total-tiles total-tiles
     :width side-length
     :height side-length}))

;; ============================================================================
;; Comments and Usage Examples
;; ============================================================================

(comment
  ;; Fertility and water functions (from game.farm)
  (farm/get-fertility 285 365)
  (farm/has-water-access? 285 365)
  (farm/get-average-fertility 276 356 18 18)
  (farm/get-water-access-percentage 276 356 18 18)

  ;; Check if area is clear
  (area-is-clear? 276 356 18 18)

  ;; Get obstructions in area
  (get-area-obstructions 276 356 18 18)

  ;; Evaluate a farm location
  (evaluate-farm-location 285 365 18 18)

  ;; Print detailed evaluation
  (print-location-evaluation 285 365 18 18)

  ;; Calculate farm size for 5 farmers
  (calculate-farm-size 5)

  :rcf)
