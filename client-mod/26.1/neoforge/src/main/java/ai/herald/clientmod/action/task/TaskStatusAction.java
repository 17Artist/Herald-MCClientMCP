package ai.herald.clientmod.action.task;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.TaskManager;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

public class TaskStatusAction implements ActionExecutor {
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

        return ActionResult.ok(entry.toJson());
    }
}
