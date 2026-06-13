package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.ReflectiveGamePackets;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.Identifier;

import java.util.Base64;

/**
 * Port of BlackBoxPro debug/CustomPayloadAction.kt — 1.20.1 ServerboundCustomPayloadPacket
 * takes a (Identifier, FriendlyByteBuf) directly; the CustomPacketPayload
 * codec system used in 1.20.2+ does not exist here.
 */
public final class CustomPayloadAction implements ActionExecutor {

    private static final int MAX_SIZE = 32768;

    @Override
    public ActionResult execute(JsonObject params) {
        String channel = JsonUtil.requireString(params, "channel");
        String dataB64 = JsonUtil.requireString(params, "data");
        byte[] data;
        try {
            data = Base64.getDecoder().decode(dataB64);
        } catch (IllegalArgumentException e) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid base64 data: " + e.getMessage());
        }
        if (data.length > MAX_SIZE) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Payload too large: " + data.length);
        }
        Identifier id = Identifier.tryParse(channel);
        if (id == null) return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid channel: " + channel);

        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();

        return ReflectiveGamePackets.sendCustomPayload(conn, id, data);
    }
}
