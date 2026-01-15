# Songs of Syx Wiki Data Extraction Plan

## Project Overview

**Goal**: Build a comprehensive game wiki by extracting game data and sprites directly from the game code using Clojure.

**Approach**: 
1. Use Clojure interop to access Java game classes at runtime
2. Extract structured data (EDN/JSON) for wiki consumption
3. Export sprite images from game assets (data.zip)

**Current Status**: Initial sprite extraction working (see `src/game/sprite.clj`)

---

## Data Categories to Extract

### 1. Resources (资源) - `init.resources`
Items/materials in the game.

| Data Type | Java Class | Priority | Status |
|-----------|-----------|----------|--------|
| All Resources | `RESOURCES.ALL()` | High | Pending |
| Stone | `RESOURCES.STONE()` | High | Pending |
| Wood | `RESOURCES.WOOD()` | High | Pending |
| Livestock | `RESOURCES.LIVESTOCK()` | High | Pending |
| Minables | `RESOURCES.minables()` | Medium | Pending |
| Growables | `RESOURCES.growable()` | Medium | Pending |
| Drinks | `RESOURCES.DRINKS()` | Medium | Pending |
| Edibles | `RESOURCES.EDI()` | Medium | Pending |

**Details Doc**: `doc/wiki/extract/resources.md`

---

### 2. Races (种族) - `init.race`
Playable and non-playable species.

| Data Type | Java Class | Priority | Status |
|-----------|-----------|----------|--------|
| All Races | `RACES.all()` | High | Pending |
| Playable Races | `RACES.playable()` | High | Pending |
| Race Stats | `Race.stats` | High | Pending |
| Race Appearance | `Race.appearance` | Medium | Pending |
| Race Bio | `Race.bio` | Medium | Pending |
| Race Boosts | `RACES.boosts()` | Low | Pending |

**Details Doc**: `doc/wiki/extract/races.md`

---

### 3. Technologies (科技) - `init.tech`
Research tree and unlockables.

| Data Type | Java Class | Priority | Status |
|-----------|-----------|----------|--------|
| All Techs | `TECHS.ALL()` | High | Pending |
| Tech Trees | `TECHS.TREES()` | High | Pending |
| Tech Costs | `TECHS.COSTS()` | Medium | Pending |
| Tech Requirements | `TECH.requires()` | Medium | Pending |

**Details Doc**: `doc/wiki/extract/technologies.md`

---

### 4. Buildings/Rooms (建筑/房间) - `settlement.room`
All placeable structures.

| Category | Examples | Priority | Status |
|----------|----------|----------|--------|
| Food Production | Farm, Fishery, Hunter, Pasture, Orchard | High | Pending |
| Industry | Mine, Refiner, Workshop, Woodcutter | High | Pending |
| Infrastructure | Stockpile, Hauler, Station, Gate | High | Pending |
| Services | Tavern, Bath, Market, Arena, Stage | Medium | Pending |
| Health | Hospital, Physician, Asylum | Medium | Pending |
| Knowledge | School, Library, University, Laboratory | Medium | Pending |
| Law | Guard, Prison, Court, Execution | Medium | Pending |
| Military | Training, Artillery, Supply | Medium | Pending |
| Spirit | Temple, Shrine, Grave, Dump | Low | Pending |
| Housing | Home, Chamber | Low | Pending |
| Water | Pump, Canal, Drain, Pool | Low | Pending |

**Details Doc**: `doc/wiki/extract/buildings.md`

---

### 5. Types & Enums (类型枚举) - `init.type`
Game constants and classifications.

| Data Type | Java Class | Priority | Status |
|-----------|-----------|----------|--------|
| Terrains | `TERRAINS` | High | Pending |
| Climates | `CLIMATES` | Medium | Pending |
| Diseases | `DISEASES` | Medium | Pending |
| Traits | `TRAITS` | Medium | Pending |
| Needs | `NEEDS` | Medium | Pending |
| Humanoid Classes | `HCLASSES` | Low | Pending |
| Humanoid Types | `HTYPES` | Low | Pending |

**Details Doc**: `doc/wiki/extract/types.md`

---

### 6. Boosters (增益) - `game.boosting`
All stat modifiers and bonuses.

| Data Type | Java Class | Priority | Status |
|-----------|-----------|----------|--------|
| All Boostables | `BOOSTABLES` | Medium | Pending |
| Boost Categories | `BoostableCat` | Medium | Pending |
| Boost Specs | `BoostSpecs` | Low | Pending |

**Existing Doc**: `doc/booster/boosters_all.md`
**Details Doc**: `doc/wiki/extract/boosters.md`

---

### 7. Religion (宗教) - `init.religion`
Religious systems.

| Data Type | Java Class | Priority | Status |
|-----------|-----------|----------|--------|
| All Religions | `RELIGIONS` | Low | Pending |

**Details Doc**: `doc/wiki/extract/religions.md`

---

### 8. Structures (结构) - `init.structure`
World structures.

| Data Type | Java Class | Priority | Status |
|-----------|-----------|----------|--------|
| All Structures | `STRUCTURES` | Low | Pending |

**Details Doc**: `doc/wiki/extract/structures.md`

---

## Sprite Categories to Extract

### 1. Race Sprites (种族精灵图) ✅ Partially Done
Character appearance sprites.

| Sprite Type | Source | Size | Status |
|-------------|--------|------|--------|
| Sheet (standing/walking) | `{Race}.png` left half | 24x24 | ✅ Working |
| Sheet Normal Maps | `{Race}.png` right half | 24x24 | ✅ Working |
| Lay (lying down) | `{Race}.png` lay section | 32x32 | ✅ Working |
| Lay Normal Maps | `{Race}.png` lay section | 32x32 | ✅ Working |
| Portraits | Portrait sprites | Various | Pending |

**Code**: `src/game/sprite.clj` - `export-race-sprite`
**Details Doc**: `doc/wiki/extract/sprites-race.md`

---

### 2. UI Icons (界面图标)
Small icons used in UI.

| Icon Size | Accessor | Size | Status |
|-----------|----------|------|--------|
| Small | `SPRITES.icons().s` | 16x16 | Pending |
| Medium | `SPRITES.icons().m` | 24x24 | Pending |
| Large | `SPRITES.icons().l` | 32x32 | Pending |

**Code**: `src/game/sprite.clj` - `icon-small`, `icon-medium`, `icon-large`
**Details Doc**: `doc/wiki/extract/sprites-icons.md`

---

### 3. Resource Sprites (资源精灵图)
Item/material icons.

| Sprite Type | Source | Status |
|-------------|--------|--------|
| Resource Icons | `init.resources.Sprite` | Pending |
| Debris Sprites | debris folder | Pending |

**Details Doc**: `doc/wiki/extract/sprites-resources.md`

---

### 4. Building/Room Sprites (建筑精灵图)
Structure visuals.

| Sprite Type | Source | Status |
|-------------|--------|--------|
| Room Sprites | `settlement.room.sprite` | Pending |
| Furniture Sprites | Various | Pending |
| Construction Overlays | `SPRITES.cons()` | Pending |

**Details Doc**: `doc/wiki/extract/sprites-buildings.md`

---

### 5. Game Sprites (游戏精灵图)
Miscellaneous game graphics.

| Sprite Type | Accessor | Status |
|-------------|----------|--------|
| Game Sheets | `SPRITES.GAME()` | Pending |
| Textures | `SPRITES.textures()` | Pending |
| Special Sprites | `SPRITES.specials()` | Pending |
| Load Screen | `SPRITES.loadScreen()` | Pending |

**Details Doc**: `doc/wiki/extract/sprites-game.md`

---

## Output Structure

```
output/
├── data/                    # Structured data (EDN/JSON)
│   ├── resources.edn
│   ├── races.edn
│   ├── technologies.edn
│   ├── buildings.edn
│   ├── types.edn
│   └── boosters.edn
├── sprites/                 # Exported images
│   ├── races/
│   │   ├── Human/
│   │   │   ├── sheet/
│   │   │   └── lay/
│   │   └── ...
│   ├── icons/
│   │   ├── small/
│   │   ├── medium/
│   │   └── large/
│   ├── resources/
│   └── buildings/
└── wiki/                    # Generated wiki content
    └── ...
```

---

## Implementation Roadmap

### Phase 1: Foundation (Current)
- [x] Set up Clojure project with game interop
- [x] Basic sprite extraction (`export-race-sprite`)
- [ ] Document extraction patterns

### Phase 2: Core Data Extraction
- [ ] Resources extraction (`src/game/resource.clj`)
- [ ] Races extraction (extend `src/game/sprite.clj`)
- [ ] Technologies extraction (`src/game/tech.clj`)
- [ ] Buildings/Rooms extraction (`src/game/building.clj`)

### Phase 3: Sprite Extraction
- [ ] Complete race sprite export (all races)
- [ ] UI icons export
- [ ] Resource sprites export
- [ ] Building sprites export

### Phase 4: Wiki Generation
- [ ] Generate markdown pages from data
- [ ] Link sprites to data
- [ ] Build wiki site (static generator)

---

## Technical Notes

### Accessing Game Data
```clojure
;; Import required classes
(import '[init.resources RESOURCES RESOURCE])
(import '[init.race RACES Race])
(import '[init.tech TECHS TECH])

;; Access all resources
(RESOURCES/ALL)

;; Access all races  
(RACES/all)

;; Access all technologies
(TECHS/ALL)
```

### Extracting Sprites from data.zip
```clojure
;; Using crop-from-png for custom regions
(crop-from-png "data/assets/sprite/race/Human.png"
               x y width height "output/sprite.png")

;; Using export-race-sprite for race sprites
(export-race-sprite :sheet "Human" :head "output/head.png")
```

### File Locations
- Game assets: `base/data.zip`
- Init configs: `data/init/` (in zip)
- Sprite assets: `data/assets/sprite/` (in zip)
- Text/locale: `base/locale.zip`

---

## Quick Reference

| Feature | Code Location | Doc Location |
|---------|--------------|--------------|
| Race Sprites | `src/game/sprite.clj` | `doc/src-code/race_sprite_usage.md` |
| Static Config | - | `doc/src-code/static_config_data.md` |
| Sprite Loading | - | `doc/src-code/sprite_loading.md` |
| Boosters | - | `doc/booster/boosters_all.md` |

---

## Next Steps

1. **Create extraction detail docs** for each category
2. **Start with Resources** - simplest data structure
3. **Build batch export** for race sprites (all races)
4. **Design output schema** for wiki consumption

---

*Last Updated: 2026-01-15*
*Status: Planning Phase*

