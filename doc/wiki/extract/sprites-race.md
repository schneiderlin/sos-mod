# Race Sprites Extraction

## Overview

Race sprites are character visuals for each playable/non-playable species. Each race has:
- **Sheet sprites**: Standing/walking animations (18 action types)
- **Lay sprites**: Lying down positions (12 positions)
- **Normal maps**: For dynamic lighting effects

## Source Files

Location: `base/data.zip` → `data/assets/sprite/race/{RaceName}.png`

### PNG Layout (448×546 pixels)

```
┌─────────────────────┬─────────────────────┐
│     LEFT HALF       │     RIGHT HALF      │
│   (224px wide)      │    (224px wide)     │
│                     │                     │
│   Body/Color        │   Normal Maps       │
│   Sprites           │   (for lighting)    │
│                     │                     │
│  ┌───────┬────────┐ │ ┌───────┬────────┐  │
│  │ Sheet │  Lay   │ │ │ Sheet │  Lay   │  │
│  │ 66px  │ 158px  │ │ │ 66px  │ 158px  │  │
│  │       │        │ │ │       │        │  │
│  │18 rows│ 3 rows │ │ │18 rows│ 3 rows │  │
│  │@ 24px │ 4 cols │ │ │@ 24px │ 4 cols │  │
│  │       │ @ 32px │ │ │       │ @ 32px │  │
│  └───────┴────────┘ │ └───────┴────────┘  │
└─────────────────────┴─────────────────────┘
```

## Java Classes

```
init.race.RACES                    - Race registry
init.race.Race                     - Individual race
init.race.appearence.RAppearence   - Appearance config
init.race.appearence.RType         - Adult/child type
init.race.appearence.RaceSheet     - Sprite sheet reference
```

## Sheet Sprite Actions (18 types)

| Index | Action Key | Description |
|-------|-----------|-------------|
| 0 | `feet-none` | Standing still (feet) |
| 1 | `feet-right` | Right foot forward |
| 2 | `feet-right2` | Right foot forward (alt) |
| 3 | `feet-left` | Left foot forward |
| 4 | `feet-left2` | Left foot forward (alt) |
| 5 | `tunic` | Tunic/clothing layer |
| 6 | `torso-still` | Torso standing |
| 7 | `torso-right` | Torso right animation |
| 8 | `torso-right2` | Torso right (alt 1) |
| 9 | `torso-right3` | Torso right (alt 2) |
| 10 | `torso-left` | Torso left animation |
| 11 | `torso-left2` | Torso left (alt 1) |
| 12 | `torso-left3` | Torso left (alt 2) |
| 13 | `torso-carry` | Carrying object |
| 14 | `torso-out` | Arms out |
| 15 | `torso-out2` | Arms out (alt) |
| 16 | `head` | Head sprite |
| 17 | `shadow` | Character shadow |

## Lay Sprite Indices (24 total)

| Index Range | Half | Description |
|-------------|------|-------------|
| 0-11 | Left | Body sprites (3 rows × 4 cols) |
| 12-23 | Right | Normal maps (3 rows × 4 cols) |

Grid position: `row = index / 4`, `col = index % 4`

## Extraction Code

### Current Implementation

Located in `src/game/sprite.clj`:

```clojure
;; Export sheet sprite (standing/walking)
(export-race-sprite :sheet "Human" :head "output/head.png")
(export-race-sprite :sheet "Human" :head "output/head_normal.png" :normal true)

;; Export lay sprite (lying down)
(export-race-sprite :lay "Human" 0 "output/lay_0.png")
(export-race-sprite :lay "Human" 12 "output/lay_12_normal.png")  ; Normal map

;; With scaling
(export-race-sprite :sheet "Human" :head "output/head_2x.png" :scale 2)
```

### Key Functions

```clojure
(ns game.sprite)

;; Get race object
(get-race "Human")  ; => Race instance

;; Get sprite sheet
(get-race-sheet :sheet "Human")  ; => Standing sprites
(get-race-sheet :lay "Human")    ; => Laying sprites

;; Export sprite
(export-race-sprite sheet-type race-key action-or-index output-path
                    :adult true :scale 1 :normal false)

;; Low-level crop
(crop-from-png zip-entry-path x y width height output-path :scale 1)
```

## Output Schema

### Directory Structure
```
output/sprites/races/
├── Human/
│   ├── sheet/
│   │   ├── feet-none.png
│   │   ├── feet-none_normal.png
│   │   ├── head.png
│   │   ├── head_normal.png
│   │   └── ...
│   └── lay/
│       ├── 0.png
│       ├── 0_normal.png
│       └── ...
├── Dondorian/
│   └── ...
└── ...
```

### Metadata (per race)
```edn
{:race "Human"
 :sheet-sprites
 [{:action :head
   :path "sprites/races/Human/sheet/head.png"
   :normal-path "sprites/races/Human/sheet/head_normal.png"
   :size 24}
  ;; ... 18 actions
  ]
 :lay-sprites
 [{:index 0
   :path "sprites/races/Human/lay/0.png"
   :normal-path "sprites/races/Human/lay/0_normal.png"
   :size 32}
  ;; ... 12 positions
  ]}
```

## Batch Export Functions

Located in `src/extract/race.clj`:

```clojure
;; Export all sprites for a single race
(export-single-race-sprites "HUMAN" "output/wiki/sprites/races")

;; Export sheet sprites only (18 actions × 2)
(export-race-sheet-sprites "HUMAN" "output/wiki/sprites/races")

;; Export lay sprites only (12 positions × 2)
(export-race-lay-sprites "HUMAN" "output/wiki/sprites/races")

;; Export all race sprites (all 8 races)
(extract-race-sprites "output/wiki")

;; Full extraction (data + sprites)
(extract-all "output/wiki")
```

### Race Key to Sprite Name Mapping

Game race keys (e.g., "HUMAN", "Q_AMEVIA") map to sprite file names:

| Race Key | Sprite File |
|----------|-------------|
| ARGONOSH | Argonosh.png |
| CANTOR | Cantor.png |
| CRETONIAN | Cretonian.png |
| DONDORIAN | Dondorian.png |
| GARTHIMI | Garthimi.png |
| HUMAN | Human.png |
| Q_AMEVIA | Amevia.png |
| TILAPI | Tilapi.png |

## Progress

- [x] Understand PNG layout
- [x] Basic export function (`export-race-sprite`)
- [x] Sheet sprite extraction
- [x] Lay sprite extraction  
- [x] Normal map extraction
- [x] Race key to sprite name mapping
- [x] Batch export all races
- [ ] Portrait sprites
- [ ] Child sprites (smaller size)
- [ ] Crown/addon sprites

## Notes

1. **Game rotates sprites**: Only one direction stored, game mirrors/rotates for 8 directions
2. **Normal maps**: Used for lighting effects, same layout as body sprites
3. **Padding**: 6px padding between sprites in the PNG
4. **Child sprites**: Smaller size, accessed via `(.child appearance)` instead of `(.adult)`

