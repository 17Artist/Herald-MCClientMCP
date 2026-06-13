package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.TestContext;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

/**
 * Add a checkpoint to an active test run.
 * Returns ok with {testId, label, timestamp, checkpointIndex}.
 */
public final class TestCheckpointAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String testId = JsonUtil.requireString(params, "testId");
        String label = JsonUtil.requireString(params, "label");
        JsonObject extraData = JsonUtil.getObjectOrNull(params, "data");

        TestContext.TestRun run = TestContext.get(testId);
        if (run == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "No active test with id: " + testId);
        }

        TestContext.addCheckpoint(testId, label, extraData);

        JsonObject data = new JsonObject();
        data.addProperty("testId", testId);
        data.addProperty("label", label);
        data.addProperty("timestamp", System.currentTimeMillis());
        data.addProperty("checkpointIndex", run.checkpointCount() - 1);
        return ActionResult.ok(data);
    }
}
