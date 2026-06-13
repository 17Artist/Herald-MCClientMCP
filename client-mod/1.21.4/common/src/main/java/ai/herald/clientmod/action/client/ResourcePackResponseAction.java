package ai.herald.clientmod.action.client;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.ReflectiveGamePackets;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.UUID;

/**
 * Port of BlackBoxPro client/ResourcePackResponseAction.kt — 1.20.1 supports only 4
 * statuses (no UUID, no DOWNLOADED/INVALID_URL/DISCARDED — those are 1.20.3+).
 */
public final class ResourcePackResponseAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String resultStr = JsonUtil.requireString(params, "result").toLowerCase();
        String actionName;
        switch (resultStr) {
            case "accepted": actionName = "ACCEPTED"; break;
            case "declined": actionName = "DECLINED"; break;
            case "successfully_loaded": actionName = "SUCCESSFULLY_LOADED"; break;
            case "failed_download": actionName = "FAILED_DOWNLOAD"; break;
            case "downloaded": actionName = "DOWNLOADED"; break;
            case "invalid_url": actionName = "INVALID_URL"; break;
            case "failed_reload": actionName = "FAILED_RELOAD"; break;
            case "discarded": actionName = "DISCARDED"; break;
            default:
                return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown result: " + resultStr);
        }
        UUID packId = null;
        String uuidStr = JsonUtil.getStringOrDefault(params, "uuid", "");
        if (!uuidStr.isEmpty()) {
            try {
                packId = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid uuid: " + uuidStr);
            }
        }
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        return ReflectiveGamePackets.sendResourcePackResponse(conn, actionName, packId);
    }
}
