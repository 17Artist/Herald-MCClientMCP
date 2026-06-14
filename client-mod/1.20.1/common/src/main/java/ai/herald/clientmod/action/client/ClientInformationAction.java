package ai.herald.clientmod.action.client;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;

/**
 * Sends client information (locale, view distance, etc.) to the server.
 * MC 1.20.1 uses ServerboundClientInformationPacket with flat fields.
 */
public final class ClientInformationAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String locale = JsonUtil.getStringOrDefault(params, "locale", "en_us");
        int viewDistance = JsonUtil.getIntOrDefault(params, "viewDistance", 12);
        int chatMode = JsonUtil.getIntOrDefault(params, "chatMode", 0);
        boolean chatColors = JsonUtil.getBooleanOrDefault(params, "chatColors", true);
        int skinParts = JsonUtil.getIntOrDefault(params, "skinParts", 127);
        int mainHand = JsonUtil.getIntOrDefault(params, "mainHand", 1);
        boolean textFiltering = JsonUtil.getBooleanOrDefault(params, "textFiltering", false);
        boolean allowServerListings = JsonUtil.getBooleanOrDefault(params, "allowServerListings", true);

        HumanoidArm arm = (mainHand == 0) ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        ChatVisiblity vis;
        switch (chatMode) {
            case 0: vis = ChatVisiblity.FULL; break;
            case 1: vis = ChatVisiblity.SYSTEM; break;
            case 2: vis = ChatVisiblity.HIDDEN; break;
            default: return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid chatMode: " + chatMode);
        }

        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();

        try {
            conn.send(new ServerboundClientInformationPacket(
                    locale, viewDistance, vis, chatColors, skinParts, arm, textFiltering, allowServerListings));
            return ActionResult.ok(new JsonObject());
        } catch (Exception e) {
            return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE,
                    "Failed to send client information: " + e.getMessage());
        }
    }
}
