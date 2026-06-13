package ai.herald.clientmod.action.block;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.DirectionUtil;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;

/** Port of BlackBoxPro block/DigCancelAction.kt. */
public final class DigCancelAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        Direction face = DirectionUtil.fromString(JsonUtil.requireString(params, "face"));
        int sequence = JsonUtil.getIntOrDefault(params, "sequence", 0);

        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, new BlockPos(x, y, z), face, sequence));
        return ActionResult.ok();
    }
}
