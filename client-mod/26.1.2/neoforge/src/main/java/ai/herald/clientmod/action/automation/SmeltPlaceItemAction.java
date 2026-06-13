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

import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/**
 * Sync: Place an item into a furnace input or fuel slot.
 * Params: slot (string: "input" or "fuel"), itemSlot (int, player inventory slot)
 *
 * Furnace menu layout:
 *   0 = input
 *   1 = fuel
 *   2 = output
 *   3-29 = player main inventory
 *   30-38 = player hotbar
 */
public final class SmeltPlaceItemAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();

        String slotName = JsonUtil.requireString(params, "slot");
        int itemSlot = JsonUtil.requireInt(params, "itemSlot");

        if (!(player.containerMenu instanceof AbstractFurnaceMenu menu)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No furnace GUI is open");
        }

        int targetIdx = switch (slotName.toLowerCase()) {
            case "input" -> 0;
            case "fuel" -> 1;
            default -> -1;
        };
        if (targetIdx == -1) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "slot must be 'input' or 'fuel'");
        }

        int srcContainerSlot = playerSlotToFurnaceSlot(itemSlot);
        if (srcContainerSlot == -1) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid itemSlot: " + itemSlot);
        }

        int containerId = menu.containerId;
        int stateId = menu.getStateId();

        // Pick up from player inventory
        Int2ObjectMap<ItemStack> c1 = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId, srcContainerSlot, 0, ContainerInput.PICKUP,
                ItemStack.EMPTY, c1);

        // Place into furnace slot
        Int2ObjectMap<ItemStack> c2 = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId + 1, targetIdx, 0, ContainerInput.PICKUP,
                ItemStack.EMPTY, c2);

        // Put remainder back
        Int2ObjectMap<ItemStack> c3 = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId + 2, srcContainerSlot, 0, ContainerInput.PICKUP,
                ItemStack.EMPTY, c3);

        JsonObject data = new JsonObject();
        data.addProperty("placed", true);
        data.addProperty("targetSlot", slotName);
        return ActionResult.ok(data);
    }

    /**
     * Maps raw player inventory slot to furnace container slot.
     * Furnace layout: 0=input, 1=fuel, 2=output, 3-29=main inv, 30-38=hotbar.
     */
    private static int playerSlotToFurnaceSlot(int raw) {
        if (raw >= 0 && raw <= 8) return raw + 30;   // hotbar -> 30-38
        if (raw >= 9 && raw <= 35) return raw - 6;   // main inv -> 3-29
        return -1;
    }
}
