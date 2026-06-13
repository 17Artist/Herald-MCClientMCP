package ai.herald.clientmod.events;

import ai.herald.clientmod.platform.HeraldEvents;
import ai.herald.clientmod.util.HeraldLogger;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import org.slf4j.Logger;

/**
 * Bridges loader-supplied game events + per-tick polling into
 * {@link EventBus} publications consumed by {@code GET /events} (SSE).
 *
 * <p>Event types currently published:
 * <ul>
 *   <li>{@code client_join}        — local player joined a world</li>
 *   <li>{@code client_disconnect}  — local player left a world</li>
 *   <li>{@code chat_received}      — incoming chat / system message</li>
 *   <li>{@code player_damage}      — health decreased since last tick</li>
 *   <li>{@code player_heal}        — health increased since last tick</li>
 *   <li>{@code player_death}       — local player health reached 0</li>
 *   <li>{@code heartbeat}          — emitted by {@code EventsHandler} every 15 s</li>
 * </ul>
 */
public final class GameEventBridge {

    private static final Logger LOG = HeraldLogger.of(GameEventBridge.class);

    private final EventBus bus;
    private final ChatHistoryBuffer chatHistory;
    private float lastHealth = -1f;
    private boolean wasDead = false;

    public GameEventBridge(EventBus bus, ChatHistoryBuffer chatHistory) {
        this.bus = bus;
        this.chatHistory = chatHistory;
    }

    /** Hooks loader event registrations. Called once on init. */
    public void install() {
        HeraldEvents.registerJoin(this::onJoin);
        HeraldEvents.registerDisconnect(this::onDisconnect);
        HeraldEvents.registerChatReceived(this::onChat);
        HeraldEvents.registerClientTick(this::onClientTick);
        LOG.info("GameEventBridge installed");
    }

    private void onJoin() {
        bus.publish(HeraldEvent.of("client_join", new JsonObject()));
        lastHealth = -1f;
        wasDead = false;
    }

    private void onDisconnect() {
        bus.publish(HeraldEvent.of("client_disconnect", new JsonObject()));
        lastHealth = -1f;
        wasDead = false;
    }

    private void onChat(String message) {
        if (message == null) return;
        chatHistory.append(message);
        JsonObject payload = new JsonObject();
        payload.addProperty("text", message);
        bus.publish(HeraldEvent.of("chat_received", payload));
    }

    /** Polled per client tick: detect health deltas + death transitions. */
    private void onClientTick() {
        LocalPlayer player = McHelper.player();
        if (player == null) {
            lastHealth = -1f;
            wasDead = false;
            return;
        }
        float h = player.getHealth();
        if (lastHealth < 0f) {
            lastHealth = h;
            wasDead = h <= 0f;
            return;
        }
        if (h < lastHealth) {
            JsonObject p = new JsonObject();
            p.addProperty("amount", lastHealth - h);
            p.addProperty("health", h);
            p.addProperty("max_health", player.getMaxHealth());
            bus.publish(HeraldEvent.of("player_damage", p));
        } else if (h > lastHealth) {
            JsonObject p = new JsonObject();
            p.addProperty("amount", h - lastHealth);
            p.addProperty("health", h);
            p.addProperty("max_health", player.getMaxHealth());
            bus.publish(HeraldEvent.of("player_heal", p));
        }
        if (h <= 0f && !wasDead) {
            JsonObject p = new JsonObject();
            p.addProperty("health", h);
            bus.publish(HeraldEvent.of("player_death", p));
            wasDead = true;
        } else if (h > 0f && wasDead) {
            wasDead = false;
        }
        lastHealth = h;
    }
}
