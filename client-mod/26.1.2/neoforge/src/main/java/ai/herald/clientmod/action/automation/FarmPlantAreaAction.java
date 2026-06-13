package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sync: plants crops across an area where the block below is farmland and current is air.
 * Auto-detects Y if not provided.
 */
public final class FarmPlantAreaAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        int x1 = JsonUtil.requireInt(params, "x1");
        int z1 = JsonUtil.requireInt(params, "z1");
        int x2 = JsonUtil.requireInt(params, "x2");
        int z2 = JsonUtil.requireInt(params, "z2");
        String seedItem = JsonUtil.requireString(params, "seedItem");
        int fixedY = JsonUtil.getIntOrDefault(params, "y", Integer.MIN_VALUE);

        String cropBlock = FarmPlantAction.seedToCrop(seedItem);
        if (cropBlock == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Unknown seed item: " + seedItem);
        }

        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        int planted = 0;
        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                if (fixedY != Integer.MIN_VALUE) {
                    if (tryPlant(level, player, bx, fixedY, bz, cropBlock)) {
                        planted++;
                    }
                } else {
                    // Auto-detect: scan from player Y up/down
                    int py = (int) player.getY();
                    for (int by = py - 5; by <= py + 5; by++) {
                        if (tryPlant(level, player, bx, by, bz, cropBlock)) {
                            planted++;
                            break;
                        }
                    }
                }
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("planted", planted);
        data.addProperty("crop", cropBlock);
        return ActionResult.ok(data);
    }

    private boolean tryPlant(ClientLevel level, LocalPlayer player, int x, int y, int z, String cropBlock) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockPos below = pos.below();
        BlockState current = level.getBlockState(pos);
        BlockState belowState = level.getBlockState(below);

        if (current.isAir() && belowState.getBlock() == Blocks.FARMLAND) {
            player.connection.sendCommand("setblock " + x + " " + y + " " + z + " " + cropBlock + "[age=0]");
            return true;
        }
        return false;
    }
}
