package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: Detailed JVM memory breakdown — heap used/max/free, thread count.
 */
public final class QueryMemoryDetailAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;

        int threadCount = Thread.activeCount();

        JsonObject data = new JsonObject();
        data.addProperty("heapUsedMB", usedMemory / (1024 * 1024));
        data.addProperty("heapMaxMB", maxMemory / (1024 * 1024));
        data.addProperty("heapFreeMB", freeMemory / (1024 * 1024));
        data.addProperty("heapTotalMB", totalMemory / (1024 * 1024));
        data.addProperty("threads", threadCount);
        return ActionResult.ok(data);
    }
}
