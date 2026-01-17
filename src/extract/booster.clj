(ns extract.booster
  "Booster data extraction for wiki.
   
   This namespace extracts all boostable data and exports to EDN/JSON.
   Uses game.booster for accessing boostable data.
   
   Usage:
     (require '[extract.booster :as booster-extract])
     (booster-extract/extract-all \"output/wiki\")
   "
  (:require
   [game.booster :as bo]
   [extract.common :as common]
   [clojure.string :as str]
   [clojure.pprint]))

;; ============================================
;; Configuration
;; ============================================

(def ^:dynamic *output-dir* "output/wiki")

;; ============================================
;; Enhanced Data Extraction
;; ============================================

(defn extract-boostable-full
  "Extract full boostable data including computed fields."
  [boostable]
  (let [base (bo/boostable->map boostable)
        key (:key base)
        ;; Determine semantic category from key prefix
        category-type (cond
                        (str/starts-with? key "PHYSICS_") :physics
                        (str/starts-with? key "BATTLE_") :battle
                        (str/starts-with? key "BEHAVIOUR_") :behaviour
                        (str/starts-with? key "ACTIVITY_") :activity
                        (str/starts-with? key "CIVIC_") :civic
                        (str/starts-with? key "NOBLE_") :noble
                        (str/starts-with? key "ROOM_") :room
                        (str/starts-with? key "WORLD_") :world
                        (str/starts-with? key "RELIGION_") :religion
                        (str/starts-with? key "RATES_") :rates
                        (str/starts-with? key "START_") :start
                        :else :other)]
    (assoc base
           :semantic-category category-type
           :icon-path (str "sprites/boosters/" key "/icon.png"))))

(defn extract-all-boostables
  "Extract all boostables with full data."
  []
  (mapv extract-boostable-full (bo/all-boostables)))

(defn extract-category-full
  "Extract a category with all its boostables."
  [cat]
  (let [base (bo/category->map cat)]
    (assoc base
           :boostables (mapv extract-boostable-full (bo/category-boostables cat)))))

(defn extract-all-categories
  "Extract all categories with full boostable data."
  []
  (mapv extract-category-full (bo/all-collections)))

;; ============================================
;; Aggregate Data Structure
;; ============================================

(defn build-boosters-data
  "Build complete boosters data structure for wiki."
  []
  (let [all-bo (extract-all-boostables)
        by-semantic (->> all-bo
                         (group-by :semantic-category)
                         (map (fn [[k v]] [k (count v)]))
                         (into (sorted-map)))]
    {:version "1.0"
     :extracted-at (str (java.time.Instant/now))
     :summary {:total-boostables (bo/boostable-count)
               :total-categories (bo/collection-count)
               :by-category (->> (bo/all-categories-as-maps)
                                 (map (fn [c] [(:name c) (:boostable-count c)]))
                                 (into (sorted-map)))
               :by-semantic-category by-semantic
               :by-type {:settlement (count (filter #(contains? (:types %) :settlement) all-bo))
                         :world (count (filter #(contains? (:types %) :world) all-bo))
                         :other (count (filter #(not (or (contains? (:types %) :settlement)
                                                          (contains? (:types %) :world))) all-bo))}}
     :categories (extract-all-categories)
     :boostables all-bo}))

;; ============================================
;; Grouped Data Builders
;; ============================================

(defn build-physics-data
  "Build physics boostables data."
  []
  {:category "Physics"
   :description "Physical attributes affecting subjects"
   :boostables (mapv extract-boostable-full (bo/physics-boostables))})

(defn build-battle-data
  "Build battle boostables data."
  []
  {:category "Battle"
   :description "Combat-related stats"
   :boostables (mapv extract-boostable-full (bo/battle-boostables))})

(defn build-behaviour-data
  "Build behaviour boostables data."
  []
  {:category "Behaviour"
   :description "Behavioral traits and tendencies"
   :boostables (mapv extract-boostable-full (bo/behaviour-boostables))})

(defn build-civic-data
  "Build civic boostables data."
  []
  {:category "Civic"
   :description "Settlement management stats"
   :boostables (mapv extract-boostable-full (bo/civic-boostables))})

(defn build-activity-data
  "Build activity boostables data."
  []
  {:category "Activity"
   :description "Activity-related rates"
   :boostables (mapv extract-boostable-full (bo/activity-boostables))})

(defn build-noble-data
  "Build noble/personality boostables data."
  []
  {:category "Noble"
   :description "Personality traits for nobles"
   :boostables (mapv extract-boostable-full (bo/noble-boostables))})

(defn build-room-data
  "Build room/building boostables data."
  []
  {:category "Rooms"
   :description "Building production and efficiency boosts"
   :boostables (mapv extract-boostable-full (bo/room-boostables))})


;; ============================================
;; Individual Exports
;; ============================================

(defn extract-boosters-edn
  "Extract all boostables to EDN file."
  ([] (extract-boosters-edn (str *output-dir* "/data")))
  ([output-dir]
   (let [data (build-boosters-data)
         path (str output-dir "/boosters.edn")]
     (common/save-edn-pretty data path)
     data)))

(defn extract-categories-only
  "Extract just the category structure."
  ([] (extract-categories-only (str *output-dir* "/data")))
  ([output-dir]
   (let [data {:version "1.0"
               :extracted-at (str (java.time.Instant/now))
               :categories (bo/all-categories-as-maps)}
         path (str output-dir "/booster-categories.edn")]
     (common/save-edn-pretty data path)
     data)))

(defn extract-by-category
  "Extract boostables grouped by category to separate files."
  ([] (extract-by-category (str *output-dir* "/data/boosters")))
  ([output-dir]
   (common/ensure-dir output-dir)
   (let [exports [["physics.edn" (build-physics-data)]
                  ["battle.edn" (build-battle-data)]
                  ["behaviour.edn" (build-behaviour-data)]
                  ["civic.edn" (build-civic-data)]
                  ["activity.edn" (build-activity-data)]
                  ["noble.edn" (build-noble-data)]
                  ["rooms.edn" (build-room-data)]]]
     (doseq [[filename data] exports]
       (let [path (str output-dir "/" filename)]
         (common/save-edn-pretty data path)))
     (println "Exported" (count exports) "category files to" output-dir))))

;; ============================================
;; Summary and Reports
;; ============================================

(defn extract-boosters-summary
  "Print summary of boostables."
  []
  (let [data (build-boosters-data)]
    (println "=== Boosters Summary ===")
    (println "Total boostables:" (:total-boostables (:summary data)))
    (println "Total categories:" (:total-categories (:summary data)))
    (println)
    (println "=== By Category ===")
    (doseq [[cat count] (:by-category (:summary data))]
      (println (str "  " cat ": " count)))
    (println)
    (println "=== By Semantic Category ===")
    (doseq [[cat count] (:by-semantic-category (:summary data))]
      (println (str "  " (name cat) ": " count)))
    (println)
    (println "=== By Type ===")
    (let [by-type (:by-type (:summary data))]
      (println (str "  Settlement: " (:settlement by-type)))
      (println (str "  World: " (:world by-type)))
      (println (str "  Other: " (:other by-type))))
    (println)
    (println "=== Sample Boostables ===")
    (doseq [b (take 10 (:boostables data))]
      (println (str "  " (:key b) " - " (:name b) 
                    " (base: " (:base-value b) ")")))
    (println "  ...")))

(defn category-report
  "Generate a report of all categories and their boostables."
  []
  (println "=== Boostable Category Report ===")
  (doseq [cat (bo/all-collections)]
    (println)
    (println (str "== " (bo/category-name cat) " =="))
    (println (str "   Prefix: " (bo/category-prefix cat)))
    (println (str "   Types: " (bo/type-mask->keywords (bo/category-type-mask cat))))
    (println (str "   Count: " (.size (bo/category-boostables cat))))
    (doseq [b (bo/category-boostables cat)]
      (println (str "     " (bo/boostable-key b) " - " (bo/boostable-name b))))))

;; ============================================
;; Main Extraction Functions
;; ============================================

(defn extract-all
  "Extract all booster data."
  ([] (extract-all *output-dir*))
  ([output-dir]
   (println "Extracting boosters to:" output-dir)
   (extract-boosters-edn (str output-dir "/data"))
   (extract-by-category (str output-dir "/data/boosters"))
   (println "Done!")))

(comment
  (extract-all)
  :rcf)

;; ============================================
;; Individual Queries
;; ============================================

(defn find-boostable
  "Find and display boostable info by key."
  [key]
  (if-let [b (bo/get-boostable key)]
    (bo/boostable->map b)
    (println "Boostable not found:" key)))

(defn find-by-name
  "Find boostables by partial name match (case-insensitive)."
  [name-part]
  (let [name-lower (str/lower-case name-part)]
    (->> (bo/all-boostables-as-maps)
         (filter #(str/includes? (str/lower-case (:name %)) name-lower)))))

(defn list-by-base-value
  "List boostables sorted by base value."
  [& {:keys [ascending] :or {ascending true}}]
  (let [sorter (if ascending < >)]
    (->> (bo/all-boostables-as-maps)
         (sort-by :base-value sorter))))

(defn list-physics
  "List all physics boostables."
  []
  (mapv bo/boostable->map (bo/physics-boostables)))

(defn list-battle
  "List all battle boostables."
  []
  (mapv bo/boostable->map (bo/battle-boostables)))

(defn list-behaviour
  "List all behaviour boostables."
  []
  (mapv bo/boostable->map (bo/behaviour-boostables)))

(defn list-civic
  "List all civic boostables."
  []
  (mapv bo/boostable->map (bo/civic-boostables)))

(defn list-rooms
  "List all room/building boostables."
  []
  (mapv bo/boostable->map (bo/room-boostables)))

(comment
  ;; === Quick Test ===
  
  ;; Check if game is loaded
  (bo/boostable-count)
  (bo/collection-count)
  
  ;; Extract summary
  (extract-boosters-summary)
  
  ;; Category report
  (category-report)
  
  ;; Extract to file
  (extract-boosters-edn)
  (extract-categories-only)
  (extract-by-category)
  
  ;; Find specific boostable
  (find-boostable "PHYSICS_SPEED")
  (find-boostable "BATTLE_MORALE")
  
  ;; Find by name
  (find-by-name "speed")
  (find-by-name "morale")
  
  ;; List by category
  (list-physics)
  (list-battle)
  (list-behaviour)
  (list-civic)
  
  ;; List sorted by value
  (take 10 (list-by-base-value))
  (take 10 (list-by-base-value :ascending false))
  
  ;; Full extraction
  (extract-all "output/wiki")
  
  :rcf)

