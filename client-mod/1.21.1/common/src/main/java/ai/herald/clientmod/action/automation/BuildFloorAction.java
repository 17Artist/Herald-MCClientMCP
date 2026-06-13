package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: fills a single-layer floor at y using /fill x1 y z1 x2 y z2 blockId.
 */
public final class BuildFloorAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int x1 = JsonUtil.requireInt(params, "x1");
        int z1 = JsonUtil.requireInt(params, "z1");
        int x2 = JsonUtil.requireInt(params, "x2");
        int z2 = JsonUtil.requireInt(params, "z2");
        int y = JsonUtil.requireInt(params, "y");
        String blockId = JsonUtil.requireString(params, "blockId");

        String cmd = "fill " + x1 + " " + y + " " + z1 + " " + x2 + " " + y + " " + z2 + " " + blockId;
        player.connection.sendCommand(cmd);

        JsonObject data = new JsonObject();
        data.addProperty("command", cmd);
        return ActionResult.ok(data);
    }
}
