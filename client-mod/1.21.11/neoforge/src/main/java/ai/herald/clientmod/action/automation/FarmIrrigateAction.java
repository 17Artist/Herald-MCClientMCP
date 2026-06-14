package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: places a water source block at the given position using /setblock.
 */
public final class FarmIrrigateAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");

        player.connection.sendCommand("setblock " + x + " " + y + " " + z + " water");

        JsonObject data = new JsonObject();
        data.addProperty("placed", "water");
        data.addProperty("x", x);
        data.addProperty("y", y);
        data.addProperty("z", z);
        return ActionResult.ok(data);
    }
}
