package ai.herald.clientmod.action.query;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.ItemStackSerializer;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class QueryFullInventoryAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        Inventory inventory = player.getInventory();
        JsonArray slots = new JsonArray();

        for (int i = 0; i <= 40; i++) {
            ItemStack stack = inventory.getItem(i);
            JsonObject slot = ItemStackSerializer.serializeSlot(stack);
            slot.addProperty("slot", i);
            slot.addProperty("slotName", slotName(i));
            slots.add(slot);
        }

        JsonObject data = new JsonObject();
        data.add("slots", slots);
        data.addProperty("selectedSlot", McVersionCompat.getSelectedSlot(inventory));
        return ActionResult.ok(data);
    }

    private static String slotName(int slot) {
        if (slot >= 0 && slot <= 8) return "hotbar_" + slot;
        if (slot >= 9 && slot <= 35) return "inventory_" + (slot - 9);
        if (slot == 36) return "armor_feet";
        if (slot == 37) return "armor_legs";
        if (slot == 38) return "armor_chest";
        if (slot == 39) return "armor_head";
        if (slot == 40) return "offhand";
        return "unknown_" + slot;
    }
}
