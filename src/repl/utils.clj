(ns repl.utils
  (:import 
   [java.lang.reflect Field Method Constructor Proxy InvocationHandler]
   [game GAME]
   [script SCRIPT ScriptEngine ScriptLoad]
   [your.mod InstanceScript]))

(defn get-field-value [instance field-name]
  (let [class (.getClass instance)
        field (.getDeclaredField class field-name)
        _ (.setAccessible field true)]
    (.get field instance)))

;; ============================================================================
;; Standalone Updater - Works without mod system
;; ============================================================================

(defonce ^:private standalone-updaters (atom {}))
(defonce ^:private standalone-script-instance (atom nil))
(defonce ^:private standalone-script-registered (atom false))

(defn- create-standalone-script-instance 
  "Create a SCRIPT_INSTANCE using Java Proxy to work around Clojure's proxy limitations."
  []
  (let [script-instance-interface (Class/forName "script.SCRIPT$SCRIPT_INSTANCE")
        handler (reify InvocationHandler
                  (invoke [this proxy method args]
                    (let [method-name (.getName method)]
                      (cond
                        (= "update" method-name)
                        (let [ds (first args)]
                          (doseq [[_ f] @standalone-updaters]
                            (try
                              (f ds)
                              (catch Exception e
                                (println "Error in standalone updater:" (.getMessage e))
                                (.printStackTrace e)))))
                        
                        (= "save" method-name) nil
                        (= "load" method-name) nil
                        (= "hoverTimer" method-name) nil
                        (= "render" method-name) nil
                        (= "mouseClick" method-name) nil
                        (= "hover" method-name) nil
                        (= "handleBrokenSavedState" method-name) false
                        (= "toString" method-name) "StandaloneScriptInstance"
                        (= "equals" method-name) (= proxy (first args))
                        (= "hashCode" method-name) (System/identityHashCode proxy)
                        :else
                        (throw (UnsupportedOperationException. 
                                (str "Method not implemented: " method-name)))))))]
    (Proxy/newProxyInstance 
     (.getClassLoader script-instance-interface)
     (into-array Class [script-instance-interface])
     handler)))

(comment
  (create-standalone-script-instance)
  :rcf)

(defn- find-instancescript-in-loads 
  "Try to find an existing InstanceScript in ScriptEngine's loads.
   If found, we can use it directly.
   
   return: InstanceScript or nil"
  []
  (try
    (let [script-engine (GAME/script)]
      (when (nil? script-engine)
        (throw (Exception. "ScriptEngine not available")))
      
      (let [
            
            ;; Get the loads field
            loads-field (.getDeclaredField ScriptEngine "loads")
            _ (.setAccessible loads-field true)
            loads (.get loads-field script-engine)
            
            ;; Get size and iterate
            size-method (.getMethod (.getClass loads) "size" (into-array Class []))
            size (.invoke size-method loads (into-array Object []))
            get-method (.getMethod (.getClass loads) "get" (into-array Class [Integer/TYPE]))]
        
        ;; Check each script for InstanceScript
        (loop [i 0]
          (if (< i size)
            (let [script-obj (.invoke get-method loads (into-array Object [i]))
                  script-class (.getClass script-obj)
                  ins-field (.getDeclaredField script-class "ins")
                  _ (.setAccessible ins-field true)
                  ins (.get ins-field script-obj)]
              (if (instance? (Class/forName "your.mod.InstanceScript") ins)
                (do
                  (println "Found existing InstanceScript in ScriptEngine, using it.")
                  ins)
                (recur (inc i))))
            nil))))
    (catch Exception e
      nil)))

(comment
  (find-instancescript-in-loads)
  :rcf)

(defn- register-standalone-script 
  "Register our standalone script instance with ScriptEngine using reflection.
   This is complex because ScriptEngine requires a ScriptLoad, which is hard to create.
   Instead, we try to find an existing InstanceScript and use it, or create a workaround.
     
   return: true if registered successfully, false otherwise"
  []
  (when-not @standalone-script-registered
    (try
      ;; First, try to find existing InstanceScript
      (if-let [existing-instance (find-instancescript-in-loads)]
        (do
          (reset! standalone-script-instance existing-instance)
          (reset! standalone-script-registered true)
          (println "Using existing InstanceScript instance."))
        
        ;; If not found, try to create our own Script entry
        (let [script-engine (GAME/script)
              _ (when (nil? script-engine)
                  (throw (Exception. "ScriptEngine not available. Game may not be initialized.")))
              
              ;; Create our standalone script instance
              script-instance (create-standalone-script-instance)
              
              ;; Try to create a ScriptLoad - this requires a SCRIPT, className, and file
              ;; We'll create a minimal SCRIPT implementation
              minimal-script (reify SCRIPT
                               (name [_] "StandaloneUpdater")
                               (desc [_] "Standalone updater for REPL")
                               (isSelectable [_] false)
                               (forceInit [_] true)
                               (initBeforeGameCreated [_] nil)
                               (initBeforeGameInited [_] nil)
                               (createInstance [_] script-instance))
              
              ;; Create ScriptLoad using reflection
              script-load-constructor (.getDeclaredConstructor ScriptLoad 
                                                               (into-array Class [SCRIPT String String]))
              _ (.setAccessible script-load-constructor true)
              script-load (.newInstance script-load-constructor 
                                        (into-array Object [minimal-script 
                                                            "StandaloneUpdater" 
                                                            "standalone.jar"]))
              
              ;; Verify ScriptLoad was created successfully
              _ (when (nil? script-load)
                  (throw (Exception. "Failed to create ScriptLoad")))
              
              ;; Get the Script inner class - inner classes use $ not $ in Class.forName
              script-class (Class/forName "script.ScriptEngine$Script")
              script-constructor (.getDeclaredConstructor script-class (into-array Class [ScriptLoad]))
              _ (.setAccessible script-constructor true)
              script-obj (.newInstance script-constructor (into-array Object [script-load]))
              
              ;; Verify Script was created successfully
              _ (when (nil? script-obj)
                  (throw (Exception. "Failed to create Script object")))
              
              ;; Verify the load field is set (should be set by constructor)
              load-field (.getDeclaredField script-class "load")
              _ (.setAccessible load-field true)
              load-value (.get load-field script-obj)
              _ (when (nil? load-value)
                  (throw (Exception. "Script.load field is null after construction")))
              
              ;; Set the ins field on the Script object
              ins-field (.getDeclaredField script-class "ins")
              _ (.setAccessible ins-field true)
              _ (.set ins-field script-obj script-instance)
              
              ;; Verify ins field was set
              ins-value (.get ins-field script-obj)
              _ (when (nil? ins-value)
                  (throw (Exception. "Failed to set Script.ins field")))
              
              ;; Get the loads field and add our script
              loads-field (.getDeclaredField ScriptEngine "loads")
              _ (.setAccessible loads-field true)
              loads (.get loads-field script-engine)
              add-method (.getMethod (.getClass loads) "add" (into-array Class [Object]))
              _ (.invoke add-method loads (into-array Object [script-obj]))]
          
          (reset! standalone-script-instance script-instance)
          (reset! standalone-script-registered true)
          (println "Standalone updater registered successfully with ScriptEngine.")))
      
      (catch Exception e
        (println "Could not register standalone script with ScriptEngine:")
        (println "  Error:" (.getMessage e))
        (println "  Stack trace:")
        (.printStackTrace e)
        (println "  Note: Standalone updaters will be registered but won't execute")
        (println "  until the mod is loaded or InstanceScript is available.")
        (println "  Try using InstanceScript directly if the mod is loaded.")
        (reset! standalone-script-registered true)))))

(defn reset-standalone-updater
  "Reset the standalone updater registration state.
   This is useful when you know a new game has been loaded and want to force re-registration.
   
   After calling this, the next call to add-standalone-updater or add-updater will re-register."
  []
  (reset! standalone-script-registered false)
  (reset! standalone-script-instance nil)
  (println "Standalone updater registration state reset. Will re-register on next updater add."))

(comment
  (register-standalone-script)
  (reset-standalone-updater)
  :rcf)

(defn add-standalone-updater
  "Add an updater function that will be called every game update cycle.
   Works even if the mod is not loaded by hooking directly into ScriptEngine.
   
   Note: If you load a new game, call (reset-standalone-updater) first to clear stale state.
   
   Args:
     key - Unique identifier for this updater
     f - Function that takes delta time (double) as argument
   
   Returns:
     true if registered successfully, false otherwise"
  [key f]
  (swap! standalone-updaters assoc key f)
  
  ;; Try to register with ScriptEngine if not already done
  (when-not @standalone-script-registered
    (register-standalone-script))
  
  true)

(defn remove-standalone-updater
  "Remove an updater function.
   
   Args:
     key - The identifier used when adding the updater"
  [key]
  (swap! standalone-updaters dissoc key))

;; ============================================================================
;; Hybrid Updater - Tries InstanceScript first, falls back to standalone
;; ============================================================================

(def ^:private use-instancescript (atom true))

(defn- instancescript-available? 
  "Check if InstanceScript is actually loaded and will be called by the game.
     Just checking if addConsumer works is not enough - we need to verify
     that InstanceScript is registered in ScriptEngine."
  []
  (try
    (some? (find-instancescript-in-loads))
    (catch Exception _e
      false)))

(defn add-updater
  "Add an updater function. Tries InstanceScript first if available, falls back to standalone.
   
   Automatically handles game reloads by checking if InstanceScript or standalone script
   is still registered and re-registering if needed.
   
   Args:
     key - Unique identifier
     f - Function that takes delta time (double) as argument
   
   Note: InstanceScript/addConsumer will always succeed, but if the mod isn't loaded,
   InstanceScript.update() is never called, so consumers won't execute. This function
   checks if InstanceScript is actually registered in ScriptEngine before using it."
  [key f]
  (if (and @use-instancescript (instancescript-available?))
    (do
      (InstanceScript/addConsumer key f)
      true)
    (do
      ;; InstanceScript not available or not working, use standalone
      (when @use-instancescript
        (reset! use-instancescript false)
        (println "InstanceScript not loaded, using standalone updater instead."))
      (add-standalone-updater key f))))

(comment
  (add-updater "test" (fn [ds] (println "test" ds)))
  :rcf)

(defn remove-updater
  "Remove an updater function.
   
   Args:
     key - The identifier used when adding the updater"
  [key]
  (if @use-instancescript
    (try
      (InstanceScript/removeConsumer key)
      (catch Exception e
        (remove-standalone-updater key)))
    (remove-standalone-updater key)))

(comment
  (remove-updater "test")
  :rcf)

;; ============================================================================
;; update-once - Execute function once in next update cycle
;; ============================================================================

(def ^:private update-once-counter (atom 0))

(defn update-once
  "Execute a function once in the next game update cycle.
   The function will receive the delta time (ds) as its argument.
   After execution, the consumer is automatically removed.
   
   This function works with or without the mod system:
   - If mod is loaded: Uses InstanceScript (faster, cleaner)
   - If mod not loaded: Uses standalone updater (hooks into ScriptEngine)
   
   Args:
     f - Function that takes delta time (double) as argument"
  [f]
  (let [consumer-id (str "update-once-" (swap! update-once-counter inc))
        new-f (fn [ds]
                (try
                  (f ds)
                  (finally
                    ;; Always remove the consumer, even if f throws an exception
                    (remove-updater consumer-id))))]
    (add-updater consumer-id new-f)))

(comment
  (update-once (fn [ds] (println "test" ds)))
  :rcf)

(defn- boxed-to-primitive-type
  "Convert a boxed type to its primitive equivalent if it exists."
  [clazz]
  (cond
    (= (.getName clazz) "java.lang.Byte") Byte/TYPE
    (= (.getName clazz) "java.lang.Short") Short/TYPE
    (= (.getName clazz) "java.lang.Integer") Integer/TYPE
    (= (.getName clazz) "java.lang.Long") Long/TYPE
    (= (.getName clazz) "java.lang.Float") Float/TYPE
    (= (.getName clazz) "java.lang.Double") Double/TYPE
    (= (.getName clazz) "java.lang.Boolean") Boolean/TYPE
    (= (.getName clazz) "java.lang.Character") Character/TYPE
    :else clazz))

(defn- get-param-types
  "Get parameter types from arguments, handling primitives correctly."
  [args]
  (if (empty? args)
    (into-array Class [])
    (into-array Class (map (fn [arg]
                             (if (nil? arg)
                               Object
                               (boxed-to-primitive-type (.getClass arg))))
                           args))))

(defn invoke-method
  "Invoke a method on an instance using reflection, bypassing access restrictions.
   Works with both public and package-private methods.
   Handles primitive types correctly (int, long, etc.).
   
   Args:
     instance - The object instance
     method-name - Name of the method (string)
     args - Method arguments
   
   Example:
     (invoke-method warehouse \"allocateCrate\" resource 5)"
  [instance method-name & args]
  (let [class (.getClass instance)
        param-types (get-param-types args)
        ;; Try public method first, then fall back to declared method (for package-private)
        method (try
                 (.getMethod class method-name param-types)
                 (catch NoSuchMethodException _
                   (try
                     (.getDeclaredMethod class method-name param-types)
                     (catch NoSuchMethodException e
                       (throw (Exception. (str "Method not found: " method-name 
                                               " with parameter types: " 
                                               (vec param-types)) e))))))
        _ (.setAccessible method true)]
    (.invoke method instance (into-array Object args))))
