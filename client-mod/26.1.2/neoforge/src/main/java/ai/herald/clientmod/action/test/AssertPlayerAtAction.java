package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Asserts that the player is within a given distance of a target position.
 */
public final class AssertPlayerAtAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        double x = JsonUtil.requireDouble(params, "x");
        double y = JsonUtil.requireDouble(params, "y");
        double z = JsonUtil.requireDouble(params, "z");
        double tolerance = JsonUtil.getDoubleOrDefault(params, "tolerance", 1.0);

        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double dz = player.getZ() - z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > tolerance) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Expected player within " + tolerance + " of (" + x + "," + y + "," + z
                    + ") but distance is " + String.format("%.2f", distance));
        }

        JsonObject data = new JsonObject();
        data.addProperty("pass", true);
        data.addProperty("message", "Player is " + String.format("%.2f", distance)
            + " blocks from (" + x + "," + y + "," + z + ")");
        return ActionResult.ok(data);
    }
}
