package ai.herald.clientmod.action.client;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import ai.herald.clientmod.util.ReflectiveGamePackets;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;

/**
 * Port of BlackBoxPro client/ClientInformationAction.kt — 1.20.1 packet takes the
 * eight client-options fields flat (no SyncedClientOptions wrapper, which is 1.20.2+).
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

        return ReflectiveGamePackets.sendClientInformation(
                conn, locale, viewDistance, vis, chatColors, skinParts, arm, textFiltering, allowServerListings);
    }
}
