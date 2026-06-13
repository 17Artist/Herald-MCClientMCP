package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

public final class QueryPerformanceAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return McHelper.notInGame();

        Runtime runtime = Runtime.getRuntime();
        long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
        long memoryMax = runtime.maxMemory();

        JsonObject data = new JsonObject();
        data.addProperty("fps", mc.getFps());
        data.addProperty("memoryUsedMB", memoryUsed / (1024 * 1024));
        data.addProperty("memoryMaxMB", memoryMax / (1024 * 1024));
        data.addProperty("loadedChunks", level.getChunkSource().getLoadedChunksCount());
        data.addProperty("entityCount", level.getEntityCount());
        data.addProperty("renderDistance", mc.options.renderDistance().get());
        return ActionResult.ok(data);
    }
}
