(ns extract.religion
  "Religion data extraction for wiki.
   
   This namespace extracts all religion data and exports to EDN/JSON.
   Uses game.religion for accessing religion data.
   
   Usage:
     (require '[extract.religion :as rel-extract])
     (rel-extract/extract-all \"output/wiki\")
   "
  (:require
   [game.religion :as rel]
   [clojure.java.io :as io])
  (:import
   [java.io File]))

;; ============================================
;; Configuration
;; ============================================

(def ^:dynamic *output-dir* "output/wiki")

;; ============================================
;; Enhanced Data Extraction
;; ============================================

(defn extract-religion-full
  "Extract full religion data including icon paths."
  [religion]
  (let [base (rel/religion->map religion)
        key (:key base)]
    (assoc base
           :icon-path (str "sprites/religions/" key "/icon.png"))))

(defn extract-all-religions
  "Extract all religions with full data."
  []
  (mapv extract-religion-full (rel/all-religions)))

;; ============================================
;; Aggregate Data Structure
;; ============================================

(defn build-religions-data
  "Build complete religions data structure for wiki."
  []
  {:version "1.0"
   :extracted-at (str (java.time.Instant/now))
   :summary {:total-religions (rel/religion-count)
             :religions-by-inclination (->> (rel/all-religions)
                                            (map (fn [r]
                                                   {:key (rel/religion-key r)
                                                    :inclination (rel/religion-inclination r)}))
                                            (sort-by :inclination >)
                                            vec)}
   :religions (extract-all-religions)
   :opposition-matrix (rel/get-opposition-matrix)})

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
;; Individual Exports
;; ============================================

(defn extract-religions-edn
  "Extract all religions to EDN file."
  ([] (extract-religions-edn (str *output-dir* "/data")))
  ([output-dir]
   (let [data (build-religions-data)
         path (str output-dir "/religions.edn")]
     (save-edn-pretty data path)
     data)))

(defn extract-opposition-matrix
  "Extract just the opposition matrix."
  ([] (extract-opposition-matrix (str *output-dir* "/data")))
  ([output-dir]
   (let [data {:version "1.0"
               :extracted-at (str (java.time.Instant/now))
               :opposition-matrix (rel/get-opposition-matrix)}
         path (str output-dir "/religion-opposition.edn")]
     (save-edn-pretty data path)
     data)))

;; ============================================
;; Summary and Reports
;; ============================================

(defn extract-religions-summary
  "Print summary of religions."
  []
  (let [data (build-religions-data)]
    (println "=== Religions Summary ===")
    (println "Total religions:" (:total-religions (:summary data)))
    (println)
    (println "=== Religions by Inclination ===")
    (doseq [{:keys [key inclination]} (:religions-by-inclination (:summary data))]
      (println (str "  " key ": " (format "%.2f" inclination))))
    (println)
    (println "=== Religion Details ===")
    (doseq [r (:religions data)]
      (println (str "  " (:key r) " - " (:name r)))
      (println (str "    Deity: " (:deity r)))
      (println (str "    Inclination: " (format "%.2f" (:inclination r))))
      (println (str "    Color: " (:hex (:color r))))
      (when-let [boosts (:boosts r)]
        (println (str "    Boosts: " (count boosts)))))
    (println)))

(defn opposition-report
  "Generate an opposition report showing relationships between religions."
  []
  (println "=== Religion Opposition Report ===")
  (println)
  (let [rels (rel/all-religions)
        keys (mapv rel/religion-key rels)]
    ;; Header
    (print "          ")
    (doseq [k keys]
      (print (format "%10s" (subs k 0 (min 8 (count k))))))
    (println)
    (println (apply str (repeat (+ 10 (* 10 (count keys))) "-")))
    ;; Rows
    (doseq [r1 rels]
      (print (format "%10s" (subs (rel/religion-key r1) 0 (min 8 (count (rel/religion-key r1))))))
      (doseq [r2 rels]
        (let [opp (rel/religion-opposition r1 r2)]
          (if (= (rel/religion-key r1) (rel/religion-key r2))
            (print "       -  ")
            (print (format "%10.2f" opp)))))
      (println))))

;; ============================================
;; Main Extraction Functions
;; ============================================

(defn extract-all
  "Extract all religion data."
  ([] (extract-all *output-dir*))
  ([output-dir]
   (println "Extracting religions to:" output-dir)
   (extract-religions-edn (str output-dir "/data"))
   (println "Done!")))

(comment
  (extract-all)
  :rcf)

;; ============================================
;; Individual Queries
;; ============================================

(defn find-religion
  "Find and display religion info by key."
  [key]
  (if-let [r (rel/get-religion key)]
    (rel/religion->map r)
    (println "Religion not found:" key)))

(defn list-religions
  "List all religions with basic info."
  []
  (rel/all-religions-basic))

(defn list-deities
  "List all religions and their deities."
  []
  (mapv (fn [r]
          {:key (rel/religion-key r)
           :name (rel/religion-name r)
           :deity (rel/religion-deity r)})
        (rel/all-religions)))

(defn find-rivals
  "Find the most and least opposed religions for a given religion key."
  [key]
  (if-let [r (rel/get-religion key)]
    (let [most (rel/find-most-opposed r)
          least (rel/find-least-opposed r)]
      {:religion key
       :most-opposed (when most
                       {:key (rel/religion-key most)
                        :opposition (rel/religion-opposition r most)})
       :least-opposed (when least
                        {:key (rel/religion-key least)
                         :opposition (rel/religion-opposition r least)})})
    (println "Religion not found:" key)))

(comment
  ;; === Quick Test ===
  
  ;; Check if game is loaded
  (rel/religion-count)
  
  ;; Extract summary
  (extract-religions-summary)
  
  ;; Opposition report
  (opposition-report)
  
  ;; Extract to file
  (extract-religions-edn)
  (extract-opposition-matrix)
  
  ;; Find specific religion
  (find-religion "CRATOR")
  
  ;; List all religions
  (list-religions)
  
  ;; List deities
  (list-deities)
  
  ;; Find rivals
  (find-rivals "CRATOR")
  
  ;; Full extraction
  (extract-all "output/wiki")
  
  :rcf)

