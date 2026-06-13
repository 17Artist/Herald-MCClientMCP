package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;

/**
 * Sync: Set the delay on a repeater block.
 * Params: x, y, z, delay (int, 1-4)
 * Preserves all other properties (facing, locked, powered) and uses setblock.
 */
public final class RedstoneSetRepeaterDelayAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        int delay = JsonUtil.requireInt(params, "delay");

        if (delay < 1 || delay > 4) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "delay must be 1-4");
        }

        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof RepeaterBlock)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "No repeater at (" + x + "," + y + "," + z + ")");
        }

        // Build blockstate string preserving all properties but overriding delay
        StringBuilder sb = new StringBuilder("repeater[");
        boolean first = true;
        for (Map.Entry<Property<?>, Comparable<?>> entry : McVersionCompat.getBlockStateEntries(state)) {
            if (!first) sb.append(",");
            first = false;
            String propName = entry.getKey().getName();
            if (propName.equals("delay")) {
                sb.append("delay=").append(delay);
            } else {
                sb.append(propName).append("=").append(entry.getValue().toString());
            }
        }
        sb.append("]");

        player.connection.sendCommand("setblock " + x + " " + y + " " + z + " minecraft:" + sb);

        JsonObject data = new JsonObject();
        data.addProperty("delay", delay);
        data.addProperty("set", true);
        return ActionResult.ok(data);
    }
}
