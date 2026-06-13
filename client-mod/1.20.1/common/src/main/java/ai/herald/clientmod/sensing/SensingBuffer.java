package ai.herald.clientmod.sensing;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Ring buffer that stores recent particle/sound events for query.
 */
public final class SensingBuffer {
    private static final int MAX_SIZE = 200;
    private static final Deque<JsonObject> PARTICLES = new ConcurrentLinkedDeque<>();
    private static final Deque<JsonObject> SOUNDS = new ConcurrentLinkedDeque<>();

    public static void addParticle(JsonObject event) {
        PARTICLES.addLast(event);
        while (PARTICLES.size() > MAX_SIZE) PARTICLES.pollFirst();
    }

    public static void addSound(JsonObject event) {
        SOUNDS.addLast(event);
        while (SOUNDS.size() > MAX_SIZE) SOUNDS.pollFirst();
    }

    public static List<JsonObject> getRecentParticles(int count) {
        List<JsonObject> list = new ArrayList<>(PARTICLES);
        int from = Math.max(0, list.size() - count);
        return list.subList(from, list.size());
    }

    public static List<JsonObject> getRecentSounds(int count) {
        List<JsonObject> list = new ArrayList<>(SOUNDS);
        int from = Math.max(0, list.size() - count);
        return list.subList(from, list.size());
    }

    public static void clear() {
        PARTICLES.clear();
        SOUNDS.clear();
    }
}
