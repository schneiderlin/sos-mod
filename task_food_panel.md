# Task: Find Food Panel Data in SOS Java Source Code

## Description
Find how to get data using code equivalent to the UI food panel, which shows:
- Production rate
- Consumption rate
- Storage
- Last how many days (remaining days based on current storage/consumption)

## Progress

### 2025-01-19 - Initial exploration
- Created task file
- Found the food panel UI implementation: `sos-src\view\sett\ui\food\UIFood.java`
- Found the food statistics implementation: `sos-src\settlement\stats\colls\StatsFood.java`
- **Completed implementation in Clojure** - Added food panel functions to `src/play/perception.clj`

### Implementation Status: COMPLETE (with bug fix 2025-01-19)

**Bug fixed:** Fixed NullPointerException in `food-days-remaining` - incorrect method chaining was calling `.getD` on `nil` instead of on the data object.

All food panel functions have been implemented in `src/play/perception.clj`:

1. `all-edible-resources` - Get all edible resources
2. `food-production-rate` - Total food production rate per day
3. `food-consumption-rate` - Total food consumption rate per day (includes production, maintenance, and population)
4. `food-storage` - Total food storage (stockpile + eateries + canteens)
5. `food-days-remaining` - Days of food remaining
6. `food-status` - Combined food status map with all metrics
7. `food-status-by-resource` - Per-resource breakdown
8. `print-food-status` - Human-readable food status display
9. `food-quick-status` - Quick one-line summary

### Key Findings - Data Sources in UIFood.java

#### 1. Production Rate (lines 57-94)
```java
// Get total production rate for all edible resources
double am = 0;
for (ResG rr : RESOURCES.EDI().all()) {
    am += ROOMS().PROD.produced(rr.resource);
}
```
**Key Methods:**
- `RESOURCES.EDI().all()` - Get all edible resources
- `ROOMS().PROD.produced(resource)` - Get production rate for a specific resource

#### 2. Consumption Rate (lines 96-159)
```java
double needed = 0;

// Production consumption (e.g., mills consuming grain)
for (ResG res : RESOURCES.EDI().all()) {
    needed += ROOMS().PROD.consumed(res.resource);
    needed += SETT.MAINTENANCE().estimateGlobal(res.resource);
}

// Population hunger consumption
for (HCLASS c : HCLASSES.ALL()) {
    if (c.player) {
        for (Race r : RACES.all()) {
            double n = NEEDS.TYPES().HUNGER.rate.get(c.get(r))
                * STATS.POP().POP.data(c).get(r, 0)
                * STATS.FOOD().FOOD.decree().get(c, r);
            needed += n;
        }
    }
}
```
**Key Methods:**
- `ROOMS().PROD.consumed(resource)` - Get consumption by production rooms
- `SETT.MAINTENANCE().estimateGlobal(resource)` - Get maintenance consumption
- `NEEDS.TYPES().HUNGER.rate.get()` - Get hunger rate per race/class
- `STATS.POP().POP.data(c).get(r, 0)` - Get population count
- `STATS.FOOD().FOOD.decree().get(c, r)` - Get food decree (rations)

#### 3. Storage (lines 161-237)
```java
int a = 0;

// Stockpile
for (ResG r : RESOURCES.EDI().all()) {
    a += ROOMS().STOCKPILE.tally().amountTotal(r.resource);
}

// Eateries
for (ROOM_EATERY e : SETT.ROOMS().EATERIES) {
    a += e.totalFood();
}

// Canteens
for (ROOM_CANTEEN e : SETT.ROOMS().CANTEENS) {
    a += e.totalFood();
}
```
**Key Methods:**
- `ROOMS().STOCKPILE.tally().amountTotal(resource)` - Get stockpile amount
- `ROOM_EATERY.totalFood()` - Get total food in eatery
- `ROOM_CANTEEN.totalFood()` - Get total food in canteen
- `ROOM_EATERY.amount(resource)` - Get specific resource amount in eatery
- `ROOM_CANTEEN.amount(resource)` - Get specific resource amount in canteen

### 4. Days Remaining - Complete Calculation (StatsFood.java, lines 69-123)

The `FOOD_DAYS` stat calculation is in `StatsFood.java`:

```java
FOOD_DAYS = new STATImp("FOOD_DAYS", init) {
    private double am;
    private int lastT = -1;

    @Override
    public int dataDivider() {
        return 24;
    }

    @Override
    protected int getDD(HCLASS s, Race race) {
        if (GAME.updateI() == lastT)
            return (int) (am*pdivider(s, race, 0));

        lastT = GAME.updateI();

        // 1. Calculate total food available
        double a = 0;
        for (ResG r : RESOURCES.EDI().all()) {
            a += ROOMS().STOCKPILE.tally().amountTotal(r.resource);
        }

        for (ROOM_EATERY e : SETT.ROOMS().EATERIES) {
            a += e.totalFood();
        }

        for (ROOM_CANTEEN e : SETT.ROOMS().CANTEENS) {
            a += e.totalFood();
        }

        // 2. Calculate total needed (consumption)
        double needed = 0;

        for (HCLASS c : HCLASSES.ALL()) {
            if (c.player) {
                for (Race r : RACES.all()) {
                    needed += NEEDS.TYPES().HUNGER.rate.get(c.get(r))
                        * STATS.POP().POP.data(c).get(r, 0)
                        * FOOD.decree().get(c, r);
                }
            }
        }

        // 3. Calculate days remaining
        if (needed == 0)
            am = a > 0 ? 1 : 0;
        else
            am = (a / needed);

        return (int) (am*pdivider(s, race, 0));
    }
};
```

**Key Formula:**
```
FOOD_DAYS = (Total Food Available) / (Total Consumption per day)
Then multiply by dataDivider() = 24 to get the display value
```

**To get actual days remaining:**
```java
STATS.FOOD().FOOD_DAYS.data().getD(null) * STATS.FOOD().FOOD_DAYS.dataDivider()
```

### Clojure Implementation Details

File: `src/play/perception.clj`

**New imports added:**
- `settlement.main.SETT` - Settlement rooms access
- `settlement.stats.STATS` - Food statistics access
- `game.faction.FACTIONS` - Faction data
- `init.type.HCLASSES` - Humanoid classes
- `init.race.RACES` - Race data
- `init.type.NEEDS` - Hunger needs

**Functions implemented:**
- `all-edible-resources` - Get all edible resources
- `food-production-rate` - Total food production rate per day
- `food-consumption-rate` - Total food consumption rate per day (includes production, maintenance, and population)
- `food-storage` - Total food storage (stockpile + eateries + canteens)
- `food-days-remaining` - Days of food remaining
- `food-status` - Combined food status map with all metrics
- `food-status-by-resource` - Per-resource breakdown
- `print-food-status` - Human-readable food status display
- `food-quick-status` - Quick one-line summary

**Usage examples:**
```clojure
(require '[play.perception :as perception])

;; Get complete food status
(perception/food-status)
;; => {:production 12.5, :consumption 8.3, :storage 250, :days-remaining 30.1, :net-rate 4.2}

;; Print human-readable status
(perception/print-food-status)

;; Quick one-line status
(perception/food-quick-status)
;; => "Food: 250 (30.1 days, +4.2/day)"

;; Per-resource breakdown
(perception/food-status-by-resource)
```

### Additional Important Classes/Methods Found

**For individual resource details (RR class, lines 370-507):**
- `SETT.ROOMS().PROD.producers(resource)` - Get all producers of a resource
- `SETT.ROOMS().PROD.consumers(resource)` - Get all consumers of a resource
- `RoomProduction.Source.am()` - Get amount from producer/consumer

**Historical data (lines 257-334):**
- `STATS.FOOD().FOOD_DAYS.data(null).getD(null, historyIndex)` - Get historical food days
- `FACTIONS.player().res().in(type).history(resource).get(ii)` - Historical incoming
- `FACTIONS.player().res().out(type).history(resource).get(ii)` - Historical outgoing
- `STATS.POP().POP.data().get(null)` - Historical population

**StatsFood class stats (lines 41-46):**
- `STATS.FOOD().FOOD_PREFFERENCE` - Food preference stat
- `STATS.FOOD().FOOD_DAYS` - Days remaining stat
- `STATS.FOOD().FOOD` - Food rations stat (with decree)
- `STATS.FOOD().DRINK` - Drink rations stat
- `STATS.FOOD().DRINK_PREFFERENCE` - Drink preference stat
- `STATS.FOOD().STARVATION` - Starvation stat

## Future Enhancements

Optional additions that could be implemented:
- Food preference by race
- Starvation count
- Historical food data tracking
- Producer/consumer breakdown per resource

## Notes
- UI File location: `sos-src\view\sett\ui\food\UIFood.java`
- Stats location: `sos-src\settlement\stats\colls\StatsFood.java`
- Implementation location: `src/play/perception.clj`
- All values are typically double/float rates or integer amounts
- Food resources include: grain, vegetables, meat, fish, fruit, etc.
- Eateries and Canteens store food separately from stockpile
- `dataDivider()` returns 24 for FOOD_DAYS stat
