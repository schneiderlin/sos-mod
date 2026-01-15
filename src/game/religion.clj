(ns game.religion
  "Religion access functions for Songs of Syx.
   Religions represent different belief systems in the game.
   
   This namespace provides reusable functions for:
   - Accessing religion registry
   - Getting religion properties
   - Querying religion relationships (opposition)"
  (:import
   [init.religion RELIGIONS Religion]
   [snake2d.util.color COLOR]))

;; ============================================
;; Religion Registry Access
;; ============================================

(defn all-religions
  "Get all religions in the game.
   Returns: LIST<Religion>"
  []
  (RELIGIONS/ALL))

(defn religion-count
  "Get total number of religions."
  []
  (.size (all-religions)))

(defn religion-map
  "Get the religion RMAP for key-based lookups."
  []
  (RELIGIONS/MAP))

(defn get-religion
  "Get a religion by key.
   Returns nil if not found."
  [key]
  (when-let [rmap (religion-map)]
    (.get rmap key nil)))

;; ============================================
;; Religion Properties
;; ============================================

(defn religion-key
  "Get religion unique key (e.g., \"CRATOR\")."
  [^Religion rel]
  (.-key rel))

(defn religion-index
  "Get religion index in ALL list."
  [^Religion rel]
  (.index rel))

(defn religion-name
  "Get religion display name."
  [^Religion rel]
  (str (.name (.-info rel))))

(defn religion-desc
  "Get religion description."
  [^Religion rel]
  (str (.desc (.-info rel))))

(defn religion-deity
  "Get religion deity name."
  [^Religion rel]
  (str (.-diety rel)))

(defn religion-color
  "Get religion color."
  [^Religion rel]
  (.-color rel))

(defn religion-icon
  "Get religion icon."
  [^Religion rel]
  (.-icon rel))

(defn religion-inclination
  "Get religion default spread/inclination value (0-1)."
  [^Religion rel]
  (.-inclination rel))

(defn religion-conversion-boostable
  "Get the Boostable for city conversion."
  [^Religion rel]
  (.-conversionCity rel))

(defn religion-boosts
  "Get the BoostSpecs for this religion."
  [^Religion rel]
  (.-boosts rel))

;; ============================================
;; Religion Opposition
;; ============================================

(defn religion-opposition
  "Get the opposition value between two religions.
   Returns a value between 0-1 where higher means more opposition."
  [^Religion rel1 ^Religion rel2]
  (.opposition rel1 rel2))

(defn all-oppositions
  "Get opposition matrix for a religion.
   Returns a map of {other-religion-key -> opposition-value}."
  [^Religion rel]
  (into {}
        (for [other (all-religions)]
          [(religion-key other) (religion-opposition rel other)])))

;; ============================================
;; Color Conversion
;; ============================================

(defn color->hex
  "Convert COLOR to hex string."
  [^COLOR color]
  (let [rgb (.toIntRGB color)]
    (format "#%06X" (bit-and rgb 0xFFFFFF))))

(defn color->map
  "Convert COLOR to RGB map."
  [^COLOR color]
  {:red (.red color)
   :green (.green color)
   :blue (.blue color)
   :hex (color->hex color)})

;; ============================================
;; Boost Extraction
;; ============================================

(defn boost-spec->map
  "Convert a BoostSpec to a Clojure map."
  [spec]
  {:boostable-key (str (.key (.-boostable spec)))
   :boostable-name (str (.name (.-boostable spec)))
   :is-mul (.-isMul (.-booster spec))
   :from (.from (.-booster spec))
   :to (.to (.-booster spec))})

(defn religion-boosts-as-maps
  "Get all boost specs for a religion as Clojure maps."
  [^Religion rel]
  (let [boosts (religion-boosts rel)]
    (when boosts
      (mapv boost-spec->map (.all boosts)))))

;; ============================================
;; Data Conversion
;; ============================================

(defn religion->map
  "Convert Religion to Clojure map."
  [^Religion rel]
  {:key (religion-key rel)
   :index (religion-index rel)
   :name (religion-name rel)
   :description (religion-desc rel)
   :deity (religion-deity rel)
   :inclination (religion-inclination rel)
   :color (color->map (religion-color rel))
   :oppositions (all-oppositions rel)
   :boosts (religion-boosts-as-maps rel)})

(defn religion->map-basic
  "Convert Religion to basic Clojure map (without boosts)."
  [^Religion rel]
  {:key (religion-key rel)
   :index (religion-index rel)
   :name (religion-name rel)
   :description (religion-desc rel)
   :deity (religion-deity rel)
   :inclination (religion-inclination rel)
   :color (color->map (religion-color rel))})

;; ============================================
;; Batch Operations
;; ============================================

(defn all-religions-as-maps
  "Get all religions as Clojure maps."
  []
  (mapv religion->map (all-religions)))

(defn all-religions-basic
  "Get all religions as basic Clojure maps (without boosts)."
  []
  (mapv religion->map-basic (all-religions)))

;; ============================================
;; Queries
;; ============================================

(defn religions-by-inclination
  "Get religions sorted by default spread/inclination."
  []
  (sort-by religion-inclination > (all-religions)))

(defn get-opposition-matrix
  "Get full opposition matrix as a 2D map.
   Returns {religion1-key {religion2-key opposition-value}}"
  []
  (into {}
        (for [rel (all-religions)]
          [(religion-key rel) (all-oppositions rel)])))

(defn find-most-opposed
  "Find the religion most opposed to the given religion."
  [^Religion rel]
  (let [others (filter #(not= (religion-key %) (religion-key rel)) (all-religions))]
    (when (seq others)
      (apply max-key #(religion-opposition rel %) others))))

(defn find-least-opposed
  "Find the religion least opposed to the given religion (excluding itself)."
  [^Religion rel]
  (let [others (filter #(not= (religion-key %) (religion-key rel)) (all-religions))]
    (when (seq others)
      (apply min-key #(religion-opposition rel %) others))))

(comment
  ;; === Usage Examples ===
  
  ;; Get all religions
  (all-religions)
  (religion-count)
  
  ;; Get first religion info
  (let [rel (first (all-religions))]
    (println "Key:" (religion-key rel))
    (println "Name:" (religion-name rel))
    (println "Deity:" (religion-deity rel))
    (println "Inclination:" (religion-inclination rel)))
  
  ;; Get opposition matrix
  (get-opposition-matrix)
  
  ;; Convert to map
  (religion->map (first (all-religions)))
  
  ;; Batch convert
  (take 2 (all-religions-as-maps))
  
  ;; Find most opposed
  (let [rel (first (all-religions))]
    (religion-key (find-most-opposed rel)))
  
  :rcf)

