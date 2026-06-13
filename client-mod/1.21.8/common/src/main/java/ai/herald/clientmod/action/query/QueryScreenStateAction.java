package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Port of BlackBoxPro QueryScreenStateAction.kt to Java + Mojang 1.20.1.
 *
 * <p>Screen classification uses simple-name matching to avoid coupling to
 * every individual screen class import (and to be resilient if a class
 * is missing on a given runtime).</p>
 */
public final class QueryScreenStateAction implements ActionExecutor {

    private static final Map<String, String> CLASSIFY = new HashMap<>();
    static {
        CLASSIFY.put("InventoryScreen", "player_inventory");
        CLASSIFY.put("CreativeModeInventoryScreen", "creative_inventory");
        CLASSIFY.put("ContainerScreen", "generic_container");
        CLASSIFY.put("Dispenser3x3ContainerScreen", "generic_3x3");
        CLASSIFY.put("ShulkerBoxScreen", "shulker_box");
        CLASSIFY.put("CraftingScreen", "crafting_table");
        CLASSIFY.put("FurnaceScreen", "furnace");
        CLASSIFY.put("SmokerScreen", "smoker");
        CLASSIFY.put("BlastFurnaceScreen", "blast_furnace");
        CLASSIFY.put("BrewingStandScreen", "brewing_stand");
        CLASSIFY.put("AnvilScreen", "anvil");
        CLASSIFY.put("EnchantmentScreen", "enchanting_table");
        CLASSIFY.put("GrindstoneScreen", "grindstone");
        CLASSIFY.put("LoomScreen", "loom");
        CLASSIFY.put("CartographyTableScreen", "cartography_table");
        CLASSIFY.put("StonecutterScreen", "stonecutter");
        CLASSIFY.put("SmithingScreen", "smithing_table");
        CLASSIFY.put("MerchantScreen", "villager_trade");
        CLASSIFY.put("HopperScreen", "hopper");
        CLASSIFY.put("BeaconScreen", "beacon");
        CLASSIFY.put("HorseInventoryScreen", "horse");
        CLASSIFY.put("BookViewScreen", "book");
        CLASSIFY.put("BookEditScreen", "book_edit");
    }

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        Screen screen = mc.screen;
        String simple = screen != null ? screen.getClass().getSimpleName() : null;
        boolean disconnected = "DisconnectedScreen".equals(simple);

        JsonObject data = new JsonObject();
        data.addProperty("open", screen != null);
        data.addProperty("screenClass", simple != null ? simple : "none");
        data.addProperty("title",
            screen != null && screen.getTitle() != null ? screen.getTitle().getString() : "");

        if (screen instanceof AbstractContainerScreen<?>) {
            data.addProperty("isContainer", true);
            LocalPlayer player = mc.player;
            if (player != null) {
                AbstractContainerMenu handler = player.containerMenu;
                data.addProperty("windowId", handler.containerId);
                data.addProperty("slotCount", handler.slots.size());
            }
        } else {
            data.addProperty("isContainer", false);
        }

        data.addProperty("screenType",
            disconnected ? "disconnected" : classifyScreen(screen, simple));

        if (disconnected) {
            String reason = readTextField(screen, "reason", "f_96306_");
            if (reason != null) data.addProperty("reason", reason);
            String details = readTextField(screen, "details", "info", "f_96307_");
            if (details != null) data.addProperty("details", details);
        }

        return ActionResult.ok(data);
    }

    private static String classifyScreen(Screen screen, String simple) {
        if (screen == null) return "none";
        if (simple != null && CLASSIFY.containsKey(simple)) return CLASSIFY.get(simple);
        if (screen instanceof AbstractContainerScreen<?>) return "container_unknown";
        return "other";
    }

    private static String readTextField(Object target, String... fieldNames) {
        if (target == null) return null;
        for (String name : fieldNames) {
            for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    Object val = f.get(target);
                    if (val != null) {
                        String s = val.toString();
                        if (!s.isBlank()) return s;
                    }
                } catch (NoSuchFieldException ignored) {
                    // try parent class
                } catch (Throwable t) {
                    // ignore and try next field name
                    break;
                }
            }
        }
        // fallback: any Component-typed field
        try {
            for (Field field : target.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object val = field.get(target);
                if (val != null && val.getClass().getName().toLowerCase().contains("component")) {
                    String s = val.toString();
                    if (!s.isBlank()) return s;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
