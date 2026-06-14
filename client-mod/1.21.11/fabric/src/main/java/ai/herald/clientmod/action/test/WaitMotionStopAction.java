package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Sync check: test if player velocity magnitude is below a threshold.
 * Returns ok if player is effectively stopped, error ASSERTION_FAILED if still moving.
 */
public final class WaitMotionStopAction implements ActionExecutor {

    private static final double DEFAULT_THRESHOLD = 0.01;

    @Override
    public ActionResult execute(JsonObject params) {
        double threshold = JsonUtil.getDoubleOrDefault(params, "threshold", DEFAULT_THRESHOLD);

        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        Vec3 velocity = player.getDeltaMovement();
        // Use horizontal magnitude by default (Y is always affected by gravity)
        double horizontalMag = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double fullMag = velocity.length();

        JsonObject data = new JsonObject();
        data.addProperty("vx", velocity.x);
        data.addProperty("vy", velocity.y);
        data.addProperty("vz", velocity.z);
        data.addProperty("horizontalMagnitude", horizontalMag);
        data.addProperty("fullMagnitude", fullMag);
        data.addProperty("threshold", threshold);
        data.addProperty("onGround", player.onGround());

        if (horizontalMag < threshold) {
            data.addProperty("stopped", true);
            return ActionResult.ok(data);
        }

        return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Player still moving: horizontal=" + String.format("%.4f", horizontalMag)
                        + " threshold=" + threshold);
    }
}
