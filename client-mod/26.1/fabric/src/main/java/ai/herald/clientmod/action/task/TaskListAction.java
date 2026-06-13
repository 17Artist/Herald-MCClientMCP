package ai.herald.clientmod.action.task;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.testing.TaskManager;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class TaskListAction implements ActionExecutor {
    @Override
    public ActionResult execute(JsonObject params) {
        String statusFilter = JsonUtil.getStringOrDefault(params, "status", null);

        List<TaskManager.TaskEntry> tasks = TaskManager.list(statusFilter);

        JsonObject data = new JsonObject();
        JsonArray arr = new JsonArray();
        for (TaskManager.TaskEntry entry : tasks) {
            arr.add(entry.toJson());
        }
        data.add("tasks", arr);
        data.addProperty("count", tasks.size());
        if (statusFilter != null) {
            data.addProperty("filter", statusFilter);
        }
        return ActionResult.ok(data);
    }
}
