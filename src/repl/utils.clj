(ns repl.utils
  (:import 
   [java.lang.reflect Field]
   [your.mod InstanceScript]))

(defn get-field-value [instance field-name]
  (let [class (.getClass instance)
        field (.getDeclaredField class field-name)
        _ (.setAccessible field true)]
    (.get field instance)))

(defn update-once [f]
  (let [new-f (fn [ds]
                (f ds)
                (InstanceScript/removeConsumer "test"))]
    (InstanceScript/addConsumer "test" new-f)))