package ai.herald.clientmod.action.test;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.events.ChatHistoryBuffer;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Sync check: look for a matching event in the EventBus history.
 * Returns ok with event data if a matching event is found since invocation,
 * or error ASSERTION_FAILED if not found.
 *
 * NOTE: Currently uses chat history as a proxy for events.
 * A proper event history buffer can be added later.
 */
public final class WaitEventAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String eventType = JsonUtil.requireString(params, "eventType");
        long since = System.currentTimeMillis() - JsonUtil.getIntOrDefault(params, "timeout", 5000);

        // For now, check chat history as the primary event source
        List<ChatHistoryBuffer.Entry> messages = HeraldClientMod.chatHistory().snapshot(100);

        for (ChatHistoryBuffer.Entry entry : messages) {
            if (entry.timestampMs < since) continue;
            if (matchesEvent(eventType, entry.text)) {
                JsonObject data = new JsonObject();
                data.addProperty("eventType", eventType);
                data.addProperty("found", true);
                data.addProperty("text", entry.text);
                data.addProperty("timestamp_ms", entry.timestampMs);
                return ActionResult.ok(data);
            }
        }

        JsonObject state = new JsonObject();
        state.addProperty("eventType", eventType);
        state.addProperty("found", false);
        state.addProperty("checked_since_ms", since);
        return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Event not found: " + eventType + " | " + state);
    }

    private boolean matchesEvent(String eventType, String text) {
        // Simple matching: event type is used as a substring filter
        return text.toLowerCase().contains(eventType.toLowerCase());
    }
}
