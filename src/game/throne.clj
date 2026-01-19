(ns game.throne
  (:import
   [view.main VIEW]
   [settlement.room.main.throne THRONE]))

(defn throne-position
  "Get the throne tile coordinates."
  []
  (let [coo (THRONE/coo)]
    {:x (int (.x coo))
     :y (int (.y coo))}))

(defn- get-game-window
  "Get the game window for camera control."
  []
  (let [sett-view (VIEW/s)]
    (.getWindow sett-view)))

(defn move-to-throne
  "Move camera to the throne position.
   Gets the throne's coordinate (in tiles) and moves the camera to center on it."
  []
  (let [throne-coo (THRONE/coo)
        window (get-game-window)
        tile-x (.x throne-coo)
        tile-y (.y throne-coo)]
    (.centerAtTile window tile-x tile-y)))

(comment
  ;; Get throne position
  (throne-position)
  
  ;; Move camera to throne
  (move-to-throne)
  :rcf)

