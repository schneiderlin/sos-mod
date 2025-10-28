(ns repl.core
  (:import [game GAME]
           [settlement.main SETT]
           [settlement.stats STATS]))

(comment
  (def world (GAME/world))
  (def settlement (GAME/s))
  (def interval (GAME/intervals))
  (def factions (GAME/factions))
  (def events (GAME/events))
  (def raiders (GAME/raiders))

  
  :rcf)

;; player 相关
(comment
  (def player (GAME/player))

  (def tech (.tech player))
  (def titles (.titles player))
  
  ;; 这些都是 全局的 race 数据, 不是 player 特有的
  (def races (.races player))
  (-> (.get races 0)
      (.population)
      (.-growth))
  (-> (.get races 0)
      (.population)
      (.-max))
  (-> (.get races 0)
      (.population)
      (.-immigrantsPerDay))
  
  (def race0 (.get races 0)) ;; 猪人
  (def race1 (.get races 1)) ;; 邓多里安人
  
  (.citizens player race0)
  (.citizens player race1) 

  (.get races 1)

  (def emissaries (.emissaries player))
  (def trade (.trade player))
  (def rulerName (.rulerName player))
  (def desc (.desc player))
  :rcf)

;; stats 相关
(comment
  (def stats-population (STATS/POP))
  (def stats-population-age (.-age stats-population))
  
  ;; 工作相关
  (def stats-work (STATS/WORK))
  (def stat-work-time (.-WORK_TIME stats-work))


  ;; 需求相关
  (def stats-needs (STATS/NEEDS)) 
  (def stat-exposure (.-EXPOSURE stats-needs))
  (def stat-danger (.-INJURIES stats-needs))
  (def stat-exhaustion (.-EXHASTION stats-needs))
  
  ;; 这里面都是空的, 暂时没用到
;;   (def stat-other-needs (.-OTHERS stats-needs))
;;   (.size stat-other-needs)
;;   (def stat-other-need1 (get stat-other-needs 6))


  :rcf)

;; settlement 相关
(comment
  (def settlement (GAME/s))
  (def city (SETT/CITY))

  (def humanoids-instance (SETT/HUMANOIDS))
  (def animals-instance (SETT/ANIMALS))
  
  (def animals (.sett animals-instance))
  (.size animals)

  (-> (.-species animals-instance)
      (.getAt 0)
      (.key))
  (-> (.-species animals-instance)
      (.getAt 1)
      (.key))
  (-> (.-species animals-instance)
      (.getAt 2)
      (.key))
  (-> (.-species animals-instance)
      (.getAt 3)
      (.key))
  (-> (.-species animals-instance)
      (.getAt 4)
      (.key))
  (-> (.-species animals-instance)
      (.getAt 5)
      (.key))
  (-> (.-species animals-instance)
      (.getAt 6)
      (.key))
  (-> (.-species animals-instance)
      (.getAt 7)
      (.key))

  (def entities-instance (SETT/ENTITIES))
  (.size entities-instance)
  (def entities (.getAllEnts entities-instance))

  (def non-nil-entities (filter #(not (nil? %)) entities))
  (def entity (first non-nil-entities))

  ;; entity 如果是 Humanoid, 可以获取他的 Induvidual
  (def induvidual (.indu entity))
  (.player induvidual)
  (.hostile induvidual)

  ;; 各种 Stats 数据需要用 induvidual 来查询
  (.lifespan stats-population-age induvidual)
  (.deathDay stats-population-age induvidual)
  (.isAdult stats-population-age induvidual)
  (.shouldDieOfOldAge stats-population-age induvidual)

  (.critical stat-exposure induvidual)
  (.isCold stat-exposure induvidual)

  (.inDanger stat-danger induvidual)
  (.willDie stat-danger induvidual 0)
  (.critical stat-danger induvidual)

  ;; STAT 是通过 indu 然后再 getD 获取
  (let [int-oe (.indu stat-work-time #_stat-exhaustion)
        d (.getD int-oe induvidual)]
    d) 
  
  ;; STAT 的信息
  (let [info (.info stat-work-time #_stat-exhaustion)]
    [(.name info)
     (.desc info)])
  
  :rcf)

