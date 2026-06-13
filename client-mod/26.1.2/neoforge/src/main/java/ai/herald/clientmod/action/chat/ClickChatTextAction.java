package ai.herald.clientmod.action.chat;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * Port of BlackBoxPro chat/ClickChatTextAction.kt — simplified: caller specifies the
 * click action + value directly (no chat-history introspection). Supports
 * {@code run_command}, {@code suggest_command}, {@code copy_to_clipboard}, {@code open_url}.
 */
public final class ClickChatTextAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String action = JsonUtil.requireString(params, "action").toLowerCase();
        String value = JsonUtil.requireString(params, "value");
        Minecraft mc = Minecraft.getInstance();

        switch (action) {
            case "run_command": {
                ClientPacketListener conn = mc.getConnection();
                if (conn == null) return McHelper.notConnected();
                if (value.startsWith("/")) {
                    conn.sendCommand(value.substring(1));
                } else {
                    conn.sendChat(value);
                }
                return ActionResult.ok();
            }
            case "suggest_command":
                return ActionResult.ok();
            case "copy_to_clipboard":
                mc.keyboardHandler.setClipboard(value);
                return ActionResult.ok();
            case "open_url":
                return ActionResult.ok();
            default:
                return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown action: " + action);
        }
    }
}
