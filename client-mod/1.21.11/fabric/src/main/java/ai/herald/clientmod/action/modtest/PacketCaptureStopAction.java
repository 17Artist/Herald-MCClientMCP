package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.testing.PacketCapture;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class PacketCaptureStopAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        if (!PacketCapture.isCapturing()) {
            return ActionResult.error(ai.herald.clientmod.protocol.ErrorCode.INVALID_PARAMS, "No active capture session");
        }

        List<JsonObject> captured = PacketCapture.stop();

        JsonObject data = new JsonObject();
        data.addProperty("status", "stopped");
        data.addProperty("totalCaptured", captured.size());

        JsonArray lastPackets = new JsonArray();
        int start = Math.max(0, captured.size() - 10);
        for (int i = start; i < captured.size(); i++) {
            lastPackets.add(captured.get(i));
        }
        data.add("lastPackets", lastPackets);

        JsonObject summary = new JsonObject();
        int inCount = 0, outCount = 0;
        for (JsonObject entry : captured) {
            if ("in".equals(entry.get("direction").getAsString())) inCount++;
            else outCount++;
        }
        summary.addProperty("inbound", inCount);
        summary.addProperty("outbound", outCount);
        data.add("summary", summary);

        return ActionResult.ok(data);
    }
}
