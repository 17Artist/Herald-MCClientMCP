package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.testing.EventSubscriptionManager;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class EventHistoryAction implements ActionExecutor {
    @Override
    public ActionResult execute(JsonObject params) {
        String type = JsonUtil.getStringOrDefault(params, "type", null);
        int count = params.has("count") ? params.get("count").getAsInt() : 50;

        if (count <= 0) count = 50;
        if (count > 500) count = 500;

        List<JsonObject> events = EventSubscriptionManager.getHistory(type, count);

        JsonObject data = new JsonObject();
        JsonArray arr = new JsonArray();
        for (JsonObject ev : events) {
            arr.add(ev);
        }
        data.add("events", arr);
        data.addProperty("count", events.size());
        if (type != null) data.addProperty("filter", type);
        return ActionResult.ok(data);
    }
}
