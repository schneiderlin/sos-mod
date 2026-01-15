# Races Extraction

## Overview

Races are the different species that can populate your settlement in Songs of Syx. Each race has unique characteristics including physical properties, population dynamics, preferences, and special boosts.

## Implementation Status

| Component | Location | Status |
|-----------|----------|--------|
| Game API | `src/game/race.clj` | ✅ Done |
| Extraction | `src/extract/race.clj` | ✅ Done |
| Sprite Export | - | ⏳ Pending (see `src/game/sprite.clj`) |

## Java Classes

```
init.race.RACES           - Race registry (static access)
init.race.Race            - Individual race
init.race.RaceInfo        - Name, description, pros/cons (extends INFO)
init.race.Physics         - Physical properties (height, hitbox, age)
init.race.RacePopulation  - Population dynamics (growth, immigration, climate prefs)
init.race.RaceStats       - Statistics and standing definitions
init.race.RacePreferrence - Food, drink, structure, and inter-race preferences
init.race.RaceBoosts      - Global race boosts registry
init.race.bio.Bio         - Biography description lines
init.race.appearence.RAppearence - Visual appearance (sprites, colors)
```

## Clojure API

### Game Functions (`game.race`)

```clojure
(require '[game.race :as race])

;; Get all races
(race/all-races)           ; => LIST<Race>
(race/race-count)          ; => int

;; Get playable races only
(race/playable-races)      ; => LIST<Race>
(race/playable-count)      ; => int

;; Get by key
(race/get-race "HUMAN")    ; => Race or nil

;; Basic properties
(race/race-key r)          ; => "HUMAN"
(race/race-index r)        ; => 0
(race/race-playable? r)    ; => true/false

;; Info properties
(race/race-name r)         ; => "Humans"
(race/race-names r)        ; => "Humans"
(race/race-desc r)         ; => "Short description..."
(race/race-desc-long r)    ; => "Long description..."
(race/race-pros r)         ; => ["Pro 1" "Pro 2" ...]
(race/race-cons r)         ; => ["Con 1" "Con 2" ...]

;; Physics properties
(race/race-height r)       ; => height over ground
(race/race-hitbox-size r)  ; => hitbox pixels
(race/race-adult-at r)     ; => day when adult
(race/race-corpse-decays? r) ; => true/false
(race/race-sleeps? r)      ; => true/false
(race/race-slave-price r)  ; => slave market price

;; Population properties
(race/race-pop-growth r)   ; => growth rate
(race/race-pop-max r)      ; => max population fraction
(race/race-immigration-rate r) ; => per day
(race/race-climate-preferences r) ; => {"TEMPERATE" 1.0 ...}
(race/race-terrain-preferences r) ; => {"FOREST" 1.0 ...}

;; Preferences
(race/race-preferred-foods r)  ; => [ResG ...]
(race/race-preferred-drinks r) ; => [ResGDrink ...]
(race/race-relations r)        ; => {"OTHER_RACE" 0.5 ...}
(race/race-most-hated r)       ; => Race

;; Boosts
(race/race-boost-specs r)      ; => [BoostSpec ...]

;; Data conversion
(race/race->map r)             ; => basic Clojure map
(race/race->map-full r)        ; => full Clojure map with all data
(race/all-races-as-maps)       ; => vector of maps
(race/all-races-full)          ; => vector of full maps
```

### Extraction Functions (`extract.race`)

```clojure
(require '[extract.race :as extract])

;; Quick summary
(extract/extract-races-summary)

;; Reports
(extract/race-comparison-report)   ; Table comparing all races
(extract/race-relations-report)    ; Matrix of race relations
(extract/race-details-report "HUMAN") ; Detailed info for one race

;; Extract to EDN
(extract/extract-races-edn)              ; default: output/wiki/data
(extract/extract-races-edn "my-dir")     ; custom dir
(extract/extract-race-relations-edn)     ; relations matrix

;; Queries
(extract/find-race "HUMAN")
(extract/list-races)
(extract/list-playable)
(extract/list-race-boosts "HUMAN")

;; Full extraction (data + sprites)
(extract/extract-all)
```

## Data Fields

### Race Core

| Field | Method | Type | Description |
|-------|--------|------|-------------|
| key | `.key()` | String | Unique ID (e.g., "HUMAN") |
| index | `.index()` | int | Index in all() list |
| playable | `.playable` | boolean | Can be played |

### RaceInfo

| Field | Method | Type | Description |
|-------|--------|------|-------------|
| name | `.name` (INFO) | CharSequence | Display name |
| names | `.names` (INFO) | CharSequence | Plural name |
| desc | `.desc` (INFO) | CharSequence | Short description |
| desc_long | `.desc_long` | String | Long description |
| namePosessive | `.namePosessive` | String | Possessive form |
| namePosessives | `.namePosessives` | String | Plural possessive |
| initialChallenge | `.initialChallenge` | String | Challenge description |
| pros | `.pros` | String[] | Advantages |
| cons | `.cons` | String[] | Disadvantages |
| armyNames | `.armyNames` | LIST<String> | Army name options |

### Physics

| Field | Method | Type | Description |
|-------|--------|------|-------------|
| height | `.height()` | double | Height over ground |
| hitBoxsize | `.hitBoxsize()` | int | Hitbox size in pixels |
| adultAt | `.adultAt` | int | Day when race becomes adult |
| decays | `.decays` | boolean | Corpse decays |
| sleeps | `.sleeps` | boolean | Race needs sleep |
| slaveprice | `.slaveprice` | int | Slave market price |
| raiding | `.raiding` | double | Raiding mercenary value |

### RacePopulation

| Field | Method | Type | Description |
|-------|--------|------|-------------|
| growth | `.growth` | double | Population growth rate |
| max | `.max` | double | Max population fraction (0-1) |
| immigrantsPerDay | `.immigrantsPerDay` | double | Immigration rate |
| climate(c) | `.climate(CLIMATE)` | double | Climate preference |
| terrain(t) | `.terrain(TERRAIN)` | double | Terrain preference |

### RacePreferrence

| Field | Method | Type | Description |
|-------|--------|------|-------------|
| food | `.food` | LIST<ResG> | Preferred foods |
| drink | `.drink` | LIST<ResGDrink> | Preferred drinks |
| mostHated | `.mostHated` | Race | Most disliked race |
| race(r) | `.race(Race)` | double | Relation with other race (0-1) |

## Output Schema

### races.edn

```edn
{:version "1.0"
 :extracted-at "2026-01-15T..."
 
 :summary
 {:total-races 8
  :playable-races 6
  :non-playable-races 2
  :playability {:playable ["HUMAN" "DONDORIAN" ...]
                :non-playable ["CREATURE1" ...]}}
 
 :races
 [{:key "HUMAN"
   :index 0
   :playable true
   
   :info
   {:name "Humans"
    :names "Humans"
    :description "The most versatile species..."
    :description-long "..."
    :possessive "Human's"
    :possessives "Humans'"
    :challenge "Starting challenge text..."
    :pros ["Versatile" "Quick learners"]
    :cons ["Average at everything"]}
   
   :physics
   {:height 200.0
    :hitbox-size 80
    :adult-at-day 16
    :corpse-decays true
    :sleeps true
    :slave-price 37
    :raiding-value 1.0}
   
   :population
   {:growth 0.001
    :max 1.0
    :immigration-rate 0.1
    :climate-preferences {"TEMPERATE" 1.0 "COLD" 0.5 ...}
    :terrain-preferences {"FOREST" 1.0 "MOUNTAIN" 0.8 ...}}
   
   :preferences
   {:preferred-foods ["BREAD" "MEAT" ...]
    :preferred-drinks ["WINE" "BEER" ...]
    :most-hated-race "CREATURE1"
    :race-relations {"DONDORIAN" 0.8 "CREATURE1" 0.2 ...}}
   
   :boosts
   [{:boostable-key "FARMING"
     :boostable-name "Farming"
     :is-mul false
     :from 0.0
     :to 0.1}
    ;; ...more boosts
   ]
   
   :appearance-types 2
   :icon-path "sprites/races/HUMAN/icon.png"
   :sheet-path "sprites/races/HUMAN/sheet/"
   :lay-path "sprites/races/HUMAN/lay/"}
  
  ;; ...more races
 ]}
```

### race-relations.edn

```edn
{:race-keys ["HUMAN" "DONDORIAN" "TILAPI" ...]
 :matrix [[1.0 0.8 0.6 ...]
          [0.8 1.0 0.7 ...]
          [0.6 0.7 1.0 ...]
          ;; ...
         ]}
```

## Quick Start

```clojure
;; In REPL with game running:

;; 1. Require namespaces
(require '[game.race :as race])
(require '[extract.race :as extract])

;; 2. Check connection
(race/race-count)  ; Should return number > 0

;; 3. Print summary
(extract/extract-races-summary)

;; 4. Compare all races
(extract/race-comparison-report)

;; 5. View race relations
(extract/race-relations-report)

;; 6. Detailed race info
(extract/race-details-report "HUMAN")

;; 7. Extract to file
(extract/extract-races-edn)
;; => Saves to output/wiki/data/races.edn

;; 8. View specific race
(extract/find-race "DONDORIAN")
```

## Progress

- [x] Create `game.race` namespace with accessor functions
- [x] Create `extract.race` namespace for extraction
- [x] Race basic info extraction (key, index, playable)
- [x] RaceInfo extraction (name, desc, pros/cons)
- [x] Physics extraction (height, age, sleep, slave price)
- [x] Population extraction (growth, immigration, climate/terrain prefs)
- [x] Preferences extraction (food, drink, race relations)
- [x] Boosts extraction
- [x] EDN output with summary
- [x] Race relations matrix output
- [ ] Race sprite export (requires full sprite extraction support)
- [ ] JSON output option
- [ ] Integration with sprite export from `game.sprite`

## Notes

1. **Playable vs Non-playable**: Some races may not be playable but can appear as NPCs or raiders
2. **Race relations**: Value 0-1 where 1.0 is positive and 0.0 is hostile
3. **Climate/Terrain preferences**: Affect immigration and population growth in different regions
4. **Boosts**: Race-specific bonuses to various activities (farming, mining, combat, etc.)
5. **Sprites**: Race sprites are in `base/data.zip` under `data/assets/sprite/race/`
6. **Adult age**: The `adultAt` field is in game days
7. Game must be running/loaded to extract data

