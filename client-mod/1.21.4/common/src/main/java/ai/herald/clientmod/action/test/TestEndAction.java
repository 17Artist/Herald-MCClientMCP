package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.TestContext;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * End a test run. Computes duration, returns checkpoint data, clears context.
 * Returns ok with {testId, status, duration, checkpoints}.
 */
public final class TestEndAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String testId = JsonUtil.requireString(params, "testId");
        String status = JsonUtil.requireString(params, "status");
        String message = JsonUtil.getStringOrDefault(params, "message", null);

        TestContext.TestRun run = TestContext.end(testId);
        if (run == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "No active test with id: " + testId);
        }

        long duration = System.currentTimeMillis() - run.startTime;

        JsonArray checkpoints = new JsonArray();
        for (TestContext.Checkpoint cp : run.checkpoints) {
            JsonObject cpObj = new JsonObject();
            cpObj.addProperty("label", cp.label);
            cpObj.addProperty("timestamp", cp.timestamp);
            if (cp.data != null) cpObj.add("data", cp.data);
            checkpoints.add(cpObj);
        }

        JsonObject data = new JsonObject();
        data.addProperty("testId", testId);
        data.addProperty("status", status);
        data.addProperty("duration", duration);
        data.addProperty("description", run.description);
        data.add("checkpoints", checkpoints);
        if (message != null) data.addProperty("message", message);

        return ActionResult.ok(data);
    }
}
