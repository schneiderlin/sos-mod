(ns game.tech
  "Technology access functions for Songs of Syx.
   Technologies are research items that unlock various boosts and rooms.
   
   This namespace provides reusable functions for:
   - Accessing technology registry
   - Getting technology properties
   - Querying tech trees and requirements"
  (:import
   [init.tech TECHS TECH TechTree TechCost TechCurrency]
   [init.tech TECH$TechRequirement]
   [snake2d.util.color COLOR]))

;; ============================================
;; Technology Registry Access
;; ============================================

(defn all-techs
  "Get all technologies in the game.
   Returns: LIST<TECH>"
  []
  (TECHS/ALL))

(defn tech-count
  "Get total number of technologies."
  []
  (.size (all-techs)))

(defn all-trees
  "Get all tech trees.
   Returns: LIST<TechTree>"
  []
  (TECHS/TREES))

(defn tree-count
  "Get total number of tech trees."
  []
  (.size (all-trees)))

(defn all-costs
  "Get all tech currencies/cost types.
   Returns: LIST<TechCurrency>"
  []
  (TECHS/COSTS))

(defn cost-types-count
  "Get number of tech currency types."
  []
  (.size (all-costs)))

(defn tech-info
  "Get tech system INFO (name and description)."
  []
  (TECHS/INFO))

;; ============================================
;; Technology Properties
;; ============================================

(defn tech-key
  "Get technology unique key (e.g., \"CIVIL_SLAVE\")."
  [^TECH tech]
  (.key tech))

(defn tech-index
  "Get technology index in ALL list."
  [^TECH tech]
  (.index tech))

(defn tech-name
  "Get technology display name."
  [^TECH tech]
  (str (.name tech)))

(defn tech-desc
  "Get technology description."
  [^TECH tech]
  (str (.desc tech)))

(defn tech-level-max
  "Get maximum research level for this tech."
  [^TECH tech]
  (.-levelMax tech))

(defn tech-cost-total
  "Get total cost for all levels."
  [^TECH tech]
  (.-costTotal tech))

(defn tech-level-cost-inc
  "Get cost increase per level."
  [^TECH tech]
  (.-levelCostInc tech))

(defn tech-costs
  "Get list of costs for this tech.
   Returns: LIST<TechCost>"
  [^TECH tech]
  (.-costs tech))

(defn tech-tree
  "Get the TechTree this tech belongs to."
  [^TECH tech]
  (.-tree tech))

(defn tech-color
  "Get tech color."
  [^TECH tech]
  (.-color tech))

(defn tech-ai-amount
  "Get AI research amount (0-1)."
  [^TECH tech]
  (.-AIAmount tech))

(defn tech-requirements
  "Get all requirements for this tech.
   Returns: LIST<TechRequirement>"
  [^TECH tech]
  (.requires tech))

(defn tech-requirements-nodes
  "Get pruned requirement nodes (direct dependencies).
   Returns: LIST<TechRequirement>"
  [^TECH tech]
  (.requiresNodes tech))

(defn tech-boosters
  "Get BoostSpecs for this tech."
  [^TECH tech]
  (.-boosters tech))

(defn tech-icon
  "Get tech icon sprite."
  [^TECH tech]
  (.icon tech))

;; ============================================
;; TechTree Properties
;; ============================================

(defn tree-key
  "Get tree unique key."
  [^TechTree tree]
  (.key tree))

(defn tree-name
  "Get tree display name."
  [^TechTree tree]
  (str (.name tree)))

(defn tree-color
  "Get tree color."
  [^TechTree tree]
  (.-color tree))

(defn tree-category
  "Get tree category (0-5)."
  [^TechTree tree]
  (.-cat tree))

(defn tree-nodes
  "Get 2D array of tech nodes in the tree.
   Returns: TECH[][] where nil means empty slot."
  [^TechTree tree]
  (.-nodes tree))

(defn tree-techs
  "Get all techs in this tree as a flat sequence."
  [^TechTree tree]
  (->> (tree-nodes tree)
       (mapcat seq)
       (filter some?)))

(defn tree-rows
  "Get number of rows in tree."
  [^TechTree tree]
  (count (tree-nodes tree)))

(defn tree-max-cols
  "Get maximum columns (constant)."
  []
  TechTree/MAX_COLS)

;; ============================================
;; TechCost Properties
;; ============================================

(defn cost-currency
  "Get the TechCurrency for this cost."
  [^TechCost cost]
  (.-cu cost))

(defn cost-amount
  "Get the amount for this cost."
  [^TechCost cost]
  (.-amount cost))

;; ============================================
;; TechCurrency Properties
;; ============================================

(defn currency-boostable
  "Get the Boostable associated with this currency."
  [^TechCurrency cu]
  (.-bo cu))

(defn currency-index
  "Get currency index."
  [^TechCurrency cu]
  (.-index cu))

(defn currency-name
  "Get currency display name from its boostable."
  [^TechCurrency cu]
  (str (.name (currency-boostable cu))))

;; ============================================
;; TechRequirement Properties
;; ============================================

(defn requirement-tech
  "Get the required TECH."
  [^TECH$TechRequirement req]
  (.-tech req))

(defn requirement-level
  "Get the required level."
  [^TECH$TechRequirement req]
  (.-level req))

;; ============================================
;; Color Conversion
;; ============================================

(defn color->hex
  "Convert COLOR to hex string."
  [^COLOR color]
  (let [r (bit-and (.red color) 0xFF)
        g (bit-and (.green color) 0xFF)
        b (bit-and (.blue color) 0xFF)
        rgb (bit-or r (bit-shift-left g 8) (bit-shift-left b 16))]
    (format "#%06X" rgb)))

(defn color->map
  "Convert COLOR to RGB map."
  [^COLOR color]
  {:red (.red color)
   :green (.green color)
   :blue (.blue color)
   :hex (color->hex color)})

;; ============================================
;; Data Conversion
;; ============================================

(defn cost->map
  "Convert TechCost to Clojure map."
  [^TechCost cost]
  {:currency-name (currency-name (cost-currency cost))
   :currency-index (currency-index (cost-currency cost))
   :amount (cost-amount cost)})

(defn requirement->map
  "Convert TechRequirement to Clojure map."
  [^TECH$TechRequirement req]
  {:tech-key (tech-key (requirement-tech req))
   :level (requirement-level req)})

(defn tech->map
  "Convert TECH to Clojure map."
  [^TECH tech]
  (let [tree (tech-tree tech)]
    {:key (tech-key tech)
     :index (tech-index tech)
     :name (tech-name tech)
     :description (tech-desc tech)
     :level-max (tech-level-max tech)
     :cost-total (tech-cost-total tech)
     :level-cost-inc (tech-level-cost-inc tech)
     :ai-amount (tech-ai-amount tech)
     :tree-key (when tree (tree-key tree))
     :color (color->map (tech-color tech))
     :costs (mapv cost->map (tech-costs tech))
     :requirements (mapv requirement->map (tech-requirements tech))
     :requirement-nodes (mapv requirement->map (tech-requirements-nodes tech))}))

(defn tree->map
  "Convert TechTree to Clojure map."
  [^TechTree tree]
  {:key (tree-key tree)
   :name (tree-name tree)
   :category (tree-category tree)
   :color (color->map (tree-color tree))
   :rows (tree-rows tree)
   :tech-count (count (tree-techs tree))
   :tech-keys (mapv tech-key (tree-techs tree))})

(defn tree->map-full
  "Convert TechTree to Clojure map with full tech data."
  [^TechTree tree]
  (let [nodes (tree-nodes tree)
        node-grid (vec (for [row nodes]
                         (vec (for [tech row]
                                (when tech (tech-key tech))))))]
    {:key (tree-key tree)
     :name (tree-name tree)
     :category (tree-category tree)
     :color (color->map (tree-color tree))
     :rows (tree-rows tree)
     :node-grid node-grid
     :techs (mapv tech->map (tree-techs tree))}))

(defn currency->map
  "Convert TechCurrency to Clojure map."
  [^TechCurrency cu]
  {:index (currency-index cu)
   :name (currency-name cu)
   :boostable-key (str (.key (currency-boostable cu)))})

;; ============================================
;; Batch Operations
;; ============================================

(defn all-techs-as-maps
  "Get all techs as Clojure maps."
  []
  (mapv tech->map (all-techs)))

(defn all-trees-as-maps
  "Get all trees as Clojure maps."
  []
  (mapv tree->map (all-trees)))

(defn all-trees-full
  "Get all trees with full tech data."
  []
  (mapv tree->map-full (all-trees)))

(defn all-currencies-as-maps
  "Get all currencies as Clojure maps."
  []
  (mapv currency->map (all-costs)))

;; ============================================
;; Queries
;; ============================================

(defn techs-by-tree
  "Get all techs grouped by tree key."
  []
  (->> (all-techs)
       (filter #(tech-tree %))
       (group-by #(tree-key (tech-tree %)))
       (into (sorted-map))))

(defn techs-with-no-requirements
  "Get all techs that have no prerequisites."
  []
  (filter #(zero? (.size (tech-requirements %))) (all-techs)))

(defn techs-requiring
  "Get all techs that require the given tech."
  [^TECH required-tech]
  (filter (fn [tech]
            (some #(= (requirement-tech %) required-tech)
                  (tech-requirements tech)))
          (all-techs)))

(comment
  ;; === Usage Examples ===
  
  ;; Get all technologies
  (all-techs)
  (tech-count)
  
  ;; Get all trees
  (all-trees)
  (tree-count)
  
  ;; Get first tech info
  (let [tech (first (all-techs))]
    (println "Key:" (tech-key tech))
    (println "Name:" (tech-name tech))
    (println "Level Max:" (tech-level-max tech))
    (println "Cost Total:" (tech-cost-total tech)))
  
  ;; Get first tree info
  (let [tree (first (all-trees))]
    (println "Key:" (tree-key tree))
    (println "Name:" (tree-name tree))
    (println "Tech Count:" (count (tree-techs tree))))
  
  ;; Convert to map
  (tech->map (first (all-techs)))
  (tree->map (first (all-trees)))
  
  ;; Batch convert
  (take 3 (all-techs-as-maps))
  (all-trees-as-maps)
  
  ;; Query by tree
  (keys (techs-by-tree))
  
  ;; Get root techs (no requirements)
  (count (techs-with-no-requirements))
  
  :rcf)

