package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.scan.AreaScanner;
import ai.herald.clientmod.testing.SnapshotManager;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

/**
 * snapshot_area — snapshots blocks in a specified area using palette format.
 * Params: name(string), x1, y1, z1, x2, y2, z2
 */
public final class SnapshotAreaAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        String name = JsonUtil.requireString(params, "name");
        int x1 = JsonUtil.requireInt(params, "x1");
        int y1 = JsonUtil.requireInt(params, "y1");
        int z1 = JsonUtil.requireInt(params, "z1");
        int x2 = JsonUtil.requireInt(params, "x2");
        int y2 = JsonUtil.requireInt(params, "y2");
        int z2 = JsonUtil.requireInt(params, "z2");

        int dx = Math.abs(x2 - x1) + 1;
        int dy = Math.abs(y2 - y1) + 1;
        int dz = Math.abs(z2 - z1) + 1;
        long volume = (long) dx * dy * dz;

        if (volume > AreaScanner.MAX_VOLUME) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Volume " + volume + " exceeds max " + AreaScanner.MAX_VOLUME);
        }

        BlockPos from = new BlockPos(x1, y1, z1);
        BlockPos to = new BlockPos(x2, y2, z2);

        JsonObject blockData = AreaScanner.scanPalette(level, from, to);
        blockData.addProperty("snapshotName", name);
        blockData.addProperty("timestamp", System.currentTimeMillis());

        SnapshotManager.save(name, blockData);

        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("volume", volume);
        data.addProperty("paletteSize", blockData.getAsJsonArray("palette").size());
        return ActionResult.ok(data);
    }
}
