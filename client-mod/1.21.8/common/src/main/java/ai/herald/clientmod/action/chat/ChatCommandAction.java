package ai.herald.clientmod.action.chat;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** Run a slash-command via the active client connection. */
public final class ChatCommandAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String cmd = JsonUtil.requireString(params, "command");
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if (cmd.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Empty command");
        }
        ClientPacketListener net = Minecraft.getInstance().getConnection();
        if (net == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not connected to a server");
        }
        net.sendCommand(cmd);
        JsonObject data = new JsonObject();
        data.addProperty("sent", cmd);
        return ActionResult.ok(data);
    }
}
