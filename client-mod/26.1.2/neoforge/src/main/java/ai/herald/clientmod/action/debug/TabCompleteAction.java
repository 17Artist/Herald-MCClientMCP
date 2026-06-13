package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Port of BlackBoxPro debug/TabCompleteAction.kt.
 *
 * <p>The transaction id is an opaque integer the server echoes back in the
 * matching response packet — clients are expected to allocate it themselves
 * and keep an in-flight map. We auto-allocate a monotonically increasing id
 * unless one is supplied explicitly.
 */
public final class TabCompleteAction implements ActionExecutor {

    private static final AtomicInteger NEXT_TXN = new AtomicInteger(1);

    @Override
    public ActionResult execute(JsonObject params) {
        String text = JsonUtil.requireString(params, "text");
        int transactionId = JsonUtil.getIntOrDefault(params, "transactionId", -1);
        if (transactionId < 0) transactionId = NEXT_TXN.getAndIncrement();
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundCommandSuggestionPacket(transactionId, text));
        JsonObject data = new JsonObject();
        data.addProperty("transactionId", transactionId);
        data.addProperty("sent", text);
        return ActionResult.ok(data);
    }
}
