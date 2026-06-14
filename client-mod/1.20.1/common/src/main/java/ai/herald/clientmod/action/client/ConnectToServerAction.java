package ai.herald.clientmod.action.client;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/**
 * Connects to a multiplayer server by IP address.
 * If the player is currently in a world, it will disconnect first then connect.
 */
public final class ConnectToServerAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String ip = JsonUtil.getStringOrDefault(params, "ip", null);
        if (ip == null || ip.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required param: ip");
        }
        String name = JsonUtil.getStringOrDefault(params, "name", ip);

        Minecraft mc = McHelper.mc();
        ServerAddress address = ServerAddress.parseString(ip);
        ServerData serverData = new ServerData(name, ip, false);

        if (mc.level != null) {
            mc.execute(() -> {
                mc.level.disconnect();
                mc.clearLevel();
                mc.setScreen(new TitleScreen());
                mc.execute(() -> {
                    net.minecraft.client.gui.screens.ConnectScreen.startConnecting(
                            mc.screen, mc, address, serverData, false);
                });
            });
        } else {
            mc.execute(() -> {
                net.minecraft.client.gui.screens.ConnectScreen.startConnecting(
                        mc.screen, mc, address, serverData, false);
            });
        }

        JsonObject data = new JsonObject();
        data.addProperty("connecting", true);
        data.addProperty("host", address.getHost());
        data.addProperty("port", address.getPort());
        return ActionResult.ok(data);
    }
}
