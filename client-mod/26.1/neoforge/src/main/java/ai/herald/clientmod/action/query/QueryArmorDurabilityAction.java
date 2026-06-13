package ai.herald.clientmod.action.query;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Sync: Read all armor slots durability.
 * Armor slots: 0=feet, 1=legs, 2=chest, 3=head in the armor list.
 * Returns: [{slot, item, durability, maxDurability, percent}]
 */
public final class QueryArmorDurabilityAction implements ActionExecutor {

    private static final String[] SLOT_NAMES = {"feet", "legs", "chest", "head"};

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        java.util.List<ItemStack> armor = McVersionCompat.getArmorItems(player.getInventory());
        JsonArray arr = new JsonArray();

        for (int i = 0; i < armor.size() && i < SLOT_NAMES.length; i++) {
            ItemStack stack = armor.get(i);
            JsonObject obj = new JsonObject();
            obj.addProperty("slot", SLOT_NAMES[i]);

            if (stack.isEmpty()) {
                obj.addProperty("item", "empty");
                obj.addProperty("durability", 0);
                obj.addProperty("maxDurability", 0);
                obj.addProperty("percent", 0);
            } else {
                Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                obj.addProperty("item", id != null ? id.toString() : "unknown");
                int maxDamage = stack.getMaxDamage();
                int damage = stack.getDamageValue();
                int remaining = maxDamage - damage;
                double percent = maxDamage > 0 ? (remaining * 100.0 / maxDamage) : 100.0;
                obj.addProperty("durability", remaining);
                obj.addProperty("maxDurability", maxDamage);
                obj.addProperty("percent", Math.round(percent * 10.0) / 10.0);
            }
            arr.add(obj);
        }

        JsonObject data = new JsonObject();
        data.add("armor", arr);
        return ActionResult.ok(data);
    }
}
