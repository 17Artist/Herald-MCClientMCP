package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Asserts that the player's health is within the given min/max range.
 */
public final class AssertPlayerHealthAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        float min = (float) JsonUtil.getDoubleOrDefault(params, "min", 0.0);
        float max = (float) JsonUtil.getDoubleOrDefault(params, "max", 20.0);

        float health = player.getHealth();

        if (health < min || health > max) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Expected health in range [" + min + ", " + max + "] but got " + health);
        }

        JsonObject data = new JsonObject();
        data.addProperty("pass", true);
        data.addProperty("message", "Player health is " + health + " (in range [" + min + ", " + max + "])");
        return ActionResult.ok(data);
    }
}
