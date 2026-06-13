package ai.herald.clientmod.action.scan;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.scan.AreaScanner;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

/**
 * scan_blocks_find — 在指定中心点和半径范围内搜索指定方块类型。
 */
public final class ScanBlocksFindAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        int radius = JsonUtil.requireInt(params, "radius");
        String blockId = JsonUtil.requireString(params, "blockId");
        int limit = JsonUtil.getIntOrDefault(params, "limit", 100);

        BlockPos center = new BlockPos(x, y, z);
        JsonArray results = AreaScanner.findBlocks(level, center, radius, blockId, limit);

        JsonObject data = new JsonObject();
        data.add("positions", results);
        data.addProperty("count", results.size());
        data.addProperty("blockId", blockId);
        return ActionResult.ok(data);
    }
}
