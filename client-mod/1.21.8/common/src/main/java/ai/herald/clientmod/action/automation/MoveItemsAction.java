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

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/**
 * move_items — moves items between inventory slots using container clicks.
 * Params: fromSlot(int), toSlot(int), count?(int, all)
 *
 * Uses the player inventory container (inventoryMenu).
 * Slot numbers are raw player inventory slots (0-8 hotbar, 9-35 main, 36-39 armor, 40 offhand).
 */
public final class MoveItemsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();

        int fromSlot = JsonUtil.requireInt(params, "fromSlot");
        int toSlot = JsonUtil.requireInt(params, "toSlot");
        int count = JsonUtil.getIntOrDefault(params, "count", -1);

        int fromContainer = rawToContainerSlot(fromSlot);
        int toContainer = rawToContainerSlot(toSlot);

        if (fromContainer == -1) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid fromSlot: " + fromSlot);
        }
        if (toContainer == -1) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid toSlot: " + toSlot);
        }

        AbstractContainerMenu menu = player.inventoryMenu;
        int containerId = menu.containerId;
        int stateId = menu.getStateId();

        if (count <= 0) {
            // Move entire stack: pick up from source, place at target, place remainder back
            Int2ObjectMap<ItemStack> changed1 = new Int2ObjectOpenHashMap<>();
            McVersionCompat.sendContainerClick(conn,
                    containerId, stateId, fromContainer, 0, ClickType.PICKUP,
                    menu.getCarried(), changed1);

            Int2ObjectMap<ItemStack> changed2 = new Int2ObjectOpenHashMap<>();
            McVersionCompat.sendContainerClick(conn,
                    containerId, stateId + 1, toContainer, 0, ClickType.PICKUP,
                    menu.getCarried(), changed2);

            // If target had items, put them back in source (swap)
            Int2ObjectMap<ItemStack> changed3 = new Int2ObjectOpenHashMap<>();
            McVersionCompat.sendContainerClick(conn,
                    containerId, stateId + 2, fromContainer, 0, ClickType.PICKUP,
                    menu.getCarried(), changed3);
        } else {
            // Move partial stack: right-click pick up places one at a time
            // Pick up full stack first
            Int2ObjectMap<ItemStack> changed1 = new Int2ObjectOpenHashMap<>();
            McVersionCompat.sendContainerClick(conn,
                    containerId, stateId, fromContainer, 0, ClickType.PICKUP,
                    menu.getCarried(), changed1);

            // Right-click to place one at a time
            int placed = 0;
            for (int i = 0; i < count; i++) {
                Int2ObjectMap<ItemStack> changedR = new Int2ObjectOpenHashMap<>();
                McVersionCompat.sendContainerClick(conn,
                        containerId, stateId + 1 + i, toContainer, 1, ClickType.PICKUP,
                        menu.getCarried(), changedR);
                placed++;
            }

            // Put remainder back in source
            Int2ObjectMap<ItemStack> changedBack = new Int2ObjectOpenHashMap<>();
            McVersionCompat.sendContainerClick(conn,
                    containerId, stateId + 1 + placed, fromContainer, 0, ClickType.PICKUP,
                    menu.getCarried(), changedBack);
        }

        JsonObject data = new JsonObject();
        data.addProperty("fromSlot", fromSlot);
        data.addProperty("toSlot", toSlot);
        data.addProperty("count", count <= 0 ? "all" : String.valueOf(count));
        data.addProperty("success", true);
        return ActionResult.ok(data);
    }

    private static int rawToContainerSlot(int raw) {
        if (raw >= 0 && raw <= 8) return raw + 36;   // hotbar
        if (raw >= 9 && raw <= 35) return raw;        // main inventory
        if (raw == 36) return 8;                      // feet
        if (raw == 37) return 7;                      // legs
        if (raw == 38) return 6;                      // chest
        if (raw == 39) return 5;                      // head
        if (raw == 40) return 45;                     // offhand
        return -1;
    }
}
