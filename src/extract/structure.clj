(ns extract.structure
  "Structure data extraction for wiki.
   
   This namespace extracts all structure (building material) data 
   and exports to EDN/JSON. Uses game.structure for accessing structure data.
   
   Usage:
     (require '[extract.structure :as struct-extract])
     (struct-extract/extract-all \"output/wiki\")
   "
  (:require
   [game.structure :as struct]
   [clojure.pprint])
  (:import
   [java.io File]))

;; ============================================
;; Configuration
;; ============================================

(def ^:dynamic *output-dir* "output/wiki")

;; ============================================
;; Enhanced Data Extraction
;; ============================================

(defn extract-structure-full
  "Extract full structure data including computed fields."
  [structure]
  (let [base (struct/structure->map structure)
        key (:key base)]
    (assoc base
           :has-resource (boolean (:resource-key base))
           :icon-path (str "sprites/structures/" key "/icon.png"))))

(defn extract-all-structures
  "Extract all structures with full data."
  []
  (mapv extract-structure-full (struct/all-structures)))

(defn extract-structures-with-resources
  "Extract only structures that require resources."
  []
  (mapv extract-structure-full (struct/structures-with-resources)))

;; ============================================
;; Grouping Functions
;; ============================================

(defn group-by-resource
  "Group structures by required resource."
  []
  (->> (extract-all-structures)
       (group-by :resource-key)
       (into (sorted-map))))

(defn structures-summary
  "Get summary statistics for structures."
  []
  (let [all (struct/all-structures)
        all-size (.size all)]
    {:total-structures all-size
     :with-resources (count (filter struct/structure-resource all))
     :unique-resources (->> all
                            (keep struct/structure-resource)
                            (map #(.key %))
                            set
                            count)
     :avg-durability (/ (reduce + (map struct/structure-durability all))
                        (max 1 all-size))
     :avg-construct-time (/ (reduce + (map struct/structure-construct-time all))
                           (max 1 all-size))}))

;; ============================================
;; Aggregate Data Structure
;; ============================================

(defn build-structures-data
  "Build complete structures data structure for wiki."
  []
  {:version "1.0"
   :extracted-at (str (java.time.Instant/now))
   :summary (structures-summary)
   :by-resource (group-by-resource)
   :structures (extract-all-structures)})

;; ============================================
;; File Output
;; ============================================

(defn ensure-dir
  "Ensure directory exists."
  [path]
  (let [dir (File. path)]
    (when-not (.exists dir)
      (.mkdirs dir))))

(defn save-edn
  "Save data as EDN file."
  [data path]
  (ensure-dir (.getParent (File. path)))
  (spit path (pr-str data))
  (println "Saved:" path))

(defn save-edn-pretty
  "Save data as pretty-printed EDN file."
  [data path]
  (ensure-dir (.getParent (File. path)))
  (spit path (with-out-str (clojure.pprint/pprint data)))
  (println "Saved:" path))

;; ============================================
;; Main Extraction Functions
;; ============================================

(defn extract-structures-edn
  "Extract structures data to EDN file."
  ([] (extract-structures-edn (str *output-dir* "/data")))
  ([output-dir]
   (let [data (build-structures-data)
         path (str output-dir "/structures.edn")]
     (save-edn-pretty data path)
     data)))

(defn extract-structures-summary
  "Print summary of structures."
  []
  (let [data (build-structures-data)
        summary (:summary data)]
    (println "=== Structures Summary ===")
    (println "Total structures:" (:total-structures summary))
    (println "With resources:" (:with-resources summary))
    (println "Unique resources:" (:unique-resources summary))
    (println (format "Avg durability: %.2f" (double (:avg-durability summary))))
    (println (format "Avg construct time: %.2f" (double (:avg-construct-time summary))))
    (println)
    (println "=== By Resource ===")
    (doseq [[res-key structs] (:by-resource data)]
      (when res-key
        (println (str "  " res-key ": " (count structs) " structures"))))
    (println)
    (println "=== Sample Structures ===")
    (doseq [s (take 5 (:structures data))]
      (println (str "  " (:key s) " - " (:name s) 
                    (when (:resource-key s)
                      (str " [" (:resource-key s) " x" (:resource-amount s) "]")))))
    (println "  ...")))

(defn extract-all
  "Extract all structure data."
  ([] (extract-all *output-dir*))
  ([output-dir]
   (println "Extracting structures to:" output-dir)
   (extract-structures-edn (str output-dir "/data"))
   (println "Done!")))

(comment
  (extract-all)
  :rcf)

;; ============================================
;; Individual Structure Queries
;; ============================================

(defn find-structure
  "Find and display structure info by key."
  [key]
  (if-let [s (struct/get-structure key)]
    (struct/structure->map s)
    (println "Structure not found:" key)))

(defn list-structures-by-resource
  "List structures grouped by resource."
  []
  (->> (struct/all-structures-as-maps)
       (group-by :resource-key)
       (into (sorted-map))))

(defn list-structures-by-durability
  "List structures sorted by durability (highest first)."
  []
  (->> (struct/all-structures-as-maps)
       (sort-by :durability >)))

(defn list-structures-by-construct-time
  "List structures sorted by construction time (fastest first)."
  []
  (->> (struct/all-structures-as-maps)
       (sort-by :construct-time)))

(defn compare-structures
  "Compare two structures by key."
  [key1 key2]
  (let [s1 (find-structure key1)
        s2 (find-structure key2)]
    (when (and s1 s2)
      {:structure-1 {:key key1
                     :name (:name s1)
                     :durability (:durability s1)
                     :construct-time (:construct-time s1)
                     :resource-cost (:resource-amount s1)}
       :structure-2 {:key key2
                     :name (:name s2)
                     :durability (:durability s2)
                     :construct-time (:construct-time s2)
                     :resource-cost (:resource-amount s2)}
       :comparison {:durability-diff (- (:durability s1) (:durability s2))
                    :time-diff (- (:construct-time s1) (:construct-time s2))}})))

(comment
  ;; === Quick Test ===
  
  ;; Check if game is loaded
  (struct/structure-count)
  
  ;; Extract summary
  (extract-structures-summary)
  
  ;; Extract to file
  (extract-structures-edn)
  
  ;; Find specific structure
  (find-structure "_STONE")
  (find-structure "_WOOD")
  (find-structure "_MUD")
  
  ;; List by resource
  (list-structures-by-resource)
  
  ;; List by durability
  (take 5 (list-structures-by-durability))
  
  ;; Compare two structures
  (compare-structures "_STONE" "_WOOD")
  
  ;; Full extraction
  (extract-all "output/wiki")
  
  :rcf)

