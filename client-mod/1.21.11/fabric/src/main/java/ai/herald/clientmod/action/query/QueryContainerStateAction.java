package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.ItemStackSerializer;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Port of BlackBoxPro QueryContainerStateAction.kt to Java + Mojang 1.20.1. */
public final class QueryContainerStateAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        LocalPlayer player = mc.player;
        if (player == null) return McHelper.notInGame();

        AbstractContainerMenu handler = player.containerMenu;
        boolean isInventoryOnly = handler == player.inventoryMenu;

        JsonObject data = new JsonObject();
        data.addProperty("open", !isInventoryOnly);
        data.addProperty("windowId", handler.containerId);
        data.addProperty("stateId", handler.getStateId());

        String typeStr = "unknown";
        try {
            MenuType<?> type = handler.getType();
            if (type != null) {
                Identifier id = BuiltInRegistries.MENU.getKey(type);
                if (id != null) typeStr = id.toString();
            }
        } catch (Throwable ignored) {
            // some menus (player inventory) throw on getType()
        }
        data.addProperty("type", typeStr);
        data.addProperty("slotCount", handler.slots.size());

        Screen screen = mc.screen;
        if (screen instanceof AbstractContainerScreen<?> acs) {
            data.addProperty("title", acs.getTitle().getString());
        }

        data.add("carriedItem", ItemStackSerializer.serializeSlot(handler.getCarried()));
        return ActionResult.ok(data);
    }
}
