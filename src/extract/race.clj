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
;; Main Extraction Functions
;; ============================================

(defn extract-all
  "Extract all race data."
  ([] (extract-all *output-dir*))
  ([output-dir]
   (println "Extracting races to:" output-dir)
   (extract-races-edn (str output-dir "/data"))
   (extract-race-relations-edn (str output-dir "/data"))
   (println "Done!")))

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
  
  ;; Full extraction
  (extract-all "output/wiki")
  
  :rcf)

