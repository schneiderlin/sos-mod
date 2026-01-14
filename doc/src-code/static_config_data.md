# 游戏静态配置数据

本文档总结了 Songs of Syx 游戏中的各种静态配置数据，包括搬运能力、腐烂速率、种族工作偏好等。

## 1. 工人搬运能力 (Carrying Capacity)

### 常量定义

**位置**: `sos-src/settlement/room/infra/stockpile/ROOM_STOCKPILE.java`

```java
public static final int MIN_CARRY = 8;  // 最小搬运量
```

**位置**: `sos-src/settlement/entity/humanoid/ai/work/WorkAbs.java`

```java
static int maxCarry = 4;  // 工作时最大搬运量
```

### 动态搬运能力计算

**位置**: `sos-src/settlement/room/infra/stockpile/ROOM_STOCKPILE.java`

```java
public int carryCap(Humanoid skill) {
    double dam = bonus.get(skill.indu());
    int am = (int) dam;
    am += RND.rFloat() < dam-am ? 1 : 0;
    am = CLAMP.i(am, 1, 100);  // 范围限制在 1-100
    return am;
}
```

**说明**:
- 搬运能力基于工人的技能和加成（bonus）
- 实际搬运量在 1-100 之间
- 最小搬运量为 8（`MIN_CARRY`）
- 工作时最大搬运量为 4（`WorkAbs.maxCarry`）

### 使用场景

- **仓库系统**: `SETT.ROOMS().STOCKPILE.carryCap(skill)` - 计算物流工人的搬运能力
- **运输系统**: 使用 `MIN_CARRY` 作为最小搬运量
- **工作系统**: 使用 `maxCarry = 4` 限制工作时搬运的资源数量

## 2. 资源腐烂速率 (Decay/Spoilage Rate)

### 资源腐烂配置

**位置**: `sos-src/init/resources/RESOURCE.java`

每个资源在配置文件中定义 `DEGRADE_RATE`:

```java
degradeSpeed = data.d("DEGRADE_RATE", 0, 1);  // 0-1之间的值
```

**获取方法**:
```java
public double degradeSpeed() {
    return degradeSpeed;
}
```

### 腐烂计算逻辑

**位置**: `sos-src/settlement/main/TUpdater.java`

```java
// 基础腐烂速率
double base = degradePerYear;

// 加成影响（SPOILAGE booster 可以降低腐烂速率）
double bonus = CLAMP.d(1.0/(BOOSTABLES.CIVICS().SPOILAGE.get(POP_CL.clP(null, null))), 0, 10);

// 有屋顶的地方腐烂速率降低 25%
if (SETT.TERRAIN().get(now).roofIs())
    b *= 0.75;

// 散落在地上的资源腐烂速率翻倍
if (t instanceof ScatteredResource) {
    degrade((RESOURCE_TILE) t, b*2);
}

// 存储在仓库中的资源腐烂速率降低
// bonus*base*t.spoilRate()*0.75
```

**影响因素**:
1. **资源本身的 `DEGRADE_RATE`**: 每个资源在配置文件中定义（0-1之间）
2. **SPOILAGE booster**: 通过 `BOOSTABLES.CIVICS().SPOILAGE` 可以降低腐烂速率
3. **存储位置**:
   - 散落在地上: 腐烂速率 × 2
   - 存储在仓库: 腐烂速率 × 0.75
   - 有屋顶的地方: 腐烂速率 × 0.75

### 相关 Booster

**位置**: `sos-src/game/boosting/BOOSTABLES.java`

```java
public final Boostable SPOILAGE = make("SPOILAGE", 1.0, UI.icons().s.fly,
    D.g("CIVIC_SPOILAGE", "Conservation"),
    D.g("CIVIC_SPOILAGE_D", "Decreases the decay rate of goods."));
```

## 3. 种族工作偏好和速率 (Race Work Preferences & Efficiency)

### 种族配置文件

**位置**: `doc/res/race/_EXAMPLE.txt` 和 `sos-src/init/race/`

### 工作偏好配置

```txt
PREFERRED: {
    **What work the race preferes. Keys are to room files in init/room
    WORK: {
        *: 0.5,              // 通配符，所有工作的基础偏好
        FARM_COTTON: 1.0,    // 特定工作的偏好值
        FARM_FRUIT: 1.0,
        FARM_GRAIN: 1.0,
        FARM_VEG: 1.0,
        FARM_SPICES: 1.0,
        EAT_EATERY_NORMAL: 1.0,
        EAT_CANTEEN_NORMAL: 1.0,
        EAT_TAVERN_NORMAL: 1.0,
    },
    ** The preference of being a recruit/soldier
    WORK_SOLDIER: 1.0,
    **The preference of being a student
    WORK_STUDENT: 0,
}
```

### 种族属性配置

```txt
PROPERTIES: {
    RESISTANCE_HOT: 0.8,      // 耐热性
    SKINNY_DIPS: 1.0,         // 游泳偏好
    INTELLIGENCE: 0.3,        // 智力
    DEATH_AGE: 75,            // 死亡年龄
    CRIMINALITY: 0.4,         // 犯罪率
    SUBMISSION: 1.0,          // 服从度
    ARMOUR: 0.1,              // 护甲
}
```

### 种族基础属性 (BONUS)

```txt
BONUS: {
    PHYSICS: {
        MASS: 80,             // 质量（影响战斗）
    },
    ** ROOM 子句包含所有有生产加成的房间
    ROOM: {
        ROOM_FARM_COTTON: 1.0,
        ROOM_FARM_FRUIT: 1.0,
        ...
    }
}
```

### 种族物理属性

```txt
PROPERTIES: {
    HEIGHT: 12,               // 视觉阴影高度
    WIDTH: 13,                // 碰撞箱大小（最大15）
    ADULT_AT_DAY: 0,          // 多少天后成为成年人
    CORPSE_DECAY: true,       // 尸体是否会腐烂
}
```

### 工作速率相关

工作速率受以下因素影响：
1. **种族工作偏好** (`PREFERRED.WORK`): 影响工人选择工作的倾向
2. **种族基础属性** (`BONUS`): 影响工作效率
3. **房间加成** (`BONUS.ROOM`): 特定房间的生产加成
4. **工人数量**: 更多工人一起工作可以提高效率
5. **搬运能力**: 搬运能力也影响效率

**位置**: `sos-src/settlement/room/infra/transport/Gui.java`

```java
"Efficiency is based on the number of workers. Each individual becomes more effective when they work together. Carry capacity also affects efficiency."
```

## 4. 其他静态配置

### 游戏常量

**位置**: `sos-src/init/constant/C.java`

```java
public static final int T_PIXELS = 16;        // 每个tile的像素数
public static final int TILE_SIZE = 64;       // Tile大小 (T_PIXELS*SCALE)
public static final int SCALE = 4;           // 缩放比例
```

### 游戏配置

**位置**: `sos-src/init/constant/Config.java`

#### 战斗配置 (`ConfigBattle`)
```java
public final double MORALE_HOLDOUT;          // 士气坚持值
public final int TRAINING_DEGRADE;           // 训练衰减
public final int MEN_PER_DIVISION;          // 每个师的人数
public final int DIVISIONS_PER_ARMY;        // 每个军队的师数
public final double DAMAGE_REDUCTION;       // 伤害减免
```

#### 定居点配置 (`ConfigSett`)
```java
public final double HAPPINESS_EXPONENT;     // 幸福指数
public final int TOURIST_PER_YEAR_MAX;      // 每年最大游客数
public final int DIMENSION;                  // 地图尺寸 (256-16000)
public final int secondsPerHour;             // 每小时秒数
public final int hoursPerDay;               // 每天小时数
```

#### 世界配置 (`ConfigWorld`)
```java
public final double TRIBUTE;                // 地区贡品数量
public final double TRADE_COST_PER_TILE;    // 每格贸易成本
public final int POPULATION_MAX_CAPITOL;    // 首都最大人口
public final int WORLD_SIZE;                // 世界尺寸
public final double FOREST_AMOUNT;          // 森林数量
public final int CREDITS_PER_WORKDAY;        // 每个工作日的信用点
```

## 5. 如何访问这些配置

### 在 Clojure 代码中访问

```clojure
;; 搬运能力
(import [settlement.room.infra.stockpile ROOM_STOCKPILE])
(ROOM_STOCKPILE/MIN_CARRY)  ; => 8

;; 资源腐烂速率
(import [init.resources RESOURCES])
(def wood (RESOURCES/WOOD))
(.degradeSpeed wood)  ; => 返回腐烂速率 (0-1)

;; 种族配置
(import [game GAME])
(def player (GAME/player))
(def races (.races player))
(def race0 (.get races 0))
;; 访问种族的各种属性...
```

### 配置文件位置

- **资源配置**: `base/data.zip` 中的 `init/resource/` 目录
- **种族配置**: `base/data.zip` 中的 `init/race/` 目录
- **游戏配置**: `base/data.zip` 中的 `init/config/` 目录

## 6. 相关文档

- **种族配置示例**: `doc/res/race/_EXAMPLE.txt`
- **Booster 列表**: `doc/booster/boosters_all.md`
- **游戏代码说明**: `doc/howto/game_code.md`

