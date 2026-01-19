---
name: sos-play
description: Play Settlements of Survival (SOS) game via nREPL using Clojure functions. Use this when you need to control the game camera, create warehouses, inspect furniture, control time flow, or automate game actions.
---

# SOS Game Control via Clojure REPL

This skill provides Clojure functions to interact with and automate the Settlements of Survival (SOS) game through a running nREPL session.

## Prerequisites

1. **Running nREPL server**: Start the SOS mod's nREPL server
2. **Loaded namespaces**: The game namespaces must be loaded in the REPL

## Quick Discovery

```bash
# Find nREPL ports in current directory
clj-nrepl-eval --discover-ports
```

## Available Function Categories

The SOS control functions are organized into these categories in `src/repl/tutorial1.clj`:

### 0. Camera Control
Move and control the in-game camera:

```clojure
;; Get game window
(get-game-window)

;; Move camera to pixel position
(move-camera-to 1000 1000)

;; Move camera to tile position
(move-camera-to-tile 50 50)

;; Move camera by delta
(move-camera-by 100 100)

;; Move camera in direction (:up, :down, :left, :right)
(move-camera-direction :up :speed 200)
(move-camera-direction :down :speed 200)
(move-camera-direction :left :speed 200)
(move-camera-direction :right :speed 200)

;; Move camera to throne position
(move-to-throne)
```

### 1. Zoom Control
Control the camera zoom level:

```clojure
;; Get current zoom level (0 = normal, positive = zoomed out)
(get-zoom)

;; Set zoom level
(set-zoom 0)   ; Normal
(set-zoom 1)   ; Zoomed out
(set-zoom -1)  ; Zoomed in

;; Incremental zoom
(zoom-in)      ; Zoom in by 1 level
(zoom-out)     ; Zoom out by 1 level
(zoom-by 2)    ; Zoom out by 2 levels
```

### 2. Warehouse/Stockpile Creation
Create automated warehouses with furniture and walls:

```clojure
;; Create a warehouse using update-once (single frame)
(create-warehouse-once 251 430 5 5)

;; With custom material
(create-warehouse-once 120 120 3 3 :material-name "STONE")

;; Without auto-placed furniture
(create-warehouse-once 280 400 5 5 :place-furniture false)
```

**Parameters:**
- `center-x`, `center-y`: Center tile coordinates
- `width`, `height`: Warehouse dimensions in tiles
- `:material-name`: Building material ("WOOD" or "STONE")
- `:place-furniture`: Auto-place crates (default: true)

### 3. Furniture Inspection
Inspect and query furniture in the game world:

```clojure
;; Get furniture at specific tile
(furniture-info 259 398)
(has-furniture? 259 398)
(has-crate? 259 398)

;; Scan an area for furniture
(scan-furniture-area 259 398 3 3)

;; Get complete warehouse furniture info
(warehouse-furniture-info 261 400 5 5)
```

### 4. Time Flow Control
Control game speed and pause state:

```clojure
;; Set time speed (0 = paused, 1 = normal, 5 = fast)
(set-time-speed 0)   ; Pause
(set-time-speed 1)   ; Normal
(set-time-speed 5)   ; 5x speed
(set-time-speed 25)  ; 25x speed

;; Convenience functions
(pause-time)       ; Pause the game
(resume-time)      ; Resume at 1x speed
(toggle-pause)     ; Toggle pause state
(time-speed-0x)    ; Pause
(time-speed-1x)    ; Normal
(time-speed-5x)    ; Fast
(time-speed-25x)   ; Very fast

;; Get current speed
(get-time-speed)
(get-time-speed-target)
```

### 5. Pure Planning Functions
Test warehouse layout planning without side effects:

```clojure
;; Find edge tiles for an area
(find-edge-tiles 261 430 5 5)

;; Calculate door position
(calculate-door-position 261 430 5 5 :side :top)

;; Calculate furniture positions
(calculate-furniture-positions 261 430 5 5 2 1)

;; Calculate occupied tiles by furniture
(calculate-occupied-tiles furniture-positions 2 1)

;; Find optimal door position
(find-door-position 261 430 5 5 occupied-tiles :preferred-side :top)
```

## Usage Patterns

### Pattern 1: Automate Warehouse Building

```bash
# Load the namespace and build warehouses
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])
;; Create multiple warehouses
(t/create-warehouse-once 251 430 5 5)
(t/create-warehouse-once 280 430 5 5)
(t/create-warehouse-once 251 450 5 5)
EOF
```

### Pattern 2: Camera Tour

```bash
# Move camera to different locations
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])
(t/move-to-throne)
(t/set-zoom 0)
(t/move-camera-direction :up :speed 500)
EOF
```

### Pattern 3: Inspect and Plan

```bash
# Check existing furniture before building
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])
;; Check if area is clear
(t/scan-furniture-area 250 430 10 10)
;; Plan new warehouse
(t/find-edge-tiles 261 430 5 5)
EOF
```

### Pattern 4: Time Control for Construction

```bash
# Pause while building, then resume
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])
(t/pause-time)
(t/create-warehouse-once 251 430 5 5)
(t/resume-time)
EOF
```

## Game State Queries

```clojure
;; Get stockpile room and constructor
(get-stockpile-room)
(get-stockpile-constructor)

;; Get furniture data instance
(get-furniture-data)

;; Get game speed instance
(get-game-speed)
```

## Important Notes

1. **Namespace Requirements**: Always require the namespace first:
   ```clojure
   (require '[repl.tutorial1 :as t])
   ;; Or use fully qualified names after loading
   ```

2. **Coordinate System**: The game uses tile coordinates. Use `move-camera-to-tile` for tile-based positioning.

3. **Warehouse Planning**: Use pure functions (like `find-edge-tiles`, `calculate-door-position`) to plan before building.

4. **Frame Updates**: Use `create-warehouse-once` for single-frame execution to avoid multi-frame state issues.

5. **Camera Speed**: Camera movement speed is automatically adjusted based on zoom level.

6. **Material Names**: Valid building materials are "WOOD" and "STONE" (use `get-building-material` to verify).

7. **Furniture Dimensions**: When planning furniture, specify item width and height (e.g., crates are 2x1).

8. **Door Placement**: Doors are automatically placed on the preferred side (:top, :bottom, :left, :right) at an optimal position.

## Example Workflows

### Workflow 1: Outpost Construction

1. Move camera to target location
2. Pause game
3. Scan area for existing structures
4. Plan warehouse layout
5. Build warehouse
6. Resume game

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])
(t/move-camera-to-tile 100 100)
(t/pause-time)
(t/scan-furniture-area 95 95 15 15)
(t/create-warehouse-once 100 100 5 5)
(t/resume-time)
EOF
```

### Workflow 2: Settlement Expansion

1. Find throne position
2. Move camera to throne
3. Calculate new warehouse positions
4. Build multiple warehouses
5. Verify construction

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])
(t/move-to-throne)
(t/create-warehouse-once 251 430 5 5)
(t/create-warehouse-once 280 430 5 5)
(doseq [[x y] [[251 450] [280 450]]]
  (t/create-warehouse-once x y 5 5))
(warehouse-furniture-info 251 430 5 5)
EOF
```

### Workflow 3: Debug Furniture Placement

1. Get furniture info at specific tiles
2. Verify crate placement
3. Check occupied tiles
4. Adjust planning as needed

```bash
clj-nrepl-eval -p 50000 <<'EOF'
(require '[repl.tutorial1 :as t])
(t/furniture-info 259 398)
(t/has-crate? 259 398)
(t/warehouse-furniture-info 261 400 5 5)
EOF
```

## Troubleshooting

### Warehouse not appearing
- Check if tile coordinates are valid
- Verify building material exists
- Ensure `place-furniture` is set correctly
- Check for conflicting structures: `(scan-furniture-area start-x start-y width height)`

### Camera not moving
- Verify game window is focused
- Check zoom level affects movement speed
- Try tile-based positioning: `(move-camera-to-tile x y)`

### Furniture not placing
- Verify `furnisher-item` exists: `(get-stockpile-constructor)`
- Check if tiles are occupied: `(has-furniture? x y)`
- Ensure warehouse area is clear

### Time control not working
- Check if game is in a menu
- Verify game speed instance: `(get-game-speed)`
- Try toggle instead: `(toggle-pause)`
