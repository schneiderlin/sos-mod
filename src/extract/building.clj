(ns extract.building
  "Building/Room data extraction for wiki.
   
   This namespace extracts all building/room data and exports to EDN/JSON.
   Uses game.building for accessing room data.
   
   Usage:
     (require '[extract.building :as build-extract])
     (build-extract/extract-all \"output/wiki\")
   "
  (:require
   [game.building :as build]
   [game.sprite :as sprite]
   [extract.common :as common]
   [clojure.pprint]
   [clojure.string :as str]))

;; ============================================
;; Configuration
;; ============================================

(def ^:dynamic *output-dir* "output/wiki")

;; ============================================
;; Enhanced Data Extraction
;; ============================================

(defn extract-room-full
  "Extract full room data including computed fields."
  [room]
  (let [base (build/room-imp->map room)
        key (:key base)]
    (assoc base
           :icon-path (str "sprites/buildings/" key "/icon.png")
           :is-production-room (build/room-has-industries? room))))

(defn extract-all-rooms
  "Extract all rooms with full data."
  []
  (mapv extract-room-full (build/all-blueprint-imps)))

(defn extract-production-rooms
  "Extract only rooms with production capabilities."
  []
  (mapv extract-room-full (build/production-rooms)))

;; ============================================
;; Category Extraction
;; ============================================

(defn extract-categories
  "Extract all room categories with room counts."
  []
  (->> (build/rooms-by-category)
       (map (fn [[cat-name rooms]]
              {:name cat-name
               :room-count (count rooms)
               :room-keys (mapv build/blueprint-key rooms)}))
       (vec)))

(defn extract-types
  "Extract all room types with room counts."
  []
  (->> (build/rooms-by-type)
       (map (fn [[type-name rooms]]
              {:type type-name
               :room-count (count rooms)
               :room-keys (mapv build/blueprint-key rooms)}))
       (vec)))

;; ============================================
;; Aggregate Data Structure
;; ============================================

(defn build-buildings-data
  "Build complete buildings data structure for wiki."
  []
  {:version "1.0"
   :extracted-at (str (java.time.Instant/now))
   :summary {:total-blueprints (build/blueprint-count)
             :total-rooms (build/blueprint-imp-count)
             :production-rooms (count (build/production-rooms))
             :categories-count (count (build/rooms-by-category))
             :types-count (count (build/rooms-by-type))}
   :categories (extract-categories)
   :types (extract-types)
   :rooms (extract-all-rooms)})

(defn build-production-data
  "Build production-focused data structure."
  []
  {:version "1.0"
   :extracted-at (str (java.time.Instant/now))
   :summary {:total-production-rooms (count (build/production-rooms))}
   :rooms (extract-production-rooms)})


;; ============================================
;; Individual Exports
;; ============================================

(defn extract-buildings-edn
  "Extract all buildings to EDN file."
  ([] (extract-buildings-edn (str *output-dir* "/data")))
  ([output-dir]
   (let [data (build-buildings-data)
         path (str output-dir "/buildings.edn")]
     (common/save-edn-pretty data path)
     data)))

(defn extract-production-edn
  "Extract production buildings to EDN file."
  ([] (extract-production-edn (str *output-dir* "/data")))
  ([output-dir]
   (let [data (build-production-data)
         path (str output-dir "/production.edn")]
     (common/save-edn-pretty data path)
     data)))

;; ============================================
;; Summary and Reports
;; ============================================

(defn extract-buildings-summary
  "Print summary of buildings."
  []
  (let [data (build-buildings-data)]
    (println "=== Buildings Summary ===")
    (println "Total blueprints:" (:total-blueprints (:summary data)))
    (println "Total rooms:" (:total-rooms (:summary data)))
    (println "Production rooms:" (:production-rooms (:summary data)))
    (println "Categories:" (:categories-count (:summary data)))
    (println "Types:" (:types-count (:summary data)))
    (println)
    (println "=== Categories ===")
    (doseq [cat (:categories data)]
      (println (str "  " (:name cat) ": " (:room-count cat) " rooms")))
    (println)
    (println "=== Types ===")
    (doseq [t (take 10 (:types data))]
      (println (str "  " (:type t) ": " (:room-count t) " rooms")))
    (when (> (count (:types data)) 10)
      (println "  ..."))
    (println)
    (println "=== Sample Rooms ===")
    (doseq [r (take 5 (:rooms data))]
      (println (str "  " (:key r) " - " (:name r) 
                    " [" (or (:type r) "misc") "]")))
    (println "  ...")))

(defn production-report
  "Generate a production report showing inputs/outputs."
  []
  (println "=== Production Report ===")
  (doseq [room (build/production-rooms)]
    (println)
    (println (str "== " (build/room-name room) " (" (build/blueprint-key room) ") =="))
    (doseq [ind (build/room-industries room)]
      (let [inputs (mapv build/industry-resource->map (build/industry-inputs ind))
            outputs (mapv build/industry-resource->map (build/industry-outputs ind))]
        (println (str "  Recipe " (.index ind) ":"))
        (when (seq inputs)
          (println "    Inputs:")
          (doseq [in inputs]
            (println (str "      - " (:resource-key in) " @ " (:rate-per-second in) "/s"))))
        (println "    Outputs:")
        (doseq [out outputs]
          (println (str "      - " (:resource-key out) " @ " (:rate-per-second out) "/s")))))))

;; ============================================
;; Main Extraction Functions
;; ============================================

(defn extract-all
  "Extract all building data."
  ([] (extract-all *output-dir*))
  ([output-dir]
   (println "Extracting buildings to:" output-dir)
   (extract-buildings-edn (str output-dir "/data"))
   (extract-production-edn (str output-dir "/data"))
   (println "Done!")))

(comment
  (extract-all)
  :rcf)

;; ============================================
;; Individual Queries
;; ============================================

(defn find-room
  "Find and display room info by key."
  [key]
  (if-let [r (build/find-room-by-key key)]
    (build/room-imp->map r)
    (println "Room not found:" key)))

(defn list-rooms-by-type
  "List rooms grouped by type."
  []
  (->> (build/all-rooms-as-maps)
       (group-by :type)
       (into (sorted-map))))

(defn list-rooms-by-category
  "List rooms grouped by category."
  []
  (->> (build/all-rooms-as-maps)
       (group-by #(get-in % [:category :name]))
       (into (sorted-map))))

(defn list-production-chains
  "List all production chains (what produces what)."
  []
  (for [room (build/production-rooms)
        ind (build/room-industries room)]
    {:room-key (build/blueprint-key room)
     :room-name (build/room-name room)
     :inputs (mapv :resource-key (mapv build/industry-resource->map 
                                       (build/industry-inputs ind)))
     :outputs (mapv :resource-key (mapv build/industry-resource->map 
                                        (build/industry-outputs ind)))}))

(defn find-producers
  "Find all rooms that produce a resource."
  [resource-key]
  (mapv #(build/room-imp->map %) (build/find-rooms-producing resource-key)))

(defn find-consumers
  "Find all rooms that consume a resource."
  [resource-key]
  (mapv #(build/room-imp->map %) (build/find-rooms-consuming resource-key)))

;; ============================================
;; Sprite Export Functions
;; ============================================

(defn get-room-icon-info
  "Get icon information for a room.
   Returns map with :key, :name, :icon-size, :tile-index, etc."
  [room]
  (build/room-icon-info room))

(defn export-single-room-icon
  "Export icon for a single room.
   
   Arguments:
     room - RoomBlueprintImp object
     output-dir - base directory for output
   
   Options:
     :scale - scale factor (default 1)
   
   Returns export result map."
  [room output-dir & {:keys [scale] :or {scale 1}}]
  (let [key (build/blueprint-key room)
        icon (build/room-icon room)
        tile-index (build/icon-tile-index icon)
        size-key (build/icon-size-key icon)
        output-path (str output-dir "/" key ".png")]
    (if (and tile-index size-key)
      (let [result (sprite/export-icon-from-sheet size-key tile-index output-path :scale scale)]
        (assoc result :room-key key :icon-size-key size-key))
      {:success false
       :room-key key
       :is-composite (build/icon-is-composite? icon)
       :inner-class (build/icon-inner-class-name icon)
       :error (if (build/icon-is-composite? icon)
                "Composite icon (BG+FG) - requires manual extraction"
                "Could not determine icon tile index or size")})))

(defn export-room-icons
  "Export all room icons to output directory.
   
   Arguments:
     output-dir - directory to save icons (e.g., \"output/wiki/sprites/buildings\")
   
   Options:
     :scale - scale factor (default 1)
   
   Returns summary of exported icons."
  [output-dir & {:keys [scale] :or {scale 1}}]
  (println "Exporting room icons to:" output-dir)
  (let [rooms (vec (build/all-blueprint-imps))
        results (doall
                 (for [r rooms]
                   (export-single-room-icon r output-dir :scale scale)))
        success-count (count (filter :success results))
        failed (vec (filter #(not (:success %)) results))
        composite-count (count (filter :is-composite failed))]
    (println (str "  Exported: " success-count "/" (count rooms)))
    (when (seq failed)
      (println (str "  Failed: " (count failed) " (" composite-count " composite icons)"))
      (doseq [{:keys [room-key error]} (take 5 failed)]
        (println (str "    - " room-key ": " error))))
    {:total (count rooms)
     :success-count success-count
     :failed-count (count failed)
     :composite-count composite-count
     :failed failed}))

(defn list-composite-icon-rooms
  "List all rooms that have composite icons (BG+FG) which can't be auto-extracted.
   Groups them by their base type (e.g., MINE_CLAY, MINE_COAL -> MINE)."
  []
  (let [rooms (vec (build/all-blueprint-imps))
        composite-rooms (->> rooms
                             (filter #(build/icon-is-composite? (build/room-icon %)))
                             (map (fn [r]
                                    (let [key (build/blueprint-key r)]
                                      {:key key
                                       :name (build/room-name r)
                                       :type (build/room-type r)
                                       :base-type (first (str/split key #"_"))}))))]
    {:total (count composite-rooms)
     :by-base-type (->> composite-rooms
                        (group-by :base-type)
                        (map (fn [[k v]] [k (mapv :key v)]))
                        (into (sorted-map)))
     :rooms composite-rooms}))

(defn build-room-icon-catalog
  "Build a catalog of all room icons with their metadata."
  []
  (->> (vec (build/all-blueprint-imps))
       (map get-room-icon-info)
       (sort-by :key)
       vec))

(defn extract-room-sprites
  "Export all room sprites to output directory.
   
   Directory structure:
     output-dir/
       sprites/
         buildings/   - Room icons by key (e.g., FARM.png)"
  ([] (extract-room-sprites *output-dir*))
  ([output-dir]
   (let [sprites-dir (str output-dir "/sprites/buildings")]
     (println "Extracting room sprites to:" sprites-dir)
     (export-room-icons sprites-dir))))

;; ============================================
;; Updated Main Extraction Functions
;; ============================================

(defn extract-all-with-sprites
  "Extract all building data and sprites.
   
   Outputs:
     - data/buildings.edn - Building data catalog
     - data/production.edn - Production-focused data
     - sprites/buildings/*.png - Room icons"
  ([] (extract-all-with-sprites *output-dir*))
  ([output-dir]
   (println "========================================")
   (println "Building/Room Extraction")
   (println "========================================")
   (println)
   
   ;; Extract data
   (println "Extracting building data...")
   (extract-buildings-edn (str output-dir "/data"))
   (extract-production-edn (str output-dir "/data"))
   (println)
   
   ;; Extract sprites
   (extract-room-sprites output-dir)
   
   (println)
   (println "========================================")))

(comment
  (extract-all-with-sprites)

  ;; === Quick Test ===
  
  ;; Check if game is loaded
  (build/blueprint-count)
  (build/blueprint-imp-count)
  
  ;; Extract summary
  (extract-buildings-summary)
  
  ;; Production report
  (production-report)
  
  ;; Extract to file
  (extract-buildings-edn)
  (extract-production-edn)
  
  ;; Find specific room
  (find-room "FARM")
  (find-room "REFINER_SMELTER")
  
  ;; List by type
  (keys (list-rooms-by-type))
  
  ;; List by category
  (keys (list-rooms-by-category))
  
  ;; Production chains
  (take 5 (list-production-chains))
  
  ;; Find producers/consumers
  (find-producers "BREAD")
  (find-consumers "_STONE")
  
  ;; Full extraction (data only)
  (extract-all "output/wiki")
  
  ;; === Room Icon Extraction ===
  
  ;; Get icon info for a room
  (get-room-icon-info (build/find-room-by-key "FARM"))
  ;; => {:key "FARM", :name "Farm", :icon-size 24, :tile-index N, ...}
  
  ;; Build catalog of all room icons
  (take 5 (build-room-icon-catalog))
  
  ;; Export single room icon
  (export-single-room-icon (build/find-room-by-key "FARM") "output/test/buildings")
  
  ;; Export all room icons
  (export-room-icons "output/wiki/sprites/buildings")
  
  ;; List composite icons that can't be auto-extracted
  (list-composite-icon-rooms)
  ;; => {:total N, :by-base-type {"MINE" [...], "FISHERY" [...]}, ...}
  
  ;; Export only sprites
  (extract-room-sprites "output/wiki")
  
  ;; Full extraction with sprites (data + sprites)
  (extract-all-with-sprites "output/wiki")
  
  :rcf)

