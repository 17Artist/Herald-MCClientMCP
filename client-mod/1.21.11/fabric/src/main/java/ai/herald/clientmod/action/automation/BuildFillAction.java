package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: fills a region with a block using /fill. Splits into chunks if > 32768 blocks.
 */
public final class BuildFillAction implements ActionExecutor {

    private static final int MAX_FILL_VOLUME = 32768;

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

        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        long volume = (long) sizeX * sizeY * sizeZ;

        int commandsSent = 0;
        if (volume <= MAX_FILL_VOLUME) {
            player.connection.sendCommand(
                "fill " + minX + " " + minY + " " + minZ + " " + maxX + " " + maxY + " " + maxZ + " " + blockId);
            commandsSent = 1;
        } else {
            // Split along Y axis into slices
            for (int cy = minY; cy <= maxY; ) {
                int sliceHeight = MAX_FILL_VOLUME / (sizeX * sizeZ);
                if (sliceHeight < 1) sliceHeight = 1;
                int endY = Math.min(cy + sliceHeight - 1, maxY);
                player.connection.sendCommand(
                    "fill " + minX + " " + cy + " " + minZ + " " + maxX + " " + endY + " " + maxZ + " " + blockId);
                commandsSent++;
                cy = endY + 1;
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("commands_sent", commandsSent);
        data.addProperty("volume", volume);
        return ActionResult.ok(data);
    }
}
