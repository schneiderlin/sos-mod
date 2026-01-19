(ns game.humanoid
  (:require [game.settlement :as settlement])
  (:import
   [game GAME]
   [view.main VIEW]
   [settlement.main SETT]
   [settlement.stats STATS]
   [settlement.entity.humanoid Humanoid]
   [settlement.room.main.construction ConstructionInit]
   [settlement.room.main.placement UtilWallPlacability]
   [settlement.room.main.placement PLACEMENT]))

(defn humanoid-info [entity]
  (when (instance? Humanoid entity)
    (let [stats-population (STATS/POP)
          stats-population-age (.-age stats-population) 
          stats-work (STATS/WORK)
          stat-work-time (.-WORK_TIME stats-work) 
          stats-needs (STATS/NEEDS)
          stat-exposure (.-EXPOSURE stats-needs)
          stat-danger (.-INJURIES stats-needs)
          stat-exhaustion (.-EXHASTION stats-needs)
          stats-appearance (STATS/APPEARANCE)
          ^util.data.GETTER_TRANS$GETTER_TRANSE friend-trans (.-FRIEND stats-population)
          induvidual (.indu entity)
          body (.body entity)]
      {:lifespan (.lifespan stats-population-age induvidual)
       :deathDay (.deathDay stats-population-age induvidual)
       :isAdult (.isAdult stats-population-age induvidual)
       :shouldDieOfOldAge (.shouldDieOfOldAge stats-population-age induvidual)
       :critical (.critical stat-exposure induvidual)
       :isCold (.isCold stat-exposure induvidual)
       :inDanger (.inDanger stat-danger induvidual)
       :willDie (.willDie stat-danger induvidual 0)
       :name (.toString (.name stats-appearance induvidual))
       :friend (.get friend-trans induvidual) 
       :cX (.cX body)
       :cY (.cY body)
       :class (.toString (.clas induvidual))})))

(comment
  (humanoid-info (first (settlement/all-entities)))

  (def entities (settlement/all-entities))

  ;; 9 个猪人小孩
  ;; 36 个人类小孩
  ;; 左上角 553 人口
  ;; 加起来 553 + 9 + 36 = 598. 但是下面能查到 601 个名字??
  (def names
    (->> entities
         (map humanoid-info)
         (keep identity)
         (map (comp #(.toString %) :name))
         (into #{})
         #_count))

  ;; 哪些人有朋友
  (->> entities
       (map humanoid-info)
       (keep identity)
       (map :friend)
       (keep identity)
       count)
  :rcf)

(defn friendship-edn []
  (let [stats-appearance (STATS/APPEARANCE)
        humanoids (->> (settlement/all-entities)
                       (map humanoid-info)
                       (keep identity))
        ;; Get all names including friends
        all-names (atom #{})
        links (atom [])]

    ;; Process each humanoid to extract friendships
    (doseq [h humanoids]
      (when-let [name-obj (:name h)]
        (let [name-str (str name-obj)]
          (swap! all-names conj name-str)

          ;; If this humanoid has a friend, extract friend's name and create a link
          (when-let [friend-obj (:friend h)]
            ;; friend-obj might be a Humanoid, so extract its induvidual if needed
            (let [friend-induvidual (if (instance? Humanoid friend-obj)
                                      (.indu friend-obj)
                                      friend-obj)]
              (when-let [friend-name-obj (.name stats-appearance friend-induvidual)]
                (let [friend-name (str friend-name-obj)]
                  (swap! all-names conj friend-name)
                  (swap! links conj {:source name-str
                                     :target friend-name
                                     :type "friend"}))))))))

    ;; Build the final data structure
    (let [data {:nodes (sort-by :id
                                (map (fn [name] {:id name})
                                     @all-names))
                :links @links}]
      data)))

(comment
  (friendship-edn)
  :rcf)

