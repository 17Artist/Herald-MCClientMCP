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
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/**
 * Sync: Collect brewed potions from the brewing stand bottle slots.
 * Params: slot? (int, 0-2; if omitted, collect all)
 */
public final class BrewCollectAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();

        if (!(player.containerMenu instanceof BrewingStandMenu menu)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No brewing stand GUI is open");
        }

        int slot = JsonUtil.getIntOrDefault(params, "slot", -1);
        int containerId = menu.containerId;
        int stateId = menu.getStateId();
        int collected = 0;

        if (slot >= 0 && slot <= 2) {
            // Collect specific slot
            if (!menu.getSlot(slot).getItem().isEmpty()) {
                Int2ObjectMap<HashedStack> changed = new Int2ObjectOpenHashMap<>();
                conn.send(new ServerboundContainerClickPacket(
                        containerId, stateId, (short) slot, (byte) 0, ContainerInput.QUICK_MOVE,
                        changed, HashedStack.EMPTY));
                collected = 1;
            }
        } else {
            // Collect all bottle slots
            for (int i = 0; i < 3; i++) {
                if (!menu.getSlot(i).getItem().isEmpty()) {
                    Int2ObjectMap<ItemStack> changed = new Int2ObjectOpenHashMap<>();
                    McVersionCompat.sendContainerClick(conn,
                            containerId, stateId + collected, i, 0, ContainerInput.QUICK_MOVE,
                            ItemStack.EMPTY, changed);
                    collected++;
                }
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("collected", collected);
        return ActionResult.ok(data);
    }
}
