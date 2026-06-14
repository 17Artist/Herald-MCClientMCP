package ai.herald.clientmod.action.task;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.TaskManager;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

public class TaskCancelAction implements ActionExecutor {
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

        if (entry.status.equals("completed") || entry.status.equals("cancelled")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Task already " + entry.status);
        }

        entry.status = "cancelled";
        entry.updatedAt = System.currentTimeMillis();

        JsonObject data = new JsonObject();
        data.addProperty("taskId", taskId);
        data.addProperty("status", "cancelled");
        return ActionResult.ok(data);
    }
}
