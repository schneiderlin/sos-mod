# Wiki Extraction Documentation

This folder contains detailed documentation for extracting each type of game data and sprite.

## Document Index

### Data Extraction
| Document | Description | Status |
|----------|-------------|--------|
| [resources.md](resources.md) | Resources/Items extraction | ✅ Done |
| [races.md](races.md) | Race data extraction | Pending |
| [technologies.md](technologies.md) | Tech tree extraction | Pending |
| [buildings.md](buildings.md) | Buildings/Rooms extraction | Pending |
| [types.md](types.md) | Game types & enums | Pending |
| [boosters.md](boosters.md) | Booster system | Pending |
| [religions.md](religions.md) | Religion data | Pending |
| [structures.md](structures.md) | World structures | Pending |

### Sprite Extraction
| Document | Description | Status |
|----------|-------------|--------|
| [sprites-race.md](sprites-race.md) | Race/character sprites | ✅ In Progress |
| [sprites-icons.md](sprites-icons.md) | UI icons | Pending |
| [sprites-resources.md](sprites-resources.md) | Resource/item icons | Pending |
| [sprites-buildings.md](sprites-buildings.md) | Building sprites | Pending |
| [sprites-game.md](sprites-game.md) | Misc game sprites | Pending |

## How to Use

1. Each document describes:
   - What data/sprites to extract
   - Java classes involved
   - Clojure code patterns
   - Output format specification

2. Implementation goes in `src/game/` as Clojure files

3. Extracted data goes to `output/` folder

## Template

When creating new extraction docs, follow this structure:

```markdown
# [Category] Extraction

## Overview
Brief description of what we're extracting.

## Java Classes
- `package.Class` - description

## Data Fields
| Field | Type | Description |
|-------|------|-------------|

## Extraction Code
\`\`\`clojure
;; Example code
\`\`\`

## Output Schema
\`\`\`edn
{:field value}
\`\`\`

## Progress
- [ ] Task 1
- [ ] Task 2
```

## Code Organization

Code is split between two locations:

### `src/game/` - Reusable Game API
Functions for accessing game data, useful for both wiki extraction AND modding.

| File | Description |
|------|-------------|
| `resource.clj` | Resource registry access |
| `sprite.clj` | Sprite/icon extraction |
| *more to come* | |

### `src/extract/` - Extraction Scripts
Wiki-specific extraction code that uses the game API.

| File | Description |
|------|-------------|
| `resource_extract.clj` | Extract resources to EDN |
| *more to come* | |

### Why This Split?

1. **Reusability**: Game API code can be used for modding, not just wiki
2. **Separation**: Extraction logic (what to export) vs access logic (how to read)
3. **Testing**: Game API can be tested independently

