package ai.herald.clientmod.events;

import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * A single Herald event published on the {@link EventBus} and streamed to
 * subscribers of {@code /events} as a Server-Sent Event (SSE) frame.
 *
 * <p>Well-known event types include {@code ready}, {@code heartbeat},
 * {@code client_join}, {@code client_disconnect}, {@code chat_received},
 * {@code player_damage}, {@code player_heal}, {@code player_death}.
 */
public final class HeraldEvent {

    private final String type;
    private final JsonObject payload;
    private final long timestamp;

    private HeraldEvent(String type, JsonObject payload, long timestamp) {
        this.type = Objects.requireNonNull(type, "type");
        this.payload = payload != null ? payload : new JsonObject();
        this.timestamp = timestamp;
    }

    public static HeraldEvent of(String type, JsonObject payload) {
        return new HeraldEvent(type, payload, System.currentTimeMillis());
    }

    public String type() { return type; }

    public JsonObject payload() { return payload; }

    public long timestamp() { return timestamp; }

    /** Wrap into the canonical JSON envelope `{type, timestamp, payload}`. */
    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        o.addProperty("timestamp", timestamp);
        o.add("payload", payload);
        return o;
    }

    /**
     * Serialise as an SSE frame. Format:
     * <pre>
     * event: &lt;type&gt;
     * data: &lt;json&gt;
     *
     * </pre>
     * The trailing blank line terminates the frame as required by the spec.
     */
    public String toSseFrame() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("event: ").append(type).append('\n');
        sb.append("data: ").append(toJson().toString()).append('\n');
        sb.append('\n');
        return sb.toString();
    }

    @Override
    public String toString() {
        return "HeraldEvent{" + type + '@' + timestamp + '}';
    }
}
