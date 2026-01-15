# Technologies Extraction

## Overview

Technologies in Songs of Syx represent the research tree that unlocks various boosts and rooms. Each technology belongs to a tech tree and may have prerequisites (other techs that must be researched first).

## Java Classes

| Class | Package | Description |
|-------|---------|-------------|
| `TECHS` | `init.tech` | Static registry of all technologies |
| `TECH` | `init.tech` | Individual technology instance |
| `TechTree` | `init.tech` | Grouping of related technologies |
| `TechCost` | `init.tech` | Cost entry (currency + amount) |
| `TechCurrency` | `init.tech` | Currency type (tied to a Boostable) |

## Clojure API

### Game API: `src/game/tech.clj`

```clojure
(require '[game.tech :as tech])

;; Registry Access
(tech/all-techs)        ; => LIST<TECH>
(tech/tech-count)       ; => int
(tech/all-trees)        ; => LIST<TechTree>
(tech/tree-count)       ; => int
(tech/all-costs)        ; => LIST<TechCurrency>

;; Technology Properties
(tech/tech-key t)       ; => "CIVIL_SLAVE"
(tech/tech-name t)      ; => "Slavery"
(tech/tech-desc t)      ; => "Enable slave management..."
(tech/tech-level-max t) ; => 3
(tech/tech-cost-total t); => 1000
(tech/tech-requirements t) ; => LIST<TechRequirement>

;; Tree Properties
(tech/tree-key tree)    ; => "CIVIL"
(tech/tree-name tree)   ; => "Civil"
(tech/tree-category tree) ; => 0
(tech/tree-techs tree)  ; => seq of TECH

;; Data Conversion
(tech/tech->map t)      ; => {:key ... :name ... :costs [...]}
(tech/tree->map tree)   ; => {:key ... :name ... :tech-keys [...]}
(tech/tree->map-full tree) ; => includes full tech data

;; Batch Operations
(tech/all-techs-as-maps)
(tech/all-trees-as-maps)
(tech/all-trees-full)
(tech/all-currencies-as-maps)

;; Queries
(tech/techs-by-tree)    ; => {tree-key [techs...]}
(tech/techs-with-no-requirements) ; => root techs
(tech/techs-requiring t) ; => techs that need t
```

### Extraction: `src/extract/tech.clj`

```clojure
(require '[extract.tech :as tech-extract])

;; Full extraction to EDN
(tech-extract/extract-all "output/wiki")

;; Summary report
(tech-extract/extract-technologies-summary)

;; Dependency report
(tech-extract/tech-dependency-report)

;; Individual queries
(tech-extract/find-tech "CIVIL_SLAVE")
(tech-extract/find-tree "CIVIL")
(tech-extract/list-currencies)
```

## Data Structure

### Technology Map

```clojure
{:key "CIVIL_SLAVE"
 :index 5
 :name "Slavery"
 :description "Enable slave management..."
 :level-max 3
 :cost-total 1000
 :level-cost-inc 0.5
 :ai-amount 1.0
 :tree-key "CIVIL"
 :color {:red 0.8 :green 0.2 :blue 0.1 :hex "#CC3319"}
 :costs [{:currency-name "Knowledge"
          :currency-index 0
          :amount 500}]
 :requirements [{:tech-key "CIVIL_BASIC" :level 1}]
 :requirement-nodes [{:tech-key "CIVIL_BASIC" :level 1}]
 :icon-path "sprites/techs/CIVIL_SLAVE/icon.png"}
```

### Tech Tree Map

```clojure
{:key "CIVIL"
 :name "Civil"
 :category 0
 :color {:red 0.5 :green 0.5 :blue 1.0 :hex "#8080FF"}
 :rows 4
 :node-grid [["BASIC" nil "SLAVE"]
             ["ADMIN" "LAW" nil]
             ...]
 :techs [{...} {...}]}
```

### Full Export Structure

```clojure
{:version "1.0"
 :extracted-at "2026-01-15T..."
 :summary {:total-techs 42
           :total-trees 6
           :total-currencies 2
           :root-techs 6
           :trees-by-category {0 3, 1 2, 2 1}}
 :currencies [{:index 0 :name "Knowledge" :boostable-key "KNOWLEDGE"}]
 :trees [{:key "CIVIL" :name "Civil" :techs [...]}]
 :techs [{:key "CIVIL_SLAVE" ...}]}
```

## Key Concepts

### Tech Requirements

Technologies can require other technologies at specific levels:

```clojure
;; Get direct requirements
(tech/tech-requirements-nodes t) ; Immediate prerequisites

;; Get all requirements (transitive)
(tech/tech-requirements t) ; All techs needed
```

### Tech Costs

Costs are tied to "currencies" which are actually Boostables:

```clojure
;; A tech cost looks like:
{:currency-name "Knowledge"   ; Display name
 :currency-index 0            ; Index in currency list
 :amount 500}                 ; Cost amount

;; Level cost increase
;; Each subsequent level costs more:
;; cost = base-cost + (level * level-cost-inc)
```

### Tech Tree Grid

Trees are organized in a 2D grid:

```
     Col 0    Col 1    Col 2    ...
Row 0 [BASIC]  [____]   [SLAVE]
Row 1 [ADMIN]  [LAW]    [____]
Row 2 [...]
```

- Maximum 10 columns per row (`TechTree/MAX_COLS`)
- `"_____"` in config represents empty slots
- `nil` in exported data represents empty slots

## Usage Examples

### Extract All Technologies

```clojure
(require '[extract.tech :as tech-extract])

;; Generate full data file
(tech-extract/extract-all "output/wiki")
;; Creates: output/wiki/data/technologies.edn
```

### Generate Summary Report

```clojure
(tech-extract/extract-technologies-summary)
;; Output:
;; === Technologies Summary ===
;; Total technologies: 42
;; Total trees: 6
;; ...
```

### Query Specific Tech

```clojure
(tech-extract/find-tech "CIVIL_SLAVE")
;; => {:key "CIVIL_SLAVE" :name "Slavery" ...}
```

### List Root Technologies

```clojure
(tech-extract/list-root-techs)
;; => [{:key "CIVIL_BASIC" ...} {:key "MILITARY_BASIC" ...}]
```

## Output Files

| File | Description |
|------|-------------|
| `output/wiki/data/technologies.edn` | Full technology data |
| `output/wiki/data/tech-trees.edn` | Just tree structure |

## Related Documentation

- [Static Config Data](../../src-code/static_config_data.md) - How game loads tech configs
- [Boosters](../../booster/boosters_all.md) - Tech boosters and effects
- [Resources Extraction](./resources.md) - Similar extraction pattern

