package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;

/** Port of BlackBoxPro advanced/SelectTradeAction.kt. */
public final class SelectTradeAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int selectedSlot = JsonUtil.requireInt(params, "selectedSlot");
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundSelectTradePacket(selectedSlot));
        return ActionResult.ok();
    }
}
