(ns game.sprite
  (:import
   [init.sprite SPRITES]
   [init.sprite.UI UI]
   [init.race RACES]
   [java.awt.image BufferedImage]
   [java.io File]
   [java.util.zip ZipFile]
   [javax.imageio ImageIO]))

"base/data.zip/data/assets/sprite/里面有各种 sprite sheet"
"最外面一层 gap 的宽度是 6"
"小图标 18 行 2 列, 24 * 24"

;; Note: SheetType, GameSheets, and Textures are imported for type hints
;; and potential future use. They may be used in function parameters or examples.

;; ============================================
;; UI Icons (图标)
;; ============================================

;; Get the Icons instance
(defn icons []
  (SPRITES/icons))

(comment
  (icons)
  :rcf)

;; Get small icons (16x16)
(defn icons-small []
  (.-s (icons)))

(comment
  (icons-small)
  :rcf)

;; Get medium icons (24x24)
(defn icons-medium []
  (.-m (icons)))

(comment
  (icons-medium)
  :rcf)

;; Get large icons (32x32)
(defn icons-large []
  (.-l (icons)))

;; Get a specific small icon by field name
;; Example: (icon-small "sword") -> accesses .sword field
(defn icon-small [icon-name]
  (let [icons-s (icons-small)]
    (try
      (let [field (.getField (class icons-s) icon-name)]
        (.get field icons-s))
      (catch Exception _
        nil))))

;; ============================================
;; Icon Inspection (图标检查)
;; ============================================

;; Get icon information (size, width, height, etc.)
(defn icon-info [icon]
  (when icon
    {:class (class icon)
     :size (try (.-size icon) (catch Exception _ nil))
     :width (try (.width icon) (catch Exception _ nil))
     :height (try (.height icon) (catch Exception _ nil))
     :has-texture (try (some? (.texture icon)) (catch Exception _ false))}))

;; Get icon sprite sheet information
;; Icons are cropped from sprite sheets, this function returns the sheet and tile index
(defn icon-sheet-info [icon]
  (when icon
    (try
      ;; Check if it's an IconSheet (which has sheet and tile fields)
      (let [icon-class (class icon)
            icon-sheet-class (Class/forName "init.sprite.UI.Icon$IconSheet")]
        (if (= icon-sheet-class icon-class)
          ;; It's an IconSheet, get the sheet and tile
          (let [sheet-field (.getDeclaredField icon-sheet-class "sheet")
                tile-field (.getDeclaredField icon-sheet-class "tile")]
            (.setAccessible sheet-field true)
            (.setAccessible tile-field true)
            (let [sheet (.get sheet-field icon)
                  tile (.get tile-field icon)]
              {:is-sheet true
               :sheet sheet
               :tile-index tile
               :total-tiles (try (.tiles sheet) (catch Exception _ nil))
               :sheet-size (try (.size sheet) (catch Exception _ nil))}))
          ;; Not an IconSheet, return basic info
          {:is-sheet false
           :note "Icon is not from a sprite sheet"}))
      (catch Exception e
        {:error (.getMessage e)}))))

;; Get the tile index from an IconSheet icon
(defn icon-tile-index [icon]
  (when icon
    (try
      (let [icon-class (class icon)
            icon-sheet-class (Class/forName "init.sprite.UI.Icon$IconSheet")]
        (if (= icon-sheet-class icon-class)
          (let [tile-field (.getDeclaredField icon-sheet-class "tile")]
            (.setAccessible tile-field true)
            (.get tile-field icon))
          nil))
      (catch Exception _ nil))))

;; Get the TILE_SHEET from an IconSheet icon
(defn icon-sheet [icon]
  (when icon
    (try
      (let [icon-class (class icon)
            icon-sheet-class (Class/forName "init.sprite.UI.Icon$IconSheet")]
        (if (= icon-sheet-class icon-class)
          (let [sheet-field (.getDeclaredField icon-sheet-class "sheet")]
            (.setAccessible sheet-field true)
            (.get sheet-field icon))
          nil))
      (catch Exception _ nil))))

;; Get comprehensive icon information including sprite sheet details
(defn icon-full-info [icon]
  (when icon
    (merge (icon-info icon)
           (icon-sheet-info icon))))

;; Get all available icon fields from icons-small
(defn list-small-icon-fields []
  (let [icons-s (icons-small)
        fields (.getFields (class icons-s))
        public-fields (filter #(java.lang.reflect.Modifier/isPublic (.getModifiers %)) fields)]
    (map #(.getName %) public-fields)))

;; Get all available icon fields from icons-medium
(defn list-medium-icon-fields []
  (let [icons-m (icons-medium)
        fields (.getFields (class icons-m))
        public-fields (filter #(java.lang.reflect.Modifier/isPublic (.getModifiers %)) fields)]
    (map #(.getName %) public-fields)))

;; Get all available icon fields from icons-large
(defn list-large-icon-fields []
  (let [icons-l (icons-large)
        fields (.getFields (class icons-l))
        public-fields (filter #(java.lang.reflect.Modifier/isPublic (.getModifiers %)) fields)]
    (map #(.getName %) public-fields)))

(comment
  ;; Get icon and inspect it
  (def sword-icon (icon-small "sword"))
  
  ;; Inspect icon properties
  (icon-info sword-icon)
  ;; => {:class init.sprite.UI.Icons$S$IconS, :size 16, :width 16, :height 16, :has-texture true}
  
  ;; Get sprite sheet information (icons are cropped from sprite sheets)
  (icon-sheet-info sword-icon)
  ;; => {:is-sheet true, :sheet TILE_SHEET, :tile-index 25, :total-tiles 100, :sheet-size 16}
  
  (icon-tile-index sword-icon) ; => 25 (the tile index in the sprite sheet)
  (icon-sheet sword-icon)      ; => TILE_SHEET object
  
  ;; Get full information including sprite sheet details
  (icon-full-info sword-icon)
  ;; => Complete information about the icon including sprite sheet data
  
  ;; List all available icon names
  (take 10 (list-small-icon-fields))
  ;; => ("magnifier" "minifier" "minimap" "arrowUp" ...)
  
  (take 10 (list-medium-icon-fields))
  (take 10 (list-large-icon-fields))
  
  ;; Direct field access (alternative to icon-small function)
  (.-sword (icons-small))
  (.-sword (icons-medium))
  (.-sword (icons-large))
  :rcf)

;; Get a specific medium icon by field name
(defn icon-medium [icon-name]
  (let [icons-m (icons-medium)]
    (try
      (let [field (.getField (class icons-m) icon-name)]
        (.get field icons-m))
      (catch Exception _
        nil))))

;; Get a specific large icon by field name
(defn icon-large [icon-name]
  (let [icons-l (icons-large)]
    (try
      (let [field (.getField (class icons-l) icon-name)]
        (.get field icons-l))
      (catch Exception _
        nil))))

;; ============================================
;; Game Sprites (游戏内建筑/物体)
;; ============================================

;; Get the GameSheets instance
(defn game-sheets []
  (SPRITES/GAME))

;; Get sheets for a specific type and file
;; type should be a SheetType (e.g., SheetType.s1x1)
;; file is the sprite file name
(defn get-sheets [type file]
  (let [game (game-sheets)]
    (.sheets game type file nil)))

;; Get raw TILE_SHEET for a specific type and file
(defn get-raw-sheets [type file]
  (let [game (game-sheets)]
    (.raws game type file nil)))

;; Get a single raw sprite by type, file, and row
(defn get-raw-sprite [type file row]
  (let [game (game-sheets)]
    (.raw game type file row nil)))

;; Get overlay sprite for a type
(defn get-overlay [type]
  (let [game (game-sheets)]
    (.overlay game type)))

;; ============================================
;; Textures (纹理)
;; ============================================

;; Get the Textures instance
(defn textures []
  (SPRITES/textures))

;; Get specific texture by field name
;; Available textures: dis_big, dis_small, dis_tiny, dis_low, fire, water, bumps, dots
;; Example: (texture "fire") or use convenience functions like (texture-fire)
(defn texture [texture-name]
  (let [tex (textures)]
    (try
      (let [field (.getField (class tex) texture-name)]
        (.get field tex))
      (catch Exception _
        nil))))

;; Convenience functions for common textures
(defn texture-fire []
  (.-fire (textures)))

(defn texture-water []
  (.-water (textures)))

(defn texture-bumps []
  (.-bumps (textures)))

(defn texture-dots []
  (.-dots (textures)))

(defn texture-displacement-big []
  (.-dis_big (textures)))

(defn texture-displacement-small []
  (.-dis_small (textures)))

(defn texture-displacement-tiny []
  (.-dis_tiny (textures)))

(defn texture-displacement-low []
  (.-dis_low (textures)))

;; ============================================
;; Other Sprite Resources
;; ============================================

;; Get construction/overlay sprites
(defn cons-sprites []
  (SPRITES/cons))

;; Get settlement sprites
;; Note: SPRITES.sett() method does not exist in the current codebase
;; Settlement sprites are accessed through game-sheets() or other sprite accessors
;; This function is commented out as the method doesn't exist
(comment
  (defn settlement-sprites []
    ;; SPRITES/sett does not exist
    nil)
  :rcf)

;; Get load screen sprite
(defn load-screen []
  (SPRITES/loadScreen))

;; Get special sprites
(defn special-sprites []
  (SPRITES/specials))

;; Get UI image maker
(defn ui-image []
  (UI/image))

;; Get UI image by path
(defn get-ui-image [path]
  (let [image-maker (ui-image)]
    (.get image-maker path)))

;; ============================================
;; Race Sprite Functions (种族 Sprite 函数)
;; ============================================

(defn get-race [race-key]
  (try
    (let [all-races (RACES/all)
          key-upper (.toUpperCase race-key)]
      (first (filter #(= key-upper (.key %)) all-races)))
    (catch Exception e
      (println "Error getting race:" (.getMessage e))
      nil)))

(comment
  (get-race "Human")
  :rcf)

;; Get race sprite sheet (sheet or lay)
;; sheet-type: :sheet (站立/行走) or :lay (躺下)
;; race-key: race name (e.g., "Human")
;; adult: true for adult, false for child (default: true)
(defn get-race-sheet [sheet-type race-key & {:keys [adult] :or {adult true}}]
  (try
    (let [race (get-race race-key)]
      (when race
        (let [appearance (.appearance race)
              r-type (if adult (.adult appearance) (.child appearance))
              race-sheet (.sheet r-type)]
          (case sheet-type
            :sheet (.sheet race-sheet)
            :lay (.lay race-sheet)
            nil))))
    (catch Exception e
      (println "Error getting race sheet:" (.getMessage e))
      nil)))

(comment
  (get-race-sheet :sheet "Human")
  :rcf)

;; Action types and their row indices in the PNG
;; PNG stores ONE sprite per action (the game rotates/mirrors for 8 directions)
(def action-rows
  {:feet-none 0
   :feet-right 1
   :feet-right2 2
   :feet-left 3
   :feet-left2 4
   :tunic 5
   :torso-still 6
   :torso-right 7
   :torso-right2 8
   :torso-right3 9
   :torso-left 10
   :torso-left2 11
   :torso-left3 12
   :torso-carry 13
   :torso-out 14
   :torso-out2 15
   :head 16
   :shadow 17})

;; Get the PNG row index for an action type (0-17)
(defn get-action-row [action]
  (get action-rows action 0))

;; Legacy function for game's internal tile index (action * 8 + direction)
;; This is used by the game engine, not needed for PNG export
(defn get-tile-index [sheet-type action direction]
  (case sheet-type
    :sheet (+ (* (get-action-row action) 8) (mod direction 8))
    :lay (mod direction 6)
    0))

(comment 
  (get-action-row :head) ;; => 16
  (get-action-row :torso-still) ;; => 6
  :rcf)

;; Get texture coordinates for a tile
;; Returns a TextureCoords object, which contains:
;; - x1, y1, x2, y2: coordinates in the OpenGL texture (0.0-1.0 normalized)
;; - width, height: pixel dimensions
;; NOTE: TextureCoords does NOT contain pixel data!
;; The actual texture data is stored in GPU memory (OpenGL) and cannot be directly accessed.
;; To extract pixels, you must either:
;; 1. Read from the source PNG file (use export-sprite-from-png)
;; 2. Render to a framebuffer and read pixels (complex, requires OpenGL context)
(defn get-tile-texture [tile-sheet tile-index]
  (try
    (when tile-sheet
      (.getTexture tile-sheet tile-index))
    (catch Exception e
      (println "Error getting texture:" (.getMessage e))
      nil)))

;; Get tile sheet information
(defn tile-sheet-info [tile-sheet]
  (when tile-sheet
    (try
      {:size (.size tile-sheet)
       :tiles (.tiles tile-sheet)
       :width (try (.width tile-sheet) (catch Exception _ nil))
       :height (try (.height tile-sheet) (catch Exception _ nil))}
      (catch Exception e
        {:error (.getMessage e)}))))

;; Export sprite from original PNG file
;; This reads directly from the source PNG file in data.zip and extracts the sprite region
;; For :sheet - index is row (0-17, one per action type)
;; For :lay - index is pose (0-5)
(defn export-sprite-from-png
  [race-key sheet-type index output-path & {:keys [scale] :or {scale 1}}]
  (try
    (let [zip-path "base/data.zip"
          zip-entry-path (str "data/assets/sprite/race/" race-key ".png")
          zip-file (File. zip-path)]
      (if (and (.exists zip-file) (.isFile zip-file))
        (let [zip-file-obj (ZipFile. zip-file)
              zip-entry (.getEntry zip-file-obj zip-entry-path)]
          (if zip-entry
            (let [entry-stream (.getInputStream zip-file-obj zip-entry)
                  source-img (ImageIO/read entry-stream)
                  _ (.close entry-stream)
                  _ (.close zip-file-obj)
                  ;; PNG layout: 448x546 pixels
                  ;; Left section (sheet): 18 rows x 2 cols (body + shadow), 24x24 each
                  ;; Right section (lay): 3 rows x 4 cols, 32x32 each, 6 body sprites
                  [sprite-width sprite-height src-x src-y]
                  (case sheet-type
                    :sheet (let [sprite-size 24
                                 padding 6
                                 row index  ; Direct row index (0-17)
                                 x (+ padding 0)  ; Column 0 (body), x = 6
                                 y (+ padding (* row (+ sprite-size padding)))]  ; 6 + row * 30
                             [sprite-size sprite-size x y])
                    :lay (let [sprite-size 32
                               padding 6
                               sheet-body-width 66  ; 6 + 2*(24+6)
                               row (quot index 2)
                               col-in-row (mod index 2)
                               tx (* col-in-row 2)  ; Skip shadow columns
                               x (+ sheet-body-width padding (* tx (+ sprite-size padding)))
                               y (+ padding (* row (+ sprite-size padding)))]
                           [sprite-size sprite-size x y])
                    [24 24 0 0])
                  scaled-width (* sprite-width scale)
                  scaled-height (* sprite-height scale)
                  output-img (BufferedImage. scaled-width scaled-height BufferedImage/TYPE_INT_ARGB)
                  g (.createGraphics output-img)]
              (.drawImage g source-img
                          0 0 scaled-width scaled-height
                          src-x src-y (+ src-x sprite-width) (+ src-y sprite-height)
                          nil)
              (.dispose g)
              (let [output-file (File. output-path)
                    parent-dir (.getParentFile output-file)]
                (when (and parent-dir (not (.exists parent-dir)))
                  (.mkdirs parent-dir)))
              (ImageIO/write output-img "png" (File. output-path))
              {:success true
               :path output-path
               :size sprite-width
               :index index
               :source-region {:x src-x :y src-y :width sprite-width :height sprite-height}})
            (do
              (.close zip-file-obj)
              {:success false :error (str "Entry not found in zip: " zip-entry-path)})))
        {:success false :error (str "Zip file not found: " zip-path)}))
    (catch Exception e
      {:success false :error (str (.getClass (.getName e)) ": " (.getMessage e))})))

;; Export a race sprite from PNG
;; For :sheet - pass action keyword (:head, :torso-still, etc.)
;;   PNG stores ONE sprite per action; game rotates/mirrors for 8 directions
;; For :lay - pass pose index (0-5)
;; adult: reserved for future child sprite support
(defn export-race-sprite 
  [sheet-type race-key action-or-index output-path 
   & {:keys [adult scale] :or {adult true scale 1}}]
  (let [_ adult  ; Reserved for future child sprite support
        index (case sheet-type
                :sheet (get-action-row action-or-index)
                :lay action-or-index)]
    (export-sprite-from-png race-key sheet-type index output-path :scale scale)))

(comment
  ;; Export standing/walking sprites (18 action types, game rotates for directions)
  (export-race-sprite :sheet "Human" :head "output/head.png")
  (export-race-sprite :sheet "Human" :tunic "output/tunic.png")
  (export-race-sprite :sheet "Human" :torso-still "output/torso-still.png")
  (export-race-sprite :sheet "Human" :torso-right "output/torso-right.png")
  (export-race-sprite :sheet "Human" :feet-none "output/feet-none.png")
  
  ;; Export lying sprites (6 poses)
  (export-race-sprite :lay "Human" 0 "output/lay_0.png")
  (export-race-sprite :lay "Human" 1 "output/lay_1.png")
  
  :rcf)

;; ============================================
;; PNG Crop Functions (PNG 裁剪函数)
;; ============================================

;; Crop a region from a PNG file in data.zip
;; Arguments:
;;   zip-entry-path: Path within zip (e.g., "data/assets/sprite/race/Human.png")
;;   x, y: Top-left corner of the region to crop
;;   width, height: Dimensions of the region to crop
;;   output-path: Where to save the cropped image
;;   scale: Optional scale factor (default: 1)
(defn crop-from-png
  [zip-entry-path x y width height output-path & {:keys [scale] :or {scale 1}}]
  (try
    (let [zip-path "base/data.zip"
          zip-file (File. zip-path)]
      (if (and (.exists zip-file) (.isFile zip-file))
        (let [zip-file-obj (ZipFile. zip-path)
              zip-entry (.getEntry zip-file-obj zip-entry-path)]
          (if zip-entry
            (let [entry-stream (.getInputStream zip-file-obj zip-entry)
                  source-img (ImageIO/read entry-stream)
                  _ (.close entry-stream)
                  _ (.close zip-file-obj)
                  ;; Validate crop region is within image bounds
                  src-width (.getWidth source-img)
                  src-height (.getHeight source-img)
                  _ (when (or (< x 0) (< y 0) (> (+ x width) src-width) (> (+ y height) src-height))
                      (throw (IllegalArgumentException.
                              (str "Crop region out of bounds: "
                                   "image=" src-width "x" src-height ", "
                                   "crop=[" x "," y "," width "," height "]"))))
                  scaled-width (* width scale)
                  scaled-height (* height scale)
                  output-img (BufferedImage. scaled-width scaled-height BufferedImage/TYPE_INT_ARGB)
                  g (.createGraphics output-img)]
              ;; Draw the cropped region from source to output
              (.drawImage g source-img
                          0 0 scaled-width scaled-height
                          x y (+ x width) (+ y height)
                          nil)
              (.dispose g)
              ;; Ensure output directory exists
              (let [output-file (File. output-path)
                    parent-dir (.getParentFile output-file)]
                (when (and parent-dir (not (.exists parent-dir)))
                  (.mkdirs parent-dir)))
              ;; Save the image
              (ImageIO/write output-img "png" (File. output-path))
              {:success true
               :path output-path
               :source-size {:width src-width :height src-height}
               :crop {:x x :y y :width width :height height}
               :output-size {:width scaled-width :height scaled-height}})
            {:success false :error (str "Entry not found in zip: " zip-entry-path)}))
        {:success false :error (str "Zip file not found: " zip-path)}))
    (catch Exception e
      {:success false :error (str (.getClass (.getName e)) ": " (.getMessage e))})))

(comment
  ;; Crop from any PNG in data.zip
  (crop-from-png "data/assets/sprite/race/Human.png"
                  6 6 24 24 "output/test.png")
  
  (crop-from-png "data/assets/sprite/race/Human.png"
                 66 0 380 114 "output/lay-section.png")
  :rcf)

(comment
  ;; Examples:
  
  ;; Get icons
  (icons)
  (icons-small)
  (icons-medium)
  (icons-large)
  
  ;; Get specific icons (use field name as string)
  (icon-small "sword")
  (icon-medium "sword")
  (icon-large "sword")
  
  ;; Or access directly via field access
  (.-sword (icons-small))
  (.-sword (icons-medium))
  (.-sword (icons-large))
  
  ;; Get game sprites
  (game-sheets)
  
  ;; Get textures
  (textures)
  (texture-fire)
  (texture-water)
  (texture "fire")
  
  ;; Get game sprite sheets (requires SheetType)
  ;; Example usage (commented out as it requires specific sprite files):
  ;; (import '[init.sprite.game SheetType])
  ;; (get-sheets SheetType/s1x1 "Building")
  ;; (get-overlay SheetType/s1x1)
  
  ;; Get raw sprite sheets
  ;; (get-raw-sheets SheetType/s1x1 "Building")
  ;; (get-raw-sprite SheetType/s1x1 "Building" 0)
  
  ;; Get other resources
  (cons-sprites)
  ;; (settlement-sprites)  ; Note: SPRITES.sett() does not exist
  (load-screen)
  (special-sprites)
  
  :rcf)
