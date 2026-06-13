package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.util.McVersionCompat;
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
 * Async: switches to food slot, starts eating, waits 32 ticks (eat duration), completes.
 */
public final class CombatEatAction implements ActionExecutor {

    private static final int EAT_DURATION_TICKS = 32;

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int slot = JsonUtil.getIntOrDefault(params, "slot", -1);

        // Switch hotbar slot if specified
        if (slot >= 0 && slot <= 8) {
            McVersionCompat.setSelectedSlot(player.getInventory(), slot);
        }

        // Start using item (eating)
        Minecraft mc = McHelper.mc();
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("combat_eat", params);

        HeraldClientMod.tickScheduler().schedule(EAT_DURATION_TICKS, new EatFinish(task.taskId()));
        return ActionResult.async(task.taskId());
    }

    private static final class EatFinish implements Runnable {
        private final String taskId;

        EatFinish(String taskId) {
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

            // Eating should auto-complete on the server side after 32 ticks
            JsonObject data = new JsonObject();
            data.addProperty("status", "eat_complete");
            data.addProperty("health", player.getHealth());
            data.addProperty("food_level", player.getFoodData().getFoodLevel());
            engine.complete(taskId, data);
        }
    }
}
