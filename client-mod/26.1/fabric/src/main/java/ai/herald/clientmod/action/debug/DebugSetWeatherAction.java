package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import ai.herald.clientmod.protocol.ErrorCode;
import net.minecraft.client.player.LocalPlayer;

public final class DebugSetWeatherAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String weather = JsonUtil.requireString(params, "weather");

        String w = weather.toLowerCase();
        if (!w.equals("clear") && !w.equals("rain") && !w.equals("thunder")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown weather: " + weather + ". Use: clear, rain, thunder");
        }

        player.connection.sendCommand("weather " + w);
        JsonObject data = new JsonObject();
        data.addProperty("weather", weather);
        data.addProperty("method", "command");
        return ActionResult.ok(data);
    }
}
