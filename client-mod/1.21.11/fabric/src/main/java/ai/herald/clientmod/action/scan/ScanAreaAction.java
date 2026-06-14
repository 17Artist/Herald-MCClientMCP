package ai.herald.clientmod.action.scan;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.scan.AreaScanner;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

/**
 * scan_area — 扫描指定区域的方块数据。
 * 支持 palette（调色板压缩）和 sparse（稀疏非空气）两种格式。
 */
public final class ScanAreaAction implements ActionExecutor {

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
        String format = JsonUtil.getStringOrDefault(params, "format", "palette");

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

        if ("sparse".equals(format)) {
            JsonObject data = new JsonObject();
            JsonArray sparse = AreaScanner.scanSparse(level, from, to);
            data.add("blocks", sparse);
            data.addProperty("count", sparse.size());
            return ActionResult.ok(data);
        } else {
            JsonObject data = AreaScanner.scanPalette(level, from, to);
            return ActionResult.ok(data);
        }
    }
}
