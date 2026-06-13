package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

public final class DebugSetTimeAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String time = JsonUtil.requireString(params, "time");

        // In MC 26.1, time management is through clockManager(); use /time command for both SP and MP
        player.connection.sendCommand("time set " + time);
        JsonObject data = new JsonObject();
        data.addProperty("time", time);
        data.addProperty("method", "command");
        return ActionResult.ok(data);
    }
}
