(ns repl.utils
  (:import 
   [java.lang.reflect Field]
   [your.mod InstanceScript]))

(defn get-field-value [instance field-name]
  (let [class (.getClass instance)
        field (.getDeclaredField class field-name)
        _ (.setAccessible field true)]
    (.get field instance)))

(def ^:private update-once-counter (atom 0))

(defn update-once
  "Execute a function once in the next game update cycle.
   The function will receive the delta time (ds) as its argument.
   After execution, the consumer is automatically removed.
   
   This uses InstanceScript which is available in the classpath.
   Note: For this to work, the mod must be loaded by the game.
   If the mod is not loaded, InstanceScript.update() won't be called,
   and your function won't execute. Make sure the mod is properly
   installed in the mods folder with the correct V70 folder structure."
  [f]
  (let [consumer-id (str "update-once-" (swap! update-once-counter inc))
        new-f (fn [ds]
                (try
                  (f ds)
                  (finally
                    ;; Always remove the consumer, even if f throws an exception
                    (InstanceScript/removeConsumer consumer-id))))]
    (InstanceScript/addConsumer consumer-id new-f)))

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
