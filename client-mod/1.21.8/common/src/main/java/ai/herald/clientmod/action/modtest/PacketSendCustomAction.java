package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.ReflectiveGamePackets;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.Base64;

public class PacketSendCustomAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in game");
        }

        String channel = JsonUtil.getStringOrDefault(params, "channel", null);
        String dataStr = JsonUtil.getStringOrDefault(params, "data", null);

        if (channel == null || channel.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "channel is required");
        }
        if (dataStr == null || dataStr.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "data is required (base64 or hex)");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dataStr);
        } catch (IllegalArgumentException e) {
            try {
                bytes = hexToBytes(dataStr);
            } catch (Exception ex) {
                return ActionResult.error(ErrorCode.INVALID_PARAMS, "data must be valid base64 or hex string");
            }
        }

        ResourceLocation resourceLocation = ResourceLocation.tryParse(channel);
        if (resourceLocation == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid channel: " + channel);
        }

        ActionResult sent = ReflectiveGamePackets.sendCustomPayload(player.connection, resourceLocation, bytes);
        if (!sent.isSuccess()) {
            return sent;
        }

        JsonObject data = new JsonObject();
        data.addProperty("sent", true);
        data.addProperty("channel", channel);
        data.addProperty("dataSize", bytes.length);
        return ActionResult.ok(data);
    }

    private static byte[] hexToBytes(String hex) {
        hex = hex.replaceAll("\\s", "");
        if (hex.length() % 2 != 0) throw new IllegalArgumentException("Invalid hex length");
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
}
