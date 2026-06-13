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
 * Async: aims at specified yaw/pitch, charges bow for N ticks, then releases.
 */
public final class CombatShootBowAction implements ActionExecutor {

    private static final int DEFAULT_CHARGE_TICKS = 20;

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        float yaw = (float) JsonUtil.requireDouble(params, "yaw");
        float pitch = (float) JsonUtil.requireDouble(params, "pitch");
        int chargeTicks = JsonUtil.getIntOrDefault(params, "chargeTicks", DEFAULT_CHARGE_TICKS);
        if (chargeTicks < 3) chargeTicks = 3;
        if (chargeTicks > 60) chargeTicks = 60;

        // Set look direction
        player.setYRot(yaw);
        player.setXRot(pitch);

        // Start using bow (main hand)
        Minecraft mc = McHelper.mc();
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("combat_shoot_bow", params);

        HeraldClientMod.tickScheduler().schedule(chargeTicks, new BowRelease(task.taskId()));
        return ActionResult.async(task.taskId());
    }

    private static final class BowRelease implements Runnable {
        private final String taskId;

        BowRelease(String taskId) {
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
            data.addProperty("status", "arrow_released");
            engine.complete(taskId, data);
        }
    }
}
