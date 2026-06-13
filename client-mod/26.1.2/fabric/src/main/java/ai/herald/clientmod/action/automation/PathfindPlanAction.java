package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: Calculate a simplified path from player to target.
 * Currently returns start and end as the "path" (placeholder for real A* pathfinding).
 * Returns: {path: [{x,y,z}, ...], distance: N, estimatedTicks: N}
 */
public final class PathfindPlanAction implements ActionExecutor {

    private static final double WALK_SPEED_BPS = 4.317; // blocks per second walking

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        double tx = JsonUtil.requireDouble(params, "x");
        double ty = JsonUtil.requireDouble(params, "y");
        double tz = JsonUtil.requireDouble(params, "z");
        @SuppressWarnings("unused")
        boolean allowSwim = JsonUtil.getBooleanOrDefault(params, "allowSwim", false);

        double sx = player.getX();
        double sy = player.getY();
        double sz = player.getZ();

        double dx = tx - sx;
        double dy = ty - sy;
        double dz = tz - sz;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Simplified: return straight-line path with start and end waypoints
        JsonArray path = new JsonArray();

        JsonObject start = new JsonObject();
        start.addProperty("x", sx);
        start.addProperty("y", sy);
        start.addProperty("z", sz);
        path.add(start);

        JsonObject end = new JsonObject();
        end.addProperty("x", tx);
        end.addProperty("y", ty);
        end.addProperty("z", tz);
        path.add(end);

        int estimatedTicks = (int) Math.ceil((distance / WALK_SPEED_BPS) * 20.0);

        JsonObject data = new JsonObject();
        data.add("path", path);
        data.addProperty("distance", distance);
        data.addProperty("estimatedTicks", estimatedTicks);
        data.addProperty("waypoints", path.size());
        data.addProperty("note", "Simplified straight-line path; no obstacle avoidance");
        return ActionResult.ok(data);
    }
}
