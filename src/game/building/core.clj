(ns game.building.core
  "Core building operations for all room types in Songs of Syx.

   This namespace provides low-level, reusable functions for:
   - Accessing room blueprints from registry
   - Creating TmpArea workspaces
   - Marking building areas
   - Creating ConstructionInit objects
   - Finalizing building creation (createClean + clear)

   These functions are orthogonal to specific building types and can be
   composed to create wells, hearths, homes, warehouses, farms, etc."

  (:import
   [settlement.main SETT]
   [settlement.room.main RoomBlueprintImp]
   [settlement.room.main.construction ConstructionInit]))

;; ============================================================================
;; Blueprint Access
;; ============================================================================

(defn get-blueprint-by-key
  "Get room blueprint by unique key (e.g., 'WELL', 'HOME', 'FARM_VEG').
   Returns RoomBlueprintImp or nil if not found.

   This is the primary way to access any room type by string key."
  [key]
  (let [all-blueprints (RoomBlueprintImp/imps)]
    (first (filter #(= (.key %) (name key)) all-blueprints))))

(comment
  (get-blueprint-by-key "WELL")
  (get-blueprint-by-key "HOME")
  (get-blueprint-by-key "FARM_VEG")
  :rcf)

(defn get-blueprint-constructor
  "Get the Furnisher/constructor for a blueprint.
   Throws exception if blueprint or constructor is nil."
  [blueprint]
  (when-not blueprint
    (throw (Exception. (str "Blueprint is nil, cannot get constructor"))))
  (let [constructor (.constructor blueprint)]
    (when-not constructor
      (throw (Exception. (str "Constructor is nil for blueprint: " (.key blueprint))))
    constructor)))

(comment
  (def well-bp (get-blueprint-by-key "WELL"))
  (get-blueprint-constructor well-bp)
  :rcf)

;; ============================================================================
;; TmpArea Operations
;; ============================================================================

(defn create-tmp-area
  "Create a new TmpArea with a unique name.

   IMPORTANT: TmpArea names should be unique to avoid 'In use by' errors.
   Use timestamp or UUID when creating multiple areas in sequence.

   Parameters:
   - rooms: SETT.ROOMS() instance
   - name: String identifier for this temporary area

   Returns: TmpArea instance ready for building operations"
  [rooms name]
  (.tmpArea rooms name))

(comment
  (def rooms (SETT/ROOMS))
  (create-tmp-area rooms "well_1234567890")
  (create-tmp-area rooms (str "home_" (System/currentTimeMillis)))
  :rcf)

(defn mark-tmp-area
  "Mark all tiles in a rectangular area within a TmpArea.

   This reserves the tiles for building. Must be called before
   placing furniture or creating the construction.

   Parameters:
   - tmp: TmpArea instance
   - start-x: Top-left X coordinate
   - start-y: Top-left Y coordinate
   - width: Width in tiles
   - height: Height in tiles"
  [tmp start-x start-y width height]
  (doseq [y (range height)
          x (range width)]
    (.set tmp (+ start-x x) (+ start-y y))))

(comment
  (def rooms (SETT/ROOMS))
  (def tmp (create-tmp-area rooms "test"))
  (mark-tmp-area tmp 100 100 5 5)
  :rcf)

(defn mark-tmp-area-centered
  "Mark area using center coordinates (more intuitive for building functions).

   Calculates top-left corner from center and marks the area.

   Parameters:
   - tmp: TmpArea instance
   - center-x: Center X coordinate
   - center-y: Center Y coordinate
   - width: Width in tiles
   - height: Height in tiles"
  [tmp center-x center-y width height]
  (let [start-x (- center-x (quot width 2))
        start-y (- center-y (quot height 2))]
    (mark-tmp-area tmp start-x start-y width height)))

(comment
  (def rooms (SETT/ROOMS))
  (def tmp (create-tmp-area rooms "test"))
  (mark-tmp-area-centered tmp 150 150 5 5)
  :rcf)

(defn clear-tmp-area
  "Clear TmpArea and release all tile reservations.

   CRITICAL: Must be called after construction is created to avoid
   'In use by' errors on subsequent building operations.

   Parameters:
   - tmp: TmpArea instance

   Note: After clearing, the tmp instance should not be reused."
  [tmp]
  (.clear tmp))

(comment
  (def rooms (SETT/ROOMS))
  (def tmp (create-tmp-area rooms "test"))
  (mark-tmp-area tmp 100 100 5 5)
  (clear-tmp-area tmp)
  :rcf)

;; ============================================================================
;; ConstructionInit Creation
;; ============================================================================

(defn make-construction-init
  "Create a ConstructionInit object with all parameters.

   ConstructionInit bundles together:
   - upgrade: Upgrade level (usually 0)
   - constructor: The Furnisher/constructor from blueprint
   - tbuilding: The TBuilding material (WOOD, STONE, etc.)
   - degrade: Degradation level (usually 0)
   - state: RoomState (usually nil)

   Parameters:
   - upgrade: Integer upgrade level
   - constructor: Furnisher instance
   - tbuilding: TBuilding material instance
   - opts: Optional map with :degrade and :state keys

   Returns: ConstructionInit instance"
  ([upgrade constructor tbuilding]
   (make-construction-init upgrade constructor tbuilding {:degrade 0 :state nil}))

  ([upgrade constructor tbuilding {:keys [degrade state]
                                         :or {degrade 0 state nil}}]
    (ConstructionInit. upgrade constructor tbuilding degrade state)))

(comment
    
  :rcf)

;; ============================================================================
;; Finalization
;; ============================================================================

(defn finalize-building
  "Complete building creation by calling createClean and clearing TmpArea.

   This is the final step in ALL building operations:
   1. Call construction.createClean() to activate the building
   2. Call tmp.clear() to release tile reservations

   Parameters:
   - tmp: TmpArea instance
   - construction-init: ConstructionInit object with building parameters

   Note: After this function completes, the tmp should not be reused."
  [tmp construction-init]
  (let [construction (.construction (SETT/ROOMS))]
    (.createClean construction tmp construction-init)
    (.clear tmp)))

(comment
  (def rooms (SETT/ROOMS))
  (def tmp (create-tmp-area rooms "test"))
  (mark-tmp-area tmp 100 100 5 5)
  (def cons-init (make-construction-init 0 (.constructor (get-blueprint-by-key "WELL")) (game.common/get-building-material "STONE")))
  (finalize-building tmp cons-init)
  :rcf)

;; ============================================================================
;; Unique Name Generation
;; ============================================================================

(defn generate-tmp-name
  "Generate a unique TmpArea name to avoid 'In use by' errors.

   Use this when creating multiple buildings in sequence to ensure
   each TmpArea has a unique identifier.

   Parameters:
   - prefix: String prefix like 'well', 'home', 'warehouse'

   Returns: Unique string name (e.g., 'well_1234567890')"

  ([] (generate-tmp-name "tmp"))

  ([prefix]
   (str prefix "_" (System/currentTimeMillis))))

(comment
  (generate-tmp-name)
  (generate-tmp-name "well")
  (generate-tmp-name "home")
  :rcf)

(defn generate-uuid-tmp-name
  "Generate a UUID-based TmpArea name for maximum uniqueness.

   Parameters:
   - prefix: String prefix like 'well', 'home', 'warehouse'

   Returns: Unique string name (e.g., 'home_a1b2c3d-4e5f-6g7h')"
  ([prefix] (str prefix "_" (java.util.UUID/randomUUID))))

(comment
  (generate-uuid-tmp-name "tmp")
  (generate-uuid-tmp-name "well")
  :rcf)

;; ============================================================================
;; Complete Example Usage
;; ============================================================================

(comment
  "=== Example: Building a well using orthogonal functions ===

   1. Get blueprint and constructor
   2. Create unique TmpArea name
   3. Mark the building area
   4. Finalize construction"

  (require '[game.common :as common])

  (def rooms (SETT/ROOMS))
  (def well-bp (get-blueprint-by-key "WELL"))
  (def well-constructor (get-blueprint-constructor well-bp))
  (def tbuilding (common/get-building-material "STONE"))

  ;; Create construction init
  (def cons-init (make-construction-init 0 well-constructor tbuilding))

  ;; Create unique TmpArea
  (def tmp (create-tmp-area rooms (generate-tmp-name "well")))

  ;; Mark area centered at (100, 100), 3x3 size
  (mark-tmp-area-centered tmp 100 100 3 3)

  ;; Finalize building (createClean + clear)
  (finalize-building tmp cons-init)

  :done)
