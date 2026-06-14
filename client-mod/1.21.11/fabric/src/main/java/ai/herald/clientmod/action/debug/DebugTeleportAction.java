package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;

public final class DebugTeleportAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        double x = JsonUtil.requireDouble(params, "x");
        double y = JsonUtil.requireDouble(params, "y");
        double z = JsonUtil.requireDouble(params, "z");

        Minecraft mc = McHelper.mc();

        if (mc.getSingleplayerServer() != null) {
            ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(player.getUUID());
            if (sp != null) {
                sp.teleportTo(x, y, z);
            }
            // Also set client-side position immediately
            player.setPos(x, y, z);

            JsonObject data = new JsonObject();
            data.addProperty("x", x);
            data.addProperty("y", y);
            data.addProperty("z", z);
            data.addProperty("teleported", true);
            return ActionResult.ok(data);
        } else {
            String cmd = "tp @s " + x + " " + y + " " + z;
            player.connection.sendCommand(cmd);
            JsonObject data = new JsonObject();
            data.addProperty("command", cmd);
            return ActionResult.ok(data);
        }
    }
}
