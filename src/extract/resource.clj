(ns extract.resource
  "Resource data extraction for wiki.
   
   This namespace extracts all resource data and exports to EDN/JSON.
   Uses game.resource for accessing resource data.
   
   Usage:
     (require '[extract.resource :as res-extract])
     (res-extract/extract-all \"output/wiki\")
   "
  (:require
   [game.resource :as res]
   [game.sprite :as sprite]
   [clojure.java.io :as io]
   [clojure.edn :as edn])
  (:import
   [java.io File]))

;; ============================================
;; Configuration
;; ============================================

(def ^:dynamic *output-dir* "output/wiki")

;; ============================================
;; Enhanced Data Extraction
;; ============================================

(defn extract-resource-full
  "Extract full resource data including sprite paths."
  [resource]
  (let [base (res/resource->map resource)
        key (:key base)]
    (assoc base
           :icon-path (str "sprites/resources/" key "/icon.png")
           :sprite-path (str "sprites/resources/" key "/lay.png"))))

(defn extract-all-resources
  "Extract all resources with full data."
  []
  (mapv extract-resource-full (res/all-resources)))

(defn extract-minables-full
  "Extract minables with terrain preferences."
  []
  (res/all-minables-as-maps))

(defn extract-growables-full
  "Extract growables with climate data."
  []
  (res/all-growables-as-maps))

;; ============================================
;; Aggregate Data Structure
;; ============================================

(defn build-resources-data
  "Build complete resources data structure for wiki."
  []
  {:version "1.0"
   :extracted-at (str (java.time.Instant/now))
   :summary {:total-resources (res/resource-count)
             :total-minables (count (res/minable-list))
             :total-growables (count (res/growable-list))
             :total-edibles (count (res/edible-list))
             :total-drinkables (count (res/drink-list))
             :categories (res/categories-count)}
   :resources (extract-all-resources)
   :minables (extract-minables-full)
   :growables (extract-growables-full)
   :edibles (res/all-edibles-as-maps)
   :drinkables (res/all-drinks-as-maps)})

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
;; Sprite Export
;; ============================================

(defn export-resource-icons
  "Export all resource icons.
   Note: Resource icons come from sprite sheets, need icon extraction support."
  [output-dir]
  ;; TODO: Implement when icon extraction is ready
  ;; Resource icons are from init.sprite.UI.Icons
  (println "Resource icon export not yet implemented"))

;; ============================================
;; Main Extraction Functions
;; ============================================

(defn extract-resources-edn
  "Extract resources data to EDN file."
  ([] (extract-resources-edn (str *output-dir* "/data")))
  ([output-dir]
   (let [data (build-resources-data)
         path (str output-dir "/resources.edn")]
     (save-edn-pretty data path)
     data)))

(defn extract-resources-summary
  "Print summary of resources."
  []
  (let [data (build-resources-data)]
    (println "=== Resources Summary ===")
    (println "Total resources:" (:total-resources (:summary data)))
    (println "Minables:" (:total-minables (:summary data)))
    (println "Growables:" (:total-growables (:summary data)))
    (println "Edibles:" (:total-edibles (:summary data)))
    (println "Drinkables:" (:total-drinkables (:summary data)))
    (println "Categories:" (:categories (:summary data)))
    (println)
    (println "=== Sample Resources ===")
    (doseq [r (take 5 (:resources data))]
      (println (str "  " (:key r) " - " (:name r))))
    (println "  ...")))

(defn extract-all
  "Extract all resource data and sprites."
  ([] (extract-all *output-dir*))
  ([output-dir]
   (println "Extracting resources to:" output-dir)
   (extract-resources-edn (str output-dir "/data"))
   (export-resource-icons (str output-dir "/sprites"))
   (println "Done!")))

;; ============================================
;; Individual Resource Queries
;; ============================================

(defn find-resource
  "Find and display resource info by key."
  [key]
  (if-let [r (res/get-resource key)]
    (res/resource->map r)
    (println "Resource not found:" key)))

(defn list-resources-by-category
  "List resources grouped by category."
  []
  (->> (res/all-resources-as-maps)
       (group-by :category)
       (into (sorted-map))))

(defn list-edibles
  "List all edible resources."
  []
  (filter :edible (res/all-resources-as-maps)))

(defn list-drinkables
  "List all drinkable resources."
  []
  (filter :drinkable (res/all-resources-as-maps)))

(comment
  ;; === Quick Test ===
  
  ;; Check if game is loaded
  (res/resource-count)
  
  ;; Extract summary
  (extract-resources-summary)
  
  ;; Extract to file
  (extract-resources-edn)
  
  ;; Find specific resource
  (find-resource "BREAD")
  (find-resource "_STONE")
  
  ;; List by category
  (list-resources-by-category)
  
  ;; List edibles
  (list-edibles)
  
  ;; Full extraction
  (extract-all "output/wiki")
  
  :rcf)

