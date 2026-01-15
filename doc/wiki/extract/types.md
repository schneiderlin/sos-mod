# Types & Enums Extraction

This document describes the extraction of game types and enumerations for the wiki.

## Overview

Types and enums are game constants and classifications that define fundamental game mechanics:

| Type Category | Java Classes | Count | Description |
|---------------|--------------|-------|-------------|
| Terrains | `TERRAINS`, `TERRAIN` | 5 | Geographic terrain types |
| Climates | `CLIMATES`, `CLIMATE` | 3 | Climate zones |
| Diseases | `DISEASES`, `DISEASE` | Varies | Illnesses affecting subjects |
| Traits | `TRAITS`, `TRAIT` | Varies | Character personality traits |
| Needs | `NEEDS`, `NEED` | Varies | Service needs for subjects |
| Humanoid Classes | `HCLASSES`, `HCLASS` | 5 | Social class hierarchy |
| Humanoid Types | `HTYPES`, `HTYPE` | 13 | Detailed subject categories |

## Code Locations

- **Game API**: `src/game/type.clj`
- **Extraction**: `src/extract/type.clj`
- **Java Sources**: `sos-src/init/type/`

## Terrains

Geographic terrain types affect resources, buildings, and gameplay.

### Fixed Terrains

| Key | Name | Description | World Map |
|-----|------|-------------|-----------|
| `OCEAN` | Ocean | Salt water, fish plentiful | Yes |
| `WET` | Fresh Water | Rivers/lakes, clay/fish | Yes |
| `MOUNTAIN` | Mountain | Caverns, minerals | Yes |
| `FOREST` | Forest | Forested areas, lumber | Yes |
| `NONE` | Open Land | Default open terrain | Yes |

### Java Access

```java
// Get all terrains
TERRAINS.ALL()

// Get specific terrain
TERRAINS.OCEAN()
TERRAINS.WET()
TERRAINS.MOUNTAIN()
TERRAINS.FOREST()
TERRAINS.NONE()

// Get terrain map
TERRAINS.MAP()
```

### Clojure Access

```clojure
(require '[game.type :as typ])

;; Get all terrains
(typ/all-terrains)
(typ/terrain-count)

;; Get specific terrain
(typ/terrain-ocean)
(typ/terrain-mountain)

;; Convert to map
(typ/terrain->map (typ/terrain-ocean))
;; => {:key "OCEAN", :name "Ocean", :description "...", ...}

;; Get all as maps
(typ/all-terrains-as-maps)
```

## Climates

Climate zones affect temperature, crop growth, and diseases.

### Fixed Climates

| Key | Name | Seasonal Change | Fertility | Description |
|-----|------|-----------------|-----------|-------------|
| `COLD` | Cold | High | Low | Very cold winters, unique crops, low disease |
| `TEMPERATE` | Temperate | Medium | Medium | Varying temperature |
| `HOT` | Warm | Low | High | Hot summers |

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `seasonChange` | double | Seasonal variation factor (0-1) |
| `tempCold` | double | Cold temperature value |
| `tempWarm` | double | Warm temperature value |
| `fertility` | double | Base fertility multiplier |
| `boosters` | BoostSpecs | Climate-specific bonuses |

### Clojure Access

```clojure
(require '[game.type :as typ])

;; Get all climates
(typ/all-climates)

;; Get specific climate
(typ/climate-cold)
(typ/climate-temperate)
(typ/climate-hot)

;; Access properties
(typ/climate-fertility (typ/climate-hot))
(typ/climate-temp-cold (typ/climate-cold))

;; Convert to map
(typ/climate->map (typ/climate-cold))
```

## Diseases

Diseases affect subject health and can spread through the population.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `infectRate` | double | Spread rate (0-1) |
| `incubationDays` | int | Days before symptoms |
| `fatalityRate` | double | Death rate (0-1) |
| `length` | int | Duration in days |
| `epidemic` | boolean | Can cause epidemics |
| `regular` | boolean | Occurs regularly |

### Disease Types

- **Epidemic**: Can spread rapidly through population
- **Regular**: Occurs at regular intervals based on `regularDays`

### Clojure Access

```clojure
(require '[game.type :as typ])

;; Get all diseases
(typ/all-diseases)

;; Get disease count
(typ/disease-count)

;; Get regular interval
(typ/regular-disease-days)

;; Convert to map
(typ/disease->map (first (typ/all-diseases)))
;; => {:key "...", :infect-rate 0.5, :fatality-rate 0.1, ...}
```

## Traits

Character traits affect subject behavior and abilities.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `key` | String | Unique identifier |
| `info` | INFO | Name, description |
| `rTitle` | CharSequence | Role title |
| `bios` | CharSequence[] | Biography descriptions |
| `boosters` | BoostSpecs | Trait-specific bonuses |
| `disables` | LIST<TRAIT> | Traits disabled by this one |
| `occRaces` | double[] | Occurrence rate per race |

### Clojure Access

```clojure
(require '[game.type :as typ])

;; Get all traits
(typ/all-traits)
(typ/trait-count)

;; Access properties
(typ/trait-name (first (typ/all-traits)))
(typ/trait-disables (first (typ/all-traits)))

;; Convert to map
(typ/trait->map (first (typ/all-traits)))
```

## Needs

Service needs represent what subjects require for happiness.

### Special Need Types

| Key | Name | Type | Description |
|-----|------|------|-------------|
| `_HUNGER` | Hunger | Essential | Food requirement |
| `_THIRST` | Thirst | Essential | Drink requirement |
| `_SHOPPING` | Shopping | Essential | Market access |
| `_SKINNYDIP` | Skinny Dip | Service | Recreation need |
| `_TEMPLE` | Temple | Service | Religious need |
| `_SHRINE` | Shrine | Service | Religious need |

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `key` | String | Unique identifier |
| `nameNeed` | CharSequence | Display name |
| `basic` | boolean | Is essential need |
| `event` | double | Event multiplier |
| `rate` | Boostable | Rate of increase |

### Clojure Access

```clojure
(require '[game.type :as typ])

;; Get all needs
(typ/all-needs)
(typ/all-simple-needs)  ; non-essential

;; Get special types
(typ/need-types)  ; HUNGER, THIRST, etc.

;; Convert to map
(typ/need->map (first (typ/all-needs)))
```

## Humanoid Classes (HCLASSES)

Social class hierarchy for subjects.

### Fixed Classes

| Key | Name | Player | Description |
|-----|------|--------|-------------|
| `NOBLE` | Nobility | Yes | Top social layer, high demands |
| `CITIZEN` | Plebeian | Yes | Bulk workforce |
| `SLAVE` | Slave | Yes | Forced labor, low demands |
| `CHILD` | Child | Yes | Future citizens, need nurseries |
| `OTHER` | Other | No | Non-player characters |

### Clojure Access

```clojure
(require '[game.type :as typ])

;; Get all classes
(typ/all-hclasses)
(typ/all-player-hclasses)  ; player-controllable only

;; Get specific class
(typ/hclass-noble)
(typ/hclass-citizen)
(typ/hclass-slave)
(typ/hclass-child)

;; Convert to map
(typ/hclass->map (typ/hclass-noble))
```

## Humanoid Types (HTYPES)

Detailed categories of subjects with specific behaviors.

### All Types

| Key | Class | Player | Works | Hostile | Description |
|-----|-------|--------|-------|---------|-------------|
| `CITIZEN` | CITIZEN | ✓ | ✓ | - | Regular workers |
| `RETIREE` | CITIZEN | ✓ | - | - | Retired subjects |
| `RECRUIT` | CITIZEN | ✓ | - | - | Training soldiers |
| `STUDENT` | CITIZEN | ✓ | - | - | University students |
| `SOLDIER` | CITIZEN | ✓ | - | - | Battlefield soldiers |
| `PRISONER` | OTHER | - | - | - | Criminals/POWs |
| `TOURIST` | OTHER | - | - | - | Foreign visitors |
| `ENEMY` | OTHER | - | - | ✓ | Hostile invaders |
| `RIOTER` | OTHER | - | - | ✓ | Rioting citizens |
| `DERANGED` | OTHER | - | - | - | Insane subjects |
| `NOBILITY` | NOBLE | ✓ | - | - | Noble subjects |
| `SLAVE` | SLAVE | ✓ | ✓ | - | Enslaved workers |
| `CHILD` | CHILD | ✓ | - | - | Children |

### Clojure Access

```clojure
(require '[game.type :as typ])

;; Get all types
(typ/all-htypes)
(typ/htype-count)

;; Get specific type
(typ/htype-subject)   ; CITIZEN
(typ/htype-prisoner)
(typ/htype-soldier)

;; Check properties
(typ/htype-player? (typ/htype-subject))
(typ/htype-works? (typ/htype-subject))
(typ/htype-hostile? (typ/htype-enemy))

;; Get class
(typ/htype-class (typ/htype-subject))

;; Convert to map
(typ/htype->map (typ/htype-subject))
```

## Extraction Functions

### Extract All Types

```clojure
(require '[extract.type :as type-extract])

;; Full extraction to file
(type-extract/extract-all "output/wiki")

;; Get data structure
(type-extract/build-types-data)

;; Print summary
(type-extract/extract-types-summary)
```

### Individual Category Extraction

```clojure
;; Extract by category
(type-extract/extract-terrains)
(type-extract/extract-climates)
(type-extract/extract-diseases)
(type-extract/extract-traits)
(type-extract/extract-needs)
(type-extract/extract-hclasses)
(type-extract/extract-htypes)
```

### Find Specific Types

```clojure
;; Find by key
(type-extract/find-terrain "OCEAN")
(type-extract/find-climate "COLD")
(type-extract/find-disease "PLAGUE")
(type-extract/find-trait "STRONG")
(type-extract/find-need "_HUNGER")
(type-extract/find-hclass "NOBLE")
(type-extract/find-htype "CITIZEN")
```

### Grouped Queries

```clojure
;; Diseases by type
(type-extract/list-diseases-by-type)
;; => {:epidemic [...], :regular [...], :neither [...]}

;; HTypes by class
(type-extract/list-htypes-by-class)
;; => {"CITIZEN" [...], "NOBLE" [...], ...}

;; HTypes by behavior
(type-extract/list-htypes-by-behavior)
;; => {:player [...], :worker [...], :hostile [...], :other [...]}
```

## Output Schema

### types.edn Structure

```clojure
{:version "1.0"
 :extracted-at "2026-01-15T..."
 :summary {:terrains 5
           :climates 3
           :diseases N
           :traits N
           :needs N
           :hclasses 5
           :htypes 13}
 :terrains {:count 5
            :terrains [{:key "OCEAN" :name "Ocean" ...} ...]}
 :climates {:count 3
            :climates [{:key "COLD" :name "Cold" ...} ...]}
 :diseases {:count N
            :regular-interval-days 30
            :diseases [{:key "..." :fatality-rate 0.1 ...} ...]
            :epidemic-diseases ["PLAGUE" ...]
            :regular-diseases ["FLU" ...]}
 :traits {:count N
          :traits [{:key "..." :disables [...] ...} ...]}
 :needs {:count N
         :needs [{:key "_HUNGER" :basic true ...} ...]
         :basic-needs ["_HUNGER" "_THIRST" ...]}
 :hclasses {:count 5
            :hclasses [{:key "NOBLE" :player true ...} ...]
            :player-classes ["NOBLE" "CITIZEN" ...]}
 :htypes {:count 13
          :htypes [{:key "CITIZEN" :works true ...} ...]
          :player-types ["CITIZEN" "NOBILITY" ...]
          :worker-types ["CITIZEN" "SLAVE"]
          :hostile-types ["ENEMY" "RIOTER"]}}
```

## Related Documentation

- [Resources](resources.md) - Game resources
- [Technologies](technologies.md) - Research tree
- [Buildings](buildings.md) - Room/building types
- [Races](races.md) - Playable races (pending)

---

*Last Updated: 2026-01-15*

