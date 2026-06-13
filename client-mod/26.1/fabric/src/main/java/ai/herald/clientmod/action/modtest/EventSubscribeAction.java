package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.EventSubscriptionManager;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

public class EventSubscribeAction implements ActionExecutor {
    @Override
    public ActionResult execute(JsonObject params) {
        String eventType = JsonUtil.getStringOrDefault(params, "eventType", null);
        if (eventType == null || eventType.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required param: eventType");
        }

        String filter = JsonUtil.getStringOrDefault(params, "filter", null);

        String subscriptionId = EventSubscriptionManager.subscribe(eventType, filter);

        JsonObject data = new JsonObject();
        data.addProperty("subscriptionId", subscriptionId);
        data.addProperty("eventType", eventType);
        if (filter != null) data.addProperty("filter", filter);
        return ActionResult.ok(data);
    }
}
