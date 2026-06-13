package ai.herald.clientmod.action.player;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;

import java.util.UUID;

/** Port of BlackBoxPro player/SpectatorTeleportAction.kt. */
public final class SpectatorTeleportAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String uuidStr = JsonUtil.requireString(params, "targetUuid");
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid UUID: " + uuidStr);
        }
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundTeleportToEntityPacket(uuid));
        return ActionResult.ok();
    }
}
