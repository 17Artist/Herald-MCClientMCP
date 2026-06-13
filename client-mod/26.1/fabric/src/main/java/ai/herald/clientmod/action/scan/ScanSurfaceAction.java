package ai.herald.clientmod.action.scan;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.scan.AreaScanner;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * scan_surface — 获取指定 xz 范围的地表高度图。
 * 最大面积 64×64。
 */
public final class ScanSurfaceAction implements ActionExecutor {

    private static final int MAX_SIDE = 64;

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        int x1 = JsonUtil.requireInt(params, "x1");
        int z1 = JsonUtil.requireInt(params, "z1");
        int x2 = JsonUtil.requireInt(params, "x2");
        int z2 = JsonUtil.requireInt(params, "z2");

        int dx = Math.abs(x2 - x1) + 1;
        int dz = Math.abs(z2 - z1) + 1;
        long area = (long) dx * dz;

        if (area > (long) MAX_SIDE * MAX_SIDE) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Area " + area + " exceeds max " + (MAX_SIDE * MAX_SIDE));
        }

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        JsonObject data = AreaScanner.surfaceMap(level, minX, minZ, maxX, maxZ);
        return ActionResult.ok(data);
    }
}
