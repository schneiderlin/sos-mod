(ns repl.utils
  (:import [java.lang.reflect Field]))

(defn get-field-value [instance field-name]
  (let [class (.getClass instance)
        field (.getDeclaredField class field-name)
        _ (.setAccessible field true)]
    (.get field instance)))
