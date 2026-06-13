package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Sync: Read the redstone signal strength at a given position.
 * Params: x, y, z
 * Returns: directPower (max from all sides), neighborSignal, hasNeighborSignal.
 */
public final class RedstoneQueryPowerAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");

        BlockPos pos = new BlockPos(x, y, z);

        int maxDirect = 0;
        for (Direction dir : Direction.values()) {
            int signal = level.getDirectSignal(pos, dir);
            if (signal > maxDirect) maxDirect = signal;
        }

        int neighborSignal = level.getBestNeighborSignal(pos);
        boolean hasNeighborSignal = level.hasNeighborSignal(pos);

        JsonObject data = new JsonObject();
        data.addProperty("directPower", maxDirect);
        data.addProperty("neighborSignal", neighborSignal);
        data.addProperty("hasNeighborSignal", hasNeighborSignal);
        return ActionResult.ok(data);
    }
}
