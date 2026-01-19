# SOS Play Examples

Practical examples for automating SOS game actions.

## Table of Contents
- [Discovery and Setup](#discovery-and-setup)
- [Camera Navigation](#camera-navigation)
- [Warehouse Construction](#warehouse-construction)
- [Settlement Planning](#settlement-planning)
- [Time Control](#time-control)
- [Debugging and Inspection](#debugging-and-inspection)

## Discovery and Setup

### Find nREPL port

```bash
# Discover nREPL ports in current directory
clj-nrepl-eval --discover-ports
```

### Load the tutorial namespace

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])
;; Now all functions are available as t/function-name
EOF
```

## Camera Navigation

### Quick camera tour around the map

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Start at throne
(t/move-to-throne)

;; Reset zoom
(t/set-zoom 0)

;; Move in a pattern
(t/move-camera-direction :up :speed 500)
(t/move-camera-direction :right :speed 500)
(t/move-camera-direction :down :speed 500)
(t/move-camera-direction :left :speed 500)
EOF
```

### Move to specific tile coordinates

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Move to various locations
(t/move-camera-to-tile 100 100)   ; Top-left area
(t/move-camera-to-tile 500 500)   ; Center area
(t/move-camera-to-tile 1000 1000) ; Far area
EOF
```

### Zoom to get overview

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

(t/move-to-throne)
(t/set-zoom 3)  ; Zoom out for overview
EOF
```

## Warehouse Construction

### Build single warehouse

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Move to location first
(t/move-camera-to-tile 251 430)

;; Build warehouse
(t/create-warehouse-once 251 430 5 5)

;; Verify construction
(t/warehouse-furniture-info 251 430 5 5)
EOF
```

### Build multiple warehouses in a grid

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Create a 2x2 grid of warehouses
(doseq [row [0 1]
        col [0 1]]
  (let [center-x (+ 251 (* col 30))
        center-y (+ 430 (* row 30))]
    (t/create-warehouse-once center-x center-y 5 5)))

;; Check all warehouses
(t/scan-furniture-area 240 420 50 50)
EOF
```

### Build stone warehouse for durability

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

(t/move-camera-to-tile 120 120)
(t/create-warehouse-once 120 120 5 5 :material-name "STONE")
EOF
```

### Build warehouse without auto-furniture

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Let the game auto-place furniture
(t/create-warehouse-once 280 400 5 5 :place-furniture false)
EOF
```

## Settlement Planning

### Plan before building

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Define settlement parameters
(def center-x 261)
(def center-y 430)
(def warehouse-size 5)

;; 1. Check if area is clear
(t/scan-furniture-area
 (- center-x 20) (- center-y 20) 40 40)

;; 2. Calculate layout
(t/find-edge-tiles center-x center-y warehouse-size warehouse-size)

;; 3. Plan furniture
(t/calculate-furniture-positions center-x center-y warehouse-size warehouse-size 2 1)

;; 4. Find door position
(let [furn-positions (t/calculate-furniture-positions center-x center-y 5 5 2 1)
      occupied (t/calculate-occupied-tiles furn-positions 2 1)]
  (t/find-door-position center-x center-y 5 5 occupied :preferred-side :top))
EOF
```

### Create storage area

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Pause while building
(t/pause-time)

;; Create row of warehouses
(doseq [i (range 3)]
  (let [x (+ 251 (* i 30))
        y 430]
    (t/create-warehouse-once x y 5 5)))

;; Resume
(t/resume-time)

;; Verify
(t/warehouse-furniture-info 251 430 5 5)
(t/warehouse-furniture-info 281 430 5 5)
(t/warehouse-furniture-info 311 430 5 5)
EOF
```

### Find optimal building locations

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Scan for clear areas
(defn scan-for-clear-area [start-x start-y range-x range-y]
  (filter (fn [{:keys [x y info]}]
            (and (not (:has-item info))
                 (not (:has-tile info))
                 (not (:has-crate info))))
          (t/scan-furniture-area start-x start-y range-x range-y)))

;; Check 100x100 area around throne
(scan-for-clear-area 200 400 100 100)
EOF
```

## Time Control

### Pause while working

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Pause game
(t/pause-time)

;; Do work while paused
(t/create-warehouse-once 251 430 5 5)
(t/move-camera-to-tile 251 430)

;; Resume when ready
(t/resume-time)
EOF
```

### Speed up construction

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Build at high speed
(t/set-time-speed 25)

;; Create multiple warehouses
(dotimes [i 5]
  (t/create-warehouse-once (+ 251 (* i 30)) 430 5 5))

;; Return to normal speed
(t/set-time-speed 1)
EOF
```

### Toggle pause state

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Toggle pause on/off
(t/toggle-pause)
EOF
```

## Debugging and Inspection

### Check warehouse contents

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Get detailed furniture info
(t/warehouse-furniture-info 261 400 5 5)

;; Check specific tile
(t/furniture-info 259 398)

;; Check if crate exists
(t/has-crate? 259 398)
EOF
```

### Verify construction

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; After building, verify
(def warehouse-result (t/create-warehouse-once 251 430 5 5))

;; Check success
(:success warehouse-result)

;; Scan area
(t/scan-furniture-area 248 427 10 10)
EOF
```

### Debug furniture placement

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Check what's at a tile
(def tile-info (t/furniture-info 259 398))

;; Print details
(println "Has item:" (:has-item tile-info))
(println "Has tile:" (:has-tile tile-info))
(println "Has crate:" (t/has-crate? 259 398))
(println "Item width:" (:item-width tile-info))
(println "Item height:" (:item-height tile-info))
EOF
```

### Test pure functions

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Test edge tile calculation
(t/find-edge-tiles 261 430 5 5)

;; Test door position
(t/calculate-door-position 261 430 5 5 :side :top)

;; Test furniture layout
(def positions (t/calculate-furniture-positions 261 430 5 5 2 1))
(println "Furniture count:" (count positions))
(println "Occupied tiles:" (count (t/calculate-occupied-tiles positions 2 1)))
EOF
```

## Advanced Patterns

### Hermann multi-step workflow

```bash
# Step 1: Load namespace
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])
EOF

# Step 2: Move camera
clj-nrepl-eval -p 50000 <<'EOF'
(t/move-to-throne)
(t/set-zoom 0)
EOF

# Step 3: Build
clj-nrepl-eval -p 50000 <<'EOF'
(t/pause-time)
(t/create-warehouse-once 251 430 5 5)
EOF

# Step 4: Verify
clj-nrepl-eval -p 50000 <<'EOF'
(t/resume-time)
(t/warehouse-furniture-info 251 430 5 5)
EOF
```

### Build settlement with error handling

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

(defn safe-build-warehouse [x y]
  (try
    ;; Check if area is clear first
    (let [scan (t/scan-furniture-area (- x 10) (- y 10) 20 20)]
      (if (empty? scan)
        (do
          (t/create-warehouse-once x y 5 5)
          {:status :success :x x :y y})
        {:status :blocked :x x :y y :reason scan}))
    (catch Exception e
      {:status :error :x x :y y :message (.getMessage e)})))

;; Build multiple warehouses safely
(map safe-build-warehouse
     [251 281 311]
     [430 430 430])
EOF
```

### Create warehouse pattern

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])

;; Create U-shaped warehouse pattern
(defn build-u-pattern [start-x start-y]
  (let [positions [[0 0] [30 0] [60 0]  ; Top row
                   [0 30]                ; Left side
                   [0 60] [30 60] [60 60]]] ; Bottom row
    (doseq [[dx dy] positions]
      (t/create-warehouse-once (+ start-x dx) (+ start-y dy) 5 5))))

;; Build the pattern
(t/pause-time)
(build-u-pattern 200 400)
(t/resume-time)
EOF
```
