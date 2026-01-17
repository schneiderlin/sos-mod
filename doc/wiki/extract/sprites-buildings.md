# Building/Room Sprite Extraction

## Overview

Building/room sprites consist of:
1. **Room Icons** - UI icons shown in menus (24x24 or 32x32)
2. **Furniture Sprites** - In-game furniture visuals (pending)
3. **Construction Overlays** - Building placement overlays (pending)

## Room Icon Extraction

### Simple Icons (IconSheet)

Most room icons are stored as tiles in icon sheets and can be extracted using the tile index:

```clojure
(require '[extract.building :as build-extract])

;; Export all room icons
(build-extract/export-room-icons "output/wiki/sprites/buildings")

;; Export single room icon
(build-extract/export-single-room-icon 
  (build/find-room-by-key "FARM") 
  "output/test/buildings")
```

**Status**: ✅ Working - 81/115 rooms extracted successfully

### Composite Icons (BG+FG) - TODO

34 rooms have **composite icons** that combine a background (BG) and foreground (FG) sprite. These cannot be auto-extracted with the simple tile-index method.

#### How Composite Icons Work

From `Icons.java`:
```java
SPRITE bg = get(json.value("BG"), json, "BG").scaled(scale);
SPRITE fg = get(json.value("FG"), json, "FG").scaled(scale);
```

The icon is rendered by:
1. Drawing the BG sprite
2. Optionally drawing a shadow
3. Drawing the FG sprite (with offset and color)

#### Affected Rooms

```clojure
(build-extract/list-composite-icon-rooms)
```

Groups by base type:
- **MINE**: MINE_CLAY, MINE_COAL, MINE_GEM, MINE_ORE, MINE_SAND, MINE_STONE
- **FISHERY**: FISHERY_NORMAL
- **REFINER**: Multiple variants
- **WORKSHOP**: Multiple variants
- etc.

#### Potential Solutions

1. **Use base room icon**: For variants like MINE_CLAY, use the MINE base icon
2. **Manual extraction**: Create these icons manually in image editor
3. **Programmatic rendering**: Implement composite icon rendering:
   - Extract BG icon tile index
   - Extract FG icon tile index  
   - Composite them with proper offsets/colors

#### Implementation Notes

The composite icon's inner sprite is an anonymous `SPRITE.Imp` class that captures BG/FG references:

```clojure
;; Check if icon is composite
(build/icon-is-composite? icon)  ; => true for composite

;; Get inner class name for debugging
(build/icon-inner-class-name icon)  ; => "init.sprite.UI.Icons$M$1" (anonymous class)
```

To properly extract composite icons, we would need to:
1. Access the captured BG and FG sprites from the anonymous class
2. Get their tile indices (if they're IconSheets)
3. Render them to a BufferedImage with proper compositing

## Files

| File | Purpose |
|------|---------|
| `src/game/building.clj` | Room icon access functions |
| `src/extract/building.clj` | Room icon export functions |

## Related Documentation

- `doc/wiki/extract/buildings.md` - Building data extraction
- `doc/wiki/extract/sprites-icons.md` - UI icon extraction
- `sos-src/init/sprite/UI/Icons.java` - Icon system source

---

*Last Updated: 2026-01-17*

