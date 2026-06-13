package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.skill.SkillEngine;
import ai.herald.clientmod.skill.SkillStatus;
import ai.herald.clientmod.skill.SkillTask;
import ai.herald.clientmod.util.InjectedInput;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.MathUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.PathfindingConfig;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Async: Each tick find entity by id, calculate direction, move toward it.
 * Like navigate_to but target updates each tick (tracks moving entity).
 * Completes when within stopDistance.
 */
public final class NavigateToEntityAction implements ActionExecutor {

    private static final double DEFAULT_STOP_DISTANCE = 2.0;

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        int entityId = JsonUtil.requireInt(params, "entityId");
        double stopDistance = MathUtil.clamp(
                JsonUtil.getDoubleOrDefault(params, "stopDistance", DEFAULT_STOP_DISTANCE),
                0.5, 16.0);

        Entity target = level.getEntity(entityId);
        if (target == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Entity not found: " + entityId);
        }

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("navigate_to_entity", params);

        InjectedInput injected = new InjectedInput();
        injected.install(player);

        HeraldClientMod.tickScheduler().schedule(1,
                new ChaseState(task.taskId(), entityId, stopDistance, injected));
        return ActionResult.async(task.taskId());
    }

    private static final class ChaseState implements Runnable {
        private final String taskId;
        private final int entityId;
        private final double stopDistance;
        private final InjectedInput injected;
        private int ticks = 0;

        ChaseState(String taskId, int entityId, double stopDistance, InjectedInput injected) {
            this.taskId = taskId;
            this.entityId = entityId;
            this.stopDistance = stopDistance;
            this.injected = injected;
        }

        @Override
        public void run() {
            SkillEngine engine = HeraldClientMod.skillEngine();
            SkillTask task = engine.get(taskId);
            if (task == null || task.status() != SkillStatus.RUNNING) {
                cleanup(null);
                return;
            }

            LocalPlayer p = Minecraft.getInstance().player;
            ClientLevel level = Minecraft.getInstance().level;
            if (p == null || level == null) {
                cleanup(null);
                engine.fail(taskId, "Player left world");
                return;
            }
            ticks++;

            Entity target = level.getEntity(entityId);
            if (target == null) {
                cleanup(p);
                engine.fail(taskId, "Target entity no longer exists");
                return;
            }

            double dx = target.getX() - p.getX();
            double dy = target.getY() - p.getY();
            double dz = target.getZ() - p.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist <= stopDistance) {
                cleanup(p);
                JsonObject result = new JsonObject();
                result.addProperty("status", "arrived");
                result.addProperty("ticks_elapsed", ticks);
                result.addProperty("final_distance", dist);
                engine.complete(taskId, result);
                return;
            }
            if (ticks >= PathfindingConfig.DEFAULT_TIMEOUT_TICKS) {
                cleanup(p);
                engine.fail(taskId, "Timed out after " + ticks + " ticks, distance=" +
                        String.format("%.2f", dist));
                return;
            }

            p.setYRot(MathUtil.yawTo(dx, dz));
            injected.forward = true;
            injected.jumping = dy > PathfindingConfig.AUTO_JUMP_MIN_DY && p.onGround();
            injected.sprinting = dist > 8.0;
            p.setSprinting(dist > 8.0);

            HeraldClientMod.tickScheduler().schedule(1, this);
        }

        private void cleanup(LocalPlayer p) {
            try {
                injected.reset();
                if (p != null) injected.uninstall(p); else injected.uninstall();
            } catch (Throwable ignored) {}
            LocalPlayer cur = Minecraft.getInstance().player;
            if (cur != null) cur.setSprinting(false);
        }
    }
}
