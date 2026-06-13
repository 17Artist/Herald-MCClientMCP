package ai.herald.clientmod.action.container;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

/** Port of BlackBoxPro container/CloseContainerAction.kt. */
public final class CloseContainerAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientPacketListener conn = mc.getConnection();
        if (player == null || conn == null) return McHelper.notInGame();
        int windowId = JsonUtil.getIntOrDefault(params, "windowId", player.containerMenu.containerId);
        conn.send(new ServerboundContainerClosePacket(windowId));
        mc.execute(() -> mc.setScreen(null));
        return ActionResult.ok();
    }
}
