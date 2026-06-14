package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: Return basic connection stats — connection status, protocol version, address.
 */
public final class QueryPacketStatsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            return McHelper.notInGame();
        }

        boolean connected = connection.getConnection().isConnected();
        String address = connection.getConnection().getRemoteAddress() != null
                ? connection.getConnection().getRemoteAddress().toString()
                : "unknown";

        JsonObject data = new JsonObject();
        data.addProperty("connected", connected);
        data.addProperty("address", address);
        return ActionResult.ok(data);
    }
}
