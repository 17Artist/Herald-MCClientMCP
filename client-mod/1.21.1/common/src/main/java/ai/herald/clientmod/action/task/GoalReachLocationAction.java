package ai.herald.clientmod.action.task;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class GoalReachLocationAction implements ActionExecutor {
    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in game");
        }

        if (!params.has("x") || !params.has("y") || !params.has("z")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required params: x, y, z");
        }

        double targetX = params.get("x").getAsDouble();
        double targetY = params.get("y").getAsDouble();
        double targetZ = params.get("z").getAsDouble();

        Vec3 playerPos = player.position();
        double dx = targetX - playerPos.x;
        double dy = targetY - playerPos.y;
        double dz = targetZ - playerPos.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Estimate ticks: assume ~4.3 blocks/sec walking speed = ~0.215 blocks/tick
        int estimatedTicks = (int) Math.ceil(horizontalDist / 0.215);

        // Calculate direction as angle
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        if (angle < 0) angle += 360;

        String direction = getCardinalDirection(angle);

        JsonObject data = new JsonObject();
        data.addProperty("targetX", targetX);
        data.addProperty("targetY", targetY);
        data.addProperty("targetZ", targetZ);
        data.addProperty("distance", Math.round(distance * 100.0) / 100.0);
        data.addProperty("horizontalDistance", Math.round(horizontalDist * 100.0) / 100.0);
        data.addProperty("verticalDistance", Math.round(dy * 100.0) / 100.0);
        data.addProperty("estimatedTicks", estimatedTicks);
        data.addProperty("direction", direction);
        data.addProperty("angle", Math.round(angle * 10.0) / 10.0);

        boolean pathfinding = params.has("pathfinding") && params.get("pathfinding").getAsBoolean();
        data.addProperty("pathfinding", pathfinding);

        return ActionResult.ok(data);
    }

    private String getCardinalDirection(double angle) {
        if (angle >= 337.5 || angle < 22.5) return "south";
        if (angle >= 22.5 && angle < 67.5) return "southwest";
        if (angle >= 67.5 && angle < 112.5) return "west";
        if (angle >= 112.5 && angle < 157.5) return "northwest";
        if (angle >= 157.5 && angle < 202.5) return "north";
        if (angle >= 202.5 && angle < 247.5) return "northeast";
        if (angle >= 247.5 && angle < 292.5) return "east";
        if (angle >= 292.5 && angle < 337.5) return "southeast";
        return "unknown";
    }
}
