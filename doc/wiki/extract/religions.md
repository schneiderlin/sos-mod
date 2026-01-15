# Religions Extraction

## Overview

Religions in Songs of Syx represent different belief systems that affect population happiness, faction relationships, and various gameplay mechanics. Each religion has a deity, color, icon, and defines opposition relationships with other religions.

## Java Classes

| Class | Package | Description |
|-------|---------|-------------|
| `RELIGIONS` | `init.religion` | Static registry of all religions |
| `Religion` | `init.religion` | Individual religion instance |

## Key Properties

| Property | Type | Description |
|----------|------|-------------|
| `key` | String | Unique identifier (e.g., "CRATOR") |
| `index` | int | Position in religion list |
| `info` | INFO | Name and description |
| `diety` | CharSequence | Name of the religion's deity |
| `color` | COLOR | Religion's display color |
| `icon` | Icon | Religion's UI icon |
| `inclination` | double | Default spread rate (0-1) |
| `conversionCity` | Boostable | City conversion boostable |
| `boosts` | BoostSpecs | Stat boosters for followers |

## Clojure API

### Game API: `src/game/religion.clj`

```clojure
(require '[game.religion :as rel])

;; Registry Access
(rel/all-religions)      ; => LIST<Religion>
(rel/religion-count)     ; => int
(rel/get-religion "CRATOR") ; => Religion or nil

;; Religion Properties
(rel/religion-key rel)   ; => "CRATOR"
(rel/religion-name rel)  ; => "Crator's Faith"
(rel/religion-desc rel)  ; => "Believers in..."
(rel/religion-deity rel) ; => "Crator"
(rel/religion-inclination rel) ; => 0.5
(rel/religion-color rel) ; => COLOR object
(rel/religion-icon rel)  ; => Icon object

;; Opposition
(rel/religion-opposition rel1 rel2) ; => 0.0-1.0
(rel/all-oppositions rel) ; => {key -> opposition}
(rel/get-opposition-matrix) ; => nested map

;; Boosts
(rel/religion-boosts-as-maps rel) ; => [{:boostable-key ...}]

;; Data Conversion
(rel/religion->map rel)      ; => {:key ... :name ... :boosts [...]}
(rel/religion->map-basic rel) ; => without boosts

;; Batch Operations
(rel/all-religions-as-maps)
(rel/all-religions-basic)

;; Queries
(rel/religions-by-inclination) ; sorted by spread rate
(rel/find-most-opposed rel)    ; most hostile religion
(rel/find-least-opposed rel)   ; most compatible religion
```

### Extraction: `src/extract/religion.clj`

```clojure
(require '[extract.religion :as rel-extract])

;; Full extraction to EDN
(rel-extract/extract-all "output/wiki")

;; Summary report
(rel-extract/extract-religions-summary)

;; Opposition matrix report
(rel-extract/opposition-report)

;; Individual queries
(rel-extract/find-religion "CRATOR")
(rel-extract/list-deities)
(rel-extract/find-rivals "CRATOR")
```

## Data Structure

### Religion Map

```clojure
{:key "CRATOR"
 :index 0
 :name "Crator's Faith"
 :description "Followers of the great creator..."
 :deity "Crator"
 :inclination 0.5
 :color {:red 0.8 :green 0.2 :blue 0.4 :hex "#CC3366"}
 :oppositions {"CRATOR" 0.0
               "AMINION" 0.75
               "ATHURI" 0.5}
 :boosts [{:boostable-key "HAPPINESS"
           :boostable-name "Happiness"
           :is-mul false
           :from 0.0
           :to 0.1}]
 :icon-path "sprites/religions/CRATOR/icon.png"}
```

### Full Export Structure

```clojure
{:version "1.0"
 :extracted-at "2026-01-15T..."
 :summary {:total-religions 5
           :religions-by-inclination [{:key "CRATOR" :inclination 0.6}
                                      {:key "AMINION" :inclination 0.4}]}
 :religions [{:key "CRATOR" ...} {:key "AMINION" ...}]
 :opposition-matrix {"CRATOR" {"CRATOR" 0.0 "AMINION" 0.75}
                     "AMINION" {"CRATOR" 0.75 "AMINION" 0.0}}}
```

## Key Concepts

### Religion Opposition

Opposition defines how conflicting two religions are:

```clojure
;; Get opposition between two religions
(rel/religion-opposition rel1 rel2) ; => 0.0-1.0

;; Higher values mean more conflict
;; 0.0 = same religion (no conflict)
;; 0.5 = moderate opposition
;; 1.0 = maximum opposition (rare)
```

Opposition affects:
- Population happiness when multiple religions coexist
- Faction diplomacy
- Religious conversion rates

### Religion Inclination

Default spread rate determines how quickly a religion spreads naturally:

```clojure
;; Higher inclination = faster natural spread
(rel/religion-inclination rel) ; => 0.0-1.0
```

### Religion Boosts

Each religion can provide stat boosts to its followers:

```clojure
;; Get boosts for a religion
(rel/religion-boosts-as-maps rel)
;; => [{:boostable-key "HAPPINESS"
;;      :boostable-name "Happiness"
;;      :is-mul false        ; additive boost
;;      :from 0.0            ; min value
;;      :to 0.1}]            ; max value
```

### Temples and Shrines

Religions require buildings for worship:
- **Temples** (`ROOM_TEMPLE`) - Main worship buildings
- **Shrines** (`ROOM_SHRINE`) - Smaller worship sites

See `settlement.stats.colls.StatsReligion` for religion stat tracking.

## Usage Examples

### Extract All Religions

```clojure
(require '[extract.religion :as rel-extract])

;; Generate full data file
(rel-extract/extract-all "output/wiki")
;; Creates: output/wiki/data/religions.edn
```

### Generate Summary Report

```clojure
(rel-extract/extract-religions-summary)
;; Output:
;; === Religions Summary ===
;; Total religions: 5
;; 
;; === Religions by Inclination ===
;;   CRATOR: 0.60
;;   AMINION: 0.40
;; ...
```

### Opposition Matrix Report

```clojure
(rel-extract/opposition-report)
;; Output:
;; === Religion Opposition Report ===
;;             CRATOR   AMINION   ATHURI
;; ----------------------------------------
;; CRATOR         -      0.75      0.50
;; AMINION      0.75       -       0.25
;; ...
```

### Query Specific Religion

```clojure
(rel-extract/find-religion "CRATOR")
;; => {:key "CRATOR" :name "Crator's Faith" ...}
```

### Find Religious Rivals

```clojure
(rel-extract/find-rivals "CRATOR")
;; => {:religion "CRATOR"
;;     :most-opposed {:key "AMINION" :opposition 0.75}
;;     :least-opposed {:key "ATHURI" :opposition 0.25}}
```

## Output Files

| File | Description |
|------|-------------|
| `output/wiki/data/religions.edn` | Full religion data with boosts |
| `output/wiki/data/religion-opposition.edn` | Just opposition matrix |

## Related Documentation

- [Static Config Data](../../src-code/static_config_data.md) - How game loads religion configs
- [Boosters](../../booster/boosters_all.md) - Religion boost effects
- [Buildings Extraction](./buildings.md) - Temple/Shrine buildings
- [Resources Extraction](./resources.md) - Similar extraction pattern

