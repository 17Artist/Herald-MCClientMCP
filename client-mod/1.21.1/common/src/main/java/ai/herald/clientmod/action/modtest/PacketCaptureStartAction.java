package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.PacketCapture;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

public class PacketCaptureStartAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String direction = JsonUtil.getStringOrDefault(params, "direction", "both");
        String filter = JsonUtil.getStringOrDefault(params, "filter", null);

        if (!direction.equals("in") && !direction.equals("out") && !direction.equals("both")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "direction must be 'in', 'out', or 'both'");
        }

        if (PacketCapture.isCapturing()) {
            PacketCapture.stop();
        }

        String captureId = PacketCapture.start(direction, filter);

        JsonObject data = new JsonObject();
        data.addProperty("captureId", captureId);
        data.addProperty("direction", direction);
        if (filter != null) {
            data.addProperty("filter", filter);
        }
        data.addProperty("status", "capturing");
        return ActionResult.ok(data);
    }
}
