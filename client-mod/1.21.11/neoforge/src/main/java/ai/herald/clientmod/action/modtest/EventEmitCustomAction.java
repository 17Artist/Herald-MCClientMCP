package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.EventSubscriptionManager;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

public class EventEmitCustomAction implements ActionExecutor {
    @Override
    public ActionResult execute(JsonObject params) {
        String eventType = JsonUtil.getStringOrDefault(params, "eventType", null);
        if (eventType == null || eventType.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required param: eventType");
        }

        JsonObject payload = params.has("payload") && params.get("payload").isJsonObject()
                ? params.getAsJsonObject("payload")
                : new JsonObject();

        EventSubscriptionManager.recordEvent(eventType, payload);

        long timestamp = System.currentTimeMillis();
        JsonObject data = new JsonObject();
        data.addProperty("eventType", eventType);
        data.addProperty("timestamp", timestamp);
        data.addProperty("recorded", true);
        return ActionResult.ok(data);
    }
}
