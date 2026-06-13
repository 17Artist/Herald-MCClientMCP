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
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/**
 * Sync: Place fuel into a furnace fuel slot (slot 1).
 * Params: itemSlot (int, player inventory slot)
 *
 * Convenience action that always targets the fuel slot.
 */
public final class SmeltPlaceFuelAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();

        int itemSlot = JsonUtil.requireInt(params, "itemSlot");

        if (!(player.containerMenu instanceof AbstractFurnaceMenu menu)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No furnace GUI is open");
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
                containerId, stateId, srcContainerSlot, 0, ClickType.PICKUP,
                ItemStack.EMPTY, c1);

        // Place into fuel slot (index 1)
        Int2ObjectMap<ItemStack> c2 = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId + 1, 1, 0, ClickType.PICKUP,
                ItemStack.EMPTY, c2);

        // Put remainder back
        Int2ObjectMap<ItemStack> c3 = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId + 2, srcContainerSlot, 0, ClickType.PICKUP,
                ItemStack.EMPTY, c3);

        JsonObject data = new JsonObject();
        data.addProperty("placed", true);
        data.addProperty("targetSlot", "fuel");
        return ActionResult.ok(data);
    }

    private static int playerSlotToFurnaceSlot(int raw) {
        if (raw >= 0 && raw <= 8) return raw + 30;
        if (raw >= 9 && raw <= 35) return raw - 6;
        return -1;
    }
}
