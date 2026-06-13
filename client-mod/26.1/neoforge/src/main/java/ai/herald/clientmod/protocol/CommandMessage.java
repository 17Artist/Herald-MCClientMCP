package ai.herald.clientmod.protocol;

import com.google.gson.JsonObject;

/**
 * Inbound command from the Herald controller.
 *
 * <p>Matches the request envelope defined in {@code S2_CLIENT_MOD_TECH.md §3.2}.
 * The HTTP handler is responsible for synthesising the {@code id} when the
 * caller does not supply one (we always do — it is the idempotency key for
 * skill lookup).
 */
public final class CommandMessage {

    private final String id;
    private final String action;
    private final JsonObject params;
    private final long delayMs;

    public CommandMessage(String id, String action, JsonObject params, long delayMs) {
        this.id = id;
        this.action = action;
        this.params = params != null ? params : new JsonObject();
        this.delayMs = Math.max(0L, delayMs);
    }

    public CommandMessage(String id, String action, JsonObject params) {
        this(id, action, params, 0L);
    }

    public String id() { return id; }
    public String action() { return action; }
    public JsonObject params() { return params; }
    public long delayMs() { return delayMs; }

    @Override
    public String toString() {
        return "CommandMessage{id=" + id + ", action=" + action + ", delayMs=" + delayMs + "}";
    }
}
