package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
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
 * Sync: Collect the output from a furnace (shift-click output slot 2).
 * No params.
 */
public final class SmeltCollectOutputAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();

        if (!(player.containerMenu instanceof AbstractFurnaceMenu menu)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No furnace GUI is open");
        }

        ItemStack output = menu.getSlot(2).getItem();
        if (output.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Output slot is empty");
        }

        int containerId = menu.containerId;
        int stateId = menu.getStateId();

        // Shift-click output slot (index 2)
        Int2ObjectMap<ItemStack> changed = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId, 2, 0, ContainerInput.QUICK_MOVE,
                ItemStack.EMPTY, changed);

        JsonObject data = new JsonObject();
        data.addProperty("collected", true);
        return ActionResult.ok(data);
    }
}
