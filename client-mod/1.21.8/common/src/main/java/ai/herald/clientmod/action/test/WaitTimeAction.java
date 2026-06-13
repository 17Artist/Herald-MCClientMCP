package ai.herald.clientmod.action.test;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.skill.SkillEngine;
import ai.herald.clientmod.skill.SkillStatus;
import ai.herald.clientmod.skill.SkillTask;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

/**
 * Async: wait for the specified number of milliseconds (converted to ticks at 50ms/tick).
 * Uses the SkillEngine + TickScheduler pattern.
 */
public final class WaitTimeAction implements ActionExecutor {

    private static final int MAX_MS = 5 * 60 * 1000; // 5 minutes cap

    @Override
    public ActionResult execute(JsonObject params) {
        int ms = JsonUtil.requireInt(params, "ms");
        if (ms < 0) ms = 0;
        if (ms > MAX_MS) ms = MAX_MS;

        int ticks = Math.max(1, ms / 50);

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("wait_time", params);
        final int totalTicks = ticks;
        final int requestedMs = ms;
        scheduleTick(task.taskId(), totalTicks, 0, requestedMs);
        return ActionResult.async(task.taskId());
    }

    private void scheduleTick(String taskId, int totalTicks, int elapsed, int requestedMs) {
        HeraldClientMod.tickScheduler().schedule(1, () -> {
            SkillTask task = HeraldClientMod.skillEngine().get(taskId);
            if (task == null || task.status() != SkillStatus.RUNNING) return;

            int next = elapsed + 1;
            if (next >= totalTicks) {
                JsonObject data = new JsonObject();
                data.addProperty("waited_ms", requestedMs);
                data.addProperty("waited_ticks", totalTicks);
                HeraldClientMod.skillEngine().complete(taskId, data);
                return;
            }
            scheduleTick(taskId, totalTicks, next, requestedMs);
        });
    }
}
