(ns game.common
  (:import
   [view.main VIEW]
   [snake2d.util.sets ArrayListResize LIST]))

(defn array-list->vec [^LIST array-list]
  (let [size (.size array-list)
        indices (range size)]
    (map (fn [idx]
           (.get array-list idx))
         indices)))

(defn array-list-resize->vec [^ArrayListResize array-list]
  (let [size (.size array-list)
        indices (range size)]
    (map (fn [idx]
           (.get array-list idx))
         indices)))

(defn focus [{:keys [cX cY]}]
  (let [sett-view (VIEW/s)
        game-window (.getWindow sett-view)]
    (.centerAt game-window cX cY)))

(defn focus-entity [entity]
  (let [body (.body entity)]
    (focus {:cX (.cX body) :cY (.cY body)})))

(comment
  
(require '[game.humanoid :as humanoid])

  (-> (humanoid/all-entities)
      (nth 0)
      (focus-entity))
  :rcf)
