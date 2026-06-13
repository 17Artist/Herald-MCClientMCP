package ai.herald.clientmod.action.movement;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;

/** Port of BlackBoxPro movement/ConfirmTeleportationAction.kt. */
public final class ConfirmTeleportationAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int teleportId = JsonUtil.requireInt(params, "teleportId");
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundAcceptTeleportationPacket(teleportId));
        return ActionResult.ok();
    }
}
