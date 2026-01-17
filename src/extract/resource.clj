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
   [extract.common :as common]))

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
             :total-minables (.size (res/minable-list))
             :total-growables (.size (res/growable-list))
             :total-edibles (.size (res/edible-list))
             :total-drinkables (.size (res/drink-list))
             :categories (res/categories-count)}
   :resources (extract-all-resources)
   :minables (extract-minables-full)
   :growables (extract-growables-full)
   :edibles (res/all-edibles-as-maps)
   :drinkables (res/all-drinks-as-maps)})


;; ============================================
;; Sprite Export
;; ============================================

;; Resource key to icon file name mapping
;; Some resources have icon file names that differ from their keys
(def resource-icon-name-map
  {"ALCO_BEER" "Alcohol"
   "ALCO_WINE" "Alcohol"
   "_STONE" "Stone"
   "_WOOD" "Wood"
   "_LIVESTOCK" "Livestock"
   "STONE_CUT" "StoneCut"
   "ARMOUR_LEATHER" "Armour_leather"
   "ARMOUR_PLATE" "Armour_plate"
   "WEAPON_HAMMER" "Weapon_hammer"
   "WEAPON_MOUNT" "Weapon_mount"
   "WEAPON_SHIELD" "Weapon_shield"
   "WEAPON_SHORT" "Weapon_short"
   "WEAPON_SLASH" "Weapon_slash"
   "WEAPON_SPEAR" "Weapon_spear"})

(defn resource-key->icon-name
  "Convert resource key to icon file name.
   E.g., 'BREAD' -> 'Bread', '_STONE' -> 'Stone'"
  [resource-key]
  (if-let [mapped-name (get resource-icon-name-map resource-key)]
    mapped-name
    ;; Default: capitalize first letter, lowercase rest
    (let [key (if (.startsWith resource-key "_")
                (.substring resource-key 1)
                resource-key)]
      (str (.toUpperCase (subs key 0 1))
           (.toLowerCase (subs key 1))))))

(defn get-resource-icon-info
  "Get icon information for a resource.
   Returns map with :key, :name, :icon-name, etc."
  [resource]
  (let [key (res/resource-key resource)
        icon-name (resource-key->icon-name key)]
    {:key key
     :name (res/resource-name resource)
     :icon-name icon-name
     :icon-path (str "data/assets/sprite/icon/24/resource/" icon-name ".png")}))

(defn export-single-resource-icon
  "Export icon for a single resource.
   
   Arguments:
     resource - RESOURCE object
     output-dir - base directory for output
   
   Options:
     :scale - scale factor (default 1)
     :index - icon index in the sheet (default 0)
   
   Returns export result map."
  [resource output-dir & {:keys [scale index] :or {scale 1 index 0}}]
  (let [key (res/resource-key resource)
        icon-name (resource-key->icon-name key)
        output-path (str output-dir "/" key ".png")
        result (sprite/export-resource-icon-from-file icon-name output-path :scale scale :index index)]
    (assoc result :resource-key key :icon-name icon-name)))

(defn export-resource-icons
  "Export all resource icons to output directory.
   
   Arguments:
     output-dir - directory to save icons (e.g., \"output/wiki/sprites/resources\")
   
   Options:
     :scale - scale factor (default 1)
   
   Returns summary of exported icons."
  [output-dir & {:keys [scale] :or {scale 1}}]
  (println "Exporting resource icons to:" output-dir)
  (let [resources (vec (res/all-resources))  ; Convert ArrayList to vector
        results (doall
                 (for [r resources]
                   (export-single-resource-icon r output-dir :scale scale)))
        success-count (count (filter :success results))
        failed (vec (filter #(not (:success %)) results))]
    (println (str "  Exported: " success-count "/" (count resources)))
    (when (seq failed)
      (println (str "  Failed: " (count failed)))
      (doseq [{:keys [resource-key error]} (take 5 failed)]
        (println (str "    - " resource-key ": " error))))
    {:total (count resources)
     :success-count success-count
     :failed-count (count failed)
     :failed failed}))

(defn build-resource-icon-catalog
  "Build a catalog of all resource icons with their metadata."
  []
  (->> (vec (res/all-resources))
       (map get-resource-icon-info)
       (sort-by :key)
       vec))

;; ============================================
;; Main Extraction Functions
;; ============================================

(defn extract-resources-edn
  "Extract resources data to EDN file."
  ([] (extract-resources-edn (str *output-dir* "/data")))
  ([output-dir]
   (let [data (build-resources-data)
         path (str output-dir "/resources.edn")]
     (common/save-edn-pretty data path)
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

(defn extract-resource-sprites
  "Export all resource sprites to output directory.
   
   Directory structure:
     output-dir/
       sprites/
         resources/   - Resource icons by key (e.g., BREAD.png)"
  ([] (extract-resource-sprites *output-dir*))
  ([output-dir]
   (let [sprites-dir (str output-dir "/sprites/resources")]
     (println "Exporting resource sprites to:" sprites-dir)
     (export-resource-icons sprites-dir))))

(defn extract-all
  "Extract all resource data and sprites.
   
   Outputs:
     - data/resources.edn - Resource data catalog
     - sprites/resources/*.png - Resource icons"
  ([] (extract-all *output-dir*))
  ([output-dir]
   (println "========================================")
   (println "Resource Extraction")
   (println "========================================")
   (println)
   
   ;; Extract data
   (println "Extracting resource data...")
   (extract-resources-edn (str output-dir "/data"))
   (println)
   
   ;; Extract sprites
   (extract-resource-sprites output-dir)
   
   (println)
   (println "========================================")))

(comment
  (extract-all)
  :rcf)

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
  
  ;; === Resource Icons ===
  
  ;; Get icon info for a resource
  (get-resource-icon-info (res/get-resource "BREAD"))
  ;; => {:key "BREAD", :name "Bread", :icon-size 24, :tile-index N, ...}
  
  ;; Build catalog of all resource icons
  (take 5 (build-resource-icon-catalog))
  
  ;; Export single resource icon
  (export-single-resource-icon (res/get-resource "BREAD") "output/test/resources")
  
  ;; Export all resource icons
  (export-resource-icons "output/wiki/sprites/resources")
  
  ;; Export only sprites
  (extract-resource-sprites "output/wiki")
  
  ;; Full extraction (data + sprites)
  (extract-all "output/wiki")
  
  :rcf)

