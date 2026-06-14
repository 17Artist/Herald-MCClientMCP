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

/** Port of BlackBoxPro movement/PlayerMoveLookAction.kt — walk to target while holding pitch. */
public final class PlayerMoveLookAction implements ActionExecutor {

    private static final double MAX_DISTANCE = 64.0;
    private static final double ARRIVAL_THRESHOLD = 1.0;

    @Override
    public ActionResult execute(JsonObject params) {
        double tx = JsonUtil.requireDouble(params, "x");
        double ty = JsonUtil.requireDouble(params, "y");
        double tz = JsonUtil.requireDouble(params, "z");
        float pitch = (float) JsonUtil.requireDouble(params, "pitch");
        double speed = MathUtil.clamp(JsonUtil.getDoubleOrDefault(params, "speed", 1.0), 0.1, 2.0);
        int timeoutTicks = JsonUtil.getIntOrDefault(params, "timeout", 200);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");

        double dist = MathUtil.distance3D(tx, ty, tz, player.getX(), player.getY(), player.getZ());
        if (dist > MAX_DISTANCE) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Target too far: " + dist + " > " + MAX_DISTANCE);
        }
        if (dist < ARRIVAL_THRESHOLD) {
            JsonObject d = new JsonObject();
            d.addProperty("status", "already_at_target");
            return ActionResult.ok(d);
        }

        InjectedInput injected = new InjectedInput();
        injected.install(player);
        TickScheduler sched = HeraldClientMod.tickScheduler();
        sched.schedule(1, new Walker(injected, tx, ty, tz, pitch, speed, timeoutTicks, sched));

        JsonObject data = new JsonObject();
        data.addProperty("target_x", tx);
        data.addProperty("target_y", ty);
        data.addProperty("target_z", tz);
        data.addProperty("distance", dist);
        return ActionResult.ok(data);
    }

    private static final class Walker implements Runnable {
        private final InjectedInput injected;
        private final double tx, ty, tz;
        private final float pitch;
        private final double speed;
        private final int timeoutTicks;
        private final TickScheduler scheduler;
        private int ticks = 0;

        Walker(InjectedInput injected, double tx, double ty, double tz, float pitch,
               double speed, int timeoutTicks, TickScheduler scheduler) {
            this.injected = injected;
            this.tx = tx; this.ty = ty; this.tz = tz;
            this.pitch = pitch;
            this.speed = speed;
            this.timeoutTicks = timeoutTicks;
            this.scheduler = scheduler;
        }

        @Override
        public void run() {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null) { injected.reset(); injected.uninstall(); return; }
            ticks++;
            double dx = tx - p.getX();
            double dy = ty - p.getY();
            double dz = tz - p.getZ();
            double dist = MathUtil.distance3D(tx, ty, tz, p.getX(), p.getY(), p.getZ());
            if (dist < ARRIVAL_THRESHOLD || ticks >= timeoutTicks) {
                injected.reset();
                injected.uninstall(p);
                p.setSprinting(false);
                return;
            }
            p.setYRot(MathUtil.yawTo(dx, dz));
            p.setXRot(pitch);
            injected.forward = true;
            injected.jumping = dy > 0.5 && p.onGround();
            injected.sprinting = speed > 1.0;
            p.setSprinting(speed > 1.0);
            scheduler.schedule(1, this);
        }
    }
}
