package ai.herald.clientmod.action.composite;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.skill.SkillEngine;
import ai.herald.clientmod.skill.SkillStatus;
import ai.herald.clientmod.skill.SkillTask;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

/**
 * Async: park for {@code ticks} client ticks, then complete the skill task
 * successfully. Cancellation is observed each tick.
 */
public final class WaitAction implements ActionExecutor {

    private static final int MAX_WAIT_TICKS = 20 * 60 * 5; // 5 minutes safety cap

    @Override
    public ActionResult execute(JsonObject params) {
        int ticks = JsonUtil.getIntOrDefault(params, "ticks", 0);
        if (ticks < 0) ticks = 0;
        if (ticks > MAX_WAIT_TICKS) ticks = MAX_WAIT_TICKS;

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("wait");
        final int totalTicks = ticks;
        scheduleTick(task.taskId(), totalTicks, 0);
        return ActionResult.async(task.taskId());
    }

    private void scheduleTick(String taskId, int totalTicks, int elapsed) {
        HeraldClientMod.tickScheduler().schedule(1, () -> {
            SkillTask task = HeraldClientMod.skillEngine().get(taskId);
            if (task == null || task.status() != SkillStatus.RUNNING) return;

            int next = elapsed + 1;
            if (next >= totalTicks) {
                JsonObject data = new JsonObject();
                data.addProperty("waited_ticks", totalTicks);
                HeraldClientMod.skillEngine().complete(taskId, data);
                return;
            }
            scheduleTick(taskId, totalTicks, next);
        });
    }
}
