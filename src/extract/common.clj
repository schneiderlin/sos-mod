(ns extract.common
  "Common utilities for extract namespaces.
   
   This namespace provides shared file output utilities used across
   all extract namespaces to avoid duplication.
   
   Usage:
     (require '[extract.common :as common])
     (common/save-edn-pretty data \"path/to/file.edn\")
   "
  (:require
   [clojure.pprint])
  (:import
   [java.io File]))

;; ============================================
;; File Output Utilities
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

