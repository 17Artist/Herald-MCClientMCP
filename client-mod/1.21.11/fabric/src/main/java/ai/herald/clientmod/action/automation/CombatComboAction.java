package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.skill.SkillEngine;
import ai.herald.clientmod.skill.SkillStatus;
import ai.herald.clientmod.skill.SkillTask;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Async: attacks an entity N times with attack-cooldown delay between hits (10 ticks).
 */
public final class CombatComboAction implements ActionExecutor {

    private static final int ATTACK_COOLDOWN_TICKS = 10;

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        MultiPlayerGameMode gm = mc.gameMode;
        if (player == null || level == null || gm == null) return McHelper.notInGame();

        int entityId = JsonUtil.requireInt(params, "entityId");
        int hits = JsonUtil.getIntOrDefault(params, "hits", 3);
        if (hits < 1) hits = 1;
        if (hits > 50) hits = 50;

        Entity target = level.getEntity(entityId);
        if (target == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No entity with id " + entityId);
        }

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("combat_combo", params);

        HeraldClientMod.tickScheduler().schedule(1, new ComboRunner(task.taskId(), entityId, hits, 0));
        return ActionResult.async(task.taskId());
    }

    private static final class ComboRunner implements Runnable {
        private final String taskId;
        private final int entityId;
        private final int totalHits;
        private int hitsLanded;

        ComboRunner(String taskId, int entityId, int totalHits, int hitsLanded) {
            this.taskId = taskId;
            this.entityId = entityId;
            this.totalHits = totalHits;
            this.hitsLanded = hitsLanded;
        }

        @Override
        public void run() {
            SkillEngine engine = HeraldClientMod.skillEngine();
            SkillTask task = engine.get(taskId);
            if (task == null || task.status() != SkillStatus.RUNNING) return;

            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            ClientLevel level = mc.level;
            MultiPlayerGameMode gm = mc.gameMode;
            if (player == null || level == null || gm == null) {
                engine.fail(taskId, "Player left world");
                return;
            }

            Entity target = level.getEntity(entityId);
            if (target == null || !target.isAlive()) {
                JsonObject data = new JsonObject();
                data.addProperty("hits_landed", hitsLanded);
                data.addProperty("reason", "target_gone");
                engine.complete(taskId, data);
                return;
            }

            // Look at target
            double dx = target.getX() - player.getX();
            double dy = target.getEyeY() - player.getEyeY();
            double dz = target.getZ() - player.getZ();
            double hDist = Math.sqrt(dx * dx + dz * dz);
            player.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
            player.setXRot((float) (-Math.toDegrees(Math.atan2(dy, hDist))));

            // Attack
            gm.attack(player, target);
            player.swing(InteractionHand.MAIN_HAND);
            hitsLanded++;

            if (hitsLanded >= totalHits) {
                JsonObject data = new JsonObject();
                data.addProperty("hits_landed", hitsLanded);
                data.addProperty("reason", "combo_complete");
                if (target instanceof LivingEntity le) {
                    data.addProperty("target_health", le.getHealth());
                }
                engine.complete(taskId, data);
            } else {
                HeraldClientMod.tickScheduler().schedule(ATTACK_COOLDOWN_TICKS, this);
            }
        }
    }
}
