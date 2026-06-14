package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.skill.SkillEngine;
import ai.herald.clientmod.skill.SkillStatus;
import ai.herald.clientmod.skill.SkillTask;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/**
 * Async: starts using shield (offhand), holds for duration ticks, then releases.
 */
public final class CombatBlockShieldAction implements ActionExecutor {

    private static final int DEFAULT_DURATION = 40;

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int durationTicks = JsonUtil.getIntOrDefault(params, "durationTicks", DEFAULT_DURATION);
        if (durationTicks < 1) durationTicks = 1;
        if (durationTicks > 200) durationTicks = 200;

        // Start blocking with offhand (shield)
        Minecraft mc = McHelper.mc();
        mc.gameMode.useItem(player, InteractionHand.OFF_HAND);

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("combat_block_shield", params);

        HeraldClientMod.tickScheduler().schedule(durationTicks, new ShieldRelease(task.taskId()));
        return ActionResult.async(task.taskId());
    }

    private static final class ShieldRelease implements Runnable {
        private final String taskId;

        ShieldRelease(String taskId) {
            this.taskId = taskId;
        }

        @Override
        public void run() {
            SkillEngine engine = HeraldClientMod.skillEngine();
            SkillTask task = engine.get(taskId);
            if (task == null || task.status() != SkillStatus.RUNNING) return;

            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                engine.fail(taskId, "Player left world");
                return;
            }

            player.releaseUsingItem();

            JsonObject data = new JsonObject();
            data.addProperty("status", "shield_released");
            engine.complete(taskId, data);
        }
    }
}
