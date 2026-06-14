package ai.herald.clientmod.action.composite;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;

/**
 * Switch hotbar to {@code slot}, then drop one item (or the whole stack
 * if {@code dropStack=true}). Synchronous: emits two C2S packets directly.
 */
public final class DropInventoryAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientPacketListener conn = mc.getConnection();
        if (player == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        if (conn == null)   return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not connected");

        int slot = JsonUtil.requireInt(params, "slot");
        if (slot < 0 || slot > 8) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "slot must be 0..8 (hotbar)");
        }
        boolean dropStack = JsonUtil.getBooleanOrDefault(params, "dropStack", false);

        Inventory inv = player.getInventory();
        if (McVersionCompat.getSelectedSlot(inv) != slot) {
            McVersionCompat.setSelectedSlot(inv, slot);
            conn.send(new ServerboundSetCarriedItemPacket(slot));
        }

        ServerboundPlayerActionPacket.Action action = dropStack
            ? ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS
            : ServerboundPlayerActionPacket.Action.DROP_ITEM;
        conn.send(new ServerboundPlayerActionPacket(action, BlockPos.ZERO, Direction.DOWN));

        JsonObject data = new JsonObject();
        data.addProperty("slot", slot);
        data.addProperty("drop_stack", dropStack);
        return ActionResult.ok(data);
    }
}
