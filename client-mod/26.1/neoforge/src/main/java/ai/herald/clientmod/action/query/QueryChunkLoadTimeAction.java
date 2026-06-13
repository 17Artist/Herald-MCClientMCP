package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: Return chunk loading statistics — loaded count, render distance, total possible.
 */
public final class QueryChunkLoadTimeAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        int loadedChunks = level.getChunkSource().getLoadedChunksCount();
        int renderDistance = mc.options.renderDistance().get();
        // Total possible chunks in view = (2*rd+1)^2
        int diameter = 2 * renderDistance + 1;
        int totalPossible = diameter * diameter;

        JsonObject data = new JsonObject();
        data.addProperty("loadedChunks", loadedChunks);
        data.addProperty("renderDistance", renderDistance);
        data.addProperty("totalPossible", totalPossible);
        return ActionResult.ok(data);
    }
}
