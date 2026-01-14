(ns game.sprite
  (:import
   [init.sprite SPRITES]
   [init.sprite.UI UI]
   [init.sprite.game GameSheets SheetType]
   [init.sprite Textures]
   [init.race RACES]
   [snake2d.util.datatypes DIR]
   [java.awt.image BufferedImage]
   [java.io File]
   [javax.imageio ImageIO]))

"base/data.zip/data/assets/sprite/里面有各种 sprite sheet"

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

;; Get icon size
(defn icon-size [icon]
  (when icon
    (try (.-size icon) (catch Exception _ nil))))

;; Get icon width
(defn icon-width [icon]
  (when icon
    (try (.width icon) (catch Exception _ nil))))

;; Get icon height
(defn icon-height [icon]
  (when icon
    (try (.height icon) (catch Exception _ nil))))

;; Get icon texture coordinates (if available)
(defn icon-texture [icon]
  (when icon
    (try (.texture icon) (catch Exception _ nil))))

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

;; Get tile index for a direction and action
;; For sheet (standing/walking):
;;   - action: :head, :torso-still, :torso-right, :torso-left, :torso-carry, :tunic, :feet-none, :feet-right, :feet-left, :shadow
;;   - direction: 0-7 (8 directions, where 0 is typically right/east)
;; For lay (lying down):
;;   - action: ignored (lay sheet only has direction-based sprites), pass nil
;;   - direction: 0-5 (6 directions for lying sprites)
(defn get-tile-index [sheet-type action direction]
  (try
    (let [NR 8  ; Number of directions for sheet
          ;; Base indices for sheet actions (from HSpriteConst)
          base-indices {:head (* 0 NR)
                        :torso-still (* 0 NR)
                        :torso-right (* 1 NR)
                        :torso-left (* 2 NR)
                        :torso-carry (* 3 NR)
                        :tunic (* 2 NR)
                        :feet-none (* 0 NR)
                        :feet-right (* 1 NR)
                        :feet-left (* 3 NR)
                        :shadow (* 1 NR)}] 
      (case sheet-type
        :sheet (let [base (get base-indices action 0)]
                 (+ base (mod direction NR)))
        :lay (mod direction 6)  ; Lay sheet has 6 sprites
        0))
    (catch Exception e
      (println "Error calculating tile index:" (.getMessage e))
      0)))

(comment
  (def base-indices
    (let [NR 8]
      {:head (* 0 NR)
       :torso-still (* 0 NR)
       :torso-right (* 1 NR)
       :torso-left (* 2 NR)
       :torso-carry (* 3 NR)
       :tunic (* 2 NR)
       :feet-none (* 0 NR)
       :feet-right (* 1 NR)
       :feet-left (* 3 NR)
       :shadow (* 1 NR)}))
  
  (get base-indices :torso-still)

  (get-tile-index :sheet :head 0)
  (get-tile-index :sheet :torso-right 0)
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
;; This reads directly from the source PNG file and extracts the sprite region
(defn export-sprite-from-png
  [race-key sheet-type tile-index output-path & {:keys [scale] :or {scale 1}}]
  (try
    (let [;; Path to the original PNG file
          base-path "base/data.zip/data/assets/sprite/race"
          png-file (str base-path "/" race-key ".png")
          file-obj (File. png-file)]
      (if (.exists file-obj)
        (let [source-img (ImageIO/read file-obj)
              ;; Sprite sheet layout: 448x546 pixels
              ;; Sheet: 18 rows, each row has 2 sprites (left=body, right=shadow)
              ;; Each sprite in source is ~224x30 pixels, output is 24x24
              ;; Lay: 6 sprites from right side, each 32x32
              [sprite-width sprite-height src-x src-y]
              (case sheet-type
                :sheet (let [;; Sheet layout from RaceSheet.java:
                             ;; - Source: 448x546 pixels
                             ;; - s.singles.init(0, 0, 1, 1, 2, 18, d.s24)
                             ;;   = from (0,0), 1x1 tiles, 2 per row, 18 rows, output 24x24
                             ;; - Code skips shadow (column 1), only uses body (column 0)
                             ;; - Each row in source: 546/18 ≈ 30.33 pixels high
                             ;; - Body sprite: left half (0-224px), shadow: right half (224-448px)
                             row tile-index  ; Tile index = row number (0-17)
                             sprite-w 224   ; Body sprite width (left half of 448px image)
                             sprite-h 30    ; Row height (546/18, rounded)
                             x 0            ; Always from left side (body sprites only)
                             y (* row sprite-h)]
                         [24 24 x y])  ; Output size is 24x24
                :lay (let [;; Lay layout from RaceSheet.java:
                           ;; - s.singles.init(s.singles.body().x2(), 0, 1, 1, 4, 3, d.s32)
                           ;;   = from right half (x2()), 1x1 tiles, 4 per row, 3 rows, output 32x32
                           ;; - Code skips shadow sprites, only uses body
                           ;; - Each row: 546/3 = 182 pixels high
                           ;; - Each sprite in row: 224/4 = 56 pixels wide (but we use every other)
                           row (quot tile-index 2)  ; Which row (0-2), skip shadow sprites
                           sprite-h 182             ; Row height (546/3)
                           x 224                    ; Start from right half (x=224)
                           y (* row sprite-h)]
                       [32 32 x y])  ; Output size is 32x32
                [24 24 0 0])
              scaled-width (* sprite-width scale)
              scaled-height (* sprite-height scale)
              output-img (BufferedImage. scaled-width scaled-height BufferedImage/TYPE_INT_ARGB)
              g (.createGraphics output-img)]
          ;; Draw the sprite region from source to output
          (.drawImage g source-img
                      0 0 scaled-width scaled-height
                      src-x src-y (+ src-x sprite-width) (+ src-y sprite-height)
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
           :size sprite-width
           :tile-index tile-index
           :source-region {:x src-x :y src-y :width sprite-width :height sprite-height}})
        {:success false :error (str "Source PNG file not found: " png-file)}))
    (catch Exception e
      {:success false :error (.getMessage e)})))

;; Export a sprite tile to PNG file
;; This is a simplified version - actual implementation may need to use game's rendering system
;; Note: Direct texture extraction may not be straightforward due to OpenGL texture management
;; TextureCoords only contains texture coordinates, not pixel data!
(defn export-sprite-to-png 
  [tile-sheet tile-index output-path & {:keys [scale] :or {scale 1}}]
  (try
    (when tile-sheet
      (let [size (.size tile-sheet)
            scaled-size (* size scale)
            texture (.getTexture tile-sheet tile-index)
            img (BufferedImage. scaled-size scaled-size BufferedImage/TYPE_INT_ARGB)
            g (.createGraphics img)]
        ;; Note: TextureCoords only contains coordinates (x1, y1, x2, y2) in the texture,
        ;; NOT the actual pixel data. The texture data is in GPU memory (OpenGL),
        ;; which cannot be directly accessed from Java.
        (println "Warning: Cannot extract pixels from TextureCoords.")
        (println "TextureCoords only contains:" 
                 "x1=" (try (.x1 texture) (catch Exception _ "N/A"))
                 "y1=" (try (.y1 texture) (catch Exception _ "N/A"))
                 "x2=" (try (.x2 texture) (catch Exception _ "N/A"))
                 "y2=" (try (.y2 texture) (catch Exception _ "N/A")))
        (println "Use export-sprite-from-png instead to read from source PNG file.")
        ;; For now, create a placeholder image
        (.setColor g java.awt.Color/BLACK)
        (.fillRect g 0 0 scaled-size scaled-size)
        (.setColor g java.awt.Color/WHITE)
        (.drawString g (str "Tile " tile-index) 10 20)
        (.dispose g)
        (ImageIO/write img "png" (File. output-path))
        {:success true :path output-path :size size :tile-index tile-index}))
    (catch Exception e
      {:success false :error (.getMessage e)})))

;; Convenience function to get and export a race sprite
;; This function uses the source PNG file method (recommended)
(defn export-race-sprite 
  [sheet-type race-key action direction output-path 
   & {:keys [adult scale] :or {adult true scale 1}}]
  (let [tile-index (get-tile-index sheet-type action direction)]
    (export-sprite-from-png race-key sheet-type tile-index output-path :scale scale)))

;; Alternative: Export using tile-sheet (creates placeholder, doesn't extract actual pixels)
(defn export-race-sprite-from-sheet
  [sheet-type race-key action direction output-path 
   & {:keys [adult scale] :or {adult true scale 1}}]
  (let [tile-sheet (get-race-sheet sheet-type race-key :adult adult)
        tile-index (get-tile-index sheet-type action direction)]
    (if tile-sheet
      (export-sprite-to-png tile-sheet tile-index output-path :scale scale)
      {:success false :error "Failed to get tile sheet"})))

(comment
  

  (export-race-sprite :sheet "Human" :head 0 "output/head_0.png")
  (export-race-sprite :sheet "Human" :torso-right 3 "output/head_3.png")
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
