package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Asserts that the block at (x, y, z) is NOT the given blockId.
 */
public final class AssertBlockNotAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        String forbiddenId = JsonUtil.requireString(params, "blockId");

        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        Identifier actualId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String actualIdStr = actualId != null ? actualId.toString() : "unknown";

        if (actualIdStr.equals(forbiddenId)) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Expected block at (" + x + "," + y + "," + z + ") to NOT be " + forbiddenId + " but it is");
        }

        JsonObject data = new JsonObject();
        data.addProperty("pass", true);
        data.addProperty("message", "Block at (" + x + "," + y + "," + z + ") is " + actualIdStr + " (not " + forbiddenId + ")");
        return ActionResult.ok(data);
    }
}
