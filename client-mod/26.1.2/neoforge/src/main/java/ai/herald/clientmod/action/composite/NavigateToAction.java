package ai.herald.clientmod.action.composite;

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
import ai.herald.clientmod.util.PathfindingConfig;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Async movement: drives the player toward (x,y,z) by injecting forward
 * input and steering yaw each tick. Auto-jumps when the target is more
 * than {@link PathfindingConfig#AUTO_JUMP_MIN_DY} above the player and
 * the player is on ground. Completes when arrived or on timeout.
 */
public final class NavigateToAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");

        double tx = JsonUtil.requireDouble(params, "x");
        double ty = JsonUtil.requireDouble(params, "y");
        double tz = JsonUtil.requireDouble(params, "z");
        double speed = MathUtil.clamp(JsonUtil.getDoubleOrDefault(params, "speed", 1.0), 0.1, 2.0);
        int timeout = JsonUtil.getIntOrDefault(params, "timeout", PathfindingConfig.DEFAULT_TIMEOUT_TICKS);
        boolean allowJump = JsonUtil.getBooleanOrDefault(params, "allowJump", true);

        double dist = MathUtil.distance3D(tx, ty, tz, player.getX(), player.getY(), player.getZ());
        if (dist > PathfindingConfig.MAX_DISTANCE) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Target too far: " + String.format("%.1f", dist) + " > " + PathfindingConfig.MAX_DISTANCE);
        }

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("navigate_to");

        if (dist < PathfindingConfig.ARRIVAL_THRESHOLD) {
            JsonObject d = new JsonObject();
            d.addProperty("status", "already_at_target");
            engine.complete(task.taskId(), d);
            return ActionResult.async(task.taskId());
        }

        InjectedInput injected = new InjectedInput();
        injected.install(player);

        State st = new State(injected, tx, ty, tz, speed, timeout, allowJump, task.taskId());
        HeraldClientMod.tickScheduler().schedule(1, st);
        return ActionResult.async(task.taskId());
    }

    private static final class State implements Runnable {
        final InjectedInput injected;
        final double tx, ty, tz;
        final double speed;
        final int timeout;
        final boolean allowJump;
        final String taskId;
        int ticksElapsed = 0;

        State(InjectedInput in, double tx, double ty, double tz,
              double speed, int timeout, boolean allowJump, String taskId) {
            this.injected = in;
            this.tx = tx; this.ty = ty; this.tz = tz;
            this.speed = speed; this.timeout = timeout;
            this.allowJump = allowJump; this.taskId = taskId;
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
            if (p == null) {
                cleanup(null);
                engine.fail(taskId, "NOT_IN_GAME: player vanished mid-run");
                return;
            }
            ticksElapsed++;
            double dx = tx - p.getX();
            double dy = ty - p.getY();
            double dz = tz - p.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < PathfindingConfig.ARRIVAL_THRESHOLD) {
                cleanup(p);
                JsonObject d = new JsonObject();
                d.addProperty("status", "arrived");
                d.addProperty("ticks_elapsed", ticksElapsed);
                d.addProperty("final_distance", dist);
                engine.complete(taskId, d);
                return;
            }
            if (ticksElapsed >= timeout) {
                cleanup(p);
                engine.fail(taskId, "Timed out after " + ticksElapsed + " ticks (distance still " + String.format("%.2f", dist) + ")");
                return;
            }

            p.setYRot(MathUtil.yawTo(dx, dz));
            injected.forward = true;
            injected.jumping = allowJump && dy > PathfindingConfig.AUTO_JUMP_MIN_DY && p.onGround();
            injected.sprinting = speed > 1.0;
            p.setSprinting(speed > 1.0);

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
