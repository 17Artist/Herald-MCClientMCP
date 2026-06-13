package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: calculates a dodge vector based on player yaw + direction, applies impulse.
 * Directions: left (yaw-90), right (yaw+90), back (yaw+180).
 */
public final class CombatDodgeAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String direction = JsonUtil.getStringOrDefault(params, "direction", "back");
        double distance = JsonUtil.getDoubleOrDefault(params, "distance", 2.0);
        if (distance <= 0) distance = 2.0;
        if (distance > 10.0) distance = 10.0;

        float yaw = player.getYRot();
        float dodgeYaw;
        switch (direction.toLowerCase()) {
            case "left":
                dodgeYaw = yaw - 90f;
                break;
            case "right":
                dodgeYaw = yaw + 90f;
                break;
            case "back":
            default:
                dodgeYaw = yaw + 180f;
                break;
        }

        double rad = Math.toRadians(dodgeYaw);
        double speed = Math.min(distance * 0.5, 3.0);
        double vx = -Math.sin(rad) * speed;
        double vz = Math.cos(rad) * speed;

        player.setDeltaMovement(vx, player.getDeltaMovement().y + 0.1, vz);
        player.hurtMarked = true;

        JsonObject data = new JsonObject();
        data.addProperty("direction", direction);
        data.addProperty("dodge_yaw", dodgeYaw);
        data.addProperty("velocity_x", vx);
        data.addProperty("velocity_z", vz);
        return ActionResult.ok(data);
    }
}
