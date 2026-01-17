(ns extract.race
  "Race data extraction for wiki.
   
   This namespace extracts all race data and exports to EDN/JSON.
   Uses game.race for accessing race data.
   
   Usage:
     (require '[extract.race :as race-extract])
     (race-extract/extract-all \"output/wiki\")
   "
  (:require
   [game.race :as race]
   [game.sprite :as sprite]
   [extract.common :as common]
   [clojure.string]))

;; ============================================
;; Configuration
;; ============================================

(def ^:dynamic *output-dir* "output/wiki")

;; ============================================
;; Enhanced Data Extraction
;; ============================================

(defn extract-race-full
  "Extract full race data including sprite paths."
  [r]
  (let [base (race/race->map-full r)
        key (:key base)]
    (assoc base
           :icon-path (str "sprites/races/" key "/icon.png")
           :sheet-path (str "sprites/races/" key "/sheet/")
           :lay-path (str "sprites/races/" key "/lay/"))))

(defn extract-all-races
  "Extract all races with full data."
  []
  (mapv extract-race-full (race/all-races)))

(comment
  (extract-all-races)
  :rcf)

(defn extract-playable-races
  "Extract only playable races."
  []
  (mapv extract-race-full (race/playable-races)))

;; ============================================
;; Aggregate Data Structure
;; ============================================

(defn build-races-data
  "Build complete races data structure for wiki."
  []
  {:version "1.0"
   :extracted-at (str (java.time.Instant/now))
   :summary {:total-races (race/race-count)
             :playable-races (race/playable-count)
             :non-playable-races (- (race/race-count) (race/playable-count))
             :playability (race/races-by-playability)}
   :races (extract-all-races)})

(defn build-race-relations-matrix
  "Build race relations matrix."
  []
  (let [races (race/all-races)
        keys (mapv race/race-key races)]
    {:race-keys keys
     :matrix (vec (for [r1 races]
                    (vec (for [r2 races]
                           (race/race-relation r1 r2)))))}))

(comment
  (build-race-relations-matrix)
  :rcf)


;; ============================================
;; Individual Exports
;; ============================================

(defn extract-races-edn
  "Extract all races to EDN file."
  ([] (extract-races-edn (str *output-dir* "/data")))
  ([output-dir]
   (let [data (build-races-data)
         path (str output-dir "/races.edn")]
     (common/save-edn-pretty data path)
     data)))

(defn extract-race-relations-edn
  "Extract race relations matrix to EDN file."
  ([] (extract-race-relations-edn (str *output-dir* "/data")))
  ([output-dir]
   (let [data (build-race-relations-matrix)
         path (str output-dir "/race-relations.edn")]
     (common/save-edn-pretty data path)
     data)))

;; ============================================
;; Summary and Reports
;; ============================================

(defn extract-races-summary
  "Print summary of races."
  []
  (let [data (build-races-data)]
    (println "=== Races Summary ===")
    (println "Total races:" (:total-races (:summary data)))
    (println "Playable races:" (:playable-races (:summary data)))
    (println "Non-playable races:" (:non-playable-races (:summary data)))
    (println)
    (println "=== Playable Races ===")
    (doseq [r (filter #(:playable %) (:races data))]
      (println (str "  " (:key r) " - " (get-in r [:info :name]))))
    (println)
    (println "=== Non-Playable Races ===")
    (doseq [r (filter #(not (:playable %)) (:races data))]
      (println (str "  " (:key r) " - " (get-in r [:info :name]))))
    (println)))

(comment
  (extract-races-summary)
  :rcf)

(defn race-details-report
  "Generate detailed report for a specific race."
  [key]
  (if-let [r (race/get-race key)]
    (let [data (race/race->map-full r)]
      (println "=== Race:" (get-in data [:info :name]) "===")
      (println)
      (println "Key:" (:key data))
      (println "Playable:" (:playable data))
      (println)
      (println "=== Info ===")
      (println "Description:" (get-in data [:info :description]))
      (println "Pros:" (clojure.string/join ", " (get-in data [:info :pros])))
      (println "Cons:" (clojure.string/join ", " (get-in data [:info :cons])))
      (println)
      (println "=== Physics ===")
      (println "Height:" (get-in data [:physics :height]))
      (println "Adult at day:" (get-in data [:physics :adult-at-day]))
      (println "Sleeps:" (get-in data [:physics :sleeps]))
      (println "Corpse decays:" (get-in data [:physics :corpse-decays]))
      (println "Slave price:" (get-in data [:physics :slave-price]))
      (println)
      (println "=== Population ===")
      (println "Growth rate:" (get-in data [:population :growth]))
      (println "Max fraction:" (get-in data [:population :max]))
      (println "Immigration rate:" (get-in data [:population :immigration-rate]))
      (println)
      (println "=== Preferences ===")
      (println "Preferred foods:" (clojure.string/join ", " (get-in data [:preferences :preferred-foods])))
      (println "Preferred drinks:" (clojure.string/join ", " (get-in data [:preferences :preferred-drinks])))
      (println "Most hated race:" (get-in data [:preferences :most-hated-race]))
      (println)
      (println "=== Boosts (" (count (:boosts data)) " total) ===")
      (doseq [b (take 5 (:boosts data))]
        (println (str "  " (:boostable-name b) ": " 
                      (if (:is-mul b) 
                        (str "x" (:to b))
                        (str "+" (:to b))))))
      (when (> (count (:boosts data)) 5)
        (println "  ...")))
    (println "Race not found:" key)))

(defn race-comparison-report
  "Generate a comparison report for all races."
  []
  (println "=== Race Comparison ===")
  (println)
  (println (format "%-15s %-10s %-10s %-10s %-10s %-10s"
                   "Race" "Playable" "Height" "Adult@Day" "SlavePrice" "Boosts"))
  (println (apply str (repeat 65 "-")))
  (doseq [r (race/all-races)]
    (let [data (race/race->map-full r)]
      (println (format "%-15s %-10s %-10.0f %-10d %-10d %-10d"
                       (subs (:key data) 0 (min 15 (count (:key data))))
                       (if (:playable data) "Yes" "No")
                       (double (get-in data [:physics :height]))
                       (int (get-in data [:physics :adult-at-day]))
                       (int (get-in data [:physics :slave-price]))
                       (count (:boosts data)))))))

(defn race-relations-report
  "Print race relations in a matrix format."
  []
  (let [races (race/all-races)
        keys (mapv #(subs (race/race-key %) 0 (min 8 (count (race/race-key %)))) races)]
    (println "=== Race Relations Matrix ===")
    (println "(1.0 = positive, 0.0 = hostile)")
    (println)
    ;; Header
    (print (format "%-9s" ""))
    (doseq [k keys]
      (print (format "%-8s" k)))
    (println)
    ;; Rows
    (doseq [r1 races]
      (print (format "%-9s" (subs (race/race-key r1) 0 (min 8 (count (race/race-key r1))))))
      (doseq [r2 races]
        (print (format "%-8.2f" (double (race/race-relation r1 r2)))))
      (println))))

;; ============================================
;; Sprite Export
;; ============================================

;; Action types for sheet sprites (from game.sprite)
(def sheet-actions
  [:feet-none :feet-right :feet-right2 :feet-left :feet-left2
   :tunic :torso-still :torso-right :torso-right2 :torso-right3
   :torso-left :torso-left2 :torso-left3 :torso-carry :torso-out :torso-out2
   :head :shadow])

;; Map game race keys to sprite file names
;; Sprite files are stored as: data/assets/sprite/race/{Name}.png
;; Game keys are uppercase (e.g., "HUMAN"), file names are capitalized (e.g., "Human")
(def race-key->sprite-name
  {"ARGONOSH"  "Argonosh"
   "CANTOR"    "Cantor"
   "CRETONIAN" "Cretonian"
   "DONDORIAN" "Dondorian"
   "GARTHIMI"  "Garthimi"
   "HUMAN"     "Human"
   "Q_AMEVIA"  "Amevia"
   "TILAPI"    "Tilapi"})

(defn get-race-sprite-name
  "Get the sprite file name for a race key.
   E.g., 'HUMAN' -> 'Human', 'Q_AMEVIA' -> 'Amevia'"
  [race-key]
  (get race-key->sprite-name race-key
       ;; Fallback: capitalize first letter, lowercase rest
       (str (.toUpperCase (subs race-key 0 1))
            (.toLowerCase (subs race-key 1)))))

(defn export-race-sheet-sprites
  "Export all sheet sprites (standing/walking) for a single race.
   
   Arguments:
     race-key - game race key string (e.g., \"HUMAN\", \"Q_AMEVIA\")
     output-dir - base directory (will create race-key/sheet/ subdirectory)
   
   Options:
     :scale - scale factor (default 1)
   
   Returns summary of exported sprites."
  [race-key output-dir & {:keys [scale] :or {scale 1}}]
  (let [sprite-name (get-race-sprite-name race-key)
        sheet-dir (str output-dir "/" race-key "/sheet")
        results (doall
                 (for [action sheet-actions]
                   (let [action-name (name action)
                         ;; Export body sprite
                         body-path (str sheet-dir "/" action-name ".png")
                         body-result (sprite/export-race-sprite :sheet sprite-name action body-path :scale scale)
                         ;; Export normal map
                         normal-path (str sheet-dir "/" action-name "_normal.png")
                         normal-result (sprite/export-race-sprite :sheet sprite-name action normal-path :scale scale :normal true)]
                     {:action action
                      :body body-result
                      :normal normal-result})))
        success-count (count (filter #(and (:success (:body %)) (:success (:normal %))) results))]
    {:race-key race-key
     :sprite-name sprite-name
     :type :sheet
     :total (* 2 (count sheet-actions))  ; body + normal for each action
     :success-count (* 2 success-count)
     :output-dir sheet-dir
     :results results}))

(defn export-race-lay-sprites
  "Export all lay sprites (lying down) for a single race.
   
   Arguments:
     race-key - game race key string (e.g., \"HUMAN\", \"Q_AMEVIA\")
     output-dir - base directory (will create race-key/lay/ subdirectory)
   
   Options:
     :scale - scale factor (default 1)
   
   Returns summary of exported sprites."
  [race-key output-dir & {:keys [scale] :or {scale 1}}]
  (let [sprite-name (get-race-sprite-name race-key)
        lay-dir (str output-dir "/" race-key "/lay")
        ;; Export body sprites (indices 0-11)
        body-results (doall
                      (for [i (range 12)]
                        (let [path (str lay-dir "/" i ".png")
                              result (sprite/export-race-sprite :lay sprite-name i path :scale scale)]
                          {:index i :type :body :result result})))
        ;; Export normal maps (indices 12-23)
        normal-results (doall
                        (for [i (range 12)]
                          (let [path (str lay-dir "/" i "_normal.png")
                                result (sprite/export-race-sprite :lay sprite-name (+ i 12) path :scale scale)]
                            {:index (+ i 12) :type :normal :result result})))
        all-results (concat body-results normal-results)
        success-count (count (filter #(:success (:result %)) all-results))]
    {:race-key race-key
     :sprite-name sprite-name
     :type :lay
     :total (* 2 12)  ; 12 body + 12 normal
     :success-count success-count
     :output-dir lay-dir
     :results all-results}))

(defn export-single-race-sprites
  "Export all sprites (sheet + lay) for a single race.
   
   Arguments:
     race-key - race key string (e.g., \"Human\")
     output-dir - base sprites directory
   
   Options:
     :scale - scale factor (default 1)
   
   Returns combined summary."
  [race-key output-dir & {:keys [scale] :or {scale 1}}]
  (let [sheet-result (export-race-sheet-sprites race-key output-dir :scale scale)
        lay-result (export-race-lay-sprites race-key output-dir :scale scale)]
    {:race-key race-key
     :sheet sheet-result
     :lay lay-result
     :total (+ (:total sheet-result) (:total lay-result))
     :success-count (+ (:success-count sheet-result) (:success-count lay-result))}))

(defn export-all-race-sprites
  "Export all sprites for all races.
   
   Arguments:
     output-dir - base sprites directory (will create races/ subdirectory)
   
   Options:
     :scale - scale factor (default 1)
   
   Returns summary of all exports."
  [output-dir & {:keys [scale] :or {scale 1}}]
  (let [races-dir (str output-dir "/races")
        all-races (vec (race/all-races))  ; Convert ArrayList to vector
        results (doall
                 (for [r all-races]
                   (let [race-key (race/race-key r)
                         _ (println (str "  Exporting " race-key "..."))
                         result (export-single-race-sprites race-key races-dir :scale scale)]
                     result)))
        total-exported (reduce + (map :success-count results))
        total-expected (reduce + (map :total results))]
    {:total-races (count all-races)
     :total-sprites total-expected
     :success-count total-exported
     :output-dir races-dir
     :results results}))

(defn extract-race-sprites
  "Export all race sprites to output directory.
   
   Directory structure:
     output-dir/
       sprites/
         races/
           Human/
             sheet/    - 18 actions × 2 (body + normal)
             lay/      - 12 positions × 2 (body + normal)
           Dondorian/
             ...
   "
  ([] (extract-race-sprites *output-dir*))
  ([output-dir]
   (let [sprites-dir (str output-dir "/sprites")]
     (println "Exporting race sprites to:" sprites-dir)
     (export-all-race-sprites sprites-dir))))

(defn build-race-sprite-metadata
  "Build metadata for all race sprites (for wiki data).
   Returns a map with sprite paths for each race."
  []
  (vec
   (for [r (race/all-races)]
     (let [race-key (race/race-key r)
           base-path (str "sprites/races/" race-key)]
       {:race-key race-key
        :sheet-sprites (vec (for [action sheet-actions]
                              {:action action
                               :path (str base-path "/sheet/" (name action) ".png")
                               :normal-path (str base-path "/sheet/" (name action) "_normal.png")
                               :size 24}))
        :lay-sprites (vec (for [i (range 12)]
                            {:index i
                             :path (str base-path "/lay/" i ".png")
                             :normal-path (str base-path "/lay/" i "_normal.png")
                             :size 32}))}))))

;; ============================================
;; Main Extraction Functions
;; ============================================

(defn extract-all
  "Extract all race data and sprites.
   
   Outputs:
     - data/races.edn - Race data catalog
     - data/race-relations.edn - Race relations matrix
     - sprites/races/*/sheet/*.png - Sheet sprites (standing/walking)
     - sprites/races/*/lay/*.png - Lay sprites (lying down)"
  ([] (extract-all *output-dir*))
  ([output-dir]
   (println "========================================")
   (println "Race Extraction")
   (println "========================================")
   (println)
   
   ;; Extract data
   (println "Extracting race data...")
   (extract-races-edn (str output-dir "/data"))
   (extract-race-relations-edn (str output-dir "/data"))
   (println)
   
   ;; Extract sprites
   (println "Extracting race sprites...")
   (let [result (extract-race-sprites output-dir)]
     (println (str "  Exported: " (:success-count result) "/" (:total-sprites result) " sprites"))
     (println (str "  Races: " (:total-races result))))
   
   (println)
   (println "========================================")))

(comment
  (extract-all)
  :rcf)

;; ============================================
;; Individual Queries
;; ============================================

(defn find-race
  "Find and display race info by key."
  [key]
  (if-let [r (race/get-race key)]
    (race/race->map-full r)
    (println "Race not found:" key)))

(defn list-races
  "List all races with basic info."
  []
  (->> (race/all-races)
       (map (fn [r]
              {:key (race/race-key r)
               :name (race/race-name r)
               :playable (race/race-playable? r)}))
       vec))

(defn list-playable
  "List all playable races."
  []
  (filter :playable (list-races)))

(defn list-non-playable
  "List all non-playable races."
  []
  (filter #(not (:playable %)) (list-races)))

(defn list-race-boosts
  "List all boosts for a specific race."
  [key]
  (if-let [r (race/get-race key)]
    (mapv race/boost-spec->map (race/race-boost-specs r))
    (println "Race not found:" key)))

(comment
  ;; === Quick Test ===
  
  ;; Check if game is loaded
  (race/race-count)
  
  ;; Extract summary
  (extract-races-summary)
  
  ;; Race comparison
  (race-comparison-report)
  
  ;; Race relations
  (race-relations-report)
  
  ;; Detailed report
  (race-details-report "HUMAN")
  
  ;; Extract to file
  (extract-races-edn)
  (extract-race-relations-edn)
  
  ;; Find specific race
  (find-race "HUMAN")
  (find-race "DONDORIAN")
  
  ;; List races
  (list-races)
  (list-playable)
  
  ;; List boosts for a race
  (list-race-boosts "HUMAN")
  
  ;; === Sprite Export ===
  
  ;; Export single race sprites (for testing)
  ;; Use game race keys like "HUMAN", "Q_AMEVIA", "DONDORIAN"
  (export-single-race-sprites "HUMAN" "output/wiki/sprites/races")
  
  ;; Export sheet sprites only (standing/walking, 18 actions × 2)
  (export-race-sheet-sprites "HUMAN" "output/wiki/sprites/races")
  
  ;; Export lay sprites only (lying down, 12 positions × 2)
  (export-race-lay-sprites "HUMAN" "output/wiki/sprites/races")
  
  ;; Export all race sprites (all 8 races)
  (extract-race-sprites "output/wiki")
  
  ;; Build sprite metadata for wiki
  (take 1 (build-race-sprite-metadata))
  
  ;; Race key to sprite name mapping
  (get-race-sprite-name "Q_AMEVIA")  ; => "Amevia"
  (get-race-sprite-name "HUMAN")     ; => "Human"
  
  ;; === Full Extraction (data + sprites) ===
  (extract-all "output/wiki")
  
  :rcf)

