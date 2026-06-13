package ai.herald.clientmod.action.task;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

public class QueryRecipeDependenciesAction implements ActionExecutor {
    private static final int MAX_DEPTH = 3;

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not in game");
        }

        String itemId = JsonUtil.getStringOrDefault(params, "itemId", null);
        if (itemId == null || itemId.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required param: itemId");
        }

        int count = params.has("count") ? params.get("count").getAsInt() : 1;
        RecipeManager recipeManager = (net.minecraft.world.item.crafting.RecipeManager) McVersionCompat.getRecipeManager(mc.level);

        JsonObject data = resolveRecipeTree(recipeManager, itemId, count, 0, mc);
        return ActionResult.ok(data);
    }

    private JsonObject resolveRecipeTree(RecipeManager rm, String itemId, int count, int depth, Minecraft mc) {
        JsonObject node = new JsonObject();
        node.addProperty("item", itemId);
        node.addProperty("count", count);

        if (depth >= MAX_DEPTH) {
            node.addProperty("maxDepthReached", true);
            return node;
        }

        ResourceLocation targetRL = ResourceLocation.tryParse(itemId);
        Object foundHolder = null;
        Recipe<?> foundRecipe = null;
        for (Object recipeObj : McVersionCompat.iterateRecipes(rm)) {
            Recipe<?> recipe = (Recipe<?>) McVersionCompat.getRecipeValue(recipeObj);
            ResourceLocation outputId = BuiltInRegistries.ITEM.getKey(
                    recipe.getResultItem(mc.level.registryAccess()).getItem());
            if (outputId.equals(targetRL)) {
                foundHolder = recipeObj;
                foundRecipe = recipe;
                break;
            }
        }

        if (foundRecipe == null) {
            node.addProperty("isRaw", true);
            return node;
        }

        JsonObject recipeInfo = new JsonObject();
        ResourceLocation rid = McVersionCompat.getRecipeId(foundHolder);
        recipeInfo.addProperty("recipeId", rid != null ? rid.toString() : "");
        recipeInfo.addProperty("type", foundRecipe.getType().toString());
        node.add("recipe", recipeInfo);

        JsonArray ingredients = new JsonArray();
        for (Ingredient ingredient : foundRecipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;
            ItemStack[] matchingStacks = ingredient.getItems();
            if (matchingStacks.length == 0) continue;

            ItemStack representative = matchingStacks[0];
            String ingId = BuiltInRegistries.ITEM.getKey(representative.getItem()).toString();
            int ingCount = representative.getCount() * count;

            JsonObject subTree = resolveRecipeTree(rm, ingId, ingCount, depth + 1, mc);
            ingredients.add(subTree);
        }

        node.add("ingredients", ingredients);
        return node;
    }
}
