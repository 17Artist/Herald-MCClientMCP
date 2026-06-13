package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: Step on a pressure plate by teleporting the player to its position.
 * Params: x, y, z (position of the pressure plate)
 */
public final class RedstoneStepPressurePlateAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");

        // Teleport player to center of pressure plate
        player.connection.sendCommand("tp @s " + (x + 0.5) + " " + y + " " + (z + 0.5));

        JsonObject data = new JsonObject();
        data.addProperty("teleported", true);
        data.addProperty("x", x + 0.5);
        data.addProperty("y", y);
        data.addProperty("z", z + 0.5);
        return ActionResult.ok(data);
    }
}
