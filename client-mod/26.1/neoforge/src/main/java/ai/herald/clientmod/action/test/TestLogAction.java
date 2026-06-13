package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.testing.TestContext;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log a message to MC logger and optionally store in active test context.
 * Returns ok with {logged: true, timestamp}.
 */
public final class TestLogAction implements ActionExecutor {

    private static final Logger LOG = LoggerFactory.getLogger("Herald-Test");

    @Override
    public ActionResult execute(JsonObject params) {
        String level = JsonUtil.requireString(params, "level");
        String message = JsonUtil.requireString(params, "message");
        JsonObject extraData = JsonUtil.getObjectOrNull(params, "data");

        // Log to SLF4J
        switch (level.toLowerCase()) {
            case "warn":
                LOG.warn("[TEST] {}", message);
                break;
            case "error":
                LOG.error("[TEST] {}", message);
                break;
            default:
                LOG.info("[TEST] {}", message);
                break;
        }

        // If there is an active test, store the log as a checkpoint
        // Look for any active test to attach the log to
        String testId = JsonUtil.getStringOrDefault(params, "testId", null);
        if (testId != null) {
            TestContext.TestRun run = TestContext.get(testId);
            if (run != null) {
                JsonObject logData = new JsonObject();
                logData.addProperty("level", level);
                logData.addProperty("message", message);
                if (extraData != null) logData.add("extra", extraData);
                TestContext.addCheckpoint(testId, "log:" + level, logData);
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("logged", true);
        data.addProperty("timestamp", System.currentTimeMillis());
        data.addProperty("level", level);
        return ActionResult.ok(data);
    }
}
