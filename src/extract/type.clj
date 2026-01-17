(ns extract.type
  "Type and enum data extraction for wiki.
   
   This namespace extracts all type/enum data and exports to EDN/JSON.
   Uses game.type for accessing type data.
   
   Types extracted:
   - Terrains (OCEAN, WET, MOUNTAIN, FOREST, NONE)
   - Climates (COLD, TEMPERATE, HOT)
   - Diseases (various illnesses)
   - Traits (character traits)
   - Needs (service needs like HUNGER, THIRST)
   - Humanoid Classes (NOBLE, CITIZEN, SLAVE, CHILD, OTHER)
   - Humanoid Types (SUBJECT, RETIREE, RECRUIT, STUDENT, etc.)
   
   Usage:
     (require '[extract.type :as type-extract])
     (type-extract/extract-all \"output/wiki\")
   "
  (:require
   [game.type :as typ]
   [extract.common :as common]
   [clojure.string]))

;; ============================================
;; Configuration
;; ============================================

(def ^:dynamic *output-dir* "output/wiki")


;; ============================================
;; Terrain Extraction
;; ============================================

(defn extract-terrains
  "Extract all terrain data."
  []
  {:count (typ/terrain-count)
   :terrains (typ/all-terrains-as-maps)})

(defn extract-terrain-summary
  "Print terrain summary."
  []
  (println "=== Terrains ===")
  (println "Count:" (typ/terrain-count))
  (doseq [t (typ/all-terrains-as-maps)]
    (println (str "  " (:key t) " - " (:name t)))))

;; ============================================
;; Climate Extraction
;; ============================================

(defn extract-climates
  "Extract all climate data."
  []
  {:count (typ/climate-count)
   :climates (typ/all-climates-as-maps)})

(defn extract-climate-summary
  "Print climate summary."
  []
  (println "=== Climates ===")
  (println "Count:" (typ/climate-count))
  (doseq [c (typ/all-climates-as-maps)]
    (println (str "  " (:key c) " - " (:name c) 
                  " (temp: " (:temp-cold c) "-" (:temp-warm c) ")"))))

;; ============================================
;; Disease Extraction
;; ============================================

(defn extract-diseases
  "Extract all disease data."
  []
  {:count (typ/disease-count)
   :regular-interval-days (typ/regular-disease-days)
   :diseases (typ/all-diseases-as-maps)
   :epidemic-diseases (->> (typ/all-diseases-as-maps)
                           (filter :epidemic)
                           (mapv :key))
   :regular-diseases (->> (typ/all-diseases-as-maps)
                          (filter :regular)
                          (mapv :key))})

(defn extract-disease-summary
  "Print disease summary."
  []
  (println "=== Diseases ===")
  (println "Count:" (typ/disease-count))
  (println "Regular interval:" (typ/regular-disease-days) "days")
  (doseq [d (typ/all-diseases-as-maps)]
    (println (str "  " (:key d) " - " (:name d)
                  (when (:epidemic d) " [EPIDEMIC]")
                  (when (:regular d) " [REGULAR]")
                  " (fatality: " (int (* (:fatality-rate d) 100)) "%)"))))

;; ============================================
;; Trait Extraction
;; ============================================

(defn extract-traits
  "Extract all trait data."
  []
  {:count (typ/trait-count)
   :traits (typ/all-traits-as-maps)})

(defn extract-trait-summary
  "Print trait summary."
  []
  (println "=== Traits ===")
  (println "Count:" (typ/trait-count))
  (doseq [t (typ/all-traits-as-maps)]
    (println (str "  " (:key t) " - " (:name t)
                  (when (seq (:disables t))
                    (str " (disables: " (clojure.string/join ", " (:disables t)) ")"))))))

;; ============================================
;; Need Extraction
;; ============================================

(defn extract-needs
  "Extract all need data."
  []
  {:count (typ/need-count)
   :needs (typ/all-needs-as-maps)
   :basic-needs (->> (typ/all-needs-as-maps)
                     (filter :basic)
                     (mapv :key))})

(defn extract-need-summary
  "Print need summary."
  []
  (println "=== Needs ===")
  (println "Count:" (typ/need-count))
  (doseq [n (typ/all-needs-as-maps)]
    (println (str "  " (:key n) " - " (:name n)
                  (when (:basic n) " [BASIC]")))))

;; ============================================
;; Humanoid Class Extraction
;; ============================================

(defn extract-hclasses
  "Extract all humanoid class data."
  []
  {:count (typ/hclass-count)
   :hclasses (typ/all-hclasses-as-maps)
   :player-classes (->> (typ/all-hclasses-as-maps)
                        (filter :player)
                        (mapv :key))})

(defn extract-hclass-summary
  "Print humanoid class summary."
  []
  (println "=== Humanoid Classes ===")
  (println "Count:" (typ/hclass-count))
  (doseq [c (typ/all-hclasses-as-maps)]
    (println (str "  " (:key c) " - " (:name c)
                  (when (:player c) " [PLAYER]")))))

;; ============================================
;; Humanoid Type Extraction
;; ============================================

(defn extract-htypes
  "Extract all humanoid type data."
  []
  {:count (typ/htype-count)
   :htypes (typ/all-htypes-as-maps)
   :player-types (->> (typ/all-htypes-as-maps)
                      (filter :player)
                      (mapv :key))
   :worker-types (->> (typ/all-htypes-as-maps)
                      (filter :works)
                      (mapv :key))
   :hostile-types (->> (typ/all-htypes-as-maps)
                       (filter :hostile)
                       (mapv :key))})

(defn extract-htype-summary
  "Print humanoid type summary."
  []
  (println "=== Humanoid Types ===")
  (println "Count:" (typ/htype-count))
  (doseq [t (typ/all-htypes-as-maps)]
    (println (str "  " (:key t) " - " (:name t)
                  " [" (:class-key t) "]"
                  (when (:player t) " PLAYER")
                  (when (:works t) " WORKS")
                  (when (:hostile t) " HOSTILE")))))

;; ============================================
;; Aggregate Data Structure
;; ============================================

(defn build-types-data
  "Build complete types data structure for wiki."
  []
  {:version "1.0"
   :extracted-at (str (java.time.Instant/now))
   :summary (typ/types-summary)
   :terrains (extract-terrains)
   :climates (extract-climates)
   :diseases (extract-diseases)
   :traits (extract-traits)
   :needs (extract-needs)
   :hclasses (extract-hclasses)
   :htypes (extract-htypes)})

;; ============================================
;; Main Extraction Functions
;; ============================================

(defn extract-types-edn
  "Extract types data to EDN file."
  ([] (extract-types-edn (str *output-dir* "/data")))
  ([output-dir]
   (let [data (build-types-data)
         path (str output-dir "/types.edn")]
     (common/save-edn-pretty data path)
     data)))

(defn extract-types-summary
  "Print summary of all types."
  []
  (let [summary (typ/types-summary)]
    (println "=== Types & Enums Summary ===")
    (println "Terrains:" (:terrains summary))
    (println "Climates:" (:climates summary))
    (println "Diseases:" (:diseases summary))
    (println "Traits:" (:traits summary))
    (println "Needs:" (:needs summary))
    (println "Humanoid Classes:" (:hclasses summary))
    (println "Humanoid Types:" (:htypes summary))
    (println)
    (extract-terrain-summary)
    (println)
    (extract-climate-summary)
    (println)
    (extract-disease-summary)
    (println)
    (extract-trait-summary)
    (println)
    (extract-need-summary)
    (println)
    (extract-hclass-summary)
    (println)
    (extract-htype-summary)))

(defn extract-all
  "Extract all type data."
  ([] (extract-all *output-dir*))
  ([output-dir]
   (println "Extracting types to:" output-dir)
   (extract-types-edn (str output-dir "/data"))
   (println "Done!")))

(comment
  (extract-all)
  :rcf)

;; ============================================
;; Individual Type Queries
;; ============================================

(defn find-terrain
  "Find and display terrain info by key."
  [key]
  (first (filter #(= (:key %) key) (typ/all-terrains-as-maps))))

(defn find-climate
  "Find and display climate info by key."
  [key]
  (first (filter #(= (:key %) key) (typ/all-climates-as-maps))))

(defn find-disease
  "Find and display disease info by key."
  [key]
  (first (filter #(= (:key %) key) (typ/all-diseases-as-maps))))

(defn find-trait
  "Find and display trait info by key."
  [key]
  (first (filter #(= (:key %) key) (typ/all-traits-as-maps))))

(defn find-need
  "Find and display need info by key."
  [key]
  (first (filter #(= (:key %) key) (typ/all-needs-as-maps))))

(defn find-hclass
  "Find and display humanoid class info by key."
  [key]
  (first (filter #(= (:key %) key) (typ/all-hclasses-as-maps))))

(defn find-htype
  "Find and display humanoid type info by key."
  [key]
  (first (filter #(= (:key %) key) (typ/all-htypes-as-maps))))

;; ============================================
;; Grouped Queries
;; ============================================

(defn list-diseases-by-type
  "List diseases grouped by type (epidemic/regular)."
  []
  (let [diseases (typ/all-diseases-as-maps)]
    {:epidemic (filter :epidemic diseases)
     :regular (filter :regular diseases)
     :neither (filter #(and (not (:epidemic %)) (not (:regular %))) diseases)}))

(defn list-htypes-by-class
  "List humanoid types grouped by class."
  []
  (->> (typ/all-htypes-as-maps)
       (group-by :class-key)
       (into (sorted-map))))

(defn list-htypes-by-behavior
  "List humanoid types grouped by behavior."
  []
  {:player (filter :player (typ/all-htypes-as-maps))
   :worker (filter :works (typ/all-htypes-as-maps))
   :hostile (filter :hostile (typ/all-htypes-as-maps))
   :other (filter #(and (not (:player %)) (not (:hostile %))) (typ/all-htypes-as-maps))})

(comment
  ;; === Quick Test ===
  
  ;; Check type counts
  (typ/types-summary)
  
  ;; Extract summary
  (extract-types-summary)
  
  ;; Extract to file
  (extract-types-edn)
  
  ;; Find specific types
  (find-terrain "OCEAN")
  (find-climate "COLD")
  (find-disease "PLAGUE") 
  (find-trait "STRONG")
  (find-need "_HUNGER")
  (find-hclass "NOBLE")
  (find-htype "CITIZEN")
  
  ;; List grouped
  (list-diseases-by-type)
  (list-htypes-by-class)
  (list-htypes-by-behavior)
  
  ;; Full extraction
  (extract-all "output/wiki")
  
  :rcf)

