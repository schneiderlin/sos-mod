# Boosters Data Extraction

This document describes how to extract booster (stat modifier) data from Songs of Syx.

## Overview

Boosters (also called Boostables) are stat modifiers that affect various aspects of the game. They include:
- **Physics** - Physical attributes (speed, health, stamina, etc.)
- **Battle** - Combat stats (offence, defence, morale, etc.)
- **Behaviour** - Behavioral traits (lawfulness, loyalty, happiness, etc.)
- **Civic** - Settlement management (maintenance, spoilage, immigration, etc.)
- **Activity** - Activity rates (mourning, punishment, social, etc.)
- **Noble** - Personality traits (aggression, pride, honour, etc.)
- **Rooms** - Building-specific boosts

## Java Classes

| Class | Description |
|-------|-------------|
| `game.boosting.BOOSTING` | Main registry, access via `BOOSTING.ALL()` |
| `game.boosting.BOOSTABLES` | Category collections (PHYSICS, BATTLE, etc.) |
| `game.boosting.Boostable` | Individual boostable stat |
| `game.boosting.BoostableCat` | Boostable category |
| `game.boosting.BoostSpecs` | Collection of boost specifications |
| `game.boosting.BoostSpec` | Single boost specification (booster + target) |

## Code Locations

| File | Purpose |
|------|---------|
| `src/game/booster.clj` | Game API for accessing boostables |
| `src/extract/booster.clj` | Extraction functions for wiki |

## Usage

### Basic Access

```clojure
(require '[game.booster :as bo])

;; Get all boostables
(bo/all-boostables)
(bo/boostable-count)  ; => 255 (approx)

;; Get all categories
(bo/all-collections)
(bo/collection-count)  ; => 7

;; Get specific category
(bo/physics-category)
(bo/battle-category)
(bo/behaviour-category)
```

### Accessing Specific Boostables

```clojure
;; By category accessor
(bo/physics-speed)
(bo/physics-health)
(bo/battle-morale)
(bo/behaviour-happiness)
(bo/civic-immigration)

;; By key
(bo/get-boostable "PHYSICS_SPEED")
(bo/get-boostable "BATTLE_MORALE")
```

### Getting Boostable Properties

```clojure
(let [speed (bo/physics-speed)]
  (bo/boostable-key speed)        ; => "PHYSICS_SPEED"
  (bo/boostable-name speed)       ; => "Speed"
  (bo/boostable-desc speed)       ; => "The speed of a subject..."
  (bo/boostable-base-value speed) ; => 4.5
  (bo/boostable-min-value speed)) ; => -10000000
```

### Category Properties

```clojure
(let [phys (bo/physics-category)]
  (bo/category-prefix phys)     ; => "PHYSICS_"
  (bo/category-name phys)       ; => "Physics"
  (bo/category-boostables phys) ; => LIST<Boostable>
  (.size (bo/category-boostables phys))) ; => 9
```

### Converting to Clojure Data

```clojure
;; Single boostable
(bo/boostable->map (bo/physics-speed))
; => {:key "PHYSICS_SPEED"
;     :name "Speed"
;     :description "The speed of a subject..."
;     :base-value 4.5
;     :category-name "Physics"
;     ...}

;; All boostables as maps
(bo/all-boostables-as-maps)

;; All categories with boostables
(bo/all-categories-full)
```

## Extraction Functions

```clojure
(require '[extract.booster :as booster-extract])

;; Print summary
(booster-extract/extract-boosters-summary)

;; Category report
(booster-extract/category-report)

;; Extract to EDN file
(booster-extract/extract-boosters-edn)
; => output/wiki/data/boosters.edn

;; Extract by category (separate files)
(booster-extract/extract-by-category)
; => output/wiki/data/boosters/physics.edn
; => output/wiki/data/boosters/battle.edn
; => ...

;; Full extraction
(booster-extract/extract-all "output/wiki")
```

## Data Structure

### Boostable Map

```clojure
{:key "PHYSICS_SPEED"
 :index 3
 :name "Speed"
 :description "The speed of a subject, expressed in tiles per second."
 :base-value 4.5
 :min-value -10000000.0
 :category-prefix "PHYSICS_"
 :category-name "Physics"
 :type-mask 4
 :types #{:settlement}
 :semantic-category :physics
 :icon-path "sprites/boosters/PHYSICS_SPEED/icon.png"}
```

### Category Map

```clojure
{:prefix "PHYSICS_"
 :name "Physics"
 :description ""
 :type-mask 4
 :types #{:settlement}
 :boostable-count 9
 :boostable-keys ["PHYSICS_MASS" "PHYSICS_STAMINA" ...]}
```

### Full Export Structure

```clojure
{:version "1.0"
 :extracted-at "2026-01-15T..."
 :summary {:total-boostables 255
           :total-categories 7
           :by-category {"Physics" 9, "Battle" 25, ...}
           :by-semantic-category {:physics 9, :battle 25, ...}
           :by-type {:settlement 200, :world 50, :other 5}}
 :categories [...]
 :boostables [...]}
```

## Boostable Categories

### Physics (9 boostables)
| Key | Name | Base Value | Description |
|-----|------|------------|-------------|
| `PHYSICS_MASS` | Weight | 80.0 | Subject weight |
| `PHYSICS_STAMINA` | Stamina | 1.0 | Walking/running endurance |
| `PHYSICS_SPEED` | Speed | 4.5 | Tiles per second |
| `PHYSICS_ACCELERATION` | Acceleration | 3.0 | Speed increase rate |
| `PHYSICS_HEALTH` | Health | 1.0 | Disease resistance |
| `PHYSICS_DEATH_AGE` | Lifespan | 100.0 | Maximum age |
| `PHYSICS_RESISTANCE_HOT` | Heat Resistance | 0.5 | Hot temperature tolerance |
| `PHYSICS_RESISTANCE_COLD` | Cold Resistance | 0.5 | Cold temperature tolerance |
| `PHYSICS_SOILING` | Soiling | 0.125 | Rate of becoming dirty |

### Battle (25+ boostables)
| Key | Name | Base Value | Description |
|-----|------|------------|-------------|
| `BATTLE_OFFENCE_SKILL` | Offence | 1 | Attack ability |
| `BATTLE_DEFENCE_SKILL` | Defence | 1 | Block ability |
| `BATTLE_MORALE` | Morale | 4.0 | Combat determination |
| `BATTLE_BLUNT_ATTACK` | Force | 40 | Attack force |
| `BATTLE_BLUNT_DEFENCE` | Force Absorption | 40 | Damage absorption |
| ... | ... | ... | ... |

### Behaviour (5 boostables)
| Key | Name | Base Value | Description |
|-----|------|------------|-------------|
| `BEHAVIOUR_LAWFULNESS` | Lawfulness | 1.0 | Crime reluctance |
| `BEHAVIOUR_SUBMISSION` | Submission | 1.0 | Slave compliance |
| `BEHAVIOUR_LOYALTY` | Loyalty | 1.0 | Riot prevention |
| `BEHAVIOUR_HAPPINESS` | Happiness | 1.0 | Subject happiness |
| `BEHAVIOUR_SANITY` | Sanity | 1.0 | Mental stability |

### Civic (15+ boostables)
| Key | Name | Base Value | Description |
|-----|------|------------|-------------|
| `CIVIC_MAINTENANCE` | Robustness | 1.0 | Building degradation rate |
| `CIVIC_SPOILAGE` | Conservation | 1.0 | Goods decay rate |
| `CIVIC_ACCIDENT` | Safety | 1.0 | Work accident chance |
| `CIVIC_FURNITURE` | Furnishing | 1.0 | Furniture usage rate |
| `CIVIC_IMMIGRATION` | Immigration | 1 | Immigration pool |
| `CIVIC_INNOVATION` | Innovation | 0.0 | Tech research |
| `CIVIC_DIPLOMACY` | Emissary Points | 0.0 | Faction manipulation |
| ... | ... | ... | ... |

### Activity (4 boostables)
| Key | Name | Base Value | Description |
|-----|------|------------|-------------|
| `ACTIVITY_MOURN` | Mourning | 1.0 | Grave visiting frequency |
| `ACTIVITY_PUNISHMENT` | Punishment | 1.0 | Execution viewing desire |
| `ACTIVITY_JUDGE` | Judgement | 1.0 | Court visiting desire |
| `ACTIVITY_SOCIAL` | Social | 1.0 | Socialization desire |

### Noble (6 boostables)
| Key | Name | Base Value | Description |
|-----|------|------------|-------------|
| `NOBLE_AGRRESSION` | Aggression | 1.0 | War affinity |
| `NOBLE_PRIDE` | Pride | 1.0 | Flattery value |
| `NOBLE_HONOUR` | Honour | 1.0 | Agreement value |
| `NOBLE_MERCY` | Mercy | 1.0 | Mercy inclination |
| `NOBLE_COMPETENCE` | Competence | 1.0 | General competence |
| `NOBLE_TOLERANCE` | Tolerance | 1.0 | New things tolerance |

### Room Boostables (150+ boostables)
Room-specific production and efficiency modifiers. Examples:
- `ROOM_FARM_GRAIN` - Grain farm production
- `ROOM_MINE_STONE` - Stone mine production
- `ROOM_REFINER_BAKERY` - Bakery efficiency
- `ROOM_WORKSHOP_SMITHY` - Smithy production

## Type Masks

Boostables are tagged with type masks indicating where they apply:

| Constant | Value | Description |
|----------|-------|-------------|
| `TYPE_SETT` | 4 | Settlement/city level |
| `TYPE_WORLD` | 2 | World map level |
| `TYPE_CRAP` | 1 | Miscellaneous/unused |

## See Also

- [Resources Extraction](resources.md)
- [Technologies Extraction](technologies.md)
- [Buildings Extraction](buildings.md)
- [Existing Booster List](../../booster/boosters_all.md)

