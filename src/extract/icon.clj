(ns extract.icon
  "UI Icon extraction for wiki.
   
   This namespace extracts all UI icons from the game's sprite sheets
   and exports them as individual PNG files.
   
   Icon sizes:
     - Small (16x16): General UI icons
     - Medium (24x24): Action/building icons  
     - Large (32x32): Category/menu icons
   
   Usage:
     (require '[extract.icon :as icon-extract])
     (icon-extract/extract-all \"output/wiki\")
   "
  (:require
   [game.sprite :as sprite]
   [extract.common :as common]))

;; ============================================
;; Configuration
;; ============================================

(def ^:dynamic *output-dir* "output/wiki")

;; Icon size categories with descriptive names
(def icon-sizes
  {:small  {:label "Small (16x16)"  :key :small}
   :medium {:label "Medium (24x24)" :key :medium}
   :large  {:label "Large (32x32)"  :key :large}})

;; ============================================
;; Icon Catalog Building
;; ============================================

(defn build-small-icon-catalog
  "Build catalog of small icons with names and indices."
  []
  (->> (sprite/list-small-icon-fields)
       (map (fn [name]
              (let [icon (sprite/icon-small name)
                    index (sprite/icon-tile-index icon)]
                {:name name
                 :index index
                 :size 16
                 :category :small})))
       (filter :index)
       (sort-by :index)))

(defn build-medium-icon-catalog
  "Build catalog of medium icons with names and indices."
  []
  (->> (sprite/list-medium-icon-fields)
       (map (fn [name]
              (let [icon (sprite/icon-medium name)
                    index (sprite/icon-tile-index icon)]
                {:name name
                 :index index
                 :size 24
                 :category :medium})))
       (filter :index)
       (sort-by :index)))

(defn build-large-icon-catalog
  "Build catalog of large icons with names and indices."
  []
  (->> (sprite/list-large-icon-fields)
       (map (fn [name]
              (let [icon (sprite/icon-large name)
                    index (sprite/icon-tile-index icon)]
                {:name name
                 :index index
                 :size 32
                 :category :large})))
       (filter :index)
       (sort-by :index)))

(defn build-full-catalog
  "Build complete icon catalog for all sizes."
  []
  {:small (build-small-icon-catalog)
   :medium (build-medium-icon-catalog)
   :large (build-large-icon-catalog)
   :summary {:small-count (count (build-small-icon-catalog))
             :medium-count (count (build-medium-icon-catalog))
             :large-count (count (build-large-icon-catalog))}})

;; ============================================
;; Icon Export Functions
;; ============================================

(defn export-named-icons
  "Export all named icons for a given size.
   
   Arguments:
     size-key - :small, :medium, or :large
     output-dir - directory to save icons
   
   Options:
     :scale - scale factor (default 1)
   
   Returns summary of exported icons."
  [size-key output-dir & {:keys [scale] :or {scale 1}}]
  (let [catalog (case size-key
                  :small (build-small-icon-catalog)
                  :medium (build-medium-icon-catalog)
                  :large (build-large-icon-catalog))
        results (doall
                 (for [{:keys [name index]} catalog]
                   (let [filename (str name ".png")
                         path (str output-dir "/" filename)
                         result (sprite/export-icon-from-sheet size-key index path :scale scale)]
                     (assoc result :name name :index index))))]
    {:size size-key
     :output-dir output-dir
     :total (count catalog)
     :success-count (count (filter :success results))
     :failed (filter #(not (:success %)) results)}))

(defn export-all-icons-by-index
  "Export all icons from a sheet by index (includes unnamed icons).
   
   Arguments:
     size-key - :small, :medium, or :large
     output-dir - directory to save icons
   
   Options:
     :scale - scale factor (default 1)
   
   Returns summary of exported icons."
  [size-key output-dir & {:keys [scale] :or {scale 1}}]
  (sprite/export-all-icons-from-sheet size-key output-dir :scale scale))

;; ============================================
;; Summary and Statistics
;; ============================================

(defn get-icon-statistics
  "Get statistics about available icons."
  []
  (let [small-info (sprite/get-icon-sheet-info :small)
        medium-info (sprite/get-icon-sheet-info :medium)
        large-info (sprite/get-icon-sheet-info :large)
        small-named (build-small-icon-catalog)
        medium-named (build-medium-icon-catalog)
        large-named (build-large-icon-catalog)]
    {:small {:sheet-total (:total small-info)
             :named-count (count small-named)
             :grid small-info}
     :medium {:sheet-total (:total medium-info)
              :named-count (count medium-named)
              :grid medium-info}
     :large {:sheet-total (:total large-info)
             :named-count (count large-named)
             :grid large-info}}))

(defn print-icon-summary
  "Print a summary of available icons."
  []
  (let [stats (get-icon-statistics)]
    (println "=== Icon Summary ===")
    (println)
    (doseq [[size-key {:keys [sheet-total named-count grid]}] stats]
      (println (str "  " (name size-key) " icons:"))
      (println (str "    Sheet grid: " (:cols grid) "x" (:rows grid) " = " sheet-total " total"))
      (println (str "    Named icons: " named-count))
      (println))))

;; ============================================
;; Data Export (EDN)
;; ============================================

(defn build-icons-data
  "Build complete icons data structure for wiki."
  []
  {:version "1.0"
   :extracted-at (str (java.time.Instant/now))
   :statistics (get-icon-statistics)
   :icons {:small (build-small-icon-catalog)
           :medium (build-medium-icon-catalog)
           :large (build-large-icon-catalog)}})

(defn extract-icons-edn
  "Extract icon catalog to EDN file."
  ([] (extract-icons-edn (str *output-dir* "/data")))
  ([output-dir]
   (let [data (build-icons-data)
         path (str output-dir "/icons.edn")]
     (common/save-edn-pretty data path)
     data)))

;; ============================================
;; Main Extraction Functions
;; ============================================

(defn extract-icons-sprites
  "Export all icon sprites to output directory.
   
   Directory structure:
     output-dir/
       sprites/
         icons/
           small/     - 16x16 icons by name
           medium/    - 24x24 icons by name
           large/     - 32x32 icons by name"
  ([] (extract-icons-sprites *output-dir*))
  ([output-dir]
   (let [sprites-dir (str output-dir "/sprites/icons")]
     (println "Exporting icon sprites to:" sprites-dir)
     (println)
     
     ;; Export small icons
     (println "Exporting small (16x16) icons...")
     (let [result (export-named-icons :small (str sprites-dir "/small"))]
       (println (str "  Exported: " (:success-count result) "/" (:total result))))
     
     ;; Export medium icons
     (println "Exporting medium (24x24) icons...")
     (let [result (export-named-icons :medium (str sprites-dir "/medium"))]
       (println (str "  Exported: " (:success-count result) "/" (:total result))))
     
     ;; Export large icons
     (println "Exporting large (32x32) icons...")
     (let [result (export-named-icons :large (str sprites-dir "/large"))]
       (println (str "  Exported: " (:success-count result) "/" (:total result))))
     
     (println)
     (println "Done!"))))

(defn extract-all
  "Extract all icon data and sprites.
   
   Outputs:
     - data/icons.edn - Icon catalog with names and indices
     - sprites/icons/small/*.png - Small icons
     - sprites/icons/medium/*.png - Medium icons  
     - sprites/icons/large/*.png - Large icons"
  ([] (extract-all *output-dir*))
  ([output-dir]
   (println "========================================")
   (println "Icon Extraction")
   (println "========================================")
   (println)
   
   ;; Print summary first
   (print-icon-summary)
   
   ;; Extract data
   (println "Extracting icon catalog...")
   (extract-icons-edn (str output-dir "/data"))
   (println)
   
   ;; Extract sprites
   (extract-icons-sprites output-dir)
   
   (println)
   (println "========================================")))

(comment
  (extract-all)
  :rcf)

(comment
  ;; === Quick Tests ===
  
  ;; Check icon statistics
  (get-icon-statistics)
  
  ;; Print summary
  (print-icon-summary)
  
  ;; Build catalogs
  (take 10 (build-small-icon-catalog))
  (take 10 (build-medium-icon-catalog))
  (take 10 (build-large-icon-catalog))
  
  ;; Export single icon by name
  (sprite/export-named-icon :small "sword" "output/test/sword.png")
  
  ;; Export named icons for one size
  (export-named-icons :small "output/test/small")
  
  ;; Export data only
  (extract-icons-edn)
  
  ;; Export sprites only
  (extract-icons-sprites)
  
  ;; Full extraction
  (extract-all "output/wiki")
  
  :rcf)

