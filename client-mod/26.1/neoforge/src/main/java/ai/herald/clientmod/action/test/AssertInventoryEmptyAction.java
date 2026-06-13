package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Asserts that a specific inventory slot is empty.
 */
public final class AssertInventoryEmptyAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int slot = JsonUtil.requireInt(params, "slot");
        if (slot < 0 || slot > 40) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid slot: " + slot + " (expected 0-40)");
        }

        Inventory inventory = player.getInventory();
        ItemStack stack = inventory.getItem(slot);

        if (!stack.isEmpty()) {
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String idStr = id != null ? id.toString() : "unknown";
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Expected slot " + slot + " to be empty but contains " + stack.getCount() + "x " + idStr);
        }

        JsonObject data = new JsonObject();
        data.addProperty("pass", true);
        data.addProperty("message", "Slot " + slot + " is empty");
        return ActionResult.ok(data);
    }
}
