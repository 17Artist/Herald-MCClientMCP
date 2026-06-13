package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync check: evaluate a simple condition against current player state.
 * Returns ok if condition is met NOW, error ASSERTION_FAILED if not.
 *
 * Supported conditions:
 *   health>N, health<N, food>N, food<N, y>N, y<N, in_water, on_ground
 */
public final class WaitConditionAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String condition = JsonUtil.requireString(params, "condition");
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        boolean met = evaluate(condition, player);
        if (met) {
            JsonObject data = new JsonObject();
            data.addProperty("condition", condition);
            data.addProperty("met", true);
            return ActionResult.ok(data);
        }

        JsonObject state = new JsonObject();
        state.addProperty("health", player.getHealth());
        state.addProperty("food", player.getFoodData().getFoodLevel());
        state.addProperty("y", player.getY());
        state.addProperty("in_water", player.isInWater());
        state.addProperty("on_ground", player.onGround());
        return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Condition not met: " + condition + " | state=" + state);
    }

    private boolean evaluate(String condition, LocalPlayer player) {
        condition = condition.trim().toLowerCase();

        if ("in_water".equals(condition)) return player.isInWater();
        if ("on_ground".equals(condition)) return player.onGround();

        if (condition.startsWith("health>")) return player.getHealth() > parseNum(condition, 7);
        if (condition.startsWith("health<")) return player.getHealth() < parseNum(condition, 7);
        if (condition.startsWith("food>")) return player.getFoodData().getFoodLevel() > parseNum(condition, 5);
        if (condition.startsWith("food<")) return player.getFoodData().getFoodLevel() < parseNum(condition, 5);
        if (condition.startsWith("y>")) return player.getY() > parseNum(condition, 2);
        if (condition.startsWith("y<")) return player.getY() < parseNum(condition, 2);

        // Unknown condition — treat as not met
        return false;
    }

    private double parseNum(String condition, int offset) {
        try {
            return Double.parseDouble(condition.substring(offset));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}