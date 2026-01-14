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

The `update-once` function executes a callback once in the next update cycle:

```clojure
(update-once (fn [ds] 
  (println "This runs once, then removes itself" ds)))
```

**Implementation details:**
- Generates unique consumer IDs to avoid conflicts
- Automatically removes itself after execution
- Uses try/finally to ensure cleanup even on exceptions

## Standalone Updater (Without Mod System)

If the mod is not loaded, `InstanceScript.update()` is never called by the game, so consumers won't execute. However, we can create a standalone updater that hooks directly into the ScriptEngine.

### Approach

1. **Access ScriptEngine**: Use `GAME.script()` to get the ScriptEngine instance
2. **Create Custom SCRIPT_INSTANCE**: Implement `SCRIPT.SCRIPT_INSTANCE` interface
3. **Inject into ScriptEngine**: Use reflection to add our instance to ScriptEngine's `loads` list

### Limitations

- Requires reflection to access private fields
- More complex than using InstanceScript
- May break if game internals change
- Still requires the mod JAR to be in classpath (for the classes to exist)

### Alternative: Manual Update Polling

If we can't hook into ScriptEngine, we could:
1. Poll game state manually using a separate thread
2. Use game events/hooks if available
3. Hook into other game systems that update regularly

## Troubleshooting

### update-once Not Executing

**Symptoms:**
- `update-once` is called but the function never executes
- No errors, but no output

**Possible Causes:**
1. **Mod not loaded**: Check startup log for mod loading messages
2. **Version mismatch**: Mod version folder doesn't match game version
3. **InstanceScript not instantiated**: Mod's `createInstance()` not called

**Solutions:**
1. Verify mod folder structure and `_Info.txt` version
2. Rebuild mod with correct version in `pom.xml`
3. Run `mvn clean package install` to rebuild and install mod
4. Check mod is enabled in game settings

### InstanceScript Class Not Found

**Symptoms:**
- `ClassNotFoundException` when calling `InstanceScript/addConsumer`

**Causes:**
- Mod JAR not in classpath
- Running game without mod support

**Solutions:**
- Ensure mod JAR is in classpath when running game
- Use standalone updater as fallback

## Best Practices

1. **Always use unique consumer IDs**: Use counters or UUIDs to avoid conflicts
2. **Clean up consumers**: Remove consumers when done to prevent memory leaks
3. **Handle exceptions**: Wrap update callbacks in try/catch to prevent crashes
4. **Check mod loading**: Verify mod is loaded before relying on InstanceScript
5. **Use update-once for one-time operations**: Prevents accidental repeated execution

## References

- `sos-src/script/ScriptEngine.java` - Script management and update loop
- `sos-src/script/SCRIPT.java` - Script interface definitions
- `sos-src/game/GAME.java` - Main game class with ScriptEngine
- `src/main/java/your/mod/InstanceScript.java` - Our mod's script instance
- `src/repl/utils.clj` - Clojure utilities for update mechanism

