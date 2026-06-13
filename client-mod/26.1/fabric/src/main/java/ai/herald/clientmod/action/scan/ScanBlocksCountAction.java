package ai.herald.clientmod.action.scan;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.scan.AreaScanner;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

/**
 * scan_blocks_count — 统计指定区域内各方块类型的数量。
 */
public final class ScanBlocksCountAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

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

        JsonObject counts = AreaScanner.countBlocks(level, from, to);

        JsonObject data = new JsonObject();
        data.add("counts", counts);
        data.addProperty("volume", volume);
        return ActionResult.ok(data);
    }
}
