package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sync: queries crop growth state at a given position.
 * Returns crop type, current age, max age, and growth percentage.
 */
public final class QueryCropGrowthAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");

        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof CropBlock crop)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Block at position is not a crop");
        }

        int currentAge = crop.getAge(state);
        int maxAge = crop.getMaxAge();
        double growthPct = maxAge > 0 ? (double) currentAge / maxAge * 100.0 : 0.0;

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());

        JsonObject data = new JsonObject();
        data.addProperty("crop", blockId != null ? blockId.toString() : "unknown");
        data.addProperty("age", currentAge);
        data.addProperty("maxAge", maxAge);
        data.addProperty("growthPercent", Math.round(growthPct * 10.0) / 10.0);
        data.addProperty("mature", currentAge >= maxAge);
        data.addProperty("x", x);
        data.addProperty("y", y);
        data.addProperty("z", z);
        return ActionResult.ok(data);
    }
}
