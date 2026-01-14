# Sprite 加载机制和资源类型

本文档详细说明了 Songs of Syx 游戏中 sprite 的加载机制和可用的 sprite 资源类型。

## 核心类

### SPRITES 类

**位置**: `sos-src/init/sprite/SPRITES.java`

`SPRITES` 是游戏中所有 sprite 资源的主入口点，提供以下静态方法：

```java
SPRITES.GAME()        // 游戏内建筑/物体的 sprite
SPRITES.cons()        // 建筑覆盖层和面板 sprite
SPRITES.icons()       // UI 图标 (16x16, 24x24, 32x32)
SPRITES.sett()        // 定居点相关 sprite
SPRITES.armyCard()    // 军队卡片 sprite
SPRITES.loadScreen()  // 加载屏幕 sprite
SPRITES.specials()    // 特殊 sprite
SPRITES.textures()    // 纹理 sprite
SPRITES.loader()      // 加载进度打印器
```

## Sprite 资源路径

**位置**: `sos-src/init/paths/PATHS.java`

游戏使用以下路径来组织 sprite 资源：

```java
PATHS.SPRITE()                    // 基础 sprite 路径
PATHS.SPRITE_GAME()              // 游戏内 sprite (建筑、物体等)
PATHS.SPRITE_UI()                // UI sprite (界面元素)
PATHS.SPRITE_SETTLEMENT()        // 定居点 sprite
PATHS.SPRITE_SETTLEMENT_MAP()    // 定居点地图 sprite
```

## Sprite 类型 (SheetType)

**位置**: `sos-src/init/sprite/game/SheetType.java`

游戏支持多种 sprite 类型，每种类型对应不同的瓦片尺寸和用途：

### 1. 1x1 Sprite (`s1x1`)
- **路径**: `sprite/game/1x1/`
- **尺寸**: 16+6 x 16+6 像素 (每个瓦片)
- **用途**: 单个瓦片的建筑和物体
- **支持旋转**: 是
- **示例**: 小型建筑、装饰物

### 2. 2x2 Sprite (`s2x2`)
- **路径**: `sprite/game/2x2/`
- **尺寸**: 2x2 瓦片 (32+12 x 32+12 像素)
- **用途**: 中等大小的建筑
- **支持旋转**: 是

### 3. 3x3 Sprite (`s3x3`)
- **路径**: `sprite/game/3x3/`
- **尺寸**: 3x3 瓦片 (48+12 x 48+12 像素)
- **用途**: 大型建筑
- **支持旋转**: 是

### 4. Combo Sprite (`sCombo`)
- **路径**: `sprite/game/combo/`
- **尺寸**: 72 x 72 像素
- **用途**: 复杂的组合建筑
- **支持旋转**: 否

### 5. Box Sprite (`sBox`)
- **路径**: `sprite/game/box/`
- **尺寸**: 76 x 76 像素
- **用途**: 盒子类型的建筑
- **支持旋转**: 是

### 6. Texture Sprite (`sTex`)
- **路径**: `sprite/game/texture/`
- **尺寸**: 16+6 x 16+6 像素
- **用途**: 纹理贴图
- **支持旋转**: 是

## UI Sprite 系统

### Icons (图标)

**位置**: `sos-src/init/sprite/UI/Icons.java`

图标系统分为三个尺寸类别：

#### 1. Small Icons (16x16) - `SPRITES.icons().s`
- **路径**: `sprite/ui/16/`
- **用途**: 小尺寸 UI 图标
- **示例图标**:
  - `magnifier`, `minifier`, `minimap`
  - `cancel`, `camera`, `menu`, `cog`
  - `storage`, `sword`, `shield`, `bow`
  - `citizen`, `slave`, `noble`
  - `plus`, `minus`, `arrowUp`, `arrowDown`
  - 等等...

#### 2. Medium Icons (24x24) - `SPRITES.icons().m`
- **路径**: `sprite/ui/24/`
- **用途**: 中等尺寸 UI 图标
- **示例图标**:
  - `clear_structure`, `capitol`, `furniture`
  - `cancel`, `terrain`, `wall`, `noble`
  - `sword`, `shield`, `bow`, `horn`
  - `plus`, `minus`, `rotate`, `exit`
  - `for_loose`, `for_tight`, `b_muster`
  - 等等...

#### 3. Large Icons (32x32) - `SPRITES.icons().l`
- **路径**: `sprite/ui/32/`
- **用途**: 大尺寸 UI 图标
- **示例图标**:
  - `agri`, `work`, `service`, `gov`
  - `mine`, `pasture`, `farm`, `fish`
  - `refiner`, `workshop`, `law`, `admin`
  - `water`, `health`, `entertain`, `death`
  - 等等...

### Images (通用图片)

**位置**: `sos-src/init/sprite/UI/UIImageMaker.java`

- **路径**: `sprite/image/`
- **尺寸**: 32x32 像素 (每个瓦片)
- **用途**: 通用图片资源
- **访问**: `SPRITES.icons().image().get(path)`

### UI 装饰和面板

**位置**: `sos-src/init/sprite/UI/UI.java`

```java
SPRITES.icons().FONT()      // 字体
SPRITES.icons().PANEL()     // 面板样式
SPRITES.icons().decor()     // 装饰元素
```

## 纹理系统 (Textures)

**位置**: `sos-src/init/sprite/Textures.java`

**路径**: `sprite/textures/`

游戏提供以下纹理资源：

```java
SPRITES.textures().dis_big      // 大位移纹理
SPRITES.textures().dis_small     // 小位移纹理
SPRITES.textures().dis_tiny      // 微小位移纹理
SPRITES.textures().dis_low       // 低分辨率位移纹理
SPRITES.textures().fire          // 火焰纹理
SPRITES.textures().water         // 水纹理
SPRITES.textures().bumps         // 凹凸纹理
SPRITES.textures().dots          // 点纹理
```

## 游戏 Sprite 加载机制

### GameSheets 类

**位置**: `sos-src/init/sprite/game/GameSheets.java`

`GameSheets` 负责加载和管理游戏内的 sprite 表：

```java
// 获取指定类型的 sprite 表
SPRITES.GAME().sheets(SheetType type, String file, Json error)

// 获取原始 TILE_SHEET
SPRITES.GAME().raws(SheetType type, String file, Json error)

// 获取单个 sprite (通过文件名和行号)
SPRITES.GAME().raw(SheetType type, String file, int row, Json error)

// 获取覆盖层 sprite
SPRITES.GAME().overlay(SheetType type)
```

### Sprite 文件格式

游戏 sprite 文件使用特殊的格式：

1. **图片尺寸要求**:
   - 宽度必须是 `(瓦片宽度 * 瓦片数量) * 2` (因为有阴影层)
   - 高度必须是 `瓦片高度 * 行数`

2. **文件引用格式**:
   ```
   FILENAME:ROW
   ```
   例如: `Building:0` 表示使用 `Building.png` 文件的第 0 行

3. **JSON 配置格式**:
   ```json
   {
     "FRAMES": [
       "Building:0",
       "Building:1",
       "-"  // 使用占位符
     ],
     "OVERWRITE": [
       // 可选的覆盖配置
     ]
   }
   ```

## 种族 Sprite

**位置**: `doc/res/race/_EXAMPLE.txt`

种族 sprite 使用特殊的配置：

```json
APPEARANCE: {
  SPRITE_FILE: Dondorian,        // 主 sprite 文件 (sprite/race/)
  SPRITE_EXTRA_FILE: Normal,     // 额外 sprite (sprite/race/extra/)
  SPRITE_CHILD_FILE: Humanoid,   // 儿童 sprite (sprite/race/child/)
  COLOR_CLOTHES: [...],          // 衣服颜色
  COLOR_ARMOUR_LEVELS: [...],    // 护甲颜色
  COLORS: {...}                   // 其他颜色配置
}
```

## 使用示例

### 在 Clojure 代码中访问 Sprite

```clojure
;; 获取 UI 图标
(SPRITES/icons)
(SPRITES/icons/s/sword)      ; 小图标
(SPRITES/icons/m/sword)     ; 中等图标
(SPRITES/icons/l/sword)     ; 大图标

;; 获取游戏 sprite
(SPRITES/GAME)
(SPRITES/GAME/sheets ...)

;; 获取纹理
(SPRITES/textures)
(SPRITES/textures/fire)
```

### 在 Java 代码中访问 Sprite

```java
// UI 图标
SPRITES.icons().s.sword
SPRITES.icons().m.sword
SPRITES.icons().l.sword

// 游戏 sprite
SPRITES.GAME().sheets(SheetType.s1x1, "Building", null)

// 纹理
SPRITES.textures().fire
```

## 重要注意事项

1. **文件路径**: Sprite 文件必须放在正确的路径下，否则会显示警告并使用占位符
2. **图片尺寸**: 必须符合对应 SheetType 的尺寸要求
3. **缓存机制**: Sprite 会被缓存，重复加载不会重新读取文件
4. **错误处理**: 如果 sprite 文件不存在，会使用 `DUMMY` sprite 作为占位符
5. **最大数量**: 游戏支持最多 65536 个 sprite (`C.MAX_SPRITES`)

## 相关文件

- `sos-src/init/sprite/SPRITES.java` - Sprite 主类
- `sos-src/init/sprite/game/GameSheets.java` - 游戏 sprite 管理
- `sos-src/init/sprite/game/SheetType.java` - Sprite 类型定义
- `sos-src/init/sprite/UI/UI.java` - UI sprite 系统
- `sos-src/init/sprite/UI/Icons.java` - 图标系统
- `sos-src/init/sprite/Textures.java` - 纹理系统
- `sos-src/init/paths/PATHS.java` - 路径管理

