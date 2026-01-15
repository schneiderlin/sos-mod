(ns game.type
  "Type and enum access functions for Songs of Syx.
   Types include terrains, climates, diseases, traits, needs, and humanoid classes/types.
   
   This namespace provides reusable functions for:
   - Accessing type registries
   - Getting type properties
   - Converting types to Clojure maps"
  (:import
   [init.type TERRAINS TERRAIN CLIMATES CLIMATE DISEASES DISEASE 
    TRAITS TRAIT NEEDS NEED HCLASSES HCLASS HTYPES HTYPE]))

;; ============================================
;; TERRAINS - Geographic terrain types
;; ============================================

(defn all-terrains
  "Get all terrain types in the game.
   Returns: LIST<TERRAIN>"
  []
  (TERRAINS/ALL))

(defn terrain-count
  "Get total number of terrain types."
  []
  (.size (all-terrains)))

(defn terrain-map
  "Get terrain registry map."
  []
  (TERRAINS/MAP))

(defn terrain-ocean
  "Get the Ocean terrain type."
  []
  (TERRAINS/OCEAN))

(defn terrain-wet
  "Get the Fresh Water terrain type."
  []
  (TERRAINS/WET))

(defn terrain-mountain
  "Get the Mountain terrain type."
  []
  (TERRAINS/MOUNTAIN))

(defn terrain-forest
  "Get the Forest terrain type."
  []
  (TERRAINS/FOREST))

(defn terrain-none
  "Get the Open Land terrain type."
  []
  (TERRAINS/NONE))

;; TERRAIN Properties
(defn terrain-key
  "Get terrain unique key."
  [^TERRAIN t]
  (.key t))

(defn terrain-index
  "Get terrain index."
  [^TERRAIN t]
  (.index t))

(defn terrain-name
  "Get terrain display name."
  [^TERRAIN t]
  (str (.name t)))

(defn terrain-desc
  "Get terrain description."
  [^TERRAIN t]
  (str (.desc t)))

(defn terrain-world?
  "Check if terrain applies to world map."
  [^TERRAIN t]
  (.-world t))

(defn terrain->map
  "Convert TERRAIN to Clojure map."
  [^TERRAIN t]
  {:key (terrain-key t)
   :index (terrain-index t)
   :name (terrain-name t)
   :description (terrain-desc t)
   :world (terrain-world? t)})

(defn all-terrains-as-maps
  "Get all terrains as Clojure maps."
  []
  (mapv terrain->map (all-terrains)))

;; ============================================
;; CLIMATES - Climate zones
;; ============================================

(defn all-climates
  "Get all climate types in the game.
   Returns: LIST<CLIMATE>"
  []
  (CLIMATES/ALL))

(defn climate-count
  "Get total number of climate types."
  []
  (.size (all-climates)))

(defn climate-map
  "Get climate registry map."
  []
  (CLIMATES/MAP))

(defn climate-cold
  "Get the Cold climate type."
  []
  (CLIMATES/COLD))

(defn climate-temperate
  "Get the Temperate climate type."
  []
  (CLIMATES/TEMP))

(defn climate-hot
  "Get the Hot climate type."
  []
  (CLIMATES/HOT))

;; CLIMATE Properties
(defn climate-key
  "Get climate unique key."
  [^CLIMATE c]
  (.key c))

(defn climate-index
  "Get climate index."
  [^CLIMATE c]
  (.index c))

(defn climate-name
  "Get climate display name."
  [^CLIMATE c]
  (str (.name c)))

(defn climate-desc
  "Get climate description."
  [^CLIMATE c]
  (str (.desc c)))

(defn climate-season-change
  "Get climate seasonal change factor (0-1)."
  [^CLIMATE c]
  (.-seasonChange c))

(defn climate-temp-cold
  "Get climate cold temperature."
  [^CLIMATE c]
  (.-tempCold c))

(defn climate-temp-warm
  "Get climate warm temperature."
  [^CLIMATE c]
  (.-tempWarm c))

(defn climate-fertility
  "Get climate fertility factor."
  [^CLIMATE c]
  (.-fertility c))

(defn climate->map
  "Convert CLIMATE to Clojure map."
  [^CLIMATE c]
  {:key (climate-key c)
   :index (climate-index c)
   :name (climate-name c)
   :description (climate-desc c)
   :season-change (climate-season-change c)
   :temp-cold (climate-temp-cold c)
   :temp-warm (climate-temp-warm c)
   :fertility (climate-fertility c)})

(defn all-climates-as-maps
  "Get all climates as Clojure maps."
  []
  (mapv climate->map (all-climates)))

;; ============================================
;; DISEASES - Disease types
;; ============================================

(defn all-diseases
  "Get all disease types in the game.
   Returns: LIST<DISEASE>"
  []
  (DISEASES/all))

(defn disease-count
  "Get total number of disease types."
  []
  (.size (all-diseases)))

(defn disease-map
  "Get disease registry map."
  []
  (DISEASES/map))

(defn regular-disease-days
  "Get interval for regular disease occurrence."
  []
  (DISEASES/regularDays))

;; DISEASE Properties
(defn disease-key
  "Get disease unique key."
  [^DISEASE d]
  (.key d))

(defn disease-index
  "Get disease index."
  [^DISEASE d]
  (.index d))

(defn disease-name
  "Get disease display name."
  [^DISEASE d]
  (str (.-name (.-info d))))

(defn disease-desc
  "Get disease description."
  [^DISEASE d]
  (str (.-desc (.-info d))))

(defn disease-infect-rate
  "Get disease infection/spread rate (0-1)."
  [^DISEASE d]
  (.-infectRate d))

(defn disease-incubation-days
  "Get disease incubation period in days."
  [^DISEASE d]
  (.-incubationDays d))

(defn disease-fatality-rate
  "Get disease fatality rate (0-1)."
  [^DISEASE d]
  (.-fatalityRate d))

(defn disease-length
  "Get disease duration in days."
  [^DISEASE d]
  (.-length d))

(defn disease-epidemic?
  "Check if disease can cause epidemics."
  [^DISEASE d]
  (.-epidemic d))

(defn disease-regular?
  "Check if disease occurs regularly."
  [^DISEASE d]
  (.-regular d))

(defn disease->map
  "Convert DISEASE to Clojure map."
  [^DISEASE d]
  {:key (disease-key d)
   :index (disease-index d)
   :name (disease-name d)
   :description (disease-desc d)
   :infect-rate (disease-infect-rate d)
   :incubation-days (disease-incubation-days d)
   :fatality-rate (disease-fatality-rate d)
   :length (disease-length d)
   :epidemic (disease-epidemic? d)
   :regular (disease-regular? d)})

(defn all-diseases-as-maps
  "Get all diseases as Clojure maps."
  []
  (mapv disease->map (all-diseases)))

;; ============================================
;; TRAITS - Character traits
;; ============================================

(defn all-traits
  "Get all trait types in the game.
   Returns: LIST<TRAIT>"
  []
  (TRAITS/ALL))

(defn trait-count
  "Get total number of trait types."
  []
  (.size (all-traits)))

(defn trait-map
  "Get trait registry map."
  []
  (TRAITS/MAP))

;; TRAIT Properties
(defn trait-key
  "Get trait unique key."
  [^TRAIT t]
  (.key t))

(defn trait-index
  "Get trait index."
  [^TRAIT t]
  (.index t))

(defn trait-name
  "Get trait display name."
  [^TRAIT t]
  (str (.-name (.-info t))))

(defn trait-desc
  "Get trait description."
  [^TRAIT t]
  (str (.-desc (.-info t))))

(defn trait-title
  "Get trait role title."
  [^TRAIT t]
  (str (.-rTitle t)))

(defn trait-bios
  "Get trait biography descriptions."
  [^TRAIT t]
  (mapv str (.-bios t)))

(defn trait-disables
  "Get list of traits disabled by this trait."
  [^TRAIT t]
  (mapv trait-key (.disables t)))

(defn trait->map
  "Convert TRAIT to Clojure map."
  [^TRAIT t]
  {:key (trait-key t)
   :index (trait-index t)
   :name (trait-name t)
   :description (trait-desc t)
   :title (trait-title t)
   :bios (trait-bios t)
   :disables (trait-disables t)})

(defn all-traits-as-maps
  "Get all traits as Clojure maps."
  []
  (mapv trait->map (all-traits)))

;; ============================================
;; NEEDS - Service needs
;; ============================================

(defn all-needs
  "Get all need types in the game.
   Returns: LIST<NEED>"
  []
  (NEEDS/ALL))

(defn all-simple-needs
  "Get all simple (non-essential) needs.
   Returns: LIST<NEED>"
  []
  (NEEDS/ALLSIMPLE))

(defn need-count
  "Get total number of need types."
  []
  (.size (all-needs)))

(defn need-map
  "Get need registry map."
  []
  (NEEDS/MAP))

(defn need-types
  "Get special need types (HUNGER, THIRST, etc.)."
  []
  (NEEDS/TYPES))

;; NEED Properties
(defn need-key
  "Get need unique key."
  [^NEED n]
  (.key n))

(defn need-index
  "Get need index."
  [^NEED n]
  (.index n))

(defn need-name
  "Get need display name."
  [^NEED n]
  (str (.-nameNeed n)))

(defn need-basic?
  "Check if need is a basic need."
  [^NEED n]
  (.-basic n))

(defn need-event
  "Get need event multiplier."
  [^NEED n]
  (.-event n))

(defn need->map
  "Convert NEED to Clojure map."
  [^NEED n]
  {:key (need-key n)
   :index (need-index n)
   :name (need-name n)
   :basic (need-basic? n)
   :event (need-event n)})

(defn all-needs-as-maps
  "Get all needs as Clojure maps."
  []
  (mapv need->map (all-needs)))

;; ============================================
;; HCLASSES - Humanoid social classes
;; ============================================

(defn all-hclasses
  "Get all humanoid class types in the game.
   Returns: LIST<HCLASS>"
  []
  (HCLASSES/ALL))

(defn all-player-hclasses
  "Get all player-controllable humanoid classes.
   Returns: LIST<HCLASS>"
  []
  (HCLASSES/ALLP))

(defn hclass-count
  "Get total number of humanoid class types."
  []
  (.size (all-hclasses)))

(defn hclass-map
  "Get humanoid class registry map."
  []
  (HCLASSES/MAP))

(defn hclass-noble
  "Get the Noble class."
  []
  (HCLASSES/NOBLE))

(defn hclass-citizen
  "Get the Citizen/Plebeian class."
  []
  (HCLASSES/CITIZEN))

(defn hclass-slave
  "Get the Slave class."
  []
  (HCLASSES/SLAVE))

(defn hclass-child
  "Get the Child class."
  []
  (HCLASSES/CHILD))

(defn hclass-other
  "Get the Other class (non-player)."
  []
  (HCLASSES/OTHER))

;; HCLASS Properties
(defn hclass-key
  "Get humanoid class unique key."
  [^HCLASS c]
  (.key c))

(defn hclass-index
  "Get humanoid class index."
  [^HCLASS c]
  (.index c))

(defn hclass-name
  "Get humanoid class display name."
  [^HCLASS c]
  (str (.name c)))

(defn hclass-names
  "Get humanoid class plural name."
  [^HCLASS c]
  (str (.names c)))

(defn hclass-desc
  "Get humanoid class description."
  [^HCLASS c]
  (str (.desc c)))

(defn hclass-player?
  "Check if humanoid class is player-controllable."
  [^HCLASS c]
  (.-player c))

(defn hclass-player-index
  "Get player index for humanoid class (-1 if not player)."
  [^HCLASS c]
  (.-playerIndex c))

(defn hclass->map
  "Convert HCLASS to Clojure map."
  [^HCLASS c]
  {:key (hclass-key c)
   :index (hclass-index c)
   :name (hclass-name c)
   :names (hclass-names c)
   :description (hclass-desc c)
   :player (hclass-player? c)
   :player-index (hclass-player-index c)})

(defn all-hclasses-as-maps
  "Get all humanoid classes as Clojure maps."
  []
  (mapv hclass->map (all-hclasses)))

;; ============================================
;; HTYPES - Humanoid types (detailed)
;; ============================================

(defn all-htypes
  "Get all humanoid types in the game.
   Returns: LIST<HTYPE>"
  []
  (HTYPES/ALL))

(defn htype-count
  "Get total number of humanoid types."
  []
  (.size (all-htypes)))

(defn htype-map
  "Get humanoid type registry map."
  []
  (HTYPES/MAP))

;; Specific HTYPE accessors
(defn htype-subject
  "Get the Subject/Citizen type."
  []
  (HTYPES/SUBJECT))

(defn htype-retiree
  "Get the Retiree type."
  []
  (HTYPES/RETIREE))

(defn htype-recruit
  "Get the Recruit type."
  []
  (HTYPES/RECRUIT))

(defn htype-student
  "Get the Student type."
  []
  (HTYPES/STUDENT))

(defn htype-prisoner
  "Get the Prisoner type."
  []
  (HTYPES/PRISONER))

(defn htype-tourist
  "Get the Tourist type."
  []
  (HTYPES/TOURIST))

(defn htype-soldier
  "Get the Soldier type."
  []
  (HTYPES/SOLDIER))

(defn htype-enemy
  "Get the Enemy type."
  []
  (HTYPES/ENEMY))

(defn htype-rioter
  "Get the Rioter type."
  []
  (HTYPES/RIOTER))

(defn htype-deranged
  "Get the Deranged type."
  []
  (HTYPES/DERANGED))

(defn htype-nobility
  "Get the Nobility type."
  []
  (HTYPES/NOBILITY))

(defn htype-slave
  "Get the Slave type."
  []
  (HTYPES/SLAVE))

(defn htype-child
  "Get the Child type."
  []
  (HTYPES/CHILD))

;; HTYPE Properties
(defn htype-key
  "Get humanoid type unique key."
  [^HTYPE t]
  (.key t))

(defn htype-index
  "Get humanoid type index."
  [^HTYPE t]
  (.index t))

(defn htype-name
  "Get humanoid type display name."
  [^HTYPE t]
  (str (.name t)))

(defn htype-names
  "Get humanoid type plural name."
  [^HTYPE t]
  (str (.names t)))

(defn htype-desc
  "Get humanoid type description."
  [^HTYPE t]
  (str (.desc t)))

(defn htype-player?
  "Check if humanoid type belongs to player."
  [^HTYPE t]
  (.-player t))

(defn htype-works?
  "Check if humanoid type can work."
  [^HTYPE t]
  (.-works t))

(defn htype-hostile?
  "Check if humanoid type is hostile."
  [^HTYPE t]
  (.-hostile t))

(defn htype-visible?
  "Check if humanoid type is visible."
  [^HTYPE t]
  (.-visible t))

(defn htype-class
  "Get the HCLASS for this humanoid type."
  [^HTYPE t]
  (.-CLASS t))

(defn htype->map
  "Convert HTYPE to Clojure map."
  [^HTYPE t]
  {:key (htype-key t)
   :index (htype-index t)
   :name (htype-name t)
   :names (htype-names t)
   :description (htype-desc t)
   :player (htype-player? t)
   :works (htype-works? t)
   :hostile (htype-hostile? t)
   :visible (htype-visible? t)
   :class-key (hclass-key (htype-class t))})

(defn all-htypes-as-maps
  "Get all humanoid types as Clojure maps."
  []
  (mapv htype->map (all-htypes)))

;; ============================================
;; Summary Functions
;; ============================================

(defn types-summary
  "Get summary of all type counts."
  []
  {:terrains (terrain-count)
   :climates (climate-count)
   :diseases (disease-count)
   :traits (trait-count)
   :needs (need-count)
   :hclasses (hclass-count)
   :htypes (htype-count)})

(comment
  ;; === Usage Examples ===
  
  ;; Get all types
  (all-terrains)
  (all-climates)
  (all-diseases)
  (all-traits)
  (all-needs)
  (all-hclasses)
  (all-htypes)
  
  ;; Get counts
  (types-summary)
  
  ;; Get specific types
  (terrain-name (terrain-ocean))
  (climate-name (climate-cold))
  (htype-name (htype-citizen))
  
  ;; Convert to maps
  (terrain->map (terrain-ocean))
  (climate->map (climate-cold))
  (disease->map (first (all-diseases)))
  (trait->map (first (all-traits)))
  (need->map (first (all-needs)))
  (hclass->map (hclass-noble))
  (htype->map (htype-subject))
  
  ;; Batch convert
  (all-terrains-as-maps)
  (all-climates-as-maps)
  (all-diseases-as-maps)
  (all-traits-as-maps)
  (all-needs-as-maps)
  (all-hclasses-as-maps)
  (all-htypes-as-maps)
  
  :rcf)

