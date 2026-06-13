package ai.herald.clientmod.testing;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PacketCapture {
    private static final AtomicBoolean CAPTURING = new AtomicBoolean(false);
    private static final Deque<JsonObject> CAPTURED = new ConcurrentLinkedDeque<>();
    private static final int MAX_CAPTURE = 1000;
    private static volatile String captureFilter = null;
    private static volatile String captureDirection = "both";

    public static String start(String direction, String filter) {
        captureDirection = direction != null ? direction : "both";
        captureFilter = filter;
        CAPTURED.clear();
        CAPTURING.set(true);
        return "capture_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static List<JsonObject> stop() {
        CAPTURING.set(false);
        List<JsonObject> result = new ArrayList<>(CAPTURED);
        CAPTURED.clear();
        return result;
    }

    public static boolean isCapturing() { return CAPTURING.get(); }

    public static void record(String direction, String packetName, JsonObject details) {
        if (!CAPTURING.get()) return;
        if (!captureDirection.equals("both") && !captureDirection.equals(direction)) return;
        if (captureFilter != null && !packetName.toLowerCase().contains(captureFilter.toLowerCase())) return;

        JsonObject entry = new JsonObject();
        entry.addProperty("direction", direction);
        entry.addProperty("packet", packetName);
        entry.addProperty("timestamp", System.currentTimeMillis());
        entry.add("details", details);
        CAPTURED.addLast(entry);
        while (CAPTURED.size() > MAX_CAPTURE) CAPTURED.pollFirst();
    }

    public static List<JsonObject> getLog(String direction, int count, String filter) {
        List<JsonObject> result = new ArrayList<>();
        for (JsonObject entry : CAPTURED) {
            if (direction != null && !direction.equals(entry.get("direction").getAsString())) continue;
            if (filter != null && !entry.get("packet").getAsString().toLowerCase().contains(filter.toLowerCase())) continue;
            result.add(entry);
            if (result.size() >= count) break;
        }
        return result;
    }
}
