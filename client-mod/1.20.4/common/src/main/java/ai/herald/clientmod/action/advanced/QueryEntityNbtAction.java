package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundEntityTagQuery;

/** Port of BlackBoxPro advanced/QueryEntityNbtAction.kt. */
public final class QueryEntityNbtAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int transactionId = JsonUtil.requireInt(params, "transactionId");
        int entityId = JsonUtil.requireInt(params, "entityId");
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundEntityTagQuery(transactionId, entityId));
        return ActionResult.ok();
    }
}
