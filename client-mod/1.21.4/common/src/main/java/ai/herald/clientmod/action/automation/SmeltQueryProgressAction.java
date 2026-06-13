package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Sync: Query smelting progress from an open furnace/blast furnace/smoker.
 * No params. Returns: lit status, burn time remaining, cook progress, and slot items.
 */
public final class SmeltQueryProgressAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        if (!(player.containerMenu instanceof AbstractFurnaceMenu menu)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No furnace GUI is open");
        }

        // Furnace data slots: 0=litTime, 1=litDuration, 2=cookingProgress, 3=cookingTotalTime
        int litTime = menu.data.get(0);
        int litDuration = menu.data.get(1);
        int cookingProgress = menu.data.get(2);
        int cookingTotalTime = menu.data.get(3);

        JsonObject data = new JsonObject();
        data.addProperty("lit", litTime > 0);
        data.addProperty("burnTimeRemaining", litTime);
        data.addProperty("burnTimeTotal", litDuration);
        data.addProperty("cookProgress", cookingProgress);
        data.addProperty("cookTotal", cookingTotalTime);

        // Input slot (0)
        ItemStack input = menu.getSlot(0).getItem();
        if (!input.isEmpty()) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(input.getItem());
            data.addProperty("inputItem", key.toString());
            data.addProperty("inputCount", input.getCount());
        }

        // Fuel slot (1)
        ItemStack fuel = menu.getSlot(1).getItem();
        if (!fuel.isEmpty()) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(fuel.getItem());
            data.addProperty("fuelItem", key.toString());
            data.addProperty("fuelCount", fuel.getCount());
        }

        // Output slot (2)
        ItemStack output = menu.getSlot(2).getItem();
        if (!output.isEmpty()) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(output.getItem());
            data.addProperty("outputItem", key.toString());
            data.addProperty("outputCount", output.getCount());
        }

        return ActionResult.ok(data);
    }
}
