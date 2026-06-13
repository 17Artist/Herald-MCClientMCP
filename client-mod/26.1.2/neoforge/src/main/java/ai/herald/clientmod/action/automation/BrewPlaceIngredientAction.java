package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/**
 * Sync: Place an ingredient or fuel into a brewing stand slot.
 * Params: itemSlot (int, player inv slot), targetSlot (string: ingredient/fuel/bottle0/bottle1/bottle2)
 *
 * Brewing stand menu layout:
 *   0 = bottle slot 0
 *   1 = bottle slot 1
 *   2 = bottle slot 2
 *   3 = ingredient
 *   4 = fuel (blaze powder)
 *   5-31 = player main inventory
 *   32-40 = player hotbar
 */
public final class BrewPlaceIngredientAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();

        int itemSlot = JsonUtil.requireInt(params, "itemSlot");
        String targetSlotName = JsonUtil.requireString(params, "targetSlot");

        if (!(player.containerMenu instanceof BrewingStandMenu menu)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No brewing stand GUI is open");
        }

        int targetIdx = mapTargetSlot(targetSlotName);
        if (targetIdx == -1) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Invalid targetSlot: " + targetSlotName + ". Use: ingredient, fuel, bottle0, bottle1, bottle2");
        }

        int srcContainerSlot = playerSlotToBrewSlot(itemSlot);
        if (srcContainerSlot == -1) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid itemSlot: " + itemSlot);
        }

        int containerId = menu.containerId;
        int stateId = menu.getStateId();

        // Pick up item from player inventory slot
        Int2ObjectMap<ItemStack> c1 = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId, srcContainerSlot, 0, ContainerInput.PICKUP,
                ItemStack.EMPTY, c1);

        // Place into target slot
        Int2ObjectMap<ItemStack> c2 = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId + 1, targetIdx, 0, ContainerInput.PICKUP,
                ItemStack.EMPTY, c2);

        // Put back any remainder
        Int2ObjectMap<ItemStack> c3 = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId + 2, srcContainerSlot, 0, ContainerInput.PICKUP,
                ItemStack.EMPTY, c3);

        JsonObject data = new JsonObject();
        data.addProperty("placed", true);
        data.addProperty("targetSlot", targetSlotName);
        return ActionResult.ok(data);
    }

    private static int mapTargetSlot(String name) {
        return switch (name.toLowerCase()) {
            case "bottle0" -> 0;
            case "bottle1" -> 1;
            case "bottle2" -> 2;
            case "ingredient" -> 3;
            case "fuel" -> 4;
            default -> -1;
        };
    }

    /**
     * Maps raw player inventory slot to brewing stand container slot.
     * Brew layout: 0-2 bottles, 3 ingredient, 4 fuel, 5-31 main inv, 32-40 hotbar.
     */
    private static int playerSlotToBrewSlot(int raw) {
        if (raw >= 0 && raw <= 8) return raw + 32;   // hotbar -> 32-40
        if (raw >= 9 && raw <= 35) return raw - 4;   // main inv -> 5-31
        return -1;
    }
}
