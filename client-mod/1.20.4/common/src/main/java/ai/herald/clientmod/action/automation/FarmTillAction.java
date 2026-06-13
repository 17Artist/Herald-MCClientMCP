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
 * Sync: converts grass_block or dirt at the given position to farmland.
 */
public final class FarmTillAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");

        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() != Blocks.GRASS_BLOCK && state.getBlock() != Blocks.DIRT) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Block is not grass_block or dirt, cannot till");
        }

        player.connection.sendCommand("setblock " + x + " " + y + " " + z + " farmland");

        JsonObject data = new JsonObject();
        data.addProperty("tilled", true);
        data.addProperty("x", x);
        data.addProperty("y", y);
        data.addProperty("z", z);
        return ActionResult.ok(data);
    }
}
