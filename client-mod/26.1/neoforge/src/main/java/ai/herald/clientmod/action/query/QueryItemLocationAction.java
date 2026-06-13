package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * query_item_location — searches player inventory AND open container for matching items.
 * Params: itemId(string, partial match), container?(boolean, check open container too)
 */
public final class QueryItemLocationAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String itemId = JsonUtil.requireString(params, "itemId").toLowerCase();
        boolean checkContainer = JsonUtil.getBooleanOrDefault(params, "container", false);

        // Search player inventory
        JsonArray inventoryResults = new JsonArray();
        Inventory inventory = player.getInventory();
        for (int i = 0; i <= 40; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String idStr = id != null ? id.toString() : "";

            if (idStr.contains(itemId)) {
                JsonObject entry = new JsonObject();
                entry.addProperty("slot", i);
                entry.addProperty("itemId", idStr);
                entry.addProperty("count", stack.getCount());
                inventoryResults.add(entry);
            }
        }

        // Search open container if requested
        JsonArray containerResults = new JsonArray();
        if (checkContainer) {
            AbstractContainerMenu containerMenu = player.containerMenu;
            // Only search if a container is open (not the default player inventory)
            if (containerMenu != player.inventoryMenu) {
                int containerSize = containerMenu.slots.size();
                // Container slots are typically before the player inventory slots
                // The player inventory takes the last 36 slots (9 main rows + 9 hotbar at end)
                int playerSlotsStart = containerSize - 36;
                int searchEnd = Math.max(0, playerSlotsStart);

                for (int i = 0; i < searchEnd; i++) {
                    Slot slot = containerMenu.slots.get(i);
                    ItemStack stack = slot.getItem();
                    if (stack.isEmpty()) continue;

                    Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    String idStr = id != null ? id.toString() : "";

                    if (idStr.contains(itemId)) {
                        JsonObject entry = new JsonObject();
                        entry.addProperty("slot", i);
                        entry.addProperty("itemId", idStr);
                        entry.addProperty("count", stack.getCount());
                        containerResults.add(entry);
                    }
                }
            }
        }

        JsonObject data = new JsonObject();
        data.add("inventory", inventoryResults);
        data.add("container", containerResults);
        data.addProperty("containerOpen", checkContainer && player.containerMenu != player.inventoryMenu);
        return ActionResult.ok(data);
    }
}
