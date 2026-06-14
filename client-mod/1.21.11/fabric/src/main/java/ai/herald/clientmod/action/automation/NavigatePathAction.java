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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Async: Walk through each waypoint in path sequentially.
 * Move to waypoint[0], when close switch to [1], etc.
 * Each tick: calculate direction to current waypoint, move player, auto-jump if needed.
 * Completes when all waypoints visited.
 */
public final class NavigatePathAction implements ActionExecutor {

    private static final double WAYPOINT_THRESHOLD = 1.2;

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        JsonElement pathEl = params.get("path");
        if (pathEl == null || !pathEl.isJsonArray()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing 'path' array");
        }
        JsonArray pathArray = pathEl.getAsJsonArray();
        if (pathArray.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Path is empty");
        }

        List<double[]> waypoints = new ArrayList<>();
        for (JsonElement el : pathArray) {
            if (!el.isJsonObject()) {
                return ActionResult.error(ErrorCode.INVALID_PARAMS,
                        "Each waypoint must be {x, y, z}");
            }
            JsonObject wp = el.getAsJsonObject();
            double x = JsonUtil.requireDouble(wp, "x");
            double y = JsonUtil.requireDouble(wp, "y");
            double z = JsonUtil.requireDouble(wp, "z");
            waypoints.add(new double[]{x, y, z});
        }

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("navigate_path", params);

        InjectedInput injected = new InjectedInput();
        injected.install(player);

        HeraldClientMod.tickScheduler().schedule(1,
                new PathWalker(task.taskId(), waypoints, injected));
        return ActionResult.async(task.taskId());
    }

    private static final class PathWalker implements Runnable {
        private final String taskId;
        private final List<double[]> waypoints;
        private final InjectedInput injected;
        private int currentIndex = 0;
        private int ticks = 0;

        PathWalker(String taskId, List<double[]> waypoints, InjectedInput injected) {
            this.taskId = taskId;
            this.waypoints = waypoints;
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
            if (p == null) {
                cleanup(null);
                engine.fail(taskId, "Player left world");
                return;
            }
            ticks++;

            if (ticks >= PathfindingConfig.DEFAULT_TIMEOUT_TICKS * waypoints.size()) {
                cleanup(p);
                engine.fail(taskId, "Timed out navigating path at waypoint " + currentIndex);
                return;
            }

            double[] wp = waypoints.get(currentIndex);
            double dx = wp[0] - p.getX();
            double dy = wp[1] - p.getY();
            double dz = wp[2] - p.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < WAYPOINT_THRESHOLD) {
                currentIndex++;
                if (currentIndex >= waypoints.size()) {
                    cleanup(p);
                    JsonObject result = new JsonObject();
                    result.addProperty("status", "completed");
                    result.addProperty("ticks_elapsed", ticks);
                    result.addProperty("waypoints_visited", waypoints.size());
                    engine.complete(taskId, result);
                    return;
                }
                // Recalculate for next waypoint
                wp = waypoints.get(currentIndex);
                dx = wp[0] - p.getX();
                dy = wp[1] - p.getY();
                dz = wp[2] - p.getZ();
            }

            p.setYRot(MathUtil.yawTo(dx, dz));
            injected.forward = true;
            injected.jumping = dy > PathfindingConfig.AUTO_JUMP_MIN_DY && p.onGround();

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
