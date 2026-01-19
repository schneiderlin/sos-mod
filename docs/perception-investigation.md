# Perception Investigation: Finding "Ground Items" in SOS

## Problem Statement

The user reported that **tile (325, 578) contains 100 wood**, but this wood is **NOT**:
- In warehouses/stockpiles (all report 0)
- A furniture item
- An entity on the tile
- A tree/natural resource (can't be cleared)

The game has a mechanic where resources not in warehouses are not reported by standard warehouse queries. We need to find the API to access these "ground items" or "loose resources".

---

## What We've Tried

### 1. Entity System ✓ (Working)
```clojure
(defn entities-at-tile [tx ty]
  (seq (.getAtTile (SETT/ENTITIES) tx ty)))
```
- **Result:** Returns 0 entities at tile (325, 578)
- **Works for:** Humanoids, animals
- **Does NOT find:** Wood items

### 2. Furniture Item System ✓ (Working but Empty)
```clojure
(defn get-furniture-item [tx ty]
  (let [fdata (get-furniture-data)]
    (.get (.item fdata) tx ty)))
```
- **Result:** Returns `nil` at tile (325, 578)
- **Works for:** Crates, placed furniture
- **Does NOT find:** Ground wood items

### 3. Warehouse/Stockpile System ✓ (Working but Empty)
```clojure
(defn all-warehouses []
  (let [rooms (SETT/ROOMS)
        stockpile (.-STOCKPILE rooms)]
    (array-list-resize->vec (.all stockpile))))
```
- **Result:** 0 warehouses in settlement, all crate counts = 0
- **Expected:** This should show stored resources, but user has no warehouses

### 4. Clearing Jobs System ✓ (Working but No Trees)
```clojure
(defn has-wood-at-tile? [tx ty]
  (let [jobs (get-clearing-jobs)
        wood-job (.wood jobs)
        placer (.placer wood-job)]
    (nil? (.isPlacable placer tx ty nil nil))))
```
- **Result:** Returns "必须放在树上" (Must be placed on a tree) - no tree exists
- **Works for:** Natural resources (trees, rocks)
- **Does NOT find:** Loose wood items

---

## What Didn't Work

| Method | API Used | Result |
|--------|----------|--------|
| Entities | `(.getAtTile (SETT/ENTITIES) tx ty)` | 0 entities found |
| Furniture | `(.get (.item fdata) tx ty)` | nil (no furniture) |
| Warehouses | `(warehouse/all-warehouses)` | 0 warehouses exist |
| Crates | `(warehouse/crates-by-material-in-area ...)` | All counts = 0 |
| Natural Resources | `(.isPlacable placer tx ty nil nil)` | No tree at tile |

---

## Current Settlement Status

```
Pop:10 | ❄️ 10 cold | ⚠️ NO FOOD!
```

| Resource | Amount |
|----------|--------|
| Wood | 0 (in warehouses) |
| Stone | 0 |
| Food (all types) | 0 |
| Animals (wild) | 63 |

**Buildings:** 115 rooms (farms, mines, workshops built)
**Throne Position:** Tile (324, 576)
**Wood Location:** Tile (325, 578) - per user

---

## What to Try Next

### High Priority

1. **Tile Data Layer**
   - Look for `SETT/TILE`, `TILE_DATA`, or similar
   - May contain resource counts per tile
   - Check if there's a `layer` system for tile resources

2. **Item/Entity Subclass Check**
   - The wood might be a specific entity type we're not filtering for
   - Need to list ALL entity classes found at/near tile (325, 578)
   - Check for `Item`, `Resource`, `WoodItem` classes

3. **Sprite/Visual Layer**
   - May be in `SETT/SPRITE` or visual data
   - Wood on ground might be stored as sprites/visuals

4. **Job/Task System**
   - Check if there are "haul" or "carry" jobs
   - Wood might be "assigned" to a humanoid/job

### Medium Priority

5. **Area Scan with Class Info**
   - Scan larger area around (325, 578)
   - Print ALL entity class types
   - Find pattern for wood items

6. **Stockpile Room (Not Warehouse)**
   - There might be a distinction between "stockpile room" and "warehouse"
   - Check for `ROOM/STOCKPILE` vs warehouse system

7. **Resource Deposits**
   - Check if there's a `DEPOSIT` or `RESOURCE_DEPOSIT` system
   - Wood might be stored as "deposit" on ground

### Low Priority

8. **Ask User**
   - Where exactly are they seeing "100 wood"?
   - Is it a debug view? Tooltip? UI overlay?
   - This would reveal which system to query

9. **Java Class Exploration**
   - Search for `Wood`, `Item`, `Resource` classes in Java code
   - Look for static methods or singleton accessors

10. **Save File Analysis**
    - If game has save files, examine structure
    - May reveal how ground items are stored

---

## Code Files to Investigate

- `src/settlement/main/SETT.java` - Main settlement class, look for item accessors
- `src/settlement/entity/*` - Entity classes
- `src/settlement/tile/*` - Tile data systems
- `src/init/item/*` - Item initialization
- `src/settlement/room/main/stockpile/*` - Stockpile implementation

---

## Useful Test Commands

```clojure
;; List all entity classes in an area
(doseq [x (range 320 330) y (range 575 585)]
  (when-let [ents (seq (.getAtTile (SETT/ENTITIES) x y))]
    (println [x y] (map #(str (.getClass %)) ents))))

;; Check if SETT has other item-related fields
(bean SETT)

;; Look for tile data
(try
  (.tile SETT)
  (catch Exception e
    (.getMessage e)))
```

---

## NEW: Systems to Explore

Based on code investigation, we found several promising SETT methods (from `doc/index.md`):

| System | Method | Description | Status |
|--------|--------|-------------|--------|
| THINGS | `SETT.THINGS()` | Loose items/objects on ground | To Test |
| TILE_MAP | `SETT.TILE_MAP()` | Per-tile data map | To Test |
| HALFENTS | `SETT.HALFENTS()` | Half-entities (items?) | To Test |
| MINERALS | `SETT.MINERALS()` | Mineral/resource deposits | To Test |
| STORED | `STATS.STORED()` | Global resource tracking | To Test |

### New Exploration Functions Added

Added to `src/play/perception.clj`:

```clojure
;; Explore SETT.THINGS() structure
(explore-things)

;; Explore SETT.TILE_MAP() structure  
(explore-tile-map)
(get-tile-map-data tx ty)

;; Explore STATS.STORED() for global totals
(explore-stats-stored)
(get-stored-amount resource)
(global-resource-amounts)

;; Check tally for global totals without warehouse
(explore-tally-totals)

;; Explore HALFENTS
(explore-halfents)

;; Explore MINERALS
(explore-minerals)
(get-minerals-at-tile tx ty)

;; Comprehensive scan using ALL systems
(comprehensive-tile-scan tx ty)
```

### To Run Tests

Execute these in REPL connected to the game:

```clojure
(require '[play.perception :as p])

;; 1. Explore all systems
(p/explore-things)
(p/explore-tile-map)
(p/explore-stats-stored)
(p/explore-tally-totals)
(p/explore-halfents)
(p/explore-minerals)

;; 2. Comprehensive scan of the wood tile
(p/comprehensive-tile-scan 325 578)

;; 3. Check global stored amounts
(p/global-resource-amounts)
```

---

## Questions for User

1. **Where are you seeing the "100 wood at (325, 578)"?**
   - Game UI? Debug menu? Tooltip?
   - This would point us to the right API

2. **Is the wood visible on screen?**
   - Can you see wood piles/tree logs on the ground?
   - Or is it only shown in a menu?

3. **Can you interact with it?**
   - Can you click on it?
   - What actions are available?

---

## Next Steps After Testing

1. **Run exploration functions** to understand each system's structure
2. **Find the right method** for accessing ground items based on results
3. **Implement `find-ground-items`** function once API is discovered
4. **Update `settlement-overview`** to include loose/ground resources
