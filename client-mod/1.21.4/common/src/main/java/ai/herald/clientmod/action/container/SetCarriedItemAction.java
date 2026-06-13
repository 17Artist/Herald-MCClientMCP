package ai.herald.clientmod.action.container;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

/**
 * Port of BlackBoxPro container/SetCarriedItemAction.kt.
 *
 * <p>We must update the local {@code Inventory.selected} index in addition to
 * sending the packet — otherwise the server moves the held item but the client
 * still renders + reports the previous slot until something else triggers a
 * refresh.
 */
public final class SetCarriedItemAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int slot = JsonUtil.requireInt(params, "slot");
        if (slot < 0 || slot > 8) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Slot must be 0..8, got " + slot);
        }
        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();

        // Local mirror — keeps query_held_item / rendering / mainhand item in sync.
        if (McVersionCompat.getSelectedSlot(player.getInventory()) != slot) {
            McVersionCompat.setSelectedSlot(player.getInventory(), slot);
        }
        conn.send(new ServerboundSetCarriedItemPacket(slot));
        return ActionResult.ok();
    }
}
