package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Locale;

/** Port of BlackBoxPro QueryScoreboardAction.kt to Java + cross-version scoreboard API. */
public final class QueryScoreboardAction implements ActionExecutor {

    private static final int DISPLAY_SLOT_SIDEBAR = 1;

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        LocalPlayer player = McHelper.player();
        if (level == null || player == null) return McHelper.notInGame();

        Scoreboard scoreboard = level.getScoreboard();
        String objectiveName = JsonUtil.getStringOrDefault(params, "objective", null);

        JsonObject data = new JsonObject();

        JsonArray objectives = new JsonArray();
        for (Objective obj : scoreboard.getObjectives()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", obj.getName());
            o.addProperty("displayName", obj.getDisplayName().getString());
            o.addProperty("criteria", obj.getCriteria().getName());
            o.addProperty("renderType", obj.getRenderType().name().toLowerCase(Locale.ROOT));
            objectives.add(o);
        }
        data.add("objectives", objectives);

        Object sidebarObj = McVersionCompat.getDisplayObjective(scoreboard, DISPLAY_SLOT_SIDEBAR);
        if (sidebarObj instanceof Objective sidebarObjective) {
            JsonObject sidebar = new JsonObject();
            sidebar.addProperty("objectiveName", sidebarObjective.getName());
            sidebar.addProperty("displayName", sidebarObjective.getDisplayName().getString());
            JsonArray scores = new JsonArray();
            for (Object s : McVersionCompat.listPlayerScores(scoreboard, sidebarObjective)) {
                JsonObject so = new JsonObject();
                so.addProperty("name", McVersionCompat.getScoreOwner(s));
                so.addProperty("value", McVersionCompat.getScoreValue(s));
                scores.add(so);
            }
            sidebar.add("scores", scores);
            data.add("sidebar", sidebar);
        }

        if (objectiveName != null) {
            Objective obj = scoreboard.getObjective(objectiveName);
            if (obj != null) {
                JsonArray scores = new JsonArray();
                for (Object s : McVersionCompat.listPlayerScores(scoreboard, obj)) {
                    JsonObject so = new JsonObject();
                    so.addProperty("name", McVersionCompat.getScoreOwner(s));
                    so.addProperty("value", McVersionCompat.getScoreValue(s));
                    scores.add(so);
                }
                data.add("queriedScores", scores);
            }
        }

        PlayerTeam team = scoreboard.getPlayersTeam(player.getScoreboardName());
        if (team != null) {
            JsonObject t = new JsonObject();
            t.addProperty("name", team.getName());
            t.addProperty("displayName", team.getDisplayName().getString());
            t.addProperty("prefix", team.getPlayerPrefix().getString());
            t.addProperty("suffix", team.getPlayerSuffix().getString());
            t.addProperty("color", team.getColor().name());
            data.add("playerTeam", t);
        }

        return ActionResult.ok(data);
    }
}
