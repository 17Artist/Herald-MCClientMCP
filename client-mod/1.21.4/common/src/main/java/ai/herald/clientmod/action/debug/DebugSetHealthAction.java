package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

public final class DebugSetHealthAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        float health = (float) JsonUtil.requireDouble(params, "health");
        player.setHealth(health);

        JsonObject data = new JsonObject();
        data.addProperty("health", health);
        data.addProperty("note", "Client-side only; server may override");
        return ActionResult.ok(data);
    }
}
