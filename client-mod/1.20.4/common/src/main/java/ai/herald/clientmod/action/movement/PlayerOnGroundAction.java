package ai.herald.clientmod.action.movement;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Port of BlackBoxPro movement/PlayerOnGroundAction.kt. */
public final class PlayerOnGroundAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        boolean onGround = JsonUtil.requireBoolean(params, "onGround");
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        McVersionCompat.sendStatusOnlyPacket(conn, onGround);
        return ActionResult.ok();
    }
}
