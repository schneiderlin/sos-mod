(ns repl.core
  (:require
   [repl.utils :refer [get-field-value update-once]])
  (:import
   [game GAME]
   [view.main VIEW]
   [settlement.main SETT]
   [settlement.stats STATS]
   [settlement.entity.humanoid Humanoid]
   [settlement.room.main.construction ConstructionInit]
   [settlement.room.main.placement UtilWallPlacability]
   [settlement.room.main.placement PLACEMENT]
   [your.mod InstanceScript]))

(def world (GAME/world))
(def settlement (GAME/s))
(def interval (GAME/intervals))
(def factions (GAME/factions))
(def events (GAME/events))
(def raiders (GAME/raiders))
(def sett-view (VIEW/s))

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

;; 名称相关
(def stats-appearance (STATS/APPEARANCE))

;; 关系相关
(def ^util.data.GETTER_TRANS$GETTER_TRANSE friend-trans (.-FRIEND stats-population))

(comment 
  (.get friend-trans induvidual)
  :rcf)

(def entities-instance (SETT/ENTITIES))
(def entities (.getAllEnts entities-instance))
(def non-nil-entities (filter #(not (nil? %)) entities))
(def entity (first non-nil-entities)) ;; 这个可能是 humanoid

(defn humanoid-info [entity]
  (when (instance? Humanoid entity)
    (let [induvidual (.indu entity)]
      {:lifespan (.lifespan stats-population-age induvidual)
       :deathDay (.deathDay stats-population-age induvidual)
       :isAdult (.isAdult stats-population-age induvidual)
       :shouldDieOfOldAge (.shouldDieOfOldAge stats-population-age induvidual)
       :critical (.critical stat-exposure induvidual)
       :isCold (.isCold stat-exposure induvidual)
       :inDanger (.inDanger stat-danger induvidual)
       :willDie (.willDie stat-danger induvidual 0)
       :name (.name stats-appearance induvidual)
       :friend (.get friend-trans induvidual)
       :class (.clas induvidual)
       })))

(comment
  (humanoid-info entity)
  :rcf)

;; 人口相关
(comment
  ;; 9 个猪人小孩
  ;; 36 个人类小孩
  ;; 左上角 553 人口
  ;; 加起来 553 + 9 + 36 = 598. 但是下面能查到 601 个名字??
  (def names
    (->> non-nil-entities
         (map humanoid-info)
         (keep identity)
         (map (comp #(.toString %) :name))
         (into #{})
         #_count))
  
  ;; 哪些人有朋友
  (->> non-nil-entities
       (map humanoid-info)
       (keep identity)
       (map :friend)
       (keep identity)
       count) 
  
  (.clas induvidual)
  :rcf)

;; Export friendship relations to EDN
(defn export-friendship-edn [filename]
  (let [humanoids (->> non-nil-entities
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
  (pr-str (export-friendship-edn "friendship.edn"))
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


  ;; entity 如果是 Humanoid, 可以获取他的 AI
  (def ai-manager (get-field-value entity "ai"))

  (let [state (.state ai-manager)]
    (.-key state))

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

(def rooms (SETT/ROOMS))
;; 获取住宅相关的 
(def home (.-HOME rooms))
(def home-constructor (.constructor home))

;; 建筑材料
(def woods (.get (.-BUILDINGS (SETT/TERRAIN)) "WOOD"))
(.get woods 0)

;; 创建工地
(comment
  ;; 测试一下 instance script 里面的 consumer
  (InstanceScript/addConsumer "test" (fn [ds] (println "test" ds)))
  (InstanceScript/removeConsumer "test")

  (update-once (fn [ds] (println "test" ds)))



  ;; 获取Field对象 
  (def tmp-area (get-field-value rooms "tmpArea"))
  (get-field-value tmp-area "lastUser")

  ;; 创建个 ConstructionInit 来测试一下
  (def construction-init (let [upgrade 0 ;; 无升级 
                               furnisher home-constructor ;; 住宅构造器
                               structure (.get woods 0) ;; 木制建筑
                               degrade 0 ;; 无退化
                               state nil ;; 无状态
                               ]
                           (ConstructionInit. upgrade furnisher structure degrade state)))


  ;; 这些操作必须在单个 frame 里面完成, 否则 render 和 update 的时候会出错
  (defn create-room [_ds]
    (let [center-x 130
          center-y 120
          construction-init (let [upgrade 0 ;; 无升级 
                                  furnisher home-constructor ;; 住宅构造器
                                  structure (.get woods 0) ;; 木制建筑
                                  degrade 0 ;; 无退化
                                  state nil ;; 无状态
                                  ]
                              (ConstructionInit. upgrade furnisher structure degrade state))
          tmp (.tmpArea rooms "1")]

      ;; 设置建造区域（3x3的房子）
      (doseq [y (range 3)
              x (range 3)]
        (.set tmp
              (+ x (- center-x 1)) (+ y (- center-y 1))))

      (let [furnisher-groups (.get (.pgroups home-constructor) 0)
            furnisher-item (.item furnisher-groups 0 0) ;; 获取第一个 FurnisherItem
            tx (- center-x 1) ;; 起始 x 坐标
            ty (- center-y 1)] ;; 起始 y 坐标
        ;; 设置 FurnisherItem 到 fData 中，这样 HomeInstance 构造时就能找到它
        (.itemSet (.fData rooms) tx ty furnisher-item (.room tmp)))

      ;; 创建工地
      (.createClean (.construction rooms) tmp construction-init)

      ;; 少了 placer place 的步骤, 所以完成建造的时候获取不到相关信息

      ;; 清除临时区域
      (.clear tmp)))

  (update-once create-room)

  (defn build-wall [_ds]
    (let [tx 110
          ty 110]
      (UtilWallPlacability/wallBuild tx ty (.get woods 0))))

  (update-once build-wall)

  (defn build-door [_ds]
    (let [tx 110
          ty 111]
      (UtilWallPlacability/openingBuild tx ty (.get woods 0))))
  (update-once build-door)


  (let [furnisher-groups (.get (.pgroups home-constructor) 0)
        furnisher-item (.item furnisher-groups 0 0)]
    {:area (.-area furnisher-item)
     :rotation (.-rotation furnisher-item)
     :multiplierCosts (.-multiplierCosts furnisher-item)
     :multiplierStats (.-multiplierStats furnisher-item)
     :width (.width furnisher-item)
     :height (.height furnisher-item)})
  :rcf)

;; 判断地形是否可以建造
(comment
  ;; 目前只是 3*3 的可以判断, cx cy 是中心点.
  (defn can-place-home [home-blueprint cx cy]
    (let [room-width 3
          room-height 3
          start-x (- cx 1)
          start-y (- cy 1)
          build-on-walls false] ; 房子可以在墙上建造（室内）
      ;; 检查所有 3x3 瓷砖是否都可以放置
      (every? (fn [[x y]]
                (let [tx (+ start-x x)
                      ty (+ start-y y)]
                  ;; PLACEMENT.placable 返回 null 如果可以放置，否则返回错误消息
                  (nil? (PLACEMENT/placable tx ty home-blueprint build-on-walls))))
              (for [y (range room-height)
                    x (range room-width)]
                [x y]))))

  (can-place-home home 130 120)
  :rcf)

;; 动态加其他东西
(comment
  (require '[datalevin.core :as d])
  :rcf)

(defn focus-entity [entity]
  (let [ game-window (.getWindow sett-view)
        body (.body entity)]
    (.centerAt game-window (.cX body) (.cY body))))

;; 移动镜头到小人位置
(comment
  (let [body (.body entity)]
    {:center [(.cX body)
              (.cY body)]
     :x1 (.x1 body)
     :y1 (.y1 body)
     :x2 (.x2 body)
     :y2 (.y2 body)})

   
  (-> non-nil-entities
      (nth 3)
      (focus-entity))

  
  (humanoid-info entity)
  (humanoid-info (nth non-nil-entities 1))
  :rcf)

