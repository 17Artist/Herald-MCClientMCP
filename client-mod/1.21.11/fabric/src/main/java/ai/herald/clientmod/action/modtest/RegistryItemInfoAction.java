package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * Returns detailed information about an item from its registry ID.
 */
public final class RegistryItemInfoAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String itemId = JsonUtil.requireString(params, "itemId");
        Identifier loc = Identifier.tryParse(itemId);
        if (loc == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid item ID: " + itemId);
        }

        if (!BuiltInRegistries.ITEM.containsKey(loc)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Item not found: " + itemId);
        }

        Item item = McVersionCompat.registryGet(BuiltInRegistries.ITEM, loc);

        JsonObject data = new JsonObject();
        data.addProperty("id", loc.toString());
        data.addProperty("maxStackSize", McVersionCompat.itemMaxStackSize(item));
        data.addProperty("maxDamage", McVersionCompat.itemMaxDamage(item));
        data.addProperty("isFireResistant", McVersionCompat.itemIsFireResistant(item));
        data.addProperty("rarity", McVersionCompat.itemRarityName(item));

        Object food = McVersionCompat.itemFoodProperties(item);
        if (food != null) {
            JsonObject foodObj = new JsonObject();
            foodObj.addProperty("nutrition", McVersionCompat.foodNutrition(food));
            foodObj.addProperty("saturation", McVersionCompat.foodSaturation(food));
            foodObj.addProperty("isMeat", McVersionCompat.foodIsMeat(food));
            foodObj.addProperty("canAlwaysEat", McVersionCompat.foodCanAlwaysEat(food));
            data.add("foodProperties", foodObj);
        }

        // Description ID (translation key)
        data.addProperty("descriptionId", item.getDescriptionId());

        return ActionResult.ok(data);
    }
}
