package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.protocol.HeraldException;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.ReflectiveGamePackets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** Port of BlackBoxPro debug/KeepAliveAction.kt. */
public final class KeepAliveAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        if (params == null || !params.has("id") || params.get("id").isJsonNull()) {
            throw new HeraldException(ErrorCode.INVALID_PARAMS, "Missing required parameter: id");
        }
        JsonElement el = params.get("id");
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            throw new HeraldException(ErrorCode.INVALID_PARAMS, "Parameter 'id' must be a long");
        }
        long id = el.getAsLong();
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        return ReflectiveGamePackets.sendKeepAlive(conn, id);
    }
}
