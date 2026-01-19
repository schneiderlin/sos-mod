(ns game.construction
  "Construction site management functions for Songs of Syx.

   This namespace provides functions for:
   - Finding construction sites
   - Canceling/removing constructions
   - Getting construction info
   - Managing construction state"
  (:import
   [settlement.main SETT]
   [java.lang.reflect Field]))

;; ============================================================================
;; Construction Lookup Functions
;; ============================================================================

;; Get the construction manager
(defn get-construction []
  (.construction (SETT/ROOMS)))

(comment
  (get-construction)
  :rcf)

;; Get the construction blueprint
(defn get-construction-blueprint []
  (let [construction (get-construction)
        bp-field (first (filter #(.contains (.getName %) "construction")
                                (.getDeclaredFields (.getClass construction))))]
    (.setAccessible bp-field true)
    (.get bp-field construction)))

(comment
  (get-construction-blueprint)
  :rcf)

;; Get construction instance at specific tile coordinates
;; Returns ConstructionInstance if found, nil otherwise
(defn construction-at [x y]
  (let [bp (get-construction-blueprint)]
    (try
      (.get bp x y)
      (catch Exception _ nil))))

(comment
  (construction-at 276 356)
  :rcf)

;; Get all construction instances
;; Note: This returns the count, not the actual instances
(defn construction-count []
  (.instances (get-construction)))

(comment
  (construction-count)
  :rcf)

;; Check if there's a construction at specific coordinates
(defn has-construction? [x y]
  (let [isser (.isser (get-construction))]
    (.is isser x y)))

(comment
  (has-construction? 276 356)
  :rcf)

;; Find all constructions in an area
;; Returns a list of [x y] coordinates where constructions exist
(defn find-constructions-in-area [start-x start-y width height]
  (let [isser (.isser (get-construction))
        found (atom [])]
    (doseq [y (range height)
            x (range width)]
      (let [tx (+ start-x x)
            ty (+ start-y y)]
        (when (.is isser tx ty)
          (swap! found conj [tx ty]))))
    @found))

(comment
  (find-constructions-in-area 270 350 30 30)
  :rcf)

;; Get a unique construction instance from an area
;; Returns the first ConstructionInstance found, or nil if none exist
(defn get-construction-in-area [start-x start-y width height]
  (let [locations (find-constructions-in-area start-x start-y width height)]
    (when (seq locations)
      (let [[x y] (first locations)]
        (construction-at x y)))))

(comment
  (get-construction-in-area 270 350 30 30)
  :rcf)

;; ============================================================================
;; Construction Info Functions
;; ============================================================================

;; Get construction instance info as a map
(defn construction-info [instance]
  (when instance
    (try
      (let [constructor (.constructor instance)
            base-info {:instance-class (.getSimpleName (.getClass instance))
                       :mX (.mX instance)
                       :mY (.mY instance)
                       :constructor-class (.getSimpleName (.getClass constructor))}
            blue (.blueprint constructor)]
        (assoc base-info
               :blueprint-class (.getSimpleName (.getClass blue))
               :blueprint-key (.key blue)
               :blueprint-name (.name (.info blue))))
      (catch Exception _
        {:instance-class (.getSimpleName (.getClass instance))
         :mX (.mX instance)
         :mY (.mY instance)
         :constructor-info "Unable to get full info"})))))

(comment
  (def ci (construction-at 276 356))
  (construction-info ci)
  :rcf)

;; Get construction instance type info
;; Returns a map with :type (e.g., "FARM") and :subtype (e.g., "FARM_VEG")
(defn construction-type-info [instance]
  (when instance
    (try
      (let [constructor (.constructor instance)
             blue (.blueprint constructor)]
        {:type (.getSimpleName (.getClass blue))
         :key (.key blue)
         :name (.name (.info blue))})
      (catch Exception _ nil))))

(comment
  (def ci (construction-at 276 356))
  (construction-type-info ci)
  :rcf)

;; ============================================================================
;; Construction Cancellation Functions
;; ============================================================================

;; Cancel a construction at specific tile coordinates
;; Parameters:
;;   x, y: Tile coordinates (can be any tile within the construction)
;;   return-tmp-area: If true, returns the TmpArea (default: false)
;;   context-object: Optional context object (default: nil)
;;   force: If true, forces removal (default: false)
;;
;; Returns: The TmpArea if return-tmp-area is true, nil otherwise
;;
;; Example:
;;   (cancel-construction 276 356)
;;   (cancel-construction 276 356 false nil true)
(defn cancel-construction
  ([x y]
   (cancel-construction x y false nil false))
  ([x y return-tmp-area context-object force]
   (let [instance (construction-at x y)]
     (when instance
       (.remove instance x y return-tmp-area context-object force)))))

(comment
  ;; Cancel construction at specific tile
  (cancel-construction 276 356)

  ;; Cancel with force flag
  (cancel-construction 276 356 false nil true)

  :rcf)

;; Cancel a construction by instance
(defn cancel-construction-instance
  ([instance]
   (cancel-construction-instance instance false nil false))
  ([instance return-tmp-area context-object force]
   (when instance
     (.remove instance (.mX instance) (.mY instance) return-tmp-area context-object force))))

(comment
  (def ci (construction-at 276 356))
  (cancel-construction-instance ci)
  :rcf)

;; Cancel all constructions in an area
;; Returns the number of constructions canceled
(defn cancel-constructions-in-area [start-x start-y width height]
  (let [locations (find-constructions-in-area start-x start-y width height)]
    (doseq [[x y] locations]
      (cancel-construction x y))
    (count locations)))

(comment
  ;; Cancel all constructions in a 30x30 area
  (cancel-constructions-in-area 270 350 30 30)
  :rcf)

;; ============================================================================
;; Utility Functions
;; ============================================================================

;; Print all construction locations in an area with their types
(defn print-constructions-in-area [start-x start-y width height]
  (let [isser (.isser (get-construction))]
    (doseq [y (range height)
            x (range width)]
      (let [tx (+ start-x x)
            ty (+ start-y y)]
        (when (.is isser tx ty)
          (if-let [ci (construction-at tx ty)]
            (if-let [info (construction-type-info ci)]
              (println (format "Construction at (%d,%d): %s - %s"
                              tx ty (:key info) (:name info)))
              (println (format "Construction at (%d,%d): Unknown type" tx ty)))
            (println (format "Construction at (%d,%d): Unable to get info" tx ty))))))))

(comment
  (print-constructions-in-area 270 350 30 30)
  :rcf)

;; Scan the entire map for constructions (expensive!)
;; Returns a list of all construction coordinates found
(defn scan-all-constructions
  ([] (scan-all-constructions 0 0 1000 1000))
  ([start-x start-y width height]
   (find-constructions-in-area start-x start-y width height)))

(comment
  ;; Scan for all constructions (default 1000x1000)
  (scan-all-constructions)

  ;; Scan a specific area
  (scan-all-constructions 200 200 100 100)
  :rcf)

;; ============================================================================
;; Examples and Usage Patterns
;; ============================================================================

(comment
  ;; === Common Usage Patterns ===

  ;; 1. Find and inspect a construction
  (def ci (construction-at 276 356))
  (construction-info ci)
  (construction-type-info ci)

  ;; 2. Check if there's a construction before building
  (when (has-construction? 276 356)
    (println "Found construction at target location!")
    (cancel-construction 276 356))

  ;; 3. Scan an area and cancel all constructions
  (print-constructions-in-area 270 350 30 30)
  (cancel-constructions-in-area 270 350 30 30)

  ;; 4. Find the first construction in an area and get its type
  (if-let [ci (get-construction-in-area 270 350 30 30)]
    (construction-type-info ci)
    (println "No constructions found"))

  ;; 5. Cancel a specific farm type
  (doseq [[x y] (find-constructions-in-area 270 350 30 30)]
    (if-let [ci (construction-at x y)]
      (if-let [info (construction-type-info ci)]
        (when (= (:key info) "FARM_COTTON")
          (println "Canceling cotton farm at" x y)
          (cancel-construction x y)))))

  :rcf)
