---
date: 2026-01-15T10:09:02Z
researcher: zihao
git_commit: 9a8ddbdd3afa7a40fbc6f7b257776df7a56ca8cd
branch: master
repository: sos-mod
topic: "How the game uses PNG spritesheets and the get-tile-index function bug"
tags: [research, spritesheet, tile-index, game-sprite, HSpriteConst, RaceSheet]
status: complete
last_updated: 2026-01-15
last_updated_by: zihao
last_updated_note: Added PNG file layout diagram in markdown
---

# Research: Spritesheet Tile Index Calculation Bug

**Date**: 2026-01-15T10:09:02Z
**Researcher**: zihao
**Git Commit**: 9a8ddbdd3afa7a40fbc6f7b257776df7a56ca8cd
**Branch**: master
**Repository**: sos-mod

## Research Question

How does the game use PNG spritesheets in `base/data.zip/data/assets/sprite/`? Specifically, how does the `get-tile-index` function work in `game.sprite`? The user suspects the current Clojure code is wrong and wants to understand how the Java code crops tiles.

## Summary

The research identified **critical bugs** in the Clojure implementation of `get-tile-index` at `src/game/sprite.clj:357-388`. The Java code uses `NR = 8` (8 directions) and calculates tile indices as `base + direction`, while the Clojure code incorrectly uses `NR = 1` and incorrectly applies `(mod direction NR)`.

## Detailed Findings

### Java Implementation (Correct)

**File**: `sos-src/settlement/entity/humanoid/spirte/HSpriteConst.java`

```java
final static int NR = 8;  // 8 directions!

final static int IFEET_NONE = i++ * NR;    // i=0, result = 0
final static int IFEET_RIGHT = i++ * NR;   // i=1, result = 8
final static int IFEET_RIGHT2 = i++ * NR;  // i=2, result = 16
final static int IFEET_LEFT = i++ * NR;    // i=3, result = 24
final static int IFEET_LEFT2 = i++ * NR;   // i=4, result = 32
final static int ITUNIC = i++ * NR;        // i=5, result = 40
final static int ITORSO_STILL = i++ * NR;  // i=6, result = 48
final static int ITORSO_RIGHT = i++ * NR;  // i=7, result = 56
final static int ITORSO_RIGHT2 = i++ * NR; // i=8, result = 64
final static int ITORSO_RIGHT3 = i++ * NR; // i=9, result = 72
final static int ITORSO_LEFT = i++ * NR;   // i=10, result = 80
final static int ITORSO_LEFT2 = i++ * NR;  // i=11, result = 88
final static int ITORSO_LEFT3 = i++ * NR;  // i=12, result = 96
final static int ITORSO_CARRY = i++ * NR;  // i=13, result = 104
final static int ITORSO_OUT = i++ * NR;    // i=14, result = 112
final static int ITORSO_OUT2 = i++ * NR;   // i=15, result = 120
final static int IHEAD = i++ * NR;         // i=16, result = 128
final static int ISHADOW = i++ * NR;       // i=17, result = 136
```

**Usage in HSprite.java** (line 211):
```java
sp.render(r, dir + IHEAD, x, y);  // dir is 0-7, so tiles 128-135 for head
```

### Spritesheet Layout (from RaceSheet.java)

**File**: `sos-src/init/race/appearence/RaceSheet.java`

```java
sheet = new ITileSheet(path, 448, 546) {
    @Override
    protected TILE_SHEET init(ComposerUtil c, ComposerSources s, ComposerDests d) {
        int a = 18;  // 18 action types
        s.singles.init(0, 0, 1, 1, 2, a, d.s24);  // tilesX=2, tilesY=18
        for (int i = 0; i < a; i++) {
            s.singles.setSkip(i * 2, 2).paste(3, true);  // Skip shadows, paste body sprites
        }
        return d.s24.saveGame();
    }
}.get();
```

The spritesheet is 448x546 pixels with:
- **18 rows** (one per action type: feet-none, feet-right, ..., head, shadow)
- **2 columns per row** (column 0 = body, column 1 = shadow)
- The `setSkip(i * 2, 2)` skips shadow sprites, only processing body sprites
- The `paste(3, true)` creates 8 directions by rotating each sprite 4 times (0°, 90°, 180°, 270°)

**Total tiles in output sheet**: 18 actions × 8 directions = 144 tiles (0-143)

### Source PNG File Layout (448 × 546 pixels)

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ PNG SOURCE FILE: base/data.zip/data/assets/sprite/race/Human.png (448 × 546)        │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌─ SHEET SECTION (standing sprites) ─────────────────────────────────────────┐     │
│  │   x: 0-447, y: 0-545                                                       │     │
│  │   Each sprite: 24×24 pixels                                                │     │
│  │   Padding (m): 6 pixels                                                    │     │
│  │   Grid spacing: 24+6 = 30 pixels                                           │     │
│  │                                                                            │     │
│  │   Col 0 (body)      Col 1 (shadow)                                         │     │
│  │   ↓                 ↓                                                      │     │
│  │   ┌────┐┌────┐  Row 0: feet-none (i=0)                                     │     │
│  │   │ 🦶 ││ 🌑 │     setSkip(0, 2) → paste body only                        │     │
│  │   └────┘└────┘     Output: tiles 0-7 (8 directions)                        │     │
│  │   ┌────┐┌────┐  Row 1: feet-right (i=1)                                    │     │
│  │   │ 🦶 ││ 🌑 │     setSkip(2, 2) → paste body only                        │     │
│  │   └────┘└────┘     Output: tiles 8-15                                      │     │
│  │   ┌────┐┌────┐  Row 2: feet-right2 (i=2)                                   │     │
│  │   │ 🦶 ││ 🌑 │     setSkip(4, 2) → paste body only                        │     │
│  │   └────┘└────┘     Output: tiles 16-23                                     │     │
│  │      ...                                                                   │     │
│  │   ┌────┐┌────┐  Row 5: tunic (i=5)                                         │     │
│  │   │ 👕 ││ 🌑 │     setSkip(10, 2) → paste body only                       │     │
│  │   └────┘└────┘     Output: tiles 40-47                                     │     │
│  │      ...                                                                   │     │
│  │   ┌────┐┌────┐  Row 16: head (i=16)                                        │     │
│  │   │ 😐 ││ 🌑 │     setSkip(32, 2) → paste body only                       │     │
│  │   └────┘└────┘     Output: tiles 128-135                                   │     │
│  │   ┌────┐┌────┐  Row 17: shadow (i=17)                                      │     │
│  │   │ 🌑 ││ 🌑 │     setSkip(34, 2) → paste body only                       │     │
│  │   └────┘└────┘     Output: tiles 136-143                                   │     │
│  │                                                                            │     │
│  │   Width calculation: m + tilesX * (size + m) = 6 + 2 * (24 + 6) = 66       │     │
│  │   But source is 448px wide (includes multiple var sections)                │     │
│  └────────────────────────────────────────────────────────────────────────────┘     │
│                                                                                     │
│  ┌─ LAY SECTION (lying sprites) ──────────────────────────────────────────────┐     │
│  │   x: 66-447, y: 0-113                                                       │     │
│  │   Each sprite: 32×32 pixels                                                 │     │
│  │   Padding (m): 6 pixels                                                     │     │
│  │   Grid spacing: 32+6 = 38 pixels                                           │     │
│  │                                                                              │     │
│  │   Starts at sheet.body().x2() = 66 (right edge of sheet body)               │     │
│  │                                                                              │     │
│  │   Col 0   Col 1   Col 2   Col 3                                             │     │
│  │   (body) (shadow)(body) (shadow)                                            │     │
│  │    ↓       ↓       ↓       ↓                                               │     │
│  │   ┌────┐ ┌────┐ ┌────┐ ┌────┐                                              │     │
│  │   │ 😴 │ │ 🌑 │ │ 😴 │ │ 🌑 │  Row 0                                        │     │
│  │   └────┘ └────┘ └────┘ └────┘  setSkip skips shadow sprites                 │     │
│  │   ┌────┐ ┌────┐ ┌────┐ ┌────┐                                              │     │
│  │   │ 😴 │ │ 🌑 │ │ 😴 │ │ 🌑 │  Row 1                                        │     │
│  │   └────┘ └────┘ └────┘ └────┘                                              │     │
│  │   ┌────┐ ┌────┐ ┌────┐ ┌────┐                                              │     │
│  │   │ 😴 │ │ 🌑 │ │ 😴 │ │ 🌑 │  Row 2                                        │     │
│  │   └────┘ └────┘ └────┘ └────┘                                              │     │
│  │                                                                              │     │
│  │   Output: 6 sprites total (tiles 0-5 in lay sheet)                          │     │
│  └──────────────────────────────────────────────────────────────────────────────┘     │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### Pixel Position Calculation

From `ComposerSources.java:756-761`:

```java
private void calc() {
    int tx = tileCurrent % tilesX;  // Column in source (0 or 1 for sheet)
    int ty = tileCurrent / tilesX;  // Row in source (0-17 for sheet)
    pixelX = body.getStartX() + tx * (size + m);  // m = 6 pixels padding
    pixelY = body.getStartY() + ty * (size + m);
}
```

**For SHEET (24×24 sprites, m=6, spacing=30):**
- `body.getStartX() = 6` (padding m)
- `body.getStartY() = 6` (padding m)
- Tile at (tx=0, ty=16): `pixelX = 6 + 0×30 = 6`, `pixelY = 6 + 16×30 = 486`
- Head sprite is at (6, 486) in source PNG

**For LAY (32×32 sprites, m=6, spacing=38):**
- `lay-start-x = 66` (sheet.body().x2())
- Tile at (tx=0, ty=0): `pixelX = 66 + 6 + 0×38 = 72`, `pixelY = 6 + 0×38 = 6`

### Output TILE_SHEET Layout (After Processing)

The `paste(3, true)` call creates 8 directions by rotating:

```
┌─────────────────────────────────────────────────────────────────────┐
│ OUTPUT TILE_SHEET (processed, used in-game)                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Action          │ Base  │ Directions (0-7)                        │
│  ────────────────┼───────┼────────────────────────────────────────│
│  feet-none       │ 0     │ 0,1,2,3,4,5,6,7                         │
│  feet-right      │ 8     │ 8,9,10,11,12,13,14,15                   │
│  feet-right2     │ 16    │ 16,17,18,19,20,21,22,23                 │
│  feet-left       │ 24    │ 24-31                                   │
│  feet-left2      │ 32    │ 32-39                                   │
│  tunic           │ 40    │ 40-47                                   │
│  torso-still     │ 48    │ 48-55                                   │
│  torso-right     │ 56    │ 56-63                                   │
│  torso-right2    │ 64    │ 64-71                                   │
│  torso-right3    │ 72    │ 72-79                                   │
│  torso-left      │ 80    │ 80-87                                   │
│  torso-left2     │ 88    │ 88-95                                   │
│  torso-left3     │ 96    │ 96-103                                  │
│  torso-carry     │ 104   │ 104-111                                 │
│  torso-out       │ 112   │ 112-119                                 │
│  torso-out2      │ 120   │ 120-127                                 │
│  head            │ 128   │ 128,129,130,131,132,133,134,135         │
│  shadow          │ 136   │ 136-143                                 │
│                                                                     │
│  Total: 144 tiles (18 actions × 8 directions)                       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Clojure Implementation (Incorrect)

**File**: `src/game/sprite.clj:357-388`

```clojure
(defn get-tile-index [sheet-type action direction]
  (try
    (let [NR 1  ; !!! WRONG: Should be 8, not 1 !!!
          base-indices {...}]  ; Base indices calculated with NR=1
      (case sheet-type
        :sheet (let [base (get base-indices action 0)]
                 (+ base (mod direction NR)))  ; Always returns base!
        :lay (mod direction 6)
        0))
```

### Bugs Identified

1. **NR = 1 instead of NR = 8** (line 359)
   - All base indices are calculated as `i * 1 = i` instead of `i * 8`
   - Java: `IHEAD = 16 * 8 = 128`
   - Clojure: `:head = 16 * 1 = 16`

2. **Incorrect modulo operation** (line 383)
   - `(mod direction NR)` where `NR = 1` always returns 0
   - Direction (0-7) is never added to the base index
   - The correct formula is `(+ base direction)` (no modulo needed)

3. **Comparison of indices**:

   | Action | Java (Correct) | Clojure (Wrong) |
   |--------|----------------|-----------------|
   | :feet-none | 0 | 0 |
   | :feet-right | 8 | 1 |
   | :tunic | 40 | 5 |
   | :torso-still | 48 | 6 |
   | :head | 128 | 16 |
   | :shadow | 136 | 17 |

4. **Example bug**:
   - `(get-tile-index :sheet :head 0)` returns `16 + (mod 0 1) = 16`
   - Should return `128 + 0 = 128`
   - This points to the wrong sprite entirely (tunic at direction 0 instead of head)

## Code References

### Java Files (Correct implementation)

- `sos-src/settlement/entity/humanoid/spirte/HSpriteConst.java:19-38` - Tile index constants with NR=8
- `sos-src/settlement/entity/humanoid/spirte/HSprite.java:211` - Usage: `dir + IHEAD`
- `sos-src/init/race/appearence/RaceSheet.java:18-29` - Spritesheet initialization
- `sos-src/util/spritecomposer/ComposerSources.java:711-807` - Singles class with `setSkip()` method

### Clojure Files (Buggy implementation)

- `src/game/sprite.clj:357-388` - The `get-tile-index` function with bugs
  - Line 359: `NR 1` should be `NR 8`
  - Line 383: `(mod direction NR)` should be `direction`

## Architecture Documentation

### How Tile Indexing Works

1. **Base indices** are calculated as `action_index * NR` where NR = 8 (number of directions)
2. **Final tile index** = `base_index + direction` (where direction is 0-7)
3. **Example**: For head facing direction 3:
   - Base index = 128 (16th action * 8)
   - Final index = 128 + 3 = 131

### How the Java Spritesheet Cropping Works

From `ComposerSources.java:756-761`:
```java
private void calc() {
    int tx = tileCurrent % tilesX;  // Column in source
    int ty = tileCurrent / tilesX;  // Row in source
    pixelX = body.getStartX() + tx * (size + m);  // m = 6 pixels padding
    pixelY = body.getStartY() + ty * (size + m);
}
```

The `Singles` class:
- Uses `m = 6` pixels padding between sprites
- Calculates pixel position as `startX + tx * (size + 6)` and `startY + ty * (size + 6)`
- For sheet: size = 24, so sprites are at `6 + tx * 30` and `6 + ty * 30`
- `setSkip(start, amount)` controls which tiles to process

## Historical Context (from thoughts/)

No relevant historical documents found in thoughts/ directory regarding spritesheet tile indexing.

## Related Research

No related research documents found.

## Open Questions

1. Is there a reason the Clojure code was written with `NR = 1`? Perhaps it was intended for a different use case?
2. Are there other places in the Clojure codebase that assume the wrong tile indices?
3. What about the `:lay` sheet implementation - is it also incorrect?
