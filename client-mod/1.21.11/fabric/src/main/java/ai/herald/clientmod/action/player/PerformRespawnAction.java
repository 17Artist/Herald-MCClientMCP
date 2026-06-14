package ai.herald.clientmod.action.player;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action;

/** Port of BlackBoxPro player/PerformRespawnAction.kt. */
public final class PerformRespawnAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundClientCommandPacket(Action.PERFORM_RESPAWN));
        return ActionResult.ok();
    }
}
