package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.EventSubscriptionManager;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

public class EventUnsubscribeAction implements ActionExecutor {
    @Override
    public ActionResult execute(JsonObject params) {
        String subscriptionId = JsonUtil.getStringOrDefault(params, "subscriptionId", null);
        if (subscriptionId == null || subscriptionId.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required param: subscriptionId");
        }

        boolean removed = EventSubscriptionManager.unsubscribe(subscriptionId);
        if (!removed) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Subscription not found: " + subscriptionId);
        }

        JsonObject data = new JsonObject();
        data.addProperty("subscriptionId", subscriptionId);
        data.addProperty("removed", true);
        return ActionResult.ok(data);
    }
}
