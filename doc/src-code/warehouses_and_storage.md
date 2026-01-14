# Warehouses and Storage Management

This document explains how to programmatically manage warehouses (stockpiles) in Songs of Syx, including querying crate information, setting material types, and aggregating data across multiple warehouses.

## Overview

Warehouses (stockpiles) in Songs of Syx are managed through the `SETT.ROOMS().STOCKPILE` system. Each warehouse contains crates that can store different resource types. You can query crate allocations, set which materials can be stored, and aggregate information across multiple warehouses.

## Key Classes

- `settlement.room.infra.stockpile.StockpileInstance` - Individual warehouse instance
- `settlement.room.infra.stockpile.StockpileTally` - Tally system for tracking crates, amounts, and space
- `settlement.room.infra.stockpile.ROOM_STOCKPILE` - Stockpile room blueprint
- `settlement.room.main.job.StorageCrate` - Individual crate storage
- `init.resources.RESOURCES` - Resource type definitions

## Getting Warehouses

### Get All Warehouses

```clojure
(ns game.warehouse
  (:require 
   [game.common :refer [array-list-resize->vec]]
   [repl.utils :as utils])
  (:import 
   [init.resources RESOURCES]
   [settlement.main SETT]))

;; Get all warehouses in the settlement
(defn all-warehouses []
  (let [rooms (SETT/ROOMS)
        stockpile (.-STOCKPILE rooms)]
    (array-list-resize->vec (.all stockpile))))
```

### Get Warehouse Information

```clojure
;; Get basic information about a warehouse
(defn warehouse-info [warehouse]
  (let [crates (utils/invoke-method warehouse "totalCrates")
        usage (utils/invoke-method warehouse "getUsedSpace")
        name (.toString (utils/invoke-method warehouse "name"))]
    {:crates crates
     :usage usage
     :name name
     :crate-size (utils/invoke-method warehouse "crateSize")
     :radius (utils/invoke-method warehouse "radius")}))
```

## Querying Crate Information

### Getting Crates for a Specific Resource

The stockpile tally system tracks how many crates are allocated to each resource type:

```clojure
;; Get the stockpile tally system
(defn get-stockpile-tally []
  (let [rooms (SETT/ROOMS)
        stockpile (.-STOCKPILE rooms)]
    (.tally stockpile)))

;; Get number of crates allocated to a specific resource
(defn crates-for-resource [warehouse resource]
  (let [tally (get-stockpile-tally)
        crates (.crates tally)]
    (.get crates resource warehouse)))
```

### Getting All Resource Crate Counts

```clojure
;; Get all resources
(defn all-resources []
  (array-list->vec (RESOURCES/ALL)))

;; Get crates for each material in a warehouse
(defn crates-by-material [warehouse]
  (let [tally (get-stockpile-tally)
        crates (.crates tally)
        resources (all-resources)]
    (into {}
          (map (fn [resource]
                 [resource (.get crates resource warehouse)]))
          resources)))

;; Get crates with resource names (more readable)
(defn crates-by-material-named [warehouse]
  (let [tally (get-stockpile-tally)
        crates (.crates tally)
        resources (all-resources)]
    (into {}
          (map (fn [resource]
                 [(.toString (.name resource)) (.get crates resource warehouse)]))
          resources)))
```

### Example Usage

```clojure
(def warehouse (first (all-warehouses)))

;; Get crates for specific resources
(crates-for-resource warehouse (RESOURCES/WOOD))
(crates-for-resource warehouse (RESOURCES/STONE))

;; Get all resource crate counts
(crates-by-material-named warehouse)

;; Filter to only show materials with crates > 0
(->> (crates-by-material-named warehouse)
     (filter (fn [[_name count]] (> count 0)))
     (into {}))
```

## Aggregating Across Multiple Warehouses

### Aggregate Crates Across a Set of Warehouses

```clojure
;; Aggregate crates across multiple warehouses
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

;; With resource names
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

;; Aggregate across ALL warehouses
(defn crates-by-material-all-warehouses []
  (crates-by-material-warehouses (all-warehouses)))

(defn crates-by-material-all-warehouses-named []
  (crates-by-material-warehouses-named (all-warehouses)))
```

### Example Usage

```clojure
;; Aggregate across a set of warehouses
(def warehouses (take 3 (all-warehouses)))
(crates-by-material-warehouses-named warehouses)

;; Aggregate across ALL warehouses
(crates-by-material-all-warehouses-named)

;; Filter to only show materials with crates > 0
(->> (crates-by-material-all-warehouses-named)
     (filter (fn [[_name count]] (> count 0)))
     (into {}))
```

## Warehouse Position and Area Queries

### Getting Warehouse Position

```clojure
;; Get warehouse position (center coordinates)
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
```

### Finding Warehouses in an Area

```clojure
;; Check if a warehouse is within a rectangular area
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

(defn crates-by-material-in-area-named [start-x start-y width height]
  (crates-by-material-warehouses-named (warehouses-in-area start-x start-y width height)))
```

### Example Usage

```clojure
;; Get warehouse position
(warehouse-position warehouse)

;; Find warehouses in a specific area (e.g., 200x200 to 300x300)
(warehouses-in-area 200 200 100 100)

;; Aggregate crates across warehouses in a specific area
(crates-by-material-in-area-named 200 200 100 100)
```

## Setting Material Types for Crates

### Understanding Crate Allocation

Warehouses use a crate allocation system to determine which resources can be stored:
- **Crate Allocation**: The number of crates allocated to a resource type determines if that resource can be stored
- **Special Amount Limit**: Optional per-crate limit (0 = use default crate size, 1-100 = restrict to that amount)

### Getting Crate Allocations

```clojure
;; Get the number of crates allocated to a resource
(defn get-crates-allocated-to-resource [warehouse resource]
  (let [tally (get-stockpile-tally)
        crates (.crates tally)]
    (.get crates resource warehouse)))

;; Get all crate allocations for a warehouse
(defn get-crate-allocations [warehouse]
  (let [resources (all-resources)]
    (into {}
          (map (fn [resource]
                 [resource (get-crates-allocated-to-resource warehouse resource)]))
          resources)))

;; With resource names
(defn get-crate-allocations-named [warehouse]
  (let [resources (all-resources)]
    (into {}
          (map (fn [resource]
                 [(.toString (.name resource)) (get-crates-allocated-to-resource warehouse resource)]))
          resources)))
```

### Setting Crate Allocations

```clojure
;; Allocate a specific number of crates to a resource type
;; This is the primary way to set which resources can be stored
(defn allocate-crates-to-resource-once [warehouse resource amount]
  (utils/update-once
   (fn [_ds]
     (utils/invoke-method warehouse "allocateCrate" resource amount))))

;; Set warehouse to only accept a specific resource
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

;; Set warehouse to accept multiple resources
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
```

### Special Amount Limits (Optional)

```clojure
;; Get the special amount limit for a resource (if set)
;; Returns 0 if not restricted, >0 if restricted to that amount per crate
(defn get-crate-material-limit [warehouse resource]
  (utils/invoke-method warehouse "getSpecialAmount" resource))

;; Set the special amount limit for a resource
;; amount: 0 = allow all (use default crate size), 1-100 = restrict to that amount per crate
(defn set-crate-material-limit-once [warehouse resource amount]
  (utils/update-once
   (fn [_ds]
     (utils/invoke-method warehouse "setSpecialAmount" resource amount))))
```

### Example Usage

```clojure
(def warehouse (first (all-warehouses)))

;; Check current allocations
(get-crate-allocations-named warehouse)

;; Allocate 5 crates to wood
(allocate-crates-to-resource-once warehouse (RESOURCES/WOOD) 5)

;; Set warehouse to only accept wood (allocates all crates to wood)
(set-warehouse-single-material-once warehouse (RESOURCES/WOOD))

;; Set warehouse to accept wood and stone (distributes crates evenly)
(set-warehouse-materials-once warehouse [(RESOURCES/WOOD) (RESOURCES/STONE)])

;; Clear all restrictions
(clear-warehouse-material-restrictions-once warehouse)

;; Set special per-crate limit (optional)
(set-crate-material-limit-once warehouse (RESOURCES/WOOD) 50)
```

## Understanding the Tally System

The stockpile tally system tracks multiple metrics for each resource:

```clojure
(defn get-stockpile-tally []
  (let [rooms (SETT/ROOMS)
        stockpile (.-STOCKPILE rooms)]
    (.tally stockpile)))

;; The tally system provides:
;; - .crates - Number of crates allocated to each resource
;; - .amount - Current amount stored for each resource
;; - .space - Total storage space available for each resource
;; - .amountReserved - Amount reserved for jobs
;; - .spaceReserved - Storage space reserved
```

### Accessing Tally Data

```clojure
(let [tally (get-stockpile-tally)
      wood (RESOURCES/WOOD)
      warehouse (first (all-warehouses))]
  
  ;; Get number of crates allocated to wood
  (.get (.crates tally) wood warehouse)
  
  ;; Get current amount of wood stored
  (.get (.amount tally) wood warehouse)
  
  ;; Get total storage space for wood
  (.get (.space tally) wood warehouse)
  
  ;; Get reserved amount
  (.get (.amountReserved tally) wood warehouse)
  
  ;; Get reserved space
  (.get (.spaceReserved tally) wood warehouse))
```

## Source Code References

- `sos-src/settlement/room/infra/stockpile/StockpileInstance.java` - Main warehouse instance class
- `sos-src/settlement/room/infra/stockpile/StockpileTally.java` - Tally system for tracking resources
- `sos-src/settlement/room/infra/stockpile/Constructor.java` - Stockpile constructor
- `sos-src/settlement/room/main/job/StorageCrate.java` - Individual crate storage
- `sos-src/settlement/room/infra/stockpile/Gui.java` - UI implementation (shows how allocation works)

## Important Notes

1. **Use `update-once`**: All functions that modify warehouse state must be called within `update-once` to ensure they execute in a single frame
2. **Crate Allocation vs Special Limits**: 
   - `allocateCrate` sets which resources can be stored (primary method)
   - `setSpecialAmount` sets optional per-crate limits (secondary, optional)
3. **Position Methods**: Warehouse position uses `mX()`/`mY()` methods (center) or `fx()`/`fy()` methods (first accessible tile) as fallback
4. **Resource Indexing**: Resources are indexed starting from 0, but the tally system uses resource objects directly

## Common Patterns

### Check Which Resources Are Stored

```clojure
;; Get all resources with crates > 0
(->> (crates-by-material-named warehouse)
     (filter (fn [[_name count]] (> count 0)))
     (into {}))
```

### Find Warehouses Storing a Specific Resource

```clojure
;; Find all warehouses that have crates allocated to wood
(let [wood (RESOURCES/WOOD)]
  (filter (fn [warehouse]
            (> (crates-for-resource warehouse wood) 0))
          (all-warehouses)))
```

### Aggregate Storage Across Area

```clojure
;; Get total crates for each resource in a specific area
(crates-by-material-in-area-named 200 200 100 100)
```

