# Update Mechanism and Mod System

This document explains how the game's update loop works, how mods are loaded, and how to hook into the update cycle both with and without the mod system.

## Game Update Loop

The game uses a frame-based update system where various game systems are updated each frame. The update loop is managed by the `GAME` class and calls `update(double ds)` on registered game resources, where `ds` is the delta time (seconds since last update).

### Script Engine Update Cycle

The game has a `ScriptEngine` class that manages all loaded mod scripts:

**Source Reference:**
- `script.ScriptEngine` - `sos-src/script/ScriptEngine.java`
- `game.GAME` - Contains the ScriptEngine instance

The ScriptEngine:
1. Loads scripts from mod JAR files in the `mods/` folder
2. Creates `SCRIPT_INSTANCE` objects for each loaded script
3. Calls `update(double ds)` on each instance every frame (line 165 in ScriptEngine.java)

```java
@Override
protected void update(double ds, Profiler prof) {
    prof.logStart(ScriptEngine.class);
    for (Script s : loads)
        try {
            s.ins.update(ds);  // Calls update on each script instance
        } catch (Exception e) {
            error(s.load, e);
        }
    prof.logEnd(ScriptEngine.class);
}
```

## Mod System

### Mod Loading Process

1. **Mod Discovery**: The game scans the `mods/` folder for mod directories
2. **Version Matching**: Each mod must have a `VXX` folder matching the game's major version (e.g., `V70` for game version 70.x)
3. **Info File**: Each mod must have an `_Info.txt` file with metadata:
   ```
   VERSION: "1.0.0",
   GAME_VERSION_MAJOR: 70,
   GAME_VERSION_MINOR: 23,
   NAME: "Example Mod",
   DESC: "Description",
   AUTHOR: "Author",
   INFO: "Info",
   ```
4. **Script Loading**: The game looks for JAR files in `mods/ModName/V70/script/` and loads classes implementing `SCRIPT`
5. **Instance Creation**: For each `SCRIPT`, the game calls `createInstance()` to get a `SCRIPT_INSTANCE`
6. **Update Registration**: The `SCRIPT_INSTANCE` is added to ScriptEngine's update loop

### Mod Folder Structure

```
mods/
└── Example Mod/
    ├── _Info.txt          # Mod metadata (REQUIRED)
    └── V70/               # Version folder (must match game major version)
        └── script/
            └── Example Mod.jar  # Compiled mod code
```

### Checking if Mod is Loaded

You can check if a mod is loaded by looking at the game startup log:

```
[GAME] (script.ScriptLoad.java:157)
[157] loading script jar 000_Tutorial
      loading script jar 001_Tutorial
[123] SCRIPTS
[139]  -script available: : tutorial
        -script available: : tutorial
```

If your mod is loaded, you'll see it listed here. If not, check:
1. Version folder matches game version (V70 for game 70.x)
2. `_Info.txt` has correct `GAME_VERSION_MAJOR` and `GAME_VERSION_MINOR`
3. Mod is in the correct location: `%APPDATA%/Roaming/songsofsyx/mods/ModName/`

## InstanceScript Update Mechanism

### How InstanceScript Works

`InstanceScript` is a `SCRIPT_INSTANCE` implementation that provides a consumer pattern for update callbacks:

**Source Reference:**
- `your.mod.InstanceScript` - `src/main/java/your/mod/InstanceScript.java`

```java
public final class InstanceScript implements SCRIPT.SCRIPT_INSTANCE {
    private static final Map<String, Consumer<Double>> updaters = new ConcurrentHashMap<>();
    
    public static void addConsumer(String key, Consumer<Double> f) {
        updaters.put(key, f);
    }
    
    public static void removeConsumer(String key) {
        updaters.remove(key);
    }
    
    @Override
    public void update(double ds) {
        updaters.forEach((_k, f) -> {
            f.accept(ds);
        });
    }
}
```

### Using InstanceScript from Clojure

```clojure
(ns my-mod
  (:import [your.mod InstanceScript]))

;; Add a consumer that runs every frame
(InstanceScript/addConsumer "my-updater" 
  (fn [ds] 
    (println "Delta time:" ds)))

;; Remove the consumer
(InstanceScript/removeConsumer "my-updater")
```

### update-once Pattern

The `update-once` function executes a callback once in the next update cycle. It works with or without the mod system:

```clojure
(update-once (fn [ds] 
  (println "This runs once, then removes itself" ds)))
```

**Implementation details:**
- Generates unique consumer IDs to avoid conflicts
- Automatically removes itself after execution
- Uses try/finally to ensure cleanup even on exceptions
- Automatically detects if InstanceScript is available
- Falls back to standalone updater if mod isn't loaded

## Standalone Updater (Without Mod System)

If the mod is not loaded, `InstanceScript.update()` is never called by the game, so consumers won't execute. However, we can create a standalone updater that hooks directly into the ScriptEngine using reflection.

### How It Works

The standalone updater implementation in `src/repl/utils.clj`:

1. **Detects InstanceScript Availability**: Checks if InstanceScript is registered in ScriptEngine's loads
2. **Creates Custom SCRIPT_INSTANCE**: Uses Java Proxy to dynamically implement the interface
3. **Injects into ScriptEngine**: Uses reflection to create a Script object and add it to ScriptEngine's loads list
4. **Automatic Fallback**: If InstanceScript isn't available, automatically uses standalone updater

### Implementation Details

```clojure
;; The system automatically detects if InstanceScript is loaded
(add-updater "my-key" (fn [ds] (println "Update:" ds)))

;; Works with or without mod:
;; - If mod loaded: Uses InstanceScript (faster, cleaner)
;; - If mod not loaded: Uses standalone updater (hooks into ScriptEngine)
```

**Key Functions:**
- `add-updater [key f]` - Hybrid: tries InstanceScript, falls back to standalone
- `add-standalone-updater [key f]` - Direct standalone updater (bypasses InstanceScript)
- `remove-updater [key]` - Remove updater (works with both systems)
- `update-once [f]` - Execute function once (works with both systems)

### How Detection Works

**Important**: `InstanceScript/addConsumer` will always succeed (it just adds to a static Map), but that doesn't mean consumers will execute. The system checks if InstanceScript is actually registered in ScriptEngine:

```clojure
(defn- instancescript-available? []
  "Check if InstanceScript is actually loaded and will be called by the game."
  (some? (find-instancescript-in-loads)))
```

This ensures updaters execute even when the mod isn't loaded.

### Standalone Updater Registration

When a standalone updater is first added:

1. Creates a `SCRIPT_INSTANCE` using Java Proxy
2. Creates a minimal `SCRIPT` implementation
3. Creates a `ScriptLoad` wrapper
4. Creates a `ScriptEngine$Script` object
5. Sets the `ins` field to our custom instance
6. Adds it to ScriptEngine's `loads` list

The standalone script instance handles all interface methods:
- `update(ds)` - Calls all registered standalone updaters
- Other methods (`hover`, `render`, etc.) - No-ops

### Limitations

- Requires reflection to access private fields
- More complex than using InstanceScript
- May break if game internals change (though unlikely)
- Requires mod JAR in classpath (for InstanceScript class to exist, even if not loaded)

## Troubleshooting

### update-once Not Executing

**Symptoms:**
- `update-once` is called but the function never executes
- No errors, but no output

**Possible Causes:**
1. **Mod not loaded and standalone updater failed**: Check if standalone updater registered successfully
2. **ScriptEngine not initialized**: Game may not be fully loaded
3. **Reflection access denied**: Security manager blocking reflection

**Solutions:**
1. Check console for "Standalone updater registered successfully" message
2. Ensure game is fully loaded before calling `update-once`
3. If standalone fails, try loading the mod properly (see below)
4. Use `add-updater` which automatically handles fallback

### Mod Not Loading

**Symptoms:**
- Mod doesn't appear in startup log
- `instancescript-available?` returns false

**Possible Causes:**
1. **Version mismatch**: Mod version folder doesn't match game version
2. **Missing _Info.txt**: Mod folder missing required metadata file
3. **Wrong location**: Mod not in correct mods folder

**Solutions:**
1. Verify mod folder structure and `_Info.txt` version (must match game version)
2. Rebuild mod with correct version in `pom.xml`:
   ```xml
   <game.version.major>70</game.version.major>
   <game.version.minor>23</game.version.minor>
   ```
3. Run `mvn clean package install` to rebuild and install mod
4. Check mod is in: `%APPDATA%/Roaming/songsofsyx/mods/Example Mod/V70/`
5. **Note**: Even if mod doesn't load, standalone updater should still work

### Standalone Updater Registration Fails

**Symptoms:**
- "Could not register standalone script with ScriptEngine" message
- Updaters registered but never execute

**Possible Causes:**
1. ScriptEngine not initialized yet
2. Reflection access issues
3. ScriptLoad creation failed

**Solutions:**
1. Ensure game is fully loaded before registering updaters
2. Check error message for specific failure point
3. Try using `add-updater` which handles errors gracefully
4. If all else fails, ensure mod is loaded properly

## Best Practices

1. **Use `add-updater` or `update-once`**: These automatically handle InstanceScript vs standalone
2. **Always use unique consumer IDs**: Use counters or UUIDs to avoid conflicts
3. **Clean up consumers**: Remove consumers when done to prevent memory leaks
4. **Handle exceptions**: Wrap update callbacks in try/catch to prevent crashes
5. **Don't assume mod is loaded**: The system automatically detects and falls back
6. **Use `update-once` for one-time operations**: Prevents accidental repeated execution
7. **Test without mod**: Verify your code works even if mod isn't loaded

## Quick Start Guide

### Basic Usage

```clojure
(ns my-mod
  (:require [repl.utils :refer [update-once add-updater remove-updater]]))

;; Execute a function once in the next update cycle
(update-once (fn [ds] 
  (println "Delta time:" ds)))

;; Add a persistent updater
(add-updater "my-updater" 
  (fn [ds] 
    (when (> ds 0.1)
      (println "Large delta:" ds))))

;; Remove the updater when done
(remove-updater "my-updater")
```

### How It Works

1. **First call to `add-updater` or `update-once`**:
   - Checks if InstanceScript is available in ScriptEngine
   - If yes: Uses InstanceScript (mod is loaded)
   - If no: Registers standalone updater with ScriptEngine

2. **Subsequent calls**:
   - Uses the same system (InstanceScript or standalone)
   - All updaters execute every game frame

3. **Automatic cleanup**:
   - `update-once` automatically removes itself after execution
   - Manual cleanup with `remove-updater`

### Example: One-Time Operation

```clojure
;; Create a building once when called
(update-once 
  (fn [_ds]
    (create-building 100 100)))
```

### Example: Continuous Monitoring

```clojure
;; Monitor population every frame
(add-updater "population-monitor"
  (fn [_ds]
    (let [pop (get-population)]
      (when (> pop 1000)
        (println "Population exceeded 1000!")))))
```

## References

- `sos-src/script/ScriptEngine.java` - Script management and update loop
- `sos-src/script/SCRIPT.java` - Script interface definitions
- `sos-src/game/GAME.java` - Main game class with ScriptEngine
- `src/main/java/your/mod/InstanceScript.java` - Our mod's script instance
- `src/repl/utils.clj` - Clojure utilities for update mechanism (hybrid updater implementation)

