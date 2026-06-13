package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Sync: Query the current status of an open brewing stand.
 * No params. Returns brewing progress, fuel, and bottle slot contents.
 */
public final class BrewQueryStatusAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        if (!(player.containerMenu instanceof BrewingStandMenu menu)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No brewing stand GUI is open");
        }

        // Data slot 0 = brew time remaining (ticks), slot 1 = fuel
        int brewTime = McVersionCompat.getMenuData(menu, "brewingStandData", 0);
        int fuel = McVersionCompat.getMenuData(menu, "brewingStandData", 1);

        JsonObject data = new JsonObject();
        data.addProperty("brewing", brewTime > 0);
        data.addProperty("progress", brewTime);
        data.addProperty("fuel", fuel);

        JsonArray slots = new JsonArray();
        for (int i = 0; i < 3; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            JsonObject slotObj = new JsonObject();
            slotObj.addProperty("slot", "bottle" + i);
            if (!stack.isEmpty()) {
                Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                slotObj.addProperty("item", key.toString());
                slotObj.addProperty("count", stack.getCount());
            } else {
                slotObj.addProperty("item", "empty");
            }
            slots.add(slotObj);
        }
        data.add("bottles", slots);

        // Ingredient slot
        ItemStack ingredient = menu.getSlot(3).getItem();
        if (!ingredient.isEmpty()) {
            Identifier iKey = BuiltInRegistries.ITEM.getKey(ingredient.getItem());
            data.addProperty("ingredient", iKey.toString());
        }

        return ActionResult.ok(data);
    }
}
