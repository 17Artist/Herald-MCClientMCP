package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: Read client statistics.
 * Note: In 1.20.1 client, detailed statistics require server-side query.
 * The StatsCounter is server-authoritative; client only sees what server sends.
 * Returns a note that statistics require server-side query, or sends /stats command.
 */
public final class QueryStatisticsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        @SuppressWarnings("unused")
        String category = JsonUtil.getStringOrDefault(params, "category", null);
        @SuppressWarnings("unused")
        String stat = JsonUtil.getStringOrDefault(params, "stat", null);

        // In 1.20.1, the client's StatsCounter is populated by the server
        // via ClientboundAwardStatsPacket. We can request it:
        // player.connection.send(new ServerboundClientCommandPacket(
        //     ServerboundClientCommandPacket.Action.REQUEST_STATS));
        // But the response is async and arrives as a packet later.

        JsonObject data = new JsonObject();
        data.addProperty("available", false);
        data.addProperty("note",
                "Statistics require server-side query. Use chat command '/stats' " +
                "or the statistics screen to request data from the server.");
        return ActionResult.ok(data);
    }
}
