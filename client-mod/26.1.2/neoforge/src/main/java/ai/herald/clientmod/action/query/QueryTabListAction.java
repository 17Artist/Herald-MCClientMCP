package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Port of BlackBoxPro QueryTabListAction.kt to Java + Mojang 1.20.1. */
public final class QueryTabListAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientPacketListener net = McHelper.connection();
        if (net == null) return McHelper.notConnected();

        int limit = (int) Math.max(1, Math.min(200, JsonUtil.getIntOrDefault(params, "limit", 100)));

        List<PlayerInfo> all = new ArrayList<>(net.getOnlinePlayers());
        all.sort(Comparator.comparingInt(PlayerInfo::getLatency));
        List<PlayerInfo> taken = all.size() > limit ? all.subList(0, limit) : all;

        ClientLevel level = McHelper.level();
        Scoreboard scoreboard = level != null ? level.getScoreboard() : null;

        JsonArray arr = new JsonArray();
        for (PlayerInfo entry : taken) {
            GameProfile profile = entry.getProfile();
            JsonObject o = new JsonObject();
            o.addProperty("name", profile.name());
            o.addProperty("uuid", profile.id() != null ? profile.id().toString() : "");
            o.addProperty("latency", entry.getLatency());
            GameType gm = entry.getGameMode();
            o.addProperty("gameMode", gm != null ? gm.name() : "unknown");
            Component display = entry.getTabListDisplayName();
            if (display != null) o.addProperty("displayName", display.getString());
            if (scoreboard != null) {
                PlayerTeam team = scoreboard.getPlayersTeam(profile.name());
                if (team != null) o.addProperty("team", team.getName());
            }
            arr.add(o);
        }

        JsonObject data = new JsonObject();
        data.add("players", arr);
        data.addProperty("count", arr.size());
        data.addProperty("total", net.getOnlinePlayers().size());
        return ActionResult.ok(data);
    }
}
