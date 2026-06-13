package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import ai.herald.clientmod.protocol.ErrorCode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;

public final class DebugSetWeatherAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String weather = JsonUtil.requireString(params, "weather");

        Minecraft mc = McHelper.mc();

        if (mc.getSingleplayerServer() != null) {
            ServerLevel overworld = mc.getSingleplayerServer().overworld();
            switch (weather.toLowerCase()) {
                case "clear":
                    overworld.setWeatherParameters(6000, 0, false, false);
                    break;
                case "rain":
                    overworld.setWeatherParameters(0, 6000, true, false);
                    break;
                case "thunder":
                    overworld.setWeatherParameters(0, 6000, true, true);
                    break;
                default:
                    return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown weather: " + weather + ". Use: clear, rain, thunder");
            }

            JsonObject data = new JsonObject();
            data.addProperty("weather", weather);
            return ActionResult.ok(data);
        } else {
            player.connection.sendCommand("weather " + weather);
            JsonObject data = new JsonObject();
            data.addProperty("command", "weather " + weather);
            return ActionResult.ok(data);
        }
    }
}
