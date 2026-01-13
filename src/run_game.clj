(ns run-game
  (:import [init MainProcess]))

(defn -main
  "Runs the game's MainProcess main class with the given arguments."
  [& args]
  (future
    (let [main-class (Class/forName "init.MainProcess" true (.getClassLoader MainProcess))
          string-array-type (type (into-array String []))
          main-method (.getMethod main-class "main" (into-array Class [string-array-type]))
          args-array (into-array String args)]
      (.setAccessible main-method true)
      (.invoke main-method nil (into-array Object [args-array])))))

(comment
  (-main)

  *1
  :rcf)



