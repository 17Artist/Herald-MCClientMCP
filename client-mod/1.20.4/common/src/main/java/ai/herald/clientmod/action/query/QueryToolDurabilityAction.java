package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Sync: Read durability of item in given slot (or current held item).
 * Returns: {item, durability, maxDurability, percentRemaining}
 */
public final class QueryToolDurabilityAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int slot = JsonUtil.getIntOrDefault(params, "slot", -1);

        ItemStack stack;
        if (slot < 0) {
            // Default: current main hand item
            stack = player.getMainHandItem();
        } else {
            if (slot < 0 || slot >= player.getInventory().getContainerSize()) {
                stack = ItemStack.EMPTY;
            } else {
                stack = player.getInventory().getItem(slot);
            }
        }

        JsonObject data = new JsonObject();
        if (stack.isEmpty()) {
            data.addProperty("item", "empty");
            data.addProperty("durability", 0);
            data.addProperty("maxDurability", 0);
            data.addProperty("percentRemaining", 0);
            data.addProperty("hasDurability", false);
        } else {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            data.addProperty("item", id != null ? id.toString() : "unknown");
            int maxDamage = stack.getMaxDamage();
            if (maxDamage > 0) {
                int damage = stack.getDamageValue();
                int remaining = maxDamage - damage;
                double percent = (remaining * 100.0) / maxDamage;
                data.addProperty("durability", remaining);
                data.addProperty("maxDurability", maxDamage);
                data.addProperty("percentRemaining", Math.round(percent * 10.0) / 10.0);
                data.addProperty("hasDurability", true);
            } else {
                data.addProperty("durability", 0);
                data.addProperty("maxDurability", 0);
                data.addProperty("percentRemaining", 100.0);
                data.addProperty("hasDurability", false);
            }
        }
        return ActionResult.ok(data);
    }
}
