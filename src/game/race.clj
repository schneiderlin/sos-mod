(ns game.race
  "Race access functions for Songs of Syx.
   Races are the different species that can populate your settlement.
   
   This namespace provides reusable functions for:
   - Accessing race registry
   - Getting race properties (info, physics, population)
   - Querying race preferences and boosts"
  (:import
   [init.race RACES Race RaceInfo RaceStats RacePopulation Physics RaceBoosts]
   [init.race.bio Bio BioLine]
   [init.race.appearence RAppearence]
   [init.type CLIMATES CLIMATE TERRAINS TERRAIN]
   [game.boosting BoostSpec BoostSpecs]
   [snake2d.util.color COLOR]))

;; ============================================
;; Race Registry Access
;; ============================================

(defn all-races
  "Get all races in the game.
   Returns: LIST<Race>"
  []
  (RACES/all))

(defn race-count
  "Get total number of races."
  []
  (.size (all-races)))

(defn playable-races
  "Get all playable races.
   Returns: LIST<Race>"
  []
  (RACES/playable))

(defn playable-count
  "Get number of playable races."
  []
  (.size (playable-races)))

(defn get-race
  "Get a race by key (e.g., \"HUMAN\", \"DONDORIAN\").
   Returns nil if not found."
  [key]
  (.get (RACES/map) key nil))

(defn race-boosts-registry
  "Get the global race boosts registry.
   Returns: RaceBoosts"
  []
  (RACES/boosts))

;; ============================================
;; Race Basic Properties
;; ============================================

(defn race-key
  "Get race unique key (e.g., \"HUMAN\")."
  [^Race race]
  (.key race))

(defn race-index
  "Get race index in all() list."
  [^Race race]
  (.index race))

(defn race-playable?
  "Check if race is playable."
  [^Race race]
  (.-playable race))

;; ============================================
;; Race Info Properties
;; ============================================

(defn race-info
  "Get RaceInfo for a race."
  [^Race race]
  (.-info race))

(defn race-name
  "Get race display name."
  [^Race race]
  (str (.name (race-info race))))

(defn race-names
  "Get race plural name."
  [^Race race]
  (str (.names (race-info race))))

(defn race-desc
  "Get race short description."
  [^Race race]
  (str (.desc (race-info race))))

(defn race-desc-long
  "Get race long description."
  [^Race race]
  (str (.-desc_long (race-info race))))

(defn race-name-posessive
  "Get race possessive form (e.g., \"Human's\")."
  [^Race race]
  (str (.-namePosessive (race-info race))))

(defn race-name-posessives
  "Get race plural possessive form."
  [^Race race]
  (str (.-namePosessives (race-info race))))

(defn race-challenge
  "Get initial challenge description for race."
  [^Race race]
  (str (.-initialChallenge (race-info race))))

(defn race-pros
  "Get list of race advantages."
  [^Race race]
  (vec (.-pros (race-info race))))

(defn race-cons
  "Get list of race disadvantages."
  [^Race race]
  (vec (.-cons (race-info race))))

(defn race-army-names
  "Get list of army name options."
  [^Race race]
  (vec (.-armyNames (race-info race))))

;; ============================================
;; Race Physics Properties
;; ============================================

(defn race-physics
  "Get Physics for a race."
  [^Race race]
  (.-physics race))

(defn race-height
  "Get race height over ground."
  [^Race race]
  (.height (race-physics race)))

(defn race-hitbox-size
  "Get race hitbox size."
  [^Race race]
  (.hitBoxsize (race-physics race)))

(defn race-adult-at
  "Get day when race becomes adult."
  [^Race race]
  (.-adultAt (race-physics race)))

(defn race-corpse-decays?
  "Check if race corpse decays."
  [^Race race]
  (.-decays (race-physics race)))

(defn race-sleeps?
  "Check if race sleeps."
  [^Race race]
  (.-sleeps (race-physics race)))

(defn race-slave-price
  "Get race slave price."
  [^Race race]
  (.-slaveprice (race-physics race)))

(defn race-raiding-value
  "Get race raiding mercenary value."
  [^Race race]
  (.-raiding (race-physics race)))

;; ============================================
;; Race Population Properties
;; ============================================

(defn race-population
  "Get RacePopulation for a race."
  [^Race race]
  (.population race))

(defn race-pop-growth
  "Get race population growth rate."
  [^Race race]
  (.-growth (race-population race)))

(defn race-pop-max
  "Get race maximum population fraction."
  [^Race race]
  (.-max (race-population race)))

(defn race-immigration-rate
  "Get race immigration rate per day."
  [^Race race]
  (.-immigrantsPerDay (race-population race)))

(defn race-climate-preference
  "Get race preference for a specific climate."
  [^Race race ^CLIMATE climate]
  (.climate (race-population race) climate))

(defn race-terrain-preference
  "Get race preference for a specific terrain."
  [^Race race ^TERRAIN terrain]
  (.terrain (race-population race) terrain))

(defn race-climate-preferences
  "Get all climate preferences as a map."
  [^Race race]
  (let [pop (race-population race)]
    (into {}
          (for [climate (CLIMATES/ALL)]
            [(str (.key climate)) (.climate pop climate)]))))

(defn race-terrain-preferences
  "Get all terrain preferences as a map."
  [^Race race]
  (let [pop (race-population race)]
    (into {}
          (for [terrain (TERRAINS/ALL)]
            [(str (.key terrain)) (.terrain pop terrain)]))))

;; ============================================
;; Race Bio Properties
;; ============================================

(defn race-bio
  "Get Bio for a race."
  [^Race race]
  (.bio race))

(defn race-bio-lines
  "Get bio description lines."
  [^Race race]
  (vec (.lines (race-bio race))))

;; ============================================
;; Race Appearance Properties
;; ============================================

(defn race-appearance
  "Get RAppearence for a race."
  [^Race race]
  (.appearance race))

(defn race-types-count
  "Get number of appearance types (e.g., gender variants)."
  [^Race race]
  (.size (.-types (race-appearance race))))

;; ============================================
;; Race Boosts Properties
;; ============================================

(defn race-boosts
  "Get BoostSpecs for a race."
  [^Race race]
  (.-boosts race))

(defn race-boost-specs
  "Get all boost specs for a race as a sequence."
  [^Race race]
  (seq (.all (race-boosts race))))

;; ============================================
;; Race Preference Properties  
;; ============================================

(defn race-preferences
  "Get RacePreferrence for a race."
  [^Race race]
  (.pref race))

(defn race-preferred-foods
  "Get list of preferred food resources."
  [^Race race]
  (vec (.-food (race-preferences race))))

(defn race-preferred-drinks
  "Get list of preferred drink resources."
  [^Race race]
  (vec (.-drink (race-preferences race))))

(defn race-most-hated
  "Get the most hated other race."
  [^Race race]
  (.-mostHated (race-preferences race)))

(defn race-relation
  "Get relation value with another race (0-1)."
  [^Race race ^Race other-race]
  (.race (race-preferences race) other-race))

(defn race-relations
  "Get all race relations as a map."
  [^Race race]
  (let [pref (race-preferences race)]
    (into {}
          (for [other (all-races)]
            [(race-key other) (.race pref other)]))))

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
;; Data Conversion
;; ============================================

(defn boost-spec->map
  "Convert BoostSpec to Clojure map."
  [^BoostSpec spec]
  {:boostable-key (str (.-key (.-boostable spec)))
   :boostable-name (str (.name (.-boostable spec)))
   :is-mul (.-isMul (.-booster spec))
   :from (.from (.-booster spec))
   :to (.to (.-booster spec))})

(defn physics->map
  "Convert Physics to Clojure map."
  [^Race race]
  {:height (race-height race)
   :hitbox-size (race-hitbox-size race)
   :adult-at-day (race-adult-at race)
   :corpse-decays (race-corpse-decays? race)
   :sleeps (race-sleeps? race)
   :slave-price (race-slave-price race)
   :raiding-value (race-raiding-value race)})

(defn population->map
  "Convert Population to Clojure map."
  [^Race race]
  {:growth (race-pop-growth race)
   :max (race-pop-max race)
   :immigration-rate (race-immigration-rate race)
   :climate-preferences (race-climate-preferences race)
   :terrain-preferences (race-terrain-preferences race)})

(defn info->map
  "Convert RaceInfo to Clojure map."
  [^Race race]
  {:name (race-name race)
   :names (race-names race)
   :description (race-desc race)
   :description-long (race-desc-long race)
   :possessive (race-name-posessive race)
   :possessives (race-name-posessives race)
   :challenge (race-challenge race)
   :pros (race-pros race)
   :cons (race-cons race)})

(defn preferences->map
  "Convert race preferences to Clojure map."
  [^Race race]
  (let [pref (race-preferences race)]
    {:preferred-foods (mapv #(str (.key (.-resource %))) (race-preferred-foods race))
     :preferred-drinks (mapv #(str (.key (.-resource %))) (race-preferred-drinks race))
     :most-hated-race (when-let [hated (race-most-hated race)]
                        (race-key hated))
     :race-relations (race-relations race)}))

(defn race->map
  "Convert Race to Clojure map with core data."
  [^Race race]
  {:key (race-key race)
   :index (race-index race)
   :playable (race-playable? race)
   :info (info->map race)
   :physics (physics->map race)
   :population (population->map race)})

(defn race->map-full
  "Convert Race to full Clojure map with all data."
  [^Race race]
  (merge (race->map race)
         {:preferences (preferences->map race)
          :boosts (mapv boost-spec->map (race-boost-specs race))
          :appearance-types (race-types-count race)}))

;; ============================================
;; Batch Operations
;; ============================================

(defn all-races-as-maps
  "Get all races as Clojure maps."
  []
  (mapv race->map (all-races)))

(defn all-races-full
  "Get all races with full data as Clojure maps."
  []
  (mapv race->map-full (all-races)))

(defn playable-races-as-maps
  "Get all playable races as Clojure maps."
  []
  (mapv race->map (playable-races)))

;; ============================================
;; Queries
;; ============================================

(defn races-by-playability
  "Get races grouped by playability."
  []
  {:playable (mapv race-key (playable-races))
   :non-playable (mapv race-key (filter #(not (race-playable? %)) (all-races)))})

(defn races-sorted-by-slave-price
  "Get races sorted by slave price (ascending)."
  []
  (sort-by race-slave-price (all-races)))

(defn find-race-by-name
  "Find race by display name (case-insensitive)."
  [name]
  (let [name-lower (clojure.string/lower-case name)]
    (first (filter #(= (clojure.string/lower-case (race-name %)) name-lower)
                   (all-races)))))

(comment
  ;; === Usage Examples ===
  
  ;; Get all races
  (all-races)
  (race-count)
  
  ;; Get playable races
  (playable-races)
  (playable-count)
  
  ;; Get specific race
  (def human (get-race "HUMAN"))
  (race-name human)
  (race-desc human)
  (race-playable? human)
  
  ;; Get physics
  (race-height human)
  (race-adult-at human)
  (race-sleeps? human)
  
  ;; Get population data
  (race-pop-growth human)
  (race-immigration-rate human)
  (race-climate-preferences human)
  
  ;; Get preferences
  (race-preferred-foods human)
  (race-relations human)
  
  ;; Convert to map
  (race->map human)
  (race->map-full human)
  
  ;; Batch convert
  (take 2 (all-races-as-maps))
  
  ;; Queries
  (races-by-playability)
  
  :rcf)

