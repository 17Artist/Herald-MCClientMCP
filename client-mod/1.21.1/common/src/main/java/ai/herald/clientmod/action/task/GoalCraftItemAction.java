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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Optional;

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

        RecipeManager recipeManager = (net.minecraft.world.item.crafting.RecipeManager) McVersionCompat.getRecipeManager(mc.level);
        ResourceLocation targetRL = ResourceLocation.tryParse(itemId);

        // Find a recipe that produces this item
        Object foundHolder = null;
        Recipe<?> foundRecipe = null;
        for (Object recipeObj : McVersionCompat.iterateRecipes(recipeManager)) {
            Recipe<?> recipe = (Recipe<?>) McVersionCompat.getRecipeValue(recipeObj);
            ResourceLocation outputId = BuiltInRegistries.ITEM.getKey(recipe.getResultItem(mc.level.registryAccess()).getItem());
            if (outputId.equals(targetRL)) {
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

        // Analyze ingredients
        JsonArray ingredients = new JsonArray();
        JsonArray missingMaterials = new JsonArray();
        boolean canCraft = true;

        for (Ingredient ingredient : foundRecipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;
            ItemStack[] matchingStacks = ingredient.getItems();
            if (matchingStacks.length == 0) continue;

            ItemStack representative = matchingStacks[0];
            String ingId = BuiltInRegistries.ITEM.getKey(representative.getItem()).toString();
            int needed = representative.getCount() * count;

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
        var item = BuiltInRegistries.ITEM.get(rl);
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item))) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
