package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.testing.TestContext;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

/**
 * Begin a test run. Stores test context in a static map for later checkpoints and end.
 * Returns ok with {testId, startTime, description}.
 */
public final class TestBeginAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String testId = JsonUtil.requireString(params, "testId");
        String description = JsonUtil.getStringOrDefault(params, "description", "");
        int timeoutMs = JsonUtil.getIntOrDefault(params, "timeout", 0);

        TestContext.begin(testId, description);

        JsonObject data = new JsonObject();
        data.addProperty("testId", testId);
        data.addProperty("startTime", System.currentTimeMillis());
        data.addProperty("description", description);
        if (timeoutMs > 0) {
            data.addProperty("timeout", timeoutMs);
        }
        return ActionResult.ok(data);
    }
}
