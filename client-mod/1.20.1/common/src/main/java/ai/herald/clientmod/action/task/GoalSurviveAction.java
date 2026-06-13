package ai.herald.clientmod.action.task;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.TaskManager;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class GoalSurviveAction implements ActionExecutor {
    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in game");
        }

        if (!params.has("durationSecs")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required param: durationSecs");
        }

        int durationSecs = params.get("durationSecs").getAsInt();
        if (durationSecs <= 0) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "durationSecs must be positive");
        }

        // Create an async task for survival tracking
        JsonArray steps = new JsonArray();
        steps.add("survive_" + durationSecs + "s");
        String taskId = TaskManager.create("survive", steps, "abort");

        TaskManager.TaskEntry entry = TaskManager.get(taskId);
        entry.status = "running";
        entry.updatedAt = System.currentTimeMillis();

        return ActionResult.async(taskId);
    }
}
