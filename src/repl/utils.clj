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

(defn invoke-method
  "Invoke a method on an instance using reflection, bypassing access restrictions."
  [instance method-name & args]
  (let [class (.getClass instance)
        param-types (if (empty? args)
                      (into-array Class [])
                      (into-array Class (map #(.getClass %) args)))
        method (.getMethod class method-name param-types)
        _ (.setAccessible method true)]
    (.invoke method instance (into-array Object args))))
