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

;; 建筑相关
(comment
  (def rooms (SETT/ROOMS))

  ;; 王座
  (def throne (.-THRONE rooms))
  
  (let [info (.-info throne)]
    [(.-name info)
     (.desc info)])
  
  (import '[settlement.room.main.throne THRONE])
  (THRONE/tile)
  (THRONE/coo)

  ;; 和水相关的设施, 不是地图上的河流
  (def room-water (.-WATER rooms))
  ;; 水渠 和 drain, 暂时不知道是什么
  (let [canal (.-canal room-water)
        drain (.-drain room-water)]
    [canal drain])
  
  ;; 住宅类型, 不是 instance. ChamberInstance 才是实例
  (def room-chamber (.-CHAMBER rooms))
  ;; 这个可能是内饰? 
  (def furnisher (.constructor room-chamber))
  {:heavy (.isHeavy furnisher)
   :mustBeIndoors (.mustBeIndoors furnisher)
   :needsIsolation (.needsIsolation furnisher)
   :needFlooring (.needFlooring furnisher)
   :mustBeOutdoors (.mustBeOutdoors furnisher)
   :usesArea (.usesArea furnisher) ;; 使用区域就是需要自己拉格子然后摆放那种, 否则就是水井那种固定的
   :canBeCopied (.canBeCopied furnisher)}
  
  ;; 厕所
  (def room-janitor (.-JANITOR rooms))
  (let [furnisher (.constructor room-janitor)]
    {:heavy (.isHeavy furnisher)
     :mustBeIndoors (.mustBeIndoors furnisher)
     :needsIsolation (.needsIsolation furnisher)
     :needFlooring (.needFlooring furnisher)
     :mustBeOutdoors (.mustBeOutdoors furnisher)
     :usesArea (.usesArea furnisher)
     :canBeCopied (.canBeCopied furnisher)})

  
  :rcf)

;; 创建工地

(comment
  ;; 创建3x3大小的住宅
  (def center-x 100)
  (def center-y 100)
  
  ;; 获取住宅相关的
  (def rooms (SETT/ROOMS))
  (def home (.-HOME rooms))
  (def home-constructor (.constructor home))
  
  ;; 获取Field对象
  (require '[repl.utils :refer [get-field-value]])
  (def tmp-area (get-field-value rooms "tmpArea"))
  (get-field-value tmp-area "lastUser")

  ;; this可以是任何Object
  ;; 这些操作必须在单个 frame 里面完成, 否则 render 和 update 的时候会报错
  (def tmp (.tmpArea rooms "1"))
  
  ;; // 设置建造区域（3x3的房子）
  ;; int size = 3;
  ;; for (int y = 0; y < size; y++) {
  ;;     for (int x = 0; x < size; x++) {
  ;;         tmp.set(centerX - 1 + x, centerY - 1 + y);
  ;;     }
  ;; }
  
  ;; // 创建ConstructionInit
  ;; TBuilding structure = SETT.TERRAIN().BUILDINGS.get("WOOD");  // 木制建筑
  ;; ConstructionInit init = new ConstructionInit(
  ;;     0,                       // 无升级
  ;;     homeConstructor,         // 住宅构造器
  ;;     structure,               // 室内建筑
  ;;     0,                       // 无退化
  ;;     null                     // 无状态
  ;; );
  
  ;; // 创建工地
  ;; SETT.ROOMS().construction.createClean(tmp, init);
  
  ;; // 必须清理！
  ;; tmp.clear();
  :rcf)

