(ns repl.tutorial1
  (:require
   [repl.utils :as utils])
  (:import
   [game GAME]
   [view.main VIEW]
   [settlement.main SETT]
   [settlement.room.main.construction ConstructionInit]
   [settlement.room.main.throne THRONE]
   [init.structure STRUCTURES]
   [init.resources RESOURCES]))

;; Get the game window for camera control
(defn get-game-window []
  (let [sett-view (VIEW/s)]
    (.getWindow sett-view)))

;; Move camera to a specific pixel position
(defn move-camera-to [x y]
  (let [window (get-game-window)]
    (.centerAt window x y)))

;; Move camera to a specific tile position
(defn move-camera-to-tile [tile-x tile-y]
  (let [window (get-game-window)]
    (.centerAtTile window tile-x tile-y)))

;; Move camera by a delta (increment)
(defn move-camera-by [dx dy]
  (let [window (get-game-window)]
    (.inc window dx dy)))

;; Move camera in a direction (mimicking WASD)
;; Directions: :up (W), :down (S), :left (A), :right (D)
(defn move-camera-direction [direction & {:keys [speed] :or {speed 100}}]
  (let [window (get-game-window)
        zoomout (.zoomout window)
        ;; Adjust speed based on zoom level (similar to how the game does it)
        adjusted-speed (* speed (Math/pow 2 zoomout))
        [dx dy] (case direction
                  :up [0 (- adjusted-speed)]
                  :down [0 adjusted-speed]
                  :left [(- adjusted-speed) 0]
                  :right [adjusted-speed 0]
                  [0 0])]
    (.inc window dx dy)))

(comment
  ;; Move camera to a specific position
  (move-camera-to 1000 1000)
  
  ;; Move camera to a specific tile
  (move-camera-to-tile 50 50)
  
  ;; Move camera by a delta (one-time movement)
  (move-camera-by 100 100)
  
  ;; Move camera in a direction once
  (move-camera-direction :up :speed 200)
  (move-camera-direction :down :speed 200)
  (move-camera-direction :left :speed 200)
  (move-camera-direction :right :speed 200)
  :rcf)

;; Get current zoom level
(defn get-zoom []
  (let [window (get-game-window)]
    (.zoomout window)))

;; Set zoom level (0 = normal, positive = zoomed out, negative = zoomed in)
(defn set-zoom [level]
  (let [window (get-game-window)]
    (.setZoomout window level)))

;; Zoom in (decrease zoom level by 1)
(defn zoom-in []
  (let [window (get-game-window)]
    (.zoomInc window -1)))

;; Zoom out (increase zoom level by 1)
(defn zoom-out []
  (let [window (get-game-window)]
    (.zoomInc window 1)))

;; Zoom by a delta amount (positive = zoom out, negative = zoom in)
(defn zoom-by [delta]
  (let [window (get-game-window)]
    (.zoomInc window delta)))

(comment
   ;; Zoom functions
  (get-zoom)                    ; Get current zoom level
  (set-zoom 0)                 ; Set zoom to normal (0)
  (set-zoom 1)                 ; Set zoom to level 1 (zoomed out)
  (set-zoom -1)                ; Set zoom to level -1 (zoomed in)
  (zoom-in)                    ; Zoom in by 1 level
  (zoom-out)                   ; Zoom out by 1 level
  (zoom-by 2)                  ; Zoom out by 2 levels
  (zoom-by -1)                 ; Zoom in by 1 level
  :rcf)

;; Warehouse/Stockpile creation functions

;; Get the stockpile room and constructor
(defn get-stockpile-room []
  (let [rooms (SETT/ROOMS)]
    (.-STOCKPILE rooms)))

(comment
  (get-stockpile-room)
  :rcf)

(defn get-stockpile-constructor []
  (let [stockpile-room (get-stockpile-room)]
    (.constructor stockpile-room)))

(comment
  (get-stockpile-constructor)
  :rcf)

;; Get building materials (e.g., wood, stone)
;; Returns the TBuilding for the material (ConstructionInit needs TBuilding, not Structure)
;; material-name should be a resource name like "WOOD", "STONE", etc.
;; This finds the structure by matching the resource type, then converts to TBuilding
(defn get-building-material [material-name]
  (let [material-upper (.toUpperCase material-name)
        resource (case material-upper
                   "WOOD" (RESOURCES/WOOD)
                   "STONE" (RESOURCES/STONE)
                   (throw (Exception. (str "Unknown material: " material-name ". Supported: WOOD, STONE"))))
        all-structures (STRUCTURES/all)
        structure (first (filter #(= resource (.-resource %)) all-structures))]
    (if structure
      ;; Convert Structure to TBuilding using BUILDINGS.get(Structure)
      (let [buildings (.-BUILDINGS (SETT/TERRAIN))]
        (.get buildings structure))
      (throw (Exception. (str "Could not find structure for material: " material-name))))))

(comment
  (get-building-material "WOOD")
  (get-building-material "STONE")
  :rcf)

;; Create a warehouse/stockpile at the specified location
;; center-x, center-y: center tile coordinates
;; width, height: dimensions of the warehouse (in tiles)
;; material-name: building material name (e.g., "WOOD", "STONE")
;; upgrade: upgrade level (default 0)
(defn create-warehouse [center-x center-y width height & {:keys [material-name upgrade] 
                                                           :or {material-name "WOOD" upgrade 0}}]
  (let [rooms (SETT/ROOMS)
        stockpile-constructor (get-stockpile-constructor)
        tbuilding (get-building-material material-name)  ; Returns TBuilding (not Structure)
        degrade 0  ; No degradation
        state nil  ; No special state
        
        ;; Create ConstructionInit (note: third parameter is TBuilding, not Structure)
        construction-init (ConstructionInit. upgrade stockpile-constructor tbuilding degrade state)
        
        ;; Get temporary area
        tmp (.tmpArea rooms "warehouse")]
    
    ;; Set the building area
    (let [start-x (- center-x (quot width 2))
          start-y (- center-y (quot height 2))]
      (doseq [y (range height)
              x (range width)]
        (.set tmp (+ start-x x) (+ start-y y))))
    
    ;; Create the construction site
    (.createClean (.construction rooms) tmp construction-init)
    
    ;; Clear temporary area
    (.clear tmp)
    
    {:success true
     :center-x center-x
     :center-y center-y
     :width width
     :height height}))

;; Create a warehouse using update-once (ensures it happens in a single frame)
(defn create-warehouse-once [center-x center-y width height & {:keys [material-name upgrade] 
                                                                 :or {material-name "WOOD" upgrade 0}}]
  (utils/update-once 
   (fn [_ds]
     (create-warehouse center-x center-y width height 
                      :material-name material-name 
                      :upgrade upgrade))))

;; Example usage:
(comment
  ;; Warehouse creation
  ;; Create a 5x5 warehouse at tile (100, 100) using wood
  (THRONE/coo)

  (create-warehouse-once 261 400 5 5)
  ;; move camera to the warehouse
  (move-camera-to-tile 100 100)
  
  ;; Create a 3x3 warehouse at tile (120, 120) using stone
  (create-warehouse-once 120 120 3 3 :material-name "STONE")
  
  ;; Create a larger warehouse (7x7) at tile (130, 130)
  (create-warehouse-once 130 130 7 7)
  
  :rcf)

;; Move camera to the throne position
;; Gets the throne's coordinate (in tiles) and moves the camera to center on it
(defn move-to-throne []
  (let [throne-coo (THRONE/coo)
        window (get-game-window)
        tile-x (.x throne-coo)
        tile-y (.y throne-coo)]
    (.centerAtTile window tile-x tile-y)))

(comment
  ;; Move camera to throne
  (move-to-throne)
  :rcf)

;; ============================================================================
;; Time Flow Control
;; ============================================================================

;; Get the GameSpeed instance
(defn get-game-speed []
  (GAME/SPEED))

;; Set game speed (0 = paused, 1 = normal, 5 = 5x, 25 = 25x, etc.)
(defn set-time-speed [speed]
  (let [game-speed (get-game-speed)]
    (.speedSet game-speed speed)))

;; Get current time speed
(defn get-time-speed []
  (let [game-speed (get-game-speed)]
    (.speed game-speed)))

;; Get target time speed (what speed is set to, before any adjustments)
(defn get-time-speed-target []
  (let [game-speed (get-game-speed)]
    (.speedTarget game-speed)))

;; Pause the game (sets speed to 0)
(defn pause-time []
  (set-time-speed 0))

;; Resume time at normal speed (1x)
(defn resume-time []
  (set-time-speed 1))

;; Toggle pause (if paused, resume to previous speed; if running, pause)
(defn toggle-pause []
  (let [game-speed (get-game-speed)]
    (.togglePause game-speed)))

;; Set time to specific speeds (matching the game's speed buttons)
(defn time-speed-0x
  "Pause (0x speed)"
  []
  (set-time-speed 0))

(defn time-speed-1x
  "Normal speed (1x)"
  []
  (set-time-speed 1))

(defn time-speed-5x
  "Fast speed (5x)"
  []
  (set-time-speed 5))

(defn time-speed-25x
  "Very fast speed (25x)"
  []
  (set-time-speed 25))

(comment
  ;; Time flow control examples
  (pause-time)                    ; Pause the game
  (resume-time)                   ; Resume at normal speed
  (toggle-pause)                  ; Toggle pause/resume
  
  ;; Set specific speeds
  (time-speed-0x)                  ; Pause (0x)
  (time-speed-1x)                  ; Normal (1x)
  (time-speed-5x)                  ; Fast (5x)
  (time-speed-25x)                 ; Very fast (25x)
  
  ;; Custom speed
  (set-time-speed 10)              ; Set to 10x speed
  
  ;; Get current speed
  (get-time-speed)                 ; Get actual current speed
  (get-time-speed-target)          ; Get target speed setting
  
  :rcf)
