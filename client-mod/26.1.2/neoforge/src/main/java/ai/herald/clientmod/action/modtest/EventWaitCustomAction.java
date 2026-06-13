package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.EventSubscriptionManager;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

import java.util.List;

public class EventWaitCustomAction implements ActionExecutor {
    @Override
    public ActionResult execute(JsonObject params) {
        String eventType = JsonUtil.getStringOrDefault(params, "eventType", null);
        if (eventType == null || eventType.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required param: eventType");
        }

        int timeout = params.has("timeout") ? params.get("timeout").getAsInt() : 5000;
        String filter = JsonUtil.getStringOrDefault(params, "filter", null);
        long startTime = System.currentTimeMillis();

        // Check event history for a matching event that occurred recently
        List<JsonObject> events = EventSubscriptionManager.getHistory(eventType, 10);

        // Look for an event within the timeout window from now
        JsonObject matchedEvent = null;
        for (JsonObject ev : events) {
            long evTime = ev.get("timestamp").getAsLong();
            if (evTime >= startTime - timeout) {
                if (filter == null || matchesFilter(ev, filter)) {
                    matchedEvent = ev;
                    break;
                }
            }
        }

        if (matchedEvent != null) {
            JsonObject data = new JsonObject();
            data.addProperty("found", true);
            data.add("event", matchedEvent);
            return ActionResult.ok(data);
        }

        return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "No matching event of type '" + eventType + "' found within timeout");
    }

    private boolean matchesFilter(JsonObject event, String filter) {
        // Simple substring match on event payload string representation
        if (event.has("payload")) {
            return event.get("payload").toString().contains(filter);
        }
        return false;
    }
}
