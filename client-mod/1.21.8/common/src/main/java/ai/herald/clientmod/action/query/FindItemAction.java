package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * find_item — searches player inventory for items matching a partial ID.
 * Params: itemId(string, partial match), where?(inventory/hotbar/armor/all, default all)
 */
public final class FindItemAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String itemId = JsonUtil.requireString(params, "itemId").toLowerCase();
        String where = JsonUtil.getStringOrDefault(params, "where", "all");

        Inventory inventory = player.getInventory();
        JsonArray results = new JsonArray();

        int startSlot;
        int endSlot;
        switch (where) {
            case "hotbar":
                startSlot = 0;
                endSlot = 8;
                break;
            case "armor":
                startSlot = 36;
                endSlot = 39;
                break;
            case "inventory":
                startSlot = 9;
                endSlot = 35;
                break;
            default: // "all"
                startSlot = 0;
                endSlot = 40;
                break;
        }

        for (int i = startSlot; i <= endSlot; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String idStr = id != null ? id.toString() : "";

            if (idStr.contains(itemId)) {
                JsonObject entry = new JsonObject();
                entry.addProperty("slot", i);
                entry.addProperty("itemId", idStr);
                entry.addProperty("count", stack.getCount());
                entry.addProperty("displayName", stack.getHoverName().getString());
                results.add(entry);
            }
        }

        JsonObject data = new JsonObject();
        data.add("items", results);
        data.addProperty("count", results.size());
        return ActionResult.ok(data);
    }
}
