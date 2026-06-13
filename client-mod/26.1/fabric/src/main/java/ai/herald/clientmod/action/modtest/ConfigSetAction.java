package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

public class ConfigSetAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String modId = JsonUtil.getStringOrDefault(params, "modId", null);
        String key = JsonUtil.getStringOrDefault(params, "key", null);
        String value = JsonUtil.getStringOrDefault(params, "value", null);

        if (modId == null || modId.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "modId is required");
        }
        if (key == null || key.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "key is required");
        }
        if (value == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "value is required");
        }

        JsonObject data = new JsonObject();
        data.addProperty("modId", modId);
        data.addProperty("key", key);
        data.addProperty("value", value);
        data.addProperty("note", "Config modification requires mod-specific API. Use config_reload after manual edit.");
        return ActionResult.ok(data);
    }
}
