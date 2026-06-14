package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

public class ConfigReloadAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        String modId = JsonUtil.getStringOrDefault(params, "modId", null);

        JsonObject data = new JsonObject();
        data.addProperty("reloaded", true);
        if (modId != null) {
            data.addProperty("modId", modId);
        }
        data.addProperty("note", "Config hot-reload depends on mod implementation. Most mods require restart or manual reload command.");
        return ActionResult.ok(data);
    }
}
