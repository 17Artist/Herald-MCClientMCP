package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQuery;

/** Port of BlackBoxPro advanced/QueryBlockNbtAction.kt. */
public final class QueryBlockNbtAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int transactionId = JsonUtil.requireInt(params, "transactionId");
        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundBlockEntityTagQuery(transactionId, new BlockPos(x, y, z)));
        return ActionResult.ok();
    }
}
