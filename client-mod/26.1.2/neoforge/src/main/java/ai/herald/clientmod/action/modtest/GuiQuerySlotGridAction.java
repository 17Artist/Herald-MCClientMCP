package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * If the current screen is a container screen, lists all slots with positions and item info.
 */
public final class GuiQuerySlotGridAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        Screen screen = mc.screen;
        if (screen == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "No screen is currently open");
        }
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Current screen is not a container screen");
        }

        AbstractContainerMenu menu = containerScreen.getMenu();
        JsonArray slots = new JsonArray();

        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            JsonObject slotObj = new JsonObject();
            slotObj.addProperty("slotIndex", i);
            slotObj.addProperty("x", slot.x);
            slotObj.addProperty("y", slot.y);

            ItemStack stack = slot.getItem();
            boolean hasItem = !stack.isEmpty();
            slotObj.addProperty("hasItem", hasItem);
            if (hasItem) {
                Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                slotObj.addProperty("itemId", id.toString());
                slotObj.addProperty("count", stack.getCount());
            }
            slots.add(slotObj);
        }

        JsonObject data = new JsonObject();
        data.add("slots", slots);
        data.addProperty("slotCount", menu.slots.size());
        data.addProperty("windowId", menu.containerId);
        return ActionResult.ok(data);
    }
}
