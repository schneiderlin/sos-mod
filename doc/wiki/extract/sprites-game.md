# Game Sprites Extraction (游戏精灵图提取)

This document describes how to extract miscellaneous game sprites including textures, load screen, and UI sprites.

## Overview

The game sprites are organized into several categories:

| Category | Description | Source Location |
|----------|-------------|-----------------|
| **Textures** | Effect textures (fire, water, bumps, displacement maps) | `data/assets/sprite/textures/` |
| **Load Screen** | Game loading screen image | `data/assets/sprite/image/_LoadScreen.png` |
| **UI Specials** | UI special sprites (clockwork, seasons, buttons) | `data/assets/sprite/ui/Specials.png` |
| **UI Cons** | Construction/overlay sprites | `data/assets/sprite/ui/Cons.png` |
| **Game Sheets** | Game object sprite sheets (1x1, 2x2, 3x3) | `data/assets/sprite/game/` |

## Code Location

- **Extraction**: `src/extract/game_sprite.clj`
- **Sprite API**: `src/game/sprite.clj`

## Available Textures

The game includes 8 texture sprites:

| Texture Key | File | Description |
|-------------|------|-------------|
| `:fire` | Fire.png | Fire effect texture |
| `:water` | Water.png | Water effect texture |
| `:bumps` | Bumps.png | Bump map texture |
| `:dots` | Dots.png | Dot pattern texture |
| `:displacement-big` | Displacement_Big.png | Large displacement map |
| `:displacement-small` | Displacement_small.png | Small displacement map |
| `:displacement-tiny` | Displacement_tiny.png | Tiny displacement map |
| `:displacement-low` | Displacement_low.png | Low-res displacement map |

## Available UI Sprites

| Sprite Key | File | Description |
|------------|------|-------------|
| `:load-screen` | _LoadScreen.png | Game loading screen |
| `:specials` | Specials.png | UI special elements (time, seasons) |
| `:cons` | Cons.png | Construction overlay sprites |
| `:panels` | Panels.png | UI panel backgrounds |
| `:decor` | Decor.png | UI decorative elements |
| `:titles` | Titles.png | Title text sprites |
| `:title-box` | TitleBox.png | Title box backgrounds |
| `:faction-banners` | FactionBanners.png | Faction banner sprites |
| `:division-symbols` | DivisionSymbols.png | Military division symbols |

## Game Sheet Categories

Game sprite sheets are organized by tile size:

| Category | Path | Description |
|----------|------|-------------|
| `:1x1` | `game/1x1/` | Single-tile sprites (furniture, decorations) |
| `:2x2` | `game/2x2/` | 2x2 tile sprites (medium objects) |
| `:3x3` | `game/3x3/` | 3x3 tile sprites (large objects) |
| `:box` | `game/box/` | Box sprites |
| `:combo` | `game/combo/` | Combo sprites |
| `:texture` | `game/texture/` | Game texture sprites |

### Example 1x1 Sprites

- ANIMAL.png - Animal sprites
- CHAIRS.png - Chair furniture
- TABLES.png - Table furniture
- SHELVES.png - Shelf furniture
- STORAGE.png - Storage containers
- WORK.png - Work station sprites
- WALL.png - Wall decorations
- TORCH.png - Torch/lighting

## Usage Examples

### Export Single Texture

```clojure
(require '[extract.game-sprite :as gs])

;; Export fire texture
(gs/export-texture :fire "output/wiki/sprites/textures/fire.png")
;; => {:success true, :path "output/wiki/sprites/textures/fire.png", ...}

;; Export with scaling
(gs/export-texture :water "output/wiki/sprites/textures/water_2x.png" :scale 2)
```

### Export All Textures

```clojure
;; Export all 8 textures to a directory
(gs/export-all-textures "output/wiki/sprites/textures")
;; => {:success true, :count 8, :total 8, :output-dir "...", :results [...]}
```

### Export Load Screen

```clojure
(gs/export-load-screen "output/wiki/sprites/ui/load_screen.png")
```

### Export UI Sprites

```clojure
;; Export single UI sprite
(gs/export-ui-sprite :specials "output/wiki/sprites/ui/specials.png")

;; Export all UI sprites
(gs/export-all-ui-sprites "output/wiki/sprites/ui")
```

### List Game Sheet Files

```clojure
;; List all sprite files in 1x1 folder
(gs/list-game-sheet-files :1x1)
;; => ("2xBED.png" "2xROOF.png" "ANIMAL.png" "BARRACKS.png" ...)

;; Export a specific game sheet
(gs/export-game-sheet :1x1 "ANIMAL.png" "output/wiki/sprites/game/1x1/ANIMAL.png")
```

### Batch Export All

```clojure
;; Export everything at once
(gs/extract-all-game-sprites "output/wiki/sprites")
;; => {:success true, :output-dir "...", :results {:textures {...}, :ui {...}, :load-screen {...}}}

;; With options
(gs/extract-all-game-sprites "output/wiki/sprites"
                              :scale 1
                              :include-textures true
                              :include-ui true
                              :include-load-screen true)
```

## Runtime Sprite Access

When the game is running, you can access sprite objects directly:

```clojure
(require '[game.sprite :as sprite])

;; Get texture objects
(sprite/textures)
(sprite/texture-fire)
(sprite/texture-water)

;; Get special sprites
(sprite/special-sprites)

;; Get construction sprites
(sprite/cons-sprites)

;; Get load screen sprite
(sprite/load-screen)

;; Get game sheets
(sprite/game-sheets)
```

### Runtime Info Functions

```clojure
(require '[extract.game-sprite :as gs])

;; Get texture object info
(gs/textures-info)

;; Get specials info
(gs/specials-info)

;; Get construction sprites info
(gs/cons-info)

;; Get game sheets info
(gs/game-sheets-info)
```

## Java Class Reference

| Class | Access | Description |
|-------|--------|-------------|
| `SPRITES` | Static methods | Main sprite accessor |
| `SPRITES.textures()` | Returns `Textures` | Texture sprites |
| `SPRITES.specials()` | Returns `UISpecials` | UI special sprites |
| `SPRITES.cons()` | Returns `UIConses` | Construction sprites |
| `SPRITES.loadScreen()` | Returns `SPRITE` | Load screen sprite |
| `SPRITES.GAME()` | Returns `GameSheets` | Game sprite sheets |
| `SPRITES.icons()` | Returns `Icons` | UI icons |

## Output Structure

After running `extract-all-game-sprites`:

```
output/wiki/sprites/
├── textures/
│   ├── bumps.png
│   ├── displacement-big.png
│   ├── displacement-low.png
│   ├── displacement-small.png
│   ├── displacement-tiny.png
│   ├── dots.png
│   ├── fire.png
│   └── water.png
├── ui/
│   ├── cons.png
│   ├── decor.png
│   ├── division-symbols.png
│   ├── faction-banners.png
│   ├── load_screen.png
│   ├── panels.png
│   ├── specials.png
│   ├── title-box.png
│   └── titles.png
└── game/
    ├── 1x1/
    │   └── *.png
    ├── 2x2/
    │   └── *.png
    └── 3x3/
        └── *.png
```

## Related Documentation

- [Sprite Loading](../../src-code/sprite_loading.md) - How sprites are loaded
- [Race Sprites](sprites-race.md) - Race/character sprite extraction
- [UI Icons](sprites-icons.md) - Icon sprite extraction
- [Resource Sprites](sprites-resources.md) - Resource icon extraction
- [Building Sprites](sprites-buildings.md) - Building icon extraction

---

*Last Updated: 2026-01-17*

