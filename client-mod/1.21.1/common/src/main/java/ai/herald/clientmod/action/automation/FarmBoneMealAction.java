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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sync: instantly grows a crop to max age by setting the block state.
 * Simulates bone meal usage without requiring the item.
 */
public final class FarmBoneMealAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        int count = JsonUtil.getIntOrDefault(params, "count", 1);

        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof CropBlock crop)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Block at position is not a crop");
        }

        int currentAge = crop.getAge(state);
        int maxAge = crop.getMaxAge();

        // Set to max age directly
        String blockName = getCropName(crop);
        if (blockName == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Cannot identify crop type");
        }

        player.connection.sendCommand(
            "setblock " + x + " " + y + " " + z + " " + blockName + "[age=" + maxAge + "]");

        JsonObject data = new JsonObject();
        data.addProperty("crop", blockName);
        data.addProperty("previousAge", currentAge);
        data.addProperty("newAge", maxAge);
        data.addProperty("maxAge", maxAge);
        return ActionResult.ok(data);
    }

    private static String getCropName(CropBlock crop) {
        net.minecraft.resources.ResourceLocation id =
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(crop);
        return id != null ? id.getPath() : null;
    }
}
