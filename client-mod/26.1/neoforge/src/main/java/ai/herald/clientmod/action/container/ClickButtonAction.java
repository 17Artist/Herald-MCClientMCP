package ai.herald.clientmod.action.container;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;

/** Port of BlackBoxPro container/ClickButtonAction.kt. */
public final class ClickButtonAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int windowId = JsonUtil.requireInt(params, "windowId");
        int buttonId = JsonUtil.requireInt(params, "buttonId");
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundContainerButtonClickPacket(windowId, buttonId));
        return ActionResult.ok();
    }
}
