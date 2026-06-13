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
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/**
 * Sync: Apply an enchanted book via an open anvil.
 * Params: itemSlot (int, player inventory slot of item), bookSlot (int, player inventory slot of book)
 *
 * Anvil menu slot layout:
 *   0 = left input (item)
 *   1 = right input (book)
 *   2 = output
 *   3+ = player inventory slots
 */
public final class EnchantApplyBookAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();

        int itemSlot = JsonUtil.requireInt(params, "itemSlot");
        int bookSlot = JsonUtil.requireInt(params, "bookSlot");

        if (!(player.containerMenu instanceof AnvilMenu menu)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No anvil GUI is open");
        }

        int containerId = menu.containerId;
        int stateId = menu.getStateId();

        // Map player inventory slot to anvil container slot (player inv starts at slot 3)
        int itemContainerSlot = playerSlotToAnvilSlot(itemSlot);
        int bookContainerSlot = playerSlotToAnvilSlot(bookSlot);

        if (itemContainerSlot == -1) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid itemSlot: " + itemSlot);
        }
        if (bookContainerSlot == -1) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid bookSlot: " + bookSlot);
        }

        // Shift-click item into left input slot (0)
        Int2ObjectMap<ItemStack> c1 = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId, itemContainerSlot, 0, ContainerInput.QUICK_MOVE,
                ItemStack.EMPTY, c1);

        // Shift-click book into right input slot (1)
        Int2ObjectMap<ItemStack> c2 = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId + 1, bookContainerSlot, 0, ContainerInput.QUICK_MOVE,
                ItemStack.EMPTY, c2);

        // Shift-click output slot (2) to collect result
        Int2ObjectMap<ItemStack> c3 = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId + 2, 2, 0, ContainerInput.QUICK_MOVE,
                ItemStack.EMPTY, c3);

        JsonObject data = new JsonObject();
        data.addProperty("applied", true);
        return ActionResult.ok(data);
    }

    /**
     * Maps raw player inventory slot to anvil container slot index.
     * Anvil layout: 0=input, 1=book, 2=output, 3-29=main inv(9-35), 30-38=hotbar(0-8).
     */
    private static int playerSlotToAnvilSlot(int raw) {
        if (raw >= 0 && raw <= 8) return raw + 30;   // hotbar -> anvil slots 30-38
        if (raw >= 9 && raw <= 35) return raw - 6;   // main inv -> anvil slots 3-29
        return -1;
    }
}
