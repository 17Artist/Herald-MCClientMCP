package ai.herald.clientmod.action.movement;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;

/** Port of BlackBoxPro movement/PaddleBoatAction.kt. */
public final class PaddleBoatAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        boolean left = JsonUtil.requireBoolean(params, "leftPaddling");
        boolean right = JsonUtil.requireBoolean(params, "rightPaddling");
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundPaddleBoatPacket(left, right));
        return ActionResult.ok();
    }
}
