package ai.herald.clientmod.action.task;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.TaskManager;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

public class TaskPauseAction implements ActionExecutor {
    @Override
    public ActionResult execute(JsonObject params) {
        String taskId = JsonUtil.getStringOrDefault(params, "taskId", null);
        if (taskId == null || taskId.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required param: taskId");
        }

        TaskManager.TaskEntry entry = TaskManager.get(taskId);
        if (entry == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Task not found: " + taskId);
        }

        if (!entry.status.equals("running") && !entry.status.equals("pending")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Task cannot be paused from status '" + entry.status + "'. Must be running or pending.");
        }

        entry.status = "paused";
        entry.updatedAt = System.currentTimeMillis();

        JsonObject data = new JsonObject();
        data.addProperty("taskId", taskId);
        data.addProperty("status", "paused");
        return ActionResult.ok(data);
    }
}
