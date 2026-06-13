package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.world.level.block.entity.CommandBlockEntity.Mode;

/** Port of BlackBoxPro advanced/UpdateCommandBlockAction.kt. */
public final class UpdateCommandBlockAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        String command = JsonUtil.requireString(params, "command");
        int mode = JsonUtil.getIntOrDefault(params, "mode", 2);
        boolean trackOutput = JsonUtil.getBooleanOrDefault(params, "trackOutput", true);
        boolean conditional = JsonUtil.getBooleanOrDefault(params, "conditional", false);
        boolean alwaysActive = JsonUtil.getBooleanOrDefault(params, "alwaysActive", false);

        Mode type;
        switch (mode) {
            case 0: type = Mode.SEQUENCE; break;
            case 1: type = Mode.AUTO; break;
            case 2: type = Mode.REDSTONE; break;
            default: return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid mode: " + mode);
        }
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundSetCommandBlockPacket(new BlockPos(x, y, z), command, type,
            trackOutput, conditional, alwaysActive));
        return ActionResult.ok();
    }
}
