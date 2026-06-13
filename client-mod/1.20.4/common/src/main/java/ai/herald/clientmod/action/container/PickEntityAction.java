package ai.herald.clientmod.action.container;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;


/** Port of BlackBoxPro container/PickEntityAction.kt. */
public final class PickEntityAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int entityId = JsonUtil.requireInt(params, "entityId");
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || level == null || conn == null) return McHelper.notInGame();
        if (level.getEntity(entityId) == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No entity with id " + entityId);
        }
        ai.herald.clientmod.util.McVersionCompat.sendPickItemPacket(conn, McVersionCompat.getSelectedSlot(player.getInventory()));
        return ActionResult.ok();
    }
}
