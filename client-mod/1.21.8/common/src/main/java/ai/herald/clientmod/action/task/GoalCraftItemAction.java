package ai.herald.clientmod.action.task;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

public class GoalCraftItemAction implements ActionExecutor {
    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in game");
        }

        String itemId = JsonUtil.getStringOrDefault(params, "itemId", null);
        if (itemId == null || itemId.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required param: itemId");
        }
        int count = params.has("count") ? params.get("count").getAsInt() : 1;

        RecipeManager recipeManager = (RecipeManager) McVersionCompat.getRecipeManager(mc.level);
        if (recipeManager == null) {
            JsonObject data = new JsonObject();
            data.addProperty("itemId", itemId);
            data.addProperty("recipeFound", false);
            data.addProperty("canCraft", false);
            data.addProperty("note", "Recipe lookup not available on multiplayer in 1.21.4");
            return ActionResult.ok(data);
        }

        ResourceLocation targetRL = ResourceLocation.tryParse(itemId);

        // Find a recipe that produces this item
        Object foundHolder = null;
        Recipe<?> foundRecipe = null;
        for (Object recipeObj : McVersionCompat.iterateRecipes(recipeManager)) {
            Recipe<?> recipe = (Recipe<?>) McVersionCompat.getRecipeValue(recipeObj);
            if (recipe == null) continue;
            String outputId = McVersionCompat.getRecipeOutputItemId(recipe);
            if (outputId.equals(itemId) || ResourceLocation.tryParse(outputId).equals(targetRL)) {
                foundHolder = recipeObj;
                foundRecipe = recipe;
                break;
            }
        }

        if (foundRecipe == null) {
            JsonObject data = new JsonObject();
            data.addProperty("itemId", itemId);
            data.addProperty("recipeFound", false);
            data.addProperty("canCraft", false);
            return ActionResult.ok(data);
        }

        // Analyze ingredients via placementInfo
        JsonArray ingredients = new JsonArray();
        JsonArray missingMaterials = new JsonArray();
        boolean canCraft = true;

        List<Ingredient> recipeIngredients = foundRecipe.placementInfo().ingredients();
        for (Ingredient ingredient : recipeIngredients) {
            if (ingredient.isEmpty()) continue;
            List<Holder<Item>> items = ingredient.items().toList();
            if (items.isEmpty()) continue;

            Holder<Item> representative = items.get(0);
            String ingId = BuiltInRegistries.ITEM.getKey(representative.value()).toString();
            int needed = count; // each ingredient slot needs 1 per craft

            int inInventory = countItemInInventory(player, ingId);

            JsonObject ing = new JsonObject();
            ing.addProperty("itemId", ingId);
            ing.addProperty("needed", needed);
            ing.addProperty("inInventory", inInventory);
            ingredients.add(ing);

            if (inInventory < needed) {
                canCraft = false;
                JsonObject missing = new JsonObject();
                missing.addProperty("itemId", ingId);
                missing.addProperty("stillNeeded", needed - inInventory);
                missingMaterials.add(missing);
            }
        }

        JsonObject recipeInfo = new JsonObject();
        ResourceLocation rid = McVersionCompat.getRecipeId(foundHolder);
        recipeInfo.addProperty("recipeId", rid != null ? rid.toString() : "");
        recipeInfo.addProperty("type", foundRecipe.getType().toString());
        recipeInfo.add("ingredients", ingredients);

        JsonObject data = new JsonObject();
        data.addProperty("itemId", itemId);
        data.addProperty("count", count);
        data.addProperty("recipeFound", true);
        data.add("recipe", recipeInfo);
        data.add("missingMaterials", missingMaterials);
        data.addProperty("canCraft", canCraft);
        return ActionResult.ok(data);
    }

    private int countItemInInventory(LocalPlayer player, String itemId) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        var item = BuiltInRegistries.ITEM.get(rl).orElse(null);
        if (item == null) return 0;
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
