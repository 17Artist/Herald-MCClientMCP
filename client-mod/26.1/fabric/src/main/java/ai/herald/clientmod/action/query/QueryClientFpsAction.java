package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: Return current FPS, frame time, render distance, and vsync status.
 */
public final class QueryClientFpsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int fps = mc.getFps();
        float frameTimeMs = fps > 0 ? 1000.0f / fps : 0.0f;
        int renderDistance = mc.options.renderDistance().get();
        boolean vsync = mc.options.enableVsync().get();

        JsonObject data = new JsonObject();
        data.addProperty("fps", fps);
        data.addProperty("frameTimeMs", frameTimeMs);
        data.addProperty("renderDistance", renderDistance);
        data.addProperty("vsync", vsync);
        return ActionResult.ok(data);
    }
}
