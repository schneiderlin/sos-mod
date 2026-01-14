# Race Sprite Sheet 使用说明

本文档详细说明 `base/data.zip/data/assets/sprite/race/` 目录中的 sprite sheet 是如何使用的，以 `Human.png` 为例。

## 文件位置

Race sprite 文件位于：
```
base/data.zip/data/assets/sprite/race/Human.png
```

游戏通过 `PATHS.SPRITE().getFolder("race")` 访问这个目录。

## 配置文件中的引用

在种族配置文件中（如 `init/race/Human.txt`），通过 `APPEARANCE` 部分引用 sprite 文件：

```json
APPEARANCE: {
    // 主 sprite 文件，指向 sprite/race/ 目录下的文件（不含扩展名）
    SPRITE_FILE: Human,        // 对应 Human.png
    
    // 额外 sprite（工具动画、污垢、水覆盖层等）
    SPRITE_EXTRA_FILE: Normal, // 对应 sprite/race/extra/Normal.png
    
    // 儿童 sprite（当个体是儿童时使用）
    SPRITE_CHILD_FILE: Humanoid, // 对应 sprite/race/child/Humanoid.png
    
    // ... 其他配置（颜色、肖像等）
}
```

## 加载流程

### 1. 初始化阶段

在 `RACES.expand()` 方法中：

```java
// 创建 ExpandInit，其中 sg 指向 sprite/race/ 目录
ExpandInit init = new ExpandInit();
// init.sg = PATHS.SPRITE().getFolder("race")

// 为每个种族扩展资源
for (Race r : i.all) {
    r.expand(init);  // 加载 sprite 等资源
}
```

### 2. Sprite Sheet 加载

在 `RType` 构造函数中（`sos-src/init/race/appearence/RType.java`）：

```java
RType(RColors colors, Json json, RExtras extra, ExpandInit init) {
    // 从配置读取 sprite 文件名
    String ssprite = json.value("SPRITE_FILE");  // "Human"
    
    // 检查缓存，如果已加载则复用
    if (init.map.containsKey(ssprite)) {
        sheet = init.map.get(ssprite);
    } else {
        // 加载并创建 RaceSheet
        // init.sg.get("Human") 返回 Human.png 的路径
        sheet = new RaceSheet(init.sg.get(ssprite));
        init.map.put(ssprite, sheet);  // 缓存
    }
}
```

### 3. RaceSheet 处理

`RaceSheet` 类（`sos-src/init/race/appearence/RaceSheet.java`）将 PNG 图片处理成两个 `TILE_SHEET`：

#### a) `sheet` - 站立/行走 Sprite

```java
sheet = new ITileSheet(path, 448, 546) {
    protected TILE_SHEET init(ComposerUtil c, ComposerSources s, ComposerDests d) {
        int a = 18;  // 18 行
        // 初始化：从 (0,0) 开始，每行 2 个 sprite，共 18 行，输出到 s24 (24x24 像素)
        s.singles.init(0, 0, 1, 1, 2, a, d.s24);
        
        // 处理每一行：跳过阴影层（每行第 2 个），粘贴到第 3 个位置
        for (int i = 0; i < a; i++) {
            s.singles.setSkip(i * 2, 2).paste(3, true);
        }
        return d.s24.saveGame();
    }
}.get();
```

**图片结构**：
- 图片尺寸：448 x 546 像素
- 每行 2 个 sprite（左侧是主体，右侧是阴影）
- 共 18 行
- 每个 sprite 尺寸：约 224 x 30 像素（实际输出为 24x24）

**处理逻辑**：
1. 图片左侧（0-224 像素）是主体 sprite
2. 图片右侧（224-448 像素）是阴影 sprite
3. 代码跳过阴影，只使用主体部分
4. 最终生成 18 个 24x24 的 sprite，用于不同方向和动作

#### b) `lay` - 躺下 Sprite

```java
lay = new ITileSheet() {
    protected TILE_SHEET init(ComposerUtil c, ComposerSources s, ComposerDests d) {
        int a = 6;  // 6 个 sprite
        // 从原图右侧部分（x2() 位置）开始，每行 4 个，共 3 行，输出到 s32 (32x32 像素)
        s.singles.init(s.singles.body().x2(), 0, 1, 1, 4, 3, d.s32);
        
        for (int i = 0; i < a; i++) {
            s.singles.setSkip(i * 2, 2).paste(3, true);
        }
        return d.s32.saveGame();
    }
}.get();
```

**图片结构**：
- 从原图右侧部分提取
- 每行 4 个 sprite（主体 + 阴影交替）
- 共 3 行，6 个 sprite
- 每个 sprite 尺寸：32x32 像素

## Sprite Sheet 布局示例

以 `Human.png` 为例，图片布局大致如下：

```
┌─────────────────────────────────────────────────────────┐
│ 448 像素宽                                               │
├──────────────────┬──────────────────────────────────────┤
│ 主体 Sprite      │ 阴影 Sprite                          │
│ (224 像素)       │ (224 像素)                           │
│                  │                                      │
│ 行 0: 站立方向 0 │ 行 0: 阴影 0                         │
│ 行 1: 站立方向 1 │ 行 1: 阴影 1                         │
│ ...              │ ...                                  │
│ 行 17: 站立方向17│ 行 17: 阴影 17                       │
│                  │                                      │
│                  │ ┌──────────────────────────────────┐ │
│                  │ │ 躺下 Sprite (从右侧提取)          │ │
│                  │ │ 行 0-2: 躺下方向 0-5             │ │
│                  │ └──────────────────────────────────┘ │
└──────────────────┴──────────────────────────────────────┘
     546 像素高
```

## 在游戏中的使用

### 1. 获取 Sprite Sheet

```clojure
;; 在 Clojure 中，可以通过反射访问
(import '[init.race RACES])

;; 获取某个种族的 appearance
(def human-race (first (filter #(= "Human" (.key %)) (RACES/all))))

;; 获取 adult 类型的 sheet
(def adult-sheet (-> human-race .appearance .adult .sheet .sheet))

;; 获取 lay sheet（躺下）
(def lay-sheet (-> human-race .appearance .adult .sheet .lay))
```

### 2. 渲染实体

游戏在渲染实体时：
- 使用 `sheet` 渲染站立/行走的实体
- 使用 `lay` 渲染躺下/死亡的实体
- 根据实体的方向、动作、状态选择对应的 sprite frame

### 3. 颜色应用

Sprite sheet 中的颜色会被配置文件中定义的 `COLORS` 替换：
- `COLOR_SKIN`: 皮肤颜色
- `COLOR_HAIR`: 头发颜色
- `COLOR_EYE`: 眼睛颜色
- `COLOR_LEG`: 腿部颜色
- `COLOR_CLOTHES`: 衣服颜色（根据装备的衣物数量调整饱和度）

## 关键代码位置

1. **路径定义**: `sos-src/init/paths/PATHS.java`
   - `PATHS.SPRITE().getFolder("race")` 指向 `sprite/race/` 目录

2. **加载入口**: `sos-src/init/race/RACES.java`
   - `RACES.expand()` 初始化所有种族的 sprite

3. **Sprite 处理**: `sos-src/init/race/appearence/RaceSheet.java`
   - 将 PNG 图片转换为 `TILE_SHEET`

4. **类型定义**: `sos-src/init/race/appearence/RType.java`
   - 管理不同类型的 sprite（adult/child）

5. **外观配置**: `sos-src/init/race/appearence/RAppearence.java`
   - 管理整个外观系统

## 注意事项

1. **文件命名**: 配置中的 `SPRITE_FILE` 值必须与 PNG 文件名（不含扩展名）匹配
2. **图片尺寸**: 必须符合 448x546 像素的规格
3. **缓存机制**: 相同文件名的 sprite 会被缓存，避免重复加载
4. **颜色替换**: Sprite 中的颜色会被配置文件中的颜色值替换
5. **方向数量**: 默认支持多个方向（通过不同的 frame 实现）

## 示例：Human.png 的使用

1. **配置文件** (`init/race/Human.txt`):
   ```json
   APPEARANCE: {
       SPRITE_FILE: Human,  // 指向 Human.png
       // ...
   }
   ```

2. **加载过程**:
   - `RACES.expand()` 被调用
   - `ExpandInit.sg.get("Human")` 返回 `Human.png` 的路径
   - `RaceSheet` 构造函数加载并处理图片
   - 生成 `sheet` 和 `lay` 两个 `TILE_SHEET`

3. **游戏使用**:
   - 渲染人类实体时，根据状态选择 `sheet` 或 `lay`
   - 根据方向、动作选择对应的 frame
   - 应用配置的颜色替换

## 获取单个 Sprite 并导出为图片

### 输入参数

当你输入 `sheet` 或 `lay`，加上方向和动作时，你会得到一个 **TILE_SHEET** 对象和对应的 **tile index**。

- **sheet**: 站立/行走 sprite sheet（18 个 sprite，24x24 像素）
- **lay**: 躺下 sprite sheet（6 个 sprite，32x32 像素）
- **方向 (direction)**: 
  - 对于 `sheet`: 0-7（8 个方向）
  - 对于 `lay`: 0-5（6 个方向）
- **动作 (action)**: 
  - 对于 `sheet`: `:head`, `:torso-still`, `:torso-right`, `:torso-left`, `:torso-carry`, `:tunic`, `:feet-none`, `:feet-right`, `:feet-left`, `:shadow`
  - 对于 `lay`: 不需要动作参数（只有方向）

### 返回结果

调用相关函数会返回：

1. **TILE_SHEET 对象**: 包含所有 sprite 的纹理表
2. **Tile Index**: 计算得到的 tile 索引（动作基础索引 + 方向）
3. **TextureCoords**: 纹理坐标对象，包含该 sprite 在纹理中的位置信息

### 使用示例

```clojure
(require '[game.sprite :as sprite])

;; 1. 获取 Human 种族的 sheet
(def human-sheet (sprite/get-race-sheet :sheet "Human"))
(def human-lay (sprite/get-race-sheet :lay "Human"))

;; 2. 获取 tile sheet 信息
(sprite/tile-sheet-info human-sheet)
;; => {:size 24, :tiles 18, :width nil, :height nil}

;; 3. 计算特定方向和动作的 tile index
(sprite/get-tile-index :sheet :head 0)  ; 方向 0 的头部
(sprite/get-tile-index :sheet :torso-still 3)  ; 方向 3 的静止躯干
(sprite/get-tile-index :lay nil 0)  ; 躺下方向 0

;; 4. 获取纹理坐标
(let [tile-index (sprite/get-tile-index :sheet :head 0)]
  (sprite/get-tile-texture human-sheet tile-index))
;; => TextureCoords 对象

;; 5. 导出为 PNG（注意：需要完整的渲染实现）
(sprite/export-race-sprite :sheet "Human" :head 0 "output/head_0.png")
(sprite/export-race-sprite :lay "Human" nil 0 "output/lay_0.png" :adult true :scale 2)
```

### 导出为图片的注意事项

**重要**: 直接从 OpenGL 纹理提取像素数据比较复杂，因为：

1. **纹理在 GPU 上**: TILE_SHEET 的纹理数据存储在 GPU 的显存中
2. **需要渲染**: 通常需要将 sprite 渲染到离屏缓冲区（framebuffer），然后读取像素
3. **颜色替换**: 游戏在渲染时会应用颜色替换（COLORS 配置），直接导出可能不包含这些效果

**可行的方案**:

1. **使用游戏渲染系统**: 
   - 创建一个离屏渲染目标
   - 使用游戏的 Renderer 渲染 sprite
   - 读取渲染结果到 BufferedImage
   - 保存为 PNG

2. **直接读取原始 PNG**: 
   - 从 `base/data.zip/data/assets/sprite/race/Human.png` 直接读取
   - 根据 tile index 计算位置
   - 裁剪对应的 sprite 区域
   - 保存为单独的 PNG 文件

3. **使用游戏截图功能**: 
   - 在游戏中渲染 sprite
   - 使用游戏的截图功能保存

### Tile Index 计算规则

对于 `sheet` (站立/行走):
- 每个动作有基础索引（base index）
- 最终索引 = base_index + (direction % 8)
- 例如：`:head` 基础索引是 0，方向 3 的索引是 0 + 3 = 3

对于 `lay` (躺下):
- 直接使用方向作为索引（0-5）

### 动作常量映射

从 `HSpriteConst.java` 中的常量：
- `IHEAD = 0 * 8 = 0` (头部)
- `ITORSO_STILL = 0 * 8 = 0` (静止躯干)
- `ITORSO_RIGHT = 1 * 8 = 8` (向右躯干)
- `ITORSO_LEFT = 2 * 8 = 16` (向左躯干)
- `ITORSO_CARRY = 3 * 8 = 24` (搬运躯干)
- `ITUNIC = 2 * 8 = 16` (衣服)
- `IFEET_NONE = 0 * 8 = 0` (无脚部动作)
- `IFEET_RIGHT = 1 * 8 = 8` (右脚)
- `IFEET_LEFT = 3 * 8 = 24` (左脚)
- `ISHADOW = 1 * 8 = 8` (阴影)

