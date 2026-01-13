(ns game.warehouse
  (:require 
   [game.common :refer [array-list->vec array-list-resize->vec]]
   [repl.utils :as utils])
  (:import 
   [init.resources RESOURCES]
   [settlement.main SETT]
   [settlement.room.infra.stockpile StockpileInstance]))



(defn all-warehouses []
  (let [rooms (SETT/ROOMS)
        stockpile (.-STOCKPILE rooms)]
    (array-list-resize->vec (.all stockpile))))

(comment
  (def warehouses (all-warehouses)) 
  
  (array-list-resize->vec warehouses)
  
  (def warehouse (first warehouses))

  
  :rcf)

(defn warehouse-info [warehouse]
  (let [crates (utils/invoke-method warehouse "totalCrates")
        usage (utils/invoke-method warehouse "getUsedSpace")
        name (.toString (utils/invoke-method warehouse "name"))]
    {:crates crates
     :usage usage
     :name name
     :crate-size (utils/invoke-method warehouse "crateSize") ;; 单个箱子容量
     :radius (utils/invoke-method warehouse "radius") ;; 拾取半径?
     }))

(comment
  (warehouse-info warehouse)

  (utils/invoke-method warehouse "crateSize")

  (def wood (RESOURCES/WOOD))
  (def stone (RESOURCES/STONE))
  (def livestock (RESOURCES/LIVESTOCK))
  (def minables (array-list->vec (.all (RESOURCES/minables)))) 
  (def clay (.resource (first minables)))
  (def coal (.resource (second minables)))

  (def minable (first minables))
  
  (def rooms (SETT/ROOMS))
  (def stockpile (.-STOCKPILE rooms))
  (def tally (.tally stockpile))
  (def amount (.amount tally))
  (def crates (.crates tally))
  (def space (.space tally))
  (def amountReserved (.amountReserved tally))
  (def spaceReserved (.spaceReserved tally))

  (.get amount wood warehouse)
  (.get crates wood warehouse)
  (.get space wood warehouse)
  (.get amountReserved wood warehouse)
  (.get spaceReserved wood warehouse)

  (.get amount stone warehouse)
  (.get amount livestock warehouse)
  (.get amount clay warehouse)
  (.get amount coal warehouse)


  :rcf)

(defn minable-info [minable]
  {:name (.toString (.name minable))})

(comment
  (minable-info minable)

  (def minables (array-list->vec (.all (RESOURCES/minables))))
  (->> minables
       (map minable-info))
  
  :rcf)

