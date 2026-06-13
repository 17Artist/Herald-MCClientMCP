package ai.herald.clientmod.action.movement;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.scheduler.TickScheduler;
import ai.herald.clientmod.util.InjectedInput;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.MathUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Walk to a target by injecting forward input + steering yaw each tick.
 * Lets MC's physics handle gravity / collisions / step-up.
 *
 * <p>Port of {@code BlackBoxPro fabric/action/movement/PlayerMoveAction.kt}.
 */
public final class PlayerMoveAction implements ActionExecutor {

    private static final double MAX_DISTANCE = 64.0;
    private static final double ARRIVAL_THRESHOLD = 1.0;

    @Override
    public ActionResult execute(JsonObject params) {
        double tx = JsonUtil.requireDouble(params, "x");
        double ty = JsonUtil.requireDouble(params, "y");
        double tz = JsonUtil.requireDouble(params, "z");
        double speed = MathUtil.clamp(JsonUtil.getDoubleOrDefault(params, "speed", 1.0), 0.1, 2.0);
        int timeoutTicks = JsonUtil.getIntOrDefault(params, "timeout", 200);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        }
        double distance = MathUtil.distance3D(tx, ty, tz, player.getX(), player.getY(), player.getZ());
        if (distance > MAX_DISTANCE) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Target too far: " + String.format("%.1f", distance) + " > " + MAX_DISTANCE);
        }
        if (distance < ARRIVAL_THRESHOLD) {
            JsonObject d = new JsonObject();
            d.addProperty("status", "already_at_target");
            return ActionResult.ok(d);
        }

        InjectedInput injected = new InjectedInput();
        injected.install(player);

        TickScheduler sched = HeraldClientMod.tickScheduler();
        Walker walker = new Walker(injected, tx, ty, tz, speed, timeoutTicks, sched);
        sched.schedule(1, walker);

        JsonObject data = new JsonObject();
        data.addProperty("target_x", tx);
        data.addProperty("target_y", ty);
        data.addProperty("target_z", tz);
        data.addProperty("distance", distance);
        return ActionResult.ok(data);
    }

    /** Per-tick state machine driving the InjectedInput. */
    private static final class Walker implements Runnable {
        private final InjectedInput injected;
        private final double tx, ty, tz;
        private final double speed;
        private final int timeoutTicks;
        private final TickScheduler scheduler;
        private int ticksElapsed = 0;

        Walker(InjectedInput injected, double tx, double ty, double tz,
               double speed, int timeoutTicks, TickScheduler scheduler) {
            this.injected = injected;
            this.tx = tx; this.ty = ty; this.tz = tz;
            this.speed = speed;
            this.timeoutTicks = timeoutTicks;
            this.scheduler = scheduler;
        }

        @Override
        public void run() {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null) {
                injected.reset();
                injected.uninstall();
                return;
            }
            ticksElapsed++;
            double dx = tx - p.getX();
            double dy = ty - p.getY();
            double dz = tz - p.getZ();
            double dist = MathUtil.distance3D(tx, ty, tz, p.getX(), p.getY(), p.getZ());

            if (dist < ARRIVAL_THRESHOLD || ticksElapsed >= timeoutTicks) {
                injected.reset();
                injected.uninstall(p);
                p.setSprinting(false);
                return;
            }

            p.setYRot(MathUtil.yawTo(dx, dz));
            injected.forward = true;
            injected.jumping = dy > 0.5 && p.onGround();
            injected.sprinting = speed > 1.0;
            p.setSprinting(speed > 1.0);

            scheduler.schedule(1, this);
        }
    }
}
