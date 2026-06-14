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
 * Sync check: inspect the block at (x, y, z).
 * If targetBlock is specified, checks if current block matches.
 * Returns current block state info regardless.
 */
public final class WaitBlockChangeAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        String targetBlock = JsonUtil.getStringOrDefault(params, "targetBlock", null);

        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String currentBlock = blockId != null ? blockId.toString() : "unknown";

        JsonObject data = new JsonObject();
        data.addProperty("x", x);
        data.addProperty("y", y);
        data.addProperty("z", z);
        data.addProperty("currentBlock", currentBlock);
        data.addProperty("blockState", state.toString());

        if (targetBlock == null) {
            // No target specified — just return current state
            data.addProperty("matched", true);
            return ActionResult.ok(data);
        }

        boolean matches = currentBlock.equals(targetBlock)
                || currentBlock.equals("minecraft:" + targetBlock);
        data.addProperty("targetBlock", targetBlock);
        data.addProperty("matched", matches);

        if (matches) {
            return ActionResult.ok(data);
        }
        return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Block mismatch at " + x + "," + y + "," + z
                        + ": expected=" + targetBlock + " actual=" + currentBlock);
    }
}
