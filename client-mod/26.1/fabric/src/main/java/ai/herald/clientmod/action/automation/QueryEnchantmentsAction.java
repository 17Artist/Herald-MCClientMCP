package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Sync: Read all enchantments on an item in a given inventory slot.
 * Params: slot (int, inventory slot number)
 * Returns: array of {id, level} for each enchantment.
 */
public final class QueryEnchantmentsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int slot = JsonUtil.requireInt(params, "slot");

        ItemStack stack = getItemAtSlot(player, slot);
        if (stack == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid slot: " + slot);
        }
        if (stack.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No item in slot " + slot);
        }

        List<?> enchantments = McVersionCompat.getItemEnchantmentTagList(stack);
        JsonArray arr = new JsonArray();
        for (int i = 0; i < enchantments.size(); i++) {
            Object entry = enchantments.get(i);
            CompoundTag tag = entry instanceof CompoundTag c ? c : null;
            if (tag == null) continue;
            JsonObject row = new JsonObject();
            row.addProperty("id", McVersionCompat.tagGetString(tag, "id"));
            row.addProperty("level", (int) McVersionCompat.tagGetShort(tag, "lvl"));
            arr.add(row);
        }

        JsonObject data = new JsonObject();
        data.add("enchantments", arr);
        data.addProperty("itemSlot", slot);
        return ActionResult.ok(data);
    }

    private static ItemStack getItemAtSlot(LocalPlayer player, int slot) {
        if (slot >= 0 && slot <= 35) {
            return player.getInventory().getItem(slot);
        }
        if (slot >= 36 && slot <= 39) {
            return McVersionCompat.getArmorItems(player.getInventory()).get(slot - 36);
        }
        if (slot == 40) {
            return McVersionCompat.getOffhandItem(player.getInventory());
        }
        return null;
    }
}
