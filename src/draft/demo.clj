(ns draft.demo 
  (:require
    [game.animal :as animal]))

;; 获取资源数量
(comment
  (require '[game.warehouse :refer [total-amount-for-resource]])
  (import '[init.resources RESOURCES])
  
  (total-amount-for-resource (RESOURCES/WOOD))
  :rcf)

(comment
  ;; 查询当前有多少建筑可以用
  (require '[game.building :as building]
           '[game.warehouse :as warehouse])
  
  ;; 方法一：获取建筑蓝图总数（推荐）
  (building/blueprint-imp-count)
  
  ;; 查看前几个建筑的名称
  (->> (building/all-blueprint-imps)
       (take 10)
       (map building/room-name))
  
  ;; 查看目前定居点里面有多少个仓库
  ;; 方法一：获取仓库总数（推荐）
  (count (warehouse/all-warehouses))
  
  ;; 方法二：查看每个仓库的信息
  (->> (warehouse/all-warehouses)
       (map warehouse/warehouse-info))
  
  :rcf)

(comment
  ;; 查询目前定居点有多少小人
  (require '[game.humanoid :as humanoid]
           '[game.common :refer [focus-entity]])
  (import '[settlement.entity.humanoid Humanoid])
  
  ;; 获取所有小人实体
  (def entities (humanoid/all-entities))
  (def humanoids (filter #(instance? Humanoid %) entities))
  
  ;; 查询小人总数
  (count humanoids)
  
  ;; 获取前3个小人的名字
  (->> humanoids
       (take 3)
       (map humanoid/humanoid-info)
       (keep identity)
       (map :name))
  
  ;; 定位到第一个小人
  (when-let [first-humanoid (first humanoids)]
    (focus-entity first-humanoid))
  
  :rcf)


(comment
  ;; 演示怎么找野生动物，怎么标记一个范围内的狩猎
  (require '[game.animal :as animal]
           '[game.common :refer [focus]])
  
  ;; 1. 查找所有野生动物
  (def wild-animals (animal/wild-animals))
  (count wild-animals)  ; 查看野生动物总数
  
  ;; 2. 查看前几个野生动物的信息
  (->> wild-animals
       (take 3)
       (map animal/animal-info))
  
  ;; 3. 查找特定位置的野生动物（例如：tile 157, 483）
  (def animals-at-location (animal/animals-at-tile 157 483))
  (when animals-at-location
    (animal/animal-info animals-at-location))
  
  ;; 4. 获取一个范围内的所有动物
  ;; 参数：中心点 tile 坐标 (tx, ty) 和半径（格数）
  ;; 例如：在 tile (157, 483) 周围 15 格范围内的所有动物
  (def animals-in-range (animal/animals-in-area 157 483 15))
  (count animals-in-range)  ; 查看范围内的动物数量
  
  ;; 只获取野生动物
  (def wild-in-range (filter #(not (.domesticated %)) animals-in-range))
  (count wild-in-range)
  
  ;; 5. 标记一个范围内的野生动物用于狩猎
  ;; 在脚本层面循环，标记所有可狩猎的野生动物
  (let [tx 157
        ty 483
        radius 15
        wild-animals-in-area (->> (animal/animals-in-area tx ty radius)
                                  (filter #(not (.domesticated %)))
                                  (filter #(.huntMarkedCan %)))]
    (doseq [animal wild-animals-in-area]
      (animal/hunt-animal animal))
    (count wild-animals-in-area))  ; 返回标记的数量
  
  ;; 取消狩猎（取消单个动物的标记）
  (when animals-at-location
    (animal/unmark-hunt-animal animals-at-location))
  
  ;; 取消区域内所有已标记的动物
  (let [tx 157
        ty 483
        radius 15
        marked-animals (->> (animal/animals-in-area tx ty radius)
                            (filter #(.huntMarkedIs %)))]
    (doseq [animal marked-animals]
      (animal/unmark-hunt-animal animal))
    (count marked-animals))  ; 返回取消标记的数量 
  

  
  :rcf)

(comment
  ;; 演示怎么创建仓库和水井
  (require '[repl.tutorial1 :as tutorial]
           '[game.well :as well]
           '[game.common :refer [focus]])
  
  ;; 1. 创建仓库在 tile (378, 205)
  ;; 仓库大小：5x5，使用木头材料
  (def warehouse-center-x 378)
  (def warehouse-center-y 205)
  (def warehouse-width 5)
  (def warehouse-height 5)
  
  (tutorial/create-warehouse-once warehouse-center-x warehouse-center-y 
                                  warehouse-width warehouse-height)
  
  ;; 2. 在仓库旁边 10 格距离创建水井
  ;; 水井是固定的 3x3，必须使用石头材料
  ;; 在仓库右侧 10 格处创建（x 方向 +10）
  (def well-center-x (+ warehouse-center-x 10))
  (def well-center-y warehouse-center-y)
  
  (well/create-well-once well-center-x well-center-y)
  
  ;; 3. 定位到仓库位置
  (focus {:cX (* warehouse-center-x 32)  ; 转换为像素坐标（1 tile = 32 pixels）
          :cY (* warehouse-center-y 32)})
  
  ;; 或者定位到水井位置
  (focus {:cX (* well-center-x 32)
          :cY (* well-center-y 32)})
  
  :rcf)

