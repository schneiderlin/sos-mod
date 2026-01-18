# SOS Wiki IR (Intermediate Representation) Schema

本文档定义了从游戏数据解包出来的中间表示（IR）schema，用于 wiki 编辑。编辑者不需要了解游戏解包的具体实现，只需要引用 IR 中定义的字段即可。

## 概览

IR 包含以下主要数据类型：

1. **Races** (种族) - 游戏中的种族信息
2. **Buildings** (建筑) - 可建造的建筑房间
3. **Technologies** (科技) - 科技树和科技节点
4. **Resources** (资源) - 游戏中的所有资源
5. **Boosters** (加成) - 角色和建筑的各种属性加成
6. **Religions** (宗教) - 游戏中的宗教系统
7. **Types** (类型定义) - 基础类型定义（需求、疾病、气候等）
8. **Production** (生产) - 生产房间和产业信息
9. **Structures** (结构) - 建筑结构类型
10. **Race Relations** (种族关系) - 种族间的关系数据

---

## 1. Races (种族)

**文件**: `races.edn`

种族定义了游戏中不同种族的基本属性、能力、偏好等。

### 字段说明

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `key` | string | 种族唯一标识符 | 例如：ARGONOSH, HUMAN |
| `name` | string | 种族中文名称 | 阿戈诺什人、人类 |
| `names` | string | 种族名称复数形式 | 阿戈诺什人、人类 |
| `possessive` | string | 种族所有格形式 | 阿戈诺什的、人类的 |
| `possessives` | string | 种族所有格复数形式 | 阿戈诺什的、人类的 |
| `index` | number | 种族索引 | 用于内部引用 |
| `playable` | boolean | 是否为可玩种族 | true 表示玩家可选择 |
| `appearance-types` | number | 外观类型数量 | 种族有多少种外观变体 |
| `description` | string | 简短描述 | 种族的简短介绍 |
| `description-long` | string | 详细描述 | 种族的背景故事和详细介绍 |
| `pros` | array | 优点列表 | 种族优势 |
| `cons` | array | 缺点列表 | 种族劣势 |
| `challenge` | string | 挑战说明 | 该种族的游玩挑战 |

### 资源路径

| 字段 | 说明 |
|------|------|
| `icon-path` | 图标路径，例如：`sprites/races/ARGONOSH/icon.png` |
| `sheet-path` | 精灵图集路径 |
| `lay-path` | 躺卧姿态路径 |

### Boosts (加成)

种族对各种属性的加成。

| 字段 | 类型 | 说明 |
|------|------|------|
| `boostable-key` | string | 加成属性的唯一标识 |
| `boostable-name` | string | 加成属性的中文名称 |
| `is-mul` | boolean | 是否为乘法加成 |
| `from` | number | 基础值 |
| `to` | number | 加成后值 |

**常见加成属性分类**：
- `PHYSICS_*` - 体质相关（体重、健康、速度、寿命、耐寒、耐热等）
- `RATES_*` - 消耗率相关（饥饿、虔诚等）
- `ROOM_*` - 建筑效率相关（各种房间的生产效率）
- `BATTLE_*` - 战斗相关（士气、攻击、防御等）
- `BEHAVIOUR_*` - 行为相关（守法、服从度、理智等）
- `RELIGION_*` - 宗教相关（对各教派的倾向）

### Preferences (偏好)

| 字段 | 类型 | 说明 |
|------|------|------|
| `preferred-foods` | array | 首选食物列表 |
| `preferred-drinks` | array | 首选饮料列表 |
| `most-hated-race` | string | 最痛恨的种族 |
| `race-relations` | map | 种族关系映射（种族 -> 关系值） |

### Physics (物理属性)

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `height` | number | 角色高度 | 单位：格子 |
| `hitbox-size` | number | 碰撞箱大小 | |
| `adult-at-day` | number | 成年所需天数 | 角色成长速度 |
| `corpse-decays` | boolean | 尸体是否腐烂 | |
| `sleeps` | boolean | 是否需要睡眠 | |
| `slave-price` | number | 奴隶价格 | 购买该种族奴隶的费用 |
| `raiding-value` | number | 掠夺价值 | 被攻击时的价值 |

### Population (人口属性)

| 字段 | 类型 | 说明 |
|------|------|------|
| `growth` | number | 人口增长率 |
| `max` | number | 最大人口 |
| `immigration-rate` | number | 移民率 |
| `climate-preferences` | map | 气候偏好（寒冷、温暖、炎热） |
| `terrain-preferences` | map | 地形偏好（海洋、淡水、山地、森林、旷野） |

---

## 2. Buildings (建筑)

**文件**: `buildings.edn`

建筑定义了游戏中可以建造的房间和设施。

### 字段说明

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `key` | string | 建筑唯一标识符 | 例如：_STOCKPILE, FARM_FRUIT |
| `name` | string | 建筑中文名称 | 仓库、水果农场 |
| `index` | number | 建筑索引 |
| `type` | string | 建筑类型（可选） | 例如：FARM, MINE, WORKSHOP |
| `type-index` | number | 类型索引 |
| `description` | string | 建筑描述 | 功能和用途说明 |
| `is-production-room` | boolean | 是否为生产房间 | 是否生产资源 |
| `degrade-rate` | number | 劣化速度 | 建筑磨损速度 |
| `has-bonus` | boolean | 是否有加成 | 是否可以被种族加成影响 |

### Category (类别)

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | string | 类别名称 | 例如：农业、军事 |
| `main-name` | string | 主类别名称 | 例如：工业、服务 |
| `room-count` | number | 该类别的建筑数量 |

### Construction (建造)

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `uses-area` | boolean | 是否占用区域 | 建筑是否需要一定空间 |
| `must-be-indoors` | boolean | 是否必须在室内 | 必须建造在室内 |
| `must-be-outdoors` | boolean | 是否必须在室外 | 必须建造在室外 |
| `resources` | array | 建造所需资源 | 每个资源包含 key 和 area-cost |
| `stats-count` | number | 统计数量 | 影响建筑效率的属性数量 |

**建造资源结构**：
```clojure
{:resource-key "_WOOD", :area-cost 0.0}
```

### Industries (产业)

生产建筑的生产链信息。

| 字段 | 类型 | 说明 |
|------|------|------|
| `index` | number | 产业索引 |
| `ai-multiplier` | number | AI 乘数 | AI 使用该建筑时的效率系数 |
| `inputs` | array | 输入资源列表 | 每个输入包含 resource-key 和 rate-per-second |
| `outputs` | array | 输出资源列表 | 每个输出包含 resource-key 和 rate-per-second |

**输入/输出结构**：
```clojure
{:resource-key "GRAIN", :rate-per-second 0.006944444444444444}
```

---

## 3. Technologies (科技)

**文件**: `technologies.edn`

科技定义了科技树的节点和关系。

### 字段说明

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `key` | string | 科技唯一标识符 | 例如：AGRICULTURE_BASE0, ARCHITECTURE_HOME0 |
| `name` | string | 科技中文名称 | 基础种植、地下室 |
| `index` | number | 科技索引 |
| `tree-key` | string | 所属科技树 | 例如：AGRICULTURE, ARCHITECTURE |
| `description` | string | 科技描述 | 作用和效果说明 |
| `level-max` | number | 最大等级 | 该科技可以升级到的最高等级 |
| `cost-total` | number | 总成本 | 升级到最高等级的总花费 |
| `level-cost-inc` | number | 每级成本增量 | 每升一级增加的花费 |
| `ai-amount` | number | AI 数量 | AI 优先级 |
| `color` | object | 颜色信息 | 包含 red, green, blue, hex |

### Costs (成本)

| 字段 | 类型 | 说明 |
|------|------|------|
| `currency-name` | string | 货币名称 | 创新或知识 |
| `currency-index` | number | 货币索引 |
| `amount` | number | 花费数量 |

### Requirements (前置要求)

| 字段 | 类型 | 说明 |
|------|------|------|
| `tech-key` | string | 前置科技标识符 |
| `level` | number | 前置科技所需等级 |

### Trees (科技树)

| 字段 | 类型 | 说明 |
|------|------|------|
| `key` | string | 科技树标识符 |
| `name` | string | 科技树名称 |
| `category` | number | 科技树类别 |
| `color` | object | 科技树颜色 |
| `rows` | number | 科技树行数 |
| `node-grid` | array | 科技节点网格 | 二维数组，每个节点是科技 key |

### Currencies (货币)

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `index` | number | 货币索引 |
| `name` | string | 货币名称 | 创新或知识 |
| `boostable-key` | string | 对应的加成属性标识符 |

---

## 4. Resources (资源)

**文件**: `resources.edn`

资源定义了游戏中的所有可获取和使用的物品。

### 字段说明

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `key` | string | 资源唯一标识符 | 例如：_WOOD, MEAT, METAL |
| `name` | string | 资源中文名称 | 木材、肉、金属 |
| `names` | string | 资源名称复数形式 | 木材、肉、金属 |
| `index` | number | 资源索引 |
| `description` | string | 资源描述 | 用途和获取方式说明 |
| `category` | number | 资源类别 | 0:食物/饮料，1:原材料，2:加工品，3:高级品，4:军事装备 |
| `price-mul` | number | 价格乘数 | 影响交易价格 |
| `price-cap` | number | 价格上限 |
| `edible` | boolean | 是否可食用 | |
| `drinkable` | boolean | 是否可饮用 | |
| `degrade-speed` | number | 腐烂速度 | 资源自然腐坏的速度 |

### 资源路径

| 字段 | 说明 |
|------|------|
| `icon-path` | 图标路径，例如：`sprites/resources/MEAT/icon.png` |
| `sprite-path` | 精灵图路径 |

### Minables (矿藏)

| 字段 | 类型 | 说明 |
|------|------|------|
| `key` | string | 矿藏标识符 |
| `name` | string | 矿藏名称 |
| `resource-key` | string | 产出的资源标识符 |
| `on-every-map` | boolean | 是否每张地图都有 |
| `occurence` | number | 出现频率 |
| `fertility-increase` | number | 肥力加成 | 对地形肥力的影响 |

### Growables (农作物)

| 字段 | 类型 | 说明 |
|------|------|------|
| `key` | string | 农作物标识符 |
| `resource-key` | string | 产出的资源标识符 |
| `seasonal-offset` | number | 季节偏移 |
| `growth-value` | number | 生长值 | 影响产量 |

### Edibles (食物)

| 字段 | 类型 | 说明 |
|------|------|------|
| `key` | string | 食物标识符 |
| `resource-key` | string | 对应的资源标识符 |
| `serve` | boolean | 是否可以上菜 | 是否可以正式食用 |

### Drinkables (饮料)

| 字段 | 类型 | 说明 |
|------|------|------|
| `key` | string | 饮料标识符 |
| `resource-key` | string | 对应的资源标识符 |
| `serve` | boolean | 是否可以供应 |

---

## 5. Boosters (加成/属性)

**文件**: `boosters.edn`

加成系统定义了角色和建筑可以被影响的属性。

### Categories (类别)

| 字段 | 类型 | 说明 |
|------|------|------|
| `prefix` | string | 类别前缀 | 例如：PHYSICS_, BEHAVIOUR_, ROOM_ |
| `name` | string | 类别名称 | 体质、行为、建筑物 |
| `type-mask` | number | 类型掩码 |
| `types` | set | 类型集合 |
| `boostable-count` | number | 该类别的属性数量 |
| `boostable-keys` | array | 属性标识符列表 |
| `boostables` | array | 属性详情列表 |

### Boostable (属性)

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `key` | string | 属性唯一标识符 | 例如：PHYSICS_MASS, BATTLE_MORALE |
| `name` | string | 属性中文名称 | 体重、士气 |
| `index` | number | 属性索引 |
| `description` | string | 属性描述 | 作用和效果说明 |
| `icon-path` | string | 图标路径 |
| `min-value` | number | 最小值 |
| `base-value` | number | 基础值 | 默认值 |
| `types` | set | 适用类型 |
| `category-prefix` | string | 所属类别前缀 |
| `semantic-category` | keyword | 语义类别 | :physics, :battle, :behaviour 等 |
| `type-mask` | number | 类型掩码 |
| `category-name` | string | 类别名称 |

### 主要属性类别

#### PHYSICS_* (体质)
- `PHYSICS_MASS` - 体重
- `PHYSICS_STAMINA` - 耐力
- `PHYSICS_SPEED` - 速度
- `PHYSICS_ACCELERATION` - 加速能力
- `PHYSICS_HEALTH` - 健康
- `PHYSICS_DEATH_AGE` - 寿命
- `PHYSICS_RESISTANCE_HOT` - 耐热
- `PHYSICS_RESISTANCE_COLD` - 耐寒
- `PHYSICS_SOILING` - 脏污

#### BEHAVIOUR_* (行为)
- `BEHAVIOUR_LAWFULNESS` - 守法
- `BEHAVIOUR_SUBMISSION` - 服从度
- `BEHAVIOUR_LOYALTY` - 忠诚度
- `BEHAVIOUR_HAPPINESS` - 幸福度
- `BEHAVIOUR_SANITY` - 理智

#### BATTLE_* (战斗)
- `BATTLE_OFFENCE_SKILL` - 进攻技能
- `BATTLE_DEFENCE_SKILL` - 防御技能
- `BATTLE_MORALE` - 士气
- `BATTLE_BLUNT_ATTACK` - 打击力量
- `BATTLE_BLUNT_DEFENCE` - 打击承受
- `BATTLE_PIERCE_ATTACK` - 穿刺伤害
- `BATTLE_PIERCE_DEFENCE` - 穿刺护甲
- `BATTLE_SLASH_ATTACK` - 劈砍伤害
- `BATTLE_SLASH_DEFENCE` - 劈砍护甲
- `BATTLE_RANGED_BOW` - 技能: 弓

#### ROOM_* (建筑物)
各种房间的生产效率，例如：
- `ROOM_STOCKPILE` - 运载量
- `ROOM_FARM_FRUIT` - 水果农场
- `ROOM_MINE_ORE` - 矿石矿场
- `ROOM_LIBRARY_NORMAL` - 图书馆

#### CIVIC_* (民政)
- `CIVIC_MAINTENANCE` - 耐久
- `CIVIC_SPOILAGE` - 保养
- `CIVIC_ACCIDENT` - 安全
- `CIVIC_INNOVATION` - 创新
- `CIVIC_DIPLOMACY` - 使者点数
- `CIVIC_KNOWLEDGE` - 知识

#### ACTIVITY_* (活动)
- `ACTIVITY_MOURN` - 悼念
- `ACTIVITY_PUNISHMENT` - 惩罚
- `ACTIVITY_JUDGE` - 审判
- `ACTIVITY_SOCIAL` - 社交

#### NOBLE_* (性格)
- `NOBLE_AGRRESSION` - 好战
- `NOBLE_PRIDE` - 骄傲
- `NOBLE_HONOUR` - 荣誉
- `NOBLE_MERCY` - 慈悲
- `NOBLE_COMPETENCE` - 能力
- `NOBLE_TOLERANCE` - 宽容

---

## 6. Religions (宗教)

**文件**: `religions.edn`

宗教系统定义了游戏中的宗教信仰和相关加成。

### 字段说明

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `key` | string | 宗教唯一标识符 | 例如：AMINION, ATHURI, CRATOR, SHMALOR |
| `name` | string | 宗教中文名称 | 阿明农教派 |
| `deity` | string | 神祇名称 | 阿明农 |
| `index` | number | 宗教索引 |
| `description` | string | 宗教描述 | 信仰和效果说明 |
| `inclination` | number | 倾向值 | 信仰该宗教的倾向 |
| `color` | object | 颜色信息 | 包含 red, green, blue, hex |
| `icon-path` | string | 图标路径 |

### Boosts (加成)

宗教提供的加成，结构同 Races 的 boosts。

### Opposition Matrix (对立矩阵)

宗教间的对立关系，矩阵中的值表示对立程度（0=无对立，4=强烈对立）。

```clojure
{"AMINION" {"AMINION" 0.0, "ATHURI" 4.0, "CRATOR" 0.0, "SHMALOR" 0.0}
 "ATHURI" {"AMINION" 4.0, "ATHURI" 0.0, "CRATOR" 0.0, "SHMALOR" 4.0}}
```

---

## 7. Types (类型定义)

**文件**: `types.edn`

定义了游戏中的各种基础类型。

### Needs (需求)

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `key` | string | 需求唯一标识符 | 例如：_HUNGER, _THIRST, _TEMPLE |
| `name` | string | 需求中文名称 | 食物、酒水、崇拜 |
| `index` | number | 需求索引 |
| `basic` | boolean | 是否为基础需求 | 必须满足的基本需求 |
| `event` | number | 事件触发概率 |

**基础需求**：_HUNGER (食物), _THIRST (酒水), _SHOPPING (购物)

### HTypes (人类类型)

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `key` | string | 类型唯一标识符 | 例如：CITIZEN, NOBILITY, SLAVE |
| `name` | string | 类型中文名称 | 公民、贵族、奴隶 |
| `index` | number | 类型索引 |
| `description` | string | 类型描述 | 角色说明 |
| `class-key` | string | 类别标识符 | 例如：CITIZEN, NOBLE, SLAVE, CHILD, OTHER |
| `names` | string | 名称形式 |
| `works` | boolean | 是否工作 | 是否可以分配工作 |
| `player` | boolean | 是否为玩家类型 | 玩家能否控制 |
| `visible` | boolean | 是否可见 | 是否显示在 UI 中 |
| `hostile` | boolean | 是否敌对 | 是否敌对单位 |

**主要类型**：
- `CITIZEN` - 公民
- `RETIREE` - 退休人员
- `RECRUIT` - 新兵
- `STUDENT` - 学生
- `SOLDIER` - 士兵
- `NOBILITY` - 贵族
- `SLAVE` - 奴隶
- `CHILD` - 儿童
- `PRISONER` - 囚犯
- `TOURIST` - 游客
- `ENEMY` - 敌人
- `RIOTER` - 暴徒
- `DERANGED` - 精神错乱者

### Diseases (疾病)

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `key` | string | 疾病唯一标识符 | 例如：BLEEDING_FEVER, PLAGUE |
| `name` | string | 疾病中文名称 | 出血热、黑死疫 |
| `index` | number | 疾病索引 |
| `description` | string | 疾病描述 | 症状和影响说明 |
| `fatality-rate` | number | 死亡率 | 0.0-1.0 |
| `infect-rate` | number | 传染率 | 0.0-1.0 |
| `incubation-days` | number | 潜伏期天数 |
| `length` | number | 病程天数 |
| `regular` | boolean | 是否为常规疾病 | 定期发生的疾病 |
| `epidemic` | boolean | 是否为流行病 | 可能爆发的疾病 |

### Climates (气候)

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `key` | string | 气候唯一标识符 | COLD, TEMPERATE, HOT |
| `name` | string | 气候中文名称 | 寒冷、温暖、炎热 |
| `index` | number | 气候索引 |
| `description` | string | 气候描述 | 对游戏的影响 |
| `season-change` | number | 季节变化 | 0.0-1.0 |
| `temp-cold` | number | 寒冷温度 | |
| `temp-warm` | number | 温暖温度 | |
| `fertility` | number | 肥力 | 土地肥力 |

### Terrains (地形)

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `key` | string | 地形唯一标识符 | OCEAN, WET, MOUNTAIN, FOREST, NONE |
| `name` | string | 地形中文名称 | 海洋、淡水、山地、森林、旷野 |
| `index` | number | 地形索引 |
| `description` | string | 地形描述 | 特征和资源 |
| `world` | boolean | 是否世界地形 | 是否出现在世界地图上 |

### HCclasses (人类阶级)

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `key` | string | 阶级唯一标识符 | NOBLE, CITIZEN, SLAVE, CHILD, OTHER |
| `name` | string | 阶级中文名称 | 贵族、平民、奴隶、儿童 |
| `index` | number | 阶级索引 |
| `description` | string | 阶级描述 | 阶级特征和作用 |
| `names` | string | 名称形式 |
| `player` | boolean | 是否为玩家阶级 | 玩家能否使用 |

### Traits (特质)

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `key` | string | 特质唯一标识符 | COMPETENT, LAZY, HONEST 等 |
| `name` | string | 特质中文名称 | 野心勃勃、懒惰、正直 |
| `index` | number | 特质索引 |
| `description` | string | 特质描述 | 作用和效果 |
| `title` | string | 称号 | 获得的称号 |
| `bios` | array | 传记文本 | 角色背景故事模板 |
| `disables` | array | 互斥特质 | 不能同时拥有的特质 |

**主要特质**：
- `COMPETENT` - 野心勃勃
- `CONSERVATIVE` - 保守
- `CRUEL` - 残忍
- `CUNNING` - 狡猾
- `HONEST` - 正直
- `LAZY` - 懒惰
- `MERCIFUL` - 仁慈
- `MODEST` - 谦逊
- `PROUD` - 骄傲
- `TOLERANT` - 宽容
- `WARRIOR` - 好战
- `WARRIOR_NOT` - 和平主义者

---

## 8. Production (生产)

**文件**: `production.edn`

定义了生产房间和产业信息，结构同 Buildings 的 Industries 部分。

### 字段说明

见 Buildings 的 Industries 部分。

---

## 9. Structures (结构)

**文件**: `structures.edn`

定义了建筑结构类型，影响建筑的耐用性和建造方式。

### 字段说明

| 字段 | 类型 | 说明 | 游戏概念 |
|------|------|------|----------|
| `key` | string | 结构唯一标识符 | _MUD, WOOD, STONE, GRAND |
| `name` | string | 结构中文名称 | 泥制建筑、木制建筑、石制建筑、豪华建筑 |
| `name-wall` | string | 墙体名称 | 泥墙、木墙、石墙、豪华墙 |
| `name-ceiling` | string | 天花板名称 | 泥制天花板、木天花板、石天花板、豪华天花板 |
| `index` | number | 结构索引 |
| `description` | string | 结构描述 | 材质和特性说明 |
| `resource-key` | string | 所需资源标识符 | 例如：_WOOD, _STONE, STONE_CUT |
| `resource-amount` | number | 所需资源数量 |
| `has-resource` | boolean | 是否需要资源 | 泥制建筑不需要资源 |
| `durability` | number | 耐用性 | 建筑的耐用程度 |
| `construct-time` | number | 建造时间 | 建造所需时间 |
| `minimap-color` | object | 小地图颜色 | 包含 red, green, blue |
| `icon-path` | string | 图标路径 |

---

## 10. Race Relations (种族关系)

**文件**: `race-relations.edn`

定义了种族之间的相互关系。

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `from` | string | 种族标识符 |
| `to` | string | 种族标识符 |
| `relation` | number | 关系值 | 通常为 0.0-1.0，1.0 表示非常友好，0.0 表示中立，更低的值表示敌对 |

---

## 公共字段说明

以下字段在多个数据类型中通用：

### 图标和精灵图
- `icon-path` - 图标文件路径，用于 UI 显示
- `sprite-path` / `sheet-path` / `lay-path` - 精灵图文件路径，用于游戏内渲染

### 颜色信息
- `color` - 颜色对象，包含：
  - `red` - 红色分量 (0-255)
  - `green` - 绿色分量 (0-255)
  - `blue` - 蓝色分量 (0-255)
  - `hex` - 十六进制颜色值

### 索引字段
- `index` - 数据在数组中的位置，用于内部引用
- `key` - 唯一标识符，用于跨文件引用

---

## 数据引用示例

### 引用种族信息

```markdown
{{RACES:ARGONOSH:name}} - 阿戈诺什人
{{RACES:ARGONOSH:description-long}} - 详细背景故事
{{RACES:ARGONOSH:boosts}} - 种族加成列表
{{RACES:ARGONOSH:physics:health}} - 健康值
```

### 引用建筑信息

```markdown
{{BUILDINGS:FARM_FRUIT:name}} - 水果农场
{{BUILDINGS:FARM_FRUIT:description}} - 建筑描述
{{BUILDINGS:FARM_FRUIT:industries:0:outputs:0}} - 产出资源
```

### 引用科技信息

```markdown
{{TECHNOLOGIES:AGRICULTURE_BASE0:name}} - 基础种植
{{TECHNOLOGIES:AGRICULTURE_BASE0:description}} - 科技描述
{{TECHNOLOGIES:AGRICULTURE_BASE0:costs}} - 科技成本
```

### 引用资源信息

```markdown
{{RESOURCES:FRUIT:name}} - 水果
{{RESOURCES:FRUIT:description}} - 资源描述
{{RESOURCES:FRUIT:category}} - 资源类别
```

---

## IR 设计原则

1. **解耦**：IR 与游戏内部实现完全解耦，编辑者不需要了解游戏解包机制
2. **统一性**：所有数据使用统一的字段命名规范
3. **可扩展性**：新字段可以方便地添加，不影响现有结构
4. **可读性**：字段名使用英文键值，但提供中文说明，便于理解
5. **完整性**：包含游戏内所有关键数据，满足 wiki 编辑的所有需求

---

## 附录：资源路径模式

所有资源路径遵循以下模式：

- 图标：`sprites/{type}/{key}/icon.png`
- 精灵图：`sprites/{type}/{key}/lay.png`
- 精灵图集：`sprites/{type}/{key}/sheet/`

其中 `{type}` 为数据类型（races, buildings, resources, techs 等），`{key}` 为数据唯一标识符。

---

## JSON Schema

本文档对应的 JSON Schema 文件位于：`wiki-ir-schema.json`

可以使用 JSON Schema 验证工具来检查 IR 数据的结构是否符合预期。

### 验证示例

使用 `ajv-cli` 验证：
```bash
ajv validate -s wiki-ir-schema.json -d your-data.json
```

使用 Python 验证：
```python
import jsonschema

with open('wiki-ir-schema.json') as f:
    schema = json.load(f)

with open('your-data.json') as f:
    data = json.load(f)

jsonschema.validate(instance=data, schema=schema)
```

使用 Node.js 验证：
```javascript
import Ajv from 'ajv';
import schema from './wiki-ir-schema.json' assert {type: 'json'};
import data from './your-data.json' assert {type: 'json'};

const ajv = new Ajv();
const validate = ajv.compile(schema);
const valid = validate(data);

if (!valid) {
  console.error('Validation errors:', validate.errors);
} else {
  console.log('Data is valid!');
}
```
