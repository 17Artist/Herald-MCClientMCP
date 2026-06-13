package ai.herald.clientmod.testing;

import com.google.gson.JsonObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class EventSubscriptionManager {
    private static final Map<String, Subscription> SUBSCRIPTIONS = new ConcurrentHashMap<>();
    private static final Deque<JsonObject> EVENT_HISTORY = new ConcurrentLinkedDeque<>();
    private static final int MAX_HISTORY = 500;

    public static String subscribe(String eventType, String filter) {
        String id = "sub_" + UUID.randomUUID().toString().substring(0, 8);
        SUBSCRIPTIONS.put(id, new Subscription(id, eventType, filter));
        return id;
    }

    public static boolean unsubscribe(String id) {
        return SUBSCRIPTIONS.remove(id) != null;
    }

    public static void recordEvent(String type, JsonObject payload) {
        JsonObject event = new JsonObject();
        event.addProperty("type", type);
        event.addProperty("timestamp", System.currentTimeMillis());
        event.add("payload", payload);
        EVENT_HISTORY.addLast(event);
        while (EVENT_HISTORY.size() > MAX_HISTORY) EVENT_HISTORY.pollFirst();
    }

    public static List<JsonObject> getHistory(String type, int count) {
        List<JsonObject> all = new ArrayList<>(EVENT_HISTORY);
        List<JsonObject> filtered = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && filtered.size() < count; i--) {
            JsonObject ev = all.get(i);
            if (type == null || type.equals(ev.get("type").getAsString())) {
                filtered.add(ev);
            }
        }
        Collections.reverse(filtered);
        return filtered;
    }

    public static Map<String, Subscription> getSubscriptions() { return SUBSCRIPTIONS; }

    public static class Subscription {
        public final String id;
        public final String eventType;
        public final String filter;
        public final long createdAt = System.currentTimeMillis();
        public Subscription(String id, String eventType, String filter) {
            this.id = id; this.eventType = eventType; this.filter = filter;
        }
    }
}
