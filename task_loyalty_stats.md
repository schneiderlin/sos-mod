# Task: Find Loyalty and Population Stats in SOS Java Source Code

## Description
Find how to get loyalty and population-related statistics data, similar to the food panel UI. Need to explore various stat categories including:

1. **Loyalty** (忠诚度)
2. **Happiness** (快乐度)
3. **Population** (人口)
4. **Distribution** (分配 - ration distribution, decree settings)
5. **Service Facilities** (服务设施 - access to services)
6. **Environment** (环境影响 - pollution, beauty, etc.)
7. **Religion** (宗教)
8. **Profession/Jobs** (职业)
9. **Government** (政府)

## Progress

### 2025-01-19 - Task Created
- Created task file
- Need to explore stats system in Java source code

## Research Plan

### Step 1: Find the Stats System
- Look for `STATS` class in `sos-src\settlement\stats\`
- Find stat collections similar to `StatsFood.java`
- Identify how loyalty and happiness are calculated

### Step 2: Explore Key Files
Based on the food panel exploration, look for:
- `sos-src\settlement\stats\STATS.java` - Main stats class
- `sos-src\settlement\stats\colls\` - Stat collection implementations
- UI files for loyalty/population panels (similar to `UIFood.java`)

### Step 3: Identify Stat Categories
For each category, find:
- The stat class/instance
- Calculation methods
- Data access patterns
- Related decrees/policies

## Specific Categories to Explore

### 1. Loyalty (忠诚度)
- How is loyalty calculated?
- What affects loyalty? (food, housing, protection, etc.)
- Is there a per-population loyalty or overall loyalty?

### 2. Happiness (快乐度)
- How is happiness different from loyalty?
- What factors affect happiness?

### 3. Population (人口)
- Total population count
- Population by race
- Population by class (HCLASS)
- Population changes over time

### 4. Distribution (分配)
- Food rations decree
- How decrees affect consumption
- Per-class/race distribution settings

### 5. Service Facilities (服务设施)
- Access to services (eateries, canteens, taverns, etc.)
- Service coverage calculations
- Wait times, capacity

### 6. Environment (环境影响)
- Pollution levels
- Beauty/decoration
- Heating/cooling
- Impact on population

### 7. Religion (宗教)
- Religion access
- Temple coverage
- Religious satisfaction

### 8. Profession/Jobs (职业)
- Employment rate
- Job satisfaction
- Unemployment

### 9. Government (政府)
- Tax rates
- Laws/policies
- Administration

## Known References from Food Panel

From `UIFood.java` and `StatsFood.java`, we know:
- `STATS.FOOD()` - Food statistics
- `STATS.POP()` - Population statistics
- `STATS.FOOD().FOOD_DAYS` - Days remaining
- `STATS.FOOD().FOOD.decree()` - Food decree (rations)
- `STATS.POP().POP.data(c).get(r, 0)` - Population count by class and race

## Key Classes to Investigate

- `settlement.stats.STATS` - Main stats entry point
- `settlement.stats.colls.*` - Individual stat collections
- `init.type.NEEDS` - Need types (HUNGER, etc.)
- `init.type.HCLASSES` - Humanoid classes
- `init.race.RACES` - Races

## Implementation Goals

After research, create Clojure functions in `play.perception` namespace:
- `loyalty-status` - Overall loyalty information
- `happiness-status` - Happiness metrics
- `population-breakdown` - Detailed population info
- `service-coverage` - Access to services
- `environment-status` - Pollution, beauty, etc.
- `religion-status` - Religious satisfaction
- `employment-status` - Jobs and unemployment
- `government-status` - Taxes and policies

## Notes

- This is a read-only task (exploration only)
- Functions should go in `play.perception` namespace
- Similar approach to food panel exploration
- May need to explore multiple stat collection classes

## Research Findings

### 2025-01-19 - Comprehensive Research Completed

All stat categories have been explored and documented below.

---

## 1. LOYALTY (忠诚度)

### Key Classes
- **File:** `sos-src\settlement\stats\standing\StandingCitizen.java`
- **Access:** `STATS.STANDINGS().CITIZEN()`

### How Loyalty is Calculated

```
Happiness = (Fulfillment / Expectation) × Behavior Multiplier
Loyalty Target = Happiness × LOYALTY booster
Loyalty → gradually adjusts toward Loyalty Target
```

### Data Access Patterns

```java
// Overall loyalty
STATS.STANDINGS().CITIZEN().loyalty.current()

// Target loyalty
STATS.STANDINGS().CITIZEN().loyalty.target()

// Individual's loyalty
STATS.STANDINGS().CITIZEN().loyalty.current(Induvidual)

// Fulfillment (what affects loyalty)
STATS.STANDINGS().CITIZEN().fullfillment.fullfillment(Race)

// Expectation (population-based)
STATS.STANDINGS().CITIZEN().expectation(Race, amount, other)

// Happiness
STATS.STANDINGS().CITIZEN().happiness.hap(Race)
```

### What Affects Loyalty
All stats with `standing()` values contribute to fulfillment:
- Food rations (STATS.FOOD())
- Housing quality (STATS.HOME())
- Service access (STATS.SERVICE())
- Work conditions (STATS.WORK())
- Religious access (STATS.RELIGION())
- Environmental factors (STATS.ENV(), STATS.ACCESS())
- And more...

---

## 2. HAPPINESS (快乐度)

### Key Classes
- **File:** `sos-src\settlement\stats\standing\StandingCitizen.java` (Happiness class)
- **Access:** `STATS.STANDINGS().CITIZEN().happiness`

### How Happiness is Calculated

```java
// From StandingCitizen.Happiness.hap(Race r)
double sup = fullfillment.getD(r);
double exp = expectation.getD(r);
sup /= exp; // Fulfillment divided by expectation
sup *= BOOSTABLES.BEHAVIOUR().HAPPI.get(POP_CL.clP(r, cl));
return sup;
```

### Difference from Loyalty
- **Happiness** = Current satisfaction level (Fulfillment / Expectation)
- **Loyalty** = Long-term alignment with settlement (derived from happiness)
- Happiness changes quickly; loyalty adjusts gradually toward target

---

## 3. POPULATION (人口)

### Key Classes
- **File:** `sos-src\settlement\stats\colls\StatsPopulation.java`
- **Access:** `STATS.POP()`

### Available Population Data

```java
// Total population
STATS.POP().POP.data(null).get(null)

// Population by class (CITIZEN, NOBLE, CHILD, SLAVE)
STATS.POP().POP.data(HCLASSES.CITIZEN()).get(race)

// Population by race
STATS.POP().POP.data(null).get(race)

// Population by type (IMMIGRANT, NATIVE, FORMER_SLAVE)
STATS.POP().pop(HTYPE.SUBJECT())
STATS.POP().pop(race, HTYPE.SUBJECT())

// Historical data
STATS.POP().POP.data(HCLASS).get(race, daysBack)

// Demographics (age distribution)
STATS.POP().demography()

// Age tracking
STATS.POP().age.DAYS
STATS.POP().age.years
STATS.POP().age.lifespan(Induvidual)
STATS.POP().age.isAdult(Induvidual)
```

### Population Changes

```java
// Death tracking by cause
STATS.POP().COUNT.leaves()     // Deaths (CAUSE_LEAVE)
STATS.POP().COUNT.enters()     // Arrivals (CAUSE_ARRIVE)
STATS.POP().COUNT.wrongful     // Wrongful deaths

// Yearly population history
STATS.POP().popYearly()

// Population status
STATS.POP().TRAPPED        // Trapped individuals
STATS.POP().EMMIGRATING    // Emigrating
STATS.POP().MAJORITY       // Race majority ratio
STATS.POP().SLAVES_SELF    // Slaves of same race
STATS.POP().SLAVES_OTHER   // Slaves of other races
```

---

## 4. DISTRIBUTION (分配)

### Key Classes
- **File:** `sos-src\settlement\stats\colls\StatsFood.java`
- **Access:** `STATS.FOOD()`

### Food/Drink Ration Decree

```java
// Food decree (rations per meal)
STATS.FOOD().FOOD.decree()           // Current decree value
STATS.FOOD().FOOD.decree().get(HCLASS, Race)
// Range: 2-5 servings

// Drink decree
STATS.FOOD().DRINK.decree()
STATS.FOOD().DRINK.decree().get(HCLASS, Race)

// Set decree value
STATS.FOOD().FOOD.decree().set(HCLASS, Race, value)
```

### Per-Class/Race Settings

```java
// Permission system for resource access
STATS.FOOD().permission().is(POP_CL.clP(race, class))
STATS.FOOD().permission().set(POP_CL.clP(race, class), boolean)

// Individual rations received
STATS.FOOD().FOOD.indu().getD(Induvidual)
STATS.FOOD().DRINK.indu().getD(Induvidual)
```

### Food Days Calculation

```java
// Days of food available
STATS.FOOD().FOOD_DAYS

// Formula:
// totalFood / sum(population × hungerRate × decreeValue)
```

---

## 5. SERVICE FACILITIES (服务设施)

### Key Classes
- **File:** `sos-src\settlement\stats\service\StatsService.java`
- **Access:** `STATS.SERVICE()`

### Service Access Calculation

```
Total = Access × (0.2 + 0.8 × Upgrade) × (0.2 + 0.8 × Quality) × (0.5 + 0.5 × Proximity)
```

### Available Services

```java
// All service rooms
STATS.SERVICE().ROOMS

// Need to service mapping
STATS.SERVICE().needMap
STATS.SERVICE().needTot[need.index()]

// For specific service:
service.access().data(HCLASS).getD(Race)
service.upgrade().data(HCLASS).getD(Race)
service.quality().data(HCLASS).getD(Race)
service.proximity().data(HCLASS).getD(Race)
```

### Service Types
- **Eateries** - Food distribution (HUNGER need)
- **Canteens** - Food with fuel industry (HUNGER need)
- **Taverns** - Drink service (THIRST need)
- **Hospitals** - Medical care
- And more...

### Service Coverage (City-wide)

```java
// From RoomServiceAccess.cityAccess()
// Weighted average across all population classes and races
double access = service.stats().access().data(c).getD(r)
double pop = STATS.POP().POP.data(c).get(r)
// Returns: sum(access × pop) / sum(pop)
```

### Components Explained

| Component | Range | Description |
|-----------|-------|-------------|
| **Access** | 0-1 | Binary: can reach the service |
| **Upgrade** | 0-1 | (upgrade_level + 1) / (max_upgrades + 1) |
| **Quality** | 0-1 | base × (1 - 0.9 × degradation) |
| **Proximity** | 0-1 | sqrt(1 - (distance - radius/3) / radius) |

---

## 6. ENVIRONMENT (环境影响)

### Key Classes
- **File:** `sos-src\settlement\stats\colls\StatsEnv.java`
- **Access:** `STATS.ENV()`
- **File:** `sos-src\settlement\stats\colls\StatsAccess.java`
- **Access:** `STATS.ACCESS()`

### Environmental Stats

```java
// From StatsEnv
STATS.ENV().BUILDING_PREF    // Building preference (0-15)
STATS.ENV().ROAD_PREF        // Road quality preference (0-255)
STATS.ENV().POOL_PREF        // Pool preference (0-15)
STATS.ENV().CLIMATE          // Climate suitability (0-1)
STATS.ENV().OTHERS           // Racial preference for others (0-1)
STATS.ENV().CANNIBALISM      // Cannibalism history
STATS.ENV().UNBURRIED        // Unburied corpses per person
STATS.ENV().ACCESS_ROAD      // Binary road access

// From StatsAccess (via SettEnv)
STATS.ACCESS().NOISE         // Noise pollution
STATS.ACCESS().LIGHT         // Light levels
STATS.ACCESS().SPACE         // Open space
STATS.ACCESS().WATER_SWEET   // Fresh water access
STATS.ACCESS().WATER_SALT    // Salt water access
STATS.ACCESS().URBAN         // Urbanization level
```

### Monument Access

```java
// From StatsMonuments
monument.access()
monument.amount()       // Number nearby (max: maxEnv())
monument.upgrade()
monument.degrade()      // Degradation status
// Value = (amount / (access × maxEnv())) × (1 - degrade)
```

### Environmental Impact

Environmental factors affect standing calculations (loyalty/happiness):
- High noise, low light, crowded space → negative impact
- Water access, road access → positive impact
- Unburied corpses → significant negative impact

---

## 7. RELIGION (宗教)

### Key Classes
- **File:** `sos-src\settlement\stats\colls\StatsReligion.java`
- **Access:** `STATS.RELIGION()`

### Religious Access

```java
// For specific religion:
religion.access(Religion)           // Binary access
religion.quality(Religion)          // Service quality

// Per-class/race stats
religion.ACCESS.data(HCLASS).getD(Race)
religion.QUALITY.data(HCLASS).getD(Race)
```

### Religious Satisfaction

```java
// Total value calculation
total = access × (0.2 + 0.8 × quality)

// Opposition (inter-religious tension)
religion.opposition()
// Weighted sum of opposition values / total population
```

### Temple Coverage

```java
// Per-religion tracking
religion.followers(Religion)        // Population count
religion.accesses(Religion)         // Access stat array
religion.qualities(Religion)        // Quality stat array

// Conversion boost
religion.conversionCity             // City-wide conversion rate
// Temple: 1.5x boost
// Shrine: 1.25x boost
```

---

## 8. PROFESSION/JOBS (职业)

### Key Classes
- **File:** `sos-src\settlement\stats\colls\StatsWork.java`
- **Access:** `STATS.WORK()`

### Employment Tracking

```java
// Employment status
STATS.WORK().EMPLOYED.stat()        // Employment stat
STATS.WORK().EMPLOYED.data(HCLASS).getD(Race)
// Returns: room/instance where person works

// Work fulfillment (job satisfaction)
STATS.WORK().WORK_FULFILLMENT
STATS.WORK().WORK_FULFILLMENT.data(HCLASS).getD(Race)
// Based on: race.pref().getWork(job_type)

// Work time
STATS.WORK().WORK_TIME              // Hours worked
```

### Employment Rate Calculation

```java
// Workforce calculation
STATS.WORK().workforce()            // Total workforce
STATS.WORK().workforce(Race)        // By race

// Formula:
// Slaves: pop(SLAVE)
// Others: pop(STUDENT) + pop(RECRUIT) + pop(SUBJECT)
// Employment = employed / workforce
```

### Unemployment

```java
// Incapacitated (unable to work)
STATS.WORK().incap.stat()           // Incapacitated count
STATS.WORK().incap.data(HCLASS).getD(Race)

// Unemployment = workforce - employed - incapacitated
```

### Retirement

```java
// Retirement age (decree: 0-100)
STATS.WORK().RET.RETIREMENT_AGE

// Retirement home access
STATS.WORK().RET.RETIREMENT_HOME
// Formula: 0.2 × retiree_ratio + 0.8 × (access × (0.5 + quality × 0.5 × type))
```

### Health Impact

```java
// Job-related health factors
STATS.WORK().health                 // Cumulative health factor
// Applied to population as health booster
```

---

## 9. GOVERNMENT (政府)

### Key Classes
- **File:** `sos-src\settlement\stats\colls\StatsGovern.java`
- **Access:** `STATS.GOVERN()`
- **File:** `sos-src\settlement\stats\law\StatsLaw.java`
- **Access:** `STATS.LAW()`

### Government Stats

```java
// Riches/Wealth
STATS.GOVERN().RICHES
// Formula: credits / (avg_price × 4 × (citizens + nobles))

// Tourism
STATS.GOVERN().tourismFriend        // From friendly races
STATS.GOVERN().tourismEnemy         // From enemy races
```

### Law and Policies

```java
// Equality (racial equality in punishments)
STATS.LAW().EQUALITY
// Disparity between race-specific and general punishment limits

// Law effectiveness
STATS.LAW().rate()
// Components: population size, arrests, punishments, escapes
// Affects BOOSTABLES.CIVICS().LAW and BEHAVIOUR().LAWFULNESS
```

### Punishment System

```java
// 5 main punishments (with multipliers):
exile     (-0.25 multiplier)
prison    (0 multiplier)
execution (2.0 multiplier)
enslaved  (1.0 multiplier)
arena     (2.0 multiplier)

// Per-race punishment limits
processing.dec[RACE.index]
// Sum adjusted to maintain = 1.0
```

### Crime Types

- THEFT (1.0 occurrence weight)
- VANDALISM (1.0 occurrence weight)
- FLASHING (0.2 occurrence weight)
- MURDER (0.05 occurrence weight)
- DISRESPECT (0.02 occurrence weight)

### Prisoner Tracking

```java
// Ex-convict status (decays daily)
STATS.LAW().EX_CON

// Prisoner types: WAR, PLEASURE, + various crimes
processing.prisonerType
```

### Tax System
No explicit tax system found in these files. Wealth is tracked via RICHES stat.

---

## KEY STAT COLLECTIONS SUMMARY

All stat collections are accessed via `STATS.{collection}()`:

| Collection | Purpose |
|------------|---------|
| `STATS.STANDINGS().CITIZEN()` | Loyalty and happiness |
| `STATS.POP()` | Population stats |
| `STATS.FOOD()` | Food/drink rations |
| `STATS.SERVICE()` | Service facilities |
| `STATS.ENV()` | Environmental preferences |
| `STATS.ACCESS()` | Environment access |
| `STATS.RELIGION()` | Religious stats |
| `STATS.WORK()` | Employment and jobs |
| `STATS.GOVERN()` | Government/tourism |
| `STATS.LAW()` | Law and punishment |
| `STATS.HOME()` | Housing quality |
| `STATS.NEEDS()` | Basic needs |

---

## Implementation Notes for Clojure Functions

### Common Patterns

1. **Data access** usually via `.data(HCLASS).getD(Race)` or `.data(HCLASS).get(Race, daysBack)`
2. **Individual access** via `.indu().get(Induvidual)` or `.indu().getD(Induvidual)`
3. **City-wide averages** calculated as weighted sums across population classes/races
4. **Historical data** available with `daysBack` parameter (typically 16-32 day history)

### Standing System

Each STAT has a `standing()` object that affects loyalty:
```java
stat.standing().max(HCLASS, Race)      // Max value
stat.standing().get(HCLASS, Race)      // Current value
stat.standing().get(Induvidual)        // Individual value
```

### Configuration Files

Loyalty configurations loaded from JSON files in `pd.getFolder("loyalty")`:
- Standing definitions with multiplier, exponent, max values
- Per-class and per-race configurations

---

## Next Steps

1. ✅ Search for loyalty/happiness related files in sos-src
2. ✅ Find the main STATS class structure
3. ✅ Identify stat collections for each category
4. ✅ Document data access patterns
5. ✅ Implement Clojure wrapper functions

---

## Implementation Completed (2025-01-19)

### Functions Implemented

All functions have been implemented in `src/play/perception.clj`:

| Function | Description | Status |
|----------|-------------|--------|
| `loyalty-status` | Overall loyalty, target, fulfillment by race | ✅ Working |
| `happiness-status` | Happiness and fulfillment by race | ✅ Working |
| `population-breakdown` | Population by class, race, and type | ✅ Working |
| `distribution-status` | Food/drink rations by class and race | ✅ Implemented |
| `service-coverage` | Service access, upgrade, quality, proximity | ✅ Working |
| `environment-status` | Environmental preferences and pollution | ✅ Implemented |
| `religion-status` | Religious access, quality, followers | ✅ Implemented |
| `employment-status` | Workforce, fulfillment, retirement | ✅ Working |
| `government-status` | Wealth, tourism, law stats | ✅ Implemented |
| `settlement-stats-summary` | Combined summary of all stats | ✅ Implemented |

### Key Implementation Notes

1. **Race Keys**: Use `(.-key race)` instead of `(.name race)` for race string representation
2. **STANDINGS Access**: Use `STANDINGS/CITIZEN()` to get citizen standing data
3. **Stat Access**: Use pattern `(.getD (.data stat) race)` for per-race stat values
4. **HTYPE Constants**: Use `(HTYPES/SUBJECT)`, etc., calling them as functions
5. **StatObject**: For `EMPLOYED` stat, use `(.stat (.-EMPLOYED work-stats))` first
6. **Service Access**: For `StatServiceRoom`, use `(.-name service)` for service name, and method calls like `(.access service)` instead of field access
7. **Need Totals**: Use `NEEDS/ALL()` instead of `NEEDS/TYPES()` (which is not a LIST), and call `(.needTot service-stats need)` method instead of accessing private field

### Example Usage

```clojure
(require '[play.perception :as perception])

;; Get loyalty status
(perception/loyalty-status)
;; => {:overall 1.0, :target 1.0, :fulfillment {...}, :happiness {...}}

;; Get population breakdown
(perception/population-breakdown)
;; => {:by-class {...}, :by-race {...}, :by-type {...}, :total 10, ...}

;; Get employment status
(perception/employment-status)
;; => {:workforce {:total 10, :by-race {...}}, :fulfillment {...}, ...}
```

### Known Limitations

Some stat objects are returned as Java objects rather than primitive values:
- Age stats (AGE_DAYS)
- Demography data
- Status stats (trapped, emigrating, etc.)
- Retirement age and home-access

These can be further processed if needed by calling appropriate methods on the Java objects.
