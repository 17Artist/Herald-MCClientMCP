package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;

/**
 * Asserts that the block at (x, y, z) matches the expected blockId
 * and optionally the given block state properties.
 */
public final class AssertBlockAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        String expectedId = JsonUtil.requireString(params, "blockId");
        JsonObject properties = JsonUtil.getObjectOrNull(params, "properties");

        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        ResourceLocation actualId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String actualIdStr = actualId != null ? actualId.toString() : "unknown";

        if (!actualIdStr.equals(expectedId)) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Expected block " + expectedId + " at (" + x + "," + y + "," + z + ") but got " + actualIdStr);
        }

        if (properties != null) {
            for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
                String propName = entry.getKey();
                String expectedValue = entry.getValue().getAsString();
                String actualValue = getPropertyValue(state, propName);
                if (actualValue == null) {
                    return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                        "Block " + expectedId + " has no property '" + propName + "'");
                }
                if (!actualValue.equals(expectedValue)) {
                    return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                        "Expected property " + propName + "=" + expectedValue + " but got " + actualValue);
                }
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("pass", true);
        data.addProperty("message", "Block at (" + x + "," + y + "," + z + ") is " + expectedId);
        return ActionResult.ok(data);
    }

    private static String getPropertyValue(BlockState state, String propName) {
        for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
            if (entry.getKey().getName().equals(propName)) {
                return entry.getValue().toString();
            }
        }
        return null;
    }
}
