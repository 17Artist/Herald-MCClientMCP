package ai.herald.clientmod.testing;

import com.google.gson.JsonObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Thread-safe in-memory store for state snapshots.
 * Used by snapshot actions to save/compare game state at different points in time.
 */
public final class SnapshotManager {
    private static final Map<String, JsonObject> SNAPSHOTS = new ConcurrentHashMap<>();

    private SnapshotManager() {}

    public static void save(String name, JsonObject snapshot) {
        SNAPSHOTS.put(name, snapshot);
    }

    public static JsonObject get(String name) {
        return SNAPSHOTS.get(name);
    }

    public static boolean exists(String name) {
        return SNAPSHOTS.containsKey(name);
    }

    public static void remove(String name) {
        SNAPSHOTS.remove(name);
    }

    public static void clear() {
        SNAPSHOTS.clear();
    }
}
