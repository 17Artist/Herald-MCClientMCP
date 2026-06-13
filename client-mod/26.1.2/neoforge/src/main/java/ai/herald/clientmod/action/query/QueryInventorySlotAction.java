package ai.herald.clientmod.action.query;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.ItemStackSerializer;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Port of BlackBoxPro QueryInventorySlotAction.kt to Java + Mojang 1.20.1. */
public final class QueryInventorySlotAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        Inventory inventory = player.getInventory();
        int slot = JsonUtil.getIntOrDefault(params, "slot", McVersionCompat.getSelectedSlot(inventory));

        if (slot < 0 || slot > 40) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Invalid slot: " + slot + " (expected 0-40)");
        }

        ItemStack stack = inventory.getItem(slot);
        JsonObject data = ItemStackSerializer.serialize(stack);
        data.addProperty("slot", slot);
        return ActionResult.ok(data);
    }
}
