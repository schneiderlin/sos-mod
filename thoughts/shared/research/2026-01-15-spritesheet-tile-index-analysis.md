---
date: 2026-01-15T10:09:02Z
researcher: zihao
git_commit: 9a8ddbdd3afa7a40fbc6f7b257776df7a56ca8cd
branch: master
repository: sos-mod
topic: "Race PNG spritesheet layout and how to export sprites"
tags: [research, spritesheet, race-sprite, PNG-layout, normal-map]
status: complete
last_updated: 2026-01-15
last_updated_by: claude
last_updated_note: Corrected PNG layout understanding - left/right split, 24 lay tiles
---

# Research: Race PNG Spritesheet Layout

**Date**: 2026-01-15
**Researcher**: zihao
**Repository**: sos-mod

## Summary

Race sprite PNGs (e.g., `Human.png`) are **448×546 pixels** and split **vertically** into two halves:
- **Left half (224px)**: Body/color sprites
- **Right half (224px)**: Normal maps (for lighting effects)

Each half contains:
- **Sheet section**: 18 rows × 2 cols of 24×24 sprites (standing/walking)
- **Lay section**: 3 rows × 4 cols of 32×32 sprites (lying down)

**Key insight**: The PNG stores **ONE sprite per action type**. The game generates 8 directions by rotating/mirroring at runtime.

## PNG Layout Diagram

```
┌───────────────────────────────────────────────────────────────────────────────┐
│  PNG SOURCE: base/data.zip/data/assets/sprite/race/Human.png (448 × 546)      │
├───────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌─────────────────────────┬─────────────────────────┐                        │
│  │      LEFT HALF          │      RIGHT HALF         │                        │
│  │     (0-223 px)          │     (224-447 px)        │                        │
│  │                         │                         │                        │
│  │   Body/Color Sprites    │    Normal Maps          │                        │
│  │                         │   (for lighting)        │                        │
│  ├─────────────────────────┼─────────────────────────┤                        │
│  │                         │                         │                        │
│  │  SHEET (24×24 each)     │  SHEET (24×24 each)     │                        │
│  │  18 rows × 2 cols       │  18 rows × 2 cols       │                        │
│  │  x: 0-65                │  x: 224-289             │                        │
│  │                         │                         │                        │
│  │  Row 0: feet-none       │  Row 0: feet-none       │                        │
│  │  Row 1: feet-right      │  Row 1: feet-right      │                        │
│  │  ...                    │  ...                    │                        │
│  │  Row 16: head           │  Row 16: head           │                        │
│  │  Row 17: shadow         │  Row 17: shadow         │                        │
│  │                         │                         │                        │
│  ├─────────────────────────┼─────────────────────────┤                        │
│  │                         │                         │                        │
│  │  LAY (32×32 each)       │  LAY (32×32 each)       │                        │
│  │  3 rows × 4 cols = 12   │  3 rows × 4 cols = 12   │                        │
│  │  x: 66-223              │  x: 290-447             │                        │
│  │                         │                         │                        │
│  │  indices 0-11           │  indices 12-23          │                        │
│  │                         │                         │                        │
│  └─────────────────────────┴─────────────────────────┘                        │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘
```

## Sheet Section Details

**Location**: Left side of each half  
**Size**: 24×24 pixels per sprite  
**Padding**: 6 pixels between sprites  
**Grid**: 18 rows × 2 columns (body + shadow per row)

### Action Types (18 rows)

| Row | Action | Description |
|-----|--------|-------------|
| 0 | feet-none | Standing feet |
| 1 | feet-right | Walking right foot |
| 2 | feet-right2 | Walking right foot frame 2 |
| 3 | feet-left | Walking left foot |
| 4 | feet-left2 | Walking left foot frame 2 |
| 5 | tunic | Body clothing |
| 6 | torso-still | Standing torso |
| 7 | torso-right | Walking torso (right arm) |
| 8 | torso-right2 | Walking torso frame 2 |
| 9 | torso-right3 | Walking torso frame 3 |
| 10 | torso-left | Walking torso (left arm) |
| 11 | torso-left2 | Walking torso frame 2 |
| 12 | torso-left3 | Walking torso frame 3 |
| 13 | torso-carry | Carrying pose |
| 14 | torso-out | Arms out |
| 15 | torso-out2 | Arms out frame 2 |
| 16 | head | Head |
| 17 | shadow | Ground shadow |

### Pixel Calculation for Sheet

```
sprite_size = 24
padding = 6
grid_spacing = 30  (24 + 6)

For body sprite at row N:
  x = 6  (first column, body)
  y = 6 + N * 30

For normal map at row N:
  x = 224 + 6 = 230
  y = 6 + N * 30
```

## Lay Section Details

**Location**: Right of sheet section in each half  
**Size**: 32×32 pixels per sprite  
**Padding**: 6 pixels between sprites  
**Grid**: 3 rows × 4 columns = 12 tiles per half

### Index Layout

```
Body sprites (indices 0-11):
┌─────┬─────┬─────┬─────┐
│  0  │  1  │  2  │  3  │  Row 0
├─────┼─────┼─────┼─────┤
│  4  │  5  │  6  │  7  │  Row 1
├─────┼─────┼─────┼─────┤
│  8  │  9  │ 10  │ 11  │  Row 2
└─────┴─────┴─────┴─────┘

Normal maps (indices 12-23):
┌─────┬─────┬─────┬─────┐
│ 12  │ 13  │ 14  │ 15  │  Row 0
├─────┼─────┼─────┼─────┤
│ 16  │ 17  │ 18  │ 19  │  Row 1
├─────┼─────┼─────┼─────┤
│ 20  │ 21  │ 22  │ 23  │  Row 2
└─────┴─────┴─────┴─────┘
```

### Pixel Calculation for Lay

```
sprite_size = 32
padding = 6
grid_spacing = 38  (32 + 6)
sheet_section_width = 66

For body sprite at index I (0-11):
  row = I / 4
  col = I % 4
  x = 66 + 6 + col * 38
  y = 6 + row * 38

For normal map at index I (12-23):
  actual_index = I - 12
  row = actual_index / 4
  col = actual_index % 4
  x = 224 + 66 + 6 + col * 38
  y = 6 + row * 38
```

## Normal Maps Explained

Normal maps are special textures that encode surface direction using RGB colors:
- **Red channel**: How much the surface faces left/right
- **Green channel**: How much it faces up/down
- **Blue channel**: How much it faces toward the viewer

They appear **purple/blue** in the PNG because flat surfaces are mostly blue (facing toward viewer).

The game uses normal maps for **dynamic lighting** - when light sources move, the sprites appear to have depth and react to light realistically.

**For most modding purposes, you only need the body sprites (left half). Normal maps are optional unless you're implementing custom lighting.**

## Clojure API Usage

```clojure
;; === SHEET sprites (standing/walking) ===
;; Export body sprite
(export-race-sprite :sheet "Human" :head "output/head.png")

;; Export normal map
(export-race-sprite :sheet "Human" :head "output/head_normal.png" :normal true)

;; === LAY sprites (lying down) ===
;; Body sprites (indices 0-11)
(export-race-sprite :lay "Human" 0 "output/lay_0.png")
(export-race-sprite :lay "Human" 5 "output/lay_5.png")

;; Normal maps (indices 12-23)
(export-race-sprite :lay "Human" 12 "output/lay_12_normal.png")
```

## Game's Internal Tile Indexing (Advanced)

The game's Java code uses a different indexing system **after processing** the PNG:

```java
// From HSpriteConst.java
final static int NR = 8;  // 8 directions
final static int IHEAD = 16 * NR;  // = 128
```

The game generates **144 tiles** (18 actions × 8 directions) by rotating each source sprite.

**This is NOT needed for PNG export** - we read directly from the source PNG which stores only one sprite per action type.

## Code References

### Clojure Implementation
- `src/game/sprite.clj` - `export-race-sprite` and `export-sprite-from-png` functions

### Java Source (for reference)
- `sos-src/settlement/entity/humanoid/spirte/HSpriteConst.java` - Tile index constants
- `sos-src/init/race/appearence/RaceSheet.java` - Spritesheet initialization
- `sos-src/util/spritecomposer/ComposerSources.java` - PNG cropping logic

## Open Questions

1. What are the 12 lay sprites used for? Different sleeping/death poses?
2. Are there other PNG sections we haven't documented?
