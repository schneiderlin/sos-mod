# Task: Ensure At Least 3 Crates for Vegetables

## Description
Ensure that warehouses have at least 3 crates allocated for storing vegetables.

## Progress

### 2025-01-19 - Task Created
- Created task file
- **Completed implementation** - Added functions to `src/game/warehouse.clj`

### Implementation Status: COMPLETE

**Note:** Functions moved from `play.perception` to `game.warehouse` namespace because they modify game state (perception should be read-only).

## Functions Implemented in game.warehouse

### 1. `ensure-resource-crates` (generic)
Ensure at least N crates are allocated for any specific resource across warehouses.

**Parameters:**
- `resource-key` - string key (e.g., "VEGETABLE", "WOOD")
- `min-crates` - minimum number of crates to allocate

**Returns:**
- Total number of crates allocated for the resource

**Usage:**
```clojure
(require '[game.warehouse :as warehouse])

;; Ensure 3 crates for vegetables
(warehouse/ensure-resource-crates "VEGETABLE" 3)

;; Ensure 5 crates for wood
(warehouse/ensure-resource-crates "WOOD" 5)
```

### 2. `get-resource-crate-status` (generic)
Get current crate allocation status for any specific resource.

**Parameters:**
- `resource-key` - string key (e.g., "VEGETABLE", "WOOD")

**Returns:**
- `:total-crates` - Total crates across all warehouses
- `:by-warehouse` - Breakdown of crates per warehouse

**Usage:**
```clojure
(warehouse/get-resource-crate-status "VEGETABLE")
;; => {:total-crates 3, :by-warehouse [{:warehouse {...} :crates 2} ...]}
```

### 3. `ensure-vegetable-crates` (convenience)
Convenience function specifically for vegetables (3 crates).

**Usage:**
```clojure
(warehouse/ensure-vegetable-crates)
```

### 4. `get-vegetable-crate-status` (convenience)
Convenience function specifically for vegetables.

**Usage:**
```clojure
(warehouse/get-vegetable-crate-status)
```

## Implementation Notes

**Logic:**
- Finds current crate allocation across all warehouses
- If less than minimum, allocates additional crates to warehouses with available space
- Distributes to warehouses that have free crate capacity

**Namespace:** `game.warehouse` (correct namespace for state-modifying operations)

**Dependencies:**
- `warehouse/allocate-crates-to-resource-once` - For allocating crates
- `warehouse/get-crates-allocated-to-resource` - For checking current allocation
- `repl.utils` - For invoking methods
- `RESOURCES` - Resource lookup

## Testing

```clojure
(require '[game.warehouse :as warehouse])

;; Check current status
(warehouse/get-vegetable-crate-status)

;; Ensure 3 crates
(warehouse/ensure-vegetable-crates)

;; Verify
(warehouse/get-vegetable-crate-status)

;; Generic version for other resources
(warehouse/ensure-resource-crates "WOOD" 5)
(warehouse/get-resource-crate-status "WOOD")
```

## Notes
- Vegetable resource key is "VEGETABLE"
- Allocation is total across all warehouses (not per warehouse)
- Uses update-once for side effects (requires game update cycle)
- Perception namespace remains read-only
