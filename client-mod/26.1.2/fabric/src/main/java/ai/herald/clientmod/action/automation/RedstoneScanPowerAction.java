package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

/**
 * Sync: Scan an area for blocks with redstone signal > 0.
 * Params: x1,y1,z1, x2,y2,z2
 * Max area: 32x32x32. Returns sparse list of powered positions.
 */
public final class RedstoneScanPowerAction implements ActionExecutor {

    private static final int MAX_SIZE = 32;

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        int x1 = JsonUtil.requireInt(params, "x1");
        int y1 = JsonUtil.requireInt(params, "y1");
        int z1 = JsonUtil.requireInt(params, "z1");
        int x2 = JsonUtil.requireInt(params, "x2");
        int y2 = JsonUtil.requireInt(params, "y2");
        int z2 = JsonUtil.requireInt(params, "z2");

        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        if (maxX - minX + 1 > MAX_SIZE || maxY - minY + 1 > MAX_SIZE || maxZ - minZ + 1 > MAX_SIZE) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Scan area exceeds 32x32x32 limit");
        }

        JsonArray results = new JsonArray();
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    mPos.set(bx, by, bz);
                    int signal = level.getBestNeighborSignal(mPos);
                    if (signal > 0) {
                        JsonObject entry = new JsonObject();
                        entry.addProperty("x", bx);
                        entry.addProperty("y", by);
                        entry.addProperty("z", bz);
                        entry.addProperty("power", signal);
                        results.add(entry);
                    }
                }
            }
        }

        JsonObject data = new JsonObject();
        data.add("powered", results);
        data.addProperty("count", results.size());
        return ActionResult.ok(data);
    }
}
