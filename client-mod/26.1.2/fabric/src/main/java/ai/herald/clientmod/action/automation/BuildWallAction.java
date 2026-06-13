package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: fills a wall (single-plane region) using /fill.
 */
public final class BuildWallAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int x1 = JsonUtil.requireInt(params, "x1");
        int y1 = JsonUtil.requireInt(params, "y1");
        int z1 = JsonUtil.requireInt(params, "z1");
        int x2 = JsonUtil.requireInt(params, "x2");
        int y2 = JsonUtil.requireInt(params, "y2");
        int z2 = JsonUtil.requireInt(params, "z2");
        String blockId = JsonUtil.requireString(params, "blockId");

        String cmd = "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " " + blockId;
        player.connection.sendCommand(cmd);

        JsonObject data = new JsonObject();
        data.addProperty("command", cmd);
        return ActionResult.ok(data);
    }
}
