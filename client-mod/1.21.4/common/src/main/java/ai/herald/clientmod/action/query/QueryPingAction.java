package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: Get player's latency from PlayerInfo tab list entry.
 */
public final class QueryPingAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            return McHelper.notInGame();
        }

        PlayerInfo info = connection.getPlayerInfo(player.getUUID());
        int latency = info != null ? info.getLatency() : -1;

        JsonObject data = new JsonObject();
        data.addProperty("ping", latency);
        return ActionResult.ok(data);
    }
}
