package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;

public final class DebugSetTimeAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String time = JsonUtil.requireString(params, "time");

        Minecraft mc = McHelper.mc();

        if (mc.getSingleplayerServer() != null) {
            long ticks = parseTime(time);
            ServerLevel overworld = mc.getSingleplayerServer().overworld();
            overworld.setDayTime(ticks);

            JsonObject data = new JsonObject();
            data.addProperty("time", time);
            data.addProperty("ticks", ticks);
            return ActionResult.ok(data);
        } else {
            player.connection.sendCommand("time set " + time);
            JsonObject data = new JsonObject();
            data.addProperty("command", "time set " + time);
            return ActionResult.ok(data);
        }
    }

    private static long parseTime(String time) {
        switch (time.toLowerCase()) {
            case "day": return 1000;
            case "noon": return 6000;
            case "sunset": return 12000;
            case "night": return 13000;
            case "midnight": return 18000;
            case "sunrise": return 23000;
            default:
                try { return Long.parseLong(time); }
                catch (NumberFormatException e) { return 0; }
        }
    }
}
