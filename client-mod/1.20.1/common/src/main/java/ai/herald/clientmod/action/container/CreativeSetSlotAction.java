package ai.herald.clientmod.action.container;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Port of BlackBoxPro container/CreativeSetSlotAction.kt.
 *
 * <p>Params:
 * <ul>
 *   <li>{@code slot}    — required, server-side container slot index</li>
 *   <li>{@code itemId}  — optional registry id (default {@code minecraft:air} → clear)</li>
 *   <li>{@code count}   — optional, default 1</li>
 *   <li>{@code nbt}     — optional, SNBT string copied into stack tag</li>
 * </ul>
 *
 * <p>Also mirrors the change into the local player's container so the client
 * UI / queries reflect it without waiting for a server echo.
 */
public final class CreativeSetSlotAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int slot = JsonUtil.requireInt(params, "slot");
        String itemId = JsonUtil.getStringOrDefault(params, "itemId", "minecraft:air");
        int count = JsonUtil.getIntOrDefault(params, "count", 1);
        if (count < 0) count = 0;

        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null) return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid itemId: " + itemId);
        Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        if (item == null) return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown item: " + itemId);

        ItemStack stack;
        if (item == net.minecraft.world.item.Items.AIR || count == 0) {
            stack = ItemStack.EMPTY;
        } else {
            stack = new ItemStack(item, Math.min(count, McVersionCompat.itemMaxStackSize(item)));
            String nbt = JsonUtil.getStringOrDefault(params, "nbt", "");
            if (!nbt.isEmpty()) {
                try {
                    CompoundTag tag = McVersionCompat.parseSnbt(nbt);
                    if (tag == null) throw new IllegalArgumentException("parse failed");
                    McVersionCompat.stackSetTag(stack, tag);
                } catch (Exception e) {
                    return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid NBT: " + e.getMessage());
                }
            }
        }

        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();

        // Mirror locally so /query_inventory_slot reads the new contents immediately.
        try {
            player.getInventory().setItem(slotToInventoryIndex(slot), stack.copy());
        } catch (Exception ignore) {
            // Slot indexing is best-effort; the authoritative state lives server-side.
        }
        conn.send(new ServerboundSetCreativeModeSlotPacket(slot, stack));
        return ActionResult.ok();
    }

    /**
     * Vanilla container-slot → {@link net.minecraft.world.entity.player.Inventory} index.
     * Server slot ids: 0=craft-result, 1..4=craft, 5..8=armor, 9..35=main inv,
     * 36..44=hotbar, 45=offhand. Inventory indexing is: 0..8=hotbar, 9..35=main,
     * 36..39=armor, 40=offhand.
     */
    private static int slotToInventoryIndex(int slot) {
        if (slot >= 36 && slot <= 44) return slot - 36;          // hotbar
        if (slot >= 9 && slot <= 35)  return slot;               // main inv
        if (slot >= 5 && slot <= 8)   return 36 + (slot - 5);    // armor
        if (slot == 45)               return 40;                 // offhand
        return -1;
    }
}
