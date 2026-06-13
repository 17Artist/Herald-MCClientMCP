package ai.herald.clientmod.action.task;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

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
        RecipeManager recipeManager = (RecipeManager) McVersionCompat.getRecipeManager(mc.level);
        if (recipeManager == null) {
            JsonObject data = new JsonObject();
            data.addProperty("item", itemId);
            data.addProperty("count", count);
            data.addProperty("isRaw", true);
            data.addProperty("note", "Recipe lookup not available on multiplayer in 1.21.4");
            return ActionResult.ok(data);
        }

        JsonObject data = resolveRecipeTree(recipeManager, itemId, count, 0);
        return ActionResult.ok(data);
    }

    private JsonObject resolveRecipeTree(RecipeManager rm, String itemId, int count, int depth) {
        JsonObject node = new JsonObject();
        node.addProperty("item", itemId);
        node.addProperty("count", count);

        if (depth >= MAX_DEPTH) {
            node.addProperty("maxDepthReached", true);
            return node;
        }

        Identifier targetRL = Identifier.tryParse(itemId);
        Object foundHolder = null;
        Recipe<?> foundRecipe = null;
        for (Object recipeObj : McVersionCompat.iterateRecipes(rm)) {
            Recipe<?> recipe = (Recipe<?>) McVersionCompat.getRecipeValue(recipeObj);
            if (recipe == null) continue;
            String outputId = McVersionCompat.getRecipeOutputItemId(recipe);
            if (outputId.equals(itemId) || Identifier.tryParse(outputId).equals(targetRL)) {
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
        Identifier rid = McVersionCompat.getRecipeId(foundHolder);
        recipeInfo.addProperty("recipeId", rid != null ? rid.toString() : "");
        recipeInfo.addProperty("type", foundRecipe.getType().toString());
        node.add("recipe", recipeInfo);

        JsonArray ingredients = new JsonArray();
        List<Ingredient> recipeIngredients = foundRecipe.placementInfo().ingredients();
        for (Ingredient ingredient : recipeIngredients) {
            if (ingredient.isEmpty()) continue;
            List<Holder<Item>> items = ingredient.items().toList();
            if (items.isEmpty()) continue;

            Holder<Item> representative = items.get(0);
            String ingId = BuiltInRegistries.ITEM.getKey(representative.value()).toString();
            int ingCount = count; // each slot needs 1 per craft

            JsonObject subTree = resolveRecipeTree(rm, ingId, ingCount, depth + 1);
            ingredients.add(subTree);
        }

        node.add("ingredients", ingredients);
        return node;
    }
}
