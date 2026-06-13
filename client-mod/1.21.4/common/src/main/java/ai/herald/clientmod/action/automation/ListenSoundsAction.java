package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.sensing.SensingBuffer;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: Enable sound listening and return current buffer state.
 * The SensingBuffer collects sounds via Mixin hooks.
 * Returns: {listening: true, bufferSize: N}
 */
public final class ListenSoundsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        @SuppressWarnings("unused")
        int duration = JsonUtil.getIntOrDefault(params, "duration", 100);
        @SuppressWarnings("unused")
        double radius = JsonUtil.getDoubleOrDefault(params, "radius", 32.0);

        int bufferSize = SensingBuffer.getRecentSounds(Integer.MAX_VALUE).size();

        JsonObject data = new JsonObject();
        data.addProperty("listening", true);
        data.addProperty("bufferSize", bufferSize);
        return ActionResult.ok(data);
    }
}
