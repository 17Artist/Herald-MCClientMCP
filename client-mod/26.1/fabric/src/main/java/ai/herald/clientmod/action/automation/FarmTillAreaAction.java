package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sync: tills all grass_block and dirt in an area to farmland.
 * Scans around player's Y level to find surface blocks.
 */
public final class FarmTillAreaAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        int x1 = JsonUtil.requireInt(params, "x1");
        int z1 = JsonUtil.requireInt(params, "z1");
        int x2 = JsonUtil.requireInt(params, "x2");
        int z2 = JsonUtil.requireInt(params, "z2");

        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        int py = (int) player.getY();

        int tilled = 0;
        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                // Scan a range around player Y for surface dirt/grass
                for (int by = py - 5; by <= py + 5; by++) {
                    BlockPos pos = new BlockPos(bx, by, bz);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() == Blocks.GRASS_BLOCK || state.getBlock() == Blocks.DIRT) {
                        // Verify block above is air (surface block)
                        BlockState above = level.getBlockState(pos.above());
                        if (above.isAir()) {
                            player.connection.sendCommand(
                                "setblock " + bx + " " + by + " " + bz + " farmland");
                            tilled++;
                            break;
                        }
                    }
                }
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("tilled", tilled);
        return ActionResult.ok(data);
    }
}
