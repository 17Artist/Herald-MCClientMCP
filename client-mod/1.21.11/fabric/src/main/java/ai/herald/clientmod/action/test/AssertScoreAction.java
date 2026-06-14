package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

/**
 * Asserts that a player's score on a given objective is within the specified min/max range.
 */
public final class AssertScoreAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        LocalPlayer player = McHelper.player();
        if (level == null || player == null) return McHelper.notInGame();

        String objectiveName = JsonUtil.requireString(params, "objective");
        String targetPlayer = JsonUtil.getStringOrDefault(params, "player", null);
        int min = JsonUtil.getIntOrDefault(params, "min", Integer.MIN_VALUE);
        int max = JsonUtil.getIntOrDefault(params, "max", Integer.MAX_VALUE);

        if (targetPlayer == null) {
            targetPlayer = player.getScoreboardName();
        }

        Scoreboard scoreboard = level.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Objective '" + objectiveName + "' does not exist");
        }

        // 1.20.1: check if there is a score entry for this player + objective
        if (!McVersionCompat.hasPlayerScore(scoreboard, targetPlayer, objective)) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Player '" + targetPlayer + "' has no score for objective '" + objectiveName + "'");
        }

        int value = McVersionCompat.getPlayerScore(scoreboard, targetPlayer, objective);
        if (value < min || value > max) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Expected score for '" + targetPlayer + "' on '" + objectiveName
                    + "' in range [" + min + ", " + max + "] but got " + value);
        }

        JsonObject data = new JsonObject();
        data.addProperty("pass", true);
        data.addProperty("message", "Score for '" + targetPlayer + "' on '" + objectiveName + "' is " + value);
        return ActionResult.ok(data);
    }
}
