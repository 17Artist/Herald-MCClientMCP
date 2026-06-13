package ai.herald.clientmod.action.player;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;

/** Port of BlackBoxPro player/DropItemStackAction.kt. */
public final class DropItemStackAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int sequence = JsonUtil.getIntOrDefault(params, "sequence", 0);
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundPlayerActionPacket(Action.DROP_ALL_ITEMS, BlockPos.ZERO, Direction.DOWN, sequence));
        return ActionResult.ok();
    }
}
