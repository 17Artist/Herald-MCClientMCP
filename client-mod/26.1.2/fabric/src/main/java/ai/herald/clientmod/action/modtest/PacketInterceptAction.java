package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PacketInterceptAction implements ActionExecutor {

    private static final Map<String, String> INTERCEPT_RULES = new ConcurrentHashMap<>();

    @Override
    public ActionResult execute(JsonObject params) {
        String packetType = JsonUtil.getStringOrDefault(params, "packetType", null);
        String action = JsonUtil.getStringOrDefault(params, "action", null);

        if (packetType == null || packetType.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "packetType is required");
        }
        if (action == null || (!action.equals("log") && !action.equals("drop"))) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "action must be 'log' or 'drop'");
        }

        INTERCEPT_RULES.put(packetType, action);

        JsonObject data = new JsonObject();
        data.addProperty("registered", true);
        data.addProperty("packetType", packetType);
        data.addProperty("action", action);
        data.addProperty("totalRules", INTERCEPT_RULES.size());
        data.addProperty("note", "Interception requires Mixin hooks to be effective");
        return ActionResult.ok(data);
    }

    public static Map<String, String> getInterceptRules() {
        return INTERCEPT_RULES;
    }

    public static void clearRules() {
        INTERCEPT_RULES.clear();
    }
}
