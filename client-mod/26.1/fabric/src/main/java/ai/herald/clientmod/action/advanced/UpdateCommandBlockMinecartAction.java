package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;

/** Port of BlackBoxPro advanced/UpdateCommandBlockMinecartAction.kt. */
public final class UpdateCommandBlockMinecartAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int entityId = JsonUtil.requireInt(params, "entityId");
        String command = JsonUtil.requireString(params, "command");
        boolean trackOutput = JsonUtil.getBooleanOrDefault(params, "trackOutput", true);
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundSetCommandMinecartPacket(entityId, command, trackOutput));
        return ActionResult.ok();
    }
}
