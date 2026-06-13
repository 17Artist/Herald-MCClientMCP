package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Asserts that the player's inventory contains at least minCount of the given item.
 * If slot is specified (>= 0), only checks that specific slot.
 */
public final class AssertInventoryContainsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String itemId = JsonUtil.requireString(params, "itemId");
        int minCount = JsonUtil.getIntOrDefault(params, "minCount", 1);
        int slot = JsonUtil.getIntOrDefault(params, "slot", -1);

        Inventory inventory = player.getInventory();

        if (slot >= 0) {
            if (slot > 40) {
                return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid slot: " + slot);
            }
            ItemStack stack = inventory.getItem(slot);
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String idStr = id != null ? id.toString() : "empty";
            if (stack.isEmpty() || !idStr.equals(itemId) || stack.getCount() < minCount) {
                return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                    "Expected slot " + slot + " to contain " + minCount + "x " + itemId
                        + " but got " + (stack.isEmpty() ? "empty" : stack.getCount() + "x " + idStr));
            }
            JsonObject data = new JsonObject();
            data.addProperty("pass", true);
            data.addProperty("message", "Slot " + slot + " contains " + stack.getCount() + "x " + itemId);
            return ActionResult.ok(data);
        }

        // Search all slots
        int totalCount = 0;
        for (int i = 0; i <= 40; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null && id.toString().equals(itemId)) {
                totalCount += stack.getCount();
            }
        }

        if (totalCount < minCount) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Expected at least " + minCount + "x " + itemId + " in inventory but found " + totalCount);
        }

        JsonObject data = new JsonObject();
        data.addProperty("pass", true);
        data.addProperty("message", "Inventory contains " + totalCount + "x " + itemId);
        return ActionResult.ok(data);
    }
}
