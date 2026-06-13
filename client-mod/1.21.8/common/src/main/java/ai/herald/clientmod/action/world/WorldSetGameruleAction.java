package ai.herald.clientmod.action.world;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

public class WorldSetGameruleAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in game");
        }

        String rule = JsonUtil.getStringOrDefault(params, "rule", null);
        String value = JsonUtil.getStringOrDefault(params, "value", null);

        if (rule == null || rule.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "rule is required");
        }
        if (value == null || value.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "value is required");
        }

        player.connection.sendCommand("gamerule " + rule + " " + value);

        JsonObject data = new JsonObject();
        data.addProperty("rule", rule);
        data.addProperty("value", value);
        data.addProperty("executed", true);
        return ActionResult.ok(data);
    }
}
