package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.PacketCapture;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class PacketQueryLogAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String direction = JsonUtil.getStringOrDefault(params, "direction", null);
        int count = JsonUtil.getIntOrDefault(params, "count", 50);
        String filter = JsonUtil.getStringOrDefault(params, "filter", null);

        if (direction != null && !direction.equals("in") && !direction.equals("out") && !direction.equals("both")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "direction must be 'in', 'out', or 'both'");
        }

        String queryDirection = "both".equals(direction) ? null : direction;
        List<JsonObject> logs = PacketCapture.getLog(queryDirection, count, filter);

        JsonObject data = new JsonObject();
        data.addProperty("count", logs.size());
        data.addProperty("capturing", PacketCapture.isCapturing());

        JsonArray packets = new JsonArray();
        for (JsonObject entry : logs) {
            packets.add(entry);
        }
        data.add("packets", packets);

        return ActionResult.ok(data);
    }
}
