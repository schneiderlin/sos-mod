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

;; Aggregate crates across a set of warehouses for each resource
;; Returns a map of resource -> total crate count
(defn crates-by-material-warehouses [warehouses]
  (let [tally (get-stockpile-tally)
        crates (.crates tally)
        resources (all-resources)]
    (into {}
          (map (fn [resource]
                 [resource (reduce + 0
                                  (map (fn [warehouse]
                                         (.get crates resource warehouse))
                                       warehouses))]))
          resources)))

;; Aggregate crates across a set of warehouses with resource names (more readable)
(defn crates-by-material-warehouses-named [warehouses]
  (let [tally (get-stockpile-tally)
        crates (.crates tally)
        resources (all-resources)]
    (into {}
          (map (fn [resource]
                 [(.toString (.name resource))
                  (reduce + 0
                          (map (fn [warehouse]
                                 (.get crates resource warehouse))
                               warehouses))]))
          resources)))

;; Aggregate crates across ALL warehouses for each resource
(defn crates-by-material-all-warehouses []
  (crates-by-material-warehouses (all-warehouses)))

;; Aggregate crates across ALL warehouses with resource names (more readable)
(defn crates-by-material-all-warehouses-named []
  (crates-by-material-warehouses-named (all-warehouses)))

;; Get warehouse position (center coordinates)
;; Returns {:x x :y y} or nil if position cannot be determined
(defn warehouse-position [warehouse]
  (try
    ;; Try to get center coordinates using mX() and mY() methods
    (let [mx (utils/invoke-method warehouse "mX")
          my (utils/invoke-method warehouse "mY")]
      (when (and mx my)
        {:x mx :y my}))
    (catch Exception _e
      (try
        ;; Fallback: try to get first accessible tile (fx, fy)
        (let [fx (utils/invoke-method warehouse "fx")
              fy (utils/invoke-method warehouse "fy")]
          (when (and fx fy)
            {:x fx :y fy}))
        (catch Exception _e2
          nil)))))

;; Check if a warehouse is within a rectangular area
;; start-x, start-y: top-left corner of the area
;; width, height: dimensions of the area
(defn warehouse-in-area? [warehouse start-x start-y width height]
  (when-let [pos (warehouse-position warehouse)]
    (and (>= (:x pos) start-x)
         (< (:x pos) (+ start-x width))
         (>= (:y pos) start-y)
         (< (:y pos) (+ start-y height)))))

;; Get all warehouses within a rectangular area
(defn warehouses-in-area [start-x start-y width height]
  (filter (fn [warehouse]
            (warehouse-in-area? warehouse start-x start-y width height))
          (all-warehouses)))

;; Aggregate crates across warehouses in a specific area
(defn crates-by-material-in-area [start-x start-y width height]
  (crates-by-material-warehouses (warehouses-in-area start-x start-y width height)))

;; Aggregate crates across warehouses in a specific area with resource names
(defn crates-by-material-in-area-named [start-x start-y width height]
  (crates-by-material-warehouses-named (warehouses-in-area start-x start-y width height)))

;; ============================================================================
;; Setting Material Types for Crates
;; ============================================================================

;; Get the number of crates allocated to a resource in a warehouse
;; Returns the number of crates allocated to this resource type
(defn get-crates-allocated-to-resource [warehouse resource]
  (let [tally (get-stockpile-tally)
        crates (.crates tally)]
    (.get crates resource warehouse)))

(comment
  (get-crates-allocated-to-resource warehouse1 (RESOURCES/WOOD))
  (get-crates-allocated-to-resource warehouse2 (RESOURCES/WOOD))

  (get-crates-allocated-to-resource warehouse1 (RESOURCES/STONE))
  (get-crates-allocated-to-resource warehouse2 (RESOURCES/STONE))
  :rcf)

;; Get the special amount limit for a resource (if set)
;; Returns the limit (0 = not restricted, >0 = restricted to that amount per crate)
;; Note: This is different from crate allocation - this sets a per-crate limit
(defn get-crate-material-limit [warehouse resource]
  (utils/invoke-method warehouse "getSpecialAmount" resource))

;; Allocate a specific number of crates to a resource type
;; This is the primary way to set which resources can be stored
;; amount: number of crates to allocate to this resource (0 = remove allocation)
(defn allocate-crates-to-resource-once [warehouse resource amount]
  (utils/update-once
   (fn [_ds]
     (utils/invoke-method warehouse "allocateCrate" resource amount))))

;; Set the special amount limit for a resource (if set)
;; amount: 0 = allow all (use default crate size), 1-100 = restrict to that amount per crate
;; Note: This is different from allocation - this sets a per-crate limit
;; Note: This must be called within update-once for side effects
(defn set-crate-material-limit [warehouse resource amount]
  (utils/invoke-method warehouse "setSpecialAmount" resource amount))

;; Set material limit for a resource (with update-once wrapper)
(defn set-crate-material-limit-once [warehouse resource amount]
  (utils/update-once
   (fn [_ds]
     (set-crate-material-limit warehouse resource amount))))

;; Get all crate allocations for a warehouse
;; Returns a map of resource -> number of crates allocated
(defn get-crate-allocations [warehouse]
  (let [resources (all-resources)]
    (into {}
          (map (fn [resource]
                 [resource (get-crates-allocated-to-resource warehouse resource)]))
          resources)))

;; Get all crate allocations for a warehouse with resource names
(defn get-crate-allocations-named [warehouse]
  (let [resources (all-resources)]
    (into {}
          (map (fn [resource]
                 [(.toString (.name resource)) (get-crates-allocated-to-resource warehouse resource)]))
          resources)))

;; Get all material limits for a warehouse
;; Returns a map of resource -> limit
(defn get-crate-material-limits [warehouse]
  (let [resources (all-resources)]
    (into {}
          (map (fn [resource]
                 [resource (get-crate-material-limit warehouse resource)]))
          resources)))

;; Get all material limits for a warehouse with resource names
(defn get-crate-material-limits-named [warehouse]
  (let [resources (all-resources)]
    (into {}
          (map (fn [resource]
                 [(.toString (.name resource)) (get-crate-material-limit warehouse resource)]))
          resources)))

;; Set a warehouse to only accept a specific resource
;; Allocates all available crates to the specified resource
(defn set-warehouse-single-material-once [warehouse resource]
  (utils/update-once
   (fn [_ds]
     (let [total-crates (utils/invoke-method warehouse "totalCrates")
           all-res (all-resources)]
       ;; Remove allocations from all other resources
       (doseq [r all-res]
         (when (not= r resource)
           (utils/invoke-method warehouse "allocateCrate" r 0)))
       ;; Allocate all crates to the specified resource
       (utils/invoke-method warehouse "allocateCrate" resource total-crates)))))

;; Set a warehouse to accept multiple resources
;; resources: a collection of RESOURCE objects
;; Distributes crates evenly among the specified resources
(defn set-warehouse-materials-once [warehouse resources]
  (utils/update-once
   (fn [_ds]
     (let [total-crates (utils/invoke-method warehouse "totalCrates")
           all-res (all-resources)
           allowed-set (set resources)
           crates-per-resource (when (seq resources)
                                 (quot total-crates (count resources)))]
       ;; Remove allocations from resources not in allowed set
       (doseq [r all-res]
         (when (not (contains? allowed-set r))
           (utils/invoke-method warehouse "allocateCrate" r 0)))
       ;; Allocate crates to allowed resources
       (when crates-per-resource
         (doseq [r resources]
           (utils/invoke-method warehouse "allocateCrate" r crates-per-resource)))))))

;; Clear all material restrictions (remove all crate allocations)
(defn clear-warehouse-material-restrictions-once [warehouse]
  (utils/update-once
   (fn [_ds]
     (let [resources (all-resources)]
       (doseq [r resources]
         (utils/invoke-method warehouse "allocateCrate" r 0))))))

(comment
  ;; Example usage:
  (def warehouse (first (all-warehouses)))
  
  ;; Get crates for a specific resource in one warehouse
  (crates-for-resource warehouse (RESOURCES/WOOD))
  (crates-for-resource warehouse (RESOURCES/STONE))
  
  ;; Get crates for all materials in one warehouse (as resource objects)
  (crates-by-material warehouse)
  
  ;; Get crates for all materials in one warehouse (with resource names as keys)
  (crates-by-material-named warehouse)
  
  ;; Filter to only show materials with crates > 0 in one warehouse
  (->> (crates-by-material-named warehouse)
       (filter (fn [[_name count]] (> count 0)))
       (into {}))
  
  ;; Aggregate crates across a set of warehouses
  (def warehouses (take 3 (all-warehouses)))
  (crates-by-material-warehouses warehouses)
  (crates-by-material-warehouses-named warehouses)
  
  ;; Aggregate crates across ALL warehouses
  (crates-by-material-all-warehouses)
  (crates-by-material-all-warehouses-named)
  
  ;; Filter to only show materials with crates > 0 across all warehouses
  (->> (crates-by-material-all-warehouses-named)
       (filter (fn [[_name count]] (> count 0)))
       (into {}))
  
  ;; Get warehouse position
  (warehouse-position warehouse)
  
  ;; Find warehouses in a specific area (e.g., 200x200 to 300x300)
  (warehouses-in-area 200 200 100 100)
  
  ;; Aggregate crates across warehouses in a specific area
  (crates-by-material-in-area 200 200 100 100)
  (crates-by-material-in-area-named 200 200 100 100)
  
  ;; Filter to only show materials with crates > 0 in area
  (->> (crates-by-material-in-area-named 200 200 100 100)
       (filter (fn [[_name count]] (> count 0)))
       (into {}))
  
  ;; ============================================================================
  ;; Setting Material Types for Crates
  ;; ============================================================================
  
  (def warehouses (all-warehouses))
  (def warehouse1 (first warehouses))
  (def warehouse2 (second warehouses))

  ;; Get current material limit for a resource
  (get-crate-material-limit warehouse1 (RESOURCES/WOOD))
  (get-crate-material-limit warehouse2 (RESOURCES/WOOD))
  
  ;; Get all material limits
  (get-crate-material-limits warehouse1)
  (get-crate-material-limits warehouse2)
  (get-crate-material-limits-named warehouse1)
  (get-crate-material-limits-named warehouse2)
  
  ;; Set material limit for a resource (0 = allow all, 1-100 = restrict to that amount)
  (set-crate-material-limit-once warehouse (RESOURCES/WOOD) 50)
  
  ;; Set warehouse to only accept one material type
  (set-warehouse-single-material-once warehouse (RESOURCES/WOOD))
  
  ;; Set warehouse to accept multiple materials
  (set-warehouse-materials-once warehouse [(RESOURCES/WOOD) (RESOURCES/STONE)])
  
  ;; Clear all restrictions (allow all materials with default crate size)
  (clear-warehouse-material-restrictions-once warehouse)
  
  :rcf)

