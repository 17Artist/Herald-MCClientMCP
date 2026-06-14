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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sync: harvests a single crop at the given position if it is at max age.
 * Uses /setblock x y z air to break the crop.
 */
public final class HarvestCropAction implements ActionExecutor {

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
        if (!(state.getBlock() instanceof CropBlock crop)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Block at position is not a crop");
        }

        if (!crop.isMaxAge(state)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Crop is not fully grown (age=" + crop.getAge(state) + "/" + crop.getMaxAge() + ")");
        }

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        player.connection.sendCommand("setblock " + x + " " + y + " " + z + " air");

        JsonObject data = new JsonObject();
        data.addProperty("harvested", blockId != null ? blockId.toString() : "unknown");
        data.addProperty("x", x);
        data.addProperty("y", y);
        data.addProperty("z", z);
        return ActionResult.ok(data);
    }
}
