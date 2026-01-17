(ns extract.tech
  "Technology data extraction for wiki.
   
   This namespace extracts all technology data and exports to EDN/JSON.
   Uses game.tech for accessing technology data.
   
   Usage:
     (require '[extract.tech :as tech-extract])
     (tech-extract/extract-all \"output/wiki\")
   "
  (:require
   [game.tech :as tech]
   [extract.common :as common]
   [clojure.string]))

;; ============================================
;; Configuration
;; ============================================

(def ^:dynamic *output-dir* "output/wiki")

;; ============================================
;; Enhanced Data Extraction
;; ============================================

(defn extract-tech-full
  "Extract full tech data including computed fields."
  [t]
  (let [base (tech/tech->map t)
        key (:key base)]
    (assoc base
           :icon-path (str "sprites/techs/" key "/icon.png"))))

(defn extract-all-techs
  "Extract all technologies with full data."
  []
  (mapv extract-tech-full (tech/all-techs)))

(defn extract-tree-full
  "Extract a tree with all its technologies."
  [tree]
  (let [base (tech/tree->map-full tree)]
    (assoc base
           :techs (mapv extract-tech-full (tech/tree-techs tree)))))

(defn extract-all-trees
  "Extract all tech trees with full data."
  []
  (mapv extract-tree-full (tech/all-trees)))

;; ============================================
;; Aggregate Data Structure
;; ============================================

(defn build-technologies-data
  "Build complete technologies data structure for wiki."
  []
  {:version "1.0"
   :extracted-at (str (java.time.Instant/now))
   :summary {:total-techs (tech/tech-count)
             :total-trees (tech/tree-count)
             :total-currencies (tech/cost-types-count)
             :root-techs (count (tech/techs-with-no-requirements))
             :trees-by-category (->> (tech/all-trees)
                                     (group-by tech/tree-category)
                                     (map (fn [[k v]] [k (count v)]))
                                     (into (sorted-map)))}
   :currencies (tech/all-currencies-as-maps)
   :trees (extract-all-trees)
   :techs (extract-all-techs)})


;; ============================================
;; Individual Exports
;; ============================================

(defn extract-techs-edn
  "Extract all technologies to EDN file."
  ([] (extract-techs-edn (str *output-dir* "/data")))
  ([output-dir]
   (let [data (build-technologies-data)
         path (str output-dir "/technologies.edn")]
     (common/save-edn-pretty data path)
     data)))

(defn extract-trees-only
  "Extract just the tree structure."
  ([] (extract-trees-only (str *output-dir* "/data")))
  ([output-dir]
   (let [data {:version "1.0"
               :extracted-at (str (java.time.Instant/now))
               :trees (tech/all-trees-as-maps)}
         path (str output-dir "/tech-trees.edn")]
     (common/save-edn-pretty data path)
     data)))

;; ============================================
;; Summary and Reports
;; ============================================

(defn extract-technologies-summary
  "Print summary of technologies."
  []
  (let [data (build-technologies-data)]
    (println "=== Technologies Summary ===")
    (println "Total technologies:" (:total-techs (:summary data)))
    (println "Total trees:" (:total-trees (:summary data)))
    (println "Currency types:" (:total-currencies (:summary data)))
    (println "Root techs (no prereqs):" (:root-techs (:summary data)))
    (println)
    (println "=== Trees by Category ===")
    (doseq [[cat count] (:trees-by-category (:summary data))]
      (println (str "  Category " cat ": " count " trees")))
    (println)
    (println "=== Tech Trees ===")
    (doseq [tree (:trees data)]
      (println (str "  " (:key tree) " - " (:name tree) 
                    " (" (count (:techs tree)) " techs)")))
    (println)
    (println "=== Sample Technologies ===")
    (doseq [t (take 5 (:techs data))]
      (println (str "  " (:key t) " - " (:name t) 
                    " (max level: " (:level-max t) ")")))
    (println "  ...")))

(defn tech-dependency-report
  "Generate a dependency report showing tech prerequisites."
  []
  (println "=== Tech Dependency Report ===")
  (doseq [tree (tech/all-trees)]
    (println)
    (println (str "== " (tech/tree-name tree) " =="))
    (doseq [t (tech/tree-techs tree)]
      (let [reqs (tech/tech-requirements-nodes t)]
        (println (str "  " (tech/tech-key t) 
                      (when (pos? (.size reqs))
                        (str " <- " (clojure.string/join ", " 
                                     (map #(str (tech/tech-key (tech/requirement-tech %)) 
                                               "@L" (tech/requirement-level %))
                                          reqs))))))))))

;; ============================================
;; Main Extraction Functions
;; ============================================

(defn extract-all
  "Extract all technology data."
  ([] (extract-all *output-dir*))
  ([output-dir]
   (println "Extracting technologies to:" output-dir)
   (extract-techs-edn (str output-dir "/data"))
   (println "Done!")))

(comment
  (extract-all)
  :rcf)

;; ============================================
;; Individual Queries
;; ============================================

(defn find-tech
  "Find and display tech info by key."
  [key]
  (if-let [t (first (filter #(= (tech/tech-key %) key) (tech/all-techs)))]
    (tech/tech->map t)
    (println "Technology not found:" key)))

(defn find-tree
  "Find and display tree info by key."
  [key]
  (if-let [t (first (filter #(= (tech/tree-key %) key) (tech/all-trees)))]
    (tech/tree->map-full t)
    (println "Tech tree not found:" key)))

(defn list-techs-by-tree
  "List technologies grouped by tree."
  []
  (->> (tech/all-techs-as-maps)
       (group-by :tree-key)
       (into (sorted-map))))

(defn list-root-techs
  "List all technologies with no prerequisites."
  []
  (mapv tech/tech->map (tech/techs-with-no-requirements)))

(defn list-currencies
  "List all tech currencies."
  []
  (tech/all-currencies-as-maps))

(comment
  ;; === Quick Test ===
  
  ;; Check if game is loaded
  (tech/tech-count)
  (tech/tree-count)
  
  ;; Extract summary
  (extract-technologies-summary)
  
  ;; Dependency report
  (tech-dependency-report)
  
  ;; Extract to file
  (extract-techs-edn)
  (extract-trees-only)
  
  ;; Find specific tech
  (find-tech "CIVIL_SLAVE")
  
  ;; Find tree
  (find-tree "CIVIL")
  
  ;; List by tree
  (keys (list-techs-by-tree))
  
  ;; List root techs
  (list-root-techs)
  
  ;; List currencies
  (list-currencies)
  
  ;; Full extraction
  (extract-all "output/wiki")
  
  :rcf)

