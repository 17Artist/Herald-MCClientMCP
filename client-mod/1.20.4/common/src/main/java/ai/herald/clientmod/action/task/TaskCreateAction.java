package ai.herald.clientmod.action.task;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.TaskManager;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TaskCreateAction implements ActionExecutor {
    @Override
    public ActionResult execute(JsonObject params) {
        String name = JsonUtil.getStringOrDefault(params, "name", null);
        if (name == null || name.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required param: name");
        }

        JsonArray steps = params.has("steps") ? params.getAsJsonArray("steps") : new JsonArray();
        String failurePolicy = JsonUtil.getStringOrDefault(params, "failurePolicy", "abort");

        if (!failurePolicy.equals("abort") && !failurePolicy.equals("continue") && !failurePolicy.equals("retry")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "failurePolicy must be abort, continue, or retry");
        }

        String taskId = TaskManager.create(name, steps, failurePolicy);
        TaskManager.TaskEntry entry = TaskManager.get(taskId);

        JsonObject data = new JsonObject();
        data.addProperty("taskId", taskId);
        data.addProperty("name", name);
        data.addProperty("status", entry.status);
        data.addProperty("totalSteps", steps.size());
        data.addProperty("failurePolicy", failurePolicy);
        return ActionResult.ok(data);
    }
}
