package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.skill.SkillEngine;
import ai.herald.clientmod.skill.SkillStatus;
import ai.herald.clientmod.skill.SkillTask;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.MathUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Async: If player can fly (creative/spectator), enable flying and move
 * toward target each tick by setting deltaMovement. Completes when within
 * 1 block of target.
 */
public final class FlyToAction implements ActionExecutor {

    private static final double DEFAULT_SPEED = 0.5;
    private static final double ARRIVAL_DISTANCE = 1.0;
    private static final int TIMEOUT_TICKS = 600;

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        double tx = JsonUtil.requireDouble(params, "x");
        double ty = JsonUtil.requireDouble(params, "y");
        double tz = JsonUtil.requireDouble(params, "z");
        double speed = MathUtil.clamp(
                JsonUtil.getDoubleOrDefault(params, "speed", DEFAULT_SPEED), 0.1, 2.0);

        // Check if player can fly: abilities.mayfly OR creative/spectator mode
        boolean canFly = player.getAbilities().mayfly;
        if (!canFly) {
            Minecraft mc = McHelper.mc();
            GameType mode = null;
            // In singleplayer, read authoritative game mode from integrated server
            if (mc.getSingleplayerServer() != null) {
                ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(player.getUUID());
                if (sp != null) {
                    mode = sp.gameMode.getGameModeForPlayer();
                }
            }
            if (mode == null && mc.gameMode != null) {
                mode = mc.gameMode.getPlayerMode();
            }
            canFly = (mode == GameType.CREATIVE || mode == GameType.SPECTATOR);
            if (canFly) {
                // Force-sync abilities so flying actually works
                player.getAbilities().mayfly = true;
            }
        }
        if (!canFly) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Player cannot fly (not in creative/spectator)");
        }

        player.getAbilities().flying = true;
        player.onUpdateAbilities();

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("fly_to", params);

        HeraldClientMod.tickScheduler().schedule(1,
                new FlyState(task.taskId(), tx, ty, tz, speed));
        return ActionResult.async(task.taskId());
    }

    private static final class FlyState implements Runnable {
        private final String taskId;
        private final double tx, ty, tz;
        private final double speed;
        private int ticks = 0;

        FlyState(String taskId, double tx, double ty, double tz, double speed) {
            this.taskId = taskId;
            this.tx = tx;
            this.ty = ty;
            this.tz = tz;
            this.speed = speed;
        }

        @Override
        public void run() {
            SkillEngine engine = HeraldClientMod.skillEngine();
            SkillTask task = engine.get(taskId);
            if (task == null || task.status() != SkillStatus.RUNNING) return;

            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null) {
                engine.fail(taskId, "Player left world");
                return;
            }
            ticks++;

            double dx = tx - p.getX();
            double dy = ty - p.getY();
            double dz = tz - p.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < ARRIVAL_DISTANCE) {
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
                JsonObject result = new JsonObject();
                result.addProperty("status", "arrived");
                result.addProperty("ticks_elapsed", ticks);
                result.addProperty("final_distance", dist);
                engine.complete(taskId, result);
                return;
            }
            if (ticks >= TIMEOUT_TICKS) {
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
                engine.fail(taskId, "Timed out after " + ticks + " ticks, distance=" +
                        String.format("%.2f", dist));
                return;
            }

            // Normalize direction and set velocity
            double factor = speed / dist;
            p.setDeltaMovement(new Vec3(dx * factor, dy * factor, dz * factor));

            HeraldClientMod.tickScheduler().schedule(1, this);
        }
    }
}
