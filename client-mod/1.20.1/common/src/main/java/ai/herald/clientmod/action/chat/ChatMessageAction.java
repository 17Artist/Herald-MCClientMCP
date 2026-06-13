package ai.herald.clientmod.action.chat;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** Send a chat message via the active client connection. */
public final class ChatMessageAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String message = JsonUtil.requireString(params, "message");
        if (message.length() > 256) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Message too long: " + message.length() + " > 256");
        }
        ClientPacketListener net = Minecraft.getInstance().getConnection();
        if (net == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not connected to a server");
        }
        net.sendChat(message);
        JsonObject data = new JsonObject();
        data.addProperty("sent", message);
        return ActionResult.ok(data);
    }
}
