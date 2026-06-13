package ai.herald.clientmod.action.container;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;


/** Port of BlackBoxPro container/PickItemAction.kt. */
public final class PickItemAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();
        ai.herald.clientmod.util.McVersionCompat.sendPickItemPacket(conn, McVersionCompat.getSelectedSlot(player.getInventory()));
        return ActionResult.ok();
    }
}
