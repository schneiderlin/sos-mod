# Task: Build Houses Until at Least 10 People Have Homes

## Description
Build houses in the Settlements of Survival (SOS) game until at least 10 people have homes.

## IMPORTANT INSTRUCTIONS FOR NEXT AGENT
**IMPORTANT:** Use the `/sos-play` skill to interact with the game. This skill provides functions to control the SOS game via nREPL using Clojure functions.

The Java source code is in `sos-src` folder. Refer to it when you need to understand game mechanics.

## Progress

### 2025-01-19 - Task Created and Initial Progress
- Created task file
- Connected to nREPL on port 30315
- Checked current status: **10 people, 0 housed** - everyone needs a home!
- Explored Java source code for housing system:
  - `sos-src/settlement/room/home/house/` - Home room types
  - `HomeContructor.java` - Home constructor with sizes: 3x3, 3x5, 5x6
  - `StatsHome.java` - Housing statistics
- Added `create-home` and `create-home-once` functions to `src/repl/tutorial1.clj`
- Built 4 homes but **CRASHED THE GAME** with NullPointerException

### ISSUE - Game Crash
The `create-home` function caused a crash because it didn't properly handle `FurnisherItem` placement. The error:
```
java.lang.NullPointerException: Cannot invoke "FurnisherItem.height()" because "it" is null
```

This happened because homes require furniture items to be placed before construction, similar to how warehouses handle crates. The warehouse creation code in `tutorial1.clj` shows the proper pattern.

### 2026-01-19 - User Clarification and New Attempt
- User clarified: **houses don't need furniture placement** - they're like wells, just choose size
- Connected to nREPL on port 57161
- Checked current status: **0 homes, 0 capacity**
- Attempted to build 4 small homes (3x3) at coordinates:
  - (240, 420), (250, 420), (240, 430), (250, 430)
- **CRASHED THE GAME AGAIN** - RuntimeException: "In use by: home"

### ISSUE - Location Conflict
The game crashed because we tried to build homes where there was already existing construction. Need to:
1. Check if area is clear before building
2. Use `area-is-clear?` function
3. Find valid locations for home construction

### NEXT STEPS
1. Restart the game and nREPL
2. Check `play-plan-building` skill for area-clearance functions
3. Find clear areas to build homes
4. Build homes properly with area checking
5. Continue until 10 people have homes

## Plan

### Step 1: Explore sos-play skill
- Understand available functions for building and game control
- Learn how to check current population and housing status

### Step 2: Check current game state
- Check current population count
- Check how many people currently have homes
- Identify available resources and building options

### Step 3: Build houses
- Build houses as needed
- Monitor progress
- Continue until at least 10 people have homes

## Known References

From previous tasks, the game uses:
- `STATS.POP()` for population statistics
- `STATS.HOME()` for housing statistics
- sos-play skill for game interaction

## Success Criteria
- At least 10 people have homes
- Task file is updated with progress before stopping
