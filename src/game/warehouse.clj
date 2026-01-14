(ns game.warehouse
  (:require 
   [game.common :refer [array-list->vec array-list-resize->vec]]
   [repl.utils :as utils])
  (:import 
   [init.resources RESOURCES]
   [settlement.main SETT]))



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

;; Get the stockpile tally system
(defn get-stockpile-tally []
  (let [rooms (SETT/ROOMS)
        stockpile (.-STOCKPILE rooms)]
    (.tally stockpile)))

(comment
  (get-stockpile-tally)
  :rcf)

;; Get crates for a specific resource in a warehouse
(defn crates-for-resource [warehouse resource]
  (let [tally (get-stockpile-tally)
        crates (.crates tally)]
    (.get crates resource warehouse)))

(comment
  (crates-for-resource warehouse (RESOURCES/WOOD))
  :rcf)

;; Get all resources
(defn all-resources []
  (array-list->vec (RESOURCES/ALL)))

;; Get crates for each material in a warehouse
;; Returns a map of resource -> crate count
(defn crates-by-material [warehouse]
  (let [tally (get-stockpile-tally)
        crates (.crates tally)
        resources (all-resources)]
    (into {}
          (map (fn [resource]
                 [resource (.get crates resource warehouse)]))
          resources)))

(comment
  (crates-by-material warehouse)
  :rcf)

;; Get crates by material with resource names (more readable)
(defn crates-by-material-named [warehouse]
  (let [tally (get-stockpile-tally)
        crates (.crates tally)
        resources (all-resources)]
    (into {}
          (map (fn [resource]
                 [(.toString (.name resource)) (.get crates resource warehouse)]))
          resources)))

(comment
  ;; Example usage:
  (def warehouse (first (all-warehouses)))
  
  ;; Get crates for a specific resource
  (crates-for-resource warehouse (RESOURCES/WOOD))
  (crates-for-resource warehouse (RESOURCES/STONE))
  
  ;; Get crates for all materials (as resource objects)
  (crates-by-material warehouse)
  
  ;; Get crates for all materials (with resource names as keys)
  (crates-by-material-named warehouse)
  
  ;; Filter to only show materials with crates > 0
  (->> (crates-by-material-named warehouse)
       (filter (fn [[_name count]] (> count 0)))
       (into {}))
  
  :rcf)

