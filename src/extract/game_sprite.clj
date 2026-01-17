(ns extract.game-sprite
  "Extract game sprites from Songs of Syx.
   
   This namespace provides functions to extract various game sprites including:
   - Textures (fire, water, bumps, dots, displacement maps)
   - Load screen sprite
   - UI special sprites
   - Construction overlay sprites
   
   See: doc/wiki/extract/sprites-game.md for documentation."
  (:import
   [init.sprite SPRITES]
   [java.awt.image BufferedImage]
   [java.io File]
   [java.util.zip ZipFile]
   [javax.imageio ImageIO]))

;; ============================================
;; Texture Sprite Paths (纹理精灵图路径)
;; ============================================

(def texture-paths
  "Mapping of texture names to their paths in data.zip"
  {:bumps "data/assets/sprite/textures/Bumps.png"
   :displacement-big "data/assets/sprite/textures/Displacement_Big.png"
   :displacement-low "data/assets/sprite/textures/Displacement_low.png"
   :displacement-small "data/assets/sprite/textures/Displacement_small.png"
   :displacement-tiny "data/assets/sprite/textures/Displacement_tiny.png"
   :dots "data/assets/sprite/textures/Dots.png"
   :fire "data/assets/sprite/textures/Fire.png"
   :water "data/assets/sprite/textures/Water.png"})

(def ui-sprite-paths
  "Mapping of UI sprite names to their paths in data.zip"
  {:load-screen "data/assets/sprite/image/_LoadScreen.png"
   :specials "data/assets/sprite/ui/Specials.png"
   :cons "data/assets/sprite/ui/Cons.png"
   :panels "data/assets/sprite/ui/Panels.png"
   :decor "data/assets/sprite/ui/Decor.png"
   :titles "data/assets/sprite/ui/Titles.png"
   :title-box "data/assets/sprite/ui/TitleBox.png"
   :faction-banners "data/assets/sprite/ui/FactionBanners.png"
   :division-symbols "data/assets/sprite/ui/DivisionSymbols.png"})

;; ============================================
;; Utility Functions (工具函数)
;; ============================================

(defn- ensure-parent-dirs
  "Ensure parent directories exist for the given path."
  [path]
  (let [file (File. path)
        parent (.getParentFile file)]
    (when (and parent (not (.exists parent)))
      (.mkdirs parent))))

(defn read-image-from-zip
  "Read an image from inside a zip file.
   Returns BufferedImage or nil on error."
  [zip-path zip-entry-path]
  (try
    (let [zip-file (File. zip-path)]
      (when (and (.exists zip-file) (.isFile zip-file))
        (let [zip-file-obj (ZipFile. zip-file)
              zip-entry (.getEntry zip-file-obj zip-entry-path)]
          (if zip-entry
            (let [entry-stream (.getInputStream zip-file-obj zip-entry)
                  img (ImageIO/read entry-stream)]
              (.close entry-stream)
              (.close zip-file-obj)
              img)
            (do
              (.close zip-file-obj)
              nil)))))
    (catch Exception _
      nil)))

(defn export-full-image
  "Export a full image from data.zip to the output path.
   
   Arguments:
     zip-entry-path - Path within data.zip
     output-path - Where to save the image
   
   Options:
     :scale - Scale factor (default 1)"
  [zip-entry-path output-path & {:keys [scale] :or {scale 1}}]
  (try
    (let [zip-path "base/data.zip"
          source-img (read-image-from-zip zip-path zip-entry-path)]
      (if source-img
        (let [src-width (.getWidth source-img)
              src-height (.getHeight source-img)
              scaled-width (* src-width scale)
              scaled-height (* src-height scale)
              output-img (if (= scale 1)
                           source-img
                           (let [img (BufferedImage. scaled-width scaled-height BufferedImage/TYPE_INT_ARGB)
                                 g (.createGraphics img)]
                             (.drawImage g source-img
                                         0 0 scaled-width scaled-height
                                         0 0 src-width src-height
                                         nil)
                             (.dispose g)
                             img))]
          (ensure-parent-dirs output-path)
          (ImageIO/write output-img "png" (File. output-path))
          {:success true
           :path output-path
           :source-size {:width src-width :height src-height}
           :output-size {:width scaled-width :height scaled-height}})
        {:success false :error (str "Image not found: " zip-entry-path)}))
    (catch Exception e
      {:success false :error (str (.getName (.getClass e)) ": " (.getMessage e))})))

(defn crop-from-image
  "Crop a region from an image in data.zip.
   
   Arguments:
     zip-entry-path - Path within data.zip
     x, y - Top-left corner of crop region
     width, height - Dimensions of crop region  
     output-path - Where to save the cropped image
   
   Options:
     :scale - Scale factor (default 1)"
  [zip-entry-path x y width height output-path & {:keys [scale] :or {scale 1}}]
  (try
    (let [zip-path "base/data.zip"
          source-img (read-image-from-zip zip-path zip-entry-path)]
      (if source-img
        (let [src-width (.getWidth source-img)
              src-height (.getHeight source-img)]
          ;; Validate crop region
          (when (or (< x 0) (< y 0) (> (+ x width) src-width) (> (+ y height) src-height))
            (throw (IllegalArgumentException.
                    (str "Crop region out of bounds: "
                         "image=" src-width "x" src-height ", "
                         "crop=[" x "," y "," width "," height "]"))))
          (let [scaled-width (* width scale)
                scaled-height (* height scale)
                output-img (BufferedImage. scaled-width scaled-height BufferedImage/TYPE_INT_ARGB)
                g (.createGraphics output-img)]
            (.drawImage g source-img
                        0 0 scaled-width scaled-height
                        x y (+ x width) (+ y height)
                        nil)
            (.dispose g)
            (ensure-parent-dirs output-path)
            (ImageIO/write output-img "png" (File. output-path))
            {:success true
             :path output-path
             :source-size {:width src-width :height src-height}
             :crop {:x x :y y :width width :height height}
             :output-size {:width scaled-width :height scaled-height}}))
        {:success false :error (str "Image not found: " zip-entry-path)}))
    (catch Exception e
      {:success false :error (str (.getName (.getClass e)) ": " (.getMessage e))})))

;; ============================================
;; Texture Export Functions (纹理导出函数)
;; ============================================

(defn list-textures
  "List all available texture names."
  []
  (keys texture-paths))

(defn export-texture
  "Export a texture sprite from data.zip.
   
   Arguments:
     texture-key - Keyword from texture-paths (:fire, :water, :bumps, etc.)
     output-path - Where to save the texture
   
   Options:
     :scale - Scale factor (default 1)
   
   Example:
     (export-texture :fire \"output/wiki/sprites/textures/fire.png\")"
  [texture-key output-path & {:keys [scale] :or {scale 1}}]
  (if-let [zip-entry-path (get texture-paths texture-key)]
    (export-full-image zip-entry-path output-path :scale scale)
    {:success false :error (str "Unknown texture: " texture-key)}))

(defn export-all-textures
  "Export all texture sprites to a directory.
   
   Arguments:
     output-dir - Directory to save textures
   
   Options:
     :scale - Scale factor (default 1)
   
   Example:
     (export-all-textures \"output/wiki/sprites/textures\")"
  [output-dir & {:keys [scale] :or {scale 1}}]
  (let [results (for [[texture-key _] texture-paths]
                  (let [filename (str (name texture-key) ".png")
                        path (str output-dir "/" filename)]
                    (assoc (export-texture texture-key path :scale scale)
                           :texture-key texture-key)))]
    {:success true
     :count (count (filter :success results))
     :total (count texture-paths)
     :output-dir output-dir
     :results (vec results)}))

;; ============================================
;; Load Screen Export (加载屏幕导出)
;; ============================================

(defn export-load-screen
  "Export the game load screen sprite.
   
   Arguments:
     output-path - Where to save the load screen
   
   Options:
     :scale - Scale factor (default 1)
   
   Example:
     (export-load-screen \"output/wiki/sprites/ui/load_screen.png\")"
  [output-path & {:keys [scale] :or {scale 1}}]
  (export-full-image (:load-screen ui-sprite-paths) output-path :scale scale))

;; ============================================
;; UI Sprite Export (UI精灵图导出)
;; ============================================

(defn list-ui-sprites
  "List all available UI sprite names."
  []
  (keys ui-sprite-paths))

(defn export-ui-sprite
  "Export a UI sprite sheet from data.zip.
   
   Arguments:
     sprite-key - Keyword from ui-sprite-paths (:specials, :cons, :panels, etc.)
     output-path - Where to save the sprite sheet
   
   Options:
     :scale - Scale factor (default 1)
   
   Example:
     (export-ui-sprite :specials \"output/wiki/sprites/ui/specials.png\")"
  [sprite-key output-path & {:keys [scale] :or {scale 1}}]
  (if-let [zip-entry-path (get ui-sprite-paths sprite-key)]
    (export-full-image zip-entry-path output-path :scale scale)
    {:success false :error (str "Unknown UI sprite: " sprite-key)}))

(defn export-all-ui-sprites
  "Export all UI sprite sheets to a directory.
   
   Arguments:
     output-dir - Directory to save sprites
   
   Options:
     :scale - Scale factor (default 1)
   
   Example:
     (export-all-ui-sprites \"output/wiki/sprites/ui\")"
  [output-dir & {:keys [scale] :or {scale 1}}]
  (let [results (for [[sprite-key _] ui-sprite-paths]
                  (let [filename (str (name sprite-key) ".png")
                        path (str output-dir "/" filename)]
                    (assoc (export-ui-sprite sprite-key path :scale scale)
                           :sprite-key sprite-key)))]
    {:success true
     :count (count (filter :success results))
     :total (count ui-sprite-paths)
     :output-dir output-dir
     :results (vec results)}))

;; ============================================
;; Game Sprite Sheet Info (游戏精灵表信息)
;; ============================================

(def game-sheet-folders
  "Game sprite sheet folder structure"
  {:1x1 {:path "data/assets/sprite/game/1x1"
         :description "1x1 tile sprites (furniture, decorations, small objects)"}
   :2x2 {:path "data/assets/sprite/game/2x2"
         :description "2x2 tile sprites (medium objects)"}
   :3x3 {:path "data/assets/sprite/game/3x3"
         :description "3x3 tile sprites (large objects)"}
   :box {:path "data/assets/sprite/game/box"
         :description "Box sprites"}
   :combo {:path "data/assets/sprite/game/combo"
           :description "Combo sprites"}
   :texture {:path "data/assets/sprite/game/texture"
             :description "Game texture sprites"}})

(defn list-game-sheet-files
  "List all game sprite sheet files in a category folder.
   
   Arguments:
     category-key - Keyword from game-sheet-folders (:1x1, :2x2, :3x3, :box, :combo, :texture)
   
   Returns list of file names in the folder."
  [category-key]
  (try
    (let [folder-info (get game-sheet-folders category-key)
          folder-path (:path folder-info)
          zip-path "base/data.zip"
          zip-file (ZipFile. (File. zip-path))
          entries (.entries zip-file)
          prefix (str folder-path "/")
          files (atom [])]
      (while (.hasMoreElements entries)
        (let [entry (.nextElement entries)
              name (.getName entry)]
          (when (and (.startsWith name prefix)
                     (.endsWith name ".png")
                     (not (.isDirectory entry)))
            (swap! files conj (subs name (count prefix))))))
      (.close zip-file)
      (sort @files))
    (catch Exception e
      {:error (.getMessage e)})))

(defn export-game-sheet
  "Export a game sprite sheet PNG.
   
   Arguments:
     category-key - Keyword from game-sheet-folders (:1x1, :2x2, :3x3, etc.)
     filename - Name of the sprite sheet file (e.g., \"ANIMAL.png\")
     output-path - Where to save the sprite sheet
   
   Options:
     :scale - Scale factor (default 1)
   
   Example:
     (export-game-sheet :1x1 \"ANIMAL.png\" \"output/wiki/sprites/game/1x1/ANIMAL.png\")"
  [category-key filename output-path & {:keys [scale] :or {scale 1}}]
  (if-let [folder-info (get game-sheet-folders category-key)]
    (let [zip-entry-path (str (:path folder-info) "/" filename)]
      (export-full-image zip-entry-path output-path :scale scale))
    {:success false :error (str "Unknown game sheet category: " category-key)}))

;; ============================================
;; Batch Export Functions (批量导出函数)
;; ============================================

(defn extract-all-game-sprites
  "Extract all game sprites to the output directory.
   
   Arguments:
     output-dir - Base output directory
   
   Options:
     :scale - Scale factor (default 1)
     :include-textures - Export texture sprites (default true)
     :include-ui - Export UI sprites (default true)
     :include-load-screen - Export load screen (default true)
   
   Example:
     (extract-all-game-sprites \"output/wiki/sprites\")"
  [output-dir & {:keys [scale include-textures include-ui include-load-screen]
                 :or {scale 1 include-textures true include-ui true include-load-screen true}}]
  (let [results (atom {:textures nil :ui nil :load-screen nil})]
    
    ;; Export textures
    (when include-textures
      (swap! results assoc :textures 
             (export-all-textures (str output-dir "/textures") :scale scale)))
    
    ;; Export UI sprites
    (when include-ui
      (swap! results assoc :ui
             (export-all-ui-sprites (str output-dir "/ui") :scale scale)))
    
    ;; Export load screen
    (when include-load-screen
      (swap! results assoc :load-screen
             (export-load-screen (str output-dir "/ui/load_screen.png") :scale scale)))
    
    {:success true
     :output-dir output-dir
     :results @results}))

(comment
  (extract-all-game-sprites "output/wiki/sprites/game")
  :rcf)

;; ============================================
;; Runtime Sprite Info (运行时精灵信息)
;; ============================================

(defn textures-info
  "Get information about the Textures object from the game runtime.
   Requires game to be running."
  []
  (try
    (let [tex (SPRITES/textures)]
      {:class (str (class tex))
       :fields (mapv (fn [f] 
                       {:name (.getName f)
                        :type (str (.getType f))})
                     (.getFields (class tex)))})
    (catch Exception e
      {:error (.getMessage e)})))

(defn specials-info
  "Get information about the UISpecials object from the game runtime.
   Requires game to be running."
  []
  (try
    (let [specials (SPRITES/specials)]
      {:class (str (class specials))
       :methods (mapv (fn [m]
                        {:name (.getName m)
                         :return-type (str (.getReturnType m))})
                      (->> (.getMethods (class specials))
                           (filter #(zero? (count (.getParameterTypes %))))))})
    (catch Exception e
      {:error (.getMessage e)})))

(defn cons-info
  "Get information about the UIConses (construction sprites) object.
   Requires game to be running."
  []
  (try
    (let [cons (SPRITES/cons)]
      {:class (str (class cons))
       :fields (mapv (fn [f]
                       {:name (.getName f)
                        :type (str (.getType f))})
                     (.getFields (class cons)))})
    (catch Exception e
      {:error (.getMessage e)})))

(defn game-sheets-info
  "Get information about the GameSheets object.
   Requires game to be running."
  []
  (try
    (let [game (SPRITES/GAME)]
      {:class (str (class game))
       :methods (mapv (fn [m]
                        {:name (.getName m)
                         :return-type (str (.getReturnType m))
                         :param-count (count (.getParameterTypes m))})
                      (.getMethods (class game)))})
    (catch Exception e
      {:error (.getMessage e)})))

;; ============================================
;; REPL Examples
;; ============================================

(comment
  ;; List available sprites
  (list-textures)
  ;; => (:bumps :displacement-big :displacement-low :displacement-small 
  ;;     :displacement-tiny :dots :fire :water)
  
  (list-ui-sprites)
  ;; => (:load-screen :specials :cons :panels :decor :titles :title-box 
  ;;     :faction-banners :division-symbols)
  
  ;; Export single texture
  (export-texture :fire "output/wiki/sprites/textures/fire.png")
  
  ;; Export all textures
  (export-all-textures "output/wiki/sprites/textures")
  
  ;; Export load screen
  (export-load-screen "output/wiki/sprites/ui/load_screen.png")
  
  ;; Export all UI sprites
  (export-all-ui-sprites "output/wiki/sprites/ui")
  
  ;; Export all game sprites at once
  (extract-all-game-sprites "output/wiki/sprites")
  
  ;; List game sheet files in 1x1 folder
  (list-game-sheet-files :1x1)
  
  ;; Export a specific game sheet
  (export-game-sheet :1x1 "ANIMAL.png" "output/wiki/sprites/game/1x1/ANIMAL.png")
  
  ;; Get runtime info (requires game running)
  (textures-info)
  (specials-info)
  (cons-info)
  (game-sheets-info)
  
  :rcf)

