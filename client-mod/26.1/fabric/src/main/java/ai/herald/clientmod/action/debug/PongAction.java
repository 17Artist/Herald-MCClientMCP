package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.ReflectiveGamePackets;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** Port of BlackBoxPro debug/PongAction.kt. */
public final class PongAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int parameter = JsonUtil.requireInt(params, "parameter");
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        return ReflectiveGamePackets.sendPong(conn, parameter);
    }
}
