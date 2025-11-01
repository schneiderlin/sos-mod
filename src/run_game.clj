(ns run-game
  (:import [init MainProcess]))

(defn -main
  "Runs the game's MainProcess main class with the given arguments."
  [& args]
  (MainProcess/main (into-array String args)))

