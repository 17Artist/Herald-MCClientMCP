package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * Query entity NBT. In 1.21+ the dedicated packet was removed;
 * we fall back to sending the /data command (requires OP 2+).
 */
public final class QueryEntityNbtAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int entityId = JsonUtil.requireInt(params, "entityId");
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.sendCommand("data get entity @e[limit=1,sort=nearest]");
        return ActionResult.ok();
    }
}
